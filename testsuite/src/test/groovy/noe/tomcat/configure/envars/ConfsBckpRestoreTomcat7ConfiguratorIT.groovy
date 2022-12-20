package noe.tomcat.configure.envars

import org.junit.Assume
import org.junit.BeforeClass
import noe.common.utils.Platform

class ConfsBckpRestoreTomcat7ConfiguratorIT extends ConfsBckpRestoreTomcatConfiguratorIT {

  @BeforeClass
   static void beforeClass() {
    Assume.assumeTrue("EWS 3.1.0 is not supported on RHEL8+", new Platform().isRHEL7())
    loadTestProperties("/tomcat7-common-test.properties")
    prepareWorkspace()
  }
}
