package noe.common.utils

import groovy.util.logging.Slf4j
import org.junit.Assume
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

/**
 * @author Michal Hasko <mhasko@redhat.com>
 * @author Jan Blizňák <jbliznak@redhat.com>
 */
@Slf4j
class JBFileTest {

  static Platform platform = new Platform()
  static String sep = platform.sep

  static File resourceDir = new File(new File(".").getCanonicalFile(), "src${sep}test${sep}resources${sep}" + JBFileTest.class.name.replace('.', sep)+"Dir")
  static File fileLoremIpsum = new File(resourceDir, "lorem_ipsum.txt")
  static File fileMd5 = new File(resourceDir, "lorem_ipsum.md5")
  static File fileZip = new File(resourceDir, "lorem_ipsum.zip")
  static File testStructureDir = new File(resourceDir, "testStructure")
  static File dirWithOnlyOneDir = new File(testStructureDir, "oneSubdir")
  static File dirWithOnlyTwoFiles = new File(testStructureDir, "twoFiles")
  static File dirWithOneFile = new File(dirWithOnlyOneDir, "oneFile")

  @Test
  void testCleanDirectory() {

    File tmpDir = null
    try {
      tmpDir = initTestDir()
      assertTrue 'Test directory must not be empty to this test to work', tmpDir.list().length > 0

      assertFalse "Should return false for not existing directory", JBFile.cleanDirectory(new File(tmpDir, "not_existing_dir"))

      assertTrue "Cleaning must be successful", JBFile.cleanDirectory(tmpDir, false)
      assertTrue 'Test directory must be empty after cleaning: ' + tmpDir.list(), tmpDir.list().length == 0
    } finally {
      tmpDir?.deleteDir()
    }
  }

  @Test
  void testCopyDirectoryContent() {
    File tmpDestDir = null

    try {
      tmpDestDir = File.createTempDir('noe', 'jbfile')

      assertFalse "Copy must not be successful when source doesn't exist", JBFile.copyDirectoryContent(new File(testStructureDir, "not_exists"), tmpDestDir)

      assertTrue "Copy must be successful", JBFile.copyDirectoryContent(testStructureDir, tmpDestDir)

      List files1 = getFilePathsRelativeTo(testStructureDir)
      List files2 = getFilePathsRelativeTo(tmpDestDir)
      log.debug("files1: ${files1.toString()}")
      log.debug("files2: ${files2.toString()}")
      assertTrue 'Source and destination directory must contain the same structure', files1 == files2
    } finally {
      tmpDestDir?.deleteDir()
    }
  }

  @Test
  void testCopyWithDir() {
    File tmpDestDir = null

    try {
      tmpDestDir = File.createTempDir('noe', 'jbfile')
      File copiedDir = new File(tmpDestDir, testStructureDir.name)

      assertFalse "Copy must not be successful when source doesn't exist", JBFile.copy(new File(testStructureDir, "not_exists"), tmpDestDir)

      assertTrue "Copy must be successful", JBFile.copy(testStructureDir, tmpDestDir)
      assertTrue "Copied directory must exist in destination", copiedDir.exists()

      List files1 = getFilePathsRelativeTo(testStructureDir)
      List files2 = getFilePathsRelativeTo(copiedDir)
      assertTrue 'Source and destination directory must contain the same structure', files1 == files2
    } finally {
      tmpDestDir?.deleteDir()
    }
  }

  @Test
  void testCopyWithFile() {
    File tmpDestDir = null

    try {
      tmpDestDir = File.createTempDir('noe', 'jbfile')
      File copiedFile = new File(tmpDestDir, fileLoremIpsum.name)

      assertTrue("Copy must be successful", JBFile.copy(fileLoremIpsum, tmpDestDir))
      assertTrue "Copied file must exist in destination", copiedFile.exists()
    } finally {
      tmpDestDir?.deleteDir()
    }
  }

