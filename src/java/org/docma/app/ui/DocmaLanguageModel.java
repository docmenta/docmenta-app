/*
 * DocmaLanguageModel.java
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

package org.docma.app.ui;

import org.docma.app.DocmaLanguage;

/**
 *
 * @author MP
 */
public class DocmaLanguageModel extends DocmaLanguage
{
    private boolean translation = false;

    public DocmaLanguageModel(DocmaLanguage lang, boolean translation)
    {
        super(lang.getCode(), lang.getDescription());
        this.translation = translation;
    }

    public DocmaLanguageModel(String code, String description)
    {
        super(code, description);
    }

    public boolean isTranslation()
    {
        return translation;
    }

    public void setTranslation(boolean translation)
    {
        this.translation = translation;
    }

}
