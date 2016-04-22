/*
 * DocTextImpl.java
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
import java.io.*;
import org.w3c.dom.*;

/**
 *
 * @author MP
 */
public class DocTextImpl extends DocContentImpl implements DocText
{
    /** Creates a new instance of DocTextImpl */
    public DocTextImpl(DocStoreSessionImpl docSess, int node_id)
    {
        super(docSess, node_id);
    }

    public DocTextImpl(DocStoreSessionImpl docSess, Element existingDOM)
    {
        super(docSess, existingDOM);
        // getIdRegistry().registerId(existingDOM.getAttribute(XMLConstants.ATTR_ID));
        // setDOMElement(existingDOM);
    }

    /* --------------  Interface DocText ---------------------- */



}
