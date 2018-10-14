package noe.server

import groovy.util.logging.Slf4j
import noe.common.newcmd.CmdBuilder
import noe.common.newcmd.CmdCommand
import noe.common.utils.Cmd
import noe.common.utils.Library
/**
 * @author Jan Stefl     <jstefl@redhat.com>
 */
@Slf4j
class Sahi extends ServerAbstract {
  String sahiDir

  Sahi(String basedir) {
    super(basedir, '')
    this.sahiDir = "${platform.sep}sahi"
    this.basedir = basedir + "${platform.sep}${this.sahiDir}"
    this.mainHttpPort = 9999
    this.ignoreShutdownPort = true
    this.binPath = "${platform.sep}bin"
    this.start = !platform.isWindows() ? ["./sahi.sh"] : ["sahi.bat"]
    // Workaround for Solaris 10 and HP-UX
    // Solaris 10 does not support export VARIABLE at one line, but sahi.sh contains it
    // HP-UX is not good with relative paths, which are used in sahi.sh)
    if (platform.isSolaris10() || platform.isHP()) {
      def javaHome = Library.getUniversalProperty('JAVA_HOME')
      def sahiClassPath = "${this.basedir}/lib/sahi.jar:${this.basedir}/extlib/rhino/js.jar:${this.basedir}/extlib/apc/commons-codec-1.3.jar"
      def sahiUserdataDirTmp = "${this.basedir}/userdata"
      this.start = ["${javaHome}/bin/java", '-classpath', sahiClassPath, 'net.sf.sahi.Proxy', this.basedir, sahiUserdataDirTmp]
    }

    if (platform.isWindows()) {
      this.processCode = 'sahi'
    } else {
      if (platform.isSolaris()) {
        this.processCode = 'sahi'
      } else {
        this.processCode = 'sahi.Proxy'
      }
    }
  }

  @Override
  void start(Map conf = [:]) {
    log.debug('Starting server {} ', serverId)
    portsAvailable()

    CmdCommand cmdCommand = new CmdBuilder<>(start).setWorkDir(new File(getBinDirFullPath())).build()
    process = Cmd.startProcess(cmdCommand)
    process.consumeProcessOutput(System.out, System.err)
    waitForStartComplete()
    this.pid = extractPid()

    log.trace('Server {} started', serverId)
  }

  @Override
  long stop(Map conf = [:]) {
    log.debug("Stopping Sahi server {}.", serverId)
    if (!isRunning()) {
      log.debug("Sahi server is already down.")
    } else {
      this.killTree()
      waitForShutdownComplete(20)
      log.debug("Sahi server {} stopped.", serverId)
    }
    return -1
  }

  void killAllInSystem() {
    Cmd.killAllInSystem(["sahi"])
  }

  @Override
  void updateConfSetBindAddress(String address) {
    throw new RuntimeException("TODO rework concept of what is and abstract in serverAbstract")
  }

  @Override
  void shiftPorts(int offset) {
    throw new RuntimeException("TODO rework concept of what is and abstract in serverAbstract")
  }
}
