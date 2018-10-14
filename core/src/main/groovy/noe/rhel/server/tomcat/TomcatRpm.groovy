package noe.rhel.server.tomcat

import groovy.util.logging.Slf4j
import noe.common.NoeContext
import noe.common.utils.Cmd
import noe.common.utils.JBFile
import noe.common.utils.Version
import noe.server.Tomcat
/**
 * RHEL Tomcat server
 *
 * @author Jan Martiska   <jstefl@redhat.com>
 * @author Jan Stefl      <jmartiska@redhat.com>
 * @author Ondrej Chaloupka   <ochaloup@redhat.com>
 * @author Michal Hasko   <mhasko@redhat.com>
 *
 */
@Slf4j
class TomcatRpm extends Tomcat {
  String serviceName
  List<String> pinExtractionPaths
  String defaultPidFilePath

  TomcatRpm(String basedir, version, String tomcatDir = '') {
    super(basedir, version)
    // on RHEL7, package name for Tomcat 7 in base channel is 'tomcat' == we don't need the version suffix.
    def dontNeedVersionSuffix = platform.isRHEL7() && this.version == new Version('7') && NoeContext.forCurrentContext().areInSingleGroup(['rhel','tomcat'])
    this.tomcatDir = "tomcat" + (dontNeedVersionSuffix ? "" : this.version)
    this.serviceName = "tomcat" + (dontNeedVersionSuffix ? "" : this.version)
    this.basedir = "/usr/share/${this.tomcatDir}"
    this.refBasedir = basedir + '/tomcat' + this.version

    // please initialize in setDefault() method
    setDefault()
  }

  void setDefault() {
    super.setDefault()
    this.configDirs = ["/conf"] //backup (ServerAbstract.backupConfs)
    this.logDirs = ["/logs"]
    this.binPath = '/bin'
    this.start = ['sudo', 'service', serviceName, 'start'] // TODO add runContext see TomcatRhel.groovy
    // TODO ?? this.startSecurity = ''
    this.stop = ['sudo', 'service', serviceName, 'stop']
    this.confDeploymentPath = this.basedir + "/conf"
    this.deploymentPath = this.basedir + "/webapps"
    this.libDir = this.basedir + "/lib"
    this.extrasDir = this.libDir + "/extras"
    this.extrasDirs = [this.extrasDir, this.basedir + '/extras']
    this.binDir = this.basedir + "/bin"
    this.jsvc = "" // TODO: see the extrasDir comment above
    this.workDir = "/work" // Tomcat.undeployByDeleting
    pinExtractionPaths = [
        "/etc/sysconfig/${serviceName}",
        "/etc/${serviceName}/${serviceName}.conf"
    ]
    defaultPidFilePath = "/var/run/${serviceName}.pid"

    // ! please add new paths like absolute paths
  }

