/*
 * ProductDialog.java
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

import org.docma.coreapi.*;
import org.docma.app.*;
import org.docma.app.ui.*;
import java.io.*;
import java.util.*;
import org.docma.coreapi.fsimplementation.FilesystemStoreProperties;
import org.zkoss.zul.*;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.SuspendNotAllowedException;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;

/**
 *
 * @author MP
 */
public class ProductDialog extends Window
{
    private static final int MODE_NEW = 0;
    private static final int MODE_EDIT = 1;

    private int modalResult = -1;
    private int mode = -1;

    private MainWindow mainWin;
    private boolean loadError;
    private Textbox idbox;
    private Textbox namebox;
    private Listbox storetypeListbox;
    private Component pathArea;
    private Textbox pathbox;
    // private Textbox defaultpathbox;
    private Radiogroup pathgroup;
    private Radio defaultpathradio;
    private Radio custompathradio;
    private Listbox langslistbox;
    private ListModelList langslistmodel = null;
    private Button origLangButton = null;
    private Component exportInDbArea; 
    private Checkbox exportInDbCheckbox;
    private Component externalDbArea;
    private Textbox dbUrlTextbox;
    private Listbox dbDialectListbox;
    private Textbox dbDriverTextbox;
    private Textbox dbUserTextbox;
    private Textbox dbPasswdTextbox;
    private Label hintLabel;

    private ProductModel pm = null;
    private GUI_List_Products guiListProducts;
    // private ListModelList productsListmodel;
    private DocmaSession docmaSess;

    
    public void newProduct(ProductModel pm, GUI_List_Products guiList, DocmaSession docmaSess) throws Exception
    {
        this.pm = pm;
        this.guiListProducts = guiList;
        // this.productsListmodel = guiList.getListModel();
        this.docmaSess = docmaSess;
        boolean busy = false;
        try {
            updateGUI(this.pm, docmaSess);  // init dialog fields
            boolean has_error;
            do {
                doModal_NewProduct();
                if (getModalResult() != GUIConstants.MODAL_OKAY) {
                    return;
                }
                has_error = hasInvalidInputs(docmaSess);
            } while (has_error);
            updateModel(this.pm);
            Clients.showBusy("Creating product '" + pm.getId() + "'. Please wait...");
            busy = true;
            // String op_id = CMD_CREATE + System.currentTimeMillis();
            // mainWin.setAttribute(op_id, vm, Component.SESSION_SCOPE);
            Events.echoEvent("onCreateProduct", this, null);
        } catch (Exception ex) {
            ex.printStackTrace();
            if (busy) Clients.clearBusy();
            Messagebox.show("Error: " + ex.getMessage());
        }
    }

