package noe.jk.configure

import noe.server.Tomcat
import noe.tomcat.configure.AjpConnectorTomcat
import noe.tomcat.configure.NonSecureHttpConnectorTomcat
import noe.tomcat.configure.TomcatConfigurator

/**
 * Represents Tomcat server in JK scenarios.
 */
class DefaultTomcatWorkerConfigurator implements Configurator<DefaultTomcatWorkerConfigurator> {

  WorkerNode<Tomcat> workerNode


  DefaultTomcatWorkerConfigurator(WorkerNode workerNode) {
    this.workerNode = workerNode
  }

  @Override
  DefaultTomcatWorkerConfigurator configure() {
    new TomcatConfigurator(workerNode.getServer())
        .httpConnector(new NonSecureHttpConnectorTomcat().setAddress(workerNode.getHost()))
        .ajpConnector(new AjpConnectorTomcat().setPort(workerNode.getAjpPort()))
        .jvmRoute(workerNode.getId())

    return this
  }

}
