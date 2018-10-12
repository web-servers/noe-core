package noe.common.utils.processid;

import com.sun.jna.Pointer;
import noe.common.utils.Platform;

import java.lang.reflect.Field;

/**
 * Util class that helps getting different information from Process object.
 *
 * Borrowed implementation of methods for retrieving processId from Process class from other Red Hat test suites
 */
public final class ProcessUtils {
    public static final int UNDEFINED_PROCESS_ID = -1;
    private static final Platform platform = new Platform();

    public static int getProcessId(final Process process) {
        if (platform.isWindows()) {
            return getWindowsProcessId(process);
        } else {
            return getUnixProcessId(process);
        }
    }

    /**
     * Returning process id when process is instance of windows process.
     *
     * @param process running process
     * @return process id
     */
    public static int getWindowsProcessId(final Process process) {
        int processId = UNDEFINED_PROCESS_ID;

        if (process.getClass().getName().equals("java.lang.Win32Process")
                || process.getClass().getName().equals("java.lang.ProcessImpl")) {
            /* determine the process id on windows plattforms */
            try {
                Field f = process.getClass().getDeclaredField("handle");
                f.setAccessible(true);
                long handl = f.getLong(process);

                Kernel32 kernel = Kernel32.INSTANCE;
                W32API.HANDLE handle = new W32API.HANDLE();
                handle.setPointer(Pointer.createConstant(handl));
                processId = kernel.GetProcessId(handle);
            } catch (Throwable e) {
                throw new RuntimeException("Not able to get process id on Windows " + processId, e);
            }
        }
        return processId;
    }

    /**
     * Returning process id when process is instance of unix process.
     *
     * @param process running process
     * @return process id
     */
    public static int getUnixProcessId(final Process process) {
        int processId = UNDEFINED_PROCESS_ID;

        if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
            /* get the PID on unix/linux systems */
            try {
                Field f = process.getClass().getDeclaredField("pid");
                f.setAccessible(true);
                processId = f.getInt(process);
            } catch (Throwable e) {
                throw new RuntimeException("Not able to get process id on Unix " + processId, e);
            }
        }

        return processId;
    }

    /**
     * Checks whether process is alive (still running) or not.
     * @param proc - process to be checked
     * @return true if the process is still alive, false otherwise.
     */
    public static boolean isProcAlive(Process proc) {
        try {
            proc.exitValue();
            // if exit value is returned => process was already terminated => returning false
            return false;
        } catch (IllegalThreadStateException ex) {
            // this exception is being thrown if the process is still alive => returning true
            return true;
        }
    }
}
