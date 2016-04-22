/*
 * L10nConverter.java
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
package org.docma.app;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

/**
 *
 * @author MP
 */
public class L10nConverter {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception
    {
        getL10nAsProperties(new File("C:\\Arbeit\\tmp\\de.xml"));
    }
    
    public static Properties getL10nAsProperties(File inputFile) throws Exception
    {
        Properties props = new Properties();
        
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        // dbf.setXIncludeAware(true);
        dbf.setValidating(false);
        dbf.setNamespaceAware(true);
        dbf.setExpandEntityReferences(false);
        // dbf.setSchema(null);
        DocumentBuilder dom = dbf.newDocumentBuilder();
        // dom.setEntityResolver(new org.xml.sax.helpers.DefaultHandler());
        // dom.setEntityResolver(new MyEntityResolver());
        // if (dom.isValidating()) System.out.println("validierend!");
        Document document = dom.parse(inputFile);
        NodeList rootlist = document.getElementsByTagNameNS("*", "l10n");
        if (rootlist.getLength() < 1) {
            return props;  // empty properties 
        }
        Node root = rootlist.item(0);
        // System.out.println("Root: " + ((root == null) ? "null" : root.getNodeName()));
        NodeList nlist = root.getChildNodes();
        for (int i=0; i < nlist.getLength(); i++) {
            Node n = nlist.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) n;
                String tagname = e.getLocalName().toLowerCase();
                if ("gentext".equals(tagname)) {
                    String key = e.getAttribute("key");
                    String val = e.getAttribute("text");
                    // System.out.println("Key: " + key + "  Value: " + val);
                    props.put(key, val);
                } else
                if ("context".equals(tagname)) {
                    String ctxname = e.getAttribute("name");
                    NodeList templs = e.getElementsByTagNameNS("*", "template");
                    for (int k=0; k < templs.getLength(); k++) {
                        Node tn = templs.item(k);
                        if (tn.getNodeType() == Node.ELEMENT_NODE) {
                            Element te = (Element) tn;
                            String tname = te.getAttribute("name");
                            String tval = te.getAttribute("text");
                            props.put(ctxname + "|" + tname, tval);
                            // System.out.println("Context: " + ctxname + "|" + 
                            //                    tname + "  Value: " + tval);
                        }
                    }
                }
            }
        }
        return props;
    }
}
