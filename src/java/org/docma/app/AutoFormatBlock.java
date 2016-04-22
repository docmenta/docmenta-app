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
    private StringWriter outWriter;
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
        this.outWriter = new StringWriter(input.length() + 4096);

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
        return outWriter.getBuffer();
    }

    boolean isImageWithTitle()
    {
        return getTagName().equals("img") && tagAttribs.containsKey("title");
    }

    String getStyleID()
    {
        return tagAttribs.get("class");
    }

    boolean isStyleRecursion()
    {
        return styleRecursion;
    }

    /* ------------  Interface TransformationContext  -------------- */

    public int getArgumentCount()
    {
        return call.getArgumentCount();
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
        return outWriter;
    }

    public void setStyleRecursion(boolean styleRecursion)
    {
        this.styleRecursion = styleRecursion;
    }

    /* ------------  Private methods  -------------- */

}
