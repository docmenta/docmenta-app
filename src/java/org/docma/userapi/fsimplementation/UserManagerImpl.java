/*
 * UserManagerImpl.java
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

package org.docma.userapi.fsimplementation;

import java.io.*;
import java.util.*;
import java.security.*;
import java.math.BigInteger;
import org.docma.coreapi.*;
import org.docma.util.Log;
import org.docma.userapi.*;
import org.docma.app.DocmaConstants;

/**
 *
 * @author MP
 */
public class UserManagerImpl implements UserManager
{
    private static final String CONFIG_FILENAME = "userconfig.properties";
    private static final String USER_ID_PREFIX = "u";
    private static final String USER_ID_PATTERN = "000000";
    private static final String USER_FILE_EXTENSION = ".properties";
    private static final String SYS_ADMIN_USER_ID = USER_ID_PREFIX + USER_ID_PATTERN;
    private static final String GROUP_ID_PREFIX = "g";
    private static final String GROUP_ID_PATTERN = "000000";
    private static final String GROUP_FILE_EXTENSION = ".properties";

    // user manager configuration properties:
    private static final String PROP_CONFIG_USER_ID_RANGE = "userIdRange";
    // user properties:
    private static final String PROP_USERNAME = "userName";
    private static final String PROP_PASSWORD = "userPassword";
    private static final String PROP_PW_ENCODING = "userPwEncoding";
    private static final String PROP_GROUPS_OF_USER = "groupsOfUser";
    // group properties:
    private static final String PROP_GROUPNAME = "groupName";

    private File baseDir;
    private File usersDir;
    private File groupsDir;
    private File configFile;
    private Properties configProps = null;

    private Map userPropsMap = new HashMap(5000);
    private Map groupPropsMap = new HashMap(200);
    
    private Random randomIdGenerator = new Random();  // used to generate unique user ids

    private UserFilenameFilter userFilter;
    private GroupFilenameFilter groupFilter;

    private class UserFilenameFilter implements FilenameFilter 
    {
        public boolean accept(File dir, String name) {
            return name.startsWith(USER_ID_PREFIX) && name.endsWith(USER_FILE_EXTENSION);
        }
    }

    private class GroupFilenameFilter implements FilenameFilter
    {
        public boolean accept(File dir, String name) {
            return name.startsWith(GROUP_ID_PREFIX) && name.endsWith(GROUP_FILE_EXTENSION);
        }
    }

    public UserManagerImpl(String baseDirectory)
    {
        baseDir = new File(baseDirectory);
        if (! (baseDir.exists() && baseDir.isDirectory())) {
            throw new DocRuntimeException("User directory does not exist: " + baseDir.getAbsolutePath());
        }
        configFile = new File(baseDir, CONFIG_FILENAME);
        if (! configFile.exists()) {
            createConfigFile();
        }
        usersDir = new File(baseDir, "users");
        if (! usersDir.exists()) {
            if (! usersDir.mkdir()) {
                throw new DocRuntimeException("Cannot create users directory: " + usersDir.getAbsolutePath());
            }
        }
        groupsDir = new File(baseDir, "groups");
        if (! groupsDir.exists()) {
            if (! groupsDir.mkdir()) {
                throw new DocRuntimeException("Cannot create groups directory: " + groupsDir.getAbsolutePath());
            }
        }

        userFilter = new UserFilenameFilter();
        groupFilter = new GroupFilenameFilter();
    }

    /* --------------  Private helper methods  --------------- */

    private String createRandomUserRange()
    {
        // Create random two digit decimal number
        String s = String.valueOf(randomIdGenerator.nextInt(100));
        if (s.length() < 2) {
            s = "0" + s;
        }
        return s;
    }
    
