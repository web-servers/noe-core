package noe.common.utils

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.server.ServerAbstract
import noe.workspace.WorkspaceAbstract

/**
 * Shared for all utils classes, zip installation is similar
 * @author Bogdan Sikora <bsikora@redhat.com>
 */
@Slf4j
class InstallerUtils {

  String pathToSource
  String basedir
  Boolean isStaticEnvVerifiedUrl
  Platform platform
  Version version
  String resourcesPath
  PlatformSuffixHelper platformUtils

  InstallerUtils(String prefix, Version version) {
    this(prefix, version, WorkspaceAbstract.retrieveBaseDir())
  }

  /**
   * Zips are looked up in
   *                   if {@link Hudson#staticDir} is system path then
   *                                        {@link Hudson#staticDir}/{@code prefix}
   *                   else if {@link Hudson#staticDir} is url, zips are downloaded from that page
   * @param prefix, under from directory are zips stored after {@link Hudson#staticDir}
   * @param version, version of the product this InstallerUtils represent
   * @param basedir used for installation
   */
  InstallerUtils(String prefix, Version version, String basedir) {
    this.basedir = basedir
    this.platform = WorkspaceAbstract.platform
    this.resourcesPath = ServerAbstract.getDeplSrcPath()
    this.version = version
    this.platformUtils = new PlatformSuffixHelper(platform)

    String hudsonStaticEnv = Hudson.staticDir

    if (hudsonStaticEnv) {
      try {
        this.isStaticEnvVerifiedUrl = (Library.getHttpStatusCode(hudsonStaticEnv.toURL(), true) == 200)
        pathToSource = hudsonStaticEnv
      } catch (MalformedURLException ex) {
        log.trace('HUDSON_STATIC_ENV is not valid URL')
        this.isStaticEnvVerifiedUrl = false
      }
      if (!isStaticEnvVerifiedUrl) {
        pathToSource = PathHelper.join(hudsonStaticEnv, prefix, version.toString())
      }
    } else {
      throw new RuntimeException('HUDSON_STATIC_ENV is not set in environment')
    }
  }

  /**
   * Install zip file to basedir
   *        1. Copy zip file to basedir (fail if zip file not found)
   *        2. Unzip file
   *        3. Delete zip file if {@link DefaultProperties#isRemoveZipAfterUnzip()}
   * @param zipFileName
   */
  void installZipFile(String zipFileName) throws FileNotFoundException {

    if (!zipFileName || zipFileName.trim().length() == 0) {
      log.warn('Empty zipFileName -> skipping zip installation ...')
      return
    }

    File zipSource = new File(pathToSource, zipFileName)
    File zipDest = new File(basedir, zipFileName)

    if (isStaticEnvVerifiedUrl) {
      String downloadZipUrl = "${pathToSource}/${zipFileName}"
      log.info ("Downloading ${downloadZipUrl} to ${zipDest}")
      Library.downloadFile(downloadZipUrl, zipDest)
    } else {
      if (!JBFile.isExistingFile(zipSource)) {
        throw new FileNotFoundException("Expected zip file ${zipSource} doesn't exist.")
      }
      File dest = new File(basedir)
      if (!JBFile.copy(zipSource, dest)) {
        throw new FileNotFoundException("Something has gone wrong with copying file ${zipSource} to the targed directory ${dest}. Check debug log for more information.")
      }
      log.info ("Copying ${zipSource} to ${dest.canonicalPath}")
    }

    if (!zipDest.exists()) {
      log.error('FAILED to Install zip file ' + zipSource + ' to ' + basedir)
      log.error("Zip file $zipSource, exist = ${zipSource.exists()}")
      throw new FileNotFoundException("Zip installation failed, $zipDest doesn't exits")
    }
    JBFile.nativeUnzip(zipDest, new File(basedir))
    if (DefaultProperties.isRemoveZipAfterUnzip()) {
      JBFile.delete(zipDest)
    }
  }
}
