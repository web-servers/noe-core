package noe.tomcat

import noe.common.utils.Java
import noe.common.utils.Platform
import org.junit.Assume
import org.junit.BeforeClass

class JmxTomcat9ConfiguratorIT extends JmxTomcatConfiguratorIT {

  @BeforeClass
  public static void beforeClass() {
    Platform platform = new Platform()
    Assume.assumeFalse("JWS is not supported on HP-UX => skipping", platform.isHP())
    Assume.assumeTrue("Tomcat from JWS 3.0 requires at least Java 1.7", Java.isJdk1xOrHigher('1.8'))

    loadTestProperties("/tomcat9-common-test.properties")
    prepareWorkspace()
  }

}
