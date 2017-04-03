package noe.jk.configure

import noe.server.jk.WorkerServer

/**
 * Represents JK Worker (Tomcat, EAP, ...)
 */
class WorkerNode<T extends WorkerServer> implements JkNode {
  public static String DEFAULT_ID_PREFIX = 'worker_'

  enum Type {
    /**
     * @link https://tomcat.apache.org/connectors-doc/reference/workers.html
     */
    AJP13, AJP14
  }

  String id
  final T server
  Integer lbFactor
  Integer ajpPort
  String host
  final WorkerNode.Type type = WorkerNode.Type.AJP13
  List<String> urlsMap = []
  Integer socketTimeout

  WorkerNode(T server) {
    this(server, "${WorkerNode.DEFAULT_ID_PREFIX}${server.getServerId()}")
  }

  WorkerNode(T server, String id) {
    this.id = id
    this.server = server
  }

  @Override
  String getId() {
    return id
  }

  T getServer() {
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
    if (host != null) return host
    else if (server.getHost() != null && !server.getHost().isEmpty()) return server.getHost()
    else return null
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
