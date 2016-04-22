/*
 * PublicationArchivesSessImpl.java
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

package org.docma.app.fsimplementation;

import java.util.*;
import java.io.*;
import org.docma.app.DocmaConstants;
import org.docma.coreapi.*;
import org.docma.coreapi.fsimplementation.DocStoreSessionImpl;
import org.docma.app.dbimplementation.PublicationArchiveDbImpl;
import org.docma.util.Log;

/**
 *
 * @author MP
 */
public class PublicationArchivesSessImpl implements PublicationArchivesSession
{
    public static final String PUBLICATION_ARCHIVES_FOLDERNAME = "publications";
    
    private static final Map<String, PublicationArchive> globalArchives = new HashMap<String, PublicationArchive>();

    private DocStoreSessionImpl docSess;
    private File tempDir;


    public PublicationArchivesSessImpl(DocStoreSession sess, File tempDir)
    {
        if (! (sess instanceof DocStoreSessionImpl)) {
            throw new DocRuntimeException(
               "Filesystem implementation of PublicationArchives can only " +
               "be used with filesystem implementation of document store: " + 
                ((sess == null) ? "null" : sess.getClass().getName()));
        }
        this.docSess = (DocStoreSessionImpl) sess;
        this.tempDir = tempDir;
    }

    public PublicationArchive getArchive(String storeId, DocVersionId verId)
    {
        boolean is_db_store = docSess.isDbStore(storeId);
        String key = getGlobalArchiveKey(storeId, verId);
        synchronized (globalArchives) {
            PublicationArchive ar = globalArchives.get(key);
            if (ar == null) {
                if (is_db_store) {
                    //
                    // Database store (two cases exist: store exports in database or in filesystem)
                    //
                    String ar_type = docSess.getDocStoreProperty(storeId, DocmaConstants.PROP_STORE_ARCHIVE_TYPE);
                    boolean is_db_archive = (ar_type != null) && ar_type.equalsIgnoreCase(DocmaConstants.ARCHIVE_TYPE_DB);
                    if (is_db_archive) {
                        // Case 1: Exported publications are stored in the database.
                        ar = new PublicationArchiveDbImpl(storeId, verId, 
                                                          docSess.getVersionIdFactory(), 
                                                          docSess.getDbConnectionData(storeId), 
                                                          tempDir);
                    } else {
                        // Case 2: Exported publications are stored in the filesystem.
                        File baseDir = defaultDbArchiveBase(storeId, verId);
                        PublicationArchiveImpl ar_fs = new PublicationArchiveImpl(storeId, verId, baseDir);
                        ar_fs.prepareAccess();   // prepare directories
                        ar = ar_fs;
                    }
                } else {
                    //
                    // Filesystem store (exported publications are stored in the filesystem)
                    //
                    File storeVersionDir = docSess.getDocStoreDir(storeId, verId);
                    File baseDir = new File(storeVersionDir, "publications");
                    PublicationArchiveImpl ar_fs = new PublicationArchiveImpl(storeId, verId, baseDir);
                    ar_fs.prepareAccess();   // prepare directories
                    ar = ar_fs;
                }
                globalArchives.put(key, ar);
            } else {
                if (ar instanceof PublicationArchiveImpl) { // if filesystem-based archive
                    ((PublicationArchiveImpl) ar).prepareAccess();  // prepare directories
                }
            }
            return ar;
        }
    }

    public void invalidateCache(String storeId) 
    {
        synchronized (globalArchives) {
            int old_cnt = globalArchives.size();
            int remove_cnt = 0;
            Iterator<PublicationArchive> it = globalArchives.values().iterator();
            while (it.hasNext()) {
                PublicationArchive ar = it.next();
                if (storeId.equals(ar.getDocStoreId())) {
                    it.remove();
                    ++remove_cnt;
                    release_archive(ar);
                }
            }
            if (globalArchives.size() != (old_cnt - remove_cnt)) {
                Log.warning("Removing of PublicationArchive instances from globalArchives cache failed!");
            }
        }
    }
    
    public void invalidateCache(String storeId, DocVersionId verId)
    {
        synchronized (globalArchives) {
            PublicationArchive ar = globalArchives.remove(getGlobalArchiveKey(storeId, verId));
            if (ar != null) {
                release_archive(ar);
            }
        }
    }
    
    private void release_archive(PublicationArchive ar) 
    {
        if (ar instanceof PublicationArchiveDbImpl) {
            ((PublicationArchiveDbImpl) ar).close();
        }
    }

    private static String getGlobalArchiveKey(String storeId, DocVersionId verId)
    {
        return storeId + "#" + verId.toString();
    }

    private File defaultDbArchiveBase(String storeId, DocVersionId verId)
    {
        File storeDir = docSess.getStoreDirFromId(storeId);
        File exportDir = new File(storeDir, PUBLICATION_ARCHIVES_FOLDERNAME);
        return new File(exportDir, docSess.getVersionUUID(storeId, verId).toString());
    }
    
            
}
