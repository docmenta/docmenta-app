/*
 * GUIUtil.java
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

import java.util.*;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.docma.app.*;
import org.docma.app.ui.UserModel;
import org.docma.coreapi.*;
import org.docma.lockapi.Lock;
import org.docma.util.Log;

import org.zkoss.util.Locales;
import org.zkoss.web.Attributes;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Messagebox;


/**
 *
 * @author MP
 */
public class GUIUtil
{
    /* -----  Get/set DocmaWebApplication from/in application context  ------ */

    public static DocmaWebApplication getDocmaWebApplication(Component comp)
    {
        return (DocmaWebApplication) 
                comp.getDesktop().getWebApp().getAttribute(
                DocmaWebApplication.class.getName());
    }

    public static DocmaWebApplication getDocmaWebApplication(ServletContext ctx)
    {
        return (DocmaWebApplication) ctx.getAttribute(DocmaWebApplication.class.getName());
    }

    public static void setDocmaWebApplication(Component comp, DocmaWebApplication app) {
        comp.getDesktop().getWebApp().setAttribute(DocmaWebApplication.class.getName(), app);
    }

    public static void setDocmaWebApplication(ServletContext ctx, DocmaWebApplication app) {
        ctx.setAttribute(DocmaWebApplication.class.getName(), app);
    }

    /* -----  Get/set DocmaWebSession from/in desktop context  ------ */

    public static DocmaWebSession getDocmaWebSession(Component comp)
    {
        Desktop desk = comp.getDesktop();
        return (DocmaWebSession)
                desk.getSession().getAttribute(
                DocmaWebSession.class.getName() + "#" + desk.getId());
    }

    public static DocmaWebSession getDocmaWebSession(Session sess, String desktopId)
    {
        return (DocmaWebSession) sess.getAttribute(DocmaWebSession.class.getName() + "#" + desktopId);
    }

    public static DocmaWebSession getDocmaWebSession(HttpSession sess, String desktopId)
    {
        return (DocmaWebSession) sess.getAttribute(DocmaWebSession.class.getName() + "#" + desktopId);
    }

    public static void setDocmaWebSession(Component comp, DocmaWebSession sess)
    {
        Desktop desk = comp.getDesktop();
        desk.getSession().setAttribute(DocmaWebSession.class.getName() + "#" + desk.getId(), sess);
    }

    public static DocmaWebSession removeDocmaWebSession(Component comp)
    {
        Desktop desk = comp.getDesktop();
        return (DocmaWebSession)
                desk.getSession().removeAttribute(
                DocmaWebSession.class.getName() + "#" + desk.getId());
    }

    /* -----  Get/set DocmaWebSession from/in session context  ------ */

    public static void setDocmaWebSession_SessionContext(Session uiSess, DocmaWebSession sess)
    {
        uiSess.setAttribute(DocmaWebSession.class.getName(), sess);
    }

    public static DocmaWebSession getDocmaWebSession_SessionContext(Session uiSess)
    {
        return (DocmaWebSession) uiSess.getAttribute(DocmaWebSession.class.getName());
    }

    /* ------  Other utility methods  ------ */

    public static void closeAllDocmaSessions(Session uiSess)
    {
        // Close Docma session attached to session context:
        DocmaWebSession webSess = getDocmaWebSession_SessionContext(uiSess);
        if (webSess != null) webSess.closeSession();
        setDocmaWebSession_SessionContext(uiSess, null);

        // close all Docma sessions attached to desktops
        Iterator sess_it = uiSess.getAttributes().keySet().iterator();
        while (sess_it.hasNext()) {
            try {
                String attname = (String) sess_it.next();
                if (attname.startsWith(DocmaWebSession.class.getName() + "#")) {
                    webSess = (DocmaWebSession) uiSess.getAttribute(attname);
                    if (webSess != null) webSess.closeSession();
                    uiSess.setAttribute(attname, null);
                }
            } catch (Exception ex) {
                org.docma.util.Log.warning("Exception in GUIUtil.closeAllDocmaSessions: " + ex.getMessage());
            }
        }
    }
    
