/*
 * ExportJobImpl.java
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

import org.docma.app.DocmaExportJob;
import org.docma.plugin.ExportJob;
import org.docma.plugin.VersionId;

/**
 *
 * @author MP
 */
public class ExportJobImpl implements ExportJob
{
    private final DocmaExportJob docmaJob;

    public ExportJobImpl(DocmaExportJob docmaJob) 
    {
        this.docmaJob = docmaJob;
    }

    public int getState() 
    {
        return docmaJob.getState();
    }

    public String getUserId() 
    {
        return docmaJob.getUserId();
    }

    public long getCreationTime() 
    {
        return docmaJob.getCreationTime();
    }

    public String getStoreId() 
    {
        return docmaJob.getStoreId();
    }

    public VersionId getVersionId() 
    {
        return VersionIdCreator.create(docmaJob.getVersionId());
    }

    public String getLanguageCode() 
    {
        return docmaJob.getLanguage();
    }

    public String getPublicationId() 
    {
        return docmaJob.getPublicationId();
    }

    public void cancel() 
    {
        docmaJob.cancel();
    }
    
}
