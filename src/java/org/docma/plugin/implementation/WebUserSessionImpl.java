/*
 * WebUserSessionImpl.java
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
package org.docma.plugin.implementation;

import org.docma.app.*;
import org.docma.webapp.*;
import org.docma.util.Log;
import org.docma.plugin.web.*;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import javax.servlet.http.HttpSession;
import org.docma.plugin.OutputConfig;

import org.zkoss.zul.*;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.event.*;
import org.zkoss.zk.ui.util.Clients;

/**
 *
 * @author MP
 */
public class WebUserSessionImpl extends UserSessionImpl implements WebUserSession, EventListener
{
    private final DocmaWebSession webSess;
    private final PluginManager pluginMgr;
    // Map plugin-Id to components:
    private final Map<String, List<Component>> mapPluginToComponents = new HashMap<String, List<Component>>();
    // Map tab-Id to plugin-Id:
    private final Map<String, String> mapTabToPlugin = new HashMap<String, String>();
    
    
    public WebUserSessionImpl(DocmaSession docmaSess, DocmaWebSession webSess, PluginManager pluginMgr)
    {
        super(docmaSess);
        this.webSess = webSess;
        this.pluginMgr = pluginMgr;
    }

    // ***************** Interface WebUserSession *********************
    
    public String getLabel(String key) 
    {
        return webSess.getDocmaWebApplication().i18().getLabel(key);
    }

    public String addDialog(String zulPath)
    {
        MainWindow mainWin = webSess.getMainWindow();
        Execution exec = mainWin.getDesktop().getExecution();
        Component[] comps = exec.createComponents(zulPath, null);
        Window dialogWin = null;
        for (Component comp : comps) {
            if (comp instanceof Window) {
                dialogWin = (Window) comp;
                break;
            }
        }
        if (dialogWin == null) {
            throw new RuntimeException("Invalid component type. Root component has to be of type Window: " + zulPath);
        }
        String dialogId = dialogWin.getId();
        if (dialogId == null) {
            dialogId = "dialog" + System.currentTimeMillis();
            dialogWin.setId(dialogId);
        }
        dialogWin.setPage(mainWin.getPage());
        return dialogId;
    }
    
    public Object getDialog(String dialogId)
    {
        MainWindow mainWin = webSess.getMainWindow();
        Window dialogWin = (Window) mainWin.getPage().getFellow(dialogId);
        return dialogWin;
    }
    
    public boolean removeDialog(String dialogId)
    {
        MainWindow mainWin = webSess.getMainWindow();
        Window dialogWin = (Window) mainWin.getPage().getFellow(dialogId);
        if (dialogWin != null) {
            dialogWin.detach();
            return true;
        } else {
            return false;
        }
    }

    public void addAdminTab(WebPluginContext ctx, String tabId, String title, int pos, String zulPath) 
    {
        Tabbox tabbox = (Tabbox) webSess.getMainWindow().getFellow("AdminTabbox");
        addTab(tabbox, ctx, tabId, title, pos, zulPath);
    }

    public boolean removeAdminTab(String tabId) 
    {
        Tabbox tabbox = (Tabbox) webSess.getMainWindow().getFellow("AdminTabbox");
        return removeTab(tabbox, tabId);
    }

    public void addPublishingTab(WebPluginContext ctx, String tabId, String title, int pos, String zulPath) 
    {
        Tabbox tabbox = (Tabbox) webSess.getMainWindow().getFellow("PublishingTabbox");
        addTab(tabbox, ctx, tabId, title, pos, zulPath);
    }

    public boolean removePublishingTab(String tabId) 
    {
        Tabbox tabbox = (Tabbox) webSess.getMainWindow().getFellow("PublishingTabbox");
        return removeTab(tabbox, tabId);
    }

    public void addUserTab(WebPluginContext ctx, String tabId, String title, int pos, String zulPath) 
    {
        UserDialog usrDialog = (UserDialog) webSess.getMainWindow().getPage().getFellow("UserDialog");
        Tabbox tabbox = (Tabbox) usrDialog.getFellow("UserDialogTabbox");
        addTab(tabbox, ctx, tabId, title, pos, zulPath);
    }

    public boolean removeUserTab(String tabId) 
    {
        UserDialog usrDialog = (UserDialog) webSess.getMainWindow().getPage().getFellow("UserDialog");
        Tabbox tabbox = (Tabbox) usrDialog.getFellow("UserDialogTabbox");
        return removeTab(tabbox, tabId);
    }
    
