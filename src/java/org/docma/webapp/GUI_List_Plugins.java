/*
 * GUI_List_Plugins.java
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
import java.io.*;
import org.zkoss.zul.*;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.Clients;

import org.docma.app.*;
import org.docma.plugin.implementation.*;

/**
 *
 * @author MP
 */
public class GUI_List_Plugins implements ListitemRenderer
{
    private MainWindow mainWin;
    // private String guiLanguage = null;

    private Listbox        plugins_listbox;
    private ListModelList  plugins_listmodel;

    public GUI_List_Plugins(MainWindow mainWin)
    {
        this.mainWin = mainWin;
        plugins_listbox = (Listbox) mainWin.getFellow("PluginsConfigListbox");
        plugins_listmodel = new ListModelList();
        plugins_listmodel.setMultiple(true);
        plugins_listbox.setModel(plugins_listmodel);
        plugins_listbox.setItemRenderer(this);
    }

    public void refresh()
    {
        plugins_listmodel.clear();
        loadAll();
    }

    public void loadAll()
    {
        if (plugins_listmodel.size() > 0) return;  // list already loaded; only load once within a user session
        
        DocmaWebApplication webapp = GUIUtil.getDocmaWebApplication(mainWin);
        PluginManager pm = webapp.getPluginManager();
        if (pm == null) return;
        plugins_listmodel.addAll(Arrays.asList(pm.listControls()));
    }

    public void installPlugin()
    {
        DocmaWebApplication webapp = GUIUtil.getDocmaWebApplication(mainWin);
        PluginManager pm = webapp.getPluginManager();
        
        if (pm.hasUninstallOnServerRestart()) {
            Messagebox.show("You have to restart the web-server to finish the uninstall \n" + 
                            "of an existing plugin before new plugins can be installed!");
            return;
        }
        
        String message = label("label.plugin.upload.message") + ": ";
        String title = label("label.plugin.upload.title");
        Media media = Fileupload.get(message, title, true);
        if (media != null) {
            try {
                InputStream pin = media.getStreamData();
                File plug_file = pm.writePluginStreamToTempFile(pin);
                Map<String, String> license_map = new HashMap<String, String>(); 
                Properties pluginProps = pm.checkPluginFile(plug_file, license_map, null, null, null, null);
                
                // Check if plugin requires a newer application version
                try {
                    String ver_str = pluginProps.getProperty(PluginManager.PLUGIN_PROP_REQUIRED_APP_VERSION, "").trim();
                    if (ver_str.length() > 0) {
                        ApplicationVersionId req_ver = new ApplicationVersionId(ver_str);
                        ApplicationVersionId app_ver = new ApplicationVersionId(DocmaConstants.DISPLAY_APP_VERSION);
                        if (app_ver.isLowerThan(req_ver)) {
                            Messagebox.show("Cannot install plugin. The plugin requires application version " + 
                                            ver_str + " or higher.");
                            return;  // abort installation
                        }
                    }
                } catch (Exception ex) {
                    Messagebox.show("Warning: Failed to check required version. \nRoot cause: " + ex.getMessage());
                }
                
                // Request acceptance of license agreement (if provided) 
                String show_lic = pluginProps.getProperty(PluginManager.PLUGIN_PROP_SHOW_LICENSE, "").trim();
                if (show_lic.equalsIgnoreCase("true") && !license_map.isEmpty()) {  // show license and license exists
                    String gui_lang = mainWin.getGUILanguage().toLowerCase();
                    String license_html = license_map.get(gui_lang);
                    if (license_html == null) {  // license of user's GUI language does not exist
                        license_html = license_map.get("en");  // fallback to English license
                        if (license_html == null) {  // if no English license either, use first license found
                            license_html = license_map.values().iterator().next();
                        }
                    }
                    LicenseDialog licDialog = (LicenseDialog) mainWin.getPage().getFellow("LicenseDialog");
                    boolean accepted = licDialog.requestLicenseAcceptance(license_html);
                    if (! accepted) {
                        Messagebox.show("Installation aborted!");
                        return;  // abort installation
                    }
                }

                // Check if the same plugin is already installed; Replace on user request
                String pluginId = pluginProps.getProperty(PluginManager.PLUGIN_PROP_ID).trim();
                if (pm.isInstalled(pluginId)) {
                    String msg = "A plugin with ID '" + pluginId + "' is already installed. Replace existing plugin?";
                    if (Messagebox.show(msg, "Replace?",
                                        Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION) == Messagebox.OK) {
                        if (checkConnectedUsers()) {
                            PluginControl ctrl = pm.getControl(pluginId);
                            final boolean REINSTALL_MODE = true;    // keep files for reinstall
                            boolean completed = pm.uninstall(pluginId, REINSTALL_MODE); // uninstall plugin
                            removePluginFromWebGUI(pluginId); // remove plugin from web GUI of current user
                            // Note: if sessions of other users exist, then
                            // for them the plugin is still visible in the GUI,
                            // but may no longer work properly.
                            if ((! completed) && ctrl.isUninstallOnStartUp()) {
                                String restart_msg = "You have to restart the web-server to finish the uninstall procedure. \n" +
                                                     "After uninstall is done, proceed with installing the new version of the plugin!";
                                Messagebox.show(restart_msg);
                                refresh();
                                return;
                            }
                        } else {
                            return;  // uninstall canceled because users are still connected
                        }
                    } else {
                        return; // uninstall canceled by user
                    }
                }
                
                // Install plugin; install() throws an exception if installation fails
                pm.install(plug_file, 
                           pluginProps, 
                           false,  // complete install, restore web = false
                           true,   // overwrite shared web files
                           true,   // overwrite global web files
                           null, null, null, null);   // to do (show file conflicts as warning)
                PluginControl ctrl = pm.getControl(pluginId);
                if (ctrl != null) {  // should not be null if installation was ok!
                    ctrl.setLoadOnStartUp(true);  // set flag to load plugin on every server start-up
                    if (ctrl.isLoadTypeImmediate()) {
                        ctrl.load();  // load plugin
                        if (ctrl.isLoaded()) {  // if loading was okay
                            addPluginToWebGUI(ctrl);  // immediately add plugin to web GUI of current user
                            // Note: Other users do not see the GUI components added
                            // by the plugin until they start a new session.
                        }
                    } else {
                        Messagebox.show("Installation finished. You have to restart \n" + 
                                        "the web-server for the plugin to be loaded!");
                    }
                }
            } catch (Exception ex) {
                Messagebox.show("Error: " + ex.getMessage());
            }
            refresh();  // refresh plugin list to show installed plugin
        }
    }

