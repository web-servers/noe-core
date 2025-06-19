package noe.tomcat

import noe.common.utils.Java
import noe.common.utils.Platform
import org.junit.Assume
import org.junit.BeforeClass

class BindingsTomcat10ConfiguratorIT extends BindingsTomcatConfiguratorIT {

  @BeforeClass
  public static void beforeClass() {
    Platform platform = new Platform()
    Assume.assumeFalse("JWS is not supported on HP-UX => skipping", platform.isHP())
    Assume.assumeTrue("Tomcat from JWS 6.0 requires at least Java 11", Java.isJdk1xOrHigher('11'))

    loadTestProperties("/tomcat10-common-test.properties")
    prepareWorkspace()
  }
}
