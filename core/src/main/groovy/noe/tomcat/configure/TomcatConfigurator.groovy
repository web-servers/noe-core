package noe.tomcat.configure

import noe.byteman.Byteman
import noe.byteman.BytemanInstaller
import noe.common.utils.FileStateVault
import noe.common.utils.JBFile
import noe.common.utils.PathHelper
import noe.ews.server.tomcat.TomcatCommandUtils
import noe.server.Tomcat
import noe.tomcat.configure.envars.EnvVarsFileFactory
import noe.tomcat.configure.envars.ZipTomcatEnvVarsFile

/**
 * Universal Tomcat configuration.
 *
 * <ul>
 *  <li>Connectors</li>
 *  <li>Ports shifting</li>
 *  <li>JVM routes</li>
 *  <li>Custom config files manipulation</li>
 *  <li>JMX</li>
 * </ul>
 *
 * Since Tomcat 6
 *
 * Any configuration is backed-up to vault (`FileStateVault`).
 * For all configuration reverting to original state, call method `revertAllConfiguration()`.
 */
class TomcatConfigurator {

  private final Tomcat tomcatInstance
  private Map<File, Node> parsedConfigs = [:]
  private final FileStateVault configVault = new FileStateVault()


  TomcatConfigurator(Tomcat tomcatInstance) {
    this.tomcatInstance = tomcatInstance
  }

  // - Methods ----------------------------------------------------------------------------------------

  /**
   * Set JVM route
   */
  TomcatConfigurator jvmRoute(String jvmRoute) {
    File serverXml = getServerXml()

    if (serverXml?.exists()) {
      Node Server = getParsedConfig(serverXml)
      setParsedConfig(serverXml, new JvmRouteConfiguratorTomcat(Server).define(jvmRoute))
    } else {
      missingConfigFile('server.xml')
    }

    return configure()
  }

  /**
   * Set Tomcat http connector.
   *
   * This methods work with situation that there is 0 or 1 http connector.
   * More http connectors is not possible to handle from code because they
   * have no unique id. *
   *
   * If there is not http (non-secure) connector then new connector is created.
   *
   * Non secure http protocol is distinguished that attribute secure=false or is not set.
   *
   * Http connector has one of the following protocols and is not secured
   *   HTTP/1.1
   *   org.apache.coyote.http11.Http11Protocol - blocking Java connector
   *   org.apache.coyote.http11.Http11NioProtocol - non blocking Java connector
   *   org.apache.coyote.http11.Http11AprProtocol - the APR/native connector.
   *
   */
  TomcatConfigurator httpConnector(NonSecureHttpConnectorTomcat connector) {
    File serverXml = getServerXml()

    if (serverXml?.exists()) {
      Node Server = getParsedConfig(serverXml)
      setParsedConfig(serverXml, new ConnectorConfiguratorTomcat(Server).defineHttpConnector(connector))

      if (connector.getPort() > 0) tomcatInstance.mainHttpPort = connector.getPort()
      if (connector.getAddress() != null) tomcatInstance.host = connector.getAddress()
    } else {
      missingConfigFile('server.xml')
    }

    return configure()
  }

  /**
   * Set Tomcat secure https connector.
   *
   * This methods work with situation that there is 0 or 1 secure http connector.
   * More such connectors is not possible to handle from code because they have
   * no unique id.
   *
   * If there is no secure HTTP connector then new secure HTTP connector is created.
   *
   * Secure connector is distingushed that has attribute secure=true.
   *
   * Http connector has one of the following protocols and is secured
   *   HTTP/1.1
   *   org.apache.coyote.http11.Http11Protocol
   *   org.apache.coyote.http11.Http11NioProtocol
   *   org.apache.coyote.http11.Http11AprProtocol
   *
   */
  TomcatConfigurator httpsConnector(SecureHttpConnectorTomcat connector) {
    File serverXml = getServerXml()

    if (serverXml?.exists()) {
      Node Server = getParsedConfig(serverXml)
      setParsedConfig(serverXml, new ConnectorConfiguratorTomcat(Server).defineHttpsConnector(connector))

      if (connector.getPort() > 0) tomcatInstance.mainHttpsPort = connector.getPort()
      if (connector.getAddress() != null) tomcatInstance.host = connector.getAddress()
    } else {
      missingConfigFile('server.xml')
    }

    return configure()
  }

