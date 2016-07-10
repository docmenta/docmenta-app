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
import org.docma.coreapi.DocI18n;
import org.docma.userapi.*;
import org.zkoss.zul.*;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;

/**
 *
 * @author MP
 */
public class UserGroupDialog extends Window // implements EventListener, Deferrable
{
    public static final String EVENT_UPDATED = "onUpdated";
    
    private static final int MODE_NEW = 0;
    private static final int MODE_EDIT = 1;
    private static final int MODE_COPY = 2;

    private int mode = -1;

    private Textbox groupNameBox;
    private Textbox groupDNBox;

    private UserGroupModel groupModel = null;
    private UserLoader userLoader = null;
    private Callback callback = null;

    
    public void onOkayClick()
    {
        UserManager um = userLoader.getUserManager();
        if (hasInvalidInputs(um)) {
            return;  // keep dialog opened
        }
        try {
            updateModel(groupModel, userLoader);
        } catch (Exception ex) {
            Messagebox.show("Error: " + ex.getMessage());
            return;  // keep dialog opened
        }
        setVisible(false);   // close dialog
    }

    public void onCancelClick()
    {
        setVisible(false);   // close dialog
    }

    public void onSetFocus()
    {
        // groupNameBox = (Textbox) getFellow("UserGroupNameTextbox");
        groupNameBox.setFocus(true);
    }

    public void newGroup(UserGroupModel grp, UserLoader loader, Callback callback) 
    {
        mode = MODE_NEW;
        setTitle(label("label.usergroup.dialog.new.title"));
        doEditGroup(grp, loader, callback);
    }

    public void editGroup(UserGroupModel grp, UserLoader loader, Callback callback)
    {
        mode = MODE_EDIT;
        setTitle(label("label.usergroup.dialog.edit.title"));
        doEditGroup(grp, loader, callback);
    }

    public void copyGroup(UserGroupModel grp, UserLoader loader, Callback callback)
    {
        mode = MODE_COPY;
        setTitle(label("label.usergroup.dialog.copy.title"));
        doEditGroup(grp, loader, callback);
    }

    private void doEditGroup(UserGroupModel grp, UserLoader loader, Callback callback)
    {
        this.groupModel = grp;
        this.userLoader = loader;
        this.callback = callback;
        
        // addEventListener("onOpen", this);
        this.groupNameBox = (Textbox) getFellow("UserGroupNameTextbox");
        this.groupDNBox = (Textbox) getFellow("UserGroupDNTextbox");
        UserManager um = loader.getUserManager();
        updateGUI(grp, um); // init dialog fields
        groupNameBox.setFocus(true);
        // Events.echoEvent("onSetFocus", this, null);
        doHighlighted();
    }
    
    public void onUpdateGroup(Event evt)
    {
        String warning_error = null;
        try {
            Object[] params = (Object[]) evt.getData();
            UserGroupModel grp = (UserGroupModel) params[0];
            UserLoader loader = (UserLoader) params[1];
            warning_error = updateModelNow(grp, loader);
        } catch (Exception ex) {
            warning_error = "Error: " + ex.getMessage();
        } finally {
            Clients.clearBusy();
        }
        if ((warning_error != null) && (warning_error.length() > 0)) {
            Messagebox.show(warning_error);
        }
    }

    private boolean hasInvalidInputs(UserManager um)
    {
        try {
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
        } catch (Exception ex) {
            Messagebox.show("Error: " + ex.getMessage());
            return true;
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

    private void updateModel(UserGroupModel grp, UserLoader loader) throws Exception
    {
        UserManager um = loader.getUserManager();
        boolean isDir = um instanceof DirectoryUserManager;
        if (isDir) {
            // As this can be a long operation, show busy message
            Clients.showBusy("Updating group information...");
            Events.echoEvent("onUpdateGroup", this, new Object[] {grp, loader} );
        } else {
            updateModelNow(grp, loader);
        }
    }
    
    private String updateModelNow(UserGroupModel grp, UserLoader loader) throws Exception
    {
        UserManager um = loader.getUserManager();
        if (um == null) {
            throw new Exception("UserManager is null.");
        }
        boolean isDir = um instanceof DirectoryUserManager;
        String gname = groupNameBox.getValue().trim();
        String dn = isDir ? groupDNBox.getValue().trim() : "";
        boolean local_group = dn.equals("");
        
        if (mode == MODE_NEW) {
            String gid = um.createGroup(gname);
            grp.setGroupId(gid);
        } else if (mode == MODE_COPY) {
            // grp is a new instance where access rights, members and group-DN 
            // has been copied from an existing group. The textbox groupNameBox 
            // contains the new name that the user has entered. The textbox
            // groupDNBox contains the new DN that the user has entered.
            
            String gid = um.createGroup(gname);
            grp.setGroupId(gid);
            
            String rights_str = AccessRights.toString(grp.getAccessRights());
            um.setGroupProperty(gid, DocmaConstants.PROP_USERGROUP_RIGHTS, rights_str);

            if (local_group) {  
                // Copy members if its a local group. Note: If it's a directory 
                // group then members will be loaded from the directory-server 
                // based on the group-DN that the user has entered (see 
                // "Update directory information" section below).
                UserModel[] members = grp.getMembers(loader);
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
            DirectoryUserManager dum = (DirectoryUserManager) um;
            String gid = grp.getGroupId();
            dum.setGroupDN(gid, dn);
            grp.setGroupDN(dn);
            if (! local_group) {  // dn.length() > 0
                if (dum.groupDNExists(dn)) {  
                    // Read membership information from directory server.
                    // Create members as directory users (if not already existent).
                    int count_members = dum.synchronizeDirectoryGroupMembers(gid);
                    if (count_members == 0) {
                        warning = "The directory group \n \n " + dn + 
                                  " \n \nhas currently no members!";
                    }

                    // The group DN may have changed. Therefore the group members
                    // need to be updated.
                    grp.loadMembers(loader);
                } else {
                    // If group dn is not found on LDAP server, then display
                    // the group as an empty group.
                    grp.setMembers(null);   // clear members
                    warning = "Directory group \n \n " + dn + 
                              " \n \nnot found on directory-server!";
                }
            }
        }
        
        // Notify caller that model has been updated
        if (callback != null) {
            callback.onEvent(EVENT_UPDATED);
        }

        return warning;
    }

    private String label(String key, Object... args)
    {
        return GUIUtil.getI18n(this).getLabel(key, args);
    }
}
