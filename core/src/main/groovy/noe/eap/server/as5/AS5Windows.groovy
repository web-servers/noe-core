package noe.eap.server.as5

import groovy.util.logging.Slf4j
import noe.eap.server.ServerEap
import noe.server.AS5

/**
 * AS5Rhel implements RHEL specific
 * aspects of AS5 setup and execution.
 * This includes paths, commands (sh/bat) and more.
 *
 * @author Michal Karm Babacek <mbabacek@redhat.com>
 *
 */
@Slf4j
class AS5Windows extends AS5 {

  AS5Windows(String basedir, String as5Dir = "") {
    super(basedir)
    this.as5Dir = (as5Dir) ?: (ServerEap.getPrefix())
    this.basedir = basedir + "\\${this.as5Dir}\\jboss-as"
    this.refBasedir = basedir + "\\" + ServerEap.getPrefix() + "\\jboss-as"
    this.nativeDirLocationEnvName = 'CHECK_NATIVE_HOME'
    setDefault()
  }

  void setDefault() {
    super.setDefault()
    // TODO: Extend to support other profiles... (only default is supported atm)
    this.binDir = this.basedir + "\\bin"
    this.deploymentPath = this.basedir + "\\server\\${this.profile}\\deploy"
    this.binPath = "\\bin"
    this.start = [
        "run.bat",
        "-c",
        "${this.profile}",
        "-b",
        "${this.host}"
    ]
    this.stop = [
        "shutdown.bat",
        "-S"
    ]
    // TODO: Extend to support other profiles... (only default is supported atm)
    this.configDirs = [
        "\\server\\${this.profile}\\conf"
    ]
    this.deployConfigDirs = [
        this.deploymentPath + "\\jbossweb.sar"
    ]
    this.logDirs = [
        "\\server\\${this.profile}\\log"
    ]
    this.libDir = this.basedir + "\\server\\${this.profile}\\lib"
    this.workDir = "\\server\\${this.profile}\\work\\jboss.web"
    this.nativesDir = this.basedir + "\\${AS5Properties.NATIVES_DIR_NAME}\\bin"
    // ! please add new paths like absolute paths
  }
}
