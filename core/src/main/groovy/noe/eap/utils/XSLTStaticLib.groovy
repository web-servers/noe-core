package noe.eap.utils

import noe.common.DefaultProperties
import noe.common.utils.Library
import noe.eap.server.as7.AS7Properties
import groovy.xml.XmlUtil

import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource
/**
 * XSLTStaticLib - your first step in the realm of AS7 configuration!
 *
 * Whenever you need to have something configured on AS7, follow these steps:
 *  1) Create your transformation stylesheet in /as7/xslt
 *  2) Use some tags for easy substitution, like #WHATEVER#
 *  3) Prepare your method, e.g. like: def static nativeConnectors(Boolean trueOrFalse = true, String absolutePathToConfig)
 *  4) Use it.
 *
 *  Pls., try to keep as much configuration in XSLT as possible and as little as possible in regular expressions, for obvious reasons...
 *
 * @author Michal Karm Babacek <mbabacek@redhat.com>
 *
 */
class XSLTStaticLib {

  private static TransformerFactory factory = null
  def static RELATIVE_XSLT_PATH = "as7/xslt"

  /**
   * Creates or updates system property
   *
   * For instance: If you put "jboss.mod_cluster.jvmRoute" and  "myNode01", you will get:
   *
   * <system-properties>
   *     <property name="jboss.mod_cluster.jvmRoute" value="myNode01" />
   * </system-properties>
   *
   * It can handle even situations such as:
   * <system-properties>
   *     <property name="jboss.mod_cluster.jvmRoute" value="SOME_OLD_VALUE" />
   * </system-properties>
   * or
   * <system-properties>
   *     <property name="some" value="other property in place" />
   * </system-properties>
   *
   * @param absolutePathToConfig
   * @param property
   * @param value
   */
  def static addOrUpdateSystemProperties(String propertyName = "evilprop", String propertyValue = "something", String absolutePathToConfig) {
    def xslt = Library.retrieveResourceAsFile(RELATIVE_XSLT_PATH + "/system-properties.xsl").getText().replace("@SYSTEM_PROPERTY_NAME@", propertyName)
    xslt = xslt.replace("@SYSTEM_PROPERTY_VALUE@", XmlUtil.escapeXml(propertyValue))
    transformIt(xslt, new File(absolutePathToConfig))
  }

  /**
   * Remove connector
   *
   * @param absolutePathToConfig
   * @param connectorName
   * @return
   */
  def static removeConnector(String connectorName = "", String absolutePathToConfig) {
    def xslt = Library.retrieveResourceAsFile(RELATIVE_XSLT_PATH + "/remove-connector.xsl").getText().replace("@CONNECTOR_NAME@", connectorName)
    transformIt(xslt, new File(absolutePathToConfig))
  }

  /**
   * Add AJP or HTTP connector
   *
   * @param absolutePathToConfig
   * @param ajpOrHttp - can be either AJP or HTTP.  HTTPS is being dealt with in another xslt sheet.
   * @param protocol
   * @param socketBinding
   * @param enabled
   * @param scheme
   * @return
   */
  def static addAJPorHTTPConnector(String ajpOrHttp = "ajp", String protocol = "AJP/1.3", String socketBinding = "ajp",
                                   Boolean enabled = true, String scheme = "http", String absolutePathToConfig) {
    def xslt = Library.retrieveResourceAsFile(RELATIVE_XSLT_PATH + "/add-ajp-or-http-connector.xsl").getText().replace("@AJP_OR_HTTP@", ajpOrHttp)
    xslt = xslt.replace("@PROTOCOL@", protocol)
    xslt = xslt.replace("@SOCKET_BINDING@", socketBinding)
    xslt = xslt.replace("@ENABLED@", enabled.toString())
    xslt = xslt.replace("@SCHEME@", scheme)
    transformIt(xslt, new File(absolutePathToConfig))
  }

