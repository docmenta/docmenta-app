/*
 * DocContentImpl.java
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
 * Created on 17. Oktober 2007, 13:24
 */

package org.docma.coreapi.fsimplementation;

import java.io.*;
import org.docma.coreapi.*;
import org.docma.coreapi.implementation.*;
import org.docma.lockapi.*;
import org.w3c.dom.*;


/**
 *
 * @author MP
 */
public class DocContentImpl extends DocNodeImpl implements DocContent
{

    /** Creates a new instance of DocAtomImpl */
    public DocContentImpl(DocStoreSessionImpl docSess, int node_id)
    {
        super(docSess);
        Document indexDoc = getDocStore().getIndexDocument();
        Element elem = indexDoc.createElement(XMLConstants.TAG_CONTENT);
        elem.setAttribute(XMLConstants.ATTR_ID, IdRegistry.idToDOMString(node_id));
        elem.setIdAttribute(XMLConstants.ATTR_ID, true);
        setDOMElement(elem);
        // elem.setAttribute(XMLConstants.ATTR_CONTENT_TYPE, "text/plain");
    }

    public DocContentImpl(DocStoreSessionImpl docSess, Element existingDOM)
    {
        super(docSess);
        setDOMElement(existingDOM);
    }

    private String contentFileName()
    {
        String idstr = IdRegistry.idStringToDOMString(getId());
        return idstr.substring(0, 6) + File.separatorChar + idstr + ".content";
    }

    /* --------------  Package local methods ---------------------- */

    void makeParentDirs(File f)
    {
        File p = f.getParentFile();
        if (! p.exists()) p.mkdirs();
    }

