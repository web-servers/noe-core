package noe.common.utils

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties

/**
 *
 * @author Jiri Sedlacek <jsedlace@redhat.com>
 * @author Jan Stefl     <jstefl@redhat.com>
 *
 */
@Slf4j
class Platform {
  public static final PLATFORM_i386 = 32
  public static final PLATFORM_x64 = 64
  public static final PLATFORM_SPARC64 = 66

  String osName
  String osArch
  Integer solPreferredArch
  String osVersion
  String archModel
  String sep = File.separator
  String nl = DefaultProperties.NL
  String pathsep = File.pathSeparator
  String tmpDir
  String homeDir = System.getProperty("user.home")
  String actualUser = System.getProperty("user.name")

  Platform() {
    this.osName = System.getProperty('os.name')
    this.osArch = System.getProperty('os.arch')
    this.osVersion = System.getProperty('os.version')
    this.archModel = System.getProperty('sun.arch.data.model')
    this.solPreferredArch = Integer.valueOf(Library.getUniversalProperty('solaris.preferred.arch', '32'))
    if (isWindows()) {
      if (new File("C:\\temp").exists()) {
        this.tmpDir = "C:\\temp"
      } else {
        if (new File("C:\\tmp").exists()) {
          this.tmpDir = "C:\\tmp"
        } else {
          this.tmpDir = System.getProperty("java.io.tmpdir")
        }
      }
    } else {
      this.tmpDir = System.getProperty("java.io.tmpdir")
    }
  }

  String toString() {
    "${osName} ${osVersion} ${osArch}"
  }

  boolean isWindows() {
    return (osName ==~ /.*[Ww]indows.*/)
  }

  boolean isLinux() {
    return (osName ==~ /[Ll]inux.*/)
  }

  boolean isRHEL() {
    return isLinux()
  }

  boolean isSolaris() {
    return (osName == 'SunOS')
  }

  boolean isHP() {
    return (osName == 'HP-UX')
  }

  boolean isMac() {
    return osName.contains("Mac OS")
  }

  boolean isAix() {
    return osName.contains("AIX")
  }

  boolean isX64() {
    return (osArch == 'amd64') || (isSolaris() && solPreferredArch == PLATFORM_x64) || (osArch == 'IA64N')
  }

  boolean isIA64() {
    return (osArch == 'IA64N')
  }

  boolean isX86() {
    return (osArch == 'x386') || (osArch == 'x86') || (osArch == 'i386') || (osArch == 'i686') || (isSolaris() && solPreferredArch == PLATFORM_i386)
  }

  boolean isSparc() {
    return (osArch == 'sparc' || osArch == 'sparcv9')
  }

  boolean isSparc64() {
    return (isSparc() && (solPreferredArch == PLATFORM_SPARC64 || isX64()))
  }

  boolean isPpc64() {
    return osArch == 'ppc64'
  }

  boolean isRHEL4() {
    return isRHEL() && (osVersion ==~ /.*EL[^5678][a-zA-Z]*/)
  }

  boolean isRHEL5() {
    return isRHEL() && (osVersion ==~ /.*el5.*/)
  }

  boolean isRHEL6() {
    return isRHEL() && (osVersion ==~ /.*el6.*/)
  }

  boolean isRHEL7() {
    return (isRHEL() && (osVersion ==~ /.*el7.*/)) || forceRhel7()
  }

  boolean isRHEL8() {
    return (isRHEL() && (osVersion ==~ /.*el8.*/)) || forceRhel8()
  }

  boolean isSolaris11() {
    return isSolaris() && (osVersion ==~ /5\.11/)
  }

  boolean isSolaris10() {
    return isSolaris() && (osVersion ==~ /5\.10/)
  }

  boolean isSolaris9() {
    return isSolaris() && (osVersion ==~ /5\.9/)
  }

  boolean isHP11() {
    return isHP() && (osName ==~ /.*11.*/)
  }

  public String getScriptSuffix() {
    return isWindows() ? 'bat' : 'sh'
  }

  /**
   * @return number of CPUs/cores available on the machine or -1 if unknown
   */
  public int numberOfCpus() {
    int numOfCpus = -1;
    if (this.isSolaris() && this.isSparc()) {
      def p = ['/bin/sh', '-c', 'kstat cpu_info | grep instance: | uniq | wc -l'].execute()
      if (p.waitFor() == 0) {
        def result = p.text.trim()
        numOfCpus = Integer.valueOf(result)
      }
    } else {
      log.warn("Not implemented for the current platform => -1 should be returned")
    }
    return numOfCpus;
  }

  /**
   * As on Docker, the 'os.name' of the system is determined by the host OS and not the container one, to make tests
   * work it is necessary to mock that we are, indeed, using RHEL7 machine.
   * @return value of the 'force.rhel.7' property
   */
  private boolean forceRhel7() {
    return Boolean.parseBoolean(Library.getUniversalProperty('force.rhel.7', 'false'))
  }

  /**
   * As on Docker, the 'os.name' of the system is determined by the host OS and not the container one, to make tests
   * work it is necessary to mock that we are, indeed, using RHEL8 machine.
   * @return value of the 'force.rhel.8' property
   */
  private boolean forceRhel8() {
    return Boolean.parseBoolean(Library.getUniversalProperty('force.rhel.8', 'false'))
  }
}
