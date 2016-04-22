/*
 * UserGroupDialog.java
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
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Deferrable;

import java.util.*;

/**
 *
 * @author MP
 */
public class UserGroupDialog extends Window // implements EventListener, Deferrable
{
    private static final int MODE_NEW = 0;
    private static final int MODE_EDIT = 1;
    private static final int MODE_COPY = 2;

    private int modalResult = -1;
    private int mode = -1;

    private Textbox groupNameBox;
    private Textbox groupDNBox;

    public void onOkayClick()
    {
        modalResult = GUIConstants.MODAL_OKAY;
        setVisible(false);
    }

    public void onCancelClick()
    {
        modalResult = GUIConstants.MODAL_CANCEL;
        setVisible(false);
    }

    public void onSetFocus()
    {
        // groupNameBox = (Textbox) getFellow("UserGroupNameTextbox");
        groupNameBox.setFocus(true);
    }

    public void setMode_NewGroup()
    {
        mode = MODE_NEW;
        setTitle(GUIUtil.i18(this).getLabel("label.usergroup.dialog.new.title"));
    }

    public void setMode_EditGroup()
    {
        mode = MODE_EDIT;
        setTitle(GUIUtil.i18(this).getLabel("label.usergroup.dialog.edit.title"));
    }

    public void setMode_CopyGroup()
    {
        mode = MODE_COPY;
        setTitle(GUIUtil.i18(this).getLabel("label.usergroup.dialog.copy.title"));
    }

    public boolean doEditGroup(UserGroupModel grp, DocmaSession docmaSess)
    throws Exception
    {
        // addEventListener("onOpen", this);
        groupNameBox = (Textbox) getFellow("UserGroupNameTextbox");
        groupDNBox = (Textbox) getFellow("UserGroupDNTextbox");
        UserManager um = docmaSess.getUserManager();
        updateGUI(grp, um); // init dialog fields
        do {
            modalResult = -1;
            groupNameBox.setFocus(true);
            // Events.echoEvent("onSetFocus", this, null);
            doModal();
            if (modalResult != GUIConstants.MODAL_OKAY) {
                return false;
            }
            if (hasInvalidInputs(um)) {
                continue;
            }
            updateModel(grp, um);
            return true;
        } while (true);
    }
    
    public void onUpdateGroup(Event evt)
    {
        String warning_error = null;
        try {
            Object[] params = (Object[]) evt.getData();
            UserGroupModel grp = (UserGroupModel) params[0];
            UserManager um = (UserManager) params[1];
            warning_error = updateModelNow(grp, um);
        } catch (Exception ex) {
            warning_error = "Error: " + ex.getMessage();
        } finally {
            Clients.clearBusy();
        }
        if ((warning_error != null) && (warning_error.length() > 0)) {
            Messagebox.show(warning_error);
        }
    }

    private boolean hasInvalidInputs(UserManager um) throws Exception
    {
        String name = groupNameBox.getValue().trim();
        if (name.equals("")) {
            Messagebox.show("Please enter a group name!");
            return true;
        }
        if (name.length() > 40) {
            Messagebox.show("Group name is too long. Maximum length is 40 characters.");
            return true;
        }
        if (! name.matches(DocmaConstants.REGEXP_NAME)) {
            Messagebox.show("Invalid group name. Allowed characters are letters, digits and punctuation characters.");
            return true;
        }
        if ((mode == MODE_NEW) || (mode == MODE_COPY)) {
            if (um.getGroupIdFromName(name) != null) {
                Messagebox.show("A group with this name already exists.");
                return true;
            }
        }
        return false;
    }

    private void updateGUI(UserGroupModel grp, UserManager um)
    {
        groupNameBox.setValue(grp.getGroupName());
        if (um instanceof DirectoryUserManager) {
            groupDNBox.setDisabled(false);
            String grpDN = grp.getGroupDN();
            groupDNBox.setValue((grpDN == null) ? "" : grpDN);
        } else {
            groupDNBox.setValue("");
            groupDNBox.setDisabled(true);
        }
    }

    private void updateModel(UserGroupModel grp, UserManager um) throws Exception
    {
        boolean isDir = um instanceof DirectoryUserManager;
        if (isDir) {
            // As this can be a long operation, show busy message
            Clients.showBusy("Contacting directory server...");
            Events.echoEvent("onUpdateGroup", this, new Object[] {grp, um} );
        } else {
            updateModelNow(grp, um);
        }
    }
    
    private String updateModelNow(UserGroupModel grp, UserManager um) throws Exception
    {
        boolean isDir = um instanceof DirectoryUserManager;
        DirectoryUserManager dum = isDir ? ((DirectoryUserManager) um) : null;
        String gname = groupNameBox.getValue().trim();
        String dn = isDir ? groupDNBox.getValue().trim() : "";
        
        if (mode == MODE_NEW) {
            String gid = um.createGroup(gname);
            grp.setGroupId(gid);
        } else
        if (mode == MODE_COPY) {
            // grp is a new instance where access rights, members and group-DN 
            // has been copied from an existing group. The textbox groupNameBox 
            // contains the new name that the user has entered. The textbox
            // groupDNBox contains the new DN that the user has entered.
            
            String gid = um.createGroup(gname);
            grp.setGroupId(gid);
            
            String rights_str = AccessRights.toString(grp.getAccessRights());
            um.setGroupProperty(gid, DocmaConstants.PROP_USERGROUP_RIGHTS, rights_str);

            if (! isDir) {  
                // Copy members if its a local group. Note: If it's a directory 
                // group then members will be loaded from the directory-server 
                // based on the group-DN that the user has entered (see 
                // "Update directory information" section below).
                UserModel[] members = grp.getMembers();
                String[] member_ids = new String[members.length];
                for (int i=0; i < members.length; i++) {
                    member_ids[i] = members[i].getUserId();
                }
                um.addUsersToGroup(member_ids, gid);
            }
        } else {
            um.setGroupName(grp.getGroupId(), gname);
        }
        grp.setGroupName(gname);

        String warning = null;
        // Update directory information
        if (isDir) {
            String gid = grp.getGroupId();
            dum.setGroupDN(gid, dn);
            grp.setGroupDN(dn);
            if (dum.groupDNExists(dn)) {  
                // Read membership information from directory server.
                // Create members as directory users (if not already existent).
                int count_members = dum.synchronizeDirectoryGroupMembers(gid);
                if (count_members == 0) {
                    warning = "The directory group \n \n " + dn + 
                              " \n \nhas currently no members!";
                }
            } else {
                warning = "Directory group \n \n " + dn + 
                          " \n \nnot found on directory-server!";
            }
        }
        return warning;
    }

}
