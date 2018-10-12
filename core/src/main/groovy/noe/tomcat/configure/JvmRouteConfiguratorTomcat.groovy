package noe.tomcat.configure;

/**
 * IMPORTANT: For usage within noe-core:tomcat.configure only
 *
 * Configure jvmRoute in server.xml
 */
class JvmRouteConfiguratorTomcat {

  private final Node server


  public JvmRouteConfiguratorTomcat(Node server) {
    this.server = server
  }

  public Node define(String jvmRoute) {
    server.Service.Engine.@jvmRoute = jvmRoute

    return server
  }
}
