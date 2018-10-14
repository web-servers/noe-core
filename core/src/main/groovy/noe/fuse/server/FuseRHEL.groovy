package noe.fuse.server

import groovy.util.logging.Slf4j
import noe.server.Fuse
/**
 *
 * @author tplevko
 */
@Slf4j
public class FuseRHEL extends Fuse {

  FuseRHEL(String basedir, String fuseDir = "") {
    super(basedir, fuseDir)
    this.basedir = basedir + "/jboss-fuse-${fuseVersion}"
    this.binDir = this.basedir + "/bin"
    this.binPath = "${platform.sep}bin"
    setDefault()
  }

  void setDefault() {
    super.setDefault()
    this.start = ["./start", "${processCode}"]
    this.stop = ["./stop"]
  }
}
