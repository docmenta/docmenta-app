/*
 * ActivityImpl.java
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
import org.docma.coreapi.implementation.DefaultProgressCallback;
import org.docma.util.Log;

import java.io.*;
import java.util.*;
import org.docma.coreapi.implementation.DefaultLog;

/**
 *
 * @author MP
 */
public class ActivityImpl extends DefaultProgressCallback implements Activity
{
    private static final String PROP_LANGUAGE = "language";
    private static final String PROP_COUNTRY = "country";
    private static final String PROP_TITLE_KEY = "title";
    private static final String PROP_TITLE_ARG = "title_arg";
    private static final String PROP_MESSAGE_KEY = "msg";
    private static final String PROP_MESSAGE_ARG = "msg_arg";
    private static final String PROP_STATE = "state";
    private static final String PROP_STORE_UUID = "store_uuid";
    
    private static final String STATE_NEW = "new";
    private static final String STATE_RUNNING = "running";
    private static final String STATE_FINISHED_OK = "finished";
    private static final String STATE_FINISHED_ERROR = "error";
    

    private long activityId;
    private File activityFile = null;     // activity properties file
    private File activityLogFile = null;  // activity log file
    private Properties props = new Properties();
    
    private String titleKey = null;
    private Object[] titleArgs = {};
    private Locale activityLocale = null;
    private Thread activityThread = null;

    private UUID storeUUID = null;
    

    ActivityImpl(long actId, File activityFile, File logFile, DocI18n i18n) 
    {
        super(i18n);
        this.activityId = actId;
        this.activityFile = activityFile;
        this.activityLogFile = logFile;
        this.activityLocale = i18n.getCurrentLocale();
        this.log.setLocale(activityLocale);
        if (activityFile.exists()) {
            restoreActivityAfterServerRestart();
        }
    }
    
    File getActivityFile()
    {
        return activityFile;
    }
    
    File getLogFile()
    {
        return activityLogFile;
    }
    
    UUID getStoreUUID()
    {
        return storeUUID;
    }
    
    void setStoreUUID(UUID storeUUID) 
    {
        this.storeUUID = storeUUID;
    }

    public long getActivityId()
    {
        return activityId;
    }
    
    public void setTitle(String labelKey, Object... args) 
    {
        this.titleKey = labelKey;
        this.titleArgs = args;
        persist();
    }

    public String getTitleKey() 
    {
        return (titleKey != null) ? titleKey : "text.default.activity.title";
    }

    public Object[] getTitleArgs() 
    {
        return (titleArgs != null) ? titleArgs.clone() : new Object[0];
    }

    public void setMessage(String labelKey, Object... args) 
    {
        super.setMessage(labelKey, args);
        persist();
    }

    public void setFinished() 
    {
        super.setFinished();
        persist();
        writeLog();
    }

    public void start(Runnable task) 
    {
        activityThread = new Thread(task);
        activityThread.start();
    }

    public boolean isRunning() 
    {
        return is_Running(true);
    }
    
    public boolean isFinished() 
    {
        return is_Finished(true);
    }
    
    protected boolean is_Running(boolean check_state) 
    {
        if (check_state) {
            checkThreadState(true);
        }
        return (activityThread != null) && !super.isFinished();
    }

    protected boolean is_Finished(boolean check_state) 
    {
        if (check_state) {
            checkThreadState(true);
        }
        return super.isFinished();
    }
    
    private void checkThreadState(boolean allow_persist)
    {
        if ((activityThread != null) && !activityThread.isAlive()) {
            if (! super.isFinished()) {
                super.logError("text.activity.unexpected_termination");
                super.setFinished();
                if (allow_persist) {
                    persist();  // Note: this does not lead to an infinite loop, 
                                // because super.setFinished() is called before!
                }
            }
        }
    }

    private void persist()
    {
        checkThreadState(false);
        
        props.clear();
        
        // Activity locale
        if (activityLocale != null) {
            props.setProperty(PROP_LANGUAGE, activityLocale.getLanguage());
            props.setProperty(PROP_COUNTRY, activityLocale.getCountry());
        }
        
        // Activity title
        props.setProperty(PROP_TITLE_KEY, getTitleKey());
        if (titleArgs != null) {
            for (int i = 0; i < titleArgs.length; i++) {
                props.setProperty(PROP_TITLE_ARG + "." + i, "" + titleArgs[i]);
            }
        }
        
        // Progress message
        String msg_key = getMessageKey();
        if (msg_key == null) {
            msg_key = "";
        }
        props.setProperty(PROP_MESSAGE_KEY, msg_key);
        Object[] msgArgs = getMessageArgs();
        if (msgArgs != null) {
            for (int i = 0; i < msgArgs.length; i++) {
                props.setProperty(PROP_MESSAGE_ARG + "." + i, "" + msgArgs[i]);
            }
        }
        
        // Activity state
        String state_str = STATE_NEW;
        if (is_Running(false)) {   // false to avoid recursive loop
            state_str = STATE_RUNNING;
        } else
        if (is_Finished(false)) {  // false to avoid recursive loop
            if (getErrorCount() > 0) {
                state_str = STATE_FINISHED_ERROR;
            } else {
                state_str = STATE_FINISHED_OK;
            }
        }
        props.setProperty(PROP_STATE, state_str);

        // Store UUID
        if (this.storeUUID != null) {
            props.setProperty(PROP_STORE_UUID, this.storeUUID.toString());
        }
        
        // Save updated properties
        savePropFile();
    }
    
