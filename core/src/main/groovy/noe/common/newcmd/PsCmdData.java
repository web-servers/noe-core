package noe.common.newcmd;


import noe.common.utils.Platform;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Class that contains all data needs to construct the ps command for all supported platforms.
 * This means that you find here formatting option, filtering options etc.
 */
@SuppressWarnings("serial")
public class PsCmdData {

    private static final Platform platform = new Platform();

    /**
     * If windows is under use then it's possible to specify what
     * command should be used for showing ps info.
     */
    public enum WindowsPsType {
        WMIC,
        WMIC_PERF,
        TASKLIST
    }

    /**
     * This specifies information that you can get from data class
     * for specific platform.
     */
    public enum PsArg {
        BASE_COMMAND, // base command (ps or wmic or tasklist)
        BASE_COMMAND_ARGS,  // wmic needs arguments
        BC_ARGS_AFTER_FILTER,  // wmic needs to put args after filter
        ALL_PROC, // ps -A means all and does not consider any filter (-u, -p)
        // if filter is used then -A can't be
        FORMAT_SWITCH,  // what if format option for listing just some columns
        FILTER_USER,  // show just processes owned by one user
        FILTER_PROCESS_ID  // show processes with particular process id
    }

    // TODO: "tasklist /FO \"CSV\" /FI \"Username eq ${uids.join(', ')}\"" and "ps -f -U ${uids.join(', ')}"

    // Maps of PS commands on particular platforms. If there is null as value of some of the
    // argument then the command does not support such argument
    // What to think about:
    //  no header param: some of linuxes support --no-header, tasklist has /nh, wmic does not support at all
    private static final Map<PsArg, String[]> LINUX_PS = new HashMap<PsArg, String[]>() {{
        put(PsArg.BASE_COMMAND, new String[]{"ps"});
        put(PsArg.BASE_COMMAND_ARGS, new String[]{""});
        put(PsArg.BC_ARGS_AFTER_FILTER, new String[]{""});
        put(PsArg.ALL_PROC, new String[]{"-A"}); // if -A then all proc shown but filter (-u,-p) is not used
        put(PsArg.FORMAT_SWITCH, new String[]{"-o"});
        put(PsArg.FILTER_USER, new String[]{"-u %1$s -U %1$s"});
        put(PsArg.FILTER_PROCESS_ID, new String[]{"-p"});
    }};
    private static final Map<PsArg, String[]> WINDOWS_WMIC = new HashMap<PsArg, String[]>() {{
        put(PsArg.BASE_COMMAND, new String[]{"wmic"});
        put(PsArg.BASE_COMMAND_ARGS, new String[]{"process"});
        put(PsArg.BC_ARGS_AFTER_FILTER, new String[]{"get"});
        put(PsArg.ALL_PROC, new String[]{}); // add nothing and all procs are shown
        put(PsArg.FORMAT_SWITCH, new String[]{});
        put(PsArg.FILTER_USER, new String[]{}); // NOT possible to filter by user with wmic
        put(PsArg.FILTER_PROCESS_ID, new String[]{"ProcessId=%s"}); // 'where' not defined
    }};
    private static final Map<PsArg, String[]> WINDOWS_WMIC_PERF = new HashMap<PsArg, String[]>(WINDOWS_WMIC) {{
        put(PsArg.BASE_COMMAND_ARGS, new String[]{"path", "Win32_PerfFormattedData_PerfProc_Process"});
        put(PsArg.FILTER_PROCESS_ID, new String[]{"IDProcess=%s"}); // 'where' not defined
    }};
    private static final Map<PsArg, String[]> WINDOWS_TASKLIST = new HashMap<PsArg, String[]>() {{
        put(PsArg.BASE_COMMAND, new String[]{"tasklist"});
        put(PsArg.BASE_COMMAND_ARGS, new String[]{"/v"});
        put(PsArg.BC_ARGS_AFTER_FILTER, new String[]{""});
        put(PsArg.ALL_PROC, new String[]{}); // add nothing and all procs are shown
        put(PsArg.FORMAT_SWITCH, new String[]{});  // NOT possible to format output of tasklist command
        put(PsArg.FILTER_USER, new String[]{"/FI \"USERNAME eq %s\""});
        put(PsArg.FILTER_PROCESS_ID, new String[]{"/FI \"PID eq %s\""});
    }};
    private static final Map<PsArg, String[]> SOLARIS_PS = new HashMap<PsArg, String[]>(LINUX_PS) {{
    }};
    private static final Map<PsArg, String[]> HPUX_PS = new HashMap<PsArg, String[]>(LINUX_PS) {{
        put(PsArg.FILTER_USER, new String[]{"-u %s"});
    }};
    private static final Map<PsArg, String[]> AIX_PS = new HashMap<PsArg, String[]>(LINUX_PS) {{
    }};
    private static final Map<PsArg, String[]> MAC_PS = new HashMap<PsArg, String[]>(LINUX_PS) {{
    }};


