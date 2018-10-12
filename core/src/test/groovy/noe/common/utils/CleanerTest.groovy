package noe.common.utils

import org.junit.Test
import static org.junit.Assert.assertTrue

/**
 * @author Bogdan Sikora <bsikora@redhat.com>
 */
class CleanerTest {


  @Test
  void cleanDirectoryBasedOnRegexTest() {

    File root = File.createTempDir('noe', 'cleaner')

    try {
      doCleanDirectoryBasedOnRegexTest(['jws-3.0', 'jboss-eap-7', 'jboss-ews-2', 'httpd', 'test'], root)

      doCleanDirectoryBasedOnRegexTest(['jws-3.0file', 'jboss-eap-7', 'jboss-ews-2', 'httpd', 'testfile'], root)

      doCleanDirectoryBasedOnRegexTest(['jws-3.0file', 'jboss-eap-7file', 'jboss-ews-2file', 'httpdfile', 'test'], root)
    } finally {
      JBFile.delete(root)
    }

  }

  void doCleanDirectoryBasedOnRegexTest(List<String> testFileList, File root) {
    String path = root.getAbsolutePath()
    fileGenerator(testFileList, path)
    testFileList.each { fileName ->
      assertTrue('File wasn\'t correctly created', new File(path, fileName).exists())
    }
    Cleaner.cleanDirectoryBasedOnRegex(root, /.*(jws|jboss-ews|jbcs|jboss-eap|httpd).*/)
    root.listFiles().each { file ->
      assertTrue('Files weren\'t correctly deleted', file.getName() == testFileList[-1])
      testFileList[0..-2].each {
        assertTrue("File $it should be deleted", file.getName() != it)
      }
    }
    JBFile.cleanDirectory(root)
  }

  /**
   * Method used to generate files and folder in folder
   * @param files, list of files to be created, if name end with file file is created, folder otherwise
   * @param path, string path as path to the root folder where files should be created
   */
  static fileGenerator(List<String> files, String path) {
    files.each { fileName ->
      if ( fileName ==~ /.*file/) {
        JBFile.createFile(new File(path, fileName))
      } else {
        JBFile.mkdir(new File(path, fileName))
      }
    }
  }
}
