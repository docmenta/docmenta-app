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

import java.util.Map;
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
    private final DocmaApplication docmaApp;
    private final LoggerImpl logger = new LoggerImpl();
    
    public ApplicationContextImpl(DocmaApplication app)
    {
        this.docmaApp = app;
    }

    // ********* Interface ApplicationContext (visible by plugins) **********

    public String getApplicationProperty(String name) throws DocmaException 
    {
        try {
            return docmaApp.getApplicationProperty(name);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void setApplicationProperty(String name, String value) throws DocmaException 
    {
        try {
            docmaApp.setApplicationProperty(name, value);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void setApplicationProperties(Map<String, String> props) throws DocmaException 
    {
        try {
            docmaApp.setApplicationProperties(props);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public Language[] getSupportedContentLanguages() throws DocmaException
    {
        try {
            return docmaApp.getSupportedContentLanguages();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }
    
    public CharEntity[] getCharEntities() throws DocmaException 
    {
        try {
            return docmaApp.getCharEntities();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void setCharEntities(CharEntity[] entities) throws DocmaException 
    {
        try {
            docmaApp.setCharEntities(PlugHelper.toDocmaCharEntities(entities));
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public CharEntity createCharEntity(String sym, String num, String desc) throws DocmaException 
    {
        return createCharEntity(sym, num, desc, true);
    }

    public CharEntity createCharEntity(String sym, String num, String desc, boolean sel) throws DocmaException 
    {
        return new DocmaCharEntity(sym, num, sel, desc);
    }

    public String[] getTextFileExtensions() throws DocmaException
    {
        return docmaApp.getTextFileExtensions();
    }

    public String[] getAutoFormatClassNames() throws DocmaException 
    {
        return docmaApp.getAutoFormatClassNames();
    }

    public void registerAutoFormatClasses(String... clsNames) throws DocmaException 
    {
        try {
            docmaApp.registerAutoFormatClasses(clsNames);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void unregisterAutoFormatClasses(String... clsNames) throws DocmaException 
    {
        try {
            docmaApp.unregisterAutoFormatClasses(clsNames);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String[] getRuleClassNames() throws DocmaException 
    {
        return docmaApp.getRuleClassNames();
    }

    public void registerRuleClasses(String... clsNames) throws DocmaException 
    {
        try {
            docmaApp.registerRuleClasses(clsNames);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void unregisterRuleClasses(String... clsNames) throws DocmaException 
    {
        try {
            docmaApp.unregisterRuleClasses(clsNames);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

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
    
    public Logger getLogger()
    {
        return logger;
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
