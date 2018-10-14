package noe.common.newcmd;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static noe.common.newcmd.PsCmdData.PsArg.*;

/**
 * Builder class for creating command line for listing processes on system.
 * This builder is platform independent but it will create platform dependent command line.
 *
 * This is a dummy command builder just for one command. The result will go to stdout in unformatted way.
 *
 * This class produces the {@link noe.common.utils.Cmd} data structure
 *
 * This is platform specific and following commands will be run by platform (or different by setting method)
 * LINUXes: ps -A
 * WINDOWS: wmic process get, tasklist /v, wmic Win32_PerfFormattedData_PerfProc_Process get
 * see {@link PsCmdData}
 *
 */
public class PsCmdBuilder extends CmdBuilder<PsCmdBuilder> {

    private static final Logger log = LoggerFactory.getLogger(PsCmdBuilder.class);
    private Map<PsCmdData.PsArg,String[]> psCmdData;
    private Map<String, String> psEnvVars;
    private Map<PsCmdFormat, String> psCmdFormatArguments;
    private PsCmdData.WindowsPsType windowsType;

    private String userNameFilter; // null means no filter
    private Set<String> processIdFilter;  // null means no filter
    private Set<PsCmdFormat> formatOptions = new LinkedHashSet<PsCmdFormat>();

    public PsCmdBuilder() {
        this(PsCmdData.WindowsPsType.WMIC);
    }

    public PsCmdBuilder(final PsCmdData.WindowsPsType winType) {
        super("ps");

        this.windowsType = winType;
        psCmdData = PsCmdData.getPsArgs(windowsType);
        psEnvVars = PsCmdData.getPsEnvVars();
        psCmdFormatArguments = PsCmdData.getFormat(windowsType);
        setBaseCommand(psCmdData.get(BASE_COMMAND)[0]); // base command can be just a string
        addArguments(psCmdData.get(BASE_COMMAND_ARGS));
    }

    /**
     * Building command based on methods called on the ps builder ({@link PsCmdBuilder}).
     */
    @Override
    public CmdCommand build() {
        CmdCommand cmd = super.build();

        // filter: just one filter to use
        if(userNameFilter != null && !userNameFilter.isEmpty() && psCmdData.get(FILTER_USER).length > 0) {
            log.trace("Built ps command will be filtered on user {}", userNameFilter );
            String userFilterFormattedArg = String.format(psCmdData.get(FILTER_USER)[0], userNameFilter);
            addArgumentsToCmd(cmd, userFilterFormattedArg);
        } else if (processIdFilter != null && !processIdFilter.isEmpty()) {
            log.trace("Built ps command will be filtered by process ids {}", processIdFilter);
            injectPidFilter(cmd);
        } else {
            // show all if there is no other filter defined
            log.trace("Built ps command won't be filtered and will show all processes on system");
            addArgumentsToCmd(cmd, psCmdData.get(ALL_PROC));
        }

        // base command: filter arguments added wmic needs after filter word 'get'
        addArgumentsToCmd(cmd, psCmdData.get(BC_ARGS_AFTER_FILTER));

        // format: what will be shown (which columns) in output
        String formatArgs = getFormatArguments();
        String[] formatSwitch = psCmdData.get(FORMAT_SWITCH);
        if(!formatArgs.isEmpty() && formatSwitch != null) {
            addArgumentsToCmd(cmd, formatSwitch);
            addArgumentsToCmd(cmd, formatArgs);
        }

        // environment variables:
        cmd.getEnvProperties().putAll(psEnvVars);

        log.trace("Ps command built: " + cmd);
        return cmd;
    }

    /**
     * Adding format option that means that specific column will be part
     * of the output. The ordering of addFormatOption call
     * should be preserved.
     *
     * Be aware of the fact that not all formats are supported by all
     * available ps commands. This is mainly trouble of widows system.
     * Consult data in {@link PsCmdData}.
     *
     * @param formatOption  format option - a column which will be shown in output
     * @return this
     */
    public PsCmdBuilder addFormatOption(final PsCmdFormat formatOption) {
        formatOptions.add(formatOption);
        return this;
    }

    /**
     * Adding format options which switch on the presence a column in output.
     * The order of format option should be preserved.
     *
     * Be aware of the fact that not all formats are supported by all
     * available ps commands. This is mainly trouble of widows system.
     * Consult data in {@link PsCmdData}.
     *
     * @param formatOptionList  list of format options - a columns that will be shown in output
     * @return this
     */
    public PsCmdBuilder addFormatOptions(final PsCmdFormat... formatOptionList) {
        for(PsCmdFormat formatOption: formatOptionList) {
            addFormatOption(formatOption);
        }
        return this;
    }

    /**
     * Showing processes that belongs to user with specified name.
     * This resets all other filters.
     *
     * @return this
     */
    public PsCmdBuilder filterByUser(final String userName) {
        userNameFilter = userName;
        processIdFilter = null;
        return this;
    }

    /**
     * Adding process ids to list of process ids that filter the result.
     * See {@link #addPidsToFilter(String...)}
     */
    public PsCmdBuilder addPidsToFilter(final int... processIds) {
        String[] processIdsAsString = new String[processIds.length];
        for(int i = 0; i<processIds.length; i++) {
            processIdsAsString[i] = String.valueOf(processIds[i]);
        }
        return addPidsToFilter(processIdsAsString);
    }

    /**
     * Adding process ids to list of process ids that filter the result.
     * This resets all other filters.
     *
     * @param processIds  process ids to use as filter
     * @return  this
     */
    public PsCmdBuilder addPidsToFilter(final String... processIds) {
        if(processIdFilter == null) {
            processIdFilter = new LinkedHashSet<String>();
        }
        for(String processId: processIds) {
            Preconditions.checkArgument(processId.matches("\\d+"), "Process Id "+ processId + " has to be positive integer number");
            processIdFilter.add(processId);
        }
        // reseting all other filters
        userNameFilter = null;
        return this;
    }

    private String getFormatArguments() {
       StringBuilder sb = new StringBuilder();
       boolean first = true;

       for (PsCmdFormat formatOption : formatOptions) {
          String arg = psCmdFormatArguments.get(formatOption);
          if(arg != null && !arg.isEmpty()) {
              if (first) {
                  first = false;
               } else {
                  sb.append(",");
               }
              sb.append(arg);
          }
       }
       return sb.toString();
    }

    private void injectPidFilter(final CmdCommand cmd) {
        if(platform.isWindows() &&
                (windowsType == PsCmdData.WindowsPsType.WMIC || windowsType == PsCmdData.WindowsPsType.WMIC_PERF)) {
            cmd.addArgument("where");
            StringBuffer filterArg = new StringBuffer();
            filterArg.append("\"");

            boolean first = true;
            for (String processId : processIdFilter) {
                if (first) {
                    first = false;
                } else {
                    filterArg.append(" OR ");
                }
                String arg = String.format(psCmdData.get(FILTER_PROCESS_ID)[0], processId);
                filterArg.append(arg);
             }

            filterArg.append("\"");
            cmd.addArgument(filterArg.toString());
        } else if(platform.isWindows()) {  // Windows TASKLIST
            for(String processId: processIdFilter) {
                String arg = String.format(psCmdData.get(FILTER_PROCESS_ID)[0], processId);
                cmd.addArgument(arg);
            }
        } else {  // Linux
            for(String processId: processIdFilter) {
                cmd.addArgument(psCmdData.get(FILTER_PROCESS_ID)[0]);
                cmd.addArgument(processId);
            }
        }
    }
}
