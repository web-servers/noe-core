package noe.eap.server.as7.domain

import groovy.util.logging.Slf4j
import noe.server.AS7Domain
/**
 * Created by rhatlapa on 11/5/14.
 */
@Slf4j
class AS7DomainRhel extends AS7Domain {

  AS7DomainRhel(String basedir, String as7Dir = "") {
    super(basedir, as7Dir)
    setDefault()
  }

  void setDefault() {
    super.setDefault()
    this.start = [
        "${getBinDirFullPath()}/domain.sh",
        "-D${processCode}"
    ]
  }
}
