package noe.tomcat.configure.envars

import org.junit.BeforeClass


class ConfsBckpRestoreTomcat8ConfiguratorIT extends ConfsBckpRestoreTomcatConfiguratorIT {

  @BeforeClass
   static void beforeClass() {
    loadTestProperties("/tomcat8-common-test.properties")
    prepareWorkspace()
  }
}
