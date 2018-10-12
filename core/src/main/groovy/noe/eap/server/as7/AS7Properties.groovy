package noe.eap.server.as7

import noe.common.DefaultProperties
import noe.common.utils.Library


/**
 *
 * @author Michal Karm Babacek <mbabacek@redhat.com>
 *
 */
class AS7Properties {
  public static final String JAVA_OPTS_HEAP_SETTING = '-XX:MaxHeapSize=256m'
  public static final int MAIN_HTTP_PORT_DEFAULT = 8080
  public static final int MAIN_HTTP_PORT = Library.getUniversalProperty('as7.main.http.port', MAIN_HTTP_PORT_DEFAULT).toInteger()
  public static final int MAIN_HTTPS_PORT_DEFAULT = 8443
  public static final int MAIN_HTTPS_PORT = Library.getUniversalProperty('as7.main.https.port', MAIN_HTTPS_PORT_DEFAULT).toInteger()
  public static final int AJP_PORT_DEFAULT = 8009
  public static final int AJP_PORT = Library.getUniversalProperty('as7.main.ajp.port', AJP_PORT_DEFAULT).toInteger()
  public static final int MANAGEMENT_HTTP_PORT_DEFAULT = 9990
  public static final int MANAGEMENT_HTTP_PORT = Library.getUniversalProperty('as7.main.management.http.port', MANAGEMENT_HTTP_PORT_DEFAULT).toInteger()
  public static final int MANAGEMENT_HTTPS_PORT_DEFAULT = 9443
  public static final int MANAGEMENT_HTTPS_PORT = Library.getUniversalProperty('as7.main.management.https.port', MANAGEMENT_HTTPS_PORT_DEFAULT).toInteger()
  public static final int MANAGEMENT_NATIVE_PORT_DEFAULT = 9999
  public static final int MANAGEMENT_NATIVE_PORT = Library.getUniversalProperty('as7.main.management.native.port', MANAGEMENT_NATIVE_PORT_DEFAULT).toInteger()
  public static final List<String> DEPLOYMENT_SCANNER_MARKER_FILES = [
      ".dodeploy",
      ".skipdeploy",
      ".isdeploying",
      ".deployed",
      ".failed",
      ".isundeploying",
      ".undeployed",
      ".pending"
  ]
  public static final String PUBLIC_IP_ADDRESS = Library.getUniversalProperty('as7.public.ip.address', DefaultProperties.HOST)
  public static final String MANAGEMENT_IP_ADDRESS = Library.getUniversalProperty('as7.management.ip.address', PUBLIC_IP_ADDRESS)
  public static final String PRIVATE_IP_ADDRESS = Library.getUniversalProperty('as7.private.ip.address', PUBLIC_IP_ADDRESS)
  public static final String UDP_MCAST_ADDRESS = Library.getUniversalProperty('as7.udp.mcast.address', '230.0.0.4')
  public static final String DIAGNOSTICS_MCAST_ADDRESS = Library.getUniversalProperty('as7.diagnostics.mcast.address', '224.0.75.75')
  public static final String MPING_MCAST_ADDRESS = Library.getUniversalProperty('as7.mping.mcast.address', '230.0.0.4')
  public static final String STANDALONE_CONFIG_FILE = Library.getUniversalProperty('as7.standalone.config.file', 'standalone-ha.xml')
  public static final String DOMAIN_CONFIG_FILE = Library.getUniversalProperty('as7.domain.config.file', 'host.xml')

  public static final String STANDALONE_MANAGEMENT_IP_ADDRESS = Library.getUniversalProperty('as7.standalone.management.ip.address', PUBLIC_IP_ADDRESS)
  public static final String STANDALONE_PUBLIC_IP_ADDRESS = Library.getUniversalProperty('as7.standalone.public.ip.address', MANAGEMENT_IP_ADDRESS)

  public static final String DOMAIN_MANAGEMENT_IP_ADDRESS = Library.getUniversalProperty('as7.domain.management.ip.address', MANAGEMENT_IP_ADDRESS)
  public static final String DOMAIN_PUBLIC_IP_ADDRESS = Library.getUniversalProperty('as7.domain.public.ip.address', PUBLIC_IP_ADDRESS)
}
