package noe.common.utils

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.StreamConsumer
import noe.common.StreamFiller
import noe.common.newcmd.CmdCommand
import noe.common.newcmd.KillCmdBuilder
import noe.common.newcmd.ListProcess
import noe.common.newcmd.PsCmdFormat
import noe.common.utils.processid.ProcessUtils

import java.lang.management.ManagementFactory
import java.lang.management.RuntimeMXBean
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
/**
 * @author Jan Stefl       <jstefl@redhat.com>
 * @author Libor Fuka      <lfuka@redhat.com>
 * @author Michal Babacek  <mbabacek@redhat.com>
 */

@Slf4j
public class Cmd {
  private static Platform platform = new Platform()
  private static Map props = [:]  /// Environment properties
  //The PID of this JVM
  private static Integer noePid
  private static Collection<Integer> noePidIncludingParentPids

  // These we do not want to kill: maven, surefire
  private static List<String> KILL_ALL_PROCESS_IDENTIFIERS_TO_IGNORE = ['org.codehaus.plexus.classworlds.launcher.Launcher', 'surefirebooter']

  static {
    // Initialize environment properties
    System.getenv().each { key, val ->
      props.put(key, val)
    }
    noePid = retrieveNOEPid()
    try {
      noePidIncludingParentPids = retrieveNoePidIncludingParentPids()
    } catch (Throwable t) {
      log.error("Exception thrown while looking for parent pids", t)
      throw t
    }

    log.debug("noePid is set to ${noePid}, where noePid including parents are: ${noePidIncludingParentPids}")
  }

  /**
   * Library design pattern
   */
  private Cmd() {}

  /**
   * Get environment properties
   */
  static Map getProps() {
    return props
  }

  /**
   * Retrieve NOE Pid
   * See stackoverflow.com/questions/35842/how-can-a-java-program-get-its-own-process-id
   * See http://blog.philippheckel.com/2014/06/14/getting-the-java-process-pid-and-managing-pid-files-linux-windows/
   * @return pid of the JVM NOE is running in or -1 if it wasn't possible to get the pid
   */
  static int retrieveNOEPid() {
    String pidStr = ManagementFactory.getRuntimeMXBean().getName();
    log.trace("retrieveNOEPid: pidStr was ${pidStr}")
    if (pidStr != null && pidStr.contains('@')) {
      return Integer.parseInt(pidStr.split('@')[0]);
    } else {
      log.debug("retrieveNOEPid: ManagementFactory failed, gonna try getDeclaredField(\"jvm\").")
      RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
      Field jvm = runtime.getClass().getDeclaredField("jvm");
      jvm.setAccessible(true);

      Object vmManagement = jvm.get(runtime);
      Method pidMethod = vmManagement.getClass().getDeclaredMethod("getProcessId");

      pidMethod.setAccessible(true);
      return (Integer) pidMethod.invoke(vmManagement);
    }
  }

  static Collection<Integer> retrieveNoePidIncludingParentPids() {
    int noePid = retrieveNOEPid();
    // getting tree of processes running under launched cmd
    ListProcess listProcessUtil = new ListProcess();
    return listProcessUtil.listParentPids(noePid);
  }

  /**
   * Execute command line command, redirect input, output and error
   *
   * TODO implement using of props - setting env. var. for single run
   *
   * @deprecated args
   */
  public static int executeCommandRedirectIO(command, File targetDir, InputStream input, OutputStream output, OutputStream error, Map args = null, Map tmpProps = null) {
    if (args != null) {
      // Create string command - backward compatibility
      if (command instanceof String || command instanceof GString) command += " ${mapToCommandLine(args)} "
      // Create list command
      // TODO this concept is WRONG - arguments needs to be as part of command, because of commands like ['sudo', 'su', 'user', '-c', 'commmand']
      else if (command instanceof List) {
        args.each { command += it.value }
      }
      // Not supported command type
      else throw new RuntimeException('Incorrect command type')
    }

    log.debug('Executing command {} in target directory {}', command, targetDir)
    log.trace('PROPERTIES: ' + Library.map2list(Library.mapUnion(Cmd.props, tmpProps)))

    //TODO: Command should always be an instance of List, not String.
    Process process = (command).execute(Library.map2list(Library.mapUnion(Cmd.props, tmpProps)), targetDir)

    StreamFiller stdIn = new StreamFiller(process.getOutputStream(), input)
    stdIn.start()

    process.waitForProcessOutput(output,error)
    final int errCode = process.waitFor()
    stdIn.join(5000)

    return errCode
  }

