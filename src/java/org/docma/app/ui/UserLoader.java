/*
 * UserLoader.java
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

import org.docma.userapi.*;
import org.docma.util.Log;

import java.util.*;

/**
 *
 * @author MP
 */
public class UserLoader
{
    private UserManager um;
    private Map userMap = new HashMap(500);
    // private Map groupMap = new HashMap(100);

    public UserLoader(UserManager um)
    {
        this.um = um;
    }

    public UserModel getUser(String userId)
    {
        UserModel usr = (UserModel) userMap.get(userId);
        if (usr == null) {
            try {
                usr = new UserModel(userId, um);
                userMap.put(userId, usr);
            } catch (Exception ex) {
                Log.error("Cannot load user: '" + userId +
                          "'. Exception in UserLoader.getUser(): " + ex.getMessage());
                return null;
            }
        }
        return usr;
    }

    public UserModel[] getUsers()
    {
        String[] ids = um.getUserIds();
        List usr_list = new ArrayList(ids.length);
        for (int i=0; i < ids.length; i++) {
            UserModel usr = getUser(ids[i]);
            if (usr != null) usr_list.add(usr);
        }
        UserModel[] usr_arr = new UserModel[usr_list.size()];
        usr_arr = (UserModel[]) usr_list.toArray(usr_arr);
        return usr_arr;
    }

    public UserGroupModel getGroup(String groupId)
    {
        // UserGroupModel grp = (UserGroupModel) groupMap.get(groupId);
        // if (grp == null) {
        //     grp = new UserGroupModel(groupId, um, this);
        //     groupMap.put(groupId, grp);
        // }
        return new UserGroupModel(groupId, um, this);  // grp;
    }

    public UserGroupModel[] getGroups()
    {
        String[] ids = um.getGroupIds();
        UserGroupModel[] grp_arr = new UserGroupModel[ids.length];
        for (int i=0; i < ids.length; i++) {
            grp_arr[i] = new UserGroupModel(ids[i], um, this); // getGroup(ids[i]);
        }
        return grp_arr;
    }


}