    public void uninstallPlugin()
    {
        DocmaWebApplication webapp = GUIUtil.getDocmaWebApplication(mainWin);
        PluginManager pm = webapp.getPluginManager();
        
        Set selection = plugins_listmodel.getSelection();
        int sel_cnt = selection.size();
        if (sel_cnt <= 0) {
            Messagebox.show("Please select one or more plugins from the list!");
            return;
        }
        if (! checkConnectedUsers()) {
            return;
        }
        String msg;
        Iterator it = selection.iterator();
        PluginControl ctrl = (PluginControl) it.next();
        if (sel_cnt == 1) {
            msg = "Uninstall plugin '" + ctrl.getId() + "'?";
        } else {
            msg = "Uninstall " + sel_cnt + " plugins?";
        }
        if (Messagebox.show(msg, "Uninstall?",
            Messagebox.YES | Messagebox.NO, Messagebox.QUESTION) == Messagebox.YES) {
            String restart_msg = null;
            while (true) {
                try {
                    if (ctrl.isLoaded()) {
                        ctrl.unload();
                        removePluginFromWebGUI(ctrl.getId()); // remove plugin from web GUI of current user
                        // Note: if sessions of other users exist, then
                        // for them the plugin is still visible in the GUI,
                        // but does no longer work properly.
                    }
                    boolean completed = pm.uninstall(ctrl.getId(), false);  // argument false means complete uninstall
                    // Note: For some reasons (e.g. files are still in use), the
                    // uninstall may be deferred to the next start-up.
                    // In this case ctrl.isUninstallOnStartUp() returns true. 
                    if ((! completed) && ctrl.isUninstallOnStartUp()) {
                        restart_msg = "You have to restart the web-server \n" +
                                      "to finish the uninstall procedure!";
                    }
                } catch(Exception ex) {
                    // ex.printStackTrace();
                    Messagebox.show("Plugin uninstall error: " + ex.getMessage());
                }
                if (it.hasNext()) {
                    ctrl = (PluginControl) it.next();
                } else {
                    break;
                }
            }
            if (restart_msg != null) {
                Messagebox.show(restart_msg);
            }
            refresh(); // refresh plugin list
        }
    }

