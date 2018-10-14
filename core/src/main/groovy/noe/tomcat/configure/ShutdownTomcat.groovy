package noe.tomcat.configure;

/**
 * Abstraction of Shutdown attributes in Tomcat server.xml.
 *
 * https://tomcat.apache.org/tomcat-8.0-doc/config/server.html
 */
public class ShutdownTomcat {

  private String className
  private String address
  private Integer port
  private String shutdown


  String getClassName() {
    return className
  }

  String getAddress() {
    return address
  }

  Integer getPort() {
    return port
  }

  String getShutdown() {
    return shutdown
  }

  ShutdownTomcat setClassName(String className) {
    this.className = className
    return this
  }

  ShutdownTomcat setAddress(String address) {
    this.address = address
    return this
  }

  ShutdownTomcat setPort(Integer port) {
    this.port = port
    return this
  }

  ShutdownTomcat setShutdown(String shutdown) {
    this.shutdown = shutdown
    return this
  }


}
