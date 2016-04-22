/*
 * DocmaAppUtil.java
 * 
 *  Copyright (C) 2013  Manfred Paula, http://www.docmenta.org
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

import java.util.*;
import java.io.*;
import javax.servlet.http.*;

import org.docma.coreapi.*;
import org.docma.util.CSSUtil;

/**
 *
 * @author MP
 */
public class DocmaAppUtil
{
    private static final DocmaOutputConfig DEFAULT_OUTPUT_CONFIG = new DocmaOutputConfig("default");


    public static DocmaNode getNodeByPath(DocmaSession docmaSess, String path) 
    {
        String remaining = path.trim();
        if (! remaining.startsWith("/")) {
            return null;  // Valid path has to start with a slash (/).
        }
        DocmaNode currentNode = docmaSess.getRoot();
        if (remaining.equals("/")) {
            return currentNode;
        }
        // Normalize path: remove trailing slash
        // if ((remaining.length() > 1) && remaining.endsWith("/")) {
        //     if (remaining.charAt(remaining.length() - 2) != '\\') {
        //         remaining = remaining.substring(0, remaining.length() - 1);
        //     }
        // }
        boolean has_more = true;
        while (has_more) {  
            // Note: "remaining" always starts with a slash (/)
            int p_start = 1;  // Start search of next slash after leading slash.
            int p_end = remaining.length();  // Character position after node filename.
            while (p_start < p_end) {
                // Find next unescaped slash
                int p = remaining.indexOf('/', p_start);
                if (p >= 0) {
                    if (remaining.charAt(p - 1) == '\\') {  // Slash is escaped by backslash
                        // Remove backslash; continue search after slash
                        remaining = remaining.substring(0, p - 1) + remaining.substring(p);
                        p_end = remaining.length();
                        p_start = p;
                        continue;
                    }
                    p_end = p;  // Unescaped slash has been found at position p
                }
                break;
            }
            // Node name starts after leading slash to next unescaped slash
            String node_filename = remaining.substring(1, p_end);
            remaining = remaining.substring(p_end);  // remaining path
            // "remaining" is now an empty string or a string that starts with a slash
            has_more = remaining.startsWith("/") && (remaining.length() > 1);
            currentNode = currentNode.getChildByFilename(node_filename);
            if (currentNode == null) {
                return null;  // path does not exist
            }
            // if (has_more && !(currentNode.isFolder() || currentNode.isSection())) {
            //     return null;  // invalid path
            // }
        }
        return currentNode;
    }
    
    public static List listAliasesStartWith(String alias_prefix, List aliases, List outlist)
    {
        if (outlist == null) outlist = new ArrayList(aliases.size());

        int pos = Collections.binarySearch(aliases, alias_prefix);
        if (pos < 0) {  // if not found
            pos = -(pos + 1);  // insertion point
        }
        while (pos < aliases.size()) {
            String a = (String) aliases.get(pos);
            if (a.startsWith(alias_prefix)) {
                outlist.add(a);
            } else {
                break;
            }
            pos++;
        }
        return outlist;
    }

    public static String getLinkAlias(String alias_str)
    {
        if (alias_str == null) return null;
        int p = alias_str.indexOf(DocmaConstants.LINKALIAS_SEPARATOR);
        if (p < 0) return alias_str;
        return alias_str.substring(0, p);
    }
    
    public static String toValidAlias(String invalid_alias) 
    {
        StringBuilder buf = new StringBuilder(invalid_alias);
        if (buf.length() > DocmaConstants.ALIAS_MAX_LENGTH) {
            buf.setLength(DocmaConstants.ALIAS_MAX_LENGTH);
        }
        for (int i=0; i < buf.length(); i++) {
            char ch = buf.charAt(i);
            boolean is_letter = ((ch >= 'a') && (ch <= 'z')) ||
                                ((ch >= 'A') && (ch <= 'Z'));
            boolean is_digit = (ch >= '0') && (ch <= '9');
            boolean is_underscore = (ch == '_');
            if (i == 0) {
                if (! (is_letter || is_underscore)) {
                    buf.setCharAt(i, '_');
                }
            } else {
                if (! (is_letter || is_digit || is_underscore || (ch == '-'))) {
                    buf.setCharAt(i, '_');
                }
            }
        }
        return buf.toString();
    }

    public static void getImageNodesRecursive(DocmaNode rootNode, List resultList)
    {
        if (! rootNode.isChildable()) return;

        for (int i=0; i < rootNode.getChildCount(); i++) {
            DocmaNode child = rootNode.getChild(i);
            if (child.isImageContent()) {
                resultList.add(child);
            } else
            if (child.isSection() || child.isFolder()) {
                getImageNodesRecursive(child, resultList);
            }
        }
    }

    public static void exportContentCSS(DocmaSession docmaSess,
                                        DocmaOutputConfig outputConf,
                                        Writer out)
    throws IOException
    {
        writeContentCSS(docmaSess, outputConf, true, false, out);
    }

