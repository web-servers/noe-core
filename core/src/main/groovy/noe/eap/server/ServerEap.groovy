package noe.eap.server

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.utils.Library
import noe.common.utils.Version

@Slf4j
class ServerEap {

  // Whether we use EAP (default) or Wildfly (must be set to true, then).
  public static final boolean IS_WILDFLY = Boolean.valueOf(Library.getUniversalProperty('server.wildfly',
          Boolean.FALSE))

  /**
   * Returns directory prefix of EAP server instance. E.g.: for "jboss-eap-7.1.0-1" instance it returns substring
   * "jboss-eap-7.1.0". Be careful, that this is not derived directly from instance name. "server.wildfly" property is
   * used to determine first part (wildfly vs jboss-eap) of string and "eap.version" property is used to determine
   * second part (version). When "build.zip.root.dir" is defined, it is used as a prefix instead.
   * @return Prefix of EAP server instance
   */
  static String getPrefix() {
    final String buildZipRootDirProperty = Library.getUniversalProperty("build.zip.root.dir")
    final Version eapVersion = DefaultProperties.eapVersion()

    if (buildZipRootDirProperty == null) {
      final String prefix = buildPrefixForEap(IS_WILDFLY, eapVersion)
      log.debug("Build prefix '{}' based on 'server.wildfly' ({}) and 'eap.version' ({}) properties.", prefix,
              IS_WILDFLY, eapVersion.toString())
      return prefix
    } else {
      log.debug("Using value from 'build.zip.root.dir' property as a prefix: '{}'.", buildZipRootDirProperty)
      return buildZipRootDirProperty
    }
  }

  private static String buildPrefixForEap(boolean isWildfly, Version eapVersion) {
    final String eapPrefix = 'jboss-eap'
    final String wildflyPrefix = 'wildfly'

    if (isWildfly) {
      return "${wildflyPrefix}-${eapVersion}"
    } else {
      return "${eapPrefix}-${eapVersion.getMajorVersion()}.${eapVersion.getMinorVersion()}"
    }
  }

}
