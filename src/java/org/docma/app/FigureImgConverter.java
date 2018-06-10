/*
 * FigureImgConverter.java
 * 
 *  Copyright (C) 2018  Manfred Paula, http://www.docmenta.org
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.docma.util.CSSParser;
import org.docma.util.XMLParser;

/**
 *
 * @author MP
 */
public class FigureImgConverter 
{
    public static String imgToFigure(String xml, String figClass) throws Exception
    {
        StringBuilder buf = null;
        int copypos = 0;
        List<String> imgAttNames = null;
        List<String> imgAttValues = null;
        boolean insideFigure = false;
        XMLParser xmlParser = new XMLParser(xml);
        int nextType;
        do {
            nextType = xmlParser.next();
            if (nextType == XMLParser.START_ELEMENT) {
                String elemName = xmlParser.getElementName().toLowerCase();
                
                if (elemName.equals("figure")) {
                    boolean isEmpty = xmlParser.isEmptyElement();
                    insideFigure = !isEmpty;
                    
                } else if (elemName.equals("img")) {
                    if (! insideFigure) {
                        int imgStart = xmlParser.getStartOffset();
                        if (imgAttNames == null) {
                            imgAttNames = new ArrayList<String>();
                            imgAttValues = new ArrayList<String>();
                        }
                        xmlParser.getAttributesLower(imgAttNames, imgAttValues);
                        boolean isEmpty = xmlParser.isEmptyElement();
                        if (! isEmpty) {
                            xmlParser.readUntilCorrespondingClosingTag();
                        }
                        int imgEnd = xmlParser.getEndOffset();  // after img element
                        
                        if (! imgAttNames.contains("title")) {
                            continue;
                        }
                        if (buf == null) {
                            buf = new StringBuilder(xml.length() + 50);
                        }
                        
                        // Replace img by figure
                        buf.append(xml, copypos, imgStart);
                        copypos = imgEnd;  // continue after img element
                        writeFigure(buf, figClass, imgAttNames, imgAttValues);
                    }
                }

            } else if (nextType == XMLParser.END_ELEMENT) {
                String elemName = xmlParser.getElementName().toLowerCase();
                if (elemName.equals("figure")) {
                    insideFigure = false;
                }
            }
        } while (nextType != XMLParser.FINISHED);
        
        if (buf == null) {
            return xml;  // xml is unchanged
        }
        
        if (copypos < xml.length()) {  // copy remaining content
            buf.append(xml, copypos, xml.length());
        }
        return buf.toString();        
    }
    
    public static String figureToImg(String xml) throws Exception
    {
        StringBuilder buf = null;
        int copypos = 0;
        boolean insideFigure = false;
        Map<String, String> figAtts = null;
        Map<String, String> imgAtts = null;
        String figCaption = null;
        int figStart = -1;
        XMLParser xmlParser = new XMLParser(xml);
        int nextType;
        do {
            nextType = xmlParser.next();
            if (nextType == XMLParser.START_ELEMENT) {
                String elemName = xmlParser.getElementName().toLowerCase();
                if (elemName.equals("figure")) {
                    boolean isEmpty = xmlParser.isEmptyElement();
                    if (figAtts == null) {
                        figAtts = new HashMap<String, String>();
                    }
                    xmlParser.getAttributesLower(figAtts);  // lowercase names
                    insideFigure = !isEmpty;
                    figStart = insideFigure ? xmlParser.getStartOffset() : -1; 
                } else if (elemName.equals("figcaption")) {
                    if (insideFigure) { 
                        if  (xmlParser.isEmptyElement()) {
                            figCaption = "";
                        } else {
                            int p1 = xmlParser.getEndOffset();
                            xmlParser.readUntilCorrespondingClosingTag();
                            int p2 = xmlParser.getStartOffset();
                            figCaption = xml.substring(p1, p2);
                        }
                    }
                } else if (elemName.equals("img")) {
                    if (insideFigure) {
                        if (imgAtts == null) {
                            imgAtts = new TreeMap<String, String>();
                        }
                        xmlParser.getAttributesLower(imgAtts);  // lowercase names
                    }
                }

            } else if (nextType == XMLParser.END_ELEMENT) {
                String elemName = xmlParser.getElementName().toLowerCase();
                if (elemName.equals("figure")) {
                    if (insideFigure && (imgAtts != null)) {  // should always be true
                        // Remove figure and figcaption and add attributes to inner img element.
                        figCaption = (figCaption == null) ? "" 
                                     : figCaption.trim().replace("<", "&lt;").replace(">", "&gt;")
                                                        .replace("\"", "&quot;");

                        convertFigureToImgAtts(figCaption, figAtts, imgAtts);
                        if (buf == null) {
                            buf = new StringBuilder(xml.length());
                        }
                        // Replace figure by img element
                        buf.append(xml, copypos, figStart);
                        copypos = xmlParser.getEndOffset();  // after closing figure tag
                        buf.append("<img ");
                        writeAttributes(buf, imgAtts);  // write attributes (sorted by name)
                        buf.append("/>");
                    }
                    insideFigure = false;
                    figAtts = null;
                    imgAtts = null;
                    figCaption = null;
                    figStart = -1;
                }
            }
        } while (nextType != XMLParser.FINISHED);
        
        if (buf == null) {
            return xml;  // xml is unchanged (no figure contained)
        }
        
        if (copypos < xml.length()) {  // copy remaining content
            buf.append(xml, copypos, xml.length());
        }
        return buf.toString();
    }

