package noe.eap.utils

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.utils.Platform
import noe.common.utils.Version
import noe.eap.creaper.ManagementClientProvider
import noe.eap.creaper.ServerVerProvider
import noe.server.AS7
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.StringUtils
import org.wildfly.extras.creaper.core.online.ModelNodeResult
import org.wildfly.extras.creaper.core.online.OnlineManagementClient
import org.wildfly.extras.creaper.core.online.operations.Address
import org.wildfly.extras.creaper.core.online.operations.Operations

/**
 * CLILib - your first step in the realm of AS7/Wildfly configuration!
 *
 * @author Michal Karm Babacek <karm@redhat.com>
 *
 */
@Slf4j
class CLILib {

  public static enum Connector {
    AJP, HTTPS, HTTP
  }

  /**
   * Add an extension, e.g. org.jboss.as.modcluster
   */
  public static class ExtensionBuilder {
    AS7 as7serverInstance
    String extension

    private ExtensionBuilder() {}

    /**
     * MANDATORY
     * @param extension
     * @return
     */
    ExtensionBuilder extension(String extension) {
      this.extension = extension
      return this
    }

    /**
     * MANDATORY
     * @param as7serverInstance
     * @return
     */
    ExtensionBuilder as7serverInstance(AS7 as7serverInstance) {
      this.as7serverInstance = as7serverInstance
      return this
    }

    public static int build(block) {
      final ExtensionBuilder extensionBuilder = new ExtensionBuilder().with block
      if (extensionBuilder.as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")
      if (StringUtils.isBlank(extensionBuilder.extension)) throw new IllegalArgumentException("Extension must not be null nor empty.")
      return extensionBuilder.as7serverInstance.as7Cli.runArbitraryCommand("/extension=${extensionBuilder.extension}:add()").exitValue
    }
  }

  /**
   * Add AJP Connector
   *
   *  TODO: Check if the connector is already present.
   *
   * JBossWeb: OK
   * Undertow: OK
   */
  public static class AJPConnectorBuilder {
    AS7 as7serverInstance

    private AJPConnectorBuilder() {}

    /**
     * MANDATORY
     * @param as7serverInstance
     * @return
     */
    AJPConnectorBuilder as7serverInstance(AS7 as7serverInstance) {
      this.as7serverInstance = as7serverInstance
      return this
    }

    public static int build(block) {
      final AJPConnectorBuilder ajpConnectorBuilder = new AJPConnectorBuilder().with block
      if (ajpConnectorBuilder.as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")
      final String cmdStrUndertow = "/subsystem=undertow/server=default-server/ajp-listener=ajp:add(socket-binding=ajp)"
      final String cmdStrJBossWeb = "/subsystem=web/connector=ajp:add(name=ajp,protocol=ajp,scheme=ajp,socket-binding=ajp)"
      return ajpConnectorBuilder.as7serverInstance.as7Cli.runArbitraryCommand(
          (ajpConnectorBuilder.as7serverInstance.eapVersion >= new Version("7.0.0.DR1")) ? cmdStrUndertow : cmdStrJBossWeb
      ).exitValue
    }
  }

  static class UndertowLocationBuilder {

    AS7 as7serverInstance
    String name
    String handler
    Map<String, String> options

    private UndertowLocationBuilder() {}

    /**
     * MANDATORY
     */
    UndertowLocationBuilder as7serverInstance(AS7 as7serverInstance) {
      this.as7serverInstance = as7serverInstance
      return this
    }

    /**
     * MANDATORY
     * Every slash(/) should be negated with (\)
     */
    UndertowLocationBuilder name(String name) {
      this.name = name
      return this
    }

    /**
     * MANDATORY for build
     */
    UndertowLocationBuilder handler(String handler) {
      this.handler = handler
      return this
    }

    static int build(block) {
      final UndertowLocationBuilder builder = new UndertowLocationBuilder().with block
      if (builder.as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")
      if (StringUtils.isBlank(builder.name)) throw new IllegalArgumentException("name must not be null.")
      if (StringUtils.isBlank(builder.handler)) throw new IllegalArgumentException("handler must not be null.")
      String cmdStr
      if (builder.exists()) {
        int ret = builder.remove()
        if (ret > 0) {
          log.error("Failure: ${cmdStr}")
          return ret
        }
        ret = CLILib.reload(builder.as7serverInstance, true) //Reload in admin mode as this removed http-listener might be used
        if (ret > 0) {
          log.error("Failure: ${cmdStr}")
          return ret
        }
      }
      cmdStr = "/subsystem=undertow/server=default-server/host=default-host/location=${builder.name}:add(handler=${builder.handler}"
      return builder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
    }

    static int remove(block) {
      final UndertowLocationBuilder builder = new UndertowLocationBuilder().with block
      return builder.remove()
    }

    int remove() {
      if (!exists()) {
        log.trace("Undertow location with name ${name} doesn't exist and therefore cannot remove it")
        return 0
      }
      String cmdStr = "/subsystem=undertow/server=default-server/host=default-host/location=${name}:remove()"
      return as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
    }

    private boolean exists() {
      if (as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")
      if (StringUtils.isBlank(name)) throw new IllegalArgumentException("name must not be null.")
      String cmdStr = "/subsystem=undertow/server=default-server/host=default-host:read-children-names(child-type=location)"
      return as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).stdOut ==~ /[\s\S]*"result" => [\s\S]*"${name.replace('\\/', '/')}"[\s\S]*/
    }
  }

  static class UndertowHandlerBuilder {

    AS7 as7serverInstance
    String name
    String path
    Map<String, String> options

    private UndertowHandlerBuilder() {}

    /**
     * MANDATORY
     */
    UndertowHandlerBuilder as7serverInstance(AS7 as7serverInstance) {
      this.as7serverInstance = as7serverInstance
      return this
    }

    /**
     * MANDATORY
     */
    UndertowHandlerBuilder name(String name) {
      this.name = name
      return this
    }

    /**
     * MANDATORY for build
     */
    UndertowHandlerBuilder path(String path) {
      this.path = path
      return this
    }

    /**
     * Optional attributes:
     * cache-buffers       directory-listing   path
     * cache-buffer-size   case-sensitive      follow-symlink      safe-symlink-paths
     */
    UndertowHandlerBuilder options(Map<String,String> options) {
      this.options = options
      return this
    }

    static int build(block) {
      final UndertowHandlerBuilder builder = new UndertowHandlerBuilder().with block
      if (builder.as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")
      if (StringUtils.isBlank(builder.name)) throw new IllegalArgumentException("name must not be null.")
      if (StringUtils.isBlank(builder.path)) throw new IllegalArgumentException("path must not be null.")
      int ret = 0
      String cmdStr
      if (builder.exists()) {
        ret = builder.remove()
        if (ret > 0) {
          log.error("Failure: ${cmdStr}")
          return ret
        }
        ret = CLILib.reload(builder.as7serverInstance, true) //Reload in admin mode as this removed http-listener might be used
        if (ret > 0) {
          log.error("Failure: ${cmdStr}")
          return ret
        }
      }
      StringBuilder strBuilder = new StringBuilder("/subsystem=undertow/configuration=handler/file=${builder.name}:add(path=${builder.path}")
      builder.options.each { key, value ->
        strBuilder.append(", ${key}=${value}")
      }
      cmdStr = strBuilder.toString()
      ret = builder.as7serverInstance.as7Cli.runArbitraryCommand(strBuilder.toString()).exitValue
      if (ret > 0) {
        log.error("Failure: ${cmdStr}")
        return ret
      }
      return CLILib.reload(builder.as7serverInstance)
    }

    static int update(block) {
      final UndertowHandlerBuilder builder = new UndertowHandlerBuilder().with block
      if (!builder.exists()) {
        throw new IllegalArgumentException("http-listener with ${builder.name} doesn't exists")
      }
      Map output
      Map.Entry<String, String> failed = builder.options.find { key, value ->
        String cmdStr = "/subsystem=undertow/configuration=handler/file=${builder.name}:write-attribute(name=${key},value=${value}"
        output = builder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr)
        if (output.exitValue > 0) {
          return true
        }
      }
      if (failed) {
        log.error("Writing attribute ${failed.key} with value ${failed.value} failed!")
        return output.exitValue
      }
      return CLILib.reload(builder.as7serverInstance)
    }

    static int remove(block) {
      final UndertowHandlerBuilder builder = new UndertowHandlerBuilder().with block
      return builder.remove()
    }

    int remove() {
      if (!exists()) {
        log.trace("Undertow handler with name ${name} doesn't exist and therefore cannot remove it")
        return 0
      }
      String cmdStr = "/subsystem=undertow/configuration=handler/file=${name}:remove()"
      return as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
    }

    private boolean exists() {
      if (as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")
      if (StringUtils.isBlank(name)) throw new IllegalArgumentException("name must not be null.")
      String cmdStr = "/subsystem=undertow/configuration=handler:read-children-names(child-type=file)"
      return as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).stdOut ==~ /[\s\S]*"result" => [\s\S]*"${name}"[\s\S]*/
    }
  }

  static class HttpConnectorBuilder {

    AS7 as7serverInstance
    String socketBinding
    String name = "default"
    Map<String, String> options

    private HttpConnectorBuilder() {}

    /**
     * MANDATORY
     */
    HttpConnectorBuilder as7serverInstance(AS7 as7serverInstance) {
      this.as7serverInstance = as7serverInstance
      return this
    }

    /**
     * MANDATORY
     */
    HttpConnectorBuilder name(String name) {
      this.name = name
      return this
    }

    /**
     * MANDATORY for build
     */
    HttpConnectorBuilder socketBinding(String socketBinding) {
      this.socketBinding = socketBinding
      return this
    }

    /**
     * Optional attributes:
     * allow-encoded-slash           decode-url                    http2-initial-window-size     max-cookies
     * proxy-address-forwarding      require-host-http11           tcp-keep-alive
     * allow-equals-in-cookie-value  disallowed-methods            http2-max-concurrent-streams  max-header-size
     * read-timeout                  resolve-peer-address          url-charset
     * always-set-keep-alive         enable-http2                  http2-max-frame-size          max-headers
     * receive-buffer                secure                        worker
     * buffer-pipelined-data         enabled                       http2-max-header-list-size    max-parameters
     * record-request-start-time     send-buffer                   write-timeout
     * buffer-pool                   http2-enable-push             max-buffered-request-size     max-post-size
     * redirect-socket               socket-binding
     * certificate-forwarding        http2-header-table-size       max-connections               no-request-timeout
     * request-parse-timeout         tcp-backlog
     */
    HttpConnectorBuilder options(Map<String,String> options) {
      this.options = options
      return this
    }

    static int build(block) {
      final HttpConnectorBuilder builder = new HttpConnectorBuilder().with block
      if (builder.as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")
      if (StringUtils.isBlank(builder.name)) throw new IllegalArgumentException("name must not be null.")
      if (StringUtils.isBlank(builder.socketBinding)) throw new IllegalArgumentException("socketBinding must not be null.")
      int ret = 0
      String cmdStr
      if (builder.exists()) {
        cmdStr = "/subsystem=undertow/server=default-server/http-listener=${builder.name}:remove()"
        ret = builder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (ret > 0) {
          log.error("Failure: ${cmdStr}")
          return ret
        }
        ret = CLILib.reload(builder.as7serverInstance, true) //Reload in admin mode as this removed http-listener might be used
        if (ret > 0) {
          log.error("Failure: ${cmdStr}")
          return ret
        }
      }
      StringBuilder strBuilder = new StringBuilder("/subsystem=undertow/server=default-server/http-listener=${builder.name}:add(socket-binding=${builder.socketBinding}")
      builder.options.each { key, value ->
        strBuilder.append(", ${key}=${value}")
      }
      cmdStr = strBuilder.toString()
      ret = builder.as7serverInstance.as7Cli.runArbitraryCommand(strBuilder.toString()).exitValue
      if (ret > 0) {
        log.error("Failure: ${cmdStr}")
        return ret
      }
      return CLILib.reload(builder.as7serverInstance)
    }

    static int update(block) {
      final HttpConnectorBuilder builder = new HttpConnectorBuilder().with block
      if (!builder.exists()) {
        throw new IllegalArgumentException("http-listener with ${builder.name} doesn't exists")
      }
      Map output
      Map.Entry<String, String> failed = builder.options.find { key, value ->
        String cmdStr = "/subsystem=undertow/server=default-server/http-listener=${builder.name}:write-attribute(name=${key},value=${value}"
        output = builder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr)
        if (output.exitValue > 0) {
          return true
        }
      }
      if (failed) {
        log.error("Writing attribute ${failed.key} with value ${failed.value} failed!")
        return output.exitValue
      }
      return CLILib.reload(builder.as7serverInstance)
    }

    private boolean exists() {
      if (as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")
      if (StringUtils.isBlank(name)) throw new IllegalArgumentException("name must not be null.")
      String cmdStr = "/subsystem=undertow/server=default-server:read-children-names(child-type=http-listener)"
      return as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).stdOut ==~ /[\s\S]*"result" => [\s\S]*"${name}"[\s\S]*/
    }

  }

  /**
   * Add HTTPS Connector
   *
   *
   * JBossWeb: TODO, currently in XSLT
   * Undertow: OK
   */
  public static class HTTPSConnectorBuilder {
    AS7 as7serverInstance
    String securityRealm
    String sslContext
    String verifyClient = VerifyClient.NOT_REQUESTED.value

    public enum VerifyClient {
      NOT_REQUESTED("NOT_REQUESTED"), REQUESTED("REQUESTED"), REQUIRED("REQUIRED")
      private String value
      private VerifyClient(String value) {
        this.value = value
      }
    }

    private HTTPSConnectorBuilder() {}

    /**
     * MANDATORY
     * @param as7serverInstance
     * @return
     */
    HTTPSConnectorBuilder as7serverInstance(AS7 as7serverInstance) {
      this.as7serverInstance = as7serverInstance
      return this
    }

