<?xml version="1.0" encoding="UTF-8"?>
<!-- @author Michal Karm Babacek <mbabacek@redhat.com> -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="1.0">
    <xsl:output method="xml" indent="yes" />
    <xsl:variable name="nsMC" select="'urn:jboss:domain:modcluster:'" />

    <!-- Removes everything within <dynamic-load-provider>...</dynamic-load-provider> -->
      <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $nsMC)]/*[local-name()='mod-cluster-config' and starts-with(namespace-uri(), $nsMC)]/*[local-name()='dynamic-load-provider' and starts-with(namespace-uri(), $nsMC)]/*[local-name()='load-metric' and starts-with(namespace-uri(), $nsMC)]"/>

    <!-- Copy everything else untouched. -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>