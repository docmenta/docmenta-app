/*
 * DocmaSession.java
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

package org.docma.app;

import java.util.*;
import java.io.*;

import org.docma.coreapi.*;
import org.docma.coreapi.fsimplementation.DocStoreSessionImpl;
import org.docma.coreapi.fsimplementation.FilesystemStoreProperties;
import org.docma.util.*;
import org.docma.userapi.UserManager;
import org.docma.lockapi.LockListener;
import org.docma.plugin.ApplicationContext;
import org.docma.plugin.StoreConnection;
import org.docma.plugin.UserSession;
import org.docma.plugin.implementation.StoreConnectionImpl;
import org.docma.plugin.implementation.UserSessionImpl;
import org.docma.app.fsimplementation.PublicationArchivesSessImpl;

/**
 *
 * @author MP
 */
public class DocmaSession
{
    static final String ROOT_ALIAS = "root";
    static final String DOC_ROOT_ALIAS = "document_root";
    static final String SYS_ROOT_ALIAS = "system_root";
    static final String MEDIA_ROOT_ALIAS = "media_root";
    static final String FILE_ROOT_ALIAS = "file_root";
    static final String TEMPLATES_ALIAS = "system_templates";

    private final DocmaApplication docmaApp;
    private final DocStoreSession  docSess;
    private final Map              nodeMap = new HashMap(8000);
    private Map              accessRights = null;
    private Set              openedStoreRights = null;

    private boolean isUISession = false;
    private Map<String, DocmaSession> childSessions = null;
    private DocmaSession parentSession = null;
    
    private UserSessionImpl pluginUserSession = null;
    private StoreConnectionImpl pluginStoreConnection = null;

    private Properties genTextProps = null;
    private Date       genTextPropsDate = null;

    // entity maps used in methods decodeCharEntities(), toCharEntity() and encodeCharEntities()
    private Map charToEntityMap = null;
    private Map entityToCharMap = null;

    private PublicationManager   publicationManager = null;
    private PublicationArchivesSession publicationArchives = null;
    private RevisionStoreSession revisionStore = null;
    private DocmaSessionListener sessionListener = null;

    private boolean local_transaction = false;
    private int openedStoreCounter = 0;

    DocmaNode root = null;
    DocmaNode documentRoot = null;
    DocmaNode systemRoot = null;


    /* ------------  Constructor and initialization  ---------------- */

    DocmaSession(DocmaApplication docmaApp, DocStoreSession docSess)
    {
        this.docmaApp = docmaApp;
        this.docSess = docSess;
    }

    /**
     * Initializes the UI session.
     * If this session is the user's UI session, then this method has to be
     * called after this instance has been created.
     * 
     * @param userSess  the plugin facade of the UI user session 
     */
    public void initUISession(UserSessionImpl userSess)
    {
        this.isUISession = true;
        this.pluginUserSession = userSess;
    }
    
    /* --------------  private methods  ---------------------- */

    private DocmaNode addToNodeMap(DocNode docNode) {
        if (docNode == null) return null;
        DocmaNode n = new DocmaNode(this, docNode);
        nodeMap.put(n.getId(), n);
        return n;
    }

    private DocmaNode getFromNodeMap(String id) {
        return (DocmaNode) nodeMap.get(id);
    }

    private void refreshAllNodes()
    {
        Iterator it = nodeMap.values().iterator();
        while (it.hasNext()) {
            DocmaNode node = (DocmaNode) it.next();
            node.refresh();
        }
    }

    private void loadAccessRights()
    {
        if (accessRights == null) {
            accessRights = new HashMap();
            UserManager um = getUserManager();
            String[] gids = um.getGroupsOfUser(getUserId());
            for (int i=0; i < gids.length; i++) {
                String str = um.getGroupProperty(gids[i], DocmaConstants.PROP_USERGROUP_RIGHTS);
                if (str != null) {
                    AccessRights[] arr = AccessRights.parseAccessRights(str);
                    for (int k=0; k < arr.length; k++) {
                        AccessRights ar = arr[k];
                        Set rset = (Set) accessRights.get(ar.getStoreId());
                        if (rset == null) {
                            rset = new HashSet();
                            accessRights.put(ar.getStoreId(), rset);
                        }
                        rset.addAll(Arrays.asList(ar.getRights()));
                    }
                }
            }
        }
    }

    private void initStoreRights()
    {
        loadAccessRights();  // load access rights if not already loaded
        openedStoreRights = new HashSet();
        String sid = getStoreId();
        if (sid != null) {
            Set rights1 = (Set) accessRights.get("*");
            if (rights1 != null) {
                openedStoreRights.addAll(rights1);
            }
            Set rights2 = (Set) accessRights.get(sid);
            if (rights2 != null) {
                openedStoreRights.addAll(rights2);
            }
        }
    }

    private boolean startLocalTransaction()
    {
        if (local_transaction) {
            return false;
            // throw new DocRuntimeException("Cannot start local transaction: local transaction already running.");
        } else {
            // if a transaction is not already running, then start
            // a "local" transaction
            if (runningTransaction()) {
                return false;
            } else {
                try {
                    startTransaction();
                    local_transaction = true;
                    return true;
                } catch (DocException dex) {
                    throw new DocRuntimeException(dex);
                }
            }
        }
    }

    private void commitLocalTransaction(boolean started)
    {
        if (! started) return;
        if (local_transaction) {
            try {
                commitTransaction();
                local_transaction = false;
            } catch (DocException dex) {
                throw new DocRuntimeException(dex);
            }
        } else {
            throw new DocRuntimeException("Cannot commit local transaction: no local transaction running.");
        }
    }

    private void rollbackLocalTransaction(boolean started)
    {
        if (! started) return;
        if (local_transaction) {  // rollback transaction if started locally
            try {
                rollbackTransaction();
            } finally {
                local_transaction = false;
            }
        } else {
            throw new DocRuntimeException("Cannot commit local transaction: no local transaction running.");
        }
    }

    private void createTemplateImages(DocmaNode templates_folder)
    {
        // Create image folder within templates folder
        DocmaNode img_folder = createImageFolder();
        img_folder.setTitle(getI18n().getLabel("label.templatesfolder.images.title"));
        templates_folder.insertChild(0, img_folder);
        
        String res_path = getApplicationProperty(DocmaConstants.PROP_DOCMA_RESOURCES_PATH);
        File img_dir = new File(res_path, "template_images");
        loadImageFolderFromDirectory(img_folder, img_dir);
    }

    private void createDefaultImages(DocmaNode sys_folder)
    {
        // This method should be called within a transaction

        // Create image folder within system folder
        DocmaNode img_folder = createImageFolder();
        img_folder.setTitle(getI18n().getLabel("label.sysimagefolder.title"));
        sys_folder.insertChild(0, img_folder);

        // Insert default images into image folder
        String res_path = getApplicationProperty(DocmaConstants.PROP_DOCMA_RESOURCES_PATH);
        File img_dir = new File(res_path, "sys_images");
        loadImageFolderFromDirectory(img_folder, img_dir);
    }

    private void loadImageFolderFromDirectory(DocmaNode img_folder, File img_dir)
    {
        File[] imgfiles = img_dir.listFiles();
        FileInputStream img_in = null;
        try {
            for (int i=0; i < imgfiles.length; i++) {
                File img = imgfiles[i];
                if (img.isHidden() || !img.isFile()) {
                    continue;
                }
                String fn = img.getName();
                int p = fn.lastIndexOf('.');
                if (p < 1) {
                    Log.error("Image file has missing extension: " + img);
                    continue;
                }
                String alias = fn.substring(0, p);
                String ext = fn.substring(p + 1);
                if (ImageUtil.isSupportedImageExtension(ext)) {
                    img_in = new FileInputStream(img);
                    DocmaNode img_node = createImageContent();
                    img_folder.addChild(img_node);
                    img_node.setImageContentStream(img_in, alias, null, ext);
                    img_in.close();
                    img_in = null;
                }
            }
            // if (DocmaConstants.DEBUG) DebugUtil.printChildren(img_folder);
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.error("Error during creation of default system image files: " + ex.getMessage());
            // Ignore this exception. Default image files will be missing.
        } finally {
            try {
                if (img_in != null) img_in.close();
            } catch (Exception ex2) {}
        }
    }

    private void createDefaultHtmlConfig(DocmaNode sys_folder)
    {
        // This method should be called within a transaction

        // Create file folder within system folder
        DocmaNode folder = createSystemFolder();
        folder.setTitle(getI18n().getLabel("label.htmlconfigfolder.title"));
        folder.setAlias(DocmaConstants.HTML_CONFIG_FOLDER_ALIAS_NAME);
        sys_folder.insertChild(0, folder);

        // Insert default files into folder
        String res_path = getApplicationProperty(DocmaConstants.PROP_DOCMA_RESOURCES_PATH);
        FileInputStream file_in = null;
        try {
            File dir = new File(res_path, "html_config");
            File[] files = dir.listFiles();
            for (int i=0; i < files.length; i++) {
                File f = files[i];
                if (f.isFile() && !f.isHidden()) {
                    file_in = new FileInputStream(f);
                    DocmaNode node = createFileContent();
                    folder.addChild(node);
                    node.setFileContentStream(file_in, f.getName());
                    file_in.close();
                    file_in = null;
                }
            }

            // Create Web Help default files
            DocmaNode wh_folder = createSystemFolder();
            wh_folder.setTitle("webhelp");
            folder.addChild(wh_folder);

            File wh_dir = new File(dir, "webhelp");
            File[] wh_files = wh_dir.listFiles();
            for (int i=0; i < wh_files.length; i++) {
                File f = wh_files[i];
                if (f.isFile() && !f.isHidden()) {
                    String fn = f.getName();
                    int pos = fn.lastIndexOf('.');
                    String fext = (pos < 0) ? "" : fn.substring(pos + 1);
                    file_in = new FileInputStream(f);
                    boolean is_image = ImageUtil.isSupportedImageExtension(fext);
                    DocmaNode node = is_image ? createImageContent() : createFileContent();
                    wh_folder.addChild(node);
                    if (is_image) {
                        String img_alias = fn.substring(0, pos);
                        node.setImageContentStream(file_in, img_alias, null, fext);
                    } else {
                        node.setFileContentStream(file_in, fn);
                    }
                    file_in.close();
                    file_in = null;
                }
            }
            // if (DocmaConstants.DEBUG) DebugUtil.printChildren(folder);
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.error("Error during creation of default HTML customization files: " + ex.getMessage());
            // Ignore this exception. Default html files will be missing.
        } finally {
            try {
                if (file_in != null) file_in.close();
            } catch (Exception ex2) {}
        }
    }

    private void createDefaultGentext(String trans_mode, DocmaNode genfile_node)
    {
        FileInputStream gen_in = null;
        try {
            String conf_path = getApplicationProperty(DocmaConstants.PROP_DOCMA_GENTEXT_PATH);
            File conf_dir = new File(conf_path);

            String lang_code = (trans_mode != null) ? trans_mode : getOriginalLanguage().getCode();

            String gen_filename = "gentext_default_" + lang_code.toLowerCase() + ".properties";
            File gen_file = new File(conf_dir, gen_filename);
            if (! gen_file.exists()) gen_file = new File(conf_dir, "gentext_default_en.properties");
            if (! gen_file.exists()) {
                Log.warning("Default gentext file does not exist: " + gen_file.getAbsolutePath());
                Log.warning("Skipping default gentext file creation!");
                return;
            }

            gen_in = new FileInputStream(gen_file);
            startTransaction();
            if (trans_mode == null) {
                // Create default gentext file
                DocmaNode sys_folder = getSystemRoot();
                genfile_node = createFileContent();
                sys_folder.addChild(genfile_node);
                genfile_node.setAlias(DocmaConstants.GENTEXT_ALIAS_NAME);
                genfile_node.setFileContentStream(gen_in, DocmaConstants.GENTEXT_ALIAS_NAME + ".properties");
            } else {
                // Set default translated gentext file
                if (genfile_node != null) {
                    String fn = DocmaConstants.GENTEXT_ALIAS_NAME +
                                "[" + trans_mode.toUpperCase() + "].properties";
                    genfile_node.setFileContentStream(gen_in, fn);
                }
            }
            commitTransaction();
        } catch (Exception ex) {
            ex.printStackTrace();
            if (runningTransaction()) rollbackTransaction();
            Log.error("Could not create default gentext file: " + ex.getMessage());
        } finally {
            try {
                if (gen_in != null) gen_in.close();
            } catch (Exception ex2) {}
        }
    }

