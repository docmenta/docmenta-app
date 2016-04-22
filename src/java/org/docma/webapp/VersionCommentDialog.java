/*
 * VersionCommentDialog.java
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

/**
 *
 * @author MP
 */
public class VersionCommentDialog extends Window
{
    private int modalResult = -1;

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

    public boolean editComment(String comment) throws Exception
    {
        setComment(comment);
        modalResult = -1;
        doModal();
        if (modalResult != GUIConstants.MODAL_OKAY) {
            return false;
        } else {
            return true;
        }
    }

    public String getComment()
    {
        Textbox box = (Textbox) getFellow("VersionCommentTextbox");
        return box.getValue();
    }

    private void setComment(String comment)
    {
        Textbox box = (Textbox) getFellow("VersionCommentTextbox");
        box.setValue(comment);
    }


}
