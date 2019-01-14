package noe.eap.workspace

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.utils.Cleaner
import noe.common.utils.Library
import noe.common.utils.Version
import noe.eap.utils.Eap6Utils
import noe.ews.utils.EwsUtils
import noe.jbcs.utils.HttpdHelper
import noe.jbcs.utils.JbcsUtils
import noe.server.Httpd
import noe.workspace.WorkspaceAS7
import noe.workspace.WorkspaceAbstract
import noe.workspace.WorkspaceHttpd
/**
 *
 * @author Michal Karm Babacek <mbabacek@redhat.com>
 *
 */
@Slf4j
class WorkspaceHttpdAS7 extends WorkspaceAbstract {

  protected static WorkspaceHttpd workspaceHttpd
  protected static WorkspaceAS7 workspaceAS7
  protected Eap6Utils eapW
  protected EwsUtils ewsW

  Boolean eap
  Boolean ews
  String eapVersion /// like 6.0.1.ER2 in jboss-eap-6.0.1.ER2.zip
  String ewsVersion /// like 2.0.0 in jboss-ews-httpd-2.0.0-RHEL6-x86_64.zip
  Boolean installNativesZip
  Boolean installNativesUtilsZip
  Boolean installWebConnectorsZip

  /**
   * This method is meant for overriding. If you want to skip running postinstall in subclass, you should override this method,
   * otherwise postinstall is not skipped by default.
   * @return whether httpdPostInstall should be skipped
   */
  protected boolean skipHttpdPostInstall() {
    return false
  }

  WorkspaceHttpdAS7(Boolean installNativesZip = false, Boolean installWebConnectorsZip = false, Boolean installNativesUtilsZip = false) {
    this.eapVersion = Library.getUniversalProperty('eap.version')
    this.ewsVersion = DefaultProperties.apacheCoreVersion() ? '' : Library.getUniversalProperty('ews.version')

    assert new Version(eapVersion).majorVersion >= new Version("6.0.0").majorVersion
    //downloadClusterBench()
    workspaceHttpd = new WorkspaceHttpd()
    workspaceAS7 = new WorkspaceAS7()

    this.eap = Boolean.valueOf(Library.getUniversalProperty('eap', 'true'))
    this.ews = Boolean.valueOf(Library.getUniversalProperty('ews', 'true'))
    this.installNativesZip = installNativesZip
    this.installWebConnectorsZip = installWebConnectorsZip
    this.installNativesUtilsZip = installNativesUtilsZip
    initWorkspaceHttpdAS7()
  }

  /**
   * Prepare the workspace.
   */
  def prepare() {
    log.info('Creating new Workspace')
    workspaceHttpd.skipHttpdPostInstall = skipHttpdPostInstall()
    workspaceHttpd.prepare(false)
    workspaceAS7.prepare()
    /// Backup all servers state
    serverController.backup()
  }

  /**
   * Destroy the workspace.
   */
  def destroy() {
    log.info('Destroying Workspace')
    workspaceHttpd.destroy()
    workspaceAS7.destroy()
    // TODO: Verify this
    if (this.eap && this.ews && !this.skipInstall) {
      if (deleteWorkspace) {
        Cleaner.cleanDirectoryBasedOnRegex(new File(getBasedir()), /.*(jws|jboss-ews|jbcs|jboss-eap).*/)
        log.info('EAP workspace deleted: ' + basedir)
      }
      log.info('EAP workspace NOT deleted: ' + basedir)
    }
  }

  /**
   * Initialize the workspace.
   */
  void initWorkspaceHttpdAS7() {
    if (!skipInstall) {
      installHttpdAS7()
    }
  }

  /**
   * Install HttpdAS7
   * Static dir expected.
   */
  void installHttpdAS7() {
    log.info('WorkspaceHttpdAS7.installHttpdAS7(): EAP BEGIN')
    eapW = new Eap6Utils(basedir, ant, platform, eapVersion, ewsVersion)
    eapW.getIt()
    if (installNativesZip) eapW.installNativesZip()
    if (installNativesUtilsZip) eapW.installNativesUtilsZip()
    if (installWebConnectorsZip) eapW.installWebConnectorsZip()
    ewsW = new EwsUtils()
    // No ews.getIt(), we use class Eap6 for getting httpd zip...
    if (DefaultProperties.apacheCoreVersion()) {
      new JbcsUtils().installHttpd()
    }
    if (!DefaultProperties.USE_HTTPD_RPM && !skipHttpdPostInstall()) {
      serverController.getHttpdServerIds().each { httpdServerId ->
        Httpd httpd = serverController.getServerById(httpdServerId)
        HttpdHelper httpdHelper = new HttpdHelper(platform)
        httpdHelper.runPostinstallAndFixExecRights(httpd)
      }
    }
  }

  /**
   * Implementing of groovy fallback missingProeperty.
   *
   * @link http://groovy.codehaus.org/Using+methodMissing+and+propertyMissing
   */
  def propertyMissing(String name) {
    if (workspaceHttpd.hasProperty(name)) {
      workspaceHttpd.name
    } else if (workspaceAS7.hasProperty(name)) {
      workspaceAS7.name
    } else throw new RuntimeException("WorkspaceHttpdAS7.propertyMissing(): WorkspaceHttpd nor WorkspaceAS7 has property $name")
  }

}
