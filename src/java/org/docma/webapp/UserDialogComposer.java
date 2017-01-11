/*
 * UserDialogComposer.java
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

import java.util.List;

import org.docma.app.*;
import org.docma.app.ui.*;
import org.docma.userapi.*;
import org.docma.util.Log;
import org.docma.plugin.implementation.PluginTab;
import org.docma.plugin.implementation.WebUserSessionImpl;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.*;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.OpenEvent;
import org.zkoss.zul.*;

/**
 *
 * @author MP
 */
public class UserDialogComposer extends SelectorComposer<Component> 
{
    public static final String EVENT_OKAY = "onOkay";

    private static final int MODE_NEW = 0;
    private static final int MODE_EDIT = 1;

    private int mode = -1;
    private UserModel user = null; 
    private DocmaSession docmaSess = null;
    private Callback callback = null;

    @Wire("#UserDialog") Window userDialog;
    @Wire("#UserDialogOkayBtn") Button okayBtn;
    @Wire("#UserDialogCancelBtn") Button cancelBtn;
    @Wire("#UserDialogTabbox") Tabbox userDialogTabbox;
    @Wire("#UserDialogGeneralTab") Tab generalTab;
    @Wire("#UserDialogGroupsTab") Tab groupsTab;
    @Wire("#UserLoginNameTextbox") Textbox usernameBox;
    @Wire("#UserLastNameTextbox") Textbox lastnameBox;
    @Wire("#UserFirstNameTextbox") Textbox firstnameBox;
    @Wire("#UserEmailTextbox") Textbox emailBox;
    @Wire("#UserGUILangListbox") Listbox guilanguageBox;
    @Wire("#UserEditorIdListbox") Listbox editorIdBox;
    @Wire("#UserTxtEditorIdListbox") Listbox txtEditorIdBox;
    @Wire("#UserDateFormatTextbox") Textbox dateformatBox;
    @Wire("#UserQuickLinksCheckbox") Checkbox quickLinksBox;
    @Wire("#UserPasswordTextbox1") Textbox password1Box;
    @Wire("#UserPasswordTextbox2") Textbox password2Box;
    @Wire("#UserMembershipListbox") Listbox membershipBox;
    
    private String[] editorIds = null;


    @Listen("onClick = #UserDialogOkayBtn")
    public void onOkayClick(Event evt)
    {
        if (hasInvalidInputs(docmaSess)) {
            return;  // keep dialog opened
        }
        try {
            propagateEventToPlugins(evt);
            updateModel(user, docmaSess.getUserManager());
            
            // Notify caller that model has been updated
            if (callback != null) {
                callback.onEvent(EVENT_OKAY);
            }
        } catch (Exception ex) {
            Messagebox.show(ex.getLocalizedMessage());
            return;  // keep dialog opened
        }
        userDialog.setVisible(false);
    }

    @Listen("onClick = #UserDialogCancelBtn")
    public void onCancelClick(Event evt)
    {
        try {
            propagateEventToPlugins(evt);
        } catch (Exception ex) {
            Messagebox.show(ex.getLocalizedMessage());
        }
        userDialog.setVisible(false);   // close dialog
    }

    @Listen("onClose = #UserDialog")
    public void onDialogClose(Event evt)
    {
        try {
            propagateEventToPlugins(evt);
        } catch (Exception ex) {
            Messagebox.show(ex.getLocalizedMessage());
        }
        userDialog.setVisible(false);   // close dialog
        evt.stopPropagation();          // prevent detaching the window
    }
    
    public void newUser(UserModel usr, DocmaSession docmaSess, Callback callback)
    {
        mode = MODE_NEW;
        userDialog.setTitle(label("label.users.dialog.new.title"));
        generalTab.setSelected(true);
        groupsTab.setVisible(false);
        doEditUser(usr, docmaSess, callback);
    }

    public void editUser(UserModel usr, DocmaSession docmaSess, Callback callback)
    {
        mode = MODE_EDIT;
        userDialog.setTitle(label("label.users.dialog.edit.title"));
        generalTab.setSelected(true);
        groupsTab.setVisible(true);
        doEditUser(usr, docmaSess, callback);
    }

    private void doEditUser(UserModel usr, DocmaSession docmaSess, Callback callback)
    {
        this.user = usr;
        this.docmaSess = docmaSess;
        this.callback = callback;
        
        updateGUI(usr); // init dialog fields
        try {
            propagateEventToPlugins("onOpen", userDialog);
        } catch (Exception ex) {
            Log.error(ex.getMessage());
        }
        userDialog.doHighlighted();   // show dialog
    }

    private void initGUILanguages()
    {
        // userTabbox = (Tabbox) getFellow("UserDialogTabbox");
        if (guilanguageBox.getItemCount() == 0) {
            // Add item for default language, i.e. if no language has been set.
            // If no language is set, then the browser or OS language is used.
            String txt_default_lang = label("text.users.default_ui_lang");
            guilanguageBox.appendChild(new Listitem(txt_default_lang, ""));

            // Add all supported UI languages to the list.
            DocmaWebApplication webapp = GUIUtil.getDocmaWebApplication(userDialog);
            DocmaLanguage[] langs = webapp.getGUILanguages();
            for (DocmaLanguage lang : langs) {
                Listitem item = new Listitem();
                String label = lang.getCode() + ": " + lang.getDescription();
                item.setLabel(label);
                item.setValue(lang);
                guilanguageBox.appendChild(item);
            }
        }
    }
    
