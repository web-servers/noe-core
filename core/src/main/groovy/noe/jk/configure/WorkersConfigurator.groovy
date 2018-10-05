package noe.jk.configure

import groovy.util.logging.Slf4j

/**
 * Configure workers in JK scenario
 */
@Slf4j
class WorkersConfigurator implements Configurator<WorkersConfigurator> {

  JkScenario jkScenario
  Class<? extends Configurator> configurator
  List<Configurator> configurators = []


  WorkersConfigurator(JkScenario jkScenario, Class<? extends Configurator> configurator) {
    this.jkScenario = jkScenario
    this.configurator = configurator
  }

  @Override
  WorkersConfigurator configure() {
    configureBalancedWorkers()
    configureNonBalancedWorkers()

    return this
  }

  @Override
  WorkersConfigurator revertAll() {
    configurators.each { Configurator c -> c.revertAll() }

    return this
  }

  private configureBalancedWorkers() {
    if (jkScenario.getBalancers().isEmpty()) {
      log.debug("No balancers has been specified, continuing ...")
    } else {
      jkScenario.getBalancers().each { BalancerNode balancer ->
        balancer.getWorkers().each { WorkerNode worker ->
            configurators << configurator
                .newInstance(worker)
                .configure()
        }
      }
    }
  }

  private configureNonBalancedWorkers() {
    if (jkScenario.getWorkers().isEmpty()) {
      log.debug("No workers has been specified, continuing ...")
    } else {
      jkScenario.getWorkers().each { WorkerNode worker ->
          configurators << configurator
              .newInstance(worker)
              .configure()
      }
    }
  }

}
