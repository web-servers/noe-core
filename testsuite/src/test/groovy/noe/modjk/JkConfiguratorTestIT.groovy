package noe.modjk

import groovy.util.logging.Slf4j
import noe.common.TestAbstract
import noe.common.utils.JBFile
import noe.common.utils.Platform
import noe.common.utils.VerifyURLBuilder
import noe.eap.workspace.WorkspaceMultipleHttpdAS7
import noe.ews.workspace.WorkspaceHttpdTomcats
import noe.jk.configure.DefaultAS7WorkerConfigurator
import noe.jk.configure.DefaultHttpdConfigurator
import noe.jk.configure.FacingServerNode
import noe.jk.configure.JkScenario
import noe.jk.configure.FacingServerConfigurator
import noe.jk.configure.JkScenarioConfigurator
import noe.jk.configure.NodeOperations
import noe.jk.configure.StatusWorkerNode
import noe.jk.configure.DefaultTomcatWorkerConfigurator
import noe.jk.configure.StatusWorkerOperation
import noe.jk.configure.WorkerNode
import noe.jk.configure.BalancerNode
import noe.jk.configure.modjk.ModJkConf
import noe.jk.configure.UriWorkerMapProperties
import noe.jk.configure.WorkersProperties
import noe.server.AS7
import noe.server.Httpd
import noe.server.ServerAbstract
import noe.server.Tomcat
import noe.server.jk.WorkerServer
import noe.workspace.IWorkspace
import noe.workspace.ServersWorkspace
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import static groovy.test.GroovyAssert.shouldFail
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue
import static org.junit.Assume.assumeTrue

@Slf4j
@RunWith(Parameterized.class)
class JkConfiguratorTestIT extends TestAbstract {
  static List<String> runOncePerParameterSet = []

  JkScenarioConfigurator configurator
  JkClassInstancesCreator np
  boolean executeTest

  FacingServerNode facingServerNode
  ServerAbstract facingServer
  WorkerServer workerServer1
  WorkerServer workerServer2
  WorkerServer workerServer3
  WorkerServer workerServer4


  @Parameterized.Parameters
  static Collection<Object[]> data() {
    Platform platform = new Platform()
    boolean modJkPlatforms =
            (platform.isRHEL() && (platform.isX64())) ||
            (platform.isSolaris11() && platform.isX64()) ||
            (platform.isWindows() && platform.isX64())

    [
      [Httpd.class, Tomcat.class, modJkPlatforms],
//      [Httpd.class, AS7.class, modJkPlatforms]
    ].collect { it as Object[] }

  }

  JkConfiguratorTestIT(Class <? extends ServerAbstract > facingServerClass, Class workerServerClass, boolean executeTest) {
    this.np = new JkClassInstancesCreator(facingServerClass, workerServerClass)
    this.executeTest = executeTest
  }

  @Before
  void before() {
    String key = np.getFacingServerClass().toString() + np.getWorkerServerClass().toString()

    // Create workspace (for example: httpd/mod_jk + Tomcat) once and execute all test over it
    // For other set of parametres (for example httpd/mod_jk + AS7) recreate workspace again
    if (!runOncePerParameterSet.contains(key)) {
      prepareWorkspace()
      runOncePerParameterSet << key
    }

    prepareFacingServerAndWorkers()

    // Clean the environment
    super.prepare()
  }

  private prepareWorkspace() {
    loadTestProperties("/${np.retrievePropertiesFileName()}")
    assumeTrue("The test is not supported on this platform", executeTest)
    workspace = new ServersWorkspace(
        np.retrieveWorkspaceInstance()
    )
    workspace.prepare()

    np.prepareFacingServer()
  }

  @After
  void after() {
    configurator?.revertAll()
  }

  @Test
  void testDefaultSimpleProxy() {
    JkScenario scenario = new JkScenario()
      .setFacingServerNode(facingServerNode)
      .addWorkerNode(new WorkerNode(workerServer1))

    NodeOperations ops = configureFacingServerAndWorkers(scenario)

    ops.startAll()

    testModJkLoaded(facingServer)
    testDefaulProxytUriWorkersMap(facingServer)
    testDefaultProxyWorkersProperties(facingServer)
    testDefaultMojkConf(facingServer)
    httpdLogFileExists(facingServer, /jk.shm.*/)
    httpdDisplaysWorkerResponce(facingServer)

    ops.stopAll()
  }

