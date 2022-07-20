package noe.server

import com.gargoylesoftware.htmlunit.WebClient
import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.NoeContext
import noe.common.newcmd.CmdBuilder
import noe.common.newcmd.CmdCommand
import noe.common.utils.Cmd
import noe.common.utils.IO
import noe.common.utils.JBFile
import noe.common.utils.Library
import noe.common.utils.Version
import noe.eap.server.ServerEap
import noe.eap.server.as7.AS7Cli
import noe.eap.server.as7.AS7Properties
import noe.eap.server.as7.domain.AS7DomainHPUX
import noe.eap.server.as7.domain.AS7DomainRhel
import noe.eap.server.as7.domain.AS7DomainSolaris
import noe.eap.server.as7.domain.AS7DomainWindows
import sun.reflect.generics.reflectiveObjects.NotImplementedException
/**
 * Created by rhatlapa on 11/4/14.
 * TODO: there are too many common parts for AS7 and AS7Domain, create common abstraction to contain common stuff instead of duplicating so much code
 */
@Slf4j
class AS7Domain extends ServerAbstract {

  def static final WELCOME_ROOT_CONTEXT = '.*(Welcome to|JBoss Management|HAL Management Console).*'

  AS7Cli as7Cli // cliClient abstraction for running cli commands and generating cli
  Version eapVersion // eap version

  Integer ajpPort = null /// Port used for communication via AJP, e.g. with load balancer
  Integer managementNativePort = null
  Integer managementHttpPort = null
  Integer managementHttpsPort = null
  String as7Dir /// name of parent directory
  String cfgHost = null
  Integer portOffset = null ///jboss.socket.binding.port-offset
  protected boolean portOffsetNeverUpdated = true
  String modulesDir
  String configFile = null
  String cliPath = null
  String domainHost = null

  public static final Map<String, Integer> defaultServerOffsets = ['server-one': 0, 'server-two': 150, 'server-three': 250]
  Map<String, Integer> serverOffsets = null


  AS7Domain(String basedir, String as7Dir = "") {
    super(basedir, '')
    startStopTimeout = 120
    this.as7Dir = (as7Dir) ?: ServerEap.getPrefix()
    this.basedir = basedir + platform.sep + "${this.as7Dir}"
    this.refBasedir = basedir + platform.sep + ServerEap.getPrefix()
    setDefault()
  }

  void setDefault() {
    super.setDefault()

    this.eapVersion = (eapVersion) ?: DefaultProperties.eapVersion()
    this.portOffset = (portOffset) ?: 0
    serverOffsets = (serverOffsets) ?: ([:] << defaultServerOffsets)
    this.ajpPort = (ajpPort) ?: AS7Properties.AJP_PORT
    this.managementHttpPort = (managementHttpPort) ?: AS7Properties.MANAGEMENT_HTTP_PORT
    this.managementHttpsPort = (managementHttpsPort) ?: AS7Properties.MANAGEMENT_HTTPS_PORT
    this.managementNativePort = (managementNativePort) ?: AS7Properties.MANAGEMENT_NATIVE_PORT

    this.mainHttpPort = this.managementHttpPort
    this.mainHttpsPort = this.managementHttpsPort

    this.sslCertificate = getDeplSrcPath() + "${platform.sep}ssl${platform.sep}proper${platform.sep}client-cert-key.jks"
    this.sslKey = getDeplSrcPath() + "${platform.sep}ssl${platform.sep}proper${platform.sep}client-cert-key.jks"
    this.keystorePath = getDeplSrcPath() + "${platform.sep}ssl${platform.sep}proper${platform.sep}client-cert-key.jks"
    this.truststorePassword = 'tomcat'
    this.sslKeystorePassword = 'tomcat'
    this.ignoreShutdownPort = true
    this.cfgHost = (cfgHost) ?: AS7Properties.MANAGEMENT_IP_ADDRESS
    this.host = (host) ?: AS7Properties.PUBLIC_IP_ADDRESS
    this.configFile = (configFile) ?: AS7Properties.DOMAIN_CONFIG_FILE

    this.binDir = basedir + "${platform.sep}bin"
    // I am not aware of having deploymentPath in domain like in standalone mode
    // this.deploymentPath = this.basedir + "${platform.sep}domain${platform.sep}deployments"
    this.binPath = "${platform.sep}bin"
    this.cliPath = this.binPath
    configureCliClient()


    this.configDirs = [
        "${platform.sep}domain${platform.sep}configuration"
    ]
    this.logDirs = ["${platform.sep}domain${platform.sep}log"]
    this.libDir = this.basedir + "${platform.sep}domain${platform.sep}lib"
    this.modulesDir = this.basedir + "${platform.sep}modules"
    // ! please add new paths like absolute paths

    // TODO: add support for other controllers than master
    this.domainHost = (domainHost) ?: "master"
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


    IO.handleOutput "TIGITAG: cfgHost is $cfgHost whereas AS7Properties.MANAGEMENT_IP_ADDRESS is ${AS7Properties.MANAGEMENT_IP_ADDRESS} " +
        "and AS7Properties.PUBLIC_IP_ADDRESS is ${AS7Properties.PUBLIC_IP_ADDRESS}."
  }

