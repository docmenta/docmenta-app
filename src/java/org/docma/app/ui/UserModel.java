/*
 * UserModel.java
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

import java.util.*;
import org.docma.coreapi.*;
import org.docma.app.*;
import org.docma.userapi.*;
import org.docma.util.DocmaUtil;
import org.docma.webapp.GUIConstants;

/**
 *
 * @author MP
 */
public class UserModel implements Comparable
{
    private boolean loginNameChanged = false;

    private String   userId    = null;
    private String   loginName = "";
    private String   lastName  = "";
    private String   firstName = "";
    private String   email     = "";
    private Date     lastLogin = null;
    private String[] groupNames  = {};
    private String   guiLanguage = "";   // Empty string means default language (OS or browser language)
    private String   editorId    = null;
    private String   dateFormat  = "";
    private String   newPassword = null;
    private boolean  quickLinksEnabled = false;
    private boolean  isDirUser = false;

    private String   fullName = null;

    /* -----------  Public constructors  ------------------ */

    /**
     * Constructor for creating a new user instance.
     */
    public UserModel()
    {
    }

    /**
     * Constructor to initialize instance for an existing user.
     * @param userId The user identifier.
     * @param um     The UserManager instance (persistence layer).
     */
    public UserModel(String userId, UserManager um)
    {
        this.userId = userId;
        load(um);
    }

    /* -----------  Public methods  ------------------ */

    /**
     * Load or reload all user data from the persistence layer.
     * Before this method can be called, the user Id needs to be set.
     * 
     * @param um The UserManager instance (persistence layer).
     */
    public void load(UserManager um)
    {
        loginName = um.getUserNameFromId(userId);
        if (loginName == null) {
            throw new DocRuntimeException("User with ID '" + userId + "' does not exist.");
        }
        loginNameChanged = false;
        
        lastName = um.getUserProperty(userId, DocmaConstants.PROP_USER_LAST_NAME);
        firstName = um.getUserProperty(userId, DocmaConstants.PROP_USER_FIRST_NAME);
        email = um.getUserProperty(userId, DocmaConstants.PROP_USER_EMAIL);
        String login_str = um.getUserProperty(userId, DocmaConstants.PROP_USER_LAST_LOGIN);
        if ((login_str != null) && (login_str.trim().length() > 0)) {
            try {
                lastLogin = new Date(Long.parseLong(login_str));
            } catch (Exception ex) {}
        }
        guiLanguage = um.getUserProperty(userId, DocmaConstants.PROP_USER_GUI_LANGUAGE);
        dateFormat = um.getUserProperty(userId, DocmaConstants.PROP_USER_DATE_FORMAT);
        editorId = um.getUserProperty(userId, GUIConstants.PROP_USER_EDITOR_ID);
        String ql_str = um.getUserProperty(userId, GUIConstants.PROP_USER_QUICKLINKS_ENABLED);
        quickLinksEnabled = (ql_str != null) && ql_str.equalsIgnoreCase("true");
        refreshGroups(um);

        fullName = null;
        
        if (um instanceof DirectoryUserManager) {
            DirectoryUserManager dum = (DirectoryUserManager) um;
            isDirUser = dum.isDirectoryUser(userId);
        } else {
            isDirUser = false;
        }
    }

    public void refreshGroups(UserManager um)
    {
        String[] gids = um.getGroupsOfUser(userId);
        groupNames = new String[gids.length];
        for (int i=0; i < gids.length; i++) {
            groupNames[i] = um.getGroupNameFromId(gids[i]);
        }
    }

