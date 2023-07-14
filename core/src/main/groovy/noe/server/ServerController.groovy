package noe.server

import groovy.util.logging.Slf4j
import noe.common.NoeContext
import noe.common.utils.Platform

import noe.ews.server.tomcat.TomcatProperties

import java.util.stream.Collectors

/**
 * Global class for management of servers
 * It is a singleton.
 */
@Slf4j
class ServerController {
  private static ServerController instance /// singleton instance
  final String context /// Context of controller (default, ews, ...)
  final NoeContext noeContext /// Context of controller (default, ews, ...)
  protected static Platform platform
  static String databaseLabel = 'oracle11gR2RAC'
  @Deprecated
  static AntBuilder ant

  protected Map<String, ServerAbstract> servers // all server instances, map[id: server]

  boolean refreshServersForEachTest = true

  protected ServerController() {
    servers = [:]
    this.ant = new AntBuilder()
    this.ant.property(environment: 'env')
    this.platform = new Platform()
    this.noeContext = NoeContext.forCurrentContext()
    this.context = noeContext.toString()
  }

  /**
   * Get instance of ServerConstroller (singleton)
   */
  static ServerController getInstance() {
    instance = instance ?: new ServerController()

    return instance
  }

  void clean() {
    servers.clear()
  }

  ServerAbstract addServer(String id, ServerAbstract server, Map props = [:]) {
    props.each { prop, val -> if (val) server.setProperty("$prop", val) }
    server.serverId = id
    servers[id] = server

    return server
  }

  ServerAbstract removeServer(String id) {
    return servers.remove(id)
  }

  int numberOfTomcatServers() {
    return servers.entrySet()
            .stream()
            .filter(
                    { Map.Entry<String, ServerAbstract> x ->
                      x.getKey().contains("tomcat")
                    }).count()
  }

  int numberOfHttpdServers() {
    return servers.entrySet()
            .stream()
            .filter(
                    { Map.Entry<String, ServerAbstract> x ->
                      x.getKey().contains("httpd")
                    }).count()
  }

  @Deprecated
  void startServer(String id, Map conf = [:]) {
    if (servers.containsKey(id)) {
      servers[id].start(conf)
    } else {
      throw new RuntimeException("Server with id $id does not exist.")
    }
  }

  void startAllServers(Map conf = [:]) {
    servers.each { id, server ->
      server.start(conf)
    }
  }

  def stopServer(String id, Map conf = [:]) {
    if (servers.containsKey(id)) {
      servers[id].stop(conf)
    } else {
      throw new RuntimeException("Server with id $id does not exist.")
    }
  }

  void stopAllServers(Map conf = [:]) {
    servers.each { id, server ->
      server.stop(conf)
    }
  }

  void killAll() {
    servers.each { id, server ->
      try {
        server.kill()
      } catch (e) {
        log.debug("[$id] Server was probaly stopped earlier.")
      }
    }
  }

  void killTreeAll() {
    servers.each { id, server ->
      try {
        server.killTree()
      } catch (e) {
        log.debug("[$id] Server was probaly stopped earlier.")
      }
    }
  }

  void killAllInSystem() {
    log.debug("Starting system wide process cleaning.")

    def killed = []
    def serverClassName = ''

    servers.each { id, server ->
      try {
        serverClassName = server.getClass().getName()
        // Kill all server types just once is enough.
        log.debug("[$serverClassName] try to kill.")
        if (!killed.contains(serverClassName)) {
          log.debug("[$serverClassName] killing.")
          server.killAllInSystem()
          killed.add(serverClassName)
        }
      }
      catch (e) {
       log.debug("[$id] Some error during servers killing, caught exception", e)
      }
    }

    log.debug("System wide process cleaning finished.")
  }

  /**
   * Set server properties.
   *
   * <strong>Use with care.</strong>
   * Calling this elsewhere than Workspace needs really reason and settings should be reverted after the test!
   *
   * We really do not want this public.
   * Because tests expects predefined settings from workspaces.
   */
  private void serverSetProperties(String id, Map properties) {
    if (servers[id]) {
      properties.each { prop, val -> servers[id].@"$prop" = val }
    } else {
      throw new RuntimeException("Server with id $id does not exist.")
    }
  }

  Map serverGetProperties(String id) {
    if (servers.containsKey(id)) {
      return servers[id].getProperties()
    } else {
      throw new RuntimeException("Server with id $id does not exist.")
    }
  }

  void backupConfsAll() {
    servers.each { id, server ->
      log.trace("Backing up configuration files of ${id}")
      server.backupConfs()
    }
  }

  void restoreConfsAll() {
    servers.each { id, server ->
      server.restoreConfs()
    }
  }

  void archiveLogsAll(testName) {
    servers.each { id, server ->
      server.archiveLogs(testName, id)
    }
  }

  void archiveConfsAll(testName) {
    servers.each { id, server ->
      server.archiveConfs(testName, id)
    }
  }

  void cleanLogsAll() {
    servers.each { id, server ->
      server.cleanLogs()
    }
  }

  Httpd addServerHttpd(String id, String basedir, version, Map props = [:], String httpdDir = '') {
    def newServerInstance = noe.server.Httpd.getInstance(basedir, version, httpdDir, noeContext)
    addServer(id, newServerInstance, props)

    return newServerInstance
  }

  OpensslServer addServerOpenssl(String id, String basedir) {
    OpensslServer newServerInstance = OpensslServer.getInstance(basedir)
    addServer(id, newServerInstance)
    return newServerInstance
  }

  void startTomcatWindowsService(String id, Map conf = [:]) {
    if (servers.containsKey(id)) {
      servers[id].startAsWindowsService()
    } else {
      throw new RuntimeException("Server with id $id does not exist.")
    }
  }

