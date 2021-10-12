package noe.common.utils

import com.gargoylesoftware.htmlunit.CookieManager
import com.gargoylesoftware.htmlunit.Page
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.WebResponse
import com.gargoylesoftware.htmlunit.util.Cookie
import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.NOEWebClient

import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * @author Jan Stefl     <jstefl@redhat.com>
 * @author Martin Vecera <mvecera@redhat.com>
 *
 */


@Slf4j
class Library {

  private static Platform platform = new Platform()

  private Library() {
  }

  /**
   * Get property from system properties and environment variables.
   * Input is of form my.great.property. Following properties
   * will be tested (the first not null value will be returned):
   * * 'my.great.property' in system properties
   * * 'my.great.property' in environment variables
   * * 'my_great_property' in system properties
   * * 'my_great_property' in environment variables
   * * 'MY_GREAT_PROPERTY' in environment variables
   * * 'myGreatProperty'   in system properties
   */
  static String getUniversalProperty(String propName) {
    def propName2 = propName.replaceAll('\\.', '_')
    def propName3 = propName.replaceAll('\\.', '_').toUpperCase()
    def propname4 = propName.replaceAll('_', '.').toLowerCase()
    def propName5 = null
    def sp = propName.split('\\.')
    if (sp.length > 1) {
      propName5 = sp[0] + sp[1..-1].collect { it.capitalize() }.join()
    }

    def val = System.getProperty(propName) ?: System.getenv(propName)
    if (!val) val = System.getProperty(propName2) ?: System.getenv(propName2)
    if (!val) val = System.getProperty(propName3) ?: System.getenv(propName3)
    if (!val) val = System.getProperty(propname4) ?: System.getenv(propname4)
    if (!val && propName5) val = System.getProperty(propName5) ?: System.getenv(propName5)
    return val
  }

  static String getUniversalProperty(String propName, Object defaultValue) {
    return getUniversalProperty(propName) ?: defaultValue
  }

  /**
   * Renames key in the map by putting old value under a new key and removing the old key
   * @param map a map to update
   * @param oldKey old key name
   * @param newKey new key name
   */
  static void renameKeyInMap(Map map, oldKey, newKey) {
    // in AS5 the property sslKeyStorePassword is named sslKeyStorePass
    if (map.containsKey(oldKey)) {
      map.put(newKey, map[oldKey])
      map.remove(oldKey)
    }

  }

  /**
   * @deprecated Use {@link noe.common.utils.JBFile#nativeUnzip(java.io.File, java.io.File, java.lang.Boolean, java.lang.Boolean)} instead
   */
  def static nativeUnzip(AntBuilder ant, String srcFile, String dstFile) {
    return JBFile.nativeUnzip(new File(srcFile), new File(dstFile))
  }

  /**
   * Check if all paths are valid. Returns invalid paths.
   */
  static List<String> validatePaths(List paths) {
    def res = []
    paths.each { path ->
      if (path instanceof String) {
        if (!(new File(path).exists())) res << path
      } else if (path instanceof List) {
        res = res + Library.validatePaths(path)
      }
    }

    return res
  }

  static List map2list(Map m) {
    def l = new ArrayList()

    m.each { key, val ->
      l << "${key}=${val}"
    }

    return l
  }

  static Map mapUnion(Map base, Map extension) {
    def result = new HashMap(base)

    extension.each { key, value ->
      if (result.containsKey(key)) {
        result[key] = "${value}${platform.pathsep}${result[key]}"
      } else {
        result.put(key, value)
      }
    }

    return result
  }

  static boolean waitForUrl(Map params = [:], String urldef, final int tsec) {
    def url = new URL(urldef)
    def now = System.currentTimeMillis()
    def startTime = now
    def connTimeout = params["connectTimeout"] ?: 1000
    def readTimeout = params["readTimeout"] ?: 2000
    while (now - startTime < 1000 * tsec) {
      try {
        def conn = url.openConnection()
        conn.setConnectTimeout(connTimeout)
        conn.setReadTimeout(readTimeout)
        conn.getContent()
        return true
      } catch (IOException e) {
        // ignore
      }
      Thread.sleep(1000)
      now += 1000
    }
    return false
  }

