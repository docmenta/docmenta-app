/*
 * StorePropertiesLoader.java
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

package org.docma.coreapi.fsimplementation;

import java.io.*;
import java.util.*;
import org.docma.coreapi.*;
import org.docma.hibernate.DbConnectionData;
import org.docma.util.PropertiesLoader;

/**
 * Helper class to read and write store properties.
 * 
 * @author MP
 */
public class StorePropertiesLoader extends PropertiesLoader
{
    private static final String FILE_COMMENT_DB_CONNECTION = "External DB connection properties";
    
    private String storeType = null;
    private File pathfile;
    private PropertiesLoader pathLoader = null;
    private PropertiesLoader dbExternalConnectionProps = null;
    private String storeId;
    private File storeBaseDir;
    private DocStoreManager storeManager;

    public StorePropertiesLoader(String storeId, File storeBaseDir, DocStoreManager storeManager)
    {
        this.storeId = storeId;
        this.storeBaseDir = storeBaseDir;
        this.storeManager = storeManager;

        pathfile = new File(storeBaseDir, FilesystemStoreProperties.STORE_PATH_FILENAME);
        initPathLoader();
        String store_path = getStorePathProperty();
        if (store_path == null) {
            this.propfile = new File(storeBaseDir, FilesystemStoreProperties.STORE_PROP_FILENAME);
        } else {
            this.propfile = new File(store_path, FilesystemStoreProperties.STORE_PROP_FILENAME);
        }

        if (this.propfile.exists()) {
            try {
                loadPropFile();
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new DocRuntimeException("Could not read properties file: " + this.propfile.getAbsolutePath());
            }
        } else {
            setEmptyProps();  // newly created store has no properties yet
        }
        setComments("Document store properties.");
    }

    File getStoreBaseDir()
    {
        return storeBaseDir;
    }

    /**
     * Return property of filesystem-based store.
     * Note: For database-stores this method should not be called, as in this
     * case the property has to be read from the database.
     * @param pname
     * @return 
     */
    public String getProp(String pname)
    {
        return super.getProp(pname);
    }

    /**
     * Set property of filesystem-based store.
     * Note: For database-stores this method should not be called, as in this
     * case the property has to be written into the database.
     * @param pname
     * @param pvalue
     * @return 
     */
    public String setProp(String pname, String pvalue)
    {
        return super.setProp(pname, pvalue);
    }
    
    private void initStoreType()
    {
        if (this.storeType == null) {
            File store_dir;
            try {
                store_dir = getStoreDir();
            } catch (Exception ex) {
                // invalid external directory path; fall back to base directory
                store_dir = storeBaseDir;
            }
            File prop_file = new File(store_dir, FilesystemStoreProperties.STORE_PROP_FILENAME);
            if (prop_file.exists()) {
                this.storeType = FilesystemStoreProperties.STORE_TYPE_FS;
            } else {
                File extdb_file = new File(storeBaseDir, FilesystemStoreProperties.DB_CONNECTION_FILENAME);
                if (extdb_file.exists()) {
                    this.storeType = FilesystemStoreProperties.STORE_TYPE_DB_EXTERNAL;
                } else {
                    File embedded_dir = new File(store_dir, FilesystemStoreProperties.DB_EMBEDDED_FOLDERNAME);
                    this.storeType = embedded_dir.exists() ? 
                            FilesystemStoreProperties.STORE_TYPE_DB_EMBEDDED : 
                            FilesystemStoreProperties.STORE_TYPE_FS;
                }
            }
        }
    }
    
    String getStoreType()
    {
        initStoreType();
        return this.storeType;
    }

    boolean isDbStore()
    {
        initStoreType();
        return !this.storeType.equals(FilesystemStoreProperties.STORE_TYPE_FS);
    }

    boolean isDbEmbeddedStore()
    {
        initStoreType();
        return this.storeType.equals(FilesystemStoreProperties.STORE_TYPE_DB_EMBEDDED);
    }

    boolean isDbExternalStore()
    {
        initStoreType();
        return this.storeType.equals(FilesystemStoreProperties.STORE_TYPE_DB_EXTERNAL);
    }

