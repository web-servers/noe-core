package noe.tomcat

import noe.common.DefaultProperties
import noe.common.utils.Java
import noe.common.utils.Platform
import org.junit.Assume
import org.junit.BeforeClass

class BindingsTomcat9ConfiguratorIT extends BindingsTomcatConfiguratorIT {

  @BeforeClass
  public static void beforeClass() {
    Platform platform = new Platform()
    Assume.assumeFalse("JWS is not supported on HP-UX => skipping", platform.isHP())
    Assume.assumeTrue("Tomcat from JWS 5.0 requires at least Java 1.8",
            (DefaultProperties.SERVER_JAVA_HOME?.contains('jdk1.8')) ?: Java.isJdkXOrHigher('1.8')
    )
    if (platform.isRHEL()) {
      Assume.assumeTrue("JWS 5.0 is officially supported on RHEL versions 9 and lower",
              platform.OSVersionLessThan(10));
    }
    loadTestProperties("/tomcat9-common-test.properties")
    prepareWorkspace()
  }
}
