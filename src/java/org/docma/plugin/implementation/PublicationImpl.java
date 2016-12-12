/*
 * PublicationImpl.java
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
package org.docma.plugin.implementation;

import java.io.InputStream;
import java.util.Date;
import org.docma.app.DocmaPublication;
import org.docma.coreapi.DocVersionId;
import org.docma.coreapi.DocmaExportLog;
import org.docma.plugin.DocmaException;
import org.docma.plugin.LogEntries;
import org.docma.plugin.Publication;
import org.docma.plugin.VersionId;

/**
 *
 * @author MP
 */
public class PublicationImpl implements Publication 
{
    private final DocmaPublication docmaPub;

    public PublicationImpl(DocmaPublication docmaPub)
    {
        this.docmaPub = docmaPub;
    }

    public String getId() 
    {
        return docmaPub.getId();
    }

    public String getFilename() throws DocmaException
    {
        try {
            return docmaPub.getFilename();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void setFilename(String fn) throws DocmaException 
    {
        try {
            docmaPub.setFilename(fn);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String getLanguageCode() throws DocmaException
    {
        try {
            return docmaPub.getLanguage();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String getTitle() throws DocmaException
    {
        try {
            return docmaPub.getTitle();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String getFormat() throws DocmaException
    {
        try {
            return docmaPub.getFormat();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String getSubformat() throws DocmaException
    {
        try {
            return docmaPub.getSubformat();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String getPublicationState() throws DocmaException
    {
        try {
            return docmaPub.getPublicationState();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String getPublicationConfigId() throws DocmaException
    {
        try {
            return docmaPub.getPublicationConfigId();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String getOutputConfigId() throws DocmaException
    {
        try {
            return docmaPub.getOutputConfigId();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public VersionId getVersionId() throws DocmaException
    {
        try {
            DocVersionId vid = docmaPub.getVersionId();
            return (vid == null) ? null : VersionIdCreator.create(vid);
       } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public Date getReleaseDate() throws DocmaException
    {
        try {
            return docmaPub.getReleaseDate();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public Date getExportDate() throws DocmaException
    {
        try {
            return docmaPub.getExportDate();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String getExportedByUser() throws DocmaException
    {
        try {
            return docmaPub.getExportedByUser();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String getComment() throws DocmaException
    {
        try {
            return docmaPub.getComment();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public long getContentSize() throws DocmaException
    {
        try {
            return docmaPub.getContentSize();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public InputStream getContentStream() throws DocmaException
    {
        try {
            return docmaPub.getContentStream();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public LogEntries getExportLog() throws DocmaException
    {
        try {
            DocmaExportLog docmaLog = docmaPub.getExportLog();
            return (docmaLog == null) ? null : new LogEntriesImpl(docmaLog);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public boolean isExportFinished() throws DocmaException
    {
        try {
            return docmaPub.isExportFinished();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String getExportProgressMessage() throws DocmaException
    {
        try {
            return docmaPub.getExportProgressMessage();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public int getErrorCount() throws DocmaException
    {
        try {
            return docmaPub.getErrorCount();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public int getWarningCount() throws DocmaException
    {
        try {
            return docmaPub.getWarningCount();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public boolean hasError() throws DocmaException
    {
        try {
            return docmaPub.hasError();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public boolean hasWarning() throws DocmaException
    {
        try {
            return docmaPub.hasWarning();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public boolean isOnline() throws DocmaException
    {
        try {
            return docmaPub.isOnline();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void setOnline(boolean is_online) throws DocmaException
    {
        try {
            docmaPub.setOnline(is_online);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }
    
}
