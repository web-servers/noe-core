package noe.ews.workspace

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.utils.Cleaner
import noe.common.utils.JBFile
import noe.common.utils.Library
import noe.common.utils.Version
import noe.ews.server.tomcat.TomcatProperties
import noe.jbcs.utils.HttpdHelper
import noe.server.Httpd
import noe.workspace.WorkspaceHttpd
import noe.workspace.WorkspaceMultipleTomcats
import noe.workspace.WorkspaceTomcat

@Slf4j
class WorkspaceHttpdTomcats extends WorkspaceMultipleTomcats {

  int numberOfAdditionalTomcats

  protected static WorkspaceHttpd workspaceHttpd
  protected static WorkspaceTomcat workspaceTomcat

  Boolean ews
  Boolean rpm
  Version ewsVersion
  Boolean skipEwsPostinstall
  String hostIpAddress = DefaultProperties.HOST
  Map originalTomcatHosts = [:]

  WorkspaceHttpdTomcats(int numberOfAdditionalTomcats = 0) {
    super()
    this.numberOfAdditionalTomcats = numberOfAdditionalTomcats
    //downloadClusterBench()
    workspaceHttpd = new WorkspaceHttpd()
    workspaceTomcat = new WorkspaceTomcat()
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

    boolean purge = (ewsVersion >= new Version('3.1.0-DR0') ? true : false) || (DefaultProperties.apacheCoreVersion() != null)
    workspaceTomcat.prepare(purge)

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

    Httpd httpd = serverController.getServerById(serverController.getHttpdServerId())
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

    def httpdServerId = serverController.getHttpdServerId()
    serverController.setHost(httpdServerId, hostIpAddress)

    serverController.getTomcatServerIds([TomcatProperties.TOMCAT_MAJOR_VERSION]).each { tomcatId ->
      serverController.setHost(tomcatId, originalTomcatHosts.get(tomcatId))
    }

    super.destroy()

    workspaceHttpd.destroy()
    workspaceTomcat.destroy()

    // TODO HP And no basedir was set??
    //      What about Testing standalone tomcats (this.ews should be sufficient)
    if (this.ews && !this.rpm && !this.skipInstall) {
      if (deleteWorkspace) {
        Cleaner.cleanDirectoryBasedOnRegex(new File(getBasedir()), /.*(jws|jboss-ews|jbcs).*/)
        log.info('EWS workspace deleted: ' + basedir)
      }
      log.info('EWS workspace NOT deleted: ' + basedir)
    }
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
   * Install EWS
   * Static dir expected.
   */
  void installEws() {
    if (!this.context.areInSingleGroup(['ews', 'rpm'])) {
      log.info('Installing EWS zip distribution')
      if (platform.isSolaris() && JBFile.useAdminPrivileges) {
        serverController.getHttpdServerIds().each { httpdId ->
          Httpd httpd = (Httpd) serverController.getServerById(httpdId)
          HttpdHelper.setEolOnSolarisHttpd(httpd)
        }
      }
    } else {
      // Silence is golden.
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
    } else if (workspaceTomcat.hasProperty(name)) {
      workspaceTomcat.name
    } else throw new RuntimeException("WorkspaceHttpdTomcats.propertyMissing(): WorkspaceHttpd nor WorkspaceTomcat has property $name")
  }
}
