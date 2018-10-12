package noe.ews.server.httpd

import groovy.util.logging.Slf4j
import noe.common.utils.Cmd
import noe.common.utils.Library
import noe.common.utils.Version
import noe.ews.server.ServerEws
import noe.server.Httpd

@Slf4j
class HttpdWindows extends Httpd {

  HttpdWindows(String basedir, version, String httpdDir) {
    super(basedir, version)
    // Hmm...wrong?
    this.httpdDir = (httpdDir) ? httpdDir : ServerEws.getPrefix()
    this.basedir = basedir + '\\' + this.httpdDir
    this.refBasedir = basedir + '\\' + ServerEws.getPrefix()
    log.debug("Init: basedir:${this.basedir}, ServerEws.getPrefix():${ServerEws.getPrefix()}, httpdDir:${this.httpdDir}")
    //..this would try to access nonexisting folder
    //installApacheWindowsService(new File(basedir, this.httpdDir + "\\bin"))
    // please initialize in setDefault() method
    setDefault()
  }

  void setDefault() {
    super.setDefault()
    this.httpdServerRoot = "\\etc\\httpd"
    this.configDirs = [
        httpdServerRoot + "\\conf",
        httpdServerRoot + "\\conf.d"
    ]
    if (version >= new Version("2.4")) {
      this.configDirs.add(httpdServerRoot + "\\conf.modules.d")
    }
    this.logDirs = ["\\var\\log\\httpd"]
    this.libDir = this.basedir + "\\lib" + (platform.isX64() ? "64" : "")
    this.modulesDir = this.libDir + "\\httpd\\modules"
    this.binPath = "\\bin"
    this.binDir = this.basedir + "\\bin"
    this.apachectl = [this.binDir + "\\httpd.exe"]
    this.start =  apachectl + ["-k", "start"]
    this.stop = apachectl + ["-k", "stop"]
    this.deploymentPath = this.basedir + "\\var\\www\\html"
    this.confDeploymentPath = this.basedir + httpdServerRoot + "\\conf.d"
    this.cgiDeploymentPath = this.basedir + "\\var\\www\\cgi-bin"
    this.modClusterCacheDir = this.basedir + "\\var\\cache\\mod_cluster"
    this.opensslPath = this.binDir + "\\openssl.exe"
    this.abPath = this.binDir + "\\ab.exe"
    this.htdbmPath = this.binDir + "\\htdbm.exe"
    this.htpasswdPath = this.binDir + "\\htpasswd.exe"
    this.httxt2dbmPath = this.binDir + "\\httxt2dbm.exe"
    this.logresolvePath = this.binDir + "\\logresolve.exe"
    this.cachePath = this.basedir + '\\var\\cache\\httpd'

    // ! please add new paths like absolute paths
  }

  /**
   * Install default Apache2.2 Windows Service method
   */
  void installApacheWindowsService(File file) {
    int ret
    // Try to stop IIS, just in case...
    ret = Cmd.executeCommand(["net", "stop", "W3SVC"], new File("."))
    // Check if Windows Apache Service is installed, if not, install it.
    if (!Library.isWinServiceInstalled(this.serverId)) {
      // Try to install httpd
      ret = Cmd.executeCommand([
          this.binDir + "\\httpd.exe",
          "-k",
          "install",
          "-n",
          this.serverId,
          "-f",
          this.basedir + httpdServerRoot + "\\conf\\httpd.conf"
      ], file)
      if (ret != 0) {
        log.error("Cannot install ${this.serverId} service. Return code: $ret, I'll continue, but the results might be unexpected. " +
                "Note: The service wasn't installed previously.")
      }
    } else {
      uninstallApacheWindowsService(file)
      // Try to install httpd
      ret = Cmd.executeCommand([
          this.binDir + "\\httpd.exe",
          "-k",
          "install",
          "-n",
          serverId,
          "-f",
          this.basedir + httpdServerRoot + "\\conf\\httpd.conf"
      ], file)
      if (ret != 0) {
        throw new RuntimeException("Cannot install ${this.serverId} service. Return code: $ret. Note: Had been installed " +
                "previously and was uninstalled.")
      }
    }
  }

  /**
   * Uninstall default Apache2.2 Windows Service method
   */
  void uninstallApacheWindowsService(File file) {
    int ret
    if (Library.isWinServiceInstalled(this.serverId)) {
      // First try to stop it, then uninstall it
      ret = Cmd.executeCommand([
          this.binDir + "\\httpd.exe",
          "-k",
          "stop",
          "-n",
          this.serverId
      ], file)
      ret = Cmd.executeCommand([
          this.binDir + "\\httpd.exe",
          "-k",
          "uninstall",
          "-n",
          this.serverId
      ], file)
      if (ret != 0) {
        throw new RuntimeException("Cannot uninstall ${this.serverId} service. Return code: $ret")
      }
    }
  }

  /**
   * Start the httpd server overridden for Windows
   */
  void start(Map conf = [:], List<String> options = []) {
    List<String> windowsOptions = options + ['-n', serverId]
    super.start(conf, windowsOptions)
  }

  /**
   * Stopping httpd server overriden for Windows
   */
  long stop(Map conf = [:]) {
    log.debug("Stopping httpd server ${serverId}")
    if (!isRunning()) {
      log.debug("Server is already down.")
      return 0
    } else {
      long startTime = new Date().getTime()
      def stopCmd = stop + ["-n", serverId]
      def ret = Cmd.executeCommand(stopCmd, new File(getBinDirFullPath()))
      waitForShutdownComplete()
      long endTime = new Date().getTime()
      if (ret == 0) {
        pid = null
      }
      return endTime - startTime
    }
  }

}
