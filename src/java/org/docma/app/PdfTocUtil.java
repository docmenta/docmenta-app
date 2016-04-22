/*
 * PdfTocUtil.java
 * 
 *  Copyright (C) 2015  Manfred Paula, http://www.docmenta.org
 *   
 *  This file is part of Docmenta. Docmenta is free software: you can 
 *  redistribute it and/or modify it under the terms of the GNU Lesser 
 *  General Public License as published by the Free Software Foundation, 
 *  either version 3 of the License, or (at your option) any later version.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Docmenta.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.docma.app;

import org.docma.util.*;
import java.io.*;

/**
 *
 * @author MP
 */
public class PdfTocUtil 
{
    private static String xslTemplate = null;
    
    public static void writeCustomTocLineXsl(StringBuilder buf, 
                                             File configDir,
                                             DocmaOutputConfig outConfig,
                                             DocmaStyle[] styles)
    {
        addCustomTocHeader(buf, FormattingEngine.getStyle(styles, "toc_header", outConfig));
        addCustomTocLineProperties(buf, FormattingEngine.getStyle(styles, "toc_line", outConfig));
        
        String named_list = outConfig.getTocNamedLabels();
        named_list = (named_list == null) ? "" : named_list.trim().toLowerCase();
        boolean isNamedPart = named_list.contains("part");
        boolean isNamedChap = named_list.contains("chapter");  // named chapter and appendix
        boolean isNamed = isNamedPart || isNamedChap;
        String namedCondition = "false()";  // do not use named labels by default
        if (isNamedPart && isNamedChap) {
            namedCondition = "self::part or self::chapter or self::appendix";
        } else 
        if (isNamedPart) {
            namedCondition = "self::part";
        } else
        if (isNamedChap) {
            namedCondition = "self::chapter or self::appendix";
        }
        
        DocmaStyle line_style_part = FormattingEngine.getStyle(styles, "toc_line_part", outConfig);
        DocmaStyle line_style_chapter = FormattingEngine.getStyle(styles, "toc_line_chapter", outConfig);
        DocmaStyle line_style_appendix = FormattingEngine.getStyle(styles, "toc_line_appendix", outConfig);
        DocmaStyle line_style_preface = FormattingEngine.getStyle(styles, "toc_line_preface", outConfig);
        
        final int MAX_LINE_LEVELS = 6;
        boolean has_level_style = false;
        DocmaStyle[] level_styles = new DocmaStyle[MAX_LINE_LEVELS];
        for (int i=1; i <= MAX_LINE_LEVELS; i++) {
            level_styles[i-1] = FormattingEngine.getStyle(styles, "toc_line_section" + i, outConfig);
            if (level_styles[i-1] != null) {
                has_level_style = true;
            }
        }

        boolean has_custom_style = has_level_style || 
                                   (line_style_part != null) ||
                                   (line_style_chapter != null) ||
                                   (line_style_preface != null) ||
                                   (line_style_appendix != null);
        boolean requires_custom_xsl = isNamed || has_custom_style;
        if (! requires_custom_xsl) {
            return;  // nothing more to do
        }
        
        String line_templ = getTemplate(configDir);
        if (line_templ == null) {
            return;   // error reading template -> use default toc-line template
        }
        
        final String BLOCK_START_PATTERN = "<!--start_line_block-->";
        final String BLOCK_END_PATTERN = "<!--end_line_block-->";
        int block_start = line_templ.indexOf(BLOCK_START_PATTERN);
        if (block_start < 0) {
            Log.error("Invalid ToC-line template: Could not find block start.");
            return;
        }
        int block_end = line_templ.indexOf(BLOCK_END_PATTERN, block_start);
        if (block_end < 0) {
            Log.error("Invalid ToC-line template: Could not find block end.");
            return;
        }
        
        String block_templ = line_templ.substring(block_start + BLOCK_START_PATTERN.length(), block_end);
        final String CONDITION_PATTERN = "###named_label_condition###";
        block_templ = block_templ.replace(CONDITION_PATTERN, namedCondition);

        StringBuilder custom_buf = new StringBuilder();
        if (has_custom_style) {
            custom_buf.append("<xsl:choose>");
            writeWhenCustomStyleXsl(custom_buf, "self::part", block_templ, line_style_part);
            writeWhenCustomStyleXsl(custom_buf, "self::chapter", block_templ, line_style_chapter);
            writeWhenCustomStyleXsl(custom_buf, "self::preface", block_templ, line_style_preface);
            writeWhenCustomStyleXsl(custom_buf, "self::appendix", block_templ, line_style_appendix);
            if (has_level_style) {
                for (int i=0; i < MAX_LINE_LEVELS; i++) {
                    writeWhenCustomStyleXsl(custom_buf, "count(ancestor::section)=" + i, block_templ, level_styles[i]);
                }
            }
            custom_buf.append("<xsl:otherwise>");
        }
        custom_buf.append(getLineBlockXsl(block_templ, null));
        if (has_custom_style) {
            custom_buf.append("</xsl:otherwise>");
            custom_buf.append("</xsl:choose>");
        }
        
        buf.append(line_templ.substring(0, block_start));
        buf.append(custom_buf);
        buf.append(line_templ.substring(block_end + BLOCK_END_PATTERN.length()));
    }
    
