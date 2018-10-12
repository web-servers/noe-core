package noe.tomcat.configure

import noe.common.utils.FileStateVault
import noe.common.utils.Platform
import noe.server.Tomcat
import noe.tomcat.configure.envars.EnvVarsFile
import noe.tomcat.configure.envars.EnvVarsFileFactory

/**
 * IMPORTANT: For usage within noe-core:tomcat.configure only
 *
 * JMX configuration in Tomcat
 * Handles:
 * <ul>
 *   <li>CATALINA_OPTS in setenv.sh (or setenv.bat) or rpm specific environment file</li>
 *   <li>Handles access rights for fields `jmxremote.authenticate` and `jmxremote.password`</li>
 *   <li>Files `jmxremote.authenticate` and `jmxremote.password` are
 *       recreated always (whole content of existing files is replaced with new content)</li>
 * </ul>
 *
 * It is user responsibility to set all values semantically.
 */
class JmxConfiguratorTomcat {

  private final EnvVarsFile envVarsFile
  private final FileStateVault vault

  JmxConfiguratorTomcat(Tomcat tomcatInstance, FileStateVault vault) {
    this.envVarsFile = EnvVarsFileFactory.getInstance(tomcatInstance, vault)
    this.vault = vault
  }

  JmxConfiguratorTomcat(Tomcat tomcatInstance) {
    this(tomcatInstance, new FileStateVault())
  }

  /**
   * Method does not any validation, it is user responsibility to pass valid data
   */
  JmxConfiguratorTomcat define(JmxTomcat jmx) {
    List catalinaOpsValue = createCatalinaOpsValue(jmx)

    if (!catalinaOpsValue.isEmpty()) {
      envVarsFile.appendVariable("CATALINA_OPTS", catalinaOpsValue.join(' '))

      if (jmx.accessFile) jmx.accessFile.reCreate()
      if (jmx.passwordFile) jmx.passwordFile.reCreate()
    }

    return this
  }

  private List createCatalinaOpsValue(JmxTomcat jmx) {
    def value = []

    if (jmx.getPort() != null && jmx.getPort() != "") {
      value << "-Dcom.sun.management.jmxremote.port=${jmx.getPort()}"
    }

    if (jmx.getSsl() != null && jmx.getSsl() != "") {
      value << "-Dcom.sun.management.jmxremote.ssl=${jmx.getSsl()}"
    }

    if (jmx.getAuthenticate() != null && jmx.getAuthenticate() != "") {
      value << "-Dcom.sun.management.jmxremote.authenticate=${jmx.getAuthenticate()}"
    }

    if (jmx.getPasswordFile() != null && jmx.getPasswordFile() != "") {
      value << "-Dcom.sun.management.jmxremote.password.file=${jmx.getPasswordFile().getPath().path}"
    }

    if (jmx.getAccessFile() != null && jmx.getAccessFile() != "") {
      value << "-Dcom.sun.management.jmxremote.access.file=${jmx.getAccessFile().getPath().path}"
    }
    if (!value.isEmpty()) {
      value << "-Dcom.sun.management.jmxremote"
    }

    return value
  }
}
