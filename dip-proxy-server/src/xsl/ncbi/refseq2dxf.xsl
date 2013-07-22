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

  <xsl:template match="INSDSet">
    <xsl:element name="ns1:node">
      <xsl:attribute name="ac"><xsl:value-of select="INSDSeq/INSDSeq_locus/text()"/></xsl:attribute>
      <xsl:attribute name="ns">refseq</xsl:attribute>
      <xsl:attribute name="id">1</xsl:attribute>
      
      <xsl:element name="ns1:type"> 
	    <xsl:attribute name="name">protein</xsl:attribute>      
        <xsl:attribute name="ac">dxf:0003</xsl:attribute>
	    <xsl:attribute name="ns">dxf</xsl:attribute>
      </xsl:element>

      <xsl:element name="ns1:label">
	    <xsl:value-of select="INSDSeq/INSDSeq_locus/text()"/>
      </xsl:element>

      <xsl:variable name="nodename" select="INSDSeq/INSDSeq_feature-table/INSDFeature[INSDFeature_key='Protein']/INSDFeature_quals/INSDQualifier[INSDQualifier_name='note']/INSDQualifier_value/text()"/>
      <xsl:if test="$nodename != ''">
        <xsl:element name="ns1:name">
	        <xsl:value-of select="$nodename"/>  
        </xsl:element>
      </xsl:if>

      <xsl:if test="$edu.ucla.mbi.services.detail != 'stub'">   
         
        <xsl:variable name="taxon" select="INSDSeq/INSDSeq_feature-table/INSDFeature[INSDFeature_key='source']/INSDFeature_quals/INSDQualifier[INSDQualifier_name='db_xref']/INSDQualifier_value/text()"/>
      
        <xsl:element name="ns1:xrefList">
	        <xsl:element name="ns1:xref">
	            <xsl:attribute name="type">encoded-by</xsl:attribute>
	            <xsl:attribute name="typeAc">dxf:0022</xsl:attribute>
	            <xsl:attribute name="typeNs">dxf</xsl:attribute>
	  
	            <xsl:for-each select="INSDSeq/INSDSeq_feature-table/INSDFeature[INSDFeature_key='CDS']/INSDFeature_quals/INSDQualifier">
	                <xsl:variable name="flag" select="INSDQualifier_value/text()"/>
	                <xsl:if test="starts-with($flag, 'GeneID:')">
                        <xsl:variable name="geneid" select="substring-after($flag,'GeneID:')"/>
	                    <xsl:attribute name="ac"><xsl:value-of select="$geneid"/></xsl:attribute>
                    </xsl:if>
                </xsl:for-each>
							  
	            <xsl:attribute name="ns">entrezgene</xsl:attribute>
	        </xsl:element>
	
	        <xsl:element name="ns1:xref">
	            <xsl:attribute name="type">produced-by</xsl:attribute>
	            <xsl:attribute name="typeAc">dxf:0007</xsl:attribute>
	            <xsl:attribute name="typeNs">dxf</xsl:attribute>
	            <xsl:attribute name="ac"><xsl:value-of select="substring-after($taxon,'taxon:')"/></xsl:attribute>
	            <xsl:attribute name="ns">ncbitaxid</xsl:attribute>

                <xsl:if test="$edu.ucla.mbi.services.detail = 'full'">
	                <xsl:element name="ns1:node">
	                    <xsl:attribute name="ac"><xsl:value-of select="substring-after($taxon,'taxon:')"/></xsl:attribute>
	                    <xsl:attribute name="ns">ncbitaxid</xsl:attribute>
	                    <xsl:attribute name="id">2</xsl:attribute>
          
	    
	                    <xsl:element name="ns1:type"> 
	                        <xsl:attribute name="name">organism</xsl:attribute>
	                        <xsl:attribute name="ac">dip:0301</xsl:attribute>
	                        <xsl:attribute name="ns">dip</xsl:attribute>
                        </xsl:element>
      
	                    <xsl:element name="ns1:label">
	                        <xsl:value-of select="INSDSeq/INSDSeq_feature-table/INSDFeature[INSDFeature_key='source']/INSDFeature_quals/INSDQualifier[INSDQualifier_name='organism']/INSDQualifier_value/text()"/>
                        </xsl:element>
	                </xsl:element>
                </xsl:if>
            </xsl:element>	  
        </xsl:element>
      </xsl:if>
 
      <xsl:if test="$edu.ucla.mbi.services.detail = 'full'">
        <xsl:variable name="lcletters">abcdefghijklmnopqrstuvwxyz</xsl:variable>
        <xsl:variable name="ucletters">ABCDEFGHIJKLMNOPQRSTUVWXYZ</xsl:variable>

        <xsl:variable name="seq" select="INSDSeq/INSDSeq_sequence/text()"/>
        <xsl:variable name="mat_pep" select="INSDSeq/INSDSeq_feature-table/INSDFeature[INSDFeature_key='mat_peptide']/INSDFeature_quals/INSDQualifier[INSDQualifier_name='peptide']/INSDQualifier_value/text()"/>

        <xsl:if test="$seq != '' or $mat_pep != ''">
            <xsl:element name="ns1:attrList">
                <xsl:if test="$seq != ''">
	                <xsl:element name="ns1:attr">	      
	                    <xsl:attribute name="name">sequence</xsl:attribute>
	                    <xsl:attribute name="ac">dip:0008</xsl:attribute>
	                    <xsl:attribute name="ns">dip</xsl:attribute>

	                    <xsl:element name="ns1:value">
	                        <xsl:value-of select="translate($seq,$lcletters,$ucletters)" />
	                    </xsl:element>
                    </xsl:element>
                </xsl:if>

                <xsl:if test="$mat_pep != ''">
                    <xsl:element name="ns1:attr">
                        <xsl:attribute name="name">mature-sequence</xsl:attribute>
                        <xsl:attribute name="ac">dip:0053</xsl:attribute>
                        <xsl:attribute name="ns">dip</xsl:attribute>

                        <xsl:element name="ns1:value">
                            <xsl:value-of select="translate($mat_pep,$lcletters,$ucletters)" />
                        </xsl:element>
                    </xsl:element>
                </xsl:if>
            </xsl:element> 
        </xsl:if>
      </xsl:if>
    </xsl:element> 
  </xsl:template>
</xsl:stylesheet>
