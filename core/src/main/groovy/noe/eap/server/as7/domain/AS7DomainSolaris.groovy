package noe.eap.server.as7.domain

import groovy.transform.TypeChecked
import groovy.util.logging.Slf4j

/**
 * Created by rhatlapa on 11/5/14.
 */
@TypeChecked
@Slf4j
class AS7DomainSolaris extends AS7DomainRhel {
  AS7DomainSolaris(String basedir, String as7Dir = "") {
    super(basedir, as7Dir)
  }
}
