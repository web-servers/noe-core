package noe.jk.configure

/**
 * Operations over connector, like starting, stopping.
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

    jkScenario.getWorkerNodes().each { WorkerNode worker -> worker.getServer().start() }

    jkScenario.getBalancerNodes().each { BalancerNode balancer ->
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

    jkScenario.getWorkerNodes().each { WorkerNode worker -> worker.getServer().stop()}
    jkScenario.getBalancerNodes().each { BalancerNode balancer -> balancer.getWorkers()*.getServer()*.stop() }

    return this
  }

}
