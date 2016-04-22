/*
 * PreviewPDFHandler.java
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
import org.zkoss.zul.Messagebox;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.util.Clients;

/**
 *
 * @author MP
 */
public class PreviewPDFHandler
{
    private MainWindow mainwin;


    public PreviewPDFHandler(MainWindow mainwin)
    {
        this.mainwin = mainwin;
    }

    public void openPreviewWindow() throws Exception
    {
        DocmaNode nd = mainwin.getSelectedDocmaNode();
        if (nd == null) {
            Messagebox.show("Please select a content or section node!");
            return;
        }
        DocmaWebSession webSess = GUIUtil.getDocmaWebSession(mainwin);

        int win_left = webSess.getPdfPreviewPositionX();
        int win_top = webSess.getPdfPreviewPositionY();
        int win_width = webSess.getPdfPreviewWidth();
        int win_height = webSess.getPdfPreviewHeight();

        Desktop desk = mainwin.getDesktop();
        String preview_url = desk.getExecution().encodeURL("PreviewPDFWindow.zul?desk=" +
                             desk.getId() + "&nodeid=" + nd.getId());
        String client_action = "window.open('" + preview_url +
              "', '_blank', 'width=" + win_width + ",height=" + win_height +
              ",left=" + win_left + ",top=" + win_top +
              ",resizable=yes,status=yes,location=no,menubar=no');";
        Clients.evalJavaScript(client_action);
    }

}
