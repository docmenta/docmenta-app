/*
 * RuleConfigComposer.java
 * 
 *  Copyright (C) 2016  Manfred Paula, http://www.docmenta.org
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

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.docma.app.DocmaConstants;

import org.docma.app.DocmaSession;
import org.docma.app.RuleConfig;
import org.docma.plugin.LogLevel;
import org.docma.util.Log;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.*;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

/**
 *
 * @author MP
 */
public class RuleConfigComposer extends SelectorComposer<Component> implements EventListener
{
    public static final String EVENT_OKAY = "onOkay";
    
    private static final int MODE_NEW = 0;
    private static final int MODE_EDIT = 1;

    private static final String LOG_LEVEL_BOX_ = "LogLevelBox_";
    private static final String EXEC_CHECK_BOX_ = "ExecCheckBox_";
    private static final String EXEC_SAVE_BOX_ = "ExecSaveBox_";
    private static final String CORRECT_CHECK_BOX_ = "CorrectCheckBox_";
    private static final String CORRECT_SAVE_BOX_ = "CorrectSaveBox_";

    private int mode = -1;
    private RuleConfig ruleConfig = null;  // This is the supplied config object. 
                                           // It is only updated if user clicks OK.
    private RuleConfig tempConfig = null;  // This is a temporary config object.
    private final Map<String, RuleConfig> tmpConfigs = new HashMap<String, RuleConfig>();
    private Callback callback = null;
    private String guiLanguage = "en";
    private int rowIdCounter = 0;
    private final Map<String, Integer> mapCheckToRow = new HashMap<String, Integer>();
    
    @Wire("#RuleConfigDialog") Window ruleDialog;
    @Wire("#RuleConfigIdTextbox") Textbox idBox;
    @Wire("#RuleConfigTitleTextbox") Textbox titleBox;
    @Wire("#RuleConfigClsCombobox") Combobox clsBox;
    @Wire("#RuleConfigArgsTextbox") Textbox argsBox;
    @Wire("#RuleConfigClsHelpBtn") Button clsHelpBtn;
    @Wire("#RuleConfigEnabledCheckbox") Checkbox enabledBox;
    @Wire("#RuleConfigDefStateListbox") Listbox defStateBox;
    @Wire("#RuleConfigScopeAllCheckbox") Checkbox scopeAllBox;
    @Wire("#RuleConfigScopeListbox") Listbox scopeBox;
    @Wire("#RuleConfigChecksGrid") Grid checksGrid;

    public void newRule(RuleConfig ruleConf, Callback callback) 
    {
        this.mode = MODE_NEW;
        this.ruleConfig = ruleConf;
        this.tempConfig = ruleConf;
        this.callback = callback;

        ruleDialog.setTitle(label("rule.config.dialog.new.title"));
        showDialog();
    }
    
    public void editRule(RuleConfig ruleConf, Callback callback) 
    {
        this.mode = MODE_EDIT;
        this.ruleConfig = ruleConf;
        this.tempConfig = ruleConf;
        this.callback = callback;
        
        ruleDialog.setTitle(label("rule.config.dialog.edit.title"));
        showDialog();
    }
    
    @Listen("onClick = #RuleConfigOkayBtn")
    public void onOkayClick()
    {
        if (hasInvalidInput()) {
            return;
        }
        updateModel();
        ruleDialog.setVisible(false);
    }
   
    @Listen("onClick = #RuleConfigCancelBtn")
    public void onCancelClick()
    {
        ruleDialog.setVisible(false);
    }

    @Listen("onChange = #RuleConfigClsCombobox")
    public void onChangeRuleClass()
    {
        updateTempConfig();
        updateGUI();
    }

    @Listen("onCheck = #RuleConfigScopeAllCheckbox")
    public void onCheckScopeAll()
    {
        boolean isAll = scopeAllBox.isChecked();
        if (isAll) {
            scopeBox.getItems().clear();  
            scopeBox.clearSelection();
        } else {
            fillScopeList();
        }
        scopeBox.setDisabled(isAll);
        ruleDialog.invalidate();
    }
    
    @Listen("onSelect = #RuleConfigScopeListbox")
    public void onSelectScope()
    {
        boolean isAll = scopeAllBox.isChecked();
        if (isAll) {
            scopeAllBox.setChecked(false);
            scopeBox.setDisabled(false);
            if (scopeBox.getItemCount() == 0) {
                fillScopeList();
                ruleDialog.invalidate();
            }
        }
    }

