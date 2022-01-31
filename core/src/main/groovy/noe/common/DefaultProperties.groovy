package noe.common

import noe.common.utils.Library
import noe.common.utils.OpenSslVersion
import noe.common.utils.Platform
import noe.common.utils.Version
/**
 *
 * @author Michal Karm Babacek <mbabacek@redhat.com>
 * @author Jan Stefl <jstefl@redhat.com>
 * @author Radim Hatlapatka <rhatlapa@redhat.com>
 *
 */
class DefaultProperties {
  // Default host to bind to.
  public static final String HOST = Library.getUniversalProperty('host', '127.0.0.1')
  // Default IPv6 host to bind to
  public static final String HOST_IPV6 = Library.getUniversalProperty('host_ipv6', '::1')
  // ServerName directive for Apache HTTP Server might not be equal to HOST
  public static final String HTTPD_SERVER_NAME = Library.getUniversalProperty('httpd.server.name', HOST)
  // TODO: Add a comment here, what "TOMCAT_NODE_FIRST_IDX" does?
  public static final int TOMCAT_NODE_FIRST_IDX = 1 // TODO LP: remove
  // TODO: Add a comment here, what "TOMCAT_NODE_LAST_IDX" does?
  public static final int TOMCAT_NODE_LAST_IDX = 3 // TODO LP: remove
  // TODO: Add a comment here. Is is like ews/eap6/ews-rpm? Why "def"?
  public static final String DEFAULT_CONTEXT_NAME = 'ews'
  // TODO: Add a comment here. Is it a number to be added to all ports in order to perform a port shift?
  public static final int DEFAULT_SHIFT_PORT_OFFSET = 101
  // TODO: Add a comment here. Is it a HTTP session id header string representation?
  public static final String JSESSIONID = "JSESSIONID"
  // Default timeout for wait for Start/Stop of the server in seconds
  public static final Integer START_STOP_TIMEOUT = Integer.valueOf(Library.getUniversalProperty('start.stop.timeout', '60'))
  // Property whether run postinstall after ews installation
  public static final Boolean EWS_SKIP_POSTINSTALL = Boolean.valueOf(Library.getUniversalProperty('ews.postinstall.skip', 'false'))
  // Well, just a line separator :-)
  public static final String NL = System.getProperty('line.separator')
  // .postinstall with sudo?
  public static final Boolean RUN_WITH_SUDO = Library.getUniversalProperty('run.with.sudo', 'false').toBoolean()
  public static final Boolean USE_HTTPD_RPM = Library.getUniversalProperty('use.httpd.rpm', 'false').toBoolean()
  public static final String HTTPD_CORE_DIR = Library.getUniversalProperty('httpd.core.dir', 'jbcs-httpd24-2.4')
  public static final String HTTPD_SCL_ROOT = Library.getUniversalProperty('httpd.scl.root', '/opt/rh/jbcs-httpd24/root')
  public static final String EAP7_RPM_ROOT = Library.getUniversalProperty('eap7.rmp.root', '/opt/rh/eap7/root/usr/share/wildfly')
  public static final String EAP6_RPM_ROOT = Library.getUniversalProperty('eap6.rpm.root', '/usr/share/jbossas/')
  public static final String SERVER_JAVA_HOME = Library.getUniversalProperty("server.java.home")
  public static final String JAVA_HOME = Library.getUniversalProperty("java.home")
  // Default AJP Secret for Tomcat, modjk and Mod_Cluster tests.
  public static final String DEFAULT_AJP_SECRET = Library.getUniversalProperty("default.ajp.secret", "SHH_ITS_A_SECRET")

  // Default workspace basedir
  public static final String WORKSPACE_BASEDIR = Library.getUniversalProperty('workspace.basedir', new File('.').getCanonicalPath() + "${File.separator}workspace")
  // Clean workspace_basedir
  public static final Boolean CLEAN_WORKSPACE_AT_START = Boolean.valueOf(Library.getUniversalProperty('clean.workspace.at.start', 'false'))
  // Prefix for the path containing Jenkins jobs build artifacts
  public static final String JENKINS_JOBS_DIR_PREFIX = Library.getUniversalProperty('jenkins.jobs.dir.prefix', '/qa/services/hudson/JOBS')
  // Clusterbench test app
  public static final String CLUSTERBENCH_EE5_DIST = Library.getUniversalProperty('clusterbench.ee5.dist',
      "${JENKINS_JOBS_DIR_PREFIX}/clusterbench-mod_cluster-mbabacek/lastSuccessful/archive/clusterbench-ee5-web/target/clusterbench.war")
  public static final String CLUSTERBENCH_EE6_DIST = Library.getUniversalProperty('clusterbench.ee6.dist',
      "${JENKINS_JOBS_DIR_PREFIX}/clusterbench-mod_cluster-mbabacek/lastSuccessful/archive/clusterbench-ee6-web/target/clusterbench.war")
  public static final String CLUSTERBENCH_EE6_DIST_NONDIST = Library.getUniversalProperty('clusterbench.ee6.dist.nondist',
      "${JENKINS_JOBS_DIR_PREFIX}/clusterbench-mod_cluster-mbabacek/lastSuccessful/archive/clusterbench-ee6-web-nondist/target/clusterbench.war")
  public static final boolean SOLARIS_DEFAULT_LIBRARY_PATH_CLEAN = Boolean.valueOf(Library.getUniversalProperty('solaris.default.library.path.clean', 'false'))

