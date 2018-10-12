package noe.common.utils

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

/**
 *
 * Notice:
 *  - 6.0.0 == 6.0.0.GA
 *
 * @author Jan Stefl   <jstefl@redhat.com>
 *
 */
class VersionTest {

  @Test
  public void testEqualsObject() {
    assertTrue 'new Version("6.0.0").equals("6.0.0")', new Version("6.0.0").equals("6.0.0")
    assertTrue 'new Version("6.0.0").equals(\'6.0.0\')', new Version("6.0.0").equals('6.0.0')
    assertTrue 'new Version("6.0.0").equals("6.0.0.GA")', new Version("6.0.0").equals("6.0.0.GA")
    assertTrue 'new Version("6.0.0.GA").equals("6.0.0")', new Version("6.0.0.GA").equals("6.0.0")
    assertTrue 'new Version("6.0.0.DR1").equals("6.0.0.DR1")', new Version("6.0.0.DR1").equals("6.0.0.DR1")
    assertTrue 'new Version("6.0.0.ER4.1").equals("6.0.0.ER4.1")', new Version("6.0.0.ER4.1").equals("6.0.0.ER4.1")
    assertTrue 'new Version("6.2.1.CP.CR1").equals("6.2.1.CP.CR1")', new Version("6.2.1.CP.CR1").equals("6.2.1.CP.CR1")
    assertTrue 'new Version("6.2.1-CP-CR1").equals("6.2.1-CP-CR1")', new Version("6.2.1-CP-CR1").equals("6.2.1-CP-CR1")

    assertTrue 'new Version("6.0.0").equals(new Version("6.0.0"))', new Version("6.0.0").equals(new Version("6.0.0"))
    assertTrue 'new Version("6.0.0").equals(new Version(\'6.0.0\'))', new Version("6.0.0").equals(new Version('6.0.0'))
    assertTrue 'new Version("6.0.0").equals(new Version("6.0.0.GA"))', new Version("6.0.0").equals(new Version("6.0.0.GA"))
    assertTrue 'new Version("6.0.0.GA").equals(new Version("6.0.0"))', new Version("6.0.0.GA").equals(new Version("6.0.0"))
    assertTrue 'new Version("6.0.0.DR1").equals(new Version("6.0.0.DR1"))', new Version("6.0.0.DR1").equals(new Version("6.0.0.DR1"))
    assertTrue 'new Version("6.0.0.ER4.1").equals(new Version("6.0.0.ER4.1"))', new Version("6.0.0.ER4.1").equals(new Version("6.0.0.ER4.1"))
    assertTrue 'new Version("6.2.1.CP.CR1").equals(new Version("6.2.1.CP.CR1"))', new Version("6.2.1.CP.CR1").equals(new Version("6.2.1.CP.CR1"))
    assertTrue 'new Version("6.2.1.CP.CR1.10").equals(new Version("6.2.1.CP.CR1.10"))', new Version("6.2.1.CP.CR1.10").equals(new Version("6.2.1.CP.CR1.10"))
    assertTrue 'new Version("6.0.0-ER4.1").equals(new Version("6.0.0-ER4.1"))', new Version("6.0.0-ER4.1").equals(new Version("6.0.0-ER4.1"))
    assertTrue 'new Version("6.2.1-CP-CR1").equals(new Version("6.2.1-CP-CR1"))', new Version("6.2.1-CP-CR1").equals(new Version("6.2.1-CP-CR1"))

    assertFalse 'new Version("5.0.0").equals(new Version("6.0.0"))', new Version("5.0.0").equals(new Version("6.0.0"))
    assertFalse 'new Version("6.0.0").equals(new Version("6.0.0.DR1"))', new Version("6.0.0").equals(new Version("6.0.0.DR1"))
    assertFalse 'new Version("6.0.0.DR1").equals(new Version("6.0.0.ER1"))', new Version("6.0.0.DR1").equals(new Version("6.0.0.ER1"))
    assertFalse 'new Version("6.1.0.DR1").equals(new Version("5.1.0.DR1"))', new Version("6.1.0.DR1").equals(new Version("5.1.0.DR1"))
    assertFalse 'new Version("6.1.1.DR1").equals(new Version("6.1.0.DR1"))', new Version("6.1.1.DR1").equals(new Version("6.1.0.DR1"))
    assertFalse 'new Version("6.0.0.CR1.1").equals(new Version("6.0.0.ER1.1"))', new Version("6.0.0.CR1.1").equals(new Version("6.0.0.ER1.1"))
    assertFalse 'new Version("6.0.0.DR1").equals(new Version("6.0.0.DR1.1"))', new Version("6.0.0.DR1").equals(new Version("6.0.0.DR1.1"))
    assertFalse 'new Version("6.2.1.CP.CR1").equals(new Version("6.2.1.CP.CR1"))', new Version("6.2.1.CP.CR1").equals(new Version("6.2.1.CR1"))
    assertFalse 'new Version("6.2.1.CP.CR1").equals(new Version("6.2.1.CP.CR1.1"))', new Version("6.2.1.CP.CR1").equals(new Version("6.2.1.CR1.1"))
    assertFalse 'new Version("6.2.1.CP.CR1").equals(new Version("6.2.1.CP.CR2"))', new Version("6.2.1.CP.CR1").equals(new Version("6.2.1.CP.CR2"))
    assertFalse 'new Version("6.2.1.CP.CR2").equals(new Version("6.2.1.CP.CR1"))', new Version("6.2.1.CP.CR2").equals(new Version("6.2.1.CP.CR1"))
    assertFalse 'new Version("6.2.1.CP.CR2").equals(new Version("6.2.1"))', new Version("6.2.1.CP.CR2").equals(new Version("6.2.1"))
    assertFalse 'new Version("6.2.1.CP.CR2").equals(new Version("6.2.1.GA"))', new Version("6.2.1.CP.CR2").equals(new Version("6.2.1.GA"))
    assertFalse 'new Version("6.2.1-CP-CR1").equals(new Version("6.2.1-CP-CR1.1"))', new Version("6.2.1-CP-CR1").equals(new Version("6.2.1-CR1.1"))
    assertFalse 'new Version("6.2.1-CP-CR1").equals(new Version("6.2.1-CP-CR2"))', new Version("6.2.1-CP-CR1").equals(new Version("6.2.1-CP-CR2"))
    assertFalse 'new Version("6.2.1-CP-CR2").equals(new Version("6.2.1"))', new Version("6.2.1-CP-CR2").equals(new Version("6.2.1"))
  }


