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
    private static final String ARG_UPDATE_SPACE = "update_space";
    
    private String scriptAlias = null;
    private String factoryClsName = null;
    private boolean updateSpace = true;
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
        keepMap.clear();
        scriptAlias = null;
        factoryClsName = null;
        updateSpace = true;
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
                } else if (ARG_UPDATE_SPACE.equalsIgnoreCase(aname)) {
                    updateSpace = "true".equalsIgnoreCase(avalue);
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
                if (elementsAreEqual(elem, replace)) {
                    // The XSL transformation did not change the element.
                    // Skip the complete element (including sub-elements).
                    // To avoid processing of sub-elements, replace the
                    // element by itself. 
                    elemCtx.replaceElement(elem);
                } else {
                    if (ruleCtx.isAutoCorrect(CHECK_ID_APPLY_XSL)) {
                        // Auto-correction is enabled.
                        elemCtx.replaceElement(replace);
                        transformed = true;
                        ruleCtx.logText(label("msgElementUpdated", elemName), elem);
                        ruleCtx.logText(label("msgReplacedBy"), replace);
                    } else {
                        // Auto-correction is disabled. Therefore, only log the 
                        // element (but do not replace the element). 
                        // To avoid processing of sub-elements, replace the
                        // element by itself. 
                        elemCtx.replaceElement(elem);
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

    private boolean elementsAreEqual(String elem, String replace) 
    {
        if (updateSpace) {
            return elem.equals(replace);
        } else {
            return equalsIgnoreSpace(elem, replace);
        }
    }
    
    private boolean equalsIgnoreSpace(String str1, String str2)
    {
        int len1 = str1.length();
        int len2 = str2.length();
        int p1 = 0;
        int p2 = 0;
        while ((p1 < len1) && (p2 < len2)) {
            char  ch1 = str1.charAt(p1);
            char  ch2 = str2.charAt(p2);
            if (Character.isWhitespace(ch1)) {
                if (Character.isWhitespace(ch2)) {
                    // Skip whitespace in both strings.
                    p1 = skipWhitespace(str1, p1 + 1); // p1 is next non-whitespace or end of string
                    p2 = skipWhitespace(str2, p2 + 1); // p2 is next non-whitespace or end of string
                } else {
                    return false;
                }
            } else {
                if (ch1 != ch2) {
                    return false;
                }
                p1++;
                p2++;
            }
        }
        
        // End of str1 and/or str2 has been reached.
        // If end of one of both strings has not been reached, then
        // the strings are considered equal, only if all remaining characters
        // are whitespace.
        if ((p1 < len1) && (skipWhitespace(str1, p1) < len1)) {
            return false;
        }
        if ((p2 < len2) && (skipWhitespace(str2, p2) < len2)) {
            return false;
        }
        return true;
    }
    
    private int skipWhitespace(String str, int startPos) 
    {
        int len = str.length();
        while ((startPos < len) && Character.isWhitespace(str.charAt(startPos))) {
            startPos++;
        }
        return startPos;
    }
    
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
                            boolean isNot = false;
                            while (att.startsWith("!")) {
                                isNot = !isNot;
                                att = att.substring(1);
                            }
                            String attName = att;
                            String comparator = null;
                            int p3 = att.indexOf('=');
                            if (p3 > 0) {  // value exists
                                val = att.substring(p3 + 1);
                                char ch = att.charAt(p3 - 1);
                                if (ch == '!') {  // operator != (equals not)
                                    isNot = !isNot;
                                    comparator = "=";
                                    attName = att.substring(0, p3 - 1);
                                } else if (ch == '~') {
                                    comparator = "~=";
                                    attName = att.substring(0, p3 - 1);
                                } else {
                                    comparator = "=";
                                    attName = att.substring(0, p3);
                                }
                            }
                            andList.add(new AttribConf(attName, val, comparator, isNot));
                        }
                        orList.add(andList);
                    }
                }
            }
            
            // elem contains one element name or several element names 
            // separated by slash character (/).
            elem = elem.toLowerCase();
            if (elem.contains("/")) {
                // Element names separated by slash character (/) share the 
                // same attribute conditions.   
                StringTokenizer elemNames = new StringTokenizer(elem, "/");
                while (elemNames.hasMoreTokens()) {
                    result.put(elemNames.nextToken().trim(), orList);
                }
            } else {
                // Only one element name.
                result.put(elem.trim(), orList);
            }
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
            orList = configMap.get("*");   // Get wildcard configuration
        }
        if (orList == null) {  // elemName and wildcard is not in configMap...
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
                if (compVal == null) { 
                    // If no comparison value give, then this is an attribute 
                    // existance check: attribute name, optionally preceded
                    // by exclamation mark (logical not), but no comparator.
                    b = (attVal != null);  // true if attribute exists
                    if (ac.isNot()) {  
                        // Attribute name is preceded by exclamation mark.
                        b = !b;  // true if attribute does not exist
                    }
                } else { 
                    // Comparison value exists. Evaluate comparator expression.
                    b = ac.evaluate(attVal);
                }
                if (! b) { // If attribute-expression is false, then 
                           // the complete and-expression is false.
                    andResult = false;
                    break;
                }
            }
            
            if (andResult) { // If and-expression is true, then the 
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

    private static String resolveConfigEscapes(String str)
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
                        char unicode = (char) Integer.parseInt(str.substring(uni_start + 2, uni_end), 16);
                        str = str.substring(0, uni_start) + unicode + str.substring(uni_end);
                    } catch (Exception ex) {}  // no valid escape; continue search at pos
                }
            }
        }
        return str;
    }
    
    private static String removeQuotes(String val)
    {
        if ((val == null) || (val.length() <= 1)) {
            return val;
        }
        if ((val.startsWith("\"") && val.endsWith("\"")) || 
            (val.startsWith("'") && val.endsWith("'"))) { 
            return val.substring(1, val.length() - 1);
        } else {
            return val;
        }
    }

