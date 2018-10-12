package noe.common.utils

import groovy.util.logging.Slf4j
import noe.common.Constants
import org.junit.Assert
import org.junit.Test

@Slf4j
class LibraryTest {

  // TODO: to properly tested you either need to figure out how to put test jar on classpath
  // TODO: potential solutions are dynamic class loading (not sure if it is possible in this case) or add test jar as dependency
  @Test
  void retrieveResourceFromJarTest() {
    File resourceFile = Library.retrieveResourceAsFile("LICENSE.txt") // this resource exists at least in JUnit
    try {
      Assert.assertNotNull("Resource should be found", resourceFile)
      Assert.assertTrue("The license string not found", resourceFile.text.contains("License"))
    } finally {
      resourceFile.delete()
    }
  }

  @Test
  void groovyAndAntAsClasspathTest() {
    File testLibDir = new File(new Platform().getTmpDir(), "groovyAndAntTestLibDir")
    if (testLibDir.exists()) {
      JBFile.delete(testLibDir)
    }
    def classpath = Library.groovyWithAntAsClasspath(testLibDir)
    File tmpFile = File.createTempFile("noe", "jbfile-external-groovy")
    try {
      def escapedFilePath = tmpFile.absolutePath.replaceAll(Constants.TWO_DOUBLE_BACKSLASHES, Constants.FOUR_DOUBLE_BACKSLASHES)
      tmpFile.text = "aaa"
      def command = [
          "java",
          "-cp",
          classpath,
          "groovy.ui.GroovyMain",
          "-e",
          "new groovy.util.AntBuilder().replaceregexp(file: '${escapedFilePath}', match: 'aaa', replace: 'bbb', encoding: 'UTF-8')"
      ]
      Assert.assertEquals("Command ${command} should be successful, but it failed",
              0, Cmd.executeCommand(command, new File(".")))
      Assert.assertEquals("bbb", tmpFile.text)
    } finally {
      tmpFile.delete()
    }
  }
}
