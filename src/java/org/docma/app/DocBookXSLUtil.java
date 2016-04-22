/*
 * DocBookXSLUtil.java
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

import java.util.*;
import org.docma.app.tools.GentextPropertiesGenerator;

/**
 *
 * @author MP
 */
public class DocBookXSLUtil
{
    public static String createCustomGeneratedTextI18nXML(Properties props, String langCode)
    {
        StringBuilder buf = new StringBuilder(200 + (props.size() * 100));
        buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
        buf.append("<l:i18n xmlns:l=\"http://docbook.sourceforge.net/xmlns/l10n/1.0\">");
        buf.append("<l:l10n language=\"").append(langCode).append("\">");
        Enumeration pnames = props.propertyNames();
        Map context_all = new HashMap();
        while (pnames.hasMoreElements()) {
            String pname = (String) pnames.nextElement();
            String pval = props.getProperty(pname);
            if ((pval == null) || pval.trim().equals("")) continue;
            // pname = pname.replace('"', ' ');  // normally pname should not contain double quotes!
            pval = pval.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&#34;");
            int pos = pname.indexOf(GentextPropertiesGenerator.CONTEXT_SEPARATOR);
            if (pos < 0) {
                // Create gentext element
                buf.append("<l:gentext key=\"").append(pname).append("\" text=\"")
                                               .append(pval).append("\"/>");
            } else {
                // Add to context map
                String context_name = pname.substring(0, pos).trim();
                String template_name = pname.substring(pos + 1).trim();
                Map context_map = (Map) context_all.get(context_name);
                if (context_map == null) {
                    context_map = new HashMap();
                    context_all.put(context_name, context_map);
                }
                context_map.put(template_name, pval);
            }
        }
        Iterator it = context_all.keySet().iterator();
        while (it.hasNext()) {
            String context_name = (String) it.next();
            buf.append("<l:context name=\"").append(context_name).append("\">");
            Map context_map = (Map) context_all.get(context_name);
            Iterator templ_it = context_map.keySet().iterator();
            while (templ_it.hasNext()) {
                String template_name = (String) templ_it.next();
                String template_text = (String) context_map.get(template_name);
                buf.append("<l:template name=\"").append(template_name)
                                                 .append("\" text=\"")
                                                 .append(template_text).append("\"/>");
            }
            buf.append("</l:context>");
        }
        buf.append("</l:l10n>");
        buf.append("</l:i18n>");
        return buf.toString();
    }
}
