package noe.jk.configure

import noe.tomcat.configure.AjpConnectorTomcat
import noe.tomcat.configure.NonSecureHttpConnectorTomcat
import noe.tomcat.configure.TomcatConfigurator

/**
 * Configure Tomcat server to be a mod_jk workers. Prepares JVM route and connectors.
 *
 * @see WorkerNode
 * @see Configurator
 *
 * Example:<br>
 *   <code>
 *     JkScenario scenario = new JkScenario()
 *       .setFacingServerNode(new FacingServerNode(new Httpd(...)))
 *       .addBalancerNode(new BalancerNode()
 *         .addWorker(new WorkerNode(new Tomcat(...)))
 *         .addWorker(new WorkerNode(new Tomcat(...))))
 *
 *     NodeOperations ops =
 *       new JkScenarioConfigurator(
 *         scenario,
 *         DefaultHttpdConfigurator.class,
 *         DefaultTomcatWorkerConfigurator.class
 *       ).configure()
 *
 *     ops.startAll()
 *
 *     // ...
 *
 *     ops.stopAll()
 *   </code>
 */
class DefaultTomcatWorkerConfigurator implements Configurator<DefaultTomcatWorkerConfigurator> {

  WorkerNode workerNode
  TomcatConfigurator configurator


  DefaultTomcatWorkerConfigurator(WorkerNode workerNode) {
    this.workerNode = workerNode
    this.configurator = new TomcatConfigurator(workerNode.getServer())
  }

  @Override
  DefaultTomcatWorkerConfigurator configure() {
    configurator
        .httpConnector(new NonSecureHttpConnectorTomcat().setAddress(workerNode.getHost()))
        .ajpConnector(new AjpConnectorTomcat().setPort(workerNode.getAjpPort()))
        .jvmRoute(workerNode.getId())

    return this
  }

  @Override
  DefaultTomcatWorkerConfigurator revertAll() {
    configurator.revertAllConfiguration()

    return this
  }
}
