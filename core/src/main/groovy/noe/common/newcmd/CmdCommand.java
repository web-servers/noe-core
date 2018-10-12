package noe.common.newcmd;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * DTO class which represent an external command to be executed.
 * It consist at first from base-command which is string (e.g. java) and then with arguments
 * which each is defined separately in list of string (e.g. [-Xms, 512m]). Exec framework (in our case common exec)
 * will take care of escaping each parameter separately and correctly.
 * The last important part is prefix which servers when base command is need to be executed with
 * some special settings (e.g. UNIX95=1 on HPUX or with sudo). Prefix is prepended on command line before base-command.
 *
 */
public class CmdCommand {
    private static final Logger log = LoggerFactory.getLogger(CmdCommand.class);
    String baseCommand;
    File workingDirectory;
    List<String> arguments = new ArrayList<String>();
    List<String> prefix;
    int[] exitValues = new int[0];
    Map<String, String> envProperties;

    /**
     * Create empty new command without any specific settings.
     *
     * @param baseCommand Base program name to execute.
     */
    public CmdCommand(final String baseCommand) {
        this.baseCommand = baseCommand;
    }

    /**
     * Create a command with parameters added as list of string.
     *
     * @param baseCommand  Base program name to execute.
     * @param arguments  List of parameters that will be added on command line as params to base command.
     */
    public CmdCommand(final String baseCommand, final String... arguments) {
        this(baseCommand);
        this.arguments = new ArrayList<String>(Arrays.asList(arguments));
    }

    /**
     * Copy constructor - creating a duplicate the passed cmd.
     */
    public CmdCommand(final CmdCommand cmd) {
        log.trace("Creating copy of cmd '{}' with object hash '{}'", cmd, ((Object) cmd).hashCode());
        this.setBaseCommand(cmd.getBaseCommand());
        for(String argumentForDefensiveCopy: cmd.getArguments()) {
            this.addArgumentNotLogged(argumentForDefensiveCopy);
        }
        this.setWorkingDirectoryNotLogged(new File(cmd.getWorkingDirectory().getAbsolutePath()));
        if(cmd.getPrefix().isPresent()) {
            List<String> prefixAsDefensiveCopy = new ArrayList<String>();
            for(String prefixPart: cmd.getPrefix().get()) {
                prefixAsDefensiveCopy.add(new String(prefixPart));
            }
            this.setPrefixNotLogged(prefixAsDefensiveCopy);
        }
        Map<String, String> envPropertiesAsDefensiveCopy = new HashMap<String, String>();
        for(Entry<String,String> entryForDefensiveCopy: cmd.getEnvProperties().entrySet()) {
            envPropertiesAsDefensiveCopy.put(entryForDefensiveCopy.getKey(), new String(entryForDefensiveCopy.getValue()));
        }
        this.setEnvProperties(envPropertiesAsDefensiveCopy);
        this.setExitValues(cmd.getExitValues());
    }

    public CmdCommand setExitValues(final int[] values) {
        exitValues = values;
        return this;
    }

