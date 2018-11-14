package noe.tomcat.configure.envars

import org.junit.BeforeClass


class ConfsBckpRestoreTomcat9ConfiguratorIT extends ConfsBckpRestoreTomcatConfiguratorIT {

  @BeforeClass
   static void beforeClass() {
    loadTestProperties("/tomcat9-common-test.properties")
    prepareWorkspace()
  }
}
