/*
 * AutoFormatConfigDialog.java
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
public class AutoFormatConfigDialog extends Window
{
    private int modalResult = -1;
    private Textbox clsNameBox;


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

    public String edit(String clsname)
    throws Exception
    {
        init();
        clsNameBox.setValue(clsname); // init dialog fields
        do {
            modalResult = -1;
            doModal();
            if (modalResult != GUIConstants.MODAL_OKAY) {
                return null;
            }
            if (hasInvalidInputs()) {
                continue;
            }
            return clsNameBox.getValue().trim();
        } while (true);
    }

    private void init()
    {
        clsNameBox = (Textbox) getFellow("AutoFormatClassnameTextbox");
    }

    private boolean hasInvalidInputs() throws Exception
    {
        String name = clsNameBox.getValue().trim();
        if (name.equals("")) {
            Messagebox.show("Please enter an Auto-Format classname!");
            return true;
        }
        return false;
    }

}
