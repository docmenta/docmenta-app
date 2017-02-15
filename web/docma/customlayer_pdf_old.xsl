<xsl:template match="para[@role]">
  <xsl:variable name="keep.together">
    <xsl:call-template name="pi.dbfo_keep-together"/>
  </xsl:variable>
  <fo:block>
    <xsl:variable name="role_val" select="concat(' ', @role, ' ')" />
    <xsl:choose>
      ###when_block_style###
      <xsl:when test="contains($role_val, ' normal-para ')">
        <xsl:if test="not((count(preceding-sibling::*)=0) and boolean(parent::para))">
          ###para_spacing###
        </xsl:if>
      </xsl:when>
      <!-- <xsl:when test="@role = 'docma-table-cell')"></xsl:when> -->
    </xsl:choose>
    <xsl:if test="contains(@role, 'indent-level')">
      <xsl:variable name="level_str" select="substring-after($role_val, 'indent-level')" />
      <xsl:variable name="level_num" select="normalize-space(substring-before($level_str, ' '))" />
      <xsl:variable name="padd_value">###para_indent###</xsl:variable>
      <xsl:attribute name="margin-left"><xsl:value-of select="$padd_value" /></xsl:attribute>
    </xsl:if>
    <!--
        <xsl:if test="contains(@role, 'padding-left:')">
          <xsl:attribute name="margin-left"><xsl:value-of
            select="normalize-space(substring-before(substring-after(@role, 'padding-left:'), ';'))" />
          </xsl:attribute>
        </xsl:if>
    -->
    <xsl:if test="contains(@role, 'padding:')">
      <xsl:attribute name="padding"><xsl:value-of
        select="normalize-space(substring-before(substring-after(@role, 'padding:'), ';'))" />
      </xsl:attribute>
    </xsl:if>
    <xsl:if test="contains(@role, 'align-left')">
      <xsl:attribute name="text-align">left</xsl:attribute>
    </xsl:if>
    <xsl:if test="contains(@role, 'align-right')">
      <xsl:attribute name="text-align">right</xsl:attribute>
    </xsl:if>
    <xsl:if test="contains(@role, 'align-center')">
      <xsl:attribute name="text-align">center</xsl:attribute>
    </xsl:if>
    <xsl:if test="contains(@role, 'align-full')">
      <xsl:attribute name="text-align">justify</xsl:attribute>
    </xsl:if>

    <xsl:if test="$keep.together != ''">
      <xsl:attribute name="keep-together.within-column"><xsl:value-of
                      select="$keep.together"/></xsl:attribute>
    </xsl:if>
    <xsl:call-template name="anchor"/>
    <xsl:apply-templates/>
  </fo:block>
</xsl:template>

<xsl:template match="anchor">
  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>
  <xsl:choose>
    <xsl:when test="parent::section"><fo:block id="{$id}"/></xsl:when>
    <xsl:when test="parent::chapter"><fo:block id="{$id}"/></xsl:when>
    <xsl:when test="parent::part"><fo:block id="{$id}"/></xsl:when>
    <xsl:when test="parent::article"><fo:block id="{$id}"/></xsl:when>
    <xsl:when test="parent::preface"><fo:block id="{$id}"/></xsl:when>
    <xsl:when test="parent::appendix"><fo:block id="{$id}"/></xsl:when>
    <xsl:otherwise><fo:inline id="{$id}"/></xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="processing-instruction('linebreak')">
  <fo:block/>
</xsl:template>

<xsl:template match="processing-instruction('hard-pagebreak')">
  <fo:block break-after='page'/>
</xsl:template>

<xsl:template name="itemizedlist.label.markup">
  <xsl:param name="itemsymbol" select="'disc'"/>

  <xsl:choose>
    <xsl:when test="$itemsymbol='none'"></xsl:when>
    <xsl:when test="$itemsymbol='disc'">&#x2022;</xsl:when>
    <xsl:when test="$itemsymbol='bullet'">&#x2022;</xsl:when>
    <xsl:when test="$itemsymbol='square'">-</xsl:when>
    <xsl:when test="$itemsymbol='circle'"><fo:inline font-family="Courier" font-size="0.8em">O</fo:inline></xsl:when>
    <!-- <xsl:when test="$itemsymbol='circle'">&#x25CB;</xsl:when> -->
    <xsl:when test="$itemsymbol='endash'">&#x2013;</xsl:when>
    <xsl:when test="$itemsymbol='emdash'">&#x2014;</xsl:when>
    <xsl:otherwise>&#x2022;</xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:attribute-set name="example.properties">
  <xsl:attribute name="space-before.minimum">0pt</xsl:attribute>
  <xsl:attribute name="space-before.optimum">0pt</xsl:attribute>
  <xsl:attribute name="space-before.maximum">0pt</xsl:attribute>
  <xsl:attribute name="space-after.minimum">0pt</xsl:attribute>
  <xsl:attribute name="space-after.optimum">0pt</xsl:attribute>
  <xsl:attribute name="space-after.maximum">0pt</xsl:attribute>
  <xsl:attribute name="keep-together.within-column">always</xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="table.cell.padding">
  <xsl:attribute name="padding-start">0pt</xsl:attribute>
  <xsl:attribute name="padding-end">0pt</xsl:attribute>
  <xsl:attribute name="padding-top">0pt</xsl:attribute>
  <xsl:attribute name="padding-bottom">0pt</xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="table.properties">
  <xsl:attribute name="margin-left">###table_indent###</xsl:attribute>
  <xsl:attribute name="keep-together.within-column">
    <xsl:choose>
      <xsl:when test="contains(@class, 'keep_together_auto') or (contains(@style, 'keep-together') and starts-with(normalize-space(substring-after(substring-after(@style, 'keep-together'), ':')), 'auto'))">auto</xsl:when>
      <xsl:otherwise>always</xsl:otherwise>
    </xsl:choose>
  </xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="informaltable.properties" use-attribute-sets="table.properties" />

