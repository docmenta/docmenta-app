/*
 * CreateStyleVariantDialog.java
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

import java.util.*;
import org.zkoss.zul.*;

/**
 *
 * @author MP
 */
public class CreateStyleVariantDialog extends Window
{
    private int modalResult = -1;

    private Textbox baseBox;
    private Combobox nameBox;

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

    public boolean doEdit(DocmaStyle docmaStyle, DocmaSession docmaSess)
    throws Exception
    {
        baseBox = (Textbox) getFellow("StyleVariantBaseTextbox");
        nameBox = (Combobox) getFellow("StyleVariantNameTextbox");

        String baseid = docmaStyle.getBaseId();
        String variantname = docmaStyle.getVariantId();
        if (variantname == null) {
            variantname = "";
        }
        baseBox.setValue(baseid);
        nameBox.setValue(variantname);
        nameBox.getItems().clear();

        Iterator it = Arrays.asList(docmaSess.getStyleVariantIds()).iterator();
        while (it.hasNext()) {
            nameBox.appendItem((String) it.next());
        }

        do {
            modalResult = -1;
            doModal();
            if (getModalResult() != GUIConstants.MODAL_OKAY) {
                return false;
            }
            if (hasInvalidInputs(docmaSess)) {
                continue;
            }
            docmaStyle.setId(baseid + DocmaStyle.VARIANT_DELIMITER + nameBox.getValue().trim());
            return true;
        } while (true);
    }

    private boolean hasInvalidInputs(DocmaSession docmaSess) throws Exception
    {
        String var_name = nameBox.getValue().trim();
        if (var_name.equals("")) {
            Messagebox.show("Please enter a variant name!");
            return true;
        }
        if (var_name.length() > 16) {
            Messagebox.show("Variant name is too long. Maximum length is 16 characters.");
            return true;
        }
        if (! var_name.matches("[A-Za-z][0-9A-Za-z_]*")) {
            Messagebox.show("Invalid variant name. Allowed characters are ASCII letters and underscore.");
            return true;
        }
        if (docmaSess.getStyle(baseBox.getValue() + DocmaStyle.VARIANT_DELIMITER + nameBox.getValue().trim()) != null) {
            Messagebox.show("A variant with this name already exists!");
            return true;
        }
        return false;
    }

}
