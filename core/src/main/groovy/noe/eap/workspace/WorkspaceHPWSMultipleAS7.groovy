package noe.eap.workspace

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.utils.Cleaner
import noe.common.utils.IO
import noe.common.utils.JBFile
import noe.common.utils.Library
import noe.eap.server.ServerEap
import noe.eap.utils.Eap6Utils
import noe.server.AS7
import noe.server.Httpd
import noe.workspace.WorkspaceAS7
import noe.workspace.WorkspaceAbstract

/**
 *
 * @author Michal Karm Babacek <mbabacek@redhat.com>
 *
 */
@Slf4j
class WorkspaceHPWSMultipleAS7 extends WorkspaceAbstract {
  protected static WorkspaceAS7 workspaceAS7

  int numberOfAdditionalServers = 3

  Boolean eap
  String eapVersion /// like 6.0.1.ER2 in jboss-eap-6.0.1.ER2.zip
  Boolean installNativesZip
  Boolean installWebConnectorsZip
  String bindAddressHttpd

  WorkspaceHPWSMultipleAS7(Boolean installNativesZip, Boolean installWebConnectorsZip) {
    //downloadClusterBench()
    workspaceAS7 = new WorkspaceAS7()
    this.eap = Boolean.valueOf(Library.getUniversalProperty('eap', 'true'))
    this.eapVersion = Library.getUniversalProperty('eap.version')
    this.installNativesZip = installNativesZip
    this.installWebConnectorsZip = installWebConnectorsZip
    initWorkspaceHPWSMultipleAS7()
  }

  def prepare() {
    IO.handleOutput('Creating of new Workspace started')
    workspaceAS7.prepare()
    IO.handleOutput "PRDEL: ${serverController.servers} and ${ServerEap.getPrefix()}"
    serverController.getServerById(ServerEap.getPrefix()).shiftPorts(DefaultProperties.DEFAULT_SHIFT_PORT_OFFSET)

    def as7Dir = ''
    AS7 nextServer = null
    def id = ''
    for (int i = 2; i < numberOfAdditionalServers + 2; i++) {
      id = "${ServerEap.getPrefix()}-${i}"
      IO.handleOutput("Creating new AS7 server instance: ${id}")

      // if node2 is not defined - create default
      if (!serverController.getAs7ServerIds().contains(id)) {
        as7Dir = id
        nextServer = AS7.getInstance(basedir, id, context)
        nextServer.createNewServerInstance(id, DefaultProperties.DEFAULT_SHIFT_PORT_OFFSET * i)
        serverController.addServer(id, nextServer)
      }
    }
    IO.handleOutput('Creating of new Workspace finished')

    /// Backup all servers state
    serverController.backup()
  }

  /**
   * Destroy the workspace.
   */
  def destroy() {
    IO.handleOutput('Destroying of default Workspace started')
    workspaceAS7.destroy()
    // TODO: Verify this
    if (this.eap && !this.skipInstall) {
      if (deleteWorkspace) {
        Cleaner.cleanDirectoryBasedOnRegex(new File(getBasedir()), /.*(jboss-eap).*/)
        IO.handleOutput('EAP workspace deleted: ' + basedir)
      }
      IO.handleOutput('EAP workspace NOT deleted: ' + basedir)
    }
    IO.handleOutput('Destroying of default Workspace finished')
  }

  /**
   * Initialize the workspace.
   */
  void initWorkspaceHPWSMultipleAS7() {
    IO.handleOutput('WorkspaceHPWSMultipleAS7.initWorkspaceHPWSMultipleAS7(): BEGIN')
    if (!skipInstall) {
      installHPWSMultipleAS7()
    }
    // TODO validate EAP installation
    IO.handleOutput('WorkspaceHPWSMultipleAS7.initWorkspaceHPWSMultipleAS7(): END')
  }

