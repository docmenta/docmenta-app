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

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import javax.servlet.http.*;

import org.docma.coreapi.*;

/**
 *
 * @author MP
 */
public class DocmaAppUtil
{
    private static final Pattern ALIAS_PATTERN = Pattern.compile(DocmaConstants.REGEXP_ALIAS);


    public static boolean isValidAlias(String name)
    {
        return ALIAS_PATTERN.matcher(name).matches();
    }
    
    public static String getNodePathAsText(DocmaNode node, int max_length)
    {
        if (node == null) {
            return "";
        }
        try {
            StringBuilder sb;
            if (node.isFileContent() || node.isImageContent()) {
                sb = new StringBuilder(node.getDefaultFileName());
            } else {
                sb = new StringBuilder(node.getTitle());
            }
            DocmaNode n = node.getParent();
            
            // Add child position of file and content nodes
            if ((n != null) && !node.isChildable()) {
                sb.insert(0, "[" + n.getChildPos(node) + "] ");
            }
            
            while (n != null) {
                if (sb.length() > max_length) {
                    sb.insert(0, "... / ");
                    break;
                }
                sb.insert(0, n.getTitle() + " / ");
                // if (n.isDocumentRoot()) {
                //     break;
                // }
                n = n.getParent();
            }
            return sb.toString();
        } catch (Exception ex) {  // should never occur
            return "[Node path error: " + ex.getLocalizedMessage() + "]";
        }
    }
    
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
    
    public static List listValuesStartWith(String prefix, List values, List outlist)
    {
        if (outlist == null) outlist = new ArrayList(values.size());

        int pos = Collections.binarySearch(values, prefix);
        if (pos < 0) {  // if not found
            pos = -(pos + 1);  // insertion point
        }
        while (pos < values.size()) {
            Object obj = values.get(pos);
            if (obj.toString().startsWith(prefix)) {
                outlist.add(obj);
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
        ContentCSS.write(docmaSess, outputConf, true, false, out);
    }

    public static void writeContentCSS(DocmaSession docmaSess, 
                                       DocmaOutputConfig outputConf,
                                       boolean exportMode,
                                       boolean editMode,
                                       Writer out)
    throws IOException
    {
        ContentCSS.write(docmaSess, outputConf, exportMode, editMode, out);
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
            boolean is_part_level = is_parts && (node_depth == 1);
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
                n_title = ContentResolver.resolveSectionTitle(exportCtx, n_title);
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

            if (omit_single_title && (content_count == 0) && (sect_nodes.size() == 1) && !is_part_level) {
                overwrite_title = "";  // omit title of single sub-section
                // Note: If the first level is rendered as book-part, and the
                // next level contains a single chapter only, then do not omit
                // the chapter-title, because a book-part must contain a chapter.
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
