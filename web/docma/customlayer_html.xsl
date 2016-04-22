<xsl:param name="html.cleanup">0</xsl:param>

<xsl:template match="processing-instruction('linebreak')">
  <xsl:choose>
    <xsl:when test="###is_epub###">
      <xsl:element name="br" namespace="http://www.w3.org/1999/xhtml" />
    </xsl:when>
    <xsl:otherwise><xsl:element name="br" /></xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="mediaobject|inlinemediaobject" mode="class.value">
  <xsl:param name="class" select="local-name(.)"/>
  <xsl:choose>
    <xsl:when test="boolean(@role) and (string-length(@role) != 0)"><xsl:value-of select="@role"/></xsl:when>
    <xsl:otherwise><xsl:value-of select="$class"/></xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="breadcrumbs">
  <xsl:param name="this.node" select="." />
  <div id="breadcrumbsnavigation" class="breadcrumbs">
    <xsl:for-each select="$this.node/ancestor::*">
      <span class="breadcrumb-link">
        <a class="breadcrumb_node">
          <xsl:attribute name="href">
            <xsl:call-template name="href.target">
              <xsl:with-param name="object" select="." />
              <xsl:with-param name="context" select="$this.node" />
            </xsl:call-template>
          </xsl:attribute>
          <xsl:apply-templates select="." mode="title.markup" />
        </a>
      </span>
      <xsl:text> </xsl:text>
      <span class="breadcrumb_separator"><xsl:text>###bread_separator###</xsl:text></span>
      <xsl:text> </xsl:text>
    </xsl:for-each>
    <!-- And display the current node, but not as a link -->
    <span class="breadcrumb_node breadcrumb_lastnode">
        <xsl:apply-templates select="$this.node" mode="title.markup" />
    </span>
  </div>
</xsl:template>
