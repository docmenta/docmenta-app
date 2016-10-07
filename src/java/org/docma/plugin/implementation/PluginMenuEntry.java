/*
 * PluginMenuEntry.java
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
package org.docma.plugin.implementation;

import java.util.Map;
import java.util.HashMap;


/**
 *
 * @author MP
 */
public class PluginMenuEntry 
{
    public static final int ITEM = 1;
    public static final int SUB_MENU = 2;
    public static final int SEPARATOR = 3;
    
    private int type = ITEM;
    private String pluginId = null;
    private String parentMenuId = null;
    private String entryId = null;
    private String neighbourId = null;
    private boolean insertBefore = false;
    private final Map<MenuOption, Object> options = new HashMap<MenuOption, Object>();


    public PluginMenuEntry()
    {
    }

    public int getType() 
    {
        return type;
    }

    public void setType(int type) 
    {
        this.type = type;
    }

    public String getPluginId() 
    {
        return pluginId;
    }

    public void setPluginId(String pluginId) 
    {
        this.pluginId = pluginId;
    }

    public String getParentMenuId() 
    {
        return parentMenuId;
    }

    public void setParentMenuId(String parentMenuId) 
    {
        this.parentMenuId = parentMenuId;
    }

    public String getEntryId() 
    {
        return entryId;
    }

    public void setEntryId(String entryId) 
    {
        this.entryId = entryId;
    }

    public Map<MenuOption, Object> getOptions() 
    {
        return options;
    }

    public void setOptions(Map<MenuOption, Object> opts) 
    {
        if (opts != null) {
            options.putAll(opts);
        }
    }
    
    public Object getOption(MenuOption name)
    {
        return options.get(name);
    }
    
    public String getStringOption(MenuOption name)
    {
        Object obj = options.get(name);
        return (obj != null) ? obj.toString() : null;
    }
    
    public void setOption(MenuOption name, Object value)
    {
        if (name != null) {
            if (value != null) {
                options.put(name, value);
            } else {
                options.remove(name);
            }
        }
    }

    public String getNeighbourId() 
    {
        return neighbourId;
    }

    public void setNeighbourId(String neighbourId) 
    {
        this.neighbourId = neighbourId;
    }

    public boolean isInsertBefore() 
    {
        return insertBefore;
    }

    public void setInsertBefore(boolean insertBefore) 
    {
        this.insertBefore = insertBefore;
    }

}
