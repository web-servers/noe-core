package noe.common.utils

import groovy.util.logging.Slf4j
import noe.common.Constants
import noe.common.DefaultProperties
import org.apache.commons.io.FileUtils

import java.util.concurrent.TimeUnit

/**
 * @author Jan Stefl     <jstefl@redhat.com>
 */
@Slf4j
class JBFile {

  static AntBuilder ant = new AntBuilder()
  static Platform platform = new Platform()
  static int hasZip = -1
  static Boolean useAdminPrivileges = Boolean.valueOf(Library.getUniversalProperty('USE_ADMIN_PRIVILEGES', String.valueOf(DefaultProperties.RUN_WITH_SUDO)))

  static {
    ant.project.buildListeners[0].messageOutputLevel = 1
  }

  private JBFile() {
  }

  /**
   * Remove all dirs and files form directory.
   */
  static boolean cleanDirectory(File dir, Boolean trySudo = false) {
    log.debug("Cleaning content of directory " + dir.getAbsolutePath())
    if (!dir?.exists()) {
      return false
    }
    def ret = [:]
    // try to delete with ant
    try {
      ant.delete(includeemptydirs: true) {
        fileset(dir: dir) { include(name: '**/*') }
      }
      if (dir.listFiles().length > 0) {
        throw new RuntimeException("Directory content should be removed but it isn't")
      }
    } catch (e) {
      log.debug("Cleaning directory content with sudo privileges")
      if (trySudo || useAdminPrivileges) {
        if (!platform.isWindows()) {
          ret = Cmd.executeSudoCommandConsumeStreams(["/bin/bash", "-c", "rm -rf ${dir}/*"], new File('.'))
          if (ret['exitValue'] == 0) {
            return true
          }
        }
        // TODO administrator cleaning for Windows
      }
    }

    // Remove symlinks
    dir.listFiles().each {file ->
      file.delete()
      if (!platform.isWindows() && file.exists() && (trySudo || useAdminPrivileges)) {
        ret = Cmd.executeSudoCommandConsumeStreams(["rm", "-rf", "${file.absolutePath}"], new File('.'))
        if (ret['exitValue'] != 0) {
          throw new RuntimeException("Cannot remove file or directory: ${file.getAbsolutePath()}")
        }
      }
    }
    return true
  }

  /**
   * Copy whole content of source directory to destination directory.
   *
   * @param dir existing source directory
   * @param destDir destination directory, will be created if not exists yet
   * @param dereference whether to follow symbolic links (on non-windows platforms)
   * @param preserveRights whether to preserve rights for copied files and directories
   * @return false if source directory doesn't exist or copying fails at any point, true otherwise
   */
  static boolean copyDirectoryContent(File dir, File destDir, Boolean dereference = false, Boolean preserveRights = true) {
    log.debug("Copying ${dir.absolutePath} content to ${destDir.absolutePath}, dereference: ${dereference}, preserveRights: ${preserveRights}")
    if (!dir.exists()) {
      log.warn("Source dir doesn't exist ${dir.absolutePath}")
      return false
    }

    if (!destDir.exists()) {
      JBFile.mkdir(destDir)
    }

    /**
     * Linux/Unix Note: File permissions are not retained when files are copied;
     * they end up with the default UMASK permissions instead. This is caused by
     * the lack of any means to query or set file permissions in the current Java runtimes.
     * If you need a permission-preserving copy function, use a native command, like cp :-)
     */
    if (platform.isWindows()) {
      // Well, let's try it the native way instead :-)
      def command = ["xcopy", "${dir.absolutePath}${File.separator}.", destDir.absolutePath, "/H", "/S", "/E", "/Y", "/C", "/I", "/F", "/R", "/K", "/X"]
      return Cmd.executeCommand(command, new File('.')) == 0
    } else {
      def command = ["cp", "-r", "${dir.absolutePath}${File.separator}.", destDir.absolutePath]
      if (preserveRights) command.addAll(1, "-p")
      if (dereference) command.addAll(1, "-L")
      def result = (useAdminPrivileges) ? Cmd.executeSudoCommand(command, new File('.')) : Cmd.executeCommand(command, new File('.'))
      if (result != 0) {
        log.debug("Failed copy directory as whole. Trying again one subdir at a time...")
        dir.eachFile {
          command = ["cp", "-r", it.absolutePath, "${destDir.absolutePath}/"]
          if (preserveRights) command.addAll(1, "-p")
          if (dereference) command.addAll(1, "-L")
          if (useAdminPrivileges) {
            result = Cmd.executeSudoCommand(command, new File('.'))
          } else {
            result = Cmd.executeCommand(command, new File('.'))
          }
          if (result != 0) return false
        }
      }
    }
    return true
  }

