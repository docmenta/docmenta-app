/*
 * PdfHeaderFooterUtil.java
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
import java.io.IOException;
import java.io.FileWriter;
import java.io.File;

import org.docma.util.Log;
import org.docma.util.DocmaUtil;
import org.docma.coreapi.ExportLog;

/**
 *
 * @author MP
 */
public class PdfHeaderFooterUtil 
{
    private static final int ROW_CNT = 2;
    private static final int COL_CNT = 3;
    
    private static String xslTemplate = null;
    
    public static String getHeaderFooterXsl(DocmaExportContext exportCtx, 
                                            ImageURLTransformer imgURLTransformer, 
                                            DocmaStyle[] styles)
    {
        StringBuilder buf = new StringBuilder(4096);
        writeHeaderFooterXsl(buf, exportCtx, imgURLTransformer, styles);
        String xsl = buf.toString();
        // Write debugging output
        if (DocmaConstants.DEBUG) {
            try {
                FileWriter fw = new FileWriter(new File(DocmaConstants.DEBUG_DIR, "custom_headerfooter.xsl"));
                fw.write(xsl); 
                fw.close();
            } catch (Exception ex) {}
        }
        return xsl;
    }
    
    public static void writeHeaderFooterXsl(Appendable buf, 
                                            DocmaExportContext exportCtx, 
                                            ImageURLTransformer imgUrlTransformer, 
                                            DocmaStyle[] styles) 
    {
        DocmaOutputConfig outConf = exportCtx.getOutputConfig();
        String[] pageTypes = outConf.getPdfCustomHeaderFooterPageTypes();
        SortedMap<String, List<String>> map = new TreeMap<String, List<String>>();
        for (int i = 0; i < pageTypes.length; i++) {
            String pt = pageTypes[i];  // pagetype; for example "body_odd"
            int pos = pt.indexOf('_');
            if (pos < 0) {
                Log.error("Invalid page type:" + pt);
                continue;
            }
            String pageClass = pt.substring(0, pos);  // for example "body"
            String sequence = pt.substring(pos + 1);  // for example "odd"
            
            List<String> seq_list = map.get(pageClass);
            if (seq_list == null) {
                seq_list = new ArrayList<String>();
                map.put(pageClass, seq_list);
            }
            if (! seq_list.contains(sequence)) {
                seq_list.add(sequence);
            }
        }
        try {
            String hwidths = outConf.getPdfHeaderWidths();
            String fwidths = outConf.getPdfFooterWidths();
            // int[] header_widths = parseColumnWidths(hwidths);
            // int[] footer_widths = parseColumnWidths(fwidths);
            if ((hwidths != null) && (hwidths.length() > 0)) {
                buf.append("<xsl:param name=\"header.column.widths\">").append(hwidths).append("</xsl:param>\n");
            }
            if ((fwidths != null) && (fwidths.length() > 0)) {
                buf.append("<xsl:param name=\"footer.column.widths\">").append(fwidths).append("</xsl:param>\n");
            }
            
            // Helper template
            buf.append("<xsl:template match=\"pubdate\" mode=\"docma.headerfooter.mode\">")
               .append("<xsl:apply-templates />")
               .append("</xsl:template>\n");

            String xsl_temp = getTemplate(exportCtx);
            if (xsl_temp != null) {
                StringBuilder header_rows = new StringBuilder();
                StringBuilder footer_rows = new StringBuilder();
                StringBuilder header_flag = new StringBuilder();
                StringBuilder footer_flag = new StringBuilder();
                writeXsl(header_rows, header_flag, map, "header", /*header_widths,*/ exportCtx, imgUrlTransformer, styles);
                writeXsl(footer_rows, footer_flag, map, "footer", /*footer_widths,*/ exportCtx, imgUrlTransformer, styles);
                xsl_temp = xsl_temp.replace("###header_flag###", header_flag)
                                   .replace("###footer_flag###", footer_flag)
                                   .replace("###header_table_rows###", header_rows)
                                   .replace("###footer_table_rows###", footer_rows);
                buf.append(xsl_temp);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    
    /* --------------  Private methods  --------------- */
    
    private static int[] parseColumnWidths(String widths) 
    {
        int[] res = new int[COL_CNT];
        if (widths == null) {
            widths = "";
        }
        StringTokenizer st = new StringTokenizer(widths);
        for (int i=0; i < res.length; i++) {
            if (st.hasMoreTokens()) {
                try { 
                    res[i] = Integer.parseInt(st.nextToken());
                    continue;
                } catch (NumberFormatException nfe) {}
            }
            res[i] = 1;   // default if parsing error or less than 3 tokens
        }
        return res;
    }
    
    private static synchronized String getTemplate(DocmaExportContext exportCtx)
    {
        if (xslTemplate == null) {
            String config_path = exportCtx.getApplicationProperty(DocmaConstants.PROP_DOCMA_XSL_PATH);
            File f = new File(config_path, "custom_header_footer_pdf.xsl");
            if (f.exists()) {
                try {
                    xslTemplate = DocmaUtil.readFileToString(f);
                } catch (IOException ex) {
                    ex.printStackTrace();  // ignore; xslTemplate remains null
                }
            }
            if (xslTemplate == null) {
                ExportLog exp_log = exportCtx.getExportLog();
                if (exp_log != null) {
                    exp_log.errorMsg("Failed to read Header/Footer Template: " + f.getAbsolutePath());
                }
            }
        }
        return xslTemplate;
    }
    
    private static void writeXsl(Appendable buf,    // output table rows
                                 Appendable flg,    // output flag to turn display of table on/off
                                 SortedMap<String, List<String>> pageClassMap,
                                 String region, 
                                 // int[] columnWidths,
                                 DocmaExportContext exportCtx, 
                                 ImageURLTransformer imgUrlTransformer, 
                                 DocmaStyle[] styles) 
    throws IOException
    {
        DocmaOutputConfig outConf = exportCtx.getOutputConfig();
        // Name of default style
        DocmaStyle default_style = FormattingEngine.getStyle(styles, "page_" + region + "_cell", outConf);
        
        final String DISP_ALIGN = region.equals("footer") ? "after" : "before";
        
        if (pageClassMap.isEmpty()) {
            return;
        }
        
        boolean double_sided = outConf.isPdfDoubleSided();
        
        // choose pageclass: titlepage, lot, front, body, back, index
        buf.append(" <xsl:choose>\n");
        flg.append(" <xsl:choose>\n");
        Iterator<String> it = pageClassMap.keySet().iterator();
        while (it.hasNext()) {
            String pageCls = it.next();
            String cls_condition = "$pageclass = '" + pageCls + "'";
            if (pageCls.equals("body")) {
                if (! pageClassMap.containsKey("titlepage")) cls_condition += " or $pageclass = 'titlepage'";
                if (! pageClassMap.containsKey("lot")) cls_condition += " or $pageclass = 'lot'";
                if (! pageClassMap.containsKey("front")) cls_condition += " or $pageclass = 'front'";
                if (! pageClassMap.containsKey("back")) cls_condition += " or $pageclass = 'back'";
                if (! pageClassMap.containsKey("index")) cls_condition += " or $pageclass = 'index'";
            }
            buf.append("  <xsl:when test=\"").append(cls_condition).append("\">\n");
            flg.append("  <xsl:when test=\"").append(cls_condition).append("\">\n");

            // Choose sequence: first, odd, even, blank
            List<String> seq_list = pageClassMap.get(pageCls);
            if (seq_list.isEmpty()) {  // normally this should not occur
                seq_list.add("odd");   // fallback to odd 
            }
            buf.append("   <xsl:choose>\n");
            flg.append("   <xsl:choose>\n");
            final String[] all_sequences = new String[] {"first", "odd", "even", "blank"};
            for (int i=0; i < all_sequences.length; i++) {
                String seq = all_sequences[i];
                String condition = "$sequence = '" + seq + "'";
                if (seq.equals("odd")) {
                    // Non-existing page type for even/first/blank pages 
                    // means that configuration of odd pages shall be used.
                    if (! seq_list.contains("first")) condition += " or $sequence = 'first'";
                    if (! double_sided) {
                        if (! seq_list.contains("even")) condition += " or $sequence = 'even'";
                        if (! seq_list.contains("blank")) condition += " or $sequence = 'blank'";
                    }
                }
                String pageType = pageCls + "_" + seq;
                boolean mirrored = false;
                if (double_sided && (seq.equals("even") || seq.equals("blank"))) {
                    if (! seq_list.contains(seq)) {   // mirrored header/footer of odd pages shall be used
                        pageType = pageCls + "_odd";
                        mirrored = true;
                    }
                }
                
                if (! (mirrored || seq_list.contains(seq))) {
                    continue;
                }
                buf.append("    <xsl:when test=\"").append(condition).append("\">\n");
                flg.append("    <xsl:when test=\"").append(condition).append("\">\n");
                
                int max_spanned_row = 0;
                boolean[][] spanned_cell = new boolean[ROW_CNT][COL_CNT];
                for (int row = 0; row < ROW_CNT; row++) {
                    for (int col = 0; col < COL_CNT; col++) {
                        spanned_cell[row][col] = false;
                    }
                }
                boolean empty_table = true;
                for (int row = 1; row <= ROW_CNT; row++) {
                    boolean has_content = false;
                    StringBuilder rowbuf = new StringBuilder("<fo:table-row>");
                    for (int col = 1; col <= COL_CNT; col++) {  // column: left, center, right
                        if (spanned_cell[row - 1][col - 1]) {  // skip spanned cells
                            continue;
                        }
                        // Get content of cell
                        int content_col = mirrored ? (COL_CNT - col + 1) : col;
                        String cont = outConf.getPdfHeaderFooterContent(pageType, region, content_col, row);
                        if ((cont != null) && !cont.equals("")) {
                            has_content = true;
                        }
                        int rowspan = extractRowspan(cont);
                        int colspan = extractColspan(cont);
                        rowbuf.append("<fo:table-cell text-align=\"")
                              .append((col == 1) ? "start" : ((col == COL_CNT) ? "end" : "center"))
                              .append("\" display-align=\"").append(DISP_ALIGN).append("\" ");
                        if (rowspan > 1) {
                            if (row + rowspan - 1 > ROW_CNT) {
                                rowspan = ROW_CNT - row + 1;
                            }
                            max_spanned_row = Math.max(max_spanned_row, row + rowspan - 1);
                            rowbuf.append(" number-rows-spanned=\"").append(rowspan).append("\" ");
                            for (int r = row + 1; r < row + rowspan; r++) {
                                spanned_cell[r - 1][col - 1] = true; 
                            }
                        } else {
                            rowspan = 1;
                        }
                        if (colspan > 1) {
                            if (col + colspan - 1 > COL_CNT) { 
                                colspan = COL_CNT - col + 1;
                            }
                            rowbuf.append(" number-columns-spanned=\"").append(colspan).append("\" ");
                            for (int r = row; r < row + rowspan; r++) {
                                for (int c = col + 1; c < col + colspan; c++) {
                                    spanned_cell[r - 1][c - 1] = true; 
                                }
                            }
                        } else {
                            colspan = 1;
                        }
                        rowbuf.append(">");
                        DocmaStyle cell_style = default_style;
                        String custom_style_id = extractCustomStyleId(cont);
                        if (custom_style_id != null) {
                            cell_style = FormattingEngine.getStyle(styles, custom_style_id, outConf);
                        }
                        writeCellStyleAttributes(rowbuf, cell_style, mirrored);
                        rowbuf.append("<fo:block margin=\"0pt\" padding=\"0pt\" >");
                        writeUserDefinedContent(rowbuf, cont, exportCtx, imgUrlTransformer);
                        rowbuf.append("</fo:block>");
                        rowbuf.append("</fo:table-cell>");
                    }  // end of column loop
                    rowbuf.append("</fo:table-row>");
                    if (has_content || (row <= max_spanned_row)) {
                        // Output row only if user has defined content for this 
                        // row or if row is spanned. If an empty row shall not 
                        // be suppressed, then user can enter the empty string
                        // literal '' in one of the cells.
                        buf.append(rowbuf);
                        empty_table = false;
                    }
                } // end of row loop
                flg.append("<xsl:value-of select=\"").append(empty_table ? "''" : "'on'").append("\" />");
                
                buf.append("    </xsl:when>\n");   // end of when: sequence
                flg.append("    </xsl:when>\n");  // end of when: sequence
            }  // end of for loop for sequence list
            
            buf.append("   </xsl:choose>\n");   // end of choose: sequence 
            flg.append("   </xsl:choose>\n");   // end of choose: sequence 
            buf.append("  </xsl:when>\n");  // end of when: pageclass
            flg.append("  </xsl:when>\n");  // end of when: pageclass
        }  // end of while loop for pageclass
        buf.append(" </xsl:choose>\n");  // end of choose: pageclass 
        flg.append(" </xsl:choose>\n");  // end of choose: pageclass 
    }
    
//    private static void writeEmptyTableRow(Appendable buf) throws IOException
//    {
//        buf.append("<fo:table-row><fo:table-cell><fo:block></fo:block></fo:table-cell></fo:table-row>");
//    }
    
    private static void writeCellStyleAttributes(Appendable buf, 
                                                 DocmaStyle cell_style, 
                                                 boolean mirrored) throws IOException
    {
        String css = (cell_style == null) ? null : cell_style.getCSS();
        if ((css == null) || ((css.indexOf("margin") < 0) && (css.indexOf("MARGIN") < 0))) {
            // Set margin due to FOP problem: if margin is not set, then
            // left padding of a block within a table cell is ignored.
            buf.append("<xsl:attribute name=\"margin\">0pt</xsl:attribute>");
        }
        if ((css == null) || ((css.indexOf("padding") < 0) && (css.indexOf("PADDING") < 0))) {
            // If user has not set any padding, then set padding to 0, just  
            // to be sure that no extra space is added.
            buf.append("<xsl:attribute name=\"padding\">0pt</xsl:attribute>");
        }
        if ((css != null) && !css.equals("")) {
            StringBuilder cell_fo = new StringBuilder();
            FormattingEngine.getFOFromCSS(cell_fo, css, true, mirrored);
            buf.append(cell_fo);
        }
    }

    private static String extractParam(String content, String param_name)
    {
        if (content == null) {
            return null;
        }
        final String pattern = "%" + param_name + "{";
        int p1 = content.indexOf(pattern);
        if (p1 < 0) return null;
        p1 += pattern.length();  // position after '{'
        int p2 = content.indexOf('}', p1);
        if (p2 < 0) return null;
        return content.substring(p1, p2).trim();
    }
    
    private static String extractCustomStyleId(String content)
    {
        return extractParam(content, "style");
    }
    
    private static int extractColspan(String content)
    {
        String val = extractParam(content, "cols");
        int span;
        try {
            span = (val != null) ? Integer.parseInt(val) : -1;
        } catch (NumberFormatException ex) {
            span = -1;
        }
        return span;
    }
    
    private static int extractRowspan(String content)
    {
        String val = extractParam(content, "rows");
        int span;
        try {
            span = (val != null) ? Integer.parseInt(val) : -1;
        } catch (NumberFormatException ex) {
            span = -1;
        }
        return span;
    }
    
    private static void writeHeaderFooterField(Appendable buf, 
                                               String contentType, 
                                               String param, 
                                               ImageURLTransformer imgUrlTransformer)
    throws IOException
    {
        if (contentType.equals("biblio_id")) {
            writeBookInfoField(buf, "biblioid");
        } else
        if (contentType.equals("copyright")) {
            writeBookInfoField(buf, "copyright");
        } else
        if (contentType.equals("corporate")) {
            writeBookInfoField(buf, "corpauthor");
        } else
        if (contentType.equals("component_title")) {
            buf.append("<xsl:apply-templates select=\".\"  mode=\"titleabbrev.markup\" />");
        } else
        if (contentType.equals("component_numtitle")) {
            buf.append("<xsl:apply-templates select=\".\"  mode=\"object.title.markup\" />");
        } else
        if (contentType.equals("timestamp")) {
            writeTimeStamp(buf, param);  // param contains the format, e.g. "Y-m-d"
        } else
        if (contentType.equals("draft")) {
            buf.append("<xsl:call-template  name=\"draft.text\" />");
        } else
        if (contentType.equals("generated_title")) {
            writeGentextCall(buf, "$gentext-key");
        } else
        // if (contentType.equals("custom_gentext")) {
        //     if ((contentValue.indexOf("'") < 0) && (contentValue.indexOf('"') < 0)) {
        //         writeGentextCall(buf, "'" + contentValue + "'");
        //     }
        // } else
        if (contentType.equals("pagenumber")) {
            buf.append("<fo:page-number />");
        } else
        if (contentType.equals("pub_date")) {
            buf.append("<xsl:apply-templates select=\"//bookinfo/pubdate[1]\" mode=\"docma.headerfooter.mode\" />");
        } else
        if (contentType.equals("pub_subtitle")) {
            writeBookInfoField(buf, "subtitle");
        } else
        if (contentType.equals("pub_title")) {
            writeBookInfoField(buf, "title");
        } else
        if (contentType.equals("publisher")) {
            writeBookInfoField(buf, "publishername");
        } else
        if (contentType.equals("release_info")) {
            writeBookInfoField(buf, "releaseinfo");
        } else
        if (contentType.equals("section_title")) {
            buf.append("<fo:retrieve-marker retrieve-class-name=\"section.head.marker\" ")
               .append(" retrieve-position=\"first-including-carryover\" ")
               .append(" retrieve-boundary=\"page-sequence\" />");
        } else 
        if (contentType.equals("image")) {
            if (param != null) {
                String imgAlias = param;
                String imgHeight = null;
                int pos = param.indexOf(';');
                if (pos >= 0) {
                    imgAlias = param.substring(0, pos);
                    param = param.substring(pos + 1).trim().toLowerCase();
                    pos = param.indexOf(':');
                    if (pos >= 0) {
                        String propName = param.substring(0, pos).trim();
                        int prop_end = param.indexOf(';', pos);
                        if (prop_end < 0) prop_end = param.length();
                        if (propName.equals("height")) {
                            imgHeight = param.substring(pos + 1, prop_end).trim();
                        }
                    }
                }
                imgAlias = imgAlias.trim();
                if (imgAlias.length() > 0) {
                    String imgUrl = imgUrlTransformer.getImageURLByAlias(imgAlias);
                    if (imgUrl != null && (imgUrl.length() > 0)) {
                        buf.append("<fo:external-graphic");
                        if ((imgHeight != null) && (imgHeight.length() > 0)) {
                            imgHeight = imgHeight.replace('\'', ' ').replace('<', ' ').replace('>', ' ');
                            buf.append(" content-height='").append(imgHeight).append("'");
                        }
                        buf.append(">")
                           .append("<xsl:attribute name=\"src\">")
                           .append("<xsl:call-template name=\"fo-external-image\">")
                           .append("<xsl:with-param name=\"filename\" select=\"'")
                           .append(imgUrl)
                           .append("'\" /></xsl:call-template>")
                           .append("</xsl:attribute>")
                           .append("</fo:external-graphic>");
                    }
                }
            }
        } else
        if (contentType.equals("br")) {
            buf.append("<fo:block />");
        }
    }
    
    private static void writeBookInfoField(Appendable buf, String fieldName)
    throws IOException
    {
        buf.append("<xsl:apply-templates select=\"//bookinfo/").append(fieldName)
           .append("[1]\" mode=\"titlepage.mode\" />");
    }
    
    private static void writeUserDefinedContent(Appendable buf, 
                                                String txt, 
                                                DocmaExportContext exportCtx, 
                                                ImageURLTransformer imgUrlTransformer) 
    throws IOException
    {
        if ((txt == null) || (txt.length() == 0)) return;

        DocmaSession docmaSess = exportCtx.getDocmaSession();
        DocmaOutputConfig outConf = exportCtx.getOutputConfig();
        
        // Parsing modes
        final int MODE_TXT = 0;
        final int MODE_TXT_QUOTED = 1;
        
        int mode = MODE_TXT;
        int pos = 0;          // current parse position
        int token_start = 0;  // start of next fixed text token
        final int END_POS = txt.length() - 1;
        char ch = ' ';
        while (pos <= END_POS) {
            char prev_char = ch;
            ch = txt.charAt(pos);
            boolean is_quote = (ch == '\'');
            boolean start_include = (ch == '[');
            boolean start_placeholder = (ch == '%');
            if (mode == MODE_TXT) {
                if (pos >= END_POS) {   // end has been reached
                    writeFixedText(buf, unescapeText(txt.substring(token_start)));
                    break;
                }
                
                // Parse inclusion
                if (start_include && (prev_char != '\\')) {
                    int token_end = pos;  // end of fixed text
                    ++pos;
                    if (pos == END_POS) continue;  // cannot be a valid inclusion
                    ch = txt.charAt(pos);
                    boolean is_include_title = (ch == '#');
                    boolean is_include_gentext = (ch == '$') || (ch == '~');
                    if (is_include_title || is_include_gentext) {
                        int end_pos = txt.indexOf(']', pos);
                        if (end_pos < 0) { 
                            ++pos;
                            continue; // not a valid inclusion -> continue with text mode parsing
                        }
                        
                        // Write fixed text that preceeds the inclusion 
                        String token = unescapeText(txt.substring(token_start, token_end));
                        writeFixedText(buf, token);

                        // Write gentext or title inclusion
                        String name = txt.substring(pos + 1, end_pos);
                        if (is_include_gentext) {
                            writeGentext(buf, name);
                        } else {  
                            // if is_include_title
                            String[] applics = outConf.getEffectiveFilterApplics();
                            DocmaNode node = docmaSess.getApplicableNodeByLinkAlias(name, applics);
                            if (node != null) {
                                writeFixedText(buf, node.getTitleEntityEncoded());
                            }
                        }
                        ch = txt.charAt(end_pos);  // set ch to have correct prev_char in next loop (ch = ']')
                        pos = end_pos + 1;  // continue after ']'
                        token_start = pos;  // start next fixed text token after ']'
                        // mode = MODE_TXT;
                        continue;  // continue with text mode parsing
                    } else {  // not an inclusion -> continue with text mode parsing
                        ++pos;
                        continue; 
                    }
                }

                // Parse placeholder, e.g. %pagenumber or %image{mypic; height:25pt}
                if (start_placeholder && (prev_char != '\\')) {
                    int token_end = pos;   // end of fixed text
                    ++pos;
                    int name_end = pos;
                    while (name_end <= END_POS) {
                        ch = txt.charAt(name_end);
                        boolean is_name_char = Character.isLetter(ch) || (ch == '_');
                        if (! is_name_char) break;
                        ++name_end;
                    }
                    String name = txt.substring(pos, name_end);
                    pos = name_end; 
                    // ch contains character after name
                    if (name.length() > 0) {
                        // Write fixed text that preceeds the placeholder 
                        String token = unescapeText(txt.substring(token_start, token_end));
                        writeFixedText(buf, token);
                        
                        token_start = name_end;
                        String param = "";
                        
                        // If placeholder with parameter:
                        boolean start_param = (ch == '{');
                        if (start_param) {   // start parameter
                            int param_end = txt.indexOf('}', pos);
                            if (param_end >= 0) {
                                param = txt.substring(pos + 1, param_end);
                                ch = txt.charAt(param_end); // set ch to have correct prev_char in next loop
                                pos = param_end + 1;  // continue after parameter
                                token_start = pos;    // continue next token after parameter
                            }
                        }
                        // Write placeholder
                        writeHeaderFooterField(buf, name, param, imgUrlTransformer);
                        continue;   // continue parsing in text mode after placeholder
                    
                    } else {  // no valid placeholder name
                        continue; // continue with text mode parsing
                    }
                }

                // Start quoted text
                if (is_quote && (prev_char != '\\')) { 
                    // Write fixed text that preceeds the quoted text
                    String token = unescapeText(txt.substring(token_start, pos));
                    writeFixedText(buf, token);
                    ++pos;
                    token_start = pos;  // start next token after quote character
                    mode = MODE_TXT_QUOTED; 
                    continue;
                }
            } else
            if (mode == MODE_TXT_QUOTED) {
                if (is_quote && (prev_char != '\\')) {  // end of quoted text (if not escaped)
                    // Write quoted text
                    String token = unescapeText(txt.substring(token_start, pos));
                    writeFixedText(buf, token);
                    ++pos;
                    token_start = pos;  // start next token after ending quote character
                    mode = MODE_TXT;  // switch back to start mode
                    continue;
                }
                // otherwise
                if (pos == END_POS) {   // end has been reached
                    writeFixedText(buf, unescapeText(txt.substring(token_start)));
                    break;
                }
            }
            
            ++pos;  // Read next character in text or quoted mode
        }
    }
    
    private static String unescapeText(String txt)
    {
        return txt.replace("\\'", "'").replace("\\[", "[").replace("\\%", "%");
    }

    private static void writeFixedText(Appendable buf, String fixed_txt)
    throws IOException
    {
        if ((fixed_txt == null) || fixed_txt.equals("")) return;
        
        fixed_txt = fixed_txt.replace("<", "&#60;").replace(">", "&#62;"); // .replace("\"", "&#34;");
        buf.append("<xsl:text>").append(fixed_txt).append("</xsl:text>");
    }

    private static void writeTimeStamp(Appendable buf, String contentValue)
    throws IOException
    {
        contentValue = contentValue.trim();
        String format = contentValue;   // timestamp format; for example: "Y-m-d H:M" 
//        String prefix = null;
//        if (contentValue.startsWith("\"")) {  // fixed text prefix
//            int end_pos = contentValue.indexOf('"', 1);
//            if (end_pos > 0) {
//                prefix = contentValue.substring(1, end_pos);
//                format = contentValue.substring(end_pos + 1).trim();
//            }
//        } else
//        if (contentValue.startsWith("$")) {  // gentext reference
//            int end_pos = contentValue.indexOf(' ');
//            if (end_pos < 0) {  // only gentext reference is given but no format string
//                prefix = contentValue;
//                format = null;
//            } else {  // gentext reference and format string is given
//                prefix = contentValue.substring(0, end_pos);
//                format = contentValue.substring(end_pos + 1).trim();
//            }
//        }
//        writeUserDefined(buf, prefix);
        if ((format == null) || format.equals("")) {
            // use the gentext template named "format" in the context named "datetime"
            buf.append("<xsl:variable name=\"fmtstr\">");
            writeGentextTemplateCall(buf, "'datetime'", "'format'");
            buf.append("</xsl:variable>");
            format = "$fmtstr";
        } else {
            format = format.replace('\'', ' ').replace('"', ' ');
            format = "'" + format + "'";
        }
        buf.append("<xsl:call-template name=\"datetime.format\">")
           .append("<xsl:with-param name=\"date\" select=\"date:date-time()\" />")
           .append("<xsl:with-param name=\"format\" select=\"").append(format).append("\" />")
           .append("</xsl:call-template>");
    }
    
    private static void writeGentext(Appendable buf, String fullname) throws IOException
    {
        int pos = fullname.indexOf('|');
        if (pos < 0) {
            writeGentextCall(buf, "'" + fullname + "'");
        } else {
            String context = "'" + fullname.substring(0, pos) + "'";
            String name = "'" + fullname.substring(pos + 1) + "'";
            writeGentextTemplateCall(buf, context, name);
        }
    }
    
    private static void writeGentextTemplateCall(Appendable buf, String context, String name)
    throws IOException
    {
        buf.append("<xsl:call-template name=\"gentext.template\">")
           .append("<xsl:with-param name=\"name\" select=\"").append(name).append("\" />")
           .append("<xsl:with-param name=\"context\" select=\"").append(context).append("\" />")
           .append("</xsl:call-template>");
    }

    private static void writeGentextCall(Appendable buf, String key)
    throws IOException
    {
        buf.append("<xsl:call-template name=\"gentext\">")
           .append("<xsl:with-param name=\"key\" select=\"").append(key).append("\" />")
           .append("</xsl:call-template>");
    }
}
