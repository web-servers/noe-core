package noe.common.utils

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * @author Jan Stefl <jstefl@redhat.com>
 */
@Slf4j
class Net {
  static Platform platform = new Platform()

  /**
   * Ping address (hostname) natively.
   *
   *   Solaris does not accept params. Do just simple ping (at least Solaris 11)
   */
  static int ping(String address, Integer timeout = 5, Integer count = 5) {
    List command = []
    def timeoutArg
    def countArg
    command.add((platform.isSolaris()) ? '/usr/sbin/ping' : 'ping')
    command.add(address)

    if (!platform.isSolaris()) {
      timeoutArg = '-W'
      countArg = '-c'
      if (platform.isWindows()) {
        timeoutArg = '-w'
        countArg = '-n'
      }
      if (platform.isHP()) {
        timeoutArg = '-m'
        countArg = '-n'
      }

      command.add(countArg)
      command.add(count)
      command.add(timeoutArg)
      command.add(timeout)
    }

    return Cmd.executeCommand(command, new File('.'))
  }

  /**
   * Validate ip address with regular expression
   * @param ip ip address for validation
   * @return true valid ip address, false invalid ip address
   */
  public static boolean validateIpAddress(String ip) {
    Pattern pattern = Pattern.compile(
        "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            '([01]?\\d\\d?|2[0-4]\\d|25[0-5])$')

    Matcher matcher = pattern.matcher(ip)
    return matcher.matches()
  }

  public static String getHostname() {
    String hostname = DefaultProperties.HOST
    try {
      InetAddress addr = InetAddress.getLocalHost()

      // Get hostname
      hostname = addr.getHostName()
    } catch (UnknownHostException e) {
      log.warn("Hostname of ${hostname} wasn't found")
    }
    return hostname
  }

  /**
   * Transforms IP address to hostname
   * @param ipAddress
   * @return hostname , if hostname is not found, the IP address textual representation is returned
   */
  static String getHostname(ipAddress) {
    InetAddress addr = InetAddress.getByName(ipAddress)
    return addr.hostName
  }

  /**
   * Send an arbitrary string to a socket
   * @param host
   * @param port
   * @param request, String to be fed to the socket
   * @param charset used to decode bytes into String, choose carefully, e.g. UTF-8.
   * The default is a single-byte encoding ISO-8859-1 that has the whole lower segment the same as ASCII
   * @return reply from the socket, decoded as ISO-8859-1 string by default
   */
  static String sendStringOverSocket(final String host, final int port, final String request, String charset = "ISO-8859-1") {
    String response = null
    Socket conn = null
    try {
      conn = new Socket(host, port)
      final OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream())
      final InputStream reader = new BufferedInputStream(conn.getInputStream())
      writer.write(request)
      writer.flush()
      response = reader.getText(charset)
      writer.close()
      reader.close()
    } finally {
      conn?.close()
    }
    return response
  }
}