  /**
   * Copy file to destination directory.
   *
   * @param file file or directory to be copied
   * @param destDir destination directory, will be created if not exists yet
   * @param trySudo only for API backward compatibility, unused
   * @param preserveRights whether to preserve rights for copied files and directories
   * @param dereference whether to follow symbolic links (on non-windows platforms)
   * @return false if file not exists, can't create destination directory or copy fails, true otherwise
   */
  static boolean copy(File file, File destDir, Boolean trySudo = false, Boolean preserveRights = true, Boolean dereference = false) {
    if (!file.exists()) return false

    if (!destDir.exists() && !JBFile.mkdir(destDir)) {
      return false
    }

    def returnValue = -1

    if (platform.isWindows()) {
      File dest = file.isDirectory() ? new File(destDir, file.name) : destDir

      def command = ["xcopy", "${file.absolutePath}", dest.absolutePath, "/H", "/S", "/E", "/I", "/Y", "/C", "/F", "/R", "/K", "/X"]
      returnValue = Cmd.executeCommand(command, new File('.'))
      if (returnValue > 0) {
        try {
          // try to copy with ant
          if (file.isDirectory()) {
            ant.copy(todir: destDir.getAbsolutePath(), overwrite: true) { fileset(dir: file.getAbsolutePath()) }
          } else {
            ant.copy(file: file.getAbsolutePath(), todir: destDir.getAbsolutePath(), overwrite: "true")
          }
          returnValue = 0
        } catch (e) {
          log.trace("JBFIle.copy with ant failed", e)
          returnValue = 2
        }
      }
    } else {
      def command = ["cp", "-r", "${file.absolutePath}", destDir.absolutePath]
      if (preserveRights) command.addAll(1, "-p")
      if (dereference) command.addAll(1, "-L")
      returnValue = Cmd.executeCommand(command, new File('.'))
      if (returnValue != 0 && (useAdminPrivileges)) {
        returnValue = Cmd.executeSudoCommand(command, new File('.'))
      }
    }
    return returnValue == 0
  }

  /**
   * Copy file to specified destination (file).

   * @param src source file
   * @param dest destination path of copied file
   * @param trySudo only for API backward compatibility, unused
   * @param preserveRights whether to preserve rights for copied file
   * @param dereference whether to follow symbolic links (on non-windows platforms)
   * @return false if src not exists, src is not single file or copy fails, true otherwise
   */
  static boolean copyFile(File src, File dest, Boolean trySudo = false, Boolean preserveRights = true, Boolean dereference = false) {
    if (!src.exists() || !src.isFile()) {
      return false
    }

    def returnValue = 0
    try {
      // try to copy with ant
      ant.copy(file: src.getAbsolutePath(), tofile: dest.getAbsolutePath(), overwrite: "true")
    } catch (e) {
      // Try it with sudo rights
      if (!platform.isWindows()) {
        def args = (preserveRights) ? '-p' : ''
        args += (dereference) ? ' -L' : ''
        if (useAdminPrivileges) {
          returnValue += Cmd.executeSudoCommand("cp -r ${args} ${src.absolutePath} ${dest}", new File('.'))
        } else {
          returnValue += Cmd.executeCommand("cp -r ${args} ${src.absolutePath} ${dest}", new File('.'))
        }
      } else {
        returnValue = -1
      }
    }
    return returnValue == 0
  }

  /**
   * Create new file with defined content. Existing file will be overwritten.
   *
   * @param file path where to create file
   * @param content desired content of created file
   * @param trySudo only for API backward compatibility, unused
   * @return whether creation was successful
   */
  static boolean createFile(File file, String content = '', Boolean trySudo = false) {
    if (file.exists()) JBFile.delete(file)

    try {
      file.write(content)
    } catch (e) {
      if (!platform.isWindows() && (useAdminPrivileges)) {
        Process p
        p = [
            'sudo',
            'touch',
            file.getAbsolutePath()
        ].execute()
        p.waitFor()

        JBFile.makeAccessible(file, true)
        if (content) {
          file.write(content)
        }
      }
    }

    return file.exists()
  }

  /**
   * Move file or whole directory to destination directory.
   *
   * @param file file or directory to move
   * @param destDir destination directory
   * @param trySudo only for API backward compatibility, unused
   * @return false if file doesn't denote existing file or directory or moving fails, true if everything was moved successfully
   */
  static boolean move(File file, File destDir, Boolean trySudo = false) {
    if (!file.exists()) return false

    if (!destDir.exists()) {
      try {
        ant.mkdir(dir: destDir)
      } catch (e) {
        if (useAdminPrivileges) Cmd.executeSudoCommand("mkdir ${destDir}", new File('.'))
      }

    }

    def result = false
    try {
      // try to copy with ant
      ant.move(file: file.getAbsolutePath(), todir: destDir.getAbsolutePath())
      result = true
    } catch (e) {
      // Try it with sudo rights
      if (!platform.isWindows()) {
        if (useAdminPrivileges) result = !Cmd.executeSudoCommand("mv ${file.getAbsolutePath()} ${destDir}", new File('.'))
        else result = !Cmd.executeCommand("mv ${file.getAbsolutePath()} ${destDir}", new File('.'))
      }
    }
    return result
  }

