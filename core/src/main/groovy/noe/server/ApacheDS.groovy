package noe.server

import groovy.util.logging.Slf4j
import noe.common.newcmd.CmdBuilder
import noe.common.newcmd.CmdCommand
import noe.common.utils.Cmd
import noe.common.utils.JBFile
/**
 *  Class for manipulting with ApacheDS server
 *    - Pure java LDAP, Kerberos server
 *
 */
@Slf4j
abstract class ApacheDS extends ServerAbstract {
  def apacheDSBin // bin directory
  def kerberosPort

  ApacheDS(String basedir, version) {
    super(basedir, version)
    this.apacheDSBin = "${basedir}${platform.sep}bin"
    this.mainHttpPort = 10389 // ldap port
    this.mainHttpsPort = 10636 // ldaps port
    this.kerberosPort = 60088
    this.logDirs = ["${platform.sep}instances${platform.sep}default${platform.sep}log"]
    this.configDirs = ["${platform.sep}instances${platform.sep}default${platform.sep}conf"]
  }

  static ServerAbstract getInstance(basedir, version) {
    def server
    if (platform.isWindows()) {
      server = new ApacheDSWindows(basedir, version)
    } else {
      server = new ApacheDSUnix(basedir, version)
    }
    return server
  }

  void killAllInSystem() {
    Cmd.killAllInSystem(["apache.directory"])
  }

  /**
   * Start the server
   */
  void start(Map conf = [:]) {
    log.debug('Starting server ApacheDS {}', serverId)

    // clean the mess from previous exceptional situation
    def tmp = System.getProperty('java.io.tmpdir')
    JBFile.delete(new File(tmp, 'changePwdReplayCache.data'))
    JBFile.delete(new File(tmp, 'kdcReplayCache.data'))

    portsAvailable()
    CmdCommand cmdCommand = new CmdBuilder<>(start).setWorkDir(new File(apacheDSBin)).build()
    process = Cmd.startProcess(cmdCommand)
    process.consumeProcessOutput(System.out, System.err)
    waitForStartComplete()
    pid = extractPid()

    log.debug("ApacheDS server ${serverId} started PID: ${pid}")
  }

  /**
   * Stop the server
   */
  long stop(Map conf = [:]) {
    log.debug("Stopping server ApacheDS ${serverId} by killing process tree PID: ${pid}")

    if (!isRunning()) {
      log.debug("ApacheDS server {} is already down.", serverId)
    } else {
      killTree()
      log.debug('ApacheDS server {} stopped', serverId)
    }
    pid = null
    return -1
  }

  void updateConfSetBindAddress(String address) {
  }

  void shiftPorts(int offset) {
  }

  boolean kill() {
    // stop use kill tree
    this.stop()
    return true
  }

}
