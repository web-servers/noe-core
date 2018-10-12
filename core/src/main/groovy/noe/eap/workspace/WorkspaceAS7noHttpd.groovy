package noe.eap.workspace

import groovy.util.logging.Slf4j
import noe.common.utils.Cleaner
import noe.common.utils.IO
import noe.common.utils.Library
import noe.common.utils.Version
import noe.eap.utils.Eap6Utils
import noe.workspace.WorkspaceAS7
import noe.workspace.WorkspaceAbstract


/**
 *
 * @author Michal Karm Babacek <mbabacek@redhat.com>
 * TODO: shouldn't this be part of standard WorkspaceAS7 class
 */
@Slf4j
class WorkspaceAS7noHttpd extends WorkspaceAbstract {

  protected static WorkspaceAS7 workspaceAS7

  Boolean eap
  String eapVersion /// like 6.0.1.ER2 in jboss-eap-6.0.1.ER2.zip

  WorkspaceAS7noHttpd(serverId = null) {
    this.eap = Boolean.valueOf(Library.getUniversalProperty('eap', 'true'))
    this.eapVersion = Library.getUniversalProperty('eap.version')
    assert new Version(eapVersion) >= new Version("6.0.0")
    //downloadClusterBench()
    workspaceAS7 = new WorkspaceAS7(serverId)
    initWorkspaceAS7noHttpd()
  }

  /**
   * Prepare the workspace.
   */
  def prepare() {
    IO.handleOutput('Creating of new Workspace started')

    workspaceAS7.prepare()

    /// Backup all servers state
    serverController.backup()

    IO.handleOutput('Creating of new Workspace finished')
  }

  /**
   * Destroy the workspace.
   */
  def destroy() {
    IO.handleOutput('Destroying of default Workspace started')
    workspaceAS7.destroy()
    // TODO: Verify this
    if (this.eap && !this.skipInstall) {
      if (deleteWorkspace) {
        Cleaner.cleanDirectoryBasedOnRegex(new File(getBasedir()), /.*(jboss-eap).*/)
        IO.handleOutput('EAP workspace deleted: ' + basedir)
      }
      IO.handleOutput('EAP workspace NOT deleted: ' + basedir)
    }
    IO.handleOutput('Destroying of default Workspace finished')
  }

  /**
   * Initialize the workspace.
   */
  void initWorkspaceAS7noHttpd() {
    IO.handleOutput('WorkspaceAS7noHttpd.initWorkspaceAS7noHttpd(): BEGIN')
    // TODO: Validate paths
    // TODO: better input param verification
    if (!skipInstall) {
      installAS7()
    }
    // TODO validate EAP installation
    IO.handleOutput('WorkspaceAS7noHttpd.initWorkspaceAS7noHttpd(): END')
  }

  /**
   * Static dir expected.
   */
  void installAS7() {
    IO.handleOutput('WorkspaceAS7noHttpd.installAS7(): EAP BEGIN')
    def eap = new Eap6Utils(basedir, ant, platform, eapVersion, "")
    eap.getIt()
    IO.handleOutput('WorkspaceAS7noHttpd.installAS7(): EAP END')
  }

  /**
   * Implementing of groovy fallback missingProeperty.
   *
   * @link http://groovy.codehaus.org/Using+methodMissing+and+propertyMissing
   */
  def propertyMissing(String name) {
    if (workspaceAS7.hasProperty(name)) {
      workspaceAS7.name
    } else throw new RuntimeException("WorkspaceHttpdAS7.propertyMissing(): WorkspaceHttpd nor WorkspaceAS7 has property $name")
  }

}
