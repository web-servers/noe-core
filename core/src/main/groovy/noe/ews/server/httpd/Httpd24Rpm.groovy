package noe.ews.server.httpd

import groovy.transform.TypeChecked
import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.utils.JBFile

/**
 * Httpd server for JWS 30 RPM installation
 * for RHEL6 and RHEL7
 *
 * @author Libor Fuka   <lfuka@redhat.com>
 *
 */
@TypeChecked
@Slf4j
class Httpd24Rpm extends HttpdRpm {

  Httpd24Rpm(String basedir, version) {
    super(basedir, version)
    this.serviceName = "httpd24"
    this.basedir = "/etc/${serviceName}"

    // please initialize in setDefault() method
    setDefault()
    logDirs.each { JBFile.makeAccessible(new File(getServerRoot() + it)) }
  }

  void setDefault() {
    super.setDefault()
    this.start = ['service', serviceName, 'start']  // starting as service
    this.stop = "service ${serviceName} stop"  // stopping as service
    this.apachectl = ['/usr/sbin/apachectl24']
    this.deploymentPath = "/var/www/${serviceName}"
    this.confDeploymentPath = this.basedir + "/${DefaultProperties.CONF_DIRECTORY}"
    this.confModulesDeploymentPath = this.basedir + "/${DefaultProperties.CONF_MODULES_DIRECTORY}"
    this.cgiDeploymentPath = this.deploymentPath + "/cgi-bin"
    this.abPath = "/usr/bin/ab24"
    this.htdbmPath = "/usr/bin/htdbm24"
    this.htpasswdPath = "/usr/bin/htpasswd24"
    this.httxt2dbmPath = "/usr/sbin/httxt2dbm24"
    this.logresolvePath = "/usr/bin/logresolve24"
    this.apxsPath = "/usr/sbin/apxs24"

  }

}
