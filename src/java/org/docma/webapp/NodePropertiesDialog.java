/*
 * NodePropertiesDialog.java
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

import org.docma.coreapi.*;
import org.docma.app.*;
import org.zkoss.zul.*;
import org.zkoss.zk.ui.event.*;

/**
 *
 * @author MP
 */
public class NodePropertiesDialog extends Window implements EventListener
{
    private static final int MODE_CONTENT = 0;
    private static final int MODE_SECTION = 1;

    private int modalResult = -1;
    private int mode = -1;

    private Textbox titleBox;
    private Textbox aliasBox;
    private Textbox applicBox;
    // private Listbox styleBox;
    private Listbox statusBox;
    private Label progressLabel;
    private Slider progressSlider;
    private Textbox commentBox;
    private Listbox priorityBox;

    private boolean isRootSection = false;
    private boolean initialized = false;

    private void init()
    {
        if (! initialized) {
            titleBox = (Textbox) getFellow("NodeTitleTextbox");
            aliasBox = (Textbox) getFellow("NodeAliasTextbox");
            applicBox = (Textbox) getFellow("NodeApplicTextbox");
            // styleBox = (Listbox) getFellow("NodeStyleListbox");
            statusBox = (Listbox) getFellow("NodeStatusListbox");
            progressLabel = (Label) getFellow("NodeProgressLabel");
            progressSlider = (Slider) getFellow("NodeProgressSlider");
            // progressSlider.addEventListener("onScroll", this);
            commentBox = (Textbox) getFellow("NodeCommentTextbox");
            priorityBox = (Listbox) getFellow("NodePriorityListbox");

            initialized = true;
        }
    }

