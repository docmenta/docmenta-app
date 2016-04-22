/*
 * DocFileImpl.java
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
 * 
 * Created on 03. April 2011, 15:03
 */

package org.docma.coreapi.fsimplementation;

import java.io.*;
import org.w3c.dom.*;

import org.docma.coreapi.*;
import org.docma.util.Log;

/**
 *
 * @author MP
 */
public class DocFileImpl extends DocContentImpl implements DocFile
{
    
    /** Creates a new instance of DocFileImpl */
    public DocFileImpl(DocStoreSessionImpl docSess, int node_id)
    {
        super(docSess, node_id);
        getDOMElement().setAttribute(XMLConstants.ATTR_CONTENT_CLASS, XMLConstants.CONTENT_CLASS_FILE);
    }

    public DocFileImpl(DocStoreSessionImpl docSess, Element existingDOM)
    {
        super(docSess, existingDOM);
    }

    public String getContentPath(String lang) {
        if (lang == null) {
            return "content" + File.separatorChar + "files" + File.separatorChar + docFileName();
        } else {
            return "translations" + File.separatorChar + lang + File.separatorChar + "files" + File.separatorChar + docFileName();
        }
    }

    private String docFileName()
    {
        String idstr = IdRegistry.idStringToDOMString(getId());
        String fname = getFileName();
        if ((fname != null) && (fname.trim().length() > 0)) {
            return idstr + "_" + fname;
        } else {
            return idstr;
        }
    }


    /* --------------  Interface DocFile ---------------------- */

    public String getFileName()
    {
        String ext = getFileExtension();
        if ((ext == null) || (ext.length() == 0)) {
            return getTitle();
        } else {
            return getTitle() + "." + ext;
        }
    }
    
    public void setFileName(String filename)
    {
        String ext_new = "";
        String name_new = filename;
        int p = filename.lastIndexOf('.');
        if (p >= 0) { 
            ext_new = filename.substring(p + 1);
            name_new = filename.substring(0, p);
        }

        String lang = getDocSession().getTranslationMode();
        File path_old = getContentFileAbsolute(lang);
        String name_old = getTitle();
        String ext_old = getFileExtension();

        // Set new metadata
        super.setTitle(name_new);
        super.setFileExtension(ext_new);

        // Rename content file
        File path_new = getContentFileAbsolute(lang);
        if (! path_new.equals(path_old)) {
            if (path_old.exists() && !path_old.renameTo(path_new)) {
                super.setTitle(name_old);  // undo metadata change
                super.setFileExtension(ext_old);  // undo metadata change
                throw new DocRuntimeException("File rename failed: " + path_old + " to " + path_new);
            }
        }
    }
    
    public void setFileExtension(String file_extension)
    {
        if (file_extension == null) file_extension = ""; 
        String fname_new = getTitle() + "." + file_extension;
        setFileName(fname_new);  // Set new metadata and rename content file
    }
    
    public void setTitle(String new_title)
    {
        String ext = getFileExtension();
        if (ext == null) ext = "";         
        setFileName(new_title + "." + ext);  // Set new metadata and rename content file
    }

}
