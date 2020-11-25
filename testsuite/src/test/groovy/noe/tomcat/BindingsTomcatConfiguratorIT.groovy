package noe.tomcat

import groovy.util.slurpersupport.GPathResult
import noe.server.Tomcat
import noe.tomcat.configure.AjpConnectorTomcat
import noe.tomcat.configure.TomcatConfigurator
import noe.tomcat.configure.NonSecureHttpConnectorTomcat
import noe.tomcat.configure.SecureHttpConnectorTomcat
import noe.tomcat.configure.ShutdownTomcat
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

/**
 * Abstract class for Tomcat bindings testing.
 *
 * Needs to be extended and implemented following method:
 *
 * <code>
 *   @BeforeClass
 *   public static void beforeClass() {
 *     loadTestProperties("/tomcatX.properties")
 *     prepareWorkspace()
 *   }
 * </code>
 *
 */
abstract class BindingsTomcatConfiguratorIT extends TomcatTestAbstract {

  @Test
  void updateExistingHttpConnectorAllAttribtuesGivenSuccessExpected() {
    def onlyOneNonSecureConnector = 1
    Integer testHttpPort = 18080
    def testAddress = "my-test-host"
    Integer testMaxThreads = 111
    Integer testRedirectPort = 8445
    Integer testConnectionTimeout = 16161
    def testScheme = "newScheme"

    new TomcatConfigurator(tomcat)
      .httpConnector(new NonSecureHttpConnectorTomcat()
        .setPort(testHttpPort)
        .setAddress(testAddress)
        .setRedirectPort(testRedirectPort)
        .setMaxThreads(testMaxThreads)
        .setConnectionTimeout(testConnectionTimeout)
        .setScheme(testScheme))

    GPathResult Server = new XmlSlurper().parse(new File(tomcat.basedir, "conf/server.xml"))
    assertEquals onlyOneNonSecureConnector, Server.Service.Connector.find { isNotSecuredHttpProtocol(it)  }.size()
    assertEquals testHttpPort, Integer.valueOf(Server.Service.Connector.find { isNotSecuredHttpProtocol(it) }.@port.toString())
    assertEquals testAddress, Server.Service.Connector.find { isNotSecuredHttpProtocol(it) }.@address.toString()
    assertEquals testMaxThreads, Integer.valueOf(Server.Service.Connector.find { isNotSecuredHttpProtocol(it) }.@maxThreads.toString())
    assertEquals testRedirectPort, Integer.valueOf(Server.Service.Connector.find { isNotSecuredHttpProtocol(it) }.@redirectPort.toString())
    assertEquals testConnectionTimeout, Integer.valueOf(Server.Service.Connector.find { isNotSecuredHttpProtocol(it) }.@connectionTimeout.toString())
    assertEquals testScheme, Server.Service.Connector.find { isNotSecuredHttpProtocol(it) }.@scheme.toString()
  }

