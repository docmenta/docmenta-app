/*
 * RevisionStoreSessImpl.java
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

import java.util.*;
import java.io.*;

import org.docma.app.*;
import org.docma.coreapi.*;
import org.docma.coreapi.dbimplementation.DocStoreDbConnection;
import org.docma.util.*;

/**
 *
 * @author MP
 */
public class RevisionStoreSessImpl implements RevisionStoreSession
{
    private static final int ADD_SUB_DIR_ID_LENGTH = 5;
    private static final Comparator REV_COMPARATOR = new RevComparator();

    private final DocStoreSessionImpl docSess;
    private int maxRevisionsPerUser;

    public RevisionStoreSessImpl(DocStoreSession docStoreSess, int maxRevsPerUser)
    {
        if (! (docStoreSess instanceof DocStoreSessionImpl)) {
            throw new DocRuntimeException(
               "Filesystem implementation of RevisionStoreSession can only " +
               "be used with filesystem implementation of document store.");
        }
        this.docSess = (DocStoreSessionImpl) docStoreSess;
        this.maxRevisionsPerUser = maxRevsPerUser;
    }


    public DocContentRevision[] getRevisions(String storeId, DocVersionId verId,
                                             String nodeId, String langCode)
    {
        //
        // Database implementation
        //
        if (docSess.isDbStore(storeId)) {
            DocContentRevision[] res = null;
            synchronized (docSess) {
                DocStoreDbConnection dbcon = docSess.acquireDbConnection(storeId);
                try {
                    res = dbcon.getRevisions(storeId, verId, nodeId, langCode);
                } finally {
                    docSess.releaseDbConnection(storeId, dbcon);
                }
            }
            return res;
        }
        
        //
        // Filesystem-based implementation
        //
        File nodeDir = getNodeDir(storeId, verId, nodeId, langCode);
        if (! nodeDir.exists()) {
            return new DocContentRevision[0];
        }
        File[] farr = nodeDir.listFiles();
        ArrayList list = new ArrayList(farr.length);
        for (int i=0; i < farr.length; i++) {
            if (farr[i].isFile()) {  // skip directories
                try {
                    list.add(new ContentRevisionImpl(farr[i]));
                } catch (Exception ex) {} // skip files that do not comply to the revision filename pattern
            }
        }
        Collections.sort(list, REV_COMPARATOR);
        return (DocContentRevision[]) list.toArray(new DocContentRevision[list.size()]);
    }

    public SortedSet<String> getRevisionNodeIds(String storeId, DocVersionId verId) 
    {
        SortedSet<String> res = null;
        //
        // Database implementation
        //
        if (docSess.isDbStore(storeId)) {
            synchronized (docSess) {
                DocStoreDbConnection dbcon = docSess.acquireDbConnection(storeId);
                try {
                    res = dbcon.getRevisionNodeIds(storeId, verId);
                } finally {
                    docSess.releaseDbConnection(storeId, dbcon);
                }
            }
            return res;
        }
        
        //
        // Filesystem-based implementation
        //
        res = new TreeSet<String>();
        File revsDir = getRevisionsDir(storeId, verId);
        boolean isDir = revsDir.isDirectory();
        if (! isDir) {
            Log.warning("The revisions path is not a directory: " + revsDir.getAbsolutePath());
        }
        if (! (revsDir.exists() && isDir)) {
            return res;  // return empty set
        }
        getNodeIdsInDir(revsDir, res, 0);
        return res;
    }
    
    private void getNodeIdsInDir(File dir, SortedSet<String> res, int level)
    {
        File[] farr = dir.listFiles();
        for (int i=0; i < farr.length; i++) {
            File f = farr[i];
            if (f.isDirectory()) {  // skip if it's not a directory
                String fn = f.getName();
                if ((level == 0) && (fn.length() >= ADD_SUB_DIR_ID_LENGTH)) {
                    getNodeIdsInDir(f, res, 1);
                } else {
                    res.add(fn);
                }
            }
        }
    }

    public void deleteRevisions(String storeId, DocVersionId verId, String nodeId)
    {
        //
        // Database implementation
        //
        if (docSess.isDbStore(storeId)) {
            synchronized (docSess) {
                DocStoreDbConnection dbcon = docSess.acquireDbConnection(storeId);
                try {
                    dbcon.deleteRevisions(storeId, verId, nodeId);
                } finally {
                    docSess.releaseDbConnection(storeId, dbcon);
                }
            }
            return;
        }
        
        //
        // Filesystem-based implementation
        //
        File nodeDir = getNodeDir(storeId, verId, nodeId, null);
        DocmaUtil.recursiveFileDelete(nodeDir);
        // Note: The revisions of the translated content are stored in 
        // sub-directories of nodeDir, i.e. this method deletes the revisions
        // of the original content and of all translations of the content.
    }

    public void clearRevisions(String storeId, DocVersionId verId)
    {
        //
        // Database implementation
        //
        if (docSess.isDbStore(storeId)) {
            synchronized (docSess) {
                DocStoreDbConnection dbcon = docSess.acquireDbConnection(storeId);
                try {
                    dbcon.clearRevisions(storeId, verId);
                } finally {
                    docSess.releaseDbConnection(storeId, dbcon);
                }
            }
            return;
        }
        
        //
        // Filesystem-based implementation
        //
        File revDir = getRevisionsDir(storeId, verId);
        DocmaUtil.recursiveFileDelete(revDir);
    }

