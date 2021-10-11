package noe.common.newcmd;

/**
 * This represents all the columns that portable ps utility could produce.
 *
 * The portable term means that not every information is easily accessible on all supported
 * platforms and those that are not accessible (or the functionality was not implemented yet)
 * are not listed here.
 *
 * Please for ps listing functionality use util class (TODO: will be added as part of JBQA-10920).
 *
 * You can use {@link PsCmdBuilder} to get information in raw text format. Builder on its own creates
 * each time just one command and especially on windows you need to use more commands to get
 * all information what you need.
 *
 */
public enum PsCmdFormat {
    // linux: ps, windows: wmic
    /**
     * Command name (without argumens).
     */
    COMMAND(true, String.class),
    /**
     * Command with all arguments.
     */
    COMMAND_ARGS(true, String.class),
    /**
     * Id of process.
     */
    PROCESS_ID(false, Long.class),
    /**
     * Pid of process parent.
     */
    PARENT_PROCESS_ID(false, Long.class),
    /**
     * Priority (nice) of the process.
     */
    PRIORITY(false, Integer.class),
    /**
     * Cumulative CPU time.
     */
    TIME(true, String.class),
    /**
     * Controlling terminal for process.
     * For windows this will show session id.
     */
    TTY(false, Integer.class),
    /**
     * Virtual memory size of process in kilobytes.
     */
    VIRT_MEM(false, Integer.class),

    // windows: wmic path Win32_PerfFormattedData_PerfProc_Process
    /**
     * Percentage how much the cpu is loaded by process
     * This has to be space separated to algorithm of {@link ListProcess} class
     * would not shuffle wmic and wmic perf data
     */
    CPU(true, String.class),
    /**
     * How much time elapsed from start of the process
     */
    ELAPSED_TIME(true, String.class),

    // windows: tasklist
    /**
     * User running this command.
     * Note: windows does not know much about ruser and
     *       I didn't find way to easily get group so there is left just
     *       this column to be listed as platform independent record.
     * This is not supported in {@link ListProcess} - not easy to parse tasklist.
     */
    USER(true, String.class);



    /**
     * Says that the value could contain spaces. Meaning e.g. process id is always
     * one number that can't be separated by space in ps listing.
     * On the other hand command with args will be space separated for sure
     */
    private boolean isSpaceSeparated;

    /**
     *
     * Type that the result will be tried to cast to e.g. for ordering.
     */
    private Class<?> dataType;

    PsCmdFormat(final boolean isSpaceSeparated, final Class<?> castType) {
        this.isSpaceSeparated = isSpaceSeparated;
        this.dataType = castType;
    }

    /**
     * Returning information if the column in listing could be space separated or not.
     * This is important for parsing results mainly for utility class {@link ListProcess}
     *
     * @return true if parsed value for ps column could contain spaces, false otherwise
     */
    public boolean isSpaceSeparated() {
        return this.isSpaceSeparated;
    }

    /**
     * Returns type of the value (when it's parsed) that we can try to convert to.
     * See {@link ListProcessData} comparator.
     *
     * @return  class type that we can convert the parsed value of column to
     */
    public Class<?> getDataType() {
        return this.dataType;
    }
}
