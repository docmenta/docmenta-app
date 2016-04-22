/*
 * CopyProductDialog.java
 * 
 *  Copyright (C) 2014  Manfred Paula, http://www.docmenta.org
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

import java.util.*;

import org.docma.coreapi.*;
import org.docma.util.Log;
import org.docma.app.*;
import org.docma.app.ui.ProductModel;
import org.zkoss.zul.*;

/**
 *
 * @author MP
 */
public class CopyProductDialog extends Window
{
    private int modalResult = -1;
    private MainWindow mainWin;
    private DocmaSession docmaSess;
    private GUI_List_Products guiListProducts;
    
    private Listbox sourceList;
    private Listbox targetList;
    private Listbox versionsList;
    private Listbox languagesList;
    private Checkbox copyExportsCheckbox;
    private Checkbox copyRevisionsCheckbox;
    private Checkbox verifyCheckbox;
    
    public boolean copyProduct(MainWindow main_win, GUI_List_Products prod_list) throws Exception
    {
        this.mainWin = main_win;
        this.docmaSess = mainWin.getDocmaSession();
        this.guiListProducts = prod_list;

        init();
        updateGUI(); // init dialog fields
        if (this.guiListProducts.getCount() < 2) {
            MessageUtil.showError(mainWin, "error.copyproduct.product_count");
            return false;
        }
        do {
            modalResult = -1;
            doModal();
            if (modalResult != GUIConstants.MODAL_OKAY) {
                return false;
            }
            if (hasInvalidInputs()) {
                continue;
            }
            String src_id = getSelectedValue(sourceList);
            String tar_id = getSelectedValue(targetList);
            if (MessageUtil.showOkCancelExclamation(this, 
                    "confirm.copy_product.overwrite.title", 
                    "confirm.copy_product.overwrite.msg", src_id, tar_id) == MessageUtil.CANCEL) 
            {
                continue;
            }
            startCopy(src_id, tar_id, getSelectedVersions(), getSelectedLanguages(), 
                      verifyCheckbox.isChecked(),
                      copyExportsCheckbox.isChecked(), 
                      copyRevisionsCheckbox.isChecked());
            return true;
        } while (true);
    }

    public void onStartClick()
    {
        modalResult = GUIConstants.MODAL_OKAY;
        setVisible(false);
    }

    public void onCancelClick()
    {
        modalResult = GUIConstants.MODAL_CANCEL;
        setVisible(false);
    }
    
    public void onSelectSourceProduct()
    {
        fillVersionsList();
        fillLanguagesList();
        checkTargetOriginalLanguage(false, false);
    }

    public void onSelectTargetProduct()
    {
        checkTargetOriginalLanguage(false, false);
    }

    public void onSelectVersions()
    {
        // nothing to do
    }

    public void onSelectLanguages()
    {
        if (languagesList.getItemCount() > 0) {
            checkTargetOriginalLanguage(true, true);
        }
    }

    private void init()
    {
        sourceList = (Listbox) getFellow("CopyProductSourceListbox");
        targetList = (Listbox) getFellow("CopyProductTargetListbox");
        versionsList = (Listbox) getFellow("CopyProductVersionsListbox");
        languagesList = (Listbox) getFellow("CopyProductLanguagesListbox");
        copyExportsCheckbox = (Checkbox) getFellow("CopyProductPubExportsCheckbox");
        copyRevisionsCheckbox = (Checkbox) getFellow("CopyProductRevisionsCheckbox");
        verifyCheckbox = (Checkbox) getFellow("CopyProductVerifyCheckbox");
    }
    
