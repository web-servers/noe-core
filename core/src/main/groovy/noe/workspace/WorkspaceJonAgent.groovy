package noe.workspace

import groovy.util.logging.Slf4j
import noe.common.utils.Library
import noe.jon.server.JonAgent
import noe.jon.utils.JonUtils

@Slf4j
class WorkspaceJonAgent extends WorkspaceAbstract {
  String jonAgentJar = Library.getUniversalProperty('jon.agent.jar', '')
  String jonAgentIp = Library.getUniversalProperty('jon.agent.ip', '')
  String jonServerIp = Library.getUniversalProperty('jon.server.ip', '')
  String jonServerPort = Library.getUniversalProperty('jon.server.port', '7080')
  def jonAgentPort  /// Port is created dynamically by agent - see JonUtils.installJonAgent(...)
  String jonAgentName  /// Port is created dynamically by agent - see JonUtils.installJonAgent(...)
  String jonAgentSettingsPath
  JonUtils jonUtils

  WorkspaceJonAgent(String serverId = 'jon-agent') {
    this.jonUtils = new JonUtils(basedir, WorkspaceAbstract.ant, WorkspaceAbstract.platform)
    def agentInfo = [:]
    agentInfo = this.jonUtils.installJonAgent(jonAgentJar, jonAgentIp, jonServerIp, jonServerPort)

    def final outgoingCommandTrace = '''
         <category name="org.rhq.enterprise.communications.command.client.OutgoingCommandTrace">
            <priority value="TRACE"/>
            <appender-ref ref="COMMANDTRACE"/>
         </category>
         '''

    JonAgent jonAgentServer = new JonAgent(
        this.jonUtils.getInstallJonAgentJonDirectory() + "${WorkspaceAbstract.platform.sep}rhq-agent",
        '',
        agentInfo['name'],
        agentInfo['port'],
        agentInfo['settingsPath'])
    jonAgentServer.host = jonAgentIp
    jonAgentServer.updateLogCategory(outgoingCommandTrace)

    serverController.addServer(serverId, jonAgentServer)

    jonAgentName = agentInfo['name']
    jonAgentPort = agentInfo['port']
    jonAgentSettingsPath = agentInfo['settingsPath']
  }
}
