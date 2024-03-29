package noe.ews.utils

import groovy.util.logging.Slf4j
import noe.common.utils.Platform
import noe.common.utils.Version

/**
 * JWS Name helper provides logic behind JWS ZIP filenames. These filenames differ based on the EWS version, as well as
 * old vs new naming. If you require old naming, toggle compatibility mode. Otherwise, new naming will be provided.
 * Old naming example: jws-application-server-5.0.0-RHEL7-x86_64.zip
 * New naming example: jws-5.0.0-application-server-RHEL7-x86_64.zip
 */
@Slf4j
class JwsNameHelper_Legacy extends AbstractJwsNameHelper{

  JwsNameHelper_Legacy(Version version) {
    this.version = version
    this.jwsMajorVersion = version.majorVersion
    this.platform = new Platform()
    this.productName = jwsMajorVersion <= 2 ? "jboss-ews" : "jws"
    this.archSeparator = "."
  }

  /**
   * Returns either application-server, or application-servers based on the given JWS version
   */
  String applicationServerBaseName() {
    return jwsMajorVersion <= 3 ? "application-servers" : "application-server"
  }
  
  /**
   * Returns either "application-servers" or "application-server"
   */
  String applicationServerNativeName() {
    return applicationServerBaseName()
  }
}
