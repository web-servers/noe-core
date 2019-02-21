package noe.workspace

import groovy.util.logging.Slf4j
import noe.common.utils.JBFile
import noe.ews.utils.ApacheDSUtils
import noe.server.ApacheDS

@Slf4j
class WorkspaceApacheDS extends WorkspaceAbstract {

  private static final String SERVER_ID = "ews-ldap-kerberos"
  private static final String VERSION = "2M7"

  WorkspaceApacheDS() {
    prepare()
  }

  @Override
  Object prepare() {
    basedir = ApacheDSUtils.installApacheDS(new File(basedir.toString()))
    serverController.addServer(SERVER_ID, ApacheDS.getInstance(basedir, VERSION))
  }

  @Override
  Object destroy() {
    serverController.removeServer(SERVER_ID)
    File apacheDsBaseDir = new File(basedir)
    if (apacheDsBaseDir.exists()) {
      log.debug("Deleting $apacheDsBaseDir")
      JBFile.delete(apacheDsBaseDir)
    }
  }
}
