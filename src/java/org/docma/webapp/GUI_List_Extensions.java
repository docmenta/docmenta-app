/*
 * GUI_List_Extensions.java
 * 
 *  Copyright (C) 2016  Manfred Paula, http://www.docmenta.org
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
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.docma.app.DocmaConstants;
import org.zkoss.zul.Window;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.*;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Listitem;

/**
 *
 * @author MP
 */
public abstract class GUI_List_Extensions implements RowRenderer
{
    protected final Window rootWin;

    protected Listbox       contentEditorList = null;
    protected Grid          extensionsGrid = null;
    
    private ListModelList extensionsModel = null;
    
    private final SortedSet<String> text_extensions = new TreeSet<String>();
    private long loadTimestamp = 0;
    
    public GUI_List_Extensions(Window win, Grid extensions_grid, Listbox content_editor_list) 
    {
        rootWin = win;
        extensionsGrid = extensions_grid;
        contentEditorList = content_editor_list;
        
        if (contentEditorList != null) {
            contentEditorList.addEventListener("onSelect", new EventListener() {
                public void onEvent(Event evt) throws Exception 
                {
                    if (evt instanceof SelectEvent) {
                        Set<Listitem> selitems = ((SelectEvent) evt).getSelectedItems();
                        if (! selitems.isEmpty()) {
                            Listitem item = selitems.iterator().next();
                            String app_id = item.getValue();
                            String app_nm = ((app_id == null) || app_id.equals("")) ? "Default" : item.getLabel();
                            String msg = "Saved: " + app_nm;
                            try {
                                changeAssignment(new String[] { "content" }, app_id);
                            } catch (Exception ex) {
                                msg = "Error: " + ex.getLocalizedMessage();
                                markRefresh();
                                if (DocmaConstants.DEBUG) ex.printStackTrace();
                            }
                            Clients.showNotification(msg, Clients.NOTIFICATION_TYPE_INFO, 
                                                     evt.getTarget(), "end_before", 3500);
                        }
                    }
                }
            } );
        }
        extensionsModel = new ListModelList();
        extensionsModel.setMultiple(false);
        extensionsGrid.setModel(extensionsModel);
        extensionsGrid.setRowRenderer(this);
    }

    /**
     * This method is called every time the tab containing the list/grid is selected.
     */
    public void loadAll()
    {
        long now = System.currentTimeMillis();
        if ((now - loadTimestamp) > (1000 * 60 * 5)) {
            refresh();   // clear all cached data
        }
        if ((contentEditorList != null) && (contentEditorList.getItemCount() == 0)) {  // if not already loaded
            fillContentEditorList();
        }
        if (extensionsModel.isEmpty()) { 
            fillExtensionsModelList();
        }
        loadTimestamp = now;
    }

    
    public void render(Row row, Object data, int index) throws Exception 
    {
        if (! (data instanceof ExtAssignment)) {
            return;  // should never occur
        }
        
        DocmaWebApplication webapp = GUIUtil.getDocmaWebApplication(rootWin);
        if (text_extensions.isEmpty()) {
            text_extensions.addAll(Arrays.asList(webapp.getTextFileExtensions()));
        }

        boolean is_txt = false;
        final ExtAssignment ea = (ExtAssignment) data;
        String[] exts = ea.listExtensions();
        StringBuilder sb = new StringBuilder();
        for (String ext : exts) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(ext);
            
            if (text_extensions.contains(ext)) {
                is_txt = true;
            }
        }
        String mime = ea.getMimeType();
        mime = (mime == null) ? "" : mime.toLowerCase();
        if ((! is_txt) && (mime != null) && mime.startsWith("text/")) {
            is_txt = true;
        }
        row.appendChild(new Label(sb.toString()));
        row.appendChild(new Label(mime));

        Listbox apps_box = new Listbox();
        apps_box.setMold("select");
        apps_box.setRows(1);
        
        String def_app = getDefaultApp(exts);
        if ((def_app == null) && is_txt) {
            // System text-editor (installed default text-editor)
            def_app = webapp.getSystemDefaultTextEditor();
        }
        Listitem def_item;
        if (def_app != null) {
            String def_name = webapp.getHelperAppName(def_app);
            def_item = new Listitem("Default [" + def_name + "]", "");
        } else {
            def_item = new Listitem("", "");
        }
        
