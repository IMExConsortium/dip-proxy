<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0"
   xmlns:ns1="http://dip.doe-mbi.ucla.edu/services/dxf14"
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   >
   <xsl:output method="xml" indent="yes" />    
   <xsl:param name="edu.ucla.mbi.services.detail" select="stub"/>

   <xsl:template match="/">      
    <ns1:dataset>	  
	    <xsl:apply-templates />
    </ns1:dataset>

  </xsl:template>

  <xsl:template match="NLMCatalogRecordSet/NLMCatalogRecord">
    <xsl:element name="ns1:node">
	    <xsl:attribute name="ac">
            <xsl:value-of select="./NlmUniqueID/text()"/>
        </xsl:attribute>
        <xsl:attribute name="ns">nlm</xsl:attribute>
        <xsl:attribute name="id">1</xsl:attribute>
        
        <xsl:element name="ns1:type">
	        <xsl:attribute name="name">data-source</xsl:attribute>
            <xsl:attribute name="ac">dxf:0016</xsl:attribute>
	        <xsl:attribute name="ns">dxf</xsl:attribute>
        </xsl:element>

        <xsl:element name="ns1:label">
          <xsl:variable name="medline_title" select="./MedlineTA/text()"/>
          <xsl:choose>
            <xsl:when test="$medline_title!=''">                
              <xsl:value-of select="$medline_title"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:variable name="abbr_title" select="./TitleOther[@TitleType='OtherTA' and @Owner='NCBI']/TitleAlternate/text()"/>
              <xsl:choose>
                <xsl:when test="$abbr_title!=''">
                    <xsl:value-of select="$abbr_title"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="./TitleMain/Title/text()"/>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:element>

        <xsl:if test="$edu.ucla.mbi.services.detail != 'stub'"> 
            <xsl:element name="ns1:xrefList">
                <xsl:element name="ns1:xref">
                    <xsl:attribute name="type">identical-to</xsl:attribute>
                    <xsl:attribute name="typeAc">dxf:0009</xsl:attribute>
                    <xsl:attribute name="typeNs">dxf</xsl:attribute>
                    <xsl:attribute name="ac">
    
                    <xsl:variable name="issnType" select="./ISSN/@IssnType"/>
                        <xsl:choose>
                            <xsl:when test="$issnType = 'Print' ">
                                <xsl:value-of select="./ISSN/text()"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="./ISSN[@IssnType='Electronic']/text()"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                
                    <xsl:attribute name="ns">issn</xsl:attribute>
                </xsl:element>
            </xsl:element>
          

            <xsl:element name="ns1:attrList">
                <xsl:element name="ns1:attr">
                    <xsl:attribute name="name">title</xsl:attribute>
	                <xsl:attribute name="ac">dip:0004</xsl:attribute>
	                <xsl:attribute name="ns">dip</xsl:attribute>

                    <xsl:element name="ns1:value">
		                <xsl:value-of select="./TitleMain/Title/text()"/>
                    </xsl:element>
                </xsl:element>
            </xsl:element>
        </xsl:if> 

    </xsl:element>
  </xsl:template>

</xsl:stylesheet>
