package noe

import groovy.util.logging.Slf4j
import noe.common.TestAbstract
import noe.common.utils.Cmd
import noe.common.utils.JBFile
import noe.common.utils.Java
import noe.common.utils.Platform
import noe.common.utils.processid.ProcessUtils
import org.junit.Assert
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test
/**
 * Test if windows is able to run noe-test without false negatives
 */
@Slf4j
class WindowsUsabilityIT extends TestAbstract {

  @BeforeClass
  public static void beforeClass() {
    Platform platform = new Platform()
    Assume.assumeTrue('This test is related only for Windows', platform.isWindows())
  }

  @Test
  public void cFolderPermissionsTest() {
    String testString = 'Test'
    String linkFilePath = '"C:\\Program Files\\test"'
    File file
    File textFile
    File linkFile
    try {
      file = new File('C:', 'test')
      Assert.assertTrue('Creation of directory inside C: has failed!', JBFile.mkdir(file))
      textFile = new File(file.getAbsolutePath(), 'test.txt')
      Assert.assertTrue('Creation of text file has failed!', textFile.createNewFile())
      textFile.append(testString)
      Assert.assertTrue('Writing into text file wasn\'t successful!', JBFile.hasMatchingLine(textFile, testString))
      linkFile = new File('C:\\Program Files\\test')
      if (linkFile.exists()) {
        JBFile.delete(linkFile)
      }
      int ret = Cmd.executeCommand(['cmd', '/C', 'mklink', '/d', linkFilePath, file.getAbsolutePath()], new File('.'))
      Assert.assertTrue('Creating of link wasn\'t successful!', ret == 0)
      Assert.assertTrue('Link file is not existing!', linkFile.exists())
    } finally {
      if (file && file.exists()) {
        JBFile.delete(file)
      }
      if (textFile && textFile.exists()) {
        JBFile.delete(file)
      }
      if (linkFile && linkFile.exists()) {
        JBFile.delete(linkFile)
      }
    }
  }

  @Test
  public void taskListTest() {
    Assume.assumeFalse("With JDK 1.6 the process remains hanging even though the Process ID used by the Java Process is properly destroyed. " +
        "Seems as some bug in JDK 1.6 => skipping for JDK 1.6", new Java().isJdk16())
    Process proc
    String windowName = 'KAREL'
    try {
      proc = Cmd.startProcess('CMD', new File('.'), windowName, [NOPAUSE: true])
      Assert.assertTrue('Staring CMD in the background wasn\'t successful', ProcessUtils.isProcAlive(proc))
      List<Integer> pids = Cmd.extractWindowsPids(windowName)
      Assert.assertTrue('TASKLIST wasn\'t successful!', !pids.isEmpty())
      pids.each {
        log.info("Extracted pids from TASKLIST" + it)
      }
    } finally {
      if (proc && ProcessUtils.isProcAlive(proc)) {
        int processId = ProcessUtils.getProcessId(proc)
        log.info("Detected processId ${processId}")
        Cmd.killTree(processId, windowName)
      }
      Cmd.killTree(null, "Administrator: ${windowName}")
      Cmd.killTree(null, "${windowName}")
    }
  }
}