  @Test
  void testCopyFile() {

    File tmpDestDir = null

    try {
      tmpDestDir = File.createTempDir('noe', 'jbfile')

      assertFalse "Should return false for not existing file as source", JBFile.copyFile(new File(testStructureDir, "not_exists"), tmpDestDir)
      assertFalse "Should return false for directory as source", JBFile.copyFile(testStructureDir, tmpDestDir)

      File copiedFile = new File(tmpDestDir, fileLoremIpsum.name)
      assertTrue "Copy must be successful", JBFile.copyFile(fileLoremIpsum, copiedFile)
      assertTrue "Copied file must exists", copiedFile.exists()
    } finally {
      tmpDestDir?.deleteDir()
    }
  }

  @Test
  void testCreateFile() {
    File tmpDir = null
    String contentA = 'AAAA'
    String contentB = 'BBBB'

    try {
      tmpDir = File.createTempDir('noe', 'jbfile')
      File testFile = new File(tmpDir, "testfile")

      //create new file with content A
      assertFalse 'Test file should not exist before to make this test work', testFile.exists()
      assertTrue 'Creation should be successful in empty temp directory', JBFile.createFile(testFile, contentA)
      assertTrue 'Test file must exist after creation', testFile.exists()
      assertEquals 'Test file must contain desired content', contentA, testFile.text

      //create the same file with content B
      assertTrue 'Creation should be successful even when file already exists', JBFile.createFile(testFile, contentB)
      assertTrue 'Test file must exist after creation', testFile.exists()
      assertEquals 'Test file must contain new desired content', contentB, testFile.text
    } finally {
      tmpDir?.deleteDir()
    }
  }

  @Test
  void testMoveWithFile() {

    File tmpDir1 = null
    File tmpDir2 = null

    try {
      tmpDir1 = File.createTempDir('noe', 'jbfile')
      tmpDir2 = File.createTempDir('noe', 'jbfile')

      File testFile = new File(tmpDir1, fileLoremIpsum.name)
      File movedFile = new File(tmpDir2, fileLoremIpsum.name)

      assertTrue "File copy must be successful to make this test working", JBFile.copy(fileLoremIpsum, tmpDir1)
      assertTrue "Test file must exist in test destination", testFile.exists()

      assertFalse "Moving must not be successful for not existing file", JBFile.move(new File(tmpDir1, "not_exists"), tmpDir2)

      assertTrue "Moving must be successful", JBFile.move(testFile, tmpDir2)
      assertFalse "Moved file must not exist in old destination", testFile.exists()
      assertTrue "Moved file must exist in new destination", movedFile.exists()

    } finally {
      tmpDir1?.deleteDir()
      tmpDir2?.deleteDir()
    }
  }

  @Test
  void testMoveWithDir() {
    File tmpSrcDir = null
    File tmpSrcDirMirror = null
    File tmpDestDir = null

    try {
      tmpSrcDir = initTestDir()
      tmpSrcDirMirror = initTestDir() //just to be able to compare with moved content
      tmpDestDir = File.createTempDir('noe', 'jbfile')

      File movedDir = new File(tmpDestDir, tmpSrcDir.name)

      assertFalse "Moving must not be successful for not existing directory", JBFile.move(new File(tmpSrcDir, "not_exists"), tmpSrcDirMirror)

      assertTrue "Moving must be successful", JBFile.move(tmpSrcDir, tmpDestDir)
      assertFalse "Moved directory must not exist in old destination", tmpSrcDir.exists()
      assertTrue "Moved directory must exist in new destination", movedDir.exists()

      List files1 = getFilePathsRelativeTo(tmpSrcDirMirror)
      List files2 = getFilePathsRelativeTo(movedDir)
      assertTrue 'Source and destination directory must contain the same structure', files1 == files2
    } finally {
      tmpSrcDir?.deleteDir()
      tmpDestDir?.deleteDir()
      tmpSrcDirMirror?.deleteDir()
    }
  }

  @Test
  void testDelete() {

    assertFalse 'Deletion of not existing file should return false', JBFile.delete(new File(testStructureDir, "not_exists"))

    File tmpDir1 = null
    File tmpDir2 = null
    try {
      tmpDir1 = File.createTempDir('noe', 'jbfile')
      tmpDir2 = initTestDir()
      File testFile = new File(tmpDir1, "testfile")
      testFile.createNewFile()

      assertTrue 'Deletion of existing file must be successful', JBFile.delete(testFile)
      assertFalse 'Test file must not exist after successful deletion', testFile.exists()

      assertTrue 'Deletion of existing empty directory must be successful', JBFile.delete(tmpDir1)
      assertFalse 'Deleted directory must not exist after successful deletion', tmpDir1.exists()

      assertTrue 'Deletion of existing non-empty directory must be also successful', JBFile.delete(tmpDir2)
      assertFalse 'Deleted directory must not exist after successful deletion', tmpDir2.exists()
    } finally {
      tmpDir1?.deleteDir()
      tmpDir2?.deleteDir()
    }
  }

