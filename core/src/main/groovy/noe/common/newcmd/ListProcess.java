package noe.common.newcmd;

import static noe.common.newcmd.PsCmdData.WindowsPsType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import noe.common.utils.Cmd;
import noe.common.utils.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This class is mentioned to be a wrapper of {@link PsCmdBuilder}
 * as for getting data in platform independent way it's needed to do
 * operations string command output.
 * <p>
 * The util class run different commands against the OS and the operation of listing
 * commands is not atomic. If there will be spawned new process during checking the
 * state the information could be a bit inconsistent.
 */
// TODO: pargs program on solaris and hpux to get whole list of command arguments (ps could be limited in string length)
public class ListProcess {
    private static final Logger log = LoggerFactory.getLogger(ListProcess.class);

    private final Platform platform = new Platform();

    /**
     * This is utility method for listing running processes on underlying system. It uses {@link PsCmdBuilder} to get
     * the command - on *unix it's ps on windows it's wmic.
     * Nevertheless there are limitation - for this being "portable" you can use just formating option passed as
     * argument in type of {@link PsCmdFormat}.
     * And there are another limitations for Windows {@link PsCmdFormat#USER} is skipped as output from
     * tasklist is hardly parsable.
     * There is one pitfall that needs to be take care of. For being able to parse all demanded data it's needed
     * in some cases run ps command several times with different parameters one by one. Between such run
     * there could be created a new process or old one could die. Then it could happend that some of the
     * listed processes does not contain absolutely all information that was demanded. Especially this
     * happends for Windows and CPU or Memory where wmic perf is used.
     *
     * @param formatOptions list of format options that defines what data about process will be returned
     * @return array of array of String in order as format options defined
     * @throws IOException when error on execution ps command occurs
     */
    public String[][] listAllAsArray(final PsCmdFormat... formatOptions) throws IOException {
        Collection<Map<PsCmdFormat, String>> listPsData = listAll(formatOptions).getAsList();
        String[][] listResult = new String[listPsData.size()][formatOptions.length];
        int psDataIndex = 0;
        for (Map<PsCmdFormat, String> dataEntry : listPsData) {
            for (int formatIndex = 0; formatIndex < formatOptions.length; formatIndex++) {
                listResult[psDataIndex][formatIndex] = dataEntry.get(formatOptions[formatIndex]);
            }
            psDataIndex++;
        }
        return listResult;
    }

    /**
     * Listing data running on system. Return type is inner data structure that offers sorting
     * and filtering
     * For information about limitations of this method see documentation at {@link #listAllAsArray(PsCmdFormat...)}
     *
     * @param formatOptions what data will be returned about processes
     * @return list of running processes on underlying system
     * @throws IOException when error on execution of ps command occurs
     */
    public ListProcessData listAll(final PsCmdFormat... formatOptions) throws IOException {
        ListProcessData data = new ListProcessData();

        // as there could be several calls of 'ps' command results - then number of listed processes
        // could be different in each run of 'ps' command
        // this says how much records are expected for each 'ps line' - others are filtered out
        int expectedNumberOfRecordsInListing = formatOptions.length;

        Set<PsCmdFormat> formatNotSeparated = new LinkedHashSet<PsCmdFormat>();
        Set<PsCmdFormat> formatSpaceSeparated = new LinkedHashSet<PsCmdFormat>();
        formatNotSeparated.add(PsCmdFormat.PROCESS_ID);
        for (PsCmdFormat psCmdFormat : formatOptions) {
            // if user is used then skip it - not able to parse tasklist output
            if (platform.isWindows() && psCmdFormat == PsCmdFormat.USER) {
                log.error("{} is not able to work with output of tasklist command - skipping USER format option", this.getClass());
                expectedNumberOfRecordsInListing--;
                continue;
            }
            if (psCmdFormat.isSpaceSeparated()) {
                formatSpaceSeparated.add(psCmdFormat);
            } else {
                formatNotSeparated.add(psCmdFormat);
            }
        }
        // there is just process id under list of format not separated
        if (formatNotSeparated.size() == 1 && formatSpaceSeparated.size() > 0) {
            PsCmdFormat cmdFormatToTransfer = formatSpaceSeparated.iterator().next();
            formatNotSeparated.add(cmdFormatToTransfer);
            formatSpaceSeparated.remove(cmdFormatToTransfer);
        }

        PsCmdBuilder builder = getPsCmdBuilder(formatNotSeparated);
        processPsOutput(data, builder, formatNotSeparated, true);
        for (PsCmdFormat withSpaceSeparatedRecord : formatSpaceSeparated) {
            Set<PsCmdFormat> separatedCycleSet = new LinkedHashSet<PsCmdFormat>();
            separatedCycleSet.add(PsCmdFormat.PROCESS_ID);
            separatedCycleSet.add(withSpaceSeparatedRecord);
            builder = getPsCmdBuilder(separatedCycleSet);
            processPsOutput(data, builder, separatedCycleSet, false);
        }

        // remove not complete 'ps lines' from listing (see description above)
        data.removeIncomplete(expectedNumberOfRecordsInListing);
        // log.trace("Listing: {}", data);
        return data;
    }