  static boolean checkTcpPort(int port) {
    return checkTcpPort(DefaultProperties.HOST, port)
  }

  static boolean checkTcpPort(String host, int port, int timeout = 4000) {
    def address
    def socket
    try {
      if (host == "0.0.0.0") host = "127.0.0.1"
      if (host == "::") host = "::1"
      address = java.net.InetAddress.getByName(host)
    } catch (java.net.UnknownHostException e) {
      println "host unknown: $host"
      return false
    }
    def socketAddr = new java.net.InetSocketAddress(address, port)
    try {
      socket = new java.net.Socket()
      socket.connect(socketAddr, timeout)
      socket.shutdownInput()
      socket.shutdownOutput()
      return true
    } catch (IOException e) {
      log.trace("checkTcpPort: TAGOTAG: host:port - ${host}:${port} is not ready", e)
      return false
    } finally {
      socket?.close()
    }
  }

  /**
   * Waits until a port is opened by some process. If there is connection refused, the method waits until tsec timeout runs out.
   * If the connection is accepted, we consider the port ready to be used since the process that opened it is listening.
   * Note that no writing to the socket is done here. Writing garbage to a socket might shut down the process that is listening.
   * @param host The hostname of the port, e.g. 127.0.0.1
   * @param port The port you want to check
   * @param tsec How long you want to wait for the whole check to finish, in seconds
   * @param stimeout Timeout for the socket to connect to the address
   * @return true if the port is open
   */
  static boolean waitForTcp(String host, final int port, final int tsec, final int stimeout = 4000) {
    def address
    def socket
    def now = System.currentTimeMillis()
    def startTime = now
    try {
      if (host == "0.0.0.0") host = "127.0.0.1"
      if (host == "::") host = "::1"
      address = java.net.InetAddress.getByName(host)
    } catch (java.net.UnknownHostException e) {
      println "host unknown: $host"
      return false
    }
    def socketAddr = new java.net.InetSocketAddress(address, port)
    while (now - startTime < 1000 * tsec) {
      try {
        socket = new java.net.Socket()
        socket.connect(socketAddr, stimeout)
        socket.shutdownInput()
        socket.shutdownOutput()
        return true
      } catch (IOException e) {
        log.trace("waitForTcp: TAGOTAG: host:port - ${host}:${port} is not ready", e)
      } finally {
        socket?.close()
      }
      Library.letsSleep(1000)
      now += 1000
    }
    // still port not ready, failing
    return false
  }

  /**
   * Waits for port to be closed. If the method can write to a socket, the socket is still open by some process.
   * If we get an IOException, it means socket is not open by another process. Note that this does not necessarily
   * mean that port is available for further use; it can be reserved by the system to ensure all communication has
   * ceased properly.
   * @param host The hostname of the port, e.g. 127.0.0.1
   * @param port The port you want to check
   * @param tsec How long you want to wait for the whole check to finish, in seconds
   * @param stimeout Timeout for the socket to connect to the address
   * @return true if the socket is closed
   */
  static boolean waitForTcpClosed(String host, int port, final int tsec, int stimeout = 4000) {
    InetAddress address
    Socket socket
    long now = System.currentTimeMillis()
    long startTime = now
    try {
      address = InetAddress.getByName(host)
    } catch (UnknownHostException e) {
      log.trace("host unknown: $host")
      return false
    }
    InetSocketAddress socketAddr = new InetSocketAddress(address, port)
    while (now - startTime < 1000 * tsec) {
      try {
        socket = new Socket()
        socket.connect(socketAddr, stimeout)
        socket.setSendBufferSize(1)
        socket << 1
        socket.shutdownInput()
        socket.shutdownOutput()
        log.debug("Socket apparently still available? :-( host: ${host}, port: ${port}, tsec: ${tsec}, stimeout: ${stimeout}")
      } catch (IOException e) {
        //port closed
        log.trace("Returning true, closed.")
        return true
      } finally {
        socket?.close()
      }
      Library.letsSleep(1000)
      now += 1000
    }
    // still port opened
    log.trace("Returning, still opened :-(")
    return false
  }

