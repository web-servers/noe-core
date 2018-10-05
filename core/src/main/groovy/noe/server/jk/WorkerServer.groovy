package noe.server.jk

/**
 * Specify operations over worker nodes within JK scenarios.
 */
interface WorkerServer {

  void start()
  long stop()

  String getHost()
  void setHost(String host)

  Integer getAjpPort()
  void setAjpPort(Integer ajpPort)

}
