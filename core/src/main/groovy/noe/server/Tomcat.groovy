package noe.server

import com.gargoylesoftware.htmlunit.WebClient
import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.NoeContext
import noe.common.utils.Cmd
import noe.common.utils.JBFile
import noe.common.utils.Library
import noe.common.utils.PathHelper
import noe.common.utils.VerifyURLBuilder
import noe.common.utils.Version
import noe.ews.server.tomcat.TomcatCommandUtils
import noe.ews.server.tomcat.TomcatRhel
import noe.ews.server.tomcat.TomcatSolaris
import noe.ews.server.tomcat.TomcatWindows
import noe.rhel.server.tomcat.TomcatJwsRpmScl
import noe.rhel.server.tomcat.TomcatRpm
import noe.server.jk.WorkerServer
import noe.tomcat.configure.TomcatConfigurator

import java.util.concurrent.TimeUnit

@Slf4j
class Tomcat extends ServerAbstract implements WorkerServer {
  public static final int DEFAULT_AJP_PORT = 8009
  public static final int DEFAULT_HTTP_PORT = 8080
  public static final int DEFAULT_HTTPS_PORT = 8443

  List<String> catalina /// Path to catalina.sh
  def extrasDir /// Path to extras dir
  List<String> extrasDirs
  def workDir   /// Path to work dir where tomcat stores compiled JSPs
  def jsvc /// path to jsvc service wrapper
  def daemonStart /// daemon start script handler
  def daemonStop /// daemon stop script handler
  Integer ajpPort /// Port used for communication via AJP, e.g. with load balancer
  Integer jmxPort
  def startSecurity // command for starting tomcat with security
  def cfgHost
  def tomcatDir /// name of parent directory
  def rootBasedir
  File postInstallErrFile
  File postInstallOutFile
  File sslCertDir //Path to directory holding ssl certificates

  Tomcat(String basedir, version) {
    super(basedir, version)
    // Needed because overridden ServerAbstract.getServerRoot
    this.rootBasedir = basedir
    setDefault()
  }

  void setDefault() {
    super.setDefault()
    this.mainHttpPort = (mainHttpPort) ?: DEFAULT_HTTP_PORT
    this.mainHttpsPort = (mainHttpsPort) ?: DEFAULT_HTTPS_PORT
    this.shutdownPort = (shutdownPort) ?: 8005
    this.ajpPort = (ajpPort) ?: DEFAULT_AJP_PORT
    this.jmxPort = (jmxPort) ?: 9012
    this.ignoreShutdownPort = (ignoreShutdownPort) ?: false
    this.cfgHost = (cfgHost) ?: ''
    postInstallErrFile = new File(basedir,  'tomcatPostInstallErr.log')
    postInstallOutFile = new File(basedir,  'tomcatPostInstallOut.log')
    String sslStringDir = PathHelper.join(platform.tmpDir, "ssl", DefaultProperties.SELF_SIGNED_CERTIFICATE_RESOURCE)
    this.sslCertDir = new File(sslStringDir)
    this.sslCertificate = new File(sslCertDir, "server.crt").absolutePath
    this.sslKey = new File(sslCertDir, "server.key").absolutePath

  }

  static ServerAbstract getInstance(String basedir, version, String tomcatDir = "", NoeContext context = NoeContext.forCurrentContext()) {
    def server

    if (context.areInSingleGroup(['ews', 'rpm']) || context.areInSingleGroup(['rhel','tomcat'])) {
      if (DefaultProperties.ewsVersion() >= new Version("4")) {
        server = new TomcatJwsRpmScl(basedir, version, tomcatDir)
      } else {
        server = new TomcatRpm(basedir, version, tomcatDir)
      }
    } else if (context.consistsOf(['ews'])) {
      if (platform.isRHEL()) server = new TomcatRhel(basedir, version, tomcatDir)
      else if (platform.isSolaris()) server = new TomcatSolaris(basedir, version, tomcatDir)
      else if (platform.isWindows()) server = new TomcatWindows(basedir, version, tomcatDir)
      else {
        throw new RuntimeException("Cannot create server for actual platform.")
      }
    } else {
      throw new RuntimeException("Tomcat is not implemented for this context ($context).")
    }

    return server
  }

