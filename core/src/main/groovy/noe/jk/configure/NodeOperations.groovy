package noe.jk.configure

/**
 * Operations over connector, like starting, stopping
 */
class NodeOperations {

  final JkScenario jkScenario


  NodeOperations(JkScenario jkScenario) {
    this.jkScenario = jkScenario
  }

  /**
   * Starts all nodes (workers and facing servers and balanced workers)
   */
  NodeOperations startAll() {
    if (jkScenario.getFacingServerNode() == null) {
      throw new IllegalStateException('Server has not been set')
    }
    else {
      jkScenario.getFacingServerNode().getServer().start()
    }

    jkScenario.getWorkers().each { WorkerNode worker -> worker.getServer().start() }

    jkScenario.getBalancers().each { BalancerNode balancer ->
      balancer.getWorkers().each { WorkerNode worker ->
        worker.getServer().start()
      }
    }

    return this
  }

  /**
   * Starts all nodes (workers and facing servers and balanced workers)
   */
  NodeOperations stopAll() {
    if (jkScenario.getFacingServerNode() == null) {
      throw new IllegalStateException('Server has not been set')
    }
    else {
      jkScenario.getFacingServerNode().getServer().stop()
    }

    jkScenario.getWorkers().each { WorkerNode worker -> worker.getServer().stop()}
    jkScenario.getBalancers().each { BalancerNode balancer -> balancer.getWorkers()*.getServer()*.stop() }

    return this
  }

}
