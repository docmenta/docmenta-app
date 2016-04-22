/*
 * LockManagerImpl.java
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

package org.docma.lockapi.fsimplementation;

import java.util.*;
import java.io.*;
import org.docma.coreapi.*;
import org.docma.coreapi.implementation.*;
import org.docma.lockapi.*;
import org.docma.util.Log;

/**
 *
 * @author MP
 */
public class LockManagerImpl extends AbstractLockManager implements LockManager
{
    private File lockDir;
    private Map locks;

    public LockManagerImpl(String lockDir)
    {
        this.lockDir = new File(lockDir);
        loadLocks();
    }

    /* --------------  Interface LockManager  --------------- */

    public synchronized boolean setLock(String objId, String lockname, String user, long timeout)
    {
        Lock existinglock = getLock(objId, lockname);
        if (existinglock != null) {
            return false;
        } else {
            long currentTime = System.currentTimeMillis();
            Lock newlock = new LockImpl(objId, lockname, user, currentTime, timeout);
            locks.put(getLockKey(newlock), newlock);
            writeLockFile(newlock);
            lockAddedEvent(newlock);
            return true;
        }
    }

    public synchronized boolean refreshLock(String objId, String lockname, long timeout)
    {
        LockImpl existinglock = (LockImpl) getLock(objId, lockname);
        if (existinglock != null) {
            long currentTime = System.currentTimeMillis();
            long newtimeout = timeout + (currentTime - existinglock.getCreationTime());
            existinglock.setTimeout(newtimeout);
            writeLockFile(existinglock);
            return true;
        } else {
            return false;
        }
    }

    // public Lock[] getLocks(String objId) {
    //     return null;
    // }

    public synchronized Lock getLock(String objId, String lockname)
    {
        String key = getLockKey(objId, lockname);
        Lock lock = (Lock) locks.get(key);
        if (lock == null) {
            return null;
        } else {
            long currentTime = System.currentTimeMillis();
            if (checkLockTimeout(currentTime, lock)) {
                removeLockByKey(key);
                lockTimeoutEvent(lock);
                return null;
            } else {
                return lock;
            }
        }
    }

    // public Lock[] removeLocks(String objId) {
    //     return null;
    // }

    public synchronized Lock removeLock(String objId, String lockname)
    {
        Lock lock = removeLockByKey(getLockKey(objId, lockname));
        if (lock != null) lockRemovedEvent(lock);
        return lock;
    }


    /* --------------  Private helper methods  --------------- */


    private void loadLocks()
    {
        locks = new HashMap(100);
        File[] lockfiles = lockDir.listFiles();
        for (int i = 0; i < lockfiles.length; i++) {
            Lock lock = null;
            try {
                BufferedReader in = new BufferedReader(new FileReader(lockfiles[i]));
                String objId = in.readLine();
                String name = in.readLine();
                String user = in.readLine();
                long creationTime = Long.parseLong(in.readLine());
                long timeout = Long.parseLong(in.readLine());
                in.close();
                lock = new LockImpl(objId, name, user, creationTime, timeout);
            } catch (Exception ex) {
                // ignore
                Log.warning("Unable to read lock file: " + lockfiles[i]);
                lockfiles[i].delete();
            }
            if (lock != null) {
                long currentTime = System.currentTimeMillis();
                if (checkLockTimeout(currentTime, lock)) {
                    lockfiles[i].delete();
                } else {
                    locks.put(getLockKey(lock), lock);
                }
            }
        }
    }


    private void writeLockFile(Lock lock)
    {
        File f = getLockFile(getLockKey(lock));
        try {
            PrintWriter out = new PrintWriter(new FileWriter(f));
            out.println(lock.getLockedObjectId());
            out.println(lock.getName());
            out.println(lock.getUser());
            out.println("" + lock.getCreationTime());
            out.println("" + lock.getTimeout());
            out.close();
        } catch (Exception ex) {
            throw new DocRuntimeException("Unable to write lock file: " + f +
                                          ". Cause: " + ex.getMessage());
        }
    }


    private String getLockKey(Lock lock)
    {
        return getLockKey(lock.getLockedObjectId(), lock.getName());
    }

    private String getLockKey(String objId, String name)
    {
        return objId + " " + name;
    }

    private File getLockFile(String key)
    {
        return new File(lockDir, key + ".lock");
    }

    private Lock removeLockByKey(String key)
    {
        Lock lock = (Lock) locks.remove(key);
        File lockfile = getLockFile(key);
        if (lockfile.exists()) {
            if (! lockfile.delete()) {
                throw new DocRuntimeException("Unable to delete lock file: " + lockfile);
            }
        }
        return lock;
    }

}