    @Listen("onClick = #RuleConfigClsHelpBtn")
    public void onHelpClick() throws Exception
    {
        String clsName = clsBox.getValue().trim();
        Desktop desk = ruleDialog.getDesktop();
        String deskId = desk.getId();
        String help_url = desk.getExecution().encodeURL("viewRuleClsInfo.jsp?desk=" +
                            deskId + "&cls=" + URLEncoder.encode(clsName, "UTF-8"));
        
        String client_action = "window.open('" + help_url + "', " +
          "'_blank', 'width=580,height=450,resizable=yes,scrollbars=yes,location=yes,menubar=yes,status=yes');";
        Clients.evalJavaScript(client_action);
    }
    
    @Override
    public void onEvent(Event evt) throws Exception 
    {
        Component tar = evt.getTarget();
        String tarId = tar.getId();
        String ename = evt.getName();
        if (tarId.startsWith(EXEC_CHECK_BOX_) && ename.equalsIgnoreCase("onCheck")) {
            String rowId = tarId.substring(EXEC_CHECK_BOX_.length());
            Checkbox correctBox = (Checkbox) tar.getFellowIfAny(CORRECT_CHECK_BOX_ + rowId);
            if (correctBox != null) {
                Checkbox execBox = (Checkbox) tar;
                if (execBox.isChecked()) {
                    correctBox.setDisabled(false);
                } else {
                    correctBox.setChecked(false);
                    correctBox.setDisabled(true);
                }
            }
        } else if (tarId.startsWith(EXEC_SAVE_BOX_) && ename.equalsIgnoreCase("onCheck")) {
            String rowId = tarId.substring(EXEC_SAVE_BOX_.length());
            Checkbox correctBox = (Checkbox) tar.getFellowIfAny(CORRECT_SAVE_BOX_ + rowId);
            if (correctBox != null) {
                Checkbox execBox = (Checkbox) tar;
                if (execBox.isChecked()) {
                    correctBox.setDisabled(false);
                } else {
                    correctBox.setChecked(false);
                    correctBox.setDisabled(true);
                }
            }
        }
    }
    
    public String getLongInfo(String clsName)
    {
        return getTemporaryRuleConfig(clsName).getLongInfo(guiLanguage);
    }

    private void updateTempConfig()
    {
        String clsName = clsBox.getValue().trim();
        if (clsName.equals(ruleConfig.getRuleClassName())) {
            tempConfig = ruleConfig;
        } else {
            if (! clsName.equals(tempConfig.getRuleClassName())) {
                tempConfig = getTemporaryRuleConfig(clsName);
            }
        }
    }
    
    private RuleConfig getTemporaryRuleConfig(String clsName)
    {
        RuleConfig rc = tmpConfigs.get(clsName);
        if (rc == null) {
            rc = new RuleConfig();
            rc.setRuleClassName(clsName);
            rc.setTitle(rc.getShortInfo(guiLanguage));
            tmpConfigs.put(clsName, rc);
        }
        return rc;
    }
    
    private boolean hasInvalidInput()
    {
        String ruleId = idBox.getValue().trim();
        if (! ruleId.matches(DocmaConstants.REGEXP_RULE_ID)) {
            MessageUtil.showError(ruleDialog, "rule.config.invalid_id");
            return true;
        }
        String clsName = clsBox.getValue().trim();
        if (clsName.equals("")) {
            MessageUtil.showError(ruleDialog, "rule.config.empty_rule_cls");
            return true;
        }
        updateTempConfig();
        if (tempConfig.getRuleInstance() == null) {
            MessageUtil.showError(ruleDialog, "rule.config.invalid_rule_cls");
            return true;
        }
        try {
            tempConfig.setArgsLine(argsBox.getValue().trim());
        } catch (Exception ex) {
            MessageUtil.showError(ruleDialog, "rule.config.invalid_cls_args", ex.getLocalizedMessage());
            return true;
        }
        return false;
    }
    
    private void showDialog()
    {
        DocmaWebSession webSess = GUIUtil.getDocmaWebSession(ruleDialog);
        guiLanguage = webSess.getCurrentLocale().getLanguage();
        
        DocmaWebApplication docmaApp = GUIUtil.getDocmaWebApplication(ruleDialog);
        clsBox.getItems().clear();
        for (String cn : docmaApp.getRuleClassNames()) {
            clsBox.appendItem(cn);
        }
        
        updateGUI();
        idBox.setReadonly(mode == MODE_EDIT);
        ruleDialog.doHighlighted();
    }
    
