package noe.jk.configure
/**
 * JK Scenario consists of balancers, workers and status-workers
 * Balancer: Balances load between workers which are part of it, see `Balancer#setWorkers`.
 * Worker: The "proxy" workers, facing server passing requests directly to them without load balacing.
 * Status-worker: Provides web-UI for jk-configuration management
 *
 * @link https://tomcat.apache.org/connectors-doc/reference/workers.html
 * @link https://tomcat.apache.org/connectors-doc/reference/status.html
 * @link https://tomcat.apache.org/connectors-doc/common_howto/loadbalancers.html
 *
 * @see FacingServerNode
 * @see BalancerNode
 * @see StatusWorkerNode
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
 *
 */
class JkScenario {

  FacingServerNode facingServerNode
  Map<String, String> additionalUrlMaps = [:]

  // Nodes
  List<BalancerNode> balancerNodes = []
  List<WorkerNode> workerNodes = []
  List<StatusWorkerNode> statusWorkerNodes = []


  /**
   * Returns instance of facing server, like Httpd , ...
   */
  FacingServerNode getFacingServerNode() {
    return facingServerNode
  }

  JkScenario setFacingServerNode(FacingServerNode server) {
    this.facingServerNode = server

    return this
  }

  List<BalancerNode> getBalancerNodes() {
    return balancerNodes
  }

  JkScenario setBalancerNodes(List<BalancerNode> balancers) {
    this.balancerNodes = balancers

    return this
  }

  JkScenario addBalancerNode(BalancerNode balancer) {
    balancerNodes.add(balancer)

    return this
  }

  List<WorkerNode> getWorkerNodes() {
    return workerNodes
  }

  JkScenario addWorkerNode(WorkerNode worker) {
    workerNodes.add(worker)

    return this
  }

  JkScenario setWorkerNodes(List<WorkerNode> worker) {
    workerNodes = worker

    return this
  }

  List<StatusWorkerNode> getStatusWorkerNodes() {
    return statusWorkerNodes
  }

  JkScenario addStatusWorkerNode(StatusWorkerNode statusWorker) {
    statusWorkerNodes.add(statusWorker)

    return this
  }

  JkScenario setStatusWorkerNodes(List<StatusWorkerNode> statusWorkers) {
    this.statusWorkerNodes = statusWorkers

    return this
  }

  /**
   * Returns map of URLs to workers
   *
   * @link https://tomcat.apache.org/connectors-doc/reference/uriworkermap.html
   */
  Map<String, String> getAdditionalUrlMaps() {
    return additionalUrlMaps
  }

  /**
   * For additional mapping rules. For worker specific rules use `Worker.addUrlMap()`
   * Pattern is [url:workerId]
   *
   * @link https://tomcat.apache.org/connectors-doc/reference/uriworkermap.html
   */
  JkScenario addAdditionalUrlMap(String url, String worker) {
    additionalUrlMaps.put(url, worker)

    return this
  }

  /**
   * For additional mapping rules. For worker specific rules use `Worker.addUrlMap()`
   *
   * @link https://tomcat.apache.org/connectors-doc/reference/uriworkermap.html
   */
  JkScenario setAdditionalUrlMaps(Map<String, String> additionalUrlMaps) {
    this.additionalUrlMaps = additionalUrlMaps

    return this
  }

}
