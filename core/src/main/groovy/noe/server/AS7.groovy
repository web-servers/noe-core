package noe.server

import com.gargoylesoftware.htmlunit.WebClient
import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.NoeContext
import noe.common.newcmd.CmdBuilder
import noe.common.newcmd.CmdCommand
import noe.common.utils.Cmd
import noe.common.utils.JBFile
import noe.common.utils.Java
import noe.common.utils.Library
import noe.common.utils.VerifyURLBuilder
import noe.common.utils.Version
import noe.eap.server.ServerEap
import noe.eap.server.as7.AS7Cli
import noe.eap.server.as7.AS7HPUX
import noe.eap.server.as7.AS7Properties
import noe.eap.server.as7.AS7Rhel
import noe.eap.server.as7.AS7Rpm
import noe.eap.server.as7.AS7Solaris
import noe.eap.server.as7.AS7Windows
import noe.eap.utils.CLILib
import noe.eap.utils.XSLTStaticLib
import noe.server.jk.WorkerServer

import javax.net.ssl.SSLSocketFactory

/**
 * AS7 Class
 * TODO: refactor common attributes for standalone/domain to sth like AS7Abstract
 * TODO: We need to take a look at the Domain mode with Libor.
 */
@Slf4j
class AS7 extends ServerAbstract implements WorkerServer {

  public static final String WELCOME_ROOT_CONTEXT = "Welcome to"
  public static final String ADDITIONAL_PARAMETERS_LIST = "ADDITIONAL_PARAMETERS_LIST"
  public static final String ADDITIONAL_ENV_VARIABLES_LIST = "ADDITIONAL_ENV_VARIABLES_LIST"

  AS7Cli as7Cli // cliClient abstraction for running cli commands and generating cli
  Version eapVersion // eapVersion

  Integer ajpPort = null /// Port used for communication via AJP, e.g. with load balancer
  Integer managementNativePort = null
  Integer managementHttpPort = null
  Integer managementHttpsPort = null
  String as7Dir /// name of parent directory
  String cfgHost = null
  Integer portOffset = null ///jboss.socket.binding.port-offset
  String modulesDir
  String configFile = null
  String cliPath = null
  boolean welcomeHandlerOn = true

  /**
   * Construct new AS7 instance
   * @param basedir Workspace directory in which the as7Dir is placed
   * @param as7Dir Root directory of this AS7 instance
   */
  AS7(String basedir, String as7Dir = "") {
    super(basedir, '')
    this.as7Dir = (as7Dir) ?: ServerEap.getPrefix()
    this.basedir = basedir + platform.sep + "${this.as7Dir}"
    /* refBasedir denotes the origin of all AS7 instances. Since a case, where unzipped directory has different name
    than zip may occur, "build.zip.root.dir" property is used to determine name of this and further prevent errors
    caused by non-existent source directory when creating new AS7 instances.*/
    this.refBasedir = basedir + platform.sep + ServerEap.getPrefix()
    setDefault()
  }

