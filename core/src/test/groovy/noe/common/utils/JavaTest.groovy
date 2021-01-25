package noe.common.utils

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

/**
 * Some basic tests for Java util class.
 *
 * @author Jan Stourac <jstourac@redhat.com>
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Java.class)
class JavaTest {

  /**
   * Sets 'javaVersion' static property of the Java class do desired value.
   *
   * @param desiredJavaVersion
   */
  private void setJavaVersion(String desiredJavaVersion) {
    PowerMockito.field(Java.class, "javaVersion").set(Java.class, desiredJavaVersion)
  }

  @Test
  void testIsJdk6() {
    List<String> testedStrings = new LinkedList<>()
    testedStrings.add("1.6.0")
    testedStrings.add("1.6.3")

    for (String testedString : testedStrings) {
      setJavaVersion(testedString)
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdk6())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk7())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk8())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk9())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk11())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk15())
    }
  }

  @Test
  void testIsJdk7() {
    List<String> testedStrings = new LinkedList<>()
    testedStrings.add("1.7.0")
    testedStrings.add("1.7.3")

    for (String testedString : testedStrings) {
      setJavaVersion(testedString)
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdk7())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk6())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk8())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk9())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk11())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk15())
    }
  }

  @Test
  void testIsJdk8() {
    List<String> testedStrings = new LinkedList<>()
    testedStrings.add("1.8.0")
    testedStrings.add("1.8.3")

    for (String testedString : testedStrings) {
      setJavaVersion(testedString)
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdk8())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk6())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk7())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk9())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk11())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk15())
    }
  }

  @Test
  void testIsJdk9() {
    List<String> testedStrings = new LinkedList<>()
    testedStrings.add("9-ea")
    testedStrings.add("9.0.0")
    testedStrings.add("9.6.3")

    for (String testedString : testedStrings) {
      setJavaVersion(testedString)
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdk9())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk6())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk7())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk8())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk11())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk15())
    }
  }

  @Test
  void testIsJdk11() {
    List<String> testedStrings = new LinkedList<>()
    testedStrings.add("11.0.2")
    testedStrings.add("11.1.5")
    testedStrings.add("11-test")

    for (String testedString : testedStrings) {
      setJavaVersion(testedString)
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdk11())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk9())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk6())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk7())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk8())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk15())
    }
  }

  @Test
  void testIsJdk15() {
    List<String> testedStrings = new LinkedList<>()
    testedStrings.add("15.0.1")
    testedStrings.add("15.1.5")
    testedStrings.add("15-test")

    for (String testedString : testedStrings) {
      setJavaVersion(testedString)
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdk15())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk6())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk7())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk8())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk9())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk11())
    }
  }

  @Test
  void testIsJdkXOrHigher() {
    List<String> testedStrings = new LinkedList<>()
    testedStrings.add("9-ea")
    testedStrings.add("9.0.0")
    testedStrings.add("9.6.3")

    for (String testedString : testedStrings) {
      setJavaVersion(testedString)
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdkXOrHigher("1.6"))
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdkXOrHigher("1.7"))
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdkXOrHigher("1.8"))
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdkXOrHigher("9"))
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdkXOrHigher("1.9"))
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdkXOrHigher("10"))
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdkXOrHigher("1.10"))
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdkXOrHigher("11"))
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdkXOrHigher("1.11"))
    }

    testedStrings = new LinkedList<>()
    testedStrings.add("1.7.0")
    testedStrings.add("1.7.3")

    for (String testedString : testedStrings) {
      setJavaVersion(testedString)
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdkXOrHigher("1.6"))
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdkXOrHigher("1.7"))
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdkXOrHigher("1.8"))
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdkXOrHigher("9"))
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdkXOrHigher("1.9"))
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdkXOrHigher("10"))
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdkXOrHigher("1.10"))
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdkXOrHigher("11"))
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdkXOrHigher("1.11"))
    }

    testedStrings = new LinkedList<>()
    testedStrings.add("11.0.2")
    testedStrings.add("11-test")

    for (String testedString : testedStrings) {
      setJavaVersion(testedString)
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdkXOrHigher("1.6"))
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdkXOrHigher("1.7"))
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdkXOrHigher("1.8"))
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdkXOrHigher("9"))
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdkXOrHigher("1.9"))
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdkXOrHigher("10"))
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdkXOrHigher("1.10"))
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdkXOrHigher("11"))
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdkXOrHigher("1.11"))
    }
  }
}
