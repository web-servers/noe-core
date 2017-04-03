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
 */
class JkScenario {

  FacingServerNode facingServerNode
  Map<String, String> additionalUrlMaps = [:]

  // Nodes
  List<BalancerNode> balancers = []
  List<WorkerNode> workers = []
  List<StatusWorkerNode> statusWorkers = []


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

  List<BalancerNode> getBalancers() {
    return balancers
  }

  JkScenario setBalancers(List<BalancerNode> balancers) {
    this.balancers = balancers

    return this
  }

  JkScenario addBalancer(BalancerNode balancer) {
    balancers.add(balancer)

    return this
  }

  List<WorkerNode> getWorkers() {
    return workers
  }

  JkScenario addWorker(WorkerNode worker) {
    workers.add(worker)

    return this
  }

  JkScenario setWorkers(List<WorkerNode> worker) {
    workers = worker

    return this
  }

  List<StatusWorkerNode> getStatusWorkers() {
    return statusWorkers
  }

  JkScenario addStatusWorker(StatusWorkerNode statusWorker) {
    statusWorkers.add(statusWorker)

    return this
  }

  JkScenario setStatusWorkers(List<StatusWorkerNode> statusWorkers) {
    this.statusWorkers = statusWorkers

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