  @Test
  void createNewHttpsConnectorSuccessExpected() {
    def onlyOneSecureConnector = 1
    Integer testHttpsPort = 18443
    def testProtocol = "HTTP/1.1"
    def testAddress = "my-test-host"
    Integer testMaxThreads = 111
    Boolean testClientAuth = true
    Boolean testSSLEnabled = true
    def testSslProtocol = "TLS"
    def testKeystoreFile = "fakeKeystore"
    def testKeystorePass = "fakekeystorePass"
    def testSSLCertificateFile = "fakeSSLCertificateFile"
    def testSSLCertificateKeyFile = "fakeSSLCertificateKeyFile"
    def testSSLPassword = "fakeSSLPassword"
    def testScheme = "newScheme"

    new TomcatConfigurator(tomcat)
      .httpsConnector(
        new SecureHttpConnectorTomcat()
          .setProtocol(testProtocol)
          .setPort(testHttpsPort)
          .setAddress(testAddress)
          .setMaxThreads(testMaxThreads)
          .setClientAuth(testClientAuth)
          .setSslProtocol(testSslProtocol)
          .setKeystoreFile(testKeystoreFile)
          .setKeystorePass(testKeystorePass)
          .setSslCertificateFile(testSSLCertificateFile)
          .setSslCertificateKeyFile(testSSLCertificateKeyFile)
          .setSslPassword(testSSLPassword)
          .setScheme(testScheme))

    GPathResult Server = new XmlSlurper().parse(new File(tomcat.basedir, "conf/server.xml"))
    assertEquals onlyOneSecureConnector, Server.Service.Connector.find { isSecure(it) }.size()
    assertEquals "true", Server.Service.Connector.find { isSecure(it) }.@secure.toString()
    assertEquals testProtocol, Server.Service.Connector.find { isSecure(it) }.@protocol.toString()
    assertEquals testHttpsPort, Integer.valueOf(Server.Service.Connector.find { isSecure(it) }.@port.toString())
    assertEquals testAddress, Server.Service.Connector.find { isSecure(it) }.@address.toString()
    assertEquals testMaxThreads, Integer.valueOf(Server.Service.Connector.find { isSecure(it) }.@maxThreads.toString())
    assertEquals testClientAuth, Boolean.valueOf(Server.Service.Connector.find { isSecure(it) }.@clientAuth.toString())
    assertEquals testSSLEnabled, Boolean.valueOf(Server.Service.Connector.find { isSecure(it) }.@SSLEnabled.toString())
    assertEquals testSslProtocol, Server.Service.Connector.find { isSecure(it) }.@sslProtocol.toString()
    assertEquals testKeystoreFile, Server.Service.Connector.find { isSecure(it) }.@keystoreFile.toString()
    assertEquals testKeystorePass, Server.Service.Connector.find { isSecure(it) }.@keystorePass.toString()
    assertEquals testSSLCertificateFile, Server.Service.Connector.find { isSecure(it) }.@SSLCertificateFile.toString()
    assertEquals testSSLCertificateKeyFile, Server.Service.Connector.find { isSecure(it) }.@SSLCertificateKeyFile.toString()
    assertEquals testSSLPassword, Server.Service.Connector.find { isSecure(it) }.@SSLPassword.toString()
    assertEquals testScheme, Server.Service.Connector.find { isSecure(it) }.@scheme.toString()

    assertEquals testHttpsPort, Integer.valueOf(Server.Service.Connector.find { isNotSecuredHttpProtocol(it) }.@redirectPort.toString())
    assertEquals testHttpsPort, Integer.valueOf(Server.Service.Connector.find { isAjpProtocol(it) }.@redirectPort.toString())
  }

  @Test
  void updateExistingAjpConnectorAllAttribtuesGivenSuccessExpected() {
    def onlyOneAjpConnector = 1
    Integer testAjpPort = 18009
    def testAddress = "my-test-host"
    Integer testMaxThreads = 111
    Integer testRedirectPort = 8445
    Integer testConnectionTimeout = 16161
    def testScheme = "newScheme"

    new TomcatConfigurator(tomcat)
      .ajpConnector(new AjpConnectorTomcat()
      .setAddress(testAddress)
      .setPort(testAjpPort)
      .setRedirectPort(testRedirectPort)
      .setMaxThreads(testMaxThreads)
      .setConnectionTimeout(testConnectionTimeout)
      .setScheme(testScheme))

    GPathResult Server = new XmlSlurper().parse(new File(tomcat.basedir, "conf/server.xml"))
    assertEquals onlyOneAjpConnector, Server.Service.Connector.find { isAjpProtocol(it) && !isSecure(it)  }.size()
    assertEquals testAjpPort, Integer.valueOf(Server.Service.Connector.find { isAjpProtocol(it) }.@port.toString())
    assertEquals testAddress, Server.Service.Connector.find { isAjpProtocol(it) }.@address.toString()
    assertEquals testMaxThreads, Integer.valueOf(Server.Service.Connector.find { isAjpProtocol(it) }.@maxThreads.toString())
    assertEquals testRedirectPort, Integer.valueOf(Server.Service.Connector.find { isAjpProtocol(it) }.@redirectPort.toString())
    assertEquals testConnectionTimeout, Integer.valueOf(Server.Service.Connector.find { isAjpProtocol(it) }.@connectionTimeout.toString())
    assertEquals testScheme, Server.Service.Connector.find { isAjpProtocol(it) }.@scheme.toString()
  }

