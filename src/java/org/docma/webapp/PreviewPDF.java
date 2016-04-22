/*
 * PreviewPDF.java
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

import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.Composer;

/**
 *
 * @author MP
 */
public class PreviewPDF implements Composer
{
    public void doAfterCompose(Component comp) throws Exception
    {
        PreviewPDFWindow win = (PreviewPDFWindow) comp;
        Execution exec = win.getDesktop().getExecution();
        String deskid = exec.getParameter("desk");
        String nodeid = exec.getParameter("nodeid");
        win.init(deskid, nodeid);
        
        DocmaWebSession webSess = GUIUtil.getDocmaWebSession(win.getDesktop().getSession(), deskid);
        
        // Set Theme
        win.setContentStyle(webSess.getThemeProperty("popupwin.contentstyle.css"));

        // Iframe frm = (Iframe) win.getFellow("contentfrm");
        // frm.setSrc(url);
    }

}
