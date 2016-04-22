/*
 * Setup.java
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
import org.docma.coreapi.*;
import org.docma.userapi.UserManager;
import org.docma.util.PropertiesLoader;

import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.Composer;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Window;
import org.zkoss.zul.Button;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;

/**
 *
 * @author MP
 */
public class Setup implements Composer, EventListener
{
    static final String ADMIN_USERNAME = DocmaConstants.SYS_ADMIN_LOGIN_NAME;
    static final String ADMIN_GROUPNAME = DocmaConstants.SYS_ADMIN_GROUP_NAME;

    Window setupWin;
    Button saveBtn;
    Textbox docstore_box;
    Textbox adminpwd_box;
    Textbox repeatpwd_box;
    Checkbox license_box;
    Label license_label;
    Toolbarbutton viewlicenseBtn;

    String app_root;
    PropertiesLoader propLoader;

    public void doAfterCompose(Component comp) throws Exception
    {
        setupWin = (Window) comp;
        saveBtn = (Button) setupWin.getFellow("setupsavebtn");
        saveBtn.addEventListener("onClick", this);
        docstore_box = (Textbox) setupWin.getFellow("docstorepath");
        adminpwd_box = (Textbox) setupWin.getFellow("adminpwd");
        repeatpwd_box = (Textbox) setupWin.getFellow("adminrepeatpwd");
        license_box = (Checkbox) setupWin.getFellow("acceptlicensebox");
        license_label = (Label) setupWin.getFellow("acceptlicenselabel");
        license_label.addEventListener("onClick", this);
        viewlicenseBtn = (Toolbarbutton) setupWin.getFellow("viewlicensebtn");
        viewlicenseBtn.addEventListener("onClick", this);

        Session uiSess = setupWin.getDesktop().getSession();
        GUIUtil.closeAllDocmaSessions(uiSess);

        app_root = setupWin.getDesktop().getWebApp().getRealPath("/");
        propLoader = AppConfigurator.getWebApplicationConfigLoader(app_root);
        String docStoreDir = propLoader.getProp(AppConfigurator.PROP_DOCSTORE_DIR);
        if (docStoreDir == null) docStoreDir = "";
        if (docStoreDir.trim().length() > 0) {
            setupWin.getDesktop().getExecution().sendRedirect("../login.zul?redir=false");
            return;
        }
        docstore_box.setValue(docStoreDir);
    }

    public void onEvent(Event evt) throws Exception
    {
        Component t = evt.getTarget();
        if (t == saveBtn) {
            if (! license_box.isChecked()) {
                Messagebox.show("You have to accept the License Agreement to continue!");
                return;
            }

            String docstore = docstore_box.getValue();
            String adminpwd = adminpwd_box.getValue();
            String repeatpwd = repeatpwd_box.getValue();
            adminpwd_box.setValue("");
            repeatpwd_box.setValue("");

            if (! adminpwd.equals(repeatpwd)) {
                Messagebox.show("Password mismatch!");
                return;
            }

            propLoader.setProp(AppConfigurator.PROP_DOCSTORE_DIR, docstore);
            propLoader.savePropFile("Docmenta configuration properties");

            try {
                AppConfigurator appconf = new AppConfigurator();
                appconf.initWebApplication(setupWin.getDesktop().getWebApp().getServletContext());
                DocmaWebApplication app = appconf.getWebApplication();
                GUIUtil.setDocmaWebApplication(setupWin, app);

                // Set administrator user and group
                UserManager userManager = app.getUserManager();
                String admin_id = userManager.getUserIdFromName(ADMIN_USERNAME);
                if (admin_id == null) {
                    admin_id = userManager.createUser(ADMIN_USERNAME);
                }
                userManager.setPassword(admin_id, adminpwd);

                String grp_id = userManager.getGroupIdFromName(ADMIN_GROUPNAME);
                if (grp_id == null) {
                    grp_id = userManager.createGroup(ADMIN_GROUPNAME);
                }
                AccessRights ar = new AccessRights("*", AccessRights.getAllRights());
                String rights_str = ar.toString();
                userManager.setGroupProperty(grp_id, DocmaConstants.PROP_USERGROUP_RIGHTS, rights_str);

                if (! userManager.isUserInGroup(admin_id, grp_id)) {
                    userManager.addUserToGroup(admin_id, grp_id);
                }

                // Set example Auto-Format classes
                ApplicationProperties appProps = app.getApplicationProperties();
                String afc = appProps.getProperty(DocmaConstants.PROP_AUTOFORMAT_CLASSES);
                if ((afc == null) || afc.equals("")) {
                    afc = "org.docma.plugin.examples.ApplyTemplate" +
                          " org.docma.plugin.examples.FormatLines" +
                          " org.docma.plugin.examples.RegExpHighlight" + 
                          " org.docma.plugin.examples.XSLTAutoFormat";
                    appProps.setProperty(DocmaConstants.PROP_AUTOFORMAT_CLASSES, afc);
                }
            } catch (DocException dex) {
                Messagebox.show("Error: " + dex.getMessage());
            }
            setupWin.getDesktop().getExecution().sendRedirect("../login.zul");
        } else
        if (t == viewlicenseBtn) {
            AboutDialog.openLicenseAgreement("../license/license.html");
        } else 
        if (t == license_label) {
            license_box.setChecked(! license_box.isChecked());
        }
    }

    // public void onViewLicense()
    // {
    // }

}
