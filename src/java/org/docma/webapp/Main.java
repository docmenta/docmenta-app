/*
 * Main.java
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
import org.docma.plugin.implementation.PluginManager;

import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.Composer;
import org.zkoss.zul.*;

/**
 *
 * @author MP
 */
public class Main implements Composer {
    MainWindow mainWin;
    Label appversion;


    public void doAfterCompose(Component comp) throws Exception
    {
        mainWin = (MainWindow) comp;
        String version_label = "V" + DocmaConstants.DISPLAY_APP_VERSION;
        if (DocmaConstants.DEBUG) {
            version_label += " Debug";
        }
        appversion = (Label) mainWin.getFellow("appversion");
        appversion.setValue(version_label);
        // treeMenu = (Menupopup) mainWin.getFellow("treemenu");
        // treeMenu.addEventListener("onOpen", this);
        // Menuitem itemTest = new Menuitem("Test xyz", "img/docicon.gif");
        // itemTest.addEventListener("onClick", this);
        // treeMenu.appendChild(itemTest); // itemNewContent.setParent(treePopup);
        // Tabpanels tabpanes = mainTabbox.getTabpanels();
        // Component[] comps = mainWin.getDesktop().getExecution().createComponents("tab_versioning.inc.zul", null);
        // for (int i=0; i < comps.length; i++) comps[i].setParent(tabpanes);

        // Take over Docma session created by Login.java from global session context
        // and move it to desktop context:
        Session uiSess = mainWin.getDesktop().getSession();
        DocmaWebSession webSess = GUIUtil.getDocmaWebSession_SessionContext(uiSess);
        DocmaSession docmaSess = null;
        if (webSess != null) {
            docmaSess = webSess.getDocmaSession();
        }
        if ((webSess == null) || (docmaSess == null)) {
            mainWin.getDesktop().getExecution().sendRedirect("login.zul");
            return;
        }
        GUIUtil.setDocmaWebSession_SessionContext(uiSess, null);  // remove from global session context
        GUIUtil.setDocmaWebSession(mainWin, webSess);  // store in desktop context of main window

        // Set Theme
        // Image appLogo = (Image) mainWin.getFellow("MainWinAppLogo");
        mainWin.setContentStyle(webSess.getThemeProperty("mainwin.contentstyle.css"));
        Div mainArea = (Div) mainWin.getFellow("MainWinContentArea");
        mainArea.setStyle(webSess.getThemeProperty("mainwin.contentarea.css"));
        
        // Add user name to user-profile button
        Button btn = (Button) mainWin.getFellow("EditProfileHeadButton");
        String uid = docmaSess.getUserId();
        String usrname = docmaSess.getUserManager().getUserNameFromId(uid);
        String btn_label = mainWin.i18("label.edituserprofile") + ": " + usrname;
        btn.setLabel(btn_label);

        // Initialize web-session object (connect main-window to web-session)
        webSess.setMainWindow(mainWin);
        mainWin.initMainWindow();

        // Send onInitMainWindow to all loaded plugins
        PluginManager pluginMgr = webSess.getDocmaWebApplication().getPluginManager();
        if (pluginMgr != null) {
            try {
                pluginMgr.sendOnInitMainWindow(webSess.getPluginInterface());
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }

        // The context menu has to be created after the onInitMainWindow 
        // has been sent to the plugins, because the plugins may register
        // additional context menu items.
        Menupopup treemenu = (Menupopup) mainWin.getFellow("treemenu");
        MenuUtil.createDocTreeContextMenu(treemenu, webSess);
    }

}
