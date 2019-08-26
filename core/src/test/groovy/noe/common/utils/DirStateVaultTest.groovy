package noe.common.utils

import groovy.io.FileType
import org.junit.Assert
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.assertEquals


class DirStateVaultTest {
  List firstLevelDir = []
  byte[] fileContentToTest
  final static int TEST_FILE_LENGTH = 200
  final static int FILES_PER_FOLDER = 3
  final static int FOLDERS_PER_FOLDER = 2

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();
  File testDir


  @Before
  void prepareTestFile() {
    testDir = testFolder.newFolder()

    for (int i=0; i < FILES_PER_FOLDER; i++) {
      File workspace = new File(testDir, "junit-my-${i}.tmp")
      workspace.createNewFile()

      fileContentToTest = new byte[TEST_FILE_LENGTH]
      new Random().nextBytes(fileContentToTest)
      workspace.setBytes(fileContentToTest)

      firstLevelDir.add(workspace)
    }

    for (int i=0; i < FOLDERS_PER_FOLDER; i++) {
      File workspace = new File(testDir, "my-folder-${i}")
      workspace.mkdirs()
      List<File> dir = []

      for (int j=0; j < FILES_PER_FOLDER; j++) {
        File targetFile = new File(workspace, "my-file-${j}.tmp")
        targetFile.createNewFile()

        fileContentToTest = new byte[TEST_FILE_LENGTH]
        new Random().nextBytes(fileContentToTest)
        targetFile.setBytes(fileContentToTest)

        dir.add(targetFile)
      }

      firstLevelDir.add(dir)
    }

  }

  @Test
  void pushPopDirFilesAddedShouldBeRemoved() {
    DirStateVault vault = new DirStateVault().push(testDir)
    File file = new File(testDir, "this-file-must-not-exists-after-pop.tmp")
    file.createNewFile()

    File dir = new File(testDir, 'this-dir-must-not-exists-after-pop')
    dir.mkdirs()
    File file1 = new File(dir, 'dummy1.tmp')
    File file2 = new File(dir, 'dummy2.tmp')
    [file1,file2]*.createNewFile()

    Assert.assertTrue("Test environment was not created successfully (${file})", file.exists())
    Assert.assertTrue("Test environment was not created successfully (${dir})", dir.exists())
    Assert.assertTrue("Test environment was not created successfully (${file1})", file1.exists())
    Assert.assertTrue("Test environment was not created successfully (${file2})", file2.exists())
    Assert.assertTrue("Testing directory was pushed, it should be registered in vault.", vault.isPushed(testDir))

    vault.pop(testDir)

    Assert.assertFalse(
      "The testing file (${file}) can not exists after pop operation (this could be unstable on windows due to mechanism locking of files)",
      file.exists()
    )
    Assert.assertFalse(
      "The testing file (${dir}) can not exists after pop operation (this could be unstable on windows due to mechanism locking of files)",
      dir.exists()
    )
    Assert.assertFalse(
      "The testing file (${file1}) can not exists after pop operation (this could be unstable on windows due to mechanism locking of files)",
      file1.exists()
    )
    Assert.assertFalse(
      "The testing file (${file2}) can not exists after pop operation (this could be unstable on windows due to mechanism locking of files)",
      file2.exists()
    )

    Assert.assertFalse("Testing directory was popped out, it should not be in vault yet.", vault.isPushed(testDir))
  }

  @Test
  void nonExistingDirectoryIsPushed() {
    File dir = new File(testDir, 'i-have-never-existed-physically')
    DirStateVault vault = new DirStateVault().push(dir)

    Assume.assumeFalse("Directory (${dir}) must not exists.", dir.exists())
    Assert.assertTrue("Testing directory was pushed, it should be registered in vault.", vault.isPushed(dir))

    vault.pop(dir)

    Assert.assertFalse("Testing directory was pushed and then poped, it must not be registered in vault.", vault.isPushed(dir))
    Assert.assertFalse("Directory (${dir}) must not exists.", dir.exists())
  }

  @Test
  void existingDirectoryWithSubdirectoryPushedAndPoped() {
    File dir = new File(testDir, 'existing-before-push')
    File subDir = new File(dir, "subDir1")

    subDir.mkdirs()
    try {
      DirStateVault vault = new DirStateVault()
      vault.push(dir)

      Assume.assumeTrue("Directory (${dir}) must exists.", dir.exists())
      Assert.assertTrue("Testing directory was pushed, it should be registered in vault.", vault.isPushed(dir))

      vault.popAll()
    } catch (IllegalStateException ex) {
      Assert.fail("IllegalStateException was thrown, but shouldn't. ${ex}")
    }
    Assert.assertFalse("Testing directory was pushed and then poped, it must not be registered in vault.", vault.isPushed(dir))
    Assert.assertTrue("Directory (${dir}) must exists.", dir.exists())
  }

