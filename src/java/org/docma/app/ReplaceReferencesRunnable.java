/*
 * ReplaceReferencesRunnable.java
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
import java.util.SortedMap;
import java.util.TreeMap;
import org.docma.coreapi.DocI18n;
import org.docma.coreapi.ProgressCallback;

/**
 *
 * @author MP
 */
public class ReplaceReferencesRunnable implements Runnable
{
    private final ProgressCallback progress;
    private final DocmaSession userSess;  // The session of the user that starts the activity.
    private final String[] nodeIds;
    private final String oldAlias;
    private final String newAlias;
    private final boolean processTranslations;
    
    private DocmaSession workSess = null;
    private final SortedMap<String, Integer> replacedStats = new TreeMap<String, Integer>();

    public ReplaceReferencesRunnable(ProgressCallback progress, 
                                     DocmaSession userSess, 
                                     String[] nodeIds, 
                                     String oldAlias, 
                                     String newAlias, 
                                     boolean processTranslations)
    {
        this.progress = progress;
        this.userSess = userSess;
        this.nodeIds = nodeIds;
        this.oldAlias = oldAlias;
        this.newAlias = newAlias;
        this.processTranslations = processTranslations;
    }

    public void run() 
    {
        workSess = null;  // working session
        try {
            // Note that the user session may be closed before the activity is finished.
            // Therefore separate session workSess has to be created,
            // which has to be closed after the activity is finished. 
            workSess = userSess.createNewSession();
            workSess.openDocStore(userSess.getStoreId(), userSess.getVersionId());
            String lang = userSess.getTranslationMode();
            if (lang != null) {
                workSess.enterTranslationMode(lang);
            }
            
            // Replace references using workSess
            replaceRefs();
        } catch (Throwable ex) {
            if (! progress.getCancelFlag()) {
                progress.logError("error.exception", ex.getMessage());
                ex.printStackTrace();
            }
        } finally {
            try { 
                if (workSess != null) workSess.closeSession(); 
            } catch (Exception ex1) {}
            progress.setFinished();  // indicate that complete activity is finished
        }
    }
    
    private void replaceRefs() throws Exception
    {
        DocI18n i18n = workSess.getI18n();
        List<DocmaNode> nodes = getNodes();
        String desc = "Replacing target alias '" + oldAlias + 
                      "' by '" + newAlias + "'.";
        // progress.logHeader(3, desc);
        // progress.logText(desc, null);
        progress.logText(i18n.getLabel("replace_references.old_alias"), oldAlias);
        progress.logText(i18n.getLabel("replace_references.new_alias"), newAlias);
        boolean isOriginalMode = (workSess.getTranslationMode() == null);
        
        Integer nodeCnt = 0;
        Integer errorCnt = 0;
        // replacedStats.clear();
        progress.startWork(nodes.size());
        try {
            List<String> replacedRefs = new ArrayList<String>();  // helper list
            for (DocmaNode nd : nodes) {
                if (progress.getCancelFlag()) {
                    String cancelMsg = i18n.getLabel("activity.canceled_by_user");
                    progress.logText(cancelMsg, null);
                    throw new CanceledByUserException("Replacement of references has been canceled by the user.");
                }
                boolean replaced = false;
                try {
                    String path = DocmaAppUtil.getNodePathAsText(nd, 500);
                    String pathMsg = path; //  i18n.getLabel("findnodes.entering_node", path);
                    progress.logHeader(5, pathMsg);
                    if (nd.isHTMLContent()) {
                        if (replaceRefsInContent(nd, replacedRefs, i18n)) {
                            replaced = true;
                        }
                        // Note: If in the future title inclusion will be 
                        //       supported within content titles, then these  
                        //       inclusions have to be replaced as well.
                        //       Same is for section titles.
                        
                        // Now, replace references in translated content
                        if (isOriginalMode && processTranslations) {
                            String[] langs = nd.listTranslations();
                            if (langs.length > 0) {
                                for (String lang : langs) {
                                    progress.logHeader(6, "replace_references.replacing_translation", lang);
                                    try {
                                        if (nd.isContentTranslated(lang)) {
                                            workSess.enterTranslationMode(lang);
                                            if (replaceRefsInContent(nd, replacedRefs, i18n)) {
                                                replaced = true;
                                            }
                                        }
                                    } catch (Exception ex)  {
                                        errorCnt++;
                                        progress.logError(ex.getMessage());
                                    }
                                }
                                
                                // If workSess is in translation mode, go back to original mode
                                workSess.leaveTranslationMode();
                            }
                        }
                    } else if (nd.isReference()) {
                        String refAlias = nd.getReferenceTarget();
                        if (oldAlias.equals(refAlias)) {
                            // Note: Setting oldAlias equal to newAlias can be 
                            // used to simulate the replacement, i.e. creating
                            // the log entries but not performing the update 
                            // of the nodes.
                            if (! oldAlias.equals(newAlias)) {
                                nd.setReferenceTarget(newAlias);
                                replaced = true;
                                increaseStats(nd.getTranslationMode(), 1);
                            }
                            String msg = i18n.getLabel("replace_references.found_reference_node", refAlias);
                            progress.logText(null, msg);
                        }
                    } 
                    // else if (nd.isSection()) {
                    //     Handle title inclusion
                    // }
                } catch (Exception ex) {
                    errorCnt++;
                    progress.logError(ex.getMessage());
                } finally {
                    progress.stepFinished();
                }
                if (replaced) {
                    nodeCnt++;
                }
            }  // for-loop
        } finally {
            progress.finishWork();
        }
        
        if (processTranslations && !replacedStats.isEmpty()) {
            // Write Statistics per Language
            progress.logHeader(4, "replace_references.language_summary_header");
            writeSummaryPerLanguage();
        }
        
        if (errorCnt > 0) {
            progress.logHeader(4, "log_statistics.error_count", errorCnt);
        }
        
        Integer total = 0;
        for (Integer cnt : replacedStats.values()) {
            total += cnt;
        }
        // String finished = i18n.getLabel("replace_references.finished_summary", total, nodeCnt);
        // progress.logText(finished, null);
        progress.logHeader(4, "replace_references.finished_summary", total, oldAlias, nodeCnt); 
    }
    