  @Test
  void updateExistingAjpConnectorOneAttribtueGivenSuccessExpected() {
    def onlyOneAjpConnector = 1
    Integer testAjpPort = 18009
    def defAttributesCountOfAjpConnector = 3

    new TomcatConfigurator(tomcat)
      .ajpConnector(new AjpConnectorTomcat().setPort(testAjpPort))

    GPathResult Server = new XmlSlurper().parse(new File(tomcat.basedir, "conf/server.xml"))
    assertEquals onlyOneAjpConnector, Server.Service.Connector.find { isAjpProtocol(it) && !isSecure(it)  }.size()
    assertEquals testAjpPort, Integer.valueOf(Server.Service.Connector.find { isAjpProtocol(it) }.@port.toString())
    assertEquals Tomcat.DEFAULT_HTTPS_PORT, Integer.valueOf(Server.Service.Connector.find { isAjpProtocol(it) }.@redirectPort.toString())
    assertEquals defAttributesCountOfAjpConnector, Server.Service.Connector.find { isAjpProtocol(it) }.attributes().size()
  }

  @Test
  void updateShuddownOneAttribtueGivenSuccessExpected() {
    Integer testShutdownPort = 18009
    def defAttributesCountOfServerElement = 2

    new TomcatConfigurator(tomcat)
      .shutdown(new ShutdownTomcat().setPort(testShutdownPort))

    GPathResult Server = new XmlSlurper().parse(new File(tomcat.basedir, "conf/server.xml"))
    assertEquals testShutdownPort, Integer.valueOf(Server.@port.toString())
    assertEquals "SHUTDOWN", Server.@shutdown.toString()
    assertEquals defAttributesCountOfServerElement, Server.attributes().size()
  }

  @Test
  void updateShuddownAllAttribetesGivenSuccessExpected() {
    Integer testShutdownPort = 18009
    def testShutdown = "testShutdown"
    def testClassName = "test.class.name"
    def testAddress = "test.address"
    def defAttributesCountOfServerElement = 4

    new TomcatConfigurator(tomcat)
      .shutdown(
        new ShutdownTomcat()
          .setPort(testShutdownPort)
          .setShutdown(testShutdown)
          .setClassName(testClassName)
          .setAddress(testAddress))

    GPathResult Server = new XmlSlurper().parse(new File(tomcat.basedir, "conf/server.xml"))
    assertEquals testShutdownPort, Integer.valueOf(Server.@port.toString())
    assertEquals testShutdown, Server.@shutdown.toString()
    assertEquals testAddress, Server.@address.toString()
    assertEquals testClassName, Server.@className.toString()
    assertEquals defAttributesCountOfServerElement, Server.attributes().size()
  }

  @Test
  void shiftPortDefaultServerXmlChangeExpected() {
    int testOffset = 10000
    Integer shiftedHttpPort = 18080
    Integer shiftedShutdownPort = 18005
    Integer shiftedAjpPort = 18009

    new TomcatConfigurator(tomcat)
        .portOffset(testOffset)

    GPathResult Server = new XmlSlurper().parse(new File(tomcat.basedir, "conf/server.xml"))
    assertEquals shiftedHttpPort, Integer.valueOf(Server.Service.Connector.find { isNotSecuredHttpProtocol(it) }.@port.toString())
    assertEquals Tomcat.DEFAULT_HTTPS_PORT, Integer.valueOf(Server.Service.Connector.find { isNotSecuredHttpProtocol(it) }.@redirectPort.toString())
    assertEquals "", Server.Service.Connector.find { isSecure(it) }.@port.toString() // https connector was not created by port shifting
    assertEquals shiftedShutdownPort, Integer.valueOf(Server.@port.toString())
    assertEquals shiftedAjpPort, Integer.valueOf(Server.Service.Connector.find { isAjpProtocol(it) }.@port.toString())
  }

