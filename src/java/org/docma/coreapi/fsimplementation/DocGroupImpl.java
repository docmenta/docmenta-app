/*
 * DocGroupImpl.java
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
 * 
 * Created on 13. Oktober 2007, 23:55
 */

package org.docma.coreapi.fsimplementation;

import org.docma.coreapi.*;
import org.docma.util.*;

import java.util.*;
import org.w3c.dom.*;


/**
 *
 * @author MP
 */
public class DocGroupImpl extends DocNodeImpl implements DocGroup {

    private ArrayList nodeList = null;  // helper list -> see getChildNodes

    
    /** Creates a new instance of DocGroupImpl */
    public DocGroupImpl(DocStoreSessionImpl docSess, int node_id) {
        super(docSess);
        setDOMElement(createGroupDOM(getIndexDocument(), node_id));
    }

    /** Creates a new instance of DocGroupImpl from an existing DOM */
    public DocGroupImpl(DocStoreSessionImpl docSess, Element existingDOM) {
        super(docSess);
        // getIdRegistry().registerId(existingDOM.getAttribute(XMLConstants.ATTR_ID));
        setDOMElement(existingDOM);
    }

    
    static Element createGroupDOM(Document doc, int group_id) {
        String dom_id = IdRegistry.idToDOMString(group_id);
        Element elem = doc.createElement(XMLConstants.TAG_GROUP);
        elem.setAttribute(XMLConstants.ATTR_ID, dom_id);
        elem.setIdAttribute(XMLConstants.ATTR_ID, true);
        return elem; 
    }
    
    private synchronized void refreshChildNodes() {
        nodeList = null;
    }

    /* --------------  Methods used in sub-classes  ---------------- */

    void fireChildAddedEvent(DocNode newChild) {
        getDocSession().nodeAddedEvent(this, newChild);
    }

    void fireChildRemovedEvent(DocNode child) {
        getDocSession().nodeRemovedEvent(this, child);
    }

    void fireChildMovedEvent(DocNode child) {
        getDocSession().nodeRemovedEvent(this, child);
        getDocSession().nodeAddedEvent(this, child);
    }

    /* --------------  Interface DocGroup ---------------------- */

    public synchronized void refresh()
    {
        super.refresh();
        nodeList = null;   // refreshChildNodes();
    }

    public synchronized DocNode[] getChildNodes()
    {
        if (nodeList == null) {  // else nodeList.clear();
            nodeList = new ArrayList();
            synchronized (docStore) {
                NodeList children = getDOMElement().getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node child = children.item(i);
                    if (child instanceof Element) {
                        Element childElem = (Element) child;
                        DocNodeImpl childNode = (DocNodeImpl) getDocSession().createDocNodeFromDOMElement(childElem);
                        if (childNode != null) {
                            childNode.setParentGroup(this);
                            nodeList.add(childNode);
                        } else {
                            // alias or attribute element -> skip
                        }
                    } else {
                        Log.warning("Unexpected Node: " + child.getNodeName());
                    }
                }
            }
        }
        DocNode[] nodeArr = new DocNode[nodeList.size()];
        return (DocNode[]) nodeList.toArray(nodeArr);
    }

    public synchronized int getChildPos(DocNode childNode) {
        if (nodeList == null) getChildNodes();
        return nodeList.indexOf(childNode);
    }
    
    public DocNode appendChild(DocNode newChild) {
        return insertBefore(newChild, null);
    }

    public synchronized DocNode insertBefore(DocNode newChild, DocNode refChild) {
        DocNodeImpl newChildImpl = (DocNodeImpl) newChild;
        DocGroupImpl oldParent = (DocGroupImpl) newChildImpl.getParentGroup();
        // int oldPos = -1;
        // if (oldParent != null) {
        //     oldPos = oldParent.getChildPos(newChild);
        // }
        synchronized (docStore) {
            Element newChildDOM = newChildImpl.getDOMElement();
            if (refChild == null) {
                getDOMElement().appendChild(newChildDOM);
            } else {
                Element refChildDOM = ((DocNodeImpl) refChild).getDOMElement();
                getDOMElement().insertBefore(newChildDOM, refChildDOM);
            }
            newChildImpl.setParentGroup(this);
            getDocStore().saveOnCommit();
        }
        refreshChildNodes();
        if (oldParent == this) {  // child node changed position
            // int newPos = getChildPos(newChild);
            fireChildMovedEvent(newChild);
        } else {
            if (oldParent != null) {
                oldParent.refreshChildNodes();
                oldParent.fireChildRemovedEvent(newChild);
            }
            fireChildAddedEvent(newChild);
        }
        return newChild;
    }

    public synchronized DocNode removeChild(DocNode child) {
        synchronized (docStore) {
            // int delpos = getChildPos(child);
            DocNodeImpl childImpl = (DocNodeImpl) child;
            getDOMElement().removeChild(childImpl.getDOMElement());
            childImpl.setParentGroup(null);
            getDocStore().saveOnCommit();
        }
        refreshChildNodes();
        fireChildRemovedEvent(child);
        return child;
    }

    
}
