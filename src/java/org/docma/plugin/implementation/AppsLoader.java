/*
 * AppsLoader.java
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
package org.docma.plugin.implementation;

import java.io.*;
import java.util.*;

import org.docma.app.DocmaConstants;
import org.docma.coreapi.DocI18n;
import org.docma.util.Log;
import org.docma.plugin.CharEntity;
import org.docma.plugin.web.*;
import org.docma.plugin.tinymce.OldTinymceHandler;
import org.zkoss.util.resource.Labels;

/**
 *
 * @author MP
 */
public class AppsLoader 
{
    private static final String PLUGIN_APPS_PATH = "apps";
    private static final String OLD_TINYMCE_PATH = "tinymce_editor" + File.separator + "jscripts";
    private static final String HANDLER_PROPERTIES_FILENAME = "apphandler.properties";
    private static final String PROP_HANDLER_CLASS = "handler_class";
    
    private final File webAppsDir;
    private final File pluginAppsDir;
    private final DocI18n i18n;
    
    // Maps application id to handler instance.
    private final SortedMap<String, ContentAppHandler> loadedApps = new TreeMap<String, ContentAppHandler>();
    // Maps file extension to set of editor-ids that support this extension.
    private final SortedMap<String, SortedSet<String>> editorsMap = new TreeMap<String, SortedSet<String>>();
    // Maps file extension to set of viewer-ids that support this extension.
    private final SortedMap<String, SortedSet<String>> viewersMap = new TreeMap<String, SortedSet<String>>();

    // Lists all files in plugin directory. Maps filename to application id.  
    // Maps to empty string if filename is no valid application directory. 
    private Map<String, String> pluginDirMap = new HashMap<String, String>();
    // Lists all loaded TinyMCE folders in tinymce_editor/jscripts/ directory (old location).
    // TinyMCE editors at old location are still supported for backwards compatibility.
    private Map<String, String> oldTinyMap = new HashMap<String, String>();
    
    private long updateTimestamp = 0;

    private CharEntity[] charEntities = null;
    
    // All application directories from which the labels have already been loaded.
    // If an application is removed and added again, this set allows to detect
    // that labels have already been loaded and need to be refreshed.
    private final Set<String> loadedLabels = new HashSet<String>();

    public AppsLoader(File webAppsDir, DocI18n i18n)
    {
        this.webAppsDir = webAppsDir;
        this.pluginAppsDir = new File(webAppsDir, PLUGIN_APPS_PATH);
        this.i18n = i18n;
    }
    
    public String getApplicationName(String app_id)
    {
        ContentAppHandler app = loadedApps.get(app_id);
        if (app == null) {
            return app_id; 
        } else { 
            Locale loc = i18n.getCurrentLocale();
            return app.getApplicationName(loc.getLanguage()); 
        }
    }
    
    public ContentAppHandler getContentAppHandler(String app_id)
    {
        return loadedApps.get(app_id);
    }
    
    public synchronized String[] listSupportedEditExtensions() 
    {
        checkUpdate();
        Set<String> exts = editorsMap.keySet();
        return exts.toArray(new String[exts.size()]);
    }
    
    public synchronized String[] listSupportedViewExtensions() 
    {
        checkUpdate();
        Set<String> exts = viewersMap.keySet();
        return exts.toArray(new String[exts.size()]);
    }
    
    public synchronized String[] listEditors(String... exts) 
    {
        checkUpdate();
        if (exts.length == 1) {
            String ext = DefaultContentAppHandler.normalizeExt(exts[0]);
            SortedSet<String> ids = editorsMap.get(ext);
            return (ids == null) ? new String[0] : ids.toArray(new String[ids.size()]);
        } else {
            SortedSet<String> res = new TreeSet<String>();
            for (String e : exts) {
                String ext = DefaultContentAppHandler.normalizeExt(e);
                SortedSet<String> app_ids = editorsMap.get(ext);
                if (app_ids != null) {
                    res.addAll(app_ids);
                }
            }
            return res.toArray(new String[res.size()]);
        }
    }

