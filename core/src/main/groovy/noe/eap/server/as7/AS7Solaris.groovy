package noe.eap.server.as7

import groovy.util.logging.Slf4j

/**
 * AS7Solaris implements Solaris specific
 * aspects of AS7 setup and execution.
 * This includes paths, commands (sh/bat) and more.
 *
 * @author Michal Karm Babacek <mbabacek@redhat.com>
 *
 */
@Slf4j
class AS7Solaris extends AS7Rhel {

  AS7Solaris(String basedir, String as7Dir = "") {
    super(basedir, as7Dir)
  }
}
