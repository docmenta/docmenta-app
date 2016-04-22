/*
 * UserSessionImpl.java
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

import java.util.*;

import org.docma.coreapi.DocVersionId;
import org.docma.coreapi.DocException;
import org.docma.app.*;
import org.docma.plugin.*;

/**
 *
 * @author MP
 */
public class UserSessionImpl implements UserSession
{
    final DocmaSession docmaSess;
    private StoreConnectionImpl openedStore = null;
    private UserImpl user = null;
    private final Map<String, StoreConnectionImpl> tempStores = new HashMap<String, StoreConnectionImpl>();
    
    UserSessionImpl(DocmaSession docmaSess)
    {
        this.docmaSess = docmaSess;
    }
    
    // ********* Interface UserSession (visible by plugins) **********

    public String getSessionId() 
    {
        return docmaSess.getSessionId();
    }

    public ApplicationContext getApplicationContext() 
    {
        return docmaSess.getApplicationContext();
    }

    public User getUser() 
    {
        if (user == null) {
            user = new UserImpl(docmaSess.getUserManager(), docmaSess.getUserId());
        }
        return user;
    }

    public StoreConnection getOpenedStore() 
    {
        String storeId = docmaSess.getStoreId();
        DocVersionId verId = docmaSess.getVersionId();
        if ((storeId == null) || (verId == null)) {
            return null;
        }
        if ((openedStore != null) && openedStore.isClosed()) {
            openedStore = null;
        }
        if (openedStore == null) {
            openedStore = new StoreConnectionImpl(docmaSess, storeId, verId, true);
        }
        return openedStore;
    }

    public VersionId createVersionId(String verId) throws InvalidVersionIdException
    {
        try {
            return VersionIdCreator.create(docmaSess.createVersionId(verId));
        } catch (DocException dex) {
            throw new InvalidVersionIdException(dex);
        }
    }

    public StoreConnection createTempStoreConnection(String storeId, String verId) throws Exception
    {
        return createTempStoreConnection(storeId, createVersionId(verId));
    }
    
    public StoreConnection createTempStoreConnection(String storeId, VersionId verId) throws Exception
    {
        DocmaSession new_sess = docmaSess.createNewSession();
        try {
            DocVersionId dver = versionIdToInternal(verId);
            new_sess.openDocStore(storeId, dver);

            StoreConnectionImpl temp_conn = new StoreConnectionImpl(new_sess, storeId, dver, false);
            String conn_id = temp_conn.getConnectionId();
            if (tempStores.containsKey(conn_id)) {
                throw new Exception("Store connection ID is not unique.");
            }
            tempStores.put(conn_id, temp_conn);
            return temp_conn;
        } catch (Exception ex) {
            try {
                new_sess.closeSession();
            } catch (Exception ex2) {}
            throw ex;
        }
    }

    public void closeTempStoreConnection(StoreConnection conn) 
    {
        StoreConnectionImpl impl = (StoreConnectionImpl) conn;
        String conn_id = impl.getConnectionId();
        if (tempStores.remove(conn_id) != null) {
            impl.close();
        }
    }

    public String[] listStores() 
    {
        return docmaSess.listDocStores();
    }

    public VersionId[] listVersions(String storeId) 
    {
        DocVersionId[] vids = docmaSess.listVersions(storeId);
        if (vids == null) {
            return null;
        }
        VersionId[] res = new VersionId[vids.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = VersionIdCreator.create(vids[i]);
        }
        return res;
    }

    public VersionId getLatestVersion(String storeId) 
    {
        DocVersionId vid = docmaSess.getLatestVersionId(storeId);
        return (vid == null) ? null : VersionIdCreator.create(vid);
    }

    // ********* Other methods (not visible by plugins) **********

    public void onSessionClose()
    {
        // Close all temporary store connections
        Iterator<StoreConnectionImpl> it = tempStores.values().iterator();
        while (it.hasNext()) {
            try {
                StoreConnectionImpl sconn = it.next();
                it.remove();
                sconn.close();
            } catch (Exception ex) {}
        }
    }

    DocmaSession getDocmaSession()
    {
        return docmaSess;
    }
    
    DocVersionId versionIdToInternal(VersionId verId) throws Exception
    {
        DocVersionId dver = null;
        if (verId instanceof VersionIdImpl) {
            dver = ((VersionIdImpl) verId).getDocVersionId();
        }
        if (dver == null) {
            dver = docmaSess.createVersionId(verId.toString());
        }
        return dver;
    }

}
