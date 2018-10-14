package noe.workspace

import groovy.util.logging.Slf4j
import noe.common.utils.Library
import noe.jon.utils.JonUtils
import noe.server.Sahi

@Slf4j
class WorkspaceSahi extends WorkspaceAbstract {
  JonUtils jonUtils

  WorkspaceSahi(String serverId = 'sahi') {

    this.jonUtils = new JonUtils(basedir, WorkspaceAbstract.ant, WorkspaceAbstract.platform)

    // installing sahi
    this.jonUtils.installSahi(Library.getUniversalProperty('sahi.zip.name', 'sahi_20130429.zip'))
    serverController.addServer(serverId, new Sahi(this.jonUtils.getInstallSahiDirectory()))
  }

}
