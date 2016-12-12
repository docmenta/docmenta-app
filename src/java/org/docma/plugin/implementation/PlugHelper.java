/*
 * PlugHelper.java
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
package org.docma.plugin.implementation;

import org.docma.app.DocmaCharEntity;
import org.docma.app.DocmaLanguage;
import org.docma.plugin.CharEntity;
import org.docma.plugin.Language;

/**
 *
 * @author MP
 */
public class PlugHelper 
{

    public static DocmaLanguage toDocmaLanguage(Language lang)
    {
        if (lang instanceof DocmaLanguage) {
            return (DocmaLanguage) lang;
        } else {
            return new DocmaLanguage(lang.getCode(), lang.getDisplayName());
        }
    }
    
    public static DocmaLanguage[] toDocmaLanguages(Language[] langs)
    {
        if (langs == null) {
            return null;
        }
        DocmaLanguage[] arr = new DocmaLanguage[langs.length];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = toDocmaLanguage(langs[i]);
        }
        return arr;
    }
    
    public static DocmaCharEntity toDocmaCharEntity(CharEntity e)
    {
        if (e instanceof DocmaCharEntity) {
            return (DocmaCharEntity) e;
        } else {
            return new DocmaCharEntity(e.getSymbolic(), e.getNumeric(), e.isSelectable(), e.getDescription());
        }
    }
    
    public static DocmaCharEntity[] toDocmaCharEntities(CharEntity[] entities)
    {
        if (entities == null) {
            return null;
        }
        DocmaCharEntity[] arr = new DocmaCharEntity[entities.length];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = toDocmaCharEntity(entities[i]);
        }
        return arr;
    }

}