    public synchronized String[] listViewers(String... exts) 
    {
        checkUpdate();
        if (exts.length == 1) {
            String ext = DefaultContentAppHandler.normalizeExt(exts[0]);
            SortedSet<String> ids = viewersMap.get(ext);
            return (ids == null) ? new String[0] : ids.toArray(new String[ids.size()]);
        } else {
            SortedSet<String> res = new TreeSet<String>();
            for (String e : exts) {
                String ext = DefaultContentAppHandler.normalizeExt(e);
                SortedSet<String> app_ids = viewersMap.get(ext);
                if (app_ids != null) {
                    res.addAll(app_ids);
                }
            }
            return res.toArray(new String[res.size()]);
        }
    }

    /**
     * Load all helper applications. This method is called once on startup of
     * the web-application.
     */
    public synchronized void loadApps()
    {
        if (DocmaConstants.DEBUG) {
            Log.info("Loading Apps...");
        }
        loadedApps.clear();
        editorsMap.clear();
        viewersMap.clear();
        pluginDirMap.clear();
        updateApps(System.currentTimeMillis());
        if (DocmaConstants.DEBUG) {
            Log.info("Loading Apps finished.");
        }
    }
    
    public synchronized void setCharEntities(CharEntity[] entities)
    {
        this.charEntities = entities;
        for (ContentAppHandler h : loadedApps.values()) {
            setCharEntities(h, entities);
        }
    }
    
    /* --------------  Private methods  ---------------------- */

    private void setCharEntities(ContentAppHandler h, CharEntity[] entities)
    {
        if ((entities != null) && (entities.length > 0)) {
            try {
                h.setCharEntities(entities);
            } catch (Exception ex) {
                Log.error("Failed to set character entities for handler '" + h.getApplicationId() + "'.");
                if (DocmaConstants.DEBUG) {
                    ex.printStackTrace();
                }
            }
        }
    }
    
    private void checkUpdate()
    {
        long now = System.currentTimeMillis();
        if ((now - updateTimestamp) < 2000) {
            return; // do not check for updates if last update was within last 2 seconds
        }
        updateApps(now);
    }
    
    private void updateApps(long now)
    {
        File[] files = pluginAppsDir.listFiles();
        if (files == null) {
            files = new File[0];
        }
        Map<String, String> currentMap = new HashMap<String, String>();

        // Load new apps (check if new directories have been added since last update)
        for (File f : files) {
            String fn = f.getName();
            String app_id = pluginDirMap.get(fn);
            if ((app_id == null) && f.isDirectory()) {   // new directory
                app_id = loadApp(PLUGIN_APPS_PATH + File.separator + fn);
            }
            // Map fn to application id.
            // If fn is no valid application directory then map to empty string.
            currentMap.put(fn, (app_id == null) ? "" : app_id);
        }

        // Remove apps that no longer exist
        for (String fn : pluginDirMap.keySet()) {
            if (! currentMap.containsKey(fn)) {  // directory has been removed
                String loaded_app = pluginDirMap.get(fn);
                if (! "".equals(loaded_app)) {  // fn was a valid application directory
                    removeApp(loaded_app);
                }
            }
        }
        
        pluginDirMap = currentMap;  // Replace old listing by updated listing. 
        
        updateOldTinyApps();   // Load TinyMCE editors from old directory for backwards compatibility.
        
        updateTimestamp = now;
    }
    
    private String loadApp(String relativePath)
    {
        File appDir = new File(webAppsDir, relativePath);
        try {
            Log.info("Loading App " + relativePath);
            File propfile = new File(appDir, HANDLER_PROPERTIES_FILENAME);
            if (! propfile.exists()) {  // property file missing -> no valid app directory
                return null;
            }
            Properties props = loadProps(propfile);
            String cls_name = props.getProperty(PROP_HANDLER_CLASS, "");
            ContentAppHandler handler;
            if (cls_name.trim().equals("")) {
                handler = new DefaultContentAppHandler();
            } else {
                Class handler_cls = Class.forName(cls_name);
                handler = (ContentAppHandler) handler_cls.newInstance();
            }
            handler.initialize(webAppsDir, relativePath, props);
            String app_id = handler.getApplicationId();
            loadedApps.put(app_id, handler);
            
            addExtensionsToMap(app_id, handler.getSupportedEditExtensions(), editorsMap);
            addExtensionsToMap(app_id, handler.getSupportedViewExtensions(), viewersMap);
            
            setCharEntities(handler, charEntities);
            
            loadLabels(appDir);
            
            return app_id;
        } catch (Exception ex) {
            Log.error("Failed to load app '" + appDir.getAbsolutePath() + "': " + ex.getMessage());
            if (DocmaConstants.DEBUG) {
                ex.printStackTrace();
            }
            return null;
        }
    }
    
