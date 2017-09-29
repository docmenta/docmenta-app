/*
 * DocmaI18Impl.java
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

package org.docma.webapp;

import java.util.Locale;
import org.zkoss.util.resource.Labels;
import org.zkoss.util.Locales;
import org.docma.coreapi.DocmaI18;
import org.docma.plugin.internals.LabelProvider;
import org.docma.plugin.internals.LabelUtil;

/**
 *
 * @author MP
 */
public class DocmaI18Impl implements DocmaI18, LabelProvider
{
    private static final DocmaI18Impl i18 = new DocmaI18Impl();

    static {
        LabelUtil.setLabelProvider(i18);
    }
    
    private DocmaI18Impl()
    {
    }
    
    public static DocmaI18 getInstance()
    {
        return i18;
    }
    
    public Locale getCurrentLocale()
    {
        return Locales.getCurrent();
    }

    public String getLabel(String key) 
    {
        return Labels.getLabel(key, key);
    }

    public String getLabel(String key, Object... args) 
    {
        if ((args == null) || (args.length == 0)) {
            return Labels.getLabel(key, key);
        } else {
            return Labels.getLabel(key, key, args);
        }
    }

    public String getLabel(Locale locale, String key) 
    {
        if (locale == null) {
            return Labels.getLabel(key, key);
        } else {
            Locale old = Locales.setThreadLocal(locale);
            try {
                return Labels.getLabel(key, key);
            } finally {
                Locales.setThreadLocal(old);
            }
        }
    }

    public String getLabel(Locale locale, String key, Object[] args) 
    {
        if (locale == null) {
            if ((args == null) || (args.length == 0)) {
                return Labels.getLabel(key, key);
            } else {
                return Labels.getLabel(key, key, args);
            }
        } else {
            Locale old = Locales.setThreadLocal(locale);
            try {
                if ((args == null) || (args.length == 0)) {
                    return Labels.getLabel(key, key);
                } else {
                    return Labels.getLabel(key, key, args);
                }
            } finally {
                Locales.setThreadLocal(old);
            }
        }
    }

}
