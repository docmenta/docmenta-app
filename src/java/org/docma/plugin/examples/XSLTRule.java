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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
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
import org.docma.plugin.UserSession;
import org.docma.plugin.rules.HTMLRule;
import org.docma.plugin.rules.HTMLRuleConfig;
import org.docma.plugin.rules.HTMLRuleContext;
import org.docma.util.Log;
import org.docma.util.XMLElementContext;
import org.docma.util.XMLElementHandler;
import org.docma.util.XMLParseException;
import org.docma.util.XMLProcessor;
import org.docma.util.XMLProcessorFactory;

/**
 *
 * @author MP
 */
public class XSLTRule implements HTMLRule, ErrorListener, XMLElementHandler
{
    public static final String CHECK_ID_APPLY_XSL = "apply_xsl";
    
    private static final String ARG_XSLT_SCRIPT = "script";
    private static final String ARG_FACTORY_CLASS = "factory";
    private static final String ARG_TAGS = "tags"; // Deprecated; should be replaced by the "apply" argument.
    private static final String ARG_APPLY = "apply";
    private static final String ARG_KEEP = "keep";
    
    private String scriptAlias = null;
    private String factoryClsName = null;
    private static final Map<String, TransformerFactory> factoryMap = new HashMap<String, TransformerFactory>();
    private Transformer transformer = null;
    private String lang = "en";
    
    // Configuration of elements to be transformed
    private final Map<String, List<List<AttribConf>>> elemMap = new HashMap<String, List<List<AttribConf>>>();
    
    // Configuration of elements to be kept
    private final Map<String, List<List<AttribConf>>> keepMap = new HashMap<String, List<List<AttribConf>>>();
    
