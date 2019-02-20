package noe.common.utils

interface StateVault<U> {

  /**
   * If item toStore does exist it's current content is stored into memory.
   * If does not then this fact is just noted.
   *
   * All subitems are handled recursively.
   *
   * The state of the item can be loaded by `pop(...)` or `popAll()` methods, they restore
   * last saved state and delete this state from memory.
   */
  U push(File toStore)

  /**
   * Restores last known state of given item and remove that state from memory.
   * If information that file did not exist was found, file is deleted.
   */
  U pop(File toRestore)

  /**
   * Restores last saved state of all directories and remove those states from memory.
   * If information that file did not exist was found, file is deleted.
   */
  U pop()

  /**
   * Restore all stored items to first state, all other states are not applied
   * and are removed from memory.
   */
  U popAll(File toRestore)

  /**
   * Restore all pushed items to first state, all other states are not applied and are removed from memory.
   */
  U popAll()

  /**
   * Checks whether the file is pushed to vault already.
   */
  boolean isPushed(File file)
}
