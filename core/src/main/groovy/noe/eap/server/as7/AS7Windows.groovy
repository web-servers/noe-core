package noe.eap.server.as7

import groovy.util.logging.Slf4j
import noe.server.AS7
/**
 * AS7Rhel implements RHEL specific
 * aspects of AS7 setup and execution.
 * This includes paths, commands (sh/bat) and more.
 *
 * @author Michal Karm Babacek <mbabacek@redhat.com>
 *
 */
@Slf4j
class AS7Windows extends AS7 {

  AS7Windows(String basedir, String as7Dir = "") {
    super(basedir, as7Dir)
    setDefault()
  }

  void setDefault() {
    super.setDefault()
    // TODO: Extend to support other profiles... (only standalone-ha is supported atm)

    //Hmm, see -Xss1024KB
    //    "set","JAVA_OPTS=","-Xms1303M","-Xmx1303M","-XX:MaxPermSize=256M","-Xss1024KB","-Djava.net.preferIPv4Stack=true","&",
    setProfile(this.configFile)

  }

  void setProfile(String profileXML) {
    this.configFile = profileXML
    this.start = [
        "standalone.bat",
        "-c",
        "${profileXML}"
    ]
  }
}
