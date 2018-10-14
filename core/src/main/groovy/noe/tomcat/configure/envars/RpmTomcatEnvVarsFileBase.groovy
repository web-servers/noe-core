package noe.tomcat.configure.envars

import noe.common.utils.FileStateVault
import noe.common.utils.Platform

/**
 * IMPORTANT: For usage within noe-core:tomcat.configure only
 */
abstract class RpmTomcatEnvVarsFileBase implements EnvVarsFile {

  protected File envFile
  protected FileStateVault vault


  /**
   * Appends new environment variable Tomcat specific file.
   * No special check in file are performed just straightforward appending.
   */
  @Override
  void appendVariable(String name, String value) {
    Platform platform = new Platform()
    def expression = "${name}=${value}${platform.nl}"

    vault?.push(envFile)

    envFile.append(expression)
  }

  /**
   * Returns environment file being modified.
   */
  @Override
  File getEnvFile() {
    return envFile
  }
}
