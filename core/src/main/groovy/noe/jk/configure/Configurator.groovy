package noe.jk.configure

/**
 * Any class which can configure itself.
 */
interface Configurator<E extends Configurator> {

  /**
   * Perform configuration
   */
  E configure()

  /**
   * Revert to configured state before any configuration.
   */
  E revertAll()

}
