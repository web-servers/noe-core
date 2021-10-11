package noe.tomcat

import groovy.util.slurpersupport.GPathResult
import noe.server.Tomcat
import noe.tomcat.configure.AjpConnectorTomcat
import noe.tomcat.configure.TomcatConfigurator
import noe.tomcat.configure.ConnectorSSLHostConfigTomcat
import noe.tomcat.configure.ConnectorCertificateTomcat
import noe.tomcat.configure.NonSecureHttpConnectorTomcat
import noe.tomcat.configure.SecureHttpConnectorTomcat
import noe.tomcat.configure.ShutdownTomcat
import org.junit.Test

import noe.common.utils.Platform
import noe.common.utils.PathHelper
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
  void updateExistingHttpConnectorAllAttributesGivenSuccessExpected() {
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
  void createNewAjpConnectorSuccessExpected() {
    Integer testAjpPort = 18585
    String testProtocol = "AJP/1.3"
    Integer testRedirectPort = 18989
    Boolean testSecretRequired = true
    String testSecret = "mysecret"
    String testAllowedRequestAttributesPattern = "customAttributesPattern"

    new TomcatConfigurator(tomcat)
      .ajpConnector(new AjpConnectorTomcat()
        .setPort(testAjpPort)
        .setProtocol(testProtocol)
        .setRedirectPort(testRedirectPort)
        .setSecretRequired(testSecretRequired)
        .setSecret(testSecret)
        .setAllowedRequestAttributesPattern(testAllowedRequestAttributesPattern))

    GPathResult Server = new XmlSlurper().parse(new File(tomcat.basedir, "conf/server.xml"))
    assertEquals testAjpPort, Integer.valueOf(Server.Service.Connector.find { isAjpProtocol(it) }.@port.toString())
    assertEquals testProtocol, Server.Service.Connector.find { isAjpProtocol(it) }.@protocol.toString()
    assertEquals testRedirectPort, Integer.valueOf(Server.Service.Connector.find { isAjpProtocol(it) }.@redirectPort.toString())
    assertEquals testSecretRequired, Boolean.valueOf(Server.Service.Connector.find { isAjpProtocol(it) }.@secretRequired.toString())
    assertEquals testSecret, Server.Service.Connector.find { isAjpProtocol(it) }.@secret.toString()
    assertEquals testAllowedRequestAttributesPattern, Server.Service.Connector.find { isAjpProtocol(it) }.@allowedRequestAttributesPattern.toString()
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
  void createNewHttpConnectorSuccessExpected() {
    Integer testAjpPort = 18585
    String testProtocol = "AJP/1.3"
    Integer testRedirectPort = 18989
    Boolean testSecretRequired = true
    String testSecret = "mysecret"
    String testAllowedRequestAttributesPattern = "customAttributesPattern"

    new TomcatConfigurator(tomcat)
      .ajpConnector(new AjpConnectorTomcat()
        .setPort(testAjpPort)
        .setProtocol(testProtocol)
        .setRedirectPort(testRedirectPort)
        .setSecretRequired(testSecretRequired)
        .setSecret(testSecret)
        .setAllowedRequestAttributesPattern(testAllowedRequestAttributesPattern))

    GPathResult Server = new XmlSlurper().parse(new File(tomcat.basedir, "conf/server.xml"))
    assertEquals testAjpPort, Integer.valueOf(Server.Service.Connector.find { isAjpProtocol(it) }.@port.toString())
    assertEquals testProtocol, Server.Service.Connector.find { isAjpProtocol(it) }.@protocol.toString()
    assertEquals testRedirectPort, Integer.valueOf(Server.Service.Connector.find { isAjpProtocol(it) }.@redirectPort.toString())
    assertEquals testSecretRequired, Boolean.valueOf(Server.Service.Connector.find { isAjpProtocol(it) }.@secretRequired.toString())
    assertEquals testSecret, Server.Service.Connector.find { isAjpProtocol(it) }.@secret.toString()
    assertEquals testAllowedRequestAttributesPattern, Server.Service.Connector.find { isAjpProtocol(it) }.@allowedRequestAttributesPattern.toString()
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

    TomcatConfigurator tconf = new TomcatConfigurator(tomcat)
      .httpsConnector(new SecureHttpConnectorTomcat().setPort(testHttpsPort))
      .portOffset(testOffset)

    GPathResult Server = new XmlSlurper().parse(new File(tomcat.basedir, "conf/server.xml"))
    assertEquals testHttpsPort + testOffset, Integer.valueOf(Server.Service.Connector.find { isSecure(it) }.@port.toString())
  }

  @Test
  void shiftPortAndSetHttpsPortDefaultServerXmlChangeExpected() {
    int testOffset = 10000
    Integer testHttpsPort = 18443

    new TomcatConfigurator(tomcat)
      .portOffset(testOffset)
      .httpsConnector(new SecureHttpConnectorTomcat().setPort(testHttpsPort))

    GPathResult Server = new XmlSlurper().parse(new File(tomcat.basedir, "conf/server.xml"))
    assertEquals testHttpsPort, Integer.valueOf(Server.Service.Connector.find { isSecuredHttpProtocol(it) }.@port.toString())
    assertEquals Tomcat.DEFAULT_HTTP_PORT + testOffset, Integer.valueOf(Server.Service.Connector.find { isNotSecuredHttpProtocol(it) }.@port.toString())
  }

  @Test
  void testCertificateDefaultsServerXmlChangeExpected() {
    String sslIntermediate = PathHelper.join(new Platform().getTmpDir(), "ssl", "proper", "generated", "ca", "intermediate")
    String sslCertificate = new File(PathHelper.join(sslIntermediate, "certs"), "localhost.server.cert.pem").getCanonicalPath()
    String sslCertificateKey = new File(PathHelper.join(sslIntermediate, "private"), "localhost.server.key.pem").getCanonicalPath()
    String keystoreFilePath = new File(PathHelper.join(sslIntermediate, "keystores"), "localhost.server.keystore.jks").getCanonicalPath()
    String password = "testpass"
    Integer testHttpsPort = 8443

    new TomcatConfigurator(tomcat)
      .httpsConnector(new SecureHttpConnectorTomcat()
        .setPort(testHttpsPort)
        .setDefaultCertificatesConfiguration())

    GPathResult Server = new XmlSlurper().parse(new File(tomcat.basedir, "conf/server.xml"))
    assertEquals sslCertificate, Server.Service.Connector.find { isSecuredHttpProtocol(it) }.@SSLCertificateFile.toString()
    assertEquals sslCertificateKey, Server.Service.Connector.find { isSecuredHttpProtocol(it) }.@SSLCertificateKeyFile.toString()
    assertEquals keystoreFilePath, Server.Service.Connector.find { isSecuredHttpProtocol(it) }.@keystoreFile.toString()
    assertEquals password, Server.Service.Connector.find { isSecuredHttpProtocol(it) }.@SSLPassword.toString()
    assertEquals password, Server.Service.Connector.find { isSecuredHttpProtocol(it) }.@truststorePass.toString()
    assertEquals password, Server.Service.Connector.find { isSecuredHttpProtocol(it) }.@keystorePass.toString()
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

  @Test
  void sslHostConfigCreateSecureConnector() {
    Integer testHttpsPort = 18443
    String hostName = "myHostName"
    String certificateVerification = "certVerification"
    String certFile = "server.key"
    String certPath = "path/to/cert"
    String tType = "ttype"
    String tFile = "tfile"
    String tPass = "tpasswd"
    String tProvider = "tprovider"
    String protocols = "protocols"
    String sslProtocol = "sslprotocol"
    String ciphers = "several_ciphers"

    ConnectorSSLHostConfigTomcat sslHostConfObj = new ConnectorSSLHostConfigTomcat()
      .setHostName(hostName)
      .setCertificateVerification(certificateVerification)
      .setCaCertificateFile(certFile)
      .setCaCertificatePath(certPath)
      .setTruststoreType(tType)
      .setTruststoreFile(tFile)
      .setTruststorePassword(tPass)
      .setTruststoreProvider(tProvider)
      .setProtocols(protocols)
      .setSSLProtocol(sslProtocol)
      .setCiphers(ciphers)

    new TomcatConfigurator(tomcat)
      .httpsConnector(
        new SecureHttpConnectorTomcat()
          .setPort(testHttpsPort)
          .setSSLHostConfigs(sslHostConfObj)
      )

    GPathResult Server = new XmlSlurper().parse(new File(tomcat.basedir, "conf/server.xml"))
    assertEquals hostName, Server.Service.Connector.find { isSecuredHttpProtocol(it) }.SSLHostConfig.@hostName.toString()
    assertEquals certificateVerification, Server.Service.Connector.find { isSecuredHttpProtocol(it) }.SSLHostConfig.@certificateVerification.toString()
    assertEquals certFile, Server.Service.Connector.find { isSecuredHttpProtocol(it) }.SSLHostConfig.@caCertificateFile.toString()
    assertEquals certPath, Server.Service.Connector.find { isSecuredHttpProtocol(it) }.SSLHostConfig.@caCertificatePath.toString()
    assertEquals tFile, Server.Service.Connector.find { isSecuredHttpProtocol(it) }.SSLHostConfig.@truststoreFile.toString()
    assertEquals tPass, Server.Service.Connector.find { isSecuredHttpProtocol(it) }.SSLHostConfig.@truststorePassword.toString()
    assertEquals tType, Server.Service.Connector.find { isSecuredHttpProtocol(it) }.SSLHostConfig.@truststoreType.toString()
    assertEquals tProvider, Server.Service.Connector.find { isSecuredHttpProtocol(it) }.SSLHostConfig.@truststoreProvider.toString()
    assertEquals protocols, Server.Service.Connector.find { isSecuredHttpProtocol(it) }.SSLHostConfig.@protocols.toString()
    assertEquals sslProtocol, Server.Service.Connector.find { isSecuredHttpProtocol(it) }.SSLHostConfig.@sslProtocol.toString()
    assertEquals ciphers, Server.Service.Connector.find { isSecuredHttpProtocol(it) }.SSLHostConfig.@ciphers.toString()
  }

  @Test
  void certificateCreateSecureConnector() {
    Integer testHttpsPort = 18443
    String certFile = "certfile"
    String chain = "chain"
    String keyAlias = "keyalias"
    String keyFile = "keyfile"
    String keyPass = "keypass"
    String keystoreFile = "tprovider"
    String keystorePass = "protocols"
    String keystoreType = "sslprotocol"
    String keystoreProvider = "several_ciphers"
    String certType = "certType"

    ConnectorCertificateTomcat certificateObj = new ConnectorCertificateTomcat()
      .setCertificateFile(certFile)
      .setCertificateChainFile(chain)
      .setCertificateKeyAlias(keyAlias)
      .setCertificateKeyFile(keyFile)
      .setCertificateKeyPassword(keyPass)
      .setCertificateKeystoreFile(keystoreFile)
      .setCertificateKeystorePassword(keystorePass)
      .setCertificateKeystoreType(keystoreType)
      .setCertificateKeystoreProvider(keystoreProvider)
      .setCertificateType(certType)

    ConnectorSSLHostConfigTomcat sslHostConfObj = new ConnectorSSLHostConfigTomcat()
      .setCertificate(certificateObj)

    new TomcatConfigurator(tomcat)
      .httpsConnector(
        new SecureHttpConnectorTomcat()
          .setPort(testHttpsPort)
          .setSSLHostConfigs(sslHostConfObj)
      )

    GPathResult Server = new XmlSlurper().parse(new File(tomcat.basedir, "conf/server.xml"))
    assertEquals certFile, Server.Service.Connector.find { isSecuredHttpProtocol(it) }.SSLHostConfig.Certificate.@certificateFile.toString()
    assertEquals chain, Server.Service.Connector.find { isSecuredHttpProtocol(it) }.SSLHostConfig.Certificate.@certificateChainFile.toString()
    assertEquals keyAlias, Server.Service.Connector.find { isSecuredHttpProtocol(it) }.SSLHostConfig.Certificate.@certificateKeyAlias.toString()
    assertEquals keyFile, Server.Service.Connector.find { isSecuredHttpProtocol(it) }.SSLHostConfig.Certificate.@certificateKeyFile.toString()
    assertEquals keyPass, Server.Service.Connector.find { isSecuredHttpProtocol(it) }.SSLHostConfig.Certificate.@certificateKeyPassword.toString()
    assertEquals keystoreFile, Server.Service.Connector.find { isSecuredHttpProtocol(it) }.SSLHostConfig.Certificate.@certificateKeystoreFile.toString()
    assertEquals keystorePass, Server.Service.Connector.find { isSecuredHttpProtocol(it) }.SSLHostConfig.Certificate.@certificateKeystorePassword.toString()
    assertEquals keystoreProvider, Server.Service.Connector.find { isSecuredHttpProtocol(it) }.SSLHostConfig.Certificate.@certificateKeystoreProvider.toString()
    assertEquals keystoreType, Server.Service.Connector.find { isSecuredHttpProtocol(it) }.SSLHostConfig.Certificate.@certificateKeystoreType.toString()
    assertEquals certType, Server.Service.Connector.find { isSecuredHttpProtocol(it) }.SSLHostConfig.Certificate.@certificateType.toString()
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