    private void initEntitiesMaps()
    {
        // Initialize entity maps charToEntityMap and entityToCharMap
        // used in methods decodeCharEntities(), toCharEntity() and encodeCharEntities()
        if ((charToEntityMap == null) || (entityToCharMap == null)) {
            DocmaCharEntity[] entities = getCharEntities();
            charToEntityMap = new HashMap(2 * entities.length);
            entityToCharMap = new HashMap(2 * entities.length);

            for (int i=0; i < entities.length; i++) {
                DocmaCharEntity ent = entities[i];
                int numeric = ent.getNumericValue();
                if (numeric >= 0) {
                    Integer char_int = new Integer(numeric);
                    charToEntityMap.put(char_int, ent);
                    entityToCharMap.put(ent.getNumeric(), char_int);
                    String symbolic = ent.getSymbolic();
                    if ((symbolic != null) && (symbolic.length() > 0)) {
                        entityToCharMap.put(symbolic, char_int);
                    }
                }
            }
        }
    }
    
    private void invalidatePublicationArchive(String storeId, DocVersionId verId)
    {
        try {
            PublicationArchivesSession pub_ar = getPublicationArchives();
            if (pub_ar != null) {
                if (verId == null) {
                    pub_ar.invalidateCache(storeId);
                } else {
                    pub_ar.invalidateCache(storeId, verId);
                }
            }
            if (publicationManager != null) {
                publicationManager.invalidateArchive(storeId, verId);
            }
        } catch (Exception ex) {
            Log.error("Exception in invalidatePublicationArchive: " + ex.getMessage());
        }
    }

    /* ------  package local methods: called from DocmaNode and helper classes  ------ */

    DocStoreSession getBackendSession()
    {
        return docSess;
    }

    /** Gets the corresponding DocmaNode for a persistent or transient DocNode */
    DocmaNode getDocmaNode(DocNode backendNode)
    {
        if (backendNode == null) return null;
        DocmaNode n = getFromNodeMap(backendNode.getId());
        if (n == null) {
            n = addToNodeMap(backendNode);
        }
        return n;
    }

    void removeDocmaNodeFromCache(String nodeId)
    {
        nodeMap.remove(nodeId);
    }

    FormattingEngine getFormatter() throws Exception
    {
        return docmaApp.getFormatter();
    }

    String getOpenedStoreMediaBaseURL() throws Exception
    {
        String base_URL = getOpenedStoreDir();
        if (base_URL == null) return null; // return null if no filesystem storage (e.g. database storage)

        base_URL = (new File(base_URL)).toURI().toURL().toString(); // base_URL = base_URL.replace('\\', '/');
        // if (! base_URL.endsWith("/")) base_URL += "/";
        return base_URL;
    }

    String getOpenedStoreDir()
    {
        if (docSess instanceof DocStoreSessionImpl) {  // if filesystem storage
            return ((DocStoreSessionImpl) docSess).getOpenedDocStoreDir();
        } else {
            return null; // return null if no filesystem storage (e.g. database storage)
        }
    }

    RevisionStoreSession getRevisionStore()
    {
        if (revisionStore == null) {
            RevisionStoreFactory revFactory = docmaApp.getRevisionStoreFactory();
            if (revFactory != null) {
                revisionStore = revFactory.createSession(docSess);
            }
        }
        return revisionStore;
    }
    
    PublicationManager getPublicationManager()
    {
        if (publicationManager == null) {
            publicationManager = new PublicationManager(this, getPublicationArchives());
        }
        return publicationManager;
    }

    PublicationArchivesSession getPublicationArchives()
    {
        if (publicationArchives == null) {
            publicationArchives =
                docmaApp.getPublicationArchivesFactory().createSession(docSess);
        }
        return publicationArchives;
    }

    int getOpenedStoreCounter()
    {
        return this.openedStoreCounter;
    }

    /* --------------  public methods  ---------------------- */

    public DocI18n getI18n()
    {
        return docmaApp.getI18n();
    }

    public String getSessionId()
    {
        return docSess.getSessionId();
    }
    
    /**
     * Create a new independent session owned by the same user as this session.
     * The lifetime of the new session is independent from the lifetime 
     * of this session.
     * 
     * @return the created session
     */
    public DocmaSession createNewSession()
    {
        try {
            DocmaSession newSess = docmaApp.connect(getUserId());
            newSess.accessRights = this.accessRights;  // avoid reloading of access rights
            return newSess;
        } catch (Exception ex) {
            throw new DocRuntimeException(ex);
        }
    }

    /**
     * Create a new child session owned by the same user as this session.
     * Closing this session causes all created child session to be closed  
     * as well.
     * 
     * @return the created session
     */
    public DocmaSession createChildSession()
    {
        if (childSessions == null) {
            childSessions = new HashMap<String, DocmaSession>();
        }
        DocmaSession newSess = createNewSession();
        newSess.parentSession = this;
        try {
            String newId = newSess.getSessionId();
            if (childSessions.containsKey(newId)) {
                throw new DocRuntimeException("ID of temporary session is not unique: " + newId);
            }
            childSessions.put(newId, newSess);
            return newSess;
        } catch (Exception ex) {
            try {
                newSess.closeSession();
            } catch (Exception ex2) {}
            throw new DocRuntimeException(ex);
        }
    }

    public UserSession getPluginUserSession()
    {
        if (pluginUserSession == null) {
            // pluginUserSession is null if the initUISession method has not 
            // been called. In other words, this is a daemon session (a session
            // that is not connected to the UI).
            if (parentSession != null) {  // this is a child session
                // In the plugin API, the plugin user session of child sessions 
                // (temporary store connections) is the user session from which 
                // the child session has been created (i.e. the parent session).
                pluginUserSession = (UserSessionImpl) parentSession.getPluginUserSession();
            } else {
                // If this session is a daemon session and also no child session 
                // of another session, then the plugin user session is 
                // this session itself.
                pluginUserSession = new UserSessionImpl(this);
            }
        }
        return pluginUserSession;
    }
    
    public StoreConnection getPluginStoreConnection()
    {
        if (pluginStoreConnection == null) {
            String sId = getStoreId();
            DocVersionId vId = getVersionId();
            if ((sId != null) && (vId != null)) {
                pluginStoreConnection = 
                  new StoreConnectionImpl(getPluginUserSession(), this, sId, vId, isUISession);
            }
        }
        return pluginStoreConnection;
    }

    public String getUserProperty(String name)
    {
        return getUserManager().getUserProperty(getUserId(), name);
    }

    public void setUserProperty(String name, String value)
    {
        try {
            getUserManager().setUserProperty(getUserId(), name, value);
        } catch (Exception ex) {
            throw new DocRuntimeException(ex);
        }
    }

    public void setUserProperties(String[] names, String[] values)
    {
        try {
            getUserManager().setUserProperties(getUserId(), names, values);
        } catch (Exception ex) {
            throw new DocRuntimeException(ex);
        }
    }

    public String getApplicationProperty(String name) {
        return docmaApp.getApplicationProperties().getProperty(name);
    }

    public void setApplicationProperty(String name, String value) throws DocException {
        docmaApp.getApplicationProperties().setProperty(name, value);
    }

    public void setApplicationProperties(String[] names, String[] values) throws DocException {
        docmaApp.getApplicationProperties().setProperties(names, values);
    }

    public String getDocStoreProperty(String storeId, String name) {
        return docSess.getDocStoreProperty(storeId, name);
    }

    public void setDocStoreProperty(String storeId, String name, String value) throws DocException {
        docSess.setDocStoreProperty(storeId, name, value);
    }

    public void setDocStoreProperties(String storeId, String[] names, String[] values) throws DocException {
        docSess.setDocStoreProperties(storeId, names, values);
    }

    public String getVersionProperty(String storeId, DocVersionId verId, String name)
    {
        return docSess.getVersionProperty(storeId, verId, name);
    }

    public void setVersionProperty(String storeId, DocVersionId verId, String name, String value) throws DocException
    {
        docSess.setVersionProperty(storeId, verId, name, value);
    }

    public void setVersionProperties(String storeId, DocVersionId verId, String[] names, String[] values) throws DocException
    {
        docSess.setVersionProperties(storeId, verId, names, values);
    }

    public ApplicationContext getApplicationContext()
    {
        return docmaApp.getApplicationContext();
    }

    public UserManager getUserManager()
    {
        return docmaApp.getUserManager();
    }
    
    public RulesManager getRulesManager()
    {
        return docmaApp.getRulesManager();
    }

    public DocmaCharEntity[] getCharEntities()
    {
        return docmaApp.getCharEntities();
    }

    public void setCharEntities(DocmaCharEntity[] entities)
    {
        docmaApp.setCharEntities(entities);
        // clear entity maps used in methods decodeEntities(), toCharEntity() and encodeEntities()
        charToEntityMap = null;
        entityToCharMap = null;
    }

    public String toCharEntity(char ch, boolean symbolic)
    {
        initEntitiesMaps();
        DocmaCharEntity ent = (DocmaCharEntity) charToEntityMap.get(new Integer((int) ch));
        if (ent == null) return null;
        if (symbolic) {
            String entity_str = ent.getSymbolic();
            if ((entity_str == null) || (entity_str.length() == 0)) {
                entity_str = ent.getNumeric();
            }
            return entity_str;
        } else {
            return ent.getNumeric();
        }
    }

    public String encodeCharEntities(String str, boolean symbolic)
    {
        return encodeCharEntities(str, symbolic, true);
    }

    public String encodeCharEntities(String str, boolean symbolic, boolean keepEntities)
    {
        initEntitiesMaps();
        int len = str.length();
        StringBuilder buf = new StringBuilder(len + (len / 2));
        for (int i=0; i < len; i++) {
            char ch = str.charAt(i);
            if (keepEntities && (ch == '&')) {
                int pos2 = str.indexOf(';', i);
                if (pos2 >= 0) {
                    String ent_str = str.substring(i, pos2 + 1);
                    Integer char_int = (Integer) entityToCharMap.get(ent_str);
                    if (char_int != null) {   // entity exists
                        buf.append(ent_str);  // keep entity
                        i = pos2;  // continue after entity
                        continue;
                    }
                }
            }
            String entity_str = toCharEntity(ch, symbolic);
            if ((entity_str != null) && (entity_str.length() > 0)) {
                buf.append(entity_str);
            } else {
                buf.append(ch);
            }
        }
        return buf.toString();
    }

    public String decodeCharEntities(String str)
    {
        initEntitiesMaps();
        int start_pos = 0;
        while (start_pos < str.length()) {
            int pos = str.indexOf('&', start_pos);
            if (pos < 0) break;

            int pos2 = str.indexOf(';', pos);
            if (pos2 >= 0) {
                pos2++;  // pos2 is now position after semicolon ';'
                String ent_str = str.substring(pos, pos2);
                Integer char_int = (Integer) entityToCharMap.get(ent_str);
                if (char_int != null) {
                    char ent_char = (char) char_int.intValue();
                    str = str.substring(0, pos) + ent_char + str.substring(pos2);
                }
            }
            start_pos = pos + 1;
        }
        return str;
    }


    public DocmaLanguage[] getSupportedContentLanguages()
    {
        return docmaApp.getContentLanguages().getSupportedLanguages();
    }

    public String getLanguageCode()
    {
        String trans_mode = getTranslationMode();
        if (trans_mode == null) {
            return getOriginalLanguage().getCode();
        } else {
            return trans_mode;
        }
    }

    public DocmaLanguage getLanguage()
    {
        String trans_mode = getTranslationMode();
        DocmaLanguage res;
        if (trans_mode == null) {
            res = getOriginalLanguage();
        } else {
            res = docmaApp.getContentLanguages().getLanguage(trans_mode);
            if (res == null) {  // this should never occur
                // Create language object to avoid NullPointerException
                res = new DocmaLanguage(trans_mode, trans_mode);
            }
        }
        return res;
    }
    
    public DocmaLanguage getOriginalLanguage()
    {
        return getOriginalLanguage(getStoreId());
    }

    public DocmaLanguage getOriginalLanguage(String storeId)
    {
        String lang_code = getDocStoreProperty(storeId, DocmaConstants.PROP_STORE_ORIG_LANGUAGE);
        if ((lang_code == null) || lang_code.trim().equals("")) {
            lang_code = "en";  // set English as default language
        }
        DocmaLanguage res = docmaApp.getContentLanguages().getLanguage(lang_code);
        if (res == null) {  // this should never occur
            // Create language object to avoid NullPointerException
            res = new DocmaLanguage(lang_code, lang_code);
        }
        return res;
    }

