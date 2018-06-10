/*
 * PluginManager.java
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
import java.util.zip.*;
import org.docma.app.DocmaConstants;

import org.docma.util.*;
import org.docma.plugin.*;
import org.docma.plugin.web.*;

/**
 *
 * @author MP
 */
public class PluginManager 
{
    public static final String REGEXP_PLUGIN_ID = "[A-Za-z_][0-9A-Za-z_]{1,39}";
    
    // Plugin properties filename:
    public static final String PLUGIN_PROPS_FILENAME = "plugin.properties";
    // Plugin properties to be provided by the plugin-developer:
    public static final String PLUGIN_PROP_ID = "id";                           // plugin id
    public static final String PLUGIN_PROP_VERSION = "version";                 // plugin version
    public static final String PLUGIN_PROP_CLASS = "plugin_class";
    public static final String PLUGIN_PROP_SHOW_LICENSE = "show_license";    // true / false
    public static final String PLUGIN_PROP_CONFIG_DIALOG = "config_dialog";  // true / false
    public static final String PLUGIN_PROP_KEEP_FILES = "keep_files"; 
    public static final String PLUGIN_PROP_FORCE_OVERWRITE = "force_overwrite"; 
    public static final String PLUGIN_PROP_REQUIRED_APP_VERSION = "required_app_version";
    public static final String PLUGIN_PROP_REMOVE_WEBFILES = "remove_webfiles"; // true / false
    public static final String PLUGIN_PROP_LOAD_TYPE = "load_type";  // immediate, next_startup, request_restart
    public static final String PLUGIN_PROP_LOAD_CLASSES = "load_classes";
    public static final String PLUGIN_PROP_DISABLE_SUPPORTED = "disable_supported";
    // Plugin properties used internally:
    public static final String PLUGIN_PROP_INSTALL_STATUS = "install_status";   // installation status: ok / error
    public static final String PLUGIN_PROP_INSTALL_MESSAGE = "install_message"; // message shown to user in case of error
    public static final String PLUGIN_PROP_LOAD_ON_STARTUP = "load_on_startup";   // true / false
    public static final String PLUGIN_PROP_UNINSTALL_ON_STARTUP = "uninstall_on_startup";   // true / false
    public static final String PLUGIN_PROP_PREINSTALLED = "preinstalled";  // true / false
    // Plugin property values
    public static final String PLUGIN_INSTALL_STATUS_OK = "ok";
    public static final String PLUGIN_INSTALL_STATUS_ERROR = "error";
    
    // Label properties filename:
    public static final String LABEL_PROPS_FILE_PATTERN = "locale?.properties";
    // Label properties:
    public static final String LABEL_PROP_DESCRIPTION = "description";
    public static final String LABEL_PROP_HELP_URL = "help_url";
    
    // License filename constants
    public static final String LICENSE_DIR = "license";
    public static final String LICENSE_FILE_NAME = "license";
    public static final String LICENSE_FILE_EXTENSION = ".xhtml";
    static final String LICENSE_START = LICENSE_DIR + "/" + LICENSE_FILE_NAME;

    // Plugin manager properties filename:
    private static final String INSTALLED_PROPS_FILENAME = "installed_plugins.properties";
    // Plugin manager properties:
    private static final String INSTALLED_PROP_IDS = "installed_plugins";       // plugin ids in installation sequence
    private static final String INSTALLED_PROP_LOAD_SEQUENCE = "load_sequence"; // plugin ids in load sequence
    
    private static final String TEMP_FILE_PREFIX = "temp_";

    private File pluginsDir;
    private File webAppDir;
    private File preinstalledDir;
    private ApplicationContext appContext;
    private WebContext webContext;

    private boolean initialized = false;
    private Properties installedProps = new Properties();
    private List<PluginControl> controlList = null;
    private SortedMap<String, String> installedWebFiles = new TreeMap<String, String>();
    private boolean files_restored_on_startup = false;
    private boolean load_error_on_startup = false;
    
    
    public PluginManager(File pluginsDir, 
                         ApplicationContext appCtx, 
                         WebContext webCtx) throws Exception
    {
        if (! pluginsDir.exists()) {
            throw new Exception("PluginManager Exception: Plugins directory does not exist: " + pluginsDir);
        }
        this.pluginsDir = pluginsDir;
        this.webAppDir =  webCtx.getWebAppDirectory();
        this.appContext = appCtx;
        this.webContext = webCtx;
        if (! webAppDir.exists()) {
            throw new Exception("PluginManager Exception: WebApp directory does not exist: " + webAppDir);
        }
        preinstalledDir = new File(webAppDir, "docma" + File.separator + "preinstall_plugs");
    }
    