  static boolean waitForPortsAvailable(Map params) {
    def now = System.currentTimeMillis()
    def startTime = now
    def ports = []
    def host = params["host"]

    if (params["port"] != null) ports.add(params["port"])
    else if (params["ports"] != null) params["ports"].each { ports.add(it) }
    else throw new RuntimeException("parameter port or ports needs to be specified")

    if (params["timeout"] == null) throw new RuntimeException("You need to specify timeout parameter in seconds")
    final int tsec = params["timeout"]

    while (now - startTime < 1000 * tsec) {
      ports = ports.findAll { port ->
        // TODO review checking of all available local address
        !portIsAvailable(port, host)
      }
      if (ports.isEmpty()) return true
      Thread.sleep(1000)
      now += 1000
    }

    // still some of the ports unavailable
    return false
  }

  /**
   * Tries to close port to make it available for further use. Inspired by Apache Camel:
   * http://svn.apache.org/viewvc/camel/trunk/components/camel-test/src/main/java/org/apache/camel/test/AvailablePortFinder.java?view=markup#l130
   * Note that if a process is using the port, this will not make a difference. However, if a
   * process stopped using it, the OS may still hold it as bound even if the process is already free. In that case,
   * this method should help.
   *
   * @param port Port to be made available
   * @param host The hostname of the port, e.g. 127.0.0.1
   * @return true if port was made available
   */
  static boolean portIsAvailable(int port, String addr) {
    ServerSocket ss = null
    DatagramSocket ds = null
    try {

      ss = new ServerSocket(port, 0, InetAddress.getByName(addr))
      ss.setReuseAddress(true)
      ds = new DatagramSocket(port, InetAddress.getByName(addr))
      ds.setReuseAddress(true)

      return true
    } catch (IOException e) {
    } finally {
      if (ds != null) {
        ds.close()
      }

      if (ss != null) {
        try {
          ss.close()
        } catch (IOException e) {
          /* should not be thrown */
        }
      }
    }

    return false
  }

  static int getHttpStatusCode(URL url, boolean allowRedirects = true) {
    HttpURLConnection.setFollowRedirects(allowRedirects)
    def HttpURLConnection conn = (HttpURLConnection) url.openConnection()
    def responseCode = conn.responseCode
    conn.disconnect()
    return responseCode
  }

  static int getHttpStatusCodeWithAuthentication(URL url, boolean allowRedirects = true,
                                                        String username, String password) {
    HttpURLConnection.setFollowRedirects(allowRedirects)
    def HttpURLConnection conn = (HttpURLConnection) url.openConnection()
    def login = "${username}:${password}"
    def basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(login.getBytes())
    conn.setRequestProperty("Authorization", basicAuth)
    return conn.responseCode
  }

  /**
   Authenticate to specified URL and POST data (header and body)
   @return HTTP response code
   */
  static int getHttpStatusCodeAndPostDataWithAuthentication(URL url, String header, String body,
                                                                   String username, String password,
                                                                   int readTimeout = 1000, boolean allowRedirects = true) {
    def bodyBytes = URLEncoder.encode(body, 'UTF8').getBytes()

    def HttpURLConnection conn = (HttpURLConnection) url.openConnection();

    conn.setDoOutput(true);
    conn.setDoInput(true);
    conn.setInstanceFollowRedirects(allowRedirects);

    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", header)
    conn.setRequestProperty("Content-Length", Integer.toString(bodyBytes.length));
    def login = "${username}:${password}"
    def basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(login.getBytes());
    conn.setRequestProperty("Authorization", basicAuth);
    conn.setUseCaches(false);
    conn.setReadTimeout(readTimeout);

    OutputStream out = conn.getOutputStream();
    out.write(bodyBytes);
    out.close();

    return conn.getResponseCode()
  }

  // TODO: check usage and refactor the name
  static boolean ChecLinksBrowser() {
    List<String> command
    if (platform.isSolaris()) {
      command = ["lynx", "-version"]
    } else if (platform.isRHEL()) {
      command = ["links", "-version"]
    } else {
      return false
    }
    try {
      return (Cmd.executeCommandConsumeStreams(command, new File('.'))).exitValue == 0
    } catch (IOException ex) {
      return false
    }
  }