  static ServerAbstract getInstance(String basedir, String as7Dir = "", NoeContext context = NoeContext.forCurrentContext()) {
    def server
    if (platform.isRHEL()) {
      server = new AS7DomainRhel(basedir, as7Dir)
    } else if (platform.isWindows()) {
      server = new AS7DomainWindows(basedir, as7Dir)
    } else if (platform.isSolaris()) {
      server = new AS7DomainSolaris(basedir, as7Dir)
    } else if (platform.isHP()) {
      server = new AS7DomainHPUX(basedir, as7Dir)
    } else {
      throw new RuntimeException("Cannot create AS7 server for platform: ${platform.getOsName()}")
    }

    return server
  }


  @Override
  void start(Map conf = [:]) {
    log.debug("Starting server ${serverId}")
    portsAvailable()
    serverCustomization(conf)
    CmdCommand cmdCommand = new CmdBuilder(start)
            .setWorkDir(new File(getBinDirFullPath()))
            .build()
    cmdCommand.setEnvProperties([NOPAUSE: true])
    log.debug('Start command: "{}"', cmdCommand)
    process = Cmd.startProcess(cmdCommand)
    process.consumeProcessOutput(System.out, System.err)
    waitForStartComplete(startStopTimeout)
    this.pid = extractPid()
    waitUntilHostIsRunning(startStopTimeout)
    log.debug('Server {} successfully started')
  }


  @Override
  long stop(Map conf = [:]) {
    log.debug("Stopping server ${serverId}")
    if (!isRunning()) {
      log.debug("Server is already down.")
      return 0
    } else {
      long startTime = new Date().getTime()
      CmdCommand stopCmd = new CmdBuilder(stop)
              .setWorkDir(new File(getBinDirFullPath()))
              .build()
      stopCmd.setEnvProperties([NOPAUSE: true])
      Process cliProcess = Cmd.startProcess(stopCmd)
      cliProcess.consumeProcessOutput(System.out, System.err)
      waitForShutdownComplete()
      long endTime = new Date().getTime()
      pid = null
      log.debug('Server {} stopped', serverId)
      return endTime - startTime
    }
  }

  /**
   * Kill the server.
   * As domain consists of multiple processes, proper way to kill it is to kill whole tree
   */
  @Override
  boolean kill() {
    log.debug("Killing server ${serverId}")
    if (process) {
      if (Cmd.destroyProcess(process)) {
        pid = null
        return true
      } else {
        return super.killTree()
      }
    } else {
      return super.killTree()
    }
  }

  @Override
  void killAllInSystem() {
    Cmd.killAllInSystem(["jboss-modules.jar", "domain.bat", "jboss-cli.bat", ServerEap.prefix])
  }

  /**
   * Wait until the server is started.
   *
   * TODO: Is it better to look into boot log or examine "/" web page?
   */
  void waitForStartComplete(int timeout = startStopTimeout, int port = mainHttpPort) {
    super.waitForStartComplete(timeout, port)

    // TODO: shall we add checking
    //Gets / (root context)
    // TODO: Hmm, if enable-welcome-root is set to false, this "Welcome to" won't work...
    //If there is only HTTPS connector configured, we assume mainHttpPort and mainHttpsPort being the same.
    if (mainHttpPort == mainHttpsPort) {
      File f = new File(keystorePath)
      URL certUrl = f.toURI().toURL()
      WebClient webClient = new WebClient()
      webClient.getOptions().setRedirectEnabled(true)
      webClient.getOptions().setPrintContentOnFailingStatusCode(true)
      webClient.getOptions().setUseInsecureSSL(true)
      webClient.getOptions().setSSLClientCertificate(certUrl, sslKeystorePassword, "jks")
      Library.verifyUrl(getUrl('', true), 200, WELCOME_ROOT_CONTEXT, 30000, true, false, "", "", webClient, false, false, true)
    } else {
      Library.verifyUrl(getUrl(), 200, WELCOME_ROOT_CONTEXT, 30000, true, false, "", "", null, false, false, true)
    }
    waitUntilHostIsRunning()

  }

