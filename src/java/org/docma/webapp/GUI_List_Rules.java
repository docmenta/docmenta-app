/*
 * GUI_List_Rules.java
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.docma.app.RuleConfig;
import org.docma.app.RulesManager;
import org.docma.plugin.LogLevel;
import org.docma.plugin.web.ButtonType;
import org.docma.plugin.web.UIEvent;
import org.docma.plugin.web.UIListener;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;

import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Space;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

/**
 *
 * @author MP
 */
public class GUI_List_Rules implements ListitemRenderer
{
    private final MainWindow mainWin;
    private final Listbox       rules_listbox;
    private final ListModelList<RuleConfig> rules_listmodel;
    
    private long listLoadTime = 0;


    public GUI_List_Rules(MainWindow mainWin)
    {
        this.mainWin = mainWin;
        rules_listbox = (Listbox) mainWin.getFellow("RulesConfigListbox");
        rules_listmodel = new ListModelList<RuleConfig>();
        rules_listmodel.setMultiple(true);
        rules_listbox.setModel(rules_listmodel);
        rules_listbox.setItemRenderer(this);
    }

    public void refresh()
    {
        rules_listmodel.clear();
        
        DocmaWebApplication webapp = GUIUtil.getDocmaWebApplication(mainWin);
        RulesManager rm = webapp.getRulesManager();
        if (rm == null) { 
            return;
        }
        rules_listmodel.addAll(Arrays.asList(rm.getAllRules()));
        listLoadTime = System.currentTimeMillis();
    }
    
    public void onShowList()
    {
        // Load list if it has not been loaded yet, or if last loading was
        // more than 3 minutes ago.
        if ((listLoadTime <= 0) || ((System.currentTimeMillis() - listLoadTime) > (1000 * 60 * 3))) {
            refresh();
        }
    }

    public void onNewRule() 
    {
        DocmaWebApplication webApp = GUIUtil.getDocmaWebApplication(mainWin);
        showNewRuleDialog(webApp.getRulesManager().createTransientRule());
    }
    
    public void onEditRule() 
    {
        Set<RuleConfig> selection = rules_listmodel.getSelection();
        int sel_cnt = selection.size();
        if (sel_cnt != 1) {
            MessageUtil.showInfo(mainWin, "rule.config.list.select_one");
            return;
        }
        final RuleConfig selRule = selection.iterator().next();
        final String oldRuleId = selRule.getId();
        
        Window dialog = (Window) mainWin.getPage().getFellow("RuleConfigDialog");
        RuleConfigComposer composer = (RuleConfigComposer) dialog.getAttribute("$composer");
        composer.editRule(selRule, new Callback() {
            public void onEvent(String evt) {
                if (RuleConfigComposer.EVENT_OKAY.equals(evt)) {
                    boolean isNew = !oldRuleId.equals(selRule.getId());
                    
                    // Persist changes
                    persistRule(selRule);
                    if (isNew) {
                        deleteRules(oldRuleId);
                        refresh();  // Update UI: refresh complete list
                    } else {
                        // Update UI: refresh list item 
                        int pos = getRuleListPos(selRule);
                        if (pos >= 0) {
                            rules_listmodel.set(pos, selRule);
                        }
                    }
                }
            }
        });
    }
    
    public void onDeleteRule() 
    {
        Set<RuleConfig> selection = rules_listmodel.getSelection();
        int sel_cnt = selection.size();
        if (sel_cnt < 1) {
            MessageUtil.showInfo(mainWin, "rule.config.list.select_one_or_more");
            return;
        }
        final List<String> deleteIds = new ArrayList<String>();
        for (RuleConfig rc : selection) {
            deleteIds.add(rc.getId());
        }
        String msgKey;
        String[] args = new String[1];
        if (sel_cnt == 1) {
            msgKey = "rule.config.list.confirm_delete_rule"; 
            args[0] = deleteIds.get(0);
        } else {
            msgKey = "rule.config.list.confirm_delete_count"; 
            args[0] = String.valueOf(sel_cnt);
        }
        MessageUtil.showYesNoQuestion(mainWin, "rule.config.list.confirm_delete_title", msgKey, args, 
            new UIListener() {
                public void onEvent(UIEvent evt) {
                    if (ButtonType.YES.equals(evt.getButtonType())) {
                        // Delete selected rules
                        deleteRules(deleteIds);
                        
                        // Update UI
                        refresh();
                    }
                }
            }
        );
    }
    
    public void onCopyRule() throws Exception
    {
        Set<RuleConfig> selection = rules_listmodel.getSelection();
        int sel_cnt = selection.size();
        if (sel_cnt != 1) {
            MessageUtil.showInfo(mainWin, "rule.config.list.select_one");
            return;
        }
        RuleConfig selRule = selection.iterator().next();
        RuleConfig copyRule = (RuleConfig) selRule.clone();
        copyRule.setId("");
        showNewRuleDialog(copyRule);
    }
    
