/*
 * GUI_List_UsersAndGroups.java
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
package org.docma.webapp;

import java.util.Arrays;
import java.util.Iterator;
import org.docma.app.AccessRights;
import org.docma.app.DocmaSession;
import org.docma.app.ui.UserGroupModel;
import org.docma.app.ui.UserLoader;
import org.docma.app.ui.UserModel;
import org.docma.userapi.UserManager;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Listheader;
import org.zkoss.zul.Messagebox;

/**
 *
 * @author MP
 */
public class GUI_List_UsersAndGroups 
{
    private final MainWindow mainWin;

    private final Listbox        groups_listbox;
    private final ListModelList  groups_listmodel;
    private final Listbox        rights_listbox;
    private final ListModelList  rights_listmodel;
    private final Listbox        members_listbox;
    private final ListModelList  members_listmodel;
    private final Listbox        users_listbox;
    private final ListModelList  users_listmodel;

    private final Label rights_label;
    private final Label members_label;
    private final Listheader members_name_listheader;
    private final Listheader users_name_listheader;
    
    public GUI_List_UsersAndGroups(MainWindow mainWin) 
    {
        this.mainWin = mainWin;
        
        UserGroupsListitemRenderer groups_renderer = new UserGroupsListitemRenderer(mainWin);
        groups_listbox = (Listbox) mainWin.getFellow("GroupsListbox");
        groups_listmodel = new ListModelList();
        groups_listmodel.setMultiple(true);
        groups_listbox.setModel(groups_listmodel);
        groups_listbox.setItemRenderer(groups_renderer);
        rights_listbox = (Listbox) mainWin.getFellow("AccessRightsListbox");
        rights_listmodel = new ListModelList();
        rights_listbox.setModel(rights_listmodel);
        rights_listbox.setItemRenderer(groups_renderer);
        members_listbox = (Listbox) mainWin.getFellow("GroupMembersListbox");
        members_listmodel = new ListModelList();
        members_listmodel.setMultiple(true);
        members_listbox.setModel(members_listmodel);
        members_listbox.setItemRenderer(groups_renderer);
        users_listbox = (Listbox) mainWin.getFellow("UsersListbox");
        users_listmodel = new ListModelList();
        users_listbox.setModel(users_listmodel);
        users_listbox.setItemRenderer(mainWin.getListitemRenderer());
        
        rights_label = (Label) mainWin.getFellow("AccessRightsHeadLabel");
        members_label = (Label) mainWin.getFellow("GroupMembersHeadLabel");
        members_name_listheader = (Listheader) mainWin.getFellow("GroupMembersNameListheader");
        users_name_listheader = (Listheader) mainWin.getFellow("UsersLoginNameListheader");
    }

    void loadUsers()
    {
        users_listmodel.clear();
        users_listmodel.addAll(mainWin.getUserLoader().getUserList());
        users_name_listheader.sort(true, true); // sort list by login name
    }

    void loadUserGroups()
    {
        groups_listmodel.clear();
        groups_listmodel.addAll(Arrays.asList(mainWin.getUserLoader().getGroups()));
    }

    void loadUsersIfEmpty()
    {
        if (users_listmodel.isEmpty()) {
            loadUsers();
        }
    }
    
    void loadUserGroupsIfEmpty()
    {
        if (groups_listmodel.isEmpty()) {
            loadUserGroups();
        }
    }

    /**
     * Shows busy message and creates event onShowUserGroupData.
     */
    public void onSelectUserGroup()
    {
        Clients.showBusy("Retrieving group information...");
        Events.echoEvent("onShowUserGroupData", mainWin, null);
    }

    /**
     * This is a follow-up event created by onSelectUserGroup.
     */    
    public void onShowUserGroupData()
    {
        String error = null;
        try {
            fillUserGroupData();
        } catch (Exception ex) {
            error = "Error: " + ex.getMessage();
        } finally {
            Clients.clearBusy();
        }
        if (error != null) {
            Messagebox.show(error);
        }
    }
    
