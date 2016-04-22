/*
 * ContentLanguagesImpl.java
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

package org.docma.app.fsimplementation;

import org.docma.app.DocmaLanguage;
import org.docma.app.ContentLanguages;
import org.docma.coreapi.*;
import org.docma.util.Log;
import java.io.*;
import java.util.*;

/**
 *
 * @author MP
 */
public class ContentLanguagesImpl implements ContentLanguages
{
    private static final String LANGUAGES_FILENAME = "contentlangs.properties";

    private File baseDir;
    private File propFile;
    private Properties props;
    private DocmaLanguage[] supportedLangs;
    private Map langMap;

    public ContentLanguagesImpl(String baseDirectory) throws DocException
    {
        baseDir = new File(baseDirectory);
        propFile = new File(baseDir, LANGUAGES_FILENAME);
        try {
            props = new Properties();
            InputStream fin = new FileInputStream(propFile);
            props.load(fin);
            fin.close();
        } catch (Exception ex) {
            Log.error("Could not load supported languages file: " + propFile);
            throw new DocException(ex);
        }
        int cnt = props.size();
        supportedLangs = new DocmaLanguage[cnt];
        langMap = new HashMap(2*cnt);
        Enumeration names = props.propertyNames();
        for (int i=0; i < cnt; i++) {
            String lang_code = (String) names.nextElement();
            DocmaLanguage lang = new DocmaLanguage(lang_code, props.getProperty(lang_code));
            // lang.setTranslation(! lang_code.equalsIgnoreCase("en"));  // default original language is English
            supportedLangs[i] = lang;
            langMap.put(lang_code.toLowerCase(), lang);
        }
    }

    public DocmaLanguage[] getSupportedLanguages()
    {
        return supportedLangs;
    }

    public DocmaLanguage getLanguage(String lang_code)
    {
        return (DocmaLanguage) langMap.get(lang_code.toLowerCase());
    }

}
