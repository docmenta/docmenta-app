<xsl:template name="user.pagemasters">
    <fo:simple-page-master master-name="coverpage-first"
                             page-width="{$page.width}"
                             page-height="{$page.height}"
                             margin-top="0"
                             margin-bottom="0">
        <xsl:attribute name="margin-{$direction.align.start}">0</xsl:attribute>
        <xsl:attribute name="margin-{$direction.align.end}">0</xsl:attribute>
        <fo:region-body margin-top="0" margin-bottom="0" margin-left="0" margin-right="0"
                        column-gap="0" column-count="1">
            <xsl:attribute name="background-image">
              <xsl:call-template name="fo-external-image">
                <xsl:with-param name="filename" select="'###coverimage_url###'"/>
              </xsl:call-template>
            </xsl:attribute>
            <xsl:attribute name="background-attachment">fixed</xsl:attribute>
            <xsl:attribute name="background-repeat">no-repeat</xsl:attribute>
            <xsl:attribute name="background-position-horizontal">###cover_pos_x###</xsl:attribute>
            <xsl:attribute name="background-position-vertical">###cover_pos_y###</xsl:attribute>
            ###coverpage_attributes###
        </fo:region-body>
    </fo:simple-page-master>

    <fo:page-sequence-master master-name="coverpage">
      <fo:repeatable-page-master-alternatives>
        <fo:conditional-page-master-reference master-reference="blank"
                                              blank-or-not-blank="blank"/>
        <fo:conditional-page-master-reference master-reference="coverpage-first"
                                              page-position="first"/>
        <fo:conditional-page-master-reference master-reference="titlepage-odd"
                                              odd-or-even="odd"/>
        <fo:conditional-page-master-reference 
                                              odd-or-even="even">
          <xsl:attribute name="master-reference">
            <xsl:choose>
              <xsl:when test="$double.sided != 0">titlepage-even</xsl:when>
              <xsl:otherwise>titlepage-odd</xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
        </fo:conditional-page-master-reference>
      </fo:repeatable-page-master-alternatives>
    </fo:page-sequence-master>
</xsl:template>

<xsl:template name="front.cover" >
  <xsl:if test="###extra_coverpage###">
    <xsl:call-template name="page.sequence" >
      <xsl:with-param name="master-reference" select="'coverpage'" />
      <xsl:with-param name="content">
          <fo:block-container width="100%" height="100%" ###cover_pagebreak### >
            <fo:block></fo:block>
          </fo:block-container>
          ###coverpage_blank###
      </xsl:with-param>
    </xsl:call-template>
  </xsl:if>
</xsl:template>