    public File getPluginsDir()
    {
        return pluginsDir;
    }
    
    public ApplicationContext getApplicationContext()
    {
        return appContext;
    }
    
    public WebContext getWebContext()
    {
        return webContext;
    }

    public void sendOnInitMainWindow(WebUserSession webSess, String pluginId)
    {
        PluginControl ctrl = getControl(pluginId);
        if ((ctrl != null) && ctrl.isLoaded()) { 
            ctrl.sendOnInitMainWindow(webSess);
        }
    }

    public void sendOnInitMainWindow(WebUserSession webSess)
    {
        for (PluginControl ctrl : controlList) {
            if (ctrl.isLoaded()) {
                ctrl.sendOnInitMainWindow(webSess);
            }
        }
    }
    
    /*
     * Create plugin controls. Initial state of installed plugins is "unloaded".
     */    
    public synchronized void init()
    {
        if (initialized) return;

        Log.info("Plugin manager: initialization started.");
        loadInstalledProps(); // read plugin manager properties
        refreshControls();    // create controls for all installed plugins; initial state is "unloaded"
        
        // Load preinstalled plugins
        PreinstallHelper.preinstallAll(this, preinstalledDir);
        
        files_restored_on_startup = checkWebFilesOnStartUp();
        if (installedWebFiles.isEmpty()) {  // if not empty then init was already done in checkWebFilesOnStartUp()
            initWebFilesMap();
        }
        Log.info("Plugin manager: initialization finished.");
        
        initialized = true;
    }

    
    /**
     * Refreshes the controls list. Call this method after a plugin has been 
     * installed, uninstalled or has changed its loading position.
     * It is recommended to unload plugins before this method is called.
     */
    private synchronized void refreshControls() 
    {
        List<PluginControl> old_controls = controlList;  // is null on startup
        controlList = new ArrayList<PluginControl>();

        String[] pluginIds = getLoadSequence();
        if ((pluginIds == null) || (pluginIds.length == 0)) {
            pluginIds = listInstalledPlugins();
        }
        for (int i=0; i < pluginIds.length; i++) {
            String plugId = pluginIds[i];
            PluginControl ctrl = null;
            // Find existing control with given id
            if (old_controls != null) {
                for (int k = 0; k < old_controls.size(); k++) {
                    PluginControl old_ctrl = old_controls.get(k);
                    if (old_ctrl.getId().equals(plugId)) {
                        ctrl = old_ctrl;
                        old_controls.remove(k);
                        break;
                    }
                }
            }
            
            // Reuse existing control or create new control
            if (ctrl == null) { // no control exists with given id -> create new control
                try {
                    ctrl = new PluginControl(plugId, this);  // initial state is "unloaded"
                } catch (Throwable ex) {
                    Log.error("Could not create control for plugin '" + plugId + "': " + ex.getMessage());
                }
            }
            // else {
            //    ctrl.refresh();
            // }
            if (ctrl != null) {
                controlList.add(ctrl);
            }
        }
    }


    public synchronized PluginControl[] listControls()
    {
        // init();
        int cnt = controlList.size();
        return controlList.toArray(new PluginControl[cnt]);
    }
    
    
    public PluginControl getControl(String pluginId) 
    {
        // init();
        for (int i=0; i < controlList.size(); i++) {
            PluginControl ctrl = controlList.get(i);
            if (ctrl.getId().equals(pluginId)) return ctrl;
        }
        return null;
    }


    public synchronized void startUpPlugins()
    {
        // init();
        load_error_on_startup = false;
        
        for (int i=0; i < controlList.size(); i++) {
            PluginControl ctrl = controlList.get(i);
            if (ctrl.isLoadOnStartUp()) {
                boolean okay = ctrl.load();
                if (! okay) {
                    load_error_on_startup = true;
                    Log.warning("Failed to load plugin '" + ctrl.getId() + 
                                "' at position " + i + ": " + ctrl.getErrorMessage());
                }
            }
        }
    }


