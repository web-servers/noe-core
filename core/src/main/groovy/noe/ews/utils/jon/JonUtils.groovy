package noe.ews.utils.jon

/**
 * Author: jmartisk
 * Date: 7/30/12
 * Time: 11:30 AM
 */
class JonUtils {

  public static boolean wasTomcatDiscovered(String output) {
    return output.contains("type={Tomcat}Tomcat Server")
  }

  public static boolean wasHttpdDiscovered(String output) {
    return output.contains("type={Apache}Apache HTTP Server")
  }

  public static Process runStandalonePluginContainer(String absolutePathToScriptFile) {
    return Runtime.getRuntime().exec("${System.getProperty("jon.agent.home")}/bin/standalone.sh $absolutePathToScriptFile")
  }
}