    public static void writeContentCSS(DocmaSession docmaSess, 
                                       DocmaOutputConfig outputConf,
                                       boolean exportMode,
                                       boolean editMode,
                                       Writer out)
    throws IOException
    {
        Writer out_orig;
        if (DocmaConstants.DEBUG) {
            out_orig = out;
            out = new StringWriter();
        }

        if (outputConf == null) {
            outputConf = DEFAULT_OUTPUT_CONFIG;  // Default settings
        }
        
        boolean previewMode = !(exportMode || editMode);
        
        String subformat = outputConf.getSubformat();
        boolean is_webhelp = subformat.startsWith("webhelp");
        boolean is_webhelp_1 = subformat.equals("webhelp1");
        boolean is_webhelp_new = is_webhelp && !is_webhelp_1;

        // Default style
        String variant = outputConf.getStyleVariant();
        DocmaStyle default_style = docmaSess.getStyleVariant("default", variant);
        String def_css;
        if (default_style == null) {
            def_css = DocmaConstants.STYLE_DEFAULT_CSS;
        } else {
            def_css = default_style.getCSS();
        }
        out.write("body, td, th {" + def_css + "}\n");

        // Set table defaults
        out.write("table { border-color:#000000; border-collapse:collapse; }\n");

        // Set spacing for floating blocks
        final String DEFAULT_LEFT_FLOAT = DocmaConstants.STYLE_FLOAT_LEFT_CSS;
        final String DEFAULT_RIGHT_FLOAT = DocmaConstants.STYLE_FLOAT_RIGHT_CSS;
        DocmaStyle floatleft_style = docmaSess.getStyleVariant("float_left", variant);
        String floatleft_css;
        String floatright_css;
        if (floatleft_style == null) {  // if not defined by user, use default spacing
            floatleft_css = DEFAULT_LEFT_FLOAT;
            out.write(".float_left { " + DEFAULT_LEFT_FLOAT + " }\n");  // space on right side and bottom
        } else {
            floatleft_css = floatleft_style.getCSS();
            // Note: .float_left is not written here, because it is written as user-defined style (see below) 
        }
        DocmaStyle floatright_style = docmaSess.getStyleVariant("float_right", variant);
        if (floatright_style == null) {  // if not defined by user, use default spacing
            floatright_css = DEFAULT_RIGHT_FLOAT;
            out.write(".float_right { " + DEFAULT_RIGHT_FLOAT + " }\n");  // space on left side and bottom
        } else {
            floatright_css = floatright_style.getCSS();
            // Note: .float_right is not written here, because it is written as user-defined style (see below) 
        }
        
        // Set styles for left/right-aligned and centered tables
        out.write("table[align=left] { " + floatleft_css + " }\n");
        out.write("table[align=right] { " + floatright_css + " }\n");
        out.write("table[align=center] { margin-left:auto; margin-right:auto; }\n");
        out.write("table[align=center] caption { text-align:center; }\n");
        
        // Paragraph spacing
        String para_space = outputConf.getParaSpace();
        if ((para_space == null) || para_space.equals("")) {
            para_space = "0";
        }
        String para_margin = "margin-top:" + para_space + "; margin-bottom:0; margin-right:0; margin-left:0;";
        out.write("p, table, div.normal-para { " + para_margin + " }\n");
        // Following rules are to supress space before the first paragraph within a div box or table cell.
        // Note: Use of the CSS operator > did not reliably work for Web Help / XHTML.
        out.write("div p, div div.normal-para { margin:0; }\n");
        out.write("td p, td div.normal-para { margin:0; }\n");
        out.write("th p, th div.normal-para { margin:0; }\n");
        // Following rules match the paragraphs that are NOT the first paragraph.
        out.write("p + p, div.normal-para + div.normal-para { " + para_margin + " }\n");
        out.write("div + p, div + div.normal-para { " + para_margin + " }\n");
        out.write("ol + p, ol + div.normal-para { " + para_margin + " }\n");
        out.write("ul + p, ul + div.normal-para { " + para_margin + " }\n");
        out.write("table + p, table + div.normal-para { " + para_margin + " }\n");
        out.write("blockquote + p, blockquote + div.normal-para { " + para_margin + " }\n");
        out.write("div.doc-content + div.doc-content { " + para_margin + " }\n");
        
        out.write(".toc { margin-bottom:" + para_space + "; }\n");

        // DocmaStyle para_style = docmaSess.getStyleVariant("paragraph", variant);
        // if (para_style != null) {
        //     out.write("body > p {" + para_style.getCSS() + "}\n");
        // }
        // out.write("p, div, td, th {" + def_css + "}\n");
        out.write(".align-left { text-align:left; }\n");
        out.write(".align-right { text-align:right; }\n");
        out.write(".align-center { text-align:center; }\n");
        out.write(".align-full { text-align:justify; }\n");
        out.write("img.align-right { display:block; margin-left:auto; margin-right:0; text-align:right; }\n");
        out.write("img.align-center { display:block; margin-left:auto; margin-right:auto; text-align:center; }\n");

        // Link style
        DocmaStyle link_style = docmaSess.getStyleVariant("link", variant);
        DocmaStyle link_style_visited = docmaSess.getStyleVariant("link_visited", variant);
        DocmaStyle link_style_focus = docmaSess.getStyleVariant("link_focus", variant);
        DocmaStyle link_style_hover = docmaSess.getStyleVariant("link_hover", variant);
        DocmaStyle link_style_active = docmaSess.getStyleVariant("link_active", variant);
        DocmaStyle extlink_style = docmaSess.getStyleVariant("link_external", variant);
        if (link_style != null) {
            out.write("a[href] {" + link_style.getCSS() + "}\n");
            out.write("a:link {" + link_style.getCSS() + "}\n");
        }
        if (link_style_visited != null) {
            out.write("a:visited {" + link_style_visited.getCSS() + "}\n");
        }
        if (link_style_focus != null) {
            out.write("a:focus {" + link_style_focus.getCSS() + "}\n");
        }
        if (link_style_hover != null) {
            out.write("a:hover {" + link_style_hover.getCSS() + "}\n");
        }
        if (link_style_active != null) {
            out.write("a:active {" + link_style_active.getCSS() + "}\n");
        }
        if (extlink_style != null) {
            // out.write("a[href~=://] {" + extlink_style.getCSS() + "}\n");
            out.write("a.link_external {" + extlink_style.getCSS() + "}\n");
        }
        DocmaStyle breadnode_style = docmaSess.getStyleVariant("breadcrumb_node", variant);
        if (breadnode_style != null) {
            // Note: this is required to be able to overwrite the link style settings
            out.write("a.breadcrumb_node {" + breadnode_style.getCSS() + "}\n");
        }

        // Predefined styles
        DocmaStyle strong_style = docmaSess.getStyleVariant("strong", variant);
        if (strong_style != null) {
            out.write("b, strong {" + strong_style.getCSS() + "}\n");
        }
        DocmaStyle em_style = docmaSess.getStyleVariant("emphasis", variant);
        if (em_style != null) {
            out.write("i, em {" + em_style.getCSS() + "}\n");
        }
        DocmaStyle td_style = docmaSess.getStyleVariant("table_cell", variant);
        if (td_style != null) {
            out.write("td {" + td_style.getCSS() + "}\n");
        }
        DocmaStyle th_style = docmaSess.getStyleVariant("table_header", variant);
        if (th_style != null) {
            out.write("th {" + th_style.getCSS() + "}\n");
        }
        DocmaStyle big_style = docmaSess.getStyleVariant("big", variant);
        if (big_style != null) {
            out.write("big {" + big_style.getCSS() + "}\n");
        }
        DocmaStyle tt_style = docmaSess.getStyleVariant("tt", variant);
        if (tt_style != null) {
            out.write("tt {" + tt_style.getCSS() + "}\n");
        }
        DocmaStyle pre_style = docmaSess.getStyleVariant("pre", variant);
        if (pre_style != null) {
            out.write("pre {" + pre_style.getCSS() + "}\n");
        }

        // table caption / formal title styles
        DocmaStyle caption_style = docmaSess.getStyleVariant("caption", variant);
        String cap_css = (caption_style == null) ? "" : caption_style.getCSS();
        String cap_align = (cap_css.toLowerCase().contains("text-align")) ? "" : "text-align:left; ";
        String cap_edit = editMode ? "background-color:#F0F0F0; " : "";
        String cap_side = "after".equalsIgnoreCase(outputConf.getTitlePlacement()) ? "bottom" : "top";
        out.write("caption { caption-side:" + cap_side + "; " + cap_align + cap_edit + cap_css + "}\n");
        out.write("p.title {" + cap_css + "}\n");
        
        // header styles
        DocmaStyle h1_style = docmaSess.getStyleVariant("header1", variant);
        String h1_css = (h1_style == null) ? "" : h1_style.getCSS();
        DocmaStyle ph_style = docmaSess.getStyleVariant("partheader", variant);
        if (exportMode) {
            // Note: In exported HTML, book title and part headers both use h1.
            //       In HTML generated by DocBook stylesheets, chapter and 
            //       1st-level section header both use h2, whereas 
            //       in WebHelp2 output 1st-level section headers use h3 
            //       instead of h2 (to avoid style merging effects).
            DocmaStyle h2_style = docmaSess.getStyleVariant("header2", variant);
            DocmaStyle h3_style = docmaSess.getStyleVariant("header3", variant);
            String h2_css = (h2_style == null) ? "" : h2_style.getCSS();
            String h3_css = (h3_style == null) ? "" : h3_style.getCSS();
            out.write("h1 {" + h1_css + "}\n");  // book title
            out.write("h2 {" + h2_css + "}\n");  // chapter header
            // 1st-level-section header:
            if (is_webhelp_new) {
                out.write("h3 {" + h3_css + "}\n");  
            } else {
                out.write(".section h2 {" + h3_css + "}\n");
            }
            // Remaining section-level headers:
            int hi = is_webhelp_new ? 4 : 3;
            for (int i = 4; i <= 9; i++) {
                DocmaStyle h_style = docmaSess.getStyleVariant("header" + i, variant);
                String h_css = (h_style == null) ? "" : h_style.getCSS();
                if (hi <= 6) {
                    out.write("h" + hi + " {" + h_css + "}\n");
                } else {
                    out.write("h6.hlevel" + hi + " {" + h_css + "}\n");
                }
                hi++;
            }
            // Part header
            if (ph_style != null) {
                out.write(".part h1 {" + ph_style.getCSS() + "}\n");
            }
        } else {   // Preview mode

            out.write("h1 {" + h1_css + "}\n");  // book title
            final boolean is_part = "part".equals(outputConf.getRender1stLevel());
            if (is_part) {  // part title
                String ph_css = (ph_style == null) ? "" : ph_style.getCSS();
                if (ph_css.length() == 0) {
                    ph_css = h1_css;   // if no partheader is defined, use header1 
                }
                out.write("h2 {" + ph_css + "}\n");
            }
            // Remaining chapter- and section-level headers
            int hi = is_part ? 3 : 2;
            for (int i = 2; i <= 9; i++) {
                DocmaStyle h_style = docmaSess.getStyleVariant("header" + i, variant);
                String h_css = (h_style == null) ? "" : h_style.getCSS();
                if (hi <= 6) {
                    out.write("h" + hi + " {" + h_css + "}\n");
                } else {
                    out.write("h6.hlevel" + hi + " {" + h_css + "}\n");
                }
                hi++;
            }
        }

        // list styles
        String list_indent = outputConf.getListIndent();
        out.write("ul, ol { padding-left:13pt; margin-bottom:0; margin-top:" + para_space +
                  "; margin-left:" + list_indent + " }\n");
        // list item spacing
        String li_space = outputConf.getItemSpace();
        if ((li_space == null) || (li_space.trim().equals(""))) {
            li_space = "0";
        }
        out.write("ul li { margin-bottom:0; margin-top:" + li_space + " }\n");
        out.write("ol li { margin-bottom:0; margin-top:" + li_space + " }\n");
        // Spacing of list inside list
        out.write("ul ul { margin-top:" + li_space + " }\n");
        out.write("ol ol { margin-top:" + li_space + " }\n");
        out.write("ul ol { margin-top:" + li_space + " }\n");
        out.write("ol ul { margin-top:" + li_space + " }\n");
        // styles for ordered list rendered as table (e.g. for EPUB output)
        out.write("table.ol-table { margin-top:" + para_space +
                  "; margin-left:" + list_indent + "; margin-bottom:0; border-width:0; border-spacing:0; border-collapse:collapse; }\n");
        out.write("td.ol-td-number { width:13pt; vertical-align:top; padding:" + li_space + " 0 0 0; }\n");
        out.write("td.ol-td-content { vertical-align:top; padding:" + li_space + " 0 0 0; }\n");
        // Spacing of list inside list (for ordered list which is rendered as table)
        out.write("table.ol-table table.ol-table { margin-top:0pt; }\n");
        out.write("ul table.ol-table { margin-top:0pt; }\n");
        out.write("table.ol-table ul { margin-top:0pt; }\n");

        // blockquote style (is used as workaround to indent lists)
        out.write("blockquote { padding:0; margin-bottom:0; margin-top:0;" +
                  " margin-left:" + list_indent + " }\n");

        // paragraph indent styles
        String para_indent = outputConf.getParaIndent();
        if ((para_indent != null) && (para_indent.length() > 0)) {
            String unit = CSSUtil.getSizeUnit(para_indent);
            if (para_indent.contains(".")) {
                float indent_step = CSSUtil.getSizeFloat(para_indent);
                float indent_val = indent_step;
                for (int i=1; i <= DocmaConstants.MAX_INDENT_LEVELS; i++) {
                    String val_str = CSSUtil.formatFloatSize(indent_val);
                    out.write(".indent-level" + i + " { padding-left:" + val_str + unit + " }\n");
                    out.write("table.indent-level" + i + " { padding-left:0; margin-left:" + val_str + unit + " }\n");
                    indent_val += indent_step;
                }
            } else {
                int indent_step = CSSUtil.getSizeInt(para_indent);
                int indent_val = indent_step;
                for (int i=1; i <= DocmaConstants.MAX_INDENT_LEVELS; i++) {
                    out.write(".indent-level" + i + " { padding-left:" + indent_val + unit + " }\n");
                    out.write("table.indent-level" + i + " { padding-left:0; margin-left:" + indent_val + unit + " }\n");
                    indent_val += indent_step;
                }
            }
        }

        // Footnote style
        DocmaStyle footnote_style = docmaSess.getStyleVariant("footnote", variant);
        String footnote_css = (footnote_style == null) ? "" : footnote_style.getCSS();
        if (!exportMode) {  // Highlight footnote in preview and edit mode
            if ((footnote_css.length() > 0) && !footnote_css.endsWith(";")) footnote_css += ";";
            if (! footnote_css.contains("border")) footnote_css += " border-width:1px; border-style:dashed; border-color:#808080;";
            if (! footnote_css.contains("background")) footnote_css += " background-color:#F0F0F0;";
            if (! editMode) {  // preview mode
                out.write(".footnote:before { content:\"Footnote: \"; font-weight:bold; }");
            }
        }
        out.write(".footnote {" + footnote_css + "}\n");

        // Other styles
        DocmaStyle index_style = docmaSess.getStyleVariant("indexterm", variant);
        String show_index_terms; 
        if (index_style == null) {
            show_index_terms = "span.indexterm { background-color:#FFFF80; border: 1px dashed #808080; }\n";
        } else {
            show_index_terms = "span.indexterm { " + index_style.getCSS() + " }\n";
        }
        String hide_index_terms = "span.indexterm { display:none; }\n";
        if (editMode) {
            // Styles specific for edit mode
            String anchor_edit_style = "background-color:#F0F0F0; border: 1px dashed #808080;";
            out.write("span[id] { " + anchor_edit_style + " }\n");
            out.write("cite { font-style:inherit; " + anchor_edit_style + " }\n");
            out.write(show_index_terms);

            // This fixes a bug in IE, where the caption line is not editable if empty
            String caption_hint = docmaSess.getI18().getLabel("text.caption_edit_hint");
            caption_hint = caption_hint.replace('\'', ' ');
            out.write("caption:before { content:'" + caption_hint + " '; color:#707070; }\n");
        } else {
            // Styles for preview and export mode, but not for edit mode
            out.write("cite { font-style:inherit; }\n");
            if (outputConf.isShowIndexTerms()) {
                out.write(show_index_terms);
            } else {
                out.write(hide_index_terms);
            }
        }
   
        // Styles only required for exported publication
        if (exportMode) {
            // Set default title-page styles if no user-defined styles exist
            DocmaStyle author_style = docmaSess.getStyleVariant("author", variant);
            if (author_style == null) {
                out.write(".author { " + DocmaConstants.STYLE_AUTHOR_CSS + " }\n");
            }
            DocmaStyle credit_style = docmaSess.getStyleVariant("othercredit", variant);
            if (credit_style == null) {
                out.write(".othercredit { " + DocmaConstants.STYLE_OTHERCREDIT_CSS + " }\n");
            }
            DocmaStyle subtitle_style = docmaSess.getStyleVariant("subtitle", variant);
            if (subtitle_style == null) {
                out.write(".subtitle { " + DocmaConstants.STYLE_SUBTITLE_CSS + " }\n");
            }
            
            // Set default spacing for table of contents
            out.write(".toc dl, .toc dt, .toc dd { margin-top:2pt; margin-bottom:0; padding-top:0; padding-bottom:0; }\n");
            out.write(".toc > p { margin-bottom: 4pt; }\n");
            String toc_indent = outputConf.getTocIndentWidth();
            if ((toc_indent != null) && !toc_indent.trim().equals("")) {
                out.write(".toc dd { padding-left:0; margin-left:" + toc_indent + "; }");
            }
            
            // Avoid empty line after title-line of floating images
            out.write(".figure-float br.figure-break { display:none; }\n");
        }

        // User-defined styles
        out.write(docmaSess.getCSS(variant));

        // Preview-mode styles
        if (previewMode) {
            // Preview of image titles (caption-line). See AutoImagePreview.java.
            out.write("span.title-preview { display:inline-table; }\n");
            
            // Show page breaks in preview
            out.write("p[style~='page-break-before:']:before { content:attr(style); background-color:#C0C0FF; }\n");
            out.write("p[style~='page-break-after:']:after { content:attr(style); background-color:#C0C0FF; }\n");
            out.write("ol[style~='page-break-before:']:before { content:attr(style); background-color:#C0C0FF; }\n");
            out.write("ol[style~='page-break-after:']:after { content:attr(style); background-color:#C0C0FF; }\n");
            out.write("ul[style~='page-break-before:']:before { content:attr(style); background-color:#C0C0FF; }\n");
            out.write("ul[style~='page-break-after:']:after { content:attr(style); background-color:#C0C0FF; }\n");
            out.write("table[style~='page-break-before:']:before { content:'page-break'; background-color:#C0C0FF; }\n");
            out.write("table[style~='page-break-after:']:after { content:'page-break'; background-color:#C0C0FF; }\n");

            // Show label for table caption in preview-mode
            Properties genprops = docmaSess.getGenTextProps();
            String tab_label = (genprops == null) ? null : genprops.getProperty("title|table");
            if (tab_label == null) {
                tab_label = AutoCaptionPreview.getDefaultTitlePattern(docmaSess.getLanguageCode());
            }
            if (tab_label != null) {
                int tpos = tab_label.lastIndexOf("%t");
                if (tpos > 0) {
                    tab_label = tab_label.substring(0, tpos).replace('\'', ' ').replace('\u00A0', ' ');
                    out.write("caption:before { content:'" + tab_label + "'; }\n");
                }
            }
            
            // Mark insertions and deletions
            out.write("ins { color:green; text-decoration:underline; }\n");
            out.write("del { color:red; text-decoration:line-through; }\n");
        }

        // Styles specific for preview-mode and edit-mode
        if (! exportMode) {  // if (previewMode || editMode)
            // Highlight keep-together blocks by a dashed borderline
            out.write("div.keep_together { margin:0; padding-right:4px; border-right: 2px dashed #D0D0D0; }\n");
            out.write("p + div.keep_together { " + para_margin + " }\n");
        }
        
        // Styles required for all HTML outputs that are not based on the Docbook-Stylesheets.
        if (is_webhelp_new || (! exportMode)) {  // if (previewMode || editMode || is_webhelp_new)
            // Set float spacing for images in preview/edit-mode.
            // Furthermore in WebHelp2 export this is required for img elements
            // that have no title, but have a style attribute with float value.
            // Note 1: For WebHelp2 exports, img elements with title and float
            //         are enclosed in a div with style float_left/float_right 
            //         and the style attribute is removed from the img element
            //         (i.e. the CSS rules below do not apply).
            // Note 2: For DocBook-Stylesheet based exports these CSS rules are not required, 
            //         because images are enclosed in a div with style float_left/float_right
            //         and the style attribute is removed from the img element.
            // See also CSS definition for class float_left/float_right above.
            out.write("img[style*='float: left'], img[style*='float:left'] { " + floatleft_css + " }\n");
            out.write("img[style*='float: right'], img[style*='float:right'] { " + floatright_css + " }\n");
        }

        if (DocmaConstants.DEBUG) {
            out.close();
            String outstr = ((StringWriter) out).toString();
            File fname = new File(DocmaConstants.DEBUG_DIR, "docma_content.css");
            FileWriter debugfile = new FileWriter(fname);
            debugfile.write(outstr);
            debugfile.close();
            out_orig.write(outstr);
        }
    }