    private void fillUserGroupData()
    {
        String group_name = "";
        rights_listmodel.clear();
        members_listmodel.clear();
        int sel_idx = groups_listbox.getSelectedIndex();
        if (sel_idx >= 0) {
            UserLoader loader = mainWin.getUserLoader();
            UserGroupModel ug = (UserGroupModel) groups_listmodel.get(sel_idx);
            rights_listmodel.addAll(Arrays.asList(ug.getAccessRights()));
            members_listmodel.addAll(Arrays.asList(ug.getMembers(loader)));
            members_name_listheader.sort(true, true);  // sort list by login name
            group_name = ug.getGroupName();
        }
        rights_label.setValue(mainWin.i18n("label.accessrights_of_group.listhead", group_name));
        members_label.setValue(mainWin.i18n("label.members_of_group.listhead", group_name));
    }

    public void onNewUserGroup() throws Exception
    {
        UserGroupDialog dialog = (UserGroupDialog) mainWin.getPage().getFellow("UserGroupDialog");
        final UserGroupModel grp = new UserGroupModel();
        dialog.newGroup(grp, mainWin.getUserLoader(), new Callback() {
            public void onEvent(String evt) {
                if (UserGroupDialog.EVENT_UPDATED.equals(evt)) {
                    // int ins_pos = -(Collections.binarySearch(users_listmodel, usr)) - 1;
                    groups_listmodel.add(grp);
                }
            }
        });
    }

    public void onEditUserGroup() throws Exception
    {
        UserGroupDialog dialog = (UserGroupDialog) mainWin.getPage().getFellow("UserGroupDialog");
        if (groups_listbox.getSelectedCount() <= 0) {
            Messagebox.show("Please select a group from the list!");
            return;
        }
        final int sel_idx = groups_listbox.getSelectedIndex();
        final UserGroupModel grp = (UserGroupModel) groups_listmodel.getElementAt(sel_idx);
        dialog.editGroup(grp, mainWin.getUserLoader(), new Callback() {
            public void onEvent(String evt) {
                if (UserGroupDialog.EVENT_UPDATED.equals(evt)) {
                    groups_listmodel.set(sel_idx, grp);        // update list entry in GUI
                    groups_listbox.setSelectedIndex(sel_idx);  // reselect list entry (workaround for bug)
                    fillUserGroupData();  // Update members list (just in case group DN has changed)
                }
            }
        });
    }

    public void onDeleteUserGroup() throws Exception
    {
        DocmaSession docmaSess = mainWin.getDocmaSession();
        if (groups_listbox.getSelectedCount() <= 0) {
            Messagebox.show("Please select a group from the list!");
            return;
        }
        int sel_idx = groups_listbox.getSelectedIndex();
        UserGroupModel grp = (UserGroupModel) groups_listmodel.getElementAt(sel_idx);
        if (Messagebox.show("Delete group '" + grp.getGroupName() + "'?", "Delete?",
            Messagebox.YES | Messagebox.NO, Messagebox.QUESTION) == Messagebox.YES) {
            UserManager um = docmaSess.getUserManager();
            um.deleteGroup(grp.getGroupId());
            groups_listmodel.remove(sel_idx);
        }
    }

    public void onCopyUserGroup() throws Exception
    {
        UserGroupDialog dialog = (UserGroupDialog) mainWin.getPage().getFellow("UserGroupDialog");
        if (groups_listbox.getSelectedCount() <= 0) {
            Messagebox.show("Please select a group to be copied!");
            return;
        }
        int sel_idx = groups_listbox.getSelectedIndex();
        UserLoader loader = mainWin.getUserLoader();
        UserGroupModel grp = (UserGroupModel) groups_listmodel.getElementAt(sel_idx);
        final UserGroupModel grp_new = new UserGroupModel();
        grp_new.setGroupName("Copy of " + grp.getGroupName());
        grp_new.setAccessRights(grp.getAccessRights());
        grp_new.setMembers(grp.getMembers(loader));
        grp_new.setGroupDN(grp.getGroupDN());
        dialog.copyGroup(grp_new, loader, new Callback() {
            public void onEvent(String evt) {
                if (UserGroupDialog.EVENT_UPDATED.equals(evt)) {
                    groups_listmodel.add(grp_new);
                }
            }
        });
    }


