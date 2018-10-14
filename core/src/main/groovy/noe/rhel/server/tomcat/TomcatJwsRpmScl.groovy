package noe.rhel.server.tomcat

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties


/**
 * RHEL JWS RPM product since version 5 became Software collection and it needs proper handling.
 */
@Slf4j
class TomcatJwsRpmScl extends TomcatRpm {
  String jwsPlusVersionString = "jws" + DefaultProperties.ewsVersion().getMajorVersion()
  String jwsSclPrefix = "/opt/rh/${jwsPlusVersionString}"
  String baseSclDir = jwsSclPrefix + "/root"

  TomcatJwsRpmScl(String basedir, version, String tomcatDir = "") {
    super(basedir, version)
    this.tomcatDir = "tomcat"
    this.serviceName = "${jwsPlusVersionString}-tomcat"
    this.basedir = baseSclDir + "/usr/share/${this.tomcatDir}"
    this.refBasedir = basedir

    // please initialize in setDefault() method
    setDefault()
  }

  @Override
  void setDefault() {
    super.setDefault()

    this.extrasDir = this.basedir + '/extras'
    this.extrasDirs = [this.extrasDir]
    this.start = ['sudo', 'service', serviceName, 'start']
    this.stop = ['sudo', 'service', serviceName, 'stop']

    this.confDeploymentPath = this.basedir + "/conf"
    this.deploymentPath = this.basedir + "/webapps"
    this.libDir = this.basedir + "/lib"
    this.binDir = this.basedir + "/bin"
    // ! please add new paths like absolute paths
    pinExtractionPaths = [
        "/etc${jwsSclPrefix}/sysconfig/tomcat",
        "/etc${jwsSclPrefix}/tomcat/tomcat.conf"
    ]
    defaultPidFilePath = "/var${jwsSclPrefix}/run/${serviceName}.pid"
  }

  @Override
  void startSecurity() {
    super.startSecurity("tomcat")
  }
}
