package noe.fuse.server

import noe.common.DefaultProperties
import noe.common.utils.Library

/**
 *
 * @author tplevko
 */
public class FuseProperties {

  public static final String PROFILE = Library.getUniversalProperty('fuse.profile', 'karaf')

  public static final int MAIN_HTTP_PORT_DEFAULT = 8181
  public static final int MAIN_HTTP_PORT = Library.getUniversalProperty('fuse.main.http.port', MAIN_HTTP_PORT_DEFAULT).toInteger()
  public static final int JNDI_PORT_DEFAULT = 1099
  public static final int JNDI_PORT = Library.getUniversalProperty('fuse.main.jndi.port', JNDI_PORT_DEFAULT).toInteger()

  public static final String PUBLIC_IP_ADDRESS = Library.getUniversalProperty('fuse.public.ip.address', DefaultProperties.HOST)
  public static final String USER_CONFIG_FILE = Library.getUniversalProperty('fuse.user.config.file', 'users.properties')

  public static final String ADMIN_NAME = Library.getUniversalProperty('fuse.admin.name', 'admin')
  public static final String ADMIN_PASSWORD = Library.getUniversalProperty('fuse.admin.password', 'admin')
  public static final String ADMIN_ROLES = Library.getUniversalProperty('fuse.admin.roles', 'admin,manager,viewer,Operator, Maintainer, Deployer, Auditor, Administrator, SuperUser')
}
