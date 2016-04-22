/*
 * ProductVersionDialog.java
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

import java.util.Arrays;

import org.docma.app.ui.ProductVersionModel;
import org.docma.app.*;
import org.docma.coreapi.*;
import org.zkoss.zul.*;
import org.zkoss.zk.ui.SuspendNotAllowedException;

/**
 *
 * @author MP
 */
public class ProductVersionDialog extends Window
{
    private static final int MODE_NEW = 0;
    private static final int MODE_EDIT = 1;

    private int modalResult = -1;
    private int mode = -1;

    private Combobox versionidbox;
    private Listbox derivedfromlist;


    public boolean doCreateNewVersion(ProductVersionModel vm, DocmaSession docmaSess)
    throws Exception
    {
        init();
        setMode_NewVersion();
        updateGUI(vm, docmaSess); // init dialog fields
        do {
            setMode_NewVersion();
            modalResult = -1;
            doModal();
            if (modalResult != GUIConstants.MODAL_OKAY) {
                return false;
            }
            if (hasInvalidInputs(docmaSess)) {
                continue;
            }
            updateModel(vm, docmaSess);
            return true;
        } while (true);
    }

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


    private void init()
    {
        versionidbox = (Combobox) getFellow("ProductVersionIdBox");
        derivedfromlist = (Listbox) getFellow("ProductVersionDerivedFromList");
    }

    private DocVersionId getDerivedFromId(DocmaSession docmaSess)
    throws DocException
    {
        String derived_str = (String) derivedfromlist.getSelectedItem().getValue();
        if (derived_str.equals("")) {
            return null;
        } else {
            return docmaSess.createVersionId(derived_str);
        }
    }

    private boolean hasInvalidInputs(DocmaSession docmaSess) throws Exception
    {
        init();
        // check whether version id is valid
        String ver_str = versionidbox.getValue().trim();
        if (! docmaSess.isValidVersionId(ver_str)) {
            Messagebox.show("Error: Version-ID is not valid.");
            return true;
        }
        DocVersionId ver_id = GUIUtil.getNormalizedVerId(this, ver_str);

        // check whether new version id is higher than the version it is derived from
        DocVersionId derivedId = getDerivedFromId(docmaSess);
        if (derivedId != null) {
            if (! ver_id.isHigherThan(derivedId)) {
                Messagebox.show("Error: Version-ID must be higher than the version it is derived from.");
                return true;
            }
        }

        // check whether version id already exists
        if (Arrays.asList(docmaSess.listVersions(docmaSess.getStoreId())).contains(ver_id)) {
            Messagebox.show("Error: Version-ID already exists: " + ver_id);
            return true;
        }

        return false;
    }

    private void setMode_NewVersion() throws Exception
    {
        mode = MODE_NEW;
        setTitle(GUIUtil.i18(this).getLabel("label.productversion.dialog.new.title"));
        derivedfromlist.setDisabled(false);
    }

    private void setMode_EditVersionId() throws Exception
    {
        mode = MODE_EDIT;
        setTitle(GUIUtil.i18(this).getLabel("label.productversion.dialog.edit.title"));
        derivedfromlist.setDisabled(true);
    }

    private void updateGUI(ProductVersionModel vm, DocmaSession docmaSess)
    {
        DocVersionId verId = vm.getVersionId();
        String ver_str = (verId == null) ? "" : verId.toString();
        versionidbox.getItems().clear();
        versionidbox.setValue(ver_str);
        versionidbox.appendItem(GUIUtil.getLatestVerIdLabel(this));
        DocVersionId baseId = vm.getDerivedFrom();

        // clear derivedFrom list
        while (derivedfromlist.getItemCount() > 0) derivedfromlist.removeItemAt(0);

        // fill derivedFrom list with all released versions
        DocVersionId[] vids = docmaSess.listVersions(vm.getProductId());
        int sel_idx = 0;  // default is latest version
        if (vids.length == 0) {  // initial version
            derivedfromlist.appendItem("", "");
        } else {
            int cnt = 0;
            for (int i = 0; i < vids.length; i++) {
                DocVersionId v_id = vids[i];
                String state = docmaSess.getVersionState(docmaSess.getStoreId(), v_id);
                if (state.equals(DocmaConstants.VERSION_STATE_RELEASED)) {
                    String vstr = v_id.toString();
                    derivedfromlist.appendItem(vstr, vstr);
                    if ((baseId != null) && baseId.equals(v_id)) {
                        sel_idx = cnt;
                    }
                    cnt++;
                }
            }
        }
        derivedfromlist.setSelectedIndex(sel_idx);
        derivedfromlist.invalidate();  // workaround for zk bug
    }

    private void updateModel(ProductVersionModel vm, DocmaSession docmaSess)
    throws Exception
    {
        init();
        DocVersionId verId = GUIUtil.getNormalizedVerId(this, versionidbox.getValue());
        vm.setVersionId(verId);
        if (mode == MODE_NEW) {
            vm.setDerivedFrom(getDerivedFromId(docmaSess));
        }
    }

}
