/*
 * ReferencedPubsDialog.java
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
import org.docma.app.ui.ReferencedPubsModel;
import java.util.*;
import org.zkoss.zul.*;

/**
 *
 * @author MP
 */
public class ReferencedPubsDialog extends Window
{
    private int modalResult = -1;

    private Listbox publist;

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

    public boolean doEdit(ReferencedPubsModel rm, String pubId, DocmaSession docmaSess)
    throws Exception
    {
        init();
        // setTitle(GUIUtil.i18(this).getLabel("label.filtersetting.dialog.edit.title"));
        updateGUI(rm, pubId, docmaSess); // init dialog fields
        do {
            modalResult = -1;
            doModal();
            if (getModalResult() != GUIConstants.MODAL_OKAY) {
                return false;
            }
            if (hasInvalidInputs()) {
                continue;
            }
            updateModel(rm);
            return true;
        } while (true);
    }

    private void init()
    {
        publist = (Listbox) getFellow("EditReferencedPubsListbox");
    }

    private int getModalResult()
    {
        return modalResult;
    }

    private boolean hasInvalidInputs() throws Exception
    {
        return false;
    }

    private void updateGUI(ReferencedPubsModel rm, String sourcePubId, DocmaSession docmaSess)
    {
        // clear list
        publist.getItems().clear();

        String[] id_arr = docmaSess.getPublicationConfigIds();
        for (String pubid : id_arr) {
            if (! pubid.equals(sourcePubId)) {  // publication cannot reference itself
                Listitem item = publist.appendItem(pubid, pubid);
                item.setSelected(rm.containsId(pubid));
            }
        }
    }

    private void updateModel(ReferencedPubsModel rm)
    {
        ArrayList<String> ids = new ArrayList<String>();
        Set<Listitem> sel_items = publist.getSelectedItems();
        for (Listitem item : sel_items) {
            ids.add(item.getValue().toString());
        }
        rm.setIds(ids.toArray(new String[ids.size()]));
    }
    
}
