/*
 * LockImpl.java
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

/**
 *
 * @author MP
 */
public class LockImpl implements org.docma.plugin.Lock
{
    private final org.docma.lockapi.Lock backendLock;
    
    LockImpl(org.docma.lockapi.Lock backendLock)
    {
        this.backendLock = backendLock;
    }

    public String getUserId() 
    {
        return backendLock.getUser();
    }

    public long getCreationTime() 
    {
        return backendLock.getCreationTime();
    }

    public long getTimeout() 
    {
        return backendLock.getTimeout();
    }
    
}
