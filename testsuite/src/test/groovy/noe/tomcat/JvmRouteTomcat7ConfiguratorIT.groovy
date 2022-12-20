package noe.tomcat

import noe.common.utils.Platform
import org.junit.Assume
import org.junit.BeforeClass


class JvmRouteTomcat7ConfiguratorIT extends JvmRouteTomcatConfiguratorIT {

  @BeforeClass
  public static void beforeClass() {
    Assume.assumeTrue("Tomcat 7 is not released for RHEL8+", new Platform().isRHEL7())
    loadTestProperties("/tomcat7-common-test.properties")
    prepareWorkspace()
  }
}