  @Test
  void testReplaceRegexpLineModeEnabled() {

    String contentBefore = """blah blah blah
{abcd} : => 'xxxx'
{1234} : => 'yyyy'
blah blah blah
{1a2b} : => 'zzzz'
{} : => 'zzzz'
blah blah blah
"""
    String contentAfter = """blah blah blah
#{abcd}=xxxx
#{1234}=yyyy
blah blah blah
#{1a2b}=zzzz
{} : => 'zzzz'
blah blah blah
"""

    File testFile = null
    try {
      testFile = File.createTempFile('noe', 'jbfile')
      assertTrue JBFile.createFile(testFile, contentBefore)

      assertTrue JBFile.replaceregexp(testFile, "(\\{[a-z0-9]+\\}) \\: => '([a-z]+)'", "#\\1=\\2", true)
      assertEquals 'File content after replace must match to desired content', contentAfter, testFile.text
    } finally {
      testFile?.delete()
    }
  }

  @Test
  void testReplaceRegexp() {
    //line mode disabled == can match for newlines

    String contentBefore = """blah blah blah
{abcd} : => 'xxxx'
{1234} : => 'yyyy'
blah blah blah
{1a2b} : => 'zzzz'
{} : => 'zzzz'
blah blah blah
"""
    String contentAfter = """blah blah blah
{abcd} : => 'xxxx'
{1234} : => 'yyyy'#{1a2b}=zzzz
{} : => 'zzzz'
blah blah blah
"""

    File testFile = null
    try {
      testFile = File.createTempFile('noe', 'jbfile')
      assertTrue JBFile.createFile(testFile, contentBefore)

      assertTrue JBFile.replaceregexp(testFile, "\\nblah blah blah\\n(\\{[a-z0-9]+\\}) \\: => '([a-z]+)'", "#\\1=\\2", false)
      assertEquals 'File content after replace must match to desired content', contentAfter, testFile.text
    } finally {
      testFile?.delete()
    }
  }

  @Test
  void testReplace() {

    String contentBefore = """blah blah blah
{abcd} : => 'xxxx'
blah blah blah
"""
    String contentAfter = """##abcd='xxxx'
blah blah blah
"""

    File testFile = null
    try {
      testFile = File.createTempFile('noe', 'jbfile')
      assertTrue JBFile.createFile(testFile, contentBefore)

      assertTrue JBFile.replace(testFile, "blah blah blah\n{abcd} : => ", "##abcd=") //regular string, no need to escape as regexp
      assertEquals 'File content after replace must match to desired content', contentAfter, testFile.text
    } finally {
      testFile?.delete()
    }
  }

  @Test
  void readOctalPermissionsTest() {
    Assume.assumeFalse('Octal permissions are only present on Unix-like', platform.isWindows())
    File testFile = null
    File testDir = null
    try {
      testFile = File.createTempFile('noe', 'jbfile')
      String perm = "4755"
      assertTrue("Wrong chmod return code ", JBFile.chmod(perm, testFile) == 0)
      assertTrue perm == JBFile.readOctalPermissions(testFile)

      perm = "0147"
      assertTrue("Wrong chmod return code ", JBFile.chmod(perm, testFile) == 0)
      assertTrue perm == JBFile.readOctalPermissions(testFile)

      perm = "4111"
      assertTrue("Wrong chmod return code ", JBFile.chmod(perm, testFile) == 0)
      assertTrue perm == JBFile.readOctalPermissions(testFile)

      perm = "2000"
      assertTrue("Wrong chmod return code ", JBFile.chmod(perm, testFile) == 0)
      assertTrue perm == JBFile.readOctalPermissions(testFile)

      perm = "2010"
      assertTrue("Wrong chmod return code ", JBFile.chmod(perm, testFile) == 0)
      assertTrue perm == JBFile.readOctalPermissions(testFile)

      perm = "4000"
      assertTrue("Wrong chmod return code ", JBFile.chmod(perm, testFile) == 0)
      assertTrue perm == JBFile.readOctalPermissions(testFile)

      perm = "0777"
      assertTrue("Wrong chmod return code ", JBFile.chmod(perm, testFile) == 0)
      assertTrue perm == JBFile.readOctalPermissions(testFile)

      testDir = File.createTempDir('noe', 'jbfile')
      perm = "5000"
      assertTrue("Wrong chmod return code ", JBFile.chmod(perm, testDir) == 0)
      assertTrue perm == JBFile.readOctalPermissions(testDir)

      perm = "4777"
      assertTrue("Wrong chmod return code ", JBFile.chmod(perm, testDir) == 0)
      assertTrue perm == JBFile.readOctalPermissions(testDir)


    } finally {
      testFile?.delete()
      testDir?.delete()
    }
  }

