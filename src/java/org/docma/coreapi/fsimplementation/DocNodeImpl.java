/*
 * DocNodeImpl.java
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
 * Created on 14. Oktober 2007, 00:07
 */

package org.docma.coreapi.fsimplementation;

import java.util.*;
import org.docma.coreapi.*;
import org.docma.coreapi.implementation.*;
import org.docma.util.*;
import org.w3c.dom.*;

/**
 *
 * @author MP
 */
public class DocNodeImpl implements DocNode 
{
    protected final DocStoreImpl docStore;
    
    private DocStoreSessionImpl docSession;
    // private Document indexDoc;
    private long lastOpenedTime;
    private DocGroupImpl parentGroup = null;
    private Element domElement = null;  // the DOM element that represents this DocNode
    private String nodeId = null;
    
    
    /** Creates a new instance of DocNodeImpl */
    public DocNodeImpl(DocStoreSessionImpl docSess) {
        docSession = docSess;
        docStore = docSession.getDocStoreFs();
        // indexDoc = docStore.getIndexDocument();
        lastOpenedTime = docStore.getLastOpenedTime();
    }

    /* --------------  Private methods ---------------------- */

    private void checkDOMRefresh() {
        if (lastOpenedTime != getDocStore().getLastOpenedTime()) {
            if (nodeId != null) {
                String dom_id; 
                try {
                    dom_id = IdRegistry.idStringToDOMString(nodeId);
                } catch (Exception ex) {
                    Log.error("Invalid node id in checkDOMRefresh():" + nodeId);
                    return;
                }
                synchronized (docStore) {
                    Element domNew = getIndexDocument().getElementById(dom_id);
                    if (domNew != null) domElement = domNew;
                }
            }
        }
    }

