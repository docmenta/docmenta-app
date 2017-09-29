/*
 * NodePropertiesComposer.java
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
public class NodePropertiesComposer extends SelectorComposer<Component>
{
    public static final String EVENT_OKAY = "onOkay";
    
    private static final int MODE_CONTENT = 0;
    private static final int MODE_SECTION = 1;

    private int mode = -1;
    private DocmaNode docmaNode = null;
    private DocmaSession docmaSess = null;
    private Callback callback = null;

    // Helper fields
    private boolean isRootSection = false;
    private boolean isNewTextFile = false;
    private String oldApplic = null;

    @Wire("#NodePropertiesDialog") Window propsDialog;
    @Wire("#NodePropsHelpBtn") Toolbarbutton helpBtn;
    @Wire("#NodePropsFindRefsBtn") Toolbarbutton findRefsBtn;
    @Wire("#NodeTitleLabel") Label titleLabel;
    @Wire("#NodeTitleTextbox") Textbox titleBox;
    @Wire("#NodeAliasTextbox") Textbox aliasBox;
    @Wire("#NodeApplicTextbox") Combobox applicBox;
    @Wire("#NodeStatusListbox") Listbox statusBox;
    @Wire("#NodeProgressLabel") Label progressLabel;
    @Wire("#NodeProgressSlider") Slider progressSlider;
    @Wire("#NodeCommentTextbox") Textbox commentBox;
    @Wire("#NodePriorityListbox") Listbox priorityBox;

    /* ----------  Open dialog methods (main entry points)  ----------------- */
    
    public void doEdit_SectionProps(DocmaNode docmaNode, DocmaSession docmaSess, Callback callback) throws Exception
    {
        mode = MODE_SECTION;
        isNewTextFile = false;
        doEdit(docmaNode, docmaSess, callback);
    }

    public void doEdit_NewTextFileProps(DocmaSession docmaSess, Callback callback) throws Exception
    {
        mode = MODE_CONTENT;
        isNewTextFile = true;
        doEdit(null, docmaSess, callback);  // null means new file
    }
    
    public void doEdit_ContentProps(DocmaNode docmaNode, DocmaSession docmaSess, Callback callback) throws Exception
    {
        if (docmaNode != null) {   // edit an existing node
            boolean isTransMode = docmaNode.isTranslationMode();
            // if (isTransMode && !docmaNode.isTranslated()) {
            //     Messagebox.show("Content has no translation!");
            //     return false;
            // }
            if (isTransMode && !(docmaNode.isHTMLContent() || docmaNode.isImageContent())) {
                Messagebox.show(label("label.nodeprops.translate_file_props_not_allowed"));
                return;
            }
        }

        mode = MODE_CONTENT;
        isNewTextFile = false;
        doEdit(docmaNode, docmaSess, callback);
    }

    /* --------------  Public methods  ---------------------- */
    
    @Listen("onClick = #NodePropDialogOkayBtn; onOK = #NodeTitleTextbox; onOK = #NodeAliasTextbox; onOK = #NodeApplicTextbox")
    public void onOkayClick()
    {
        try {
            if (hasInvalidInputs()) {
                return;  // keep dialog opened
            }
            if (callback != null) {
                callback.onEvent(EVENT_OKAY);
            }
        } catch (Exception ex) {
            Messagebox.show(ex.getLocalizedMessage());
            return;  // keep dialog opened
        }
        propsDialog.setVisible(false);
    }

    @Listen("onClick = #NodePropDialogCancelBtn")
    public void onCancelClick()
    {
        propsDialog.setVisible(false);
    }

