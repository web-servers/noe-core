package noe.eap.workspace

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.eap.server.ServerEap
import noe.ews.server.ServerEws
import noe.jbcs.utils.HttpdHelper
import noe.server.AS7
import noe.server.Httpd
/**
 *
 * @author Michal Karm Babacek <mbabacek@redhat.com>
 *
 */
@Slf4j
class WorkspaceMultipleHttpdAS7 extends WorkspaceHttpdAS7 {

  int numberOfAdditionalAS7s = 3

  WorkspaceMultipleHttpdAS7(Boolean installNativesZip, Boolean installWebConnectorsZip) {
    super(installNativesZip, installWebConnectorsZip)
  }

  @Override
  protected boolean skipHttpdPostInstall() {
    return true
  }

  def prepare() {
    def as7Dir = ''
    AS7 nextServer = null
    def id = ''
    for (int i = 2; i < numberOfAdditionalAS7s + 2; i++) {
      //AS7s
      id = "${ServerEap.getPrefix()}-${i}"
      log.info("Creating new AS7 server instance: ${id}")
      // if node2 is not defined - create default
      if (!serverController.getAs7ServerIds().contains(id)) {
        as7Dir = id
        nextServer = AS7.getInstance(basedir, id, context)
        nextServer.createNewServerInstance(id, DefaultProperties.DEFAULT_SHIFT_PORT_OFFSET * (i - 1))
        serverController.addServer(id, nextServer)
      }

      if (!DefaultProperties.USE_HTTPD_RPM) {
        def httpdDir
        //HTTPD
        if (platform.isWindows() || platform.isSolaris()) {
          if (DefaultProperties.apacheCoreVersion()) {
            httpdDir = "${DefaultProperties.HTTPD_CORE_DIR}-${i}"
          } else {
            httpdDir = "${ServerEws.getPrefix()}-${i}"
          }
        } else {
          httpdDir = "${Httpd.defaultServerId}-${i}"
        }
        id = "${Httpd.defaultServerId}-${i}"
        log.info("Creating new HTTPD server instance: ${id}")
        if (!serverController.httpdServerIds.contains(id)) {
          def httpdBasedir = getBasedir()
          if (workspaceHttpd.basedirHttpd) httpdBasedir = workspaceHttpd.basedirHttpd
          Httpd newHttpd = serverController.addServerHttpd(id, httpdBasedir, ServerEws.getHttpdVersion(), [host: workspaceHttpd.bindAddressHttpd], httpdDir)
          newHttpd.createNewServerInstance(id, DefaultProperties.DEFAULT_SHIFT_PORT_OFFSET * (i-1))
        }
      }
    }
    super.prepare()
    if (!DefaultProperties.USE_HTTPD_RPM) {
      HttpdHelper httpdHelper = new HttpdHelper(platform)
      serverController.getHttpdServerIds().each { httpdServerId ->
        Httpd httpd = serverController.getServerById(httpdServerId)
        httpdHelper.runPostinstallAndFixExecRights(httpd)
      }
    }
    /// Backup all servers state
    serverController.backup()
  }
}
