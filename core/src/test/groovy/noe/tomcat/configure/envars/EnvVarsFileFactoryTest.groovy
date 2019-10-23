package noe.tomcat.configure.envars

import noe.common.utils.FileStateVault
import noe.common.utils.Platform
import noe.common.utils.Version
import noe.server.Tomcat
import org.junit.Assume
import org.junit.Before;
import org.junit.Test;


class EnvVarsFileFactoryTest {
  FileStateVault vault
  Version version
  Tomcat tomcat
  String baseDir = 'tomcat'
  String binDir = "${baseDir}/bin"
  Platform platform = new Platform()


  @Before
  void before() {
    vault = new FileStateVault()
    version = new Version('1.0.0-FAKE')
    tomcat = new Tomcat(baseDir, version)
    tomcat.binDir = binDir
  }

  @Test
  void getInstanceZip() {
    EnvVarsFile envVars = new ZipTomcatEnvVarsFile(tomcat, vault)

    Assume.assumeTrue envVars.getEnvFile().getPath() == "${binDir}/setenv.${platform.getScriptSuffix()}"
  }

  @Test
  void getInstanceRpmJWS3() {
    EnvVarsFile envVars = RpmTomcatEnvVarsFileFactory.getInstance(tomcat, vault, [
      isJws3TestsRunning: { return true },
      isJws5TestsRunning: { return false },
      isBaseOsTomcatTestsRunning: { return false }
    ] as RpmTomcatEnvVarsFileFactory.RpmRun)

    Assume.assumeTrue envVars.getEnvFile().getPath() == "/etc/sysconfig/tomcat${version.getMajorVersion()}"
  }

  @Test
  void getInstanceRpmJWS5() {
    EnvVarsFile envVars = RpmTomcatEnvVarsFileFactory.getInstance(tomcat, vault, [
      isJws3TestsRunning: { return false },
      isJws5TestsRunning: { return true },
      isBaseOsTomcatTestsRunning: { return false }
    ] as RpmTomcatEnvVarsFileFactory.RpmRun)

    String tomcatServicePath
    if (platform.isRHEL8()) {
      tomcatServicePath = "/etc/opt/rh/scls/jws5/sysconfig/tomcat"
    } else {
      tomcatServicePath = "/etc/opt/rh/jws5/sysconfig/tomcat"
    }
    Assume.assumeTrue envVars.getEnvFile().getPath() == tomcatServicePath
  }

  @Test
  void getInstanceRpmBaseOsRhel6() {
    EnvVarsFile envVars = RpmTomcatEnvVarsFileFactory.getInstance(tomcat, vault, [
      isJws3TestsRunning: { return false },
      isJws5TestsRunning: { return false },
      isBaseOsTomcatTestsRunning: { return true }
    ] as RpmTomcatEnvVarsFileFactory.RpmRun, [
      isRHEL6: { return true },
      isRHEL7: { return false }
    ] as Platform)

    Assume.assumeTrue envVars.getEnvFile().getPath() == "/etc/sysconfig/tomcat6"
  }

  @Test
  void getInstanceRpmBaseOsRhel7() {
    EnvVarsFile envVars = RpmTomcatEnvVarsFileFactory.getInstance(tomcat, vault, [
      isJws3TestsRunning: { return false },
      isJws5TestsRunning: { return false },
      isBaseOsTomcatTestsRunning: { return true }
    ] as RpmTomcatEnvVarsFileFactory.RpmRun, [
      isRHEL6: { return false },
      isRHEL7: { return true }
    ] as Platform)

    Assume.assumeTrue envVars.getEnvFile().getPath() == "/etc/sysconfig/tomcat"
  }

  @Test
  void getInstanceZipDefault() {
    EnvVarsFile envVars = EnvVarsFileFactory.getInstance(tomcat, vault)

    Assume.assumeTrue envVars.getEnvFile().getPath() == "tomcat/bin/setenv.${platform.getScriptSuffix()}"
  }

}

