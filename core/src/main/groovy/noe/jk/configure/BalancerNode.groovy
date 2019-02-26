package noe.jk.configure

import groovy.util.logging.Slf4j

/**
 * Node responsible for balancing load between workers.
 *
 * @link https://tomcat.apache.org/connectors-doc/reference/workers.html
 * @link https://tomcat.apache.org/connectors-doc/common_howto/loadbalancers.html
 *
 * @see WorkerNode
 * @see JkScenario
 *
 * Example:<br>
 *   <code>
 *     JkScenario scenario = new JkScenario()
 *       .setFacingServerNode(new FacingServerNode(new Httpd(...)))
 *       .addBalancerNode(new BalancerNode()
 *         .addWorker(new WorkerNode(new Tomcat(...)))
 *         .addWorker(new WorkerNode(new Tomcat(...))))
 *
 *     NodeOperations ops =
 *       new JkScenarioConfigurator(
 *         scenario,
 *         DefaultHttpdConfigurator.class,
 *         DefaultAS7WorkerConfigurator.class
 *       ).configure()
 *
 *     ops.startAll()
 *
 *     // ...
 *
 *     ops.stopAll()
 *   </code>
 */
@Slf4j
class BalancerNode {
  protected static final String DEFAULT_ID_PREFIX = 'balancer_'
  private static int numberOfBalancers = 0

  String id
  List<WorkerNode> workers = []
  Boolean stickySession
  List<String> urlsMap = []


  BalancerNode() {
    this("${DEFAULT_ID_PREFIX}${numberOfBalancers}")
  }

  BalancerNode(String id) {
    this.id = id
    numberOfBalancers++
  }

  String getId() {
    return id
  }

  List<WorkerNode> getWorkers() {
    return workers
  }

  BalancerNode addWorker(WorkerNode worker) {
    workers.add(worker)

    return this
  }

  BalancerNode setWorkers(List<WorkerNode> worker) {
    workers = worker

    return this
  }

  List<String> getUrlsMap() {
    return urlsMap
  }

  BalancerNode addUrlMap(String url) {
    this.urlsMap.add(url)

    return this
  }

  BalancerNode setUrlsMap(List<String> urlsMap) {
    this.urlsMap = urlsMap

    return this
  }

  Boolean getStickySession() {
    return stickySession
  }

  BalancerNode setStickySession(Boolean sticky) {
    stickySession = sticky

    return this
  }

}