    /**
     * Add single argument
     *
     * @param argument adds single param or switch
     * @return this
     */
    public CmdCommand addArgument(final String argument) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(argument),
                "Cannot add empty parameter to basecommand: " + baseCommand);
        log.trace("For {} adding argument {}", baseCommand, argument);
        addArgumentNotLogged(argument);
        return this;
    }

    /**
     * Add multiple arguments one by one.
     *
     * @param arguments  arguments to be added to cmd
     * @return this
     */
    public CmdCommand addArguments(final String... arguments) {
        for(String argument: arguments) {
            this.addArgument(argument);
        }

        return this;
    }

    /**
     * Set root directory for command execution.
     *
     * @param directory Root directory for the process.
     *                  If not set, new File('.') will be used
     */
    public CmdCommand setWorkingDirectory(final File directory) {
        Preconditions.checkNotNull(directory);
        Preconditions.checkArgument(directory.isDirectory(), "File " + directory + " is not a directory");
        setWorkingDirectoryNotLogged(directory);
        log.trace("Cmd {} working directory is being set to {}", baseCommand, directory);
        return this;
    }

    /**
     * Build command line into string list (useful for creating {@link Process} instances.
     *
     * @return Whole command line as list of strings.
     */
    public List<String> getCommandLine() {
        ImmutableList.Builder<String> cmdListBuilder = ImmutableList.builder();
        if (prefix != null) {
            cmdListBuilder.addAll(prefix);
        }
        cmdListBuilder.add(baseCommand);
        cmdListBuilder.addAll(arguments);
        return cmdListBuilder.build();
    }

    /**
     * Returning prefix of the command. It's used when you want to run base command
     * with some specific settings. E.g. with sudo or on HPUX with UNIX95=1 etc.
     *
     * @return  list of strings which will be used as prefix of basecommand
     */
    Optional<List<String>> getPrefix() {
        return Optional.fromNullable(prefix);
    }

    /**
     * Setting prefix strings. See {@link #getPrefix()}.
     * If null is passed then prefix is set as empty (the same as empty list)
     *
     * @param prefix  list of string that will be put before base command
     *                when command will be put on command line
     */
    void setPrefix(final List<String> prefix) {
        log.trace("Cmd {} prefix is being set to {}", baseCommand, prefix);
        this.setPrefixNotLogged(prefix);
    }

    /**
     * Setting prefix strings. See {@link #getPrefix()}.
     *
     * @param prefix  array of strings that will be put before base command
     *                when command will be put on command line
     */
    public void setPrefix(final String[] prefix) {
        log.trace("Cmd {} prefix is being set to {}", baseCommand, prefix);
        this.setPrefixNotLogged(Arrays.asList(prefix));
    }

    /**
     * Returns whether prefix of the command is defined or not
     */
    public boolean hasPrefix() {
        return getPrefix().isPresent();
    }


    /**
     * Build command line into single string.
     *
     * @return While command line as string.
     */
    @Override
    public String toString() {
        return Joiner.on(" ").join(getCommandLine());
    }

    private void setPrefixNotLogged(final List<String> prefix) {
        this.prefix = prefix;
    }

    private void addArgumentNotLogged(final String argument) {
        arguments.add(argument);
    }

    private void setWorkingDirectoryNotLogged(final File directory) {
        this.workingDirectory = directory;
    }

    public String getBaseCommand() {
        return baseCommand;
    }

    public void setBaseCommand(String baseCommand) {
        this.baseCommand = baseCommand;
    }

    public File getWorkingDirectory() {
        return workingDirectory;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public void setArguments(List<String> arguments) {
        this.arguments = arguments;
    }

    public int[] getExitValues() {
        return exitValues;
    }

    public Map<String, String> getEnvProperties() {
        return envProperties;
    }

    public void setEnvProperties(Map<String, String> envProperties) {
        this.envProperties = envProperties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CmdCommand)) return false;

        CmdCommand that = (CmdCommand) o;

        if (getBaseCommand() != null ? !getBaseCommand().equals(that.getBaseCommand()) : that.getBaseCommand() != null)
            return false;
        if (getWorkingDirectory() != null ? !getWorkingDirectory().equals(that.getWorkingDirectory()) : that.getWorkingDirectory() != null)
            return false;
        if (getArguments() != null ? !getArguments().equals(that.getArguments()) : that.getArguments() != null)
            return false;
        if (getPrefix() != null ? !getPrefix().equals(that.getPrefix()) : that.getPrefix() != null) return false;
        if (!Arrays.equals(getExitValues(), that.getExitValues())) return false;
        return getEnvProperties() != null ? getEnvProperties().equals(that.getEnvProperties()) : that.getEnvProperties() == null;
    }

    @Override
    public int hashCode() {
        int result = getBaseCommand() != null ? getBaseCommand().hashCode() : 0;
        result = 31 * result + (getWorkingDirectory() != null ? getWorkingDirectory().hashCode() : 0);
        result = 31 * result + (getArguments() != null ? getArguments().hashCode() : 0);
        result = 31 * result + (getPrefix() != null ? getPrefix().hashCode() : 0);
        result = 31 * result + Arrays.hashCode(getExitValues());
        result = 31 * result + (getEnvProperties() != null ? getEnvProperties().hashCode() : 0);
        return result;
    }
}