    private boolean replaceRefsInContent(DocmaNode nd, 
                                         List<String> replacedRefs, 
                                         DocI18n i18n) throws Exception
    {
        replacedRefs.clear();
        boolean replaced = false;
        try {
            // Note: Setting oldAlias equal to newAlias can be used to 
            // simulate the replacement, i.e. creating the log entries but not 
            // performing the update of the nodes 
            // (because new_content.equals(old_content) evaluates to true).
            String old_content = nd.getContentString();
            String new_content = 
                ContentUtil.replaceAliasInReferences(old_content, 
                                                     oldAlias, 
                                                     newAlias, 
                                                     replacedRefs);
            if ((new_content != null) && !new_content.equals(old_content)) {
                nd.makeRevision();
                nd.setContentString(new_content);
                replaced = true;
                increaseStats(nd.getTranslationMode(), replacedRefs.size());
            }
        } finally {
            if (! replacedRefs.isEmpty()) {
                StringBuilder str = new StringBuilder();
                for (String ref : replacedRefs) {
                    str.append(ref).append("\n");
                }
                String header = null; // i18n.getLabel("replace_references.found_refs_header")
                progress.logText(header, str.toString());
            }
        }
        return replaced;
    }
    
    private void writeSummaryPerLanguage()
    {
        final String dots  = "........................................";
        DocmaLanguage origLang = workSess.getOriginalLanguage();
        String orig = (origLang == null) ? "Original" : origLang.getCode();
        StringBuilder buf = new StringBuilder();
        for (String langKey : replacedStats.keySet()) {
            String lang = langKey.equals("") ? orig : langKey;
            String cnt =  " " + replacedStats.get(langKey);
            int len = lang.length() + cnt.length() + 1;
            buf.append(lang).append(" ");
            if (len < dots.length() - 3) {
                buf.append(dots.substring(len));
            } else {
                buf.append("...");
            }
            buf.append(cnt).append("\n");
        }
        progress.logText(null, buf.toString());
    }
    
    private void increaseStats(String langCode, int cnt)
    {
        String key = (langCode != null) ? langCode : "";
        Integer val = replacedStats.get(key);
        replacedStats.put(key, (val != null) ? val + cnt : cnt);
    }
    
    private List<DocmaNode> getNodes()
    {
        List<DocmaNode> nodes = new ArrayList<DocmaNode>(nodeIds.length);
        for (String nId : nodeIds) {
            DocmaNode nd = workSess.getNodeById(nId);
            if (nd != null) {
                nodes.add(nd);
            } else {
                progress.logWarning("node_id_not_found", nId);
            }
        }
        return nodes;
    }
}
