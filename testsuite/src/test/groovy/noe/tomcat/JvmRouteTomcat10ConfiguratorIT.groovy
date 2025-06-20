package noe.tomcat

import noe.common.DefaultProperties
import noe.common.utils.Java
import noe.common.utils.Platform
import org.junit.Assume
import org.junit.BeforeClass

class JvmRouteTomcat10ConfiguratorIT extends JvmRouteTomcatConfiguratorIT {

    @BeforeClass
    public static void beforeClass() {
        Platform platform = new Platform()
        Assume.assumeFalse("JWS is not supported on HP-UX => skipping", platform.isHP())

        Assume.assumeTrue("Tomcat from JWS 6.0 requires at least Java 11",
                (DefaultProperties.SERVER_JAVA_HOME?.contains('jdk11')) ?: Java.isJdkXOrHigher('11')
        )

        loadTestProperties("/tomcat10-common-test.properties")
        prepareWorkspace()
    }

}
