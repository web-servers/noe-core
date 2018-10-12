package noe.tomcat

import org.junit.BeforeClass


class JvmRouteTomcat6ConfiguratorIT extends JvmRouteTomcatConfiguratorIT {

  @BeforeClass
  public static void beforeClass() {
    loadTestProperties("/tomcat6-common-test.properties")
    prepareWorkspace()
  }
}