  /**
   *  Is MS Windows service installed ?
   *
   */
  static boolean isWinServiceInstalled(String servicename) {
    if (platform.isWindows()) {
      int ret = Cmd.executeCommand(['sc', 'query', servicename], new File("."))
      if (ret == 0) {
        return true
      } else {
        return false
      }
    } else {
      return false
    }
  }

  /**
   * Retrieves resource from classpath and returns it as File, it is able to get the resource even from jar on classpath
   * @param resource path to resource
   * @param clazz class which should be used as a context for looking for the resource, if not specified Thread.currentThread().getContextClassLoader() is used
   * @return File with the retrieved resource or null when the resource wasn't found
   * @throws IOException when IO error occurs during retrieval of the resource
   */
  static File retrieveResourceAsFile(String resource, Class clazz = null) throws IOException {
    log.debug("Retrieving resource " + resource)
    URL resourceURL = null
    if (clazz == null) {
      ClassLoader tccl = Thread.currentThread().getContextClassLoader()
      resourceURL = tccl.getResource(resource)
    } else {
      resourceURL = clazz.getResource(resource)
    }

    if (resourceURL == null) {
      log.warn("Resource ${resource} not found on classpath.")
      return null
    }

    File destFile = new File(platform.tmpDir, new File(resource).getName())

    destFile.withOutputStream { out ->
      out << resourceURL.openStream()
    }

    return destFile;
  }

  /**
   * Copies resource from classpath (even JAR) to the specified File. Sudo is used if necessary.
   * @param resource path to resource
   * @param clazz class which should be used as a context for looking for the resource, if not specified Thread.currentThread().getContextClassLoader() is used
   * @param directory destination
   * @throws IOException when IO error occurs during retrieval of the resource
   */
  static void copyResourceTo(final String resource, final File directory, final Class clazz = null) throws IOException {
    File resourceFound = retrieveResourceAsFile(resource, clazz)
    if (resourceFound == null) {
      throw IOException("Unable to find resource '$resource'")
    }
    if (!JBFile.copy(resourceFound, directory)) {
      throw IOException("Unable to copy resource '$resource' into directory '$directory'")
    }
  }

  /**
   * Application root path
   *
   * Do not change with basedir (basedir = WORKSPACE root)!
   *
   */
  static String getRootPath() {
    def rootPath = Library.getUniversalProperty("project.root.path", '..')
    return new File(rootPath).getCanonicalPath()
  }

  /**
   * Get root dir of tools for applications
   */
  static String getAppToolsPath() {
    return getUniversalProperty("app.tools.path", Hudson.staticDir + "${platform.sep}noe${platform.sep}tools")
  }

  /**
   * Sleep for a defined time.
   * It has sense to change waiting factor globally - we working on different machines
   */
  static void letsSleep(long baseTime) {
    def speedFactor = Double.valueOf(Library.getUniversalProperty('machine.speed.factor', '1'))
    sleep((long) (baseTime * speedFactor))
  }

  /**
   * Change privileges for whole directory or for file.
   * chmod for destination(file or directory) in directory
   * @deprecated use {@link JBFile#chmod}
   */
  static void chmod(String destination, String permissions, String directory = ".") {
    if (!platform.isWindows()) {
      //      File destinationFile = new File(destination)
      log.trace("Changing permissions to ${permissions} for ${destination} in ${directory}")
      def ret = Cmd.executeSudoCommand(["chmod", "-R", permissions, destination], new File(directory))
      if (ret != 0) {
        throw new RuntimeException("Library.chmod sudo chmod -R ${permissions} ${destination} in ${directory} retrurned: ${ret}")
      }
    } else {
      log.trace("This is MS Windows, Library.chmod is not needed")
    }
  }

  /**
   *
   * @param url
   * @param code = 200
   * @param content = ""
   * @param timeout = 30000
   * @param allowRedirects = true
   * @param setReqProp = false
   * @param reqKey = ""
   * @param reqValue = ""
   * @param contentAsRegex if true, the content of @param content is considered to be regex
   * @return boolean
   * @deprecated, use {@link VerifyURLBuilder}
   * TODO: Refactor usages to use VerifyURLBuilder.
   */
  static boolean verifyUrl(URL url, int code = 200, String content = "", long timeout = 30000, boolean allowRedirects = true,
                               boolean setReqProp = false, String reqKey = "", String reqValue = "",
                               WebClient webClient = null, boolean clearWebClientCache = false, boolean reThrowAnyException = false,
                               boolean contentAsRegex = false) {
    return VerifyURLBuilder.verifyURL {
      it.url url
      it.code code
      it.content content
      it.timeout timeout
      it.allowRedirects allowRedirects
      it.setReqProp setReqProp
      it.reqKey reqKey
      it.reqValue reqValue
      it.webClient webClient
      it.clearWebClientCache clearWebClientCache
      it.reThrowAnyException reThrowAnyException
      it.contentAsRegex contentAsRegex
    }
  }

