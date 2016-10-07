/*
 * StoreConnectionImpl.java
 * 
 *  Copyright (C) 2016  Manfred Paula, http://www.docmenta.org
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

import java.util.Properties;
import org.docma.app.DocmaAppUtil;
import org.docma.app.DocmaNode;
import org.docma.app.DocmaSession;
import org.docma.coreapi.DocVersionId;
import org.docma.plugin.CharEntity;
import org.docma.plugin.DocmaException;
import org.docma.plugin.Node;
import org.docma.plugin.Style;
import org.docma.plugin.VersionId;
import org.docma.plugin.StoreConnection;
import org.docma.plugin.StoreClosedException;
import org.docma.webapp.EditContentTransformer;

/**
 *
 * @author MP
 */
public class StoreConnectionImpl implements StoreConnection
{
    private String connectionId = null;
    private DocmaSession docmaSess = null;
    private final String storeId;
    private final DocVersionId docVerId;  // the internal version id
    private final VersionId verId;        // the plugin interface wrapper of the version id
    private final boolean isUIConnection;

    StoreConnectionImpl(DocmaSession docmaSess, String storeId, DocVersionId docVerId, boolean isUI)
    {
        this.docmaSess = docmaSess;
        this.storeId = storeId;
        this.docVerId = docVerId;
        this.verId = VersionIdCreator.create(docVerId);
        this.isUIConnection = isUI;
    }
    
    // ********* Interface StoreConnection (visible by plugins) **********

    public String getStoreId() 
    {
        return storeId;
    }

    public VersionId getVersionId() 
    {
        return verId;
    }

    public boolean isClosed() 
    {
        // There are two cases:
        // 1. If this is a temporary store connection, then the connection is
        // closed by the plugin itself by calling closeTempStoreConnection(...)   
        // on the plugin UserSession object or in onSessionClose() if session is
        // closed by user. In this case docmaSess is set to null.
        // 2. If this is a store connection returned by  
        // UserSession.getOpenedStore() then this is the store currently opened
        // in the UI. User can switch to another store in the UI at any time.
        // Therefore, before each method-call that accesses the store, it needs 
        // to be checked whether docmaSess is still connected to the same store 
        // as when this StoreConnection object has been created.

        if (isUIConnection) {
            return (docmaSess == null) || 
                   (! storeId.equals(docmaSess.getStoreId())) || 
                   (! docVerId.equals(docmaSess.getVersionId()));
        } else {
            return (docmaSess == null);
        }
    }

    public Node getNodeById(String id) throws DocmaException
    {
        checkClosed();
        DocmaNode nd;
        try {
            nd = docmaSess.getNodeById(id);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
        return (nd == null) ? null : NodeImpl.createNodeInstance(this, nd);
    }

    public Style[] getStyles() throws DocmaException
    {
        checkClosed();
        try {
            return docmaSess.getStyles();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }
    
    public CharEntity[] getCharEntities() throws DocmaException
    {
        checkClosed();
        try {
            return docmaSess.getCharEntities();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String encodeCharEntities(String text, boolean symbolic) throws DocmaException
    {
        checkClosed();
        try {
            return docmaSess.encodeCharEntities(text, symbolic);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String getTranslationMode()  throws DocmaException
    {
        checkClosed();
        try {
            return docmaSess.getTranslationMode();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void enterTranslationMode(String langCode)  throws DocmaException
    {
        checkClosed();
        try {
            docmaSess.enterTranslationMode(langCode);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void leaveTranslationMode()  throws DocmaException
    {
        checkClosed();
        try {
            docmaSess.leaveTranslationMode();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String getLanguageCode()  throws DocmaException
    {
        checkClosed();
        try {
            return docmaSess.getLanguageCode();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void startTransaction() throws DocmaException 
    {
        checkClosed();
        try {
            docmaSess.startTransaction();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void commitTransaction() throws DocmaException 
    {
        checkClosed();
        try {
            docmaSess.commitTransaction();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void rollbackTransaction() 
    {
        checkClosed();
        docmaSess.rollbackTransaction();
    }

    public boolean runningTransaction() 
    {
        checkClosed();
        return docmaSess.runningTransaction();
    }

    public boolean isValidAlias(String name) 
    {
        return DocmaAppUtil.isValidAlias(name);
    }
    
    public String prepareXHTML(String content, Properties props) throws DocmaException
    {
        try {
            return EditContentTransformer.prepareContentForSave(docmaSess, content, props);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    //
    // ********* Other methods (not directly visible by plugins) **********
    //

    private void checkClosed()
    {
        if (isClosed()) { 
            throw new StoreClosedException("The connection to store " + storeId + "/" + verId + " is closed.");
        }
    }
    
    String getConnectionId()
    {
        if (connectionId == null) {
            connectionId = "scon" + hashCode() + "_" + storeId + "_" + verId;
        }
        return connectionId;
    }
    
    void setConnectionId(String connId)
    {
        if (connectionId != null) {
            throw new RuntimeException("Cannot overwrite store connection ID.");
        }
        this.connectionId = connId;
    }
    
    DocVersionId getDocVersionId()
    {
        return docVerId;
    }

    void close() 
    {
        docmaSess.closeSession();
        docmaSess = null;
    }

}
