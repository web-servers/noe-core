package noe.common.utils

import org.junit.Before
import org.junit.Rule;
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertFalse

class FileStateVaultTest {
  File testFile
  byte[] fileContentToTest
  final static int TEST_FILE_LENGTH = 200

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();


  @Before
  void prepareTestFile() {
    testFile = testFolder.newFile()
    fileContentToTest = new byte[TEST_FILE_LENGTH]
    new Random().nextBytes(fileContentToTest)
    testFile.setBytes(fileContentToTest)
  }

  @Test
  void pushPop1levelFileWasModified() {
    FileStateVault vault = new FileStateVault().push(testFile)

    testFile.setText("garbage")
    vault.pop(testFile)

    assertTrue(Arrays.equals(testFile.getBytes(), fileContentToTest))
  }

  @Test
  void pushPop1levelFileWasNotModified() {
    new FileStateVault()
      .push(testFile)
      .pop(testFile)

    assertTrue(Arrays.equals(testFile.getBytes(), fileContentToTest))
  }

  @Test
  void pushPopFileWasNotExisted() {
    File testFolder = testFolder.newFolder()
    File notExistingFile = new File(testFolder, 'i-do-not-exists-in-time-of-push')

    FileStateVault vault = new FileStateVault().push(notExistingFile)
    notExistingFile.setText("I am just created, I am expected to be deleted on pop.")
    vault.pop(notExistingFile)

    assertFalse(notExistingFile.exists())
  }

  @Test(expected = IllegalStateException.class)
  void pushDirectory() {
    File testFolder = testFolder.newFolder()
    new FileStateVault().push(testFolder)
  }

  @Test
  void pushPop2levelFileWasModified() {
    File testFile2 = testFolder.newFile()
    byte[] fileContentToTest2
    fileContentToTest2 = new byte[TEST_FILE_LENGTH]
    new Random().nextBytes(fileContentToTest2)
    testFile2.setBytes(fileContentToTest2)

    FileStateVault vault = new FileStateVault()
      .push(testFile)
      .push(testFile2)

    testFile.setText("garbage1")
    testFile2.setText("garbage2")

    vault.pop(testFile)
    vault.pop(testFile2)

    assertTrue(Arrays.equals(testFile.getBytes(), fileContentToTest))
    assertTrue(Arrays.equals(testFile2.getBytes(), fileContentToTest2))
  }

  @Test
  void pushesPopsPopAllSingle() {
    FileStateVault vault = new FileStateVault().push(testFile)

    testFile.setText("garbage1")
    vault.push(testFile)
    testFile.setText("garbage2")
    vault.push(testFile)
    testFile.setText("garbage3")
    vault.push(testFile)
    testFile.setText("garbage4")
    vault.push(testFile)
    testFile.setText("garbage5")

    vault.pop(testFile)
    vault.pop(testFile)
    assertTrue(Arrays.equals(testFile.getBytes(), 'garbage3'.getBytes()))

    vault.popAll(testFile)

    assertTrue(Arrays.equals(testFile.getBytes(), fileContentToTest))
  }

  @Test
  void pushesPopAllMulti() {
    File testFile2 = testFolder.newFile()
    byte[] fileContentToTest2
    fileContentToTest2 = new byte[TEST_FILE_LENGTH]
    new Random().nextBytes(fileContentToTest2)
    testFile2.setBytes(fileContentToTest2)


    FileStateVault vault = new FileStateVault().push(testFile)
    testFile.setText("garbage1")
    vault.push(testFile)

    vault.push(testFile2)
    testFile2.setText('other-garbage')
    vault.push(testFile2)


    assertTrue(Arrays.equals(testFile.getBytes(), 'garbage1'.getBytes()))
    assertTrue(Arrays.equals(testFile2.getBytes(), 'other-garbage'.getBytes()))

    vault.popAll()

    assertTrue(Arrays.equals(testFile.getBytes(), fileContentToTest))
    assertTrue(Arrays.equals(testFile2.getBytes(), fileContentToTest2))
  }


}
