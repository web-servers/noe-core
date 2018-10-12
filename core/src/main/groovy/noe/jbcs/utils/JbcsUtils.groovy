package noe.jbcs.utils

import groovy.transform.TypeChecked
import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.utils.JBFile
import noe.common.utils.Library
import noe.common.utils.PlatformSuffixHelper
import noe.common.utils.InstallerUtils
import noe.ews.server.ServerEws
import noe.workspace.WorkspaceAbstract

/**
 * Class representing jbcs zip installation related stuff
 * @author Bogdan Sikora (bsikora@redhat.com)
 * @author Filip Goldefus
 */
@TypeChecked
@Slf4j
class JbcsUtils extends InstallerUtils {

  final static String PREFIX = "jbcs${WorkspaceAbstract.platform.sep}httpd"

  String jbcsHttpdPrefix
  String coreHttpd

  JbcsUtils() {
    super(PREFIX, DefaultProperties.apacheCoreVersion())
    this.platform = platform
    this.platformUtils = new PlatformSuffixHelper(platform)
    // JBCS prefix zip file names prefix
    this.jbcsHttpdPrefix = Library.getUniversalProperty('jbcs.httpd.prefix', 'jbcs-httpd24')
    this.coreHttpd = getCoreHttpdZipFileName()
  }

  void installHttpd(Boolean purge = true) {
    if (!DefaultProperties.USE_HTTPD_RPM) {
      //Clean Httpd dir
      File jbcsHome = new File(ServerEws.getHttpdHome())
      if (purge) {
        JBFile.delete(jbcsHome)
        if (jbcsHome.exists()) {
          throw new RuntimeException('Purge of old httpd installation not successful')
        }
      }

      // Installs Httpd
      log.debug("CoreVersion ${DefaultProperties.apacheCoreVersion()} and coreHttpdZip ${coreHttpd}")
      try {
        installZipFile(coreHttpd)
      } catch (FileNotFoundException e) {
        String alternativeCoreHttpd = getCoreHttpdZipFileName(true)
        if (this.coreHttpd == alternativeCoreHttpd) {
          log.debug("Failed to find: ${this.coreHttpd}, no alternative name to try.")
          throw e
        }
        log.info("Failed to find: ${this.coreHttpd}, trying ${alternativeCoreHttpd}.")
        installZipFile(alternativeCoreHttpd)
      }
    }
  }

  String getCoreHttpdZipFileName(boolean alternative = false) {
    String suffix
    if (alternative) {
      suffix = platformUtils.suffixForCoreSvc(false)
    } else {
      suffix = platformUtils.suffixForCoreSvc()
    }

    return "${jbcsHttpdPrefix}-httpd-${DefaultProperties.apacheCoreVersion()}-${suffix}.zip"
  }
}
