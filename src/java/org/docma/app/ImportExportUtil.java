/*
 * ImportExportUtil.java
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

import java.io.*;
import java.util.*;
import java.util.zip.*;

import org.docma.coreapi.*;
import org.docma.coreapi.fsimplementation.*;
import org.docma.util.*;

/**
 *
 * @author MP
 */
public class ImportExportUtil
{
    private static final String EXPORT_ZIP_PREFIX = "export_nodes_";
    private static final String EXPORT_DIR_PREFIX = "export_";
    private static final String IMPORT_DIR_PREFIX = "import_";

    private static final String EXPORTED_NODES_STORE_ID = "exported_nodes";

    public static File exportNodesToFile(DocmaSession sourceSess,
                                         DocmaNode[] nodes,
                                         File exportFile)
    {
        String storeDTDPath = sourceSess.getApplicationProperty(DocmaConstants.PROP_STORE_DTD_PATH);
        String tempPath = sourceSess.getApplicationProperty(DocmaConstants.PROP_TEMP_PATH);
        File storeDTDFile = new File(storeDTDPath);
        File tempDir = new File(tempPath);

        DocNode[] docnodes = new DocNode[nodes.length];
        for (int i=0; i < nodes.length; i++) {
            docnodes[i] = nodes[i].getBackendNode();
        }
        DocStoreSession sourceDocSess = sourceSess.getBackendSession();

        cleanTempDir(tempDir);

        long export_time = System.currentTimeMillis();
        File exportDir = new File(tempDir, EXPORT_DIR_PREFIX + export_time);
        if (! exportDir.exists()) {
            if (! exportDir.mkdirs()) throw new DocRuntimeException("Could not create export directory: " + exportDir);
        }

        DocStoreManagerImpl dsm = new DocStoreManagerImpl(exportDir.getAbsolutePath(),
                                                          storeDTDFile.getAbsolutePath());
        dsm.setVersionIdFactory(new DefaultVersionIdFactory());

        try {
            DocStoreSession exportSess = null;
            try {
                exportSess = dsm.connect(sourceSess.getUserId());

                DocVersionId verId = exportSess.createVersionId("0");
                exportSess.createDocStore(EXPORTED_NODES_STORE_ID, null, null);
                exportSess.createVersion(EXPORTED_NODES_STORE_ID, null, verId);
                exportSess.openDocStore(EXPORTED_NODES_STORE_ID, verId);

                exportSess.startTransaction();
                DocStoreUtilities.copyNodesToPosition(docnodes,
                                                      sourceDocSess,
                                                      exportSess.getRoot(),
                                                      null,  // nodeAfter, null means append
                                                      exportSess,
                                                      null,  // languages, null means all languages
                                                      false, // do not commit each node
                                                      DocStoreUtilities.COPY_NODE_ID_TRY_KEEP,
                                                      null,  // node-id map not needed
                                                      null,  // use default alias rename strategy (rename should not occur)
                                                      new HashMap(),  // alias map not needed
                                                      null);  // use default content copy strategy
                exportSess.commitTransaction();
                exportSess.closeDocStore();
                exportSess.closeSession();
            } catch (DocException ex) {  // do not catch DocRuntimeException
                throw new DocRuntimeException(ex);
            }

            // zip export directory
            if (exportFile == null) {
                exportFile = new File(tempDir, EXPORT_ZIP_PREFIX + export_time + ".zip");
            }
            File parentDir = exportFile.getParentFile();
            if (! parentDir.exists()) parentDir.mkdirs();

            try {
                OutputStream fout = new FileOutputStream(exportFile);
                ZipOutputStream zipout = new ZipOutputStream(fout);
                ZipUtil.addDirectoryToZip(zipout, exportDir);
                zipout.close();
                // fout.close();
            } catch (IOException ex) {  
                throw new DocRuntimeException(ex);
            }
        } finally {
            if (!DocmaConstants.DEBUG) {  // do not delete temporary files in debug mode
                try {
                    DocmaUtil.recursiveFileDelete(exportDir);
                } catch (Exception ex) {
                    Log.warning("Could not delete temporary export directory: " + exportDir);
                }
            }
        }
        return exportFile;
    }

