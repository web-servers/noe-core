package noe.ews.utils

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.utils.Cmd
import noe.common.utils.InstallerUtils
import noe.common.utils.JBFile
import noe.common.utils.Library
import noe.common.utils.PathHelper
import noe.common.utils.Platform
import noe.common.utils.Version
import noe.common.utils.db.JDBCLoader
import noe.ews.server.ServerEws
import noe.workspace.WorkspaceAbstract

/**
 * Class representing ews/jws zip installation related stuff
 * @author Jan Stefl <jstefl@redhat.com>
 * @author Martin Vecera <mvecera@redhat.com>
 * @author Bogdan Sikora <bsikora@redhat.com>
 */
@Slf4j
class EwsUtils extends InstallerUtils {

  final static String PREFIX = 'ews'

  String ewsDistro
  String ewsApplicationServer
  String ewsExamples
  String ewsHibernate
  String jwsBaseZipName
  boolean installNatives

  String databaseLabel
  String dbDriverEapVersion
  JwsNameHelper nameHelper

  List<String> sourceZipTested = []



  /**
   * Get EWS from static directory.
   */
  EwsUtils(Version ewsVersion = DefaultProperties.ewsVersion(), String basedir = WorkspaceAbstract.retrieveBaseDir()) {
    super(PREFIX, ewsVersion, basedir)
    nameHelper = new JwsNameHelper(version)
    this.ewsDistro = loadNativeZipFileName()
    this.installNatives = Boolean.parseBoolean(Library.getUniversalProperty('install.jws.natives', true))
    this.databaseLabel = Library.getUniversalProperty('database', 'oracle11gR2RAC')
    this.dbDriverEapVersion = Library.getUniversalProperty('db.driver.eap.version', '6.0.0')
    loadEwsZipNames()
  }

  private void loadEwsZipNames(boolean compatibilityMode = false) {
    if(compatibilityMode) {
      nameHelper.toggleCompatibilityMode()
    }

    this.ewsApplicationServer = nameHelper.getJWSApplicationServerNativeZipFileName()
    this.ewsExamples = nameHelper.getExamplesZipFileName()
    this.ewsHibernate = nameHelper.getHibernateZipName()
    this.jwsBaseZipName = nameHelper.getJWSApplicationServerBaseZipFileName()
  }

  /**
   * Trigger installation of
   */
  synchronized void getIt() {
    log.info("Getting EWS ${version}")

    // TODO: add apacheCore installation
    if (DefaultProperties.apacheCoreVersion()) {

    } else {
      installHttpd()
    }

    // Needed since EWS2
    if (version.getMajorVersion() > 1) {
      installTomcat(false)
    }
  }

  /**
   * Install httpd
   *
   * Note that this JWS-3.0.3+ don't include httpd, {@link DefaultProperties#apacheCoreVersion()} must be set to install
   * apache httpd from jbcs
   * @param purge, if true, previous installation/dir "basedir/server_prefix" will be deleted before installation
   */
  void installHttpd(Boolean purge = true) {
    File httpdHome = new File(basedir, ServerEws.getPrefix())
    if (purge) {
      JBFile.delete(httpdHome)
      if (httpdHome.exists()) {
        throw new RuntimeException('Purge of old httpd installation not successful')
      }
    }

    if (platform.isSolaris() && JBFile.useAdminPrivileges) {
      Cmd.executeSudoCommandConsumeStreams(['/bin/sh', '-c', 'rm -rf /var/cache/mod_*'], new File('.'))
      Cmd.executeSudoCommandConsumeStreams(['/bin/sh', '-c', 'rm -rf /var/log/httpd'], new File('.'))
    }

    installZipFile(ewsDistro)
  }

