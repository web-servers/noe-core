package noe.common.utils

import groovy.util.slurpersupport.GPathResult
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil

/**
 * @author rhatlapa (rhatlapa@redhat.com)
 */
class XmlUtils {

  static void writeXmlToFile(File file, GPathResult xml) {
    file.withWriter { outWriter ->
      XmlUtil.serialize(new StreamingMarkupBuilder().bind {
        mkp.declareNamespace('': xml[0].namespaceURI())
        mkp.yield xml
      }, outWriter)
    }
  }
}
