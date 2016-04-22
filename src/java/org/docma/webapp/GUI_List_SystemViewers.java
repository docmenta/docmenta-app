/*
 * GUI_List_SystemViewers.java
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
package org.docma.webapp;

import org.zkoss.zul.Grid;
import org.zkoss.zul.Window;

/**
 *
 * @author MP
 */
public class GUI_List_SystemViewers extends GUI_List_Extensions
{
    
    public GUI_List_SystemViewers(Window win) 
    {
        super(win, (Grid) win.getFellow("ViewAppsConfigGrid"), null);
    }

    @Override
    void changeAssignment(String[] exts, String app_id) throws Exception
    {
        DocmaWebApplication webapp = GUIUtil.getDocmaWebApplication(rootWin);
        webapp.getViewerAssignments().setAssignments(exts, app_id);
        webapp.saveViewerAssignments();
    }

    @Override
    ExtAssignment[] getExtAssignments()
    {
        DocmaWebApplication webapp = GUIUtil.getDocmaWebApplication(rootWin);
        return webapp.getViewerAssignments().listAssignments();
    }

    @Override
    String getAssignedContentEditor() 
    {
        return null;   // not applicable
    }

    @Override
    String getDefaultContentEditor() 
    {
        return null;   // not applicable
    }

    @Override
    String[] getAvailableApps(String[] exts) 
    {
        DocmaWebApplication webapp = GUIUtil.getDocmaWebApplication(rootWin);
        return webapp.getViewerIds(exts);
    }

    @Override
    String getDefaultApp(String[] exts) 
    {
        return null;
    }

}
