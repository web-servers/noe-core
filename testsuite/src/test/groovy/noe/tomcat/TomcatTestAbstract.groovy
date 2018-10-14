package noe.tomcat

import noe.common.TestAbstract
import noe.common.utils.Platform
import noe.server.Tomcat
import noe.workspace.ServersWorkspace
import noe.workspace.WorkspaceTomcat
import org.junit.Assume
import org.junit.Before

abstract class TomcatTestAbstract extends TestAbstract {
  Tomcat tomcat

  static void prepareWorkspace() {
    Assume.assumeFalse("EWS is not supported on HP-UX => skipping", new Platform().isHP())
    new ServersWorkspace(new WorkspaceTomcat()).prepare();
  }

  @Before
  void before() {
    String serverId = serverController.getTomcatServerIds().first()
    tomcat = serverController.getServerById(serverId)
  }

}