  /**
   * Simple command execution with an arbitrary input
   * @param command - the command and its arguments as elements of a list
   * @param targetDir - the directory where the command is supposed to be executed
   * @param input - string to write to process' input
   * @param timeout - waitForOrKill timeout in ms
   * @param output - process' output stream
   * @param error - process' error stream
   * @return - [stdOut:String, stdErr:String, exitValue:Integer]
   */
  public static Map executeCommandConsumeStreams(
      final List command, final File targetDir = new File('.'), final byte[] input = null, final long timeout = 60000L, final Map tmpProps = null) {
    log.debug('Executing command {} in target directory {}', command, targetDir)
    final StringBuffer stdOutAsStr = new StringBuffer()
    final StringBuffer stdErrAsStr = new StringBuffer()
    final Process process = command.execute(Library.map2list(Library.mapUnion(Cmd.props, tmpProps)), targetDir)
    StreamConsumer stdOut = new StreamConsumer(process.getInputStream(), stdOutAsStr)
    StreamConsumer stdErr = new StreamConsumer(process.getErrorStream(), stdErrAsStr)
    stdOut.start()
    stdErr.start()

    if (input) {
      try {
        final int bucket = 1024
        int offset = 0
        while (offset < input.length) {
          if ((offset + bucket) < input.length) {
            process.out.write(input, offset, bucket)
            offset += bucket
          } else {
            process.out.write(input, offset, input.length - offset)
            offset = input.length
          }
        }
      } finally {
        process.out?.flush()
        process.out?.close()
      }
    }

    process.waitForOrKill(timeout)

    stdOut.join(3000)
    stdErr.join(3000)

    final Map result = [stdOut: stdOutAsStr.toString(), stdErr: stdErrAsStr.toString(), exitValue: process.exitValue()]
    if (process.exitValue() != 0) {
      log.debug("RESULT: ${result}")
    }
    log.trace("RESULT: ${result}")

    return result
  }

  /**
   * Simple sudo command execution with an arbitrary input
   * @param command - the command and its arguments as elements of a list
   * @param targetDir - the directory where the command is supposed to be executed
   * @param input - string to write to process' input
   * @param timeout - waitForOrKill timeout in ms
   * @param output - process' output stream
   * @param error - process' error stream
   * @return - [stdOut:String, stdErr:String, exitValue:Integer]
   */
  public static Map executeSudoCommandConsumeStreams(
          final List command, final File targetDir = new File('.'),
          final byte[] input = null, final long timeout = 10000L, final Map tmpProps = null) {
    return executeCommandConsumeStreams(['sudo'] + command, targetDir, input, timeout, tmpProps)
  }

  /**
   * Sudo command execution with an arbitrary input and env. vars keep support
   * @param command - the command and its arguments as elements of a list
   * @param targetDir - the directory where the command is supposed to be executed
   * @param input - string to write to process' input
   * @param timeout - waitForOrKill timeout in ms
   * @param output - process' output stream
   * @param error - process' error stream
   * @return - [stdOut:String, stdErr:String, exitValue:Integer]
   */
  public static Map executeSudoCommandKeepVarsConsumeStreams(
          final List command, final File targetDir = new File('.'),
          final byte[] input = null, final long timeout = 10000L, final Map tmpProps = null) {
    return executeCommandConsumeStreams(['sudo', '-E'] + command, targetDir, input, timeout, tmpProps)
  }

  /**
   * Method for deciding which method to run based on availability of admin privileges
   * example of usage executeMethodBasedOnAdminPrivileges(&executeCommand, &executeSudoCommand)
   * @param noSudoMethod, as closure when admin privileges are not available
   * @param sudoMethod, as closure when admin privileges are available
   * @param sudo, if one want to force one closure or another
   * @return One of the closure based on JBFile.useAdminPrivileges or sudo param
   */
  static Closure executeMethodBasedOnAdminPrivileges(Closure noSudoMethod, Closure sudoMethod) {
    if (!noSudoMethod || !sudoMethod) {
      throw new RuntimeException("Cannot run null method!!")
    }
    if (JBFile.useAdminPrivileges && !platform.isWindows()) {
      return sudoMethod
    } else {
      return noSudoMethod
    }

  }

  /**
   * Execute command line command.
   *
   * @deprecated args
   */
  public static int executeCommand(command, File targetDir, Map args = null, Map tmpProps = null) {
    return executeCommandRedirectIO(command, targetDir, null, System.out, System.err, args, tmpProps)
  }

  /**
   *  Run command under root privileges
   *  Execute command line command with sudo.
   *
   * @deprecated args
   */
  public static int executeSudoCommand(command, File targetDir, Map args = null, Map tmpProps = null) {
    if (args != null) {
      // Create string command - backward compatibility
      if (command instanceof String || command instanceof GString) command += " ${mapToCommandLine(args)} "
      // Create list command
      // TODO this concept is WRONG - arguments needs to be as part of command, because of commands like ['sudo', 'su', 'user', '-c', 'commmand']
      else if (command instanceof List) {
        args.each { command += it.value }
      }
      // Not supported command type
      else throw new RuntimeException('Incorrect command type')
    }

    log.debug('Run under root privileges command {} in target directory {}', command, targetDir)
    log.trace('PROPERTIES: ' + Library.map2list(Library.mapUnion(Cmd.props, tmpProps)))

    def sudoPrefix
    if (command instanceof List) sudoPrefix = ["sudo"]
    else sudoPrefix = "sudo "

    def sudocommand = sudoPrefix + command
    //TODO: Command should always be an instance of List, not String.
    def Process process = (sudocommand).execute(Library.map2list(Library.mapUnion(Cmd.props, tmpProps)), targetDir)
    // TODO parametrized input, output streams
    process.waitForProcessOutput(System.out, System.err)

    return process.waitFor()
  }

