/*
 * WebIndexWriter.java
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

import java.util.*;
import java.text.Collator;

import org.docma.coreapi.ExportLog;

/**
 *
 * @author MP
 */
public class WebIndexWriter
{

    static void writeIndex(Appendable out, 
                           List<ParserIndexEntry> entries, 
                           String langCode, 
                           String textSee,
                           String textSeeAlso,
                           ExportLog exportLog) throws Exception
    {
        out.append("<div class=\"index\">");
        if (langCode == null) {
            langCode = "en";
        }
        Collections.sort(entries, new IndexSorter(new Locale(langCode)));
        
        String division = "";
        int i = 0;
        int max = entries.size();
        boolean div_opened = false;
        while (i < max) {
            ParserIndexEntry entry = entries.get(i);
            String term = entry.getTerm();
            String div_char = readFirstChar(term); // term.substring(0, 1).toUpperCase();
            if (! division.equals(div_char)) {
                division = div_char;
                if (div_opened) {
                    out.append("</div>");  // close previous div
                }
                out.append("<div class=\"indexdiv\">");
                div_opened = true;
                out.append("<h3>").append(div_char).append("</h3>");
            }
            out.append("<dl>");
            // Write 1st level entries (terms without subterm, i.e. subterm1 == null)
            int i_end = getSameEntries(term, null, null, entries, i);
            // Note: (i == i_end) -> no link exists for term -> output header only
            writeIndexLine(out, 0, entries, i, i_end, textSee, textSeeAlso, exportLog);
            i = i_end;
            while ((i < max) && term.equals(entries.get(i).getTerm())) { 
                // Write subterm1 entries (subterm2 == null)
                String subterm1 = entries.get(i).getSubterm1();
                i_end = getSameEntries(term, subterm1, null, entries, i);
                // Note: (i == i_end) -> no link exists for subterm1 -> output header only
                out.append("<dd><dl>");
                writeIndexLine(out, 1, entries, i, i_end, textSee, textSeeAlso, exportLog);
                i = i_end;
                while ((i < max) && 
                       term.equals(entries.get(i).getTerm()) &&
                       subterm1.equals(entries.get(i).getSubterm1())) { 
                    // Write subterm2 entries
                    String subterm2 = entries.get(i).getSubterm2();
                    i_end = getSameEntries(term, subterm1, subterm2, entries, i);
                    out.append("<dd><dl>");
                    writeIndexLine(out, 2, entries, i, i_end, textSee, textSeeAlso, exportLog);
                    out.append("</dl></dd>");
                    i = i_end;
                }
                out.append("</dl></dd>");
            }
            out.append("</dl>");
        }
        if (div_opened) {
            out.append("</div>");  // close previous <div class="indexdiv">
        }
        out.append("</div>");  // close <div class="index">
    }
    
    private static void writeIndexLine(Appendable out, 
                                       int level, 
                                       List<ParserIndexEntry> entries, 
                                       int i_start, 
                                       int i_end, 
                                       String textSee,
                                       String textSeeAlso,
                                       ExportLog exportLog) throws Exception
    {
        ParserIndexEntry entry = entries.get(i_start);
        String term = "";
        switch (level) {
            case 0:
                term = entry.getTerm();
                break;
            case 1:
                term = entry.getSubterm1();
                break;
            case 2:
                term = entry.getSubterm2();
                break;
        }
        out.append("<dt>").append(term);
        
        // Write see entries
        int i = i_start;
        while (i < i_end) {
            entry = entries.get(i);
            if (entry.getType().equals(ParserIndexEntry.TYPE_SEE)) {
                out.append(" (").append(textSee).append(" ").append(entry.getReferencedTitle()).append(")");
                if (i > i_start) {
                    exportLog.warningMsg("Multiple 'see' entries for index term '" + term + "': " + entry.getReferencedTitle());
                }
                i++;
            } else {
                break;
            }
        }
        
        // Write References
        StringBuilder see_also = null;
        String prev_see_also = "";
        boolean first = true;
        while (i < i_end) {
            entry = entries.get(i);
            if (entry.getType().equals(ParserIndexEntry.TYPE_SEE_ALSO)) {
                if (see_also == null) {
                    see_also = new StringBuilder();
                }
                String ref_title = entry.getReferencedTitle();
                if (! ref_title.equalsIgnoreCase(prev_see_also)) {
                    see_also.append("<dt>(").append(textSeeAlso).append(" ").append(ref_title).append(")</dt>");
                    prev_see_also = ref_title;
                }
                i++;
                continue;
            }
            String title = entry.getLinkTitle();
            out.append(first ? ": " : ", ");
            first = false;
            int next_title = i;
            do {
                next_title++;
            } while ((next_title < i_end) && title.equals(entries.get(next_title).getLinkTitle()));
            for (int k = i; k < next_title; k++) {
                entry = entries.get(k);
                if (k > i) {  // not the first loop
                    out.append(", ");
                }
                out.append("<a class=\"indexterm\" href=\"").append(entry.getLinkURL()).append("\">");
                if (entry.isPreferred()) {
                    out.append("<span class=\"index_entry index_entry_preferred\">");
                } else {
                    out.append("<span class=\"index_entry\">");
                }
                if (k == i) {   // first loop
                    out.append(title);
                    if (next_title  > (i + 1)) {  // More than one link for the same target title
                        out.append(" [1]");       // Append link counter
                    }
                } else {
                    out.append("[" + (k - i + 1) + "]");   // write link counter
                }
                out.append("</span>");
                out.append("</a>");
            }
            i = next_title;
        }
        out.append("</dt>");
        if (see_also != null) {
            out.append("<dd><dl>").append(see_also).append("</dl></dd>");
        }
    }

