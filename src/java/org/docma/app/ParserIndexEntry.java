/*
 * ParserIndexEntry.java
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

/**
 *
 * @author MP
 */
public class ParserIndexEntry 
{
    static final String TYPE_NORMAL = "normal";
    static final String TYPE_SEE = "see";
    static final String TYPE_SEE_ALSO = "see_also";

    private String type;
    private String term = null;
    private String subterm1 = null;
    private String subterm2 = null;
    private String linkURL = "";
    private String linkTitle = "";

    private ParserIndexEntry referencedEntry = null;
    private boolean preferred = false;

    public ParserIndexEntry(String type) 
    {
        this.type = type;
    }

    public ParserIndexEntry(String type, String terms) 
    {
        this.type = type;
        if ((terms != null) && (terms.length() > 0)) {
            // terms = terms.replace("&dash;", "-");
            String[] topics = terms.split("--");
            setTerm(normalizeTerm(topics[0]));
            setSubterm1((topics.length > 1) ? normalizeTerm(topics[1]) : null);
            setSubterm2((topics.length > 2) ? normalizeTerm(topics[2]) : null);
        }
    }

    public String getType() 
    {
        return type;
    }

    public void setType(String type) 
    {
        this.type = type;
    }

    public String getTerm() 
    {
        return term;
    }

    public void setTerm(String term) 
    {
        this.term = term;
    }

    public String getSubterm1() 
    {
        return subterm1;
    }

    public void setSubterm1(String subterm1) 
    {
        this.subterm1 = subterm1;
    }

    public String getSubterm2() 
    {
        return subterm2;
    }

    public void setSubterm2(String subterm2) 
    {
        this.subterm2 = subterm2;
    }

    public ParserIndexEntry getReferencedEntry() 
    {
        return referencedEntry;
    }

    public void setReferencedEntry(String referencedTerm) 
    {
        this.referencedEntry = new ParserIndexEntry(TYPE_NORMAL, referencedTerm);
    }

    public String getReferencedTitle() 
    {
        if (referencedEntry == null) {
            return "";
        }
        String ref_sub1 = referencedEntry.getSubterm1();
        String ref_sub2 = referencedEntry.getSubterm2();
        return referencedEntry.getTerm() + 
               ((ref_sub1 == null) ? "" : (" &rarr; " + ref_sub1)) +
               ((ref_sub2 == null) ? "" : (" &rarr; " + ref_sub2));
    }

    public boolean isPreferred() 
    {
        return preferred;
    }

    public void setPreferred(boolean preferred) 
    {
        this.preferred = preferred;
    }

    public String getLinkURL() 
    {
        return linkURL;
    }

    public void setLinkURL(String linkURL) 
    {
        this.linkURL = linkURL;
    }

    public String getLinkTitle() 
    {
        return linkTitle;
    }

    public void setLinkTitle(String linkTitle) 
    {
        this.linkTitle = linkTitle;
    }
    

    private String normalizeTerm(String t) 
    {
        t = t.replace("&nbsp;", " ").replace("&#160;", " ");
        return t.trim();
    }
}