  /**
   * Add HTTPS Connector
   *
   * @param absolutePathToConfig
   * @param ajpOrHttp - can be either AJP or HTTP.  HTTPS is being dealt with in another xslt sheet.
   * @param protocol
   * @param socketBinding
   * @param enabled
   * @param scheme
   * @param secure
   * @param caCertificateFile
   * @param certificateKeyFile
   * @param certificateFile
   * @param password
   * @param verifyClient
   * @param keyAlias
   * @param cipherSuite
   * @param ssl_protocol
   * @return
   */
  def static addHTTPSConnector(
      String connector = "https",
      String protocol = "HTTP/1.1",
      String socketBinding = "https",
      Boolean enabled = true,
      String scheme = "https",
      Boolean secure = true,
      String caCertificateFile = "ca-cert.jks",
      String certificateKeyFile = "server-cert-key.jks",
      String certificateFile = "server-cert-key.jks",
      String password = "tomcat",
      Boolean verifyClient = true,
      String keyAlias = "javaserver",
      String cipherSuite = DefaultProperties.MOD_CLUSTER_SSL_CIPER_SUITE,
      String ssl_protocol = "TLSv1",
      String absolutePathToConfig) {
    def xslt = Library.retrieveResourceAsFile(RELATIVE_XSLT_PATH + "/add-https-connector.xsl").getText()
    xslt = xslt.replace("@CONNECTOR_NAME@", connector)
    xslt = xslt.replace("@PROTOCOL@", protocol)
    xslt = xslt.replace("@SOCKET_BINDING@", socketBinding)
    xslt = xslt.replace("@ENABLED@", enabled.toString())
    xslt = xslt.replace("@SCHEME@", scheme)
    xslt = xslt.replace("@SECURE@", secure.toString())
    xslt = xslt.replace("@CA_CERTIFICATE_FILE@", XmlUtil.escapeXml(caCertificateFile))
    xslt = xslt.replace("@CERTIFICATE_KEY_FILE@", XmlUtil.escapeXml(certificateKeyFile))
    xslt = xslt.replace("@CERTIFICATE_FILE@", XmlUtil.escapeXml(certificateFile))
    xslt = xslt.replace("@PASSWORD@", password)
    xslt = xslt.replace("@VERIFY_CLIENT@", verifyClient.toString())
    xslt = xslt.replace("@KEY_ALIAS@", keyAlias)
    xslt = xslt.replace("@CIPHER_SUITE@", cipherSuite)
    xslt = xslt.replace("@SSL_PROTOCOL@", ssl_protocol)
    transformIt(xslt, new File(absolutePathToConfig))
  }

  /**
   * Native connectors
   *
   * TODO: There is probably a bug in addHTTPSConnector or removeConnector, so you must call THIS transformation after these two named.
   *
   * @param absolutePathToConfig
   * @param trueOrFalse , true = native connectors on, false = native connectors off
   * @return
   */
  def static nativeConnectors(Boolean trueOrFalse = true, String absolutePathToConfig) {
    def xslt = Library.retrieveResourceAsFile(RELATIVE_XSLT_PATH + "/native-connectors.xsl").getText().replace("@NATIVE_CONNECTORS_BOOLEAN@", trueOrFalse.toString())
    transformIt(xslt, new File(absolutePathToConfig))
  }

  /**
   * Change IP Addresses AS7 binds to
   *
   * @param absolutePathToConfig
   * @param attributesToBeUpdated
   *        If you leave this empty, the default is: @see ews.server.as7.AS7Properties
   * @return
   */
  def static changeIpAddresses(Map attributesToBeUpdated = [:], String absolutePathToConfig) {
    def xslt = Library.retrieveResourceAsFile(RELATIVE_XSLT_PATH + "/change-ip-addresses.xsl").getText()
    def attributes = [
        MANAGEMENT_IP_ADDRESS    : AS7Properties.MANAGEMENT_IP_ADDRESS,
        PUBLIC_IP_ADDRESS        : AS7Properties.PUBLIC_IP_ADDRESS,
        UDP_MCAST_ADDRESS        : AS7Properties.UDP_MCAST_ADDRESS,
        DIAGNOSTICS_MCAST_ADDRESS: AS7Properties.DIAGNOSTICS_MCAST_ADDRESS,
        MPING_MCAST_ADDRESS      : AS7Properties.MPING_MCAST_ADDRESS,
        MODCLUSTER_MCAST_ADDRESS : DefaultProperties.MODCLUSTER_MCAST_ADDRESS,
        PRIVATE_IP_ADDRESS       : AS7Properties.PRIVATE_IP_ADDRESS
        ]
    attributesToBeUpdated.each { key, value ->
      attributes[key] = value
    }
    attributes.each { key, value ->
      xslt = xslt.replace("@$key@", value.toString())
    }
    transformIt(xslt, new File(absolutePathToConfig))
  }

