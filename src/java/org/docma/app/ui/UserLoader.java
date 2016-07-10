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
 * For each user session, a UserLoader instance is created, that caches
 * information from the user-store (persistence layer). 
 * The persistence layer is accessed through the UserManager interface.
 * Therefore, an instance of UserManager needs to be provided in the constructor.
 * For efficiency reasons, the user information should only be retrieved 
 * through the UserLoader instance. 
 * However, the UserLoader can only be used for read access.
 * For updating user information in the datastore, the UserManager object
 * is required.
 * 
 * @author MP
 */
public class UserLoader
{
    private final UserManager um;
    private final Map userMap = new HashMap(500);
    // private Map groupMap = new HashMap(100);

    /* -----------  Constructors  ------------------ */
    
    public UserLoader(UserManager um)
    {
        if (um == null) {
            throw new RuntimeException("Failed to construct UserLoader: UserManager is null.");
        }
        this.um = um;
    }

    /* -----------  Public methods  ------------------ */

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
        List<UserModel> usr_list = getUserList();
        return usr_list.toArray(new UserModel[usr_list.size()]);
    }

    public List<UserModel> getUserList()
    {
        String[] ids = um.getUserIds();
        List<UserModel> usr_list = new ArrayList<UserModel>(ids.length);
        for (String id : ids) {
            UserModel usr = getUser(id);
            if (usr != null) usr_list.add(usr);
        }
        return usr_list;
    }

    public UserGroupModel getGroup(String groupId)
    {
        // UserGroupModel grp = (UserGroupModel) groupMap.get(groupId);
        // if (grp == null) {
        //     grp = new UserGroupModel(groupId, um, this);
        //     groupMap.put(groupId, grp);
        // }
        UserGroupModel grp = new UserGroupModel(groupId);
        grp.load(this);   // read group information from user store
        return grp;
    }

    public UserGroupModel[] getGroups()
    {
        String[] ids = um.getGroupIds();
        UserGroupModel[] grp_arr = new UserGroupModel[ids.length];
        for (int i=0; i < ids.length; i++) {
            grp_arr[i] = getGroup(ids[i]);
        }
        return grp_arr;
    }

    public UserManager getUserManager()
    {
        return um;
    }

    /* -----------  Package local methods  ------------------ */
    
}
