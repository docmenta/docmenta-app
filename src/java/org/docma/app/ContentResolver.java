/*
 * ContentResolver.java
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
import org.docma.coreapi.*;

/**
 *
 * @author MP
 */
public class ContentResolver
{

    public static String getContentRecursive(DocmaNode node)
    {
        final boolean resolveInlineInclude = true;
        final boolean suppressAutoFormat = false;
        return getContentRecursive(node, new HashSet(), null, resolveInlineInclude, suppressAutoFormat, 0);
    }

    public static String getContentRecursive(DocmaNode node,
                                             boolean resolveInlineInclude)
    {
        final boolean suppressAutoFormat = false;
        return getContentRecursive(node, new HashSet(), null, resolveInlineInclude, suppressAutoFormat, 0);
    }

    public static String getContentRecursive(DocmaNode node,
                                             Set idSet,
                                             boolean resolveInlineInclude)
    {
        final boolean suppressAutoFormat = false;
        return getContentRecursive(node, idSet, null, resolveInlineInclude, suppressAutoFormat, 0);
    }

    public static String getContentRecursive(DocmaNode node,
                                             Set idSet,
                                             DocmaExportContext exportCtx,
                                             boolean resolveInlineInclude,
                                             boolean suppressAutoFormat,
                                             int level)
    {
        return getContentRecursive(node, idSet, exportCtx, resolveInlineInclude, 
                                   suppressAutoFormat, level, null, false, null);
    }

    public static String getContentRecursive(DocmaNode node,
                                             Set idSet,
                                             DocmaExportContext exportCtx,
                                             boolean resolveInlineInclude,
                                             int level,
                                             String searchTerm,
                                             boolean ignoreCase,
                                             List matches)
    {
        return getContentRecursive(node, idSet, exportCtx, resolveInlineInclude, 
                                   false, level, searchTerm, ignoreCase, matches);
    }

    private static String getContentRecursive(DocmaNode node,
                                              Set idSet,
                                              DocmaExportContext exportCtx,
                                              boolean resolveInlineInclude,
                                              boolean suppressAutoFormat,
                                              int level,
                                              String searchTerm,
                                              boolean ignoreCase,
                                              List matches)
    {
        ExportLog exportLog = (exportCtx == null) ? null : exportCtx.getExportLog();
        String error_label = null;
        if (level >= 10) {
            error_label = "publication.export.inline_include_level_exceeded";
        }
        if (! node.isHTMLContent()) { 
            error_label = "publication.export.inline_include_invalid_nodetype";
        } else {
            if ((level > 0) && node.hasContentAnchor()) {
                error_label = "publication.export.inline_include_has_anchor";
            }
        }

        if (error_label != null) {
            if (exportLog != null) {
                String node_name = node.getAlias();
                if (node_name == null) { 
                    node_name = node.getId(); 
                }
                LogUtil.addError(exportLog, node, error_label, node_name);
            }
            return null;
        }

        String node_id = node.getId();
        boolean add_matches = idSet.add(node_id);

        String cont = node.getContentString();
        if (searchTerm != null) {
            cont = insertSearchMarks(cont, true, searchTerm, ignoreCase, node_id, add_matches, matches);
        }

        if (! resolveInlineInclude) {
            return cont; 
        }

        DocmaOutputConfig outConf = (exportCtx == null) ? null : exportCtx.getOutputConfig();
        String[] effectiveFilter = (outConf == null) ? null : outConf.getEffectiveFilterApplics();  // null means unfiltered
        
        // Resolve inline inclusions: 
        // [# is a title inclusion.
        // [## is a content inclusion.
        // [$ is a gentext inline inclusion.
        int start_pos = 0;
        while (start_pos < cont.length()) {
            int p1 = cont.indexOf("[", start_pos);
            if (p1 < 0) break;
            start_pos = p1 + 1;
            if (start_pos == cont.length()) break;
            char next_char = cont.charAt(start_pos);
            // if ((next_char != '#') && (next_char != '$')) {
            //     continue;
            // }
            if (next_char == '#') {
                int alias_start = p1 + 2;
                start_pos = alias_start;

                int p2 = cont.indexOf("]", alias_start);
                if ((p2 < 0) || (p2 - alias_start > DocmaConstants.ALIAS_MAX_LENGTH + 1)) continue;

                String ref_alias = cont.substring(alias_start, p2);
                if (ref_alias.length() == 0) continue;
                boolean is_content_inc = ref_alias.startsWith("#");
                if (is_content_inc) ref_alias = ref_alias.substring(1);  // remove leading #
                if (! ref_alias.matches(DocmaConstants.REGEXP_ALIAS_LINK)) continue;

                DocmaSession docmaSess = node.getDocmaSession();
                // DocmaNode ref_node = docmaSess.getNodeByAlias(ref_alias);
                DocmaNode ref_node = docmaSess.getApplicableNodeByLinkAlias(ref_alias, effectiveFilter);
                if (ref_node == null) continue;

                String replace_str;
                if (is_content_inc) {
                    if (ref_node.isFileContent()) {
                        replace_str = ref_node.getContentString();
                        if (replace_str == null) continue;
                        replace_str = replace_str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
                        replace_str = replace_str.replace("\r\n", "<br/>").replace("\n", "<br/>").replace("\r", "<br/>");
                    } else {
                        replace_str = getContentRecursive(ref_node, idSet, exportCtx,
                                                          resolveInlineInclude, level+1,
                                                          searchTerm, ignoreCase, matches);
                        if (replace_str == null) continue;

                        // remove enclosing p element
                        replace_str = replace_str.trim();
                        if (replace_str.startsWith("<p")) {
                            int p3 = replace_str.indexOf('>') + 1;
                            if (p3 > 0) {
                                if (replace_str.endsWith("</p>")) {
                                    int p4 = replace_str.length();
                                    replace_str = replace_str.substring(p3, p4 - 4);
                                }
                            }
                        }
                    }
                } else {
                    replace_str = ref_node.getTitleEntityEncoded();
                    if (replace_str == null) replace_str = "";
                }

                cont = cont.substring(0, p1) + replace_str + cont.substring(p2+1);
                start_pos = p1 + replace_str.length();
            } else 
            if (next_char == '$') {
                // Gentext inline inclusion
                int name_start = p1 + 2;
                start_pos = name_start;

                int p2 = cont.indexOf("]", name_start);
                if ((p2 < 0) || (p2 - name_start > DocmaConstants.GENTEXT_KEY_MAX_LENGTH + 1)) continue;

                String gen_name = cont.substring(name_start, p2);
                if (gen_name.length() == 0) continue;
                if (! gen_name.matches(DocmaConstants.REGEXP_GENTEXT_KEY)) continue;
                
                String gen_value = null;
                DocmaSession docmaSess = node.getDocmaSession();
                if (exportCtx == null) {
                    Properties props = docmaSess.getGenTextProps();
                    if (props != null) gen_value = props.getProperty(gen_name);
                } else {   // reading gentext property from export context could be more efficient
                    gen_value = exportCtx.getGenTextProperty(gen_name);
                }
                if (gen_value == null) continue;  // key not defined; do not replace
                
                String replace_str = docmaSess.encodeCharEntities(gen_value, false);
                cont = cont.substring(0, p1) + replace_str + cont.substring(p2+1);
                start_pos = p1 + replace_str.length();
            }
        }

        boolean autoformat = (exportCtx != null) && exportCtx.isAutoFormatEnabled() && !suppressAutoFormat;
        if ((searchTerm == null) && autoformat) {
            try {
                cont = AutoFormatResolver.resolveAutoFormatStyles(cont, exportCtx);
            } catch (Exception ex) {
                if (DocmaConstants.DEBUG) ex.printStackTrace();
                LogUtil.addError(exportLog, node, "publication.export.cannot_resolve_auto_format",
                                 node_id, ex.getMessage());
            }
        }
        return cont;
    }