    public void enablePlugin()
    {
        Set selection = plugins_listmodel.getSelection();
        int sel_cnt = selection.size();
        if (sel_cnt <= 0) {
            Messagebox.show("Please select one or more plugins from the list!");
            return;
        }
        DocmaWebApplication webapp = GUIUtil.getDocmaWebApplication(mainWin);
        PluginManager pm = webapp.getPluginManager();

        String restart_msg = null;
        Iterator it = selection.iterator();
        PluginControl ctrl = (PluginControl) it.next();
        while (true) {
            try {
                if (! ctrl.isLoadOnStartUp()) {
                    ctrl.setLoadOnStartUp(true);  // set flag to load plugin on every server start-up
                }
                
                // Immediately reinstall web-files, even if plugin has load type
                // "next_startup". This is required, because the jar files of
                // the plugin have to be in the lib folder when the web-server 
                // is started up.
                if (ctrl.isRemoveWebFilesOnUnload()) {
                    pm.installWebFiles(ctrl);
                }
                
                boolean was_not_loaded = !ctrl.isLoaded();
                if (was_not_loaded) {
                    if (ctrl.isLoadTypeImmediate()) {
                        ctrl.load();
                    } else {
                        restart_msg = "You have to restart the web-server \n" +
                                      "for the plugin to be loaded!";
                    }
                }
                if (was_not_loaded && ctrl.isLoaded()) {
                    addPluginToWebGUI(ctrl);  // immediately add plugin to web GUI of current user
                    // Note: Other users do not see the GUI components, which are added
                    // by the plugin, until they start a new session.
                }
            } catch(Exception ex) {
                // ex.printStackTrace();
                Messagebox.show("Error: " + ex.getMessage());
            }
            if (it.hasNext()) {
                ctrl = (PluginControl) it.next();
            } else {
                break;
            }
        }
        if (restart_msg != null) {
            Messagebox.show(restart_msg);
        }
        refresh(); // refresh plugin list
    }

    public void disablePlugin()
    {
        Set selection = plugins_listmodel.getSelection();
        int sel_cnt = selection.size();
        if (sel_cnt <= 0) {
            Messagebox.show("Please select one or more plugins from the list!");
            return;
        }
        if (! checkConnectedUsers()) {
            return;
        }
        Iterator it = selection.iterator();
        PluginControl ctrl = (PluginControl) it.next();
        String msg;
        if (sel_cnt == 1) {
            if (! ctrl.isDisableSupported()) {
                Messagebox.show("Disable is not supported for the plugin '" + ctrl.getId() + "'.");
                return;
            }
            msg = "Disable plugin '" + ctrl.getId() + "'?";
        } else {
            msg = "Disable " + sel_cnt + " plugins?";
        }
        if (Messagebox.show(msg, "Disable?",
            Messagebox.YES | Messagebox.NO, Messagebox.QUESTION) == Messagebox.YES) {
            String restart_msg = null;
            while (true) {
                try {
                    if (! ctrl.isDisableSupported()) {
                        continue;
                    }
                    if (ctrl.isLoadOnStartUp()) {
                        ctrl.setLoadOnStartUp(false);  // clear flag (do not load plugin on server start-up)
                    }
                    boolean was_loaded = ctrl.isLoaded();
                    ctrl.unload();  // this also tries to deletes the web-files if remove_webfiles is true
                    if (ctrl.isRemoveWebFilesOnUnload() && ctrl.hasError()) {
                        // Maybe unload gave errors because files could not be deleted.
                        // If files could not be deleted because they were still in use,
                        // restart resolves this problem.
                        restart_msg = "You have to restart the web-server \n" + 
                                      "to completely disable the plugin!";
                    }
                    if (was_loaded && !ctrl.isLoaded()) {
                        removePluginFromWebGUI(ctrl.getId()); // remove plugin from web GUI of current user
                        // Note: if sessions of other users exist, then
                        // for them the plugin is still visible in the GUI,
                        // but may no longer work properly.
                    }
                } catch(Exception ex) {
                    ex.printStackTrace();
                    Messagebox.show("Error: " + ex.getMessage());
                }
                if (it.hasNext()) {
                    ctrl = (PluginControl) it.next();
                } else {
                    break;
                }
            }
            if (restart_msg != null) {
                Messagebox.show(restart_msg);
            }
            refresh(); // refresh plugin list
        }
    }
    
