/*
 * DocReferenceImpl.java
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
 */

package org.docma.coreapi.fsimplementation;

import org.docma.coreapi.*;
import org.w3c.dom.*;

/**
 *
 * @author MP
 */
public class DocReferenceImpl extends DocNodeImpl implements DocReference
{
    /** Creates a new instance of DocReferenceImpl */
    public DocReferenceImpl(DocStoreSessionImpl docSess, int node_id)
    {
        super(docSess);
        setDOMElement(createReferenceDOM(getIndexDocument(), node_id));
    }

    /** Creates a new instance of DocReferenceImpl from an existing DOM */
    public DocReferenceImpl(DocStoreSessionImpl docSess, Element existingDOM)
    {
        super(docSess);
        // getIdRegistry().registerId(existingDOM.getAttribute(XMLConstants.ATTR_ID));
        setDOMElement(existingDOM);
    }


    private static Element createReferenceDOM(Document doc, int ref_id)
    {
        String dom_id = IdRegistry.idToDOMString(ref_id);
        Element elem = doc.createElement(XMLConstants.TAG_REFERENCE);
        elem.setAttribute(XMLConstants.ATTR_ID, dom_id);
        elem.setIdAttribute(XMLConstants.ATTR_ID, true);
        return elem;
    }

    public String getTargetAlias()
    {
        return getDOMElement().getAttribute(XMLConstants.ATTR_REFERENCE_TARGET);
    }

    public void setTargetAlias(String alias)
    {
        getDOMElement().setAttribute(XMLConstants.ATTR_REFERENCE_TARGET, alias);
        getDocStore().saveOnCommit();
        fireChangedEvent();
    }


}
