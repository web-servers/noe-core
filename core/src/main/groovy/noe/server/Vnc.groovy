package noe.server

import groovy.util.logging.Slf4j
import noe.common.utils.Cmd
import noe.common.utils.JBFile
import noe.common.utils.Library


@Slf4j
class Vnc extends ServerAbstract {
  String display = ':3'
  String geometry = '1920x1080'

  Vnc(String basedir, version, String display, String geometry) {
    super(basedir, version)
    this.display = display
    this.mainHttpPort = 6000 + display.substring(display.lastIndexOf(":") + 1).toInteger()
    this.geometry = geometry
  }

  @Override
  void start(Map conf = [:]) {
    log.debug("Starting vnc server")
    removeLocks()
    if (Cmd.executeCommand(['vncserver', "${display}", '-geometry', geometry], new File('.')) > 0) {
      throw new RuntimeException("Starting of VNC server failed")
    }
    Library.letsSleep(3000)
    log.debug("Vnc server started")
  }
  
  @Override
  long stop(Map conf = [:]) {
    log.debug("Stoping Vnc server")
    if (!isRunning()) {
      log.debug("VNC Server is already down.")
    } else {
      if (Cmd.executeCommand(['vncserver', '-kill', "${display}"], new File('.')) > 0) {
        throw new RuntimeException("Stopping of VNC server failed")
      }
      Library.letsSleep(1000)
      log.debug("Vnc server stoped")
    }
    return -1
  }

  void killAllInSystem() {
    Cmd.killAllInSystem(["vnc"])
    removeLocks()
  }

  @Override
  void updateConfSetBindAddress(String address) {
    throw new RuntimeException("TODO rework concept of what is and abstract in serverAbstract")
  }

  @Override
  void shiftPorts(int offset) {
    throw new RuntimeException("TODO rework concept of what is and abstract in serverAbstract")
  }

  void removeLocks() {
    def displayAsPartOfLockName = display;
    if (displayAsPartOfLockName.startsWith(":")) {
      displayAsPartOfLockName = displayAsPartOfLockName.substring(1)
    }
    if (!platform.isWindows()) {
      [new File('/tmp', ".X11-unix${platform.sep}X${displayAsPartOfLockName}"), new File('/tmp', ".X${displayAsPartOfLockName}-lock")].each {
        JBFile.delete(it)
      }
    } else {
      [new File(platform.tmpDir, ".X11-unix${platform.sep}X${displayAsPartOfLockName}"), new File(platform.tmpDir, ".X${displayAsPartOfLockName}-lock")].each {
        JBFile.delete(it)
      }
    }
  }
}
