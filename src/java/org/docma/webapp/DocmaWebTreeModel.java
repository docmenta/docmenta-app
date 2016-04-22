/*
 * DocmaWebTreeModel.java
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
import org.docma.lockapi.*;
import org.docma.app.*;
import org.docma.util.Log;
import org.docma.coreapi.implementation.EventQueueUtil;

import org.zkoss.zul.AbstractTreeModel;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.event.TreeDataEvent;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Component;

import java.util.*;

/**
 *
 * @author MP
 */
public class DocmaWebTreeModel extends AbstractTreeModel implements DocListener, LockListener
{
    private MainWindow mainWin;
    private DocmaSession docmaSess;
    private List deferredEventsQueue = new LinkedList();

    /* --------------  Constructor  ---------------------- */

    public DocmaWebTreeModel(MainWindow mainWin, DocmaSession docmaSess) {
        super(docmaSess.getRoot());
        this.mainWin = mainWin;
        this.docmaSess = docmaSess;
        if (getRoot() == null) {
            throw new DocRuntimeException("DocumentRoot of DocmaSession is null.");
        }
        setMultiple(true);
    }

    /* --------------  Interface Treemodel  ---------------------- */

    public boolean isLeaf(Object item) {
        if (item instanceof DocmaNode) {
            DocmaNode node = (DocmaNode) item;
            if (node.isChildable()) {
                return (node.getChildCount() == 0);
            } else {
                if (node.isHTMLContent() && node.hasContentAnchor()) {
                    return false;  // html content is not a leaf if it has anchors
                } else {
                    return true;  // html without anchors or any other content is a leaf
                }
            }
            // return !itemModel.isChildable();
        } else {
            // if (item instanceof DocmaAnchor)
            return true;  // an anchor is always a leaf
        }
    }

    public Object getChild(Object parent, int index) {
        if (parent instanceof DocmaNode) {
            DocmaNode node = (DocmaNode) parent;
            if (node.isHTMLContent()) {  // if html then child is an anchor
                return node.getContentAnchor(index);
            } else {  // content section or folder
                return node.getChild(index);
            }
        } else {
            return null;
        }
    }

    public int getChildCount(Object item) {
        if (item instanceof DocmaNode) {
            DocmaNode node = (DocmaNode) item;
            if (node.isHTMLContent()) {  // if html then child is an anchor
                return node.getContentAnchorCount();
            } else {
                return node.getChildCount();  // content section or folder
            }
        } else {
            return 0;   // DocmaAnchor
        }
    }

    @Override
    public int[] getPath(Object child) 
    {
        if (child instanceof DocmaNode) {
            return GUIUtil.getDocmaTreePath((DocmaNode) child);
        } else 
        if (child instanceof DocmaAnchor) {
            DocmaAnchor anch = (DocmaAnchor) child;
            DocmaNode nd = anch.getNode();
            int anch_cnt = nd.getContentAnchorCount();
            int anch_pos = 0;
            if (anch_cnt > 1) {
                String alias = anch.getAlias();
                for (int i=0; i < anch_cnt; i++) {
                    if (alias.equals(nd.getContentAnchor(i).getAlias())) {
                        anch_pos = i;
                        break;
                    }
                }
            }
            int[] temp = GUIUtil.getDocmaTreePath(nd);
            int[] path = Arrays.copyOf(temp, temp.length + 1);
            path[temp.length] = anch_pos;
            if (DocmaConstants.DEBUG) {
                System.out.println("Getting path for anchor '" + anch.getAlias() + 
                                   "' of node " + nd.getId() + ". Anchor count: " + anch_cnt);
                StringBuilder sb = new StringBuilder("Anchor Path:");
                for (int p : path) sb.append(" " + p);
                System.out.println(sb.toString());
            }
            return path;
        } else {
            return null;
        }
    }

    

    /* --------------  Interface DocListener  ---------------------- */

    public synchronized void event(DocEvent evt)
    {
        if (mustDefer()) {
            deferEvent(evt);  // defer to event listener thread of mainWin
        } else {
            // Current thread is an event listener thread of mainWin
            processDocEvent(evt);  // process within this thread
        }
    }

    /* --------------  Interface LockListener  ---------------------- */

    public synchronized void lockAdded(Lock lock)
    {
        if (mustDefer()) {
            deferEvent(lock);  // defer to event listener thread of mainWin
        } else {
            // Current thread is an event listener thread of mainWin
            processLockEvent(lock);  // process within this thread
        }
    }