  @Test
  public void testCompareTo() {
    // <=> calls compareTo(...) explicitly
    assertEquals 'new Version("5.0.0") <=> new Version("6.0.0")', -1, new Version("5.0.0") <=> new Version("6.0.0")
    assertEquals 'new Version("6.0.0") <=> new Version("6.1.0")', -1, new Version("6.0.0") <=> new Version("6.1.0")
    assertEquals 'new Version("6.0.0") <=> new Version("6.0.1")', -1, new Version("6.0.0") <=> new Version("6.0.1")
    assertEquals 'new Version("6.0.0") <=> new Version("6.1.1.ER2")', -1, new Version("6.0.0") <=> new Version("6.1.1.ER2")
    assertEquals 'new Version("6.1.1.ER1") <=> new Version("6.1.1")', -1, new Version("6.1.1.ER1") <=> new Version("6.1.1")
    assertEquals 'new Version("6.1.1.DR1") <=> new Version("6.1.1.DR2")', -1, new Version("6.1.1.DR1") <=> new Version("6.1.1.DR2")
    assertEquals 'new Version("6.1.1.DR1") <=> new Version("6.1.1.ER1")', -1, new Version("6.1.1.DR1") <=> new Version("6.1.1.ER1")
    assertEquals 'new Version("6.1.1.ER1") <=> new Version("6.1.1.CR1")', -1, new Version("6.1.1.ER1") <=> new Version("6.1.1.CR1")
    assertEquals 'new Version("6.1.1.ER1") <=> new Version("6.1.1.CR1")', -1, new Version("6.1.1.CR1") <=> new Version("6.1.1.GA")
    assertEquals 'new Version("6.1.1.DR1") <=> new Version("6.1.1.ER2")', -1, new Version("6.1.1.DR1") <=> new Version("6.1.1.ER2")
    assertEquals 'new Version("6.0.0") <=> new Version("6.1.1.DR1.1")', -1, new Version("6.0.0") <=> new Version("6.1.1.DR1.1")
    assertEquals 'new Version("6.1.1.DR1.1") <=> new Version("6.1.1")', -1, new Version("6.1.1.DR1.1") <=> new Version("6.1.1")
    assertEquals 'new Version("6.1.1.DR1.1") <=> new Version("6.1.1.DR1.2")', -1, new Version("6.1.1.DR1.1") <=> new Version("6.1.1.DR1.2")
    assertEquals 'new Version("6.1.1.CR1.15") <=> new Version("6.1.1.CR1.28")', -1, new Version("6.1.1.CR1.15") <=> new Version("6.1.1.CR1.28")
    assertEquals 'new Version("6.1.1-DR1.1") <=> new Version("6.1.1-DR1.2")', -1, new Version("6.1.1-DR1.1") <=> new Version("6.1.1-DR1.2")
    assertEquals 'new Version("6.1.1-CR1.15") <=> new Version("6.1.1-CR1.28")', -1, new Version("6.1.1-CR1.15") <=> new Version("6.1.1-CR1-28")
    assertEquals 'new Version("3.0.2-DR2") <=> new Version("3.0.2-20160305")', -1,  new Version('3.0.2-DR2') <=> new Version('3.0.2-20160305')
    assertEquals 'new Version("3.0.2") <=> new Version("3.0.2-20171205")', -1,  new Version('3.0.2') <=> new Version('3.0.2-20171205')
    assertEquals 'new Version("3.0.2-GA") <=> new Version("3.0.2-20171205")', -1,  new Version('3.0.2-GA') <=> new Version('3.0.2-20171205')
    assertEquals 'new Version("3.0.2-20160304") <=> new Version("3.0.2-20160305")', -1,  new Version('3.0.2-20160304') <=> new Version('3.0.2-20160305')



    assertEquals 'new Version("6.0.0") <=> new Version("6.0.0")', 0, new Version("6.0.0") <=> new Version("6.0.0")
    assertEquals 'new Version("6.1.0") <=> new Version("6.1.0")', 0, new Version("6.1.0") <=> new Version("6.1.0")
    assertEquals 'new Version("6.1.1") <=> new Version("6.1.1")', 0, new Version("6.1.1") <=> new Version("6.1.1")
    assertEquals 'new Version("6.1.1.GA") <=> new Version("6.1.1")', 0, new Version("6.1.1.GA") <=> new Version("6.1.1")
    assertEquals 'new Version("6.1.1") <=> new Version("6.1.1.GA")', 0, new Version("6.1.1") <=> new Version("6.1.1.GA")
    assertEquals 'new Version("6.1.1.ER1") <=> new Version("6.1.1.ER1")', 0, new Version("6.1.1.ER1") <=> new Version("6.1.1.ER1")
    assertEquals 'new Version("6.1.1.DR1.1") <=> new Version("6.1.1.DR1.1")', 0, new Version("6.1.1.DR1.1") <=> new Version("6.1.1.DR1.1")
    assertEquals 'new Version("6.1.1.ER1.1") <=> new Version("6.1.1.ER1.1")', 0, new Version("6.1.1.ER1.1") <=> new Version("6.1.1.ER1.1")
    assertEquals 'new Version("6.1.1.CR1.1") <=> new Version("6.1.1.CR1.1")', 0, new Version("6.1.1.CR1.1") <=> new Version("6.1.1.CR1.1")
    assertEquals 'new Version("6.1.1.CR1.15") <=> new Version("6.1.1.CR1.15")', 0, new Version("6.1.1.CR1.15") <=> new Version("6.1.1.CR1.15")
    assertEquals 'new Version("3.0.2-20160304") < new Version("3.0.2-20160304")', 0,  new Version('3.0.2-20160304') <=> new Version('3.0.2-20160304')

    assertEquals 'new Version("6.0.0") <=> new Version("5.0.0")', 1, new Version("6.0.0") <=> new Version("5.0.0")
    assertEquals 'new Version("6.1.0") <=> new Version("6.0.0")', 1, new Version("6.1.0") <=> new Version("6.0.0")
    assertEquals 'new Version("6.0.1") <=> new Version("6.0.0")', 1, new Version("6.0.1") <=> new Version("6.0.0")
    assertEquals 'new Version("6.1.1.ER2") <=> new Version("6.0.0")', 1, new Version("6.1.1.ER2") <=> new Version("6.0.0")
    assertEquals 'new Version("6.1.1") <=> new Version("6.1.1.ER1")', 1, new Version("6.1.1") <=> new Version("6.1.1.ER1")
    assertEquals 'new Version("6.1.1.DR2") <=> new Version("6.1.1.DR1")', 1, new Version("6.1.1.DR2") <=> new Version("6.1.1.DR1")
    assertEquals 'new Version("6.1.1.ER1") <=> new Version("6.1.1.DR1")', 1, new Version("6.1.1.ER1") <=> new Version("6.1.1.DR1")
    assertEquals 'new Version("6.1.1.CR1") <=> new Version("6.1.1.ER1")', 1, new Version("6.1.1.CR1") <=> new Version("6.1.1.ER1")
    assertEquals 'new Version("6.1.1.GA") <=> new Version("6.1.1.CR1")', 1, new Version("6.1.1.GA") <=> new Version("6.1.1.CR1")
    assertEquals 'new Version("6.1.1.ER2") <=> new Version("6.1.1.DR1")', 1, new Version("6.1.1.ER2") <=> new Version("6.1.1.DR1")
    assertEquals 'new Version("6.1.1.DR1.1") <=> new Version("6.0.0")', 1, new Version("6.1.1.DR1.1") <=> new Version("6.0.0")
    assertEquals 'new Version("6.1.1") <=> new Version("6.1.1.DR1.1")', 1, new Version("6.1.1") <=> new Version("6.1.1.DR1.1")
    assertEquals 'new Version("6.1.1.DR1.2") <=> new Version("6.1.1.DR1.1")', 1, new Version("6.1.1.DR1.2") <=> new Version("6.1.1.DR1.1")
    assertEquals 'new Version("6.1.1.CR1.28") <=> new Version("6.1.1.CR1.15")', 1, new Version("6.1.1.CR1.28") <=> new Version("6.1.1.CR1.15")
    assertEquals 'new Version("3.0.3") <=> new Version("3.0.2-20160305")', 1,  new Version('3.0.3') <=> new Version('3.0.2-20160305')
    assertEquals 'new Version("3.0.3-ER1") <=> new Version("3.0.2-20171205")', 1,  new Version('3.0.3-ER1') <=> new Version('3.0.2-20171205')
    assertEquals 'new Version("3.0.3-20160304") <=> new Version("3.0.2-20160304")', 1,  new Version('3.0.3-20160304') <=> new Version('3.0.2-20160305')
    assertEquals 'new Version("3.0.3-20160304") <=> new Version("3.0.3")', 1,  new Version('3.0.3-20160304') <=> new Version('3.0.3')
    assertEquals 'new Version("3.0.2-20160304") <=> new Version("3.0.2-201634")', 1,  new Version('3.0.2-20160304') <=> new Version('3.0.2-201634')


    assertTrue 'new Version("6.0.0") > new Version("6.0.0.CR1")', new Version("6.0.0") > new Version("6.0.0.CR1")
    assertTrue 'new Version("5.2.1.DR5") < new Version("6.0.0.CR1")', new Version("5.2.1.DR5") < new Version("6.0.0.CR1")
    assertTrue 'new Version("6.0.0") == new Version("6.0.0")', new Version("6.0.0") == new Version("6.0.0")
    assertTrue 'new Version("6.0.0") == new Version("6.0.0.GA")', new Version("6.0.0") == new Version("6.0.0.GA")
    assertTrue 'new Version("6.0.0.GA") == new Version("6.0.0")', new Version("6.0.0.GA") == new Version("6.0.0")
    assertTrue 'new Version("5.1.1.CR5") < new Version("6.0.0")', new Version("5.1.1.CR5") < new Version("6.0.0")
    assertTrue 'new Version("6.0.0") > new Version("6.0.0.ER1")', new Version("6.0.0") > new Version('6.0.0.ER1')
    assertTrue 'new Version("6.0.0") > new Version("6.0.0.DR3")', new Version("6.0.0") > new Version('6.0.0.DR3')
    assertTrue 'new Version("6.0.0.DR3") < new Version("6.0.0.ER1")', new Version("6.0.0.DR3") < new Version('6.0.0.ER1')
    assertTrue 'new Version("6.0.0.DR3") < new Version("6.0.0.CR1")', new Version("6.0.0.DR3") < new Version('6.0.0.CR1')
    assertTrue 'new Version("6.0.0-DR3") < new Version("6.0.0-CR1")', new Version("6.0.0-DR3") < new Version('6.0.0-CR1')
    assertTrue 'new Version("6.0.0-CR2") < new Version("6.0.0")', new Version("6.0.0-CR2") < new Version("6.0.0")
    assertTrue 'new Version("6.0.0") < new Version("7.0.0.DR11.fkjdsh")', new Version("6.0.0") < new Version("7.0.0.DR11.fkjdsh")
    assertTrue 'new Version("7.0.0.DR1") < new Version("7.0.0.DR11.fkjdsh")', new Version("7.0.0.DR1") < new Version("7.0.0.DR11.fkjdsh")
    assertTrue 'new Version("7.0.0.DR11") != new Version("7.0.0.DR11.fkjdsh")', new Version("7.0.0.DR11") != new Version("7.0.0.DR11.fkjdsh")
    assertTrue 'new Version("7.0.0.DR11") < new Version("7.0.0.DR11.fkjdsh")', new Version("7.0.0.DR11") < new Version("7.0.0.DR11.fkjdsh")
    assertTrue 'new Version("7.0.0.DR11.dsdsd") > new Version("6.0.0")', new Version("7.0.0.DR11.dsdsd") > new Version("6.0.0")
    assertTrue 'new Version("2.4") >= new Version("2.4")', new Version("2.4") >= new Version("2.4")
    assertTrue 'new Version("2.4") >= new Version("2.2")', new Version("2.4") >= new Version("2.2")
    assertFalse 'new Version("2.4") <= new Version("2.2")', new Version("2.4") <= new Version("2.2")


    assertFalse 'new Version("6.0.0") < new Version("6.0.0.CR1")', new Version("6.0.0") < new Version("6.0.0.CR1")
    assertFalse 'new Version("5.2.1.DR5") > new Version("6.0.0.CR1")', new Version("5.2.1.DR5") > new Version("6.0.0.CR1")
    assertFalse 'new Version("6.0.0") != new Version("6.0.0")', new Version("6.0.0") != new Version("6.0.0")
    assertFalse 'new Version("6.0.0") != new Version("6.0.0.GA")', new Version("6.0.0") != new Version("6.0.0.GA")
    assertFalse 'new Version("6.0.0.GA") != new Version("6.0.0")', new Version("6.0.0.GA") != new Version("6.0.0")
    assertFalse 'new Version("5.1.1.CR5") > new Version("6.0.0")', new Version("5.1.1.CR5") > new Version("6.0.0")
    assertFalse 'new Version("6.0.0") < new Version("6.0.0.ER1")', new Version("6.0.0") < new Version('6.0.0.ER1')
    assertFalse 'new Version("6.0.0") < new Version("6.0.0.DR3")', new Version("6.0.0") < new Version('6.0.0.DR3')
    assertFalse 'new Version("6.0.0.DR3") > new Version("6.0.0.ER1")', new Version("6.0.0.DR3") > new Version('6.0.0.ER1')
    assertFalse 'new Version("6.0.0.DR3") > new Version("6.0.0.CR1")', new Version("6.0.0.DR3") > new Version('6.0.0.CR1')
    assertFalse 'new Version("6.0.0-DR3") > new Version("6.0.0-CR1")', new Version("6.0.0-DR3") > new Version('6.0.0-CR1')

    assertTrue 'new Version("2.4.6-DR3") > new Version("2.4")', new Version("2.4.6-DR3") > new Version("2.4")

    assertTrue "['6.0.0', '6.0.0.ER1', '6.0.0.DR3', '5.1.1.CR5', ].sort() == ['5.1.1.CR5', '6.0.0.DR3', '6.0.0.ER1', '6.0.0', ]",
            [new Version('6.0.0'), new Version('6.0.0.ER1'), new Version('6.0.0.DR3'), new Version('5.1.1.CR5')].sort() ==
                    [new Version('5.1.1.CR5'), new Version('6.0.0.DR3'), new Version('6.0.0.ER1'), new Version('6.0.0')]

    String v = '6.0.0'
    def r = [new Version("${v}.DR1"), new Version("${v}.DR2"), new Version("${v}.ER1"),
             new Version("${v}.ER2"), new Version("${v}.CR1"), new Version("${v}.CR2"),
             new Version("${v}.GA"), new Version("${v}-19700101"), new Version("${v}-SP1-DR1"),
             new Version("${v}-SP1-DR2"), new Version("${v}-SP1-text"), new Version("${v}-SP1-111"),
             new Version("${v}-SP1-112"), new Version("${v}-SP2"), new Version("${v}-SP2-text"),
    ]

    def td = r.clone()
    Collections.shuffle(td, new Random(42))
    assert td.sort() == r

    assert new Version("6.0.0-2") > new Version('6.0.0.ER1')
    assert new Version("6.0.0-2") > new Version('6.0.0-text')
    assert new Version("6.0.0-20160505") > new Version('6.0.0.1')
    assert new Version("6.0.0") > new Version('6.0.0.DR3')
    assert new Version("6.0.0.DR3") < new Version('6.0.0.ER1')
    assert new Version("6.0.0.DR3") < new Version('6.0.0.CR1')
    assert new Version("6.0.0-DR3") < new Version('6.0.0-CR1')

    assert new Version("2.4.6-SP1") > new Version('2.4.6')
    assert new Version("2.4.6") < new Version('2.4.6-SP1')

    assert new Version("6.0.0-SP1") > new Version('6.0.0')
    assert new Version("6.0.0-SP1") > new Version('6.0.0-GA')
    assert new Version("6.0.0-SP1") > new Version('6.0.0-myrelease')
    assert new Version("6.0.0-SP1.DR1") < new Version('6.0.0-SP1')
    assert new Version("6.0.0-SP1.DR2") > new Version('6.0.0-SP1.DR1')
    assert new Version("6.0.0-SP1.DR2") > new Version('6.0.0-SP1.DR1-20160503')

    assert new Version("6.0.0-SP1") < new Version('6.0.0-SP2')
    assert new Version("6.0.0-SP1.DR1") < new Version('6.0.0-SP1.DR1-20160503')
    assert new Version("6.0.0-SP1.DR1") < new Version('6.0.0-SP1.20160503')
    assert new Version("6.0.0-SP1.DR1-20160528") < new Version('6.0.0-SP1.DR1.20160529')
    assert new Version("6.0.0") > new Version('6.0.0-DR1')
    assert new Version("6.0.0-DR1-fix") > new Version('6.0.0-DR1')
    assert new Version("6.0.0-SP1.DR1") < new Version('6.0.0-SP1')
    assert new Version("6.0.0-SP1.DR1-fix") < new Version('6.0.0-SP1')
    assert new Version("6.0.0-SP1.fix") > new Version('6.0.0-SP1')
    assert new Version("6.0.0-SP2.CR2") < new Version('6.0.0-SP2-CR2-fix')
    assert new Version("6.0.0-SP2.CR2-20170512") > new Version('6.0.0-SP2-CR2-fix')
    assert new Version("6.0.0-SP1.GA") == new Version('6.0.0-SP1')

    assert new Version('1')  == new Version('1.0.0')
    assert new Version('1.0') == new Version('1.0.0')
    assert new Version("1.0") == new Version('1.0.0-GA')
    assert new Version("1") == new Version('1.0.0-GA')
    assert new Version('1')  > new Version('0.0.9')
    assert new Version('1.0') < new Version('1.0.1')
    assert new Version("1.0") > new Version('1.0.0-DR1')
    assert new Version("1") < new Version('1.0.0-MySpecialRelease')

    // Case insensitive comparision (for special releases only)
    assert new Version("6.0.0.DR3") < new Version('6.0.0.er1')
    assert new Version("6.0.0.dr1") == new Version('6.0.0.dr1')
    assert new Version("6.0.0.er2") == new Version('6.0.0.ER2')
    assert new Version("6.0.0.cr3") == new Version('6.0.0.CR3')
    assert new Version("6.0.0.SP1") == new Version('6.0.0.sp1')
    assert new Version("6.0.0.GA") == new Version('6.0.0.ga')
    assert new Version("6.0.0.dr1") == new Version('6.0.0.dR1')
    assert new Version("6.0.0.er2") == new Version('6.0.0.eR2')
    assert new Version("6.0.0.cR3") == new Version('6.0.0.CR3')
    assert new Version("6.0.0.sP1") == new Version('6.0.0.sp1')
    assert new Version("6.0.0.Ga") == new Version('6.0.0.gA')

    assert new Version("6.0.0.DR3") > new Version('6.0.0.DR2')
    // Badly formatted special release => Handle as text
    assert new Version("6.0.0.DR3") < new Version('6.0.0.DR2X')

    assert new Version('1.0.0.19700101') != new Version('1.0.0.197011')
    assert new Version('1.0.0.19700101') > new Version('1.0.0.19701')
  }


