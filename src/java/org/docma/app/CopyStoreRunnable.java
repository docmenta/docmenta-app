/*
 * CopyStoreRunnable.java
 * 
 *  Copyright (C) 2014  Manfred Paula, http://www.docmenta.org
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
import org.docma.util.DocmaUtil;
import org.docma.app.fsimplementation.PublicationArchiveImpl;
import org.docma.util.Log;

/**
 *
 * @author MP
 */
public class CopyStoreRunnable implements Runnable
{
    private DocmaSession userSess = null;  // The session of the user that starts the activity.
    private String sourceStoreId = null;
    private String targetStoreId = null;
    private DocVersionId[] versionIds = null;  // null means all versions
    private String[] langCodes = null;        // null means all languages
    private boolean verify;
    private boolean copyExports;
    private boolean copyRevisions;

    private ProgressCallback progress;
    
    public CopyStoreRunnable(ProgressCallback progress,
                             DocmaSession userSess,
                             String sourceStoreId,
                             String targetStoreId,
                             DocVersionId[] versionIds,  // null means all versions
                             String[] langCodes,         // null means all languages
                             boolean verify,
                             boolean copyExports, 
                             boolean copyRevisions)
    {
        this.progress = progress;
        this.userSess = userSess;
        this.sourceStoreId = sourceStoreId;
        this.targetStoreId = targetStoreId;
        this.versionIds = versionIds;
        this.langCodes = langCodes;
        this.verify = verify;
        this.copyExports = copyExports;
        this.copyRevisions = copyRevisions;
    }

    public void run() 
    {
        DocmaSession sourceSess = null;
        DocmaSession targetSess = null;
        int step_count = 1;
        if (copyExports || copyRevisions) ++step_count;
        progress.startWork(step_count);
        try {
            // Note that the user session may be closed before the activity is finished.
            // Therefore separate sessions sourceSess and targetSess have to be
            // created, which have to be closed after the activity is finished. 
            sourceSess = userSess.createNewSession();
            targetSess = userSess.createNewSession();
            
            clearExportsInTarget(targetSess, progress);    // Delete any existing exports in target store
            clearRevisionsInTarget(targetSess);  // Delete any existing revisions in target store
            
            String src_orig = userSess.getOriginalLanguage(sourceStoreId).getCode();
            String tar_orig = userSess.getOriginalLanguage(targetStoreId).getCode();

            // If langCodes is null, then all languages of the source store have to be copied
            if (langCodes == null) { 
                DocmaLanguage[] src_langs = userSess.getTranslationLanguages(sourceStoreId);
                langCodes = new String[src_langs.length + 1];
                langCodes[0] = src_orig;
                for (int i=0; i < src_langs.length; i++) {
                    langCodes[i + 1] = src_langs[i].getCode();
                }
            }
            
            // Create missing translation languages in target store
            targetSess.startTransaction();
            try {
                for (String lcode : langCodes) {
                    if (!lcode.equals(tar_orig) && !targetSess.hasTranslationLanguage(targetStoreId, lcode)) {
                       targetSess.addTranslationLanguage(targetStoreId, lcode);
                    }
                }
                targetSess.commitTransaction();
            } catch (Throwable ex) {
                targetSess.rollbackTransaction();
                throw ex;
            }

            // Assure that the original language of the target store is in the
            // list of languages to be copied.
            List<String> langList = Arrays.asList(langCodes);
            if (! langList.contains(tar_orig)) {
                throw new Exception("Original language of target product has to be copied: " + tar_orig);
            }

            // Three cases have to be distinguished:
            // 1) If the original language of the target store is the same as of
            //    the source store, then origAsTrans and transAsOrig are null.
            // 2) Otherwise a translation language of the source store is
            //    mapped to the original language of the target store. This is
            //    indicated by setting transAsOrig to the translation language 
            //    to be mapped.
            // 3) Setting origAsTrans to the original language of the source 
            //    store indicates, that the original language of the source 
            //    is mapped to a translation language in the target store.
            // The list trans_list is the list of translation languages in the
            // source store that needs to be copied as translation language to 
            // the target store.
            boolean copyOriginal = langList.contains(src_orig);
            boolean mapTranslationToOriginal = !tar_orig.equals(src_orig);
            List<String> trans_list = new ArrayList<String>(langList);
            String origAsTrans = null;
            String transAsOrig = null;
            if (mapTranslationToOriginal) {
                // map translation of source store to original language of target store
                transAsOrig = tar_orig;
                trans_list.remove(tar_orig);
                if (copyOriginal) {
                    // map original language of source store to translation language of target store
                    origAsTrans = src_orig;   
                    trans_list.remove(src_orig);
                }
            } else {
                // map original language of source store to original language of target store
                trans_list.remove(src_orig);
            }
            String[] trans = trans_list.toArray(new String[trans_list.size()]);
            
            // Define store properties that shall NOT be overwritten in the  
            // target store (if target store already exists).
            Set<String> skipStoreProps = new HashSet<String>();
            skipStoreProps.add(DocmaConstants.PROP_STORE_ARCHIVE_TYPE);
            skipStoreProps.add(DocmaConstants.PROP_STORE_DISPLAYNAME);
            skipStoreProps.add(DocmaConstants.PROP_STORE_ORIG_LANGUAGE);
            skipStoreProps.add(DocmaConstants.PROP_STORE_TRANSLATION_LANGUAGES);

            // Perform the copy operation on the persistence layer (DocStoreSession)
            DocStoreUtilities.copyDocStore(sourceSess.getBackendSession(), sourceStoreId, 
                                           targetSess.getBackendSession(), targetStoreId, 
                                           versionIds, trans, transAsOrig, origAsTrans,
                                           skipStoreProps, progress, verify);
            progress.stepFinished();  // indicate finishing of first step
            
            if (copyExports || copyRevisions) {  // if second step
                progress.startWork((copyExports && copyRevisions) ? 2 : 1);  // start sub-work with 1 or 2 steps
                try {
                    if (copyExports) {
                        copyAllExports(targetSess);
                        progress.stepFinished();
                    }
                    if (copyRevisions) {
                        copyAllRevisions(sourceSess, trans, transAsOrig, origAsTrans);  // use sourceSess to travers all nodes
                        progress.stepFinished();
                    }
                } finally {
                    progress.finishWork();  // sub-work finished
                }
                progress.stepFinished();  // indicate finishing of second step
            }
            
            int errcnt = progress.getErrorCount();
            if (errcnt == 0) {
                progress.setMessage("text.copy_store_finished_success");
            } else {
                progress.setMessage("text.copy_store_finished_errorcount", errcnt);
            }
        } catch (Throwable ex) {
            if (progress.getErrorCount() == 0) {
                progress.logError("text.copy_store_finished_error", ex.getLocalizedMessage());
                ex.printStackTrace();
            } else {
                ex.printStackTrace();
            }
        } finally {
            try { if (sourceSess != null) sourceSess.closeSession(); } catch (Exception ex1) {}
            try { if (targetSess != null) targetSess.closeSession(); } catch (Exception ex2) {}
            progress.setFinished();  // indicate that complete activity is finished
        }
    }
    
