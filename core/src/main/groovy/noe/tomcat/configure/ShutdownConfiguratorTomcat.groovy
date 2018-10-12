package noe.tomcat.configure

/**
 * IMPORTANT: For usage within noe-core:tomcat.configure only
 *
 * Shutdown configuration in Tomcat server.xml
 *
 * It is user responsibility to set attributes of connectors semantically.
 */
class ShutdownConfiguratorTomcat {

  private final Node server


  public ShutdownConfiguratorTomcat(Node server) {
    this.server = server
  }

  /**
   * Returns modified `ShutdownConfiguratorTomcat#server`
   */
  public Node define(ShutdownTomcat shutdown) {
    new ShutdownAttributesTransformer(shutdown).transform().each { attribute ->
      server.@"${attribute.key}" = attribute.value
    }

    return server
  }

  public Integer getPort() {
     return Integer.valueOf(server.@port.toString())
  }

}
