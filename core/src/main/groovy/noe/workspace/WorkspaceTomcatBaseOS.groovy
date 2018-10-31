package noe.workspace

import noe.common.DefaultProperties
import noe.jbcs.utils.TomcatHelper
import noe.server.Tomcat
import groovy.util.logging.Slf4j
import noe.tomcat.configure.TomcatConfigurator

/**
 * Created by jonderka on 12/8/17.
 */
@Slf4j
class WorkspaceTomcatBaseOS extends WorkspaceAbstract {

  private List<TomcatConfigurator> tomcatConfiguratorInstances

  WorkspaceTomcatBaseOS() {
    def basedir = getBasedir()
    tomcatConfiguratorInstances = new LinkedList<TomcatConfigurator>()

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

  @Override
  def prepare() {
    super.prepare()

    if (DefaultProperties.SERVER_JAVA_HOME) {
      TomcatHelper.retrieveAllTomcatInstances().each { Tomcat tomcat ->
        tomcatConfiguratorInstances.add( new TomcatConfigurator(tomcat)
            .envVariableByAppend("JAVA_HOME", DefaultProperties.SERVER_JAVA_HOME))
      }
    }
  }

  @Override
  def destroy() {
    try {
      tomcatConfiguratorInstances.each { TomcatConfigurator tomcatConfigurator ->
        tomcatConfigurator.revertAllConfiguration()
      }
    } finally {
      super.destroy()
    }
  }
}
