package noe.tomcat

import org.junit.BeforeClass


class JvmRouteTomcat7ConfiguratorIT extends JvmRouteTomcatConfiguratorIT {

  @BeforeClass
  public static void beforeClass() {
    loadTestProperties("/tomcat7-common-test.properties")
    prepareWorkspace()
  }
}
