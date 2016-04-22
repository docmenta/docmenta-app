/*
 * TinyEditorUtil.java
 * 
 *  Copyright (C) 2016  Manfred Paula, http://www.docmenta.org
 *   
 *  This file is part of Docmenta. Docmenta is free software: you can 
 *  redistribute it and/or modify it under the terms of the GNU Lesser 
 *  General Public License as published by the Free Software Foundation, 
 *  either version 3 of the License, or (at your option) any later version.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Docmenta.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.docma.plugin.tinymce;

import org.docma.app.ContentUtil;
import org.docma.app.DocmaConstants;
import org.docma.webapp.EditContentTransformer;
import org.docma.util.CSSUtil;
import org.docma.util.Log;

/**
 *
 * @author MP
 */
public class TinyEditorUtil 
{
    private static final String INDENT_CSS_CLASS_PREFIX = "indent-level";

    // public static final String PROP_TRANSFORM_EDITOR_ID = EditContentTransformer.PROP_TRANSFORM_EDITOR_ID;
    // public static final String PROP_TRANSFORM_PARA_INDENT = "transform_para_indent";

    public static String prepareContentForEdit(String content, String editorId, String paraIndent)
    {
        String s = fixIndentForEdit(content, editorId, paraIndent);
        s = addEmptyPara(s);
        return s;
    }

    public static String prepareContentForSave(String content, 
                                               String editorId, 
                                               String paraIndent)
    {
        String fixcontent = fixIndentForSave(content, editorId, paraIndent);
        fixcontent = fixListIndentForSave(fixcontent, "ol");
        fixcontent = fixListIndentForSave(fixcontent, "ul");
        fixcontent = fixBlockquotedListForSave(fixcontent);
        fixcontent = fixLinkURLsForSave(fixcontent);
        return fixcontent;
    }

    public static boolean contentIsEqual(String content1, String content2, boolean strict_compare)
    {
        return ContentUtil.contentIsEqual(content1, content2, strict_compare);
    }

    public static String roundIndentToHigherInt(String indent)
    {
        return EditContentTransformer.roundIndentToHigherInt(indent);
    }

    private static String addEmptyPara(String content)
    {
        return content + "<p>&#160;</p>";
    }

    private static String fixIndentForEdit(String content, String editorId, String paraIndent)
    {
        // Find paragraphs and tables with class attribute containing CSS class "indent-levelX".
        // Remove "indent-levelX" from class and add style padding-left instead.

        int len = content.length();
        StringBuilder buf = new StringBuilder(len + 1024);
        final String PADDING_PATTERN = getPaddingPattern(editorId);

        // Find paragraphs and tables
        int startpos = 0;
        int copypos = 0;
        while (startpos < len) {
            int tag_start = content.indexOf('<', startpos);
            if (tag_start < 0) break;

            int tag_end = content.indexOf('>', tag_start);
            if (tag_end < 0) break;

            startpos = tag_end + 1;

            // search first whitespace after tagname
            int att_start = tag_start + 1;
            while ((att_start < tag_end) && !Character.isWhitespace(content.charAt(att_start))) att_start++;
            String tagname = content.substring(tag_start + 1, att_start);
            if (! (tagname.equalsIgnoreCase("p") || tagname.equalsIgnoreCase("table"))) continue;
            ++att_start;  // skip first whitespace after tag name

            int att_size = tag_end - att_start;
            if (att_size < 3) continue;  // tag cannot contain attribute
            // if (! Character.isWhitespace(content.charAt(tag_start + 2))) continue;

            StringBuilder p_attribs = new StringBuilder(att_size);
            p_attribs.append(content, att_start, tag_end);

            // Get class and style attribute values
            String class_val = extractAttribute(p_attribs, "class");
            if ((class_val == null) || (class_val.length() == 0)) continue;
            String style_val = extractAttribute(p_attribs, "style");
            String other_attribs = p_attribs.toString().trim();

            // Find and remove indent-levelX CSS class.
            int p = class_val.indexOf(INDENT_CSS_CLASS_PREFIX);
            if (p < 0) continue;
            int p2 = class_val.indexOf(' ', p);
            String indent_cls_name;
            String class_val_others;
            if ((p == 0) && (p2 < 0)) {
                indent_cls_name = class_val;
                class_val_others = "";
            } else {
                if (p2 < 0) p2 = class_val.length();
                indent_cls_name = class_val.substring(p, p2);
                class_val_others = (class_val.substring(0, p).trim() + class_val.substring(p2)).trim();
            }

            // Get indent level from class name
            int indent_level;
            try {
                String level_str = indent_cls_name.substring(INDENT_CSS_CLASS_PREFIX.length());
                indent_level = Integer.parseInt(level_str);
            } catch (NumberFormatException nfe) {
                Log.warning("Invalid indent level CSS class '" + indent_cls_name + "': " + nfe.getMessage());
                continue;
            }

            // Calculate padding-left as indent_level multiplied by indention space
            if ((paraIndent == null) || paraIndent.equals("")) {
                paraIndent = DocmaConstants.DEFAULT_PARA_INDENT;
            }
            String padd_unit = CSSUtil.getSizeUnit(paraIndent);
            String padd_value;
            if (paraIndent.indexOf('.') < 0) {  // no decimal point -> integer
                int padd_int = CSSUtil.getSizeInt(paraIndent) * indent_level;
                padd_value = Integer.toString(padd_int);
            } else {   // float value
                float padd_float = CSSUtil.getSizeFloat(paraIndent) * indent_level;
                padd_value = CSSUtil.formatFloatSize(padd_float);
            }

            // Build new style attribute value which includes padding-left
            String new_style_val = PADDING_PATTERN + padd_value + padd_unit;
            if (style_val != null) {
                // Find and remove any padding-left from style. Set new padding-left.
                StringBuilder style_buf = new StringBuilder(style_val);
                extractPaddingLeft(style_buf, PADDING_PATTERN);
                String other_style_vals = style_buf.toString().trim();
                if (other_style_vals.length() > 0) {
                    new_style_val += "; " + other_style_vals;
                }
            }

            buf.append(content, copypos, tag_start);
            buf.append("<").append(tagname);
            if (class_val_others.length() > 0) {
                buf.append(" class=\"").append(class_val_others).append('"');
            }
            if (new_style_val.length() > 0) {
                buf.append(" style=\"").append(new_style_val).append('"');
            }
            if (other_attribs.length() > 0) {
                buf.append(' ').append(other_attribs);
            }
            buf.append('>');
            copypos = tag_end + 1;
        }

        if (copypos < len) {
            buf.append(content, copypos, len);
        }
        return buf.toString();
    }


