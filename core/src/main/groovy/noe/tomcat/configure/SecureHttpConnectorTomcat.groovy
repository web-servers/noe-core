package noe.tomcat.configure

import noe.common.utils.Platform
import noe.common.utils.PathHelper

/**
 * Abstraction for secure HTTP connector to configure Tomcat server.xml.
 * It is used for transfer data from user to `TomcatConfigurator`.
 * Provides default values if needed.
 *
 * Secure HTTP connector has set attributes `secure="true", SSLEnabled = "true" always.
 *
 * IMPORTANT
 * <ul>
 *   <li>Not all connector attributes are supported. Only the most used ones.</li>
 *   <li>It is user responsibility to set values semantically, no validation is performed.</li>
 * <ul>
 *
 * @link https://tomcat.apache.org/tomcat-8.0-doc/config/http.html
 */
public class SecureHttpConnectorTomcat extends ConnectorTomcatAbstract<SecureHttpConnectorTomcat> {
  // - vvv ------------------------------------------------------
  // IMPORTANT: When adding support for new argument udpate method `ConnectorAttributesFactory` as well
  //
  // SSL common
  private boolean sslEnabled

  // SSL BIO and NIO
  private String sslProtocol
  private String keystoreFile
  private String keystorePass
  private String keystoreType
  private Boolean clientAuth
  private String truststoreFile
  private String truststorePass
  private String truststoreType
  private String sslImplementationName
  private String sslEnabledProtocols

  // SSL APR
  private String sslCACertificateFile
  private String sslCertificateFile
  private String sslCertificateKeyFile
  private String sslPassword

  // Inner elements
  private ConnectorUpgradeProtocolTomcat upgradeProtocol
  private List<ConnectorSSLHostConfigTomcat> SSLHostConfigs
  // - ^^^ ------------------------------------------------------

  public SecureHttpConnectorTomcat() {
    super.setSecure(true)
    sslEnabled = true
  }

  /**
   * Configure secure http connector to expect certificates in ${SYSTEM_TEMP}/ssl/proper/generated/ca/intermediate directory
   * Expected names:
   *  <ul>
   *    <li>certificate = localhost.server.cert.pem</li>
   *    <li>key file = localhost.server.key.pem</li>
   *    <li>keystore = localhost.server.keystore.jks</li>
   *    <li></li>
   *  </ul>
   *  Password for keystore, trustore and SSL sets to "changeit" (without apostrophes).
   */
  SecureHttpConnectorTomcat setDefaultCertificatesConfiguration() {
    String sslIntermediate = PathHelper.join(new Platform().getTmpDir(), "ssl", "proper", "generated", "ca", "intermediate")
    String sslCertificate = new File(PathHelper.join(sslIntermediate, "certs"), "localhost.server.cert.pem").getCanonicalPath()
    String sslCertificateKey = new File(PathHelper.join(sslIntermediate, "private"), "localhost.server.key.pem").getCanonicalPath()
    String keystoreFilePath = new File(PathHelper.join(sslIntermediate, "keystores"), "localhost.server.keystore.jks").getCanonicalPath()
    String password = "testpass"

    setSslCertificateFile(sslCertificate)
    setSslCertificateKeyFile(sslCertificateKey)
    setKeystoreFile(keystoreFilePath)
    setKeystorePass(password)
    setTruststorePass(password)
    setSslPassword(password)
  }

  /**
   * Input argument secure is ignored secure is set `true` always.
   */
  @Override
  public SecureHttpConnectorTomcat setSecure(boolean secure) {
    return this
  }

  public Boolean getSslEnabled() {
    return this.sslEnabled
  }

  public Boolean getClientAuth() {
    return this.clientAuth
  }

  public String getSslProtocol() {
    return this.sslProtocol
  }

  public String getKeystoreFile() {
    return this.keystoreFile
  }

  public String getKeystorePass() {
    return this.keystorePass
  }

