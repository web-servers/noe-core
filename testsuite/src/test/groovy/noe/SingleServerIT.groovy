package noe

import groovy.util.logging.Slf4j
import noe.common.TestAbstract
import noe.workspace.ServersWorkspace
import noe.workspace.WorkspaceSahi
import org.junit.BeforeClass
import org.junit.Test

@Slf4j
class SingleServerIT extends TestAbstract {

  @BeforeClass
  public static void beforeClass() {
    workspace = new ServersWorkspace(
        new WorkspaceSahi('sahi')
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

}
