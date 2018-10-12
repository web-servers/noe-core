package noe.ews.utils.selinux

public class SELinuxFileContextsTest {

  private List<SELinuxFileAssertion> list = new ArrayList<SELinuxFileAssertion>()

  public static final String SPECIAL_VALUE_ANY = '$ANY'

  public SELinuxFileContextsTest(File configFile) {
    configFile.eachLine { line ->
      // line format: zipPath | rpmPath | SELinuxContext
      // comment = begins with #
      // also ignore blank lines
      if (line.matches("[^#].+\\|.+")) {
        def (path, context) = line.tokenize("|").toList()
        context = context.trim()
        SELinuxFileAssertion assertion = new SELinuxFileAssertion(path, new SELinuxContext(context))
        try {
          SELinuxSystem.createFileIfDoesntExist(path)
          assertion.setActual(SELinuxSystem.getContextForFile(path))
        } catch (SELinuxException ex) {
          assertion.setErrorMessage(ex.getMessage())
        }
        list.add(assertion)
      }
    }
  }

  public List<SELinuxFileAssertion> getAllAssertions() {
    return list
  }

}
