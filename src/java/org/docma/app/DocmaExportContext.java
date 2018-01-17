/*
 * DocmaExportContext.java
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
import org.docma.plugin.*;
import org.docma.util.Log;
import java.util.Map;
import java.util.Set;
import java.util.Properties;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 *
 * @author MP
 */
public class DocmaExportContext implements ExportContext
{
    private final DocmaSession docmaSess;
    private final DocmaExportLog exportLog;
    private DocmaPublicationConfig pubConf;
    private DocmaOutputConfig outConf;

    private Properties genTextProps = null;
    private HashMap<String, DocmaStyle> styleCache = null;

    private boolean autoFormatEnabled = true;  // Resolve auto-formats during creation of print-instance
    private boolean previewFormattingEnabled = false; // Show image title, formal block titles,... in preview
    private boolean exportFormattingEnabled = true; // prepare print-instance for export with html2docbook.xsl
    private HashMap<String, AutoFormat> autoFormatInstances = null;

    private Map<String, Set<String>> referencedPubTargets = null;
    private Map<String, DocmaPublicationConfig> referencedPubConfigs = null;
    
    /* --------------  Constructors  ---------------------- */

    public DocmaExportContext(DocmaSession docmaSess,
                              DocmaPublicationConfig pubConf,
                              DocmaOutputConfig outConf,
                              boolean writeLog)
    {
        this.docmaSess = docmaSess;
        this.pubConf = pubConf;
        this.outConf = outConf;
        if (writeLog) {
            exportLog = new DocmaExportLog(docmaSess.getI18n());
        } else {
            exportLog = null;
        }
    }

    /* --------------  Public methods  ---------------------- */

    public ExportLog getExportLog()
    {
        return exportLog;
    }

    public DocmaExportLog getDocmaExportLog()
    {
        return exportLog;
    }

    public void finished()
    {
        if (autoFormatInstances != null) {
            Iterator<AutoFormat> it = autoFormatInstances.values().iterator();
            while (it.hasNext()) {
                AutoFormat af_instance = it.next();
                try {
                    af_instance.finished();
                } catch (Exception ex) {
                    if (DocmaConstants.DEBUG) ex.printStackTrace();
                    Log.error("Exception in AutoFormat.finished(): " + ex.getMessage());
                }
            }
        }
    }

    /* --------------  Package local methods  ---------------------- */

    DocmaSession getDocmaSession()
    {
        return docmaSess;
    }
    
    String getApplicationProperty(String prop_name) 
    {
        return docmaSess.getApplicationProperty(prop_name);
    }

    DocmaOutputConfig getOutputConfig()
    {
        return outConf;
    }

    void setOutputConfig(DocmaOutputConfig outConf)
    {
        this.outConf = outConf;
    }

    DocmaPublicationConfig getPublicationConfig()
    {
        return pubConf;
    }

    void setPublicationConfig(DocmaPublicationConfig pubConf)
    {
        this.pubConf = pubConf;
    }

    boolean isAutoFormatEnabled()
    {
        return autoFormatEnabled;
    }

    void setAutoFormatEnabled(boolean enabled)
    {
        autoFormatEnabled = enabled;
    }

    boolean isPreviewFormattingEnabled()
    {
        return previewFormattingEnabled;
    }

    void setPreviewFormattingEnabled(boolean enabled)
    {
        previewFormattingEnabled = enabled;
    }

    boolean isExportFormattingEnabled()
    {
        return exportFormattingEnabled;
    }

    void setExportFormattingEnabled(boolean enabled)
    {
        exportFormattingEnabled = enabled;
    }

    DocmaStyle getStyle(String styleID)
    {
        if (styleID == null) return null;
        if (styleCache == null) {
            styleCache = new HashMap<String, DocmaStyle>(77);
        }
        DocmaStyle style = styleCache.get(styleID);
        if (style != null) return style;
        String variant = outConf.getStyleVariant();
        style = docmaSess.getStyleVariant(styleID, variant);
        if (style != null) styleCache.put(styleID, style);
        return style;
    }

    AutoFormat getAutoFormatInstance(String clsname) throws Exception
    {
        if (autoFormatInstances == null) {
            autoFormatInstances = new HashMap<String, AutoFormat>();
        }
        AutoFormat af_instance = autoFormatInstances.get(clsname);
        if (af_instance == null) {
            Class af_class = Class.forName(clsname);
            af_instance = (AutoFormat) af_class.newInstance();
            af_instance.initialize(this);
            autoFormatInstances.put(clsname, af_instance);
        }
        return af_instance;
    }
    