  /**
   * Install tomcat
   * @param purge, if true, previous installation/dir "basedir/server_prefix" will be deleted before installation
   */
  void installTomcat(Boolean purge = true) {
    File tomcatHome = new File(basedir, ServerEws.getPrefix())
    if (purge) {
      JBFile.delete(tomcatHome)
      if (tomcatHome.exists()) {
        throw new RuntimeException('Purge of old tomcat installation not successful')
      }
    }

    if (platform.isSolaris() && JBFile.useAdminPrivileges) {
      Cmd.executeSudoCommandConsumeStreams(['/bin/sh', '-c', '''rm -rf /var/cache/tomcat*'''], new File('.'))
      Cmd.executeSudoCommandConsumeStreams(['/bin/sh', '-c', '''rm -rf /var/log/tomcat*'''], new File('.'))
    }

    try {
      installJwsZips()
    } catch (FileNotFoundException e) {
      try {
        loadEwsZipNamesInCompatibilityMode()
      } catch (FileNotFoundException ex) {
        log.debug(e.message)
        log.debug(ex.message)
        throw new FileNotFoundException("There were multiple attempts for extracting zip content from various zip compatibility names, but none of them succeed. They don't exists. ${sourceZipTested}")
      }
    }
  }

  private void loadEwsZipNamesInCompatibilityMode() {
    try {
      nameHelper.setArchSeparator("-")
      loadEwsZipNames()
      installJwsZips()
    } catch (FileNotFoundException e) {
      log.debug("Exception caught [${e.message}]")
      nameHelper.setArchSeparator(".")
      loadEwsZipNames(true)
      installJwsZips()
    }
  }

  private void installJwsZips() {
    if (version.getMajorVersion() <= 4) {
      try {
        installZipFile(ewsApplicationServer)
      } catch (FileNotFoundException ex) {
        sourceZipTested.add(ewsApplicationServer)
        throw ex
      }
    } else {
      // JWS 5 and newer base ZIP file without natives
      try {
        installZipFile(jwsBaseZipName)
      } catch (FileNotFoundException ex) {
        sourceZipTested.add(jwsBaseZipName)
        throw ex
      }

      if (installNatives) {
        try {
          installZipFile(ewsApplicationServer)
        } catch (FileNotFoundException ex) {
          sourceZipTested.add(ewsApplicationServer)
          throw ex
        }
      }
    }
    try {
      installZipFile(ewsExamples)
    } catch (FileNotFoundException ex) {
      sourceZipTested.add(ewsExamples)
      throw ex
    }
  }

  /**
   * Unzip hibernate libraries
   *
   * 1. Takes zips from basedir/prefix/hibernate/hibernate.zip
   * 2. If {@link noe.common.utils.Hudson#staticDir} is set to Url, zips will be downloaded
   * 3. Zips are unzipped and content of lib is moved to {@link noe.server.ServerAbstract#getDeplSrcPath()}/hibernate/library
   * 4. Delete zip file if {@link DefaultProperties#isRemoveZipAfterUnzip()}
   */
  void unzipHibernateLibrary() {
    log.debug('Unzipping Hibernate Library')

    File zipFileDest = new File(basedir, ewsHibernate)
    File zipFileSource = new File(PathHelper.join(pathToSource, 'hibernate', ewsHibernate))

    //Old hibernate location might not exist (hibernate/hibernate.zip) let's use hibernate.zip
    zipFileSource = (zipFileSource.exists())?: new File(PathHelper.join(pathToSource, ewsHibernate))

    if (isStaticEnvVerifiedUrl) {
      String downloadZipUrl = "${pathToSource}/${ewsHibernate}"
      Library.downloadFile(downloadZipUrl, zipFileDest)
    } else {
      if (!JBFile.isExistingFile(zipFileSource)) {
        throw new FileNotFoundException("Expected zip file ${zipFileSource} doesn't exist.")
      }
      if (!JBFile.copy(zipFileSource, new File(basedir))) {
        throw new FileNotFoundException("Something has gone wrong with copying file ${zipFileSource} to the targed directory ${basedir}. Check debug log for more information.")
      }
    }

    if (!zipFileDest.exists()) {
      log.error('FAILED to Install zip file ' + zipFileSource + ' to ' + basedir)
      log.error("Zip file $zipFileSource, exist = ${zipFileSource.exists()}")
      throw new RuntimeException("Zip installation failed, $zipFileDest doesn't exits")
    }

    // unzip Hibernate library
    File hibLibraryUnzipDest = new File(PathHelper.join(resourcesPath, 'hibernate', 'library'))
    JBFile.mkdir(hibLibraryUnzipDest)

    File hibernateTmp = new File(hibLibraryUnzipDest, 'tmp')
    JBFile.mkdir(hibernateTmp)

    JBFile.nativeUnzip(zipFileDest, hibernateTmp)

    // pick only 'lib' from Hibernate distribution
    hibernateTmp.listFiles().each { file ->
      File hibernateTmpLib = new File(hibernateTmp, PathHelper.join(file.getName(), 'lib'))
      if (hibernateTmpLib.exists() && hibernateTmpLib.isDirectory()) {
        hibernateTmpLib.eachFile { libName ->
          JBFile.copy(new File(hibernateTmpLib, libName.getName()), hibLibraryUnzipDest)
        }
      }
    }
    JBFile.delete(hibernateTmp)
    if (hibLibraryUnzipDest.listFiles().size() == 0) {
      throw new RuntimeException("Installation of $zipFileSource to $basedir has failed")
    }
    if (DefaultProperties.isRemoveZipAfterUnzip()) {
      JBFile.delete(zipFileDest)
    }
  }