    public void onCreateProduct()
    {
        try {
            String storeId = pm.getId();
            String storeType = pm.getStoreType();
            List<String> p_n = new ArrayList<String>();
            List<String> p_v = new ArrayList<String>();

            p_n.add(DocmaConstants.PROP_STORE_TYPE);
            p_v.add(storeType);
            p_n.add(DocmaConstants.PROP_STORE_DISPLAYNAME);
            p_v.add(pm.getName());
            p_n.add(DocmaConstants.PROP_STORE_ORIG_LANGUAGE);
            p_v.add(pm.getOriginalLanguage().getCode());
            p_n.add(DocmaConstants.PROP_STORE_TRANSLATION_LANGUAGES);
            p_v.add(pm.getTranslationLanguageCodesAsString());

            if (pm.isStoreTypeFs() || pm.isStoreTypeDbEmbedded()) {
                if (pm.getPathtype().equals(ProductModel.PATH_CUSTOM)) {
                    p_n.add(DocmaConstants.PROP_STORE_PATH);
                    p_v.add(pm.getPath());
                }
            }
            if (pm.isStoreTypeDb()) {  // embedded or external database
                p_n.add(DocmaConstants.PROP_STORE_ARCHIVE_TYPE);
                p_v.add(pm.getArchiveType());
            }
            if (pm.isStoreTypeDbExternal()) {
                p_n.add(DocmaConstants.PROP_STORE_DB_CONNECTION_URL);
                p_v.add(pm.getDbUrl());
                p_n.add(DocmaConstants.PROP_STORE_DB_DIALECT);
                p_v.add(pm.getDbDialect());
                p_n.add(DocmaConstants.PROP_STORE_DB_DRIVER_CLASS);
                p_v.add(pm.getDbDriver());
                p_n.add(DocmaConstants.PROP_STORE_DB_CONNECTION_USER);
                p_v.add(pm.getDbUser());
                p_n.add(DocmaConstants.PROP_STORE_DB_CONNECTION_PWD);
                p_v.add(pm.getDbPasswd());
            }

            String[] propnames = p_n.toArray(new String[p_n.size()]);
            String[] propvalues = p_v.toArray(new String[p_v.size()]);

            docmaSess.createDocStore(storeId, propnames, propvalues);
            DocVersionId verId = docmaSess.createVersionId(DocmaConstants.DEFAULT_LATEST_VERSION_ID);
            docmaSess.createVersion(storeId, null, verId);

            mainWin.addStoreToSelectList(storeId);

            try {
                docmaSess.startTransaction();
                mainWin.setLastSelectedOutputConfigId(docmaSess, storeId, "html", DocmaConstants.DEFAULT_HTML_CONFIG_ID);
                mainWin.setLastSelectedOutputConfigId(docmaSess, storeId, "pdf", DocmaConstants.DEFAULT_PDF_CONFIG_ID);
                docmaSess.commitTransaction();
            } catch (Exception ex) {
                ex.printStackTrace();
                if (docmaSess.runningTransaction()) docmaSess.rollbackTransaction();
            }

            guiListProducts.addProductModel(pm);
            pm = null;
            Clients.clearBusy();
        } catch (Exception ex) {
            Clients.clearBusy();
            ex.printStackTrace();
            Messagebox.show("Error: " + ex.getMessage());
            if ((pm != null) && Arrays.asList(docmaSess.listDocStores()).contains(pm.getId())) {
                pm.refresh(docmaSess);
                guiListProducts.addProductModel(pm);
            }
        }
    }
    
    private void init()
    {
        DocmaWebSession webSess = GUIUtil.getDocmaWebSession(this);
        mainWin = webSess.getMainWindow();
        idbox = (Textbox) getFellow("ProductIdTextbox");
        namebox = (Textbox) getFellow("ProductNameTextbox");
        storetypeListbox = (Listbox) getFellow("ProductStorageTypeList");
        pathArea = getFellow("ProductPathArea");
        pathbox = (Textbox) getFellow("ProductPathTextbox");
        // defaultpathbox = (Textbox) getFellow("ProductDefaultPathTextbox");
        pathgroup = (Radiogroup) getFellow("ProductPathRadiogroup");
        defaultpathradio = (Radio) getFellow("DefaultProductPathRadio");
        custompathradio = (Radio) getFellow("CustomProductPathRadio");
        langslistbox = (Listbox) getFellow("ProductLanguagesListbox");
        if (langslistmodel == null) {
            langslistmodel = new ListModelList();
            langslistbox.setModel(langslistmodel);
            langslistbox.setItemRenderer(mainWin.getListitemRenderer());
        }
        origLangButton = (Button) getFellow("ProductSetOrigLanguageButton");
        exportInDbArea = getFellow("ProductExportsInDbArea");
        exportInDbCheckbox = (Checkbox) getFellow("ProductExportsInDbCheckbox");
        externalDbArea = getFellow("ProductExternalDbConnectionArea");
        dbUrlTextbox = (Textbox) getFellow("ProductExternalDbUrlTextbox");
        dbDialectListbox = (Listbox) getFellow("ProductExternalDbDialectList");
        dbDriverTextbox = (Textbox) getFellow("ProductExternalDbDriverTextbox");
        dbUserTextbox = (Textbox) getFellow("ProductExternalDbUserTextbox");
        dbPasswdTextbox = (Textbox) getFellow("ProductExternalDbPasswdTextbox");
        hintLabel = (Label) getFellow("ProductDialogHintLabel");
    }

