package noe.workspace

import groovy.util.logging.Slf4j
import noe.common.utils.Library
import noe.rhel.server.httpd.HttpdBaseOS

@Slf4j
class WorkspaceHttpdBaseOS extends WorkspaceAbstract {

  WorkspaceHttpdBaseOS() {

    if (platform.isRHEL9() || platform.isRHEL8() || platform.isRHEL7()) {
      HttpdBaseOS server = new HttpdBaseOS(basedir, '2.4')
      serverController.addServer("httpd-24-1", server)
    } else if (platform.isRHEL6()) {
      HttpdBaseOS server = new HttpdBaseOS(basedir, '2.2')
      serverController.addServer('httpd-22-1', server)
    }
  }

  @Override
  def prepare() {
    log.debug("Creating Httpd BaseOS workspace.")
    (serverController.httpdServerIds).each { String httpdId ->
      /**
       * Settings of mpm module, set MPM_MODULE variable to prefork(default), worker or event
       */
      def mpmModule = Library.getUniversalProperty('mpm.module')
      if (mpmModule) {
        def file = "00-mpm.conf"
        log.info("Applying setting for specific mpm module to ${mpmModule}")
        def matchReplace = ['LoadModule mpm_prefork_module modules/mod_mpm_prefork.so',
                            'LoadModule mpm_worker_module modules/mod_mpm_worker.so',
                            'LoadModule mpm_event_module modules/mod_mpm_event.so']
        matchReplace.each {
          if (it.contains("mpm_" + mpmModule + "_module")) {
            log.trace("${it}, uncommenting")
            serverController.getServerById(httpdId).updateConfReplaceRegExp(file, /#+/ + it, it)
          } else {
            serverController.getServerById(httpdId).updateConfReplaceRegExp(file, it, "#" + it)
          }
        }
      }
    }
    serverController.backup()
  }
}
