/*
 * AutoFormatCallDialog.java
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

import org.docma.app.AutoFormatCallImpl;
import org.docma.app.ui.*;
import org.docma.plugin.AutoFormatCall;
import org.zkoss.zul.*;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.OpenEvent;

/**
 *
 * @author MP
 */
public class AutoFormatCallDialog extends Window
{
    private MainWindow mainWin;
    private int modalResult = -1;
    private Combobox clsNameBox;
    private Textbox argumentsBox;
    private Label clsSummaryLabel;
    private Label clsInfoShort;
    private Label clsInfoLabel;
    // private Div clsInfoDiv;
    private Html clsInfoHtml;


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

    public void onSelectClass()
    {
        Comboitem item = clsNameBox.getSelectedItem();
        if (item != null) {
            AutoFormatConfigModel model = (AutoFormatConfigModel) item.getValue();
            // clsNameBox.setValue(model.getAutoFormatClassName());
            showClassInfo(model);
            // clsInfoDiv.setHeight("250px");
        } else {
            // clsNameBox.setValue("");
            hideClassInfo();
        }
    }

    public void onOpenClassCombo(Event evt)
    {
        if (evt instanceof ForwardEvent) {
            evt = ((ForwardEvent) evt).getOrigin();
        }
        if (evt instanceof OpenEvent) {
            OpenEvent oe = (OpenEvent) evt;
            if (! oe.isOpen()) {  // popup is closed, i.e. new class might have been selected
                invalidate();  // required, otherwise dialog window might be smaller than content (ZK bug?)
            }
        }
    }

    public AutoFormatCall createCall(MainWindow mainWin) throws Exception
    {
        this.mainWin = mainWin;
        if (doEdit("", "", mainWin.getAutoFormatConfigs())) {
            return getAutoFormatCall();
        } else {
            return null;
        }
    }

    public AutoFormatCall editCall(AutoFormatCall call, MainWindow mainWin) throws Exception
    {
        this.mainWin = mainWin;
        String call_clsname = call.getClassName();
        String call_args = call.getArgumentsLine();
        if (doEdit(call_clsname, call_args, mainWin.getAutoFormatConfigs())) {
            AutoFormatCall newcall = getAutoFormatCall();
            if (call_clsname.equals(newcall.getClassName()) &&
                call_args.equals(newcall.getArgumentsLine())) {
                return null;  // user clicked okay but nothing changed
            } else {
                return newcall;
            }
        } else {  // user clicked cancel
            return null;
        }
    }

    private boolean doEdit(String clsname, String args, AutoFormatConfigModel[] afconfigs) throws Exception
    {
        init();
        // init dialog fields
        clsNameBox.getItems().clear();
        int sel_idx = -1;
        for (int i=0; i < afconfigs.length; i++) {
            AutoFormatConfigModel afm = afconfigs[i];
            String item_name = afm.getAutoFormatClassName(); // + ": " + afm.getShortInfo();
            // if (lab.length() > 80) lab = lab.substring(0, 80);
            Comboitem item = new Comboitem(item_name);
            item.setValue(afm);
            clsNameBox.appendChild(item);
            if (clsname.equals(item_name)) {
                sel_idx = i;
            }
        }
        if (sel_idx < 0) {
            // clsname is empty string or clsname is not a configured auto-format class
            clsNameBox.setValue(clsname);
            if (clsname.equals("")) hideClassInfo();
            else showClassNotFound(clsname);
        } else {
            clsNameBox.setSelectedIndex(sel_idx);
            showClassInfo(afconfigs[sel_idx]);
        }
        argumentsBox.setValue(args);

        // setHeight("380px");  // initial height of dialog
        do {
            modalResult = -1;
            doModal();
            if (modalResult != GUIConstants.MODAL_OKAY) {
                return false;
            }
            if (hasInvalidInputs()) {
                continue;
            }
            return true;
        } while (true);
    }

    private void init()
    {
        clsNameBox = (Combobox) getFellow("AutoFormatCallClassCombobox");
        argumentsBox = (Textbox) getFellow("AutoFormatCallArgsTextbox");
        clsSummaryLabel = (Label) getFellow("AutoFormatCallSummaryLabel");
        clsInfoShort = (Label) getFellow("AutoFormatCallInfoShort");
        clsInfoLabel = (Label) getFellow("AutoFormatCallInfoLabel");
        // clsInfoDiv = (Div) getFellow("AutoFormatCallInfoDiv");
        clsInfoHtml = (Html) getFellow("AutoFormatCallInfoHtml");
    }

    private boolean hasInvalidInputs() throws Exception
    {
        String name = clsNameBox.getValue().trim();
        if (name.equals("")) {
            Messagebox.show("Please enter an Auto-Format classname!");
            return true;
        }
        String args = argumentsBox.getValue().trim();
        if (args.contains("\"") || args.contains(":") ||
            args.contains("(") || args.contains(")")) {
            Messagebox.show("Invalid argument value. \nFollowing characters are not allowed: ( ) \" :");
            return true;
        }
        if (args.contains("/*") || args.contains("*/")) {
            Messagebox.show("Invalid argument value. \nCharacter sequences /* and */ are not allowed!");
            return true;
        }
        return false;
    }

    private AutoFormatCall getAutoFormatCall()
    {
        return new AutoFormatCallImpl(clsNameBox.getValue().trim(), argumentsBox.getValue().trim());
    }

    private void showClassInfo(AutoFormatConfigModel model)
    {
        // final String SHORT_LABEL = mainWin.i18("label.autoformat.call.shortinfo") + ":";
        // final String LONG_LABEL = mainWin.i18("label.autoformat.call.longinfo") + ":";

        clsSummaryLabel.setVisible(true);
        clsInfoShort.setValue(model.getShortInfo());
        String long_info = model.getLongInfo();
        boolean vis = (long_info.length() > 0);
        clsInfoLabel.setVisible(vis);
        clsInfoHtml.setContent(long_info);
    }

    private void hideClassInfo()
    {
        clsSummaryLabel.setVisible(false);
        clsInfoShort.setValue("");
        clsInfoLabel.setVisible(false);
        clsInfoHtml.setContent("");
    }

    private void showClassNotFound(String clsname)
    {
        clsSummaryLabel.setVisible(false);
        clsInfoShort.setValue("");
        clsInfoLabel.setVisible(false);
        clsInfoHtml.setContent("<span style=\"color:#E80000;\">Class " + clsname +
                               " is not installed!</span>");
    }

}
