package noe.tomcat.configure

/**
 * Abstraction of connector within Tomcat server.xml
 * It is used for transfer data from user to `TomcatConfigurator`.
 * No default values are provided.
 *
 * Also see wrapper classes: `NonSecureHttpConnector`, `SecureHttpConnector`, `AjpConnector`
 *
 * IMPORTANT
 * <ul>
 *   <li>Not all connector attributes are supported. Only the most used ones.</li>
 *   <li>It is user responsibility to set values semantically, no validation is performed.</li>
 * <ul>
 */
abstract public class ConnectorTomcatAbstract<E extends ConnectorTomcatAbstract> {
  // - vvv ------------------------------------------------------
  // IMPORTANT: When adding support for new argument udpate class `ConnectorAttributesFactory` as well
  //
  // Connectors: HTTP[BIO, NIO, APR], AJP[BIO, NIO, APR] -----------
  // Attributes names are equal to those in server.xml
  // Common - to all connectors and implementations
  private Integer port
  private String protocol
  private Boolean secure
  private String scheme
  private Integer maxThreads
  private String address
  private Integer connectionTimeout
  private Integer redirectPort
  private String upgradeProtocol
  // - ^^^ ------------------------------------------------------


  public Integer getPort() {
    return this.port
  }

  public String getProtocol() {
    return protocol
  }

  public Boolean getSecure() {
    return secure
  }

  public String getScheme() {
    return scheme
  }

  public Integer getMaxThreads() {
    return maxThreads
  }

  public String getAddress() {
    return address
  }

  public Integer getConnectionTimeout() {
    return connectionTimeout
  }

  public Integer getRedirectPort() {
    return redirectPort
  }

  public String getUpgradeProtocol() {
    return upgradeProtocol
  }

  public E setPort(int port) {
    this.port = port
    return (E) this
  }

  public E setProtocol(String protocol) {
    this.protocol = protocol
    return (E) this
  }

  public E setSecure(boolean secure) {
    this.secure = secure
    return (E) this
  }

  public E setMaxThreads(int maxThreads) {
    this.maxThreads = maxThreads
    return (E) this
  }

  public E setAddress(String address) {
    this.address = address
    return (E) this
  }

  public E setConnectionTimeout(int connectionTimeout) {
    this.connectionTimeout = connectionTimeout
    return (E) this
  }

  public E setRedirectPort(int redirectPort) {
    this.redirectPort = redirectPort
    return (E) this
  }

  public E setScheme(String scheme) {
    this.scheme = scheme
    return (E) this
  }

  public E setUpgradeProtocol(String upgradeProtocol) {
    this.upgradeProtocol = upgradeProtocol
    return (E) this
  }
  public E setUpgradeProtocolToHttp2Protocol() {
    setUpgradeProtocol('org.apache.coyote.http2.Http2Protocol')
    return (E) this
  }

}