    private static void writeWhenCustomStyleXsl(StringBuilder buf, 
                                                String when_condition, 
                                                String block_templ, 
                                                DocmaStyle line_style) 
    {
        if (line_style == null) {
            return;
        }
        buf.append("<xsl:when test=\"").append(when_condition).append("\">");
        buf.append(getLineBlockXsl(block_templ, line_style));
        buf.append("</xsl:when>");
    }
    
    private static String getLineBlockXsl(String block_templ, DocmaStyle line_style) 
    {
        String atts = "";
        if (line_style != null) {
            StringBuilder att_buf = new StringBuilder();
            FormattingEngine.getFOFromCSS(att_buf, line_style.getCSS(), true);
            atts = att_buf.toString();
        }
        final String BLOCK_ATTS_PATTERN = "<!--line_block_attributes-->";
        return block_templ.replace(BLOCK_ATTS_PATTERN, atts);
    }

    private static void addCustomTocHeader(StringBuilder buf, DocmaStyle toc_header)
    {
        if (toc_header == null) {
            return;
        }
        buf.append("<xsl:template name=\"table.of.contents.titlepage\" priority=\"1\">");
        buf.append("<fo:block>");
        FormattingEngine.getFOFromCSS(buf, toc_header.getCSS(), true);
        buf.append("<xsl:call-template name=\"gentext\">");
        buf.append(" <xsl:with-param name=\"key\" select=\"'TableofContents'\"/>");
        buf.append("</xsl:call-template>");
        buf.append("</fo:block>");
        buf.append("</xsl:template>\n");
    }

    private static void addCustomTocLineProperties(StringBuilder buf, DocmaStyle toc_line)
    {
        if (toc_line == null) {
            return;
        }
        buf.append("<xsl:attribute-set name=\"toc.line.properties\">\n");
        buf.append("<xsl:attribute name=\"text-align-last\">justify</xsl:attribute>");
        buf.append("<xsl:attribute name=\"text-align\">start</xsl:attribute>");
        buf.append("<xsl:attribute name=\"end-indent\"><xsl:value-of select=\"concat($toc.indent.width, 'pt')\"/></xsl:attribute>");
        buf.append("<xsl:attribute name=\"last-line-end-indent\"><xsl:value-of select=\"concat('-', $toc.indent.width, 'pt')\"/></xsl:attribute>");
        FormattingEngine.getFOFromCSS(buf, toc_line.getCSS(), true);
        buf.append("</xsl:attribute-set>\n");
    }

    private static synchronized String getTemplate(File configDir)
    {
        if (xslTemplate == null) {
            File f = new File(configDir, "tocline_template_pdf.xsl");
            if (f.exists()) {
                try {
                    xslTemplate = DocmaUtil.readFileToString(f);
                } catch (IOException ex) {
                    ex.printStackTrace();  // ignore; xslTemplate remains null
                }
            }
            if (xslTemplate == null) {
                Log.error("Failed to read ToC-line template: " + f.getAbsolutePath());
            }
        }
        return xslTemplate;
    }

}
