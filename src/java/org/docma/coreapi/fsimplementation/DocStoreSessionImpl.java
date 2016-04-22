/*
 * DocStoreSessionImpl.java
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
 * 
 * Created on 19. Oktober 2007, 19:55
 */

package org.docma.coreapi.fsimplementation;

import java.io.*;
import java.util.*;
import org.docma.app.DocmaConstants;
import org.docma.coreapi.*;
import org.docma.coreapi.implementation.*;
import org.docma.coreapi.dbimplementation.*;
import org.docma.hibernate.*;
import org.docma.util.*;
import org.w3c.dom.*;

/**
 *
 * @author MP
 */
public class DocStoreSessionImpl extends AbstractDocStoreSession implements DocStoreSession
{
    private static final String DDL_LOG_DIR = "ddl_log";
    private static final String VERSION_PROP_FILENAME = "version.properties";
    private static final String PROP_VERSION_DERIVED_FROM = PROP_VERSION_PREFIX + "derivedfrom";
    
    private File storesDir;
    private boolean running_transaction = false;
    // Cached store properties:
    private Map<String, StorePropertiesLoader> propLoaders = new HashMap<String, StorePropertiesLoader>();  

    //
    // Fields of currently opened store
    //
    private File storeDir;
    private String storeId = null;
    private DocVersionId versionId = null;

    //
    // Fields used for filesystem-based stores (STORE_TYPE_FS)
    //
    // Cached version properties:
    private Map<String, Map<String, PropertiesLoader>> versionPropLoaders = 
            new HashMap<String, Map<String, PropertiesLoader>>();
    // Changed store and version properties during transaction:
    private Set<PropertiesLoader> changedProps = new HashSet<PropertiesLoader>();
    private File versionDir;                   // Directory of opened filesystem-based store.
    private DocGroupImpl rootGroup;            // Root group of opened filesystem-based store.
    private Map aliasCache = new HashMap();    // Cached aliases for opened filesystem-based store.

    //
    // Fields used for database stores (STORE_TYPE_DB)
    //
    private DocStoreDbConnection dbOpenedConnection;
    // Open connections during transaction:
    private Map<String, DocStoreDbConnection> dbConnectionPool = new HashMap<String, DocStoreDbConnection>(); 


    static {
        // Internal store properties
        internalStoreProps.add(FilesystemStoreProperties.PROP_STORE_BASEPATH);
        internalStoreProps.add(FilesystemStoreProperties.PROP_STORE_PATH);
        internalStoreProps.add(FilesystemStoreProperties.PROP_STORE_TYPE);
        
        // Internal version properties
        internalVersionProps.add(PROP_VERSION_DERIVED_FROM);
    }
    
    /** Creates a new instance of DocStoreSessionImpl */
    public DocStoreSessionImpl(DocStoreManagerImpl dsm, String sessId, String user) 
    {
        super(dsm, sessId, user);

        storesDir = new File(dsm.getStoresDirectory());
        // userdataDir = new File(storeManager.getBaseDirectory(), "userdata");
    }

    /* --------------  Private methods ------------------ */

    private String getStorePath(String store_id) 
    {
        return store_id;
    }

    private File getVersionDir(File storeDir, DocVersionId verId) {
        String[] subdirs = storeDir.list();
        String ver_str = verId.toString();
        for (int i=0; i < subdirs.length; i++) {
            String dirname = subdirs[i];
            if (dirname.equalsIgnoreCase(ver_str)) {
                return new File(storeDir, dirname);
            }
        }
        return new File(storeDir, ver_str);
    }

    private StorePropertiesLoader getPropLoader(String storeId) {
        StorePropertiesLoader ploader = (StorePropertiesLoader) propLoaders.get(storeId);
        if (ploader == null) {
            File baseDir = new File(storesDir, getStorePath(storeId));
            ploader = new StorePropertiesLoader(storeId, baseDir, storeManager);
            propLoaders.put(storeId, ploader);
        }
        return ploader;
    }

    private PropertiesLoader getVersionPropLoader(String storeId, DocVersionId verId) {
        Map<String, PropertiesLoader> vermap = versionPropLoaders.get(storeId);
        if (vermap == null) {
            vermap = new HashMap<String, PropertiesLoader>();
            versionPropLoaders.put(storeId, vermap);
        }
        PropertiesLoader ploader = (PropertiesLoader) vermap.get(verId.toString());
        if (ploader == null) {
            File sDir = getStoreDirFromId(storeId);
            File vDir = getVersionDir(sDir, verId);
            if (! vDir.exists()) {
                throw new DocRuntimeException("Version path does not exist: " + vDir.getAbsolutePath());
            }
            File propFile = new File(vDir, VERSION_PROP_FILENAME);
            try {
                if (! propFile.exists()) {
                    propFile.createNewFile();
                }
                ploader = new PropertiesLoader(propFile);
                ploader.setComments("Document version properties.");
                vermap.put(verId.toString(), ploader);
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new DocRuntimeException("Could not read properties file: " + propFile.getAbsolutePath());
            }
        }
        return ploader;
    }
    
    private void refreshPropLoaders()
    {
        for (StorePropertiesLoader pl : propLoaders.values()) {
            if (pl == null) continue;
            try { 
                pl.refresh();
            } catch (Exception ex) {
                Log.warning("Could not refresh store properties: " + ex.getMessage());
            }
        }
        for (Map<String, PropertiesLoader> vmap : versionPropLoaders.values()) {
            if (vmap == null) continue;
            for (PropertiesLoader pl : vmap.values()) {
                if (pl == null) continue;
                try { 
                    pl.refresh();
                } catch (Exception ex) {
                    Log.warning("Could not refresh store properties: " + ex.getMessage());
                }
            }
        }
    }

    private void listIds_recursive(DocNode node, Class node_class, SortedSet id_set)
    {
        if (node_class.isAssignableFrom(node.getClass())) {
            id_set.add(node.getId());
        }
        
        if (node instanceof DocGroup) {
            DocNode[] child_nodes = ((DocGroup) node).getChildNodes();
            for (int i=0; i < child_nodes.length; i++) {
                listIds_recursive(child_nodes[i], node_class, id_set);
            }
        }
    }
    
    private void listAliases_recursive(DocNode node, Class node_class, SortedSet alias_set)
    {
        if (node_class.isAssignableFrom(node.getClass())) {
            String[] arr = node.getAliases();
            for (int i=0; i < arr.length; i++) {
                alias_set.add(arr[i]);
            }
        }
        
        if (node instanceof DocGroup) {
            DocNode[] child_nodes = ((DocGroup) node).getChildNodes();
            for (int i=0; i < child_nodes.length; i++) {
                listAliases_recursive(child_nodes[i], node_class, alias_set);
            }
        }
    }
    
    private void listNodeInfos_recursive(DocNode node, Class node_class, List<NodeInfo> info_list)
    {
        if (node_class.isAssignableFrom(node.getClass())) {
            info_list.add(new NodeInfoImpl(node));
        }
        
        if (node instanceof DocGroup) {
            DocNode[] child_nodes = ((DocGroup) node).getChildNodes();
            for (int i=0; i < child_nodes.length; i++) {
                listNodeInfos_recursive(child_nodes[i], node_class, info_list);
            }
        }
    }
    
    DocStoreDbConnection acquireDbConnection(String store_id)
    {
        if ((this.storeId != null) && this.storeId.equals(store_id)) {
           return this.dbOpenedConnection;  // should never be null!
        } else {
           // Check if DB connection already exists within a running transaction
           DocStoreDbConnection dbcon = null;
           if (this.running_transaction) {
               dbcon = dbConnectionPool.get(store_id); 
           }
           if (dbcon == null) {
               DbConnectionData con_data = getDbConnectionData(store_id);
               dbcon = new DocStoreDbConnection(con_data, this.storeManager, this);
               if (this.running_transaction) {
                   try {
                       dbcon.startTransaction();
                   } catch (DocException ex) {
                       throw new DocRuntimeException(ex);
                   }
                   dbConnectionPool.put(store_id, dbcon);
               }
           }
           return dbcon;
        }
    }
    
