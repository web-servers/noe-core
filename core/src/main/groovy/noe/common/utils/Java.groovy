package noe.common.utils

import groovy.util.logging.Slf4j

/**
 * Java class - gets information about currently running JVM
 * @author Libor Fuka <lfuka@redhat.com>
 *
 */
@Slf4j
public class Java {

  public static String javaVersion = System.getProperty('java.version')
  public static String javaVendor = System.getProperty('java.vendor')
  public static String javaVmName = System.getProperty('java.vm.name')
  public static String javaVmInfo = System.getProperty('java.vm.info')
  public static final String javaHome = System.getenv('JAVA_HOME')
  public static final String serverJavaHome = System.getenv('SERVER_JAVA_HOME')
  private static final String javaHelperClassResource = "java/JavaVersion.java"
  private static boolean initialized = false

  static {
    if (serverJavaHome && !initialized) {
      if (!new File(serverJavaHome, "bin").exists()) {
        return
      }
      String javac = PathHelper.join(serverJavaHome, "bin", "javac")
      String java = PathHelper.join(serverJavaHome, "bin", "java")
      Library.copyResourceTo(javaHelperClassResource, new File("."))
      Cmd.executeCommandConsumeStreams([javac, "JavaVersion.java"])
      javaVersion = Cmd.executeCommandConsumeStreams([java, "JavaVersion", "-version"])["stdOut"].trim()
      javaVendor = Cmd.executeCommandConsumeStreams([java, "JavaVersion", "-vendor"])["stdOut"].trim()
      javaVmName = Cmd.executeCommandConsumeStreams([java, "JavaVersion", "-vmname"])["stdOut"].trim()
      javaVmInfo = Cmd.executeCommandConsumeStreams([java, "JavaVersion", "-vminfo"])["stdOut"].trim()
      initialized = true
    }
  }

  String toString() {
    "${javaVersion} ${javaVendor} ${javaVmName} ${javaVmInfo}"
  }

  static boolean isJdk16() {
    return (javaVersion ==~ /^1\.6.*/)
  }

  static  boolean isJdk17() {
    return (javaVersion ==~ /^1\.7.*/)
  }

  static boolean isJdk18() {
    return (javaVersion ==~ /^1\.8.*/)
  }

  /**
   * Relates to http://openjdk.java.net/jeps/223
   *
   * @return true if used Java is version 9
   */
  static boolean isJdk19() {
    return (javaVersion ==~ /^9[\.\-\+].*/)
  }

  static boolean isJdk11() {
    return (javaVersion ==~ /^11[\.\-\+].*/)
  }

  /**
   * Are we running on minimumJDKVersion ?
   *
   * @param minimumJDKVersion JDK version (example: '1.7')
   * @return true if we are running on minimumJDKVersion or higher
   */
  static boolean isJdk1xOrHigher(String minimumJDKVersion) {
    int usedJavaMajorVersion
    int minimumJavaMajorVersion

    // In long form of format introduced since Java 9, there might be used '-' and '+' as a delimiter, e.g.: 9-ea+19.
    String myJavaVersion = javaVersion.replaceAll('-', '.').replaceAll('\\+', '.')
    List<String> javaVersionTokenize = myJavaVersion.tokenize('.')
    List<String> minimumVersionTokenize = minimumJDKVersion.tokenize('.')

    try {
      // Get used Java major version number
      if (javaVersion.startsWith("1.")) {
        // Old version format used
        usedJavaMajorVersion = Integer.parseInt(javaVersionTokenize[1])
      } else {
        // Currently used java uses new version format introduced since Java 9
        usedJavaMajorVersion = Integer.parseInt(javaVersionTokenize[0])
      }

      if (minimumJDKVersion.startsWith("1.")) {
        // Old version format used
        minimumJavaMajorVersion = Integer.parseInt(minimumVersionTokenize[1])
      } else {
        // Provided version is in new format introduced since Java 9
        minimumJavaMajorVersion = Integer.parseInt(minimumVersionTokenize[0])
      }

      return (usedJavaMajorVersion >= minimumJavaMajorVersion)
    } catch (NumberFormatException e) {
      log.error(printStackTrace())
      return false
    }
  }

  static boolean isOracleJDK() {
    return (javaVendor ==~ /Oracle.*/)
  }

  static boolean isOpenJDK() {
    return (javaVmName ==~ /OpenJDK.*/)
  }

  static boolean isIBMJDK() {
    return (javaVendor ==~ /IBM.*/)
  }

  static boolean is64bitJava() {
    if (isIBMJDK()) {
      return javaVmInfo.contains('-64')
    } else {
      return (javaVmName ==~ /.*64-Bit.*/)
    }
  }

  static void compileOneJavaSource(String source, File dir, List javacOptions = []) {
    def platform = new Platform()
    def s = platform.sep
    def javacBin = (platform.isWindows()) ? 'javac.exe' : 'javac'
    def javacFullPath = "${javaHome}${s}bin${s}${javacBin}"
    def javac

    if (javaHome != null && new File(javacFullPath).exists()) {
      javac = javacFullPath
    } else {
      javac = javacBin
    }

    def javacCmd = [javac]
    if (javacOptions) {
      javacCmd.addAll(javacOptions)
    }
    javacCmd.add(source)

    Cmd.executeCommand(javacCmd, dir)
  }

}
