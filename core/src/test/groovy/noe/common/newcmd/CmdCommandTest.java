package noe.common.newcmd;


import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class CmdCommandTest {

    private CmdCommand cmd;

    @Before
    public void initialize() {
        cmd = new CmdCommand("test");
        cmd.addArgument("-v").addArgument("-d");
        cmd.addArgument("-x");
    }

    @Test
    public void testParams() throws Exception {
        List<String> cmdList = cmd.getCommandLine();
        assertFalse("Built command line should not be empty", cmdList.isEmpty());
        assertEquals("Built command line should contain all switches", 4, cmdList.size());
        assertEquals("Built command line should contain proper command", "test", cmdList.get(0));
        assertTrue("Built command line should contain proper switches", cmdList.contains("-v"));
        assertTrue("Built command line should contain proper switches", cmdList.contains("-d"));
        assertTrue("Built command line should contain proper switches", cmdList.contains("-x"));
    }

    @Test
    public void testCopyConstructor() {
        cmd.setEnvProperties(System.getenv());
        cmd.setPrefix(new String[] {"sudo", "-i"});
        cmd.setWorkingDirectory(new File(System.getProperty("java.io.tmpdir")));

        CmdCommand copyCmd = new CmdCommand(cmd);
        assertEquals("Copy of cmd has to be the same as preset cmd", cmd, copyCmd);
        assertFalse("Cmds can't be the same object instance", cmd == copyCmd);
        assertFalse("Cmds env can't be the same object instance", cmd.getEnvProperties() == copyCmd.getEnvProperties());
        assertFalse("Cmds working dir can't be the same object instance", cmd.getWorkingDirectory() == copyCmd.getWorkingDirectory());
        // do not testing strings as they could point to the same object instance but are immutable
    }
}
