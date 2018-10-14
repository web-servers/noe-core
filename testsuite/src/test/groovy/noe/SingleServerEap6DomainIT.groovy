package noe

import groovy.util.logging.Slf4j
import noe.common.TestAbstract
import noe.eap.workspace.WorkspaceAS7DomainNoHttpd
import noe.server.AS7Domain
import noe.workspace.ServersWorkspace
import org.junit.BeforeClass
import org.junit.Test

@Slf4j
class SingleServerEap6DomainIT extends TestAbstract {

  @BeforeClass
  public static void beforeClass() {
    loadTestProperties('/eap6-test.properties')
    workspace = new ServersWorkspace(
        new WorkspaceAS7DomainNoHttpd('eap6-domain-1')
    );
    workspace.prepare();
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

  @Test
  void shiftPortsTest() {
    String serverId = serverController.getServerIds().first()
    AS7Domain server = serverController.getServerById(serverId)
    EAPDomainTestUtils.shiftPortsTest(server)
  }

}