  void killAllInSystem() {
    Cmd.killAllInSystem(["tomcat", "catalina", processCode])
  }

  @Override
  void serverCustomization(Object conf) {
    if (Boolean.valueOf(Library.getUniversalProperty('JBPAPP9778_WORKAROUND', false))) {
      JBFile.delete(new File(libDir, 'mod_cluster-container-jbossweb.jar'))
    }
  }

  /**
   * Wait until the server is started.
   *
   * When the servlet is ready -> server is fully started.
   * @link http://tomcat.apache.org/tomcat-6.0-doc/architecture/startup/serverStartup.txt
   */
  void waitForStartComplete(int timeout = startStopTimeout, int port = mainHttpPort) {
    super.waitForStartComplete(timeout, port)
    WebClient webClient = new WebClient()
    webClient.getOptions().setRedirectEnabled(true)
    webClient.getOptions().setPrintContentOnFailingStatusCode(false)

    URL serverUrl;
    if (mainHttpPort == mainHttpsPort) {
      File f = new File(keystorePath)
      URL certUrl = f.toURI().toURL()
      webClient.getOptions().setUseInsecureSSL(true)
      webClient.getOptions().setSSLClientCertificate(certUrl, sslKeystorePassword, keystoreType)
      serverUrl = this.getUrl('', true)
    } else {
      serverUrl = this.getUrl()
    }
    boolean startedInTime = VerifyURLBuilder.verifyURL {
      it.url serverUrl
      it.code 200
      it.content "Apache Tomcat"
      it.timeout (timeout * 1000)
      it.logResponse = false
      it.webClient webClient
    }
    if (!startedInTime) {
//      webClient.getOptions().setPrintContentOnFailingStatusCode(true)
      VerifyURLBuilder.verifyURL {
        it.url serverUrl
        it.code 200
        it.content "Apache Tomcat"
        it.tryOnlyOnce true
        it.logResponse = true
        it.webClient webClient
      }
    }
  }

  /**
   * Create new server instance - from ref. tomcat installation.
   */
  ServerAbstract createNewServerInstance(String id, int offset = DefaultProperties.DEFAULT_SHIFT_PORT_OFFSET) {
    return new ServerInstanceCreatorHelper(this).createNewServerInstancePhysicalCopy(offset)
  }

  /**
   * Physical removing of actual server instance.
   *
   * USE WITH CARE !
   */
  boolean deleteCurrentInstance() {
    return new File(this.basedir).deleteDir()
  }

  /**
   * Start the server Tomcat with catalina
   */
  void startCatalina(conf = [:]) {
    log.debug('Starting server Tomcat {} with catalina', serverId)

    portsAvailable()
    serverCustomization(conf)

    Cmd.executeCommandConsumeStreams(catalina + 'start', new File(getBinDirFullPath()))

    waitForStartComplete()
    log.debug('Server Tomcat {} successfuly started with catalina', serverId)
  }

  /**
   * Stop the server Tomcat with catalina
   */
  void stopCatalina(conf = [:]) {
    log.debug('Stopping server Tomcat {} with catalina', serverId)

    if (!isRunning()) log.debug("Server is already down.")
    else {
      Cmd.executeCommandConsumeStreams(catalina + 'stop', new File(getBinDirFullPath()))
      waitForShutdownComplete()
      log.debug('Server Tomcat {} stopped with catalina', serverId)
    }
  }

  /**
   * Check log files for ERRORS and WARNINGS
   */
  List<String> verifyLogs() {
    final List<String> defaultFilteredLines = platform.isFips() ? Arrays.asList(
            "Creation of SecureRandom instance for session ID generation using \\[.*\\] took \\[",
            "Exception initializing random number generator using algorithm \\[SHA1PRNG\\]",
            "ErrorReportValve\\.java"
    ) : Arrays.asList(
            "Creation of SecureRandom instance for session ID generation using \\[.*\\] took \\[")
    return super.verifyLogs(defaultFilteredLines)
  }

