/*
 * AutoFormatBlock.java
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
import org.docma.plugin.*;

/**
 *
 * @author MP
 */
public class AutoFormatBlock implements TransformationContext
{
    private AutoFormatCall call;
    private String input;
    private int startOffset;
    private int endOffset;
    private int innerStartOffset;
    private int innerEndOffset;
    private String outerString = null;
    private String innerString = null;
    private String tagName;
    private Map<String, String> tagAttribs = null;
    private StringWriter outWriter = null;
    private boolean styleRecursion = false;  // default is false

    /* --------------  Constructor  ---------------------- */

    AutoFormatBlock(String input,
                    String tagName,
                    List attNames,
                    List attValues,
                    int startPos,
                    int endPos,
                    int innerStartPos,
                    int innerEndPos)
    {
        this.input = input;
        this.tagName = tagName.toLowerCase();
        int attCount = attNames.size();
        if (attCount > 0) {
            tagAttribs = new HashMap<String, String>(3 * attCount);
            for (int i=0; i < attCount; i++) {
                tagAttribs.put((String) attNames.get(i), (String) attValues.get(i));
            }
        }
        this.startOffset = startPos;
        this.endOffset = endPos;
        this.innerStartOffset = innerStartPos;
        this.innerEndOffset = innerEndPos;

        // Note: this.call has to be set via setAutoFormatCall(...) before
        // transformation is done.
    }

    /* --------------  Package local methods  ---------------------- */

    void setAutoFormatCall(AutoFormatCall call)
    {
        this.call = call;
    }

    int getStartOffset()
    {
        return startOffset;
    }

    int getEndOffset()
    {
        return endOffset;
    }

    StringBuffer getOutputBuffer()
    {
        initOutWriter();
        return outWriter.getBuffer();
    }

    boolean isImageWithTitle()
    {
        return getTagName().equals("img") && (tagAttribs != null) && tagAttribs.containsKey("title");
    }
    
    boolean transformTargetTitleLink(DocmaExportContext exportCtx)
    {
        if (tagAttribs == null) {
            return false;
        }
        String hrefVal = tagAttribs.get("href");
        if ((hrefVal != null) && getTagName().equals("a")) {
            final String TARGET_PATT = "%target%";
            String title = tagAttribs.get("title");
            String linktxt = getInnerTrimmed();
            boolean isTitleTarget = (title != null) && title.startsWith(TARGET_PATT);
            boolean isTxtTarget = linktxt.startsWith(TARGET_PATT); 
            if (! (isTitleTarget || isTxtTarget)) {
                return false;
            }
            final String IMAGE_PREFIX = "image/";
            final String FILE_PREFIX = "file/";
            boolean isContLink = hrefVal.startsWith("#");
            boolean isImageLink = hrefVal.startsWith(IMAGE_PREFIX);
            boolean isFileLink = hrefVal.startsWith(FILE_PREFIX);
            
            String targetAlias;
            if (isContLink) targetAlias = hrefVal.substring(1).trim();
            else if (isImageLink) targetAlias = hrefVal.substring(IMAGE_PREFIX.length()).trim();
            else if (isFileLink) targetAlias = hrefVal.substring(FILE_PREFIX.length()).trim();
            else targetAlias = "";
            if (targetAlias.equals("")) {
                return false;
            }
         
            DocmaSession docmaSess = exportCtx.getDocmaSession();
            DocmaNode nd = docmaSess.getNodeByAlias(targetAlias);
            String targetTitle = null; 
            if (nd != null) {
                if (targetAlias.equals(nd.getAlias())) {
                    targetTitle = nd.getTitleEntityEncoded();
                } else {
                    DocmaAnchor anch = nd.getContentAnchorByAlias(targetAlias);
                    if (anch != null) {
                        targetTitle = anch.getTitle();
                        if (targetTitle == null) {
                            targetTitle = hrefVal;
                        }
                    }
                }
            }
            if (targetTitle == null) {
                targetTitle = docmaSess.getI18n().getLabel("text.unknown_target_alias", targetAlias);
            } else if (targetTitle.equals("")) {
                targetTitle = hrefVal;
            }
            
            boolean rewriteTag = (innerStartOffset < 0);  // rewrite tag if empty: change <a /> to <a>...</a>
            if (isTxtTarget && !isTitleTarget) {
                tagAttribs.put("title", linktxt.replace("<", "&lt;").replace(">", "&gt;"));
                rewriteTag = true;
            }
            StringBuilder sb = new StringBuilder((endOffset - startOffset) + 80);
            if (rewriteTag) {  // rewrite opening a tag if empty or title attribute added   
                sb.append("<a");
                if (tagAttribs != null) {
                    for (String attname : tagAttribs.keySet()) {
                        String val = tagAttribs.get(attname);
                        val = (val == null) ? "" : val.replace("\"", "&quot;");
                        sb.append(" ").append(attname).append("=\"").append(val).append("\"");
                    }
                }
                sb.append(">");
            } else {
                // Opening tag is unchanged
                sb.append(input.substring(startOffset, innerStartOffset));
            }
            startOffset = 0;
            innerStartOffset = sb.length();
            sb.append("'").append(targetTitle).append("'");
            innerEndOffset = sb.length();
            sb.append("</a>");
            endOffset = sb.length();
            input = sb.toString();
            
            // update/clear cached values:
            outerString = input;
            innerString = null;
            return true;
        }
        return false;
    }