    public static void getReferencedImageAliases(String html, List alias_list)
    {
        final String MATCH_IMGSRC = " src=\"image/";

        int pos = 0;
        while (true) {
            pos = html.indexOf("<img ", pos);
            if (pos < 0) break;
            pos += 4;
            int pos2 = html.indexOf(MATCH_IMGSRC, pos);
            if (pos2 < 0) break;
            int alias_start = pos2 + MATCH_IMGSRC.length();
            int alias_end = html.indexOf('"', alias_start);
            if (alias_end < 0) break;

            String img_alias = html.substring(alias_start, alias_end);
            alias_list.add(img_alias);

            pos = alias_end;
        }
    }

    public static String[] getFilterApplics(DocmaPublicationConfig pubConf,
                                            DocmaOutputConfig outConf)
    {
        if (pubConf == null) {   // no preview publication selected -> unfiltered preview
            return null;  // null means unfiltered; see DocmaOutputConfig.evaluateApplicabilityTerm(...)
        }
        String[] pub_applics = pubConf.getFilterSettingApplics();
        String[] out_applics = (outConf != null) ? outConf.getFilterSettingApplics() : new String[0];
        if ((out_applics == null) || (out_applics.length == 0)) return pub_applics;
        if ((pub_applics == null) || (pub_applics.length == 0)) return out_applics;

        // Merge both arrays to one array
        String[] applics = new String[pub_applics.length + out_applics.length];
        int idx = 0;
        for (int i=0; i < pub_applics.length; i++) applics[idx++] = pub_applics[i];
        for (int i=0; i < out_applics.length; i++) applics[idx++] = out_applics[i];

        return applics;
    }

