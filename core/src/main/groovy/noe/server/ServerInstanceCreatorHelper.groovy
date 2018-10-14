package noe.server

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.utils.JBFile


/**
 * Helper class for creating server instances based on ref basedir
 */
@Slf4j
class ServerInstanceCreatorHelper {

  private final ServerAbstract server

  ServerInstanceCreatorHelper(ServerAbstract server) {
    this.server = server
  }

  /**
   * Creates new server instance copy based on the refBasedir and shifts ports based on defined offset
   */
  ServerAbstract createNewServerInstancePhysicalCopy(int offset = DefaultProperties.DEFAULT_SHIFT_PORT_OFFSET) {
    if (!(new File(server.basedir).exists())) {
      log.trace("createNewServerInstance: refBasedir:${server.refBasedir}, basedir:${server.basedir}")
      if (!JBFile.copyDirectoryContent(new File(server.refBasedir), new File(server.basedir))) {
        throw new RuntimeException("Failed to create new server instance ${server.serverId} based on ${server.refBasedir}")
      }
    }
    server.shiftPorts(offset)
    return server
  }
}