    public void onEditRights() throws Exception
    {
        AccessRightsDialog dialog = (AccessRightsDialog) mainWin.getPage().getFellow("AccessRightsDialog");
        DocmaSession docmaSess = mainWin.getDocmaSession();
        if (groups_listbox.getSelectedCount() <= 0) {
            Messagebox.show("Please select a group!");
            return;
        }
        int sel_idx = groups_listbox.getSelectedIndex();
        UserGroupModel grp = (UserGroupModel) groups_listmodel.getElementAt(sel_idx);
        AccessRights sel_rights = null;
        if (rights_listbox.getSelectedCount() > 0) {
            sel_rights = (AccessRights) rights_listmodel.get(rights_listbox.getSelectedIndex());
            // rights_listbox.clearSelection();
        }
        Clients.evalJavaScript("if (document.selection) document.selection.empty();");
        if (dialog.doEditRights(grp, sel_rights, docmaSess)) {
            rights_listmodel.clear();
            rights_listmodel.addAll(Arrays.asList(grp.getAccessRights()));
        }
        rights_listbox.invalidate();
    }


    public void onAddMember() throws Exception
    {
        MembersDialog dialog = (MembersDialog) mainWin.getPage().getFellow("MembersDialog");
        DocmaSession docmaSess = mainWin.getDocmaSession();
        if (groups_listbox.getSelectedCount() <= 0) {
            Messagebox.show("Please select a group!");
            return;
        }
        int sel_idx = groups_listbox.getSelectedIndex();
        UserLoader loader = mainWin.getUserLoader();
        UserGroupModel grp = (UserGroupModel) groups_listmodel.getElementAt(sel_idx);
        if (dialog.doAddMembers(grp, docmaSess, loader)) {
            members_listmodel.clear();
            members_listmodel.addAll(Arrays.asList(grp.getMembers(loader)));
            // members_listmodel.addAll(dialog.getAddedUsers());
            users_listmodel.clear(); // reload users when users tab is opened to show changed groups
        }
    }


    public void onRemoveMember() throws Exception
    {
        if (groups_listbox.getSelectedCount() <= 0) {
            Messagebox.show("Please select a group!");
            return;
        }
        int grp_idx = groups_listbox.getSelectedIndex();
        UserGroupModel grp = (UserGroupModel) groups_listmodel.getElementAt(grp_idx);

        int sel_cnt = members_listbox.getSelectedCount();
        if (sel_cnt <= 0) {
            Messagebox.show("Please select one or more members from the list!");
            return;
        }
        Iterator it = members_listbox.getSelectedItems().iterator();
        UserModel[] sel_users = new UserModel[sel_cnt];
        String[] sel_ids = new String[sel_cnt];
        for (int i=0; i < sel_cnt; i++) {
            Listitem item = (Listitem) it.next();
            sel_users[i] = (UserModel) item.getValue();
            sel_ids[i] = sel_users[i].getUserId();
        }
        String msg;
        if (sel_cnt == 1) {
            UserModel delusr = sel_users[0];
            msg = "Remover user '" + delusr.getLoginName() + "' from group '" +
                  grp.getGroupName() + "'?";
        } else {
            msg = "Remove " + sel_cnt + " users from group '" + grp.getGroupName() + "'?";
        }
        if (Messagebox.show(msg, "Delete?",
            Messagebox.YES | Messagebox.NO, Messagebox.QUESTION) == Messagebox.YES) {
            DocmaSession docmaSess = mainWin.getDocmaSession();
            UserManager um = docmaSess.getUserManager();
            um.removeUsersFromGroup(sel_ids, grp.getGroupId());
            for (UserModel sel_user : sel_users) {
                sel_user.refreshGroups(um);
            }
            members_listmodel.removeAll(Arrays.asList(sel_users));
            UserModel[] new_members = new UserModel[members_listmodel.size()];
            new_members = (UserModel[]) members_listmodel.toArray(new_members);
            grp.setMembers(new_members);
            users_listmodel.clear(); // rerender users list when users tab is opened to show changed groups
        }
    }


