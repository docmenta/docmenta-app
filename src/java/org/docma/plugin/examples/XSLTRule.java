/*
 * XSLTRule.java
 * 
 *  Copyright (C) 2017  Manfred Paula, http://www.docmenta.org
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

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.docma.plugin.FileContent;
import org.docma.plugin.LogLevel;
import org.docma.plugin.Node;
import org.docma.plugin.PluginUtil;
import org.docma.plugin.StoreConnection;
import org.docma.plugin.rules.HTMLRule;
import org.docma.plugin.rules.HTMLRuleConfig;
import org.docma.plugin.rules.HTMLRuleContext;
import org.docma.util.Log;

/**
 *
 * @author MP
 */
public class XSLTRule implements HTMLRule, ErrorListener
{
    public static final String CHECK_ID_TRANSFORM = "apply_xsl";
    
    private static final String ARG_XSLT_SCRIPT = "script";
    private static final String ARG_FACTORY_CLASS = "factory";
    
    private String scriptAlias = null;
    private String factoryClsName = null;
    private static final Map<String, TransformerFactory> factoryMap = new HashMap<String, TransformerFactory>();
    private Transformer transformer = null;
    
    private HTMLRuleContext ruleCtx = null;
    

    /* --------------  Interface HTMLRule  ---------------------- */
    
    public String getShortInfo(String languageCode) 
    {
        return PluginUtil.getResourceString(this.getClass(), languageCode, "shortInfo");
    }

    public String getLongInfo(String languageCode) 
    {
        return PluginUtil.getResourceString(this.getClass(), languageCode, "longInfo");
    }

    public String[] getCheckIds() 
    {
        return new String[] { CHECK_ID_TRANSFORM };
    }

    public String getCheckTitle(String checkId, String languageCode) 
    {
        return PluginUtil.getResourceString(this.getClass(), languageCode, checkId + ".title");
    }

    public boolean supportsAutoCorrection(String checkId) 
    {
        return true;
    }

    public LogLevel getDefaultLogLevel(String checkId) 
    {
        return LogLevel.INFO;
    }
    
    public void configure(HTMLRuleConfig conf) 
    {
        scriptAlias = null;
        factoryClsName = null;
        transformer = null;
        for (String arg : conf.getArguments()) {
            int p = arg.indexOf('=');
            if (p > 0) {
                String aname = arg.substring(0, p).trim();
                String avalue = arg.substring(p + 1).trim();
                if (ARG_FACTORY_CLASS.equalsIgnoreCase(aname)) {
                    factoryClsName = avalue;
                } else
                if (ARG_XSLT_SCRIPT.equalsIgnoreCase(aname)) {
                    scriptAlias = avalue;
                }
            } else {
                scriptAlias = arg;
            }
        }
    }

    public void startBatch() 
    {
    }

    public void finishBatch() 
    {
    }

    public String apply(String content, HTMLRuleContext ctx) 
    {
        ruleCtx = ctx;
        try {
            initTransformer(ctx);
            StringWriter buf = new StringWriter();
            content = content.trim();
            transformer.transform(new StreamSource(new StringReader(content)),
                                  new StreamResult(buf));
            String res = buf.toString().trim();

            // Remove <?xml version="1.0" encoding="UTF-8"?>
            while (res.startsWith("<?")) {
                res = res.substring(res.indexOf("?>") + 2).trim();
            }
            if (res.equals(content)) {
                return null;
            } else {
                ctx.log(CHECK_ID_TRANSFORM, "msgContentUpdated");
                return res;
            }
        } catch (TransformerException ex) {
            throw new RuntimeException(ex);
        } finally {
            ruleCtx = null;
        }
    }
    
    /* --------------  Interface ErrorListener  ---------------------- */
    
    public void warning(TransformerException exception) throws TransformerException 
    {
        if (ruleCtx != null) {
            ruleCtx.logInfo(CHECK_ID_TRANSFORM, exception.getMessageAndLocation());
        } else {
            Log.warning(exception.getMessageAndLocation());
        }
    }

    public void error(TransformerException exception) throws TransformerException 
    {
        throw exception;
        // if (ruleCtx != null) {
        //     ruleCtx.log(CHECK_ID_TRANSFORM, exception.getMessageAndLocation());
        // } else {
        //     Log.error(exception.getMessageAndLocation());
        // }
    }

    public void fatalError(TransformerException exception) throws TransformerException 
    {
        throw exception;
    }
    
    /* --------------  Private methods  ---------------------- */

    private void initTransformer(HTMLRuleContext ctx) 
    {
        if (transformer != null) {
            return;
        }
        if (scriptAlias == null) {
            throw new RuntimeException("Missing XSLTRule argument: script");
        }
        StoreConnection conn = ctx.getStoreConnection();
        Node nd = conn.getNodeByAlias(scriptAlias);
        if (nd == null) {
            throw new RuntimeException("XSLTRule script with alias '" + scriptAlias + "' does not exist.");
        }
        if (! (nd instanceof FileContent)) {
            throw new RuntimeException("Referenced XSLTRule script with alias '" + scriptAlias + "' is no file-node.");
        }
        FileContent scriptNode = (FileContent) nd;
        String xsl_script = scriptNode.getContentString();
        
        String factKey = (factoryClsName == null) ? "" : factoryClsName;
        TransformerFactory fact;
        synchronized (factoryMap) {
            fact = factoryMap.get(factKey);
            if (fact == null) {
                if (factoryClsName == null) {
                    fact = TransformerFactory.newInstance();  // default factory
                } else {
                    fact = TransformerFactory.newInstance(factoryClsName, null);
                }
                factoryMap.put(factKey, fact);
            }
        }

        try {
            transformer = fact.newTransformer(new StreamSource(new StringReader(xsl_script)));
            transformer.setErrorListener(this);
        } catch (TransformerConfigurationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(ex);
        }
    }

}
