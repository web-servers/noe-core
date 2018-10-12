package noe

import groovy.util.logging.Slf4j
import noe.common.TestAbstract
import noe.common.utils.Platform
import noe.workspace.ServersWorkspace
import noe.workspace.WorkspaceAS5
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test

@Slf4j
class SingleServerAS5IT extends TestAbstract {

  @BeforeClass
  public static void beforeClass() {
    loadTestProperties('/as5-test.properties')
    Assume.assumeFalse("AS5 is no supported on HP-UX => skipping", new Platform().isHP())
    workspace = new ServersWorkspace(
        new WorkspaceAS5(false, "as5-unsecured-1")
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