    private void updateGUI()
    {
        ProductModel[] prods = guiListProducts.getProducts();
        ProductModel[] sel_prods = guiListProducts.getSelectedProducts();
        
        // Fill source product list
        sourceList.getItems().clear();
        sourceList.appendItem("", "");
        for (ProductModel pm : prods) {
            sourceList.appendItem(pm.getId(), pm.getId());
        }
        
        // Fill target product list
        targetList.getItems().clear();
        targetList.appendItem("", "");
        for (ProductModel pm : prods) {
            targetList.appendItem(pm.getId(), pm.getId());
        }
        
        // Clear versions list
        versionsList.getItems().clear();
        // Clear languages list
        languagesList.getItems().clear();
        
        copyExportsCheckbox.setChecked(true);
        copyRevisionsCheckbox.setChecked(true);
        verifyCheckbox.setChecked(true);
        
        // Set selected product as source product by default
        String sel_id = (sel_prods.length > 0) ? sel_prods[0].getId() : "";
        GUIUtil.selectListItem(sourceList, sel_id);
        GUIUtil.selectListItem(targetList, "");
        if (sel_id.length() > 0) {
            onSelectSourceProduct();
        }
    }
    
    private void fillVersionsList()
    {
        versionsList.getItems().clear();
        
        String src_id = getSelectedValue(sourceList);
        if (src_id.length() > 0) {
            DocVersionId[] verIds = docmaSess.listVersions(src_id);
            if (verIds != null) {
                for (DocVersionId vid : verIds) {
                    String vstr = vid.toString();
                    versionsList.appendItem(vstr, vstr);
                }
                versionsList.selectAll();
            }
        }
    }
    
    private void fillLanguagesList()
    {
        languagesList.getItems().clear();
        
        String src_id = getSelectedValue(sourceList);
        if (src_id.length() > 0) {
            DocmaLanguage orig_lang = docmaSess.getOriginalLanguage(src_id);
            languagesList.appendItem(orig_lang.getDescription(), orig_lang.getCode());
            DocmaLanguage[] trans_langs = docmaSess.getTranslationLanguages(src_id);
            if (trans_langs != null) {
                for (DocmaLanguage lang : trans_langs) {
                    languagesList.appendItem(lang.getDescription(), lang.getCode());
                }
            }
            languagesList.selectAll();
        }
    }
    
    private String getSelectedValue(Listbox listbox)
    {
        Listitem item = listbox.getSelectedItem();
        String sel_val = (item == null) ? "" : item.getValue().toString();
        if (DocmaConstants.DEBUG) {
            Log.info("Selected list value: " + sel_val);
        }
        return sel_val;
    }

