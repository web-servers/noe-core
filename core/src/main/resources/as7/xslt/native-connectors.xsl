<?xml version="1.0" encoding="UTF-8"?>
<!-- @author Luis Barreiro lbarreiro@redhat.com -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="1.0">
    <xsl:output method="xml" indent="yes" />

    <xsl:variable name="nsWeb" select="'urn:jboss:domain:web:'" />

    <xsl:param name="pNative" select="'@NATIVE_CONNECTORS_BOOLEAN@'" />

    <!-- On the web subsystem add the native attribute -->
    <xsl:template
        match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsWeb)]">
        <xsl:copy>
            <xsl:attribute name="native">
                <xsl:value-of select="$pNative" />
            </xsl:attribute>
            <xsl:apply-templates select="node()|@*" />
        </xsl:copy>
    </xsl:template>

    <!-- Copy everything else -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*" />
        </xsl:copy>
    </xsl:template>

    <!-- For elements already with the native attribute -->
    <xsl:template
        match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsWeb)]/@native">
        <xsl:attribute name="native">
                <xsl:value-of select="$pNative" />
        </xsl:attribute>
    </xsl:template>

</xsl:stylesheet>