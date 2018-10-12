package noe.server

import groovy.transform.TypeChecked
import groovy.util.logging.Slf4j
import noe.common.NoeContext
import noe.common.utils.PathHelper

import java.util.regex.Matcher
import java.util.regex.Pattern

import noe.common.DefaultProperties
import noe.common.utils.Cmd
import noe.common.utils.JBFile
import noe.common.utils.Library
import noe.common.utils.Version
import noe.ews.server.ServerEws
import noe.ews.server.httpd.Httpd22Rpm
import noe.ews.server.httpd.Httpd24Rpm
import noe.ews.server.httpd.HttpdHPUX
import noe.ews.server.httpd.HttpdRhel
import noe.ews.server.httpd.HttpdRpm
import noe.ews.server.httpd.HttpdSolaris
import noe.ews.server.httpd.HttpdWindows
import noe.jbcs.server.httpd.HttpdCoreRhel
import noe.jbcs.server.httpd.HttpdCoreRpm
import noe.jbcs.server.httpd.HttpdCoreSolaris
import noe.jbcs.server.httpd.HttpdCoreWindows

@TypeChecked
@Slf4j
abstract class Httpd extends ServerAbstract {
  String httpdServerRoot // Apache server root
  List<String> apachectl       // apachectl Script/Binary
  String httpdevent = "sudo ./httpd.event"
  String httpdworker = "sudo ./httpd.worker"
  String cgiDeploymentPath   // CGI scripts deplyment path for httpd
  String modClusterCacheDir  // directory for storing cached data
  String opensslPath         // openssl PATH
  String abPath         // path for the ApacheBench
  String htdbmPath      // path Manipulate DBM password databases
  String htpasswdPath   //path to htpasswd
  String httxt2dbmPath  // not present on Windows and HPUX...!!!
  String logresolvePath // path to logresolve
  String resConfd = "etc${platform.sep}httpd${platform.sep}conf.d"
  String resHtml = "var${platform.sep}www${platform.sep}html"
  String resCgi = "var${platform.sep}www${platform.sep}cgi-bin"
  static final String defaultServerId = "httpd"
  String httpdDir
  String modulesDir // path to modules directory
  String cachePath // directory for mod_cache caching
  File postInstallErrFile
  File postInstallOutFile
  File sslCertDir

  Httpd(String basedir, version) {
    super(basedir, version)
    setDefault()
  }

  void setDefault() {
    super.setDefault()
    this.mainHttpPort = (mainHttpPort) ?: 80
    this.shutdownPort = (shutdownPort) ?: 443
    this.mainHttpsPort = (mainHttpsPort) ?: 443
    // TODO how to identify a process?
    this.processCode = ''
    this.cachePath = this.basedir + platform.sep + 'cache'
    postInstallErrFile = new File(getHttpdServerRootFull(), 'httpdPostInstallErr.log')
    postInstallOutFile = new File(getHttpdServerRootFull(), 'httpdPostInstallOut.log')
    String sslStringDir = PathHelper.join(platform.tmpDir, "ssl", "self_signed")
    this.sslCertDir = new File(sslStringDir)
    this.sslCertificate = new File(sslCertDir, "server.crt").absolutePath
    this.sslKey = new File(sslCertDir, "server.key").absolutePath
    this.keystorePath = new File(sslCertDir, "server.jks").absolutePath
  }

