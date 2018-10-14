package noe.fuse.server

import noe.server.Fuse

/**
 * TODO(tplevko): not working yet, TBD
 *
 * @author tplevko
 */
public class FuseWindows extends Fuse {

  FuseWindows(String basedir, String fuseDir = "") {
    super(basedir, fuseDir)
    this.binPath = "${platform.sep}bin"
    setDefault()
  }

  void setDefault() {
    super.setDefault()
  }
}
