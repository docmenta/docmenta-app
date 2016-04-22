/*
 * DocmaWebTree.java
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

import org.docma.app.*;
import org.docma.util.Log;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.*;
import java.util.*;

/**
 *
 * @author MP
 */
public class DocmaWebTree extends Tree
{

    protected Component getChildByNode(Object node)
    {
        if (DocmaConstants.DEBUG) {
            System.out.println("Called getChildByNode(" + node.getClass().getName() + ")");
        }
        Component comp = super.getChildByNode(node);
        if (node instanceof DocmaNode) {
            DocmaNode docnode = (DocmaNode) node;
            if (comp instanceof Treeitem) {
                Object item_val = ((Treeitem) comp).getValue();
                if (item_val != node) {
                    if (DocmaConstants.DEBUG) {
                        System.out.println("Tree path mismatch. Recalculating node path!");
                    }
                    comp = getTreeitemByDocmaNode(docnode);
                }
            } else
            if (comp == null) {
                if (DocmaConstants.DEBUG) {
                    System.out.println("Tree path does not exist. Recalculating node path!");
                }
                comp = getTreeitemByDocmaNode(docnode);
            }
            if (comp == null) {
                if (DocmaConstants.DEBUG) {
                    System.out.println("Tree node not found on expected path. Starting recursive search!");
                }
                comp = getTreeitemByRecursiveSearch(docnode, this.getTreechildren()); // search complete tree
            }
            if (comp == null) {
                Log.warning("Could not find tree node " + docnode.getId());
            }
        }
        return comp;
    }


    private Treeitem getTreeitemByRecursiveSearch(DocmaNode node, Treechildren tree_children)
    {
        if (tree_children == null) return null;

        List items = tree_children.getChildren();
        for (int i=0; i < items.size(); i++) {
            Object obj = items.get(i);
            if (! (obj instanceof Treeitem)) {
                continue;
            }
            Treeitem item = (Treeitem) obj;
            if (item.getValue() == node) {
                return item;
            } else {
                item = getTreeitemByRecursiveSearch(node, item.getTreechildren());
                if (item != null) return item;
            }
        }
        return null;
    }


    Treeitem getTreeitemByDocmaNode(DocmaNode node)
    {
        Treechildren tc = getTChildrenByDocmaNode(node.getParent());
        Treeitem ti = getTreeitemByDocmaNode(tc, node);
        return ti;
    }

    private Treechildren getTChildrenByDocmaNode(DocmaNode parentnode)
    {
        if (parentnode.isRoot()) {
            return this.getTreechildren();
        } else {
            Treechildren tc = getTChildrenByDocmaNode(parentnode.getParent());
            Treeitem ti = getTreeitemByDocmaNode(tc, parentnode);
            if (ti == null) return null;
            return ti.getTreechildren();
        }
    }

    private Treeitem getTreeitemByDocmaNode(Treechildren tree_children, DocmaNode node)
    {
        if (tree_children == null) return null;

        List items = tree_children.getChildren();
        for (int i=0; i < items.size(); i++) {
            Object obj = items.get(i);
            if (! (obj instanceof Treeitem)) {
                continue;
            }
            Treeitem item = (Treeitem) obj;
            if (item.getValue() == node) return item;
        }
        return null;
    }

}
