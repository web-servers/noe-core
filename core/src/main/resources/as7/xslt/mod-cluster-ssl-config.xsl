<?xml version="1.0" encoding="UTF-8"?>
<!-- @author Michal Karm Babacek <mbabacek@redhat.com> -->
<!-- Adds ssl configuration to mod_cluster -->
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes" />
    <xsl:strip-space elements="*" />
    <xsl:variable name="nsModCluster" select="'urn:jboss:domain:modcluster:'" />
    <xsl:param name="pCAcertificateFile" select="'@CA_CERTIFICATE_FILE@'" />
    <xsl:param name="pCertificateKeyFile" select="'@CERTIFICATE_KEY_FILE@'" />
    <xsl:param name="pPassword" select="'@PASSWORD@'" />
    <xsl:param name="pKeyAlias" select="'@KEY_ALIAS@'" />
    <xsl:param name="pCipherSuite" select="'@CIPHER_SUITE@'" />
    <xsl:param name="pProtocol" select="'@SSL_PROTOCOL@'" />

    <!-- Remove ssl element from within the mod-cluster-config -->
    <xsl:template
        match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsModCluster)]
                /*[local-name()='mod-cluster-config' and starts-with(namespace-uri(), $nsModCluster)]
                /*[local-name()='ssl' and starts-with(namespace-uri(), $nsModCluster)]" />

    <!-- Add ssl element to mod-cluster-config -->
    <xsl:template
        match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsModCluster)]
                /*[local-name()='mod-cluster-config' and starts-with(namespace-uri(), $nsModCluster)]">
        <xsl:variable name="thisns" select="namespace-uri()" />
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />

            <xsl:element name="ssl" namespace="{$thisns}">
                <xsl:attribute name="ca-certificate-file">
                    <xsl:value-of select="$pCAcertificateFile" />
                </xsl:attribute>
                <xsl:attribute name="certificate-key-file">
                    <xsl:value-of select="$pCertificateKeyFile" />
                </xsl:attribute>
                <xsl:attribute name="password">
                    <xsl:value-of select="$pPassword" />
                </xsl:attribute>
                <xsl:attribute name="key-alias">
                    <xsl:value-of select="$pKeyAlias" />
                </xsl:attribute>
                <xsl:attribute name="cipher-suite">
                    <xsl:value-of select="$pCipherSuite" />
                </xsl:attribute>
                <xsl:attribute name="protocol">
                    <xsl:value-of select="$pProtocol" />
                </xsl:attribute>
            </xsl:element>

        </xsl:copy>
    </xsl:template>

    <!-- Copy everything else untouched -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