    HTTPSConnectorBuilder securityRealm(String securityRealm) {
      this.securityRealm = securityRealm
      return this
    }

    HTTPSConnectorBuilder verifyClient(VerifyClient verifyClient) {
      this.verifyClient = verifyClient.value
      return this
    }

    HTTPSConnectorBuilder sslContext(String sslContext) {
      this.sslContext = sslContext
      return this
    }


    static int build(block) {
      final HTTPSConnectorBuilder httpsConnectorBuilder = new HTTPSConnectorBuilder().with block
      if (httpsConnectorBuilder.as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")
      def rcode = 0
      String cmdString = "/subsystem=undertow/server=default-server:read-resource()"
      if (!(CLILib.readArbitraryCommandOutput(httpsConnectorBuilder.as7serverInstance, cmdString) =~ /\s*"https-listener"\s*=>\s*undefined\s*/)) {
        cmdString = "/subsystem=undertow/server=default-server/https-listener=https:remove()"
        rcode = httpsConnectorBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdString).exitValue
        if (rcode != 0) {
          throw new RuntimeException("Something went wrong when removing existing https-listener")
        }
        rcode = CLILib.reload(httpsConnectorBuilder.as7serverInstance)
        if (rcode != 0) {
          throw new RuntimeException("Something went wrong when removing existing https-listener")
        }
      }
      if (!StringUtils.isBlank(httpsConnectorBuilder.securityRealm)) {
        cmdString = "/subsystem=undertow/server=default-server/https-listener=https:add(socket-binding=https, verify-client=${httpsConnectorBuilder.verifyClient}," +
                "security-realm=${httpsConnectorBuilder.securityRealm})"
      } else if (!StringUtils.isBlank(httpsConnectorBuilder.sslContext)) {
        cmdString = "/subsystem=undertow/server=default-server/https-listener=https:add(socket-binding=https, ssl-context=${httpsConnectorBuilder.sslContext})"
      } else {
        throw new IllegalArgumentException("security-realm of ssl-context have to be set!")
      }
      return httpsConnectorBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdString).exitValue
    }
  }

  /**
   * Remove Connector
   *
   * JBossWeb: TODO
   * Undertow: OK
   */
  public static class RemoveConnectorBuilder {
    AS7 as7serverInstance
    Connector connector

    private RemoveConnectorBuilder() {}

    /**
     * MANDATORY
     * @param as7serverInstance
     * @return
     */
    RemoveConnectorBuilder as7serverInstance(AS7 as7serverInstance) {
      this.as7serverInstance = as7serverInstance
      return this
    }

    /**
     * MANDATORY
     * @param connector , ajp, http or https
     * @return
     */
    RemoveConnectorBuilder connector(Connector connector) {
      this.connector = connector
      return this
    }

    public static int build(block) {
      final RemoveConnectorBuilder removeConnectorBuilder = new RemoveConnectorBuilder().with block
      if (removeConnectorBuilder.as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")
      if (removeConnectorBuilder.connector == null) throw new IllegalArgumentException("Connector name must be set.")
      final String cmdStrAJP = "/subsystem=undertow/server=default-server/ajp-listener=ajp:remove()"
      final String cmdStrHTTPS = "/subsystem=undertow/server=default-server/https-listener=https:remove()"
      final String cmdStrHTTP = "/subsystem=undertow/server=default-server/http-listener=default:remove()"
      if (removeConnectorBuilder.connector == Connector.AJP) {
        return removeConnectorBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStrAJP).exitValue
      } else if (removeConnectorBuilder.connector == Connector.HTTP) {
        return removeConnectorBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStrHTTP).exitValue
      } else if (removeConnectorBuilder.connector == Connector.HTTPS) {
        return removeConnectorBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStrHTTPS).exitValue
      } else {
        throw new IllegalArgumentException("No command for such Connector name.")
      }
    }
  }


  public static class AddVirtualHostBuilder {
    AS7 as7serverInstance
    String server = 'default-server'
    String hostName
    String location = '/'
    String defaultWebModule = 'ROOT.war'


    private AddVirtualHostBuilder() {}

    /**
     * MANDATORY
     * @param as7serverInstance
     * @return
     */
    AddVirtualHostBuilder as7serverInstance(AS7 as7serverInstance) {
      this.as7serverInstance = as7serverInstance
      return this
    }

    /**
     * Name of the server you want to add this host
     */
    AddVirtualHostBuilder server(String server) {
      this.server = server
      return this
    }

    /**
     * Name of the default web module
     */
    AddVirtualHostBuilder defaultWebModule(String defaultWebModule) {
      this.defaultWebModule = defaultWebModule
      return this
    }

    /**
     * MANDATORY
     * Name of the host you want to add server
     */
    AddVirtualHostBuilder hostName(String hostName) {
      this.hostName = hostName
      return this
    }

    AddVirtualHostBuilder location(String location) {
      this.location = location
      return this
    }

    public static int build(block) {
      final AddVirtualHostBuilder addVirtualHostBuilder = new AddVirtualHostBuilder().with block
      if (addVirtualHostBuilder.as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")
      if (addVirtualHostBuilder.hostName == null) throw new IllegalArgumentException("Host name must not be null.")
      String createCommand = "/subsystem=undertow/server=${addVirtualHostBuilder.server}/host=${addVirtualHostBuilder.hostName}" +
              ":add(default-web-module=${addVirtualHostBuilder.defaultWebModule})"
      String escapedLocation =  CLILib.escapeQuotes(addVirtualHostBuilder.location)
      String locationCommand = "/subsystem=undertow/server=${addVirtualHostBuilder.server}" +
              "/host=${addVirtualHostBuilder.hostName}/location=${escapedLocation}:add(handler=welcome-content)"
      if (ServerVerProvider.provideFor(addVirtualHostBuilder.as7serverInstance).lessThan(ServerVerProvider.getMngmtVerOfEAP("7.0.0.DR1"))) {
        createCommand = "/subsystem=web/virtual-server=${addVirtualHostBuilder.hostName}:add"
        return addVirtualHostBuilder.as7serverInstance.as7Cli.runArbitraryCommand(createCommand).exitValue
      }
      addVirtualHostBuilder.as7serverInstance.as7Cli.runArbitraryCommand(createCommand).exitValue
      return addVirtualHostBuilder.as7serverInstance.as7Cli.runArbitraryCommand(locationCommand).exitValue
    }
  }

  public static boolean managementApiReady(AS7 as7serverInstance) {
    Map commandOutput = as7serverInstance.as7Cli.runArbitraryCommand(":read-attribute(name=server-state)")
    return (commandOutput.exitValue == 0 && commandOutput.stdOut =~ /\s*"result"\s*=>\s*"running"\s*/)
  }

  public static String escapeQuotes(String nonEscapedString) {
    return (new Platform()).isWindows() ? '\\"' + nonEscapedString + '\\"' : '"' + nonEscapedString + '"'
  }

  public static String escapeQuotes(List<String> nonEscapedList) {
    return nonEscapedList.collect({item -> escapeQuotes(item)})
  }



  /**
   * Adds mod_cluster subsystem to the server configuration
   */
  public static class ModClusterSubsystemBuilder {
    AS7 as7serverInstance

    String multicastAddress = DefaultProperties.MODCLUSTER_MCAST_ADDRESS
    Integer multicastPort = Integer.parseInt(DefaultProperties.MODCLUSTER_MCAST_PORT)
    String advertiseSocket = "modcluster"

    String advertiseSecurityKey
    Boolean advertise // e.g. true
    Boolean autoEnableContexts // e.g. true
    String balancer
    String connector // e.g. ajp
    String excludedContexts // e.g. "ROOT,invoker,jbossws,juddi,console"
    Boolean flushPackets // e.g. false
    Integer flushWait // e.g. -1
    String loadBalancingGroup
    Integer maxAttempts // e.g. 1
    Integer nodeTimeout // e.g. -1
    Integer ping // e.g. 10
    List<String> proxies
    String proxyList
    String proxyUrl // e.g. /
    String sessionDrainingStrategy // e.g. DEFAULT
    Integer smax // e.g. -1
    Integer socketTimeout // e.g. 20
    Integer statusInterval // e.g. 10
    Boolean stickySessionForce // e.g. false
    Boolean stickySessionRemove // e.g. false
    Boolean stickySession // e.g. true
    Integer stopContextTimeout // e.g. 10
    Integer ttl // e.g. -1
    Integer workerTimeout // e.g. -1
    Integer simpleLoadProvider
    String sslContext

    private ModClusterSubsystemBuilder() {}

    /**
     * MANDATORY
     * @param as7serverInstance
     * @return
     */
    ModClusterSubsystemBuilder as7serverInstance(AS7 as7serverInstance) {
      this.as7serverInstance = as7serverInstance
      return this
    }

    /**
     * MANDATORY
     * @param multicastPort
     * @return
     */
    ModClusterSubsystemBuilder multicastPort(Integer multicastPort) {
      this.multicastPort = multicastPort
      return this
    }

    /**
     * MANDATORY
     * @param multicastAddress
     * @return
     */
    ModClusterSubsystemBuilder multicastAddress(String multicastAddress) {
      this.multicastAddress = multicastAddress
      return this
    }

    ModClusterSubsystemBuilder advertiseSecurityKey(String advertiseSecurityKey) {
      this.advertiseSecurityKey = advertiseSecurityKey
      return this
    }

    /**
     * MANDATORY
     * @param advertiseSocket
     * @return
     */
    ModClusterSubsystemBuilder advertiseSocket(String advertiseSocket) {
      this.advertiseSocket = advertiseSocket
      return this
    }

    ModClusterSubsystemBuilder advertise(Boolean advertise) {
      this.advertise = advertise
      return this
    }

    ModClusterSubsystemBuilder autoEnableContexts(Boolean autoEnableContexts) {
      this.autoEnableContexts = autoEnableContexts
      return this
    }

    ModClusterSubsystemBuilder balancer(String balancer) {
      this.balancer = balancer
      return this
    }

    ModClusterSubsystemBuilder connector(String connector) {
      this.connector = connector
      return this
    }

    ModClusterSubsystemBuilder excludedContexts(String excludedContexts) {
      this.excludedContexts = excludedContexts
      return this
    }

    ModClusterSubsystemBuilder flushPackets(Boolean flushPackets) {
      this.flushPackets = flushPackets
      return this
    }

    ModClusterSubsystemBuilder flushWait(Integer flushWait) {
      this.flushWait = flushWait
      return this
    }

    ModClusterSubsystemBuilder loadBalancingGroup(String loadBalancingGroup) {
      this.loadBalancingGroup = loadBalancingGroup
      return this
    }

    ModClusterSubsystemBuilder maxAttempts(Integer maxAttempts) {
      this.maxAttempts = maxAttempts
      return this
    }

    ModClusterSubsystemBuilder nodeTimeout(Integer nodeTimeout) {
      this.nodeTimeout = nodeTimeout
      return this
    }

    ModClusterSubsystemBuilder ping(Integer ping) {
      this.ping = ping
      return this
    }

    ModClusterSubsystemBuilder proxies(List<String> proxies) {
      this.proxies = proxies
      return this
    }

    ModClusterSubsystemBuilder proxyList(String proxyList) {
      this.proxyList = proxyList
      return this
    }

    ModClusterSubsystemBuilder proxyUrl(String proxyUrl) {
      this.proxyUrl = proxyUrl
      return this
    }

    ModClusterSubsystemBuilder sessionDrainingStrategy(String sessionDrainingStrategy) {
      this.sessionDrainingStrategy = sessionDrainingStrategy
      return this
    }

    ModClusterSubsystemBuilder smax(Integer smax) {
      this.smax = smax
      return this
    }

    ModClusterSubsystemBuilder socketTimeout(Integer socketTimeout) {
      this.socketTimeout = socketTimeout
      return this
    }

    ModClusterSubsystemBuilder statusInterval(Integer statusInterval) {
      this.statusInterval = statusInterval
      return this
    }

    ModClusterSubsystemBuilder stickySessionForce(Boolean stickySessionForce) {
      this.stickySessionForce = stickySessionForce
      return this
    }

    ModClusterSubsystemBuilder stickySessionRemove(Boolean stickySessionRemove) {
      this.stickySessionRemove = stickySessionRemove
      return this
    }

    ModClusterSubsystemBuilder stickySession(Boolean stickySession) {
      this.stickySession = stickySession
      return this
    }

    ModClusterSubsystemBuilder stopContextTimeout(Integer stopContextTimeout) {
      this.stopContextTimeout = stopContextTimeout
      return this
    }

    ModClusterSubsystemBuilder ttl(Integer ttl) {
      this.ttl = ttl
      return this
    }

    ModClusterSubsystemBuilder workerTimeout(Integer workerTimeout) {
      this.workerTimeout = workerTimeout
      return this
    }

    ModClusterSubsystemBuilder simpleLoadProvider(Integer simpleLoadProvider) {
      this.simpleLoadProvider = simpleLoadProvider
      return this
    }

    ModClusterSubsystemBuilder sslContext(String sslContext) {
      this.sslContext = sslContext
      return this
    }

