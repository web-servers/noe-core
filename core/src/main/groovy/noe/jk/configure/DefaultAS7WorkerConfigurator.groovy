package noe.jk.configure

import groovy.util.logging.Slf4j
import noe.common.utils.FileStateVault
import noe.server.AS7
import noe.server.ServerController

/**
 * Configure AS server to be a mod_jk workers. Prepares JVM route.
 *
 * @see WorkerNode
 * @see Configurator
 *
 * Example:<br>
 *   <code>
 *     JkScenario scenario = new JkScenario()
 *       .setFacingServerNode(new FacingServerNode(new Httpd(...)))
 *       .addBalancerNode(new BalancerNode()
 *         .addWorker(new WorkerNode(new AS7(...)))
 *         .addWorker(new WorkerNode(new AS7(...))))
 *
 *     NodeOperations ops =
 *       new JkScenarioConfigurator(
 *         scenario,
 *         DefaultHttpdConfigurator.class,
 *         DefaultAS7WorkerConfigurator.class
 *       ).configure()
 *
 *     ops.startAll()
 *
 *     // ...
 *
 *     ops.stopAll()
 *   </code>
 */
@Slf4j
class DefaultAS7WorkerConfigurator implements Configurator<DefaultAS7WorkerConfigurator> {

  WorkerNode workerNode

  FileStateVault vault = new FileStateVault()


  DefaultAS7WorkerConfigurator(WorkerNode workerNode) {
    this.workerNode = workerNode
  }

  @Override
  DefaultAS7WorkerConfigurator configure() {
    setJvmRoutes()

    return this
  }

  /**
   * Requires stopped server to be passed.
   * If running server is passed, it is stopped and not started automatically.
   */
  @Override
  DefaultAS7WorkerConfigurator revertAll() {
    if (workerNode.getServer().isRunning()) {
      log.warn("Stopping started server. It will be not started automatically!")

      // server must be stopped otherwise revert will be not applied.
      workerNode.getServer().stop()
    }

    vault.popAll()

    return this
  }

  private void setJvmRoutes() {
    backupServerConfigFile()

    boolean mgmtStartExecuted = startIfStoppedMngmt()
    int workerMajorVersion = workerNode.getServer().getVersion().getMajorVersion()
    String subsystem

    if (workerMajorVersion >= 7) {
      subsystem = "undertow"
    }
    else if (workerMajorVersion >= 6) {
      subsystem = "web"
    }
    else {
      throw new IllegalStateException("AS version older than 6 is not supported")
    }

    if (getAs7Server().getAs7Cli().runArbitraryCommand("/subsystem=${subsystem}:write-attribute(name=instance-id,value=${workerNode.getId()})").exitValue != 0 ) {
      throw new RuntimeException("Configuring of instance-id on EAP worker failed")
    }

    stopIfStartedMngmt(mgmtStartExecuted)
  }

  private FileStateVault backupServerConfigFile() {
    vault.push(new File(getAs7Server().getConfigFile()))
  }

  private startIfStoppedMngmt() {
    boolean mgmtStartExecuted = false
    if (!workerNode.getServer().isRunning()) {
      workerNode.getServer().start(["ADDITIONAL_PARAMETERS_LIST" : ["--admin-only"]])
      mgmtStartExecuted = true
    }
    mgmtStartExecuted
  }

  private stopIfStartedMngmt(boolean mgmtStartExecuted) {
    if (mgmtStartExecuted) {
      workerNode.getServer().stop()
    }
  }

  private AS7 getAs7Server() {
    return ServerController.getInstance().getServerById(workerNode.getServer().getServerId()) as AS7
  }

}