  /**
   * Set Tomcat AJP connector.
   *
   * This methods work with situation that there is 0 or 1 ajp connector.
   * More such connectors is not possible to handle from code because they have
   * no unique id.
   *
   * If there is not AJP connector then new AJP connector is created.
   *
   * Http connector has one of the following protocols and is not secured
   *   AJP/1.3
   *   org.apache.coyote.ajp.AjpProtocol - blocking Java connector
   *   org.apache.coyote.ajp.AjpNioProtocol - non blocking Java connector.
   *   org.apache.coyote.ajp.AjpAprProtocol - the APR/native connector.
   *
   */
  TomcatConfigurator ajpConnector(AjpConnectorTomcat connector) {
    File serverXml = getServerXml()

    if (serverXml?.exists()) {
      Node Server = getParsedConfig(serverXml)
      setParsedConfig(serverXml, new ConnectorConfiguratorTomcat(Server).defineAjpConnector(connector))

      if (connector.getPort() != null) tomcatInstance.ajpPort = connector.getPort()
      if (connector.getAddress() != null) tomcatInstance.host = connector.getAddress()
    } else {
      missingConfigFile('server.xml')
    }

    return configure()
  }

  /**
   * Shift bindings ports (http, https, ajp, shutdow-port) by offset.
   */
  TomcatConfigurator portOffset(int portOffset) {
    File serverXml = getServerXml()

    if (serverXml?.exists()) {
      Node Server = getParsedConfig(serverXml)

      int shiftedShutdownPort = tomcatInstance.getShutdownPort() + portOffset
      int shiftedHttpPort = tomcatInstance.getMainHttpPort() + portOffset
      int shiftedHttpsPort = tomcatInstance.getMainHttpsPort() + portOffset
      int shiftedAjpPort = tomcatInstance.getAjpPort() + portOffset

      shutdown(new ShutdownTomcat().setPort(shiftedShutdownPort))

      if (new ConnectorConfiguratorTomcat(Server).loadHttpConnectorSize() > 0) {
        httpConnector(new NonSecureHttpConnectorTomcat().setPort(shiftedHttpPort))
      }

      if (new ConnectorConfiguratorTomcat(Server).loadHttpsConnectorSize() > 0) {
        httpsConnector(new SecureHttpConnectorTomcat().setPort(shiftedHttpsPort))
      }

      if (new ConnectorConfiguratorTomcat(Server).loadAjpConnectorSize() > 0) {
        ajpConnector(new AjpConnectorTomcat().setPort(shiftedAjpPort))
      }
    } else {
      missingConfigFile('server.xml')
    }

    return this
  }

  /**
   * Sets shutdown server attributes
   *
   * @link https://tomcat.apache.org/tomcat-7.0-doc/config/server.html
   *
   */
  TomcatConfigurator shutdown(ShutdownTomcat shutdown) {
    File serverXml = getServerXml()

    if (serverXml?.exists()) {
      Node Server = getParsedConfig(serverXml)
      setParsedConfig(serverXml, new ShutdownConfiguratorTomcat(Server).define(shutdown))

      tomcatInstance.shutdownPort = shutdown.getPort()
    } else {
      missingConfigFile('server.xml')
    }

    return configure()
  }

  /**
   * Manipulate with JMX options
   *
   * @link http://tomcat.apache.org/tomcat-7.0-doc/monitoring.html
   *
   */
  TomcatConfigurator jmx(JmxTomcat jmx) {
    new JmxConfiguratorTomcat(tomcatInstance).define(jmx)

    if (jmx.port != null) {
      tomcatInstance.jmxPort = jmx.getPort()
    }

    return this
  }

  @Deprecated
  /**
   * Use `TomcatConfigurator.envVariableByAppend`
   */
  TomcatConfigurator appendVariableToSetEnv(String name, String value) {
    new ZipTomcatEnvVarsFile(tomcatInstance, configVault).appendVariable(name, value);

    return this
  }

  /**
   * Append environment variable into Tomcat configuration file where environment variables are stored.
   * Zip and RPMs distribution are supported.
   */
  TomcatConfigurator envVariableByAppend(String name, String value) {
    EnvVarsFileFactory.getInstance(tomcatInstance, configVault).appendVariable(name, value)

    return this
  }

