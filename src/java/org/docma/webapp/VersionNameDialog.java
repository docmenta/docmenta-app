/*
 * VersionName.java
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
import org.docma.coreapi.*;

import java.util.*;
import org.zkoss.zul.*;

/**
 *
 * @author MP
 */
public class VersionNameDialog extends Window
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

    public void setVersionId(String verId)
    {
        Combobox cb = (Combobox) getFellow("ChangeVersionIdCombobox");
        cb.setValue(verId);
    }

    public DocVersionId changeVersionId(String storeId, DocVersionId versionId, boolean isRelease, DocmaSession docmaSess)
    throws Exception
    {
        Combobox cb = (Combobox) getFellow("ChangeVersionIdCombobox");
        cb.getItems().clear();
        if (! isRelease) {
            DocVersionId latest_ver = docmaSess.getLatestVersionId(storeId);
            if (versionId.equals(latest_ver)) {
                cb.appendItem(GUIUtil.getLatestVerIdLabel(this));
            }
        }

        DocVersionId new_verId = null;
        do {
            modalResult = -1;
            doModal();
            if (modalResult != GUIConstants.MODAL_OKAY) {
                return null;
            }
            new_verId = checkValidVersionId(storeId, versionId, isRelease, docmaSess);
            if (new_verId == null) {
                continue;
            }
            if (new_verId.equals(versionId)) return null;
            else return new_verId;
        } while (true);
    }

    private String getVersionId()
    {
        Combobox cb = (Combobox) getFellow("ChangeVersionIdCombobox");
        return cb.getValue().trim();
    }

    private DocVersionId checkValidVersionId(String storeId, DocVersionId oldVerId, boolean isRelease, DocmaSession docmaSess)
    throws Exception
    {
        String id_str = getVersionId();
        DocVersionId verId = null;
        try {
            verId = GUIUtil.getNormalizedVerId(this, id_str);
        } catch (DocException dex) {
            Messagebox.show("Invalid version ID.");
            return null;
        }
        if (isRelease) {
            if (verId.toString().equalsIgnoreCase(DocmaConstants.DEFAULT_LATEST_VERSION_ID)) {
                Messagebox.show("Version ID cannot be '" + DocmaConstants.DEFAULT_LATEST_VERSION_ID +
                                "' for a released version.");
                return null;
            }
        }
        DocVersionId[] verIds = docmaSess.listVersions(storeId);
        int idx = Arrays.asList(verIds).indexOf(oldVerId);
        if (idx < 0) {
            throw new DocRuntimeException("Version '" + oldVerId + "' is not contained in version list.");
        }
        if (idx > 0) {
            DocVersionId minId = verIds[idx-1];
            if (! verId.isHigherThan(minId)) {
                Messagebox.show("New version ID must be higher than '" + minId + "'.");
                return null;
            }
        }
        if (idx < verIds.length-1) {
            DocVersionId maxId = verIds[idx+1];
            if (! verId.isLowerThan(maxId)) {
                Messagebox.show("New version ID must be lower than '" + maxId + "'.");
                return null;
            }
        }
        return verId;
    }

}
