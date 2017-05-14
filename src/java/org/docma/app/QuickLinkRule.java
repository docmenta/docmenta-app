/*
 * QuickLinkRule.java
 * 
 *  Copyright (C) 2017  Manfred Paula, http://www.docmenta.org
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

import java.util.Locale;
import org.docma.plugin.LogLevel;
import org.docma.plugin.PluginUtil;
import org.docma.plugin.StoreConnection;
import org.docma.plugin.UserSession;
import org.docma.plugin.rules.HTMLRule;
import org.docma.plugin.rules.HTMLRuleConfig;
import org.docma.plugin.rules.HTMLRuleContext;

/**
 *
 * @author MP
 */
public class QuickLinkRule implements HTMLRule
{
    public static final String CHECK_ID_TRANSFORM = "transform_quicklinks";
    
    private static final String QUICK_LINK_START = "[[";
    private static final String QUICK_LINK_END = "]]";

    private String uiLanguage = null;

    /* --------------  Interface HTMLRule  ---------------------- */
    
    public String getShortInfo(String languageCode) 
    {
        return PluginUtil.getResourceString(this.getClass(), languageCode, "shortInfo");
    }

    public String getLongInfo(String languageCode) 
    {
        return PluginUtil.getResourceString(this.getClass(), languageCode, "longInfo");
    }

    public void configure(HTMLRuleConfig conf) 
    {
    }

    public void startBatch(HTMLRuleContext context) 
    {
        uiLanguage = null;
    }

    public void finishBatch(HTMLRuleContext context) 
    {
    }

    public String apply(String content, HTMLRuleContext context) 
    {
        if (context.isEnabled(CHECK_ID_TRANSFORM)) {
            init(context);
            boolean autoCorrect = context.isAutoCorrect(CHECK_ID_TRANSFORM);
            return transform(content, context, autoCorrect);
        } else {
            return null;  // content is unchanged
        }
    }

    public String[] getCheckIds() 
    {
        return new String[] { CHECK_ID_TRANSFORM };
    }

    public String getCheckTitle(String checkId, String languageCode) 
    {
        return PluginUtil.getResourceString(this.getClass(), languageCode, checkId + ".title");
    }

    public boolean supportsAutoCorrection(String checkId) 
    {
        return true;
    }

    public LogLevel getDefaultLogLevel(String checkId) 
    {
        return LogLevel.INFO;
    }

    /* --------------  Private methods  ---------------------- */

    private void init(HTMLRuleContext context)
    {
        // Set UI language to the UI language of  the user session
        if (uiLanguage == null) {
            StoreConnection conn = context.getStoreConnection();
            UserSession     sess = (conn == null) ? null : conn.getUserSession();
            Locale          loc  = (sess == null) ? null : sess.getCurrentLocale();
            
            uiLanguage = (loc == null)  ? "en" : loc.getLanguage();
        }
    }

    private String label(String msgKey) 
    {
        String lang = (uiLanguage == null) ? "en" : uiLanguage;
        return PluginUtil.getResourceString(this.getClass(), lang, msgKey);
    }
    
    private String transform(String content, HTMLRuleContext ctx, boolean autoCorrect)
    {
        StoreConnection storeConn = ctx.getStoreConnection();
        
        // find first quick link
        int quick_start = content.indexOf(QUICK_LINK_START);
        if (quick_start < 0) {  // no quick link found
            return null;
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
                    String ql = content.substring(quick_start + QUICK_LINK_START.length(), quick_end);
                    anchor = transformQuickLinkToAnchor(storeConn, ql);
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
                
                // Write log message
                if (autoCorrect) {
                    ctx.logInfo(CHECK_ID_TRANSFORM, quick_start, label("msgQuickLinkTransformed"));
                } else {
                    ctx.log(CHECK_ID_TRANSFORM, quick_start, label("msgQuickLinkExists"));
                }
            }
            // search next quick link
            quick_start = content.indexOf(QUICK_LINK_START, copypos);
            if (quick_start < 0) { // no more quick links found
                break;
            }
        } while (copypos < content.length());

        // Return transformed content.
        // If buf is empty then no valid quick links have been found (content is unchanged).
        if (autoCorrect && (buf.length() > 0)) { 
            if (copypos < content.length()) {  // copy remaining content
                buf.append(content, copypos, content.length());
            }
            return buf.toString();
        } else {
            return null;
        }
    }
    
    private static String transformQuickLinkToAnchor(StoreConnection storeConn, String quicklink)
    {
        // read link target (read up to first whitespace or '|')
        int p = 0;
        int len = quicklink.length();
        while (p < len) {
            char ch = quicklink.charAt(p);
            if ((ch == '|') || 
                Character.isWhitespace(ch) || 
                ((ch == '&') && (quicklink.regionMatches(p, "&#160;", 0, 6) || 
                                 quicklink.regionMatches(p, "&nbsp;", 0, 6)))) {
                break;
            }
            p++;
        }
        String target = quicklink.substring(0, p);
        boolean hash_start = target.startsWith("#");
        String alias = hash_start ? target.substring(1) : target;
        boolean is_alias = alias.matches(DocmaConstants.REGEXP_ALIAS_LINK);
        if (is_alias && !hash_start) {
            target = "#" + target;
        }
        if (is_alias && (storeConn.getNodeIdByAlias(alias) == null)) {
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
            String linktext = trimWhitespaceAndSpaceEntities(quicklink.substring(p));
            boolean deadlink_title = false;
            if (linktext.startsWith("|")) {
                deadlink_title = true;
                linktext = trimWhitespaceAndSpaceEntities(linktext.substring(1));
            }
            boolean no_linktext = (linktext.length() == 0);
            if (no_linktext) {
                // linktext = is_alias ? ("'[#" + alias + "]'") : target;
                linktext = is_alias ? "%target%" : target;
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
    
    private static int findNextTagBoundary(CharSequence content, int start_pos)
    {
        while (start_pos < content.length()) {
            char ch = content.charAt(start_pos);
            if ((ch == '<') || (ch == '>')) return start_pos;
            ++start_pos;
        }
        return -1;
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
    
}
