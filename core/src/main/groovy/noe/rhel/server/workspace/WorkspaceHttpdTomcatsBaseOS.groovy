package noe.rhel.server.workspace

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
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
  private List<File> modJkModules = []

  Boolean ews
  Boolean rpm
  Version ewsVersion
  String hostIpAddress = DefaultProperties.HOST
  Map<String,String> originalTomcatHosts = [:]

  WorkspaceHttpdTomcatsBaseOS(int numberOfAdditionalTomcats = 0) {
    super()
    this.numberOfAdditionalTomcats = numberOfAdditionalTomcats
    //downloadClusterBench()
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
    workspaceTomcat.prepare(true) //always true due to running with JWS 5 `ewsVersion >= new Version('3.1.0-DR0')`

    createAdditionalTomcatsWithRegistrations(numberOfAdditionalTomcats)
    originalTomcatHosts.putAll(originalTomcatHostsIpAddresses())
  }

  private void copyModulesIfMissing(Httpd httpd) {
    File modulePath = new File("${httpd.getServerRoot()}/modules/mod_jk.so")
    File sclModulePath = new File("${DefaultProperties.HTTPD_SCL_ROOT}/usr/lib64/httpd/modules/mod_jk.so")
    if (!modulePath.exists() && sclModulePath.exists()) {
      log.info("Copiing file ${sclModulePath} to modules folder of httpd ${modulePath}.")
      modJkModules.add(modulePath)
      JBFile.copyFile(sclModulePath, modulePath)
    }
    if (!modulePath.exists()) {
      throw new FileNotFoundException("Mod_jk module file is missing. ${modulePath}")
    }
  }

  def destroy() {
    log.debug("Destroying workspace Httpd with Tomcats BaseOS.")
    serverController.getTomcatServerIds([TomcatProperties.TOMCAT_MAJOR_VERSION]).each { tomcatId ->
      serverController.getServerById(tomcatId).setHost(originalTomcatHosts.get(tomcatId))
    }

    modJkModules.each() {File module ->
      JBFile.delete(module)
    }

    super.destroy()

    workspaceHttpd.destroy()
    workspaceTomcat.destroy()
  }
}
