package noe.common.utils

class Hudson {
  static Platform platform = new Platform()
  static String sep = platform.sep

  static final String staticDir = Library.getUniversalProperty('hudson.static.env',
      (platform.isWindows()) ? "h:\\hudson\\static_build_env" : '/home/hudson/static_build_env')
  static final String home = platform.isWindows() ? "h:\\hudson" : '/home/hudson'
  // TODO: default native tools fit on Windows
  static final String nativeTool = Library.getUniversalProperty('NATIVE_TOOLS', (platform.isWindows()) ? "t:\\opt" : '/qa/tools/opt')
  static final String toolsPath = platform.isWindows() ? "t:\\opt" : '/qa/tools/opt'
  // Prefix for the path containing Jenkins jobs build artifacts
  static final String JOBS_DIR = Library.getUniversalProperty('jenkins.jobs.dir', (platform.isWindows()) ? "h:\\hudson\\JOBS\\" : '/qa/services/hudson/JOBS')
  // env variables worth cleanup in Jenkins env (mainly on windows due http://support.microsoft.com/kb/830473)
  static final Map<String, String> jenkinsEnvVarsIrrelevantForNOE = new HashMap<String, String>()
  static {
    jenkinsEnvVarsIrrelevantForNOE.put("MAVEN_CMD_LINE_ARGS", null)
    jenkinsEnvVarsIrrelevantForNOE.put("MVN_TEST_SKIP", null)
    jenkinsEnvVarsIrrelevantForNOE.put("REDIRECT_TEST_OUPUT_TO_FILE", null)
    jenkinsEnvVarsIrrelevantForNOE.put("JENKINS_HOME", null)
    jenkinsEnvVarsIrrelevantForNOE.put("HUDSON_URL", null)
    jenkinsEnvVarsIrrelevantForNOE.put("JOB_URL", null)
    jenkinsEnvVarsIrrelevantForNOE.put("BUILD_NUMBER", null)
    jenkinsEnvVarsIrrelevantForNOE.put("HUDSON_HOME", null)
    jenkinsEnvVarsIrrelevantForNOE.put("NODE_LABELS", null)
    jenkinsEnvVarsIrrelevantForNOE.put("BUILD_ID", null)
    jenkinsEnvVarsIrrelevantForNOE.put("BUILD_TAG", null)
    jenkinsEnvVarsIrrelevantForNOE.put("JENKINS_SERVER_COOKIE", null)
    jenkinsEnvVarsIrrelevantForNOE.put("HUDSON_COOKIE", null)
    jenkinsEnvVarsIrrelevantForNOE.put("ANDROID_HOME", null)
    jenkinsEnvVarsIrrelevantForNOE.put("JAVA_FOR_SLAVE", null)
    jenkinsEnvVarsIrrelevantForNOE.put("INITD", null)
    jenkinsEnvVarsIrrelevantForNOE.put("NODE_NAME", null)
    jenkinsEnvVarsIrrelevantForNOE.put("EXECUTOR_NUMBER", null)
    jenkinsEnvVarsIrrelevantForNOE.put("label", null)
    jenkinsEnvVarsIrrelevantForNOE.put("JAVA_OPTS_FOR_SLAVE", null)
    jenkinsEnvVarsIrrelevantForNOE.put("HUDSON_CONFIG_DIR", null)
    jenkinsEnvVarsIrrelevantForNOE.put("BUILD_CAUSE", null)
    jenkinsEnvVarsIrrelevantForNOE.put("BUILD_URL", null)
    jenkinsEnvVarsIrrelevantForNOE.put("SSH_CONNECTION", null)
    jenkinsEnvVarsIrrelevantForNOE.put("BUILD_CAUSE_UPSTREAMTRIGGER", null)
    jenkinsEnvVarsIrrelevantForNOE.put("JOB_NAME", null)
    jenkinsEnvVarsIrrelevantForNOE.put("HUDSON_SERVER_COOKIE", null)
    jenkinsEnvVarsIrrelevantForNOE.put("JAVA_OPTS_FOR_SLAVE", null)
    jenkinsEnvVarsIrrelevantForNOE.put("HUDSON_SLAVE_OPTS", null)
    jenkinsEnvVarsIrrelevantForNOE.put("JAVA15", null)
    jenkinsEnvVarsIrrelevantForNOE.put("JAVA16", null)
    jenkinsEnvVarsIrrelevantForNOE.put("JAVA17", null)
    jenkinsEnvVarsIrrelevantForNOE.put("RECIPIENTS_JENKINS_TEAM", null)
    jenkinsEnvVarsIrrelevantForNOE.put("BUILD_DISPLAY_NAME", null)
    jenkinsEnvVarsIrrelevantForNOE.put("SLAVE_JAR_OPTS", null)
    jenkinsEnvVarsIrrelevantForNOE.put("ROOT_BUILD_CAUSE_MANUALTRIGGER", null)
    jenkinsEnvVarsIrrelevantForNOE.put("OPENJDK6_HOME", null)
    jenkinsEnvVarsIrrelevantForNOE.put("OPENJDK7_HOME", null)
    jenkinsEnvVarsIrrelevantForNOE.put("OPENJDK8_HOME", null)
  }
}
