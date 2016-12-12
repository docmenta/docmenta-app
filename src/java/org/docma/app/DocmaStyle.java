/*
 * DocmaStyle.java
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

import org.docma.plugin.Style;
import org.docma.coreapi.DocException;
import org.docma.util.Log;
import java.util.*;

/**
 *
 * @author MP
 */
public class DocmaStyle implements Style, Comparable, Cloneable
{
    public static final String AUTO_FORMAT_CLASS_FORMAL = "org.docma.app.AutoFormalExport";
    private static final String[] EXPORT_AUTO_FORMAT_CLASSES = { AUTO_FORMAT_CLASS_FORMAL };

    public static String INLINE_STYLE = "inline";
    public static String BLOCK_STYLE = "block";

    public static final char VARIANT_DELIMITER = '-';

    private String style_id;
    private String style_name;
    private String style_type;
    private String style_css;
    private String style_autoformat;
    private ArrayList<AutoFormatCall> autoformat_calls;
    private boolean hidden = false;


    public DocmaStyle(String style_id,
                      String style_type,
                      String style_name,
                      String style_css)
    {
        this(style_id, style_type, style_name, style_css, null);
    }

    public DocmaStyle(String style_id,
                      String style_type,
                      String style_name,
                      String style_css,
                      String autoformat)
    {
        this(style_id, style_type, style_name, style_css, autoformat, false);
    }

    public DocmaStyle(String style_id, 
                      String style_type,
                      String style_name,
                      String style_css,
                      String autoformat,
                      boolean hidden)
    {
        this.style_id = style_id;
        this.style_name = style_name;
        this.style_type = style_type;
        this.style_css = style_css;
        this.style_autoformat = autoformat;
        this.autoformat_calls = null;  // will be initialized on demand (in get method)
        this.hidden = hidden;
    }

    public String getId()
    {
        return style_id;
    }

    public void setId(String style_id)
    {
        this.style_id = style_id;
    }

    public String getCSS()
    {
        return (style_css == null) ? "" : style_css;
    }

    public void setCSS(String style_css)
    {
        this.style_css = style_css;
    }

    public String getName()
    {
        return style_name;
    }

    public void setName(String style_name)
    {
        this.style_name = style_name;
    }

    /**
     * Returns the style title. Same as <code>getName()</code>.
     * This method is part of the <code>org.docma.plugin.Style</code> interface.
     * 
     * @return  a descriptive style name
     */
    public String getTitle()
    {
        return style_name;
    }
    
    /**
     * Sets the style title. Same as <code>setName(String)</code>.
     * This method is part of the <code>org.docma.plugin.Style</code> interface.
     * 
     * @param styleTitle  a descriptive style name
     */
    public void setTitle(String styleTitle)
    {
        this.style_name = styleTitle;
    }

    public String getType()
    {
        return style_type;
    }

    public void setType(String style_type)
    {
        this.style_type = style_type;
    }

    public boolean isInlineStyle()
    {
        return style_type.equalsIgnoreCase(INLINE_STYLE);
    }

    public boolean isBlockStyle()
    {
        return style_type.equalsIgnoreCase(BLOCK_STYLE);
    }

    public boolean isInternalStyle()
    {
        return DocmaStyleUtil.isInternalStyle(getBaseId());
    }
    
    public boolean isVariant()
    {
        return (style_id.indexOf(VARIANT_DELIMITER) >= 0);
    }

    public String getBaseId()
    {
        int pos = style_id.indexOf(VARIANT_DELIMITER);
        if (pos < 0) {
            return style_id;
        } else {
            return style_id.substring(0, pos);
        }
    }

    public String getVariantId()
    {
        int pos = style_id.indexOf(VARIANT_DELIMITER);
        if (pos < 0) {
            return null;
        } else {
            return style_id.substring(pos + 1);
        }
    }

    /**
     * Same as <code>getVariantId()</code>.
     * This method is part of the <code>org.docma.plugin.Style</code> interface.
     * 
     * @return  the variant name
     */
    public String getVariantName()
    {
        return getVariantId();
    }
    
    public String getAutoFormatString()
    {
        return (style_autoformat != null) ? style_autoformat : "";
    }

    public boolean hasAutoFormatCall()
    {
        return (style_autoformat != null) && (style_autoformat.trim().length() > 0);
    }

    public AutoFormatCall[] getAutoFormatCalls()
    {
        initAutoFormat();
        return autoformat_calls.toArray(new AutoFormatCall[autoformat_calls.size()]);
    }

    public void setAutoFormatCalls(AutoFormatCall[] newCalls)
    {
        if (newCalls == null) {
            style_autoformat = null;
            autoformat_calls = null;
            return;
        }
        if (autoformat_calls == null) {
            autoformat_calls = new ArrayList<AutoFormatCall>();
        } else {
            autoformat_calls.clear();
        }
        autoformat_calls.addAll(Arrays.asList(newCalls));
        updateAutoFormatString();
    }

