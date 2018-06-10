/*
 * CKEditUtils.java
 * 
 *  Copyright (C) 2018  Manfred Paula, http://www.docmenta.org
 *   
 *  This file is part of Docmenta. Docmenta is free software: you can 
 *  redistribute it and/or modify it under the terms of the GNU Lesser 
 *  General Public License as published by the Free Software Foundation, 
 *  either version 3 of the License, or (at your option) any later version.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Docmenta.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.docma.plugin.ckeditor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.docma.app.FigureImgConverter;
import org.docma.plugin.*;
import org.docma.plugin.web.WebUserSession;

/**
 *
 * @author MP
 */
public class CKEditUtils 
{
    public static String prepareContentForEdit(String content, String editorId, WebUserSession webSess)
    {
        return content;
    }
    
//    public static String prepareContentForEdit(String content, String editorId, WebUserSession webSess)
//    {
//        CKEditPlugin plug = CKEditHandler.getRegisteredPlugin(editorId);
//        StoreConnection store = webSess.getOpenedStore();
//        boolean figureEnabled = true;
//        
//        // Get CKEditor figure configuration for the opened store 
//        if ((plug != null) && (store != null)) {  // should always be true
//            figureEnabled = plug.isFigureTagEnabled(store.getStoreId());
//        }
//
//        // If the figure tag is enabled for the opened store, then convert
//        // img tags that have a title attribute (but are not already inside of  
//        // a figure tag) to figure with figcaption.
//        if (figureEnabled && (plug != null)) {
//            String figCls = plug.getImgCaptionedClass();
//            try { 
//                content = FigureImgConverter.imgToFigure(content, figCls);
//            } catch (Exception ex) {}   // invalid XML?
//        }
//        return content;
//    }
    
    public static void writeStyleList_JSON(Appendable out, StoreConnection storeConn)
    throws IOException
    {
        out.append("[");
        Style[] styles = storeConn.getStyles();
        List<Style> blockstyles = new ArrayList<Style>(styles.length);
        
        // Inline Styles
        for (Style style : styles) {
            String sid = style.getId();
            if (! (style.isVariant() || style.isHidden() || style.isInternalStyle() || "indexterm".equals(sid))) {
                if (style.isInlineStyle()) {
                    writeStyleObject(out, sid, style.getTitle(), "span");
                    out.append(",");
                } else {
                    blockstyles.add(style);
                }
            }
        }
        
        writeStyleObject(out, "indexterm", "Index Term", "span");
        
        // Block styles
        for (Style style : blockstyles) {
            out.append(",");
            writeStyleObject(out, style.getId(), style.getTitle(), "div");
        }
        
        // Write extra styles, e.g. keep_together
        // ...
        
        out.append("]");
    }
    
    private static void writeStyleObject(Appendable out, String id, String title, String tagName)
    throws IOException
    {
        String jsTitle = title.replace('"', ' ').replace('\\', ' ');
        out.append(" { \"name\": \"").append(jsTitle)
           .append("\", \"element\": \"").append(tagName)
           .append("\", \"attributes\": { \"class\": \"").append(id).append("\" } }");
    }
}
