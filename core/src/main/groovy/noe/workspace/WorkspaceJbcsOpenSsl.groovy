package noe.workspace

import groovy.util.logging.Slf4j
import noe.common.utils.Library
import noe.jbcs.utils.CoreServicesOpenSslHelper
import noe.server.OpensslServer

@Slf4j
class WorkspaceJbcsOpenSsl extends WorkspaceAbstract {

  private String basedirJbcsOpenSsl /// Workspace directory path where jbcs openssl will be unzipped
  private String opensslDir /// Where JBCS OpenSsl files can be found

  WorkspaceJbcsOpenSsl() {
    initJbcsOpenSslWorkspace()
  }

  private void initJbcsOpenSslWorkspace() {
    log.trace(WorkspaceJbcsOpenSsl.getSimpleName() + '.initJbcsOpenSslWorkspace(): BEGIN')
    this.basedirJbcsOpenSsl = Library.getUniversalProperty('basedir.jbcs.openssl', getBasedir())
    log.trace(WorkspaceJbcsOpenSsl.getSimpleName() + "JBCS OpenSsl basedir $basedirJbcsOpenSsl")
    log.trace(WorkspaceJbcsOpenSsl.getSimpleName() + '.initJbcsOpenSslWorkspace(): END')
  }

  def prepare(Boolean purge = true) {
    log.trace('Preparing of JBCS OpenSsl directory started')

    // TODO purge?
    CoreServicesOpenSslHelper jbcsOpenSsl = new CoreServicesOpenSslHelper(basedirJbcsOpenSsl)
    jbcsOpenSsl.installZipFile()
    serverController.addServerOpenssl(OpensslServer.defaultServerId, basedirJbcsOpenSsl)

    this.opensslDir = basedirJbcsOpenSsl + File.separator + jbcsOpenSsl.getCoreOpenSslDirName() + File.separator + "openssl"

    log.trace('Preparing of JBCS OpenSsl directory finished')
  }

  def destroy() {
    super.destroy()
  }

}
