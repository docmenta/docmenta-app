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

/**
 *
 * @author MP
 */
public class ContentUtil
{
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
    
    
    public static boolean isReferencingThis(String content, String thisAlias)
    {
        if (content == null) return false;
        // String linkAlias = DocmaAppUtil.getLinkAlias(thisAlias);
        int len = content.length();

        // Find a and img tags
        int startpos = 0;
        while (startpos < len) {
            int tag_start = content.indexOf("<", startpos);
            if (tag_start < 0) break;

            int tag_end = content.indexOf('>', tag_start);
            if (tag_end < 0) break;

            startpos = tag_end + 1;

            // get tag name
            int name_end = tag_start + 1;
            while ((name_end < tag_end) && !Character.isWhitespace(content.charAt(name_end))) {
                name_end++;
            }
            //p2 = content.indexOf(' ', tag_start);
            String tagname = content.substring(tag_start + 1, name_end);

            if (tagname.equals("a")) {
                final String HREF_PATTERN = "href=\"";

                int att_start = content.indexOf(HREF_PATTERN, name_end);
                if ((att_start < 0) || (att_start > tag_end)) continue;
                int val_start = att_start + HREF_PATTERN.length();

                int val_end = content.indexOf('"', val_start);
                if (val_end < 0) continue;

                if (content.charAt(val_start) != '#') continue;

                String href_alias = content.substring(val_start + 1, val_end).trim();
                if (href_alias.equals(thisAlias)) {
                    return true;
                }
            } else
            if (tagname.equals("img")) {
                final String SRC_PATTERN = "src=\"image/";

                int att_start = content.indexOf(SRC_PATTERN, name_end);
                if ((att_start < 0) || (att_start > tag_end)) continue;
                int alias_start = att_start + SRC_PATTERN.length();

                int alias_end = content.indexOf('"', alias_start);
                if (alias_end < 0) continue;

                String src_alias = content.substring(alias_start, alias_end).trim();
                if (src_alias.equals(thisAlias)) {
                    return true;
                }
            }
        }
        
        // Find inclusions
        startpos = 0;
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
            if (refAlias.equals(thisAlias)) {
                return true;
            }
        }
        return false;
    }


    public static DocmaAnchor[] getContentAnchors(String content)
    {
        return getContentAnchors(content, null);
    }
    
    public static DocmaAnchor[] getContentAnchors(String content, DocmaNode sourceNode)
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
