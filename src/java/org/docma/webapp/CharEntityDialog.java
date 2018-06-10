/*
 * CharEntityDialog.java
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
import org.zkoss.zul.*;

/**
 *
 * @author MP
 */
public class CharEntityDialog extends Window
{
    private static final int MODE_NEW = 0;
    private static final int MODE_EDIT = 1;

    private int modalResult = -1;
    private int mode = -1;

    private Textbox numericBox;
    private Textbox symbolicBox;
    private Textbox descriptionBox;
    private Checkbox showBox;
    private Label displayCharLabel;

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

    public void setMode_NewEntity()
    {
        init();
        mode = MODE_NEW;
        setTitle(GUIUtil.i18(this).getLabel("label.charentity.dialog.new.title"));
        numericBox.setDisabled(false);
    }

    public void setMode_EditEntity()
    {
        init();
        mode = MODE_EDIT;
        setTitle(GUIUtil.i18(this).getLabel("label.charentity.dialog.edit.title"));
        numericBox.setDisabled(true);
    }

    public boolean doEditEntity(DocmaCharEntity entity, DocmaCharEntity[] all_entities)
    throws Exception
    {
        init();
        updateGUI(entity); // init dialog fields
        do {
            modalResult = -1;
            doModal();
            if (modalResult != GUIConstants.MODAL_OKAY) {
                return false;
            }
            if (hasInvalidInputs(all_entities)) {
                continue;
            }
            updateModel(entity);
            return true;
        } while (true);
    }

    private void init()
    {
        numericBox = (Textbox) getFellow("CharEntityNumericTextbox");
        symbolicBox = (Textbox) getFellow("CharEntitySymbolicTextbox");
        showBox = (Checkbox) getFellow("CharEntityShowCheckbox");
        descriptionBox = (Textbox) getFellow("CharEntityDescriptionTextbox");
        displayCharLabel = (Label) getFellow("CharEntityDisplayLabel");
    }

    private boolean hasInvalidInputs(DocmaCharEntity[] all_entities) throws Exception
    {
        String num = numericBox.getValue().trim();
        String sym = symbolicBox.getValue().trim();
        if (num.equals("")) {
            Messagebox.show("Please enter a numeric entity code!");
            return true;
        }
        if (sym.equals("")) {
            Messagebox.show("Please enter a symbolic entity code!");
            return true;
        }
        if ((num.length() > 10) || (sym.length() > 10)) {
            Messagebox.show("Entiy code is too long. Maximum length is 10 characters.");
            return true;
        }
        if (! num.matches("&#[0-9]{1,6};")) {
            Messagebox.show("Invalid numeric entity code.");
            return true;
        }
        if (! sym.matches("&[A-Za-z0-9]{1,32};")) {
            Messagebox.show("Invalid symbolic entity code.");
            return true;
        }
        if (mode == MODE_NEW) {
            for (int i=0; i < all_entities.length; i++) {
                if (num.equals(all_entities[i].getNumeric())) {
                    Messagebox.show("An entity with this numeric code already exists!");
                    return true;
                }
            }
        }
        return false;
    }

    private void updateGUI(DocmaCharEntity entity)
    {
        String num = entity.getNumeric();
        numericBox.setValue(num);
        symbolicBox.setValue(entity.getSymbolic());
        showBox.setChecked(entity.isSelectable());
        descriptionBox.setValue(entity.getDescription());
        char decoded_char = ' ';
        try {
            int char_code = Integer.parseInt(num.substring(2, num.length() - 1));
            decoded_char = (char) char_code;
        } catch (Exception ex) {};
        displayCharLabel.setValue("" + decoded_char);
    }

    private void updateModel(DocmaCharEntity entity)
    {
        if (mode == MODE_NEW) {
            entity.setNumeric(numericBox.getValue().trim());
        }
        entity.setSymbolic(symbolicBox.getValue().trim());
        entity.setSelectable(showBox.isChecked());
        entity.setDescription(descriptionBox.getValue().trim());
    }

}