  public String getKeystoreType() {
    return this.keystoreType
  }

  public String getTruststoreFile() {
    return this.truststoreFile
  }

  public String getTruststorePass() {
    return this.truststorePass
  }

  public String getTruststoreType() {
    return this.truststoreType
  }

  public String getSslCertificateFile() {
    return this.sslCertificateFile
  }

  public String getSslCACertificateFile() {
    return this.sslCACertificateFile
  }

  public String getSslCertificateKeyFile() {
    return this.sslCertificateKeyFile
  }

  public String getSslPassword() {
    return this.sslPassword
  }

  public String getSslImplementationName() {
    return this.sslImplementationName
  }

  public String getSslEnabledProtocols() {
    return this.sslEnabledProtocols
  }

  public SecureHttpConnectorTomcat setSslProtocol(String sslProtocol) {
    this.sslProtocol = sslProtocol
    return this
  }

  public SecureHttpConnectorTomcat setKeystoreFile(String keystoreFile) {
    this.keystoreFile = keystoreFile
    return this
  }

  public SecureHttpConnectorTomcat setKeystorePass(String keystorePass) {
    this.keystorePass = keystorePass
    return this
  }

  public SecureHttpConnectorTomcat setKeystoreType(String keystoreType) {
    this.keystoreType = keystoreType
    return this
  }

  public SecureHttpConnectorTomcat setTruststoreFile(String truststoreFile) {
    this.truststoreFile = truststoreFile
    return this
  }

  public SecureHttpConnectorTomcat setTruststorePass(String truststorePass) {
    this.truststorePass = truststorePass
    return this
  }

  public SecureHttpConnectorTomcat setTruststoreType(String truststoreType) {
    this.truststoreType = truststoreType
    return this
  }

  public SecureHttpConnectorTomcat setClientAuth(boolean clientAuth) {
    this.clientAuth = clientAuth
    return this
  }

  public SecureHttpConnectorTomcat setSslCertificateFile(String sslCertificateFile) {
    this.sslCertificateFile = sslCertificateFile
    return this
  }

  public SecureHttpConnectorTomcat setSslCACertificateFile(String sslCACertificateFile) {
    this.sslCACertificateFile = sslCACertificateFile
    return this
  }

  public SecureHttpConnectorTomcat setSslCertificateKeyFile(String sslCertificateKeyFile) {
    this.sslCertificateKeyFile = sslCertificateKeyFile
    return this
  }

  public SecureHttpConnectorTomcat setSslPassword(String sslPassword) {
    this.sslPassword = sslPassword
    return this
  }

  public SecureHttpConnectorTomcat setSslImplementationName(String sslImplementationName) {
    this.sslImplementationName = sslImplementationName
    return this
  }

  public SecureHttpConnectorTomcat setSslEnabledProtocols(String sslEnabledProtocols) {
    this.sslEnabledProtocols = sslEnabledProtocols
    return this
  }

  //--------------

  public ConnectorUpgradeProtocolTomcat getUpgradeProtocol() {
    return upgradeProtocol
  }

  public SecureHttpConnectorTomcat setUpgradeProtocol(ConnectorUpgradeProtocolTomcat upgradeProtocol) {
    this.upgradeProtocol = upgradeProtocol
    return this
  }

  public SecureHttpConnectorTomcat setUpgradeProtocolToHttp2Protocol() {
    setUpgradeProtocol(new ConnectorUpgradeProtocolTomcat().setClassName(ConnectorUpgradeProtocolTomcat.PROTOCOL_CLASS_HTTP2))
    return this
  }

  //--------------

  public ConnectorSSLHostConfigTomcat[] getSSLHostConfigs() {
    return SSLHostConfigs
  }

  public SecureHttpConnectorTomcat setSSLHostConfigs(ConnectorSSLHostConfigTomcat... SSLHostConfigs) {
    this.SSLHostConfigs = SSLHostConfigs
    return this
  }

}
