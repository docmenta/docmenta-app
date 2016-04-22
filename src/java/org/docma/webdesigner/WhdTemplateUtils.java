/*
 * WhdTemplateUtils.java
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
package org.docma.webdesigner;

import java.util.*;

/**
 *
 * @author manfred
 */
public class WhdTemplateUtils 
{
    public static String replace(String templ, Map<String, String> replacements)
    {
        StringBuilder result = new StringBuilder(templ.length() + 2000);
        int copy_pos = 0;
        int search_pos = 0;
        final int max_pos = templ.length();
        while (search_pos < max_pos) {
            // search start of next placeholder
            int p_start = templ.indexOf("###", search_pos);
            if (p_start < 0) {  // no more placeholder
                break;
            }
            // search end of placeholder
            final int BOUND_LEN = "###".length();
            int p_end = p_start + BOUND_LEN;
            String placeholder = null;
            while (p_end < max_pos) { 
                char ch = templ.charAt(p_end);
                if (Character.isLetterOrDigit(ch) || (ch == '_') || (ch == '-') || (ch == '.')) {
                    p_end++;
                } else {
                    if (templ.regionMatches(p_end, "###", 0, BOUND_LEN)) {
                        placeholder = templ.substring(p_start + BOUND_LEN, p_end);
                        p_end += BOUND_LEN;
                    }
                    break;
                }
            }
            if (placeholder != null) {
                // found placeholder; p_end contains position after placeholder
                String value = replacements.get(placeholder);
                if (value != null) {
                    result.append(templ, copy_pos, p_start);
                    result.append(value);
                    copy_pos = p_end;
                }
            }
            search_pos = p_end;
        }
        // copy remaining characters
        if (copy_pos < templ.length()) {
            result.append(templ, copy_pos, templ.length());
        }
        return result.toString();
    }
}
