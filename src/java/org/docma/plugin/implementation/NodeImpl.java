/*
 * NodeImpl.java
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

import java.util.Date;
import org.docma.app.DocmaNode;
import org.docma.app.DocmaSession;
import org.docma.plugin.DocmaException;
import org.docma.plugin.Lock;
import org.docma.plugin.Node;
import org.docma.plugin.StoreConnection;
import org.docma.webapp.GUIConstants;


/**
 *
 * @author MP
 */
public abstract class NodeImpl implements Node
{
    protected final StoreConnectionImpl store;
    protected final DocmaNode docNode;
    
    NodeImpl(StoreConnectionImpl store, DocmaNode docNode) 
    {
        this.store = store;
        this.docNode = docNode;
    }
    
    static NodeImpl createNodeInstance(StoreConnectionImpl store, DocmaNode docNode)
    {
        if (docNode.isImageContent()) {
            return new ImageFileImpl(store, docNode);
        } else if (docNode.isFileContent()) {
            return new FileContentImpl(store, docNode);
        } else if (docNode.isHTMLContent()) {
            return new PubContentImpl(store, docNode);
        } else if (docNode.isFolder()) {
            return new FolderImpl(store, docNode);
        } else if (docNode.isSection()) {
            return new PubSectionImpl(store, docNode);
        } else if (docNode.isReference()) {
            return new ReferenceImpl(store, docNode);
        } else {
            throw new DocmaException("Unknown node type.");
        }
    }
    
//    DocmaSession getDocmaSession()
//    {
//        return docNode.getDocmaSession();
//    }

    //************************************************************
    //**************    Attribute methods       ******************  
    //************************************************************
    
    public String getId() throws DocmaException
    {
        return docNode.getId();
    }

    public String getAttribute(String name) throws DocmaException 
    {
        try {
            return docNode.getCustomAttribute(name);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String getAttribute(String name, String lang_code) throws DocmaException
    {
        try {
            return docNode.getCustomAttribute(name, lang_code);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String getAttributeEntityEncoded(String name) throws DocmaException 
    {
        try {
            return docNode.getCustomAttributeEntityEncoded(name);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String[] getAttributeNames() throws DocmaException
    {
        try {
            return docNode.getCustomAttributeNames();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void setAttribute(String name, String value) throws DocmaException 
    {
        try {
            docNode.setCustomAttribute(name, value);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String getAlias() throws DocmaException 
    {
        try {
            return docNode.getAlias();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String getLinkName() throws DocmaException 
    {
        try {
            return docNode.getLinkAlias();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void setAlias(String name) throws DocmaException 
    {
        try {
            docNode.setAlias(name);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public Date getLastModifiedDate() throws DocmaException 
    {
        try {
            return docNode.getLastModifiedDate();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String getLastModifiedBy() throws DocmaException 
    {
        try {
            return docNode.getLastModifiedBy();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String getWorkflowStatus() throws DocmaException 
    {
        try {
            String ws = docNode.getWorkflowStatus();
            return ((ws != null) && ws.equals("")) ? null : ws;
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void setWorkflowStatus(String status) throws DocmaException 
    {
        setWorkflowStatus(status, true);
    }

    public void setWorkflowStatus(String status, boolean updateParent) throws DocmaException 
    {
        try {
            docNode.setWorkflowStatus(status, updateParent);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String getApplicability() throws DocmaException 
    {
        try {
            return docNode.getApplicability();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void setApplicability(String expression) throws DocmaException 
    {
        try {
            docNode.setApplicability(expression);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public int getProgress() 
    {
        try {
            return docNode.getProgress();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void setProgress(int percent) 
    {
        setProgress(percent, true);
    }

    public void setProgress(int percent, boolean updateParent) throws DocmaException 
    {
        // Note: this check could also be moved into DocmaNode.setProgress(...) 
        if ((percent < -1) || (percent > 100)) {
            throw new DocmaException("Invalid percent value: " + percent);
        }
        
        try {
            docNode.setProgress(percent, updateParent);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }
    
    //************************************************************
    //**************    Translation methods     ******************  
    //************************************************************
    
    public String getTranslationMode() throws DocmaException
    {
        try {
            return docNode.getTranslationMode();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public boolean isTranslationMode() throws DocmaException 
    {
        try {
            return docNode.isTranslationMode();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public boolean isTranslated() throws DocmaException 
    {
        try {
            return docNode.isTranslated();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public boolean isTranslated(String lang_code) throws DocmaException
    {
        try {
            return docNode.isTranslated(lang_code);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }
            
    public String[] listTranslations() throws DocmaException 
    {
        try {
            return docNode.listTranslations();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void deleteTranslation() throws DocmaException 
    {
        try {
            docNode.deleteTranslation();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }
    
    //************************************************************
    //**************    Lock methods            ******************  
    //************************************************************
    
    public Lock getLock() throws DocmaException
    {
        try {
            org.docma.lockapi.Lock doc_lock = docNode.getLock();
            return (doc_lock == null) ? null : new LockImpl(doc_lock);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public boolean setLock() throws DocmaException
    {
        try {
            return docNode.setLock(GUIConstants.LOCK_TIMEOUT);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public boolean refreshLock() throws DocmaException
    {
        try {
            return docNode.refreshLock(GUIConstants.LOCK_TIMEOUT);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }
    
    public Lock removeLock() throws DocmaException
    {
        try {
            org.docma.lockapi.Lock doc_lock = docNode.removeLock();
            return (doc_lock == null) ? null : new LockImpl(doc_lock);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    //************************************************************
    //**************    Other methods           ******************  
    //************************************************************

    public void invalidateCache() 
    {
        docNode.refresh();
    }

    public boolean hasAncestor(String node_id) throws DocmaException 
    {
        try {
            return docNode.hasAncestor(node_id);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public Node getParent() throws DocmaException 
    {
        try {
            DocmaNode par = docNode.getParent();
            return (par == null) ? null : createNodeInstance(store, par);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void delete() throws DocmaException 
    {
        try {
            docNode.delete();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public StoreConnection getStoreConnection() 
    {
        return store;
    }
    
    @Override
    public boolean equals(Object obj) 
    {
        if ((obj != null) && (obj instanceof Node)) {
            Node other = (Node) obj;
            return (store == other.getStoreConnection()) && getId().equals(other.getId());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() 
    {
        return getId().hashCode();
    }
    
}