    private void clearRevisionsInTarget(DocmaSession docmaSess) throws Exception
    {
        RevisionStoreSession revSess = docmaSess.getRevisionStore();
        DocVersionId[] vIds = docmaSess.listVersions(targetStoreId);
        for (DocVersionId vId : vIds) {
            checkCanceledByUser(progress);
            revSess.clearRevisions(targetStoreId, vId);
        }
    }

    private void copyAllRevisions(DocmaSession docmaSess, 
                                  String[] trans, 
                                  String transAsOrig, 
                                  String origAsTrans)
    throws Exception
    {
        DocVersionId[] vIds = docmaSess.listVersions(targetStoreId);
        RevisionStoreSession revSess = docmaSess.getRevisionStore();
        if (vIds.length > 0) {
            progress.setMessage("text.copy_revisions_started");
            progress.startWork(vIds.length); 
            try {
                for (DocVersionId vId : vIds) {
                    progress.setMessage("text.copy_revisions_of_version", vId.toString());
                    // Assure that session is initially in original mode
                    if (docmaSess.getTranslationMode() != null) {
                        docmaSess.leaveTranslationMode();
                    }
                    SortedSet<String> revIds = revSess.getRevisionNodeIds(sourceStoreId, vId);
                    if ((revIds != null) && !revIds.isEmpty()) {  // if revisions exist
                        // Copy revisions for the given version 
                        try {
                            docmaSess.openDocStore(sourceStoreId, vId);
                            for (String nodeId : revIds) {
                                checkCanceledByUser(progress);
                                DocmaNode srcNode = docmaSess.getNodeById(nodeId);
                                if (srcNode != null) {
                                    copyRevisionsOfNode(docmaSess, vId, srcNode, 
                                                        revSess, trans, transAsOrig, origAsTrans);
                                } else {
                                    progress.logWarning("Could not find node for revision node-id '" + nodeId + "'.");
                                }
                            }
                        } catch (Exception ex) {
                            progress.logError("text.copy_revisions_finished_error", vId.toString(), ex.getLocalizedMessage());
                            // if exception was caused by cancel operation of user,
                            // then rethrow exception, otherwise continue copy 
                            // operation with next version.
                            if (progress.getCancelFlag()) {
                                throw ex;  // rethrow exception
                            }
                        } finally {
                            try { docmaSess.closeDocStore(); } catch (Exception ex) {}
                        }
                    }
                    progress.stepFinished();
                }
            } finally {
                progress.finishWork();
            }
        }
    }
    