  static boolean moveToFile(File file, File dest, Boolean trySudo = false) {
    if (!file.exists()) return false

    def result = false
    try {
      // try to rename with ant
      ant.move(file: file.getAbsolutePath(), tofile: dest.getAbsolutePath())
      result = true
    } catch (e) {
      // Try it with sudo rights
      if (!platform.isWindows()) {
        if (trySudo || useAdminPrivileges) result = !Cmd.executeSudoCommand("mv ${file.getAbsolutePath()} ${dest}", new File('.'))
        else result = !Cmd.executeCommand("mv ${file.getAbsolutePath()} ${dest}", new File('.'))
      }
    }
    return result
  }

  /**
   * Delete file or directory.
   *
   * @param file file or directory to delete
   * @return whether deletion was successful
   */
  static boolean delete(File dir, Boolean trySudo = false) {
    log.debug("Deleting file {}", dir.getAbsolutePath())
    if (!dir.exists() && !fileIsSymlinkWithoutExistingDest(dir)) {
      log.debug("File {} doesn't exist, probably already deleted", dir.getAbsolutePath())
      return false
    }

    if (!platform.isWindows()) {
      List<String> cmd = ['rm', '-rf', dir.getAbsolutePath()]
      if (useAdminPrivileges) {
        Cmd.executeSudoCommandConsumeStreams(cmd)
      } else {
        Cmd.executeCommandConsumeStreams(cmd)
      }
    } else {
      if (dir.isDirectory()) {
        Cmd.executeCommandConsumeStreams(['cmd', '/c', 'rmdir',  '/s', '/q', dir.getAbsolutePath()])
      } else {
        Cmd.executeCommandConsumeStreams(['cmd', '/c', 'del', '/q', '/f', dir.getAbsolutePath()])
      }
    }

    return !dir.exists()
  }

  /**
   * Check for file existence for case when the file is symlink pointing to non existing target
   * @param file file to check
   * @return true if the file is not recognized as existing but it is known file of the parent directory
   * => it is symlink pointing to no target
   */
  private static boolean fileIsSymlinkWithoutExistingDest(File file) {
    if (file.exists()) {
      return false
    } else {
      File[] kids = file.getParentFile()?.listFiles()
      return kids != null && Arrays.asList(kids).contains(file)
    }
  }

  /**
   * Appends lines of text to specified position in file, works only for nonempty files
   * @param path represents path to the file which shall be updated
   * @param textToInsert text to be inserted to the specified position (line)
   * @param position representing line to which the text should be added (if -1 is specified the text is appended to the end of the file
   */
  static void insertTextToSpecifiedPositionInFile(String path, String textToInsert, int position) {
    File file = new File(path)
    if (file.exists()) {
      //Creating copy of file rather than creating new empty file, to preserve file original permissions
      log.debug('Updating file ' + file.absolutePath + " by putting content ${textToInsert} on row number ${position}")
      File tmpFile = new File(file.getParent(), file.getName() + 'tmp' + new Date().getTime())
      copyFile(file, tmpFile)
      tmpFile.deleteOnExit()

      // If admin privileges are used, mainly on Solaris, there is possibility that file won't be
      // accessible for user under whom runs noe (tomcat-users-xml, ...).
      // Add rights to be able to perform this task, change back after
      FilePermission rightsBackup = null
      if (!platform.isWindows() && useAdminPrivileges && (!tmpFile.canRead() || !tmpFile.canWrite())) {
        rightsBackup = retrievePermissions(file)
        chmod('o+rw', tmpFile)
      }

      Writer writer
      Reader reader
      try {
        if (position == -1) {
          log.debug('Appending text ' + textToInsert + ' to the end of the file ' + file.absolutePath)
          tmpFile.append(platform.nl + textToInsert + platform.nl) // newline at the start to be sure that we are appending on the empty line
        } else {
          FileUtils.write(tmpFile, "") // Clean the file so we can rewrite it from the original with added line on specified location
          writer = tmpFile.newWriter()
          reader = file.newReader()
          String line
          int currentLine = 0
          while ((line = reader.readLine()) != null) {
            currentLine++
            if (currentLine == position) {
              writer.write(textToInsert)
              writer.newLine()
            }
            writer.write(line)
            writer.newLine()
          }
          writer.flush()
        }
        if (tmpFile.size() > file.size()) {
          log.debug("File size of tmp file with appended content is greater then original file => appending was successful," +
                  " replacing original file by the file with appended content")
          if (rightsBackup && !platform.isWindows()) {
            definePermissions(rightsBackup, tmpFile)
          }
          delete(file)
          copyFile(tmpFile, file)
          delete(tmpFile)
        } else {
          log.error("The newly created file ${tmpFile.absolutePath} seems corrupted")
          throw new RuntimeException("Tmp file with appended content ${tmpFile.absolutePath} seems to be corrupted")
        }
      } catch (IOException ex) {
        log.error("Unable to append lines to ${position} row in ${file.absolutePath}")
        throw new RuntimeException("Unable to append lines to ${position} row in ${file.absolutePath}", ex)
      } finally {
        if (writer) {
          try {
            writer.close()
          } catch (IOException ex) {
          }
        }
        if (reader) {
          try {
            reader.close()
          } catch (IOException ex) {
          }
        }
      }
    } else {
      log.warn("File ${file} doesn't exists")
    }
  }