    public void openPluginHelp(Event evt)
    {
        Object obj = evt.getData();
        if (obj == null) {
            Messagebox.show("Cannot open help. Missing parameter!");
            return;
        }
        String plugin_id = obj.toString();
        PluginControl ctrl = getPluginControl(plugin_id);
        if (ctrl != null) {
            String help_url = ctrl.getHelpURL();
            try {
                String client_action = 
                    "window.open('" + help_url + "', " +
                    "'_blank', 'width=850,height=600,resizable=yes,scrollbars=yes,location=yes,menubar=yes,status=yes');";
                Clients.evalJavaScript(client_action);
            } catch (Exception ex) {
                Messagebox.show(ex.getMessage());
            }
        }
    }

    public void openPluginLicense(Event evt)
    {
        Object obj = evt.getData();
        if (obj == null) {
            Messagebox.show("Cannot open license dialog. Missing parameter!");
            return;
        }
        String plugin_id = obj.toString();
        PluginControl ctrl = getPluginControl(plugin_id);
        if (ctrl != null) {
            String gui_lang = mainWin.getGUILanguage();
        
            LicenseDialog licDialog = (LicenseDialog) mainWin.getPage().getFellow("LicenseDialog");
            String license_html = ctrl.getLicense(gui_lang);
            if (license_html != null) {
                licDialog.showLicense(license_html);
            } else {
                Messagebox.show("License file not found!");
            }
        }
    }

    public void openPluginConfigDialog(Event evt)
    {
        Object obj = evt.getData();
        if (obj == null) {
            Messagebox.show("Cannot open plugin dialog. Missing parameter!");
            return;
        }
        String plugin_id = obj.toString();
        showPluginConfigDialog(getPluginControl(plugin_id));
    }

