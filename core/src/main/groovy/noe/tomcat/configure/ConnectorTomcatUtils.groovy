package noe.tomcat.configure


final class ConnectorTomcatUtils {
  public static final String PROTOCOL_AJP_13 = "AJP/1.3"
  public static final String PROTOCOL_CLASS_AJP = "org.apache.coyote.ajp.AjpProtocol"
  public static final String PROTOCOL_CLASS_AJP_NIO = "org.apache.coyote.ajp.AjpNioProtocol"
  public static final String PROTOCOL_CLASS_AJP_APR = "org.apache.coyote.ajp.AjpAprProtocol"

  public static String PROTOCOL_HTTP_11 = "HTTP/1.1"
  public static String PROTOCOL_CLASS_HTTP_11 = "org.apache.coyote.http11.Http11Protocol"
  public static String PROTOCOL_CLASS_HTTP_NIO = "org.apache.coyote.http11.Http11NioProtocol"
  public static String PROTOCOL_CLASS_HTTP_APR = "org.apache.coyote.http11.Http11AprProtocol"

  private ConnectorTomcatUtils() {
    // no instance creation
  }

  static retrieveAllHttpProtocols() {
    return [
      PROTOCOL_HTTP_11,
      PROTOCOL_CLASS_HTTP_11,
      PROTOCOL_CLASS_HTTP_NIO,
      PROTOCOL_CLASS_HTTP_APR
    ]
  }

  static retrieveAllAjpProtocols() {
    return [
      PROTOCOL_AJP_13,
      PROTOCOL_CLASS_AJP,
      PROTOCOL_CLASS_AJP_NIO,
      PROTOCOL_CLASS_AJP_APR
    ]
  }

}
