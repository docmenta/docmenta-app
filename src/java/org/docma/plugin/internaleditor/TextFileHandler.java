/*
 * TextFileHandler.java
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
package org.docma.plugin.internaleditor;

import java.util.*;
import org.docma.plugin.web.DefaultContentAppHandler;

/**
 *
 * @author MP
 */
public class TextFileHandler extends DefaultContentAppHandler
{
    // Template positions where plugins are allowed to insert code
    private static final String HTML_HEAD = "htmlhead";
    private static final String ON_LOAD = "htmlonload";
    private static final String BODY_START = "htmlbodystart";
    private static final String BODY_END = "htmlbodyend";
    
    private static final Map<String, List<String>> insertOrder = new HashMap();
    private static final Map<String, String> insertValues = new HashMap();

    public static String getHTMLHead()
    {
        return getInsertion(HTML_HEAD);
    }
    
    public static String getHTMLHead(String pluginId)
    {
        return getInsertion(HTML_HEAD, pluginId);
    }
    
    public static void setHTMLHead(String pluginId, String html)
    {
        setInsertion(HTML_HEAD, pluginId, html);
    }
    
    public static String getHTMLOnLoadStatement()
    {
        return getInsertion(ON_LOAD);
    }
    
    public static String getHTMLOnLoadStatement(String pluginId)
    {
        return getInsertion(ON_LOAD, pluginId);
    }
    
    public static void setHTMLOnLoadStatement(String pluginId, String js)
    {
        setInsertion(ON_LOAD, pluginId, js);
    }
    
    public static String getHTMLBodyStart()
    {
        return getInsertion(BODY_START);
    }
    
    public static String getHTMLBodyStart(String pluginId)
    {
        return getInsertion(BODY_START, pluginId);
    }
    
    public static void setHTMLBodyStart(String pluginId, String html)
    {
        setInsertion(BODY_START, pluginId, html);
    }
    
    public static String getHTMLBodyEnd()
    {
        return getInsertion(BODY_END);
    }
    
    public static String getHTMLBodyEnd(String pluginId)
    {
        return getInsertion(BODY_END, pluginId);
    }
    
    public static void setHTMLBodyEnd(String pluginId, String html)
    {
        setInsertion(BODY_END, pluginId, html);
    }
    
    /* -------------- Private Methods ---------------- */
    
    private static String getInsertion(String posId)
    {
        List<String> insertList = insertOrder.get(posId);
        if (insertList == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String plugId : insertList) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(getInsertion(posId, plugId));
        }
        return sb.toString();
    }
    
    private static String getInsertion(String posId, String pluginId)
    {
        final String valueKey = getValueKey(posId, pluginId);
        String val = insertValues.get(valueKey);
        return (val == null) ? "" : val;
    }
    
    private static void setInsertion(String posId, String pluginId, String value)
    {
        final String valueKey = getValueKey(posId, pluginId);
        
        List<String> insertList = insertOrder.get(posId);
        if (insertList == null) {
            insertList = new ArrayList<String>();
            insertOrder.put(posId, insertList);
        }
        // remove old value
        insertList.remove(pluginId);
        insertValues.remove(valueKey);
        if ((value != null) && !value.equals("")) {
            // add new value
            insertList.add(pluginId);
            insertValues.put(valueKey, value);
        }
    }
    
    private static String getValueKey(String posId, String pluginId)
    {
        return pluginId + ":" + posId;
    }
}