  /**
   * Start the server Tomcat with JSVC wrapper
   */
  void startJaas(conf = [:]) {
    log.debug('Starting server Tomcat {} with Jaas', serverId)

    portsAvailable()
    serverCustomization(conf)

    def nl = platform.nl
    def match
    def javaOpts
    def replace

    // Enable
    if (platform.isWindows()) {
      match = 'rem limitations under the License.'
      javaOpts = 'set JAVA_OPTS="-Djava.security.auth.login.config=' + getDeplSrcPath() + "${platform.sep}tomcat${platform.sep}realmTest${platform.sep}templates${platform.sep}jaas${platform.sep}jaas.config\""
      updateBinReplaceRegExp('startup.bat', match, javaOpts, false, true)
    } else {
      //TODO workaround for Tomcat 9 and newer???
      if(version?.getMajorVersion() >= 9) {
        match = '# Start Script for the CATALINA Server'
      } else {
        match = '# export JAVA_OPTS'
      }
      // TODO remove (try previous one on RHEL)
      //javaOpts = 'export JAVA_OPTS="${JAVA_OPTS} -Djava.security.auth.login.config=' + getDeplSrcPath() + "${platform.sep}tomcat${platform.sep}realmTest${platform.sep}templates${platform.sep}jaas${platform.sep}jaas.config\""
      javaOpts = '. `pwd`/jaas.sh'
      if (!JBFile.createFile(new File(binDir, 'jaas.sh'), 'JAVA_OPTS="${JAVA_OPTS} -Djava.security.auth.login.config=' + getDeplSrcPath() + "${platform.sep}tomcat${platform.sep}realmTest${platform.sep}templates${platform.sep}jaas${platform.sep}jaas.config\"" + nl + "export JAVA_OPTS", true)) {
        log.error("${new File(binDir, 'jaas.sh').canonicalPath} wasn't created")
        throw new RuntimeException("${new File(binDir, 'jaas.sh').canonicalPath} wasn't created")
      }

      updateBinReplaceRegExp('startup.sh', match, javaOpts, false, true)
    }

    start(conf)

    // Disable
    match = javaOpts
    if (platform.isWindows()) {
      replace = 'rem limitations under the License.'
      updateBinReplaceRegExp('startup.bat', match, replace, false, true)
    } else {
      replace = '# export JAVA_OPTS'
      updateBinReplaceRegExp('startup.sh', match, replace, false, true)
    }

    log.debug('Server Tomcat {} started with Jaas', serverId)
  }

  /**
   * Start the server Tomcat with JSVC wrapper
   */
  void startJsvc(conf = [:]) {
    log.debug('Starting server Tomcat {} with JSVC', serverId)

    portsAvailable()
    serverCustomization(conf)

    if (platform.isRHEL()) {
      new TomcatConfigurator(this).appendVariableToSetEnv('LD_LIBRARY_PATH', "${basedir}${platform.sep}lib")
    } else if (platform.isSolaris()) {
      new TomcatConfigurator(this).appendVariableToSetEnv('LD_LIBRARY_PATH', "${basedir}${platform.sep}lib:${basedir}${platform.sep}lib64")
    }

    Cmd.executeCommand(daemonStart, new File(getBinDirFullPath()))

    waitForStartComplete()
    // TODO: Umm... This should be changed, ugly!
    if (version == '6') {
      logDirs.each { logDir ->
        new File(getServerRoot() + "${platform.sep}${logDir}").eachFile { File logFile ->
          JBFile.makeAccessible(logFile)
        }
      }
    }

    log.debug('Server Tomcat {} started with JSVC', serverId)
  }

  /**
   * Stop the server Tomcat with JSVC wrapper
   */
  void stopJsvc(conf = [:]) {
    log.debug('Stopping server Tomcat {} with JSVC', serverId)

    if (!isRunning()) log.info("Server is already down.")
    else {
      Cmd.executeCommand(daemonStop, new File(getBinDirFullPath()))

      waitForShutdownComplete()
      log.debug('Server Tomcat {} stopped with JSVC', serverId)
    }
  }