  /**
   * Clean unzipped hibernate libraries
   * Remove {@link noe.server.ServerAbstract#getDeplSrcPath()}/hibernate/library
   */
  boolean removeUnzippedHibernateLib() {
    log.debug('Removing unzipped Hibernate Library')

    return JBFile.delete(new File(PathHelper.join(resourcesPath, 'hibernate', 'library')))

  }

  /**
   * Download JDBC driver
   * To {@link noe.server.ServerAbstract#getDeplSrcPath()}/hibernate/database
   */
  void downloadJDBCDriver() {
    log.debug('Downloading database driver')

    int JDBCVersion = 4

    File dbDriverDest = new File(PathHelper.join(resourcesPath, 'hibernate', 'database'))
    JBFile.mkdir(dbDriverDest)

    JDBCLoader jdbcLoader = new JDBCLoader('EAP', dbDriverEapVersion, databaseLabel, JDBCVersion)
    jdbcLoader.load(dbDriverDest, true, false)
  }

  /**
   *  Remove {@link noe.server.ServerAbstract#getDeplSrcPath()}/hibernate/database
   */
  void removeJDBCDriver() {
    log.debug('Removing database driver')

    JBFile.delete(new File(PathHelper.join(resourcesPath, 'hibernate', 'database')), true)
  }

  /**
   * Counts path to the modules based on platform, ews version and bitArch
   * @param basedir - directory in which the EWS is installed
   * @param bitArch - it is connected to lib based on platform (usually '64' or ''
   * @return path to the modules
   */
  static String getPathToEwsModules(basedir, bitArch = '') {
    Platform platform = new Platform()
    String sep = platform.sep
    String modulesPath = "${basedir}${sep}${ServerEws.getPrefix()}"

    if (platform.isRHEL()) {
      modulesPath += "${sep}httpd${sep}modules"
    } else if (platform.isHP()) {
      modulesPath = "${basedir}${sep}hpws22${sep}apache${sep}modules"
    } else if (platform.isSolaris() || platform.isWindows()) {
      modulesPath += "${sep}lib${bitArch}${sep}httpd${sep}modules"
    } else {
      throw new RuntimeException('Doh... an unsupported platform. You can go ahead and implement it, huh? ;-)')
    }
    return modulesPath
  }

