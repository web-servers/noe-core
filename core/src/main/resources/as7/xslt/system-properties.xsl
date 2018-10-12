<?xml version="1.0" encoding="UTF-8"?>
<!-- @author Michal Karm Babacek <mbabacek@redhat.com> -->
<!-- This sheet allows you to manipulate system properties :-) It can handle 
    even odd situations like already existing property of the same name etc. -->
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes" />
    <xsl:strip-space elements="*" />
    <xsl:variable name="ns" select="'urn:jboss:domain:'" />
    <xsl:param name="pName" select="'@SYSTEM_PROPERTY_NAME@'" />
    <xsl:param name="pValue" select="'@SYSTEM_PROPERTY_VALUE@'" />

    <!-- Remove all properties with the same name. -->
    <xsl:template
        match="//*[local-name()='system-properties' and starts-with(namespace-uri(), $ns)]
        /*[local-name()='property' and starts-with(namespace-uri(), $ns) and @name=$pName]" />

    <!-- If there is actually no system-properties element, we add it and 
        its property child as well. -->
    <xsl:template
        match="//*[local-name()='extensions' and starts-with(namespace-uri(), $ns)]">
        <xsl:variable name="thisns" select="namespace-uri()" />
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
        <xsl:if
            test="not(//*[local-name()='system-properties' and starts-with(namespace-uri(), $ns)])">
            <xsl:element name="system-properties" namespace="{$thisns}">
                <xsl:element name="property" namespace="{$thisns}">
                    <xsl:attribute name="name">
                        <xsl:value-of select="$pName" />
                    </xsl:attribute>
                    <xsl:attribute name="value">
                        <xsl:value-of select="$pValue" />
                    </xsl:attribute>
                </xsl:element>
            </xsl:element>
        </xsl:if>
    </xsl:template>

    <!-- If system-properties element exists already, we add our property 
        in it. -->
    <xsl:template
        match="//*[local-name()='system-properties' and starts-with(namespace-uri(), $ns)]">
        <xsl:variable name="thisns" select="namespace-uri()" />
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />

            <xsl:element name="property" namespace="{$thisns}">
                <xsl:attribute name="name">
                        <xsl:value-of select="$pName" />
                    </xsl:attribute>
                <xsl:attribute name="value">
                        <xsl:value-of select="$pValue" />
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
