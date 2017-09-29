/*
 * ReferenceDialogComposer.java
 * 
 *  Copyright (C) 2017  Manfred Paula, http://www.docmenta.org
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

import java.util.*;

import org.docma.coreapi.*;
import org.docma.app.*;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.*;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.*;
import org.zkoss.zul.*;

/**
 *
 * @author MP
 */
public class ReferenceDialogComposer extends SelectorComposer<Component>
{
    public static final String EVENT_OKAY = "onOkay";
    
    private static final int MODE_NEW_SECTION_REF = 0;
    private static final int MODE_EDIT_SECTION_REF = 1;
    private static final int MODE_NEW_CONTENT_REF = 2;
    private static final int MODE_EDIT_CONTENT_REF = 3;
    private static final int MODE_NEW_IMAGE_REF = 10;
    private static final int MODE_EDIT_IMAGE_REF = 11;

    @Wire("#ReferenceDialog") Window referenceDialog;
    @Wire("#RefTitleTextbox") Textbox titleBox;
    @Wire("#RefAliasLabel") Label aliasLabel;
    @Wire("#RefAliasCombobox") Combobox aliasBox;
    @Wire("#RefApplicTextbox") Combobox applicBox;

    private int mode = -1;
    private Callback callback = null;
    private final List tempList = new ArrayList(1000);
    private List all_aliases = null;
    private DocmaSession docmaSess;
    private String oldApplic = "";

    public void setMode_NewSectionRef() throws Exception
    {
        this.mode = MODE_NEW_SECTION_REF;
        DocI18n i18 = GUIUtil.getI18n(referenceDialog);
        referenceDialog.setTitle(i18.getLabel("label.sectionref.dialog.new.title"));
        aliasLabel.setValue(i18.getLabel("label.sectionref.target"));
    }

    public void setMode_EditSectionRef() throws Exception
    {
        this.mode = MODE_EDIT_SECTION_REF;
        DocI18n i18 = GUIUtil.getI18n(referenceDialog);
        referenceDialog.setTitle(i18.getLabel("label.sectionref.dialog.edit.title"));
        aliasLabel.setValue(i18.getLabel("label.sectionref.target"));
    }

    public void setMode_NewContentRef() throws Exception
    {
        this.mode = MODE_NEW_CONTENT_REF;
        DocI18n i18 = GUIUtil.getI18n(referenceDialog);
        referenceDialog.setTitle(i18.getLabel("label.contentref.dialog.new.title"));
        aliasLabel.setValue(i18.getLabel("label.contentref.target"));
    }

    public void setMode_EditContentRef() throws Exception
    {
        this.mode = MODE_EDIT_CONTENT_REF;
        DocI18n i18 = GUIUtil.getI18n(referenceDialog);
        referenceDialog.setTitle(i18.getLabel("label.contentref.dialog.edit.title"));
        aliasLabel.setValue(i18.getLabel("label.contentref.target"));
    }

    public void setMode_NewImageRef() throws Exception
    {
        this.mode = MODE_NEW_IMAGE_REF;
        DocI18n i18 = GUIUtil.getI18n(referenceDialog);
        referenceDialog.setTitle(i18.getLabel("label.imageref.dialog.new.title"));
        aliasLabel.setValue(i18.getLabel("label.imageref.target"));
    }

    public void setMode_EditImageRef() throws Exception
    {
        this.mode = MODE_EDIT_IMAGE_REF;
        DocI18n i18 = GUIUtil.getI18n(referenceDialog);
        referenceDialog.setTitle(i18.getLabel("label.imageref.dialog.edit.title"));
        aliasLabel.setValue(i18.getLabel("label.imageref.target"));
    }

    @Listen("onClick = #RefDialogOkayBtn")
    public void onOkayClick()
    {
        if (hasInvalidInputs()) {
            return;  // keep dialog opened
        }
        if (callback != null) {
            try {
                callback.onEvent(EVENT_OKAY);
            } catch (Exception ex) {
                Messagebox.show(ex.getLocalizedMessage());
                return;  // keep dialog opened
            }
        }
        referenceDialog.setVisible(false);  // close dialog
    }

    @Listen("onClick = #RefDialogCancelBtn")
    public void onCancelClick()
    {
        referenceDialog.setVisible(false);  // close dialog
    }

    public void doEdit(DocmaNode docmaNode, DocmaSession docmaSess, Callback callback) throws Exception
    {
        this.docmaSess = docmaSess;
        this.callback = callback;
        this.all_aliases = null;
        
        updateGUI(docmaNode);  // if docmaNode is null, then fields are cleared
        referenceDialog.doHighlighted();
    }

