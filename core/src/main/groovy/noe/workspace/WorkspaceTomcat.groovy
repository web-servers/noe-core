package noe.workspace

import groovy.util.logging.Slf4j

/**
 * @author Jan Stefl     <jstefl@redhat.com>
 * @author Michal Hasko  <mhasko@redhat.com>
 * @author Jan Onderka   <jonderka@redhat.com>
 */
@Slf4j
class WorkspaceTomcat extends WorkspaceAbstract {

  WorkspaceAbstract workspace

  WorkspaceTomcat() {
    if (context.consistsOf(['ews'])) {
      this.workspace = new WorkspaceTomcatEws()
    } else if (context.areInSingleGroup(['rhel', 'tomcat'])) {
      this.workspace = new WorkspaceTomcatBaseOS()
    } else {
      log.error("You are trying to create WorkspaceTomcat with unknown context: {}. Acceptable contexts are 'ews' and 'rhel-tomcat'",
          context.toString())
    }
  }

  /**
   * @param purge
   * @return
   */
  def prepare(Boolean purge = true) {
    log.debug('Preparing new TomcatWorkspace with options purge: {}', purge)
    if (context.consistsOf(['ews'])) {
      this.workspace.prepare(purge)
    } else if (context.areInSingleGroup(['rhel', 'tomcat'])) {
      log.debug("Purge option doesn't make sense with BaseOS Tomcat, ignoring option and continue.")
      this.workspace.prepare()
    } else {
      log.error("You are trying to interact with WorkspaceTomcat with unknown context: {}. Acceptable contexts are 'ews' and 'rhel-tomcat'",
          context.toString())
    }
    copyCertificates()
  }

  def destroy() {
    super.destroy()
    this.workspace.destroy()
  }
}
