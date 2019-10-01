package noe.tomcat

import noe.common.utils.Library
import noe.common.utils.Platform
import noe.tomcat.configure.envars.ZipTomcatEnvVariableAssigmentsGenerator
import noe.tomcat.configure.JmxTomcat
import noe.tomcat.configure.JmxRemoteAccessFileTomcat
import noe.tomcat.configure.JmxRemotePasswordFileTomcat
import noe.tomcat.configure.TomcatConfigurator
import org.junit.After
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue


/**
 * Abstract class for JXM configuraiton testing.
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
abstract class JmxTomcatConfiguratorIT extends TomcatTestAbstract {
  Platform platform = new Platform()

  @After
  void after() {
    new File(tomcat.basedir, "bin/setenv." + platform.getScriptSuffix()).delete()
  }

  @Test
  void setJmxPortOnlyChangeExpected() {
    Integer testJmxPort = 10889
    File setenv = new File(tomcat.basedir, "bin/setenv." + platform.getScriptSuffix())
    String setenvBckp

    String expectedValue = new ZipTomcatEnvVariableAssigmentsGenerator().generateEnvLine("CATALINA_OPTS",
        '-Dcom.sun.management.jmxremote.port=' + testJmxPort +
        ' -Dcom.sun.management.jmxremote')

    try {
      setenvBckp = backupSetenvIfExists(setenv)

      new TomcatConfigurator(tomcat)
        .jmx(new JmxTomcat().setPort(testJmxPort))

      assertEquals expectedValue, setenv.text.readLines().last()
    } finally {
      refreshSetenv(setenvBckp, setenv)
    }
  }

  @Test
  void setJmxChangeExpected() {
    Integer testJmxPort = 10889
    Boolean testSsl = true
    Boolean testAuthenticate = true
    String setenvBckp
    File setenv = new File(tomcat.basedir, "bin/setenv." + platform.getScriptSuffix())
    JmxRemotePasswordFileTomcat testPasswordFile = new JmxRemotePasswordFileTomcat(tomcat, new File(tomcat.basedir, 'conf/jmxremote.password'), ['admin': 'password'])
    JmxRemoteAccessFileTomcat testAccessFile = new JmxRemoteAccessFileTomcat(tomcat, new File(tomcat.basedir, 'conf/jmxremote.access'), ['admin': JmxRemoteAccessFileTomcat.Access.readwrite])

    String expectedValueSetenv = new ZipTomcatEnvVariableAssigmentsGenerator().generateEnvLine("CATALINA_OPTS",
            "-Dcom.sun.management.jmxremote.port=${testJmxPort} " +
            "-Dcom.sun.management.jmxremote.ssl=${testSsl} " +
            "-Dcom.sun.management.jmxremote.authenticate=${testAuthenticate} " +
            "-Dcom.sun.management.jmxremote.password.file=${testPasswordFile.getPath().path} " +
            "-Dcom.sun.management.jmxremote.access.file=${testAccessFile.getPath().path} " +
            "-Dcom.sun.management.jmxremote")

    String expectedValueJmxRemotePassword =
      "admin password" + platform.nl

    String expectedValueJmxRemoteAccess =
      "admin ${JmxRemoteAccessFileTomcat.Access.readwrite}" + platform.nl

    try {
      setenvBckp = backupSetenvIfExists(setenv)
      new TomcatConfigurator(tomcat)
        .jmx(new JmxTomcat()
        .setPort(testJmxPort)
        .setSsl(testSsl)
        .setAuthenticate(testAuthenticate)
        .setPasswordFile(testPasswordFile)
        .setAccessFile(testAccessFile))

      assertEquals expectedValueSetenv, setenv.text.readLines().last()
      assertEquals expectedValueJmxRemotePassword, new File(tomcat.basedir, "conf/jmxremote.password").text
      assertEquals expectedValueJmxRemoteAccess, new File(tomcat.basedir, "conf/jmxremote.access").text

      tomcat.start()
      assertTrue "JMX port was not opened", Library.checkTcpPort(tomcat.host, testJmxPort)
    } finally {
      try {
        tomcat.stop()
      }
      finally {
        refreshSetenv(setenvBckp, setenv)
      }
    }
  }

  private String backupSetenvIfExists(File setenv) {
    String setenvBckp

    if (setenv.exists()) setenvBckp = setenv.text

    return setenvBckp
  }

  private void refreshSetenv(String setenvBckp, File setenv) {
    if (setenvBckp != null) setenv.setText(setenvBckp)
  }

}
