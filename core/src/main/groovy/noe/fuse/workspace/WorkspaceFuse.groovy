package noe.fuse.workspace

import groovy.util.logging.Slf4j
import noe.common.utils.IO
import noe.common.utils.JBFile
import noe.common.utils.Library
import noe.fuse.utils.FuseUtils
import noe.server.Fuse
import noe.workspace.WorkspaceAbstract

/**
 *
 * @author tplevko
 */
@Slf4j
class WorkspaceFuse extends WorkspaceAbstract {

  String fuseVersion
  String fuseType

  WorkspaceFuse(String serverId = null) {

    log.info('WorkspaceFuse(): BEGIN')
    def basedir = getBasedir()
    def server = Fuse.getInstance(basedir, '', context)
    this.fuseVersion = Library.getUniversalProperty('fuse.version')
    this.fuseType=Library.getUniversalProperty('fuse.type')

    log.info('HELL: servers map:' + serverController.servers)
    serverController.addServer(serverId, server)
    log.info('HELL: servers map:' + serverController.servers)
    log.info('WorkspaceFuse(): END')
    initWorkspaceFuse()
  }

  def prepare() {
    log.info('Creating of new WorkspaceFuse started')

    serverController.backup()
    log.info('Creating of new WorkspaceFuse finished')
  }

  /**
   * Install fuse
   * Static dir expected.
   */
  void installFuse() {
    log.info("${this.class.getName()}.installFuse(): Fuse BEGIN")

    def fuse = new FuseUtils(basedir, ant, platform, fuseVersion, fuseType)
    fuse.getIt()
    log.info("${this.class.getName()}.installFuse(): Fuse END")
  }

  /**
   * Initialize the workspace.
   */
  def initWorkspaceFuse() {
    log.info("${this.class.getName()}.initWorkspaceFuse(): BEGIN")
    if (!skipInstall) {
      installFuse()
    }
    log.info("${this.class.getName()}.initWorkspaceFuse(): END")
  }
}
