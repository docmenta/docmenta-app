/*
 * DocmaNode.java
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
import org.docma.coreapi.fsimplementation.DocContentImpl;
import org.docma.lockapi.Lock;
import org.docma.util.Log;
import java.util.*;
import java.io.*;
import org.docma.util.DocmaUtil;

/**
 *
 * @author MP
 */
public class DocmaNode
{
    static final String ATTR_GROUP_TYPE = "grouptype";
    static final String ATTR_REFERENCE_TYPE = "reftype";

    static final String TYPE_CONTENT = "CONTENT";
    static final String TYPE_IMAGE_REFERENCE = "REFERENCE_IMAGE";
    static final String TYPE_CONTENT_REFERENCE = "REFERENCE_CONTENT";
    static final String TYPE_SECTION_REFERENCE = "REFERENCE_SECTION";
    static final String TYPE_SECTION = "SECTION";
    static final String TYPE_IMG_FOLDER = "FOLDER_IMG";
    static final String TYPE_SYS_FOLDER = "FOLDER_SYS";

    private static final boolean AUTOCALC_NODE_PROGRESS = true;
    private static final boolean AUTOCALC_NODE_WFSTATUS = true;

    private final int openedStoreCounter;

    private String nodeType       = null;
    // private Boolean translated    = null;
    private String title          = null;
    private String id             = null;
    private String alias          = null;
    private String refTarget      = null;
    private Date lastModifiedDate = null;
    private String lastModifiedBy = null;
    private String workflowStatus = null;
    private int    progress       = -1;
    private String comment        = null;
    private String priority       = null;
    // private String[] authors      = null;
    private String applicability  = null;
    // private Lock   lock           = null;
    private DocmaAnchor[] anchors = null;
    private String contentType    = null;
    private String fileExtension  = null;
    private String defaultFilename = null;

    private final DocmaSession docmaSess;
    private final DocNode backendNode;

    private DocmaNode[] children = null;
    // private List childrenReadOnly = null;

    private boolean node_transaction = false;


    /* -------  package local: called from DocmaSession  ---------- */

    DocmaNode(DocmaSession docmaSess, DocNode backendNode)
    {
        this.docmaSess = docmaSess;
        this.backendNode = backendNode;
        // this.parent = parent;

        this.openedStoreCounter = docmaSess.getOpenedStoreCounter();

        if (backendNode instanceof DocGroup) {
            String grouptype = backendNode.getAttribute(ATTR_GROUP_TYPE);
            if (grouptype == null) grouptype = TYPE_SECTION;
            setNodeType(grouptype);
        } else
        if (backendNode instanceof DocContent) {
            setNodeType(TYPE_CONTENT);
        } else
        if (backendNode instanceof DocReference) {
            String reftype = backendNode.getAttribute(ATTR_REFERENCE_TYPE);
            setNodeType(reftype);
        } else {
            throw new DocRuntimeException("Unknown node type: " + backendNode.getClass().getName());
        }

        this.id = backendNode.getId();
        // docmaSess.addToNodeMap(this);
    }

    DocNode getBackendNode()
    {
        return backendNode;
    }

    void clearLocalCache()
    {
        // delete cached values
        title          = null;
        alias          = null;
        refTarget      = null;
        lastModifiedDate = null;
        lastModifiedBy = null;
        workflowStatus = null;
        progress       = -1;
        comment        = null;
        priority       = null;
        // authors      = null;
        applicability  = null;
        // lock           = null;
        anchors        = null;
        contentType    = null;
        fileExtension  = null;
        defaultFilename = null;
        children       = null;
        // translated     = null;
    }

    /* --------------  private methods  ---------------------- */

    private void setNodeType(String nodeType)
    {
        this.nodeType = nodeType;
    }

    private String getLockName()
    {
        String transMode = getTranslationMode();
        if (transMode == null) {
            return "lock";
        } else {
            return "lock" + "#" + transMode;
        }
    }

    private void getNodesRecursive(List resultList)
    {
        if (backendNode instanceof DocGroup) {
            for (int i=0; i < getChildCount(); i++) {
                resultList.add(getChild(i));
            }
        }
        resultList.add(this);
    }

    // void getPrintInstance(DocmaOutputConfig out_conf, StringBuffer buf)
    // {
    //     DocmaAppUtil.getPrintInstance(buf, this, out_conf);
    // }

    /* --------------  public methods  ---------------------- */

    public DocmaSession getDocmaSession()
    {
        if (openedStoreCounter != docmaSess.getOpenedStoreCounter()) {
            throw new DocRuntimeException("Node is no longer valid. Store has already been closed.");
        }
        return docmaSess;
    }

    public void refresh() 
    {
        clearLocalCache();
        backendNode.refresh();
    }

    public String getTranslationMode()
    {
        return docmaSess.getTranslationMode();
    }

    public boolean isTranslationMode()
    {
        return (docmaSess.getTranslationMode() != null);
    }

    public boolean isTranslated()
    {
        // if (translated == null) {  // read from backendNode if value is not cached
        String lang_mode = getTranslationMode();
        if (lang_mode == null) { 
            String[] trans = backendNode.getTranslations();
            return (trans != null) && (trans.length > 0);
        } else {
            return backendNode.hasTranslation(lang_mode);
            // if (backendNode.hasTranslation(lang_mode)) translated = Boolean.TRUE;
            // else return false;  // translated = Boolean.FALSE;
        }
        // }
        // return translated.booleanValue();
    }

    public boolean isTranslated(String lang_mode)
    {
        if (lang_mode == null) { 
            String[] trans = backendNode.getTranslations();
            return (trans != null) && (trans.length > 0);
        } else {
            return backendNode.hasTranslation(lang_mode);
        }
    }
    
    public String[] listTranslations()
    {
        return backendNode.getTranslations();
    }
    
    public boolean isTitleTranslated()
    {
        String lang_mode = getTranslationMode();
        if (lang_mode == null) { 
            return false;  // this method should only be used in translation mode
        }
        String tit = backendNode.getTitle(lang_mode);
        return (tit != null) && (tit.length() > 0);
    }

    public boolean isContentTranslated()
    {
        String lang_mode = getTranslationMode();
        if (lang_mode == null) { 
            return false;  // this method should only be used in translation mode
        }
        if (backendNode instanceof DocContent) {
            return ((DocContent) backendNode).hasContent(lang_mode);
        } else {
            return false;
        }
    }
    
    public boolean hasContent(String lang_mode)
    {
        if (backendNode instanceof DocContent) {
            return ((DocContent) backendNode).hasContent(lang_mode);
        } else {
            return false;
        }
    }

    public boolean deleteTranslation()
    {
        String lang_code = getTranslationMode();
        if (lang_code == null) return false;
        backendNode.deleteTranslation(lang_code);
        refresh();
        return true;
    }

    public String getId()
    {
        return id;
    }

