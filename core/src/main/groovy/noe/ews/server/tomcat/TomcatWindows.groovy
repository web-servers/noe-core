package noe.ews.server.tomcat

import groovy.util.logging.Slf4j
import noe.common.utils.Cmd
import noe.common.utils.Library
import noe.common.utils.Version
import noe.ews.server.ServerEws
import noe.server.Tomcat
/**
 * EWS Tomcat server on Windows
 *
 * @author Jan Stefl   <jstefl@redhat.com>
 *
 */
@Slf4j
class TomcatWindows extends Tomcat {

  String windowTitle

  TomcatWindows(String basedir, version, String tomcatDir = "") {
    super(basedir, version)

    String ewsPrefix = basedir + "\\" + ServerEws.getPrefix()
    if (ServerEws.extractMajorVersion() > 3) {
      this.tomcatDir = (tomcatDir) ?: "tomcat"
      this.refBasedir = "${ewsPrefix}\\tomcat"
      this.basedir = "${ewsPrefix}\\${this.tomcatDir}"
    } else {
      this.tomcatDir = (tomcatDir) ?: "tomcat" + this.version
      this.refBasedir = "${ewsPrefix}\\share\\tomcat${this.version}"

      this.extrasDir = "${ewsPrefix}\\share\\extras"
      this.extrasDirs = [this.extrasDir, this.basedir + '\\extras']
      this.basedir = "${ewsPrefix}\\share\\${this.tomcatDir}"
    }

    // ! please add new paths like absolute paths
    setDefault()
  }

  void setDefault() {
    super.setDefault()
    this.deploymentPath = this.basedir + "\\webapps"
    this.binPath = "\\bin"
    this.start = ['cmd', '/c', 'startup.bat']
    //this.start = Cmd.makeCommand(runContext, this.basedir + binPath + "/startup.sh $processCode")
    this.startSecurity = ['cmd', '/c', 'startup.bat -security']
    this.stop = ['cmd', '/c', 'shutdown.bat']
    this.catalina = ['cmd', '/c', 'catalina.bat']
    this.daemonStart = '' // No daemon for Windows platform
    this.daemonStop = '' // No daemon for Windows platform
    this.configDirs = ["\\conf"]
    this.logDirs = ["\\logs"]
    this.libDir = this.basedir + "\\lib"
    this.binDir = "${this.basedir}\\bin"
    if (ServerEws.extractMajorVersion() < 5) {
      this.jsvc = "${this.extrasDir}\\jsvc"
    } else {
      this.jsvc = "${this.binDir}\\jsvc"
    }
    this.workDir = "\\work" // TODO absolute path
    this.confDeploymentPath = this.basedir + "\\conf"
    // ! please add new paths like absolute paths
    this.windowTitle = configureTomcatWindowTitle()
  }

  void installTomcatAsWindowsService(File directoryContainingServiceBat) {
    log.debug("Installing tomcat tomcat${this.version} service using ${directoryContainingServiceBat.getAbsolutePath()}")
    def ret
    def filename = "service.bat"
    if (Library.isWinServiceInstalled("tomcat${this.version}")) {
      uninstallTomcatAsWindowsService(directoryContainingServiceBat)
    }
    ret = Cmd.executeCommand(["cmd", "/c", filename, "install"], directoryContainingServiceBat)
    if (ret != 0) {
      throw new RuntimeException("Cannot install tomcat${this.version} Windows service. Return code: $ret")
    }
  }

  void uninstallTomcatAsWindowsService(File directoryContainingServiceBat) {
    log.debug("Uninstalling tomcat${this.version} service using ${directoryContainingServiceBat.getAbsolutePath()}")
    def ret
    def filename = "service.bat"
    if (Library.isWinServiceInstalled("tomcat${this.version}")) {
      ret = Cmd.executeCommand(["cmd", "/c", filename, "remove"], directoryContainingServiceBat)
      if (ret != 0) {
        throw new RuntimeException("Cannot uninstall tomcat${this.version} Windows service. Return code: $ret")
      }
    } else {
      log.debug("Skipping uninstallation of tomcat${this.version} windows service; already uninstalled.")
    }
  }

  void startAsWindowsService() {
    portsAvailable()
    log.debug("Starting tomcat${this.version} as windows service")
    def ret = Cmd.executeCommand(["net", "start", "tomcat${this.version}"], new File("."))
    if (ret != 0) {
      throw new RuntimeException("Cannot start tomcat${this.version} service. Return code: $ret. Test it with locally instaled JDK, JRE.")
    }
    waitForStartComplete()
  }

  void stopAsWindowsService() {
    log.debug("Stopping tomcat${this.version} as windows service")
    def ret = Cmd.executeCommand(["net", "stop", "tomcat${this.version}"], new File("."))
    if (ret != 0) {
      throw new RuntimeException("Cannot stop tomcat${this.version} service. Return code: $ret. Test it with locally instaled JDK, JRE.")
    }
    waitForShutdownComplete()
  }

  /**
   * Set Tomcat WindowTitle
   */
  private String configureTomcatWindowTitle() {
    boolean success = false
    if (this.processCode && this.processCode != '0' && this.processCode != this.windowTitle) {
      if (version == new Version("6")) {
        success = updateBinReplaceRegExp('catalina.bat', "set _EXECJAVA=start \"[^\"]*\"", "set _EXECJAVA=start \"${this.processCode}\"", true, false)
      }
      if (version >= new Version("7")) {
        success = updateBinReplaceRegExp('catalina.bat', "set TITLE=.*", "set TITLE=${this.processCode}", true, false)
      }
    }
    log.debug("C: configureTomcatWindowTitle: this.serverId: ${this.serverId}, this.processCode: ${this.processCode}, this.windowTitle: ${this.windowTitle}")
    // Window title is process code, unless the replace wasn't successful.
    return (success) ? this.processCode : null
  }
}

