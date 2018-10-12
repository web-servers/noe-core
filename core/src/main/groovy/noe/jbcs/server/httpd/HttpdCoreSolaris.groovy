package noe.jbcs.server.httpd

import groovy.transform.TypeChecked
import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.ews.server.httpd.HttpdSolaris

@TypeChecked
@Slf4j
class HttpdCoreSolaris extends HttpdSolaris {
  HttpdCoreSolaris(String basedir, version, String httpdDir) {
    super(basedir, version, httpdDir)
    this.httpdDir = httpdDir ?: DefaultProperties.HTTPD_CORE_DIR
    this.basedir = "${basedir}/${this.httpdDir}"
    this.refBasedir = "${basedir}/${DefaultProperties.HTTPD_CORE_DIR}"
    log.debug("Init: basedir:[${this.basedir}] httpdDir:[${this.httpdDir}]")
    setDefault()
  }
}