    File getStoreDir() 
    {
        String s_path = getStorePathProperty();
        if ((s_path == null) || s_path.trim().equals("")) {
            return storeBaseDir;
        } else {
            File dir = new File(s_path);
            if (! (dir.isAbsolute() && dir.isDirectory())) {
                throw new DocRuntimeException("Store path is not an absolute directory path: " + s_path);
            }
            return dir;
        }
    }

    /**
     * Gets the connection properties of an external database-store.
     * The properties are returned as a PropertiesLoader instance. 
     * Use this method to modify the connection properties.
     * @return PropertiesLoader instance.
     */
    PropertiesLoader getDbExternalConnectionProps()
    {
        if (dbExternalConnectionProps == null) {
            File extdb_file = new File(storeBaseDir, FilesystemStoreProperties.DB_CONNECTION_FILENAME);
            if (extdb_file.exists()) {
                try { 
                    dbExternalConnectionProps = new PropertiesLoader(extdb_file);
                } catch (Exception ex) {
                    throw new DocRuntimeException(ex);
                }
            } else {
                dbExternalConnectionProps = null;
            }
        }
        return dbExternalConnectionProps;
    }
    
    DbConnectionData getDbConnectionData() 
    {
        if (isDbExternalStore()) {
            return getDbExternalConnectionData(storeBaseDir);
        } else
        if (isDbEmbeddedStore()) {
            return getDbEmbeddedConnectionData(getStoreDir());
        } else {
            return null;
        }
    }

    static DbConnectionData getDbExternalConnectionData(File store_base_dir) 
    {
        File extdb_file = new File(store_base_dir, FilesystemStoreProperties.DB_CONNECTION_FILENAME);
        if (extdb_file.exists()) {
            try {
                PropertiesLoader pl = new PropertiesLoader(extdb_file);
                DbConnectionData con_data = new DbConnectionData();
                con_data.setConnectionURL(pl.getProp(FilesystemStoreProperties.PROP_DB_CONNECTION_URL));
                con_data.setDriverClassName(pl.getProp(FilesystemStoreProperties.PROP_DB_DRIVER_CLASS));
                con_data.setDbDialect(pl.getProp(FilesystemStoreProperties.PROP_DB_DIALECT));
                con_data.setUserId(pl.getProp(FilesystemStoreProperties.PROP_DB_CONNECTION_USER));
                con_data.setUserPwd(pl.getProp(FilesystemStoreProperties.PROP_DB_CONNECTION_PWD));
                return con_data;
            } catch (DocException ex) {
                throw new DocRuntimeException(ex);
            }
        } else {
            return null;
        }
    }
    
    static DbConnectionData getDbEmbeddedConnectionData(File store_dir) 
    {
        File db_path = new File(store_dir, FilesystemStoreProperties.DB_EMBEDDED_FOLDERNAME);
        String CONNECTION_URL = "jdbc:derby:directory:" + db_path + ";create=true";
        
        DbConnectionData con_data = new DbConnectionData();
        con_data.setConnectionURL(CONNECTION_URL);
        con_data.setDriverClassName(FilesystemStoreProperties.DB_EMBEDDED_DRIVER);
        con_data.setDbDialect(FilesystemStoreProperties.DB_EMBEDDED_DIALECT);
        con_data.setUserId(null);
        con_data.setUserPwd(null);
        return con_data;
    }
    
    static void writeDbExternalConnectionData(File store_dir, DbConnectionData con_data) 
    {
        try { 
            File extdb_file = new File(store_dir, FilesystemStoreProperties.DB_CONNECTION_FILENAME);
            if (! extdb_file.exists()) {
                extdb_file.createNewFile();
            }
            PropertiesLoader pl = new PropertiesLoader(extdb_file);
            pl.setProp(FilesystemStoreProperties.PROP_DB_DRIVER_CLASS,    con_data.getDriverClassName());
            pl.setProp(FilesystemStoreProperties.PROP_DB_CONNECTION_URL,  con_data.getConnectionURL());
            pl.setProp(FilesystemStoreProperties.PROP_DB_DIALECT,         con_data.getDbDialect());
            pl.setProp(FilesystemStoreProperties.PROP_DB_CONNECTION_USER, con_data.getUserId());
            pl.setProp(FilesystemStoreProperties.PROP_DB_CONNECTION_PWD,  con_data.getUserPwd());
            pl.savePropFile(FILE_COMMENT_DB_CONNECTION);
        } catch (Exception ex) {
            throw new DocRuntimeException(ex);
        }
    }

