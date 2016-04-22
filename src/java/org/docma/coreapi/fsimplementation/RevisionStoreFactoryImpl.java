/*
 * RevisionStoreFactoryImpl.java
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

/**
 *
 * @author MP
 */
public class RevisionStoreFactoryImpl implements RevisionStoreFactory
{
    private int maxRevisionsPerUser = 5;

    public RevisionStoreSession createSession(DocStoreSession docSess)
    {
        return new RevisionStoreSessImpl(docSess, maxRevisionsPerUser);
    }

    public int getMaxRevisionsPerUser()
    {
        return this.maxRevisionsPerUser;
    }

    public void setMaxRevisionsPerUser(int max_revisions)
    {
        this.maxRevisionsPerUser = max_revisions;
    }

}