  void setDefault() {
    super.setDefault()
    this.eapVersion = (eapVersion) ?: DefaultProperties.eapVersion()
    this.version = this.eapVersion
    this.portOffset = (portOffset) ?: 0
    this.ajpPort = (ajpPort) ?: AS7Properties.AJP_PORT
    this.managementHttpPort = (managementHttpPort) ?: AS7Properties.MANAGEMENT_HTTP_PORT
    this.managementHttpsPort = (managementHttpsPort) ?: AS7Properties.MANAGEMENT_HTTPS_PORT
    this.managementNativePort = (managementNativePort) ?: AS7Properties.MANAGEMENT_NATIVE_PORT

    this.mainHttpPort = (mainHttpPort) ?: AS7Properties.MAIN_HTTP_PORT
    this.mainHttpsPort = (mainHttpsPort) ?: AS7Properties.MAIN_HTTPS_PORT

    this.sslCertificate = getDeplSrcPath() + "${platform.sep}ssl${platform.sep}proper${platform.sep}client-cert-key.jks"
    this.sslKey = getDeplSrcPath() + "${platform.sep}ssl${platform.sep}proper${platform.sep}client-cert-key.jks"
    this.keystorePath = getDeplSrcPath() + "${platform.sep}ssl${platform.sep}proper${platform.sep}client-cert-key.jks"
    this.truststorePassword = 'tomcat'
    this.sslKeystorePassword = 'tomcat'
    this.ignoreShutdownPort = true
    this.cfgHost = (cfgHost) ?: AS7Properties.MANAGEMENT_IP_ADDRESS
    this.host = (host) ?: AS7Properties.PUBLIC_IP_ADDRESS

    this.configFile = (configFile) ?: AS7Properties.STANDALONE_CONFIG_FILE

    this.binPath = "${platform.sep}bin"
    this.binDir = this.basedir +  this.binPath
    this.deploymentPath = this.basedir + "${platform.sep}standalone${platform.sep}deployments"
    this.cliPath = this.binPath
    configureCliClient()


    this.configDirs = configDirs ?: [
        "${platform.sep}standalone${platform.sep}configuration"
    ]
    this.logDirs = logDirs ?: ["${platform.sep}standalone${platform.sep}log"]
    this.libDir = libDir ?: this.basedir + "${platform.sep}standalone${platform.sep}lib"
    this.modulesDir = this.basedir + "${platform.sep}modules"
    // ! please add new paths like absolute paths

    /**
     * You might get an idea like: "Oh, why don't we just execute standalone.sh and then hit ^C, (char)3, or do .waitForOrKill / .destroy
     * on the Process instance?"
     * It won't work, because you get these two processes:
     *    root     17352  0.0  0.0 106100  1356 pts/0    S+   04:39   0:00 /bin/sh ./standalone.sh -c standalone-ha.xml -D1782555599
     *    root     17396 57.8  4.1 3734124 205172 pts/0  Sl+  04:39   0:12 /root/jdk1.7.0_last//bin/java -D[Standalone] -server -XX:+UseCompressedOops ...yada yada yada...-c standalone-ha.xml -D1782555599
     * Whereas the aforementioned approach will gracefully terminate only the first one [17352], while the other one, the actual AS7 keeps runnig...
     * I think the reason is that we are not executing the standalone.sh from a proper terminal.
     */
    configureJBossCliStop()

    log.debug("cfgHost is $cfgHost whereas AS7Properties.MANAGEMENT_IP_ADDRESS is ${AS7Properties.MANAGEMENT_IP_ADDRESS}" +
            " and AS7Properties.PUBLIC_IP_ADDRESS is ${AS7Properties.PUBLIC_IP_ADDRESS}.")
  }

  static ServerAbstract getInstance(basedir, as7Dir = "", NoeContext context = NoeContext.forCurrentContext()) {
    def server
    if (context.areInSingleGroup(['eap6', 'rpm'])) {
      if (platform.isRHEL()) {
        server = new AS7Rpm(basedir, as7Dir)
      } else {
        throw new RuntimeException("Cannot create AS7 RPM server for platform: ${platform.getOsName()} for context eap6-rpm")
      }
    } else if (context.consistsOf(['eap6'])) {
      if (platform.isRHEL()) {
        server = new AS7Rhel(basedir, as7Dir)
      } else if (platform.isWindows()) {
        server = new AS7Windows(basedir, as7Dir)
      } else if (platform.isSolaris()) {
        server = new AS7Solaris(basedir, as7Dir)
      } else if (platform.isHP()) {
        server = new AS7HPUX(basedir, as7Dir)
      } else {
        throw new RuntimeException("Cannot create AS7 server for platform: ${platform.getOsName()}")
      }
    } else {
      throw new IllegalArgumentException("Dude, you probably wanted to set context to eap6, huh?")
    }
    return server
  }

  @Override
  public void start(Map conf = [:]) {
    log.debug('Starting server {}', serverId)
    portsAvailable()
    serverCustomization(conf)

    // Lets prepare custom environment variables for executed server:
    Map envVariables = [NOPAUSE: true];
    if (conf != null && conf.containsKey(ADDITIONAL_ENV_VARIABLES_LIST)) {
        // Custom environment variables should be passed in 'conf' map instance again as a map of keys and values.
        envVariables = Library.mapUnion(envVariables, conf.get(ADDITIONAL_ENV_VARIABLES_LIST))
    }

    List additionalParams = []
    if (conf != null && conf.containsKey(ADDITIONAL_PARAMETERS_LIST)) {
      additionalParams = (List) conf.get(ADDITIONAL_PARAMETERS_LIST)
    }

    String baseCommand = start[0]
    List<String> arguments = start.subList(1, start.size())
    CmdCommand cmdCommand = new CmdBuilder(baseCommand)
            .addArguments(arguments)
            .addArguments(additionalParams)
            .setWorkDir(new File(getBinDirFullPath()))
            .build()
    cmdCommand.setEnvProperties(envVariables)

    final String javaHome = Library.getUniversalProperty("server.java.home","")
    if (javaHome) {
      process = startWithJavaHome(javaHome, cmdCommand)
    } else {
      log.debug("Starting with default JAVA_HOME.")
      process = Cmd.startProcess(cmdCommand)
    }
    log.debug('Start command: "{}"', cmdCommand)
    process.consumeProcessOutput(System.out, System.err)
    if (startInAdminOnlyOrSuspendMode(additionalParams)) {
      waitForStartComplete(startStopTimeout, managementHttpPort)
    } else {
      waitForStartComplete(startStopTimeout)
    }

    this.pid = extractPid()
    log.debug('--- Server started {} ---', serverId)
  }

