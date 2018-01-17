<?xml version="1.0" encoding="UTF-8" ?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml" encoding="UTF-8" indent="no" />

  <xsl:param name="docma_output_type" select="''" />
  <xsl:param name="docma_fit_images" select="''" />

  <xsl:template match="body">
    <book>
      <xsl:if test="boolean(@lang)">
        <xsl:attribute name="lang"><xsl:value-of select="@lang" /></xsl:attribute>
      </xsl:if>
      <xsl:apply-templates />
    </book>
  </xsl:template>
  <xsl:template match="body[@class='article']">
    <article>
      <xsl:if test="boolean(@lang)">
        <xsl:attribute name="lang"><xsl:value-of select="@lang" /></xsl:attribute>
      </xsl:if>
      <xsl:apply-templates />
    </article>
  </xsl:template>

  <xsl:template match="div[boolean(@class)]">
    <xsl:choose>
      <xsl:when test="starts-with(@class, '_formal_')">
        <xsl:variable name="label_id" select="normalize-space(substring-after(@class, '_formal_'))" />
        <example role="{$label_id}">
          <xsl:call-template name="docma_formal_id_and_title" />
          <xsl:apply-templates />
        </example>
      </xsl:when>
      <xsl:when test="not(starts-with(@class, 'doc-'))">
        <para role="{@class}">
          <xsl:if test="boolean(@id)"><xsl:attribute name="id"><xsl:value-of select="@id" /></xsl:attribute></xsl:if>
          <xsl:call-template name="docma_keep_together" />
          <xsl:apply-templates />
        </para>
      </xsl:when>
      <xsl:when test="@class='doc-part'">
        <part>
          <xsl:if test="boolean(@id)"><xsl:attribute name="id"><xsl:value-of select="@id" /></xsl:attribute></xsl:if>
          <xsl:apply-templates />
        </part>
      </xsl:when>
      <xsl:when test="@class='doc-chapter'">
        <chapter>
          <xsl:if test="boolean(@id)"><xsl:attribute name="id"><xsl:value-of select="@id" /></xsl:attribute></xsl:if>
          <xsl:apply-templates />
        </chapter>
      </xsl:when>
      <xsl:when test="@class='doc-section'">
        <section>
          <xsl:if test="boolean(@id)"><xsl:attribute name="id"><xsl:value-of select="@id" /></xsl:attribute></xsl:if>
          <xsl:apply-templates />
        </section>
      </xsl:when>
      <xsl:when test="@class='doc-content'">
        <xsl:if test="boolean(@id)">
         <xsl:if test="not(preceding-sibling::*[1][translate(local-name(), '123456', '------') = 'h-'])">
          <anchor id="{@id}" >
            <xsl:if test="boolean(@title)"><xsl:attribute name="xreflabel"><xsl:value-of select="@title" /></xsl:attribute></xsl:if>
          </anchor>
         </xsl:if>
        </xsl:if>
        <xsl:apply-templates />
      </xsl:when>
      <xsl:when test="@class='doc-preface'">
        <preface>
          <xsl:if test="boolean(@id)"><xsl:attribute name="id"><xsl:value-of select="@id" /></xsl:attribute></xsl:if>
          <xsl:apply-templates />
        </preface>
      </xsl:when>
      <xsl:when test="@class='doc-appendix'">
        <appendix>
          <xsl:if test="boolean(@id)"><xsl:attribute name="id"><xsl:value-of select="@id" /></xsl:attribute></xsl:if>
          <xsl:apply-templates />
        </appendix>
      </xsl:when>
      <xsl:when test="@class='doc-index'"><index /></xsl:when>
      <!-- Added para inside partintro, because FOP gives error if an anchor element is child of partintro. -->
      <xsl:when test="@class='doc-partintro'"><partintro><para><xsl:apply-templates /></para></partintro></xsl:when>
      <xsl:when test="@class='doc-info'"><bookinfo><xsl:apply-templates /></bookinfo></xsl:when>
      <xsl:when test="@class='doc-title'"><title><xsl:apply-templates /></title></xsl:when>
      <xsl:when test="@class='doc-subtitle'"><subtitle><xsl:apply-templates /></subtitle></xsl:when>
      <xsl:when test="@class='doc-corpauthor'"><corpauthor><xsl:apply-templates /></corpauthor></xsl:when>
      <xsl:when test="@class='doc-releaseinfo'"><releaseinfo><xsl:apply-templates /></releaseinfo></xsl:when>
      <xsl:when test="@class='doc-pubdate'"><pubdate><xsl:apply-templates /></pubdate></xsl:when>
      <xsl:when test="@class='doc-authorgroup'"><authorgroup><xsl:apply-templates /></authorgroup></xsl:when>
      <xsl:when test="@class='doc-author'"><author><othername><xsl:apply-templates /></othername></author></xsl:when>
      <xsl:when test="@class='doc-copyright'"><copyright><xsl:apply-templates /></copyright></xsl:when>
      <xsl:when test="@class='doc-year'"><year><xsl:apply-templates /></year></xsl:when>
      <xsl:when test="@class='doc-holder'"><holder><xsl:apply-templates /></holder></xsl:when>
      <xsl:when test="@class='doc-credit'"><othercredit><xsl:apply-templates /></othercredit></xsl:when>
      <xsl:when test="@class='doc-othername'"><othername><xsl:apply-templates /></othername></xsl:when>
      <xsl:when test="@class='doc-titlepage1'"><abstract role="titlepage.recto"><xsl:apply-templates /></abstract></xsl:when>
      <xsl:when test="@class='doc-titlepage2'"><abstract role="titlepage.verso"><xsl:apply-templates /></abstract></xsl:when>
      <xsl:when test="@class='doc-abstract'"><abstract><xsl:apply-templates /></abstract></xsl:when>
      <xsl:when test="@class='doc-legalnotice'"><legalnotice><xsl:apply-templates /></legalnotice></xsl:when>
      <xsl:when test="@class='doc-publisher'"><publisher><publishername><xsl:apply-templates /></publishername></publisher></xsl:when>
      <xsl:when test="@class='doc-biblioid'"><biblioid><xsl:apply-templates /></biblioid></xsl:when>
      <xsl:when test="@class='doc-coverimage'">
          <mediaobject role="cover"><imageobject><imagedata fileref="{./img[1]/@src}"/></imageobject></mediaobject>
      </xsl:when>
      <xsl:otherwise>
        <para role="{@class}">
          <xsl:if test="boolean(@id)"><xsl:attribute name="id"><xsl:value-of select="@id" /></xsl:attribute></xsl:if>
          <xsl:apply-templates />
        </para>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="h1|h2|h3|h4|h5|h6">
    <xsl:variable name="cont_next" select="following-sibling::*[1][@class='doc-content'][@id]" />
    <title>
      <xsl:if test="$cont_next">
        <!-- Put the anchor of the first content node inside of the section title, to avoid a page-break between title and content in PDF output -->
        <anchor id="{$cont_next/@id}" >
            <xsl:if test="boolean($cont_next/@title)"><xsl:attribute name="xreflabel"><xsl:value-of select="$cont_next/@title" /></xsl:attribute></xsl:if>
        </anchor>
      </xsl:if>
      <xsl:value-of select="." />
    </title>
  </xsl:template>

  <xsl:template match="p">
    <xsl:call-template name="docma_pagebreak_before" />
    <para>
      <xsl:if test="boolean(@id)"><xsl:attribute name="id"><xsl:value-of select="@id" /></xsl:attribute></xsl:if>
      <xsl:choose>
        <!--
        <xsl:when test="boolean(@style) and starts-with(@class, 'align-')">
          <xsl:attribute name="role">style=<xsl:value-of select="@class"/>;<xsl:value-of select="@style"/></xsl:attribute>
        </xsl:when>
        <xsl:when test="boolean(@style) and not(boolean(@class))">
          <xsl:attribute name="role">style=<xsl:value-of select="@style"/></xsl:attribute>
        </xsl:when>
        -->
        <xsl:when test="boolean(@class) and (string-length(@class) > 0)">
          <xsl:attribute name="role">normal-para <xsl:value-of select="@class"/></xsl:attribute>
        </xsl:when>
        <xsl:when test="starts-with(child::img[1]/@class, 'align-')">
          <xsl:attribute name="role">normal-para <xsl:value-of select="child::img[1]/@class"/></xsl:attribute>
        </xsl:when>
        <xsl:otherwise>
          <xsl:attribute name="role">normal-para</xsl:attribute>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:call-template name="docma_keep_together" />
      <xsl:apply-templates />
    </para>
    <xsl:call-template name="docma_pagebreak_after" />
  </xsl:template>

  <xsl:template match="pre">
    <xsl:call-template name="docma_pagebreak_before" />
    <para>
      <xsl:if test="boolean(@id)"><xsl:attribute name="id"><xsl:value-of select="@id" /></xsl:attribute></xsl:if>
      <xsl:attribute name="role">
        <xsl:choose>
          <xsl:when test="boolean(@class) and (string-length(@class) > 0)"><xsl:value-of select="@class"/></xsl:when>
          <xsl:otherwise><xsl:text>pre</xsl:text></xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
      <xsl:call-template name="docma_keep_together" />
      <xsl:apply-templates />
    </para>
    <xsl:call-template name="docma_pagebreak_after" />
  </xsl:template>

  <xsl:template match="a">
    <xsl:variable name="is_print" select="$docma_output_type = 'print'" />
    <xsl:choose>
      <xsl:when test="boolean(@href) and starts-with(@href, '#')">
        <xsl:variable name="href_id" select="normalize-space(substring-after(@href, '#'))" />
        <xsl:choose>
          <xsl:when test="starts-with(normalize-space(@title), '%target%')">
            <xsl:variable name="xref_style" select="normalize-space(substring-after(@title, '%target%'))" />
            <xref linkend="{$href_id}" >
              <xsl:if test="string-length($xref_style) > 0">
                <xsl:attribute name="xrefstyle">select: <xsl:value-of select="$xref_style" /></xsl:attribute>
              </xsl:if>
            </xref>
          </xsl:when>
          <xsl:when test="(starts-with(normalize-space(@title), '%target_print%')) and $is_print">
            <xsl:variable name="xref_style" select="normalize-space(substring-after(@title, '%target_print%'))" />
            <xref linkend="{$href_id}" >
              <xsl:if test="string-length($xref_style) > 0">
                <xsl:attribute name="xrefstyle">select: <xsl:value-of select="$xref_style" /></xsl:attribute>
              </xsl:if>
            </xref>
          </xsl:when>
          <xsl:when test="(string-length(normalize-space(@title)) > 0) and $is_print">
            <link linkend="{$href_id}">
              <xsl:choose>
                <xsl:when test="contains(@title, '%page')">
                  <xsl:attribute name="xrefstyle">select: page</xsl:attribute>
                  <xsl:variable name="tit_txt" select="normalize-space(substring-before(@title, '%page'))" />
                  <xsl:choose>
                    <xsl:when test="string-length($tit_txt) > 0"><xsl:value-of select="$tit_txt" /></xsl:when>
                    <xsl:otherwise><xsl:apply-templates /></xsl:otherwise>
                  </xsl:choose>
                </xsl:when>
                <xsl:when test="contains(@title, '%nopage')">
                  <xsl:attribute name="xrefstyle">select: nopage</xsl:attribute>
                  <xsl:variable name="tit_txt" select="normalize-space(substring-before(@title, '%nopage'))" />
                  <xsl:choose>
                    <xsl:when test="string-length($tit_txt) > 0"><xsl:value-of select="$tit_txt" /></xsl:when>
                    <xsl:otherwise><xsl:apply-templates /></xsl:otherwise>
                  </xsl:choose>
                </xsl:when>
                <xsl:otherwise><xsl:value-of select="@title" /></xsl:otherwise>
              </xsl:choose>
            </link>
          </xsl:when>
          <xsl:otherwise>
            <link linkend="{$href_id}"><xsl:apply-templates /></link>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <ulink url="{@href}"><phrase role="link_external">
          <xsl:choose>
            <xsl:when test="normalize-space(@title) = '%target%'">
              <xsl:value-of select="@href" />
            </xsl:when>
            <xsl:when test="(normalize-space(@title) = '%target_print%') and $is_print">
              <xsl:value-of select="@href" />
            </xsl:when>
            <xsl:when test="(string-length(normalize-space(@title)) > 0) and $is_print">
              <xsl:value-of select="@title" />
            </xsl:when>
            <xsl:otherwise><xsl:apply-templates /></xsl:otherwise>
          </xsl:choose>
        </phrase></ulink>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="span[boolean(@class)]">
      <xsl:choose>
        <xsl:when test="@class = 'indexterm'">
          <xsl:variable name="index_str" ><xsl:apply-templates /></xsl:variable>
          <xsl:call-template name="insert_index_term" >
            <xsl:with-param name="index_str" select="normalize-space($index_str)" />
          </xsl:call-template>
        </xsl:when>
        <xsl:when test="@class = 'footnote'">
          <xsl:choose>
            <xsl:when test="starts-with(normalize-space(child::node()[1]), '{see:')">
              <xsl:variable name="target_id"
                select="normalize-space(substring-before(substring-after(child::node()[1], '#'), '}'))" />
              <footnoteref linkend="{$target_id}" />
            </xsl:when>
            <xsl:otherwise>
              <footnote>
                <xsl:if test="boolean(@id)"><xsl:attribute name="id"><xsl:value-of select="@id" /></xsl:attribute></xsl:if>
                <para><xsl:apply-templates /></para>
              </footnote>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:otherwise>
          <phrase role="{@class}">
            <xsl:if test="boolean(@id)"><xsl:attribute name="id"><xsl:value-of select="@id" /></xsl:attribute></xsl:if>
            <xsl:if test="boolean(@title)"><xsl:attribute name="xreflabel"><xsl:value-of select="@title" /></xsl:attribute></xsl:if>
            <xsl:apply-templates />
          </phrase>
        </xsl:otherwise>
      </xsl:choose>
  </xsl:template>

  <xsl:template match="br">
    <xsl:processing-instruction name="linebreak" />
  </xsl:template>

  <xsl:template match="strong|b">
    <phrase role="strong">
      <xsl:if test="boolean(@id)"><xsl:attribute name="id"><xsl:value-of select="@id" /></xsl:attribute></xsl:if>
      <xsl:if test="boolean(@title)"><xsl:attribute name="xreflabel"><xsl:value-of select="@title" /></xsl:attribute></xsl:if>
      <xsl:apply-templates />
    </phrase>
  </xsl:template>

  <xsl:template match="big">
    <phrase>
      <xsl:if test="boolean(@id)"><xsl:attribute name="id"><xsl:value-of select="@id" /></xsl:attribute></xsl:if>
      <xsl:if test="boolean(@title)"><xsl:attribute name="xreflabel"><xsl:value-of select="@title" /></xsl:attribute></xsl:if>
      <xsl:attribute name="role">
        <xsl:choose>
          <xsl:when test="boolean(@class)"><xsl:value-of select="@class" /></xsl:when>
          <xsl:otherwise><xsl:text>big</xsl:text></xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
      <xsl:apply-templates />
    </phrase>
  </xsl:template>

  <xsl:template match="em|i">
    <phrase role="emphasis">
      <xsl:if test="boolean(@id)"><xsl:attribute name="id"><xsl:value-of select="@id" /></xsl:attribute></xsl:if>
      <xsl:if test="boolean(@title)"><xsl:attribute name="xreflabel"><xsl:value-of select="@title" /></xsl:attribute></xsl:if>
      <xsl:apply-templates />
    </phrase>
  </xsl:template>

  <xsl:template match="sub">
      <subscript><xsl:apply-templates /></subscript>
  </xsl:template>

  <xsl:template match="sup">
      <superscript><xsl:apply-templates /></superscript>
  </xsl:template>

  <xsl:template match="tt">
    <phrase>
      <xsl:if test="boolean(@id)"><xsl:attribute name="id"><xsl:value-of select="@id" /></xsl:attribute></xsl:if>
      <xsl:if test="boolean(@title)"><xsl:attribute name="xreflabel"><xsl:value-of select="@title" /></xsl:attribute></xsl:if>
      <xsl:attribute name="role">
        <xsl:choose>
          <xsl:when test="boolean(@class)"><xsl:value-of select="@class" /></xsl:when>
          <xsl:otherwise><xsl:text>tt</xsl:text></xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
      <xsl:apply-templates />
    </phrase>
  </xsl:template>

  <xsl:template match="ins">
      <xsl:apply-templates />
  </xsl:template>

  <xsl:template match="del"></xsl:template>

  <xsl:template match="ul">
    <xsl:if test="count(child::*) > 0">
      <xsl:call-template name="docma_pagebreak_before" />
      <xsl:choose>
        <xsl:when test="boolean(parent::ul) or boolean(parent::ol)">
          <listitem override="none"><xsl:call-template name="docma_itemizedlist" /></listitem>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="docma_itemizedlist" />
        </xsl:otherwise>
      </xsl:choose>
      <xsl:call-template name="docma_pagebreak_after" />
    </xsl:if>
  </xsl:template>

  <xsl:template name="docma_itemizedlist">
      <itemizedlist>
        <xsl:if test="contains(@style, 'list-style-type:')">
            <xsl:attribute name="mark"><xsl:value-of
              select="normalize-space(substring-before(substring-after(@style, 'list-style-type:'), ';'))" />
            </xsl:attribute>
        </xsl:if>
        <xsl:apply-templates />
      </itemizedlist>
  </xsl:template>

  <xsl:template match="ol">
    <xsl:if test="count(child::*) > 0">
      <xsl:call-template name="docma_pagebreak_before" />
      <xsl:choose>
        <xsl:when test="boolean(parent::ul) or boolean(parent::ol)">
          <listitem override="none"><xsl:call-template name="docma_orderedlist" /></listitem>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="docma_orderedlist" />
        </xsl:otherwise>
      </xsl:choose>
      <xsl:call-template name="docma_pagebreak_after" />
    </xsl:if>
  </xsl:template>

  <xsl:template name="docma_orderedlist">
      <orderedlist>
        <xsl:choose>
          <xsl:when test="contains(@style, 'list-style-type:')">
            <xsl:attribute name="numeration"><xsl:value-of
              select="translate(substring-before(substring-after(@style, 'list-style-type:'), ';'), '- ', '')" />
            </xsl:attribute>
          </xsl:when>
          <xsl:otherwise><xsl:attribute name="numeration">arabic</xsl:attribute></xsl:otherwise>
        </xsl:choose>
        <xsl:apply-templates />
      </orderedlist>
  </xsl:template>

  <xsl:template match="li">
    <listitem>
      <xsl:choose>
        <xsl:when test="boolean(@value)">
          <xsl:attribute name="override"><xsl:value-of select="@value" /></xsl:attribute>
        </xsl:when>
        <xsl:when test="contains(@style, 'list-style-type:')">
            <xsl:attribute name="override"><xsl:value-of
              select="translate(substring-before(substring-after(@style, 'list-style-type:'), ';'), '- ', '')" />
            </xsl:attribute>
        </xsl:when>
      </xsl:choose>
      <xsl:apply-templates />
    </listitem>
  </xsl:template>

  <xsl:template match="dl">
      <variablelist><xsl:apply-templates /></variablelist>
  </xsl:template>

  <xsl:template match="dt">
    <varlistentry>
      <term><xsl:apply-templates /></term>
      <listitem>
        <para><xsl:apply-templates select="following-sibling::dd[1]/node()" /></para>
      </listitem>
    </varlistentry>
  </xsl:template>

  <xsl:template match="dd">
  </xsl:template>

  <xsl:template match="img">
    <xsl:choose>
      <xsl:when test="boolean(@title) and (string-length(@title) > 0)">
        <figure>
          <xsl:if test="boolean(@id)"><xsl:attribute name="id"><xsl:value-of select="@id" /></xsl:attribute></xsl:if>
          <xsl:if test="boolean(@style) and contains(@style, 'float:') and ($docma_output_type != 'print')">
            <!-- Note: As Apache FOP does not support float, suppress float for print output. Otherwise figure is not rendered. -->
            <xsl:attribute name="float"><xsl:value-of
              select="normalize-space(substring-before(substring-after(@style, 'float:'), ';'))" />
            </xsl:attribute>
          </xsl:if>
          <mediaobject>
            <xsl:if test="boolean(@class)"><xsl:attribute name="role"><xsl:value-of select="@class" /></xsl:attribute></xsl:if>
            <imageobject>
              <imagedata fileref="{@src}" ><xsl:call-template name="docma_image_size" /></imagedata>
            </imageobject>
          </mediaobject>
          <xsl:if test="string-length(normalize-space(@title)) > 0">
            <title><xsl:value-of select="@title" /></title>
          </xsl:if>
        </figure>
      </xsl:when>
      <xsl:when test="boolean(@style) and contains(@style, 'float:')">
        <informalfigure>
          <xsl:if test="boolean(@id)"><xsl:attribute name="id"><xsl:value-of select="@id" /></xsl:attribute></xsl:if>
          <xsl:attribute name="float"><xsl:value-of
            select="normalize-space(substring-before(substring-after(@style, 'float:'), ';'))" />
          </xsl:attribute>
          <mediaobject>
            <xsl:if test="boolean(@class)"><xsl:attribute name="role"><xsl:value-of select="@class" /></xsl:attribute></xsl:if>
            <imageobject>
              <imagedata fileref="{@src}" ><xsl:call-template name="docma_image_size" /></imagedata>
            </imageobject>
          </mediaobject>
        </informalfigure>
      </xsl:when>
      <xsl:otherwise>
        <inlinemediaobject>
          <xsl:if test="boolean(@id)"><xsl:attribute name="id"><xsl:value-of select="@id" /></xsl:attribute></xsl:if>
          <xsl:if test="boolean(@class)"><xsl:attribute name="role"><xsl:value-of select="@class" /></xsl:attribute></xsl:if>
          <imageobject>
            <imagedata fileref="{@src}" >
              <xsl:call-template name="docma_image_size" >
                  <xsl:with-param name="is_inline" select="'true'" />
              </xsl:call-template>
            </imagedata>
          </imageobject>
        </inlinemediaobject>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="docma_image_size">
    <xsl:param name="is_inline" />
    <xsl:choose>
      <xsl:when test="$docma_output_type = 'print'">
        <xsl:variable name="style_val" select="concat(@style, ';')" />
        <xsl:variable name="is_pwidth1" select="contains($style_val, 'print-width:')" />
        <xsl:variable name="is_pheight1" select="contains($style_val, 'print-height:')" />
        <xsl:variable name="cls_val" select="concat(' ', @class, ' ')" />
        <xsl:variable name="is_pwidth2" select="contains($cls_val, ' print_width_')" />
        <xsl:variable name="is_pheight2" select="contains($cls_val, ' print_height_')" />
        <xsl:choose>
          <xsl:when test="$is_pwidth1 or $is_pheight1 or $is_pwidth2 or $is_pheight2">
            <xsl:choose>
              <xsl:when test="$is_pwidth2">
                <xsl:attribute name="width">
                  <xsl:call-template name="docma_size_from_cls" >
                    <xsl:with-param name="size_val" select="translate(substring-before(substring-after($cls_val, ' print_width_'), ' '), '_', '.')" />
                  </xsl:call-template>
                </xsl:attribute>
              </xsl:when>
              <xsl:when test="$is_pwidth1">
                <xsl:attribute name="width"><xsl:value-of
                  select="normalize-space(substring-before(substring-after($style_val, 'print-width:'), ';'))" />
                </xsl:attribute>
              </xsl:when>
            </xsl:choose>
            <xsl:choose>
              <xsl:when test="$is_pheight2">
                <xsl:attribute name="depth">
                  <xsl:call-template name="docma_size_from_cls" >
                    <xsl:with-param name="size_val" select="translate(substring-before(substring-after($cls_val, ' print_height_'), ' '), '_', '.')" />
                  </xsl:call-template>
                </xsl:attribute>
              </xsl:when>
              <xsl:when test="$is_pheight1">
                <xsl:attribute name="depth"><xsl:value-of
                  select="normalize-space(substring-before(substring-after($style_val, 'print-height:'), ';'))" />
                </xsl:attribute>
              </xsl:when>
            </xsl:choose>
          </xsl:when>
          <xsl:otherwise>
            <xsl:call-template name="docma_simple_image_size" >
              <xsl:with-param name="is_inline" select="$is_inline" />
            </xsl:call-template>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="docma_simple_image_size" >
          <xsl:with-param name="is_inline" select="$is_inline" />
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="docma_size_from_cls">
    <xsl:param name="size_val" />
    
    <xsl:choose>
      <xsl:when test="string(number($size_val)) = 'NaN'"> <!-- Unit is already included, e.g. pt, px, ... -->
        <xsl:value-of select="$size_val" />
      </xsl:when>
      <xsl:otherwise> <!-- A number without unit is interpreted as percent value -->
        <xsl:value-of select="concat($size_val, '%')" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="docma_simple_image_size">
    <xsl:param name="is_inline" />

    <xsl:variable name="has_width" select="boolean(@width) and (string-length(@width) > 0)" />
    <xsl:variable name="has_height" select="boolean(@height) and (string-length(@height) > 0)" />
    <xsl:choose>
      <xsl:when test="$has_width or $has_height">
        <xsl:if test="$has_width">
          <xsl:attribute name="contentwidth"><xsl:value-of select="@width" /></xsl:attribute>
        </xsl:if>
        <xsl:if test="$has_height">
          <xsl:attribute name="contentdepth"><xsl:value-of select="@height" /></xsl:attribute>
        </xsl:if>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="docma_style_size" >
          <xsl:with-param name="is_inline" select="$is_inline" />
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="docma_style_size">
    <xsl:param name="is_inline" />

    <xsl:variable name="css_width">
      <xsl:call-template name="docma_get_css_value" >
        <xsl:with-param name="style_str" select="@style" />
        <xsl:with-param name="att_name" select="'width'" />
      </xsl:call-template>
    </xsl:variable>
    <xsl:variable name="css_height">
      <xsl:call-template name="docma_get_css_value" >
        <xsl:with-param name="style_str" select="@style" />
        <xsl:with-param name="att_name" select="'height'" />
      </xsl:call-template>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="(string-length($css_width) > 0) or (string-length($css_height) > 0)">
        <xsl:if test="string-length($css_width) > 0">
          <xsl:attribute name="contentwidth"><xsl:value-of select="$css_width" /></xsl:attribute>
        </xsl:if>
        <xsl:if test="string-length($css_height) > 0">
          <xsl:attribute name="contentdepth"><xsl:value-of select="$css_height" /></xsl:attribute>
        </xsl:if>
      </xsl:when>
      <xsl:otherwise>
        <xsl:if test="($is_inline != 'true') and ($docma_fit_images = 'true')" >
          <xsl:attribute name="scalefit"><xsl:text>1</xsl:text></xsl:attribute>
          <xsl:attribute name="width"><xsl:text>100%</xsl:text></xsl:attribute>
          <xsl:attribute name="contentdepth"><xsl:text>100%</xsl:text></xsl:attribute>
        </xsl:if>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="blockquote">
    <blockquote><xsl:apply-templates /></blockquote>
  </xsl:template>

  <xsl:template match="cite">
    <phrase>
      <xsl:if test="boolean(@id)">
        <xsl:attribute name="id"><xsl:value-of select="@id" /></xsl:attribute>
      </xsl:if>
      <xsl:if test="boolean(@class)">
        <xsl:attribute name="role"><xsl:value-of select="@class" /></xsl:attribute>
      </xsl:if>
      <xsl:if test="boolean(@title)"><xsl:attribute name="xreflabel"><xsl:value-of select="@title" /></xsl:attribute></xsl:if>
      <xsl:apply-templates />
    </phrase>
  </xsl:template>

  <xsl:template match="abbr">
    <abbrev><xsl:apply-templates /></abbrev>
  </xsl:template>

  <xsl:template match="acronym">
    <acronym><xsl:apply-templates /></acronym>
  </xsl:template>

  <xsl:template match="table">
    <xsl:call-template name="docma_pagebreak_before" />
    <xsl:choose>
      <xsl:when test="boolean(caption)">
        <table><xsl:call-template name="table_template" /></table>
      </xsl:when>
      <xsl:otherwise>
        <informaltable><xsl:call-template name="table_template" /></informaltable>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:call-template name="docma_pagebreak_after" />
  </xsl:template>

  <xsl:template name="table_template">
    <!-- <xsl:if test="boolean(@id)"><xsl:attribute name="id"><xsl:value-of select="@id" /></xsl:attribute></xsl:if> -->
    <xsl:if test="boolean(@class)">
      <xsl:if test="contains(concat(' ', @class, ' '), ' landscape_table ')">
          <xsl:attribute name="orient"><xsl:text>land</xsl:text></xsl:attribute>
      </xsl:if>
    </xsl:if>
    <xsl:if test="boolean(@style)">
      <xsl:variable name="style_val" select="concat(' ', translate(@style, ';', ' '))" />
      <xsl:if test="contains($style_val, ' width:')">
        <xsl:attribute name="width"><xsl:value-of
          select="substring-before(concat(normalize-space(substring-after($style_val, ' width:')), ' '), ' ')" />
        </xsl:attribute>
      </xsl:if>
    </xsl:if>
    <xsl:apply-templates select="@*" />
    <xsl:call-template name="docma_keep_together" />
    <xsl:variable name="row1" select = "tr[1]" />
    <xsl:variable name="row2" select = "*[(local-name()='thead') or (local-name()='tbody') or (local-name()='tfoot')]/tr[1]" />
    <xsl:choose>
      <xsl:when test="boolean($row1)">
        <xsl:call-template name="create_colgroup" >
          <xsl:with-param name="row" select="$row1" /><xsl:with-param name="old_widths" select="''" />
        </xsl:call-template>
      </xsl:when>
      <xsl:when test="boolean($row2)">
        <xsl:call-template name="create_colgroup" >
          <xsl:with-param name="row" select="$row2" /><xsl:with-param name="old_widths" select="''" />
        </xsl:call-template>
      </xsl:when>
    </xsl:choose>
    <xsl:apply-templates select="node()"/>
  </xsl:template>

  <xsl:template name="create_colgroup">
    <xsl:param name="row" />
    <xsl:param name="old_widths" />
    
    <xsl:variable name="current_widths">
        <xsl:call-template name="get_col_widths" ><xsl:with-param name="row" select="$row" /></xsl:call-template>
    </xsl:variable>
    <xsl:variable name="widths">
      <xsl:call-template name="merge_col_widths" >
          <xsl:with-param name="old_widths" select="$old_widths" />
          <xsl:with-param name="current_widths" select="$current_widths" />
      </xsl:call-template>
    </xsl:variable>
      
    <xsl:variable name="next1" select="$row/following-sibling::tr[1]" />
    <xsl:choose>
      <xsl:when test="boolean($next1)">
        <xsl:call-template name="create_colgroup" >
          <xsl:with-param name="row" select="$next1" />
          <xsl:with-param name="old_widths" select="$widths" />
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:variable name="next2" select="$row/parent::*[(local-name()='thead') or (local-name()='tbody') or (local-name()='tfoot')]/following-sibling::*[(local-name()='thead') or (local-name()='tbody') or (local-name()='tfoot')][1]/tr[1]" />
        <xsl:choose>
          <xsl:when test="boolean($next2)">
            <xsl:call-template name="create_colgroup" >
              <xsl:with-param name="row" select="$next2" />
              <xsl:with-param name="old_widths" select="$widths" />
            </xsl:call-template>
          </xsl:when>
          <xsl:otherwise>
            <colgroup>
              <xsl:call-template name="write_cols" ><xsl:with-param name="widths_str" select="$widths" /></xsl:call-template>
            </colgroup>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


  <xsl:template name="write_cols">
    <xsl:param name="widths_str" />

    <!-- <xsl:message>Write cols: <xsl:value-of select="$widths_str" /></xsl:message> -->
    <xsl:variable name="first_width" select="normalize-space(substring-before($widths_str, '|'))" />
    <xsl:if test="string-length($first_width) &gt; 0" >
      <col>
        <xsl:if test="$first_width != '?'" >
          <xsl:attribute name="width"><xsl:value-of select="$first_width" /></xsl:attribute>
        </xsl:if>
      </col>
      <xsl:call-template name="write_cols" ><xsl:with-param name="widths_str" select="substring-after($widths_str, ',')" /></xsl:call-template>
    </xsl:if>
  </xsl:template>


  <xsl:template name="get_col_widths">
    <xsl:param name="row" />

    <xsl:for-each select="$row/*[(local-name() = 'td') or (local-name() = 'th')]" >
      <xsl:choose>
        <xsl:when test="@colspan &gt; 1">
          <xsl:call-template name="write_no_colwidth" >
            <xsl:with-param name="col_cnt" select="@colspan" />
            <xsl:with-param name="row_cnt" select="@rowspan" />
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:choose>
            <xsl:when test="boolean(@width)"><xsl:value-of select="@width" /></xsl:when>
            <xsl:when test="starts-with(@style, 'width:')">
              <xsl:value-of select="normalize-space(substring-before(substring-after(@style, 'width:'), ';'))" />
            </xsl:when>
            <xsl:when test="contains(@style, ' width:')">
              <xsl:value-of select="normalize-space(substring-before(substring-after(@style, ' width:'), ';'))" />
            </xsl:when>
            <xsl:when test="contains(@style, ';width:')">
              <xsl:value-of select="normalize-space(substring-before(substring-after(@style, ';width:'), ';'))" />
            </xsl:when>
            <xsl:otherwise><xsl:text>?</xsl:text></xsl:otherwise>
          </xsl:choose>
          <xsl:choose>
            <xsl:when test="@rowspan &gt; 1"><xsl:text>|</xsl:text><xsl:value-of select="@rowspan" /><xsl:text>,</xsl:text></xsl:when>
            <xsl:otherwise><xsl:text>|1,</xsl:text></xsl:otherwise>
          </xsl:choose>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:for-each>
  </xsl:template>


  <xsl:template name="write_no_colwidth">
    <xsl:param name="col_cnt" />
    <xsl:param name="row_cnt" />

    <xsl:choose>
      <xsl:when test="@rowspan &gt; 1"><xsl:text>?|</xsl:text><xsl:value-of select="@rowspan" /><xsl:text>,</xsl:text></xsl:when>
      <xsl:otherwise><xsl:text>?|1,</xsl:text></xsl:otherwise>
    </xsl:choose>
    <xsl:if test="$col_cnt &gt; 1" >
      <xsl:call-template name="write_no_colwidth" >
        <xsl:with-param name="col_cnt" select="$col_cnt - 1" />
        <xsl:with-param name="row_cnt" select="$row_cnt" />
      </xsl:call-template>
    </xsl:if>
  </xsl:template>


  <xsl:template name="merge_col_widths">
    <xsl:param name="old_widths" />
    <xsl:param name="current_widths" />

    <xsl:variable name="width_1" select="normalize-space(substring-before($old_widths, ','))" />
    <xsl:variable name="width_2" select="normalize-space(substring-before($current_widths, ','))" />
    <xsl:if test="(string-length($width_1) &gt; 0) or (string-length($width_2) &gt; 0)" >
      <xsl:variable name="value_1" select="normalize-space(substring-before($width_1, '|'))" />
      <xsl:variable name="row_cnt" select="normalize-space(substring-after($width_1, '|'))" />
      <xsl:choose>
        <xsl:when test="$row_cnt &gt; 1" ><xsl:value-of select="$value_1" /><xsl:text>|</xsl:text><xsl:value-of select="$row_cnt - 1" /></xsl:when>
        <xsl:when test="(string-length($width_1) &gt; 0) and ($value_1 != '?')" ><xsl:value-of select="$value_1" /><xsl:text>|</xsl:text><xsl:value-of select="normalize-space(substring-after($width_2, '|'))" /></xsl:when>
        <xsl:otherwise><xsl:value-of select="$width_2" /></xsl:otherwise>
      </xsl:choose>
      <xsl:text>,</xsl:text>
      <xsl:choose>
        <xsl:when test="$row_cnt &gt; 1">
          <xsl:call-template name="merge_col_widths" >
            <xsl:with-param name="old_widths" select="substring-after($old_widths, ',')" />
            <xsl:with-param name="current_widths" select="$current_widths" />
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="merge_col_widths" >
            <xsl:with-param name="old_widths" select="substring-after($old_widths, ',')" />
            <xsl:with-param name="current_widths" select="substring-after($current_widths, ',')" />
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:if>
  </xsl:template>


  <xsl:template match="caption">
    <caption><xsl:apply-templates select="@*|node()" /></caption>
  </xsl:template>

  <!--
  <xsl:template match="colgroup">
    <colgroup><xsl:apply-templates select="@*|node()" /></colgroup>
  </xsl:template>

  <xsl:template match="col">
    <col><xsl:apply-templates select="@*|node()" /></col>
  </xsl:template>
  -->

  <xsl:template match="thead">
    <xsl:if test="count(child::*) > 0">
      <thead><xsl:apply-templates select="@*|node()" /></thead>
    </xsl:if>
  </xsl:template>

  <xsl:template match="tfoot">
    <xsl:if test="count(child::*) > 0">
      <tfoot><xsl:apply-templates select="@*|node()" /></tfoot>
    </xsl:if>
  </xsl:template>

  <xsl:template match="tbody">
    <tbody><xsl:apply-templates select="@*|node()" /></tbody>
  </xsl:template>

  <xsl:template match="tr">
    <xsl:variable name="table_style"
      select="ancestor::*[(local-name() = 'table') or (local-name() = 'informaltable')]/@style" />
    <tr>
      <xsl:apply-templates select="@*" />
      <xsl:choose>
      <xsl:when test="boolean(@style) and contains(@style, 'background-color:')">
        <xsl:call-template name="insert_FO_bgcolor"><xsl:with-param name="elem_style" select="@style" /></xsl:call-template>
      </xsl:when>
      <xsl:when test="contains($table_style, 'background-color:')">
        <xsl:call-template name="insert_FO_bgcolor"><xsl:with-param name="elem_style" select="$table_style" /></xsl:call-template>
      </xsl:when>
      </xsl:choose>
      <xsl:apply-templates />
    </tr>
  </xsl:template>

  <xsl:template match="th">
    <th>
      <xsl:apply-templates select="@*[not(local-name() = 'class')]" />
      <xsl:call-template name="docma_cell_class" ><xsl:with-param name="default_cls" select="'table_header'" /></xsl:call-template>
      <xsl:if test="boolean(@style) and contains(@style, 'background-color:')">
        <xsl:call-template name="insert_FO_bgcolor"><xsl:with-param name="elem_style" select="@style" /></xsl:call-template>
      </xsl:if>
      <para>
        <xsl:choose>
          <xsl:when test="boolean(@style)">
            <xsl:attribute name="role">style=<xsl:value-of select="@style"/></xsl:attribute>
          </xsl:when>
          <xsl:otherwise><xsl:attribute name="role">docma-table-cell</xsl:attribute></xsl:otherwise>
        </xsl:choose>
        <xsl:apply-templates />
      </para>
    </th>
  </xsl:template>

  <xsl:template match="td">
    <td>
      <xsl:apply-templates select="@*[not(local-name() = 'class')]" />
      <xsl:call-template name="docma_cell_class" ><xsl:with-param name="default_cls" select="'table_cell'" /></xsl:call-template>
      <xsl:if test="boolean(@style) and contains(@style, 'background-color:')">
        <xsl:call-template name="insert_FO_bgcolor"><xsl:with-param name="elem_style" select="@style" /></xsl:call-template>
      </xsl:if>
      <para>
        <xsl:choose>
          <xsl:when test="boolean(@style)">
            <xsl:attribute name="role">style=<xsl:value-of select="@style"/></xsl:attribute>
          </xsl:when>
          <xsl:otherwise><xsl:attribute name="role">docma-table-cell</xsl:attribute></xsl:otherwise>
        </xsl:choose>
        <xsl:apply-templates />
      </para>
    </td>
  </xsl:template>

