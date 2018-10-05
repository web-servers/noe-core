package noe.jk.configure

import noe.jk.configure.modjk.ModJkConf

/**
 * Represents Apache HTTP server in JK scenarios.
 */
class DefaultHttpdConfigurator implements Configurator<DefaultHttpdConfigurator> {

  FacingServerNode facingServerNode
  ModJkConf modJkConf


  DefaultHttpdConfigurator(FacingServerNode facingServerNode) {
    this.facingServerNode = facingServerNode
    this.modJkConf = new ModJkConf().setHttpd(facingServerNode.getServer())
  }

  @Override
  DefaultHttpdConfigurator configure() {
    if (!(facingServerNode.getConfigurators().any { it instanceof ModJkConf} )) {
      facingServerNode.addConfigurations(modJkConf)
    }

    return this
  }

  @Override
  DefaultHttpdConfigurator revertAll() {
    modJkConf.revertAll()

    return this
  }
}