    public boolean hasInvalidInputs(DocmaSession docmaSess) throws Exception
    {
        init();
        String id = idbox.getValue().trim();
        if (! id.matches("[0-9A-Za-z_-]+")) {
            MessageUtil.showError(this, "error.product.invalid_id");
            return true;
        }
        String storetype = getStoreType();
        if (mode == MODE_NEW) {
            if (storetype.equals("")) {
                MessageUtil.showError(this, "error.product.select_store_type");
                return true;
            }
            List prod_ids = Arrays.asList(docmaSess.listDocStores());
            if (prod_ids.contains(id)) {
                MessageUtil.showError(this, "error.product.id_already_exists");
                return true;
            }
            Radio selradio = pathgroup.getSelectedItem();
            if (selradio == null) {
                MessageUtil.showError(this, "error.product.select_path_type");
                return true;
            }
            String pathtype = selradio.getValue();
            String path = pathbox.getValue().trim();
            if (pathtype.equals(ProductModel.PATH_CUSTOM)) {
                if (path.equals("")) {
                    MessageUtil.showError(this, "error.product.enter_path");
                    return true;
                }
                File f = new File(path);
                if (! f.isAbsolute()) {
                    MessageUtil.showError(this, "error.product.enter_absolute_path");
                    return true;
                }
                if (! f.exists()) {
                    if (MessageUtil.showYesNoQuestion(this, 
                        "question.product.create_path_directory.title",
                        "question.product.create_path_directory.msg") == MessageUtil.YES)
                    {
                        if (! f.mkdirs()) {
                            MessageUtil.showError(this, "error.product.create_directory_failed");
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
                if (! f.isDirectory()) {
                    MessageUtil.showError(this, "error.product.path_not_a_directory");
                    return true;
                }
                String[] filelist = f.list();
                if (filelist.length > 0) {
                    if (MessageUtil.showYesNoQuestion(this, "question.continue",
                        "question.product.directory_not_empty") == Messagebox.NO)
                    {
                        return true;
                    }
                }
            }
            // Check existence of embedded driver class
            if (storetype.equalsIgnoreCase(DocmaConstants.STORE_TYPE_DB_EMBEDDED)) {
                try {
                    Class.forName(FilesystemStoreProperties.DB_EMBEDDED_DRIVER);
                } catch (ClassNotFoundException cnfex) {
                    MessageUtil.showError(this, "error.product.embedded_driver_not_found", 
                                          FilesystemStoreProperties.DB_EMBEDDED_DRIVER);
                    return true;
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    MessageUtil.showError(this, "error.product.embedded_driver_error", ex.getMessage());
                    return true;
                }
            }
        }
        
        //
        // Following checks apply for new and existing stores
        //
        if (! loadError) {
            String name = namebox.getValue().trim();
            if (name.length() == 0) {
                MessageUtil.showError(this, "error.product.enter_name");
                return true;
            }
            if (! name.matches("[ \\p{javaLetterOrDigit}_+,.:-]+")) {
                MessageUtil.showError(this, "error.product.invalid_name");
                return true;
            }
            if (langslistmodel.isEmpty()) {
                MessageUtil.showError(this, "error.product.add_language");
                return true;
            }
            if (! hasOriginalLang()) {
                MessageUtil.showError(this, "error.product.set_original_language");
                return true;
            }
        }
        
        //
        // External database checks
        //
        if (storetype.equalsIgnoreCase(DocmaConstants.STORE_TYPE_DB_EXTERNAL)) {
            if (dbUrlTextbox.getValue().trim().length() == 0) {
                MessageUtil.showError(this, "error.product.enter_dburl");
                return true;
            }
            String db_driver = dbDriverTextbox.getValue().trim();
            // if (db_driver.length() == 0) {
            //     MessageUtil.showError(this, "error.product.enter_dbdriver");
            //     return true;
            // }
            
            // Show warning if JDBC driver cannot be found in classpath:
            try {
                if (db_driver.length() > 0) Class.forName(db_driver);
            } catch (ClassNotFoundException cnfex) {
                MessageUtil.showWarning(this, "error.product.jdbc_driver_not_found", db_driver);
            } catch (Throwable ex) {
                MessageUtil.showWarning(this, "error.product.jdbc_driver_error", ex.getMessage());
            }
        }
        
        return false;
    }

    public void doModal() throws SuspendNotAllowedException
    {
        modalResult = -1;
        super.doModal();
    }

    public void doModal_NewProduct() throws Exception
    {
        mode = MODE_NEW;
        setTitle(GUIUtil.i18(this).getLabel("label.product.dialog.new.title"));
        storetypeListbox.setDisabled(false);
        exportInDbCheckbox.setDisabled(false);
        pathbox.setDisabled(false);
        defaultpathradio.setDisabled(false);
        custompathradio.setDisabled(false);
        origLangButton.setDisabled(false);
        doModal();
    }

    public void doModal_EditProduct() throws Exception
    {
        mode = MODE_EDIT;
        setTitle(GUIUtil.i18(this).getLabel("label.product.dialog.edit.title"));
        storetypeListbox.setDisabled(true);
        exportInDbCheckbox.setDisabled(true);
        pathbox.setDisabled(true);
        defaultpathradio.setDisabled(true);
        custompathradio.setDisabled(true);
        origLangButton.setDisabled(true);
        doModal();
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

    public void onChangePathType() throws Exception
    {
        if (isDefaultPathSelected()) {
            setGUIDefaultPath(mainWin.getDocmaSession());
        } else {
            pathbox.setValue("");
            pathbox.setReadonly(false);
        }
    }
    
    public void onSelectStorageType() throws Exception
    {
        updateStoreTypeVisibility();
    }

    public void onAddLanguage() throws Exception
    {
        ProductLangSelectDialog dialog = (ProductLangSelectDialog) getPage().getFellow("ProductLangSelectDialog");
        dialog.resetGUI();
        dialog.doModal();
        if (dialog.getModalResult() != GUIConstants.MODAL_OKAY) {
            return;
        }
        DocmaLanguageModel[] arr = dialog.getSelectedLanguages();
        if (arr.length == 0) return;
        init();
        if (langslistmodel.isEmpty()) {
            arr[0].setTranslation(false);
        }
        for (int i=0; i < arr.length; i++) {
            if (! containsLang(arr[i].getCode())) {
                langslistmodel.add(arr[i]);
            }
        }
    }

    public void onDeleteLanguage() throws Exception
    {
        init();
        if (langslistbox.getSelectedCount() == 0) {
            MessageUtil.showError(this, "error.product.select_language");
            return;
        }
        int selidx = langslistbox.getSelectedIndex();
        DocmaLanguageModel lang = (DocmaLanguageModel) langslistmodel.get(selidx);
        if ((mode == MODE_EDIT) && !lang.isTranslation()) {
            MessageUtil.showError(this, "error.product.delete_original_not_allowed");
            return;
        }
        if (MessageUtil.showYesNoQuestion(this, "question.delete",
            "question.product.delete_language", lang.getDescription()) == Messagebox.YES) 
        {
            langslistmodel.remove(selidx);
        }
    }

    public void onSetOriginalLanguage() throws Exception
    {
        init();
        if (mode == MODE_EDIT) {
            MessageUtil.showError(this, "error.product.change_original_not_allowed");
            return;
        }
        if (langslistbox.getSelectedCount() == 0) {
            MessageUtil.showError(this, "error.product.select_language");
            return;
        }

        int oldidx = getOriginalLangIndex();
        int selidx = langslistbox.getSelectedIndex();
        if (oldidx == selidx) return; // selected language is already set as original

        DocmaLanguageModel langold = (DocmaLanguageModel) langslistmodel.get(oldidx);
        langold.setTranslation(true);
        langslistmodel.set(oldidx, langold);

        DocmaLanguageModel lang = (DocmaLanguageModel) langslistmodel.get(selidx);
        lang.setTranslation(false);
        langslistmodel.set(selidx, lang);
    }

    public int getModalResult()
    {
        return modalResult;
    }
    
    private void updateStoreTypeVisibility()
    {
        String storetype = getStoreType();
        boolean is_fs = storetype.equalsIgnoreCase(DocmaConstants.STORE_TYPE_FS);
        boolean is_emb = storetype.equalsIgnoreCase(DocmaConstants.STORE_TYPE_DB_EMBEDDED);
        boolean is_ext = storetype.equalsIgnoreCase(DocmaConstants.STORE_TYPE_DB_EXTERNAL);
        pathArea.setVisible(is_fs || is_emb);
        exportInDbArea.setVisible(is_emb || is_ext);
        
        // External DB fields:
        externalDbArea.setVisible(is_ext);
        exportInDbCheckbox.setDisabled(is_fs || (mode == MODE_EDIT));
        dbUrlTextbox.setDisabled(! is_ext);
        dbDialectListbox.setDisabled(! is_ext);
        dbDriverTextbox.setDisabled(! is_ext);
        dbUserTextbox.setDisabled(! is_ext);
        dbPasswdTextbox.setDisabled(! is_ext);
    }

    public void updateGUI(ProductModel pm, DocmaSession docmaSess)
    {
        init();
        loadError = pm.isLoadError();
        if (loadError) {
            hintLabel.setStyle("color:#800000;font-weight:bold;");
            hintLabel.setValue("Load Error! Please check the connection data.");
            hintLabel.setVisible(true);
        } else {
            hintLabel.setStyle("");
            hintLabel.setValue("");
            hintLabel.setVisible(false);
        }
        idbox.setValue(pm.getId());
        namebox.setValue(pm.getName());
        namebox.setDisabled(loadError);
        String storetype = pm.getStoreType();
        GUIUtil.selectListItem(storetypeListbox, storetype);
        updateStoreTypeVisibility();
        String pathtype = pm.getPathtype();
        GUIUtil.selectRadio(pathgroup, pathtype);
        if (ProductModel.PATH_CUSTOM.equals(pathtype)) {
            pathbox.setValue(pm.getPath());
            pathbox.setReadonly(false);
        } else
        if (ProductModel.PATH_DEFAULT.equals(pathtype)) {
            setGUIDefaultPath(docmaSess);
        } else {
            pathbox.setValue("");
            pathbox.setReadonly(true);
        }
        DocmaLanguage[] arr = pm.getTranslationLanguages();
        if (arr == null) {
            arr = new DocmaLanguage[0];
        }
        List lang_models = new ArrayList(arr.length + 1);
        DocmaLanguage orig_lang = pm.getOriginalLanguage();
        if (orig_lang != null) {
            lang_models.add(new DocmaLanguageModel(orig_lang, false));
        }
        for (int i=0; i < arr.length; i++) {
            lang_models.add(new DocmaLanguageModel(arr[i], true));
        }
        langslistmodel.clear();
        if (! loadError) {
            langslistmodel.addAll(lang_models);
        }

        // Set fields for database stores
        exportInDbCheckbox.setChecked(pm.isDbArchive());
        if (storetype.equalsIgnoreCase(DocmaConstants.STORE_TYPE_DB_EXTERNAL)) {
            String db_url = pm.getDbUrl();
            String db_driver = pm.getDbDriver();
            dbUrlTextbox.setValue((db_url != null) ? db_url : "");
            dbDriverTextbox.setValue((db_driver != null) ? db_driver : "");
            String db_dialect = pm.getDbDialect();
            if (db_dialect == null) {
                db_dialect = "";  // select auto-detect as default
            }
            if (! GUIUtil.selectListItem(dbDialectListbox, db_dialect)) { 
                // If dialect is not in the list (e.g. because the user has  
                // manually edited the properties file), then add the dialect 
                // to the list.
                dbDialectListbox.appendItem(db_dialect, db_dialect);
                GUIUtil.selectListItem(dbDialectListbox, db_dialect);
            }
            String usr_id = pm.getDbUser();
            String usr_pw = pm.getDbPasswd();
            dbUserTextbox.setValue((usr_id != null) ? usr_id : "");
            dbPasswdTextbox.setValue((usr_pw != null) ? usr_pw : "");
        }
    }

    public void updateModel(ProductModel pm)
    {
        init();
        Radio pathradio = pathgroup.getSelectedItem();

        pm.setId(idbox.getValue().trim());
        pm.setName(namebox.getValue().trim());
        // if (mode == MODE_NEW) { // store type and archive type cannot be changed in edit mode
        pm.setStoreType(getStoreType());
        pm.setDbArchive(exportInDbCheckbox.isChecked());
        // }
        String pathtype;
        if (pathradio == null) {
            pathtype = ProductModel.PATH_DEFAULT; 
        } else { 
            Object obj = pathradio.getValue();
            pathtype = (obj == null) ? ProductModel.PATH_DEFAULT : obj.toString(); 
        }
        pm.setPathtype(pathtype);
        String path = "";
        if (! pathtype.equalsIgnoreCase(ProductModel.PATH_DEFAULT)) {
            path = pathbox.getValue().trim();
        }
        pm.setPath(path);
        DocmaLanguageModel[] langs = new DocmaLanguageModel[langslistmodel.size()];
        langs = (DocmaLanguageModel[]) langslistmodel.toArray(langs);
        pm.setLanguages(langs);
        
        // Update fields for database stores
        if (pm.getStoreType().equalsIgnoreCase(DocmaConstants.STORE_TYPE_DB_EXTERNAL)) {
            pm.setDbUrl(dbUrlTextbox.getValue().trim());
            pm.setDbDriver(dbDriverTextbox.getValue().trim());
            pm.setDbDialect(getDbDialect());
            pm.setDbUser(dbUserTextbox.getValue().trim());
            pm.setDbPasswd(dbPasswdTextbox.getValue());
        }
    }
    
    private String getDbDialect()
    {
        Listitem item = dbDialectListbox.getSelectedItem();
        if (item != null) {
            Object val = item.getValue();
            return (val != null) ? val.toString() : "";
        } else {
            return "";  // auto-detect dialect
        }
    }
    
    private String getStoreType()
    {
        Listitem item = storetypeListbox.getSelectedItem();
        if (item != null) { 
            Object val = item.getValue();
            return (val != null) ? val.toString() : "";
        } else {
            return "";  // no store type has been selected
        }
    }

    private int getOriginalLangIndex()
    {
        int cnt = langslistmodel.getSize();
        for (int i=0; i < cnt; i++) {
            DocmaLanguageModel lang = (DocmaLanguageModel) langslistmodel.get(i);
            if (! lang.isTranslation()) return i;
        }
        return -1;
    }

    private boolean hasOriginalLang()
    {
        return (getOriginalLangIndex() >= 0);
    }

    private boolean containsLang(String lang_code)
    {
        int cnt = langslistmodel.getSize();
        for (int i=0; i < cnt; i++) {
            DocmaLanguageModel lang = (DocmaLanguageModel) langslistmodel.get(i);
            if (lang.getCode().equalsIgnoreCase(lang_code)) return true;
        }
        return false;
    }

    private void setGUIDefaultPath(DocmaSession docmaSess)
    {
        String storespath = docmaSess.getApplicationProperty(DocmaConstants.PROP_STORES_PATH);
        if ((storespath == null) || storespath.equals("")) {
            pathbox.setValue("???");
        } else {
            String idlabel = GUIUtil.i18(this).getLabel("label.product.id");
            File f = new File(storespath, "<" + idlabel + ">");
            pathbox.setValue(f.getAbsolutePath());
        }
        pathbox.setReadonly(true);
    }

    private boolean isDefaultPathSelected()
    {
        Radio selradio = pathgroup.getSelectedItem();
        if (selradio != null) {
            String pathtype = selradio.getValue();
            return pathtype.equals(ProductModel.PATH_DEFAULT);
        }
        return false;
    }

    private boolean isCustomPathSelected()
    {
        Radio selradio = pathgroup.getSelectedItem();
        if (selradio != null) {
            String pathtype = selradio.getValue();
            return pathtype.equals(ProductModel.PATH_CUSTOM);
        }
        return false;
    }

}
