/*
 * FilterSettingDialog.java
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
import org.docma.app.ui.FilterSettingModel;
import java.util.*;
import org.zkoss.zul.*;

/**
 *
 * @author MP
 */
public class FilterSettingDialog extends Window
{
    private static final int MODE_NEW = 0;
    private static final int MODE_EDIT = 1;

    private int modalResult = -1;
    private int mode = -1;

    // private Textbox filternamebox;
    private Listbox filtersettinglist;

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

    public void setMode_NewFilter()
    {
        init();
        mode = MODE_NEW;
        setTitle(GUIUtil.i18(this).getLabel("label.filtersetting.dialog.new.title"));
        // filternamebox.setDisabled(false);
    }

    public void setMode_EditFilter()
    {
        init();
        mode = MODE_EDIT;
        setTitle(GUIUtil.i18(this).getLabel("label.filtersetting.dialog.edit.title"));
        // filternamebox.setDisabled(true);
    }

    public boolean doEditFilter(FilterSettingModel fsm, DocmaSession docmaSess)
    throws Exception
    {
        init();
        updateGUI(fsm, docmaSess); // init dialog fields
        do {
            modalResult = -1;
            doModal();
            if (getModalResult() != GUIConstants.MODAL_OKAY) {
                return false;
            }
            if (hasInvalidInputs(docmaSess)) {
                continue;
            }
            updateModel(fsm);
            return true;
        } while (true);
    }

    private void init()
    {
        // filternamebox = (Textbox) getFellow("FilterSettingNameTextbox");
        filtersettinglist = (Listbox) getFellow("EditFilterSettingListbox");
    }

    private int getModalResult()
    {
        return modalResult;
    }

    private boolean hasInvalidInputs(DocmaSession docmaSess) throws Exception
    {
//        String name = filternamebox.getValue().trim();
//        if (name.equals("")) {
//            Messagebox.show("Please enter a filter name!");
//            return true;
//        }
//        if (name.length() > 40) {
//            Messagebox.show("Filter name is too long. Maximum length is 40 characters.");
//            return true;
//        }
//        if (! name.matches("[A-Za-z][0-9A-Za-z_]*")) {
//            Messagebox.show("Invalid filter name. Allowed characters are ASCII letters and underscore.");
//            return true;
//        }
//        if ((mode == MODE_NEW) && (docmaSess.getFilterSetting(name) != null)) {
//            Messagebox.show("A filter with this name already exists!");
//            return true;
//        }
        return false;
    }

    private void updateGUI(FilterSettingModel fsm, DocmaSession docmaSess)
    {
        // filternamebox.setValue(fsm.getName());

        // clear list
        filtersettinglist.getItems().clear(); // while (filtersettinglist.getItemCount() > 0) filtersettinglist.removeItemAt(0);

        String[] app_arr = docmaSess.getDeclaredApplics();
        for (int i=0; i < app_arr.length; i++) {
            String appvalue = app_arr[i];
            Listitem item = filtersettinglist.appendItem(appvalue, appvalue);
            item.setSelected(fsm.containsApplic(appvalue));
        }
    }

    private void updateModel(FilterSettingModel fsm)
    {
        // fsm.setName(filternamebox.getValue().trim());
        StringBuffer buf = new StringBuffer();
        Iterator sel_items = filtersettinglist.getSelectedItems().iterator();
        while (sel_items.hasNext()) {
            Listitem item = (Listitem) sel_items.next();
            if (buf.length() > 0) {
                buf.append(",");
            }
            buf.append((String) item.getValue());
        }
        fsm.setApplics(buf.toString());
    }
}
