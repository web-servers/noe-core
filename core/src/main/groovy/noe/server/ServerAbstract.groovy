package noe.server

import com.gargoylesoftware.htmlunit.WebClient
import groovy.util.logging.Slf4j
import noe.app.IApp
import noe.common.DefaultProperties
import noe.common.utils.Cmd
import noe.common.utils.JBFile
import noe.common.utils.Java
import noe.common.utils.Library
import noe.common.utils.Platform
import noe.common.utils.Version
import noe.common.utils.processid.ProcessUtils

import java.util.concurrent.TimeUnit


@Slf4j
abstract class ServerAbstract implements IApp {
  private String serverId /// This is serverId which is supposed to be the same as in serverController, by default is same as basedir name
  static AntBuilder ant /// Instance of class AntBuilder
  String basedir /// Absolute path where server is
  //TODO: EWS specific, pls remove.
  Version version /// major version for instance 6, 7 for Tomcat
  static Platform platform = new Platform() /// platform identification
  String host // human address of the server (localhost)
  Integer mainHttpPort = null /// Main port where server listening
  Integer mainHttpsPort = null /// Main https port where server listening
  //TODO: EWS specific, pls remove.
  Integer shutdownPort /// Where server listening for shutdown
  //TODO: EWS specific, pls remove.
  Boolean ignoreShutdownPort ///
  def start ///standard startup command (apachectl start)
  def stop ///standard shutdown command (apachectl stop)
  String binPath // relative path (server root) to dir where stop handler is stored
  List configDirs = [] // relative paths to dir where server config files are stored
  List logDirs = [] // relative paths to dir where server logs are stored
  String libDir // path where lib is stored
  String binDir // where binary files are stored
  //TODO: Remove misleading comments about tomcat, we have EAP in the house now :-)
  String deploymentPath // common for tomcat and httpd
  String confDeploymentPath // common for tomcat and httpd (conf.d)
  String confMainPath // common for tomcat and httpd (conf)
  String serverRoot // Root path to basedir from which is counted path to server instance specific for each platform
  String refBasedir // from what path create another server nodes

  // process management
  Process process // running server process (if was started on background)
  Integer pid // process pid
  String processCode // for process identification
  String runAs // under who user run the process, empty value means actual user
  String runContext // commandline context

  // ssl properties
  String sslCertificate
  String sslKey
  String keystorePath
  String keystoreType
  String securityPath
  String sslKeystorePassword

  // truststore related properties
  String truststorePath
  String truststorePassword

  // start stop timeout
  int startStopTimeout = DefaultProperties.START_STOP_TIMEOUT

  protected backupProps = [:] /// backuped instance properties

  /**
   * Constructor
   */
  ServerAbstract(String basedir, version) {
    this.basedir = basedir
    if (version) {
      if (version instanceof Version) {
        this.version = version
      } else {
        this.version = new Version(version)
      }
    }
    setDefault()
  }

  abstract void updateConfSetBindAddress(String address)

  abstract void shiftPorts(int offset)

  abstract void killAllInSystem()

  /**
   * Initialize server object
   * Called in constructor and after each test.
   *
   * TODO We must give setDefault concept away, it is just source of problems,
   */
  void setDefault() {
    this.ant = (ant) ?: new AntBuilder()
    this.ant.property(environment: 'env')
    this.serverRoot = basedir
    this.host = (host) ?: DefaultProperties.HOST
    this.ignoreShutdownPort = true
    this.sslCertificate = getDeplSrcPath() + "${platform.sep}ssl${platform.sep}${DefaultProperties.SELF_SIGNED_CERTIFICATE_RESOURCE}${platform.sep}server.crt"
    this.sslKey = getDeplSrcPath() + "${platform.sep}ssl${platform.sep}${DefaultProperties.SELF_SIGNED_CERTIFICATE_RESOURCE}${platform.sep}server.key"
    if( platform.isFips() ) {
      this.keystorePath = "${platform.tmpDir}${platform.sep}ssl${platform.sep}${DefaultProperties.SELF_SIGNED_CERTIFICATE_RESOURCE}${platform.sep}nssdb"
      this.keystoreType = 'PKCS11'
      def variant = (Java.openJDK ? "openjdk" : (Java.oracleJDK ? "oracle-java" : "ibm-java")) +
              (Java.isJdk8() ? "-1.8" : ( Java.isJdk11()? "-11" : "-17"))
      this.securityPath = "${platform.tmpDir}${platform.sep}ssl${platform.sep}${DefaultProperties.SELF_SIGNED_CERTIFICATE_RESOURCE}${platform.sep}java.security."+ variant
    } else{
      this.keystorePath = getDeplSrcPath() + "${platform.sep}ssl${platform.sep}${DefaultProperties.SELF_SIGNED_CERTIFICATE_RESOURCE}${platform.sep}server.jks"
      this.keystoreType = 'jks'
    }
    this.sslKeystorePassword = 'changeit'
    this.truststorePassword = 'changeit'
    this.pid = null
    setRunAs(loadRunAs())
    this.processCode = String.valueOf(Math.abs(this.hashCode()))
  }