  File getPidFile() {
    // Search for 'CATALINA_PID' variable which holds path to
    // tomcat pid file

    // Start searching in sysconfig service config file, if not
    // successful then try to find it in service config file. If not
    // successful again, use default value.

    String actualPidFile = ""
    log.debug("Scanning ${pinExtractionPaths} for CATALINA_PID ")
    for ( path in pinExtractionPaths ) {
      File confFile = new File(path)

      // Check whether file exists
      if (confFile.exists()) {
        // Read file content and replace comments
        String fileText = confFile.text
        fileText = fileText.replaceAll(~/#.*/, "")

        // Try to search for 'CATALINA_PID' in file content
        def m = fileText =~ /CATALINA_PID=(.*)/

        // Get value - actually we want last occurence of CATALINA_PID as that
        // possibly rewrites all preceding occurrences.
        for (int i = m.size()-1; i >= 0; i--) {
          if (m[i].size() > 1) {
            actualPidFile = m[i][1]
            // Remove quotes if present
            actualPidFile = actualPidFile.replaceAll(~/\"/, "")
            actualPidFile = actualPidFile.replaceAll(~/\'/, "")
            break
          }
        }

        if (actualPidFile.length() > 0) {
            break
        }
      }
    }

    if (actualPidFile.length() > 0) {
      log.debug("Actual PID file found. Using ${actualPidFile}.")
      return new File("${actualPidFile}")
    } else {
      log.debug("Cannot locate PID file. Using ${defaultPidFilePath} instead.")
      return new File("${defaultPidFilePath}")
    }
  }

  Integer extractPid() {
    try {
      def pidAsStr = pidFile.text
      pidAsStr = pidAsStr.trim()
      pidAsStr = pidAsStr.replaceAll('"', '')
      pid = Integer.valueOf(pidAsStr)
    }
    catch (e) {
      log.debug("PID file is not accessible. But continuing ...")
    }

    return pid
  }

  @Override
  boolean kill() {
    boolean res = super.kill()
    // must not delete pid file on RHEL7, otherwise tomcat would not be able to create it again
    if (!platform.isRHEL7()) JBFile.delete(pidFile)
    return res
  }

  void startCatalina(conf = [:]) {
    throw new UnsupportedOperationException("startCatalina is not possible for RPMs")
  }

  void stopCatalina(conf = [:]) {
    throw new UnsupportedOperationException("startCatalina is not possible for RPMs")
  }

  void startJaas(conf = [:]) {
    def jaasOpts = " -Djava.security.auth.login.config=" + getDeplSrcPath() + "/tomcat/realmTest/templates/jaas/jaas.config"

    configDirs.each { confDir ->
      def filePath = ""
      //RHEL 7 and above should have only tomcat package, RHEL6 has tomcat6
      if (this.serviceName == "tomcat") {
        // path to conf file contains any bash expansion like ${CATALINA_OPTS} have to be pasted into conf.d folder,
        // otherwise it leads to failure during starting tomcats https://bugzilla.redhat.com/show_bug.cgi?id=1221896
        filePath = getServerRoot() + platform.sep + confDir + platform.sep + "conf.d" + platform.sep + "addition.conf"
      } else {
        filePath = getServerRoot() + platform.sep + confDir + platform.sep + "${this.serviceName}.conf"
      }
      JBFile.insertTextToSpecifiedPositionInFile(filePath, "JAVA_OPTS=\"\${JAVA_OPTS} ${jaasOpts}\"", -1)
    }

    // Start the server
    start()
  }

  void startJsvc(conf = [:]) {
    throw new UnsupportedOperationException("Starting method for jsvc is not implemented for RPM yet.")
  }

  void stopJsvc(conf = [:]) {
    throw new UnsupportedOperationException("Stopping method for jsvc is not implemented for RPM yet.")
  }

  void startServerJmx() {
    String jmxOpts = " -Dcom.sun.management.jmxremote.port=${jmxPort}" +
        " -Dcom.sun.management.jmxremote.ssl=false" +
        " -Dcom.sun.management.jmxremote.authenticate=false"

    configDirs.each { confDir ->
      def filePath = ""
      //RHEL 7 and above should have only tomcat package, RHEL6 has tomcat6
      if (this.serviceName == "tomcat" || platform.isRHEL7()) {
        // path to conf file contains any bash expansion like ${CATALINA_OPTS} have to be pasted into conf.d folder,
        // otherwise it leads to failure during starting tomcats https://bugzilla.redhat.com/show_bug.cgi?id=1221896
        filePath = getServerRoot() + platform.sep + confDir + platform.sep + "conf.d" + platform.sep + "addition.conf"
      } else {
        filePath = getServerRoot() + platform.sep + confDir + platform.sep + "${this.serviceName}.conf"
      }
      Cmd.executeSudoCommand(["/bin/sh", "-c", "echo CATALINA_OPTS=\\\"\\\${CATALINA_OPTS} ${jmxOpts}\\\" >> ${filePath}"], new File("."))
    }

    // Start the server
    start()
  }

  void startSecurity(String tomcatConfName = this.serviceName) {
    configDirs.each { confDir ->
      String filePath = getServerRoot() + platform.sep + confDir + platform.sep + tomcatConfName + ".conf"
      JBFile.insertTextToSpecifiedPositionInFile(filePath,"SECURITY_MANAGER=true", -1)
    }
    handleExamplesPermisions()
    // Start the server
    start()
  }

  void setRequestedSELinuxContext(String context) {
    this.start = ['runcon', '-t', context, this.start]
  }
}
