package noe.tomcat

import org.junit.BeforeClass


class JmxTomcat7ConfiguratorIT extends JmxTomcatConfiguratorIT {

  @BeforeClass
  public static void beforeClass() {
    loadTestProperties("/tomcat7-common-test.properties")
    prepareWorkspace()
  }
}
