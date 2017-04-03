package noe.jk.configure

import groovy.util.logging.Slf4j

/**
 * Jk node responsible for balancing load between workers.
 *
 * @link https://tomcat.apache.org/connectors-doc/reference/workers.html
 * @link https://tomcat.apache.org/connectors-doc/common_howto/loadbalancers.html
 */
@Slf4j
class BalancerNode implements JkNode {
  public static final String DEFAULT_ID_PREFIX = 'balancer_'
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

  @Override
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

  @Override
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