    /**
     * Returns collection of process ids which are in the process tree above the specified pid
     *
     * @param processId process id to get the parent process ids for including the process id itself;
     * @return set of process ids which are above specified process id in process tree including the process id itself
     * @throws IOException when not possible to run the process list command
     */
    public Collection<Integer> listParentPids(final int processId) throws IOException {
        ListProcessData data = listAll(PsCmdFormat.PROCESS_ID, PsCmdFormat.PARENT_PROCESS_ID, PsCmdFormat.COMMAND);

        Map<PsCmdFormat, String> currentProcessData = data.get(processId);
        if (currentProcessData == null) {
            return ImmutableSet.of(processId);
        }

        Set<String> actualParentPids = new HashSet<String>();
        actualParentPids.add(currentProcessData.get(PsCmdFormat.PROCESS_ID));
        log.trace("Actual PID data are {}", currentProcessData);
        while (currentProcessData != null &&
                !actualParentPids.contains(currentProcessData.get(PsCmdFormat.PARENT_PROCESS_ID))) {
            String parentPid = currentProcessData.get(PsCmdFormat.PARENT_PROCESS_ID);
            actualParentPids.add(parentPid);
            currentProcessData = data.get(Integer.parseInt(parentPid));
            log.trace("Current process data: {}", currentProcessData);
        }

        ImmutableSet.Builder<Integer> pidsBuilder = new ImmutableSet.Builder<Integer>();
        for (String pid : actualParentPids) {
            pidsBuilder.add(Integer.parseInt(pid));
        }
        ImmutableSet<Integer> parentPids = pidsBuilder.build();

        log.trace("Parent process ids of {} are: {}", processId, parentPids);
        return parentPids;
    }

