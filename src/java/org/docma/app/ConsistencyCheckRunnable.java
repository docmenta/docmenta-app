/*
 * ConsistencyCheckRunnable.java
 * 
 *  Copyright (C) 2017  Manfred Paula, http://www.docmenta.org
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

import java.util.Map;
import org.docma.coreapi.ProgressCallback;

/**
 *
 * @author MP
 */
public class ConsistencyCheckRunnable implements Runnable
{
    private final ProgressCallback progress;
    private final DocmaSession userSess;  // The session of the user that starts the activity.
    private final String[] nodeIds;
    private final Map<Object, Object> props;
    private final boolean allowAutoCorrect;
    
    public ConsistencyCheckRunnable(ProgressCallback progress, 
                                    DocmaSession userSess, 
                                    String[] nodeIds, 
                                    boolean allowAutoCorrect,
                                    Map<Object, Object> props)
    {
        this.progress = progress;
        this.userSess = userSess;
        this.nodeIds = nodeIds;
        this.allowAutoCorrect = allowAutoCorrect;
        this.props = props;
    }

    public void run() 
    {
        DocmaSession docmaSess = null;  // working session
        try {
            // Note that the user session may be closed before the activity is finished.
            // Therefore separate session docmaSess has to be created,
            // which has to be closed after the activity is finished. 
            docmaSess = userSess.createNewSession();
            docmaSess.openDocStore(userSess.getStoreId(), userSess.getVersionId());
            String lang = userSess.getTranslationMode();
            if (lang != null) {
                docmaSess.enterTranslationMode(lang);
            }
            
            EditContentTransformer.checkNodesRecursive(nodeIds, 
                                                       props, 
                                                       allowAutoCorrect, 
                                                       docmaSess, 
                                                       progress);
        } catch (Throwable ex) {
            if (! progress.getCancelFlag()) {
                progress.logError("consistency_check.exception", ex.getMessage());
                ex.printStackTrace();
            }
        } finally {
            try { 
                if (docmaSess != null) docmaSess.closeSession(); 
            } catch (Exception ex1) {}
            progress.setFinished();  // indicate that complete activity is finished
        }
    }
    
}
