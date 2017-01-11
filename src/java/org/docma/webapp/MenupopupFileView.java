/*
 * MenupopupFileView.java
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
package org.docma.webapp;

import org.docma.app.DocmaConstants;
import org.zkoss.zul.*;
import org.zkoss.zk.ui.event.*;
import org.zkoss.zk.ui.util.Clients;

/**
 *
 * @author MP
 */
public class MenupopupFileView extends Menupopup // implements EventListener
{
    private DocmaWebSession webSess;
    
    public MenupopupFileView(DocmaWebSession webSess)
    {
        this.webSess = webSess;
    }
    
//    public void onEvent(Event evt) throws Exception
//    {
//        if (DocmaConstants.DEBUG) { 
//            System.out.println("MenupopupFileView Event: " + evt.getName());
//        }
//        if (evt instanceof OpenEvent) {
//            boolean isOpen = ((OpenEvent) evt).isOpen();
//            if (DocmaConstants.DEBUG) {
//                System.out.println(isOpen ? "Open file context" : "Close file context");
//            }
//            if (isOpen) {
//                // webSess.getMainWindow().updateContentContextMenu(this);
//            }
//        }
//    }
    
    public void onShowContextMenu(Event evt)
    {
        if (DocmaConstants.DEBUG) { 
            System.out.println("MenupopupFileView.onShowContextMenu()");
        }
        webSess.propagateMenuOpenEventToPlugins(this);
        MenuUtil.updateContentContextMenu(this, webSess.getMainWindow());
        Clients.evalJavaScript("openCtx();");  // open context menu at cursor position (or at top)
    }

}
