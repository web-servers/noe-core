package noe.eap.server.as5

import noe.common.DefaultProperties
import noe.common.utils.Library

class AS5Properties {
  def static final USE_AS5_SECURED = Boolean.parseBoolean(Library.getUniversalProperty('as5.secured', 'false'))
  def static final PROFILE = Library.getUniversalProperty('as5.profile', 'production')


  def static final MAIN_HTTP_PORT_DEFAULT = 8080
  def static final MAIN_HTTP_PORT = Library.getUniversalProperty('as5.main.http.port', MAIN_HTTP_PORT_DEFAULT).toInteger()
  def static final MAIN_HTTPS_PORT_DEFAULT = 8443
  def static final MAIN_HTTPS_PORT = Library.getUniversalProperty('as5.main.https.port', MAIN_HTTPS_PORT_DEFAULT).toInteger()
  def static final AJP_PORT_DEFAULT = 8009
  def static final AJP_PORT = Library.getUniversalProperty('as5.main.ajp.port', AJP_PORT_DEFAULT).toInteger()
  def static final JNDI_PORT_DEFAULT = 1099
  def static final JNDI_PORT = Library.getUniversalProperty('as5.main.jndi.port', JNDI_PORT_DEFAULT).toInteger()

  def static final PUBLIC_IP_ADDRESS = Library.getUniversalProperty('as5.public.ip.address', DefaultProperties.HOST)
  def static final UDP_MCAST_ADDRESS = Library.getUniversalProperty('as5.udp.mcast.address', '230.0.0.4')
  def static final DIAGNOSTICS_MCAST_ADDRESS = Library.getUniversalProperty('as5.diagnostics.mcast.address', '224.0.75.75')

  def static final HOT_DEPLOYMENT_SCAN_PERIOD = Integer.parseInt(Library.getUniversalProperty('as5.hot.deployment.scan.period', "2500"))
  def static final NATIVES_DIR_NAME = 'eap-natives'
  def static final JMX_USER = "noe"
  def static final JMX_PASSWORD = "noe"
  def static final JMX_ROLES = "JBossAdmin,HttpInvoker"
}