    public AutoFormatCall[] getAutoFormatCalls(boolean includeExportCalls)
    {
        if (includeExportCalls) {
            return getAutoFormatCalls();  // return all
        }
        initAutoFormat();
        int offset = 0;
        while (offset < autoformat_calls.size()) {
            AutoFormatCall afc = autoformat_calls.get(offset);
            if (! Arrays.asList(EXPORT_AUTO_FORMAT_CLASSES).contains(afc.getClassName())) break;
            ++offset;
        }
        AutoFormatCall[] arr = new AutoFormatCall[autoformat_calls.size() - offset];
        for (int i=offset; i < autoformat_calls.size(); i++) {
            arr[i - offset] = autoformat_calls.get(i);
        }
        return arr;
    }

    public boolean isFormal()
    {
        if ((style_autoformat == null) || style_autoformat.equals("")) {
            return false;
        }
        initAutoFormat();
        return (getCallIndex(AUTO_FORMAT_CLASS_FORMAL) >= 0);
    }

    public AutoFormatCall getFormalCall()
    {
        initAutoFormat();
        int idx = getCallIndex(AUTO_FORMAT_CLASS_FORMAL);
        return (idx < 0) ? null : autoformat_calls.get(idx);
    }
    
    private int getCallIndex(String clsname)
    {
        for (int i=0; i < autoformat_calls.size(); i++) {
            if (clsname.equals(autoformat_calls.get(i).getClassName())) return i;
        }
        return -1;
    }

    public String getFormalLabelId()
    {
        initAutoFormat();
        int formal_idx = getCallIndex(AUTO_FORMAT_CLASS_FORMAL);
        if (formal_idx >= 0) {
            AutoFormatCall fcall = autoformat_calls.get(formal_idx);
            if (fcall.getArgumentCount() == 0) return null;
            return fcall.getArgument(0).trim();
        } else {
            return null;
        }
    }

    public boolean isHidden()
    {
        return hidden;
    }

    public void setHidden(boolean hidden)
    {
        this.hidden = hidden;
    }

    public int compareTo(Object obj)
    {
        DocmaStyle other = (DocmaStyle) obj;
        return getId().compareToIgnoreCase(other.getId());
    }

    public Object clone() throws CloneNotSupportedException
    {
        DocmaStyle cloneObj = (DocmaStyle) super.clone();
        if (this.autoformat_calls != null) {
            cloneObj.autoformat_calls = (ArrayList<AutoFormatCall>) this.autoformat_calls.clone();
        }
        return cloneObj;
    }


    /* --------  public static methods  --------- */

    public static String getCSS(DocmaStyle[] arr, boolean includeMetadata)
    {
        return getCSS(arr, includeMetadata, true);
    }

    public static String getCSS(DocmaStyle[] arr, boolean includeMetadata, boolean includeVariantId)
    {
        StringBuffer buf = new StringBuffer(8*1024);
        for (int i=0; i < arr.length; i++) {
            DocmaStyle style = arr[i];
            if (includeMetadata) {
                buf.append("/* Name:\"").append(style.getName()).append("\"");
                if (style.isHidden()) {
                    buf.append(" Hidden:\"true\"");
                }
                String af_string = style.getAutoFormatString();
                if (af_string.length() > 0) {
                    buf.append(" Autoformat:\"").append(af_string).append("\"");
                }
                buf.append(" */ \n");
            }
            String s_id = includeVariantId ? style.getId() : style.getBaseId();
            buf.append('.').append(s_id).append(" { ")
               .append(style.getCSS()).append(" }\n");
        }
        return buf.toString();
    }


