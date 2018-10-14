package noe.jon.server

import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.GPathResult
import groovy.xml.XmlUtil
import noe.common.newcmd.CmdBuilder
import noe.common.newcmd.CmdCommand
import noe.common.utils.Cmd
import noe.common.utils.IO
import noe.common.utils.JBFile
import noe.common.utils.Library
import noe.common.utils.XmlUtils
import noe.server.ServerAbstract

@Slf4j
class JonAgent extends ServerAbstract {

  String name ///Jon agent name
  String jonAgentSettingsPath

  JonAgent(String basedir, version, String name, Integer port, String jonAgentSettingsPath) {
    super(basedir, version)

    this.binDir = this.basedir + "${platform.sep}bin"
    this.configDirs = ["conf"]
    this.logDirs = ["logs"]

    this.mainHttpPort = port
    this.binPath = "${platform.sep}bin"
    //this.start = ['./rhq-agent.' + platform.getScriptSuffix(), "--input=${jonAgentSettingsPath}", '--nonative', "--pref=${name}"]
    this.start = ["${getBinDirFullPath()}${platform.sep}rhq-agent." + platform.getScriptSuffix(), "--input=${jonAgentSettingsPath}", "--pref=${name}"]

    this.name = name
    this.jonAgentSettingsPath = jonAgentSettingsPath
    if (platform.isWindows()) {
      this.processCode = 'Administrator:  RHQ Agent'
    } else {
      this.processCode = 'rhq-agent'
    }
  }

  void killAllInSystem() {
    Cmd.killAllInSystem(["rhq-agent", processCode])
  }

  /**
   * Start the server
   */
  @Override
  void start(Map conf = [:]) {
    log.info('Starting Jon agent {}', serverId)

    //portsAvailable()
    serverCustomization(conf)


    CmdCommand startCmd = new CmdBuilder<>(this.start).setWorkDir(new File(getBinDirFullPath())).build()
    log.info('Start command: {}', startCmd)

    process = Cmd.startProcess(startCmd)
    process.consumeProcessOutput(System.out, System.err)

    waitForStartComplete(120)
    waitUntilAgentIsRegisteredWithServer(120)
    // wait for native enable and discovery - 15s
    Library.letsSleep(15000)

    this.pid = extractPid()
  }

  /**
   * Stop the server
   */
  long stop(Map conf = [:]) {
    log.info('Stopping Jon agent {}', serverId)
    if (!isRunning()) {
      log.debug("Jon agent is already down.")
    } else {
      this.killTree()
      waitForShutdownComplete(30)
      log.debug("--- Jon agent stopped ---'")
    }
    return -1
  }

  @Override
  void updateConfSetBindAddress(String address) {
    throw new RuntimeException("TODO rework concept of what is abstract in serverAbstract")
  }

  @Override
  void shiftPorts(int offset) {
    throw new RuntimeException("TODO rework concept of what is abstract in serverAbstract")
  }

  void updateLogCategory(String categoryNodeAsString) {
    log.debug("Adding category ${categoryNodeAsString} to log4j.xml configuration of jog agent")

    configDirs.each { confDir ->
      def log4jXmlFile = new File(this.basedir, "${confDir}${platform.sep}log4j.xml")
      if (log4jXmlFile.exists()) {
        XmlSlurper xmlParser = new XmlSlurper(false, false, true)
        xmlParser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        def categoryNode = xmlParser.parseText(categoryNodeAsString)
        GPathResult xml = xmlParser.parse(log4jXmlFile)
        GPathResult category = xml.category.find { it.@name == categoryNode.@name }
        IO.handleOutput("Old category ${XmlUtil.serialize(category)}, new category ${XmlUtil.serialize(categoryNode)}", IO.LOG_LEVEL_FINER)
        if (category.isEmpty()) {
          xml.appendNode categoryNode
        } else {
          category.replaceBody categoryNode.children()
        }
        XmlUtils.writeXmlToFile(log4jXmlFile, xml)
      }
    }
  }

  /**
   * Waits for agent to get registered with server. Done by scanning log
   */
  void waitUntilAgentIsRegisteredWithServer(int timeout = 120) {
    logDirs.each { logDir ->
      def logFile = new File(this.basedir, "${logDir}${platform.sep}agent.log")
      if (logFile.exists()) {
        def serverRegisteredRegexp = /.*Agent has successfully registered with the server.*/
        if (!JBFile.waitUntilFileContains(logFile, serverRegisteredRegexp, timeout)) {
          stop()
          kill()
          throw new RuntimeException("Agent failed to be registered with the server in specified timeout ${timeout} seconds")
        }
      }
    }
  }
}