  /**
   * Set connector for mod_cluster
   *
   * @param absolutePathToConfig
   * @param connectorName - defaults to ajp
   * @return
   */
  def static setModClusterConnector(String connectorName = "ajp", String absolutePathToConfig) {
    def xslt = Library.retrieveResourceAsFile(RELATIVE_XSLT_PATH + "/mod-cluster-connector.xsl").getText().replace("@CONNECTOR@", connectorName)
    transformIt(xslt, new File(absolutePathToConfig))
  }

  /**
   * mod_cluster advertise socket
   *
   * @param absolutePathToConfig
   * @param attributesToBeUpdated
   *        If you leave this empty, the default is: @see ews.server.as7.AS7Properties
   * @return
   */
  def static modClusterAdvertiseSocket(Map attributesToBeUpdated = [:], String absolutePathToConfig) {
    def xslt = Library.retrieveResourceAsFile(RELATIVE_XSLT_PATH + "/mod-cluster-advertise-socket.xsl").getText()
    def attributes = [
        MODCLUSTER_MCAST_PORT   : DefaultProperties.MODCLUSTER_MCAST_PORT,
        MODCLUSTER_MCAST_ADDRESS: DefaultProperties.MODCLUSTER_MCAST_ADDRESS]
    attributesToBeUpdated.each { key, value ->
      attributes[key] = value
    }
    attributes.each { key, value ->
      xslt = xslt.replace("@$key@", value.toString())
    }
    transformIt(xslt, new File(absolutePathToConfig))
  }

  /**
   * mod_cluster excluded contexts
   *
   * @param absolutePathToConfig
   * @param excludedContexts , comma separated list of excluded contexts
   * @return
   */
  def static excludedContexts(String excludedContexts = "ROOT, admin-console, invoker, jbossws, jmx-console, juddi, web-console", String absolutePathToConfig) {
    def xslt = Library.retrieveResourceAsFile(RELATIVE_XSLT_PATH + "/mod-cluster-excluded-contexts.xsl").getText().replace("@EXCLUDED_CONTEXTS_LIST@", excludedContexts)
    transformIt(xslt, new File(absolutePathToConfig))
  }

  /**
   * mod_cluster balancer
   *
   * @param absolutePathToConfig
   * @param balancer
   * @return
   */
  def static balancer(String balancer = "mybalancer", String absolutePathToConfig) {
    def xslt = Library.retrieveResourceAsFile(RELATIVE_XSLT_PATH + "/mod-cluster-balancer.xsl").getText().replace("@BALANCER_STRING@", balancer)
    transformIt(xslt, new File(absolutePathToConfig))
  }

  /**
   * mod_cluster loadbalancing group
   *
   * @param absolutePathToConfig
   * @param loadBalancingGroup
   * @return
   */
  def static loadBalancingGroup(String loadBalancingGroup = "", String absolutePathToConfig) {
    def xslt = Library.retrieveResourceAsFile(RELATIVE_XSLT_PATH + "/mod-cluster-loadbalancing-group.xsl").getText().replace("@LOAD_BALANCING_GROUP@", loadBalancingGroup)
    transformIt(xslt, new File(absolutePathToConfig))
  }

  /**
   * mod_cluster sticky-session
   *
   * @param absolutePathToConfig
   * @param true /false
   * @return
   */
  def static stickySession(String trueOrFalse = "true", String absolutePathToConfig) {
    def xslt = Library.retrieveResourceAsFile(RELATIVE_XSLT_PATH + "/mod-cluster-sticky-session.xsl").getText().replace("@STICKY_SESSION@", trueOrFalse)
    transformIt(xslt, new File(absolutePathToConfig))
  }

  /**
   * mod_cluster sticky-session-remove
   *
   * @param absolutePathToConfig
   * @param true /false
   * @return
   */
  def static stickySessionRemove(String trueOrFalse = "false", String absolutePathToConfig) {
    def xslt = Library.retrieveResourceAsFile(RELATIVE_XSLT_PATH + "/mod-cluster-sticky-session-remove.xsl").getText().replace("@STICKY_SESSION_REMOVE@", trueOrFalse)
    transformIt(xslt, new File(absolutePathToConfig))
  }

