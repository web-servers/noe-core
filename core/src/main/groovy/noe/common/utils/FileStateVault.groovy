package noe.common.utils

/**
 * Service class for storing and restoring of file states.
 * State means content or information whether file does not exist.
 *
 * This applies on files only (no directories)
 * State is stored in memory.
 *
 * If non existing file is given, it is deleted on restore if exists at the moment.
 *
 * Symlinks do not have any special care.
 *
 * There is no manipulation with access rights on file, if file is not accessable,
 * exception is thrown. When file is deleted, `sudo` is applied if simple delete does not work.
 *
 * @see JBFile#delete
 * @see DirStateVault
 *
 */
class FileStateVault implements StateVault {
  Map<String, List<FileState>> vault = [:]
  private boolean isWindows = new Platform().isWindows()

  class FileState {
    final static byte[] DID_NOT_EXIST = [6, 6, 6]
    private byte[] state

    /**
     * Attributes of files on Unix like OS only.
     */
    private JBFile.FilePermission permission


    FileState saveState(File file) {
      if (!isWindows) {
        this.permission = JBFile.retrievePermissions(file)
      }

      makeTheFileReadableIfItIsNot(file)
      this.state = file.getBytes()
      returnOriginalPermissions(file)

      return this
    }

    private void makeTheFileReadableIfItIsNot(File file) {
      if (!isWindows && !file.canRead()) {
        JBFile.chmod('ugo+r', file)
      }
    }

    private returnOriginalPermissions(File file) {
      if (!isWindows) {
        JBFile.definePermissions(permission, file)
      }
    }

    FileState restoreState(File restoreTarget) {
      makeFileWriteableIfItIsNot(restoreTarget)
      restoreTarget.setBytes(state)
      returnOriginalPermissions(restoreTarget)

      return this
    }

    private void makeFileWriteableIfItIsNot(File restoreTarget) {
      if (!isWindows && !restoreTarget.canWrite()) {
        JBFile.chmod('ugo+w', restoreTarget)
      }
    }

    FileState setDidNotExist() {
      this.state = DID_NOT_EXIST

      return this
    }

    boolean didNotExists() {
      return this.state == DID_NOT_EXIST
    }

  }

  /**
   * If file toStore does exist it's current content is stored into memory (stack).
   * If does not then this fact is just noted.
   *
   * The state of the file can be loaded by method `pop()` which restore
   * last known state and delete this state from memory.
   */
  @Override
  FileStateVault push(File toStore) {
    if (!toStore.exists()) {
      initItem(toStore)
      vault.get(key(toStore)).add(new FileState().setDidNotExist())
    } else if (toStore.isFile()) {
      initItem(toStore)
      vault.get(key(toStore)).add(new FileState().saveState(toStore))
    } else {
      throw new IllegalStateException("Target to store '${toStore}' is not file or is not accessible.")
    }

    return this
  }

  private void initItem(File toStore) {
    if (!vault.containsKey(key(toStore))) {
      vault.put(key(toStore), [])
    }
  }

  private String key(File file) {
    return file.getCanonicalPath()
  }

  /**
   * Restores last known state of all files and remove those states from memory.
   *
   * @see JBFile#delete
   */
  @Override
  FileStateVault pop() {
    Set<String> keys = Collections.unmodifiableCollection(vault.keySet())

    keys.each { String key ->
      pop(new File(key))
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
  FileStateVault pop(File toRestore) {
    if (!vault.containsKey(key(toRestore)) || vault.get(key(toRestore)).size() == 0) {
      throw new IllegalStateException("Target to re-store '${toRestore}' does not exist in vault.")
    }

    FileState fileStateToRestore = vault.get(key(toRestore)).last()
    deleteIfDidNotExistsElseRestore(toRestore, fileStateToRestore)

    vault.get(key(toRestore)).remove(fileStateToRestore)
    if (vault.get(key(toRestore)).size() == 0) {
      vault.remove(key(toRestore))
    }

    return this
  }

  boolean isPushed(File file) {
    return vault.containsKey(key(file))
  }

  private void deleteIfDidNotExistsElseRestore(File toRestore, FileState fileState) {
    if (fileState.didNotExists()) {
      JBFile.delete(toRestore)
    } else {
      if (!toRestore.exists()) {
        toRestore.getParentFile().mkdirs()
        toRestore.createNewFile()
      }
      fileState.restoreState(toRestore)
    }
  }

  /**
   * Restore to first state, all previous states are not applied but are removed from memory.
   *
   * @see JBFile#delete
   * @see FileStateVault#pop
   */
  @Override
  FileStateVault popAll(File toRestore) {
    if (!vault.containsKey(key(toRestore)) || vault.get(key(toRestore)).size() == 0) {
      throw new IllegalStateException("Target to re-store '${toRestore}' does not exist in vault.")
    }

    FileState fileStateToRestore = vault.get(key(toRestore)).first()
    deleteIfDidNotExistsElseRestore(toRestore, fileStateToRestore)

    vault.remove(key(toRestore))

    return this
  }

  /**
   * Restore all stored files to first state, all theirs previous states are not applied but are removed from memory.
   *
   * @see JBFile#delete
   * @see FileStateVault#pop
   */
  @Override
  FileStateVault popAll() {
    vault.each { String key, List<FileState> data ->
      FileState fileStateToRestore = data.first()
      deleteIfDidNotExistsElseRestore(new File(key), fileStateToRestore)
    }

    vault.clear()

    return this
  }
}