  /**
   * Set 777 privileges for Solaris 11 /system, /system/volatile, /var/log
   * and set 777 privileges for Solaris 10 /var/log and /var/run
   * and others...
   */
  void setPrivilegesOnSolaris(httpdOnly = false) {
    if (!platform.isSolaris()) {
      log.debug(" -- This is not Solaris - skipping ... -- ")
      return
    }

    List<List<String>> commands = []
    if (platform.isSolaris11()) {
      commands = [
              ['chmod', '777', '/system'],
              ['chmod', '777', '/system/volatile']
      ]
    }

    if (platform.isSolaris10()) {
      commands = [
              ['chmod', '-R', '777', '/var/run']
      ]
    }

    commands += [
            ['chmod', '-R', '777', '/var/cache'],
            ['chmod', '-R', '777', '/var/log']
    ]

    if (!httpdOnly) {
      if (ServerEws.extractMajorVersion() >= 5) {
        commands += [
            ['/bin/sh', '-c', "''chmod -R ugo+rwx ${basedir}/${ServerEws.getPrefix()}/tomcat/conf''"],
            ['/bin/sh', '-c', "''chmod g+s ${basedir}/${ServerEws.getPrefix()}/tomcat/webapps''"],
            ['/bin/sh', '-c', "''chmod o+w ${basedir}/${ServerEws.getPrefix()}/tomcat''"],
            ['/bin/sh', '-c', "''chmod o+w ${basedir}/${ServerEws.getPrefix()}/tomcat/bin/setenv.sh''"]
        ]
      } else {
        commands += [
            ['/bin/sh', '-c', "''chmod +x ${basedir}/${ServerEws.getPrefix()}/share/tomcat*/bin/*.sh''"],
            ['/bin/sh', '-c', "''chmod -R ugo+rwx ${basedir}/${ServerEws.getPrefix()}/share/tomcat*/conf''"],
            ['/bin/sh', '-c', "''chmod g+s ${basedir}/${ServerEws.getPrefix()}/share/tomcat*/webapps''"],
            ['/bin/sh', '-c', "''chmod o+w ${basedir}/${ServerEws.getPrefix()}/share/apache-tomcat-*''"]
        ]
      }
    }

    commands.each { command ->
      if (Cmd.executeSudoCommandConsumeStreams(command, new File('.')).exitValue > 0) {
        throw new RuntimeException("${command} FAILED")
      }
    }
  }

  /**
   * Maximalize limits on Solaris
   */
  void setMaxLimitsOnSolaris(httpdOnly = false) {
    if (!platform.isSolaris()) {
      log.debug(" -- This is not Solaris - skipping ... -- ")
      return
    }

    String wwwDestination
    if (ServerEws.extractMajorVersion() >= 5) {
      wwwDestination = "${basedir}/${ServerEws.getPrefix()}/tomcat/webapps"
    } else {
      wwwDestination = "${basedir}/${ServerEws.getPrefix()}/var/www"
    }
    Library.copyResourceTo("scripts/system/unix/.profile", new File(wwwDestination))
    List<List<String>> commands = [
            ['chmod', 'ugo+rwx', "${wwwDestination}/.profile"],
            ['chown', 'apache', "${wwwDestination}/.profile"]
    ]

    if (!httpdOnly) {
      String tomcatDestination
      if (ServerEws.extractMajorVersion() >= 5) {
        tomcatDestination = "${basedir}/${ServerEws.getPrefix()}/tomcat"
      } else {
        tomcatDestination = "${basedir}/${ServerEws.getPrefix()}/var/tomcat"
      }
      Library.copyResourceTo("scripts/system/unix/.profile", new File(tomcatDestination))
      String runAsUser = Library.getUniversalProperty('tomcat.run.as', new Platform().getActualUser())
      commands += [
              ['chmod', 'ugo+rwx', "${tomcatDestination}/.profile"],
              ['chown', runAsUser, "${tomcatDestination}/.profile"]
      ]
    }

    commands.each { command ->
      if (Cmd.executeCommandConsumeStreams(command, new File('.')).exitValue > 0) {
        if (JBFile.useAdminPrivileges) {
          if (Cmd.executeSudoCommandConsumeStreams(command, new File('.')).exitValue > 0) {
            throw new RuntimeException("${command} FAILED")
          }
        } else {
          throw new RuntimeException("${command} FAILED")
        }
      }
    }
  }

  /**
   * Delete old tomcatX.pid file.
   */
  void deleteOldTomcatPidOnSolaris() {
    if (platform.isSolaris()) {
      File tomcatPid
      ServerEws.getTomcatVersions(version).each { version ->
        tomcatPid = new File("/var/run/tomcat${version}.pid")
        if (tomcatPid.exists()) {
          def command = ['rm', '/var/run/tomcat' + version + '.pid']
          if (Cmd.executeCommandConsumeStreams(command, new File('.')).exitValue > 0) {
            if (JBFile.useAdminPrivileges) {
              if (Cmd.executeSudoCommandConsumeStreams(command, new File('.')).exitValue > 0) {
                throw new RuntimeException("${command} FAILED")
              }
            } else {
              throw new RuntimeException("${command} FAILED")
            }
          }
        }
      }
    }
  }