        apps_box.appendChild(def_item);

        Listitem sel_item = def_item;
        String assigned_id = ea.getAssignedApplication();
        
        // Add all available editors/viewers to list
        for (String ed_id : getAvailableApps(exts)) {
            String ed_name = webapp.getHelperAppName(ed_id);
            Listitem item = new Listitem(ed_name, ed_id);
            apps_box.appendChild(item);
            if ((assigned_id != null) && assigned_id.equals(ed_id)) {
                sel_item = item;
            }
        }
        apps_box.appendItem("", "");  // just for test; remove later
        // Select the assigned editor. If no editor is assigned, then
        // the default editor will be used.
        sel_item.setSelected(true);
        
        apps_box.addEventListener("onSelect", new EventListener() { 
            public void onEvent(Event evt) throws Exception {
                if (evt instanceof SelectEvent) {
                    Component comp = evt.getTarget();
                    Set<Listitem> selitems = ((SelectEvent) evt).getSelectedItems();
                    if (! selitems.isEmpty()) {
                        Listitem item = selitems.iterator().next();
                        String app_id = item.getValue();
                        String app_nm = ((app_id == null) || app_id.equals("")) ? "Default" : item.getLabel();
                        String msg = "Saved: " + app_nm;
                        try {
                            changeAssignment(ea.listExtensions(), app_id);
                        } catch (Exception ex) {
                            msg = "Error: " + ex.getLocalizedMessage();
                            markRefresh();
                            if (DocmaConstants.DEBUG) ex.printStackTrace();
                        }
                        Clients.showNotification(msg, Clients.NOTIFICATION_TYPE_INFO, 
                                                 comp, "start_before", 3500);
                    }
                }
            }
        });
        row.appendChild(apps_box);
    }

    /* --------------  Package local methods  --------------- */
    
    abstract void changeAssignment(String[] exts, String app_id) throws Exception;
    abstract ExtAssignment[] getExtAssignments();
    abstract String getAssignedContentEditor();
    abstract String getDefaultContentEditor();
    abstract String[] getAvailableApps(String[] exts);
    abstract String getDefaultApp(String[] exts);

    void markRefresh()
    {
        loadTimestamp = 0;
    }

    /* --------------  Private methods  --------------- */

    /**
     * Clear all cached data.
     */
    private void refresh()
    {
        text_extensions.clear();  // re-read from application layer
        if ((contentEditorList != null) && (contentEditorList.getItemCount() > 0)) {  // already loaded
            contentEditorList.getItems().clear();
        }
        if (! extensionsModel.isEmpty()) {   // already loaded
            extensionsModel.clear();
        }
    }

    private void fillExtensionsModelList()
    {
        ExtAssignment[] arr = getExtAssignments();
        if (! extensionsModel.isEmpty()) {
            extensionsModel.clear();
        }
        for (ExtAssignment ea : arr) {
            if (! ea.hasExtension("content")) {  // content apps are shown in contentEditorList
                extensionsModel.add(ea);
            }
        }
    }

    private void fillContentEditorList()
    {
        if (contentEditorList.getItemCount() > 0) {  // already loaded
            contentEditorList.getItems().clear();
        }
        DocmaWebApplication webapp = GUIUtil.getDocmaWebApplication(rootWin);
        // Assigned content-editor
        String assigned_id = getAssignedContentEditor();

        // System content-editor (installed default editor)
        String def_name = "Default";
        String def_id = getDefaultContentEditor();
        if (def_id != null) {
            def_name += " [" + webapp.getHelperAppName(def_id) + "]";
        }

        // Add item to use the system's default editor
        Listitem def_item = new Listitem(def_name, "");
        contentEditorList.appendChild(def_item);

        Listitem sel_item = def_item;
        // Add all available content editors to list
        for (String ed_id : webapp.getContentEditorIds()) {
            String ed_name = webapp.getHelperAppName(ed_id);
            Listitem item = new Listitem(ed_name, ed_id);
            contentEditorList.appendChild(item);
            if ((assigned_id != null) && assigned_id.equals(ed_id)) {
                sel_item = item;
            }
        }
        contentEditorList.appendItem("", "");  // just for test; remove later
        // Select the assigned editor. If no editor is assigned, then
        // the default editor will be used.
        sel_item.setSelected(true);
    }

}