  @Test
  void testDefaultSimpleBalancing() {
    JkScenario scenario = new JkScenario()
      .setFacingServerNode(facingServerNode)
      .addBalancerNode(new BalancerNode()
        .addWorker(new WorkerNode(workerServer1))
        .addWorker(new WorkerNode(workerServer2)))

    NodeOperations ops = configureFacingServerAndWorkers(scenario)

    ops.startAll()

    testModJkLoaded(facingServer)
    testDefaulBalancingtUriWorkersMap(facingServer)
    testDefaulBalancingtWorkersProperties(facingServer)
    testDefaultMojkConf(facingServer)
    httpdLogFileExists(facingServer, /jk.shm.*/)
    httpdDisplaysWorkerResponce(facingServer)

    ops.stopAll()
  }

  @Test
  void testDefaultSimpleBalancingWithStatusWorker() {
    JkScenario scenario = new JkScenario()
        .setFacingServerNode(facingServerNode)
        .addBalancerNode(new BalancerNode()
          .addWorker(new WorkerNode(workerServer1))
          .addWorker(new WorkerNode(workerServer2)))
        .addStatusWorkerNode(new StatusWorkerNode())

    NodeOperations ops = configureFacingServerAndWorkers(scenario)

    ops.startAll()

    testModJkLoaded(facingServer)
    testBalancingWithStatusWorkerUriWorkersMap(facingServer)
    testBalancingWithStatusWorkerWorkersProperties(facingServer)
    testDefaultMojkConf(facingServer)
    httpdLogFileExists(facingServer, /jk.shm.*/)
    httpdDisplaysWorkerResponce(facingServer)

    ops.stopAll()
  }

  @Test
  void testComplexJkConfiguration() {
    JkScenario scenario = new JkScenario()
      .setFacingServerNode(facingServerNode
        .addConfigurations(new ModJkConf()
          .setHttpd(facingServer)
          .setLogLevel('debug')
          .setAdditionalLines(['# This is just test comment'])))
      .addWorkerNode(new WorkerNode(workerServer1)
         .addUrlMap("/proxy_url1"))
      .addWorkerNode(new WorkerNode(workerServer2)
         .addUrlMap("/proxy_url2"))
      .addBalancerNode(new BalancerNode()
        .addWorker(new WorkerNode(workerServer3))
        .addWorker(new WorkerNode(workerServer4))
        .addUrlMap('/balanced_url'))
      .addStatusWorkerNode(new StatusWorkerNode()
        .setCss('my.css')
        .setUrlsMap(['/status']))
      .addStatusWorkerNode(new StatusWorkerNode()
        .setReadOnly(true)
        .setUrlsMap(['/status_readonly']))
      .setAdditionalUrlMaps(['!/admin|/admin/*':'*'])

    NodeOperations ops = configureFacingServerAndWorkers(scenario)

    ops.startAll()

    testModJkLoaded(facingServer)
    testComplexJkConfigurationUriWorkersMap(facingServer)
    testComplexJkConfigurationWorkersProperties(facingServer)
    testModifiedMojkConf(facingServer)
    httpdLogFileExists(facingServer, /jk.shm.*/)

    ops.stopAll()
  }

  @Test
  void testNoFacingServerGiven() {
    shouldFail IllegalStateException, {
      JkScenario scenario = new JkScenario()
      new FacingServerConfigurator(scenario, np.retrieveFacingServerNodeConfiguratorClass()).configure()
    }
  }

  @Test
  void testJkStatusWorkerCommandList() {
    String balancerId = "my-test-balancer"
    StatusWorkerNode statusWorker = new StatusWorkerNode()

    JkScenario scenario = new JkScenario()
        .setFacingServerNode(facingServerNode)
        .addBalancerNode(new BalancerNode(balancerId)
            .addWorker(new WorkerNode(workerServer1))
            .addWorker(new WorkerNode(workerServer2)))
        .addStatusWorkerNode(statusWorker)

    NodeOperations ops = configureFacingServerAndWorkers(scenario)

    ops.startAll()

    String res = new StatusWorkerOperation()
      .setAction(StatusWorkerOperation.Action.LIST)
      .setOutputFormat(StatusWorkerOperation.OutputFormat.TEXT)
      .setBalancerId(balancerId)
      .setAutomaticRefresh(1)
      .setHost(facingServer.getHost())
      .setPort(facingServer.getMainHttpPort())
      .buildAndExecute()
      .getLastResult()

    assertTrue res.contains("name=${balancerId}")

    ops.stopAll()
  }