//    private String normalizeSpace(String str)
//    {
//        int len = str.length();
//        StringBuilder sb = new StringBuilder(len);
//        int i = 0;
//        while (i < len) {
//            char  ch = str.charAt(i);
//            if (Character.isWhitespace(ch)) {
//                 // Normalize sequence of whitespace to normal space.
//                sb.append(' ');
//                // Skip whitespace up to the next non-whitespace character.
//                while (++i < len) {
//                    ch = str.charAt(i);
//                    if (! Character.isWhitespace(ch)) {  // found non-whitespace
//                        sb.append(ch);   // output non-whitespace
//                        i++; // Continue in outer while loop with next character
//                        break;
//                    }
//                }
//            } else {
//                sb.append(ch);   // output non-whitespace
//                i++;
//            }
//        }
//        return sb.toString();
//    }
    

    private static class AttribConf
    {
        private final String name;
        private final String compareValue;
        private final String comparator;
        private final boolean not;
        
        private List<String> subValues;
        
        AttribConf(String attName, String val, String comparator, boolean not) 
        {
            this.subValues = null;
            if (val != null) {
                val = removeQuotes(val);
                if (val.contains("*")) {
                    this.subValues = new ArrayList<String>();
                    StringTokenizer st = new StringTokenizer(val, "*");
                    while (st.hasMoreTokens()) {
                        this.subValues.add(resolveConfigEscapes(st.nextToken()));
                    }
                } else {
                    val = resolveConfigEscapes(val);
                }
            }
            this.name = attName;
            this.compareValue = val;
            this.comparator = comparator;
            this.not = not;
        }
        
        String getName()
        {
            return name;
        }
        
        String getValue()
        {
            return compareValue;
        }
        
        boolean isNot()
        {
            return not;
        }
        
        boolean evaluate(String realValue)
        {
            boolean res = false;
            if ("=".equals(comparator)) {
                res = evalEquals(realValue);
            } else if ("~=".equals(comparator)) {
                res = evalContainsToken(realValue);
            }
            return isNot() ? !res : res;
        }
        
        private boolean evalEquals(String realValue)
        {
            boolean res;
            if (realValue == null) {  
                // If attribute does not exist, then this is the same  
                // as if attribute were not equal to the comparison value.
                res = false;
            } else {
                if (subValues == null) {
                    // Comparison value does not contain wildcard (*)
                    // Compare if values are equal.
                    res = realValue.equals(getValue());
                } else {
                    // Comparison value contains one or more wildcards (*)
                    if (subValues.isEmpty()) {
                        // Value consists of wildcard (*) only
                        res = true;  // This matches any attribute value
                    } else {
                        int startPos = 0;
                        String sub = subValues.get(0);
                        
                        // Check if first substring matches
                        int p = realValue.indexOf(sub);
                        if (p >= 0) {  
                            // If the comparison value does not start with a
                            // wildcard (*), then the attribute value has to 
                            // start with the first substring. 
                            res = compareValue.startsWith("*") ? true : (p == 0);
                            startPos = p + sub.length();
                        } else {
                            res = false;
                        }
                        
                        if (res) {  // If first substring has matched...
                            // ...check the remaining substrings
                            for (int i = 1; i < subValues.size(); i++) {
                                sub = subValues.get(i);
                                p = realValue.indexOf(sub, startPos);
                                if (p >= 0) {
                                    startPos = p + sub.length();
                                } else {
                                    res = false;
                                    break;
                                }
                            }
                            
                            if (res && !compareValue.endsWith("*")) {
                                // If the last substring is contained, and the 
                                // comparison value does not end with a
                                // wildcard (*), then the attribute value has 
                                // to end with the last substring.
                                if (startPos < realValue.length()) {
                                    res = false;
                                }
                            }
                        }
                    }
                }
            }
            
            return res;
        }
        
        private boolean evalContainsToken(String realValue)
        {
            boolean res;
            if (realValue == null) {  
                // If attribute does not exist, then this is the same  
                // as if attribute does not contain the comparison value.
                res = false;
            } else {
                res = false;
                StringTokenizer st = new StringTokenizer(realValue);
                while (st.hasMoreTokens()) {
                    if (evalEquals(st.nextToken())) {
                        res = true;
                        break;
                    }
                }
            }
            
            return res;
        }
    }

}
