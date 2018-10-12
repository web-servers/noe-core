package noe.ews.utils.selinux

public class SELinuxFileAssertion {

  private String filePath;
  private SELinuxContext expected;
  private SELinuxContext actual;
  private String errorMessage;


  public SELinuxFileAssertion(String filePath, SELinuxContext expected) {
    this.filePath = filePath;
    this.expected = expected;
  }

  public String getFilePath() {
    return filePath
  }

  void setActual(SELinuxContext actual) {
    this.actual = actual
  }

  void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage
  }

/**
 *
 * @return null if OK, string explanation if test failed
 */
  public String evaluateConformity() {
    if (errorMessage != null) {
      return errorMessage;
    }
    if (!SELinuxContext.evaluateConformity(expected, actual)) {
      return "expected='$expected', actual='$actual'"
    }
    return null;
  }


  @Override
  public String toString() {
    return "SELinuxFileAssertion{" +
        "file='" + filePath + '\'' +
        ", expected='" + expected +
        "', actual='" + actual +
        '\'}';
  }
}
