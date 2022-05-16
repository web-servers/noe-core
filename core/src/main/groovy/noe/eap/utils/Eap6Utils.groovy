package noe.eap.utils

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.utils.Cmd
import noe.common.utils.Hudson
import noe.common.utils.JBFile
import noe.common.utils.Library
import noe.common.utils.Platform
import noe.common.utils.PlatformSuffixHelper
import noe.common.utils.Version
import noe.eap.server.ServerEap
import noe.ews.server.ServerEws
import org.apache.commons.io.FilenameUtils
/**
 * Eap6 class handles zip installation process.
 * It downloads/copies zip files for various parts
 * of EAP distribution (application server, natives, connectors...)
 * and install them to predefined locations.
 *
 * @author Michal Karm Babacek <mbabacek@redhat.com>
 *
 */
@Slf4j
public class Eap6Utils {

  final String basedir
  final Platform platform
  final PlatformSuffixHelper platformUtils
  final String s = File.separator
  final String eapVersion
  final String ewsVersion

  final String eapApplicationServer
  final String eapWebserverConnectors
  final String eapNativeLibs
  final String eapNativeUtils
  final String eapHttpd
  String distroDest

  final String distroStore
  final String jwsOrEws
  final String eapVersionMajor
  final String ewsVersionMajor
  final Version eapCpVersion
  boolean deleteArchive = false

  /**
   * Get EAP from the static directory.
   */
  Eap6Utils(String basedir, AntBuilder ant, Platform platform, String eapVersion, String ewsVersion) {
    this.basedir = basedir
    this.platform = platform
    this.platformUtils = new PlatformSuffixHelper(platform)
    this.eapVersion = eapVersion
    this.ewsVersion = ewsVersion
    this.eapCpVersion = DefaultProperties.eapCpVersion()
    this.eapVersionMajor = eapVersion.tokenize('.').take(2).join('.')
    this.ewsVersionMajor = ewsVersion.tokenize('.').take(2).join('.')
    // Starting with 3.0 the product is called JWS instead of JBoss EWS
    this.jwsOrEws = (ewsVersionMajor.startsWith("3")) ? "jws" : "jboss-ews"
    // e.g. ${PATH_TO_STATIC}/eap/6.0.1.ER2/jboss-eap-6.0.1.ER2.zip
    this.eapApplicationServer = getApplicationServerZipFileName()
    // e.g. ${PATH_TO_STATIC}/eap/6.0.1.ER2/natives/jboss-eap-utils-6.0.1.ER2-RHEL6-x86_64.zip
    this.eapNativeUtils = "natives${s}" + getNativeUtilsZipFileName()
    // e.g. ${PATH_TO_STATIC}/eap/6.0.1.ER2/natives/jboss-eap-native-webserver-connectors-6.0.1.ER2-RHEL6-x86_64.zip
    this.eapWebserverConnectors = "natives${s}" + getWebserverConnectorsZipFileName()
    // e.g. ${PATH_TO_STATIC}/eap/6.0.1.ER2/natives/jboss-eap-native-6.0.1.ER2-RHEL6-x86_64.zip
    this.eapNativeLibs = "natives${s}" + getNativeLibsZipFileName()
    // e.g. ${PATH_TO_STATIC}/eap/6.0.1.ER2/httpd/jboss-ews-httpd-2.0.0-RHEL6-x86_64.zip
    this.eapHttpd = "httpd${s}" + getHttpdZipFileName()

    // Normally we should determine by ServerEap.IS_WILDFLY to choose whether to use 'eap' or 'wildfly' directory name.
    // Although, we expect all builds (even those of wildfly nightly) are placed in 'STATIC/eap' directory.
    distroStore = Hudson.staticDir + s + "eap"
  }

