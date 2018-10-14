package noe.jbcs.utils

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.utils.Cmd
import noe.common.utils.JBFile
import noe.common.utils.Library
import noe.common.utils.Platform
import noe.ews.server.ServerEws
import noe.ews.server.tomcat.TomcatCommandUtils
import noe.server.Tomcat

@Slf4j
class TomcatHelper {
  Platform platform

  TomcatHelper(Platform platform) {
    this.platform = platform
  }

  static String getBasedirSuffix() {
    return ServerEws.getPrefix()
  }

  void runPostinstall(Tomcat tomcat) {
    Map postInstallOutput
    def ewsMajorVersion = ServerEws.extractMajorVersion()
    def apacheCoreVersion = DefaultProperties.apacheCoreVersion()
    def tomcatBasedir = "${tomcat.rootBasedir}${platform.sep}${ServerEws.getPrefix()}"
    File postinstallFolder
    if (ewsMajorVersion >= 5) {
      postinstallFolder = new File(tomcatBasedir, 'tomcat')
    } else {
      postinstallFolder = new File(tomcatBasedir, 'etc')
    }

    if (platform.isSolaris()) {
      final Boolean tomcatWithSudo = Library.getUniversalProperty('tomcat.postinstall.with.sudo', JBFile.useAdminPrivileges).toBoolean()
      if (tomcatWithSudo) {
        postInstallOutput = Cmd.executeSudoCommandConsumeStreams(['./.postinstall.tomcat'], postinstallFolder)
      } else {
        if (apacheCoreVersion || ewsMajorVersion >= 3) {
          postInstallOutput = Cmd.executeCommandConsumeStreams(["sh", "./.postinstall.tomcat"], postinstallFolder)
        } else {
          def isIt64 = (platform.isSparc() || platform.isX64()) ? "64" : ""
          String customTomcatPostinstall = ".postinstall.tomcat.nosudo.solaris${isIt64}"
          JBFile.copy(Library.retrieveResourceAsFile("tomcat/${customTomcatPostinstall}"), postinstallFolder, false, true)
          JBFile.chmod("ugo+rx", new File(postinstallFolder, customTomcatPostinstall))
          postInstallOutput = Cmd.executeCommandConsumeStreams(["${postinstallFolder}/${customTomcatPostinstall}"], postinstallFolder)

        }
      }
    } else if (platform.isWindows()) {
      if (ewsMajorVersion < 5) {
        postInstallOutput = Cmd.executeCommandConsumeStreams([
            "cmd",
            "/C",
            "postinstall.tomcat.bat"
        ], postinstallFolder)
      } else {
        //There is no postintall script in JWS5, creating empty successful run
        postInstallOutput = [exitValue: 0]
      }
    }
    if ( !platform.isRHEL() ) {
      if (postInstallOutput.exitValue == 0) {
        JBFile.createFile(tomcat.getPostInstallErrFile(), postInstallOutput.stdErr)
        JBFile.createFile(tomcat.getPostInstallOutFile(), postInstallOutput.stdOut)
      } else {
        throw new RuntimeException("[${tomcat.serverId}] Postinstall went wrong => ${postInstallOutput}")
      }
    }

    defineTomcatGroupOwnershipRights(tomcat)
  }

  /**
   * Access rights has to correspond with user under who is Tomcat executed.
   */
  private void defineTomcatGroupOwnershipRights(Tomcat tomcat) {
    if (JBFile.useAdminPrivileges && !platform.isWindows()) {
      String runTomcatAs = TomcatCommandUtils.getTomcatRunUser()
      int tomcatVersion = tomcat.getVersion().getMajorVersion()

      if (!runTomcatAs.isEmpty()) {
        if (platform.isSolaris()) {
          Cmd.executeSudoCommandConsumeStreams(['chgrp', '-R', runTomcatAs, "/var/cache/tomcat${tomcatVersion}"], new File('.'))
          Cmd.executeSudoCommandConsumeStreams(['chgrp', '-R', runTomcatAs, "/var/log/tomcat${tomcatVersion}"], new File('.'))
        }
      }
    }
  }

