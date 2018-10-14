package noe.ews.server.httpd

import groovy.util.logging.Slf4j
import noe.common.utils.Cmd
import noe.common.utils.JBFile
import noe.common.utils.Version
import noe.ews.server.ServerEws
import noe.server.Httpd

@Slf4j
class HttpdSolaris extends Httpd {

  boolean needsSudo = JBFile.useAdminPrivileges

  HttpdSolaris(String basedir, version, String httpdDir) {
    super(basedir, version)
    this.httpdDir = (httpdDir) ? httpdDir : ServerEws.getPrefix()
    this.basedir = basedir + '/' + this.httpdDir
    this.refBasedir = basedir + '/' + ServerEws.getPrefix()
    // please initialize in setDefault() method
    setDefault()
  }

  void setDefault() {
    super.setDefault()
    this.httpdServerRoot = '/etc/httpd'
    this.configDirs = [
        httpdServerRoot + '/conf',
        httpdServerRoot + '/conf.d'
    ]
    if (version >= new Version("2.4")) {
      this.configDirs.add(httpdServerRoot + '/conf.modules.d')
    }
    this.logDirs = [httpdServerRoot + '/logs']
    this.libDir = this.basedir + "/lib" + ((platform.isX64() || platform.isSparc64()) ? "64" : "")
    this.modulesDir = this.libDir + "/httpd/modules"
    this.binPath = '/sbin'
    configureApachectl()
    this.deploymentPath = this.basedir + "/var/www/html"
    this.confDeploymentPath = this.basedir + httpdServerRoot + '/conf.d'
    this.cgiDeploymentPath = this.basedir + "/var/www/cgi-bin"
    this.modClusterCacheDir = this.basedir + "/var/cache/mod_cluster"
    this.opensslPath = this.basedir + '/bin/openssl'
    this.abPath = this.basedir + "/bin/ab"
    this.htdbmPath = this.basedir + "/bin/htdbm"
    this.htpasswdPath = this.basedir + "/bin/htpasswd"
    this.httxt2dbmPath = this.basedir + "/bin/httxt2dbm"
    this.logresolvePath = this.basedir + "/bin/logresolve"
    this.binDir = this.basedir + "/sbin"

    // ! please add new paths like absolute paths
  }

  void setNeedsSudo(boolean needsSudo) {
    this.needsSudo = needsSudo
    configureApachectl()
    // I assume you will need this, because /var/run won't be accessible.
    if (!needsSudo) {
      // Create run dir
      JBFile.mkdir(new File(this.basedir + "/var/run"), true)
      // Handle broken symlinks :-(
      File lib64Dir = new File(this.basedir + "/lib64")
      File libDir = new File(this.basedir + "/lib")
      if (lib64Dir.exists() && !libDir.exists()) {
        Cmd.executeCommand([
            "ln",
            "-s",
            this.basedir + "/lib64",
            this.basedir + "/lib"
        ], new File("./"))
      } else if (!lib64Dir.exists() && libDir.exists()) {
        Cmd.executeCommand([
            "ln",
            "-s",
            this.basedir + "/lib",
            this.basedir + "/lib64"
        ], new File("./"))
      } else {
        log.debug("Hmm, nothing to do here: lib64Dir.exists():${lib64Dir.exists()}, libDir.exists():${libDir.exists()}")
      }
    }
  }

  // Intentionally not using setApachectl()
  void configureApachectl() {
    this.apachectl = (needsSudo) ? ['sudo', './apachectl'] : ['./apachectl']
    this.start = this.apachectl + ['start']
    this.stop = this.apachectl + ['stop']
  }
}
