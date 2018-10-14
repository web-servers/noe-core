package noe.server

import groovy.util.logging.Slf4j

/**
 *  Class for manipulting with ApacheDS server
 *    - Pure java LDAP, Kerberos server
 *
 */
@Slf4j
class ApacheDSWindows extends ApacheDS {

  ApacheDSWindows(String basedir, version) {
    super(basedir, version)
    this.start = ['apacheds.bat']
    this.processCode = "ApacheDS"
  }
}
