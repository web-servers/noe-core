package noe.jk.configure.modjk

import groovy.util.logging.Slf4j
import noe.common.utils.FileStateVault
import noe.common.utils.JBFile
import noe.common.utils.Platform
import noe.jk.configure.Configurator
import noe.jk.configure.UriWorkerMapProperties
import noe.jk.configure.WorkersProperties
import noe.server.Httpd

/**
 * Mod_jk.conf configuration file.
 *
 * IMPORTANT
 * <ul>
 *   <li>Not all directives are supported.</li>
 *   <li>New configuration file is created always (old is overwritten).</li>
 * </ul>
 *
 * @link https://tomcat.apache.org/connectors-doc/webserver_howto/apache.html
 */
@Slf4j
class ModJkConf implements Configurator {
  public static final String DEFAULT_NAME = 'mod_jk.conf'
  String fileName = DEFAULT_NAME

  Httpd httpd

  File jkModulePath
  File workersPropertiesFile
  File logFile
  String logLevel = "info"
  String logStampFormat = '"[%a %b %d %H:%M:%S %Y]"'
  String options = "+ForwardKeySize +ForwardURICompat -ForwardDirectories"
  String requestLogFormat = '"%w %V %T"'
  File jkMountFile
  File jkShmFile
  List<String> additionalLines

  FileStateVault vault = new FileStateVault()


  String getFileName() {
    return fileName
  }

  ModJkConf setFileName(String fileName) {
    this.fileName = fileName

    return this
  }

  Httpd getHttpd() {
    return httpd
  }

  ModJkConf setHttpd(Httpd httpd) {
    this.httpd = httpd

    return this
  }

  /**
   * If not specified, default value based on `facingServer` is provided.
   * If facing server is not set then `null` is returned.
   */
  File getJkModulePath() {
    if (jkModulePath != null) return jkModulePath
    else if (httpd != null) return new File(retrieveHttpdModulesDir(), "mod_jk.so")
    else return null
  }

  ModJkConf setJkModulePath(File jkModulePath) {
    this.jkModulePath = jkModulePath

    return this
  }

  /**
   * If not specified, default value based on `facingServer` is provided.
   * If facing server is not set then `null` is returned.
   */
  File getLogFile() {
    if (logFile != null) return logFile
    else if (httpd != null) return new File(retrieveHttpdLogsDir(), "mod_jk.log")
    else return null
  }

  ModJkConf setLogFile(File logFile) {
    this.logFile = logFile

    return this
  }

  String getLogLevel() {
    return logLevel
  }

  ModJkConf setLogLevel(String logLevel) {
    this.logLevel = logLevel

    return this
  }

  String getLogStampFormat() {
    return logStampFormat
  }

  ModJkConf setLogStampFormat(String logFormat) {
    this.logStampFormat = logFormat

    return this
  }

  String getOptions() {
    return options
  }

  ModJkConf setOptions(String options) {
    this.options = options

    return this
  }

  String getRequestLogFormat() {
    return requestLogFormat
  }

  ModJkConf setRequestLogFormat(String requestLogFormat) {
    this.requestLogFormat = requestLogFormat

    return this
  }

  List<String> getAdditionalLines() {
    return additionalLines
  }

  ModJkConf setAdditionalLines(List<String> additionalLines) {
    this.additionalLines = additionalLines

    return this
  }

  File getJkShmFile() {
    if (jkShmFile != null) return jkShmFile
    else if (httpd != null) return new File(retrieveHttpdLogsDir(), "jk.shm")
    else return null
  }

  ModJkConf setJkShmFile(File jkShmFile) {
    this.jkShmFile = jkShmFile

    return this
  }

  File getWorkersPropertiesFile() {
    if (workersPropertiesFile != null) return workersPropertiesFile
    else if (httpd != null) return new File(retrieveHttpdConfDeploymentPath(), WorkersProperties.DEFAULT_NAME)
    else return null
  }

  ModJkConf setWorkersPropertiesFile(File workersPropertiesFile) {
    this.workersPropertiesFile = workersPropertiesFile

    return this
  }

  /**
   * If not specified, default value based on `facingServer` is provided.
   * If facing server is not set then `null` is returned.
   */
  File getJkMountFile() {
    if (jkMountFile != null) return jkMountFile
    else if (httpd != null) return new File(retrieveHttpdConfDeploymentPath(), UriWorkerMapProperties.DEFAULT_NAME)
    else return null
  }

  ModJkConf setJkMountFile(File jkMountFile) {
    this.jkMountFile = jkMountFile

    return this
  }

  @Override
  ModJkConf configure() {
    File modJkConf = new File(retrieveHttpdConfDeploymentPath(), fileName)

    vault.push(modJkConf)
    JBFile.createFile(modJkConf, content())

    return this
  }

  @Override
  ModJkConf revertAll() {
    vault.popAll()

    return this
  }

  private String content() {
    String nl = new Platform().nl

    StringBuilder content = new StringBuilder(
        "LoadModule jk_module ${getJkModulePath().getCanonicalPath()}" + nl +
            "JkWorkersFile ${getWorkersPropertiesFile().getCanonicalPath()}" + nl +
            "JkLogFile ${getLogFile().getCanonicalPath()}" + nl +
            "JkLogLevel ${getLogLevel()}" + nl +
            "JkLogStampFormat ${getLogStampFormat()}" + nl +
            "JkOptions ${getOptions()}" + nl +
            "JkRequestLogFormat ${getRequestLogFormat()}" + nl +
            "JkMountFile ${getJkMountFile().getCanonicalPath()}" + nl +
            "JkShmFile ${getJkShmFile().getCanonicalPath()}" + nl
    )

    additionalLines.each { String line -> content.append(line) + nl }

    log.debug "${fileName}:"
    log.debug content.toString()

    return content.toString()
  }

  private File retrieveHttpdModulesDir() {
    return new File(httpd.getServerRoot(), 'modules')
  }

  private File retrieveHttpdLogsDir() {
    return new File(httpd.getServerRoot(), 'logs')
  }

  private File retrieveHttpdConfDeploymentPath() {
    return new File(httpd.getConfDeploymentPath())
  }

}
