<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0"
    xmlns:ns1="http://dip.doe-mbi.ucla.edu/services/dxf14"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    >
 
    <xsl:output method="xml" indent="yes" />
	
    <xsl:template match="/">
        <ns1:dataset>
	    	<xsl:apply-templates />
	    </ns1:dataset>
 	</xsl:template>

	<xsl:template match="TaxaSet">
		<xsl:element name="ns1:node">
      		<xsl:attribute name="ac"><xsl:value-of select="Taxon/TaxId/text()"/></xsl:attribute>
      		<xsl:attribute name="ns">ncbitaxid</xsl:attribute>
	
			<xsl:element name="ns1:type"> 
				<xsl:attribute name="name">organism</xsl:attribute>      
		        	<xsl:attribute name="ac">dip:0301</xsl:attribute>
				<xsl:attribute name="ns">dip</xsl:attribute>
	       	</xsl:element>

	 		<xsl:element name="ns1:label">
           		<xsl:variable name="label" select="Taxon/ScientificName/text()"/>
                <xsl:choose>
		    		<xsl:when test="$label != ''">
		       			<xsl:value-of select="Taxon/ScientificName/text()"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="Taxon/OtherNames/CommonName[position()=1]/text()"/>
					</xsl:otherwise>
				</xsl:choose>
	    	</xsl:element>

	    	<xsl:element name="ns1:name">
		  		<xsl:value-of select="Taxon/OtherNames/GenbankCommonName/text()"/>
    		</xsl:element>
		</xsl:element> 
  	</xsl:template>
</xsl:stylesheet>
