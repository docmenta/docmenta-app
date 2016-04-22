/*
 * ApplicationContextImpl.java
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
import org.docma.plugin.*;
import org.docma.userapi.UserManager;

/**
 * Provides the plugin interface for DocmaApplication.
 * This is a wrapper class which provides the functionality
 * of DocmaApplication to plugins. This way it is possible to change the
 * implementation/interface of DocmaApplication without breaking the 
 * functionality of existing plugins.
 * Furthermore this wrapper may restrict the functionality that is provided
 * to plugins.
 *
 * @author MP
 */
public class ApplicationContextImpl implements ApplicationContext
{
    private DocmaApplication docmaApp;
    
    public ApplicationContextImpl(DocmaApplication app)
    {
        this.docmaApp = app;
    }

    // ********* Interface ApplicationContext (visible by plugins) **********

    public boolean hasObject(String objectName) 
    {
        return docmaApp.getApplicationServices().hasInstance(objectName);
    }

    public Object getObject(String objectName) 
    {
        return docmaApp.getApplicationServices().getInstance(objectName);
    }

    public void setObject(String objectName, Object instance) 
    {
        docmaApp.getApplicationServices().setInstance(objectName, instance);
    }
    
    public UserManager getUserManager() 
    {
        return getDocmaApplication().getUserManager();
    }

    public void setUserManager(UserManager um) 
    {
        if (um == null) {
            throw new RuntimeException("UserManager instance must not be null!");
        }
        getDocmaApplication().setUserManager(um);
    }

    public boolean userExists(String userId)
    {
        throw new RuntimeException("Method not implemented.");
    }

    /**
     * Returns the User object for the given user ID.
     * Note that an object is returned no matter whether the user exists or
     * not. To check whether a user with the given ID exists or not, the 
     * method userExists(userID) has to be called.
     * 
     * @param userId The ID of the user.
     * @return The User object for the given ID.
     */    
    public User getUser(String userId)
    {
        return new UserImpl(getUserManager(), userId);
    }

    // ********* Public methods (not directly visible by plugins) **********
    
    public DocmaApplication getDocmaApplication()
    {
        return this.docmaApp;
    }

}
