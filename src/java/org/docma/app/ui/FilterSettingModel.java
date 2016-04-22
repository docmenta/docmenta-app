/*
 * FilterSettingModel.java
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

package org.docma.app.ui;

import java.util.*;

/**
 *
 * @author MP
 */
public class FilterSettingModel implements Comparable
{
    private String name;
    private String applics;

    public FilterSettingModel(String name, String applics)
    {
        this.name = name;
        if (applics == null) applics = "";
        this.applics = applics;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApplicsAsString()
    {
        return (applics == null) ? "" : applics;
    }

    public String[] getApplics()
    {
        if ((applics == null) || applics.trim().equals("")) {
            return new String[0];
        } else {
            return applics.split("[ \\t,]+");
        }
    }

    public void setApplics(String applics) {
        this.applics = applics;
    }

    public boolean containsApplic(String applic)
    {
        return Arrays.asList(getApplics()).contains(applic);
    }

    public int compareTo(Object obj)
    {
        FilterSettingModel other = (FilterSettingModel) obj;
        return getName().compareToIgnoreCase(other.getName());
    }

    /* --------------  Old code Removed from MainWindow  --------------- */

//    private Listbox        filtersettings_listbox;
//    private ListModelList  filtersettings_listmodel;
//
//    private void initPublishingTab()
//    {
//        filtersettings_listbox = (Listbox) getFellow("FilterSettingsListbox");
//        filtersettings_listmodel = new ListModelList();
//        filtersettings_listbox.setModel(filtersettings_listmodel);
//        filtersettings_listbox.setItemRenderer(listitem_renderer);
//        loadFilterSettings();
//    }
//
//    private void loadFilterSettings()
//    {
//        DocmaSession docmaSess = getDocmaSession();
//        filtersettings_listmodel.clear();
//        String[] names_arr = docmaSess.getFilterSettingNames();
//        for (int i=0; i < names_arr.length; i++) {
//            String f_name = names_arr[i].trim();
//            String setting = docmaSess.getFilterSetting(f_name);
//            FilterSettingModel fsm = new FilterSettingModel(f_name, setting);
//            filtersettings_listmodel.add(fsm);
//        }
//    }
//
//    public void onNewFilter() throws Exception
//    {
//        DocmaSession docmaSess = getDocmaSession();
//        FilterSettingDialog dialog = (FilterSettingDialog) getPage().getFellow("FilterSettingDialog");
//        FilterSettingModel fsm = new FilterSettingModel("", "");
//        dialog.setMode_NewFilter();
//        if (dialog.doEditFilter(fsm, docmaSess)) {
//            try {
//                docmaSess.createFilterSetting(fsm.getName(), fsm.getApplicsAsString());
//                int ins_pos = -(Collections.binarySearch(filtersettings_listmodel, fsm)) - 1;
//                filtersettings_listmodel.add(ins_pos, fsm);
//            } catch (DocException dex) {
//                Messagebox.show("Error: " + dex.getMessage());
//            }
//        }
//    }
//
//    public void onEditFilter() throws Exception
//    {
//        DocmaSession docmaSess = getDocmaSession();
//        if (filtersettings_listbox.getSelectedCount() <= 0) {
//            Messagebox.show("Please select a filter from the list!");
//            return;
//        }
//        int sel_idx = filtersettings_listbox.getSelectedIndex();
//        FilterSettingModel fsm = (FilterSettingModel) filtersettings_listmodel.getElementAt(sel_idx);
//        FilterSettingDialog dialog = (FilterSettingDialog) getPage().getFellow("FilterSettingDialog");
//        dialog.setMode_EditFilter();
//        if (dialog.doEditFilter(fsm, docmaSess)) {
//            try {
//                docmaSess.changeFilterSetting(fsm.getName(), fsm.getApplicsAsString());
//            } catch (DocException dex) {
//                Messagebox.show("Error: " + dex.getMessage());
//            }
//            filtersettings_listmodel.set(sel_idx, fsm);
//        }
//    }
//
//    public void onDeleteFilter() throws Exception
//    {
//        DocmaSession docmaSess = getDocmaSession();
//        if (filtersettings_listbox.getSelectedCount() <= 0) {
//            Messagebox.show("Please select a filter from the list!");
//            return;
//        }
//        int sel_idx = filtersettings_listbox.getSelectedIndex();
//        FilterSettingModel fsm = (FilterSettingModel) filtersettings_listmodel.getElementAt(sel_idx);
//        try {
//            docmaSess.deleteFilterSetting(fsm.getName());
//            filtersettings_listmodel.remove(sel_idx);
//        } catch (DocException dex) {
//            Messagebox.show("Error: " + dex.getMessage());
//        }
//    }

}
