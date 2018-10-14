package noe.ews.server.httpd

import groovy.transform.TypeChecked
import groovy.util.logging.Slf4j
import noe.common.utils.JBFile

/**
 * Httpd server for EWS 2.1 RPM installation
 * for RHEL7 only
 *
 * @author Libor Fuka   <lfuka@redhat.com>
 *
 */
@TypeChecked
@Slf4j
class Httpd22Rpm extends HttpdRpm {

  Httpd22Rpm(String basedir, version) {
    super(basedir, version)
    this.serviceName = "httpd22"
    this.basedir = "/etc/${serviceName}"

    // please initialize in setDefault() method
    setDefault()
    logDirs.each { JBFile.makeAccessible(new File(getServerRoot() + it)) }
  }

  void setDefault() {
    super.setDefault()
    this.start = ['service', serviceName, 'start']  // starting as service
    this.stop = "service ${serviceName} stop"  // stopping as service
    this.apachectl = ['/usr/sbin/apachectl22']
    this.deploymentPath = "/var/www/${serviceName}"
    this.confDeploymentPath = this.basedir + '/conf.d'
    this.cgiDeploymentPath = this.deploymentPath + "/cgi-bin"
    this.abPath = "/usr/bin/ab22"
    this.htdbmPath = "/usr/bin/htdbm22"
    this.htpasswdPath = "/usr/bin/htpasswd22"
    this.httxt2dbmPath = "/usr/sbin/httxt2dbm22"
    this.logresolvePath = "/usr/bin/logresolve22"
    this.apxsPath = "/usr/sbin/apxs22"

    this.httpdevent = "sudo ./httpd22.event22"
    this.httpdworker = "sudo ./httpd22.worker22"
  }

}
