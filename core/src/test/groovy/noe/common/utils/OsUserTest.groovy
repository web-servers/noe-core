package noe.common.utils

import org.junit.After
import org.junit.Assert
import org.junit.Assume
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class OsUserTest {

  OsUser osUser

  @BeforeClass
  static void beforeClass() {
    Platform platform = new Platform()
    Assume.assumeFalse("Testing non Windows functionality.", platform.isWindows())
    Assume.assumeTrue("Tests need admin privileges to correct work.", JBFile.useAdminPrivileges)
  }

  @Before
  void beforeTest() {
    osUser = new OsUser("testUser")
  }

  @Test
  void osUserAddNotExistingUserTest() {
    Cmd.executeSudoCommandConsumeStreams(['userdel', osUser.getUsername()])
    Assert.assertTrue("osUser.add() fail. Expected return value <true>, but was <false>", addUser())
    Assert.assertTrue("User wasn't created", (Cmd.executeSudoCommandConsumeStreams(['getent', 'passwd', osUser.getUsername()]).exitValue) == 0)
  }

  @Test
  void osUserAddAlreadyExistingUserTest() {
    Cmd.executeSudoCommandConsumeStreams(['adduser', osUser.getUsername()])
    Assert.assertFalse("User was created twice!", addUser())
  }

  @Test
  void osUserDelAlreadyExistingUserTest() {
    Cmd.executeSudoCommandConsumeStreams(['adduser', osUser.getUsername()])
    Assert.assertTrue("Existing user wasn't removed!", deleteUser())
    Assert.assertFalse("User wasn't removed", (Cmd.executeSudoCommandConsumeStreams(['getent', 'passwd', osUser.getUsername()]).exitValue) == 0)
  }

  @Test
  void osUserDelNotExistingUserTest() {
    Cmd.executeSudoCommandConsumeStreams(['userdel', osUser.getUsername()])
    Assert.assertFalse("NonExisting user was removed!", deleteUser())
  }

  private Boolean addUser() {
    return osUser.create()
  }

  private Boolean deleteUser() {
    return osUser.remove()
  }

  @After
  void tearDown() {
    Cmd.executeSudoCommandConsumeStreams(['userdel', osUser.getUsername()])
  }
}
