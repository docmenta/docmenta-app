/*
 * GUI_List_AutoFormatConfig.java
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
import org.docma.app.ui.*;
import org.docma.plugin.*;

import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

import org.zkoss.zul.*;
import org.zkoss.zk.ui.event.*;

/**
 *
 * @author MP
 */
public class GUI_List_AutoFormatConfig implements EventListener
{
    private MainWindow mainWin;

    private Listbox        af_listbox;
    private ListModelList  af_listmodel;
    private Label          infoLabel;
    private Cell           infoCell;
    private Html           infoHtml;


    public GUI_List_AutoFormatConfig(MainWindow mainWin)
    {
        this.mainWin = mainWin;
        af_listbox = (Listbox) mainWin.getFellow("AutoFormatConfigListbox");
        infoLabel = (Label) mainWin.getFellow("AutoFormatConfigInfoLabel");
        infoCell = (Cell) mainWin.getFellow("AutoFormatConfigInfoCell");
        infoHtml = (Html) mainWin.getFellow("AutoFormatConfigInfoHtml");
        af_listmodel = new ListModelList();
        af_listmodel.setMultiple(true);
        af_listbox.setModel(af_listmodel);
        af_listbox.setItemRenderer(mainWin.getListitemRenderer());
        af_listbox.addEventListener("onSelect", this);
    }

    public void refresh()
    {
        af_listmodel.clear();
        loadAll();
    }

    public void loadAll()
    {
        if (af_listmodel.size() > 0) return;  // list already loaded; only load once within a user session

        DocmaSession docmaSess = mainWin.getDocmaSession();
        String guiLanguage = mainWin.getGUILanguage();

        String classnames = docmaSess.getApplicationProperty(DocmaConstants.PROP_AUTOFORMAT_CLASSES);
        if (classnames == null) return;
        classnames = classnames.trim();
        if (classnames.equals("")) return;

        String[] arr = classnames.split(" ");
        Arrays.sort(arr);
        for (int i=0; i < arr.length; i++) {
            String clsname = arr[i];
            if (clsname.equals("")) continue;
            af_listmodel.add(createModel(clsname, guiLanguage));
        }

        af_listmodel.clearSelection();
        infoLabel.setVisible(false);
        infoCell.setVisible(false);
    }


    public void onAddAutoFormatClass() throws Exception
    {
        AutoFormatConfigDialog dialog = (AutoFormatConfigDialog) mainWin.getPage().getFellow("AutoFormatConfigDialog");
        String clsname = dialog.edit("");
        if ((clsname == null) || clsname.equals("")) return;

        // Search insert position; check if classname already exists
        int ins_pos = af_listmodel.size();  // default: insert at end of list
        for (int i=0; i < af_listmodel.size(); i++) {
            AutoFormatConfigModel afm = (AutoFormatConfigModel) af_listmodel.get(i);
            int comp = clsname.compareTo(afm.getAutoFormatClassName());
            if (comp == 0) {  // already exists
                Messagebox.show("The Auto-Format class " + clsname + " is already contained in the list!");
                return;
            }
            if (comp < 0) {
                ins_pos = i;
                break;
            }
        }

        // Update application configuration
        DocmaSession docmaSess = mainWin.getDocmaSession();
        String propval = docmaSess.getApplicationProperty(DocmaConstants.PROP_AUTOFORMAT_CLASSES);
        if ((propval == null) || (propval.equals(""))) {
            propval = clsname;
        } else {
            propval += " " + clsname;
        }
        docmaSess.setApplicationProperty(DocmaConstants.PROP_AUTOFORMAT_CLASSES, propval);

        // Update GUI list
        af_listmodel.add(ins_pos, createModel(clsname, mainWin.getGUILanguage()));
        // af_listbox.setSelectedIndex(ins_pos);  // does not work
        // selectionChanged();  
    }


    public void onRemoveAutoFormatClass()
    {
        Set selection = af_listmodel.getSelection();
        int sel_cnt = selection.size();
        if (sel_cnt <= 0) {
            Messagebox.show("Please select one or more entries from the list!");
            return;
        }
        try {
            String msg;
            if (sel_cnt == 1) {
                AutoFormatConfigModel model = (AutoFormatConfigModel) selection.iterator().next();
                msg = "Delete Auto-Format class '" + model.getAutoFormatClassName() + "'?";
            } else {
                msg = "Delete " + sel_cnt + " Auto-Format classes?";
            }
            if (Messagebox.show(msg, "Delete?",
                Messagebox.YES | Messagebox.NO, Messagebox.QUESTION) == Messagebox.YES) {

                // Update GUI list
                selection = new HashSet(selection); // create copy of live selection, because
                                                    // removeAll does not work with 'live' set
                af_listmodel.clearSelection();  // avoid multiple selection changed events
                af_listmodel.removeAll(selection);
                selectionChanged();

                // Update application configuration
                StringBuilder buf = new StringBuilder();
                for (int i=0; i < af_listmodel.size(); i++) {
                    AutoFormatConfigModel model = (AutoFormatConfigModel) af_listmodel.get(i);
                    buf.append(model.getAutoFormatClassName()).append(" ");
                }
                DocmaSession docmaSess = mainWin.getDocmaSession();
                docmaSess.setApplicationProperty(DocmaConstants.PROP_AUTOFORMAT_CLASSES, buf.toString().trim());
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            Messagebox.show("Error: " + ex.getMessage());
            refresh(); // reload all entries
        }
    }

    public AutoFormatConfigModel[] getAutoFormatClasses()
    {
        loadAll();  // load if not already loaded
        // ArrayList list = new ArrayList(af_listmodel.size());
        // for (int i=0; i < af_listmodel.size(); i++) {
        //     AutoFormatConfigModel model = (AutoFormatConfigModel) af_listmodel.get(i);
        //     if (! model.loadingFailed()) list.add(model);
        // }
        return (AutoFormatConfigModel[]) af_listmodel.toArray(new AutoFormatConfigModel[af_listmodel.size()]);
    }

    public void onEvent(Event evt) throws Exception
    {
        if (evt instanceof SelectEvent) {
            selectionChanged();
        }
    }

    private void selectionChanged()
    {
        Set selection = af_listmodel.getSelection();
        if (selection.size() == 1) {
            AutoFormatConfigModel model = (AutoFormatConfigModel) selection.iterator().next();
            infoLabel.setVisible(true);
            infoCell.setVisible(true);
            infoHtml.setContent(model.getLongInfo());
            infoCell.getParent().invalidate();
        } else {
            infoLabel.setVisible(false);
            infoCell.setVisible(false);
            infoHtml.setContent("");
        }
    }

    private AutoFormatConfigModel createModel(String clsname, String guiLanguage)
    {
        String shortInfo = null;
        String longInfo = "";
        boolean failed = false;
        try {
            Class af_class = Class.forName(clsname);
            if (AutoFormat.class.isAssignableFrom(af_class)) {
                AutoFormat af_instance = (AutoFormat) af_class.newInstance();
                shortInfo = af_instance.getShortInfo(guiLanguage);
                longInfo = af_instance.getLongInfo(guiLanguage);
            } else {
                failed = true;
                shortInfo = "Initialization error: Class does not implement the AutoFormat interface!";
            }
        } catch (Exception ex) {
            failed = true;
            shortInfo = "Initialization error: " + ex.getMessage();
        }
        return new AutoFormatConfigModel(clsname, shortInfo, longInfo, failed);
    }
}
