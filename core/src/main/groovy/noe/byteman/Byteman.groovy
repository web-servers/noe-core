package noe.byteman

import groovy.util.logging.Slf4j
import noe.common.utils.JBFile
import noe.common.utils.PathHelper

/**
 * Universal byteman configuration allowing generation of byteman specific JAVA_OPTS.
 *
 * Byteman class supports fluent API, so you can set everything and as the final step use `generateBytemanJavaOpts()`
 * for generating String containing -javaagent specific JAVA_OPTS.
 *
 * To revert all configuration call method `flush()`.
 *
 * Use example:
 * <code>
 *   String JAVA_OPTS = new Byteman()
 *     .script([bytemanScriptFile1, bytemanScriptFile2])
 *     .sys([systemJarFile])
 *     .generateBytemanJavaOpts()
 * </code>
 *
 * Byteman needs for proper working byteman.jar file fetched as maven dependency and stored during compilation inside
 * resources folder in noe-core maven artifact. Byteman.jar is copied into system folder `${java.io.tmpdir}/noe/byteman`
 * in runtime due to accessibility for various system users/accounts used during testing
 * (eg. tests run under user Hudson, but tomcat server is started as user Tomcat).
 * Byteman.jar location is used during generating -javaagent specific JAVA_OPTS.
 *
 * @link http://downloads.jboss.org/byteman/4.0.4/byteman-programmers-guide.html
 */
@Slf4j
class Byteman {
  public static final String DEFAULT_ADDRESS = "localhost"
  public static final int DEFAULT_PORT = 9091
  public static final boolean DEFAULT_POLICY = false
  public static final boolean DEFAULT_LISTENER = true

  protected static String BYTEMAN_JAR_NAME = "byteman.jar"
  private File bytemanJarFile
  private int bytemanPort
  private boolean bytemanListener
  private boolean bytemanPolicy
  private List<File> bytemanScriptFileList
  private List<File> bytemanBootFileList
  private List<File> bytemanSysFileList
  private Map<String, String> bytemanPropertiesMap

  Byteman() {
    this(prepareBytemanJar())
  }

  /**
   * Notice that it is protected to not do mess within API, but it is available for testing within same package.
   */
  protected Byteman(File bytemanFile) {
    bytemanJarFile = bytemanFile

    log.debug("Creating byteman instance with path to byteman jar: " + bytemanJarFile)

    if (!bytemanJarFile.exists()) {
      throw new FileNotFoundException("File " + bytemanJarFile + " doesn't exist.")
    }

    bytemanScriptFileList = new LinkedList<File>()
    bytemanBootFileList = new LinkedList<File>()
    bytemanSysFileList = new LinkedList<File>()
    bytemanPropertiesMap = new LinkedHashMap<String, String>()
    bytemanPort = DEFAULT_PORT
    bytemanListener = DEFAULT_LISTENER
    bytemanPolicy = DEFAULT_POLICY
  }

  /**
   * Copies byteman.jar to folder `${java.io.tmpdir}/noe/byteman` to be accessible for all users/accounts
   * present in the testing enviroment due to RPM support as servers are running under different users then testing one.
   */
  private static File prepareBytemanJar() {
    String bytemanStringDir = PathHelper.join(System.getProperty("java.io.tmpdir"), "noe", "byteman")
    File bytemanDir = new File(bytemanStringDir)
    File bytemanJarFile = new File(bytemanDir, BYTEMAN_JAR_NAME)

    if (!bytemanJarFile.exists()) {
      if (!bytemanDir.exists()) {
        JBFile.mkdir(bytemanDir)
      }

      JBFile.makeAccessible(bytemanDir)
      String resourcesPath = "/noe/byteman/"  //resources jar path is always separated by /
      URL bytemanURL = this.getClass().getResource(resourcesPath + BYTEMAN_JAR_NAME)
      File bytemanFile = new File(bytemanURL.toURI())
      JBFile.copy(bytemanFile, bytemanDir)
    }

    return bytemanJarFile
  }

  /**
   * This option causes the agent to read the rules in scriptFileList and apply them to subsequently loaded classes.
   * Multiple script arguments may be provided to ensure that more than one rule set is installed.
   * It is possible to start the agent with no initial script arguments but this only makes sense
   * if the listener option is supplied with value true (set as default value).
   */
  Byteman script(List<File> scriptFileList) {
    bytemanScriptFileList.addAll(scriptFileList)

    return this
  }

