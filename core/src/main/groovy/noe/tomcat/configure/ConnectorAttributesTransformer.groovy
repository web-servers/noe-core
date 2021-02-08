package noe.tomcat.configure

/**
 * Provides data for connectors specified in Tomcat server.xml.
 */
class ConnectorAttributesTransformer {

  private final ConnectorTomcatAbstract connector


  ConnectorAttributesTransformer(ConnectorTomcatAbstract connector) {
    this.connector = connector
  }

  /**
   * Provides attributes for non secure HTTP connector.
   * Returns Node applicable into Tomcat server.xml.
   */
  Node nonSecureHttpConnector() {
    return new CommonConnectorTransformer(connector).transform()
  }

  /**
   * Provides attributes for secure HTTP connector.
   * Returns Node applicable into Tomcat server.xml.
   */
  Node secureHttpConnector() {
    return new SecureHttpTransformer(connector).transform()
  }

  /**
   * Provides attributes for AJP connector.
   * Returns Node applicable into Tomcat server.xml.
   */
  Node ajpConnector() {
    return new CommonConnectorTransformer(connector).transform()
  }

  /**
   * Provides map of attributes and valuse corresponding connectors elements in Tomcat server.xml
   * Use for all connectors but `SecureHttpConnectorTomcat`, for it use `SecureHttpTransformer`.
   */
  private static class CommonConnectorTransformer {
    ConnectorTomcatAbstract connector

    CommonConnectorTransformer(ConnectorTomcatAbstract connector) {
      this.connector = connector
    }

    Node transform() {
      Map<String, Object> attributes = [:]

      // -- attributes -------------------------
      if (connector.getPort() != null && connector.getPort() > 0) {
        attributes.put('port', connector.getPort())
      }
      if (connector.getProtocol() != null && !connector.getProtocol().isEmpty()) {
        attributes.put('protocol', connector.getProtocol())
      }
      if (connector.getSecure() != null) {
        attributes.put('secure', connector.getSecure())
      }
      if (connector.getScheme() != null && !connector.getScheme().isEmpty()) {
        attributes.put('scheme', connector.getScheme())
      }

      if (connector.getMaxThreads() != null && connector.getMaxThreads() > 0) {
        attributes.put('maxThreads', connector.getMaxThreads())
      }
      if (connector.getAddress() != null && !connector.getAddress().isEmpty()) {
        attributes.put('address', connector.getAddress())
      }
      if (connector.getConnectionTimeout() != null && connector.getConnectionTimeout() > 0) {
        attributes.put('connectionTimeout', connector.getConnectionTimeout())
      }
      if (connector.getRedirectPort() != null && connector.getRedirectPort() > 0) {
        attributes.put('redirectPort', connector.getRedirectPort())
      }

      if(connector instanceof AjpConnectorTomcat) {
        if (connector.getSecretRequired() != null) {
          attributes.put('secretRequired', connector.getSecretRequired())
        }

        if (connector.getSecret() != null && !connector.getSecret().isEmpty()) {
          attributes.put('secret', connector.getSecret())
        }

        if (connector.getAllowedRequestAttributesPattern() != null && !connector.getAllowedRequestAttributesPattern().isEmpty()) {
          attributes.put('allowedRequestAttributesPattern', connector.getAllowedRequestAttributesPattern())
        }
      }
      // ---------------------


      Node node = new Node(null, "Connector", attributes)


      // -- sub elements -----
      if (connector instanceof NonSecureHttpConnectorTomcat) {
        if (connector.getUpgradeProtocol() != null) {
          node.appendNode("UpgradeProtocol", ['className': connector.getUpgradeProtocol().getClassName()])
        }
      }
      // --------------------

      return node
    }
  }

  private static class SecureHttpTransformer {
    SecureHttpConnectorTomcat connector

    SecureHttpTransformer(SecureHttpConnectorTomcat connector) {
      this.connector = connector
    }

