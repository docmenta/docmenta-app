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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.docma.coreapi.ProgressCallback;
import org.docma.plugin.LogEntries;
import org.docma.plugin.LogEntry;

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

    private final StringBuilder contentBuffer = new StringBuilder();   
    private int skipped = 0;
    
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
        skipped = 0;
        
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
            
            List<DocmaNode> nodes = new ArrayList<DocmaNode>(nodeIds.length);
            for (String nId : nodeIds) {
                DocmaNode nd = docmaSess.getNodeById(nId);
                if (nd != null) {
                    nodes.add(nd);
                } else {
                    progress.logWarning("consistency_check.node_id_not_found", nId);
                }
            }
            checkRecursive(docmaSess, nodes.toArray(new DocmaNode[nodes.size()]), 1, null);
            if (skipped > 0) {
                progress.logHeader(1, "consistency_check.skipped_non_html_count", skipped);
            }
            Integer err_cnt = progress.getErrorCount();
            Integer warn_cnt = progress.getWarningCount();
            progress.logHeader(1, "consistency_check.finished_summary", err_cnt, warn_cnt);
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
    
    private void checkRecursive(DocmaSession docmaSess, DocmaNode[] nodes, int depth, String parentPath)
    throws Exception
    {
        progress.startWork(nodes.length);
        try {
            for (DocmaNode nd : nodes) {
                if (progress.getCancelFlag()) {
                    progress.logHeader(1, "consistency_check.canceled_by_user");
                    throw new Exception("Consistency check canceled by user.");
                }
                try {
                    String nodePath = getNodePath(parentPath, nd);
                    if (nd.isSection()) {
                        DocmaNode[] children = nd.getChildren();
                        if ((children != null) && (children.length > 0)) {
                            checkRecursive(docmaSess, children, depth + 1, nodePath);
                        }
                    } else if (nd.isHTMLContent()) {
                        progress.logHeader(1, "consistency_check.entering_html_node", nodePath);
                        String cont = nd.getContentString();
                        contentBuffer.setLength(0);   // clear buffer
                        contentBuffer.append(cont);
                        LogEntries res = 
                            EditContentTransformer.checkHTML(contentBuffer, nd.getId(), props, 
                                                             allowAutoCorrect, docmaSess);

                        // Transfer log entries to global log
                        for (LogEntry entry : res.getLog()) {
                            progress.log(entry);
                        }

                        if (allowAutoCorrect) {
                            String updated = contentBuffer.toString();
                            if (! cont.equals(updated)) {
                                nd.makeRevision();
                                nd.setContentString(updated);
                                progress.logInfo("consistency_check.node_updated", nodePath);
                            }
                        }
                    } else {
                        skipped++;
                    }
                } catch (Exception ex) {
                    progress.logError("consistency_check.exception", ex.getMessage());
                } finally {
                    progress.stepFinished();
                }
            }
        } finally {
            progress.finishWork();
        }
    }
    
    private String getNodePath(String parentPath, DocmaNode node)
    {
        DocmaNode parent = node.getParent();
        if (parentPath == null) {
            if ((parent == null) || node.isDocumentRoot() || node.isRoot()) {
                return "";
            }
            parentPath = getNodePath(null, parent);
        }
        String prefix;
        if ((parent != null) && (parentPath.length() > 0)) {
            if (node.isSection()) {
                prefix = parentPath + " / ";
            } else {
                int pos = parent.getChildPos(node);
                prefix = parentPath + " /[" + pos + "] "; 
            }
        } else {
            prefix = "";
        }
        String title = node.getTitle();
        if ((title == null) || title.equals("")) {
            return prefix + node.getId();
        } else {
            return prefix + title;
        }
    }
}
