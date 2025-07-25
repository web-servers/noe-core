package noe.common

import groovy.util.logging.Slf4j
import noe.common.utils.Platform
import noe.server.ServerController
import noe.workspace.IWorkspace
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestName

@Slf4j
class TestAbstract {

  @Rule
  public TestName name = new TestName()
  public static ServerController serverController = ServerController.getInstance()
  static IWorkspace workspace /// Must be created in a children, see TestSuite
  Platform platform = new Platform()

  private static ByteArrayOutputStream originalProperties = null;

  public static void loadTestProperties(String testProperties = null) {
    if (testProperties) {
      originalProperties = new ByteArrayOutputStream()
      InputStream propertiesStream = null

      try {
        System.getProperties().store(originalProperties, "backup")
        propertiesStream = TestAbstract.class.getResourceAsStream(testProperties)

        if (!propertiesStream) {
          log.error("Expected properties file ${testProperties} doesn't exist.")
          throw new FileNotFoundException("Expected properties file '${testProperties}' doesn't exist.")
        }

        System.getProperties().load(propertiesStream)
        serverController = ServerController.getInstance() // we need to reset serverController as context might change

      } catch (Exception e) {
        log.error("Could not load properties file ${testProperties}: ${e.message}")
      } finally {
        if (propertiesStream) {
          try {
            propertiesStream.close()
          } catch (IOException ignored) {}
        }
      }
    }
  }


  @Before
  public void prepare() {
    serverController.backup()
    serverController.backupConfsAll()
    serverController.killAll()
    serverController.killAllInSystem()
  }

  @After
  public void cleanup() {
    serverController.killAll()
    serverController.archiveLogsAll(this.getClass().canonicalName + platform.sep + name.getMethodName())
    serverController.archiveConfsAll(this.getClass().canonicalName + platform.sep + name.getMethodName())
    serverController.cleanLogsAll()
    serverController.restoreConfsAll()
    // all servers into default state (BUT not workspace state - needs work), maybe could be enough to call it in preMethod
    serverController.refreshAll()
  }

  @AfterClass
  public static void destroyWorkspace() {
    if (workspace != null) {
      workspace.destroy()
      workspace = null
    }
    if (originalProperties != null) {
      restoreSysProps()
      originalProperties = null
    }
  }

  private static void restoreSysProps() {
    if (originalProperties != null) {
      StringReader reader = new StringReader(originalProperties.toString('ISO-8859-1'))
      System.getProperties().load(reader)
    }
  }
}
