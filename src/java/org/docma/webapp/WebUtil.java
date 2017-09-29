/*
 * WebUtil.java
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

package org.docma.webapp;

import org.docma.util.XMLUtil;

/**
 *
 * @author MP
 */
public class WebUtil 
{
    public static String escapeDoubleQuotedJsString(String value)
    {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
//        int p = 0;
//        while (p < value.length()) {
//            p = value.indexOf('"', p);
//            if (p < 0) {
//                return value;
//            } else {
//                int p1 = p - 1;
//                boolean escaped = false;
//                while (p1 >= 0) {
//                    char ch = value.charAt(p1);
//                    if (ch == '\\') {
//                        escaped = !escaped;
//                        p1--;
//                    } else {
//                        break;
//                    }
//                }
//                if (! escaped) {
//                    // If double quote is not already escaped, then escape it.
//                    value = value.substring(0, p) + "\\" + value.substring(p);
//                    p += 2;
//                } else {
//                    // If double quote is already escaped, then continue search 
//                    // after the double quote.
//                    p++;
//                }
//            }
//        }
//        return value;
    }
    
    public static String escapeDoubleQuotedXMLAttribute(String value) 
    {
        return XMLUtil.escapeDoubleQuotedCDATA(value);
    }
}
