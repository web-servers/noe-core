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

  /**
   * @deprecated since 0.17.2, please use {@link #isJdk6()} instead.
   * @return true if process is running on JDK 6 AKA JDK 1.6
   */
  static boolean isJdk16() {
    return isJdk6()
  }

  /**
   * @return true if process is running on JDK 6 AKA JDK 1.6
   */
  static boolean isJdk6() {
    return (javaVersion ==~ /^1\.6.*/)
  }

  /**
   * @deprecated since 0.17.2, please use {@link #isJdk7()} instead.
   * @return true if process is running on JDK 7 AKA JDK 1.7
   */
  static  boolean isJdk17() {
    return isJdk7()
  }

  /**
   * @return true if process is running on JDK 7 AKA JDK 1.7
   */
  static  boolean isJdk7() {
    return (javaVersion ==~ /^1\.7.*/)
  }

  /**
   * @deprecated since 0.17.2, please use {@link #isJdk8()} instead.
   * @return true if process is running on JDK 8 AKA JDK 1.8
   */
  static boolean isJdk18() {
    return isJdk8()
  }

  /**
   * @return true if process is running on JDK 8 AKA JDK 1.8
   */
  static boolean isJdk8() {
    return (javaVersion ==~ /^1\.8.*/)
  }

  /**
   * Relates to http://openjdk.java.net/jeps/223
   *
   * @deprecated since 0.17.2, please use {@link #isJdk9()} instead.
   * @return true if process is running on JDK 9
   */
  static boolean isJdk19() {
    return isJdk9()
  }

  /**
   * Relates to http://openjdk.java.net/jeps/223
   *
   * @return true if process is running on JDK 9
   */
  static boolean isJdk9() {
    return (javaVersion ==~ /^9[\.\-\+].*/)
  }

  /**
   * @return true if process is running on JDK 11
   */
  static boolean isJdk11() {
    return (javaVersion ==~ /^11[\.\-\+].*/)
  }

  /**
   * @return true if process is running on JDK 15
   */
  static boolean isJdk15() {
    return (javaVersion ==~ /^15[\.\-\+].*/)
  }

  /**
   * Are we running on minimumJDKVersion ?. This accepts both legacy (1.6, 1.7, etc.)
   * Java version format and also new one (7, 8, 9, etc.).
   *
   * @param minimumJDKVersion JDK version (example: '1.7')
   * @deprecated since 0.17.2, please use {@link #isJdkXOrHigher(java.lang.String)} instead.
   * @return true if we are running on minimumJDKVersion or higher
   */
  static boolean isJdk1xOrHigher(String minimumJDKVersion) {
    return isJdkXOrHigher(minimumJDKVersion)
  }

  /**
   * Are we running on minimumJDKVersion ?. This accepts both legacy (1.6, 1.7, etc.)
   * Java version format and also new one (7, 8, 9, etc.).
   *
   * @param minimumJDKVersion JDK version (example: '1.7' or '7')
   * @return true if we are running on minimumJDKVersion or higher
   */
  static boolean isJdkXOrHigher(String minimumJDKVersion) {
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
