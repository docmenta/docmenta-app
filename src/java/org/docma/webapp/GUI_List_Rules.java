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

import java.util.Arrays;
import java.util.ListIterator;
import java.util.Set;
import org.docma.app.RuleConfig;
import org.docma.app.RulesManager;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Window;

/**
 *
 * @author MP
 */
public class GUI_List_Rules implements ListitemRenderer
{
    private final MainWindow mainWin;
    private final Listbox       rules_listbox;
    private final ListModelList rules_listmodel;

    public GUI_List_Rules(MainWindow mainWin)
    {
        this.mainWin = mainWin;
        rules_listbox = (Listbox) mainWin.getFellow("RulesConfigListbox");
        rules_listmodel = new ListModelList();
        rules_listmodel.setMultiple(true);
        rules_listbox.setModel(rules_listmodel);
        rules_listbox.setItemRenderer(this);
    }

    public void refresh()
    {
        rules_listmodel.clear();
        loadAll();
    }

    public void loadAll()
    {
        if (rules_listmodel.size() > 0) return;  // list already loaded; only load once within a user session
        
        DocmaWebApplication webapp = GUIUtil.getDocmaWebApplication(mainWin);
        RulesManager rm = webapp.getRulesManager();
        if (rm == null) { 
            return;
        }
        rules_listmodel.addAll(Arrays.asList(rm.getAllRules()));
    }

    public void onNewRule() 
    {
        Window dialog = (Window) mainWin.getPage().getFellow("RuleConfigDialog");
        RuleConfigComposer composer = (RuleConfigComposer) dialog.getAttribute("$composer");
        final RuleConfig new_conf = new RuleConfig();
        composer.newRule(new_conf, new Callback() {
            public void onEvent(String evt) {
                if (RuleConfigComposer.EVENT_OKAY.equals(evt)) {
                    // int ins_pos = -(Collections.binarySearch(rules_listmodel, new_conf)) - 1;
                    rules_listmodel.add(new_conf);
                }
            }
        });
    }
    
    public void onEditRule() 
    {
        Set selection = rules_listmodel.getSelection();
        int sel_cnt = selection.size();
        if (sel_cnt != 1) {
            Messagebox.show("Please select one rule from the list!");
            return;
        }
        final RuleConfig selRule = (RuleConfig) selection.iterator().next();
        // final String oldRuleId = selRule.getId();
        
        Window dialog = (Window) mainWin.getPage().getFellow("RuleConfigDialog");
        RuleConfigComposer composer = (RuleConfigComposer) dialog.getAttribute("$composer");
        composer.editRule(selRule, new Callback() {
            public void onEvent(String evt) {
                if (RuleConfigComposer.EVENT_OKAY.equals(evt)) {
                    
                    // Persist changes
                    DocmaWebApplication webapp = GUIUtil.getDocmaWebApplication(mainWin);
                    RulesManager rm = webapp.getRulesManager();
                    try {
                        rm.saveRule(selRule);
                    } catch (Exception ex) {
                        MessageUtil.showError(mainWin, ex.getLocalizedMessage());
                    }
                    
                    // Update UI
                    int pos = getRuleListPos(selRule);
                    if (pos >= 0) {
                        rules_listmodel.set(pos, selRule);
                    }
                }
            }
        });
    }
    
    public void onDeleteRule() 
    {
        
    }
    
    public void onCopyRule() 
    {
        
    }
    
    public void onEnableRule() 
    {
        
    }
    
    public void onDisableRule() 
    {
        
    }
    
    public void render(Listitem lstm, Object t, int i) throws Exception 
    {
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
}