  List<File> getLogsFiles(){
    List<File> files = []

    logDirs.each { logDir ->
      files.add(new File(getServerRoot(), logDir))
    }

    return files
  }

  void backup() {
    log.trace('Starting properties backup for server {}', serverId)

    Library.getDeclaredFieldsAll(this.getClass()).each { name ->
      if (!(name == 'backupProps' || name == 'class' || name == 'ant' || name == 'metaClass' || name == 'pid' || name == 'log')) {
        this.backupProps.put(name.toString(), this."${name}")
      }
    }

    log.trace('Properties backed up for server {}', serverId)
  }

  void restore() {
    log.trace('Restoring properties of server {}', serverId)

    this.backupProps.each { name, value ->
      try {
        this.setProperty(name, value)
      } catch (e) {
        log.trace("Restoring of property $name skipped, probably read only")
      }
    }

    log.trace('Properties restored of server {}', serverId)
  }



  /**
   * Start the server
   */
  void start(Map conf = [:], List<String> options = []) {
    log.debug("Starting server ${serverId}")

    portsAvailable()
    serverCustomization(conf)
    log.debug("Start command:${start + options} in directory:${getBinDirFullPath()}")
    Cmd.executeCommandConsumeStreams(start + options, new File(getBinDirFullPath()))

    waitForStartComplete()

    this.pid = extractPid()

    log.debug("Server ${serverId} started with PID ${this.pid} and processCode: ${this.processCode}")
  }

  /**
   * Kill the server.
   */
  boolean kill() {
    log.debug("Killing server ${serverId} with PID: ${this.pid} and processCode: ${this.processCode}")

    // when the process runs in background calling destroy on its instance in order to terminate it
    // if it was executed using cmd /C or /bin/sh -c, then it doesn't need to work, thereby if I know pid, let's kill it using pid
    if (process && ProcessUtils.isProcAlive(process)) {
      Cmd.destroyProcess(process)
    }

    if (this.pid) {
      if (Cmd.pidExists(this.pid)) {
        log.debug("${this.pid} is still in the system, using pid to kill the process")
        Cmd.kill(this.pid, processCode)
      }
      this.pid = null
    } else if (platform.isWindows() && processCode) {
      Cmd.kill(this.pid, processCode)
      this.pid = null;
    } else {
      log.debug("Cannot kill, PID ${this.pid} of this server is unknown")
      return false
    }

    log.debug("Server ${serverId} killed")
    return true
  }

  /**
   * Get process id (pid) of running server.
   * Example
   *   tomcat7/bin/startup.sh 316546813 
   *
   * Default behaviour
   *  - server run with some unique id (processCode) 
   *  - running process is filtered by this id 
   */
  Integer extractPid() {
    Integer extractedPid = null
    if (process != null && ProcessUtils.isProcAlive(process)) {
      log.debug("Process is running, trying to extract pid directly from it")
      extractedPid = ProcessUtils.getProcessId(process)
    }
    if ((extractedPid == null || extractedPid == ProcessUtils.UNDEFINED_PROCESS_ID) && processCode != null) {
      extractedPid = Cmd.extractPid(processCode) ?: null
    }
    return extractedPid
  }

  /**
   * The same as extractPid, but only for java processes of the current user.
   * The main advantage is that it works the same way on all platforms which mainly improves extraction of correct pid on Solaris
   */
  Integer extractJavaPid() {
    if (processCode) {
      pid = Cmd.getJavaPid(processCode) ?: null
      log.debug('Extracted Java PID: {}', this.pid)
    }

    return pid
  }

