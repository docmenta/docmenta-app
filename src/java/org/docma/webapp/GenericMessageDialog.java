/*
 * GenericMessageDialog.java
 * 
 *  Copyright (C) 2014  Manfred Paula, http://www.docmenta.org
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

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Window;

/**
 *
 * @author MP
 */
public class GenericMessageDialog extends Window
{
    public final int MAX_OPTIONS = 4; 
    
    private Label msgLabel;
    private Component detailArea;
    private Component[] detailMsgArea = new Component[MAX_OPTIONS];
    private Label[] detailLabel = new Label[MAX_OPTIONS];
    private Component btnArea;
    private Button[] btn = new Button[MAX_OPTIONS];
    
    private int clickedBtn = -1;
    
    
    public int showDialog(String msg, String[] btnMessages)
    {
        return showDialog("", msg, null, btnMessages);
    }
    
    public int showDialog(String title, String msg, String[] btnMessages)
    {
        return showDialog(title, msg, null, btnMessages);
    }
    
    public int showDialog(String msg, String[] detailMessages, String[] btnMessages)
    {
        return showDialog("", msg, detailMessages, btnMessages);
    }
    
    public int showDialog(String title, String msg, String[] detailMessages, String[] btnMessages)
    {
        initFields();
        this.setTitle(title);
        msgLabel.setValue(msg);

        // Update detail messages
        int detailCnt = (detailMessages == null) ? 0 : Math.min(MAX_OPTIONS, detailMessages.length);
        detailArea.setVisible(detailCnt > 0);
        for (int i = 0; i < detailCnt; i++) {
            detailMsgArea[i].setVisible(true);
            detailLabel[i].setValue(detailMessages[i]);
        }
        for (int i = detailCnt; i < MAX_OPTIONS; i++) {
            detailMsgArea[i].setVisible(false);
            detailLabel[i].setValue("");
        }
        detailArea.invalidate();
        
        // Update buttons
        int btnCnt = (btnMessages == null) ? 0 : Math.min(MAX_OPTIONS, btnMessages.length);
        for (int i = 0; i < btnCnt; i++) {
            btn[i].setVisible(true);
            btn[i].setLabel(btnMessages[i]);
        }
        for (int i = btnCnt; i < MAX_OPTIONS; i++) {
            btn[i].setVisible(false);
            btn[i].setLabel("");
        }
        btnArea.invalidate();
        
        clickedBtn = -1;
        doModal();
        return clickedBtn;
    }

    public void onBtnClick(int btnNum)
    {
        clickedBtn = btnNum;
        setVisible(false);
    }

    private void initFields()
    {
        msgLabel = (Label) getFellow("GenericMessageLabel");
        detailArea = getFellow("GenericMessageDetailArea");
        btnArea = getFellow("GenericMessageBtnArea");
        for (int i = 0; i < MAX_OPTIONS; i++) {
            int id_num = i + 1;
            detailMsgArea[i] = getFellow("GenericMessageDetail" + id_num + "Area");
            detailLabel[i] = (Label) getFellow("GenericMessageDetail" + id_num + "Label");
            btn[i] = (Button) getFellow("GenericMessageBtn" + id_num);
        }
    }
    
}