    /**
     * Listing process tree of particular process id in form of list of process data.
     * Process data is a map where {@link PsCmdFormat} could be used to get particular information
     * about the process in the tree.
     * <p>
     * If the tree looks like:<br>
     * 1 - 2a<br>
     * &nbsp;&nbsp;- 2b - 3a<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- 3c<br>
     * Then order of result will be 3a 3c, 2b, 2a, 1<br>
     * <p>
     * WARN: please, do not enhance the parameters of listAll
     *
     * @param processId process id to get the tree for
     * @return list of map where information about processes in the tree is saved
     * @throws IOException when not possible to run the process list command
     */
    public List<Map<PsCmdFormat, String>> listProcessTree(final long processId) throws IOException {
        ListProcessData data = listAll(PsCmdFormat.PROCESS_ID, PsCmdFormat.PARENT_PROCESS_ID, PsCmdFormat.COMMAND);

        ImmutableList.Builder<Map<PsCmdFormat, String>> psTreeBuilder = ImmutableList.<Map<PsCmdFormat, String>>builder();

        Map<PsCmdFormat, String> rootProcessData = data.get(processId);
        if (rootProcessData == null) {
            Map<PsCmdFormat, String> record = ImmutableMap.<PsCmdFormat, String>builder()
                    .put(PsCmdFormat.PROCESS_ID, String.valueOf(processId)).build();
            return psTreeBuilder.add(record).build();
        }

        psTreeBuilder.add(rootProcessData);

        List<String> listOfIdsToCheck = new ArrayList<String>();
        List<String> listOfIdsInProgress = new ArrayList<String>();
        listOfIdsToCheck.add(Long.toString(processId));

        // passing through ps results to get all the process three (filter returns filtered copy of data)
        while (!listOfIdsToCheck.isEmpty()) {
            for (String processIdToCheck : listOfIdsToCheck) {
                ListProcessData filteredData = data.filterBy(PsCmdFormat.PARENT_PROCESS_ID, processIdToCheck);
                for (Map<PsCmdFormat, String> psRecord : filteredData.getAsList()) {
                    String processIdInProgress = psRecord.get(PsCmdFormat.PROCESS_ID);
                    // on windows could happen that process is its own parent
                    if (processIdInProgress != null && !psRecord.get(PsCmdFormat.PROCESS_ID).equals(psRecord.get(PsCmdFormat.PARENT_PROCESS_ID))) {
                        listOfIdsInProgress.add(processIdInProgress);
                    }
                    try {
                        psTreeBuilder.add(psRecord);  // adding data to result
                    } catch (NumberFormatException nfe) {
                        log.error("Not possible to parse process id {} to be added to demanded process list", processIdInProgress);
                        throw new RuntimeException("Not possible parse process id " + processIdInProgress, nfe);
                    }
                }
            }
            listOfIdsToCheck = listOfIdsInProgress;
            listOfIdsInProgress = new ArrayList<String>();
        }

        List<Map<PsCmdFormat, String>> psTree = psTreeBuilder.build().reverse();
        log.trace("Tree list of process {} consists {}", processId, psTree);
        return psTree;
    }

    /**
     * Listing information about process by id.
     *
     * @param processId process id that info should be listed about
     * @return process data of process id, parent process id and command with arguments
     * @throws IOException when some error on execution happens
     */
    public ListProcessData listProcessInfo(final int processId) throws IOException {
        ListProcessData data = listAll(PsCmdFormat.PROCESS_ID, PsCmdFormat.COMMAND_ARGS,
                PsCmdFormat.PARENT_PROCESS_ID, PsCmdFormat.USER);
        return data.filterBy(PsCmdFormat.PROCESS_ID, String.valueOf(processId));
    }

    /**
     * Returning simple string generated directly by ps or wmic command with information about
     * process put as argument.
     *
     * @param processId process id that info will be returned about
     * @return process info
     * @throws IOException when some error on execution happens
     */
    public String printProcessInfo(final long processId) throws IOException, InterruptedException {
        CmdBuilder<SimpleCmdBuilder> builder = new CmdBuilder<SimpleCmdBuilder>("");
        if (platform.isWindows()) {
            builder.setBaseCommand("wmic");
            builder.addArguments("process", "where", "\"ProcessId=" + processId + "\"", "get", "Commandline");
        } else if (platform.isHP()) {
            builder.setBaseCommand("ps");
            builder.addArguments("-p", String.valueOf(processId), "-fx");
        } else {
            builder.setBaseCommand("ps");
            builder.addArguments("-p", String.valueOf(processId), "-o", "pid,ppid,user,group,comm,args");
        }
        CmdCommand psCommand = builder.build();
        Map res = Cmd.executeCommandConsumeStreams(psCommand.getCommandLine(), psCommand.getWorkingDirectory(), null, 60000L, psCommand.getEnvProperties());
        return (String) res.get("stdOut");
    }

    /**
     * Depending on format options it returns configuration of PSCmdBuilder to be used.
     */
    private PsCmdBuilder getPsCmdBuilder(final Set<PsCmdFormat> formatOptions) {
        PsCmdBuilder psCmdBuilder;
        if (platform.isWindows()) {
            psCmdBuilder = new PsCmdBuilder(getWindowsPsType(formatOptions));
        } else {
            psCmdBuilder = new PsCmdBuilder();
        }
        return psCmdBuilder.addFormatOptions(formatOptions.toArray(new PsCmdFormat[formatOptions.size()]));
    }

