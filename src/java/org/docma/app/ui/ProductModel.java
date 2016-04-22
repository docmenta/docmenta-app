/*
 * ProductModel.java
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

import org.docma.coreapi.LogMessage;
import org.docma.coreapi.DocI18n;
import org.docma.app.*;
import java.util.*;
import org.docma.util.Log;

/**
 *
 * @author MP
 */
public class ProductModel implements Comparable, Cloneable
{
    public static final String PATH_DEFAULT = "DefaultPath";
    public static final String PATH_CUSTOM = "CustomPath";
    
    public static final String MODEL_STATE_LOAD_PENDING = "load_pending";
    public static final String MODEL_STATE_LOADING = "loading";
    public static final String MODEL_STATE_LOADED = "loaded";
    public static final String MODEL_STATE_LOAD_ERROR = "load_error";

    private String modelState = MODEL_STATE_LOADED;  // default state for transient product model
    private String loadErrorMsg = "";
    
    private String id = "";
    private String name = "";
    private String storetype = DocmaConstants.STORE_TYPE_FS;
    private String archivetype = DocmaConstants.ARCHIVE_TYPE_FS;
    private String path = "";
    private String pathtype = PATH_DEFAULT;
    private DocmaLanguage origLanguage = null;
    private DocmaLanguage[] translationLanguages = {};
    private boolean disabled = false;
    
    // External DB connection fields
    private String dbUrl = null;
    private String dbDialect = null;
    private String dbDriver = null;
    private String dbUser = null;
    private String dbPasswd = null;
    
    // Activity fields
    private long     activityId = -1;
    private String   activityTitleKey = null;
    private Object[] activityTitleArgs = null;
    private boolean  activityFinished = true;
    private boolean  activityError = false;
    private int      activityErrorCount = 0;
    private boolean  activityCanceledByUser = false;
    private int      activityPercent = 0;
    private String   activityMsgKey = null;
    private Object[] activityMsgArgs = null;
    private String   activityErrorMsg = null;


    public ProductModel()
    {
    }

    public ProductModel(DocmaSession docmaSess, String productId)
    {
        this(docmaSess, productId, false);
    }
    
    public ProductModel(DocmaSession docmaSess, String productId, boolean connect_db)
    {
        id = productId;
        updateModel(docmaSess, connect_db);
    }

