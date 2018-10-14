package noe.server

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.NoeContext
import noe.common.newcmd.CmdBuilder
import noe.common.newcmd.CmdCommand
import noe.common.utils.Cmd
import noe.common.utils.IO
import noe.common.utils.JBFile
import noe.common.utils.Library
import noe.common.utils.XmlUtils
import noe.eap.server.as5.AS5Properties
import noe.eap.server.as5.AS5Rhel
import noe.eap.server.as5.AS5Windows

@Slf4j
class AS5 extends ServerAbstract {

  static final String WELCOME_ROOT_CONTEXT = "Manage this JBoss EAP Instance"

  Integer ajpPort /// Port used for communication via AJP, e.g. with load balancer
  Integer jndiPort /// Port used to connect to JMX
  String as5Dir /// name of parent directory
  Integer portOffset ///jboss.socket.binding.port-offset
  String profile
  String workDir
  String nativesDir // location to the natives dir
  String nativeDirLocationEnvName

  List<String> deployConfigDirs
  /// configuration directories in deploy directories, as this directory can be watched for changes it is necessary to handle mainly its backup differently

  AS5(String basedir) {
    super(basedir, '')
    startStopTimeout = 300
    setDefault()
  }

  void setDefault() {
    super.setDefault()
    this.portOffset = (portOffset) ?: 0
    this.mainHttpPort = (mainHttpPort) ?: AS5Properties.MAIN_HTTP_PORT
    this.mainHttpsPort = (mainHttpsPort) ?: AS5Properties.MAIN_HTTPS_PORT
    this.ajpPort = (ajpPort) ?: AS5Properties.AJP_PORT
    this.jndiPort = (jndiPort) ?: AS5Properties.JNDI_PORT

    this.sslCertificate = getDeplSrcPath() + "${platform.sep}ssl${platform.sep}proper${platform.sep}client-cert-key.jks"
    this.sslKey = getDeplSrcPath() + "${platform.sep}ssl${platform.sep}proper${platform.sep}client-cert-key.jks"
    this.keystorePath = getDeplSrcPath() + "${platform.sep}ssl${platform.sep}proper${platform.sep}client-cert-key.jks"
    this.truststorePassword = 'tomcat'
    this.sslKeystorePassword = 'tomcat'
    this.ignoreShutdownPort = true
    this.profile = (profile) ?: AS5Properties.PROFILE
    this.host = (host) ?: AS5Properties.PUBLIC_IP_ADDRESS
  }

  void setProfile(String profile) {
    this.profile = profile
    setDefault()
  }

  static ServerAbstract getInstance(String basedir, String as5Dir = "", NoeContext context = NoeContext.forCurrentContext()) {
    def server
    if (context.consistsOf(['eap5'])) {
      if (platform.isRHEL() || platform.isSolaris()) {
        server = new AS5Rhel(basedir, as5Dir)
      } else if (platform.isWindows()) {
        server = new AS5Windows(basedir, as5Dir)
      } else {
        throw new RuntimeException("Cannot create AS5 server for platform: ${platform.getOsName()}")
      }
    } else {
      throw new IllegalArgumentException("Dude, you probably wanted to set context to eap5, huh?")
    }
    return server
  }

  void createManagementUser(username, password) {
    updateConfByInsertingTextToPositionInFile('props/jmx-console-users.properties', "${username}=${password}", -1)
    updateConfByInsertingTextToPositionInFile('props/jmx-console-roles.properties', "${username}=${AS5Properties.JMX_ROLES}", -1)
  }

  void killAllInSystem() {
    Cmd.killAllInSystem(["jboss-eap-5"])
  }

  /**
   * Wait until the server is started.
   *
   * TODO: Is it better to look into boot log or examine "/" web page?
   */
  void waitForStartComplete(int timeout = startStopTimeout) {
    super.waitForStartComplete(timeout)
    //Gets / (root context)
    Library.verifyUrl(getUrl(), 200, WELCOME_ROOT_CONTEXT)
  }

  /**
   * Create a new server instance.
   */
  ServerAbstract createNewServerInstance(String id, int offset = DefaultProperties.DEFAULT_SHIFT_PORT_OFFSET) {
    return new ServerInstanceCreatorHelper(this).createNewServerInstancePhysicalCopy(offset)
  }

  void shiftPorts(int offset = DefaultProperties.DEFAULT_SHIFT_PORT_OFFSET) {
    updateConfReplaceRegExp("bindingservice.beans/META-INF/bindings-jboss-beans.xml", '<parameter>0</parameter>', '<parameter>' + offset + '</parameter>', false, true)
    if (this.portOffset != offset) {
      this.mainHttpPort += offset
      this.mainHttpsPort += offset
      this.ajpPort += offset
      this.jndiPort += offset
      this.portOffset = offset
    }
  }

  void updateConfSetBindAddress(String address) {
    if (isRunning()) {
      IO.handleOutput ' --- ERROR: Server is running, change request for bound IP address is IGNORED!', IO.LOG_LEVEL_WARN
      return
    }
    IO.handleOutput 'New address for server binding: ' + address
    updateConfReplaceRegExp("bindingservice.beans/META-INF/bindings-jboss-beans.xml", '<parameter>${jboss.bind.address}</parameter>', '<parameter>' + address + '</parameter>', false, true)
    host = address
  }

  /**
   * Updates scanPeriod of Hot Deployment scanner
   * @param scanPeriod time set to hot deployment scanner for checking changes for making hot deployment
   */
  void setScanPeriodForHotDeployments(int scanPeriod) {
    def hdScannerXml = new File(getDeploymentPath(), "hdscanner-jboss-beans.xml")
    def xml = new XmlSlurper(false, false).parse(hdScannerXml)
    xml.bean.find { it.@name == "HDScanner" }.property.find { it.@name == "scanPeriod" }.replaceBody(scanPeriod)

    XmlUtils.writeXmlToFile(hdScannerXml, xml)
  }

