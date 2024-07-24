package noe.tomcat.configure.envars

import noe.common.NoeContext
import noe.common.utils.FileStateVault
import noe.common.utils.Library
import noe.common.utils.Platform
import noe.common.utils.Version
import noe.server.Tomcat

/**
 * IMPORTANT: For usage within noe-core:tomcat.configure only
 *
 * Service for handling RPM specific environment files.
 * Supported products are JWS3/JWS5 and BaseOs tomcat on RHEL6/RHEL7.
 */
class RpmTomcatEnvVarsFileFactory {

  private RpmTomcatEnvVarsFileFactory() {
  }

  static EnvVarsFile getInstance(Tomcat tomcatInstance, FileStateVault vault) {
    return getInstance(tomcatInstance, vault, new RpmRunImpl(), new Platform())
  }

  protected static EnvVarsFile getInstance(Tomcat tomcatInstance, FileStateVault vault, RpmRun run) {
    return getInstance(tomcatInstance, vault, run, new Platform())
  }

  protected static EnvVarsFile getInstance(Tomcat tomcatInstance, FileStateVault vault, RpmRun run, Platform platform) {
    if (run.isJws3TestsRunning()) {
      return new Jws3RpmTomcatEnvVarsFile(tomcatInstance, vault)
    } else if (run.isJws5TestsRunning()) {
      return new Jws5RpmTomcatEnvVarsFile(vault)
    } else if (run.isBaseOsTomcatTestsRunning()) {
      return new BaseOsRpmTomcatEnvVarsFile(vault, platform)
    } else {
      throw new IllegalArgumentException(
        'Unsupported product version for manipulation of Tomcat RPM environment variables. ' +
        'Only supported are tomcats in JWS3/JWS5 and BaseOs Tomcat in RHEL6/RHEL7'
      )
    }
  }

  private static class Jws3RpmTomcatEnvVarsFile extends RpmTomcatEnvVarsFileBase {
    private Jws3RpmTomcatEnvVarsFile() {
    }

    Jws3RpmTomcatEnvVarsFile(Tomcat tomcatInstance) {
      String tomcatVersion = tomcatInstance.getVersion().getMajorVersion()
      String tomcatDirName = "tomcat${tomcatVersion}"
      this.envFile = new File("/etc/sysconfig/${tomcatDirName}")
    }

    Jws3RpmTomcatEnvVarsFile(Tomcat tomcatInstance, FileStateVault vault) {
      this(tomcatInstance)
      this.vault = vault
    }
  }

  private static class Jws5RpmTomcatEnvVarsFile extends RpmTomcatEnvVarsFileBase {
    Jws5RpmTomcatEnvVarsFile() {
      Platform platform = new Platform()
      if (platform.isRHEL8() || platform.isRHEL9() || platform.isRHEL10()) {
        this.envFile = new File('/etc/opt/rh/scls/jws5/sysconfig/tomcat')
      } else {
        this.envFile = new File('/etc/opt/rh/jws5/sysconfig/tomcat')
      }
    }

    Jws5RpmTomcatEnvVarsFile(FileStateVault vault) {
      this()
      this.vault = vault
    }
  }

  private static class BaseOsRpmTomcatEnvVarsFile extends RpmTomcatEnvVarsFileBase {

    BaseOsRpmTomcatEnvVarsFile() {
      this(new Platform())
    }

    BaseOsRpmTomcatEnvVarsFile(Platform platform) {
      if (platform.isRHEL6()) {
        this.envFile = new File('/etc/sysconfig/tomcat6')
      } else if (platform.isRHEL7()) {
        this.envFile = new File('/etc/sysconfig/tomcat')
      } else {
        throw new IllegalArgumentException('BaseOs Tomcat is supported on RHEL6 and RHEL7 only')
      }
    }

    BaseOsRpmTomcatEnvVarsFile(FileStateVault vault) {
      this()
      this.vault = vault
    }

    BaseOsRpmTomcatEnvVarsFile(FileStateVault vault, Platform platform) {
      this(platform)
      this.vault = vault
    }
  }

  protected static interface RpmRun {
    boolean isJws3TestsRunning()
    boolean isJws5TestsRunning()
    boolean isBaseOsTomcatTestsRunning()
  }

  protected static class RpmRunImpl implements RpmRun {

    @Override
    boolean isJws3TestsRunning() {
      return isCurrentJwsMajorVersion(3)
    }

    @Override
    boolean isJws5TestsRunning(){
      return isCurrentJwsMajorVersion(5)
    }

    @Override
    boolean isBaseOsTomcatTestsRunning() {
      return NoeContext.forCurrentContext().areInSingleGroup(['rhel','tomcat'])
    }

    private boolean isCurrentJwsMajorVersion(int majorVersion) {
      Boolean ret = false
      String versionStr = Library.getUniversalProperty('ews.version')

      try {
        ret = new Version(versionStr).getMajorVersion() == majorVersion
      } finally {
        return ret
      }
    }
  }

}
