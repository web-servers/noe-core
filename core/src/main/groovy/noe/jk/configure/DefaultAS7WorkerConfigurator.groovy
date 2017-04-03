package noe.jk.configure

import noe.eap.creaper.ManagementClientProvider
import noe.server.AS7
import org.wildfly.extras.creaper.commands.undertow.AddUndertowListener
import org.wildfly.extras.creaper.commands.web.AddConnector

/**
 * Represents AS7 as worker in JK scenarios
 */
class DefaultAS7WorkerConfigurator implements Configurator<DefaultAS7WorkerConfigurator> {

  WorkerNode<AS7> workerNode


  DefaultAS7WorkerConfigurator(WorkerNode workerNode) {
    this.workerNode = workerNode
  }

  @Override
  DefaultAS7WorkerConfigurator configure() {
    setJvmRoutes()

    return this
  }

  private void setJvmRoutes() {
    boolean mgmtStartExecuted = startIfStoppedMngmt()

    String subsystem
    if (workerNode.getServer().getVersion().getMajorVersion() >= 7) subsystem = "undertow"
    else if (workerNode.getServer().getVersion().getMajorVersion() >= 6) subsystem = "web"
    else throw new IllegalStateException("AS version older than 6 is not supported")

    if (workerNode.getServer().getAs7Cli().runArbitraryCommand("/subsystem=${subsystem}:write-attribute(name=instance-id,value=${workerNode.getId()})").exitValue > 0 ) {
      throw new RuntimeException("Configuring of instance-id on EAP worker failed")
    }

    stopIfStartedMngmt(mgmtStartExecuted)
  }

  private startIfStoppedMngmt() {
    boolean mgmtStartExecuted = false
    if (!(workerNode.getServer()).isRunning()) {
      (workerNode.getServer()).start(["ADDITIONAL_PARAMETERS_LIST" : ["--admin-only"]])
      mgmtStartExecuted = true
    }
    mgmtStartExecuted
  }

  private stopIfStartedMngmt(boolean mgmtStartExecuted) {
    if (mgmtStartExecuted) (workerNode.getServer()).stop()
  }

}
