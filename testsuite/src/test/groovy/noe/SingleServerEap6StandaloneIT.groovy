package noe

import groovy.util.logging.Slf4j
import noe.common.TestAbstract
import noe.eap.workspace.WorkspaceAS7noHttpd
import noe.server.AS7
import noe.workspace.ServersWorkspace
import org.junit.BeforeClass
import org.junit.Test

@Slf4j
class SingleServerEap6StandaloneIT extends TestAbstract {

  @BeforeClass
  public static void beforeClass() {
    loadTestProperties('/eap6-test.properties')
    workspace = new ServersWorkspace(
        new WorkspaceAS7noHttpd('eap6-standalone-1')
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
  void startStopInAdminOnlyTest() {
    Map<String, List<String>> options = Collections.singletonMap(AS7.ADDITIONAL_PARAMETERS_LIST, ['--admin-only'])
    serverController.getServerIds().each { serverId ->
      SingleServerTestUtils.serverStartStopTest(serverId, options)
    }
  }

}