    private static String fixIndentForSave(String content, String editorId, String paraIndent)
    {
        // Find paragraphs and tables with style attribute containing padding-left.
        // Remove padding-left from style and add class indent-levelX instead.

        int len = content.length();
        StringBuilder buf = new StringBuilder(len + 1024);
        final String PADDING_PATTERN = getPaddingPattern(editorId);

        // Find paragraphs and tables
        int startpos = 0;
        int copypos = 0;
        while (startpos < len) {
            int tag_start = content.indexOf('<', startpos);
            if (tag_start < 0) break;

            int tag_end = content.indexOf('>', tag_start);
            if (tag_end < 0) break;

            startpos = tag_end + 1;

            // search first whitespace after tagname
            int att_start = tag_start + 1;
            while ((att_start < tag_end) && !Character.isWhitespace(content.charAt(att_start))) att_start++;
            String tagname = content.substring(tag_start + 1, att_start);
            if (! (tagname.equalsIgnoreCase("p") || tagname.equalsIgnoreCase("table"))) continue;
            ++att_start;  // skip first whitespace after tag name

            int att_size = tag_end - att_start;
            if (att_size < 3) continue;  // tag cannot contain attribute
            // if (! Character.isWhitespace(content.charAt(tag_start + 2))) continue;

            StringBuilder p_attribs = new StringBuilder(att_size);
            p_attribs.append(content, att_start, tag_end);

            // Get style and class attribute values
            String style_val = extractAttribute(p_attribs, "style");
            if (style_val == null) continue;
            String class_val = extractAttribute(p_attribs, "class");
            String other_attribs = p_attribs.toString().trim();

            // Get padding-left value
            StringBuilder style_buf = new StringBuilder(style_val);
            String padd_val = extractPaddingLeft(style_buf, PADDING_PATTERN);
            if (padd_val == null) continue;
            float padd_float = CSSUtil.getSizeFloat(padd_val);
            String style_val_others = style_buf.toString().trim();  // other CSS properties

            // Get configured indention space
            if ((paraIndent == null) || paraIndent.equals("")) {
                paraIndent = DocmaConstants.DEFAULT_PARA_INDENT;
            }
            float conf_float = CSSUtil.getSizeFloat(paraIndent);

            // Calculate indention level
            int indent_level = Math.round(padd_float / conf_float);
            if (indent_level > DocmaConstants.MAX_INDENT_LEVELS) {
                indent_level = DocmaConstants.MAX_INDENT_LEVELS;
            }
            String indent_cls_name;
            if (indent_level > 0) {
                indent_cls_name = INDENT_CSS_CLASS_PREFIX + indent_level;
            } else {
                indent_cls_name = "";
            }

            // Insert or replace CSS class for indention.
            if ((class_val == null) || (class_val.length() == 0)) {
                class_val = indent_cls_name;
            } else {
                int p = class_val.indexOf(INDENT_CSS_CLASS_PREFIX);
                if (p < 0) {
                    if (indent_cls_name.length() > 0) {
                        class_val = indent_cls_name + " " + class_val;
                    }
                } else {
                    int p2 = class_val.indexOf(' ', p);
                    if ((p == 0) && (p2 < 0)) {
                        class_val = indent_cls_name;
                    } else {
                        if (p2 < 0) p2 = class_val.length();
                        class_val = (class_val.substring(0, p) + indent_cls_name).trim() +
                                     class_val.substring(p2);
                    }
                }
                class_val = class_val.trim();
            }

            buf.append(content, copypos, tag_start);
            buf.append("<").append(tagname);
            if (class_val.length() > 0) {
                buf.append(" class=\"").append(class_val).append('"');
            }
            if (style_val_others.length() > 0) {
                buf.append(" style=\"").append(style_val_others).append('"');
            }
            if (other_attribs.length() > 0) {
                buf.append(' ').append(other_attribs);
            }
            buf.append('>');
            copypos = tag_end + 1;
        }

        if (copypos < len) {
            buf.append(content, copypos, len);
        }
        return buf.toString();
    }

