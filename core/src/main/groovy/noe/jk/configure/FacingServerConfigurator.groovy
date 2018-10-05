package noe.jk.configure

import groovy.util.logging.Slf4j
import noe.server.ServerAbstract

/**
 * The server acting as a load balancer for the worker server.<br/>
 * Specific FacingServer is given via 2nd parameter `configurator` in constructor.
 *
 * @see DefaultHttpdConfigurator
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
    List<BalancerNode> balancers = jkScenario.getBalancers()
    UriWorkerMapProperties uriWorkerMapProperties = jkScenario.getFacingServerNode().getUriWorkerMapProperties()

    if (uriWorkerMapProperties.getFacingServer() == null) {
      uriWorkerMapProperties.setFacingServer(facingServer)
    }

    uriWorkerMapProperties
        .setBalancers(balancers)
        .setWorkers(jkScenario.getWorkers())
        .setStatusWorkers(jkScenario.getStatusWorkers())
        .setAdditionalUrlMaps(jkScenario.getAdditionalUrlMaps())

    configurators << uriWorkerMapProperties.configure()
  }

  private configureWorkersProperties() {
    ServerAbstract facingServer = jkScenario.getFacingServerNode().getServer()
    List<BalancerNode> balancers = jkScenario.getBalancers()
    WorkersProperties workersProperties = jkScenario.getFacingServerNode().getWorkersProperties()

    if (workersProperties.getFacingServer() == null) {
      workersProperties.setFacingServer(facingServer)
    }

    workersProperties
        .setBalancers(balancers)
        .setWorkers(jkScenario.getWorkers())
        .setStatusWorkers(jkScenario.getStatusWorkers())

    configurators << workersProperties.configure()
  }

  private configureFacingServer() {
    FacingServerNode facingServerNode = jkScenario.getFacingServerNode()

    facingServerNode.getConfigurators().each { Configurator configurator ->
      configurators << configurator.configure()
    }
  }

}
