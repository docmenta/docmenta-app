/*
 * AppConfigurator.java
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

package org.docma.webapp;

import org.docma.app.*;
import org.docma.app.fsimplementation.*;
import org.docma.coreapi.*;
import org.docma.coreapi.fsimplementation.*;
import org.docma.util.PropertiesLoader;
import org.docma.util.Log;
import org.docma.userapi.UserManager;
import org.docma.userapi.fsimplementation.UserManagerImpl;
import org.docma.hibernate.HibernateUtil;

import java.io.File;
import java.util.logging.Level;
import javax.servlet.ServletContext;


/**
 *
 * @author MP
 */
public class AppConfigurator implements DocmaConfiguration, WebConfiguration
{
    private static final String DOCMA_CONFIG_FILE = "WEB-INF" + File.separator + "docma.properties";
    private static final String DB_CONFIG_FILE = "WEB-INF" + File.separator + "db_config.properties";
    private static final String DB_CONFIG_FILE_EMBEDDED = "WEB-INF" + File.separator + "db_config_embedded.properties";

    static final String PROP_DOCSTORE_DIR = "DocStoreDirectory";
    static final String PROP_LOG_LEVEL = "LogLevel";

    private File propFile = null;
    private PropertiesLoader propLoader = null;

    private File                 webAppDir = null;
    private File                 webInfDir = null;
    private File                 docmaDir  = null;
    private File                 gentextDir = null;
    private File                 resourcesDir = null;
    private File                 publicationOnlineDir = null;
    private File                 docStoreDir  = null;
    private File                 userDataDir  = null;
    private DocmaWebApplication  webApp   = null;
    private UserManager          userMgmt = null;
    private DocmaI18             i18      = null;
    private DocStoreManagerImpl  dsm      = null;
    private ContentLanguages     contentLangs = null;
    private ApplicationProperties appProps = null;


    /* --------------  Package local methods  --------------- */

    static PropertiesLoader getWebApplicationConfigLoader(String webAppDir) throws DocException
    {
        File pFile = new File(webAppDir, DOCMA_CONFIG_FILE);
        return new PropertiesLoader(pFile);
    }

    /* --------------  Public methods  --------------- */