    private void copyRevisionsOfNode(DocmaSession sourceSess,
                                     DocVersionId verId, 
                                     DocmaNode sourceNode, 
                                     RevisionStoreSession revSess, 
                                     String[] trans, 
                                     String transAsOrig, 
                                     String origAsTrans)
    throws DocException
    {
        if (sourceNode.isContent()) {
            // Create revisions of the original language in the target store
            copyNodeRevisions(sourceSess, verId, sourceNode, revSess, transAsOrig, null);
            
            // Create revisions of translated content
            if (origAsTrans != null) {
                // The original language of the source store has been mapped
                // to a translation language in the target store.
                copyNodeRevisions(sourceSess, verId, sourceNode, revSess, null, origAsTrans);
            }
            for (int i = 0; i < trans.length; i++) {
                copyNodeRevisions(sourceSess, verId, sourceNode, revSess, trans[i], trans[i]);
            }
        }
        
        // Assure that session is in original mode (after content revisions have been copied)
        if (sourceSess.getTranslationMode() != null) {
            sourceSess.leaveTranslationMode();
        }
    }
    
    private void copyNodeRevisions(DocmaSession docmaSess,
                                   DocVersionId verId, 
                                   DocmaNode sourceNode, 
                                   RevisionStoreSession revSess, 
                                   String sourceLang, 
                                   String targetLang)
    throws DocException
    {
        checkCanceledByUser(progress);

        // Get node revisions of language sourceLang (null means "original" language)
        if (sourceLang == null) {
            if (docmaSess.getTranslationMode() != null) {
                docmaSess.leaveTranslationMode();
            }
        } else {
            docmaSess.enterTranslationMode(sourceLang);
        }
        DocContentRevision[] revs = sourceNode.getRevisions();

        // Copy revisions to corresponding node and language in target store  
        if ((revs != null) && (revs.length > 0)) {
            String node_id = sourceNode.getId();
            try {
                for (DocContentRevision rev : revs) {
                    revSess.addRevision(targetStoreId, verId, node_id, targetLang, 
                                        rev.getContent(), rev.getDate(), rev.getUserId());
                }
            } catch (Exception ex) {
                final String displayLang = (targetLang == null) ? "original" : targetLang; 
                progress.logWarning("text.copy_revision_failed", node_id, displayLang, ex.getLocalizedMessage());
            }
        }
    }

    private void clearExportsInTarget(DocmaSession docmaSess, ProgressCallback progress) 
    throws Exception
    {
        PublicationArchivesSession pubArchives = docmaSess.getPublicationArchives();
        DocVersionId[] vIds = docmaSess.listVersions(targetStoreId);
        for (DocVersionId vId : vIds) {
            PublicationArchive tarArchive = pubArchives.getArchive(targetStoreId, vId);
            String[] pubIds = tarArchive.listPublications();
            for (String pubId : pubIds) {
                checkCanceledByUser(progress);
                tarArchive.deletePublication(pubId);
            }
            if (tarArchive instanceof PublicationArchiveImpl) {
                try {
                    PublicationArchiveImpl aimpl = (PublicationArchiveImpl) tarArchive;
                    File adir = aimpl.getBaseDir();
                    if ((adir.list().length > 0) || !adir.delete()) {
                        progress.logWarning("text.copy_exports_delete_target_dir_failed", adir.getAbsolutePath());
                    }
                } catch (Exception ex) {
                    progress.logWarning("error.exception", "Could not delete export directory in target store: " + ex.getMessage());
                }
            }
        }
    }
    
