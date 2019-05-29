package noe.eap.creaper

import noe.common.utils.Library
import noe.server.AS7
import org.wildfly.extras.creaper.core.ManagementClient
import org.wildfly.extras.creaper.core.ServerVersion
import org.wildfly.extras.creaper.core.offline.OfflineManagementClient
import org.wildfly.extras.creaper.core.offline.OfflineOptions
import org.wildfly.extras.creaper.core.online.ManagementProtocol
import org.wildfly.extras.creaper.core.online.OnlineManagementClient
import org.wildfly.extras.creaper.core.online.OnlineOptions

/**
 * Provider for Creaper's {@link OnlineManagementClient} and {@link OfflineManagementClient}
 */
final class ManagementClientProvider {

  /**
   * Creates {@link OnlineManagementClient} for standalone instance
   * @param serverInstance target server instance which will be managed by this client
   * @return Initialized OnlineManagementClient, don't forget to close it
   */
  static OnlineManagementClient createOnlineManagementClient(AS7 serverInstance) {
    int port = serverInstance.getManagementNativePort()
    ManagementProtocol protocol = ManagementProtocol.REMOTE
    if (ServerVerProvider.provideFor(serverInstance).greaterThan(ServerVersion.VERSION_1_8_0)) {
      //EAP version > 6.4
      port = serverInstance.getManagementHttpPort()
      protocol = ManagementProtocol.HTTP_REMOTING
    }

    return ManagementClient.onlineLazy(OnlineOptions.standalone()
            .hostAndPort(Library.getUniversalProperty("eap.creaper.client.online.host", serverInstance.getHost()),
                    Integer.parseInt(Library.getUniversalProperty("eap.creaper.client.online.managementport",
                            port.toString())))
            .protocol(protocol)
            .connectionTimeout(Integer.parseInt(Library.getUniversalProperty("eap.creaper.client.online.connectiontimeout", 10000)))
            .bootTimeout(Integer.parseInt(Library.getUniversalProperty("eap.creaper.client.online.boottimeout", 20000)))
            .build())
  }

  /**
   * Creates {@link OfflineManagementClient} for standalone instance
   * @param serverInstance target server instance which will be managed by this client
   * @return Initialized OfflineManagementClient
   */
  static OfflineManagementClient createOfflineManagementClient(AS7 serverInstance) {

    return ManagementClient.offline(OfflineOptions.standalone()
            .rootDirectory(new File(serverInstance.getBasedir()))
            .configurationFile(serverInstance.getConfigFile())
            .build())
  }

}
