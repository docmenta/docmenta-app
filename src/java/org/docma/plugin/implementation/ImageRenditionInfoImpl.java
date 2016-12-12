/*
 * ImageRenditionInfoImpl.java
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

import org.docma.coreapi.DocImageRendition;
import org.docma.plugin.ImageRenditionInfo;

/**
 *
 * @author MP
 */
public class ImageRenditionInfoImpl implements ImageRenditionInfo
{
    private final DocImageRendition docRendition;

    ImageRenditionInfoImpl(DocImageRendition docRendition) 
    {
        this.docRendition = docRendition;
    }
    
    public String getName() 
    {
        return docRendition.getName();
    }

    public String getFormat() 
    {
        return docRendition.getFormat();
    }

    public int getMaxHeight() 
    {
        return docRendition.getMaxHeight();
    }

    public int getMaxWidth() 
    {
        return docRendition.getMaxWidth();
    }
    
}