    public String getThemeProperty(String propName)
    {
        return webSess.getThemeProperty(propName);
    }

    public OutputConfig getPreviewHTMLConfig() 
    {
        return new OutputConfigImpl(webSess.getMainWindow().getPreviewHTMLConfig());
    }
    
    public ContentAppHandler getContentAppHandler(String application_id) 
    {
        return webSess.getDocmaWebApplication().getContentAppHandler(application_id);
    }
    
    public String encodeURL(String url) 
    {
        return webSess.getMainWindow().getDesktop().getExecution().encodeURL(url);
    }
    
    public HttpSession getHttpSession()
    {
        return webSess.getHttpSession();
    }

    public void evalJavaScript(String javascript) 
    {
        Clients.evalJavaScript(javascript);
    }

    public void showMessage(String message) 
    {
        Messagebox.show(message);
    }

    public void showMessage(String message, String title, String msg_type) 
    {
        Messagebox.show(message, title, 0, messageTypeToIcon(msg_type));
    }

    public void showMessage(String message, String title, String msg_type, final UIListener listener) 
    {
        Messagebox.show(message, title, 0, messageTypeToIcon(msg_type), 
            new EventListener() {
                public void onEvent(Event t) throws Exception {
                    listener.onEvent(new UIEventImpl(t));
                }
            }
        );
    }

    public void showMessage(String message, String title, String msg_type, ButtonType[] btns, ButtonType focus, final UIListener listener) 
    {
        Messagebox.Button[] zkbtns = buttonTypesToZk(btns);
        if (zkbtns.length == 0) {
            zkbtns = new Messagebox.Button[] { Messagebox.Button.OK };
        }
        Messagebox.Button zkfocus = buttonTypeToZk(focus);
        if (zkfocus == null) {
            zkfocus = zkbtns[0];
        }
        Messagebox.show(message, title, zkbtns, messageTypeToIcon(msg_type), zkfocus, 
            new EventListener() {
                public void onEvent(Event t) throws Exception {
                    listener.onEvent(new UIEventImpl(t));
                }
            }
        );
    }


    // ***************** Other public methods *********************
    
    public void onEvent(Event evt) throws Exception 
    {
        Component target = evt.getTarget();
        if ("onSelect".equalsIgnoreCase(evt.getName()) && (target instanceof Tab)) {
            String tabId = target.getId();
            String pluginId = mapTabToPlugin.get(tabId);
            if (pluginId != null) {
                PluginControl ctrl = pluginMgr.getControl(pluginId);
                if (ctrl != null) {
                    ctrl.sendOnSelectTab(this, tabId);
                } else {
                    Log.error("Tab onSelect event for plugin '" + pluginId + "' which has no control.");
                }
            } else {
                Log.warning("onSelect event for tab '" + tabId + "' which is not registered for any plugin.");
            }
        }
    }
    
    public synchronized void unloadPluginComponents(String pluginId)
    {
        List<Component> comp_list = mapPluginToComponents.get(pluginId);
        if (comp_list != null) {
            for (Component comp : comp_list) {
                if (comp.getParent() != null) {
                    comp.setParent(null);
                }
            }
            comp_list.clear();
        }
    }

    // ***************** Private methods *********************

    private void addTab(Tabbox tabbox, WebPluginContext ctx, String tabId, String title, int pos, String zulPath) 
    {
        Tabs tabs = tabbox.getTabs();
        if (tabs.hasFellow(tabId)) {
            throw new RuntimeException("Cannot add tab: component with id '" + tabId + "' already exists!");
        }
        Tabpanels panels = tabbox.getTabpanels();
        
        Tab tab = new Tab(title);
        tab.setId(tabId);
        tab.addEventListener("onSelect", this);
        
        Tabpanel tp = new Tabpanel();
        tp.setWidth("100%");
        tp.setHeight("100%");
        // tp.appendChild(new Label("Hello world!"));
        if ((zulPath != null) && (zulPath.length() > 0)) {
            Execution exec = webSess.getMainWindow().getDesktop().getExecution();
            Component[] comps = exec.createComponents(zulPath, null);
            for (Component com : comps) {
                tp.appendChild(com);
            }
        }
        List tab_list = tabs.getChildren();
        List panel_list = panels.getChildren();
        int maxpos = Math.min(tab_list.size(), panel_list.size());
        if ((pos < 0) || (pos > maxpos)) {
            pos = maxpos;
        }
        
        tab_list.add(pos, tab);
        panel_list.add(pos, tp);
        addPluginComponentToMap(ctx, tab);
        addPluginComponentToMap(ctx, tp);
    }

