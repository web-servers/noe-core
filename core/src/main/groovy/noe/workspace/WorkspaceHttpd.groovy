package noe.workspace

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.utils.Cleaner
import noe.common.utils.JBFile
import noe.common.utils.Library
import noe.common.utils.Version
import noe.ews.server.ServerEws
import noe.ews.utils.EwsUtils
import noe.jbcs.utils.HttpdHelper
import noe.jbcs.utils.JbcsUtils
import noe.server.Httpd

@Slf4j
class WorkspaceHttpd extends WorkspaceAbstract {

  def bindAddressHttpd /// Where to bind Httpd server
  def basedirHttpd /// Where Httpd server can be found
  def jbcsW // CoreServicesUtils
  EwsUtils ewsUtils
  final boolean SKIP_POSTINSTALL_DEFAULT = Library.getUniversalProperty('httpd.skip.postinstall', 'false').toBoolean()
  boolean skipHttpdPostInstall = SKIP_POSTINSTALL_DEFAULT

  WorkspaceHttpd() {
    this.bindAddressHttpd = Library.getUniversalProperty('bind.address.httpd', DefaultProperties.HOST)
    this.basedirHttpd = Library.getUniversalProperty('basedir.httpd', null)
    initHttpWorkspace()
  }

  void initHttpWorkspace() {
    def httpdBasedir = getBasedir()
    if (basedirHttpd) httpdBasedir = basedirHttpd
    def httpdDir
    if (platform.isWindows() || platform.isSolaris()) {
      if (DefaultProperties.apacheCoreVersion()) {
        httpdDir = null
      } else {
        httpdDir = ServerEws.getPrefix()
      }
    } else {
      httpdDir = Httpd.defaultServerId
    }

    if (context.consistsOf(['ews']) || DefaultProperties.USE_HTTPD_RPM) {
      serverController.addServerHttpd(Httpd.defaultServerId, httpdBasedir, ServerEws.getHttpdVersion(), [host: bindAddressHttpd], httpdDir)
    } else {
      serverController.addServerHttpd(Httpd.defaultServerId, httpdBasedir, new Version("2.2"), [host: bindAddressHttpd], httpdDir)
    }
    copyCertificates()
  }

  private installHttpd(Boolean purge) {
    if (DefaultProperties.apacheCoreVersion()) {
      jbcsW = new JbcsUtils()
      if (!skipInstall) jbcsW.installHttpd(purge)
    } else if (serverController.noeContext.consistsOf(['ews'])) {
      ewsUtils = new EwsUtils()
      if (!skipInstall) ewsUtils.installHttpd(purge)
    } else if (serverController.noeContext.consistsOf(['eap6'])) {
      log.info("Skipping installation in case of EAP6 as it should already be done as part of its workspace initialization")

    }
    if (!skipHttpdPostInstall) {
      if (platform.isSolaris() && JBFile.useAdminPrivileges && DefaultProperties.SOLARIS_DEFAULT_LIBRARY_PATH_CLEAN) {
        if (Cleaner.clearSolarisDefaultLibraryPath()) {
          throw new RuntimeException('Cleaning Default Library Path on Solaris went wrong')
        }
      }
      HttpdHelper httpdHelper = new HttpdHelper(platform)
      httpdHelper.runPostinstall(serverController.getServerById(Httpd.defaultServerId))
    }
  }

  def prepare(Boolean purge = true) {
    log.debug('Creating Httpd workspace with options purge: {}, skipPostInstall: {}', purge, skipHttpdPostInstall)

    if (!this.context.areInSingleGroup(['ews','rpm']) && !DefaultProperties.USE_HTTPD_RPM) {
      installHttpd(purge)
    }

    Httpd httpd = serverController.getServerById(Httpd.defaultServerId)
    httpd.shiftPorts(0)

    (serverController.httpdServerIds).each { httpdId ->
      // just remove file auth_kerb.conf
      if (Boolean.valueOf(Library.getUniversalProperty("JBPAPP9445_WORKAROUND", false))) {
        log.warn("#${httpdId} Applying patch JBPAPP9445_WORKAROUND")
        if (!platform.isWindows()) {
          serverController.getServerById(httpdId).getConfigDirs().each { confDir ->
            def filePath = serverController.getServerById(httpdId).getServerRoot() + platform.sep + confDir + platform.sep + 'auth_kerb.conf'
            JBFile.delete(new File(filePath))
          }
        }
      }

      /**
       * Settings of mpm module, set MPM_MODULE variable to prefork(default), worker or event
       */
      if (platform.isWindows()) {
        log.debug("Windows use different conf settings, skipping mpm module setting")
      } else {
        def mpmModule = Library.getUniversalProperty('mpm.module', null)
        if(mpmModule) {
          def file = "00-mpm.conf"
          log.info("Applying setting for specific mpm module to ${mpmModule}")
          def matchReplace = ['LoadModule mpm_prefork_module modules/mod_mpm_prefork.so',
                              'LoadModule mpm_worker_module modules/mod_mpm_worker.so',
                              'LoadModule mpm_event_module modules/mod_mpm_event.so']
          matchReplace.each {
            if (it.contains("mpm_" + mpmModule + "_module")) {
              log.trace("${it}, uncommenting")
              serverController.getServerById(httpdId).updateConfReplaceRegExp(file, /#+/ + it, it)
            } else {
              serverController.getServerById(httpdId).updateConfReplaceRegExp(file, it, "#" + it)
            }
          }
        }
      }
    }
    /// Backup all server state
    serverController.backup()
  }

  def destroy() {
    super.destroy(false)
    (serverController.httpdServerIds).each { httpdId ->
      log.debug("cleaning postinstall files for {}", httpdId)
      Httpd httpdServer = serverController.getServerById(httpdId)
      JBFile.delete(httpdServer.getPostInstallErrFile())
      JBFile.delete(httpdServer.getPostInstallOutFile())
    }
    if (platform.isSolaris() && JBFile.useAdminPrivileges && !skipInstall) {
      if (Cleaner.clearSolarisDefaultLibraryPath()) {
        throw new RuntimeException('Cleaning Default Library Path on Solaris went wrong')
      }
    }
    if (!skipInstall && !DefaultProperties.USE_HTTPD_RPM) {
      (serverController.httpdServerIds).each { httpdId ->
        log.debug("removing basedir of {}", httpdId)
        Httpd httpdServer = serverController.getServerById(httpdId)
        JBFile.delete(new File(httpdServer.basedir))
      }
    }
    serverController.clean()
  }

}
