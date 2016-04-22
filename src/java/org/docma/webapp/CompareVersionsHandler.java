/*
 * CompareVersionsHandler.java
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

import org.docma.app.*;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Messagebox;

/**
 *
 * @author MP
 */
public class CompareVersionsHandler
{
    private MainWindow mainwin;


    public CompareVersionsHandler(MainWindow mainwin)
    {
        this.mainwin = mainwin;
    }

    public void compare() throws Exception
    {
        DocmaNode nd = mainwin.getSelectedDocmaNode();
        if (nd == null) {
            Messagebox.show("Please select a single node to be compared!");
            return;
        }
        DocmaSession docmaSess = mainwin.getDocmaSession();

        DocmaWebSession webSess = GUIUtil.getDocmaWebSession(mainwin);
        int win_left = 0; // webSess.getEditorPositionX();
        int win_top = webSess.getEditorPositionY();
        int win_width = 800; // docmaSess.getUserProperty(GUIConstants.PROP_USER_EDIT_WIN_WIDTH);
        String win_height = docmaSess.getUserProperty(GUIConstants.PROP_USER_EDIT_WIN_HEIGHT);
        if ((win_height == null) || win_height.equals("")) {
            win_height = "" + GUIConstants.EDIT_WIN_DEFAULT_HEIGHT;
        }
        // Set minimum window width
        // try {
        //     int width_int = Integer.parseInt(win_width);
        //     if (width_int < 640) win_width = "640";
        // } catch(Exception ex) {}

        Desktop desk = mainwin.getDesktop();
        String compare_url = desk.getExecution().encodeURL("CompareVersionsDialog.zul?desk=" + desk.getId() +
                             "&nodeid=" + nd.getId());
        String client_action = "window.open('" + compare_url +
              "', '_blank', 'width=" + win_width + ",height=" + win_height +
              ",left=" + win_left + ",top=" + win_top +
              ",resizable=yes,location=no,menubar=no');";
        Clients.evalJavaScript(client_action);
    }

}