    public void setOriginalLanguage(String lang_code) throws DocException
    {
        setDocStoreProperty(getStoreId(), DocmaConstants.PROP_STORE_ORIG_LANGUAGE, lang_code.toLowerCase());
    }

    public boolean hasTranslationLanguage(String store_id, String lang_code)
    {
        lang_code = lang_code.trim().toLowerCase();
        String codes = getDocStoreProperty(store_id, DocmaConstants.PROP_STORE_TRANSLATION_LANGUAGES);
        String[] str_arr;
        if ((codes != null) && (codes.trim().length() > 0)) {
            str_arr = codes.split(",");
            return Arrays.asList(str_arr).contains(lang_code);
        } else {
            return false;
        }
    }

    public boolean addTranslationLanguage(String lang_code) throws DocException
    {
        return addTranslationLanguage(getStoreId(), lang_code);
    }
    
    public boolean addTranslationLanguage(String store_id, String lang_code) throws DocException
    {
        lang_code = lang_code.trim().toLowerCase();
        String codes = getDocStoreProperty(store_id, DocmaConstants.PROP_STORE_TRANSLATION_LANGUAGES);
        String[] str_arr;
        if ((codes != null) && (codes.trim().length() > 0)) {
            str_arr = codes.split(",");
            List code_list = Arrays.asList(str_arr);
            if (code_list.contains(lang_code)) {
                return false;
            }
            // List list_new = new ArrayList(code_list);
            // list_new.add(lang_code);
            // Collections.sort(list_new);
            // codes = DocmaUtil.concatStrings(list_new, ",");
            codes += "," + lang_code;
        } else {
            codes = lang_code;
        }
        setDocStoreProperty(store_id, DocmaConstants.PROP_STORE_TRANSLATION_LANGUAGES, codes);
        return true;
    }

    public boolean deleteTranslationLanguage(String lang_code) throws DocException
    {
        return deleteTranslationLanguage(getStoreId(), lang_code);
    }
    
    public boolean deleteTranslationLanguage(String store_id, String lang_code) throws DocException
    {
        lang_code = lang_code.trim().toLowerCase();
        String codes = getDocStoreProperty(store_id, DocmaConstants.PROP_STORE_TRANSLATION_LANGUAGES);
        String[] str_arr;
        if ((codes == null) || (codes.trim().length() == 0)) {
            return false;
        }
        str_arr = codes.split(",");
        List code_list = Arrays.asList(str_arr);
        if (! code_list.contains(lang_code)) {
            return false;
        }
        List list_new = new ArrayList(code_list);
        list_new.remove(lang_code);
        codes = DocmaUtil.concatStrings(list_new, ",");
        setDocStoreProperty(store_id, DocmaConstants.PROP_STORE_TRANSLATION_LANGUAGES, codes);
        return true;
    }

    public DocmaLanguage[] getTranslationLanguages()
    {
        return getTranslationLanguages(getStoreId());
    }

    public DocmaLanguage[] getTranslationLanguages(String storeId)
    {
        String codes = getDocStoreProperty(storeId, DocmaConstants.PROP_STORE_TRANSLATION_LANGUAGES);
        if ((codes != null) && (codes.trim().length() > 0)) {
            String[] arr = codes.split(",");
            DocmaLanguage[] langs = new DocmaLanguage[arr.length];
            ContentLanguages all_langs = docmaApp.getContentLanguages();
            for (int i=0; i < arr.length; i++) {
                langs[i] = all_langs.getLanguage(arr[i].trim());
            }
            return langs;
        } else {
            return new DocmaLanguage[0];
        }
    }

    public String getTranslationMode()
    {
        return docSess.getTranslationMode();
    }

    public void enterTranslationMode(String lang_code)
    {
        if (lang_code == null) {
            Log.error("enterTranslationMode() called with null value!");
            // throw new DocRuntimeException("enterTranslationMode() called with null value!");
        }
        docSess.enterTranslationMode(lang_code);
        refreshAllNodes(); // refresh all nodes (clear cached values)

        String sid = getStoreId();
        if (sid != null) {  // store is opened
            String state = getVersionState(sid, getVersionId(), lang_code);
            if (! DocmaConstants.VERSION_STATE_TRANSLATION_PENDING.equalsIgnoreCase(state)) {
                // Create default translated gentext file for this language if not yet existent
                DocmaNode genfile_node = getNodeByAlias(DocmaConstants.GENTEXT_ALIAS_NAME);
                if ((genfile_node != null) && !genfile_node.isContentTranslated()) {
                    createDefaultGentext(lang_code, genfile_node);
                }
            }
        }
    }

    public void leaveTranslationMode()
    {
        docSess.leaveTranslationMode();
        refreshAllNodes(); // refresh all nodes (clear cached values)
    }

    public DocmaNode getGenTextNode()
    {
        DocmaNode nd = getNodeByAlias(DocmaConstants.GENTEXT_ALIAS_NAME);
        if (nd == null) {
            DocmaNode sysroot = getSystemRoot();
            String fn = DocmaConstants.GENTEXT_ALIAS_NAME;
            String langCode = getLanguageCode();
            if (langCode != null) {
                fn += "[" + langCode.toUpperCase() + "]";
            }
            nd = sysroot.getChildByFilename(fn + ".properties");
        }
        return nd;
    }

    public Properties getGenTextProps()
    {
        DocmaNode nd = getGenTextNode();
        if (nd == null) return null;
        Date lastDate = nd.getLastModifiedDate();
        // Check if cached properties have to be re-loaded
        if ((genTextPropsDate == null) || !genTextPropsDate.equals(lastDate)) {
            genTextProps = null;
        }
        if (genTextProps == null) {
            genTextPropsDate = lastDate;
            byte[] arr = nd.getContent();
            Properties props = new Properties();
            if (arr != null) {
                try {
                    props.load(new ByteArrayInputStream(arr));
                } catch (Exception ex) {
                    throw new DocRuntimeException(ex);
                }
            }
            genTextProps = props;
        }
        return genTextProps;
    }


    public boolean hasRight(String right)
    {
        if (openedStoreRights == null) {
            initStoreRights();
        }
        return openedStoreRights.contains(right);
    }
    
    public boolean hasFullAdminRight()
    {
        loadAccessRights();
        Set rset = (Set) accessRights.get("*");
        return (rset != null) && rset.contains(AccessRights.RIGHT_ADMINISTRATION);
    }

    public boolean hasAdminRight(String storeId)
    {
        loadAccessRights();
        Set rset1 = (Set) accessRights.get("*");
        if ((rset1 != null) && rset1.contains(AccessRights.RIGHT_ADMINISTRATION)) {
            return true;
        }
        Set rset2 = (Set) accessRights.get(storeId);
        return (rset2 != null) && rset2.contains(AccessRights.RIGHT_ADMINISTRATION);
    }

    public boolean hasViewRight(String storeId)
    {
        loadAccessRights();
        Set rset1 = (Set) accessRights.get("*");
        if ((rset1 != null) && rset1.contains(AccessRights.RIGHT_VIEW_CONTENT)) {
            return true;
        }
        Set rset2 = (Set) accessRights.get(storeId);
        return (rset2 != null) && rset2.contains(AccessRights.RIGHT_VIEW_CONTENT);
    }

