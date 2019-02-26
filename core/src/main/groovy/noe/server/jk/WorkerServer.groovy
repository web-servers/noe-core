package noe.server.jk

/**
 * Specify operations over worker nodes within JK scenarios.
 */
interface WorkerServer {

  String getServerId()

  void start()
  long stop()

  void shiftPorts(int offset)

  String getHost()
  void setHost(String host)

  Integer getAjpPort()
  void setAjpPort(Integer ajpPort)

  void deployByCopying(String appPath, Boolean explodeFirst, String contextName, Boolean zipAsWar)
  void undeployByDeleting(String appName)

}
