package noe.eap.server.as7.domain

import groovy.util.logging.Slf4j

/**
 * Created by rhatlapa on 11/5/14.
 */
@Slf4j
class AS7DomainHPUX extends AS7DomainRhel {
  AS7DomainHPUX(String basedir, String as7Dir = "") {
    super(basedir, as7Dir)
  }
}