    private Element getAttributeElement(String attname, String lang_mode, boolean exact_lang_match) {
        Element elem = getDOMElement();
        // String lang_mode = docSession.getTranslationMode();
        Element origElem = null;
        NodeList children = elem.getChildNodes();
        for (int i=0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                Element childElem = (Element) child;
                if (XMLConstants.TAG_ATT.equals(childElem.getTagName()) &&
                    attname.equals(childElem.getAttribute(XMLConstants.ATTR_ATTNAME))) {

                    String lang_value = childElem.getAttribute(XMLConstants.ATTR_LANGUAGE);
                    if ((lang_value == null) || (lang_value.length() == 0)) {
                        // childElem is the original attribute (non-translated)
                        if (lang_mode == null) return childElem;
                        else origElem = childElem;
                    } else {
                        // childElem is a translated attribute
                        if ((lang_mode != null) && lang_mode.equals(lang_value)) {
                            return childElem;
                        }
                    }
                }
            }
        }
        if (exact_lang_match) return null;
        else return origElem;
    }

    private void removeAttributeElements(String attname, String lang_mode) {
        Element node_elem = getDOMElement();
        if (lang_mode == null) {
            // Original mode: Remove all translations before removing the original attribute
            String[] lang_ids = getTranslations();
            for (int i=0; i < lang_ids.length; i++) {
                Element elem = getAttributeElement(attname, lang_ids[i], true);
                if (elem != null) node_elem.removeChild(elem);
            }
        }
        // In original mode: remove original attribute
        // In translation mode: remove translated attribute
        Element elem = getAttributeElement(attname, lang_mode, true);
        if (elem != null) node_elem.removeChild(elem);
    }

    private Element createAttributeElement(String attname, String lang_mode) {
        Element elem = getDOMElement();
        // String lang_mode = docSession.getTranslationMode();
        Element insElem = getIndexDocument().createElement(XMLConstants.TAG_ATT);
        insElem.setAttribute(XMLConstants.ATTR_ATTNAME, attname);
        if (lang_mode != null) {
            insElem.setAttribute(XMLConstants.ATTR_LANGUAGE, lang_mode);
            addTranslation(elem, lang_mode);
        }
        NodeList children = elem.getChildNodes();

        // search insert position (after alias elements, but before other elements)
        for (int i=0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                Element childElem = (Element) child;
                if (! XMLConstants.TAG_ALIAS.equals(childElem.getTagName())) {
                    // first Element which is not an alias element -> insert attribute here
                    elem.insertBefore(insElem, childElem);
                    return insElem;
                }
            }
        }

        // only alias children or non-element children exist or no children -> append attribute
        elem.appendChild(insElem);
        return insElem;
    }


    /* --------------  Methods used in sub-classes  ---------------- */

    void setDOMElement(Element elem) {
        domElement = elem;
        int id_number;
        synchronized (docStore) {
            id_number = IdRegistry.DOMStringToId(domElement.getAttribute(XMLConstants.ATTR_ID));
        }
        nodeId = String.valueOf(id_number);
    }

    Element getDOMElement() {
        checkDOMRefresh();
        return domElement;
    }

    DocStoreSessionImpl getDocSession() {
        return docSession;
    }
    
    DocStoreImpl getDocStore() {
        return docStore;
    }
    
    Document getIndexDocument() {
        return getDocStore().getIndexDocument();
    }
    
    IdRegistry getIdRegistry() {
        return getDocStore().getIdRegistry();
    }

    void fireChangedEvent() {
        fireChangedEvent(docSession.getTranslationMode());
    }

    void fireChangedEvent(String lang) {
        DocGroup par = getParentGroup();
        if (par != null) {
            docSession.nodeChangedEvent(par, this, lang);
        }
    }

    void addTranslation(Element elem, String lang_code) {
        String langs = elem.getAttribute(XMLConstants.ATTR_TRANSLATIONS);
        if ((langs == null) || (langs.length() == 0)) {
            langs = lang_code;
        } else {
            String[] lang_arr = langs.split(",");
            if (! Arrays.asList(lang_arr).contains(lang_code)) {
                langs += "," + lang_code;
            }
        }
        elem.setAttribute(XMLConstants.ATTR_TRANSLATIONS, langs);
    }

    /* --------------  Package local ---------------------- */

    void setParentGroup(DocGroupImpl parentGroup) {
        this.parentGroup = parentGroup;
    }

    // void setId(String idvalue) {
    //     domElement.setAttribute(XMLConstants.ATTR_ID, IdRegistry.idStringToDOMString(idvalue));
    //     nodeId = idvalue;
    // }

    /* --------------  Public  ---------------------- */
    
    public boolean equals(Object obj) {
        if ((obj != null) && (obj instanceof DocNodeImpl)) {
            DocNodeImpl other = (DocNodeImpl) obj;
            return (getDocStore() == other.getDocStore()) && getId().equals(other.getId());
        } else {
            return false;
        }
    }

    /* --------------  Interface DocNode ---------------------- */

    public synchronized void refresh() {
        // no values are cached, therefore nothing has to be done here
    }

    public String getId() {
        return nodeId; // getDOMElement().getAttribute(XMLConstants.ATTR_ID);
    }

    public String getTitle() {
        return getAttribute(DocAttributes.TITLE);
    }

    public String getTitle(String lang_id) {
        return getAttribute(DocAttributes.TITLE, lang_id);
    }

    public void setTitle(String title) {
        setAttribute(DocAttributes.TITLE, title);
    }

    public void setTitle(String title, String lang_id) {
        setAttribute(DocAttributes.TITLE, title, lang_id);
    }

    public String getAlias() {
        synchronized (docStore) {
            Element aliaselem = XMLUtil.getChildByTagName(getDOMElement(), XMLConstants.TAG_ALIAS);
            if (aliaselem != null) { 
                String s = aliaselem.getAttribute(XMLConstants.ATTR_ID);
                return ((s == null) || (s.length() == 0)) ? null : s;
            }
            else return null;
        }
    }

    public String[] getAliases() {
        synchronized (docStore) {
            List aliaslist = XMLUtil.getChildrenByTagName(getDOMElement(), XMLConstants.TAG_ALIAS);
            String[] arr = new String[aliaslist.size()]; 
            int cnt = 0;
            for (int i=0; i < aliaslist.size(); i++) {
                Element elem = (Element) aliaslist.get(i);
                String s = elem.getAttribute(XMLConstants.ATTR_ID);
                if ((s != null) && (s.length() > 0)) {
                    arr[cnt++] = s;
                }
            }
            if (cnt < arr.length) {
                arr = Arrays.copyOf(arr, cnt);
            }
            return arr;
        }
    }

    /**
     * Insert as first alias to the list.
     * @param alias
     */
    public void addAlias(String alias) {
        if (alias.length() == 0) {
            throw new DocRuntimeException("Invalid alias: empty string.");
        }
        boolean added = false;
        synchronized (docStore) {
            if (! hasAlias(alias)) {
                Element aliasElem = getIndexDocument().createElement(XMLConstants.TAG_ALIAS);
                aliasElem.setAttribute(XMLConstants.ATTR_ID, alias);
                aliasElem.setIdAttribute(XMLConstants.ATTR_ID, true);
                Element elem = getDOMElement();
                elem.insertBefore(aliasElem, elem.getFirstChild());  // insert as first child
                getDocStore().saveOnCommit();
                added = true;
            }
        }
        if (added) {
            fireChangedEvent();
            docSession.refreshAliasList();
        }
    }

    public void setAliases(String[] aliases) {
        synchronized (docStore) {
            deleteAliases();
            Element elem = getDOMElement();
            for (int i = aliases.length - 1; i >= 0; i--) {
                Element aliasElem = getIndexDocument().createElement(XMLConstants.TAG_ALIAS);
                String a = aliases[i];
                if (a.length() == 0) {
                    throw new DocRuntimeException("Invalid alias: empty string.");
                }
                aliasElem.setAttribute(XMLConstants.ATTR_ID, a);
                aliasElem.setIdAttribute(XMLConstants.ATTR_ID, true);
                elem.insertBefore(aliasElem, elem.getFirstChild());  // insert as first child
            }
            getDocStore().saveOnCommit();
        }
        fireChangedEvent();
        docSession.refreshAliasList();
    }

    public boolean deleteAlias(String alias) {
        boolean deleted = false;
        synchronized (docStore) {
            Element domSelf = getDOMElement();
            List aliaslist = XMLUtil.getChildrenByTagName(domSelf, XMLConstants.TAG_ALIAS);
            for (int i=0; i < aliaslist.size(); i++) {
                Element elem = (Element) aliaslist.get(i);
                if (elem.getAttribute(XMLConstants.ATTR_ID).equals(alias)) {
                    domSelf.removeChild(elem);
                    getDocStore().saveOnCommit();
                    deleted = true;
                    break;
                }
            }
        }
        if (deleted) {
            fireChangedEvent();
            docSession.refreshAliasList();
        }
        return deleted;
    }

    private boolean deleteAliases() {
        Element domSelf = getDOMElement();
        List aliaslist = XMLUtil.getChildrenByTagName(domSelf, XMLConstants.TAG_ALIAS);
        if (aliaslist.size() > 0) {
            for (int i=0; i < aliaslist.size(); i++) {
                Element elem = (Element) aliaslist.get(i);
                domSelf.removeChild(elem);
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean hasAlias(String alias) {
        synchronized (docStore) {
            List aliaslist = XMLUtil.getChildrenByTagName(getDOMElement(), XMLConstants.TAG_ALIAS);
            for (int i=0; i < aliaslist.size(); i++) {
                Element elem = (Element) aliaslist.get(i);
                if (elem.getAttribute(XMLConstants.ATTR_ID).equals(alias)) return true;
            }
            return false;
        }
    }

    /**
     * This is an utility method which is a combination of getAttributeNames() 
     * and getAttribute(name) to retrieve all attributes in one call. 
     * 
     * Note that the attribute values are as returned by method getAttribute(),
     * i.e. the value depends on the current translation mode.
     * 
     * @return Map of attribute name and value pairs.
     */
    public Map<String, String> getAttributes()
    {
        Map<String, String> result_map = new HashMap<String, String>();
        synchronized (docStore) {
            String[] names = getAttributeNames();
            for (String nm : names) {
                result_map.put(nm, getAttribute(nm));
            }
        }
        return result_map;
    }

    /**
     * This is an utility method which is a combination of getAttributeNames() 
     * and getAttribute(name, lang_id) to retrieve all attributes in one call. 
     * 
     * @param lang_id The language id or null for the original language.
     * @return Map of attribute name and value pairs for the given language.
     */
    public Map<String, String> getAttributes(String lang_id)
    {
        Map<String, String> result_map = new HashMap<String, String>();
        synchronized (docStore) {
            NodeList children = getDOMElement().getChildNodes();
            for (int i=0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child instanceof Element) {
                    Element childElem = (Element) child;
                    if (XMLConstants.TAG_ATT.equals(childElem.getTagName())) {
                        String lang_value = childElem.getAttribute(XMLConstants.ATTR_LANGUAGE);
                        if ((lang_value != null) && (lang_value.length() == 0)) {
                            lang_value = null;
                        }
                        if ((lang_id == null) ? (lang_value == null) : lang_id.equals(lang_value)) {
                            String att_name = childElem.getAttribute(XMLConstants.ATTR_ATTNAME);
                            if (! DocAttributes.isInternalAttributeName(att_name)) {
                                String val = XMLUtil.readTextChild(childElem);
                                if (val != null) {
                                    result_map.put(att_name, val);
                                }
                            }
                        }
                    }
                }
            }
        }
        return result_map;
    }

    public String[] getAttributeNames() 
    {
        // String lang_mode = docSession.getTranslationMode();
        SortedSet result_set = new TreeSet();
        synchronized (docStore) {
            NodeList children = getDOMElement().getChildNodes();
            for (int i=0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child instanceof Element) {
                    Element childElem = (Element) child;
                    if (XMLConstants.TAG_ATT.equals(childElem.getTagName())) {
                        String lang_value = childElem.getAttribute(XMLConstants.ATTR_LANGUAGE);
                        if ((lang_value == null) || (lang_value.length() == 0)) {
                            String att_name = childElem.getAttribute(XMLConstants.ATTR_ATTNAME);
                            if (! DocAttributes.isInternalAttributeName(att_name)) {
                                result_set.add(att_name);
                            }
                        }
                    }
                }
            }
        }
        return (String[]) result_set.toArray(new String[result_set.size()]);
    }

    public String getAttribute(String name) 
    {
        synchronized (docStore) {
            Element elem = getAttributeElement(name, docSession.getTranslationMode(), false);
            if (elem != null) return XMLUtil.readTextChild(elem);
            else return "";  // return empty string as default value
        }
    }

    public String getAttribute(String name, String lang_id) 
    {
        synchronized (docStore) {
            Element elem = getAttributeElement(name, lang_id, true);
            if (elem != null) return XMLUtil.readTextChild(elem);
            else return (lang_id == null) ? "" : null;  // return null to indicate that no translation exists
        }
    }

    public void setAttribute(String name, String value) {
        String lang_mode = docSession.getTranslationMode();
        setAttribute(name, value, lang_mode);
    }

    public void setAttribute(String name, String value, String lang_id) {
        synchronized (docStore) {
            if (value == null) {
                removeAttributeElements(name, lang_id);
            } else {
                Element elem = getAttributeElement(name, lang_id, true);
                if (elem == null) elem = createAttributeElement(name, lang_id);
                XMLUtil.writeTextChild(getIndexDocument(), elem, value);
            }
            if (isPersistent()) { 
                getDocStore().saveOnCommit();
            }
        }
        fireChangedEvent(lang_id);
    }

    public DocGroup getParentGroup() {
        if (parentGroup == null) {
            synchronized (docStore) {
                Node pnode = getDOMElement().getParentNode();
                if ((pnode != null) && pnode.getNodeName().equals(XMLConstants.TAG_GROUP)) {
                    parentGroup = new DocGroupImpl(getDocSession(), (Element) pnode);
                }
            }
        }
        return parentGroup;
    }
    
    private boolean isPersistent()
    {
        synchronized (docStore) {
            Node nd = getDOMElement();
            // Get root node
            while (nd != null) {
                Node pnode = nd.getParentNode();
                if (pnode == null) {
                    break;
                }
                nd = pnode;
            }
            // If the root node is an instance of Document, then this node is 
            // connected to the document tree. Otherwise this node is transient.
            return (nd instanceof org.w3c.dom.Document);
        }
    }

    public String[] getTranslations() {
        String langs;
        synchronized (docStore) {
            langs = getDOMElement().getAttribute(XMLConstants.ATTR_TRANSLATIONS);
        }
        if ((langs == null) || (langs.length() == 0)) {
            return new String[0];
        } else {
            return langs.split(",");
        }
    }

    public boolean hasTranslation(String lang_code) {
        return Arrays.asList(getTranslations()).contains(lang_code);
    }

    public void deleteTranslation(String lang_code) {
        synchronized (docStore) {
            Element elem = getDOMElement();

            // delete language code from translations attribute
            String langs = elem.getAttribute(XMLConstants.ATTR_TRANSLATIONS);
            if ((langs == null) || (langs.length() == 0)) return;
            List langlist = new ArrayList(Arrays.asList(langs.split(",")));
            langlist.remove(lang_code);
            langs = DocmaUtil.concatStrings(langlist, ",");
            elem.setAttribute(XMLConstants.ATTR_TRANSLATIONS, langs);

            // delete all child elements for this language
            NodeList children = elem.getChildNodes();
            int lastidx = children.getLength() - 1;
            for (int i=lastidx; i >= 0; i--) {
                Node child = children.item(i);
                if (child instanceof Element) {
                    Element childElem = (Element) child;
                    if (XMLConstants.TAG_ATT.equals(childElem.getTagName())) {
                        String lang_value = childElem.getAttribute(XMLConstants.ATTR_LANGUAGE);
                        if ((lang_value != null) && (lang_value.length() > 0) && lang_value.equals(lang_code)) {
                            elem.removeChild(child);
                        }
                    }
                }
            }
        }
        fireChangedEvent(lang_code);
    }

}