    public void update(UserManager um) throws DocException
    {
        if (loginNameChanged) {
            um.setUserName(userId, loginName);
            loginNameChanged = false;
        }
        String[] propNames = {
            DocmaConstants.PROP_USER_LAST_NAME,
            DocmaConstants.PROP_USER_FIRST_NAME,
            DocmaConstants.PROP_USER_EMAIL,
            DocmaConstants.PROP_USER_GUI_LANGUAGE,
            DocmaConstants.PROP_USER_DATE_FORMAT,
            GUIConstants.PROP_USER_EDITOR_ID,
            GUIConstants.PROP_USER_QUICKLINKS_ENABLED
        };
        String[] propValues = {
            lastName,
            firstName,
            email,
            guiLanguage,
            dateFormat,
            editorId,
            quickLinksEnabled ? "true" : "false"
        };
        um.setUserProperties(userId, propNames, propValues);

        if (newPassword != null) {
            um.setPassword(userId, newPassword);
        }
    }

    public void create(UserManager um) throws DocException
    {
        if (loginName.trim().equals("")) {
            throw new DocException("Cannot create user: Missing user name.");
        }
        userId = um.createUser(loginName);
        loginNameChanged = false;
        update(um);
    }

    public int compareTo(Object obj)
    {
        UserModel other = (UserModel) obj;
        return getLoginName().compareToIgnoreCase(other.getLoginName());
    }

    /* -----------  Public getter / setter methods  ------------------ */

    public String getDateFormat()
    {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat)
    {
        this.dateFormat = dateFormat;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getFirstName()
    {
        return firstName;
    }

    public void setFirstName(String firstName)
    {
        this.firstName = firstName;
        this.fullName = null;
    }

    public String[] getGroupNames()
    {
        return groupNames;
    }
    
    public String getGroupNamesAsString()
    {
        return DocmaUtil.concatStrings(Arrays.asList(getGroupNames()), ", ");
    }

    // public void setGroupNames(String[] groupNames)
    // {
    //     this.groupNames = groupNames;
    // }

    public String getGuiLanguage()
    {
        return (guiLanguage == null) ? "" : guiLanguage;
    }

    public void setGuiLanguage(String guiLanguage)
    {
        this.guiLanguage = (guiLanguage == null) ? "" : guiLanguage;
    }

    public Date getLastLogin()
    {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin)
    {
        this.lastLogin = lastLogin;
    }

    public String getLastName()
    {
        return lastName;
    }

    public void setLastName(String lastName)
    {
        this.lastName = lastName;
        this.fullName = null;
    }

    public String getLoginName()
    {
        return loginName;
    }

    public void setLoginName(String loginName)
    {
        if (! loginName.equals(this.loginName)) {
            this.loginName = loginName;
            loginNameChanged = true;
        }
    }

    public String getNewPassword()
    {
        return newPassword;
    }

    public void setNewPassword(String password)
    {
        this.newPassword = password;
    }

    public String getUserId()
    {
        return userId;
    }

    public String getEditorId()
    {
        return editorId;
    }

    public void setEditorId(String editorId)
    {
        this.editorId = editorId;
    }
    
    public boolean isQuickLinksEnabled()
    {
        return quickLinksEnabled;
    }
    
    public void setQuickLinksEnabled(boolean is_enabled)
    {
        quickLinksEnabled = is_enabled;
    }
    
    public boolean isDirectoryUser()
    {
        return isDirUser;
    }


    /* -----------  Public utility methods  ------------------ */

    public String getFullName()
    {
        if (fullName == null) {
            String ln = getLastName();
            String fn = getFirstName();
            if ((ln != null) && (ln.length() == 0)) ln = null;
            if ((fn != null) && (fn.length() == 0)) fn = null;
            if ((ln != null) && (fn != null)) {
                fullName = ln + ", " + fn;
            } else {
                if (ln != null) fullName = ln;
                else {
                    if (fn != null) fullName = fn;
                    else {
                        fullName = "";
                    }
                }
            }
        }
        return fullName;
    }

    public String getDisplayName()
    {
        String full_nm = getFullName();
        if (full_nm.length() > 0) {
            return full_nm;
        } else {
            return getLoginName();
        }
    }

}