  /**
   * When set to true (set true by default) this option causes the agent to start a listener thread at startup. The listener can be talked to
   * using the `bmsubmit` script, either to provide listings of rule applications performed by the agent.
   * If set to true, option uses `port` option to definition of listening port (default 9091).
   */
  Byteman listener(boolean listener) {
    this.bytemanListener = listener

    return this
  }

  /**
   * This option selects the port used by the agent listener when opening a server socket to listen on.
   * If not supplied the port defaults to 9091.
   *
   * @throws IllegalArgumentException if port is not positive integer value
   */
  Byteman port(int port) {
    if (port <= 0) {
      throw new IllegalArgumentException("Only positive integer is allowed.")
    }
    this.bytemanPort = port

    return this
  }

  /**
   * This option provides a similar facility to the `sys()` method but it ensures that the classes contained in the jar file
   * are loaded by the bootstrap class loader. This is only significant when rules try to inject code into JVM classes
   * which are loaded by the bootstrap class loader (which is a parent of the system loader).
   *
   * @param `bootJarPathList` is a List of paths to to a jar file to be added to the JVM bootstrap class path.
   */
  Byteman boot(List<File> bootJarFileList) {
    bytemanBootFileList.addAll(bootJarFileList)

    return this
  }

  /**
   * This option makes classes contained in the jar file available for use when type checking, compiling and executing rule
   * conditions and actions. It provides a useful way to ensure that Helper classes mentioned in rules are able to be resolved.
   * If a rule’s trigger class is loaded by some other class loader this loader will normally have the system loader as
   * a parent so references to the Helper class should resolve correctly.
   *
   * @param `systemJarPathList` is a List of paths to to a jar file to be added to the JVM system class path.
   */
  Byteman sys(List<File> systemJarFileList) {
    bytemanSysFileList.addAll(systemJarFileList)

    return this
  }

  /**
   * When set to true this option causes the agent to install an access all areas security policy for the Byteman agent code.
   * This may be necessary when the JVM is running an application employing a security manager which imposes access
   * restrictions (this includes recent versions of JBoss Wildfly/EAP).
   */
  Byteman policy(boolean policy) {
    this.bytemanPolicy = policy

    return this
  }

  /**
   * Used for setting System properties.
   *
   * @param `props` name=value where name identifies a System property to be set to value or to the empty String
   * if no value is provided. Note that property names must begin with the prefix “`org.jboss.byteman'.”.
   */
  Byteman prop(Map<String, String> props) {
    bytemanPropertiesMap.putAll(props)

    return this
  }

  /**
   * Composing valid -javaagent options with stored values.
   *
   * @return something like:
   * -javaagent:/tmp/noe/byteman/byteman.jar=boot:/tmp/noe/byteman/byteman.jar,listener:true,port:9091,script:/opt/tomcat.btm,sys:/opt/karmor.jar
   */
  String generateBytemanJavaOpts() {
    StringBuilder javaAgentPropertyBuilder = new StringBuilder()
    javaAgentPropertyBuilder.append("-javaagent:" + bytemanJarFile.getAbsolutePath() + "=boot:" + bytemanJarFile.getAbsolutePath())

    if (bytemanListener) {
      javaAgentPropertyBuilder.append(",listener:" + bytemanListener + ",port:" + bytemanPort)
    }

    if (bytemanPolicy) {
      javaAgentPropertyBuilder.append(",policy:" + bytemanPolicy)
    }

    javaAgentPropertyBuilder.append(generateCmdArg(bytemanScriptFileList, "script"))
    javaAgentPropertyBuilder.append(generateCmdArg(bytemanBootFileList, "boot"))
    javaAgentPropertyBuilder.append(generateCmdArg(bytemanSysFileList, "sys"))

    javaAgentPropertyBuilder.append(generatePropertiesMapArg())

    log.debug("Returning byteman JAVA_OPTS specific properties: " + javaAgentPropertyBuilder.toString())

    return javaAgentPropertyBuilder.toString()
  }

  private String generateCmdArg(List<File> paths, String bytemanPropName) {
    StringBuilder arg = new StringBuilder()

    if (!paths.isEmpty()) {
      for (File path : paths) {
        arg.append(",${bytemanPropName}:" + path.getAbsolutePath())
      }
    }

    return arg
  }

  private String generatePropertiesMapArg() {
    StringBuilder propertiesBuilder = new StringBuilder()

    if (!bytemanPropertiesMap.isEmpty()) {
      for (Map.Entry<String, String> entry : bytemanPropertiesMap.entrySet()) {
        String key = entry.getKey()
        String value = entry.getValue()
        propertiesBuilder.append(",prop:" + key + "=" + value)
      }
    }

    return propertiesBuilder
  }
}
