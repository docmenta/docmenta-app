/*
 * ContentRevisionImpl.java
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

package org.docma.coreapi.fsimplementation;

import java.util.*;
import java.io.*;
import org.docma.coreapi.*;


/**
 *
 * @author MP
 */
public class ContentRevisionImpl implements DocContentRevision
{
    private File revisionFile;
    private Date revisionDate;
    private String userId;

    public ContentRevisionImpl(File revisionFile)
    {
        this.revisionFile = revisionFile;
        String fn = revisionFile.getName();
        this.revisionDate = new Date(RevisionStoreSessImpl.getTimeFromFilename(fn));
        this.userId = RevisionStoreSessImpl.getUserIdFromFilename(fn);
    }

    public Date getDate()
    {
        return revisionDate;
    }

    public String getUserId()
    {
        return userId;
    }

    public byte[] getContent()
    {
        if (! revisionFile.exists()) return null;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream((int) revisionFile.length());
            InputStream in = new FileInputStream(revisionFile);
            try {
                byte[] buf = new byte[1024];
                int cnt;
                while ((cnt = in.read(buf)) >= 0) out.write(buf, 0, cnt);
            } finally {
                try { in.close(); } catch (Exception ex) {}
            }
            return out.toByteArray();
        } catch (Exception e) { throw new DocRuntimeException(e); }
    }

    public InputStream getContentStream()
    {
        if (! revisionFile.exists()) return null;
        try {
            return new FileInputStream(revisionFile);
        } catch (FileNotFoundException e) { throw new DocRuntimeException(e); }
    }

    public String getContentString()
    {
        return getContentString("UTF-8");
    }

    public String getContentString(String charsetName)
    {
        if (! revisionFile.exists()) return null;
        try {
            int sz = (int) revisionFile.length();
            FileInputStream fin = new FileInputStream(revisionFile);
            Reader in = new InputStreamReader(fin, charsetName);
            StringWriter out = new StringWriter(2*sz);
            try {
                int cnt;
                char[] buf = new char[1024];
                while ((cnt = in.read(buf)) >= 0) out.write(buf, 0, cnt);
            } finally {
                try { fin.close(); } catch (Exception ex) {}
            }
            return out.toString();
        } catch (Exception e) { throw new DocRuntimeException(e); }
    }

    public long getContentLength()
    {
        if (revisionFile.exists()) {
            return revisionFile.length();
        } else {
            return 0;
        }
    }

}
