package noe.eap.workspace

import groovy.util.logging.Slf4j
import noe.common.utils.Cleaner
import noe.common.utils.IO
import noe.common.utils.Library
import noe.eap.utils.Eap6Utils
import noe.workspace.WorkspaceAS7Domain
import noe.workspace.WorkspaceAbstract
/**
 *
 * @author Radim Hatlapatka rhatlapa@redhat.com
 * noe-jon
 */
@Slf4j
class WorkspaceAS7DomainNoHttpd extends WorkspaceAbstract {

  protected static WorkspaceAS7Domain workspaceAS7Domain

  Boolean eap
  String eapVersion /// like 6.0.1.ER2 in jboss-eap-6.0.1.ER2.zip

  WorkspaceAS7DomainNoHttpd(String serverId = null) {
    workspaceAS7Domain = new WorkspaceAS7Domain(serverId)

    this.eap = Boolean.valueOf(Library.getUniversalProperty('eap', 'true'))
    this.eapVersion = Library.getUniversalProperty('eap.version')
    initWorkspaceAS7noHttpd()
  }

  /**
   * Prepare the workspace.
   */
  def prepare() {
    IO.handleOutput('Creating of new Workspace started')

    workspaceAS7Domain.prepare()

    /// Backup all servers state
    serverController.backup()

    IO.handleOutput('Creating of new Workspace finished')
  }

  /**
   * Destroy the workspace.
   */
  def destroy() {
    IO.handleOutput('Destroying of default Workspace started')
    workspaceAS7Domain.destroy()
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
    IO.handleOutput('WorkspaceAS7DomainNoHttpd.initWorkspaceAS7noHttpd(): BEGIN')
    // TODO: Validate paths
    // TODO: better input param verification
    if (!skipInstall) {
      installAS7()
    }
    // TODO validate EAP installation
    IO.handleOutput('WorkspaceAS7DomainNoHttpd.initWorkspaceAS7noHttpd(): END')
  }

  /**
   * Static dir expected.
   */
  void installAS7() {
    IO.handleOutput('WorkspaceAS7DomainNoHttpd.installAS7(): EAP BEGIN')
    def eap = new Eap6Utils(basedir, ant, platform, eapVersion, "")
    eap.getIt()
    IO.handleOutput('WorkspaceAS7DomainNoHttpd.installAS7(): EAP END')
  }

  /**
   * Implementing of groovy fallback missingProeperty.
   *
   * @link http://groovy.codehaus.org/Using+methodMissing+and+propertyMissing
   */
  def propertyMissing(String name) {
    if (workspaceAS7Domain.hasProperty(name)) {
      workspaceAS7Domain.name
    } else throw new RuntimeException("WorkspaceAS7.propertyMissing(): WorkspaceAS7 has property $name")
  }

}
