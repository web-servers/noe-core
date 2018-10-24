package noe.jk.configure

import noe.server.ServerAbstract

/**
 * Represent mod_jk facing server abstraction, configured by `FacingServerConfigurator`.
 *
 * @see FacingServerConfigurator
 *
 * Example:<br>
 *   <code>
 *     JkScenario scenario = new JkScenario()
 *       .setFacingServerNode(new FacingServerNode(new Httpd()))
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
class FacingServerNode {

  ServerAbstract server
  UriWorkerMapProperties uriWorkerMapProperties = new UriWorkerMapProperties()
  WorkersProperties workersProperties = new WorkersProperties()
  List<Configurator> configurations = []


  FacingServerNode(ServerAbstract server) {
    this.server = server
  }

  ServerAbstract getServer() {
    return server
  }

  FacingServerNode setServer(ServerAbstract server) {
    this.server = server

    return this
  }

  UriWorkerMapProperties getUriWorkerMapProperties() {
    return uriWorkerMapProperties
  }

  JkScenario setUriWorkerMapProperties(UriWorkerMapProperties uriWorkerMapProperties) {
    this.uriWorkerMapProperties = uriWorkerMapProperties

    return this
  }

  WorkersProperties getWorkersProperties() {
    return workersProperties
  }

  JkScenario setWorkersProperties(WorkersProperties workersProperties) {
    this.workersProperties = workersProperties

    return this
  }

  List<Configurator> getConfigurators() {
    return configurations
  }

  FacingServerNode setConfigurations(List<Configurator> configurations) {
    this.configurations = configurations

    return this
  }

  FacingServerNode addConfigurations(Configurator configuration) {
    this.configurations << configuration

    return this
  }
}
