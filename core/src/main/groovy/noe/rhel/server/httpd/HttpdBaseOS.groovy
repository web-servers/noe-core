package noe.rhel.server.httpd

import groovy.util.logging.Slf4j
import noe.common.utils.DirStateVault
import noe.common.utils.JBFile
import noe.common.utils.Library
import noe.common.utils.Version
import noe.server.Httpd
/**
 * RHEL BaseOS Httpd server
 * @author Jan Onderka <jonderka@redhat.com> *
 */
@Slf4j
class HttpdBaseOS extends Httpd {
  protected String serviceName
  private DirStateVault dirStateVault

  HttpdBaseOS(String basedir, version) {
    super(basedir, version)
    this.serviceName = "httpd"
    this.basedir = "/etc/${serviceName}"
    this.dirStateVault = new DirStateVault()

    setDefault()
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
    if (platform.isRHEL9() || platform.isRHEL8() || platform.isRHEL7()) {
      this.start = ['systemctl', 'start', serviceName]
      this.stop = ['systemctl', 'stop', serviceName]
    } else if (platform.isRHEL6()) {
      this.start = ['service', serviceName, 'start']
      this.stop = ['service', serviceName, 'stop']
    } else {
      String osIdentification = platform.getOsName() + platform.getOsVersion() + platform.getOsArch()
      log.error("Currently unsupported RHEL version. " + osIdentification)
      throw new RuntimeException("Unsupported RHEL system version. " +osIdentification)
    }
    this.apachectl = ['/usr/sbin/apachectl']
    this.deploymentPath = "/var/www/html"
    this.confDeploymentPath = this.basedir + '/conf.d'
    this.cgiDeploymentPath = "/var/www/cgi-bin"
    this.modClusterCacheDir = "/var/cache/mod_cluster"
    this.opensslPath = 'openssl' // openssl is on $PATH on RHEL by OS installation
    this.abPath = "/usr/bin/ab"
    this.htdbmPath = "/usr/bin/htdbm"
    this.htpasswdPath = "/usr/bin/htpasswd"
    this.httxt2dbmPath = "/usr/bin/httxt2dbm"
    this.logresolvePath = "/usr/bin/logresolve"

  }

  File getPidFile() {
    return new File("/var/run/${serviceName}/${serviceName}.pid")
  }

  Integer extractPid() {
    int pid
    try {
      String pidFromFile = JBFile.read(pidFile).trim().replaceAll('"','')
      pid = Integer.valueOf(pidFromFile)
    } catch (e) {
      pid = super.extractPid()
    }

    return pid
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
    configDirs.each { String dir ->
      File srcDir = new File(getServerRoot(), dir)
      dirStateVault.push(srcDir)
    }
  }

  void restoreConfs() {
    dirStateVault.popAll()
  }
}
