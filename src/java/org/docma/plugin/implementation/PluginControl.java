/*
 * PluginControl.java
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

import java.io.*;
import java.util.*;
import org.docma.app.DocmaConstants;

import org.zkoss.util.resource.Labels;

import org.docma.util.Log;
import org.docma.plugin.*;
import org.docma.plugin.web.*;
import org.docma.util.DocmaUtil;

/**
 *
 * @author MP
 */
public class PluginControl 
{
    private final String pluginId;
    private final PluginManager pluginManager;
    private final PluginContext pluginContext;
    
    private File pluginDir;
    private File pluginPropsFile;
    private Properties pluginProps;
    
    private Plugin pluginInstance = null;

    private boolean loaded = false;
    private boolean installError = false;
    private String errorMessage = "";
    private boolean labelsLoaded = false;


    PluginControl(String pluginId, PluginManager pluginMgr) 
    {
        this.pluginId = pluginId;
        this.pluginManager = pluginMgr;
        this.pluginContext = new PluginContextImpl(this);
        
        init();
    }
    
    private void init() 
    {
        pluginDir = new File(pluginManager.getPluginsDir(), pluginId);
        if (! pluginDir.exists()) {
            throw new RuntimeException("Plugin directory does not exist: " + pluginDir.getAbsolutePath());
        }
        pluginProps = new Properties();
        pluginPropsFile = new File(pluginDir, PluginManager.PLUGIN_PROPS_FILENAME);
        if (pluginPropsFile.exists()) {
            loadProps(pluginPropsFile, pluginProps);

            String id_val = pluginProps.getProperty(PluginManager.PLUGIN_PROP_ID, "");
            if (! pluginId.equals(id_val)) {
                throw new RuntimeException("Plugin ID mismatch. Expected: " + pluginId + " Found: " + id_val);
            }
            String install_status = pluginProps.getProperty(PluginManager.PLUGIN_PROP_INSTALL_STATUS, "");
            installError = !install_status.equals(PluginManager.PLUGIN_INSTALL_STATUS_OK);
        } else {
            // If plugin properties file is missing, mark installation as invalid 
            // and use minimal properties as dummy
            pluginProps.setProperty(PluginManager.PLUGIN_PROP_ID, pluginId);
            installError = true;
        }
        loadLabels();
    }
    
    private void initPluginInstance()
    {
        if ((pluginInstance == null) && !installError) {
            // Instantiate the plugin class
            String cls_name = pluginProps.getProperty(PluginManager.PLUGIN_PROP_CLASS, "").trim();
            if (cls_name.length() > 0) {
                try {
                    Class cls = Class.forName(cls_name);
                    pluginInstance = (Plugin) cls.newInstance();
                } catch (Exception ex) {
                    installError = true;
                    errorMessage = "Failed to create class instance: " + cls_name;
                    ex.printStackTrace();
                }
            } else {
                pluginInstance = new DefaultPlugin();
            }
        }
    }
    
    PluginManager getPluginManager()
    {
        return pluginManager;
    }
    
    public String getId()
    {
        return pluginId;
    }
    
    public String getVersion()
    {
        return pluginProps.getProperty(PluginManager.PLUGIN_PROP_VERSION, "");
    }
    
    public String getDescription()
    {
        return Labels.getLabel(pluginId + "." + PluginManager.LABEL_PROP_DESCRIPTION, "");
    }
    
    public String getHelpURL()
    {
        return Labels.getLabel(pluginId + "." + PluginManager.LABEL_PROP_HELP_URL, "");
    }
    
    public boolean hasLicense()
    {
        return pluginProps.getProperty(PluginManager.PLUGIN_PROP_SHOW_LICENSE, "").equalsIgnoreCase("true");
    }