    public static int build(block) {
      final ModClusterSubsystemBuilder modClusterSubsystemBuilder = new ModClusterSubsystemBuilder().with block
      if (modClusterSubsystemBuilder.as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")

      // Due to the nature of pre-EAP7 subsystem, it had to be added as a composite operation.
      String cmdStr
      int rcode = 0
      boolean justCreated = false
      cmdStr = ":read-resource"
      if (!(CLILib.readArbitraryCommandOutput(modClusterSubsystemBuilder.as7serverInstance, cmdStr) =~ /subsystem[[^\n]*\n]*modcluster[ "=>\{\n]*undefined/)) {
          rcode = CLILib.MulticastBindingBuilder.build {
            as7serverInstance modClusterSubsystemBuilder.as7serverInstance
            multicastAddress DefaultProperties.MODCLUSTER_MCAST_ADDRESS
            multicastPort Integer.parseInt(DefaultProperties.MODCLUSTER_MCAST_PORT)
            name modClusterSubsystemBuilder.advertiseSocket
            port 0
          }
          if (rcode > 0) {
            log.error("MulticastBindingBuilder failure.")
            return rcode
          }

          cmdStr = "/subsystem=modcluster:add()"
          rcode = modClusterSubsystemBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
          if (rcode > 0) {
            log.error("Failure: ${cmdStr}")
            return rcode
          }

          cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration:add(connector=${CLILib.escapeQuotes(modClusterSubsystemBuilder.connector)}, advertise-socket=${modClusterSubsystemBuilder.advertiseSocket})"
          rcode = modClusterSubsystemBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
          if (rcode > 0) {
            log.error("Failure: ${cmdStr}")
            return rcode
          }
          justCreated = true
      }

      if (!justCreated && !StringUtils.isBlank(modClusterSubsystemBuilder.advertiseSocket)) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration:write-attribute(name=advertise-socket, value=${CLILib.escapeQuotes(modClusterSubsystemBuilder.advertiseSocket)})"
        rcode = modClusterSubsystemBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!StringUtils.isBlank(modClusterSubsystemBuilder.advertiseSecurityKey)) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration:write-attribute(name=advertise-security-key, value=${CLILib.escapeQuotes(modClusterSubsystemBuilder.advertiseSecurityKey)})"
        rcode = modClusterSubsystemBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterSubsystemBuilder.advertise != null) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration:write-attribute(name=advertise, value=${modClusterSubsystemBuilder.advertise})"
        rcode = modClusterSubsystemBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterSubsystemBuilder.autoEnableContexts != null) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration:write-attribute(name=auto-enable-contexts, value=${modClusterSubsystemBuilder.autoEnableContexts})"
        rcode = modClusterSubsystemBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!StringUtils.isBlank(modClusterSubsystemBuilder.balancer)) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration:write-attribute(name=balancer, value=${CLILib.escapeQuotes(modClusterSubsystemBuilder.balancer)})"
        rcode = modClusterSubsystemBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!justCreated && !StringUtils.isBlank(modClusterSubsystemBuilder.connector)) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration:write-attribute(name=connector, value=${CLILib.escapeQuotes(modClusterSubsystemBuilder.connector)}"
        rcode = modClusterSubsystemBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!StringUtils.isBlank(modClusterSubsystemBuilder.excludedContexts)) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration:write-attribute(name=excluded-contexts, value=${CLILib.escapeQuotes(modClusterSubsystemBuilder.excludedContexts)})"
        rcode = modClusterSubsystemBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterSubsystemBuilder.flushPackets != null) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration:write-attribute(name=flush-packets, value=${modClusterSubsystemBuilder.flushPackets})"
        rcode = modClusterSubsystemBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterSubsystemBuilder.flushWait != null) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration:write-attribute(name=flush-wait, value=${modClusterSubsystemBuilder.flushWait})"
        rcode = modClusterSubsystemBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!StringUtils.isBlank(modClusterSubsystemBuilder.loadBalancingGroup)) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration:write-attribute(name=load-balancing-group, value=${CLILib.escapeQuotes(modClusterSubsystemBuilder.loadBalancingGroup)})"
        rcode = modClusterSubsystemBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterSubsystemBuilder.maxAttempts != null) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration:write-attribute(name=max-attempts, value=${modClusterSubsystemBuilder.maxAttempts})"
        rcode = modClusterSubsystemBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterSubsystemBuilder.nodeTimeout != null) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration:write-attribute(name=node-timeout, value=${modClusterSubsystemBuilder.nodeTimeout})"
        rcode = modClusterSubsystemBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterSubsystemBuilder.ping != null) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration:write-attribute(name=ping, value=${modClusterSubsystemBuilder.ping})"
        rcode = modClusterSubsystemBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterSubsystemBuilder.proxies != null) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration:write-attribute(name=proxies, value=[${modClusterSubsystemBuilder.proxies.join(",")}])"
        rcode = modClusterSubsystemBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!StringUtils.isBlank(modClusterSubsystemBuilder.sslContext)) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration:write-attribute(name=ssl-context, value=${CLILib.escapeQuotes(modClusterSubsystemBuilder.sslContext)})"
        rcode = modClusterSubsystemBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!StringUtils.isBlank(modClusterSubsystemBuilder.proxyList)) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration:write-attribute(name=proxy-list, value=${CLILib.escapeQuotes(modClusterSubsystemBuilder.proxyList)})"
        rcode = modClusterSubsystemBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!StringUtils.isBlank(modClusterSubsystemBuilder.proxyUrl)) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration:write-attribute(name=proxy-url, value=${CLILib.escapeQuotes(modClusterSubsystemBuilder.proxyUrl)})"
        rcode = modClusterSubsystemBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!StringUtils.isBlank(modClusterSubsystemBuilder.sessionDrainingStrategy)) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration:write-attribute(name=session-draining-strategy, value=${CLILib.escapeQuotes(modClusterSubsystemBuilder.sessionDrainingStrategy)})"
        rcode = modClusterSubsystemBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterSubsystemBuilder.smax != null) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration:write-attribute(name=smax, value=${modClusterSubsystemBuilder.smax})"
        rcode = modClusterSubsystemBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterSubsystemBuilder.socketTimeout != null) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration:write-attribute(name=socket-timeout, value=${modClusterSubsystemBuilder.socketTimeout})"
        rcode = modClusterSubsystemBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterSubsystemBuilder.statusInterval != null) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration:write-attribute(name=status-interval, value=${modClusterSubsystemBuilder.statusInterval})"
        rcode = modClusterSubsystemBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterSubsystemBuilder.stickySessionForce != null) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration:write-attribute(name=sticky-session-force, value=${modClusterSubsystemBuilder.stickySessionForce})"
        rcode = modClusterSubsystemBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterSubsystemBuilder.stickySessionRemove != null) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration:write-attribute(name=sticky-session-remove, value=${modClusterSubsystemBuilder.stickySessionRemove})"
        rcode = modClusterSubsystemBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterSubsystemBuilder.stickySession != null) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration:write-attribute(name=sticky-session, value=${modClusterSubsystemBuilder.stickySession})"
        rcode = modClusterSubsystemBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterSubsystemBuilder.stopContextTimeout != null) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration:write-attribute(name=stop-context-timeout, value=${modClusterSubsystemBuilder.stopContextTimeout})"
        rcode = modClusterSubsystemBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterSubsystemBuilder.ttl != null) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration:write-attribute(name=ttl, value=${modClusterSubsystemBuilder.ttl})"
        rcode = modClusterSubsystemBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterSubsystemBuilder.workerTimeout != null) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration:write-attribute(name=worker-timeout, value=${modClusterSubsystemBuilder.workerTimeout})"
        rcode = modClusterSubsystemBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterSubsystemBuilder.simpleLoadProvider != null) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration:write-attribute(name=simple-load-provider, value=${modClusterSubsystemBuilder.simpleLoadProvider})"
        rcode = modClusterSubsystemBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }
      return rcode
    }
  }

  /**
   * Adds Multicast socket binding
   */
  public static class MulticastBindingBuilder {
    AS7 as7serverInstance
    String name = "modcluster-adv"
    String multicastAddress = DefaultProperties.MODCLUSTER_MCAST_ADDRESS
    Integer port = 0
    Integer multicastPort = Integer.parseInt(DefaultProperties.MODCLUSTER_MCAST_PORT)
    String socketInterface

    private MulticastBindingBuilder() {}

    /**
     * MANDATORY
     * @param name
     * @return
     */
    MulticastBindingBuilder name(String name) {
      this.name = name
      return this
    }

    MulticastBindingBuilder socketInterface(String socketInterface) {
      this.socketInterface = socketInterface
      return this
    }

    /**
     * MANDATORY
     * @param multicastAddress
     * @return
     */
    MulticastBindingBuilder multicastAddress(String multicastAddress) {
      this.multicastAddress = multicastAddress
      return this
    }

    MulticastBindingBuilder multicastPort(Integer multicastPort) {
      this.multicastPort = multicastPort
      return this
    }

    MulticastBindingBuilder port(Integer port) {
      this.port = port
      return this
    }

    /**
     * MANDATORY
     * @param as7serverInstance
     * @return
     */
    MulticastBindingBuilder as7serverInstance(AS7 as7serverInstance) {
      this.as7serverInstance = as7serverInstance
      return this
    }

    public static int build(block) {
      int rcode = 0
      boolean justCreated = false
      final MulticastBindingBuilder multicastBindingBuilder = new MulticastBindingBuilder().with block
      if (multicastBindingBuilder.as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")
      if (StringUtils.isBlank(multicastBindingBuilder.name)) throw new IllegalArgumentException("Name must not be null nor empty.")
      if (!(CLILib.readSocketBindingGroup(multicastBindingBuilder.as7serverInstance) =~ /.*"${multicastBindingBuilder.name}".*/)) {
        log.info("Defining modcluster socket binding ${multicastBindingBuilder.name}")
        if (StringUtils.isBlank(multicastBindingBuilder.multicastAddress)) throw new IllegalArgumentException("MulticastAddress must not be null nor empty.")
        if (multicastBindingBuilder.port == null || multicastBindingBuilder.port < 0) throw new IllegalArgumentException("Port must be bigger than 0.")
        if (multicastBindingBuilder.multicastPort == null || multicastBindingBuilder.multicastPort < 0) throw new IllegalArgumentException("MulticastPort must be bigger than 0.")
        String cmdStr = "/socket-binding-group=standard-sockets/socket-binding=${multicastBindingBuilder.name}:add(multicast-address=${multicastBindingBuilder.multicastAddress}, port=${multicastBindingBuilder.port}, multicast-port=${multicastBindingBuilder.multicastPort})"
        rcode = multicastBindingBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
        justCreated = true
      }
      if (!justCreated) {
        if (multicastBindingBuilder.multicastAddress != null) {
          String cmdStr = "/socket-binding-group=standard-sockets/socket-binding=${multicastBindingBuilder.name}:write-attribute(name=multicast-address,value=${multicastBindingBuilder.multicastAddress})"
          rcode = multicastBindingBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
          if (rcode > 0) {
            log.error("Failure: ${cmdStr}")
            return rcode
          }
        }
        if (multicastBindingBuilder.port != null) {
          String cmdStr = "/socket-binding-group=standard-sockets/socket-binding=${multicastBindingBuilder.name}:write-attribute(name=port,value=${multicastBindingBuilder.port})"
          rcode = multicastBindingBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
          if (rcode > 0) {
            log.error("Failure: ${cmdStr}")
            return rcode
          }
        }
        if (multicastBindingBuilder.multicastPort != null) {
          String cmdStr = "/socket-binding-group=standard-sockets/socket-binding=${multicastBindingBuilder.name}:write-attribute(name=multicast-port,value=${multicastBindingBuilder.multicastPort})"
          rcode = multicastBindingBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
          if (rcode > 0) {
            log.error("Failure: ${cmdStr}")
            return rcode
          }
        }
      }
      if (multicastBindingBuilder.socketInterface != null) {
        String cmdStr = "/socket-binding-group=standard-sockets/socket-binding=${multicastBindingBuilder.name}:write-attribute(name=interface,value=${multicastBindingBuilder.socketInterface})"
        rcode = multicastBindingBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }
      return rcode
    }
  }



  /**
   * Update socket-binding
   */
  public static class SeparateClusterBuilder {
    AS7 as7serverInstance
    int offset = 1
    private static final String JGROUPS_UDP_BINDING_SOCKET = "jgroups-udp"
    private static final String JGROUPS_MPING_BINDING_SOCKET = "jgroups-mping"
    private static final String MULTICAST_PORT = "multicast-port"

    private SeparateClusterBuilder() {}


    /**
     * MANDATORY
     * @param as7serverInstance
     * @return
     */
    SeparateClusterBuilder as7serverInstance(AS7 as7serverInstance) {
      this.as7serverInstance = as7serverInstance
      return this
    }

    SeparateClusterBuilder offset(int offset) {
      this.offset = offset
      return this
    }

    public static int build(block) {
      String cmdStr
      final SeparateClusterBuilder builder = new SeparateClusterBuilder().with block
      log.info("Defining modcluster socket binding")
      if (builder.as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")
      int port = Integer.valueOf(CLILib.readSocketBinding(builder.as7serverInstance, JGROUPS_UDP_BINDING_SOCKET, MULTICAST_PORT))
      cmdStr = "/socket-binding-group=standard-sockets/socket-binding=$JGROUPS_UDP_BINDING_SOCKET:write-attribute(name=$MULTICAST_PORT,value=${port + builder.offset})"
      int rcode = builder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
      if (rcode > 0) {
        log.error("Failure: ${cmdStr}")
        return rcode
      }
      port = Integer.valueOf(CLILib.readSocketBinding(builder.as7serverInstance, JGROUPS_MPING_BINDING_SOCKET, MULTICAST_PORT))
      cmdStr = "/socket-binding-group=standard-sockets/socket-binding=$JGROUPS_MPING_BINDING_SOCKET:write-attribute(name=$MULTICAST_PORT,value=${port + builder.offset})"
      rcode = builder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
      if (rcode > 0) {
        log.error("Failure: ${cmdStr}")
        return rcode
      }
      CLILib.reload(builder.as7serverInstance)
      return rcode
    }
  }

  /**
   * Adds mod_cluster filter
   */
  static class ModClusterFilterBuilder {
    AS7 as7serverInstance
    Integer advertiseFrequency // e.g. 10000
    String advertisePath // e.g. "/"
    String advertiseProtocol // e.g. "http"
    String advertiseSocketBinding // e.g. "modcluster-adv" @See MulticastBindingBuilder
    String managementSocketBinding // e.g. "http"
    String managementAccessPredicate // e.g. "undefined"
    Integer brokenNodeTimeout // e.g. 60000
    Integer cachedConnectionsPerThread // e.g. 5
    Integer connectionIdleTimeout // e.g. 60
    Integer connectionsPerThread // e.g. 10
    Integer healthCheckInterval // e.g. 10000
    Integer maxRequestTime // e.g. -1
    Integer requestQueueSize // e.g. 10
    String securityKey // e.g. "undefined"
    String securityRealm // e.g. "undefined"
    String worker // e.g. "default"
    Boolean useAlias // e.g. False
    String failoverStrategy // e.g. "undefined"
    String sslContext
    Boolean enableHttp2
    Boolean http2EnablePush // true
    Integer http2HeaderTableSize// 4096
    Integer http2InitialWindowSize // 65535
    Integer http2MaxConcurrentStreams
    Integer http2MaxFrameSize // 16384
    Integer http2MaxHeaderListSize
    Integer maxAjpPacketSize // 8192
    Integer maxRetries // 1

    private ModClusterFilterBuilder() {}

    enum FailoverStrategy {
      Deterministic('DETERMINISTIC'), LoadBalanced('LOAD_BALANCED')

      String value

      FailoverStrategy(String strategy) {
        value = strategy
      }
    }

    /*
    * OMG, why don't we use missingMethod instead of this chunk of useless code?
    * Well, until IDE masters missingMethod runtime stuff, this is better for a user. */

    ModClusterFilterBuilder advertiseFrequency(Integer advertiseFrequency) {
      this.advertiseFrequency = advertiseFrequency
      return this
    }

    ModClusterFilterBuilder advertisePath(String advertisePath) {
      this.advertisePath = advertisePath
      return this
    }

    ModClusterFilterBuilder advertiseProtocol(String advertiseProtocol) {
      this.advertiseProtocol = advertiseProtocol
      return this
    }

    ModClusterFilterBuilder advertiseSocketBinding(String advertiseSocketBinding) {
      this.advertiseSocketBinding = advertiseSocketBinding
      return this
    }

    ModClusterFilterBuilder managementSocketBinding(String managementSocketBinding) {
      this.managementSocketBinding = managementSocketBinding
      return this
    }

    ModClusterFilterBuilder managementAccessPredicate(String managementAccessPredicate) {
      this.managementAccessPredicate = managementAccessPredicate
      return this
    }

    ModClusterFilterBuilder brokenNodeTimeout(Integer brokenNodeTimeout) {
      this.brokenNodeTimeout = brokenNodeTimeout
      return this
    }

    ModClusterFilterBuilder cachedConnectionsPerThread(Integer cachedConnectionsPerThread) {
      this.cachedConnectionsPerThread = cachedConnectionsPerThread
      return this
    }

    ModClusterFilterBuilder connectionIdleTimeout(Integer connectionIdleTimeout) {
      this.connectionIdleTimeout = connectionIdleTimeout
      return this
    }

    ModClusterFilterBuilder connectionsPerThread(Integer connectionsPerThread) {
      this.connectionsPerThread = connectionsPerThread
      return this
    }

    ModClusterFilterBuilder healthCheckInterval(Integer healthCheckInterval) {
      this.healthCheckInterval = healthCheckInterval
      return this
    }

    ModClusterFilterBuilder maxRequestTime(Integer maxRequestTime) {
      this.maxRequestTime = maxRequestTime
      return this
    }

    ModClusterFilterBuilder requestQueueSize(Integer requestQueueSize) {
      this.requestQueueSize = requestQueueSize
      return this
    }

    ModClusterFilterBuilder securityKey(String securityKey) {
      this.securityKey = securityKey
      return this
    }

    ModClusterFilterBuilder securityRealm(String securityRealm) {
      this.securityRealm = securityRealm
      return this
    }

    ModClusterFilterBuilder worker(String worker) {
      this.worker = worker
      return this
    }

    ModClusterFilterBuilder useAlias(Boolean useAlias) {
      this.useAlias = useAlias
      return this
    }

    ModClusterFilterBuilder failoverStrategy(FailoverStrategy failoverStrategy) {
      this.failoverStrategy = failoverStrategy.value
      return this
    }

    ModClusterFilterBuilder sslContext(String sslContext) {
      this.sslContext = sslContext
      return this
    }

    ModClusterFilterBuilder enableHttp2(Boolean enableHttp2) {
      this.enableHttp2 = enableHttp2
      return this
    }

    ModClusterFilterBuilder http2EnablePush(Boolean http2EnablePush) {
      this.http2EnablePush = http2EnablePush
      return this
    }

    ModClusterFilterBuilder http2HeaderTableSize(Integer http2HeaderTableSize) {
      this.http2HeaderTableSize = http2HeaderTableSize
      return this
    }

    ModClusterFilterBuilder http2InitialWindowSize(Integer http2InitialWindowSize) {
      this.http2InitialWindowSize = http2InitialWindowSize
      return this
    }

    ModClusterFilterBuilder http2MaxConcurrentStreams(Integer http2MaxConcurrentStreams) {
      this.http2MaxConcurrentStreams = http2MaxConcurrentStreams
      return this
    }

    ModClusterFilterBuilder http2MaxFrameSize(Integer http2MaxFrameSize) {
      this.http2MaxFrameSize = http2MaxFrameSize
      return this
    }

    ModClusterFilterBuilder http2MaxHeaderListSize(Integer http2MaxHeaderListSize) {
      this.http2MaxHeaderListSize = http2MaxHeaderListSize
      return this
    }

    ModClusterFilterBuilder maxAjpPacketSize(Integer maxAjpPacketSize) {
      this.maxAjpPacketSize = maxAjpPacketSize
      return this
    }

    ModClusterFilterBuilder maxRetries(Integer maxRetries) {
      this.maxRetries = maxRetries
      return this
    }

    /**
     * MANDATORY
     * @param as7serverInstance
     * @return
     */
    ModClusterFilterBuilder as7serverInstance(AS7 as7serverInstance) {
      this.as7serverInstance = as7serverInstance
      return this
    }

    static int build(block) {
      final ModClusterFilterBuilder modClusterFilterBuilder = new ModClusterFilterBuilder().with block
      // Mandatory stuff
      if (modClusterFilterBuilder.as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")
      // First, we need to figure whether the resource exists
      int rcode = 0
      boolean justCreated = false
      String cmdStr = "/subsystem=undertow/configuration=filter:read-resource(include-runtime=true, recursive=true, recursive-depth=100)"
      if (CLILib.readArbitraryCommandOutput(modClusterFilterBuilder.as7serverInstance, cmdStr) =~ /mod-cluster[" =>]*undefined/) {
        // Mandatory minimum
        if (StringUtils.isBlank(modClusterFilterBuilder.managementSocketBinding)) throw new IllegalArgumentException("ManagementSocketBinding must not be null nor empty.")
        if (StringUtils.isBlank(modClusterFilterBuilder.advertiseSocketBinding)) throw new IllegalArgumentException("AdvertiseSocketBinding must not be null nor empty.")
        // Create mod-cluster filter
        cmdStr = "/subsystem=undertow/configuration=filter/mod-cluster=modcluster:add(management-socket-binding=${modClusterFilterBuilder.managementSocketBinding},advertise-socket-binding=${modClusterFilterBuilder.advertiseSocketBinding})"
        rcode = modClusterFilterBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        // If there was an error, we cannot continue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
        // Add filter to the host
        cmdStr = "/subsystem=undertow/server=default-server/host=default-host/filter-ref=modcluster:add()"
        rcode = modClusterFilterBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
        justCreated = true
      }

      if (modClusterFilterBuilder.advertiseFrequency != null) {
        cmdStr = "/subsystem=undertow/configuration=filter/mod-cluster=modcluster:write-attribute(name=advertise-frequency,value=${modClusterFilterBuilder.advertiseFrequency})"
        rcode = modClusterFilterBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!StringUtils.isBlank(modClusterFilterBuilder.advertisePath)) {
        cmdStr = "/subsystem=undertow/configuration=filter/mod-cluster=modcluster:write-attribute(name=advertise-path,value=${modClusterFilterBuilder.advertisePath})"
        rcode = modClusterFilterBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!StringUtils.isBlank(modClusterFilterBuilder.sslContext)) {
        cmdStr = "/subsystem=undertow/configuration=filter/mod-cluster=modcluster:write-attribute(name=ssl-context,value=${modClusterFilterBuilder.sslContext})"
        rcode = modClusterFilterBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!StringUtils.isBlank(modClusterFilterBuilder.advertiseProtocol)) {
        cmdStr = "/subsystem=undertow/configuration=filter/mod-cluster=modcluster:write-attribute(name=advertise-protocol,value=${modClusterFilterBuilder.advertiseProtocol})"
        rcode = modClusterFilterBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!justCreated && !StringUtils.isBlank(modClusterFilterBuilder.advertiseSocketBinding)) {
        cmdStr = "/subsystem=undertow/configuration=filter/mod-cluster=modcluster:write-attribute(name=advertise-socket-binding,value=${modClusterFilterBuilder.advertiseSocketBinding})"
        rcode = modClusterFilterBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!justCreated && !StringUtils.isBlank(modClusterFilterBuilder.managementSocketBinding)) {
        cmdStr = "/subsystem=undertow/configuration=filter/mod-cluster=modcluster:write-attribute(name=management-socket-binding,value=${modClusterFilterBuilder.managementSocketBinding})"
        rcode = modClusterFilterBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!StringUtils.isBlank(modClusterFilterBuilder.managementAccessPredicate)) {
        cmdStr = "/subsystem=undertow/configuration=filter/mod-cluster=modcluster:write-attribute(name=management-access-predicate,value=${modClusterFilterBuilder.managementAccessPredicate})"
        rcode = modClusterFilterBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!StringUtils.isBlank(modClusterFilterBuilder.failoverStrategy)) {
        cmdStr = "/subsystem=undertow/configuration=filter/mod-cluster=modcluster:write-attribute(name=failover-strategy,value=${modClusterFilterBuilder.failoverStrategy})"
        rcode = modClusterFilterBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterFilterBuilder.brokenNodeTimeout != null) {
        cmdStr = "/subsystem=undertow/configuration=filter/mod-cluster=modcluster:write-attribute(name=broken-node-timeout,value=${modClusterFilterBuilder.brokenNodeTimeout})"
        rcode = modClusterFilterBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterFilterBuilder.cachedConnectionsPerThread != null) {
        cmdStr = "/subsystem=undertow/configuration=filter/mod-cluster=modcluster:write-attribute(name=cached-connections-per-thread,value=${modClusterFilterBuilder.cachedConnectionsPerThread})"
        rcode = modClusterFilterBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterFilterBuilder.connectionIdleTimeout != null) {
        cmdStr = "/subsystem=undertow/configuration=filter/mod-cluster=modcluster:write-attribute(name=connection-idle-timeout,value=${modClusterFilterBuilder.connectionIdleTimeout})"
        rcode = modClusterFilterBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterFilterBuilder.connectionsPerThread != null) {
        cmdStr = "/subsystem=undertow/configuration=filter/mod-cluster=modcluster:write-attribute(name=connections-per-thread,value=${modClusterFilterBuilder.connectionsPerThread})"
        rcode = modClusterFilterBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterFilterBuilder.healthCheckInterval != null) {
        cmdStr = "/subsystem=undertow/configuration=filter/mod-cluster=modcluster:write-attribute(name=health-check-interval,value=${modClusterFilterBuilder.healthCheckInterval})"
        rcode = modClusterFilterBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterFilterBuilder.maxRequestTime != null) {
        cmdStr = "/subsystem=undertow/configuration=filter/mod-cluster=modcluster:write-attribute(name=max-request-time,value=${modClusterFilterBuilder.maxRequestTime})"
        rcode = modClusterFilterBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterFilterBuilder.requestQueueSize != null) {
        cmdStr = "/subsystem=undertow/configuration=filter/mod-cluster=modcluster:write-attribute(name=request-queue-size,value=${modClusterFilterBuilder.requestQueueSize})"
        rcode = modClusterFilterBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!StringUtils.isBlank(modClusterFilterBuilder.securityKey)) {
        cmdStr = "/subsystem=undertow/configuration=filter/mod-cluster=modcluster:write-attribute(name=security-key,value=${modClusterFilterBuilder.securityKey})"
        rcode = modClusterFilterBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!StringUtils.isBlank(modClusterFilterBuilder.securityRealm)) {
        cmdStr = "/subsystem=undertow/configuration=filter/mod-cluster=modcluster:write-attribute(name=security-realm,value=${modClusterFilterBuilder.securityRealm})"
        rcode = modClusterFilterBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!StringUtils.isBlank(modClusterFilterBuilder.worker)) {
        cmdStr = "/subsystem=undertow/configuration=filter/mod-cluster=modcluster:write-attribute(name=worker,value=${modClusterFilterBuilder.worker})"
        rcode = modClusterFilterBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterFilterBuilder.useAlias != null) {
        cmdStr = "/subsystem=undertow/configuration=filter/mod-cluster=modcluster:write-attribute(name=use-alias,value=${modClusterFilterBuilder.useAlias})"
        rcode = modClusterFilterBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterFilterBuilder.enableHttp2 != null) {
        cmdStr = "/subsystem=undertow/configuration=filter/mod-cluster=modcluster:write-attribute(name=enable-http2,value=${modClusterFilterBuilder.enableHttp2})"
        rcode = modClusterFilterBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterFilterBuilder.http2EnablePush != null) {
        cmdStr = "/subsystem=undertow/configuration=filter/mod-cluster=modcluster:write-attribute(name=http2-enable-push,value=${modClusterFilterBuilder.http2EnablePush})"
        rcode = modClusterFilterBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterFilterBuilder.http2HeaderTableSize != null) {
        cmdStr = "/subsystem=undertow/configuration=filter/mod-cluster=modcluster:write-attribute(name=http2-header-table-size,value=${modClusterFilterBuilder.http2HeaderTableSize})"
        rcode = modClusterFilterBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterFilterBuilder.http2InitialWindowSize != null) {
        cmdStr = "/subsystem=undertow/configuration=filter/mod-cluster=modcluster:write-attribute(name=http2-initial-window-size,value=${modClusterFilterBuilder.http2InitialWindowSize})"
        rcode = modClusterFilterBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterFilterBuilder.http2MaxConcurrentStreams != null) {
        cmdStr = "/subsystem=undertow/configuration=filter/mod-cluster=modcluster:write-attribute(name=http2-max-concurrent-streams,value=${modClusterFilterBuilder.http2MaxConcurrentStreams})"
        rcode = modClusterFilterBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterFilterBuilder.http2MaxFrameSize != null) {
        cmdStr = "/subsystem=undertow/configuration=filter/mod-cluster=modcluster:write-attribute(name=http2-max-frame-size,value=${modClusterFilterBuilder.http2MaxFrameSize})"
        rcode = modClusterFilterBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterFilterBuilder.http2MaxHeaderListSize != null) {
        cmdStr = "/subsystem=undertow/configuration=filter/mod-cluster=modcluster:write-attribute(name=http2-max-header-list-size,value=${modClusterFilterBuilder.http2MaxHeaderListSize})"
        rcode = modClusterFilterBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterFilterBuilder.maxAjpPacketSize != null) {
        cmdStr = "/subsystem=undertow/configuration=filter/mod-cluster=modcluster:write-attribute(name=max-ajp-packet-size,value=${modClusterFilterBuilder.maxAjpPacketSize})"
        rcode = modClusterFilterBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (modClusterFilterBuilder.maxRetries != null) {
        cmdStr = "/subsystem=undertow/configuration=filter/mod-cluster=modcluster:write-attribute(name=max-retries,value=${modClusterFilterBuilder.maxRetries})"
        rcode = modClusterFilterBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      //last result
      return rcode
    }
  }

  /**
   * Adds SSL mod_cluster subsystem configuration
   */
  public static class ModClusterSSLBuilder {
    AS7 as7serverInstance
    String caCertificateFile
    String caRevocationUrl
    String certificateKeyFile
    //TODO mod_clustrer bug, should be LIST
    String cipherSuite
    String keyAlias
    String password
    String protocol

    private ModClusterSSLBuilder() {}

    /**
     * MANDATORY
     * @param caCertificateFile
     * @return
     */
    ModClusterSSLBuilder caCertificateFile(String caCertificateFile) {
      this.caCertificateFile = caCertificateFile
      return this
    }

    ModClusterSSLBuilder caRevocationUrl(String caRevocationUrl) {
      this.caRevocationUrl = caRevocationUrl
      return this
    }

    /**
     * MANDATORY
     * @param certificateKeyFile
     * @return
     */
    ModClusterSSLBuilder certificateKeyFile(String certificateKeyFile) {
      this.certificateKeyFile = certificateKeyFile
      return this
    }

    ModClusterSSLBuilder cipherSuite(String cipherSuite) {
      this.cipherSuite = cipherSuite
      return this
    }

    /**
     * MANDATORY
     * @param keyAlias
     * @return
     */
    ModClusterSSLBuilder keyAlias(String keyAlias) {
      this.keyAlias = keyAlias
      return this
    }

    ModClusterSSLBuilder password(String password) {
      this.password = password
      return this
    }

    ModClusterSSLBuilder protocol(String protocol) {
      this.protocol = protocol
      return this
    }

    /**
     * MANDATORY
     * @param as7serverInstance
     * @return
     */
    ModClusterSSLBuilder as7serverInstance(AS7 as7serverInstance) {
      this.as7serverInstance = as7serverInstance
      return this
    }

    public static int build(block) {
      final ModClusterSSLBuilder modClusterSSLBuilder = new ModClusterSSLBuilder().with block
      if (modClusterSSLBuilder.as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")
      int rcode = 0
      String cmdStr
      if (CLILib.readModClusterSubsystem(modClusterSSLBuilder.as7serverInstance) =~ /[[^\n]*\n]*ssl[ "=>\{\n]*undefined/) {
        //Add it
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration/ssl=configuration:add()"
        rcode = modClusterSSLBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!StringUtils.isBlank(modClusterSSLBuilder.caCertificateFile)) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration/ssl=configuration:write-attribute(name=ca-certificate-file, value=${CLILib.escapeQuotes(modClusterSSLBuilder.caCertificateFile.replace('\\','/'))})"
        rcode = modClusterSSLBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!StringUtils.isBlank(modClusterSSLBuilder.caRevocationUrl)) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration/ssl=configuration:write-attribute(name=ca-revocation-url, value=${CLILib.escapeQuotes(modClusterSSLBuilder.caRevocationUrl)})"
        rcode = modClusterSSLBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!StringUtils.isBlank(modClusterSSLBuilder.certificateKeyFile)) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration/ssl=configuration:write-attribute(name=certificate-key-file, value=${CLILib.escapeQuotes(modClusterSSLBuilder.certificateKeyFile.replace('\\','/'))})"
        rcode = modClusterSSLBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!StringUtils.isBlank(modClusterSSLBuilder.cipherSuite)) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration/ssl=configuration:write-attribute(name=cipher-suite,value=${CLILib.escapeQuotes(modClusterSSLBuilder.cipherSuite)})"
        rcode = modClusterSSLBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!StringUtils.isBlank(modClusterSSLBuilder.keyAlias)) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration/ssl=configuration:write-attribute(name=key-alias, value=${CLILib.escapeQuotes(modClusterSSLBuilder.keyAlias)})"
        rcode = modClusterSSLBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!StringUtils.isBlank(modClusterSSLBuilder.password)) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration/ssl=configuration:write-attribute(name=password, value=${CLILib.escapeQuotes(modClusterSSLBuilder.password)})"
        rcode = modClusterSSLBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!StringUtils.isBlank(modClusterSSLBuilder.protocol)) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration/ssl=configuration:write-attribute(name=protocol, value=${CLILib.escapeQuotes(modClusterSSLBuilder.protocol)})"
        rcode = modClusterSSLBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      return rcode
    }
  }

  /**
   * Adds Security Realm
   */
  public static class SecurityRealmBuilder {
    AS7 as7serverInstance
    String name
    String serverIdentity = "ssl"
    String authentication = "truststore"
    String keystoreAbsolutePath
    String keystorePassword
    String keystoreAlias
    String truststoreAbsolutePath
    String truststorePassword
    List<String> enabledCipherSuites // e.g. [SSL_RSA_WITH_DES_CBC_SHA, SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA]
    List<String> enabledProtocols // e.g. [TLSv1.1, TLSv1.2]
    String keystoreKeyPassword
    String keystoreProvider // e.g. JKS
    String protocol // e.g. TLS

    private SecurityRealmBuilder() {}

    /**
     * MANDATORY
     * @param name
     * @return
     */
    SecurityRealmBuilder name(String name) {
      this.name = name
      return this
    }

    SecurityRealmBuilder serverIdentity(String serverIdentity) {
      this.serverIdentity = serverIdentity
      return this
    }

    SecurityRealmBuilder keystoreAbsolutePath(String keystoreAbsolutePath) {
      this.keystoreAbsolutePath = keystoreAbsolutePath
      return this
    }

    SecurityRealmBuilder authentication(String authentication) {
      this.authentication = authentication
      return this
    }

    SecurityRealmBuilder keystorePassword(String keystorePassword) {
      this.keystorePassword = keystorePassword
      return this
    }

    SecurityRealmBuilder keystoreAlias(String keystoreAlias) {
      this.keystoreAlias = keystoreAlias
      return this
    }

    SecurityRealmBuilder truststoreAbsolutePath(String truststoreAbsolutePath) {
      this.truststoreAbsolutePath = truststoreAbsolutePath
      return this
    }

    SecurityRealmBuilder truststorePassword(String truststorePassword) {
      this.truststorePassword = truststorePassword
      return this
    }

    SecurityRealmBuilder enabledCipherSuites(List<String> enabledCipherSuites) {
      this.enabledCipherSuites = enabledCipherSuites
      return this
    }

    SecurityRealmBuilder enabledProtocols(List<String> enabledProtocols) {
      this.enabledProtocols = enabledProtocols
      return this
    }

    SecurityRealmBuilder keystoreKeyPassword(String keystoreKeyPassword) {
      this.keystoreKeyPassword = keystoreKeyPassword
      return this
    }

    SecurityRealmBuilder keystoreProvider(String keystoreProvider) {
      this.keystoreProvider = keystoreProvider
      return this
    }

    SecurityRealmBuilder protocol(String protocol) {
      this.protocol = protocol
      return this
    }

    /**
     * MANDATORY
     * @param as7serverInstance
     * @return
     */
    SecurityRealmBuilder as7serverInstance(AS7 as7serverInstance) {
      this.as7serverInstance = as7serverInstance
      return this
    }

    public static int build(block) {
      final SecurityRealmBuilder securityRealmBuilder = new SecurityRealmBuilder().with block
      if (securityRealmBuilder.as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")
      if (StringUtils.isBlank(securityRealmBuilder.name)) throw new IllegalArgumentException("Name must not be null nor empty.")
      if (StringUtils.isBlank(securityRealmBuilder.serverIdentity)) throw new IllegalArgumentException("ServerIdentity must not be null nor empty.")
      if (StringUtils.isBlank(securityRealmBuilder.authentication)) throw new IllegalArgumentException("Authentication must not be null nor empty.")

      int rcode = 0
      // Isn't the Realm present? If it isn't, add it.
      String cmdStr
      final String cmdStrReadMgt = "/core-service=management:read-resource(recursive=true,recursive-depth=1)"
      if (!(CLILib.readArbitraryCommandOutput(securityRealmBuilder.as7serverInstance, cmdStrReadMgt) =~ /${securityRealmBuilder.name}[ =>\{]*/)) {
        //Add it
        cmdStr = "/core-service=management/security-realm=${securityRealmBuilder.name}:add()"
        rcode = securityRealmBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      boolean authJustAdded = false
      // Is realm's authentication undefined?
      if (CLILib.readArbitraryCommandOutput(securityRealmBuilder.as7serverInstance, cmdStrReadMgt) =~ /${securityRealmBuilder.name}[[^\n]*\n]*authentication[ "=>\{\n]*undefined/) {
        if (StringUtils.isBlank(securityRealmBuilder.truststoreAbsolutePath) || StringUtils.isBlank(securityRealmBuilder.truststorePassword)) throw new IllegalArgumentException("One must set both TruststoreAbsolutePath and TruststorePassword.")
        cmdStr = "/core-service=management/security-realm=${securityRealmBuilder.name}/authentication=${securityRealmBuilder.authentication}:add(keystore-password=${CLILib.escapeQuotes(securityRealmBuilder.truststorePassword)}, keystore-path=${CLILib.escapeQuotes(securityRealmBuilder.truststoreAbsolutePath.replace('\\','/'))})"
        rcode = securityRealmBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
        authJustAdded = true
      }

      if (!authJustAdded && !StringUtils.isBlank(securityRealmBuilder.truststoreAbsolutePath)) {
        cmdStr = "/core-service=management/security-realm=${securityRealmBuilder.name}/authentication=${securityRealmBuilder.authentication}:write-attribute(name=keystore-path,value=${CLILib.escapeQuotes(securityRealmBuilder.truststoreAbsolutePath.replace('\\','/'))})"
        rcode = securityRealmBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!authJustAdded && !StringUtils.isBlank(securityRealmBuilder.truststorePassword)) {
        cmdStr = "/core-service=management/security-realm=${securityRealmBuilder.name}/authentication=${securityRealmBuilder.authentication}:write-attribute(name=keystore-password,value=${CLILib.escapeQuotes(securityRealmBuilder.truststorePassword)})"
        rcode = securityRealmBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      boolean identityJustAdded = false
      // Is realm's server identity undefined?
      if (CLILib.readArbitraryCommandOutput(securityRealmBuilder.as7serverInstance, cmdStrReadMgt) =~ /${securityRealmBuilder.name}[[^\n]*\n]*server-identity[ "=>\{\n]*undefined/) {
        if (StringUtils.isBlank(securityRealmBuilder.keystoreAbsolutePath) ||
            StringUtils.isBlank(securityRealmBuilder.keystorePassword) ||
            StringUtils.isBlank(securityRealmBuilder.keystoreAlias)) throw new IllegalArgumentException("One must set all KeystoreAbsolutePath, KeystoreAlias and KeystorePassword.")
        cmdStr = "/core-service=management/security-realm=${securityRealmBuilder.name}/server-identity=${securityRealmBuilder.serverIdentity}"
        cmdStr <<= ":add(keystore-password=${CLILib.escapeQuotes(securityRealmBuilder.keystorePassword)}, keystore-path=${CLILib.escapeQuotes(securityRealmBuilder.keystoreAbsolutePath.replace('\\','/'))}, alias=${CLILib.escapeQuotes(securityRealmBuilder.keystoreAlias)})"
        rcode = securityRealmBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
        identityJustAdded = true
      }

      if (!identityJustAdded && !StringUtils.isBlank(securityRealmBuilder.keystoreAbsolutePath)) {
        cmdStr = "/core-service=management/security-realm=${securityRealmBuilder.name}/server-identity=${securityRealmBuilder.serverIdentity}:write-attribute(name=keystore-path, value=${CLILib.escapeQuotes(securityRealmBuilder.keystoreAbsolutePath.replace('\\','/'))})"
        rcode = securityRealmBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!identityJustAdded && !StringUtils.isBlank(securityRealmBuilder.keystorePassword)) {
        cmdStr = "/core-service=management/security-realm=${securityRealmBuilder.name}/server-identity=${securityRealmBuilder.serverIdentity}:write-attribute(name=keystore-password, value=${CLILib.escapeQuotes(securityRealmBuilder.keystorePassword)})"
        rcode = securityRealmBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!identityJustAdded && !StringUtils.isBlank(securityRealmBuilder.keystoreAlias)) {
        cmdStr = "/core-service=management/security-realm=${securityRealmBuilder.name}/server-identity=${securityRealmBuilder.serverIdentity}:write-attribute(name=alias, value=${CLILib.escapeQuotes(securityRealmBuilder.keystoreAlias)})"
        rcode = securityRealmBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!CollectionUtils.isEmpty(securityRealmBuilder.enabledCipherSuites)) {
        cmdStr = "/core-service=management/security-realm=${securityRealmBuilder.name}/server-identity=${securityRealmBuilder.serverIdentity}:write-attribute(name=enabled-cipher-suites, value=${CLILib.escapeQuotes(securityRealmBuilder.enabledCipherSuites)})"
        rcode = securityRealmBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!CollectionUtils.isEmpty(securityRealmBuilder.enabledProtocols)) {
        cmdStr = "/core-service=management/security-realm=${securityRealmBuilder.name}/server-identity=${securityRealmBuilder.serverIdentity}:write-attribute(name=enabled-protocols, value=${securityRealmBuilder.enabledProtocols})"
        rcode = securityRealmBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!StringUtils.isBlank(securityRealmBuilder.keystoreKeyPassword)) {
        cmdStr = "/core-service=management/security-realm=${securityRealmBuilder.name}/server-identity=${securityRealmBuilder.serverIdentity}:write-attribute(name=key-password, value=${CLILib.escapeQuotes(securityRealmBuilder.keystoreKeyPassword)})"
        rcode = securityRealmBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!StringUtils.isBlank(securityRealmBuilder.keystoreProvider)) {
        cmdStr = "/core-service=management/security-realm=${securityRealmBuilder.name}/server-identity=${securityRealmBuilder.serverIdentity}:write-attribute(name=keystore-provider, value=${CLILib.escapeQuotes(securityRealmBuilder.keystoreProvider)})"
        rcode = securityRealmBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!StringUtils.isBlank(securityRealmBuilder.protocol)) {
        cmdStr = "/core-service=management/security-realm=${securityRealmBuilder.name}/server-identity=${securityRealmBuilder.serverIdentity}:write-attribute(name=protocol, value=${CLILib.escapeQuotes(securityRealmBuilder.protocol)})"
        rcode = securityRealmBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      return rcode
    }
  }

  /**
   * Add Custom Load Metric
   */
  public static class AddCustomLoadMetricBuilder {
    AS7 as7serverInstance
    Float capacity
    String clazz
    Map properties
    Integer weight
    Integer history
    Integer decay

    private AddCustomLoadMetricBuilder() {}

    /**
     * MANDATORY
     * @param as7serverInstance
     * @return
     */
    AddCustomLoadMetricBuilder as7serverInstance(AS7 as7serverInstance) {
      this.as7serverInstance = as7serverInstance
      return this
    }

    /**
     * MANDATORY
     * @param clazz
     * @return
     */
    AddCustomLoadMetricBuilder clazz(String clazz) {
      this.clazz = clazz
      return this
    }

    AddCustomLoadMetricBuilder capacity(Float capacity) {
      this.capacity = capacity
      return this
    }

    AddCustomLoadMetricBuilder properties(Map properties) {
      this.properties = properties
      return this
    }

    AddCustomLoadMetricBuilder weight(Integer weight) {
      this.weight = weight
      return this
    }

    AddCustomLoadMetricBuilder history(Integer history) {
      this.history = history
      return this
    }

    AddCustomLoadMetricBuilder decay(Integer decay) {
      this.decay = decay
      return this
    }

    public static int build(block) {
      final AddCustomLoadMetricBuilder addCustomLoadMetricBuilder = new AddCustomLoadMetricBuilder().with block
      if (addCustomLoadMetricBuilder.as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")
      if (StringUtils.isBlank(addCustomLoadMetricBuilder.clazz)) throw new IllegalArgumentException("Class must be set.")

      int rcode = 0
      String cmdStr
      if (CLILib.readModClusterSubsystem(addCustomLoadMetricBuilder.as7serverInstance) =~ /[[^\n]*\n]*dynamic-load-provider[ "=>\{\n]*undefined/) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration/dynamic-load-provider=configuration:add()"
        rcode = addCustomLoadMetricBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!(CLILib.readModClusterSubsystem(addCustomLoadMetricBuilder.as7serverInstance) =~ /[[^\n]*\n]*custom-load-metric[ "=>\{\n]*${addCustomLoadMetricBuilder.clazz}/)) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration:add-custom-metric(class=${addCustomLoadMetricBuilder.clazz})"
        rcode = addCustomLoadMetricBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (addCustomLoadMetricBuilder.decay != null) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration/dynamic-load-provider=configuration:write-attribute(name=decay, value=${addCustomLoadMetricBuilder.decay})"
        rcode = addCustomLoadMetricBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (addCustomLoadMetricBuilder.history != null) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration/dynamic-load-provider=configuration:write-attribute(name=history, value=${addCustomLoadMetricBuilder.history})"
        rcode = addCustomLoadMetricBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (addCustomLoadMetricBuilder.capacity != null) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration/dynamic-load-provider=configuration/custom-load-metric=${addCustomLoadMetricBuilder.clazz}:write-attribute(name=capacity, value=${addCustomLoadMetricBuilder.capacity})"
        rcode = addCustomLoadMetricBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (addCustomLoadMetricBuilder.weight != null) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration/dynamic-load-provider=configuration/custom-load-metric=${addCustomLoadMetricBuilder.clazz}:write-attribute(name=weight, value=${addCustomLoadMetricBuilder.weight})"
        rcode = addCustomLoadMetricBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!CollectionUtils.isEmpty(addCustomLoadMetricBuilder.properties)) {
        for (String key : addCustomLoadMetricBuilder.properties.keySet()) {
          cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration/dynamic-load-provider=configuration/custom-load-metric=${addCustomLoadMetricBuilder.clazz}:write-attribute(name=property.${key}, value=${CLILib.escapeQuotes(addCustomLoadMetricBuilder.properties.get(key))})"
          rcode = addCustomLoadMetricBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
          if (rcode > 0) {
            log.error("Failure: ${cmdStr}")
            return rcode
          }
        }
      }

      return rcode
    }
  }

  /**
   * Remove custom load metric
   */
  public static class RemoveCustomLoadMetricBuilder {
    AS7 as7serverInstance
    String clazz

    private RemoveCustomLoadMetricBuilder() {}

    /**
     * MANDATORY
     * @param clazz
     * @return
     */
    RemoveCustomLoadMetricBuilder clazz(String clazz) {
      this.clazz = clazz
      return this
    }

    /**
     * MANDATORY
     * @param as7serverInstance
     * @return
     */
    RemoveCustomLoadMetricBuilder as7serverInstance(AS7 as7serverInstance) {
      this.as7serverInstance = as7serverInstance
      return this
    }

    public static int build(block) {
      final RemoveCustomLoadMetricBuilder removeCustomLoadMetricBuilder = new RemoveCustomLoadMetricBuilder().with block
      if (removeCustomLoadMetricBuilder.as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")
      if (StringUtils.isBlank(removeCustomLoadMetricBuilder.clazz)) throw new IllegalArgumentException("Clazz must not be null nor empty.")
      return removeCustomLoadMetricBuilder.as7serverInstance.as7Cli.runArbitraryCommand("/subsystem=modcluster/mod-cluster-config=configuration:remove-custom-metric(class=${removeCustomLoadMetricBuilder.clazz})").exitValue
    }
  }

  /**
   * Build elytron client-ssl-context with key-store, key-manager and trust-manager
   *
   * @param protocols, List of protocols separated with ",", example => "TLSv1, TLSv2"
   */
  static class ClientSSLContextBuilder {
    AS7 as7serverInstance //MANDATORY

    String keyManagerName = null // If set, keyManager will be created and added to client-ssl-context
    String trustManagerName = null // If set, trustManagers will be created and added to client-ssl-context
    String clientSSLContextName = "clientSSLContext"
    String protocols = null

    private ClientSSLContextBuilder() {}

    /**
     * MANDATORY
     */
    ClientSSLContextBuilder as7serverInstance(AS7 as7serverInstance) {
      this.as7serverInstance = as7serverInstance
      return this
    }
    ClientSSLContextBuilder trustManagerName(String trustManagerName) {
      this.trustManagerName = trustManagerName
      return this
    }
    ClientSSLContextBuilder protocols(String protocols) {
      this.protocols = protocols
      return this
    }
    ClientSSLContextBuilder clientSSLContextName(String clientSSLContextName) {
      this.clientSSLContextName = clientSSLContextName
      return this
    }
    ClientSSLContextBuilder keyManagerName(String keyManagerName) {
      this.keyManagerName = keyManagerName
      return this
    }
    static int build(block) {
      final ClientSSLContextBuilder builder = new ClientSSLContextBuilder().with block
      if (builder.as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")
      String cmdStr = "/subsystem=elytron:read-children-names(child-type=client-ssl-context)"
      Map output
      int rcode
      if (!(builder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).stdOut ==~ /[\s\S]*"result" => .*${builder.clientSSLContextName}[\s\S]*/)) {
        cmdStr = "/subsystem=elytron/client-ssl-context=${builder.clientSSLContextName}:add()"
        output = builder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr)
        rcode = output.exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}, ${output}")
          return rcode
        }
      }
      if (builder.keyManagerName) {
        cmdStr = "/subsystem=elytron/client-ssl-context=${builder.clientSSLContextName}:write-attribute(name=key-manager, value=${builder.keyManagerName}"
        output = builder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr)
        rcode = output.exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}, ${output}")
          return rcode
        }
      }
      if (builder.trustManagerName) {
        cmdStr = "/subsystem=elytron/client-ssl-context=${builder.clientSSLContextName}:write-attribute(name=trust-manager, value=${builder.trustManagerName}"
        output = builder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr)
        rcode = output.exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}, ${output}")
          return rcode
        }
      }
      if (builder.protocols) {
        cmdStr = "/subsystem=elytron/client-ssl-context=${builder.clientSSLContextName}:write-attribute(name=protocols, value=[${builder.protocols}]"
        output = builder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr)
        rcode = output.exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}, ${output}")
          return rcode
        }
      }
      return 0
    }
  }

  /**
   * Build elytron key-store and trust-manager
   */
  static class TrustManagerBuilder {
    AS7 as7serverInstance //MANDATORY

    String trustManagerName = "trustManager"
    String keyStoreName = "trustKeyStore"
    String keyStorePassword = null //MANDATORY for build
    String keyStorePath = null //MANDATORY for build if keystore desn't exists
    String keyStoreType = null //MANDATORY for build if keystore desn't exists
    Boolean keyStoreRequired = true
    String algorithm = "PKIX"
    Map<String, String> options

    private TrustManagerBuilder() {}

    /**
     * MANDATORY
     */
    TrustManagerBuilder as7serverInstance(AS7 as7serverInstance) {
      this.as7serverInstance = as7serverInstance
      return this
    }
    TrustManagerBuilder trustManagerName(String trustManagerName) {
      this.trustManagerName = trustManagerName
      return this
    }
    TrustManagerBuilder keyStoreName(String keyStoreName) {
      this.keyStoreName = keyStoreName
      return this
    }
    TrustManagerBuilder keyStorePassword(String keyStorePassword) {
      this.keyStorePassword = keyStorePassword
      return this
    }
    TrustManagerBuilder keyStorePath(String keyStorePath) {
      this.keyStorePath = keyStorePath.replace('\\','/')
      return this
    }
    TrustManagerBuilder keyStoreType(String keyStoreType) {
      this.keyStoreType = keyStoreType
      return this
    }
    TrustManagerBuilder keyStoreRequired(Boolean keyStoreRequired) {
      this.keyStoreRequired = keyStoreRequired
      return this
    }
    TrustManagerBuilder algorithm(String algorithm) {
      this.algorithm = algorithm
      return this
    }

    /**
     * Optional attributes:
     * algorithm  alias-filter key-store  provider-name  providers
     * certificate-revocation-list.maximum-cert-path certificate-revocation-list.path certificate-revocation-list.relative-to
    */
    TrustManagerBuilder options(Map<String,String> options) {
      this.options = options
      return this
    }

    static int build(block) {
      final TrustManagerBuilder builder = new TrustManagerBuilder().with block
      if (builder.as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")
      if (builder.keyStorePassword == null) throw new IllegalArgumentException("keyStorePassword must not be null.")
      if (builder.keyStorePath == null) throw new IllegalArgumentException("keyStorePath must not be null.")
      if (builder.keyStoreType == null) throw new IllegalArgumentException("keyStoreType must not be null.")
      int rcode
      if(builder.exists()) {
        rcode = builder.remove()
        if (rcode > 0) {
          log.error("Failure: removing ${builder.trustManagerName} failed")
          return rcode
        }
      }

      log.info("Building elytron trust manager on ${builder.as7serverInstance.serverId}")

      // If mentioned keystore don't exist then create it
      if (!CLILib.KeyStoreBuilder.exists( {
        as7serverInstance builder.as7serverInstance
        name builder.keyStoreName
      })) {
        rcode = CLILib.KeyStoreBuilder.build {
          as7serverInstance builder.as7serverInstance
          name builder.keyStoreName
          path builder.keyStorePath
          type builder.keyStoreType
          credentialReferenceClearText builder.keyStorePassword
          options(['required':builder.keyStoreRequired.toString()])
        }
        if (rcode > 0) {
          log.error("Failure: Creating keystore ${builder.keyStoreName} failed")
          return rcode
        }
      }

      String cmdStr = "/subsystem=elytron/trust-manager=${builder.trustManagerName}:add(algorithm=${builder.algorithm}," +
              "key-store=${builder.keyStoreName}"
      Map output = builder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr)
      rcode = output.exitValue
      if (rcode > 0) {
        log.error("Failure: ${cmdStr}, ${output}")
        return rcode
      }
      return builder.update()
    }

    static int update(block) {
      final TrustManagerBuilder builder = new TrustManagerBuilder().with block
      return builder.update()
    }

    int update() {
      if (!exists()) {
        throw new IllegalArgumentException("trustManager with ${trustManagerName} doesn't exists")
      }
      if (!options) {
        return 0
      }
      Map output
      Map.Entry<String, String> failed = options.find { key, value ->
        String cmdStr = "/subsystem=elytron/trust-manager=${trustManagerName}:write-attribute(name=${key},value=${value}"
        output = as7serverInstance.as7Cli.runArbitraryCommand(cmdStr)
        if (output.exitValue > 0) {
          return true
        }
      }
      if (failed) {
        log.error("Writing attribute ${failed.key} with value ${failed.value} failed!")
        return output.exitValue
      }
      return CLILib.reload(as7serverInstance)
    }

    static int undefine(block) {
      final TrustManagerBuilder builder = new TrustManagerBuilder().with block
      if (!builder.exists()) {
        throw new IllegalArgumentException("trustManager with ${builder.trustManagerName} doesn't exists")
      }
      if (!builder.options) {
        return 0
      }
      Map output
      Map.Entry<String, String> failed = builder.options.find { key, value ->
        String cmdStr = "/subsystem=elytron/trust-manager=${builder.trustManagerName}:undefine-attribute(name=${key}"
        output = builder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr)
        if (output.exitValue > 0) {
          return true
        }
      }
      if (failed) {
        log.error("Undefining attribute ${failed.key} failed!")
        return output.exitValue
      }
      return CLILib.reload(builder.as7serverInstance)
    }

    private boolean exists() {
      if (as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")
      if (StringUtils.isBlank(trustManagerName)) throw new IllegalArgumentException("name must not be null.")
      String cmdStr = " /subsystem=elytron/:read-children-names(child-type=trust-manager)"
      return as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).stdOut ==~ /[\s\S]*"result" => [\s\S]*"${trustManagerName}"[\s\S]*/
    }

    private int remove() {
      if (as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")
      if (StringUtils.isBlank(trustManagerName)) throw new IllegalArgumentException("name must not be null.")
      String cmdStr = " /subsystem=elytron/trust-manager=${trustManagerName}:remove()"
      return as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
    }

  }

  static class KeyStoreBuilder {
    AS7 as7serverInstance //MANDATORY

    String name = "keyStore"
    String type = "JKS" //MANDATORY for build
    String path = null //MANDATORY for build
    String credentialReferenceStore = null //MANDATORY for build
    String credentialReferenceAlias = null //MANDATORY for build
    String credentialReferenceType = null //MANDATORY for build
    String credentialReferenceClearText = "tomcat" //MANDATORY for build
    Map<String, String> options

    private KeyStoreBuilder() {}

    KeyStoreBuilder as7serverInstance(AS7 as7serverInstance) {
      this.as7serverInstance = as7serverInstance
      return this
    }
    KeyStoreBuilder name(String name) {
      this.name = name
      return this
    }
    KeyStoreBuilder path(String path) {
      this.path = path
      return this
    }
    KeyStoreBuilder type(String type) {
      this.type = type
      return this
    }
    KeyStoreBuilder credentialReferenceStore(String credentialReferenceStore) {
      this.credentialReferenceStore = credentialReferenceStore
      return this
    }
    KeyStoreBuilder credentialReferenceAlias(String credentialReferenceAlias) {
      this.credentialReferenceAlias = credentialReferenceAlias
      return this
    }
    KeyStoreBuilder credentialReferenceType(String credentialReferenceType) {
      this.credentialReferenceType = credentialReferenceType
      return this
    }
    KeyStoreBuilder credentialReferenceClearText(String credentialReferenceClearText) {
      this.credentialReferenceClearText = credentialReferenceClearText
      return this
    }
    /**
     * Optional attributes:
     * alias-filter          path                  providers             required
     * credential-reference  provider-name         relative-to           type
     */
    KeyStoreBuilder options(Map<String,String> options) {
      this.options = options
      return this
    }

    static int build(block) {
      final KeyStoreBuilder builder = new KeyStoreBuilder().with block
      if (builder.as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")
      if (builder.type == null) throw new IllegalArgumentException("keyStorePassword must not be null.")
      int rcode
      if(builder.exists()) {
        rcode = builder.remove()
        if (rcode > 0) {
          log.error("Failure: removing ${builder.name} failed")
          return rcode
        }
      }

      log.info("Building elytron keystore on ${builder.as7serverInstance.serverId}")
      String cmdStr = "/subsystem=elytron/key-store=${builder.name}:add(path=${builder.path}," +
              " type=${builder.type}, credential-reference=" +
              "{" +
              "${builder.credentialReferenceAlias ? "alias=${builder.credentialReferenceAlias}, " : ""}" +
              "${builder.credentialReferenceStore ? "store=${builder.credentialReferenceStore}, " : ""}" +
              "${builder.credentialReferenceType ? "type=${builder.credentialReferenceType}, " : ""}" +
              "${builder.credentialReferenceClearText ?  "clear-text=${builder.credentialReferenceClearText}, " : ""}" +
              "}"
      Map output = builder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr)
      rcode = output.exitValue
      if (rcode > 0) {
        log.error("Failure: ${cmdStr}, ${output}")
        return rcode
      }
      return builder.update()
    }

    static int update(block) {
      final KeyStoreBuilder builder = new KeyStoreBuilder().with block
      return builder.update()
    }

    int update() {
      if (!exists()) {
        throw new IllegalArgumentException("keystore with ${name} doesn't exists")
      }
      if (!options) {
        return 0
      }
      Map output
      Map.Entry<String, String> failed = options.find { key, value ->
        String cmdStr = "/subsystem=elytron/key-store=${name}:write-attribute(name=${key},value=${value}"
        output = as7serverInstance.as7Cli.runArbitraryCommand(cmdStr)
        if (output.exitValue > 0) {
          return true
        }
      }
      if (failed) {
        log.error("Writing attribute ${failed.key} with value ${failed.value} failed!")
        return output.exitValue
      }
      return CLILib.reload(as7serverInstance)
    }

    static int undefine(block) {
      final KeyStoreBuilder builder = new KeyStoreBuilder().with block
      if (!builder.exists()) {
        throw new IllegalArgumentException("keystore with ${builder.name} doesn't exists")
      }
      if (!builder.options) {
        return 0
      }
      Map output
      Map.Entry<String, String> failed = builder.options.find { key, value ->
        String cmdStr = "/subsystem=elytron/key-store=${builder.name}:undefine-attribute(name=${key}"
        output = builder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr)
        if (output.exitValue > 0) {
          return true
        }
      }
      if (failed) {
        log.error("Undefining ${failed.key} failed!")
        return output.exitValue
      }
      return CLILib.reload(builder.as7serverInstance)
    }

    static boolean exists(block) {
      final KeyStoreBuilder builder = new KeyStoreBuilder().with block
      return builder.exists()
    }

    boolean exists() {
      if (as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")
      if (StringUtils.isBlank(name)) throw new IllegalArgumentException("name must not be null.")
      String cmdStr = "/subsystem=elytron:read-children-names(child-type=key-store)"
      return as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).stdOut ==~ /[\s\S]*"result" => [\s\S]*"${name}"[\s\S]*/
    }

    private int remove() {
      if (as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")
      if (StringUtils.isBlank(name)) throw new IllegalArgumentException("name must not be null.")
      String cmdStr = "/subsystem=elytron/key-store=${name}:remove()"
      return as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
    }
  }

  /**
   * Build elytron key-store and trust-manager
   */
  static class KeyManagerBuilder {
    AS7 as7serverInstance //MANDATORY

    String keyManagerName = "keyManager"
    String keyStoreName = "keyStore"
    String keyStorePassword = "tomcat" //MANDATORY if keystore desn't exists
    String keyStorePath = null //MANDATORY if keystore desn't exists
    String keyStoreType = null //MANDATORY if keystore desn't exists
    Boolean keyStoreRequired = true
    String credentialReferenceStore = null //MANDATORY for build
    String credentialReferenceAlias = null //MANDATORY for build
    String credentialReferenceType = null //MANDATORY for build
    String credentialReferenceClearText = keyStorePassword //MANDATORY for build
    String algorithm = "PKIX"
    Map<String, String> options

    private KeyManagerBuilder() {}

    /**
     * MANDATORY
     */
    KeyManagerBuilder as7serverInstance(AS7 as7serverInstance) {
      this.as7serverInstance = as7serverInstance
      return this
    }
    KeyManagerBuilder keyManagerName(String keyManagerName) {
      this.keyManagerName = keyManagerName
      return this
    }
    KeyManagerBuilder keyStoreName(String keyStoreName) {
      this.keyStoreName = keyStoreName
      return this
    }
    KeyManagerBuilder keyStorePassword(String keyStorePassword) {
      this.keyStorePassword = keyStorePassword
      return this
    }
    KeyManagerBuilder keyStorePath(String keyStorePath) {
      this.keyStorePath = keyStorePath.replace('\\','/')
      return this
    }
    KeyManagerBuilder keyStoreType(String keyStoreType) {
      this.keyStoreType = keyStoreType
      return this
    }
    KeyManagerBuilder keyStoreRequired(Boolean keyStoreRequired) {
      this.keyStoreRequired = keyStoreRequired
      return this
    }
    KeyManagerBuilder algorithm(String algorithm) {
      this.algorithm = algorithm
      return this
    }
    KeyManagerBuilder credentialReferenceClearText(String credentialReferenceClearText) {
      this.credentialReferenceClearText = credentialReferenceClearText
      return this
    }

    /**
    * Optional attributes:
    * algorithm  alias-filter key-store  provider-name  providers
    * certificate-revocation-list.maximum-cert-path certificate-revocation-list.path certificate-revocation-list.relative-to
    */
    KeyManagerBuilder options(Map<String,String> options) {
      this.options = options
      return this
    }

    static int build(block) {
      final KeyManagerBuilder builder = new KeyManagerBuilder().with block
      if (builder.as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")
      if (builder.keyStoreName == null) throw new IllegalArgumentException("keyStoreName must not be null.")

      int rcode
      log.info("Building elytron key manager on ${builder.as7serverInstance.serverId}")
      if(builder.exists()) {
        rcode = builder.remove()
        if (rcode > 0) {
          log.error("Failure: removing ${builder.keyManagerName} failed")
          return rcode
        }
      }
      // If mentioned keystore don't exist then create it
      if (!CLILib.KeyStoreBuilder.exists( {
        as7serverInstance builder.as7serverInstance
        name builder.keyStoreName
      })) {
        rcode = CLILib.KeyStoreBuilder.build {
          as7serverInstance builder.as7serverInstance
          name builder.keyStoreName
          path builder.keyStorePath
          type builder.keyStoreType
          credentialReferenceClearText builder.keyStorePassword
          options(['required':builder.keyStoreRequired.toString()])
        }
        if (rcode > 0) {
          log.error("Failure: Creating keystore ${builder.keyStoreName} failed")
          return rcode
        }
      }
      String cmdStr = "/subsystem=elytron/key-manager=${builder.keyManagerName}:add(algorithm=${builder.algorithm}," +
              " key-store=${builder.keyStoreName}, credential-reference=" +
              "{" +
              "${builder.credentialReferenceAlias ? "alias=${builder.credentialReferenceAlias}, " : ""}" +
              "${builder.credentialReferenceStore ? "store=${builder.credentialReferenceStore}, " : ""}" +
              "${builder.credentialReferenceType ? "type=${builder.credentialReferenceType}, " : ""}" +
              "${builder.credentialReferenceClearText ?  "clear-text=${builder.credentialReferenceClearText}, " : ""}" +
              "}"
      Map output = builder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr)
      rcode = output.exitValue
      if (rcode > 0) {
        log.error("Failure: ${cmdStr}, ${output}")
        return rcode
      }
      return builder.update()
    }

    static int update(block) {
      final KeyManagerBuilder builder = new KeyManagerBuilder().with block
      return builder.update()
    }

    int update() {
      if (!exists()) {
        throw new IllegalArgumentException("Keymanager with ${keyManagerName} doesn't exists")
      }
      if (!options) {
        return 0
      }
      Map output
      Map.Entry<String, String> failed = options.find { key, value ->
        String cmdStr = "/subsystem=elytron/key-manager=${keyManagerName}:write-attribute(name=${key},value=${value}"
        output = as7serverInstance.as7Cli.runArbitraryCommand(cmdStr)
        if (output.exitValue > 0) {
          return true
        }
      }
      if (failed) {
        log.error("Writing attribute ${failed.key} with value ${failed.value} failed!")
        return output.exitValue
      }
      return CLILib.reload(as7serverInstance)
    }

    static int undefine(block) {
      final KeyManagerBuilder builder = new KeyManagerBuilder().with block
      if (!builder.exists()) {
        throw new IllegalArgumentException("keyManager with ${builder.keyManagerName} doesn't exists")
      }
      if (!builder.options) {
        return 0
      }
      Map output
      Map.Entry<String, String> failed = builder.options.find { key, value ->
        String cmdStr = "/subsystem=elytron/key-manager=${builder.keyManagerName}:undefine-attribute(name=${key}"
        output = builder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr)
        if (output.exitValue > 0) {
          return true
        }
      }
      if (failed) {
        log.error("Undefining attribute ${failed.key} failed!")
        return output.exitValue
      }
      return CLILib.reload(builder.as7serverInstance)
    }

    private boolean exists() {
      if (as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")
      if (StringUtils.isBlank(keyManagerName)) throw new IllegalArgumentException("name must not be null.")
      String cmdStr = " /subsystem=elytron/:read-children-names(child-type=key-manager)"
      return as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).stdOut ==~ /[\s\S]*"result" => [\s\S]*"${keyManagerName}"[\s\S]*/
    }

    private int remove() {
      if (as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")
      if (StringUtils.isBlank(keyManagerName)) throw new IllegalArgumentException("name must not be null.")
      String cmdStr = "/subsystem=elytron/key-manager=${keyManagerName}:remove()"
      return as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
    }
  }

  /**
   * Build elytron server-ssl-context with key-store, key-manager and trust-manager
   *
   * @param protocols, List of protocols separated with ",", example => "TLSv1, TLSv2"
   */
  static class ServerSSLContextBuilder {
    AS7 as7serverInstance //MANDATORY

    String keyManagerName = null // MANDATORY
    String trustManagerName = null // If set, trustManagers will be created and added to server-ssl-context
    String serverSSLContextName = "serverSSLContext"
    String protocols = null
    Boolean needClientAuth = null

    private ServerSSLContextBuilder() {}

    /**
     * MANDATORY
     */
    ServerSSLContextBuilder as7serverInstance(AS7 as7serverInstance) {
      this.as7serverInstance = as7serverInstance
      return this
    }
    ServerSSLContextBuilder trustManagerName(String trustManagerName) {
      this.trustManagerName = trustManagerName
      return this
    }
    ServerSSLContextBuilder protocols(String protocols) {
      this.protocols = protocols
      return this
    }
    ServerSSLContextBuilder serverSSLContextName(String serverSSLContextName) {
      this.serverSSLContextName = serverSSLContextName
      return this
    }
    /**
     * MANDATORY
     */
    ServerSSLContextBuilder keyManagerName(String keyManagerName) {
      this.keyManagerName = keyManagerName
      return this
    }
    ServerSSLContextBuilder needClientAuth(Boolean needClientAuth) {
      this.needClientAuth = needClientAuth
      return this
    }
    static int build(block) {
      final ServerSSLContextBuilder builder = new ServerSSLContextBuilder().with block
      if (builder.as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")
      if (builder.keyManagerName == null) throw new IllegalArgumentException("keyManagerName must not be null.")
      String cmdStr = "/subsystem=elytron:read-children-names(child-type=client-ssl-context)"
      Map output
      int rcode
      if (!(builder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).stdOut ==~ /[\s\S]*"result" => .*${builder.serverSSLContextName}[\s\S]*/)) {
        cmdStr = "/subsystem=elytron/server-ssl-context=${builder.serverSSLContextName}:add(key-manager=${builder.keyManagerName})"
        output = builder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr)
        rcode = output.exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}, ${output}")
          return rcode
        }
      }
      if (builder.trustManagerName) {
        cmdStr = "/subsystem=elytron/server-ssl-context=${builder.serverSSLContextName}:write-attribute(name=trust-manager, value=${builder.trustManagerName}"
        output = builder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr)
        rcode = output.exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}, ${output}")
          return rcode
        }
      }
      if (builder.protocols) {
        cmdStr = "/subsystem=elytron/server-ssl-context=${builder.serverSSLContextName}:write-attribute(name=protocols, value=[${builder.protocols}]"
        output = builder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr)
        rcode = output.exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}, ${output}")
          return rcode
        }
      }
      if (builder.needClientAuth) {
        cmdStr = "/subsystem=elytron/server-ssl-context=${builder.serverSSLContextName}:write-attribute(name=need-client-auth, value=${builder.needClientAuth}"
        output = builder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr)
        rcode = output.exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}, ${output}")
          return rcode
        }
      }
      return 0
    }
  }

  /**
   * Add Load Metric
   */
  public static class AddLoadMetricBuilder {
    AS7 as7serverInstance
    Float capacity
    String type
    Map properties
    Integer weight
    Integer history
    Integer decay

    private AddLoadMetricBuilder() {}

    /**
     * MANDATORY
     * @param as7serverInstance
     * @return
     */
    AddLoadMetricBuilder as7serverInstance(AS7 as7serverInstance) {
      this.as7serverInstance = as7serverInstance
      return this
    }

    /**
     * MANDATORY
     * @param type , e.g.
     * busyness  cpu  heap  receive-traffic  requests  send-traffic  sessions
     * @return
     */
    AddLoadMetricBuilder type(String type) {
      this.type = type
      return this
    }

    AddLoadMetricBuilder capacity(Float capacity) {
      this.capacity = capacity
      return this
    }

    AddLoadMetricBuilder properties(Map properties) {
      this.properties = properties
      return this
    }

    AddLoadMetricBuilder weight(Integer weight) {
      this.weight = weight
      return this
    }

    AddLoadMetricBuilder history(Integer history) {
      this.history = history
      return this
    }

    AddLoadMetricBuilder decay(Integer decay) {
      this.decay = decay
      return this
    }

    public static int build(block) {
      final AddLoadMetricBuilder addLoadMetricBuilder = new AddLoadMetricBuilder().with block
      if (addLoadMetricBuilder.as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")
      if (StringUtils.isBlank(addLoadMetricBuilder.type)) throw new IllegalArgumentException("Type must be set.")

      int rcode = 0
      String cmdStr
      if (CLILib.readModClusterSubsystem(addLoadMetricBuilder.as7serverInstance) =~ /[[^\n]*\n]*dynamic-load-provider[ "=>\{\n]*undefined/) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration/dynamic-load-provider=configuration:add()"
        rcode = addLoadMetricBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!(CLILib.readModClusterSubsystem(addLoadMetricBuilder.as7serverInstance) =~ /[[^\n]*\n]*load-metric[ "=>\{\n]*${addLoadMetricBuilder.type}/)) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration:add-metric(type=${addLoadMetricBuilder.type})"
        rcode = addLoadMetricBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (addLoadMetricBuilder.decay != null) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration/dynamic-load-provider=configuration:write-attribute(name=decay, value=${addLoadMetricBuilder.decay})"
        rcode = addLoadMetricBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (addLoadMetricBuilder.history != null) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration/dynamic-load-provider=configuration:write-attribute(name=history, value=${addLoadMetricBuilder.history})"
        rcode = addLoadMetricBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (addLoadMetricBuilder.capacity != null) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration/dynamic-load-provider=configuration/load-metric=${addLoadMetricBuilder.type}:write-attribute(name=capacity, value=${addLoadMetricBuilder.capacity})"
        rcode = addLoadMetricBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (addLoadMetricBuilder.weight != null) {
        cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration/dynamic-load-provider=configuration/load-metric=${addLoadMetricBuilder.type}:write-attribute(name=weight, value=${addLoadMetricBuilder.weight})"
        rcode = addLoadMetricBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
        if (rcode > 0) {
          log.error("Failure: ${cmdStr}")
          return rcode
        }
      }

      if (!CollectionUtils.isEmpty(addLoadMetricBuilder.properties)) {
        for (String key : addLoadMetricBuilder.properties.keySet()) {
          cmdStr = "/subsystem=modcluster/mod-cluster-config=configuration/dynamic-load-provider=configuration/load-metric=${addLoadMetricBuilder.type}:write-attribute(name=property.${key}, value=${CLILib.escapeQuotes(addLoadMetricBuilder.properties.get(key))})"
          rcode = addLoadMetricBuilder.as7serverInstance.as7Cli.runArbitraryCommand(cmdStr).exitValue
          if (rcode > 0) {
            log.error("Failure: ${cmdStr}")
            return rcode
          }
        }
      }

      return rcode
    }
  }

  /**
   * Remove load metric
   */
  public static class RemoveLoadMetricBuilder {
    AS7 as7serverInstance
    String type

    private RemoveLoadMetricBuilder() {}

    /**
     * MANDATORY
     * @param type
     * @return
     */
    RemoveLoadMetricBuilder type(String type) {
      this.type = type
      return this
    }

    /**
     * MANDATORY
     * @param as7serverInstance
     * @return
     */
    RemoveLoadMetricBuilder as7serverInstance(AS7 as7serverInstance) {
      this.as7serverInstance = as7serverInstance
      return this
    }

    public static int build(block) {
      final RemoveLoadMetricBuilder removeLoadMetricBuilder = new RemoveLoadMetricBuilder().with block
      if (removeLoadMetricBuilder.as7serverInstance == null) throw new IllegalArgumentException("AS7Instance must not be null.")
      if (StringUtils.isBlank(removeLoadMetricBuilder.type)) throw new IllegalArgumentException("Type must not be null nor empty.")
      return removeLoadMetricBuilder.as7serverInstance.as7Cli.runArbitraryCommand("/subsystem=modcluster/mod-cluster-config=configuration:remove-metric(type=${removeLoadMetricBuilder.type})").exitValue
    }
  }

  /**
   * Reloads server, than waits for management api to become ready, as reload command don't block
   * for whole server start
   * @param as7serverInstance, server to reload
   * @param adminOnly, reload to full start or only to admin mode, default is false
   * @return reload command return code, 0 for OK, > 0 Fail, -1 if managementApi hasn't been ready before timeout
   */
  static int reload(AS7 as7serverInstance, boolean adminOnly = false) {
    String argument = adminOnly ? "--admin-only" : ""
    int ret = as7serverInstance.as7Cli.runArbitraryCommand('reload ' + argument).exitValue
    if (ret == 0) {
      return (as7serverInstance.waitManagementApiReady())? 0 : -1
    }
    return ret
  }

  /**
   *
   * @param as7serverInstance
   * @return String containing the mod-cluster filter details.
   *
   */
  static String readUndertowModCluster(AS7 as7serverInstance) {
    String cmdStr = "/subsystem=undertow/configuration=filter/mod-cluster=modcluster:read-resource(include-runtime=true, recursive=true, recursive-depth=100)"
    return CLILib.readArbitraryCommandOutput(as7serverInstance, cmdStr)
  }



  /**
   *
   * @param as7serverInstance
   * @return String containing details about mod_cluster subsystem.
   */
  static String readModClusterSubsystem(AS7 as7serverInstance) {
    String cmdStr = "/subsystem=modcluster:read-resource(include-runtime=true, recursive=true, recursive-depth=100)"
    return CLILib.readArbitraryCommandOutput(as7serverInstance, cmdStr)
  }

  /**
   * Get info of one node
   * @param as7serverInstance
   * @param workerid, name of the worker
   * @param balancer , name of the balancer
   * @return String containing the mod-cluster node details.
   *
   */
  static String readUndertowModClusterNode(AS7 as7serverInstance, String workerId, String balancer = "mycluster") {
    String cmdStr = "/subsystem=undertow/configuration=filter/mod-cluster=modcluster/balancer=$balancer/node=$workerId:read-resource(include-runtime=true)"
    return CLILib.readArbitraryCommandOutput(as7serverInstance, cmdStr)
  }

  /**
   * Get attribute of socket-binding
   * @param as7serverInstance
   * @param name, name of the socket-binding
   * @param attribute , name of the attribute one wants to get
   * @return String containing the result of call for given attribute.
   */
  static String readSocketBinding(AS7 as7serverInstance, String name, String attribute) {
    final Address address = Address.of("socket-binding-group", "standard-sockets").and("socket-binding", name)
    ManagementClientProvider.createOnlineManagementClient(as7serverInstance).withCloseable {
      final OnlineManagementClient client ->
        final ModelNodeResult result = new Operations(client).readAttribute(address, attribute)
        result.assertSuccess()
        return result.stringValue()
    }
  }

  /**
   * Get attribute of socket-binding
   * @param as7serverInstance
   * @param name, name of the socket-binding-group, default is "standard-sockets"
   * @return String containing the result of call for given attribute.
   */
  static String readSocketBindingGroup(AS7 as7serverInstance, String name = "standard-sockets") {
    String cmdStr = "/socket-binding-group=$name:read-resource()"
    String output = readArbitraryCommandOutput(as7serverInstance, cmdStr)
    return output
  }

  /**
   *
   * @param as7serverInstance mandatory
   * @param cmdStr arbitrary JBoss CLI command
   * @return
   */
  static String readArbitraryCommandOutput(final AS7 as7serverInstance, final String cmdStr) {
    final Map result = as7serverInstance.as7Cli.runArbitraryCommand(cmdStr)
    final String content = result.stdOut + result.stdErr
    if (result.exitValue > 0) {
      log.error("Failure: This AS7Instance probably does not have the resource you are trying to read configured. Output: ${content}")
      return ""
    }
    return content
  }

  static int enableDebugLog(AS7 as7serverInstance) {
    return as7serverInstance.as7Cli.runArbitraryCommand("batch, /subsystem=logging/console-handler=CONSOLE:write-attribute(name=level,value=DEBUG), /subsystem=logging/root-logger=ROOT:write-attribute(name=level,value=DEBUG), run-batch").exitValue
  }

  static int disableDebugLog(AS7 as7serverInstance) {
    return as7serverInstance.as7Cli.runArbitraryCommand("batch, /subsystem=logging/console-handler=CONSOLE:write-attribute(name=level,value=INFO), /subsystem=logging/root-logger=ROOT:write-attribute(name=level,value=INFO), run-batch").exitValue
  }

  static int enableUndertowAccessLog(AS7 as7serverInstance, String server = "default-server", String host = "default-host") {
    return as7serverInstance.as7Cli.runArbitraryCommand("/subsystem=undertow/server=${server}/host=${host}/setting=access-log:add()").exitValue
  }
}

