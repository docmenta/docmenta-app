/*
 * SelectNodeTypeComposer.java
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

import org.docma.app.*;
import org.docma.coreapi.*;
import org.docma.util.Log;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.*;
import org.zkoss.zul.*;

/**
 *
 * @author MP
 */
public class SelectNodeTypeComposer extends SelectorComposer<Component>
{
    private static final int MODE_INSERT_HERE = 0;
    private static final int MODE_APPEND_SUB = 1;

    private static final int NODE_TYPE_CONTENT          = 1;
    private static final int NODE_TYPE_CONTENT_INCLUDE  = 2;
    private static final int NODE_TYPE_IMAGE_INCLUDE    = 3;
    private static final int NODE_TYPE_SECTION          = 4;
    private static final int NODE_TYPE_SECTION_INCLUDE  = 5;
    private static final int NODE_TYPE_IMAGE_FOLDER     = 6;
    private static final int NODE_TYPE_FILE_FOLDER      = 7;
    private static final int NODE_TYPE_SECTION_WITH_CONTENT = 8;
    private static final int NODE_TYPE_TEXT_FILE        = 9;

    private int mode = -1;
    private DocI18n i18n = null;
    private MainWindow mainWin = null;

    @Wire("#SelectNodeTypeDialog") Window dialog;
    @Wire("#SelectNodeTypeListbox") Listbox nodetypes_listbox;