    public synchronized void lockRemoved(Lock lock)
    {
        if (mustDefer()) {
            deferEvent(lock);  // defer to event listener thread of mainWin
        } else {
            // Current thread is an event listener thread of mainWin
            processLockEvent(lock);  // process within this thread
        }
    }

    public synchronized void lockTimeout(Lock lock)
    {
        if (mustDefer()) {
            deferEvent(lock);  // defer to event listener thread of mainWin
        } else {
            // Current thread is an event listener thread of mainWin
            processLockEvent(lock);  // process within this thread
        }
    }


    /* --------------  Package local methods  ---------------------- */

    synchronized void processDeferredEvents()
    {
        if (DocmaConstants.DEBUG) {
            System.out.println("Processing deferred events: " + deferredEventsQueue.size());
        }
        EventQueueUtil.compressEvents(deferredEventsQueue);
        if (DocmaConstants.DEBUG) {
            System.out.println("Deferred queue size after compression: " + deferredEventsQueue.size());
        }
        while (! deferredEventsQueue.isEmpty()) {
            Object obj = deferredEventsQueue.get(0);
            if (obj instanceof DocEvent) {
                processDocEvent((DocEvent) obj);
            } else
            if (obj instanceof Lock) {
                processLockEvent((Lock) obj);
            }
            deferredEventsQueue.remove(0);
        }
    }

    void redrawDocmaNode(String nodeId)
    {
        if (nodeId != null) {
            DocmaNode node = docmaSess.getNodeById(nodeId);
            redrawDocmaNode(node);
        }
    }

    void redrawDocmaNode(DocmaNode node)
    {
        if (node != null) {
            DocmaNode pNode = node.getParent();
            if (pNode != null) {
                int idx = pNode.getChildPos(node);
                DocmaWebTree docmaTree = mainWin.getDocTree();
                Treeitem pItem = getTreeitemByDocmaNode(docmaTree, pNode);
                fireTreeEvent(docmaTree, pItem, pNode, idx, idx, TreeDataEvent.CONTENTS_CHANGED);
            }
        }
    }


    /* --------------  Private helper methods  ---------------------- */

    private boolean mustDefer()
    {
        // If this thread belongs to another desktop than the main window of
        // this event listener, then the event has to be deferred.
        // Returns true if current thread belongs to another desktop.
        Execution curr_exec = Executions.getCurrent();
        if (curr_exec == null) return true;
        Desktop curr_desk = curr_exec.getDesktop();
        if (curr_desk == null) return true;
        Desktop self_desk = mainWin.getDesktop();
        if (self_desk == null) {
            Log.warning("DocmaWebTreeModel.mustDefer(): main window no longer connected to desktop!");
            return true;
        }

        return ! curr_desk.getId().equals(self_desk.getId());
    }

    private void deferEvent(Object evt)
    {
        if (DocmaConstants.DEBUG) {
            System.out.println("Defer event: " + evt);
        }
        deferredEventsQueue.add(evt);
    }

    private void processDocEvent(DocEvent evt)
    {
        String evtname = evt.getEventName();
        if (DocEvent.NODES_STRUCTURE_CHANGED.equals(evtname)) {
            // This event is required to fix ZK tree rendering bug.
            // See org.docma.coreapi.implementation.EventQueueUtil.compressEvents().
            fireEvent(TreeDataEvent.STRUCTURE_CHANGED, null, -1, -1);
            return;
        }
        String pId = evt.getParentId();
        DocmaWebTree docmaTree = mainWin.getDocTree();
        DocmaNode pNode = docmaSess.getNodeById(pId);
        if (pNode == null) {
            Log.error("Cannot process DocEvent: Parent node with Id '" + pId + "' does not exist.");
            return;
        }
        pNode.refresh();  // if event was caused by another session/user, node has to be refreshed

        Treeitem pItem = null;
        String[] child_ids = null;
        if (pNode.isRoot()) {
            child_ids = getChildIds(docmaTree);
        } else {
            pItem = getTreeitemByDocmaNode(docmaTree, pNode);
            if (pItem == null) return;  // tree item not found

            if (! pItem.isLoaded()) {
                if (DocmaConstants.DEBUG) Log.info("Parent node not loaded. Loading: " + pId);
                // boolean isopen = pItem.isOpen();
                docmaTree.renderItem(pItem, pNode);
                // pItem.setOpen(isopen);
            }
            child_ids = getChildIds(pItem);
        }
        if (child_ids == null) {
            // refreshTreeitem(docmaTree, pItem, pNode);
            return;  // error: invalid tree
        }

        if (evtname.equals(DocEvent.NODES_ADDED)) {
            processDocEvent_AddedNodes(docmaTree, pItem, pNode, child_ids, evt);
        } else
        if (evtname.equals(DocEvent.NODES_CHANGED)) {
            processDocEvent_ChangedNodes(docmaTree, pItem, pNode, child_ids, evt);
        } else
        if (evtname.equals(DocEvent.NODES_REMOVED)) {
            processDocEvent_RemovedNodes(docmaTree, pItem, pNode, child_ids, evt);
        } else {
            Log.warning("Unknown DocEvent: " + evtname);
        }
    }

