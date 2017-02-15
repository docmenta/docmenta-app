/*
 * DocmaApplication.java
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

package org.docma.app;

import org.docma.coreapi.*;
import org.docma.userapi.UserManager;
import org.docma.plugin.ApplicationContext;
import org.docma.util.DocmaUtil;
import org.docma.util.Log;

import java.util.*;
import java.io.*;
import org.docma.plugin.implementation.ApplicationContextImpl;

/**
 *
 * @author MP
 */
public class DocmaApplication
{
    private final DocStoreManager dsm;
    private UserManager userMgmt;
    private final DocmaI18 i18;
    private final ApplicationProperties appProps;
    private final ContentLanguages contentLangs;
    private DocmaCharEntity[] charEntities = null;
    private FormattingEngine formatter = null;
    private final PublicationArchivesFactory pubArchivesFactory;
    private RevisionStoreFactory revStoreFactory = null;
    private final ApplicationServices appServices = new ApplicationServicesImpl();
    private final Activities activities;
    private final RulesManager rulesManager;
    private final List<DocmaSession> openSessions = new ArrayList<DocmaSession>();
    private Set<String> disabledStores = null;

    protected final ApplicationContext applicationCtx;


    public DocmaApplication(DocStoreManager dsm,
                            UserManager um,
                            DocmaI18 i18,
                            ContentLanguages contentLangs,
                            ApplicationProperties appProps,
                            PublicationArchivesFactory pubArchivesFactory)
    {
        this.dsm = dsm;
        this.userMgmt = um;
        this.i18 = i18;
        this.contentLangs = contentLangs;
        this.appProps = appProps;
        this.pubArchivesFactory = pubArchivesFactory;
        
        File activitiesDir = new File(getTempDirectory(), "activities");
        this.activities = new Activities(activitiesDir, i18);
        File rulesDir = new File(getBaseDirectory(), "rules");
        this.rulesManager = new RulesManager(this, rulesDir);
        
        this.applicationCtx = new ApplicationContextImpl(this);
    }

    /* --------------  Public methods  ---------------------- */

    public DocmaSession connect(String userName, String password) throws Exception
    {
        if (userMgmt.verifyUserNamePassword(userName, password)) {
            String userId = userMgmt.getUserIdFromName(userName);
            userMgmt.setUserProperty(userId, DocmaConstants.PROP_USER_LAST_LOGIN,
                                             "" + System.currentTimeMillis());
            return connect(userId);
        } else {
            throw new DocException("Invalid username or password.");
        }
    }

    public DocmaSession[] getOpenSessions()
    {
        DocmaSession[] arr = new DocmaSession[openSessions.size()];
        return (DocmaSession[]) openSessions.toArray(arr);
    }

    public DocmaSession[] getOpenSessions(String storeId, DocVersionId verId)
    {
        List sesslist = new ArrayList();
        for (int i=0; i < openSessions.size(); i++) {
            DocmaSession sess = openSessions.get(i);
            String sess_storeId = sess.getStoreId();
            DocVersionId sess_verId = sess.getVersionId();
            if (sess_storeId == null) continue;  // this session is not connected to a store
            if (sess_storeId.equals(storeId) && sess_verId.equals(verId)) {
                sesslist.add(sess);
            }
        }
        DocmaSession[] arr = new DocmaSession[sesslist.size()];
        return (DocmaSession[]) sesslist.toArray(arr);
    }

    public DocmaSession getOpenSession(String sessionId)
    {
        for (int i=0; i < openSessions.size(); i++) {
            DocmaSession sess = openSessions.get(i);
            if (sess.getSessionId().equals(sessionId)) return sess;
        }
        return null;
    }

    public String[] getConnectedUsers(String storeId, DocVersionId verId)
    {
        return dsm.getConnectedUsers(storeId, verId);
    }


    /**
     * Deprecated. Should be replaced by getI18n().
     * @return 
     */
    public DocmaI18 i18()
    {
        return i18;
    }
    
    public DocI18n getI18n()
    {
        return i18;
    }
    
    public final File getBaseDirectory()
    {
        String basePath = appProps.getProperty(DocmaConstants.PROP_BASE_PATH);
        return new File(basePath);
    }

    public final File getTempDirectory()
    {
        String tempPath = appProps.getProperty(DocmaConstants.PROP_TEMP_PATH);
        return new File(tempPath);
    }
    
    public ApplicationContext getApplicationContext()
    {
        return applicationCtx;
    }

    public ApplicationServices getApplicationServices()
    {
        return appServices;
    }

