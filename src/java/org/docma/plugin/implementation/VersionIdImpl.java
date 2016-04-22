/*
 * VersionIdImpl.java
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

import org.docma.plugin.VersionId;
import org.docma.coreapi.DocVersionId;

/**
 *
 * @author MP
 */
public class VersionIdImpl implements VersionId
{
    private final DocVersionId docVersionId;

    public VersionIdImpl(DocVersionId verId) 
    {
        if (verId == null) {
            throw new RuntimeException("Null as version id is not allowed.");
        }
        this.docVersionId = verId;
    }

    // ********* Interface VersionId (visible by plugins) **********
    
    public boolean isLowerThan(VersionId verId) 
    {
        return this.docVersionId.isLowerThan(((VersionIdImpl) verId).getDocVersionId());
    }

    public boolean isHigherThan(VersionId verId) 
    {
        return this.docVersionId.isHigherThan(((VersionIdImpl) verId).getDocVersionId());
    }

    public int compareTo(Object obj) 
    {
        return this.docVersionId.compareTo(((VersionIdImpl) obj).getDocVersionId());
    }

    public boolean equals(Object obj)
    {
        if (obj instanceof VersionIdImpl) {
            return this.docVersionId.equals(((VersionIdImpl) obj).getDocVersionId());
        } else {
            return false;
        }
    }

    public int hashCode()
    {
        return this.docVersionId.hashCode();
    }

    public String toString()
    {
        return this.docVersionId.toString();
    }

    // ********* Other methods (not directly visible by plugins) **********

    DocVersionId getDocVersionId()
    {
        return docVersionId;
    }
}