    private void processDocEvent_AddedNodes(DocmaWebTree docmaTree,
                                            Treeitem pItem,
                                            DocmaNode pNode,
                                            String[] guiChildIds,
                                            DocEvent evt)
    {
        // Try to synchronize GUI with model by adding nodes to GUI list.
        // If this is not possible, then the complete GUI node has to be refreshed.
        int gui_idx = 0;
        String gui_id = (guiChildIds.length > 0) ? guiChildIds[0] : null;
        int range_start = -1;
        int range_end = -1;
        int model_cnt = pNode.getChildCount();
        ArrayList ranges = new ArrayList(model_cnt);

        for (int i=0; i < model_cnt; i++) {
            String model_id = pNode.getChild(i).getId();
            if (gui_id != null) {
                if (gui_id.equals(model_id)) {
                    // Node in GUI is equal to node in model
                    if (range_start >= 0) {  // previous range to be added exists
                        // Store range to be added and start new range
                        ranges.add(new IndexRange(range_start, range_end));
                        range_start = -1;
                        range_end = -1;
                    }
                    // Jump to next GUI node
                    gui_idx++;
                    gui_id = (gui_idx < guiChildIds.length) ? guiChildIds[gui_idx] : null;
                } else {
                    // Node in model differs from node in GUI -> node has to be added to GUI
                    if (range_start < 0) {
                        range_start = i;  // start new range
                        range_end = i;
                    } else {
                        range_end++;    // extend current range
                    }
                }
            } else {    // End of GUI list reached
                // This part can only be reached when range_start is -1,
                // i.e. when GUI is empty or last model node was equal to last GUI node.
                if (range_start < 0) {
                    // Add remaining nodes to GUI list
                    range_start = i;
                    range_end = model_cnt - 1;
                    ranges.add(new IndexRange(range_start, range_end));
                    range_start = -1;
                    range_end = -1;
                } else {
                    Log.error("Unexpected range_start value in DocmaWebTreeModel.processDocEvent_AddedNodes: " + range_start);
                }
                break;
            }
        }
        if ((range_start >= 0) || (ranges.size() > 1)) {  // GUI contains node which does not exist in model
                                                          // or more than one range to add
            // Synchronization not possible by adding single range of GUI nodes -> refresh GUI node
            Log.info("processDocEvent_AddedNodes: Model and GUI out of sync. Refreshing node " + pNode.getId());
            refreshOutOfSyncTreeitem(docmaTree, pItem, pNode);
            return;
        }

        for (int i=0; i < ranges.size(); i++) {
            IndexRange range = (IndexRange) ranges.get(i);
            fireTreeEvent(docmaTree, pItem, pNode, range.indexFrom, range.indexTo, TreeDataEvent.INTERVAL_ADDED);
        }
        
        // Following is required, otherwise added nodes to an empty section are not displayed 
        if (ranges.isEmpty()) {  // no tree event has been fired
            if (DocmaConstants.DEBUG) Log.info("Nodes added to empty parent? Invalidating parent item.");
            if (pItem != null) pItem.invalidate();
        }
    }

