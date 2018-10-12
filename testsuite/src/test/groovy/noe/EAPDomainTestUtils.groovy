package noe

import noe.common.utils.VerifyURLBuilder
import noe.server.AS7Domain
import org.junit.Assert


class EAPDomainTestUtils {
  public static void shiftPortsTest(AS7Domain server) {
    int origPortOffset = server.portOffset
    try {
      server.shiftPorts(102)
      Assert.assertFalse("Server ${server.serverId} shouldn't be running before starting the test", server.isRunning())
      server.start()
      Assert.assertTrue("Server ${server.serverId} should be running, but isn't", server.isRunning())

      ['server-one', 'server-two'].each {
        serverName ->
          int serverPort = server.retrieveDomainServerPorts(serverName)['mainHttpPort']
          URL serverUrl = new URL("http://${server.host}:${serverPort}/")
          Assert.assertTrue("${serverName} should be running on ${serverUrl}",
              VerifyURLBuilder.verifyURL {
                url serverUrl
                code 200
                swallowIOExceptions true
              }
          )
      }
    } finally {
      server.killAllInSystem()
      if (server.portOffset != origPortOffset) {
        server.shiftPorts(origPortOffset - server.portOffset)
      }
    }
  }
}
