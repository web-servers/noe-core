package noe.server.installation

import noe.common.DefaultProperties
import noe.common.TestAbstract
import noe.common.utils.Version
import noe.eap.workspace.WorkspaceAS7noHttpd
import noe.server.AS7
import noe.workspace.ServersWorkspace
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test

import java.util.regex.Pattern

class Eap6WithCpIT extends TestAbstract {

  @BeforeClass
  public static void beforeClass() {
    loadTestProperties('/eap6-with-cp-patch.properties')
    workspace = new ServersWorkspace(
        new WorkspaceAS7noHttpd('eap6-standalone-1')
    );
    workspace.prepare();
  }


  @Test
  void testPatchedEapStarted() {
    String serverId = serverController.getServerIds().first()
    AS7 as7Server = serverController.getServerById(serverId)
    as7Server.start()
    try {
      Version eapCpVersion = DefaultProperties.eapCpVersion()
      // check that log contains that server was started with proper EAP CP version => contains "JBoss EAP 6.4.2.GA" for 6.4.2.CP
      Assert.assertTrue("The server wasn't started with updated CP patch, check server logs to see what version the EAP was started with",
      as7Server.waitUntilLogContains("server.log", /.*${Pattern.quote("JBoss EAP ${eapCpVersion.baseVersionString()}.GA")}.*/))
    } finally {
      as7Server.stop()
    }
  }
}