  static ServerAbstract getInstance(String basedir, version, String httpdDir = '', NoeContext context = NoeContext.forCurrentContext()) {
    def server

    if (context.areInSingleGroup(['ews', 'rpm']) || DefaultProperties.USE_HTTPD_RPM) {
      if (DefaultProperties.apacheCoreVersion()) {
        server = new HttpdCoreRpm(basedir, version)
      } else if (ServerEws.extractMajorVersion() <= 2) {
        // EWS 2.1
        if (platform.isRHEL7()) {
          // RHEL7
          server = new Httpd22Rpm(basedir, version)
        } else {
          // RHEL5 & RHEL6
          server = new HttpdRpm(basedir, version)
        }
      } else {
        // JWS 3.0 - RHEL6 & RHEL7
        log.debug("Httpd.getInstance: Httpd24Rpm: ${basedir}")
        server = new Httpd24Rpm(basedir, version)
      }
    } else {
      log.debug("I'm gonna give this httpdDir:${httpdDir}")
      if (DefaultProperties.apacheCoreVersion()) {
        log.debug("Httpd.getInstance: coreVersion: ${DefaultProperties.apacheCoreVersion()}")
        if (platform.isRHEL()) {
          server = new HttpdCoreRhel(basedir, DefaultProperties.apacheCoreVersion(), httpdDir)
        } else if (platform.isWindows()) {
          server = new HttpdCoreWindows(basedir, DefaultProperties.apacheCoreVersion(), httpdDir)
        } else if (platform.isSolaris()) {
          server = new HttpdCoreSolaris(basedir, DefaultProperties.apacheCoreVersion(), httpdDir)
        }
      } else if (platform.isRHEL()) {
        server = new HttpdRhel(basedir, version, httpdDir)
      } else if (platform.isWindows()) {
        server = new HttpdWindows(basedir, version, httpdDir)
      } else if (platform.isSolaris()) {
        server = new HttpdSolaris(basedir, version, httpdDir)
      } else if (platform.isHP()) {
        server = new HttpdHPUX(basedir, version, httpdDir)
      } else {
        throw new RuntimeException("Cannot create server for actual platform")
      }
    }

    return server
  }

  /**
   * Create a new server instance.
   */
  ServerAbstract createNewServerInstance(String id, int offset = DefaultProperties.DEFAULT_SHIFT_PORT_OFFSET) {
    return new ServerInstanceCreatorHelper(this).createNewServerInstancePhysicalCopy(offset)
  }

  /**
   * Kill the server based on parent pid
   * @exception RuntimeException, when pid is not defined and server is running
   */
  boolean kill() {
    if ( !pid && isRunning()) {
      throw new RuntimeException("Pid of httpd is not defined")
    }
    if ( !pid ) {
      log.debug("Pid is not defined, server probably isn't running")
    } else {
      Cmd.killTree(pid, serverId)
    }
    return !isRunning()
  }

  void killAllInSystem() {
    Cmd.killAllInSystem(["httpd"])
  }

  /**
   * HTTPD root path ..../httpd
   */
  String getHttpdServerRootFull() {
    if (platform.isRHEL()) return basedir
    else if (platform.isSolaris() || platform.isWindows() || platform.isHP()) return (basedir + httpdServerRoot)
    else {
      throw new RuntimeException("Cannot create ServerRootFull path for actual platform")
    }
  }

  int executeApachectl(String options) {
    List<String> parsedOptions = options.split().toList().collect {
      if (it ==~ /".*"/) {
        return it.substring(1, it.size() - 1)
      } else {
        return it
      }
    }
    return executeApachectl(parsedOptions)
  }

  /**
   * String option as options what should be added when starting apache httpd
   * Repaired Error when running with JDK1.6 on windows as it incorrectly parse string command
   * with spaces
   *
   */
  int executeApachectl(List<String> options) {
    List<String> command = apachectl.collect()
    command.addAll(options)
    return (int) Cmd.executeCommandConsumeStreams(command, new File(getBinDirFullPath())).exitValue
  }

  int executeHttpdWorker(option) {
    if (!platform.isWindows()) {
      log.debug("Execute $httpdworker $option")
      int ret = Cmd.executeCommand("$httpdworker $option", new File(getBinDirFullPath()))
      log.debug("Execute $httpdworker $option finished")
      return ret
    } else {
      log.debug("This is MS Windows platform, skipping executeHttpdWorker().")
    }
  }

  int executeHttpdEvent(option) {
    if (!platform.isWindows()) {
      log.debug("Execute $httpdevent $option")
      int ret = Cmd.executeCommand("$httpdevent $option", new File(getBinDirFullPath()))
      log.debug("Execute $httpdevent $option finished")
      return ret
    } else {
      log.debug("This is MS Windows platform, skipping executeHttpdEvent().")
    }
  }

