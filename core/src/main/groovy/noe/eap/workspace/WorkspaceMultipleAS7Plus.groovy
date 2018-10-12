package noe.eap.workspace

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.utils.Cleaner
import noe.common.utils.IO
import noe.common.utils.Library
import noe.common.utils.Version
import noe.eap.server.ServerEap
import noe.eap.utils.Eap6Utils
import noe.server.AS7
import noe.workspace.WorkspaceAbstract
/**
 *
 * @author Michal Karm Babacek <karm@redhat.com>
 *
 */
@Slf4j
class WorkspaceMultipleAS7Plus extends WorkspaceAbstract {

  int numberOfAS7s = 6
  protected Eap6Utils eapW

  Boolean eap
  String eapVersion

  WorkspaceMultipleAS7Plus() {
    this.eap = Boolean.valueOf(Library.getUniversalProperty('eap', 'true'))
    this.eapVersion = Library.getUniversalProperty('eap.version')
    assert new Version(eapVersion) >= new Version("6.0.0")
    //downloadClusterBench()
    initWorkspaceAS()
  }

  /**
   * Destroy the workspace.
   */
  def destroy() {
    if (this.eap && !this.skipInstall) {
      if (deleteWorkspace) {
        Cleaner.cleanDirectoryBasedOnRegex(new File(getBasedir()), /.*(jboss-eap).*/)
      }
    }
  }

  /**
   * Initialize the workspace.
   */
  void initWorkspaceAS() {
    if (!skipInstall) {
      installAS()
    }
  }

  /**
   * Static dir expected.
   */
  void installAS() {
    eapW = new Eap6Utils(basedir, ant, platform, eapVersion, '')
    eapW.getIt()
  }

  def prepare() {
    AS7 nextServer = null
    String id = ""
    for (int i = 1; i <= numberOfAS7s; i++) {
      id = "${ServerEap.getPrefix()}-${i}"
      IO.handleOutput("Creating new AS7 server instance: ${id}")
      if (!serverController.getAs7ServerIds().contains(id)) {
        nextServer = AS7.getInstance(basedir, id, context)
        nextServer.createNewServerInstance(id, DefaultProperties.DEFAULT_SHIFT_PORT_OFFSET * (i - 1))
        serverController.addServer(id, nextServer)
      }
    }
    serverController.backup()
  }
}


