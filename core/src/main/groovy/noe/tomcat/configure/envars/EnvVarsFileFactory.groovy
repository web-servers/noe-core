package noe.tomcat.configure.envars

import noe.common.NoeContext
import noe.common.utils.FileStateVault
import noe.server.Tomcat


/**
 * IMPORTANT: For usage within noe-core:tomcat.configure only
 *
 * Factory for creating specific environment file handler.
 */
class EnvVarsFileFactory {

  static EnvVarsFile getInstance(Tomcat tomcatInstance, FileStateVault vault) {
    return (isRpmTestingRunning()) ? RpmTomcatEnvVarsFileFactory.getInstance(tomcatInstance, vault) : new ZipTomcatEnvVarsFile(tomcatInstance, vault)
  }

  private static boolean isRpmTestingRunning() {
    NoeContext.forCurrentContext().consistsOf(['rhel','tomcat']) || NoeContext.forCurrentContext().consistsOf(['ews','rpm'])
  }
}
