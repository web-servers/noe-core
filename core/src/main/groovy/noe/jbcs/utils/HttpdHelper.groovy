package noe.jbcs.utils

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.utils.Cmd
import noe.common.utils.JBFile
import noe.common.utils.Library
import noe.common.utils.Platform
import noe.server.Httpd

@Slf4j
class HttpdHelper {
  Platform platform

  HttpdHelper(Platform platform) {
    this.platform = platform
  }

  static String getBasedirSuffix() {
    if (DefaultProperties.apacheCoreVersion()) {
      return DefaultProperties.HTTPD_CORE_DIR
    } else {
      def ewsVersion = DefaultProperties.ewsVersion()
      String ewsPrefix = (ewsVersion.majorVersion) <= 2 ? "jboss-ews" : "jws"
      return "${ewsPrefix}-${ewsVersion.majorVersion}.${ewsVersion.minorVersion}"
    }
  }

  /**
   * Set EOL for httpd on Solaris
   */
  static setEolOnSolarisHttpd(Httpd httpd) {
    def directories = [
            new File("${httpd.basedir}/${getBasedirSuffix()}/etc/httpd/conf"),
            new File("${httpd.basedir}/${getBasedirSuffix()}/etc/httpd/conf.d")
    ]

    directories.each { directory ->
      if (directory.exists()) {
        directory.eachFile { file ->
          if (file.isFile()) file.append("\n")
        }
      }
    }
  }

  void runPostinstallAndFixExecRights(Httpd httpd) {
    runPostinstall(httpd)
    log.debug("Fixing exec rights after postinstall")
    JBFile.makeAccessible(new File(httpd.basedir + "/" + httpd.binPath + "/apachectl"))
    JBFile.makeAccessible(new File(httpd.basedir + "/" + httpd.binPath + "/httpd.worker"))
    JBFile.makeAccessible(new File(httpd.basedir + "/" + httpd.binPath + "/httpd"))
  }
  