  /**
   * Performs regular expression string replacements in a text file.
   * First match is replaced. If byline is enabled, first match on every line is replaced.
   * {@code AntBuilder.replaceregexp ( ... )} is used.
   * @param file existing text file to process
   * @param match regular expression to match
   * @param replace replacement expression string (using \1 to refer to first parenthesis match in {@code match})
   * @param byline if enabled every line is processed, otherwise whole text is taken at once, enabled by default
   * @param encoding encoding to use
   * @return false if file not exists or processing file fails (on non-windows platforms), true if processing was successful
   * @throws Exception on windows when processing with AntBuilder fails
   */
  static boolean replaceregexp(File file, String match, String replace, Boolean byline = true, Boolean trySudo = false, String encoding = "UTF-8") {
    if (!file.exists()) return false
    FilePermission permBackup = retrievePermissions(file) //ant is removing executable bit, backing up
    def result = false
    try {
      ant.replaceregexp(file: file.absolutePath, match: match, replace: replace, byline: byline, encoding: encoding)
      result = true
    } catch (e) {
      if (!platform.isWindows() && (useAdminPrivileges || trySudo)) {
        def classpath = Library.groovyWithAntAsClasspath()

        match = match.replaceAll(platform.nl, '\\\\n')
        match = match.replaceAll("'", "\\\\'")
        replace = replace.replaceAll(platform.nl, '\\\\n')
        replace = replace.replaceAll("'", "\\\\'")

        def escapedFilePath = file.absolutePath.replaceAll(Constants.TWO_DOUBLE_BACKSLASHES, Constants.FOUR_DOUBLE_BACKSLASHES)

        def command = [
            "java",
            "-cp",
            classpath,
            "groovy.ui.GroovyMain",
            "-e",
            "new groovy.util.AntBuilder().replaceregexp(file: '${escapedFilePath}', match: '${match}', replace: '${replace}', byline: '${byline}', encoding: '${encoding}')"
        ]

        result = !Cmd.executeSudoCommand(command, new File("."))
      } else {
        throw e
      }
    } finally {
      if (!platform.isWindows()) {
        definePermissions(permBackup, file)
      }
    }
    return result
  }

  /**
   * Performs string replacements in a text file. First match is replaced.
   * {@code AntBuilder.replace ( ... )} is used.
   * @param file existing text file to process
   * @param token string to find
   * @param value replacement
   * @param encoding encoding to use
   * @return false if file not exists or processing file fails (on non-windows platforms), true if processing was successful
   * @throws Exception on windows when processing with AntBuilder fails
   */
  static boolean replace(File file, String token, String value, Boolean trySudo = false, String encoding = "UTF-8") {
    if (!file.exists()) return false
    FilePermission permBackup = retrievePermissions(file) //ant is removing executable bit, backing up
    def result = false
    try {
      log.debug("Replacing ${token} in ${file.absolutePath} with ${value}")
      ant.replace(file: file.absolutePath, value: value, summary: true, encoding: encoding) {
        replacefilter(token: token)
      }
      result = true
    } catch (e) {
      if (!platform.isWindows()) {
        def classpath = Library.groovyWithAntAsClasspath()

        token = token.replaceAll(platform.nl, '\\\\n')
        //        token = token.replaceAll('"', '\\\\"')
        value = value.replaceAll(platform.nl, '\\\\n')
        //        value = value.replaceAll('"', '\\\\"')

        def escapedFilePath = file.absolutePath.replaceAll(Constants.TWO_DOUBLE_BACKSLASHES, Constants.FOUR_DOUBLE_BACKSLASHES)

        def command = [
            "java",
            "-cp",
            classpath,
            "groovy.ui.GroovyMain",
            "-e",
            "new groovy.util.AntBuilder().replace(file: '${escapedFilePath}', value: '${value}', summary: 'true', encoding: '${encoding}')" +
                " { replacefilter(token: '${token}') }"
        ]

        if (useAdminPrivileges || trySudo) {
          result = !Cmd.executeSudoCommand(command, new File("."))
        } else {
          result = !Cmd.executeCommand(command, new File("."))
        }
      } else {
        throw e
      }
    } finally {
      if (!platform.isWindows()) {
        definePermissions(permBackup, file)
      }
    }
    return result
  }

