package noe.eap.server.as7

import groovy.util.logging.Slf4j

/**
 * AS7HPUX implements HP-UX specific
 * aspects of AS7 setup and execution.
 * This includes paths, commands (sh/bat) and more.
 *
 * @author Michal Karm Babacek <mbabacek@redhat.com>
 *
 */
@Slf4j
class AS7HPUX extends AS7Rhel {

  AS7HPUX(String basedir, String as7Dir = "") {
    super(basedir, as7Dir)
    setDefault()
  }

  @Override
  void setProfile(String profileXML) {
    this.configFile = profileXML
    this.start = [
        "${getBinDirFullPath()}/standalone.sh",
        "-c",
        "${profileXML}",
        "-D${processCode}"
    ]
  }

  @Override
  void setDefault() {
    super.setDefault()
    this.stop = as7Cli.generateCmdForSingleCliCommand(":shutdown")
  }

}
