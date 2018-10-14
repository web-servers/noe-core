package noe.tomcat.configure

import noe.common.utils.Cmd
import noe.common.utils.JBFile
import noe.common.utils.Library
import noe.common.utils.Platform
import noe.server.Tomcat;

/**
 * Abstraction for JMX remote acccess file
 *
 * @link http://tomcat.apache.org/tomcat-7.0-doc/monitoring.html#Enabling_JMX_Remote
 */
class JmxRemoteAccessFileTomcat {

  enum Access { readonly, readwrite }

  final private File path
  private Access monitorRole
  private Access controlRole

  Tomcat tomcat


  JmxRemoteAccessFileTomcat(Tomcat tomcat, File path) {
    this.path = path
    this.monitorRole = Access.readonly
    this.controlRole = Access.readwrite
    this.tomcat = tomcat
  }

  /**
   * Remove file if exists and create new.
   */
  void reCreate() {
    String nl = new Platform().nl
    path.delete()

    JBFile.createFile(path,
      "monitorRole ${this.monitorRole}" + nl +
      "controlRole ${this.controlRole}" + nl
    )

    JmxTomcatUtils.prepareAccessRights(tomcat, path)
  }

  File getPath() {
    return path
  }
}