    public boolean isServerRestartRecommendedToFixError()
    {
        return load_error_on_startup && files_restored_on_startup;
    }


    public boolean hasUninstallOnServerRestart()
    {
        for (int i=0; i < controlList.size(); i++) {
            PluginControl ctrl = controlList.get(i);
            if (ctrl.isUninstallOnStartUp()) {
                return true;
            }
        }
        return false;
    }


    public synchronized void unloadPlugins()
    {
        // if (controlList == null) return;
        
        for (int i=0; i < controlList.size(); i++) {
            PluginControl ctrl = controlList.get(i);
            if (ctrl.isLoaded()) {
                ctrl.unload();
            }
        }
    }


    /**
     * Returns true if a plugin with the given id is already installed. 
     * @param pluginId
     * @return true if a plugin with the given id is already installed, otherwise false.
     */
    public synchronized boolean isInstalled(String pluginId)
    {
        // init();
        for (int i=0; i < controlList.size(); i++) {
            if (controlList.get(i).getId().equals(pluginId)) return true;
        }
        return false;
    }


    public File writePluginStreamToTempFile(InputStream stream) throws Exception
    {
        deleteTempFiles();   // delete old temp files from previous installations
        
        String tempName = TEMP_FILE_PREFIX + System.currentTimeMillis();
        String tempZipName = tempName + ".zip";
        File tempZipFile = new File(pluginsDir, tempZipName);
        
        if (tempZipFile.exists()) {  // should never occur
            throw new Exception("Could not install plugin. Please retry. File conflict: " + tempZipFile);
        }

        try {
            DocmaUtil.writeStreamToFile(stream, tempZipFile);
        } finally {
            try {
                stream.close();
            } catch (Exception ex) {
                Log.warning("Could not close stream in PluginManager.install()");  // ignore close exception
            } 
        }
        return tempZipFile;
    }
    
    public void deleteTempFiles() 
    {
        File[] files = pluginsDir.listFiles();

        for (int i=0; i < files.length; i++) {
            File f = files[i];
            if (f.isFile() && f.getName().startsWith(TEMP_FILE_PREFIX)) {
                f.delete();
            }
        }
    }


    /*
     * Returns the plugin.properties file as java.util.Properties object. 
     * If the zip is not a valid plugin, an exception is thrown.
     */
    public Properties checkPluginFile(File pluginFile, 
                                      Map<String, String> license_map,
                                      SortedSet<String> sharedFilesIdentical, 
                                      SortedSet<String> sharedFilesDifferent,
                                      SortedSet<String> globalFilesIdentical,
                                      SortedSet<String> globalFilesDifferent) 
                                      throws Exception
    {
        Properties pluginProps = null;
        
        InputStream in = new FileInputStream(pluginFile);
        try {
            ZipInputStream zip_in  = new ZipInputStream(in);

            ZipEntry entry;
            while ((entry = zip_in.getNextEntry()) != null) {
                String entry_name = entry.getName();
                if (entry_name.equals(PLUGIN_PROPS_FILENAME)) {
                    try {
                        pluginProps = new Properties();
                        pluginProps.load(zip_in);
                    } catch (Exception ex) {
                        throw new Exception("Could not load file plugin.properties. " + ex.getMessage());
                    }
                    // break;  // currently only the plugin.proeprties file is checked
                } else
                if (entry_name.equals(LICENSE_START + LICENSE_FILE_EXTENSION)) {  
                    // Entry path is license/license.xhtml
                    if (license_map != null) { 
                        license_map.put("en", readUTF8StreamToString(zip_in));
                    }
                } else
                if (entry_name.startsWith(LICENSE_START + "_") && entry_name.endsWith(LICENSE_FILE_EXTENSION)) {
                    if (license_map != null) { 
                        int pos1 = LICENSE_START.length() + 1;  // position after underscore
                        int pos2 = entry_name.indexOf('.', pos1);
                        String lang_code = entry_name.substring(pos1, pos2).toLowerCase();
                        license_map.put(lang_code, readUTF8StreamToString(zip_in));
                    }
                }
                zip_in.closeEntry();
            }

            zip_in.close();

            // To do: check file conflicts with global web-files and 
            //        installed plugin-files (installedWebFiles)
            
        } finally {
            try { in.close(); } catch (Exception ex) {}
        }
        if (pluginProps == null) {
            throw new Exception("Missing file: " + PLUGIN_PROPS_FILENAME);
        }
        String pluginId = pluginProps.getProperty(PLUGIN_PROP_ID).trim();
        if (! isValidPluginId(pluginId)) {
            throw new Exception("Cannot install plugin: Invalid plugin ID!");
        }
        return pluginProps;
    }


