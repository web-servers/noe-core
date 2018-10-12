package noe.ews.utils.selinux

public class ProcessSELinuxContext extends SELinuxContext {

  protected final String level2

  ProcessSELinuxContext(String user, String role, String type, String level, String level2) {
    super(user, role, type, level)
    this.level2 = level2
  }

  ProcessSELinuxContext(String stringRepresentation) {
    String string = stringRepresentation.trim()
    (user, role, type, level, level2) = string.tokenize(":")
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
    if (level2.equals(SELinuxFileContextsTest.SPECIAL_VALUE_ANY)) {
      level2 = null
    }
  }

  public static boolean evaluateConformity(ProcessSELinuxContext expected, ProcessSELinuxContext actual) {
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
    if (expected.level2 != null) {
      if (!expected.level2.equals(actual.level2))
        return false
    }
    return true
  }

  @Override
  public String toString() {
    return "$user:$role:$type:$level:$level2"
  }
}