    private static void convertFigureToImgAtts(String figCaption, 
                                               Map<String, String> figAtts, 
                                               Map<String, String> imgAtts)
    {
        // Add figure id to img (if not already existent)
        if (figAtts.containsKey("id") && !imgAtts.containsKey("id")) {
            imgAtts.put("id", figAtts.get("id"));
        }
        
        // Add figure style to img (if not already existent)
        if (figAtts.containsKey("style")) {
            imgAtts.put("style", mergeStyles(figAtts.get("style"), imgAtts.get("style")));
        }
        // if (figAtts.containsKey("style") && !imgAtts.containsKey("style")) {
        //     imgAtts.put("style", figAtts.get("style"));
        // }
        
        // Add figure class to img 
        String figCls = figAtts.get("class");
        if (figCls != null) {
            String imgCls = imgAtts.get("class");
            imgCls = (imgCls == null) ? figCls : (imgCls + " " + figCls).trim();
            if (! imgCls.equals("")) {
                imgAtts.put("class", imgCls);
            }
        }
        
        // Add figure caption as title attribute (to img element)
        if (! figCaption.equals("")) {
            imgAtts.put("title", figCaption);
        }
        if (! imgAtts.containsKey("title")) {
            imgAtts.put("title", "");
        }
    }
    
    private static String mergeStyles(String figStyle, String imgStyle)
    {
        if (figStyle == null) {
            return imgStyle;
        }
        if (imgStyle == null) {
            return figStyle;
        }
        
        // CSS properties have been defined for the figure and the inner img element
        SortedMap<String, String> figProps = CSSParser.parseCSSProperties(figStyle);
        SortedMap<String, String> imgProps = CSSParser.parseCSSProperties(imgStyle);
        for (String pname : figProps.keySet()) { // CSS properties of the figure element
            // If the same CSS property has already been defined for the inner img  
            // element, then keep the img property.
            if (! imgProps.containsKey(pname)) {
                imgProps.put(pname, figProps.get(pname));
            }
        }
        return CSSParser.propertiesToString(imgProps);
    }
    
    private static void writeFigure(StringBuilder buf, 
                                    String figClass,
                                    List<String> imgAttNames, 
                                    List<String> imgAttValues)
    {
        int idIdx = imgAttNames.indexOf("id");
        int clsIdx = imgAttNames.indexOf("class");
        int styleIdx = imgAttNames.indexOf("style");
        int titleIdx = imgAttNames.indexOf("title");
        
        buf.append("<figure ");
        
        // Move id attribute from image to figure
        if (idIdx >= 0) {
            buf.append("id=\"").append(imgAttValues.get(idIdx)).append("\" ");
            imgAttValues.set(idIdx, null);  // remove id from img element
        }
        
        // Set figure CSS class. Remove CSS class from image, if it is the same
        // same class.
        if ((figClass != null) && !figClass.equals("")) {
            buf.append("class=\"").append(figClass).append("\" ");
            if (clsIdx >= 0) {
                imgAttValues.set(clsIdx, removeCSSClass(imgAttValues.get(clsIdx), figClass));
            }
        }
        
        // Move CSS property float from img to figure.
        if (styleIdx >= 0) {
            // Extract CSS property "float" from img style
            String styleVal = imgAttValues.get(styleIdx);
            SortedMap<String, String> cssprops = CSSParser.parseCSSProperties(styleVal);
            String floatVal = cssprops.remove("float");
            if (floatVal != null) {
                imgAttValues.set(styleIdx, CSSParser.propertiesToString(cssprops));
                buf.append("style=\"float:").append(floatVal).append(";\" ");
            }
        }
        
        buf.append(">");
        buf.append("<img ");
        writeAttributes(buf, imgAttNames, imgAttValues);
        buf.append("/>");
        String title = imgAttValues.get(titleIdx);
        if ((title != null) && !title.equals("")) {
            buf.append("<figcaption>").append(title).append("</figcaption>");
        }
        buf.append("</figure>");
    }
    
    private static String removeCSSClass(String clsValue, String removeCls)
    {
        if ((clsValue == null) || clsValue.equals("") || (removeCls == null)) {
            return clsValue;   // do nothing
        }
        return (" " + clsValue + " ").replace(" " + removeCls + " ", " ").trim();
    }
    
    private static void writeAttributes(StringBuilder buf, Map<String, String> atts) 
    {
        for (String aname : atts.keySet()) {
            String val = atts.get(aname);
            if ((val != null) && !val.equals("")) {
                val = val.replace("\"", "&quot;");
                buf.append(aname).append("=\"").append(val).append("\" ");
            }
        }
    }
    
    private static void writeAttributes(StringBuilder buf, List<String> names, List<String> values) 
    {
        for (int i=0; i < names.size(); i++) {
            String val = values.get(i);
            if (val != null) {
                val = val.replace("\"", "&quot;");
                buf.append(names.get(i)).append("=\"").append(val).append("\" ");
            }
        }
    }

}
