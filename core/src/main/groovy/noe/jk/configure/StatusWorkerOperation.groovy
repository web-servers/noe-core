package noe.jk.configure

import groovy.util.logging.Slf4j
import noe.common.utils.Library

/**
 * Abstration for commands manipulating a status worker.
 *
 * @link https://tomcat.apache.org/connectors-doc/reference/status.html
 *
 * Example: List balancer summary in text format
 * <code>
 *     new StatusWorkerOperation()
 *         .setAction(new StatusWorkerOperation.List())
 *         .setOutputFormat(StatusWorkerOperation.OutputFormat.TEXT)
 *         .setBalancerId(balancerId)
 *         .setHost(facingServer.getHost())
 *         .setPort(facingServer.getMainHttpPort())
 *         .buildAndExecute()
 * </code>
 */
@Slf4j
class StatusWorkerOperation {

  /**
   * Chapter: Usage Patterns / Actions
   * @link https://tomcat.apache.org/connectors-doc/reference/status.html
   */
  interface Action {
    String build()
  }

  static class List implements Action {
    @Override
    String build() {
      return 'list'
    }
  }

  static class Show implements Action {
    @Override
    String build() {
      return 'show'
    }
  }

  static class Reset implements Action {
    @Override
    String build() {
      return 'reset'
    }
  }

  static class Recover implements Action {
    @Override
    String build() {
      return 'recover'
    }
  }

  static class Version implements Action {
    @Override
    String build() {
      return 'version'
    }
  }

  static class Dump implements Action {
    @Override
    String build() {
      return 'dump'
    }
  }

  static class Update implements Action {
    Map<String, String> params = [:]

    @Override
    String build() {
      StringBuilder action = new StringBuilder("update")

      params.each {String name, String value ->
        action.append("&${name.toLowerCase()}=${value}")
      }

      return action.toString()
    }

    /**
     * Chapter: Request Parameters / Data Parameters for the standard Update Action
     * @link https://tomcat.apache.org/connectors-doc/reference/status.html
     */
    Update addParameter(String name, String value) {
      params.put(name, value)

      return this
    }

  }

  /**
   * Chapter: Usage Patterns / Output Format
   * @link https://tomcat.apache.org/connectors-doc/reference/status.html
   */
  enum OutputFormat { HTML, XML, PROPERTIES, TEXT }

  Action action
  OutputFormat outputFormat = OutputFormat.PROPERTIES
  String host
  String urlPath
  int port
  String balancerId
  String workerId
  Integer automaticRefresh
  String options
  String lastResult

  private String command


  Action getAction() {
    return action
  }

  StatusWorkerOperation setAction(Action action) {
    this.action = action

    return this
  }

  OutputFormat getOutputFormat() {
    return outputFormat
  }

  StatusWorkerOperation setOutputFormat(OutputFormat outputFormat) {
    this.outputFormat = outputFormat

    return this
  }

  String getHost() {
    return host
  }

  StatusWorkerOperation setHost(String host) {
    this.host = host

    return this
  }

  String getUrlPath() {
    return urlPath ?: StatusWorkerNode.DEFAULT_URL
  }

  StatusWorkerOperation setUrlPath(String urlPath) {
    this.urlPath = urlPath

    return this
  }

  int getPort() {
    return port
  }

  StatusWorkerOperation setPort(int port) {
    this.port = port

    return this
  }

  String getBalancerId() {
    return balancerId
  }

  StatusWorkerOperation setBalancerId(String balancerId) {
    this.balancerId = balancerId

    return this
  }

  String getWorkerId() {
    return workerId
  }

  StatusWorkerOperation setWorkerId(String workerId) {
    this.workerId = workerId

    return this
  }

  Integer getAutomaticRefresh() {
    return automaticRefresh
  }

  StatusWorkerOperation setAutomaticRefresh(Integer automaticRefresh) {
    this.automaticRefresh = automaticRefresh

    return this
  }

  String getOptions() {
    return options
  }

  StatusWorkerOperation setOptions(String opt) {
    this.options = opt

    return this
  }

  String getLastResult() {
    return lastResult
  }

  private StatusWorkerOperation setLastResult(String lastResult) {
    this.lastResult = lastResult

    return this
  }

  StatusWorkerOperation build() {
    StringBuilder c = new StringBuilder("?")

    if (action != null) {
      c.append("cmd=${action.build()}&")
    }
    if (outputFormat != null) {
      c.append("mime=${transformOutputType(outputFormat)}&")
    }

    if (balancerId != null) {
      c.append("w=${balancerId}&")
      if (workerId != null) {
        c.append("sw=${workerId}&")
      }
    } else if (workerId != null) {
      c.append("w=${workerId}&")
    }

    if (automaticRefresh != null) {
      c.append("re=${automaticRefresh}&")
    }

    if (options != null) {
      c.append("opt=${options}&")
    }

    log.debug("Generated command: $c")

    command = c.toString()

    return this
  }

  StatusWorkerOperation buildAndExecute() {
    build()
    lastResult = Library.retrieveURLContent(new URL("http", host, port, getUrlPath() + command))

    return this
  }

  private String transformOutputType(OutputFormat format) {
    switch (format){
      case OutputFormat.TEXT:
        return 'txt'

      case OutputFormat.XML:
        return 'xml'

      case OutputFormat.PROPERTIES:
        return 'prop'

      case OutputFormat.HTML:
        return 'html'
    }
  }

}