    // Maps of format strings that could be used to define which column to show in listing
    // Note:
    //   - when empty string value is defined then format option is not supported for the command on platform
    //   - WINDOWS_TASKLIST does not support formatting of output
    private static final Map<PsCmdFormat, String[]> FORMAT = new HashMap<PsCmdFormat, String[]>() {{
        // OS command matrix:                          LINUX[0]  SOLARIS[1] HPUX[2]  AIX[3]   MAC[4]    WMIC[5]             WMIC_PERF[6]           EMPTY[7]
        put(PsCmdFormat.COMMAND, new String[]{"comm", "comm", "comm", "%c", "comm", "Name", "Name", ""});
        put(PsCmdFormat.COMMAND_ARGS, new String[]{"args", "args", "args", "%c %a", "args", "CommandLine", "", ""});
        put(PsCmdFormat.PROCESS_ID, new String[]{"pid", "pid", "pid", "%p", "pid", "ProcessId", "IDProcess", ""});
        put(PsCmdFormat.PARENT_PROCESS_ID, new String[]{"ppid", "ppid", "ppid", "%P", "ppid", "ParentProcessId", "", ""});
        put(PsCmdFormat.PRIORITY, new String[]{"pri", "pri", "pri", "%n", "pri", "Priority", "PriorityBase", ""});
        put(PsCmdFormat.TIME, new String[]{"time", "time", "time", "%x", "time", "UserModeTime", "", ""});
        put(PsCmdFormat.TTY, new String[]{"tty", "tty", "tty", "%y", "tty", "SessionId", "", ""});
        put(PsCmdFormat.VIRT_MEM, new String[]{"vsz", "vsz", "vsz", "%z", "vsz", "VirtualSize", "VirtualBytes", ""});
        put(PsCmdFormat.CPU, new String[]{"pcpu", "pcpu", "pcpu", "%C", "pcpu", "", "PercentProcessorTime", ""});
        put(PsCmdFormat.ELAPSED_TIME, new String[]{"etime", "etime", "etime", "%t", "etime", "", "ElapsedTime", ""});
        put(PsCmdFormat.USER, new String[]{"user", "user", "user", "%U", "user", "", "",/*tasklist needed*/  ""});
        // memory percentage is trouble to get on aix and windows, leaving out for now
        // put(PsCmdFormat.MEM,          new String[]{"pmem",   "pmem",    "",      "",      "pmem",   "",                 "",                     ""});
    }};

    /**
     * Returning map of arguments for ps command based on platform.
     *
     * @param winPsType when we are on windows (only at such case) what ps command
     *                  will be used
     * @return map of ps command arguments
     */
    static Map<PsArg, String[]> getPsArgs(final WindowsPsType winPsType) {
        if (platform.isLinux()) {
            return LINUX_PS;
        } else if (platform.isHP()) {
            return HPUX_PS;
        } else if (platform.isAix()) {
            return AIX_PS;
        } else if (platform.isMac()) {
            return MAC_PS;
        } else if (platform.isSolaris()) {
            return SOLARIS_PS;
        } else if (platform.isWindows()) {
            switch (winPsType) {
                case TASKLIST:
                    return WINDOWS_TASKLIST;
                case WMIC:
                    return WINDOWS_WMIC;
                case WMIC_PERF:
                    return WINDOWS_WMIC_PERF;
                default:
                    throw new IllegalArgumentException("The Ps format builder does not support given windows PS type: " + winPsType);
            }
        } else {
            throw new IllegalArgumentException("The Ps command builder does not support os type: " + platform.getOsName());
        }
    }

    /**
     * Return environment variables to be used for the ps command based on platform.
     *
     * @return map of environment variables
     */
    static Map<String, String> getPsEnvVars() {
        if (platform.isHP()) {
            Map<String, String> result = new HashMap<String, String>();
            result.put("UNIX95", "1");
            return result;
        } else {
            return new HashMap<String, String>();
        }
    }

    /**
     * Returning formating options by platform.
     * In case of running on windows returning formating options for wmic command.
     *
     * @return map of format option and appropriate argument which will go to command line
     */
    public static Map<PsCmdFormat, String> getFormat() {
        return getFormat(WindowsPsType.WMIC);
    }

    static Map<PsCmdFormat, String> getFormat(final WindowsPsType winPsType) {
        if (platform.isLinux()) {
            return getFormatAtIndex(0);
        } else if (platform.isSolaris()) {
            return getFormatAtIndex(1);
        } else if (platform.isHP()) {
            return getFormatAtIndex(2);
        } else if (platform.isAix()) {
            return getFormatAtIndex(3);
        } else if (platform.isMac()) {
            return getFormatAtIndex(4);
        } else if (platform.isWindows()) {
            switch (winPsType) {
                case TASKLIST:
                    // all format options are empty for tasklist
                    return getFormatAtIndex(7);
                case WMIC:
                    return getFormatAtIndex(5);
                case WMIC_PERF:
                    return getFormatAtIndex(6);
                default:
                    throw new IllegalArgumentException("The Ps format builder does not support given windows PS type: " + winPsType);
            }
        } else {
            throw new IllegalArgumentException("The Ps format builder does not support os type: " + platform.getOsName());
        }
    }

    private static Map<PsCmdFormat, String> getFormatAtIndex(final int index) {
        Map<PsCmdFormat, String> res = new HashMap<PsCmdFormat, String>();
        for (Entry<PsCmdFormat, String[]> entry : FORMAT.entrySet()) {
            res.put(entry.getKey(), entry.getValue()[index]);
        }
        return res;
    }
}