  /**
   * Returns content from the URL if there was a successful request was made (returned HTTP code 200) in specified timeout
   */
  static String retrieveURLContent(URL url, WebClient webClient = null, long timeout = 30000, boolean allowRedirects = true, List<Integer> rCode = [200, 201, 202, 203],
                                       boolean setReqProp = false, String reqKey = "", String reqValue = "", boolean clearWebClientCache = true) {
    return VerifyURLBuilder.getContent {
      it.url url
      it.allowedCodes rCode
      it.timeout timeout
      it.allowRedirects allowRedirects
      it.setReqProp setReqProp
      it.reqKey reqKey
      it.reqValue reqValue
      it.webClient webClient
      it.clearWebClientCache clearWebClientCache
    }
  }

  /**
   * Downloading file
   */
  static void downloadFile(String url, File filePath) {
    log.debug("Downloading file from ${url} to ${filePath}")
    def file = new FileOutputStream(filePath)
    def out = new BufferedOutputStream(file)
    out << new URL(url).openStream()
    out.close()
  }

  /**
   * Returns JSESSIONID from the URL if there was a successful request made (returned HTTP code 200) in specified timeout
   */
  static String retrieveURLJsessionId(URL url, WebClient webClient = null, long timeout = 30000, boolean allowRedirects = true) {
    if (webClient == null) {
      webClient = new WebClient()
    }
    webClient.getOptions().setThrowExceptionOnFailingStatusCode(false)
    webClient.getOptions().setRedirectEnabled(allowRedirects)
    webClient.cache.clear()
    def respCode = 0
    def jSessionId = null
    while ((respCode != 200) && (timeout > 0)) {
      timeout += new Date().getTime() // for exact timing
      Page page = webClient.getPage(url)
      WebResponse webResponse = page.webResponse
      respCode = webResponse.statusCode
      log.debug("Trying to retrieve JSESSIONID from ${url}, response code = ${respCode}")
      if (respCode == 200) {
        jSessionId = getJSessionId(webClient)
        log.trace("Retrieved JSESSIONID = ${jSessionId}")
        break
      }
      Library.letsSleep(1000)
      timeout -= new Date().getTime() // for exact timing
    }
    return jSessionId
  }

  /**
   * Returns HTTP status (response) code by sending a request to specified URL
   */
  static int getResponseCode(URL url, WebClient webClient = null, boolean allowRedirects = true) {
    if (webClient == null) {
      webClient = new WebClient()
    }
    webClient.getOptions().setThrowExceptionOnFailingStatusCode(false)
    webClient.getOptions().setRedirectEnabled(allowRedirects)
    Page page = webClient.getPage(url)
    WebResponse webResponse = page.webResponse
    if (!webClient.getCookieManager().isCookiesEnabled() && webClient instanceof NOEWebClient) {
      def newJsessionId = webResponse.getResponseHeaderValue("Set-Cookie")
      log.debug("Cookies disabled... newJsessionId: ${newJsessionId}")
      try {
        (webClient as NOEWebClient).setMyCurrentJSessionID((newJsessionId =~ /^JSESSIONID=([^;]*);.*/)[0][1])
      } catch (IndexOutOfBoundsException ex) {
        log.warn("I can't parse newJsessionId from this:" + newJsessionId)
      }
    }
    return webResponse.statusCode
  }


