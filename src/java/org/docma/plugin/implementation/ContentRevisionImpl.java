/*
 * ContentRevisionImpl.java
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
import java.util.Date;
import org.docma.coreapi.DocContentRevision;
import org.docma.plugin.ContentRevision;
import org.docma.plugin.DocmaException;

/**
 *
 * @author MP
 */
public class ContentRevisionImpl implements ContentRevision
{
    private final DocContentRevision backendRevision; 

    ContentRevisionImpl(DocContentRevision docRevision) 
    {
        this.backendRevision = docRevision;
    }

    //************************************************************
    //**************    Interface methods       ******************  
    //************************************************************
    
    public Date getDate()  throws DocmaException
    {
        try {
            return backendRevision.getDate();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String getUserId()  throws DocmaException
    {
        try {
            return backendRevision.getUserId();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public byte[] getContent()  throws DocmaException
    {
        try {
            return backendRevision.getContent();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public InputStream getContentStream()  throws DocmaException
    {
        try {
            return backendRevision.getContentStream();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String getContentString()  throws DocmaException
    {
        try {
            return backendRevision.getContentString();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String getContentString(String charsetName)  throws DocmaException
    {
        try {
            return backendRevision.getContentString(charsetName);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public long getContentLength()  throws DocmaException
    {
        try {
            return backendRevision.getContentLength();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }
    
}