  /**
   * This is little hack, for some testing it is needed to run under different user than tomcat (Jon agent,...).
   */
  void makeTomcatsRunnableAnyUserSolaris() {
    if (platform.isSolaris()) {
      String user = Library.getUniversalProperty('user.name', Cmd.actualUser)
      ServerEws.getTomcatVersions(version).each { version ->
        List<List<String>> commands
        String ewsPrefix = "${basedir}/${ServerEws.getPrefix()}"
        if (ServerEws.extractMajorVersion() >= 5) {
          commands = [
              ['chown', '-R', "${user}", "${ewsPrefix}/tomcat/logs"],
              ['chown', '-R', "${user}", "${ewsPrefix}/tomcat/conf"],
              ['chown', '-R', "${user}", "${ewsPrefix}/tomcat/webapps"],
              ['chown', '-R', "${user}", '/var/cache/tomcat']
          ]
        } else {
          commands = [
              ['chown', '-R', "${user}", "/var/log/tomcat${version}"],
              ['chown', '-R', "${user}", "${ewsPrefix}/share/tomcat${version}/conf"],
              ['chown', '-R', "${user}", "${ewsPrefix}/share/tomcat${version}/webapps"],
              ['chown', '-R', "${user}", "/var/cache/tomcat${version}"]
          ]
        }
        commands.each { command ->
          if (Cmd.executeCommandConsumeStreams(command, new File('.')).exitValue > 0) {
            if (JBFile.useAdminPrivileges) {
              if (Cmd.executeSudoCommandConsumeStreams(command, new File('.')).exitValue > 0) {
                throw new RuntimeException("${command} FAILED")
              }
            } else {
              throw new RuntimeException("${command} FAILED")
            }
          }
        }
      }
    }
  }

  private int maskTomcatServiceToDelete() {
    int res = 0
    List<Version> tomcatVersions = ServerEws.getTomcatVersions()

    tomcatVersions.each {
      res += Cmd.executeSudoCommandConsumeStreams(['/usr/sbin/svccfg', 'delete', "tomcat${it}:default"], new File('.')).exitValue
    }

    if (res > 0) {
      log.warn("masking tomcat service to delete failed")
    }

    return res
  }



  /**
   * Explicitly set JAVA_HOME for Tomcat instances.
   */
  void setJavaHomeTomcatSolaris() {
    File sysConfFile = null
    String javaHome = (DefaultProperties.SERVER_JAVA_HOME) ?
        DefaultProperties.SERVER_JAVA_HOME : DefaultProperties.JAVA_HOME

    log.debug "JAVA_HOME ${javaHome}"

    if (!platform.isSolaris()) {
      log.debug " -- This is not Solaris - skipping ... -- "
      return
    }

    ServerEws.getTomcatVersions(version).each { version ->
      if (ServerEws.extractMajorVersion() >= 5) {
        sysConfFile = new File(basedir,
            "${ServerEws.getPrefix()}${platform.sep}tomcat${platform.sep}conf${platform.sep}jws${ServerEws.extractMajorVersion()}-tomcat.conf")
      } else {
        sysConfFile = new File(basedir, "${ServerEws.getPrefix()}${platform.sep}etc${platform.sep}sysconfig${platform.sep}tomcat${version}")
      }
      if (sysConfFile.exists()) {
        if (javaHome) {
          JBFile.makeAccessible(sysConfFile)
          JBFile.replaceregexp(sysConfFile, '# JAVA_HOME=(.*)', 'JAVA_HOME="' + javaHome + '"')

          log.trace "${sysConfFile.getAbsolutePath()}"
          log.trace "${sysConfFile.text}"
          log.trace "${sysConfFile.getAbsolutePath()}"
        } else {
          log.error "JAVA_HOME is empty -> skipping JAVA_HOME setting in ${sysConfFile}. There could be problems during Tomcat startup."
        }
      } else {
        throw new RuntimeException("File ${sysConfFile} not found. It is needed for explicit settings JAVA_HOME for tomcat ${version.toString()}.")
      }
    }
  }

