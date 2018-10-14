package noe.app

import noe.common.utils.Cmd
import noe.common.utils.Hudson
import noe.common.utils.JBFile
import noe.common.utils.Library
import noe.common.utils.Platform


/**
 * @author Jan Stefl     <jstefl@redhat.com>
 */

class Firefox implements IApp {
  static Platform platform = new Platform()
  static String sep = platform.sep
  static String profilesDir = new File(platform.getHomeDir(), (platform.isWindows() ? "AppData\\Roaming\\Mozilla\\Firefox\\Profiles" : ".mozilla/firefox")).absolutePath
  static String profilesIniFile = new File(platform.getHomeDir(), "AppData\\Roaming\\Mozilla\\Firefox\\profiles.ini").absolutePath
  static String profilesIniBackup = new File(platform.getHomeDir(), "AppData\\Roaming\\Mozilla\\Firefox\\profiles.ini.bkp").absolutePath
  static String profilesSrcDir = new File("${Hudson.staticDir}${sep}jon${sep}firefox-sahi-profile").absolutePath
  static String profilesIniWinSrc = new File(profilesSrcDir, 'profiles.ini.win').absolutePath
  static String firefoxVersion = Library.getUniversalProperty('firefox.version', '17.0.1')
  static String firefoxStartPath = null

  void start() {
  }

  long stop() {
  }

  /**
   * Clean table and kill all firefoxes.
   */
  static void killAllInSystem() {
    Cmd.killAllInSystem(["firefox"])
  }

  static void copyFirefoxToLocalDir(File destDir) {
    File firefoxDistro = new File(getDistroPath(firefoxVersion))
    if (!JBFile.copyDirectoryContent(firefoxDistro, destDir)) {
      throw new RuntimeException("Failed to copy firefox distribution from " + firefoxDistro.absolutePath + " to " + destDir.absolutePath)
    }
    JBFile.makeAccessible(destDir)
    firefoxStartPath = new File(destDir, "firefox").absolutePath
  }


  static void prepareFirefoxProfile(profile) {
    File srcDir
    File targetDir

    if (platform.isWindows()) {
      // 1. copy profile dir from hudson static
      JBFile.delete(new File(profilesDir, profile))
      srcDir = new File(profilesSrcDir, profile)
      targetDir = new File(profilesDir, profile)
      JBFile.copyDirectoryContent(srcDir, targetDir)
      // 2. backup profiles.ini and copy profiles.ini from hudson static
      JBFile.copyFile(new File(profilesIniFile), new File(profilesIniBackup))
      JBFile.copyFile(new File(profilesIniWinSrc), new File(profilesIniFile))
    } else {
      // 1. copy profile dir from hudson static
      JBFile.delete(new File('/tmp', profile))
      srcDir = new File(profilesSrcDir, profile)
      targetDir = new File('/tmp')
      JBFile.copy(srcDir, targetDir)
      // 2. profiles.ini is set on shared disk /home/hudson/.mozilla/firefox
    }
  }

  static void revertFirefoxProfilesIni() {
    if (platform.isWindows()) {
      JBFile.delete(new File(profilesIniFile))
      JBFile.copyFile(new File(profilesIniBackup), new File(profilesIniFile))
    }
  }

  static String getDistroPath(String version = '17.0.1') {
    def platformDir = ''
    def path = new File(Hudson.toolsPath)
    def suffix = ''

    if (platform.isRHEL()) {
      if (platform.isX64()) {
        platformDir = 'x86_64'
      } else if (platform.isPpc64()) {
        platformDir = 'ppc64'
        if (platform.isRHEL6()) suffix = "-RHEL6"
        else if (platform.isRHEL7()) suffix = "-RHEL7"
      }
    }

    if (platform.isWindows()) {
      platformDir = 'windows'
    }

    if (platform.isSolaris()) {
      if (platform.isSparc()) {
        if (platform.isSolaris10()) platformDir = 'solaris10_sparc'
        else if (platform.isSolaris11()) platformDir = 'solaris11_sparc'
        else throw new RuntimeException("Unsupported version of Solaris")
      } else if (platform.isX86()) {
        if (platform.isSolaris10()) platformDir = 'solaris10_x86'
        else if (platform.isSolaris11()) platformDir = 'solaris11_x86'
        else throw new RuntimeException("Unsupported version of Solaris")
      } else if (platform.isX64()) {
        if (platform.isSolaris10()) platformDir = 'solaris10_x86_64'
        else if (platform.isSolaris11()) platformDir = 'solaris11_x86_64'
        else throw new RuntimeException("Unsupported version of Solaris")
      } else {
        throw new RuntimeException("Unsupported version of Solaris")
      }
    }
    def versionSuffix = version + suffix
    return new File(path, platformDir + "${sep}firefox-${versionSuffix}").getAbsolutePath()
  }

}