  void updateConfSetBindAddress(String address) {
    if (isRunning()) {
      log.error('Server {} is running, change request for bound IP address is IGNORED!', serverId)
      return
    }

    log.debug("New address for server {} binding: '" + address + "'", serverId)
    if (version.getMajorVersion() == 5) {
      // Tomcat 5
      updateConfReplaceRegExp('server.xml', '<Connector port="' + mainHttpPort + '" maxHttpHeaderSize="8192"'
          + (cfgHost == '' ? '' : ' address="' + cfgHost + '"'),
          '<Connector port="' + mainHttpPort + '" maxHttpHeaderSize="8192" address="' + address + '"')
      updateConfReplaceRegExp('server.xml', '<Connector port="' + ajpPort + '"'
          + (cfgHost == '' ? '' : ' address="' + cfgHost + '"'),
          '<Connector port="' + ajpPort + '" address="' + address + '"')
    } else {
      // Tomcat 6, 7
      updateConfReplaceRegExp('server.xml', '<Connector port="' + mainHttpPort + '" protocol="HTTP/1.1"'
          + (cfgHost == '' ? '' : ' address="' + cfgHost + '"'),
          '<Connector port="' + mainHttpPort + '" protocol="HTTP/1.1" address="' + address + '"')
      updateConfReplaceRegExp('server.xml', '<Connector port="' + ajpPort + '" protocol="AJP/1.3"'
          + (cfgHost == '' ? '' : ' address="' + cfgHost + '"'),
          '<Connector port="' + ajpPort + '" protocol="AJP/1.3" address="' + address + '"')

      if (version.getMajorVersion() > 6) {
        if (version.getMajorVersion() > 7)
          log.warn("Possibly not supported version, but continuing ... ")

        updateConfReplaceRegExp('server.xml', '<Server port="' + shutdownPort + '"'
            + (cfgHost == '' ? '' : ' address="' + cfgHost + '"'),
            '<Server port="' + shutdownPort + '" address="' + address + '"')
      }
    }
    cfgHost = host = address
  }

  void updateDeploymentDescriptorReplaceRegExp(String app, match, replace, Boolean byline = false, Boolean useSimpleReplace = false) {
    def webXmlPath = getDeploymentPath() + platform.sep + app + platform.sep + "WEB-INF" + platform.sep + "web.xml"
    updateFileReplaceRegExp(webXmlPath, match, replace, byline, useSimpleReplace)
  }

  void updateContextDescriptorReplaceRegExp(String app, match, replace, byline = false, useSimpleReplace = false) {
    def contextXmlPath = "Catalina" + platform.sep + "localhost" + platform.sep + app + ".xml"
    updateConfReplaceRegExp(contextXmlPath, match, replace, byline, useSimpleReplace)
  }

  void createUserTomcatManagerAdmin() {
    def match
    def replace
    def nl = platform.nl

    if (version.getMajorVersion() == 6) {

      match = "</tomcat-users>"

      // uncomment
      replace = "  <role rolename=\"tomcat\"/>${nl}" +
          "  <role rolename=\"role1\"/>${nl}" +
          "  <user username=\"tomcat\" password=\"tomcat\" roles=\"tomcat,admin,manager\"/>${nl}" +
          "  <user username=\"both\" password=\"tomcat\" roles=\"tomcat,role1\"/>${nl}" +
          "  <user username=\"role1\" password=\"tomcat\" roles=\"role1\"/>${nl}" +
          "</tomcat-users>${nl}"

      updateConfReplaceRegExp('tomcat-users.xml', match, replace, true, true)
    } else if (version.getMajorVersion() > 6) {
      // TODO Skip the test
      if (version.getMajorVersion() > 7) log.warn("Possibly not supported version, but continuing ... ")

      match = "</tomcat-users>"

      // uncomment
      replace = "  <role rolename=\"tomcat\"/>${nl}" +
          "  <role rolename=\"role1\"/>${nl}" +
          "  <user username=\"tomcat\" password=\"tomcat\" roles=\"tomcat,admin-gui,manager-gui,manager-status,manager-jmx,manager-script\"/>${nl}" +
          "  <user username=\"both\" password=\"tomcat\" roles=\"tomcat,role1\"/>${nl}" +
          "  <user username=\"role1\" password=\"tomcat\" roles=\"role1\"/>${nl}" +
          "</tomcat-users>${nl}"

      updateConfReplaceRegExp('tomcat-users.xml', match, replace, true, true)
    } else {
      log.warn("Not supported version of Tomcat (you can do development ;-)")
    }
  }

