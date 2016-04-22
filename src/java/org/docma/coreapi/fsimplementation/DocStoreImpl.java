/*
 * DocStoreImpl.java
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
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

import org.docma.coreapi.*;
import org.docma.coreapi.implementation.*;
import org.docma.lockapi.fsimplementation.LockManagerImpl;
import org.docma.util.Log;
import org.docma.util.DocmaUtil;


/**
 *
 * @author MP
 */
public class DocStoreImpl extends AbstractDocStore
{
    private static final String DOCMA_DTD_FILENAME = "docma.dtd";
    
    // private static Logger logger = Logger.getLogger("org.docma");
    
    private static DocumentBuilderFactory domFactory = null;
    private static DocumentBuilder domBuilder = null;
    private static Transformer xmlTransformer = null;

    private boolean running_transaction = false;
    private boolean construction_phase = true;
    
    private File baseDir;

    private File contentDir;
    private File translationsDir;
    private File locksDir;
    private File indexFile;
    private File indexBakFile;
    private File indexTmpFile;
    private File dtdFile;
    private Document indexDoc;
    private IdRegistry idRegistry;
    private long lastOpenedTime;


    /** Creates a new instance of DocStoreImpl */
    DocStoreImpl(File baseDir, File docmaDTDFile, String storeId, DocVersionId verId) 
    {
        super(storeId, verId);
        // if (! (storesDir.exists() && storesDir.isDirectory())) {
        //     throw new DocRuntimeException("Directory " + storesDir + " does not exist.");
        // }

        construction_phase = true;
        initOnce();
        
        this.baseDir = baseDir;

        contentDir      = new File(baseDir, "content");
        translationsDir = new File(baseDir, "translations");
        locksDir        = new File(baseDir, "locks");
        indexFile       = new File(baseDir, "index.xml");
        indexBakFile    = new File(baseDir, "index.bak");
        indexTmpFile    = new File(baseDir, "index.tmp");
        dtdFile         = new File(baseDir, DOCMA_DTD_FILENAME);
        
        if (! (baseDir.exists() && baseDir.isDirectory())) {
            throw new DocRuntimeException("Directory " + baseDir + " does not exist.");
        }
        if (! contentDir.exists())      contentDir.mkdir(); 
        if (! translationsDir.exists()) translationsDir.mkdir(); 
        if (! locksDir.exists()) locksDir.mkdir();

        setLockManager(new LockManagerImpl(locksDir.getAbsolutePath()));
        idRegistry = new IdRegistry(storeId);

        if (! dtdFile.exists()) {
            DocmaUtil.fileCopy(docmaDTDFile, dtdFile, false);
        }

        indexDoc = null;
        if (indexFile.exists() || indexBakFile.exists()) openIndexFile();
        else createIndexFile();

        construction_phase = false;
    }

    /* ----------------  Private methods  --------------------- */

    private synchronized void initOnce() {
        if (domFactory == null) {
            try {
                domFactory = DocumentBuilderFactory.newInstance();
                domBuilder = domFactory.newDocumentBuilder();
                xmlTransformer = TransformerFactory.newInstance().newTransformer();
            } catch (Exception ex) { throw new DocRuntimeException(ex); }
        }
    }
    
    private void createIndexFile() {
        try {
            indexDoc = null;
            indexDoc = domBuilder.newDocument();
            Element root = DocGroupImpl.createGroupDOM(indexDoc, idRegistry.newId());
            indexDoc.appendChild(root);

            xmlTransformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, DOCMA_DTD_FILENAME);
            xmlTransformer.transform(new DOMSource(indexDoc), new StreamResult(indexFile));
        } catch (Exception ex) { throw new DocRuntimeException(ex); }
    }
    
    private synchronized void openIndexFile() {
        try {
            indexDoc = null;

            if ((! indexFile.exists()) && indexBakFile.exists()) {
                indexBakFile.renameTo(indexFile);  // restore backup
            }

            indexDoc = domBuilder.parse(indexFile);
            lastOpenedTime = System.currentTimeMillis();
            registerIds();
        } catch (Exception ex) { throw new DocRuntimeException(ex); }
    }
    
    private void registerIds() {
        NodeList list = indexDoc.getElementsByTagName("*");
        for (int i=0; i < list.getLength(); i++) {
            Node child = list.item(i);
            if (child instanceof Element) {
                Element childElem = (Element) child;
                String id = childElem.getAttribute(XMLConstants.ATTR_ID);
                if ((id != null) && (id.length() > 0)) {
                    try {
                        idRegistry.registerDOMId(id);
                    } catch (Exception ex) {}
                }
            }
        }
        Log.info("current max id: " + idRegistry.currentMaxId());
    }
        
    private void saveNow() {
        if (! construction_phase) {  // ignore save requests while constructing tree recursively 
            try {
                if (DocConstants.DEBUG) Log.info("Saving index file.");
                if (indexDoc == null) {
                    throw new DocRuntimeException("Cannot save index file: Document not loaded.");
                }

                // Create backup of index file
                if (indexBakFile.exists()) indexBakFile.delete();  // delete old backup
                if (indexFile.exists()) {
                    if (! indexFile.renameTo(indexBakFile)) {
                        throw new DocRuntimeException("Cannot save index file: file rename to '" +
                                                      indexBakFile + "' failed");
                    }
                }

                // Write new index file
                if (indexTmpFile.exists()) indexTmpFile.delete();
                xmlTransformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, DOCMA_DTD_FILENAME);
                xmlTransformer.transform(new DOMSource(indexDoc), new StreamResult(indexTmpFile));

                if (! indexTmpFile.renameTo(indexFile)) {
                    throw new DocRuntimeException("Save failed: cannot rename temporary file to " + indexFile);
                }
            } catch (Exception ex) { throw new DocRuntimeException(ex); }
        }
    }


    /* ----------------  Package local methods  --------------------- */

    synchronized void saveOnCommit() {
        if (! runningTransaction()) {
            saveNow();
        }
    }
    
    DocumentBuilder getDOMBuilder() {
        return domBuilder;
    }
    
    Transformer getXMLTransformer() {
        return xmlTransformer;
    }
    
    Document getIndexDocument() {
        return indexDoc;
    }

    long getLastOpenedTime() {
        return lastOpenedTime;
    }
    
    IdRegistry getIdRegistry() {
        return idRegistry;
    }

    /* ----------------  Override methods  --------------------- */
    
    public synchronized void dispatchEventQueue()
    {
        if (! running_transaction) {
            super.dispatchEventQueue();
        }
    }

    /* ----------------  Public methods  --------------------- */

    public String getDirectory() {
        return baseDir.getAbsolutePath();
    }

    // public String getId() {
    //     return storeId;
    // }

    public synchronized void startTransaction() throws DocException {
        // quick and dirty implementation (this is no real transaction):
        if (running_transaction) throw new DocException("Transaction conflict."); 
        running_transaction = true;
    }

    public synchronized void commitTransaction() throws DocException {
        // quick and dirty implementation (this is no real transaction):
        if (running_transaction) {
            saveNow();
            running_transaction = false;
            dispatchEventQueue();
        }
    }

    public synchronized void rollbackTransaction() {
        // quick and dirty implementation (this is no real transaction):
        if (running_transaction) { 
            openIndexFile();  // reread file -> discard unsaved changes
            running_transaction = false;
            discardEventQueue();
        }
    }

    public synchronized boolean runningTransaction() {
        return running_transaction;
    }
    
}