  void stopTomcatWindowsService(String id, Map conf = [:]) {
    if (servers.containsKey(id)) {
      servers[id].stopAsWindowsService()
    } else {
      throw new RuntimeException("Server with id $id does not exist.")
    }
  }

  void startServerCatalina(String id, Map conf = [:]) {
    if (servers.containsKey(id) && servers[id] instanceof Tomcat) {
      servers[id].startCatalina(conf)
    } else {
      throw new RuntimeException("Server with id $id does not exist or it is not instance of Tomcat.")
    }
  }

  void stopServerCatalina(String id, Map conf = [:]) {
    if (servers.containsKey(id) && servers[id] instanceof Tomcat) {
      servers[id].stopCatalina(conf)
    } else {
      throw new RuntimeException("Server with id $id does not exist or it is not instance of Tomcat.")
    }
  }

  void setRequestedSELinuxContext(String id, String context = "unconfined_t") {
    if (servers.containsKey(id)) {
      servers[id].setRequestedSELinuxContext(context)
    } else {
      throw new RuntimeException("Server with id $id does not exist.")
    }
  }

  void refreshAll() {
    if (!refreshServersForEachTest) {
      return
    }
    restore()
  }

  void backup() {
    servers.each { id, server ->
      server.backup()
    }
  }

  void restore() {
    servers.each { id, server ->
      server.restore()
    }
  }

  Set<String> getServerIds() {
    return servers.keySet()
  }

  ServerAbstract getServerById(String id) {
    log.trace("Retrieving server ${id}")
    if (servers.containsKey(String.valueOf(id))) {
      return servers[id]
    } else {
      throw new RuntimeException("Server with id $id does not exist.")
    }
  }

  String getHttpdServerId() {
    def result = servers.find { id, server -> server instanceof Httpd }
    return result ? result.key : null
  }

  Set<String> getHttpdServerIds() {
    def result = servers.findAll { id, server -> server instanceof Httpd }
    return result ? result.keySet() : null
  }

  String getApacheDSServerId() {
    def result = servers.find { id, server -> server instanceof ApacheDS }
    return result ? result.key : null
  }

  Set<String> getTomcatServerIds(List versions = [TomcatProperties.TOMCAT_MAJOR_VERSION]) {
    def returnServers = null
    if (versions.isEmpty()) {
      returnServers = servers.findAll { id, server -> server instanceof Tomcat }.keySet()
      log.trace("getTomcatServerIds: Versions was empty, returning: ${returnServers}")
    } else {
      returnServers = servers.findAll { id, server -> (server instanceof Tomcat) && (versions.contains(server.getVersion().toString())) }.keySet()
      log.trace("getTomcatServerIds: Returning: ${returnServers}, Versions was:${versions}")
    }
    return returnServers
  }

  void installApacheWindowsService(String id) {
    if (servers.containsKey(id)) {
      servers[id].installApacheWindowsService(new File(servers[id].getBinDirFullPath()))
    } else {
      throw new RuntimeException("Server with id $id does not exist.")
    }
  }

  void uninstallWindowsServices() {
    if (platform.isWindows()) {
      // Uninstall all Windows services
      servers.each { id, server ->
        if (servers[id] instanceof Httpd) {
          servers[id].uninstallApacheWindowsService(new File(servers[id].getBinDirFullPath()))
        }
      }
    }
  }

  void createUserTomcatManagerAdmin(String id) {
    if (servers.containsKey(id) && (servers[id] instanceof Tomcat)) {
      servers[id].createUserTomcatManagerAdmin()
    } else {
      throw new RuntimeException("Server with id $id does not exist.")
    }
  }

  Set<String> getAs7ServerIds() {
    return servers.findAll { id, server -> server instanceof AS7 }.keySet()
  }

  String getAs7ServerId() {
    def result = servers.find { id, server -> server instanceof AS7 }
    return result ? result.key : null
  }

  Set<String> getAs7DomainServerIds() {
    return servers.findAll { id, server -> server instanceof AS7Domain }.keySet()
  }

  String getAs7DomainServerId() {
    def result = servers.find { id, server -> server instanceof AS7Domain }
    return result ? result.key : null
  }

  Set<String> getAs5ServerIds() {
    return servers.findAll { id, server -> server instanceof AS5 }.keySet()
  }

  String getAs5ServerId() {
    def result = servers.find { id, server -> server instanceof AS5 }
    return result ? result.key : null
  }

  Tomcat addServerTomcat(String id, String basedir, version, Map props = [:], String tomcatDir = '') {
    addServer(id, noe.server.Tomcat.getInstance(basedir, version, tomcatDir, context), props)
  }

  ApacheDS addServerApacheDS(String id, String basedir, version) {
    addServer(id, noe.server.ApacheDS.getInstance(basedir, version))
  }

  String getTomcatServerId() {
    def result = servers.find { id, server -> server instanceof Tomcat }
    return result ? result.key : null
  }
  
  /**
   * Propagate method calling on server.
   * This is Groovy stuff
   *
   * @link http://groovy.codehaus.org/Using+methodMissing+and+propertyMissing
   */
  def methodMissing(String name, args) {
    def id = args.first()
    if (servers.containsKey(id) && servers[id].respondsTo(name)) {
      // Lets do dynamic invocation
      if (args.size() == 1) {
        return servers[id]."$name"()
      } else {
        return servers[id]."$name"(*args[1..args.size() - 1])
      }
    } else {
      throw new RuntimeException('Method "' + name + '" for server id ' + id + ' not found')
    }
  }

}
