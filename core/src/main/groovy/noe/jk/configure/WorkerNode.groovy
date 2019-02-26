package noe.jk.configure

import noe.server.ServerAbstract
import noe.server.jk.WorkerServer

/**
 * Represent mod_jk worker node abstraction, configured by `WorkersConfigurator`.
 *
 * @link https://tomcat.apache.org/connectors-doc/reference/workers.html
 * @link https://tomcat.apache.org/connectors-doc/common_howto/loadbalancers.html
 *
 * @see WorkerServer
 * @see BalancerNode
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
class WorkerNode {
  public static String DEFAULT_ID_PREFIX = 'worker_'

  enum Type {
    /**
     * @link https://tomcat.apache.org/connectors-doc/reference/workers.html
     */
    AJP13, AJP14
  }

  String id
  final WorkerServer server
  Integer lbFactor
  Integer ajpPort
  String host
  final WorkerNode.Type type = WorkerNode.Type.AJP13
  List<String> urlsMap = []
  Integer socketTimeout

  WorkerNode(WorkerServer server) {
    this(server, "${WorkerNode.DEFAULT_ID_PREFIX}${server.getServerId()}")
  }

  WorkerNode(WorkerServer server, String id) {
    this.id = id
    this.server = server
  }

  String getId() {
    return id
  }

  ServerAbstract getServer() {
    return server
  }

  WorkerNode.Type getType() {
    return type
  }

  Integer getlbFactor() {
    return lbFactor
  }

  WorkerNode setLbFactor(int factor) {
    this.lbFactor = factor

    return this
  }

  Integer getAjpPort() {
    if (ajpPort != null) return ajpPort
    if (server.getAjpPort() > 0) {
      return server.getAjpPort()
    }
    else return null
  }

  WorkerNode setAjpPort(int port) {
    ajpPort = port

    return this
  }

  String getHost() {
    if (host != null) {
      return host
    }
    else if (server.getHost()) {
      return server.getHost()
    }
    else {
      return null
    }
  }

  WorkerNode setHost(String host) {
    this.host = host

    return this
  }

  List<String> getUrlsMap() {
    return urlsMap
  }

  WorkerNode addUrlMap(String url) {
    this.urlsMap.add(url)

    return this
  }

  WorkerNode setUrlsMap(List<String> urlsMap) {
    this.urlsMap = urlsMap

    return this
  }

  Integer getSocketTimeout() {
    return socketTimeout
  }

  WorkerNode setSocketTimeout(Integer socketTimeout) {
    this.socketTimeout = socketTimeout

    return this
  }


}
