package noe.jk.configure

/**
 * JK status worker.
 *
 * IMPORTANT: Not all directives are supported
 *
 * @link https://tomcat.apache.org/connectors-doc/reference/workers.html (section status Worker Directives)
 */
class StatusWorkerNode implements JkNode {
  public static final String DEFAULT_ID_PREFIX = 'status_'
  public static final String DEFAULT_URL = '/status'
  private static int numberOfStatusWorkers = 0

  final String id
  String css
  Boolean readOnly

  List<StatusWorkerNode> urlsMap = []


  StatusWorkerNode() {
    this("${DEFAULT_ID_PREFIX}${numberOfStatusWorkers}")
  }

  StatusWorkerNode(String id) {
    this.id = id
    numberOfStatusWorkers++
  }

  @Override
  String getId() {
    return id
  }

  @Override
  List<String> getUrlsMap() {
    return urlsMap
  }

  StatusWorkerNode addUrlMap(String url) {
    this.urlsMap.add(url)

    return this
  }

  StatusWorkerNode setUrlsMap(List<String> urlsMap) {
    this.urlsMap = urlsMap

    return this
  }

  String getCss() {
    return css
  }

  StatusWorkerNode setCss(String css) {
    this.css = css

    return this
  }

  Boolean getReadOnly() {
    return readOnly
  }

  StatusWorkerNode setReadOnly(Boolean readOnly) {
    this.readOnly = readOnly

    return this
  }
}
