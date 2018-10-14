package noe.eap.workspace

import groovy.util.logging.Slf4j
import noe.common.utils.Cleaner
import noe.common.utils.IO
import noe.common.utils.Library
import noe.common.utils.Version
import noe.eap.server.ServerEap
import noe.eap.utils.Eap5Utils
import noe.ews.utils.EwsUtils
import noe.workspace.WorkspaceAS5
import noe.workspace.WorkspaceAbstract
import noe.workspace.WorkspaceHttpd
/**
 *
 * @author Michal Karm Babacek <mbabacek@redhat.com>
 *
 */
@Slf4j
class WorkspaceHttpdAS5 extends WorkspaceAbstract {

  protected static WorkspaceHttpd workspaceHttpd
  protected static WorkspaceAS5 workspaceAS5
  protected Eap5Utils eap5Utils
  protected EwsUtils ewsUtils

  String eapVersion /// like 6.0.1.ER2 in jboss-eap-6.0.1.ER2.zip
  String ewsVersion /// like 2.0.0 in jboss-ews-httpd-2.0.0-RHEL6-x86_64.zip
  Boolean installNativesZip
  Boolean installWebConnectors

  WorkspaceHttpdAS5(Boolean installNativesZip = false, Boolean installWebConnectors = false) {
    this.eapVersion = Library.getUniversalProperty('eap.version')
    this.ewsVersion = Library.getUniversalProperty('ews.version')
    assert new Version(eapVersion) < new Version("6.0.0")
    //downloadClusterBench()
    workspaceHttpd = new WorkspaceHttpd()
    workspaceAS5 = new WorkspaceAS5()
    this.installNativesZip = installNativesZip
    this.installWebConnectors = installWebConnectors
    initWorkspaceHttpdAS5()
  }

  /**
   * Prepare the workspace.
   */
  def prepare() {
    IO.handleOutput('Creating of new Workspace started')
    workspaceHttpd.prepare()
    workspaceAS5.prepare()
    /// Backup all servers state
    serverController.backup()
    IO.handleOutput('Creating of new Workspace finished')
  }

  /**
   * Destroy the workspace.
   */
  def destroy() {
    IO.handleOutput('Destroying of default Workspace started')
    workspaceHttpd.destroy()
    workspaceAS5.destroy()
    // TODO: Verify this
    if (!this.skipInstall) {
      if (deleteWorkspace) {
        Cleaner.cleanDirectoryBasedOnRegex(new File(getBasedir()), /.*(jws|jboss-ews|jbcs|httpd|jboss-eap).*/)
        IO.handleOutput('EAP workspace deleted: ' + basedir)
      }
      IO.handleOutput('EAP workspace NOT deleted: ' + basedir)
    }
    IO.handleOutput('Destroying of default Workspace finished')
  }

  /**
   * Initialize the workspace.
   */
  void initWorkspaceHttpdAS5() {
    IO.handleOutput('WorkspaceHttpdAS5.initWorkspaceHttpdAS5(): BEGIN')
    // TODO: Validate paths
    // TODO: better input param verification
    if (!skipInstall) {
      installHttpdAS5()
    }
    // TODO validate EAP installation
    IO.handleOutput('WorkspaceHttpdAS5.initWorkspaceHttpdAS5(): END')
  }

  /**
   * Install HttpdAS5
   * Static dir expected.
   */
  void installHttpdAS5() {
    IO.handleOutput('WorkspaceHttpdAS5.installHttpdAS5(): EAP BEGIN')
    eap5Utils = new Eap5Utils(basedir, ant, platform, eapVersion, ewsVersion)
    // eap5Utils.getIt() initialized automatically by
    File nativesDir = null
    if (installNativesZip) {
      nativesDir = eap5Utils.installNativesZip()
    }
    if (installWebConnectors) {
      eap5Utils.installWebConnectors(nativesDir)
      eap5Utils.loadModCluster("${ServerEap.getPrefix()}")
    }
    IO.handleOutput('WorkspaceHttpdAS5.installHttpdAS5(): EAP END')
  }

  /**
   * Implementing of groovy fallback missingProeperty.
   *
   * @link http://groovy.codehaus.org/Using+methodMissing+and+propertyMissing
   */
  def propertyMissing(String name) {
    if (workspaceHttpd.hasProperty(name)) {
      workspaceHttpd.name
    } else if (workspaceAS5.hasProperty(name)) {
      workspaceAS5.name
    } else {
      throw new RuntimeException("WorkspaceHttpdAS5.propertyMissing(): WorkspaceHttpd nor WorkspaceAS5 has property $name")
    }
  }

}
