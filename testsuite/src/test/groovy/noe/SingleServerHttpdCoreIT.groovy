package noe

import groovy.util.logging.Slf4j
import noe.common.TestAbstract
import noe.common.utils.JBFile
import noe.common.utils.Platform
import noe.server.ServerAbstract
import noe.workspace.ServersWorkspace
import noe.workspace.WorkspaceHttpd
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test

@Slf4j
class SingleServerHttpdCoreIT extends TestAbstract {
 
  @BeforeClass
  public static void beforeClass() {
    loadTestProperties('/core-test.properties')
    Platform platform = new Platform()
    Assume.assumeFalse('JBCS Httpd is not supported on HP-UX => skipping', platform.isHP())
    Assume.assumeFalse('JBCS Httpd is not supported on older versions of RHEL than RHEL 6', platform.isRHEL4() || platform.isRHEL5())
    workspace = new ServersWorkspace(
        new WorkspaceHttpd()
    )
    workspace.prepare()
    serverController.getHttpdServerIds().each { serverId ->
      ServerAbstract httpdServer = serverController.getServerById(serverId)
      if (httpdServer.mainHttpPort <= 1024) {
        httpdServer.shiftPorts(2000)
      }
      // we are not testing ssl here, lets remove ssl
      def sslConfFile = new File(httpdServer.getConfDeploymentPath(), "ssl.conf")
      log.debug("Deleting ssl.conf: $sslConfFile.absolutePath")
      JBFile.delete(sslConfFile)
    }
  }

  @Test
  void serverStartStopTest() {
    serverController.getServerIds().each { serverId ->
      SingleServerTestUtils.serverStartStopTest(serverId)
    }
  }

  @Test
  void serverStartKillTest() {
    serverController.getServerIds().each { serverId ->
      SingleServerTestUtils.serverStartKillTest(serverId)
    }
  }

  @Test
  void killAllInSystemTest() {
    serverController.getServerIds().each { serverId ->
      SingleServerTestUtils.killAllInSystemTest(serverId)
    }
  }
}
