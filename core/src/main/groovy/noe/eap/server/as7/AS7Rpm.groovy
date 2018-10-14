package noe.eap.server.as7

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.utils.JBFile
import noe.common.utils.Version
import noe.server.ServerAbstract
/**
 * AS7Rpm implements Rpm specific
 * aspects of AS7 setup and execution.
 * This includes paths, commands and more.
 * 
 * Author: Filip Goldefus <fgoldefu@redhat.com>
 *
 */
@Slf4j
class AS7Rpm extends AS7Rhel {
  String nodeBasedir
  String nodeDatadir
  String nodeTmpdir

  AS7Rpm(String basedir, String as7Dir = "") {
    super(basedir, as7Dir)
    this.basedir = (DefaultProperties.EAP_VERSION < new Version("7.0.0.DR1")) ? DefaultProperties.EAP6_RPM_ROOT : DefaultProperties.EAP7_RPM_ROOT
    this.nodeBasedir = "${getServerRoot()}/standalone" + (as7Dir ? "-${as7Dir}" : "")
    this.nodeDatadir = "${nodeBasedir}/data"
    this.nodeTmpdir = "${nodeBasedir}/tmp"
    setDefault()
    if (as7Dir) {
      configDirs = ["/standalone-${as7Dir}/configuration"]
      libDir = "${nodeBasedir}/lib"
    }
  }

  void setDefault() {
    super.setDefault()
    deploymentPath = "${nodeBasedir}/deployments"
  }

  public void setProfile(String profileXML) {
    this.configFile = profileXML
    // Start command for AS7 server. All servers
    // use same xml config file - port offset property
    // has to be set distinct for each server due
    // the usage of shared xml config file. Property
    // jboss.socket.binding.port-offset is used in
    // config as a base of AS7 ports.
    this.start = [
      "${getBinDirFullPath()}/standalone.sh",
      '-c',
      "${profileXML}",
      "-D${processCode}",
      "-Djboss.socket.binding.port-offset=${portOffset}",
      "-Djboss.server.base.dir=${nodeBasedir}"
    ]
  }

  void updateAS7ConfReplaceRegExp(from, to) {
    super.updateConfReplaceRegExp(configFile, from, to, true)
  }

  ServerAbstract createNewServerInstance(String id, int offset = DefaultProperties.DEFAULT_SHIFT_PORT_OFFSET) {
    copyRuntimeDir(this, id)
    setProfile(configFile)
    shiftPorts(offset)
    return this
  }

  String copyRuntimeDir(AS7Rpm server, String id) {
    JBFile.delete(new File(server.nodeBasedir))
    JBFile.copyDirectoryContent(new File("${getServerRoot()}/standalone"), new File("${server.nodeBasedir}"), true)
  }
}
