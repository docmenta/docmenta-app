/*
 * FileOverwriteDialog.java
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

import org.zkoss.zul.*;
import org.zkoss.zk.ui.Component;

/**
 *
 * @author MP
 */
public class FileOverwriteDialog extends Window
{
    static final int CANCEL_BUTTON = -1;  // close window without clicking button
    static final int OVERWRITE_BUTTON = 1;
    static final int REPLACE_BUTTON = 2;
    static final int RENAME_BUTTON = 3;
    static final int SKIP_BUTTON = 4;

    private int btn = CANCEL_BUTTON;
    private boolean is_all = false;
    
    private Label msgLabel;
    private Component folderConflictNote;
    private Button overwriteBtn;
    private Button replaceBtn;
    private Checkbox allBox;

    public int resolveFileConflict(String msg, boolean allow_overwrite) throws Exception
    {
        initFields();

        this.setWidth("400px");
        msgLabel.setValue(msg);
        folderConflictNote.setVisible(false);
        replaceBtn.setDisabled(true);
        replaceBtn.setVisible(false);
        overwriteBtn.setDisabled(! allow_overwrite);
        overwriteBtn.setVisible(allow_overwrite);
        allBox.setChecked(false);
        this.invalidate();

        btn = CANCEL_BUTTON;
        is_all = false;
        doModal();
        is_all = allBox.isChecked();
        return btn;
    }

    public int resolveFolderConflict(String msg) throws Exception
    {
        initFields();
        
        // msg += " \nClick 'Replace' to replace the existing folder with the uploaded folder. \n" +
        //        "Click 'Overwrite' to keep the existing folder and overwrite files with same pathname. \n" +
        //        "Click 'Auto-Rename' to keep the existing folder and rename the uploaded folder.";

        this.setWidth("500px");
        msgLabel.setValue(msg);
        folderConflictNote.setVisible(true);
        replaceBtn.setDisabled(false);
        replaceBtn.setVisible(true);
        overwriteBtn.setDisabled(false);
        overwriteBtn.setVisible(true);
        allBox.setChecked(false);
        this.invalidate();

        btn = CANCEL_BUTTON;
        is_all = false;
        doModal();
        is_all = allBox.isChecked();
        return btn;
    }

    public void onSkipClick()
    {
        btn = SKIP_BUTTON;
        setVisible(false);
    }

    public void onOverwriteClick()
    {
        btn = OVERWRITE_BUTTON;
        setVisible(false);
    }

    public void onReplaceClick()
    {
        btn = REPLACE_BUTTON;
        setVisible(false);
    }

    public void onRenameClick()
    {
        btn = RENAME_BUTTON;
        setVisible(false);
    }

    public boolean isAll()
    {
        return is_all;
    }
    
    private void initFields()
    {
        msgLabel = (Label) getFellow("FileOverwriteMessageLabel");
        folderConflictNote = getFellow("FileOverwriteFolderConflictNote");
        overwriteBtn = (Button) getFellow("FileOverwriteOverwriteBtn");
        replaceBtn = (Button) getFellow("FileOverwriteReplaceBtn");
        allBox = (Checkbox) getFellow("FileOverwriteAllCheckbox");
    }
}
