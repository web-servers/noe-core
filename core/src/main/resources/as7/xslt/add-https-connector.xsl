<?xml version="1.0" encoding="UTF-8"?>
<!-- @author Michal Karm Babacek <mbabacek@redhat.com> -->
<!-- Adds HTTPS connector -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="1.0">
    <xsl:output method="xml" indent="yes" />
    <xsl:variable name="nsWeb" select="'urn:jboss:domain:web:'" />
    <xsl:param name="pConnector" select="'@CONNECTOR_NAME@'" />
    <xsl:param name="pProtocol" select="'@PROTOCOL@'" />
    <xsl:param name="pSocketBinding" select="'@SOCKET_BINDING@'" />
    <xsl:param name="pEnabled" select="'@ENABLED@'" />
    <xsl:param name="pScheme" select="'@SCHEME@'" />
    <xsl:param name="pSecure" select="'@SECURE@'" />

    <xsl:param name="pCAcertificateFile" select="'@CA_CERTIFICATE_FILE@'" />
    <xsl:param name="pCertificateKeyFile" select="'@CERTIFICATE_KEY_FILE@'" />
    <xsl:param name="pCertificateFile" select="'@CERTIFICATE_FILE@'" />
    <xsl:param name="pPassword" select="'@PASSWORD@'" />
    <xsl:param name="pVerifyClient" select="'@VERIFY_CLIENT@'" />
    <xsl:param name="pKeyAlias" select="'@KEY_ALIAS@'" />
    <xsl:param name="pCipherSuite" select="'@CIPHER_SUITE@'" />
    <xsl:param name="pSSLProtocol" select="'@SSL_PROTOCOL@'" />

    <!-- Removes the connector with a given name. -->
    <xsl:template
        match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsWeb)]
        /*[local-name()='connector' and starts-with(namespace-uri(), $nsWeb) and @name=$pConnector]" />

    <!-- Add connector -->
    <xsl:template
        match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsWeb)]">
        <xsl:variable name="thisns" select="namespace-uri()" />
        <xsl:copy>
            <xsl:element name="connector" namespace="{$thisns}">
                <xsl:attribute name="name">
                    <xsl:value-of select="$pConnector" />
                </xsl:attribute>
                <xsl:attribute name="protocol">
                    <xsl:value-of select="$pProtocol" />
                </xsl:attribute>
                <xsl:attribute name="socket-binding">
                    <xsl:value-of select="$pSocketBinding" />
                </xsl:attribute>
                <xsl:attribute name="scheme">
                    <xsl:value-of select="$pScheme" />
                </xsl:attribute>
                <xsl:attribute name="enabled">
                    <xsl:value-of select="$pEnabled" />
                </xsl:attribute>
                <xsl:attribute name="secure">
                    <xsl:value-of select="$pSecure" />
                </xsl:attribute>
                <xsl:element name="ssl" namespace="{$thisns}">
                    <xsl:attribute name="name">
                        <xsl:value-of select="$pConnector" />
                        </xsl:attribute>
                    <xsl:attribute name="ca-certificate-file">
                        <xsl:value-of select="$pCAcertificateFile" />
                        </xsl:attribute>
                    <xsl:attribute name="certificate-key-file">
                        <xsl:value-of select="$pCertificateKeyFile" />
                        </xsl:attribute>
                    <xsl:attribute name="certificate-file">
                        <xsl:value-of select="$pCertificateFile" />
                        </xsl:attribute>
                    <xsl:attribute name="password">
                        <xsl:value-of select="$pPassword" />
                        </xsl:attribute>
                    <xsl:attribute name="verify-client">
                        <xsl:value-of select="$pVerifyClient" />
                        </xsl:attribute>
                    <xsl:attribute name="key-alias">
                        <xsl:value-of select="$pKeyAlias" />
                        </xsl:attribute>
                    <xsl:attribute name="cipher-suite">
                        <xsl:value-of select="$pCipherSuite" />
                        </xsl:attribute>
                    <xsl:attribute name="protocol">
                        <xsl:value-of select="$pSSLProtocol" />
                        </xsl:attribute>
                </xsl:element>
            </xsl:element>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

    <!-- Copy everything else untouched. -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>