  private prepareFacingServerAndWorkers() {
    this.facingServer = np.retrieveFacingServerNode().getServer()
    this.facingServerNode = np.retrieveFacingServerNode()
    Set<ServerAbstract> workers = np.retrieveWorkerServers()
    if (workers.size() >= 2) {
      this.workerServer1 = workers.first()
      workers = workers.drop(1)
      this.workerServer2 = workers.first()
      workers = workers.drop(1)
      if (workers.size() > 0) {
        this.workerServer3 = workers.first()
        workers = workers.drop(1)
      }
      if (workers.size() > 0) {
        this.workerServer4 = workers.first()
      }
    } else {
      throw IllegalStateException("There should be more than 1 workers in workspace")
    }
  }

  private NodeOperations configureFacingServerAndWorkers(JkScenario scenario) {
    this.configurator = new JkScenarioConfigurator(
      scenario,
      np.retrieveFacingServerNodeConfiguratorClass(),
      np.retrieveWorkerNodeConfiguratorClass()
    ).configure()

    new NodeOperations(scenario)
  }

  private httpdDisplaysWorkerResponce(httpd) {
    assertTrue VerifyURLBuilder.verifyURL {
      it.url new URL('http', httpd.getHost(), httpd.getMainHttpPort(), "")
      it.code 200
    }
  }

  private testModJkLoaded(Httpd httpd) {
    File modJkLog = getHttpdLogFile(httpd, "mod_jk.log")
    assertTrue "log file 'mod_jk.log' was not found.", modJkLog != null
    lineExists(modJkLog, /^.+mod_jk.+initialized$/)
    getHttpdLogFile(httpd, "mod_jk.log")
  }

  private testDefaulProxytUriWorkersMap(Httpd httpd) {
    testDefaulBalancingtUriWorkersMap(httpd, WorkerNode.DEFAULT_ID_PREFIX)
  }

  private testDefaulBalancingtUriWorkersMap(Httpd httpd) {
    testDefaulBalancingtUriWorkersMap(httpd, BalancerNode.DEFAULT_ID_PREFIX)
  }

  private testBalancingWithStatusWorkerUriWorkersMap(Httpd httpd) {
    File uriworkermapProperties = httpd.retrieveConfFilesByName(UriWorkerMapProperties.DEFAULT_NAME).first()
    assertEquals 2, uriworkermapProperties.readLines().size()
    lineExists(uriworkermapProperties, /^\/\*=${BalancerNode.DEFAULT_ID_PREFIX}\S+$/)
    lineExists(uriworkermapProperties, /^${StatusWorkerNode.DEFAULT_URL}=${StatusWorkerNode.DEFAULT_ID_PREFIX}\S+$/)
  }

  private testComplexJkConfigurationUriWorkersMap(Httpd httpd) {
    File uriworkermapProperties = httpd.retrieveConfFilesByName(UriWorkerMapProperties.DEFAULT_NAME).first()
    assertEquals 6, uriworkermapProperties.readLines().size()
    lineExists(uriworkermapProperties, /^\/proxy_url1=${WorkerNode.DEFAULT_ID_PREFIX}\S+$/)
    lineExists(uriworkermapProperties, /^\/proxy_url2=${WorkerNode.DEFAULT_ID_PREFIX}\S+$/)
    lineExists(uriworkermapProperties, /^\/balanced_url=${BalancerNode.DEFAULT_ID_PREFIX}\S+$/)
    lineExists(uriworkermapProperties, /^\/status=${StatusWorkerNode.DEFAULT_ID_PREFIX}\S+$/)
    lineExists(uriworkermapProperties, /^\/status_readonly=${StatusWorkerNode.DEFAULT_ID_PREFIX}\S+$/)
    lineExists(uriworkermapProperties, /^\!\/admin\|\/admin\/\*=\*$/)
  }