    public String getCustomAttribute(String name)
    {
        String name_low = name.toLowerCase();
        if (DocmaConstants.isInternalAttributeName(name_low)) {
            // Use special get method for attributes that are cached by DocmaNode
            if (name_low.equals(DocmaConstants.ATTRIBUTE_DOCNODE_APPLIC)) {
                return getApplicability();
            } else if (name_low.equals(DocmaConstants.ATTRIBUTE_DOCNODE_COMMENT)) {
                return getComment();
            } else if (name_low.equals(DocmaConstants.ATTRIBUTE_DOCNODE_LASTMOD_BY)) {
                return getLastModifiedBy();
            } else if (name_low.equals(DocmaConstants.ATTRIBUTE_DOCNODE_LASTMOD_DATE)) {
                Date dt = getLastModifiedDate();
                return (dt == null) ? "" : Long.toString(dt.getTime());
            } else if (name_low.equals(DocmaConstants.ATTRIBUTE_DOCNODE_PRIORITY)) {
                return getPriority();
            } else if (name_low.equals(DocmaConstants.ATTRIBUTE_DOCNODE_PROGRESS)) {
                return Integer.toString(getProgress());
            } else if (name_low.equals(DocmaConstants.ATTRIBUTE_DOCNODE_WORKFLOWSTATE)) {
                return getWorkflowStatus();
            } else if (DocmaConstants.isPredefinedAttributeName(name_low)) {
                return backendNode.getAttribute(name_low);
            } else {
                throw new DocRuntimeException("Cannot get user attribute: Invalid attribute name.");
            }
        } else {
            return backendNode.getAttribute(name_low);
        }
    }

    public String getCustomAttribute(String name, String lang_code)
    {
        String name_low = name.toLowerCase();
        if (DocmaConstants.isInternalAttributeName(name_low)) {
            if (DocmaConstants.isPredefinedAttributeName(name_low)) {
                return backendNode.getAttribute(name_low, lang_code);
            } else {
                throw new DocRuntimeException("Cannot get user attribute: Invalid attribute name.");
            }
        } else {
            return backendNode.getAttribute(name_low, lang_code);
        }
    }
    
    public String getCustomAttributeEntityEncoded(String name)
    {
        return docmaSess.encodeCharEntities(getCustomAttribute(name), false);
    }

    public String[] getCustomAttributeNames()
    {
        String[] attnames = backendNode.getAttributeNames();
        
        // Remove all hidden attributes 
        int old_len = attnames.length;
        int len = old_len;
        for (int i = old_len - 1; i >= 0; i--) {
            if (DocmaConstants.isHiddenAttributeName(attnames[i])) {
                // Delete name at position i by moving all upper elements one
                // position down
                int cnt = (--len) - i;  // number of upper elements
                System.arraycopy(attnames, i + 1, attnames, i, cnt);
            }
        }
        if (old_len == len) {   // no hidden attributes 
            return attnames;
        }
        String[] res = new String[len];
        System.arraycopy(attnames, 0, res, 0, len);
        return res;
    }
    
    public void setCustomAttribute(String name, String value)
    {
        String name_low = name.toLowerCase();
        if (DocmaConstants.isInternalAttributeName(name_low)) {
            // Use special set method for attributes that are cached by DocmaNode
            if (name_low.equals(DocmaConstants.ATTRIBUTE_DOCNODE_APPLIC)) {
                setApplicability(value);
            } else if (name_low.equals(DocmaConstants.ATTRIBUTE_DOCNODE_COMMENT)) {
                setComment(value);
            } else if (name_low.equals(DocmaConstants.ATTRIBUTE_DOCNODE_LASTMOD_BY)) {
                setLastModifiedBy(value);
            } else if (name_low.equals(DocmaConstants.ATTRIBUTE_DOCNODE_LASTMOD_DATE)) {
                setLastModifiedDate(new Date(Long.parseLong(value)));
            } else if (name_low.equals(DocmaConstants.ATTRIBUTE_DOCNODE_PRIORITY)) {
                setPriority(value);
            } else if (name_low.equals(DocmaConstants.ATTRIBUTE_DOCNODE_PROGRESS)) {
                setProgress(Integer.parseInt(value), false);
            } else if (name_low.equals(DocmaConstants.ATTRIBUTE_DOCNODE_WORKFLOWSTATE)) {
                setWorkflowStatus(value, false);
            } else if (DocmaConstants.isPredefinedAttributeName(name_low)) {
                backendNode.setAttribute(name_low, value);
            } else {
                throw new DocRuntimeException("Cannot set user attribute: Invalid attribute name.");
            }
        } else {
            backendNode.setAttribute(name_low, value);
        }
    }

    public String getTitle()
    {
        if (title == null) {
            if (isDocumentRoot()) {
                return docmaSess.getDocStoreTitle();
            } else {
                title = backendNode.getTitle();
            }
        }
        return title;
    }

    public String getTitleEntityEncoded()
    {
        return docmaSess.encodeCharEntities(getTitle(), false);
    }

    public void setTitle(String title)
    {
        boolean started = startNodeTransaction();
        try {
            defaultFilename = null;   // clear cached value;
            backendNode.setTitle(title);
            this.title = title;
            if (! isContent()) {  // last modified attributes of content nodes 
                                  // are updated in setContent...() methods
                updateLastModifiedAttributes();
            }
            commitNodeTransaction(started);
        } catch (Exception ex) {
            rollbackNodeTransaction(started);
            throw new DocRuntimeException(ex);
        }
    }

    public String getAlias()
    {
        if (alias == null) {  // not loaded
            String astr = backendNode.getAlias();
            if (astr == null) {
                alias = "";  // avoid reloading from backendNode by setting to empty string
            } else {
                alias = hasContentAnchor(astr) ? "" : astr;
            }
        }
        if (alias.equals("")) return null;
        return alias;
    }

    public String getLinkAlias()
    {
        return DocmaAppUtil.getLinkAlias(getAlias());
    }

    public void setAlias(String alias)
    {
        boolean not_empty = (alias != null) && (alias.length() > 0);
        if (not_empty && !DocmaAppUtil.isValidAlias(alias)) {
            throw new DocRuntimeException("Invalid alias.");
        }
        boolean started = startNodeTransaction();
        try {
            defaultFilename = null;  // clear cached value;
            String old_alias = getAlias();
            if (old_alias != null) backendNode.deleteAlias(old_alias);
            if (not_empty) {
                backendNode.addAlias(alias);
            }
            this.alias = alias;
            commitNodeTransaction(started);
        } catch (Exception ex) {
            rollbackNodeTransaction(started);
            throw new DocRuntimeException(ex);
        }
    }

    public DocmaAnchor[] getContentAnchors()
    {
        if (isHTMLContent()) {
            if (anchors == null) {
                anchors = ContentUtil.getContentAnchors(getContentString(), this);
            }
            return anchors;
        } else {
            return null;
        }
    }

    public DocmaAnchor getContentAnchor(int index)
    {
        return getContentAnchors()[index];
    }

