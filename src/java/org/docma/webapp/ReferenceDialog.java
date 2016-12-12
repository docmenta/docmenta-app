/*
 * ReferenceDialog.java
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

import java.util.*;

import org.docma.coreapi.*;
import org.docma.app.*;
import org.zkoss.zul.*;
import org.zkoss.zk.ui.event.*;

/**
 *
 * @author MP
 */
public class ReferenceDialog extends Window
{
    private static final int MODE_NEW_SECTION_REF = 0;
    private static final int MODE_EDIT_SECTION_REF = 1;
    private static final int MODE_NEW_CONTENT_REF = 2;
    private static final int MODE_EDIT_CONTENT_REF = 3;
    private static final int MODE_NEW_IMAGE_REF = 10;
    private static final int MODE_EDIT_IMAGE_REF = 11;

    private int modalResult = -1;
    private int mode = -1;

    private Textbox titleBox;
    private Label aliasLabel;
    private Combobox aliasBox;
    private Textbox applicBox;

    private boolean initialized = false;
    private List tempList = new ArrayList(1000);
    private List all_aliases = null;
    private DocmaSession docmaSess;

    private void init()
    {
        if (! initialized) {
            titleBox = (Textbox) getFellow("RefTitleTextbox");
            aliasLabel = (Label) getFellow("RefAliasLabel");
            aliasBox = (Combobox) getFellow("RefAliasCombobox");
            applicBox = (Textbox) getFellow("RefApplicTextbox");
            initialized = true;
        }
    }

    public void setMode_NewSectionRef() throws Exception
    {
        init();
        this.mode = MODE_NEW_SECTION_REF;
        DocmaI18 i18 = GUIUtil.i18(this);
        setTitle(i18.getLabel("label.sectionref.dialog.new.title"));
        aliasLabel.setValue(i18.getLabel("label.sectionref.target"));
    }

    public void setMode_EditSectionRef() throws Exception
    {
        init();
        this.mode = MODE_EDIT_SECTION_REF;
        DocmaI18 i18 = GUIUtil.i18(this);
        setTitle(i18.getLabel("label.sectionref.dialog.edit.title"));
        aliasLabel.setValue(i18.getLabel("label.sectionref.target"));
    }

    public void setMode_NewContentRef() throws Exception
    {
        init();
        this.mode = MODE_NEW_CONTENT_REF;
        DocmaI18 i18 = GUIUtil.i18(this);
        setTitle(i18.getLabel("label.contentref.dialog.new.title"));
        aliasLabel.setValue(i18.getLabel("label.contentref.target"));
    }

    public void setMode_EditContentRef() throws Exception
    {
        init();
        this.mode = MODE_EDIT_CONTENT_REF;
        DocmaI18 i18 = GUIUtil.i18(this);
        setTitle(i18.getLabel("label.contentref.dialog.edit.title"));
        aliasLabel.setValue(i18.getLabel("label.contentref.target"));
    }

    public void setMode_NewImageRef() throws Exception
    {
        init();
        this.mode = MODE_NEW_IMAGE_REF;
        DocmaI18 i18 = GUIUtil.i18(this);
        setTitle(i18.getLabel("label.imageref.dialog.new.title"));
        aliasLabel.setValue(i18.getLabel("label.imageref.target"));
    }

    public void setMode_EditImageRef() throws Exception
    {
        init();
        this.mode = MODE_EDIT_IMAGE_REF;
        DocmaI18 i18 = GUIUtil.i18(this);
        setTitle(i18.getLabel("label.imageref.dialog.edit.title"));
        aliasLabel.setValue(i18.getLabel("label.imageref.target"));
    }

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

    public boolean doEdit(DocmaSession docmaSess) throws Exception
    {
        this.docmaSess = docmaSess;
        init();
        all_aliases = null;
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

    public void updateGUI(DocmaNode docmaNode)
    {
        init();

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
        init();
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
                DocmaAppUtil.listAliasesStartWith(val, getAllAliases(), tempList);
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

    private boolean hasInvalidInputs() throws Exception
    {
        init();
        String alias = aliasBox.getValue().trim();
        if (alias.length() == 0) {
            Messagebox.show("Please enter a target alias!");
            return true;
        }
        return false;
    }

}