  /**
   * Execute command line command with sudo su - username.
   *
   * @deprecated args
   */
  public static int executeSudoSuCommand(String username, command, File targetDir, Map args = null, Map tmpProps = null) {
    if (args != null) {
      // Create string command - backward comaptibility
      if (command instanceof String || command instanceof GString) command += " ${mapToCommandLine(args)} "
      // Create list command
      // TODO this concept is WRONG - arguments needs to be as part of command, because of commands like ['sudo', 'su', 'user', '-c', 'commmand']
      else if (command instanceof List) {
        args.each { command += it.value }
      }
      // Not supported command type
      else throw new RuntimeException('Incorrect command type')
    }

    log.debug("Executing command ${command} under user ${username} in target directory ${targetDir}")
    log.trace('PROPERTIES: ' + Library.map2list(Library.mapUnion(Cmd.props, tmpProps)))

    //TODO: Command should always be an instance of List, not String.
    Process process = "sudo su - $username".execute(Library.map2list(Library.mapUnion(Cmd.props, tmpProps)), targetDir)

    String cmdString = command + '\n' + 'exit\n'
    InputStream cmdInput = new ByteArrayInputStream( cmdString.getBytes() );
    StreamFiller stdIn = new StreamFiller(process.getOutputStream(), cmdInput)
    stdIn.start()

    // TODO parametrized input, output streams
    process.waitForProcessOutput(System.out, System.err)
    final int errCode = process.waitFor()
    stdIn.join(5000)

    return errCode
  }

  /**
   * Start process in background for Unix/Windows from command line.
   * @deprecated this method creates issues with being able to retrieve properly process tree,
   * use rather {@link #startProcess(noe.common.newcmd.CmdCommand)} method.
   */
  public static Process startProcessInBackground(List command, File targetDir, winappname = null, Map tmpProps = [:]) {
    if (platform.isWindows()) {
      command = [
          "cmd",
          "/C",
          "start",
          "\"${winappname}\"",
          "/min",
          "cmd",
          "/C",
      ] + command
    }

    log.debug('Starting process in background by executing command {} in target directory {}', command, targetDir)
    log.trace('PROPERTIES: ' + Library.map2list(Library.mapUnion(Cmd.props, tmpProps)))
    return (command).execute(Library.map2list(Library.mapUnion(Cmd.props, tmpProps)), targetDir)
  }

  /**
   * Starts process for Unix/Windows from command line based on cmdCommand and returns started process.
   */
  static Process startProcess(final CmdCommand cmdCommand) {
    List<String> command = cmdCommand.getCommandLine()
    log.debug("Starting process via command {}", cmdCommand)
    return command.execute(Library.map2list(Library.mapUnion(Cmd.props, cmdCommand.getEnvProperties())),
            cmdCommand.getWorkingDirectory())
  }

  /**
   * Start process for Unix/Windows from command line.
   * @deprecated this method creates issues with being able to retrieve properly process tree,
   * use rather {@link #startProcess(noe.common.newcmd.CmdCommand)} method.
   */
  @Deprecated
  static Process startProcess(String command, File targetDir, winappname = null, Map tmpProps = null) {
    if (platform.isWindows()) {
      command = "cmd /C start \"${winappname}\" cmd /C ${command}"
    } else {
      command = "sh ${command}"
    }
    log.debug('Starting process by executing command {} in target directory {}', command, targetDir)
    log.trace('PROPERTIES: ' + Library.map2list(Library.mapUnion(Cmd.props, tmpProps)))
    //TODO: Command should always be an instance of List, not String.
    def process = (command).execute(Library.map2list(Library.mapUnion(Cmd.props, tmpProps)), targetDir)
    return process
  }

  /**
   * Stop process.
   *
   * SIGKILL
   */
  public static void stopProcess(Process process, winappname = null) {
    if (process) {
      log.debug('STOPPING PROCESS: ' + process)
      if (platform.isWindows()) {
        killTree(null, winappname)
      } else {
        process.destroy()
      }
    }
  }

  /**
   * Converts a map to a command line argument string.
   * Each map entry is added to the string in the form of -Dkey=value.
   *
   * @param map with properties
   * @returns string representation of the map
   */
  public static String mapToCommandLine(Map map, addPrefixD = true) {
    String propLine = ""
    def prefix = ''

    map.each { key, value ->
      // Add only prefix -D if it has sense
      prefix = (!key.toString().startsWith('-D') && addPrefixD) ? '-D' : ''
      propLine += " ${prefix}${key}=${value}"
    }

    return propLine
  }

  /**
   * Env. variable will be available from all runs
   * If append is true (which is default value) then the value is appended to the env value, if it is false,
   * the value is replaced
   */
  static void setGlobalEnvVar(Map map, boolean append = true) {
    map.each { key, val ->
      if (Cmd.props.containsKey(key) && append) Cmd.props[key] += map[key]
      else Cmd.props.put(key, val)
    }
  }

  /**
   * Removes global env. variable
   */
  static void removeGlobalEnvVar(Map map) {
    map.each { key, val ->
      // remove whole item
      if (Cmd.props.containsKey(key) && (map[key] == null || Cmd.props[key] == map[key])) Cmd.props.remove(key.toString())
      // remove part where value appear, typically smth. sub part of PATH, etc.
      if (Cmd.props.containsKey(key)) Cmd.props[key] = Cmd.props[key].minus(map[key])
    }
  }

  /**
   * TODO replace with plaform.getUserName
   * @return actual user
   */
  static String getActualUser() {
    if (!platform.isWindows()) return Library.getUniversalProperty('user', 'hudson') // unix command "id" would be better
    else return Library.getUniversalProperty('username', 'hudson') // maybe smth. else could b better
  }

