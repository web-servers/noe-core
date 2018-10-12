package noe.tomcat

import org.junit.BeforeClass


class JmxTomcat6ConfiguratorIT extends JmxTomcatConfiguratorIT {

  @BeforeClass
  public static void beforeClass() {
    loadTestProperties("/tomcat6-common-test.properties")
    prepareWorkspace()
  }
}