  /**
   * Do replacement with sed command.
   *
   * TODO
   *   Important do / escaping
   */
  static void sedReplace(File file, String match, String replace, Boolean byline = true, Boolean withSudo = false) {
    def matchEscaped = ''
    def replaceEscaped = ''
    def p
    def stream

    def nl = platform.nl
    def tmpDir = platform.tmpDir
    match = match.replaceAll(nl, '\n')
    replace = replace.replaceAll(nl, '\n')

    // Do escaping
    match.each {
      if (it == '/') matchEscaped += '\\' + it
      else matchEscaped += it
    }
    replace.each {
      if (it == '/') replaceEscaped += '\\' + it
      else replaceEscaped += it
    }

    // Let's create tmp file with correct rights, first
    p = (withSudo) ? [
        'sudo',
        'cp',
        '-p',
        file.getAbsolutePath(),
        tmpDir + '/' + file.getName() + '.tmp'
    ].execute() :
        [
            "cp",
            '-p',
            file.getAbsolutePath(),
            tmpDir + '/' + file.getName() + '.tmp'
        ].execute()
    p.waitFor()

    try {
      // Do replacement
      p = (withSudo) ? [
          'sudo',
          'sed',
          "s/$matchEscaped/$replaceEscaped/g",
          file.getAbsolutePath()
      ].execute() :
          [
              'sed',
              "s/$matchEscaped/$replaceEscaped/g",
              file.getAbsolutePath()
          ].execute()
      stream = new FileOutputStream(tmpDir + '/' + file.getName() + '.tmp')
      p.waitForProcessOutput(stream, System.err)
      stream.close()
    } catch (FileNotFoundException e) {
      // There were possibly problem with access rights
      // It is not ideal, but better than failed test
      makeAccessible(new File(tmpDir + '/' + file.getName() + '.tmp'), true)
      p = (withSudo) ? [
          'sudo',
          'sed',
          "s/$matchEscaped/$replaceEscaped/g",
          file.getAbsolutePath()
      ].execute() :
          [
              'sed',
              "s/$matchEscaped/$replaceEscaped/g",
              file.getAbsolutePath()
          ].execute()
      stream = new FileOutputStream(tmpDir + '/' + file.getName() + '.tmp')
      p.waitForProcessOutput(stream, System.err)
      stream.close()
    }

    // replace old file with new one
    p = (withSudo) ? [
        "sudo",
        "mv",
        tmpDir + '/' + file.getName() + '.tmp',
        file.getAbsolutePath()
    ].execute() :
        [
            "mv",
            tmpDir + '/' + file.getName() + '.tmp',
            file.getAbsolutePath()
        ].execute()
    p.waitFor()
  }

  /**
   * Unzip file to destination directory.
   * Native unzip command is used if present on current machine, otherwise falls back to Ant unzip.
   *
   * @param file existing zip file
   * @param destDir destination directory
   * @param trySudo only for API backward compatibility, unused
   * @param makeAccessible whether to make destination directory accessible when unzip must be run under admin rights (non-windows platforms only)
   * @throws RuntimeException when unzip fails even with admin rights on not-windows platform
   */
  def static nativeUnzip(File file, File destDir, Boolean trySudo = false, Boolean makeAccessible = false) {
    log.debug("Unzipping ${file.absolutePath} to ${destDir.absolutePath}, makeAccessible: ${makeAccessible}")

    if (hasZip == -1) {
      try {
        def process = "unzip -h".execute()
        process.consumeProcessOutput()
        process.waitFor()

        if (process.exitValue() == 0) {
          hasZip = 1
        } else {
          hasZip = 0
        }
      }
      catch (Exception e) {
        hasZip = 0
      }
    }

    if (hasZip == 0) {
      log.warn("Command 'unzip' is not available, using Ant to unzip '${file.getAbsolutePath()}'. Beware, that this " +
              "method won't preserve symlinks and filesystem permissions!")
      return ant.unzip(src: file.getAbsolutePath(), dest: destDir.getAbsolutePath())
    }

    if (hasZip == 1) {
      def command = [
          "unzip",
          '-o',
          '-q',
          file.getAbsolutePath(),
          "-d",
          destDir.getAbsolutePath()
      ]
      def process = command.execute()

      def output = new StringBuffer()
      process.consumeProcessOutput(output, output)
      process.waitFor()

      if (process.exitValue() != 0) {
        process.outputStream.flush()
        Thread.sleep(50) // wait for error stream
        def str = output.toString()
        log.debug("Output of unzip command without sudo: ${str}")
        if (!str.contains('Nothing to do')) {
          if (JBFile.useAdminPrivileges) {
            if (!platform.isWindows()) {
              process = [
                  'sudo',
                  "unzip",
                  '-o',
                  '-q',
                  file.getAbsolutePath(),
                  "-d",
                  destDir.getAbsolutePath()
              ].execute()

              output = new StringBuffer()
              process.consumeProcessOutput(output, output)
              process.waitFor()
              if (process.exitValue() != 0) {
                process.outputStream.flush()
                Thread.sleep(50) // wait for error stream
                str = output.toString()
                log.debug("Output of unzip command with sudo: ${str}")
                if (!str.contains('Nothing to do')) {
                  throw new RuntimeException("Cannot unzip ${file.getAbsolutePath()}.")
                } else {
                  throw new RuntimeException("Unable to unzip ${file.getAbsolutePath()} even with sudo, see log for unzip command output")
                }
              }
            }

            if (makeAccessible) JBFile.makeAccessible(destDir, true)
          }
        }
      }
    }
  }

