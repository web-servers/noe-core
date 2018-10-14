package noe.server

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.newcmd.CmdBuilder
import noe.common.utils.Cmd
import noe.server.provider.OpensslServerBuilder
import noe.server.provider.OpensslServerProvider
/**
 * Openssl server allowing to easily setup openssl as server and start/stop/kill it
 */
@Slf4j
class OpensslServer extends ServerAbstract {

  private final String openssl
  static final String defaultServerId = "openssl"

  OpensslServer(OpensslServerBuilder builder) {
    super(builder.baseDir, null)
    this.host = 'localhost' // openssl s_server listens to all IPs and it is not configurable
    this.mainHttpsPort = builder.port
    this.mainHttpPort = builder.port
    this.configDirs = []
    this.logDirs = []
    this.openssl = builder.openssl
    this.sslKey = builder.serverKey
    this.sslCertificate = builder.serverCert
    this.sslKeystorePassword = builder.keyPassword
    this.libDir = builder.libDir?.absolutePath
    this.binDir = builder.binDir?.absolutePath
  }

  @Override
  void setDefault() {
    this.processCode = 'openssl'
  }

  @Override
  void start(Map conf = [:]) {
    startWithParams([])
  }

  /**
   * Starts the server with additional params and prints the process output to the provided output and error streams
   * Params as port, server key, server cert and password are taken from server configuration,
   * the additional params is meant for any other params wanted, such as cipher or protocol setting of the server.
   */
  void startWithParams(List<String> additionalParams, OutputStream outputStream = System.out, OutputStream errorStream = System.err) {
    List start = [openssl, 's_server', '-accept', mainHttpsPort]
    if (sslKey) start.addAll(['-key', sslKey])
    if (sslCertificate) start.addAll(['-cert', sslCertificate])
    if (sslKeystorePassword) start.addAll(['-pass', sslKeystorePassword])
    if (additionalParams) start.addAll(additionalParams)
    File targetDir = new File('.')
    if (new File(openssl).isAbsolute()) {
      targetDir = new File(openssl).parentFile
    }
    this.process = Cmd.startProcess(new CmdBuilder<>(start).setWorkDir(targetDir).build())
    this.process.consumeProcessOutput(outputStream, errorStream)
    waitForStartComplete()
  }

  @Override
  long stop(Map conf = [:]) {
    long startTime = System.currentTimeMillis()
    Cmd.stopProcess(process, 'openssl')
    return System.currentTimeMillis() - startTime
  }

  @Override
  void updateConfSetBindAddress(String address) {
    log.error("Openssl s_server doesn't support changing of its IP address it gets bound to, it always listens to all IPs")
  }

  @Override
  void shiftPorts(int offset) {
    this.mainHttpPort += offset
    this.mainHttpsPort += offset
  }

  @Override
  void killAllInSystem() {
    Cmd.killAllInSystem(['openssl'])
  }

  static String getDefaultServerId() {
    return defaultServerId
  }

  /**
   * You can get instance of the Openssl server via this method (it is platform aware and creates appropriate
   * instance of the OpensslServer based on system setting).
   * @param basedir workspace basedir where the root folder of Openssl is located
   * @return instance of the Openssl server
   */
  static OpensslServer getInstance(String basedir) {
    OpensslServer server
    if (DefaultProperties.jbcsOpenSslVersion()) {
      server = OpensslServerProvider.coreServices(new File(basedir))
    } else {
      server = OpensslServerProvider.default()
    }
    return server
  }

}
