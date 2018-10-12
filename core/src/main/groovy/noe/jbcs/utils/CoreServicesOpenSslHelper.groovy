package noe.jbcs.utils

import groovy.transform.TypeChecked
import noe.common.DefaultProperties
import noe.common.utils.Hudson
import noe.common.utils.JBFile
import noe.common.utils.OpenSslVersion
import noe.common.utils.Platform
import noe.common.utils.PlatformSuffixHelper
import noe.workspace.WorkspaceAbstract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@TypeChecked
class CoreServicesOpenSslHelper {
  private static final Logger log = LoggerFactory.getLogger(CoreServicesOpenSslHelper.class)

  final String basedir
  final PlatformSuffixHelper platformUtils
  final String s = File.separator

  private final String zipArchiveDirectory = "${Hudson.staticDir}${s}jbcs${s}openssl"

  CoreServicesOpenSslHelper(String basedir) {
    this.basedir = basedir
    this.platformUtils = new PlatformSuffixHelper(new Platform())
  }

  static String getCoreOpenSslDirName() {
    return "jbcs-openssl-${DefaultProperties.jbcsOpenSslVersion().baseVersionString()}"
  }

  String getCoreOpenSslZipFileName() {
    String platformSuffix = platformUtils.suffixForCoreSvc(new OpenSslVersion("1.0.2h") >= DefaultProperties.jbcsOpenSslVersion())
    return "jbcs-openssl-${DefaultProperties.jbcsOpenSslVersion().toString()}-${platformSuffix}.zip"
  }

  def installZipFile(String zipFileName, String dir, OpenSslVersion version) {
    if (zipFileName.trim().length() != 0) {
      File distroZipSrc = new File(dir + "${s}${version.toString()}${s}" + zipFileName)
      File baseDir = new File(basedir)

      log.trace("Distribution determined: '${distroZipSrc.name}', file: '${distroZipSrc}', target workspace basedir: '${basedir.toString()}'")
      if (!JBFile.copy(distroZipSrc, baseDir)) {
        throw new RuntimeException("Failed to copy openssl zip file from ${distroZipSrc.absolutePath} to ${baseDir.absolutePath}")
      }
      File distForExtraction = new File(basedir, distroZipSrc.name)

      JBFile.nativeUnzip(distForExtraction, baseDir)
      if (DefaultProperties.isRemoveZipAfterUnzip()) {
        JBFile.delete(distForExtraction)
      }
    } else {
      log.warn('Empty zipFileName -> skipping zip installation ...')
    }
  }

  void installZipFile(String zipFileName) {
    installZipFile(zipFileName, zipArchiveDirectory, DefaultProperties.jbcsOpenSslVersion())
  }

  void installZipFile() {
    installZipFile(getCoreOpenSslZipFileName())
  }

  static String getJbcsOpenSslHome() {
    def platform = new Platform()
    return WorkspaceAbstract.retrieveBaseDir() + platform.sep + getCoreOpenSslDirName()
  }

}
