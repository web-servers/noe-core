package noe.tomcat

import org.junit.BeforeClass


class BindingsTomcat7ConfiguratorIT extends BindingsTomcatConfiguratorIT {

  @BeforeClass
  public static void beforeClass() {
    loadTestProperties("/tomcat7-common-test.properties")
    prepareWorkspace()
  }
}
