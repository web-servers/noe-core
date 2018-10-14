package noe.common;

import java.io.File;

public class TestUtils {

    private TestUtils() {
    }

    /**
     * Get source directory for test resources for current maven module.
     *
     * @return Test resources source directory.
     */
    public static File getTestResourcesDir() {
        return new File(getMavenRootDirectory(), "src/test/resources");
    }

    /**
     * Get 'target' directory for current maven module.
     *
     * @return Module's target directory
     */
    public static File getMavenTargetDir() {
        File mavenTargetDir = new File(getMavenRootDirectory(), "target");

        if (!mavenTargetDir.exists()) {
            mavenTargetDir.mkdir();
        }
        return mavenTargetDir;
    }

    /**
     * Get root directory for current maven module.
     *
     * @return Module's root directory
     */
    public static File getMavenRootDirectory() {
        String classesDir = TestUtils.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        return new File(classesDir + "../..");
    }

}