    public String getLicense(String lang)
    {
        File lic_dir = new File(pluginDir, PluginManager.LICENSE_DIR);
        if (! (lic_dir.exists() && lic_dir.isDirectory())) {
            return null;
        }
        lang = lang.toLowerCase();
        String[] fnames = lic_dir.list();
        String fallback_fn = null;
        String lic_fn = null;
        for (String fn : fnames) {
            if (fn.startsWith(PluginManager.LICENSE_FILE_NAME) && 
                fn.endsWith(PluginManager.LICENSE_FILE_EXTENSION)) {
                int pos1 = PluginManager.LICENSE_FILE_NAME.length(); // position after filename prefix
                if (fn.charAt(pos1) == '_') {  // filename contains language code
                    String fn_lang = fn.substring(pos1 + 1, fn.indexOf('.', pos1)).toLowerCase();
                    if (fn_lang.equals(lang)) {
                        lic_fn = fn;
                        break;
                    } else
                    if (fn_lang.equals("en") && (fallback_fn == null)) {
                        fallback_fn = fn;  // use English license as fallback if no default license exists
                    }
                } else {  // use license without language code (license.xhtml) as default
                    fallback_fn = fn;
                }
            }
        }
        if (lic_fn == null) {   // no exact language match -> use fallback license
            lic_fn = fallback_fn;
        }
        if (lic_fn == null) {   // no fallback license either -> use first license found
            if (fnames.length > 0) lic_fn = fnames[0];
            else return null;  // no license found
        }
        File lic_file = new File(lic_dir, lic_fn);
        try {
            return DocmaUtil.readFileToString(lic_file);
        } catch (Exception ex) {
            Log.error("Could not read license file " + lic_file + ": " + ex.getMessage());
            return null;
        }
    }
    
    public boolean hasConfigDialog()
    {
        return pluginProps.getProperty(PluginManager.PLUGIN_PROP_CONFIG_DIALOG, "").equalsIgnoreCase("true");
    }
    
    public boolean isRemoveWebFilesOnUnload()
    {
        return pluginProps.getProperty(PluginManager.PLUGIN_PROP_REMOVE_WEBFILES, "true").equalsIgnoreCase("true");
    }
    
    String getLoadType()
    {
        return pluginProps.getProperty(PluginManager.PLUGIN_PROP_LOAD_TYPE, "immediate").toLowerCase();
    }
    
    public boolean isLoadTypeImmediate()
    {
        return getLoadType().contains("immediate");
    }
    
    public boolean isLoadTypeNextStartUp()
    {
        return getLoadType().contains("next_startup");
    }
    
    public String[] getLoadClasses()
    {
        String val = pluginProps.getProperty(PluginManager.PLUGIN_PROP_LOAD_CLASSES);
        if (val == null) {
            return new String[0];
        }
        val = val.trim();
        StringTokenizer st = new StringTokenizer(val, " \t,");
        int cnt = st.countTokens();
        String[] cls_names = new String[cnt];
        for (int i=0; i < cnt; i++) {
            cls_names[i] = st.nextToken();
        }
        return cls_names;
    }
    
    public PluginContext getPluginContext()
    {
        return pluginContext;
    }
    
    public File getPluginDirectory()
    {
        return pluginDir;
    }
    
    public boolean isLoaded()
    {
        return loaded;
    }
    
    public boolean load()
    {
        if (installError) {
            loaded = false;  // cannot load plugin if installation was not okay
            return false;
        }
        
        errorMessage = "";  // reset error message
        if (loaded) {
            return true;  // plugin is already loaded; nothing to do
        }

        // Try to load the plugin
        boolean onload_started = false;
        try {
            // loadLabels(); // Load plugin labels (if not already loaded)
            getPluginManager().installWebFilesIfMissing(this);
            initPluginInstance();
            if (! installError) {
                String[] load_classes = getLoadClasses();
                for (String cls_name : load_classes) {
                    if (DocmaConstants.DEBUG) {
                        Log.info("Trying to load class: " + cls_name);
                    }
                    Class.forName(cls_name);
                }
                // Execute plugin's onLoad() method
                onload_started = true;
                pluginInstance.onLoad(pluginContext);   // if onLoad throws an exception, status is "not loaded"
                loaded = true;
            }
        } catch (Throwable ex) {
            errorMessage = ex.getMessage();
            ex.printStackTrace();
            if (onload_started) {
                try {
                    pluginInstance.onUnload(pluginContext);  // try to undo any partial loading actions
                } catch (Throwable ex2) {}
            }
        }
        return loaded;
    }
    
    public void unload()
    {
        if (loaded) {
            errorMessage = "";  // reset error message
            try {
                if (isRemoveWebFilesOnUnload()) {
                    boolean remove_complete = getPluginManager().removeWebFiles(pluginId);
                    if (! remove_complete) {
                        errorMessage = "Could not remove all files!";
                    }
                }
                pluginInstance.onUnload(pluginContext);
                // pluginInstance = null;
            } catch (Throwable ex) {
                errorMessage = ex.getMessage();  // set error message if problem occurs during unload
            }
            loaded = false;  // In any case plugin is considered as unloaded after this operation,
                             // because it has to be assumed, that unload() was
                             // (partially) successful, even if exception was thrown.
        }
    }
    
