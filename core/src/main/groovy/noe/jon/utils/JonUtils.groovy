package noe.jon.utils

import groovy.util.logging.Slf4j
import noe.common.utils.Cmd
import noe.common.utils.Hudson
import noe.common.utils.JBFile
import noe.common.utils.Library
import noe.common.utils.Net
import noe.common.utils.Platform

import java.util.prefs.Preferences

@Slf4j
class JonUtils {

  def ant
  def basedir
  Platform platform
  def sep
  def nl

  def distroStore
  def jonDistroStore
  def jonAgentDistroStore
  def sahiDistroStore
  def gradleDistroStore
  def mavenDistroStore
  def deleteArchive = false
  def winUtilsStore

  def installWinUtilsDirectory
  def installSahiDirectory
  def installJonDirectory
  def installJonAgentJonDirectory
  def installJonAgentPrefDirectory
  def installGradleDirectory
  def installMavenDirectory
  def installAutomatJonDirectory
  def installTestsuiteDirectory
  def mavenLocalRepositoryDirectory

  def static final DEF_JON_AGENT_PORT = 16163
  def static final DEF_JON_AGENT_SETTINGS_FILENAME = 'agentSetting.txt'
  def static final DEF_GRADLE = 'gradle-' + Library.getUniversalProperty('gradle.version', '1.6')
  def static final DEF_MAVEN = 'apache-maven-' + Library.getUniversalProperty('maven.version', '3.0.5')    // 2.2.1
  def static final DEF_JON_AGENT_TESTSUITE_CONF_FILENAME = 'automation.properties'

  /**
   * Get JON related utils from static directory,
   * TODO Do we need it more general?
   */
  public JonUtils(basedir, ant, platform) {
    this.basedir = basedir
    this.ant = ant
    this.platform = platform
    this.sep = platform.sep
    this.nl = platform.nl
    this.distroStore = Hudson.staticDir
    this.jonDistroStore = Hudson.staticDir + "${sep}jon"
    this.jonAgentDistroStore = this.jonDistroStore + "${sep}agent"
    this.sahiDistroStore = this.jonDistroStore + "${sep}sahi"
    this.gradleDistroStore = this.jonDistroStore + "${sep}gradle"
    this.mavenDistroStore = this.jonDistroStore + "${sep}maven"
    this.winUtilsStore = this.jonDistroStore + sep + "winUtils"
    this.installWinUtilsDirectory = new File(basedir, 'winUtils').absolutePath
    this.installSahiDirectory = new File(basedir, 'Sahi').absolutePath
    this.installJonAgentJonDirectory = new File(basedir, 'JON').absolutePath
    this.installJonAgentPrefDirectory = new File(installJonAgentJonDirectory, "rhq-agent${sep}bin${sep}" + DEF_JON_AGENT_SETTINGS_FILENAME).absolutePath
    this.installGradleDirectory = new File(basedir, 'gradle').absolutePath
    this.installMavenDirectory = new File(basedir, 'maven').absolutePath
    this.installAutomatJonDirectory = new File(basedir, 'jon-core').absolutePath
    this.installTestsuiteDirectory = new File(basedir, 'automat-jon-plugin').absolutePath
    this.mavenLocalRepositoryDirectory = new File(basedir, 'm2')
  }

  void installSahi(sahiZipName = 'sahi_20110719.zip') {
    log.info('Installing SAHI {}', sahiZipName)

    File installSahiDirectoryFile = new File(this.installSahiDirectory)
    JBFile.delete(installSahiDirectoryFile)
    JBFile.mkdir(installSahiDirectoryFile)

    JBFile.copy(new File(sahiDistroStore, sahiZipName), installSahiDirectoryFile)
    JBFile.nativeUnzip(new File(this.installSahiDirectory, sahiZipName), new File(this.installSahiDirectory))

    if (Boolean.valueOf(deleteArchive)) {
      JBFile.delete(new File(this.installSahiDirectory, sahiZipName))
    }

    JBFile.makeAccessible(new File(this.installSahiDirectory, "${sep}sahi${sep}bin${sep}sahi.${platform.getScriptSuffix()}"))
  }

