/*
 * DocmaOutputFormat.java
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

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author MP
 */
public class DocmaOutputFormat 
{
    private String format;
    private String subformat;
    // private OutputProcessor outProc;
    // private PropertyDefinition[] propertyDefs = null;
    private Map<String, String> defaultProps = null;

    public DocmaOutputFormat(String format, String subformat) 
    {
        this.format = format;
        this.subformat = subformat;
    }
    
//    public PropertyDefinition[] getGenericProperties()
//    {
//        return (propertyDefs == null) ? new PropertyDefinition[0] : propertyDefs;
//    }
//    
//    public void setGenericProperties(PropertyDefinition[] propDefs);
//    {
//        propertyDefs = propDefs;
//    }
    
    public String getDefaultPropertyValue(String propName)
    {
        if (defaultProps == null) {
            return "";
        }
        String val = defaultProps.get(propName);
        return (val == null) ? "" : val;
    }
    
    public void setDefaultPropertyValues(Map<String, String> vals)
    {
        if (vals == null) {
            defaultProps = null;
        } else {
            defaultProps = new HashMap<String, String>();
            defaultProps.putAll(vals);
        }
    }
}
