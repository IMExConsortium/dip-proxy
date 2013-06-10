<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0"
	xmlns:ebi="http://uniprot.org/uniprot"
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

  <xsl:template match="ebi:uniprot">
    <xsl:element name="ns1:node">
      <xsl:attribute name="ac"><xsl:value-of select="ebi:entry/ebi:accession[1]"/></xsl:attribute>
      <xsl:attribute name="ns">uniprot</xsl:attribute>
      
      <xsl:element name="ns1:type"> 
	    <xsl:attribute name="name">protein</xsl:attribute>	
	    <xsl:attribute name="ac">dxf:0003</xsl:attribute>
	    <xsl:attribute name="ns">dxf</xsl:attribute>
      </xsl:element>

      <xsl:element name="ns1:label">
	      <xsl:value-of select="ebi:entry/ebi:accession[1]"/>
      </xsl:element>

      <xsl:element name="ns1:name">
	      <xsl:value-of select="ebi:entry/ebi:protein/ebi:name"/>
      </xsl:element>

      <xsl:if test="$edu.ucla.mbi.services.detail != 'stub'">
        <xsl:element name="ns1:xrefList">

            <xsl:element name="ns1:xref">
	            <xsl:attribute name="type">produced-by</xsl:attribute>
	            <xsl:attribute name="typeAc">dxf:0007</xsl:attribute>
	            <xsl:attribute name="typeNs">dxf</xsl:attribute>
	  
	            <xsl:attribute name="ac"><xsl:value-of select="ebi:entry/ebi:organism/ebi:dbReference/@id"/></xsl:attribute>
	            <xsl:attribute name="ns">ncbitaxid</xsl:attribute>

                <xsl:if test="$edu.ucla.mbi.services.detail = 'full'">
                    <xsl:element name="ns1:node">
                        <xsl:attribute name="ac">
                            <xsl:value-of select="ebi:entry/ebi:organism/ebi:dbReference/@id"/>
                        </xsl:attribute>
                        <xsl:attribute name="ns">ncbitaxid</xsl:attribute>
      
                        <xsl:element name="ns1:type"> 
	                        <xsl:attribute name="name">organism</xsl:attribute>	
	                        <xsl:attribute name="ac">dip:0301</xsl:attribute>
	                        <xsl:attribute name="ns">dip</xsl:attribute>
                        </xsl:element>

                        <xsl:element name="ns1:label">
		                    <xsl:value-of select="ebi:entry/ebi:organism/ebi:name[@type='scientific']/text()"/>
                        </xsl:element>

                        <xsl:element name="ns1:name">
		                    <xsl:value-of select="ebi:entry/ebi:organism/ebi:name[@type='common']/text()"/>
                        </xsl:element>
	                </xsl:element>
                </xsl:if>
            </xsl:element>	  

            <xsl:if test="$edu.ucla.mbi.services.detail = 'full'">
                <xsl:for-each select="ebi:entry/ebi:dbReference">
                    <xsl:element name="ns1:xref">
                        <xsl:attribute name="type">annotated-with</xsl:attribute>
                        <xsl:attribute name="typeAc">dxf:0054</xsl:attribute>
                        <xsl:attribute name="typeNs">dxf</xsl:attribute>

                        <xsl:attribute name="ac"><xsl:value-of select="@id"/></xsl:attribute>
                        <xsl:attribute name="ns"><xsl:value-of select="@type"/></xsl:attribute>
                    </xsl:element>
                </xsl:for-each>
            </xsl:if>

        </xsl:element>

        <xsl:if test="$edu.ucla.mbi.services.detail = 'full'">
            <xsl:element name="ns1:attrList">
                <xsl:element name="ns1:attr">	      
	                <xsl:attribute name="ns">dip</xsl:attribute>
	                <xsl:attribute name="ac">dip:0008</xsl:attribute>
	                <xsl:attribute name="name">sequence</xsl:attribute>
	  
	                <xsl:element name="ns1:value">
                        <xsl:variable name="lcletters">abcdefghijklmnopqrstuvwxyz</xsl:variable>
                        <xsl:variable name="ucletters">ABCDEFGHIJKLMNOPQRSTUVWXYZ</xsl:variable>
                        <xsl:variable name="seq" select="ebi:entry/ebi:sequence/text()"/>
	                    <xsl:value-of select="translate($seq,$lcletters,$ucletters)" />
	    
                    </xsl:element> 
                </xsl:element>
            </xsl:element> 
        </xsl:if>

      </xsl:if>
    </xsl:element> 
  </xsl:template>
</xsl:stylesheet>
