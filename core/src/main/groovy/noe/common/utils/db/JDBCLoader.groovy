package noe.common.utils.db

import groovy.util.logging.Slf4j
/**
 * This is a helper class to fetch JDBC drivers from a shared
 * repository.
 *
 * Usage: In constructor, provide product, its version, the requested database
 * and JDBC version. Then use method load() to get the driver(s) - the one
 * attribute of the method is the target directory in which to store the files. 
 *
 * If the "rename" attribute has been set, the downloaded driver file will be 
 * named "jdbc_driver.jar".  If there is more than one driver file necessary for
 * that database, the others will be named jdbc_driver_${X}.jar, where ${X} is 
 * a number larger than 1. Otherwise, the file will be named as it was 
 * originally named. 
 *
 * The class has very descriptive exceptions, so when something goes wrong,
 * it should be immediately clear, what actually happened.
 *
 * @author lpetrovi , zroubali
 */
@Slf4j
class JDBCLoader {

  private URL URL = new URL(System.getProperty("JDBCLoader.URL"))
  private String product = "EAP"
  private String productVersion = "6.0.0"
  private String database = "oracle11gR2RAC"
  private Integer JDBCVersion = 4

  def JDBCLoader(String product, String productVersion, String database, Integer JDBCVersion) {
    this.product = product
    this.productVersion = productVersion
    this.database = database
    this.JDBCVersion = JDBCVersion
  }

  void verify() {
    // verify product URL
    def productURL = new URL(this.URL, this.product + "/")
    def connection = null
    try {
      connection = productURL.openStream()
    } catch (java.net.UnknownHostException ex) {
      throw new IllegalStateException("Unknown host: " + ex.message, ex)
    } catch (Exception ex) {
      throw new IllegalStateException("Invalid product name: " + this.product, ex)
    } finally {
      if (connection != null) connection.close()
    }
    // verify product version
    connection = null
    def productVersionURL = new URL(productURL, this.productVersion + "/")
    try {
      connection = productVersionURL.openStream()
    } catch (Exception ex) {
      throw new IllegalStateException("Invalid " + this.product + " version: " + this.productVersion, ex)
    } finally {
      if (connection != null) connection.close()
    }
    // verify database version
    connection = null
    def databaseURL = new URL(productVersionURL, this.database + "/")
    try {
      connection = databaseURL.openStream()
    } catch (Exception ex) {
      throw new IllegalStateException("Invalid " + this.product + " " + this.productVersion + " database: " + this.database, ex)
    } finally {
      if (connection != null) connection.close()
    }
    if (this.JDBCVersion != 3 && this.JDBCVersion != 4) {
      throw new IllegalStateException("Invalid JDBC version: " + this.JDBCVersion)
    }
  }

  private File downloadFile(URL f, File target, String name, boolean ignoreIfExists) throws IOException {
    def targetFile = new File(target, name)
    if (targetFile.exists() && ignoreIfExists) {
      log.info("Skipping " + f + " to " + targetFile.getAbsolutePath() + " because it already exists.")
    } else {
      log.info("Downloading " + f + " to " + targetFile.getAbsolutePath() + ".")

      new BufferedInputStream(f.openStream()).withCloseable { BufferedInputStream ins ->
        new FileOutputStream(targetFile).withCloseable { FileOutputStream fos ->
          new BufferedOutputStream(fos).withCloseable { BufferedOutputStream outs ->
            outs << ins
          }
        }
      }
    }
    return targetFile
  }

  Set<File> load(File target) {
    return load(target, false, true)
  }

  Set<File> load(File target, boolean rename, boolean ignoreIfExists) {
    verify()
    if (!target.isDirectory() || !target.canWrite()) {
      throw new IllegalArgumentException("Supplied file is not a writable directory: " + target.getAbsolutePath())
    }
    // get a list of files to fetch
    def rootURL = new URL(this.URL, this.product + "/" + this.productVersion + "/" + this.database + "/jdbc" + this.JDBCVersion + "/")
    def filesToDownload = new LinkedList<URL>()
    def metaInfURL = new URL(rootURL, "meta-inf.txt")
    try {
      def r = new BufferedReader(new InputStreamReader(metaInfURL.openStream()))
      String line
      while ((line = r.readLine()) != null) {
        filesToDownload.add(new URL(rootURL, line))
      }
    } catch (Exception ex) {
      throw new RuntimeException("Failed reading ${metaInfURL}!", ex)
    }
    def i = 1
    Set<File> files = new HashSet<File>()
    for (URL u : filesToDownload) {
      String name
      if (rename) {
        name = "jdbc_driver.jar"
        if (i > 1) {
          name = "jdbc_driver_" + i + ".jar"
        }
        i++
      } else {
        name = new File(u.getFile()).getName()
      }
      try {
        files.add(downloadFile(u, target, name, ignoreIfExists))
      } catch (Exception ex) {
        ex.printStackTrace()
        throw new RuntimeException("Failed downloading " + u, ex)
      }
    }
    return files
  }


}
