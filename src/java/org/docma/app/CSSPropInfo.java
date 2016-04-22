/*
 * CSSPropInfo.java
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

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

/**
 *
 * @author MP
 */
public class CSSPropInfo 
{
    public static final int TYPE_TEXT   = 0;
    public static final int TYPE_NUMBER = 1;
    public static final int TYPE_LENGTH = 2;
    public static final int TYPE_COLOR  = 3;
    public static final int TYPE_SELECT = 4;
    public static final int TYPE_NUMBER_OR_LENGTH = 5;
    // public static final int TYPE_LENGTH_LIST = 6;
    

    private static final CSSPropInfo[] CSS_PROPS = {
        new CSSPropInfo("font-family",         null, TYPE_SELECT, 
                                                     new String[] {"serif", 
                                                                   "sans-serif", 
                                                                   "monospace"} ),
        new CSSPropInfo("font-size",           null, TYPE_LENGTH, 
                                                     new String[] {"9pt", "10pt", "11pt", "12pt", 
                                                                   "13pt", "14pt", "15pt", "16pt", 
                                                                   "18pt", "20pt", "24pt", "32pt"} ),
        new CSSPropInfo("font-stretch",        null, TYPE_SELECT, 
                                                     new String[] {"narrower",
                                                                   "wider",
                                                                   "inherit",
                                                                   "ultra-condensed", 
                                                                   "extra-condensed", 
                                                                   "condensed", 
                                                                   "semi-condensed", 
                                                                   "normal", 
                                                                   "semi-expanded", 
                                                                   "expanded", 
                                                                   "extra-expanded", 
                                                                   "ultra-expanded"} ),
        new CSSPropInfo("font-size-adjust",    null, TYPE_NUMBER, 
                                                     new String[] {"0", "0.5", "0.6", 
                                                                   "0.7", "0.8", "0.9",
                                                                   "inherit"} ),
        new CSSPropInfo("font-style",          null, TYPE_SELECT, 
                                                     new String[] {"normal", "italic", 
                                                                   "oblique", "inherit"}),
        new CSSPropInfo("font-variant",        null, TYPE_SELECT, 
                                                     new String[] {"normal", "small-caps", "inherit"} ),
        new CSSPropInfo("font-weight",         null, TYPE_SELECT, 
                                                     new String[] {"normal", "lighter", "bold", 
                                                                   "bolder", "inherit", 
                                                                   "100", "200", "300", "400", "500", 
                                                                   "600", "700", "800", "900"}),
        new CSSPropInfo("color",               null, TYPE_COLOR ),
        new CSSPropInfo("background-color",    null, TYPE_COLOR ),
        new CSSPropInfo("letter-spacing",      null, TYPE_LENGTH, 
                                                     new String[] {"normal", "inherit", 
                                                                   "0.5em", "1em", "1.5em"} ),
        new CSSPropInfo("text-decoration",     null, TYPE_SELECT, 
                                                     new String[] {"underline", "overline", 
                                                                   "line-through", "blink", 
                                                                   "none", "inherit"} ),
        new CSSPropInfo("text-shadow",         null, TYPE_TEXT, 
                                                     new String[] {"none", "-2px 2px 3px #000000"}),
        new CSSPropInfo("text-transform",      null, TYPE_SELECT, 
                                                     new String[] {"capitalize", "uppercase", 
                                                                   "lowercase", "none", "inherit"} ),
        new CSSPropInfo("word-spacing",        null, TYPE_LENGTH, 
                                                     new String[] {"normal", "inherit", 
                                                                   "0.5em", "1em", "1.5em"} ),
        new CSSPropInfo("text-indent",         null, TYPE_LENGTH, 
                                                     new String[] {"0", "10pt", "20pt", "inherit"}),
        new CSSPropInfo("line-height",         null, TYPE_NUMBER_OR_LENGTH, 
                                                     new String[] {"normal", "inherit", 
                                                                   "120%"}),
        new CSSPropInfo("vertical-align",      null ),
        new CSSPropInfo("text-align",          null ),
        new CSSPropInfo("white-space",         null ),
        new CSSPropInfo("margin",              null ),
        new CSSPropInfo("margin-top",          null ),
        new CSSPropInfo("margin-bottom",       null ),
        new CSSPropInfo("margin-left",         null ),
        new CSSPropInfo("margin-right",        null ),
        new CSSPropInfo("padding",             null ),
        new CSSPropInfo("padding-top",         null ),
        new CSSPropInfo("padding-bottom",      null ),
        new CSSPropInfo("padding-left",        null ),
        new CSSPropInfo("padding-right",       null ),
        new CSSPropInfo("padding-start",       null ),
        new CSSPropInfo("padding-end",         null ),
        new CSSPropInfo("border",              null ),
        new CSSPropInfo("border-top",          null ),
        new CSSPropInfo("border-bottom",       null ),
        new CSSPropInfo("border-left",         null ),
        new CSSPropInfo("border-right",        null ),
        new CSSPropInfo("border-start",        null ),
        new CSSPropInfo("border-end",          null ),
        new CSSPropInfo("border-width",        null ),
        new CSSPropInfo("border-top-width",    null ),
        new CSSPropInfo("border-bottom-width", null ),
        new CSSPropInfo("border-left-width",   "border-start-width" ),
        new CSSPropInfo("border-right-width",  "border-end-width" ),
        new CSSPropInfo("border-start-width",  null ),
        new CSSPropInfo("border-end-width",    null ),
        new CSSPropInfo("border-style",        null ),
        new CSSPropInfo("border-top-style",    null ),
        new CSSPropInfo("border-bottom-style", null ),
        new CSSPropInfo("border-left-style",   "border-start-style" ),
        new CSSPropInfo("border-right-style",  "border-end-style" ),
        new CSSPropInfo("border-start-style",  null ),
        new CSSPropInfo("border-end-style",    null ),
        new CSSPropInfo("border-color",        null ),
        new CSSPropInfo("border-top-color",    null ),
        new CSSPropInfo("border-bottom-color", null ),
        new CSSPropInfo("border-left-color",   "border-start-color" ),
        new CSSPropInfo("border-right-color",  "border-end-color" ),
        new CSSPropInfo("border-start-color",  null ),
        new CSSPropInfo("border-end-color",    null )
    };
    
