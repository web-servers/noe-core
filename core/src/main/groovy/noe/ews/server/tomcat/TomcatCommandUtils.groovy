package noe.ews.server.tomcat

import noe.common.DefaultProperties
import noe.common.utils.JBFile
import noe.common.utils.Library
import noe.common.utils.Platform
import noe.server.Tomcat

/**
 * Preparation of native commands for Tomcat/Daemon.
 */
class TomcatCommandUtils {
  Tomcat tomcat
  boolean useSudo
  File javaHome

  TomcatCommandUtils(Tomcat tomcat) {
    this.tomcat = tomcat
    this.useSudo = JBFile.useAdminPrivileges

    String javaHomeEnvVar = (DefaultProperties.SERVER_JAVA_HOME) ?
        DefaultProperties.SERVER_JAVA_HOME : DefaultProperties.JAVA_HOME
    this.javaHome = (javaHomeEnvVar != null && !javaHomeEnvVar.isEmpty()) ? new File(javaHomeEnvVar) : null
  }

  static String getTomcatRunUser() {
    return Library.getUniversalProperty('tomcat.run.as', '')
  }

  List<String> prepareDaemonCommand(String operation) {
    List<String> command = [tomcat.getBasedir() + tomcat.getBinPath() + "/daemon.sh"]

    String runAsUser = getTomcatRunUser() ?: new Platform().getActualUser()
    command += [ '--tomcat-user', runAsUser ]

    if (javaHome && javaHome.exists()) {
      command += [ '--java-home', javaHome.getCanonicalPath() ]
    }

    command += [operation]

    if (useSudo) {
      command = ['sudo'] + command
    }

    return command
  }
  
}
