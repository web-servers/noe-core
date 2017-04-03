package noe.jk.configure

import noe.jk.configure.modjk.ModJkConf

/**
 * Represents Apache HTTP server in JK scenarios.
 */
class DefaultHttpdConfigurator implements Configurator<DefaultHttpdConfigurator> {

  FacingServerNode facingServerNode


  DefaultHttpdConfigurator(FacingServerNode facingServerNode) {
    this.facingServerNode = facingServerNode
  }

  @Override
  DefaultHttpdConfigurator configure() {
    if (!(facingServerNode.getConfigurators().any { it instanceof ModJkConf} )) {
      facingServerNode.addConfigurations(new ModJkConf().setHttpd(facingServerNode.getServer()))
    }

    return this
  }

}
