package noe.tomcat.configure

/**
 * Abstraction for HTTP connector, the element UpgradeProtocol.
 *
 * @link https://tomcat.apache.org/tomcat-8.5-doc/config/http2.html
 */
class ConnectorUpgradeProtocolTomcat {

  public static String PROTOCOL_CLASS_HTTP2 = "org.apache.coyote.http2.Http2Protocol"

  public String className


  public String getClassName() {
    return this.className
  }

  public setClassName(String className) {
    this.className = className
    return this
  }

}