    @Listen("onClick = #SelectNodeTypeOkayBtn; onDoubleClick = #SelectNodeTypeListbox; onOK = #SelectNodeTypeListbox")
    public void onSelectClick()
    {
        try {
            int ntype = getSelectedNodeType();
            if (ntype < 0) {
                Messagebox.show("Please select a node type from the list!");
                return;  // keep dialog open
            }

            if (ntype == NODE_TYPE_CONTENT) {
                if (mode == MODE_APPEND_SUB) mainWin.onAddContent();
                else if (mode == MODE_INSERT_HERE) mainWin.onInsertContent();
            } else
            if (ntype == NODE_TYPE_CONTENT_INCLUDE) {
                if (mode == MODE_APPEND_SUB) mainWin.onAddContentInclude();
                else if (mode == MODE_INSERT_HERE) mainWin.onInsertContentInclude();
            } else
            if (ntype == NODE_TYPE_IMAGE_INCLUDE) {
                if (mode == MODE_APPEND_SUB) mainWin.onAddImageInclude();
                else if (mode == MODE_INSERT_HERE) mainWin.onInsertImageInclude();
            } else
            if (ntype == NODE_TYPE_SECTION) {
                if (mode == MODE_APPEND_SUB) mainWin.onAddSubSection();
                else if (mode == MODE_INSERT_HERE) mainWin.onInsertSection();
            } else
            if (ntype == NODE_TYPE_SECTION_INCLUDE) {
                if (mode == MODE_APPEND_SUB) mainWin.onAddSectionInclude();
                else if (mode == MODE_INSERT_HERE) mainWin.onInsertSectionInclude();
            } else
            if (ntype == NODE_TYPE_SECTION_WITH_CONTENT) {
                if (mode == MODE_APPEND_SUB) mainWin.onAddSubSectionWithContent();
                else if (mode == MODE_INSERT_HERE) mainWin.onInsertSectionWithContent();
            } else
            if (ntype == NODE_TYPE_IMAGE_FOLDER) {
                if (mode == MODE_APPEND_SUB) mainWin.onAddImageFolder();
                else if (mode == MODE_INSERT_HERE) mainWin.onInsertImageFolder();
            } else
            if (ntype == NODE_TYPE_FILE_FOLDER) {
                if (mode == MODE_APPEND_SUB) mainWin.onAddSystemFolder();
                else if (mode == MODE_INSERT_HERE) mainWin.onInsertSystemFolder();
            } else
            if (ntype == NODE_TYPE_TEXT_FILE) {
                if (mode == MODE_APPEND_SUB) mainWin.onAddEmptyTextFile();
                else if (mode == MODE_INSERT_HERE) mainWin.onInsertEmptyTextFile();
            } else {
                Messagebox.show("Internal error: Unkown node type!");
                return;   // keep dialog open
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            try {
                Messagebox.show("Internal Error: " + ex.getMessage());
            } catch (Exception ex2) {}
            return;   // keep dialog open
        }
        dialog.setVisible(false);   // close dialog
    }

    @Listen("onClick = #SelectNodeTypeCancelBtn")
    public void onCancelClick()
    {
        dialog.setVisible(false);
    }

    public void insertNodeHere(MainWindow mainWin)
    {
        mode = MODE_INSERT_HERE;
        dialog.setTitle(GUIUtil.getI18n(dialog).getLabel("label.selectnodetype.dialog.insert.title"));
        doSelect(mainWin);
    }

    public void appendSubNode(MainWindow mainWin)
    {
        mode = MODE_APPEND_SUB;
        dialog.setTitle(GUIUtil.getI18n(dialog).getLabel("label.selectnodetype.dialog.addsub.title"));
        doSelect(mainWin);
    }

    private void doSelect(MainWindow mainWin)
    {
        this.mainWin = mainWin;
        try {
            i18n = mainWin.getI18n();
            nodetypes_listbox = (Listbox) dialog.getFellow("SelectNodeTypeListbox");

            fillNodeTypesListbox();
            if (nodetypes_listbox.getItemCount() > 0) {
                nodetypes_listbox.setSelectedIndex(0);
            }
            nodetypes_listbox.setFocus(true);
            dialog.doHighlighted();
        } catch (Exception ex) {
            ex.printStackTrace();
            try {
                Messagebox.show("Internal Error: " + ex.getMessage());
            } catch (Exception ex2) {}
        }
    }

    private int getSelectedNodeType()
    {
        Listitem sel_item = nodetypes_listbox.getSelectedItem();
        if (sel_item == null) return -1;

        String val = sel_item.getValue().toString();
        try {
            return Integer.parseInt(val);
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.error("Invalid node type value in selection list: " + val);
            return -1;
        }
    }

    private void fillNodeTypesListbox() throws Exception
    {
        nodetypes_listbox.getItems().clear();  // clear listbox

        DocmaWebTree docTree = mainWin.getDocTree();
        int sel_cnt = docTree.getSelectedCount();
        DocmaSession docmaSess = mainWin.getDocmaSession();
        String storeId = docmaSess.getStoreId();
        DocVersionId verId = docmaSess.getVersionId();
        String ver_state = docmaSess.getVersionState(storeId, verId);
        Treeitem item = docTree.getSelectedItem();
        DocmaNode node = (DocmaNode) item.getValue();
        DocmaNode parent = node.getParent();
        int pos = parent.getChildPos(node);

        boolean multiple = (sel_cnt > 1);
        boolean isTransMode = node.isTranslationMode();
        boolean hasContentRight = docmaSess.hasRight(AccessRights.RIGHT_EDIT_CONTENT);
        boolean hasTransRight = docmaSess.hasRight(AccessRights.RIGHT_TRANSLATE_CONTENT);
        boolean noEditRight = ! ((hasContentRight && !isTransMode) || (hasTransRight && isTransMode));
        boolean isReleased = (ver_state != null) && ver_state.equals(DocmaConstants.VERSION_STATE_RELEASED);
        boolean noEdit = noEditRight || isReleased;
        boolean isRoot = (parent == docmaSess.getRoot()); // node.isDocumentRoot() || node.isSystemRoot();
        boolean sectNode = node.isSection();
        boolean sysFolder = node.isSystemFolder();
        boolean insertContentAllowed = parent.isInsertContentAllowed(pos);
        boolean insertSectionAllowed = parent.isInsertSectionAllowed(pos);
        boolean folderNode = node.isImageFolder() || sysFolder;
        boolean groupNode = sectNode || folderNode;

        if (noEdit || multiple || isTransMode) {
            return;   // Insert or append not allowed -> leave list empty;
                      // Should never occur because in these cases the opening
                      // of the dialog should be prevented.
        }

        if (mode == MODE_APPEND_SUB) {
            if (sectNode || sysFolder) {
                addListitem_Content();
                addListitem_Section();
                addListitem_SectionWithContent();
                addListitem_ContentInclude();
                // addListitem_ImageInclude();
                addListitem_SectionInclude();
            }
            if (groupNode) {
                addListitem_ImageFolder();
                addListitem_FileFolder();
            }
            if (sysFolder) {
                addListitem_TextFile();
            }
        } else
        if (mode == MODE_INSERT_HERE) {
            if (isRoot) return; // Insert not allowed on root level

            if (insertContentAllowed) {
                addListitem_Content();
            }
            if (insertSectionAllowed) {
                addListitem_Section();
                addListitem_SectionWithContent();
            }
            
            // Add include nodes
            if (insertContentAllowed) {
                addListitem_ContentInclude();
                // addListitem_ImageInclude();
            }
            if (insertSectionAllowed) {
                addListitem_SectionInclude();
            }
            
            // Add folder nodes
            addListitem_ImageFolder();
            addListitem_FileFolder();
            
            // Add new file nodes
            if (parent.isSystemFolder()) {
                addListitem_TextFile();
            }
        }
    }

    private void addListitem_Content()
    {
        Listitem item = new Listitem();
        item.setLabel(i18n.getLabel("label.nodetype.content"));
        item.setImage("img/doc_icon.png");
        item.setValue("" + NODE_TYPE_CONTENT);
        item.setSclass("docitemseltype");
        nodetypes_listbox.appendChild(item);
    }

    private void addListitem_ContentInclude()
    {
        Listitem item = new Listitem();
        item.setLabel(i18n.getLabel("label.nodetype.contentinclude"));
        item.setImage("img/ref_doc.png");
        item.setValue("" + NODE_TYPE_CONTENT_INCLUDE);
        item.setSclass("docitemseltype");
        nodetypes_listbox.appendChild(item);
    }

    private void addListitem_ImageInclude()
    {
        Listitem item = new Listitem();
        item.setLabel(i18n.getLabel("label.nodetype.imageinclude"));
        item.setImage("img/ref_img.gif");
        item.setValue("" + NODE_TYPE_IMAGE_INCLUDE);
        item.setSclass("docitemseltype");
        nodetypes_listbox.appendChild(item);
    }

    private void addListitem_Section()
    {
        Listitem item = new Listitem();
        item.setLabel(i18n.getLabel("label.nodetype.section"));
        item.setImage("img/section_icon.png");
        item.setValue("" + NODE_TYPE_SECTION);
        item.setSclass("docitemseltype");
        nodetypes_listbox.appendChild(item);
    }

    private void addListitem_SectionInclude()
    {
        Listitem item = new Listitem();
        item.setLabel(i18n.getLabel("label.nodetype.sectioninclude"));
        item.setImage("img/ref_section.png");
        item.setValue("" + NODE_TYPE_SECTION_INCLUDE);
        item.setSclass("docitemseltype");
        nodetypes_listbox.appendChild(item);
    }

    private void addListitem_ImageFolder()
    {
        Listitem item = new Listitem();
        item.setLabel(i18n.getLabel("label.nodetype.imagefolder"));
        item.setImage("img/img_folder_icon.gif");
        item.setValue("" + NODE_TYPE_IMAGE_FOLDER);
        item.setSclass("docitemseltype");
        nodetypes_listbox.appendChild(item);
    }

    private void addListitem_FileFolder()
    {
        Listitem item = new Listitem();
        item.setLabel(i18n.getLabel("label.nodetype.filefolder"));
        item.setImage("img/file_folder_icon.gif");
        item.setValue("" + NODE_TYPE_FILE_FOLDER);
        item.setSclass("docitemseltype");
        nodetypes_listbox.appendChild(item);
    }

    private void addListitem_SectionWithContent()
    {
        Listitem item = new Listitem();
        item.setLabel(i18n.getLabel("label.nodetype.sectionwithcontent"));
        item.setImage("img/section_content.png");
        item.setValue("" + NODE_TYPE_SECTION_WITH_CONTENT);
        item.setSclass("docitemseltype");
        nodetypes_listbox.appendChild(item);
    }

    private void addListitem_TextFile()
    {
        Listitem item = new Listitem();
        item.setLabel(i18n.getLabel("label.nodetype.emptytextfile"));
        item.setImage("img/text_node_icon.png");
        item.setValue("" + NODE_TYPE_TEXT_FILE);
        item.setSclass("docitemseltype");
        nodetypes_listbox.appendChild(item);
    }
}
