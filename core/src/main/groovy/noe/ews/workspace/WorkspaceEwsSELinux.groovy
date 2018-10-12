package noe.ews.workspace

import groovy.util.logging.Slf4j
import noe.ews.utils.selinux.SELinuxSystem

/**
 * Author: jmartisk
 * Date: 8/2/12
 * Time: 10:05 AM
 */
@Slf4j
class WorkspaceEwsSELinux extends WorkspaceEws {

  @Override
  def prepare() {
    super.prepare()
    System.properties.setProperty("selinuxtest.filecontexts.configfile", "src/main/resources/selinux/selinux_conf_contexts.txt")
    System.properties.setProperty("selinuxtest.filecontexts.outputfile", "target/selinux-reports/selinux_file_contexts.txt")
    System.properties.setProperty("selinuxtest.portpermissions.configfile", "src/main/resources/selinux/selinux_conf_ports.txt")
    System.properties.setProperty("selinuxtest.portpermissions.outputfile", "target/selinux-reports/selinux_port_permissions.txt")
    System.properties.setProperty("selinuxtest.runtimetest.avcdenials.outputfile", "target/selinux-reports/audit.log")
    System.properties.setProperty("jon.agent.home", "jon-workspace/rhq-agent")

    SELinuxSystem.checkEnvironmentSanity()
    if (platform.isRHEL5()) {
      SELinuxSystem.setSELinuxEnforcingMode(false)
    } else {
      SELinuxSystem.setDomainPermissive("unconfined_java_t")
      SELinuxSystem.setSELinuxEnforcingMode(true)
    }

    SELinuxSystem.runAuditdIfNotRunning()

    /// Backup all servers state
    serverController.backup()
  }
}