    public static void transformImageURLs(String html,
                                          ImageURLTransformer transformer,
                                          StringBuilder out)
    {
        final String MATCH_IMGSRC = " src=\"image/";

        int copy_pos = 0;
        int pos = 0;
        while (true) {
            pos = html.indexOf("<img ", pos);
            if (pos < 0) break;
            pos += 4;
            int pos2 = html.indexOf(MATCH_IMGSRC, pos);
            if (pos2 < 0) break;
            int alias_start = pos2 + MATCH_IMGSRC.length();
            int alias_end = html.indexOf('"', alias_start);
            if (alias_end < 0) break;

            String img_alias = html.substring(alias_start, alias_end);
            String new_url = transformer.getImageURLByAlias(img_alias);
            if (new_url != null) {
                out.append(html.substring(copy_pos, pos2));
                out.append(" src=\"").append(new_url);

                copy_pos = alias_end;
            }

            pos = alias_end;
        }
        if (copy_pos < html.length()) {
            out.append(html.substring(copy_pos));
        }
    }

    /**
     * Get HTML preview of node. E.g. used for diffing of nodes.
     * 
     * @param cont
     * @param node
     * @param h_start_level
     * @param docmaSess
     * @param pub_conf
     * @param out_conf 
     */
    public static void getNodePreview(Appendable cont,
                                      DocmaNode node,
                                      int h_start_level,
                                      DocmaSession docmaSess,
                                      DocmaPublicationConfig pub_conf,
                                      DocmaOutputConfig out_conf)
    {
        String[] applics = getFilterApplics(pub_conf, out_conf);
        if (out_conf == null) out_conf = createDefaultHTMLConfig();
        out_conf.setEffectiveFilterApplics(applics);
        DocmaExportContext ctx = new DocmaExportContext(docmaSess, pub_conf, out_conf, false);
        ctx.setAutoFormatEnabled(true);  // resolve auto-format styles
        ctx.setPreviewFormattingEnabled(true);  // show titles of images, formal blocks,... in HTML preview
        ctx.setExportFormattingEnabled(false);  // preparation for html2docbook.xsl is not required
        try {
            final int MAX_NODE_COUNT = 0;  // allow preview of unlimited number of nodes
            getContentRecursive(cont, node, h_start_level, 0, ctx,
                                new HashSet(), MAX_NODE_COUNT, true, true, null, null, null, null,
                                null, false, null);
        } catch (IOException ex) {
            throw new DocRuntimeException(ex);
        } finally {
            ctx.finished();
        }
    }

