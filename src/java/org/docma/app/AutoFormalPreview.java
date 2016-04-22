/*
 * AutoFormalPreview.java
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

import org.docma.plugin.*;
import java.util.*;
import java.io.*;

/**
 *
 * @author MP
 */
public class AutoFormalPreview implements AutoFormat
{
    private DocmaExportContext exportCtx;
    private DocmaSession docmaSess;
    private DocmaOutputConfig outConf;
    private boolean isTitleAfter;

    public void initialize(ExportContext ctx)
    {
        exportCtx = (DocmaExportContext) ctx;
        docmaSess = exportCtx.getDocmaSession();
        outConf = exportCtx.getOutputConfig();
        isTitleAfter = outConf.getTitlePlacement().equalsIgnoreCase("after");
    }

    public void finished()
    {
        exportCtx = null;
        docmaSess = null;
        outConf = null;
    }

    /**
     * This transformation adds a caption line to the formal block.
     *
     * @param ctx
     * @throws Exception
     */
    public void transform(TransformationContext ctx) throws Exception
    {
        Writer out = ctx.getWriter();
        Map<String, String> atts = ctx.getTagAttributes();
        String styleID = atts.get("class");
        DocmaStyle docStyle = (styleID != null) ? exportCtx.getStyle(styleID) : null;
        if (docStyle == null) {
            exportCtx.logError("Cannot apply preview transformation on formal block. Style with ID '" +
                               styleID + "' not found!");
            out.write(ctx.getOuterString());  // do not change block
            return;
        }
        if (!docStyle.isFormal()) {
            exportCtx.logError("Cannot apply preview transformation on formal block. Style with ID '" +
                               styleID + "' is not a formal block-style!");
            out.write(ctx.getOuterString());  // do not change block
            return;
        }

        String labelID = docStyle.getFormalLabelId();
        if (labelID == null) labelID = styleID;  // fallback
        String titlePattern = (labelID != null) ? exportCtx.getGenTextProperty("title|" + labelID) : null;
        if ((titlePattern == null) || titlePattern.trim().equals("")) {
            titlePattern = "%t";
        }

        String title = atts.get("title");
        if (title == null) {
            title = "";
        }

        if (! isTitleAfter) {
            writeCaption(out, titlePattern.replace("%t", title));
        }
        out.write(ctx.getOuterString());
        if (isTitleAfter) {
            writeCaption(out, titlePattern.replace("%t", title));
        }
    }

    private void writeCaption(Writer out, String title) throws Exception
    {
        out.write("<p class=\"caption\">");
        out.write(title);
        out.write("</p>");
    }

    public String getShortInfo(String languageCode)
    {
        return "Add caption line to formal blocks.";
    }

    public String getLongInfo(String languageCode)
    {
        return "";
    }


}
