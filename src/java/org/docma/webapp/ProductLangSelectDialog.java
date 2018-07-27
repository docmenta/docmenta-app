/*
 * ProductLangSelectDialog.java
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
import org.docma.app.ui.*;

import java.util.*;
import org.zkoss.zul.*;
import org.zkoss.zk.ui.SuspendNotAllowedException;


/**
 *
 * @author MP
 */
public class ProductLangSelectDialog extends Window
{
    private int modalResult = -1;

    public void onOkayClick() throws Exception
    {
        Listbox langlistbox = (Listbox) getFellow("ProductLangSelectListbox");
        if (langlistbox.getSelectedCount() == 0) {
            Messagebox.show("Please select a language.");
            return;
        }
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

    public void doModal() throws SuspendNotAllowedException
    {
        modalResult = -1;
        super.doModal();
    }

    public void resetGUI()
    {
        setTitle(GUIUtil.i18(this).getLabel("label.langselect.dialog.title"));
        Listbox langlistbox = (Listbox) getFellow("ProductLangSelectListbox");
        if (langlistbox.getItemCount() == 0) {
            DocmaLanguage[] arr = getDocmaSession().getSupportedContentLanguages();
            sortLangs(arr);
            for (DocmaLanguage lang : arr) {
                Listitem item = new Listitem();
                item.setLabel(lang.getDisplayName());
                item.setValue(lang.getCode());
                langlistbox.appendChild(item);
            }
        }
        langlistbox.clearSelection();
    }

    public DocmaLanguageModel[] getSelectedLanguages()
    {
        Listbox langlist = (Listbox) getFellow("ProductLangSelectListbox");
        int selcnt = langlist.getSelectedCount();
        DocmaLanguageModel[] arr = new DocmaLanguageModel[selcnt];
        Iterator langs = langlist.getSelectedItems().iterator();
        int i=0;
        while (langs.hasNext()) {
            Listitem item = (Listitem) langs.next();
            arr[i] = new DocmaLanguageModel(item.getValue().toString(), item.getLabel());
            arr[i].setTranslation(true);
            i++;
        }
        return arr;
    }

    /* --------------  Private methods  --------------- */

    private DocmaSession getDocmaSession()
    {
        return GUIUtil.getDocmaWebSession(this).getDocmaSession();
    }
    
    private void sortLangs(DocmaLanguage[] arr)
    {
        Arrays.sort(arr, new Comparator() {
            public int compare(Object obj1, Object obj2) 
            {
                String lang1 = ((DocmaLanguage) obj1).getDisplayName();
                String lang2 = ((DocmaLanguage) obj2).getDisplayName();
                if (lang1 == null) {
                    return 1;
                }
                if (lang2 == null) {
                    return -1;
                }
                return lang1.compareToIgnoreCase(lang2);
            }
        } );
    }
}
