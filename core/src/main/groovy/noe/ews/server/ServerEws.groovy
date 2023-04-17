package noe.ews.server

import noe.common.utils.Library
import noe.common.utils.Platform
import noe.common.utils.Version
import noe.common.DefaultProperties
import noe.workspace.WorkspaceAbstract

abstract class ServerEws {

  static List<Version> getTomcatVersions(Version ewsVersion) {
    return getTomcatVersions(ewsVersion.getMajorVersion())
  }

  /**
   * Identify version of Tomcats incorporated into EWS
   */
  static List<Version> getTomcatVersions(String ewsVersion = null) {
    return getTomcatVersions(ServerEws.extractMajorVersion(ewsVersion))
  }

  static List<Version> getTomcatVersions(int ewsMajorVersion) {
    List<Version> tomcatVersions = []

    if (!(ewsMajorVersion > 0)) {
      throw new RuntimeException("Wrong ews version ${ewsMajorVersion}.")
    }

    if (ewsMajorVersion == 1) {
      tomcatVersions = [new Version("5"), new Version("6")]
    } else if (ewsMajorVersion == 2) {
      tomcatVersions = [new Version("6"), new Version("7")]
    } else if (ewsMajorVersion == 3) {
      tomcatVersions = [new Version("7"), new Version("8")]
    } else if (ewsMajorVersion == 4) {
      //JWS4 has exact tomcat version 8.5 but for ease of use defining just 8
      tomcatVersions = [new Version("8")]
    } else if (ewsMajorVersion == 5) {
      tomcatVersions = [new Version("9")]
    } else if (ewsMajorVersion == 6) {
      tomcatVersions = [new Version("10")]
    } else {
      throw new RuntimeException("Unsupported version of EWS ${ewsMajorVersion}.")
    }

    return tomcatVersions
  }

  /**
   * Get EWS directory prefix.
   */
  static String getPrefix() {
    def platform = new Platform()
    def ewsMajorVersion = ServerEws.extractMajorVersion()
    def ewsMinorVersion = ServerEws.extractMinorVersion()

    if (ewsMajorVersion >= 3) {
      return "jws-${ewsMajorVersion}.${ewsMinorVersion}"
    }

    if (ewsMajorVersion == 2) {
      return "jboss-ews-${ewsMajorVersion}.${ewsMinorVersion}"
    }

    if (ewsMajorVersion == 1) {
      if (platform.isRHEL()) {
        return 'jboss-ews-1.0'
      } else if (platform.isSolaris()) {
        return 'redhat/ews'
      } else if (platform.isWindows()) {
        return 'Red Hat/Enterprise Web Server'
      } else {
        throw new RuntimeException("Unexpected platform.")
      }
    }

    throw new RuntimeException("Wrong ews version ${ewsMajorVersion}.")
  }

  static String getTomcatHome() {
    def platform = new Platform()
    return WorkspaceAbstract.retrieveBaseDir() + platform.sep + ServerEws.getPrefix()
  }

  static String getHttpdHome() {
    def platform = new Platform()
    def home
    if (DefaultProperties.apacheCoreVersion()) {
      home = WorkspaceAbstract.retrieveBaseDir() + platform.sep + DefaultProperties.HTTPD_CORE_DIR
    } else {
      home = WorkspaceAbstract.retrieveBaseDir() + platform.sep + ServerEws.getPrefix()
    }
    return home
  }


  /**
   * Extract major EWS version from method argument of from global settings.
   */
  static Integer extractMajorVersion(String fullVersion = '') {
    def tmp = (fullVersion) ?: Library.getUniversalProperty('ews.version').toString()

    // Simple validation
    if (!tmp.contains('.')) {
      throw new RuntimeException("Invalid ews version")
    }

    return Integer.parseInt(tmp.tokenize('.').first())
  }

  /**
   * Extract minor EWS version from method argument of from global settings.
   */
  static Integer extractMinorVersion(String fullVersion = '') {
    def tmp = (fullVersion) ?: Library.getUniversalProperty('ews.version').toString()

    // Simple validation
    if (!tmp.contains('.')) {
      throw new RuntimeException("Invalid ews version")
    }

    return Integer.parseInt(tmp.tokenize('.')[1])
  }

  /**
   * Return {@link Version} of httpd included in particular EWS/JWS, including major and minor version only (no micro version)
   * @param ewsVersion String version of EWS/JWS
   * @return {@link Version} of httpd in the EWS/JWS
   */
  public static Version getHttpdVersion(String ewsVersion = '') {
    if (extractMajorVersion(ewsVersion) < 3) {
      return new Version("2.2")
    } else {
      return new Version("2.4")
    }
  }
}