  // Get the fips self signed directory depending on whether fips on the OS is enabled or not
  public static final String SELF_SIGNED_CERTIFICATE_RESOURCE = Library.getUniversalProperty('self_signed.certificate.resource',
      new Platform().isFips() ? "self_signed_fips" : "self_signed")
  public static final String FIPS_140_2_CIPHERS = "SSL_RSA_WITH_3DES_EDE_CBC_SHA,SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA,TLS_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_DSS_WITH_AES_128_CBC_SHA,TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_256_CBC_SHA,TLS_DHE_DSS_WITH_AES_256_CBC_SHA,TLS_DHE_RSA_WITH_AES_256_CBC_SHA,TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA,TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA,TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA,TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA,TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA,TLS_ECDH_RSA_WITH_AES_128_CBC_SHA,TLS_ECDH_RSA_WITH_AES_256_CBC_SHA,TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA,TLS_ECDH_anon_WITH_AES_128_CBC_SHA,TLS_ECDH_anon_WITH_AES_256_CBC_SHA"

  /**
   * Method which tries to retrieve version from property, if it doesn't succeed, null is returned
   */
  private static final Version versionOrNull(String versionProperty) {
    String versionAsString = Library.getUniversalProperty(versionProperty)
    if (versionAsString) {
      return new Version(versionAsString)
    } else {
      return null
    }
  }

  /**
   * Method which tries to retrieve version from property, if it doesn't succeed, null is returned
   */
  private static final OpenSslVersion opensslVersionOrNull(String versionProperty) {
    String versionAsString = Library.getUniversalProperty(versionProperty)
    if (versionAsString) {
      return new OpenSslVersion(versionAsString)
    } else {
      return null
    }
  }

  static final Version eapCpVersion() {
    return versionOrNull('eap.cp.version')
  }

  static final Version eapVersion() {
    return versionOrNull('eap.version')
  }
  static final Version ewsVersion() {
   return versionOrNull('ews.version')
  }
  static final Version tomcatVersion() {
    return versionOrNull('tomcat.major.version')
  }
  static final Version apacheCoreVersion() {
    return versionOrNull('apache.core.version')
  }
  static final boolean isRemoveZipAfterUnzip(boolean defaultValue = true) {
    return Boolean.parseBoolean(Library.getUniversalProperty('zip.remove.after.unzip', String.valueOf(defaultValue)))
  }
  static final OpenSslVersion jbcsOpenSslVersion() {
    return opensslVersionOrNull('jbcs.openssl.version')
  }
  // Used when noe-tests run httpd as root
  static final String runHttpdAsUser() {
    return Library.getUniversalProperty('run.httpd.as.user', null)
  }
  // requires the property to be set before starting the NOE, this is not true for tests or for cases where properties would be loaded from property file.
  @Deprecated static final Version EAP_VERSION = eapVersion()
  @Deprecated static final Version EWS_VERSION = ewsVersion()

  public static final String APP_PATH = Library.getUniversalProperty('apppath', "webapps${File.separator}clusterbench.war")

  // mod_cluster custom load metric for AS7 (perhaps WildFly as well...)
  public static final String MODCLUSTER_CUSTOM_LOAD_METRIC = Library.getUniversalProperty('modcluster.custom.load.metric',
      "${JENKINS_JOBS_DIR_PREFIX}/mod_cluster-custom-load-metric/lastSuccessful/archive/target/mod_cluster-custom-metric.jar")
  // mod_cluster: Cipher suite for m_c
  public static final String MOD_CLUSTER_SSL_CIPER_SUITE = Library.getUniversalProperty('mod.cluster.ssl.ciper.suite', 'AES128-SHA:ALL:!ADH:!LOW:!MD5:!SSLV2:!NULL')

  // mod_cluster shared adverise group (UDP address and port)
  public static final String MODCLUSTER_MCAST_ADDRESS = Library.getUniversalProperty('modcluster.mcast.address', '224.0.1.105')
  public static final String MODCLUSTER_MCAST_PORT = Library.getUniversalProperty('modcluster.mcast.port', '23364')
  public static final String UDP_MCAST_ADDRESS = Library.getUniversalProperty('udp.mcast.address', Library.getUniversalProperty("MCAST_ADDR", '228.11.11.11'))
  public static final String JGROUPS_MCAST_PORT = Library.getUniversalProperty('jgroups.mcast.port', Library.getUniversalProperty("MCAST_PORT", '45688'))

  // Added to allow for the new naming convention used in RHEL9
  static final String MOD_CLUSTER_CONFIG_FILE = new Platform().isRHEL9() ? Library.getUniversalProperty('mod.proxy.cluster.config.file', "mod_proxy_cluster.conf") : Library.getUniversalProperty('modcluster.config.file', "mod_cluster.conf")
  static final String MOD_PROXY_CLUSTER_CONFIG_FILE = MOD_CLUSTER_CONFIG_FILE
}
