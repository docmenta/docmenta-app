/*
 * ConnectedUsersDialog.java
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
import org.docma.coreapi.*;
import org.docma.app.ui.ConnectedUserModel;
import org.docma.userapi.*;

import java.util.*;
import org.zkoss.zul.*;

/**
 *
 * @author MP
 */
public class ConnectedUsersDialog extends Window
{
    private MainWindow mainWin;
    private DocmaApplication docmaApp;
    private DocmaSession docmaSess;
    private String storeId;
    private DocVersionId verId;
    private Listbox users_listbox;
    private ListModelList users_listmodel = null;
    private DocmaListitemRenderer listitem_renderer = null;


    public void onCloseClick()
    {
        setVisible(false);
        docmaApp = null;
        docmaSess = null;
        storeId = null;
        verId = null;
    }

    public void onDisconnectClick() throws Exception
    {
        int sel_cnt = users_listbox.getSelectedCount();
        if (sel_cnt <= 0) {
            Messagebox.show("Please select a connection from the list!");
            return;
        }
        Iterator it = users_listbox.getSelectedItems().iterator();
        ConnectedUserModel[] sel_conns = new ConnectedUserModel[sel_cnt];
        for (int i=0; i < sel_cnt; i++) {
            Listitem item = (Listitem) it.next();
            sel_conns[i] = (ConnectedUserModel) users_listmodel.getElementAt(item.getIndex());
        }
        String msg;
        if (sel_cnt == 1) {
            msg = "Disconnect session '" + sel_conns[0].getSessionId() +
                    "' of user '" + sel_conns[0].getUserName() + "'?";
        } else {
            msg = "Disconnect selected connections?";
        }
        if (Messagebox.show(msg, "Delete?",
                Messagebox.YES | Messagebox.NO, Messagebox.QUESTION) == Messagebox.YES) {

            String login_name = mainWin.getUser(docmaSess.getUserId()).getLoginName();
            boolean is_sys_admin = DocmaConstants.SYS_ADMIN_LOGIN_NAME.equals(login_name);
            for (int i = 0; i < sel_cnt; i++) {
                ConnectedUserModel model = sel_conns[i];
                String conn_store = model.getStoreId();
                boolean is_store_admin = (conn_store != null) && docmaSess.hasAdminRight(conn_store);
                if (! (is_sys_admin || is_store_admin)) {
                    Messagebox.show("You cannot close session '" + model.getSessionId() +
                                    "'. Missing administration rights!");
                    continue;
                }
                DocmaSession sess = docmaApp.getOpenSession(model.getSessionId());
                if (sess == docmaSess) {
                    Messagebox.show("You cannot close your own session!");
                    continue;
                }
                if (sess != null) {
                    sess.closeSession();
                }
            }
            updateUserList();
        }
    }

    public void showConnectedUsers(DocmaApplication docmaApp, 
                                   DocmaSession docmaSess,
                                   String storeId,
                                   DocVersionId verId)
    throws Exception
    {
        this.docmaApp = docmaApp;
        this.docmaSess = docmaSess;
        this.storeId = storeId;
        this.verId = verId;
        DocmaWebSession webSess = GUIUtil.getDocmaWebSession(this);
        this.mainWin = webSess.getMainWindow();
        this.listitem_renderer = this.mainWin.getListitemRenderer();
        updateUserList();
        doModal();
    }

    private void updateUserList()
    {
        users_listbox = (Listbox) getFellow("ConnectedUsersListbox");
        if (users_listmodel == null) {
            users_listmodel = new ListModelList();
            users_listbox.setModel(users_listmodel);
            users_listbox.setItemRenderer(listitem_renderer);
        }
        users_listmodel.clear();

        UserManager um = docmaSess.getUserManager();
        DocmaSession[] sessions = docmaApp.getOpenSessions();
        List model_list = new ArrayList(sessions.length);
        for (int i=0; i < sessions.length; i++) {
            DocmaSession sess = (DocmaSession) sessions[i];
            String sess_userId = sess.getUserId();
            String sess_storeId = sess.getStoreId();
            DocVersionId sess_verId = sess.getVersionId();
            if (((storeId == null) || storeId.equals(sess_storeId)) &&
                ((verId == null) || verId.equals(sess_verId))) {
                ConnectedUserModel model = new ConnectedUserModel();
                model.setUserName(um.getUserNameFromId(sess_userId));
                model.setFirstName(um.getUserProperty(sess_userId, DocmaConstants.PROP_USER_FIRST_NAME));
                model.setLastName(um.getUserProperty(sess_userId, DocmaConstants.PROP_USER_LAST_NAME));
                model.setSessionId(sess.getSessionId());
                if (sess_storeId == null) sess_storeId = "";
                model.setStoreId(sess_storeId);
                String ver_str = (sess_verId == null) ? "" : sess_verId.toString();
                model.setVersionId(ver_str);
                model_list.add(model);
            }
        }
        Collections.sort(model_list);  // sort by username
        users_listmodel.addAll(model_list);
    }

}
