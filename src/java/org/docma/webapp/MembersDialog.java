/*
 * MembersDialog.java
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
import org.docma.app.ui.*;
import org.docma.userapi.*;
import org.zkoss.zul.*;

import java.util.*;

/**
 *
 * @author MP
 */
public class MembersDialog extends Window implements ListitemRenderer
{
    private int modalResult = -1;

    private Listbox addMembersBox;
    private final ListModelList addMembersModel = new ListModelList();

    private UserGroupModel grp;


    public void onAddClick()
    {
        modalResult = GUIConstants.MODAL_OKAY;
        setVisible(false);
    }

    public void onCancelClick()
    {
        modalResult = GUIConstants.MODAL_CANCEL;
        setVisible(false);
    }

    public boolean doAddMembers(UserGroupModel grp, DocmaSession docmaSess, UserLoader userLoader)
    throws Exception
    {
        this.grp = grp;

        addMembersModel.clear();
        addMembersModel.setMultiple(true);
        loadUsers(userLoader);

        addMembersBox = (Listbox) getFellow("AddMembersListbox");
        addMembersBox.setModel(addMembersModel);
        addMembersBox.setItemRenderer(this);

        do {
            modalResult = -1;
            doModal();
            if (modalResult != GUIConstants.MODAL_OKAY) {
                return false;
            }
            if (addMembersBox.getSelectedCount() < 1) {
                Messagebox.show("Please select one or more users!");
                continue;
            }
            try {
                updateModel(userLoader);
            } catch (Exception ex) {
                Messagebox.show(ex.getLocalizedMessage());
                return false;
            }
            return true;
        } while (true);
    }

    private void loadUsers(UserLoader uloader)
    {
        ArrayList tempList = new ArrayList(Arrays.asList(uloader.getUsers()));
        UserModel[] members = grp.getMembers(uloader);
        tempList.removeAll(Arrays.asList(members));
        addMembersModel.addAll(tempList);
    }

    private void updateModel(UserLoader loader) throws Exception
    {
        // Calculate updated list of members
        ArrayList memberList = new ArrayList(Arrays.asList(grp.getMembers(loader)));
        int start_idx = memberList.size();
        Set sel_set = addMembersBox.getSelectedItems();
        String[] addedUserIds = new String[sel_set.size()];
        Iterator it = sel_set.iterator();
        for (int i=0; i < addedUserIds.length; i++) {
            Listitem sel_item = (Listitem) it.next();
            UserModel usr = (UserModel) sel_item.getValue();
            addedUserIds[i] = usr.getUserId();
            memberList.add(usr);   // add new member
        }

        // Update user store (persistence layer)
        UserManager um = loader.getUserManager();
        um.addUsersToGroup(addedUserIds, grp.getGroupId());
        
        // Refresh UI model
        UserModel[] new_members = new UserModel[memberList.size()];
        new_members = (UserModel[]) memberList.toArray(new_members);
        for (int i = start_idx; i < new_members.length; i++) {
            // Refresh groups list of added members
            new_members[i].refreshGroups(um);
        }
        grp.setMembers(new_members);
    }

    public void render(Listitem item, Object data, int index) throws Exception
    {
        if (data instanceof UserModel) {
            UserModel usr = (UserModel) data;
            Listcell c1 = new Listcell(usr.getLoginName());
            Listcell c2 = new Listcell(usr.getLastName());
            Listcell c3 = new Listcell(usr.getFirstName());
            item.appendChild(c1);
            item.appendChild(c2);
            item.appendChild(c3);
            // item.addForward("onDoubleClick", "mainWin", "onEditMember");
        }
        item.setValue(data);
    }

}
