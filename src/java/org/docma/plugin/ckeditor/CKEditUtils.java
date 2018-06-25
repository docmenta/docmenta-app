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
import org.docma.util.XMLUtil;

/**
 *
 * @author MP
 */
public class CKEditUtils 
{
    public static String prepareContentForEdit(String content, String editorId, WebUserSession webSess)
    {
        // If the figure tag is enabled for the opened store, then add
        // the configured figure class to all figure elements (if missing).
        // If no figure class has been configured, then add a fallback
        // figure class. Otherwise the CKEditor does not create a widget for 
        // the figure. The fallback figure class will be removed in
        // method prepareContentForSave().
        
        CKEditPlugin plug = CKEditHandler.getRegisteredPlugin(editorId);
        StoreConnection store = webSess.getOpenedStore();
        boolean figureEnabled = true;
        
        // Get CKEditor figure configuration for the opened store 
        if ((plug != null) && (store != null)) {  // should always be true
            figureEnabled = plug.isFigureTagEnabled(store.getStoreId());
        }

        if (figureEnabled) {
            String figCls = null;
            if (plug != null) {
                figCls = plug.getImgCaptionedClass();
                if (figCls != null) {
                    figCls = figCls.trim();
                }
            }
            if ((figCls == null) || figCls.equals("")) {
                figCls = CKEditHandler.FALLBACK_FIGURE_CSS_CLS;
            }
            try {
                content = XMLUtil.addCSSClass(content, "figure", figCls);
            } catch(Exception ex) {}  // invalid XML?
        }
        return content;
    }

    public static String prepareContentForSave(String content, String editorId, WebUserSession webSess)
    {
        // If the figure tag is enabled for the opened store, and no 
        // figure class is configured, then remove the fallback figure class
        // from all figure elements (which has been added in method 
        // prepareContentForEdit(...)).
        
        CKEditPlugin plug = CKEditHandler.getRegisteredPlugin(editorId);
        StoreConnection store = webSess.getOpenedStore();
        boolean figureEnabled = true;
        
        // Get CKEditor figure configuration for the opened store 
        if ((plug != null) && (store != null)) {  // should always be true
            figureEnabled = plug.isFigureTagEnabled(store.getStoreId());
        }

        if (figureEnabled) {
            String figCls = null;
            if (plug != null) {
                figCls = plug.getImgCaptionedClass();
                if (figCls != null) {
                    figCls = figCls.trim();
                }
            }
            if ((figCls == null) || figCls.equals("")) {
                figCls = CKEditHandler.FALLBACK_FIGURE_CSS_CLS;
                try {
                    content = XMLUtil.removeCSSClass(content, "figure", figCls);
                } catch(Exception ex) {}  // invalid XML?
            }
        }
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