    public static DocmaStyle[] parseCSS(String css, boolean inline) throws DocException
    {
        css = css.replaceAll("[\n\r]", "");
        List styles = new ArrayList();
        String s_type = inline ? INLINE_STYLE : BLOCK_STYLE;
        int start = 0;
        int maxLen = css.length();
        while (start < maxLen) {
            // Note: Better replace this implementation with pattern matcher:
            // final String pattern = "\\.(" + DocmaConstants.REGEXP_STYLE_ID + ")\\s*\\{";
            // final Pattern css_class_start = Pattern.compile(pattern);

            int pos = css.indexOf('.', start);
            if (pos < 0) break;
            start = pos + 1;  // continue next search loop after this match

            // Style name, hidden flag and autoformat string are encoded in
            // comment: /* Name:"..." Hidden:"true" Autoformat:"..." */
            String s_name = "";
            String autoformat = "";
            boolean is_hidden = false;
            int idx = pos;
            char ch = ' ';
            // skip whitespace characters
            while (--idx > 0) {
                ch = css.charAt(idx);
                if (! Character.isWhitespace(ch)) break;
            }
            if ((ch == '/') && (css.charAt(idx-1) == '*')) {  // end of comment: */
                int comment_start = css.lastIndexOf("/*", idx);  // start of comment: /* name */
                if (comment_start >= 0) {
                    String comment = css.substring(comment_start + 2, idx-1).trim();
                    final String NAME_PATTERN = "Name:\"";
                    final String HIDDEN_PATTERN = "Hidden:\"true\"";
                    final String AF_PATTERN = "Autoformat:\"";
                    int name_start = comment.indexOf(NAME_PATTERN);
                    if (name_start < 0) {  // Docmenta version 1.1 and below
                        s_name = comment;
                    } else {  // Docmenta version 1.2 and later
                        // Extract style name
                        name_start += NAME_PATTERN.length();
                        int name_end = comment.indexOf('"', name_start);
                        if (name_end > 0) {
                            s_name = comment.substring(name_start, name_end);
                        }
                        // Extract hidden flag
                        is_hidden = comment.contains(HIDDEN_PATTERN);
                        // Extract auto-format string
                        int af_start = comment.indexOf(AF_PATTERN);
                        if (af_start >= 0) {
                            af_start += AF_PATTERN.length();
                            int af_end = comment.indexOf('"', af_start);
                            if (af_end > 0) {
                                autoformat = comment.substring(af_start, af_end);
                            }
                        }
                    }
                }
            }
            if ((s_name.length() > 0) && !s_name.matches(DocmaConstants.REGEXP_STYLE_NAME)) {
                s_name = "";   // avoid invalid style name
            }

            idx = pos;
            ch = ' ';
            // read css class name; skip whitespace characters
            while (++idx < css.length()) {
                ch = css.charAt(idx);
                if (Character.isWhitespace(ch) || (ch == '{')) break;
            }
            String s_id = css.substring(pos + 1, idx);
            if (! s_id.matches(DocmaConstants.REGEXP_STYLE_BASE_ID)) {
                continue;  // no valid CSS class name, i.e. this dot is not start of a css class
            }
            if (Character.isWhitespace(ch)) {  // ch is whitespace or end of css string reached
                while (++idx < css.length()) {  // skip until first non-whitespace
                    ch = css.charAt(idx);
                    if (! Character.isWhitespace(ch)) break;
                }
            }
            if (ch != '{') {
                continue;  // next non-whitespace is not a {, i.e. no valid start of CSS class
            } else {
                int pos2 = idx;  // position of {
                int pos3 = css.indexOf('}', pos2);
                if (pos3 >= 0) {
                    String css_str = css.substring(pos2 + 1, pos3).trim();

                    if (s_name.equals("")) s_name = s_id;
                    DocmaStyle style = new DocmaStyle(s_id, s_type, s_name, css_str, autoformat, is_hidden);
                    styles.add(style);

                    start = pos3 + 1;
                    continue;
                }
            }
            throw new DocException("Cannot import styles: invalid syntax.");
        }  // end of loop while start < maxLen
        DocmaStyle[] arr = new DocmaStyle[styles.size()];
        return (DocmaStyle[]) styles.toArray(arr);
    }

    /* --------  private methods  --------- */

    private void initAutoFormat()
    {
        if (autoformat_calls != null) {
            return;  // already initialized
        }
        autoformat_calls = new ArrayList<AutoFormatCall>();
        if ((style_autoformat == null) || style_autoformat.equals("")) {
            return;
        }
        String af_string = style_autoformat.trim();
        int pos = 0;
        while (pos < af_string.length()) {
            int arg_start = af_string.indexOf('(', pos);
            if (arg_start < 0) {
                Log.warning("Skipping invalid auto-format-call syntax!");
                return;
            }
            int arg_end = af_string.indexOf(')', arg_start);
            if (arg_end < 0) {
                Log.warning("Skipping invalid auto-format-call syntax!");
                return;
            }
            String clsname = af_string.substring(pos, arg_start).trim();
            String args = af_string.substring(arg_start+1, arg_end).trim();
            if (clsname.equals("")) {
                Log.warning("Skipping invalid auto-format-call: Missing class name.");
                return;
            }
            autoformat_calls.add(new AutoFormatCall(clsname, args));

            pos = arg_end + 1;
        }
    }

    private void updateAutoFormatString()
    {
        StringBuilder buf = new StringBuilder(200);
        for (int i=0; i < autoformat_calls.size(); i++) {
            AutoFormatCall afc = autoformat_calls.get(i);
            if (i > 0) buf.append(" ");
            buf.append(afc.getClassName()).append("(").append(afc.getArgumentsLine()).append(")");
        }
        style_autoformat = buf.toString();
    }
}