  /**
   * mod_cluster sticky-session-force
   *
   * @param absolutePathToConfig
   * @param true /false
   * @return
   */
  def static stickySessionForce(String trueOrFalse = "false", String absolutePathToConfig) {
    def xslt = Library.retrieveResourceAsFile(RELATIVE_XSLT_PATH + "/mod-cluster-sticky-session-force.xsl").getText().replace("@STICKY_SESSION_FORCE@", trueOrFalse)
    transformIt(xslt, new File(absolutePathToConfig))
  }

  /**
   * mod_cluster proxy-list
   *
   * @param absolutePathToConfig
   * @param proxyList
   * @return
   */
  def static proxyList(String proxyList = "", String absolutePathToConfig) {
    def xslt = Library.retrieveResourceAsFile(RELATIVE_XSLT_PATH + "/mod-cluster-proxy-list.xsl").getText().replace("@PROXY_LIST@", proxyList)
    transformIt(xslt, new File(absolutePathToConfig))
  }

  /**
   * mod_cluster stop context timeout
   *
   * @param absolutePathToConfig
   * @param stopContextTimeout (by default it is in seconds)
   * @return
   */
  def static stopContextTimeout(String stopContextTimeout = 10, String absolutePathToConfig) {
    def xslt = Library.retrieveResourceAsFile(RELATIVE_XSLT_PATH + "/mod-cluster-stop-context-timeout.xsl").getText().replace("@STOP_CONTEXT_TIMEOUT@", stopContextTimeout)
    transformIt(xslt, new File(absolutePathToConfig))
  }

  /**
   * mod_cluster autoEnableContexts
   *
   * @param absolutePathToConfig
   * @param trueOrFalse , true = autoEnableContexts on, false = autoEnableContexts off
   * @return
   */
  def static autoEnableContexts(Boolean autoEnableContexts = true, String absolutePathToConfig) {
    def xslt = Library.retrieveResourceAsFile(RELATIVE_XSLT_PATH + "/mod-cluster-auto-enable-contexts.xsl").getText().replace("@AUTO_ENABLE_CONTEXTS_BOOL@", autoEnableContexts.toString())
    transformIt(xslt, new File(absolutePathToConfig))
  }

  /**
   * Configure mod_cluster for SSL
   *
   * @param absolutePathToConfig
   * @param caCertificateFile
   * @param certificateKeyFile
   * @param password
   * @param keyAlias
   * @param cipherSuite
   * @param ssl_protocol
   * @return
   */
  def static modClusterSSLConfig(
      String caCertificateFile = "ca-cert.jks",
      String certificateKeyFile = "client-cert-key.jks",
      String password = "tomcat",
      String keyAlias = "javaclient",
      String cipherSuite = DefaultProperties.MOD_CLUSTER_SSL_CIPER_SUITE,
      String ssl_protocol = "TLS",
      String absolutePathToConfig) {
    def xslt = Library.retrieveResourceAsFile(RELATIVE_XSLT_PATH + "/mod-cluster-ssl-config.xsl").getText()
    xslt = xslt.replace("@CA_CERTIFICATE_FILE@", XmlUtil.escapeXml(caCertificateFile))
    xslt = xslt.replace("@CERTIFICATE_KEY_FILE@", XmlUtil.escapeXml(certificateKeyFile))
    xslt = xslt.replace("@PASSWORD@", password)
    xslt = xslt.replace("@KEY_ALIAS@", keyAlias)
    xslt = xslt.replace("@CIPHER_SUITE@", cipherSuite)
    xslt = xslt.replace("@SSL_PROTOCOL@", ssl_protocol)
    transformIt(xslt, new File(absolutePathToConfig))
  }

  /**
   * Removes all load metrics from <dynamic-load-provider> element.
   */
  def static removeAllLoadMetrics(String absolutePathToConfig) {
    def xslt = Library.retrieveResourceAsFile(RELATIVE_XSLT_PATH + "/mod-cluster-remove-all-load-metrics.xsl").getText()
    transformIt(xslt, new File(absolutePathToConfig))
  }

