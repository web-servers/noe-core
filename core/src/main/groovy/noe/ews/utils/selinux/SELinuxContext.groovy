package noe.ews.utils.selinux

public class SELinuxContext {

  protected String user
  protected String role
  protected String type
  protected String level

  SELinuxContext() {
  }

  SELinuxContext(String user, String role, String type, String level) {
    this.user = user
    this.role = role
    this.type = type
    this.level = level
  }

  SELinuxContext(String stringRepresentation) {
    String string = stringRepresentation.trim()
    (user, role, type, level) = string.tokenize(":")
    if (user.equals(SELinuxFileContextsTest.SPECIAL_VALUE_ANY)) {
      user = null
    }
    if (role.equals(SELinuxFileContextsTest.SPECIAL_VALUE_ANY)) {
      role = null
    }
    if (type.equals(SELinuxFileContextsTest.SPECIAL_VALUE_ANY)) {
      type = null
    }
    if (level.equals(SELinuxFileContextsTest.SPECIAL_VALUE_ANY)) {
      level = null
    }
  }

  public static boolean evaluateConformity(SELinuxContext expected, SELinuxContext actual) {
    if (expected.user != null) {
      if (!expected.user.equals(actual.user))
        return false
    }
    if (expected.role != null) {
      if (!expected.role.equals(actual.role))
        return false
    }
    if (expected.type != null) {
      if (!expected.type.equals(actual.type))
        return false
    }
    if (expected.level != null) {
      if (!expected.level.equals(actual.level))
        return false
    }
    return true
  }

  @Override
  public String toString() {
    return "$user:$role:$type:$level"
  }
}