    private void copyAllExports(DocmaSession docmaSess)
    throws Exception
    {
        DocVersionId[] vIds = docmaSess.listVersions(targetStoreId);
        PublicationArchivesSession pubArchives = docmaSess.getPublicationArchives();
        if (vIds.length > 0) {
            progress.setMessage("text.copy_exports_started");
            progress.startWork(vIds.length); 
            try {
                for (DocVersionId vId : vIds) {
                    try {
                        progress.setMessage("text.copy_exports_of_version", vId.toString());
                        PublicationArchive srcArchive = pubArchives.getArchive(sourceStoreId, vId);
                        PublicationArchive tarArchive = pubArchives.getArchive(targetStoreId, vId);
                        copyExports(srcArchive, tarArchive);
                    } catch (Exception ex) {
                        progress.logError("text.copy_exports_finished_error", vId.toString(), ex.getLocalizedMessage());
                        // if exception was caused by cancel operation of user,
                        // then rethrow exception, otherwise continue copy 
                        // operation with next version.
                        if (progress.getCancelFlag()) {
                            throw ex;  // rethrow exception
                        } else {
                            ex.printStackTrace();
                        }
                    } finally {
                        try {  // release allocated memory to avoid out of memory problems
                            pubArchives.invalidateCache(sourceStoreId, vId);
                            pubArchives.invalidateCache(targetStoreId, vId);
                        } catch (Exception ex2) {
                            ex2.printStackTrace();
                        }
                    }
                    progress.stepFinished();
                }
            } finally {
                progress.finishWork();
            }
        }
    }
    
    private void copyExports(PublicationArchive sourceArchive, 
                             PublicationArchive targetArchive) throws DocException
    {
        String[] pubIds = sourceArchive.listPublications();
        for (String pubId : pubIds) {
            checkCanceledByUser(progress);
            try {
                String pubLang = sourceArchive.getAttribute(pubId, PublicationArchive.ATTRIBUTE_PUBLICATION_LANGUAGE);
                String pubFilename = sourceArchive.getAttribute(pubId, PublicationArchive.ATTRIBUTE_PUBLICATION_FILENAME);
                String targetPubId = targetArchive.createPublication(pubId, pubLang, pubFilename);
                if (! targetPubId.equals(pubId)) {
                    progress.logWarning("text.copy_export_id_changed", pubId, targetPubId);
                }
                
                // Copy publication stream
                if (sourceArchive.hasPublicationStream(pubId)) {
                    InputStream in = null;
                    OutputStream out = null;
                    try {
                        in = sourceArchive.readPublicationStream(pubId);
                        out = targetArchive.openPublicationOutputStream(targetPubId);
                        DocmaUtil.copyStream(in, out);
                    } catch (Exception ex) {
                        progress.logError("text.copy_export_stream_failed", pubId, targetPubId, ex.getLocalizedMessage());
                    } finally {
                        if (out != null) {
                            try {
                                targetArchive.closePublicationOutputStream(targetPubId); 
                            } catch (Exception ex) {
                                progress.logWarning("text.copy_export_close_failed", targetPubId, ex.getLocalizedMessage());
                            }
                        }
                        if (in != null) { 
                            try { in.close(); } catch (Exception ex) {
                                progress.logWarning("text.copy_export_close_failed", pubId, ex.getLocalizedMessage());
                            }
                        }
                    }
                }
                
                // Copy publication export log
                if (sourceArchive.hasExportLog(pubId)) {
                    try {
                        targetArchive.writeExportLog(targetPubId, sourceArchive.readExportLog(pubId));
                    } catch (Exception ex) {
                        progress.logWarning("text.copy_export_log_failed", pubId, targetPubId, ex.getLocalizedMessage());
                    }
                }
                
                // Copy attributes
                String[] att_names = sourceArchive.getAttributeNames(pubId);
                if (att_names.length > 0) {
                    String[] att_values = new String[att_names.length];
                    for (int i=0; i < att_names.length; i++) {
                        att_values[i] = sourceArchive.getAttribute(pubId, att_names[i]);
                    }
                    targetArchive.setAttributes(targetPubId, att_names, att_values);
                }
            } catch (Exception ex) {
                progress.logError("text.copy_export_exception", pubId, ex.getLocalizedMessage());
                ex.printStackTrace();
            }
        }
    }

    private static void checkCanceledByUser(ProgressCallback progress) throws DocException
    {
        if (progress.getCancelFlag()) {
            throw new DocException("The operation has been canceled by the user!");
        }
    }

}
