/*
 * GroupImpl.java
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

import org.docma.app.DocmaNode;
import org.docma.plugin.DocmaException;
import org.docma.plugin.Group;
import org.docma.plugin.Node;
import org.docma.plugin.OutOfRangeException;

/**
 *
 * @author MP
 */
public abstract class GroupImpl extends NodeImpl implements Group
{

    public GroupImpl(StoreConnectionImpl store, DocmaNode docNode) 
    {
        super(store, docNode);
    }
    
    //************************************************************
    //**************    Interface methods       ******************  
    //************************************************************

    public boolean isAncestorOf(String node_id) throws DocmaException 
    {
        try {
            return docNode.isAncestor(node_id);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public Node getChild(int index) throws DocmaException 
    {
        DocmaNode child;
        try {
            child = docNode.getChild(index);
        } catch (IndexOutOfBoundsException ioob) {
            throw new OutOfRangeException(ioob);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
        if (child == null) {
            throw new DocmaException("Internal error: child is null.");
        }
        return NodeImpl.createNodeInstance(store, child);
    }

    public Node[] getChildren() throws DocmaException 
    {
        try {
            DocmaNode[] arr = docNode.getChildren();
            Node[] res = new Node[arr.length];
            for (int i = 0; i < res.length; i++) {
                res[i] = NodeImpl.createNodeInstance(store, arr[i]);
            }
            return res;
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public Node getChildByAlias(String alias) throws DocmaException 
    {
        try {
            DocmaNode nd = docNode.getChildByAlias(alias);
            return (nd == null) ? null 
                                : NodeImpl.createNodeInstance(store, nd);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public Node getChildById(String node_id) throws DocmaException 
    {
        try {
            DocmaNode nd = docNode.getChildById(node_id);
            return (nd == null) ? null 
                                : NodeImpl.createNodeInstance(store, nd);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public int getChildCount() throws DocmaException 
    {
        try {
            return docNode.getChildCount();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public int getChildPos(Node child) throws DocmaException 
    {
        try {
            return docNode.getChildPos(((NodeImpl) child).docNode);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void removeChildren(Node... nds) throws DocmaException 
    {
        try {
            DocmaNode[] arr = new DocmaNode[nds.length];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = ((NodeImpl) nds[i]).docNode;
            }
            docNode.removeChildren(arr);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void addChildren(Node... nds) throws DocmaException 
    {
        try {
            DocmaNode[] arr = new DocmaNode[nds.length];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = ((NodeImpl) nds[i]).docNode;
            }
            docNode.addChildren(arr);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void insertChildren(int index, Node... nds) throws DocmaException 
    {
        try {
            DocmaNode[] arr = new DocmaNode[nds.length];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = ((NodeImpl) nds[i]).docNode;
            }
            docNode.insertChildren(index, arr);
        } catch (IndexOutOfBoundsException ioob) {
            throw new OutOfRangeException(ioob);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void insertChildrenBefore(Node refNode, Node... nds) throws DocmaException 
    {
        try {
            DocmaNode[] arr = new DocmaNode[nds.length];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = ((NodeImpl) nds[i]).docNode;
            }
            docNode.insertChildrenBefore(((NodeImpl) refNode).docNode, arr);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void deleteRecursive() throws DocmaException 
    {
        try {
            docNode.deleteRecursive();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    //************************************************************
    //**************    Other methods       **********************  
    //************************************************************

    public void removeChild(int index) throws DocmaException 
    {
        try {
            docNode.removeChild(index);
        } catch (IndexOutOfBoundsException ioob) {
            throw new OutOfRangeException(ioob);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void removeChildren(int firstIndex, int lastIndex) throws DocmaException 
    {
        try {
            docNode.removeChildren(firstIndex, lastIndex);
        } catch (IndexOutOfBoundsException ioob) {
            throw new OutOfRangeException(ioob);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }
    
}