    public void openDocStore(String storeId, DocVersionId versionId)
    {
        if (getStoreId() != null) {
            closeDocStore();
        }
        String lang_code = getTranslationMode();
        if ((lang_code != null) && !hasTranslationLanguage(storeId, lang_code)) {
            // If current translation language not supported by store to be
            // opened, then leave translation mode.
            leaveTranslationMode();
        }

        // Remove listeners to avoid events during store initialization
        DocListener[] old_doc_listeners = removeDocListeners();
        LockListener[] old_lock_listeners = removeLockListeners();

        openedStoreRights = null;  // reset
        docSess.openDocStore(storeId, versionId);

        DocGroup store_root = docSess.getRoot();
        if (! store_root.hasAlias(ROOT_ALIAS)) {  // opened for the first time
            if (getTranslationMode() != null) {
                // Store initialization has to be done in original mode
                leaveTranslationMode();
            }
            store_root.addAlias(ROOT_ALIAS);
        }

        root = addToNodeMap(store_root);
        Log.info("Root title: " + root.getTitle());

        // Initialize store if opened for the first time
        if (root.getChildCount() < 4) {  // opened for the first time
            if (getTranslationMode() != null) {
                // Store initialization has to be done in original mode
                leaveTranslationMode();
            }
            try {
                startTransaction();
                // Create document root node
                if (getDocumentRoot() == null) {
                    DocmaNode sec = createSection();
                    sec.setAlias(DOC_ROOT_ALIAS);
                    Log.info("Created document root: " + sec.getId());
                    root.addChild(sec);
                    documentRoot = sec;
                }

                // Create media root node
                if (getRoot().getChildByAlias(MEDIA_ROOT_ALIAS) == null) {
                    DocmaNode media_folder = createImageFolder();
                    media_folder.setAlias(MEDIA_ROOT_ALIAS);
                    media_folder.setTitle(getI18n().getLabel("label.mediafolder.title"));
                    Log.info("Created media root: " + media_folder.getId());
                    root.addChild(media_folder);
                }

                // Create source root node
                if (getRoot().getChildByAlias(FILE_ROOT_ALIAS) == null) {
                    DocmaNode file_folder = createSystemFolder();
                    file_folder.setAlias(FILE_ROOT_ALIAS);
                    file_folder.setTitle(getI18n().getLabel("label.filefolder.title"));
                    Log.info("Created file root: " + file_folder.getId());
                    root.addChild(file_folder);
                }

                // Create system root node
                if (getSystemRoot() == null) {
                    DocmaNode sys_folder = createSystemFolder();
                    sys_folder.setAlias(SYS_ROOT_ALIAS);
                    sys_folder.setTitle(getI18n().getLabel("label.systemfolder.title"));
                    Log.info("Created system root: " + sys_folder.getId());
                    root.addChild(sys_folder);
                    systemRoot = sys_folder;

                    // Create images folder
                    createDefaultImages(sys_folder);
                    // if (DocmaConstants.DEBUG) DebugUtil.printChildren(sys_folder);

                    // Create includes folder
                    // DocmaNode includes_folder = createSection(); // createSystemFolder();
                    // // includes_folder.setAlias(INCLUDES_ALIAS);
                    // includes_folder.setTitle(getI18().getLabel("label.includesfolder.title"));
                    // Log.info("Created includes folder: " + includes_folder.getId());
                    // sys_folder.addChild(includes_folder);

                    // Add example includes: title of note template
                    // DocmaNode inc_notetitle = createHTMLContent();
                    // inc_notetitle.setAlias("title_note");
                    // inc_notetitle.setTitle("Note");
                    // includes_folder.addChild(inc_notetitle);

                    // DocmaNode inc_productname = createHTMLContent();
                    // inc_productname.setAlias("productname");
                    // inc_productname.setTitle(getI18().getLabel("label.includes.productname.title"));
                    // includes_folder.addChild(inc_productname);
                    // inc_productname.setContentString("<p>" + getDocStoreTitle() + "</p>");

                    // Create templates folder
                    DocmaNode templates_folder = createSection(); // createSystemFolder();
                    templates_folder.setAlias(TEMPLATES_ALIAS);
                    templates_folder.setTitle(getI18n().getLabel("label.templatesfolder.title"));
                    Log.info("Created templates folder: " + templates_folder.getId());
                    sys_folder.addChild(templates_folder);

                    // Create images folder within templates folder
                    createTemplateImages(templates_folder);

                    // Add static example templates
                    DocmaNode table1_templ = createHTMLContent();
                    table1_templ.setTitle("Table (Row Header)");
                    table1_templ.setAlias("table_row_head");
                    table1_templ.setContentString(DocmaConstants.TEMPLATE_TABLE1_HTML);
                    templates_folder.addChild(table1_templ);

                    DocmaNode table2_templ = createHTMLContent();
                    table2_templ.setTitle("Table (Column Header)");
                    table2_templ.setAlias("table_col_head");
                    table2_templ.setContentString(DocmaConstants.TEMPLATE_TABLE2_HTML);
                    templates_folder.addChild(table2_templ);

                    // Create Auto-Format folder
                    DocmaNode autoformat_folder = createSection(); // createSystemFolder();
                    autoformat_folder.setTitle(getI18n().getLabel("label.templatesfolder.autoformat.title"));
                    templates_folder.addChild(autoformat_folder);

                    // Add Auto-Format example templates:
                    DocmaNode notetemplate1 = createHTMLContent();
                    notetemplate1.setTitle("Notebox Example 1");
                    notetemplate1.setAlias("notebox_template1");
                    notetemplate1.setContentString(DocmaConstants.TEMPLATE_NOTE_LAYOUT1_HTML);
                    autoformat_folder.addChild(notetemplate1);

                    DocmaNode notetemplate2 = createHTMLContent();
                    notetemplate2.setTitle("Notebox Example 2");
                    notetemplate2.setAlias("notebox_template2");
                    notetemplate2.setContentString(DocmaConstants.TEMPLATE_NOTE_LAYOUT2_HTML);
                    autoformat_folder.addChild(notetemplate2);
                }

                // Create internal/pre-defined inline styles:
                // Strong
                DocmaStyle strong_style = new DocmaStyle("strong",
                    DocmaStyle.INLINE_STYLE, "Strong", DocmaConstants.STYLE_STRONG_CSS);
                saveStyle(strong_style);
                // Emphasis
                DocmaStyle em_style = new DocmaStyle("emphasis",
                    DocmaStyle.INLINE_STYLE, "Emphasis", DocmaConstants.STYLE_EMPHASIS_CSS);
                saveStyle(em_style);
                // Underline
                DocmaStyle underline_style = new DocmaStyle("underline",
                    DocmaStyle.INLINE_STYLE, "Underline", DocmaConstants.STYLE_UNDERLINE_CSS);
                saveStyle(underline_style);
                // Strike-through
                DocmaStyle strike_style = new DocmaStyle("strike",
                    DocmaStyle.INLINE_STYLE, "Strike-through", DocmaConstants.STYLE_STRIKE_CSS);
                saveStyle(strike_style);
                // Link
                DocmaStyle link_style = new DocmaStyle("link",
                    DocmaStyle.INLINE_STYLE, "Link", DocmaConstants.STYLE_LINK_CSS);
                saveStyle(link_style);
                // Footnote
                DocmaStyle footnote_style = new DocmaStyle("footnote",
                    DocmaStyle.INLINE_STYLE, "Footnote", DocmaConstants.STYLE_FOOTNOTE_CSS);
                saveStyle(footnote_style);
                // Breadcrumb-node
                DocmaStyle crumbnode_style = new DocmaStyle("breadcrumb_node",
                    DocmaStyle.INLINE_STYLE, "Breadcrumb Node", DocmaConstants.STYLE_BREADCRUMB_NODE_CSS);
                saveStyle(crumbnode_style);

                // Create internal block styles:
                // Default
                DocmaStyle def_style = new DocmaStyle("default",
                    DocmaStyle.BLOCK_STYLE, "Default", DocmaConstants.STYLE_DEFAULT_CSS);
                saveStyle(def_style);
                // Paragraph
                // DocmaStyle para_style = new DocmaStyle("paragraph",
                //     DocmaStyle.BLOCK_STYLE, "Paragraph", DocmaConstants.STYLE_PARA_CSS);
                // saveStyle(para_style);

                // Caption
                DocmaStyle cap_style = new DocmaStyle("caption",
                    DocmaStyle.BLOCK_STYLE, "Caption", DocmaConstants.STYLE_CAPTION_CSS);
                saveStyle(cap_style);
                // Header 1
                DocmaStyle h1_style = new DocmaStyle("header1",
                    DocmaStyle.BLOCK_STYLE, "Header 1", DocmaConstants.STYLE_H1_CSS);
                saveStyle(h1_style);
                // Header 2
                DocmaStyle h2_style = new DocmaStyle("header2",
                    DocmaStyle.BLOCK_STYLE, "Header 2", DocmaConstants.STYLE_H2_CSS);
                saveStyle(h2_style);
                // Header 3
                DocmaStyle h3_style = new DocmaStyle("header3",
                    DocmaStyle.BLOCK_STYLE, "Header 3", DocmaConstants.STYLE_H3_CSS);
                saveStyle(h3_style);
                // Header 4
                DocmaStyle h4_style = new DocmaStyle("header4",
                    DocmaStyle.BLOCK_STYLE, "Header 4", DocmaConstants.STYLE_H4_CSS);
                saveStyle(h4_style);
                // Header 5
                DocmaStyle h5_style = new DocmaStyle("header5",
                    DocmaStyle.BLOCK_STYLE, "Header 5", DocmaConstants.STYLE_H5_CSS);
                saveStyle(h5_style);
                // Header 6
                DocmaStyle h6_style = new DocmaStyle("header6",
                    DocmaStyle.BLOCK_STYLE, "Header 6", DocmaConstants.STYLE_H6_CSS);
                saveStyle(h6_style);
                // Paragraph Header
                DocmaStyle hp_style = new DocmaStyle("header_para",
                    DocmaStyle.BLOCK_STYLE, "Paragraph Header", DocmaConstants.STYLE_HEADER_PARA_CSS);
                saveStyle(hp_style);
                // Table Cell
                DocmaStyle td_style = new DocmaStyle("table_cell",
                    DocmaStyle.BLOCK_STYLE, "Table Cell", DocmaConstants.STYLE_TABLE_CELL_CSS);
                saveStyle(td_style);
                // Table Header
                DocmaStyle th_style = new DocmaStyle("table_header",
                    DocmaStyle.BLOCK_STYLE, "Table Header", DocmaConstants.STYLE_TABLE_HEADER_CSS);
                saveStyle(th_style);
                // Page Header
                DocmaStyle headbox_style = new DocmaStyle("page_header_box",
                    DocmaStyle.BLOCK_STYLE, "Page Header", DocmaConstants.STYLE_PAGE_HEADER_CSS);
                saveStyle(headbox_style);
                // Page Footer
                DocmaStyle footbox_style = new DocmaStyle("page_footer_box",
                    DocmaStyle.BLOCK_STYLE, "Page Footer", DocmaConstants.STYLE_PAGE_FOOTER_CSS);
                saveStyle(footbox_style);
                // Breadcrumbs
                DocmaStyle breadcrumbs_style = new DocmaStyle("breadcrumbs",
                    DocmaStyle.BLOCK_STYLE, "Breadcrumbs Navigation", DocmaConstants.STYLE_BREADCRUMBS_CSS);
                saveStyle(breadcrumbs_style);
                
                // Create example title-page styles as hidden styles:
                // Subtitle
                DocmaStyle subtitle_style = new DocmaStyle("subtitle",
                    DocmaStyle.BLOCK_STYLE, "Publication Subtitle", DocmaConstants.STYLE_SUBTITLE_CSS, null, true);
                saveStyle(subtitle_style);
                // Author
                DocmaStyle author_style = new DocmaStyle("author",
                    DocmaStyle.BLOCK_STYLE, "Author Name", DocmaConstants.STYLE_AUTHOR_CSS, null, true);
                saveStyle(author_style);
                
                // Styles for static example templates: table
                DocmaStyle tablehead1_style = new DocmaStyle("table_header1",
                    DocmaStyle.BLOCK_STYLE, "Table Header Example", DocmaConstants.TEMPLATE_TABLE_HEADER1_CSS, null, true);
                saveStyle(tablehead1_style);

                // Styles for Auto-Format example templates: note
                DocmaStyle noteheader_style = new DocmaStyle("header_note",
                    DocmaStyle.BLOCK_STYLE, "Note Header Example", DocmaConstants.TEMPLATE_NOTE_HEADER_CSS, null, true);
                saveStyle(noteheader_style);
                DocmaStyle notebox1_style = new DocmaStyle("note_box",
                    DocmaStyle.BLOCK_STYLE, "Note Content Example 1", DocmaConstants.TEMPLATE_NOTE_BOX1_CSS, null, true);
                saveStyle(notebox1_style);
                DocmaStyle notebox2_style = new DocmaStyle("note_box2",
                    DocmaStyle.BLOCK_STYLE, "Note Content Example 2", DocmaConstants.TEMPLATE_NOTE_BOX2_CSS, null, true);
                saveStyle(notebox2_style);
                DocmaStyle note_style = new DocmaStyle("note",
                    DocmaStyle.BLOCK_STYLE, "Notebox", DocmaConstants.TEMPLATE_NOTE_CSS, DocmaConstants.TEMPLATE_NOTE_AUTOFORMAT);
                saveStyle(note_style);

                // Create default output configurations
                String[] out_ids = getOutputConfigIds();
                if (out_ids.length == 0) {
                    DocmaOutputConfig html_conf    = new DocmaOutputConfig(DocmaConstants.DEFAULT_HTML_CONFIG_ID);
                    DocmaOutputConfig pdf_conf     = new DocmaOutputConfig(DocmaConstants.DEFAULT_PDF_CONFIG_ID);
                    DocmaOutputConfig docbook_conf = new DocmaOutputConfig(DocmaConstants.DEFAULT_DOCBOOK_CONFIG_ID);
                    DocmaOutputConfig webhelp_conf = new DocmaOutputConfig(DocmaConstants.DEFAULT_WEBHELP_CONFIG_ID);
                    DocmaOutputConfig epub_conf    = new DocmaOutputConfig(DocmaConstants.DEFAULT_EPUB_CONFIG_ID);
                    html_conf.setFormat("html");
                    pdf_conf.setFormat("pdf");
                    docbook_conf.setFormat("docbook");
                    DocmaAppUtil.setWebHelpDefaults(webhelp_conf);
                    DocmaAppUtil.setEPUBDefaults(epub_conf);
                    saveOutputConfig(html_conf);
                    saveOutputConfig(pdf_conf);
                    saveOutputConfig(docbook_conf);
                    saveOutputConfig(webhelp_conf);
                    saveOutputConfig(epub_conf);
                }

                commitTransaction();
            } catch (DocException ex) {
                if (runningTransaction()) rollbackTransaction();
                throw new DocRuntimeException("Failed to initialize document store!", ex);
            }
        }  // opened for the first time

        // Create default gentext file if it does not yet exist and session is
        // not in translation mode:
        if (getNodeIdByAlias(DocmaConstants.GENTEXT_ALIAS_NAME) == null) {
            if (getTranslationMode() == null) createDefaultGentext(null, null);
            // Note: If store is opened in translation mode, then gentext file
            //       cannot be created, because creation of nodes is not allowed
            //       in tranlation mode. See also method enterTranslationMode().
        }

        // Create default HTML customization files if not yet existent
        if (getNodeIdByAlias(DocmaConstants.HTML_CONFIG_FOLDER_ALIAS_NAME) == null) {
            try {
                startTransaction();
                createDefaultHtmlConfig(getSystemRoot());
                commitTransaction();
            } catch (Exception ex2) {
                if (runningTransaction()) rollbackTransaction();
                throw new DocRuntimeException("Failed to create HTML configuration folder", ex2);
            }
            // if (DocmaConstants.DEBUG) DebugUtil.printChildren(sys_folder);
        }

        // Reset any listeners which where set before store was opened
        for (int i=0; i < old_doc_listeners.length; i++) {
            addDocListener(old_doc_listeners[i]);
        }
        for (int i=0; i < old_lock_listeners.length; i++) {
            addLockListener(old_lock_listeners[i]);
        }
    }

    public void closeDocStore()
    {
        openedStoreCounter++;
        docSess.closeDocStore();
        root = null;
        documentRoot = null;
        systemRoot = null;
        nodeMap.clear();
        openedStoreRights = null;
        pluginStoreConnection = null;
    }

    public boolean usersConnected(String storeId, DocVersionId verId)
    {
        DocmaSession[] sess_arr = docmaApp.getOpenSessions(storeId, verId);
        return (sess_arr.length > 0);
    }

    public DocmaNode getRoot()
    {
        // if (root == null) {
        //     root = addToNodeMap(docSess.getRoot());
        // }
        return root;
    }

    public DocmaNode getDocumentRoot()
    {
        if (documentRoot == null) {
            documentRoot = getRoot().getChildByAlias(DOC_ROOT_ALIAS);
        }
        return documentRoot;
    }

    public DocmaNode getSystemRoot()
    {
        if (systemRoot == null) {
            systemRoot = getRoot().getChildByAlias(SYS_ROOT_ALIAS);
        }
        return systemRoot;
    }

    public DocmaNode getMediaRoot()
    {
        return getRoot().getChildByAlias(MEDIA_ROOT_ALIAS);
    }

    public DocmaNode getFileRoot()
    {
        return getRoot().getChildByAlias(FILE_ROOT_ALIAS);
    }

