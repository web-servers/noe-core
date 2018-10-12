<?xml version="1.0" encoding="UTF-8"?>
<!-- @author Michal Karm Babacek <mbabacek@redhat.com> -->
<!-- This sheet just removes a connector named. -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="1.0">
    <xsl:output method="xml" indent="yes" />
    <xsl:variable name="nsWeb" select="'urn:jboss:domain:web:'" />
    <xsl:param name="pConnector" select="'@CONNECTOR_NAME@'" />

    <!-- Removes the connector with a given name. -->
    <xsl:template
        match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsWeb)]
        /*[local-name()='connector' and starts-with(namespace-uri(), $nsWeb) and @name=$pConnector]" />

    <!-- Copy everything else untouched. -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>