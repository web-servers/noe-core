package noe.workspace


class WorkspaceTomcatApacheDS extends WorkspaceAbstract {

  protected static WorkspaceTomcat workspaceTomcat
  protected static WorkspaceApacheDS workspaceApacheDs

  WorkspaceTomcatApacheDS() {
    workspaceTomcat = new WorkspaceTomcat()
    workspaceApacheDs = new WorkspaceApacheDS()
  }

  /**
   * Prepare the workspace.
   */
  def prepare() {
    workspaceTomcat.prepare()
    workspaceApacheDs.prepare()

    /// Backup all servers state
    serverController.backup()
  }

  /**
   * Destroy the workspace.
   */
  def destroy() {
    workspaceTomcat.destroy()
    workspaceApacheDs.destroy()
  }

  /**
   * Implementing of groovy fallback missingProeperty.
   *
   * @link http://groovy.codehaus.org/Using+methodMissing+and+propertyMissing
   */
  def propertyMissing(String name) {
    if (workspaceTomcat.hasProperty(name)) {
      workspaceTomcat.name
    } else if (workspaceApacheDs.hasProperty(name)) {
      workspaceApacheDs.name
    } else throw new RuntimeException("WorkspaceTomcatAapacheDS.propertyMissing(): WorkspaceTomcat nor WorkspaceApacheDS has property $name")
  }

}