  /**
   * Kill the server process tree.
   */
  boolean killTree() {
    log.debug("Starting killing the server ${serverId} process tree")

    if (pid != null || processCode != null) {
      Cmd.killTree(pid, processCode)
      if (pid != null && Cmd.pidExists(this.pid)) {
        log.debug("Parent process remained alive after killTree, trying to kill it using kill method")
        Cmd.kill(pid, processCode)
      }
      pid = null
    } else {
      log.debug("Cannot kill tree, PID or ProcessCode of server ${serverId} is unknown")
      return false
    }

    log.debug("Server ${serverId} process tree killed")
    return true
  }

  /**
   * Stop the server, returns time in ms how long it took to stop the server. Negative number means it wasn't counted.
   */
  long stop(Map conf = [:]) {
    log.debug("Stopping server ${serverId}")

    if (!isRunning()) {
      log.debug("Server is already down.")
      return 0
    } else {
      log.debug('Stop path: ' + stop)
      long startTime = new Date().getTime()
      int ret = Cmd.executeCommand(stop, new File(getBinDirFullPath()))

      waitForShutdownComplete()
      long endTime = new Date().getTime()
      if (ret == 0) {
        pid = null
      }

      log.debug("Server ${serverId} stopped")
      return endTime - startTime
    }
  }

  /**
   * Deploy test application by copying
   *
   * TODO: context name for war
   */
  void deployByCopying(String appPath, Boolean explodeFirst = false, String contextName = '', Boolean zipAsWar = false) {
    File fullDeplSrcPath = new File(getDeplSrcPath(), appPath)
    def destDirName = contextName ?: fullDeplSrcPath.getName()

    // war
    if (fullDeplSrcPath.isFile() && fullDeplSrcPath.toString().endsWith('.war')) {
      if (explodeFirst) {
        //TODO: Ad "0..-5": Removes .war from the name... :-(
        JBFile.nativeUnzip(fullDeplSrcPath, new File(getDeploymentPath() + "${platform.sep}" + fullDeplSrcPath.getName()[0..-5]), JBFile.useAdminPrivileges, true)
      } else {
        File destPathFile = new File(getDeploymentPath())
        if (destPathFile.canWrite()) {
          ant.copy(file: fullDeplSrcPath.getAbsolutePath(), todir: getDeploymentPath(), overwrite: true)
        } else {
          if (!platform.isWindows() && JBFile.useAdminPrivileges) Cmd.executeSudoCommand("cp -r ${fullDeplSrcPath.getAbsolutePath()} ${destPathFile.getAbsolutePath()}", new File(getServerRoot()))
          else throw new RuntimeException("Missing rights to deploy application")
        }
      }
    }
    // directory
    else if (fullDeplSrcPath.isDirectory()) {
      if (zipAsWar) {
        if (!destDirName.endsWith(".war")) {
          destDirName += ".war"
        }
        new AntBuilder().zip(destFile: new File(getDeploymentPath(), destDirName), basedir: fullDeplSrcPath)
      } else {
        JBFile.copyDirectoryContent(fullDeplSrcPath, new File(getDeploymentPath(), destDirName))
      }
    }
    // not valid path
    else {
      throw new RuntimeException("File $fullDeplSrcPath does not exist.")
    }
  }

  /**
   * Deploy test application config files by copying
   */
  void deployConfsByCopying(String confPath) {
    File fullConfDeplSrcPath = new File(getDeplSrcPath(), confPath)
    log.trace('fullConfDeplSrcPath: ' + fullConfDeplSrcPath)

    // file
    if (fullConfDeplSrcPath.isFile()) {
      JBFile.copy(fullConfDeplSrcPath, new File(getConfDeploymentPath()))
    }
    // directory
    else if (fullConfDeplSrcPath.isDirectory()) {
      JBFile.copyDirectoryContent(fullConfDeplSrcPath, new File(getConfDeploymentPath()))
    }
    // not valid path
    else {
      throw new RuntimeException("File $fullConfDeplSrcPath not exists.")
    }
  }

