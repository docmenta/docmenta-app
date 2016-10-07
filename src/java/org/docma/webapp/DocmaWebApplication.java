/*
 * DocmaWebApplication.java
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

import java.io.*;
import java.util.*;
import javax.servlet.ServletContext;
import org.docma.app.DocmaConstants;
import org.docma.app.DocmaApplication;
import org.docma.app.DocmaSession;
import org.docma.app.ContentLanguages;
import org.docma.app.DocmaCharEntity;
import org.docma.app.DocmaLanguage;
import org.docma.coreapi.DocmaI18;
import org.docma.coreapi.DocException;
import org.docma.coreapi.DocStoreManager;
import org.docma.userapi.UserManager;
import org.docma.coreapi.ApplicationProperties;
import org.docma.coreapi.PublicationArchivesFactory;
import org.docma.plugin.implementation.AppsLoader;
import org.docma.plugin.implementation.PluginManager;
import org.docma.plugin.implementation.WebContext;
import org.docma.plugin.implementation.WebContextImpl;
import org.docma.plugin.internals.WebAppPlugInterface;
import org.docma.plugin.web.WebUserSession;
import org.docma.plugin.web.ContentAppHandler;
import org.docma.util.Log;

/**
 *
 * @author MP
 */
public class DocmaWebApplication extends DocmaApplication implements WebAppPlugInterface
{
    private static final String THEME_FILE_PREFIX = "docma_theme_";

    private final ServletContext servletCtx;
    private final File webAppDir;
    private final AppsLoader appsLoader;
    private final ExtAssignments editorAssignments;
    private final ExtAssignments viewerAssignments;
    // private File editorDir;
    // private String[] editorIds = null;
    private Map<String, DocmaWebSession> openSessions = new HashMap<String, DocmaWebSession>();
    private String[] themeIds = null;
    private Map<String, Properties> themeMap = new HashMap<String, Properties>();
    private PluginManager pluginManager = null;
    private boolean pluginRestartNotification = false;

    public DocmaWebApplication(ServletContext servletCtx,
                               String webAppPath,
                               DocStoreManager dsm, 
                               UserManager um,
                               DocmaI18 i18,
                               ContentLanguages contentLangs,
                               ApplicationProperties appProps,
                               PublicationArchivesFactory pubArchivesFactory) throws DocException
    {
        super(dsm, um, i18, contentLangs, appProps, pubArchivesFactory);
        this.servletCtx = servletCtx;
        this.webAppDir = new File(webAppPath);
        this.appsLoader = new AppsLoader(this.webAppDir);
        this.editorAssignments = new ExtAssignments(servletCtx);
        this.viewerAssignments = new ExtAssignments(servletCtx);
        // editorDir = new File(webAppPath, "tinymce_editor" + File.separator + "jscripts");
        // updateEditorIds(); // editorIds = editorDir.list();
        // Arrays.sort(editorIds);
        updateThemeIds();

        // Create PluginManager
        File baseDir = new File(appProps.getProperty(DocmaConstants.PROP_BASE_PATH));
        File pluginsDir = new File(baseDir, "plugins");
        if (! pluginsDir.exists()) {
            pluginsDir.mkdir();
        }
        try {
            WebContext webCtx = new WebContextImpl(this);
            pluginManager = new PluginManager(pluginsDir, this.applicationCtx, webCtx);
        } catch (Exception ex) {
            throw new DocException(ex);
        }
    }

    /**
     * Get the DocmaWebApplication instance for the given servlet context.
     * This method can be used to retrieve the DocmaWebApplication instance
     * from within a servlet or JSP page. If a DocmaWebApplication instance 
     * does not yet exist, then this method creates an instance based on the
     * configuration given in the passed servlet context. Note that only a 
     * single instance of DocmaWebApplication is allowed to exist within a Java 
     * VM (i.e. this method always returns the same instance).
     * 
     * @param servletCtx The servlet context.
     * @return 
     */
    public static synchronized DocmaWebApplication getInstance(ServletContext servletCtx) throws Exception
    {
        DocmaWebApplication app = GUIUtil.getDocmaWebApplication(servletCtx);
        if (app == null) {
            AppConfigurator appconf = new AppConfigurator();
            appconf.initWebApplication(servletCtx);
            app = appconf.getWebApplication();
            GUIUtil.setDocmaWebApplication(servletCtx, app);
        }
        return app;
    }

    @Override
    public DocmaSession connect(String user, String password) throws Exception
    {
        DocmaSession docmaSess = super.connect(user, password);
        DocmaWebSession webSess = new DocmaWebSession(this, docmaSess);
        openSessions.put(docmaSess.getSessionId(), webSess);
        return docmaSess;
    }

    public DocmaWebSession getWebSessionContext(String sessId)
    {
        return openSessions.get(sessId);
    }

    public WebUserSession getWebSessionPluginInterface(String sessId)
    {
        DocmaWebSession ws = getWebSessionContext(sessId);
        return (ws == null) ? null : ws.getPluginInterface();
    }

    public String getWebAppDirectory() {
        return webAppDir.getAbsolutePath();
    }

