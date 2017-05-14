/*
 * DocmaStyleUtil.java
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
package org.docma.app;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author MP
 */
public class DocmaStyleUtil 
{
    private static Set<String> internalInlineStyles = null;
    private static Set<String> internalBlockStyles = null;
    private static Set<String> virtualStyles = null;

    private DocmaStyleUtil()
    {
    }

    /**
     * Indicates whether the supplied style is an internal style.
     * An internal style defines the formatting of generated parts in the
     * exported publication. In other words, an internal style is assigned by
     * the formatter during the export process.
     * Therefore, an internal style shall <em>not</em> be 
     * assigned to content directly. An internal style may define the  
     * formatting for all or only for specific output formats.
     * <p>
     * For example, the internal style <code>header1</code> defines the 
     * formatting of generated first-level headers for all output formats. 
     * On the other side, the internal style <code>webhelptitle1</code> defines 
     * the formatting of the headline in the header area for WebHelp output
     * only.
     * </p>
     * 
     * @param styleID  the style identifier
     * @return  <code>true</code> if the style is an internal style; 
     *          <code>false</code> otherwise
     */    
    public static boolean isInternalStyle(String styleID)
    {
        return isInternalBlockStyle(styleID) || isInternalInlineStyle(styleID);
    }

    public static boolean isInternalBlockStyle(String styleID)
    {
        if (internalBlockStyles == null) {
            initInternalBlockStyleSet();
        }
        return internalBlockStyles.contains(styleID);
    }

    public static boolean isInternalInlineStyle(String styleID)
    {
        if (internalInlineStyles == null) {
            initInternalInlineStyleSet();
        }
        return internalInlineStyles.contains(styleID);
    }

    /**
     * Indicates whether the supplied style is a virtual style.
     * A virtual style can be assigned to content as any other user-defined 
     * style. However, a virtual style has a pre-defined meaning. 
     * Therefore, assigning CSS properties to a virtual style may have no 
     * effect, because the formatting properties may be pre-defined.
     * For example, the virtual style <code>indent-level1</code> indents the
     * block to which the style is assigned by one level.
     * 
     * @param styleID  the style identifier
     * @return  <code>true</code> if the style is a virtual style; 
     *          <code>false</code> otherwise
     */
    public static boolean isVirtualStyle(String styleID)
    {
        if (virtualStyles == null) {
            initVirtualStyleSet();
        }
        return virtualStyles.contains(styleID);
    }

    /**
     * Indicates whether the supplied style has a pre-defined meaning.
     * 
     * @param styleID  the style identifier
     * @return  <code>true</code> if the style is a virtual style; 
     *          <code>false</code> otherwise
     */    
    public static boolean isPredefinedStyle(String styleID)
    {
        return isInternalStyle(styleID) || isVirtualStyle(styleID);
    }
    
    private static synchronized void initVirtualStyleSet()
    {
        virtualStyles = new HashSet();
        virtualStyles.add("align-center");
        virtualStyles.add("align-full");
        virtualStyles.add("align-left");
        virtualStyles.add("align-right");
        virtualStyles.add("footnote");
        virtualStyles.add("indexterm");
        virtualStyles.add("keep_together");
        virtualStyles.add("keep_together_auto");
        virtualStyles.add("landscape_table");
        for (int i=1; i < 10; i++) {
            virtualStyles.add("indent-level" + i);
        }
    }

    private static synchronized void initInternalBlockStyleSet()
    {
        internalBlockStyles = new HashSet();
        internalBlockStyles.add("breadcrumbs");
        internalBlockStyles.add("caption");
        internalBlockStyles.add("coverpage");
        internalBlockStyles.add("default");
        internalBlockStyles.add("float_left");
        internalBlockStyles.add("float_right");
        internalBlockStyles.add("index_entry");
        internalBlockStyles.add("index_header");
        internalBlockStyles.add("index_subheader");
        internalBlockStyles.add("orderedlist_label");
        internalBlockStyles.add("page_header");
        internalBlockStyles.add("page_header_box");
        internalBlockStyles.add("page_header_cell");
        internalBlockStyles.add("page_footer");
        internalBlockStyles.add("page_footer_box");
        internalBlockStyles.add("page_footer_cell");
        internalBlockStyles.add("partheader");
        internalBlockStyles.add("navheader");
        internalBlockStyles.add("navfooter");
        internalBlockStyles.add("table_cell");
        internalBlockStyles.add("table_header");
        internalBlockStyles.add("titlepage");
        internalBlockStyles.add("toc_header");
        internalBlockStyles.add("toc_line");
        internalBlockStyles.add("toc_line_appendix");
        internalBlockStyles.add("toc_line_chapter");
        internalBlockStyles.add("toc_line_part");
        internalBlockStyles.add("toc_line_preface");
        internalBlockStyles.add("webhelptitle1");
        internalBlockStyles.add("webhelptitle2");
        // internalBlockStyles.add("paragraph");
        for (int i=1; i < 10; i++) {
            internalBlockStyles.add("header" + i);
        }
        for (int i=1; i <= 6; i++) {
            internalBlockStyles.add("toc_line_section" + i);
        }
    }
    
    private static synchronized void initInternalInlineStyleSet()
    {
        internalInlineStyles = new HashSet();
        internalInlineStyles.add("link");
        internalInlineStyles.add("link_visited");
        internalInlineStyles.add("link_focus");
        internalInlineStyles.add("link_hover");
        internalInlineStyles.add("link_active");
        internalInlineStyles.add("link_external");
        internalInlineStyles.add("breadcrumb_node");
    }

    
}
