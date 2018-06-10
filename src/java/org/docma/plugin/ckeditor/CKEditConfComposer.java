/*
 * CKEditConfComposer.java
 * 
 *  Copyright (C) 2018  Manfred Paula, http://www.docmenta.org
 *   
 *  This file is part of Docmenta. Docmenta is free software: you can 
 *  redistribute it and/or modify it under the terms of the GNU Lesser 
 *  General Public License as published by the Free Software Foundation, 
 *  either version 3 of the License, or (at your option) any later version.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Docmenta.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.docma.plugin.ckeditor;

import org.docma.plugin.web.WebPluginContext;
import org.docma.plugin.web.WebUserSession;
import org.docma.webapp.MessageUtil;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkex.zul.Colorbox;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Doublespinner;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

/**
 *
 * @author MP
 */
public class CKEditConfComposer extends SelectorComposer<Component> 
{
    @Wire Window   CKEditConfDialog;
    @Wire Label    CKEditConfAppPath;
    @Wire Checkbox CKEditConfPastePlainBox;
    @Wire Checkbox CKEditConfIgnoreEmptyParaBox;
    @Wire Checkbox CKEditConfImgAltRequiredBox;
    @Wire Checkbox CKEditConfImgDisableResizeBox;
    @Wire Checkbox CKEditConfDisableNativeSpellcheckBox;
    @Wire Checkbox CKEditConfDisableNativeTableHandlesBox;
    @Wire Checkbox CKEditConfConfirmDialogCancelBox;
    @Wire Listbox  CKEditConfSkinBox;
    @Wire Colorbox CKEditConfUIColorBox;
    @Wire Doublespinner CKEditConfDialogBgOpacityBox;
    @Wire Listbox  CKEditConfToolbarPosBox;
    @Wire Textbox  CKEditConfRemoveButtonsBox;
    @Wire Checkbox CKEditConfHTMLEntitiesBox;
    @Wire Checkbox CKEditConfGreekEntitiesBox;
    @Wire Checkbox CKEditConfLatinEntitiesBox;
    @Wire Checkbox CKEditConfUserEntitiesBox;
    @Wire Listbox  CKEditConfUserEntitiesListbox;
    @Wire Checkbox CKEditConfNumericEntitiesBox;
    @Wire Listbox  CKEditConfNumericEntitiesListbox;
    @Wire Listbox  CKEditConfFigTagListbox;
    @Wire Textbox  CKEditConfFigTagProductsBox;
    @Wire Textbox  CKEditConfFigTagClassBox;

    private CKEditPlugin plugin = null;
    private WebPluginContext plugCtx = null;
    private WebUserSession userSession = null;
    
    void editProps(CKEditPlugin plugin, WebPluginContext ctx, WebUserSession webSess) 
    {
        this.plugin = plugin;
        this.plugCtx = ctx;
        this.userSession = webSess;
        
        try {
            updateGUI();
        } catch (Exception ex) {
            ex.printStackTrace();
            MessageUtil.showException(CKEditConfDialog, ex);
        }
        CKEditConfDialog.doHighlighted();
    }
    
    @Listen("onClick = #CKEditConfSaveBtn")
    public void onOkayClick()
    {
        if (hasInvalidInputs()) {
            return;
        }
        updatePlugin();   // update plugin configuration
        CKEditConfDialog.setVisible(false);
    }

    @Listen("onClick = #CKEditConfCancelBtn")
    public void onCancelClick()
    {
        CKEditConfDialog.setVisible(false);
    }

    @Listen("onClick = #CKEditConfHelpBtn")
    public void onHelpClick()
    {
        String help_url = Labels.getLabel("internal_ckeditor.help_url");
        openHelp(help_url);
    }

    @Listen("onCheck = #CKEditConfUserEntitiesBox")
    public void onCheckUserEntities()
    {
        CKEditConfUserEntitiesListbox.setDisabled(! CKEditConfUserEntitiesBox.isChecked());
    }
    
    @Listen("onCheck = #CKEditConfNumericEntitiesBox")
    public void onCheckNumericEntities()
    {
        CKEditConfNumericEntitiesListbox.setDisabled(! CKEditConfNumericEntitiesBox.isChecked());
    }
    
    @Listen("onSelect = #CKEditConfFigTagListbox")
    public void onSelectFigureTagMode()
    {
        String val = getSelectedValue(CKEditConfFigTagListbox);
        CKEditConfFigTagProductsBox.setDisabled(! val.endsWith("_products"));
    }
    
    // ************ Private methods ***************
    