  void installWinUtil(utilZipName) {
    if (!platform.isWindows()) {
      log.debug("There is no point to install a Windows util on non-Windows platform => skipping")
      return
    }
    log.info("Installing ${utilZipName}");

    File installWinUtilsDirectoryFile = new File(this.installWinUtilsDirectory)
    JBFile.delete(installWinUtilsDirectoryFile)
    JBFile.mkdir(installWinUtilsDirectoryFile)

    JBFile.copy(new File(this.winUtilsStore, utilZipName), installWinUtilsDirectoryFile)
    JBFile.nativeUnzip(new File(this.installWinUtilsDirectory, utilZipName), installWinUtilsDirectoryFile)

    if (Boolean.valueOf(deleteArchive)) {
      JBFile.delete(new File(this.installWinUtilsDirectory, utilZipName))
    }
  }

  /**
   * Installs jon agent which is retrieved directly from the JON server
   */
  Map installJonAgent(jonAgentIp, jonServerIp, jonServerPort) {
    // 1. Copy install jar from static_build_env directory
    File installJonDirectoryFile = new File(this.installJonAgentJonDirectory)
    JBFile.delete(installJonDirectoryFile)
    JBFile.mkdir(installJonDirectoryFile)
    def jonAgentJarName = "${jonAgentIp}-jon-agent.jar"
    Library.downloadFile("http://${jonServerIp}:${jonServerPort}/agentupdate/download".toString(),
        new File(installJonDirectoryFile, jonAgentJarName))

    //  2. Install
    Cmd.executeCommand(['java', '-jar', jonAgentJarName, '--install'], installJonDirectoryFile)

    //  3. Create agentSetting.txt
    //  3.1 select port
    //  We need change of JON_AGENT_NAME and JON_AGENT_PORT everytime on the same host, if there has been already rhq-agent running,
    //  because we are not able to set security token
/*
    def hostname=Net.getHostname().tokenize('.').first()
    def pattern="${hostname}.*"
    File agentsWorkspaceFile = new File(Hudson.home, ".java${sep}.userPrefs${sep}rhq-agent")
    List openedJonAgentPorts = []
    def jonAgentPort = DEF_JON_AGENT_PORT
    if (agentsWorkspaceFile.exists()) {
      agentsWorkspaceFile.eachFileMatch(~/${pattern}/) { 
        openedJonAgentPorts.add(Integer.valueOf(it.getAbsolutePath().tokenize('-').last()))
      }
      
      // If no jon agent is active, than use default port else last one incremented by 1.
      if (!openedJonAgentPorts.isEmpty()){
        jonAgentPort = openedJonAgentPorts.max{it} + 1
      }
    }
    else {
      IO.handleOutput "${agentsWorkspaceFile} does not exists - Using default port ${jonAgentPort}.", IO.LOG_LEVEL_FINE
    }
*/
    def jonAgentPort = freeJonAgentPort()
    //  3.2 agentSetting.txt
    def jonAgentName = Net.getHostname().tokenize('.').first() + '-agent-' + jonAgentPort
    // jon agent needs always unix end of lines
    def jonAgentSettingsString = "${jonAgentName}\n" +
        "${jonAgentIp}\n" +
        "${jonAgentPort}\n" +
        "${jonServerIp}\n" +
        "${jonServerPort}\n"
    if (Boolean.valueOf(Library.getUniversalProperty("JON_NATIVE_SUPPORT", "true"))) {
      jonAgentSettingsString += "native --enable\n"
    } else {
      jonAgentSettingsString += "native --disable\n"
    }
    jonAgentSettingsString += "discovery\n"

    File jonAgentSettingFile = new File(installJonDirectoryFile, "${sep}rhq-agent${sep}bin${sep}" + DEF_JON_AGENT_SETTINGS_FILENAME)
    JBFile.createFile(jonAgentSettingFile, jonAgentSettingsString)
    log.trace('Jon agent settings {}:\n{}', jonAgentSettingFile.getAbsolutePath(), jonAgentSettingFile.text)

    return ['name': jonAgentName, 'port': jonAgentPort, 'settingsPath': jonAgentSettingFile.getAbsolutePath()]
  }

