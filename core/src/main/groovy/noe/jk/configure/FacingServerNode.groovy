package noe.jk.configure

import noe.server.ServerAbstract

/**
 * Represent Facing server in JK scenarios.
 */
class FacingServerNode {

  ServerAbstract server
  UriWorkerMapProperties uriWorkerMapProperties = new UriWorkerMapProperties()
  WorkersProperties workersProperties = new WorkersProperties()
  List<Configurator> configurations = []


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