  /**
   * This will undeploy the app and clean the work directory of the app
   */
  void undeployByDeleting(String app) {
    super.undeployByDeleting(app)

    File workDirPath = new File(getServerRoot(), workDir)
    IO.handleOutput("workDirPath: ${workDirPath}")
    if (workDirPath.isDirectory()) {
      IO.handleOutput("--- Deleting app's work dir ---")
      ant.delete(includeemptydirs: "true") {
        fileset(dir: workDirPath.getAbsolutePath(), includes: "**/${app}/**")
      }
    }
  }

  @Override
  List<File> retrieveConfFilesByName(String confName) {
    def confFiles = super.retrieveConfFilesByName(confName)
    deployConfigDirs.each { confDir ->
      def confFile = new File(confDir, confName)
      if (confFile.exists()) {
        confFiles.add(confFile)
      }
    }
    return confFiles
  }

  @Override
  void backupConfs() {
    super.backupConfs()
    IO.handleOutput '--- Starting backup all deploy config files ---'
    deployConfigDirs.each { srcDir ->
      def destDir = new File(platform.tmpDir, countBackupConfName(new File(srcDir).name)).absolutePath
      backupConfDir(srcDir, destDir)
    }
    IO.handleOutput '--- Backup all deploy config files successfully done ---'
  }

  @Override
  void restoreConfs() {
    super.restoreConfs()
    IO.handleOutput '--- Starting restore of of all deploy config files ---'
    deployConfigDirs.each { destDir ->
      def srcDir = new File(platform.tmpDir, countBackupConfName(new File(destDir).name)).absolutePath
      restoreConfDir(srcDir, destDir)
    }
    IO.handleOutput '--- Restoring all deploy config files successfully done ---'
  }

  @Override
  void archiveConfs(String testName, String serverId = '') {
    super.archiveConfs(testName, serverId)
    IO.handleOutput '--- Starting archiving of all deploy conf dir files ---'

    def s = platform.sep
    def targetDir
    def simpleServerName = this.getClass().name + '-' + version + "${s}${serverId}"
    def simpleConfDir
    def reportDir = new File(Library.getRootPath(), "${s}target${s}jboss-reports${s}confs-archive")

    deployConfigDirs.each { confDir ->
      File confDirAsFile = new File(confDir)
      if (confDirAsFile.exists() && confDirAsFile.isDirectory()) {
        simpleServerName = simpleServerName.substring(simpleServerName.lastIndexOf('.') + 1)
        targetDir = new File(reportDir, "${testName}${s}${simpleServerName}${s}${confDirAsFile.name}")
        // Creates a directory. Also non-existent parent directories are created, when necessary. Does nothing if the directory already exist.
        ant.mkdir(dir: targetDir.getAbsolutePath())
        JBFile.copy(new File(confDir), targetDir, false)
      }
    }

    IO.handleOutput '--- Archiving of all deploy conf files successfully done ---'
  }

  /**
   * The enableNatives logic added in order to allow preparation of natives but at start point to choose whether it should be started with them or not
   * @param conf
   * @param enableNatives if true, appropriate env property will be set to specify where the natives are located otherwise it is not set and it is expected that it is
   * @return
   */
  void start(Map conf = [:], enableNatives = false) {
    log.debug('Starting server {}', serverId)
    portsAvailable()
    serverCustomization(conf)
    def envProps = [NOPAUSE: true]
    if (enableNatives) {
      envProps.put(nativeDirLocationEnvName, nativesDir)
    }
    List startCmd = start + [
        "-Djboss.messaging.ServerPeerID=${portOffset}",
        "-Djboss.jvmRoute=${serverId}",
        "-u", DefaultProperties.UDP_MCAST_ADDRESS,
        "-m", DefaultProperties.JGROUPS_MCAST_PORT,
        "-D${processCode}"
    ]
    log.debug('Start command: ' + startCmd)
    String baseCommand = startCmd[0]
    List<String> arguments = startCmd.subList(1, startCmd.size())
    CmdCommand cmdCommand = new CmdBuilder(baseCommand)
            .addArguments(arguments)
            .setWorkDir(new File(getBinDirFullPath()))
            .build()
    cmdCommand.setEnvProperties(envProps)

    process = Cmd.startProcess(cmdCommand)
    // TODO: Should I pour it to some log, so as not to flood the console...?
    process.consumeProcessOutput(System.out, System.err)
    waitForStartComplete()
    this.pid = extractPid()
    log.debug('Server {} started', serverId)
  }

  @Override
  long stop(Map conf = [native: false]) {
    log.debug('Stopping server {}', serverId)
    if (!isRunning()) {
      log.debug("Server {} is already down.", serverId)
      return 0
    } else {
      long startTime = new Date().getTime()
      def stopCmd = stop + [
          "-s", "${this.host}:${this.jndiPort}",
          "-u", AS5Properties.JMX_USER,
          "-p", AS5Properties.JMX_PASSWORD
      ]
      def shutdownProcess = Cmd.startProcess(new CmdBuilder<>(stopCmd).setWorkDir(new File(getBinDirFullPath())).build())
      shutdownProcess.consumeProcessOutput(System.out, System.err)
      waitForShutdownComplete(120)
      long endTime = new Date().getTime()
      pid = null
      log.debug('Server {} stopped', serverId)
      return endTime - startTime
    }
  }


  String countBackupConfName(confDirName) {
    return "${new File(basedir).parentFile.name}_${confDirName}.bck"
  }

}
