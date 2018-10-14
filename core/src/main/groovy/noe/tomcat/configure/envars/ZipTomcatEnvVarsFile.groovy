package noe.tomcat.configure.envars

import noe.common.utils.FileStateVault
import noe.common.utils.JBFile
import noe.common.utils.Platform
import noe.server.Tomcat

/**
 * IMPORTANT: For usage within noe-core:tomcat.configure only
 *
 * Abstraction of Tomcat setenv.sh (setenv.bat) file.
 */
class ZipTomcatEnvVarsFile implements EnvVarsFile {

  private final Tomcat tomcatInstance
  private final File setenvFile
  private final Platform platform
  private final FileStateVault vault


  private ZipTomcatEnvVarsFile() {
  }

  ZipTomcatEnvVarsFile(Tomcat tomcatInstance) {
    this.platform = new Platform()
    this.tomcatInstance = tomcatInstance
    this.setenvFile = new File(tomcatInstance.getBinDir(), 'setenv.' + platform.getScriptSuffix())
  }

  ZipTomcatEnvVarsFile(Tomcat tomcatInstance, FileStateVault vault) {
    this(tomcatInstance)
    this.vault = vault
  }

  /**
   * Append new line to the end of setenv.sh or setenv.bat.
   * Set execute access right on Unix like OS.
   *
   * IMPORTANT: Do not add SET prefix to expression but variable assigment only.
   * SET will be added when executed on Windows platform automatically.
   */
  @Override
  void appendVariable(String name, String value) {
    vault?.push(setenvFile)

    createSetenvFileIfNotExists()

    setenvFile.append(new ZipTomcatEnvVariableAssigmentsGenerator().generateEnvLine(name, value) + platform.nl)
  }

  /**
   * Returns environment file being modified.
   */
  @Override
  File getEnvFile() {
    return setenvFile
  }

  private void createSetenvFileIfNotExists() {
    if (!setenvFile.exists()) JBFile.createFile(setenvFile)
    if (!platform.isWindows()) JBFile.chmod('+x', setenvFile)
  }

}
