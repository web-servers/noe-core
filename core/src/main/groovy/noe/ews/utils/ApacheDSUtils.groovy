package noe.ews.utils

import groovy.util.logging.Slf4j
import noe.common.utils.JBFile
import noe.common.utils.Library

@Slf4j
class ApacheDSUtils {

  static String installApacheDS(final File destination) {
    File source = new File(Library.getAppToolsPath(), "ApacheDS20.zip")
    File installRoot = new File(destination, "ApacheDS20")

    if (installRoot.exists()) {
      log.debug("Deleting $installRoot, because it exists")
      JBFile.delete(installRoot)
    }
    log.debug("Installing ApacheDS from $source to $destination")
    JBFile.nativeUnzip(source, destination)
    return installRoot.absolutePath
  }

}
