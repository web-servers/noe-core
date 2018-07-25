package noe.jk.configure

import groovy.util.logging.Slf4j
import noe.common.utils.JBFile
import noe.common.utils.Platform
import noe.server.ServerAbstract

/**
 * uriworkersmap.properties configuration file builder.
 *
 * @link https://tomcat.apache.org/connectors-doc/reference/uriworkermap.html
 */
@Slf4j
class UriWorkerMapProperties implements Configurator {
  public static final String DEFAULT_NAME = 'uriworkermap.properties'
  String fileName = DEFAULT_NAME

  ServerAbstract facingServer

  List<BalancerNode> balancers = []
  List<WorkerNode> workers = []
  List<StatusWorkerNode> statusWorkers = []

  Map<String, String> additionalUrlMaps = [:]

  DefaultEmptyUriWorkerMapProperties defaultEmptyUriWorkerMapProperties = new DefaultEmptyUriWorkerMapProperties()


  String getFileName() {
    return fileName
  }

  UriWorkerMapProperties setFileName(String fileName) {
    this.fileName = fileName

    return this
  }

  List<BalancerNode> getBalancers() {
    return balancers
  }

  UriWorkerMapProperties setBalancers(List<BalancerNode> balancers) {
    this.balancers = balancers

    return this
  }

  List<WorkerNode> getWorkers() {
    return workers
  }

  UriWorkerMapProperties setWorkers(List<WorkerNode> workers) {
    this.workers = workers

    return this
  }

  List<StatusWorkerNode> getStatusWorkers() {
    return statusWorkers
  }

  UriWorkerMapProperties setStatusWorkers(List<StatusWorkerNode> statusWorkers) {
    this.statusWorkers = statusWorkers

    return this
  }

  Map<String, String> getAdditionalUrlMaps() {
    return additionalUrlMaps
  }

  UriWorkerMapProperties setAdditionalUrlMaps(Map<String, String> extraUrlMapping) {
    this.additionalUrlMaps = extraUrlMapping

    return this
  }

  @Override
  Configurator configure() {
    JBFile.createFile(new File(retrieveFacingServerConfDeploymentPath(), fileName), content())

    return this
  }

  private String content() {
    StringBuilder content = new StringBuilder()

    workersContent(content)
    balancersContent(content)
    additionalUrlMapContent(content)
    statusWorkerContent(content)
    defaultContentIfWouldBeEmpty(content)

    log.debug "${fileName}:"
    log.debug content.toString()

    return content.toString()
  }

  private void workersContent(StringBuilder content) {
    getWorkers().each { WorkerNode worker ->
      worker.getUrlsMap().each { String url ->
        content.append("${url}=${worker.getId()}" + new Platform().nl)
      }
    }
  }

  private void balancersContent(StringBuilder content) {
    getBalancers().each { BalancerNode balancer ->
      balancer.getUrlsMap().each { String url ->
        content.append("${url}=${balancer.getId()}" + new Platform().nl)
      }
    }
  }

  private void additionalUrlMapContent(StringBuilder content) {
    getAdditionalUrlMaps().each { String url, String workerName ->
      content.append("${url}=${workerName}" + new Platform().nl)
    }
  }

  private void statusWorkerContent(StringBuilder content) {
    getStatusWorkers().each { StatusWorkerNode statusWorker ->
      statusWorker.getUrlsMap().each { String url ->
        content.append("${url}=${statusWorker.getId()}" + new Platform().nl)
      }
    }
  }

  private void defaultContentIfWouldBeEmpty(StringBuilder content) {
    if (content.toString().isEmpty()) {
      content.append(defaultEmptyUriWorkerMapProperties.content())
    }
  }

  private File retrieveFacingServerConfDeploymentPath() {
    return new File(getFacingServer().getConfDeploymentPath())
  }

  class DefaultEmptyUriWorkerMapProperties implements EmptyUriWorkerMapProperties {
    String content() {
      StringBuilder content = new StringBuilder()
      boolean defaultSet = false

      contentBalancers(defaultSet, content)
      contentWorkers(defaultSet, content)
      contentStatusWorkers(content)

      if (content.toString().isEmpty()) {
        log.debug("No balaner nor worker specified, no url mapping specified.")
      }

      content.append(new Platform().nl)

      return content.toString()
    }

    private contentWorkers(boolean defaultSet, StringBuilder content) {
      getWorkers().each { WorkerNode worker ->
        if (defaultSet) {
          log.warn("Mapping already set, ignoring all other nodes because it is not possible to resolve how to set url mapping. Please set url mapping for specified nodes explicitly.")
        } else {
          content.append("/*=${worker.getId()}${new Platform().nl}")
          defaultSet = true
        }
      }
    }

    private contentBalancers(boolean defaultSet, StringBuilder content) {
      getBalancers().each { BalancerNode balancer ->
        if (!balancer.getWorkers().isEmpty()) {
          if (defaultSet) {
            log.warn("Mapping already set, ignoring all other balancers because it is not possible to resolve how to set url mapping. Please set url mapping for specified nodes explicitly.")
          } else {
            content.append("/*=${balancer.getId()}${new Platform().nl}")
            defaultSet = true
          }
        }
      }
    }

    private contentStatusWorkers(StringBuilder content) {
      boolean defaultSet = false

      getStatusWorkers().each { StatusWorkerNode statusWorker ->
        if (!statusWorkers.isEmpty()) {
          if (defaultSet) {
            log.warn("Mapping already set, ignoring all other status workers because it is not possible to resolve how to set url mapping. Please set url mapping for specified nodes explicitly.")
          } else {
            content.append("${StatusWorkerNode.DEFAULT_URL}=${statusWorker.getId()}")
            defaultSet = true
          }
        }
      }
    }
  }

  interface EmptyUriWorkerMapProperties {
    String content()
  }


}
