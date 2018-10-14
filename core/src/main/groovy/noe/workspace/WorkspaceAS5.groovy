package noe.workspace

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.utils.Cleaner
import noe.common.utils.IO
import noe.common.utils.Library
import noe.common.utils.Version
import noe.eap.server.ServerEap
import noe.eap.server.as5.AS5Properties
import noe.eap.utils.Eap5Utils
import noe.server.AS5

@Slf4j
class WorkspaceAS5 extends WorkspaceAbstract {

  boolean secured = false
  Version eapVersion = DefaultProperties.eapVersion()

  WorkspaceAS5(boolean secured = false, String serverId = null) {
    super()
    this.secured = secured
    IO.handleOutput('WorkspaceAS5(): BEGIN')

    if (secured) {
      this.basedir = Library.getUniversalProperty('as5.secured.basedir', getBasedir())
    } else {
      this.basedir = Library.getUniversalProperty('as5.unsecured.basedir', getBasedir())
    }

    def as5Profile
    if (secured) {
      as5Profile = Library.getUniversalProperty('as5.secured.profile', AS5Properties.PROFILE)
    } else {
      as5Profile = Library.getUniversalProperty('as5.unsecured.profile', AS5Properties.PROFILE)
    }
    def as5serverId = serverId ?: ServerEap.getPrefix()
    AS5 server = AS5.getInstance(basedir, '', context)
    server.setProfile(as5Profile)

    IO.handleOutput("HELL: servers map: ${serverController.servers}", IO.LOG_LEVEL_FINEST)
    IO.handleOutput("HELL: serverController.addServer(${as5serverId}, ${server.getBasedir()})", IO.LOG_LEVEL_FINEST)
    serverController.addServer(as5serverId, server)
    IO.handleOutput("HELL: servers map: ${serverController.servers}", IO.LOG_LEVEL_FINEST)
    initWorkspaceAS5()

    // initializing JMX user, it is needed for being able to properly shutdown the server
    server = serverController.getServerById(as5serverId)
    IO.handleOutput("HELL: serverController.getServerById(${as5serverId})=${server}", IO.LOG_LEVEL_FINEST)
    server.createManagementUser(AS5Properties.JMX_USER, AS5Properties.JMX_USER)
    IO.handleOutput('WorkspaceAS5(): END')
  }

  def prepare() {
    IO.handleOutput("Creating of new ${this.class.getName()} started")
    serverController.backup()
    IO.handleOutput("Creating of new ${this.class.getName()} finished")
  }

  def destroy() {
    super.destroy()
    IO.handleOutput("Destroying of default ${this.class.getName()} started")
    // TODO: Verify this
    if (!this.skipInstall) {
      if (deleteWorkspace) {
        Cleaner.cleanDirectoryBasedOnRegex(new File(getBasedir()), /.*(jboss-eap).*/)
        IO.handleOutput("EAP ${this.class.getName()} deleted: ${basedir}")
      }
      IO.handleOutput("EAP ${this.class.getName()} NOT deleted: ${basedir}")
    }
    IO.handleOutput("Destroying of default ${this.class.getName()} finished")
  }

  /**
   * Install AS5
   * Static dir expected.
   */
  void installAS5() {
    IO.handleOutput("${this.class.getName()}.installAS5(): EAP BEGIN", IO.LOG_LEVEL_FINER)
    def eap = new Eap5Utils(basedir, ant, platform, eapVersion.toString())
    eap.getIt(secured)
    IO.handleOutput("${this.class.getName()}.installAS5(): EAP END", IO.LOG_LEVEL_FINER)
  }

  /**
   * Initialize the workspace.
   */
  void initWorkspaceAS5() {
    IO.handleOutput("${this.class.getName()}.initWorkspaceAS5(): BEGIN", IO.LOG_LEVEL_FINER)
    if (!skipInstall) {
      installAS5()
    }
    IO.handleOutput("${this.class.getName()}.initWorkspaceAS5(): END", IO.LOG_LEVEL_FINER)
  }

}