    private static String fixListIndentForSave(String content, String list_name)
    {
        // The parameter list_name has to be either "ol" or "ul".
        // If list_name is "ol" then
        // search for '<ol> <li>&#160; <ol> ... </ol> </li> </ol>' and
        // replace by '<blockquote><ol> ... </ol></blockquote>'.
        // If list_name is "ul" then
        // search for '<ul> <li>&#160; <ul> ... </ul> </li> </ul>' and
        // replace by '<blockquote><ul> ... </ul></blockquote>'.

        final String START_TAG = "<" + list_name + ">";
        final String END_TAG = "</" + list_name + ">";
        final String LI_PATTERN = "<li>&#160;";
        final String LI_END_TAG = "</li>";

        int searchpos = content.indexOf(LI_PATTERN);
        if (searchpos < 0) return content;  // return content unchanged

        int len = content.length();
        StringBuilder buf = new StringBuilder(len + 1024);

        int copypos = 0;
        while (searchpos < len) {
            int listart = content.indexOf(LI_PATTERN, searchpos);
            if (listart < 0) break;  // no more matching pattern found

            // Start of the outer ol/ul element
            int outerstart = content.lastIndexOf(START_TAG, searchpos);

            searchpos += LI_PATTERN.length();   // continue statement -> continue after this match
            if (outerstart < 0) continue;  // li element was child of another list type -> skip

            // skip whitespace after LI_PATTERN
            while ((searchpos < len) && Character.isWhitespace(content.charAt(searchpos))) searchpos++;

            // If the inner ol/ul element exists
            if ((searchpos < len) && content.regionMatches(searchpos, START_TAG, 0, START_TAG.length())) {

                // Extract the inner ol/ul element
                int innerstart = searchpos;
                int after_inner = getElementEndPos(content, innerstart, START_TAG, END_TAG);
                if (after_inner < 0) continue; // something is wrong here -> continue with next match
                String inner = content.substring(innerstart, after_inner);
                String innerfixed = fixListIndentForSave(inner, list_name); // fix recursively for multiple indented lists

                // Get end position of outer ol/ul element
                int olulend = content.indexOf(END_TAG, after_inner);
                if (olulend < 0) continue;  // something is wrong here -> continue with next match

                String s = content.substring(after_inner, olulend).replace("&#160;", "").trim();
                if (! s.equals(LI_END_TAG)) {
                    continue;  // some other content is here -> skip this list and continue with next match
                }
                int after_outer = olulend + END_TAG.length();

                buf.append(content, copypos, outerstart);
                // buf.append("<blockquote>").append(innerfixed).append("</blockquote>");
                buf.append(START_TAG)
                   .append("<li style=\"list-style-type:none;\">").append(innerfixed).append("</li>")
                   .append(END_TAG);
                copypos = after_outer;
                searchpos = after_outer;
            }
        }

        if (copypos < len) {
            buf.append(content, copypos, len);
        }
        return buf.toString();
    }

