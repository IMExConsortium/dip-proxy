<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0"
    xmlns:dxf="http://dip.doe-mbi.ucla.edu/services/dxf14"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    >

  <xsl:output method="xml" indent="yes" />
  <xsl:strip-space elements="*" />

  <xsl:param name="edu.ucla.mbi.services.detail" select="stub"/>
 
  <xsl:template match="/dxf:dataset">
    <xsl:copy>
        <xsl:for-each select="/dxf:dataset/dxf:node">
            <xsl:copy>
                <xsl:copy-of select="@*"/>
                <xsl:apply-templates/>
            </xsl:copy>
        </xsl:for-each>
   </xsl:copy>
  </xsl:template>

  <xsl:template match="/dxf:dataset/dxf:node/*">
    
    <xsl:if test="$edu.ucla.mbi.services.detail='stub' and local-name()!='xrefList' and 
            local-name()!='partList' and local-name()!='attrList'" >
            <xsl:copy>
                <xsl:copy-of select="@*"/>
                <xsl:copy-of select="./text()"/> 
            </xsl:copy>
    </xsl:if>
    
    <xsl:if test="$edu.ucla.mbi.services.detail!='stub'"> 
        <xsl:call-template name="node">
            <xsl:with-param name="detailLevel">
                <xsl:value-of select="$edu.ucla.mbi.services.detail"/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:if>
  </xsl:template>

  <xsl:template name="node">
    <xsl:param name="detailLevel"/>
        <xsl:copy>
            <xsl:copy-of select="@*"/>
            <xsl:copy-of select="./text()"/>
            <xsl:choose>
                <xsl:when test="local-name()='xrefList'">
                    <xsl:for-each select="./dxf:xref">
                        <xsl:copy>
                            <xsl:copy-of select="@*"/>
                            <xsl:if test="$detailLevel='full'">
                                <xsl:apply-templates/>
                            </xsl:if> 
                        </xsl:copy>
                    </xsl:for-each>
                </xsl:when>
                <xsl:when test="local-name()='attrList'">
                    <xsl:for-each select="./dxf:attr">
                        <xsl:copy>
                            <xsl:copy-of select="@*"/>
                            <xsl:apply-templates/>
                        </xsl:copy>
                    </xsl:for-each>
                </xsl:when>
                <xsl:when test="local-name()='partList'">
                    <xsl:for-each select="./dxf:part">
                        <xsl:copy> 
                            <xsl:copy-of select="@*"/>
                            <xsl:copy-of select="./dxf:type"/>
                            <xsl:apply-templates/> 
                       </xsl:copy> 
                    </xsl:for-each>
                </xsl:when>
            </xsl:choose>
        </xsl:copy>
  </xsl:template>
  
  <xsl:template match="dxf:xref/dxf:node">
    <xsl:copy>
        <xsl:copy-of select="@*"/>
        <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="dxf:xref/dxf:node/*">
    <xsl:if test="$edu.ucla.mbi.services.detail='base'
                  and local-name()!='xrefList'
                  and local-name()!='partList' 
                  and local-name()!='attrList'" >
        <xsl:copy>
            <xsl:copy-of select="@*"/>
            <xsl:copy-of select="./text()"/>
        </xsl:copy>
    </xsl:if>

    <xsl:if test="$edu.ucla.mbi.services.detail='full'">
        <xsl:call-template name="node">
            <xsl:with-param name="detailLevel">
                base
            </xsl:with-param>
        </xsl:call-template>
    </xsl:if>

  </xsl:template>  

 
  <xsl:template match="dxf:part/dxf:node">
    <xsl:copy>
        <xsl:copy-of select="@*"/>
        <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="dxf:part/dxf:node/*">
    <xsl:if test="$edu.ucla.mbi.services.detail='base'  
                  and local-name()!='xrefList'  
                  and local-name()!='partList'  
                  and local-name()!='attrList'" >
        <xsl:copy>
            <xsl:copy-of select="@*"/>
            <xsl:copy-of select="./text()"/>
        </xsl:copy>
    </xsl:if>

    <xsl:if test="$edu.ucla.mbi.services.detail='full' ">
        <xsl:call-template name="node">
            <xsl:with-param name="detailLevel">
                base
            </xsl:with-param>
        </xsl:call-template>
    </xsl:if>
  </xsl:template>  
 
  <xsl:template match="dxf:attr/*">
    <xsl:copy>
        <xsl:copy-of select="./text()"/>
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>