  @Test
  public void testVersionFields() {
    String input = "6.1.2.DR3.2"
    Version ver = new Version(input)
    int major = ver.getMajorVersion()
    int minor = ver.getMinorVersion()
    int incremental = ver.getIncrementalVersion()
    String build = ver.getBuildNumber()

    assertEquals "Major version of $input is not $major", major, 6
    assertEquals "Minor version of $input is not $minor", minor, 1
    assertEquals "Incremental version of $input is not $incremental", incremental, 2
    assertEquals "Build version of $input is not $build", build, "DR3"
  }

  @Test
  void testBaseVersionString() {
    assertEquals 'new Version("6.0.0").baseVersionString()', '6.0.0', new Version("6.0.0").baseVersionString()
    assertEquals 'new Version("6.0.0.CR1-xxx").baseVersionString()', '6.0.0', new Version("6.0.0.CR1-xxx").baseVersionString()
    assertEquals 'new Version("6.0.0.GA").baseVersionString()', '6.0.0', new Version("6.0.0.GA").baseVersionString()
    assertEquals 'new Version("6.0").baseVersionString()', '6.0.0', new Version("6.0").baseVersionString()
    assertEquals 'new Version("6.0.1").baseVersionString()', '6.0.1', new Version("6.0.1").baseVersionString()
  }
}