    void releaseDbConnection(String store_id, DocStoreDbConnection dbcon)
    {
        if ((this.storeId != null) && this.storeId.equals(store_id)) {  // store is currently opened
            return;   // do nothing; DB connection is closed in method closeDocStore();
        }
        DocStoreDbConnection pool_con = dbConnectionPool.get(store_id); 
        if (dbcon == pool_con) {
            if (this.running_transaction) {
                return;  // do nothing; DB connections are closed at commit/rollback
            }
            dbConnectionPool.remove(store_id);  // should never be reached
        }
        dbcon.close();
    }
    
    private DbConnectionData getDbEmbeddedConnectionData(String store_id) 
    {
        File store_dir = getStoreDirFromId(store_id);
        return StorePropertiesLoader.getDbEmbeddedConnectionData(store_dir);
    }
    
    private void writeDbExternalConnectionData(File store_dir, DbConnectionData con_data) 
    {
        StorePropertiesLoader.writeDbExternalConnectionData(store_dir, con_data);
    }

    /* --------------  Package local methods ------------------ */

    AbstractDocStore getDocStore() 
    {
        return docStore;
    }

    /**
     * This method is only used by the filesystem-based node implementation.
     */
    DocStoreImpl getDocStoreFs() 
    {
        return (DocStoreImpl) docStore;
    }

    /**
     * Clears the cached alias names.
     * This method is only used by the filesystem-based node implementation.
     */
    synchronized void refreshAliasList()
    {
        aliasCache.clear();
    }


    /* --------------  Public methods ------------------ */

    public File getStoreDirFromId(String store_id) 
    {
        StorePropertiesLoader ploader = getPropLoader(store_id);
        return ploader.getStoreDir();
    }

    public boolean isDbStore(String store_id)
    {
        StorePropertiesLoader ploader = getPropLoader(store_id);
        return ploader.isDbStore();
    }
    
    public boolean isDbEmbeddedStore(String store_id)
    {
        StorePropertiesLoader ploader = getPropLoader(store_id);
        return ploader.isDbEmbeddedStore();
    }

    public boolean isDbExternalStore(String store_id)
    {
        StorePropertiesLoader ploader = getPropLoader(store_id);
        return ploader.isDbExternalStore();
    }

    public DbConnectionData getDbConnectionData(String store_id) 
    {
        StorePropertiesLoader ploader = getPropLoader(store_id);
        return ploader.getDbConnectionData();
    }

    /**
     * This method is only used by the filesystem-based node implementation.
     */
    public DocNode createDocNodeFromDOMElement(Element elem)
    {
        if (elem.getTagName().equals(XMLConstants.TAG_GROUP)) {
            return new DocGroupImpl(this, elem);
        } else
        if (elem.getTagName().equals(XMLConstants.TAG_CONTENT)) {
            String content_class = elem.getAttribute(XMLConstants.ATTR_CONTENT_CLASS);
            DocContentImpl content;
            if (content_class.equals(XMLConstants.CONTENT_CLASS_IMAGE)) {
                content = new DocImageImpl(this, elem);
            } else
            if (content_class.equals(XMLConstants.CONTENT_CLASS_FILE)) {
                content = new DocFileImpl(this, elem);
            } else {  // if (content_class.equals(XMLConstants.CONTENT_CLASS_XML))
                content = new DocXMLImpl(this, elem);
            }
            return content;
        } else
        if (elem.getTagName().equals(XMLConstants.TAG_REFERENCE)) {
            return new DocReferenceImpl(this, elem);
        }
        return null;
    }

    /**
     * This method returns the version directory of the currently opened 
     * filesystem-based store. For database stores this method returns null.
     * @return 
     */
    public String getOpenedDocStoreDir()
    {
        if (storeId == null) {   // no store is opened
            return null;
        } else {
            if (isDbStore(storeId)) return null;
        }
        if (versionDir == null) return null;
        else return versionDir.getAbsolutePath();
    }

    public File getDocStoreDir(String storeId, DocVersionId verId)
    {
        return getVersionDir(getStoreDirFromId(storeId), verId);
    }

    /* --------------  Implementation of abstract methods -------------- */

    protected void onReleaseTranslation(String storeId, DocVersionId verId, String lang)
    throws DocException
    {
        //
        // Database implementation
        //
        if (isDbStore(storeId)) {
            synchronized (this) {
                DocStoreDbConnection dbcon = acquireDbConnection(storeId);
                try {
                    dbcon.onReleaseTranslation(storeId, verId, lang);
                } finally {
                    releaseDbConnection(storeId, dbcon);
                }
            }
            return;
        }

        //
        // Filesystem-based implementation
        //
        String lang_ext = "." + lang.toLowerCase();
        String[] names = {
            PROP_VERSION_STATE + lang_ext,
            PROP_VERSION_RELEASE_DATE + lang_ext };
        String[] values = { DocVersionState.DRAFT, "" };
        File dir = getStoreDirFromId(storeId);
        File sourceDir = getVersionDir(dir, verId);
        File sourceTransDir = new File(sourceDir, "translations");
        File sourceLangDir = new File(sourceTransDir, lang);
        DocVersionId[] subs = getSubVersions(storeId, verId);
        for (int i=0; i < subs.length; i++) {
            String sub_state = getVersionState(storeId, subs[i], lang);
            if (sub_state.equalsIgnoreCase(DocVersionState.RELEASED)) {
                // Something went wrong. Never overwrite released version!
                throw new DocRuntimeException("Expected version state PENDING. Found RELEASED.");
            }
            if (! sub_state.equalsIgnoreCase(DocVersionState.TRANSLATION_PENDING)) {
                Log.warning("Expected version state PENDING. Found: " + sub_state);
            }
            // Copy files to derived versions
            File destDir = getVersionDir(dir, subs[i]);
            File destTransDir = new File(destDir, "translations");
            File destLangDir = new File(destTransDir, lang);
            if (! DocmaUtil.recursiveFileCopy(sourceLangDir, destLangDir, true)) {
                throw new DocRuntimeException("Could not copy files from " +
                    sourceLangDir + " to " + destLangDir);
            }
            // If file copy was successful,
            // change state of derived version from pending to draft
            setVersionProperties(storeId, subs[i], names, values);
        }
    }


