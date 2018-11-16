package noe.tomcat.configure

class TomcatUser {
  private String username
  private String password
  private List<String> roles
  private String parsedRoles = ""

  /**
   * Set roles on the TomcatUser object. When set, parsedRoles is set to
   * a tomcat-readable format used in tomcat-users.xml.
   * @param roles
   */
  void setRoles(List<String> roles) {
    this.roles = roles

    // All roles except for the last one
    roles.init().each { String role ->
      parsedRoles += "${role},"
    }
    parsedRoles += roles.last()
  }
}