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

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;
import javax.servlet.http.HttpSession;

import org.docma.app.*;
import org.docma.coreapi.DocI18n;
import org.docma.plugin.web.*;
import org.docma.plugin.Node;
import org.docma.plugin.OutputConfig;
import org.docma.plugin.StoreClosedException;
import org.docma.util.Log;
import org.docma.webapp.*;

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
    private static final Pattern PATTERN_COMPONENT_ID = Pattern.compile("[A-Za-z_][0-9A-Za-z_]{1,99}");
    
    private final DocmaWebSession webSess;
    private final PluginManager pluginMgr;
    // Map plugin-Id to components:
    // private final Map<String, List<Component>> mapPluginToComponents = new HashMap<String, List<Component>>();

    //Map plugin-Id to UIListeners
    private Map<String, UIListener> pluginToUIListener = null;
    // Map main window tab-Id to plugin-Id:
    private Map<String, String> mainTabsToPlugin = null;
    // Map user dialog tab-Id to plugin-Id
    private Map<String, String> userTabsToPlugin = null;
    // Plugin menu entries
    private List<PluginMenuEntry> menuEntries = null;
    
    // ***************** Constructor *********************
    
    public WebUserSessionImpl(DocmaSession docmaSess, DocmaWebSession webSess, PluginManager pluginMgr)
    {
        super(docmaSess);
        this.webSess = webSess;
        this.pluginMgr = pluginMgr;
    }

    // ***************** Interface WebUserSession *********************
    
    public String addDialog(String zulPath)
    {
        MainWindow mainWin = webSess.getMainWindow();
        Execution exec = mainWin.getDesktop().getExecution();
        if (exec == null) {  // method is called outside of ZK event
            throw new RuntimeException("Cannot access ZK Execution instance.");
        }
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
        addMainTab("AdminTabbox", ctx, tabId, title, pos, zulPath);
    }

    public boolean removeAdminTab(String tabId) 
    {
        return removeMainTab("AdminTabbox", tabId);
    }

    public void addPublishingTab(WebPluginContext ctx, String tabId, String title, int pos, String zulPath) 
    {
        addMainTab("PublishingTabbox", ctx, tabId, title, pos, zulPath);
    }

    public boolean removePublishingTab(String tabId) 
    {
        return removeMainTab("PublishingTabbox", tabId);
    }

    public void addUserTab(WebPluginContext ctx, String tabId, String title, int pos, String zulPath) 
    {
        UserDialog usrDialog = (UserDialog) webSess.getMainWindow().getPage().getFellow("UserDialog");
        if (usrDialog == null) {
            return;
        }
        Tabbox tabbox = (Tabbox) usrDialog.getFellow("UserDialogTabbox");
        if (tabbox == null) {
            return;
        }
        addTab(tabbox, tabId, title, pos, zulPath);
        
        if (userTabsToPlugin == null) {
            userTabsToPlugin = new HashMap<String, String>();
        }
        userTabsToPlugin.put(tabId, ctx.getPluginId());
    }

    public boolean removeUserTab(String tabId) 
    {
        UserDialog usrDialog = (UserDialog) webSess.getMainWindow().getPage().getFellow("UserDialog");
        if (usrDialog == null) {
            return false;
        }
        Tabbox tabbox = (Tabbox) usrDialog.getFellow("UserDialogTabbox");
        boolean res = (tabbox != null) && removeTab(tabbox, tabId);
        if (userTabsToPlugin != null) {
            userTabsToPlugin.remove(tabId);
        }
        return res;
    }

    public void addMenuItem(WebPluginContext ctx, String parentMenuId, 
                            String itemId, String title, String iconUrl, 
                            String neighbourId, boolean insertBefore) 
    {
        // MainWindow mainWin = webSess.getMainWindow();
        // Menupopup parentmenu = (Menupopup) mainWin.getFellow(parentMenuId);
        // Component neighbour = mainWin.getFellow(neighbourId);
        PluginMenuEntry entry = new PluginMenuEntry();
        entry.setType(PluginMenuEntry.ITEM);
        entry.setPluginId(ctx.getPluginId());
        entry.setParentMenuId(parentMenuId);
        entry.setEntryId(itemId);
        entry.setNeighbourId(neighbourId);
        entry.setInsertBefore(insertBefore);
        entry.setOption(MenuOption.LABEL, title);
        entry.setOption(MenuOption.IMAGE, iconUrl);
        
        initMenuEntries();
        menuEntries.add(entry);
    }

    public void addMenuSeparator(WebPluginContext ctx, String parentMenuId, 
                                 String separatorId, String neighbourId, boolean insertBefore) 
    {
        PluginMenuEntry entry = new PluginMenuEntry();
        entry.setType(PluginMenuEntry.SEPARATOR);
        entry.setPluginId(ctx.getPluginId());
        entry.setParentMenuId(parentMenuId);
        entry.setEntryId(separatorId);
        entry.setNeighbourId(neighbourId);
        entry.setInsertBefore(insertBefore);
        
        initMenuEntries();
        menuEntries.add(entry);
    }

    public void addSubMenu(WebPluginContext ctx, String parentMenuId, String subMenuId, 
                           String title, String iconUrl, String neighbourId, boolean insertBefore) 
    {
        PluginMenuEntry entry = new PluginMenuEntry();
        entry.setType(PluginMenuEntry.SUB_MENU);
        entry.setPluginId(ctx.getPluginId());
        entry.setParentMenuId(parentMenuId);
        entry.setEntryId(subMenuId);
        entry.setNeighbourId(neighbourId);
        entry.setInsertBefore(insertBefore);
        entry.setOption(MenuOption.LABEL, title);
        entry.setOption(MenuOption.IMAGE, iconUrl);
        
        initMenuEntries();
        menuEntries.add(entry);
    }

    public String getMenuLabel(String menuOrItemId) 
    {
        Object val = getMenuOption(menuOrItemId, MenuOption.LABEL);
        return (val != null) ? val.toString() : "";
    }

    public void setMenuLabel(String menuOrItemId, String label) 
    {
        setMenuOption(menuOrItemId, MenuOption.LABEL, label);
    }

    public String getMenuImage(String itemId) 
    {
        Object val = getMenuOption(itemId, MenuOption.IMAGE);
        return (val != null) ? val.toString() : null;
    }

    public void setMenuImage(String itemId, String imageUrl) 
    {
        setMenuOption(itemId, MenuOption.IMAGE, imageUrl);
    }

    public boolean isMenuDisabled(String itemId) 
    {
        // Note: If option is not set, then item is enabled by default.
        Object val = getMenuOption(itemId, MenuOption.DISABLED);
        return (val != null) ? boolValue(val) : false;
    }

    public void setMenuDisabled(String itemId, boolean disabled) 
    {
        setMenuOption(itemId, MenuOption.DISABLED, disabled);
    }

    public boolean isMenuVisible(String menuOrItemId) 
    {
        // Note: If option is not set, then entry is visible by default.
        Object val = getMenuOption(menuOrItemId, MenuOption.VISIBLE);
        return (val != null) ? boolValue(val) : true;
    }

    public void setMenuVisible(String menuOrItemId, boolean visible) 
    {
        setMenuOption(menuOrItemId, MenuOption.VISIBLE, visible);
    }

    public boolean isMenuCheckbox(String itemId) 
    {
        // Note: If option is not set, then item has no checkbox by default.
        Object val = getMenuOption(itemId, MenuOption.CHECKMARK);
        return (val != null) ? boolValue(val) : false;
    }

    public void setMenuCheckbox(String itemId, boolean checkbox) 
    {
        setMenuOption(itemId, MenuOption.CHECKMARK, checkbox);
    }

    public boolean isMenuChecked(String itemId) 
    {
        // Note: If option is not set, then item is not checked by default.
        Object val = getMenuOption(itemId, MenuOption.CHECKED);
        return (val != null) ? boolValue(val) : false;
    }

    public void setMenuChecked(String itemId, boolean checked) 
    {
        setMenuOption(itemId, MenuOption.CHECKED, checked);
    }

    public void setUIListener(WebPluginContext ctx, UIListener listener) 
    {
        // if (ctx == null) {  // This should never occur. 
        //     return;  // Just ignore to make implementation more robust.
        // }
        if (pluginToUIListener == null) {
            pluginToUIListener = new HashMap<String, UIListener>();
        }
        pluginToUIListener.put(ctx.getPluginId(), listener);
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
        Execution exec = webSess.getMainWindow().getDesktop().getExecution();
        if (exec == null) {  // The session is accessed outside of a ZK event
            // if (DocmaConstants.DEBUG) {
            //     Log.info("Cannot access Execution instance from within WebUserSessionImpl.encodeURL().");
            // }
            return url;
        } else {
            return exec.encodeURL(url);
        }
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
        Messagebox.show(message, DocmaConstants.DISPLAY_APP_SHORTNAME, Messagebox.OK, Messagebox.INFORMATION);
    }

    public void showMessage(String message, String title, MessageType msg_type) 
    {
        if (title == null) {
            title = DocmaConstants.DISPLAY_APP_SHORTNAME;
        }
        Messagebox.show(message, title, Messagebox.OK, messageTypeToIcon(msg_type));
    }

    public void showMessage(String message, String title, MessageType msg_type, final UIListener listener) 
    {
        if (title == null) {
            title = DocmaConstants.DISPLAY_APP_SHORTNAME;
        }
        final WebUserSession userSession = this;
        Messagebox.show(message, title, Messagebox.OK, messageTypeToIcon(msg_type), 
            new EventListener() {
                public void onEvent(Event t) throws Exception {
                    listener.onEvent(new UIEventImpl(t, userSession));
                }
            }
        );
    }

    public void showMessage(String message, String title, MessageType msg_type, ButtonType[] btns, ButtonType focus, final UIListener listener) 
    {
        if (title == null) {
            title = DocmaConstants.DISPLAY_APP_SHORTNAME;
        }
        Messagebox.Button[] zkbtns = buttonTypesToZk(btns);
        if (zkbtns.length == 0) {
            zkbtns = new Messagebox.Button[] { Messagebox.Button.OK };
        }
        Messagebox.Button zkfocus = buttonTypeToZk(focus);
        if (zkfocus == null) {
            zkfocus = zkbtns[0];
        }
        final WebUserSession userSession = this;
        Messagebox.show(message, title, zkbtns, messageTypeToIcon(msg_type), zkfocus, 
            new EventListener() {
                public void onEvent(Event t) throws Exception {
                    listener.onEvent(new UIEventImpl(t, userSession));
                }
            }
        );
    }

    public int selectedNodesCount() 
    {
        return webSess.getMainWindow().getSelectedNodeCount();
    }

    public Node getSingleSelectedNode(boolean showSelectError) 
    {
        StoreConnectionImpl store = (StoreConnectionImpl) getOpenedStore();
        if (store == null) {
            throw new StoreClosedException("User session has no opened store.");
        }
        DocmaNode nd = webSess.getMainWindow().getSelectedDocmaNode();
        if (nd != null) {
            return NodeImpl.createNodeInstance(store, nd);
        } else {
            if (showSelectError) {
                showMessage(getLabel("text.content_tree.select_single"), null, MessageType.ERROR);
            }
            return null;
        }
    }

    public Node[] getSelectedSiblingNodes(boolean showSelectError) 
    {
        StoreConnectionImpl store = (StoreConnectionImpl) getOpenedStore();
        if (store == null) {
            throw new StoreClosedException("User session has no opened store.");
        }
        List<DocmaNode> sel = webSess.getMainWindow().getSelectedDocmaNodes(true, showSelectError);
        if (sel == null) {
            return new Node[0];
        } else {
            Node[] arr = new Node[sel.size()];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = NodeImpl.createNodeInstance(store, sel.get(i));
            }
            return arr;
        }
    }

    // ***************** Other public methods *********************
    
    public void onEvent(Event evt) throws Exception 
    {
        Component target = evt.getTarget();
        String targetId = (target != null) ? target.getId() : null;
        
        // Get the plugin-Id for the component that caused this event.
        String pluginId = null;
        if (target instanceof Tab) {
            pluginId = (mainTabsToPlugin != null) ? mainTabsToPlugin.get(targetId) : null;
            if (pluginId == null) {
                pluginId = (userTabsToPlugin != null) ? userTabsToPlugin.get(targetId) : null;
            }
        }
        
        // Send the event to the registered listener
        if (pluginId != null) {  // If the event has been caused by a plugin.
            PluginControl ctrl = pluginMgr.getControl(pluginId);
            if ((ctrl != null) && ctrl.isLoaded()) {  // Assure that plugin is still enabled
                UIListener listener = (pluginToUIListener != null) ? 
                                      pluginToUIListener.get(pluginId) : null;
                if (listener != null) {
                    try {
                        listener.onEvent(new UIEventImpl(evt, this));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Log.error("Exception in UIListener.onEvent of plugin '" + 
                                  pluginId + "': " + ex.getMessage());
                    }
                }
            } else {
                Log.warning("Event '" + evt.getName() + "' for component '" + targetId +
                            "' that is registered for plugin '" + pluginId + 
                            "' but has no loaded control.");
            }
        } else {
            Log.warning("Event '" + evt.getName() + "' for component '" + targetId + 
                        "' which is not registered for any plugin.");
        }
    }
    
    public synchronized void unloadPluginComponents(String pluginId)
    {
        // Remove main tabs of plugin
        if (mainTabsToPlugin != null) {
            Iterator<String> it = mainTabsToPlugin.keySet().iterator();
            while (it.hasNext()) {
                String componentId = it.next();
                String pId = mainTabsToPlugin.get(componentId);
                if (pluginId.equals(pId)) {
                    try {
                        Tab tab = (Tab) webSess.getMainWindow().getFellow(componentId);
                        removeTab(tab.getTabbox(), componentId);
                        it.remove();
                    } catch (Exception ex) {
                        Log.error("Failed to unload main-window tab '" + componentId + 
                                  "':" + ex.getMessage());
                    }
                }
            }
        }
        
        // Remove user-dialog tabs of plugin
        if (userTabsToPlugin != null) {
            UserDialog usrDialog = (UserDialog) webSess.getMainWindow().getPage().getFellow("UserDialog");
            Iterator<String> it = userTabsToPlugin.keySet().iterator();
            while (it.hasNext()) {
                String componentId = it.next();
                String pId = userTabsToPlugin.get(componentId);
                if (pluginId.equals(pId)) {
                    try {
                        if (usrDialog != null) {
                            Tab tab = (Tab) usrDialog.getFellow(componentId);
                            removeTab(tab.getTabbox(), componentId);
                        }
                        it.remove();
                    } catch (Exception ex) {
                        Log.error("Failed to unload user-dialog tab '" + componentId + 
                                  "':" + ex.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Returns the list of plug-in menu entries for this session or null
     * if no plug-in entries exist.
     * This method can be called for example from org.docma.webapp.MenuUtil to 
     * retrieve the list of plug-in menu entries for the current session.
     * 
     * @return List of plugin menu entries or null.
     */
    public List<PluginMenuEntry> getMenuEntries()
    {
        return menuEntries;
    }

    public void sendMenuClickEventToPlugin(Menuitem item)
    {
        PluginMenuEntry entry = getMenuEntryById(item.getId());
        if ((entry != null) && (pluginToUIListener != null)) {
            String pluginId = entry.getPluginId();
            UIListener listener = pluginToUIListener.get(pluginId);
            if (listener != null) {
                try {
                    listener.onEvent(new UIEventImpl("onClick", item, this));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Log.error("Exception in UIListener.onEvent for 'onClick' event of plugin '" + 
                              pluginId + "': " + ex.getMessage());
                }
            }
        }
    }
    
    public void sendMenuOpenEventToPlugins(Menupopup menu)
    {
        if (pluginToUIListener == null) {   // no plug-in exists
            return;
        }
        for (String pluginId : pluginToUIListener.keySet()) {
            UIListener listener = pluginToUIListener.get(pluginId);
            if (listener != null) {
                try {
                    listener.onEvent(new UIEventImpl("onOpen", menu, this));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Log.error("Exception in UIListener.onEvent for 'onOpen' event of plugin '" + 
                              pluginId + "': " + ex.getMessage());
                }
            }
        }
    }
    
    // ***************** Package local methods *********************

    // ***************** Private methods *********************

    private Object getMenuOption(String menuOrItemId, MenuOption name)
    {
        PluginMenuEntry entry = getMenuEntryById(menuOrItemId);
        return (entry != null) ? entry.getOption(name) : null;
    }

    private void setMenuOption(String menuOrItemId, MenuOption name, Object value) 
    {
        PluginMenuEntry entry = getMenuEntryById(menuOrItemId);
        if (entry != null) {
            entry.setOption(name, value);
        }
    }

    private void initMenuEntries()
    {
        if (menuEntries == null) {
            menuEntries = new ArrayList<PluginMenuEntry>();
        }
    }
    
    private PluginMenuEntry getMenuEntryById(String id)
    {
        if (menuEntries == null) {
            return null;
        }
        for (PluginMenuEntry entry : menuEntries) {
            if (id.equals(entry.getEntryId())) {
                return entry;
            }
        }
        return null;
    }

    private void addMainTab(String tabboxId, WebPluginContext ctx, String tabId, String title, int pos, String zulPath)
    {
        Tabbox tabbox = (Tabbox) webSess.getMainWindow().getFellow(tabboxId);
        if (tabbox == null) {
            return;
        }
        addTab(tabbox, tabId, title, pos, zulPath);
        
        if (mainTabsToPlugin == null) {
            mainTabsToPlugin = new HashMap<String, String>();
        }
        mainTabsToPlugin.put(tabId, ctx.getPluginId());
    }

    private boolean removeMainTab(String tabboxId, String tabId) 
    {
        Tabbox tabbox = (Tabbox) webSess.getMainWindow().getFellow(tabboxId);
        boolean res = (tabbox != null) && removeTab(tabbox, tabId);
        if (mainTabsToPlugin != null) {
            mainTabsToPlugin.remove(tabId);
        }
        return res;
    }
    
    private void addTab(Tabbox tabbox, String tabId, String title, int pos, String zulPath) 
    {
        Tabs tabs = tabbox.getTabs();
        checkValidComponentId(tabId);
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
            if (exec == null) {  // method is called outside of ZK event
                throw new RuntimeException("Cannot access ZK Execution instance.");
            }
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
                return true;
            }
        }
        Log.warning("Could not remove tab with id '" + tabId + "'!");
        return false;
    }
    
    private String messageTypeToIcon(MessageType msg_type)
    {
        if (msg_type == null) {
            return Messagebox.NONE;
        }
        
        String icon;
        if (MessageType.ERROR.equals(msg_type)) {
            icon = Messagebox.ERROR;
        } else if (MessageType.INFO.equals(msg_type)) {
            icon = Messagebox.INFORMATION;
        } else if (MessageType.QUESTION.equals(msg_type)) {
            icon = Messagebox.QUESTION;
        } else if (MessageType.WARNING.equals(msg_type)) {
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

    private void checkValidComponentId(String id)
    {
        if (! PATTERN_COMPONENT_ID.matcher(id).matches()) {
            throw new RuntimeException("Invalid component ID!");
        }
    }
    
    private void checkValidEventName(String name)
    {
        if (! PATTERN_COMPONENT_ID.matcher(name).matches()) {
            throw new RuntimeException("Invalid event name!");
        }
    }
    
    private static boolean boolValue(Object value)
    {
        return (value instanceof Boolean) ? (Boolean) value 
                                          : "true".equalsIgnoreCase(value.toString());
    }

}