    String getClassValue()
    {
        return (tagAttribs == null) ? null : tagAttribs.get("class");
    }

    boolean isStyleRecursion()
    {
        return styleRecursion;
    }

    /* ------------  Interface TransformationContext  -------------- */

    public int getArgumentCount()
    {
        return (call == null) ? 0 : call.getArgumentCount();
    }

    public String getArgument(int idx)
    {
        if (idx >= call.getArgumentCount()) return null;
        return call.getArgument(idx);
    }

    public String getArgument(int idx, String defaultValue)
    {
        if (idx >= call.getArgumentCount()) return defaultValue;
        return call.getArgument(idx);
    }

    public String getInnerString()
    {
        if (innerString == null) {
            if ((innerStartOffset < 0) || (innerEndOffset < 0)) {  // empty element
                innerString = "";
            } else {
                innerString = input.substring(innerStartOffset, innerEndOffset);
            }
        }
        return innerString;
    }

    public String getOuterString()
    {
        if (outerString == null) {
            outerString = input.substring(startOffset, endOffset);
        }
        return outerString;
    }

    public String getTagName()
    {
        // return tag name in lower-case
        return tagName;
    }

    public Map<String, String> getTagAttributes()
    {
        if (tagAttribs == null) {
            tagAttribs = new HashMap<String, String>();  // no attributes exist, return empty map
        }
        return tagAttribs;
    }

    public Writer getWriter()
    {
        initOutWriter();
        return outWriter;
    }

    public void setStyleRecursion(boolean styleRecursion)
    {
        this.styleRecursion = styleRecursion;
    }

    /* ------------  Private methods  -------------- */

    private void initOutWriter()
    {
        if (this.outWriter == null) {
            this.outWriter = new StringWriter(input.length() + 4096);
        }
    }
    
    private String getInnerTrimmed()
    {
        String in = getInnerString();
        int len = in.length();
        int pos = 0;
        while (pos < len) {
            char ch = in.charAt(pos);
            if (Character.isWhitespace(ch)) {
                pos++;
                continue;
            } else if ((ch == '&') && 
                       (in.regionMatches(pos, "&nbsp;", 0, 6) || 
                        in.regionMatches(pos, "&#160;", 0, 6))) {
                pos += 6;
                continue;
            }
            break;  // character is no whitespace
        }
        return (pos == 0) ? in : in.substring(pos);
    }
}