  /**
   * Undeploy application by deleting it's directory and .war file
   * WARNING: This method is overridden in ews.server.Tomcat
   */
  void undeployByDeleting(String appName) {
    log.debug("Undeploying of ${appName} by deleting")

    File appDirPath = new File(getDeploymentPath(), appName)
    log.debug("appDirPath: " + appDirPath)

    File appWarPath = new File(getDeploymentPath(), appName + ".war")

    if (appDirPath.isDirectory() || appDirPath.isFile()) {
      JBFile.delete(appDirPath)
    }
    if (appWarPath.isFile() || appWarPath.isDirectory()) {
      JBFile.delete(appWarPath)
    }

    log.debug("Finished undeploying by deleting for app: ${appName}")
  }

  /**
   * Get root dir of application   
   */
  static String getDeplSrcPath() {
    // TODO move test resources somewhere else ?
    return Library.getRootPath() + "${platform.sep}resources"
  }

  /**
   * Is main http port available?
   */
  void portsAvailable() {
    if (checkmainHttpPort()) {
      //Let's give it 5...
      for (i in (1..5)) {
        if (!checkmainHttpPort()) {
          return
        }
        Library.letsSleep(1000)
      }
      log.debug("$mainHttpPort is not available, printing running processes + opened ports")
      Library.logRunningProcessesAndPorts()
      throw new RuntimeException("${serverId}: Cannot bind to $mainHttpPort or $shutdownPort")
    }
  }

  /**
   * Check if the server is not down already
   */
  boolean isRunning() {
    def res = false

    if (pid) {
      res = Cmd.pidExists(pid)
    } else {
      res = checkmainHttpPort()
    }
    return res
  }

  /**
   * Wait until the server is started.
   */
  // TODO: Try to kill it if it started in a wrong way...
  void waitForStartComplete(int timeout = startStopTimeout, int port = mainHttpPort) {
    if (!Library.waitForTcp(host, port, timeout)) {
      stop()
      kill()
      throw new RuntimeException("Server ${serverId} start problem - ${host}:${port} is not opened.")
    }
  }

  /**
   * Wait until the server is stopped.
   */
  void waitForShutdownComplete(int timeout = startStopTimeout, int port = mainHttpPort) {
    if (!Library.waitForTcpClosed(host, port, timeout)) {
      def message = "Server ${serverId} stop problem - ${host}:${port} is not closed."
      log.error(message)
      Library.logRunningProcessesAndPorts()
      throw new RuntimeException(message)
    }
    if (pid) {
      if (!Cmd.waitForPidRemoved(pid, timeout)) {
        def message = "Server ${serverId} stop problem - pid $pid was not removed from the system."

        if (platform.isWindows()) {
          log.warn(message + " - probably pid $pid incorrectly represents only a command-line window which may require" +
                  " one to \"press any key to continue...\"")
        } else {
          log.warn(message)
        }
        Library.logRunningProcessesAndPorts()
        throw new RuntimeException(message)
      }
    }
  }

  /**
   * Do initial configuration before server is started
   */
  void serverCustomization(conf) {
    //intentionally nothing here, ask jstefl :-)
  }

  /**
   * Backup all files in config dirs.
   */
  void backupConfs() {
    log.debug('Backing up all config files for server {}', serverId)

    def srcDir
    def destDir
    configDirs.each { dir ->
      srcDir = getServerRoot() + dir
      destDir = getServerRoot() + dir + ".bkp"
      backupConfDir(srcDir, destDir)
    }
  }

  /**
   * backups specified conf dir to the specified destination
   */
  void backupConfDir(String srcDir, String destDir) {
    File srcDirAsFile = new File(srcDir)
    File destDirAsFile = new File(destDir)
    JBFile.delete(destDirAsFile)
    JBFile.mkdir(destDirAsFile)

    if (!JBFile.copyDirectoryContent(srcDirAsFile, destDirAsFile, Library.isSymlink(srcDir))) {
      throw new RuntimeException("backupConfs() failed when backing up ${srcDir} to ${destDir}")
    }
  }

  /**
   * Restore all files in config dirs.
   */
  void restoreConfs() {
    log.debug('Restoring all config files for server {}', serverId)

    def srcDir
    def destDir
    configDirs.each { dir ->
      srcDir = getServerRoot() + dir + ".bkp"     // always regular
      destDir = getServerRoot() + dir             // sometimes symlink

      restoreConfDir(srcDir, destDir)
    }
  }

