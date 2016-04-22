/*
 * PublicationExportThread.java
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
import org.docma.util.*;

/**
 *
 * @author MP
 */
public class PublicationExportThread extends Thread
{
    private DocmaSession docmaSess;
    private String publicationId;

    public PublicationExportThread(DocmaSession tempSess, String publicationId)
    {
        this.docmaSess = tempSess;
        this.publicationId = publicationId;
    }

    public void run()
    {
        try {
            String storeId = docmaSess.getStoreId();
            DocVersionId verId = docmaSess.getVersionId();
            
            // Wait until job is at first position of queue
            int pos;
            boolean first_loop = true;
            while ((pos = docmaSess.getExportJobPosition(storeId, verId, publicationId)) > 0) {
                // Set progress message: Export queued. Please wait...
                if (first_loop) {
                    first_loop = false;
                    DocmaExportJob expjob = docmaSess.getExportJob(storeId, verId, publicationId);
                    DocmaPublication pub = (expjob != null) ? expjob.getPublication() : null;
                    if (pub != null) {
                        pub.setExportProgressMessage("Export queued. Please wait...");
                    }
                }
                try {
                    sleep(250);
                } catch (InterruptedException iex) {  // somebody called DocmaExportJob.cancel() method
                    return;
                }
            }
            DocmaExportJob job = docmaSess.getExportJob(storeId, verId, publicationId);
            if (job != null) {
                // job is at first position of queue (pos == 0)
                job.setState(DocmaExportJob.STATE_RUNNING);
                job.setExportThread(this);
                docmaSess.exportPublication(publicationId);
            } else {
                // job has been removed from queue (pos == -1)
                Log.info("Publication " + publicationId + " has been removed from queue by another thread!");
            }
        } finally {
            docmaSess.closeSession();
        }
    }
}
