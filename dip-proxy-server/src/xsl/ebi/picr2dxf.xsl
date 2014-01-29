<?xml version="1.0" encoding="UTF-8"?>

<!-- ========================================================================
 ! $HeadURL::                                                               $
 ! $Id::                                                                    $
 ! Version: $Rev::                                                          $
 !===========================================================================
 !
 !  picr2dxf: picr to dxf transformation
 !
 !======================================================================= -->

<xsl:stylesheet version="1.0"
	xmlns:ns1="http://dip.doe-mbi.ucla.edu/services/dxf14"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	>
  
    <xsl:output method="xml" indent="yes" />

    <xsl:param name="edu.ucla.mbi.services.ns"/> 
    <xsl:param name="edu.ucla.mbi.services.ac"/>
    <xsl:param name="edu.ucla.mbi.services.detail" select="base"/>
    
    <xsl:variable name="lcletters">abcdefghijklmnopqrstuvwxyz</xsl:variable>
    <xsl:variable name="ucletters">ABCDEFGHIJKLMNOPQRSTUVWXYZ</xsl:variable>

    <xsl:key name="unique_ac_list" 
             match="//identicalCrossReferences[../identicalCrossReferences[accession/text() = $edu.ucla.mbi.services.ac 
                    or starts-with( accession/text(), concat( $edu.ucla.mbi.services.ac, '-' ) )]]" 
             use="accession"/> 

    <xsl:template match="/">
        <ns1:dataset>
	        <xsl:apply-templates />
        </ns1:dataset>
    </xsl:template>
    
    <xsl:template match="getUPIForAccessionResponse">
        <xsl:element name="ns1:node">
            <xsl:attribute name="ac">
                <xsl:value-of select="$edu.ucla.mbi.services.ac"/>
            </xsl:attribute>
            <xsl:attribute name="id">1</xsl:attribute>
            <xsl:attribute name="ns">
                <xsl:value-of select="$edu.ucla.mbi.services.ns"/>
            </xsl:attribute>

            <xsl:element name="ns1:type">
                <xsl:attribute name="name">protein</xsl:attribute>
                <xsl:attribute name="ac">dxf:0003</xsl:attribute>
                <xsl:attribute name="ns">dxf</xsl:attribute>
            </xsl:element>

            <xsl:element name="ns1:label">
                <xsl:value-of select="$edu.ucla.mbi.services.ac"/>
            </xsl:element>
         
            <xsl:if test="$edu.ucla.mbi.services.detail = 'base' 
                            or $edu.ucla.mbi.services.detail = 'full'">
                <xsl:element name="ns1:xrefList">
                    <xsl:for-each select="getUPIForAccessionReturn">
                        <!-- comment: ac is the same as the queried one or ac is the isoform of the queried one-->
                        <xsl:if test="identicalCrossReferences[ accession/text() = $edu.ucla.mbi.services.ac ] 
                                     or identicalCrossReferences[ starts-with( accession/text(), concat( $edu.ucla.mbi.services.ac, '-' ) ) ]">
                            <xsl:call-template name="accessionReturn"/>
                        </xsl:if>       
                    </xsl:for-each>
                </xsl:element>
            </xsl:if>
        </xsl:element>
    </xsl:template>

    <xsl:template name="accessionReturn">
        <xsl:variable name="seq" select="sequence"/>
        <xsl:for-each select="identicalCrossReferences">
            <xsl:variable name="ac" select="accession/text()"/>
	        <xsl:variable name="flag" select="deleted/text()"/> 
            <xsl:variable name="taxonid" select="taxonId"/> 
            <xsl:variable name="dbdescription" select="databaseDescription/text()"/>
            <xsl:variable name="dbname" select="databaseName/text()"/>
                                
            <xsl:if test="$edu.ucla.mbi.services.detail ='base'">
                <xsl:if test="generate-id(.) = generate-id(key('unique_ac_list', $ac )[1]) "> 
                    <xsl:call-template name="xref">
                        <xsl:with-param name="type">related-to</xsl:with-param>
                        <xsl:with-param name="ac">
                            <xsl:value-of select="$ac"/>
                        </xsl:with-param>
                        <xsl:with-param name="dbname">
                            <xsl:value-of select="$dbname"/>
                        </xsl:with-param>
                        <xsl:with-param name="dbdescription">
                            <xsl:value-of select="$dbdescription"/>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:if>
            </xsl:if>
                            
            <xsl:if test="$edu.ucla.mbi.services.detail='full'">
              <xsl:if test="$taxonid[text()] 
                                or ( $taxonid[not(text())] and deleted/text()='false' )">
                <xsl:call-template name="detailxref">
                    <xsl:with-param name="type">related-to</xsl:with-param>
                    <xsl:with-param name="ac"><xsl:value-of select="$ac"/></xsl:with-param>
                    <xsl:with-param name="dbname"><xsl:value-of select="$dbname"/></xsl:with-param>
                    <xsl:with-param name="dbdescription"><xsl:value-of select="$dbdescription"/></xsl:with-param>
                    <xsl:with-param name="seq"><xsl:value-of select="$seq"/></xsl:with-param>
                    <xsl:with-param name="taxonid">
                        <xsl:choose>
                            <xsl:when test="$taxonid[not(text())]">
                                <xsl:value-of select="-3"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="$taxonid"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:with-param>
                </xsl:call-template>
              </xsl:if>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>
    
    <xsl:template name="xref">
        <xsl:param name="type"/>
        <xsl:param name="ac"/>
        <xsl:param name="dbname"/>
        <xsl:param name="dbdescription"/>
 
        <xsl:element name="ns1:xref">
            <xsl:attribute name="type"><xsl:value-of select="$type"/></xsl:attribute>
            <xsl:attribute name="typeAc">dxf:0018</xsl:attribute>
            <xsl:attribute name="typeNs">dxf</xsl:attribute>
            <xsl:attribute name="ac"><xsl:value-of select="$ac"/></xsl:attribute> 
            <xsl:attribute name="ns">
                <xsl:choose>
                    <xsl:when test="$dbdescription='UniProtKB/Swiss-Prot' 
                                        or $dbdescription='UniProtKB/TrEMBL' 
                                        or $dbname='SWISSPROT_VARSPLIC' 
                                        or $dbname='TREMBL_VARSPLIC'">

                        <xsl:text>uniprot</xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="translate($dbname,$ucletters,$lcletters)"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
        </xsl:element>

    </xsl:template>

    <xsl:template name="detailxref">
        <xsl:param name="type"/>
        <xsl:param name="ac"/>
        <xsl:param name="dbname"/>
        <xsl:param name="dbdescription"/>
        <xsl:param name="taxonid"/>
        <xsl:param name="seq"/>

        <xsl:element name="ns1:xref">
            <xsl:attribute name="type"><xsl:value-of select="$type"/></xsl:attribute>
            <xsl:attribute name="typeAc">dxf:0018</xsl:attribute>
            <xsl:attribute name="typeNs">dxf</xsl:attribute>
            <xsl:attribute name="ac"><xsl:value-of select="$ac"/></xsl:attribute>
            <xsl:attribute name="ns">
                <xsl:choose>
                    <xsl:when test="$dbdescription='UniProtKB/Swiss-Prot' 
                                        or $dbdescription='UniProtKB/TrEMBL' 
                                        or $dbname='SWISSPROT_VARSPLIC' 
                                        or $dbname='TREMBL_VARSPLIC'">

                        <xsl:text>uniprot</xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="translate($dbname,$ucletters,$lcletters)"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>

            <xsl:element name="ns1:node">
                <xsl:attribute name="ac"><xsl:value-of select="$ac"/></xsl:attribute>
                <xsl:attribute name="id">1</xsl:attribute>
                <xsl:attribute name="ns">
                    <xsl:choose>
                        <xsl:when test="$dbdescription='UniProtKB/Swiss-Prot' 
                                            or $dbdescription='UniProtKB/TrEMBL' 
                                            or $dbname='SWISSPROT_VARSPLIC' 
                                            or $dbname='TREMBL_VARSPLIC'">

                            <xsl:text>uniprot</xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="translate($dbname,$ucletters,$lcletters)"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:attribute>

                <xsl:element name="ns1:type">
                    <xsl:choose>
                        <xsl:when test="$dbname='REFSEQ'">
                            <xsl:choose>
                                <xsl:when test="starts-with($ac, 'NM_') 
                                                    or starts-with($ac, 'XM_') 
                                                    or starts-with($ac, 'NR_') 
                                                    or starts-with($ac, 'XM_') 
                                                    or starts-with($ac, 'XR_')">

                                    <xsl:attribute name="name">rna</xsl:attribute>
                                    <xsl:attribute name="ac">dxf:0060</xsl:attribute>
                                    <xsl:attribute name="ns">dxf</xsl:attribute>
                                </xsl:when>
                                <xsl:when test="starts-with($ac, 'AP_') 
                                                    or starts-with($ac, 'NP_') 
                                                    or starts-with($ac, 'XP_') 
                                                    or starts-with($ac, 'YP_') 
                                                    or starts-with($ac, 'ZP_')">

                                    <xsl:attribute name="name">protein</xsl:attribute>
                                    <xsl:attribute name="ac">dxf:0003</xsl:attribute>
                                    <xsl:attribute name="ns">dxf</xsl:attribute>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:attribute name="name">dna</xsl:attribute>
                                    <xsl:attribute name="ac">dxf:0059</xsl:attribute>
                                    <xsl:attribute name="ns">dxf</xsl:attribute>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:attribute name="name">protein</xsl:attribute>
                            <xsl:attribute name="ac">dxf:0003</xsl:attribute>
                            <xsl:attribute name="ns">dxf</xsl:attribute>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:element>

                <xsl:element name="ns1:label">
                    <xsl:value-of select="$ac"/>
                </xsl:element>

                <xsl:element name="ns1:xrefList">
                    <xsl:element name="ns1:xref">
                        <xsl:attribute name="type">produced-by</xsl:attribute>
                        <xsl:attribute name="typeAc">dxf:0007</xsl:attribute>
                        <xsl:attribute name="typeNs">dxf</xsl:attribute>
                        <xsl:attribute name="ac"><xsl:value-of select="$taxonid"/></xsl:attribute>
                        <xsl:attribute name="ns">ncbitaxid</xsl:attribute>
                    </xsl:element>
                </xsl:element>

                <xsl:element name="ns1:attrList">
                    <xsl:element name="ns1:attr">
                        <xsl:attribute name="name">sequence</xsl:attribute>
                        <xsl:attribute name="ac">dip:0008</xsl:attribute>
                        <xsl:attribute name="ns">dip</xsl:attribute>

                        <xsl:element name="ns1:value">
                            <xsl:value-of select="translate($seq, $lcletters,$ucletters)"/>
                        </xsl:element>
                    </xsl:element>
                </xsl:element>

            </xsl:element>
        </xsl:element>
    </xsl:template>
</xsl:stylesheet>
