/*
 * EditWindow.java
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

import org.docma.plugin.VersionId;
import org.docma.plugin.StoreConnection;
import org.docma.plugin.Node;
import org.docma.plugin.web.WebUserSession;
import org.docma.coreapi.DocConstants;
import org.docma.util.Log;

import org.zkoss.zul.*;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.Clients;


/**
 *
 * @author MP
 */
public class EditWindow extends Window
{
    WebUserSession webSess;        // is initialized in Edit.java
    // MainWindow mainWin;      // is initialized in Edit.java
    String storeId;          // is initialized in Edit.java
    VersionId versionId;     // is initialized in Edit.java
    String nodeId;           // is initialized in Edit.java
    String transMode;        // is initialized in Edit.java

//    private void do_Save_Okay() throws Exception
//    {
//        // Session sess = getDesktop().getSession();
//        // DocmaSession docmaSess = GUIUtil.getDocmaWebSession(sess, desktopId).getDocmaSession();
//        // Label statusLabel = (Label) getFellow("statusLabel");
//        int new_progress = getProgressSliderValue();
//        if (new_progress != node.getProgress()) {
//            try {
//                docmaSess.startTransaction();
//                node.setProgress(new_progress);
//                docmaSess.commitTransaction();
//            } catch (Exception ex) {
//                docmaSess.rollbackTransaction();
//            }
//        }
//        // statusLabel.setValue("Saving...OK");
//        // Messagebox.show("Save okay!");
//    }

    private int getProgressSliderValue()
    {
        Slider contProgress = (Slider) getFellow("editProgressSlider");
        return contProgress.getCurpos();
    }

    public void onSave() 
    {
        Label statusLabel = (Label) getFellow("statusLabel");
        statusLabel.setValue("Saving...");
        int prog_value = getProgressSliderValue();
        Clients.evalJavaScript("doSaveContent(false, " + prog_value + ");");
    }

    public void onSaveAndClose()
    {
        Label statusLabel = (Label) getFellow("statusLabel");
        statusLabel.setValue("Saving...");
        int prog_value = getProgressSliderValue();
        Clients.evalJavaScript("doSaveContent(true, " + prog_value + ");");
    }

    public void onSaveFinished(Event evt) throws Exception
    {
        if (DocConstants.DEBUG) {
            System.out.println("Executing EditWindow.onSaveFinished()");
        }
        Object data = evt.getData();
        if (data == null) {
            Messagebox.show("Save error: missing result code!");
            return;
        }
        // DocmaSession docmaSess = mainWin.getDocmaSession();
        int error_code;
        try {
            String data_str = data.toString();
            error_code = Integer.parseInt(data_str);
        } catch (Exception ex) {
            ex.printStackTrace();
            Messagebox.show("Save error: invalid result code!");
            return;
        }

        Label statusLabel = (Label) getFellow("statusLabel");
        switch (error_code) {
            case 0:
                statusLabel.setValue("Saving...OK");
                break;
            case -1:
                statusLabel.setValue("No changes");
                break;
            default:
                statusLabel.setValue("Saving...Error");
                break;
        }

        // if (error_code >= 0) {  // content was saved or error occured
        //     // rerender tree node
        //     DocmaWebSession webSess = GUIUtil.getDocmaWebSession(getDesktop().getSession(), desktopId);
        //     MainWindow mainWin = webSess.getMainWindow();
        //     Events.echoEvent("onProcessDeferredEvents", mainWin, null);
        // }

//        if (error_code <= 0) {  // Saving was okay (0) or content did not changed (-1).
//            // do_Save_Okay();  // set progress value if changed
//            return;
//        }
//
//        // if error
//        String err_msg;
//        switch (error_code) {
//            case 1:
//                err_msg = "Id of anchor is already used in another node.";
//                break;
//            case 2:
//                err_msg = "Id of anchor is not unique within node.";
//                break;
//            case 3:
//                err_msg = "Translated anchors differ from original anchors.";
//                break;
//            default:
//                err_msg = "Unknown save error. Please try again.";
//                break;
//        }
//        Messagebox.show(err_msg);
    }

    public void onCancel() throws Exception
    {
        boolean isTempConn = needsTempConnection(webSess);
        StoreConnection editConn = isTempConn ? createTempConnection() : webSess.getOpenedStore();
        Node nd = editConn.getNodeById(nodeId);
        try {
            if (nd != null) {
                nd.removeLock();
            }
        } finally {
            if (isTempConn) {
                webSess.closeTempStoreConnection(editConn);
            }
        }

        String js_action = "doCancel();"; // "window.close();";
        Clients.evalJavaScript(js_action);

        // setVisible(false);
        // detach();
    }

    public void onKeepAlive() throws Exception
    {
        if (DocConstants.DEBUG) {
            System.out.println("Edit window: keep alive event!");
        }
        boolean isTempConn = needsTempConnection(webSess);
        StoreConnection editConn = isTempConn ? createTempConnection() : webSess.getOpenedStore();
        Node nd = editConn.getNodeById(nodeId);
        try {
            if (nd != null) {
                if (! nd.refreshLock()) {
                    // Refresh fails if lock no longer exists due to a timeout.
                    // Should not occur, because keep alive messages
                    // should refresh lock before timeout occurs.
                    // Nevertheless a network problem could cause timeout.
                    nd.setLock();  // Reset lock
                    Log.warning("Lock refresh of editor window failed. Resetting lock.");
                }
            } else {
                Messagebox.show("Error: Content node no longer exists. Maybe content was deleted by another user!");
            }
        } finally {
            if (isTempConn) {
                webSess.closeTempStoreConnection(editConn);
            }
        }
    }

    private boolean needsTempConnection(WebUserSession mainSess)
    {
        StoreConnection conn = mainSess.getOpenedStore();
        if (conn == null) {
            return true;
        }
        String mainStoreId = conn.getStoreId();
        VersionId mainVerId = conn.getVersionId();
        String mainTransMode = conn.getTranslationMode();
        String mainLang = (mainTransMode != null) ? mainTransMode : "";
        String editLang = (this.transMode != null) ? this.transMode : "";

        boolean isTempSess = !(mainStoreId.equals(this.storeId) &&
                               mainVerId.equals(this.versionId) &&
                               mainLang.equals(editLang));
        return isTempSess;
    }

    private StoreConnection createTempConnection() throws Exception
    {
        StoreConnection tempConn = webSess.createTempStoreConnection(this.storeId, this.versionId);
        if (this.transMode != null) {
            tempConn.enterTranslationMode(this.transMode);
        }
        return tempConn;
    }

//    public void onIncrease()
//    {
//        // String js_action = "alert('hallo');"; // "window.contentfrm.editContent();";
//        // org.zkoss.zk.ui.util.Clients.evalJavaScript(js_action);
//
//        Label lab = (Label) getFellow("counterLabel");
//        counter++;
//        lab.setValue("" + counter);
//    }

}