//    @Listen("onClick = #NodePropsHelpBtn")
//    public void onHelpClick()
//    {
//        MainWindow.openHelp("help/node_types.html");
//    }

    @Listen("onClick = #NodePropsFindRefsBtn")
    public void onFindReferences() throws Exception
    {
        DocmaWebSession webSess = GUIUtil.getDocmaWebSession(propsDialog);
        MainWindow mainWin = webSess.getMainWindow();
        FindNodesComposer findDialog = mainWin.getFindNodesComposer();
        findDialog.doFindReferencingAlias(webSess.getDocmaSession(), aliasBox.getValue().trim(), null);
    }
    
    @Listen("onOpen = #NodeApplicTextbox")
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

    @Listen("onSelect = #NodeApplicTextbox")
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

    // public void onEvent(Event evt) throws Exception
    // {
    //     ScrollEvent se = (ScrollEvent) evt;
    //     org.docma.util.Log.info("Event value: " + se.getPos());
    // }

    public void updateModel(DocmaNode docmaNode, DocmaSession docmaSess)
    throws Exception
    {
        if (isRootSection) {
            return;  // no updates allowed on root section
        }
        docmaSess.startTransaction();
        try {
            String nd_title = titleBox.getValue().trim();
            // Note that for images with alias, the filename consists of the 
            // alias followed by extension (instead of title followed by 
            // extension). Therefore, using setFileName() on image nodes
            // with alias would update the alias instead of the title.
            if (docmaNode.isFileContent() && !docmaNode.isImageContent()) {
                // File nodes, excluding image nodes.
                // Title input field contains the filename (node's title 
                // attribute followed by node's extension attribute).
                if (nd_title.equals("")) {
                    nd_title = label("text.default.file_name");
                }
                
                // Update node's title and extension attributes.
                docmaNode.setFileName(nd_title);
            } else {
                // Any other content nodes (including image files).
                // Title input field contains the node's title attribute.
                if (nd_title.equals("") && docmaNode.isHTMLContent()) {
                    nd_title = label("text.default.content_title");
                }
                docmaNode.setTitle(nd_title);
            }
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

    /* --------------  Private methods  ---------------------- */
    
    private void doEdit(DocmaNode docmaNode, DocmaSession docmaSess, Callback callback) throws Exception
    {
        this.docmaNode = docmaNode;  // may be null
        this.docmaSess = docmaSess;
        this.callback = callback;
        
        if (mode == MODE_SECTION) {
            propsDialog.setTitle(label("label.nodeprops.dialog.editsection.title"));
        } else {
            propsDialog.setTitle(label("label.nodeprops.dialog.editcontent.title"));
        }
        updateGUI();
        titleBox.setFocus(true);
        propsDialog.doHighlighted();
    }

    private void updateGUI()
    {
        boolean is_file = isNewTextFile || ((docmaNode != null) && docmaNode.isFileContent());
        boolean is_file_excl_image = isNewTextFile || 
                                   (is_file && (docmaNode != null) && !docmaNode.isImageContent());

        if (is_file_excl_image) {
            titleLabel.setValue(label("label.node.filename") + ":");
        } else {
            titleLabel.setValue(label("label.node.title") + ":");
        }
        
        // clear status list
        statusBox.getItems().clear();
        // re-initialize status list
        DocI18n i18 = docmaSess.getI18n();
        statusBox.appendItem(i18.getLabel("label.workflowstatus.wip"), "wip");
        statusBox.appendItem(i18.getLabel("label.workflowstatus.rfa"), "rfa");
        if (docmaSess.hasRight(AccessRights.RIGHT_APPROVE_CONTENT)) {
            statusBox.appendItem(i18.getLabel("label.workflowstatus.approved"), "approved");
        }
        if (is_file) {
            statusBox.appendItem("", "");
        }

        progressSlider.setMaxpos(100);

        // Init applicability values
        applicBox.getItems().clear();
        String[] applicVals = docmaSess.getDeclaredApplics();
        for (String appl : applicVals) {
            applicBox.appendItem(appl);
        }
        
        String status = null;
        String priority = null;
        if (docmaNode != null) {  // edit existing node
            // Note that for images with alias, the filename includes the 
            // alias instead of the title.
            if (is_file_excl_image) {
                // File nodes, excluding image nodes.
                // Filename is title followed by extension.
                titleBox.setValue(docmaNode.getDefaultFileName());
            } else {
                // Any other content nodes (including image files)
                titleBox.setValue(docmaNode.getTitle());
            }
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

    private boolean hasInvalidInputs() throws Exception
    {
        String title = titleBox.getValue().trim();
        if (mode == MODE_SECTION) {
            // For sections, title has to be entered by the user.
            if (title.length() == 0) {
                Messagebox.show(label("label.nodeprops.enter_title"));
                return true;
            }
        }
        if (mode == MODE_CONTENT) {
            // For files (except images), the title field contains the filename, 
            // which cannot be empty.
            if (title.length() == 0) {
                if ((docmaNode != null) && docmaNode.isFileContent() && !docmaNode.isImageContent()) {
                    Messagebox.show(label("label.nodeprops.enter_filename"));
                    return true;
                }
            }
        }
        String newalias = aliasBox.getValue().trim();
        if (newalias.length() > 0) {
            if (newalias.equalsIgnoreCase("docmacontent")) {
                Messagebox.show(label("label.nodeprops.alias_reserved") + " \n" + 
                                label("label.nodeprops.select_other_alias"));
                // Note: docmacontent is used as id for the div enclosing the content
                //       in the content editor.
                return true;
            }
            if (! DocmaAppUtil.isValidAlias(newalias)) {
                Messagebox.show(label("label.nodeprops.invalid_alias") + " \n" + 
                                label("label.nodeprops.allowed_alias_chars"));
                return true;
            }
            String nid = docmaSess.getNodeIdByAlias(newalias);
            if (nid != null) {
                if ((docmaNode != null) && nid.equals(docmaNode.getId())) {
                    if (docmaNode.isHTMLContent() && docmaNode.hasContentAnchor(newalias)) {
                        Messagebox.show(label("label.nodeprops.alias_already_used_as_anchor"));
                        return true;
                    }
                } else {
                    Messagebox.show(label("label.nodeprops.alias_used_by_other_node"));
                    return true;
                }
            }
        }
        return false;
    }

    private String label(String key, Object... args)
    {
        return GUIUtil.getI18n(propsDialog).getLabel(key, args);
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
