/*
 * UserImpl.java
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
package org.docma.plugin.implementation;

import org.docma.app.*;
import org.docma.plugin.User;
import org.docma.userapi.UserManager;

/**
 *
 * @author MP
 */
public class UserImpl implements User
{
    private final UserManager usrManager;
    private String id = null;
    
    UserImpl(UserManager usrManager, String user_id)
    {
        this.usrManager = usrManager;
        this.id = user_id;
    }

    public String getId() 
    {
        return id;
    }

    public String getLoginId() 
    {
        return usrManager.getUserNameFromId(id);
    }

    public String getFirstName() 
    {
        String nm = getProperty(DocmaConstants.PROP_USER_FIRST_NAME);
        return (nm == null) ? "" : nm;
    }

    public String getLastName() 
    {
        String nm = getProperty(DocmaConstants.PROP_USER_LAST_NAME);
        return (nm == null) ? "" : nm;
    }

    public String getName() 
    {
        return (getFirstName() + " " + getLastName()).trim();
    }

    public String getNameOrId() 
    {
        String nm = getName();
        return nm.equals("") ? getId() : nm;
    }
    
    public String getProperty(String name) 
    {
        return usrManager.getUserProperty(id, name);
    }

    public void setProperty(String name, String value) throws Exception
    {
        usrManager.setUserProperty(id, name, value);
    }

    public void setProperties(String[] names, String[] values) throws Exception 
    {
        usrManager.setUserProperties(id, names, values);
    }

}
