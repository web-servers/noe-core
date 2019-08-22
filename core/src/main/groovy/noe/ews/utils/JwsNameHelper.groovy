package noe.ews.utils

import groovy.util.logging.Slf4j
import noe.common.utils.Platform
import noe.common.utils.Version

/**
 * JWS Name helper provides logic behind JWS ZIP filenames. These filenames differ based on the EWS version, as well as
 * old vs new naming. If you require old naming, toggle compatibility mode. Otherwise, new naming will be provided.
 * Old naming example: jws-application-server-5.0.0-RHEL7-x86_64.zip
 * New naming example: jws-5.0.0-application-server-RHEL7-x86_64.zip
 */
@Slf4j
class JwsNameHelper {
  Version version
  int jwsMajorVersion
  boolean compatibilityMode = false
  String archSeparator
  Platform platform
  String productName

  JwsNameHelper(Version version) {
    this.version = version
    this.jwsMajorVersion = version.majorVersion
    this.platform = new Platform()
    productName = jwsMajorVersion <= 2 ? "jboss-ews" : "jws"
    archSeparator = "."
  }

  /**
   * Returns the zip name for the Hibernate zip, based on the JWS version and compatibility mode toggle
   */
  String getHibernateZipName() {
    String hibernateBaseName = "hibernate-dist"

    if(jwsMajorVersion <= 2) {
      return  "${hibernateBaseName}.zip"
    } else if (compatibilityMode) {
      return "${hibernateBaseName}-${version}.zip"
    }

    return "jws-${version}-${hibernateBaseName}.zip"
  }

  /**
   * Builds the name of JWS zip natives. Returns something like jboss-ews-application-servers-2.0.0-ER5-RHEL6-i386.zip
   */
  String getJWSApplicationServerNativeZipFileName() {
    StringBuilder zipNameBuilder = new StringBuilder()
    zipNameBuilder.append(productName)

    if(compatibilityMode) {
      zipNameBuilder.append("-${applicationServerBaseName()}-${version}-")
    } else {
      zipNameBuilder.append("-${version}-${applicationServerBaseName()}-")
    }

    zipNameBuilder.append("${getOsAndArchitecture()}.zip")
    log.trace('GetEWS: applicationServerNativeZipFileName: {}', zipNameBuilder)

    return zipNameBuilder.toString()
  }

  /**
   * Builds the name of JWS zip Java base. Returns something like jboss-ews-application-servers-3.0.0-ER5.zip
   */
  String getJWSApplicationServerBaseZipFileName() {
    if(compatibilityMode) {
      return "${productName}-${applicationServerBaseName()}-${version}.zip"
    }
    return "${productName}-${version}-${applicationServerBaseName()}.zip"
  }

  /**
   * Builds the name of examples zip, e.g. jws-examples-3.0.0-ER5.zip
   */
  String getExamplesZipFileName() {
    if(compatibilityMode) {
      return "${productName}-examples-${version}.zip"
    }
    return "${productName}-${version}-examples.zip"
  }

  /**
   * Returns either application-server, or application-servers based on how many tomcats are in the given JWS version
   */
  private String applicationServerBaseName() {
    return jwsMajorVersion <= 3 ? "application-servers" : "application-server"
  }

  private String getOsAndArchitecture() {
    String result = ""
    if (platform.isRHEL()) {
      if (platform.isRHEL4()) result += 'RHEL4-'
      if (platform.isRHEL5()) result += 'RHEL5-'
      else if (platform.isRHEL6()) result += 'RHEL6-'
      else if (platform.isRHEL7()) result += 'RHEL7-'
      else if (platform.isRHEL8()) result += 'RHEL8-'
      if (platform.isX86()) {
        result += "i386"
      } else if (platform.isX64()) {
        result += "x86_64"
      } else if (platform.isPpc64()) {
        result += "ppc64"
      } else {
        log.error("platformName can't be determined. osName: ${platform.osName}, osArch: ${platform.osArch}," +
            " osVersion: ${platform.osVersion}, archModel: ${platform.archModel}, solPreferredArch: ${platform.solPreferredArch}")
      }
    } else if (platform.isWindows()) {
      result += (platform.isX86() ? "win6${archSeparator}i686" : "win6${archSeparator}x86_64")
    } else if (platform.isSolaris()) {
      String ver = '10' // on Solaris 10 and 11 -> build for 10
      String os = 'sun'
      if (platform.isSparc()) result += "${os}${ver}${archSeparator}sparc64"
      else if (platform.getSolPreferredArch() == 32) result += "${os}${ver}.i386"
      else result += "${os}${ver}${archSeparator}x86_64"
    }
    return result
  }

  /**
   * Toggle the compatibility mode so that you can get old/new naming scheme of the zipnames
   */
  void toggleCompatibilityMode() {
    this.compatibilityMode = !compatibilityMode
  }

  /**
   * Sets architecture separator so that you can get old/new naming scheme of the zipnames.
   * Architecture separator is used only for Windows and Solaris packages.
   */
  void setArchSeparator(String separator) {
    this.archSeparator = separator
  }
}
