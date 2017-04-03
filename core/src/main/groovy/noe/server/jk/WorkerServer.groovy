package noe.server.jk

/**
 * TODO
 */
interface WorkerServer {

  void start()
  long stop()

  String getHost()
  void setHost(String host)

  Integer getAjpPort()
  void setAjpPort(Integer ajpPort)

}
