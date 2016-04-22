/*
 * XMLConstants.java
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
 * 
 * Created on 16. Oktober 2007, 18:22
 */

package org.docma.coreapi.fsimplementation;

/**
 *
 * @author MP
 */
public interface XMLConstants
{
    String TAG_GROUP = "group";
    String TAG_CONTENT = "content";
    String TAG_ALIAS = "alias";
    String TAG_ATT = "att";
    String TAG_REFERENCE = "reference";
    // String TAG_TITLE = "title";
    // String TAG_LINK = "a";
    
    String ATTR_ID = "id";
    String ATTR_ATTNAME = "nm";
    String ATTR_CONTENT_CLASS = "contentclass";
    String ATTR_TRANSLATIONS = "trans";
    String ATTR_LANGUAGE = "lang";
    String ATTR_REFERENCE_TARGET = "target";
    // String ATTR_REFERENCE_TYPE = "type";
    // String ATTR_LINK_REF = "href";

    String CONTENT_CLASS_XML = "xml";
    String CONTENT_CLASS_IMAGE = "image";
    String CONTENT_CLASS_FILE = "file";

    // String REFERENCE_TYPE_INCLUDE = "include";
    String NEW_GROUP_TITLE = "New group";
}
