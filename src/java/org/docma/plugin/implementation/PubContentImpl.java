/*
 * PubContentImpl.java
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

import org.docma.app.DocmaAnchor;
import org.docma.app.DocmaNode;
import org.docma.plugin.ContentAnchor;
import org.docma.plugin.DocmaException;
import org.docma.plugin.PubContent;

/**
 *
 * @author MP
 */
public class PubContentImpl extends ContentImpl implements PubContent
{

    public PubContentImpl(StoreConnectionImpl store, DocmaNode docNode) 
    {
        super(store, docNode);
    }

    //************************************************************
    //**************    Interface methods       ******************  
    //************************************************************
    
    public String getTitle() throws DocmaException
    {
        try {
            return docNode.getTitle();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String getTitle(String lang_code) throws DocmaException 
    {
        try {
            return docNode.getTitle(lang_code);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }
    
    public String getTitleEntityEncoded() throws DocmaException
    {
        try {
            return docNode.getTitleEntityEncoded();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void setTitle(String value) throws DocmaException 
    {
        try {
            docNode.setTitle(value);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public ContentAnchor[] getContentAnchors() throws DocmaException 
    {
        try {
            DocmaAnchor[] arr = docNode.getContentAnchors();
            if (arr == null) {
                return new ContentAnchor[0];
            } else {
                ContentAnchor[] res = new ContentAnchor[arr.length];
                for (int i = 0; i < res.length; i++) {
                    res[i] = new ContentAnchorImpl(this, arr[i]);
                }
                return res;
            }
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public boolean hasContentAnchors() throws DocmaException 
    {
        try {
            return docNode.hasContentAnchor();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public ContentAnchor getContentAnchor(String anchorId) throws DocmaException 
    {
        try {
            for (DocmaAnchor anch : docNode.getContentAnchors()) {
                if (anchorId.equals(anch.getAlias())) {
                    return new ContentAnchorImpl(this, anch);
                }
            }
            return null;
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    @Override
    public String getCharset() throws DocmaException 
    {
        return "UTF-8";
    }

}
