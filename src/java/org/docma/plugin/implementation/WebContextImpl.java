/*
 * WebContextImpl.java
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

import java.io.File;
import org.docma.webapp.*;

/**
 * Provides the plugin interface for DocmaWebApplication.
 * This is a wrapper class which provides the functionality
 * of DocmaWebApplication to plugins. This way it is possible to change the
 * implementation/interface of DocmaWebApplication without breaking the 
 * functionality of existing plugins.
 * Furthermore this wrapper may restrict the functionality that is provided
 * to plugins.
 *
 * @author MP
 */
public class WebContextImpl implements WebContext
{
    private DocmaWebApplication webApp;
    private File webAppDir;
    
    public WebContextImpl(DocmaWebApplication webApp)
    {
        this.webApp = webApp;
        this.webAppDir = new File(webApp.getWebAppDirectory());
    }

    public File getWebAppDirectory() 
    {
        return webAppDir;
    }

//    public void detachPluginFromWebSessions(String pluginId) 
//    {
//        DocmaSession[] sess_arr = webApp.getOpenSessions();
//        for (DocmaSession sess : sess_arr) {
//            DocmaWebSession websess = webApp.getWebSessionContext(sess.getSessionId());
//            if (websess != null) {
//                WebUserSessionImpl plugin_interface = (WebUserSessionImpl) websess.getPluginInterface();
//                plugin_interface.unloadPluginComponents(pluginId);
//            }
//        }
//    }
    
}