  synchronized void getIt() {
    log.trace(' --- Getting EAP started --- ')

    //Clean EAP6 base dir
    JBFile.delete(new File(basedir, ServerEap.getPrefix()))
    //Clean HTTPD base dir (EWS)
    if (ewsVersion) {
      JBFile.delete(new File(basedir, ServerEws.getPrefix()))
    }

    // Installs AS7
    def eapUrl = Library.getUniversalProperty("eap.url")
    if (eapUrl) {
      installEapFromUrl(eapUrl.trim())
    } else {
      installZipFile(eapApplicationServer)
    }

    // Installs Httpd
    if (ewsVersion) {
      installZipFile(eapHttpd)
    }

    File eapRootDir = new File(basedir, ServerEap.getPrefix())

    //Hmm, sometimes on some boxes, unizp screws x permissions...
    JBFile.makeAccessible(new File(eapRootDir, "bin"))


    // first install proper EAP CP version.
    // If EAP was installed from URL, we expect it to be already in state with patches applied
    if (eapCpVersion && !eapUrl) {
      log.info("Installing EAP patch for ${eapCpVersion} to EAP with basedir ${eapRootDir.absolutePath}")
      File patchFile = retrieveEapPatchFileFromStatic(eapCpVersion)
      if (!installEapPatch(eapRootDir, patchFile)) {
        throw new RuntimeException("Failed to install EAP patch for ${eapCpVersion}")
      }
    }

    // this should point to patch, which can be applied to installed version of EAP (which can be EAP with applied CP patch
    // defined by eap.cp.version
    def eapPatchUrl = Library.getUniversalProperty("eap.patch.url")
    if (eapPatchUrl) {
      log.info("Installing EAP patch from ${eapPatchUrl} to EAP with basedir ${eapRootDir.absolutePath}")
      File patchFile = retrieveEapPatchFileFromUrl(eapPatchUrl)
      if (!installEapPatch(eapRootDir, patchFile)) {
        throw new RuntimeException("Failed to install EAP patch from ${eapPatchUrl}")
      }
    }
  }

  /**
   * Installs native libs, e.g. APR,
   * for instance, from this zip: jboss-eap-native-6.0.1.ER2-RHEL6-x86_64.zip
   *
   */
  def synchronized installNativesZip() {
    installZipFile(eapNativeLibs)
  }

  /**
   * Installs native utils - JSVC, OpenSSL
   * for instance, from this zip: jboss-eap-native-utils-6.4.0-RHEL6-x86_64.zip
   *
   */
  def installNativesUtilsZip() {
    installZipFile(eapNativeUtils)
  }

  /**
   * Installs web connectors zip file, e.g.: jboss-eap-native-webserver-connectors-6.0.1.ER2-RHEL5-i386.zip
   * into jboss-eap-6.0/modules/native/
   * and furthermore, it copies
   *    jboss-eap-6.0/modules/native/etc/httpd/conf/
   *  and
   *    jboss-eap-6.0/modules/native/lib/httpd/modules/  (jboss-eap-6.0/modules/native/lib64/httpd/modules/ for 64 bit platforms)
   *  in their proper destinations within
   *    jboss-ews-2.0/httpd/
   */
  def synchronized installWebConnectorsZip() {
    installZipFile(eapWebserverConnectors)

    //Modules
    //nothing or 64
    //C:\Program Files\jboss-ews-2.0\lib64\httpd\modules
    //C:\Program Files\jboss-ews-2.0\httpd\modules

    def bitArch = eapWebserverConnectors.tokenize('/').last().contains('64') ? "64" : ""

    File eapModulesNativeDir
    //TODO: Migrate to Version class
    if (eapVersionMajor == "6.0") {
      eapModulesNativeDir = new File(basedir.toString() + "${s}jboss-eap-${eapVersionMajor}${s}modules${s}native")
    } else if (eapVersionMajor in ["6.1", "6.2", "6.3", "6.4", "7.0"]) {
      eapModulesNativeDir = new File(basedir.toString() + "${s}jboss-eap-${eapVersionMajor}${s}modules${s}system${s}layers${s}base${s}native")
    } else {
      throw new RuntimeException("Eh, dude, I dunno how to work with $eapVersionMajor")
    }

    File httpdConfDir
    File httpdModulesDir
    if (platform.isRHEL()) {
      httpdModulesDir = new File(basedir.toString() + "${s}${jwsOrEws}-${ewsVersionMajor}${s}httpd${s}modules")
      httpdConfDir = new File(basedir.toString() + "${s}${jwsOrEws}-${ewsVersionMajor}${s}httpd${s}${DefaultProperties.CONF_DIRECTORY}")
    } else if (platform.isWindows() || platform.isSolaris()) {
      httpdModulesDir = new File(basedir.toString() + "${s}${jwsOrEws}-${ewsVersionMajor}${s}lib${bitArch}${s}httpd${s}modules")
      httpdConfDir = new File(basedir.toString() + "${s}${jwsOrEws}-${ewsVersionMajor}${s}etc${s}httpd${s}${DefaultProperties.CONF_DIRECTORY}")

    } else if (platform.isHP()) {
      httpdModulesDir = new File(basedir.toString() + "${s}hpws22${s}apache${s}modules")
      httpdConfDir = new File(basedir.toString() + "${s}hpws22${s}apache${s}conf")
    } else {
      throw new RuntimeException('Doh... an unsupported platform. You can go ahead and implement it, huh? ;-)')
    }

    JBFile.copyDirectoryContent(new File(eapModulesNativeDir, "lib${bitArch}${s}httpd${s}modules"), httpdModulesDir)
    JBFile.copyDirectoryContent(new File(eapModulesNativeDir, "etc${s}httpd${s}conf"), httpdConfDir)
  }

