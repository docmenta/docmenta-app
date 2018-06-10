/*
 * FigureInfo.java
 * 
 *  Copyright (C) 2018  Manfred Paula, http://www.docmenta.org
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

import java.util.*;
import javax.xml.stream.events.Attribute;

/**
 *
 * @author MP
 */
class FigureInfo 
{
    private String alias = null;
    private String caption = null;
    private Map<String, Attribute> imageAttribs = null;

    String getCaption() 
    {
        return (caption == null) ? "" : caption;
    }
    
    void setCaption(String capt) 
    {
        this.caption = capt;
    }
    
    String getAlias()
    {
        return alias;
    }
    
    void setAlias(String value) 
    {
        this.alias = value;
    }
    
    void setAliasIfNotExists(String value) 
    {
        if (value != null) {
            if ((this.alias == null) || this.alias.equals("")) {
                this.alias = value;
            }
        }
    }
    
    Iterator<Attribute> getImageAttribs() 
    {
        return imageAttribs.values().iterator();
    }
    
    void setImageAttribs(Iterator attribs) 
    {
        imageAttribs = WebFormatter.copyAttributesAsMap(attribs);
    }
    
    void setImageAttrib(Attribute attrib)
    {
        if (imageAttribs == null) {
            imageAttribs = new HashMap<String, Attribute>();
        }
        imageAttribs.put(attrib.getName().getLocalPart(), attrib);
    }
            
    void removeImageAttrib(String attname) 
    {
        if (imageAttribs != null) {
            imageAttribs.remove(attname);
        }
    }
    
}