  /**
   * returns pid using jps utility => works only for java processes run under the same user as this testsuite
   * @param id ... unique identificator of the java process
   * @return pid of the java process or null if such process isn't found
   */
  static Long getJavaPid(String id) {
    if (!platform.isWindows()) {
      def p
      def jps = DefaultProperties.JAVA_HOME + "${platform.sep}bin${platform.sep}jps"
      // This only eaps running under actual user, if sudo allowed, then kills all instances in system.
      try {
        if (!platform.isWindows()) {
          p = 'jps -mlvV'.execute() | "grep $id".execute()
        } else {
          p = 'jps -mlvV'.execute() | "FINDSTR $id".execute()
        }

        p.waitFor()
      } catch (IOException ex) {
        log.warn("getJavaPid($id) failed with ${ex}")
        return null;
      }
      def t = new StringTokenizer(p.text.trim(), ' ')
      if (!t.hasMoreElements()) {
        return null
      }
      String o = t.nextElement()
      // expects format pid  ....
      return Long.valueOf(o)
    }
  }

  /**
   * Kill process.
   *
   * TODO variant only for PID
   *
   */
  static int kill(Long pid, winappname) {
    log.debug("Forcibly killing process with PID=${pid}, Window title=${winappname}")
    // TODO: Get rid of black magic :-(
    if (platform.isRHEL()) {
      int killOutcome = Cmd.destroyProcessWithTree(pid) ? 0 : 1
      if (killOutcome > 0 && pidExists(pid)) {
        throw new RuntimeException("[sudo] kill -9 ${pid} failed")
      }
      return killOutcome
      //TODO: Sort out the killTree/kill mess. i.e. killTree here should be called _only_ on Windows.
    } else {
      return killTree(pid, winappname)
    }
  }

  /**
   * Kill process tree.
   * returns kill process exit code. If PID is not defined on non-windows platform, -1 is returned
   * TODO: properly implement it for Unix/Linux systems as pkill -P kills only processes with given parent PID, it doesn't go recursively killing from bottom to top
   */
  static int killTree(Long pid, winappname) {
    if (platform.isWindows()) {
      if (pid != null) {
        log.debug("killtree(): MS Windows taskkill command: taskkill /PID ${pid} /f /t")
        def pKill = [
            "taskkill",
            "/PID",
            "${pid}",
            "/f",
            "/t"
        ].execute()
        pKill.waitForProcessOutput(System.out, System.err)
        return pKill.waitFor()
      } else {
        log.debug("killtree(): MS Windows taskkill command: taskkill /fi \"windowtitle eq ${winappname}*\" /f /t")
        def pKill = [
            "taskkill",
            "/fi",
            "\"windowtitle eq ${winappname}*\"",
            "/f",
            "/t"
        ].execute()
        pKill.waitForProcessOutput(System.out, System.err)
        return pKill.waitFor()
      }
    } else {
      if (!pid) {
        log.warn("PID not defined, process is probably already killed if not check method for pid extraction")
        return -1
      }

      if (!destroyProcessWithTree(pid)) {
        if (pidExists(pid)) {
          log.warn("Destroying process with ${pid} including its child failed, lets try the old way with killing by using pkill -9 -P ${pid}")
          // UNIX - Linux, Solaris, ...
          def killOutcome = 0
          if (((killOutcome = (Cmd.executeCommand(['pkill', '-9', "-P", "${pid}"], new File('.')))) > 0) && JBFile.useAdminPrivileges) {
            killOutcome = Cmd.executeSudoCommand(['pkill', '-9', "-P", "${pid}"], new File('.'))
          }
          log.debug("pkill -9 ${pid} ended with exit code ${killOutcome}")
          if (pidExists(pid)) {
            log.debug("Killing also the process ${pid} itself")
            //Cmd.kill -> killTree -> kill -> StackOverflow
            if (((killOutcome = (Cmd.executeCommand(['kill', '-9', "${pid}"], new File('.')))) > 0) && JBFile.useAdminPrivileges) {
              killOutcome = Cmd.executeSudoCommand(['kill', '-9', "${pid}"], new File('.'))
            }
          }
          if (killOutcome > 0 && pidExists(pid)) {
            log.warn("pkill -9 -P ${pid} failed")
            log.debug("PID list: " + getPidList())
            throw new RuntimeException("pkill/kill -9 ${pid} failed")
          }
          return killOutcome
        }
      }
      return 0
    }
  }

  /**
   * Destroys process including its subprocesses
   * @param process - process to be destroyed
   * @return true if the process was destroyed including its child processes, false otherwise
   */
  static boolean destroyProcess(final Process process) {
    if (!ProcessUtils.isProcAlive(process)) {
      log.debug("Process {} is already dead", process)
      return false
    }
    long pid = ProcessUtils.getProcessId(process)
    if (pid == ProcessUtils.UNDEFINED_PROCESS_ID) {
      log.warn("Failed to extract pid from process")
      return false
    }
    log.debug("Killing process with pid {} including its child processes", pid)
    return destroyProcessWithTree(pid)
  }

