package noe.eap.server.as7.domain

import groovy.util.logging.Slf4j
import noe.server.AS7Domain
/**
 * Created by rhatlapa on 11/5/14.
 */
@Slf4j
class AS7DomainWindows extends AS7Domain {

  AS7DomainWindows(String basedir, String as7Dir = "") {
    super(basedir, as7Dir)
    setDefault()
  }

  void setDefault() {
    super.setDefault()
    this.start = [
        "domain.bat"
    ]
  }

}
