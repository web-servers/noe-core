package noe.ews.workspace

import groovy.util.logging.Slf4j
import noe.common.utils.IO
import noe.ews.server.ServerEws
import noe.jbcs.utils.HttpdHelper
import noe.server.Httpd
import noe.workspace.WorkspaceHttpd
import noe.workspace.WorkspaceMultipleTomcats
import noe.workspace.WorkspaceTomcat

/**
 *
 * @author Paul Lodge <plodge@redhat.com>
 *
 */
@Slf4j
class WorkspaceMultipleHttpdTomcats extends WorkspaceHttpd {

  private int numberOfTomcatInstances
  private int numberOfHttpdInstances

  private static WorkspaceMultipleTomcats workspaceMultipleTomcats
  private static WorkspaceTomcat workspaceTomcat

  WorkspaceMultipleHttpdTomcats(int numberOfTomcatInstances = 3, int numberOfHttpdInstances = 1) {
      this.numberOfTomcatInstances = numberOfTomcatInstances
      this.numberOfHttpdInstances = numberOfHttpdInstances

      workspaceTomcat = new WorkspaceTomcat()
      workspaceMultipleTomcats = new WorkspaceMultipleTomcats()
  }

  def prepare() {

    // Httpd prepare
    skipHttpdPostInstall = true
    super.prepare(true)

    // Tomcats
    workspaceTomcat.prepare(true)
    workspaceMultipleTomcats.createAdditionalTomcatsWithRegistrations(numberOfTomcatInstances)

    // Additional Httpds
    String id = ""
    // we start from 1 as there is already an httpd instance
    for (int i = 1; i < numberOfHttpdInstances; i++) {

      def httpdDir
      if (platform.isWindows() || platform.isSolaris()) {
        httpdDir = "${ServerEws.getPrefix()}-${i}"
      } else {
        httpdDir = "${Httpd.defaultServerId}-${i}"
      }

      id = "${Httpd.defaultServerId}-${i}"
      if (!serverController.httpdServerIds.contains(id)) {
        def httpdBasedir = getBasedir()
        if (super.basedirHttpd) httpdBasedir = super.basedirHttpd
        IO.handleOutput "HTTPD Base Directory: httpdBasedir:${httpdBasedir}"
        Httpd newHttpd = serverController.addServerHttpd(id, httpdBasedir, ServerEws.getHttpdVersion(), [host: super.bindAddressHttpd], httpdDir)
        newHttpd.createNewServerInstance(id, i)
      }
    }

    // Call the postinstall script on ALL the httpds
    serverController.getHttpdServerIds().each { httpdServerId ->
      IO.handleOutput "HTTPD Base Directory: httpdBasedir:${httpdServerId}"
        Httpd httpd = serverController.getServerById(httpdServerId)
        HttpdHelper httpdHelper = new HttpdHelper(platform)
        httpdHelper.runPostinstallAndFixExecRights(httpd)
    }

    /// Backup all servers state
    serverController.backup()
  }
}
