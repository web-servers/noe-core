package noe.jbcs.server.httpd

import groovy.transform.TypeChecked
import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.ews.server.httpd.HttpdRhel

@TypeChecked
@Slf4j
class HttpdCoreRhel extends HttpdRhel {
  HttpdCoreRhel(String basedir, version, String httpdDir) {
    super(basedir, version, httpdDir)
    httpdDir = httpdDir ?: 'httpd'
    this.basedir = "${basedir}/${DefaultProperties.HTTPD_CORE_DIR}/${httpdDir}"
    this.refBasedir = "${basedir}/${DefaultProperties.HTTPD_CORE_DIR}/httpd"
    log.debug("Init: basedir:[${this.basedir}] httpdDir:[${this.httpdDir}]")
    setDefault()
  }
}
