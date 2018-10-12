package noe.tomcat.configure

/**
 * Provides data for connectors specified in Tomcat server.xml.
 */
public class ShutdownAttributesTransformer {

  private final ShutdownTomcat shutdown


  ShutdownAttributesTransformer(ShutdownTomcat shutdown) {
    this.shutdown = shutdown
  }

  /**
   * Provides attributes for Shutdown element
   * Returns map of key:value corresponding with attributes in Tomcat server.xml.
   */
  public Map<String, Object> transform() {
    Map<String, Object> res = [:]

    if (shutdown.getClassName() != null && !shutdown.getClassName().isEmpty()) {
      res.put('className', shutdown.getClassName())
    }
    if (shutdown.getAddress() != null && !shutdown.getAddress().isEmpty()){
      res.put('address', shutdown.getAddress())
    }
    if (shutdown.getPort() != null && shutdown.getPort() > 0) {
      res.put('port', shutdown.getPort())
    }
    if (shutdown.getShutdown() != null && !shutdown.getShutdown().isEmpty()) {
      res.put('shutdown', shutdown.getShutdown())
    }

    return res
  }

}
