package noe.common.utils

import groovy.util.logging.Slf4j


/**
 * Class Cleaner is meant for overall cleaning of the workspace to make sure that tests are
 * not influenced by any condition that may persist from last or other tests
 * @author Bogdan Sikora    bsikora@redhat.com
 */
@Slf4j
class Cleaner {

  private static Platform platform = new Platform()

  /**
   * Method that clears Default Library Path from all previous jws/ews/jbcs/httpd/eap installation
   * to make sure that actual installation is using correct libraries
   * @param pathsToDelete as regex for matching each particular path and if matcher then deleting it, for example look at defaultPathsRegex variable
   * @return 1 - When regex couldn't match paths or crle not returned 0; 2 - You don't have admin privileges; 3 - This only works on Solaris; 4 - Unexplained error
   */
  static int clearSolarisDefaultLibraryPath(String pathsToDelete = /.*(jws|ews|jbcs|httpd|eap).*/) {
    if ( !JBFile.useAdminPrivileges ) {
      log.warn('One need admin privileges to set Default Library Path')
      return 2
    }
    if ( !platform.isSolaris() ) {
      log.warn('Crle is only present on Solaris')
      return 3
    }
    List<String> scriptLaunch = (Library.getUniversalProperty('solaris.preferred.arch') == '32') ? ['crle'] : ['crle', '-64']
    String crleSetNewPathsOption = '-l'
    String newPaths
    //[^\s]+, there is low possibility of a space in the paths. Therefore space is considered as the end of the paths noe wants to match
    String regexForDefaultPaths = /.*Default Library Path \(ELF\):\s*([^\s]+).*/
    long timeout = 10000
    Map crleOutput
    boolean gotOutput = false
    // There is a rare unexplained issue where crle command return 0 but there is no output to work with
    // Let's try to call it few times (10s) to not interfere with test run
    while (!gotOutput && timeout > 0) {
      timeout += new Date().getTime()
      crleOutput = (Cmd.executeCommandConsumeStreams(scriptLaunch, new File('.')))
      if ( crleOutput.exitValue != 0 ) {
        log.info('Something went wrong when calling crle command, stderr -> {}', crleOutput.stdErr)
        return 1
      } else if (!((String)crleOutput.stdOut).isEmpty() || !((String)crleOutput.stdErr).isEmpty()) {
        gotOutput = true; // return code is 0 and there is some output to work with
      }
      timeout -= new Date().getTime()
    }
    if (!gotOutput) {
      log.info('Something went wrong when calling crle, return code was always 0, but there was no output')
      return 4
    }
    log.trace('Output of crle: {}', crleOutput.stdOut)
    def group = (crleOutput.stdOut =~ regexForDefaultPaths)
    if (group.size() != 1 || (group[0]).size() != 2) {
      log.warn('Error has occurred while clearing Default Library Path from old paths')
      return 1
    }
    String defaultPaths = group[0][1]
    defaultPaths.split(':').with { paths ->
      paths.each { onePath ->
        if (!(onePath ==~ pathsToDelete)) {
          if (!newPaths) {
            newPaths = onePath
          } else {
            newPaths = newPaths + ':' + onePath
          }
        }
      }
    }
    if ( Cmd.executeSudoCommand(scriptLaunch + [crleSetNewPathsOption, newPaths], new File('.')) ) {
      return 1
    }
    return 0
  }

  /**
   * Method for safe cleaning a directory, remove only files/folder you give it as regex or use default
   * @param File file as directory which content should be deleted
   * @param String regexToDelete as regex to match files/folder you want to be removed ('.*(jws|ews|jbcs|httpd|eap).*')
   * @return false - something went wrong when deleting file/folder
   */
  static boolean cleanDirectoryBasedOnRegex(File file, String regexToDelete) {
    log.info("Safe cleaning ${file.getName()} of files that match $regexToDelete")
    file.eachFile {
      if ( it.getName() ==~ regexToDelete ) {
        log.info("Deleting file/dir ${it.getName()}")
        if ( !JBFile.delete(new File(it.getAbsolutePath())) ) {
          log.warn("Deleting of ${it.getName()} has failed")
          return false
        }
      }
    }
    return true
  }
}
