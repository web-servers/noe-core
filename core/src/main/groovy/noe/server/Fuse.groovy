package noe.server

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.NoeContext
import noe.common.newcmd.CmdBuilder
import noe.common.utils.Cmd
import noe.common.utils.Library
import noe.common.utils.Version
import noe.fuse.server.FuseProperties
import noe.fuse.server.FuseRHEL
import noe.fuse.server.FuseSolaris
import noe.fuse.server.FuseWindows

import java.util.concurrent.TimeUnit

/**
 *
 * @author tplevko
 */
@Slf4j
public class Fuse extends ServerAbstract {

  int jndiPort /// Port used to connect to JMX
  Version fuseVersion
  String fuseType

  Fuse(String basedir, String version) {
    super(basedir, version)
    setDefault()
  }

  void setDefault() {
    super.setDefault()

    this.fuseVersion = new Version(Library.getUniversalProperty('fuse.version'))
    this.fuseType = Library.getUniversalProperty('fuse.type')

    this.mainHttpPort = (mainHttpPort) ?: FuseProperties.MAIN_HTTP_PORT
    this.jndiPort = (jndiPort) ?: FuseProperties.JNDI_PORT
    this.host = FuseProperties.PUBLIC_IP_ADDRESS
    this.configDirs = [
            "${platform.sep}etc"
    ]
  }

  static ServerAbstract getInstance(String basedir, String fuseDir = "", NoeContext context = NoeContext.forCurrentContext()) {
    def server
    if (context.consistsOf(['fuse'])) {
      if (platform.isRHEL()) {
        server = new FuseRHEL(basedir, fuseDir)
      } else if (platform.isWindows()) {
        server = new FuseWindows(basedir, fuseDir)
      } else if (platform.isSolaris()) {
        server = new FuseSolaris(basedir, fuseDir)
      }  else {
        throw new RuntimeException("Cannot create Fuse server for platform: ${platform.getOsName()}")
      }
    } else {
      throw new IllegalArgumentException("Sorry, you probably wanted to set context to fuse, am I right?")
    }
    return server
  }

  void start(Map conf = [:]) {
    log.info('Starting fuse server {}', serverId)
    portsAvailable()
    log.info('Start command: {}', start)

    process = Cmd.startProcess(new CmdBuilder<>(start).setWorkDir(new File(getBinDirFullPath())).build())
    process.consumeProcessOutput(System.out, System.err)
    waitForStartComplete(300)
    this.pid = extractPid()

    log.info('Server {} started', serverId)
  }

  @Override
  Long extractPid() {
    File instancePropsFile = new File(basedir, "instances${platform.sep}instance.properties")
    if (instancePropsFile.exists()) {
      int serverPid = retrievePidFromInstancesPropsFile(instancePropsFile)
      long endTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(2)

      // as the actual pid being written to the file can take couple of ms.
      // also checking that the actual pid exists as we could retrieve pid for instance of server which was previously killed and
      // the value is still not updated
      while ((serverPid <= 0 || !Cmd.pidExists(serverPid)) && System.currentTimeMillis() < endTime) {
        Library.letsSleep(42)
        serverPid = retrievePidFromInstancesPropsFile(instancePropsFile)
      }
      if (serverPid <= 0 || !Cmd.pidExists(serverPid)) {
        log.warn("Failed to retrieve valid pid from server specific file, lets try to do it based on identifier from process list")
        return super.extractPid()
      } else {
        return serverPid
      }
    } else {
      log.warn("server specific file containing pid doesn't exist, lets try to do it based on identifier from process list")
      return super.extractPid()
    }
  }

  private int retrievePidFromInstancesPropsFile(File instancePropsFile) {
    if (!instancePropsFile.exists()) {
      throw new IllegalArgumentException("${instancePropsFile} doesn't exist => can't retrieve pid file from it")
    }
    log.debug("Retrieving pid directly from ${instancePropsFile} file")
    List<String> lines = instancePropsFile.text.readLines()
    String lineWithPid = lines.find {line -> line.contains("item.0.pid")}
    int serverPid = Integer.parseInt(lineWithPid.split("=")[1].trim())
    return serverPid
  }

  /**
   * Creates management user with user name, password and roles as specified in the FuseProperties
   *
   * @return
   */
  void createManagementUser() {
    log.info("Adding new management user")
    updateConfByInsertingTextToPositionInFile("${platform.sep}${FuseProperties.USER_CONFIG_FILE}",
         "\n${FuseProperties.ADMIN_NAME}=${FuseProperties.ADMIN_PASSWORD},${FuseProperties.ADMIN_ROLES}", -1)
  }

  long stop(Map conf = [:]) {
    log.info('Stopping server {}', serverId)
    if (!isRunning()) {
      log.info("Server is already down.")
      return 0
    } else {
      long startTime = new Date().getTime()
      def cliProcess = Cmd.startProcess(new CmdBuilder<>(stop).setWorkDir(new File(getBinDirFullPath())).build())
      log.info("the command output: " + cliProcess)

      waitForShutdownComplete()
      long endTime = new Date().getTime()
      pid = null
      log.info('Server stopped {}', serverId)
      return endTime - startTime
    }
  }

  /**
   * Wait until the server is started.
   */
  void waitForStartComplete(int timeout = 300) {
    super.waitForStartComplete(timeout)
    Library.verifyUrl(getUrl(), 200)
  }

  void killAllInSystem() {
    if (!platform.isWindows()) {
      Cmd.killAllInSystem(["fuse"])
    } else {
      // MS Windows
      log.info("Killing Fuse server on MS Windows...")
      if (!isRunning()) {
        log.info("Fuse server is already down.")
      } else {
        this.killTree()
        waitForShutdownComplete(20)
        log.info("Fuse server killed on MS Windows...")
      }
    }
  }

  void updateConfSetBindAddress(String address) {
    throw new RuntimeException("TODO: Not required yet")
  }


  void shiftPorts(int offset = DefaultProperties.DEFAULT_SHIFT_PORT_OFFSET) {
    throw new RuntimeException("TODO: Not required yet")
  }
}
