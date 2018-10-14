<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <!-- * @author Michal Karm Babacek <mbabacek@redhat.com> -->
    <xsl:variable name="ns" select="'urn:jboss:domain:'" />

    <xsl:param name="modclusterMcastAddress" select="'@MODCLUSTER_MCAST_ADDRESS@'" />
    <xsl:param name="modclusterMcastPort" select="'@MODCLUSTER_MCAST_PORT@'" />

    <!-- traverse the whole tree, so that all elements and attributes are 
        eventually current node -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*" />
        </xsl:copy>
    </xsl:template>

    <!-- change modcluster multicast port & addresses -->
    <xsl:template
        match="/*[local-name()='server' and starts-with(namespace-uri(), $ns)]
        /*[local-name()='socket-binding-group' and starts-with(namespace-uri(), $ns) and @name='standard-sockets']
        /*[local-name()='socket-binding' and starts-with(namespace-uri(), $ns) and @name='modcluster']">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*" />
            <xsl:attribute name="multicast-address">
                <xsl:value-of select="$modclusterMcastAddress" />
            </xsl:attribute>
            <xsl:attribute name="multicast-port">
                <xsl:value-of select="$modclusterMcastPort" />
            </xsl:attribute>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>