/*
 * DocXMLImpl.java
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
 * Created on 17. Oktober 2007, 13:35
 */

package org.docma.coreapi.fsimplementation;

import java.io.*;
import org.docma.coreapi.*;
import org.w3c.dom.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


/**
 *
 * @author MP
 */
public class DocXMLImpl extends DocContentImpl implements DocXML
{
   
    /** Creates a new instance of DocXMLImpl */
    public DocXMLImpl(DocStoreSessionImpl docSess, int node_id)
    {
        super(docSess, node_id);
        getDOMElement().setAttribute(XMLConstants.ATTR_CONTENT_CLASS, XMLConstants.CONTENT_CLASS_XML);
        // setContentType("text/xml");
        // setFileExtension("xml");
    }
    
    public DocXMLImpl(DocStoreSessionImpl docSess, Element existingDOM)
    {
        super(docSess, existingDOM);
        if (! existingDOM.hasAttribute(XMLConstants.ATTR_CONTENT_CLASS)) {
            existingDOM.setAttribute(XMLConstants.ATTR_CONTENT_CLASS, XMLConstants.CONTENT_CLASS_XML);
        }
    }
    
//    private String contentFileName()
//    {
//        String idstr = getId();
//        return idstr.substring(0, 7) + File.separatorChar + idstr.substring(7) + ".xml";
//    }
//
//    protected String getContentPath(String lang)
//    {
//        if (lang == null) {
//            return "content" + File.separatorChar + "xml" + File.separatorChar + contentFileName();
//        } else {
//            return "translations" + File.separatorChar + lang + File.separatorChar + "xml" + File.separatorChar + contentFileName();
//        }
//    }

    
    /* --------------  Interface DocXML ---------------------- */

    public Document getContentDOM() {
        String lang = getDocSession().getTranslationMode();
        File f = getContentFileAbsolute(lang);

        // if no translation exists, return the original content file
        if ((!f.exists()) && (lang != null)) f = getContentFileAbsolute(null);

        if (f.exists()) {
            try {
                return getDocStore().getDOMBuilder().parse(f);
            } catch (Exception ex) { throw new DocRuntimeException(ex); }
        } else {
            return null;
        }
    }

    public void setContentDOM(Document xmldoc) {
        File f = getContentFileAbsolute(getDocSession().getTranslationMode());
        try {
            getDocStore().getXMLTransformer().transform(new DOMSource(xmldoc), new StreamResult(f));
        } catch (Exception ex) { throw new DocRuntimeException(ex); }
    }
    
}