    public void addRevision(String storeId,
                            DocVersionId verId,
                            String nodeId,
                            String langCode,
                            String content,
                            Date revDate,
                            String userId)
    {
        try {
            addRevision(storeId, verId, nodeId, langCode, content.getBytes("UTF-8"), revDate, userId);
        } catch (UnsupportedEncodingException uee) {
            throw new DocRuntimeException(uee);
        }
    }

    public void addRevision(String storeId,
                            DocVersionId verId,
                            String nodeId,
                            String langCode,
                            byte[] content,
                            Date revDate,
                            String userId)
    {
        if (revDate == null) {
            revDate = new Date();  // set date to now
        }
        
        //
        // Database implementation
        //
        if (docSess.isDbStore(storeId)) {
            synchronized (docSess) {
                DocStoreDbConnection dbcon = docSess.acquireDbConnection(storeId);
                try {
                    dbcon.addRevision(storeId, verId, nodeId, langCode, content, revDate, userId, maxRevisionsPerUser);
                } finally {
                    docSess.releaseDbConnection(storeId, dbcon);
                }
            }
            return;
        }
        
        //
        // Filesystem-based implementation
        //
        File nodeDir = getNodeDir(storeId, verId, nodeId, langCode);
        if (! nodeDir.exists()) {
            if (! nodeDir.mkdirs()) {
                throw new DocRuntimeException("Could not create revision directory: " + nodeDir);
            }
        }
        String filename = revisionFilename(nodeId, langCode, revDate, userId);
        File revFile = new File(nodeDir, filename);
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(revFile);
            fout.write(content);
        } catch (Exception ex) {
            throw new DocRuntimeException(ex);
        } finally {
            try { if (fout != null) fout.close(); } catch (Exception ex2) {}
        }
        deleteOldRevisions(nodeDir, userId);
    }


    /* --------------  Package local methods  ---------------------- */

    static String revisionFilename(String nodeId, String langCode, Date revDate, String userId)
    {
        return nodeId + "_" + // ((langCode == null) ? "" : langCode + "_") +
               String.valueOf(revDate.getTime()) + "_" +
               userId + ".content";
    }

    static long getTimeFromFilename(String fn)
    {
        int endpos = fn.lastIndexOf('_');
        if (endpos > 0) {
            int startpos = fn.lastIndexOf('_', endpos - 1);
            String timestr = fn.substring(startpos + 1, endpos);
            return Long.parseLong(timestr);
        } else {
            return -1;
        }
    }

    static String getUserIdFromFilename(String fn)
    {
        int endpos = fn.lastIndexOf('.');
        if (endpos > 0) {
            int startpos = fn.lastIndexOf('_', endpos - 1);
            return fn.substring(startpos + 1, endpos);
        } else {
            return null;
        }
    }


    /* --------------  Private methods  ---------------------- */

    private void deleteOldRevisions(File nodeDir, String userId)
    {
        // if (! nodeDir.exists()) return;
        File[] files = nodeDir.listFiles();
        ArrayList list = new ArrayList(files.length);
        for (int i=0; i < files.length; i++) {
            File f = files[i];
            String fname = f.getName();
            if (f.isFile() && userId.equals(getUserIdFromFilename(fname))) {
                list.add(fname);
            }
        }
        if (list.size() > maxRevisionsPerUser) {
            try {
                Collections.sort(list, REV_COMPARATOR);
                int cnt = list.size() - maxRevisionsPerUser;
                boolean failed = false;
                for (int i=0; i < cnt; i++) {
                    File del_file = new File(nodeDir, (String) list.get(i));
                    if (! del_file.delete()) failed = true;
                }
                if (failed) Log.error("Could not delete revision file!");
            } catch (Exception ex) {
                Log.error("Could not delete old revisions: " + ex.getMessage());
                if (DocmaConstants.DEBUG) ex.printStackTrace();
            }
        }
    }

    private File getRevisionsDir(String storeId, DocVersionId verId)
    {
        File revDir;
        // if (docSess.isDbStore(storeId)) {
        //     File storeDir = docSess.getStoreDirFromId(storeId);
        //     File baseDir = new File(storeDir, "revisions");
        //     if (! baseDir.exists()) baseDir.mkdir();
        //     revDir = new File(baseDir, docSess.getVersionUUID(storeId, verId).toString());
        // } else {
            File storeDir = docSess.getDocStoreDir(storeId, verId);
            revDir = new File(storeDir, "revisions");
        // }
        if (! revDir.exists()) revDir.mkdir();
        return revDir;
    }

    private File getNodeDir(String storeId, DocVersionId verId, String nodeId, String langCode)
    {
        File dir = getRevisionsDir(storeId, verId);
        if (nodeId.length() >= ADD_SUB_DIR_ID_LENGTH) {
            String subpath = nodeId.substring(0, ADD_SUB_DIR_ID_LENGTH);
            dir = new File(dir, subpath);
        }
        File nodeDir = new File(dir, nodeId);
        if (langCode == null) {
            return nodeDir;
        } else {
            return new File(nodeDir, langCode.toLowerCase());
        }
    }


    static class RevComparator implements Comparator
    {

        public int compare(Object arg0, Object arg1)
        {
            long time0;
            long time1;
            if (arg0 instanceof String) {
                time0 = getTimeFromFilename((String) arg0);
                time1 = getTimeFromFilename((String) arg1);
            } else {
                time0 = ((DocContentRevision) arg0).getDate().getTime();
                time1 = ((DocContentRevision) arg1).getDate().getTime();
            }
            long diff = time0 - time1;
            if (diff < 0) return -1;
            if (diff > 0) return 1;
            return 0;
        }

    }
}