    private void processDocEvent_RemovedNodes(DocmaWebTree docmaTree,
                                              Treeitem pItem,
                                              DocmaNode pNode,
                                              String[] guiChildIds,
                                              DocEvent evt)
    {
        // Try to synchronize GUI with model by removing nodes from GUI list.
        // If this is not possible, then the complete GUI node has to be refreshed.
        int model_cnt = pNode.getChildCount();
        int model_idx = 0;
        String model_id = (model_cnt > 0) ? pNode.getChild(0).getId() : null;
        int range_start = -1;
        int range_end = -1;
        ArrayList ranges = new ArrayList(guiChildIds.length);
        for (int i=0; i < guiChildIds.length; i++) {
            String gui_id = guiChildIds[i];
            if (model_id != null) {
                if (model_id.equals(gui_id)) {
                    // Node in GUI is equal to node in model
                    if (range_start >= 0) {  // previous range to be removed exists
                        // Store range to be removed and start new range
                        ranges.add(new IndexRange(range_start, range_end));
                        range_start = -1;
                        range_end = -1;
                    }
                    // Jump to next model node
                    model_idx++;
                    model_id = (model_idx < model_cnt) ? pNode.getChild(model_idx).getId() : null;
                } else {
                    // Node in GUI differs from node in model -> node has to be removed in GUI
                    if (range_start < 0) {
                        range_start = model_idx;  // start new range
                        range_end = model_idx;
                    } else {
                        range_end++;    // extend current range
                    }
                }
            } else {  // End of model list reached
                // This part can only be reached when range_start is -1,
                // i.e. when model is empty or last GUI node was equal to last model node.
                if (range_start < 0) {
                    // Remove remaining nodes from GUI list
                    range_start = model_idx;
                    range_end = model_idx + guiChildIds.length - i - 1;
                    ranges.add(new IndexRange(range_start, range_end));
                    range_start = -1;
                    range_end = -1;
                } else {
                    Log.error("Unexpected range_start value in DocmaWebTreeModel.processDocEvent_RemovedNodes: " + range_start);
                }
                break;
            }
        }
        if ((range_start >= 0) || (ranges.size() > 1)) {  // Model contains node which does not exist in GUI
                                                          // or more than one range to delete
            // Synchronization not possible by single range removal of GUI nodes -> refresh GUI node
            Log.info("processDocEvent_RemovedNodes: Model and GUI out of sync. Refreshing node " + pNode.getId());
            refreshOutOfSyncTreeitem(docmaTree, pItem, pNode);
            return;
        }

        for (int i=0; i < ranges.size(); i++) {
            IndexRange range = (IndexRange) ranges.get(i);
            fireTreeEvent(docmaTree, pItem, pNode, range.indexFrom, range.indexTo, TreeDataEvent.INTERVAL_REMOVED);
        }
    }

    private void processDocEvent_ChangedNodes(DocmaWebTree docmaTree,
                                              Treeitem pItem,
                                              DocmaNode pNode,
                                              String[] guiChildIds,
                                              DocEvent evt)
    {
        int evtType = TreeDataEvent.CONTENTS_CHANGED;
        int range_start = -1;
        int range_end = -1;
        Set change_ids = evt.getNodeIds();
        for (int i=0; i < guiChildIds.length; i++) {
            if (change_ids.contains(guiChildIds[i])) {  // node at index i changed
                if (range_start < 0) {
                    range_start = i;   // start new range
                    range_end = i;
                } else {
                    range_end = i;       // extend range
                }
            } else {  // node at index i did not change
                if (range_start >= 0) {  // if previous range exists
                    // notify changes
                    fireTreeEvent(docmaTree, pItem, pNode, range_start, range_end, evtType);
                    range_start = -1;
                    range_end = -1;
                }
            }
        }
        if (range_start >= 0) {  // if previous range exists
            // notify changes
            fireTreeEvent(docmaTree, pItem, pNode, range_start, range_end, evtType);
        }
    }

    private void processLockEvent(Lock lock)
    {
        String nodeId = lock.getLockedObjectId();
        if (nodeId != null) {
            DocmaNode node = docmaSess.getNodeById(nodeId);
            if (node != null) {
                DocmaNode pNode = node.getParent();
                if (pNode != null) {
                    int idx = pNode.getChildPos(node);
                    node.refresh();  // if event was caused by another session/user, node has to be refreshed
                    if (DocmaConstants.DEBUG) {
                        System.out.println("Fire Lock Changed Event. Parent: " + pNode.getId() + 
                                           " Child index: " + idx);
                    }
                    // DocmaWebTree docmaTree = mainWin.getDocTree();
                    // Treeitem pItem = getTreeitemByDocmaNode(docmaTree, pNode);
                    // fireTreeEvent(docmaTree, pItem, pNode, idx, idx, TreeDataEvent.CONTENTS_CHANGED);
                    fireEvent(TreeDataEvent.CONTENTS_CHANGED, GUIUtil.getDocmaTreePath(pNode), idx, idx);
                }
            }
        }
    }