    private void updateGUI()
    {
        CKEditConfAppPath.setValue(plugin.getRelativeCKAppURI(plugCtx));

        CKEditConfSkinBox.getItems().clear();
        String[] skins = plugin.getCKSkins(plugCtx);
        for (String skinName : skins) {
            CKEditConfSkinBox.appendItem(skinName, skinName);
        }
        if (! selectListItem(CKEditConfSkinBox, plugin.getSkin())) {
            if (skins.length > 0) CKEditConfSkinBox.setSelectedIndex(0);
        }
        CKEditConfPastePlainBox.setChecked(plugin.isPastePlainText());
        CKEditConfIgnoreEmptyParaBox.setChecked(plugin.isIgnoreEmptyPara());
        CKEditConfImgAltRequiredBox.setChecked(plugin.isImgAltRequired());
        CKEditConfImgDisableResizeBox.setChecked(plugin.isImgDisableResizer());
        CKEditConfDisableNativeSpellcheckBox.setChecked(plugin.isDisableNativeSpellChecker());
        CKEditConfDisableNativeTableHandlesBox.setChecked(plugin.isDisableNativeTableHandles());
        CKEditConfConfirmDialogCancelBox.setChecked(! plugin.isDialogNoConfirmCancel());
        CKEditConfUIColorBox.setColor(plugin.getUIColor());
        CKEditConfDialogBgOpacityBox.setText(plugin.getDialogBgOpacity());
        selectListItem(CKEditConfToolbarPosBox, plugin.getToolbarLocation());
        CKEditConfRemoveButtonsBox.setValue(plugin.getRemoveButtons());
        CKEditConfHTMLEntitiesBox.setChecked(plugin.isEntitiesHTML());
        CKEditConfGreekEntitiesBox.setChecked(plugin.isEntitiesGreek());
        CKEditConfLatinEntitiesBox.setChecked(plugin.isEntitiesLatin());
        boolean isUserEntities = plugin.isEntitiesUser();
        CKEditConfUserEntitiesBox.setChecked(isUserEntities);
        selectListItem(CKEditConfUserEntitiesListbox, plugin.isEntitiesUserNumerical() ? "numeric" : "symbolic");
        CKEditConfUserEntitiesListbox.setDisabled(! isUserEntities);
        String numStr = plugin.getEntitiesProcessNumerical();
        boolean isForceNum = numStr.equals("force");
        boolean isNum = isForceNum || numStr.equals("true");
        CKEditConfNumericEntitiesBox.setChecked(isNum);
        selectListItem(CKEditConfNumericEntitiesListbox, isForceNum ? "all" : "other");
        CKEditConfNumericEntitiesListbox.setDisabled(! isNum);
        String figMode = plugin.getFigureTagMode() ? "enabled" : "disabled";
        String figProducts = plugin.getFigureTagProducts();
        String item_val = figMode + ((figProducts == null) ? "" : "_products");
        selectListItem(CKEditConfFigTagListbox, item_val);
        CKEditConfFigTagProductsBox.setValue((figProducts == null) ? "" : figProducts);
        CKEditConfFigTagProductsBox.setDisabled(figProducts == null);
        CKEditConfFigTagClassBox.setValue(plugin.getImgCaptionedClass());
    }
    
    private void updatePlugin()
    {
        String skin = getSelectedValue(CKEditConfSkinBox);
        plugin.setSkin(skin);
        plugin.setPastePlainText(CKEditConfPastePlainBox.isChecked());
        plugin.setIgnoreEmptyPara(CKEditConfIgnoreEmptyParaBox.isChecked());
        plugin.setImgAltRequired(CKEditConfImgAltRequiredBox.isChecked());
        plugin.setImgDisableResizer(CKEditConfImgDisableResizeBox.isChecked());
        plugin.setDisableNativeSpellChecker(CKEditConfDisableNativeSpellcheckBox.isChecked());
        plugin.setDisableNativeTableHandles(CKEditConfDisableNativeTableHandlesBox.isChecked());
        plugin.setDialogNoConfirmCancel(! CKEditConfConfirmDialogCancelBox.isChecked());
        plugin.setUIColor(CKEditConfUIColorBox.getColor());
        plugin.setDialogBgOpacity(CKEditConfDialogBgOpacityBox.getText());
        plugin.setToolbarLocation(getSelectedValue(CKEditConfToolbarPosBox));
        plugin.setRemoveButtons(CKEditConfRemoveButtonsBox.getValue().trim());
        plugin.setEntitiesHTML(CKEditConfHTMLEntitiesBox.isChecked());
        plugin.setEntitiesGreek(CKEditConfGreekEntitiesBox.isChecked());
        plugin.setEntitiesLatin(CKEditConfLatinEntitiesBox.isChecked());
        plugin.setEntitiesUser(CKEditConfUserEntitiesBox.isChecked());
        plugin.setEntitiesUserNumerical("numeric".equals(getSelectedValue(CKEditConfUserEntitiesListbox)));
        boolean isNum = CKEditConfNumericEntitiesBox.isChecked();
        boolean isForce = isNum && "all".equals(getSelectedValue(CKEditConfNumericEntitiesListbox));
        plugin.setEntitiesProcessNumerical(isForce ? "force" : (isNum ? "true" : "false"));
        String figSel = getSelectedValue(CKEditConfFigTagListbox);
        boolean isFig = figSel.startsWith("enabled");
        boolean isProducts = figSel.endsWith("_products");
        String figProducts = isProducts ? CKEditConfFigTagProductsBox.getValue().trim() : null;
        plugin.setFigureTag(isFig, figProducts);
        plugin.setImgCaptionedClass(CKEditConfFigTagClassBox.getValue().trim());
        
        plugin.saveConfiguration(plugCtx);
    }
    
    private boolean hasInvalidInputs()
    {
        return false;   // nothing to check
    }

    private void openHelp(String path)
    {
        String client_action = "window.open('" + path + "', " +
          "'_blank', 'width=850,height=600,resizable=yes,scrollbars=yes,location=yes,menubar=yes,status=yes');";
        Clients.evalJavaScript(client_action);
    }

    private String getSelectedValue(Listbox listbox)
    {
        Listitem item = listbox.getSelectedItem();
        if (item == null) {
            return "";
        }
        Object obj = item.getValue();
        return (obj == null) ? "" : obj.toString();
    }
    
    private boolean selectListItem(Listbox listbox, String itemvalue)
    {
        for (int i=0; i < listbox.getItemCount(); i++) {
            Listitem item = listbox.getItemAtIndex(i);
            Object val = item.getValue();
            if ((val != null) && val.toString().equalsIgnoreCase(itemvalue)) {
                item.setSelected(true);
                return true;
            }
        }
        return false;
    }

}
