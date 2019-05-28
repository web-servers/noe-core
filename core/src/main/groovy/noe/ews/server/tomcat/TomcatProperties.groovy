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
  public static final String EWS_VERSION
  public static final String TOMCAT_MAJOR_VERSION
  public static final String PUBLIC_IP_ADDRESS

  static {

    EWS_VERSION = Library.getUniversalProperty('ews.version')
    TOMCAT_MAJOR_VERSION = Library.getUniversalProperty('tomcat.major.version')

    if (!EWS_VERSION?.trim() && !TOMCAT_MAJOR_VERSION?.trim()) {
      throw new IllegalArgumentException("Both EWS_VERSION and TOMCAT_MAJOR_VERSION are either null or empty")
    }

    if (EWS_VERSION?.trim() && !TOMCAT_MAJOR_VERSION?.trim()) {
      throw new IllegalArgumentException("EWS_VERSION is set but the TOMCAT_MAJOR_VERSION is either null or empty")
    }

    PUBLIC_IP_ADDRESS = Library.getUniversalProperty('tomcat.public.ip.address', DefaultProperties.HOST)
  }
}
