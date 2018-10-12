package noe.ews.server.tomcat

import groovy.util.logging.Slf4j
import noe.common.utils.Cmd
import noe.common.utils.JBFile
import noe.common.utils.Library
import noe.common.utils.Platform

/**
 * Solaris service handling for Tomcat. Solaris uses Service Management Facility in order to install and manage applications as services.
 * The following helper class relies on the tomcatX.xml file, which is a manifest file that specifies methods that a user can execute and
 * path to a script that will be called on the backend. The tomcatX file is changed by the testsuite such that path to the actual script is used.
 *
 * Note that for the service to work, you have to run the postinstall script as a superuser and must run JWS as Tomcat
 * (or otherwise create the user Tomcat and manage the file permissions).
 *
 * For troubleshooting, see https://docs.oracle.com/cd/E36784_01/html/E36820/svcdelete.html
 */

@Slf4j
class TomcatSolarisServiceCommandUtils {
  final static String PATH_TO_REPLACE_IN_MANIFEST_TEMPLATE = '/opt/jboss-ews-2.0/sbin'
  TomcatSolaris tomcat
  boolean useSudo
  String tomcatRunUser
  Platform platform
  // This is a a special directory where Solaris expects the XML file. Copying the xml manifest there and
  // reloading manifest-import should import the service
  static File serviceManifestsDirectory = new File('/var/svc/manifest/application/web')
  String pathToManifestsInResources
  String shortServiceName
  int tomcatMajorVersion


  TomcatSolarisServiceCommandUtils(TomcatSolaris tomcat) {
    this.tomcat = tomcat
    this.useSudo = JBFile.useAdminPrivileges
    this.tomcatRunUser = TomcatCommandUtils.getTomcatRunUser()
    this.platform = new Platform()
    this.tomcatMajorVersion = tomcat.getVersion().getMajorVersion()
    this.shortServiceName = "tomcat${tomcatMajorVersion}"
    this.pathToManifestsInResources = "tomcat/service/solaris/${shortServiceName}.xml"
  }

  void uninstallAllExistingAndInstallFresh() {
    log.debug("Starting Uninstalling all Tomcat Solaris services and installing fresh Tomcat service")
    if (platform.isSolaris()) {
      if (useSudo) {
        uninstallAllExisting()
        deployTemplateManifestTemplate()
        updateTemplateManifest()
        reimportSmfRepository()
      } else {
        throw new IllegalArgumentException("It is not possible to uninstall Solaris service without sudo. Please set USE_ADMIN_PRIVILEGES=true")
      }
    } else {
      throw new IllegalStateException("Trying to uninstall Solaris system service but running on different platform than Solaris.")
    }
  }

  static void uninstallAllExisting() {
    log.debug("Starting Uninstalling all Tomcat Solaris")
    if (new Platform().isSolaris()) {
      if (JBFile.useAdminPrivileges) {

        if (anyAnyTomcatServiceExists()) {
          disableAllTomcatServices()
          deleteAllTomcatServices()
          removeAndCreateManifestsDirectory()
          reimportSmfRepository()
        }

      } else {
        throw new IllegalArgumentException("It is not possible to uninstall Solaris service without sudo. Please set USE_ADMIN_PRIVILEGES=true")
      }
    } else {
      throw new IllegalStateException("Trying to uninstall Solaris system service but running on different platform than Solaris.")
    }
  }

  private static void disableAllTomcatServices() {
    Cmd.executeCommandConsumeStreams(
        ["/bin/bash", "-c", "/usr/bin/svcs -a | grep tomcat | tr -s ' ' | cut -d' ' -f3 | xargs sudo /usr/sbin/svcadm disable"], new File('.')
    )
  }

  private static void deleteAllTomcatServices() {
    Cmd.executeCommandConsumeStreams(
        ["/bin/bash", "-c", "/usr/bin/svcs -a | grep tomcat | tr -s ' ' | cut -d' ' -f3 | xargs sudo /usr/sbin/svccfg delete"], new File('.')
    )
  }

  private static boolean anyAnyTomcatServiceExists() {
    int returnCode = Cmd.executeCommandConsumeStreams(['/bin/bash', '-c', '/usr/bin/svcs -a | grep tomcat'], new File('.')).exitValue
    return returnCode == 0
  }

  private static void removeAndCreateManifestsDirectory() {
    removeManifestsDirectory()
    createManifestsDirectory()
  }

  private static void createManifestsDirectory() {
    if (!JBFile.mkdir(serviceManifestsDirectory)) {
      throw new RuntimeException("Can not create directory ${serviceManifestsDirectory}")
    }
  }

  private static void removeManifestsDirectory() {
    if (serviceManifestsDirectory.exists()) {
      if (!JBFile.delete(serviceManifestsDirectory)) {
        throw new RuntimeException("Can not delete directory ${serviceManifestsDirectory}")
      }
    }
  }

  private void updateTemplateManifest() {
    File deployedManifest = new File(serviceManifestsDirectory, shortServiceName + ".xml")
    String match = PATH_TO_REPLACE_IN_MANIFEST_TEMPLATE
    String replace
    if(tomcatMajorVersion < 9) {
      // This is JWS 3 and older; sbin is in order
      replace = tomcat.getSbin()
    } else {
      // only JWS 5 and newer contain Tomcat9; sbin has been deprecated, we now have services directory
      replace = tomcat.servicesDir
    }

    JBFile.replace(deployedManifest, match, replace)
  }

  private deployTemplateManifestTemplate() {
    Library.copyResourceTo(pathToManifestsInResources, serviceManifestsDirectory)
  }

  private static reimportSmfRepository() {
    int returnCode = Cmd.executeSudoCommandConsumeStreams(['/usr/sbin/svcadm', 'restart', 'svc:/system/manifest-import'], new File('.')).exitValue

    if (returnCode > 0) {
      throw new RuntimeException("reimport SMF repository failed")
    }
  }

}
