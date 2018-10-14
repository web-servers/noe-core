package noe.ews.server.httpd

import groovy.transform.TypeChecked
import groovy.util.logging.Slf4j
import noe.common.utils.Cmd
import noe.common.utils.JBFile
import noe.common.utils.Library
import noe.common.utils.Version
import noe.server.Httpd
/**
 * Httpd server for EWS 2.1 RPM installation
 * for RHEL5 and RHEL6 only
 *
 * @author Libor Fuka   <lfuka@redhat.com>
 *
 */
@TypeChecked
@Slf4j
class HttpdRpm extends Httpd {
  protected String serviceName
  protected String apxsPath

  HttpdRpm(String basedir, version) {
    super(basedir, version)
    this.serviceName = "httpd"
    this.basedir = "/etc/${serviceName}"

    // please initialize in setDefault() method
    setDefault()
    logDirs.each { JBFile.makeAccessible(new File(getServerRoot() + it)) }
  }

  void setDefault() {
    super.setDefault()
    this.configDirs = ['/conf', '/conf.d']
    if (version >= new Version("2.4")) {
      this.configDirs.add('/conf.modules.d')
    }
    this.modulesDir = this.basedir + "/modules"
    this.logDirs = ['/logs']
    // what path will be taken as relative for star/stop cmd
    this.binPath = '/'
    this.start = ['service', serviceName, 'start']  // starting as service
    this.stop = "service ${serviceName} stop"  // stopping as service
    this.apachectl = ['/usr/sbin/apachectl']
    this.deploymentPath = "/var/www/html"
    this.confDeploymentPath = this.basedir + '/conf.d'
    this.cgiDeploymentPath = "/var/www/cgi-bin"
    this.modClusterCacheDir = "/var/cache/mod_cluster"
    this.opensslPath = 'openssl' // openssl is on $PATH on RHEL by OS installation
    this.abPath = "/usr/bin/ab"
    this.htdbmPath = "/usr/bin/htdbm"
    this.htpasswdPath = "/usr/bin/htpasswd"
    this.httxt2dbmPath = "/usr/sbin/httxt2dbm"
    this.logresolvePath = "/usr/bin/logresolve"
    this.apxsPath = "/usr/sbin/apxs"

    // ! please add new paths as absolute paths
  }

  File getPidFile() {
    return new File("/var/run/${serviceName}/${serviceName}.pid")
  }

  Integer extractPid() {
    def pidAsStr = new ByteArrayOutputStream()
    if (Cmd.executeCommandRedirectIO("sudo cat ${getPidFile()}", null, null, pidAsStr, System.err) == 0) {
      pidAsStr = pidAsStr.toString()
      pidAsStr = pidAsStr.trim()
      pidAsStr = pidAsStr.replaceAll('"', '')
      pid = Integer.valueOf(pidAsStr)

      return pid
    } else {
      log.debug("PID file is not accessible. But continuing ...")
      return null
    }
  }

  /**
   * Stop the server
   */
  void start(Map conf = [:]) {
    super.start(conf)
    Library.letsSleep(5000)
  }

  /**
   * Stop the server
   */
  long stop(Map conf = [:]) {
    long stopTime = super.stop(conf)
    Library.letsSleep(3000)
    return stopTime
  }

  void setRequestedSELinuxContext(String context) {
    this.start = ['runcon', '-t', context, this.start]
  }

  void backupConfs() {
    configDirs.each { dir ->
      def srcDir = getServerRoot() + dir
      def bkpDir = srcDir + ".bkp"

      Cmd.executeCommand("sudo cp --recursive --preserve --dereference $srcDir $bkpDir", new File("."))
    }

    configDirs.each { JBFile.makeAccessible(new File(getServerRoot() + it)) }
  }

  void restoreConfs() {
    configDirs.each { dir ->
      def srcDir = getServerRoot() + dir
      def bkpDir = srcDir + ".bkp"

      Cmd.executeCommand(["/bin/sh", "-c", "sudo cp --recursive --preserve $bkpDir/* $srcDir"], new File("."))
      Cmd.executeCommand("sudo rm --recursive --force $bkpDir", new File("."))
      Cmd.executeCommand("sudo chown -R apache:apache $srcDir", new File("."))
    }
  }

  int executeHttpdWorker(option) {
    log.debug("Execute $httpdworker $option")
    int ret = Cmd.executeCommand("$httpdworker $option", new File('/usr/sbin'))
    return ret
  }

  int executeHttpdEvent(option) {
    log.debug("Execute $httpdevent $option")
    int ret = Cmd.executeCommand("$httpdevent $option", new File('/usr/sbin'))
    return ret
  }
}