    /**
     * Get HTML of node used for previewing nodes in the content workspace.
     * This is an interactive preview, where links in the returned HTML keep the 
     * session context.
     * 
     * @param cont
     * @param node
     * @param docmaSess
     * @param pub_conf
     * @param out_conf
     * @param resp
     * @param url_base
     * @param desk_id 
     */
    public static void getEditPreview(Appendable cont,
                                      DocmaNode node,
                                      DocmaSession docmaSess,
                                      DocmaPublicationConfig pub_conf,
                                      DocmaOutputConfig out_conf,
                                      HttpServletResponse resp,
                                      String url_base,
                                      String desk_id)
    {
        // Get maximum number of nodes allowed for preview:
        String max_str = docmaSess.getApplicationProperty(DocmaConstants.PROP_PREVIEW_MAX_NODES);
        int max_node_cnt = 0;
        if ((max_str != null) && (max_str.length() > 0)) {
            try {
                max_node_cnt = Integer.parseInt(max_str);
            } catch (Exception nfe) {}
        }
        
        // Prepare effective applicability used for filtering:
        String[] applics = getFilterApplics(pub_conf, out_conf);
        if (out_conf == null) out_conf = createDefaultHTMLConfig();
        out_conf.setEffectiveFilterApplics(applics);
        
        // Determine node depth relative to publication root
        int h_level;
        if (pub_conf == null) {
            h_level = node.getDepth();
        } else {
            h_level = node.getDepthRelativeTo(pub_conf.getContentRoot());
        }
        
        DocmaExportContext ctx = new DocmaExportContext(docmaSess, pub_conf, out_conf, false);
        ctx.setAutoFormatEnabled(true);  // resolve auto-format styles
        ctx.setPreviewFormattingEnabled(true);  // show titles of images, formal blocks,... in HTML preview
        ctx.setExportFormattingEnabled(false);  // preparation for html2docbook.xsl is not required
        try {
            getContentRecursive(cont, node, h_level, 0, ctx, 
                                new HashSet(), max_node_cnt, 
                                true, true, resp, url_base, desk_id, null,
                                null, false, null);
        } catch (IOException ex) {
            // Note: DocmaLimitException is not catched here.
            throw new DocRuntimeException(ex);
        } finally {
            ctx.finished();
        }
    }


