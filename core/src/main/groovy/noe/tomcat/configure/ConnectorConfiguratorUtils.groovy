package noe.tomcat.configure

final class ConnectorConfiguratorUtils {

    private ConnectorConfiguratorUtils() {
        // no instance creation
    }

    static void createNewUpgradeProtocol(connector, Map<String, Object> attributes) {
        attributes.each {
            if (it.key == "UpgradeProtocol") {
                connector.appendNode ("UpgradeProtocol", ["className": it.value])
            }
        }
    }

    static mapAttributesForConnectorOnly(Map<String, Object> attributes) {
        Map<String, Object> res = [:]

        attributes.each {
            if (it.key != "UpgradeProtocol") {
                res.put(it.key, it.value)
            }
        }
        return res
    }

    static boolean updateExistingUpgradeProtocol(Node connector, attribute) {
        if (attribute.key == "UpgradeProtocol") {
            if (hasUpgradeProtocol(connector)) {
                connector.UpgradeProtocol.@className = attribute.value
            } else {
                connector.appendNode("UpgradeProtocol", ["className": attribute.value])
            }
            return true
        }
        return false
    }

    static boolean hasUpgradeProtocol(Node connector) {
        return connector.UpgradeProtocol.@className == null || connector.UpgradeProtocol.@className in ConnectorTomcatUtils.retrieveAllUpgradeProtocols()
    }
}