    public void initWebApplication(ServletContext servletCtx) throws DocException
    {
        String webAppDirectory = servletCtx.getRealPath("/");
        webAppDir = new File(webAppDirectory);
        if (! (webAppDir.exists() && webAppDir.isDirectory())) {
            throw new DocException("WebApplication directory does not exist: " + webAppDir.getAbsolutePath());
        }
        webInfDir = new File(webAppDir, "WEB-INF");
        docmaDir = new File(webAppDir, "docma");
        gentextDir = new File(docmaDir, "gentext");
        resourcesDir = new File(docmaDir, "resources");

        docStoreDir = null;
        propFile = new File(webAppDir, DOCMA_CONFIG_FILE);
        if (propFile.exists()) {
            propLoader = new PropertiesLoader(propFile);
            String docStorePath = propLoader.getProp(PROP_DOCSTORE_DIR); // new File("C:\\TEMP\\docmatest\\");
            if ((docStorePath != null) && (docStorePath.trim().length() > 0)) {
                docStoreDir = new File(docStorePath);
            }
            Level log_level = DocmaConstants.DEBUG ? Level.FINEST : Level.CONFIG;  // default log level
            try {
                String level_str = propLoader.getProp(PROP_LOG_LEVEL);
                if ((level_str != null) && (level_str.length() > 0)) {
                    log_level = Level.parse(level_str);
                };
            } catch (Exception ex) { ex.printStackTrace(); }
            Log.info("Setting log level " + log_level);
            Log.setLevel(log_level);
        }
        if ((docStoreDir == null) || !(docStoreDir.exists() && docStoreDir.isDirectory())) {
            throw new DocException("DocStore directory does not exist: " + docStoreDir);
        }
        userDataDir = new File(docStoreDir, "userdata");
        if (! userDataDir.exists()) userDataDir.mkdir();
        userMgmt = new UserManagerImpl(userDataDir.getAbsolutePath()); // new FiledUsers();
        i18 = new DocmaI18Impl();

        File docStoreDTDFile = new File(docmaDir, "docma.dtd");
        dsm = new DocStoreManagerImpl(docStoreDir.getAbsolutePath(), docStoreDTDFile.getAbsolutePath());
        // dsm.setUserManager(userMgmt);
        dsm.setVersionIdFactory(new DefaultVersionIdFactory());

        contentLangs = new ContentLanguagesImpl(webInfDir.getAbsolutePath());

        File tempDir = new File(docStoreDir, "temp");
        if (! tempDir.exists()) tempDir.mkdir();

        File docbookXSLDir = new File(docStoreDir, "docbook-xsl");
        if (! docbookXSLDir.exists()) docbookXSLDir = new File(webAppDir, "docbook-xsl");

        File dbConfigFile = new File(webAppDir, DB_CONFIG_FILE);
        if (dbConfigFile.exists()) {
            HibernateUtil.setExternalConnectionProperties(dbConfigFile);
        } else {
            Log.info("DB connection properties file not found: " + dbConfigFile.getAbsolutePath());
        }
        File dbConfigEmb = new File(webAppDir, DB_CONFIG_FILE_EMBEDDED);
        if (dbConfigEmb.exists()) {
            HibernateUtil.setEmbeddedConnectionProperties(dbConfigEmb);
        }
        
        File systemDir = new File(docStoreDir, "system");
        if (! systemDir.exists()) systemDir.mkdir();
        
        File derbySystemDir = new File(systemDir, "derby");
        if (! derbySystemDir.exists()) derbySystemDir.mkdir();
        
        System.setProperty("derby.system.home", derbySystemDir.getAbsolutePath());
        
        appProps = new ApplicationPropertiesImpl(docStoreDir.getAbsolutePath());

        String pubOnline_rel = appProps.getProperty(DocmaConstants.PROP_PUBLICATION_ONLINE_RELATIVE_PATH);
        String pubOnline_abs = appProps.getProperty(DocmaConstants.PROP_PUBLICATION_ONLINE_PATH);
        if (pubOnline_rel == null) pubOnline_rel = "";
        if (pubOnline_abs == null) pubOnline_abs = "";
        if (pubOnline_abs.trim().equals("") && pubOnline_rel.trim().equals("")) {  
            // if no absolute path and no relative path given, use default relative path
            pubOnline_rel = "publications";
            publicationOnlineDir = new File(webAppDir, pubOnline_rel);
        } else {
            if (pubOnline_rel.trim().length() > 0) {
                publicationOnlineDir = new File(webAppDir, pubOnline_rel);
            } else {
                publicationOnlineDir = new File(pubOnline_abs);
            }
        }

        String[] pNames = new String[] {
            DocmaConstants.PROP_BASE_PATH,
            DocmaConstants.PROP_STORES_PATH,
            DocmaConstants.PROP_STORE_DTD_PATH,
            DocmaConstants.PROP_TEMP_PATH,
            DocmaConstants.PROP_DOCBOOK_XSL_PATH,
            DocmaConstants.PROP_DOCMA_XSL_PATH,
            DocmaConstants.PROP_DOCMA_GENTEXT_PATH,
            DocmaConstants.PROP_DOCMA_RESOURCES_PATH,
            DocmaConstants.PROP_PUBLICATION_ONLINE_RELATIVE_PATH,
            DocmaConstants.PROP_PUBLICATION_ONLINE_PATH
        };
        String[] pValues = new String[] {
            docStoreDir.getAbsolutePath(),
            dsm.getStoresDirectory(),
            docStoreDTDFile.getAbsolutePath(),
            tempDir.getAbsolutePath(),
            docbookXSLDir.getAbsolutePath(),
            docmaDir.getAbsolutePath(),
            gentextDir.getAbsolutePath(),
            resourcesDir.getAbsolutePath(),
            pubOnline_rel,
            publicationOnlineDir.getAbsolutePath()
        };
        appProps.setProperties(pNames, pValues);

        webApp = new DocmaWebApplication(servletCtx, webAppDir.getAbsolutePath(), 
                                         dsm, userMgmt, i18, contentLangs, appProps,
                                         new PublicationArchivesFactoryImpl(tempDir));

        // configure revision store
        // File revisionsDir = new File(docStoreDir, "revisions");
        // if (! revisionsDir.exists()) revisionsDir.mkdirs();
        RevisionStoreFactory revStoreFact = new RevisionStoreFactoryImpl();
        revStoreFact.setMaxRevisionsPerUser(getMaxRevisionsPerUser());
        webApp.setRevisionStoreFactory(revStoreFact);

        System.setProperty("com.sun.media.jai.disableMediaLib", "true");  // disable native mode
        if (DocmaConstants.DEBUG) {
            System.out.println("java.io.tmpdir: " + System.getProperty("java.io.tmpdir"));
        }
        
        webApp.startUp();
    }

//    public void flush() throws DocException {
//        proploader.savePropFile("Docma application configuration");
//    }

    public DocmaWebApplication getWebApplication() {
        return webApp;
    }


    public String getDocStoreDirectory() {
        return docStoreDir.getAbsolutePath();
    }

//    public void setBaseDirectory(String baseDir) {
//        proploader.setProp("BaseDirectory", baseDir);
//        this.baseDir = baseDir;
//    }

    /* --------------  Interface DocmaConfiguration  --------------- */

    public UserManager getUserManager() {
        return userMgmt;
    }

    public DocmaI18 getI18() {
        return i18;
    }

    public DocStoreManager getDocStoreManager() {
        return dsm;
    }

    /* --------------  Private methods  --------------- */

    private int getMaxRevisionsPerUser()
    {
        String max_revs = appProps.getProperty(DocmaConstants.PROP_MAX_REVISIONS_PER_USER);
        if ((max_revs != null) && (max_revs.length() > 0)) {
            try {
                return Integer.parseInt(max_revs);
            } catch (Exception ex) {}
        }
        return DocmaConstants.DEFAULT_MAX_REVISIONS_PER_USER;
    }

}
