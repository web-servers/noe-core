package noe.tomcat.configure

/**
 * Abstraction for Certificate inside SSLHostConfig tag of either Secure or Non-Secure Connector in Tomcat server.xml.
 * It is used for transfer data from user to `TomcatConfigurator`.
 * Provides default values if needed.
 * *
 * IMPORTANT
 * <ul>
 *   <li>Not all Certificate attributes are supported. Only the most used ones.</li>
 *   <li>It is user responsibility to set values semantically, no validation is performed.</li>
 * <ul>
 *
 * @link https://tomcat.apache.org/tomcat-9.0-doc/config/http.html#SSL_Support_-_Certificate
 */

class ConnectorCertificateTomcat {

  public String certificateFile
  public String certificateChainFile
  public String certificateKeyAlias
  public String certificateKeyFile
  public String certificateKeyPassword
  public String certificateKeystoreFile
  public String certificateKeystorePassword
  public String certificateKeystoreProvider
  public String certificateKeystoreType
  public String certificateType

  public String getCertificateFile() {
    return this.certificateFile
  }

  public String getCertificateChainFile() {
    return this.certificateChainFile
  }

  public String getCertificateKeyAlias() {
    return this.certificateKeyAlias
  }

  public String getCertificateKeyFile() {
    return this.certificateKeyFile
  }

  public String getCertificateKeyPassword() {
    return this.certificateKeyPassword
  }

  public String getCertificateKeystoreFile() {
    return this.certificateKeystoreFile
  }

  public String getCertificateKeystorePassword() {
    return this.certificateKeystorePassword
  }

  public String getCertificateKeystoreProvider() {
    return this.certificateKeystoreProvider
  }

  public String getCertificateKeystoreType() {
    return this.certificateKeystoreType
  }

  public String getCertificateType() {
    return this.certificateType
  }

  public ConnectorCertificateTomcat setCertificateFile(String certificateFile) {
    this.certificateFile = certificateFile
    return this
  }

  public ConnectorCertificateTomcat setCertificateChainFile(String certificateChainFile) {
    this.certificateChainFile = certificateChainFile
    return this
  }

  public ConnectorCertificateTomcat setCertificateKeyAlias(String certificateKeyAlias) {
    this.certificateKeyAlias = certificateKeyAlias
    return this
  }

  public ConnectorCertificateTomcat setCertificateKeyFile(String certificateKeyFile) {
    this.certificateKeyFile = certificateKeyFile
    return this
  }

  public ConnectorCertificateTomcat setCertificateKeyPassword(String certificateKeyPassword) {
    this.certificateKeyPassword = certificateKeyPassword
    return this
  }

  public ConnectorCertificateTomcat setCertificateKeystoreFile(String certificateKeystoreFile) {
    this.certificateKeystoreFile = certificateKeystoreFile
    return this
  }

  public ConnectorCertificateTomcat setCertificateKeystorePassword(String certificateKeystorePassword) {
    this.certificateKeystorePassword = certificateKeystorePassword
    return this
  }

  public ConnectorCertificateTomcat setCertificateKeystoreType(String certificateKeystoreType) {
    this.certificateKeystoreType = certificateKeystoreType
    return this
  }

  public ConnectorCertificateTomcat setCertificateKeystoreProvider(String certificateKeystoreProvider) {
    this.certificateKeystoreProvider = certificateKeystoreProvider
    return this
  }

  public ConnectorCertificateTomcat setCertificateType(String certificateType) {
    this.certificateType = certificateType
    return this
  }

}