    private boolean hasInvalidInputs(DocmaNode docmaNode, DocmaSession docmaSess) throws Exception
    {
        init();
        if (mode == MODE_SECTION) {
            // for sections, title has to be entered by the user
            String title = titleBox.getValue().trim();
            if (title.length() == 0) {
                Messagebox.show("Please enter a title!");
                return true;
            }
        }
        String newalias = aliasBox.getValue().trim();
        if (newalias.length() > 0) {
            if (newalias.equalsIgnoreCase("docmacontent")) {
                Messagebox.show("The name 'docmacontent' is reserved for internal use. \nPlease select another alias name.");
                // Note: docmacontent is used as id for the div enclosing the content
                //       in the content editor.
                return true;
            }
            if (! DocmaAppUtil.isValidAlias(newalias)) {
                Messagebox.show("Invalid alias name. \nAllowed characters are ASCII letters, digits, underscore, dash and question mark.");
                return true;
            }
            String nid = docmaSess.getNodeIdByAlias(newalias);
            if (nid != null) {
                if ((docmaNode != null) && nid.equals(docmaNode.getId())) {
                    if (docmaNode.isHTMLContent() && docmaNode.hasContentAnchor(newalias)) {
                        Messagebox.show("Alias is already used as an anchor in the content of this node!");
                        return true;
                    }
                } else {
                    Messagebox.show("Alias is already used by another node!");
                    return true;
                }
            }
        }
        return false;
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

    public boolean doEdit_SectionProps(DocmaNode docmaNode, DocmaSession docmaSess) throws Exception
    {
        mode = MODE_SECTION;
        setTitle(GUIUtil.i18(this).getLabel("label.nodeprops.dialog.editsection.title"));
        return doEdit(docmaNode, docmaSess);
    }

    public boolean doEdit_ContentProps(DocmaNode docmaNode, DocmaSession docmaSess) throws Exception
    {
        if (docmaNode != null) {   // edit an existing node
            boolean isTransMode = docmaNode.isTranslationMode();
            // if (isTransMode && !docmaNode.isTranslated()) {
            //     Messagebox.show("Content has no translation!");
            //     return false;
            // }
            if (isTransMode && !(docmaNode.isHTMLContent() || docmaNode.isImageContent())) {
                Messagebox.show("Translation of file properties is not allowed!");
                return false;
            }
        }

        mode = MODE_CONTENT;
        setTitle(GUIUtil.i18(this).getLabel("label.nodeprops.dialog.editcontent.title"));
        return doEdit(docmaNode, docmaSess);
    }

    private boolean doEdit(DocmaNode docmaNode, DocmaSession docmaSess) throws Exception
    {
        updateGUI(docmaNode, docmaSess);
        titleBox.setFocus(true);
        do {
            modalResult = -1;
            doModal();
            if (modalResult != GUIConstants.MODAL_OKAY) {
                return false;
            }
            if (hasInvalidInputs(docmaNode, docmaSess)) {
                continue;
            }
            return true;
        } while (true);
    }

    private void updateGUI(DocmaNode docmaNode, DocmaSession docmaSess)
    {
        init();
        boolean is_file = (docmaNode != null) && docmaNode.isFileContent();

        // clear status list
        statusBox.getItems().clear();
        // re-initialize status list
        DocmaI18 i18 = GUIUtil.i18(this);
        statusBox.appendItem(i18.getLabel("label.workflowstatus.wip"), "wip");
        statusBox.appendItem(i18.getLabel("label.workflowstatus.rfa"), "rfa");
        if (docmaSess.hasRight(AccessRights.RIGHT_APPROVE_CONTENT)) {
            statusBox.appendItem(i18.getLabel("label.workflowstatus.approved"), "approved");
        }
        if (is_file) {
            statusBox.appendItem("", "");
        }

        progressSlider.setMaxpos(100);

        String status = null;
        String priority = null;
        if (docmaNode != null) {  // edit existing node
            titleBox.setValue(docmaNode.getTitle());
            aliasBox.setValue(docmaNode.getAlias());
            isRootSection = docmaNode.isDocumentRoot();
            applicBox.setValue(docmaNode.getApplicability());
            progressSlider.setCurpos(docmaNode.getProgress());
            status = docmaNode.getWorkflowStatus();
            commentBox.setValue(docmaNode.getComment());
            priority = docmaNode.getPriority();
        } else {   // new node
            titleBox.setValue("");
            aliasBox.setValue("");
            isRootSection = false;
            applicBox.setValue("");
            progressSlider.setCurpos(0);
            status = "wip";
            commentBox.setValue("");
            priority = "1";   // default priority, 1 = normal
        }
        if (is_file && ((status == null) || status.equals(""))) {
            // select last item, i.e. no workflow status
            statusBox.setSelectedIndex(statusBox.getItemCount() - 1);
        } else {
            selectStatus(status);
        }
        selectPriority(priority);

        boolean isTransMode = (docmaNode != null) && docmaNode.isTranslationMode();
        if (mode == MODE_CONTENT) {
            titleBox.setDisabled(false);
            aliasBox.setDisabled(isTransMode);
            applicBox.setDisabled(isTransMode);
            statusBox.setDisabled(false);
            progressLabel.setVisible(true);
            progressSlider.setVisible(true);
            commentBox.setDisabled(false);
            priorityBox.setDisabled(false);
        }
        if (mode == MODE_SECTION) {
            titleBox.setDisabled(isRootSection);
            aliasBox.setDisabled(isRootSection || isTransMode);
            applicBox.setDisabled(isRootSection || isTransMode);
            statusBox.setDisabled(true);
            progressLabel.setVisible(false);
            progressSlider.setVisible(false);
            commentBox.setDisabled(isRootSection);
            priorityBox.setDisabled(isRootSection);
        }
    }

    public void updateModel(DocmaNode docmaNode, DocmaSession docmaSess)
    throws Exception
    {
        init();
        if (isRootSection) {
            return;  // no updates allowed on root section
        }
        docmaSess.startTransaction();
        try {
            String nd_title = titleBox.getValue().trim();
            if (nd_title.equals("") && docmaNode.isHTMLContent()) {
                nd_title = GUIUtil.i18(this).getLabel("text.default.content_title");
            }
            docmaNode.setTitle(nd_title);
            if (! docmaNode.isTranslationMode()) {  // translation of alias and applic is not allowed
                docmaNode.setAlias(aliasBox.getValue().trim());
                docmaNode.setApplicability(applicBox.getValue().trim());
            }
            if (mode == MODE_CONTENT) {
                String wf_state = (String) statusBox.getSelectedItem().getValue();
                docmaNode.setWorkflowStatus(wf_state);
                // progressSlider.setSlidingtext("{0}");
                // org.docma.util.Log.info("Progress text: " + progressSlider.getSlidingtext());
                // org.docma.util.Log.info("Progress value: " + progressSlider.getCurpos());
                int curpos = progressSlider.getCurpos();
                if ((curpos > 0) || docmaNode.isHTMLContent()) {
                    docmaNode.setProgress(curpos);
                }
            }
            docmaNode.setComment(commentBox.getValue().trim());
            docmaNode.setPriority((String) priorityBox.getSelectedItem().getValue());
            docmaSess.commitTransaction();
        } catch (Exception ex) {
            ex.printStackTrace();
            docmaSess.rollbackTransaction();
            // docmaNode.refresh();
            Messagebox.show("Error during transaction: " + ex.getMessage());
        }
    }

    public void onEvent(Event evt) throws Exception
    {
        // ScrollEvent se = (ScrollEvent) evt;
        // org.docma.util.Log.info("Event value: " + se.getPos());
    }


    private void selectStatus(String status)
    {
        if ((status == null) || status.equals("") || status.equalsIgnoreCase("wip")) {
            statusBox.setSelectedIndex(0);
        } else
        if (status.equalsIgnoreCase("rfa")) {
            statusBox.setSelectedIndex(1);
        } else
        if (status.equalsIgnoreCase("approved")) {
            statusBox.setSelectedIndex(2);
        }
    }

    private void selectPriority(String prio)
    {
        if (prio == null) prio = "";
        if (prio.equalsIgnoreCase("0")) {
            priorityBox.setSelectedIndex(0);
        } else
        if (prio.equalsIgnoreCase("2")) {
            priorityBox.setSelectedIndex(2);
        } else {
            priorityBox.setSelectedIndex(1);
        }
    }

}