    public RevisionStoreFactory getRevisionStoreFactory()
    {
        return revStoreFactory;
    }

    public void setRevisionStoreFactory(RevisionStoreFactory revStoreFactory)
    {
        this.revStoreFactory = revStoreFactory;
    }

    public String[] getTextFileExtensions()
    {
        String ext_str = getApplicationProperties().getProperty(DocmaConstants.PROP_TEXT_FILE_EXTENSIONS);
        if (ext_str == null) {
            // return default
            return new String[] {"txt", "css", "js", "properties", "htm", "html", "xml" };
        } else
        if (ext_str.length() == 0) {
            return new String[0];
        } else {
            return ext_str.split("\\s+\\.?");
        }
    }
    
    public boolean isTextFileExtension(String ext)
    {
        if (ext == null) {
            return false;
        }
        if (ext.startsWith(".")) {
            ext = ext.substring(1);
        }
        String[] txt_extensions = getTextFileExtensions();
        for (int i=0; i < txt_extensions.length; i++) {
            if (ext.equalsIgnoreCase(txt_extensions[i])) {
                return true;
            }
        }
        return false;
    }

    public String getApplicationProperty(String name) 
    {
        return getApplicationProperties().getProperty(name);
    }

    public void setApplicationProperty(String name, String value) throws DocException 
    {
        getApplicationProperties().setProperty(name, value);
    }

    public void setApplicationProperties(String[] names, String[] values) throws DocException 
    {
        getApplicationProperties().setProperties(names, values);
    }

    public void setApplicationProperties(Map<String, String> props) throws DocException 
    {
        String[] names = props.keySet().toArray(new String[props.size()]);
        String[] values = new String[names.length];
        for (int i = 0; i < names.length; i++) {
            values[i] = props.get(names[i]);
        }
        getApplicationProperties().setProperties(names, values);
    }
    
    public DocmaLanguage[] getSupportedContentLanguages()
    {
        return getContentLanguages().getSupportedLanguages();
    }
    
    public RulesManager getRulesManager()
    {
        return rulesManager;
    }
    
    public String[] getAutoFormatClassNames()
    {
        String pval = getApplicationProperty(DocmaConstants.PROP_AUTOFORMAT_CLASSES);
        if ((pval == null) || pval.trim().equals("")) {
            return new String[0];
        } else {
            return pval.split(" ");
        }
    }
    
