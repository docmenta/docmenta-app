/*
 * CompareVersionsDialog.java
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
import java.net.URLEncoder;
import java.text.SimpleDateFormat;

import org.docma.app.*;
import org.docma.app.ui.UserModel;
import org.docma.coreapi.*;
import org.docma.util.*;
import org.zkoss.zul.*;
import org.zkoss.zk.ui.util.Clients;


/**
 *
 * @author MP
 */
public class CompareVersionsDialog extends Window
{
    private SimpleDateFormat dateformat = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    private String desktopId;
    private MainWindow mainWin;
    private String storeId;
    private DocVersionId currentVersion;
    private int currentVersionIndex = -1;
    private String nodeId;
    private String transMode;
    private boolean isHTMLContent;  // distinguish selection of content nodes and section nodes
    private boolean isVersionReadOnly;

    private Label nodeTitleLabel;
    private Label prodIdLabel;
    private Label prodLangLabel;
    private Listbox newVersionBox;
    private Listbox oldVersionBox;
    private Button restoreBtn;
    private Checkbox showImagesBox;

    private DocVersionId[] allVersions = null;


    public void onSelectRevision() throws Exception
    {
        boolean isCurrent = (newVersionBox.getSelectedIndex() == currentVersionIndex);
        restoreBtn.setDisabled(isCurrent || isVersionReadOnly || !isHTMLContent);
        restoreBtn.setVisible(isHTMLContent);
        onChangeSelection();
    }

    public void onRestoreClick() throws Exception
    {
        DocmaSession docmaSess = mainWin.getDocmaSession();
        DocmaSession tempSess = null;
        try {
            String new_value = getNewValue();
            String new_ver_str = getVersionFromValue(new_value);
            DocVersionId new_vid = docmaSess.createVersionId(new_ver_str);
            Date new_rev_date = getRevisionDateFromValue(new_value);

            String target_label;
            if (new_rev_date == null) {
                target_label = "version '" + new_vid + "'";
            } else {
                target_label = "revision '" + dateformat.format(new_rev_date) + "'";
            }
            String msg = "Restore version '" + currentVersion +
                         "' with content of " + target_label + "?";
            if (Messagebox.show(msg, "Restore node?",
                Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION) == Messagebox.OK) {

                // Get new content
                tempSess = docmaSess.createNewSession();
                tempSess.openDocStore(storeId, new_vid);
                if (transMode != null) tempSess.enterTranslationMode(transMode);
                DocmaNode node = tempSess.getNodeById(nodeId);
                if (node == null) {
                    Messagebox.show("Node with ID '" + nodeId +
                                    "' does not exist in version '" + new_vid + "'.");
                    return;
                }
                String new_content;
                if (new_rev_date == null) {
                    new_content = node.getContentString();
                } else {
                    new_content = getNodeRevision(node, new_rev_date);
                }

                // Get current content
                if (! new_vid.equals(currentVersion)) {
                    tempSess.openDocStore(storeId, currentVersion);
                    if (transMode != null) tempSess.enterTranslationMode(transMode);
                    node = tempSess.getNodeById(nodeId);
                }
                if (! (GUIUtil.isEditContentAllowed(node, true) &&
                       GUIUtil.isContentNotLocked(mainWin, tempSess, node, true, false))) {
                    return;
                }
                String current_content = node.getContentString();
                if (! current_content.equals(new_content)) {
                    node.makeRevision();
                    node.setContentString(new_content);
                } else {
                    Messagebox.show("Content of " + target_label +
                                    " is equal to content of version '" + currentVersion + "'!");
                }
                tempSess.closeDocStore();
            }
        } catch (Exception ex) {
            if (DocmaConstants.DEBUG) ex.printStackTrace();
            Messagebox.show("Error: " + ex.getMessage());
        } finally {
            if (tempSess != null) tempSess.closeSession();
        }
    }

    public void onSelectCompare() throws Exception
    {
        onChangeSelection();
    }

    public void onChangeSelection() throws Exception
    {
        String new_value = getNewValue();
        String old_value = getOldValue();
        // if (new_value.equals(old_value)) {
        //     Messagebox.show("Versions are equal!");
        //     return;
        // }

        String new_value_enc = URLEncoder.encode(new_value, "UTF-8");
        String old_value_enc = URLEncoder.encode(old_value, "UTF-8");
        String lang_para = (transMode == null) ? "" : ("&lang=" + transMode);
        Iframe frm = (Iframe) getFellow("comparefrm");
        String compare_url = "diff/compareVersions.jsp?desk=" + desktopId +
                             "&store=" + storeId + lang_para + "&nodeid=" + nodeId +
                             "&new=" + new_value_enc + "&old=" + old_value_enc +
                             "&images=" + showImagesBox.isChecked() +
                             "&stamp=" + System.currentTimeMillis();
        frm.setSrc(compare_url);
    }

    public void onCheckShowImages() throws Exception
    {
        onChangeSelection();
    }

    public void onCloseClick()
    {
        Clients.evalJavaScript("window.close();");
    }

