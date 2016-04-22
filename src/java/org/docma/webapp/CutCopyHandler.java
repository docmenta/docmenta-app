/*
 * CutCopyHandler.java
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

package org.docma.webapp;

import org.docma.coreapi.*;
import org.docma.app.*;
import org.docma.util.Log;
import java.util.*;
import java.text.MessageFormat;
import org.zkoss.zul.*;

/**
 *
 * @author MP
 */
public class CutCopyHandler
{
    private static final int MODE_CUT = 1;
    private static final int MODE_COPY = 2;

    private MainWindow mainwin;
    private List cutCopyList = new ArrayList();
    private int listMode = -1;

    public CutCopyHandler(MainWindow mainwin)
    {
        this.mainwin = mainwin;
    }

    public void cut(Tree docTree) throws Exception
    {
        clearCutCopyList();
        List nodes = getSelectedDocmaNodes(docTree);
        if (nodes == null) return;   // selection was not valid

        cutCopyList.addAll(nodes);
        listMode = MODE_CUT;
        debugInfoCutCopyList();
        redrawNodes(cutCopyList);
    }

    public void copy(Tree docTree) throws Exception
    {
        clearCutCopyList();
        List nodes = getSelectedDocmaNodes(docTree);
        if (nodes == null) return;   // selection was not valid

        cutCopyList.addAll(nodes);
        listMode = MODE_COPY;
        debugInfoCutCopyList();
    }

    public boolean hasCutNodes()
    {
        return (! isCutCopyListEmpty()) && (listMode == MODE_CUT);
    }

    public boolean hasCopyNodes()
    {
        return (! isCutCopyListEmpty()) && (listMode == MODE_COPY);
    }

    public boolean isInCutList(DocmaNode node)
    {
        if (hasCutNodes()) {
            return cutCopyList.contains(node);
        } else {
            return false;
        }
    }

    public void pasteHere(Tree docTree) throws Exception
    {
        List nodes = getSelectedDocmaNodes(docTree);
        if (nodes == null) return;   // selection was not valid

        DocmaNode insertBefore = (DocmaNode) nodes.get(0);
        DocmaNode parentNode = insertBefore.getParent();
        paste(parentNode, insertBefore);
    }

    public void pasteSub(Tree docTree) throws Exception
    {
        List nodes = getSelectedDocmaNodes(docTree);
        if (nodes == null) return;   // selection was not valid

        DocmaNode parentNode = (DocmaNode) nodes.get(0);
        paste(parentNode, null);
    }

    public void clearCutCopyList()
    {
        List tempList = cutCopyList;
        cutCopyList = new ArrayList(); // cutCopyList.clear();
        redrawNodes(tempList);
    }

    public boolean isCutCopyListEmpty()
    {
        return cutCopyList.isEmpty();
    }

    public void delete(Tree docTree) throws Exception
    {
        List nodes = getSelectedDocmaNodes(docTree);
        if (nodes == null) return;   // selection was not valid

        DocmaSession docmaSess = mainwin.getDocmaSession();
        DocmaI18 i18 = mainwin.getDocmaI18();
        boolean isTransMode = docmaSess.getTranslationMode() != null;
        String msg_pattern_name = isTransMode ? i18.getLabel("label.confirm.delete.translation") :
                                                i18.getLabel("label.confirm.delete.node");
        String msg_pattern_cnt = isTransMode ? i18.getLabel("label.confirm.delete.translationcount") :
                                               i18.getLabel("label.confirm.delete.nodecount");
        String msg = null;
        int cnt = nodes.size();
        if (cnt == 1) {
            DocmaNode delnode = (DocmaNode) nodes.get(0);
            String name = null;
            if (delnode.isFileContent() || delnode.isImageContent()) {
                name = delnode.getDefaultFileName();
            } else {
                name = delnode.getTitle();
            }
            if ((name == null) || name.equals("")) {
                name = delnode.getAlias();
            }
            if (name != null) {
                // Format message: "Delete node 'name'?" or "Delete translation of 'name'?"
                msg = MessageFormat.format(msg_pattern_name, name);
            }
        }
        if (msg == null) {
            // Format message: "Delete cnt nodes?" or "Delete translation of cnt nodes?"
            msg = MessageFormat.format(msg_pattern_cnt, "" + cnt);
        }
        if (Messagebox.show(msg, "Confirm deletion",
            Messagebox.YES | Messagebox.NO, Messagebox.QUESTION) == Messagebox.YES) {

            docmaSess.startTransaction();
            try {
                for (int i=0; i < nodes.size(); i++) {
                    DocmaNode delnode = (DocmaNode) nodes.get(i);
                    if (isTransMode) {
                        if (DocmaConstants.DEBUG) {
                            System.out.println("Delete translation: " + delnode.getId() + "/" + delnode.getTitle());
                        }
                        delnode.deleteTranslation();
                    } else {
                        if (DocmaConstants.DEBUG) {
                            System.out.println("Delete node: " + delnode.getId() + "/" + delnode.getTitle());
                        }
                        delnode.deleteRecursive();
                    }
                }
                docmaSess.commitTransaction();
            } catch (Exception ex) {
                docmaSess.rollbackTransaction();
                throw ex;
            }
        }
    }