    protected void setTranslationBackToPending(String storeId, DocVersionId verId, String lang)
    throws DocException
    {
        //
        // Database implementation
        //
        if (isDbStore(storeId)) {
            synchronized (this) {
                DocStoreDbConnection dbcon = acquireDbConnection(storeId);
                try {
                    dbcon.setTranslationBackToPending(storeId, verId, lang);
                } finally {
                    releaseDbConnection(storeId, dbcon);
                }
            }
            return;
        }

        //
        // Filesystem-based implementation
        //
        String lang_ext = "." + lang.toLowerCase();
        String[] names = {
            PROP_VERSION_STATE + lang_ext,
            PROP_VERSION_RELEASE_DATE + lang_ext };
        String[] values = { DocVersionState.TRANSLATION_PENDING, "" };
        setVersionProperties(storeId, verId, names, values);

        try {
            File dir = getStoreDirFromId(storeId);
            File destDir = getVersionDir(dir, verId);
            File transDir = new File(destDir, "translations");
            File langDir = new File(transDir, lang);
            DocmaUtil.recursiveFileDelete(langDir);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    /* --------------  Interface DocStoreSession ------------------ */
    
    public String getStoreId()
    {
        return storeId;
    }

    public DocVersionId getVersionId()
    {
        return versionId;
    }

    public void openDocStore(String storeId, DocVersionId verId)
    {
        if (runningTransaction()) {
            throw new DocRuntimeException("Cannot open document store during a transaction.");
        }
        if (this.storeId != null) closeDocStore();
        
        //
        // Database implementation
        //
        if (isDbStore(storeId)) {
            synchronized (this) {
                DocStoreDbConnection dbcon = acquireDbConnection(storeId);
                try {
                    try {
                        dbcon.openDocStore(storeId, verId);
                    } catch (DocRuntimeException dre) {
                        String[] store_ids = dbcon.listDocStores();
                        if ((store_ids.length == 1) && !store_ids[0].equals(storeId)) {
                            Log.error("Store id mismatch: Trying to rename store '" + 
                                      store_ids[0] + "' to '" + storeId + "'.");
                            dbcon.changeDocStoreId(store_ids[0], storeId);
                            Log.info("Store rename succeeded!");
                            dbcon.openDocStore(storeId, verId);  // retry to open store 
                        } else {
                            throw dre;
                        }
                    }
                    this.storeId = storeId;
                    this.storeDir = getStoreDirFromId(storeId);
                    this.docStore = dbcon.getDocStore();
                    if (verId == null) verId = dbcon.getLatestVersionId(storeId);
                    this.versionId = verId;
                    this.dbOpenedConnection = dbcon;
                    // Connection should not be in pool, as no transaction is running.
                    // However, if something went wrong, remove connection from pool:
                    this.dbConnectionPool.remove(storeId); 
                } catch (Throwable ex) {  // opening failed
                    this.storeId = null;
                    this.storeDir = null;
                    this.docStore = null;
                    this.versionId = null;
                    this.dbOpenedConnection = null;
                    releaseDbConnection(storeId, dbcon);  // close DB connection if store could not be opened
                    throw new DocRuntimeException(ex);
                }
            }
            return;
        }
        
        //
        // Filesystem-based implementation
        //
        this.storeId = storeId;
        if (verId == null) verId = getLatestVersionId(storeId);
        this.versionId = verId;

        this.storeDir = getStoreDirFromId(storeId);
        if (! this.storeDir.exists()) {
            throw new DocRuntimeException("Store path does not exist: " + this.storeDir.getAbsolutePath());
        }
        this.versionDir = getVersionDir(storeDir, verId);

        docStore = (DocStoreImpl) storeManager.acquireStore(this, storeId, versionId);
        rootGroup = new DocGroupImpl(this, getDocStoreFs().getIndexDocument().getDocumentElement());
        rootGroup.setParentGroup(null);  // this is the root

        aliasCache.clear();
        onOpenDocStore();
    }

    public void closeDocStore()
    {
        if (this.storeId == null) return;
        if (runningTransaction()) {
            throw new DocRuntimeException("Cannot close document store during a transaction.");
        }

        //
        // Database implementation
        //
        if (isDbStore(this.storeId)) {
            synchronized (this) {
                storeId = null;
                versionId = null;
                storeDir = null;
                try {
                    dbOpenedConnection.closeDocStore();
                } catch (Exception ex) { ex.printStackTrace(); }
                try {
                    dbOpenedConnection.close();
                } finally {
                    dbOpenedConnection = null;
                    docStore = null;
                }
            }
            return;
        }
        
        //
        // Filesystem-based implementation
        //
        onCloseDocStore();
        storeManager.releaseStore(this, storeId, versionId);
        storeId = null;
        versionId = null;
        storeDir = null;
        versionDir = null;
        docStore = null;
        rootGroup = null;
        aliasCache.clear();
    }

    public String[] listDocStores()
    {
        return storesDir.list();
    }
    
    public void addDocStore(String storeId, String[] propNames, String[] propValues) throws DocException
    {
        createDocStore(storeId, propNames, propValues, false);
    }

    public void createDocStore(String storeId, String[] propNames, String[] propValues) throws DocException
    {
        createDocStore(storeId, propNames, propValues, true);
    }
    
    public void createDocStore(String storeId, String[] propNames, String[] propValues, boolean create) throws DocException
    {
        // Note: If only the property PROP_STORE_PATH is supplied, then no
        // new store is created, but the store at the given path is added.
        
        File f = new File(storesDir, getStorePath(storeId));
        if (! f.mkdirs()) throw new DocException("Could not create DocStore: " + f.getAbsolutePath());
        // String[] names = { FilesystemStoreProperties.PROP_STORE_BASEPATH,
        //                    FilesystemStoreProperties.PROP_STORE_PATH };
        // String[] values = { f.getAbsolutePath(),
        //                     "" };   // empty string means contents are stored in basepath

        // Assure that the properties file is written, even if no properties are supplied.
        // For filesystem-based stores, the existence of the properties file is 
        // required, because it is used to identify the store as a filesystem-based 
        // store (see method isDbStore()).
        if (propNames == null) {
            propNames = new String[0];
            propValues = new String[0];
        }
        List<String> pn = new ArrayList<String>(Arrays.asList(propNames));
        List<String> pv = new ArrayList<String>(Arrays.asList(propValues));
        
        // Create path file, if an external store path is given
        int idx = pn.indexOf(FilesystemStoreProperties.PROP_STORE_PATH);
        if (idx >= 0) {
            String external_path = pv.get(idx);
            StorePropertiesLoader.createPathFile(f, external_path);
            pn.remove(idx);
            pv.remove(idx);
        }
        
        boolean is_fs = true; // if store type is not set, then create filesystem store 
        boolean is_db_embedded = false;
        boolean is_db_external = false;
        idx = pn.indexOf(FilesystemStoreProperties.PROP_STORE_TYPE);
        int dburl_idx = pn.indexOf(FilesystemStoreProperties.PROP_DB_CONNECTION_URL);
        if ((idx >= 0) || (dburl_idx >= 0)) {
            String stype = null;
            if (idx >= 0) {
                stype = pv.get(idx);
                // Remove store type property, because this property is only required 
                // for store creation. After creation the store type is determined
                // from the content of the store directory (see method isDbStore()).
                pn.remove(idx);
                pv.remove(idx);
            }
            
            if (stype != null) {
                is_fs = stype.equalsIgnoreCase(FilesystemStoreProperties.STORE_TYPE_FS);
                is_db_embedded = stype.equalsIgnoreCase(FilesystemStoreProperties.STORE_TYPE_DB_EMBEDDED);
                is_db_external = stype.equalsIgnoreCase(FilesystemStoreProperties.STORE_TYPE_DB_EXTERNAL);
                if (! (is_fs || is_db_embedded || is_db_external)) {
                    throw new DocException("Unknown store type: " + stype);
                }
            } else {
                if (dburl_idx >= 0) {
                    is_db_external = true;
                    is_db_embedded = false;
                    is_fs = false;
                }
            }
            if (is_db_embedded) {
                // Create database instance and tables
                if (create) {
                    DbConnectionData con_data = getDbEmbeddedConnectionData(storeId);
                    DbUtil.initDatabase(con_data, new File(f, DDL_LOG_DIR));
                }
            } else
            if (is_db_external) {
                DbConnectionData con_data = new DbConnectionData();
                con_data.setDriverClassName(
                  removePropFromList(pn, pv, FilesystemStoreProperties.PROP_DB_DRIVER_CLASS));
                con_data.setConnectionURL(
                  removePropFromList(pn, pv, FilesystemStoreProperties.PROP_DB_CONNECTION_URL));
                con_data.setDbDialect(
                  removePropFromList(pn, pv, FilesystemStoreProperties.PROP_DB_DIALECT));
                con_data.setUserId(
                  removePropFromList(pn, pv, FilesystemStoreProperties.PROP_DB_CONNECTION_USER));
                con_data.setUserPwd(
                  removePropFromList(pn, pv, FilesystemStoreProperties.PROP_DB_CONNECTION_PWD));
                
                writeDbExternalConnectionData(f, con_data);
                
                // Create tables if not already existing 
                if (create) {
                    try {
                        if (! DbUtil.checkTablesExist(con_data)) {
                            DbUtil.initDatabase(con_data, new File(f, DDL_LOG_DIR));
                        }
                    } catch (Exception ex) {
                        // If connection data is wrong this will lead to an exception
                        throw new DocException("Could not initialize to database: " + 
                                               ex.getMessage() + 
                                               " Connection-URL: " + con_data.getConnectionURL());
                    }
                }
            }
        }
        if (create) {
            if (propNames.length != pn.size()) {  // if properties have been removed
                propNames = pn.toArray(new String[pn.size()]);
                propValues = pv.toArray(new String[pv.size()]);
            }
            if (is_db_embedded || is_db_external) {
                // Create store instance in database 
                synchronized (this) {
                    DocStoreDbConnection dbcon = acquireDbConnection(storeId);
                    try {
                        dbcon.createDocStore(storeId, propNames, propValues);
                    } finally {
                        releaseDbConnection(storeId, dbcon);
                    }
                }
            } else {  // if (is_fs)
                // Write properties file for filesystem-based store
                setDocStoreProperties(storeId, propNames, propValues);
            }
        }
    }
    
    private String removePropFromList(List<String> pnames, List<String> pvalues, String prop_name) 
    {
        int p_idx;
        if ((p_idx = pnames.indexOf(prop_name)) >= 0) {
            String prop_val = pvalues.get(p_idx);
            pnames.remove(p_idx);
            pvalues.remove(p_idx);
            return prop_val;
        } else {
            return null;
        }
    }

    public void deleteDocStore(String storeId) throws DocException
    {
        deleteDocStore(storeId, false);
    }
    
    public void deleteDocStore(String storeId, boolean remove_connection_only) throws DocException
    {
        if (isDbExternalStore(storeId) && !remove_connection_only) {
            //  remove not only the connection, but also remove store from database
            synchronized (this) {
                DocStoreDbConnection dbcon = acquireDbConnection(storeId);
                try {
                    dbcon.deleteDocStore(storeId);
                } finally {
                    releaseDbConnection(storeId, dbcon);
                }
            }
        } else {
            try {
                // Delete in-memory representation of store.
                storeManager.destroyStoreInstances(storeId); // throws runtime exception if users are still connected
            } catch (Exception ex) {
                throw new DocException(ex);
            }
        }
        propLoaders.remove(storeId);  // Remove cached store properties
        versionPropLoaders.remove(storeId);
        
        // Delete store directory
        if (isDbStore(storeId)) {
            // Release database connections to (embedded) database store.
            // Otherwise deletion of embedded database directory might fail.
            DbUtil.releaseDatabase(getDbConnectionData(storeId));
        }
        File f = new File(storesDir, getStorePath(storeId));
        if (! DocmaUtil.recursiveFileDelete(f)) {
            throw new DocException("Could not delete store directory: " + f);
        }
    }
    
    public synchronized void changeDocStoreId(String oldId, String newId) throws DocException
    {
        File fold = new File(storesDir, getStorePath(oldId));
        File fnew = new File(storesDir, getStorePath(newId));
        if (fold.exists()) {
            boolean is_db = isDbStore(oldId);
            String[] uids = storeManager.getConnectedUsers(oldId);
            if (uids.length > 0) {
                throw new DocException("Could not change DocStore Id. Users are still connected.");
            }
            try {
                // Delete in-memory representation of store
                storeManager.destroyStoreInstances(oldId); // throws runtime exception if users are still connected
            } catch (Exception ex) {
                throw new DocException(ex);
            }
            if (is_db) {
                // Release database connections to (embedded) database store.
                // Otherwise rename of embedded database directory might fail.
                DbUtil.releaseDatabase(getDbConnectionData(oldId));
            }
            if (! fold.renameTo(fnew)) {
                throw new DocException("Could not change DocStore Id.");
            }
            try {
                if (is_db) {
                    // Folder has been renamed. Now also change the display id in the database. 
                    DocStoreDbConnection dbcon = acquireDbConnection(newId);
                    try {
                        try {
                            List all_ids = Arrays.asList(dbcon.listDocStores());
                            boolean do_nothing = all_ids.contains(newId) && !all_ids.contains(oldId);
                            // Note: If database does not contain oldId and 
                            //       newId already exists, then renaming of
                            //       directory is sufficient. This occurs if
                            //       user created connection data with wrong id 
                            //       and now directory name needs to be 
                            //       corrected to be consistent with id stored 
                            //       in database.
                            if (! do_nothing) {
                                dbcon.changeDocStoreId(oldId, newId);
                            }
                        } finally {
                            releaseDbConnection(newId, dbcon);
                        }
                    } catch (Exception ex) {
                        // If rename in database failed, then try to undo rename of folder
                        String err_msg = "Rename of display id in database failed.";
                        DbUtil.releaseDatabase(getDbConnectionData(newId));
                        if (! fnew.renameTo(fold)) {
                            err_msg += " Please rename the folder '" + fnew + 
                                       "' back to '" + fold + "'.";
                        }
                        throw new DocException(err_msg, ex);
                    }
                }
            } finally {
                propLoaders.remove(oldId);
                versionPropLoaders.remove(oldId);
            }
        } else {
            throw new DocException("Cannot change DocStore Id. DocStore does not exist.");
        }
    }

    public DocVersionId[] listVersions(String storeId) 
    {
        //
        // Database implementation
        //
        if (isDbStore(storeId)) {
            DocVersionId[] res = null;
            synchronized (this) {
                DocStoreDbConnection dbcon = acquireDbConnection(storeId);
                try {
                    res = dbcon.listVersions(storeId);
                } finally {
                    releaseDbConnection(storeId, dbcon);
                }
            }
            return res;
        }
        
        //
        // Filesystem-based implementation
        //
        VersionIdFactory idfactory = storeManager.getVersionIdFactory();
        File versionsDir = getStoreDirFromId(storeId);
        File[] dirnames = versionsDir.listFiles();
        ArrayList verIds = new ArrayList(dirnames.length);
        for (int i = 0; i < dirnames.length; i++) {
            File f = dirnames[i];
            if (f.isDirectory()) {
                try {
                    DocVersionId verid = idfactory.createVersionId(f.getName());
                    verIds.add(verid);
                } catch (DocException ex) {
                    Log.error("Invalid version folder: " + f.getAbsolutePath());
                }
            }
        }
        Collections.sort(verIds);
        DocVersionId[] verIdArr = new DocVersionId[verIds.size()];
        return (DocVersionId[]) verIds.toArray(verIdArr);
    }

    public DocVersionId getLatestVersionId(String storeId) 
    {
        //
        // Database implementation
        //
        if (isDbStore(storeId)) {
            DocVersionId res = null;
            synchronized (this) {
                DocStoreDbConnection dbcon = acquireDbConnection(storeId);
                try {
                    res = dbcon.getLatestVersionId(storeId);
                } finally {
                    releaseDbConnection(storeId, dbcon);
                }
            }
            return res;
        }
        
        //
        // Filesystem-based implementation
        //
        DocVersionId[] verids = listVersions(storeId);
        if ((verids != null) && (verids.length > 0)) {
            return verids[verids.length - 1];
        } else {
            return null;
        }
    }

    public void deleteVersion(String storeId, DocVersionId verId)
    throws DocException
    {
        //
        // Database implementation
        //
        if (isDbStore(storeId)) {
            synchronized (this) {
                DocStoreDbConnection dbcon = acquireDbConnection(storeId);
                try {
                    dbcon.deleteVersion(storeId, verId);
                } finally {
                    releaseDbConnection(storeId, dbcon);
                }
            }
            return;
        }
        
        //
        // Filesystem-based implementation
        //
        String[] uids = storeManager.getConnectedUsers(storeId, verId);
        if (uids.length > 0) {
            throw new DocException("Cannot delete version. Users are still connected.");
        }
        try {
            // Delete in-memory representation of version
            storeManager.destroyStoreInstance(storeId, verId); // throws runtime exception if users are still connected
        } catch (Exception ex) {
            throw new DocException(ex);
        }

        File dir = getStoreDirFromId(storeId);
        File verDir = getVersionDir(dir, verId);
        if (! verDir.exists()) {
            throw new DocException("Cannot delete version " + verId +
                ". Directory " + verDir.getAbsolutePath() + " does not exist.");
        }
        if (! DocmaUtil.recursiveFileDelete(verDir)) {
            throw new DocException("Could not delete version directory: " + verDir);
        }
        versionPropLoaders.remove(storeId);  // clear cached version properties
    }

    public int deleteAllVersions(String storeId)
    throws DocException
    {
        return deleteAllVersions(storeId, null);
    }
    
    public int deleteAllVersions(String storeId, ProgressCallback progress)
    throws DocException
    {
        //
        // Database implementation
        //
        if (isDbStore(storeId)) {
            // Use database specific implementation which is more efficient
            // than the default implementation.
            int cnt = 0;
            synchronized (this) {
                DocStoreDbConnection dbcon = acquireDbConnection(storeId);
                try {
                    cnt = dbcon.deleteAllVersions(storeId, progress);
                } finally {
                    releaseDbConnection(storeId, dbcon);
                }
            }
            return cnt;
        }
        
        //
        // Filesystem-based implementation
        //
        return super.deleteAllVersions(storeId, progress);  // use default implementation
    }
    
    public DocVersionId getVersionDerivedFrom(String storeId, DocVersionId verId)
    {
        //
        // Database implementation
        //
        if (isDbStore(storeId)) {
            DocVersionId res = null;
            synchronized (this) {
                DocStoreDbConnection dbcon = acquireDbConnection(storeId);
                try {
                    res = dbcon.getVersionDerivedFrom(storeId, verId);
                } finally {
                    releaseDbConnection(storeId, dbcon);
                }
            }
            return res;
        }
        
        //
        // Filesystem-based implementation
        //
        String verstr = getVersionProperty(storeId, verId, PROP_VERSION_DERIVED_FROM);
        try {
            return createVersionId(verstr);
        } catch (Exception ex) {
            return null;
        }
    }

    public DocVersionId[] getSubVersions(String storeId, DocVersionId verId)
    {
        //
        // Database implementation
        //
        if (isDbStore(storeId)) {
            DocVersionId[] res = null;
            synchronized (this) {
                DocStoreDbConnection dbcon = acquireDbConnection(storeId);
                try {
                    res = dbcon.getSubVersions(storeId, verId);
                } finally {
                    releaseDbConnection(storeId, dbcon);
                }
            }
            return res;
        }
        
        //
        // Filesystem-based implementation
        //
        DocVersionId[] all_ids = listVersions(storeId);
        List sub_list = new ArrayList(all_ids.length);
        for (int i=0; i < all_ids.length; i++) {
            DocVersionId derivedFrom = getVersionDerivedFrom(storeId, all_ids[i]);
            if ((derivedFrom != null) && verId.equals(derivedFrom)) {
                sub_list.add(all_ids[i]);
            }
        }
        DocVersionId[] sub_ids = new DocVersionId[sub_list.size()];
        return (DocVersionId[]) sub_list.toArray(sub_ids);
    }

    public void renameVersion(String storeId, DocVersionId oldVerId, DocVersionId newVerId)
    throws DocException
    {
        //
        // Database implementation
        //
        if (isDbStore(storeId)) {
            synchronized (this) {
                DocStoreDbConnection dbcon = acquireDbConnection(storeId);
                try {
                    dbcon.renameVersion(storeId, oldVerId, newVerId);
                } finally {
                    releaseDbConnection(storeId, dbcon);
                }
            }
            return;
        }
        
        //
        // Filesystem-based implementation
        //
        checkRenameVersion(storeId, oldVerId, newVerId);
        try {
            // Delete in-memory representation of version
            storeManager.destroyStoreInstance(storeId, oldVerId); // throws runtime exception if users are still connected
        } catch (Exception ex) {
            throw new DocException(ex);
        }
        File dir = getStoreDirFromId(storeId);
        File newDir = getVersionDir(dir, newVerId);
        if (newDir.exists()) {
            throw new DocRuntimeException("Cannot rename to version " + newVerId +
                ". File " + newDir.getAbsolutePath() + " already exists.");
        }
        File oldDir = getVersionDir(dir, oldVerId);
        if (! oldDir.renameTo(newDir)) {
            throw new DocRuntimeException("Could not rename store directory '" +
                oldDir.getAbsolutePath() + "' to '" +
                newDir.getAbsolutePath() + "'.");
        }
        versionPropLoaders.remove(storeId);  // clear cached version properties
    }

    public void createVersion(String storeId, DocVersionId baseVersion, DocVersionId newVersion)
    throws DocException
    {
        //
        // Database implementation
        //
        if (isDbStore(storeId)) {
            synchronized (this) {
                DocStoreDbConnection dbcon = acquireDbConnection(storeId);
                try {
                    dbcon.createVersion(storeId, baseVersion, newVersion);
                } finally {
                    releaseDbConnection(storeId, dbcon);
                }
            }
            return;
        }
        
        //
        // Filesystem-based implementation
        //
        checkCreateVersion(storeId, baseVersion, newVersion);

        File dir = getStoreDirFromId(storeId);
        File destDir = getVersionDir(dir, newVersion);
        if (destDir.exists()) {
            throw new DocRuntimeException("Cannot create version " + newVersion +
                ". File " + destDir.getAbsolutePath() + " already exists.");
        }
        if (! destDir.mkdir()) {
            throw new DocRuntimeException("Cannot create directory " + destDir.getAbsolutePath());
        }
        List propnames = new ArrayList();
        List propvalues = new ArrayList();
        if (baseVersion != null) {
            File sourceDir = getVersionDir(dir, baseVersion);

            // Copy all normal files
            File[] files = sourceDir.listFiles();
            for (int i=0; i < files.length; i++) {
                File f = files[i];
                if (f.isFile()) {
                    File dest_file = new File(destDir, f.getName());
                    if (! DocmaUtil.fileCopy(f, dest_file, false)) {
                        throw new DocRuntimeException("Could not copy file " + f.getName() + " from '" +
                            sourceDir.getAbsolutePath() + "' to '" + destDir.getAbsolutePath() + "'.");
                    }
                }
            }

            // Copy the content folder
            File src_content = new File(sourceDir, "content");
            File dest_content = new File(destDir, "content");
            if (! DocmaUtil.recursiveFileCopy(src_content, dest_content, false)) {
                throw new DocRuntimeException("Could not copy content directory from '" +
                    sourceDir.getAbsolutePath() + "' to '" + destDir.getAbsolutePath() + "'.");
            }

            // Copy all released translations
            File transSourceDir = new File(sourceDir, "translations");
            File transDestDir = new File(destDir, "translations");
            if (! transDestDir.mkdir()) {
                throw new DocRuntimeException("Could not create translation directory " + transDestDir);
            }
            String[] langs = transSourceDir.list();
            for (int i=0; i < langs.length; i++) {
                String lang_code = langs[i];
                String state = getVersionState(storeId, baseVersion, lang_code);
                if (state.equalsIgnoreCase(DocVersionState.RELEASED)) {
                    File langSrcDir = new File(transSourceDir, lang_code);
                    File langDestDir = new File(transDestDir, lang_code);
                    if (! DocmaUtil.recursiveFileCopy(langSrcDir, langDestDir, false)) {
                        throw new DocRuntimeException("Could not copy language directory '" +
                            langSrcDir + "' to '" + langDestDir + "'.");
                    }
                    propnames.add(PROP_VERSION_STATE + "." + lang_code.toLowerCase());
                    propvalues.add(DocVersionState.DRAFT);
                } else {
                    propnames.add(PROP_VERSION_STATE + "." + lang_code.toLowerCase());
                    propvalues.add(DocVersionState.TRANSLATION_PENDING);
                }
                propnames.add(PROP_VERSION_RELEASE_DATE + "." + lang_code.toLowerCase());
                propvalues.add("");
            }

            // Overwrite UUID of base version with empty string -> new UUID will be assigned
            propnames.add(PROP_VERSION_UUID);
            propvalues.add("");
        }
        propnames.add(PROP_VERSION_STATE);
        propvalues.add(DocVersionState.DRAFT);

        propnames.add(PROP_VERSION_CREATION_DATE);
        propvalues.add("" + System.currentTimeMillis());

        propnames.add(PROP_VERSION_RELEASE_DATE);
        propvalues.add("");

        String derived_from = (baseVersion == null) ? "" : baseVersion.toString();
        propnames.add(PROP_VERSION_DERIVED_FROM);
        propvalues.add(derived_from);

        String[] names = new String[propnames.size()];
        names = (String[]) propnames.toArray(names);
        String[] values = new String[propvalues.size()];
        values = (String[]) propvalues.toArray(values);
        setVersionProperties(storeId, newVersion, names, values);
    }

    public DocGroup getRoot() 
    {
        if (isDbStore(this.storeId)) {
            //
            // Database implementation
            //
            return dbOpenedConnection.getRoot();
        } else {
            //
            // Filesystem-based implementation
            //
            return rootGroup;
        }
    }

    public boolean nodeIdExists(String id) 
    {
        //
        // Database implementation
        //
        if (isDbStore(this.storeId)) {
            return dbOpenedConnection.nodeIdExists(id);
        }
        
        //
        // Filesystem-based implementation
        //
        String dom_id;
        try {
            dom_id = IdRegistry.idStringToDOMString(id);
        } catch (Exception ex) {
            Log.warning("Invalid node id in nodeIdExists():" + id);
            return false;  // no valid id
        }
        Element elem = getDocStoreFs().getIndexDocument().getElementById(dom_id);
        return (elem != null);
    }

    // public DocNode getNode(String idOrAlias) {
    //     String dom_id = idOrAlias;
    //     try { 
    //         int idnum = Integer.parseInt(idOrAlias);
    //         dom_id = IdRegistry.idToDOMString(idnum);
    //     } catch (Exception ex) {}
    //     Element elem = docStore.getIndexDocument().getElementById(dom_id);
    //     if (elem.getTagName().equals(XMLConstants.TAG_ALIAS)) {
    //         elem = (Element) elem.getParentNode();
    //     }
    //     return createDocNodeFromDOMElement(elem);
    // }

    public DocNode getNodeById(String id) 
    {
        //
        // Database implementation
        //
        if (isDbStore(this.storeId)) {
            return dbOpenedConnection.getNodeById(id);
        }
        
        //
        // Filesystem-based implementation
        //
        String dom_id; 
        try {
            dom_id = IdRegistry.idStringToDOMString(id);
        } catch (Exception ex) {
            Log.warning("Invalid node id in getNodeById():" + id);
            return null;  // no valid id
        }
        Element elem = getDocStoreFs().getIndexDocument().getElementById(dom_id);
        if (elem == null) {
            return null;
        }
        return createDocNodeFromDOMElement(elem);
    }

    public DocNode getNodeByAlias(String alias) 
    {
        //
        // Database implementation
        //
        if (isDbStore(this.storeId)) {
            return dbOpenedConnection.getNodeByAlias(alias);
        }
        
        //
        // Filesystem-based implementation
        //
        Element elem = getDocStoreFs().getIndexDocument().getElementById(alias);
        if ((elem != null) && elem.getTagName().equals(XMLConstants.TAG_ALIAS)) {
            elem = (Element) elem.getParentNode();
            return createDocNodeFromDOMElement(elem);
        }
        return null;
    }

    public String getNodeIdByAlias(String alias) 
    {
        //
        // Database implementation
        //
        if (isDbStore(this.storeId)) {
            return dbOpenedConnection.getNodeIdByAlias(alias);
        }
        
        //
        // Filesystem-based implementation
        //
        Element elem = getDocStoreFs().getIndexDocument().getElementById(alias);
        if ((elem != null) && elem.getTagName().equals(XMLConstants.TAG_ALIAS)) {
            elem = (Element) elem.getParentNode();
            return String.valueOf(IdRegistry.DOMStringToId(elem.getAttribute(XMLConstants.ATTR_ID)));
        }
        return null;
    }

    public synchronized String[] listIds(Class node_class)
    {
        //
        // Database implementation
        //
        if (isDbStore(this.storeId)) {
            return dbOpenedConnection.listIds(node_class);
        }
        
        //
        // Filesystem-based implementation
        //
        if (rootGroup == null) {
            throw new DocRuntimeException("Method listIds() can only be called on an open document store!");
        }
        if (node_class == null) node_class = DocNode.class;

        SortedSet<String> id_set = new TreeSet<String>();
        listIds_recursive(rootGroup, node_class, id_set);
        return id_set.toArray(new String[id_set.size()]);
    }

    public synchronized String[] listAliases(Class node_class)
    {
        //
        // Database implementation
        //
        if (isDbStore(this.storeId)) {
            return dbOpenedConnection.listAliases(node_class);
        }
        
        //
        // Filesystem-based implementation
        //
        if (rootGroup == null) {
            throw new DocRuntimeException("Method listAliases() can only be called on an open document store!");
        }
        if (node_class == null) node_class = DocNode.class;

        String[] aliases = (String[]) aliasCache.get(node_class.getName());
        if (aliases != null) return aliases;  // return cached list

        SortedSet alias_set = new TreeSet();
        listAliases_recursive(rootGroup, node_class, alias_set);
        aliases = (String[]) alias_set.toArray(new String[alias_set.size()]);
        aliasCache.put(node_class.getName(), aliases);
        return aliases;
    }

    public synchronized List<NodeInfo> listNodeInfos(Class node_class)
    {
        //
        // Database implementation
        //
        if (isDbStore(this.storeId)) {
            return dbOpenedConnection.listNodeInfos(node_class);
        }
        
        //
        // Filesystem-based implementation
        //
        if (rootGroup == null) {
            throw new DocRuntimeException("Method listIds() can only be called on an open document store!");
        }
        if (node_class == null) node_class = DocNode.class;

        List<NodeInfo> info_list = new ArrayList<NodeInfo>();
        listNodeInfos_recursive(rootGroup, node_class, info_list);
        return info_list;
    }


    public DocGroup createGroup() 
    {
        if (isDbStore(this.storeId)) {
            //
            // Database implementation
            //
            return dbOpenedConnection.createGroup();
        } else {
            //
            // Filesystem-based implementation
            //
            int node_num = getDocStoreFs().getIdRegistry().newId();
            return new DocGroupImpl(this, node_num);
        }
    }

    public DocGroup createGroup(String node_id) 
    {
        if (isDbStore(this.storeId)) {
            //
            // Database implementation
            //
            return dbOpenedConnection.createGroup(node_id);
        } else {
            //
            // Filesystem-based implementation
            //
            int node_num = Integer.parseInt(node_id);
            getDocStoreFs().getIdRegistry().registerId(node_num);
            return new DocGroupImpl(this, node_num);
        }
    }

    public DocXML createXML() 
    {
        if (isDbStore(this.storeId)) {
            //
            // Database implementation
            //
            return dbOpenedConnection.createXML();
        } else {
            //
            // Filesystem-based implementation
            //
            int node_num = getDocStoreFs().getIdRegistry().newId();
            return new DocXMLImpl(this, node_num);
        }
    }

    public DocXML createXML(String node_id) 
    {
        if (isDbStore(this.storeId)) {
            //
            // Database implementation
            //
            return dbOpenedConnection.createXML(node_id);
        } else {
            //
            // Filesystem-based implementation
            //
            int node_num = Integer.parseInt(node_id);
            getDocStoreFs().getIdRegistry().registerId(node_num);
            return new DocXMLImpl(this, node_num);
        }
    }

    public DocImage createImage() 
    {
        if (isDbStore(this.storeId)) {
            //
            // Database implementation
            //
            return dbOpenedConnection.createImage();
        } else {
            //
            // Filesystem-based implementation
            //
            int node_num = getDocStoreFs().getIdRegistry().newId();
            return new DocImageImpl(this, node_num);
        }
    }

    public DocImage createImage(String node_id) 
    {
        if (isDbStore(this.storeId)) {
            //
            // Database implementation
            //
            return dbOpenedConnection.createImage(node_id);
        } else {
            //
            // Filesystem-based implementation
            //
            int node_num = Integer.parseInt(node_id);
            getDocStoreFs().getIdRegistry().registerId(node_num);
            return new DocImageImpl(this, node_num);
        }
    }

    public DocFile createFile() 
    {
        if (isDbStore(this.storeId)) {
            //
            // Database implementation
            //
            return dbOpenedConnection.createFile();
        } else {
            //
            // Filesystem-based implementation
            //
            int node_num = getDocStoreFs().getIdRegistry().newId();
            return new DocFileImpl(this, node_num);
        }
    }

    public DocFile createFile(String node_id) 
    {
        if (isDbStore(this.storeId)) {
            //
            // Database implementation
            //
            return dbOpenedConnection.createFile(node_id);
        } else {
            //
            // Filesystem-based implementation
            //
            int node_num = Integer.parseInt(node_id);
            getDocStoreFs().getIdRegistry().registerId(node_num);
            return new DocFileImpl(this, node_num);
        }
    }

    public DocReference createReference() 
    {
        if (isDbStore(this.storeId)) {
            //
            // Database implementation
            //
            return dbOpenedConnection.createReference();
        } else {
            //
            // Filesystem-based implementation
            //
            int node_num = getDocStoreFs().getIdRegistry().newId();
            return new DocReferenceImpl(this, node_num);
        }
    }

    public DocReference createReference(String node_id) 
    {
        if (isDbStore(this.storeId)) {
            //
            // Database implementation
            //
            return dbOpenedConnection.createReference(node_id);
        } else {
            //
            // Filesystem-based implementation
            //
            int node_num = Integer.parseInt(node_id);
            getDocStoreFs().getIdRegistry().registerId(node_num);
            return new DocReferenceImpl(this, node_num);
        }
    }

    public void startTransaction() throws DocException 
    {
        if (running_transaction) {
            throw new DocException("Cannot start transaction: transaction is running.");
        }
        
        refreshPropLoaders();  // reload properties if properties have been changed by other sessions
        
        // if no store is opened
        if (this.storeId == null) {  
            running_transaction = true;
            return;
        }
        
        //
        // Database implementation
        //
        if (isDbStore(this.storeId)) {
            synchronized (this) {
                dbOpenedConnection.startTransaction();
                running_transaction = true;
            }
            return;
        }
        
        //
        // Filesystem-based implementation
        //
        // try to start transaction within 1 second
        DocStoreImpl docStoreFs = getDocStoreFs();
        for (int i=0; i < 10; i++) {
            if (docStoreFs.runningTransaction()) {
                try {
                    Thread.sleep(100);
                } catch (Exception ex) {
                    // ignore
                    Log.warning("Exception in Thread.sleep: " + ex.getMessage());
                }
            } else {
                docStoreFs.startTransaction();
                running_transaction = true;
                break;
            }
        }
        if (! running_transaction) {
            throw new DocException("Cannot start transaction: document store is busy.");
        }
    }

    public void commitTransaction() throws DocException 
    {
        if (! running_transaction) {
            throw new DocException("Cannot commit transaction: no transaction running.");
        }
        
        if (this.storeId != null) { 
            if (isDbStore(this.storeId)) {
                dbOpenedConnection.commitTransaction();
            } else {
                getDocStoreFs().commitTransaction();
            }
        }

        // Write changed properties of filesystem-based stores
        Iterator it = changedProps.iterator();
        while (it.hasNext()) {
            PropertiesLoader p = (PropertiesLoader) it.next();
            p.savePropFile();
        }
        changedProps.clear();

        // Call commit on pooled database-store connections
        for (DocStoreDbConnection dbcon : dbConnectionPool.values()) {
            try {
                dbcon.commitTransaction();
            } catch (Exception ex) {
                // Note: This is a simple implementation. Distributed transactions
                // are not supported. If the commit on a pooled connection
                // fails, this is ignored. This means, if a transaction spans
                // more than one store, this is no real transaction.
                Log.error("Could not commit transaction on pooled DB connection: " + ex.getMessage());
            } finally {
                try { 
                    dbcon.close(); 
                } catch (Exception ex) {
                    Log.warning("Could not close pooled DB connection after commit: " + ex.getMessage());
                }
            }
        }
        dbConnectionPool.clear();
        
        running_transaction = false;
    }

    public void rollbackTransaction()
    {
        if (running_transaction) {
            try {
                if (this.storeId != null) {
                    if (isDbStore(this.storeId)) {
                        dbOpenedConnection.rollbackTransaction();
                    } else {
                        getDocStoreFs().rollbackTransaction();
                    }
                }
                
                // Discard changed properties of filesystem-based stores
                Iterator it = changedProps.iterator();
                while (it.hasNext()) {
                    PropertiesLoader p = (PropertiesLoader) it.next();
                    try {
                        p.discard();
                    } catch (DocException dex) {}
                }
                
                // Call rollback on pooled database-store connections
                for (DocStoreDbConnection dbcon : dbConnectionPool.values()) {
                    try {
                        dbcon.rollbackTransaction();
                    } catch (Exception ex) {
                        // Note: This is a simple implementation. Distributed transactions
                        // are not supported. If the rollback on a pooled connection
                        // fails, this is ignored. This means, if a transaction spans
                        // more than one store, this is no real transaction.
                        Log.error("Could not rollback transaction on pooled DB connection: " + ex.getMessage());
                    } finally {
                        try { 
                            dbcon.close(); 
                        } catch (Exception ex) {
                            Log.warning("Could not close pooled DB connection after rollback: " + ex.getMessage());
                        }
                    }
                }
            } finally {
                changedProps.clear();
                dbConnectionPool.clear();
                running_transaction = false;
            }
        } else {
            Log.warning("Cannot rollback transaction: no transaction running.");
        }
    }

    public boolean runningTransaction()
    {
        return running_transaction;
    }

    public void closeSession() 
    {
        if (runningTransaction()) {
            Log.warning("Closing session during transaction. Transaction is rolled back!");
            try { 
                rollbackTransaction(); 
            } catch (Exception ex) { ex.printStackTrace(); }
        }
        if (this.storeId != null) { 
            try {
                closeDocStore();
            } catch (Exception ex) {
                Log.error("Exception in DocStoreSessionImpl.closeSession(). Closing of store failed: " + ex.getMessage());
            }
        }
        if ((dbConnectionPool != null) && !dbConnectionPool.isEmpty()) {
            Log.warning("Closing DB connections in closeSession(): " + dbConnectionPool.size());
            closePooledDbConnections();
        }
        storeManager.destroySession(this);
    }
    
    private void closePooledDbConnections()
    {
        for (DocStoreDbConnection dbcon : dbConnectionPool.values()) {
            try {
                dbcon.close();
            } catch (Exception ex) {
                Log.error("Could not close pooled DB connection: " + ex.getMessage());
            }
        }
        dbConnectionPool.clear();
    }

    public String getDocStoreProperty(String storeId, String name) 
    {
        StorePropertiesLoader ploader = getPropLoader(storeId);
        if (name.equals(FilesystemStoreProperties.PROP_STORE_PATH)) {
            return ploader.getStorePathProperty();
        } else
        if (name.equals(FilesystemStoreProperties.PROP_STORE_BASEPATH)) {
            return ploader.getStoreBaseDir().getAbsolutePath();
        } else
        if (name.equals(FilesystemStoreProperties.PROP_STORE_TYPE)) {
            return ploader.getStoreType();
        } else 
        if (ploader.isDbStore()) {
            if (ploader.isDbExternalStore()) {
                PropertiesLoader con_props = ploader.getDbExternalConnectionProps();
                // If connection property
                if (name.equals(FilesystemStoreProperties.PROP_DB_DIALECT) || 
                    name.equals(FilesystemStoreProperties.PROP_DB_DRIVER_CLASS) ||
                    name.equals(FilesystemStoreProperties.PROP_DB_CONNECTION_URL) ||
                    name.equals(FilesystemStoreProperties.PROP_DB_CONNECTION_USER) ||
                    name.equals(FilesystemStoreProperties.PROP_DB_CONNECTION_PWD)) {
                    return con_props.getProp(name);
                }
            }
            // If no connection property of an external DB store, then return 
            // property of database store
            String res = null;
            synchronized (this) {
                DocStoreDbConnection dbcon = acquireDbConnection(storeId);
                try {
                    res = dbcon.getDocStoreProperty(storeId, name);
                } finally {
                    releaseDbConnection(storeId, dbcon);
                }
            }
            return res;
        } else {
            // Return property of filesystem-based store
            if (! runningTransaction()) {
                ploader.refresh();  // reload properties if timestamp of properties-file has changed
            }
            return ploader.getProp(name);
        }
    }
    
    private boolean setSpecialStoreProp(StorePropertiesLoader ploader, 
                                        String storeId, 
                                        String name, 
                                        String value)
    {
        if (name.equals(FilesystemStoreProperties.PROP_STORE_PATH)) {
            ploader.setStorePathProperty(value);
            return true;
        } else
        if (name.equals(FilesystemStoreProperties.PROP_STORE_BASEPATH) || 
            name.equals(FilesystemStoreProperties.PROP_STORE_TYPE)) {
            Log.warning("Cannot set store property '" + name + "' of store '" + 
                        storeId + "'. Property is read-only!");
            // throw new DocRuntimeException("Property " + name + " is read-only!");
            return true;
        }
        return false;
    }
    
    private boolean setDbConnectionProp(StorePropertiesLoader ploader, 
                                        String storeId, 
                                        String name, 
                                        String value)
    {
        if (ploader.isDbExternalStore()) {
            PropertiesLoader con_props = ploader.getDbExternalConnectionProps();
            // If connection property
            if (name.equals(FilesystemStoreProperties.PROP_DB_DIALECT) || 
                name.equals(FilesystemStoreProperties.PROP_DB_DRIVER_CLASS) ||
                name.equals(FilesystemStoreProperties.PROP_DB_CONNECTION_URL) ||
                name.equals(FilesystemStoreProperties.PROP_DB_CONNECTION_USER) ||
                name.equals(FilesystemStoreProperties.PROP_DB_CONNECTION_PWD)) {
                con_props.setProp(name, value);
                return true;
            }
        }
        return false;
    }

    public void setDocStoreProperty(String storeId, String name, String value) throws DocException 
    {
        StorePropertiesLoader ploader = getPropLoader(storeId);
        boolean propFileChanged = false;
        if (setSpecialStoreProp(ploader, storeId, name, value)) {
            return;
        } else
        if (setDbConnectionProp(ploader, storeId, name, value)) {
            propFileChanged = true;
        } else {
            if (ploader.isDbStore()) {
                // Set property of database store
                synchronized (this) {
                    DocStoreDbConnection dbcon = acquireDbConnection(storeId);
                    try {
                        dbcon.setDocStoreProperty(storeId, name, value);
                    } finally {
                        releaseDbConnection(storeId, dbcon);
                    }
                }
            } else {  
                // Set property of filesystem-based store
                if (! runningTransaction()) {
                    ploader.refresh();  // reload properties if timestamp of properties-file has changed
                    if (DocmaConstants.DEBUG) {
                        Log.info("Setting property outside of transaction context: " + name);
                    }
                }
                ploader.setProp(name, value);
                propFileChanged = true;
            }
        }
        if (propFileChanged) {
            if (runningTransaction()) {
                changedProps.add(ploader);
            } else {
                ploader.savePropFile();
            }
        }
    }

    public void setDocStoreProperties(String storeId, String[] names, String[] values) throws DocException 
    {
        boolean started = startLocalTransaction();
        try {
            for (int i=0; i < names.length; i++) {
                setDocStoreProperty(storeId, names[i], values[i]);
            }
            commitLocalTransaction(started);
        } catch (Exception ex) {
            rollbackLocalTransactionRethrow(started, ex);
        }
    }

    public String[] getDocStorePropertyNames(String storeId)
    {
        String[] all_names;
        //
        // Database implementation
        //
        if (isDbStore(storeId)) {
            synchronized (this) {
                DocStoreDbConnection dbcon = acquireDbConnection(storeId);
                try {
                    all_names = dbcon.getDocStorePropertyNames(storeId);
                } finally {
                    releaseDbConnection(storeId, dbcon);
                }
            }
            return all_names;
        }
        
        //
        // Filesystem-based implementation
        //
        StorePropertiesLoader ploader = getPropLoader(storeId);
        if (! runningTransaction()) {
            ploader.refresh();  // reload properties if timestamp of properties-file has changed
        }
        all_names = ploader.getPropNames();
        ArrayList names = new ArrayList();
        for (int i=0; i < all_names.length; i++) {
            String nm = all_names[i];
            if (! isInternalStoreProperty(nm)) names.add(nm);
        }
        return (String[]) names.toArray(new String[names.size()]);
    }

    public String getVersionProperty(String storeId, DocVersionId verId, String name) 
    {
        //
        // Database implementation
        //
        if (isDbStore(storeId)) {
            String res;
            synchronized (this) {
                DocStoreDbConnection dbcon = acquireDbConnection(storeId);
                try {
                    res = dbcon.getVersionProperty(storeId, verId, name);
                } finally {
                    releaseDbConnection(storeId, dbcon);
                }
            }
            return res;
        }
        
        //
        // Filesystem-based implementation
        //
        PropertiesLoader ploader = getVersionPropLoader(storeId, verId);
        if (! runningTransaction()) {
            ploader.refresh();  // reload properties if timestamp of properties-file has changed
        }
        String val = ploader.getProp(name);
        return (val == null) ? "" : val;
    }

    public void setVersionProperty(String storeId, DocVersionId verId, String name, String value) throws DocException 
    {
        //
        // Database implementation
        //
        if (isDbStore(storeId)) {
            synchronized (this) {
                DocStoreDbConnection dbcon = acquireDbConnection(storeId);
                try {
                    dbcon.setVersionProperty(storeId, verId, name, value);
                } finally {
                    releaseDbConnection(storeId, dbcon);
                }
            }
            return;
        }
        
        //
        // Filesystem-based implementation
        //
        PropertiesLoader ploader = getVersionPropLoader(storeId, verId);
        if (! runningTransaction()) {
            ploader.refresh();  // reload properties if timestamp of properties-file has changed
            if (DocmaConstants.DEBUG) {
                Log.info("Setting version property outside of transaction context: " + name);
            }
        }
        ploader.setProp(name, value);
        if (runningTransaction()) {
            changedProps.add(ploader);
        } else {
            ploader.savePropFile();
        }
    }

    public void setVersionProperties(String storeId, DocVersionId verId, String[] names, String[] values) throws DocException 
    {
        //
        // Database implementation
        //
        if (isDbStore(storeId)) {
            synchronized (this) {
                DocStoreDbConnection dbcon = acquireDbConnection(storeId);
                try {
                    dbcon.setVersionProperties(storeId, verId, names, values);
                } finally {
                    releaseDbConnection(storeId, dbcon);
                }
            }
            return;
        }
        
        //
        // Filesystem-based implementation
        //
        PropertiesLoader ploader = getVersionPropLoader(storeId, verId);
        if (! runningTransaction()) {
            ploader.refresh();  // reload properties if timestamp of properties-file has changed
            if (DocmaConstants.DEBUG) {
                Log.info("Setting version properties outside of transaction context.");
            }
        }
        for (int i=0; i < names.length; i++) {
            ploader.setProp(names[i], values[i]);
        }
        if (runningTransaction()) {
            changedProps.add(ploader);
        } else {
            ploader.savePropFile();
        }
    }

    public String[] getVersionPropertyNames(String storeId, DocVersionId verId)
    {
        String[] all_names;
        //
        // Database implementation
        //
        if (isDbStore(storeId)) {
            synchronized (this) {
                DocStoreDbConnection dbcon = acquireDbConnection(storeId);
                try {
                    all_names = dbcon.getVersionPropertyNames(storeId, verId);
                } finally {
                    releaseDbConnection(storeId, dbcon);
                }
            }
            return all_names;
        }
        
        //
        // Filesystem-based implementation
        //
        PropertiesLoader ploader = getVersionPropLoader(storeId, verId);
        if (! runningTransaction()) {
            ploader.refresh();  // reload properties if timestamp of properties-file has changed
        }
        all_names = ploader.getPropNames();
        ArrayList names = new ArrayList();
        for (int i=0; i < all_names.length; i++) {
            String nm = all_names[i];
            if (! isInternalVersionProperty(nm)) names.add(nm);
        }
        return (String[]) names.toArray(new String[names.size()]);
    }

}