  private Process startWithJavaHome(final String serverJavaHome, final CmdCommand cmdCommand) {
    log.debug("You are trying to start the server with the following Java Home: $serverJavaHome. Continuing.")
    final String origJavaHome = Cmd.props["JAVA_HOME"]
    Process process = null
    try {
      Cmd.props["JAVA_HOME"] = serverJavaHome
      process = Cmd.startProcess(cmdCommand)
    } finally {
      Cmd.props["JAVA_HOME"] = origJavaHome
    }
    return process
  }

  /**
   * used to define config file, if you need it to update also the start command, you should override it.
   * @param profileXML - name of the config file to use by the server
   */
  void setProfile(String profileXML) {
    this.configFile = profileXML
  }

  /**
   * Checks whether the additional params contain option which means that server should start only in admin-only or suspended mode
   * => only management api will become fully available
   *
   * @return true if and only if the provided params contain attribute defining that the server should start in admin-only or suspended mode
   *
   */
  private boolean startInAdminOnlyOrSuspendMode(List additionalParamsToCheck) {
    if (additionalParamsToCheck.contains('--admin-only') || additionalParamsToCheck.contains('--start-mode=admin-only')
        || additionalParamsToCheck.contains('--start-mode=suspend')) {
      return true
    }

    // parameter of --start-mode can be also provided as additional parameter
    int idx = additionalParamsToCheck.indexOf('--start-mode')
    if (idx >= 0 && ((idx + 1) < additionalParamsToCheck.size())) {
        String value = additionalParamsToCheck.get(idx+1)
        if (value.equals('admin-only') || value.equals('suspend')) {
            return true
        }
    }
    return false
  }

  @Override
  public long stop(Map conf = [:]) {
    log.debug('Stopping server {} with PROCESSCODE: {}', serverId, processCode)
    if (!isRunning()) {
      log.warn("Server is already down.")
      return 0
    } else {
      long startTime = new Date().getTime()
      def cliProcess = Cmd.startProcess(new CmdBuilder<>(stop).setWorkDir(new File(getBinDirFullPath())).build())
      cliProcess.consumeProcessOutput(System.out, System.err)
      waitForShutdownComplete(startStopTimeout, managementHttpPort)
      long endTime = new Date().getTime()
      pid = null
      log.debug('Server {} stopped', serverId)
      return endTime - startTime
    }
  }

  void killAllInSystem() {
    Cmd.killAllInSystem(["jboss-modules.jar", "standalone.bat", "jboss-cli.bat", ServerEap.prefix])
  }

  /**
   * Check if the server is not down already
   */
  boolean isRunning() {
    if (pid) {
      return Cmd.pidExists(pid)
    } else {
      return Library.checkTcpPort(host, mainHttpPort) || Library.checkTcpPort(host, managementHttpPort)
    }
  }

  boolean waitForDeployComplete(String appName, long timeout = 10000) {
    while (timeout > 0) {
      timeout += new Date().getTime()
      if (as7Cli.runArbitraryCommand("/deployment=${appName}:read-resource").exitValue == 0) {
        return true
      }
      Library.letsSleep(1000)
      timeout -= new Date().getTime()
    }
    if (as7Cli.runArbitraryCommand("/deployment=${appName}:read-resource").exitValue == 0) {
      return true
    }
    return false
  }

