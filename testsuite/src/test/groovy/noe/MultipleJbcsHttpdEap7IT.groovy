package noe

import groovy.util.logging.Slf4j
import noe.common.TestAbstract
import noe.common.utils.Java
import noe.common.utils.Platform
import noe.eap.workspace.WorkspaceMultipleHttpdAS7
import noe.server.Httpd
import noe.workspace.ServersWorkspace
import org.junit.Assert
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test

@Slf4j
class MultipleJbcsHttpdEap7IT extends TestAbstract {

  @BeforeClass
  public static void beforeClass() {
    Assume.assumeTrue("EAP 7 requires at least JDK 1.8", new Java().isJdk1xOrHigher('1.8'))
    Platform platform = new Platform()
    Assume.assumeFalse('JBCS Httpd is not supported on HP-UX => skipping', platform.isHP())
    Assume.assumeFalse('JBCS Httpd is not supported on older versions of RHEL than RHEL 6',
        platform.isRHEL4() || platform.isRHEL5())
    loadTestProperties('/eap7-multiple-jbcs-httpd-test.properties')
    workspace = new ServersWorkspace(
        new WorkspaceMultipleHttpdAS7(false, false)
    );
    workspace.prepare()
  }

  @Test
  public void multipleHttpdInstancesExist() {
    def httpdServerIds = serverController.getHttpdServerIds()
    Assert.assertTrue("There should be multiple HTTPD server's installed, but there is ${httpdServerIds.size()}",
        httpdServerIds.size() >= 2)
    httpdServerIds.each { serverId ->
      Httpd httpd = serverController.getServerById(serverId)
      Assert.assertTrue("Basedir of ${serverId} should exist!", new File(httpd.basedir).exists())
    }
  }
}
