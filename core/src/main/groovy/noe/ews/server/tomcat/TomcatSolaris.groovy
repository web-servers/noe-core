package noe.ews.server.tomcat

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.utils.Cmd
import noe.common.utils.JBFile
import noe.ews.server.ServerEws
import noe.server.ServerAbstract
import noe.server.Tomcat

/**
 * EWS Tomcat server on Solaris
 *
 * @author Jan Stefl   <jstefl@redhat.com>
 *
 */
@Slf4j
class TomcatSolaris extends Tomcat {

  String sbin // where is main sbin directory
  String executables // where are main executable scripts
  String servicesDir // Replacement for sbin for JWS5 and newer. It is set to null on JWS3

  TomcatSolaris(String basedir, version, String tomcatDir = "") {
    super(basedir, version)
    String ewsPrefix = "${basedir}/${ServerEws.getPrefix()}"
    if (ServerEws.extractMajorVersion() > 3) {
      this.tomcatDir = (tomcatDir) ?: "tomcat"
      this.refBasedir = "${ewsPrefix}/tomcat"
      this.basedir = "${ewsPrefix}/${this.tomcatDir}"
      this.executables = binDir
      this.servicesDir = "${this.basedir}/services"
    } else {
      this.tomcatDir = (tomcatDir) ?: "tomcat" + this.version
      this.refBasedir = "${ewsPrefix}/share/tomcat${this.version}"
      this.extrasDir = "${ewsPrefix}/share/extras"
      this.extrasDirs = [this.extrasDir, this.basedir + '/extras']
      this.basedir = "${ewsPrefix}/share/${this.tomcatDir}"
      this.sbin = "${ewsPrefix}/sbin"
      this.executables = sbin
    }

    // please initialize in setDefault() method
    setDefault()
  }

  void setDefault() {
    super.setDefault()
    this.deploymentPath = this.basedir + "/webapps"
    this.binPath = "/bin"
    this.start = Cmd.makeCommand(runContext, this.basedir + binPath + "/startup.sh $processCode") //TODO: Hmm, $processCode for -security as well?
    this.startSecurity = Cmd.makeCommand(runContext, getBinDirFullPath() + '/startup.sh -security')
    this.stop = Cmd.makeCommand(runContext, getBinDirFullPath() + "/shutdown.sh")
    this.catalina = runContext.split().toList() + "${this.basedir}${binPath}/catalina.sh"
    this.daemonStart = new TomcatCommandUtils(this).prepareDaemonCommand("start")
    this.daemonStop = new TomcatCommandUtils(this).prepareDaemonCommand("stop")
    this.configDirs = ["/conf"]
    this.logDirs = ["/logs"]
    this.libDir = this.basedir + "/lib"
    this.binDir = this.basedir + '/bin'
    this.jsvc = "${this.executables}/jsvc"
    this.workDir = "/share/$tomcatDir/work"
    this.confDeploymentPath = this.basedir + "/conf"

    // ! please add new paths like absolute paths
  }

  String createRunContext() {
    def res = super.createRunContext()
    def runTomcatAs = TomcatCommandUtils.getTomcatRunUser()

    // TODO maybe refactor to class Tomcat
    if (runTomcatAs && JBFile.useAdminPrivileges && !res.toString().contains('sudo')) {
      res = "sudo ${super.createRunContext()}"
    }

    return res
  }

  private File getPidFile() {
    File ppid
    if (ServerEws.extractMajorVersion() >= 5) {
      ppid = new File("/var/run/jws${ServerEws.extractMajorVersion()}-tomcat.pid")
    } else {
      ppid = new File("/var/run/${tomcatDir}.pid")
    }
    return ppid
  }

  @Override
  boolean kill() {
    boolean res = super.kill()
    JBFile.delete(pidFile, true)
    return res
  }

  @Override
  long stop(Map conf = [:]) {
    def result = super.stop(conf)
    JBFile.delete(pidFile, true)
    return result
  }

  Integer extractPid() {
    try {
      String pidFromFile = JBFile.read(pidFile).trim().replaceAll('"', '')
      pid = Integer.valueOf(pidFromFile)
    } catch (e) {
      pid = super.extractPid()
    }

    return pid
  }

  /**
   * Create new server instance - from ref. tomcat installation.
   */
  ServerAbstract createNewServerInstance(String id, int offset = DefaultProperties.DEFAULT_SHIFT_PORT_OFFSET) {
    def newInstance = super.createNewServerInstance(id, offset)

    String versionSuffix = version.majorVersion

    updateBinReplaceRegExp('catalina.sh', "sysconfig/tomcat${versionSuffix}", "sysconfig/${id}", true, true)
    if (JBFile.useAdminPrivileges) {
      Cmd.executeSudoCommandConsumeStreams(["cp", "-rp",
                                            new File(ServerEws.getTomcatHome(),"/etc/sysconfig/tomcat${versionSuffix}").getAbsolutePath(),
                                            new File(ServerEws.getTomcatHome(),"/etc/sysconfig/${id}").getAbsoluteFile()])
      JBFile.replace(new File(ServerEws.getTomcatHome(), "/etc/sysconfig/${id}"), "tomcat${versionSuffix}", "${id}")

      Cmd.executeSudoCommandConsumeStreams(["cp", "-rp", "/var/cache/tomcat${versionSuffix}", "/var/cache/${id}"])
    }
    return newInstance
  }

  void startServerSolarisLocalService(conf = [:]) {
    log.debug('Starting Tomcat {} as local service', serverId)

    portsAvailable()
    serverCustomization(conf)

    List executeCmd
    File executeDir

    if(ServerEws.extractMajorVersion() >= 5) {
      executeCmd = ["./jws5-tomcat.init", "start"]
      executeDir = new File(this.servicesDir)
    } else {
      executeCmd = ["./tomcat${version}", "start"]
      executeDir = new File(this.executables)
    }

    Cmd.executeSudoCommandConsumeStreams(executeCmd, executeDir)

    waitForStartComplete()
  }

  void stopServerSolarisLocalService() {
    log.debug('Stopping Tomcat {} local server', serverId)

    if (!isRunning()) {
      log.warn("Server {} is already down.", serverId)
    } else {
      List executeCmd
      File executeDir

      if(ServerEws.extractMajorVersion() >= 5) {
        executeCmd = ["./jws5-tomcat.init", "stop"]
        executeDir = new File(this.servicesDir)
      } else {
        executeCmd = ["./tomcat${version}", "stop"]
        executeDir = new File(this.executables)
      }

      int ret = Cmd.executeSudoCommandConsumeStreams(executeCmd, executeDir).exitValue

      waitForShutdownComplete()
      if (ret == 0) {
        pid = null
      }
    }
  }

  void startServerSolarisGlobalService(conf = [:]) {
    log.debug('Starting Tomcat as global service')

    portsAvailable()
    serverCustomization(conf)

    Cmd.executeSudoCommandConsumeStreams(["/usr/sbin/svcadm", "enable", "tomcat${version}:default"],
            new File('.'))

    waitForStartComplete()
  }

  void stopServerSolarisGlobalService() {
    log.debug('Stopping Tomcat server global service')

    if (!isRunning()) {
      log.warn("Server is already down.")
    } else {
      int ret = Cmd.executeSudoCommandConsumeStreams(["/usr/sbin/svcadm", "disable", "tomcat${version}:default"],
              new File('.')).exitValue

      waitForShutdownComplete()
      if (ret == 0) {
        pid = null
      }
    }
  }
}
