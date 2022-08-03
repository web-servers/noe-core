package noe.ews.workspace

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.utils.Cleaner
import noe.common.utils.JBFile
import noe.common.utils.Library
import noe.common.utils.Version
import noe.ews.server.tomcat.TomcatProperties
import noe.ews.utils.EwsUtils
import noe.jbcs.utils.HttpdHelper
import noe.server.Httpd
import noe.workspace.WorkspaceApacheDS
import noe.workspace.WorkspaceHttpd
import noe.workspace.WorkspaceMultipleTomcats
import noe.workspace.WorkspaceTomcat

/**
 * This workspace allow you to have multiple tomcats and a single httpd
 * @author Paul Lodge
 */
@Slf4j
class WorkspaceMultipleTomcatsHttpd extends WorkspaceMultipleTomcats {

  int numberOfAdditionalTomcats

  protected static WorkspaceHttpd workspaceHttpd
  protected static WorkspaceTomcat workspaceTomcat
  protected static WorkspaceApacheDS workspaceApacheDS

  protected EwsUtils ewsW

  Boolean ews
  Boolean rpm
  Version ewsVersion
  Boolean skipEwsPostinstall
  String hostIpAddress = DefaultProperties.HOST
  Map originalTomcatHosts = [:]

    WorkspaceMultipleTomcatsHttpd(int numberOfAdditionalTomcats = 0) {
    super()
    this.numberOfAdditionalTomcats = numberOfAdditionalTomcats

    workspaceHttpd = new WorkspaceHttpd()
    workspaceTomcat = new WorkspaceTomcat()
    workspaceApacheDS = new WorkspaceApacheDS()
    // we want multiple instances, baseline needs to be without postinstall being already executed,
    // we will execute it later on ourselves after having created copies
    workspaceHttpd.skipHttpdPostInstall = true

    this.ews = Boolean.valueOf(Library.getUniversalProperty('ews', 'true'))
    this.ewsVersion = DefaultProperties.ewsVersion()
    this.skipEwsPostinstall = Boolean.valueOf(Library.getUniversalProperty('ews.postinstall.skip', 'false'))
    this.rpm = Boolean.valueOf(Library.getUniversalProperty('ews.rpm', 'false'))

    initWorkspaceEws()
  }

  def prepare() {
    log.info('Executing prepare for WorkspaceHttpdTomcats.')
    super.prepare()

    workspaceHttpd.prepare()
    if (platform.isWindows()) {
      serverController.installApacheWindowsService(serverController.getHttpdServerId())
    }

    boolean purge = (ewsVersion >= new Version('3.1.0-DR0')) || (DefaultProperties.apacheCoreVersion() != null)
    workspaceTomcat.prepare(purge)
    workspaceApacheDS.prepare()

    /**
     * Tomcats
     */
    createAdditionalTomcatsWithRegistrations(numberOfAdditionalTomcats)

    if (!context.areInSingleGroup(["jbcs", "rpm"])) {
      serverController.getHttpdServerIds().each { httpdServerId ->
        log.debug("EWS: httpdBasedir:${httpdServerId}")
        Httpd httpd = serverController.getServerById(httpdServerId)
        HttpdHelper httpdHelper = new HttpdHelper(platform)
        httpdHelper.runPostinstall(httpd)


        if (platform.isWindows()) {
          serverController.installApacheWindowsService(httpdServerId)
        } else {
          def tmpHttpd = serverController.getServerById(httpdServerId)
          JBFile.makeAccessible(new File(tmpHttpd.basedir + "/" + tmpHttpd.binPath + "/apachectl"))
          if (tmpHttpd.getVersion() < new Version("2.4")) {
            // httpd2.2 in EWS2.1
            JBFile.makeAccessible(new File(tmpHttpd.basedir + "/" + tmpHttpd.binPath + "/httpd.worker"))
            if (!platform.isSolaris()) {
              JBFile.makeAccessible(new File(tmpHttpd.basedir + "/" + tmpHttpd.binPath + "/httpd"))
            }
          } else {
            // httpd2.4 in JWS3.0
            JBFile.makeAccessible(new File(tmpHttpd.basedir + "/" + tmpHttpd.binPath + "/httpd"))
            // Logdir is a symlink to /var/log/httpd
            tmpHttpd.logDirs.each { String logdir ->
              JBFile.makeAccessible(new File(tmpHttpd.basedir + "/" + logdir))
            }
          }
        }
      }

      if (!skipEwsPostinstall) {
        installEws()
      }
    }

    Httpd httpd = serverController.getServerById(serverController.getHttpdServerId()) as Httpd
    httpd.setHost(hostIpAddress)
    httpd.updateConfSetBindAddress(hostIpAddress)

    originalTomcatHosts.putAll(originalTomcatHostsIpAddresses())

    serverController.backup()

    log.info('Creating of new WorkspaceHttpdTomcats has finished.')
  }

  def destroy() {
    log.info('Destroying of WorkspaceHttpdTomcats has started.')
    if (platform.isWindows()) {
      serverController.uninstallWindowsServices()
    }

    String httpdServerId = serverController.getHttpdServerId()
    serverController.getServerById(httpdServerId).setHost(hostIpAddress)

    serverController.getTomcatServerIds([TomcatProperties.TOMCAT_MAJOR_VERSION]).each { String tomcatId ->
      serverController.getServerById(tomcatId).setHost(originalTomcatHosts.get(tomcatId) as String)
    }

    super.destroy()

    workspaceHttpd?.destroy()
    workspaceTomcat?.destroy()
    workspaceApacheDS?.destroy()

    // TODO HP And no basedir was set??
    //      What about Testing standalone tomcats (this.ews should be sufficient)
    if (this.ews && !this.rpm && !this.skipInstall) {
      if (deleteWorkspace) {
        Cleaner.cleanDirectoryBasedOnRegex(new File(getBasedir()), /.*(jws|jboss-ews|jbcs).*/)
        log.info('EWS workspace deleted: ' + basedir)
      }
      log.info('EWS workspace NOT deleted: ' + basedir)
    }

    workspaceHttpd = null
    workspaceTomcat = null
    workspaceApacheDS = null
  }

  /**
   * Initialize the workspace.
   */
  void initWorkspaceEws() {
    log.info('WorkspaceEws.initWorkspaceEws(): BEGIN')

    // Are the paths valid?
    // TODO validate tomcat paths (nodenames)
    def validPaths = Library.validatePaths([
        basedir,
        workspaceHttpd.basedirHttpd
    ])
    if (!validPaths.isEmpty()) {
      // TODO LP How to better handle this?
      throw new RuntimeException('Invalid paths' + validPaths.toString())
    }

    // TODO validate ews installation

    log.info('WorkspaceEws.initWorkspaceEws(): END')
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
      log.info('Installing EWS zip distribution')
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
   * Implementing of groovy fallback missingProperty.
   *
   * @link http://groovy.codehaus.org/Using+methodMissing+and+propertyMissing
   */
  def propertyMissing(String name) {
    if (workspaceHttpd.hasProperty(name)) {
      workspaceHttpd.name
    } else if (workspaceTomcat.hasProperty(name)) {
      workspaceTomcat.name
    } else if (workspaceApacheDS.hasProperty(name)) {
        workspaceApacheDS.name
    } else throw new RuntimeException("WorkspaceHttpdTomcats.propertyMissing(): WorkspaceHttpd nor WorkspaceTomcat has property $name")
  }
}