    public void render(Listitem item, Object data, int index) throws Exception 
    {
        // initGuiLanguage();
        if (! (data instanceof PluginControl)) return;
        
        PluginControl ctrl = (PluginControl) data;
        
        Listcell c1 = new Listcell(ctrl.getId());
        Listcell c2 = new Listcell(ctrl.getVersion());
        Listcell c3 = new Listcell(ctrl.getDescription());
        
        String status;
        String css = null;
        boolean is_loaded = ctrl.isLoaded();
        if (is_loaded) {
            css = "color:#F0F0F0; font-weight:bold; background-color:#00AA00;";  // green -> loaded
            status = label("text.plugin.loaded");
            if (! ctrl.isLoadOnStartUp()) {
                status += " [" + label("text.plugin.disable_on_startup") + "]";
            }
        } else {
            css = "color:#000000;";
            if (ctrl.isUninstallOnStartUp()) {
                status = label("text.plugin.uninstall_on_startup");
            } else 
            if (ctrl.isInstallError()) {
                css = "color:#AA0000;";   // red color for error
                status = ctrl.getErrorMessage();
                if ((status == null) || (status.equals(""))) {
                    status = label("text.plugin.install_error");
                }
            } else
            if (ctrl.isLoadOnStartUp()) {
                // should have been loaded on start-up; maybe some error occured
                String errmsg = ctrl.getErrorMessage();
                if ((errmsg == null) || (errmsg.length() == 0)) {
                    status = label("text.plugin.not_loaded") + " [" + label("text.plugin.restart_required") + "]";
                } else {
                    status = label("text.plugin.not_loaded") + ". " + errmsg;
                    css = "color:#AA0000;";   // red color for error
                }
            } else {
                status = label("text.plugin.disabled");
            }
        }
        Listcell c4 = new Listcell(status);
        if (css != null) {
            c4.setStyle(css);  // highlight status with green/red font color
        }
        
        Listcell c5 = new Listcell();
        Hbox hbox = new Hbox();
        hbox.setSpacing("5px");
        if (is_loaded && ctrl.hasConfigDialog()) {
            Button config_btn = new Button(label("label.plugin.config.btn"));
            // config_btn.setStyle("color:#000080;font-size:1em;");
            config_btn.addForward("onClick", "mainWin", "onOpenPluginConfig", ctrl.getId());
            hbox.appendChild(config_btn);
        }
        String help_url = ctrl.getHelpURL();
        if (is_loaded && (help_url != null) && (help_url.length() > 0)) {
            Toolbarbutton help_btn = new Toolbarbutton(label("label.plugin.help.btn"));
            help_btn.setStyle("color:#000080;font-size:1em;");
            help_btn.addForward("onClick", "mainWin", "onOpenPluginHelp", ctrl.getId());
            hbox.appendChild(help_btn);
        }
        if (ctrl.hasLicense()) {
            Toolbarbutton lic_btn = new Toolbarbutton(label("label.plugin.license.btn"));
            lic_btn.setStyle("color:#000080;font-size:1em;");
            lic_btn.addForward("onClick", "mainWin", "onOpenPluginLicense", ctrl.getId());
            hbox.appendChild(lic_btn);
        }
        c5.appendChild(hbox);

        item.appendChild(c1);
        item.appendChild(c2);
        item.appendChild(c3);
        item.appendChild(c4);
        item.appendChild(c5);
    }

    private String label(String key) 
    {
        return mainWin.i18n(key);
    }
    
//    private void initGuiLanguage()
//    {
//        if (guiLanguage == null) {
//            guiLanguage = mainWin.getGUILanguage();
//        }
//    }
    
    private void removePluginFromWebGUI(String pluginId)
    {
        DocmaWebSession webSess = GUIUtil.getDocmaWebSession(mainWin);
        webSess.unloadPluginComponents(pluginId);
    }
    
    private void addPluginToWebGUI(PluginControl ctrl) 
    {
        DocmaWebSession webSess = GUIUtil.getDocmaWebSession(mainWin);
        ctrl.sendOnInitMainWindow(webSess.getPluginInterface());
    }

    private void showPluginConfigDialog(PluginControl ctrl) 
    {
        DocmaWebSession webSess = GUIUtil.getDocmaWebSession(mainWin);
        try {
            ctrl.sendOnShowConfigDialog(webSess.getPluginInterface());
        } catch (Exception ex) {
            ex.printStackTrace();
            Messagebox.show("Plugin dialog error: " + ex.getMessage());
        }
    }
    
    private PluginControl getPluginControl(String plugin_id)
    {
        DocmaWebApplication webapp = GUIUtil.getDocmaWebApplication(mainWin);
        PluginManager pm = webapp.getPluginManager();
        return pm.getControl(plugin_id);
    }
    
    private boolean checkConnectedUsers() 
    {
        DocmaSession docmaSess = mainWin.getDocmaSession();
        DocmaWebApplication webApp = GUIUtil.getDocmaWebApplication(mainWin);
        Set<String> user_ids = new HashSet<String>(Arrays.asList(webApp.getConnectedUsers(null, null)));
        user_ids.remove(docmaSess.getUserId());
        if (! user_ids.isEmpty()) {
            if (Messagebox.show("Other users are still connected. It is discouraged, \n" + 
                                "to unload a plugin while it is still in use. \n" + 
                                "Continue anyway?", 
                                "Unload plugin?",
                                Messagebox.YES | Messagebox.NO, Messagebox.QUESTION) == Messagebox.YES) {
                return true;  // continue;
            } else {
                return false;  // abort
            }
        }
        return true;  // continue
    }
}
