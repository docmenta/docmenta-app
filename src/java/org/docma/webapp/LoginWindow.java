/*
 * LoginWindow.java
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

import org.zkoss.zk.ui.event.*;
import org.zkoss.zul.*;

/**
 *
 * @author MP
 */
public class LoginWindow extends Window
{
    public void onShowHelp(String path) throws Exception
    {
        MainWindow.openHelp(path);
    }
    
    public void onShowPluginRestartMsg()
    {
        Messagebox.show("Plugins have been restored. You have to restart \n" + 
                         "the web-server to load the restored plugins!");
    }

    public void onShowErrorMsg(Event evt)
    {
        Object dat = evt.getData();
        String msg = (dat == null) ? "" : dat.toString();
        if (msg.length() == 0) {
            msg = "Internal login error. Please contact the administrator!";
        }
        Messagebox.show(msg); 
    }
}
