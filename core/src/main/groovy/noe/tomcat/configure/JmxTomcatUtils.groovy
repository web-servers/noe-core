package noe.tomcat.configure

import noe.common.utils.Cmd
import noe.common.utils.JBFile
import noe.common.utils.Platform
import noe.server.Tomcat;

/**
 * Various Tomcat JMX methods.
 */
class JmxTomcatUtils {

  /**
   * Tomcat documentation:
   *   The password file should be read-only and only
   *   accessible by the operating system user Tomcat is running as.
   */
  static void prepareAccessRights(Tomcat tomcat, File path) {
    Platform platform = new Platform()

    if (platform.isWindows()) {
      if (JBFile.chown(platform.actualUser, path) > 0) {
        throw new RuntimeException("Windows chown ${platform.actualUser} $path FAILED")
      }

      Cmd.executeCommand(["icacls", path, "/inheritance:r"], new File('.'))

      // set read permissions
      if (JBFile.chmod("R", path, platform.actualUser) > 0) {
        throw new RuntimeException("Windows chmod ${platform.actualUser}:R $path FAILED")
      }
    } else {
      if (JBFile.chmod("0400", path) > 0) throw new RuntimeException("chmod 700 $path FAILED")

      // Change group if tomcat run under another user than testsuite runner
      String runAs = tomcat.loadRunAs()
      if (runAs) {
        if (JBFile.chgrp(runAs, path) > 0) {
          throw new RuntimeException("chgrp $runAs $path FAILED")
        }
        if (JBFile.chown(runAs, path) > 0) {
          throw new RuntimeException("chown $runAs $path FAILED")
        }
      }
    }
  }

}
