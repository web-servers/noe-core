package noe.workspace

import noe.ews.utils.ApacheDSUtils
import noe.server.ApacheDS

class WorkspaceApacheDS extends WorkspaceAbstract {

  WorkspaceApacheDS() {
    basedir = ApacheDSUtils.installApacheDS(new File(basedir.toString()))
    serverController.addServer('ews-ldap-kerberos', ApacheDS.getInstance(basedir, '2M7'))
  }

}