    public static String getNodeRevision(DocmaNode node, Date revdate)
    {
        DocContentRevision[] revs = node.getRevisions();
        if (revs != null) {
            for (int i = revs.length-1; i >= 0; i--) {
                DocContentRevision rev = revs[i];
                if (rev.getDate().equals(revdate)) {
                    return rev.getContentString();
                }
            }
        }
        return null;
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
        currentVersion = docmaSess.getVersionId();
        transMode = docmaSess.getTranslationMode();

        allVersions = docmaSess.listVersions(storeId);

        nodeTitleLabel = (Label) getFellow("CompareVersionsNodeTitleLabel");
        prodIdLabel    = (Label) getFellow("CompareVersionsProductId");
        prodLangLabel  = (Label) getFellow("CompareVersionsProductLanguage");
        newVersionBox  = (Listbox) getFellow("CompareVersionsNewListbox");
        oldVersionBox  = (Listbox) getFellow("CompareVersionsOldListbox");
        restoreBtn     = (Button) getFellow("CompareVersionsRestoreBtn");
        showImagesBox  = (Checkbox) getFellow("CompareVersionsShowImagesBox");

        // Update GUI

        showImagesBox.setChecked(true);  // show images by default
        applyUserSettings(mainWin);
        DocmaNode node = docmaSess.getNodeById(nodeId);
        String title = node.getTitle();
        if (title == null) title = "";
        if (title.length() > 40) {
            title = title.substring(0, 40) + "...";
        }
        nodeTitleLabel.setValue(title);
        prodIdLabel.setValue(storeId);
        String lang = transMode;
        if ((lang == null) || lang.equals("")) {
            lang = "original";
        }
        prodLangLabel.setValue(lang);

        isHTMLContent = node.isHTMLContent();
        DocContentRevision[] revisions = isHTMLContent ? node.getRevisions() : null;

        newVersionBox.getItems().clear();
        oldVersionBox.getItems().clear();
        oldVersionBox.appendItem(" ", "none");

        int listbox_idx = -1;
        for (int i = allVersions.length-1; i >= 0; i--) {
            DocVersionId vid = allVersions[i];
            String vstr = vid.toString();
            boolean isCurrentVersion = vid.equals(currentVersion);

            final String v_value = "version:" + vstr;
            Listitem item1 = newVersionBox.appendItem(vstr, v_value);
            Listitem item2 = oldVersionBox.appendItem(vstr, v_value);
            listbox_idx++;
            if (isCurrentVersion) {
                currentVersionIndex = listbox_idx;
                final String highlight_style = "background-color:#D8D8D8 !important;";
                item1.setStyle(highlight_style);
                item2.setStyle(highlight_style);
            }

            if (isCurrentVersion && (revisions != null)) {
                for (int r = revisions.length-1; r >= 0; r--) {
                    DocContentRevision rev = revisions[r];
                    Date revdate = rev.getDate();
                    String date_str = dateformat.format(revdate);
                    String usr_id = rev.getUserId();
                    UserModel usr_model = mainWin.getUser(usr_id);
                    String usr_name = (usr_model == null) ? usr_id : usr_model.getDisplayName();
                    String label = date_str + "; " + usr_name;
                    String value = "revision:" + revdate.getTime() + "_version:" + vstr;
                    newVersionBox.appendItem(label, value);
                    oldVersionBox.appendItem(label, value);
                    listbox_idx++;
                }
            }
        }
        newVersionBox.setSelectedIndex(currentVersionIndex);
        // newVersionBox.invalidate();   // ZK bug

        // int old_idx;
        // if (allVersions.length == 1) old_idx = new_idx;
        // else old_idx = (new_idx > 0) ? (new_idx - 1) : (new_idx + 1);
        oldVersionBox.setSelectedIndex(0);
        
        try {
            isVersionReadOnly = !GUIUtil.isUpdateVersionAllowed(docmaSess, false);
            onSelectRevision();  // show selection, disable/enable restore button
        } catch (Exception ex) {
            Log.error("Error in CompareVersionsDialog.onSelectRevision(): " + ex.getMessage());
            if (DocmaConstants.DEBUG) ex.printStackTrace();
        }
    }

    public static String getVersionFromValue(String value) throws Exception
    {
        final String VER_PREFIX = "version:";
        int vstart = value.indexOf(VER_PREFIX);
        if (vstart < 0) throw new DocException("Invalid parameter value: " + value);
        return value.substring(vstart + VER_PREFIX.length());
    }

    public static Date getRevisionDateFromValue(String value) throws Exception
    {
        final String REV_PREFIX = "revision:";
        int rstart = value.indexOf(REV_PREFIX);
        if (rstart != 0) return null;

        int rend = value.indexOf('_');
        if (rend < 0) throw new DocException("Invalid parameter value: " + value);
        String timestr = value.substring(REV_PREFIX.length(), rend);
        return new Date(Long.parseLong(timestr));
    }


    private String getNewValue()
    {
        return newVersionBox.getSelectedItem().getValue().toString();
    }

    private String getOldValue()
    {
        return oldVersionBox.getSelectedItem().getValue().toString();
    }

    public void applyUserSettings(MainWindow mainWin)
    {
        String userId = mainWin.getDocmaSession().getUserId();
        UserModel usr = mainWin.getUser(userId);
        String pattern = (usr == null) ? null : usr.getDateFormat();
        if ((pattern == null) || (pattern.trim().equals(""))) {
            pattern = DocmaConstants.DEFAULT_DATE_FORMAT;
        }
        try {
            dateformat.applyPattern(pattern);
        } catch (IllegalArgumentException ex) {
            Log.error("User " + usr.getUserId() + " has invalid date format pattern: " + pattern);
            dateformat.applyPattern(DocmaConstants.DEFAULT_DATE_FORMAT);
        }
    }

}