    public void onEditMember() throws Exception
    {
        doEditUser(members_listbox, members_listmodel);
        users_listmodel.clear(); // reload users when users tab is opened
    }


    public void onNewUser() throws Exception
    {
        UserDialog dialog = (UserDialog) mainWin.getPage().getFellow("UserDialog");
        DocmaSession docmaSess = mainWin.getDocmaSession();
        final UserModel usr = new UserModel();
        dialog.newUser(usr, docmaSess, new Callback() {
            public void onEvent(String evt) {
                if (UserDialog.EVENT_OKAY.equals(evt)) {
                    // int ins_pos = -(Collections.binarySearch(users_listmodel, usr)) - 1;
                    users_listmodel.add(usr);
                }
            }
        });
    }


    public void onEditUser() throws Exception
    {
        doEditUser(users_listbox, users_listmodel);
    }

    public void onEditUserProfile() throws Exception
    {
        DocmaSession docmaSess = mainWin.getDocmaSession();
        final UserModel usr = mainWin.getUserLoader().getUser(docmaSess.getUserId());
        // final String old_lang = usr.getGuiLanguage();
        UserDialog dialog = (UserDialog) mainWin.getPage().getFellow("UserDialog");
        dialog.editUser(usr, docmaSess, new Callback() {
            public void onEvent(String evt) {
                if (UserDialog.EVENT_OKAY.equals(evt)) {
                    if ((users_listmodel != null) && !users_listmodel.isEmpty()) {
                        loadUsers();  // reload users
                    }

                    // Following is commented out, because it didn't work.
                    // Switch UI language if it has changed.
                    // String new_lang = usr.getGuiLanguage();
                    // if ((new_lang != null) && !new_lang.equals("") && !new_lang.equals(old_lang)) {
                    //     GUIUtil.setCurrentUILanguage(mainWin, new_lang, true);
                    // }
                }
            }
        });
    }

    private void doEditUser(Listbox u_listbox, final ListModelList u_listmodel) throws Exception
    {
        UserDialog dialog = (UserDialog) mainWin.getPage().getFellow("UserDialog");
        DocmaSession docmaSess = mainWin.getDocmaSession();
        if (u_listbox.getSelectedCount() <= 0) {
            Messagebox.show("Please select a user from the list!");
            return;
        }
        final int sel_idx = u_listbox.getSelectedIndex();
        final UserModel usr = (UserModel) u_listmodel.getElementAt(sel_idx);
        dialog.editUser(usr, docmaSess, new Callback() {
            public void onEvent(String evt) {
                if (UserDialog.EVENT_OKAY.equals(evt)) {
                    u_listmodel.set(sel_idx, usr);
                }
            }
        });
    }


    public void onDeleteUser() throws Exception
    {
        DocmaSession docmaSess = mainWin.getDocmaSession();
        if (users_listbox.getSelectedCount() <= 0) {
            Messagebox.show("Please select a user from the list!");
            return;
        }
        int sel_idx = users_listbox.getSelectedIndex();
        UserModel usr = (UserModel) users_listmodel.getElementAt(sel_idx);
        if (Messagebox.show("Delete user '" + usr.getLoginName() + "'?", "Delete?",
            Messagebox.YES | Messagebox.NO, Messagebox.QUESTION) == Messagebox.YES) {
            UserManager um = docmaSess.getUserManager();
            um.deleteUser(usr.getUserId());
            users_listmodel.remove(sel_idx);
        }
    }


}