    private void removeApp(String app_id)
    {
        try {
            Log.info("Removing App '" + app_id + "'");
            loadedApps.remove(app_id);
            removeApp(app_id, editorsMap);
            removeApp(app_id, viewersMap);
        } catch (Exception ex) {
            Log.error("Failed to remove app '" + app_id + "': " + ex.getMessage());
            if (DocmaConstants.DEBUG) {
                ex.printStackTrace();
            }
        }
    }
    
    private void removeApp(String app_id, SortedMap<String, SortedSet<String>> extMap)
    {
        Iterator<String> it = extMap.keySet().iterator();
        while (it.hasNext()) {
            String ext = it.next();
            SortedSet<String> apps = extMap.get(ext);
            if (apps != null) {
                apps.remove(app_id);
                if (apps.isEmpty()) {
                    it.remove();
                }
            }
        }
    }

    private synchronized void loadLabels(File appDir)
    {
        String app_path = appDir.getAbsolutePath();
        if (loadedLabels.contains(app_path)) {
            // The application was removed and is now added again.
            Labels.reset();  // reload labels
        } else {
            Labels.register(new PluginLabelLocator(appDir, PluginManager.LABEL_PROPS_FILE_PATTERN));
            loadedLabels.add(app_path);
        }
    }

    private void addExtensionsToMap(String app_id, String[] exts, Map<String, SortedSet<String>> extmap)
    {
        if (exts != null) {
            for (String ext : exts) {
                SortedSet<String> app_set = extmap.get(ext);
                if (app_set == null) {
                    app_set = new TreeSet<String>();
                    extmap.put(ext, app_set);
                }
                app_set.add(app_id);
            }
        }
    }
    
    private Properties loadProps(File propfile) throws IOException
    {
        Properties props = new Properties();
        InputStream pin = new FileInputStream(propfile);
        try {
            props.load(pin);
        } finally {
            try { pin.close(); } catch (Exception ex) {}
        }
        return props;
    }

    
    private void updateOldTinyApps()
    {
        File tinyDir = new File(webAppsDir, OLD_TINYMCE_PATH);
        if (! tinyDir.exists()) {
            return;
        }
        
        File[] files = tinyDir.listFiles();
        if (files == null) {
            files = new File[0];
        }
        Map<String, String> currentMap = new HashMap<String, String>();

        // Load new apps (check if new directories have been added since last update)
        for (File f : files) {
            String fn = f.getName();
            if (fn.startsWith("tinymce") && f.isDirectory()) {
                String app_id = oldTinyMap.get(fn);
                if (app_id == null) {   // new directory
                    app_id = loadOldTinymce(fn);
                }
                // Map fn to application id.
                // If fn is no valid application directory then map to empty string.
                currentMap.put(fn, (app_id == null) ? "" : app_id);
            }
        }

        // Remove apps that no longer exist
        for (String fn : oldTinyMap.keySet()) {
            if (! currentMap.containsKey(fn)) {  // directory has been removed
                String loaded_app = oldTinyMap.get(fn);
                if (! "".equals(loaded_app)) {  // fn was a valid application directory
                    removeApp(loaded_app);
                }
            }
        }
        
        oldTinyMap = currentMap;  // Replace old listing by updated listing. 
    }

    /**
     * Load old TinyMCE plugins for backwards compatibility.
     */    
    private String loadOldTinymce(String fn)
    {
        String app_path = OLD_TINYMCE_PATH + File.separator + fn;
        try {
            Log.info("Loading Tinymce: " + app_path);
            ContentAppHandler handler = new OldTinymceHandler();
            handler.initialize(webAppsDir, app_path, null);
            String app_id = handler.getApplicationId();
            if (! loadedApps.containsKey(app_id)) {
                loadedApps.put(app_id, handler);
                addExtensionsToMap(app_id, handler.getSupportedEditExtensions(), editorsMap);
                // addExtensionsToMap(app_id, handler.getSupportedViewExtensions(), viewersMap);
                setCharEntities(handler, charEntities);
                return app_id;
            }
        } catch (Exception ex) {
            Log.error("Failed to load '" + app_path + "': " + ex.getMessage());
            if (DocmaConstants.DEBUG) {
                ex.printStackTrace();
            }
        }
        return null;   // not loaded
    }

}