    private synchronized void createConfigFile()
    {
        FileOutputStream fout = null;
        try {
            configProps = new Properties();
            configProps.setProperty(PROP_CONFIG_USER_ID_RANGE, createRandomUserRange());
            fout = new FileOutputStream(configFile);
            configProps.store(fout, "User Manager Configuration");
        } catch (Exception ex) {
            configProps = null;
            Log.error("Could not create configuration file " + configFile.getAbsolutePath());
            ex.printStackTrace();
        } finally {
            if (fout != null) {
                try { fout.close(); } catch (Exception ex2) {}
            }
        }
    }
    
    private synchronized String getUserIdRange()
    {
        if (configProps == null) {
            if (! configFile.exists()) {
                createConfigFile();
            }
            if (configProps == null) {
                configProps = loadPropertiesFile(configFile);
            }
        }
        String res = null;
        if (configProps != null) {
            res = configProps.getProperty(PROP_CONFIG_USER_ID_RANGE);
            if (res != null) {
                res = res.trim();
                if (res.equals("")) { 
                    res = null;
                } else {
                    try {
                        int val = Integer.parseInt(res);
                        if (val < 0) throw new Exception("Cannot be negativ");
                    } catch (Exception ex) {
                        Log.error("Invalid value for property '" + PROP_CONFIG_USER_ID_RANGE + "': " + res);
                        res = null;
                    }
                }
            }
        }
        return res;
    }
    
    private String getNextUserId()
    {
        return getNextUserId(null);
    }
    
    private String getNextUserId(String proposedId)
    {
        // Generates a user-id in the format uXXXXXX, where X is a decimal digit.
        // If 6 digits are not sufficient (i.e. no unused number can be found),
        // then up to 2 more digits are added. 
        // The first one or two digits can be configured for each user base 
        // directory (see method getUserIdRange()). This allows avoiding 
        // user-id conflicts between different installations.
        
        Set<String> existing_ids = new HashSet<String>(Arrays.asList(getUserIds()));
        if ((proposedId != null) && !existing_ids.contains(proposedId)) {
            return proposedId;
        }
        
        String configIdRange = getUserIdRange(); // allowed values: null, 1 or more digits
        int RAND_RANGE = 100000; // generate 5 digit random number by default
        String SEQ_PATTERN = "00000";
        // if configured prefix has >= 2 digits...
        if ((configIdRange != null) && (configIdRange.length() > 1)) {  
            RAND_RANGE = 10000;  //  ...then generate 4 digit random number
            SEQ_PATTERN = "0000";
        }
        int MID_RANGE = (RAND_RANGE / 2);
        int range = 0;  // only used if configIdRange is null or if 4/5 digits random sequence is not sufficient
        String next_id = null;
        boolean is_free = false;
        String userIdRange = configIdRange;
        do {
            String range_string = (userIdRange != null) ? userIdRange : String.valueOf(range);
            
            // Generate random id. If the random id is already used, then
            // do a sequential search for the next unused id.
            int seq = randomIdGenerator.nextInt(RAND_RANGE);  // max. 4 or 5 decimal digits
            int direction = (seq < MID_RANGE) ? 1 : -1;
            int loop_count = 0;
            do {
                loop_count++;
                String seq_str = String.valueOf(seq);  // e.g. "123"
                if (seq_str.length() < SEQ_PATTERN.length()) {  // pad with leading zeros to assure minimal length of 4 or 5
                    seq_str = SEQ_PATTERN.substring(seq_str.length()) + seq_str;      // e.g. 0123
                }
                next_id = USER_ID_PREFIX + range_string + seq_str;
                is_free = !existing_ids.contains(next_id);
                if (! is_free) {
                    seq += direction;  // sequential search for next unused user id
                }
            } while ((! is_free) && (loop_count < 1000) && (seq >= 0));
            
            // No free number found in the current range (range_string)
            if (! is_free) {
                if (range < 99) {  // up to 2 digits may be added
                    if (userIdRange != null) {  // no free number found in current userIdRange
                        // add extra digits to the configured user-id range
                        userIdRange = configIdRange + String.valueOf(range++);
                    } else {
                        range++;
                    }
                } else {
                   throw new DocRuntimeException("Could not generate unique user id: no free number found!");
                }
            }
        } while (! is_free);
        return next_id;
        
        // Old implementation:
        // ------------------
        // String[] arr = getUserIds();
        // int startpos = USER_ID_PREFIX.length();
        // int max_id = -1;
        // for (int i=0; i < arr.length; i++) {
        //     int uid = Integer.parseInt(arr[i].substring(startpos));
        //     if (uid > max_id) {
        //         max_id = uid;
        //     }
        // }
        // String next_id = String.valueOf(max_id + 1);   // e.g. 123
        // String uid = USER_ID_PATTERN + next_id;        // e.g. 000000123
        // uid = uid.substring(next_id.length());         // e.g. 000123
        // return USER_ID_PREFIX + uid;
    }

