<?xml version="1.0" encoding="UTF-8"?>
<!-- @author Michal Karm Babacek <mbabacek@redhat.com> -->
<!-- Simply adds an HTTP or AJP Connector -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="1.0">
    <xsl:output method="xml" indent="yes" />
    <xsl:variable name="nsWeb" select="'urn:jboss:domain:web:'" />
    <xsl:param name="pConnector" select="'@AJP_OR_HTTP@'" />
    <xsl:param name="pProtocol" select="'@PROTOCOL@'" />
    <xsl:param name="pSocketBinding" select="'@SOCKET_BINDING@'" />
    <xsl:param name="pEnabled" select="'@ENABLED@'" />
    <xsl:param name="pScheme" select="'@SCHEME@'" />

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