  @Test
  void setHttpsPortAndShiftPortDefaltServerXmlChangeExpected() {
    int testOffset = 10000
    Integer testHttpsPort = 18443

    new TomcatConfigurator(tomcat)
      .httpsConnector(new SecureHttpConnectorTomcat().setPort(testHttpsPort))
      .portOffset(testOffset)

    GPathResult Server = new XmlSlurper().parse(new File(tomcat.basedir, "conf/server.xml"))
    assertEquals testHttpsPort + testOffset, Integer.valueOf(Server.Service.Connector.find { isNotSecuredHttpProtocol(it) }.@redirectPort.toString())
    assertEquals testHttpsPort + testOffset, Integer.valueOf(Server.Service.Connector.find { isAjpProtocol(it) }.@redirectPort.toString())
    assertEquals testHttpsPort + testOffset, Integer.valueOf(Server.Service.Connector.find { isSecure(it) }.@port.toString())
  }

  @Test
  void shiftPortAndSetHttpsPortDefaltServerXmlChangeExpected() {
    int testOffset = 10000
    Integer testHttpsPort = 18443

    new TomcatConfigurator(tomcat)
      .portOffset(testOffset)
      .httpsConnector(new SecureHttpConnectorTomcat().setPort(testHttpsPort))

    GPathResult Server = new XmlSlurper().parse(new File(tomcat.basedir, "conf/server.xml"))
    assertEquals testHttpsPort, Integer.valueOf(Server.Service.Connector.find { isNotSecuredHttpProtocol(it) }.@redirectPort.toString())
    assertEquals testHttpsPort, Integer.valueOf(Server.Service.Connector.find { isAjpProtocol(it) }.@redirectPort.toString())
    assertEquals testHttpsPort, Integer.valueOf(Server.Service.Connector.find { isSecuredHttpProtocol(it) }.@port.toString())
    assertEquals Tomcat.DEFAULT_HTTP_PORT + testOffset, Integer.valueOf(Server.Service.Connector.find { isNotSecuredHttpProtocol(it) }.@port.toString())
  }

  @Test
  void testCertificateDefaultsServerXmlChangeExpected() {
    String sslStringDir = new File(new Platform().getTmpDir(), new PathHelper().join("ssl", "self_signed")).getCanonicalPath()
    String sslCertificate = new File(sslStringDir, "server.crt").getCanonicalPath()
    String sslCertificateKey = new File(sslStringDir, "server.key").getCanonicalPath()
    String keystoreFilePath = new File(sslStringDir, "server.jks").getCanonicalPath()
    String password = "changeit"

    new TomcatConfigurator(tomcat).httpsConnector(new SecureHttpConnectorTomcat().setDefaultCertificatesConfiguration())

    assertEquals sslCertificate, Integer.valueOf(Server.Service.Connector.find { isSecuredHttpProtocol(it) }.@SSLCertificateFile.toString())
    assertEquals sslCertificate, Integer.valueOf(Server.Service.Connector.find { isSecuredHttpProtocol(it) }.@SSLCACertificateFile.toString())
    assertEquals sslCertificateKey, Integer.valueOf(Server.Service.Connector.find { isSecuredHttpProtocol(it) }.@SSLCertificateKeyFile.toString())
    assertEquals keystoreFilePath, Integer.valueOf(Server.Service.Connector.find { isSecuredHttpProtocol(it) }.@keystoreFile.toString())
    assertEquals password, Integer.valueOf(Server.Service.Connector.find { isSecuredHttpProtocol(it) }.@SSLPassword.toString())
    assertEquals password, Integer.valueOf(Server.Service.Connector.find { isSecuredHttpProtocol(it) }.@truststorePass.toString())
    assertEquals password, Integer.valueOf(Server.Service.Connector.find { isSecuredHttpProtocol(it) }.@keystorePass.toString())
  }