    public DocmaNode getTemplatesFolder()
    {
        return getSystemRoot().getChildByAlias(TEMPLATES_ALIAS);
    }

    public String getLastOpenedStore() {
        return getUserProperty("openedStore");
    }

    public DocVersionId getLastOpenedVersion() {
        String verStr = getUserProperty("openedVersion");
        if ((verStr == null) || verStr.trim().equals("")) {
            return null;
        } else {
            try {
                return docSess.createVersionId(verStr);
            } catch (DocException dex) {
                return null;
            }
        }
    }

    public DocVersionId[] listVersions(String storeId) 
    {
        return docSess.listVersions(storeId);
    }

    public String[] listDocStores() 
    {
        return docSess.listDocStores();
    }

    public void createDocStore(String storeId) throws DocException 
    {
        docSess.createDocStore(storeId, null, null);
    }

    public void createDocStore(String storeId, String[] propNames, String[] propValues) throws DocException 
    {
        docSess.createDocStore(storeId, propNames, propValues);
    }
    
    public void addDocStore(String storeId, File externalStorePath) throws DocException
    {
        docSess.addDocStore(storeId, new String[] { DocmaConstants.PROP_STORE_PATH }, 
                                     new String[] { externalStorePath.getPath() } );
    }

    public void addExternalDbDocStore(String storeId, 
                                      String dburl, 
                                      String driver, 
                                      String dialect, 
                                      String usr, 
                                      String pw) throws DocException
    {
        docSess.addDocStore(storeId, 
                            new String[] { FilesystemStoreProperties.PROP_DB_CONNECTION_URL, 
                                           FilesystemStoreProperties.PROP_DB_DRIVER_CLASS, 
                                           FilesystemStoreProperties.PROP_DB_DIALECT, 
                                           FilesystemStoreProperties.PROP_DB_CONNECTION_USER, 
                                           FilesystemStoreProperties.PROP_DB_CONNECTION_PWD }, 
                            new String[] { dburl, driver, dialect, usr, pw } );
    }

    public DocVersionId createVersionId(String vid) throws DocException 
    {
        return docSess.createVersionId(vid);
    }

    public boolean isValidVersionId(String verId)
    {
        return DefaultVersionId.isValidVersionId(verId);
    }

    public void createVersion(String storeId, DocVersionId baseVersion, DocVersionId newVersion)
    throws DocException
    {
        boolean started = startLocalTransaction();
        try {
            docSess.createVersion(storeId, baseVersion, newVersion);
            setVersionComment(storeId, newVersion, "");
            commitLocalTransaction(started);
        } catch (Exception ex) {
            rollbackLocalTransaction(started);
            throw new DocException(ex);
        }
    }

    public void deleteDocStore(String storeId) throws DocException 
    {
        clearRevisions(storeId);
        docSess.deleteDocStore(storeId);
        invalidatePublicationArchive(storeId, null);
    }

    public void removeExternalDocStoreConnection(String storeId) throws DocException 
    {
        if (docSess instanceof DocStoreSessionImpl) {
            DocStoreSessionImpl docSessImpl = (DocStoreSessionImpl) docSess;
            if (docSessImpl.isDbExternalStore(storeId)) {
                File f = docSessImpl.getStoreDirFromId(storeId);
                if (f != null) {
                    File pubdir = new File(f, PublicationArchivesSessImpl.PUBLICATION_ARCHIVES_FOLDERNAME);
                    if (pubdir.exists()) {
                        String msg = getI18n().getLabel("text.product.remove_db_connection.error_fsarchive", new Object[] { pubdir.getAbsolutePath() });
                        throw new DocException(msg);
                    }
                }
            }
        }
        docSess.deleteDocStore(storeId, true);
        invalidatePublicationArchive(storeId, null);
    }

    public void changeDocStoreId(String oldId, String newId) throws DocException 
    {
        // Check if oldId is a valid store.
        boolean connection_ok = true;
        try {
            // If oldId is not a valid connection, then getDocStoreProperty 
            // throws an exception.
            getDocStoreProperty(oldId, DocmaConstants.PROP_STORE_DISPLAYNAME);
        } catch (Exception ex) {
            connection_ok = false;
        }
        
        // If oldId is valid, then check if running activity exists.
        if (connection_ok) {
            Activity act = getDocStoreActivity(oldId);
            if ((act != null) && act.isRunning()) {
                throw new DocException("Cannot change store ID. Running activity exists!");
            }
        }
        
        // Note: changeDocStoreId() is called even if oldId is not valid.
        //       For DB connections this is used to allow renaming of the
        //       configuration folder (configuration folder must have the same
        //       name that is stored as display ID in the database. 
        docSess.changeDocStoreId(oldId, newId);
        invalidatePublicationArchive(oldId, null);
    }

    public DocVersionId getVersionId() 
    {
        return docSess.getVersionId();
    }

    public void renameVersion(String storeId, DocVersionId oldVerId, DocVersionId newVerId)
    throws DocException
    {
        if (DocmaConstants.VERSION_STATE_RELEASED.equalsIgnoreCase(getVersionState(storeId, oldVerId))) {
            throw new DocException("Cannot rename released version:" + oldVerId);
        }
        // Activity act = getDocStoreActivity(storeId);
        // if ((act != null) && act.isRunning()) {
        //     throw new DocException("Cannot rename version. Running activity exists!");
        // }
        docSess.renameVersion(storeId, oldVerId, newVerId);
        invalidatePublicationArchive(storeId, oldVerId);
    }

    public void deleteVersion(String storeId, DocVersionId verId)
    throws DocException
    {
        DocVersionId[] subs = getSubVersions(storeId, verId);
        if (subs.length > 0) {
            throw new DocException("Cannot delete version. Another version is derived from this version.");
        }
        docSess.deleteVersion(storeId, verId);
        invalidatePublicationArchive(storeId, verId);
    }

    public String getVersionState(String storeId, DocVersionId verId)
    {
        return docSess.getVersionState(storeId, verId);
    }

    public String getVersionState(String storeId, DocVersionId verId, String lang)
    {
        return docSess.getVersionState(storeId, verId, lang);
    }

    public void setVersionState(String storeId, DocVersionId verId, String newState)
    throws DocException
    {
        docSess.setVersionState(storeId, verId, newState);
    }

    public Date getVersionCreationDate(String storeId, DocVersionId verId)
    {
        return docSess.getVersionCreationDate(storeId, verId);
    }

    public String getVersionComment(String storeId, DocVersionId verId)
    {
        String lang = getTranslationMode();
        String nm = DocmaConstants.PROP_VERSION_COMMENT;
        if (lang != null) {
            nm += "." + lang.toLowerCase();
        }
        String str = docSess.getVersionProperty(storeId, verId, nm);
        return (str == null) ? "" : str;
    }

    public void setVersionComment(String storeId, DocVersionId verId, String comment)
    throws DocException
    {
        String lang = getTranslationMode();
        String nm = DocmaConstants.PROP_VERSION_COMMENT;
        if (lang != null) {
            nm += "." + lang.toLowerCase();
        }
        docSess.setVersionProperty(storeId, verId, nm, comment);
    }

    public Date getVersionLastModifiedDate(String storeId, DocVersionId verId)
    {
        String lang = getTranslationMode();
        String nm = DocmaConstants.PROP_VERSION_LAST_MODIFIED_DATE;
        if (lang != null) {
            nm += "." + lang.toLowerCase();
        }
        String millis = docSess.getVersionProperty(storeId, verId, nm);
        try {
            return new Date(Long.parseLong(millis));
        } catch (Exception ex) {
            return null;
        }
    }

    void setVersionLastModifiedDate(String storeId, DocVersionId verId, Date lastmod)
    throws DocException
    {
        String lang = getTranslationMode();
        String nm = DocmaConstants.PROP_VERSION_LAST_MODIFIED_DATE;
        if (lang != null) {
            nm += "." + lang.toLowerCase();
        }
        String val = Long.toString(lastmod.getTime());
        docSess.setVersionProperty(storeId, verId, nm, val);
    }

    public Date getVersionReleaseDate(String storeId, DocVersionId verId)
    {
        return docSess.getVersionReleaseDate(storeId, verId);
    }

    public DocVersionId getVersionDerivedFrom(String storeId, DocVersionId verId)
    {
        return docSess.getVersionDerivedFrom(storeId, verId);
    }

    public DocVersionId[] getSubVersions(String storeId, DocVersionId verId)
    {
        return docSess.getSubVersions(storeId, verId);
    }

    public String getUserId() {
        return docSess.getUserId();
    }

    public String getStoreId() {
        return docSess.getStoreId();
    }

    public DocVersionId getLatestVersionId(String storeId) {
        return docSess.getLatestVersionId(storeId);
    }

    public String getDocStoreTitle() {
        return getDocStoreProperty(getStoreId(), DocmaConstants.PROP_STORE_DISPLAYNAME);
    }

    public DocListener[] getDocListeners() {
        return docSess.getDocListeners();
    }

    public LockListener[] getLockListeners() {
        return docSess.getLockListeners();
    }

    public boolean removeLockListener(LockListener listener) {
        return docSess.removeLockListener(listener);
    }

    public boolean removeDocListener(DocListener listener) {
        return docSess.removeDocListener(listener);
    }

    public LockListener[] removeLockListeners() {
        LockListener[] arr = getLockListeners();
        for (int i=0; i < arr.length; i++) removeLockListener(arr[i]);
        return arr;
    }

    public DocListener[] removeDocListeners() {
        DocListener[] arr = getDocListeners();
        for (int i=0; i < arr.length; i++) removeDocListener(arr[i]);
        return arr;
    }

    public void addLockListener(LockListener listener) {
        docSess.addLockListener(listener);
    }

    public void addDocListener(DocListener listener) {
        docSess.addDocListener(listener);
    }

    /** Gets a persistent node by id. Node: Finds only persistent nodes. */
    public DocmaNode getNodeById(String id) {
        if (id == null) return null;
        DocmaNode node = getFromNodeMap(id);
        if (node == null) {
            node = addToNodeMap(docSess.getNodeById(id));
        }
        return node;
    }

    public String getNodeIdByAlias(String alias) {
        if (alias == null) return null;
        return docSess.getNodeIdByAlias(alias);
    }

    public DocmaNode getNodeByAlias(String alias) {
        if (alias == null) return null;
        return getNodeById(getNodeIdByAlias(alias));
    }

    public DocmaNode[] getNodesByLinkAlias(String linkAlias)
    {
        List list = new ArrayList();
        DocmaNode nd = getNodeByAlias(linkAlias);
        if (nd != null) {
            list.add(nd);
        }

        String[] all_aliases = docSess.listAliases(null);
        String link_prefix = linkAlias + DocmaConstants.LINKALIAS_SEPARATOR;
        int pos = Arrays.binarySearch(all_aliases, link_prefix);
        if (pos < 0) {  // if not found
            pos = -(pos + 1);  // insertion point
        }
        while (pos < all_aliases.length) {
            String a = all_aliases[pos];
            if (a.startsWith(link_prefix)) {
                DocmaNode n = getNodeByAlias(a);
                if (n != null) list.add(n);
            } else {
                break;
            }
            pos++;
        }

        DocmaNode[] node_arr = new DocmaNode[list.size()];
        return (DocmaNode[]) list.toArray(node_arr);
    }

    public DocmaNode getApplicableNodeByLinkAlias(String linkAlias, String[] applics)
    {
        DocmaNode[] nodes = getNodesByLinkAlias(linkAlias);
        if (nodes.length == 0) return null;

        // Simple case: only one node without applicability assigned
        if (nodes.length == 1) {
            String appl = nodes[0].getApplicability();
            if ((appl == null) || appl.trim().equals("")) {
                return nodes[0];  // no applicability assigned, therefore node is applicable
            }
        }
        
        // In unfiltered mode return first node
        if (applics == null) {
            return nodes[0];
        }

        // Difficult case: evaluate applicability of node(s)
        ApplicEvaluator evaluator = new ApplicEvaluator();
        evaluator.setApplicability(applics);
        for (DocmaNode nd : nodes) {
            String appl = nd.getApplicability();
            if ((appl == null) || appl.trim().equals("")) {
                return nd;  // no applicability assigned, therefore node is applicable
            } else {
                try {
                    if (evaluator.evaluate(appl)) return nd;
                } catch (DocException dex) {}
            }
        }
        // None of the nodes is applicable
        return null;
    }

    
    /**
     * Returns a sorted array of all node aliases.
     * The sort order is defined by the
     * <code>java.lang.String.compareTo(Object)</code> method.
     * 
     * @return  sorted list of aliases.
     */
    public String[] getNodeAliases()
    {
        return docSess.listAliases(DocNode.class);
    }