    private void restoreActivityAfterServerRestart()
    {
        loadPropFile();  // load activity state from properties file
        
        // Restore activity locale
        String lang = props.getProperty(PROP_LANGUAGE, "");
        String country = props.getProperty(PROP_COUNTRY, "");
        if (lang.equals("") && country.equals("")) {
            activityLocale = null;
        } else {
            activityLocale = new Locale(lang, country);
        }
        
        this.log.setLocale(activityLocale);
        
        loadLog();  // restore persisted log
        
        // Restore activity title
        this.titleKey = props.getProperty(PROP_TITLE_KEY);
        List<String> args = new ArrayList<String>();
        String arg_value;
        while ((arg_value = props.getProperty(PROP_TITLE_ARG + "." + args.size())) != null) {
            args.add(arg_value);
        }
        this.titleArgs = args.toArray();
        
        // Restore progress message
        String msgKey = props.getProperty(PROP_MESSAGE_KEY);
        args.clear();
        while ((arg_value = props.getProperty(PROP_MESSAGE_ARG + "." + args.size())) != null) {
            args.add(arg_value);
        }
        super.setMessage(false, msgKey, args.toArray());
        
        // Restore activity state
        String persisted_state = props.getProperty(PROP_STATE, STATE_FINISHED_ERROR);
        
        if (persisted_state.equalsIgnoreCase(STATE_FINISHED_ERROR)) {
            if (getErrorCount() == 0) {
                // Set default error message
                super.logError("text.activity.finished_with_error");
            }
            super.setFinished();
        } else
        if (persisted_state.equalsIgnoreCase(STATE_FINISHED_OK)) {
            super.setFinished();
        } else
        if (persisted_state.equalsIgnoreCase(STATE_RUNNING) || 
            persisted_state.equalsIgnoreCase(STATE_NEW)) {
            if (activityThread == null) {
                super.logError("text.activity.unexpected_server_restart");
                super.setFinished();
                persist();
            }
        }
        
        // Restore store UUID
        String uuid = props.getProperty(PROP_STORE_UUID);
        if ((uuid != null) && (uuid.trim().length() > 0)) {
            this.storeUUID = UUID.fromString(uuid.trim());
        } else {
            this.storeUUID = null;
        }
    }

    private void writeLog()
    {
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(activityLogFile);
            this.log.storeToXML(fout);
        } catch (Exception ex) {
            Log.error("Could not write activitiy log '" + activityLogFile + "': " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            if (fout != null) try { fout.close(); } catch (Exception ex2) {}  
        }
    }
    
    private void loadLog()
    {
        if (activityLogFile.exists()) {
            InputStream fin = null;
            try {
                fin = new FileInputStream(activityLogFile);
                DefaultLog.loadFromXML(fin, this.log);
            } catch (Exception ex) {
                Log.error("Could not load activitiy log '" + activityLogFile + "': " + ex.getMessage());
                ex.printStackTrace();
            } finally {
                if (fin != null) try { fin.close(); } catch (Exception ex2) {}  
            }
        }
    }
    

    private void loadPropFile()
    {
        try {
            // ClassLoader cl = PropertiesLoader.class.getClassLoader();
            // InputStream fin = cl.getResourceAsStream(inifilename);
            InputStream fin = new FileInputStream(activityFile);
            props.load(fin);
            fin.close();
        } catch (Exception ex) {
            Log.error("Could not load activitiy file: " + activityFile);
            throw new DocRuntimeException(ex);
        }
    }


    private void savePropFile()
    {
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(activityFile);
            props.store(fout, "Activity Properties");
            fout.close();
        } catch (Exception ex) {
            Log.error("Could not save activity file: " + activityFile);
            if (fout != null) try { fout.close(); } catch (Exception ex2) {}
            throw new DocRuntimeException(ex);
        }
    }


}
