/*
 * UserGroupModel.java
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

package org.docma.app.ui;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.docma.app.*;
import org.docma.userapi.*;

/**
 * UserGroupModel is the model of a user-group that is used on the user-interface
 * layer. It holds user-group information to be displayed in the GUI.
 * It is a leightweight object, i.e. it has no direct connection to the 
 * persistence layer. If user information has to be retrieved from the 
 * persistence layer, then the method load() has to be called.
 * 
 * @author MP
 */
public class UserGroupModel
{
    // private UserManager um;
    // private UserLoader loader;

    private String groupId;
    private String groupName;
    private AccessRights[] accessRights = null;
    private List<UserModel> members = null;
    private String groupDN = null;

    /**
     * Creates a new instance of UserGroupModel.
     * The group id and name is set to empty string.
     */
    public UserGroupModel()
    {
        this.groupId = "";
        this.groupName = "";
    }

    /**
     * Creates a new instance of UserGroupModel.
     * The group id is initialized with the passed value.
     * The group name is set to empty string.
     * 
     * @param groupId The group id.
     */
    UserGroupModel(String groupId)
    {
        // this.um = um;
        // this.loader = loader;
        this.groupId = groupId;
        this.groupName = "";
    }

    /**
     * Reads user-group information from the persistence layer for the given group ID. 
     * Before calling this method, the group ID needs to be set.
     * For accessing the persistence layer, an instance of UserLoader
     * has to be passed as argument.
     * For performance reasons, this method does not retrieve the list of members.
     * The list of members is retrieved when the method getMembers() is called 
     * for the first time (lazy loading). 
     * To explicitely load the list of members the method loadMembers() could be 
     * called (e.g. to refresh the list of members if information in the 
     * persistence layer has changed).
     * 
     * @param loader An instance of UserLoader.
     */
    public void load(UserLoader loader)
    {
        UserManager um = loader.getUserManager();
        
        groupName = um.getGroupNameFromId(groupId);

        String rights_str = um.getGroupProperty(groupId, DocmaConstants.PROP_USERGROUP_RIGHTS);
        accessRights = AccessRights.parseAccessRights(rights_str);
        
        if (um instanceof DirectoryUserManager) {
            DirectoryUserManager dum = (DirectoryUserManager) um;
            groupDN = dum.getGroupDN(groupId);
        } else {
            groupDN = null;
        }

        // loadMembers(loader);
    }

    /**
     * Reads the group members from the persistence layer for the given group ID.
     * Before calling this method, the group ID needs to be set.
     * 
     * @param loader An instance of UserLoader.
     */
    public void loadMembers(UserLoader loader)
    {
        String[] member_ids = loader.getUserManager().getUsersInGroup(groupId);
        members = new ArrayList<UserModel>(member_ids.length);
        for (String mid : member_ids) {
            UserModel mem = loader.getUser(mid);
            if (mem != null) {
                members.add(mem);
            } else {
                if (DocmaConstants.DEBUG) {
                    System.out.println("Could not load group member for group '" + groupName + "': " + mid);
                }
            }
        }
    }
    
    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId(String groupId)
    {
        this.groupId = groupId;
    }

    public String getGroupName()
    {
        return groupName;
    }

    public void setGroupName(String groupName)
    {
        this.groupName = groupName;
    }

    public AccessRights[] getAccessRights()
    {
        if (accessRights == null) accessRights = new AccessRights[0];
        return accessRights;
    }

    public void setAccessRights(AccessRights[] accessRights)
    {
        this.accessRights = (AccessRights[]) accessRights.clone();
    }

    public UserModel[] getMembers(UserLoader loader)
    {
        if (members == null) {
            if ((groupId == null) || groupId.equals("")) {
                return new UserModel[0];
            }
            loadMembers(loader);
        }
        return members.toArray(new UserModel[members.size()]);
    }

    public void setMembers(UserModel[] users)
    {
        if (members == null) {
            members = new ArrayList<UserModel>();
        } else {
            members.clear();
        }
        if (users != null) {
            members.addAll(Arrays.asList(users));
        }
    }
    
    public boolean isDirectoryGroup()
    {
        return (groupDN != null) && (groupDN.length() > 0);
    }

    public String getGroupDN()
    {
        return groupDN;
    }
    
    public void setGroupDN(String grpDN)
    {
        this.groupDN = grpDN;
    }

}
