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
  void testIsJdk16() {
    List<String> testedStrings = new LinkedList<>()
    testedStrings.add("1.6.0")
    testedStrings.add("1.6.3")

    for (String testedString : testedStrings) {
      setJavaVersion(testedString)
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdk16())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk17())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk18())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk19())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk11())
    }
  }

  @Test
  void testIsJdk17() {
    List<String> testedStrings = new LinkedList<>()
    testedStrings.add("1.7.0")
    testedStrings.add("1.7.3")

    for (String testedString : testedStrings) {
      setJavaVersion(testedString)
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdk17())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk16())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk18())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk19())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk11())
    }
  }

  @Test
  void testIsJdk18() {
    List<String> testedStrings = new LinkedList<>()
    testedStrings.add("1.8.0")
    testedStrings.add("1.8.3")

    for (String testedString : testedStrings) {
      setJavaVersion(testedString)
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdk18())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk16())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk17())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk19())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk11())
    }
  }

  @Test
  void testIsJdk19() {
    List<String> testedStrings = new LinkedList<>()
    testedStrings.add("9-ea")
    testedStrings.add("9.0.0")
    testedStrings.add("9.6.3")

    for (String testedString : testedStrings) {
      setJavaVersion(testedString)
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdk19())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk16())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk17())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk18())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk11())
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
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk19())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk16())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk17())
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk18())
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdk11())
    }
  }

  @Test
  void testIsJdk1xOrHigher() {
    List<String> testedStrings = new LinkedList<>()
    testedStrings.add("9-ea")
    testedStrings.add("9.0.0")
    testedStrings.add("9.6.3")

    for (String testedString : testedStrings) {
      setJavaVersion(testedString)
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdk1xOrHigher("1.6"))
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdk1xOrHigher("1.7"))
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdk1xOrHigher("1.8"))
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdk1xOrHigher("9"))
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdk1xOrHigher("1.9"))
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk1xOrHigher("10"))
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk1xOrHigher("1.10"))
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk1xOrHigher("11"))
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk1xOrHigher("1.11"))
    }

    testedStrings = new LinkedList<>()
    testedStrings.add("1.7.0")
    testedStrings.add("1.7.3")

    for (String testedString : testedStrings) {
      setJavaVersion(testedString)
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdk1xOrHigher("1.6"))
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdk1xOrHigher("1.7"))
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk1xOrHigher("1.8"))
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk1xOrHigher("9"))
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk1xOrHigher("1.9"))
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk1xOrHigher("10"))
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk1xOrHigher("1.10"))
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk1xOrHigher("11"))
      Assert.assertFalse("Java version string compared '" + testedString + "'", Java.isJdk1xOrHigher("1.11"))
    }

    testedStrings = new LinkedList<>()
    testedStrings.add("11.0.2")
    testedStrings.add("11-test")

    for (String testedString : testedStrings) {
      setJavaVersion(testedString)
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdk1xOrHigher("1.6"))
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdk1xOrHigher("1.7"))
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdk1xOrHigher("1.8"))
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdk1xOrHigher("9"))
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdk1xOrHigher("1.9"))
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdk1xOrHigher("10"))
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdk1xOrHigher("1.10"))
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdk1xOrHigher("11"))
      Assert.assertTrue("Java version string compared '" + testedString + "'", Java.isJdk1xOrHigher("1.11"))
    }
  }
}
