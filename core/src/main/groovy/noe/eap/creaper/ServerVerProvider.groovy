package noe.eap.creaper

import groovy.util.logging.Slf4j
import noe.common.utils.Version
import noe.server.AS7
import noe.server.ServerAbstract
import noe.server.ServerController
import org.wildfly.extras.creaper.core.ServerVersion

/**
 * Class providing management API versions for AS7 instance identified either by its object or id. It can also
 * translate EAP version to management API version.
 *
 * @author Jan Kasik <jkasik@redhat.com>
 */
@Slf4j
class ServerVerProvider {

  /**
   * Get management API version of target server instance
   * @param serverInstance AS7 instance
   * @return Management API version wrapped in {@link ServerVersion}
   */
  static ServerVersion provideFor(ServerAbstract serverInstance) {
    if (serverInstance instanceof AS7) {
      return ManagementClientProvider.createOfflineManagementClient(serverInstance as AS7).version()
    } else {
      throw new IllegalArgumentException("Server instance must be an EAP/Wildfly node!")
    }
  }

  /**
   * Get management API version of target server instance
   * @param id of server fow which the management API version will be provided
   * @return Management API version wrapped in {@link ServerVersion}
   */
  static ServerVersion provideFor(String serverID) {
    final ServerController serverController = ServerController.getInstance()
    return provideFor(serverController.getServerById(serverID) as AS7)
  }

  /**
   * Get management API version from list of AS7 server IDs. It is expected, that all instances have same version.
   * @param serverIds list of AS7 server ids
   * @return Management API version wrapped in {@link ServerVersion}
   */
  static ServerVersion provideFor(Set<String> serverIds) {
    if (!serverIds) {
      throw new IllegalArgumentException("Server ID list is either null or empty!")
    }
    final List<ServerVersion> versions = serverIds.collect({ final String id -> provideFor(id) })
    final Set<ServerVersion> versionSet = versions.toSet()
    if (versionSet.size() == 1) {
      log.debug("Common version for '${serverIds}' is '${versionSet.first()}'.")
      return versionSet.first()
    } else {
      throw new IllegalStateException("Versions of provided instances are not same!")
    }
  }

  /**
   * Get management API version used in specified EAP version. Use only for legacy purposes and prefer comparing to
   * {@link ServerVersion}. {@link IllegalArgumentException} is thrown if conversion for given version is not
   * supported.
   * @param eapVersion version of EAP, 6.0.0 to 7.1.0
   * @return Management API version wrapped in {@link ServerVersion}
   */
  static ServerVersion getMngmtVerOfEAP(String eapVersion) {
    final Version eap = new Version(eapVersion)
    if (eap.getMajorVersion() == 7) {
      if (eap.getMinorVersion() == 0) {
        return ServerVersion.VERSION_4_1_0
      } else {
        return ServerVersion.VERSION_5_0_0
      }
    } else if (eap.getMajorVersion() == 6) {
      switch (eap.getMinorVersion()) {
        case 0:
          if (eap.getIncrementalVersion() == 0) {
            return ServerVersion.VERSION_1_2_0
          } else {
            return ServerVersion.VERSION_1_3_0
          }
          break
        case 1:
          return ServerVersion.VERSION_1_4_0
        case 2:
          return ServerVersion.VERSION_1_5_0
        case 3:
          return ServerVersion.VERSION_1_6_0
        case 4:
          if (eap.getIncrementalVersion() >= 0 && eap.getIncrementalVersion() < 7) {
            return ServerVersion.VERSION_1_7_0
          } else {
            return ServerVersion.VERSION_1_8_0
          }
        default: throw new IllegalArgumentException("Not implemented for EAP version " + eapVersion)
      }
    } else {
      throw new IllegalArgumentException("Not implemented for EAP version " + eapVersion)
    }
  }

}
