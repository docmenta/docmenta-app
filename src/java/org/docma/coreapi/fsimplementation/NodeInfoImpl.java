/*
 * NodeInfoImpl.java
 * 
 *  Copyright (C) 2014  Manfred Paula, http://www.docmenta.org
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
public class NodeInfoImpl implements NodeInfo
{
    private DocNode node;
    
    public NodeInfoImpl(DocNode node) 
    {
        this.node = node;
    }

    public String getId() 
    {
        return node.getId();
    }

    public String getTitle() 
    {
        return node.getTitle();
    }

    public String getAlias() 
    {
        return node.getAlias();
    }

    @Override
    public int hashCode() 
    {
        String id = getId();
        return (id == null) ? super.hashCode() : id.hashCode();
    }

    @Override
    public boolean equals(Object obj) 
    {
        if (obj == null) {
            return false;
        }
        if (! (obj instanceof NodeInfo)) {
            return false;
        }
        final String other_id = ((NodeInfo) obj).getId();
        return (other_id != null) && other_id.equals(getId());
    }

}
