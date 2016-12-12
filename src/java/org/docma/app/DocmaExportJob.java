/*
 * DocmaExportJob.java
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

import org.docma.coreapi.*;
import org.docma.plugin.ExportJob;
import org.docma.util.Log;

/**
 *
 * @author MP
 */
public class DocmaExportJob
{
    public static final int STATE_QUEUED = ExportJob.STATE_QUEUED;
    public static final int STATE_RUNNING = ExportJob.STATE_RUNNING;
    public static final int STATE_CANCELED = ExportJob.STATE_CANCELED;
    
    private final String storeId;
    private final DocVersionId versionId;
    private final String publicationId;
    private final DocmaPublication publication;
    private final long creationTime;
    private int state;
    private Thread exportThread = null;

    public DocmaExportJob(String storeId,
                          DocVersionId verId,
                          DocmaPublication pub)
    {
        this.storeId = storeId;
        this.versionId = verId;
        this.publicationId = pub.getId();
        this.publication = pub;
        this.state = STATE_QUEUED;
        this.creationTime = System.currentTimeMillis();
    }

    public int getState()
    {
        return state;
    }

    public void setState(int state)
    {
        this.state = state;
    }

    public String getUserId()
    {
        return publication.getExportedByUser();
    }

    public long getCreationTime()
    {
        return creationTime;
    }

    public String getStoreId()
    {
        return storeId;
    }

    public DocVersionId getVersionId()
    {
        return versionId;
    }

    public String getLanguage()
    {
        return publication.getLanguage();
    }

    public String getPublicationId()
    {
        return publicationId;
    }

    DocmaPublication getPublication()
    {
        return publication;
    }

    Thread getExportThread()
    {
        return exportThread;
    }

    void setExportThread(Thread exportThread)
    {
        this.exportThread = exportThread;
    }

    public void cancel()
    {
        if (exportThread != null) {
            try {
                exportThread.interrupt();
                setState(STATE_CANCELED);
            } catch (Exception ex) {
                Log.error("Cannot cancel export thread: " + ex.getMessage());
            }
        }
    }

}
