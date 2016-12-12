/*
 * ContentAnchorImpl.java
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
import org.docma.plugin.Content;
import org.docma.plugin.ContentAnchor;

/**
 *
 * @author MP
 */
public class ContentAnchorImpl implements ContentAnchor
{
    private final Content owner;
    private final DocmaAnchor docmaAnchor;

    public ContentAnchorImpl(Content owner, DocmaAnchor docmaAnchor) 
    {
        this.owner = owner;
        this.docmaAnchor = docmaAnchor;
    }

    //************************************************************
    //**************    Interface methods       ******************  
    //************************************************************

    public String getAlias() 
    {
        return docmaAnchor.getAlias();
    }

    public String getTitle() 
    {
        return docmaAnchor.getTitle();
    }

    public int getTagPosition() 
    {
        return docmaAnchor.getTagPosition();
    }

    public Content getNode() 
    {
        return owner;
    }
    
}
