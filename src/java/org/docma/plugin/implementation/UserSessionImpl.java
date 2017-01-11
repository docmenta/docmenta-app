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

import java.io.File;
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
    private UserImpl user = null;
    
    public UserSessionImpl(DocmaSession docmaSess)
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

    public Locale getCurrentLocale()
    {
        return docmaSess.getI18n().getCurrentLocale();
    }

    public String  getLabel(String key, Object... args)
    {
        return docmaSess.getI18n().getLabel(key, args);
    }

    public StoreConnection getOpenedStore() 
    {
        return docmaSess.getPluginStoreConnection();
    }

    public VersionId createVersionId(String verId) throws InvalidVersionIdException
    {
        try {
            return VersionIdCreator.create(docmaSess.createVersionId(verId));
        } catch (DocException dex) {
            throw new InvalidVersionIdException(dex);
        }
    }

    public StoreConnection createTempStoreConnection(String storeId, String verId) throws DocmaException
    {
        return createTempStoreConnection(storeId, createVersionId(verId));
    }
    
    public StoreConnection createTempStoreConnection(String storeId, VersionId verId) throws DocmaException
    {
        DocmaSession new_sess = docmaSess.createChildSession();
        try {
            DocVersionId dver = versionIdToInternal(verId);
            new_sess.openDocStore(storeId, dver);
            return new_sess.getPluginStoreConnection();
        } catch (Exception ex) {
            try {
                new_sess.closeSession();
            } catch (Exception ex2) {}
            throw new DocmaException(ex);
        }
    }

    public String[] listStores() throws DocmaException
    {
        try {
            return docmaSess.listDocStores();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public VersionId[] listVersions(String storeId) throws DocmaException
    {
        DocVersionId[] vids;
        try {
            vids = docmaSess.listVersions(storeId);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
        if (vids == null) {
            return null;
        }
        VersionId[] res = new VersionId[vids.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = VersionIdCreator.create(vids[i]);
        }
        return res;
    }

    public VersionId getLatestVersionId(String storeId) throws DocmaException
    {
        DocVersionId vid;
        try {
            vid = docmaSess.getLatestVersionId(storeId);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
        return (vid == null) ? null : VersionIdCreator.create(vid);
    }

    public void createVersion(String storeId, VersionId baseVersion, VersionId newVersion) throws DocmaException
    {
        try {
            DocVersionId basever = versionIdToInternal(baseVersion);
            DocVersionId newver = versionIdToInternal(newVersion);
            docmaSess.createVersion(storeId, basever, newver);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void deleteVersion(String storeId, VersionId verId) throws DocmaException 
    {
        try {
            docmaSess.deleteVersion(storeId, versionIdToInternal(verId));
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }
    
    public void renameVersion(String storeId, VersionId oldVerId, VersionId newVerId) throws DocmaException
    {
        try {
            docmaSess.renameVersion(storeId, versionIdToInternal(oldVerId), 
                                             versionIdToInternal(newVerId));
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public VersionId[] getSubVersions(String storeId, VersionId verId) throws DocmaException
    {
        DocVersionId[] subs;
        try {
            subs = docmaSess.getSubVersions(storeId, versionIdToInternal(verId));
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
        if (subs == null) {
            return new VersionId[0];
        }
        VersionId[] res = new VersionId[subs.length];
        for (int i=0; i < res.length; i++) {
            res[i] = VersionIdCreator.create(subs[i]);
        }
        return res;
    }
    
    public VersionId getVersionDerivedFrom(String storeId, VersionId verId) throws DocmaException
    {
        DocVersionId base;
        try {
            base = docmaSess.getVersionDerivedFrom(storeId, versionIdToInternal(verId));
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
        return (base == null) ? null : VersionIdCreator.create(base);
    }

    public ExportJob[] getExportQueue() throws DocmaException
    {
        DocmaExportJob[] queue;
        try {
            queue = docmaSess.getExportQueue();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
        if (queue == null) {
            return new ExportJob[0];
        }
        ExportJob[] res = new ExportJob[queue.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = new ExportJobImpl(queue[i]);
        }
        return res;
    }
    
    public Language[] listImportTranslations(File importDir) throws DocmaException
    {
        try {
            return docmaSess.getImportTranslationLanguages(importDir);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }
    
    // ********* Other methods (not visible by plugins) **********


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