  /**
   * Set User Home for Apache and Tomcat users on Solaris.
   */
  void setUserHomeSolaris(httpdOnly = false) {
    if (!platform.isSolaris()) {
      log.debug " -- This is not Solaris - skipping ... -- "
      return
    }
    String passwd = '/etc/passwd'
    File passwdFile = new File(passwd)
    List<String> command = ['cp', passwd, '/etc/passwd.tmp']
    if (Cmd.executeSudoCommandConsumeStreams(command, new File('.')).exitValue > 0) {
      throw new RuntimeException("${command} FAILED")
    }
    // fix for EWS 1.0.2 - create var/tomcat dir
    if ((ServerEws.extractMajorVersion() == 1) && (!httpdOnly)) {
      new File("${basedir}/${ServerEws.getPrefix()}/var/tomcat").mkdir()
    }
    JBFile.makeAccessible(passwdFile)
    JBFile.replaceregexp(passwdFile, 'apache(.*)', "apache:x:48:48:Apache:${basedir}/${ServerEws.getPrefix()}/var/www:/bin/sh")
    if (!httpdOnly) {
      JBFile.replaceregexp(passwdFile, 'tomcat(.*)', "tomcat:x:91:91:Apache Tomcat:${basedir}/${ServerEws.getPrefix()}/var/tomcat:/bin/sh")
    }
    log.trace "${passwdFile.getAbsolutePath()}"
    log.trace "${passwdFile.text}"
    log.trace "${passwdFile.getAbsolutePath()}"
  }

  /**
   * Build up name of httpd zip
   * @return name of httpd zip
   */
  private String loadNativeZipFileName() {
    //jboss-ews-2.0.0-ER3-RHEL6-i386.zip
    int ewsMajorVersion = version.getMajorVersion()
    String fn = ewsMajorVersion <= 2 ? "jboss-ews-" : "jws-"

    String platArchSep = '-'

    // EWS 1
    if (ewsMajorVersion == 1) {
      // prefix for Solaris and Windows is other than on RHEL, and EWS
      if (platform.isWindows() || platform.isSolaris()) fn = 'RHATews-'
    }
    // EWS 2 or higher
    else {
      fn += "httpd-"
    }

    if (platform.isRHEL()) {
      if (platform.isRHEL4()) fn += "${version}-RHEL4-"
      if (platform.isRHEL5()) fn += "${version}-RHEL5-"
      else if (platform.isRHEL6()) fn += "${version}-RHEL6-"
      else if (platform.isRHEL7()) fn += "${version}-RHEL7-"
      else if (platform.isRHEL8()) fn += "${version}-RHEL8-"
      else if (platform.isRHEL9()) fn += "${version}-RHEL9-"
      else if (platform.isRHEL10()) fn += "${version}-RHEL10-"
      if (platform.isX86()) {
        fn += "i386"
      } else if (platform.isX64()) {
        fn += "x86_64"
      } else if (platform.isPpc64()) {
        fn += "ppc64"
      } else if (platform.isS390x()) {
        fn += "s390x"
      } else {
        log.error("platformName can't be determined. osName: ${platform.osName}, osArch: ${platform.osArch}," +
                " osVersion: ${platform.osVersion}, archModel: ${platform.archModel}, solPreferredArch: ${platform.solPreferredArch}")
      }
    } else if (platform.isWindows()) {
      if (ewsMajorVersion > 1) {
        fn += (platform.isX86() ? "${version}-win6.i686" : "${version}-win6.x86_64")
      } else {
        fn += (platform.isX86() ? "${version}-windows32-i386" : "${version}-windows64-x86_64")
      }

    } else if (platform.isSolaris()) {
      if (ewsMajorVersion > 1) platArchSep = '.'
      def ver = '10' // on Solaris 10 and 11 -> build for 10
      def os = (ewsMajorVersion == 1) ? 'solaris' : 'sun'
      if (platform.isSparc()) {
        fn += "${version}-${os}${ver}${platArchSep}sparc64"
      } else if (platform.solPreferredArch == 32) {
        fn += "${version}-${os}${ver}${platArchSep}i386"
      } else if (platform.solPreferredArch == 64) {
        fn += "${version}-${os}${ver}${platArchSep}x86_64"
      } else {
        log.error("platformName can't be determined. osName: ${platform.osName}, osArch: ${platform.osArch}," +
                " osVersion: ${platform.osVersion}, archModel: ${platform.archModel}, solPreferredArch: ${platform.solPreferredArch}")
      }
    }

    fn += '.zip'

    log.trace('GetEWS: nativeZipFileName: ' + fn)

    return fn
  }
}
