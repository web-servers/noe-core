package noe.server

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.utils.Cmd
import noe.common.utils.Library
/**
 * TODO
 * - features tested on IIS 6
 *  - Cmd.executeCommand("ICACLS ${serverDir}\\modules\\native\\sbin\\isapi_redirect.dll /grant \"IIS_IUSRS\":F", new File(basedir))
 *    Cmd.executeCommand("ICACLS ${serverDir}\\modules\\native\\sbin\\isapi_redirect.properties /grant \"IIS_IUSRS\":F", new File(basedir))
 *    Cmd.executeCommand("ICACLS ${serverDir}\\modules\\native\\sbin /grant \"IIS_IUSRS\":F /t", new File(basedir))
 *    Cmd.executeCommand("ICACLS ${isapiConfPath} /grant \"IIS_IUSRS\":F /t", new File(basedir))
 *    As part od preparation phase
 *  - Why not use default test site - for better works with basedir
 *    c:\inetpub\wwwroot
 */

/**
 * @author Jan Stefl <jstefl@redhat.com>
 */
@Slf4j
class IIS extends ServerAbstract {

  String windir /// Where is Windows installed
  String defaultVdirPhysicalPath
  /// Default path for virtual directory (in our test is enough one vdir standardly named jboss), this value should be set during the test (.../jbosseap/modules/natives/sbin)

  // appcmd commands 
  Map commandIsapiCgiRestriction = ['path': defaultVdirPhysicalPath + '\\isapi_redirect.dll', 'description': 'jboss', 'allowed': 'true']
  Map commandIsapiFilters = ['path': defaultVdirPhysicalPath + '\\isapi_redirect.dll', 'name': 'jboss', 'enabled': 'true']
  Map commandHandlersAccessPolicy = ['accessPolicy': 'Read,Script,Execute']

  IIS(String basedir, version) {
    super(basedir, version)

    this.start = ['net', 'start', 'w3svc']
    this.stop = 'net stop w3svc'
    this.mainHttpPort = 80
    // this.shutdownPort = 443
    // this.mainHttpsPort = 443
    // TODO how to indetify process?
    this.processCode = ''

    windir = Library.getUniversalProperty('WINDIR')
  }

  /**
   * Wait until the server is started.
   */
  void waitForStartComplete() {
    super.waitForStartComplete()
    Library.verifyUrl(getUrl(), 200, "welcome.png")
  }

  /**
   * Create new server instance - from ref. tomcat installation.
   */
  ServerAbstract createNewServerInstance(String id, int offset = DefaultProperties.DEFAULT_SHIFT_PORT_OFFSET) {
    throw new UnsupportedOperationException("We don't support creation of new IIS instance.")
  }

  void killAllInSystem() {
    Cmd.killAllInSystem(["w3wp"])
  }

  /**
   * Physical removing of actual server instance.
   */
  boolean deleteCurrentInstance() {
    throw new UnsupportedOperationException("We don't support deletion of new IIS instance.")
  }

  /**
   * Bing server on address
   *   TODO implement updateConfSetBindAddress(address)
   *   TODO doc for address format (regarding IPv6)
   */
  void updateConfSetBindAddress(String address) {
    // !! http://www.iis.net/configreference/system.applicationhost/sites/site/bindings/binding
    // IIS 7: appcmd set site /site.name: contoso /+bindings.[protocol='https',bindingInformation='*:443:']
    // http://technet.microsoft.com/en-us/library/cc731692(v=ws.10).aspx
    throw new UnsupportedOperationException("TODO: Implementation needed.")
  }

  /**
   * Shift ports about offset.
   */
  void shiftPorts(int offset = DefaultProperties.DEFAULT_SHIFT_PORT_OFFSET) {
    throw new UnsupportedOperationException("We don't support shif ports for IIS (it is needed? -> jstefl).")
  }

  /**
   * Instruct IIS to perform command.
   */
  int executeAppcmd(String command) {
    def sep = platform.sep
    return Cmd.executeCommand("${windir}${sep}system32${sep}inetsrv${sep}appcmd " + command, new File('.'))
  }

  /**
   * Create new virtual directory to the website.
   */
  int createVirtualDirectory(String physicalPath, String path = 'jboss', String webSite = 'Default Web Site') {
    return executeAppcmd("add vdir /app.name:\"Default Web Site\" /path:/${path} /physicalPath:\"${physicalPath}\"")
  }

  /**
   * Delete virtual directory from the website.
   */
  int deleteVirtualDirectory(path = 'jboss', webSite = 'Default Web Site') {
    return executeAppcmd("delete vdir \"${webSite}/${path}\"")
  }

  /**
   * Set Cgi restrictions
   * @param section
   *   isapiCgiRestriction
   *   isapiFilters
   *   handlers
   *   ...
   *
   * @param properties
   *   path: ' ... /isapi_redirect.dll',
   *   description: 'jboss',
   *   allowed: 'true'
   *
   * @param op
   *   + add 
   *   - remove
   *
   */
  int setConfig(String section, Map properties, op = '+') {
    StringBuilder propertiesString = new StringBuilder()
    properties.each { property ->
      propertiesString << "'" + property + '",'
    }

    // remove last ","
    String props = propertiesString.substring(0, propertiesString.length() - 2).toString()

    return executeAppcmd("set config /section:${section} /${op}\"[${props}]\"")
  }

  /**
   * Allow execute isapi_redirect.
   */
  int addIsapiCgiRestriction() {
    return setConfig('isapiCgiRestriction', commandIsapiCgiRestriction)
  }

  /**
   * Disallow execute isapi_redirect.
   */
  int removeIsapiCgiRestriction() {
    return setConfig('isapiCgiRestriction', commandIsapiCgiRestriction, '-')
  }

  /**
   * Enable isapi_redirect filter.
   */
  int addIsapiFilters() {
    return setConfig('isapiFilters', commandIsapiFilters)
  }

  /**
   * Disable isapi_redirect filter.
   */
  int removeIsapiFilters() {
    return setConfig('handlers', commandIsapiFilters, '-')
  }

  /**
   * grants read / scrip / execute
   */
  int addHandlersAccessPolicy() {
    return setConfig('handlers', commandHandlersAccessPolicy)
  }

  /**
   * removes granted read / scrip / execute
   */
  int removeHandlersAccessPolicy() {
    return setConfig('handlers', commandHandlersAccessPolicy, '-')
  }

}