  /**
   * restores one config dir
   * @param srcDir directory of the backup
   * @param destDir destination to restore it to
   */
  void restoreConfDir(String srcDir, String destDir) {

    JBFile.cleanDirectory(new File(destDir))
    if (!JBFile.copyDirectoryContent(new File(srcDir), new File(destDir))) {
      throw new RuntimeException("restoreConfs() failed when restoring ${srcDir} to ${destDir}")
    }
  }


  List<File> retrieveConfFilesByName(String confName) {
    def confFiles = []

    configDirs.each { confDir ->
      def filePath = getServerRoot() + platform.sep + confDir + platform.sep + confName
      log.trace("Looking up: ${filePath}")
      def confFile = new File(filePath)
      if (confFile.exists()) {
        confFiles.add(confFile)
      }
    }
    log.debug("Found config files: ${confFiles} for conf name ${confName}")
    return confFiles
  }

  /**
   * Replace config file reg. expression.
   */
  boolean updateConfReplaceRegExp(String file, String match, String replace, boolean byline = false, boolean useSimpleReplace = false) {
    String ret = configDirs.find { confDir ->
      File confFile = new File(getServerRoot() + platform.sep + confDir + platform.sep + file)
      boolean res = updateFileReplaceRegExp(confFile.absolutePath, match, replace, byline, useSimpleReplace)
      return res
    }
    return (ret != null)
  }

  /**
   * Replace config file reg. expression.
   */
  boolean updateBinReplaceRegExp(String file, String match, String replace, boolean byline = false, boolean useSimpleReplace = false) {
    File binFile = new File(binDir + platform.sep + file)
    boolean res = updateFileReplaceRegExp(binFile.absolutePath, match, replace, byline, useSimpleReplace)
    return res
  }

  /**
   * Updates bin by putting text to specified position
   */
  void updateBinByInsertingTextToPositionInFile(String file, String textToInsert, int position) {
    File binFile = new File(binDir + platform.sep + file)
    JBFile.insertTextToSpecifiedPositionInFile(binFile.absolutePath, textToInsert, position)
  }

  /**
   * Updates conf by putting text to specified position
   */
  void updateConfByInsertingTextToPositionInFile(String file, String textToInsert, int position) {
    configDirs.each { confDir ->
      File confFile = new File(getServerRoot() + platform.sep + confDir + platform.sep + file)
      JBFile.insertTextToSpecifiedPositionInFile(confFile.absolutePath, textToInsert, position)
    }
  }

  /**
   * Replace file regular expression
   */
  boolean updateFileReplaceRegExp(String path, String match, String replace, boolean byline = false, boolean useSimpleReplace = false) {
    File file = new File(path)

    if (file.exists()) {
      String fileText = JBFile.read(file)
      if (fileText.contains(replace) && !((fileText =~ match).find())) {
        log.warn('ALREADY UPDATED - skipping {}', file.absolutePath)
        return false
      }
      if (!fileText.contains(match) && useSimpleReplace) {
        log.warn('NOTHING TO UPDATE - File ' + file.absolutePath + ' does not contain string:\n' + match)
        return false
      }
      log.debug('Updating file ' + file.absolutePath)
      log.trace('BEFORE UPDATE:')
      log.trace('-----------------------------------------------')
      // show only text before change
      log.trace(match)
      log.trace('-----------------------------------------------')

      if (useSimpleReplace) JBFile.replace(file, match, replace)
      else JBFile.replaceregexp(file, match, replace, byline)

      fileText = JBFile.read(file)

      if (fileText.contains(replace)) {
        log.trace('AFTER UPDATE:')
        log.trace('-----------------------------------------------')
        // show only text after change
        log.trace(replace)
        log.trace('-----------------------------------------------')
        return true
      } else {
        log.warn('PROBABLY ERROR AFTER UPDATE - File ' + file.absolutePath + ' does not contain string ' + replace)
        return false
      }
    } else {
      log.debug('Unable to update file ' + file.absolutePath + ' as it does not exist')
      return false
    }
  }

