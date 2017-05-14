/*
 * ActivityWinComposer.java
 * 
 *  Copyright (C) 2017  Manfred Paula, http://www.docmenta.org
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

import org.docma.app.Activity;
import org.docma.app.DocmaSession;
import org.docma.coreapi.DocI18n;
import org.docma.coreapi.DocVersionId;
import org.docma.plugin.web.ButtonType;
import org.docma.plugin.web.UIEvent;
import org.docma.plugin.web.UIListener;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.*;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Window;

/**
 *
 * @author MP
 */
public class ActivityWinComposer extends SelectorComposer<Component>
{
    @Wire("#ActivityWindow") Window activityWin;
    @Wire("#ActivityWinProgress") Progressmeter progressmeter;
    @Wire("#ActivityWinPercentLabel") Label percentLabel;
    @Wire("#ActivityWinMsgLabel") Label msgLabel;
    @Wire("#ActivityWinErrorCountLabel") Label errorCountLabel;
    @Wire("#ActivityWinWarningCountLabel") Label warningCountLabel;
    @Wire("#ActivityWinCancelBtn") Button cancelBtn;
    @Wire("#ActivityWinCloseBtn") Button closeBtn;
    @Wire("#ActivityWinRefreshTimer") Timer refreshTimer;
    
    private Activity activity;
    private String activityTitle;
    private String storeId;
    private DocVersionId versionId;

    public void openWindow(Activity act)
    {
        DocmaWebSession webSess = GUIUtil.getDocmaWebSession(activityWin);
        DocmaSession docmaSess = webSess.getDocmaSession();
        DocI18n i18n = docmaSess.getI18n();
        this.storeId = docmaSess.getStoreId();
        this.versionId = docmaSess.getVersionId();
        this.activity = act;
        this.activityTitle = i18n.getLabel(act.getTitleKey(), act.getTitleArgs());
        activityWin.setTitle(activityTitle);
        refreshGUI();
        activityWin.setVisible(true);
        refreshTimer.setDelay(3000);
        refreshTimer.setRunning(true);
    }

    /**
     * Window is closed by the application, for example if user closes the
     * store. In this case the activity should not be removed (if user
     * opens the store again, the activity window is opened again).
     */ 
    public void closeWindow()
    {
        prepareClose(false);
        activityWin.setVisible(false);   // close dialog
    }
    
    public boolean isActivityRunning()
    {
        return (activity != null) && activity.isRunning();
    }
    
    public boolean isWindowOpened()
    {
        return activityWin.isVisible();
    }

    /**
     * User closes window by clicking the Window close button provided by the 
     * UI framework.
     * 
     * @param evt  the onClose event
     */    
    @Listen("onClose = #ActivityWindow")
    public void onWindowClose(Event evt)
    {
        if (activity.isFinished()) {
            prepareClose(true);
            activityWin.setVisible(false);   // close dialog
            evt.stopPropagation();           // prevent detaching the window
        } else {
            onCancelClick();
        }
    }

    /**
     * User closes window by clicking the button provided by the application.
     */
    @Listen("onClick = #ActivityWinCloseBtn")
    public void onCloseClick()
    {
        if (activity.isFinished()) {
            prepareClose(true);
            activityWin.setVisible(false);   // close dialog
        } else {
            onCancelClick();
        }
    }
    
    @Listen("onClick = #ActivityWinCancelBtn")
    public void onCancelClick()
    {
        MessageUtil.showYesNoQuestion(activityWin, 
            "activity.window.confirm_cancel_title", 
            "activity.window.confirm_cancel_msg", 
            new Object[] { activityTitle },
            new UIListener() {
                public void onEvent(UIEvent evt) {
                    if (ButtonType.YES.equals(evt.getButtonType())) {
                        activity.setCancelFlag(true);
                    }
                }
            }
        );
    }
    
    @Listen("onClick = #ActivityWinOpenLogBtn")
    public void onOpenLogClick()
    {
        DocmaWebSession webSess = GUIUtil.getDocmaWebSession(activityWin);
        Desktop desk = webSess.getMainWindow().getDesktop();
        String url = desk.getExecution().encodeURL("viewActivityLog.jsp?desk=" + 
                     desk.getId() + "&activity=" + activity.getActivityId() +
                     "&stamp=" + System.currentTimeMillis());
        String win_name = "activity_log" + activity.getActivityId();
        String client_action = 
            "window.open('" + url + "', '" + win_name +
            "', 'width=600,height=500,left=50,top=50" +
            ",resizable=yes,menubar=yes,toolbar=yes,location=no,scrollbars=yes');"; 
        Clients.evalJavaScript(client_action);
    }
    
    @Listen("onTimer = #ActivityWinRefreshTimer")
    public void onTimerRefresh()
    {
        int delay = refreshTimer.getDelay();
        if (delay < 8000) {
            refreshTimer.setDelay(delay + 1000);
        }
        refreshGUI();
    }

    private void refreshGUI()
    {
        DocI18n i18n = GUIUtil.getI18n(activityWin);
        String msgKey = activity.getMessageKey();
        String msg; 
        if ((msgKey == null) || msgKey.equals("")) {
            msg = "";
            msgLabel.setVisible(false);
        } else {
            msg = i18n.getLabel(msgKey, activity.getMessageArgs());
            msgLabel.setVisible(true);
        }
        msgLabel.setValue(msg);
        errorCountLabel.setValue("" + activity.getErrorCount());
        warningCountLabel.setValue("" + activity.getWarningCount());
        
        int percent = activity.getPercent();
        progressmeter.setValue(percent);
        String percentString;
        if (activity.isRunning()) {
            percentString = percent + "%";
            cancelBtn.setDisabled(false);
            cancelBtn.setVisible(true);
            closeBtn.setDisabled(true);
            closeBtn.setVisible(false);
        } else {
            percentString = "100%";
            cancelBtn.setDisabled(true);
            cancelBtn.setVisible(false);
            closeBtn.setDisabled(false);
            closeBtn.setVisible(true);
            
            refreshTimer.setRunning(false);
        }
        percentLabel.setValue(percentString);
    }
    
    private void prepareClose(boolean closedByUser)
    {
        if (closedByUser && activity.isFinished()) {
            DocmaWebSession webSess = GUIUtil.getDocmaWebSession(activityWin);
            DocmaSession docmaSess = webSess.getDocmaSession();

            docmaSess.removeDocStoreActivity(storeId, versionId, activity.getActivityId());
        }
        refreshTimer.setRunning(false);
        
        // Set progressmeter to 0 percent, to avoid backwards moving 
        // progressbar on next opening of the window.
        progressmeter.setValue(0);
    }
}
