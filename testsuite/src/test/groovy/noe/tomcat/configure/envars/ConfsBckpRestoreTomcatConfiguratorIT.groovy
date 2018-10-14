package noe.tomcat.configure.envars

import noe.common.utils.Platform
import noe.tomcat.TomcatTestAbstract
import noe.tomcat.configure.TomcatConfigurator
import org.junit.Assert
import org.junit.Test

/**
 * Abstract class for backing up and restoring configurations files.
 *
 * Needs to be extended and implemented following method:
 *
 * <code>
 *   @BeforeClass
 *   public static void beforeClass() {
 *     loadTestProperties("/tomcatX.properties")
 *     prepareWorkspace()
 *   }
 * </code>
 */
abstract class ConfsBckpRestoreTomcatConfiguratorIT extends TomcatTestAbstract {


  @Test
  void revertAllChangesInConfs() {
    int testOffset = 10000
    File serverXml = new File(tomcat.basedir, "conf/server.xml")
    File setenv = new File(tomcat.basedir, "bin/setenv." + platform.getScriptSuffix())
    String origContent = serverXml.getText()
    checkSetenvPresencePerPlatform(setenv)

    TomcatConfigurator configurator = new TomcatConfigurator(tomcat)
      .portOffset(testOffset)
    String modifiedContent = serverXml.getText()
    Assert.assertFalse origContent.equals(modifiedContent)

    configurator
      .envVariableByAppend('TEST_VARIABLE', 'TEST_VALUE')
      .envVariableByAppend('TEST_VARIABLE2', 'TEST_VALUE2')
    Assert.assertTrue setenv.getText().contains('TEST_VARIABLE2')

    configurator.revertAllConfiguration()
    String revertedContent = serverXml.getText()
    Assert.assertTrue origContent.equals(revertedContent)
    checkSetenvPresencePerPlatform(setenv)
  }

  private void checkSetenvPresencePerPlatform(File setenv) {
    if (new Platform().isWindows()) {
      Assert.assertTrue setenv.exists()
      Assert.assertFalse setenv.getText().contains('TEST_VARIABLE2')
    } else {
      Assert.assertFalse setenv.exists()
    }
  }

}