    byte[] getContent(File f)
    {
        if (f.exists()) {
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream((int) f.length());
                InputStream in = new FileInputStream(f);
                try {
                    byte[] buf = new byte[1024];
                    int cnt;
                    while ((cnt = in.read(buf)) >= 0) out.write(buf, 0, cnt);
                } finally {
                    try { in.close(); } catch (Exception ex) {}
                }
                return out.toByteArray();
            } catch (Exception e) { throw new DocRuntimeException(e); }
        } else {
            return null;
        }
    }

    InputStream getContentStream(File f)
    {
        if (f.exists()) {
            try {
                return new FileInputStream(f);
            } catch (FileNotFoundException e) { throw new DocRuntimeException(e); }
        } else {
            return null;
        }
    }

    /* --------------  Protected methods ---------------------- */

    private String getContentPath()
    {
        return getContentPath(getDocSession().getTranslationMode());
    }

    public String getContentPath(String lang)
    {
        if (lang == null) {
            return "content" + File.separatorChar + contentFileName();
        } else {
            return "translations" + File.separatorChar + lang + File.separatorChar + contentFileName();
        }
    }

    private File getContentFileAbsolute()
    {
        String lang = getDocSession().getTranslationMode();
        File f = getContentFileAbsolute(lang);
        boolean notrans = (lang != null) && !hasTranslation(lang);
        // if (notrans && f.exists()) {
        //     f.delete();  // delete because file should not exist
        // }

        // if no translation exists, return the original content file
        if (notrans || ((lang != null) && !f.exists())) {
            f = getContentFileAbsolute(null);
        }

        return f;
    }

    protected File getContentFileAbsolute(String lang)
    {
        return new File(getDocStore().getDirectory(), getContentPath(lang));
    }
    

    /* --------------  Interface DocContent ---------------------- */

    public String getContentType() {
        return getAttribute(DocAttributes.CONTENT_TYPE);
    }

    
    public String getContentType(String lang_code) {
        return getAttribute(DocAttributes.CONTENT_TYPE, lang_code);
    }

    
    public void setContentType(String mime_type) {
        if ((mime_type != null) && (mime_type.length() == 0)) {
            mime_type = null;
        }
        setAttribute(DocAttributes.CONTENT_TYPE, mime_type);
    }
    
    
    public String getFileExtension() {
        return getAttribute(DocAttributes.FILE_EXTENSION);
    }


    public String getFileExtension(String lang_code) {
        return getAttribute(DocAttributes.FILE_EXTENSION, lang_code);
    }


    public void setFileExtension(String file_extension) {
        setAttribute(DocAttributes.FILE_EXTENSION, file_extension);
    }


    public byte[] getContent() {
        File f = getContentFileAbsolute();
        return getContent(f);
    }
    
    
    public void setContent(byte[] content) {
        setContentStream(new ByteArrayInputStream(content));
    }
    
    
    public InputStream getContentStream() {
        File f = getContentFileAbsolute();
        return getContentStream(f);
    }

    
    public void setContentStream(InputStream content_stream) {
        try {
            String lang = getDocSession().getTranslationMode();
            File f = getContentFileAbsolute(lang);
            makeParentDirs(f);
            FileOutputStream fout = new FileOutputStream(f);
            try {
                byte[] buf = new byte[1024];
                int cnt;
                while ((cnt = content_stream.read(buf)) >= 0) {
                    fout.write(buf, 0, cnt);
                }
            } finally {
                fout.close();
            }
            if (lang != null) {
                addTranslation(getDOMElement(), lang);
                getDocStore().saveOnCommit();
            }
            // fireChangedEvent();
        } catch (IOException ex) { throw new DocRuntimeException(ex); }
    }


    public String getContentString() {
        return getContentString("UTF-8");
    }


    public String getContentString(String charsetName) {
        File f = getContentFileAbsolute();
        if (f.exists()) {
            try {
                int sz = (int) f.length();
                FileInputStream fin = new FileInputStream(f);
                Reader in = new InputStreamReader(fin, charsetName);
                StringWriter out = new StringWriter(2*sz);
                try {
                    int cnt;
                    char[] buf = new char[1024];
                    while ((cnt = in.read(buf)) >= 0) out.write(buf, 0, cnt);
                } finally {
                    try { fin.close(); } catch (Exception ex) {}
                }
                return out.toString();
            } catch (Exception e) { throw new DocRuntimeException(e); }
        } else {
            return null;
        }

    }


    public void setContentString(String xmlstr) {
        setContentString(xmlstr, "UTF-8");
    }


    public void setContentString(String xmlstr, String charsetName) {
        try {
            setContent(xmlstr.getBytes(charsetName));
        } catch (UnsupportedEncodingException e) { throw new DocRuntimeException(e); }
    }


    public long getContentLength()
    {
        File f = getContentFileAbsolute();
        if (f.exists()) {
            return f.length();
        } else {
            return 0;
        }
    }

    public void deleteContent()
    {
        deleteContent(getDocSession().getTranslationMode());
    }

    public void deleteContent(String lang_code)
    {
        if (lang_code == null) {  
            // delete all translations before deleting the original content
            for (String trans : getTranslations()) { 
                if (trans != null) deleteContent(trans);   // recursive call (trans should never be null)
            }
        }
        File f = getContentFileAbsolute(lang_code);
        if (f.exists()) {
            if (! f.delete()) {
                org.docma.util.Log.warning("Could not delete file " + f.getAbsolutePath());
            }
        }
    }

    public void deleteTranslation(String lang_code)
    {
        deleteContent(lang_code);
        super.deleteTranslation(lang_code);
    }

    public boolean hasContent(String lang_code)
    {
        if ((lang_code != null) && !hasTranslation(lang_code)) { 
            return false;
        }
        File f = getContentFileAbsolute(lang_code);
        return f.exists();
    }

    public Lock getLock(String lockname)
    {
        return getDocStore().getLockManager().getLock(getId(), lockname);
    }

    public boolean setLock(String lockname, long timeout)
    {
        return getDocStore().getLockManager().setLock(getId(), lockname, getDocSession().getUserId(), timeout);
    }

    public boolean refreshLock(String lockname, long timeout)
    {
        return getDocStore().getLockManager().refreshLock(getId(), lockname, timeout);
    }

    public Lock removeLock(String lockname)
    {
        return getDocStore().getLockManager().removeLock(getId(), lockname);
    }


    public HistoryEntry[] getHistory() {
        return null; // not implemented yet
    }
}
