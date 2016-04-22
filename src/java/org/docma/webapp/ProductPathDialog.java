/*
 * ProductPathDialog.java
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
import org.docma.coreapi.fsimplementation.FilesystemStoreProperties;

import java.io.*;
import java.util.*;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.*;

/**
 *
 * @author MP
 */
public class ProductPathDialog extends Window
{
    private static final int MODE_ADD = 0;
    private static final int MODE_EDIT = 1;

    private int modalResult = -1;
    private int mode = -1;

    private String productId;
    private String productPath;

    private MainWindow mainWin;
    private Textbox idbox;
    private Listbox storetypeListbox;
    private Component pathArea;
    private Textbox pathbox;
    private Component externalDbArea;
    private Textbox dbUrlTextbox;
    private Listbox dbDialectListbox;
    private Textbox dbDriverTextbox;
    private Textbox dbUserTextbox;
    private Textbox dbPasswdTextbox;
    

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

    public boolean addExternalProduct(DocmaSession docmaSess) throws Exception
    {
        mode = MODE_ADD;
        setTitle(GUIUtil.i18(this).getLabel("label.productpath.dialog.add.title"));
        init();
        idbox.setDisabled(false);
        storetypeListbox.setDisabled(false);
        showFilesystemGUI();
        return edit(null, docmaSess);
    }

    public boolean editProductPath(String productId, DocmaSession docmaSess) throws Exception
    {
        mode = MODE_EDIT;
        setTitle(GUIUtil.i18(this).getLabel("label.productpath.dialog.edit.title"));
        init();
        idbox.setDisabled(true);
        // String stype = docmaSess.getDocStoreProperty(productId, FilesystemStoreProperties.PROP_STORE_TYPE);
        // if (stype.equals(FilesystemStoreProperties.STORE_TYPE_DB_EXTERNAL)) {...}
        showFilesystemGUI();
        // Editing path is only allowed for filesystem or embedded DB store
        storetypeListbox.setDisabled(true);
        return edit(productId, docmaSess);
    }

    public String getProductId()
    {
        return productId;
    }

    public String getProductPath()
    {
        return productPath;
    }
    
    public void onSelectStorageType()
    {
        if (isFilesystemSelected()) {
            showFilesystemGUI();
        } else {
           showDbExternalGUI();
        }
    }
    
    private boolean isFilesystemSelected()
    {
        Listitem item = storetypeListbox.getSelectedItem();
        if (item != null) {
            Object obj = item.getValue();
            return (obj != null) && (obj.toString().equals("fs"));
        }
        return false;
    }
    
    private void showFilesystemGUI()
    {
        GUIUtil.selectListItem(storetypeListbox, "fs");
        pathArea.setVisible(true);
        externalDbArea.setVisible(false);
        invalidate();
    }

    private void showDbExternalGUI()
    {
        GUIUtil.selectListItem(storetypeListbox, "db_external");
        pathArea.setVisible(false);
        externalDbArea.setVisible(true);
        invalidate();
    }

    private boolean edit(String productId, DocmaSession docmaSess) throws Exception
    {
        updateGUI(productId, docmaSess); // init dialog fields
        do {
            modalResult = -1;
            doModal();
            if (modalResult != GUIConstants.MODAL_OKAY) {
                return false;
            }
            if (hasInvalidInputs(docmaSess)) {
                continue;
            }
            updateModel(docmaSess);
            return true;
        } while (true);
    }

    private void init()
    {
        DocmaWebSession webSess = GUIUtil.getDocmaWebSession(this);
        mainWin = webSess.getMainWindow();
        idbox = (Textbox) getFellow("ExtProductIdTextbox");
        storetypeListbox = (Listbox) getFellow("ExtProductStorageTypeList");
        pathArea = (Component) getFellow("ExtProductPathArea");
        pathbox = (Textbox) getFellow("ExtProductPathTextbox");
        externalDbArea = (Component) getFellow("ProductExternalDbConnectionArea");
        dbUrlTextbox = (Textbox) getFellow("ProductExternalDbUrlTextbox");
        dbDialectListbox = (Listbox) getFellow("ProductExternalDbDialectList");
        dbDriverTextbox = (Textbox) getFellow("ProductExternalDbDriverTextbox");
        dbUserTextbox = (Textbox) getFellow("ProductExternalDbUserTextbox");
        dbPasswdTextbox = (Textbox) getFellow("ProductExternalDbPasswdTextbox");
    }

    private boolean hasInvalidInputs(DocmaSession docmaSess) throws Exception
    {
        init();
        if (mode == MODE_ADD) {
            String id = idbox.getValue().trim();
            if (! id.matches(DocmaConstants.REGEXP_PRODUCT_ID)) {
                Messagebox.show("Error: Product-ID is not valid. Allowed characters are ASCII letters (a-z), digits, underscore and dash.");
                return true;
            }
            List prod_ids = Arrays.asList(docmaSess.listDocStores());
            if (prod_ids.contains(id)) {
                Messagebox.show("Error: A product with this ID already exists.");
                return true;
            }
        }
        if (isFilesystemSelected()) {
            String path = pathbox.getValue().trim();
            if (path.equals("")) {
                Messagebox.show("Error: Please enter a path.");
                return true;
            }
            File f = new File(path);
            if (! f.isAbsolute()) {
                Messagebox.show("Error: Path is relative. Please enter an absolute path.");
                return true;
            }
            if (! f.exists()) {
                Messagebox.show("Error: Path does not exist!");
                return true;
            }
            File propfile = new File(f, FilesystemStoreProperties.STORE_PROP_FILENAME);
            File dbfolder = new File(f, FilesystemStoreProperties.DB_EMBEDDED_FOLDERNAME);
            if (! (propfile.exists() || dbfolder.exists())) {
                Messagebox.show("Error: No store found on the given path!");
                return true;
            }
        } else {
            String dburl = dbUrlTextbox.getValue().trim();
            if (dburl.equals("")) {
                Messagebox.show("Error: Please enter a database URL.");
                return true;
            }
        }
        return false;
    }


    private void updateGUI(String productId, DocmaSession docmaSess)
    {
        init();
        this.productId = productId;   // null if mode == MODE_ADD
        if (mode == MODE_ADD) {
            idbox.setValue("");
            productPath = "";
        } else {
            idbox.setValue(productId);
            productPath = docmaSess.getDocStoreProperty(productId, DocmaConstants.PROP_STORE_PATH);
        }
        pathbox.setValue(productPath);
    }
    

    private void updateModel(DocmaSession docmaSess) throws Exception
    {
        init();
        String pid = idbox.getValue().trim();
        String path = pathbox.getValue().trim();
        if (mode == MODE_ADD) {
            if (isFilesystemSelected()) {
                docmaSess.addDocStore(pid, new File(path));
            } else {
                String dburl = dbUrlTextbox.getValue().trim();
                String driver = dbDriverTextbox.getValue().trim();
                String dialect = getDbDialect();
                String dbusr = dbUserTextbox.getValue().trim();
                String dbpw = dbPasswdTextbox.getValue();
                docmaSess.addExternalDbDocStore(pid, dburl, driver, dialect, dbusr, dbpw);
            }
            productId = pid;   // creation succeeded
        } else {
            if (! path.equals(productPath)) { 
                docmaSess.setDocStoreProperty(pid, DocmaConstants.PROP_STORE_PATH, path);
            }
        }
        productPath = path;  // setting new path succeeded
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

}
