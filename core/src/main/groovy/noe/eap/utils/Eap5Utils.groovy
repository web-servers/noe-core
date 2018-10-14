package noe.eap.utils

import groovy.util.logging.Slf4j
import noe.common.utils.Hudson
import noe.common.utils.JBFile
import noe.common.utils.Platform
import noe.common.utils.PlatformSuffixHelper
import noe.common.utils.Version
import noe.eap.server.ServerEap
import noe.eap.server.as5.AS5Properties
import noe.ews.server.ServerEws
import noe.ews.utils.EwsUtils
import noe.server.AS5
import noe.server.ServerAbstract
import noe.server.ServerController
/**
 * Eap5 class handles zip installation process.
 * It downloads/copies zip files for various parts
 * of EAP distribution (application server, natives, connectors...)
 * and install them to predefined locations.
 *
 * @author Michal Karm Babacek <mbabacek@redhat.com>
 *
 */
@Slf4j
public class Eap5Utils {

  def ant
  def basedir
  Platform platform
  PlatformSuffixHelper platformUtils
  def sep = File.separator
  def eapVersion
  def ewsVersion

  def eapApplicationServer
  def eapApplicationServerNoAuth
  def eapNativeLibs
  def distroDest

  def distroStore = "${Hudson.staticDir}${sep}eap"
  def deleteArchive = false
  def resourcesPath
  ServerController serverController = ServerController.getInstance()

  /**
   * Get EAP from the static directory.
   */
  Eap5Utils(basedir, ant, platform, eapVersion, ewsVersion = null) {
    this.basedir = basedir
    this.ant = ant
    this.platform = platform
    this.platformUtils = new PlatformSuffixHelper(platform)
    this.eapVersion = eapVersion
    this.ewsVersion = ewsVersion

    this.eapApplicationServer = getApplicationServerZipFileName(eapVersion, true)
    this.eapApplicationServerNoAuth = getApplicationServerZipFileName(eapVersion, false)
    this.eapNativeLibs = getNativeLibsZipFileName(eapVersion)

    this.resourcesPath = ServerAbstract.getDeplSrcPath()

    if (!(new File(distroStore).exists())) {
      distroStore = Hudson.staticDir + sep + 'eap'
    }
  }

  synchronized getIt(boolean secured = false) {
    log.trace(' --- Getting EAP started --- ')
    //Clean EAP5 base dir
    JBFile.delete(new File(basedir, ServerEap.getPrefix()))

    if (ewsVersion) {
      JBFile.delete(new File(basedir, ServerEws.getPrefix()))
    }

    // Installs EAP5
    if (secured) {
      installZipFile(eapApplicationServer)
    } else {
      installZipFile(eapApplicationServerNoAuth)
    }

    //Hmm, sometimes on some boxes, unizp screws x permissions...
    JBFile.makeAccessible(new File(basedir, ServerEap.getPrefix() + "${sep}jboss-as${sep}bin"))
  }

  /**
   * Installs native libs, e.g. APR,
   * for instance, from this zip: jboss-ep-native-5.2.0-RHEL5-x86_64.zip
   * @return path to the directory with natives
   *
   */
  synchronized File installNativesZip() {
    installZipFile(eapNativeLibs)
    File nativesDir = new File(basedir, "${ServerEap.getPrefix()}${sep}jboss-as${sep}${AS5Properties.NATIVES_DIR_NAME}")
    JBFile.mkdir(nativesDir)
    Version versionOfEap = new Version(eapVersion)
    def versionPrefix = "${versionOfEap.majorVersion}.${versionOfEap.minorVersion}"
    JBFile.copyDirectoryContent(new File(basedir, "jboss-ep-${versionPrefix}${sep}native"), nativesDir)
    JBFile.delete(new File(basedir, "jboss-ep-${versionPrefix}"))
    return nativesDir
  }

  void installZipFile(zipFileName) {
    if (!(zipFileName.trim().length() == 0)) {
      distroDest = new File(distroStore + "${sep}${eapVersion}${sep}" + zipFileName).name
      JBFile.copy(new File(distroStore + "${sep}${eapVersion}", zipFileName), new File(basedir))

      JBFile.nativeUnzip(new File(basedir, distroDest), new File(basedir))

      if (Boolean.valueOf(deleteArchive)) {
        JBFile.delete(new File(basedir, distroDest))
      }
    } else {
      log.warn('Empty zipFileName -> skipping zip installation ...')
    }
  }

  /**
   * Installs mod_cluster for EAP5 and updates httpd modules and configuration for mod_cluster by the ones provided by EAP 5
   *
   */
  synchronized installWebConnectors(File eapNativesDir = null) {
    def eapVersionMajor = eapVersion.tokenize('.').take(2).join('.')
    def nativesDir = eapNativesDir ?: new File(basedir, "jboss-ep-${eapVersionMajor}${sep}native")

    def bitArch = eapNativeLibs.tokenize('/').last().contains('64') ? "64" : ""
    EwsUtils.getPathToEwsModules(basedir, bitArch)
    ant.copy(todir: EwsUtils.getPathToEwsModules(basedir, bitArch), overwrite: 'true') {
      fileset(dir: new File(nativesDir, "lib${bitArch}${sep}httpd${sep}modules").absolutePath)
    }
  }

  /**
   * loads mod-cluster.sar to the AS5 server
   */
  void loadModCluster(serverId) {
    AS5 server = serverController.getServerById(serverId)
    def modClusterSar = new File(new File(server.refBasedir).parentFile, "mod_cluster${sep}mod-cluster.sar")
    log.info("Loading mod_cluster by coppying ${modClusterSar.absolutePath} to ${server.deploymentPath}")
    JBFile.copyDirectoryContent(modClusterSar, new File(server.deploymentPath, modClusterSar.name))
    def modClusterSarDeployConfDir = server.deploymentPath + platform.sep + modClusterSar.name
    if (!server.deployConfigDirs.contains(modClusterSarDeployConfDir)) {
      server.deployConfigDirs.add(modClusterSarDeployConfDir)
    }
  }

  String getApplicationServerZipFileName(eapVersion, secured = false) {
    if (secured) {
      return "jboss-eap-${getVersionPart(eapVersion)}.zip"
    } else {
      return "jboss-eap-noauth-${getVersionPart(eapVersion)}.zip"
    }
  }

  String getNativeLibsZipFileName(eapVersion) {
    return "jboss-ep-native-${getVersionPart(eapVersion)}-${platformUtils.suffixForEAP5()}.zip"
  }

  String getVersionPart(eapVersion) {
    Version version = new Version(eapVersion)
    String build = version.getBuildNumber() ?: ''

    if (!build || build == "GA") {
      return version.baseVersionString()
    } else {
      return eapVersion

    }
  }
}