  /**
   * Make given file or directory (and each subdirectory and file) accessible, ie. allow to read/write/execute it for anybody.
   * Do nothing on Windows platform.
   * @param file file or directory
   * @throws RuntimeException when on non-windows platform and changing rights failed
   */
  static void makeAccessible(File file, Boolean withSudo = false) {
    def unixCommand = [
        'chmod',
        '-R',
        'ugo+rwx',
        file.getAbsolutePath()
    ]
    if (!platform.isWindows()) {
      if (Cmd.executeCommand(unixCommand, new File('.')) > 0) {
        if (JBFile.useAdminPrivileges || withSudo) {
          if (Cmd.executeSudoCommand(unixCommand, new File('.')) > 0) {
            log.warn("Cannot make ${file} accessible")
          }
        } else {
          log.warn("Cannot make ${file} accessible")
        }
      } else {
        // Solaris11 fix - chmod -R fails, but exit code = 0
        if (platform.isSolaris11()) {
          if (JBFile.useAdminPrivileges || withSudo) {
            log.debug("Solaris11 fix - chmod -R can fail with exit code = 0, trying one more time with sudo")
            if (Cmd.executeSudoCommand(unixCommand, new File('.')) > 0) {
              log.warn("Cannot make ${file} accessible")
            }
          }
        }
      }
    }

    // TODO JBfile.makeAccessible(...) for Windows
  }

  /**
   * Change group ownership of given file. Calling native command 'chgrp' to do that.
   * <b>Note: on windows nothing happens and 0 is returned.</b>
   * @param group group name to set
   * @param target file or directory
   * @return exit code of 'chgrp' command or 0 on windows
   */
  static int chgrp(String group, File target) {
    if (!platform.isWindows()) {
      Closure executeCommand = Cmd.executeMethodBasedOnAdminPrivileges(Cmd.&executeCommandConsumeStreams, Cmd.&executeSudoCommandConsumeStreams)
      return executeCommand(['chgrp', '-R', group, target.getAbsoluteFile()], new File('.')).exitValue
    }
    // TODO implementation for Windows?
    return 0
  }

  static int chown(String owner, File target) {
    Closure executeCommand = Cmd.executeMethodBasedOnAdminPrivileges(Cmd.&executeCommandConsumeStreams, Cmd.&executeSudoCommandConsumeStreams)
    if (!platform.isWindows()) {
      return executeCommand(['chown', '-R', owner, target.getAbsoluteFile()], new File('.')).exitValue
    } else {
      // Take ownership of file - implementation for Windows
      // Sets ownership to user who is running testsuite (for example hudson)
      return executeCommand(['cmd', '/C', 'takeown', '/f', target.getAbsoluteFile()], new File('.')).exitValue
    }
  }

  /**
   * Change privileges for whole directory or for file.
   * @param mode, privileges you want to set
                   Implementation for Windows
                   [/grant[:r] <Sid>:<Perm>[...]]
                   icacls /grant - Grants specified user access rights. Permissions replace previously granted explicit permissions.
                   Without :r, permissions are added to any previously granted explicit permissions.
                   mode (winPermission) can be:
                   F (full access)
                   M (modify access)
                   RX (read and execute access)
                   R (read-only access)
                   W (write-only access)
   * @param target, file to modify
   * @param winUser, user to change permissions if platform is windows
   * @param windowsPerMode, mode to use [/grant, /deny]
   * @return return code from command chmod(linux), icacls(Windows)
   */
  static int chmod(String mode, File target, String winUser = '%USERNAME%') {
    Closure executeCommand = Cmd.executeMethodBasedOnAdminPrivileges(Cmd.&executeCommandConsumeStreams, Cmd.&executeSudoCommandConsumeStreams)
    if (!platform.isWindows()) {
      return executeCommand(['chmod', mode, target.getAbsoluteFile()], new File('.')).exitValue
    } else {
      return Cmd.executeCommandConsumeStreams(["icacls", target.getAbsoluteFile(), '/grant', "${winUser}:${mode}"], new File('.')).exitValue
    }
  }

  static String read(File target) {
    try {
      return target.text
    } catch (FileNotFoundException e) {
      if (useAdminPrivileges && !platform.isWindows()) {
        def output = new ByteArrayOutputStream()
        def code = Cmd.executeCommandRedirectIO("sudo cat " + target.getAbsolutePath(), null, null, output, System.err)
        if (code != 0) throw new RuntimeException("Can't read file ${target.getAbsolutePath()}")
        return output.toString()
      } else {
        throw new RuntimeException("Can't read file ${target.getAbsolutePath()}")
      }
    }
  }

  /**
   * Checks if file exists and if it is not a dictionary.
   * @param file - file to check
   */
  static boolean isExistingFile(File f) {

    if (f.exists() && !f.isDirectory()) {
      return true
    }

    if (platform.isWindows() || !useAdminPrivileges) {
      return false
    }

    return Cmd.executeSudoCommand(["test", "-f",  "${f.getAbsolutePath()}"], new File(".")) == 0
  }

