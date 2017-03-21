/*
 * AutoFormatResolver.java
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

import org.docma.util.*;
import org.docma.plugin.*;
import java.util.*;

/**
 *
 * @author MP
 */
public class AutoFormatResolver
{
    private static final int MAX_RECURSION_DEPTH = 5;
    private static final AutoFormatCall AF_CALL_IMAGE_PREVIEW = new AutoFormatCallImpl("org.docma.app.AutoImagePreview", "");
    private static final AutoFormatCall AF_CALL_FORMAL_PREVIEW = new AutoFormatCallImpl("org.docma.app.AutoFormalPreview", "");

    /**
     * This method is thread safe.
     *
     * @param content
     * @param exportCtx
     * @return
     */
    public static String resolveAutoFormatStyles(String content,
                                                 DocmaExportContext exportCtx) throws Exception
    {
        // Control whether to insert caption lines for images and formal blocks in preview mode
        boolean prevFormatting = exportCtx.isPreviewFormattingEnabled();
        // Control export specific transformations (i.e. transformations required by html2docbook.xsl)
        boolean exportFormatting = exportCtx.isExportFormattingEnabled();
        return resolveAutoFormatStyles(content, exportCtx, null, prevFormatting, exportFormatting, 0, null);
    }

    private static String resolveAutoFormatStyles(String content,
                                                 DocmaExportContext exportCtx,
                                                 StringBuffer workingBuffer,
                                                 boolean doPreviewFormatting,
                                                 boolean doExportFormatting,
                                                 int recursionDepth,
                                                 Set<String> skipStyles) throws Exception
    {
        // Note: This method has to be thread safe!
        AutoFormatParser parser = new AutoFormatParser(content, exportCtx, skipStyles);
        if (! parser.hasNext()) {
            return content;  // no transformation is applied; return original content
        }
        int len = content.length();
        if (workingBuffer == null) {
            workingBuffer = new StringBuffer(len + 4096);
        } else {
            workingBuffer.setLength(0);
        }
        int copypos = 0;
        do {
            AutoFormatBlock block = parser.getNext();
            workingBuffer.append(content, copypos, block.getStartOffset());
            copypos = block.getEndOffset();
            transformBlock(block, workingBuffer, exportCtx, 
                           doPreviewFormatting, doExportFormatting, 
                           recursionDepth, skipStyles);
        } while (parser.hasNext());
        if (copypos < len) {
            workingBuffer.append(content, copypos, len);
        }
        return workingBuffer.toString();
    }

    private static void transformBlock(AutoFormatBlock block,
                                       StringBuffer resultBuf,
                                       DocmaExportContext exportCtx,
                                       boolean doPreviewFormatting,
                                       boolean doExportFormatting,
                                       int recursionDepth,
                                       Set<String> skipStyles) throws Exception
    {
        String clsVal = block.getClassValue();  // may be null, e.g. if img is transformed for preview
        String styleID = null;
        DocmaStyle blockStyle = null;
        if (clsVal != null) {
            if (! clsVal.contains(" ")) {  // class attribute contains only a single style
                styleID = clsVal;
                blockStyle = exportCtx.getStyle(styleID);
            } else {
                // If multiple style names are given, determine the first auto-format style
                StringTokenizer st = new StringTokenizer(clsVal, " ");
                while (st.hasMoreTokens()) {
                    String nm = st.nextToken();
                    DocmaStyle dstyle = exportCtx.getStyle(nm);
                    if ((dstyle != null) && dstyle.hasAutoFormatCall()) {
                        styleID = nm;
                        blockStyle = dstyle;
                        break;
                    }
                }
            }
        }

        // HTML preview specific formattings
        if (doPreviewFormatting) {
            // Add caption lines to images and formal blocks before auto-format
            // transformations are applied
            if (block.isImageWithTitle()) {
                doPreviewTransform(block, AF_CALL_IMAGE_PREVIEW, resultBuf, 
                                   exportCtx, recursionDepth, skipStyles);
                return;
            }
            if ((blockStyle != null) && blockStyle.isFormal()) {
                doPreviewTransform(block, AF_CALL_FORMAL_PREVIEW, resultBuf, 
                                   exportCtx, recursionDepth, skipStyles);
                return;
            }
        }

        // Export specific formattings (i.e. print-instance is processed by html2docbook.xsl)
        if (doExportFormatting) {  // not preview, i.e. it is export mode
            if ((blockStyle != null) && blockStyle.isFormal()) {
                doExportTransformFormal(block, blockStyle.getFormalCall(), resultBuf, 
                                        exportCtx, recursionDepth, skipStyles);
                return;
            }
        }

        // If block is a link with link text replacement, replace link text by target title
        block.transformTargetTitleLink(exportCtx);
        
        // Get user defined auto-format calls
        AutoFormatCall[] calls = (blockStyle != null) ? blockStyle.getAutoFormatCalls(false) : null;
        if ((calls == null) || (calls.length == 0)) {  // no user-defined calls exists
            resultBuf.append(block.getOuterString());  // do not transform content
            return;
        }

        // Apply first auto-format transformation; result is written to new buffer outbuf
        String afclass = calls[0].getClassName();
        if (DocmaConstants.DEBUG) {
            Log.info("Applying " + afclass + " on style " + styleID);
        }
        AutoFormat af_instance = exportCtx.getAutoFormatInstance(afclass);
        block.setAutoFormatCall(calls[0]);
        af_instance.transform(block);
        StringBuffer outbuf = block.getOutputBuffer();
        skipStyles = null;  // Be aware to not reuse the set provided as parameter,
                            // i.e. skipStyles.clear() would be wrong, because the original
                            // set is still needed in follow-up transformBlock() calls.
        if (! block.isStyleRecursion()) {
            // Exclude this style from transformation, in next recursion loop.
            skipStyles = new HashSet();
            skipStyles.add(styleID);
        }

        // If more than one auto-format call is assigned to the style then
        // piping of transformations is required
        if (calls.length > 1) {
            for (int i=1; i < calls.length; i++) {  // piping level
                // outbuf contains result of previous level
                AutoFormatBlock[] splitted = splitRootBlocks(outbuf.toString());
                outbuf.setLength(0);  // re-use output buffer of first block transformation
                afclass = calls[i].getClassName();
                af_instance = exportCtx.getAutoFormatInstance(afclass);
                for (int s=0; s < splitted.length; s++) {
                    AutoFormatBlock split_block = splitted[s];
                    split_block.setAutoFormatCall(calls[i]);
                    af_instance.transform(split_block);
                    outbuf.append(split_block.getOutputBuffer());
                }
                // Result of this level is now in outbuf, which is used as
                // input for next level.
            }
        }

        // Recursive call to correctly resolve nested auto-format blocks
        if (recursionDepth < MAX_RECURSION_DEPTH) {
            String res = resolveAutoFormatStyles(outbuf.toString(), exportCtx, outbuf, 
                                                 doPreviewFormatting, doExportFormatting,
                                                 recursionDepth + 1, skipStyles);
            resultBuf.append(res);
        } else {
            resultBuf.append(outbuf);
        }
    }


