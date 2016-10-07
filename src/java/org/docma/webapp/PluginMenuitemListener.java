/*
 * PluginMenuitemListener.java
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

import org.docma.app.DocmaConstants;
import org.docma.util.Log;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Menuitem;

/**
 *
 * @author MP
 */
public class PluginMenuitemListener implements EventListener
{
    private final DocmaWebSession webSess;

    public PluginMenuitemListener(DocmaWebSession webSess) 
    {
        this.webSess = webSess;
    }

    public void onEvent(Event evt) throws Exception 
    {
        String name = evt.getName();
        Component comp = evt.getTarget();
        if (DocmaConstants.DEBUG) {
            Log.info("Plugin menu item event " + name + " for target " + 
                     ((comp != null) ? comp.getId() : "null"));
        }
        if ("onClick".equalsIgnoreCase(name) && (comp instanceof Menuitem)) {
            webSess.sendMenuClickEventToPlugin((Menuitem) comp);
        }
    }
    
}
