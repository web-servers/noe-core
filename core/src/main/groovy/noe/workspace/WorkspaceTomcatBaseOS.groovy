package noe.workspace

import noe.server.Tomcat
import groovy.util.logging.Slf4j

/**
 * Created by jonderka on 12/8/17.
 */
@Slf4j
class WorkspaceTomcatBaseOS extends WorkspaceAbstract{

  WorkspaceTomcatBaseOS() {
    def basedir = getBasedir()

    if (platform.isRHEL7()) {
      def server = Tomcat.getInstance(basedir, '7', '', context)
      serverController.addServer("tomcat-7-1", server)
    } else if (platform.isRHEL6()) {
      def server = Tomcat.getInstance(basedir, '6', '', context)
      serverController.addServer('tomcat-6-1', server)
    } else if (platform.isRHEL5()) {
      def server = Tomcat.getInstance(basedir, '5', '', context)
      serverController.addServer('tomcat-5-1', server)
    }
  }
}