  static String getJSessionId(WebClient webClient) {
    log.trace("-- Entering getJSessionId(WebClient webClient = ${webClient})")
    if (webClient == null) {
      log.debug("WebClient is null, returning.")
      return null
    }
    CookieManager cookieManager = webClient.getCookieManager()
    if (cookieManager.isCookiesEnabled()) {
      Cookie cookie = cookieManager.getCookie(DefaultProperties.JSESSIONID)
      if (cookie != null) {
        log.trace("--- Cookie name:${cookie.getName()} cookie value: ${cookie.getValue()}")
        return cookie.getValue()
      } else {
        cookieManager.cookies.each { onecookie ->
          log.trace("--- Cookie name:${onecookie.getName()} cookie value: ${onecookie.getValue()}")
        }
        log.trace("--- Hmm, no cookies?: ${cookieManager.cookies}")
        return null
      }
    } else {
      /**
       null if unknown, @see noe.common.NOEWebClient
       **/
      ((NOEWebClient)webClient).getMyCurrentJSessionID()
    }
  }


  static List<File> isFileExists(List files, path) {
    def res = []

    new File(path).eachFileRecurse { File file ->
      if (files.contains(file.name)) res << file
    }

    return res
  }

  static Map getHttpHeaders(URL url) {
    def conn = url.openConnection()
    def headers = conn.getHeaderFields()
    def result = [:]
    def list = []
    StringBuilder builder = null
    headers.keySet().each { key ->
      list = headers.get(key)
      builder = new StringBuilder()
      list.each { param ->
        builder.append(param)
        builder.append(' ')
      }
      result.put(key, builder.toString())
    }
    log.trace("HTTP Headers: {}", result)

    return result
  }

  /**
   * Handle JSVC command creation.
   */
  static String getJsvcCommand(String bootstrapClass, String classPath, String outFile, String errFile, String pidFile, Map params) {
    def jsvc = params["jsvc"] ?: "jsvc"
    def endorsed = params["endorsed"] ? "-Djava.endorsed.dirs=" + params["endorsed"] : ""
    def procName = params["procname"] ? "-procname " + params["procname"] : ""
    def stop = params["stop"] ? "-stop" : ""
    def debug = params["debug"] ? "-debug" : ""
    def user = params["user"] ? "-user " + params["user"] : ""
    def extraOptions = params["extraJavaOptions"] ? Cmd.mapToCommandLine(params["extraJavaOptions"]) : ""

    // add -debug for debug output -verbose:jni
    def command = "${jsvc} ${stop} ${debug} -showversion -cp ${classPath} ${endorsed} -outfile $outFile -errfile $errFile -pidfile $pidFile  $procName $user $extraOptions $bootstrapClass"
    return command
  }

  static boolean isSymlink(String filePath) {
    return !((new File(filePath).getCanonicalPath()).equals(new File(filePath).getAbsolutePath()))
  }

  /**
   * @deprecated Use {@link Net#getHostname()} instead
   */
  static String getHostname() {
    return Net.getHostname()
  }

  static String getHostIpAddress() {
    String hostIpAddress = DefaultProperties.HOST
    try {
      InetAddress addr = InetAddress.getByName(hostIpAddress)
      // Get host addresss
      hostIpAddress = addr.getHostAddress()
    } catch (UnknownHostException e) {
      log.warn("Hostname of ${hostIpAddress} wasn't found")
    }
    log.trace("getHostIpAddress() returns ${hostIpAddress}")
    return hostIpAddress
  }

  static String getIPAdress(String hostname) {
    def ipaddress
    try {
      // Get IP address from hostname
      InetAddress addr = InetAddress.getByName(hostname)
      ipaddress = addr.getHostAddress()
    } catch (UnknownHostException e) {
      log.warn("Hostname of ${hostname} wasn't found")
    }
    log.trace("Resolved IP address: hostname: ${hostname}, ipaddress: ${ipaddress}")
    return ipaddress
  }

  /**
   * Get all declared fields (even from parents).
   * Used for identification of readonly properties (Groovy stuff).
   */
  static List getDeclaredFieldsAll(Class clazz, boolean ignoreInternal = true) {
    def fieldNames = []
    def name = ''

    // Extract properties from parent.
    clazz.getDeclaredFields().each {
      name = it.getName()
      if (!((name.startsWith('$') || name.startsWith('__') || name == 'metaClass') && ignoreInternal)) {
        fieldNames << it.getName()
      }
    }

    // Class has not explicit parent.
    if (clazz.getSuperclass().getCanonicalName() != 'java.lang.Object') {
      fieldNames += getDeclaredFieldsAll(clazz.getSuperclass(), ignoreInternal)
    }

    return fieldNames
  }