  private testBalancingWithStatusWorkerWorkersProperties(Httpd httpd) {
    File workersProperties = httpd.retrieveConfFilesByName(WorkersProperties.DEFAULT_NAME).first()
    assertEquals 15, workersProperties.readLines().size()
    lineExists(workersProperties, /^worker.list=${BalancerNode.DEFAULT_ID_PREFIX}\S+,${StatusWorkerNode.DEFAULT_ID_PREFIX}\S+$/)
    lineExists(workersProperties, /^worker.${WorkerNode.DEFAULT_ID_PREFIX}\S+.type=ajp13$/)
    lineExists(workersProperties, /^worker.${WorkerNode.DEFAULT_ID_PREFIX}\S+.host=.+$/)
    lineExists(workersProperties, /^worker.${WorkerNode.DEFAULT_ID_PREFIX}\S+.port=\S+$/)

    lineExists(workersProperties, /^worker.${WorkerNode.DEFAULT_ID_PREFIX}\S+.type=ajp13$/)
    lineExists(workersProperties, /^worker.${WorkerNode.DEFAULT_ID_PREFIX}\S+.host=.+$/)
    lineExists(workersProperties, /^worker.${WorkerNode.DEFAULT_ID_PREFIX}\S+.port=\S+$/)

    lineExists(workersProperties, /^worker.${BalancerNode.DEFAULT_ID_PREFIX}\S+.type=lb$/)
    lineExists(workersProperties, /^worker.${BalancerNode.DEFAULT_ID_PREFIX}\S+.balance_workers=${WorkerNode.DEFAULT_ID_PREFIX}\S+,${WorkerNode.DEFAULT_ID_PREFIX}\S+/)

    lineExists(workersProperties, /^worker.${StatusWorkerNode.DEFAULT_ID_PREFIX}\S+.type=status$/)
  }

  private testComplexJkConfigurationWorkersProperties(Httpd httpd) {
    File workersProperties = httpd.retrieveConfFilesByName(WorkersProperties.DEFAULT_NAME).first()
    assertEquals 27, workersProperties.readLines().size()
    lineExists(workersProperties, /^worker.list=${BalancerNode.DEFAULT_ID_PREFIX}\S+,${WorkerNode.DEFAULT_ID_PREFIX}\S+,${WorkerNode.DEFAULT_ID_PREFIX}\S+,${StatusWorkerNode.DEFAULT_ID_PREFIX}\S+,${StatusWorkerNode.DEFAULT_ID_PREFIX}\S+$/)
    lineExists(workersProperties, /^worker.${WorkerNode.DEFAULT_ID_PREFIX}\S+.type=ajp13$/)
    lineExists(workersProperties, /^worker.${WorkerNode.DEFAULT_ID_PREFIX}\S+.host=.+$/)
    lineExists(workersProperties, /^worker.${WorkerNode.DEFAULT_ID_PREFIX}\S+.port=\S+$/)

    lineExists(workersProperties, /^worker.${WorkerNode.DEFAULT_ID_PREFIX}\S+.type=ajp13$/)
    lineExists(workersProperties, /^worker.${WorkerNode.DEFAULT_ID_PREFIX}\S+.host=.+$/)
    lineExists(workersProperties, /^worker.${WorkerNode.DEFAULT_ID_PREFIX}\S+.port=\S+$/)

    lineExists(workersProperties, /^worker.${BalancerNode.DEFAULT_ID_PREFIX}\S+.type=lb$/)
    lineExists(workersProperties, /^worker.${BalancerNode.DEFAULT_ID_PREFIX}\S+.balance_workers=${WorkerNode.DEFAULT_ID_PREFIX}\S+,${WorkerNode.DEFAULT_ID_PREFIX}\S+/)

    lineExists(workersProperties, /^worker.${WorkerNode.DEFAULT_ID_PREFIX}\S+.type=ajp13$/)
    lineExists(workersProperties, /^worker.${WorkerNode.DEFAULT_ID_PREFIX}\S+.host=.+$/)
    lineExists(workersProperties, /^worker.${WorkerNode.DEFAULT_ID_PREFIX}\S+.port=\S+$/)

    lineExists(workersProperties, /^worker.${WorkerNode.DEFAULT_ID_PREFIX}\S+.type=ajp13$/)
    lineExists(workersProperties, /^worker.${WorkerNode.DEFAULT_ID_PREFIX}\S+.host=.+$/)
    lineExists(workersProperties, /^worker.${WorkerNode.DEFAULT_ID_PREFIX}\S+.port=${Tomcat.DEFAULT_AJP_PORT}$/)

    lineExists(workersProperties, /^worker.${StatusWorkerNode.DEFAULT_ID_PREFIX}\S+.type=status$/)
    lineExists(workersProperties, /^worker.${StatusWorkerNode.DEFAULT_ID_PREFIX}\S+.css=my.css/)

    lineExists(workersProperties, /^worker.${StatusWorkerNode.DEFAULT_ID_PREFIX}\S+.type=status$/)
    lineExists(workersProperties, /^worker.${StatusWorkerNode.DEFAULT_ID_PREFIX}\S+.read_only=true/)
  }