    public void onEnableRule() 
    {
        setRuleState(true);
    }
    
    public void onDisableRule() 
    {
        setRuleState(false);
    }
    
    public void render(Listitem item, Object data, int index) throws Exception 
    {
        if (! (data instanceof RuleConfig)) {
            return;
        }
        
        RuleConfig conf = (RuleConfig) data;
        Listcell c1 = new Listcell(conf.getId());
        c1.setStyle("font-weight:bold;");
        Listcell c2 = new Listcell(conf.getTitle());

        boolean enabled = conf.isRuleEnabled();    
        String state = enabled ? "rule.enabled" : "rule.disabled";
        String onOff = conf.isDefaultOn() ? "rule.state_on" : "rule.state_off";
        Listcell c3 = new Listcell();
        if (enabled) {
            c3.setStyle("color:#F0F0F0;background-color:#00AA00;");
        }
        Label stateLab = new Label(i18n(state));
        stateLab.setStyle("font-weight:bold;");
        Label onOffLab = new Label(i18n(onOff));
        onOffLab.setStyle("font-weight:bold;");
        Hbox hbox = new Hbox();
        hbox.setSpacing("4px");
        hbox.appendChild(new Label(i18n("rule.default_state") + ": "));
        hbox.appendChild(onOffLab);
        Vbox vbox = new Vbox();
        vbox.appendChild(stateLab);
        vbox.appendChild(hbox);
        c3.appendChild(vbox);
        
        Listcell c4 = new Listcell();
        Vbox vb = new Vbox();
        vb.setHflex("1");
        vb.setAlign("stretch");
        for (String checkid : conf.getCheckIds()) {
            boolean isCheck = conf.isExecuteOnCheck(checkid);
            boolean isSave = conf.isExecuteOnSave(checkid);
            if (! (isCheck || isSave)) {
                continue;
            }
            Hbox hb1 = new Hbox();
            hb1.setHflex("1");
            Label lab1 = new Label(checkid + ":");
            lab1.setStyle("font-weight:bold;");
            lab1.setHflex("1");
            LogLevel lev = conf.getLogLevel(checkid);
            Label lab2 = new Label("[ " + lev + " ]");
            String css;
            if (lev == LogLevel.ERROR) {
                css = "color:#AA0000;";   // red color
            } else if (lev == LogLevel.WARNING) {
                css = "color:#0000AA;";   // blue color
            } else {
                css = "color:#000000;";
            }
            lab2.setStyle(css);
            lab2.setHflex("min");
            hb1.appendChild(lab1);
            hb1.appendChild(lab2);
            vb.appendChild(hb1);
            
            boolean supportsAC = conf.supportsAutoCorrection(checkid);
            String setting = "";
            if (isCheck) {
                setting = i18n("rule.config.dialog.exec_on_check");
                if (supportsAC) {
                    String ac = conf.isCorrectOnCheck(checkid) ? "rule.state_on" : "rule.state_off";
                    setting += " (" + i18n("rule.config.dialog.auto_correct") + ": " + i18n(ac) + ")";
                }
            }
            if (isSave) {
                setting += (isCheck ? ", " : "") + i18n("rule.config.dialog.exec_on_save");
                if (supportsAC) {
                    String ac = conf.isCorrectOnSave(checkid) ? "rule.state_on" : "rule.state_off";
                    setting += " (" + i18n("rule.config.dialog.auto_correct") + ": " + i18n(ac) + ")";
                }
            }
            Hbox hb2 = new Hbox();
            Space spc = new Space();
            spc.setSpacing("12px");
            hb2.appendChild(spc);
            hb2.appendChild(new Label(setting));
            vb.appendChild(hb2);
        }
        c4.appendChild(vb);
        
        Listcell c5;
        if (conf.isScopeAll()) {
            c5 = new Listcell(i18n("rule.scope_all"));
            c5.setStyle("font-style:italic; text-align:center;");
        } else {
            StringBuilder sb = new StringBuilder();
            for (String sid : conf.getScope()) {
                if (sb.length() > 0) {
                    sb.append(", ");
                    if (sb.length() > 300) {
                        sb.append("...");
                        break;
                    }
                }
                sb.append(sid);
            }
            c5 = new Listcell(sb.toString());
        }

        item.appendChild(c1);
        item.appendChild(c2);
        item.appendChild(c3);
        item.appendChild(c4);
        item.appendChild(c5);
        item.addForward("onDoubleClick", "mainWin", "onEditRule");
    }

