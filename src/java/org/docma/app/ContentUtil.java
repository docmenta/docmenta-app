/*
 * ContentUtil.java
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
import org.docma.coreapi.DocRuntimeException;
import org.docma.util.XMLElementContext;
import org.docma.util.XMLElementHandler;
import org.docma.util.XMLParseException;
import org.docma.util.XMLParser;
import org.docma.util.XMLProcessor;
import org.docma.util.XMLProcessorFactory;

/**
 *
 * @author MP
 */
public class ContentUtil
{
    private static final String FILE_PREFIX = FileRefsProcessor.FILE_PREFIX;    // "file/";
    private static final String IMAGE_PREFIX = FileRefsProcessor.IMAGE_PREFIX;  // "image/";


    public static boolean contentIsEqual(String content1, String content2, boolean strict_compare)
    {
        if (strict_compare) {
            if (content1 == null) {
                return (content2 == null);
            } else {
                return content1.equals(content2);
            }
        } else {
            if (content1 == null) {
                return (content2 == null) || content2.trim().equals("");
            }
            if (content2 == null) {
                content2 = "";
            }
            // Ignore all whitespace (e.g. after closing paragraph or at end of content). 
            String c1 = content1.replace("&#160;", "").replace(" ", "").replace("\n", "").replace("\r", "");
            String c2 = content2.replace("&#160;", "").replace(" ", "").replace("\n", "").replace("\r", "");
            return c1.equals(c2);
        }
    }
    
    
    public static boolean isReferencingAlias(String content, String alias)
    {
        if (content == null) { 
            return false;
        }
        // String linkAlias = DocmaAppUtil.getLinkAlias(thisAlias);
        int len = content.length();

        //
        // Find referencing elements (e.g. links, images).
        //
        try {
            // Check if an attribute src/href/poster exists that references the
            // given alias.
            XMLParser parser = new XMLParser(content);
            Map<String, String> atts = new HashMap<String, String>();
            int eventType;
            do {
                eventType = parser.next();
                if (eventType == XMLParser.START_ELEMENT) {
                    parser.getAttributes(atts);
                    if (isRefAtt(atts.get("href"), alias) || 
                        isRefAtt(atts.get("src"), alias) ||
                        isRefAtt(atts.get("poster"), alias)) {
                        return true;
                    }
                }
            } while (eventType != XMLParser.FINISHED);
        } catch (Exception ex) {  
            // Content cannot be parsed.
        }
        
        //
        // Find inclusions.
        //
        int startpos = 0;
        while (startpos < len) {
            int p1 = content.indexOf("[#", startpos);
            if (p1 < 0) {  // no inclusion found
                break;
            }
            p1 += 2;                // p1 is position after [#
            if (p1 >= len) break;   // end of content string reached
            startpos = p1;          // in next loop continue search at p1
            int p2 = content.indexOf("]", startpos);
            if ((p2 < 0) || (p2 - p1 > DocmaConstants.ALIAS_MAX_LENGTH + 1)) {
                continue;
            }
            if (content.charAt(p1) == '#') {  // content inclusion: [##
                p1++;
            }
            String refAlias = content.substring(p1, p2);
            if (refAlias.equals(alias)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRefAtt(String value, String alias)
    {
        if (value == null) {
            return false;
        }        
        value = value.trim();
        
        String ref_alias = null;
        if (value.startsWith("#")) {
            ref_alias = value.substring(1);
        } else if (value.startsWith(IMAGE_PREFIX)) {
            ref_alias = value.substring(IMAGE_PREFIX.length()).trim();
        } else if (value.startsWith(FILE_PREFIX)) {
            ref_alias = value.substring(FILE_PREFIX.length()).trim();
        }
        
        return (ref_alias != null) ? ref_alias.equals(alias) : false;
    }

    public static String replaceAliasInReferences(final String content, 
                                                  final String currentAlias, 
                                                  final String newAlias, 
                                                  final List<String> replacedRefs)
    throws Exception
    {
        if (content == null) { 
            return null;
        }
        
        XMLProcessor xmlproc = XMLProcessorFactory.newInstance();
        xmlproc.setIgnoreAttributeCase(true);
        xmlproc.setIgnoreElementCase(true);
        
        XMLElementHandler linkHandler = new XMLElementHandler() 
        {
            public void processElement(XMLElementContext ctx) 
            {
                String val = ctx.getAttributeValue("href");
                if (val == null) {
                    return;
                }
                if (val.startsWith("#")) {
                    if (val.trim().substring(1).equals(currentAlias)) {
                        if (replacedRefs != null) {
                            replacedRefs.add(ctx.getElement());
                        }
                        ctx.setAttribute("href", "#" + newAlias);
                    }
                } else if (val.startsWith(FILE_PREFIX)) {
                    if (val.trim().substring(FILE_PREFIX.length()).equals(currentAlias)) {
                        if (replacedRefs != null) {
                            replacedRefs.add(ctx.getElement());
                        }
                        ctx.setAttribute("href", FILE_PREFIX + newAlias);
                    }
                }
            }
        };
        
        XMLElementHandler imgHandler = new XMLElementHandler() 
        {
            public void processElement(XMLElementContext ctx) 
            {
                String val = ctx.getAttributeValue("src");
                if (val == null) {
                    return;
                }
                if (val.startsWith(IMAGE_PREFIX)) {
                    if (val.trim().substring(IMAGE_PREFIX.length()).equals(currentAlias)) {
                        if (replacedRefs != null) {
                            replacedRefs.add(ctx.getElement());
                        }
                        ctx.setAttribute("src", IMAGE_PREFIX + newAlias);
                    }
                }
                // else if (val.startsWith(FILE_PREFIX)) {
                //     if (val.trim().substring(FILE_PREFIX.length()).equals(currentAlias)) {
                //         ctx.setAttribute("src", FILE_PREFIX + newAlias);
                //     }
                // }
            }
        };
        
        // Replace a and img tags
        xmlproc.setElementHandler("a", linkHandler);
        xmlproc.setElementHandler("img", imgHandler);
        
        StringBuilder out = new StringBuilder();
        xmlproc.process(content, out);
        
        // Replace inclusions
        int startpos = 0;
        while (startpos < out.length()) {
            int p1 = out.indexOf("[#", startpos);
            if (p1 < 0) {  // no inclusion found
                break;
            }
            int inclusion_start = p1;
            p1 += 2;    // p1 is position after [#
            if (p1 >= out.length()) {
                break;  // end of content string reached
            }
            startpos = p1;          // in next loop continue search at p1
            int p2 = out.indexOf("]", startpos);
            if ((p2 < 0) || (p2 - p1 > DocmaConstants.ALIAS_MAX_LENGTH + 1)) {
                continue;
            }
            if (out.charAt(p1) == '#') {  // content inclusion: [##
                p1++;
            }
            String refAlias = out.substring(p1, p2);
            if (refAlias.equals(currentAlias)) {
                if (replacedRefs != null) {
                    replacedRefs.add(out.substring(inclusion_start, p2 + 1));
                }
                out.replace(p1, p2, newAlias);
                startpos = p1 + newAlias.length() + 1;
            }
        }
        
        return out.toString();
    }

    public static boolean isReferencingStyle(String content, String styleId)
    {
        if (content == null) { 
            return false;
        }
        
        try {
            final String STYLE_ID_PATTERN = " " + styleId + " ";
            XMLParser parser = new XMLParser(content);
            Map<String, String> atts = new HashMap<String, String>();
            int eventType;
            do {
                eventType = parser.next();
                if (eventType == XMLParser.START_ELEMENT) {
                    parser.getAttributes(atts);
                    String cls_value = atts.get("class");
                    if ((cls_value != null) && 
                        (" " + cls_value + " ").contains(STYLE_ID_PATTERN)) {
                        return true;
                    }
                }
            } while (eventType != XMLParser.FINISHED);
            return false;
        } catch (Exception ex) {  // If content cannot be parsed
            return false;
        }
    }

    public static String replaceStyle(final String content, 
                                      final String currentStyle, 
                                      final String newStyle, 
                                      final List<String> replacedElems)
    throws Exception
    {
        if (content == null) {
            return null;
        }
        
        XMLProcessor xmlproc = XMLProcessorFactory.newInstance();
        xmlproc.setIgnoreAttributeCase(true);
        xmlproc.setIgnoreElementCase(true);
        
        XMLElementHandler elemHandler = new XMLElementHandler() 
        {
            public void processElement(XMLElementContext ctx) 
            {
                final String STYLE_PATTERN = " " + currentStyle + " ";
                String val = ctx.getAttributeValue("class");
                if (val == null) {
                    return;
                }
                String oldVal = val.trim();
                val = " " + oldVal + " ";
                if ((val).contains(STYLE_PATTERN)) {
                    if (replacedElems != null) {
                        replacedElems.add(ctx.getElement());
                    }
                    String newVal = val.replace(STYLE_PATTERN, " " + newStyle + " ").trim();
                    if (! newVal.equals(oldVal)) {
                        ctx.setAttribute("class", newVal);
                    }
                }
            }
        };
        
        xmlproc.setElementHandler(elemHandler);
        
        StringBuilder out = new StringBuilder();
        xmlproc.process(content, out);
        return out.toString();
    }
    
    public static DocmaAnchor[] getContentAnchors(String content)
    {
        return getContentAnchors(content, null);
    }
    
    public static DocmaAnchor[] getContentAnchors(String content, DocmaNode sourceNode)
    {
        DocmaAnchor[] res = parseWellFormedXML(content);
        for (DocmaAnchor anch : res) {
            anch.setNode(sourceNode);
        }
        return res;
    }
    
    public static DocmaAnchor[] parseWellFormedXML(String content)
    {
        // To do: Replace XMLParser by SimpleXMLProcessor, because 
        //        invocations of XMLParser.readUntilCorrespondingClosingTag()
        //        does not check if XML is well-formed.
        
        if ((content == null) || content.equals("")) {
            return new DocmaAnchor[0];
        }
        List<DocmaAnchor> res = new ArrayList<DocmaAnchor>();
        Deque<String> openElements = new ArrayDeque<String>();
        try {
            XMLParser parser = new XMLParser(content);
            Map<String, String> atts = new HashMap<String, String>();
                        
            int eventType;
            do {
                eventType = parser.next();
                if (eventType == XMLParser.START_ELEMENT) {
                    String elemName = parser.getElementName();
                    boolean isEmpty = parser.isEmptyElement();
                    if (! isEmpty) {
                        openElements.addLast(elemName);
                    }
                    parser.getAttributes(atts);
                    
                    String id_value = atts.get("id");
                    if ((id_value == null) || !id_value.matches(DocmaConstants.REGEXP_ID)) {
                        continue;   // skip invalid id values
                    }
                    
                    int tagStart = parser.getStartOffset();
                    
                    String title = atts.get("title");
                    if (elemName.equals("span") || (elemName.equals("cite"))) {
                        if ((title == null) && !isEmpty) {
                            // If span/cite has no title attribute, then the 
                            // use the content of the element as title.
                            int innerStart = parser.getEndOffset();
                            parser.readUntilCorrespondingClosingTag();
                            openElements.pollLast();
                            int innerEnd = parser.getStartOffset();
                            title = content.substring(innerStart, innerEnd);
                        }
                    } else if (elemName.equals("table")) {
                        if ((title == null) && !isEmpty) {
                            // Extract caption element.
                            // This is a quick and dirty implementation.
                            // To do: replace by real XML parsing.
                            int tab_start = parser.getEndOffset();
                            parser.readUntilCorrespondingClosingTag();
                            openElements.pollLast();
                            int tab_end = parser.getStartOffset();
                            int p1 = content.indexOf("<caption>", tab_start);
                            if ((p1 >= 0) && (p1 < tab_end)) {  // if this table contains a caption
                                p1 += "<caption>".length();
                                int p2 = content.indexOf("</caption>", p1);
                                if ((p2 >= 0) && (p2 < tab_end)) { 
                                    title = content.substring(p1, p2);
                                }
                            }
                        }
                    }
                    
                    if (title == null) title = "";

                    DocmaAnchor anchor = new DocmaAnchor();
                    anchor.setAlias(id_value);
                    anchor.setTitle(title);
                    anchor.setTagPosition(tagStart);
                    res.add(anchor);
                } else if (eventType == XMLParser.END_ELEMENT) {
                    String elemName = parser.getElementName();
                    String openName = openElements.pollLast();
                    if ((openName == null) || !openName.equals(elemName)) {
                        throw new XMLParseException("Closing tag '" + elemName + 
                                                    "' has no matching opening tag!");
                    }
                }
            } while (eventType != XMLParser.FINISHED);
            if (! openElements.isEmpty()) {
                throw new XMLParseException("Opening tag '" + openElements.getLast() + 
                                            "' has no matching closing tag!");
            }
        } catch (XMLParseException xpe) {
            throw new DocRuntimeException(xpe);
        } catch (DocRuntimeException dre) {
            throw dre;
        } catch (Exception ex) {
            throw new DocRuntimeException(ex);
        }
        return res.toArray(new DocmaAnchor[res.size()]);
    }
    
    private static DocmaAnchor[] getContentAnchors_old(String content, DocmaNode sourceNode)
    {
        if (content == null) {
            return new DocmaAnchor[0];
        }
        final String ID_PATTERN = " id=\"";
        final String TITLE_PATTERN = " title=\"";

        ArrayList list = new ArrayList();
        int startpos = 0;
        int len = content.length();
        while (startpos < len) {
            int pos = content.indexOf(ID_PATTERN, startpos);
            if (pos < 0) break;

            startpos = pos + 1;  // continue search after this position

            // search start of tag
            int idx = pos;
            boolean valid = false;
            while (idx > 0) {
                idx--;
                if (content.charAt(idx) == '<') {
                    valid = true;
                    break;
                }
                else
                if (content.charAt(idx) == '>') {
                    break;
                }
            }
            if (! valid) continue;
            int tag_start = idx;

            // search end of tag
            int tag_end = content.indexOf('>', pos);
            if (tag_end < 0) break;  // should never occur if content is well formed xml

            // get value of id attribute
            int p1 = pos + ID_PATTERN.length();
            int p2 = content.indexOf('"', p1);
            if (p2 < 0) break;  // should never occur if content is well formed xml
            String id_value = content.substring(p1, p2);
            if (! id_value.matches(DocmaConstants.REGEXP_ID)) {
                continue;   // skip invalid id values
            }

            // get value of title attribute if existent
            String title = null;
            pos = content.indexOf(TITLE_PATTERN, tag_start);
            if ((pos > tag_start) && (pos < tag_end)) {
                p1 = pos + TITLE_PATTERN.length();
                p2 = content.indexOf('"', p1);
                if ((p2 > p1) && (p2 < tag_end)) {
                    title = content.substring(p1, p2);
                }
            }

            // get tag name
            p2 = tag_start + 1;
            while ((p2 < tag_end) && !Character.isWhitespace(content.charAt(p2))) p2++;
            //p2 = content.indexOf(' ', tag_start);
            String tagname = content.substring(tag_start + 1, p2);

            if (tagname.equals("span")) {
                if (title == null) {
                    // title is content of span element
                    p2 = content.indexOf("</span>", tag_end);
                    if (p2 < 0) continue;  // should not occur
                    title = content.substring(tag_end + 1, p2);
                }
            } else
            if (tagname.equals("cite")) {
                if (title == null) {
                    // title is content of cite element
                    p2 = content.indexOf("</cite>", tag_end);
                    if (p2 < 0) continue;  // should not occur
                    title = content.substring(tag_end + 1, p2);
                }
            } else
            if (tagname.equals("table")) {
                if (title == null) {
                    int table_end = content.indexOf("</table>", tag_end);
                    if (table_end < 0) continue;  // should not occur
                    p1 = content.indexOf("<caption>", tag_end);
                    if ((p1 >= 0) && (p1 < table_end)) {  // if this table contains a caption
                        p1 += "<caption>".length();
                        p2 = content.indexOf("</caption>", p1);
                        if ((p2 < 0) || (p2 > table_end)) continue;  // should not occur
                        title = content.substring(p1, p2);
                    }
                }
            } // else
            // if (tagname.equals("img")) {
            // } else
            // if (tagname.equals("div")) {
            // }

            if (title == null) title = "";

            DocmaAnchor anchor = new DocmaAnchor();
            anchor.setAlias(id_value);
            anchor.setTitle(title);
            anchor.setTagPosition(tag_start);
            anchor.setNode(sourceNode);
            list.add(anchor);
        }
        DocmaAnchor[] arr = new DocmaAnchor[list.size()];
        arr = (DocmaAnchor[]) list.toArray(arr);
        return arr;
    }


    public static void getIdValues(String content, Set id_values)
    {
        if (content == null) {
            return;
        }
        try {
            // Collect values of all id attributes.
            XMLParser parser = new XMLParser(content);
            Map<String, String> atts = new HashMap<String, String>();
            int eventType;
            do {
                eventType = parser.next();
                if (eventType == XMLParser.START_ELEMENT) {
                    parser.getAttributes(atts);
                    String val = atts.get("id");
                    if (val == null) {
                        val = atts.get("ID");
                    }
                    if ((val != null) && !val.equals("")) {
                        id_values.add(val);
                    }
                }
            } while (eventType != XMLParser.FINISHED);
        } catch (Exception ex) {
            // Content cannot be parsed.
        }
    }
 
    /**
     * Quick and dirty implementation. Should be replaced by implementation
     * given above.
     * 
     * @param content
     * @param id_values 
     */
    public static void getIdValues(StringBuilder content, Set id_values)
    {
        if (content == null) {
            return;
        }
        final String ID_PATTERN = " id=\"";

        int startpos = 0;
        int len = content.length();
        while (startpos < len) {
            int pos = content.indexOf(ID_PATTERN, startpos);
            if (pos < 0) break;

            startpos = pos + 1;  // continue search after this position

            // search start of tag
            int idx = pos;
            boolean valid = false;
            while (idx > 0) {
                idx--;
                if (content.charAt(idx) == '<') {
                    valid = true;
                    break;
                }
                else
                if (content.charAt(idx) == '>') {
                    break;
                }
            }
            if (! valid) continue;
            // int tag_start = idx;

            // search end of tag
            // int tag_end = content.indexOf('>', pos);
            // if (tag_end < 0) break;  // should never occur if content is well formed xml

            // get value of id attribute
            int p1 = pos + ID_PATTERN.length();
            int p2 = content.indexOf("\"", p1);
            if (p2 < 0) continue;  // should never occur if content is well formed xml
            String id_val = content.substring(p1, p2);
            // if (! id_value.matches(DocmaConstants.REGEXP_ID)) {
            //     continue;   // skip invalid id values
            // }

            id_values.add(id_val);
        }
    }

}