  static Boolean mkdir(File dir, trySudo = false) {
    try {
      ant.mkdir(dir: dir.getAbsolutePath())
    } catch (e) {
      log.trace("Creating directory via ant failed", e)

      if (platform.isWindows()) return false

      log.trace("Trying to create directory calling a native command")
      String command = "mkdir -p ${dir.getAbsolutePath()}"

      if (useAdminPrivileges || trySudo) {
        Cmd.executeSudoCommand(command, new File('.'))
      } else {
        Cmd.executeCommand(command, new File('.'))
      }

      if (dir.exists()) {
        JBFile.makeAccessible(dir)
      } else {
        log.warn("Couldn't create directory {} even when calling native command", dir.absolutePath)
      }
    }
    return dir.exists()
  }

  /**
   * Generate a random binary file of desired length
   *
   * @param destination where the generated file is to be stored
   * @param length desired length in bytes of the generated file
   * @throws FileNotFoundException if the file exists but is a directory rather than a regular file, does not exist but
   * cannot be created, or cannot be opened for any other reason.
   * @throws IOException if an I/O error occurs
   */
  static void generateRandomBinaryFile(File destination, int length) throws FileNotFoundException, IOException {
    if (!destination.parentFile.isDirectory() && !mkdir(destination.parentFile)) {
      throw new FileNotFoundException("Directory ${destination.parentFile} cannot be created")
    }

    if (!destination.isFile() && !createFile(destination)) {
      throw new FileNotFoundException("File ${destination} cannot be created")
    }

    DataOutputStream dos = new DataOutputStream(new FileOutputStream(destination));
    Random random = new Random();
    int written = 0;

    try {
      while (written < length) {
        int toWrite = Math.min(length - written, 1024);
        byte[] bytes = new byte[toWrite];
        random.nextBytes(bytes);
        dos.write(bytes);
        written += toWrite;
      }
    } finally {
      dos.close();
    }
  }

  /**
   * Checks whether the file contains line matching provided regexp
   * @param fileToCheck - file to check
   * @param regexp - regexp to match
   * @param firstLine - first line which is being checked, the line numbering starts with 1
   * @return true if the file contains matching line starting with firstLine, false otherwise
   */
  static boolean hasMatchingLine(File fileToCheck, regexp, int firstLine = 1) {
    log.debug("Scanning file " + fileToCheck.absoluteFile + " for value: " + regexp.toString())

    String[] lines = JBFile.read(fileToCheck).split("\r\n|\n|\r")
    for (int lineNumber = firstLine; lineNumber <= lines.length; lineNumber++) {
      String line = lines[lineNumber-1]
      if (line ==~ regexp) {
        log.debug("Found matching line at ${lineNumber}: ${line} in ${fileToCheck.absolutePath}")
        return true
      }
    }
    return false
  }

  /**
   * Waits for file to contain specific string.
   * Returns true if the file contains specified regexp before reaching timeout, false otherwise.
   */
  static boolean waitUntilFileContains(File fileToCheck, regexp, long timeout = 60, TimeUnit unit = TimeUnit.SECONDS) {
    long startTime = System.currentTimeMillis()
    long timeoutInMs = unit.toMillis(timeout)

    while (System.currentTimeMillis() < (startTime + timeoutInMs)) {
      if (JBFile.hasMatchingLine(fileToCheck, regexp)) {
        return true
      }

      Library.sleep(1000)
    }
    return false
  }

  /**
   * Waits given time for file if exists.
   * Returns true if the file exists, false otherwise.
   */
  static boolean waitUntilFileExists(File fileToCheck, long timeout = 60, TimeUnit unit = TimeUnit.SECONDS) {
    long startTime = System.currentTimeMillis()
    long timeoutInMs = unit.toMillis(timeout)

    while (!fileToCheck.exists() && System.currentTimeMillis() < (startTime + timeoutInMs)) {
      Library.sleep(1000)
    }
    return fileToCheck.exists()
  }

  /**
   * Returns number of occurrences of words matching regexp in given file.
   * @param file - file to check
   * @param regexp - regexp to match
   */
  static int fileRegexpOccurrences(File file, regexp) {
    if (file.isFile()) {
      def regexpOccurrences = (file.text =~ regexp).getCount()
      log.trace("Regexp Occurrences: regexp ${regexp} matches ${regexpOccurrences} times in file ${file}")
      return regexpOccurrences
    } else {
      log.trace("Regexp Occurrences: ${file} is directory, return 0")
      return 0
    }
  }

  /**
   * Waiting for file or directory being removed (waiting in maximum for specified timeout)
   * @param file represents file or directory which is being checked for existence
   * @return true if the file doesn't exist => was removed in specified time, false otherwise.
   */
  static boolean waitUntilFileIsRemoved(File file, long timeout, TimeUnit units) {
    long startTime = System.currentTimeMillis()
    long timeoutInMs = units.toMillis(timeout)

    while (file.exists() && System.currentTimeMillis() < (startTime + timeoutInMs)) {
      Library.sleep(1000)
    }
    return file.exists()
  }