  void startServerJmx(Integer port = null, Boolean ssl = false, Boolean authenticate = false, String accessFile = null, String passwordFile = null, Boolean preserve = false) {
    // TODO automatic change directories detection
    //def nl = platform.nl
    def match
    def enableJmx
    def replace
    def jmxOpts = " -Dcom.sun.management.jmxremote.port=${port ?: jmxPort}" +
        " -Dcom.sun.management.jmxremote.ssl=${ssl.toString()}" +
        " -Dcom.sun.management.jmxremote.authenticate=${authenticate.toString()}"

    if (accessFile) {
      // Tomcat demand following file restriction
      if (platform.isWindows()) {
        // change owner
        if (JBFile.chown(platform.actualUser, new File(accessFile)) > 0) {
          throw new RuntimeException("Windows chown ${platform.actualUser} $accessFile FAILED")
        }
        // set read permissions
        if (JBFile.chmod("R", new File(accessFile), platform.actualUser) > 0) {
          throw new RuntimeException("Windows chmod ${platform.actualUser}:R $accessFile FAILED")
        }
      } else {
        if (JBFile.chmod('700', new File(accessFile)) > 0) {
          throw new RuntimeException("chmod 700 $accessFile FAILED")
        }

        // Change group if tomcat run under another user than testsuite runner
        if (loadRunAs()) {
          if (JBFile.chgrp(loadRunAs().toString(), new File(accessFile)) > 0) {
            throw new RuntimeException("chgrp $loadRunAs $accessFile FAILED")
          }
          if (JBFile.chown(loadRunAs().toString(), new File(accessFile)) > 0) {
            throw new RuntimeException("chown $loadRunAs $accessFile FAILED")
          }
        }
      }
      jmxOpts += " -Dcom.sun.management.jmxremote.access.file=\"${accessFile}\""
    }

    if (passwordFile) {
      // Tomcat demand following file restriction
      if (platform.isWindows()) {
        // change owner
        if (JBFile.chown(platform.actualUser, new File(passwordFile)) > 0) {
          throw new RuntimeException("Windows chown ${platform.actualUser} $passwordFile FAILED")
        }
        // set read permissions
        if (JBFile.chmod("R", new File(passwordFile), platform.actualUser) > 0) {
          throw new RuntimeException("Windows chmod ${platform.actualUser}:R $passwordFile FAILED")
        }
      } else {
        if (JBFile.chmod('700', new File(passwordFile)) > 0) {
          throw new RuntimeException("chmod 700 $passwordFile FAILED")
        }

        // Change group if tomcat run under another user than testsuite runner
        if (loadRunAs()) {
          if (JBFile.chgrp(loadRunAs().toString(), new File(passwordFile)) > 0) {
            throw new RuntimeException("chgrp $loadRunAs $passwordFile FAILED")
          }
          if (JBFile.chown(loadRunAs().toString(), new File(passwordFile)) > 0) {
            throw new RuntimeException("chown $loadRunAs $passwordFile FAILED")
          }
        }
      }
      jmxOpts += " -Dcom.sun.management.jmxremote.password.file=\"${passwordFile}\""
    }

    if (!preserve) {
      // do backup
      JBFile.copy(new File(binDir, 'startup.' + platform.getScriptSuffix()), new File(platform.getTmpDir()))
    }

    // Enable Jmx console
    if (platform.isWindows()) {
      // TODO: Change this!
      if (version == '5') {
        match = 'rem Start script for the CATALINA Server'
      } else {
        match = 'rem limitations under the License.'
      }
      enableJmx = 'set CATALINA_OPTS=' + jmxOpts
      log.trace("JMX options: ${enableJmx}")
      updateBinReplaceRegExp('startup.bat', match, enableJmx, true, true)
    } else {
      match = '# JAVA_OPTS="${JAVA_OPTS} -Dcom.sun.management.jmxremote.port=<port>"'
      enableJmx = 'JAVA_OPTS="${JAVA_OPTS} ' + jmxOpts + '"'
      log.trace("JMX options: ${enableJmx}")
      //TODO this is workaround for JWS 5
      if(!updateBinReplaceRegExp('startup.sh', match, enableJmx, true, true)) {
        match = '# Start Script for the CATALINA Server'
        enableJmx += platform.nl + 'export JAVA_OPTS'
        updateBinReplaceRegExp('startup.sh', match, enableJmx, true, true)
      } else {
        updateBinReplaceRegExp('startup.sh', '# export JAVA_OPTS', 'export JAVA_OPTS', true, true)
      }
    }

    // Start the server
    start()

    if (!preserve) {
      // Revert from backup
      JBFile.copy(new File(platform.getTmpDir(), 'startup.' + platform.getScriptSuffix()), new File(binDir), true)
    }
  }

