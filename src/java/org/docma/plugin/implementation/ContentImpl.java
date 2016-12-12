/*
 * ContentImpl.java
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

import java.io.InputStream;
import org.docma.app.DocmaNode;
import org.docma.coreapi.DocContentRevision;
import org.docma.plugin.Content;
import org.docma.plugin.ContentRevision;
import org.docma.plugin.DocmaException;

/**
 *
 * @author MP
 */
public abstract class ContentImpl extends NodeImpl implements Content
{

    public ContentImpl(StoreConnectionImpl store, DocmaNode docNode) 
    {
        super(store, docNode);
    }

    //************************************************************
    //**************    Interface methods       ******************  
    //************************************************************

    public byte[] getContent() throws DocmaException
    {
        try {
            return docNode.getContent();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String getContentString() throws DocmaException 
    {
        try {
            return docNode.getContentString();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void setContentString(String content) throws DocmaException
    {
        try {
            docNode.setContentString(content);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void setContent(byte[] content) throws DocmaException 
    {
        try {
            docNode.setContent(content);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void setContentStream(InputStream content) throws DocmaException 
    {
        try {
            docNode.setContentStream(content);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void clearContent() throws DocmaException 
    {
        try {
            docNode.deleteContent();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public long getContentLength() throws DocmaException 
    {
        try {
            return docNode.getContentLength();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String getContentType() throws DocmaException 
    {
        try {
            String ct = docNode.getContentType();
            return ((ct != null) && ct.equals("")) ? null : ct;
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public abstract String getCharset() throws DocmaException;

    public boolean hasContent(String lang_code) throws DocmaException 
    {
        try {
            return docNode.hasContent(lang_code);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void checkUpdateContentAllowed() throws DocmaException
    {
        try {
            docNode.checkUpdateContentAllowed();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public boolean makeRevision() 
    {
        try {
            return docNode.makeRevision();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public ContentRevision[] getRevisions() throws DocmaException 
    {
        try {
            DocContentRevision[] docrevs = docNode.getRevisions();
            if (docrevs == null) {
                return new ContentRevision[0];
            }
            ContentRevision[] revs = new ContentRevision[docrevs.length];
            for (int i = 0; i < revs.length; i++) {
                revs[i] = new ContentRevisionImpl(docrevs[i]);
            }
            return revs;
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }
    
}
