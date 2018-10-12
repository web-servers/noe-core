package noe.eap.workspace

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.utils.Library
import noe.eap.server.ServerEap
import noe.eap.server.as7.AS7Rpm
import noe.ews.server.ServerEws
import noe.jbcs.utils.HttpdHelper
import noe.server.Httpd
/**
 *
 * @author Filip Goldefus <fgoldefu@redhat.com>
 *
 */
@Slf4j
class WorkspaceMultipleHttpdAS7Rpm extends WorkspaceHttpdAS7Rpm {

  int numberOfAdditionalAS7s = 2

  WorkspaceMultipleHttpdAS7Rpm(Boolean installNativesZip = false, Boolean installWebConnectorsZip = false) {
    super(installNativesZip, installWebConnectorsZip)
  }

  @Override
  protected boolean skipHttpdPostInstall() {
    return true
  }

  def prepare() {

    AS7Rpm nextServer

    for (int i = 2; i < numberOfAdditionalAS7s + 2; i++) {
      //AS7s
      def id = "${ServerEap.getPrefix()}-${i}"
      log.info("Creating new AS7 server instance: ${id}")
      if (!serverController.getAs7ServerIds().contains(id)) {
        def offset = DefaultProperties.DEFAULT_SHIFT_PORT_OFFSET * (i - 1)
        nextServer = AS7Rpm.getInstance(basedir, id, context)
        nextServer.createNewServerInstance(id, offset)
        serverController.addServer(id, nextServer)
      }

      if (!DefaultProperties.USE_HTTPD_RPM) {
        def httpdDir
        //HTTPD
        if (platform.isSolaris() || platform.isWindows()) {
          httpdDir = "${ServerEws.getPrefix()}-${i}"
        } else {
          httpdDir = "${Httpd.defaultServerId}-${i}"
        }
        id = "${Httpd.defaultServerId}-${i}"
        if (!serverController.httpdServerIds.contains(id)) {
          def httpdBasedir = getBasedir()
          if (workspaceHttpd.basedirHttpd) httpdBasedir = workspaceHttpd.basedirHttpd
          Httpd newHttpd = serverController.addServerHttpd(id, httpdBasedir, ServerEws.getHttpdVersion(),
                  [host: workspaceHttpd.bindAddressHttpd], httpdDir)
          newHttpd.createNewServerInstance(id, i)
        }
      }

      if (!DefaultProperties.USE_HTTPD_RPM) {
        HttpdHelper httpdHelper = new HttpdHelper(platform)
        serverController.getHttpdServerIds().each { httpdServerId ->
          Httpd httpd = serverController.getServerById(httpdServerId)
          httpdHelper.runPostinstallAndFixExecRights(httpd)
        }
      }
    }

    super.prepare()
    updateWorkersConfig()
  }

  void updateWorkersConfig() {
    def hostIpAddress = Library.getHostIpAddress()
    serverController.getAs7ServerIds().each { as7 ->
      serverController.updateAS7ConfReplaceRegExp(as7, 'jboss.bind.address.unsecure:[^}]*', 'jboss.bind.address.unsecure:' + hostIpAddress)
      serverController.updateAS7ConfReplaceRegExp(as7, 'jboss.bind.address:[^}]*', 'jboss.bind.address:' + hostIpAddress)
    }
  }
}
