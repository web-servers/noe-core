<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <!-- @author: Richard Achmatowicz -->
    <xsl:variable name="ns" select="'urn:jboss:domain:'" />

    <!-- IP addresses -->
    <xsl:param name="managementIPAddress" select="'@MANAGEMENT_IP_ADDRESS@'" />
    <xsl:param name="publicIPAddress" select="'@PUBLIC_IP_ADDRESS@'" />
    <xsl:param name="privateIPAddress" select="'@PRIVATE_IP_ADDRESS@'" />

    <!-- mcast addresses -->
    <xsl:param name="udpMcastAddress" select="'@UDP_MCAST_ADDRESS@'" />
    <xsl:param name="diagnosticsMcastAddress" select="'@DIAGNOSTICS_MCAST_ADDRESS@'" />
    <xsl:param name="mpingMcastAddress" select="'@MPING_MCAST_ADDRESS@'" />
    <xsl:param name="modclusterMcastAddress" select="'@MODCLUSTER_MCAST_ADDRESS@'" />

    <!-- traverse the whole tree, so that all elements and attributes are 
        eventually current node -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*" />
        </xsl:copy>
    </xsl:template>

    <!-- change the public IP addresses -->
    <xsl:template
        match="/*[local-name()='server' and starts-with(namespace-uri(), $ns)]
      /*[local-name()='interfaces' and starts-with(namespace-uri(), $ns)]
      /*[local-name()='interface' and starts-with(namespace-uri(), $ns) and @name='public']
      /*[local-name()='inet-address' and starts-with(namespace-uri(), $ns)]">
        <xsl:copy>
            <xsl:attribute name="value">
            <xsl:value-of select="$publicIPAddress" />
        </xsl:attribute>
        </xsl:copy>
    </xsl:template>

    <!-- change the private IP addresses introduced with EAP 7 -->
    <!-- change the private IP addresses -->
    <xsl:template
            match="/*[local-name()='server' and starts-with(namespace-uri(), $ns)]
      /*[local-name()='interfaces' and starts-with(namespace-uri(), $ns)]
      /*[local-name()='interface' and starts-with(namespace-uri(), $ns) and @name='private']
      /*[local-name()='inet-address' and starts-with(namespace-uri(), $ns)]">
        <xsl:copy>
            <xsl:attribute name="value">
                <xsl:value-of select="$privateIPAddress" />
            </xsl:attribute>
        </xsl:copy>
    </xsl:template>

    <!-- change the management IP addresses -->
    <xsl:template
        match="/*[local-name()='server' and starts-with(namespace-uri(), $ns)]
        /*[local-name()='interfaces' and starts-with(namespace-uri(), $ns)]
        /*[local-name()='interface' and starts-with(namespace-uri(), $ns) and @name='management']
        /*[local-name()='inet-address' and starts-with(namespace-uri(), $ns)]">
        <xsl:copy>
            <xsl:attribute name="value">
            <xsl:value-of select="$managementIPAddress" />
        </xsl:attribute>
        </xsl:copy>
    </xsl:template>

    <!-- change udp multicast addresses -->
    <xsl:template
        match="/*[local-name()='server' and starts-with(namespace-uri(), $ns)]
        /*[local-name()='socket-binding-group' and starts-with(namespace-uri(), $ns) and @name='standard-sockets']
        /*[local-name()='socket-binding' and starts-with(namespace-uri(), $ns) and @name='jgroups-udp']">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*" />
            <xsl:attribute name="multicast-address">
            <xsl:value-of select="$udpMcastAddress" />
        </xsl:attribute>
        </xsl:copy>
    </xsl:template>

    <!-- change diagnostics multicast addresses -->
    <xsl:template
        match="/*[local-name()='server' and starts-with(namespace-uri(), $ns)]
        /*[local-name()='socket-binding-group' and starts-with(namespace-uri(), $ns) and @name='standard-sockets']
        /*[local-name()='socket-binding' and starts-with(namespace-uri(), $ns) and @name='jgroups-diagnostics']">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*" />
            <xsl:attribute name="multicast-address">
            <xsl:value-of select="$diagnosticsMcastAddress" />
        </xsl:attribute>
        </xsl:copy>
    </xsl:template>

    <!-- change MPING multicast addresses -->
    <xsl:template
        match="/*[local-name()='server' and starts-with(namespace-uri(), $ns)]
        /*[local-name()='socket-binding-group' and starts-with(namespace-uri(), $ns) and @name='standard-sockets']
        /*[local-name()='socket-binding' and starts-with(namespace-uri(), $ns) and @name='jgroups-mping']">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*" />
            <xsl:attribute name="multicast-address">
            <xsl:value-of select="$mpingMcastAddress" />
        </xsl:attribute>
        </xsl:copy>
    </xsl:template>

    <!-- change modcluster multicast addresses -->
    <xsl:template
        match="/*[local-name()='server' and starts-with(namespace-uri(), $ns)]
        /*[local-name()='socket-binding-group' and starts-with(namespace-uri(), $ns) and @name='standard-sockets']
        /*[local-name()='socket-binding' and starts-with(namespace-uri(), $ns) and @name='modcluster']">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*" />
            <xsl:attribute name="multicast-address">
            <xsl:value-of select="$modclusterMcastAddress" />
        </xsl:attribute>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
