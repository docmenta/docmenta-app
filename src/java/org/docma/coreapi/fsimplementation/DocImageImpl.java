/*
 * DocImageImpl.java
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
 * Created on 17. Oktober 2007, 15:03
 *
 */

package org.docma.coreapi.fsimplementation;

import java.io.*;
import org.w3c.dom.*;

import org.docma.coreapi.*;
import org.docma.coreapi.implementation.*;
import org.docma.util.Log;

/**
 *
 * @author MP
 */
public class DocImageImpl extends DocContentImpl implements DocImage
{
    
    /** Creates a new instance of DocImageImpl */
    public DocImageImpl(DocStoreSessionImpl docSess, int node_id)
    {
        super(docSess, node_id);
        getDOMElement().setAttribute(XMLConstants.ATTR_CONTENT_CLASS, XMLConstants.CONTENT_CLASS_IMAGE);
    }

    public DocImageImpl(DocStoreSessionImpl docSess, Element existingDOM)
    {
        super(docSess, existingDOM);
    }

    public String getContentPath(String lang) {
        if (lang == null) {
            return "content" + File.separatorChar + "images" + File.separatorChar + imageFileName();
        } else {
            return "translations" + File.separatorChar + lang + File.separatorChar + "images" + File.separatorChar + imageFileName();
        }
    }

    private String imageFileName()
    {
        String idstr = IdRegistry.idStringToDOMString(getId());
        String ext = getFileExtension();
        if ((ext == null) || (ext.length() == 0)) {
            return idstr + ".image";
        } else {
            return idstr + "." + ext;
        }
    }

    private String getRenditionBasePath(String lang)
    {
        if (lang == null) {
            return "content" + File.separatorChar + "images" + File.separatorChar +
                   "_renditions";
        } else {
            return "translations" + File.separatorChar + lang + File.separatorChar + "images" + File.separatorChar +
                   "_renditions";
        }
    }

    private String getRenditionPath(DocImageRendition rendition, String lang)
    {
        return getRenditionBasePath(lang) + File.separatorChar + rendition.getName() +
               File.separatorChar + renditionFileName(rendition);
    }

    private String renditionFileName(DocImageRendition rendition)
    {
        return IdRegistry.idStringToDOMString(getId()) + "_" + rendition.getMaxWidth() +
                "_" + rendition.getMaxHeight() + "." + rendition.getFormat();
    }

    private File getRenditionFileAbsolute(DocImageRendition rendition, String lang)
    {
        return new File(getDocStore().getDirectory(), getRenditionPath(rendition, lang));
    }
    
    private void deleteRenditions(String lang)
    {
        File base = new File(getDocStore().getDirectory(), getRenditionBasePath(lang));
        if (base.exists()) {
            File[] rends = base.listFiles();
            String search_prefix = IdRegistry.idStringToDOMString(getId()) + "_";
            for (int i=0; i < rends.length; i++) {
                File rendDir = rends[i];
                File[] rendFiles = rendDir.listFiles();
                for (int j=0; j < rendFiles.length; j++) {
                    File f = rendFiles[j];
                    if (f.getName().startsWith(search_prefix)) {
                        f.delete();
                    }
                }
            }
        }
    }

    private File createRendition(DocImageRendition rendition, String lang) throws Exception
    {
        File fout = getRenditionFileAbsolute(rendition, lang);
        if (! fout.exists()) {
            makeParentDirs(fout);
            File fin = getContentFileAbsolute(lang);
            ImageHelper.createRendition(fout, fin, rendition);
        }
        return fout;
    }


    private String getContentLang()
    {
        String lang = getDocSession().getTranslationMode();
        File f = getContentFileAbsolute(lang);
        if ((lang != null) && hasTranslation(lang) && f.exists()) return lang;
        else return null;
    }

    /* --------------  Interface DocImage ---------------------- */

    public void setFileExtension(String file_extension)
    {
        String lang = getDocSession().getTranslationMode();
        File f_old = getContentFileAbsolute(lang);
        String ext_old = getFileExtension();
        super.setFileExtension(file_extension);
        File f_new = getContentFileAbsolute(lang);
        if (! f_new.equals(f_old)) {
            if (f_old.exists() && !f_old.renameTo(f_new)) {
                super.setFileExtension(ext_old);  // undo change
                throw new DocRuntimeException("File rename failed: " + f_old + " to " + f_new);
            }
        }
    }

    public void setContentFile(File img, String mimeType) throws DocException 
    {
        throw new DocException("Operation setContentFile not supported yet.");
    }

    public byte[] getRendition(DocImageRendition rendition) throws DocException
    {
        try {
            String lang = getContentLang();
            File f = createRendition(rendition, lang);
            return getContent(f);
        } catch (Exception ex) {
            throw new DocException("Could not create rendition: " + ex.getMessage());
        }
    }

    public InputStream getRenditionStream(DocImageRendition rendition) throws DocException
    {
        try {
            String lang = getContentLang();
            File f = createRendition(rendition, lang);
            return getContentStream(f);
        } catch (Exception ex) {
            throw new DocException("Could not create rendition: " + ex.getMessage());
        }
    }

    public void setContentType(String mime_type)
    {
        if (! mime_type.startsWith("image")) {
            throw new DocRuntimeException("MIME-Type of an image must be an image type.");
        }
        super.setContentType(mime_type);
    }

    public void setContentStream(InputStream dataStream)
    {
        super.setContentStream(dataStream);
        deleteRenditions(getDocSession().getTranslationMode());
    }
    
    public void deleteContent(String lang_code)
    {
        super.deleteContent(lang_code);
        deleteRenditions(lang_code);
    }

    // public void deleteTranslation(String lang_code)
    // {
    //     super.deleteTranslation(lang_code);
    //     deleteRenditions(lang_code);
    // }

}
