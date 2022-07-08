package noe.tomcat.configure

/**
 * Abstraction for AJP connector to configure in Tomcat server.xml.
 * It is used for transfer data from user to `TomcatConfigurator`.
 * No default values are provided.
 *
 * IMPORTANT
 * <ul>
 *   <li>Not all connector attributes are supported. Only the most used ones.</li>
 *   <li>It is user responsibility to set values semantically, no validation is performed.</li>
 * <ul>
 *
 * @link https://tomcat.apache.org/tomcat-8.0-doc/config/http.html
 */
public class AjpConnectorTomcat extends ConnectorTomcatAbstract<AjpConnectorTomcat> {

  private Boolean secretRequired
  private String secret
  private String allowedRequestAttributesPattern

  public AjpConnectorTomcat() {
    protocol = "AJP/1.3"
  }

  Boolean getSecretRequired() {
    return secretRequired
  }

  String getSecret() {
    return secret
  }

  String getAllowedRequestAttributesPattern() {
    return allowedRequestAttributesPattern
  }

  public AjpConnectorTomcat setSecretRequired(Boolean secretRequired) {
    this.secretRequired = secretRequired
    return this
  }

  public AjpConnectorTomcat setSecret(String secret) {
    this.secret = secret
    return this
  }

  public AjpConnectorTomcat setAllowedRequestAttributesPattern(String allowedRequestAttributesPattern) {
    this.allowedRequestAttributesPattern = allowedRequestAttributesPattern
    return this
  }
}
