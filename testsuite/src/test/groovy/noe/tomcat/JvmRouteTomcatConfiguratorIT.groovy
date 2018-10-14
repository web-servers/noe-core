package noe.tomcat


import noe.tomcat.configure.TomcatConfigurator
import org.junit.Test

import static org.junit.Assert.assertTrue


/**
 * Abstract class for JVM route testing.
 *
 * Needs to be extended and implemented following method:
 *
 * <code>
 *   @BeforeClass
 *   public static void beforeClass() {
 *     loadTestProperties("/tomcatX.properties")
 *     prepareWorkspace()
 *   }
 * </code>
 */
abstract class JvmRouteTomcatConfiguratorIT extends TomcatTestAbstract {

  @Test
  void setJvmRouteDefaultServerXmlChangeExpected() {
    def testRoute = "my-new-test-route"

    new TomcatConfigurator(tomcat)
        .jvmRoute(testRoute)

    assertTrue new File(tomcat.basedir, "conf/server.xml").text.contains(testRoute)
  }


}
