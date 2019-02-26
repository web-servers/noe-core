package noe.jk.configure

/**
 * Configurator helper class for JK scenario preparation.
 */
class JkScenarioConfigurator implements Configurator<JkScenarioConfigurator> {

  FacingServerConfigurator facingServerConfigurator
  WorkersConfigurator workersConfigurator


  JkScenarioConfigurator(
    JkScenario scenario,
    Class<? extends Configurator> facingServerConfiguratorClass,
    Class<? extends Configurator> workersConfiguratorClass)
  {
    this.facingServerConfigurator = new FacingServerConfigurator(scenario, facingServerConfiguratorClass)
    this.workersConfigurator = new WorkersConfigurator(scenario, workersConfiguratorClass)
  }

  @Override
  JkScenarioConfigurator configure() {
    facingServerConfigurator.configure()
    workersConfigurator.configure()

    return this
  }

  @Override
  JkScenarioConfigurator revertAll() {
    facingServerConfigurator.revertAll()
    workersConfigurator.revertAll()

    return this
  }
}