<xsl:template name="docma_table_class_properties">
  <xsl:variable name="class_val">
    <xsl:choose>
      <xsl:when test="contains(@class, ' ') and contains(@class, 'indent-level')">
        <xsl:choose>
          <xsl:when test="starts-with(@class, 'indent-level')">
            <xsl:value-of select="normalize-space(substring-after(@class, ' '))"/>
          </xsl:when>
          <xsl:otherwise><xsl:value-of select="substring-before(@class, ' ')"/></xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise><xsl:value-of select="@class"/></xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  ###table_classes###
</xsl:template>

<xsl:template name="table.row.properties">

  <xsl:variable name="row-height">
    <xsl:if test="processing-instruction('dbfo')">
      <xsl:call-template name="pi.dbfo_row-height"/>
    </xsl:if>
  </xsl:variable>

  <xsl:if test="$row-height != ''">
    <xsl:attribute name="block-progression-dimension">
      <xsl:value-of select="$row-height"/>
    </xsl:attribute>
  </xsl:if>

  <xsl:variable name="bgcolor">
    <xsl:call-template name="pi.dbfo_bgcolor"/>
  </xsl:variable>

  <xsl:if test="$bgcolor != ''">
    <xsl:attribute name="background-color">
      <xsl:value-of select="$bgcolor"/>
    </xsl:attribute>
  </xsl:if>

  <!-- Keep header row with next row -->
  <xsl:if test="ancestor::thead">
    <xsl:attribute name="keep-with-next.within-column">always</xsl:attribute>
  </xsl:if>
  <xsl:variable name="class_val" select="@class" />
  ###table_row_classes###
</xsl:template>

<xsl:template name="table.cell.properties">
  <xsl:param name="bgcolor.pi" select="''"/>
  <xsl:param name="rowsep.inherit" select="1"/>
  <xsl:param name="colsep.inherit" select="1"/>
  <xsl:param name="col" select="1"/>
  <xsl:param name="valign.inherit" select="''"/>
  <xsl:param name="align.inherit" select="''"/>
  <xsl:param name="char.inherit" select="''"/>

  <xsl:choose>
    <xsl:when test="ancestor::tgroup">
      <!-- CALS tables are not supported by Docmenta -->
    </xsl:when>
    <xsl:otherwise>
      <!-- HTML table -->
      <xsl:if test="$bgcolor.pi != ''">
        <xsl:attribute name="background-color">
          <xsl:value-of select="$bgcolor.pi"/>
        </xsl:attribute>
      </xsl:if>

      <xsl:if test="$align.inherit != ''">
        <xsl:attribute name="text-align">
          <xsl:value-of select="$align.inherit"/>
        </xsl:attribute>
      </xsl:if>

      <xsl:if test="$valign.inherit != ''">
        <xsl:attribute name="display-align">
          <xsl:choose>
            <xsl:when test="$valign.inherit='top'">before</xsl:when>
            <xsl:when test="$valign.inherit='middle'">center</xsl:when>
            <xsl:when test="$valign.inherit='bottom'">after</xsl:when>
            <xsl:otherwise>
              <xsl:message>
                <xsl:text>Unexpected valign value: </xsl:text>
                <xsl:value-of select="$valign.inherit"/>
                <xsl:text>, center used.</xsl:text>
              </xsl:message>
              <xsl:text>center</xsl:text>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
      </xsl:if>

      <xsl:if test="boolean(@class)">
        <xsl:variable name="class_val" select="@class" />
        ###table_cell_classes###
      </xsl:if>
      <xsl:call-template name="html.table.cell.rules"/>

    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="table.cell.block.properties">
  <xsl:variable name="table_padding"
    select="ancestor::*[(local-name() = 'table') or (local-name() = 'informaltable')]/@cellpadding" />
  <xsl:choose>
    <xsl:when test="string-length($table_padding) > 0">
      <xsl:attribute name="margin"><xsl:value-of select="$table_padding" />pt</xsl:attribute>
    </xsl:when>
    <xsl:otherwise><xsl:attribute name="margin">0pt</xsl:attribute></xsl:otherwise>
  </xsl:choose>
</xsl:template>
