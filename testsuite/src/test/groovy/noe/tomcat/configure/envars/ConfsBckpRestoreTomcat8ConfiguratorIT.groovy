package noe.tomcat.configure.envars

import noe.tomcat.JvmRouteTomcatConfiguratorIT
import org.junit.BeforeClass


class ConfsBckpRestoreTomcat8ConfiguratorIT extends ConfsBckpRestoreTomcatConfiguratorIT {

  @BeforeClass
   static void beforeClass() {
    loadTestProperties("/tomcat8-common-test.properties")
    prepareWorkspace()
  }
}
