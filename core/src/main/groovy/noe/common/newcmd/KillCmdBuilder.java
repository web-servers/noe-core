package noe.common.newcmd;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder class for creating command line for killing a system process by id.
 * This builder will create platform dependent command.
 *
 * This class produces the {@link noe.common.newcmd.CmdCommand} data structure
 *
 */
public class KillCmdBuilder extends CmdBuilder<KillCmdBuilder> {
    private boolean isForce = false;
    private List<String> processIdsToKill = new ArrayList<String>();

    public KillCmdBuilder() {
        super("kill");
        setBaseCommand(getExecutable());
    }

    @Override
    public CmdCommand build() {
        CmdCommand cmd = super.build();

        if(isForce) {
            if(platform.isWindows()) {
                cmd.addArgument("/f");
            } else {
                cmd.addArgument("-9");
            }
        }

        // process ids to kill are arguments and have to be passes on
        // command line as last ones
        for(String processId: processIdsToKill) {
            if(platform.isWindows()) {
                cmd.addArgument("/PID");
            }
            cmd.addArgument(processId);
        }

        return cmd;
    }

    /**
     * Passing list of process ids (positive integer numbers) to be killed.
     * There will be left the order of process ids that are put in input array
     * and ids will be pass on command line in such order.
     *
     * @param processIds  list of process ids to kill
     * @return this
     */
    public KillCmdBuilder addProcessId(final String... processIds) {
        for(String processId: processIds) {
            Preconditions.checkArgument(processId.matches("\\d+"), "Process id "+ processId + " has to be positive integer number");
            processIdsToKill.add(processId);
        }
        return this;
    }

    /**
     * Adding process ids to kill as integers. For details see {@link #addProcessId(String...)}.
     *
     * @param processIds  process ids to kill as integer
     * @return  this
     */
    public KillCmdBuilder addProcessId(final int... processIds) {
        for(int processId: processIds) {
            String stringProcessId = String.valueOf(processId);
            addProcessId(stringProcessId);
        }
        return this;
    }

    /**
     * The kill will be provided forcibly
     * (meaning kill -9 or taskkill /f)
     *
     * @return this
     */
    public KillCmdBuilder setForce() {
       isForce = true;
       return this;
    }

    private String getExecutable() {
        if(platform.isWindows()) {
            return "taskkill";
        } else {
            return "kill";
        }
    }
}