  @Test
  void pushRemoveFilesDirPopRemovedMustBeReverted() {
    LinkedHashMap<File, byte[]> origContent = loadContentFromFiles()
    DirStateVault vault = new DirStateVault().push(testDir)

    testDir.deleteDir()

    vault.pop(testDir)

    testDir.eachFileRecurse { File item ->
      Assert.assertTrue("The items (${item}) must exists after pop operation.", item.exists())
      if (item.isFile()) {
        Assert.assertTrue("Restored data differs from original data", origContent.get(item) == item.getBytes())
      }
    }
  }

  @Test
  void pushModifyFilesPopModifiedMustBeReverted() {
    LinkedHashMap<File, byte[]> origContent = loadContentFromFiles()
    DirStateVault vault = new DirStateVault().push(testDir)

    modifyFilesInTestDir('footer')

    vault.pop(testDir)

    contentEqual(origContent)
  }

  @Test
  void pushModifyPushModifyPopPop(){
    LinkedHashMap<File, byte[]> origContent = loadContentFromFiles()
    DirStateVault vault = new DirStateVault().push(testDir)

    modifyFilesInTestDir('footer1')
    LinkedHashMap<File, byte[]> contentChanged1 = loadContentFromFiles()
    vault.push(testDir)
    Assert.assertTrue("Testing directory was pushed, it should be registered in vault.", vault.isPushed(testDir))

    modifyFilesInTestDir('footer2')
    LinkedHashMap<File, byte[]> contentChanged2 = loadContentFromFiles()
    contentEqual(contentChanged2)

    vault.pop(testDir)
    contentEqual(contentChanged1)
    Assert.assertTrue("Testing directory was pushed, it should be registered in vault.", vault.isPushed(testDir))

    vault.pop(testDir)
    contentEqual(origContent)
    Assert.assertFalse("Testing directory was popped out, it should not be in vault yet.", vault.isPushed(testDir))
  }

  @Test
  void pushPushSubdirModifyPushSubdirModifyPushSubdirPopParent() {
    LinkedHashMap<File, byte[]> origContent = loadContentFromFiles()
    DirStateVault vault = new DirStateVault().push(testDir)

    File subDir = new File(testDir, 'my-folder-0')
    vault.push(subDir)
    modifyFilesInTestDir('footer1')
    vault.push(subDir)
    modifyFilesInTestDir('footer2')

    vault.pop(testDir)
    contentEqual(origContent)

    Assert.assertFalse("Testing directory was pushed, it should be registered in vault.", vault.isPushed(testDir))
    Assert.assertTrue("Testing directory was popped out, it should not be in vault yet.", vault.isPushed(subDir))
  }

  @Test
  void pushPushPushOtherPopAll(){
    LinkedHashMap<File, byte[]> origContent = loadContentFromFiles()
    DirStateVault vault = new DirStateVault().push(testDir)
    modifyFilesInTestDir('footer1')
    vault.push(testDir)

    File lvlDir = testFolder.newFolder('lvl1')
    new File(lvlDir, 'fileLvl1').createNewFile()

    vault.push(lvlDir)
    File shouldNotExists1 = new File(lvlDir, 'should-not-exists-after-popall1')
    shouldNotExists1.createNewFile()

    vault.push(lvlDir)
    File shouldNotExists2 = new File(lvlDir, 'should-not-exists-after-popall2')
    shouldNotExists2.createNewFile()

    vault.popAll()

    Assert.assertFalse("File ($shouldNotExists1) should not exists after popall()", shouldNotExists1.exists())
    Assert.assertFalse("File ($shouldNotExists2) should not exists after popall()", shouldNotExists2.exists())

    contentEqual(origContent)
  }

  @Test
  void storingInaccessibleDirAndModifyPermissions() {
    Assume.assumeTrue("On non Windows only", !new Platform().isWindows())
    Assume.assumeTrue("To run this test -Drun.with.sudo=true must be set. Oper. system user has to have passwordless sudo configured.", JBFile.useAdminPrivileges)

    LinkedHashMap<File, byte[]> origContent = loadContentFromFiles()
    JBFile.chmod('ugo-rwx', testDir)
    DirStateVault vault = new DirStateVault().push(testDir)

    JBFile.chmod('u+rwx', testDir)
    File mustNotExistsFIle = new File(testDir, 'must-not-exists-after-pop')
    mustNotExistsFIle.createNewFile()
    vault.pop(testDir)

    JBFile.FilePermission permAfterPop = JBFile.retrievePermissions(testDir)
    assertEquals("Permissions on file was not reverted.", permAfterPop.perm, '0000')

    JBFile.chmod('ugo+rwx', testDir)

    Assert.assertTrue(!mustNotExistsFIle.exists())

    contentEqual(origContent)
  }

  private LinkedHashMap<File, byte[]> loadContentFromFiles() {
    Map<File, byte[]> origData = [:]
    testDir.eachFileRecurse(FileType.FILES) { File item ->
      origData.put(item, item.getBytes())
    }

    return origData
  }

  private contentEqual(origContent) {
    testDir.eachFileRecurse(FileType.FILES) { File file ->
      Assert.assertTrue("Restored data differs from original data", origContent.get(file) == file.getBytes())
    }
  }

  private modifyFilesInTestDir(String text) {
    testDir.eachFileRecurse(FileType.FILES) { File item ->
      item.setText(item.getText() + "${text}")
    }
  }

}
