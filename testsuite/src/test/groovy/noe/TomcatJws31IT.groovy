package noe

import groovy.util.logging.Slf4j
import noe.common.TestAbstract
import noe.common.utils.Java
import noe.common.utils.Platform
import noe.workspace.ServersWorkspace
import noe.workspace.WorkspaceTomcat
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test

@Slf4j
class TomcatJws31IT extends TestAbstract {

  @BeforeClass
  public static void beforeClass() {
    Platform platform = new Platform()
    Assume.assumeFalse("JWS is not supported on HP-UX => skipping", platform.isHP())
    Assume.assumeTrue("JWS 3.1 is not supported on RHEL8+ => skipping", platform.isRHEL7())
    Assume.assumeTrue("Tomcat from JWS 3.1 requires at least Java 1.7", Java.isJdk1xOrHigher('1.7'))
    loadTestProperties('/jws31-test.properties')
    workspace = new ServersWorkspace(
        new WorkspaceTomcat()
    );
    workspace.prepare();
  }

  @Test
  void serverStartKillTest() {
    serverController.getServerIds().each { serverId ->
      SingleServerTestUtils.serverStartKillTest(serverId)
    }
  }
}