    private String getNextGroupId()
    {
        String[] arr = getGroupIds();
        int startpos = GROUP_ID_PREFIX.length();
        int max_id = -1;
        for (int i=0; i < arr.length; i++) {
            int gid = Integer.parseInt(arr[i].substring(startpos));
            if (gid > max_id) {
                max_id = gid;
            }
        }
        String next_id = String.valueOf(max_id + 1);   // e.g. 123
        String gid = GROUP_ID_PATTERN + next_id;        // e.g. 000000123
        gid = gid.substring(next_id.length());         // e.g. 000123
        return GROUP_ID_PREFIX + gid;
    }

    private Properties loadPropertiesFile(File propFile)
    {
        if (propFile.exists()) {
            try {
                Properties props = new Properties();
                props.load(new FileInputStream(propFile));
                return props;
            } catch(Exception ex) {
                throw new DocRuntimeException("Could not load properties: " + propFile.getAbsolutePath());
            }
        } else {
            return null;
        }
    }

    private Properties loadUserProperties(String userId)
    {
        File propFile = new File(usersDir, userId + USER_FILE_EXTENSION);
        return loadPropertiesFile(propFile);
    }

    private Properties loadGroupProperties(String groupId)
    {
        File propFile = new File(groupsDir, groupId + GROUP_FILE_EXTENSION);
        return loadPropertiesFile(propFile);
    }

    private Properties getUserProperties(String userId)
    {
        Properties props = (Properties) userPropsMap.get(userId);
        if (props == null) {
            props = loadUserProperties(userId);
            if (props != null) {  // may be null if user does not exist
                userPropsMap.put(userId, props);
            }
        }
        return props;
    }

    private Properties getGroupProperties(String groupId)
    {
        Properties props = (Properties) groupPropsMap.get(groupId);
        if (props == null) {
            props = loadGroupProperties(groupId);
            groupPropsMap.put(groupId, props);
        }
        return props;
    }

