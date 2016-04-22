/*
 * FolderDialog.java
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

import org.docma.app.*;
import org.zkoss.zul.*;

/**
 *
 * @author MP
 */
public class FolderDialog extends Window
{
    private static final int IMG_FOLDER = 1;
    private static final int SYS_FOLDER = 2;

    private static final int MODE_NEW = 0;
    private static final int MODE_EDIT = 1;

    private int modalResult = -1;
    private int mode = -1;
    
    private Textbox nameBox;
    private Listbox typeBox;
    

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

    public int getModalResult()
    {
        return modalResult;
    }

    public String getFolderName()
    {
        init();
        return nameBox.getValue().trim();
    }

    public void setFolderName(String folderName)
    {
        init();
        nameBox.setValue(folderName);
    }
    
    public boolean addImageFolder(DocmaNode parentNode, DocmaSession docmaSess)
    throws Exception
    {
        return newFolder(IMG_FOLDER, parentNode, -1, docmaSess);
    }

    public boolean addSystemFolder(DocmaNode parentNode, DocmaSession docmaSess)
    throws Exception
    {
        return newFolder(SYS_FOLDER, parentNode, -1, docmaSess);
    }

    public boolean insertImageFolder(DocmaNode beforeNode, DocmaSession docmaSess)
    throws Exception
    {
        DocmaNode parentNode = beforeNode.getParent();
        int insertpos = parentNode.getChildPos(beforeNode);
        return newFolder(IMG_FOLDER, parentNode, insertpos, docmaSess);
    }

    public boolean insertSystemFolder(DocmaNode beforeNode, DocmaSession docmaSess)
    throws Exception
    {
        DocmaNode parentNode = beforeNode.getParent();
        int insertpos = parentNode.getChildPos(beforeNode);
        return newFolder(SYS_FOLDER, parentNode, insertpos, docmaSess);
    }

    public boolean editImageFolder(DocmaNode node, DocmaSession docmaSess)
    throws Exception
    {
        return editFolder(node, docmaSess);
    }

    public boolean editSystemFolder(DocmaNode node, DocmaSession docmaSess)
    throws Exception
    {
        return editFolder(node, docmaSess);
    }


    /* --------------  Private methods  --------------- */

    private void init()
    {
        nameBox = (Textbox) getFellow("FolderNameTextbox");
        typeBox = (Listbox) getFellow("FolderTypeListbox");
    }
    
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

    private boolean newFolder(int folderType,
                              DocmaNode parentNode,
                              int insertpos,
                              DocmaSession docmaSess)
    throws Exception
    {
        init();
        setMode_New();
        setFolderName("");
        setFolderType(folderType);
        typeBox.setDisabled(true);
        do {
            modalResult = -1;
            doModal();
            if (getModalResult() != GUIConstants.MODAL_OKAY) {
                return false;
            }
            if (hasInvalidInputs()) {
                continue;
            }
            DocmaNode new_folder = null;
            if (folderType == IMG_FOLDER) new_folder = docmaSess.createImageFolder();
            if (folderType == SYS_FOLDER) new_folder = docmaSess.createSystemFolder();
            new_folder.setTitle(getFolderName());
            if (insertpos < 0) {
                insertpos = parentNode.getDefaultInsertPos(new_folder);
            }
            parentNode.insertChild(insertpos, new_folder);
            return true;
        } while (true);
    }


    private boolean editFolder(DocmaNode node, DocmaSession docmaSess)
    throws Exception
    {
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
        int oldType = node.isImageFolder() ? IMG_FOLDER : SYS_FOLDER;
        setFolderType(oldType);
        do {
            modalResult = -1;
            doModal();
            if (getModalResult() != GUIConstants.MODAL_OKAY) {
                return false;
            }
            if (hasInvalidInputs()) {
                continue;
            }
            node.setTitle(getFolderName());
            int editedType = getFolderType();
            if (editedType != oldType) {
                if (editedType == IMG_FOLDER) {
                    node.setFolderTypeImage();
                } else {
                    node.setFolderTypeFile();
                }
            }
            return true;
        } while (true);
    }

    private void setMode_New()
    {
        mode = MODE_NEW;
        setTitle(GUIUtil.i18(this).getLabel("label.folder.dialog.new.title"));
    }

    private void setMode_Edit()
    {
        mode = MODE_EDIT;
        setTitle(GUIUtil.i18(this).getLabel("label.folder.dialog.edit.title"));
    }

    private boolean hasInvalidInputs() throws Exception
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