    String getNodeTitleEntityEncoded(String nodeAlias)
    {
        DocmaNode nd = docmaSess.getNodeByAlias(nodeAlias);
        return (nd == null) ? null : nd.getTitleEntityEncoded();
    }
    
    String getNodeTitleInReferencedPub(String nodeAlias) 
    {
        try {
            String refPubId = isNodeInReferencedPub(nodeAlias);
            if (refPubId == null) {
                return null;
            } 
            DocmaPublicationConfig pc = getReferencedPubConfig(refPubId);
            if (pc == null) {  // should not occur if refPubId is not null;
                return null;
            }
            String refPubTitle = pc.getTitle();
            if ((refPubTitle == null) || refPubTitle.trim().equals("")) {
                refPubTitle = refPubId;
            } else {
                refPubTitle = docmaSess.encodeCharEntities(refPubTitle, false);
            }
            DocmaNode nd = docmaSess.getNodeByAlias(nodeAlias);
            if (nd == null) {
                return null;
            }
            String nodeTitle = nd.getTitleEntityEncoded();
            String genTxtPattern = getGenTextProperty("external-pub-ref");
            String ref_title;
            if ((genTxtPattern == null) || genTxtPattern.trim().equals("")) {
                ref_title = "'" + nodeTitle + "' [" + refPubTitle + "]";  // default
            } else {
                ref_title = genTxtPattern;
                // Replace %t and %r in genTxtPattern:
                // Note: Maybe the titles contain "%t" and/or "%r". 
                // Therefore indexOf() is used instead of simple replace()
                int t_idx = genTxtPattern.indexOf("%t");
                int r_idx = genTxtPattern.indexOf("%r");
                if ((t_idx >= 0) || (r_idx >= 0)) {  // placeholder %t or %r exists
                    int pos1 = (t_idx >= 0) && (t_idx < r_idx) ? t_idx : r_idx;  // index of 1st placeholder
                    int pos2 = (pos1 == t_idx) ? r_idx : t_idx;  // index of 2nd placeholder, or -1 if no 2nd placeholder
                    StringBuilder sb = new StringBuilder();
                    sb.append(genTxtPattern, 0, pos1).append((pos1 == t_idx) ? nodeTitle : refPubTitle);
                    if (pos2 >= 0) {
                        sb.append(genTxtPattern, pos1 + 2, pos2).append((pos2 == t_idx) ? nodeTitle : refPubTitle)
                          .append(genTxtPattern, pos2 + 2, genTxtPattern.length());
                    } else {
                        sb.append(genTxtPattern, pos1 + 2, genTxtPattern.length());
                    }
                    ref_title = sb.toString();
                }
            }
            return ref_title;
        } catch (Throwable ex) {
            if (DocmaConstants.DEBUG) {
                ex.printStackTrace();
            }
            if (exportLog != null) {
                exportLog.errorMsg("Could not retrieve node title in referenced publication: " + ex.getMessage());
            }
            return null;
        }
    }
    
    /* --------------  Interface ExportContext  ---------------------- */

    public String getGUILanguage()
    {
        String lang = docmaSess.getUserProperty(DocmaConstants.PROP_USER_GUI_LANGUAGE);
        if ((lang == null) || lang.trim().equals("")) return "en";
        else return lang.toLowerCase();
    }

    public String getContentStringByAlias(String nodeAlias)
    {
        return getContentStringByAlias(nodeAlias, false);
    }

    public String getContentStringByAlias(String nodeAlias, boolean resolveInclusions)
    {
        DocmaNode nd = docmaSess.getNodeByAlias(nodeAlias);
        if (nd == null) return null;
        String cont;
        if (resolveInclusions && nd.isHTMLContent()) {
            final boolean suppressAutoFormat = true;   // do not resolve autoformat to avoid infinite recursion 
            cont = ContentResolver.getContentRecursive(nd, new HashSet(), this, resolveInclusions, suppressAutoFormat, 0);
        } else {
            cont = nd.getContentString();
        }
        return (cont == null) ? "" : cont;
    }

    public byte[] getContentBytesByAlias(String nodeAlias)
    {
        DocmaNode nd = docmaSess.getNodeByAlias(nodeAlias);
        if (nd == null) return null;
        byte[] arr = nd.getContent();
        return (arr == null) ? new byte[0] : arr;
    }

    public String getExportLanguage()
    {
        return docmaSess.getLanguageCode();  // should be the same as outConf.getLanguageCode()
        // Be careful: outConf.getLanguageCode() may not yet be initialized when constructor is called
    }

