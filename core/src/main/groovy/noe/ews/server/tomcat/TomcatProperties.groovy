package noe.ews.server.tomcat

import noe.common.DefaultProperties
import noe.common.utils.Library


/**
 *
 * @author Michal Karm Babacek <mbabacek@redhat.com>
 *
 */
class TomcatProperties {
  public static final String TOMCAT_MAJOR_VERSION = Library.getUniversalProperty('tomcat.major.version', '7')
  public static final String PUBLIC_IP_ADDRESS = Library.getUniversalProperty('tomcat.public.ip.address', DefaultProperties.HOST)
}