    private HTMLRuleContext ruleCtx = null;
    private boolean transformed = false;
    

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
        return new String[] { CHECK_ID_APPLY_XSL };
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
        elemMap.clear();
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
                } else if (ARG_XSLT_SCRIPT.equalsIgnoreCase(aname)) {
                    scriptAlias = avalue;
                } else if (ARG_APPLY.equalsIgnoreCase(aname) || ARG_TAGS.equalsIgnoreCase(aname)) {
                    readElementsConfigString(avalue, elemMap);
                } else if (ARG_KEEP.equalsIgnoreCase(aname)) {
                    readElementsConfigString(avalue, keepMap);
                }
            } else {
                scriptAlias = arg;
            }
        }
    }

    public void startBatch(HTMLRuleContext ctx) 
    {
        StoreConnection conn = ctx.getStoreConnection();
        UserSession     sess = (conn == null) ? null : conn.getUserSession();
        Locale          loc  = (sess == null) ? null : sess.getCurrentLocale();
        lang = (loc == null)  ? "en" : loc.getLanguage();
        transformer = null;  // The XSLT script may have changed. Create new transformer instance.
    }

    public void finishBatch(HTMLRuleContext ctx) 
    {
    }

    public String apply(String content, HTMLRuleContext ctx) 
    {
        if (! ctx.isEnabled(CHECK_ID_APPLY_XSL)) {
            return null;   // Check is disabled. Nothing to do.
        }
        
        ruleCtx = ctx;
        try {
            initTransformer(ctx);
            
            content = content.trim();
            StringBuilder output = new StringBuilder();

            XMLProcessor xmlproc = XMLProcessorFactory.newInstance();
            xmlproc.setCheckWellformed(false);
            xmlproc.setIgnoreElementCase(true);
            xmlproc.setIgnoreAttributeCase(true);
            
            transformed = false;
            if (elemMap.isEmpty()) {
                // Transform all root elements
                // res = transformRootElements(content);
                xmlproc.setElementHandler(this);
            } else {
                for (String ename : elemMap.keySet()) {
                    xmlproc.setElementHandler(ename, this);
                }
            }
            xmlproc.process(content, output);
            if (transformed) {  // If at least one element in content has been transformed...
                return output.toString();  // ...get the updated content.
            } else {
                return null;
            }
        // } catch (TransformerException te) {
        //     throw new RuntimeException(te);
        } catch (XMLParseException pe) {
            throw new RuntimeException(pe);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            ruleCtx = null;
        }
    }

    /* --------------  Interface XMLElementHandler  ---------------------- */
    
    public void processElement(XMLElementContext elemCtx) 
    {
        String elemName = elemCtx.getElementName().toLowerCase();
        if (! keepMap.isEmpty()) {  // Argument "keep" has been configured
            // Check if element shall be kept
            if (evaluate(elemName, elemCtx, keepMap)) {
                // The element and all sub-elements shall be kept (unchanged).
                // This can be achieved by replacing the element (including 
                // sub-elements) by itself. This way the sub-elements are 
                // not processed anymore.
                elemCtx.replaceElement(elemCtx.getElement());
                return;
            }
        }
        
        // If no element configuration exists, then transform all (root) elements.
        // If element configuration exists, then transform the element given by 
        // elemCtx only if the configured expression evaluates to true.        
        if (elemMap.isEmpty() || evaluate(elemName, elemCtx, elemMap)) {
            try {
                StringWriter buf = new StringWriter();
                String elem = elemCtx.getElement();
                transformer.transform(new StreamSource(new StringReader(elem)),
                                      new StreamResult(buf));
                String replace = buf.toString().trim();
                // Remove <?xml version="1.0" encoding="UTF-8"?>
                while (replace.startsWith("<?")) {
                    replace = replace.substring(replace.indexOf("?>") + 2).trim();
                }
                if (! elem.equals(replace)) {
                    if (ruleCtx.isAutoCorrect(CHECK_ID_APPLY_XSL)) {
                        elemCtx.replaceElement(replace);
                        transformed = true;
                        ruleCtx.logText(label("msgElementUpdated", elemName), elem);
                        ruleCtx.logText(label("msgReplacedBy"), replace);
                    } else {
                        ruleCtx.log(CHECK_ID_APPLY_XSL, label("msgFoundElementUpdate", elemName));
                        ruleCtx.logText(label("msgHeaderOldElement"), elem);
                        ruleCtx.logText(label("msgHeaderNewElement"), replace);
                    }
                }
            } catch (TransformerException te) {
                if (ruleCtx != null) {
                    ruleCtx.log(LogLevel.ERROR, te.getMessageAndLocation());
                } else {
                    Log.error(te.getMessageAndLocation());
                }
            }
        }
    }

    /* --------------  Interface ErrorListener  ---------------------- */
    
    public void warning(TransformerException exception) throws TransformerException 
    {
        if (ruleCtx != null) {
            ruleCtx.log(CHECK_ID_APPLY_XSL, exception.getMessageAndLocation());
        } else {
            Log.warning(exception.getMessageAndLocation());
        }
    }

    public void error(TransformerException exception) throws TransformerException 
    {
        throw exception;
    }

    public void fatalError(TransformerException exception) throws TransformerException 
    {
        throw exception;
    }
    
    /* --------------  Private methods  ---------------------- */

    private void readElementsConfigString(String configValue, Map<String, List<List<AttribConf>>> result)
    {
        StringTokenizer st = new StringTokenizer(configValue, ",");
        while (st.hasMoreTokens()) {
            String elem = st.nextToken();
            List<List<AttribConf>> orList = new ArrayList<List<AttribConf>>();
            int p1 = elem.indexOf('[');
            if (p1 > 0) {
                int p2 = elem.lastIndexOf(']');
                if (p2 > p1) {
                    String expr = elem.substring(p1 + 1, p2);
                    // Note: whitespace is not allowed. Therefore no trim required.
                    elem =  elem.substring(0, p1);
                    StringTokenizer st2 = new StringTokenizer(expr, "|");
                    while (st2.hasMoreTokens()) {
                        String atts = st2.nextToken();
                        List<AttribConf> andList = new ArrayList<AttribConf>();
                        StringTokenizer st3 = new StringTokenizer(atts, "+");
                        while (st3.hasMoreTokens()) {
                            String att = st3.nextToken();
                            String val = null;
                            boolean isNot = att.startsWith("!");
                            if (isNot) {
                                att = att.substring(1);
                            }
                            int p3 = att.indexOf('=');
                            if (p3 > 0) {  // value exists
                                val = att.substring(p3 + 1);
                                if (att.charAt(p3 - 1) == '!') {  // operator != (equals not)
                                    att = att.substring(0, p3 - 1);
                                    isNot = !isNot;
                                } else {
                                    att = att.substring(0, p3);
                                }
                                if ((val.startsWith("\"") && val.endsWith("\"")) || 
                                    (val.startsWith("'") && val.endsWith("'"))) { 
                                    val = val.substring(1, val.length() - 1);
                                }
                                val = resolveConfigEscapes(val);
                            }
                            andList.add(new AttribConf(att, val, isNot));
                        }
                        orList.add(andList);
                    }
                }
            }
            result.put(elem.toLowerCase(), orList);
        }
    }
    
    private boolean evaluate(String elemName, 
                             XMLElementContext elemCtx, 
                             Map<String, List<List<AttribConf>>> configMap) 
                          // List<String> attNames, List<String> attValues
    {
        // Element names have been configured. Check if elemName is included.
        List<List<AttribConf>> orList = configMap.get(elemName);
        if (orList == null) {  // elemName is not in configMap...
            return false;      // ...do not transform this element
        }

        // Check if attribute expression exists.
        if (orList.isEmpty()) {
            // elemName is configured, but without attribute expression 
            // (no or empty square brackets []) ...
            return true;  // ...transform this element
        }

        // Element name has been configured with attribute expression.
        // Evaluate attribute expression.
        boolean orResult = false;
        for (List<AttribConf> andList : orList) {
            boolean andResult = true;
            // Note: andList is never empty
            for (AttribConf ac : andList) {
                String aName = ac.getName();
                String compVal = ac.getValue();  // Configured comparison value.
                // int idx = attNames.indexOf(aName);
                // String attVal = (idx < 0) ? null : attValues.get(idx);
                String attVal = elemCtx.getAttributeValue(aName);
                boolean b;  // The result of the attribute expression.
                if (compVal == null) { // No comparison value -> check if attribute exists.
                    b = (attVal != null);
                    if (ac.isNot()) b = !b;
                } else { // Comparison value exists -> check if attribute equals value
                    if (attVal == null) {  
                        // If attribute does not exist, then this is the same  
                        // as if attribute were not equal to the comparison value.
                        b = ac.isNot();
                    } else {
                        b = compVal.equals(attVal);
                        if (ac.isNot()) b = !b;
                    }
                }
                if (! b) { // If first attribute-expression is false, then 
                           // the complete and-expression is false.
                    andResult = false;
                    break;
                }
            }
            
            if (andResult) { // If first and-expression is true, then the 
                             // complete or-expression is true.
                orResult = true;
                break;
            }
        }
        return orResult;
    }

