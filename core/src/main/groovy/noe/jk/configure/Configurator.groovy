package noe.jk.configure

/**
 * Any class which can configure itself.
 */
interface Configurator<E extends Configurator> {

  E configure()

}