    private static void doPreviewTransform(AutoFormatBlock block,
                                           AutoFormatCall call,
                                           StringBuffer resultBuf,
                                           DocmaExportContext exportCtx,
                                           int recursionDepth,
                                           Set<String> skipStyles) throws Exception
    {
        if (DocmaConstants.DEBUG) {
            Log.info("Applying preview transformation" + call.getClassName());
        }
        AutoFormat af_instance = exportCtx.getAutoFormatInstance(call.getClassName());
        block.setAutoFormatCall(call);
        af_instance.transform(block);
        StringBuffer outbuf = block.getOutputBuffer();
        // Recursive call, but this time preview formatting is set to false,
        // otherwise caption line would be inserted multiple times.
        // Export formatting is also set to false, because in preview mode
        // this is not required.
        String res = resolveAutoFormatStyles(outbuf.toString(), exportCtx, outbuf,
                                             false, false, recursionDepth, skipStyles);
        resultBuf.append(res);
    }


    private static void doExportTransformFormal(AutoFormatBlock block,
                                                AutoFormatCall call,
                                                StringBuffer resultBuf,
                                                DocmaExportContext exportCtx,
                                                int recursionDepth,
                                                Set<String> skipStyles) throws Exception
    {
        if (DocmaConstants.DEBUG) {
            Log.info("Applying export transformation" + call.getClassName());
        }
        AutoFormat af_instance = exportCtx.getAutoFormatInstance(call.getClassName());
        block.setAutoFormatCall(call);
        af_instance.transform(block);
        StringBuffer outbuf = block.getOutputBuffer();
        // Recursive call, but this time export formatting is set to false,
        // otherwise enclosing formal div block would be inserted multiple times.
        String res = resolveAutoFormatStyles(outbuf.toString(), exportCtx, outbuf,
                                             false, false, recursionDepth, skipStyles);
        resultBuf.append(res);
    }


    private static AutoFormatBlock[] splitRootBlocks(String input) throws Exception
    {
        XMLParser parser = new XMLParser(input);
        List<AutoFormatBlock> list = new ArrayList<AutoFormatBlock>();
        List attNames = new ArrayList();
        List attValues = new ArrayList();
        int eventType;
        do {
            eventType = parser.next();
            if (eventType == XMLParser.START_ELEMENT) {
                String elemName = parser.getElementName();
                parser.getAttributes(attNames, attValues);
                int outerStart = parser.getStartOffset();
                int outerEnd;
                int innerStart;
                int innerEnd;
                if (parser.isEmptyElement()) {
                    outerEnd = parser.getEndOffset();
                    innerStart = -1;
                    innerEnd = -1;
                } else {
                    innerStart = parser.getEndOffset();
                    parser.readUntilCorrespondingClosingTag();
                    innerEnd = parser.getStartOffset();
                    outerEnd = parser.getEndOffset();
                }
                AutoFormatBlock block = new AutoFormatBlock(input, elemName,
                                                            attNames, attValues,
                                                            outerStart, outerEnd,
                                                            innerStart, innerEnd);
                list.add(block);
            }
        } while (eventType != XMLParser.FINISHED);
        return list.toArray(new AutoFormatBlock[list.size()]);
    }
}