    private void fillScopeList()
    {
        DocmaWebSession webSess = GUIUtil.getDocmaWebSession(ruleDialog);
        scopeBox.getItems().clear();
        DocmaSession docmaSess = webSess.getDocmaSession();
        String[] storeIds = docmaSess.listDocStores();
        if (storeIds.length > 10) {
            scopeBox.setHeight("300px");
        } else {
            scopeBox.setHeight(null);  // best fit
        }
        for (String sid : storeIds) {
            scopeBox.appendItem(sid, sid);
        }
        
        // Set selection
        List scopeIds = Arrays.asList(tempConfig.getScope());
        for (Listitem item : scopeBox.getItems()) {
            item.setSelected(scopeIds.contains(item.getValue()));
        }
    }
    
    private void updateModel() 
    {
        ruleConfig.setId(idBox.getValue().trim());
        ruleConfig.setTitle(titleBox.getValue().trim());
        String clsName = clsBox.getValue().trim();
        if (! clsName.equals(ruleConfig.getRuleClassName())) {
            ruleConfig.setRuleClassName(clsName);
        }
        try {
            ruleConfig.setArgsLine(argsBox.getValue().trim());
        } catch (Exception ex) {}  // is caught in hasInvalidInput()
        ruleConfig.setRuleEnabled(enabledBox.isChecked());
        ruleConfig.setDefaultOn(getSelectedValue(defStateBox).equals("on"));
        if (scopeAllBox.isChecked()) {
            ruleConfig.setScopeAll();
        } else {
            List<String> storeIds = new ArrayList<String>();
            for (Listitem item : scopeBox.getSelectedItems()) {
                if (item.isSelected()) {
                    storeIds.add(item.getValue().toString());
                }
            }
            ruleConfig.setScope(storeIds.toArray(new String[storeIds.size()]));
        }
        for (String checkId : ruleConfig.getCheckIds()) {
            Integer rowNum = mapCheckToRow.get(checkId);
            if (rowNum == null) {
                continue;
            }
            Listbox levBox = (Listbox) checksGrid.getFellowIfAny(LOG_LEVEL_BOX_ + rowNum);
            Checkbox execCheck = (Checkbox) checksGrid.getFellowIfAny(EXEC_CHECK_BOX_ + rowNum);
            Checkbox execSave = (Checkbox) checksGrid.getFellowIfAny(EXEC_SAVE_BOX_ + rowNum);
            Checkbox correctCheck = (Checkbox) checksGrid.getFellowIfAny(CORRECT_CHECK_BOX_ + rowNum);
            Checkbox correctSave = (Checkbox) checksGrid.getFellowIfAny(CORRECT_SAVE_BOX_ + rowNum);
            String levName = getSelectedValue(levBox);
            LogLevel lev;
            try {
                lev = LogLevel.valueOf(levName);
            } catch (Exception ex) {
                lev = LogLevel.ERROR;
            }
            ruleConfig.setLogLevel(checkId, lev);
            ruleConfig.setExecuteOnCheck(checkId, execCheck.isChecked());
            ruleConfig.setExecuteOnSave(checkId, execSave.isChecked());
            if (ruleConfig.supportsAutoCorrection(checkId)) {
                if (correctCheck != null) {
                    ruleConfig.setCorrectOnCheck(checkId, correctCheck.isChecked());
                }
                if (correctSave != null) {
                    ruleConfig.setCorrectOnSave(checkId, correctSave.isChecked());
                }
            }
        }
    }
    
    private void updateGUI()
    {
        idBox.setValue(tempConfig.getId());
        titleBox.setValue(tempConfig.getTitle());
        clsBox.setValue(tempConfig.getRuleClassName());
        argsBox.setValue(tempConfig.getArgsLine());
        clsHelpBtn.setDisabled(tempConfig.getRuleInstance() == null);
        enabledBox.setChecked(tempConfig.isRuleEnabled());
        selectListItem(defStateBox, tempConfig.isDefaultOn() ? "on" : "off");
        boolean isScopeAll = tempConfig.isScopeAll();
        scopeAllBox.setChecked(isScopeAll);
        scopeBox.clearSelection();
        if (isScopeAll) {
            scopeBox.getItems().clear();
        } else {
            fillScopeList();
        }
        scopeBox.setDisabled(isScopeAll);
        
        updateChecksGrid();
    }
    
