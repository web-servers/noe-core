package noe.common.utils

/**
 * Service class for storing and restoring of directories states.
 * State means content of files in directory and subdirectories or information whether files
 * and subdirectories does not exist.
 *
 * State is stored in memory.
 *
 * If non existing directory is given, it is deleted on restore if exists at the moment.
 *
 * Symlinks do not have any special care.
 *
 * There is no manipulation with access rights on files, only exception is when file
 * is being removed - when normal delete fails deleting with sudo is activated (if enabled).
 *
 * Each record/pushed directory is handled individually, implication is that push of subdirectory of
 * already pushed directory is handled separately. There is no logic checking relation between pushed directories.
 *
 * For work with files only check class FileStateVault
 *
 * @see FileStateVault
 * @see JBFile#delete
 */
class DirStateVault implements StateVault<DirStateVault> {
  private Map<String, List<DirState>> vault = [:]

  static class DirState {
    FileStateVault fileStateVault
    DirStateVault dirStateVault
    boolean existed = true

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
    DirState dirState = new DirState()

    // all files in directory are handle by 1 FileStateVault instance and
    // all directories are handled by 1 DirStateVault instance
    StateVault filesInDirFileStateVault
    StateVault dirsInDirStateVault

    if (!toStore.exists()) {
      DirState ds = new DirState()
      ds.existed = false
      vault.get(key(toStore)).add(ds)
    } else {
      toStore.listFiles().each { File item ->
        if (item.isFile() && item.canRead()) {
          filesInDirFileStateVault = (filesInDirFileStateVault ?: new FileStateVault()).push(item)
        } else if (item.isDirectory() && item.canRead() && item.canExecute()) {
          dirsInDirStateVault = (dirsInDirStateVault ?: new DirStateVault()).push(item)
        } else {
          throw new IllegalStateException("Target to store '${toStore}' is not accessible.")
        }
      }

      if (filesInDirFileStateVault) {
        dirState.fileStateVault = filesInDirFileStateVault
      }
      if (dirsInDirStateVault) {
        dirState.dirStateVault = dirsInDirStateVault
      }

      if (!dirState.isEmpty()) {
        vault.get(key(toStore)).add(dirState)
      }
    }

    return this
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
    if (!vault.containsKey(key(toRestore)) || vault.get(key(toRestore)).size() == 0) {
      throw new IllegalStateException("Target to re-store '${toRestore}' does not exist in vault.")
    }

    DirState dirState = vault.get(key(toRestore)).last()
    FileStateVault fileStateVault = dirState?.fileStateVault
    DirStateVault dirStateVault = dirState?.dirStateVault
    restoreDirectory(toRestore, fileStateVault, dirStateVault)

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
    if (!vault.containsKey(key(toRestore)) || vault.get(key(toRestore)).size() == 0) {
      throw new IllegalStateException("Target to re-store '${toRestore}' does not exist in vault.")
    }

    FileStateVault fileStateVault = vault.get(key(toRestore)).first()?.fileStateVault
    DirStateVault dirStateVault = vault.get(key(toRestore)).first()?.dirStateVault
    restoreDirectory(toRestore, fileStateVault, dirStateVault)

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

  private void restoreDirectory(File toRestore, FileStateVault fileStateVault, DirStateVault dirStateVault) {
    boolean fileDidExist
    boolean dirDidExist

    // directory did ont exists -> delete
    if (!vault.get(key(toRestore)).last().existed) {
      JBFile.delete(toRestore)
      return
    }

    // target exists but it is file not directory and directory existed (not returned in previous step)
    if (toRestore.exists() && toRestore.isFile()) {
      JBFile.delete()
      toRestore.mkdirs()
    }

    // remove anything new
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
        throw IllegalStateException("Can not access to target ${existingItemInToRestoreFolder}")
      }

    }

    // now, there are only files what was pushed, any missing, will be recovered as well as state of existing
    fileStateVault?.pop()
    dirStateVault?.pop()
  }

  private String key(File file) {
    return file.getCanonicalPath()
  }

  private File file(String key) {
    return new File(key)
  }

}
