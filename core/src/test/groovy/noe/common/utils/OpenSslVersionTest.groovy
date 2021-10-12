package noe.common.utils

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

/**
 * @author Jan Stourac   <jstourac@redhat.com>
 */
class OpenSslVersionTest {

  @Test
  public void testEqualsObject() {
    List<String> values = new LinkedList<>();
    values.add("0.9.8")
    values.add("1.0.0")
    values.add("1.1.0")
    values.add("1.1.1")
    values.add("0.9.8b")
    values.add("1.0.0b")
    values.add("1.1.0b")
    values.add("1.2.3b")

    for (int i = 0; i < values.size(); i++) {
      String testedValue = values.get(i);
      assertTrue 'new OpenSslVersion("' + testedValue + '").equals("' + testedValue + '")', new OpenSslVersion(testedValue).equals(testedValue)
    }
  }


  @Test
  public void testCompareTo() {
    // <=> calls compareTo(...) explicitly
    List<Tuple> values = new LinkedList<>()
    values.add(new Tuple("0.9.8", "0.9.9"))
    values.add(new Tuple("0.9.8b", "0.9.8c"))
    values.add(new Tuple("1.0.2c", "1.2.3a"))
    values.add(new Tuple("0.9.8", "1.2.9c"))
    values.add(new Tuple("0.9.8", "1.9.9"))
    values.add(new Tuple("0.9.8a", "0.9.8b"))
    values.add(new Tuple("1.9.8b", "1.9.8g"))
    values.add(new Tuple("1.0.2h", "1.0.2h-SP2-CR1"))

    for (int i = 0; i < values.size(); i++) {
      String testedValue1 = values.get(i).get(0)
      String testedValue2 = values.get(i).get(1)
      assertEquals 'new OpenSslVersion("' + testedValue1 + '") <=> new OpenSslVersion("' + testedValue2 + '")', -1, new OpenSslVersion(testedValue1) <=> new OpenSslVersion(testedValue2)
      assertEquals 'new OpenSslVersion("' + testedValue2 + '") <=> new OpenSslVersion("' + testedValue1 + '")', 1, new OpenSslVersion(testedValue2) <=> new OpenSslVersion(testedValue1)
    }

    values = new LinkedList<>();
    values.add("0.9.8")
    values.add("1.0.0")
    values.add("1.1.0")
    values.add("1.1.1")
    values.add("0.9.8b")
    values.add("1.0.0b")
    values.add("1.1.0b")
    values.add("1.2.3b")
    values.add("1.0.2h-SP2-CR1")

    for (int i = 0; i < values.size(); i++) {
      String testedValue = values.get(i);
      assertEquals 'new OpenSslVersion("' + testedValue + '") <=> new OpenSslVersion("' + testedValue + '")', 0, new OpenSslVersion(testedValue) <=> new OpenSslVersion(testedValue)
    }
  }


  @Test
  public void testVersionFields() {
    String input = "1.2.3c"
    OpenSslVersion ver = new OpenSslVersion(input)
    int major = ver.getMajorVersion()
    int minor = ver.getMinorVersion()
    int incremental = ver.getIncrementalVersion()
    String build = ver.getIncrementalCharacter()

    assertEquals "Major version of $input is not $major", 1, major
    assertEquals "Minor version of $input is not $minor", 2, minor
    assertEquals "Incremental version of $input is not $incremental", 3, incremental
    assertEquals "Build version of $input is not $build", "c", build
  }

  @Test
  void testBaseVersionString() {
    assertEquals 'new OpenSslVersion("0.9.1h").baseVersionString()', '0.9.1h', new OpenSslVersion("0.9.1h").baseVersionString()
    assertEquals 'new OpenSslVersion("1.1.0").baseVersionString()', '1.1.0', new OpenSslVersion("1.1.0").baseVersionString()
    assertEquals 'new OpenSslVersion("1.0").baseVersionString()', '1.0.0', new OpenSslVersion("1.0").baseVersionString()
    assertEquals 'new OpenSslVersion("1.1.0b").baseVersionString()', '1.1.0b', new OpenSslVersion("1.1.0b").baseVersionString()
    assertEquals 'new OpenSslVersion("1").baseVersionString()', '1.0.0', new OpenSslVersion("1").baseVersionString()
    assertEquals 'new OpenSslVersion("1.0.2h-SP2-CR1").baseVersionString()', '1.0.2h', new OpenSslVersion("1.0.2h-SP2-CR1").baseVersionString()
  }
}