    public static void getSearchReplacePreview(Appendable cont,
                                               DocmaNode node,
                                               DocmaSession docmaSess,
                                               DocmaPublicationConfig pub_conf,
                                               DocmaOutputConfig out_conf,
                                               HttpServletResponse resp,
                                               String url_base,
                                               String desk_id,
                                               String searchTerm,
                                               boolean searchIgnoreCase,
                                               boolean resolveStructureInclude,
                                               boolean resolveInlineInclude,
                                               List searchMatches)
    {
        String[] applics = getFilterApplics(pub_conf, out_conf);
        if (out_conf == null) out_conf = createDefaultHTMLConfig();
        out_conf.setEffectiveFilterApplics(applics);
        int h_level;
        if (pub_conf == null) {
            h_level = node.getDepth();
        } else {
            h_level = node.getDepthRelativeTo(pub_conf.getContentRoot());
        }
        DocmaExportContext ctx = new DocmaExportContext(docmaSess, pub_conf, out_conf, false);
        ctx.setAutoFormatEnabled(false);  // do not resolve auto-format styles
        ctx.setPreviewFormattingEnabled(false);  // not allowed for search/replace preview
        ctx.setExportFormattingEnabled(false);  // preparation for html2docbook.xsl is not required
        try {
            final int MAX_NODE_COUNT = 0;  // allow unlimited number of nodes for search/replace
            getContentRecursive(cont, node, h_level, 0, ctx,
                                new HashSet(), MAX_NODE_COUNT, 
                                resolveStructureInclude, resolveInlineInclude,
                                resp, url_base, desk_id, null, searchTerm, searchIgnoreCase,
                                searchMatches);
        } catch (IOException ex) {
            throw new DocRuntimeException(ex);
        } finally {
            ctx.finished();
        }
    }

    /**
     * Get HTML used as source for print output.
     * 
     * @param cont
     * @param node
     * @param h_start_level
     * @param exportCtx
     * @param id_set 
     * @param max_node_count
     */
    static void getNodePrintInstance(Appendable cont,
                                     DocmaNode node,
                                     int h_start_level,
                                     DocmaExportContext exportCtx,
                                     Set id_set, 
                                     int max_node_count) throws DocmaLimitException
    {
        if (id_set == null) {
            id_set = new HashSet();
        }
        // int node_depth;
        // if (pub_conf == null) {
        //     node_depth = node.getDepth();
        // } else {
        //     node_depth = node.getDepthRelativeTo(pub_conf.getContentRoot());
        // }
        exportCtx.setAutoFormatEnabled(true);  // resolve auto-format styles
        exportCtx.setPreviewFormattingEnabled(false);  // titles will be inserted by export stylesheets
        exportCtx.setExportFormattingEnabled(true);  // prepare for html2docbook.xsl
        try {
            getContentRecursive(cont, node, h_start_level, 0, exportCtx,
                                id_set, max_node_count, true, true, 
                                null, null, null, null, null, false, null);
        } catch (IOException ex) {
            // Note: DocmaLimitException is not catched here.
            throw new DocRuntimeException(ex);
        }
    }


