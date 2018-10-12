package noe.common.utils

import noe.common.TestUtils
import org.junit.AfterClass
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test

class GitUtilsTest {

    private static final String TEST_REPO_URL = "git://github.com/schacon/grit.git"

    private static File TEST_DIR

    private static File TEST_BRANCH_DIR

    @BeforeClass
    static void createTestDirectories() {
        TEST_DIR = new File(TestUtils.getMavenTargetDir(), "test-grit")
        TEST_BRANCH_DIR = new File(TestUtils.getMavenTargetDir(), "test-grit-branch")
    }

    @AfterClass
    static void deleteTestDirectories() throws Exception {
        if (TEST_DIR != null) {
            JBFile.delete(TEST_DIR)
        }
        if (TEST_BRANCH_DIR != null) {
            JBFile.delete(TEST_BRANCH_DIR)
        }
    }

    @Test
    void testCheckout() throws Exception {
        Assert.assertTrue(GitUtils.cloneCheckout(TEST_REPO_URL, null, TEST_DIR, ["LICENSE"]))
        Assert.assertTrue("Target directory " + TEST_DIR.getAbsolutePath() + " doesn't exist", TEST_DIR.exists())

        File fileToVerify = new File(TEST_DIR, "LICENSE")
        Assert.assertTrue("File " + fileToVerify.getAbsolutePath() + " doesn't exist", fileToVerify.exists())
    }

    @Test
    void testBranchCheckout() throws Exception {
        Assert.assertTrue(GitUtils.cloneCheckout(TEST_REPO_URL, "integration", TEST_BRANCH_DIR, ["README.txt"]))

        Assert.assertTrue("Target directory " + TEST_BRANCH_DIR.getAbsolutePath() + " doesn't exist.", TEST_BRANCH_DIR.exists())

        File fileToVerify = new File(TEST_BRANCH_DIR, "README.txt")
        Assert.assertTrue("File " + fileToVerify.getAbsolutePath() + " doesn't exist.", fileToVerify.exists())
    }

}
