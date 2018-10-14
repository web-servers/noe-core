package noe.common.utils

import groovy.transform.TypeChecked
import groovy.util.logging.Slf4j
/**
 * @author Jan Stefl     <jstefl@redhat.com>
 * @Deprecated define logback.xml and use proper logging using slf4j in your classes. This one is planned to be removed when all logging is migrated.
 */
@Slf4j
@TypeChecked
@Deprecated
class IO {
  static final int LOG_LEVEL_SEVERE = 1
  static final int LOG_LEVEL_WARN = 2
  static final int LOG_LEVEL_INFO = 3
  static final int LOG_LEVEL_FINE = 4
  static final int LOG_LEVEL_FINER = 5
  static final int LOG_LEVEL_FINEST = 6
  static final int LOG_LEVEL_VERBOSE = 7

  static Integer LOGGING_LEVEL = Integer.valueOf(Library.getUniversalProperty('logging.level', String.valueOf(LOG_LEVEL_INFO)).trim())

  static void handleOutput(text) {
    handleOutput(text, LOG_LEVEL_INFO)
  }

  static void handleOutput(text, int level) {
    String textAsString = String.valueOf(text) // originally there wasn't always provided String => lets convert it to String first.
    switch (level) {

      case LOG_LEVEL_SEVERE:
        if (LOGGING_LEVEL >= LOG_LEVEL_SEVERE) log.error(textAsString)
        break

      case LOG_LEVEL_WARN:
        if (LOGGING_LEVEL >= LOG_LEVEL_WARN) log.warn(textAsString)
        break

      case LOG_LEVEL_INFO:
        if (LOGGING_LEVEL >= LOG_LEVEL_INFO) log.info(textAsString)
        break

      case LOG_LEVEL_FINE:
        if (LOGGING_LEVEL >= LOG_LEVEL_FINE) log.debug(textAsString)
        break

      case LOG_LEVEL_FINER:
        if (LOGGING_LEVEL >= LOG_LEVEL_FINER) log.debug(textAsString)
        break

      case LOG_LEVEL_FINEST:
        if (LOGGING_LEVEL >= LOG_LEVEL_FINEST) log.trace(textAsString)
        break
      case LOG_LEVEL_VERBOSE:
        if (LOGGING_LEVEL >= LOG_LEVEL_VERBOSE) log.trace(textAsString)
        break
    }
  }
}