    private static void getContentRecursive(Appendable cont,
                                            DocmaNode node,
                                            int node_depth,
                                            int level,   // recursion level; not needed?
                                            DocmaExportContext exportCtx,
                                            Set id_set,
                                            int max_node_count,
                                            boolean resolveStructureInclude,
                                            boolean resolveInlineInclude,
                                            HttpServletResponse resp,
                                            String url_base,
                                            String desk_id,
                                            String overwrite_title,
                                            String searchTerm,
                                            boolean searchIgnoreCase,
                                            List searchMatches) 
    throws IOException, DocmaLimitException
    {
        // For the root-node of the publication, the node_depth is 0.
        // For the first level of the publication (preface, parts/chapter, appendix)
        // the node_depth is 1, and so on.
        // The node_depth also defines which header tag is used for the titles.
        // For node_depth 0, the h1 tag is used, for node_depth 1, the h2 tag 
        // is used and so on.
        DocmaOutputConfig out_conf = exportCtx.getOutputConfig();
        ExportLog export_log = exportCtx.getExportLog();
        boolean mode_publish = (resp == null);
        boolean mode_search = (searchTerm != null);
        boolean mode_edit = !(mode_publish || mode_search);

        String node_id = node.getId();
        if (id_set.contains(node_id)) {
            if (export_log != null) {
                String node_name = node.getAlias();
                if (node_name == null) {
                    node_name = node_id; 
                }
                LogUtil.addError(export_log, node, "publication.export.node_included_twice", node_name);
            }
            return;
        }
        // id_set.add(node_id);
        if ((max_node_count > 0) && (id_set.size() >= max_node_count)) {
            throw new DocmaLimitException("Maximum number of nodes reached: " + max_node_count);
        }

        if (node.isContent()) {
            String node_alias = node.getLinkAlias();
            if (mode_publish) {
                cont.append(getDivStart(node_alias, null, node.getTitleEntityEncoded()));
            } else {
                cont.append(getDivStart_Preview(node_id, node_alias, out_conf, mode_edit));
            }

            cont.append(ContentResolver.getContentRecursive(node, id_set, exportCtx,
                                                            resolveInlineInclude, 0,
                                                            searchTerm, searchIgnoreCase, searchMatches));
            cont.append(getDivEnd());
        } else
        if (node.isSection()) {
            id_set.add(node_id);

            boolean is_parts = (out_conf != null) && "part".equals(out_conf.getRender1stLevel());
            boolean omit_single_title = (out_conf != null) && out_conf.isOmitSingleTitle();
            boolean omit_title = omit_single_title && (overwrite_title != null) && overwrite_title.equals("");
            if (! omit_title) {
                String node_alias = node.getLinkAlias();
                String styleclass = null;
                String firstclass = (out_conf != null) ? ("doc-" + out_conf.getRender1stLevel()) : "doc-chapter";
                if (node_depth == 1) {
                    styleclass = firstclass;
                } else if ((node_depth == 2) && is_parts) {
                    styleclass = "doc-chapter";
                } else if (node_depth >= 2) {
                    styleclass = "doc-section";
                }
                
                cont.append(getDivStart(node_alias, styleclass, null));
                String n_title;
                if (overwrite_title == null) {
                    if (mode_search) {
                        n_title = ContentResolver.getTitleWithSearchMarks(node, searchTerm, searchIgnoreCase, searchMatches);
                    } else {
                        n_title = node.getTitleEntityEncoded();
                    }
                } else {
                    n_title = overwrite_title;
                }
                // Note that in the print instance, the titles of first level
                // (preface, parts/chapter, appendix) are always rendered with
                // the h1 tag. The titles of the second level with h2, and so on.
                // In the exported HTML the tags may be transformed.
                int h_level = node_depth + 1;
                if (h_level <= 6) {
                    cont.append("<h" + h_level + ">");
                    cont.append(n_title);
                    cont.append("</h" + h_level + ">");
                } else {
                    cont.append("<h6 class=\"hlevel" + h_level + "\">");
                    cont.append(n_title);
                    cont.append("</h6>");
                }
            }

            // Determine all applicable content and section nodes
            List cont_nodes = new LinkedList();
            List sect_nodes = new LinkedList();
            int max = node.getChildCount();
            for (int i=0; i < max; i++) {
                DocmaNode child_node = node.getChild(i);
                if (child_node.isHTMLContent() || child_node.isContentIncludeReference()) {
                    if (isApplicable(child_node, out_conf)) cont_nodes.add(child_node);
                } else
                if (child_node.isSection() || child_node.isSectionIncludeReference()) {
                    if (isApplicable(child_node, out_conf)) sect_nodes.add(child_node);
                }
            }

            int content_count = cont_nodes.size();
            boolean part_intro = is_parts && (node_depth == 1) && (content_count > 0);
            if (part_intro) cont.append("<div class=\"doc-partintro\">");
            Iterator it = cont_nodes.iterator();
            while (it.hasNext()) {
                DocmaNode child_node = (DocmaNode) it.next();
                if (child_node.isContent()) {
                    String child_alias = child_node.getLinkAlias();
                    if (mode_publish) {
                        cont.append(getDivStart(child_alias, null, child_node.getTitleEntityEncoded()));
                    } else {
                        boolean allow_edit = mode_edit;
                        cont.append(getDivStart_Preview(child_node.getId(), child_alias, out_conf, allow_edit));
                    }
                    cont.append(ContentResolver.getContentRecursive(
                        child_node, id_set, exportCtx, resolveInlineInclude,
                        0, searchTerm, searchIgnoreCase, searchMatches));
                    cont.append(getDivEnd());
                } else
                if (child_node.isContentIncludeReference()) {
                    getContentRecursive(cont, child_node, node_depth, level,
                                        exportCtx, id_set, max_node_count,
                                        resolveStructureInclude, resolveInlineInclude,
                                        resp, url_base, desk_id, null,
                                        searchTerm, searchIgnoreCase, searchMatches);
                }
            }
            if (part_intro) cont.append("</div>");
            
            if ((content_count == 0) && sect_nodes.isEmpty() && 
                (out_conf != null) && "pdf".equals(out_conf.getFormat())) {
                // Fix problem of PDF output: If many subsequent sections
                // have no content, then the headers may run out of page,  
                // because no page-break is inserted between the section headers.
                // Therefore, empty paragraph is inserted to avoid this problem.
                cont.append("<p></p>");
            }

            if (omit_single_title && (content_count == 0) && (sect_nodes.size() == 1)) {
                overwrite_title = "";  // omit title of single sub-section
            } else {
                overwrite_title = null;
            }

            it = sect_nodes.iterator();
            while (it.hasNext()) {
                DocmaNode child_node = (DocmaNode) it.next();
                getContentRecursive(cont, child_node, node_depth + 1,
                                    level + 1, exportCtx, id_set, max_node_count,
                                    resolveStructureInclude, resolveInlineInclude,
                                    resp, url_base, desk_id, overwrite_title,
                                    searchTerm, searchIgnoreCase, searchMatches);
            }

            if (! omit_title) {
                cont.append(getDivEnd());
            }
        }  // if node.isSection()
        else
        if (node.isImageIncludeReference()) {
            String tit = node.getTitleEntityEncoded();
            String alias = node.getReferenceTarget();
            if ((tit == null) || tit.equals("")) {
                DocmaNode target_node = node.getReferencedNode();
                if (target_node != null) tit = target_node.getTitleEntityEncoded();
            }
            if (tit == null) tit = "";
            tit = tit.replace('"', '\'').replace('\\', '/');
            cont.append("<p><img src=\"image/").append(alias).append("\" title=\"")
                .append(tit).append("\" alt=\"").append(tit).append("\" /></p>");
        } else
        if (node.isIncludeReference() && resolveStructureInclude) {
            DocmaNode target_node = node.getReferencedNode();
            if (target_node != null) {
                // String alias = target_node.getAlias();
                String inc_title = overwrite_title;
                if ((overwrite_title == null) && target_node.isSection()) {
                    inc_title = node.getTitleEntityEncoded();
                    if (mode_search && (inc_title != null)) {
                        inc_title = ContentResolver.getTitleWithSearchMarks(node, searchTerm, searchIgnoreCase, searchMatches);
                    }
                    if ((inc_title != null) && inc_title.trim().equals("")) {
                        inc_title = null;
                    }
                }
                getContentRecursive(cont, target_node, node_depth, level,
                                    exportCtx, id_set, max_node_count,
                                    resolveStructureInclude, resolveInlineInclude, 
                                    resp, url_base, desk_id, inc_title,
                                    searchTerm, searchIgnoreCase, searchMatches);
            }
        }
    }