  private static synchronized boolean destroyProcessWithTree(final Long pid) {
    // getting tree of processes running under launched cmd
    ListProcess listProcessUtil = new ListProcess();
    List<Map<PsCmdFormat, String>> processTree = listProcessUtil.listProcessTree(pid);

    // showing debug about what will be killed
    try {
      log.debug("Killing process {}:\n{}", pid, listProcessUtil.printProcessInfo(pid));
    } catch (Exception e) {
      log.debug("Killing process {} failed but can't get more detailed info", pid, e);
    }

    KillCmdBuilder killBuilder = new KillCmdBuilder();
    killBuilder.setForce();

    if(platform.isWindows()) {
      log.trace("Windows platform: using /t option of taskkill to kill process tree of {}", pid);
      killBuilder.addArgument("/t"); // killing process tree
      killBuilder.addProcessId(pid);
      return Cmd.executeCommand(killBuilder.build().getCommandLine(), new File(".")) != null
    } else {
      long[] processTreePidArray = new long[processTree.size()];
      int i = 0;
      for(Map<PsCmdFormat, String> processTreeRecord: processTree) {
        processTreePidArray[i++] = Long.valueOf(processTreeRecord.get(PsCmdFormat.PROCESS_ID));
      }
      log.debug("*nix platform: destroying process tree of {} as list of pids {}",
              pid, processTreePidArray);
      killSpecifiedProcesses(Arrays.asList(processTreePidArray))
      return waitForPidsRemoved(Arrays.asList(processTreePidArray), 3, TimeUnit.SECONDS)
    }
  }

  /**
   * The objective of this method is to take a list of identifiers and try
   * to kill as many application processes as possible based on these identifiers.
   * The method is _greedy_, i.e. it won't stop at the first match, it will go as
   * far as possible.
   *
   * Select identifiers with care.
   *
   * @param identifiers
   */
  static void killAllInSystem(List<String> identifiers) {
    if (identifiers.size() < 1) {
      throw new IllegalArgumentException("Calling killAllInSystem with an empty list of identifiers ain't supported.")
    }
    log.debug("System wide killing of processes based on provided identifiers: ${identifiers}")
    if (platform.isWindows()) {
      killAllInWindowsSystem(identifiers)
    } else {
      killAllInUnixSystem(identifiers)
    }
  }

  static void killAllInWindowsSystem(List<String> identifiers) {
    /**
     * We shall give it a shot with wmic, browsing command line
     *
     * ProcessId=1836
     */
    List<Integer> pidsToKill = []
    identifiers.each { identifier ->
      List wmicCmd = ["wmic", "process", "where", "(", "commandline", "like", "\"%${identifier}%\"",
                      "and", "not", "commandline", "like", "\"%wmic%\""]
      KILL_ALL_PROCESS_IDENTIFIERS_TO_IGNORE.each { it ->
        wmicCmd.addAll(["and", "not", "commandline", "like", "\"%" + it + "%\""])
      }
      wmicCmd.addAll([")", "get", "Processid", "/format:list"])
      Process wmicProc = wmicCmd.execute()
      wmicProc.waitFor()
      wmicProc.in.eachLine { line ->
        if (line.contains("ProcessId=")) {
          try {
            pidsToKill.add(Integer.parseInt(line.split("=")[1]))
          } catch (NumberFormatException ex) {
            log.debug("We don't care that line contained an invalid processCode to parse: ${line}. Continuing...")
          }
        }
      }
    }

    /**
     * Let's kill all matching windows titles
     */
    identifiers.each { identifier ->
      try {
        pidsToKill.addAll(extractWindowsPids(identifier))
      } catch (Throwable ex) {
        log.debug("No pids in windows titles with identifier ${identifier}, exception detected", ex)
      }
    }

    // Actual killing (Note duplicity with killTree, TODO: Eventually fuse...)
    // TODO: Also duplicate with killSpecifiedProcesses
    // Safeguard against accidental killing of NOE TS
    // we need to remove by index as in case of list the noe PID would be considered index and not object in the list
    log.trace("Ignoring NOE pid including its parents from selection for system wide killing " +
            "=> excluding ${noePidIncludingParentPids} from ${pidsToKill}")
    pidsToKill = pidsToKill.minus(noePidIncludingParentPids)


    pidsToKill.each { pid ->
      // /t for Tree is intentional here.
      Process pKill = ["taskkill", "/PID", "${pid}", "/f", "/t"].execute()
      pKill.waitForProcessOutput(System.out, System.err)
      pKill.waitFor()
    }

    /**
     * Third time is the charm, let's kill images of that name
     */
    identifiers.each { identifier ->
      // /t for Tree is intentional here.
      // *  expands to e.g. .exe i.e. identifier "httpd" would kill "httpd.exe" image
      Process pKill = ["taskkill", "/IM", "${identifier}*", "/f", "/t"].execute()
      pKill.waitForProcessOutput(System.out, System.err)
      pKill.waitFor()
    }
  }

  static void killAllInUnixSystem(List<String> identifiers) {
    List<String> psCommand
    Pattern psRegExp
    Set<Integer> pidsToKill = new HashSet<>()
    //TODO: As soon as it's tested, we should factor it out so as to avoid duplicating code in extractUNIXPids
    if (platform.isHP()) {
      psCommand = ['ps', '-elfx']
      psRegExp = ~/^[0-9]*[ ]*[A-Z][ ]*[a-z]*[ ]*([0-9]*).*/
    } else {
      psCommand = ['ps', '-ef']
      psRegExp = ~/^[ a-z]*[ ]*([0-9]*)[ ]*[0-9]*.*/
    }
    def jps = DefaultProperties.JAVA_HOME + "${platform.sep}bin${platform.sep}jps"
    def jpsCommand = ['jps', '-mlvV']
    def jpsRegExp = ~/(\d+)\s+.*/

    // Clean ps for each identifier
    identifiers.each { identifier ->
      Process psProc = psCommand.execute()
      Process proc2 = ['grep', identifier].execute()
      Process excludeGrepProc = ['grep', '-v', 'grep'].execute()

      def filteredProc = psProc | proc2
      KILL_ALL_PROCESS_IDENTIFIERS_TO_IGNORE.each { it ->
        filteredProc |= ['grep', '-v', it].execute()
      }
      filteredProc |= excludeGrepProc

      pidsToKill.addAll(retrievePidsByRegexpFromProcOutput(filteredProc, psRegExp))
      if (new File(jps).exists()) {
        Process jpsProc = jpsCommand.execute()
        Process grepProc = ['grep', identifier].execute()
        def filteredJpsProc = jpsProc | grepProc
        KILL_ALL_PROCESS_IDENTIFIERS_TO_IGNORE.each { it ->
          filteredJpsProc |= ['grep', '-v', it].execute()
        }
        pidsToKill.addAll(retrievePidsByRegexpFromProcOutput(filteredJpsProc, jpsRegExp))
      }
    }

    log.trace("Ignoring NOE pid including its parents from selection for system wide killing " +
            "=> excluding ${noePidIncludingParentPids} from ${pidsToKill}")
    pidsToKill = pidsToKill.minus(noePidIncludingParentPids)

    killSpecifiedProcesses(pidsToKill)
  }