    public DocmaLanguage[] getGUILanguages()
    {
        File webInfDir = new File(webAppDir, "WEB-INF");
        List list = new ArrayList();
        DocmaLanguage lang = new DocmaLanguage("en", "English");
        list.add(lang);
        
        Locale current = GUIUtil.getCurrentLocale();
        String current_lang_code = (current != null) ? current.getLanguage() : null;
        if (current_lang_code != null) {
            current_lang_code = current_lang_code.toLowerCase();
        }
        boolean current_exists = "en".equals(current_lang_code);
        
        ContentLanguages supportedLangs = getContentLanguages();
        String[] fnames = webInfDir.list();
        for (String fn : fnames) {
            final String PREFIX = "i3-label_";
            if (fn.startsWith(PREFIX)) {
                int idx = PREFIX.length();
                int idx_end = fn.lastIndexOf('.');
                if (idx_end < idx) {
                    idx_end = fn.length();
                }
                String lang_code = fn.substring(idx, idx_end).toLowerCase();
                if (lang_code.equals(current_lang_code)) {
                    current_exists = true;
                }
                lang = supportedLangs.getLanguage(lang_code);
                if (lang == null) {
                    lang = new DocmaLanguage(lang_code, "");
                }
                list.add(lang);
            }
        }
        if ((current_lang_code != null) &&  !current_exists) {
            // Add the current language even if it is not supported by the 
            // web application.
            // This helps to determine which language is currently used by 
            // the GUI framework to load labels. 
            lang = new DocmaLanguage(current_lang_code, "");
            list.add(lang);
        }
        DocmaLanguage[] arr = new DocmaLanguage[list.size()];
        return (DocmaLanguage[]) list.toArray(arr);
    }

    public DocmaLanguage getDefaultGUILanguage()
    {
        return getContentLanguages().getLanguage("en");
    }

//    public void updateEditorIds()
//    {
//        editorIds = editorDir.list();
//    }
//    
//    public String[] getEditorIds()
//    {
//        return editorIds;
//    }
    
    public String[] getEditorIds(String... exts)
    {
        return appsLoader.listEditors(exts);
    }
    
    public String[] getViewerIds(String... exts)
    {
        return appsLoader.listViewers(exts);
    }
    
    /**
     * Returns the editor ID to be used for content editing. If  no editor has  
     * been assigned for content editing, then this method returns the same ID  
     * as getSystemDefaultContentEditor(). Note that this method does not 
     * consider user specific editor assignments.
     * @return The editor ID to be used for content editing.
     */
    public String getContentEditorId()
    {
        String app_id = getEditorAssignments().getAssignedApplication("content");
        return (app_id == null) ? getSystemDefaultContentEditor() : app_id;
    }
    
    public String[] getContentEditorIds()
    {
        return appsLoader.listEditors("content");
    }
    
    public String getSystemDefaultContentEditor()
    {
        return GUIConstants.CONTENT_EDITOR_DEFAULT_ID;
    }
    
    public String getSystemDefaultCSSEditor()
    {
        return GUIConstants.CONTENT_EDITOR_DEFAULT_ID;
    }
    
    public String getSystemDefaultTextEditor()
    {
        return GUIConstants.TEXT_EDITOR_DEFAULT_ID;
    }
    
    public ContentAppHandler getContentAppHandler(String app_id)
    {
        return appsLoader.getContentAppHandler(app_id);
    }
    
    public String getHelperAppName(String app_id)
    {
        String app_name = appsLoader.getApplicationName(app_id);
        return (app_name == null) ? app_id : app_name;
    }

    public void updateThemeIds()
    {
        File webInfDir = new File(webAppDir, "WEB-INF");
        ArrayList<String> idlist = new ArrayList<String>();
        String[] fnames = webInfDir.list();
        for (String fn : fnames) {
            if (fn.startsWith(THEME_FILE_PREFIX) && fn.endsWith(".properties")) {
                int startpos = THEME_FILE_PREFIX.length();
                String theme_id = fn.substring(startpos, fn.lastIndexOf('.'));
                idlist.add(theme_id);
            }
        }
        themeIds = new String[idlist.size()];
        themeIds = idlist.toArray(themeIds);
        Arrays.sort(themeIds);
    }
    
    public String[] getThemeIds()
    {
        return themeIds;
    }
    
    public String getDefaultTheme()
    {
        ApplicationProperties appProps = getApplicationProperties();
        String def_theme = appProps.getProperty(GUIConstants.PROP_APP_THEME);
        
        // Return initial default theme, if default theme was not changed by the user
        if ((def_theme == null) || (def_theme.length() == 0)) {
            return GUIConstants.APP_THEME_DEFAULT_ID;  // initial default theme
        }
        
        // If new application version has been installed, reset to initial default theme
        String version_rel = appProps.getProperty(GUIConstants.PROP_APP_THEME_VERSION_RELATION);
        if ((version_rel != null) && version_rel.startsWith(DocmaConstants.DISPLAY_APP_SHORTVERSION)) {
            return def_theme;  // return theme set by the user, if still the same major/minor version
        } else {
            return GUIConstants.APP_THEME_DEFAULT_ID;  // reset to initial default theme
        }
    }
    
