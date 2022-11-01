package noe.server

import noe.eap.server.as7.AS7Properties
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

class AS7UndeployTest {

    private static final String as7Dir = "test-as7"

    private static File baseDir
    private static File deploymentsDir

    private static AS7 testServer

    @BeforeClass
    static void prepare() {
        baseDir = File.createTempDir('noe', 'ServerAbstract')
        assertNotNull(baseDir)
        deploymentsDir = new File(baseDir, "${as7Dir}/standalone/deployments")
        assertNotNull(deploymentsDir)
        deploymentsDir.mkdirs()

        //some random value, necessary for AS7 to instantiate (not optimal)
        System.setProperty("eap.version", "7.0.0")
        testServer = new AS7(baseDir.getAbsolutePath(), as7Dir)
    }

    @AfterClass
    static void cleanUp() {
        testServer?.kill()
        baseDir?.deleteDir()
        System.clearProperty("eap.version")
    }

    @Test
    void undeployByDeletingTestEar() {
        final String appName = "my-test-app123"
        final String appNameArchive = "ear"

        final File deploymentItself = prepareDeployment(appName, appNameArchive)
        final List<File> markerFiles = prepareAllMarkerFiles(appName, appNameArchive)

        //let's undeploy
        testServer.undeployByDeleting(appName)

        markerFiles.each { file ->
            assertFalse(file.exists())
        }
        assertFalse(deploymentItself.exists())
    }

    @Test
    void undeployByDeletingTestWar() {
        final String appName = "my-test-app123"
        final String appNameArchive = "war"

        final File deploymentItself = prepareDeployment(appName, appNameArchive)
        final List<File> markerFiles = prepareAllMarkerFiles(appName, appNameArchive)

        //let's undeploy
        testServer.undeployByDeleting(appName)

        markerFiles.each { file ->
            assertFalse(file.exists())
        }
        assertFalse(deploymentItself.exists())
    }

    private File prepareDeployment(final String appName, final String appNameArchive) {
        final File deploymentItself = new File(deploymentsDir, "${appName}.${appNameArchive}")
        deploymentItself.createNewFile()
        assertTrue(deploymentItself.exists())
        return deploymentItself
    }


    private List<File> prepareAllMarkerFiles(final String appName, final String appNameArchive) {
        final List<File> markerFiles = []

        AS7Properties.DEPLOYMENT_SCANNER_MARKER_FILES.each { markerSuffix ->
            final File markerFile = new File(deploymentsDir, "${appName}.${appNameArchive}${markerSuffix}")
            markerFile.createNewFile()
            markerFiles.add(markerFile)
            assertTrue(markerFile.exists())
        }

        return markerFiles
    }
}
