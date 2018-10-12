package noe.ews.utils.selinux

import noe.common.utils.Cmd

public class SELinuxSystem {

  // TODO SUEXEC
  public static void createFileIfDoesntExist(String filePath) {
    File file = new File(filePath)
    if (!file.exists()) {
      try {
        new File(filePath).parentFile.mkdirs()
        boolean success = file.createNewFile()
      } catch (Exception e) {
        throw new SELinuxException("Could not create $filePath", e)
      }
    }
  }

  public static SELinuxContext getContextForFile(String filePath) {
    String[] strings = new String[0]
    Process p = Runtime.getRuntime().exec("ls -d --scontext $filePath", strings, new File("."))
    p.waitFor()
    //mbabacek: Is it really OK to try to consume first stdin and then stderr sequentially in a one thread? What if the process stuck waiting for somebody to read stderr? Java vs. Groovy...
    String output = p.getInputStream().getText()
    String error = p.getErrorStream().getText()
    int exitValue = p.exitValue()
    switch (exitValue) {
      case 0: // OK
        String context = output.tokenize(" ").get(0)
        return new SELinuxContext(context)
      case 2: // file does not exist
      default:  // or something else
        throw new SELinuxException(error.trim())
    }
  }

  /**
   * Use the 'restorecon' unix command to relabel a file. If a directory is specified, all files within it will
   * be relabeled recursively.
   * @param path the filesystem path to the FILE or DIRECTORY to be relabeled
   */
  public static void relabelFile(String path) {
    Process p = Runtime.getRuntime().exec("restorecon $path")
    p.waitFor()
    if (p.exitValue() != 0) {
      throw new SELinuxException("restorecon command failed: ${p.errorStream.text}")
    }
  }

  public static boolean isSELinuxEnabled() {
    Process p = Runtime.runtime.exec("selinuxenabled")
    p.waitFor()
    return p.exitValue() == 0
  }

  public static boolean isSELinuxInEnforcingMode() {
    if (!isSELinuxEnabled())
      return false
    Process p = Runtime.runtime.exec("getenforce")
    p.waitFor()
    String output = p.getInputStream().getText().trim()
    if (output.equalsIgnoreCase("enforcing")) {
      return true
    } else if (output.equalsIgnoreCase("permissive")) {
      return false
    } else {
      throw new SELinuxException("Output of 'getenforce' command not recognized: $output")
    }
  }

  public static void checkEnvironmentSanity() {
    if (!isSELinuxEnabled())
      throw new SELinuxException("Environment sanity check failed: SELinux is not enabled.")
  }

  public static ProcessSELinuxContext getContextOfRunningProcess(int pid) {
    Process p = Runtime.getRuntime().exec("ps h -o label $pid")
    p.waitFor()
    if (p.exitValue() != 0) {
      String error = p.getErrorStream().text
      throw new SELinuxException("unable to get context of process $pid: ps command returned ${p.exitValue()} $error")
    }
    String out = p.inputStream.text
    return new ProcessSELinuxContext(out)
  }

  public static List<Integer> getProcessIDsByProgramName(String name) {
    Process p = Runtime.getRuntime().exec("ps h -o pid -C $name")
    p.waitFor()
    if (p.exitValue() != 0) {
      String error = p.getErrorStream().text
      throw new SELinuxException("unable to get pid of process $name: ps command returned ${p.exitValue()} $error")
    }
    List<Integer> ret = new ArrayList<Integer>()
    p.inputStream.text.eachLine { line ->
      ret.add(Integer.parseInt(line.trim()))
    }
    return ret
  }

  public static List<Integer> getProcessIDofTomcat() {
    Process p = Runtime.getRuntime().exec("ps h -o pid -u tomcat")
    p.waitFor()
    if (p.exitValue() != 0) {
      String error = p.getErrorStream().text
      throw new SELinuxException("unable to get pid of tomcat process: ps command returned ${p.exitValue()} $error")
    }
    List<Integer> ret = new ArrayList<Integer>()
    p.inputStream.text.eachLine { line ->
      ret.add(Integer.parseInt(line.trim()))
    }
    return ret
  }

  public static List<Integer> getProcessIDofHttpd() {
    Process p = Runtime.getRuntime().exec("ps h -o pid -u apache")
    p.waitFor()
    if (p.exitValue() != 0) {
      String error = p.getErrorStream().text
      throw new SELinuxException("unable to get pid of httpd process: ps command returned ${p.exitValue()} $error")
    }
    List<Integer> ret = new ArrayList<Integer>()
    p.inputStream.text.eachLine { line ->
      ret.add(Integer.parseInt(line.trim()))
    }
    return ret
  }

  public static void runAuditdIfNotRunning() {
    Process p = Runtime.getRuntime().exec("service auditd start")
    p.waitFor()
    if (p.exitValue() != 0) {
      String error = p.getErrorStream().text
      throw new SELinuxException("unable to run audit daemon: exitvalue=${p.exitValue()}; stderr=$error")
    }
  }

  /**
   * @param type
   * @param TCP true=TCP, false=UDP
   * @return
   */
  private static List<Integer> getPortsBySELinuxType(String type, boolean TCP) {
    Process p = Runtime.getRuntime().exec("semanage port -nl")
    p.waitFor()
    if (p.exitValue() != 0) {
      String error = p.getErrorStream().text
      throw new SELinuxException("unable to get list of ports assigned to type $type: semanage command returned ${p.exitValue()} $error")
    }
    List<Integer> ret = new ArrayList<Integer>()
    String text = p.inputStream.text
    text.eachLine { line ->
      if (line =~ "^$type +${TCP ? "tcp" : "udp"}") {
        println line
        String[] ports = ((line.split("  +"))[2]).split(", *")
        println ports
        ports.each { port ->
          if (port.contains("-")) {
            def (begin, end) = port.split("-")
            for (int i = Integer.parseInt(begin); i <= Integer.parseInt(end); i++) {
              ret.add(i)
            }
          } else {
            ret.add(Integer.parseInt(port))
          }
        }
      }
    }
    return ret
  }

  public static List<Integer> getTCPPortsBySELinuxType(String type) {
    return getPortsBySELinuxType(type, true)
  }

  public static List<Integer> getUDPPortsBySELinuxType(String type) {
    return getPortsBySELinuxType(type, false)
  }

  public static void stopAuditd() {
    int code = Cmd.executeSudoCommand("service auditd stop", new File("/"))
    if (code != 0) {
      throw new RuntimeException("'service auditd stop' returned: ${code}")
    }
  }

  public static void startAuditd() {
    int code = Cmd.executeSudoCommand("service auditd start", new File("/"))
    if (code != 0) {
      throw new RuntimeException("'service auditd stop' returned: ${code}")
    }
  }

  /*    *//**
   * a little hack resetting the audit rules in permissive mode, which would otherwise cause every violation to be logged only once.
   * required to run this before every test
   *//*
   public static void setEnforcingAndThenPermissive() {
   setSELinuxEnforcingMode(true)
   Library.letsSleep(1000)
   setSELinuxEnforcingMode(false)
   }*/

  public static void setSELinuxEnforcingMode(boolean enforcing) {
    String command = "setenforce " + (enforcing ? "1" : "0")
    int code = Cmd.executeSudoCommand(command, new File("/"))
    if (code != 0) {
      throw new RuntimeException("'$command' returned: ${code}")
    }
  }

  public static void setDomainPermissive(String domain) {
    String command = "semanage permissive -a $domain"
    int code = Cmd.executeSudoCommand(command, new File("/"))
    if (code != 0) {
      throw new RuntimeException("'$command' returned: ${code}")
    }
  }

}
