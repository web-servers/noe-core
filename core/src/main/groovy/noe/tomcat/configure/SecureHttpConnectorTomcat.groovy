package noe.tomcat.configure
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
  private boolean clientAuth

  // SSL APR
  private String sslCertificateFile
  private String sslCertificateKeyFile
  private String sslPassword
  // - ^^^ ------------------------------------------------------

  public SecureHttpConnectorTomcat() {
    super.setSecure(true)
    sslEnabled = true
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

  public String getSslCertificateFile() {
    return this.sslCertificateFile
  }

  public String getSslCertificateKeyFile() {
    return this.sslCertificateKeyFile
  }

  public String getSslPassword() {
    return this.sslPassword
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

  public SecureHttpConnectorTomcat setClientAuth(boolean clientAuth) {
    this.clientAuth = clientAuth
    return this
  }

  public SecureHttpConnectorTomcat setSslCertificateFile(String sslCertificateFile) {
    this.sslCertificateFile = sslCertificateFile
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

}