    /**
     * This method is called within setContentString().
     * @param content
     */
    private void setContentAnchors(String content)
    {
        String content_alias = getAlias();  // getAlias() has to be called before the
                                            // anchors fields is updated,
                                            // otherwise getAlias() may return a replaced anchor as node alias!
        DocmaAnchor[] new_anchors = ContentUtil.getContentAnchors(content, this);
        int offset = (content_alias == null) ? 0 : 1;
        String[] alias_arr = new String[new_anchors.length + offset];
        if (offset == 1) {
            alias_arr[0] = content_alias;
        }
        HashSet aset = new HashSet(2*alias_arr.length);
        for (int i=0; i < new_anchors.length; i++) {
            String a_id = new_anchors[i].getAlias();
            if (aset.contains(a_id)) {
                throw new DocmaDuplicateAnchorException("Id of anchor is not unique within node.");
            }
            DocmaNode onode = docmaSess.getNodeByAlias(a_id);
            if ((onode != null) && !onode.getId().equals(this.getId())) {
                throw new DocmaDuplicateAliasException("Id of anchor is already used in another node.");
            }
            aset.add(a_id);
            alias_arr[i + offset] = a_id;
        }
        if (isTranslationMode()) {
            String[] orig_arr = backendNode.getAliases();
            Set orig_set = new TreeSet(Arrays.asList(orig_arr));
            Set trans_set = new TreeSet(Arrays.asList(alias_arr));
            if (! trans_set.equals(orig_set)) {
                throw new DocmaAnchorsDifferException("Translated anchors differ from original anchors.");
            }
        } else {
            backendNode.setAliases(alias_arr);
        }
        // Everything is okay
        anchors = new_anchors;
    }

    public int getContentAnchorCount()
    {
        getContentAnchors();  // set anchors field if not already set
        return (anchors == null) ? 0 : anchors.length;
    }

    public boolean hasContentAnchor()
    {
        getContentAnchors();  // set anchors field if not already set
        return (anchors != null) && (anchors.length > 0);
    }

    public boolean hasContentAnchor(String anchorAlias)
    {
        getContentAnchors();  // set anchors field if not already set
        if (anchors == null) return false;
        for (int i=0; i < anchors.length; i++) {
            if (anchorAlias.equalsIgnoreCase(anchors[i].getAlias())) return true;
        }
        return false;
    }

    public String getReferenceTarget()
    {
        if (refTarget == null) {
            if (backendNode instanceof DocReference) {
                refTarget = ((DocReference) backendNode).getTargetAlias();
            }
        }
        return refTarget;
    }

    public void setReferenceTarget(String target)
    {
        boolean started = startNodeTransaction();
        try {
            ((DocReference) backendNode).setTargetAlias(target);
            this.refTarget = target;
            commitNodeTransaction(started);
        } catch (Exception ex) {
            rollbackNodeTransaction(started);
            throw new DocRuntimeException(ex);
        }
    }

    public DocmaNode getReferencedNode()
    {
        String target_alias = getReferenceTarget();
        if ((target_alias == null) || target_alias.equals("")) return null;
        return docmaSess.getNodeByAlias(target_alias);
    }