  void enableModStatus() {
    if (version >= new Version("2.4")) {
      // httpd 2.4 jws30
      def add = '<Location /server-status>\n' +
          '    SetHandler server-status\n' +
          '    Require all granted\n' +
          '</Location>\n'
      updateConfByInsertingTextToPositionInFile("httpd.conf", add, -1)
    } else {
      // httpd2.2 ews 2.1
      def match = '#<Location /server-status>\n' +
          '#    SetHandler server-status\n' +
          '#    Order deny,allow\n' +
          '#    Deny from all\n' +
          '#    Allow from .example.com\n' +
          '#</Location>\n'
      def replace = '<Location /server-status>\n' +
          '    SetHandler server-status\n' +
          '    Order deny,allow\n' +
          '    Deny from all\n' +
          '    Allow from all\n' +
          '</Location>\n'
      updateConfReplaceRegExp('httpd.conf', match, replace)
    }

  }

  void updateConfSetBindAddress(String address) {
    if (isRunning()) {
      log.error('Server is running, change request for bound IP address is IGNORED!')
      return
    }
    log.debug("New address:port for server binding: '" + address + ":" + mainHttpPort + "'")
    updateConfReplaceRegExp('httpd.conf', 'Listen (.*)',
        'Listen ' + ((address.contains(':') && !address.contains(']')) ? '[' + address + ']' : address) + ':' + mainHttpPort, true)
    host = address
  }

  /**
   * Deploy CGI test scripts files by copying
   * must be copied everytime in cgi-bin/sometestdirectory
   */
  void deployCgiByCopying(String cgiPath) {
    File fullCgiDeplSrcPath = new File(getDeplSrcPath(), cgiPath)
    // directory
    if (fullCgiDeplSrcPath.isDirectory()) {
      def dir = getCgiDeploymentPath() + "${platform.sep}" + fullCgiDeplSrcPath.getName()
      JBFile.copyDirectoryContent(fullCgiDeplSrcPath, new File(dir))

      // chmod 777
      Library.chmod(dir, "777")
    }
    // not valid path
    else {
      throw new RuntimeException("'$fullCgiDeplSrcPath' is not a directory.")
    }
  }

  /**
   * Undeploy CGI test scripts files by deleting it's directory.  
   */
  void undeployCgiByDeleting(String cgiDirName) {
    log.debug("Starting of undeploying CGI by deleting")

    File cgiDirPath = new File(getCgiDeploymentPath(), cgiDirName)

    JBFile.delete(cgiDirPath, true)

    log.debug("Stopping undeploying CGI by deleting")
  }

  void shiftPorts(int offset = DefaultProperties.DEFAULT_SHIFT_PORT_OFFSET) {
    if (isRunning()) {
      log.error('Server is running, change request for bound IP address : port is IGNORED!')
      return
    }

    // HTTP
    log.debug("mainHttpPort:${mainHttpPort}, offset:${offset}")
    int port = offset + mainHttpPort
    def listen = 'Listen ' + ((host.contains(':') && !host.contains(']')) ? '[' + host + ']' : host) + ':' + port
    log.debug('New Listen: ' + listen)
    updateConfReplaceRegExp('httpd.conf', 'Listen (.*)', listen, true)
    def serverName = 'ServerName ' + ((DefaultProperties.HTTPD_SERVER_NAME.contains(':') && !DefaultProperties.HTTPD_SERVER_NAME.contains(']')) ? '[' + DefaultProperties.HTTPD_SERVER_NAME + ']' : DefaultProperties.HTTPD_SERVER_NAME) + ':' + port
    updateConfReplaceRegExp('httpd.conf', 'ServerName (.*)', serverName, true)
    mainHttpPort = port

    // HTTPS
    log.debug("mainHttpsPort:${mainHttpsPort}, offset:${offset}")
    port = offset + mainHttpsPort
    listen = 'Listen ' + ((host.contains(':') && !host.contains(']')) ? '[' + host + ']' : host) + ':' + port
    log.debug('New Listen: ' + listen)
    updateConfReplaceRegExp('ssl.conf', 'Listen (.*)', listen, true)
    updateConfReplaceRegExp('ssl.conf', '<VirtualHost _default_:(.*)', "<VirtualHost _default_:${port}>", true)
    serverName = 'ServerName ' + ((DefaultProperties.HTTPD_SERVER_NAME.contains(':') && !DefaultProperties.HTTPD_SERVER_NAME.contains(']')) ? '[' + DefaultProperties.HTTPD_SERVER_NAME + ']' : DefaultProperties.HTTPD_SERVER_NAME) + ':' + port
    updateConfReplaceRegExp('ssl.conf', 'ServerName (.*)', serverName, true)
    mainHttpsPort = port
  }