  @Test
  void testNativeUnzip() {
    File tmpDestDir = null

    try {
      tmpDestDir = File.createTempDir('noe', 'jbfile')
      JBFile.nativeUnzip(fileZip, tmpDestDir)
      assertTrue 'Unzipped content must match to expected one', [fileLoremIpsum.name, fileMd5.name].sort() == tmpDestDir.list().sort()
    } finally {
      tmpDestDir?.deleteDir()
    }
  }

  @Test
  void testGenerateRandomBinaryFile() {
    File destination = File.createTempFile("random", ".bin")
    int length = 12345

    try {
      JBFile.generateRandomBinaryFile(destination, length)
      assertEquals("Generated file length", length, destination.length())
    } finally {
      destination.delete()
    }
  }

  @Test
  void testHasMatchingLine() {
    String content = """first line
second line
third line
"""


    File testFile = null
    try {
      testFile = File.createTempFile('noe', 'jbfile')
      assertTrue JBFile.createFile(testFile, content)

      assertFalse JBFile.hasMatchingLine(testFile, "no line") // should fail as there is no such line
      assertTrue JBFile.hasMatchingLine(testFile, /second.*/) // regular expression which should match
      assertFalse JBFile.hasMatchingLine(testFile, /first.*/, 2) // regular expression which should match

    } finally {
      testFile?.delete()
    }
  }

  @Test
  void testIsExistingFile() {

    File tmpDir = null
    try {
      tmpDir = initTestDir()
      assertTrue 'Test directory must not be empty to this test to work', tmpDir.list().length > 0

      assertFalse "Should return false for non-existing directory", JBFile.isExistingFile(new File(tmpDir, "not_existing_dir"))
      assertFalse "Should return false for existing directory", JBFile.isExistingFile(new File(tmpDir, "subdir1"))
      assertTrue "Should return true for existing file",
              JBFile.isExistingFile(new File(tmpDir, "noe_test.1"))
      assertFalse "Should return false for non-existing file",
              JBFile.isExistingFile(new File(tmpDir, "not_existing_dir"))

    } finally {
      tmpDir?.deleteDir()
    }
  }

  /**
   * Create temp test directory and fill it with some file structure
   */
  private static File initTestDir() {

    File tmpDir = File.createTempDir('noe', 'jbfile')
    new File(tmpDir, "noe_test.1").createNewFile()
    new File(tmpDir, "noe_test.2").createNewFile()


    File subDir1 = new File(tmpDir, 'subdir1')
    subDir1.mkdirs()
    new File(subDir1, "noe_test.11").createNewFile()
    new File(subDir1, "noe_test.12").createNewFile()

    File subDir2 = new File(tmpDir, 'subdir2')
    subDir2.mkdirs()
    new File(subDir1, "noe_test.21").createNewFile()
    new File(subDir1, "noe_test.22").createNewFile()
    File subDir3 = new File(subDir2, 'subdir3')
    subDir3.mkdirs()
    return tmpDir
  }

  private static List getFilePathsRelativeTo(File dir) {
    List files = []
    dir.eachFileRecurse { files << it.absolutePath.replace(dir.absolutePath, '') }
    return files.sort()
  }
}
