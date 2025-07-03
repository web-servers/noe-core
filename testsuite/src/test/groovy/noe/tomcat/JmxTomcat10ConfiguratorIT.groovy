package noe.tomcat

import noe.common.DefaultProperties
import noe.common.utils.Java
import noe.common.utils.Platform
import org.junit.Assume
import org.junit.BeforeClass

class JmxTomcat10ConfiguratorIT extends JmxTomcatConfiguratorIT {

  @BeforeClass
  public static void beforeClass() {
    Platform platform = new Platform()
    def java11Indicators = ['jdk11', 'java-11', 'openjdk-11']
    def serverJavaHomeMatches = java11Indicators.any { DefaultProperties.SERVER_JAVA_HOME?.contains(it) }

    Assume.assumeFalse("JWS is not supported on HP-UX => skipping", platform.isHP())
    Assume.assumeFalse("JWS 6.0 is officially supported on RHEL versions 8 and above", platform.OSVersionLessThan(8));
    Assume.assumeTrue("Tomcat from JWS 6.0 requires at least Java 11",
            serverJavaHomeMatches ?: Java.isJdkXOrHigher('11')
    )

    loadTestProperties("/tomcat10-common-test.properties")
    prepareWorkspace()
  }

}