  private void waitUntilHostIsRunning(int timeout = 60) {
    long startTime = System.currentTimeMillis()
    while (System.currentTimeMillis() < (startTime + (1000 * timeout))) {
      Map res = as7Cli.runArbitraryCommand("/host=${domainHost}:read-attribute(name=host-state)")
      if (res.exitValue == 0 && res.stdOut.contains('running')) {
        break;
      } else {
        Library.letsSleep(500)
      }
    }
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

  @Override
  void updateConfSetBindAddress(String address) {
    if (isRunning()) {
      IO.handleOutput ' --- ERROR: Server is running, change request for bound IP address is IGNORED!'
      return
    }
    updateConfReplaceRegExp(configFile, 'jboss.bind.address.management:[^}]*', 'jboss.bind.address.management:' + address)
    updateConfReplaceRegExp(configFile, 'jboss.bind.address:[^}]*', 'jboss.bind.address:' + address)
    cfgHost = host = address
    configureCliClient()
    configureJBossCliStop()
  }


  void createManagementUser(String username, String hashpassword) {
    updateConfReplaceRegExp('mgmt-users.properties', '#admin=2a0923285184943425d1f53ddd58ec7a', "${username}=${hashpassword}", false, true)
  }

  @Override
  void shiftPorts(int offset) {
    this.portOffset += offset

    // update domain management socket-bindings
    this.managementHttpPort += offset
    this.managementHttpsPort += offset
    this.managementNativePort += offset
    this.mainHttpPort = this.managementHttpPort
    this.mainHttpsPort = this.managementHttpsPort
    updateConfReplaceRegExp(configFile, 'jboss.management.native.port:[^}]*', 'jboss.management.native.port:' + this.managementNativePort)
    updateConfReplaceRegExp(configFile, 'jboss.management.http.port:[^}]*', 'jboss.management.http.port:' + this.managementHttpPort)

    // update socket-bindings for domain managed servers
    // we have three servers configured by default
    // its enough for testing
    serverOffsets.each { key, value ->

      def stringToReplace = '<socket-bindings port-offset="' + value + '"/>'
      def newString = '<socket-bindings port-offset="' + (value + offset) + '"/>'
      if (serverOffsets[key] == 0 && portOffsetNeverUpdated) { // there is port offset still not defined, we need to create the definition
        stringToReplace = '<server name="server-one" group="main-server-group"/>'
        newString = "<server name=\"server-one\" group=\"main-server-group\">${newString}</server>"
      }
      updateConfReplaceRegExp(configFile, stringToReplace, newString)
      serverOffsets[key] += offset
    }
    IO.handleOutput("portOffset: $portOffset", IO.LOG_LEVEL_FINEST)
    IO.handleOutput("mainHttpPort: $mainHttpPort", IO.LOG_LEVEL_FINEST)
    IO.handleOutput("mainHttpsPort: $mainHttpsPort", IO.LOG_LEVEL_FINEST)
    IO.handleOutput("ajpPort: $ajpPort", IO.LOG_LEVEL_FINEST)
    IO.handleOutput("managementHttpPort: $managementHttpPort", IO.LOG_LEVEL_FINEST)
    IO.handleOutput("managementHttpsPort: $managementHttpsPort", IO.LOG_LEVEL_FINEST)
    IO.handleOutput("managementNativePort: $managementNativePort", IO.LOG_LEVEL_FINEST)
    IO.handleOutput("serverOffsets: $serverOffsets", IO.LOG_LEVEL_FINEST)
    setDefault()
    this.portOffsetNeverUpdated = false
  }

  /**
   * Deploy by copying +
   * removes appName.undeployed marker if present.
   */
  void deployByCopying(String appPath, boolean explodeFirst = false, String contextName = '') {
    throw new NotImplementedException()
  }

  void undeployByDeleting(String appName) {
    throw new NotImplementedException()
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
   * Configuring as7 cli client
   */
  void configureCliClient() {
    def cliPort = managementNativePort
    if (eapVersion.majorVersion > 6) {
      cliPort = managementHttpPort
    }
    as7Cli = new AS7Cli(eapVersion, this.cfgHost, cliPort, new File(getBinDirFullPath(), "jboss-cli." + platform.getScriptSuffix()));
  }

  /**
   * Setting this.stop property for EAP6 with current properties this.cfgHost and this.managementNativePort
   */
  void configureJBossCliStop() {
    this.stop = as7Cli.generateCmdForSingleCliCommand("/host=${domainHost}/:shutdown")
  }

  /**
   * Returns ports for specified serverName in domain in regards to offset
   */
  Map<String, Integer> retrieveDomainServerPorts(String serverName) {
    def defaultMainHttpPort = 8080
    IO.handleOutput("Retrieving domain servers ports", IO.LOG_LEVEL_FINER)
    def serverPorts = [:]
    // main http port
    serverPorts['mainHttpPort'] = defaultMainHttpPort + serverOffsets.get(serverName)
    return serverPorts
  }

}