    private static final Map<String, CSSPropInfo> mapCSSInfo = new HashMap<String, CSSPropInfo>(200);

    private String cssName;
    private String foName;
    private int propType;
    private String[] values;
    
    /* --------------  Static initialization  --------------- */
    
    static 
    {
        for (CSSPropInfo pinfo : CSS_PROPS) {
            mapCSSInfo.put(pinfo.getCssName(), pinfo);
        }
    }

    /* --------------  Constructors  --------------- */
    
    public CSSPropInfo(String cssName, String foName)
    {
        this(cssName, foName, TYPE_TEXT, null);
    }
    
    public CSSPropInfo(String cssName, String foName, int type)
    {
        this(cssName, foName, type, null);
    }
    
    public CSSPropInfo(String cssName, String foName, int ptype, String[] values)
    {
        this.cssName = cssName;
        this.foName = foName;
        this.propType = ptype;
        this.values = values;
    }

    /* --------------  Methods  --------------- */
    
    public String getCssName() 
    {
        return cssName;
    }

    public String getFoName() 
    {
        return foName;
    }

    public int getType() 
    {
        return propType;
    }

    public String[] getValues() 
    {
        return values;
    }
    

    /*------- static methods --------*/
    
    public static Iterator<CSSPropInfo> allProps()
    {
        return Arrays.asList(CSS_PROPS).iterator();
    }
    
    public static CSSPropInfo getInfoByCSSName(String css_name)
    {
        return mapCSSInfo.get(css_name);
    }
    
}