  static makeTomcatsRunnableAnyUserSolaris(Tomcat tomcat) {
    String user = Library.getUniversalProperty('user.name', Cmd.actualUser)
    int ewsMajorVersion = ServerEws.extractMajorVersion()
    ServerEws.getTomcatVersions(ewsMajorVersion).each { version ->
      // JWS4 and later has no version after tomcat
      List<String> dirs
      if (ewsMajorVersion >= 5) {
        // there is new folder structure since JWS5
        dirs = [
            '/var/log/tomcat',
            "${tomcat.rootBasedir}/${getBasedirSuffix()}/tomcat/bin".toString(),
            "${tomcat.rootBasedir}/${getBasedirSuffix()}/tomcat/conf".toString(),
            "${tomcat.rootBasedir}/${getBasedirSuffix()}/tomcat/lib".toString(),
            "${tomcat.rootBasedir}/${getBasedirSuffix()}/tomcat/logs".toString(),
            "${tomcat.rootBasedir}/${getBasedirSuffix()}/tomcat/webapps".toString(),
            '/var/cache/tomcat'
        ]
      } else if (ewsMajorVersion == 4) {
        dirs = [
            '/var/log/tomcat',
            "${tomcat.rootBasedir}/${getBasedirSuffix()}/share/tomcat/conf".toString(),
            "${tomcat.rootBasedir}/${getBasedirSuffix()}/share/tomcat/webapps".toString(),
            '/var/cache/tomcat'
        ]
      } else {
        dirs = [
            '/var/log/tomcat' + version.toString(),
            "${tomcat.rootBasedir}/${getBasedirSuffix()}/share/tomcat${version.toString()}/conf".toString(),
            "${tomcat.rootBasedir}/${getBasedirSuffix()}/share/tomcat${version.toString()}/webapps".toString(),
            '/var/cache/tomcat' + version.toString()
        ]
      }
      dirs.each { String dir ->
        log.debug("Changing ownership recursively for ${dir} to ${user}")
        if (new File(dir).exists()) {
          if (JBFile.chown(user, new File(dir)) != 0) {
            throw new RuntimeException("Failed changing ownership of ${dir} to user ${user}")
          }
        }
      }
    }
  }

  /**
   * Set EOL for httpd on Solaris
   */
  static setEolOnSolarisTomcat(Tomcat tomcat) {
    List<File> directories
    List<File> files
    int ewsMajorVersion = ServerEws.extractMajorVersion()
    ServerEws.getTomcatVersions(ewsMajorVersion).each { version ->
      if (ewsMajorVersion == 4) {
        directories = [new File("${tomcat.rootBasedir}/${getBasedirSuffix()}/share/tomcat/conf")]
        files =  [
            new File("${tomcat.rootBasedir}/${getBasedirSuffix()}/share/tomcat/bin/startup.sh"),
            new File("${tomcat.rootBasedir}/${getBasedirSuffix()}/share/tomcat/bin/shutdown.sh")
        ]
      } else {
        directories = [new File("${tomcat.rootBasedir}/${getBasedirSuffix()}/share/tomcat${version.toString()}/conf")]
        files = [
            new File("${tomcat.rootBasedir}/${getBasedirSuffix()}/share/tomcat${version.toString()}/bin/startup.sh"),
            new File("${tomcat.rootBasedir}/${getBasedirSuffix()}/share/tomcat${version.toString()}/bin/shutdown.sh")
        ]
      }
    }

    directories.each { File directory ->
      if (directory.exists()) {
        directory.eachFile { File file ->
          if (file.isFile()) file.append("\n")
        }
      }
    }

    files.each { file ->
      if (file.exists()) {
        if (JBFile.useAdminPrivileges) {
          Cmd.executeSudoCommand('echo "\n" >> ' + file.absolutePath, new File('.'))
        }
      }
    }
  }
}
