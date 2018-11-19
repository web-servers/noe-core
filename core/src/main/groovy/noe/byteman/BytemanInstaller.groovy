package noe.byteman

import noe.common.utils.JBFile
import noe.common.utils.PathHelper

import java.nio.file.Files
import java.nio.file.Paths

/**
 * Helping class for Byteman to handle copying byteman.jar file to visible location in the system or to user defined file.
 */

class BytemanInstaller {
  protected static String DEFAULT_BYTEMAN_JAR_NAME = "byteman.jar"
  protected static String DEFAULT_BYTEMAN_JAR_FOLDER = PathHelper.join(System.getProperty("java.io.tmpdir"), "noe", "byteman")

  private File bytemanJar = null

  BytemanInstaller() {
    this(new File(DEFAULT_BYTEMAN_JAR_FOLDER, DEFAULT_BYTEMAN_JAR_NAME))
  }

  BytemanInstaller(File targetBytemanJarFile) {
    bytemanJar = targetBytemanJarFile
  }

  /**
   * Copies internal byteman.jar file to defined not physically existing target file (default: `${java.io.tmpdir}/noe/byteman/byteman.jar`)
   */
  File prepareBytemanJar() {
    if (!bytemanJar.exists()) {
      if (!bytemanJar.getParentFile().exists()) {
        JBFile.mkdir(bytemanJar.getParentFile())
      }

      JBFile.makeAccessible(bytemanJar.getParentFile())
      InputStream inputStream = Byteman.class.getResourceAsStream(DEFAULT_BYTEMAN_JAR_NAME)
      Files.copy(inputStream, Paths.get(bytemanJar.getPath()))
    }

    return bytemanJar
  }
}