  void runPostinstall(Httpd httpd) {
    log.debug("runPostinstall started sudo=${JBFile.useAdminPrivileges} and httpd.basedir=${httpd.basedir}")
    def postInstallOutput = null
    def commandExecution = Cmd.executeMethodBasedOnAdminPrivileges(
            Cmd.&executeCommandConsumeStreams,
            Cmd.&executeSudoCommandConsumeStreams)
    def ewsMajorVersion = DefaultProperties.ewsVersion() ? DefaultProperties.ewsVersion().getMajorVersion() : null
    def apacheCoreVersion = DefaultProperties.apacheCoreVersion()
    def httpdBasedir = httpd.basedir
    def sep = platform.sep

    if (platform.isRHEL()) {
      postInstallOutput = commandExecution(["sh", ".postinstall"], new File(httpdBasedir))
    } else if (platform.isSolaris()) {
      // EWS1
      if (ewsMajorVersion == 1) {
        if (JBFile.useAdminPrivileges) {
          postInstallOutput = commandExecution(["sh", ".postinstall"], new File(httpdBasedir, "etc"))
        } else {
          def isIt64 = (platform.isSparc64() || platform.isX64()) ? "64" : ""
          JBFile.copy(Library.retrieveResourceAsFile("httpd/.postinstall.ews1.httpd.nosudo.solaris${isIt64}"), new File(httpdBasedir, "etc"), false, true)
          postInstallOutput = Cmd.executeCommandConsumeStreams([
            "sh",
            ".postinstall.ews1.httpd.nosudo.solaris${isIt64}"
          ], new File(httpdBasedir, "etc"))
        }
      } else {
        // EWS2+ & JBCS
        if (Boolean.valueOf(Library.getUniversalProperty('BZ1093674_WORKAROUND', false))) {
          [
                  new File(httpdBasedir, "etc/httpd/${DefaultProperties.CONF_DIRECTORY}/manual.conf.in"),
                  new File(httpdBasedir, "etc/httpd/conf.d/ssl.conf.in"),
                  new File(httpdBasedir, "etc/httpd/conf/httpd.conf.in")
          ].each { file ->
            JBFile.replace(file, "\"/bin", "\"@installroot@/bin")
            JBFile.replace(file, "\"/etc", "\"@installroot@/etc")
            JBFile.replace(file, "\"/lib64", "\"@installroot@/lib64")
            JBFile.replace(file, "\"/lib", "\"@installroot@/lib")
            JBFile.replace(file, "\"/sbin", "\"@installroot@/sbin")
            JBFile.replace(file, "\"/share", "\"@installroot@/share")
            JBFile.replace(file, "\"/var", "\"@installroot@/var")

            JBFile.replace(file, " /bin", " @installroot@/bin")
            JBFile.replace(file, " /etc", " @installroot@/etc")
            JBFile.replace(file, " /lib64", " @installroot@/lib64")
            JBFile.replace(file, " /lib", " @installroot@/lib")
            JBFile.replace(file, " /sbin", " @installroot@/sbin")
            JBFile.replace(file, " /share", " @installroot@/share")
            JBFile.replace(file, " /var", " @installroot@/var")
          }
        }
        if (JBFile.useAdminPrivileges) {
          postInstallOutput = Cmd.executeSudoCommandConsumeStreams(["sh", ".postinstall.httpd"], new File(httpdBasedir, "etc"))
        } else {
          if (apacheCoreVersion || ewsMajorVersion == 3) {
            postInstallOutput = Cmd.executeCommandConsumeStreams(["sh", ".postinstall.httpd"], new File(httpdBasedir, 'etc'))
          } else {
            // The following postinstall juggling is necessary for EWS 2x. JWS 3x needs it until JWS-153 is fixed.
            def isIt64 = (platform.isSparc() || platform.isX64()) ? "64" : ""
            String customHttpdPostinstall = ".postinstall.httpd.nosudo.solaris${isIt64}"
            JBFile.copy(Library.retrieveResourceAsFile("httpd/${customHttpdPostinstall}"), new File(httpdBasedir, "etc"), false, true)
            JBFile.chmod("ugo+rx", new File(httpdBasedir, "etc/${customHttpdPostinstall}"))
            postInstallOutput = Cmd.executeCommandConsumeStreams(["${new File(httpdBasedir, "etc")}/${customHttpdPostinstall}"], new File(httpdBasedir, "etc"))
          }
        }
      }
    } else if (platform.isWindows()) {
      File target = new File(httpdBasedir, "etc")
      // EWS1
      if (ewsMajorVersion == 1) {
        postInstallOutput = Cmd.executeCommandConsumeStreams([
          "cmd",
          "/C",
          "postinstall.bat"
        ], target)
      } else {

        postInstallOutput = Cmd.executeCommandConsumeStreams([
          "cmd",
          "/C",
          "postinstall.httpd.bat"
        ], target)

        JBFile.delete(new File(httpdBasedir, "etc${sep}httpd${sep}conf.d${sep}mod_cluster-native.conf"))

        httpd.installApacheWindowsService(new File('.'))
      }
    }

    //Windows/Solaris specific -- but it's O.K. there is no if-else, it defaults to 'false'
    if (Boolean.valueOf(Library.getUniversalProperty('BZ1117659_WORKAROUND', false))) {
      JBFile.delete(new File(httpdBasedir, "etc${sep}httpd${sep}conf.d${sep}mod_snmp.conf"))
      JBFile.delete(new File(httpdBasedir, "etc${sep}httpd${sep}conf.d${sep}mod_rt.conf"))
    }

    if (Boolean.valueOf(Library.getUniversalProperty('JWS_198_WORKAROUND', false))) {
      JBFile.replace(new File(httpdBasedir, "etc${sep}httpd${sep}conf${sep}httpd.conf"), "<Files ~ \".ht*\">", "<Files ~ \"^\\.ht\">", true)
    }
    // Ignore any output from the postinstall script, it should be used with caution.
    boolean ignorePostinstallOutput = Boolean.valueOf(Library.getUniversalProperty('IGNORE_POSTINSTALL_OUTPUT', false))
    if (ignorePostinstallOutput || postInstallOutput.exitValue == 0 || postInstallOutput.exitValue == 17) { // 17 means postinstall was already executed
      if (postInstallOutput.exitValue == 17) {
        log.warn("Postinstall was already executed for " + httpd.getServerId())
      }
      JBFile.createFile(httpd.getPostInstallErrFile(), postInstallOutput.stdErr)
      JBFile.createFile(httpd.getPostInstallOutFile(), postInstallOutput.stdOut)
    } else {
      throw new RuntimeException("[${httpd.serverId}] Postinstall went wrong => ${postInstallOutput}")
    }

    // Overrride the user that installed httpd
    String OVERRIDE_INSTALL_AS_USER = Library.getUniversalProperty('OVERRIDE_INSTALL_AS_USER', '')
    if(platform.isRHEL() && !OVERRIDE_INSTALL_AS_USER.isEmpty()) {
      def result = commandExecution(["chown", "-R", "${OVERRIDE_INSTALL_AS_USER}:${OVERRIDE_INSTALL_AS_USER}", "${httpdBasedir}"], new File(httpdBasedir))
    }
  }

  /**
   * Change owner of httpd dir to make sure one who runs test-suite can edit
   * config dirs when unzipped as root(sudo)
   * @param httpd
   */
  static void changeOwnerOfHttpdDir(Httpd httpd, String user) {
    File dir = new File(httpd.getHttpdServerRootFull())
    log.debug("Changing owner of httpd dir ${dir.absolutePath} to $user")
    JBFile.chown(user, dir)
  }

}