    /**
     * Returns a sorted read-only list of all node aliases.
     * This method provides the same functionality as {@link getNodeAliases()}, 
     * except that the values are returned in a read-only list instead of 
     * an array.
     * 
     * @return  sorted list of aliases.
     */
    public List<String> listNodeAliases()
    {
        return Arrays.asList(getNodeAliases());
    }

    /**
     * Returns the node information of all nodes.
     * The returned list might be read-only.
     * 
     * @return  node information of all nodes
     */
    public List<NodeInfo> listNodeInfos()
    {
        return docSess.listNodeInfos(DocNode.class);
    }

    /**
     * Returns a sorted array of all content aliases.
     * The sort order is defined by the
     * <code>java.lang.String.compareTo(Object)</code> method.
     * 
     * @return  sorted list of aliases.
     */
    public String[] getContentAliases()
    {
        return docSess.listAliases(DocContent.class);
    }
    
    /**
     * Returns a sorted read-only list of all content aliases.
     * This method provides the same functionality as {@link getContentAliases()}, 
     * except that the values are returned in a read-only list instead of 
     * an array.
     * 
     * @return  sorted list of aliases.
     */
    public List<String> listContentAliases()
    {
        return Arrays.asList(getContentAliases());
    }
    
    /**
     * Returns the node information of all content nodes.
     * The returned list might be read-only.
     * 
     * @return  node information of all content nodes
     */
    public List<NodeInfo> listContentInfos()
    {
        return docSess.listNodeInfos(DocContent.class);
    }

    /**
     * Returns a sorted array of all HTML content aliases.
     * The sort order is defined by the
     * <code>java.lang.String.compareTo(Object)</code> method.
     * 
     * @return  sorted list of aliases.
     */
    public String[] getHTMLContentAliases()
    {
        return docSess.listAliases(DocXML.class);
    }
    
    /**
     * Returns a sorted read-only list of all HTML content aliases.
     * This method provides the same functionality as {@link getHTMLContentAliases()}, 
     * except that the values are returned in a read-only list instead of 
     * an array.
     * 
     * @return  sorted list of aliases.
     */
    public List<String> listHTMLContentAliases()
    {
        return Arrays.asList(getHTMLContentAliases());
    }
    
    /**
     * Returns the node information of all HTML content nodes.
     * The returned list might be read-only.
     * 
     * @return  node information of all HTML content nodes
     */
    public List<NodeInfo> listHTMLContentInfos()
    {
        return docSess.listNodeInfos(DocXML.class);
    }

    /**
     * Returns a sorted array of all file aliases.
     * The sort order is defined by the
     * <code>java.lang.String.compareTo(Object)</code> method.
     * 
     * @return  sorted array of aliases.
     */
    public String[] getFileAliases()
    {
        return docSess.listAliases(DocFile.class);
    }

    /**
     * Returns a sorted read-only list of all file aliases.
     * This method provides the same functionality as {@link getFileAliases()}, 
     * except that the values are returned in a read-only list instead of 
     * an array.
     * 
     * @return  sorted list of aliases.
     */
    public List<String> listFileAliases()
    {
        return Arrays.asList(getFileAliases());
    }
    
    /**
     * Returns the node information of all file nodes.
     * The returned list might be read-only.
     * 
     * @return  node information of all file nodes
     */
    public List<NodeInfo> listFileInfos()
    {
        return docSess.listNodeInfos(DocFile.class);
    }

    /**
     * Returns a sorted array of all image aliases.
     * The sort order is defined by the
     * <code>java.lang.String.compareTo(Object)</code> method.
     * 
     * @return  sorted array of aliases.
     */
    public String[] getImageAliases()
    {
        return docSess.listAliases(DocImage.class);
    }

    /**
     * Returns a sorted read-only list of all image aliases.
     * This method provides the same functionality as {@link getImageAliases()}, 
     * except that the values are returned in a read-only list instead of 
     * an array.
     * 
     * @return  sorted list of aliases.
     */
    public List<String> listImageAliases()
    {
        return Arrays.asList(getImageAliases());
    }

    /**
     * Returns the node information of all image nodes.
     * The returned list might be read-only.
     * 
     * @return  node information of all image nodes
     */    
    public List<NodeInfo> listImageInfos()
    {
        return docSess.listNodeInfos(DocImage.class);
    }

    /**
     * Returns a sorted array of all group aliases.
     * The sort order is defined by the
     * <code>java.lang.String.compareTo(Object)</code> method.
     * 
     * @return  sorted array of aliases.
     */
    public String[] getGroupAliases()
    {
        return docSess.listAliases(DocGroup.class);
    }

    /**
     * Returns a sorted read-only list of all group aliases.
     * This method provides the same functionality as {@link getGroupAliases()}, 
     * except that the values are returned in a read-only list instead of 
     * an array.
     * 
     * @return  sorted list of aliases.
     */
    public List<String> listGroupAliases()
    {
        return Arrays.asList(getGroupAliases());
    }

    /**
     * Returns the node information of all group nodes.
     * The returned list might be read-only.
     * 
     * @return  node information of all group nodes
     */    
    public List<NodeInfo> listGroupInfos()
    {
        return docSess.listNodeInfos(DocGroup.class);
    }

    /**
     * Returns a sorted array of all section aliases.
     * The sort order is defined by the
     * <code>java.lang.String.compareTo(Object)</code> method.
     * 
     * @return  sorted list of aliases.
     */
    public String[] getSectionAliases()
    {
        List<String> res = listSectionAliases();
        return res.toArray(new String[res.size()]);
    }

    /**
     * Returns a sorted read-only list of all section aliases.
     * This method provides the same functionality as {@link getSectionAliases()}, 
     * except that the values are returned in a read-only list instead of 
     * an array.
     * 
     * @return Sorted list of aliases.
     */
    public List<String> listSectionAliases()
    {
        String[] aliases = getGroupAliases();
        ArrayList<String> res_list = new ArrayList<String>(aliases.length);
        for (String a : aliases) {
            DocmaNode n = getNodeByAlias(a);
            if ((n != null) && n.isSection()) {
                res_list.add(a);
            }
        }
        return res_list;
    }

    /**
     * Returns the node information of all section nodes.
     * The returned list might be read-only.
     * 
     * @return  node information of all section nodes
     */    
    public List<NodeInfo> listSectionInfos()
    {
        List<NodeInfo> infos = listGroupInfos();
        ArrayList<NodeInfo> res = new ArrayList<NodeInfo>(infos.size());
        for (NodeInfo info : infos) {
            DocmaNode n = getNodeById(info.getId());
            if ((n != null) && n.isSection()) {
                res.add(info);
            }
        }
        return res;
    }

    /**
     * Returns a sorted array of all folder aliases.
     * The sort order is defined by the
     * <code>java.lang.String.compareTo(Object)</code> method.
     * 
     * @return  sorted list of aliases.
     */
    public String[] getFolderAliases()
    {
        List<String> res = listFolderAliases();
        return res.toArray(new String[res.size()]);
    }

    /**
     * Returns a sorted read-only list of all folder aliases.
     * This method provides the same functionality as {@link getFolderAliases()}, 
     * except that the values are returned in a read-only list instead of 
     * an array.
     * 
     * @return Sorted list of aliases.
     */
    public List<String> listFolderAliases()
    {
        String[] aliases = getGroupAliases();
        ArrayList<String> res_list = new ArrayList<String>(aliases.length);
        for (String a : aliases) {
            DocmaNode n = getNodeByAlias(a);
            if ((n != null) && n.isFolder()) {
                res_list.add(a);
            }
        }
        return res_list;
    }

    /**
     * Returns the node information of all folder nodes.
     * The returned list might be read-only.
     * 
     * @return  node information of all folder nodes
     */    
    public List<NodeInfo> listFolderInfos()
    {
        List<NodeInfo> infos = listGroupInfos();
        ArrayList<NodeInfo> res = new ArrayList<NodeInfo>(infos.size());
        for (NodeInfo info : infos) {
            DocmaNode n = getNodeById(info.getId());
            if ((n != null) && n.isFolder()) {
                res.add(info);
            }
        }
        return res;
    }

    /**
     * Returns a sorted array of all reference aliases.
     * The sort order is defined by the
     * <code>java.lang.String.compareTo(Object)</code> method.
     * 
     * @return  sorted list of aliases.
     */
    public String[] getReferenceAliases()
    {
        return docSess.listAliases(DocReference.class);
    }

    /**
     * Returns a sorted read-only list of all reference aliases.
     * This method provides the same functionality as {@link getReferenceAliases()}, 
     * except that the values are returned in a read-only list instead of 
     * an array.
     * 
     * @return  sorted list of aliases.
     */
    public List<String> listReferenceAliases()
    {
        return Arrays.asList(getReferenceAliases());
    }

    /**
     * Returns the node information of all reference nodes.
     * The returned list might be read-only.
     * 
     * @return  node information of all group nodes
     */    
    public List<NodeInfo> listReferenceInfos()
    {
        return docSess.listNodeInfos(DocReference.class);
    }

    /**
     * Tests whether the current user is allowed to edit this product version.
     * If editing is allowed, this method does nothing. Otherwise an exception
     * is thrown.
     * 
     * @throws DocException if editing is not allowed, for example, because
     *                      version is already released or due to missing
     *                      access rights.
     */
    public void checkUpdateVersionAllowed() throws DocException
    {
        String ver_state = getVersionState(getStoreId(), getVersionId());
        boolean isReleased = (ver_state != null) && ver_state.equals(DocmaConstants.VERSION_STATE_RELEASED);
        boolean isTransMode = (getTranslationMode() != null);
        boolean hasContentRight = hasRight(AccessRights.RIGHT_EDIT_CONTENT);
        boolean hasTransRight = hasRight(AccessRights.RIGHT_TRANSLATE_CONTENT);
        boolean allowEdit = (hasContentRight && !isTransMode) || (hasTransRight && isTransMode);
        if (isReleased || !allowEdit) {
            String msg;
            if (isReleased) {
                msg = "This product version is already released. Editing is not allowed.";
            } else {
                msg = "Editing is not allowed due to missing access rights.";
            }
            throw new DocException(msg);  // disable editing if user has no rights for editing
                                          // or version is already released.
        }
        // No exception means editing is allowed
    }
    

    public DocmaNode createFileContent() {
        DocFile cont = docSess.createFile();
        cont.setContentType("application/octet-stream");
        return addToNodeMap(cont);
    }

    public DocmaNode createImageContent() {
        DocImage cont = docSess.createImage();
        return addToNodeMap(cont);
    }

    public DocmaNode createHTMLContent() {
        if (DocmaConstants.DEBUG) {
            Log.info("--- Creating HTML content ---");
        }
        DocXML cont = docSess.createXML();  // XHTML
        cont.setContentType("text/html");   // or "application/xhtml+xml"
        cont.setFileExtension("html");      // or "xhtml"
        cont.setContentString("");
        DocmaNode html_node = addToNodeMap(cont);
        html_node.setLastModifiedDate(new Date());
        html_node.setLastModifiedBy(getUserId());
        html_node.setWorkflowStatus("wip");
        html_node.setProgress(0);
        if (DocmaConstants.DEBUG) {
            Log.info("--- Creating HTML content finished ---");
        }
        return html_node;
    }

    public DocmaNode createSection() {
        if (DocmaConstants.DEBUG) {
            Log.info("--- Creating Section node ---");
        }
        DocGroup group = docSess.createGroup();
        group.setAttribute(DocmaNode.ATTR_GROUP_TYPE, DocmaNode.TYPE_SECTION);
        if (DocmaConstants.DEBUG) {
            Log.info("--- Creating Section node finished ---");
        }
        return addToNodeMap(group);
    }

    public DocmaNode createContentIncludeReference() {
        DocReference ref = docSess.createReference();
        ref.setAttribute(DocmaNode.ATTR_REFERENCE_TYPE, DocmaNode.TYPE_CONTENT_REFERENCE);
        ref.setTargetAlias("");
        return addToNodeMap(ref);
    }

    public DocmaNode createSectionIncludeReference() {
        DocReference ref = docSess.createReference();
        ref.setAttribute(DocmaNode.ATTR_REFERENCE_TYPE, DocmaNode.TYPE_SECTION_REFERENCE);
        ref.setTargetAlias("");
        return addToNodeMap(ref);
    }