    static boolean isApplicable(DocmaNode nd, DocmaOutputConfig out_conf)
    {
        if (out_conf == null) {
            return true;  // no configuration given -> unfiltered
        } else {
            return out_conf.evaluateApplicabilityTerm(nd.getApplicability());
        }
    }

    private static String getDivStart_Preview(String node_id,
                                              String node_alias,
                                              DocmaOutputConfig out_conf,
                                              // HttpServletResponse resp,
                                              // String url_base,
                                              // String desk_id,
                                              boolean allow_edit)
    {
        // HTML Preview mode: wrap content; allow editing of content by double-click
        String id_att = (node_alias == null) ? "" : "id=\"" + node_alias + "\"";
        String dbl_click;
        if (allow_edit) {
            // String edit_url = resp.encodeURL(url_base + "edit.zul?nodeid=" + node_id + "&desk=" + desk_id);
            String edit_action = "parent.openEditWin('" + node_id + "');";
            dbl_click = "ondblclick=\"" + edit_action + "\"";
        } else {
            dbl_click = "";
        }

        String para_space = (out_conf != null) ? out_conf.getParaSpace() : "0px";
        return "<div " + id_att +  // " onclick=\"\"" +
               " style=\"border:1px solid transparent; padding:0px 0px 0px 2px; margin:0px 0px " +
               para_space + " 0px; cursor:pointer;\" " + dbl_click +
               " onmouseover=\"this.style.border = '1px dashed black';\" onmouseout=\"this.style.border = '1px solid transparent';\" >";
    }

    static String getDivStart(String node_alias, String styleclass, String node_title)
    {
        // Publishing mode: wrap sections (chapter,...) and content.
        // If alias exists, set it as id attribute.
        // A title should only be provided for content nodes. For sections the
        // title will be supplied by a child element.
        String id_att = (node_alias == null) ? "" : "id=\"" + node_alias + "\" ";
        String ttl = (node_title != null) ? "title=\"" + node_title + "\" " : "";
        if (styleclass == null) styleclass = "doc-content";
        return "<div class=\"" + styleclass + "\" " + id_att + ttl + ">";
    }

    static String getDivEnd()
    {
        return "</div>";
    }

    public static DocmaOutputConfig createDefaultHTMLConfig()
    {
        DocmaOutputConfig defaultConfig = new DocmaOutputConfig("___docma_default_html_config___");
        defaultConfig.setFormat("html");
        defaultConfig.setFilterSetting(null);  // null means unfiltered
        return defaultConfig;
    }

    public static void setWebHelpDefaults(DocmaOutputConfig out_conf)
    {
        out_conf.setFormat("html");
        out_conf.setSubformat("webhelp2");
        out_conf.setHtmlSingleFile(false);
        out_conf.setHtmlSeparateFileLevel(2);
    }

    public static void setEPUBDefaults(DocmaOutputConfig out_conf)
    {
        out_conf.setFormat("html");
        out_conf.setSubformat("epub");
        out_conf.setHtmlSingleFile(false);
        out_conf.setToc(true);
        out_conf.setHtmlSeparateTOC(true);
        out_conf.setSectionTocDepth(2);
        // out_conf.setHtmlSeparateEachTable(true);
        out_conf.setHtmlSeparateFileLevel(1);
    }

    public static class DocmaNodeAliasComparator implements Comparator
    {

        public int compare(Object node1, Object node2)
        {
            String a1 = ((DocmaNode) node1).getAlias();
            String a2 = ((DocmaNode) node2).getAlias();
            if (a1 == null) {
                if (a2 == null) return 0;
                else return -1;
            }
            if (a2 == null) return 1;
            return a1.compareToIgnoreCase(a2);
        }
    }

    public static class NodeInfoAliasComparator implements Comparator
    {

        public int compare(Object node1, Object node2)
        {
            String a1 = ((NodeInfo) node1).getAlias();
            String a2 = ((NodeInfo) node2).getAlias();
            if (a1 == null) {
                if (a2 == null) return 0;
                else return -1;
            }
            if (a2 == null) return 1;
            return a1.compareToIgnoreCase(a2);
        }
    }

}
