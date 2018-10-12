package noe.common.newcmd;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import noe.common.utils.Platform;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * Builder for creating commands.
 * <p>
 * This and its children are shortcut for creating {@link CmdCommand} by platform independent way. For formatted output and more
 * sophisticated cases, use util classes.
 * <p>
 * If you use builder or any child builder which depends on this basic class you can influence which environmental properties
 * are passed to run native command.
 *
 * @param <THIS> - return type of the builder methods, e.g. see {@link #addArgument(String)}
 */
@SuppressWarnings("unchecked")
public class CmdBuilder<THIS extends CmdBuilder<THIS>> implements CmdBuilderInterface {
    private final CmdCommand cmd;
    protected static final Platform platform = new Platform();

    private static final String USER_DIR_PROP_NAME = "user.dir";

    public static final String[] WINDOWS_SUFFIXES = new String[]{".bat", ".cmd"};

    private Map<String, String> envProperties;


    /**
     * Command builder for user defined base command.
     * <br>
     *
     * @param baseCommand base command of {@link CmdCommand} that will be created
     */
    public CmdBuilder(final String baseCommand) {
        this.cmd = new CmdCommand(baseCommand);
        // env properties could be immutable but we want to let user to change them
        envProperties = new HashMap<String, String>();
    }

    /**
     * Command builder for user defined command.
     * <br>
     *
     * @param wholeCommand command of {@link CmdCommand} that will be created
     */
    public CmdBuilder(final List wholeCommand) {
        if (wholeCommand.size() < 1) throw new IllegalArgumentException("The command must contain at least one item representing basecommand");
        String baseCommand = String.valueOf(wholeCommand.get(0));
        this.cmd = new CmdCommand(baseCommand);
        if (wholeCommand.size() > 1) {
            for (int i = 1; i < wholeCommand.size(); i++) {
                cmd.addArgument(String.valueOf(wholeCommand.get(i)));
            }
        }
        // env properties could be immutable but we want to let user to change them
        envProperties = new HashMap<String, String>();
    }


    /**
     * Main method of builder. From defined arguments and from other setter method builds {@link CmdCommand}.
     *
     * @return built instance of {@link CmdCommand}.
     */
    public CmdCommand build() {
        // setting working directory if user did not set it manually
        if (cmd.getWorkingDirectory() == null) {
            cmd.setWorkingDirectory(new File("."));
        }

        ImmutableList.Builder<String> prefix = new ImmutableList.Builder<String>();

        if (platform.isWindows()) {
            envProperties.put("NOPAUSE", "true");
        }

        cmd.setEnvProperties(envProperties);

        // if we are on widows and the command is absolute path then it's a command which
        // does not need being prefixed with 'cmd /c'. Furthermore it would cause troubles
        // when path with spaces would be used.
        // If it's a relative path to a command then we need to use 'cmd /c' as it manages
        // change of working directory or stuff around native commands appropriately
        if (platform.isWindows() && !new File(cmd.getBaseCommand()).exists()) {
            prefix.add("cmd", "/c");
        }


        cmd.setPrefix(prefix.build());
        return cmd;
    }

    public THIS setExitValues(final int[] values) {
        if (values != null) {
            cmd.setExitValues(values);
        }
        return (THIS) this;
    }

    /**
     * Adding argument to command NOT prefixed. Pass the call over {@link CmdCommand#addArgument(String)} method. But if argument is
     * empty (or null) it ignores this argument to be added.
     *
     * @param argument argument to be added to cmd
     * @return this
     */
    public THIS addArgument(final String argument) {
        if (argument != null && !argument.isEmpty()) {
            cmd.addArgument(argument);
        }
        return (THIS) this;
    }

    /**
     * Adding arguments to command NOT prefixed. If some of the arguments in list is empty (or null) such argument is ignored
     * from adding it to Cmd (to pass it to {@link CmdCommand#addArgument(String)} method).
     *
     * @param arguments arguments to be added to cmd
     * @return this
     */
    public THIS addArguments(final String... arguments) {
        for (String argument : arguments) {
            addArgument(argument);
        }
        return (THIS) this;
    }

    /**
     * Adding arguments to command NOT prefixed. Each argument is transformed to String using {@link Object#toString()} method.
     * If some of the arguments in list is null or its String representation is empty such argument is ignored from adding it
     * to Cmd (to pass it to {@link CmdCommand#addArgument(String)} method).
     *
     * @param arguments arguments to be added to cmd
     * @return this
     */
    public THIS addArguments(final Iterable<Object> arguments) {
        for (Object arg : arguments) {
            if (arg != null) {
                addArgument(arg.toString());
            }
        }
        return (THIS) this;
    }

    /**
     * Set based command on inner {@link CmdCommand} instance.
     *
     * @param baseCommand base command to set
     * @return this
     */
    public THIS setBaseCommand(final String baseCommand) {
        cmd.setBaseCommand(baseCommand);
        return (THIS) this;
    }

    /**
     * Setting working directory for built command.
     *
     * @param workDir where the command will be executed at
     * @return this
     */
    public THIS setWorkDir(final File workDir) {
        cmd.setWorkingDirectory(workDir);
        return (THIS) this;
    }

    /**
     * Check whether command contains argument. If match found then argument is returned (first match).
     *
     * @param regexp regular expression used for searching arguments
     * @return first match of found argument
     */
    public Optional<String> containsArgument(final String regexp) {
        Pattern p = Pattern.compile(regexp);

        for (String arg : cmd.getArguments()) {
            if (p.matcher(arg).matches()) {
                return Optional.of(arg);
            }
        }

        return Optional.absent();
    }

    /**
     * Removes env variables based on settings.
     *
     * @param sourceEnv   source list of env variables
     * @param envToRemove env variables to remove from source list
     */
    private Map<String, String> filterEnv(final Map<String, String> sourceEnv, final List<String> envToRemove) {
        Map<String, String> filteredEnv = new HashMap<String, String>();
        for (Entry<String, String> sourceEnvEntry : sourceEnv.entrySet()) {
            if (!envToRemove.contains(sourceEnvEntry.getKey())) {
                filteredEnv.put(sourceEnvEntry.getKey(), sourceEnvEntry.getValue());
            }
        }
        return filteredEnv;
    }

    /**
     * Putting arguments to cmd but on difference to {@link CmdCommand#addArguments(String...)} it
     * check whether arguments are not empty. If so, then such argument
     * is ignored to be added to cmd.
     * <p>
     * Used by childern builders on copied cmd.
     */
    protected CmdCommand addArgumentsToCmd(final CmdCommand cmd, final String... arguments) {
        for (String argument : arguments) {
            if (argument != null && !argument.isEmpty()) {
                cmd.addArgument(argument);
            }
        }
        return cmd;
    }
}