  static Set<Long> retrievePidsByRegexpFromProcOutput(Process proc, regexp) {
    Set<Long> pids = new HashSet<>()

    proc.in.eachLine { line ->
      log.debug("Found process to matching ${regexp}: ${line}")
      def match = line =~ regexp
      if (match.groupCount() > 0 && match.size() > 0 && match[0].size() > 0) {
        String pid = match[0][1]
        try {
          pids.add(Long.parseLong(pid))
        } catch (NumberFormatException ex) {
          log.debug("We don't care that line contained an invalid processCode to parse: ${pid}. Continuing...")
        }
      }
    }
    return pids
  }


  static Long extractPid(identifier) {
    log.debug("Extracting pid using identifier ${identifier}")
    List<Long> pids = extractPids(identifier, false)
    if (pids.isEmpty()) {
      return null
    }
    return pids.get(0)
  }

  static List<Integer> extractPids(identifier, getAll = true) {
    try {
      if (platform.isWindows()) {
        extractWindowsPids(identifier, getAll)
      } else {
        extractUNIXPids(identifier, getAll)
      }
    } catch (IllegalArgumentException ex) {
      if (log.isTraceEnabled()) {
        log.trace("Extracting pids for identifier ${identifier} failed with ${ex.getMessage()}, " +
                  "printing all running processes")
        Cmd.logSystemProcesses()
      }

      throw ex
    }
  }

  static List<Long> extractUNIXPids(identifier, getAll = true) {
    List<Long> pids = []
    final String ALL_FAILED = "All pid extraction options have failed, including the last resort 'pargs' one. This means that the application the pid of which we were trying to" +
        "extract hadn't been started in a supported way. Hint: domain.sh? any custom launch script?"
    /**
     * UNIX STUFF
     */
    List<String> psCommand
    Pattern psRegExp
    if (platform.isHP()) {
      psCommand = ['ps', '-elfx']
      psRegExp = ~/^[0-9]*[ ]*[A-Z][ ]*[a-z]*[ ]*([0-9]+).*/
    } else {
      psCommand = ['ps', '-ef']
      psRegExp = ~/^[ a-z+]*[ ]*([0-9]+)[ ]*[0-9]*.*/
    }

    /**
     * The idea behind HP-UX pid extraction is that we can't use /proc, Caliper is lame, pgrep does not show the whole
     * command line and neither does ps -elfx. What we need is a two steps approach: First, we get the shell process
     * and second, we hunt for its child java process. Neat, huh?
     *
     * Possible problems to address:
     *   - the process is only a java one and the .sh does not exist
     *   - there are multiple .sh processes with the identifier
     *   - the answer is correct right away and the second ps is wrong then, i.e. no children
     *   - domain.sh
     *
     * "Release notes"
     *  - Tested on HP-UX, Solaris 10, Solaris 11, RHEL6, RHEL7 with killing 1 out of 3 running EAP servers.
     */
    Process psProc = psCommand.execute()
    Process proc2 = ['grep', identifier].execute()
    // This is a big assumption, but I guess both EAP and Catalina have .sh start scripts...
    Process proc3 = ['grep', '.sh'].execute()
    Process proc4 = ['grep', '-v', 'grep'].execute()
    psProc | proc2 | proc3 | proc4
    proc4.waitFor()
    if (proc4.exitValue()) {
      /**
       * Hmm, it looks like we didn't find anything. Might happen with Tomcat on Solaris 10. Let's go this way then:
       */
      psProc = psCommand.execute()
      proc2 = ['grep', 'java'].execute()
      psProc | proc2
      proc2.waitFor()
      if (proc2.exitValue()) {
        throw new IllegalArgumentException("Extracting shell pid failed horribly: err:[${proc2.err.text}], out:[${proc2.text}]")
      }

      /**
       * Now, we will collect all java processes.
       */
      List<Long> javaPids = []
      proc2.in.eachLine { line ->
        def match = line =~ psRegExp
        log.trace("Java processes match.groupCount(): ${match.groupCount()}")
        if (match.groupCount() > 0) {
          log.trace("Java processes match[0][1]:${match[0][1]}")
        } else {
          throw new IllegalArgumentException("Extracting shell pid failed horribly, pid extraction regular expression had 0 matches.")
        }
        String pid = match[0][1]
        try {
          javaPids.add(Long.parseLong(pid))
        } catch (NumberFormatException ex) {
          log.error("Error trying to parse process ID from: \"${pid}\"")
          //ignore non-pid matches, consider the rest
          //throw ex
        }
      }

      /**
       * Now let's go with pargs. Warning: This is like the last resort fall--back.
       * It might not work if pargs ain't on your path, or on HP-UX...whatever.
       */
      javaPids.each { javaPid ->
        try {
          def processArgsCommand = ['pargs']
          if (platform.isRHEL()) { // On RHEL there is no pargs, lets try equivalent command for linux, see http://andunix.net/info/linux/pargs
            processArgsCommand = ['ps', 'ww', '-p']
          }
          processArgsCommand.add(javaPid)
          Process pargsproc = processArgsCommand.execute()
          pargsproc.waitFor()
          if (pargsproc.exitValue()) {
            log.debug(ALL_FAILED)
            return pids
          }
          pargsproc.in.eachLine { line ->
            if (line.contains(identifier.toString())) {
              pids.add(javaPid)
            }
            return
          }
        } catch (IOException ex) {
          throw new IOException(ALL_FAILED, ex)
        }
        if (!getAll) {
          return pids
        }
      }
    } else {
      String line = proc4.text
      def match = line =~ psRegExp
      log.trace("1) match.groupCount(): ${match.groupCount()}")
      log.trace("1) match proc4.text: ${line}")
      String pid = match[0][1]
      try {
        pids.add(Long.parseLong(pid))
      } catch (NumberFormatException ex) {
        log.error("Error trying to parse process ID from: \"${pid}\"")
        //ignore non-pid match
        //throw ex
      }
    }

    return pids
  }

