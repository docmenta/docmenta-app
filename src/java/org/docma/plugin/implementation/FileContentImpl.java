/*
 * FileContentImpl.java
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
import org.docma.plugin.FileContent;

/**
 *
 * @author MP
 */
public class FileContentImpl extends ContentImpl implements FileContent
{

    public FileContentImpl(StoreConnectionImpl store, DocmaNode docNode) 
    {
        super(store, docNode);
    }

    //************************************************************
    //**************    Interface methods       ******************  
    //************************************************************

    public String getFileExtension() throws DocmaException 
    {
        try {
            return docNode.getFileExtension();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void setFileExtension(String ext) throws DocmaException 
    {
        try {
            docNode.setFileExtension(ext);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String getFileName() throws DocmaException 
    {
        try {
            return docNode.getDefaultFileName();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void setFileName(String filename) throws DocmaException 
    {
        try {
            docNode.setFileName(filename);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public boolean isTextFile() throws DocmaException 
    {
        try {
            return docNode.hasTextFileExtension();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    @Override
    public String getCharset() throws DocmaException 
    {
        try {
            String cs = docNode.getFileCharset();
            return (cs == null) ? "UTF-8" : cs;
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }
    
    public void setCharset(String charsetName) throws DocmaException 
    {
        try {
            docNode.setFileCharset(charsetName);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

}
