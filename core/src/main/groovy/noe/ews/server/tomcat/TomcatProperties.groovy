package noe.ews.server.tomcat

import noe.common.DefaultProperties
import noe.common.utils.Library


/**
 *
 * @author Michal Karm Babacek <mbabacek@redhat.com>
 * @author Paul Lodge <plodge@redhat.com>
 *
 */
class TomcatProperties {
  public static final String TOMCAT_MAJOR_VERSION
  public static final String PUBLIC_IP_ADDRESS

  static {
    TOMCAT_MAJOR_VERSION = Library.getUniversalProperty('tomcat.major.version')
    if (!TOMCAT_MAJOR_VERSION?.trim()) {
      new IllegalArgumentException("TOMCAT_MAJOR_VERSION is either null or empty")
    }

    PUBLIC_IP_ADDRESS = Library.getUniversalProperty('tomcat.public.ip.address', DefaultProperties.HOST)
  }
}