  /**
   * Check whether firewall is enabled, currently implemented only for Windows
   * @return true if the currently active profile in firewall is in enabled state, false otherwise
   * @throws IOException if detection of enabled firewall fails due failure in running external command
   */
  static boolean isFirewallEnabled() throws IOException {
    if (platform.isWindows()) {
      Map res = Cmd.executeCommandConsumeStreams(["netsh", "advfirewall", "show", "currentprofile", "state"])
      if (res.exitValue != 0) {
        throw new IOException("Detection of firewall state failed with this error: " + res.stdErr)
      } else {
        String out = res.stdOut
        log.debug("Detecting state of firewall from this output:\n{}", out)
        return out.matches(Pattern.compile(".*State\\s+ON.*", Pattern.DOTALL))
      }
    }
  }

  static switchWindowsFirewall(boolean on) {
    if (platform.isWindows()) {
      if (on) {
        return Cmd.executeCommand(["netsh", "advfirewall", "set", "currentprofile", "state", "on"], new File('.'))
      } else {
        return Cmd.executeCommand(["netsh", "advfirewall", "set", "currentprofile", "state", "off"], new File('.'))
      }
    }
  }

  static String getMavenExecutable() {
    if (platform.isWindows()) {
      try {
        Cmd.executeCommandConsumeStreams(["mvn.cmd", "-v"])
        return 'mvn.cmd'
      } catch (IOException ignored) {}

      try {
        Cmd.executeCommandConsumeStreams(["mvn.bat", "-v"])
        return 'mvn.bat'
      } catch (IOException ignored) {}

      throw new FileNotFoundException("Maven not found, ending.")
    } else {
      return 'mvn'
    }
  }

  static void logRunningProcessesAndPorts() {
    log.debug("Printing running processes + opened ports")
    log.debug('------------------------------------------------')
    log.debug('PROCESS LIST')
    log.debug('------------------------------------------------')
    Cmd.logSystemProcesses(new Platform().actualUser)
    log.debug('------------------------------------------------')
    log.debug('OPENED PORTS')
    log.debug('------------------------------------------------')
    Cmd.logSystemOpenedPorts()
    log.debug('------------------------------------------------')
  }

  /**
   * Puts the groovy and ant related jar files to lib dir if they are already not there and creates classpath string
   *
   * @param libDir location to put the jar files if not specified, the default location is used which is new File(Library.getRootPath(), "lib")
   * @return generated classpath for groovy with ant
   */
  static String groovyWithAntAsClasspath(File libDir = null) {
    def groovyAndAntLibs = ["groovy-3.0.9.jar", "groovy-ant-3.0.9.jar", "ant-1.9.2.jar", "ant-launcher-1.9.2.jar"]
    def libDirToUse = (libDir) ?: new File(Library.getRootPath(), "lib")
    def classpath = []
    if (!libDirToUse.exists()) {
      JBFile.mkdir(libDirToUse)
    }
    groovyAndAntLibs.each { libName ->
      if (!new File(libDirToUse, libName).exists()) {
        JBFile.move(Library.retrieveResourceAsFile("lib/${libName}"), libDirToUse)
      }
      classpath.add(new File(libDirToUse, libName).absolutePath)
    }

    return classpath.join(platform.pathsep)
  }

  /**
   * Wait until condition is satisfied
   * @param time amount of time to wait
   * @param unit time unit to wait
   * @param condition condition for which the waiting will be done
   * @return true if condition was satisfied and it was waited lower amount of time than provided, false otherwise
   */
  static boolean waitForCondition(long time, TimeUnit unit, Closure<Boolean> condition) {
    final long start = new Date().getTime()
    final long timeout = unit.toMillis(time)
    while (new Date().getTime() - start < timeout) {
      if (condition()) {
        return true
      }
      sleep(500)
    }
    return false
  }

  /**
   * Measure time which took the closure to execute
   */
  static Closure<Long> measureTime = { closure ->
    long start = System.currentTimeMillis()
    closure.call()
    long now = System.currentTimeMillis()
    now - start
  }

}