    private PsCmdData.WindowsPsType getWindowsPsType(final Collection<PsCmdFormat> formatOptions) {
        if (formatOptions.contains(PsCmdFormat.USER)) {
            return WindowsPsType.TASKLIST;
        } else if (formatOptions.contains(PsCmdFormat.ELAPSED_TIME) || formatOptions.contains(PsCmdFormat.CPU)) {
            return WindowsPsType.WMIC_PERF;
        } else {
            return WindowsPsType.WMIC;
        }
    }

    /**
     * Reading from InputStream (closing it at the end) and each line as added under format option to process list.
     * The parsing is done by space and tabulator.
     */
    private void processPsOutput(final ListProcessData listing, final PsCmdBuilder builder, final Set<PsCmdFormat> formatOptionsAsSet,
                                 final boolean isCreating) throws IOException {
        CmdCommand psCommand = builder.build();
        Map res = Cmd.executeCommandConsumeStreams(psCommand.getCommandLine(), psCommand.getWorkingDirectory(), null, 60000L, psCommand.getEnvProperties());
        String stdout = (String) res.get("stdOut");


        // trouble of windows is that it does not sort the output column by
        // order that was put to wmic program (like wmic process get ProcessId, Name)
        // but in alphabetical order - the result of the command will be 'Idle process  0'
        List<PsCmdFormat> formatOptionsAsList = new ArrayList<PsCmdFormat>(formatOptionsAsSet);
        if (platform.isWindows()) {
            this.sortPsCmdFormat(formatOptionsAsList);
        }

        StringBuffer stringPattern = getFormatPattern(formatOptionsAsList);
        Pattern pattern = Pattern.compile(stringPattern.toString());

        int lineNumber = 0;
        List<String> lines = Arrays.asList(stdout.split(platform.getNl()));

        int linesOfHeader = getSizeOfHeader(formatOptionsAsList);
        for (String line : lines) {
            lineNumber++;
            // skipping header
            if (lineNumber <= linesOfHeader) {
                continue;
            }
            if (platform.isWindows()) {
                // java wrongly interprets end of line (it seems) - skipping empty line if it's
                if (line.trim().isEmpty()) {
                    continue;
                }
            }
            Matcher matcher = pattern.matcher(line.trim());
            if (!matcher.find()) {
                log.error("Error on getting process list data from output " + line.trim());
                continue;
            }
            Map<PsCmdFormat, String> listingEntry = new HashMap<PsCmdFormat, String>();
            int columnIndex = 1;
            for (PsCmdFormat psCmdFormat : formatOptionsAsList) {
                listingEntry.put(psCmdFormat, matcher.group(columnIndex));
                columnIndex++;
            }

            // listing passed as argument - adding data there
            // if wanted to created new bundle of data under listing data storage - meaning when
            // data does not exist they are created
            // isCreating == false means that if process_id does not exist in data storage
            // this line won't be added at all
            if (isCreating) {
                listing.add(listingEntry);
            } else {
                listing.addIfExists(listingEntry);
            }
        }
    }

    private int getSizeOfHeader(final Collection<PsCmdFormat> formatOptions) {
        if (platform.isWindows() && formatOptions.contains(PsCmdFormat.USER)) {
            // tasklist is used
            return 3;
        } else {
            return 1;
        }
    }

    private StringBuffer getFormatPattern(final Collection<PsCmdFormat> formatOptions) {
        StringBuffer stringPattern = new StringBuffer("^");
        for (PsCmdFormat format : formatOptions) {
            if (format.isSpaceSeparated()) {
                stringPattern.append("(.*?)[ \\t]*");
            } else {
                stringPattern.append("([^ \\t]+)[ \\t]*");
            }
        }
        stringPattern.append("$");
        return stringPattern;
    }

    private void sortPsCmdFormat(final List<PsCmdFormat> psCmdFormat) {
        final Map<PsCmdFormat, String> formatNameForOs = PsCmdData.getFormat(getWindowsPsType(psCmdFormat));
        Collections.sort(psCmdFormat, new Comparator<PsCmdFormat>() {
            @Override
            public int compare(final PsCmdFormat o1, final PsCmdFormat o2) {
                return formatNameForOs.get(o1).compareTo(formatNameForOs.get(o2));
            }
        });
    }
}