  /**
   * Check log files for ERRORS and WARNINGS
   * TODO add offsets for logs (or delete logs before each test - replace symlinks)
   */
  List<String> verifyLogs() {
    def affectedLines = []

    logDirs.each { logDir ->
      new File(getServerRoot() + platform.sep + logDir).eachFile { File logFile ->
        log.debug("Scanning log file {}", logFile.absoluteFile)
        if (!logFile.canRead()) JBFile.makeAccessible(logFile)
        logFile.eachLine { line, number ->
          log.trace(line)

          if (Boolean.valueOf(Library.getUniversalProperty('logs.skip.warnings', false))) {
            if (line ==~ /(?i)(.*)ERROR(.*)|(.*)SEVERE(.*)/) {
              affectedLines << logFile.getAbsolutePath() + " " + line
            }
          } else {
            if (line ==~ /(?i)(.*)ERROR(.*)|(.*)WARN(.*)|(.*)SEVERE(.*)/) {
              affectedLines << logFile.getAbsolutePath() + " " + line
            }
          }

        }
      }
    }
    return affectedLines
  }

  /**
   * Check if logs files contains value
   * TODO add offsets for logs (or delete logs before each test - replace symlinks)
   */
  boolean logsContains(regexp) {
    def res = false
    def partRes = false

    logDirs.each { logDir ->
      new File(getServerRoot() + platform.sep + logDir).eachFile { File logFile ->
        log.debug("Scanning log file " + logFile.absoluteFile + " for value: " + regexp.toString())
        logFile.eachLine { line ->
          partRes = (line ==~ regexp)
          res = res || partRes
          log.trace(line)
          if (partRes) log.debug('Found')
        }
      }
    }

    return res
  }

  boolean logExists(logFileNeedle) {

    for (String logDir : logDirs) {
      String path = getServerRoot() + platform.sep + logDir + platform.sep + logFileNeedle

      log.debug("Searching log file $path")

      if (JBFile.isExistingFile(new File(path))) {
        return true
      }
    }

    return false
  }

  List<String> returnMatchingLines(regexp) {
    List result = []
    logDirs.each { logDir ->
      new File(getServerRoot() + platform.sep + logDir).eachFile { File logFile ->
        result.addAll(logFile.readLines().findAll { it =~ regexp })
      }
    }
    return result
  }

  Boolean logContains(regexp) {
    for (String logDir : logDirs) {
      File dir = new File(getServerRoot() + platform.sep + logDir)

      if (!dir.exists() && new File(logDir).isAbsolute()) {
        dir = new File(logDir)
      }

      for (File logFile : dir.listFiles()) {
        if (!JBFile.isExistingFile(logFile)) {
          log.debug("Log file ${logFile.getAbsolutePath()} does not exist.")
          continue
        }
        if (JBFile.hasMatchingLine(logFile, regexp)) {
          return true
        }
      }
    }
    return false
  }

  boolean logContains(logFileNeedle, regexp) {
    for (String logDir : logDirs) {
      File logFile = new File(
              getServerRoot() + platform.sep + logDir + platform.sep + logFileNeedle)
      if (!JBFile.isExistingFile(logFile)) {
        log.debug("Log file ${logFile.getAbsolutePath()} does not exist.")
        continue
      }
      if (JBFile.hasMatchingLine(logFile, regexp)) {
        return true
      }
    }
    return false
  }

  /**
   * Waits for log file to emerge and contain provided regexp. It waits in maximum for specified timeout.
   *
   * It returns TRUE if the expected regexp was found in specified timeout, false otherwise.
   */
  boolean waitUntilLogContains(String logFileName, regexp, long timeout = 60, TimeUnit timeUnit = TimeUnit.SECONDS) {

    final long endTime = System.currentTimeMillis() + timeUnit.toMillis(timeout)
    // sleep is from interval <100,1000>
    final sleep = Math.min(1000, Math.max(100,timeUnit.toMillis(timeout).intdiv(60)))

    //  Wait until the existing log file is found
    while (System.currentTimeMillis() < endTime ) {
      if (logContains(logFileName, regexp)) {
        return true
      }
      Library.sleep(sleep)
    }
    return false
  }

  /**
   * Returns number of regexp matches in a log file. 
   */
  int countOccurrencesInLog(logFileNeedle, regexp) {
    int occurrences = 0

    logDirs.each { logDir ->
      new File(getServerRoot() + platform.sep + logDir).eachFileMatch(~logFileNeedle) { File logFile ->
        log.debug("Searching log file ${logFile} check for ${regexp} match")
        occurrences += JBFile.fileRegexpOccurrences(logFile, regexp)
      }
    }

    return occurrences
  }

