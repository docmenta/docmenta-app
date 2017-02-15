/*
 * AutoFormatParser.java
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
import org.docma.util.XMLParser;

/**
 *
 * @author MP
 */
public class AutoFormatParser
{
    private String input;
    private XMLParser xmlParser;
    private DocmaExportContext exportCtx;
    private Set<String> skipStyles;
    private DocmaSession docmaSess;
    private DocmaOutputConfig outConfig;
    private AutoFormatBlock nextBlock = null;
    private boolean nextPending;

    private List<String> nextAttNames = null;
    private List<String> nextAttValues = null;


    public AutoFormatParser(String input, 
                            DocmaExportContext exportCtx,
                            Set<String> skipStyles) throws Exception
    {
        this.input = input;
        this.xmlParser = new XMLParser(input, 0);
        this.exportCtx = exportCtx;
        this.skipStyles = skipStyles;
        this.docmaSess = exportCtx.getDocmaSession();
        this.outConfig = exportCtx.getOutputConfig();
        this.nextPending = true;
    }

    /* --------------  Public methods  ---------------------- */

    public boolean hasNext() throws Exception
    {
        if (nextPending) {
            readNext();
            nextPending = false;
        }
        return (nextBlock != null);
    }

    public AutoFormatBlock getNext() throws Exception
    {
        if (nextPending) {
            readNext();
        }
        nextPending = true;
        return nextBlock;
    }

    /* --------------  Private methods  ---------------------- */


    private void readNext() throws Exception
    {
        nextBlock = null;
        int nextType;
        do {
            nextType = xmlParser.next();
            if (nextType == XMLParser.START_ELEMENT) {
                String elemName = xmlParser.getElementName();
                initAttLists();
                xmlParser.getAttributes(nextAttNames, nextAttValues);
                int cls_idx = nextAttNames.indexOf("class");
                // boolean is_formal = false;
                boolean isLink = elemName.equalsIgnoreCase("a") && nextAttNames.contains("href");
                boolean is_autoformat_style = isLink;  // links have to be auto formatted
                if ((! isLink) && (cls_idx >= 0)) {    // if element is no link but has class attribute
                    StringTokenizer st = new StringTokenizer(nextAttValues.get(cls_idx), " ");
                    while (st.hasMoreTokens()) {
                        String clsName = st.nextToken();
                        DocmaStyle style = exportCtx.getStyle(clsName);
                        if (style != null) {
                            boolean skip = (skipStyles != null) && skipStyles.contains(clsName);
                            if ((!skip) && style.hasAutoFormatCall()) {
                                is_autoformat_style = true;
                                break;
                            }
                            // is_formal = style.isFormal();
                        }
                    }
                }
                boolean is_af;
                if (exportCtx.isPreviewFormattingEnabled()) {
                    // If preview formatting is enabled then images with title
                    // and formal blocks have to be auto-formatted, i.e. a caption line
                    // showing the title will automatically be added (for HTML preview)
                    is_af = is_autoformat_style || // is_formal ||
                            (elemName.equalsIgnoreCase("img") && nextAttNames.contains("title"));
                } else {
                    is_af = is_autoformat_style;
                }
                if (is_af) {  // this block has to be auto-formatted
                    boolean isEmpty = xmlParser.isEmptyElement();
                    int outerStart = xmlParser.getStartOffset();
                    int outerEnd;
                    int innerStart;
                    int innerEnd;
                    if (isEmpty) {
                        outerEnd = xmlParser.getEndOffset();
                        innerStart = -1;
                        innerEnd = -1;
                    } else {
                        innerStart = xmlParser.getEndOffset();
                        xmlParser.readUntilCorrespondingClosingTag();
                        innerEnd = xmlParser.getStartOffset();
                        outerEnd = xmlParser.getEndOffset();
                    }
                    nextBlock = new AutoFormatBlock(input, elemName, 
                                                    nextAttNames, nextAttValues, 
                                                    outerStart, outerEnd,
                                                    innerStart, innerEnd);
                    break;
                }
            }
        } while (nextType != XMLParser.FINISHED);
    }

    private void initAttLists()
    {
        if (nextAttNames == null) {
            nextAttNames = new ArrayList<String>();
            nextAttValues = new ArrayList<String>();
        }
    }

//    private static synchronized XMLStreamReader getStreamReader(Reader input) throws Exception
//    {
//        if (factory == null) {
//            factory = XMLInputFactory.newInstance();
//            factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);
//        }
//        return factory.createXMLStreamReader(input);
//    }

}
