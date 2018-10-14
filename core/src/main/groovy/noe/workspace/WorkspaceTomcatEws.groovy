package noe.workspace

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.utils.JBFile
import noe.common.utils.Library
import noe.common.utils.OsUser
import noe.ews.server.ServerEws
import noe.ews.server.tomcat.TomcatCommandUtils
import noe.ews.server.tomcat.TomcatWindows
import noe.ews.utils.EwsUtils
import noe.jbcs.utils.TomcatHelper
import noe.server.Tomcat
import noe.tomcat.configure.TomcatConfigurator

/**
 * Used for creating and setup EWS/JWS tomcat workspaces for testing
 */
@Slf4j
class WorkspaceTomcatEws extends WorkspaceAbstract {

  private final boolean SKIP_POSTINSTALL_DEFAULT = Library.getUniversalProperty('tomcat.skip.postinstall', 'false').toBoolean()
  boolean skipTomcatPostInstall = SKIP_POSTINSTALL_DEFAULT
  EwsUtils ewsUtils
  String ewsVersion
  boolean solarisTomcatAnyone /// do extra "hacks" after post installation phase, to enable run tomcat any user

  WorkspaceTomcatEws() {
    ewsUtils = new EwsUtils()
    this.ewsVersion = DefaultProperties.ewsVersion().toString()
    def basedir = getBasedir()
    this.solarisTomcatAnyone = Boolean.valueOf(Library.getUniversalProperty('ews.solaris.tomcat.anyone', 'false'))

    ServerEws.getTomcatVersions().each { version ->
      log.debug("Register Tomcat ${version.toString()}")
      def server = Tomcat.getInstance(basedir, version.toString(), '', context)
      serverController.addServer("tomcat-${version.toString()}-1", server)
    }
  }

  /**
   * @param purge
   * @return
   */
  def prepare(Boolean purge = true) {
    log.debug('Preparing new TomcatWorkspaceEWS with options purge: {}, skipPostInstall: {}', purge, skipTomcatPostInstall)

    if (!context.areInSingleGroup(['ews', 'rpm'])) {
      installTomcat(purge)
    }

    if (!Boolean.valueOf(Library.getUniversalProperty('JBPAPP9788_FIXED', 'true'))) {
      def lib = serverController.getServerById('tomcat-6-1').getLibDir()
      JBFile.copy(new File("${ews.pathToSource}/2.0.0-ER11/patch/mod_cluster-container-tomcat6.jar"), new File(lib), true)
    }

    if (platform.isSolaris()) {
      prepareSolarisSpecificWorkspace()
    }
    /// Backup all server state
    serverController.backup()
  }

  private installTomcat(Boolean purge) {
    if (!skipInstall) {
      ewsUtils.installTomcat(purge)
    }
    if (!skipTomcatPostInstall) {
      serverController.getTomcatServerIds().each { tomcatId ->
        Tomcat tomcatServer = serverController.getServerById(tomcatId)
        TomcatHelper tomcatHelper = new TomcatHelper(platform)

        // System user tomcat obsolete almost with each run - since its home is depends on JWS_HOME.
        // This leads to unstable behaviour and error "su: No directory!" when trying to
        // execute operation under this user.
        if (JBFile.useAdminPrivileges) {
          new OsUser('tomcat').remove()
        }
        tomcatHelper.runPostinstall(tomcatServer)
      }
    }

    // note this is hack for issue caused by inconsistency between server state in Java object and the state on disk
    // TODO: proper solution would be some factory method, which would take care of both creating physical and Java reference of the new server instance
    serverController.getTomcatServerIds([]).each { tomcatId ->
      Tomcat tomcatServer = serverController.getServerById(tomcatId)

      // resetting window title to make sure it is updated as Tomcat server was just installed
      if (tomcatServer instanceof TomcatWindows) {
        (tomcatServer as TomcatWindows).setWindowTitle(null)
      }
      tomcatServer.setDefault()

      updateTomcatUser(tomcatServer)
    }
  }

  private void updateTomcatUser(Tomcat tomcatServer) {
    if (platform.isSolaris()) {
      String runTomcatAs = TomcatCommandUtils.getTomcatRunUser()

      if (!runTomcatAs.isEmpty()) {
        JBFile.replaceregexp(
            new File(ServerEws.getTomcatHome(), "/etc/sysconfig/tomcat${tomcatServer.getVersion().getMajorVersion()}"),
            'TOMCAT_USER=.*',
            'TOMCAT_USER="' + runTomcatAs + '"',
            true,
            true
        )
      }
    }
  }

  private void prepareSolarisSpecificWorkspace() {
    if (JBFile.useAdminPrivileges) {
      ewsUtils.setMaxLimitsOnSolaris()
      ewsUtils.setPrivilegesOnSolaris()
      ewsUtils.deleteOldTomcatPidOnSolaris()
      ewsUtils.setJavaHomeTomcatSolaris()
      serverController.getTomcatServerIds().each { tomcatId ->
        Tomcat tomcat = (Tomcat) serverController.getServerById(tomcatId)
        if (Boolean.valueOf(Library.getUniversalProperty('JWS795_WORKAROUND', 'false'))) {
          new TomcatConfigurator((Tomcat) serverController.getServerById(tomcatId)).envVariableByAppend('CATALINA_PID', '$CATALINA_HOME/run')
        }
        TomcatHelper.setEolOnSolarisTomcat(tomcat)
        if (solarisTomcatAnyone) {
          TomcatHelper.makeTomcatsRunnableAnyUserSolaris(tomcat)
        }
      }
    }

    // Set -d32 or -d64 to JAVA_OPTS, to use that bit java version
    serverController.getTomcatServerIds([]).each { tomcat ->
      ((Tomcat) serverController.getServerById(tomcat)).setSolarisJavaOpts()
    }
  }

  def destroy() {
    try {
      log.debug('Destroying')
      serverController.getTomcatServerIds().each { tomcatId ->
        Tomcat tomcatServer = serverController.getServerById(tomcatId)
        JBFile.delete(tomcatServer.getPostInstallErrFile())
        JBFile.delete(tomcatServer.getPostInstallOutFile())
      }
    } finally {
      super.destroy()
    }
  }
}
