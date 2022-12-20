package noe.tomcat

import org.junit.Assume
import org.junit.BeforeClass
import noe.common.utils.Platform


class JmxTomcat7ConfiguratorIT extends JmxTomcatConfiguratorIT {

  @BeforeClass
  public static void beforeClass() {
    Assume.assumeTrue("Tomcat 7 is not released for RHEL8+", new Platform().isRHEL7())
    loadTestProperties("/tomcat7-common-test.properties")
    prepareWorkspace()
  }
}
