package noe.rhel.server.workspace

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.utils.FileStateVault
import noe.common.utils.JBFile
import noe.common.utils.Version
import noe.ews.server.tomcat.TomcatProperties
import noe.rhel.server.httpd.HttpdBaseOS
import noe.server.Httpd
import noe.workspace.WorkspaceHttpdBaseOS
import noe.workspace.WorkspaceMultipleTomcats
import noe.workspace.WorkspaceTomcat

@Slf4j
class WorkspaceHttpdTomcatsBaseOS extends WorkspaceMultipleTomcats{

  int numberOfAdditionalTomcats

  protected static WorkspaceHttpdBaseOS workspaceHttpd
  protected static WorkspaceTomcat workspaceTomcat
  private List<File> moduleFiles = []
  private FileStateVault fileStateVault = new FileStateVault()

  Boolean ews
  Boolean rpm
  Version ewsVersion
  String hostIpAddress = DefaultProperties.HOST
  Map<String,String> originalTomcatHosts = [:]

  WorkspaceHttpdTomcatsBaseOS(int numberOfAdditionalTomcats = 0) {
    super()
    this.numberOfAdditionalTomcats = numberOfAdditionalTomcats
    workspaceHttpd = new WorkspaceHttpdBaseOS()
    workspaceTomcat = new WorkspaceTomcat()

    this.ewsVersion = DefaultProperties.ewsVersion()
  }

  def prepare() {
    log.debug("Creating workspace Httpd with ${numberOfAdditionalTomcats} Tomcats BaseOS.")
    super.prepare()
    workspaceHttpd.prepare()

    HttpdBaseOS httpd = (HttpdBaseOS) serverController.getServerById(serverController.getHttpdServerId())
    httpd.setHost(hostIpAddress)
    httpd.updateConfSetBindAddress(hostIpAddress)

    copyModulesIfMissing(httpd)
    copyConfIfMissing(httpd)
    workspaceTomcat.prepare(true) //always true due to running with JWS 5 `ewsVersion >= new Version('3.1.0-DR0')`

    createAdditionalTomcatsWithRegistrations(numberOfAdditionalTomcats)
    originalTomcatHosts.putAll(originalTomcatHostsIpAddresses())
  }

  /**
   * JBCS modules are provided in different folder and need to be properly loaded or copied for smooth working.
   * @link https://issues.jboss.org/browse/JBCS-730
   * @link https://issues.jboss.org/browse/JBCS-734
   * @param httpd Instance of `Httpd` server
   */
  private void copyModulesIfMissing(Httpd httpd) {
    List<String> moduleNames = ["mod_jk", "mod_advertise", "mod_cluster_slotmem", "mod_manager", "mod_proxy_cluster"]
    moduleNames.each() { String moduleName ->
      File modulePath = new File("${httpd.getServerRoot()}/modules/${moduleName}.so")
      File sclModulePath = new File("${DefaultProperties.HTTPD_SCL_ROOT}/usr/lib64/httpd/modules/${moduleName}.so")
      if (!modulePath.exists() && sclModulePath.exists()) {
        log.debug("Copying file ${sclModulePath} to modules folder of httpd ${modulePath}.")
        moduleFiles.add(modulePath)
        JBFile.copyFile(sclModulePath, modulePath)
      }
      if (!modulePath.exists()) {
        throw new FileNotFoundException("Module file is missing. ${modulePath}")
      }
    }
  }

  /**
   * JBCS modules are provided in different folder then BaseOS httpd expects them and have to be copied for sake of
   * mod_cluster tests
   * @link https://issues.jboss.org/browse/JBCS-730
   * @link https://issues.jboss.org/browse/JBCS-734
   * @param httpd Instance of `Httpd` server
   */
  private void copyConfIfMissing(Httpd httpd) {
    List<String> confRelativePaths = ["conf.d/mod_cluster.conf"]
    confRelativePaths.each() { String confRelativePath->
      File confPath = new File(httpd.getServerRoot(), confRelativePath)
      File sclConfPath = new File("${DefaultProperties.HTTPD_SCL_ROOT}/etc/httpd/${confRelativePath}")
      if (!confPath.exists() && sclConfPath.exists()) {
        log.debug("Copying file ${sclConfPath} to config.d folder of httpd ${confPath}.")
        fileStateVault.push(confPath)
        JBFile.copyFile(sclConfPath, confPath)
      }
      if (!confPath.exists()) {
        throw new FileNotFoundException("Module file is missing. ${confPath}")
      }
    }
  }

  def destroy() {
    log.debug("Destroying workspace Httpd with Tomcats BaseOS.")
    serverController.getTomcatServerIds([TomcatProperties.TOMCAT_MAJOR_VERSION]).each { tomcatId ->
      serverController.getServerById(tomcatId).setHost(originalTomcatHosts.get(tomcatId))
    }

    moduleFiles.each() { File module ->
      JBFile.delete(module)
    }

    fileStateVault.popAll()

    super.destroy()

    workspaceHttpd.destroy()
    workspaceTomcat.destroy()
  }
}
