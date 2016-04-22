/*
 * GUI_List_ProductVersions.java
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
import org.docma.app.ui.ProductVersionModel;
import org.docma.coreapi.*;
import java.util.*;
import org.zkoss.zul.*;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;

/**
 *
 * @author MP
 */
public class GUI_List_ProductVersions
{
    private MainWindow mainWin;
    private Listbox        productversions_listbox;
    private ListModelList  productversions_listmodel;

    public GUI_List_ProductVersions(MainWindow mainWin, Listbox productversions_list)
    {
        this.mainWin = mainWin;
        this.productversions_listbox = productversions_list;

        productversions_listmodel = new ListModelList();
        productversions_listmodel.setMultiple(true);
        productversions_listbox.setModel(productversions_listmodel);
        productversions_listbox.setItemRenderer(mainWin.getListitemRenderer());
    }

    public void loadProductVersions()
    {
        productversions_listmodel.clear();
        DocmaSession docmaSess = mainWin.getDocmaSession();
        String storeId = docmaSess.getStoreId();
        DocVersionId[] verIds = docmaSess.listVersions(storeId);
        for (int i=0; i < verIds.length; i++) {
            ProductVersionModel vm = new ProductVersionModel(docmaSess, storeId, verIds[i]);
            productversions_listmodel.add(vm);
        }
    }