    public Plugin getPluginInstance()
    {
        return pluginInstance;
    }

    public void sendOnShowConfigDialog(WebUserSession webSess)
    {
        if (hasConfigDialog() && isLoaded()) {
            if (pluginInstance instanceof WebPlugin) {
                // loadLabels();  // Load plugin labels (might be needed by dialog)
                WebPlugin webplug = (WebPlugin) pluginInstance;
                webplug.onShowConfigDialog((WebPluginContext) pluginContext, webSess);
            }
        }
    }

    public void sendOnInitMainWindow(WebUserSession webSess)
    {
        if (isLoaded()) {
            if (pluginInstance instanceof WebPlugin) {
                WebPlugin webplug = (WebPlugin) pluginInstance;
                try {
                    webplug.onInitMainWindow((WebPluginContext) pluginContext, webSess);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Log.error("Exception in onInitMainWindow of plugin '" + 
                              getId() + "': " + ex.getMessage());
                }
            }
        }
    }
    
    public boolean isLoadOnStartUp()
    {
        // By default, plugin is automatically loaded on startup. However,
        // this can be disabled by setting the load_on_startup property to false.
        return pluginProps.getProperty(PluginManager.PLUGIN_PROP_LOAD_ON_STARTUP, "true").equals("true");
    }
    
    public void setLoadOnStartUp(boolean load_on_start)
    {
        pluginProps.setProperty(PluginManager.PLUGIN_PROP_LOAD_ON_STARTUP, load_on_start ? "true" : "false");
        savePluginProps();
    }
    
    public boolean isUninstallOnStartUp()
    {
        return pluginProps.getProperty(PluginManager.PLUGIN_PROP_UNINSTALL_ON_STARTUP, "").equals("true");
    }
    
    public void setUninstallOnStartUp()
    {
        pluginProps.setProperty(PluginManager.PLUGIN_PROP_LOAD_ON_STARTUP, "false");
        pluginProps.setProperty(PluginManager.PLUGIN_PROP_UNINSTALL_ON_STARTUP, "true");
        savePluginProps();
    }
    
    public boolean hasError()
    {
        if (isInstallError()) {
            return true;
        }
        return (errorMessage != null) && (errorMessage.length() > 0);
    }
    
    public String getErrorMessage()
    {
        return errorMessage;
    }

    public boolean isInstallError()
    {
        return installError;
    }
    
    void setInstallError(boolean isError)
    {
        if (isError) {
            setInstallError(isError, "Installation failed.");
        } else {
            setInstallError(false, null);
        }
    }
    
    void setInstallError(boolean isError, String message)
    {
        if (message == null) {
            message = "";
        }
        installError = isError;
        errorMessage = message;
        String status = isError ? PluginManager.PLUGIN_INSTALL_STATUS_ERROR : 
                                  PluginManager.PLUGIN_INSTALL_STATUS_OK;
        pluginProps.setProperty(PluginManager.PLUGIN_PROP_INSTALL_STATUS, status);
        pluginProps.setProperty(PluginManager.PLUGIN_PROP_INSTALL_MESSAGE, message);
        savePluginProps();
    }
    
    Properties getPluginProperties()
    {
        return pluginProps;
    }
    
    private void savePluginProps()
    {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(pluginPropsFile);
            pluginProps.store(out, "Plugin properties");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (out != null) {
                try { out.close(); } catch (Exception ex2) {}  // ignore close exception
            }
        }
    }

    private void loadProps(File propsFile, Properties props)
    {
        InputStream props_in = null;
        try {
            props_in = new FileInputStream(propsFile);
            props.load(props_in);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            if (props_in != null) {
                try { props_in.close(); } catch (Exception ex2) {}  // ignore close exception
            }
        }
    }
    
    private synchronized void loadLabels()
    {
        if (labelsLoaded) return;
        
        Labels.register(new PluginLabelLocator(pluginDir, PluginManager.LABEL_PROPS_FILE_PATTERN)); // locale specific labels
        labelsLoaded = true;
    }
}
