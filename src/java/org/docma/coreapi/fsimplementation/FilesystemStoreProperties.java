/*
 * FilesystemStoreProperties.java
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

import org.docma.coreapi.dbimplementation.DbConstants;

/**
 *
 * @author MP
 */
public class FilesystemStoreProperties
{
    static final String STORE_PATH_FILENAME = "storepath.properties";
    public static final String STORE_PROP_FILENAME = "store.properties";
    // External DB connection properties filename
    static final String DB_CONNECTION_FILENAME = "db.properties";
    // Embedded DB directory, JDBC driver class and dialect
    public static final String DB_EMBEDDED_FOLDERNAME = "dbstore";
    public static final String DB_EMBEDDED_DRIVER = DbConstants.DB_EMBEDDED_DRIVER;
    public static final String DB_EMBEDDED_DIALECT = DbConstants.DB_EMBEDDED_DIALECT;
    
    // Store property names
    public static final String PROP_STORE_BASEPATH = "docstore.basepath";
    public static final String PROP_STORE_PATH = "docstore.path";
    public static final String PROP_STORE_TYPE = "docstore.type";

    // PROP_STORE_TYPE property values
    public static final String STORE_TYPE_FS = "fs_store";
    public static final String STORE_TYPE_DB_EMBEDDED = "db_embedded_store";
    public static final String STORE_TYPE_DB_EXTERNAL = "db_external_store";

    // External DB connection property names
    public static final String PROP_DB_DIALECT = "db.dialect";
    public static final String PROP_DB_DRIVER_CLASS = "db.connection.driver_class";
    public static final String PROP_DB_CONNECTION_URL = "db.connection.url";
    public static final String PROP_DB_CONNECTION_USER = "db.connection.username";
    public static final String PROP_DB_CONNECTION_PWD = "db.connection.password";
    
}
