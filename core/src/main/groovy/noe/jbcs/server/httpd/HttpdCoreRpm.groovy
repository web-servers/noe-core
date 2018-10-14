package noe.jbcs.server.httpd

import groovy.transform.TypeChecked
import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.ews.server.httpd.HttpdRpm
/**
 * JBCS RPM Httpd server 
 *
 * @author fgoldefu@redhat.com
 *
 */
@TypeChecked
@Slf4j
class HttpdCoreRpm extends HttpdRpm {
  static String baseSCLdir = DefaultProperties.HTTPD_SCL_ROOT

  HttpdCoreRpm(String basedir, version) {
    super(basedir, version)
    this.serviceName = "jbcs-httpd24-httpd"
    this.basedir = this.baseSCLdir + '/etc/httpd'
    this.refBasedir = this.basedir
    this.configDirs = [this.basedir + '/conf', this.basedir + '/conf.d', this.basedir + '/conf.modules.d']
    this.logDirs = [this.basedir + '/logs']
    this.resConfd = this.basedir + '/conf.d'

    log.debug("Init: basedir:[${this.basedir}] httpdDir:[${this.httpdDir}]")
    setDefault()
  }

  void setDefault() {
    super.setDefault()
    this.start = ['service', serviceName, 'start']  // starting as service
    this.stop = "service ${serviceName} stop"  // stopping as service
    this.apachectl = [this.baseSCLdir + '/usr/sbin/apachectl']
    this.deploymentPath = this.baseSCLdir + "/var/www/html"
    this.confDeploymentPath = this.basedir + '/conf.d'
    this.cgiDeploymentPath = this.baseSCLdir + "/var/www/cgi-bin"
    this.abPath = this.baseSCLdir + '/usr/bin/ab'
    this.htdbmPath = this.baseSCLdir + '/usr/bin/htdbm'
    this.htpasswdPath = this.baseSCLdir + '/usr/bin/htpasswd'
    this.httxt2dbmPath = this.baseSCLdir + '/usr/bin/httxt2dbm'
    this.logresolvePath = this.baseSCLdir + '/usr/bin/logresolve'
    this.apxsPath = this.baseSCLdir + '/usr/sbin/apxs'

  }
  
  File getPidFile() {
    return new File(baseSCLdir + "/var/run/${serviceName}/httpd.pid")
  }
}
