/*
 * NewApplicDialog.java
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
import org.zkoss.zk.ui.SuspendNotAllowedException;

/**
 *
 * @author MP
 */
public class NewApplicDialog extends Window
{
    private int modalResult = -1;

    public void onOkayClick()
    {
        modalResult = GUIConstants.MODAL_OKAY;
        setVisible(false);
    }

    public void onCancelClick()
    {
        modalResult = GUIConstants.MODAL_CANCEL;
        setVisible(false);
    }

    public int getModalResult()
    {
        return modalResult;
    }

    public void doModal() throws SuspendNotAllowedException
    {
        modalResult = -1;
        super.doModal();
    }

    public String getApplic()
    {
        Textbox tb = (Textbox) getFellow("NewApplicNameTextbox");
        return tb.getValue().trim();
    }

    public void setApplic(String applic_value)
    {
        Textbox tb = (Textbox) getFellow("NewApplicNameTextbox");
        tb.setValue(applic_value);
    }

    public boolean hasInvalidInputs() throws Exception
    {
        String name = getApplic();
        if (name.equals("")) {
            Messagebox.show("Please enter a value.");
            return true;
        }
        if (name.length() > 40) {
            Messagebox.show("Value is too long. Maximum length is 40 characters.");
            return true;
        }
        if (! name.matches("[A-Za-z][0-9A-Za-z_]*")) {
            Messagebox.show("Invalid applicability name. Allowed characters are ASCII letters and underscore.");
            return true;
        }
        return false;
    }
}
