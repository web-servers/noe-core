package noe.ews.workspace

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.utils.Cleaner
import noe.common.utils.JBFile
import noe.common.utils.Library
import noe.common.utils.Version
import noe.ews.utils.EwsUtils
import noe.jbcs.utils.HttpdHelper
import noe.server.Httpd
import noe.workspace.WorkspaceAbstract
import noe.workspace.WorkspaceApacheDS
import noe.workspace.WorkspaceHttpd
import noe.workspace.WorkspaceTomcat


@Slf4j
class WorkspaceEws extends WorkspaceAbstract {

  protected static workspaceHttpd
  protected static workspaceTomcat
  protected static workspaceApacheDS

  Boolean ews /// Are we working with EWS servers (we want test Tomcats outside the EWS too)
  protected EwsUtils ewsW
  Boolean rpm /// Are we manipulating with rpm distro?
  String ewsVersion /// like 2.0.0-ER3

  WorkspaceEws(boolean installHttpd = true) {
    if (installHttpd) {
      workspaceHttpd = new WorkspaceHttpd()
    }

    workspaceTomcat = new WorkspaceTomcat()
    workspaceApacheDS = new WorkspaceApacheDS()
    this.ews = Boolean.valueOf(Library.getUniversalProperty('ews', 'true'))
    this.rpm = Boolean.valueOf(Library.getUniversalProperty('ews.rpm', 'false'))
    this.ewsVersion = Library.getUniversalProperty('ews.version')
    initWorkspaceEws()
  }

  /**
   * Prepare the workspace.
   */
  def prepare() {
    if(workspaceHttpd) {
      workspaceHttpd.prepare()
      if (platform.isWindows()) {
        serverController.installApacheWindowsService(serverController.getHttpdServerId())
      }
    }
    // version newer that 3.1.0-DR1 have separated apache and tomcat installations
    boolean purge = (new Version(ewsVersion) >= new Version('3.1.0-DR0')) || (DefaultProperties.apacheCoreVersion() != null)
    workspaceTomcat.prepare(purge)
    workspaceApacheDS.prepare()
    
    if (!DefaultProperties.EWS_SKIP_POSTINSTALL) {
      installEws()
    }

    /// Backup all servers state
    serverController.backup()
  }

  /**
   * Destroy the workspace.
   */
  def destroy() {
    workspaceHttpd?.destroy()
    workspaceTomcat?.destroy()
    workspaceApacheDS?.destroy()

    // TODO HP And no basedir was set??
    //      What about Testing standalone tomcats (this.ews should be sufficient)
    if (this.ews && !this.rpm && !this.skipInstall) {
      if (deleteWorkspace) {
        Cleaner.cleanDirectoryBasedOnRegex(new File(getBasedir()), /.*(jws|jboss-ews|jbcs).*/)
        log.debug('EWS workspace deleted: ' + basedir)
      } else {
        log.debug('EWS workspace NOT deleted: ' + basedir)
      }
    }

    workspaceHttpd = null
    workspaceTomcat = null
    workspaceApacheDS = null
  }

  /**
   * Initialize the workspace.
   */
  void initWorkspaceEws() {
    // Are the paths valid?
    // TODO validate tomcat paths (nodenames)
    def validPaths = Library.validatePaths([
        basedir,
        workspaceHttpd?.basedirHttpd
    ])
    if (!validPaths.isEmpty()) {
      // TODO LP How to better handle this?
      throw new RuntimeException('Invalid paths' + validPaths.toString())
    }
  }

  /**
   * Unzips Hibernate Library archive in src/main/resources/hibernate/library 
   * Downloads JDBC drivers
   */
  void prepareHibernate(Boolean unzipHibernate = true) {
    ewsW = new EwsUtils()

    if (unzipHibernate) {
      ewsW.unzipHibernateLibrary()
    }

    ewsW.downloadJDBCDriver()
  }

  /**
   * Remove Hibernate Library and JDBC driver from resources
   */
  void cleanupHibernate() {
    ewsW = new EwsUtils()
    ewsW.removeUnzippedHibernateLib()
    ewsW.removeJDBCDriver()
  }

  /**
   * Install EWS
   * Static dir expected.
   *
   * @see GetEWS.getIt ( )
   */
  void installEws() {
    if (!this.context.areInSingleGroup(['ews', 'rpm'])) {
      ewsW = new EwsUtils()
      if (platform.isSolaris() && JBFile.useAdminPrivileges) {
        serverController.getHttpdServerIds().each { httpdId ->
          Httpd httpd = (Httpd) serverController.getServerById(httpdId)
          HttpdHelper.setEolOnSolarisHttpd(httpd)
        }
      }
    }
  }

  /**
   * Implementing of groovy fallback missingProeperty.
   *
   * @link http://groovy.codehaus.org/Using+methodMissing+and+propertyMissing
   */
  def propertyMissing(String name) {
    if (workspaceHttpd?.hasProperty(name)) {
      workspaceHttpd.name
    } else if (workspaceTomcat.hasProperty(name)) {
      workspaceTomcat.name
    } else if (workspaceApacheDS.hasProperty(name)) {
      workspaceApacheDS.name
    } else throw new RuntimeException("WorkspaceEws.propertyMissing(): WorkspaceHttpd nor WorkspaceTomcat has property $name")
  }

}
