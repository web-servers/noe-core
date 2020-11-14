package noe.tomcat.configure

/**
 * Abstraction for non-secure HTTP connector to configure Tomcat server.xml.
 * It is used for transfer data from user to `TomcatConfigurator`.
 * Provides default values if needed.
 *
 * Non-secure HTTP connector has set attributes `secure="false" always.
 *
 * IMPORTANT
 * <ul>
 *   <li>Not all connector attributes are supported. Only the most used ones.</li>
 *   <li>It is user responsibility to set values semantically, no validation is performed.</li>
 * <ul>
 *
 * @link https://tomcat.apache.org/tomcat-8.0-doc/config/http.html
 */
public class NonSecureHttpConnectorTomcat extends ConnectorTomcatAbstract<NonSecureHttpConnectorTomcat> {

  public NonSecureHttpConnectorTomcat() {
    super.setSecure(false)
    protocol = "HTTP/1.1"
    connectionTimeout = 20000
  }



  /**
   * Input argument secure is ignored secure is set 'false' always.
   */
  @Override
  NonSecureHttpConnectorTomcat setSecure(boolean secure) {
    return this
  }
}
