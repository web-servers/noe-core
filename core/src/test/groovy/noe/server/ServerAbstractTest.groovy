package noe.server

import noe.common.utils.JBFile
import noe.common.utils.PathHelper
import org.junit.After
import org.junit.Before

import groovy.util.logging.Slf4j
import org.junit.Test

import java.util.concurrent.TimeUnit

import static org.junit.Assert.*

/**
 * @author Pavel Reichl <preichl@redhat.com>
 */
@Slf4j
class ServerAbstractTest {
  final String text = "GET /index.html"
  final String pattern = ".*" + text + ".*"
  final List logDirs = ['logDir', 'logDir2']
  final String shouldContainMsg = 'Log file should contain pattern.'
  final String shouldNotContainMsg = 'Log file should not contain pattern.'

  ServerAbstract server
  File baseDir
  File accessLog

  void verifyIsInRange(long value, long min, long max) {
    assertTrue("${value} is not in <${min},${max}>", value >= min && value <= max)
  }

  class TestServer extends Httpd {
    TestServer(String basedir, version) {
      super(basedir, version)
    }
  }

  @Before
  void prepare() {
    baseDir = File.createTempDir('noe', 'ServerAbstract')
    assertNotNull(baseDir)
    accessLog = new File(PathHelper.join(baseDir.getAbsolutePath(), logDirs[0], 'access'))

    logDirs.each { logDir ->
      JBFile.mkdir(new File(PathHelper.join(baseDir.getAbsolutePath(), logDir)))
    }

    server = new TestServer(baseDir.getAbsolutePath(), '2.4.51')
    server.setLogDirs(logDirs)
  }

  @After
  void cleanup() {
    if (baseDir) {
      JBFile.delete(baseDir)
    }
  }

  @Test
  void serverWaitUntilLogContainTest() {

    final long timeout = 3000
    final long tt = 300 // Time tolerance, how long can the computation last in ms

    def benchmark = { closure ->
      def start = System.currentTimeMillis()
      def ret = closure.call()
      def now = System.currentTimeMillis()
      [ret, now - start]
    }

    // Logs don't exist yet
    def (ret, time) = benchmark({
      server.waitUntilLogContains('access', pattern, timeout, TimeUnit.MILLISECONDS)
    })
    assertFalse(shouldNotContainMsg, ret)
    verifyIsInRange(time, timeout, timeout + tt)

    // Create log
    assertTrue(JBFile.createFile(accessLog))
    (ret, time) = benchmark({
      server.waitUntilLogContains('access', pattern, timeout, TimeUnit.MILLISECONDS)
    })
    assertFalse(shouldNotContainMsg, ret)
    verifyIsInRange(time, timeout, timeout + tt)

    //Fill the log
    def thread = Thread.start {
      sleep(timeout.intdiv(2))
      // Add pattern
      accessLog.write(text)
    }

    (ret, time) = benchmark({
      server.waitUntilLogContains('access', pattern, timeout,
              TimeUnit.MILLISECONDS)
    })
    assertTrue(shouldContainMsg, ret)
    verifyIsInRange(time, timeout.intdiv(2), timeout + tt)

    thread.join()
    (ret, time) = benchmark({
      server.waitUntilLogContains('access', pattern, timeout,
              TimeUnit.MILLISECONDS)
    })
    assertTrue(shouldContainMsg, ret)
    verifyIsInRange(time, 0, tt)
  }

  @Test
  void serverLogContainTest() {
    assertFalse(shouldNotContainMsg, server.logContains('access', pattern))
    assertFalse(shouldNotContainMsg, server.logContains(pattern))

    // Create log
    assertTrue(JBFile.createFile(accessLog))
    assertFalse(shouldNotContainMsg, server.logContains('access', pattern))
    assertFalse(shouldNotContainMsg, server.logContains(pattern))

    // Add some data
    accessLog.write('some random text\nnex line of random text')
    assertFalse(shouldNotContainMsg, server.logContains('access', pattern))
    assertFalse(shouldNotContainMsg, server.logContains(pattern))

    // Append pattern
    accessLog.append(text)
    assertTrue(shouldContainMsg, server.logContains('access', pattern))
    assertTrue(shouldContainMsg, server.logContains(pattern))
  }

  @Test
  void serverCountOccurrencesInLogTest() {
    assertEquals(server.countOccurrencesInLog('access', pattern), 0)

    // Create log
    assertTrue(shouldContainMsg, JBFile.createFile(accessLog))
    assertEquals(server.countOccurrencesInLog('access', pattern), 0)

    // Add some data
    accessLog.write('some random text\nnex line of random text')
    assertEquals(server.countOccurrencesInLog('access', pattern), 0)

    // Append pattern
    accessLog.append(text)
    assertEquals(server.countOccurrencesInLog('access', pattern), 1)

    // Append pattern
    accessLog.append('random data\n' + text)
    assertEquals(server.countOccurrencesInLog('access', pattern), 2)
  }
}