    public DocmaNode createImageIncludeReference() {
        DocReference ref = docSess.createReference();
        ref.setAttribute(DocmaNode.ATTR_REFERENCE_TYPE, DocmaNode.TYPE_IMAGE_REFERENCE);
        ref.setTargetAlias("");
        return addToNodeMap(ref);
    }

    public DocmaNode createImageFolder() {
        DocGroup group = docSess.createGroup();
        group.setAttribute(DocmaNode.ATTR_GROUP_TYPE, DocmaNode.TYPE_IMG_FOLDER);
        return addToNodeMap(group);
    }

    public DocmaNode createFileFolder() {
        return createSystemFolder();
    }

    /**
     * This method is deprecated. Use createFileFolder() instead.
     * @return a file folder instance
     */    
    public DocmaNode createSystemFolder() {
        DocGroup group = docSess.createGroup();
        group.setAttribute(DocmaNode.ATTR_GROUP_TYPE, DocmaNode.TYPE_SYS_FOLDER);
        return addToNodeMap(group);
    }

    public void startTransaction() throws DocException {
        docSess.startTransaction();
    }

    public void commitTransaction() throws DocException {
        docSess.commitTransaction();
    }

    public void rollbackTransaction() {
        docSess.rollbackTransaction();
        // ((DocmaTreeitemModel) getRoot()).refresh();
        // after calling this method -> re-render tree: setModel(getModel())
    }

    public boolean runningTransaction() {
        return docSess.runningTransaction();
    }

    public DocmaNode[] getClipboardContent() {
        return null;
    }

    public void clearClipboard() {

    }

    public void copyToClipboard(DocmaNode node) {

    }

    public void moveToClipboard(DocmaNode node) {

    }

    public void closeSession()
    {
        // First, close all child sessions
        if (childSessions != null) {
            Map<String, DocmaSession> tempMap = childSessions;
            childSessions = null;
            for (DocmaSession childSess : tempMap.values()) {
                try {
                    childSess.closeSession();
                } catch (Exception ex) {
                    Log.error("Failed to close child session: " + ex.getMessage());
                }
            }
        }
        
        // If this session is a child of another session, remove this session 
        // from the child list of the parent session.
        if (parentSession != null) {
            if (parentSession.childSessions != null) {
                parentSession.childSessions.remove(getSessionId());
            }
            parentSession = null;
        }
        
        // Now, close this session
        docSess.closeSession();
        pluginStoreConnection = null;
        docmaApp.releaseSession(this);
        if (sessionListener != null) {
            sessionListener.sessionClosed();
        }
    }

    public void setSessionListener(DocmaSessionListener sessListener)
    {
        this.sessionListener = sessListener;
    }

    public String[] getDeclaredApplics()
    {
        String applic_str = getDocStoreProperty(getStoreId(), DocmaConstants.PROP_STORE_DECLARED_APPLICS);
        if (applic_str != null) {
            return applic_str.split(",");
        } else {
            return new String[0];
        }
    }

    public void setDeclaredApplics(String[] applics) throws DocException
    {
        setDeclaredApplics(Arrays.asList(applics));
    }

    public void setDeclaredApplics(List applics) throws DocException
    {
        String vals = DocmaUtil.concatStrings(applics, ",");
        setDocStoreProperty(getStoreId(), DocmaConstants.PROP_STORE_DECLARED_APPLICS, vals);
    }

    public void addDeclaredApplics(String... applics) throws DocException
    {
        List<String> list = new ArrayList<String>(Arrays.asList(getDeclaredApplics()));
        for (String a : applics) {
            if (! list.contains(a)) {
                list.add(a);
            }
        }
        setDeclaredApplics(list);
    }
            
    public void removeDeclaredApplics(String... applics) throws DocException
    {
        List<String> list = new ArrayList<String>(Arrays.asList(getDeclaredApplics()));
        for (String a : applics) {
            list.remove(a);
        }
        setDeclaredApplics(list);
    }
            
    public String[] getFilterSettingNames()
    {
        String filternames = getDocStoreProperty(getStoreId(), DocmaConstants.PROP_STORE_FILTER_NAMES);
        if (filternames != null) {
            return filternames.split(",");
        } else {
            return new String[0];
        }
    }

    public String getFilterSetting(String filtername)
    {
        return getDocStoreProperty(getStoreId(),
                                   DocmaConstants.PROP_STORE_FILTER_SETTING + "." + filtername);
    }

    public void createFilterSetting(String filtername, String filtersetting) throws DocException
    {
        List names = Arrays.asList(getFilterSettingNames());
        if (names.contains(filtername)) {
            throw new DocException("Cannot create filter setting. Filter name already exists.");
        }
        List namesnew = new ArrayList(names);
        namesnew.add(filtername);
        Collections.sort(namesnew);
        String names_str = DocmaUtil.concatStrings(namesnew, ",");
        String[] prop_names = new String[] {
            DocmaConstants.PROP_STORE_FILTER_NAMES,
            DocmaConstants.PROP_STORE_FILTER_SETTING + "." + filtername
        };
        String[] prop_values = new String[] { names_str, filtersetting };
        setDocStoreProperties(getStoreId(), prop_names, prop_values);
    }

    public void changeFilterSetting(String filtername, String filtersetting) throws DocException
    {
        setDocStoreProperty(getStoreId(),
                            DocmaConstants.PROP_STORE_FILTER_SETTING + "." + filtername,
                            filtersetting);
    }

    public void deleteFilterSetting(String filtername) throws DocException
    {
        List names = new ArrayList(Arrays.asList(getFilterSettingNames()));
        names.remove(filtername);
        String names_str = DocmaUtil.concatStrings(names, ",");
        setDocStoreProperty(getStoreId(),
                            DocmaConstants.PROP_STORE_FILTER_NAMES,
                            names_str);
    }

    public String[] getStyleIds()
    {
        String style_ids = getVersionProperty(getStoreId(), getVersionId(),
                                              DocmaConstants.PROP_VERSION_STYLE_IDS);
        if ((style_ids != null) && (style_ids.trim().length() > 0)) {
            String[] id_arr = style_ids.split(",");
            for (int i=0; i < id_arr.length; i++) {
                id_arr[i] = id_arr[i].trim();
            }
            return id_arr;
        } else {
            return new String[0];
        }
    }

    public DocmaStyle[] getStyles()
    {
        return getStyles(getStyleIds());
    }

    public DocmaStyle[] getStyles(String[] id_arr)
    {
        DocmaStyle[] s_arr = new DocmaStyle[id_arr.length];
        for (int i=0; i < id_arr.length; i++) {
            s_arr[i] = getStyle(id_arr[i]);
        }
        return s_arr;
    }

    public DocmaStyle[] getStyles(String variantId)
    {
        String[] id_arr = getStyleIds();
        SortedMap map = new TreeMap();
        for (int i=0; i < id_arr.length; i++) {
            String s_id = id_arr[i];
            int pos = s_id.indexOf(DocmaStyle.VARIANT_DELIMITER);
            if (pos < 0) {  // if s_id is a base style
                String base_id = s_id;
                if (! map.containsKey(base_id)) {
                    map.put(base_id, s_id);
                }
            } else {  // if s_id is a variant style
                String var_id = s_id.substring(pos + 1);
                if (var_id.equals(variantId)) {
                    String base_id = s_id.substring(0, pos);
                    map.put(base_id, s_id);
                }
            }
        }
        DocmaStyle[] s_arr = new DocmaStyle[map.size()];
        Iterator it = map.values().iterator();
        for (int i=0; i < s_arr.length; i++) {
            s_arr[i] = getStyle((String) it.next());
        }
        return s_arr;
    }

    public String[] getStyleVariantIds()
    {
        DocmaStyle[] styles = getStyles();
        SortedSet variants = new TreeSet();
        for (int i=0; i < styles.length; i++) {
            String var_id = styles[i].getVariantId();
            if (var_id != null) {
                variants.add(var_id);
            }
        }
        String[] arr = new String[variants.size()];
        return (String[]) variants.toArray(arr);
    }

    public DocmaStyle getStyle(String styleId)
    {
        String stype = getVersionProperty(getStoreId(), getVersionId(),
                           DocmaConstants.PROP_VERSION_STYLE_TYPE + "." + styleId);
        if ((stype == null) || stype.trim().equals("")) {
            return null;
        }
        String sname = getVersionProperty(getStoreId(), getVersionId(),
                           DocmaConstants.PROP_VERSION_STYLE_NAME + "." + styleId);
        String scss = getVersionProperty(getStoreId(), getVersionId(),
                           DocmaConstants.PROP_VERSION_STYLE_CSS + "." + styleId);
        String sautof = getVersionProperty(getStoreId(), getVersionId(),
                           DocmaConstants.PROP_VERSION_STYLE_AUTOFORMAT + "." + styleId);
        String shidd = getVersionProperty(getStoreId(), getVersionId(),
                           DocmaConstants.PROP_VERSION_STYLE_HIDDEN + "." + styleId);
        boolean is_hidden = (shidd != null) && shidd.equalsIgnoreCase("true");
        return new DocmaStyle(styleId, stype, sname, scss, sautof, is_hidden);
    }

    public DocmaStyle getStyleVariant(String baseId, String variantId)
    {
        if ((variantId != null) && (variantId.length() > 0)) {
            String style_id = baseId + DocmaStyle.VARIANT_DELIMITER + variantId;
            DocmaStyle var_style = getStyle(style_id);
            if (var_style != null) return var_style;
        }
        return getStyle(baseId);
    }

    public void saveStyle(DocmaStyle style) throws DocException
    {
        String style_ids = getVersionProperty(getStoreId(), getVersionId(),
                                              DocmaConstants.PROP_VERSION_STYLE_IDS);
        String[] id_arr;
        if ((style_ids == null) || (style_ids.trim().length() == 0)) {
            id_arr = new String[0];
        } else {
            id_arr = style_ids.split(",");
        }
        String styleid = style.getId();
        List id_list = Arrays.asList(id_arr);
        if (! id_list.contains(styleid)) {
            // add new style to list
            List ids_new = new ArrayList(id_list);
            ids_new.add(styleid);
            Collections.sort(ids_new);
            String ids_str = DocmaUtil.concatStrings(ids_new, ",");
            setVersionProperty(getStoreId(), getVersionId(),
                               DocmaConstants.PROP_VERSION_STYLE_IDS, ids_str);
        }
        String[] prop_names = new String[] {
            DocmaConstants.PROP_VERSION_STYLE_TYPE + "." + styleid,
            DocmaConstants.PROP_VERSION_STYLE_NAME + "." + styleid,
            DocmaConstants.PROP_VERSION_STYLE_CSS + "." + styleid,
            DocmaConstants.PROP_VERSION_STYLE_AUTOFORMAT + "." + styleid,
            DocmaConstants.PROP_VERSION_STYLE_HIDDEN + "." + styleid };
        String s_hidden = style.isHidden() ? "true" : "false";
        String[] prop_values = new String[] { 
            style.getType(), style.getName(), style.getCSS(), style.getAutoFormatString(), s_hidden };
        setVersionProperties(getStoreId(), getVersionId(), prop_names, prop_values);
    }

    public void deleteStyle(String styleId) throws DocException
    {
        String style_ids = getVersionProperty(getStoreId(), getVersionId(),
                                              DocmaConstants.PROP_VERSION_STYLE_IDS);
        if ((style_ids == null) || (style_ids.trim().length() == 0)) {
            return;
        }
        String[] id_arr = style_ids.split(",");
        List id_list = new ArrayList(Arrays.asList(id_arr));
        id_list.remove(styleId);
        String id_str = DocmaUtil.concatStrings(id_list, ",");
        String[] prop_names = new String[] {
            DocmaConstants.PROP_VERSION_STYLE_IDS,
            DocmaConstants.PROP_VERSION_STYLE_TYPE + "." + styleId,
            DocmaConstants.PROP_VERSION_STYLE_NAME + "." + styleId,
            DocmaConstants.PROP_VERSION_STYLE_CSS + "." + styleId,
            DocmaConstants.PROP_VERSION_STYLE_AUTOFORMAT + "." + styleId,
            DocmaConstants.PROP_VERSION_STYLE_HIDDEN + "." + styleId };
        String[] prop_values = new String[] { id_str, null, null, null, null, null };
        setVersionProperties(getStoreId(), getVersionId(), prop_names, prop_values);
    }


    public String getCSS()
    {
        return DocmaStyle.getCSS(getStyles(), false, true);
    }


