/*
 * MainApp.java
 *
 * Created on 13. Oktober 2007, 22:34
 *
 */

package org.docma.apptest;

import java.util.logging.*;
import java.io.*;

import org.docma.coreapi.*;
import org.docma.coreapi.fsimplementation.*;
import org.docma.userapi.fsimplementation.*;
import org.docma.app.DefaultVersionIdFactory;
import org.docma.util.DocmaUtil;


/**
 *
 * @author MP
 */
public class MainApp implements Runnable
{
    private static Logger logger = Logger.getLogger("org.docma");
    private final static String BASE_DIR = "C:\\work\\docma_test_stores";
    private final static String DTD_PATH = "C:\\work\\docma_test_stores\\docma.dtd";
    
    private static DocStoreManager dsm = null;
    
    private String userId;
    private String storeId;
    private String versionId;

    MainApp(String userId, String storeId, String verId)
    {
        this.userId = userId;
        this.storeId = storeId;
        this.versionId = verId;
    }
    
    private static void init() throws Exception {
        if (dsm == null) {
            logger.setLevel(Level.FINEST);
            logger.setUseParentHandlers(false);
            Handler handler = new ConsoleHandler();
            handler.setLevel(Level.FINEST);
            logger.addHandler(handler);

            // String basepath = Configurator.getProp("basedir");
            
            DocStoreManagerImpl dsm_impl = new DocStoreManagerImpl();
            dsm_impl.setBaseDirectory(BASE_DIR);   // basepath
            dsm_impl.setStoreDTDFile(DTD_PATH);
            dsm_impl.setVersionIdFactory(new DefaultVersionIdFactory());
            // dsm_impl.setUserManager(new FiledUsers());
        
            dsm = dsm_impl;
        }
    }

    
    public static synchronized DocStoreManager getDocStoreManager() throws Exception 
    {
        if (dsm == null) init();
        return dsm;
    }
    
    
    public static void main(String[] args) throws Exception 
    {
        final String STORE_ID_RWTEST = "store_readwrite_test";
        
        DocStoreManager mydsm = getDocStoreManager();
        DocStoreSession docsess = mydsm.connect("mustermann");

        File stores_dir = new File(BASE_DIR, "docstores");
        File store_dir = new File(stores_dir, STORE_ID_RWTEST);
        if (store_dir.exists() && !DocmaUtil.recursiveFileDelete(store_dir)) {
            throw new DocException("Could not delete store directory: " + store_dir);
        }

        System.out.println("Creating store...");
        docsess.createDocStore(STORE_ID_RWTEST, null, null);
        DocVersionId verId = docsess.createVersionId("1.0");
        System.out.println("Creating version...");
        docsess.createVersion(STORE_ID_RWTEST, null, verId);
        
        System.out.println("Creating nodes...");
        docsess.openDocStore(STORE_ID_RWTEST, verId);
        docsess.startTransaction();
        DocGroup newgroup = docsess.createGroup();
        docsess.getRoot().appendChild(newgroup);
        for (int i=1; i <= 20; i++) {
            DocXML node = docsess.createXML();
            newgroup.appendChild(node);
            node.setTitle("MyNode Title " + i);
            node.setAttribute("testatt", "attval " + i);
            node.setContentString("Test content " + i);
        }
        docsess.commitTransaction();
        docsess.closeDocStore();
        docsess.closeSession();

        Thread thread1 = new Thread(new MainApp("user1", STORE_ID_RWTEST, "1.0"));
        Thread thread2 = new Thread(new MainApp("user2", STORE_ID_RWTEST, "1.0"));
        thread1.start();
        thread2.start();
        
        // DocVersionId verId = docsess.getLatestVersionId("mydocstore");
        // docsess.openDocStore("mydocstore", verId);
        
        // docsess.startTransaction();
        // printGroup(docsess.getRoot(), "");
        // DocGroup newgroup = docsess.createGroup();
        // docsess.getRoot().appendChild(newgroup);
        
        // System.out.println(docstore.getRoot().getTitle());
        // DocGroup g = (DocGroup) docsess.getRoot().getChildNodes()[0];
        // g.setTitle("Ein Test Hallo!");
        // System.out.println(g.getTitle());
        
        // System.out.println("test 123");
        // docsess.commitTransaction();
    }
    
    
    private static void printGroup(DocGroup group, String indent) throws DocException {
        System.out.println(indent + "Gruppe, Titel: " + group.getTitle());
        DocNode[] children = group.getChildNodes();
        for (int i=0; i < children.length; i++) {
            printGroup((DocGroup) children[i], indent + "  ");
        }
    }

    public void run() 
    {
        try {
            DocStoreManager mydsm = getDocStoreManager();
            DocStoreSession docsess = mydsm.connect(this.userId);
            DocVersionId verId = docsess.createVersionId(this.versionId);
            startReadWriteTest(docsess, this.storeId, verId);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void startReadWriteTest(DocStoreSession docsess, String storeId, DocVersionId verId) throws Exception
    {
        System.out.println("Opening version for user " + userId);
        docsess.openDocStore(storeId, verId);
        
        DocGroup root = docsess.getRoot();
        // System.out.println("Root docstore: " + ((DocGroupImpl) root).docStore.toString());
        DocGroup childgrp = (DocGroup) root.getChildNodes()[0];
        System.out.println("Getting child nodes for user " + userId);
        DocNode[] nodearr = childgrp.getChildNodes();
        // System.out.println("Getting child nodes for user " + userId + " finished");
        for (int i=1; i <= 100; i++) {
            if ((i % 10) == 1) System.out.println("Read/Write count user " + userId + ": " + i); 
            
            int idx = (int) Math.floor(Math.random() * nodearr.length); 
            // if (idx >= nodearr.length) idx = nodearr.length - 1;
            DocNode nd = nodearr[idx];
            // long now = System.currentTimeMillis();
            // docsess.startTransaction();
            nd.getTitle();
            nd.getAttribute("testatt");
            // docsess.commitTransaction();
            // nd.setTitle("New title " + now);
            // nd.setAttribute("testatt", "New value " + now);
        }
        
        System.out.println("Closing session of user " + userId);
        docsess.closeDocStore();
        docsess.closeSession();
        System.out.println("Finished: " + userId);
    }

}
