/*
 * UserDialog.java
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
import org.docma.app.ui.*;
import org.docma.userapi.*;
import org.zkoss.zul.*;

import java.util.*;

/**
 *
 * @author MP
 */
public class UserDialog extends Window
{
    private static final int MODE_NEW = 0;
    private static final int MODE_EDIT = 1;

    private int modalResult = -1;
    private int mode = -1;

    // private Tabbox userTabbox;
    private Tab generalTab;
    private Tab groupsTab;
    private Textbox usernameBox;
    private Textbox lastnameBox;
    private Textbox firstnameBox;
    private Textbox emailBox;
    private Listbox guilanguageBox;
    private Listbox editorIdBox;
    private Textbox dateformatBox;
    private Checkbox quickLinksBox;
    private Textbox password1Box;
    private Textbox password2Box;
    private Listbox membershipBox;
    
    private String[] editorIds = null;


    public void onOkayClick()
    {
        modalResult = GUIConstants.MODAL_OKAY;
        setVisible(false);
    }

    public void onCancelClick()
    {
        modalResult = GUIConstants.MODAL_CANCEL;
        setVisible(false);
    }

    public void setMode_NewUser()
    {
        init();
        mode = MODE_NEW;
        setTitle(GUIUtil.i18(this).getLabel("label.users.dialog.new.title"));
        generalTab.setSelected(true);
        groupsTab.setVisible(false);
    }

    public void setMode_EditUser()
    {
        init();
        mode = MODE_EDIT;
        setTitle(GUIUtil.i18(this).getLabel("label.users.dialog.edit.title"));
        generalTab.setSelected(true);
        groupsTab.setVisible(true);
    }

    public boolean doEditUser(UserModel usr, DocmaSession docmaSess)
    throws Exception
    {
        init();
        updateGUI(usr, docmaSess); // init dialog fields
        do {
            modalResult = -1;
            doModal();
            if (modalResult != GUIConstants.MODAL_OKAY) {
                return false;
            }
            if (hasInvalidInputs(docmaSess)) {
                continue;
            }
            updateModel(usr, docmaSess.getUserManager());
            return true;
        } while (true);
    }

    private void init()
    {
        // userTabbox = (Tabbox) getFellow("UserDialogTabbox");
        generalTab = (Tab) getFellow("UserDialogGeneralTab");
        groupsTab = (Tab) getFellow("UserDialogGroupsTab");
        usernameBox = (Textbox) getFellow("UserLoginNameTextbox");
        lastnameBox = (Textbox) getFellow("UserLastNameTextbox");
        firstnameBox = (Textbox) getFellow("UserFirstNameTextbox");
        emailBox = (Textbox) getFellow("UserEmailTextbox");
        guilanguageBox = (Listbox) getFellow("UserGUILangListbox");
        editorIdBox = (Listbox) getFellow("UserEditorIdListbox");
        quickLinksBox = (Checkbox) getFellow("UserQuickLinksCheckbox");
        dateformatBox = (Textbox) getFellow("UserDateFormatTextbox");
        password1Box = (Textbox) getFellow("UserPasswordTextbox1");
        password2Box = (Textbox) getFellow("UserPasswordTextbox2");
        if (guilanguageBox.getItemCount() == 0) {
            DocmaWebApplication webapp = GUIUtil.getDocmaWebApplication(this);
            DocmaLanguage[] langs = webapp.getGUILanguages();
            for (DocmaLanguage lang : langs) {
                Listitem item = new Listitem();
                String label = lang.getCode() + ": " + lang.getDescription();
                item.setLabel(label);
                item.setValue(lang);
                guilanguageBox.appendChild(item);
            }
        }
        updateEditorIds();
        membershipBox = (Listbox) getFellow("UserMembershipListbox");
    }
    
    private void updateEditorIds()
    {
        DocmaWebApplication webapp = GUIUtil.getDocmaWebApplication(this);
        // webapp.updateEditorIds();
        String[] new_ids = webapp.getContentEditorIds();
        boolean do_update = false;
        if ((editorIds == null) || (editorIds.length != new_ids.length)) {
            do_update = true;
        } else {
            // Check if list has changed
            for (int i=0; i < editorIds.length; i++) {
                if (! editorIds[i].equals(new_ids[i])) {
                    do_update = true;
                    break;
                }
            }
        }
        
        if (do_update) {
            editorIds = new_ids;
            editorIdBox.getItems().clear();
            String defEditor = webapp.getSystemDefaultContentEditor();
            String edName = webapp.getHelperAppName(defEditor);
            Listitem item = new Listitem("Default (" + edName + ")", "");
            editorIdBox.appendChild(item);
            for (String eId : editorIds) {
                edName = webapp.getHelperAppName(eId);
                item = new Listitem(edName, eId);
                editorIdBox.appendChild(item);
            }
        }
        
    }

