package noe

import groovy.util.logging.Slf4j
import noe.common.TestAbstract
import noe.common.utils.Java
import noe.common.utils.Platform
import noe.workspace.ServersWorkspace
import noe.workspace.WorkspaceTomcat
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test

@Slf4j
class TomcatJws6IT extends TestAbstract {


    @BeforeClass
    public static void beforeClass() {
        Platform platform = new Platform()
        Assume.assumeFalse("JWS is not supported on HP-UX => skipping", platform.isHP())
        Assume.assumeTrue("Tomcat from JWS 6 requires at least Java 11", Java.isJdk1xOrHigher('11'))
        loadTestProperties('/jws6-test.properties')
        workspace = new ServersWorkspace(
                new WorkspaceTomcat()
        )
        workspace.prepare()
    }

    @Test
    void serverStartStopTest() {
        serverController.getServerIds().each { serverId ->
            SingleServerTestUtils.serverStartStopTest(serverId)
        }
    }

    @Test
    void serverStartKillTest() {
        serverController.getServerIds().each { serverId ->
            SingleServerTestUtils.serverStartKillTest(serverId)
        }
    }

    @Test
    void killAllInSystemTest() {
        serverController.getServerIds().each { serverId ->
            SingleServerTestUtils.killAllInSystemTest(serverId)
        }
    }

}
