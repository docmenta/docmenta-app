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

import org.docma.app.*;
import org.docma.userapi.*;

/**
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
    private UserModel[] members = null;
    private String groupDN = null;

    public UserGroupModel()
    {
        groupId = "";
        groupName = "";
    }

    UserGroupModel(String groupId, UserManager um, UserLoader loader)
    {
        // this.um = um;
        // this.loader = loader;
        this.groupId = groupId;
        load(um, loader);
    }

    void load(UserManager um, UserLoader loader)
    {
        groupName = um.getGroupNameFromId(groupId);

        String rights_str = um.getGroupProperty(groupId, DocmaConstants.PROP_USERGROUP_RIGHTS);
        accessRights = AccessRights.parseAccessRights(rights_str);

        String[] member_ids = um.getUsersInGroup(groupId);
        members = new UserModel[member_ids.length];
        for (int i=0; i < members.length; i++) {
            members[i] = loader.getUser(member_ids[i]);
        }

        if (um instanceof DirectoryUserManager) {
            DirectoryUserManager dum = (DirectoryUserManager) um;
            groupDN = dum.getGroupDN(groupId);
        } else {
            groupDN = null;
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

    public UserModel[] getMembers()
    {
        if (members == null) members = new UserModel[0];
        return members;
    }

    public void setMembers(UserModel[] members)
    {
        this.members = (UserModel[]) members.clone();
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
