package noe.workspace

import noe.server.ServerController

class ServersWorkspace implements IWorkspace {

  ServerController serverController = ServerController.getInstance();
  List<IWorkspace> serverWorkspaces

  ServersWorkspace(IWorkspace... workspaces) {
    this.serverWorkspaces = workspaces
  }

  @Override
  def prepare() {
    serverController.backup()
    serverWorkspaces.each { workspace -> workspace.prepare() }
  }

  @Override
  def destroy() {
    serverController.killAllInSystem()
    serverWorkspaces.each { workspace -> workspace.destroy() }
    serverController.clean()
  }
}