    private boolean hasInvalidInputs(DocmaSession docmaSess) throws Exception
    {
        String name = usernameBox.getValue().trim();
        if (name.equals("")) {
            Messagebox.show("Please enter a user name!");
            return true;
        }
        if (name.length() > 40) {
            Messagebox.show("User name is too long. Maximum length is 40 characters.");
            return true;
        }
        if (! name.matches("[A-Za-z][0-9A-Za-z_]*")) {
            Messagebox.show("Invalid user name. Allowed characters are ASCII letters and underscore.");
            return true;
        }
        if (mode == MODE_NEW) {
            UserManager um = docmaSess.getUserManager();
            if (um.getUserIdFromName(name) != null) {
                Messagebox.show("A user with this name already exists!");
                return true;
            }
        }
        String pw1 = password1Box.getValue();
        String pw2 = password2Box.getValue();
        if ((pw1.length() > 0) || (pw2.length() > 0)) {
            if (! pw1.equals(pw2)) {
                Messagebox.show("Entered passwords differ!");
                return true;
            }
        }
        return false;
    }

    private void updateGUI(UserModel usr, DocmaSession docmaSess)
    {
        usernameBox.setValue(usr.getLoginName());
        lastnameBox.setValue(usr.getLastName());
        firstnameBox.setValue(usr.getFirstName());
        emailBox.setValue(usr.getEmail());

        String lang_code = usr.getGuiLanguage();
        if ((lang_code == null) || lang_code.trim().equals("")) {
            DocmaWebApplication webapp = GUIUtil.getDocmaWebApplication(this);
            DocmaLanguage lang = webapp.getDefaultGUILanguage();
            lang_code = (lang == null) ? "en" : lang.getCode();
        }
        int langidx = getLangListIndex(lang_code);
        if (langidx >= 0) {
            guilanguageBox.setSelectedIndex(langidx);
        }

        String editorId = usr.getEditorId();
        if (editorId == null) editorId = "";
        int idx = getListIndex(editorIdBox, editorId);
        if (idx < 0) idx = 0;
        editorIdBox.setSelectedIndex(idx);
        quickLinksBox.setChecked(usr.isQuickLinksEnabled());

        String df = usr.getDateFormat();
        if (df.trim().equals("")) df = GUIUtil.i18(this).getLabel("format.lastmodified");
        dateformatBox.setValue(df);
        password1Box.setValue("");
        password2Box.setValue("");
        
        if (mode != MODE_NEW) {
            membershipBox.getItems().clear();
            String[] groupnames = usr.getGroupNames();
            if (groupnames != null) {
                for (String gn : groupnames) {
                    membershipBox.appendItem(gn, gn);
                }
            }
        }
        membershipBox.setDisabled(true);  // only show groups
    }

    private void updateModel(UserModel usr, UserManager um) throws Exception
    {
        usr.setLoginName(usernameBox.getValue());
        usr.setLastName(lastnameBox.getValue());
        usr.setFirstName(firstnameBox.getValue());
        usr.setEmail(emailBox.getValue());
        Listitem item = guilanguageBox.getSelectedItem();
        if (item != null) {
            DocmaLanguage lang = (DocmaLanguage) item.getValue();
            usr.setGuiLanguage(lang.getCode());
        }
        Listitem item2 = editorIdBox.getSelectedItem();
        if (item2 != null) {
            String editorId = (String) item2.getValue();
            usr.setEditorId(editorId);
        }
        usr.setQuickLinksEnabled(quickLinksBox.isChecked());
        usr.setDateFormat(dateformatBox.getValue());
        String pw = password1Box.getValue();
        if (pw.length() > 0) {
            usr.setNewPassword(pw);
        }
        
        // persist changes
        if (mode == MODE_NEW) {
            usr.create(um);
        } else {
            usr.update(um);
        }
    }

    private int getLangListIndex(String lang_code)
    {
        List list = guilanguageBox.getItems();
        for (int i=0; i < list.size(); i++) {
            Listitem item = (Listitem) list.get(i);
            DocmaLanguage lang = (DocmaLanguage) item.getValue();
            if (lang_code.equalsIgnoreCase(lang.getCode())) return i;
        }
        return -1;
    }

    private int getListIndex(Listbox box, String value)
    {
        List list = box.getItems();
        for (int i=0; i < list.size(); i++) {
            Listitem item = (Listitem) list.get(i);
            String v = (String) item.getValue();
            if ((v != null) && v.equalsIgnoreCase(value)) return i;
        }
        return -1;
    }

}
