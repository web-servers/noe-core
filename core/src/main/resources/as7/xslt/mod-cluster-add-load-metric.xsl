<?xml version="1.0" encoding="UTF-8"?>
<!-- @author Michal Karm Babacek <mbabacek@redhat.com> -->
<!-- Adds load metric -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="1.0">
    <xsl:output method="xml" indent="yes" />
    <xsl:variable name="nsMC" select="'urn:jboss:domain:modcluster:'" />
    <xsl:param name="pType" select="'@LOAD_METRIC_TYPE@'" />
    <xsl:param name="pWeight" select="'@WEIGHT@'" />

    <!-- Removes load metric of type pType -->
    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsMC)]/*[local-name()='mod-cluster-config' and starts-with(namespace-uri(), $nsMC)]/*[local-name()='dynamic-load-provider' and starts-with(namespace-uri(), $nsMC)]/*[local-name()='load-metric' and starts-with(namespace-uri(), $nsMC) and @type=$pType]"/>

    <!-- Add metric -->
    <xsl:template
        match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsMC)]/*[local-name()='mod-cluster-config' and starts-with(namespace-uri(), $nsMC)]/*[local-name()='dynamic-load-provider' and starts-with(namespace-uri(), $nsMC)]">
        <xsl:variable name="thisns" select="namespace-uri()" />
        <xsl:copy>
            <xsl:element name="load-metric" namespace="{$thisns}">
                <xsl:attribute name="type">
                    <xsl:value-of select="$pType" />
                </xsl:attribute>
                <xsl:attribute name="weight">
                    <xsl:value-of select="$pWeight" />
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