package noe.eap.server.as7

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.utils.Cmd
import noe.common.utils.Platform
import noe.common.utils.Version
import noe.eap.utils.CLILib
/**
 * Created by rhatlapa on 11/5/14.
 * Should be preferably replaced by some library, what about creaper?
 */
@Slf4j
class AS7Cli {

  public static final int DEFAULT_CLI_TIMEOUT = 120 * 1000
  private Version eapVersion
  private String cfgHost
  private int managementPort
  private File cliClient
  private int timeout // timeout for cli in ms
  private final Platform platform = new Platform() // OMG, Platform should be static.

  AS7Cli(Version eapVersion, String cfgHost, int managementPort, File cliClient, int timeout = DEFAULT_CLI_TIMEOUT) {
    this.eapVersion = eapVersion
    this.cfgHost = cfgHost
    this.managementPort = managementPort
    this.cliClient = cliClient
    this.timeout = timeout
  }

  public List<String> generateCmdForSingleCliCommand(String command) {
    def cliCmd = [
        "${cliClient.absolutePath}",
        "--connect",
        "--controller=${cfgHost}:${managementPort}"
    ]
    if (eapVersion >= new Version("6.1.0")) {
      cliCmd.add("--timeout=${timeout}")
    }
    cliCmd.add("--command=${command}")
    return cliCmd
  }

  /**
   * Generates CLI command --commands="your string"
   *
   * Groovy when launching command as list wraps each part in "" on Windows and '' on Unixlike,
   * one must escape them with this knowledge
   * {@link CLILib#escapeQuotes}
   *
   * @param command , MIND THE '"' and escape them...
   * @return the command to run
   */
  public List<String> generateCmdForArbitraryCliCommand(String command) {
    List<String> myArbitraryCommand = [
        "${cliClient.absolutePath}",
        "--connect",
        "--controller=${cfgHost}:${managementPort}"
    ]
    if (eapVersion >= new Version("6.1.0")) {
      myArbitraryCommand.add("--timeout=${timeout}")
    }
    myArbitraryCommand.add("--commands=${command}")
    return myArbitraryCommand
  }

  /**
   * This string will be passed directly to the --commands="your string"
   *
   * Groovy when launching command as list wraps each part in "" on Windows and '' on Unixlike,
   * one must escape them with this knowledge
   * {@link CLILib#escapeQuotes}
   *
   * @param command , MIND THE '"' and escape them...
   * @return the command to run
   */
  public Map runArbitraryCommand(final String command, int timeout = this.timeout) {
    log.debug("Executing CLI command {}", command)
    final List myArbitraryCommand = generateCmdForArbitraryCliCommand(command)
    return Cmd.executeCommandConsumeStreams(
        myArbitraryCommand,
        cliClient.parentFile,
        platform.isWindows() ? DefaultProperties.NL.bytes : null,
        timeout + 1000, // to give chance to jboss cli console to timeout
        platform.isWindows() ? [NOPAUSE: true] : null)
  }

}

