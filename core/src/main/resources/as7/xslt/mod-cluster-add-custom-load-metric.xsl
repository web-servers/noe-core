<?xml version="1.0" encoding="UTF-8"?>
<!-- @author Michal Karm Babacek <mbabacek@redhat.com> -->
<!-- Adds custom load metric -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="1.0">
    <xsl:output method="xml" indent="yes" />
    <xsl:variable name="nsMC" select="'urn:jboss:domain:modcluster:'" />
    <xsl:param name="pHistory" select="'@HISTORY@'" />
    <xsl:param name="pDecay" select="'@DECAY@'" />
    <xsl:param name="pClass" select="'@LOAD_METRIC_CLASS@'" />
    <xsl:param name="pWeight" select="'@WEIGHT@'" />
    <xsl:param name="pCapacity" select="'@CAPACITY@'" />
    <xsl:param name="pLoadfile" select="'@LOAD_FILE@'" />
    <xsl:param name="pParseExpression" select="'@PARSE_EXPRESSION@'" />

    <!-- Removes load metric of particular pClass -->
    <xsl:template
        match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsMC)]/*[local-name()='mod-cluster-config' and starts-with(namespace-uri(), $nsMC)]/*[local-name()='dynamic-load-provider' and starts-with(namespace-uri(), $nsMC)]/*[local-name()='custom-load-metric' and starts-with(namespace-uri(), $nsMC) and @class=$pClass]" />

    <!-- Add metric -->
    <xsl:template
        match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsMC)]/*[local-name()='mod-cluster-config' and starts-with(namespace-uri(), $nsMC)]/*[local-name()='dynamic-load-provider' and starts-with(namespace-uri(), $nsMC)]">
        <xsl:variable name="thisns" select="namespace-uri()" />
        <xsl:copy>
            <xsl:attribute name="history">
                <xsl:value-of select="$pHistory" />
            </xsl:attribute>
            <xsl:attribute name="decay">
                <xsl:value-of select="$pDecay" />
            </xsl:attribute>
            <xsl:element name="custom-load-metric"
                namespace="{$thisns}">
                <xsl:attribute name="class">
                    <xsl:value-of select="$pClass" />
                </xsl:attribute>
                <xsl:attribute name="weight">
                    <xsl:value-of select="$pWeight" />
                </xsl:attribute>
                <xsl:element name="property" namespace="{$thisns}">
                    <xsl:attribute name="name">
                        <xsl:value-of select="'capacity'" />
                    </xsl:attribute>
                    <xsl:attribute name="value">
                        <xsl:value-of select="$pCapacity" />
                    </xsl:attribute>
                </xsl:element>
                <xsl:element name="property" namespace="{$thisns}">
                    <xsl:attribute name="name">
                    <xsl:value-of select="'loadfile'" />
                </xsl:attribute>
                    <xsl:attribute name="value">
                    <xsl:value-of select="$pLoadfile" />
                </xsl:attribute>
                </xsl:element>
                <xsl:element name="property" namespace="{$thisns}">
                    <xsl:attribute name="name">
                    <xsl:value-of select="'parseexpression'" />
                </xsl:attribute>
                    <xsl:attribute name="value">
                    <xsl:value-of select="$pParseExpression" />
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