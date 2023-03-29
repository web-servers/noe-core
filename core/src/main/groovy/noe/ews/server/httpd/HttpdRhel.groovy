package noe.ews.server.httpd

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.utils.JBFile
import noe.common.utils.Version
import noe.ews.server.ServerEws
import noe.server.Httpd

@Slf4j
class HttpdRhel extends Httpd {
  String apxsPath

  boolean needsSudo = JBFile.useAdminPrivileges

  HttpdRhel(String basedir, version, httpdDir) {
    super(basedir, version)
    this.httpdDir = (httpdDir) ? httpdDir : Httpd.defaultServerId
    this.basedir = basedir + '/' + ServerEws.getPrefix() + '/' + this.httpdDir
    this.refBasedir = basedir + '/' + ServerEws.getPrefix() + '/' + Httpd.defaultServerId
    // please initialize in setDefault() method
    setDefault()
  }

  void setDefault() {
    super.setDefault()
    this.configDirs = ['/conf', '/conf.d']
    if (version >= new Version("2.4")) {
      this.configDirs.add('/conf.modules.d')
    }
    this.logDirs = ['/logs']
    this.modulesDir = this.basedir + "/modules"
    this.binPath = '/sbin'
    configureApachectl()
    this.deploymentPath = this.basedir + "/www/html"
    this.confDeploymentPath = this.basedir + "/${DefaultProperties.CONF_DIRECTORY}"
    this.confModulesDeploymentPath = this.basedir + "/${DefaultProperties.CONF_MODULES_DIRECTORY}"
    this.cgiDeploymentPath = this.basedir + "/www/cgi-bin"
    this.modClusterCacheDir = this.basedir + "/cache/mod_cluster"
    this.opensslPath = 'openssl' // openssl is in $PATH on RHEL by OS installation
    this.abPath = this.basedir + "/sbin/ab"
    this.htdbmPath = this.basedir + "/sbin/htdbm"
    this.htpasswdPath = this.basedir + "/sbin/htpasswd"
    this.httxt2dbmPath = this.basedir + "/sbin/httxt2dbm"
    this.logresolvePath = this.basedir + "/sbin/logresolve"
    this.apxsPath = this.basedir + "/sbin/apxs"
    this.binDir = this.basedir + "/sbin"
    this.libDir = this.basedir + "/lib"

    // ! please add new paths like absolute paths
  }

  void setNeedsSudo(boolean needsSudo) {
    this.needsSudo = needsSudo
    configureApachectl()
  }

  //Intentionally not using setApachectl()
  void configureApachectl() {
    this.apachectl = (needsSudo) ? ['sudo', './apachectl'] : ['./apachectl']
    this.start = apachectl + ['start']
    this.stop = apachectl + ["stop"]
  }

}
