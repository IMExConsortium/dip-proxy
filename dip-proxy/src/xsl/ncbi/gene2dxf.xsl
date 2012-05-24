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

  <xsl:template match="Entrezgene-Set">
    <xsl:element name="ns1:node">
      
      <xsl:attribute name="ac"><xsl:value-of select="Entrezgene/Entrezgene_track-info/Gene-track/Gene-track_geneid/text()"/></xsl:attribute>
      <xsl:attribute name="ns">entrezgene</xsl:attribute>
      <xsl:element name="ns1:type"> 
	<xsl:attribute name="name">gene</xsl:attribute>      
        <xsl:attribute name="ac">dxf:0025</xsl:attribute>
	<xsl:attribute name="ns">dxf</xsl:attribute>
      </xsl:element>

      <xsl:element name="ns1:label">
	      <xsl:value-of select="Entrezgene//Entrezgene_gene/Gene-ref/Gene-ref_locus/text()"/>
      </xsl:element>

      <xsl:element name="ns1:name">
	      <xsl:value-of select="Entrezgene//Entrezgene_gene/Gene-ref/Gene-ref_desc/text()"/>
      </xsl:element>

      <xsl:if test="$edu.ucla.mbi.services.detail != 'stub'">
        <xsl:element name="ns1:xrefList">
	        <xsl:element name="ns1:xref">
	            <xsl:attribute name="type">produced-by</xsl:attribute>
	            <xsl:attribute name="typeAc">dxf:0007</xsl:attribute>
	            <xsl:attribute name="typeNs">dxf</xsl:attribute>
	            <xsl:attribute name="ac"><xsl:value-of select="Entrezgene//BioSource_org/Org-ref/Org-ref_db/Dbtag/Dbtag_tag/Object-id/Object-id_id/text()"/></xsl:attribute>
	            <xsl:attribute name="ns">ncbitaxid</xsl:attribute>
                
                <xsl:if test="$edu.ucla.mbi.services.detail = 'full'">
	                <xsl:element name="ns1:node">
	                    <xsl:attribute name="ac"><xsl:value-of select="Entrezgene//BioSource_org/Org-ref/Org-ref_db/Dbtag/Dbtag_tag/Object-id/Object-id_id/text()"/></xsl:attribute>
	                    <xsl:attribute name="ns">ncbitaxid</xsl:attribute>
	    
	                    <xsl:element name="ns1:type"> 
	                        <xsl:attribute name="name">organism</xsl:attribute>
	                        <xsl:attribute name="ac">dip:0301</xsl:attribute>
	                        <xsl:attribute name="ns">dip</xsl:attribute>
                        </xsl:element>
      
	                    <xsl:element name="ns1:label">
	                        <xsl:value-of select="Entrezgene//BioSource_org/Org-ref/Org-ref_taxname/text()"/>
                        </xsl:element>
                
	                </xsl:element>
                </xsl:if>
            
            </xsl:element>	  

        </xsl:element>  
      </xsl:if>
    </xsl:element> 
    
  </xsl:template>
</xsl:stylesheet>
