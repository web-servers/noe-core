package noe.bootstrap

import groovy.util.logging.Slf4j
import noe.app.Firefox
import noe.common.utils.Cmd
import noe.common.utils.Hudson
import noe.common.utils.Library
import noe.common.utils.Platform
import noe.server.ServerController

@Slf4j
class Main {
  public static final TASKS_CLEANUP_PROPERTY_NAME = 'tasks.cleanup.disable'

  static main(args) {
    log.info("[bootrstrap mode]: STARTED")
    log.debug("Cleaning for NOE irrelevant variables introduced by Jenkins env done mainly because of " +
        "http://support.microsoft.com/kb/830473: " + Hudson.jenkinsEnvVarsIrrelevantForNOE.keySet())
    Cmd.removeGlobalEnvVar(Hudson.jenkinsEnvVarsIrrelevantForNOE)

    StringBuilder propsAsString = new StringBuilder()
    Platform platform = new Platform()
    Cmd.props.each {propsAsString.append("${it.key}=${it.value}").append(platform.nl)}
    log.info("[bootstrap mode] running with ENV:\n{}", propsAsString.toString())

    def disableTSCleanup = Boolean.valueOf(Library.getUniversalProperty(TASKS_CLEANUP_PROPERTY_NAME, 'false'))

    if (!disableTSCleanup) {
      log.debug("Registering shutdown hook")
      // Register shutdown hook
      addShutdownHook {

        log.info("[Shutdown Hook]: STARTED")
        ServerController serverController = ServerController.getInstance()
        serverController.killAllInSystem()
        // We need to revert firefox profiles.ini on MS Windows
        Firefox.revertFirefoxProfilesIni()
        // Switch-on windows firewall
        Library.switchWindowsFirewall(true)

        log.info('------------------------------------------------')
        log.info('PROCESS LIST')
        log.info('------------------------------------------------')
        Cmd.logSystemProcesses(new Platform().actualUser)
        log.info('------------------------------------------------')

        log.info('------------------------------------------------')
        log.info('OPENED PORTS')
        log.info('------------------------------------------------')
        Cmd.logSystemOpenedPorts()
        log.info('------------------------------------------------')

        log.info("[Shutdown Hook]: FINISHED")

      }
    } else {
      log.warn("Shutdown hook is disabled based on property ${TASKS_CLEANUP_PROPERTY_NAME} => skipping shutdown hook registration, " +
          "this way is not guaranteed that started servers are stopped when TS exits.\n" +
          "It can be useful for preparing environment for further manual investigation, use at your own risk")
    }

    // get tasks
    def tasks = TaskAbstract.loadTasks()

    // at default set running to unsuccessful, if no exception thrown it should be reset to 0 to indicate successful run
    int exitCode = 127
    try {
      // execute tasks
      tasks.each {
        log.info("TASK STARTED: " + it.getClass().getCanonicalName())
        it.prepare()
        it.execute()
        if (!disableTSCleanup) {
          it.destroy()
        } else {
          log.info("Skipping cleanup after task execution based on property ${TASKS_CLEANUP_PROPERTY_NAME} for task " + it.getClass().getCanonicalName())
        }
        log.info("TASK FINISHED: " + it.getClass().getCanonicalName())
      }

      log.info("[bootrstrap mode]: FINISHED")
      exitCode = 0 // set to 0 only if no exception occurs
    } catch (Exception ex) {
      log.error("Unhandled exception caught", ex);
    } finally {
      // System.exit should invoke shutdown hook, thereby this should be the desired behaviour and prevent stucking of the job even if exception is thrown
      System.exit(exitCode)
    }
  }
}