    public void install(File pluginFile, 
                        Properties pluginProps, 
                        boolean modeRestoreWebFiles,
                        boolean overwriteShared, 
                        boolean overwriteGlobal, 
                        SortedSet<String> sharedFilesSkipped, 
                        SortedSet<String> sharedFilesOverwritten, 
                        SortedSet<String> globalFilesSkipped, 
                        SortedSet<String> globalFilesOverwritten) 
                        throws Exception 
    {
        boolean modeCompleteInstall = !modeRestoreWebFiles;
        
        String pluginId = pluginProps.getProperty(PLUGIN_PROP_ID).trim();
        if (! isValidPluginId(pluginId)) {
            throw new Exception("Cannot install plugin: Invalid plugin ID!");
        }
        File installDir = new File(pluginsDir, pluginId);

        Set<String> keepFiles = null;
        boolean modeUpgrade = modeCompleteInstall && installDir.exists(); 
        if (modeUpgrade) {
            keepFiles = getKeepFilesOnReinstall(pluginProps);
        } else {
            installDir.mkdir();
        }

        // Add plugin id to list of installed plugins:
        if (modeCompleteInstall) {
            String[] id_arr = listInstalledPlugins();
            if (Arrays.asList(id_arr).contains(pluginId)) {
                throw new Exception("Cannot install plugin: Plugin with same ID already exists!");
            }
            String ids = installedProps.getProperty(INSTALLED_PROP_IDS, "").trim();
            if (ids.length() > 0) {
                ids += "," + pluginId;
            } else {
                ids = pluginId;  // the first plugin 
            }
            installedProps.setProperty(INSTALLED_PROP_IDS, ids);
            saveInstalledProps();
        }

        // Move zip file to plugin installation directory (for later restore of web-files on application upgrade)
        if (modeCompleteInstall) {
            File destFile = new File(installDir, pluginId + ".zip");
            if (destFile.exists()) {
                if (! destFile.delete()) {  // should not occur, if old version of plugin was uninstalled; 
                    throw new Exception("Cannot install plugin: Failed to remove old version of plugin!");
                }
            }
            if (pluginFile.renameTo(destFile)) {
                pluginFile = destFile;
            } else {
                throw new Exception("Could not move plugin file to installation folder.");
            }
        }

        // Install files (extract zip file):
        List<String> webFilesList = new ArrayList<String>(500);
        InputStream in = new FileInputStream(pluginFile);
        try {
            ZipInputStream zip_in  = new ZipInputStream(in);

            ZipEntry entry;
            while ((entry = zip_in.getNextEntry()) != null) {
                String entry_name = entry.getName();
                boolean isDir = entry.isDirectory();
                if (entry_name.startsWith("web/")) {
                    // Install web-files
                    String path = entry_name.substring("web/".length());
                    installZipEntry(webAppDir, path, isDir, zip_in, null, 
                                    webFilesList, overwriteShared, overwriteGlobal);
                } else
                if (entry_name.startsWith("lib/")) {
                    // Install libraries to web-folder WEB-INF/lib
                    String path = "WEB-INF/" + entry_name;
                    installZipEntry(webAppDir, path, isDir, zip_in, null, 
                                    webFilesList, overwriteShared, overwriteGlobal);
                } else {
                    if (modeCompleteInstall) {
                        // Install non web-files to plugin directory
                        installZipEntry(installDir, entry_name, isDir, zip_in, keepFiles, 
                                        null, false, false);  // 3 arguments only of interest for web-files
                    }
                }
                zip_in.closeEntry();
            }
            zip_in.close();
        } finally {
            try { in.close(); } catch (Exception ex) {}
        }
        
        // Write uninstall web-files log
        writeWebUninstallLog(pluginId, webFilesList);
        updateWebFilesMap(webFilesList, pluginId);

        // Write installation status OK:
        pluginProps.setProperty(PLUGIN_PROP_INSTALL_STATUS, PLUGIN_INSTALL_STATUS_OK);
        pluginProps.setProperty(PLUGIN_PROP_INSTALL_MESSAGE, "");
        File propsFile = new File(installDir, PLUGIN_PROPS_FILENAME);
        OutputStream out = new FileOutputStream(propsFile);
        pluginProps.store(out, "Plugin properties");
        out.close();

        refreshControls();  // Add control for installed plugin to list;
                            // Control can now be used to load the installed plugin.
    }
    

