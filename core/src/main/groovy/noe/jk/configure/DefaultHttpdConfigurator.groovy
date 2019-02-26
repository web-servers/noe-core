package noe.jk.configure

import noe.jk.configure.modjk.ModJkConf
import noe.server.Httpd

/**
 * Configure server to be a mod_jk proxy server. Prepares default mod_jk.conf configuration file.
 *
 * @see FacingServerNode
 * @see ModJkConf
 * @see Configurator
 *
 * Example:<br>
 *   <code>
 *     JkScenario scenario = new JkScenario()
 *       .setFacingServerNode(new FacingServerNode(new Httpd()))
 *       .addBalancerNode(new BalancerNode()
 *         .addWorker(new WorkerNode(new Tomcat(...)))
 *         .addWorker(new WorkerNode(new Tomcat(...))))
 *
 *     NodeOperations ops =
 *       new JkScenarioConfigurator(
 *         scenario,
 *         DefaultHttpdConfigurator.class,
 *         DefaultAS7WorkerConfigurator.class
 *       ).configure()
 *
 *     ops.startAll()
 *
 *     // ...
 *
 *     ops.stopAll()
 *   </code>
 *
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