    public static String getContentWithSearchMarks(DocmaNode node,
                                                   String searchTerm,
                                                   boolean ignoreCase,
                                                   List matches)
    {
        if (node.isHTMLContent()) {
            return insertSearchMarks(node.getContentString(), true, searchTerm, ignoreCase, node.getId(), true, matches);
        } else {
            return null;
        }
    }

    public static String getTitleWithSearchMarks(DocmaNode node,
                                                 String searchTerm,
                                                 boolean ignoreCase,
                                                 List matches)
    {
        return insertSearchMarks(node.getTitle(), false, searchTerm, ignoreCase, node.getId(), true, matches);
    }

    private static String insertSearchMarks(String str,
                                            boolean isXML,
                                            String searchTerm,
                                            boolean ignoreCase,
                                            String nodeId,
                                            boolean addMatches,
                                            List matches)
    {
        if (str == null) return null;

        String searchStr;
        if (ignoreCase) {
            searchTerm = searchTerm.toLowerCase();
            searchStr = str.toLowerCase();
        } else {
            searchStr = str;
        }
        if (searchStr.indexOf(searchTerm) < 0) return str; // no match

        StringBuilder buf = new StringBuilder(2 * str.length());
        int copyPos = 0;
        int match_cnt = 0;
        while (copyPos < str.length()) {
            int p = findNextMatch(searchStr, searchTerm, copyPos, isXML);
            if (p >= 0) {
                while (copyPos < p) buf.append(str.charAt(copyPos++));

                if (addMatches) {
                    String matchId = "match_" + nodeId + "_" + match_cnt;
                    SearchMatch sm = new SearchMatch();
                    sm.setMatchId(matchId);
                    sm.setNodeId(nodeId);
                    sm.setTextPos(p);
                    matches.add(sm);
                    match_cnt++;

                    buf.append("<a name=\"").append(matchId).append("\"></a>");
                    buf.append("<span class=\"docma-match\" id=\"").append(matchId).append("\">");
                } else {
                    buf.append("<span class=\"docma-dupmatch\">");
                }
                buf.append(str.substring(p, p + searchTerm.length()));
                buf.append("</span>");

                copyPos += searchTerm.length();
            } else {
                // copy remaining string
                while (copyPos < str.length()) buf.append(str.charAt(copyPos++));
                break;
            }
        }
        return buf.toString();
    }

    private static int findNextMatch(String str, String searchTerm, int startPos, boolean isXML)
    {
        boolean found = false;
        while ((startPos < str.length()) && !found) {
            int pos = str.indexOf(searchTerm, startPos);
            if (! isXML) return pos;

            if (pos >= 0) {
                found = true;
                // check if match is inside a XML tag
                int i = pos + searchTerm.length();
                while (i < str.length()) {
                    if (str.charAt(i) == '<') {  // valid match
                        break;
                    } else
                    if (str.charAt(i) == '>') {  // match is part of a tag: ignore match
                        startPos = i + 1;
                        found = false;
                        break;
                    }
                    i++;
                }
                if (found) return pos;
            } else {
                return -1;
            }
        }
        return -1;
    }

//    private static void eliminateXMLComments(StringBuffer buf)
//    {
//        int startPos = 0;
//        while (true) {
//            int p = buf.indexOf("<!--", startPos);
//            if (p < 0) return;
//            int p2 = buf.indexOf("-->", p);
//            if (p2 < 0) return;
//            p2 += "-->".length();
//            for (int i = p; i < p2; i++) {
//                buf.setCharAt(i, ' ');
//            }
//        }
//    }


}