    public String getExportFormat()
    {
        return outConf.getFormat();
    }

    public String getExportSubformat()
    {
        return outConf.getSubformat();
    }

    public String getGenTextProperty(String propertyName)
    {
        if (genTextProps == null) {
            // Be careful: outConf.getGentextProps() may not yet be initialized
            // when DocmaExportContext constructor is called. Therefore the
            // field genTextProps must be initialized here and not in the constructor!
            genTextProps = outConf.getGentextProps();
            if (genTextProps == null) {
                // This happens on HTML preview because outConf.setGentextProps(...)
                // is called by PublicationManager which is not used for preview.
                // Maybe better solution is to move outConf.setGentextProps(...)
                // out of PublicationManager (e.g. into DocmaAppUtil.getNodePreview(), ...
                // and DocmaAppUtil.getNodePrintInstance(...).
                try {
                    genTextProps = docmaSess.getGenTextProps();
                } catch (Exception ex) {
                    if (exportLog != null) exportLog.errorMsg("Could not load gentext properties!");
                    // set empty properties to avoid repeated logging of this error message:
                    genTextProps = new Properties();
                }
            }
        }
        if (genTextProps == null) return "";
        return genTextProps.getProperty(propertyName, "");
    }

    public String toCharEntity(char ch, boolean symbolic)
    {
        return docmaSess.toCharEntity(ch, symbolic);
    }

    public String decodeCharEntities(String str)
    {
        return docmaSess.decodeCharEntities(str);
    }

    public void logError(String message)
    {
        if (exportLog != null) exportLog.errorMsg(message);
    }

    public void logWarning(String message)
    {
        if (exportLog != null) exportLog.warningMsg(message);
    }


    /* --------------  Private methods  ---------------------- */
    
    private String isNodeInReferencedPub(String nodeAlias)
    {
        if (pubConf == null) {
            return null;
        }
        String[] pids = pubConf.getReferencedPubIds();
        if (referencedPubTargets == null) {
            referencedPubTargets = new HashMap<String, Set<String>>();
        }
        for (String pid : pids) {
            Set<String> targets = referencedPubTargets.get(pid);
            if (targets == null) {
                try {
                    targets = getPublicationTargets(pid);
                } catch (Throwable ex) {
                    if (DocmaConstants.DEBUG) ex.printStackTrace();
                    if (exportLog != null) {
                        exportLog.errorMsg("Could not retrieve targets in referenced publication '" + 
                                           pid + "': " + ex.getMessage());
                    }
                }
                if (targets == null) {
                    targets = new HashSet<String>();  // set empty set for invalid publication ids
                }
                referencedPubTargets.put(pid, targets);
            }
            if (targets.contains(nodeAlias)) {
                return pid;
            }
        }
        return null; // nodeAlias not found in any of the referenced publications
    }
    
    private Set<String> getPublicationTargets(String pubConfId) throws DocException
    {
        DocmaPublicationConfig pc = getReferencedPubConfig(pubConfId);
        if (pc == null) {
            return null;
        }
        String rootId = pc.getContentRoot();
        DocmaNode rootNode = docmaSess.getNodeById(rootId);
        if (rootNode == null) {
            return null;
        }
        
        // Create temporary objects for retrieving the referenced print instance
        DocmaOutputConfig temp_out_conf = docmaSess.getOutputConfig(getOutputConfig().getId());
        DocmaExportContext temp_ctx = new DocmaExportContext(docmaSess, pc, temp_out_conf, false);
        
        PublicationManager pm = docmaSess.getPublicationManager();
        StringBuilder html_buf = pm.getPrintInstance(rootNode, temp_ctx);
        HashSet<String> alias_set = new HashSet<String>();
        // ContentUtil.getIdValues(html_buf, alias_set);
        ContentUtil.getIdValues(html_buf.toString(), alias_set);
        return alias_set;
    }
    
    private DocmaPublicationConfig getReferencedPubConfig(String refPubId) 
    {
        if (referencedPubConfigs == null) {
            referencedPubConfigs = new HashMap<String, DocmaPublicationConfig>();
        }
        DocmaPublicationConfig pc = referencedPubConfigs.get(refPubId);
        if (pc == null) {
            pc = docmaSess.getPublicationConfig(refPubId);
            if (pc != null) {
                referencedPubConfigs.put(refPubId, pc);
            } else {
                if (exportLog != null) {
                    exportLog.errorMsg("Referenced publication '" + refPubId + "' does not exist!");
                }
            }
        }
        return pc;
    }

}
