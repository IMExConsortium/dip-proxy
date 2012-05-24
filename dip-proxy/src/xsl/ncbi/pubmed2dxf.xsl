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

  <xsl:template match="PubmedArticleSet">
    <xsl:element name="ns1:node">
	    <xsl:attribute name="ac">
            <xsl:value-of select="./PubmedArticle/MedlineCitation/PMID"/>
        </xsl:attribute>
        <xsl:attribute name="ns">pubmed</xsl:attribute>
        <xsl:attribute name="id">1</xsl:attribute>
        
        <xsl:element name="ns1:type">
	        <xsl:attribute name="name">data-source</xsl:attribute>
            <xsl:attribute name="ac">dxf:0016</xsl:attribute>
	        <xsl:attribute name="ns">dxf</xsl:attribute>
        </xsl:element>

        <xsl:element name="ns1:label">
	        <xsl:value-of select="./PubmedArticle/MedlineCitation/PMID/text()"/>
        </xsl:element>

        <xsl:if test="$edu.ucla.mbi.services.detail != 'stub'">           
            <xsl:element name="ns1:xrefList">
                <xsl:element name="ns1:xref">
                    <xsl:attribute name="type">published-by</xsl:attribute>
                    <xsl:attribute name="typeAc">dxf:0040</xsl:attribute>
                    <xsl:attribute name="typeNs">dxf</xsl:attribute>
                    <xsl:attribute name="ac">
                        <xsl:value-of select="./PubmedArticle/MedlineCitation/MedlineJournalInfo/NlmUniqueID/text()"/>
                    </xsl:attribute>
                    <xsl:attribute name="ns">nlmid</xsl:attribute>
                </xsl:element>

                <xsl:element name="ns1:xref">
                    <xsl:attribute name="type">published-by</xsl:attribute>
                    <xsl:attribute name="typeAc">dxf:0040</xsl:attribute>
                    <xsl:attribute name="typeNs">dxf</xsl:attribute>
                    <xsl:attribute name="ac">
    
                    <xsl:variable name="issnType" select="./PubmedArticle/MedlineCitation//ISSN/@IssnType"/>
                        <xsl:choose>
                            <xsl:when test="$issnType = 'Print' ">
                                <xsl:value-of select="./PubmedArticle/MedlineCitation//ISSN/text()"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="./PubmedArticle/MedlineCitation//ISSN[@IssnType='Electronic']/text()"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                
                    <xsl:attribute name="ns">issn</xsl:attribute>
                </xsl:element>

                <xsl:apply-templates select="./PubmedArticle/PubmedData/ArticleIdList"/>
            </xsl:element>
      

            <xsl:element name="ns1:attrList">
                <xsl:element name="ns1:attr">
                    <xsl:attribute name="name">title</xsl:attribute>
	                <xsl:attribute name="ac">dip:0004</xsl:attribute>
	                <xsl:attribute name="ns">dip</xsl:attribute>

                    <xsl:element name="ns1:value">
		                <xsl:value-of select="./PubmedArticle/MedlineCitation//ArticleTitle/text()"/>
                    </xsl:element>
                </xsl:element>

	            <xsl:element name="ns1:attr">
	                <xsl:attribute name="name">authors</xsl:attribute>
	                <xsl:attribute name="ac">dip:0010</xsl:attribute>
	                <xsl:attribute name="ns">dip</xsl:attribute>

	                <xsl:element name="ns1:value">		  
		                <xsl:apply-templates select="./PubmedArticle/MedlineCitation//AuthorList"/>
                    </xsl:element>
                </xsl:element>
	
	            <xsl:element name="ns1:attr">
	                <xsl:attribute name="name">journal-title</xsl:attribute>
	                <xsl:attribute name="ac">dip:0009</xsl:attribute>
	                <xsl:attribute name="ns">dip</xsl:attribute>

                    <xsl:element name="ns1:value">
		                <xsl:value-of select="./PubmedArticle/MedlineCitation//Article/Journal/Title/text()"/>
                    </xsl:element>
                </xsl:element>
	
	            <xsl:element name="ns1:attr">
	                <xsl:attribute name="name">volume</xsl:attribute>
	                <xsl:attribute name="ac">dip:0011</xsl:attribute>
	                <xsl:attribute name="ns">dip</xsl:attribute>

                    <xsl:element name="ns1:value">
		                <xsl:value-of select="./PubmedArticle/MedlineCitation//JournalIssue/Volume/text()"/>
                    </xsl:element>
                </xsl:element>

       	        <xsl:element name="ns1:attr">
	                <xsl:attribute name="name">issue</xsl:attribute>
	                <xsl:attribute name="ac">dip:0012</xsl:attribute>
	                <xsl:attribute name="ns">dip</xsl:attribute>

                    <xsl:element name="ns1:value">
		                <xsl:value-of select="./PubmedArticle/MedlineCitation//JournalIssue/Issue/text()"/>
                    </xsl:element>
                </xsl:element>

        	    <xsl:element name="ns1:attr">
	                <xsl:attribute name="name">pages</xsl:attribute>
	                <xsl:attribute name="ac">dip:0015</xsl:attribute>
	                <xsl:attribute name="ns">dip</xsl:attribute>

                    <xsl:element name="ns1:value">
		                <xsl:value-of select="./PubmedArticle/MedlineCitation//Pagination/MedlinePgn/text()"/>
                    </xsl:element>
                </xsl:element>

 	            <xsl:element name="ns1:attr">
	                <xsl:attribute name="name">year</xsl:attribute>
	                <xsl:attribute name="ac">dip:0013</xsl:attribute>
	                <xsl:attribute name="ns">dip</xsl:attribute>

                    <xsl:element name="ns1:value">
	                    <xsl:choose>
		                    <xsl:when test="./PubmedArticle/MedlineCitation//JournalIssue/PubDate/Year/text() != '' ">	    
			                    <xsl:value-of select="./PubmedArticle/MedlineCitation//JournalIssue/PubDate/Year/text()"/>
	                        </xsl:when>
	                        <xsl:otherwise>
		                        <xsl:variable name="year" select="./PubmedArticle/MedlineCitation//JournalIssue/PubDate/MedlineDate/text()"/>
		                        <xsl:value-of select="substring($year,1,4)"/> 
	                        </xsl:otherwise>
                        </xsl:choose>
                    </xsl:element>
                </xsl:element>

	            <xsl:if test="$edu.ucla.mbi.services.detail = 'full'">
                    <xsl:variable name="month" select="./PubmedArticle/MedlineCitation/Article//PubDate/Month/text()"/>
                    <xsl:if test="$month != ''">

                        <xsl:element name="ns1:attr">
                            <xsl:attribute name="name">publication-date</xsl:attribute>
                            <xsl:attribute name="ac">dxf:0043</xsl:attribute>
                            <xsl:attribute name="ns">dxf</xsl:attribute>

                    
                            <xsl:element name="ns1:value">
                                <xsl:call-template name="date-format">
                                    <xsl:with-param name="year" select="./PubmedArticle/MedlineCitation/Article//PubDate/Year/text()"/>
                                    <xsl:with-param name="month" select="./PubmedArticle/MedlineCitation/Article//PubDate/Month/text()"/>
                                    <xsl:with-param name="day" select="./PubmedArticle/MedlineCitation/Article//PubDate/Day/text()"/>
                                </xsl:call-template>
                            </xsl:element>
                        </xsl:element>
                    </xsl:if>

                    <xsl:element name="ns1:attr">
                        <xsl:attribute name="name">abstract</xsl:attribute>
	                    <xsl:attribute name="ac">dip:0014</xsl:attribute>
	                    <xsl:attribute name="ns">dip</xsl:attribute>

                        <xsl:element name="ns1:value">
		                    <xsl:value-of select="./PubmedArticle/MedlineCitation//Abstract/AbstractText/text()"/>
                        </xsl:element>
                    </xsl:element>
                </xsl:if>
       
            </xsl:element>
        </xsl:if>

    </xsl:element>
  </xsl:template>

  <xsl:template match="AuthorList">
    <xsl:for-each select="Author">
        <xsl:variable name="collectiveName" select="CollectiveName/text()"/>
        <xsl:choose>
            <xsl:when test="not($collectiveName)">
                <xsl:value-of select="LastName"/>
                <xsl:text> </xsl:text>
                <xsl:value-of select="Initials"/>
                <xsl:if test="position() &lt; last()"><xsl:text>, </xsl:text>
                </xsl:if>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$collectiveName"/>
                <xsl:if test="position() &lt; last()"><xsl:text>, </xsl:text>
                </xsl:if>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="ArticleIdList">
    <xsl:for-each select="ArticleId">
        <xsl:element name="ns1:xref">
            <xsl:attribute name="type">identical-to</xsl:attribute>
            <xsl:attribute name="typeAc">dxf:0009</xsl:attribute>
            <xsl:attribute name="typeNs">dxf</xsl:attribute>
            <xsl:attribute name="ac">
                <xsl:value-of select="text()"/>
            </xsl:attribute>
            <xsl:attribute name="ns">
                <xsl:value-of select="@IdType"/>
            </xsl:attribute>
        </xsl:element>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="date-format">
    <xsl:param name="year"/>
    <xsl:param name="month"/>
    <xsl:param name="day"/>

    <xsl:choose>
        <xsl:when test="$month='Jan'">
            <xsl:choose>
                <xsl:when test="not($day)">
                    <xsl:value-of select="concat($year,'-01-01')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="concat($year,'-01-',$day)"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:when>
        <xsl:when test="$month='Feb'">
            <xsl:choose>
                <xsl:when test="not($day)">
                    <xsl:value-of select="concat($year,'-02-01')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="concat($year,'-02-',$day)"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:when>
        <xsl:when test="$month='Mar'">
            <xsl:choose>
                <xsl:when test="not($day)">
                    <xsl:value-of select="concat($year,'-03-01')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="concat($year,'-03-',$day)"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:when>
        <xsl:when test="$month='Apr'">
            <xsl:choose>
                <xsl:when test="not($day)">
                    <xsl:value-of select="concat($year,'-04-01')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="concat($year,'-04-',$day)"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:when>
        <xsl:when test="$month='May'">
            <xsl:choose>
                <xsl:when test="not($day)">
                    <xsl:value-of select="concat($year,'-05-01')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="concat($year,'-05-',$day)"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:when>
        <xsl:when test="$month='Jun'">
            <xsl:choose>
                <xsl:when test="not($day)">
                    <xsl:value-of select="concat($year,'-06-01')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="concat($year,'-06-',$day)"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:when>
        <xsl:when test="$month='Jul'">
            <xsl:choose>
                <xsl:when test="not($day)">
                    <xsl:value-of select="concat($year,'-07-01')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="concat($year,'-07-',$day)"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:when>
        <xsl:when test="$month='Aug'">
            <xsl:choose>
                <xsl:when test="not($day)">
                    <xsl:value-of select="concat($year,'-08-01')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="concat($year,'-08-',$day)"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:when>
        <xsl:when test="$month='Sep'">
            <xsl:choose>
                <xsl:when test="not($day)">
                    <xsl:value-of select="concat($year,'-09-01')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="concat($year,'-09-',$day)"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:when>
        <xsl:when test="$month='Oct'">
            <xsl:choose>
                <xsl:when test="not($day)">
                    <xsl:value-of select="concat($year,'-10-01')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="concat($year,'-10-',$day)"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:when>
        <xsl:when test="$month='Nov'">
            <xsl:choose>
                <xsl:when test="not($day)">
                    <xsl:value-of select="concat($year,'-11-01')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="concat($year,'-11-',$day)"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:when>
        <xsl:when test="$month='Dec'">
            <xsl:choose>
                <xsl:when test="not($day)">
                    <xsl:value-of select="concat($year,'-12-01')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="concat($year,'-12-',$day)"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:when>
        <xsl:otherwise>
            <xsl:value-of select="concat($year,'-',$month,'-',$day)"/>
        </xsl:otherwise>
    </xsl:choose>
        
  </xsl:template>
</xsl:stylesheet>