<!--
  <xsl:template match="td[boolean(@class)]">
    <td>
      <xsl:apply-templates select="@*" />
      <xsl:if test="boolean(@style) and contains(@style, 'background-color:')">
        <xsl:call-template name="insert_FO_bgcolor"><xsl:with-param name="elem_style" select="@style" /></xsl:call-template>
      </xsl:if>
      <para role="{@class}"><xsl:apply-templates /></para>
    </td>
  </xsl:template>
-->

  <xsl:template match="video">
    <xsl:choose>
      <xsl:when test="boolean(@poster) and $docma_output_type = 'print'">
        <informalfigure>
          <xsl:if test="boolean(@id)"><xsl:attribute name="id"><xsl:value-of select="@id" /></xsl:attribute></xsl:if>
          <xsl:if test="boolean(@style) and contains(@style, 'float:')">
            <xsl:attribute name="float"><xsl:value-of
              select="normalize-space(substring-before(substring-after(@style, 'float:'), ';'))" />
            </xsl:attribute>
          </xsl:if>
          <mediaobject>
            <xsl:if test="boolean(@class)"><xsl:attribute name="role"><xsl:value-of select="@class" /></xsl:attribute></xsl:if>
            <imageobject>
              <imagedata fileref="{@poster}" ><xsl:call-template name="docma_image_size" /></imagedata>
            </imageobject>
          </mediaobject>
        </informalfigure>
      </xsl:when>
      <xsl:otherwise>
        <xsl:variable name="subcont" ><xsl:apply-templates /></xsl:variable>
        <xsl:choose>
          <xsl:when test="string-length(normalize-space($subcont)) > 0">
            <para><xsl:copy-of select="$subcont" /></para>
          </xsl:when>
          <xsl:otherwise>
             <para><xsl:text>Video: </xsl:text><ulink url="{@src}"><xsl:value-of select="@src" /></ulink></para>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="@*">
    <xsl:copy />
  </xsl:template>

  <xsl:template name="insert_FO_bgcolor">
    <xsl:param name="elem_style" />

    <xsl:processing-instruction name="dbfo">
      <xsl:text>bgcolor="</xsl:text>
      <xsl:value-of
        select="normalize-space(substring-before(substring-after($elem_style, 'background-color:'), ';'))" />
      <xsl:text>"</xsl:text>
    </xsl:processing-instruction>
  </xsl:template>

  <xsl:template name="insert_index_term">
    <xsl:param name="index_str" />
    <xsl:choose>
      <xsl:when test="contains($index_str, '|')">
        <xsl:call-template name="insert_index_term" ><xsl:with-param name="index_str" select="normalize-space(substring-before($index_str, '|'))" /></xsl:call-template>
        <xsl:call-template name="insert_index_term" ><xsl:with-param name="index_str" select="normalize-space(substring-after($index_str, '|'))" /></xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:variable name="range_start_id" >
          <xsl:if test="starts-with($index_str, '{start:')">
            <xsl:value-of select="normalize-space(substring-before(substring-after($index_str, '{start:'), '}'))" />
          </xsl:if>
        </xsl:variable>
        <xsl:variable name="range_end_id" >
          <xsl:if test="starts-with($index_str, '{end:')">
            <xsl:value-of select="normalize-space(substring-before(substring-after($index_str, '{end:'), '}'))" />
          </xsl:if>
        </xsl:variable>
        <xsl:choose>
          <xsl:when test="starts-with($index_str, '{start:') or starts-with($index_str, '{end:')">
            <xsl:call-template name="index_term_or_range" >
              <xsl:with-param name="index_term" select="normalize-space(substring-after($index_str, '}'))" />
              <xsl:with-param name="start_id" select="$range_start_id" />
              <xsl:with-param name="end_id" select="$range_end_id" />
            </xsl:call-template>
          </xsl:when>
          <xsl:otherwise>
            <xsl:call-template name="index_term_or_range" >
              <xsl:with-param name="index_term" select="$index_str" />
            </xsl:call-template>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="index_term_or_range">
    <xsl:param name="index_term" />
    <xsl:param name="start_id" />
    <xsl:param name="end_id" />
    <indexterm>

      <xsl:variable name="index_tmp" >
        <xsl:choose>
          <xsl:when test="contains($index_term, '{see:')">
            <xsl:value-of select="normalize-space(substring-before($index_term, '{see:'))" />
          </xsl:when>
          <xsl:when test="contains($index_term, '{seealso:')">
            <xsl:value-of select="normalize-space(substring-before($index_term, '{seealso:'))" />
          </xsl:when>
          <xsl:otherwise><xsl:value-of select="normalize-space($index_term)" /></xsl:otherwise>
        </xsl:choose>
      </xsl:variable>

      <xsl:variable name="tmp_len" select="string-length($index_tmp)" />
      <xsl:variable name="is_preferred" select="substring($index_tmp, $tmp_len - 2) = '{!}'" />
      <xsl:if test="$is_preferred">
        <xsl:attribute name="significance">preferred</xsl:attribute>
      </xsl:if>

      <xsl:variable name="index_str" >
        <xsl:choose>
          <xsl:when test="$is_preferred">
            <xsl:value-of select="normalize-space(substring($index_tmp, 1, $tmp_len - 3))" />
          </xsl:when>
          <xsl:otherwise><xsl:value-of select="$index_tmp" /></xsl:otherwise>
        </xsl:choose>
      </xsl:variable>

      <xsl:if test="string-length($start_id) > 0">
        <xsl:attribute name="class">startofrange</xsl:attribute>
        <xsl:attribute name="id">docma_idxid__<xsl:value-of select="$start_id" /></xsl:attribute>
      </xsl:if>
      <xsl:if test="string-length($end_id) > 0">
        <xsl:attribute name="class">endofrange</xsl:attribute>
        <xsl:attribute name="startref">docma_idxid__<xsl:value-of select="$end_id" /></xsl:attribute>
      </xsl:if>

      <xsl:choose>
        <xsl:when test="contains($index_str, '--')">
          <primary><xsl:value-of select="normalize-space(substring-before($index_str, '--'))" /></primary>
          <xsl:variable name="index2_str" select="substring-after($index_str, '--')" />
          <xsl:choose>
            <xsl:when test="contains($index2_str, '--')">
              <secondary><xsl:value-of select="normalize-space(substring-before($index2_str, '--'))" /></secondary>
              <tertiary><xsl:value-of select="normalize-space(substring-after($index2_str, '--'))" /></tertiary>
            </xsl:when>
            <xsl:otherwise>
              <secondary><xsl:value-of select="normalize-space($index2_str)" /></secondary>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:otherwise>
          <xsl:if test="string-length($index_str) > 0">
            <primary><xsl:value-of select="$index_str" /></primary>
          </xsl:if>
        </xsl:otherwise>
      </xsl:choose>

      <xsl:if test="contains($index_term, '{see:')">
        <see><xsl:value-of select="normalize-space(substring-before(substring-after($index_term, '{see:'), '}'))" /></see>
      </xsl:if>
      <xsl:if test="contains($index_term, '{seealso:')">
        <xsl:call-template name="index_seealso" >
          <xsl:with-param name="seealso_str" select="$index_term" />
        </xsl:call-template>
      </xsl:if>
    </indexterm>
  </xsl:template>

  <xsl:template name="index_seealso">
    <xsl:param name="seealso_str" />

    <xsl:variable name="temp_str" select="substring-after($seealso_str, '{seealso:')" />
    <seealso><xsl:value-of select="normalize-space(substring-before($temp_str, '}'))" /></seealso>
    <xsl:if test="contains($temp_str, '{seealso:')">
      <xsl:call-template name="index_seealso" >
        <xsl:with-param name="seealso_str" select="$temp_str" />
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

  <xsl:template name="docma_pagebreak_before">
    <xsl:variable name="styleval">
      <xsl:call-template name="docma_get_pagebreak_style" />
    </xsl:variable>
    <xsl:if test="contains($styleval, 'page-break-condition')">
      <xsl:variable name="cond_tmp" select="normalize-space(substring-after(substring-after($styleval, 'page-break-condition'), ':'))" />
      <xsl:variable name="cond_val" >
        <xsl:choose>
          <xsl:when test="contains($cond_tmp, ';')">
            <xsl:value-of select="normalize-space(substring-before($cond_tmp, ';'))" />
          </xsl:when>
          <xsl:otherwise><xsl:value-of select="$cond_tmp" /></xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:processing-instruction name="dbfo-need" >
        <xsl:text>height="</xsl:text><xsl:value-of select="$cond_val"/><xsl:text>"</xsl:text>
      </xsl:processing-instruction>
    </xsl:if>
    <xsl:if test="contains($styleval, 'page-break-before')">
      <xsl:if test="starts-with(normalize-space(substring-after(substring-after($styleval, 'page-break-before'), ':')), 'always')">
        <xsl:processing-instruction name="hard-pagebreak" />
      </xsl:if>
    </xsl:if>
  </xsl:template>

  <xsl:template name="docma_pagebreak_after">
    <xsl:variable name="styleval">
      <xsl:call-template name="docma_get_pagebreak_style" />
    </xsl:variable>
    <xsl:if test="contains($styleval, 'page-break-after')">
      <xsl:if test="starts-with(normalize-space(substring-after(substring-after($styleval, 'page-break-after'), ':')), 'always')">
        <xsl:processing-instruction name="hard-pagebreak" />
      </xsl:if>
    </xsl:if>
  </xsl:template>

  <xsl:template name="docma_get_pagebreak_style">
    <xsl:choose>
      <xsl:when test="@style"><xsl:value-of select="@style" /></xsl:when>
      <xsl:when test="((self::ol) or (self::ul)) and (child::li[1][@style])">
        <xsl:value-of select="child::li[1]/@style" />
      </xsl:when>
      <xsl:when test="(self::div) and (child::*[1][@style])">
        <xsl:value-of select="child::*[1]/@style" />
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="docma_keep_together">
    <xsl:choose>
      <xsl:when test="contains(@class, 'keep_together_auto')">
        <xsl:processing-instruction name="dbfo" ><xsl:text>keep-together="auto"</xsl:text></xsl:processing-instruction>
      </xsl:when>
      <xsl:when test="contains(concat(' ', @class, ' '), ' keep_together ')">
        <xsl:processing-instruction name="dbfo" ><xsl:text>keep-together="always"</xsl:text></xsl:processing-instruction>
      </xsl:when>
      <xsl:when test="contains(@style, 'keep-together')">
        <xsl:choose>
          <xsl:when test="starts-with(normalize-space(substring-after(substring-after(@style, 'keep-together'), ':')), 'auto')">
            <xsl:processing-instruction name="dbfo" ><xsl:text>keep-together="auto"</xsl:text></xsl:processing-instruction>
          </xsl:when>
          <xsl:when test="starts-with(normalize-space(substring-after(substring-after(@style, 'keep-together'), ':')), 'always')">
            <xsl:processing-instruction name="dbfo" ><xsl:text>keep-together="always"</xsl:text></xsl:processing-instruction>
          </xsl:when>
        </xsl:choose>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="docma_get_css_value">
    <xsl:param name="style_str" />
    <xsl:param name="att_name" />

    <xsl:variable name="style_s" select="concat(';', $style_str, ';')" />
    <xsl:variable name="a_pattern1" select="concat(';', $att_name, ':')" />
    <xsl:variable name="a_pattern2" select="concat(' ', $att_name, ':')" />
    <xsl:variable name="a_pattern3" select="concat(';', $att_name, ' :')" />
    <xsl:variable name="a_pattern4" select="concat(' ', $att_name, ' :')" />
    <xsl:choose>
      <xsl:when test="contains($style_s, $a_pattern1)">
        <xsl:value-of select="normalize-space(substring-before(substring-after($style_s, $a_pattern1), ';'))" />
      </xsl:when>
      <xsl:when test="contains($style_s, $a_pattern2)">
        <xsl:value-of select="normalize-space(substring-before(substring-after($style_s, $a_pattern2), ';'))" />
      </xsl:when>
      <xsl:when test="contains($style_s, $a_pattern3)">
        <xsl:value-of select="normalize-space(substring-before(substring-after($style_s, $a_pattern3), ';'))" />
      </xsl:when>
      <xsl:when test="contains($style_s, $a_pattern4)">
        <xsl:value-of select="normalize-space(substring-before(substring-after($style_s, $a_pattern4), ';'))" />
      </xsl:when>
      <xsl:otherwise><xsl:text></xsl:text></xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="docma_formal_id_and_title">
    <xsl:if test="@id"><xsl:attribute name="id"><xsl:value-of select="@id" /></xsl:attribute></xsl:if>
    <xsl:variable name="styleval" select="(descendant-or-self::*[@style])[position()=1]/@style" />
    <xsl:if test="(contains($styleval, 'keep-together') and starts-with(normalize-space(substring-after(substring-after($styleval, 'keep-together'), ':')), 'auto'))">
      <xsl:processing-instruction name="dbfo" >
        <xsl:text>keep-together="auto"</xsl:text>
      </xsl:processing-instruction>
    </xsl:if>
    <title><xsl:if test="@title"><xsl:value-of select="@title" /></xsl:if></title>
  </xsl:template>

  <xsl:template name="docma_cell_class">
    <xsl:param name="default_cls" />

    <xsl:choose>
      <xsl:when test="boolean(@class)">
        <xsl:variable name="cls_val" select="concat(' ', @class, ' ')" />
        <xsl:choose>
          <xsl:when test="contains($cls_val, ' align-left ')">
            <xsl:attribute name="align">left</xsl:attribute>
            <xsl:call-template name="docma_remove_cls" >
              <xsl:with-param name="cls_val" select="$cls_val" />
              <xsl:with-param name="remove_cls" select="' align-left '" />
              <xsl:with-param name="default_cls" select="$default_cls" />
            </xsl:call-template>
          </xsl:when>
          <xsl:when test="contains($cls_val, ' align-right ')">
            <xsl:attribute name="align">right</xsl:attribute>
            <xsl:call-template name="docma_remove_cls" >
              <xsl:with-param name="cls_val" select="$cls_val" />
              <xsl:with-param name="remove_cls" select="' align-right '" />
              <xsl:with-param name="default_cls" select="$default_cls" />
            </xsl:call-template>
          </xsl:when>
          <xsl:when test="contains($cls_val, ' align-center ')">
            <xsl:attribute name="align">center</xsl:attribute>
            <xsl:call-template name="docma_remove_cls" >
              <xsl:with-param name="cls_val" select="$cls_val" />
              <xsl:with-param name="remove_cls" select="' align-center '" />
              <xsl:with-param name="default_cls" select="$default_cls" />
            </xsl:call-template>
          </xsl:when>
          <xsl:when test="contains($cls_val, ' align-justify ')">
            <xsl:attribute name="align">justify</xsl:attribute>
            <xsl:call-template name="docma_remove_cls" >
              <xsl:with-param name="cls_val" select="$cls_val" />
              <xsl:with-param name="remove_cls" select="' align-justify '" />
              <xsl:with-param name="default_cls" select="$default_cls" />
            </xsl:call-template>
          </xsl:when>
          <xsl:when test="contains($cls_val, ' align-full ')">
            <xsl:attribute name="align">justify</xsl:attribute>
            <xsl:call-template name="docma_remove_cls" >
              <xsl:with-param name="cls_val" select="$cls_val" />
              <xsl:with-param name="remove_cls" select="' align-full '" />
              <xsl:with-param name="default_cls" select="$default_cls" />
            </xsl:call-template>
          </xsl:when>
          <xsl:otherwise>
            <xsl:attribute name="class"><xsl:value-of select="@class" /></xsl:attribute>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:when test="string-length($default_cls) > 0">
        <xsl:attribute name="class"><xsl:value-of select="$default_cls" /></xsl:attribute>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="docma_remove_cls">
    <xsl:param name="cls_val" />
    <xsl:param name="remove_cls" />
    <xsl:param name="default_cls" />

    <xsl:variable name="cls_other" select="normalize-space(concat(substring-before($cls_val, $remove_cls), ' ', substring-after($cls_val, $remove_cls)))" />
    <xsl:choose>
      <xsl:when test="string-length($cls_other) > 0">
        <xsl:attribute name="class"><xsl:value-of select="$cls_other" /></xsl:attribute>
      </xsl:when>
      <xsl:when test="string-length($default_cls) > 0">
        <xsl:attribute name="class"><xsl:value-of select="$default_cls" /></xsl:attribute>
      </xsl:when>
    </xsl:choose>
  </xsl:template>
<!--plugins_xsl-->
</xsl:stylesheet>