  void archiveLogs(String testName, String serverId = '') {
    log.debug('Archiving of all log files of server {}', serverId)

    def s = platform.sep
    def targetDir
    def simpleServerName = this.getClass().getSimpleName() + '-' + version + "${s}${serverId}"
    def simpleLogDir
    def reportDir = new File(Library.getRootPath(), "${s}target${s}jboss-reports${s}logs-archive")

    logDirs.each { logDir ->
      File logDirAsFile = new File(getServerRoot() + "${s}${logDir}")
      log.trace("Gonna backup:${logDirAsFile.getAbsolutePath()}, does it exist? ${logDirAsFile.exists()} Is it a directory? ${logDirAsFile.isDirectory()}")
      if (logDirAsFile.exists() && logDirAsFile.isDirectory()) {
        logDirAsFile.eachFile { File logFile ->
          simpleLogDir = new File(getServerRoot() + "${s}${logDir}").getAbsolutePath().substring(new File(getServerRoot() + "${s}${logDir}").getAbsolutePath().lastIndexOf(s) + 1)
          targetDir = new File(reportDir.getAbsolutePath(), "${s}${testName}${s}${simpleServerName}${s}${simpleLogDir}")
          log.trace("Gonna backup: simpleLogDir: ${simpleLogDir} to targetDir: ${targetDir}")
          // Creates a directory. Also non-existent parent directories are created, when necessary. Does nothing if the directory already exist.
          JBFile.copy(logFile, targetDir)
          if (JBFile.useAdminPrivileges) { //If run with sudo there is possibility that logs won't be accessible to user
            JBFile.makeAccessible(targetDir)
          }
        }
      }
    }

    log.trace('Archiving of all log files successfully done')
  }

  void archiveConfs(String testName, String serverId = '') {
    log.debug('Archiving of all conf files of server {}', serverId)

    def s = platform.sep
    def targetDir
    def simpleServerName = this.getClass().getSimpleName() + '-' + version + "${s}${serverId}"
    def simpleConfDir
    def reportDir = new File(Library.getRootPath(), "${s}target${s}jboss-reports${s}confs-archive")

    configDirs.each { confDir ->
      File confDirAsFile = new File(getServerRoot() + "${s}${confDir}")
      if (confDirAsFile.exists() && confDirAsFile.isDirectory()) {
        confDirAsFile.eachFile { File confFile ->
          simpleConfDir = new File(getServerRoot() + "${s}${confFile}").getAbsolutePath().substring(new File(getServerRoot() + "${s}${confFile}").getAbsolutePath().lastIndexOf(s) + 1)
          targetDir = new File(reportDir.getAbsolutePath(), "${s}${testName}${s}${simpleServerName}${s}${simpleConfDir}")
          // Creates a directory. Also non-existent parent directories are created, when necessary. Does nothing if the directory already exist.
          JBFile.copy(confFile, targetDir, false)
        }
      }
    }

    log.trace('Archiving of all conf files successfully done')
  }

  void cleanLogs() {
    log.debug('Removing all log files of server {}', serverId)

    File logFile2Del
    logDirs.each { logDir ->
      logFile2Del = new File(getServerRoot() + platform.sep + logDir)
      if (logFile2Del.exists()) {
        logFile2Del.eachFile { logFile ->
          JBFile.delete(logFile)
        }
      }
    }

    log.debug('Removing of all config files for server {} successfully done', serverId)
  }

  String getBinDirFullPath() {
    return getServerRoot() + binPath
  }

  String getServerRoot() {
    return basedir
  }

  Boolean checkmainHttpPort() {
    return Library.checkTcpPort(host, mainHttpPort)
  }

  Boolean checkshutdownPort() {
    return Library.checkTcpPort(host, shutdownPort)
  }

  int getmainHttpRespStatCode() {
    return Library.getHttpStatusCode(getUrl() /*."http://$host:$mainHttpPort".toURL()*/)
  }

  int getHttpRespStatCode(String urlPath = '') {
    return Library.getHttpStatusCode(getUrl(urlPath))
  }


