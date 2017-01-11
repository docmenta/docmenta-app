/*
 * EditContentTransformer.java
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

package org.docma.webapp;

import java.util.*;

import org.docma.app.*;
import org.docma.plugin.DocmaException;
import org.docma.plugin.LogEntries;
import org.docma.plugin.implementation.HTMLRuleContextImpl;
import org.docma.plugin.implementation.LogEntriesImpl;
import org.docma.plugin.implementation.StoreConnectionImpl;
import org.docma.plugin.rules.HTMLRule;
import org.docma.util.CSSUtil;
import org.docma.util.XMLParser;


/**
 *
 * @author MP
 */
public class EditContentTransformer
{
    // private static final String PADDING_PATTERN = "padding-left:";
    private static final String QUICK_LINK_START = "[[";
    private static final String QUICK_LINK_END = "]]";

    public static final String PROP_TRANSFORM_EDITOR_ID = "editor_id";
    public static final String PROP_TRANSFORM_QUICKLINKS = "quick_links";
    public static final String PROP_TRANSFORM_TRIM_EMPTY_PARAS = "trim_empty_paras";
    public static final String PROP_TRANSFORM_TRIM_FIGURE_SPACES = "trim_figure_spaces";

    public static LogEntries prepareHTMLForSave(StringBuilder content,
                                                String nodeId,
                                                Map<Object, Object> props,
                                                DocmaSession docmaSess)
    {
        // String editorId = props.getProperty(PROP_TRANSFORM_EDITOR_ID, "");
        String ql = null;
        boolean trim_empty_p = true;
        boolean trim_fig_spaces = true;
        if (props != null) {
            Object obj = props.get(PROP_TRANSFORM_QUICKLINKS);
            ql = (obj == null) ? null : obj.toString();
            obj = props.get(PROP_TRANSFORM_TRIM_EMPTY_PARAS);
            trim_empty_p = (obj == null) ? true : obj.toString().equalsIgnoreCase("true");
            obj = props.get(PROP_TRANSFORM_TRIM_FIGURE_SPACES);
            trim_fig_spaces = (obj == null) ? true : obj.toString().equalsIgnoreCase("true");
        }
        if ((ql == null) || ql.equals("")) {
            ql = docmaSess.getUserProperty(GUIConstants.PROP_USER_QUICKLINKS_ENABLED);
        }
        boolean ql_enabled = (ql != null) && ql.equalsIgnoreCase("true");
        
        if (ql_enabled || trim_empty_p || trim_fig_spaces) {
            String cont = content.toString();
            String fixcontent = ql_enabled ? transformQuickLinks(docmaSess, cont) : cont;
            if (trim_empty_p) {
                fixcontent = removeEmptyParaFromEnd(fixcontent);
            }
            if (trim_fig_spaces) {
                fixcontent = removeSpacesBeforeAfterFigure(fixcontent);
            }
            // Replace old content by new content
            content.replace(0, content.length(), fixcontent);
        }
        return applyHTMLRules(content, nodeId, props, docmaSess);
    }

    private static LogEntries applyHTMLRules(StringBuilder content,
                                             String nodeId, 
                                             Map<Object, Object> props, 
                                             DocmaSession docmaSess) throws DocmaException
    {
        RulesManager rm = docmaSess.getRulesManager();
        RuleConfig[] rules = rm.getAllRules();
        StoreConnectionImpl storeConn = (StoreConnectionImpl) docmaSess.getPluginStoreConnection();
        HTMLRuleContextImpl ctx = new HTMLRuleContextImpl(storeConn, content);
        if (props != null) {
            ctx.setProperties(props);
        }
        ctx.setModeSave();
        ctx.setAllowAutoCorrect(true);
        ctx.setNodeId(nodeId);
        
        for (RuleConfig rc : rules) {
            if (rc.isRuleEnabled() && rc.isApplicableForStore(docmaSess.getStoreId())) {
                // check if rule is turned off by supplied properties
                Object val = (props == null) ? null : props.get(rc.getId());
                if (val != null) {
                    String v = val.toString();
                    if (v.equalsIgnoreCase("false") || v.equalsIgnoreCase("off")) {
                        continue;   // skip this rule
                    }
                }
                
                Object obj = rc.getRuleInstance();
                if (obj instanceof HTMLRule) {
                    HTMLRule hr = (HTMLRule) obj;
                    ctx.setActiveRule(rc);
                    try {
                        hr.apply(content, ctx);
                    } catch (Exception ex) {
                        ctx.log(null, "Exception in rule " + rc.getId() + ": " + ex.getMessage());
                    }
                }
            }
        }
        
        return new LogEntriesImpl(ctx.getLog());
    }
    
