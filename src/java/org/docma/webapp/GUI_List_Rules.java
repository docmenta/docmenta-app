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
import org.docma.app.RuleConfig;
import org.docma.app.RulesManager;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
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
        rules_listmodel.addAll(Arrays.asList(rm.listRules()));
    }

    public void onNewRule() 
    {
        Window dialog = (Window) mainWin.getPage().getFellow("ruleConfigDialog");
        RuleConfigComposer composer = (RuleConfigComposer) dialog.getAttribute("$composer");
        final RuleConfig new_conf = new RuleConfig();
        composer.showRuleConfigDialog(new_conf, new Callback() {
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
        
    }
    
    public void onDeleteRule() 
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
    
}
