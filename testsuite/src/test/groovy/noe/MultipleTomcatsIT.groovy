package noe

import groovy.util.logging.Slf4j
import noe.common.TestAbstract
import noe.common.utils.JBFile
import noe.common.utils.Library
import noe.common.utils.Platform
import noe.ews.workspace.WorkspaceHttpdTomcats
import noe.server.ServerAbstract
import noe.workspace.ServersWorkspace
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test

import static org.junit.Assert.assertTrue

@Slf4j
class MultipleTomcatsIT extends TestAbstract {

    @BeforeClass
    static void beforeClass() {
        loadTestProperties('/ews-test.properties')
        Assume.assumeFalse("EWS is not supported on HP-UX => skipping", new Platform().isHP())
        workspace = new ServersWorkspace(
                new WorkspaceHttpdTomcats(1)
        );
        workspace.prepare();
        serverController.getHttpdServerIds().each { serverId ->
            ServerAbstract httpdServer = serverController.getServerById(serverId)
            if (httpdServer.mainHttpPort <= 1024) {
                httpdServer.shiftPorts(2000)
            }
            // we are not testing ssl here, lets remove ssl
            def sslConfFile = new File(httpdServer.getConfDeploymentPath(), "ssl.conf")
            log.debug("Deleting ssl.conf: $sslConfFile.absolutePath")
            JBFile.delete(sslConfFile)
        }
    }

    @Test
    void multipleTomcatsStartKillTest() {
        def serverIds = serverController.getTomcatServerIds().toList().take(2)
        try {
            serverIds.each { id ->
                serverController.getServerById(id).start()
            }
            Library.letsSleep(1000) // to make them running at least shortly
            serverIds.each { id ->
                ServerAbstract server = serverController.getServerById(id)
                assertTrue("Server ${id} is not running but should, start or kill problem",
                        server.isRunning())
                server.kill()
                Library.letsSleep(500) // not sure why, but sometimes server is detected to be running right after the stop/kill command
                assertTrue("Server ${id} is running but shouldn't, kill problem",
                        !server.isRunning())
            }
        } finally {
            serverController.killAllInSystem()
        }
    }

    @Test
    void multipleTomcatsStartStopTest() {
        def serverIds = serverController.getTomcatServerIds().toList().take(2)
        try {
            serverIds.each { id ->
                serverController.getServerById(id).start()
            }
            Library.letsSleep(1000) // to make them running at least shortly
            serverIds.each { id ->
                ServerAbstract server = serverController.getServerById(id)
                assertTrue("Server ${id} is not running but should, start or kill problem",
                        server.isRunning())
                server.stop()
                Library.letsSleep(500) // not sure why, but sometimes server is detected to be running right after the stop/kill command
                assertTrue("Server ${id} is running but shouldn't, kill problem",
                        !server.isRunning())
            }
        } finally {
            serverController.killAllInSystem()
        }
    }
}
