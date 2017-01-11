/*
 * PluginTab.java
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
package org.docma.plugin.implementation;

/**
 *
 * @author MP
 */
public class PluginTab 
{
    private final String pluginId;
    private final String tabId;
    private final String tabTitle;
    private final int position;
    private final String zulPath;

    public PluginTab(String pluginId, String tabId, String tabTitle, int position, String zulPath) 
    {
        this.pluginId = pluginId;
        this.tabId = tabId;
        this.tabTitle = tabTitle;
        this.position = position;
        this.zulPath = zulPath;
    }

    public String getPluginId() 
    {
        return pluginId;
    }

    public String getTabId() 
    {
        return tabId;
    }

    public String getTabTitle() 
    {
        return tabTitle;
    }

    public int getPosition() 
    {
        return position;
    }

    public String getZulPath() 
    {
        return zulPath;
    }

}
