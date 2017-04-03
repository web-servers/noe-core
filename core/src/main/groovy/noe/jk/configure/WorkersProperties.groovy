package noe.jk.configure

import groovy.util.logging.Slf4j
import noe.common.utils.JBFile
import noe.common.utils.Platform
import noe.server.ServerAbstract

/**
 * Workers.properties configuration file builder.
 *
 * Balancer: Balances load between workers which are part of it, see `Balancer#setWorkers`.
 * Worker: The "proxy" workers, facing server passing requests directly to them without load balacing.
 * Status-worker: Provides web-UI for jk-configuration management
 *
 * IMPORTANT
 * <ul>
 *   <li>Not all directives are supported.</li>
 *   <li>New configuration file is created always (old is overwritten).</li>
 * </ul>
 *
 * @link https://tomcat.apache.org/connectors-doc/reference/workers.html
 */
@Slf4j
class WorkersProperties implements Configurator {
  public static final String DEFAULT_NAME = 'workers.properties'
  String fileName = DEFAULT_NAME

  ServerAbstract facingServer

  List<BalancerNode> balancers = []
  List<WorkerNode> workers = []
  List<StatusWorkerNode> statusWorkers = []


  String getFileName() {
    return fileName
  }

  WorkersProperties setFileName(String fileName) {
    this.fileName = fileName

    return this
  }

  List<BalancerNode> getBalancers() {
    return balancers
  }

  WorkersProperties setBalancers(List<BalancerNode> balancers) {
    this.balancers = balancers

    return this
  }

  List<WorkerNode> getWorkers() {
    return workers
  }

  WorkersProperties setWorkers(List<WorkerNode> workers) {
    this.workers = workers

    return this
  }

  List<StatusWorkerNode> getStatusWorkers() {
    return statusWorkers
  }

  WorkersProperties setStatusWorkers(List<StatusWorkerNode> statusWorkers) {
    this.statusWorkers = statusWorkers

    return this
  }

  @Override
  WorkersProperties configure() {
    JBFile.createFile(new File(retrieveFacingServerConfDeploymentPath(), fileName), content())

    return this
  }

  private String content() {
    StringBuilder content = new StringBuilder()

    prepareWorkersList(content)
    prepareWorkers(content)
    prepareBalancers(content)
    prepareStatusWorker(content)

    log.debug "${fileName}:"
    log.debug "------------"
    log.debug content.toString()
    log.debug "------------"

    return content.toString()
  }

  private void prepareWorkersList(StringBuilder content) {
    String nl = new Platform().nl
    List<String> workerList = []

    workerList.addAll(getBalancerIds())
    workerList.addAll(getWorkerIds())
    workerList.addAll(getStatusWorkersIds())

    content.append(nl)
    content.append("worker.list=${workerList.join(',')}" + nl)
  }

  private List<String> getBalancerIds() {
    return getBalancers().collect { BalancerNode balancer -> balancer.getId() }
  }

  private List<String> getWorkerIds() {
    return getWorkers().collect { WorkerNode worker -> worker.getId() }
  }

  private List<String> getStatusWorkersIds() {
    return getStatusWorkers().collect { StatusWorkerNode worker -> worker.getId() }
  }

  private void prepareWorkers(StringBuilder content) {
    getWorkers().each { WorkerNode worker -> prepareWorker(content, worker) }
  }

  private void prepareWorker(StringBuilder content, WorkerNode worker) {
    String nl = new Platform().nl

    content.append(nl)
    content.append("worker.${worker.getId()}.type=${transformWorkerType(worker.getType())}" + nl)

    if (worker.getHost() != null) content.append("worker.${worker.getId()}.host=${worker.getHost()}" + nl)
    if (worker.getAjpPort() != null) content.append("worker.${worker.getId()}.port=${worker.getAjpPort()}" + nl)
    if (worker.getlbFactor() != null) content.append("worker.${worker.getId()}.lbfactor=${worker.getlbFactor()}" + nl)
    if (worker.getSocketTimeout() != null) content.append("worker.${worker.getId()}.socket_timeout=${worker.getSocketTimeout()}" + nl)
  }

  private String transformWorkerType(WorkerNode.Type type) {
    switch (type) {
      case WorkerNode.Type.AJP13:
        return "ajp13"
      case WorkerNode.Type.AJP14:
        return "ajp14"
    }
  }

  private void prepareBalancers(StringBuilder content) {
    String nl = new Platform().nl

    getBalancers().each { BalancerNode balancer ->
      content.append(nl)
      content.append("worker.${balancer.getId()}.type=lb" + nl)
      content.append("worker.${balancer.getId()}.balance_workers=${getBalancedWorkerIds(balancer).join(',')}" + nl)
      if (balancer.getStickySession() != null) content.append("worker.${balancer.getId()}.sticky_session=${balancer.getStickySession()}" + nl)

      balancer.getWorkers().each { WorkerNode worker -> prepareWorker(content, worker) }
    }
  }

  private List<String> getBalancedWorkerIds(BalancerNode balancer) {
    return balancer.workers.collect { WorkerNode worker -> worker.getId() }
  }

  private prepareStatusWorker(StringBuilder content) {
    String nl = new Platform().nl

    getStatusWorkers().each { StatusWorkerNode statusWorker ->
      content.append(nl)
      content.append("worker.${statusWorker.getId()}.type=status" + nl)
      if (statusWorker.getCss() != null) content.append("worker.${statusWorker.getId()}.css=${statusWorker.getCss()}" + nl)
      if (statusWorker.getReadOnly() != null) content.append("worker.${statusWorker.getId()}.read_only=${statusWorker.getReadOnly()}" + nl)
    }
  }

  private File retrieveFacingServerConfDeploymentPath() {
    return new File(getFacingServer().getConfDeploymentPath())
  }

}