  /**
   * Install HPWSAS7
   * Static dir expected.
   */
  void installHPWSMultipleAS7() {
    IO.handleOutput('WorkspaceHPWSMultipleAS7.installHPWSMultipleAS7: EAP BEGIN')
    def eap = new Eap6Utils(basedir, ant, platform, eapVersion, "")
    eap.getIt()
    if (installNativesZip) eap.installNativesZip()

    def httpdDirOriginal = "hpws22"

    IO.handleOutput "TAGHPWW1: Creating HPWS instance ${httpdDirOriginal} in ${basedir.toString()}..."
    /**
     *  false, false with copyDirectoryContent prevents:
     * -rwxr-xr-x   1 root       hudson        7103 Nov 12  2012 /hudson_workspace/workspace/job/hpws22/apache/error/HTTP_REQUEST_URI_TOO_LARGE.html.var
     *               ^^^^^^
     * */
    JBFile.copyDirectoryContent(new File("/opt/${httpdDirOriginal}"), new File(basedir.toString() + "/" + httpdDirOriginal), false, false)

    // Installs modules to basedir.toString()+"/"+httpdDirOriginal
    if (installWebConnectorsZip) eap.installWebConnectorsZip()

    def httpdDir = httpdDirOriginal

    for (int i = 1; i < 5; i++) {
      // Don't worry, it's not that weird: We do not copy the forst instance, because it's been copied already.
      if (i > 1) {
        httpdDir = "${httpdDirOriginal}-${i}"
        JBFile.copyDirectoryContent(new File(basedir.toString() + "/" + httpdDirOriginal), new File(basedir.toString() + "/" + httpdDir), false, false)
      }
      JBFile.mkdir(new File(basedir + "/" + httpdDir + "/apache/conf.d"))
      File httpconf = new File(basedir + "/" + httpdDir + "/apache/conf/httpd.conf")
      JBFile.copy(Library.retrieveResourceAsFile("httpd/hpws/conf/httpd.conf"), new File(basedir + "/" + httpdDir + "/apache/conf"))
      JBFile.replace(httpconf, "@WORKSPACE@", basedir.toString())
      JBFile.replace(httpconf, "@HTTPD_DIR@", httpdDir.toString())
      JBFile.replace(httpconf, "@USER@", "hudson")
      JBFile.replace(httpconf, "@GROUP@", "hudson")
      [
          new File(basedir + "/" + httpdDir + "/apache/conf/extra/httpd-autoindex.conf"),
          new File(basedir + "/" + httpdDir + "/apache/conf/extra/httpd-dav.conf"),
          new File(basedir + "/" + httpdDir + "/apache/conf/extra/httpd-default.conf"),
          new File(basedir + "/" + httpdDir + "/apache/conf/extra/httpd-info.conf"),
          new File(basedir + "/" + httpdDir + "/apache/conf/extra/httpd-languages.conf"),
          new File(basedir + "/" + httpdDir + "/apache/conf/extra/httpd-manual.conf"),
          new File(basedir + "/" + httpdDir + "/apache/conf/extra/httpd-mpm.conf"),
          new File(basedir + "/" + httpdDir + "/apache/conf/extra/httpd-multilang-errordoc.conf"),
          new File(basedir + "/" + httpdDir + "/apache/conf/extra/httpd-ssl.conf"),
          new File(basedir + "/" + httpdDir + "/apache/conf/extra/httpd-userdir.conf"),
          new File(basedir + "/" + httpdDir + "/apache/conf/extra/httpd-vhosts.conf"),
          new File(basedir + "/" + httpdDir + "/apache/conf/mod_jk.conf"),
          new File(basedir + "/" + httpdDir + "/apache/conf/mod_jk2.conf"),
          new File(basedir + "/" + httpdDir + "/apache/bin/apachectl")
      ].each { file ->
        JBFile.replace(file, "/opt/", basedir.toString() + '/')
      }
      JBFile.makeAccessible(new File(basedir + "/" + httpdDir + "/apache/bin/apachectl"))
      JBFile.makeAccessible(new File(basedir + "/" + httpdDir + "/apache/bin/httpd"))

      def id = "${Httpd.defaultServerId}-${i}"
      this.bindAddressHttpd = Library.getUniversalProperty('bind.address.httpd', DefaultProperties.HOST)
      // Hardcoded until HPWS on HP-UX is updated. ETA: Not in this century.
      Httpd newHttpd = serverController.addServerHttpd(id, getBasedir(), '2.2', [host: bindAddressHttpd], httpdDir)
      newHttpd.shiftPorts(i)
    }
    /// Backup all server state
    serverController.backup()
    IO.handleOutput('WorkspaceHPWSMultipleAS7.installHPWSMultipleAS7: EAP END')
  }

  /**
   * Implementing of groovy fallback missingProeperty.
   *
   * @link http://groovy.codehaus.org/Using+methodMissing+and+propertyMissing
   */
  def propertyMissing(String name) {
    if (workspaceAS7.hasProperty(name)) {
      workspaceAS7.name
    } else throw new RuntimeException("WorkspaceHPWSMultipleAS7.propertyMissing():  WorkspaceAS7 has not the property $name")
  }
}