  static List<Long> extractWindowsPids(identifier, getAll = true) {
    List<Long> pids = []

    /**
     * WINDOWS STUFF
     *
     * Returns something like: "cmd.exe","3968","RDP-Tcp#0","2","3,356 K","Running","KARM-VIRT1\Administrator","0:00:00","1471948789"
     * where 1471948789 is our identifier i.e. windowTitle
     */
    Pattern taskListPattern = ~/^"[^"]*","([0-9]*)","[^"]*","[0-9]*","[^"]*","[^"]*","[^"]*","[^"]*","([^"]*)".*/
    /**
     * Why not ["TASKLIST", "/V", "/FI", "\"IMAGENAME EQ CMD.EXE\"", "/FO", "CSV", "/NH"]  ?
     * Well, while standalone.sh with EAP spawns cmd.exe window with a windowTitle, Tomcat startup.bat spawns java.exe window.
     */
    List<String> possibleWindowTitlesIds = [identifier, "${Cmd.actualUser}: ${identifier}", "Administrator: ${identifier}"]

    List<String> command = ["cmd", "/C", "TASKLIST", "/V", "/FO", "CSV", "/NH"]
    //["TASKLIST", "/V", "/FO", "CSV", "/NH"]
    Map tasklistResult = Cmd.executeCommandConsumeStreams(command)
    if (tasklistResult.exitValue != 0) {
      throw new RuntimeException("Failed to run ${command}, failed with ${tasklistResult}")
    }
    tasklistResult.stdOut.eachLine { line ->
      if (line.contains("No tasks are running")) {
        throw new IllegalArgumentException("Extracting PID failed horribly with command ${command.toString()}: ${line}." +
                "If the command seems to be correct; i.e. it contains a string corresponding to the identifier: ${identifier}, you probably executed the testsuite directly from the CygWin environment;" +
                "thus disabling the WindowTitle functionality. One can start the testsuite from CygWin environment by executing cmd first and then typing run.bat.")
      }

      if (line.contains(String.valueOf(identifier))) {
        log.trace("WIDLE(identifier:${identifier}): Line:[${line}]")

        def match = line =~ taskListPattern

        log.trace("WIDLE match.groupCount(): ${match.groupCount()}")

        log.trace("WIDLE match SIZES:: match.size():${match.size()}")

        if (match.groupCount() > 1 && match.size() > 0 && match[0].size() >= 2) {
          log.trace("WIDLE match[0][0]:'${match[0][0]}'")
          log.trace("WIDLE match[0][1]:'${match[0][1]}'")
          log.trace("WIDLE match[0][2]:'${match[0][2]}'")
        }
        if (match.size() > 0 && match[0].size() >= 2 && possibleWindowTitlesIds.any { it == match[0][2]}) {
          String pid = match[0][1]
          try {
            pids.add(Long.parseLong(pid))
          } catch (NumberFormatException ex) {
            log.error("Error trying to parse process ID from: \"${pid}\"")
            throw ex
          }
        } else {
          log.error("WIDLE match didn't succeed :-) it was: match.size():${match.size()}")
        }
        if (!getAll) {
          return pids
        }
      }
    }
    return pids
  }

  static boolean killSpecifiedProcesses(Collection<Long> pidList = []) {
    if (!pidList) {
      log.debug("No process IDs given => nothing to kill")
      return false
    }
    def pidListAsCommandAttr = []
    def killUtilityCommand
    if (platform.isWindows()) {
      killUtilityCommand = ["taskkill", "/F"]
      pidList.each { pid ->
        pidListAsCommandAttr.addAll(['/pid', pid])
      }
    } else {
      killUtilityCommand = ["kill", "-9"]
      pidListAsCommandAttr = pidList
    }

    def killCommand = killUtilityCommand + pidListAsCommandAttr
    int exitCode
    if (JBFile.useAdminPrivileges) {
      exitCode = Cmd.executeSudoCommand(killCommand, new File("."))
    } else {
      exitCode = Cmd.executeCommand(killCommand, new File("."))
    }
    if (exitCode > 0) {
      log.warn("Unable to kill specified processes using command: ${killCommand}")
    }
    return exitCode == 0
  }

  /**
   * Create correct command.
   *
   * sudo su - tomcat -c "/.../startup.sh -security"
   * VS
   * /.../startup.sh -security
   */
  static List makeCommand(String runContext, String command) {
    return (runContext.trim() == '') ? (command.split() as List) : ((runContext.split() as List) + command)
  }

  /**
   * Wait until is PID removed from system - at max. timeout
   */
  static boolean waitForPidRemoved(Long pid, int timeout = 30) {
    if (!pid) return true
    int now = 0

    log.trace("waitForPidRemoved(): For pid=${pid} with timeout=${timeout}")
    while (now < timeout) {
      log.trace("waitForPidRemoved(): Iteration $now, all pids ${getPidList()}")

      if (!Cmd.pidExists(pid)) {
        log.trace("waitForPidRemoved(${pid}): Not found -> OK")
        return true
      }
      log.trace("waitForPidRemoved(${pid}): Found -> continuing")
      Library.letsSleep(1000)
      now++
    }

    // PID still exists
    return false
  }

  /**
   * Check if PID exists in a system - process is present in system
   */
  static boolean pidExists(Long pid) {
    def res = false

    try {
      res = getPidList().contains(pid)
    }
    catch (e) {
    }

    return res
  }

  /**
   * Checks periodically whether all defined pids disappeared from the system.
   * Useful for checking that the result of some kill command took effect.
   *
   * @return true if none of the pids provided exist in the system, false otherwise
   */
  static boolean waitForPidsRemoved(List<Long> pids, int timeout, TimeUnit timeUnit) {
    long maxTime = System.currentTimeMillis() + timeUnit.toMillis(timeout)
    boolean anyPidExist = !getPidList().intersect(pids).isEmpty()
    while (anyPidExist && System.currentTimeMillis() <= maxTime) {
      anyPidExist = !getPidList().intersect(pids).isEmpty()
      Library.letsSleep(42)
    }
    if (anyPidExist) {
      log.warn("There are still some pids not removed: ${getPidList().intersect(pids)}")
    }
    return !anyPidExist
  }

  /**
   * Check that at least one of the pids is in a system - processes are present in system
   */
  static boolean pidExists(List pids) {
    def res = false

    pids.each { pid ->
      res = res || pidExists(pid)
    }

    return res
  }

  /**
   * Check that all of the pids are in a system - processes are present in system
   */
  static boolean pidExistsAll(List pids) {
    def res = true

    pids.each { pid ->
      res = res && pidExists(pid)
    }

    return res
  }

  /**
   * Extract all PIDS from the underlying system
   */
  static List<Long> getPidList() {
    def res = []
    def row = []
    def command

    Process p
    if (platform.isWindows()) {
      command = 'tasklist /FO "CSV"'
    } else {
      // Sometimes output from ps could be limited - for instance for not sufficient width of terminal
      command = 'ps -ef' //OK at least for RHEL, Solaris10, Solaris11
    }
    //TODO: Command should always be an instance of List, not String.
    p = command.execute()
    def procList = p.text
    p.waitFor()

    procList.eachLine { line ->
      if (platform.isWindows()) row = line.split(",") as List
      else row = line.split() as List

      // on all tested system is PID in 2nd col
      if (!row[1].contains("PID")) res << Long.parseLong(row[1].replaceAll('"', ''))
    }

    return res
  }

  static void logProcessOutput(Process p) {
    final StringBuffer stdOutAsStr = new StringBuffer()
    final StringBuffer stdErrAsStr = new StringBuffer()
    StreamConsumer stdOut = new StreamConsumer(p.getInputStream(), stdOutAsStr)
    StreamConsumer stdErr = new StreamConsumer(p.getErrorStream(), stdErrAsStr)
    stdOut.start()
    stdErr.start()
    p.waitFor()
    stdOut.join(5000)
    stdErr.join(5000)
    log.debug("stdOut:${stdOutAsStr.toString()}")
    log.debug("stdErr:${stdErrAsStr.toString()}")
  }

  static void logSystemProcesses(String grepArgs = '') {
    Process p

    if (platform.isWindows()) {
      p = ["tasklist", "/V"].execute()
    } else {
      if (grepArgs.isEmpty()) {
        p = "ps -ef".execute()
      } else {
        p = "ps -ef".execute() | ["grep", grepArgs].execute()
      }
    }
    logProcessOutput(p)
  }

  static void logSystemOpenedPorts() {
    Process p

    if (platform.isWindows()) {
      p = ["netstat", "-an"].execute() | ["findstr", "LISTEN"].execute()
    } else if (platform.isSolaris() || platform.isHP()) {
      p = ["netstat", "-an"].execute() | ["grep", "-w", "LISTEN"].execute()
    } else if (platform.isRHEL7()) {
      // using absolute path to ss as for nologin shell the /usr/sbin is removed from path for non root user
      p = ["/usr/sbin/ss", "-ltnp"].execute()
    } else {
      p = ["netstat", "-vltnep"].execute()
    }
    logProcessOutput(p)
  }

  /**
   * returns copy of cmd.props, usefull for backing up
   */
  static Map createCopyOfCmdProps() {
    def propsBck = [:]
    propsBck << props
    return propsBck
  }
}
