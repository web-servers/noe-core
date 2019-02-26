package noe

import groovy.util.logging.Slf4j
import noe.common.utils.Cmd
import noe.common.utils.Library
import noe.common.utils.Platform
import noe.server.ServerAbstract
import noe.server.ServerController
import org.junit.Assert
import org.junit.Assume

@Slf4j
class SingleServerTestUtils {

  public static void serverStartStopTest(String serverId, Map startConf = [:]) {
    ServerController serverController = ServerController.getInstance()
    try {
      ServerAbstract server = serverController.getServerById(serverId)
      Assert.assertFalse("Server ${serverId} shouldn't be running before starting the test", server.isRunning())
      server.start()
      Assert.assertTrue("Server ${serverId} should be running, but isn't", server.isRunning())
      Library.letsSleep(1000) // to make it running at least shortly
      server.stop()
      Library.letsSleep(500) // not sure why, but sometimes server is detected to be running right after the stop/kill command
      boolean running = server.isRunning()
      if (running) {
       log.info("${serverId} is running even though it is not expected to, printing running processes and opened ports")
        Cmd.logSystemProcesses()
        Cmd.logSystemOpenedPorts()
      }
      Assert.assertFalse("Server ${serverId} shouldn't be running after being stopped", running)
    } catch (Exception ex) {
      log.error("Server start stop test for ${serverId} exception detected", ex)
      Assert.fail("${serverId}: serverStartStopTest failed: ${ex}")
    } finally {
      serverController.getServerById(serverId).killAllInSystem()
    }
  }

  public static void serverStartKillTest(String serverId, Map startConf = [:]) {
    ServerController serverController = ServerController.getInstance()
    try {
      ServerAbstract server = serverController.getServerById(serverId)
      Assert.assertFalse("Server ${serverId} shouldn't be running before starting the test", server.isRunning())
      server.start()
      Assert.assertTrue("Server ${serverId} should be running, but isn't", server.isRunning())
      Library.letsSleep(1000) // to make it running at least shortly
      server.kill()
      Library.letsSleep(500) // not sure why, but sometimes server is detected to be running right after the stop/kill command
      boolean running = server.isRunning()
      if (running) {
        log.info("${serverId} is running even though it is not expected to, printing running processes and opened ports")
        Cmd.logSystemProcesses()
        Cmd.logSystemOpenedPorts()
      }
      Assert.assertFalse("Server ${serverId} shouldn't be running after being killed", running)
    } catch (Exception ex) {
      log.error("ServerStartKillTest failed with exception", ex)
      Assert.fail("${serverId}: serverStartKillTest failed: ${ex}")
    } finally {
      serverController.getServerById(serverId).killAllInSystem()
    }
  }

  public static void killAllInSystemTest(String serverId, Map startConf = [:]) {
    Platform platform = new Platform()
    Assume.assumeFalse("Skipping killAllInSystem test on Solaris due limitations of command length printed in ps -ef resulting in killAll " +
        "not working properly. This is known issue and making this step skipped till it is resolved.", platform.isSolaris())
    ServerController serverController = ServerController.getInstance()
    try {
      ServerAbstract server = serverController.getServerById(serverId)
      Assert.assertFalse("Server ${serverId} shouldn't be running before starting the test", server.isRunning())
      server.start()
      Assert.assertTrue("Server ${serverId} should be running, but isn't", server.isRunning())
      Library.letsSleep(1000) // to make it running at least shortly
      server.killAllInSystem()
      Library.letsSleep(500) // not sure why, but sometimes server is detected to be running right after the stop/kill command
      boolean running = server.isRunning()
      if (running) {
        log.info("${serverId} is running even though it is not expected to, printing running processes and opened ports")
        Cmd.logSystemProcesses()
        Cmd.logSystemOpenedPorts()
      }
      Assert.assertFalse("Server ${serverId} shouldn't be running after being killed", running)
    } catch (Exception ex) {
      log.error("Server kill all in system test for ${serverId} exception detected", ex)
      Assert.fail("${serverId}: serverkillAllInSystemTest failed: ${ex}")
    }
  }
}