    private void updateGUI(DocmaNode docmaNode)
    {
        // Init applicability values
        applicBox.getItems().clear();
        String[] applicVals = docmaSess.getDeclaredApplics();
        for (String appl : applicVals) {
            applicBox.appendItem(appl);
        }
        
        if (docmaNode != null) {  // edit existing node
            titleBox.setValue(docmaNode.getTitle());
            aliasBox.setValue(docmaNode.getReferenceTarget());
            applicBox.setValue(docmaNode.getApplicability());
        } else {   // new node
            titleBox.setValue("");
            aliasBox.setValue("");
            applicBox.setValue("");
        }
    }

    public void updateModel(DocmaNode docmaNode, DocmaSession docmaSess) throws Exception
    {
        boolean local_trans = !docmaSess.runningTransaction();
        if (local_trans) docmaSess.startTransaction();
        try {
            docmaNode.setTitle(titleBox.getValue().trim());
            docmaNode.setReferenceTarget(aliasBox.getValue().trim());
            docmaNode.setApplicability(applicBox.getValue().trim());
            if (local_trans) docmaSess.commitTransaction();
        } catch (Exception ex) {
            if (local_trans) docmaSess.rollbackTransaction();
            // docmaNode.refresh();
            Messagebox.show("Error during transaction: " + ex.getMessage());
        }
    }

    @Listen("onOpen = #RefApplicTextbox")
    public void onOpenApplicItems(Event evt) 
    {
        if (evt instanceof OpenEvent) {
            OpenEvent oevt = (OpenEvent) evt;
            if (oevt.isOpen()) {
                // Save the current applic value in oldApplic. See onSelectApplic().
                Object value = oevt.getValue(); // combo_box.getValue();
                oldApplic = (value == null) ? "" : value.toString();
            }
        }
    }

    @Listen("onSelect = #RefApplicTextbox")
    public void onSelectApplic() 
    {
        Comboitem item = applicBox.getSelectedItem();
        if (item == null) {
            return;  // no item selected because user has manually edited text field
        }
        String value = item.getLabel();  // the value that user has selected from the drop-down list
        
        // If applic field is not empty, then append the selected item to the current value.
        // See onOpenApplicItems().
        String oldVal = (oldApplic == null) ? "" : oldApplic.trim();
        if (! oldVal.equals("")) {
            if (oldVal.endsWith("|") || oldVal.endsWith(",") || oldVal.endsWith("-")) {
                applicBox.setValue(oldVal + " " + value);
            } else {
                applicBox.setValue(oldVal + " | " + value);
            }
        }
    }

    @Listen("onChanging = #RefAliasCombobox")
    public void onChangeRefAlias(Event evt) throws Exception
    {
        final String MORE = "...";
        String val = getInputValue(evt);
        if (val != null) {
            tempList.clear();
            if (val.equals(MORE)) {
                return;  // do nothing; user has reached the end of the list (more entry)
            }
            if (val.length() > 0) {
                DocmaAppUtil.listValuesStartWith(val, getAllAliases(), tempList);
            }
            int item_cnt = aliasBox.getItemCount();
            if ((item_cnt > 0) && tempList.size() == 1) {  // exact match
                return;  // do nothing; user is scrolling through the list
            }
            aliasBox.getItems().clear();
            int sz = tempList.size();
            int max = (sz > 20) ? 20 : sz;
            for (int i=0; i < max; i++) {
                aliasBox.appendItem((String) tempList.get(i));
            }
            if (sz > max) {
                aliasBox.appendItem(MORE);
            }
        }
    }

    private String getInputValue(Event evt)
    {
        if (evt instanceof ForwardEvent) {
            evt = ((ForwardEvent) evt).getOrigin();
        }
        if (evt instanceof InputEvent) {
            InputEvent ievt = (InputEvent) evt;
            String val = ievt.getValue(); // customTitlePage1Box.getValue().trim();
            return (val == null) ? "" : val.trim();
        } else {
            return null;
        }
    }

    private List getAllAliases()
    {
        if (all_aliases == null) {
            if ((mode == MODE_EDIT_CONTENT_REF) || (mode == MODE_NEW_CONTENT_REF)) {
                all_aliases = docmaSess.listHTMLContentAliases();
            } else
            if ((mode == MODE_EDIT_SECTION_REF) || (mode == MODE_NEW_SECTION_REF)) {
                all_aliases = docmaSess.listSectionAliases();
            } else
            if ((mode == MODE_EDIT_IMAGE_REF) || (mode == MODE_NEW_IMAGE_REF)) {
                all_aliases = docmaSess.listImageAliases();
            }
        }
        return all_aliases;
    }

    private boolean hasInvalidInputs()
    {
        String alias = aliasBox.getValue().trim();
        if (alias.length() == 0) {
            Messagebox.show("Please enter a target alias!");
            return true;
        }
        return false;
    }

}