    public String getCutIdsAsJavascriptArray()
    {
        StringBuilder buf = new StringBuilder();
        buf.append("[");
        if (hasCutNodes()) {
            boolean isFirst = true;
            for (Object obj : cutCopyList) {
                if (obj instanceof DocmaNode) {
                    String node_id = ((DocmaNode) obj).getId();
                    if (isFirst) isFirst = false;
                    else buf.append(',');
                    buf.append('"').append(node_id).append('"');
                }
            }
        }
        buf.append("]");
        return buf.toString();
    }

    /* --------------  Private methods  --------------- */

    private void redrawNodes(List node_list)
    {
        DocmaWebTreeModel t_model = mainwin.getDocTreeModel();
        for (int i=0; i < node_list.size(); i++) {
            DocmaNode nd = (DocmaNode) node_list.get(i);
            t_model.redrawDocmaNode(nd);
        }
    }

    private void paste(DocmaNode parentNode, DocmaNode insertBefore) throws Exception
    {
        Set insertpath_ids = new HashSet();
        DocmaNode n;
        if (listMode == MODE_COPY) {
            // Note: in copy-mode: a copy of a node can be inserted before itself
            n = parentNode;
        } else {
            n = (insertBefore != null) ? insertBefore : parentNode;
        }
        do {
            insertpath_ids.add(n.getId());
            n = n.getParent();
        } while(n != null);

        boolean hasContent = false;
        boolean hasSection = false;
        for (int i=0; i < cutCopyList.size(); i++) {
            DocmaNode nd = (DocmaNode) cutCopyList.get(i);
            if (insertpath_ids.contains(nd.getId())) {  // insert position is child of node to be inserted
                Messagebox.show("Cannot insert nodes here!");
                return;
            }
            if (nd.isContent() || nd.isContentIncludeReference()) {
                hasContent = true;
            } else
            if (nd.isSection() || nd.isSectionIncludeReference()) {
                hasSection = true;
            }
        }

        int insertPos = -1;
        if (insertBefore == null) {
            if (hasContent) {
                insertPos = parentNode.getChildPosFirstSection();
            }
            if (insertPos < 0) {
                insertPos = parentNode.getChildCount();  // add to end of list
            }
        } else {
            insertPos = parentNode.getChildPos(insertBefore);
        }
        if (hasContent) {
            if (! parentNode.isInsertContentAllowed(insertPos)) {
                Messagebox.show("Cannot insert content here!");
                return;
            }
        }
        if (hasSection) {
            if (! parentNode.isInsertSectionAllowed(insertPos)) {
                Messagebox.show("Cannot insert section here!");
                return;
            }
        }

        DocmaNode[] arr = new DocmaNode[cutCopyList.size()];
        arr = (DocmaNode[]) cutCopyList.toArray(arr);
        // fixNodeOrder(arr);
        DocmaSession docmaSess = mainwin.getDocmaSession();
        try {
            docmaSess.startTransaction();
            if (listMode == MODE_CUT) {
                parentNode.insertChildren(insertPos, arr);
            } else
            if (listMode == MODE_COPY) {
                docmaSess.copyNodes(arr, parentNode, insertPos);
            } else {
                Log.error("CutCopyHandler: Unknown mode '" + listMode + "'");
            }
            docmaSess.commitTransaction();
        } catch (Exception ex) {
            ex.printStackTrace();
            docmaSess.rollbackTransaction();
            Messagebox.show("Internal error: " + ex.getMessage());
        } finally {
            // try {
            clearCutCopyList();
            // } catch(Exception ex) {}
        }
    }

    private void fixNodeOrder(DocmaNode[] arr) {
        // Content nodes should be before section nodes
        // for (int i = 0; i < arr.length; i++) {
        // }
    }

    private List getSelectedDocmaNodes(Tree docTree) throws Exception
    {
        return GUIUtil.getSelectedDocmaNodes(docTree);
    }

    private void debugInfoCutCopyList()
    {
        if (DocmaConstants.DEBUG) {
            System.out.println("Cut list size: " + cutCopyList.size());
            for (int i=0; i < cutCopyList.size(); i++) {
                DocmaNode nd = (DocmaNode) cutCopyList.get(i);
                System.out.println("  " + i + ": " + nd.getTitle());
            }
        }
    }
}
