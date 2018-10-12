package noe.tomcat.configure.envars

import noe.common.utils.Platform

/**
 * IMPORTANT: For usage within noe-core:tomcat.configure only
 *
 * Generates environment variables assignment with regard on platform.
 * Non-Windows<br>
 *   VARIABLE="value" && export VARIABLE
 * Windows<br>
 *   set "VARIABLE=value"
 */
class ZipTomcatEnvVariableAssigmentsGenerator {
  boolean isWindows = new Platform().isWindows()

  String generateEnvLine(String name, String value) {
    def expression = "${name}=${getValueStartQuotation()}${value}${getValueEndQuotation()}"
    def line = "${getExprPrefix()}${expression}${getExprSuffix(name)}"

    return line
  }

  String getExprPrefix() { isWindows ? 'set "' : '' }
  String getExprSuffix(String name) { isWindows ? '"' : " && export ${name} " }
  String getValueStartQuotation() { isWindows ? '' : '"' }
  String getValueEndQuotation() { isWindows ? '' : '"' }
}
