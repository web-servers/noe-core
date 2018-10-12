package noe.fuse.utils

import groovy.util.logging.Slf4j
import noe.common.utils.Hudson
import noe.common.utils.JBFile
import noe.common.utils.Platform
import noe.server.ServerAbstract
/**
 *
 * @author tplevko
 */
@Slf4j
class FuseUtils {

  final AntBuilder ant
  final String basedir
  final Platform platform
  final String sep = File.separator
  final String fuseVersion
  final String fuseType

  final String fuseServer
  File distroDest

  final String distroStore = "${Hudson.staticDir}${sep}fuse"
  boolean deleteArchive = false
  final String resourcesPath

  /**
   * Get Fuse from the static directory.
   */
  FuseUtils(basedir, ant, platform, fuseVersion, fuseType) {
    this.basedir = basedir
    this.ant = ant
    this.platform = platform
    this.fuseVersion = fuseVersion
    this.fuseType = fuseType
    this.fuseServer = "jboss-fuse-${fuseType}-${fuseVersion}.zip"
    this.resourcesPath = ServerAbstract.getDeplSrcPath()

    if (!(new File(distroStore).exists())) {
      distroStore = Hudson.staticDir + sep + 'fuse'
    }
  }

  synchronized void getIt() {
    log.info(' --- Getting Fuse started --- ')
    //Clean Fuse base dir
    JBFile.delete(new File(basedir, "jboss-fuse-${fuseType}-${fuseVersion}"))

    // Installs Fuse
    installZipFile(fuseServer)

    JBFile.makeAccessible(new File(basedir, "jboss-fuse-${fuseVersion}${sep}bin"))

    log.info(' --- Getting Fuse finished --- ')
  }

  void installZipFile(zipFileName) {
    if (!(zipFileName.trim().length() == 0)) {
      distroDest = new File(distroStore + sep + zipFileName)
      log.info('distroDest: {}', distroDest.name)

      log.info('file: {}', distroDest.name)
      log.info('basedir: {}', basedir.toString())
      if(JBFile.mkdir(new File(basedir))) {
        if (!JBFile.copy(distroDest, new File(basedir))) {
          throw new RuntimeException("When installing zip file, it failed to copy ${distroDest.absolutePath} to ${basedir}." +
                  " The file either is missing in source location or you don't have permission to write to destination.")
        }
      }
      log.info('nativeUnzip SRC: {}', new File(basedir, distroDest.name).absolutePath)
      log.info('nativeUnzip DEST: {}', new File(basedir).absolutePath)
      JBFile.nativeUnzip(new File(basedir, distroDest.name), new File(basedir))

      if (Boolean.valueOf(deleteArchive)) {
        JBFile.delete(distroDest)
      }
    } else {
      log.warn('Empty zipFileName -> skipping zip installation ...')
    }
  }
}
