/*
 * FolderDialogComposer.java
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
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.*;
import org.zkoss.zul.*;

/**
 *
 * @author MP
 */
public class FolderDialogComposer extends SelectorComposer<Component>
{
    public static final String EVENT_OKAY = "onOkay";
    
    private static final int IMG_FOLDER = 1;
    private static final int SYS_FOLDER = 2;

    private static final int MODE_NEW = 0;
    private static final int MODE_EDIT = 1;

    private int mode = -1;
    private DocmaSession docmaSess = null;
    private DocmaNode parentNode = null;
    private DocmaNode folderNode = null;
    private int oldFolderType = -1;
    private int insertPos = -1;
    private Callback callback = null;
    
    @Wire("#FolderDialog") Window folderDialog; 
    @Wire("#FolderNameTextbox") Textbox nameBox;
    @Wire("#FolderTypeListbox") Listbox typeBox;
    

    @Listen("onClick = #FolderDialogOkayBtn")
    public void onOkayClick()
    {
        if (hasInvalidInputs()) {
            return;  // keep dialog opened
        }
        try {
            int folderType = getFolderType();
            if (mode == MODE_NEW) {
                DocmaNode new_folder = null;
                if (folderType == IMG_FOLDER) new_folder = docmaSess.createImageFolder();
                if (folderType == SYS_FOLDER) new_folder = docmaSess.createSystemFolder();
                if (new_folder == null) {  // should never occur
                    Messagebox.show("Error: Unknown folder type!");
                    return;  // keep dialog opened
                }
                new_folder.setTitle(getFolderName());
                if (insertPos < 0) {
                    insertPos = parentNode.getDefaultInsertPos(new_folder);
                }
                parentNode.insertChild(insertPos, new_folder);
            } else if (mode == MODE_EDIT) {
                folderNode.setTitle(getFolderName());
                int editedType = getFolderType();
                if (editedType != oldFolderType) {
                    if (editedType == IMG_FOLDER) {
                        folderNode.setFolderTypeImage();
                    } else {
                        folderNode.setFolderTypeFile();
                    }
                }
            }
            
            if (callback != null) {
                callback.onEvent(EVENT_OKAY);
            }
        } catch (Exception ex) {
            Messagebox.show(ex.getLocalizedMessage());
            return;  // keep dialog opened
        }
        folderDialog.setVisible(false);
    }

    @Listen("onClick = #FolderDialogCancelBtn")
    public void onCancelClick()
    {
        folderDialog.setVisible(false);
    }

    public String getFolderName()
    {
        return nameBox.getValue().trim();
    }

    public void setFolderName(String folderName)
    {
        nameBox.setValue(folderName);
    }
    
    public void addImageFolder(DocmaNode parentNode, DocmaSession docmaSess, Callback callback)
    throws Exception
    {
        newFolder(IMG_FOLDER, parentNode, -1, docmaSess, callback);
    }

    public void addSystemFolder(DocmaNode parentNode, DocmaSession docmaSess, Callback callback)
    throws Exception
    {
        newFolder(SYS_FOLDER, parentNode, -1, docmaSess, callback);
    }

    public void insertImageFolder(DocmaNode beforeNode, DocmaSession docmaSess, Callback callback)
    throws Exception
    {
        DocmaNode parNode = beforeNode.getParent();
        int insPos = parNode.getChildPos(beforeNode);
        newFolder(IMG_FOLDER, parNode, insPos, docmaSess, callback);
    }

    public void insertSystemFolder(DocmaNode beforeNode, DocmaSession docmaSess, Callback callback)
    throws Exception
    {
        DocmaNode parNode = beforeNode.getParent();
        int insPos = parNode.getChildPos(beforeNode);
        newFolder(SYS_FOLDER, parNode, insPos, docmaSess, callback);
    }

    public void editFolder(DocmaNode node, DocmaSession docmaSess, Callback callback)
    throws Exception
    {
        this.parentNode = null;  // not used in edit mode; only used in MODE_NEW
        this.folderNode = node;
        this.insertPos = -1;     // not used in edit mode; only used in MODE_NEW
        this.docmaSess = docmaSess;
        this.callback = callback;
        setMode_Edit();
        setFolderName(node.getTitle());
        if ((node == docmaSess.getRoot()) ||
            (node == docmaSess.getSystemRoot()) || 
            (node == docmaSess.getFileRoot()) ||
            (node == docmaSess.getMediaRoot())) 
        {
            typeBox.setDisabled(true);  // do not allow changing node type of predefined root folders
        } else {
            typeBox.setDisabled(false);
        }
        oldFolderType = node.isImageFolder() ? IMG_FOLDER : SYS_FOLDER;
        setFolderType(oldFolderType);
        folderDialog.doHighlighted();
    }


    /* --------------  Private methods  --------------- */

    private int getFolderType()
    {
        Listitem item = typeBox.getSelectedItem();
        if (item == null) {
            return SYS_FOLDER;
        } else {
            Object val = item.getValue();
            return ((val != null) && val.toString().equals("imagefolder")) ? IMG_FOLDER : SYS_FOLDER;
        }
    }
    
    private void setFolderType(int ftype)
    {
        GUIUtil.selectListItem(typeBox, (ftype == SYS_FOLDER) ? "filefolder" : "imagefolder");
    }

    private void newFolder(int folderType,
                           DocmaNode parentNode,
                           int insertPos,
                           DocmaSession docmaSess, 
                           Callback callback)
    throws Exception
    {
        this.parentNode = parentNode;
        this.folderNode = null;        // not used in new mode; only used in MODE_EDIT
        this.insertPos = insertPos;
        this.docmaSess = docmaSess;
        this.callback = callback;
        setMode_New();
        setFolderName("");
        setFolderType(folderType);
        typeBox.setDisabled(true);
        folderDialog.doHighlighted();
    }


    private void setMode_New()
    {
        mode = MODE_NEW;
        folderDialog.setTitle(GUIUtil.getI18n(folderDialog).getLabel("label.folder.dialog.new.title"));
    }

    private void setMode_Edit()
    {
        mode = MODE_EDIT;
        folderDialog.setTitle(GUIUtil.getI18n(folderDialog).getLabel("label.folder.dialog.edit.title"));
    }

    private boolean hasInvalidInputs()
    {
        String name = getFolderName();
        if (name.equals("")) {
            Messagebox.show("Please enter a folder name.");
            return true;
        }
        if (name.length() > 40) {
            Messagebox.show("Name is too long. Maximum length is 40 characters.");
            return true;
        }
        if (! name.matches(DocmaConstants.REGEXP_NAME)) {
            Messagebox.show("Invalid folder name. Allowed characters are letters, punctuation characters and space.");
            return true;
        }
        return false;
    }

}
