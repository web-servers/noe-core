package noe.jk.configure

/**
 * Most general element in JK scenarios.<br/>
 * It could be worker or balancer for example.
 *
 * @see BalancerNode
 * @see StatusWorkerNode
 * @see WorkerNode
 */
interface JkNode {

  /**
   * ID has to be universal in JK scenario.
   */
  String getId()

  /**
   * Returns URLs what are mapped to JK node.
   */
  List<String> getUrlsMap()
}
