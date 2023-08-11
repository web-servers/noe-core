package noe.ews.utils

import groovy.util.logging.Slf4j
import noe.common.utils.Version

/**
 * JWS Name helper provides logic behind JWS ZIP filenames. These filenames differ based on the EWS version, as well as
 * old vs new naming. If you require old naming, toggle compatibility mode. Otherwise, new naming will be provided.
 * Old naming example: jws-application-server-5.0.0-RHEL7-x86_64.zip
 * New naming example: jws-5.0.0-application-server-RHEL7-x86_64.zip
 */
@Slf4j
class JwsNameHelper {
  private AbstractJwsNameHelper helper = null

  JwsNameHelper(Version version) {
    if (version.getMajorVersion() <= 5) {
      helper = new JwsNameHelper_Legacy(version)
    } else {
      helper = new JwsNameHelper_Current(version)
    }
  }

  void setArchSeparator(String archSeparator) {
    helper.setArchSeparator(archSeparator)
  }

  /**
   * Returns the zip name for the Hibernate zip, based on the JWS version and compatibility mode toggle
  */
  String getHibernateZipName() {
    return helper.getHibernateZipName()
  }

  /**
   * Builds the name of JWS zip natives. Returns something like jboss-ews-application-servers-2.0.0-ER5-RHEL6-i386.zip
   */
  String getJWSApplicationServerNativeZipFileName() {
    return helper.getJWSApplicationServerNativeZipFileName()
  }

  /**
   * Builds the name of JWS zip Java base. Returns something like jboss-ews-application-servers-3.0.0-ER5.zip
   */
  String getJWSApplicationServerBaseZipFileName() {
    return helper.getJWSApplicationServerBaseZipFileName()
  }

  /**
   * Builds the name of examples zip, e.g. jws-examples-3.0.0-ER5.zip
   */
  String getExamplesZipFileName() {
    return helper.getExamplesZipFileName()
  }

  /**
   * Returns either application-server, or application-servers based on how many tomcats are in the given JWS version
   */
  String applicationServerBaseName() {
    return helper.applicationServerBaseName()
  }

  /**
   * Toggle the compatibility mode so that you can get old/new naming scheme of the zipnames
   */
  void toggleCompatibilityMode() {
    helper.compatibilityMode = !helper.compatibilityMode
  }
}
