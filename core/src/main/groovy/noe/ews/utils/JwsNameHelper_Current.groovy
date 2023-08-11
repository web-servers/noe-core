package noe.ews.utils

import groovy.util.logging.Slf4j
import noe.common.utils.Platform
import noe.common.utils.Version

/**
 * JWS Name helper provides logic behind JWS ZIP filenames. These filenames differ based on the EWS version, as well as
 * old vs new naming. If you require old naming, toggle compatibility mode. Otherwise, new naming will be provided.
 * naming example 1: jws-6.0.0-application-server-RHEL8-x86_64.zip
 * naming example 2: jws-6.0.0-optional-native-components-RHEL8-x86_64.zip
 */
@Slf4j
class JwsNameHelper_Current extends AbstractJwsNameHelper{

  JwsNameHelper_Current(Version version) {
    this.version = version
    this.jwsMajorVersion = version.majorVersion
    this.platform = new Platform()
    this.productName = "jws"
    this.archSeparator = "."
  }

  /**
   * Returns application-server
   */
  String applicationServerBaseName() {
    return "application-server"
  }
  
  /**
   * Returns optional-native-components
   */
  String applicationServerNativeName() {
    return "optional-native-components"
  }
}