    public void registerAutoFormatClasses(String... clsNames) throws Exception
    {
        String pval = getApplicationProperty(DocmaConstants.PROP_AUTOFORMAT_CLASSES);
        pval = (pval == null) ? "" : pval.trim();
        List clsList = Arrays.asList(pval.split(" "));
        StringBuilder sb = new StringBuilder(pval);
        for (String cn : clsNames) {
            if ((cn != null) && (cn.length() > 0) && !clsList.contains(cn)) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(cn);
            }
        }
        setApplicationProperty(DocmaConstants.PROP_AUTOFORMAT_CLASSES, sb.toString());
    }
    
    public void unregisterAutoFormatClasses(String... clsNames) throws Exception
    {
        String[] oldNames = getAutoFormatClassNames();
        List removeList = Arrays.asList(clsNames);
        StringBuilder sb = new StringBuilder();
        for (String old : oldNames) {
            if (! removeList.contains(old)) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(old);
            }
        }
        setApplicationProperty(DocmaConstants.PROP_AUTOFORMAT_CLASSES, sb.toString());
    }

    public String[] getRuleClassNames()
    {
        String pval = getApplicationProperty(DocmaConstants.PROP_RULE_CLASSES);
        if ((pval == null) || pval.trim().equals("")) {
            return new String[0];
        } else {
            return pval.split(" ");
        }
    }
    
    public void registerRuleClasses(String... clsNames) throws Exception
    {
        String pval = getApplicationProperty(DocmaConstants.PROP_RULE_CLASSES);
        pval = (pval == null) ? "" : pval.trim();
        List clsList = Arrays.asList(pval.split(" "));
        StringBuilder sb = new StringBuilder(pval);
        for (String cn : clsNames) {
            if ((cn != null) && (cn.length() > 0) && !clsList.contains(cn)) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(cn);
            }
        }
        setApplicationProperty(DocmaConstants.PROP_RULE_CLASSES, sb.toString());
    }
    
    public void unregisterRuleClasses(String... clsNames) throws Exception
    {
        String[] oldNames = getRuleClassNames();
        List removeList = Arrays.asList(clsNames);
        StringBuilder sb = new StringBuilder();
        for (String old : oldNames) {
            if (! removeList.contains(old)) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(old);
            }
        }
        setApplicationProperty(DocmaConstants.PROP_RULE_CLASSES, sb.toString());
    }

    /* --------------  Package local methods  ---------------------- */

    DocmaSession connect(String userId) throws Exception
    {
        DocStoreSession docSess = dsm.connect(userId);
        DocmaSession docmaSess = new DocmaSession(this, docSess);
        openSessions.add(docmaSess);
        return docmaSess;
    }

    void releaseSession(DocmaSession docmaSess)
    {
        openSessions.remove(docmaSess);
    }

    public UserManager getUserManager()
    {
        return userMgmt;
    }
    
    public void setUserManager(UserManager um)
    {
        this.userMgmt = um;
    }

    protected ApplicationProperties getApplicationProperties()
    {
        return appProps;
    }

    protected ContentLanguages getContentLanguages()
    {
        return contentLangs;
    }

    public DocmaCharEntity[] getCharEntities()
    {
        if (charEntities == null) {
            synchronized(this) {
                String ent_str = appProps.getProperty(DocmaConstants.PROP_CHAR_ENTITIES);
                if ((ent_str != null) && !ent_str.equals("")) {
                    try {
                        charEntities = DocmaCharEntity.loadFromString(ent_str);
                    } catch (Exception ex) {
                        Log.error("Could not load char entities from application property. Falling back to defaults.");
                    }
                }
                if ((charEntities == null) || (charEntities.length == 0)) {
                    charEntities = DocmaCharEntity.DEFAULT_ENTITIES;
                }
            }
        }
        return charEntities;
    }

    public synchronized void setCharEntities(DocmaCharEntity[] entities)
    {
        // Store character entities as application property (to be able to
        // restore editor configuration files after installation of new
        // Docmenta version -> see AppConfigurator.initWebApplication).
        // Note: This methods needs to be overwritten by DocmaWebApplication
        // to pass the new char entities to the loaded content handlers.
        String ent_str = DocmaCharEntity.saveToString(entities);
        try {
            appProps.setProperty(DocmaConstants.PROP_CHAR_ENTITIES, ent_str);
        } catch (Exception ex) {
            throw new DocRuntimeException(ex);
        }

        // Store character entities in editor configuration files:
        charEntities = entities;
    }

    FormattingEngine getFormatter() throws Exception
    {
        if (formatter == null) {
            String tempPath = appProps.getProperty(DocmaConstants.PROP_TEMP_PATH);
            String docbookXSLPath = appProps.getProperty(DocmaConstants.PROP_DOCBOOK_XSL_PATH);
            String docmaXSLPath = appProps.getProperty(DocmaConstants.PROP_DOCMA_XSL_PATH);

            formatter = new FormattingEngine(new File(docmaXSLPath),
                                             new File(docbookXSLPath),
                                             new File(tempPath));
        }
        return formatter;
    }


    PublicationArchivesFactory getPublicationArchivesFactory()
    {
        return pubArchivesFactory;
    }

    Activities getActivities()
    {
        return this.activities;
    }
    
    synchronized boolean isStoreDisabled(String storeId) 
    {
        initDisabledStoreSet();
        return disabledStores.contains(storeId);
    }
    
    synchronized void setStoreDisabled(String storeId, boolean isDisabled)
    {
        initDisabledStoreSet();
        int old_size = disabledStores.size();
        if (isDisabled) {
            disabledStores.add(storeId);
        } else {
            disabledStores.remove(storeId);
        }
        if (old_size != disabledStores.size()) {
            writeDisabledStoresProperty();
        }
    }
    
    private void writeDisabledStoresProperty()
    {
        String[] id_arr = disabledStores.toArray(new String[disabledStores.size()]);
        String val = DocmaUtil.concatStrings(id_arr, " ");
        try {
            appProps.setProperty(DocmaConstants.PROP_DISABLED_STORES, val);
        } catch (DocException dex) {
            throw new DocRuntimeException(dex);
        }
    }
    
    private void initDisabledStoreSet()
    {
        if (disabledStores == null) {
            disabledStores = new TreeSet<String>();
            String val = appProps.getProperty(DocmaConstants.PROP_DISABLED_STORES);
            if (val != null) {
                String[] id_arr = val.split(" ");
                for (String sid : id_arr) {
                    if (sid.length() > 0) disabledStores.add(sid);
                }
            }
        }
    }
    
}
