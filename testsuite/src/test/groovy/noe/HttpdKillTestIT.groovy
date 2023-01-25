package noe

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.TestAbstract
import noe.common.utils.JBFile
import noe.common.utils.Platform
import noe.eap.workspace.WorkspaceMultipleHttpdAS7
import noe.server.ServerAbstract
import noe.workspace.ServersWorkspace
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test

import static org.junit.Assert.assertTrue

@Slf4j
class HttpdKillTestIT extends TestAbstract {

  @BeforeClass
  public static void beforeClass() {
    Platform platform = new Platform()
    Assume.assumeFalse('JBCS Httpd is not supported on HP-UX => skipping', platform.isHP())
    Assume.assumeFalse('JBCS Httpd is not supported on older versions of RHEL than RHEL 6',
            platform.isRHEL4() || platform.isRHEL5())
    loadTestProperties('/eap7-multiple-jbcs-httpd-test.properties')
    workspace = new ServersWorkspace(
            new WorkspaceMultipleHttpdAS7(false, false)
    );
    workspace.prepare()
    serverController.getHttpdServerIds().each { serverId ->
      ServerAbstract httpdServer = serverController.getServerById(serverId)
      httpdServer.shiftPorts(2000)
      // we are not testing ssl here, lets remove ssl
      def sslConfFile = new File(httpdServer.getConfDeploymentPath(), "ssl.conf")
      log.debug("Deleting ssl.conf: $sslConfFile.absolutePath")
      JBFile.delete(sslConfFile)
      // we are not testing mod_cluster here, lets remove mod_cluster
      def mod_clusterConfFile = new File(httpdServer.getConfDeploymentPath(), DefaultProperties.MOD_CLUSTER_CONFIG_FILE)
      log.debug("Deleting ${DefaultProperties.MOD_CLUSTER_CONFIG_FILE}: $mod_clusterConfFile.absolutePath")
      JBFile.delete(mod_clusterConfFile)
      // remove mod_proxy_cluster too
      mod_clusterConfFile = new File(httpdServer.getConfDeploymentPath(), DefaultProperties.MOD_PROXY_CLUSTER_CONFIG_FILE)
      log.debug("Deleting ${DefaultProperties.MOD_PROXY_CLUSTER_CONFIG_FILE}: $mod_clusterConfFile.absolutePath")
      JBFile.delete(mod_clusterConfFile)
    }
  }

  /**
   * Start server and then kill it and check if it was killed
   */
  @Test
  void singleHttpdKillTest() {
    def httpdIds = serverController.getHttpdServerIds().toList().take(1)

    try {
      httpdIds.each { id ->
        SingleServerTestUtils.serverStartKillTest(id)
      }
    } finally {
      serverController.killAllInSystem()
    }
  }

  /**
   * Start all servers and then try to kill one by one
   */
  @Test
  void multipleHttpdKillTest() {
    def server
    def httpdIds = serverController.getHttpdServerIds().toList().take(2)
    try {
      //
      httpdIds.each { id ->
        serverController.startServer(id)
      }
      httpdIds.each { id ->
        server = serverController.getServerById(id)
        assertTrue("Server ${id} is not running but should, start or kill problem",
                server.isRunning())
        server.kill()
        assertTrue("Server ${id} is running but shouldn't, kill problem",
                !server.isRunning())
      }
    } finally {
      serverController.killAllInSystem()
    }
  }

  /**
   * Start all servers then stop them and then try to kill one by one
   */
  @Test
  void httpdStopKillTest() {
    def server
    def httpdIds = serverController.getHttpdServerIds().toList().take(2)
    try {
      //
      httpdIds.each { id ->
        serverController.getServerById(id).start()
      }
      httpdIds.each { id ->
        serverController.getServerById(id).stop()
      }
      httpdIds.each { id ->
        server = serverController.getServerById(id)
        server.kill()
        assertTrue("Server ${id} is running but shouldn't, kill problem",
                !server.isRunning())
      }
    } finally {
      serverController.killAllInSystem()
    }
  }

  /**
   * Start all servers then killThemAll them and then try to kill one by one
   */
  @Test
  void httpdKillAllKillTest() {
    def server
    def httpdIds = serverController.getHttpdServerIds().toList().take(2)
    try {//
      httpdIds.each { id ->
        serverController.startServer(id)
      }
      httpdIds.each { id ->
        serverController.killAllInSystem()
      }
      httpdIds.each { id ->
        server = serverController.getServerById(id)
        server.kill()
        assertTrue("Server ${id} is running but shouldn't, kill problem",
                !server.isRunning())
      }
    } finally {
      serverController.killAllInSystem()
    }
  }

  /**
   * Start all servers then kill then one by one  them and then try to kill one by one
   */
  @Test
  void httpdKillKillTest() {
    def server
    def httpdIds = serverController.getHttpdServerIds().toList().take(2)
    try {
      httpdIds.each { id ->
        serverController.startServer(id)
      }
      httpdIds.each { id ->
        serverController.kill(id)
      }
      httpdIds.each { id ->
        server = serverController.getServerById(id)
        server.kill()
        assertTrue("Server ${id} is running but shouldn't, kill problem",
                !server.isRunning())
      }
    } finally {
      serverController.killAllInSystem()
    }
  }
}

