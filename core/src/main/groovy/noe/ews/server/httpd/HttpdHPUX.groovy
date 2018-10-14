package noe.ews.server.httpd

import groovy.transform.TypeChecked
import groovy.util.logging.Slf4j
import noe.common.utils.JBFile
import noe.server.Httpd

/**
 *
 * @author Michal Karm Babacek <mbabacek@redhat.com>
 *
 */
@TypeChecked
@Slf4j
class HttpdHPUX extends Httpd {
  boolean needsSudo = JBFile.useAdminPrivileges

  HttpdHPUX(String basedir, version, String httpdDir) {
    super(basedir, version)
    this.httpdDir = (httpdDir) ? httpdDir : "hpws22"
    this.basedir = basedir + '/' + this.httpdDir + '/apache'
    // please initialize in setDefault() method
    setDefault()
  }

  void setDefault() {
    super.setDefault()
    this.httpdServerRoot = ''
    this.configDirs = [
        httpdServerRoot + '/conf',
        httpdServerRoot + '/conf.d'
    ]
    this.logDirs = [httpdServerRoot + '/logs']
    this.binPath = '/bin'
    configureApachectl()
    this.deploymentPath = this.basedir + "/htdocs"
    this.confDeploymentPath = this.basedir + httpdServerRoot + '/conf.d'
    this.cgiDeploymentPath = this.basedir + "/cgi-bin"
    this.modClusterCacheDir = this.basedir + "/cache/mod_cluster"
    this.opensslPath = null
    this.abPath = this.basedir + "/bin/ab"
    this.htdbmPath = this.basedir + "/bin/htdbm"
    this.htpasswdPath = this.basedir + "/bin/htpasswd"
    this.httxt2dbmPath = null
    this.logresolvePath = this.basedir + "/bin/logresolve"
    // ! please add new paths like absolute paths
  }

  void setNeedsSudo(boolean needsSudo) {
    this.needsSudo = needsSudo
    configureApachectl()
  }

  // Intentionally not using setApachectl()
  void configureApachectl() {
    this.apachectl = [getBinDirFullPath() + '/apachectl']
    this.start = apachectl + ['-f', basedir + '/conf/httpd.conf', '-E', basedir + '/logs/httpd.log', '-k', 'start']
    this.stop = apachectl + ["-f", basedir + '/conf/httpd.conf', '-E', basedir + '/logs/httpd.log', '-k', 'stop']
  }
}