    public void doCreateVersion(Event evt) throws Exception
    {
        final String CMD_CREATE = "createversion";
        Object para = (evt == null) ? null : evt.getData();
        String operation_id = (para == null) ? "" : para.toString();
        boolean busy = operation_id.startsWith(CMD_CREATE);
        // Two cases exist:
        // 1) If no version exists yet, then a new version without any content can be created.
        // 2) If at least one version in state "released" exists, then a new version
        //    can be derived from one of the "released" versions.
        DocmaSession docmaSess = mainWin.getDocmaSession();
        try {
            if (! busy) {  // before long operation -> get user inputs
                // Show error if versions exist but no version is in state "released".
                int ver_cnt = productversions_listmodel.size();
                if (ver_cnt > 0) {
                    boolean has_released = false;
                    for (int i=0; i < ver_cnt; i++) {
                        ProductVersionModel ver = (ProductVersionModel)
                            productversions_listmodel.getElementAt(i);
                        if (ver.getState().equals(DocmaConstants.VERSION_STATE_RELEASED)) {
                            has_released = true;
                            break;
                        }
                    }
                    if (! has_released) {
                        Messagebox.show("Cannot create new version from draft version!");
                        return;
                    }
                }

                // Initialize the fields of the version model (vm). The version
                // model vm is the model for the version to be created.
                ProductVersionDialog dialog = (ProductVersionDialog) mainWin.getPage().getFellow("ProductVersionDialog");
                ProductVersionModel vm = new ProductVersionModel();   // version model with empty fields
                vm.setProductId(docmaSess.getStoreId());
                vm.setState(DocmaConstants.VERSION_STATE_DRAFT);
                // If user selected a "released" version then set this version
                // as the default for the "derivedFrom" field of the version model.
                if (productversions_listbox.getSelectedCount() == 1) {
                    int sel_idx = productversions_listbox.getSelectedIndex();
                    ProductVersionModel baseVersion = (ProductVersionModel)
                            productversions_listmodel.getElementAt(sel_idx);
                    if (baseVersion.getState().equals(DocmaConstants.VERSION_STATE_RELEASED)) {
                        vm.setDerivedFrom(baseVersion.getVersionId());
                    }
                }
                if (dialog.doCreateNewVersion(vm, docmaSess)) {
                    // Create the new version based on the version model fields.
                    Clients.showBusy("Creating version " + vm.getVersionId() +
                                     " derived from version " + vm.getDerivedFrom() +
                                     ". Please wait...");
                    busy = true;
                    String op_id = CMD_CREATE + System.currentTimeMillis();
                    mainWin.setAttribute(op_id, vm, Component.SESSION_SCOPE);
                    Events.echoEvent("onCreateVersion", mainWin, op_id);
                }
            } else {  // start long operation
                // Create the new version based on the version model fields.
                ProductVersionModel vm = (ProductVersionModel)
                    mainWin.getAttribute(operation_id, Component.SESSION_SCOPE);
                docmaSess.createVersion(vm.getProductId(),
                                        vm.getDerivedFrom(),
                                        vm.getVersionId());
                vm.setCreationDate(new Date());

                // Update user interface
                loadProductVersions(); // productversions_listmodel.add(vm);
                mainWin.updateVersionSelectionList(docmaSess);
                Clients.clearBusy();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Messagebox.show("Error: " + ex.getMessage());
            if (busy) Clients.clearBusy();
        }
    }

    public void doReleaseVersion() throws Exception
    {
        try {
            DocmaSession docmaSess = mainWin.getDocmaSession();
            String lang = docmaSess.getTranslationMode();
            // Show error if user did not select a version
            int sel_idx;
            if (productversions_listbox.getSelectedCount() != 1) {
                if (productversions_listmodel.size() == 1) {
                    sel_idx = 0;
                } else {
                    Messagebox.show("Please select the version to be released from the list!");
                    return;
                }
            } else {
                sel_idx = productversions_listbox.getSelectedIndex();
            }

            ProductVersionModel sel_ver = (ProductVersionModel)
                    productversions_listmodel.getElementAt(sel_idx);

            if (sel_ver.getState().equalsIgnoreCase(DocmaConstants.VERSION_STATE_TRANSLATION_PENDING)) {
                // Special case in translation mode: When a pending version shall be released,
                // then ask user if he wants to release all previous versions from
                // the draft version up to the selected pending version.
                if (Messagebox.show("Release all versions from the draft version up to the selected version?", 
                                    "Release all previous versions?",
                                    Messagebox.YES | Messagebox.NO, Messagebox.QUESTION) == Messagebox.YES) {
                    List prevs = new ArrayList();
                    String storeId = sel_ver.getProductId();
                    ProductVersionModel vm = sel_ver;
                    do {
                        prevs.add(0, vm);
                        DocVersionId previd = vm.getDerivedFrom();
                        if (previd != null) {
                            vm = new ProductVersionModel(docmaSess, storeId, previd);
                        } else {
                            break;
                        }
                    } while (! vm.getState().equalsIgnoreCase(DocmaConstants.VERSION_STATE_RELEASED));
                    
                    for (int i=0; i < prevs.size(); i++) {
                        vm = (ProductVersionModel) prevs.get(i);
                        vm.refresh(docmaSess);  // reload fields because state has changed from
                                                // pending to draft due to releasing previous version
                        if (! releaseVersion(vm, docmaSess)) break;
                    }
                    // Update user interface
                    loadProductVersions();   // reload list
                }
            } else {
                // Normal case: release a single draft version
                if (releaseVersion(sel_ver, docmaSess)) {
                    // Update user interface
                    if (lang == null) {
                        productversions_listmodel.set(sel_idx, sel_ver);
                        productversions_listbox.setSelectedIndex(sel_idx);
                    } else {
                        // In translation mode also the state of derived versions
                        // changes, therefore reload complete list
                        loadProductVersions();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Messagebox.show("Error: " + ex.getMessage());
            loadProductVersions();   // reload list
        }
    }

    private boolean releaseVersion(ProductVersionModel sel_ver, DocmaSession docmaSess)
    throws Exception
    {
        String lang = docmaSess.getTranslationMode();
        // Show error if the selected version is not in state "draft"
        if (! sel_ver.getState().equalsIgnoreCase(DocmaConstants.VERSION_STATE_DRAFT)) {
            Messagebox.show("The version is not in draft state. Current state: " + sel_ver.getState());
            return false;
        }

        // If original mode and version ID is "LATEST" then ask user to enter a valid release version ID.
        String sel_sid = sel_ver.getProductId();
        DocVersionId sel_vid = sel_ver.getVersionId();
        DocVersionId new_vid = null;
        boolean is_latest = sel_vid.toString().equalsIgnoreCase(DocmaConstants.DEFAULT_LATEST_VERSION_ID);
        if ((lang == null) && is_latest) {
            VersionNameDialog dialog = (VersionNameDialog) mainWin.getPage().getFellow("VersionNameDialog");
            dialog.setVersionId("");
            new_vid = dialog.changeVersionId(sel_sid, sel_vid, true, docmaSess);
            if (new_vid == null) return false;  // User canceled
        }

        // See if version to be released is currently opened by this session
        String opened_sid = docmaSess.getStoreId();
        DocVersionId opened_vid = docmaSess.getVersionId();
        boolean is_opened = (opened_sid != null) && sel_sid.equals(opened_sid) &&
                            sel_vid.equals(opened_vid);

        // Close store. Version can only be released when no sessions are connected.
        if (is_opened) {
            mainWin.closeDocStore(docmaSess, true);
        }

        try {
            // Show error if other sessions are still connected to the selected version.
            if (docmaSess.usersConnected(sel_sid, sel_vid)) {
                Messagebox.show("Cannot release version. Other sessions are still connected!");
                return false;
            }

            // If user entered a new release version ID, then rename the version.
            if (new_vid != null) {
                try {
                    docmaSess.renameVersion(sel_sid, sel_vid, new_vid);
                    sel_ver.setVersionId(new_vid);
                    sel_vid = new_vid;
                } catch (DocException dex) {
                    Messagebox.show(dex.getMessage());
                    return false;
                }
            }

            // Change the state of the selected version to "released".
            try {
                docmaSess.setVersionState(sel_sid, sel_vid,
                                          DocmaConstants.VERSION_STATE_RELEASED);
                // Note: docmaSess.setVersionState(...) automatically sets the release date to now.
                sel_ver.setState(DocmaConstants.VERSION_STATE_RELEASED);
                sel_ver.setReleaseDate(docmaSess.getVersionReleaseDate(sel_sid, sel_vid));  // Should be now.
                return true;
            } catch (DocException dex) {
                Messagebox.show(dex.getMessage());
                return false;
            }
            // } catch (Exception ex) {  // unknown exception
            //     if (is_opened) mainWin.closeDocStore(docmaSess); // close store and tabs
            //     is_opened = false;  // do not try to re-open store in the finally block
            //     throw ex;
        } finally {
            // re-open store if it was opened before
            if (is_opened) {
                mainWin.openDocStore(docmaSess, sel_sid, sel_vid);
            }
        }
    }

    public void doUnreleaseVersion() throws Exception
    {
        try {
            // Show error if no version is selected.
            int sel_idx;
            if (productversions_listbox.getSelectedCount() != 1) {
                if (productversions_listmodel.size() == 1) {
                    sel_idx = 0;
                } else {
                    Messagebox.show("Please select a draft version from the list!");
                    return;
                }
            } else {
                sel_idx = productversions_listbox.getSelectedIndex();
            }

            // Show error if the selected version is already in state "draft".
            ProductVersionModel sel_ver = (ProductVersionModel)
                    productversions_listmodel.getElementAt(sel_idx);
            if (sel_ver.getState().equals(DocmaConstants.VERSION_STATE_DRAFT)) {
                Messagebox.show("The selected version is already in state 'Draft'!");
                return;
            }

            DocmaSession docmaSess = mainWin.getDocmaSession();
            String storeId = sel_ver.getProductId();
            DocVersionId verId = sel_ver.getVersionId();

            // Show error if released exports exist for the selected version.
            if (docmaSess.exportsExist(storeId, verId, DocmaConstants.VERSION_STATE_RELEASED)) {
                Messagebox.show("Cannot unrelease version: Released exports already exist!");
                return;
            }

            String lang = docmaSess.getTranslationMode();
            DocVersionId[] subs = docmaSess.getSubVersions(storeId, verId);
            // In original mode show error if versions are already derived from this version.
            if ((lang == null) && (subs.length > 0)) {
                Messagebox.show("Cannot unrelease version: derived versions already exist!");
                return;
            }

            // Ask user to confirm the operation
            if (Messagebox.show("Unrelease version '" + verId + "'?", "Unrelease?",
                Messagebox.YES | Messagebox.NO, Messagebox.QUESTION) == Messagebox.YES) {

                if ((lang != null) && (subs.length > 0)) {
                    // Show warning that translation of derived versions will be lost
                    if (Messagebox.show("Translation of derived versions will be lost. Continue?", "Continue?",
                        Messagebox.YES | Messagebox.NO, Messagebox.QUESTION) == Messagebox.NO) {
                        return;
                    }
                }

                // Set state of the version to "draft".
                // Will throw an exception if another version is derived from this version.
                docmaSess.setVersionState(storeId, verId, DocmaConstants.VERSION_STATE_DRAFT);

                // Update user interface.
                sel_ver.setState(DocmaConstants.VERSION_STATE_DRAFT);
                sel_ver.setReleaseDate(null);
                if (lang == null) {
                    productversions_listmodel.set(sel_idx, sel_ver);
                    productversions_listbox.setSelectedIndex(sel_idx);
                } else {
                    // In translation mode also the state of derived versions
                    // changes, therefore reload complete list
                    loadProductVersions();
                }
                mainWin.updateVersionSelectionList(docmaSess);  // show draft label
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Messagebox.show("Error: " + ex.getMessage());
        }
    }


    public void doRenameVersion() throws Exception
    {
        DocmaSession docmaSess = mainWin.getDocmaSession();
        String lang = docmaSess.getTranslationMode();
        // Do not allow rename in translation mode
        if (lang != null) {
            Messagebox.show("Rename is not allowed in translation mode!");
            return;
        }

        // Show error if no version is selected.
        if (productversions_listbox.getSelectedCount() != 1) {
            Messagebox.show("Please select a draft version from the list!");
            return;
        }

        // Show error if selected version is in state "released".
        int sel_idx = productversions_listbox.getSelectedIndex();
        ProductVersionModel sel_ver = (ProductVersionModel)
                productversions_listmodel.getElementAt(sel_idx);
        if (sel_ver.getState().equalsIgnoreCase(DocmaConstants.VERSION_STATE_RELEASED)) {
            Messagebox.show("Cannot rename a released version!");
            return;
        }

        // Ask user to enter a new version ID.
        VersionNameDialog dialog = (VersionNameDialog) mainWin.getPage().getFellow("VersionNameDialog");
        String sel_sid = sel_ver.getProductId();
        DocVersionId sel_vid = sel_ver.getVersionId();
        dialog.setVersionId(sel_vid.toString());
        DocVersionId new_vid = dialog.changeVersionId(sel_sid, sel_vid, false, docmaSess);
        if (new_vid == null) return;  // User canceled.

        try {
            // See if version to be renamed is currently opened by this session
            String opened_sid = docmaSess.getStoreId();
            DocVersionId opened_vid = docmaSess.getVersionId();
            boolean is_opened = (opened_sid != null) && sel_sid.equals(opened_sid) &&
                                sel_vid.equals(opened_vid);

            // Close store. Version can only be renamed when no sessions are connected.
            if (is_opened) {
                mainWin.closeDocStore(docmaSess, true);
            }

            try {
                // Show error if other sessions are still connected to the selected version.
                if (docmaSess.usersConnected(sel_sid, sel_vid)) {
                    Messagebox.show("Cannot rename version. Other sessions are still connected!");
                    return;
                }

                // Rename the version.
                docmaSess.renameVersion(sel_sid, sel_vid, new_vid);
                sel_ver.setVersionId(new_vid);
                sel_vid = new_vid;

                // Update user interface
                productversions_listmodel.set(sel_idx, sel_ver);
                productversions_listbox.setSelectedIndex(sel_idx);
                if (! is_opened) mainWin.updateVersionSelectionList(docmaSess);
            } finally {
                if (is_opened) {
                    // Re-open store if it was opened before. This will also
                    // update the version selection list.
                    mainWin.openDocStore(docmaSess, sel_sid, sel_vid);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Messagebox.show("Error: " + ex.getMessage());
        }
    }


    public void doDeleteVersion() throws Exception
    {
        DocmaSession docmaSess = mainWin.getDocmaSession();
        String lang = docmaSess.getTranslationMode();
        // Do not allow rename in translation mode
        if (lang != null) {
            Messagebox.show("Delete is not allowed in translation mode!");
            return;
        }
        // Show error if no version is selected.
        if (productversions_listbox.getSelectedCount() != 1) {
            Messagebox.show("Please select the version to be deleted from the list!");
            return;
        }

        // Get selected version model.
        int sel_idx = productversions_listbox.getSelectedIndex();
        ProductVersionModel sel_ver = (ProductVersionModel)
                productversions_listmodel.getElementAt(sel_idx);
        String sel_sid = sel_ver.getProductId();
        DocVersionId sel_vid = sel_ver.getVersionId();

        try {
            // Show error if another version is derived from the selected version.
            DocVersionId[] subs = docmaSess.getSubVersions(sel_sid, sel_vid);
            if (subs.length > 0) {
                Messagebox.show("Cannot delete version. Another version is derived from this version.");
                return;
            }

            // Show error if released exports exist for the selected version.
            if (docmaSess.exportsExist(sel_sid, sel_vid, null)) {
                Messagebox.show("Cannot delete version: Exports already exist!");
                return;
            }

            // Ask user to confirm the delete operation
            if (Messagebox.show("Delete version '" + sel_ver.getVersionId() + "'?", "Delete?",
                Messagebox.YES | Messagebox.NO, Messagebox.QUESTION) == Messagebox.YES) {

                // See if version to be deleted is currently opened by this session
                String opened_sid = docmaSess.getStoreId();
                DocVersionId opened_vid = docmaSess.getVersionId();
                boolean delete_opened = (opened_sid != null) && sel_sid.equals(opened_sid) &&
                                    sel_vid.equals(opened_vid);

                // Close store. Version can only be deleted when no sessions are connected.
                if (delete_opened) {
                    mainWin.closeDocStore(docmaSess, true);  // close temporarily
                }

                // Show error if other sessions are still connected to the selected version.
                if (docmaSess.usersConnected(sel_sid, sel_vid)) {
                    Messagebox.show("Cannot delete version. Other sessions are still connected!");
                    if (delete_opened) {  // was closed
                        mainWin.openDocStore(docmaSess, opened_sid, opened_vid);  // reopen
                    }
                    return;
                }

                // Delete the version.
                docmaSess.deleteVersion(sel_sid, sel_vid);

                // If opened version was deleted -> open latest version
                if (delete_opened) {
                    mainWin.openDocStore(docmaSess, opened_sid, null);
                }

                // Update user interface
                boolean is_open = (docmaSess.getStoreId() != null);
                if (is_open) {
                    loadProductVersions(); // productversions_listmodel.remove(vm);
                    if (! delete_opened) {
                        // If another version then the opened version was deleted, 
                        // then just remove the deleted version from the version 
                        // selection-list:
                        mainWin.updateVersionSelectionList(docmaSess);
                        // If the opened version was deleted, then the user interface
                        // is updated in mainWin.openDocStore(...) (see above).
                    }
                } else {
                    mainWin.closeDocStore(docmaSess);  // Update GUI, i.e. clear all lists
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Messagebox.show("Error: " + ex.getMessage());
            if (docmaSess.getStoreId() == null) {
                mainWin.closeDocStore(docmaSess);  // Update GUI, i.e. clear all lists
            }
        }
    }

    public void doEditVersionComment() throws Exception
    {
        DocmaSession docmaSess = mainWin.getDocmaSession();
        // Show error if no version is selected.
        if (productversions_listbox.getSelectedCount() <= 0) {
            Messagebox.show("Please select one or more version from the list!");
            return;
        }

        VersionCommentDialog dialog = (VersionCommentDialog) mainWin.getPage().getFellow("VersionCommentDialog");
        try {
            String comment = null;
            List idx_list = new ArrayList();
            Iterator it = productversions_listbox.getSelectedItems().iterator();
            while (it.hasNext()) {
                Listitem item = (Listitem) it.next();
                idx_list.add(new Integer(item.getIndex()));
            }
            for (int i=0; i < idx_list.size(); i++) {
                int idx = ((Integer) idx_list.get(i)).intValue();
                ProductVersionModel vm =
                    (ProductVersionModel) productversions_listmodel.getElementAt(idx);
                if (comment == null) {
                    if (dialog.editComment(vm.getComment())) {
                        comment = dialog.getComment();
                    } else {
                        return;  // do nothing
                    }
                }
                // Set comment
                docmaSess.setVersionComment(vm.getProductId(), vm.getVersionId(), comment);
                // Update GUI
                vm.setComment(comment);
                productversions_listmodel.set(idx, vm);
                productversions_listbox.setSelectedIndex(idx);  // re-select
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Messagebox.show("Error: " + ex.getMessage());
        }
    }

    public void doClearRevisions(Event evt) throws Exception
    {
        DocmaSession docmaSess = mainWin.getDocmaSession();
        final String CMD_START = "start";
        Object para = (evt == null) ? null : evt.getData();
        boolean isStart = (para != null) && para.toString().equals(CMD_START);
        if (! isStart) {  // before start: check selection and ask user if he really wants to delete revisions!
            int sel_cnt = productversions_listbox.getSelectedCount();
            if (sel_cnt <= 0) {  // Show error if no version is selected.
                Messagebox.show("Please select one or more version from the list!");
                return;
            }
            String msg;
            if (sel_cnt == 1) {
                msg = "Clear content revisions of selected version?";
            } else {
                msg = "Clear content revisions of selected versions?";
            }
            if (Messagebox.show(msg, "Clear Revisions?",
                Messagebox.YES | Messagebox.NO, Messagebox.QUESTION) != Messagebox.YES) {
                return;
            }
            try {
                Clients.showBusy("Deleting revisions. Please wait...");
                Events.echoEvent("onClearRevisions", mainWin, CMD_START);
            } catch (Exception ex) {
                ex.printStackTrace();
                Clients.clearBusy();
            }
            return;
        }
        try {
            List idx_list = new ArrayList();
            Iterator it = productversions_listbox.getSelectedItems().iterator();
            while (it.hasNext()) {
                Listitem item = (Listitem) it.next();
                idx_list.add(new Integer(item.getIndex()));
            }
            for (int i=0; i < idx_list.size(); i++) {
                int idx = ((Integer) idx_list.get(i)).intValue();
                ProductVersionModel vm =
                    (ProductVersionModel) productversions_listmodel.getElementAt(idx);
                // Set comment
                docmaSess.clearRevisions(vm.getProductId(), vm.getVersionId());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Messagebox.show("Error: " + ex.getMessage());
        } finally {
            Clients.clearBusy();
        }
    }


}
