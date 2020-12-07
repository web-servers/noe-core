package noe.tomcat.configure

/**
 * Abstraction for SSLHostConfig element of either Secure or Non-Secure Connector in Tomcat server.xml.
 * It is used for transfer data from user to `TomcatConfigurator`.
 * Provides default values if needed.
 * *
 * IMPORTANT
 * <ul>
 *   <li>Not all SSLHostConfig attributes are supported. Only the most used ones.</li>
 *   <li>It is user responsibility to set values semantically, no validation is performed.</li>
 * <ul>
 *
 * @link https://tomcat.apache.org/tomcat-9.0-doc/config/http.html#SSL_Support_-_SSLHostConfig
 */
class ConnectorSSLHostConfigTomcat {

  public String caCertificateFile
  public String caCertificatePath
  public String ciphers
  public String sslProtocol
  public String protocols
  public String truststoreFile
  public String truststorePassword
  public String truststoreProvider
  public String truststoreType

  public String getCaCertificateFile() {
    return this.caCertificateFile
  }

  public String getCaCertificatePath() {
    return this.caCertificatePath
  }

  public String getCiphers() {
    return this.ciphers
  }

  public String getSslProtocol() {
    return this.sslProtocol
  }

  public String getProtocols() {
    return this.protocols
  }

  public String getTruststoreFile() {
    return this.truststoreFile
  }

  public String getTruststorePassword() {
    return this.truststorePassword
  }

  public String getTruststoreProvider() {
    return this.truststoreProvider
  }

  public String getTruststoreType() {
    return this.truststoreType
  }

  public ConnectorSSLHostConfigTomcat setCaCertificateFile(String caCertificateFile) {
    this.caCertificateFile = caCertificateFile
    return this
  }

  public ConnectorSSLHostConfigTomcat setCaCertificatePath(String caCertificatePath) {
    this.caCertificatePath = caCertificatePath
    return this
  }

  public ConnectorSSLHostConfigTomcat setCiphers(String ciphers) {
    this.ciphers = ciphers
    return this
  }

  public ConnectorSSLHostConfigTomcat setSSLProtocol(String sslProtocol) {
    this.sslProtocol = sslProtocol
    return this
  }

  public ConnectorSSLHostConfigTomcat setProtocols(String protocols) {
    this.protocols = protocols
    return this
  }

  public ConnectorSSLHostConfigTomcat setTruststoreFile(String truststoreFile) {
    this.truststoreFile = truststoreFile
    return this
  }

  public ConnectorSSLHostConfigTomcat setTruststorePassword(String truststorePassword) {
    this.truststorePassword = truststorePassword
    return this
  }

  public ConnectorSSLHostConfigTomcat setTruststoreProvider(String truststoreProvider) {
    this.truststoreProvider = truststoreProvider
    return this
  }

  public ConnectorSSLHostConfigTomcat setTruststoreType(String truststoreType) {
    this.truststoreType = truststoreType
    return this
  }

}