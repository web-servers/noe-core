package noe.jk.configure

import groovy.util.logging.Slf4j
import noe.server.ServerAbstract

/**
 * Configure facing server in JK scenarios.<br/>
 * Specific FacingServer is given via 2nd paramenter `configurator` in constructor.
 *
 * @see DefaultHttpdConfigurator
 */
@Slf4j
class FacingServerConfigurator implements Configurator<FacingServerConfigurator> {

  JkScenario jkScenario
  Class<? extends Configurator> configurator


  FacingServerConfigurator(JkScenario jkScenario, Class<? extends Configurator> configurator) {
    this.jkScenario = jkScenario
    this.configurator = configurator
  }

  @Override
  FacingServerConfigurator configure() {
    if (jkScenario?.getFacingServerNode()?.getServer() == null) {
      throw new IllegalStateException("Server was not set. Connector can not be configured.")
    }

    configurator.newInstance(jkScenario.getFacingServerNode()).configure()

    configureUriWorkersmap()
    configureWorkersProperties()
    configureFacingServer()

    return this
  }

  private configureUriWorkersmap() {
    ServerAbstract facingServer = jkScenario.getFacingServerNode().getServer()
    List<BalancerNode> balancers = jkScenario.getBalancers()
    UriWorkerMapProperties uriWorkerMapProperties = jkScenario.getFacingServerNode().getUriWorkerMapProperties()

    if (uriWorkerMapProperties.getFacingServer() == null) uriWorkerMapProperties.setFacingServer(facingServer)
    uriWorkerMapProperties
        .setBalancers(balancers)
        .setWorkers(jkScenario.getWorkers())
        .setStatusWorkers(jkScenario.getStatusWorkers())
        .setAdditionalUrlMaps(jkScenario.getAdditionalUrlMaps())

    uriWorkerMapProperties.configure()
  }

  private configureWorkersProperties() {
    ServerAbstract facingServer = jkScenario.getFacingServerNode().getServer()
    List<BalancerNode> balancers = jkScenario.getBalancers()
    WorkersProperties workersProperties = jkScenario.getFacingServerNode().getWorkersProperties()

    if (workersProperties.getFacingServer() == null) workersProperties.setFacingServer(facingServer)
    workersProperties
        .setBalancers(balancers)
        .setWorkers(jkScenario.getWorkers())
        .setStatusWorkers(jkScenario.getStatusWorkers())

    workersProperties.configure()
  }

  private configureFacingServer() {
    FacingServerNode facingServerNode = jkScenario.getFacingServerNode()

    facingServerNode.getConfigurators().each { Configurator configurator ->
      configurator.configure()
    }
  }

}
