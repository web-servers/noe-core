package noe.ews.server.tomcat

import noe.common.DefaultProperties
import noe.common.utils.Library
import noe.common.utils.Version


/**
 *
 * @author Michal Karm Babacek <mbabacek@redhat.com>
 * @author Paul Lodge <plodge@redhat.com>
 *
 *   Determine proper TOMCAT_MAJOR_VERSION or set it to null.
 *   Do not throw any Exception as it broke execution of tests in mixtured JWS & EAP scenario.
 *
 */
class TomcatProperties {
  public static final String TOMCAT_MAJOR_VERSION
  public static final String PUBLIC_IP_ADDRESS

  static {

    Version ewsVersion = DefaultProperties.ewsVersion()
    Version tomcatVersion = DefaultProperties.tomcatVersion()

    if (tomcatVersion == null) {
      if (ewsVersion?.getMajorVersion() == 5) {
        TOMCAT_MAJOR_VERSION = "9"
      } else {
        TOMCAT_MAJOR_VERSION = null
      }
    } else {
      TOMCAT_MAJOR_VERSION = tomcatVersion.getMajorVersion().toString()
    }

    PUBLIC_IP_ADDRESS = Library.getUniversalProperty('tomcat.public.ip.address', DefaultProperties.HOST)
  }
}
