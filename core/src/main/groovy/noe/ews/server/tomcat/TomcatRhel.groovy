package noe.ews.server.tomcat

import groovy.util.logging.Slf4j
import noe.common.utils.Cmd
import noe.ews.server.ServerEws
import noe.server.Tomcat

/**
 * EWS Tomcat server on Rhel
 *
 * @author Jan Stefl   <jstefl@redhat.com>
 *
 */
@Slf4j
class TomcatRhel extends Tomcat {
  TomcatRhel(String basedir, version, tomcatDir = "") {
    super(basedir, version)

    if (ServerEws.extractMajorVersion() > 3) {
      this.tomcatDir = (tomcatDir) ?: "tomcat"
      this.refBasedir = basedir + '/' + ServerEws.getPrefix() + "/tomcat"
    } else {
      this.tomcatDir = (tomcatDir) ?: "tomcat" + this.version
      this.refBasedir = basedir + '/' + ServerEws.getPrefix() + "/tomcat" + this.version
    }
    
    this.basedir = basedir + '/' + ServerEws.getPrefix() + "/${this.tomcatDir}"
    this.extrasDir = basedir + '/' + ServerEws.getPrefix() + "/extras"
    this.extrasDirs = [this.extrasDir, this.basedir + '/extras']

    // please initialize in setDefault() method
    setDefault()
  }

  void setDefault() {
    super.setDefault()
    this.deploymentPath = this.basedir + "/webapps"
    this.javaEEdeploymentPath = this.basedir + "/webapps-javaee"
    this.binPath = "/bin"
    this.start = Cmd.makeCommand(runContext, this.basedir + binPath + "/startup.sh $processCode")
    this.startSecurity = Cmd.makeCommand(runContext, getBinDirFullPath() + '/catalina.sh start -security')
    this.stop = runContext + this.basedir + binPath + "/shutdown.sh"
    this.catalina = runContext.split().toList() + "${this.basedir}${binPath}/catalina.sh"
    this.daemonStart = new TomcatCommandUtils(this).prepareDaemonCommand("start")
    this.daemonStop = new TomcatCommandUtils(this).prepareDaemonCommand("stop")
    this.configDirs = ["/conf"]
    this.confDeploymentPath = this.basedir + '/conf'
    this.logDirs = ["/logs"]
    this.libDir = this.basedir + "/lib"
    this.binDir = this.basedir + "/bin"
    this.jsvc = this.extrasDir + '/jsvc'
    this.workDir = "/work"

    // ! please add new paths like absolute paths
  }

}