    public Date getLastModifiedDate()
    {
        if (lastModifiedDate == null) {
            String lastmod_str = backendNode.getAttribute(DocmaConstants.ATTRIBUTE_DOCNODE_LASTMOD_DATE);
            if ((lastmod_str != null) && (lastmod_str.length() > 0)) {
                try {
                    lastModifiedDate = new Date(Long.parseLong(lastmod_str));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Date lastModifiedDate)
    {
        String last_mod_millis = Long.toString(lastModifiedDate.getTime());
        backendNode.setAttribute(DocmaConstants.ATTRIBUTE_DOCNODE_LASTMOD_DATE, last_mod_millis);
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getLastModifiedBy()
    {
        if (lastModifiedBy == null) {
            lastModifiedBy = backendNode.getAttribute(DocmaConstants.ATTRIBUTE_DOCNODE_LASTMOD_BY);
        }
        if ((lastModifiedBy != null) && (lastModifiedBy.length() == 0)) return null;
        else return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy)
    {
        backendNode.setAttribute(DocmaConstants.ATTRIBUTE_DOCNODE_LASTMOD_BY, lastModifiedBy);
        this.lastModifiedBy = lastModifiedBy;
    }

    public String getWorkflowStatus()
    {
        if (workflowStatus == null) {
            if (isTranslationMode() && !isTranslated()) return null;
            workflowStatus = backendNode.getAttribute(DocmaConstants.ATTRIBUTE_DOCNODE_WORKFLOWSTATE);
        }
        return workflowStatus;
    }

    public String getWorkflowStatusLabel()
    {
        String wf_state = getWorkflowStatus();
        if ((wf_state == null) || wf_state.equals("")) {
            return "-";
        }
        DocmaI18 i18 = docmaSess.getI18();
        return i18.getLabel("label.workflowstatus." + wf_state.toLowerCase());
    }

    public void setWorkflowStatus(String wfStatus)
    {
        setWorkflowStatus(wfStatus, true);
    }
    
    public void setWorkflowStatus(String wfStatus, boolean updateParent)
    {
        backendNode.setAttribute(DocmaConstants.ATTRIBUTE_DOCNODE_WORKFLOWSTATE, wfStatus);
        this.workflowStatus = wfStatus;
        if (AUTOCALC_NODE_WFSTATUS && updateParent) {
            // recalculate workflow status of all parent nodes
            DocmaNode parnode = getParent();
            if ((parnode != null) && !isDocumentRoot()) {
                parnode.recalculateSectionWorkflowStatus();
            }
        }
    }

    public int getProgress()
    {
        if (progress < 0) {
            String str = backendNode.getAttribute(DocmaConstants.ATTRIBUTE_DOCNODE_PROGRESS);
            if (str == null) {
                progress = -1;
            } else {
                try {
                    progress = Integer.parseInt(str);
                } catch (Exception ex) {
                    progress = -1;
                    // Log.error("Invalid progress value: '" + str + "'. Exception: " + ex.getMessage());
                    // ex.printStackTrace();
                }
            }
            if (isTranslationMode() && (!isTranslated()) && (progress > 0) && isHTMLContent()) {
                progress = 0;
            }
        }
        return progress;
    }

    public void setProgress(int progress)
    {
        setProgress(progress, true);
    }
    
    public void setProgress(int progress, boolean updateParent)
    {
        backendNode.setAttribute(DocmaConstants.ATTRIBUTE_DOCNODE_PROGRESS, "" + progress);
        this.progress = progress;
        if (AUTOCALC_NODE_PROGRESS && updateParent) {
            // recalculate progress value of all parent nodes
            DocmaNode parnode = getParent();
            if ((parnode != null) && !isDocumentRoot()) {
                parnode.recalculateSectionProgress();
            }
        }
    }

    public void recalculateSectionProgress()
    {
        if (! this.isSection()) return;  // do nothing
        int newprog = 0;
        int cnt = 0;
        for (int i=0; i < this.getChildCount(); i++) {
            int prog = this.getChild(i).getProgress();
            if (prog >= 0) {
                newprog += prog;
                cnt++;
            }
        }
        if (cnt > 1) newprog = (int) Math.floor(newprog / cnt);
        this.setProgress(newprog, true);  // recursive loop until document root is reached
    }

    public void recalculateSectionWorkflowStatus()
    {
        if (! this.isSection()) return;  // do nothing
        String newstat = null;
        for (int i=0; i < this.getChildCount(); i++) {
            String stat = this.getChild(i).getWorkflowStatus();
            if (stat != null) {
                if (stat.equals("wip")) {
                    newstat = "wip";
                    break;
                } else
                if (stat.equals("rfa")) {
                    newstat = "rfa";
                } else
                if (stat.equals("approved")) {
                    if (newstat == null) newstat = "approved";
                }
            }
        }
        this.setWorkflowStatus(newstat, true);  // recursive loop until document root is reached
    }

    public void recalculateSectionAttributes()
    {
        recalculateSectionProgress();
        recalculateSectionWorkflowStatus();
    }

    public String getComment()
    {
        if (comment == null) {
            comment = backendNode.getAttribute(DocmaConstants.ATTRIBUTE_DOCNODE_COMMENT, getTranslationMode());
            if (comment == null) comment = "";
        }
        return comment;
    }

    public void setComment(String comment)
    {
        backendNode.setAttribute(DocmaConstants.ATTRIBUTE_DOCNODE_COMMENT, comment);
        this.comment = comment;
    }

    public String getPriority()
    {
        if (priority == null) {
            priority = backendNode.getAttribute(DocmaConstants.ATTRIBUTE_DOCNODE_PRIORITY, getTranslationMode());
            if (priority == null) priority = "";
        }
        return priority;
    }

    public void setPriority(String prio)
    {
        backendNode.setAttribute(DocmaConstants.ATTRIBUTE_DOCNODE_PRIORITY, prio);
        this.priority = prio;
    }

    // public String[] getAuthors()
    // {
    //     return authors;
    // }

    // public void setAuthors(String[] authors)
    // {
    //     this.authors = authors;
    // }

    public String getApplicability()
    {
        if (applicability == null) {
            applicability = backendNode.getAttribute(DocmaConstants.ATTRIBUTE_DOCNODE_APPLIC);
        }
        return applicability;
    }

    public void setApplicability(String applic)
    {
        backendNode.setAttribute(DocmaConstants.ATTRIBUTE_DOCNODE_APPLIC, applic);
        this.applicability = applic;
    }

    public String getFileCharset()
    {
        String cs = backendNode.getAttribute(DocmaConstants.ATTRIBUTE_DOCNODE_CHARSET);
        if (cs == null) return null;
        return (cs.length() == 0) ? null : cs;
    }

    public void setFileCharset(String charsetName)
    {
        backendNode.setAttribute(DocmaConstants.ATTRIBUTE_DOCNODE_CHARSET, charsetName);
    }

    public Lock getLock()
    {
        if (backendNode instanceof DocContent) {
            return ((DocContent) backendNode).getLock(getLockName());
        } else {
            return null;   // Lock can only be set for content nodes
        }
    }

    public boolean isLocked()
    {
        return (getLock() != null);
    }

    public boolean setLock(long timeout)
    {
        if (backendNode instanceof DocContent) {
            return ((DocContent) backendNode).setLock(getLockName(), timeout);
        } else {
            return false;  // Lock can only be set for content nodes
        }
    }

    public boolean refreshLock(long timeout)
    {
        if (backendNode instanceof DocContent) {
            return ((DocContent) backendNode).refreshLock(getLockName(), timeout);
        } else {
            return false;  // Lock can only be set for content nodes
        }
    }

    public Lock removeLock()
    {
        if (backendNode instanceof DocContent) {
            return ((DocContent) backendNode).removeLock(getLockName());
        } else {
            return null;   // Lock can only be set for content nodes
        }
    }

    public String getContentRelativeURL(String lang)
    {
        if (backendNode instanceof DocContentImpl) {
            String path = ((DocContentImpl) backendNode).getContentPath(lang);
            if (File.separatorChar != '/') {
                path = path.replace(File.separatorChar, '/');
            }
            return path;
        } else {
            return null;
        }
    }

    public byte[] getContent()
    {
        if (backendNode instanceof DocContent) {
            return ((DocContent) backendNode).getContent();
        } else {
            return null;
        }
    }

    public InputStream getContentStream()
    {
        if (backendNode instanceof DocContent) {
            return ((DocContent) backendNode).getContentStream();
        } else {
            return null;
        }
    }

    public String getContentString()
    {
        if (backendNode instanceof DocFile) {
            String charsetName = getFileCharset();
            if (charsetName == null) {
                return ((DocContent) backendNode).getContentString();
            } else {
                return ((DocContent) backendNode).getContentString(charsetName);
            }
        } else
        if (backendNode instanceof DocContent) {
            return ((DocContent) backendNode).getContentString();
        } else {
            return null;
        }
    }

    public void setContent(byte[] cont)
    {
        // Note: For HTML content, the setContentString method has to be used,
        //       because setContentString additionally updates the content
        //       anchors and the translation status.
        if (isHTMLContent()) {
            String contstr;
            try {
                contstr = new String(cont, "UTF-8");
            } catch (Exception ex) {
                throw new DocRuntimeException(ex);
            }
            setContentString(contstr);
        } else if (backendNode instanceof DocContent) {
            boolean started = startNodeTransaction();
            try {
                ((DocContent) backendNode).setContent(cont);
                updateLastModifiedAttributes();
                commitNodeTransaction(started);
            } catch (Exception ex) {
                rollbackNodeTransaction(started);
                if (ex instanceof DocRuntimeException) throw (DocRuntimeException) ex;
                else throw new DocRuntimeException(ex);
            }
        } else {
            throw new DocRuntimeException("setContent not allowed for node of type " +
                backendNode.getClass().getName());
        }
    }
    
    public void setContentStream(InputStream cont)
    {
        // Note: For HTML content, the setContentString method has to be used,
        //       because setContentString additionally updates the content
        //       anchors and the translation status.
        if (isHTMLContent()) {
            String contstr;
            try {
                ByteArrayOutputStream outstream = new ByteArrayOutputStream();
                DocmaUtil.copyStream(cont, outstream);
                outstream.close();
                contstr = outstream.toString("UTF-8");
            } catch (Exception ex) {
                throw new DocRuntimeException(ex);
            }
            setContentString(contstr);
        } else if (backendNode instanceof DocContent) {
            boolean started = startNodeTransaction();
            try {
                ((DocContent) backendNode).setContentStream(cont);
                updateLastModifiedAttributes();
                commitNodeTransaction(started);
            } catch (Exception ex) {
                rollbackNodeTransaction(started);
                if (ex instanceof DocRuntimeException) throw (DocRuntimeException) ex;
                else throw new DocRuntimeException(ex);
            }
        } else {
            throw new DocRuntimeException("setContentStream not allowed for node of type " +
                backendNode.getClass().getName());
        }
    }
    
    public void setContentString(String cont)
    {
        if (backendNode instanceof DocContent) {
            boolean started = startNodeTransaction();
            try {
                boolean is_file = isFileContent();
                if (! is_file) setContentAnchors(cont);
                String charsetName = is_file ? getFileCharset() : null;
                DocContent backendCont = (DocContent) backendNode;
                if (charsetName == null) {
                    backendCont.setContentString(cont);
                } else {
                    backendCont.setContentString(cont, charsetName);
                }
                updateLastModifiedAttributes();
                if (! is_file) updateTranslationState();
                commitNodeTransaction(started);
            } catch (Exception ex) {
                rollbackNodeTransaction(started);
                if (ex instanceof DocRuntimeException) throw (DocRuntimeException) ex;
                else throw new DocRuntimeException(ex);
            }
        } else {
            throw new DocRuntimeException("setContentString not allowed for node of type " +
                backendNode.getClass().getName());
        }
    }

    /**
     * Called within setContentString, setUnparsedContentStream, setImageContentStream
     * and setTitle. Should be called within a node transaction.
     */
    private void updateLastModifiedAttributes()
    {
        Date now = new Date();
        setLastModifiedDate(now);
        setLastModifiedBy(docmaSess.getUserId());
    }

    private void updateTranslationState()
    {
        if (!isTranslationMode()) {
            String[] lang_ids = backendNode.getTranslations();
            for (int i = 0; i < lang_ids.length; i++) {
                String langid = lang_ids[i];
                String wfstate = backendNode.getAttribute(DocmaConstants.ATTRIBUTE_DOCNODE_WORKFLOWSTATE, langid);
                if ((wfstate != null) && !wfstate.equalsIgnoreCase("wip")) {
                    backendNode.setAttribute(DocmaConstants.ATTRIBUTE_DOCNODE_WORKFLOWSTATE, "wip", langid);
                }
                String prog = backendNode.getAttribute(DocmaConstants.ATTRIBUTE_DOCNODE_PROGRESS, langid);
                if (prog != null) {
                    try {
                        int pval = Integer.parseInt(prog);
                        if (pval > 75) {
                            backendNode.setAttribute(DocmaConstants.ATTRIBUTE_DOCNODE_PROGRESS, "75", langid);
                        }
                    } catch (Exception ex) {}
                }
            }
        }
    }

    public void setFileContentStream(InputStream content, String file_name)
    {
        if (backendNode instanceof DocFile) {
            boolean started = startNodeTransaction();
            try {
                title = null;            // clear cached title
                fileExtension = null;    // clear cached file-extension
                defaultFilename = null;  // clear cached filename
                DocFile doc_cont = (DocFile) backendNode;
                doc_cont.setContentStream(content);
                doc_cont.setFileName(file_name);
                updateLastModifiedAttributes();
                commitNodeTransaction(started);
            } catch (Exception ex) {
                rollbackNodeTransaction(started);
                if (ex instanceof DocRuntimeException) throw (DocRuntimeException) ex;
                else throw new DocRuntimeException(ex);
            }
        } else {
            throw new DocRuntimeException("setFileContentStream not allowed for node of type " +
                backendNode.getClass().getName());
        }
    }

    public void setImageContent(byte[] content, String alias, String mime_type, String file_ext)
    {
        setImageContentStream(new ByteArrayInputStream(content), alias, mime_type, file_ext);
    }

    public void setImageContentStream(InputStream content, String alias, String mime_type, String file_ext)
    {
        if (backendNode instanceof DocImage) {
            if ((mime_type == null) && (file_ext != null)) {
                mime_type = ImageUtil.guessMIMETypeByExt(file_ext);
            }
            if (mime_type == null) {
                throw new DocRuntimeException("Missing image MIME type.");
            }
            mime_type = mime_type.toLowerCase();
            boolean started = startNodeTransaction();
            try {
                contentType = null;      // clear cached content-type
                fileExtension = null;    // clear cached file-extension
                defaultFilename = null;  // clear cached filename
                DocImage docimg = (DocImage) backendNode;
                docimg.setContentStream(content);
                docimg.setContentType(mime_type);
                if (file_ext == null) {
                    file_ext = mime_type.substring(mime_type.lastIndexOf('/') + 1);
                    // if (mime_type.equals("image/bmp")) file_ext = "bmp";
                    // else if (mime_type.equals("image/cgm")) file_ext = "cgm";
                    // else if (mime_type.equals("image/gif")) file_ext = "gif";
                    // else if (mime_type.equals("image/jpeg")) file_ext = "jpeg";
                    // else if (mime_type.equals("image/png")) file_ext = "png";
                    // else if (mime_type.equals("image/tiff")) file_ext = "tiff";
                }
                docimg.setFileExtension(file_ext);
                setAlias(alias);
                updateLastModifiedAttributes();
                updateTranslationState();
                commitNodeTransaction(started);
            } catch (Exception ex) {
                rollbackNodeTransaction(started);
                if (ex instanceof DocRuntimeException) throw (DocRuntimeException) ex;
                else throw new DocRuntimeException(ex);
            }
        } else {
            throw new DocRuntimeException("setImageContent not allowed for node of type " +
                backendNode.getClass().getName());
        }
    }

    public byte[] getImageRendition(DocImageRendition rendition)
    {
        if (backendNode instanceof DocImage) {
            try {
                return ((DocImage) backendNode).getRendition(rendition);
            } catch (DocException dex) {
                Log.error("Cannot get image rendition: " + dex.getMessage());
                return new byte[0];
            }
        } else {
            throw new DocRuntimeException("getImageRendition not allowed for node of type " +
                backendNode.getClass().getName());
        }
    }

    public String getContentType()
    {
        if (contentType == null) {
            if (backendNode instanceof DocContent) {
                contentType = ((DocContent) backendNode).getContentType();
            } else {
                throw new DocRuntimeException("getContentType not allowed for node of type " +
                    backendNode.getClass().getName());
            }
        }
        return contentType;
    }

    public String getFileExtension()
    {
        if (fileExtension == null) {
            if (backendNode instanceof DocContent) {
                fileExtension = ((DocContent) backendNode).getFileExtension();
            } else {
                throw new DocRuntimeException("getFileExtension not allowed for node of type " +
                    backendNode.getClass().getName());
            }
        }
        return fileExtension;
    }

    public String getDefaultFileName()
    {
        if (defaultFilename == null) {
            if (isFileContent()) {
                defaultFilename = ((DocFile) backendNode).getFileName();
            } else {
                String fname;
                if (isImageContent()) {
                    String a = getAlias();
                    fname = ((a == null) || a.equals("")) ? getId() : a;
                    String lang_code = getTranslationMode();
                    if ((lang_code != null) && isTranslated()) {
                        fname += "[" + lang_code.toUpperCase() + "]";
                    }
                } else {
                    fname = getTitle();
                }
                if (backendNode instanceof DocContent) {
                    String ext = ((DocContent) backendNode).getFileExtension();
                    if ((ext != null) && (ext.length() > 0)) {
                        fname += "." + ext;
                    }
                }
                defaultFilename = fname;
            }
        }
        return defaultFilename;
    }

    public int getDepth()
    {
        int d = 0;
        DocmaNode n = this;
        while ((n != null) && !n.isDocumentRoot()) {
            d++;
            n = n.getParent();
        }
        return d;
    }

    public int getDepthRelativeTo(String ancestor_id)
    {
        int d = 0;
        DocmaNode n = this;
        while ((n != null) && (! n.isDocumentRoot()) && (! n.getId().equals(ancestor_id))) {
            d++;
            n = n.getParent();
        }
        return d;
    }

    public boolean hasAncestor(String node_id)
    {
        DocmaNode ancestor = this.getParent();
        while (ancestor != null) {
            if (ancestor.getId().equals(node_id)) return true;
            ancestor = ancestor.getParent();
        }
        return false;
    }

    public boolean isAncestor(String node_id)
    {
        if (node_id == null) return false;
        DocmaNode nd = getDocmaSession().getNodeById(node_id);
        if (nd == null) return false;
        return nd.hasAncestor(this.getId());
    }

    public boolean isChildable()
    {
        return !(isContent() || isReference());
    }

    public DocmaNode getParent()
    {
        DocGroup pgroup = backendNode.getParentGroup();
        return getDocmaSession().getDocmaNode(pgroup);
    }

    public DocmaNode getChild(int index)
    {
        initChildren();
        return (children == null) ? null : children[index];
    }

    public DocmaNode getChildByAlias(String alias)
    {
        for (int i=0; i < getChildCount(); i++) {
            DocmaNode n = getChild(i);
            if (alias.equals(n.getAlias())) return n;
        }
        return null;
    }

    public DocmaNode getChildByTitle(String title)
    {
        for (int i=0; i < getChildCount(); i++) {
            DocmaNode n = getChild(i);
            if (title.equals(n.getTitle())) return n;
        }
        return null;
    }

    public DocmaNode getChildByTitleIgnoreCase(String title)
    {
        for (int i=0; i < getChildCount(); i++) {
            DocmaNode n = getChild(i);
            if (title.equalsIgnoreCase(n.getTitle())) return n;
        }
        return null;
    }

    public DocmaNode getChildByTitleAndExtension(String title, String ext)
    {
        for (int i=0; i < getChildCount(); i++) {
            DocmaNode n = getChild(i);
            if (title.equals(n.getTitle())) {
                String n_ext = n.getFileExtension();
                if (ext == null) {
                    if (n_ext == null) return n;
                } else {
                    if (ext.equals(n_ext)) return n;
                }
            }
        }
        return null;
    }

    public DocmaNode getChildByFilename(String filename)
    {
        for (int i=0; i < getChildCount(); i++) {
            DocmaNode n = getChild(i);
            if (filename.equals(n.getDefaultFileName())) {
                return n;
            }
        }
        return null;
    }


    public int getChildCount()
    {
        initChildren();
        return (children == null) ? 0 : children.length;
    }

    public int getChildPos(DocmaNode child) {
        if (isChildable()) {
            return ((DocGroup) backendNode).getChildPos(child.backendNode);
        } else {
            return -1;
        }
    }

    public boolean isRoot()
    {
        return (this == getDocmaSession().root);
    }

    public boolean isDocumentRoot()
    {
        return (this == getDocmaSession().getDocumentRoot());
    }

    public boolean isSystemRoot()
    {
        return (this == getDocmaSession().getSystemRoot());
    }

    public boolean isContent()
    {
        return nodeType.equals(TYPE_CONTENT);
    }

    public boolean isHTMLContent()
    {
        return isContent() && "text/html".equals(getContentType());
    }

    public boolean isImageContent()
    {
        return (backendNode instanceof DocImage);
    }

    public boolean isFileContent()
    {
        return (backendNode instanceof DocFile);
    }

    public boolean isReference()
    {
        return (backendNode instanceof DocReference);
    }

    public boolean isIncludeReference()
    {
        return nodeType.equals(TYPE_SECTION_REFERENCE) ||
               nodeType.equals(TYPE_IMAGE_REFERENCE) ||
               nodeType.equals(TYPE_CONTENT_REFERENCE);
    }

    public boolean isImageIncludeReference()
    {
        return nodeType.equals(TYPE_IMAGE_REFERENCE);
    }

    public boolean isContentIncludeReference()
    {
        return nodeType.equals(TYPE_IMAGE_REFERENCE) ||
               nodeType.equals(TYPE_CONTENT_REFERENCE);
    }

    public boolean isSectionIncludeReference()
    {
        return nodeType.equals(TYPE_SECTION_REFERENCE);
    }

    public boolean isSection()
    {
        return nodeType.equals(TYPE_SECTION);
    }

    public boolean isImageFolder()
    {
        return nodeType.equals(TYPE_IMG_FOLDER);
    }

    public boolean isSystemFolder()
    {
        return isFileFolder();
    }

    public boolean isFileFolder()
    {
        return nodeType.equals(TYPE_SYS_FOLDER);
    }

    public boolean isFolder()
    {
        return isImageFolder() || isFileFolder();
    }
    
    public void setFolderTypeImage()
    {
        if (backendNode instanceof DocGroup) {
            boolean started = startNodeTransaction();
            try {
                backendNode.setAttribute(ATTR_GROUP_TYPE, TYPE_IMG_FOLDER);
                this.nodeType = TYPE_IMG_FOLDER;
                commitNodeTransaction(started);   // GUI update (event processing) is done here
            } catch (Exception ex) {
                rollbackNodeTransaction(started);
                throw new DocRuntimeException(ex);
            }
        } else {
            throw new DocRuntimeException("setFolderTypeImage() not allowed for node of type " + backendNode.getClass().getName());
        }
    }
    
    public void setFolderTypeFile()
    {
        if (backendNode instanceof DocGroup) {
            boolean started = startNodeTransaction();
            try {
                backendNode.setAttribute(ATTR_GROUP_TYPE, TYPE_SYS_FOLDER);
                this.nodeType = TYPE_SYS_FOLDER;
                commitNodeTransaction(started);    // GUI update (event processing) is done here
            } catch (Exception ex) {
                rollbackNodeTransaction(started);
                throw new DocRuntimeException(ex);
            }
        } else {
            throw new DocRuntimeException("setFolderTypeFile() not allowed for node of type " + backendNode.getClass().getName());
        }
    }
    
    public boolean isTextFile()
    {
        if (isFileContent()) {
            DocmaSession sess = getDocmaSession();
            return sess.isTextFileExtension(getFileExtension());
        } else {
            return false;
        }
    }

    public boolean isInsertContentAllowed(int insert_pos)
    {
        int child_cnt = getChildCount();
        if ((insert_pos < 0) || (insert_pos > child_cnt)) {
            return false;
        }
        int first_sect_pos = getChildPosFirstSection();
        if (first_sect_pos < 0) { // if no section exists
            return true;  // allow insert at any position
        } else {  // if section exists
            return insert_pos <= first_sect_pos; // allow insert before first section
        }
    }

    public boolean isInsertSectionAllowed(int insert_pos)
    {
        // int last_cont_pos = getChildPosLastContent();
        int last_cont_pos = -1;
        for (int i=getChildCount()-1; i >= 0; i--) {
            DocmaNode ch = getChild(i);
            if (ch.isHTMLContent() || ch.isContentIncludeReference()) {
                last_cont_pos = i;
                break;
            }
        }
        // If no HTML content exists, new section can be inserted at any position.
        // If HTML content exists, new section has to be inserted after last HTML content.
        return (insert_pos > last_cont_pos) && (insert_pos <= getChildCount());
    }

    public int getChildPosFirstSection()
    {
        for (int i=0; i < getChildCount(); i++) {
            DocmaNode ch = getChild(i);
            if (ch.isSection() || ch.isSectionIncludeReference()) return i;
        }
        return -1;
    }

    public int getChildPosFirstContent()
    {
        for (int i=0; i < getChildCount(); i++) {
            DocmaNode ch = getChild(i);
            if (ch.isContent() || ch.isContentIncludeReference()) return i;
        }
        return -1;
    }

    public int getChildPosLastContent()
    {
        for (int i=getChildCount()-1; i >= 0; i--) {
            DocmaNode ch = getChild(i);
            if (ch.isContent() || ch.isContentIncludeReference()) return i;
        }
        return -1;
    }


    public int getDefaultInsertPos(DocmaNode insert_node)
    {
        boolean sect_ref = false;
        boolean cont_ref = false;
        if (insert_node.isReference()) {
            sect_ref = insert_node.isSectionIncludeReference();
            cont_ref = insert_node.isContentIncludeReference();
            // DocmaNode target_node = insert_node.getReferencedNode();
            // if ((target_node != null) && (target_node.isSection())) sect_ref = true;
            // else cont_ref = true;
        }
        if (insert_node.isContent() || cont_ref) {
            // before first section or append if no section exists
            int first_sect_pos = getChildPosFirstSection();
            if (first_sect_pos < 0) {
                return getChildCount();
            }
            return first_sect_pos;
        } else
        if (insert_node.isSection() || sect_ref) {
            return getChildCount();  // append section
        } else {
            return getChildCount();  // append
        }
    }

    public void removeChild(int index)
        throws IndexOutOfBoundsException
    {
        removeChildren(index, index);
    }


    public void removeChildren(int indexFrom, int indexTo)
        throws IndexOutOfBoundsException
    {
        if (getTranslationMode() != null) {
            throw new DocRuntimeException("Removing nodes in translation mode is not allowed!");
        }
        initChildren();
        if (children == null) throw new DocRuntimeException("Cannot remove child from leaf node");
        if ((indexFrom < 0) || (indexFrom >= children.length)) throw new IndexOutOfBoundsException("Index out of bounds: " + indexFrom);
        if (indexFrom > indexTo) throw new IndexOutOfBoundsException("Invalid index range: IndexFrom is higher than IndexTo.");

        int cnt = indexTo - indexFrom + 1;
        DocmaNode[] tempChildren = children;  // create local variable to avoid problems with
                                              // concurrent access
        DocmaNode[] newarr = new DocmaNode[tempChildren.length - cnt];
        DocGroup backendGroup = (DocGroup) backendNode; // (DocGroup) docsess.getNodeById(id);

        boolean started = startNodeTransaction();
        try {
            int j = 0;
            for (int i = 0; i < tempChildren.length; i++) {
                if ((i < indexFrom) || (i > indexTo)) {
                    newarr[j++] = tempChildren[i];
                } else {
                    backendGroup.removeChild(tempChildren[i].backendNode); // docsess.getNodeById(children[i].id);
                }
            }
            if (children != tempChildren) {
                Log.warning("Concurrent access in DocmaNode.removeChildren()");
            }
            children = newarr;

            if (this.isSection()) this.recalculateSectionAttributes();

            commitNodeTransaction(started);
        } catch (Exception ex) {
            rollbackNodeTransaction(started);
            children = null;  // re-initialize to repair damaged data structure
            // initChildren();
            throw new DocRuntimeException(ex);
        }
    }


    public void addChild(DocmaNode newNode)
    {
        addChildren(new DocmaNode[] {newNode});
    }


    public void addChildren(DocmaNode[] newNodes)
    {
        insertChildren(getChildCount(), newNodes);
    }


    public void insertChild(int index, DocmaNode newNode)
        throws IndexOutOfBoundsException
    {
        insertChildren(index, new DocmaNode[] {newNode});
    }


    public void insertChildren(int indexFrom, DocmaNode[] newNodes)
        throws IndexOutOfBoundsException
    {
        // Note: newNodes can also include nodes which are already children
        //       of this node (i.e. node just changes its index position).
        //       This means the number of children does not necessarily increase
        //       by newNodes.length. Furthermore the index position of the first
        //       inserted node is not necessarily equal to indexFrom after insertion.
        if (getTranslationMode() != null) {
            throw new DocRuntimeException("Inserting nodes in translation mode is not allowed!");
        }
        initChildren();
        if (children == null) throw new DocRuntimeException("Cannot add child to leaf node");
        if ((indexFrom < 0) || (indexFrom > children.length)) throw new IndexOutOfBoundsException("Index out of bounds: " + indexFrom);

        int cnt = newNodes.length;

        HashMap affectedparents = new HashMap();
        affectedparents.put(this.getId(), this);
        for (int i=0; i < cnt; i++) {
            DocmaNode p = newNodes[i].getParent();
            if (p != null) affectedparents.put(p.getId(), p);
        }

        // int indexTo = indexFrom + cnt - 1;
        DocmaNode[] tempChildren = children;  // create local variable to avoid problems with
                                              // concurrent access
        children = null;  // invalidate cache
        // DocmaNode[] newarr = new DocmaNode[tempChildren.length + cnt];

        DocGroup backendGroup = (DocGroup) backendNode; // docsess.getNodeById(id);
        DocNode nodeafter = (indexFrom == tempChildren.length) ? null : tempChildren[indexFrom].backendNode;

        boolean started = startNodeTransaction();
        try {
            // for (int i = 0; i < indexFrom; i++) {
            //     newarr[i] = tempChildren[i];
            // }
            // for (int i = indexFrom; i < tempChildren.length; i++) {
            //     newarr[i + cnt] = tempChildren[i];
            // }
            for (int i = 0; i < cnt; i++) {
                // newarr[indexFrom + i] = newNodes[i];
                DocNode insnode = newNodes[i].backendNode;
                backendGroup.insertBefore(insnode, nodeafter); // nodeafter == null -> appended
            }
            if (children != null) {
                Log.warning("Concurrent access in DocmaNode.insertChildren()");
                children = null;  // invalidate cache again
            }
            // children = newarr;

            // Invalidate cached children of affected parent nodes.
            // Update section attributes (progress, workflow status, ...) of affected parents
            for (Iterator it = affectedparents.values().iterator(); it.hasNext(); ) {
                DocmaNode n = (DocmaNode) it.next();
                n.children = null;  // invalidate cached children
                if (n.isSection()) n.recalculateSectionAttributes();
            }

            commitNodeTransaction(started);
        } catch (Exception ex) {
            rollbackNodeTransaction(started);
            children = null;  // re-initialize to repair damaged data structure
            // initChildren();
            throw new DocRuntimeException(ex);
        }
    }


    public long getContentLength()
    {
        if (backendNode instanceof DocContent) {
            return ((DocContent) backendNode).getContentLength();
        } else {
            return 0;
        }
    }


    public void delete()
    {
        if (getTranslationMode() != null) {
            throw new DocRuntimeException("Deleting nodes in translation mode is not allowed!");
        }
        if (backendNode instanceof DocGroup) {
            throw new DocRuntimeException("Cannot delete group node. Use deleteRecursive instead.");
        }
        boolean started = startNodeTransaction();
        try {
            if (backendNode instanceof DocContent) {
                ((DocContent) backendNode).deleteContent();
                deleteAllContentRevisions();
            }
            DocmaNode par = getParent();
            if (par != null) {
                int pos = par.getChildPos(this);
                if (pos >= 0) par.removeChild(pos);
            }
            commitNodeTransaction(started);
        } catch (Exception ex) {
            rollbackNodeTransaction(started);
            throw new DocRuntimeException(ex);
        }
        docmaSess.removeDocmaNodeFromCache(getId());
    }

    public void deleteContent()
    {
        if (backendNode instanceof DocContent) {
            // boolean started = startNodeTransaction();
            // try {
            ((DocContent) backendNode).deleteContent();
            //     deleteAllContentRevisions();
            //     commitNodeTransaction(started);
            // } catch (Exception ex) {
            //     rollbackNodeTransaction(started);
            //     throw new DocRuntimeException(ex);
            // }
        }
    }
    
    public void deleteContentRecursive()
    {
        if (getTranslationMode() != null) {
            throw new DocRuntimeException("Deleting nodes in translation mode is not allowed!");
        }
        if (backendNode instanceof DocContent) {
            boolean started = startNodeTransaction();
            try {
                ((DocContent) backendNode).deleteContent();
                deleteAllContentRevisions();
                commitNodeTransaction(started);
            } catch (Exception ex) {
                rollbackNodeTransaction(started);
                throw new DocRuntimeException(ex);
            }
        } else
        if (backendNode instanceof DocGroup) {
            for (int i=0; i < getChildCount(); i++) {
                getChild(i).deleteContentRecursive();
            }
        }
    }


    public void deleteRecursive()
    {
        ArrayList all_nodes = new ArrayList(500);
        getNodesRecursive(all_nodes);

        deleteContentRecursive();
        DocmaNode par = getParent();
        if (par != null) {
            int pos = par.getChildPos(this);
            if (pos >= 0) par.removeChild(pos);
        }
        // Remove deleted nodes from cache
        for (int i=0; i < all_nodes.size(); i++) {
            DocmaNode del_node = (DocmaNode) all_nodes.get(i);
            docmaSess.removeDocmaNodeFromCache(del_node.getId());
        }
    }


    public void sortByFilename()
    {
        if (! (isImageFolder() || isSystemFolder())) {
            throw new DocRuntimeException("Sort by filename is only allowed for folder nodes!");
        }
        int cnt = getChildCount();
        if (cnt <= 1) return;
        DocmaNode[] nodes = new DocmaNode[cnt];
        for (int i=0; i < cnt; i++) nodes[i] = getChild(i);
        Arrays.sort(nodes, new NodeFilenameComparator());
        boolean started = startNodeTransaction();
        try {
            removeChildren(0, cnt-1);
            addChildren(nodes);
            commitNodeTransaction(started);
        } catch (Exception ex) {
            rollbackNodeTransaction(started);
        }
    }
    
    private void deleteAllContentRevisions()
    {
        RevisionStoreSession revstore = docmaSess.getRevisionStore();
        if (revstore == null) {  // revisions are not supported
            return;
        }
        try {
            revstore.deleteRevisions(docmaSess.getStoreId(), docmaSess.getVersionId(), this.id);
        } catch (Exception ex) {
            Log.error("Failed to delete revisions for node " + this.id + ": " + ex.getMessage());
        }
    }

    public DocContentRevision[] getRevisions()
    {
        if (! isContent()) {
            return null;  // return null if node does not support revisions
        }
        DocmaSession sess = getDocmaSession();
        RevisionStoreSession revstore = sess.getRevisionStore();
        if (revstore == null) {  // revisions are not supported
            return new DocContentRevision[0];
        }
        String storeId = sess.getStoreId();
        DocVersionId verId = sess.getVersionId();
        // UUID ver_uuid = sess.getBackendSession().getVersionUUID(storeId, verId);
        String lang_code = getTranslationMode();
        return revstore.getRevisions(storeId, verId, this.id, lang_code);
    }

    public boolean makeRevision()
    {
        if (! isContent()) {
            return false;
        }
        DocmaSession sess = getDocmaSession();
        RevisionStoreSession revstore = sess.getRevisionStore();
        if (revstore == null) {  // revisions are not supported
            return false;
        }
        String storeId = sess.getStoreId();
        DocVersionId verId = sess.getVersionId();
        // UUID ver_uuid = sess.getBackendSession().getVersionUUID(storeId, verId);
        String lang_code = getTranslationMode();
        refresh();  // clear cached values to get latest modified-date/user/content
        revstore.addRevision(storeId, verId, this.id, lang_code, getContent(),
                             getLastModifiedDate(), getLastModifiedBy());
        return true;
    }

    /* --------------  private helper methods  ---------------------- */


    private void initChildren()
    {
        if (isChildable()) {
            if (children == null) {
                DocGroup backendGroup = (DocGroup) backendNode;
                DocNode[] arr = backendGroup.getChildNodes();
                if (arr != null) {
                    DocmaSession sess = getDocmaSession();
                    children = new DocmaNode[arr.length];
                    for (int i=0; i < arr.length; i++) {
                        children[i] = sess.getDocmaNode(arr[i]);
                    }
                    // childrenReadOnly = Arrays.asList(children);
                } else {
                    children = new DocmaNode[0];
                }
            }
        } else {
            children = null;
        }
    }

    private boolean startNodeTransaction()
    {
        if (node_transaction) {
            return false;
            // throw new DocRuntimeException("Cannot start local transaction: local transaction already running.");
        } else {
            // if a transaction is not already running, then start
            // a "local" transaction
            if (docmaSess.runningTransaction()) {
                return false;
            } else {
                try {
                    docmaSess.startTransaction();
                    node_transaction = true;
                    return true;
                } catch (DocException dex) {
                    throw new DocRuntimeException(dex);
                }
            }
        }
    }

    private void commitNodeTransaction(boolean started)
    {
        if (! started) return;
        if (node_transaction) {
            try {
                docmaSess.commitTransaction();
                node_transaction = false;
            } catch (DocException dex) {
                throw new DocRuntimeException(dex);
            }
        } else {
            throw new DocRuntimeException("Cannot commit node transaction: no node transaction running.");
        }
    }

    private void rollbackNodeTransaction(boolean started)
    {
        if (! started) return;
        if (node_transaction) {
            try {
                docmaSess.rollbackTransaction();
            } finally {
                node_transaction = false;
            }
        } else {
            throw new DocRuntimeException("Cannot rollback node transaction: no node transaction running.");
        }
    }

}
