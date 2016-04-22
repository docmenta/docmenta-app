/*
 * DocmaAnchor.java
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

package org.docma.app;

/**
 *
 * @author MP
 */
public class DocmaAnchor
{
    private String alias;
    private String title;
    private int tagPosition;
    private DocmaNode node;

    public String getAlias()
    {
        return alias;
    }

    public void setAlias(String alias)
    {
        this.alias = alias;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public int getTagPosition()
    {
        return tagPosition;
    }

    public void setTagPosition(int pos)
    {
        this.tagPosition = pos;
    }

    public DocmaNode getNode() 
    {
        return node;
    }

    public void setNode(DocmaNode node) 
    {
        this.node = node;
    }
    

}
