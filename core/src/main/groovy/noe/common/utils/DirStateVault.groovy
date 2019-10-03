package noe.common.utils

/**
 * Service class for storing and restoring of directories states.
 * State means content of files in directory and subdirectories or information whether files
 * and subdirectories does not exist, it also means file permissions.
 *
 * State is stored in memory.
 *
 * If non existing directory is given, it is deleted on restore if exists at the moment.
 *
 * Symlinks do not have any special care.
 *
 * There is manipulation with access rights on items, if item is not accessible it's access rights are changed
 * temporalily for time if saving or restoring state, To enable this functionality NOE has to be executed with
 * -Drun.with.sudo=true.
 *
 * Each record/pushed directory is handled individually, implication is that push of subdirectory of
 * already pushed directory is handled separately. There is no logic checking relation between pushed directories.
 *
 * For work with files only check class FileStateVault
 *
 * IMPORTANT
 * -----------------------------------------------------------------------------------------------------
 * Since states of files are stored in memory, it is highly recommended to use `DirStateVault` with care.
 *
 * For instance saving larger binary files is not good idea since space on heap can be consumed
 * quickly and runtime requirements can change uncontrollably.
 *
 * On other hand, a smaller files like configurations or scripts files, suites perfectly for usage with
 * `DirStateVault` in general.
 * -----------------------------------------------------------------------------------------------------
 *
 * @see FileStateVault
 * @see JBFile#delete
 */
class DirStateVault implements StateVault<DirStateVault> {
  private Map<String, List<DirState>> vault = [:]
  private boolean isWindows = new Platform().isWindows()

  class DirState {
    FileStateVault fileStateVault
    DirStateVault dirStateVault
    boolean existed = true
    File dir

    /**
     * Permissions of file on Unix like OS only.
     */
    private JBFile.FilePermission permission

    DirState(File targetDir) {
      this.dir = targetDir
    }

    void storeDirPermissions() {
      if (dir.exists() && !isWindows) {
        permission = JBFile.retrievePermissions(dir)
      }
    }

    void loadDirPermissions() {
      if (dir.exists() && !isWindows) {
        JBFile.definePermissions(permission, dir)
      }
    }

    boolean isEmpty() {
      return fileStateVault == null && dirStateVault == null
    }
  }

  /**
   * If directory toStore does exist it's current content is stored into memory.
   * If does not then this fact is just noted.
   *
   * All subdirectories are handled recursively.
   *
   * The state of the file can be loaded by `pop(...)` or `popAll()` methods, they restore
   * last saved state and delete this state from memory.
   */
  @Override
  DirStateVault push(File toStore) {
    initItem(toStore)
    DirState dirState = new DirState(toStore)

    if (toStore.isFile()) {
      throw new IllegalStateException("Target to store '${toStore}' is not directory.")
    }

    // all files in directory are handle by 1 FileStateVault instance and
    // all directories are handled by 1 DirStateVault instance
    FileStateVault filesInDirFileStateVault
    DirStateVault dirsInDirStateVault

    if (!toStore.exists()) {
      dirState.existed = false
      vault.get(key(toStore)).add(dirState)
    } else {
      dirState.storeDirPermissions()
      makeDirAccessibleIfItIsNot(toStore)

      toStore.listFiles().each { File item ->
        if (item.isFile()) {
          filesInDirFileStateVault = (filesInDirFileStateVault ?: new FileStateVault()).push(item)
        } else if (item.isDirectory()) {
          dirsInDirStateVault = (dirsInDirStateVault ?: new DirStateVault()).push(item)
        } else {
          throw new IllegalStateException("Target to store '${toStore}' is not accessible.")
        }
      }
      dirState.loadDirPermissions()

      if (filesInDirFileStateVault) {
        dirState.fileStateVault = filesInDirFileStateVault
      }
      if (dirsInDirStateVault) {
        dirState.dirStateVault = dirsInDirStateVault
      }

      vault.get(key(toStore)).add(dirState)
    }

    return this
  }

  private void makeDirAccessibleIfItIsNot(File dir) {
    if (!isWindows && !(dir.canRead() && dir.canExecute() && dir.canExecute())) {
      JBFile.chmod('u+rwx', dir)
    }
  }