  private testDefaulBalancingtUriWorkersMap(Httpd httpd, String nodeName) {
    File uriworkermapProperties = httpd.retrieveConfFilesByName(UriWorkerMapProperties.DEFAULT_NAME).first()
    assertEquals 2, uriworkermapProperties.readLines().size()
    lineExists(uriworkermapProperties, /^\/\*=${nodeName}\S+$/)
  }

  private testDefaultProxyWorkersProperties(Httpd httpd) {
    File workersProperties = httpd.retrieveConfFilesByName(WorkersProperties.DEFAULT_NAME).first()
    assertEquals 6, workersProperties.readLines().size()
    lineExists(workersProperties, /^worker.list=${WorkerNode.DEFAULT_ID_PREFIX}\S+$/)
    lineExists(workersProperties, /^worker.${WorkerNode.DEFAULT_ID_PREFIX}\S+.type=ajp13$/)
    lineExists(workersProperties, /^worker.${WorkerNode.DEFAULT_ID_PREFIX}\S+.host=.+$/)
    lineExists(workersProperties, /^worker.${WorkerNode.DEFAULT_ID_PREFIX}\S+.port=\S+$/)
  }

  private testDefaulBalancingtWorkersProperties(Httpd httpd) {
    File workersProperties = httpd.retrieveConfFilesByName(WorkersProperties.DEFAULT_NAME).first()
    assertEquals 13, workersProperties.readLines().size()
    lineExists(workersProperties, /^worker.list=${BalancerNode.DEFAULT_ID_PREFIX}\S+$/)
    lineExists(workersProperties, /^worker.${WorkerNode.DEFAULT_ID_PREFIX}\S+.type=ajp13$/)
    lineExists(workersProperties, /^worker.${WorkerNode.DEFAULT_ID_PREFIX}\S+.host=.+$/)
    lineExists(workersProperties, /^worker.${WorkerNode.DEFAULT_ID_PREFIX}\S+.port=\S+$/)

    lineExists(workersProperties, /^worker.${WorkerNode.DEFAULT_ID_PREFIX}\S+.type=ajp13$/)
    lineExists(workersProperties, /^worker.${WorkerNode.DEFAULT_ID_PREFIX}\S+.host=.+$/)
    lineExists(workersProperties, /^worker.${WorkerNode.DEFAULT_ID_PREFIX}\S+.port=\S+$/)

    lineExists(workersProperties, /^worker.${BalancerNode.DEFAULT_ID_PREFIX}\S+.type=lb$/)
    lineExists(workersProperties, /^worker.${BalancerNode.DEFAULT_ID_PREFIX}\S+.balance_workers=${WorkerNode.DEFAULT_ID_PREFIX}\S+,${WorkerNode.DEFAULT_ID_PREFIX}\S+/)
  }

  private testDefaultMojkConf(Httpd httpd) {
    File modJkConf = httpd.retrieveConfFilesByName(ModJkConf.DEFAULT_NAME).first()
    assertEquals 9, modJkConf.readLines().size()
    lineExists(modJkConf, /^LoadModule jk_module.+mod_jk.so$/)
    lineExists(modJkConf, /^JkWorkersFile.+workers.properties$/)
    lineExists(modJkConf, /^JkLogLevel info$/)
    lineExists(modJkConf, /^JkLogStampFormat "\[%a %b %d %H:%M:%S %Y\]"$/)
    lineExists(modJkConf, /^JkOptions \+ForwardKeySize \+ForwardURICompat \-ForwardDirectories$/)
    lineExists(modJkConf, /^JkRequestLogFormat "%w %V %T"$/)
    lineExists(modJkConf, /^JkMountFile.+uriworkermap.properties$/)
    lineExists(modJkConf, /^JkShmFile.+jk.shm$/)
  }

