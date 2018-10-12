<?xml version="1.0" encoding="UTF-8"?>
<!-- @author Michal Karm Babacek <mbabacek@redhat.com> -->
<!-- Adds a jar resource to module.xml -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="1.0">
    <xsl:output method="xml" indent="yes" />
    <xsl:variable name="nsMC" select="'urn:jboss:module:'" />
    <xsl:param name="pJar" select="'@JAR@'" />

    <!-- Removes resource of path pJar -->
    <xsl:template
        match="//*[local-name()='module' and starts-with(namespace-uri(), $nsMC)]/*[local-name()='resources' and starts-with(namespace-uri(), $nsMC)]/*[local-name()='resource-root' and starts-with(namespace-uri(), $nsMC) and @path=$pJar]" />

    <!-- Add metric -->
    <xsl:template
        match="//*[local-name()='module' and starts-with(namespace-uri(), $nsMC)]/*[local-name()='resources' and starts-with(namespace-uri(), $nsMC)]">
        <xsl:variable name="thisns" select="namespace-uri()" />
        <xsl:copy>
            <xsl:element name="resource-root" namespace="{$thisns}">
                <xsl:attribute name="path">
                    <xsl:value-of select="$pJar" />
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