package noe.tomcat.configure

/***
 * IMPORTANT: For usage within noe-core:tomcat.configure only
 *
 * Connectors configuration in Tomcat server.xml
 * Supported connectors: HTTP, (HTTPS), AJP
 *
 * It is user responsibility to set attributes of connectors semantically.
 *
 * It is expected that one or zero non-secure HTTP connector and one or zero secure HTTP connector
 * will be present in server.xml.
 * Limitation is because there is no possibility to specify connectors IDs.
 */
class ConnectorConfiguratorTomcat {
  private final Node server

  public ConnectorConfiguratorTomcat(Node server) {
    this.server = server
  }

  /**
   * Configure not secure HTTP connector.
   * Returns updated server element, see `ConnectorConfiguratorTomcat#server`
   */
  public Node defineHttpConnector(NonSecureHttpConnectorTomcat connector) {
    int httpConnectorSize = loadHttpConnectorSize()

    if (httpConnectorSize > 1) {
      throw new IllegalStateException("Unexpected server.xml format - one non secure connector expected at most")
    }
    if (httpConnectorSize == 1) {
      Node Connector = loadExistingHttpConnector()
      updateExistingConnector(Connector, new ConnectorAttributesTransformer(connector).nonSecureHttpConnector())
    } else {
      createNewConnector(new ConnectorAttributesTransformer(connector).nonSecureHttpConnector())
    }

    return server
  }

  /**
   * Configure secure HTTP connector
   * Returns updated server element, see `ConnectorConfiguratorTomcat#server`
   */
  public Node defineHttpsConnector(SecureHttpConnectorTomcat connector) {
    int httpsConnectorSize = loadHttpsConnectorSize()

    if (httpsConnectorSize > 1) {
      throw new IllegalStateException("Unexpected server.xml format - one secure connector expected at most")
    }
    if (httpsConnectorSize == 1) {
      Node Connector = loadExistingHttpsConnector()
      updateExistingConnector(Connector, new ConnectorAttributesTransformer(connector).secureHttpConnector())
    } else {
      createNewConnector(new ConnectorAttributesTransformer(connector).secureHttpConnector())
    }

    defineRedirectPorts(connector.getPort())

    return server
  }

  /**
   * Configure AJP connector
   * Returns updated server element, see `ConnectorConfiguratorTomcat#server`
   */
  public Node defineAjpConnector(AjpConnectorTomcat connector) {
    int ajpConnectorSize = loadAjpConnectorSize()

    if (ajpConnectorSize > 1) {
      throw new IllegalStateException("Unexpected server.xml format - one AJP connector expected at most")
    }
    if (ajpConnectorSize == 1) {
      Node Connector = loadExistingAjpConnector()
      updateExistingConnector(Connector, new ConnectorAttributesTransformer(connector).ajpConnector())
    } else {
      createNewConnector(new ConnectorAttributesTransformer(connector).ajpConnector())
    }

    return server
  }

  private void defineRedirectPorts(Integer port) {
    defineHttpConnector(new NonSecureHttpConnectorTomcat().setRedirectPort(port))
    defineAjpConnector(new AjpConnectorTomcat().setRedirectPort(port))
  }

  private Node loadExistingHttpConnector() {
    def connectors = server.Service.Connector.findAll { connector -> hasHttpProtocol(connector) && !isSecured(connector) }
    if (connectors.size() == 1) {
      return connectors.first()
    } else if (connectors.size() < 1) {
      return null
    } else {
      throw new IllegalStateException("Unexpected server.xml format - one http connector expected at most")
    }
  }

  private Node loadExistingHttpsConnector() {
    def connectors = server.Service.Connector.findAll { connector -> hasHttpProtocol(connector) && isSecured(connector) }
    if (connectors.size() == 1) {
      return connectors.first()
    } else if (connectors.size() < 1) {
      return null
    } else {
      throw new IllegalStateException("Unexpected server.xml format - one https connector expected at most")
    }
  }

  private Node loadExistingAjpConnector() {
    def connectors = server.Service.Connector.findAll { connector -> hasAjpProtocol(connector) }
    if (connectors.size() == 1) {
      return connectors.first()
    } else if (connectors.size() < 1) {
      return null
    } else {
      throw new IllegalStateException("Unexpected server.xml format - one ajp connector expected at most")
    }
  }

  private boolean isSecured(Node connector) {
    return connector.@secure.toString().toLowerCase().trim() == "true"
  }

  private void createNewConnector(Map<String, Object> attributes) {
    Node connector

    UpgradeProtocol upgradeProtocol = (UpgradeProtocol) attributes.remove('UpgradeProtocol')

    server.Service.each { service ->
      connector = service.appendNode("Connector", attributes)
    }

    if (upgradeProtocol != null) ConnectorConfiguratorUtils.createNewUpgradeProtocol(connector, upgradeProtocol)
  }

  private void updateExistingConnector(Node Connector, Map<String, Object> attributes) {
    UpgradeProtocol upgradeProtocol = (UpgradeProtocol) attributes.remove('UpgradeProtocol')

    attributes.each {
        Connector.@"${it.key}" = it.value
    }

    if (upgradeProtocol != null) {
      ConnectorConfiguratorUtils.updateExistingUpgradeProtocol(Connector, upgradeProtocol)
    } else ConnectorConfiguratorUtils.removeExistingUpgradeProtocol(Connector)
  }

  /**
   * Returns count of non secure HTTP connectors
   */
  public int loadHttpConnectorSize() {
    def connectors = server.Service.Connector.findAll { connector -> hasHttpProtocol(connector) && !isSecured(connector) }
    return connectors.size()
  }

  /**
   * Returns count of secure HTTP connectors
   */
  public int loadHttpsConnectorSize() {
    def connectors = server.Service.Connector.findAll { connector -> hasHttpProtocol(connector) && isSecured(connector) }
    return connectors.size()
  }

  /**
   * Returns count of AJP secure HTTP connectors
   */
  public int loadAjpConnectorSize() {
    def connectors = server.Service.Connector.findAll { connector -> hasAjpProtocol(connector) }
    return connectors.size()
  }

  private boolean hasHttpProtocol(Node connector) {
    return connector.@protocol == null || connector.@protocol.toString() in ConnectorTomcatUtils.retrieveAllHttpProtocols()
  }

  private boolean hasAjpProtocol(Node connector) {
    return connector.@protocol != null && connector.@protocol.toString() in ConnectorTomcatUtils.retrieveAllAjpProtocols()
  }

}