  Integer freeJonAgentPort() {
    def hostname = Net.getHostname().tokenize('.').first()
    def pattern = "${hostname}.*"
    Preferences userRoot = Preferences.userRoot();
    List<String> agentNames = []
    List<Integer> openedJonAgentPorts = []
    def jonAgentPort = DEF_JON_AGENT_PORT

    def javaPrefsRootDir = Library.getUniversalProperty("JAVA_PREFS_ROOT_DIR", "")
    def rhqAgentJavaPrefsDir = new File(javaPrefsRootDir, ".java${sep}.userPrefs${sep}rhq-agent")
    if (javaPrefsRootDir && rhqAgentJavaPrefsDir.isDirectory()) {
      agentNames = (rhqAgentJavaPrefsDir.list(new FilenameFilter() {
        @Override
        boolean accept(File dir, String name) {
          return name.matches(~/${pattern}/)
        }
      }) as List<String>)
    }
    log.debug("Agent names retrieved via file listing = ${agentNames}")

    // Unix filesystem: /home/hudson/.java/.userPrefs/rhq-agent
    // win registry: HKEY_CURRENT_USER\Software\JavaSoft\Prefs\rhq-agent
    agentNames += (userRoot.childSpi("rhq-agent").childrenNamesSpi() as List<String>)

    if (!agentNames.isEmpty()) {
      agentNames.each() {
        if (it.matches(~/${pattern}/)) {
          try {
            openedJonAgentPorts.add(Integer.valueOf(it.tokenize('-').last()))
          } catch (NumberFormatException nfe) {
            // ignore, this is probably just from some manual testing and pref name doesn't end with port number
          }
        }
      }
      log.debug("Existing agent names are ${agentNames}, counting new one based hostname " +
              "(${hostname} and the highest port suffix")

      // If no jon agent is active, than use default port else last one incremented by 1.
      if (!openedJonAgentPorts.isEmpty()) {
        jonAgentPort = openedJonAgentPorts.max { it } + 1
      }
    } else {
      log.debug("No agentName exists yet - Using default port ${jonAgentPort}.")
    }
    return jonAgentPort
  }

  def installMavenGradle(gradleZipName = DEF_GRADLE, mavenZipName = DEF_MAVEN) {
    log.info("Setup gradle")
    File installGradleDirectoryFile = new File(this.installGradleDirectory)
    File gradleDistroFile = new File(gradleDistroStore, gradleZipName + '-bin.zip')
    if (!gradleDistroFile.exists()) {
      throw new RuntimeException("${gradleDistroFile} not found")
    }
    JBFile.delete(installGradleDirectoryFile)
    JBFile.mkdir(installGradleDirectoryFile)
    JBFile.copy(gradleDistroFile, installGradleDirectoryFile)
    JBFile.nativeUnzip(new File(installGradleDirectory, gradleZipName + '-bin.zip'), new File(installGradleDirectory))

    log.info('Setup maven')
    File installMavenDirectoryFile = new File(this.installMavenDirectory)
    File mavenDistroFile = new File(mavenDistroStore, mavenZipName + '-bin.zip')
    if (!mavenDistroFile.exists()) {
      throw new RuntimeException("${mavenDistroFile} not found")
    }
    JBFile.delete(installMavenDirectoryFile)
    JBFile.mkdir(installMavenDirectoryFile)
    JBFile.copy(mavenDistroFile, installMavenDirectoryFile)
    JBFile.nativeUnzip(new File(installMavenDirectory, mavenZipName + '-bin.zip'), new File(installMavenDirectory))

    File mavenHome = new File(installMavenDirectory, mavenZipName)
    if (mavenHome.exists()) {
      Cmd.props['M2_HOME'] = mavenHome.getAbsolutePath()
    } else {
      throw new RuntimeException("${mavenHome} not found")
    }

    File mavenSettingsXml = new File(mavenHome, "conf${sep}settings.xml")
    if (mavenSettingsXml.exists()) {
      JBFile.replace(
          mavenSettingsXml,
          '<!-- localRepository',
          "<localRepository>${mavenLocalRepositoryDirectory}</localRepository><!-- localRepository")

      log.trace('-MAVEN: settings.xml:\n{}', mavenSettingsXml.text)
    } else {
      throw new RuntimeException("${mavenSettingsXml} not found")
    }
  }
}
