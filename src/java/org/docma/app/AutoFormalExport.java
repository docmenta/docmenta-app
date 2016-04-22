/*
 * AutoFormalExport.java
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
public class AutoFormalExport implements AutoFormat
{
    private DocmaExportContext exportCtx;
    private DocmaSession docmaSess;
    private DocmaOutputConfig outConf;


    public void initialize(ExportContext ctx)
    {
        exportCtx = (DocmaExportContext) ctx;
        docmaSess = exportCtx.getDocmaSession();
        outConf = exportCtx.getOutputConfig();
    }

    public void finished()
    {
        exportCtx = null;
        docmaSess = null;
        outConf = null;
    }

    /**
     *
     * @param ctx
     * @throws Exception
     */
    public void transform(TransformationContext ctx) throws Exception
    {
        // Input is a block which has a formal block-style assigned, e.g.:
        //   <div class="listing" id="mycode" title="An example listing">
        //      ...
        //   </div>
        //
        // Given this example, a formal block-style with ID 'listing' has to exist.
        // If the label-ID which is assigned to this style is 'label_listing',
        // then the output of this transformation has to be as follows:
        //   <div class="_formal_label_listing" id="mycode" title="An example listing">
        //     <div class="listing">
        //        ...
        //     </div>
        //   </div>
        //
        // Note: During export this will be transformed by the html2docbook.xsl
        //       stylesheet to following:
        //   <example role="label_listing" id="mycode" title="An example listing">
        //     <para role="listing">
        //        ...
        //     </para>
        //   </example>

        Writer out = ctx.getWriter();
        Map<String, String> atts = ctx.getTagAttributes();
        String styleID = atts.get("class");
        DocmaStyle docStyle = (styleID != null) ? exportCtx.getStyle(styleID) : null;
        if (docStyle == null) {
            exportCtx.logError("Cannot apply export transformation on formal block. Style with ID '" +
                               styleID + "' not found!");
            out.write(ctx.getOuterString());  // do not change block
            return;
        }
        if (!docStyle.isFormal()) {
            exportCtx.logError("Cannot apply export transformation on formal block. Style with ID '" +
                               styleID + "' is not a formal block-style!");
            out.write(ctx.getOuterString());  // do not change block
            return;
        }

        String labelID = docStyle.getFormalLabelId();
        if (labelID == null) labelID = styleID;  // fallback

        String blockID = atts.get("id");
        String blockTitle = atts.get("title");

        // Open outer div
        out.append("<div class=\"_formal_").append(labelID).append("\"");
        if ((blockID != null) && (blockID.length() > 0)) {
            out.append(" id=\"").append(blockID).append("\"");
        }
        if (blockTitle != null) {
            blockTitle = blockTitle.replace("\"", "&#34;");
            out.append(" title=\"").append(blockTitle).append("\"");
        }
        out.append(">");
        // Open inner block
        String tagname = ctx.getTagName();
        out.append("<").append(tagname);
        Iterator<String> it = atts.keySet().iterator();
        while (it.hasNext()) {
            String attname = it.next();
            if (! (attname.equals("id") || attname.equals("title"))) {
                String val = atts.get(attname);
                val = val.replace("\"", "&#34;");
                out.append(" ").append(attname).append("=\"").append(val).append("\"");
            }
        }
        out.append(">");
        out.append(ctx.getInnerString());
        // Close inner block
        out.append("</").append(tagname).append(">");
        // Close outer div
        out.append("</div>");
    }

    public String getShortInfo(String languageCode)
    {
        return "Prepare formal block for export-stylesheet html2docbook.xsl.";
    }

    public String getLongInfo(String languageCode)
    {
        return "";
    }

}
