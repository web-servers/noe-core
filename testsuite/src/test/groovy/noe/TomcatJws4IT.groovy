package noe

import groovy.util.logging.Slf4j
import noe.common.TestAbstract
import noe.common.DefaultProperties
import noe.common.utils.Java
import noe.common.utils.Platform
import noe.workspace.ServersWorkspace
import noe.workspace.WorkspaceTomcat
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test

@Slf4j
class TomcatJws4IT extends TestAbstract {


  @BeforeClass
  public static void beforeClass() {
    Platform platform = new Platform()
    Assume.assumeFalse("JWS is not supported on HP-UX => skipping", platform.isHP())
    Assume.assumeTrue("JWS 4 is not supported on RHEL8+ => skipping", platform.isRHEL7())
    Assume.assumeTrue("Tomcat from JWS 4 requires at least Java 1.8",
            (DefaultProperties.SERVER_JAVA_HOME?.contains('jdk1.8')) ?: Java.isJdkXOrHigher('1.8')
    )

    Assume.assumeTrue("We have currently only builds for RHEL, skipping for other platforms",
            platform.isRHEL())
    loadTestProperties('/jws4-test.properties')
    workspace = new ServersWorkspace(
            new WorkspaceTomcat()
    );
    workspace.prepare();
  }


  @Test
  void serverStartStopTest() {
    serverController.getServerIds().each { serverId ->
      SingleServerTestUtils.serverStartStopTest(serverId)
    }
  }

  @Test
  void serverStartKillTest() {
    serverController.getServerIds().each { serverId ->
      SingleServerTestUtils.serverStartKillTest(serverId)
    }
  }

  @Test
  void killAllInSystemTest() {
    serverController.getServerIds().each { serverId ->
      SingleServerTestUtils.killAllInSystemTest(serverId)
    }
  }

}
