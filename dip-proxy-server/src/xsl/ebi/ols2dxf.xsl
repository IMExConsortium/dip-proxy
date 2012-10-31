<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0"
    xmlns:dxf="http://dip.doe-mbi.ucla.edu/services/dxf14"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    >

    <xsl:output method="xml" indent="yes" />
    <xsl:param name="edu.ucla.mbi.services.ac"/>
    <xsl:param name="edu.ucla.mbi.services.ns"/>
    <xsl:param name="edu.ucla.mbi.services.detail" select="stub"/>

    <xsl:template match="/">
        <dxf:dataset>
            <xsl:apply-templates />
        </dxf:dataset>
    </xsl:template>

    <xsl:template match="response">
        <xsl:element name="dxf:node">
            <xsl:attribute name="ac">
                <xsl:value-of select="$edu.ucla.mbi.services.ac"/>
            </xsl:attribute>
            <xsl:attribute name="ns">
                <xsl:value-of select="$edu.ucla.mbi.services.ns"/>
            </xsl:attribute>

            <xsl:element name="dxf:type">
                <xsl:attribute name="name">cv-term</xsl:attribute>
                <xsl:attribute name="ac">dxf:0030</xsl:attribute>
                <xsl:attribute name="ns">dxf</xsl:attribute>
            </xsl:element>

            <xsl:element name="dxf:label">
                <xsl:value-of select="item[name='preferred name']/value/text()"/>
            </xsl:element>

            <xsl:if test="$edu.ucla.mbi.services.detail = 'full'">
                <xsl:element name="dxf:attrList">
                    <xsl:element name="dxf:attr">
                        <xsl:attribute name="ns">dxf</xsl:attribute>
                        <xsl:attribute name="ac">dxf:0032</xsl:attribute>
                        <xsl:attribute name="name">definition</xsl:attribute>

                        <xsl:element name="dxf:value">
                            <xsl:value-of select="item[name='definition']/value/text()"/>
                        </xsl:element>
                    </xsl:element>
                </xsl:element>
            </xsl:if>
        </xsl:element>
    </xsl:template>
</xsl:stylesheet>