    private static String fixBlockquotedListForSave(String content)
    {
        // Search for '<blockquote> <ol> ... </ol> </blockquote>' and
        // replace by '<ol><ol> ... </ol></ol>'.
        // Search for '<blockquote> <ul> ... </ul> </blockquote>' and
        // replace by '<ul><ul> ... </ul></ul>'.

        final String START_TAG = "<blockquote>";
        final String END_TAG = "</blockquote>";

        int searchpos = content.indexOf(START_TAG);
        if (searchpos < 0) return content;  // return content unchanged

        int len = content.length();
        StringBuilder buf = new StringBuilder(len + 1024);

        int copypos = 0;
        while (searchpos < len) {
            // Start of the blockquote element
            int blockstart = content.indexOf(START_TAG, searchpos);
            if (blockstart < 0) break;  // no more matching pattern found

            int innerstart = blockstart + START_TAG.length();
            searchpos = innerstart;   // continue statement -> continue after this match

            int afterblock = getElementEndPos(content, blockstart, START_TAG, END_TAG);
            if (afterblock < 0) continue; // something is wrong here -> continue with next match
            int innerend = afterblock - END_TAG.length();
            String inner = content.substring(innerstart, innerend);
            String innerfixed = fixBlockquotedListForSave(inner); // fix recursively for multiple indented lists

            int pos1 = innerfixed.indexOf('<');
            if (pos1 < 0) continue;
            pos1++;
            int pos2 = pos1;
            String inner_name = null;
            while (pos2 < innerfixed.length()) {
                char ch = innerfixed.charAt(pos2);
                if ((ch == '>') || Character.isWhitespace(ch)) {
                    inner_name = innerfixed.substring(pos1, pos2);
                    break;
                }
                pos2++;
            }
            if (inner_name == null) continue;

            if (inner_name.equals("ol") || inner_name.equals("ul")) {
                String inner_tagstart = "<" + inner_name + ">";
                String inner_tagend = "</" + inner_name + ">";
                buf.append(content, copypos, blockstart);
                buf.append(inner_tagstart)
                   .append("<li style=\"list-style-type:none;\">").append(innerfixed).append("</li>")
                   .append(inner_tagend);
                copypos = afterblock;
                searchpos = afterblock;
            }
        }

        if (copypos < len) {
            buf.append(content, copypos, len);
        }
        return buf.toString();
    }

    private static int getElementEndPos(String content, int offset, String start_tag, String end_tag)
    {
        // Search the matching end tag.
        int searchpos = offset;
        int level = 0;
        while (searchpos < content.length()) {
            searchpos = content.indexOf('<', searchpos);
            if (searchpos < 0) break;

            if (content.regionMatches(searchpos, start_tag, 0, start_tag.length())) {
                level++;
            } else
            if (content.regionMatches(searchpos, end_tag, 0, end_tag.length())) {
                level--;
                if (level == 0) {  // matching end tag was found
                    return searchpos + end_tag.length();
                } else
                if (level < 0) {
                    Log.warning("Class EditContentTransformer: Invalid nested lists.");
                    return -1;
                }
            }
            searchpos++;
        }
        return -1;  // no matching end tag found
    }

    private static String fixLinkURLsForSave(String content)
    {
        return content;
    }


    private static String extractAttribute(StringBuilder attribs, String att_name)
    {
        String ATT_PATTERN = att_name + "=\"";

        int att_start = attribs.indexOf(ATT_PATTERN);
        if (att_start < 0) return null;

        int att_val_start = att_start + ATT_PATTERN.length();
        int att_end = attribs.indexOf("\"", att_val_start);
        if (att_end < 0) return null;   // should not occur

        String res = attribs.substring(att_val_start, att_end);
        att_end++;  // position after double-quote (")
        if ((att_end < attribs.length()) && (attribs.charAt(att_end) == ' ')) {
            att_end++;
        }
        attribs.delete(att_start, att_end);

        // trim spaces at end
        // int last_idx = attribs.length() - 1;
        // while ((last_idx >= 0) && (attribs.charAt(last_idx) == ' ')) {
        //     attribs.deleteCharAt(last_idx--);
        // }

        //  attribs.substring(0, att_start) + attribs.substring(att_end + 1);
        return res;
    }

    private static String extractPaddingLeft(StringBuilder style_val, String PADDING_PATTERN)
    {
        int padd_start = style_val.indexOf(PADDING_PATTERN);
        if (padd_start < 0) return null;

        int padd_val_start = padd_start + PADDING_PATTERN.length();
        int semicol = style_val.indexOf(";", padd_val_start);
        int padd_end = (semicol < 0) ? style_val.length() : semicol;

        String padd_val = style_val.substring(padd_val_start, padd_end);
        // skip semicolon and spaces
        while (padd_end < style_val.length()) {
            int ch = style_val.charAt(padd_end);
            if ((ch == ';') || (ch == ' ')) padd_end++;
            else break;
        }
        style_val.delete(padd_start, padd_end);
        // (style_val.substring(0, padd_start) + style_val.substring(padd_end)).trim();
        return padd_val;
    }

    private static String getPaddingPattern(String editorId)
    {
        if (editorId.startsWith("tinymce_3_3")) {
            return "padding-left:";
        } else {
            return "margin-left:";
        }
    }
    
}
