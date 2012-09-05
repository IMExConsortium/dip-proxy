<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0"
    xmlns="http://dip.doe-mbi.ucla.edu/services/dxf14"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    >

  <xsl:output method="xml" indent="yes" />
  <xsl:strip-space elements="*" />

  <xsl:param name="edu.ucla.mbi.services.detail" select="stub"/>

  <xsl:template match="/">
    <dataset>
        <xsl:apply-templates />
    </dataset>
  </xsl:template>

  <xsl:template match="ResultSet/Result">
    <xsl:element name="node">
        <xsl:attribute name="ac">
            <xsl:value-of select="i[3]"/>
        </xsl:attribute>
        <xsl:attribute name="ns">sgd</xsl:attribute>
        <xsl:attribute name="id">1</xsl:attribute>

        <xsl:element name="type">
            <xsl:attribute name="name">gene</xsl:attribute>
            <xsl:attribute name="ac">dxf:0025</xsl:attribute>
            <xsl:attribute name="ns">dxf</xsl:attribute>
        </xsl:element>

        <xsl:element name="label">
            <xsl:value-of select="i[2]"/>
        </xsl:element>

        <xsl:element name="name">
            <xsl:value-of select="i[1]"/>
        </xsl:element>

        <xsl:if test="$edu.ucla.mbi.services.detail = 'base'
                        or $edu.ucla.mbi.services.detail = 'full'">

          <xsl:element name="xrefList">
            <xsl:element name="xref">
                <xsl:attribute name="type">produced-by</xsl:attribute>
                <xsl:attribute name="typeAc">dxf:0007</xsl:attribute>
                <xsl:attribute name="typeNs">dxf</xsl:attribute>
                <xsl:attribute name="ac"><xsl:value-of select="i[5]"/></xsl:attribute>
                <xsl:attribute name="ns">taxid</xsl:attribute>

                <xsl:if test="$edu.ucla.mbi.services.detail = 'full'">
                    <xsl:element name="node">
                        <xsl:attribute name="ac">
                            <xsl:value-of select="i[5]"/>
                        </xsl:attribute>
                        
                        <xsl:attribute name="ns">taxid</xsl:attribute>
                        <xsl:attribute name="id">1</xsl:attribute>

                        <xsl:element name="type">
                            <xsl:attribute name="name">organism</xsl:attribute>
                            <xsl:attribute name="ac">dip:0301</xsl:attribute>
                            <xsl:attribute name="ns">dip</xsl:attribute>
                        </xsl:element>

                        <xsl:element name="label">
                            <xsl:value-of select="i[6]"/>
                        </xsl:element>
    
                        <xsl:element name="name">
                            <xsl:value-of select="i[4]"/>
                        </xsl:element>
                    </xsl:element>
                </xsl:if>
            </xsl:element>
          </xsl:element>
        </xsl:if>
    </xsl:element>   
  </xsl:template>

</xsl:stylesheet>