//    private String transformRootElements(String content) throws Exception
//    {
//        StringBuilder output = new StringBuilder();
//        XMLParser parser = new XMLParser(content);
//        List<String> attNames = new ArrayList<String>();
//        List<String> attValues = new ArrayList<String>();
//        boolean transformed = false;
//        int eventType;
//        do {
//            eventType = parser.next();
//            if (eventType == XMLParser.START_ELEMENT) {
//                String elemName = parser.getElementName().toLowerCase();
//
//                parser.getAttributes(attNames, attValues);
//                int outerStart = parser.getStartOffset();
//                int outerEnd;
//                if (parser.isEmptyElement()) {
//                    outerEnd = parser.getEndOffset();
//                } else {
//                    // Read up to the closing tag of the root element
//                    parser.readUntilCorrespondingClosingTag();
//                    outerEnd = parser.getEndOffset();
//                }
//
//                String elem = content.substring(outerStart, outerEnd);
//                if (evaluate(elemName, attNames, attValues)) {
//                    StringWriter buf = new StringWriter();
//                    transformer.transform(new StreamSource(new StringReader(elem)),
//                                          new StreamResult(buf));
//                    elem = buf.toString().trim();
//                    // Remove <?xml version="1.0" encoding="UTF-8"?>
//                    while (elem.startsWith("<?")) {
//                        elem = elem.substring(elem.indexOf("?>") + 2).trim();
//                    }
//                    transformed = true;
//                }
//                output.append(elem);
//            }
//        } while (eventType != XMLParser.FINISHED);
//        
//        if (transformed) {
//            String res = output.toString();
//            return res.equals(content) ? null : res;
//        } else {
//            return null;
//        }
//    }

    private String label(String msgKey, Object... args) 
    {
        return PluginUtil.getResourceString(this.getClass(), lang, msgKey, args);
    }
    
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
            // transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        } catch (TransformerConfigurationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String resolveConfigEscapes(String str)
    {
        int pos = 0;
        while (pos < str.length()) {
            int uni_start = str.indexOf("\\u", pos);
            if (uni_start < 0) {
                return str;
            } else {
                pos = uni_start + 1;  // continue after this position
                int uni_end = uni_start + 6;
                if (str.length() >= uni_end) {
                    try {
                        char unicode = (char) Integer.parseInt(str.substring(uni_start + 2, uni_end));
                        str = str.substring(0, uni_start) + unicode + str.substring(uni_end);
                    } catch (Exception ex) {}  // no valid escape; continue search at pos
                }
            }
        }
        return str;
    }

    private static class AttribConf
    {
        private final String name;
        private final String value;
        private final boolean not;
        
        AttribConf(String attName, String val, boolean not) 
        {
            this.name = attName;
            this.value = val;
            this.not = not;
        }
        
        String getName()
        {
            return name;
        }
        
        String getValue()
        {
            return value;
        }
        
        boolean isNot()
        {
            return not;
        }
    }

}
