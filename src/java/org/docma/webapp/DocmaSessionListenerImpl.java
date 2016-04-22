/*
 * DocmaSessionListenerImpl.java
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
import org.docma.util.Log;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Session;

/**
 *
 * @author MP
 */
public class DocmaSessionListenerImpl implements DocmaSessionListener
{
    private Desktop desk;

    public DocmaSessionListenerImpl(Desktop desk)
    {
        this.desk = desk;
    }

    public void sessionClosed()
    {
        if (desk != null) {
            Log.info("Invalidating session of desktop " + desk.getId());
            Session sess = desk.getSession(); 
            if (sess != null) {
                sess.invalidate();
            } else {
                Log.warning("Session of desktop is null!");
            }
        }
    }


}