    private boolean hasInvalidInputs() throws Exception
    {
        // Check that source store is selected.
        String src_id = getSelectedValue(sourceList);
        if (src_id.equals("")) {
            MessageUtil.showError(mainWin, "error.copyproduct.select_source");
            return true;
        }
        
        // Check that target store is selected.
        String tar_id = getSelectedValue(targetList);
        if (tar_id.equals("")) {
            MessageUtil.showError(mainWin, "error.copyproduct.select_target");
            return true;
        }
        
        // Check that at least one version is selected.
        int cnt_vers = versionsList.getSelectedCount();
        if (cnt_vers == 0) {
            MessageUtil.showError(mainWin, "error.copyproduct.select_version");
            return true;
        }

        // Check that at least the original language of the target store is 
        // selected. This implies that at least one language is selected.
        // Precondition: Source and target store have to be selected.
        if (checkTargetOriginalLanguage(true, false)) {
            return true;
        }
        
        if (checkPendingRootVersion(src_id)) {
            return true;
        }
        
        return false;
    }

    
    private boolean checkPendingRootVersion(String sourceStore) throws DocException
    {
        String[] lang_codes = getSelectedLanguages();
        DocmaLanguage orig_lang = docmaSess.getOriginalLanguage(sourceStore);
        String orig_code = orig_lang.getCode();
        DocVersionId[] verIds = getSelectedVersions();
        List<DocVersionId> rootVerIds = getRootVersions(sourceStore, verIds);
        for (DocVersionId vid : rootVerIds) {
            for (String lang : lang_codes) {
                if (! lang.equalsIgnoreCase(orig_code)) {
                    String st = docmaSess.getVersionState(sourceStore, vid, lang);
                    if (DocmaConstants.VERSION_STATE_TRANSLATION_PENDING.equals(st)) {
                        MessageUtil.showError(mainWin, "error.copyproduct.source_version_pending", vid.toString(), lang);
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private List<DocVersionId> getRootVersions(String sourceStore, DocVersionId[] verIds) 
    {
        List<DocVersionId> rootlist = new ArrayList<DocVersionId>(Arrays.asList(verIds));
        for (DocVersionId verId : verIds) {
            DocVersionId v_id = verId;
            while (v_id != null) {
                v_id = docmaSess.getVersionDerivedFrom(sourceStore, v_id);
                if ((v_id != null) && rootlist.contains(v_id)) {
                    rootlist.remove(verId);  // one of the ancestors is in the root list
                    break;  // leave while loop
                }
            }
        }
        return rootlist;
    }
    
    /**
     * Checks if the original language of the target store exists
     * in the selected source. If the check_selected argument is true,
     * it also checks if the original language of the target store is selected.
     * If the check fails an error message is displayed to the user.
     * 
     * @param check_selected
     * @param set_selected
     * @return Returns true if the check fails, otherwise false.
     */
    private boolean checkTargetOriginalLanguage(boolean check_selected, boolean set_selected) 
    {
        String tar_id = getSelectedValue(targetList);
        if (! tar_id.equals("")) {
            DocmaLanguage orig_lang = docmaSess.getOriginalLanguage(tar_id);
            String orig_code = orig_lang.getCode();
            for (Listitem item : languagesList.getItems()) {
                if (orig_code.equalsIgnoreCase(item.getValue().toString())) {
                    if (check_selected) {
                        if (!item.isSelected()) {
                            if (set_selected) {
                                item.setSelected(true);
                            }
                            MessageUtil.showError(
                                mainWin, "error.copyproduct.target_lang_select", orig_code
                            );
                            return true;
                        }
                    }
                    return false;
                }
            }
            // Error because original language of selected target store is 
            // not defined in the selected source store:
            MessageUtil.showError(mainWin, "error.copyproduct.target_lang_missing", orig_code);
            return true;
        } else {
            return false;  // no error, because no target store selected!
        }
    }
    
    private void startCopy(String src_id, 
                           String tar_id, 
                           DocVersionId[] verIds, 
                           String[] langs, 
                           boolean verify,
                           boolean copyExports, 
                           boolean copyRevs)
    {
        try {
            // If an old activity exists which is finished, then remove
            // this activity to allow the creation of a new activity.
            Activity old_act = docmaSess.getDocStoreActivity(tar_id);
            if ((old_act != null) && old_act.isFinished()) {
                docmaSess.removeDocStoreActivity(tar_id);
            }
            // Note: If an activity already exists which is still running,
            //       then creating another activity for the same store will
            //       cause an exception.
            Activity act = docmaSess.createDocStoreActivity(tar_id);
            act.setTitle("text.copy_store_activity_title", src_id, tar_id);
            Runnable thread_obj = 
                new CopyStoreRunnable(act, docmaSess, src_id, tar_id, verIds, langs, verify, copyExports, copyRevs);
            act.start(thread_obj);
        } catch (Exception ex) {
            ex.printStackTrace();
            MessageUtil.showError(mainWin, "error.exception", ex.getLocalizedMessage());
        }
        
        this.guiListProducts.renderNewActivity(tar_id); // refresh();  // leads to periodic refresh until ativity is finished
    }

    private DocVersionId[] getSelectedVersions() throws DocException
    {
        Listitem[] sel_items = new Listitem[versionsList.getSelectedCount()];
        sel_items = versionsList.getSelectedItems().toArray(sel_items);
        DocVersionId[] vids = new DocVersionId[sel_items.length];
        for (int i = 0; i < vids.length; i++) {
            vids[i] = docmaSess.createVersionId(sel_items[i].getValue().toString());
        }
        return vids;
    }
    
    private String[] getSelectedLanguages()
    {
        Listitem[] sel_items = new Listitem[languagesList.getSelectedCount()];
        sel_items = languagesList.getSelectedItems().toArray(sel_items);
        String[] langs = new String[sel_items.length];
        for (int i = 0; i < langs.length; i++) {
            langs[i] = sel_items[i].getValue().toString();
        }
        return langs;
    }
    
}