  /**
   * Restores last saved state of all directories and remove those states from memory.
   * If information that file did not exist was found, file is deleted.
   *
   * @see JBFile#delete
   */
  @Override
  DirStateVault pop() {
    Set<String> keys = Collections.unmodifiableCollection(vault.keySet())

    keys.each {String key ->
      pop(file(key))
    }

    return this
  }

  /**
   * Restores last known state of given file and remove that state from memory.
   * If information that file did not exist was found, file is deleted.
   *
   * @see JBFile#delete
   */
  @Override
  DirStateVault pop(File toRestore) {
    if (!vault.containsKey(key(toRestore))) {
      throw new IllegalStateException("Target to re-store '${toRestore}' does not exist in vault.")
    }

    DirState dirState = vault.get(key(toRestore)).last()

    restoreDirectory(toRestore, dirState)

    vault.get(key(toRestore)).pop()
    if (vault.get(key(toRestore)).isEmpty()) {
      vault.remove(key(toRestore))
    }

    return this
  }

  /**
   * Restore all stored directories to first state, all other states are not applied and are removed from memory.
   *
   * @see JBFile#delete
   * @see FileStateVault#pop
   */
  @Override
  DirStateVault popAll() {
    Set<String> keys = Collections.unmodifiableCollection(vault.keySet())

    keys.each { String key ->
      popAll(file(key))
    }

    vault.clear()

    return this
  }

  /**
   * Restore to first state, all other states are not applied and are removed from memory.
   *
   * @see JBFile#delete
   */
  @Override
  DirStateVault popAll(File toRestore) {
    if (!vault.containsKey(key(toRestore))) {
      throw new IllegalStateException("Target to re-store '${toRestore}' does not exist in vault.")
    }

    DirState dirState = vault.get(key(toRestore)).first()
    restoreDirectory(toRestore, dirState)

    vault.remove(key(toRestore))

    return this
  }

  /**
   * Checks whether the file is pushed to vault already.
   */
  @Override
  boolean isPushed(File file) {
    return vault.containsKey(key(file))
  }

  private void initItem(File toStore) {
    if (!vault.containsKey(key(toStore))) {
      vault.put(key(toStore), [])
    }
  }

  private void restoreDirectory(File toRestore, DirState dirState) {
    boolean fileDidExist
    boolean dirDidExist
    FileStateVault fileStateVault = dirState?.fileStateVault
    DirStateVault dirStateVault = dirState?.dirStateVault

    // Directory was empty -> remove content
    if (dirState.isEmpty()) {
      JBFile.cleanDirectory(toRestore)
      return
    }

    // directory did ont exists -> delete
    if (!vault.get(key(toRestore)).last().existed) {
      JBFile.delete(toRestore)
      return
    }

    // target exists but it is file not directory and directory existed (not returned in previous step)
    if (toRestore.exists() && toRestore.isFile()) {
      JBFile.delete(toRestore)
      toRestore.mkdirs()
    }

    // remove anything, what was not present on push
    toRestore.listFiles().each { File existingItemInToRestoreFolder ->

      if (existingItemInToRestoreFolder.isFile()) {

        fileDidExist = (fileStateVault) ? (fileStateVault?.isPushed(existingItemInToRestoreFolder)) : false
        if (!fileDidExist) {
          JBFile.delete(existingItemInToRestoreFolder)
        }

      } else if (existingItemInToRestoreFolder.isDirectory()) {

        dirDidExist = (dirStateVault) ? (dirStateVault?.isPushed(existingItemInToRestoreFolder)) : false
        if (!dirDidExist) {
          JBFile.delete(existingItemInToRestoreFolder)
        }

      } else {
        throw new IllegalStateException("Can not access to target ${existingItemInToRestoreFolder}")
      }

    }

    // now, there are only files what was pushed, any missing, will be recovered as well as state of existing
    fileStateVault?.pop()

    makeDirAccessibleIfItIsNot(toRestore)
    dirStateVault?.pop()
    dirState.loadDirPermissions()
  }

  private String key(File file) {
    return file.getCanonicalPath()
  }

  private File file(String key) {
    return new File(key)
  }

}
