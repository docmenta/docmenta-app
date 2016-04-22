/*
 * WebChunk.java
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

import java.io.*;
import java.util.*;

/**
 *
 * @author manfred
 */
class WebChunk 
{
    private String relativeFilePath;
    private StringWriter content = new StringWriter();
    private List<ParserTocEntry> footnotes = new ArrayList<ParserTocEntry>();
    private List<ParserIndexEntry> indexEntries = new ArrayList<ParserIndexEntry>();
    private ParserTocEntry tocEntry = null;
    private int anchorCounter = 0;
    
    WebChunk() 
    {
    }
    
    WebChunk(String filepath) 
    {
        this.relativeFilePath = filepath;
    }
    
    String getRelativeFilePath()
    {
        return relativeFilePath;
    }
    
    String getRelativeURL()
    {
        return transformFilePathToURL(relativeFilePath);
    }
    
    Writer getWriter()
    {
        return content;
    }
    
    StringBuffer getContent()
    {
        return content.getBuffer();
    }
    
    int getFootnoteCount()
    {
        return footnotes.size();
    }
    
    void addFootnote(ParserTocEntry entry)
    {
        footnotes.add(entry);
    }
    
    ParserTocEntry getFootnote(int idx)
    {
        return footnotes.get(idx);
    }
    
    ParserTocEntry getFootnoteByAlias(String alias)
    {
        for (ParserTocEntry entry : footnotes) {
            String a = entry.getAlias();
            if ((a != null) && a.equals(alias)) {
                return entry;
            }
        }
        return null;
    }

    int getIndexEntryCount()
    {
        return indexEntries.size();
    }
    
    void addIndexEntry(ParserIndexEntry entry)
    {
        indexEntries.add(entry);
    }
    
    ParserIndexEntry getIndexEntry(int idx)
    {
        return indexEntries.get(idx);
    }
    
    public ParserTocEntry getTocEntry() 
    {
        return tocEntry;
    }

    void setTocEntry(ParserTocEntry tocEntry) 
    {
        this.tocEntry = tocEntry;
    }
    
    String generateAnchorId()
    {
        final String PATT = "00000000";
        String s = Integer.toString(++anchorCounter);
        if (s.length() < PATT.length()) {
            s = PATT.substring(s.length()) + s;
        }
        return "dest-" + s;
    }

    /* -------------  Utility methods  ------------------- */

    private String transformFilePathToURL(String filepath)
    {
        String url = filepath;
        if (File.separatorChar != '/') {
            url = url.replace(File.separatorChar, '/');
        }
        return url;
    }
   
}
