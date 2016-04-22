/*
 * GUI_List_Products.java
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

import java.util.*;
import java.io.File;

import org.docma.app.*;
import org.docma.app.ui.ProductModel;
import org.docma.coreapi.*;
import org.docma.util.Log;

import org.zkoss.zul.*;
import org.zkoss.zul.Timer;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.Clients;

/**
 *
 * @author MP
 */
public class GUI_List_Products implements EventListener
{
    private MainWindow     mainWin;
    private DocmaI18       docmaI18n = null;   // see method i18nLabel()
    private Listbox        products_listbox;
    private ListModelList  products_listmodel;
    private Timer          refreshTimer = null; 
    private long           lastRefresh = 0;
    private boolean        skipRefresh = false;

    private static final List<ProductsConnectThread> globalConnectThreads = new ArrayList<ProductsConnectThread>();
    private ProductsConnectThread connectThread = null;
    private final List<ProductModel> loadedDbProducts = new ArrayList<ProductModel>();
    
    
    public GUI_List_Products(MainWindow mainWin)
    {
        this.mainWin = mainWin;
        
        products_listbox = (Listbox) mainWin.getFellow("ProductsListbox");
        products_listmodel = new ListModelList();
        products_listbox.setModel(products_listmodel);
        products_listbox.setItemRenderer(new ProductModelRenderer());
    }
    
    // ListModelList getListModel()
    // {
    //     return products_listmodel;
    // }
    
    synchronized void loadProducts()
    {
        products_listmodel.clear();
        DocmaSession docmaSess = mainWin.getDocmaSession();
        String[] storeIds = docmaSess.listDocStores();
        Arrays.sort(storeIds);
        
        ProductModel currentlyLoading = getLoadingProduct();
        String loadingId = (currentlyLoading == null) ? null : currentlyLoading.getId();
        
        boolean periodic_refresh = false;
        boolean load_from_db = false;
        for (int i=0; i < storeIds.length; i++) {
            ProductModel pm;
            if ((loadingId != null) && loadingId.equals(storeIds[i])) {
                pm = currentlyLoading;
            } else {
                pm = new ProductModel(docmaSess, storeIds[i], false);
            }
            products_listmodel.add(pm);
            if (pm.isStoreTypeDbExternal()) {
                if (pm.isLoadPending() || pm.isLoading()) {
                    load_from_db = true;
                    periodic_refresh = true;
                }
            }
            if (pm.hasActivity() && !pm.isActivityFinished()) {
                periodic_refresh = true;
            }
        }
        setListRefresh(periodic_refresh);
        if (load_from_db) {
            startDbConnectThread();
        }
    }
    
    synchronized void addProductModel(ProductModel pm)
    {
        products_listmodel.add(pm);
        if (pm.isLoadPending()) {
            setListRefresh(true);
            startDbConnectThread();
        }
    }
    
    synchronized ProductModel getPendingExtDbProduct()
    {
        for (Object obj : products_listmodel) {
            ProductModel pm = (ProductModel) obj;
            if (pm.isStoreTypeDbExternal() && pm.isLoadPending()) {
                return pm;
            }
        }
        return null;
    }
    
    synchronized ProductModel getLoadingProduct()
    {
        if ((connectThread == null) || !connectThread.isAlive()) { 
            return null; 
        } else { 
            return connectThread.getLoadingProduct(); 
        }
    }
    
    private synchronized void startDbConnectThread()
    {
        if ((connectThread != null) && connectThread.isAlive() && !connectThread.isFinished()) {
            return;  // Current thread is still running. No new thread needs to be created. 
        }
        connectThread = new ProductsConnectThread(mainWin, this);
        connectThread.start();
        synchronized (globalConnectThreads) {
            cleanGlobalConnectThreads();
            globalConnectThreads.add(connectThread);
            
            int cnt = globalConnectThreads.size();
            if (DocmaConstants.DEBUG || (cnt > 30)) {
                Log.info("globalConnectThreads: " + cnt);
            }
        }
    }
    
    private synchronized void interruptDbConnectThread(String productId)
    {
        if (connectThread != null) {
            if (connectThread.isAlive()) {
                ProductModel pm = connectThread.getLoadingProduct();
                try {
                    if ((productId == null) || 
                        ((pm != null) && productId.equals(pm.getId()))) {
                        connectThread.interrupt();
                    }
                } catch (Exception ex) { ex.printStackTrace(); }
            } else {
                connectThread = null;
            }
        }
    }
    