    private void fireTreeEvent(DocmaWebTree docmaTree,
                               Treeitem pItem,
                               DocmaNode pNode,
                               int indexFrom,
                               int indexTo,
                               int evtType)
    {
        // Note: pItem is null if pNode is the root of the tree, i.e. docmaSess.getRoot().
        //       This happens when an event occurs for one of the visible root folders.
        if (DocmaConstants.DEBUG) {
            System.out.println("Fire Tree Event. Parent: " + pNode.getId() + ". From child index: " + 
                               indexFrom + " To: " + indexTo + " Type: " + evtType);
        }
        if (evtType == TreeDataEvent.CONTENTS_CHANGED) {
            for (int i = indexFrom; i <= indexTo; i++) {
                try {
                    DocmaNode childnode = pNode.getChild(i);
                    childnode.refresh(); // if event was caused by another session/user, node has to be refreshed

                    if (childnode.isHTMLContent()) {
                        Treeitem childitem = getChildItem(pItem, childnode.getId()); // docmaTree.getTreeitemByDocmaNode(childnode);
                        if (childitem == null) {
                            Log.error("DocmaWebTreeModel.fireTreeEvent: Treeitem not found for node " +
                                      childnode.getId());
                        } else {
                            refreshHTMLContentTreeitem(docmaTree, childitem, childnode);
                        }
                    }
                } catch (Exception ex) {
                    // log error and ignore
                    Log.error("Exception in DocmaWebTreeModel.fireTreeEvent at index position " +
                              i + " event type " + evtType + ": " + ex.getMessage());
                }
            }
        }
        // fireEvent(pNode, indexFrom, indexTo, evtType);
        fireEvent(evtType, GUIUtil.getDocmaTreePath(pNode), indexFrom, indexTo);
    }

    private void refreshHTMLContentTreeitem(DocmaWebTree docmaTree, Treeitem tree_item, DocmaNode model)
    {
        if (tree_item == null) {
            Log.warning("Cannot refresh HTML content tree item. Item is null.");
            return;
        }
        if (DocmaConstants.DEBUG) {
            System.out.println("Refreshing tree item for HTML content node " + model.getId());
        }
        int child_count = 0;
        Treechildren tchildren = tree_item.getTreechildren();
        if (tchildren != null) {
            List treeItems = tchildren.getChildren();
            if (treeItems != null) {
                child_count = treeItems.size();
            }
        }
        int anch_count = model.getContentAnchorCount();
        boolean added = (anch_count > child_count);
        boolean removed = (child_count > anch_count);
        int min_count = Math.min(child_count, anch_count);
        int[] path = GUIUtil.getDocmaTreePath(model);
        // if (DocmaConstants.DEBUG) {
        //     System.out.println("Tree item count: " + child_count + ",  Anchor count: " + anch_count);
        //     StringBuilder sb = new StringBuilder("HTML content path:");
        //     for (int p : path) sb.append(" " + p);
        //     System.out.println(sb.toString());
        // }
        if (min_count > 0) {
            if (DocmaConstants.DEBUG) System.out.println("Fire anchors changed: 0 to " + (min_count - 1));
            fireEvent(TreeDataEvent.CONTENTS_CHANGED, path, 0, min_count - 1);
        }
        if (added) {
            if (DocmaConstants.DEBUG) System.out.println("Fire anchors added: " + child_count + " to " + (anch_count - 1));
            // if (child_count == 0) {  // if previously no anchors existed open node to display added anchors
            //     addOpenPath(path);
            // } else
            fireEvent(TreeDataEvent.INTERVAL_ADDED, path, child_count, anch_count - 1);
            tree_item.invalidate();  // required, otherwise "failed to mount" error (ZK bug?)
        }
        if (removed) {
            if (DocmaConstants.DEBUG) System.out.println("Fire anchors removed: " + anch_count + " to " + (child_count - 1));
            fireEvent(TreeDataEvent.INTERVAL_REMOVED, path, anch_count, child_count - 1);
        }
//        int[] path = GUIUtil.getDocmaTreePath(model);
//        boolean isopen = tree_item.isOpen();
//        if (isopen) removeOpenPath(path);
//        tree_item.unload();
//        if (DocmaConstants.DEBUG) {
//            System.out.println("Tree item unloaded.");
//        }
//        docmaTree.renderItem(tree_item, model);
//        if (DocmaConstants.DEBUG) {
//            System.out.println("Tree item refreshed.");
//        }
//        // tree_item.setOpen(isopen);
//        if (isopen) { 
//            addOpenPath(path);
//        }
    }
    
