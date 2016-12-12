/*
 * FolderImpl.java
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
import org.docma.plugin.Folder;
import org.docma.plugin.FolderType;

/**
 *
 * @author MP
 */
public class FolderImpl extends GroupImpl implements Folder
{

    public FolderImpl(StoreConnectionImpl store, DocmaNode docNode) 
    {
        super(store, docNode);
    }

    //************************************************************
    //**************    Interface methods       ******************  
    //************************************************************
    
    public String getName() throws DocmaException 
    {
        try {
            return docNode.getTitle();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String getName(String lang_code) throws DocmaException 
    {
        try {
            return docNode.getTitle(lang_code);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }
    
    public void setName(String value) throws DocmaException 
    {
        try {
            docNode.setTitle(value);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public FolderType getFolderType() throws DocmaException 
    {
        try {
            if (docNode.isImageFolder()) {
                return FolderType.IMAGE;
            } else {
                return FolderType.GENERAL;
            }
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void setFolderType(FolderType folder_type) throws DocmaException 
    {
        try {
            if (FolderType.IMAGE.equals(folder_type)) {
                docNode.setFolderTypeImage();
            } else {
                docNode.setFolderTypeFile();
            }
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

}