    private void cleanGlobalConnectThreads()
    {
        Iterator<ProductsConnectThread> it = globalConnectThreads.iterator();
        while (it.hasNext()) {
            ProductsConnectThread ct = it.next();
            if (ct.isAlive()) {
                try {
                    long now = System.currentTimeMillis();
                    long running_time = now - ct.getCreationTime();
                    // if (running_time > 1000*60*60*24) {
                    //     ct.stop();
                    // } else
                    if (running_time > 1000*60*60) {  // 1 hour
                        ct.interrupt();
                    }
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            } else {
                it.remove();
            }
        }
    }
    
    private synchronized boolean refreshActivities(Set<String> skip_products)
    {
        // System.out.println("--- Start refreshActivities");
        DocmaSession docmaSess = mainWin.getDocmaSession();
        boolean periodic_refresh = false;
        for (int i=0; i < products_listmodel.size(); i++) {
            try {
                ProductModel pm = (ProductModel) products_listmodel.get(i);
                // System.out.println("--- Start refreshActivities: " + pm.getId());
                if (skip_products.contains(pm.getId()) || pm.isLoadPending() || pm.isLoading()) {  // if not loaded (DB connection pending)
                    periodic_refresh = true;  // wait until pm is loaded
                    continue;
                }
                // System.out.println("--- Start refreshActivities: check existence of activity for " + pm.getId());
                if (pm.hasActivity()) {    // && !pm.isActivityFinished()
                    // System.out.println("--- Start refreshActivities: activity exists for " + pm.getId());
                    if (! pm.isActivityFinished()) {
                        periodic_refresh = true;
                    }
                    long old_act_id = pm.getActivityId();
                    Activity act = docmaSess.getDocStoreActivity(pm.getId());
                    if ((act != null) && (act.getActivityId() == old_act_id)) {
                        boolean is_finished = act.isFinished();
                        pm.refreshActivity(docmaSess);
                        long act_id = pm.getActivityId();
                        int percent = pm.getActivityPercent();
                        // Update percent, progress message and buttons
                        Label msg = getActicityMessageLabel(act_id);
                        Label per = getActicityPercentLabel(act_id);
                        Progressmeter met = getActicityProgressMeter(act_id);
                        Button closebtn = getActivityCloseButton(act_id);
                        Button cancelbtn = getActivityCancelButton(act_id);
                        Button logbtn = getActivityLogButton(act_id);
                        met.setValue(percent);
                        updateActivityMsg(msg, pm);
                        closebtn.setDisabled(! is_finished);
                        closebtn.setVisible(is_finished);
                        cancelbtn.setDisabled(is_finished || pm.isActivityCanceledByUser());
                        cancelbtn.setVisible(! is_finished);
                        logbtn.setDisabled(! is_finished);
                        logbtn.setVisible(is_finished);
                        per.setValue(percent + "%");
                        per.setVisible(! is_finished);
                        per.getParent().invalidate();
                        // Listitem item = products_listbox.getItemAtIndex(i);
                        // item.invalidate();
                        // products_listmodel.set(i, pm);
                    } else {
                        // Rerender complete row
                        ProductModel pm_updated = new ProductModel(docmaSess, pm.getId(), false);
                        products_listmodel.set(i, pm_updated);
                        if (pm_updated.isLoadPending()) {
                            startDbConnectThread();
                        }
                    }
                }
            } catch (Exception ex) {
                Log.error("Error in refreshActivities:" + ex.getMessage());
                // periodic_refresh = true;
            }
        }
        // System.out.println("--- End refreshActivities");
        return periodic_refresh;
    }
    
    private synchronized Set<String> refreshLoadingStates()
    {
        Set<String> refresh_ids = new HashSet<String>();
        // System.out.println("--- Start refreshLoadingStates");
        // Update name label of products that have been loaded by the ProductsConnectThread.
        synchronized (loadedDbProducts) {
            if (! loadedDbProducts.isEmpty()) {
                for (ProductModel pm : loadedDbProducts) {
                    refresh_ids.add(pm.getId());
                    // Rerender complete row (if activity exists, activity is rendered too)
                    int pidx = getProductModelIndexById(pm.getId());
                    if (pidx >= 0) {
                        products_listmodel.set(pidx, (ProductModel) pm.clone());
                    } else {
                        Log.error("Product model not contained in list: " + pm.getId());
                    }
                }
                loadedDbProducts.clear();
            }
        }
        
        // Check if pending or loading products still exist.
        // Continue list refresh until all enabled products have been loaded. 
        for (Object obj : products_listmodel) {
            ProductModel pm = (ProductModel) obj;
            if (pm.isLoadPending() || pm.isLoading()) {
                refresh_ids.add(pm.getId());
                Listcell cell = getProductNameCell(pm.getId());
                if (cell != null) {
                    updateNameCell(cell, pm);
                }
                break;
            }
        }
        // System.out.println("--- End refreshLoadingStates");
        return refresh_ids;
    }

    /**
     * This method is called by ProductsConnectThread.
     * @param pm The product model for which loading has finished.
     */
    void loadingFinished(ProductModel pm)
    {
        // System.out.println("--- Loading finished: " + pm.getId());
        synchronized (loadedDbProducts) {
            loadedDbProducts.add(pm);
        }
    }
    
    void renderNewActivity(String pm_id)
    {
        DocmaSession docmaSess = mainWin.getDocmaSession();
        int idx = getProductModelIndexById(pm_id);
        ProductModel pm = (ProductModel) products_listmodel.get(idx);
        ProductModel pm_updated = (ProductModel) pm.clone();  // new instance to force GUI update
        pm_updated.refreshActivity(docmaSess);
        products_listmodel.set(idx, pm_updated);
        setListRefresh(true);
    }

    void refreshActivity(String pm_id)
    {
        DocmaSession docmaSess = mainWin.getDocmaSession();
        int idx = getProductModelIndexById(pm_id);
        ProductModel pm = (ProductModel) products_listmodel.get(idx);
        ProductModel pm_updated = (ProductModel) pm.clone();  // new instance to force GUI update
        pm_updated.refreshActivity(docmaSess);
        products_listmodel.set(idx, pm_updated);
        setListRefresh(true);
    }

    /**
     * Replace the existing product model by a newly created product model 
     * (re-read all product model data from the persistence layer) and render
     * the new product model.
     * @param pm_id Product identifier.
     */
    private synchronized void refreshProductModel(String pm_id)
    {
        DocmaSession docmaSess = mainWin.getDocmaSession();
        for (int i=0; i < products_listmodel.size(); i++) {
            ProductModel pm = (ProductModel) products_listmodel.get(i);
            if (pm.getId().equals(pm_id)) {
                ProductModel pm_updated = new ProductModel(docmaSess, pm_id, false);
                products_listmodel.set(i, pm_updated);
                boolean pending = pm_updated.isLoadPending();
                if (pending || (pm_updated.hasActivity() && !pm_updated.isActivityFinished())) {
                    setListRefresh(true);
                    if (pending) startDbConnectThread();
                }
                break;
            }
        }
    }

    /**
     * Replace the existing product model by a newly created product model 
     * (re-read all product model data from the persistence layer) and render
     * the new product model.
     * @param pm Product model to be replaced.
     */
    private synchronized void refreshProductModel(ProductModel pm)
    {
        int idx = products_listmodel.indexOf(pm);
        if (idx >= 0) {
            DocmaSession docmaSess = mainWin.getDocmaSession();
            // Create new model object to force GUI update
            ProductModel pm_updated = new ProductModel(docmaSess, pm.getId(), false);
            products_listmodel.set(idx, pm_updated);
            boolean pending = pm_updated.isLoadPending();
            if (pending || (pm_updated.hasActivity() && !pm_updated.isActivityFinished())) {
                setListRefresh(true);
                if (pending) startDbConnectThread();
            }
        } else {
            Log.warning("ProductModel not contained in list: " + pm.getId());
        }
    }

    private void setListRefresh(boolean enabled)
    {
        if (refreshTimer == null) {
            refreshTimer = (Timer) mainWin.getFellow("ProductListRefreshTimer");
            refreshTimer.removeEventListener("onTimer", this);  // not required; should always return false
            refreshTimer.addEventListener("onTimer", this);
        }
        refreshTimer.setRunning(enabled);
    }
    
    synchronized void refresh() 
    {
        if (! skipRefresh) {
            loadProducts();
        }
    }
    
    synchronized int getCount()
    {
        return products_listmodel.getSize();
    }
    
    synchronized ProductModel[] getProducts()
    {
        ProductModel[] arr = new ProductModel[products_listmodel.getSize()];
        return (ProductModel[]) products_listmodel.toArray(arr);
    }
    
    synchronized ProductModel[] getSelectedProducts()
    {
        Set sel_set = products_listmodel.getSelection();
        if (sel_set == null) {
            return new ProductModel[0];
        } else {
            return (ProductModel[]) sel_set.toArray(new ProductModel[sel_set.size()]);
        }
    }

    public void onNewProduct() throws Exception
    {
        ProductDialog dialog = (ProductDialog) mainWin.getPage().getFellow("ProductDialog");
        DocmaSession docmaSess = mainWin.getDocmaSession();
        ProductModel pm = new ProductModel();   // product with empty fields
        dialog.newProduct(pm, this, docmaSess);
    }


    public void onEditProduct() throws Exception
    {
        skipRefresh = true;
        try {
            ProductDialog dialog = (ProductDialog) mainWin.getPage().getFellow("ProductDialog");
            DocmaSession docmaSess = mainWin.getDocmaSession();
            if (products_listbox.getSelectedCount() == 0) {
                Messagebox.show(i18nLabel("text.product.select_product"));
                return;
            }
            int sel_idx = products_listbox.getSelectedIndex();
            ProductModel pm = (ProductModel) products_listmodel.get(sel_idx);
            
            if (pm.isStoreTypeFs() || pm.isStoreTypeDbEmbedded()) {
                if (pm.getPathtype().equals(ProductModel.PATH_CUSTOM)) {
                    File cust_dir = new File(pm.getPath());
                    // File prop_file = new File(cust_dir, FilesystemStoreProperties.STORE_PROP_FILENAME);
                    if (! (cust_dir.exists() && cust_dir.isDirectory())) {
                        Messagebox.show(i18nLabel("text.product.invalid_path"));
                        return;
                    }
                }
            }
            if (pm.isLoadPending() || pm.isLoading()) {
                // System.out.println("--- Open Messagebox waiting for connection: " + pm.getId());
                Messagebox.show(i18nLabel("text.product.waiting_for_connection"));
                return;
            }

            // System.out.println("--- Open edit product dialog: " + pm.getId());
            dialog.updateGUI(pm, docmaSess);  // init dialog fields
            boolean has_error = true;
            do {
                dialog.doModal_EditProduct();
                if (dialog.getModalResult() != GUIConstants.MODAL_OKAY) {
                    return;
                }
                if (dialog.hasInvalidInputs(docmaSess)) {
                    continue;
                }
                ProductModel pm_edited = new ProductModel();
                dialog.updateModel(pm_edited);

                String opened_sid = docmaSess.getStoreId();
                DocVersionId opened_vid = docmaSess.getVersionId();
                boolean is_open = (opened_sid != null) && opened_sid.equals(pm.getId());
                if (is_open) {
                    mainWin.closeDocStore(docmaSess);
                }

                // Handle update of external database connection properties:
                if (pm_edited.isStoreTypeDbExternal()) {
                    boolean unchanged = pm_edited.getDbUrl().equals(pm.getDbUrl()) &&
                                        pm_edited.getDbDriver().equals(pm.getDbDriver()) &&
                                        pm_edited.getDbDialect().equals(pm.getDbDialect()) &&
                                        pm_edited.getDbUser().equals(pm.getDbUser()) &&
                                        pm_edited.getDbPasswd().equals(pm.getDbPasswd());
                    if (! unchanged) {
                        String[] db_names = {
                            DocmaConstants.PROP_STORE_DB_CONNECTION_URL,
                            DocmaConstants.PROP_STORE_DB_DRIVER_CLASS, 
                            DocmaConstants.PROP_STORE_DB_DIALECT, 
                            DocmaConstants.PROP_STORE_DB_CONNECTION_USER, 
                            DocmaConstants.PROP_STORE_DB_CONNECTION_PWD
                        };
                        String[] db_values = {
                            pm_edited.getDbUrl(),
                            pm_edited.getDbDriver(), 
                            pm_edited.getDbDialect(), 
                            pm_edited.getDbUser(), 
                            pm_edited.getDbPasswd()
                        };
                        docmaSess.setDocStoreProperties(pm.getId(), db_names, db_values);
                    }
                }

                // Handle change of store id:
                if (! pm_edited.getId().equals(pm.getId())) {
                    // Check if other users are still connected:
                    if (docmaSess.usersConnected(pm.getId(), null)) {
                        Messagebox.show(i18nLabel("text.product.change_id.users_connected", pm.getId()));
                    } else {
                        docmaSess.changeDocStoreId(pm.getId(), pm_edited.getId());
                        mainWin.removeStoreFromSelectList(pm.getId());
                        mainWin.addStoreToSelectList(pm_edited.getId());
                    }
                }

                // Update general store properties
                if (pm.isLoadError()) {
                    // if connection properties have been edited, reload from new connection
                    pm_edited.refresh(docmaSess);
                } else {
                    String[] propnames = {
                            DocmaConstants.PROP_STORE_DISPLAYNAME,
                            DocmaConstants.PROP_STORE_ORIG_LANGUAGE,
                            DocmaConstants.PROP_STORE_TRANSLATION_LANGUAGES };
                    String[] propvalues = {
                            pm_edited.getName(),
                            pm_edited.getOriginalLanguage().getCode(),
                            pm_edited.getTranslationLanguageCodesAsString() };
                    docmaSess.setDocStoreProperties(pm_edited.getId(), propnames, propvalues);
                }

                // Refresh user interface
                synchronized (this) {
                    sel_idx = products_listmodel.indexOf(pm);  // if list has been refreshed, get new position
                    products_listmodel.set(sel_idx, pm_edited);
                    boolean pending = pm_edited.isLoadPending();
                    if (pending || (pm_edited.hasActivity() && !pm_edited.isActivityFinished())) {
                        setListRefresh(true);
                        if (pending) startDbConnectThread();
                    }
                }

                if (is_open) {  // reopen store
                    mainWin.openDocStore(docmaSess, pm_edited.getId(), opened_vid);
                }
                has_error = false;
            } while (has_error);

        } catch (Exception ex) {
            Messagebox.show("Error: " + ex.getMessage());
        } finally {
            skipRefresh = false;
        }
    }

    public void onAddExternalProduct() throws Exception
    {
        ProductPathDialog dialog = (ProductPathDialog) mainWin.getPage().getFellow("ProductPathDialog");
        DocmaSession docmaSess = mainWin.getDocmaSession();
        if (dialog.addExternalProduct(docmaSess)) {
            String pid = dialog.getProductId();
            addProductModel(new ProductModel(docmaSess, pid));
            mainWin.addStoreToSelectList(pid);
        }
    }

    public void onChangeProductPath() throws Exception
    {
        skipRefresh = true;
        try {
            ProductPathDialog dialog = (ProductPathDialog) mainWin.getPage().getFellow("ProductPathDialog");
            DocmaSession docmaSess = mainWin.getDocmaSession();
            if (products_listbox.getSelectedCount() == 0) {
                Messagebox.show(i18nLabel("text.product.select_product"));
                return;
            }
            int sel_idx = products_listbox.getSelectedIndex();
            ProductModel pm = (ProductModel) products_listmodel.get(sel_idx);

            if (pm.isStoreTypeDbExternal()) {
                Messagebox.show(i18nLabel("text.product.operation_not_supported_for_ext_db"));
                return;
            }
            if (pm.getPathtype().equals(ProductModel.PATH_DEFAULT)) {
                Messagebox.show(i18nLabel("text.product.cannot_change_default_path"));
                return;
            }

            String opened_sid = docmaSess.getStoreId();
            DocVersionId opened_vid = docmaSess.getVersionId();
            boolean is_open = (opened_sid != null) && opened_sid.equals(pm.getId());
            if (is_open) {
                mainWin.closeDocStore(docmaSess);
            }
            if (dialog.editProductPath(pm.getId(), docmaSess)) {
                ProductModel pm_edited = new ProductModel(docmaSess, pm.getId());
                synchronized (this) {
                    sel_idx = products_listmodel.indexOf(pm);  // if list has been refreshed, get new position
                    products_listmodel.set(sel_idx, pm_edited);
                }
            }
            if (is_open) {  // reopen store
                mainWin.openDocStore(docmaSess, opened_sid, opened_vid);
            }
        } finally {
            skipRefresh = false;
        }
    }

    public void onDeleteProduct() throws Exception
    {
        skipRefresh = true;
        try {
            DocmaSession docmaSess = mainWin.getDocmaSession();
            if (products_listbox.getSelectedCount() == 0) {
                Messagebox.show(i18nLabel("text.product.select_product"));
                return;
            }
            int sel_idx = products_listbox.getSelectedIndex();
            ProductModel pm = (ProductModel) products_listmodel.get(sel_idx);
            
            String dialog_title = i18nLabel("text.product.confirm_delete.title");
            boolean delete_product = false;
            boolean remove_connection = false;
            boolean is_ext_db = pm.isStoreTypeDbExternal();
            if (is_ext_db) {
                GenericMessageDialog dialog = (GenericMessageDialog) mainWin.getPage().getFellow("GenericMessageDialog");
                dialog.setWidth("460px");
                String msg = i18nLabel("text.product.confirm_delete.message", pm.getId());
                String[] btns = new String[3];
                btns[0] = i18nLabel("label.products.remove_db_connection.btn");
                btns[1] = i18nLabel("label.products.delete_product_data.btn");
                btns[2] = i18nLabel("label.cancel.btn");
                String[] details = {
                    i18nLabel("text.product.remove_db_connection.detail_message", btns[0]),
                    i18nLabel("text.product.delete_product_data.detail_message", btns[1]) 
                    + "\n" + i18nLabel("text.product.delete_product_data.warning")
                };
                int btn_idx = dialog.showDialog(dialog_title, msg, details, btns);
                if (btn_idx == 0) {  // remove connection to database
                    remove_connection = true;
                } else
                if (btn_idx == 1) {  // delete product in database
                    delete_product = true;
                }
                // if (remove_connection && !pm.isDbArchive()) {
                //     Messagebox.show(i18nLabel("text.product.remove_db_connection.error_fsarchive"));
                //     return;
                // }
                if ((remove_connection || delete_product) && pm.isLoading()) {
                    interruptDbConnectThread(pm.getId());
                    Thread.sleep(50);  // wait a little before actually deleting the product
                }
            } else {
                String msg;
                String pm_path = pm.getPath();
                pm_path = (pm_path == null) ? "" : pm_path.trim();
                boolean is_custom_path = pm.isPathtypeCustom() && !pm_path.equals("");
                if (is_custom_path) {
                    msg = i18nLabel("text.product.remove_folder_connection", pm.getId(), pm_path);
                } else {
                    msg = i18nLabel("text.product.confirm_delete.message", pm.getId()) + "\n" + 
                          i18nLabel("text.product.delete_product_data.warning");
                }
                if (Messagebox.show(msg, dialog_title,
                    Messagebox.YES | Messagebox.NO, Messagebox.QUESTION) == Messagebox.YES) 
                {
                    if (is_custom_path) {
                        remove_connection = true;
                    } else {
                        delete_product = true;
                    }
                }
            }
            if (delete_product) {
                // Message: This operation cannot be undone! Click 'OK' to really delete all data of product ... 
                String msg = i18nLabel("text.product.delete_product_continue.message", pm.getId());
                if (Messagebox.show(msg, dialog_title,
                    Messagebox.OK | Messagebox.CANCEL, Messagebox.EXCLAMATION) == Messagebox.CANCEL) 
                {
                    delete_product = false;   // canceled
                }
            }
            if (delete_product || remove_connection) {
                // Close the product to be deleted, if it is currently opened.
                String opened_sid = docmaSess.getStoreId();
                boolean is_open = (opened_sid != null) && opened_sid.equals(pm.getId());
                if (is_open) {
                    mainWin.closeDocStore(docmaSess);
                }
                
                // Delete the product or remove connection to product.
                if (remove_connection) {
                    docmaSess.removeExternalDocStoreConnection(pm.getId());
                } else {
                    docmaSess.deleteDocStore(pm.getId());
                }
                
                // Remove the product from the product lists in the GUI.
                synchronized (this) {
                    // sel_idx = products_listmodel.indexOf(pm);  // if list has been refreshed, get new position
                    products_listmodel.remove(pm);
                }
                mainWin.removeStoreFromSelectList(pm.getId()); // remove product from select list
            }
        } catch (Exception ex) {
            Messagebox.show("Error: " + ex.getMessage());
        } finally {
            skipRefresh = false;
        }
    }

    public void onEnableProduct() throws Exception
    {
        skipRefresh = true;
        try {
            DocmaSession docmaSess = mainWin.getDocmaSession();
            if (products_listbox.getSelectedCount() <= 0) {
                Messagebox.show(i18nLabel("text.product.select_product"));
                return;
            }
            synchronized (this) {
                Set sel_set = products_listmodel.getSelection();
                Iterator it = sel_set.iterator();
                while (it.hasNext()) {
                    ProductModel pm = (ProductModel) it.next();
                    docmaSess.setStoreDisabled(pm.getId(), false);
                    refreshProductModel(pm); // Create new model object to force GUI update
                    mainWin.addStoreToSelectList(pm.getId());
                }
            }
        } catch (Exception ex) {
            Messagebox.show("Error: " + ex.getMessage());
        } finally {
            skipRefresh = false;
        }
    }

    public void onDisableProduct() throws Exception
    {
        skipRefresh = true;
        try {
            DocmaSession docmaSess = mainWin.getDocmaSession();
            Set sel_set = products_listmodel.getSelection();
            if (sel_set.isEmpty()) {
                Messagebox.show(i18nLabel("text.product.select_product"));
                return;
            }
            int sel_cnt = sel_set.size();
            boolean confirm = true;
            Iterator it = sel_set.iterator();
            while (it.hasNext()) {
                ProductModel pm = (ProductModel) it.next();
                String pid = pm.getId();
                if (confirm) {
                    confirm = false;
                    String msg;
                    if (sel_cnt == 1) {
                        msg = i18nLabel("text.product.confirm_disable.single", pid);
                    } else {
                        msg = i18nLabel("text.product.confirm_disable.multiple", sel_cnt);
                    }
                    if (Messagebox.show(msg, i18nLabel("text.product.confirm_disable.title"), 
                                        Messagebox.YES | Messagebox.NO, 
                                        Messagebox.QUESTION) == Messagebox.NO) {
                        break;
                    }
                }
                
                // Close connection if user is still connected to the product to be disabled:
                String opened_sid = docmaSess.getStoreId();
                boolean is_open = (opened_sid != null) && opened_sid.equals(pid);
                if (is_open) {
                    mainWin.closeDocStore(docmaSess);
                }
                // Check if other users are still connected:
                if (docmaSess.usersConnected(pid, null)) {
                    Messagebox.show(i18nLabel("text.product.disable.users_connected", pid));
                    continue;
                }

                // Disable product
                synchronized (this) {
                    docmaSess.setStoreDisabled(pid, true);
                    refreshProductModel(pm);  // Create new model object to force GUI update
                }
                // Remove product from select list:
                mainWin.removeStoreFromSelectList(pm.getId());
            }
        } catch (Exception ex) {
            Messagebox.show("Error: " + ex.getMessage());
        } finally {
            skipRefresh = false;
        }
    }
    
    public void onCopyProduct() throws Exception
    {
        CopyProductDialog dialog = (CopyProductDialog) mainWin.getPage().getFellow("CopyProductDialog");
        dialog.copyProduct(mainWin, this);
    }

    private synchronized int getProductModelIndexById(String productId)
    {
        for (int i = 0; i < products_listmodel.size(); i++) {
            Object obj = products_listmodel.get(i);
            if (obj instanceof ProductModel) {
                ProductModel pm = (ProductModel) obj;
                if (productId.equals(pm.getId())) return i;
            }
        }
        return -1;
    }
    
//    private ProductModel getProductModelByActivityId(long actId)
//    {
//        for (Object obj : products_listmodel) {
//            if (obj instanceof ProductModel) {
//                ProductModel pm = (ProductModel) obj;
//                if (pm.getActivityId() == actId) return pm;
//            }
//        }
//        return null;
//    }

    private String productNameCellId(String productId)
    {
        return "prodlist_namecell_" + productId;
    }
    
    private String actPercentLabelId(long actId)
    {
        return "act_percentlabel_" + actId;
    }
    
    private String actCloseButtonId(long actId)
    {
        return "act_closebtn_" + actId;
    }
    
    private String actProgressMeterId(long actId)
    {
        return "act_progressmeter_" + actId;
    }
    
    private String actMessageLabelId(long actId)
    {
        return "act_msglabel_" + actId;
    }
    
    private String actCancelButtonId(long actId)
    {
        return "act_cancelbtn_" + actId;
    }
    
    private String actLogButtonId(long actId)
    {
        return "act_logbtn_" + actId;
    }
    
    private Listcell getProductNameCell(String productId) 
    {
        return (Listcell) products_listbox.getFellow(productNameCellId(productId));
    }
    
    private Label getActicityPercentLabel(long actId) 
    {
        return (Label) products_listbox.getFellow(actPercentLabelId(actId));
    }

    private Progressmeter getActicityProgressMeter(long actId) 
    {
        return (Progressmeter) products_listbox.getFellow(actProgressMeterId(actId));
    }

    private Label getActicityMessageLabel(long actId) 
    {
        return (Label) products_listbox.getFellow(actMessageLabelId(actId));
    }
    
    private Toolbarbutton getActivityCloseButton(long actId)
    {
        return (Toolbarbutton) products_listbox.getFellow(actCloseButtonId(actId));
    }

    private Button getActivityCancelButton(long actId)
    {
        return (Button) products_listbox.getFellow(actCancelButtonId(actId));
    }

    private Button getActivityLogButton(long actId)
    {
        return (Button) products_listbox.getFellow(actLogButtonId(actId));
    }
    
    private void updateNameCell(Listcell cell, ProductModel pm)
    {
        boolean is_error = pm.isLoadError();
        boolean is_pending = pm.isLoadPending();
        boolean is_loading = pm.isLoading();
        cell.setLabel(is_error   ? pm.getLoadErrorMessage() : 
                      is_pending ? "Waiting for database connection..." : 
                      is_loading ? "Connecting database..." : pm.getName());
        if (is_error) {
            final String error_style = "color:#800000;";
            cell.setStyle(error_style);
        } else
        if (is_pending || is_loading) {
            final String busy_style = "color:#A0A0A0;font-style:italic;";
            cell.setStyle(busy_style);
        } else {
            cell.setStyle("");
        }
    }
    
    private void updateActivityMsg(Label msg_label, ProductModel pm) 
    {
        msg_label.setValue(pm.getActivityMsg(getI18n()));
        if (pm.isActivityError()) {
            msg_label.setStyle("color:#C00000;font-size:1em;");  // mark red if errors exist
        } else {
            // mark green if activity finished successfully, otherwise use default font color
            msg_label.setStyle(pm.isActivityFinished() ? "color:#00c000;font-size:1em;" : "font-size:1em;");
        }
    }

    private String i18nLabel(String key, Object... args)
    {
        return getI18n().getLabel(key, args);
    }
    
    private DocI18n getI18n()
    {
        if (this.docmaI18n == null) {
            this.docmaI18n = mainWin.getDocmaI18();
        }
        return this.docmaI18n;
    }

    public void onEvent(Event evt) throws Exception 
    {
        String ename = evt.getName();
        long now = System.currentTimeMillis();
        if (DocmaConstants.DEBUG) {
            Log.info("GUI_List_products.onEvent(). Event: " + ename + ". Current time: " + now);
        }
        if (ename.equalsIgnoreCase("onTimer") && !skipRefresh) {
            // Ignore multiple refresh requests within one second (normally not required):
            if ((now - lastRefresh) > 1000) {
                Set<String> refreshed_ids = refreshLoadingStates();
                boolean act_exists = refreshActivities(refreshed_ids);
                setListRefresh(act_exists || !refreshed_ids.isEmpty());
                lastRefresh = now;
            }
        }
    }
    
    class ProductModelRenderer implements ListitemRenderer
    {
        public void render(Listitem item, Object data, int index) throws Exception
        {
            if (data instanceof ProductModel) {
                final ProductModel pm = (ProductModel) data;
                boolean disabled = pm.isDisabled();
                // item.setValue(pm.getId());
                Listcell c1 = disabled ? new Listcell(pm.getId(), "img/disabled.gif") : 
                                         new Listcell(pm.getId());
                Listcell c2 = new Listcell();
                c2.setId(productNameCellId(pm.getId()));
                updateNameCell(c2, pm);
                Listcell c3 = new Listcell();
                boolean is_db_ext = pm.isStoreTypeDbExternal();
                String location = is_db_ext ? pm.getDbUrl() : pm.getPath();
                if (location == null) {
                    location = "";
                }
                if (location.equals("") && !is_db_ext) {
                    location = "Default";
                    c3.setStyle("font-style:italic");
                }
                c3.setLabel(location);
                Listcell c4 = new Listcell();
                String state_str = disabled ? i18nLabel("label.productstatus.disabled") :
                                              i18nLabel("label.productstatus.enabled");
                Label state_label = new Label(state_str);
                if (disabled) {
                    state_label.setStyle("font-weight:bold;");
                }
                boolean has_activity = pm.hasActivity();
                boolean is_finished = pm.isActivityFinished();
                if (has_activity) {
                    long act_id = pm.getActivityId();
                    Vbox vb = new Vbox();
                    vb.setHflex("1");
                    vb.setSpacing("0");
                    
                    // First line: product state
                    vb.appendChild(state_label);

                    // If activity is running, show progress in percentage, otherwise show close button
                    Hlayout hlay = new Hlayout();
                    hlay.setHflex("1");
                    // hlay.setValign("center");
                    Vlayout vlay1 = new Vlayout();
                    vlay1.setHflex("min");
                    vlay1.setStyle("padding-right:4px");

                    Toolbarbutton close_btn = new Toolbarbutton();
                    close_btn.setId(actCloseButtonId(act_id));
                    close_btn.setHflex("min");
                    close_btn.setImage("img/activity_close.gif");
                    close_btn.setStyle("min-width:22px;");
                    close_btn.setTooltiptext("Delete log");
                    close_btn.addEventListener("onClick", new EventListener() {
                        public void onEvent(Event t) throws Exception {
                            deleteActivityMessageClick(pm);
                        }
                    } );
                    close_btn.setDisabled(! is_finished);
                    close_btn.setVisible(is_finished);
                    vlay1.appendChild(close_btn);
                    
                    Label percent_label = new Label(pm.getActivityPercent() + "%");
                    percent_label.setId(actPercentLabelId(act_id));
                    percent_label.setVisible(! is_finished);
                    vlay1.appendChild(percent_label);
                    
                    hlay.appendChild(vlay1);
                    
                    // Second and third line: activity title and progress message
                    String act_head = pm.getActivityTitle(getI18n());  // pm.getActivityPercent() + "% " + 
                    Label act_head_label = new Label(act_head);
                    act_head_label.setStyle("font-size:1em;font-style:italic;");
                    Label msg_label = new Label();
                    msg_label.setId(actMessageLabelId(act_id));
                    updateActivityMsg(msg_label, pm);
                    Progressmeter meter = new Progressmeter(pm.getActivityPercent());
                    meter.setId(actProgressMeterId(act_id));
                    meter.setWidth("99%");
                    meter.setHeight("12px");
                    Vlayout vlay2 = new Vlayout();
                    vlay2.setHflex("1");
                    vlay2.appendChild(act_head_label);
                    vlay2.appendChild(meter);
                    vlay2.appendChild(msg_label);
                    
                    // Fourth line: If activity is finished, show "Open log" button, otherwise show "Cancel" button
                    Hlayout btnlay = new Hlayout();
                    
                    Toolbarbutton log_btn = new Toolbarbutton(i18nLabel("label.log.btn"));
                    log_btn.setId(actLogButtonId(act_id));
                    log_btn.setStyle("color:#000080;font-size:1em;font-weight:bold;");
                    log_btn.addEventListener("onClick", new EventListener() {
                        public void onEvent(Event t) throws Exception {
                            showProductActivityLogClick(pm);
                        }
                    } );
                    // log_btn.addForward("onClick", "mainWin", "onShowProductActivityLogClick", "" + pm.getId());
                    log_btn.setDisabled(! is_finished);
                    log_btn.setVisible(is_finished);
                    btnlay.appendChild(log_btn);

                    Toolbarbutton cancel_btn = new Toolbarbutton(i18nLabel("label.cancel.btn"));
                    cancel_btn.setId(actCancelButtonId(act_id));
                    cancel_btn.setStyle("color:#000080;font-size:1em;font-weight:bold;");
                    cancel_btn.setDisabled(pm.isActivityCanceledByUser());
                    cancel_btn.addEventListener("onClick", new EventListener() {
                        public void onEvent(Event evt) throws Exception {
                            cancelProductActivityClick(evt, pm);
                        }
                    } );
                    // cancel_btn.addForward("onClick", "mainWin", "onCancelProductActivityClick", "" + pm.getId());
                    cancel_btn.setDisabled(is_finished);
                    cancel_btn.setVisible(! is_finished);
                    btnlay.appendChild(cancel_btn);

                    vlay2.appendChild(btnlay);
                    hlay.appendChild(vlay2);
                    vb.appendChild(hlay);

                    c4.appendChild(vb);
                } else {
                    c4.appendChild(state_label);
                }
                // if (has_activity && !is_finished) {
                //     item.setStyle("background-color:#FFFF88;");
                // }
                item.appendChild(c1);
                item.appendChild(c2);
                item.appendChild(c3);
                item.appendChild(c4);
                item.addForward("onDoubleClick", "mainWin", "onEditProduct");
            }
        }

        public void cancelProductActivityClick(Event evt, ProductModel pm)
        {
            Component comp = evt.getTarget();
            // Event evt = fe.getOrigin();
            // Object data = evt.getData();
            // if (data == null) {
            //     Messagebox.show("Error: missing parameter!");
            //     return;
            // }
            String productId = pm.getId(); // data.toString();

            DocmaSession docmaSess = mainWin.getDocmaSession();
            Activity act = docmaSess.getDocStoreActivity(productId);
            if ((act != null) && act.isRunning()) {
                if (pm.getActivityId() != act.getActivityId()) {
                    Messagebox.show(i18nLabel("text.product.activity_no_longer_valid", pm.getActivityId()));
                    return;
                }
                act.setCancelFlag(true);
                if (comp instanceof Button) {
                    ((Button) comp).setDisabled(true);
                }
                //    try {
                //        Thread.sleep(100);  // wait for activity thread to finish
                //        int idx = getProductModelIndexById(productId);
                //        ProductModel pm = (ProductModel) products_listmodel.get(idx);
                //        if (pm != null) { 
                //            pm.refresh(docmaSess);
                //            // force refresh:
                //            ProductModel replaced_pm = (ProductModel) products_listmodel.set(idx, pm);  
                //            if ((replaced_pm != null) && !pm.getId().equals(replaced_pm.getId())) {
                //                refresh();  // complete refresh of list
                //            }
                //        }
                //    } catch (Exception ex) {
                //        Log.error("Exception in onCancelProductActivityClick: " + ex.getMessage());
                //    }
            } else {
                Messagebox.show(i18nLabel("text.product.no_activity_exists", productId));
            }
        }

        public void deleteActivityMessageClick(ProductModel pm)
        {
            String productId = pm.getId(); // data.toString();

            DocmaSession docmaSess = mainWin.getDocmaSession();
            Activity act = docmaSess.getDocStoreActivity(productId);
            if ((act != null) && !act.isRunning()) {
                docmaSess.removeDocStoreActivity(productId);
            } else {
                // Should not occur:
                Messagebox.show("Activity for '" + productId + "' does not exist or is still running!");
            }
            refreshActivity(productId);  // update GUI (remove activity log)
        }

        public void showProductActivityLogClick(ProductModel pm)
        {
            // Event evt = fe.getOrigin();
            // Object data = evt.getData();
            // if (data == null) {
            //     Messagebox.show("Error: missing parameter!");
            //     return;
            // }
            String productId = pm.getId();  // data.toString();
            Desktop desk = mainWin.getDesktop();
            String url = desk.getExecution().encodeURL("viewActivityLog.jsp?desk=" + 
                         desk.getId() + "&storeid=" + productId +
                         "&stamp=" + System.currentTimeMillis());
            String client_action = "window.open('" + url +
              "', '_blank', 'width=400,height=400,left=50,top=50" +
              ",resizable=yes,location=no,menubar=yes,scrollbars=yes');";
            Clients.evalJavaScript(client_action);
        }
            
    }
}