  /**
   * Append byteman specific JAVA_OPTS into Tomcat configuration file via `envVariableByAppend()` method.
   *
   * For disabling byteman configuration, it is possible to call method `revertAllConfiguration()`,
   * however be aware that it removes all configuration changes applied.
   *
   * Byteman instance example:
   * new Byteman(new BytemanInstaller().prepareBytemanJar())
   *      .script([new File("bytemanScriptFile1.btm"), new File("bytemanScriptFile2.btm")])
   *      .sys([new File("systemJarFile.jar")])
   */
  TomcatConfigurator enableByteman(Byteman byteman) {
    envVariableByAppend("JAVA_OPTS", byteman.generateBytemanJavaOpts())

    return this
  }

  /**
   * Append byteman default JAVA_OPTS setup into Tomcat configuration file via `envVariableByAppend()` method.
   * Default setup: -javaagent:/tmp/noe/byteman/byteman.jar=boot:/tmp/noe/byteman/byteman.jar,listener:true,port:9091
   *
   * For disabling byteman configuration, it is possible to call method `revertAllConfiguration()`,
   * however be aware that it removes all configuration changes applied.
   */
  TomcatConfigurator enableByteman() {
    File bytemanJar = new BytemanInstaller().prepareBytemanJar()
    return enableByteman(new Byteman(bytemanJar))
  }

  /**
   * Returns parsed xml-configuration file.
   *
   * NOTE: This method doesn't back up the xml file!
   */
  Node retrieveXmlConfigurationFileParsed(String confName) {
    File confFile = retrieveConfFile(confName)

    if (confFile?.exists()) {
      return new XmlParser().parse(confFile)
    } else {
      throw new IllegalArgumentException("'${confFile}' does not exist in ${tomcatInstance.getConfigDirs()}/conf")
    }
  }

  /**
   * Persists parsed xml-configuration.
   * It updates existing file in `TOMCAT_HOME/conf` directory.
   *
   * NOTE: This method backups the original xml file.
   */
  TomcatConfigurator configureXmlConfigurationFile(String confName, Node parsedXml) {
    File confFile = retrieveConfFile(confName)

    if (confFile?.exists()) {
      configVault.push(confFile)
      printNodeToFile(confFile, parsedXml)
    } else {
      missingConfigFile('server.xml')
    }

    return this
  }

  /**
   * Returns parsed properties-configuration file.
   */
  Properties retrievePropertiesConfigurationFileParsed(String confName) {
    File confFile = retrieveConfFile(confName)

    if (confFile?.exists()) {
      Properties props = new Properties()
      confFile.newReader("us-ascii").withCloseable { BufferedReader inStr ->
        props.load(inStr)
      }
      return props
    } else {
      throw new IllegalArgumentException("'${confFile}' does not exist in ${tomcatInstance.getConfigDirs()}/conf")
    }
  }
  /**
   * Creates a specified TomcatUser in the tomcat-users.xml file
   * @param user TomcatUser instance you want to persist
   * @return this
   */
  TomcatConfigurator addUser(TomcatUser user) {
    String tomcatUsers = 'tomcat-users.xml'
    File confFile = retrieveConfFile(tomcatUsers)
    if (confFile?.exists()) {
      configVault.push(confFile)
      Map userAttributes = ['username' : user.username,
                            'password' : user.password,
                            'roles'    : user.parsedRoles]

      Node usersNode = getParsedConfig(confFile)
      usersNode.appendNode('user',userAttributes)
      printNodeToFile(confFile, usersNode)
    } else {
      missingConfigFile(tomcatUsers)
    }
    return this
  }

  /**
   * Adds the specified listener to server.xml
   * @param listenerFQCN the fully qualified class name of the listener
   * @return this
   */
  TomcatConfigurator addListener(String listenerFQCN) {
    String serverXml = 'server.xml'
    File confFile = retrieveConfFile(serverXml)
    if (confFile?.exists()) {
      configVault.push(confFile)
      Node serverNode = getParsedConfig(confFile)
      serverNode.appendNode('Listener', ['className' : listenerFQCN])
      printNodeToFile(confFile, serverNode)
    } else {
      missingConfigFile(serverXml)
    }
    return this
  }

