package noe.common.utils

import noe.common.DefaultProperties
import noe.common.NoeContext
import org.junit.Test

/**
 * This contains test for static initialization
 * Recursive static initialization will fail without much info about what happens, to prevent such
 * encounters this tests were created
 *
 * Example:
 *
 * class JBFile {
    static {
      DefaultProperties.TOMCAT_POSTINSTALL_WITH_SUDO
    }
  }

 * Class DefaultProperties {
    static {
      TOMCAT_POSTINSTALL_WITH_SUDO = JBFile.useAdminPrivileges
    }
  }
 *
 * JBFile.delete(new File('I_DO_NOT_EXITS'))
 *
 * Will fail with java.lang.NoClassDefFoundError: Could not initialize class noe.common.utils.JBFile
 *
 *
 * To achieve full test functionality each test has to be run separately 
 */
class StaticInitializationTest {

  @Test
  void classJBFileInitializationTest() {
    JBFile.delete(new File('I_DO_NOT_EXITS'))
  }

  @Test
  void classDefaultPropertiesInitializationTest() {
    DefaultProperties.HOST
  }

  @Test
  void classLibraryInitializationTest() {
    Library.getUniversalProperty('I_DO_NOT_EXITS')
  }

  @Test
  void classCmdInitializationTest() {
    Cmd.getProps()
  }

  @Test
  void classNoeContextInitializationTest() {
    NoeContext.forCurrentContext()
  }

  @Test
  void classPathHelperInitializationTest() {
    PathHelper.isAbsolute('I_DO_NOT_EXITS')
  }

  @Test
  void classJavaInitializationTest() {
    Java.isJdk16()
  }
}
