package noe.tomcat

import org.junit.BeforeClass


class BindingsTomcat6ConfiguratorIT extends BindingsTomcatConfiguratorIT {

  @BeforeClass
  public static void beforeClass() {
    loadTestProperties("/tomcat6-common-test.properties")
    prepareWorkspace()
  }
}