    private boolean removeTab(Tabbox tabbox, String tabId) 
    {
        Tabs tabs = tabbox.getTabs();
        Tabpanels panels = tabbox.getTabpanels();
        List tab_list = tabs.getChildren();
        List panel_list = panels.getChildren();
        int maxpos = Math.min(tab_list.size(), panel_list.size());
        for (int i=0; i < maxpos; i++) {
            Component tab = (Component) tab_list.get(i);
            Component pan = (Component) panel_list.get(i);
            if (tabId.equals(tab.getId())) {
                tabs.removeChild(tab);
                panels.removeChild(pan);
                removePluginComponentsFromMap(tab, pan);
                return true;
            }
        }
        Log.warning("Could not remove tab with id '" + tabId + "'!");
        return false;
    }

    private void addPluginComponentToMap(WebPluginContext ctx, Component comp)
    {
        addPluginComponentToMap(ctx.getPluginId(), comp);
    }
    
    private void addPluginComponentToMap(String pluginId, Component comp)
    {
        List<Component> comp_list = mapPluginToComponents.get(pluginId);
        if (comp_list == null) {
            comp_list = new ArrayList<Component>();
            mapPluginToComponents.put(pluginId, comp_list);
        }
        comp_list.add(comp);
        if (comp instanceof Tab) {
            mapTabToPlugin.put(comp.getId(), pluginId);
        }
    }
    
    private void removePluginComponentFromMap(Component comp1)
    {
        removePluginComponentsFromMap(comp1, null);
    }
    
    private void removePluginComponentsFromMap(Component comp1, Component comp2)
    {
        Iterator<List<Component>> it = mapPluginToComponents.values().iterator();
        while (it.hasNext()) {
            List<Component> clist = it.next();
            if (clist != null) {
                clist.remove(comp1);
                if (comp2 != null) { 
                    clist.remove(comp2);
                }
            }
        }
        if (comp1 instanceof Tab) {
            mapTabToPlugin.remove(comp1.getId());
        }
        if (comp2 instanceof Tab) {
            mapTabToPlugin.remove(comp2.getId());
        }
    }

    private String messageTypeToIcon(String msg_type)
    {
        if (msg_type == null) {
            return Messagebox.NONE;
        }
        
        String icon;
        if (WebUserSession.MSG_ERROR.equals(msg_type)) {
            icon = Messagebox.ERROR;
        } else if (WebUserSession.MSG_INFO.equals(msg_type)) {
            icon = Messagebox.INFORMATION;
        } else if (WebUserSession.MSG_QUESTION.equals(msg_type)) {
            icon = Messagebox.QUESTION;
        } else if (WebUserSession.MSG_WARNING.equals(msg_type)) {
            icon = Messagebox.EXCLAMATION;
        } else {
            icon = Messagebox.NONE;
        }
        return icon;
    }
    
    private Messagebox.Button[] buttonTypesToZk(ButtonType[] btnTypes)
    {
        if (btnTypes == null) {
            return null;
        }
        
        List<Messagebox.Button> res = new ArrayList<Messagebox.Button>();
        for (ButtonType bt : btnTypes) {
            Messagebox.Button zkBtn = buttonTypeToZk(bt);
            if (zkBtn != null) {
                res.add(zkBtn);
            }
        }
        return res.toArray(new Messagebox.Button[res.size()]);
    }
    
    private Messagebox.Button buttonTypeToZk(ButtonType btnType)
    {
        if (btnType == null) {
            return null;
        }
        if (btnType.equals(ButtonType.ABORT)) {
            return Messagebox.Button.ABORT;
        }
        if (btnType.equals(ButtonType.CANCEL)) {
            return Messagebox.Button.CANCEL;
        }
        if (btnType.equals(ButtonType.CLOSE)) {
            return Messagebox.Button.ABORT;  // or map to null?
        }
        if (btnType.equals(ButtonType.IGNORE)) {
            return Messagebox.Button.IGNORE;
        }
        if (btnType.equals(ButtonType.NO)) {
            return Messagebox.Button.NO;
        }
        if (btnType.equals(ButtonType.OK)) {
            return Messagebox.Button.OK;
        }
        if (btnType.equals(ButtonType.RETRY)) {
            return Messagebox.Button.RETRY;
        }
        if (btnType.equals(ButtonType.YES)) {
            return Messagebox.Button.YES;
        }
        
        return null;
    }

}
