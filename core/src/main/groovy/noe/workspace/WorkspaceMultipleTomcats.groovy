package noe.workspace

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.ews.server.tomcat.TomcatProperties
import noe.server.Tomcat

/**
 * Helper Workspace for multiple tomcat instance setup
 */
@Slf4j
class WorkspaceMultipleTomcats extends WorkspaceAbstract{

  WorkspaceMultipleTomcats() {
    super()
  }

  void createAdditionalTomcatsWithRegistrations(int numberOfAdditionalTomcats) {
    for (int i = 2; i < numberOfAdditionalTomcats + 2; i++) {
      // We start tests always with only one tomcat
      String tomcatVersion = TomcatProperties.TOMCAT_MAJOR_VERSION
      String id = "tomcat-${tomcatVersion}-${i}"
      log.info("Creating new tomcat server instance: ${id}")
      // if node2 is not defined - create default
      if (!serverController.getTomcatServerIds([TomcatProperties.TOMCAT_MAJOR_VERSION]).contains(id)) {
        String tomcatDir = id
        Tomcat nextServer = (Tomcat) Tomcat.getInstance(basedir, tomcatVersion.toString(), tomcatDir, context)
        nextServer.createNewServerInstance(id, DefaultProperties.DEFAULT_SHIFT_PORT_OFFSET * (i - 1))

        serverController.addServer(id, nextServer)
      }
    }
  }

  Map<String,String> originalTomcatHostsIpAddresses(String hostIpAddress = DefaultProperties.HOST) {
    Map<String,String> originalTomcatHosts = [:]
    Set<String> tomcatIds = serverController.getTomcatServerIds([TomcatProperties.TOMCAT_MAJOR_VERSION])
    tomcatIds.each { tomcatId ->
      Tomcat tomcat = (Tomcat) serverController.getServerById(tomcatId)
      String host = tomcat.getHost()
      originalTomcatHosts.put(tomcatId, host)
      if (host == null || host.isEmpty() || host.equals(DefaultProperties.HOST)) {
        tomcat.setHost(hostIpAddress)
      }
    }
    return originalTomcatHosts
  }
}