    public static DocI18n getI18n(Component comp)
    {
        return getDocmaWebApplication(comp).getI18n();
    }

    /**
     * Deprecated. Should be replaced by method getI18n().
     * @param comp
     * @return 
     */
    public static DocmaI18 i18(Component comp)
    {
        return getDocmaWebApplication(comp).i18();
    }

    public static Locale getCurrentLocale()
    {
        return Locales.getCurrent();
    }

    public static String getCurrentUILanguage(Component comp)
    {
        Locale loc = getCurrentLocale();
        String lang = null;
        if (loc != null) {
            lang = loc.getLanguage();
        }
        return ((lang == null) || lang.equals("")) ? "en" : lang;
    }

    public static void setCurrentUILanguage(Component comp, String lang_code, boolean switchNow)
    {
        if ((lang_code != null) && !lang_code.equals("")) {
            Locale current_loc = GUIUtil.getCurrentLocale();
            if ((current_loc == null) || 
                !lang_code.equalsIgnoreCase(current_loc.getLanguage())) {

                try {
                    if (DocmaConstants.DEBUG) {
                        Log.info("Switching UI language from " + current_loc + 
                                 " to " + lang_code + ".");
                    }
                    Locale loc = Locales.getLocale(lang_code);
                    if (loc == null) {
                        if (DocmaConstants.DEBUG) {
                            Log.warning("Could not find locale for " + lang_code + 
                                        ". Creating new instance.");
                        }
                        loc = new Locale(lang_code);
                    }
                    comp.getDesktop().getSession().setAttribute(Attributes.PREFERRED_LOCALE, loc);

                    // Switch language in current page
                    if (switchNow) {
                        Clients.reloadMessages(loc);
                        Locales.setThreadLocal(loc);
                    }
                } catch (Exception ex) {
                    Log.error("Failed to switch UI language: " + ex.getMessage());
                    if (DocmaConstants.DEBUG) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    public static String getUserNameFromCookie(Component comp)
    {
        Object obj = comp.getDesktop().getExecution().getNativeRequest();
        if (obj instanceof HttpServletRequest) {
            HttpServletRequest serv_req = (HttpServletRequest) obj;
            Cookie[] cookies = serv_req.getCookies();
            if (cookies != null) {
                for (int j = 0; j < cookies.length; j++) {
                    if (cookies[j].getName().equals("org.docma.username")) {
                        return cookies[j].getValue();
                    }
                }
            }
        }
        return null;
    }


    public static String getNodeIconPath(DocmaNode node)
    {
        String imgsrc = null;
        if (node.isImageContent()) {
            imgsrc = "img/image_icon.gif";
        } else
        if (node.isHTMLContent()) {
            imgsrc = "img/doc_icon.gif";
        } else
        if (node.isContent()) {
            imgsrc = "img/file_icon.gif";
        } else
        if (node.isSection()) {
                imgsrc = "img/section_icon.gif";
        } else
        if (node.isImageFolder()) {
            imgsrc = "img/img_folder_icon.gif";
        } else
        if (node.isSystemFolder()) {
            if (node.isSystemRoot()) {
                imgsrc = "img/sys_folder_icon.gif";
            } else {
                imgsrc = "img/folderclosed.gif";
            }
        } else
        if (node.isIncludeReference()) {
            DocmaNode refnode = node.getReferencedNode();
            if (refnode == null) {
                imgsrc = "img/ref_invalid.gif";
            } else {
                if (node.isImageIncludeReference() && refnode.isImageContent()) {
                    imgsrc = "img/ref_img.gif";
                } else
                if (node.isContentIncludeReference() && refnode.isContent()) {
                    imgsrc = "img/ref_doc.gif";
                } else
                if (node.isSectionIncludeReference() && refnode.isSection()) {
                    imgsrc = "img/ref_section.gif";
                } else {
                    imgsrc = "img/ref_invalid.gif";
                }
            }
        }
        return imgsrc;
    }

    public static boolean selectRadio(Radiogroup radiogroup, String radiovalue)
    {
        for (int i=0; i < radiogroup.getItemCount(); i++) {
            Radio rad = radiogroup.getItemAtIndex(i);
            Object obj = rad.getValue();
            String v = (obj == null) ? "" : obj.toString();
            if (radiovalue.equalsIgnoreCase(v)) {
                rad.setSelected(true);
                return true;
            }
        }
        return false;
    }

    public static boolean selectListItem(Listbox listbox, String itemvalue)
    {
        for (int i=0; i < listbox.getItemCount(); i++) {
            Listitem item = listbox.getItemAtIndex(i);
            Object val = item.getValue();
            if ((val != null) && val.toString().equalsIgnoreCase(itemvalue)) {
                item.setSelected(true);
                return true;
            }
        }
        return false;
    }

    public static void getTreeState(Treechildren tree_children, List treestate_list)
    {
        if (tree_children == null) return;

        List items = tree_children.getChildren();
        for (int i=0; i < items.size(); i++) {
            Object obj = items.get(i);
            if (! (obj instanceof Treeitem)) {
                continue;
            }
            Treeitem item = (Treeitem) obj;
            TreeState ts = new TreeState();
            ts.setIndex(i);
            ts.setSelected(item.isSelected());
            boolean is_open = item.isOpen();
            ts.setOpen(is_open);
            if (is_open) {
                Treechildren tch = item.getTreechildren();
                if (tch != null) {
                    getTreeState(tch, ts.children());
                }
            }
            treestate_list.add(ts);
        }
    }

    public static void applyTreeState(Tree tree, Treechildren tree_children, List treestate_list)
    {
        // Tree tree = tree_children.getTree();
        List items = tree_children.getChildren();
        for (int i=0; i < treestate_list.size(); i++) {
            TreeState ts = (TreeState) treestate_list.get(i);
            int item_idx = ts.getIndex();
            Object obj = items.get(item_idx);
            if (! (obj instanceof Treeitem)) continue;
            Treeitem item = (Treeitem) obj;
            // item.setSelected(ts.isSelected());
            if (ts.isSelected()) {
                if (tree.getSelectedCount() <= 0) tree.setSelectedItem(item);
                else tree.addItemToSelection(item);
            }
            boolean is_open = ts.isOpen();
            item.setOpen(is_open);
            if (is_open && ts.hasChildren()) {
                Treechildren tch = item.getTreechildren();
                if (tch != null) {
                    applyTreeState(tree, tch, ts.children());
                }
            }
        }
    }

    public static List<DocmaNode> getSelectedDocmaNodes(Tree docTree)
    {
        return getSelectedDocmaNodes(docTree, true, true);
    }

    public static List<DocmaNode> getSelectedDocmaNodes(Tree docTree, boolean siblingsOnly, boolean showSelectError)
    {
        int cnt = docTree.getSelectedCount();
        if (DocmaConstants.DEBUG) {
            System.out.println("Selected count: " + cnt);
        }
        if (cnt == 0) return null;

        List<DocmaNode> list = new ArrayList<DocmaNode>(cnt);
        Set id_list = new HashSet(2*cnt);
        for (Treeitem item : docTree.getSelectedItems()) {
            Object obj = item.getValue();
            if ((obj != null) && (obj instanceof DocmaNode)) {
                DocmaNode node = (DocmaNode) obj;
                id_list.add(node.getId());
                list.add(node);
            }
        }

        List<DocmaNode> resultlist = new ArrayList<DocmaNode>(cnt);
        String parentId = null;
        for (int i=0; i < list.size(); i++) {
            DocmaNode node = list.get(i);
            DocmaNode par = node.getParent();
            if ((par == null) || node.isRoot()) {
                if (showSelectError) {
                    Messagebox.show("Operation cannot be applied on root folder!");
                }
                return null;
            }
            if (id_list.contains(par.getId())) {
                // If parent is cut or copied, then child nodes will be cut or copied
                // automatically. Therefore If parent is in selection list, then
                // ignore any selected child nodes.
                // This check is needed, because when a range is selected, then
                // all child nodes of a selected node are also included in
                // the list of selected nodes.
                continue;  // exclude this node, because parent is in selection list
            }
            if (parentId == null) {
                parentId = par.getId();
            } else {
                if (siblingsOnly && !parentId.equals(par.getId())) {
                    if (showSelectError) {
                        Messagebox.show(getI18n(docTree).getLabel("text.content_tree.select_siblings"));
                    }
                    return null;
                }
            }
            // if (node.isContent() || node.isImageFolder() || node.isSystemFolder()) {
            resultlist.add(node);
            // }
        }
        if (resultlist.isEmpty()) return null;
        else return resultlist;
    }
    
    public static int[] getDocmaTreePath(DocmaNode node)
    {
        ArrayList<Integer> path_list = new ArrayList<Integer>();
        DocmaNode nd = node;
        DocmaNode p; 
        while ((p = nd.getParent()) != null) {
            path_list.add(0, p.getChildPos(nd));
            nd = p;
        }
        int[] path = new int[path_list.size()];
        for (int i=0; i < path.length; i++) {
            path[i] = path_list.get(i);
        }
        if (DocmaConstants.DEBUG) {
            StringBuilder sb = new StringBuilder("Path of node " + node.getId() + ":");
            for (int pos : path) sb.append(" " + pos);
            System.out.println(sb.toString());
        }
        return path;
    }

    public static String getLatestVerIdLabel(Component comp)
    {
        return i18(comp).getLabel("label.versionid.latest");
    }

    public static DocVersionId getNormalizedVerId(Component comp, String ver_str) throws Exception
    {
        DocmaSession docmaSess = GUIUtil.getDocmaWebSession(comp).getDocmaSession();
        ver_str = ver_str.trim();
        String latest_label = getLatestVerIdLabel(comp);

        if (ver_str.equalsIgnoreCase(latest_label)) {
            ver_str = DocmaConstants.DEFAULT_LATEST_VERSION_ID;
        } else
        if (ver_str.equalsIgnoreCase(DocmaConstants.DEFAULT_LATEST_VERSION_ID)) {
            ver_str = DocmaConstants.DEFAULT_LATEST_VERSION_ID;
        }
        return docmaSess.createVersionId(ver_str);
    }

    public static boolean isUpdateVersionAllowed(DocmaSession docmaSess, 
                                                 boolean showMessage) 
    {
        try {
            docmaSess.checkUpdateVersionAllowed();
            return true;
        } catch (Exception ex) {
            if (showMessage) {
                Messagebox.show(ex.getLocalizedMessage());
            }
            return false;
        }
    }

    public static boolean isEditContentAllowed(DocmaNode node,
                                               boolean showMessage)
    {
        try {
            node.checkEditContentAllowed();
            return true;
        } catch (Exception ex) {
            if (showMessage) {
                Messagebox.show(ex.getLocalizedMessage());
            }
            return false;
        }
    }

    public static boolean isContentNotLocked(MainWindow mainWin,
                                             DocmaSession docmaSess,
                                             DocmaNode node,
                                             boolean showMessage,
                                             boolean setNodeLock)
    {
        // Check if lock exists
        Lock lock = node.getLock();
        boolean do_lock = setNodeLock; // true;
        if (lock != null) {
            String usr_id = lock.getUser();
            if (docmaSess.getUserId().equals(usr_id)) {
                if (showMessage) {
                    Messagebox.show("Warning: This content is already locked by you!");
                }
                if (setNodeLock) node.refreshLock(GUIConstants.LOCK_TIMEOUT);
                do_lock = false;
            } else {
                if (showMessage) {
                    UserModel usr = mainWin.getUser(usr_id);
                    String usr_name = usr_id;
                    if (usr != null) {
                        usr_name = usr.getFirstName() + " " + usr.getLastName();
                    }
                    Messagebox.show("This content is locked by user '" + usr_name + "'.");
                }
                return false;
            }
        }

        // Set lock
        if (do_lock && !node.setLock(GUIConstants.LOCK_TIMEOUT)) {
            if (showMessage) {
                Messagebox.show("Could not set lock! Content may be locked by another user.");
            }
            return false;
        }

        return true;
    }

}