  URL getUrl(String path = '', Boolean https = false, Integer port = null) {
    if (host == "0.0.0.0") host = "127.0.0.1"
    if (host == "::") host = "::1"

    def addr_host = host.contains(':') ? '[' + host + ']' : host
    def usePort = port
    if (usePort == null) {
      usePort = (https) ? mainHttpsPort : mainHttpPort
    }
    def protocol = (https) ? "https" : "http"
    def url = "${protocol}://${addr_host}:${usePort}"
    if (path) url += '/' + path
    return url.toURL()
  }

  Boolean verifyUrl(int code = 200, String content = "", long timeout = 30000, Boolean allowRedirects = true,
                    Boolean setReqProp = false, String reqKey = "", String reqValue = "", String urlPath = '', WebClient webClient = null,
                    Boolean clearWebClientCache = false, Boolean reThrowAnyException = false, Boolean contentAsRegex = false) {
    return Library.verifyUrl(getUrl(urlPath), code, content, timeout, allowRedirects, setReqProp, reqKey, reqValue, webClient, clearWebClientCache, reThrowAnyException, contentAsRegex)
  }

  Boolean verifySecuredUrl(int code = 200, String content = "", long timeout = 30000, Boolean allowRedirects = true, Boolean setReqProp = false,
                           String reqKey = "", String reqValue = "", String urlPath = '') {
    System.setProperty('javax.net.ssl.trustStore', getKeystorePath())
    System.setProperty('javax.net.ssl.trustStoreType', getKeystoreType())
    System.setProperty('javax.net.ssl.trustStorePassword', getSslKeystorePassword())
    def ret = Library.verifyUrl(getUrl(urlPath, true), code, content, timeout, allowRedirects, setReqProp, reqKey, reqValue)
    System.clearProperty('javax.net.ssl.trustStore')
    System.clearProperty('javax.net.ssl.trustStoreType')
    System.clearProperty('javax.net.ssl.trustStorePassword')
    return ret
  }

  /**
   * Checks if files exists in a basepath
   *
   * @return existing files
   */
  List isFileExists(List files) {
    return Library.isFileExists(files, this.basedir)
  }

  /**
   * If run as changed, than run context must be changed too. Always.
   * This is groovy stuff - we override groove generated setter.
   */
  void setRunAs(String runAs) {
    this.runAs = runAs
    this.runContext = createRunContext()
  }

  /**
   * Get general user, under whom run servers.
   */
  String loadRunAs() {
    return Library.getUniversalProperty('server.run.as', '')
  }

  /**
   * TODO refactor to Cmd
   */
  String createRunContext() {
    def res = ''

    if (!platform.isWindows()) {
      if (platform.isSolaris()) res = (this.runAs) ? "su $runAs -c " : ''
      else res = (this.runAs) ? "su - $runAs -c " : ''
    }

    return res
  }

  /**
   * Get CPU load of server
   * Suported only on RHEL
   * @param ppid , true to get sum of loads of children, false otherwise
   * @return Actual cpu load of the server, -1 if platform is not Rhel
   */
  double actualCPULoad(boolean ppid = true) {
    if (platform.isRHEL()) {
      log.debug("Gonna run ps. with $pid to retrieve actual CPU load")
      String ppidOrPid = ppid ? '--ppid' : '--pid'
      Process p = ["ps", "--no-heading", "-o", "pcpu", ppidOrPid, getPid()].execute()
      p.waitFor()
      double load = 0d
      String output = p.text
      (output.split(DefaultProperties.NL) as List).each { String line ->
        try {
          load += Double.parseDouble(line.trim())
        } catch (NumberFormatException ex) {
          throw new RuntimeException(ex)
        }
      }
      log.debug("Sum load for ${pid} was ${load}.")
      return load
    } else {
      log.error("retrieving CPU load is not supported/implemented on this OS, returning -1")
      return -1
    }
  }

  boolean equals(Object o) {
    if (this.is(o)) return true
    if (!(o instanceof ServerAbstract)) return false

    ServerAbstract that = (ServerAbstract) o

    return serverId == that.serverId
  }

  int hashCode() {
    return (serverId != null ? serverId.hashCode() : 0)
  }

  String getServerId() {
    return serverId
  }

  void setServerId(final String serverId) {
    this.serverId = serverId
    setDefault() // we need serverId to propagate to fields being based on serverId or processCode
    log.debug("Called setServerId(${serverId}), processCode is now: ${this.processCode}, serverId is now: ${this.serverId}")
  }
}

