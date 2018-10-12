package noe.server

import groovy.util.logging.Slf4j

/**
 *  Class for manipulting with ApacheDS server
 *    - Pure java LDAP, Kerberos server
 *
 */
@Slf4j
class ApacheDSUnix extends ApacheDS {

  ApacheDSUnix(String basedir, version) {
    super(basedir, version)
    this.start = ['/bin/sh', 'apacheds.sh']
    if (platform.isSolaris()) this.processCode = "org.apache.directory"
    else this.processCode = "apacheds"
  }
}
