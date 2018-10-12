package noe.common.utils

/**
 * Class for basic management of OS users.
 */
class OsUser {
  Platform platform
  String username

  OsUser(String username) {
    this.platform = new Platform()
    this.username = username
  }

  /**
   * Create system user if not exists
   * Affects non Windows platforms
   * For successful user creation Administer privileges are required.
   */
  boolean create() {
    if (!platform.isWindows()) {
      if (JBFile.useAdminPrivileges) {
        if ((Cmd.executeSudoCommandConsumeStreams(['id', username]).exitValue) != 0) {
          return Cmd.executeSudoCommandConsumeStreams(['useradd', username]).exitValue == 0
        }
      }
    }
    return false
  }

  /**
   * Removes system user if exists.
   * Affects non Windows platforms.
   * For successful user removal Administer privileges are required.
   * On Windows false is returned always.
   */
  boolean remove() {
    if (!platform.isWindows()) {
      if (JBFile.useAdminPrivileges) {
        if ((Cmd.executeSudoCommandConsumeStreams(['id', username]).exitValue) == 0) {
          return Cmd.executeSudoCommandConsumeStreams(['userdel', username]).exitValue == 0
        }
      }
    }

    return false
  }

}