    private void installZipEntry(File outDir,
                                 String relative_path, 
                                 boolean isDirectory,
                                 InputStream in, 
                                 Set<String> keepFiles,
                                 List<String> webFilesList, 
                                 boolean overwriteShared, 
                                 boolean overwriteGlobal) 
                                 throws Exception
    {
        if (relative_path.length() == 0) return;
        
        String filepath = systemPath(relative_path);  // transform to system-dependant separator character
        File outFile = new File(outDir, filepath);
        boolean doWrite = true;
        boolean isGlobalWebFile = false;
        boolean fileExists = outFile.exists();
        if (fileExists) {
            if (webFilesList != null) {  // if to be installed in webapp directory
                // Note: A global web-file is a file that belongs to the base 
                // application, i.e. it existed before the plugin was installed
                // and the file has not been installed by another plugin.
                isGlobalWebFile = !installedWebFiles.containsKey(relative_path);
                doWrite = isGlobalWebFile ? overwriteGlobal : overwriteShared; 
            }
            if (keepFiles != null) {  // keepFiles should be null for web-files
                // Do not overwrite a file in the plugin installation directory
                // if the file is in the keepFiles list.
                // For example this can be used to reinstall a plugin and
                // keep e.g. the configuration files from the previous installation.
                String rel_path2 = relative_path.startsWith("/") ? relative_path.substring(1) : 
                                                                   ("/" + relative_path);
                if (keepFiles.contains(relative_path) || keepFiles.contains(rel_path2)) {
                    doWrite = false; 
                }
            }
        }
        if (doWrite) {
            if (isDirectory) {
                // Create directory entry if it does not already exist:
                if (! fileExists) outFile.mkdirs();
            } else {
                // Write new file or overwrite existing file:
                File parentDir = outFile.getParentFile();
                if (! parentDir.exists()) parentDir.mkdirs();
                FileOutputStream out = new FileOutputStream(outFile);
                try {
                    DocmaUtil.copyStream(in, out);
                } finally {
                    try { out.close(); } catch (Exception ex) {} // ignore close exception
                }
            }
        }
        if ((webFilesList != null) && !isGlobalWebFile) {
            // Add file to uninstall log:
            webFilesList.add(relative_path);
            // Note:
            // If a plugin overwrites a global web-file, then this global
            // file is not listed in the uninstall log, because deleting
            // a global web-file would damage the application.
        }
    }

    boolean removeWebFiles(String pluginId) 
    {
        return uninstallWebFiles(pluginId, false);
    }