    private static int getSameEntries(String term,
                                      String subterm1,
                                      String subterm2,
                                      List<ParserIndexEntry> entries, 
                                      int start_idx)
    {
        
        int idx = start_idx;
        while (idx < entries.size()) { 
            ParserIndexEntry entry = entries.get(idx);
            String entry_t0 = entry.getTerm();
            String entry_t1 = entry.getSubterm1();
            String entry_t2 = entry.getSubterm2();
            boolean same_terms = 
               term.equals(entry_t0) && 
               ((subterm1 == null) ? (entry_t1 == null) : subterm1.equals(entry_t1)) && 
               ((subterm2 == null) ? (entry_t2 == null) : subterm2.equals(entry_t2)); 
            if (! same_terms) {
                break;
            }
            idx++;
        }
        return idx;
    }

    /**
     * Return the first character of the index term in upper case. 
     * If the first character is encoded as character entity, then return
     * the complete entity notation. The character entity notation is used for
     * the characters &amp;, &lt; and &gt;.
     * @param term The index term.
     * @return The first character of the index term in upper case.
     */
    private static String readFirstChar(String term)
    {
        final int len = term.length();
        if (len == 0) {
            return "";
        }
        String first_char = term.substring(0, 1);
        if (first_char.equals("&")) {
            boolean is_entity = false;
            int pos = 1;
            while (pos < len) {
                char ch = term.charAt(pos);
                if (ch == ';') {
                    is_entity = (pos > 1);
                    break;
                }
                if (Character.isWhitespace(ch)) {
                    break;
                }
                pos++;
            }
            return (is_entity) ? term.substring(0, pos + 1) : "&amp;";
        } else {
            return first_char.toUpperCase();
        }
    }
    
    static class IndexSorter implements Comparator
    {
        private Collator collator;
        
        public IndexSorter(Locale loc)
        {
            collator = Collator.getInstance(loc);
            if (collator == null) {
                collator = Collator.getInstance();
            }
            collator.setStrength(Collator.PRIMARY);
        }
        
        public int compare(Object obj1, Object obj2)
        {
            ParserIndexEntry e1 = (ParserIndexEntry) obj1;
            ParserIndexEntry e2 = (ParserIndexEntry) obj2;
            
            int comp1 = collator.compare(e1.getTerm(), e2.getTerm());
            if (comp1 == 0) {
                int res = compareTerms(e1.getSubterm1(), e2.getSubterm1());
                if (res == 0) {
                    res = compareTerms(e1.getSubterm2(), e2.getSubterm2());
                }
                if (res == 0) {
                    // terms and subterms are equal -> order depends on type (normal, see, see also)
                    String e1type = e1.getType();
                    res = compareTypes(e1type, e2.getType());
                    if ((res == 0) &&
                        (e1type.equals(ParserIndexEntry.TYPE_SEE) || e1type.equals(ParserIndexEntry.TYPE_SEE_ALSO))) {
                            // Sort "see"/"see also" entries by referenced terms 
                            return collator.compare(e1.getReferencedTitle(), e2.getReferencedTitle());
                    }
                    return res;
                } else {
                    return res;
                }
            } else {
                return comp1;
            }
        }
        
        private int compareTerms(String s1, String s2)
        {
            if (s1 != null) {
                return (s2 != null) ? collator.compare(s1, s2) : 1;
            } else {
                return (s2 == null) ? 0 : -1;
            }
        }

        private int compareTypes(String s1, String s2)
        {
            if (s1.equals(s2)) {
               return 0; 
            } else {
                // "See" entries must be listed first.
                // "See also" entries must be listed last.
                if (s1.equals(ParserIndexEntry.TYPE_SEE)) {
                    return -1;  
                } else if (s1.equals(ParserIndexEntry.TYPE_NORMAL)) {
                    return s2.equals(ParserIndexEntry.TYPE_SEE) ? 1 : -1;
                } else {  // s1 equals TYPE_SEE_ALSO
                    return 1;
                }
            }
        }

        public boolean equals(Object obj)
        {
            if (obj instanceof ParserIndexEntry) {
                return compare(this, obj) == 0;
            } else {
                return false;
            }
        }
    }
}