    private static String transformQuickLinks(DocmaSession docmaSess, String content)
    {
        // find first quick link
        int quick_start = content.indexOf(QUICK_LINK_START);
        if (quick_start < 0) {  // no quick link found
            return content;
        }
        
        StringBuilder buf = new StringBuilder();
        int copypos = 0;
        do {
            String anchor = null;
            int quick_end = content.indexOf(QUICK_LINK_END, quick_start);
            if (quick_end < 0) {  // no end-pattern found in whole content
                break;  // stop searching and return remaining content unmodified
            } else {
                // Note: if the pattern ']]]' is found, then the inner ] may 
                // belong to a title inclusion. 
                int after_end = quick_end + QUICK_LINK_END.length();
                if ((after_end < content.length()) && (content.charAt(after_end) == ']')) {
                    quick_end++;
                    after_end++;
                }
                // skip if end pattern']]' is inside another element or  
                // if quick-link is inside the attribute section of an element
                int next_boundary = findNextTagBoundary(content, quick_start);
                boolean inside_attribute = (next_boundary >= 0) && (content.charAt(next_boundary) == '>');
                boolean invalid = inside_attribute || (next_boundary < quick_end);
                if (! invalid) {
                    anchor = transformQuickLinkToAnchor(docmaSess, content, quick_start, quick_end);
                }
            }
            if (anchor == null) {
                // Do not transform invalid quick link (continue search after start pattern)
                int after_start = quick_start + QUICK_LINK_START.length();
                buf.append(content, copypos, after_start);
                copypos = after_start;
            } else {
                // Replace quick-link by anchor tag
                buf.append(content, copypos, quick_start);
                buf.append(anchor);
                copypos = quick_end + QUICK_LINK_END.length();
            }
            // search next quick link
            quick_start = content.indexOf(QUICK_LINK_START, copypos);
            if (quick_start < 0) { // no more quick links found
                break;
            }
        } while (copypos < content.length());

        // return transformed content
        if (buf.length() == 0) {  // no valid quick links have been found
            return content;  // return original content
        } else {
            if (copypos < content.length()) {  // copy remaining content
                buf.append(content, copypos, content.length());
            }
            return buf.toString();
        }
    }
    
    private static String transformQuickLinkToAnchor(DocmaSession docmaSess, 
                                                     String content, 
                                                     int quick_start, 
                                                     int quick_end)
    {
        // read link target (read up to first whitespace or '|')
        int target_start = quick_start + QUICK_LINK_START.length();
        int p = target_start;
        while (p < quick_end) {
            char ch = content.charAt(p);
            if ((ch == '|') || 
                Character.isWhitespace(ch) || 
                ((ch == '&') && (content.regionMatches(p, "&#160;", 0, 6) || 
                                 content.regionMatches(p, "&nbsp;", 0, 6)))) {
                break;
            }
            p++;
        }
        String target = content.substring(target_start, p);
        boolean hash_start = target.startsWith("#");
        String alias = hash_start ? target.substring(1) : target;
        boolean is_alias = alias.matches(DocmaConstants.REGEXP_ALIAS_LINK);
        if (is_alias && !hash_start) {
            target = "#" + target;
        }
        if (is_alias && (docmaSess.getNodeIdByAlias(alias) == null)) {
            return null;  // node with given alias does not exist -> do not transform
        }
        boolean is_url = false;
        boolean is_internal_link = is_alias;
        if (! (is_alias || hash_start)) {
            is_internal_link = target.startsWith("file/") || 
                               target.startsWith("image/"); 
            // URL link: e.g. http:// or mailto:
            // Note: Quotes and backslash are not allowed in URL 
            is_url = is_internal_link ||
                     target.matches("[A-Za-z][0-9A-Za-z_]*:[^'\"\\\\]+");  // literal "\\\\" is resolved to \\
        }
        if (is_alias || is_url) {
            // read link text
            String linktext = trimWhitespaceAndSpaceEntities(content.substring(p, quick_end));
            boolean deadlink_title = false;
            if (linktext.startsWith("|")) {
                deadlink_title = true;
                linktext = trimWhitespaceAndSpaceEntities(linktext.substring(1));
            }
            boolean no_linktext = (linktext.length() == 0);
            if (no_linktext) {
                linktext = is_alias ? ("'[#" + alias + "]'") : target;
            }
            StringBuilder anchor = new StringBuilder();
            anchor.append("<a href=\"").append(target).append("\"");
            if (is_internal_link && (no_linktext || deadlink_title)) {
                anchor.append(" title=\"%target%\"");
            }
            anchor.append(">").append(linktext).append("</a>");
            return anchor.toString();
        } else {
            return null;  // no valid quick-link
        }
    }
    
    private static String trimWhitespaceAndSpaceEntities(String str)
    {
        int startpos = skipWhiteSpaceAfter(str, 0);
        if (startpos >= str.length()) {
            return "";
        }
        int endpos = skipWhiteSpaceBefore(str, str.length());
        return str.substring(startpos, endpos + 1);
    }
    
