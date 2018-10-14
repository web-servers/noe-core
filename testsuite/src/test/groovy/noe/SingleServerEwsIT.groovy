package noe

import groovy.util.logging.Slf4j
import noe.common.TestAbstract
import noe.common.utils.IO
import noe.common.utils.JBFile
import noe.common.utils.Platform
import noe.ews.workspace.WorkspaceEws
import noe.server.ServerAbstract
import noe.workspace.ServersWorkspace
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test

@Slf4j
class SingleServerEwsIT extends TestAbstract {

  @BeforeClass
  public static void beforeClass() {
    loadTestProperties('/ews-test.properties')
    Assume.assumeFalse("EWS is not supported on HP-UX => skipping", new Platform().isHP())
    workspace = new ServersWorkspace(
        new WorkspaceEws()
    );
    workspace.prepare();
    serverController.getHttpdServerIds().each { serverId ->
      ServerAbstract httpdServer = serverController.getServerById(serverId)
      if (httpdServer.mainHttpPort <= 1024) {
        httpdServer.shiftPorts(2000)
      }
      // we are not testing ssl here, lets remove ssl
      def sslConfFile = new File(httpdServer.getConfDeploymentPath(), "ssl.conf")
      IO.handleOutput "Deleting ssl.conf: $sslConfFile.absolutePath"
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
