/*
 * AboutDialog.java
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
import org.zkoss.zul.*;
import org.zkoss.zk.ui.util.Clients;

/**
 *
 * @author MP
 */
public class AboutDialog extends Window
{

    public void onCloseClick()
    {
        setVisible(false);
    }

    public void showAbout(MainWindow mainWin) throws Exception
    {
        Label app_edition = (Label) getFellow("AboutAppEditionLabel");
        Label app_copyright = (Label) getFellow("AboutCopyrightLabel");
        Label app_homepage = (Label) getFellow("AboutHomepageLabel");
        Label app_email =  (Label) getFellow("AboutEMailLabel");

        DocmaWebApplication webApp = GUIUtil.getDocmaWebApplication(mainWin);
        ApplicationServices appserv = webApp.getApplicationServices();
        app_edition.setValue(mainWin.getAppEditionName(appserv));
        app_copyright.setValue(DocmaConstants.DISPLAY_APP_COPYRIGHT);
        app_homepage.setValue(DocmaConstants.DISPLAY_APP_HOMEPAGE);
        app_email.setValue(DocmaConstants.DISPLAY_APP_EMAIL);
        doModal();
    }

    public void onViewLicense()
    {
        openLicenseAgreement();
    }

    public static void openLicenseAgreement()
    {
        openLicenseAgreement("license/license.html");
    }

    public static void openLicenseAgreement(String license_path)
    {
        String client_action = "window.open('" + license_path + "', " +
          "'_blank', 'width=680,height=500,resizable=yes,scrollbars=yes,location=no,menubar=yes,status=yes,');";
        Clients.evalJavaScript(client_action);
    }
}