    private static int findNextTagBoundary(String content, int start_pos)
    {
        while (start_pos < content.length()) {
            char ch = content.charAt(start_pos);
            if ((ch == '<') || (ch == '>')) return start_pos;
            ++start_pos;
        }
        return -1;
    }
    
    private static String removeSpacesBeforeAfterFigure(String content)
    {
        final String IMG_START = "<img ";
        int tag_start = content.indexOf(IMG_START);
        if (tag_start < 0) {
            return content;  // content does not contain any image tag
        }
        int len = content.length();
        List attnames = new ArrayList();
        List attvalues = new ArrayList();
        StringBuilder buf = null;
        int copypos = 0;
        do {
            int att_start = tag_start + IMG_START.length();  // start of img attributes
            int startpos = att_start;  // continue search for next img tag after this tag
            attnames.clear(); 
            attvalues.clear();
            // int tag_end = content.indexOf("/>", tag_start);
            int tag_end = XMLParser.parseTagAttributes(content, att_start, attnames, attvalues);
            if (tag_end > 0) {   // position of '>', tag_end < 0 means syntax error 
                int pos_after_tag = tag_end + 1;
                startpos = pos_after_tag;  // continue search for next tag after this tag
                if (attnames.contains("title")) { // img is a figure, i.e. has a title
                    
                    // Skip all whitespace before the img tag
                    int pos = skipWhiteSpaceBefore(content, tag_start); 
                    // if only whitespace exists between figure and previous element
                    if ((pos < (tag_start - 1)) && (pos > 0) && (content.charAt(pos) == '>')) { 
                        if (buf == null) { buf = new StringBuilder(len); }
                        buf.append(content, copypos, pos + 1);
                        copypos = tag_start;  // continue copying after the whitespace
                    }
                    
                    // Skip all whitespace after the img tag
                    pos = skipWhiteSpaceAfter(content, pos_after_tag);
                    // if only whitespace exists between figure and following element
                    if ((pos > pos_after_tag) && (pos < len) && (content.charAt(pos) == '<')) {  
                        if (buf == null) { buf = new StringBuilder(len); }
                        buf.append(content, copypos, pos_after_tag);
                        copypos = pos;  // continue copying after the whitespace
                    }
                }
            }
            if (startpos >= len) break;
            tag_start = content.indexOf(IMG_START, startpos);  // search next img tag
        } while (tag_start >= 0);

        if (buf == null) {   // no whitespace removed
            return content;
        } else {
            if (copypos < len) {
                buf.append(content, copypos, len); // copy remaining content
            }
            return buf.toString();
        }
    }
    
    private static int skipWhiteSpaceBefore(String content, int startpos) 
    {
        int pos = startpos;
        while (pos > 0) {
            char ch = content.charAt(--pos);
            if (Character.isWhitespace(ch)) continue;
            if ((ch == ';') && (pos >= 5) && 
                (content.regionMatches(pos - 5, "&#160;", 0, 6) ||
                 content.regionMatches(pos - 5, "&nbsp;", 0, 6))) {
                pos -= 5;
                continue;
            }
            break;
        }
        // return 0 or the position of the first non-whitespace character.
        return pos;
    }
    
    private static int skipWhiteSpaceAfter(String content, int startpos) 
    {
        int pos = startpos;
        int len = content.length();
        while (pos < len) {
            char ch = content.charAt(pos);
            if (Character.isWhitespace(ch)) {
                pos++;
                continue;
            }
            if ((ch == '&') && ((pos + 5) < len) && 
                (content.regionMatches(pos, "&#160;", 0, 6) ||
                 content.regionMatches(pos, "&nbsp;", 0, 6))) {
                pos += 6;
                continue;
            }
            break;
        }
        // pos is either the position of the first non-whitespace character 
        // or equal to content.length() 
        return pos;
    }

    private static String removeEmptyParaFromEnd(String content)
    {
        final String PATT_1 = "<p>&#160;</p>";
        final String PATT_2 = "<p>&nbsp;</p>";
        final String PATT_3 = "<p></p>";

        String str = content.trim();
        int strlen = str.length();
        if (str.endsWith(PATT_1)) return str.substring(0, strlen - PATT_1.length());
        if (str.endsWith(PATT_2)) return str.substring(0, strlen - PATT_2.length());
        if (str.endsWith(PATT_3)) return str.substring(0, strlen - PATT_3.length());
        return content;
    }


    public static String roundIndentToHigherInt(String indent)
    {
        if (indent.contains(".")) {
            String unit = CSSUtil.getSizeUnit(indent);
            float f = CSSUtil.getSizeFloat(indent);
            int indent_int = (int) Math.ceil(f);
            return Integer.toString(indent_int) + unit;
        } else {
            return indent;
        }
    }

}
