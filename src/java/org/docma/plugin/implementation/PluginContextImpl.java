/*
 * PluginContextImpl.java
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

import java.io.File;
import org.docma.plugin.*;
import org.docma.plugin.web.*;

/**
 *
 * @author MP
 */
public class PluginContextImpl implements WebPluginContext
{
    private PluginControl pluginCtrl;
    
    PluginContextImpl(PluginControl ctrl)
    {
        this.pluginCtrl = ctrl;
    }

    public String getPluginId() 
    {
        return pluginCtrl.getId();
    }

    public ApplicationContext getApplicationContext() 
    {
        return pluginCtrl.getPluginManager().getApplicationContext();
    }

    public File getWebAppDirectory() 
    {
        return pluginCtrl.getPluginManager().getWebContext().getWebAppDirectory();
    }

    public File getPluginDirectory() 
    {
        return pluginCtrl.getPluginDirectory();
    }

    public boolean isPluginLoaded() 
    {
        return pluginCtrl.isLoaded();
    }
    
}
