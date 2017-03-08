/*
 * Activities.java
 * 
 *  Copyright (C) 2014  Manfred Paula, http://www.docmenta.org
 *   
 *  This file is part of Docmenta. Docmenta is free software: you can 
 *  redistribute it and/or modify it under the terms of the GNU Lesser 
 *  General Public License as published by the Free Software Foundation, 
 *  either version 3 of the License, or (at your option) any later version.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Docmenta.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.docma.app;

import org.docma.coreapi.*;
import org.docma.util.Log;
import java.io.*;
import java.util.*;

/**
 *
 * @author MP
 */
class Activities 
{
    // Filename constants have to be in lower case. See method readPersistedActivities().
    private static final String ACT_PREFIX = "act_";
    private static final String EXT_PROP = ".properties";
    private static final String EXT_LOG = ".log";
    
    private File activitiesDir;
    private DocI18n i18n;
    private long last_id = 0;
    private Map<UUID, Activity> storeActivities = new HashMap<UUID, Activity>();
    private List<ActivityImpl> versionActivities = new ArrayList<ActivityImpl>();
    
    Activities(File activitiesDir, DocI18n i18n)
    {
        this.activitiesDir = activitiesDir;
        this.i18n = i18n;
        if (! activitiesDir.exists()) {
            if (! activitiesDir.mkdirs()) {
                Log.error("Could not create activities directory: " + activitiesDir);
            }
        }
        if (! activitiesDir.isDirectory()) {
            throw new DocRuntimeException("Invalid activities directory: " + activitiesDir);
        }
        readPersistedActivities();
    }
    
    Activity getActivityById(long activityId)
    {
        Activity res = getVersionActivity(activityId);
        if (res == null) {
            for (Activity act : storeActivities.values()) {
                if (act.getActivityId() == activityId) {
                    return act;
                }
            }
        }
        return res;
    }
    
    Activity getStoreActivity(UUID storeUUID)
    {
        return storeActivities.get(storeUUID);
    }
    
    synchronized Activity createStoreActivity(UUID storeUUID, String userId) throws DocException 
    {
        Activity act = getStoreActivity(storeUUID);
        if (act != null) {
            throw new DocException("Cannot create activity. Activity for store with UUID '" + 
                                   storeUUID + "' already exists!");
        }
        long act_id = createNewActivityId();
        File act_file = propFile(act_id);
        File log_file = logFile(act_id);
        ActivityImpl act_new = new ActivityImpl(act_id, act_file, log_file, i18n);
        act_new.setStoreUUID(storeUUID);
        act_new.setUserId(userId);
        storeActivities.put(storeUUID, act_new);
        return act_new;
    }
    
    synchronized boolean removeStoreActivity(UUID storeUUID) 
    {
        Activity act = getStoreActivity(storeUUID);
        if (act == null) {
            return false;
        }
        if (act.isRunning()) {
            return false;  // cannot remove running activity
        }
        act = storeActivities.remove(storeUUID);
        deleteActivityFiles(act);
        return true;  // activity no longer contained in map
    }
    
    Activity getVersionActivity(long actId)
    {
        for (ActivityImpl act : versionActivities) {
            if (actId == act.getActivityId()) {
                return act;
            }
        }
        return null;
    }
    
    Activity[] getVersionActivities(UUID versionUUID, String userId)
    {
        List<Activity> res = new ArrayList<Activity>();
        for (ActivityImpl act : versionActivities) {
            if (versionUUID.equals(act.getVersionUUID()) && 
                ((userId == null) || userId.equals(act.getUserId()))) {
                res.add(act);
            }
        }
        return res.toArray(new Activity[res.size()]);
    }
    
    synchronized Activity createVersionActivity(UUID versionUUID, String userId) throws DocException 
    {
        long act_id = createNewActivityId();
        File act_file = propFile(act_id);
        File log_file = logFile(act_id);
        ActivityImpl act_new = new ActivityImpl(act_id, act_file, log_file, i18n);
        act_new.setVersionUUID(versionUUID);
        act_new.setUserId(userId);
        versionActivities.add(act_new);
        return act_new;
    }

    synchronized boolean removeVersionActivity(long actId) 
    {
        Iterator<ActivityImpl> it = versionActivities.iterator();
        ActivityImpl removedAct = null;
        while (it.hasNext()) {
            ActivityImpl act = it.next();
            if (actId == act.getActivityId()) {
                if (act.isRunning()) {
                    return false;  // cannot remove running activity
                }
                it.remove();
                removedAct = act;
                break;
            }
        }
        if (removedAct == null) {   // no activity with given id found
            return false;
        }
        deleteActivityFiles(removedAct);
        return true;  // activity no longer contained in map
    }
    
    private long createNewActivityId()
    {
        long act_id = System.currentTimeMillis();
        if (act_id == last_id) {
            act_id++;
        }
        last_id = act_id;
        return act_id;
    }
    
    private void deleteActivityFiles(Activity act)
    {
        if ((act != null) && (act instanceof ActivityImpl)) {
            ActivityImpl removed_act = (ActivityImpl) act;
            File act_file = removed_act.getActivityFile();
            File log_file = removed_act.getLogFile();
            if ((act_file != null) && act_file.exists()) {
                if (! act_file.delete()) { 
                    Log.warning("Could not delete activity properties file: " + act_file);
                }
            }
            if ((log_file != null) && log_file.exists()) {
                if (! log_file.delete()) { 
                    Log.warning("Could not delete activity log file: " + log_file);
                }
            }
        }
    }
    
    private File propFile(long id)
    {
        return new File(activitiesDir, ACT_PREFIX + id + EXT_PROP);
    }
    
    private File logFile(long id)
    {
        return new File(activitiesDir, ACT_PREFIX + id + EXT_LOG);
    }
    
    private void readPersistedActivities()
    {
        String[] names = activitiesDir.list();
        for (String fn : names) {
            if (fn.startsWith(ACT_PREFIX) && fn.endsWith(EXT_PROP)) {
                try {
                    String act_name = fn.substring(0, fn.length() - EXT_PROP.length());
                    String id_str = act_name.substring(ACT_PREFIX.length());
                    long act_id = Long.parseLong(id_str);
                    File prop_file = new File(activitiesDir, fn);
                    File log_file = new File(activitiesDir, act_name + EXT_LOG);
                    ActivityImpl act = new ActivityImpl(act_id, prop_file, log_file, i18n);
                    UUID store_id = act.getStoreUUID();
                    UUID ver_id = act.getVersionUUID();
                    if (ver_id != null) {
                        versionActivities.add(act);
                    } else if (store_id != null) {
                        storeActivities.put(store_id, act);
                    } else {
                        Log.warning("Missing store UUID in activity file: " + fn);
                    }
                } catch (Exception ex) {
                    Log.error("Could not read activity file '" + fn + "': " + ex.getMessage());
                }
            }
        }
    }
    
}