    private void refreshOutOfSyncTreeitem(DocmaWebTree docmaTree, Treeitem tree_item, DocmaNode model)
    {
        if (tree_item == null) {
            Log.warning("Cannot refresh out of sync tree item. Item is null.");
            return;
        }
        if (DocmaConstants.DEBUG) {
            System.out.println("Refreshing out of sync tree item for node " + model.getId());
        }
        boolean isopen = tree_item.isOpen();
        tree_item.unload();
        docmaTree.renderItem(tree_item, model);
        tree_item.setOpen(isopen);
        // fireEvent(TreeDataEvent.STRUCTURE_CHANGED, GUIUtil.getDocmaTreePath(model), -1, -1);
        // if (DocmaConstants.DEBUG) {
        //     System.out.println("Structure_change event fired for node " + model.getId());
        // }
    }

    private Treeitem getTreeitemByDocmaNode(DocmaWebTree docmaTree, DocmaNode node)
    {
        Component comp = docmaTree.getChildByNode(node);  // docmaTree.getTreeitemByDocmaNode(node);
        if (comp instanceof Treeitem) {
            return (Treeitem) comp;
        } else {
            if (comp instanceof DocmaWebTree) {
                return null;  // node is the root, i.e. there is no visible Treeitem for this node.
            } else
            if (comp == null) {
                Log.warning("Treeitem for DocmaNode not found: " + node.getId());
            } else {
                Log.warning("Unknown component type in tree: " + comp.getClass().getName());
            }
            return null;
        }
    }

    private Treeitem getChildItem(Treeitem parentItem, String child_id)
    {
        Treechildren tchildren = parentItem.getTreechildren();
        if (tchildren == null) {
            Log.info("getChildItem: Treechildren is null.");
            return null;
        }
        List treeItems = tchildren.getChildren();
        if (treeItems != null) {
            for (int i=0; i < treeItems.size(); i++) {
                Object comp = treeItems.get(i);
                if (comp instanceof Treeitem) {
                    Treeitem child_item = (Treeitem) comp;
                    Object val = child_item.getValue();
                    if (val instanceof DocmaNode) {
                        DocmaNode nd = (DocmaNode) val;
                        if (child_id.equals(nd.getId())) return child_item;
                    }
                }
            }
        }
        return null;
    }


    private String[] getChildIds(Component parentComp)
    {
        Treechildren tchildren = null;
        if (parentComp instanceof Treeitem) {
            tchildren = ((Treeitem) parentComp).getTreechildren();
        } else
        if (parentComp instanceof DocmaWebTree) {
            tchildren = ((DocmaWebTree) parentComp).getTreechildren();
        }
        if (tchildren == null) {
            if (DocmaConstants.DEBUG) Log.info("getChildIds: Treechildren is null.");
            return new String[0];
        }
        List treeItems = tchildren.getChildren();
        if (treeItems == null) {
            if (DocmaConstants.DEBUG) Log.info("getChildIds: Treechildren returned null.");
            return new String[0];
        }

        int item_cnt = treeItems.size();
        String[] child_ids = new String[item_cnt];
        for (int i=0; i < item_cnt; i++) {
            Object comp = treeItems.get(i);
            if (comp instanceof Treeitem) {
                Object val = ((Treeitem) comp).getValue();
                if (val instanceof DocmaNode) {
                    child_ids[i] = ((DocmaNode) val).getId();
                } else {
                    child_ids = null;
                    break;
                }
            } else {
                child_ids = null;
                break;
            }
        }
        if (child_ids == null) {
            Log.error("getChildIds: invalid tree nodes!");
        }
        return child_ids;
    }


    private static class IndexRange
    {
        int indexFrom;
        int indexTo;

        IndexRange(int idx_from, int idx_to)
        {
            this.indexFrom = idx_from;
            this.indexTo = idx_to;
        }
    }
}