    private void saveUserProperties(String userId, Properties props)
    {
        File propFile = new File(usersDir, userId + USER_FILE_EXTENSION);
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(propFile);
            props.store(fout, "Properties for user " + userId);
        } catch (Exception ex) {
            throw new DocRuntimeException("Could not save user properties: " + propFile.getAbsolutePath());
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (Exception ex2) {}
            }
        }
    }

    private void saveGroupProperties(String groupId, Properties props)
    {
        File propFile = new File(groupsDir, groupId + GROUP_FILE_EXTENSION);
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(propFile);
            props.store(fout, "Properties for group " + groupId);
        } catch (Exception ex) {
            throw new DocRuntimeException("Could not save group properties: " + propFile.getAbsolutePath());
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (Exception ex2) {}
            }
        }
    }

    private void checkUserNameExists(String userId, String newUserName) throws DocException
    {
        String uid = getUserIdFromName(newUserName);
        if (uid != null) {  // a user with this name already exists
            if (! uid.equals(userId)) {  // the username belongs to another user
                throw new DocException("A user with name '" + newUserName + "' already exists.");
            }
        }
    }

    private void checkGroupNameExists(String groupId, String newGroupName) throws DocException
    {
        String gid = getGroupIdFromName(newGroupName);
        if (gid != null) {  // a group with this name already exists
            if (! gid.equals(groupId)) {  // the groupname belongs to another group
                throw new DocException("A group with name '" + newGroupName + "' already exists.");
            }
        }
    }

    private String getMD5Hash(String txt) throws Exception
    {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] enc = md.digest(txt.getBytes("UTF-8"));
        BigInteger bigint = new BigInteger(1, enc);
        return bigint.toString(16);
    }

    public boolean isDirectoryUser(String userId)
    {
        String val = getUserProperty(userId, DirectoryUserManager.PROPERTY_USER_DN);
        return (val != null) && (val.length() > 0);
    }

    /* --------------  Interface UserManager  --------------- */

    public synchronized String createUser(String userName) throws DocException
    {
        checkUserNameExists("", userName);
        String newUserId;
        if (userName.equals(DocmaConstants.SYS_ADMIN_LOGIN_NAME)) {
            newUserId = getNextUserId(SYS_ADMIN_USER_ID);  // use default user-id for admin (if possible)
        } else {
            newUserId = getNextUserId();  // generate random user-id
        }
        Properties props = new Properties();
        props.setProperty(PROP_USERNAME, userName);
        saveUserProperties(newUserId, props);
        userPropsMap.put(newUserId, props);
        return newUserId;
    }

    public synchronized void deleteUser(String userId) throws DocException
    {
        File del_file = new File(usersDir, userId + USER_FILE_EXTENSION);
        if (! del_file.delete()) {
            throw new DocRuntimeException("Cannot delete user file: " + del_file.getAbsolutePath());
        }
        userPropsMap.remove(userId);
    }

    public synchronized String[] getUserIds()
    {
        String[] fnames = usersDir.list(userFilter);
        int normal_endpos = USER_ID_PREFIX.length() + USER_ID_PATTERN.length();
        for (int i=0; i < fnames.length; i++) {
            String fn = fnames[i];
            int endpos;
            if (fn.charAt(normal_endpos) == '.') {
                endpos = normal_endpos;
            } else {
                endpos = fn.lastIndexOf('.');
            }
            fnames[i] = fn.substring(0, endpos);
        }
        return fnames;
    }

    public synchronized String getUserIdFromName(String userName)
    {
        String[] uids = getUserIds();
        for (int i=0; i < uids.length; i++) {
            String name = getUserNameFromId(uids[i]);
            if (userName.equals(name)) return uids[i];
        }
        return null;
    }

    public synchronized String getUserNameFromId(String userId)
    {
        return getUserProperty(userId, PROP_USERNAME);
    }

    public synchronized void setUserName(String userId, String newUserName) throws DocException
    {
        checkUserNameExists(userId, newUserName);
        setUserProperty(userId, PROP_USERNAME, newUserName);
    }

    public synchronized void setPassword(String userId, String newPassword) throws DocException
    {
        if (newPassword == null) {
            newPassword = "";
        }
        String save_pw;
        String pw_encoding;
        try {
            save_pw = getMD5Hash(newPassword);
            pw_encoding = "MD5";
        } catch (Exception ex) {
            // No encryption algorithm available; store password unencrypted
            save_pw = newPassword;
            pw_encoding = "";
        }
        setUserProperties(userId,
                          new String[] { PROP_PASSWORD, PROP_PW_ENCODING },
                          new String[] { save_pw, pw_encoding });
    }

    public boolean verifyUserNamePassword(String userName, String password) 
    {
        String uid = getUserIdFromName(userName);
        if (uid == null) {
            return false;
        }
        if (isDirectoryUser(uid)) {
            throw new DocRuntimeException("Cannot verify password of directory users!");
        }
        String pw_encoding = getUserProperty(uid, PROP_PW_ENCODING);
        String saved_pw = getUserProperty(uid, PROP_PASSWORD);
        if ((pw_encoding == null) || pw_encoding.trim().equals("")) {
            // Password was stored unencrypted
            return password.equals(saved_pw);
        } else if (pw_encoding.equalsIgnoreCase("MD5")) {
            try {
                return getMD5Hash(password).equals(saved_pw);
            } catch (Exception ex) {
                throw new DocRuntimeException(ex);
            }
        } else {
            throw new DocRuntimeException("Unknown password encryption method: " + pw_encoding);
        }
    }

    public synchronized String getUserProperty(String userId, String propName)
    {
        Properties props = getUserProperties(userId);
        if (props == null) return null;
        return props.getProperty(propName, "");
    }

    public synchronized void setUserProperty(String userId, String propName, String propValue) throws DocException
    {
        Properties props = getUserProperties(userId);
        if (props == null) {
            throw new DocException("Cannot set property: User with ID '" + userId + "' does not exist.");
        }
        if (propValue == null) {
            props.remove(propName);
        } else {
            props.setProperty(propName, propValue);
        }
        saveUserProperties(userId, props);
    }

    public synchronized void setUserProperties(String userId, String[] propNames, String[] propValues) throws DocException
    {
        Properties props = getUserProperties(userId);
        if (props == null) {
            throw new DocException("Cannot set property: User with ID '" + userId + "' does not exist.");
        }
        for (int i=0; i < propNames.length; i++) {
            props.setProperty(propNames[i], propValues[i]);
        }
        saveUserProperties(userId, props);
    }

    public synchronized String createGroup(String groupName) throws DocException
    {
        checkGroupNameExists("", groupName);
        String newGroupId = getNextGroupId();
        Properties props = new Properties();
        props.setProperty(PROP_GROUPNAME, groupName);
        saveGroupProperties(newGroupId, props);
        groupPropsMap.put(newGroupId, props);
        return newGroupId;
    }

    public synchronized void deleteGroup(String groupId) throws DocException
    {
        File del_file = new File(groupsDir, groupId + GROUP_FILE_EXTENSION);
        if (! del_file.delete()) {
            throw new DocRuntimeException("Cannot delete group file: " + del_file.getAbsolutePath());
        }
        groupPropsMap.remove(groupId);
    }

    public synchronized String[] getGroupIds()
    {
        String[] fnames = groupsDir.list(groupFilter);
        int endpos = GROUP_ID_PREFIX.length() + GROUP_ID_PATTERN.length();
        for (int i=0; i < fnames.length; i++) {
            fnames[i] = fnames[i].substring(0, endpos);
        }
        return fnames;
    }

    public synchronized String getGroupNameFromId(String groupId)
    {
        return getGroupProperty(groupId, PROP_GROUPNAME);
    }

    public synchronized String getGroupIdFromName(String groupName)
    {
        String[] gids = getGroupIds();
        for (int i=0; i < gids.length; i++) {
            String name = getGroupNameFromId(gids[i]);
            if (groupName.equals(name)) return gids[i];
        }
        return null;
    }

    public synchronized void setGroupName(String groupId, String newGroupName) throws DocException
    {
        checkGroupNameExists(groupId, newGroupName);
        setGroupProperty(groupId, PROP_GROUPNAME, newGroupName);
    }

    public synchronized String getGroupProperty(String groupId, String propName)
    {
        Properties props = getGroupProperties(groupId);
        if (props == null) return null;
        return props.getProperty(propName, "");
    }

    public synchronized void setGroupProperty(String groupId, String propName, String propValue) throws DocException
    {
        Properties props = getGroupProperties(groupId);
        props.setProperty(propName, propValue);
        saveGroupProperties(groupId, props);
    }

    public synchronized void setGroupProperties(String groupId, String[] propNames, String[] propValues) throws DocException
    {
        Properties props = getGroupProperties(groupId);
        for (int i=0; i < propNames.length; i++) {
            props.setProperty(propNames[i], propValues[i]);
        }
        saveGroupProperties(groupId, props);
    }

    public synchronized boolean isUserInGroup(String userId, String groupId) 
    {
        String[] gids = getGroupsOfUser(userId);
        List glist = Arrays.asList(gids);
        return glist.contains(groupId);
        // String[] uids = getUsersInGroup(groupId);
        // List ulist = Arrays.asList(uids);
        // return ulist.contains(userId);
    }

    public synchronized String[] getUsersInGroup(String groupId) 
    {
        String[] uids = getUserIds();
        List uList = new ArrayList(uids.length);
        for (int i=0; i < uids.length; i++) {
            String userId = uids[i];
            List groups = Arrays.asList(getGroupsOfUser(userId));
            if (groups.contains(groupId)) uList.add(userId);
        }
        uids = new String[uList.size()];
        return (String[]) uList.toArray(uids);
    }

    public String[] getGroupsOfUser(String userId)
    {
        String glist = getUserProperty(userId, PROP_GROUPS_OF_USER);
        if ((glist == null) || glist.trim().equals("")) {
            return new String[0];
        }
        String[] garr = glist.split(",");
        for (int i=0; i < garr.length; i++) {
            garr[i] = garr[i].trim();
        }
        return garr;
    }

    public void setGroupsOfUser(String userId, String[] groupIds) throws DocException
    {
        String grps_val;
        if ((groupIds != null) && (groupIds.length > 0)) {
            StringBuilder sbuf = new StringBuilder(groupIds[0]);
            for (int i=1; i < groupIds.length; i++) {
                sbuf.append(",");
                sbuf.append(groupIds[i]);
            }
            grps_val = sbuf.toString();
        } else {
            grps_val = "";
        }
        setUserProperty(userId, PROP_GROUPS_OF_USER, grps_val);
    }

    public synchronized boolean addUserToGroup(String userId, String groupId) throws DocException
    {
        int cnt = addUsersToGroup(new String[] {userId}, groupId);
        return (cnt > 0);
    }

    public synchronized int addUsersToGroup(String[] userIds, String groupId) throws DocException
    {
        int cnt = 0;
        for (int k=0; k < userIds.length; k++) {
            String userId = userIds[k];
            List groups = Arrays.asList(getGroupsOfUser(userId));
            if (! groups.contains(groupId)) {
                StringBuilder sbuf = new StringBuilder(groupId);
                for (int i=0; i < groups.size(); i++) {
                    sbuf.append(",");
                    sbuf.append((String) groups.get(i));
                }
                setUserProperty(userId, PROP_GROUPS_OF_USER, sbuf.toString());
                cnt++;
            }
        }
        return cnt;
    }

    public synchronized boolean removeUserFromGroup(String userId, String groupId) throws DocException
    {
        int cnt = removeUsersFromGroup(new String[] {userId}, groupId);
        return (cnt > 0);
    }

    public synchronized int removeUsersFromGroup(String[] userIds, String groupId) throws DocException
    {
        int cnt = 0;
        for (int k=0; k < userIds.length; k++) {
            String userId = userIds[k];
            List groups = Arrays.asList(getGroupsOfUser(userId));
            if (groups.contains(groupId)) {
                StringBuffer sbuf = new StringBuffer();
                for (int i=0; i < groups.size(); i++) {
                    String gid = (String) groups.get(i);
                    if (! gid.equals(groupId)) {
                        if (sbuf.length() > 0) sbuf.append(",");
                        sbuf.append(gid);
                    }
                }
                setUserProperty(userId, PROP_GROUPS_OF_USER, sbuf.toString());
                cnt++;
            }
        }
        return cnt;
    }

}
