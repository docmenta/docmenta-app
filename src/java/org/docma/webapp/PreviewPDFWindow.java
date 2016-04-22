/*
 * PreviewPDFWindow.java
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

import java.util.*;

import org.docma.app.*;
import org.docma.coreapi.*;

import org.zkoss.zul.*;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.event.Events;

/**
 *
 * @author MP
 */
public class PreviewPDFWindow extends Window
{
    private String desktopId;
    private MainWindow mainWin;
    private String storeId;
    private String nodeId;
    private DocVersionId verId;
    private String langCode;

    private Label nodeTitleLabel;
    private Label prodIdLabel;
    private Label prodVerLabel;
    private Label prodLangLabel;
    private Listbox pubConfListbox;
    private Listbox outConfListbox;

    private List pubConfIds = null;
    private List outConfIds = null;


    public void onReloadClick() throws Exception
    {
        // Get selected configuration ids
        String pubConfId = getSelectedPubConfId();
        String outConfId = getSelectedOutConfId();

        // Update configuration lists:
        // If the selected configurations no longer exist (i.e. were deleted),
        // then the selection will be set to the defaults (selected index == 0)
        updateConfLists(pubConfId, outConfId);

        // If the selected configurations no longer exist, this will show an error message
        setFrameURL(desktopId, nodeId, pubConfId, outConfId);
    }

    public void onCloseClick()
    {
        Clients.evalJavaScript("window.close();");
    }

    public void onChangeConfiguration()
    {
        setFramePressReload();
        
        // Save the selected output configuration as a user preference, so that
        // the last selected output configuration is preselected when the
        // preview window is opened next time.
        DocmaSession docmaSess = mainWin.getDocmaSession();
        String lastOutId = mainWin.getLastSelectedOutputConfigId(docmaSess, storeId, "pdf");
        String outConfId = getSelectedOutConfId();
        if ((outConfId != null) && !outConfId.equals(lastOutId)) {
            mainWin.setLastSelectedOutputConfigId(docmaSess, storeId, "pdf", outConfId);
        }
    }

    public void onInitFrame()
    {
        try {
            setFrameURL(desktopId, nodeId, getSelectedPubConfId(), getSelectedOutConfId());
        } catch (Exception ex) {
            ex.printStackTrace();
            clearFrame();
        }
    }

    void init(String desktopId, String nodeId)
    {
        // Init fields

        this.desktopId = desktopId;
        this.nodeId = nodeId;

        DocmaWebSession webSess = GUIUtil.getDocmaWebSession(getDesktop().getSession(), desktopId);
        DocmaSession docmaSess = webSess.getDocmaSession();
        // Note: Don't use docmaSess within this dialog after init was called.
        //       This could cause inconsistency, because user can open another
        //       store with this docmaSess object while the dialog is open.

        mainWin = webSess.getMainWindow();
        storeId = docmaSess.getStoreId();
        pubConfIds = Arrays.asList(docmaSess.getPublicationConfigIds());
        outConfIds = Arrays.asList(docmaSess.getOutputConfigIds());

        nodeTitleLabel = (Label) getFellow("PreviewPDFNodeTitleLabel");
        prodIdLabel    = (Label) getFellow("PreviewPDFProductId");
        prodVerLabel    = (Label) getFellow("PreviewPDFProductVersion");
        prodLangLabel  = (Label) getFellow("PreviewPDFProductLanguage");
        pubConfListbox  = (Listbox) getFellow("PreviewPDFPubConfList");
        outConfListbox  = (Listbox) getFellow("PreviewPDFOutputConfList");

        String selPubId = mainWin.getPreviewPubConfigId();
        String selOutId = mainWin.getLastSelectedOutputConfigId(docmaSess, storeId, "pdf");

        // Update GUI
        updateConfLists(selPubId, selOutId);
        setFrameWait();

        Events.echoEvent("onInitFrame", this, null);
    }

    private void updateConfLists(String selPubId, String selOutId)
    {
        DocmaSession docmaSess = mainWin.getDocmaSession();

        pubConfListbox.getItems().clear();
        outConfListbox.getItems().clear();

        int sel_idx = 0;
        pubConfListbox.appendItem("unfiltered", "");
        for (int i=0; i < pubConfIds.size(); i++) {
            String str = (String) pubConfIds.get(i);
            pubConfListbox.appendItem(str, str);
            if (str.equals(selPubId)) {
                sel_idx = i + 1;
            }
        }
        pubConfListbox.setSelectedIndex(sel_idx);

        sel_idx = 0;
        outConfListbox.appendItem("default", "");
        int idx = 0;
        for (int i=0; i < outConfIds.size(); i++) {
            String id = (String) outConfIds.get(i);
            DocmaOutputConfig outconf = docmaSess.getOutputConfig(id);
            if (outconf == null) continue;

            if (outconf.getFormat().equalsIgnoreCase("pdf")) {
                idx++;
                outConfListbox.appendItem(id, id);
                if (id.equals(selOutId)) {
                    sel_idx = idx;
                }
            }
        }
        outConfListbox.setSelectedIndex(sel_idx);
    }

    private void clearFrame()
    {
        Iframe frm = (Iframe) getFellow("previewpdffrm");
        frm.setSrc("empty_preview.html");
    }

    private void setFrameWait()
    {
        Iframe frm = (Iframe) getFellow("previewpdffrm");
        frm.setSrc("preview_wait.html");
    }

    private void setFramePressReload()
    {
        Iframe frm = (Iframe) getFellow("previewpdffrm");
        frm.setSrc("preview_pressreload.html");
    }

    private void setFrameURL(String desk, String nodeId, String pubConfId, String outConfId)
    throws Exception
    {
        DocmaSession docmaSess = mainWin.getDocmaSession();
        storeId = docmaSess.getStoreId();
        verId = docmaSess.getVersionId();
        langCode = docmaSess.getLanguageCode();
        if ((storeId == null) || (verId == null)) {
            Messagebox.show("Cannot reload: Product session is closed!");
            return;
        }

        DocmaNode nd = docmaSess.getNodeById(nodeId);
        if (nd == null) {
            Messagebox.show("Node not found: " + nodeId);
            return;
        }

        nodeTitleLabel.setValue(nd.getTitle());
        prodIdLabel.setValue(storeId);
        prodVerLabel.setValue(verId.toString());
        prodLangLabel.setValue(langCode.toUpperCase());

        if (pubConfId.length() > 0) {
            // If selected configuration does not exist
            if (! pubConfIds.contains(pubConfId)) {
                Messagebox.show("Publication configuration not found: " + pubConfId);
                clearFrame();
                return;
            }
        }
        if (outConfId.length() > 0) {
            // If selected configuration does not exist
            if (! outConfIds.contains(outConfId)) {
                Messagebox.show("Media configuration not found: " + outConfId);
                clearFrame();
                return;
            }
        }

        // String desk_enc = URLEncoder.encode(desk, "UTF-8");
        Iframe frm = (Iframe) getFellow("previewpdffrm");
        String preview_url = "viewPDF.jsp?desk=" + desk +
                             "&nodeid=" + nodeId +
                             "&pub=" + pubConfId + "&out=" + outConfId +
                             "&stamp=" + System.currentTimeMillis();
        frm.setSrc(preview_url);
    }

    private String getSelectedPubConfId()
    {
        return pubConfListbox.getSelectedItem().getValue().toString();
    }

    private String getSelectedOutConfId()
    {
        return outConfListbox.getSelectedItem().getValue().toString();
    }

}
