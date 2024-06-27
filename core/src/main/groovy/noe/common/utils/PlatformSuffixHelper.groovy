package noe.common.utils

import groovy.util.logging.Slf4j

@Slf4j
class PlatformSuffixHelper {
  
  Platform platform
  
  PlatformSuffixHelper(Platform platform) {
    this.platform = platform
  }

  /**
   * JBCS/new style: jbcs-{product}-{upstream_version}-{coreservices version}-{build_tag}-{platform}-{arch}
   * For example: jbcs-openssl-1.0.2h-SP2-CR1-RHEL6-i686.zip
   *
   * For more information, contact Georgios Zaronikas Karagiannis
   *
   * @param oldStyle whether to use style for platform suffix used with EAP6 or new style
   * @return suffix corresponding to given platform, used mainly for counting proper zip file name
   */
  String suffixForCoreSvc(boolean oldStyle = true) {
    if (oldStyle) {
      return suffixForEAP6()
    } else {
      return suffixForCoreSvcSchemaBasedOnAgreedNamingSchemaInDoc1150441()
    }
  }

  private String suffixForCoreSvcSchemaBasedOnAgreedNamingSchemaInDoc1150441() {
    String platformName = ""
    if (platform.isRHEL()) {
      if (platform.isRHEL4()) platformName += 'RHEL4-'
      else if (platform.isRHEL5()) platformName += "RHEL5-"
      else if (platform.isRHEL6()) platformName += "RHEL6-"
      else if (platform.isRHEL7()) platformName += "RHEL7-"
      else if (platform.isRHEL8()) platformName += "RHEL8-"
      else if (platform.isRHEL9()) platformName += "RHEL9-"
      else if (platform.isRHEL10()) platformName += "RHEL10-"
      if (platform.isX86()) {
        platformName += "i686"
      } else if (platform.isX64()) {
        platformName += "x86_64"
      } else if (platform.isPpc64()) {
        platformName += "ppc64"
      } else {
        log.error("platformName can't be determined. osName: ${platform.osName}, osArch: ${platform.osArch}, osVersion: ${platform.osVersion}, " +
                "archModel: ${platform.archModel}, solPreferredArch: ${platform.solPreferredArch}")
      }
    } else if (platform.isWindows()) {
      platformName += (platform.isX86() ? "win6-i686" : "win6-x86_64")
    } else if (platform.isSolaris()) {
      if (platform.isSparc()) {
        platformName += 'sun10-sparc64'
      } else if (platform.isX86()) {
        platformName += 'sun10-i386'
      } else if (platform.isX64()) {
        platformName += 'sun10-x86_64'
      } else {
        log.error("platformName can't be determined. osName: ${platform.osName}, osArch: ${platform.osArch}, osVersion: ${platform.osVersion}, " +
                "archModel: ${platform.archModel}, solPreferredArch: ${platform.solPreferredArch}")
      }
    } else if (platform.isHP()) {
      platformName += 'hpux3-ia64'
    } else {
      log.error("platformName can't be determined. osName: ${platform.osName}, osArch: ${platform.osArch}, osVersion: ${platform.osVersion}, " +
              "archModel: ${platform.archModel}, solPreferredArch: ${platform.solPreferredArch}")
    }
    return platformName

  }

  String suffixForEAP5() {
    def platformName = ""
    if (platform.isRHEL()) {
      if (platform.isRHEL4()) platformName += 'RHEL4-'
      else if (platform.isRHEL5()) platformName += "RHEL5-"
      else if (platform.isRHEL6()) platformName += "RHEL6-"
      platformName += (platform.isX86() ? "i386" : "x86_64")
    } else if (platform.isWindows()) {
      platformName += (platform.isX86() ? "windows32-i386" : "windows64-x86_64")
    } else if (platform.isSolaris()) {
      if (platform.isSolaris9()) {
        platformName += 'solaris9-'
      } else {
        platformName += 'solaris10-'
      }
      if (platform.isSparc64() || (platform.isSparc() && !platform.isSolaris9())) {
        platformName += 'sparc64'
      } else if (platform.isSparc()) {
        platformName += 'sparc'
      } else if (platform.isX86()) {
        platformName += 'i386'
      } else if (platform.isX64()) {
        platformName += 'x86_64'
      } else {
        log.error("platformName can't be determined. osName: ${platform.osName}, osArch: ${platform.osArch}, osVersion: ${platform.osVersion}," +
                " archModel: ${platform.archModel}, solPreferredArch: ${platform.solPreferredArch}")
      }
    } else if (platform.isHP()) {
      platformName += 'hpux3-ia64'
    } else {
      log.error("platformName can't be determined. osName: ${platform.osName}, osArch: ${platform.osArch}, osVersion: ${platform.osVersion}, " +
              "archModel: ${platform.archModel}, solPreferredArch: ${platform.solPreferredArch}")
    }
    return platformName
  }

  String suffixForEAP6() {
    def platformName = ""
    if (platform.isRHEL()) {
      if (platform.isRHEL4()) platformName += 'RHEL4-'
      else if (platform.isRHEL5()) platformName += "RHEL5-"
      else if (platform.isRHEL6()) platformName += "RHEL6-"
      else if (platform.isRHEL7()) platformName += "RHEL7-"
      else if (platform.isRHEL8()) platformName += "RHEL8-"
      else if (platform.isRHEL9()) platformName += "RHEL9-"
      if (platform.isX86()) {
        platformName += "i386"
      } else if (platform.isX64()) {
        platformName += "x86_64"
      } else if (platform.isPpc64()) {
        platformName += "ppc64"
      } else if (platform.isS390x()) {
        platformName += "s390x"
      } else {
        log.error("platformName can't be determined. osName: ${platform.osName}, osArch: ${platform.osArch}, osVersion: ${platform.osVersion}, " +
                "archModel: ${platform.archModel}, solPreferredArch: ${platform.solPreferredArch}")
      }
    } else if (platform.isWindows()) {
      platformName += (platform.isX86() ? "win6.i686" : "win6.x86_64")
    } else if (platform.isSolaris()) {
      if (platform.isSparc()) {
        platformName += 'sun10.sparc64'
      } else if (platform.isX86()) {
        platformName += 'sun10.i386'
      } else if (platform.isX64()) {
        platformName += 'sun10.x86_64'
      } else {
        log.error("platformName can't be determined. osName: ${platform.osName}, osArch: ${platform.osArch}, osVersion: ${platform.osVersion}, " +
                "archModel: ${platform.archModel}, solPreferredArch: ${platform.solPreferredArch}")
      }
    } else if (platform.isHP()) {
      platformName += 'hpux3.ia64'
    } else {
      log.error("platformName can't be determined. osName: ${platform.osName}, osArch: ${platform.osArch}, osVersion: ${platform.osVersion}, " +
              "archModel: ${platform.archModel}, solPreferredArch: ${platform.solPreferredArch}")
    }
    return platformName
  }
}