  /**
   * Sets Solaris JAVA_OPTS='-d64' or '-d32' in startup.sh
   */
  void setSolarisJavaOpts() {
    if (!platform.isSolaris()) {
      return
    }

    def javaOpts
    if (platform.getSolPreferredArch() == 32) {
      javaOpts = '-d32'
    } else {
      // 64bit or SPARC
      javaOpts = '-d64'
    }

    def match = '# Licensed to the Apache Software Foundation (ASF) under one or more'
    def replace = 'JAVA_OPTS="${JAVA_OPTS} ' + javaOpts + '"' + platform.nl + "export JAVA_OPTS"
    log.trace("Set JAVA_OPTS on Solaris: ${replace}")
    updateBinReplaceRegExp('startup.sh', match, replace, true, true)
  }

  void deployContextDescriptor(String app, String context) {
    File fullCntxDscrDeplSrcPath = new File(getDeplSrcPath(), context)
    log.trace('fullCntxDscrDeplSrcPath: ' + fullCntxDscrDeplSrcPath)

    configDirs.each { confDir ->
      File contextXmlPath = new File(getServerRoot() + platform.sep + confDir + platform.sep + "Catalina" + platform.sep + "localhost", app + ".xml")
      log.trace('contextXmlPath: ' + contextXmlPath.getAbsolutePath())
      // ant.copy(file: fullCntxDscrDeplSrcPath.getAbsolutePath(), tofile: contextXmlPath.getAbsolutePath(), overwrite: true)
      JBFile.copyFile(fullCntxDscrDeplSrcPath, contextXmlPath, true)
    }
  }

  void undeployContextDescriptor(String app) {
    configDirs.each { confDir ->
      File contextXmlPath = new File(getServerRoot() + platform.sep + confDir + platform.sep + "Catalina" + platform.sep + "localhost", app + ".xml")
      if (contextXmlPath.isFile()) {
        log.debug("Deleting context descriptor file ${contextXmlPath}")
        //ant.delete(file: contextXmlPath.getAbsolutePath())
        JBFile.delete(contextXmlPath, true)
      }
    }
  }

  /**
   * This will undeploy the app and clean the work directory of the app
   */
  void undeployByDeleting(app) {
    super.undeployByDeleting(app)
    undeployContextDescriptor(app)

    File workDirPath = new File(getServerRoot(), workDir)
    log.trace("workDirPath: ${workDirPath}")
    File appWorkDir = new File(workDirPath, "Catalina${platform.sep}localhost${platform.sep}${app}")
    if (workDirPath.isDirectory() && appWorkDir.exists()) {
      // done to prevent exceptions in logs by not leaving first handling of removal of the work dir up to Tomcat.
      if (!JBFile.waitUntilFileIsRemoved(appWorkDir, 10, TimeUnit.SECONDS)) {
        log.debug("App's work dir ${appWorkDir} not removed automatically by tomcat, removing myself")
        JBFile.delete(appWorkDir)
      }
    }
  }

  String getJsvcCommand(Map params = [:]) {
    def s = platform.sep

    Map defParams = [
        'extraJavaOptions': [
            'catalina.home' : basedir,
            'java.io.tmpdir': basedir + "${s}temp"
        ],
        'user'            : TomcatCommandUtils.getTomcatRunUser(),
        'jsvc'            : jsvc
    ]

    return Library.getJsvcCommand(
        "org.apache.catalina.startup.Bootstrap",
        "${basedir}${s}bin${s}bootstrap.jar",
        "${basedir}${s}logs${s}catalina.out",
        "${basedir}${s}logs${s}catalina.err",
        "${basedir}${s}tomcat${version}.pid",
        defParams + params)
  }

