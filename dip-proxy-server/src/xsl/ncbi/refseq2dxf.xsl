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
    <xsl:variable name="node_ac" select="INSDSeq/INSDSeq_locus/text()"/>
    <xsl:element name="ns1:node">
      <xsl:attribute name="ac"><xsl:value-of select="$node_ac"/></xsl:attribute>
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

      <!-- addition on 01/28/14 -->
      <xsl:element name="ns1:name">
        <xsl:value-of select="INSDSeq/INSDSeq_definition/text()"/>  
      </xsl:element>

      <!-- delete on 01/28/14 -->
      <!--
      <xsl:variable name="nodename" select="INSDSeq/INSDSeq_feature-table/INSDFeature[INSDFeature_key='Protein']/INSDFeature_quals/INSDQualifier[INSDQualifier_name='note']/INSDQualifier_value/text()"/>
      <xsl:if test="$nodename != ''">
        <xsl:element name="ns1:name">
	        <xsl:value-of select="$nodename"/>  
        </xsl:element>
      </xsl:if>
      -->  
      <xsl:if test="$edu.ucla.mbi.services.detail != 'stub'">   
         
        <xsl:element name="ns1:xrefList">
            <xsl:variable name="cds" select="INSDSeq/INSDSeq_feature-table/INSDFeature[INSDFeature_key='CDS']"/> 
            <xsl:if test="$cds != ''">
                <xsl:for-each select="$cds/INSDFeature_quals/INSDQualifier">
	                <xsl:variable name="flag" select="INSDQualifier_value/text()"/>
	                <xsl:if test="starts-with($flag, 'GeneID:')">
                        <xsl:variable name="geneid" select="substring-after($flag,'GeneID:')"/>
                        <xsl:element name="ns1:xref">
                            <xsl:attribute name="type">encoded-by</xsl:attribute>
                            <xsl:attribute name="typeAc">dxf:0022</xsl:attribute>
                            <xsl:attribute name="typeNs">dxf</xsl:attribute>
	                        <xsl:attribute name="ac"><xsl:value-of select="$geneid"/></xsl:attribute>
                            <xsl:attribute name="ns">entrezgene</xsl:attribute>
                        </xsl:element>
                    </xsl:if>
                </xsl:for-each>
		    </xsl:if>
					 
            <xsl:variable name="taxon" select="INSDSeq/INSDSeq_feature-table/INSDFeature[INSDFeature_key='source']/INSDFeature_quals/INSDQualifier[INSDQualifier_name='db_xref']/INSDQualifier_value/text()"/>
            <xsl:variable name="taxonAc" select="substring-after($taxon,'taxon:')"/>
            <xsl:if test="$taxonAc != '' "> 
	            <xsl:element name="ns1:xref">
	                <xsl:attribute name="type">produced-by</xsl:attribute>
	                <xsl:attribute name="typeAc">dxf:0007</xsl:attribute>
	                <xsl:attribute name="typeNs">dxf</xsl:attribute>
	                <xsl:attribute name="ac"><xsl:value-of select="$taxonAc"/></xsl:attribute>
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
            </xsl:if>
        
            <xsl:variable name="contig" select="INSDSeq/INSDSeq_contig/text()"/>
            <xsl:if test="$contig != '' ">
              <xsl:choose>
              <xsl:when test="contains($contig, ':')" >
                <xsl:variable name="refseqAc" select="substring-before($contig, ':')"/>
                <xsl:choose>
                  <xsl:when test="contains($refseqAc, '_')">
                    <xsl:variable name="prefix" select="substring-before($refseqAc, '_')"/>
                    <xsl:variable name="suffix" select="substring-after($refseqAc, '_')"/>
                    <xsl:variable name="prefixLen" select="string-length($prefix)"/>
                    <xsl:variable name="prefixLen" select="number($prefixLen - 1 )"/>
                    <xsl:variable name="prefix" select="substring($prefix, $prefixLen, 2)"/>
                
                    <xsl:variable name="refseqAc" select="concat( $prefix, '_', $suffix )"/>
                    <xsl:variable name="refseqAc" select="substring-before($refseqAc, '.')"/>
                    <xsl:call-template name="instance-of">
                       <xsl:with-param name="ac"><xsl:value-of select="$refseqAc"/></xsl:with-param>
                       <xsl:with-param name="contig"><xsl:value-of select="$contig"/></xsl:with-param>
                    </xsl:call-template>
                  </xsl:when>
                  <xsl:otherwise>
                    <xsl:call-template name="instance-of">
                        <xsl:with-param name="ac"><xsl:value-of select="$node_ac"/></xsl:with-param>
                        <xsl:with-param name="contig"><xsl:value-of select="$contig"/></xsl:with-param>
                    </xsl:call-template>
                  </xsl:otherwise>
                </xsl:choose>
              </xsl:when>
              <xsl:otherwise>
                <xsl:call-template name="instance-of">
                    <xsl:with-param name="ac"><xsl:value-of select="$node_ac"/></xsl:with-param>
                    <xsl:with-param name="contig"><xsl:value-of select="$contig"/></xsl:with-param>
                </xsl:call-template>
              </xsl:otherwise>
              </xsl:choose>
            </xsl:if>
        </xsl:element>
      </xsl:if>

      <xsl:element name="ns1:attrList">
        <!-- addition on 01/28/14 -->
        <xsl:for-each select="INSDSeq/INSDSeq_feature-table/INSDFeature[INSDFeature_key='CDS']/INSDFeature_quals/INSDQualifier">
            <xsl:choose>
                <xsl:when test="INSDQualifier_name='gene'">
                    <xsl:element name="ns1:attr">
                        <xsl:attribute name="name">gene-name-primary</xsl:attribute>
                        <xsl:attribute name="ac">dip:0055</xsl:attribute>
                        <xsl:call-template name="attr-value"/>
                    </xsl:element>
                </xsl:when>
                    
                <xsl:when test="INSDQualifier_name='gene_synonym'">
                    <xsl:variable name="gene_synonym_list" select="INSDQualifier_value"/>
                    <xsl:choose>
                        <xsl:when test="not(contains($gene_synonym_list, ';'))">
                            <xsl:element name="ns1:attr">
                                <xsl:attribute name="name">gene-name-synonym</xsl:attribute>
                                <xsl:attribute name="ac">dip:0056</xsl:attribute>
                                <xsl:attribute name="ns">dip</xsl:attribute>
                                <xsl:element name="value">
                                    <xsl:value-of select="normalize-space($gene_synonym_list)" />
                                </xsl:element>  
                            </xsl:element>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:call-template name="parseString">
                                <xsl:with-param name="list" select="$gene_synonym_list"/>
                            </xsl:call-template>         
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>
                  
                <xsl:when test="INSDQualifier_name='locus_tag'">
                    <xsl:element name="ns1:attr">
                        <xsl:attribute name="name">gene-ordered-locus</xsl:attribute>
                        <xsl:attribute name="ac">dip:0057</xsl:attribute>
                        <xsl:call-template name="attr-value"/>
                    </xsl:element>
                </xsl:when>
            </xsl:choose>
        </xsl:for-each>

        <xsl:if test="$edu.ucla.mbi.services.detail = 'full'">
            <xsl:variable name="lcletters">abcdefghijklmnopqrstuvwxyz</xsl:variable>
            <xsl:variable name="ucletters">ABCDEFGHIJKLMNOPQRSTUVWXYZ</xsl:variable>

            <xsl:variable name="seq" select="INSDSeq/INSDSeq_sequence/text()"/>
            <xsl:variable name="mat_pep" select="INSDSeq/INSDSeq_feature-table/INSDFeature[INSDFeature_key='mat_peptide']/INSDFeature_quals/INSDQualifier[INSDQualifier_name='peptide']/INSDQualifier_value/text()"/>

            <xsl:if test="$seq != '' or $mat_pep != ''">
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
            </xsl:if>
        </xsl:if>
      </xsl:element>
    </xsl:element> 
  </xsl:template>

  <!-- addition on 01/28/14 -->
  <xsl:template name="attr-value">
    <xsl:attribute name="ns">dip</xsl:attribute>
    <xsl:element name="value">
        <xsl:value-of select="INSDQualifier_value" />
    </xsl:element>
  </xsl:template>

  <!-- addition on 01/28/14 -->
  <xsl:template name="parseString">
    <xsl:param name="list"/>
    <xsl:choose>
      <xsl:when test="contains($list, ';')">
        <xsl:element name="ns1:attr">
            <xsl:attribute name="name">gene-name-synonym</xsl:attribute>
            <xsl:attribute name="ac">dip:0056</xsl:attribute>
            <xsl:attribute name="ns">dip</xsl:attribute>
            <xsl:element name="value">
                <xsl:value-of select="normalize-space(substring-before($list, ';'))" />
            </xsl:element>
        </xsl:element>
        <xsl:call-template name="parseString">
            <xsl:with-param name="list" select="substring-after($list,';')"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:element name="ns1:attr">
            <xsl:attribute name="name">gene-name-synonym</xsl:attribute>
            <xsl:attribute name="ac">dip:0056</xsl:attribute>
            <xsl:attribute name="ns">dip</xsl:attribute>
            <xsl:element name="value">
                <xsl:value-of select="normalize-space($list)" />
            </xsl:element>
        </xsl:element>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="instance-of">
    <xsl:param name="ac"/>
    <xsl:param name="contig"/>

    <xsl:element name="ns1:xref">
        <xsl:attribute name="type">instance-of</xsl:attribute>
        <xsl:attribute name="typeAc">dxf:0006</xsl:attribute>
        <xsl:attribute name="typeNs">dxf</xsl:attribute>
        <xsl:attribute name="ac"><xsl:value-of select="$ac"/></xsl:attribute>
        <xsl:attribute name="ns">refseq</xsl:attribute>             
        <xsl:if test="$edu.ucla.mbi.services.detail = 'full'">
            <xsl:element name="ns1:node">
                <xsl:attribute name="ac"><xsl:value-of select="$ac"/></xsl:attribute>
                <xsl:attribute name="ns">refseq</xsl:attribute>
                <xsl:attribute name="id">3</xsl:attribute>

                <xsl:element name="ns1:type">
                    <xsl:attribute name="name">reference-protein</xsl:attribute>
                    <xsl:attribute name="ac">dxf:0061</xsl:attribute>
                    <xsl:attribute name="ns">dxf</xsl:attribute>
                </xsl:element>

                <xsl:element name="ns1:label">
                    <xsl:value-of select="$ac"/>
                </xsl:element>
                
                <xsl:element name="ns1:attrList">
                    <xsl:element name="ns1:attr">
                        <xsl:attribute name="name">location</xsl:attribute>
                        <xsl:attribute name="ac">dxf:0062</xsl:attribute>
                        <xsl:attribute name="ns">dxf</xsl:attribute>
                        <xsl:element name="ns1:value">
                            <xsl:value-of select="$contig" />
                        </xsl:element>
                    </xsl:element>
                </xsl:element>
            </xsl:element>
        </xsl:if>
    </xsl:element>    
  </xsl:template>
</xsl:stylesheet>
