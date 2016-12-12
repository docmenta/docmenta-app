/*
 * PubSectionImpl.java
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

import org.docma.app.DocmaNode;
import org.docma.plugin.DocmaException;
import org.docma.plugin.PubSection;

/**
 *
 * @author MP
 */
public class PubSectionImpl extends GroupImpl implements PubSection
{

    public PubSectionImpl(StoreConnectionImpl store, DocmaNode docNode) 
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

}