    boolean uninstallWebFiles(String pluginId, boolean force_delete)
    {
        boolean completely_removed = true;

        // Uninstall web files (if file not used by other plugin)
        File logFile = getUninstallLogFile(pluginId);
        if (logFile.exists()) {
            try {
                List<String> file_list = new ArrayList<String>(1000);
                FileInputStream fin = new FileInputStream(logFile);
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(fin, "UTF-8"));
                    String webpath;
                    while ((webpath = reader.readLine()) != null) {
                        webpath = webpath.trim();
                        if (! webFileUsedByOther(webpath, pluginId)) {
                            String filepath = systemPath(webpath);
                            file_list.add(filepath);
                        }
                    }
                } finally {
                    try { fin.close(); } catch (Exception cex) {}
                }
                // Delete files in reverse order as they have been created.
                // This assures that directories are empty before they are deleted.
                for (int i = file_list.size() - 1; i >= 0; i--) {
                    String filepath = file_list.get(i);
                    File f = new File(webAppDir, filepath);
                    if (f.exists()) {
                        // if (DocmaConstants.DEBUG) Log.info("Deleting file " + f);
                        if (! f.delete()) {
                            Log.warning("Failed to delete file " + f);
                            // Note: If deleting a directory fails, this shall be ignored.
                            if (! f.isDirectory()) { 
                                completely_removed = false;
                                if (force_delete) {
                                    f.deleteOnExit();
                                    Log.info("Deleting file " + f + " on exit.");
                                }
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                completely_removed = false;
                ex.printStackTrace();
            }
            if (completely_removed) {
                // Remove the uninstall log itself
                if (! logFile.delete()) {
                    completely_removed = false;
                }
            }
        }
        initWebFilesMap();  // remove uninstalled web-files from installedWebFiles map
        return completely_removed;
    }

    public boolean uninstall(String pluginId, boolean reinstall_mode)
    {
        PluginControl ctrl = getControl(pluginId);
        if (ctrl == null) return false;

        // Plugin should be unloaded before uninstall
        if (ctrl.isLoaded()) ctrl.unload();

        // Uninstall web-files
        boolean completely_removed = uninstallWebFiles(pluginId, true);
        if (! completely_removed) {
            // This can occur if, for example, a jar file to be deleted is still
            // loaded by the web-server and therefore delete fails. In this case
            // the uninstall has to be deferred to the next server start-up.
            ctrl.setUninstallOnStartUp();
            return false; // return false to indicate that uninstall is not completed
        }

        // Delete plugin directory (if reinstall mode, then keep marked files)
        File installDir = new File(pluginsDir, pluginId);
        if (installDir.exists()) {
            Set<String> keepFiles = null;
            if (reinstall_mode) {
                keepFiles = getKeepFilesOnReinstall(ctrl.getPluginProperties());
            }
            if (! recursiveFileDelete(installDir, "", keepFiles)) {
                completely_removed = false;
            }
        }
        
        // Remove plugin id from list of installed plugins
        List new_installed = new ArrayList(Arrays.asList(listInstalledPlugins()));
        new_installed.remove(pluginId);
        installedProps.setProperty(INSTALLED_PROP_IDS, DocmaUtil.concatStrings(new_installed, ","));
        // Remove plugin id from load sequence list
        String load_ids = installedProps.getProperty(INSTALLED_PROP_LOAD_SEQUENCE , "").trim();
        if (load_ids.length() > 0) {  // if load sequence is set
            List new_sequence = new ArrayList(Arrays.asList(getLoadSequence()));
            new_sequence.remove(pluginId);
            installedProps.setProperty(INSTALLED_PROP_LOAD_SEQUENCE, 
                                       DocmaUtil.concatStrings(new_sequence, ","));
        }
        saveInstalledProps();

        refreshControls();  // remove control from list
        
        return completely_removed;
    }

    
    public boolean webFilesInstalled(String pluginId) 
    {
        File pluginLogFile = getUninstallLogFile(pluginId);
        return pluginLogFile.exists();
    }

    public void installWebFilesIfMissing(PluginControl ctrl) 
    {
        if (! webFilesInstalled(ctrl.getId())) {
            installWebFiles(ctrl);  // try to restore web-files
        }
    }

    
    public void installWebFiles(PluginControl ctrl)
    {
        Log.info("Plugin '" + ctrl.getId() + "': Restoring web-files.");

        // Set error. This error is shown if installWebFiles has not been
        // finished, e.g. due to power failure.
        ctrl.setInstallError(true, "Installation error: incomplete web-files installation.");  // mark installation as corrupted

        try {
            File logDir = getPluginWebLogDir();
            if (! logDir.exists()) { 
                logDir.mkdirs();
            }
            String pluginId = ctrl.getId();
            File installDir = new File(pluginsDir, pluginId);
            File pluginZipFile = new File(installDir, pluginId + ".zip");
            if (pluginZipFile.exists()) {
                final boolean MODE_RESTORE = true;
                install(pluginZipFile, 
                        ctrl.getPluginProperties(), 
                        MODE_RESTORE, 
                        true,   // overwrite files shared by plugins
                        true,   // overwrite non-plugin files
                        null, null, null, null);
            } else {
                throw new Exception("Plugin file missing: " + pluginZipFile.getName());
            }
            ctrl.setInstallError(false);  // clear error
        } catch (Exception ex) {
            ctrl.setInstallError(true, "Installation corrupted. Could not reinstall web-files: " + ex.getMessage());
        }
    }


    public synchronized String[] getLoadSequence()
    {
        String ids = installedProps.getProperty(INSTALLED_PROP_LOAD_SEQUENCE , "").trim();
        if (ids.length() == 0) { 
            return listInstalledPlugins();  // installation sequence is load sequence
        } else {
            return ids.split("[, ]+");
        }
    }


    public synchronized void setLoadSequence(String[] pluginIds) throws Exception
    {
        List load_list = Arrays.asList(pluginIds);
        Set load_set = new HashSet(load_list);
        Set installed_set = new HashSet(Arrays.asList(listInstalledPlugins()));
        if (! load_set.equals(installed_set)) {
            throw new Exception("PluginManager.setLoadSequence() failed: set of ids differs from set of installed plugins.");
        }
        String ids = DocmaUtil.concatStrings(load_list, ",");
        installedProps.setProperty(INSTALLED_PROP_LOAD_SEQUENCE, ids);
        saveInstalledProps();
    }


    private void loadInstalledProps()
    {
        installedProps.clear();
        try {
            File f = new File(pluginsDir, INSTALLED_PROPS_FILENAME);
            if (f.exists()) { 
                InputStream fin = new FileInputStream(f);
                try {
                    installedProps.load(fin);
                } finally {
                    try { fin.close(); } catch (Exception ex) {}  // ignore close exception
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }


    private void saveInstalledProps()
    {
        FileOutputStream out = null;
        try {
            File f = new File(pluginsDir, INSTALLED_PROPS_FILENAME);
            out = new FileOutputStream(f);
            installedProps.store(out, "Plugin manager properties");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (out != null) {
                try { out.close(); } catch (Exception ex2) {}  // ignore close exception
            }
        }
    }


    private String[] listInstalledPlugins()
    {
        String ids = installedProps.getProperty(INSTALLED_PROP_IDS, "").trim();
        if (ids.length() == 0) { 
            return new String[0];
        } else {
            return ids.split("[, ]+");
        }
    }


    private boolean checkWebFilesOnStartUp()
    {
        boolean files_restored = false;
        for (int i=0; i < controlList.size(); i++) {
            PluginControl ctrl = controlList.get(i);
            try {
                if (ctrl.isUninstallOnStartUp()) {
                    Log.info("Uninstalling plugin " + ctrl.getId() + " on start-up.");
                    uninstall(ctrl.getId(), false);  // argument false means complete uninstall
                } else {
                    boolean webfiles_exist = webFilesInstalled(ctrl.getId());
                    if (ctrl.isLoadOnStartUp()) {
                        if (! webfiles_exist) { 
                            Log.info("Installing web-files of plugin " + ctrl.getId());
                            installWebFiles(ctrl);
                            files_restored = true;
                        }
                    } else  // plugin is disabled
                    if (webfiles_exist && ctrl.isRemoveWebFilesOnUnload()) {
                        // For some reason, the web-files were not deleted
                        // when plugin was disabled (e.g. files were still in use
                        // and therefore could not be deleted). 
                        // Try to remove the web-files now (on start-up).
                        Log.info("Removing web-files of plugin " + ctrl.getId());
                        removeWebFiles(ctrl.getId());
                    }
                }
            } catch (Exception ex) {  // Catch any kind of runtime exception
                ex.printStackTrace();
                Log.error("Could not synchronize web-files for plugin '" + ctrl.getId() + "': " + ex.getMessage());
            }
        }
        return files_restored;
    }
    

    private void initWebFilesMap()
    {
        // Merge web uninstall-log of all plugins
        installedWebFiles.clear();
        try {
            for (int i=0; i < controlList.size(); i++) {
                PluginControl ctrl = controlList.get(i);
                String plugId = ctrl.getId();
                File pluginLogFile = getUninstallLogFile(plugId);
                if (pluginLogFile.exists()) {
                    FileInputStream fin = new FileInputStream(pluginLogFile);
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(fin, "UTF-8"));
                        String webfile;
                        while ((webfile = reader.readLine()) != null) {
                            registerInstalledWebFile(webfile.trim(), plugId);
                        }
                    } finally {
                        try { fin.close(); } catch (Exception cex) {}
                    }
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }


    private void updateWebFilesMap(List<String> webFiles, String pluginId)
    {
        for (int i = 0; i < webFiles.size(); i++) {
            registerInstalledWebFile(webFiles.get(i), pluginId);
        }
    }


    private void registerInstalledWebFile(String webfile, String pluginId) 
    {
        if (webfile.length() > 0) {
            String id_list = installedWebFiles.get(webfile);
            if (id_list == null) {
                id_list = pluginId;
            } else {
                id_list += "," + pluginId;
            }
            installedWebFiles.put(webfile, id_list);
        }
    }


    private File getUninstallLogFile(String pluginId)
    {
        File logDir = getPluginWebLogDir();
        String logFilename = "uninstall_" + pluginId + ".log";
        return new File(logDir, logFilename);
    }


    private File getPluginWebLogDir()
    {
        File webInfDir = new File(webAppDir, "WEB-INF");
        return new File(webInfDir, "plugin_logs");
    }


    private void writeWebUninstallLog(String pluginId, List<String> fileList) throws Exception
    {
        File log_file = getUninstallLogFile(pluginId);
        File log_dir = log_file.getParentFile();
        if (! log_dir.exists()) log_dir.mkdirs();
        FileOutputStream log_out = new FileOutputStream(log_file);
        try {
            OutputStreamWriter log_writer = new OutputStreamWriter(log_out, "UTF-8");
            for (int i=0; i < fileList.size(); i++) {
                log_writer.write(fileList.get(i) + "\n");
            }
            log_writer.flush();
        } finally {
            try { log_out.close(); } catch (Exception ex) {}
        }
    }

    private Set<String> getKeepFilesOnReinstall(Properties pluginProps) 
    {
        Set<String> fileset = new TreeSet<String>();
        String val = pluginProps.getProperty(PLUGIN_PROP_KEEP_FILES, "").trim();
        if (val.length() > 0) {
            StringTokenizer st = new StringTokenizer(val, ", ", false);
            while (st.hasMoreTokens()) fileset.add(st.nextToken());
        }
        return fileset;
    }

    private static boolean isValidPluginId(String id)
    {
        return id.matches(REGEXP_PLUGIN_ID);
    }
    
    private static String systemPath(String path) 
    {
        if (File.separatorChar != '/') {
            return path.replace('/', File.separatorChar);
        } else {
            return path;
        }
    }

    private boolean webFileUsedByOther(String webpath, String pluginId) 
    {
        boolean used_by_other = false;
        String id_list = installedWebFiles.get(webpath);
        if (id_list == null) {
            return false;
        }
        StringTokenizer st = new StringTokenizer(id_list, ",", false);
        while (st.hasMoreTokens()) {
            if (! pluginId.equals(st.nextToken())) {
                used_by_other = true;
                break;
            }
        }
        return used_by_other;
    }
    
    private String readUTF8StreamToString(InputStream in)
    {
        try {
            InputStreamReader reader = new InputStreamReader(in, "UTF-8");
            StringBuilder outbuf = new StringBuilder();
            char[] buf = new char[8 * 1024];
            int cnt;
            while ((cnt = reader.read(buf)) >= 0) {
                outbuf.append(buf, 0, cnt);
            }
            return outbuf.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    private static boolean recursiveFileDelete(File f, String rel_path, Set<String> keepFiles)
    {
        if (! f.isAbsolute()) {
            throw new RuntimeException("PluginManager: Recursive file deletion is not allowed for relative path names: " + f);
        }
        boolean del_okay = true;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (! rel_path.endsWith("/")) {
                rel_path += "/";
            }
            for (int i=0; i < children.length; i++) {
                File child = children[i];
                if (! recursiveFileDelete(child, rel_path + child.getName(), keepFiles)) { 
                    del_okay = false;
                }
            }
            // delete directory if no file within has been kept
            if (del_okay) {  // if true, directory should be empty
                del_okay = f.delete();
            }
        } else {
            // delete regular file if not listed in keepFiles 
            String rel_path2 = rel_path.startsWith("/") ? rel_path.substring(1) : 
                                                          ("/" + rel_path);
            boolean do_keep = (keepFiles != null) && 
                              (keepFiles.contains(rel_path) || keepFiles.contains(rel_path2));
            if (! do_keep) {
                del_okay = f.delete();
            } else {
                del_okay = false;
            }
        }

        return del_okay;  // true if file/directory has been deleted, otherwise false
    }


}
