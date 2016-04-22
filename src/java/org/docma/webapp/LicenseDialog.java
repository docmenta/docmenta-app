/*
 * LicenseDialog.java
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

import org.zkoss.zul.*;

/**
 *
 * @author MP
 */
public class LicenseDialog extends Window
{
    // private MainWindow mainWin;
    private boolean accepted = false;
    private Html licenseHtml;
    private Label licenseLabel;
    private Button declineBtn;
    private Button acceptBtn;
    private Button closeBtn;

    public void onAcceptClick()
    {
        accepted = true;
        setVisible(false);
    }

    public void onDeclineClick()
    {
        accepted = false;
        setVisible(false);
    }

    public void onCloseClick()
    {
        setVisible(false);
    }

    public boolean requestLicenseAcceptance(String licenseHtmlText)
    {
        init();
        String msg = GUIUtil.i18(this).getLabel("label.license.accept.message") + ":";
        licenseLabel.setValue(msg);
        declineBtn.setVisible(true);
        acceptBtn.setVisible(true);
        closeBtn.setVisible(false);
        licenseHtml.setContent(licenseHtmlText);
        accepted = false;
        doModal();  // show dialog and wait until user clicks accept or decline
        return accepted;
    }
    
    public void showLicense(String licenseHtmlText) 
    {
        init();
        String msg = GUIUtil.i18(this).getLabel("label.license.show.message") + ":";
        licenseLabel.setValue(msg);
        declineBtn.setVisible(false);
        acceptBtn.setVisible(false);
        closeBtn.setVisible(true);
        licenseHtml.setContent(licenseHtmlText);
        doModal();  // show dialog and wait until user clicks accept or decline
    }
    
    private void init()
    {
        licenseHtml = (Html) getFellow("LicenseDialogContentHtml");
        licenseLabel = (Label) getFellow("LicenseDialogMessageLabel");
        declineBtn = (Button) getFellow("LicenseDialogDeclineBtn");
        acceptBtn = (Button) getFellow("LicenseDialogAcceptBtn");
        closeBtn = (Button) getFellow("LicenseDialogCloseBtn");
    }
    
}
