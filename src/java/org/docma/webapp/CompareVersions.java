/*
 * CompareVersions.java
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
public class CompareVersions implements Composer
{
    public void doAfterCompose(Component comp) throws Exception
    {
        CompareVersionsDialog dialog = (CompareVersionsDialog) comp;
        Execution exec = dialog.getDesktop().getExecution();
        String deskid = exec.getParameter("desk");
        String nodeid = exec.getParameter("nodeid");
        // String langid = exec.getParameter("langid");
        dialog.init(deskid, nodeid);   // fill version lists

        DocmaWebSession webSess = GUIUtil.getDocmaWebSession(dialog.getDesktop().getSession(), deskid);
        
        // Set Theme
        dialog.setContentStyle(webSess.getThemeProperty("popupwin.contentstyle.css"));

    }
}
