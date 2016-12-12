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

import org.docma.app.RuleConfig;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.*;
import org.zkoss.zul.Window;

/**
 *
 * @author MP
 */
public class RuleConfigComposer extends SelectorComposer<Component>
{
    public static final String EVENT_OKAY = "onOkay";
    
    @Wire Window ruleConfigDialog;

    public void showRuleConfigDialog(RuleConfig ruleConfig, Callback callback) 
    {
        ruleConfigDialog.doHighlighted();
    }
    
    @Listen("onClick = #ruleConfigOkayBtn")
    public void onOkayClick()
    {
        ruleConfigDialog.setVisible(false);
    }
   
    @Listen("onClick = #ruleConfigCancelBtn")
    public void onCancelClick()
    {
        ruleConfigDialog.setVisible(false);
    }

}
