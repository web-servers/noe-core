package noe.tomcat.configure;

/**
 * Abstraction of JMX attributes.
 *
 * https://tomcat.apache.org/tomcat-8.0-doc/monitoring.html
 */
public class JmxTomcat {

  private Integer port
  private Boolean ssl
  private Boolean authenticate
  private JmxRemoteAccessFileTomcat accessFile
  private JmxRemotePasswordFileTomcat passwordFile


  Integer getPort() {
    return port
  }

  Boolean getSsl() {
    return ssl
  }

  Boolean getAuthenticate() {
    return authenticate
  }

  JmxRemoteAccessFileTomcat getAccessFile() {
    return accessFile
  }

  JmxRemotePasswordFileTomcat getPasswordFile() {
    return passwordFile
  }

  JmxTomcat setPort(Integer port) {
    this.port = port
    return this
  }

  JmxTomcat setSsl(Boolean ssl) {
    this.ssl = ssl
    return this
  }

  JmxTomcat setAuthenticate(Boolean authenticate) {
    this.authenticate = authenticate
    return this
  }

  JmxTomcat setAccessFile(JmxRemoteAccessFileTomcat accessFile) {
    this.accessFile = accessFile
    return this
  }

  JmxTomcat setPasswordFile(JmxRemotePasswordFileTomcat passwordFile) {
    this.passwordFile = passwordFile
    return this
  }

}