  @Test
  void customModificationDefaltServerXmlChangeExpected() {
    Integer testShutdownPort = 1609

    TomcatConfigurator tConfigurator = new TomcatConfigurator(tomcat)
    Node serverXml = tConfigurator.retrieveXmlConfigurationFileParsed('server.xml')
    serverXml.@port = testShutdownPort
    tConfigurator.configureXmlConfigurationFile('server.xml', serverXml)

    GPathResult Server = new XmlSlurper().parse(new File(tomcat.basedir, "conf/server.xml"))
    assertEquals testShutdownPort, Integer.valueOf(Server.@port.toString())
  }

  @Test
  void customModificationDefaltCatalinaPropertiesChangeExpected() {
    String propFile = 'catalina.properties'
    String propName = "my.prop"
    String newValue = "true"

    TomcatConfigurator tConfigurator = new TomcatConfigurator(tomcat)
    Properties props = tConfigurator.retrievePropertiesConfigurationFileParsed(propFile)
    props.setProperty(propName, newValue)
    tConfigurator.configurePropertiesConfigurationFile(propFile, props)

    Properties newProps = new Properties()
    newProps.load(new File(tomcat.basedir, "conf/${propFile}").newDataInputStream())

    assertEquals newValue, newProps.getProperty(propName)
  }

  @Test
  void addSpecificListenerOfServerXml() {
    String value = 'customListener'
    new TomcatConfigurator(tomcat).addListener(value)

    GPathResult Server = new XmlSlurper().parse(new File(tomcat.basedir, "conf/server.xml"))
    assertEquals value, Server.Listener.find { it.@className=value }.@className.toString()
  }

  @Test
  void removeSpecificListenerOfServerXml() {
    String value = "org.apache.catalina.core.AprLifecycleListener"
    new TomcatConfigurator(tomcat).removeListener(value)

    GPathResult Server = new XmlSlurper().parse(new File(tomcat.basedir, "conf/server.xml"))
    assertTrue Server.Listener.find { it.@className.contains('AprLifecycleListener') }.toString().isEmpty()
  }

  @Test
  void upgradeProtocolToHTTP2ProtocolSecureAndNonSecureConnector() {
    Integer testHttpsPort = 18443
    Integer testHttpPort = 18080

    String value = "org.apache.coyote.http2.Http2Protocol"
    new TomcatConfigurator(tomcat)
      .httpsConnector(
        new SecureHttpConnectorTomcat()
          .setPort(testHttpsPort)
          .setUpgradeProtocolToHttp2Protocol())
      .httpConnector(
        new NonSecureHttpConnectorTomcat()
          .setPort(testHttpPort)
          .setUpgradeProtocolToHttp2Protocol())

    GPathResult Server = new XmlSlurper().parse(new File(tomcat.basedir, "conf/server.xml"))
    assertEquals value, Server.Service.Connector.find { isNotSecuredHttpProtocol(it) }.UpgradeProtocol.@className.toString()
    assertEquals value, Server.Service.Connector.find { isSecuredHttpProtocol(it) }.UpgradeProtocol.@className.toString()
  }

  boolean isSecuredHttpProtocol(GPathResult connector) {
    return isSecure(connector) && (connector.@protocol in getDefHttpProtocol())
  }

  boolean isNotSecuredHttpProtocol(GPathResult connector) {
    return !isSecure(connector) && (connector.@protocol in getDefHttpProtocol())
  }

  boolean isAjpProtocol(GPathResult connector) {
    return connector.@protocol == "AJP/1.3"
  }

  boolean isSecure(GPathResult connector) {
    return connector.@secure == "true"
  }

  List getDefHttpProtocol() {
    return ["HTTP/1.1", ""] // no protocol -> HTTP protocol (https://tomcat.apache.org/tomcat-7.0-doc/config/ajp.html)
  }


}
