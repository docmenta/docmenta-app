/*
 * BusyDialog.java
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

import org.zkoss.zul.*;

/**
 *
 * @author MP
 */
public class BusyDialog extends Window
{

    private void setMessage(String msg)
    {
        Label lab = (Label) getFellow("busyMessageLabel");
        lab.setValue(msg);
    }

    public void showBusy(String msg) throws Exception
    {
        setMessage(msg);
        setMode("highlighted");
        setVisible(true);
    }

    public void clearBusy()
    {
        setVisible(false);
    }
}