  /**
   * Add load metric to <dynamic-load-provider>
   *
   * @param absolutePathToConfig
   * @param loadMetricType
   * @param loadMetricWeight
   * @return
   */
  def static addLoadMetric(String loadMetricType, String loadMetricWeight, String absolutePathToConfig) {
    def xslt = Library.retrieveResourceAsFile(RELATIVE_XSLT_PATH + "/mod-cluster-add-load-metric.xsl").getText()
    xslt = xslt.replace("@LOAD_METRIC_TYPE@", loadMetricType)
    xslt = xslt.replace("@WEIGHT@", loadMetricWeight)
    transformIt(xslt, new File(absolutePathToConfig))
  }

  /**
   *  Create hotstandby configuration
   *
   * @param absolutePathToConfig
   * @return
   */
  def static makeHotStandbyNode(String absolutePathToConfig) {
    def xslt = Library.retrieveResourceAsFile(RELATIVE_XSLT_PATH + "/mod-cluster-create-hot-standby-node.xsl").getText()
    transformIt(xslt, new File(absolutePathToConfig))
  }

  /**
   * Add custom load metric to <dynamic-load-provider>
   *
   * @param absolutePathToConfig
   * @param loadMetricClass
   * @param loadMetricWeight
   * @param loadMetricCapacity
   * @param loadMetricLoadFile
   * @param loadMetricParseExpression
   * @param history
   * @return
   */
  def
  static addCustomLoadMetric(String history, String decay, String loadMetricClass, String loadMetricWeight, String loadMetricCapacity, String loadMetricLoadFile, String loadMetricParseExpression, String absolutePathToConfig) {
    def xslt = Library.retrieveResourceAsFile(RELATIVE_XSLT_PATH + "/mod-cluster-add-custom-load-metric.xsl").getText()
    xslt = xslt.replace("@HISTORY@", history)
    xslt = xslt.replace("@DECAY@", decay)
    xslt = xslt.replace("@LOAD_METRIC_CLASS@", loadMetricClass)
    xslt = xslt.replace("@WEIGHT@", loadMetricWeight)
    xslt = xslt.replace("@CAPACITY@", loadMetricCapacity)
    xslt = xslt.replace("@LOAD_FILE@", loadMetricLoadFile)
    xslt = xslt.replace("@PARSE_EXPRESSION@", loadMetricParseExpression)
    transformIt(xslt, new File(absolutePathToConfig))
  }

  /**
   * Add a jar resource to module.xml
   *
   * @param jar
   * @param absolutePathToConfig
   * @return
   */
  def static addJarToModuleXML(String jar, String absolutePathToConfig) {
    def xslt = Library.retrieveResourceAsFile(RELATIVE_XSLT_PATH + "/mod-cluster-add-jar-to-module.xsl").getText()
    xslt = xslt.replace("@JAR@", jar)
    transformIt(xslt, new File(absolutePathToConfig))
  }

  /**
   * mod_cluster session-draining-strategy
   *
   * @param absolutePathToConfig
   * @param ALWAYS |DEFAULT|NEVER
   * @return
   */
  def static sessionDrainingStrategy(String strategy = "DEFAULT", String absolutePathToConfig) {
    def xslt = Library.retrieveResourceAsFile(RELATIVE_XSLT_PATH + "/mod-cluster-session-draining-strategy.xsl").getText().replace("@STRATEGY@", strategy)
    transformIt(xslt, new File(absolutePathToConfig))
  }

  def static transformIt(String xslt, File configFile) {
    def input = configFile.getText()
    def output = new FileOutputStream(configFile)
    if (factory == null) {
      factory = TransformerFactory.newInstance()
    }
    if (input == null || output == null || xslt == null || input.isEmpty() || !configFile.exists() || xslt.isEmpty()) {
      throw new IllegalArgumentException("Please, check the file path and transformation string: input:${input}, output:${output}, xslt:${xslt}")
    }
    Transformer transformer = factory.newTransformer(new StreamSource(new StringReader(xslt)))
    if (transformer == null) {
      throw new IllegalArgumentException("Something is wrong with the xslt transformer, is this O.K.? : " + xslt)
    }
    try {
      transformer.transform(new StreamSource(new StringReader(input)), new StreamResult(output))
    } catch (NullPointerException ex) {
      throw new NullPointerException("Probably malformed xslt:" + xslt)
    } finally {
      output.close()
    }
  }
}
