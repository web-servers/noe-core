package noe.tomcat.configure.envars

import noe.tomcat.JvmRouteTomcatConfiguratorIT
import org.junit.BeforeClass


class ConfsBckpRestoreTomcat7ConfiguratorIT extends ConfsBckpRestoreTomcatConfiguratorIT {

  @BeforeClass
   static void beforeClass() {
    loadTestProperties("/tomcat7-common-test.properties")
    prepareWorkspace()
  }
}
