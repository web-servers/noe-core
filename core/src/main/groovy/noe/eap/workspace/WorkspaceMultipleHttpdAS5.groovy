package noe.eap.workspace

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.utils.IO
import noe.eap.server.ServerEap
import noe.ews.server.ServerEws
import noe.jbcs.utils.HttpdHelper
import noe.server.AS5
import noe.server.Httpd
/**
 *
 * @author Michal Karm Babacek <mbabacek@redhat.com>
 *
 */
@Slf4j
class WorkspaceMultipleHttpdAS5 extends WorkspaceHttpdAS5 {

  int numberOfAdditionalAS5s = 3

  WorkspaceMultipleHttpdAS5(installNativesZip, installWebConnectors) {
    super(installNativesZip, installWebConnectors)
  }

  def prepare() {
    def as5Dir = ''
    AS5 nextServer = null
    def id = ''
    for (int i = 2; i < numberOfAdditionalAS5s + 2; i++) {
      //AS5s
      id = "${ServerEap.getPrefix()}-${i}"
      log.info("Creating new AS5 server instance: ${id}")
      // if node2 is not defined - create default
      if (!serverController.getAs5ServerIds().contains(id)) {
        as5Dir = id
        nextServer = AS5.getInstance(basedir, id, context)
        nextServer.createNewServerInstance(id, DefaultProperties.DEFAULT_SHIFT_PORT_OFFSET * (i - 1))
        serverController.addServer(id, nextServer)
      }

      def httpdDir
      //HTTPD
      if (platform.isWindows() || platform.isSolaris()) {
        httpdDir = "${ServerEws.getPrefix()}-${i}"
      } else {
        httpdDir = "${Httpd.defaultServerId}-${i}"
      }
      id = "${Httpd.defaultServerId}-${i}"
      if (!serverController.httpdServerIds.contains(id)) {
        def httpdBasedir = getBasedir()
        if (workspaceHttpd.basedirHttpd) httpdBasedir = workspaceHttpd.basedirHttpd
        IO.handleOutput "TAGMONOCO: httpdBasedir:${httpdBasedir}"
        Httpd newHttpd = serverController.addServerHttpd(id, httpdBasedir, ServerEws.getHttpdVersion(), [host: workspaceHttpd.bindAddressHttpd], httpdDir)
        newHttpd.createNewServerInstance(id, i)
      }

      super.prepare()
    }

    serverController.getHttpdServerIds().each { httpdServerId ->
      IO.handleOutput "TAGMONOYYO: httpdBasedir:${httpdServerId}"
      if (httpdServerId != Httpd.defaultServerId) {
        Httpd httpd = serverController.getServerById(httpdServerId)
        HttpdHelper httpdHelper = new HttpdHelper(platform)
        httpdHelper.runPostinstallAndFixExecRights(httpd)
      }
    }

    /// Backup all servers state
    serverController.backup()
  }
}
