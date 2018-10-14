package noe.common.newcmd;



/**
 * Interface of builders creating commands.
 * This is used by executor service to get built {@link CmdCommand}
 * that will be executed in the next step.
 */
public interface CmdBuilderInterface {

    /**
     * Builder method where {@link CmdCommand} is put as output of the builder "process".
     * This method needs to be implemented by any builder as it's used to get command
     *
     * @return built instance of {@link CmdCommand}.
     */
    CmdCommand build();
}