    private void showNewRuleDialog(final RuleConfig newConf)
    {
        Window dialog = (Window) mainWin.getPage().getFellow("RuleConfigDialog");
        RuleConfigComposer composer = (RuleConfigComposer) dialog.getAttribute("$composer");
        composer.newRule(newConf, new Callback() {
            public void onEvent(String evt) {
                if (RuleConfigComposer.EVENT_OKAY.equals(evt)) {
                    // Persist new rule
                    boolean ok = persistRule(newConf);
                    refresh();
                    // if (ok) {
                    //     // Update UI (add new rule to list)
                    //     int ins_pos = -(Collections.binarySearch(rules_listmodel, new_conf)) - 1;
                    //     rules_listmodel.add(ins_pos, new_conf);
                    // } else {
                    //     refresh();
                    // }
                }
            }
        });
    }
    
    private void setRuleState(final boolean enabled)
    {
        Set<RuleConfig> selection = rules_listmodel.getSelection();
        int sel_cnt = selection.size();
        if (sel_cnt < 1) {
            MessageUtil.showInfo(mainWin, "rule.config.list.select_one_or_more");
            return;
        }
        final List<String> ruleIds = new ArrayList<String>();
        for (RuleConfig rc : selection) {
            ruleIds.add(rc.getId());
        }
        String msgKey;
        String[] args = new String[1];
        if (sel_cnt == 1) {
            msgKey = enabled ? "rule.config.list.confirm_enable_rule" : "rule.config.list.confirm_disable_rule"; 
            args[0] = ruleIds.get(0);
        } else {
            msgKey = enabled ? "rule.config.list.confirm_enable_count" : "rule.config.list.confirm_disable_count"; 
            args[0] = String.valueOf(sel_cnt);
        }
        String titleKey = enabled ? "rule.config.list.confirm_enable_title" : "rule.config.list.confirm_disable_title";
        MessageUtil.showYesNoQuestion(mainWin, titleKey, msgKey, args, 
            new UIListener() {
                public void onEvent(UIEvent evt) {
                    if (ButtonType.YES.equals(evt.getButtonType())) {
                        // Update state of selected rules
                        changeRuleState(ruleIds, enabled);
                        
                        // Update UI
                        refresh();
                    }
                }
            }
        );
        
    }
    
    private void changeRuleState(List<String> ruleIds, boolean enabled)
    {
        DocmaWebApplication webapp = GUIUtil.getDocmaWebApplication(mainWin);
        RulesManager rm = webapp.getRulesManager();
        String err_msg = null;
        int err_cnt = 0;
        for (String rid : ruleIds) {
            RuleConfig rc = rm.getRule(rid);
            rc.setRuleEnabled(enabled);
            try {
                rm.saveRule(rc);
            } catch (Exception ex) {
                if (err_msg == null) {
                    err_msg = ex.getLocalizedMessage();
                }
                err_cnt++;
            }
        }
        if (err_msg != null) {
            if (err_cnt > 1) {
                err_msg += " ... \nUpdate of " + err_cnt + " rules failed!";
            }
            MessageUtil.showError(mainWin, err_msg);
        }
    }
    
    private void deleteRules(String... ruleIds) 
    {
        deleteRules(Arrays.asList(ruleIds));
    }
    
    private void deleteRules(List<String> ruleIds)
    {
        DocmaWebApplication webapp = GUIUtil.getDocmaWebApplication(mainWin);
        RulesManager rm = webapp.getRulesManager();
        String err_msg = null;
        int err_cnt = 0;
        for (String rid : ruleIds) {
            try {
                rm.deleteRule(rid);
            } catch (Exception ex) {
                if (err_msg == null) {
                    err_msg = ex.getLocalizedMessage();
                }
                err_cnt++;
            }
        }
        if (err_msg != null) {
            if (err_cnt > 1) {
                err_msg += " ... \nDeletion of " + err_cnt + " rules failed!";
            }
            MessageUtil.showError(mainWin, err_msg);
        }
    }
    
    private boolean persistRule(RuleConfig ruleConf)
    {
        DocmaWebApplication webapp = GUIUtil.getDocmaWebApplication(mainWin);
        RulesManager rm = webapp.getRulesManager();
        try {
            rm.saveRule(ruleConf);
            return true;
        } catch (Exception ex) {
            String err_msg = ex.getLocalizedMessage();
            try {
                rm.loadRule(ruleConf.getId());
            } catch (Exception ex2) {
                err_msg += " \n" + ex2.getLocalizedMessage();
            }
            MessageUtil.showError(mainWin, err_msg);
            return false;
        }
    }
    
    private int getRuleListPos(RuleConfig ruleConf)
    {
        for (int i = 0; i < rules_listmodel.size(); i++) {
            RuleConfig rc = (RuleConfig) rules_listmodel.get(i);
            if (rc == ruleConf) {
                return i;
            }
        }
        return -1;
    }
    
    private String i18n(String key) 
    {
        return mainWin.i18n(key);
    }

}
