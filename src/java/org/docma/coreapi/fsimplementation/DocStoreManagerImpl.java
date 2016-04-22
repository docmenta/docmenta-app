/*
 * DocStoreManagerImpl.java
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
import org.docma.coreapi.*;
import org.docma.coreapi.dbimplementation.DocStoreDbImpl;
import org.docma.coreapi.implementation.*;
// import org.docma.userapi.*;


/**
 *
 * @author MP
 */
public final class DocStoreManagerImpl extends AbstractDocStoreManager
{
    // private UserManager userMgmt = null;
    
    private File baseDir = null; 
    private File storesDir = null;
    private File storeDTDFile = null;
    
    
    /**
     * Creates a new instance of DocStoreManagerImpl
     */
    public DocStoreManagerImpl()
    {
    }

    public DocStoreManagerImpl(String baseDirectory)
    {
        setBaseDirectory(baseDirectory);
    }

    public DocStoreManagerImpl(String baseDirectory, String storeDTDFile)
    {
        setBaseDirectory(baseDirectory);
        setStoreDTDFile(storeDTDFile);
    }

    /* --------------  Configuration: injected objects  -------------------- */
    
    public void setBaseDirectory(String path)
    {
        baseDir = new File(path);
        storesDir = new File(baseDir, getStoresPath());
        if (! storesDir.exists()) storesDir.mkdir();
    }

    public void setStoreDTDFile(String path)
    {
        storeDTDFile = new File(path);
    }

    // public void setUserManager(UserManager um) {
    //     userMgmt = um;
    // }

    /* --------  Implementation specific retrieval methods  ------------ */

    public String getBaseDirectory()
    {
        return baseDir.getAbsolutePath();
    }

    public String getStoreDTDFile()
    {
        return storeDTDFile.getAbsolutePath();
    }

    public String getStoresDirectory()
    {
        return storesDir.getAbsolutePath();
    }
    
    public String getStoresPath()
    {
        return "docstores";
    }
    
    /* --------------  Implementation of abstract methods --------------- */

    protected DocStoreSession createSessionInstance(String sessionId, String userId)
    {
        return new DocStoreSessionImpl(this, sessionId, userId);
    }

    protected AbstractDocStore createStoreInstance(DocStoreSession sess, String storeId, DocVersionId verId)
    {
        DocStoreSessionImpl sessImpl = (DocStoreSessionImpl) sess;
        if (sessImpl.isDbStore(storeId)) {
            // LockManager lm = new LockManagerDbImpl(storeId, verId, getVersionIdFactory(), 
            //                                        sessImpl.getDbConnectionData(storeId));
            return new DocStoreDbImpl(storeId, verId, getVersionIdFactory(), sessImpl.getDbConnectionData(storeId));
        } else {
            File dir = ((DocStoreSessionImpl) sess).getDocStoreDir(storeId, verId);
            return new DocStoreImpl(dir, storeDTDFile, storeId, verId);
        }
    }

}