  private testModifiedMojkConf(Httpd httpd) {
    File modJkConf = httpd.retrieveConfFilesByName(ModJkConf.DEFAULT_NAME).first()
    assertEquals 10, modJkConf.readLines().size()
    lineExists(modJkConf, /^LoadModule jk_module.+mod_jk.so$/)
    lineExists(modJkConf, /^JkWorkersFile.+workers.properties$/)
    lineExists(modJkConf, /^JkLogLevel debug/)
    lineExists(modJkConf, /^JkLogStampFormat "\[%a %b %d %H:%M:%S %Y\]"$/)
    lineExists(modJkConf, /^JkOptions \+ForwardKeySize \+ForwardURICompat \-ForwardDirectories$/)
    lineExists(modJkConf, /^JkRequestLogFormat "%w %V %T"$/)
    lineExists(modJkConf, /^JkMountFile.+uriworkermap.properties$/)
    lineExists(modJkConf, /^JkShmFile.+jk.shm$/)
    lineExists(modJkConf, /^# This is just test comment$/)
  }

  private getHttpdLogFile(Httpd httpd, String logName) {
    File logFile

    httpd.getLogDirs().each { logDir ->
      logFile = new File(httpd.getServerRoot(), "${logDir}${new Platform().sep}${logName}")
    }

    return logFile.exists() ? logFile : null
  }

  private httpdLogFileExists(Httpd httpd, String regexpFileName) {
    httpd.getLogDirs().each { logDir ->
      new File(httpd.getServerRoot(), logDir).eachFile { File logFile ->
        if (logFile.getName() ==~ regexpFileName) return true
      }
    }

    return false
  }

  private lineExists(File file, String regexp) {
    assertTrue "No line corresponding '${regexp}' found in '${file}'", file.readLines().find { it ==~ regexp } != null
  }


  private class JkClassInstancesCreator {
    Class workerServerClass
    Class facingServerClass

    private JkClassInstancesCreator(Class facingServerClass, Class workerServerClass) {
      this.facingServerClass = facingServerClass
      this.workerServerClass = workerServerClass
    }

    FacingServerNode retrieveFacingServerNode() {
      if (facingServerClass == Httpd.class) {
        return new FacingServerNode().setServer(serverController.getServerById(serverController.getHttpdServerId()))
      } else {
        throw new IllegalStateException("Unsupported facing server type")
      }
    }

    Class retrieveFacingServerNodeConfiguratorClass() {
      if (facingServerClass == Httpd.class) {
        return DefaultHttpdConfigurator.class
      } else {
        throw new IllegalStateException("Unsupported facing server type")
      }
    }

    Class retrieveWorkerNodeConfiguratorClass() {
      if (workerServerClass == Tomcat.class) {
        return DefaultTomcatWorkerConfigurator.class
      } else if (workerServerClass == AS7.class) {
        return DefaultAS7WorkerConfigurator.class
      } else {
        throw new IllegalStateException("Unsupported worker node type")
      }
    }

    ServerAbstract retrieveOneWorkerServer() {
      def workerId

      if (workerServerClass == Tomcat.class) {
        (workerId) = serverController.getTomcatServerIds()
      } else {
        (workerId) = serverController.getAs7ServerIds()
      }

      return serverController.getServerById(workerId)
    }

    Set<ServerAbstract> retrieveWorkerServers() {
      Set<String> workerIds
      Set<ServerAbstract> workersServers = new HashSet<ServerAbstract>()

      if (workerServerClass == Tomcat.class) {
        workerIds = serverController.getTomcatServerIds()
      } else {
        workerIds = serverController.getAs7ServerIds()
      }

      workerIds.each { workersServers << serverController.getServerById(it) }

      return workersServers
    }

    void prepareFacingServer() {
      ServerAbstract facingServer = np.retrieveFacingServerNode().getServer()
      if (facingServer.mainHttpPort <= 1024) {
        facingServer.shiftPorts(2000)
      }

      if (facingServerClass == Httpd.class) {
        // we are not testing ssl here, lets remove ssl
        def sslConfFile = new File(facingServer.getConfDeploymentPath(), "ssl.conf")
        log.debug("Deleting ssl.conf: $sslConfFile.absolutePath")
        JBFile.delete(sslConfFile)
      }
    }

    String retrievePropertiesFileName() {
      if (workerServerClass == Tomcat.class) return "ews-test.properties"
      else return "eap6-test.properties"
    }

    IWorkspace retrieveWorkspaceInstance() {
      if (workerServerClass == Tomcat.class) {
        return new WorkspaceHttpdTomcats(3)
      }
      else {
        def workspace = new WorkspaceMultipleHttpdAS7(false, false)
        workspace.numberOfAdditionalAS7s = 3
        return workspace
      }
    }

  }
}