    private void updateChecksGrid()
    {
        String[] checkIds = (tempConfig != null) ? tempConfig.getCheckIds() : null;
        if (checkIds == null) {
            checkIds = new String[0];
        }
        if (checkIds.length > 4) {
            checksGrid.setHeight("200px");
        } else {
            checksGrid.setHeight(null);   // use best fit
        }
        
        // Clear/initialize grid rows 
        Rows gridrows = checksGrid.getRows();
        if (gridrows != null) {
            gridrows.getChildren().clear();
        } else {
            gridrows = new Rows();
            checksGrid.appendChild(gridrows);
        }
        mapCheckToRow.clear();
        
        // Fill grid
        for (String checkId : checkIds) {
            int rowNum = ++rowIdCounter;  // create unique row number
            gridrows.appendChild(buildRow(checkId, String.valueOf(rowNum)));
            mapCheckToRow.put(checkId, rowNum);
        }
        
        ruleDialog.invalidate();  // adapt dialog size
    }
    
    private Row buildRow(String checkId, String rowId) 
    {
        Row r = new Row();
        r.appendChild(new Label(checkId));   // the first column

        LogLevel checkLevel = tempConfig.getLogLevel(checkId);
        Listbox levelList = new Listbox();
        levelList.setId(LOG_LEVEL_BOX_ + rowId);
        levelList.setMold("select");
        levelList.setRows(1);
        Listitem selItem = null;
        for (LogLevel lev : LogLevel.values()) {
            Listitem item = new Listitem(lev.toString(), lev.name());
            levelList.appendChild(item);
            if (lev == checkLevel) {
                selItem = item;
            }
        }
        if (selItem == null) {
            levelList.setSelectedIndex(levelList.getItemCount() - 1);
        } else {
            levelList.selectItem(selItem);
        }
        
        r.appendChild(levelList);   // the second column

        // Create checkboxes for the third column
        boolean supportsCorrection = tempConfig.supportsAutoCorrection(checkId);
        boolean isCheck = tempConfig.isExecuteOnCheck(checkId);
        boolean isSave = tempConfig.isExecuteOnSave(checkId);
        boolean isCorrectOnCheck = supportsCorrection && tempConfig.isCorrectOnCheck(checkId);
        boolean isCorrectOnSave = supportsCorrection && tempConfig.isCorrectOnSave(checkId);
        
        Vbox vb = new Vbox();  // the third column
        
        Checkbox cb1 = new Checkbox(label("rule.config.dialog.exec_on_check"));
        cb1.setId(EXEC_CHECK_BOX_ + rowId);
        cb1.addEventListener("onCheck", this);
        cb1.setChecked(isCheck);
        vb.appendChild(cb1);
        if (supportsCorrection) {
            Checkbox cb = new Checkbox(label("rule.config.dialog.auto_correct"));
            cb.setId(CORRECT_CHECK_BOX_ + rowId);
            cb.setChecked(isCheck && isCorrectOnCheck);
            cb.setDisabled(! isCheck);
            Space space = new Space();
            space.setSpacing("16px");
            Hbox hb = new Hbox();
            hb.appendChild(space);
            hb.appendChild(cb);
            vb.appendChild(hb); 
        }

        Checkbox cb2 = new Checkbox(label("rule.config.dialog.exec_on_save"));
        cb2.setId(EXEC_SAVE_BOX_ + rowId);
        cb2.addEventListener("onCheck", this);
        cb2.setChecked(isSave);
        vb.appendChild(cb2);
        if (supportsCorrection) {
            Checkbox cb = new Checkbox(label("rule.config.dialog.auto_correct"));
            cb.setId(CORRECT_SAVE_BOX_ + rowId);
            cb.setChecked(isSave && isCorrectOnSave);
            cb.setDisabled(! isSave);
            Space space = new Space();
            space.setSpacing("16px");
            Hbox hb = new Hbox();
            hb.appendChild(space);
            hb.appendChild(cb);
            vb.appendChild(hb); 
        }
        r.appendChild(vb);
        return r;
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
    
    private String label(String key, Object... args)
    {
        return GUIUtil.getI18n(ruleDialog).getLabel(key, args);
    }

}
