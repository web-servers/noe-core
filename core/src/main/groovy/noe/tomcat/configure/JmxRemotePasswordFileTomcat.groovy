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
  /**
   * JMX remote access require password specified. If not given the default is used.
   */
  public static final String DEFAULT_PASSWORD = 'tomcat'

  private final File path
  private final String monitorRolePassword
  private final String controlRolePassword

  Tomcat tomcat


  JmxRemotePasswordFileTomcat(Tomcat tomcat, File path) {
    this(tomcat, path, DEFAULT_PASSWORD, DEFAULT_PASSWORD)
  }

  JmxRemotePasswordFileTomcat(Tomcat tomcat, File path, String monitorRolePassword, String controlRolePassword) {
    this.path = path
    this.monitorRolePassword = monitorRolePassword
    this.controlRolePassword = controlRolePassword
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
    if (!this.monitorRolePassword.isEmpty()) {
      content += "monitorRole ${this.monitorRolePassword}" + nl
    }
    if (!this.controlRolePassword.isEmpty()) {
      content += "controlRole ${this.controlRolePassword}" + nl
    }

    JBFile.createFile(path, content)

    JmxTomcatUtils.prepareAccessRights(tomcat, path)
  }

  File getPath() {
    return path
  }
}
