<xsl:param name="use.extensions">1</xsl:param>
<xsl:param name="tablecolumns.extension">1</xsl:param>
<xsl:param name="default.table.width">auto</xsl:param>
<xsl:param name="default.table.rules">none</xsl:param>

<xsl:template match="abstract[@role='titlepage.recto']" mode="titlepage.mode"></xsl:template>
<xsl:template match="abstract[@role='titlepage.verso']" mode="titlepage.mode"></xsl:template>

<xsl:template name="docma_para_style_att">
  <xsl:param name="docma_CSS_style" />
  <xsl:param name="docma_FO_style" />
</xsl:template>
