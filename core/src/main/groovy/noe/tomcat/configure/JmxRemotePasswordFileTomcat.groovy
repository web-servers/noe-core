package noe.tomcat.configure

import noe.common.utils.Cmd
import noe.common.utils.JBFile
import noe.common.utils.Library
import noe.common.utils.Platform
import noe.server.Tomcat

/**
 * Abstraction for JMX remote password file.
 * Default password for monitoreRole and controlRole is `tomcat`.
 *
 * @link http://tomcat.apache.org/tomcat-7.0-doc/monitoring.html#Enabling_JMX_Remote
 */
class JmxRemotePasswordFileTomcat {

  private final File path
  private final Map<String, String> rolePassword

  Tomcat tomcat


  JmxRemotePasswordFileTomcat(Tomcat tomcat, File path, Map<String, String> rolePassword) {
    this.path = path
    this.rolePassword = rolePassword
    this.tomcat = tomcat
  }

  /**
   * Remove file if exists and create new.
   * Alse sets required access rights (`0400`) on it on unix-like OS.
   */
  void reCreate() {
    Platform platform = new Platform()
    String nl = platform.nl
    path.delete()

    def content = ""

    rolePassword.each { String role, String password ->
      content += "${role} ${password}"
      content += "${nl}"
    }

    JBFile.createFile(path, content)

    JmxTomcatUtils.prepareAccessRights(tomcat, path)
  }

  File getPath() {
    return path
  }
}
