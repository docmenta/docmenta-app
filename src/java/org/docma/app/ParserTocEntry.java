/*
 *  ParserTocEntry.java
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

import java.io.File;
import java.util.*;
import javax.xml.stream.XMLEventWriter;

/**
 *
 * @author manfred
 */
class ParserTocEntry 
{
    // Main ToC entries
    static final String TYPE_PREFACE = "preface";
    static final String TYPE_PART = "part";
    static final String TYPE_PARTINTRO = "partintro";
    static final String TYPE_CHAPTER = "chapter";
    static final String TYPE_APPENDIX = "appendix";
    static final String TYPE_SECTION = "section";
    static final String TYPE_INDEX = "index";
    
    // Other labeled elements
    static final String TYPE_FIGURE = "figure";
    static final String TYPE_TABLE = "table";
    static final String TYPE_FORMAL = "formal";  // labeled blocks
    static final String TYPE_BLOCK = "block";    // non-formal blocks with id
    static final String TYPE_INLINE = "inline";  // inline elements with id
    static final String TYPE_FOOTNOTE = "footnote";

    private String type = "";
    private String title = "";
    private String labelNumber = "";
    private String alias = null;
    private String formalId = null;
    private String blockName = null;
    private String linkURL = null;  // calculated from alias and chunk
    
    private WebChunk chunk = null;
    private XMLEventWriter xmlWriter = null;
    
    private ParserTocEntry parentEntry = null;
    private ArrayList<ParserTocEntry> subEntries = new ArrayList<ParserTocEntry>();
    private ArrayList<ParserTocEntry> targetElements = new ArrayList<ParserTocEntry>();
    
    ParserTocEntry()
    {
    }

    ParserTocEntry(String entry_type)
    {
        setType(entry_type);
    }

    ParserTocEntry(String entry_type, String alias, String title)
    {
        setType(entry_type);
        setAlias(alias);
        setTitle(title);
    }

    @Override
    public String toString() 
    {
        return "ParserTocEntry{" + "type=" + type + ", title=" + title + ", labelNumber=" + labelNumber + '}';
    }

    public String getType() 
    {
        return type;
    }
    
    public boolean isPart()
    {
        return TYPE_PART.equals(type);
    }

    public boolean isPartIntro()
    {
        return TYPE_PARTINTRO.equals(type);
    }

    public boolean isChapter()
    {
        return TYPE_CHAPTER.equals(type);
    }

    public boolean isAppendix()
    {
        return TYPE_APPENDIX.equals(type);
    }

    public boolean isPreface()
    {
        return TYPE_PREFACE.equals(type);
    }

    public boolean isSection()
    {
        return TYPE_SECTION.equals(type);
    }

    public boolean isFigure()
    {
        return TYPE_FIGURE.equals(type);
    }

    public boolean isTable()
    {
        return TYPE_TABLE.equals(type);
    }

    public boolean isFormal()
    {
        return TYPE_FORMAL.equals(type);
    }

    public void setType(String type) 
    {
        this.type = type;
    }

    public String getTitle() 
    {
        return (title == null) ? "" : title;
    }

    public void setTitle(String title) 
    {
        this.title = title;
    }

    public String getNumberedTitle()
    {
        String lab = getLabelNumber();
        return (lab.length() == 0) ? getTitle() : (lab + ". " + getTitle());
    }
    
    public void setTitleIfNotExists(String title) 
    {
        if ((this.title == null) || this.title.equals("")) {
            this.title = title;
        }
    }

    public String getLabelNumber() 
    {
        return (labelNumber == null) ? "" : labelNumber;
    }

    public void setLabelNumber(String labelNumber) 
    {
        this.labelNumber = labelNumber;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) 
    {
        this.alias = alias;
    }

    public void setAliasIfNotExists(String value) 
    {
        if (value != null) {
            if ((this.alias == null) || this.alias.equals("")) {
                this.alias = value;
            }
        }
    }

    public String getLinkURL() 
    {
        if (this.linkURL == null) {
            WebChunk ch = this.getChunk();
            if (ch != null) {
                this.linkURL = ch.getRelativeURL();
            } else {
                ParserTocEntry e = this;
                while (ch == null) {
                    e = e.getParentEntry();
                    if (e == null) {
                        break;
                    }
                    ch = e.getChunk();
                }
                if (ch != null) {
                    String url = ch.getRelativeURL();
                    if (alias != null) {
                        url += "#" + alias;
                    }
                    this.linkURL = url;
                }
            }
        }
        return this.linkURL;
    }

    public String getBlockName() 
    {
        return (blockName == null) ? "" : blockName;
    }

    public void setBlockName(String blockName) 
    {
        this.blockName = blockName;
    }

    public String getFormalId() 
    {
        return (formalId == null) ? "" : formalId;
    }

    public void setFormalId(String formal_id) 
    {
        this.formalId = formal_id;
    }

    public WebChunk getChunk() 
    {
        return chunk;
    }

    public void setChunk(WebChunk chunk) 
    {
        this.chunk = chunk;
        this.chunk.setTocEntry(this);
    }
    
    public XMLEventWriter getXMLWriter() 
    {
        return xmlWriter;
    }

    public void setXMLChunk(WebChunk chunk, XMLEventWriter xw) 
    {
        this.chunk = chunk;
        this.xmlWriter = xw;
        this.chunk.setTocEntry(this);
    }
    
    public ParserTocEntry getParentEntry() 
    {
        return parentEntry;
    }
    
    public int getSubEntryCount()
    {
        return subEntries.size();
    }

    public ParserTocEntry getSubEntry(int idx) 
    {
        return subEntries.get(idx);
    }

    public int getSubEntryIndex(ParserTocEntry subentry)
    {
        for (int i=0; i < subEntries.size(); i++) {
            if (subEntries.get(i) == subentry) return i;
        }
        return -1;
    }

    public int getSubEntryIndexSameType(ParserTocEntry subentry)
    {
        String entry_type = subentry.getType();
        int idx = 0;
        for (int i=0; i < subEntries.size(); i++) {
            ParserTocEntry sub_i = subEntries.get(i);
            if (sub_i == subentry) return idx;
            if (entry_type.equals(sub_i.getType())) {
                idx++;
            }
        }
        return -1;
    }
    
    public ParserTocEntry[] getSubEntries(String entry_type)
    {
        if (entry_type == null) {  // return all sub entries
            return subEntries.toArray(new ParserTocEntry[subEntries.size()]);
        } else {
            List<ParserTocEntry> res = new ArrayList<ParserTocEntry>();
            for (ParserTocEntry e : subEntries) {
                if (entry_type.equals(e.getType())) {
                    res.add(e);
                }
            }
            return res.toArray(new ParserTocEntry[res.size()]);
        }
    }

    public void addSubEntry(ParserTocEntry entry) 
    {
        this.subEntries.add(entry);
        entry.parentEntry = this;
    }
    
    public int getTargetElementCount()
    {
        return targetElements.size();
    }

    public ParserTocEntry getTargetElement(int idx) 
    {
        return targetElements.get(idx);
    }

    public void addTargetElement(ParserTocEntry entry) 
    {
        this.targetElements.add(entry);
        entry.parentEntry = this;
    }

}