  /**
   * Wait until the server is started.
   *
   */
  void waitForStartComplete(int timeout = startStopTimeout, int port = mainHttpPort) {
    super.waitForStartComplete(timeout, port)
    if (mainHttpPort == mainHttpsPort) {
      //If there is only HTTPS connector configured, we assume mainHttpPort and mainHttpsPort being the same.
      File f = new File(keystorePath)
      URL certUrl = f.toURI().toURL()
      WebClient webClient = new WebClient()
      webClient.getOptions().setRedirectEnabled(true)
      webClient.getOptions().setPrintContentOnFailingStatusCode(true)
      webClient.getOptions().setUseInsecureSSL(true)
      if (Java.isJdk1xOrHigher("1.7")) {
        webClient.getOptions().setSSLClientProtocols(["TLSv1.2"] as String[])
      } else {
        webClient.getOptions().setSSLClientProtocols(["TLSv1.1"] as String[])
      }
      webClient.getOptions().setSSLClientCipherSuites(((SSLSocketFactory) SSLSocketFactory.getDefault()).getSupportedCipherSuites())
      webClient.getOptions().setSSLClientCertificate(certUrl, sslKeystorePassword, "jks")
      if (welcomeHandlerOn) {
        if (!VerifyURLBuilder.verifyURL {
          it.url super.getUrl('', true)
          it.code 200
          it.content WELCOME_ROOT_CONTEXT
          it.timeout timeout * 1000
          it.webClient webClient
          it.swallowIOExceptions true
          // WebClient by default uses 90 seconds, which can result in only single try ending with read timeout exception,
          // lowering the connection timeout to make sure that with 60 seconds timeout there is at least one retry
          it.webConnectionTimeout 25000
        }) {
          throw new RuntimeException("Server start problem - $serverId is not started.")
        }
      }
    } else if (welcomeHandlerOn) {
      if (!VerifyURLBuilder.verifyURL {
        it.url super.getUrl('', false, port)
        it.code 200
        it.content WELCOME_ROOT_CONTEXT
        it.timeout timeout*1000
        it.swallowIOExceptions true
        it.webConnectionTimeout 25000
      }) {
        throw new RuntimeException("Server start problem - $serverId is not started.")
      }
    }
    if (!waitManagementApiReady(timeout)) {
      throw new RuntimeException("Server start problem - $serverId is not started.")
    }

  }

  /**
   * Checks if the Cli console successfully confirm that server is started
   * @param timeout is seconds
   * @return
   */
  boolean waitManagementApiReady(int timeout = 60) {
    long miliTimeout = timeout * 1000
    while (miliTimeout > 0) {
      miliTimeout += new Date().getTime()
      if (CLILib.managementApiReady(this)) {
        return true
      }
      Library.letsSleep(1000)
      miliTimeout -= new Date().getTime()
    }
    return CLILib.managementApiReady(this)
  }

  /**
   * Create a new server instance.
   */
  ServerAbstract createNewServerInstance(String id, int offset = DefaultProperties.DEFAULT_SHIFT_PORT_OFFSET) {
    return new ServerInstanceCreatorHelper(this).createNewServerInstancePhysicalCopy(offset)
  }

  /**
   * Physical removing of the actual server instance.
   */
  boolean deleteCurrentInstance() {
    return JBFile.delete(new File(this.basedir))
  }

  void updateConfSetBindAddress(String address) {
    if (isRunning()) {
      log.warn(' --- ERROR: Server is running, change request for bound IP address is IGNORED!')
      return
    }
    log.debug('New address for server binding: ' + address)
    configDirs.each { configDir ->
      File configDirAsFile = new File(getServerRoot(), configDir)
      File configAsFile = new File(configDirAsFile, configFile)
      if (configAsFile.exists()) {
        XSLTStaticLib.changeIpAddresses([MANAGEMENT_IP_ADDRESS: address, PUBLIC_IP_ADDRESS: address, PRIVATE_IP_ADDRESS: address],
                configAsFile.absolutePath)
      }
    }
    cfgHost = host = address
    configureCliClient()
    configureJBossCliStop()
  }

  void createManagementUser(String username, String hashpassword) {
    updateConfReplaceRegExp('mgmt-users.properties', '#admin=2a0923285184943425d1f53ddd58ec7a', "${username}=${hashpassword}", false, true)
  }

