package noe.eap.server.as7

import groovy.util.logging.Slf4j
import noe.server.AS7
/**
 * AS7Rhel implements RHEL specific
 * aspects of AS7 setup and execution.
 * This includes paths, commands (sh/bat) and more.
 *
 * TODO: Use Version class
 *
 * @author Michal Karm Babacek <mbabacek@redhat.com>
 *
 */
@Slf4j
class AS7Rhel extends AS7 {

  AS7Rhel(String basedir, String as7Dir = "") {
    super(basedir, as7Dir)
    setDefault()
  }

  void setDefault() {
    super.setDefault()
    setProfile(this.configFile)
    // ! please add new paths like absolute paths
  }

  void setProfile(String profileXML) {
    this.configFile = profileXML
    this.start = [
        "./standalone.sh",
        "-c",
        "${profileXML}",
        "-D${processCode}"
    ]
  }

  @Override
  double actualCPULoad() {
    return super.actualCPULoad(false)
  }
}
