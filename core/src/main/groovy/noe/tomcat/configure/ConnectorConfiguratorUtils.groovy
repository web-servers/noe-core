package noe.tomcat.configure

final class ConnectorConfiguratorUtils {

    private ConnectorConfiguratorUtils() {
        // no instance creation
    }

    static void createNewUpgradeProtocol(Node connector, UpgradeProtocol upgradeProtocol) {
        Map<String, String> upgradeProtocolAttributes = new HashMap<String, String>()

            if (upgradeProtocol.className != null && !upgradeProtocol.className.isEmpty()) {
                upgradeProtocolAttributes.put('className', upgradeProtocol.className)
            }

        connector.appendNode ("UpgradeProtocol", upgradeProtocolAttributes)
    }

    static void updateExistingUpgradeProtocol(Node connector, UpgradeProtocol upgradeProtocol) {
        if (connector.UpgradeProtocol.find { true } != null) {
            removeExistingUpgradeProtocol(connector)
        }
        createNewUpgradeProtocol(connector, upgradeProtocol)
    }

    static void removeExistingUpgradeProtocol(Node connector) {
        connector.remove(connector.UpgradeProtocol.find { true })
    }

}