    private void updateContentEditorIds()
    {
        DocmaWebApplication webapp = GUIUtil.getDocmaWebApplication(userDialog);
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
            
            // Determine HTML source editor
            String srcEditor = webapp.getFileEditorId("html");
            if (srcEditor == null) {
                // If no HTML source editor exists, fall back to plain text editor
                srcEditor = webapp.getFileEditorId("txt");
                if (srcEditor == null) {
                    srcEditor = webapp.getSystemDefaultTextEditor();
                }
            }

            // Create list of available content editors
            String defEditor = webapp.getContentEditorId();  // default content editor
            String edName = webapp.getHelperAppName(defEditor);
            String txtdefault = label("text.users.default_editor");
            Listitem item = new Listitem(txtdefault + " [" + edName + "]", "");
            editorIdBox.appendChild(item);   // default item
            boolean srcEditorIncluded = false;
            for (String eId : editorIds) {
                if (eId.equals(srcEditor)) {
                    srcEditorIncluded = true;
                }
                edName = webapp.getHelperAppName(eId);
                item = new Listitem(edName, eId);
                editorIdBox.appendChild(item);
            }
            
            // Add HTML source editor to the list
            if (! srcEditorIncluded) {
                edName = webapp.getHelperAppName(srcEditor);
                item = new Listitem(edName, srcEditor);
                editorIdBox.appendChild(item);
            }
        }
    }

    private void updateTextEditorIds()
    {
        if (txtEditorIdBox.getItemCount() > 0) {
            return;   // list is already initialized; do nothing
        }
        
        // txtEditorIdBox.getItems().clear();
        DocmaWebApplication webapp = GUIUtil.getDocmaWebApplication(userDialog);
        
        String[] txtExts = docmaSess.getTextFileExtensions();
        if (txtExts.length == 0) {
            txtExts = new String[] { "txt" };
        }
        String[] edIds = webapp.getEditorIds(txtExts);
        
        String defEditor = webapp.getFileEditorId("txt");
        if (defEditor == null) {
            defEditor = webapp.getSystemDefaultTextEditor();
        }
        String edName = webapp.getHelperAppName(defEditor);
        String txtdefault = label("text.users.default_editor");
        Listitem item = new Listitem(txtdefault + " [" + edName + "]", "");
        txtEditorIdBox.appendChild(item);   // default item
        for (String eId : edIds) {
            edName = webapp.getHelperAppName(eId);
            item = new Listitem(edName, eId);
            txtEditorIdBox.appendChild(item);
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
        initGUILanguages();
        updateContentEditorIds();
        updateTextEditorIds();
        
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

        String txtEditorId = usr.getTxtEditorId();
        if (txtEditorId == null) txtEditorId = "";
        int txtIdx = getListIndex(txtEditorIdBox, txtEditorId);
        if (txtIdx < 0) txtIdx = 0;  // default text editor
        txtEditorIdBox.setSelectedIndex(txtIdx);

        String df = usr.getDateFormat();
        if (df.trim().equals("")) {
            df = label("format.lastmodified");
        }
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
        
        addPluginTabs();  // add plugin tabs if not already added
    }

    private void addPluginTabs()
    {
        DocmaWebSession webSess = GUIUtil.getDocmaWebSession(userDialog);
        WebUserSessionImpl plugSess = (WebUserSessionImpl) webSess.getPluginInterface();
        List<PluginTab> utabs = plugSess.getUserDialogTabs();
        if (utabs == null) {
            return;  // no tabs to be added
        }
        for (PluginTab tab_data : utabs) {
            // Add tab if not already added
            plugSess.addTab(userDialogTabbox, tab_data.getTabId(), 
                            tab_data.getTabTitle(), tab_data.getPosition(),
                            tab_data.getZulPath());
        }
    }
    
    private void updateModel(UserModel usr, UserManager um) throws Exception
    {
        usr.setLoginName(usernameBox.getValue());
        usr.setLastName(lastnameBox.getValue());
        usr.setFirstName(firstnameBox.getValue());
        usr.setEmail(emailBox.getValue());

        // Update GUI language and date format
        Listitem item = guilanguageBox.getSelectedItem();
        if (item != null) {
            String gui_lang = "";
            Object obj = item.getValue();
            if (obj instanceof DocmaLanguage) {
              gui_lang = ((DocmaLanguage) obj).getCode();
            }
            usr.setGuiLanguage(gui_lang);
        }
        usr.setDateFormat(dateformatBox.getValue());
        
        // Update content editor and quick links setting
        Listitem item2 = editorIdBox.getSelectedItem();
        if (item2 != null) {
            usr.setEditorId((String) item2.getValue());
        }
        usr.setQuickLinksEnabled(quickLinksBox.isChecked());

        // Update text editor setting
        Listitem item3 = txtEditorIdBox.getSelectedItem();
        if (item3 != null) {
            usr.setTxtEditorId((String) item3.getValue());
        }
        
        // Change password?
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
        return GUIUtil.getI18n(userDialog).getLabel(key, args);
    }

    private void propagateEventToPlugins(String evtName, Component evtTarget) 
    {
        DocmaWebSession webSess = GUIUtil.getDocmaWebSession(userDialog);
        WebUserSessionImpl plugSess = (WebUserSessionImpl) webSess.getPluginInterface();
        
        // Following call may throw a runtime exception
        plugSess.propagateUserDialogEventToPlugins(evtName, evtTarget);
    }

    private void propagateEventToPlugins(Event evt) 
    {
        DocmaWebSession webSess = GUIUtil.getDocmaWebSession(userDialog);
        WebUserSessionImpl plugSess = (WebUserSessionImpl) webSess.getPluginInterface();
        
        // Following call may throw a runtime exception, to prevent closing of 
        // the user dialog 
        plugSess.propagateUserDialogEventToPlugins(evt);
    }

}
