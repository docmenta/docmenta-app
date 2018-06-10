/*
 * ContentCSS.java
 * 
 *  Copyright (C) 2018  Manfred Paula, http://www.docmenta.org
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;
import org.docma.util.CSSUtil;

/**
 *
 * @author MP
 */
public class ContentCSS 
{
    private static final DocmaOutputConfig DEFAULT_OUTPUT_CONFIG = new DocmaOutputConfig("default");
    
    public static void write(DocmaSession docmaSess, 
                             DocmaOutputConfig outputConf,
                             boolean exportMode,
                             boolean editMode,
                             Writer out) throws IOException 
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

        final String EDIT_BGCOL = "background-color:#F0F0F0; ";  // keep space at end!
        // table caption / formal title styles
        DocmaStyle caption_style = docmaSess.getStyleVariant("caption", variant);
        String cap_css = (caption_style == null) ? "" : caption_style.getCSS();
        String cap_align = (cap_css.toLowerCase().contains("text-align")) ? "" : "text-align:left; ";
        String cap_edit = (editMode && !cap_css.contains("background-color")) ? EDIT_BGCOL : "";
        String cap_side = "after".equalsIgnoreCase(outputConf.getTitlePlacement()) ? "bottom" : "top";
        out.write("caption { caption-side:" + cap_side + "; " + cap_align + cap_edit + cap_css + "}\n");
        out.write("p.title {" + cap_css + "}\n");

        // HTML5 figure / figcaption
        // If no style "figcaption" exists, then fall back to style "caption".
        DocmaStyle figcaption_style = docmaSess.getStyleVariant("figcaption", variant);
        String figcap_css = (figcaption_style == null) ? cap_css : figcaption_style.getCSS();
        String figcap_edit = (editMode && !figcap_css.contains("background-color")) ? EDIT_BGCOL : "";
        out.write("figcaption {" + figcap_edit + figcap_css + "}\n");
        DocmaStyle figure_style = docmaSess.getStyleVariant("figure", variant);
        if (figure_style != null) {
            out.write("figure {" + figure_style.getCSS() + "}\n");
        }
        
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
            String caption_hint = docmaSess.getI18n().getLabel("text.caption_edit_hint");
            caption_hint = caption_hint.replace('\'', ' ');
            out.write("caption:before { content:'" + caption_hint + " '; color:#707070; }\n");
            out.write("figcaption:before { content:'" + caption_hint + " '; color:#707070; }\n");
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
            
            // Show label for figure caption (figcaption) in preview-mode
            String langCode = docmaSess.getLanguageCode();
            Properties genprops = docmaSess.getGenTextProps();
            String fig_label = (genprops == null) ? null : genprops.getProperty("title|figure");
            if (fig_label == null) {
                fig_label = AutoImagePreview.getDefaultTitlePattern(langCode);
            }
            if (fig_label != null) {
                int tpos = fig_label.lastIndexOf("%t");
                if (tpos > 0) {
                    fig_label = fig_label.substring(0, tpos).replace('\'', ' ').replace('\u00A0', ' ');
                    out.write("figcaption:before { content:'" + fig_label + "'; }\n");
                }
            }
            
            // Show label for table caption in preview-mode
            String tab_label = (genprops == null) ? null : genprops.getProperty("title|table");
            if (tab_label == null) {
                tab_label = AutoCaptionPreview.getDefaultTitlePattern(langCode);
            }
            if (tab_label != null) {
                int tpos = tab_label.lastIndexOf("%t");
                if (tpos > 0) {
                    tab_label = tab_label.substring(0, tpos).replace('\'', ' ').replace('\u00A0', ' ');
                    out.write("caption:before { content:'" + tab_label + "'; }\n");
                }
            }
            
            // Show page breaks in preview
            out.write("p[style~='page-break-before:']:before { content:attr(style); background-color:#C0C0FF; }\n");
            out.write("p[style~='page-break-after:']:after { content:attr(style); background-color:#C0C0FF; }\n");
            out.write("ol[style~='page-break-before:']:before { content:attr(style); background-color:#C0C0FF; }\n");
            out.write("ol[style~='page-break-after:']:after { content:attr(style); background-color:#C0C0FF; }\n");
            out.write("ul[style~='page-break-before:']:before { content:attr(style); background-color:#C0C0FF; }\n");
            out.write("ul[style~='page-break-after:']:after { content:attr(style); background-color:#C0C0FF; }\n");
            out.write("table[style~='page-break-before:']:before { content:'page-break'; background-color:#C0C0FF; }\n");
            out.write("table[style~='page-break-after:']:after { content:'page-break'; background-color:#C0C0FF; }\n");

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
    
}