  void shiftPorts(int offset = DefaultProperties.DEFAULT_SHIFT_PORT_OFFSET) {
    // TODO: Extend to support other profiles
    // TODO: Extend to support Domain configurations...
    // TODO: Replace with proper XSLT manipulation!
    updateConfReplaceRegExp(configFile, 'jboss.socket.binding.port-offset:[^}]*', 'jboss.socket.binding.port-offset:' + offset)
    if (this.portOffset != offset) {
      this.mainHttpPort += offset
      this.mainHttpsPort += offset
      this.ajpPort += offset
      this.managementHttpPort += offset
      this.managementHttpsPort += offset
      this.managementNativePort += offset
      this.portOffset = offset
    }

    log.trace("portOffset: $portOffset")
    log.trace("mainHttpPort: $mainHttpPort")
    log.trace("mainHttpsPort: $mainHttpsPort")
    log.trace("ajpPort: $ajpPort")
    log.trace("managementHttpPort: $managementHttpPort")
    log.trace("managementHttpsPort: $managementHttpsPort")
    log.trace("managementNativePort: $managementNativePort")
    setDefault()
  }

  /**
   * Deploy by copying +
   * removes appName.undeployed marker if present.
   */
  @Override
  void deployByCopying(String appPath, Boolean explodeFirst = false, String contextName = '', Boolean zipAsWar = false) {
    log.debug("Deploying by copying appPath:${appPath} with contextName:${contextName}")
    File fullDeplSrcPath = new File(getDeplSrcPath(), appPath)
    String destDirName = contextName ?: fullDeplSrcPath.getName()
    log.trace("destDirName before check:${destDirName}")
    // With AS7, the folder name must end with .ear, .jar, .rar, .sar or .war
    if (!(destDirName.endsWith(".ear") || destDirName.endsWith(".jar") || destDirName.endsWith(".war") || destDirName.endsWith(".sar") || destDirName.endsWith(".rar"))) {
      // I choose .war in this emergency situation...
      destDirName = destDirName + ".war"
    }
    log.trace("destDirName after check:${destDirName}")
    // Actually, IMHO, there is no need for exploding archives for AS7. Unlike with tomcat. I will take the liberty of fixing explodeFirst = false.
    super.deployByCopying(appPath, false, destDirName, zipAsWar)
    File scannerHintFilePath = new File(getDeploymentPath(), destDirName + ".undeployed")
    if (scannerHintFilePath.exists()) {
      JBFile.delete(scannerHintFilePath)
    }
    // Meh, we have to create appName.dodeploy scanner marker in order to deploy exploded war/ear
    if (explodeFirst || fullDeplSrcPath.isDirectory()) {
      log.debug("Creating .dodeploy!")
      scannerHintFilePath = new File(getDeploymentPath(), destDirName + ".dodeploy")
      scannerHintFilePath.createNewFile()
    }
  }

  void undeployByDeleting(String appName) {
    super.undeployByDeleting(appName)
    //AS7 Specific clean-up, yes we use .war with AS7.
    AS7Properties.DEPLOYMENT_SCANNER_MARKER_FILES.each { postfix ->
      File scannerHintFilePath = new File(getDeploymentPath(), appName + ".war" + postfix)
      if (scannerHintFilePath.exists()) {
        log.debug("Deleting: " + scannerHintFilePath.toString())
        JBFile.delete(scannerHintFilePath)
      }
    }
  }


  /**
   * Get user, under whom run AS7 server.
   */
  String loadRunAs() {
    def res = ''
    if (!(res = Library.getUniversalProperty('as7.run.as', ''))) {
      res = super.loadRunAs()
    }
    return res
  }

  /**
   * This string will be passed directly to the --commands="your string"
   *
   * @param command , MIND THE '"' and escape them...
   * @return return code
   * @deprecated use rather as7Cli directly, added here for backward compatibility
   */
  Map runArbitraryCommand(final String command) {
    return as7Cli.runArbitraryCommand(command)
  }

  /**
   * Configuring as7 cli client
   */
  void configureCliClient() {
    def cliPort = managementNativePort

    if (eapVersion.getMajorVersion() > 6) {
      cliPort = managementHttpPort
    }
    as7Cli = new AS7Cli(eapVersion, this.cfgHost, cliPort, new File(getBinDirFullPath(), "jboss-cli." + platform.getScriptSuffix()));
  }

  /**
   * Setting this.stop property for EAP6 with current properties this.cfgHost and this.managementNativePort
   */
  void configureJBossCliStop() {
    this.stop = as7Cli.generateCmdForSingleCliCommand(":shutdown")
  }

}
