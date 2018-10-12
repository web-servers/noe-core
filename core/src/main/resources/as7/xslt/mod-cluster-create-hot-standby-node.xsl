<?xml version="1.0" encoding="UTF-8"?>
<!-- @author Michal Karm Babacek <mbabacek@redhat.com> -->
<!-- Removes dynamic-load-provider and adds simple-load-provider -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="1.0">
    <xsl:output method="xml" indent="yes" />
    <xsl:variable name="nsMC" select="'urn:jboss:domain:modcluster:'" />

    <!-- Removes dynamic-load-provider -->
    <xsl:template
        match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsMC)]/*[local-name()='mod-cluster-config' and starts-with(namespace-uri(), $nsMC)]/*[local-name()='dynamic-load-provider' and starts-with(namespace-uri(), $nsMC)]" />

    <!-- Add metric -->
    <xsl:template
        match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsMC)]/*[local-name()='mod-cluster-config' and starts-with(namespace-uri(), $nsMC)]">
        <xsl:variable name="thisns" select="namespace-uri()" />
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
            <xsl:element name="simple-load-provider"
                namespace="{$thisns}">
                <xsl:attribute name="factor">
                    <xsl:value-of select="0" />
                </xsl:attribute>
            </xsl:element>
        </xsl:copy>
    </xsl:template>

    <!-- Copy everything else untouched. -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>