/*
 * TinyInitInsertion.java
 * 
 *  Copyright (C) 2017  Manfred Paula, http://www.docmenta.org
 *   
 *  This file is part of Docmenta. Docmenta is free software: you can 
 *  redistribute it and/or modify it under the terms of the GNU Lesser 
 *  General Public License as published by the Free Software Foundation, 
 *  either version 3 of the License, or (at your option) any later version.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Docmenta.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.docma.plugin.tinymce;

import org.docma.plugin.internals.ScriptInsertion;
import org.docma.plugin.internals.ScriptInsertions;
import org.docma.plugin.web.WebUserSession;

/**
 *
 * @author MP
 */
public class TinyInitInsertion 
{
    public static final String APPEND_PLUGINS = "append_plugins";
    public static final String APPEND_BUTTONS_3 = "append_buttons_3";
    public static final String APPEND_BUTTONS_4 = "append_buttons_4";
    public static final String APPEND_OPTIONS = "append_options";
    
    private static final ScriptInsertions insertions = new ScriptInsertions();
    
    public static String getPlugins(WebUserSession userSess)
    {
        return insertions.getInsertion(userSess, "content", APPEND_PLUGINS);
    }

    public static String getButtons3(WebUserSession userSess)
    {
        return insertions.getInsertion(userSess, "content", APPEND_BUTTONS_3);
    }

    public static String getButtons4(WebUserSession userSess)
    {
        return insertions.getInsertion(userSess, "content", APPEND_BUTTONS_4);
    }

    public static String getOptions(WebUserSession userSess)
    {
        return insertions.getInsertion(userSess, "content", APPEND_OPTIONS);
    }

    public static void insert(ScriptInsertion ins)
    {
        insertions.addInsertion(ins);
    }
    
    public static void clearPluginInsertions(String pluginId)
    {
        insertions.clearPluginInsertions(pluginId);
    }
    
}
