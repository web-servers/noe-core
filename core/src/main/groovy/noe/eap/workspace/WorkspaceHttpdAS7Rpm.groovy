package noe.eap.workspace

import groovy.util.logging.Slf4j

/**
 *
 * @author Filip Goldefus <fgoldefu@redhat.com>
 *
 */
@Slf4j
class WorkspaceHttpdAS7Rpm extends WorkspaceHttpdAS7 {

  WorkspaceHttpdAS7Rpm(Boolean installNativesZip = false, Boolean installWebConnectorsZip = false) {
    super(installNativesZip, installWebConnectorsZip)
    // Reentrant feature - update of the config to initial values
    serverController.getAs7ServerIds().each { as7 ->
      serverController.getServerById(as7).shiftPorts(0)
    }
  }
}
