<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0"
    xmlns:dxf="http://dip.doe-mbi.ucla.edu/services/dxf14"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    >

  <xsl:output method="xml" indent="yes" />
  <xsl:strip-space elements="*" />

  <xsl:param name="edu.ucla.mbi.services.detail" select="stub"/>

  <xsl:template match="/">
    <ns1:dataset>
        <xsl:apply-templates />
    </ns1:dataset>
  </xsl:template>

  <xsl:template match="<ResultSet><Result>">
    <xsl:element name="ns1:node">
        <xsl:attribute name="ac">
            <xsl:value-of select="i[3]"/>
        </xsl:attribute>
        <xsl:attribute name="ns">sgd</xsl:attribute>
        <xsl:attribute name="id">1</xsl:attribute>

        <xsl:element name="ns1:type">
            <xsl:attribute name="name">gene</xsl:attribute>
            <xsl:attribute name="ac">dxf:0025</xsl:attribute>
            <xsl:attribute name="ns">dxf</xsl:attribute>
        </xsl:element>

        <xsl:element name="ns1:label">
            <xsl:value-of select="i[2]"/>
        </xsl:element>

        <xsl:element name="ns1:name">
            <xsl:value-of select="i[1]"/>
        </xsl:element>

        <xsl:if test="$edu.ucla.mbi.services.detail = 'base|full'">
          <xsl:element name="ns1:xrefList">
            <xsl:element name="ns1:xref">
                <xsl:attribute name="type">produced-by</xsl:attribute>
                <xsl:attribute name="typeAc">dxf:0007</xsl:attribute>
                <xsl:attribute name="typeNs">dxf</xsl:attribute>
                <xsl:attribute name="ac"><xsl:value-of select="i[5]"/></xsl:attribute>
                <xsl:attribute name="ns">taxid</xsl:attribute>

                <xsl:if test="$edu.ucla.mbi.services.detail = 'full'">
                    <xsl:element name="ns1:node">
                        <xsl:attribute name="ac"><xsl:value-of select="i]5]"/></xsl:attribute>
                        <xsl:attribute name="ns">taxid</xsl:attribute>
                        <xsl:attribute name="id">1</xsl:attribute>

                        <xsl:element name="ns1:type">
                            <xsl:attribute name="name">organism</xsl:attribute>
                            <xsl:attribute name="ac">dip:0301</xsl:attribute>
                            <xsl:attribute name="ns">dip</xsl:attribute>
                        </xsl:element>

                        <xsl:element name="ns1:label">
                            <xsl:value-of select="i[6]"/>
                        </xsl:element>
    
                        <xsl:element name="ns1:name">
                            <xsl:value-of select="i[4]"/>
                        </xsl:element>
                    </xsl:element>
                </xsl:if>
            </xsl:element>
        </xsl:element>
     </xsl:if>   
  </xsl:template>
</xsl:stylesheet>
