package noe.common.utils.processid;

import com.sun.jna.Pointer;
import noe.common.utils.Java;
import noe.common.utils.Platform;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 * Util class that helps getting different information from Process object.
 *
 * Borrowed implementation of methods for retrieving processId from Process class from other Red Hat test suites
 */
public final class ProcessUtils {
    public static final long UNDEFINED_PROCESS_ID = Long.MIN_VALUE;
    private static final Platform platform = new Platform();

    public static long getProcessId(final Process process) {
        if (Java.isJdkXOrHigher("9")) {
            return getProcessIdJava9AndNewer(process);
        }

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
    public static long getWindowsProcessId(final Process process) {
        if (process.getClass().getName().equals("java.lang.Win32Process")
                || process.getClass().getName().equals("java.lang.ProcessImpl")) {
            /* determine the process id on windows platforms */
            try {
                Field f = process.getClass().getDeclaredField("handle");
                f.setAccessible(true);
                long handl = f.getLong(process);

                Kernel32 kernel = Kernel32.INSTANCE;
                W32API.HANDLE handle = new W32API.HANDLE();
                handle.setPointer(Pointer.createConstant(handl));
                return kernel.GetProcessId(handle);
            } catch (Throwable e) {
                throw new RuntimeException("Not able to get process id of process " + process, e);
            }
        }
        return UNDEFINED_PROCESS_ID;
    }

    /**
     * Returning process id when process is instance of unix process.
     *
     * @param process running process
     * @return process id
     */
    public static long getUnixProcessId(final Process process) {
        long processId = UNDEFINED_PROCESS_ID;

        if (process.getClass().getName().equals("java.lang.UNIXProcess")
                || process.getClass().getName().equals("java.lang.ProcessImpl")) {
            /* get the PID on unix/linux systems */
            try {
                Field f = process.getClass().getDeclaredField("pid");
                f.setAccessible(true);
                processId = f.getLong(process);
            } catch (Throwable e) {
                throw new RuntimeException("Not able to get process id of process " + process, e);
            }
        }

        return processId;
    }

    private static long getProcessIdJava9AndNewer(final Process process) {
        try {
            // although the method is public, we are using reflection to execute it to be able to compile on JDK8
            // as the method exists only in JDK9+
            return (long) Process.class.getDeclaredMethod("pid").invoke(process);
        } catch (SecurityException | NoSuchMethodException e) {
            throw new RuntimeException("Not able to get process id of process " + process, e);
        }
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
