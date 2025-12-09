package noe.tomcat

import noe.common.DefaultProperties
import noe.common.utils.Java
import noe.common.utils.Platform
import org.junit.Assume
import org.junit.BeforeClass

class BindingsTomcat10ConfiguratorIT extends BindingsTomcatConfiguratorIT {

  @BeforeClass
  public static void beforeClass() {
    Platform platform = new Platform()
    def serverJavaHomeMatches = Java.JAVA_17_INDICATORS.any { DefaultProperties.SERVER_JAVA_HOME?.contains(it) }

    Assume.assumeFalse("JWS is not supported on HP-UX => skipping", platform.isHP())
    if (platform.isRHEL()) {
      Assume.assumeFalse("JWS 6.0 is officially supported on RHEL versions 8 and above",
              platform.OSVersionLessThan(8))
    }
    Assume.assumeTrue("Tomcat from JWS 6.0 requires at least Java 17",
            serverJavaHomeMatches ?: Java.isJdkXOrHigher('17')
    )

    loadTestProperties("/tomcat11-common-test.properties")
    prepareWorkspace()
  }
}