    public String getCSS(String variant)
    {
        return DocmaStyle.getCSS(getStyles(variant), false, false);
    }


    public String[] getPublicationConfigIds()
    {
        return DocmaPublicationConfig.getIds(this);
    }

    public DocmaPublicationConfig getPublicationConfig(String pubConfigId)
    {
        if (pubConfigId == null) return null;
        DocmaPublicationConfig pubconf = new DocmaPublicationConfig(pubConfigId);
        pubconf.init(this, pubConfigId);
        return pubconf;
    }

    public void savePublicationConfig(DocmaPublicationConfig pubconfig) throws DocException
    {
        pubconfig.save(this);
    }

    public void deletePublicationConfig(String pubConfigId) throws DocException
    {
        DocmaPublicationConfig pubconf = getPublicationConfig(pubConfigId);
        if (pubconf != null) pubconf.delete(this);
    }

    public String[] getOutputConfigIds()
    {
        return DocmaOutputConfig.getIds(this);
    }

    public DocmaOutputConfig getOutputConfig(String outConfigId)
    {
        if (outConfigId == null) return null;
        DocmaOutputConfig outconf = new DocmaOutputConfig(outConfigId);
        outconf.init(this, outConfigId);
        return outconf;
    }

    public void saveOutputConfig(DocmaOutputConfig outconfig) throws DocException
    {
        outconfig.save(this);
    }

    public void deleteOutputConfig(String outConfigId) throws DocException
    {
        DocmaOutputConfig outconf = getOutputConfig(outConfigId);
        if (outconf != null) outconf.delete(this);
    }

    public String getDefaultPublicationFilename(String pubConfigId, String outConfigId)
    {
        return getPublicationManager().getDefaultPublicationFilename(pubConfigId, outConfigId);
    }

    public String createPublication(String pubConfigId,
                                    String outConfigId,
                                    String filename)
    {
        return getPublicationManager().createPublication(pubConfigId, outConfigId, filename);
    }

    public String createPublication(String pubConfigId,
                                    String outConfigId,
                                    String langCode,
                                    String filename)
    {
        return getPublicationManager().createPublication(pubConfigId, outConfigId, langCode, filename);
    }

    public void exportPublication(String publicationId)
    {
        getPublicationManager().exportPublication(publicationId);
    }

    public void exportPublicationAsync(String publicationId)
    {
        getPublicationManager().exportPublicationAsync(publicationId);
    }

    public int getExportJobPosition(String storeId,
                                    DocVersionId verId,
                                    String publicationId)
    {
        return PublicationManager.getExportJobPosition(storeId, verId, publicationId);
    }

    public DocmaExportJob getExportJob(String storeId,
                                       DocVersionId verId,
                                       String publicationId)
    {
        return PublicationManager.getExportJob(storeId, verId, publicationId);
    }

    public DocmaExportJob[] getExportQueue()
    {
        return PublicationManager.getExportQueue();
    }

    public String[] getPublicationIds()
    {
        return getPublicationManager().getPublicationIds();
    }

    public DocmaPublication getPublication(String publicationId)
    {
        return getPublicationManager().getPublication(publicationId);
    }

    /**
     * Returns all archived publications for the current language.
     * 
     * @return  the exported publications for the current language
     */
    public DocmaPublication[] listPublications()
    {
        String lang = getTranslationMode();
        if (lang == null) lang = getOriginalLanguage().getCode();
        return getPublicationManager().listPublications(lang);
    }

    /**
     * Returns all archived publications for the given language and release state.
     * If the <code>langCode</code> argument is <code>null</code>, then 
     * the exported publications for all languages are returned
     * (for the original language and for all translation languages).
     * If the <code>versionState</code> argument is <code>null</code>, then 
     * the exported publications for all version states are returned 
     * (draft and released publications).
     * 
     * @param langCode  the language code, or <code>null</code>
     * @param versionState  the release state, or <code>null</code>
     * @return  all exported publications for the given language and release state
     */
    public DocmaPublication[] listPublications(String langCode, String versionState)
    {
        return getPublicationManager().listPublications(langCode, versionState);
    }

    public void deletePublication(String publicationId)
    {
        getPublicationManager().deletePublication(publicationId);
    }

    public DocmaExportLog previewPDF(OutputStream outstream,
                           String node_id,
                           DocmaPublicationConfig pubConf, 
                           DocmaOutputConfig outConf)
    throws DocException
    {
        final boolean WRITE_LOG = true;
        DocmaExportContext export_ctx = new DocmaExportContext(this, pubConf, outConf, WRITE_LOG);
        previewPDF(outstream, node_id, export_ctx);
        export_ctx.finished();
        return export_ctx.getDocmaExportLog();
    }

    private void previewPDF(OutputStream outstream,
                           String node_id,
                           DocmaExportContext export_ctx)
    throws DocException
    {
        getPublicationManager().previewPDF(outstream, node_id, export_ctx);
    }

    public boolean exportsExist(String storeId, DocVersionId verId, String versionState)
    {
        return getPublicationManager().exportsExist(storeId, verId, versionState);
    }

    public void copyNodes(DocmaNode[] sourceNodes, DocmaNode targetParent, int insertPos)
    throws DocException
    {
        DocmaNode nodeAfter  = null;
        if ((insertPos >= 0) && (insertPos < targetParent.getChildCount())) {
            nodeAfter = targetParent.getChild(insertPos);
        }
        copyNodes(sourceNodes, targetParent, nodeAfter);
    }

    public void copyNodes(DocmaNode[] sourceNodes, DocmaNode targetParent, DocmaNode refChild)
    throws DocException
    {
        DocNode[] copy_nodes = new DocNode[sourceNodes.length];
        for (int i=0; i < sourceNodes.length; i++) {
            copy_nodes[i] = sourceNodes[i].getBackendNode();
        }
        DocGroup doc_parent = (DocGroup) targetParent.getBackendNode();
        DocNode node_after = (refChild == null) ? null : refChild.getBackendNode();
        
        DocStoreSession target_session = targetParent.getDocmaSession().docSess;
        Map aliasMap = new HashMap();
        ContentCopyStrategy copyStrategy = new HTMLCopyStrategy(aliasMap);
        boolean started = startLocalTransaction();
        try {
            DocStoreUtilities.copyNodesToPosition(copy_nodes, docSess,
                                                  doc_parent, node_after, target_session,
                                                  null,  // languages: null means all languages
                                                  false, // do not commit each node (for performance reasons)
                                                  DocStoreUtilities.COPY_NODE_ID_TRY_KEEP,
                                                  null,  // nodeIdMap not needed
                                                  null , // use DefaultAliasRenameStrategy
                                                  aliasMap,
                                                  copyStrategy);
            targetParent.clearLocalCache();
            targetParent.recalculateSectionAttributes();
            commitLocalTransaction(started);
        } catch (Exception ex) {
            rollbackLocalTransaction(started);
            if (ex instanceof DocException) throw (DocException) ex;
            else throw new DocException(ex);
        }
    }

    public File exportNodesToFile(DocmaNode[] nodes, File exportFile)
    {
        return ImportExportUtil.exportNodesToFile(this, nodes, exportFile);
    }

    public DocmaLanguage[] getImportTranslationLanguages(File importDir)
    {
        String[] lang_codes = ImportExportUtil.getImportTranslationLanguages(importDir);
        DocmaLanguage[] langs = new DocmaLanguage[lang_codes.length];
        ContentLanguages all_langs = docmaApp.getContentLanguages();
        for (int i=0; i < langs.length; i++) {
            langs[i] = all_langs.getLanguage(lang_codes[i]);
        }
        return langs;
    }

    public void importNodes(DocmaNode parentNode, File importDir, DocmaLanguage[] langs)
    {
        String[] lang_codes = new String[langs.length];
        for (int i=0; i < langs.length; i++) {
            lang_codes[i] = langs[i].getCode();
        }
        ImportExportUtil.importNodes(this, parentNode, importDir, lang_codes);
    }

    public void clearRevisions(String storeId)
    {
        DocVersionId[] vids = listVersions(storeId);
        for (DocVersionId verId : vids) {
            getRevisionStore().clearRevisions(storeId, verId);
        }
    }
    
    public void clearRevisions(String storeId, DocVersionId verId)
    {
        getRevisionStore().clearRevisions(storeId, verId);
    }

    public String[] getTextFileExtensions()
    {
        return docmaApp.getTextFileExtensions();
    }
    
    public boolean isTextFileExtension(String ext)
    {
        return docmaApp.isTextFileExtension(ext);
    }

    public Activity getActivityById(long activityId)
    {
        return docmaApp.getActivities().getActivityById(activityId);
    }
    
    public Activity getDocStoreActivity(String storeId)
    {
        UUID store_uuid = docSess.getDocStoreUUID(storeId);
        if (store_uuid != null) {
            return docmaApp.getActivities().getStoreActivity(store_uuid);
        } else {
            return null;
        }
    }
    
    public Activity[] getDocStoreActivities(String storeId, DocVersionId verId, String userId)
    {
        UUID ver_uuid = docSess.getVersionUUID(storeId, verId);
        if (ver_uuid != null) {
            return docmaApp.getActivities().getVersionActivities(ver_uuid, userId);
        } else {
            return new Activity[0];
        }
    }

    public Activity[] getOpenedStoreUserActivities()
    {
        String sid = getStoreId();
        DocVersionId vid = getVersionId();
        if ((sid != null) && (vid != null)) {
            return getDocStoreActivities(sid, vid, getUserId());
        }
        return new Activity[0];
    }
    
    public Activity createDocStoreActivity(String storeId) throws DocException
    {
        UUID store_uuid = docSess.getDocStoreUUID(storeId);
        if (store_uuid != null) {
            return docmaApp.getActivities().createStoreActivity(store_uuid, getUserId());
        } else {
            throw new DocException("Cannot create store activity. Store with ID '" + 
                                   storeId + "' not found!");
        }
    }

    public Activity createDocStoreActivity(String storeId, DocVersionId verId) throws DocException
    {
        UUID ver_uuid = docSess.getVersionUUID(storeId, verId);
        if (ver_uuid != null) {
            return docmaApp.getActivities().createVersionActivity(ver_uuid, getUserId());
        } else {
            throw new DocException("Cannot create activity. Store with ID '" + 
                                   storeId + "' version '" + verId + "' not found!");
        }
    }
    
    public boolean removeDocStoreActivity(String storeId)
    {
        UUID store_uuid = docSess.getDocStoreUUID(storeId);
        if (store_uuid != null) {
            return docmaApp.getActivities().removeStoreActivity(store_uuid);
        } else {
            return false;
        }
    }

    public boolean removeDocStoreActivity(String storeId, DocVersionId verId, long activityId)
    {
        return docmaApp.getActivities().removeVersionActivity(activityId);
    }

    /**
     * Clear all finished user activities for the currently opened store.
     * This removes all finished user activities for the user that owns this
     * session. Activities of other users are not affected.
     * 
     * @return <code>true</code> if no more user activities exist;
     *         <code>false</code> otherwise
     */
    public boolean clearFinishedUserActivities()
    {
        String sid = getStoreId();
        DocVersionId vid = getVersionId();
        if ((sid != null) && (vid != null)) {
            return clearFinishedUserActivities(sid, vid);
        } else {
            return true;  // no store is currently opened; do nothing
        }
    }
    
    /**
     * Clear all finished user activities for the specified store.
     * This removes all finished user activities for the user that owns this
     * session. Activities of other users are not affected.
     * 
     * @param storeId  the store identifier
     * @param verId    the version identifier of the store
     * @return <code>true</code> if no more user activities exist for the given store;
     *         <code>false</code> otherwise
     */
    public boolean clearFinishedUserActivities(String storeId, DocVersionId verId)
    {
        try {
            Activity[] acts = getDocStoreActivities(storeId, verId, getUserId());
            boolean cleared = true;
            for (Activity a : acts) {
                if (a.isFinished()) {
                    if (! removeDocStoreActivity(storeId, 
                                                 verId, 
                                                 a.getActivityId())) {
                        cleared = false;  // finished activity could not be removed
                    }
                } else {
                    cleared = false;  // running activity exists
                }
            }
            return cleared;
        } catch (Exception ex) {  // should never occur
            ex.printStackTrace();
            return false;
        }
    }
    
    public boolean isStoreDisabled(String storeId) 
    {
        return docmaApp.isStoreDisabled(storeId);
    }
    
    public void setStoreDisabled(String storeId, boolean isDisabled)
    {
        docmaApp.setStoreDisabled(storeId, isDisabled);
    }
}
