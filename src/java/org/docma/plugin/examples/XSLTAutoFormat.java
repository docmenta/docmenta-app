/*
 * XSLTAutoFormat.java
 *
 *  Copyright (C) 2015  Manfred Paula, http://www.docmenta.org
 *
 *  This file is part of Docmenta. Docmenta is free software: you can
 *  redistribute it and/or modify it under the terms of the GNU Lesser
 *  General Public License as published by the Free Software Foundation,
 *  either version 3 of the License, or (at your option) any later version.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Docmenta.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.docma.plugin.examples;

import java.io.*;
import java.util.*;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;

import org.docma.plugin.*;

/**
 *
 * @author MP
 */
public class XSLTAutoFormat implements AutoFormat
{
    private static final String ARG_XSLT_SCRIPT = "script";
    private static final String ARG_FACTORY_CLASS = "factory";
    
    private ExportContext exportCtx;
    private TransformerFactory defaultFactory;
    private Map<String, TransformerFactory> factoryMap;
    private Map<String, Transformer> transformerMap;
    
    public void initialize(ExportContext ctx)
    {
        exportCtx = ctx;
        defaultFactory = null;
        factoryMap = new HashMap<String, TransformerFactory>();
        transformerMap = new HashMap<String, Transformer>();
    }

    public void finished()
    {
        exportCtx = null;
        defaultFactory = null;
        factoryMap = null;
        transformerMap = null;
    }

    public void transform(TransformationContext ctx) throws Exception
    {
        String alias = null;
        String fact_classname = null;
        for (int i=0; i < ctx.getArgumentCount(); i++) {
            String arg = ctx.getArgument(i, "");
            int p = arg.indexOf('=');
            if (p > 0) {
                String aname = arg.substring(0, p).trim();
                String avalue = arg.substring(p + 1).trim();
                if (ARG_FACTORY_CLASS.equalsIgnoreCase(aname)) {
                    fact_classname = avalue;
                } else
                if (ARG_XSLT_SCRIPT.equalsIgnoreCase(aname)) {
                    alias = avalue;
                }
            } else {
                alias = arg;
            }
        }
        if (alias == null) {
            throw new Exception("Parameter missing: xslt-script alias.");
        }
        
        String trans_key;
        if (fact_classname == null) {
            trans_key = alias;
        } else {
            trans_key = fact_classname + "#" + alias;
        }
        Transformer transformer = transformerMap.get(trans_key);
        if (transformer == null) {
            String xslt_script = exportCtx.getContentStringByAlias(alias);
            if (xslt_script == null) {
                throw new Exception("Could not find xslt-script with alias " + alias);
            }
            TransformerFactory fact;
            if (fact_classname == null) {
                if (defaultFactory == null) {
                    defaultFactory = TransformerFactory.newInstance();
                }
                fact = defaultFactory;
            } else {
                fact = factoryMap.get(fact_classname);
                if (fact == null) {
                    fact = TransformerFactory.newInstance(fact_classname, null);
                    factoryMap.put(fact_classname, fact);
                }
            }
            transformer = fact.newTransformer(new StreamSource(new StringReader(xslt_script)));
            // transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformerMap.put(trans_key, transformer);
        }

        String input = ctx.getOuterString();
        
        StringWriter buf = new StringWriter();
        transformer.transform(new StreamSource(new StringReader(input)),
                              new StreamResult(buf));
        String res = buf.toString().trim();
        
        // Remove <?xml version="1.0" encoding="UTF-8"?>
        while (res.startsWith("<?")) {
            res = res.substring(res.indexOf("?>") + 2).trim();
        }
        ctx.getWriter().write(res);
    }

    public String getShortInfo(String languageCode)
    {
        return PluginUtil.getResourceString(this.getClass(), languageCode, "shortInfo");
    }

    public String getLongInfo(String languageCode)
    {
        return PluginUtil.getResourceString(this.getClass(), languageCode, "longInfo");
    }

}
