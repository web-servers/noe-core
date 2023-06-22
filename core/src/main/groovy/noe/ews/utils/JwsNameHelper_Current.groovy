package noe.ews.utils

import groovy.util.logging.Slf4j
import noe.common.utils.Platform
import noe.common.utils.Version

/**
 * JWS Name helper provides logic behind JWS ZIP filenames. These filenames differ based on the EWS version, as well as
 * old vs new naming. If you require old naming, toggle compatibility mode. Otherwise, new naming will be provided.
 * naming example 2: Red-Hat-JBoss-Web-Server-5.0.0-Optional-Native-Components-for-RHEL7-x86_64.zip
 */
@Slf4j
class JwsNameHelper_Current extends AbstractJwsNameHelper{

  JwsNameHelper_Current(Version version) {
    this.version = version
    this.jwsMajorVersion = version.majorVersion
    this.platform = new Platform()
    productName = "jws"
    archSeparator = "."
  }

  /**
   * Returns Optional-Native-Components-for
   */
  String applicationServerBaseName() {
    return "optional-native-components"
  }
}
