/*
 * NodeImpl.java
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

import java.util.Properties;
import org.docma.app.*;
import org.docma.webapp.GUIConstants;
import org.docma.webapp.EditContentTransformer;
import org.docma.plugin.Lock;
import org.docma.plugin.Node;


/**
 *
 * @author MP
 */
public class NodeImpl implements Node
{
    private final DocmaNode docNode;
    
    NodeImpl(DocmaNode docNode) 
    {
        this.docNode = docNode;
    }

    public String getId() 
    {
        return docNode.getId();
    }
    
    public Lock getLock() 
    {
        org.docma.lockapi.Lock doc_lock = docNode.getLock();
        return (doc_lock == null) ? null : new LockImpl(doc_lock);
    }

    public boolean setLock() 
    {
        return docNode.setLock(GUIConstants.LOCK_TIMEOUT);
    }

    public boolean refreshLock() 
    {
        return docNode.refreshLock(GUIConstants.LOCK_TIMEOUT);
    }
    
    public Lock removeLock()
    {
        org.docma.lockapi.Lock doc_lock = docNode.removeLock();
        return (doc_lock == null) ? null : new LockImpl(doc_lock);
    }

    public String getTitle() 
    {
        return docNode.getTitle();
    }

    public String getTitleEntityEncoded() 
    {
        return docNode.getTitleEntityEncoded();
    }

    public String getTranslationMode() 
    {
        return docNode.getTranslationMode();
    }

    public byte[] getContent() 
    {
        return docNode.getContent();
    }

    public String getContentString() 
    {
        return docNode.getContentString();
    }

    public void setContentString(String content) throws Exception
    {
        docNode.setContentString(content);
    }

    public String prepareXHTML(String content, Properties props) throws Exception
    {
        return EditContentTransformer.prepareContentForSave(getDocmaSession(), content, props);
    }

    public boolean makeRevision() 
    {
        return docNode.makeRevision();
    }

    public int getProgress() 
    {
        return docNode.getProgress();
    }

    public void setProgress(int value) 
    {
        docNode.setProgress(value);
    }

    // ------------ Private Methods ------------------
    
    private DocmaSession getDocmaSession()
    {
        return docNode.getDocmaSession();
    }
}