  def installZipFile(String zipFileName) {
    if (!(zipFileName.trim().length() == 0)) {
      //TODO: Examine, on a live system Win/HP-UX/RHEL/SOL, whether simply using: new File(zipFileName).name, wouldn't do the same job.
      distroDest = (new File(distroStore + "${s}${eapVersion}${s}" + zipFileName)).name
      File sourceZipFile = new File(distroStore + "${s}${eapVersion}${s}", zipFileName)
      log.trace("Copy ${sourceZipFile.absolutePath} to ${basedir}")
      if (!JBFile.copy(sourceZipFile, new File(basedir), false, true, true)) {
        throw new RuntimeException("Failed to copy ${sourceZipFile} to ${basedir}")
      }
      JBFile.nativeUnzip(new File(basedir, distroDest), new File(basedir))
      if (DefaultProperties.isRemoveZipAfterUnzip()) {
        JBFile.delete(new File(basedir, distroDest))
      }
    } else {
      log.warn('Empty zipFileName -> skipping zip installation ...')
    }
  }

  /**
   * Installs provided EAP patch in format acceptable since EAP 6.2.0 via patch apply CLI command
   * @param eapRootDir root dir of EAP server (its JBOSS_HOME)
   * @param patchFile patch file
   * @return true if the CLI client returned 0, false otherwise
   */
  boolean installEapPatch(File eapRootDir, File patchFile) {
    File binDir = new File(eapRootDir, "bin")
    File cliClient = new File(binDir, "jboss-cli." + platform.getScriptSuffix())
    String command = "patch apply ${patchFile.absolutePath}"
    List cliCommand = [cliClient.absolutePath, "--command=$command"]
    log.debug("I'm going to run $command")
    return Cmd.executeCommand(cliCommand, cliClient.parentFile, null, [NOPAUSE: true]) == 0
  }

  File retrieveEapPatchFileFromStatic(Version eapCpVersion) {
    if (!eapCpVersion) {
      log.trace("EAP_CP_VERSION not provided => no patch to apply")
      return
    }
    File patchZipFile = new File(new File(distroStore, eapCpVersion.toString()), getApplicationServerPatchZipFileName())

    log.debug("Retrieving patch file from ${patchZipFile.absolutePath}")
    JBFile.copy(patchZipFile, new File(basedir))

    return new File(basedir, patchZipFile.name)
  }

  File retrieveEapPatchFileFromUrl(String urlAsStr) {
    File eapPatchZipFile = new File(basedir, FilenameUtils.getName(urlAsStr))
    Library.downloadFile(urlAsStr, eapPatchZipFile)
    return eapPatchZipFile
  }

  void installEapFromUrl(urlAsStr) {
    log.info("Getting EAP from URL ${urlAsStr}")

    File eapZipFile = new File(basedir, FilenameUtils.getName(urlAsStr))
    Library.downloadFile(urlAsStr, eapZipFile)
    log.trace('nativeUnzip SRC: {}, DEST: {}', eapZipFile.absolutePath, new File(basedir).absolutePath)
    JBFile.nativeUnzip(eapZipFile, new File(basedir))

    if (Boolean.valueOf(deleteArchive)) {
      JBFile.delete(eapZipFile)
    }
  }

  String getApplicationServerZipFileName() {
    if (ServerEap.IS_WILDFLY) {
      return "wildfly-${eapVersion}.zip"
    } else {
      return "jboss-eap-${eapVersion}.zip"
    }
  }

  String getApplicationServerPatchZipFileName() {
    String eapCpVersionAsString = eapCpVersion.toString()
    if (eapCpVersion.toString().endsWith(".CP")) {
      eapCpVersionAsString = eapCpVersion.baseVersionString()
    }
    return "jboss-eap-${eapCpVersionAsString}-patch.zip"
  }

  String getNativeUtilsZipFileName() {
    return "jboss-eap-native-utils-${eapVersion}-${platformUtils.suffixForEAP6()}.zip"
  }

  String getWebserverConnectorsZipFileName() {
    return "jboss-eap-native-webserver-connectors-${eapVersion}-${platformUtils.suffixForEAP6()}.zip"
  }

  String getNativeLibsZipFileName() {
    return "jboss-eap-native-${eapVersion}-${platformUtils.suffixForEAP6()}.zip"
  }

  String getHttpdZipFileName() {
    return "${jwsOrEws}-httpd-$ewsVersion-${platformUtils.suffixForEAP6()}.zip"
  }

}
