package noe.tomcat

import noe.common.utils.Java
import noe.common.utils.Platform
import org.junit.Assume
import org.junit.BeforeClass

class BindingsTomcat8ConfiguratorIT extends BindingsTomcatConfiguratorIT {

  @BeforeClass
  public static void beforeClass() {
    Platform platform = new Platform()
    Assume.assumeFalse("JWS is not supported on HP-UX => skipping", platform.isHP())
    Assume.assumeFalse("JWS 3.0 is not supported on RHEL5 => skipping", platform.isRHEL5())
    Assume.assumeTrue("Tomcat from JWS 3.0 requires at least Java 1.7", Java.isJdk1xOrHigher('1.7'))

    loadTestProperties("/tomcat8-common-test.properties")
    prepareWorkspace()
  }
}
