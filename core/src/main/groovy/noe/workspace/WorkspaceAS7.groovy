package noe.workspace

import groovy.util.logging.Slf4j
import noe.common.utils.IO
import noe.common.utils.JBFile
import noe.common.utils.Library
import noe.eap.server.ServerEap
import noe.server.AS7


/**
 *
 * @author Michal Karm Babacek <mbabacek@redhat.com>
 *
 */
@Slf4j
class WorkspaceAS7 extends WorkspaceAbstract {

  WorkspaceAS7(String serverId = null) {
    IO.handleOutput('WorkspaceAS7(): BEGIN')
    def basedir = getBasedir()
    def as7ServerId = serverId ?: ServerEap.getPrefix()

    def server = AS7.getInstance(basedir, '', context)
    serverController.addServer("${as7ServerId}", server)
    IO.handleOutput('WorkspaceAS7(): END')
  }

  def prepare() {
    IO.handleOutput('Creating of new WorkspaceAS7 started')

    // jboss-cli.bat does not work when EAP installed into directory with spaces
    if (Boolean.valueOf(Library.getUniversalProperty("WINDOWS_PATH_WORKAROUND_BZ1031173", false))) {
      if (platform.isWindows()) {
        serverController.getAs7ServerIds().each { serverId ->
          def myserver = serverController.getServerById(serverId)
          def filePath = myserver.basedir + platform.sep + myserver.cliPath + platform.sep + "jboss-cli.bat"
          JBFile.replace(new File(filePath), "set \"JAVA_OPTS=%JAVA_OPTS% -Djboss.modules", "set JAVA_OPTS=\"%JAVA_OPTS% -Djboss.modules", true)
        }
      }
    }

    serverController.backup()
    IO.handleOutput('Creating of new WorkspaceAS7 finished')
  }

  def destroy() {
    super.destroy()
  }
}