  void startSecurity(conf = [:]) {
    log.debug('Starting server Tomcat {} with security manager enabled', serverId)

    portsAvailable()
    serverCustomization(conf)

    log.debug('Start security path: ' + this.startSecurity)
    /*
     def process = startSecurity.execute()
     process.consumeProcessOutput(System.out, System.err)
     process.waitFor()
     */
    handleExamplesPermisions()
    Cmd.executeCommandConsumeStreams(startSecurity, new File(getBinDirFullPath()))
    waitForStartComplete()
    this.pid = extractPid()

    log.debug('Server Tomcat {} started with security manager enabled', serverId)
  }

  void handleExamplesPermisions() {
    def nl = platform.nl
    def permissions = '// ========== SYSTEM CODE PERMISSIONS =========================================' + nl +
            'grant codeBase "file:${catalina.home}/webapps/examples/-" {' + nl +
            '  permission  java.security.AllPermission;' + nl +
            '};' + nl
    updateConfReplaceRegExp('catalina.policy',
            '// ========== SYSTEM CODE PERMISSIONS =========================================',permissions,
            true, true)
  }

  @Deprecated
  /**
   * @see TomcatCommandUtils().getTomcatRunUser()
   */
  String getJsvcUser() {
    return TomcatCommandUtils().getTomcatRunUser()
  }

  void shiftPorts(int offset = DefaultProperties.DEFAULT_SHIFT_PORT_OFFSET) {
    this.mainHttpPort += offset
    this.shutdownPort += offset
    this.mainHttpsPort += offset
    this.ajpPort += offset

    log.debug("shifting ports of server ${serverId}, updating to values: mainHttpPort ${mainHttpPort}, " +
            "shutdownPort: ${shutdownPort}, mainHttpsPort: ${mainHttpsPort}, ajpPort: ${ajpPort}")

    // TODO update server.xml ports with actual ports (this. ... )
    this.updateConfReplaceRegExp('server.xml', '<Server port="8005"', '<Server port="' + Integer.valueOf(8005 + offset) + '"', true, true)
    this.updateConfReplaceRegExp('server.xml', '<Connector port="8080" protocol="HTTP/1.1"', '<Connector port="' + Integer.valueOf(8080 + offset) + '" protocol="HTTP/1.1"', true, true)
    this.updateConfReplaceRegExp('server.xml', 'redirectPort="8443"', 'redirectPort="' + Integer.valueOf(8443 + offset) + '"', false, true)

    try {
      this.updateConfReplaceRegExp('server.xml', '<Connector port="8443" protocol="HTTP/1.1"', '<Connector port="' + Integer.valueOf(8443 + offset) + '" protocol="HTTP/1.1"', true, true)
    }
    catch (RuntimeException rex) {
      this.updateConfReplaceRegExp('server.xml', '<Connector port="8443" protocol="org.apache.coyote.http11.Http11Protocol"', '<Connector port="' + Integer.valueOf(8443 + offset) + '" protocol="org.apache.coyote.http11.Http11Protocol"', true, true)
    }

    this.updateConfReplaceRegExp('server.xml', '<Connector port="8009" protocol="AJP/1.3"', '<Connector port="' + Integer.valueOf(8009 + offset) + '" protocol="AJP/1.3"', true, true)
  }