    String getStorePathProperty()
    {
        initPathLoader();
        if (pathLoader != null) {
            String path = pathLoader.getProp(FilesystemStoreProperties.PROP_STORE_PATH);
            return ((path == null) || (path.length() == 0)) ? null : path;
        } else {
            return null;
        }
    }
    
    private void initPathLoader()
    {
        if (pathLoader == null) {
            if (pathfile.exists()) {
                try {
                    pathLoader = new PropertiesLoader(pathfile);
                } catch (DocException ex) {
                    throw new DocRuntimeException(ex);
                }
            }
        }
    }

    String setStorePathProperty(String newpath)
    {
        String[] uids = storeManager.getConnectedUsers(storeId);
        if (uids.length > 0) {
            throw new DocRuntimeException("Cannot change store path. Users are still connected.");
        }
        if ((newpath == null) || (newpath.length() == 0)) {
            String oldpath = getStorePathProperty();
            if (pathfile.exists()) {
                if (pathfile.delete()) pathLoader = null;
                else throw new DocRuntimeException("Could not delete file " + pathfile);
            }
            return oldpath;
        } else {
            try {
                // Save new store path
                if (! pathfile.exists()) { 
                    pathfile.createNewFile();
                    pathLoader = null;
                    initPathLoader();
                }
                String oldpath = getStorePathProperty();
                updatePathFile(pathLoader, newpath);

                // Reset store type
                this.storeType = null;
                initStoreType();
                
                // Load store properties from new path (for filesystem-based store)
                File newdir = new File(newpath);
                propfile = new File(newdir, FilesystemStoreProperties.STORE_PROP_FILENAME);
                if (propfile.exists()) {
                    loadPropFile();
                } else {
                    setEmptyProps();  // newly created store has no properties yet
                }
                return oldpath;
            } catch (Exception ex) {
                throw new DocRuntimeException(ex);
            }
        }
    }

    /**
     * Saves the file-based properties of the store. 
     * If the store is a filesystem-based store, then this method saves
     * the stores properties.
     * If the store is an external database-store, then this method saves
     * the database connection data (if modified).
     * If the store is an embedded database-store, then this method has no effect.
     * @throws DocException 
     */
    public void savePropFile() throws DocException
    {
        if (isDbStore()) {
            // If dbExternalConnectionProps is null, then the connection 
            // properties have not yet been loaded and therefore have not 
            // been modified.
            if (isDbExternalStore() && (dbExternalConnectionProps != null)) {
                dbExternalConnectionProps.savePropFile(FILE_COMMENT_DB_CONNECTION);
            }
        } else {
            // Save properties of filesystem-based store
            super.savePropFile();
        }
    }
    
    static PropertiesLoader createPathLoader(File baseDir) 
    throws DocException
    {
        File pathfile = new File(baseDir, FilesystemStoreProperties.STORE_PATH_FILENAME);
        if (! pathfile.exists()) {
            try { 
                pathfile.createNewFile();
            } catch (IOException ex) {
                throw new DocException(ex);
            }
        }
        return new PropertiesLoader(pathfile);
    }
    
    static void createPathFile(File baseDir, String external_path) 
    throws DocException
    {
        updatePathFile(createPathLoader(baseDir), external_path);
    }
    
    static void updatePathFile(PropertiesLoader pathLoader, String external_path) 
    throws DocException
    {
        File external_dir = new File(external_path);
        if (! (external_dir.isDirectory() && external_dir.isAbsolute())) {
            throw new DocException("Invalid store path: " + external_dir);
        }
        pathLoader.setProp(FilesystemStoreProperties.PROP_STORE_PATH, external_path);
        pathLoader.savePropFile("Location of external store (absolute file path)");
    }
    
}