  /**
   * Persists parsed properties-configuration.
   * The file will be created/updated in `TOMCAT_HOME/conf` directory.
   */
  TomcatConfigurator configurePropertiesConfigurationFile(String confName, Properties properties) {
    File confFile = retrieveConfFile(confName)

    if (confFile?.exists()) {
      configVault.push(confFile)

      confFile.newWriter("us-ascii", true).withCloseable { BufferedWriter outStr ->
        properties.store(outStr, null)
      }
    } else {
      missingConfigFile('server.xml')
    }

    return this
  }

  /**
   * Returns all modified configurations files to original state.
   */
  TomcatConfigurator revertAllConfiguration() {
    configVault.popAll()

    return this
  }

  TomcatConfigurator revertConfiguration(File config) {
    configVault.pop(config)

    return this
  }

  /**
   * Creates CATALINA_BASE on the filesystem, copies `TOMCAT_HOME/conf` and `TOMCAT_HOME/webapps` directories, and sets
   * CATALINA_BASE property in the setenv file.
   *
   * @param String path of CATALINA_BASE to be created
   * @return true if CATALINA_BASE has been created; otherwise false
   */
  boolean createNewInstanceUsingCatalinaBaseDir(String catalinaBasePath) {
    boolean success = true
    String confPath = tomcatInstance.getConfDeploymentPath()
    String deploymentPath = tomcatInstance.getDeploymentPath()
    File catalinaBaseFile = new File(catalinaBasePath)

    if (catalinaBaseFile.exists()) {
      JBFile.delete(catalinaBaseFile)
    }

    // Create CATALINA_BASE
    success &= JBFile.mkdir(catalinaBaseFile)
    // lack of temp could throw errors
    success &= JBFile.mkdir(new File(PathHelper.join(catalinaBasePath, "temp")))
    // lack of logs prevents startup.sh from starting
    success &= JBFile.mkdir(new File(PathHelper.join(catalinaBasePath, "logs")))
    success &= JBFile.copyDirectoryContent(new File(confPath), new File(catalinaBasePath, "conf"))
    success &= JBFile.copy(new File(deploymentPath), catalinaBaseFile)
    // Set CATALINA_BASE path in setenv
    appendVariableToSetEnv("CATALINA_BASE", catalinaBasePath)

    String tomcatUser = TomcatCommandUtils.getTomcatRunUser()
    if (tomcatUser) {
      JBFile.chown(tomcatUser, catalinaBaseFile)
    } else {
      JBFile.makeAccessible(catalinaBaseFile)
    }

    return success
  }

  /**
   * Persists configuration model stored in memory.
   * Model in memory will be cleared.
   */
  TomcatConfigurator configure() {
    parsedConfigs.each { File config, Node node ->
      configVault.push(config)
      printNodeToFile(config, node)
    }

    parsedConfigs.clear()

    return this
  }

  private File getServerXml() {
    return retrieveConfFile("server.xml")
  }

  private void setParsedConfig(File config, Node parsedConfig) {
    parsedConfigs[config] = parsedConfig
  }

  private Node getParsedConfig(File config) {
    if (!isConfigParsed(config)) {
      parsedConfigs[config] = new XmlParser().parse(config)
    }

    return parsedConfigs[config]
  }

  private boolean isConfigParsed(File config) {
    return parsedConfigs.containsKey(config)
  }

  /**
   *  NOTICE: Return null if file does not exists!
   */
  private File retrieveConfFile(String fileName) {
    List<File> files = tomcatInstance.retrieveConfFilesByName(fileName)

    return (files.size() > 0) ? files.first() : null
  }

  private void missingConfigFile(String fileName) {
    throw new IllegalArgumentException("'${fileName}' was not found in tomcat with id '${tomcatInstance.getServerId()}'")
  }

  /**
   * Use this method to write parsed Node into a file, and automatically close all streams as necessary. This is especially important
   * for Windows, where opened file descriptors may cause IOExceptions when other Java processes try to write into the file
   */
  private void printNodeToFile(File confFile, Node parsedXml) {
    new FileWriter(confFile).withCloseable { FileWriter fileWriter ->
      new PrintWriter(fileWriter).withCloseable { PrintWriter printWriter ->
        new XmlNodePrinter(printWriter).print(parsedXml)
      }
    }
  }
}
