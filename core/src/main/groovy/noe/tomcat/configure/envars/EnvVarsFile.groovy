package noe.tomcat.configure.envars

/**
 * IMPORTANT: For usage within noe-core:tomcat.configure only
 *
 * Service for handling Tomcat files with environment variables.
 */
interface EnvVarsFile {

  /**
   * Appends new environment variable Tomcat specific file.
   * No special check in file are performed just straightforward appending.
   */
  void appendVariable(String name, String value)

  /**
   * Returns environment file being modified.
   */
  File getEnvFile()

}
