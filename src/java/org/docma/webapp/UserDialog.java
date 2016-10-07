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
import org.docma.util.Log;
import org.zkoss.zul.*;

import java.util.*;

/**
 *
 * @author MP
 */
public class UserDialog extends Window
{
    public static final String EVENT_OKAY = "onOkay";

    private static final int MODE_NEW = 0;
    private static final int MODE_EDIT = 1;

    private int mode = -1;
    private UserModel user = null; 
    private DocmaSession docmaSess = null;
    private Callback callback = null;

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
        if (hasInvalidInputs(docmaSess)) {
            return;  // keep dialog opened
        }
        try {
            updateModel(user, docmaSess.getUserManager());
            
            // Notify caller that model has been updated
            if (callback != null) {
                callback.onEvent(EVENT_OKAY);
            }
        } catch (Exception ex) {
            Messagebox.show("Error: " + ex.getMessage());
            return;  // keep dialog opened
        }
        setVisible(false);
    }

    public void onCancelClick()
    {
        setVisible(false);   // close dialog
    }

    public void newUser(UserModel usr, DocmaSession docmaSess, Callback callback)
    {
        init();
        mode = MODE_NEW;
        setTitle(label("label.users.dialog.new.title"));
        generalTab.setSelected(true);
        groupsTab.setVisible(false);
        doEditUser(usr, docmaSess, callback);
    }

    public void editUser(UserModel usr, DocmaSession docmaSess, Callback callback)
    {
        init();
        mode = MODE_EDIT;
        setTitle(label("label.users.dialog.edit.title"));
        generalTab.setSelected(true);
        groupsTab.setVisible(true);
        doEditUser(usr, docmaSess, callback);
    }

    private void doEditUser(UserModel usr, DocmaSession docmaSess, Callback callback)
    {
        this.user = usr;
        this.docmaSess = docmaSess;
        this.callback = callback;
        
        init();
        updateGUI(usr); // init dialog fields
        doHighlighted();
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
            // Add item for default language, i.e. if no language has been set.
            // If no language is set, then the browser or OS language is used.
            String txt_default_lang = label("text.users.default_ui_lang");
            guilanguageBox.appendChild(new Listitem(txt_default_lang, ""));

            // Add all supported UI languages to the list.
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
            String txtdefault = label("text.users.default_editor");
            Listitem item = new Listitem(txtdefault + " (" + edName + ")", "");
            editorIdBox.appendChild(item);
            for (String eId : editorIds) {
                edName = webapp.getHelperAppName(eId);
                item = new Listitem(edName, eId);
                editorIdBox.appendChild(item);
            }
        }
        
    }

    private boolean hasInvalidInputs(DocmaSession docmaSess)
    {
        String name = usernameBox.getValue().trim();
        if (name.equals("")) {
            Messagebox.show(label("text.users.enter_username"));
            return true;
        }
        if (name.length() > 40) {
            Messagebox.show(label("text.users.username_too_long"));
            return true;
        }
        if (! name.matches("[A-Za-z][0-9A-Za-z_]*")) {
            Messagebox.show(label("text.users.invalid_username"));
            return true;
        }
        if (mode == MODE_NEW) {
            UserManager um = docmaSess.getUserManager();
            if (um.getUserIdFromName(name) != null) {
                Messagebox.show(label("text.users.username_already_exists"));
                return true;
            }
        }
        String pw1 = password1Box.getValue();
        String pw2 = password2Box.getValue();
        if ((pw1.length() > 0) || (pw2.length() > 0)) {
            if (! pw1.equals(pw2)) {
                Messagebox.show(label("text.users.passwords_differ"));
                return true;
            }
        }
        return false;
    }

    private void updateGUI(UserModel usr)
    {
        usernameBox.setValue(usr.getLoginName());
        lastnameBox.setValue(usr.getLastName());
        firstnameBox.setValue(usr.getFirstName());
        emailBox.setValue(usr.getEmail());

        String lang_code = usr.getGuiLanguage();
        lang_code = (lang_code != null) ? lang_code.trim() : "";
        int langidx = getLangListIndex(lang_code);
        guilanguageBox.setSelectedIndex((langidx >= 0) ? langidx : 0);

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
            String gui_lang = "";
            Object obj = item.getValue();
            if (obj instanceof DocmaLanguage) {
              gui_lang = ((DocmaLanguage) obj).getCode();
            }
            usr.setGuiLanguage(gui_lang);
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
            try {
                usr.update(um);
            } catch (Exception ex) {  
                // if update fails, try to reset fields to old values
                try {
                    usr.load(um);
                } catch (Exception ex2) {
                    Log.warning("Failed to reload user information."); // ex2.printStackTrace();
                }
                throw ex;
            }
        }
    }

    private int getLangListIndex(String lang_code)
    {
        List list = guilanguageBox.getItems();
        for (int i=0; i < list.size(); i++) {
            Listitem item = (Listitem) list.get(i);
            Object obj = item.getValue();
            String item_lang;
            if (obj instanceof DocmaLanguage) {
                item_lang = ((DocmaLanguage) obj).getCode();
            } else {
                item_lang = (obj instanceof String) ? (String) obj : "";
            }
            if (lang_code.equalsIgnoreCase(item_lang)) {
                return i;
            }
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

    private String label(String key, Object... args)
    {
        return GUIUtil.getI18n(this).getLabel(key, args);
    }

}