  void modJkSetSticky(value, boolean restart = true) {
    if (restart) stop()
    updateConfReplaceRegExp('workers.properties', 'sticky_session=(.*)', 'sticky_session=' + ((value) ? '1' : '0'))
    if (restart) start()
  }

  /**
   * Get user under who run httpd server.
   */
  String loadRunAs() {
    def res = ''
    if (!(res = Library.getUniversalProperty('httpd.run.as', ''))) {
      res = super.loadRunAs()
    }
    return res
  }

  void start(Map conf = [:], List<String> options = []) {
    File pidFile = getPidFile()
    if (pidFile.exists() && !isRunning()) {
      log.debug("PidFile $pidFile exist even when server is not running, probably leftover after kill, removing it")
      JBFile.delete(pidFile)
    }
    super.start(conf, options)
  }

  /**
   * Returns PidFile, if configured in httpd.conf file.
   * Otherwise returns null.
   */
  File getPidFile() {
    File httpdConfFile = null
    configDirs.each { configDir ->
      def confToCheckFile = new File("${basedir}${platform.sep}${configDir}${platform.sep}httpd.conf")
      if (confToCheckFile.exists()) {
        httpdConfFile = confToCheckFile
      }
    }
    def pidPattern = "^PidFile\\s+(.*)"
    Pattern pattern = Pattern.compile(pidPattern)
    String line
    httpdConfFile?.withReader { reader ->
      while ((line = reader.readLine()) != null) {
        Matcher matcher = pattern.matcher(line)
        if (matcher.matches()) {
          String pidFilePath = matcher.group(1)
          matcher = (pidFilePath =~ /(.*)\s+/) //Take care of any spaces after path
          if (matcher.matches()) {
            pidFilePath = matcher.group(1)
          }
          matcher = (pidFilePath =~ /"(.*)"/)
          if (matcher.matches()) {
            pidFilePath = matcher.group(1)
          }
          File pidFile = new File(pidFilePath)
          return pidFile.isAbsolute() ? pidFile : new File(getHttpdServerRootFull(), pidFilePath)
        }
      }
    }
  }

  Integer extractPid() {
    String stringPid = null
    File pidFile = getPidFile()
    if (!pidFile.exists()) {
      throw new RuntimeException("Extraction of httpd PID went wrong, ${pidFile} don't exist")
    } else {
      stringPid = JBFile.read(pidFile)
    }
    if ( stringPid.isEmpty() ) {
      throw new RuntimeException("Extraction of httpd PID went wrong, ${pidFile} is empty")
    }
    pid = stringPid.toInteger()
    return pid
  }

  void waitForStartComplete(int timeout = startStopTimeout, int port = mainHttpPort) {
    super.waitForStartComplete(timeout, port)
    File pidFile = getPidFile()
    if (!JBFile.waitUntilFileExists(pidFile, timeout)) {
      throw new RuntimeException("Server start problem - $pidFile not created!!.")
    }
  }

  @Override
  double actualCPULoad() {
    return super.actualCPULoad(true)
  }
}
