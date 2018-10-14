package noe.tomcat.configure.envars

import noe.tomcat.JvmRouteTomcatConfiguratorIT
import org.junit.BeforeClass


class ConfsBckpRestoreTomcat6ConfiguratorIT extends ConfsBckpRestoreTomcatConfiguratorIT {

  @BeforeClass
   static void beforeClass() {
    loadTestProperties("/tomcat6-common-test.properties")
    prepareWorkspace()
  }
}
