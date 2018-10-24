package noe.jk.configure

import groovy.util.logging.Slf4j
import noe.server.ServerAbstract

/**
 * Common configuration for all types of balancers (Httpd, IIS, iPlanet).
 * Prepares `uriworkermap.conf` and `workers.properties`. And let the server perform
 * it's specific mod_jk configuration.
 *
 * Specific facing server is given via 2nd parameter `configurator` in constructor.
 *
 * @see DefaultHttpdConfigurator
 * @see DefaultAS7WorkerConfigurator
 */
@Slf4j
class FacingServerConfigurator implements Configurator<FacingServerConfigurator> {

  JkScenario jkScenario
  Class<? extends Configurator> configurator
  List<Configurator> configurators = []


  FacingServerConfigurator(JkScenario jkScenario, Class<? extends Configurator> configurator) {
    this.jkScenario = jkScenario
    this.configurator = configurator
  }

  @Override
  FacingServerConfigurator configure() {
    if (jkScenario?.getFacingServerNode()?.getServer() == null) {
      throw new IllegalStateException("Server was not set. Connector cannot be configured.")
    }

    configurators << configurator
      .newInstance(jkScenario.getFacingServerNode())
      .configure()

    configureUriWorkerMap()
    configureWorkersProperties()
    configureFacingServer()

    return this
  }

  @Override
  FacingServerConfigurator revertAll() {
    configurators.each { Configurator c -> c.revertAll() }

    return this
  }

  private configureUriWorkerMap() {
    ServerAbstract facingServer = jkScenario.getFacingServerNode().getServer()
    List<BalancerNode> balancers = jkScenario.getBalancerNodes()
    UriWorkerMapProperties uriWorkerMapProperties = jkScenario.getFacingServerNode().getUriWorkerMapProperties()

    if (uriWorkerMapProperties.getFacingServer() == null) {
      uriWorkerMapProperties.setFacingServer(facingServer)
    }

    uriWorkerMapProperties
        .setBalancers(balancers)
        .setWorkers(jkScenario.getWorkerNodes())
        .setStatusWorkers(jkScenario.getStatusWorkerNodes())
        .setAdditionalUrlMaps(jkScenario.getAdditionalUrlMaps())

    configurators << uriWorkerMapProperties.configure()
  }

  private configureWorkersProperties() {
    ServerAbstract facingServer = jkScenario.getFacingServerNode().getServer()
    List<BalancerNode> balancers = jkScenario.getBalancerNodes()
    WorkersProperties workersProperties = jkScenario.getFacingServerNode().getWorkersProperties()

    if (workersProperties.getFacingServer() == null) {
      workersProperties.setFacingServer(facingServer)
    }

    workersProperties
        .setBalancers(balancers)
        .setWorkers(jkScenario.getWorkerNodes())
        .setStatusWorkers(jkScenario.getStatusWorkerNodes())

    configurators << workersProperties.configure()
  }

  private configureFacingServer() {
    FacingServerNode facingServerNode = jkScenario.getFacingServerNode()

    facingServerNode.getConfigurators().each { Configurator configurator ->
      configurators << configurator.configure()
    }
  }

}