    public static int importNodes(DocmaSession targetSess,
                                  DocmaNode parentNode,
                                  File importDir,
                                  String[] translations)
    {
        if (targetSess.getTranslationMode() != null) {
            throw new DocRuntimeException("Cannot import nodes in translation mode!");
        }
        DocStoreManagerImpl dsm = new DocStoreManagerImpl(importDir.getAbsolutePath());
        dsm.setVersionIdFactory(new DefaultVersionIdFactory());

        DocStoreSession targetDocSess = targetSess.getBackendSession();
        DocStoreSession importDocSess = null;
        boolean started = false;
        try {
            importDocSess = dsm.connect(targetSess.getUserId());
            DocVersionId verId = importDocSess.getLatestVersionId(EXPORTED_NODES_STORE_ID);
            importDocSess.openDocStore(EXPORTED_NODES_STORE_ID, verId);

            if (! targetSess.runningTransaction()) {
                targetSess.startTransaction();
                started = true;
            }

            DocGroup importRoot = importDocSess.getRoot();
            DocNode[] importNodes = importRoot.getChildNodes();
            DocGroup parentDocNode = (DocGroup) parentNode.getBackendNode();
            Map aliasMap = new HashMap();
            int cnt = 0;

            // If target node has section node, then copy non-group-nodes before the first section node.
            // This is to avoid that content nodes (DocXML) are inserted after section nodes.
            int first_sect_pos = parentNode.getChildPosFirstSection();
            if (first_sect_pos >= 0) {
                // separate group nodes and other nodes
                List groupList = new ArrayList(importNodes.length);
                List otherList = new ArrayList(importNodes.length);
                for (int i=0; i < importNodes.length; i++) {
                    if (importNodes[i] instanceof DocGroup) groupList.add(importNodes[i]);
                    else otherList.add(importNodes[i]);
                }
                if (otherList.size() > 0) {  // non-group-nodes exist
                    DocNode[] nonGroupNodes = (DocNode[]) otherList.toArray(new DocNode[otherList.size()]);
                    importNodes = (DocNode[]) groupList.toArray(new DocNode[groupList.size()]);

                    // copy non-group-nodes (content, files, images,...) before first section node
                    DocmaNode nodeAfter = parentNode.getChild(first_sect_pos);
                    DocNode docNodeAfter = nodeAfter.getBackendNode();
                    cnt +=
                      DocStoreUtilities.copyNodesToPosition(nonGroupNodes,
                                                            importDocSess,
                                                            parentDocNode,
                                                            docNodeAfter,
                                                            targetDocSess,
                                                            translations,  // languages, null means all translations
                                                            false, // do not commit each node
                                                            DocStoreUtilities.COPY_NODE_ID_TRY_KEEP,
                                                            null,  // node-id map not needed
                                                            null,  // use default alias rename strategy (rename should not occur)
                                                            aliasMap,
                                                            null);  // use default content copy strategy
                }
            }

            // append nodes (if target node has no section, then all nodes, otherwise only group nodes)
            cnt +=
                DocStoreUtilities.copyNodesToPosition(importNodes,
                                                      importDocSess,
                                                      parentDocNode,
                                                      null,  // nodeAfter, null means append
                                                      targetDocSess,
                                                      translations,  // languages, null means all translations
                                                      false, // do not commit each node
                                                      DocStoreUtilities.COPY_NODE_ID_TRY_KEEP,
                                                      null,  // node-id map not needed
                                                      null,  // use default alias rename strategy (rename should not occur)
                                                      aliasMap,
                                                      null);  // use default content copy strategy

            if (started) targetSess.commitTransaction();
            return cnt;
        } catch (DocException ex) {  // do not catch DocRuntimeException
            if (started) targetSess.rollbackTransaction();
            throw new DocRuntimeException(ex);
        } finally {
            try { if (importDocSess != null) importDocSess.closeSession(); } catch (Exception ex) {
                if (DocmaConstants.DEBUG) ex.printStackTrace();
            }
        }
    }

    public static String[] getImportTranslationLanguages(File importDir)
    {
        DocStoreManagerImpl dsm = new DocStoreManagerImpl(importDir.getAbsolutePath());
        dsm.setVersionIdFactory(new DefaultVersionIdFactory());

        DocStoreSession importSess = null;
        try {
            importSess = dsm.connect("__system_import_");  // dummy user id
            DocVersionId verId = importSess.getLatestVersionId(EXPORTED_NODES_STORE_ID);
            importSess.openDocStore(EXPORTED_NODES_STORE_ID, verId);
            String[] langs = DocStoreUtilities.getTranslationLanguagesRecursive(importSess.getRoot());
            // importSess.closeDocStore();
            return langs;
        } catch (DocException ex) {  // do not catch DocRuntimeException
            throw new DocRuntimeException(ex);
        } finally {
            try { if (importSess != null) importSess.closeSession(); } catch (Exception ex) {
                if (DocmaConstants.DEBUG) ex.printStackTrace();
            }
        }
    }


    /* -------------  private methods  -------------- */


    private static void cleanTempDir(File tempDir)
    {
        if (! tempDir.exists()) return;

        long nowtime = System.currentTimeMillis();
        String[] filenames = tempDir.list();
        for (int i=0; i < filenames.length; i++) {
            String fn = filenames[i];
            String timestr = null;
            if (fn.startsWith(EXPORT_ZIP_PREFIX)) {
                int ext_start = fn.indexOf('.');
                if (ext_start > 0) {
                    timestr = fn.substring(EXPORT_ZIP_PREFIX.length(), ext_start);
                }
            } else
            if (fn.startsWith(EXPORT_DIR_PREFIX)) {
                timestr = fn.substring(EXPORT_DIR_PREFIX.length());
            } else
            if (fn.startsWith(IMPORT_DIR_PREFIX)) {
                timestr = fn.substring(IMPORT_DIR_PREFIX.length());
            }

            if (timestr != null) {
                File del_file = new File(tempDir, fn);
                try {
                    long filetime = Long.parseLong(timestr);
                    if ((nowtime - filetime) > (48*60*60*1000)) {  // delete files older than 48h
                        DocmaUtil.recursiveFileDelete(del_file);
                    }
                } catch (Exception ex) {
                    Log.warning("Could not delete files in temporary directory: " + del_file);
                }
            }
        }

    }

}