    Node transform() {
      Node node = new CommonConnectorTransformer(connector).transform()
      Map<String, Object> attributes = node.attributes()

      // -- attributes -------------------------
      if (connector.getSslEnabled() != null) {
        attributes.put('SSLEnabled', connector.getSslEnabled())
      }

      // SSL BIO and NIO
      if (connector.getSslProtocol() != null && !connector.getSslProtocol().isEmpty()) {
        attributes.put('sslProtocol', connector.getSslProtocol())
      }
      if (connector.getKeystoreFile() != null && !connector.getKeystoreFile().isEmpty()) {
        attributes.put('keystoreFile', connector.getKeystoreFile())
      }
      if (connector.getKeystorePass() != null && !connector.getKeystorePass().isEmpty()) {
        attributes.put('keystorePass', connector.getKeystorePass())
      }
      if (connector.getKeystoreType() != null && !connector.getKeystoreType().isEmpty()) {
        attributes.put('keystoreType', connector.getKeystoreType())
      }
      if (connector.getTruststoreFile() != null && !connector.getTruststoreFile().isEmpty()) {
        attributes.put('truststoreFile', connector.getTruststoreFile())
      }
      if (connector.getTruststorePass() != null && !connector.getTruststorePass().isEmpty()) {
        attributes.put('truststorePass', connector.getTruststorePass())
      }
      if (connector.getTruststoreType() != null && !connector.getTruststoreType().isEmpty()) {
        attributes.put('truststoreType', connector.getTruststoreType())
      }
      if (connector.getClientAuth() != null) {
        attributes.put('clientAuth', connector.getClientAuth())
      }

      // HTTP2
      if (connector.getSslImplementationName() != null && !connector.getSslImplementationName().isEmpty()) {
        attributes.put('sslImplementationName', connector.getSslImplementationName())
      }

      // SSL APR
      if (connector.getSslCertificateFile() != null && !connector.getSslCertificateFile().isEmpty()) {
        attributes.put('SSLCertificateFile', connector.getSslCertificateFile())
      }
      if (connector.getSslCACertificateFile() != null && !connector.getSslCACertificateFile().isEmpty()) {
        attributes.put('SSLCACertificateFile', connector.getSslCACertificateFile())
      }
      if (connector.getSslCertificateKeyFile() != null && !connector.getSslCertificateKeyFile().isEmpty()) {
        attributes.put('SSLCertificateKeyFile', connector.getSslCertificateKeyFile())
      }
      if (connector.getSslPassword() != null && !connector.getSslPassword().isEmpty()) {
        attributes.put('SSLPassword', connector.getSslPassword())
      }
      if (connector.getSslEnabledProtocols() != null && !connector.getSslEnabledProtocols().isEmpty()) {
        attributes.put('sslEnabledProtocols', connector.getSslEnabledProtocols())
      }
      // ---------------------

      if (connector.getUpgradeProtocol() != null) {
        node.appendNode("UpgradeProtocol", ['className': connector.getUpgradeProtocol().getClassName()])
      }

      if (connector.getSSLHostConfigs() != null) {
        for (ConnectorSSLHostConfigTomcat sslHostConfig : connector.getSSLHostConfigs()) {

          Map<String, Object> sslHostConfigAttributes = [:]

          if (sslHostConfig.getHostName() != null && !sslHostConfig.getHostName().isEmpty()) {
            sslHostConfigAttributes.put('hostName', sslHostConfig.getHostName())
          }
          if (sslHostConfig.getCertificateVerification() != null && !sslHostConfig.getCertificateVerification().isEmpty()) {
            sslHostConfigAttributes.put('certificateVerification', sslHostConfig.getCertificateVerification())
          }
          if (sslHostConfig.getCaCertificateFile() != null && !sslHostConfig.getCaCertificateFile().isEmpty()) {
            sslHostConfigAttributes.put('caCertificateFile', sslHostConfig.getCaCertificateFile())
          }
          if (sslHostConfig.getCaCertificatePath() != null && !sslHostConfig.getCaCertificatePath().isEmpty()) {
            sslHostConfigAttributes.put('caCertificatePath', sslHostConfig.getCaCertificatePath())
          }
          if (sslHostConfig.getCiphers() != null && !sslHostConfig.getCiphers().isEmpty()) {
            sslHostConfigAttributes.put('ciphers', sslHostConfig.getCiphers())
          }
          if (sslHostConfig.getSslProtocol() != null && !sslHostConfig.getSslProtocol().isEmpty()) {
            sslHostConfigAttributes.put('sslProtocol', sslHostConfig.getSslProtocol())
          }
          if (sslHostConfig.getProtocols() != null && !sslHostConfig.getProtocols().isEmpty()) {
            sslHostConfigAttributes.put('protocols', sslHostConfig.getProtocols())
          }
          if (sslHostConfig.getTruststoreFile() != null && !sslHostConfig.getTruststoreFile().isEmpty()) {
            sslHostConfigAttributes.put('truststoreFile', sslHostConfig.getTruststoreFile())
          }
          if (sslHostConfig.getTruststorePassword() != null && !sslHostConfig.getTruststorePassword().isEmpty()) {
            sslHostConfigAttributes.put('truststorePassword', sslHostConfig.getTruststorePassword())
          }
          if (sslHostConfig.getTruststoreProvider() != null && !sslHostConfig.getTruststoreProvider().isEmpty()) {
            sslHostConfigAttributes.put('truststoreProvider', sslHostConfig.getTruststoreProvider())
          }
          if (sslHostConfig.getTruststoreType() != null && !sslHostConfig.getTruststoreType().isEmpty()) {
            sslHostConfigAttributes.put('truststoreType', sslHostConfig.getTruststoreType())
          }

          Node sslHostConfigNode = node.appendNode("SSLHostConfig", sslHostConfigAttributes)

          if (sslHostConfig.getCertificate() != null ) {
            Map<String, Object> certificateAttributes = [:]
            ConnectorCertificateTomcat certificate = sslHostConfig.getCertificate()

            if (certificate.getCertificateFile() != null && !certificate.getCertificateFile().isEmpty()) {
              certificateAttributes.put('certificateFile', certificate.getCertificateFile())
            }
            if (certificate.getCertificateChainFile() != null && !certificate.getCertificateChainFile().isEmpty()) {
              certificateAttributes.put('certificateChainFile', certificate.getCertificateChainFile())
            }
            if (certificate.getCertificateKeyAlias() != null && !certificate.getCertificateKeyAlias().isEmpty()) {
              certificateAttributes.put('certificateKeyAlias', certificate.getCertificateKeyAlias())
            }
            if (certificate.getCertificateKeyFile() != null && !certificate.getCertificateKeyFile().isEmpty()) {
              certificateAttributes.put('certificateKeyFile', certificate.getCertificateKeyFile())
            }
            if (certificate.getCertificateKeyPassword() != null && !certificate.getCertificateKeyPassword().isEmpty()) {
              certificateAttributes.put('certificateKeyPassword', certificate.getCertificateKeyPassword())
            }
            if (certificate.getCertificateKeystoreFile() != null && !certificate.getCertificateKeystoreFile().isEmpty()) {
              certificateAttributes.put('certificateKeystoreFile', certificate.getCertificateKeystoreFile())
            }
            if (certificate.getCertificateKeystorePassword() != null && !certificate.getCertificateKeystorePassword().isEmpty()) {
              certificateAttributes.put('certificateKeystorePassword', certificate.getCertificateKeystorePassword())
            }
            if (certificate.getCertificateKeystoreProvider() != null && !certificate.getCertificateKeystoreProvider().isEmpty()) {
              certificateAttributes.put('certificateKeystoreProvider', certificate.getCertificateKeystoreProvider())
            }
            if (certificate.getCertificateKeystoreType() != null && !certificate.getCertificateKeystoreType().isEmpty()) {
              certificateAttributes.put('certificateKeystoreType', certificate.getCertificateKeystoreType())
            }
            if (certificate.getCertificateType() != null && !certificate.getCertificateType().isEmpty()) {
              certificateAttributes.put('certificateType', certificate.getCertificateType())
            }

            sslHostConfigNode.appendNode("Certificate", certificateAttributes)
          }
        }
      }

      return node
    }
  }

}