  /**
   * File permissions
   * owner - name of the owner of the file
   * group - name of the group owning this file
   * perm - permissions of the file in octal form
   */
  static class FilePermission {
    String owner, group, perm

    FilePermission() {}

    FilePermission(String owner, String group, String perm) {
      this.owner = owner
      this.group = group
      this.perm = perm
    }
  }

  /**
   * Read owner, group, permissions of file
   * @param file to call upon
   * @return FilePermissions
   */
  static FilePermission retrievePermissions(File file) {
    if (platform.isWindows()) {
      return null
    }
    FilePermission filePerm = new FilePermission()
    String output = callListLongDirectory(file)
    filePerm.setOwner(((String) (output).split()[2]))
    filePerm.setGroup((String) (output).split()[3])
    filePerm.setPerm(readOctalPermissions(output))
    return filePerm
  }

  /**
   * Set owner, group, permissions to a file, only those set in FilePermission object with be called
   * @param filePerm, FilePermission where not every attribute must be set
   * @param file to call upon
   */
  static void definePermissions(FilePermission filePerm, File file) {
    if (!file || platform.isWindows()) {
      return
    }
    if (filePerm.getOwner() && chown(filePerm.getOwner(), file) != 0) {
      throw new RuntimeException("Setting owner ${filePerm.getOwner()} to ${file.absolutePath} went wrong")
    }
    if (filePerm.getGroup() && chgrp(filePerm.getGroup(), file) != 0) {
      throw new RuntimeException("Setting group ${filePerm.getGroup()} to ${file.absolutePath} went wrong")
    }
    if (filePerm.getPerm() && chmod(filePerm.getPerm(), file) != 0) {
      throw new RuntimeException("Setting permissions ${filePerm.getPerm()} to ${file.absolutePath} went wrong")
    }
  }

  /**
   * Call ls -ld on file
   * @param file to call upon
   * @return stdOut of ls -ld command, "" for windows
   */
  private static String callListLongDirectory(File file) {
    if (platform.isWindows()) {
      return ""
    }
    log.debug("Extracting octal permission of file ${file.absolutePath}")
    Map ret =  Cmd.executeCommandConsumeStreams(['ls', '-ld', file.absolutePath], new File('.'))
    if (ret.exitValue > 0 && useAdminPrivileges) {
      ret = Cmd.executeSudoCommandConsumeStreams(['ls', '-ld', file.absolutePath], new File('.'))
    }
    if (ret.exitValue > 0) {
      throw new RuntimeException("Reading of file permissions went wrong")
    }
    return (String) ret.stdOut
  }

  /**
   * Read permissions of file in octal format
   * Algorith:
   *          1. drwx------. 13 owner group 4.0K May 30 07:27 .
   *          2. drwx------.
   *          3. rwx------
   *          4. 0700
   * @param file, to read permissions of
   * @return octal permissions as String, empty string if used on windows
   */
  private static String readOctalPermissions(File file) {
    return readOctalPermissions(callListLongDirectory(file))
  }

  /**
   * Read permissions of file in octal format
   * Algorith:
   *          1. drwx------. 13 owner group 4.0K May 30 07:27 .
   *          2. drwx------.
   *          3. rwx------
   *          4. 0700
   * @param file, to read permissions of
   * @return octal permissions as String, empty string if used on windows
   */
  private static String readOctalPermissions(String output) {
    // drwx------. 13 owner group 4.0K May 30 07:27 . -> drwx------. -> rwx------. (removed first char to have just permissions)
    String rwxPerm = ((String) (output).split()[0]).substring(1)
    int rwxSize = 9 // rwxrwxrwx
    if (!(rwxPerm.size() == rwxSize || rwxPerm.size() == rwxSize + 1)) { //drwx------ or drwx------.
      throw new RuntimeException("Reading of file permissions went wrong, unexpected rwx permissions, $rwxPerm")
    }
    String octalPerm = ""
    // Take one of the group (owner(index 0-2), group (index 3-5), others (index 6-8))
    int special = 0
    for (int startI = 0; startI < rwxSize; startI += 3) {
      int endI = startI + 2
      int tmpPerm = 0
      (startI..endI).each {
        switch (rwxPerm[it]) {
          case 'r':
            tmpPerm += 4
            break
          case 'w':
            tmpPerm += 2
            break
          case 'x':
            tmpPerm += 1
            break
          case 't':
            tmpPerm += 1
          case 'T':
            special += 1
            break
          case 's':
            tmpPerm += 1
          case 'l':
          case 'S':
          case 'L':
            special += (it == 2) ? 4 : 2
            break
          case '-':
            break
          default:
            throw new RuntimeException("Reading of file permissions went wrong, (${rwxPerm[it]}) unexpected rwx permission, $rwxPerm")
        }
      }
      octalPerm += tmpPerm.toString()
    }
    octalPerm = special.toString() + octalPerm
    log.debug("Extracted permission ${rwxPerm} is in octal ${octalPerm}")
    return octalPerm
  }
}
