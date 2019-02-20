package noe.workspace

import noe.common.utils.JBFile
import noe.ews.utils.ApacheDSUtils
import noe.server.ApacheDS

class WorkspaceApacheDS extends WorkspaceAbstract {

  private static final String SERVER_ID = "ews-ldap-kerberos"
//  private static final

  WorkspaceApacheDS() {
    basedir = ApacheDSUtils.installApacheDS(new File(basedir.toString()))
    serverController.addServer(SERVER_ID, ApacheDS.getInstance(basedir, '2M7'))
  }

  // Has to return an Object because abstract uses def, which returns an object
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