    public String getThemeProperty(String theme_id, String propName)
    {
        Properties props = themeMap.get(theme_id);
        if (props == null) {
            props = loadThemeProperties(theme_id);
            themeMap.put(theme_id, props);
        }
        return props.getProperty(propName, "");
    }
    
    public ExtAssignments getEditorAssignments()
    {
        return editorAssignments;
    }

    public ExtAssignments getViewerAssignments()
    {
        return viewerAssignments;
    }
    
    public void saveEditorAssignments() throws Exception
    {
        String assign_str = getEditorAssignments().writeAssignmentsToString();
        ApplicationProperties appProps = getApplicationProperties();
        appProps.setProperty(GUIConstants.PROP_APP_ASSIGNMENT_EDITORS, assign_str);
    }

    public void saveViewerAssignments() throws Exception
    {
        String assign_str = getViewerAssignments().writeAssignmentsToString();
        ApplicationProperties appProps = getApplicationProperties();
        appProps.setProperty(GUIConstants.PROP_APP_ASSIGNMENT_VIEWERS, assign_str);
    }

    @Override
    protected ApplicationProperties getApplicationProperties()
    {
        return super.getApplicationProperties();
    }
    
    @Override
    protected synchronized void setCharEntities(DocmaCharEntity[] entities)
    {
        super.setCharEntities(entities);
        appsLoader.setCharEntities(entities);
    }

    /* --------------  Package local methods  ---------------------- */

    void releaseSession(String sessId)
    {
        openSessions.remove(sessId);
    }

    PluginManager getPluginManager()
    {
        return pluginManager;
    }
    
    void startUp()
    {
        pluginManager.startUpPlugins();
        pluginRestartNotification = pluginManager.isServerRestartRecommendedToFixError();
        
        // On server startup, the character entities that are 
        // stored in the application properties have to be written to 
        // the editor's configuration files. This is necessary because
        // when a new version of Docmenta is installed, then the editor 
        // configuration files are overwritten with the installation defaults.
        appsLoader.setCharEntities(getCharEntities());  // pass char entities to apps loader
        appsLoader.loadApps();
        initHelperAppsAssignment();
    }
    
    boolean hasPluginRestartNotification(boolean clearNotification)
    {
        if (pluginRestartNotification) {
            if (clearNotification) pluginRestartNotification = false;
            return true;
        }
        return false;
    }

    /* --------------  Private methods  ---------------------- */

    private void initHelperAppsAssignment()
    {
        // Explicitely set assignment to null for all supported file extensions.
        // This is required, because otherwise the file extension would not be 
        // listed in the GUI editor assignment list.
        String[] txt_exts = getTextFileExtensions();
        for (String ext : txt_exts) {
            editorAssignments.setAssignment(ext, null);   // set to no assignment (use default editor)
            viewerAssignments.setAssignment(ext, null);   // set to no assignment (use default viewer)
            // Note: this assignment may be overwritten by readAssignmentsFromString() below.
        }
        String[] edit_exts = appsLoader.listSupportedEditExtensions();
        for (String ext : edit_exts) {
            editorAssignments.setAssignment(ext, null);   // set to no assignment
            // Note: this assignment may be overwritten by readAssignmentsFromString() below.
        }
        String[] view_exts = appsLoader.listSupportedViewExtensions();
        for (String ext : view_exts) {
            viewerAssignments.setAssignment(ext, null);   // set to no assignment
            // Note: this assignment may be overwritten by readAssignmentsFromString() below.
        }

        // Read configured file extension assignments.        
        ApplicationProperties appProps = getApplicationProperties();
        String ea = appProps.getProperty(GUIConstants.PROP_APP_ASSIGNMENT_EDITORS);
        if (ea != null) {
            try {
                editorAssignments.readAssignmentsFromString(ea);
            } catch (Exception ex) {
                if (DocmaConstants.DEBUG) {
                    ex.printStackTrace();
                }
                Log.error("Could not read editor assignments from property value: " + ea);
            }
        }
        String va = appProps.getProperty(GUIConstants.PROP_APP_ASSIGNMENT_VIEWERS);
        if (va != null) {
            try {
                viewerAssignments.readAssignmentsFromString(va);
            } catch (Exception ex) {
                if (DocmaConstants.DEBUG) {
                    ex.printStackTrace();
                }
                Log.error("Could not read viewer assignments from property value: " + va);
            }
        }
    }
    
    private Properties loadThemeProperties(String themeId)
    {
        InputStream fin = null;
        Properties props = new Properties();
        File webInfDir = new File(webAppDir, "WEB-INF");
        File propFile = new File(webInfDir, THEME_FILE_PREFIX + themeId + ".properties");
        try {
            fin = new FileInputStream(propFile);
            props.load(fin);
        } catch (Exception ex) {
            Log.error("Could not load properties file: " + propFile);
        } finally {
            if (fin != null) {
                try { fin.close(); } catch (Exception ex) {}
            }
        }
        return props;
    }


}
