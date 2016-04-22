/*
 * DefaultVersionId.java
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

import org.docma.coreapi.DocVersionId;
import org.docma.coreapi.DocException;
import org.docma.coreapi.DocRuntimeException;

/**
 *
 * @author MP
 */
public class DefaultVersionId implements DocVersionId
{
    private String verStr;

    public DefaultVersionId(String verStr) throws DocException 
    {
        if (isValidVersionId(verStr)) {
            this.verStr = verStr;
        } else {
            throw new DocException("Invalid Version ID '" + verStr + "'");
        }
    }

    public boolean isLowerThan(DocVersionId verId)
    {
        return (compareTo(verId) < 0);
    }

    public boolean isHigherThan(DocVersionId verId)
    {
        return (compareTo(verId) > 0);
    }

    public int compareTo(Object obj) 
    {
        if (equals(obj)) return 0;
        if (isLatest(verStr)) return 1;
        String otherStr = ((DocVersionId) obj).toString();
        if (isLatest(otherStr)) return -1;

        // return verStr.compareToIgnoreCase(otherStr);
        int v_startpos = 0;
        int o_startpos = 0;
        int part_counter = 0;
        while (part_counter < 100) {  // only consider first 100 parts to avoid infinite loop
            boolean v_finished = v_startpos >= verStr.length();
            boolean o_finished = o_startpos >= otherStr.length();
            if (v_finished && o_finished) return 0;  // all parts are equal and same number of parts

            String v_part;
            String o_part;
            if (v_finished) {  // first parts are equal, but otherStr has more parts -> otherStr is higher
                return -1;
            } else {
                // extract next part from verStr
                int v_endpos = verStr.indexOf('.', v_startpos);
                if (v_endpos < 0) v_endpos = verStr.length();
                v_part = verStr.substring(v_startpos, v_endpos);
                v_startpos = v_endpos + 1; // set start position for next loop
            }
            if (o_finished) {  // first parts are equal, but verStr has more parts -> verStr is higher
                return 1;
            } else {
                // extract next part from otherStr
                int o_endpos = otherStr.indexOf('.', o_startpos);
                if (o_endpos < 0) o_endpos = otherStr.length();
                o_part = otherStr.substring(o_startpos, o_endpos);
                o_startpos = o_endpos + 1; // set start position for next loop
            }

            // Now, compare v_part and o_part:

            String[] ve = extractDigitPart(v_part);  // split into leading digits and remaining string
            String[] oe = extractDigitPart(o_part);  // split into leading digits and remaining string

            // compare digit parts
            long v_num = (ve[0].length() > 0) ? Long.parseLong(ve[0]) : -1;  // -1 means no leading digits
            long o_num = (oe[0].length() > 0) ? Long.parseLong(oe[0]) : -1;  // -1 means no leading digits

            if (v_num != o_num) {
                if (v_num == -1) return 1;  // no digit part, but other has digit part -> verStr is higher
                if (o_num == -1) return -1;  // has digit part, but other has not -> verStr is lower
                return (v_num > o_num) ? 1 : -1;
            } else {
                // Digit parts do not exist or are equal -> compare string parts
                int res = ve[1].compareToIgnoreCase(oe[1]);
                if (res != 0) return res;
            }
            part_counter++;
        }
        // return 0;  // all parts are equal -> version identifiers are equal
        throw new DocRuntimeException("Version ID has too many parts!");
    }

    private String[] extractDigitPart(String str)
    {
        String[] res = new String[2];
        // if (str.length() == 0) {
        //     res[0] = "";
        //     res[1] = "";
        // } else {
        int i = 0;
        while ((i < str.length()) && Character.isDigit(str.charAt(i))) i++;
        res[0] = str.substring(0, i);   // leading digits
        res[1] = str.substring(i);      // remaining string part
        // }
        return res;  // if res[0] is empty string, this means str has no leading digits
    }

    public boolean equals(Object obj)
    {
        return verStr.equalsIgnoreCase(((DocVersionId) obj).toString());
    }

    public int hashCode()
    {
        return verStr.hashCode();
    }

    public String toString()
    {
        return verStr;
    }

    public static boolean isValidVersionId(String ver_str)
    {
        if (ver_str.equals(DocmaConstants.DEFAULT_LATEST_VERSION_ID)) {
            return true;
        } else {
            return ver_str.matches("[0-9A-Za-z][0-9A-Za-z_.-]*");
        }
    }

    private static boolean isLatest(String ver_str)
    {
        return ver_str.equalsIgnoreCase(DocmaConstants.DEFAULT_LATEST_VERSION_ID);
    }
}
