/*
 * LogUtil.java
 * 
 *  Copyright (C) 2016  Manfred Paula, http://www.docmenta.org
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

import org.docma.coreapi.ExportLog;

/**
 *
 * @author MP
 */
public class LogUtil 
{
    private LogUtil()
    {
    }
    
    public static void addInfo(ExportLog log, DocmaNode nd, String message_key, Object... args)
    {
        if (log != null) {
            log.info(getNodePath(nd), message_key, args);
        }
    }
    
    public static void addWarning(ExportLog log, DocmaNode nd, String message_key, Object... args)
    {
        if (log != null) {
            log.warning(getNodePath(nd), message_key, args);
        }
    }
    
    public static void addError(ExportLog log, DocmaNode nd, String message_key, Object... args)
    {
        if (log != null) {
            log.error(getNodePath(nd), message_key, args);
        }
    }

    public static void addInfo(ExportLog log, 
                               StringBuilder content, 
                               int pos_start, 
                               int pos_end, 
                               String message_key, 
                               Object... args)
    {
        if ((log != null) && (content != null)) {
            log.info(getContentContext(content, pos_start, pos_end), message_key, args);
        }
    }
    
    public static void addWarning(ExportLog log, 
                                  StringBuilder content, 
                                  int pos_start, 
                                  int pos_end, 
                                  String message_key, 
                                  Object... args)
    {
        if ((log != null) && (content != null)) {
            log.warning(getContentContext(content, pos_start, pos_end), message_key, args);
        }
    }
    
    public static void addError(ExportLog log, 
                                StringBuilder content, 
                                int pos_start, 
                                int pos_end, 
                                String message_key, 
                                Object... args)
    {
        if ((log != null) && (content != null)) {
            log.error(getContentContext(content, pos_start, pos_end), message_key, args);
        }
    }
    
    private static String getNodePath(DocmaNode node)
    {
        if (node == null) {
            return "";
        }
        try {
            StringBuilder sb;
            if (node.isFileContent() || node.isImageContent()) {
                sb = new StringBuilder(node.getDefaultFileName());
            } else {
                sb = new StringBuilder(node.getTitle());
            }
            DocmaNode n = node.getParent();
            if (n != null) {
                sb.insert(0, "[" + n.getChildPos(node) + "] ");
            }
            while (n != null) {
                sb.insert(0, n.getTitle() + " / ");
                if (n.isDocumentRoot()) {
                    break;
                }
            }
            return sb.toString();
        } catch (Exception ex) {  // should never occur
            return "[Node path error: " + ex.getLocalizedMessage() + "]";
        }
    }

    private static String getContentContext(StringBuilder content, int cont_start, int cont_end)
    {
        if (cont_start < 0) {
            cont_start = 0;
        }
        if (cont_end < cont_start) {
            cont_end = cont_start;
        }
        
        // Find previous header (h1...h6).
        final String HPATTERN = "<h";
        int pos = cont_start - HPATTERN.length() - 1;
        int hpos = 0;
        boolean header_found = false;
        while (pos >= 0) {
            hpos = content.lastIndexOf(HPATTERN, pos);
            if (hpos < 0) {
                break;
            }
            if (Character.isDigit(content.charAt(hpos + HPATTERN.length()))) {
                header_found = true;
                break;
            }
            pos = hpos - 1;
        }
        
        // Extract header text
        String htxt = null;
        if (header_found) {
            int txtstart = content.indexOf(">", hpos);
            if (txtstart > 0) {
                int txtend = content.indexOf("</h", txtstart);
                if (txtend > 0) {
                    final int MAX_POS = txtstart + 200;
                    if (txtend > MAX_POS) {
                        htxt = content.substring(txtstart + 1, MAX_POS) + "...";
                    } else {
                        htxt = content.substring(txtstart + 1, txtend);
                    }
                }
            }
        }
        
        final int CONTEXT_SIZE = 24;  // Show at least 24 characters before and after the given position
        int ctx_start = Math.max(0, cont_start - CONTEXT_SIZE);
        int ctx_end = Math.min(content.length(), cont_end + CONTEXT_SIZE);
        
        // Search tag boundary within previous 32 characters 
        for (int i = 0; i < 32; i++) {
            if (ctx_start == 0) {
                break;
            }
            char ch = content.charAt(ctx_start);
            // if (ch == '<') {
            //     break;
            // }
            if (ch == '>') {
                ctx_start++;
                break;
            }
            ctx_start--;
        }
        
        // Search end of tag within next 20 characters 
        for (int i = 0; i < 20; i++) {
            if (ctx_end >= content.length()) {
                break;
            }
            char ch = content.charAt(ctx_end++);
            if (ch == '>') {
                break;
            }
        }
        
        // Append header text
        int bufsize = 50 + (ctx_end - ctx_start) + ((htxt == null) ? 0 : htxt.length());
        StringBuilder sb = new StringBuilder(bufsize);
        sb.append("At position ");
        if (htxt != null) {
            sb.append("[").append(htxt).append("] ");
        }
        
        // Append content context
        sb.append("...");
        pos = ctx_start;
        while (pos < ctx_end) {
            char ch = content.charAt(pos++);
            if (Character.isWhitespace(ch)) {
                    sb.append(' ');
            } else {
                    sb.append(ch);
            }
        }
        sb.append("...");
        
        return sb.toString();
    }

}
