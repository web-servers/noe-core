package noe.tomcat

import noe.common.DefaultProperties
import noe.common.utils.Java
import noe.common.utils.Platform
import org.junit.Assume
import org.junit.BeforeClass

class JvmRouteTomcat8ConfiguratorIT extends JvmRouteTomcatConfiguratorIT {

  @BeforeClass
  public static void beforeClass() {
    Platform platform = new Platform()
    Assume.assumeFalse("JWS is not supported on HP-UX => skipping", platform.isHP())
    Assume.assumeTrue("JWS 3.0 is not supported on RHEL8+ => skipping", platform.isRHEL6() || platform.isRHEL7())
    Assume.assumeTrue("Tomcat from JWS 3.0 requires at least Java 1.7",
            (DefaultProperties.SERVER_JAVA_HOME?.contains('jdk1.7')) ?: Java.isJdkXOrHigher('1.7')
    )

    loadTestProperties("/tomcat8-common-test.properties")
    prepareWorkspace()
  }

}
