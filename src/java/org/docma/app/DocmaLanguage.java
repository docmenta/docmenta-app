/*
 * DocmaLanguage.java
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
public class DocmaLanguage implements Comparable
{
    private String code;
    private String description;

    public DocmaLanguage(String code, String description)
    {
        this.code = code.toLowerCase();
        this.description = description;
    }

    public String getCode()
    {
        return code;
    }

    public String getDescription()
    {
        return description;
    }

    public int hashCode()
    {
        return code.hashCode();   // toLowerCase
    }

    public boolean equals(Object other)
    {
        if (other instanceof DocmaLanguage) {
            return code.equalsIgnoreCase(((DocmaLanguage) other).getCode());
        } else {
            return false;
        }
    }

    public int compareTo(Object obj) 
    {
        String other_code = (obj instanceof DocmaLanguage) ? ((DocmaLanguage) obj).getCode() : obj.toString();
        return this.getCode().compareToIgnoreCase(other_code);
    }

}
