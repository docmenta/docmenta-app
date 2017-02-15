/*
 * BaseRule.java
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.docma.plugin.LogLevel;
import org.docma.plugin.PluginUtil;
import org.docma.plugin.StoreConnection;
import org.docma.plugin.UserSession;
import org.docma.plugin.rules.HTMLRule;
import org.docma.plugin.rules.HTMLRuleConfig;
import org.docma.plugin.rules.HTMLRuleContext;
import org.docma.util.Log;
import org.docma.util.XMLParser;


/**
 *
 * @author MP
 */
public class BaseRule implements HTMLRule
{
    public static final String CHECK_ID_TRIM_EMPTY_PARAS = "trim_empty_paras";
    public static final String CHECK_ID_TRIM_FIGURE_SPACES = "trim_figure_spaces";

    private boolean initialized = false;
    private String msgEmptyParaExists = "";
    private String msgEmptyParaRemoved = "";
    private String msgFigureSpacesExist = "";
    private String msgFigureSpacesRemoved = "";

    public BaseRule()
    {
        debug("BaseRule constructor()");
    }
    
    /* --------------  Interface HTMLRule  ---------------------- */
    
    public String getShortInfo(String languageCode) 
    {
        debug("BaseRule.getShortInfo()");
        return PluginUtil.getResourceString(this.getClass(), languageCode, "shortInfo");
    }

    public String getLongInfo(String languageCode) 
    {
        debug("BaseRule.getLongInfo()");
        return PluginUtil.getResourceString(this.getClass(), languageCode, "longInfo");
    }

    public void configure(HTMLRuleConfig conf) 
    {
        debug("BaseRule.configure()");
    }

    public void startBatch() 
    {
        debug("BaseRule.startBatch()");
        initialized = false;
    }

    public void finishBatch() 
    {
        debug("BaseRule.finishBatch()");
    }

    public void apply(StringBuilder content, HTMLRuleContext context) 
    {
        debug("BaseRule.apply()");
        init(context);
        boolean check_empty_p = context.isEnabled(CHECK_ID_TRIM_EMPTY_PARAS);
        boolean check_fig_spaces = context.isEnabled(CHECK_ID_TRIM_FIGURE_SPACES);
        boolean correct_empty_p = context.isAutoCorrect(CHECK_ID_TRIM_EMPTY_PARAS);
        boolean correct_fig_spaces = context.isAutoCorrect(CHECK_ID_TRIM_FIGURE_SPACES);
        
        if (check_empty_p || check_fig_spaces) {
            String fixcontent = content.toString();
            if (check_empty_p) {
                String s = removeEmptyParaFromEnd(fixcontent, correct_empty_p, context);
                if (correct_empty_p) {
                    fixcontent = s;
                }
            }
            if (check_fig_spaces) {
                String s = removeSpacesBeforeAfterFigure(fixcontent, correct_fig_spaces, context);
                if (correct_fig_spaces) {
                    fixcontent = s;
                }
            }
            if (correct_empty_p || correct_fig_spaces) {
                // Replace old content by new content
                content.replace(0, content.length(), fixcontent);
            }
        }
    }

    public String[] getCheckIds() 
    {
        debug("BaseRule.getCheckIds()");
        return new String[] { CHECK_ID_TRIM_EMPTY_PARAS, CHECK_ID_TRIM_FIGURE_SPACES };
    }

    public String getCheckTitle(String checkId, String languageCode) 
    {
        debug("BaseRule.getCheckTitle()");
        return PluginUtil.getResourceString(this.getClass(), languageCode, checkId + ".title");
    }

    public boolean supportsAutoCorrection(String checkId) 
    {
        debug("BaseRule.supportsAutoCorrection()");
        return checkId.equals(CHECK_ID_TRIM_EMPTY_PARAS) || 
               checkId.equals(CHECK_ID_TRIM_FIGURE_SPACES);
    }

    public LogLevel getDefaultLogLevel(String checkId) 
    {
        debug("BaseRule.getDefaultLogLevel()");
        return LogLevel.INFO;
    }
    
    /* --------------  Private methods  ---------------------- */

    private void debug(String msg)
    {
        if (DocmaConstants.DEBUG) {
            Log.info(msg);
        }
    }
    
    private void init(HTMLRuleContext context)
    {
        // Initialize log messages
        if (! initialized) {
            StoreConnection conn = context.getStoreConnection();
            UserSession     sess = (conn == null) ? null : conn.getUserSession();
            Locale          loc  = (sess == null) ? null : sess.getCurrentLocale();
            String          lang = (loc == null)  ? "en" : loc.getLanguage();
            
            msgEmptyParaExists = label(lang, "msgEmptyParaExists");
            msgEmptyParaRemoved = label(lang, "msgEmptyParaRemoved");
            msgFigureSpacesExist = label(lang, "msgFigureSpacesExist");
            msgFigureSpacesRemoved = label(lang, "msgFigureSpacesRemoved");
            
            initialized = true;
        }
    }
    
    private String label(String lang, String msgKey) 
    {
        return PluginUtil.getResourceString(this.getClass(), lang, msgKey);
    }
    
    private String removeSpacesBeforeAfterFigure(String content, boolean correct, HTMLRuleContext context)
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
                    boolean skipped = false;
                    int pos = skipWhiteSpaceBefore(content, tag_start); 
                    // if only whitespace exists between figure and previous element
                    if ((pos < (tag_start - 1)) && (pos > 0) && (content.charAt(pos) == '>')) { 
                        if (buf == null) { buf = new StringBuilder(len); }
                        buf.append(content, copypos, pos + 1);
                        copypos = tag_start;  // continue copying after the whitespace
                        skipped = true;
                    }
                    
                    // Skip all whitespace after the img tag
                    pos = skipWhiteSpaceAfter(content, pos_after_tag);
                    // if only whitespace exists between figure and following element
                    if ((pos > pos_after_tag) && (pos < len) && (content.charAt(pos) == '<')) {  
                        if (buf == null) { buf = new StringBuilder(len); }
                        buf.append(content, copypos, pos_after_tag);
                        copypos = pos;  // continue copying after the whitespace
                        skipped = true;
                    }
                    
                    // Write log message
                    if (skipped) {
                        if (correct) {
                            context.logInfo(CHECK_ID_TRIM_FIGURE_SPACES, tag_start, msgFigureSpacesRemoved);
                        } else {
                            context.log(CHECK_ID_TRIM_FIGURE_SPACES, tag_start, msgFigureSpacesExist);
                        }
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

    private String removeEmptyParaFromEnd(String content, boolean correct, HTMLRuleContext context)
    {
        final String PATT_1 = "<p>&#160;</p>";
        final String PATT_2 = "<p>&nbsp;</p>";
        final String PATT_3 = "<p></p>";

        String str = content.trim();
        int strlen = str.length();
        int endpos = -1;
        
        if (str.endsWith(PATT_1)) {
            endpos = strlen - PATT_1.length();
        } else if (str.endsWith(PATT_2)) { 
            endpos = strlen - PATT_2.length();
        } else if (str.endsWith(PATT_3)) {
            endpos = strlen - PATT_3.length();
        }
        
        if (endpos >= 0) {  // if empty para exists
            // Write log message
            if (correct) {
                context.logInfo(CHECK_ID_TRIM_EMPTY_PARAS, endpos, msgEmptyParaRemoved);
            } else {
                context.log(CHECK_ID_TRIM_EMPTY_PARAS, endpos, msgEmptyParaExists);
            }
            
            return str.substring(0, endpos);  // trim empty para
        } else {
            return content;
        }
    }

}