  @Deprecated
  void enableSslJsse() {
    def nl = platform.nl
    def aprListener = '<Listener className="org.apache.catalina.core.AprLifecycleListener" SSLEngine="on" />'
    def commentAprListener = '<!-- <Listener className="org.apache.catalina.core.AprLifecycleListener" SSLEngine="on" /> -->'
    def dummyComment = '<!-- Define an AJP 1.3 Connector on port 8009 -->'
    def enableJavaSsl = '<Connector port="' + this.mainHttpsPort.toString() + '" protocol="HTTP/1.1" SSLEnabled="true"' + nl +
        'maxThreads="150" scheme="https" secure="true"' + nl +
        'keystoreFile="' + this.keystorePath + '" keystoreType="' + this.keystoreType + '" keystorePass="' + this.sslKeystorePassword + '"' + nl +
        'clientAuth="false" sslProtocol="TLS" />'
    if (platform.isFips()) {
      enableJavaSsl = '<Connector port="' + this.mainHttpsPort.toString() + '" protocol="HTTP/1.1"' + nl +
              '          SSLEnabled="true" maxThreads="150" scheme="https" secure="true"' + nl +
              '          clientAuth="false" sslEnabledProtocols="TLSv1.1+TLSv1.2"' + nl +
              '          keystorePass="' + this.sslKeystorePassword + '"' + nl +
              '          keystoreType="' + this.keystoreType + '"' + nl +
              '          ciphers="' + DefaultProperties.FIPS_140_2_CIPHERS+ '" />'
      }
    updateConfReplaceRegExp('server.xml', aprListener, commentAprListener, true, true)
    updateConfReplaceRegExp('server.xml', dummyComment, enableJavaSsl, true, true)
  }

  @Deprecated
  void enableSslOpenSsl() {
    def nl = platform.nl
    def dummyComment = '<!-- Define an AJP 1.3 Connector on port 8009 -->'
    def enableOpenSsl = '<Connector port="' + this.mainHttpsPort.toString() + '" SSLEnabled="true"' + nl +
        'maxThreads="200" scheme="https" secure="true"' + nl +
        'SSLCertificateFile="' + this.sslCertificate + '"' + nl +
        'SSLCertificateKeyFile="' + this.sslKey + '"' + nl +
        'SSLPassword="' + this.sslKeystorePassword + '"' + nl
    if(platform.isFips()) {
      enableOpenSsl += 'ciphers="' + DefaultProperties.FIPS_140_2_CIPHERS + '"'
    }
    enableOpenSsl += '/>'

    updateConfReplaceRegExp('server.xml', dummyComment, enableOpenSsl, true, true)
  }

  @Deprecated
  void enableSslNIOProtocol() {
    def nl = platform.nl
    def aprListener = '<Listener className="org.apache.catalina.core.AprLifecycleListener" SSLEngine="on" />'
    def commentAprListener = '<!-- <Listener className="org.apache.catalina.core.AprLifecycleListener" SSLEngine="on" /> -->'
    def dummyComment = '<!-- Define an AJP 1.3 Connector on port 8009 -->'
    def enableNIOSsl = '<Connector port="' + this.mainHttpsPort.toString() + '" protocol="org.apache.coyote.http11.Http11NioProtocol" SSLEnabled="true"' + nl +
        'maxThreads="150" scheme="https" secure="true"' + nl +
        'keystoreFile="' + this.keystorePath + '" keystorePass="' + this.sslKeystorePassword + '"' + nl +
        'clientAuth="false" '
    if(platform.isFips()) {
      enableNIOSsl += 'sslEnabledProtocols="TLSv1.1+TLSv1.2" '
    } else {
      enableNIOSsl += 'sslProtocol="TLS" '
    }
    enableNIOSsl +=  'keystoreType="' + this.keystoreType + '" />'

    updateConfReplaceRegExp('server.xml', aprListener, commentAprListener, true, true)
    updateConfReplaceRegExp('server.xml', dummyComment, enableNIOSsl, true, true)
  }

  Map defaultSslProps = [
      ssl                  : true,
      sslKeyStorePass      : "changeit",
      sslKeyStore          : "client-cert-key.jks",
      sslKeyStoreType      : "JKS",
      sslTrustStore        : "ca-cert.jks",
      sslTrustStoreType    : "JKS",
      sslTrustStorePassword: "changeit"
  ]

  /**
   * Get user under who run tomcat server.
   */
  String loadRunAs() {
    String res

    if (!(res = TomcatCommandUtils.getTomcatRunUser())) {
      res = super.loadRunAs()
    }

    return res
  }

  /**
   * Find file that is in one of tomcats extras folder
   * @param name of the file
   * @return file if found, null otherwise
   */
  File findExtrasFile(String name) {
    File file
    getExtrasDirs().find { path ->
      file = new File(path, name)
      return file.exists()
    }
    return file.exists() ? file : null
  }

}
