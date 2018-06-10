/*
 * PreinstallHelper.java
 * 
 *  Copyright (C) 2018  Manfred Paula, http://www.docmenta.org
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

import java.io.*;
import java.util.*;

/**
 *
 * @author MP
 */
public class PreinstallHelper 
{
    /**
     * Install all plugin zip files located in the preinstallFolder directory.
     * If the folder contains a different plugin-version than an already installed 
     * plugin, then the old version is uninstalled before the version located
     * in the directory is installed.
     * <p>
     * The plugins that have been installed from this directory 
     * are marked as "preinstalled", that means they are considered part of the base 
     * installation (so called system plugins). System plugins cannot be uninstalled 
     * by the user.
     * </p>
     * <p>
     * Plugins that are marked as "preinstalled", but for which no corresponding 
     * zip file exists in the preinstallFolder direcory are uninstalled. 
     * This way, system plugins are removed from existing 
     * installations, as soon as they are no longer included in the installation package.
     * </p>
     * <p>
     * System plugins should be "dummy" plugins. That means:
     * The libraries and web-files that are required/referenced by the plugin have to be
     * included in the application installation package. The plugin archive (zip file) itself   
     * should only contain the plugin configuration. This assures that system plugins    
     * can be loaded immediately on first server-startup. Furthermore, this allows loading of
     * system plugins with read-only access to the webapp folder.
     * </p>
     */
    public static boolean preinstallAll(PluginManager pm, File preinstallFolder)
    {
        boolean restart_required = false;
        if ((preinstallFolder != null) && preinstallFolder.isDirectory()) {
            File[] arr = preinstallFolder.listFiles();
            // if (arr == null) { // IO Error?
            //     return;
            // }
            Set<String> preIds = new HashSet<String>();
            for (File f : arr) {
                String fn = f.getName();
                if (f.isFile() && fn.endsWith(".zip")) {
                    Properties pluginProps = null;
                    try {
                        pluginProps = pm.checkPluginFile(f, null, null, null, null, null);
                    } catch(Exception ex) {  // invalid plugin zip
                        ex.printStackTrace();
                        continue;
                    }
                    String plugId = pluginProps.getProperty(PluginManager.PLUGIN_PROP_ID).trim();
                    preIds.add(plugId);
                    String plugVer = pluginProps.getProperty(PluginManager.PLUGIN_PROP_VERSION).trim();
                    PluginControl ctrl = pm.getControl(plugId);
                    if (ctrl != null) {  // plugin is already installed
                        String installedVer = ctrl.getVersion();
                        if (plugVer.equals(installedVer)) {
                            continue;  // same version is already installed -> do nothing
                        } else {
                            // Another plugin version is installed (from previous Docmenta version).
                            // Uninstall this version (to allow installation of the new version).
                            boolean completed = pm.uninstall(plugId, true);
                            if (! completed) {
                                restart_required = true;
                                continue;
                            }
                        }
                    }
                    
                    // Plugin is not installed or old version has been uninstalled.
                    boolean ok = preinstall(pm, f, pluginProps);
                    if (! ok) {
                        restart_required = true;
                    }
                }
            }
            
            // Now uninstall preinstalled plugins, for which no zip exists in the
            // preinstalled folder. This occurs in case an old Docmenta version  
            // included a preinstalled plugin, which is no longer included in  
            // newer Docmenta versions.
            for (PluginControl ctrl : pm.listControls()) {
                if (ctrl.isPreinstalled() && !preIds.contains(ctrl.getId())) {
                    boolean completed = pm.uninstall(ctrl.getId(), false);
                    if (! completed) {
                        restart_required = true;
                    }
                }
            }
        }
        return restart_required;
    }
    
    private static boolean preinstall(PluginManager pm, File plugFile, Properties pluginProps)
    {
        try {
            // Install plugin; install() throws an exception if installation fails
            pm.install(plugFile, 
                       pluginProps, 
                       false,  // complete install, restore web = false
                       false,   // do not overwrite shared web files
                       false,   // do not overwrite global web files
                       null, null, null, null);   // to do (show file conflicts as warning)
            // For plugins placed in the preinstall folder:
            // The libraries and web-files that are required/referenced by the plugin have to be
            // included in the application installation package, to allow immediate
            // loading of the plugin on first server-startup (and to allow loading of
            // preinstalled plugins with read-only access to the webapp folder).
            String plugId = pluginProps.getProperty(PluginManager.PLUGIN_PROP_ID).trim();
            PluginControl ctrl = pm.getControl(plugId);
            if (ctrl != null) {  // should not be null if installation was ok!
                ctrl.setLoadOnStartUp(true);  // set flag to load plugin on every server start-up
                ctrl.setPreinstalled(true);  // mark plugin as preinstalled
                // ctrl.load();  // load plugin
                return true;  // installation succeeded
            } else {
                return false;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
    
}
