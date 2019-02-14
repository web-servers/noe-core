package noe.common.utils

/**
 * Service class for storing and restoring of file statuses.
 * Status means content or information whether file does not exist.
 *
 * This applies on files only (no directories)
 * State is stored in memory.
 *
 * If non existing file is given, it is deleted on restore if exists at the moment.
 *
 * Symlinks do not have any special care.
 *
 * There is no manipulation with access rights on files, only exception is when file
 * is being removed - when normal delete fails deleting with sudo is activated (if enabled).
 * @see JBFile#delete
 *
 */
class FileStateVault {
  final static byte[] DID_NOT_EXIST = [6, 6, 6]
  Map<String, List<byte[]>> vault = [:]

  /**
   * If file toStore does exist it's current content is stored into memory (stack).
   * If does not then this fact is just noted.
   *
   * The state of the file can be loaded by method `pop()` which restore
   * last known state and delete this state from memory.
   */
  FileStateVault push(File toStore) {
    if (!toStore.exists()) {
      initItem(toStore)
      vault.get(key(toStore)).add(DID_NOT_EXIST)
    } else if (toStore.isFile() && toStore.canRead()) {
      initItem(toStore)
      vault.get(key(toStore)).add(toStore.getBytes())
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
   * Restores last known state of given file and remove that state from memory.
   * If information that file did not exist was found, file is deleted.
   *
   * @see JBFile#delete
   */
  FileStateVault pop(File toRestore) {
    if (!vault.containsKey(key(toRestore)) || vault.get(key(toRestore)).size() == 0) {
      throw new IllegalStateException("Target to re-store '${toRestore}' does not exist in vault.")
    }

    byte[] contentToRestore = vault.get(key(toRestore)).last()
    deleteIfDidNotExistsElseRestore(toRestore, contentToRestore)

    vault.get(key(toRestore)).remove(contentToRestore)
    if (vault.get(key(toRestore)).size() == 0) {
      vault.remove(key(toRestore))
    }

    return this
  }

  private void deleteIfDidNotExistsElseRestore(File toRestore, byte[] contentToRestore) {
    if (contentToRestore == DID_NOT_EXIST) {
      JBFile.delete(toRestore)
    } else {
      toRestore.setBytes(contentToRestore)
    }
  }

  /**
   * Restore to first state, all previous states are not applied but are removed from memory.
   *
   * @see JBFile#delete
   * @see FileStateVault#pop
   */
  FileStateVault popAll(File toRestore) {
    if (!vault.containsKey(key(toRestore)) || vault.get(key(toRestore)).size() == 0) {
      throw new IllegalStateException("Target to re-store '${toRestore}' does not exist in vault.")
    }

    byte[] contentToRestore = vault.get(key(toRestore)).first()
    deleteIfDidNotExistsElseRestore(toRestore, contentToRestore)

    vault.remove(key(toRestore))

    return this
  }

  /**
   * Restore all stored files to first state, all theirs previous states are not applied but are removed from memory.
   *
   * @see JBFile#delete
   * @see FileStateVault#pop
   */
  FileStateVault popAll() {
    vault.each { String key, List<byte[]> data ->
      byte[] contentToRestore = data.first()
      deleteIfDidNotExistsElseRestore(new File(key), contentToRestore)
    }

    vault.clear()

    return this
  }
}