    public Object clone()
    {
        try {
            return super.clone();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    private void updateModel(DocmaSession docmaSess, boolean connect_db)
    {
        if (id != null) {
            modelState = MODEL_STATE_LOAD_PENDING;
            name = "";   // clear field; field is loaded below
            loadErrorMsg = "";
            try {
                storetype = docmaSess.getDocStoreProperty(id, DocmaConstants.PROP_STORE_TYPE);
                path = docmaSess.getDocStoreProperty(id, DocmaConstants.PROP_STORE_PATH);
                if (path == null) path = "";
                if (path.equals("")) {
                    pathtype = PATH_DEFAULT;
                } else {
                    pathtype = PATH_CUSTOM;
                }
                disabled = docmaSess.isStoreDisabled(id);

                boolean is_external_db = DocmaConstants.STORE_TYPE_DB_EXTERNAL.equalsIgnoreCase(storetype);
                boolean connect_external = is_external_db && connect_db && !disabled;
                if (is_external_db) {
                    dbUrl = docmaSess.getDocStoreProperty(id, DocmaConstants.PROP_STORE_DB_CONNECTION_URL);
                    dbDialect = docmaSess.getDocStoreProperty(id, DocmaConstants.PROP_STORE_DB_DIALECT);
                    dbDriver = docmaSess.getDocStoreProperty(id, DocmaConstants.PROP_STORE_DB_DRIVER_CLASS);
                    dbUser = docmaSess.getDocStoreProperty(id, DocmaConstants.PROP_STORE_DB_CONNECTION_USER);
                    dbPasswd = docmaSess.getDocStoreProperty(id, DocmaConstants.PROP_STORE_DB_CONNECTION_PWD);
                }
                
                if (DocmaConstants.STORE_TYPE_FS.equalsIgnoreCase(storetype)) {
                    archivetype = DocmaConstants.ARCHIVE_TYPE_FS;
                } else {
                    if (connect_external || !is_external_db) {
                        updateArchiveType(docmaSess);
                    }
                }
                
                if (connect_external || !is_external_db) {
                    updateDisplayName(docmaSess);
                    updateProductLanguages(docmaSess);
                    try {
                        updateActivity(docmaSess);
                    } catch (Exception ex) {
                        Log.error("Could not get activity data for " + id + ": " + ex.getMessage());
                    }
                }
                modelState = (connect_external || !is_external_db) ? MODEL_STATE_LOADED : 
                                                                     MODEL_STATE_LOAD_PENDING;
            } catch (Exception ex) {
                modelState = MODEL_STATE_LOAD_ERROR;
                loadErrorMsg = "Error: " + ex.getMessage();
                if (DocmaConstants.DEBUG) {
                    ex.printStackTrace();
                }
            }
        }
    }
    
    private void updateArchiveType(DocmaSession docmaSess)
    {
        archivetype = docmaSess.getDocStoreProperty(id, DocmaConstants.PROP_STORE_ARCHIVE_TYPE);
        if ((archivetype == null) || archivetype.equals("")) {  // should never be true
            archivetype = DocmaConstants.ARCHIVE_TYPE_FS;  // fall back to default
        }
    }
    
    private void updateDisplayName(DocmaSession docmaSess)
    {
        name = docmaSess.getDocStoreProperty(id, DocmaConstants.PROP_STORE_DISPLAYNAME);
        if (name == null) name = "";
    }
    
    private void updateProductLanguages(DocmaSession docmaSess)
    {
        origLanguage = docmaSess.getOriginalLanguage(id);
        translationLanguages = docmaSess.getTranslationLanguages(id);
    }
    
    private void updateActivity(DocmaSession docmaSess)
    {
        if (id != null) {
            Activity act = null; 
            try {
                act = docmaSess.getDocStoreActivity(id);
            } catch (Exception ex) {
                Log.error("Could not get store activity for '" + id + "': " + ex.getMessage());
            }
            if (act != null) {
                activityId = act.getActivityId();
                activityTitleKey = act.getTitleKey();
                activityTitleArgs = act.getTitleArgs();
                activityMsgKey = act.getMessageKey();
                activityMsgArgs = act.getMessageArgs();
                activityFinished = act.isFinished();
                activityErrorCount = act.getErrorCount();
                activityError = (activityErrorCount > 0);
                if (activityError) {
                    LogMessage[] msgarr = act.getLog(false, false, true);  // get only error messages
                    if ((msgarr != null) && (msgarr.length > 0)) {
                        activityErrorMsg = msgarr[0].getMessage();
                    } else {  // Should never occur
                        activityErrorMsg = "Undefined error!";
                    }
                } else {
                    activityErrorMsg = null;
                }
                activityCanceledByUser = act.getCancelFlag();
                activityPercent = act.getPercent();
            } else {
                activityId = -1;
                activityTitleKey = null;
                activityTitleArgs = null;
                activityMsgKey = null;
                activityMsgArgs = null;
                activityFinished = true;
                activityError = false;
                activityErrorCount = 0;
                activityErrorMsg = null;
                activityCanceledByUser = false;
                activityPercent = 0;
            }
        }
    }
    
    public String getModelState()
    {
        return this.modelState;
    }
    
    public boolean isLoadError()
    {
        return MODEL_STATE_LOAD_ERROR.equals(this.modelState);
    }
    
    public boolean isLoadPending()
    {
        return MODEL_STATE_LOAD_PENDING.equals(this.modelState);
    }
    
    public boolean isLoaded()
    {
        return MODEL_STATE_LOADED.equals(this.modelState);
    }
    
    public boolean isLoading()
    {
        return MODEL_STATE_LOADING.equals(this.modelState);
    }
    
    public String getLoadErrorMessage()
    {
        return loadErrorMsg;
    }
    
    public void loadFromExternalDb(DocmaSession docmaSess)
    {
        try {
            modelState = MODEL_STATE_LOADING;
            
            updateDisplayName(docmaSess);
            updateArchiveType(docmaSess);
            updateProductLanguages(docmaSess);
            updateActivity(docmaSess);
            
            modelState = MODEL_STATE_LOADED;
        } catch (Throwable ex) {
            modelState = MODEL_STATE_LOAD_ERROR;
            loadErrorMsg = "Error: " + ex.getMessage();
            if (DocmaConstants.DEBUG) {
                ex.printStackTrace();
            }
        }
    }
    
    public void refresh(DocmaSession docmaSess)
    {
        updateModel(docmaSess, false);
    }

    public void refreshActivity(DocmaSession docmaSess)
    {
        updateActivity(docmaSess);
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getStoreType() 
    {
        return storetype;
    }
    
    public boolean isStoreTypeFs()
    {
        return DocmaConstants.STORE_TYPE_FS.equalsIgnoreCase(this.storetype);
    }
    
    public boolean isStoreTypeDb()
    {
        return isStoreTypeDbEmbedded() || isStoreTypeDbExternal();
    }
    
    public boolean isStoreTypeDbEmbedded()
    {
        return DocmaConstants.STORE_TYPE_DB_EMBEDDED.equalsIgnoreCase(this.storetype);
    }
    
    public boolean isStoreTypeDbExternal()
    {
        return DocmaConstants.STORE_TYPE_DB_EXTERNAL.equalsIgnoreCase(this.storetype);
    }
    
    public String getArchiveType() 
    {
        return archivetype;
    }
    
    public boolean isDbArchive()
    {
        return (archivetype != null) && archivetype.equalsIgnoreCase(DocmaConstants.ARCHIVE_TYPE_DB);
    }

    public String getPath()
    {
        return path;
    }

    public String getPathtype()
    {
        return pathtype;
    }
    
    public boolean isPathtypeCustom()
    {
        return (pathtype != null) && pathtype.equals(PATH_CUSTOM);
    }

    public void setId(String id) 
    {
        this.id = id;
    }

    public void setName(String name) 
    {
        this.name = name;
    }

    public void setStoreType(String storetype) 
    {
        if (storetype == null) {
            throw new RuntimeException("Store type cannot be null!");
        }
        this.storetype = storetype;
    }
    
    // public void setArchiveType(String archivetype) 
    // {
    //     this.archivetype = archivetype;
    // }
    
    public void setDbArchive(boolean is_db_archive)
    {
        this.archivetype = is_db_archive ? DocmaConstants.ARCHIVE_TYPE_DB : DocmaConstants.ARCHIVE_TYPE_FS;
    }

    public void setPath(String path) 
    {
        this.path = path;
    }

    public void setPathtype(String pathtype) 
    {
        this.pathtype = pathtype;
    }

//    public DocmaLanguage[] getLanguages()
//    {
//        if (origLanguage == null) {
//            return translationLanguages;
//        }
//        DocmaLanguage[] arr = new DocmaLanguage[translationLanguages.length + 1];
//        arr[0] = origLanguage;
//        for (int i=0; i < translationLanguages.length; i++) {
//            arr[i+1] = translationLanguages[i];
//        }
//        return arr;
//    }

    public void setLanguages(DocmaLanguageModel[] languages)
    {
        List list = new ArrayList(languages.length);
        for (int i=0; i < languages.length; i++) {
            DocmaLanguageModel lang = languages[i];
            if (lang.isTranslation()) {
                list.add(lang);
            } else {
                origLanguage = lang;
            }
        }
        translationLanguages = (DocmaLanguage[]) list.toArray(translationLanguages);
    }

    public DocmaLanguage getOriginalLanguage()
    {
        return origLanguage;
    }

    public DocmaLanguage[] getTranslationLanguages()
    {
        return translationLanguages;
    }

    public String getTranslationLanguageCodesAsString()
    {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < translationLanguages.length; i++) {
            if (buf.length() > 0) buf.append(',');
            buf.append(translationLanguages[i].getCode());
        }
        return buf.toString();
    }
    
    public boolean isDisabled()
    {
        return disabled;
    }
    
    public void setDisabled(boolean is_disabled) 
    {
        disabled = is_disabled;
    }

    public String getDbUrl() 
    {
        return dbUrl;
    }

    public void setDbUrl(String db_url) 
    {
        this.dbUrl = db_url;
    }

    public String getDbDialect() 
    {
        return dbDialect;
    }

    public void setDbDialect(String db_dialect) 
    {
        this.dbDialect = db_dialect;
    }

    public String getDbDriver() 
    {
        return dbDriver;
    }

    public void setDbDriver(String db_driver) 
    {
        this.dbDriver = db_driver;
    }

    public String getDbUser() 
    {
        return dbUser;
    }

    public void setDbUser(String db_user) 
    {
        this.dbUser = db_user;
    }

    public String getDbPasswd() 
    {
        return dbPasswd;
    }

    public void setDbPasswd(String db_passwd) 
    {
        this.dbPasswd = db_passwd;
    }

    public boolean hasActivity()
    {
        return (activityTitleKey != null);
    }
    
    public long getActivityId()
    {
        return activityId;
    }
    
    public String getActivityTitle(DocI18n i18n)
    {
        if (activityTitleKey != null) {
            return i18n.getLabel(activityTitleKey, activityTitleArgs);
        } else {
            return "";
        }
    }
    
    public String getActivityMsg(DocI18n i18n)
    {
        String msg = "";
        if (activityFinished) {
            msg = i18n.getLabel(activityError ? "text.activity.finished_with_error" : 
                                                "text.activity.finished_success");
        } else {
            if (activityMsgKey != null) {
                msg = i18n.getLabel(activityMsgKey, activityMsgArgs);
            }
        }
        if (msg == null) {
            msg = "";
        }
        if (activityError) {
            msg += " (" + i18n.getLabel("error.count", new Object[] { activityErrorCount } ) + ")";
        }
        return msg;
    }
    
    public boolean isActivityFinished() 
    {
        return activityFinished;
    }
    
    public boolean isActivityError() 
    {
        return activityError;
    }
    
    public int getActivityErrorCount() 
    {
        return activityErrorCount;
    }
    
    public boolean isActivityCanceledByUser() 
    {
        return activityCanceledByUser;
    }
    
    public int getActivityPercent()
    {
        return activityPercent;
    }
    
    public int compareTo(Object obj)
    {
        return getId().compareTo(((ProductModel) obj).getId());
    }

}
