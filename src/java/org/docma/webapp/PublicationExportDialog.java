/*
 * PublicationExportDialog.java
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
import org.docma.coreapi.*;
import org.zkoss.zul.*;

/**
 *
 * @author MP
 */
public class PublicationExportDialog extends Window
{
    private static final int MODE_NEW = 0;
    private static final int MODE_EDIT = 1;

    private int modalResult = -1;
    private int mode = -1;

    private DocmaSession docmaSess;

    private Listbox pubconflist;
    private Listbox outconflist;
    private Listbox languagelist;
    private Textbox filenamebox;
    private Button okaybutton;

    private String default_file_extension = null;


    public boolean doStartExport(DocmaSession docmaSess) throws Exception
    {
        this.mode = MODE_NEW;
        this.docmaSess = docmaSess;

        init();
        updateGUI(docmaSess, null); // init dialog fields
        if (pubconflist.getItemCount() == 0) {
            Messagebox.show("Please create a publication configuration first.");
            return false;
        }
        if (outconflist.getItemCount() == 0) {
            Messagebox.show("Please create a media configuration first.");
            return false;
        }
        do {
            modalResult = -1;
            doModal();
            if (modalResult != GUIConstants.MODAL_OKAY) {
                return false;
            }
            if (hasInvalidInputs(docmaSess)) {
                continue;
            }
            updateModel(docmaSess, null);
            return true;
        } while (true);
    }

    public boolean doEditExport(DocmaPublication pub, DocmaSession docmaSess) throws Exception
    {
        this.mode = MODE_EDIT;
        this.docmaSess = docmaSess;

        init();
        updateGUI(docmaSess, pub); // init dialog fields
        do {
            modalResult = -1;
            doModal();
            if (modalResult != GUIConstants.MODAL_OKAY) {
                return false;
            }
            if (hasInvalidInputs(docmaSess)) {
                continue;
            }
            updateModel(docmaSess, pub);
            return true;
        } while (true);
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

    public void onSelectPublication()
    {
        init();
        setDefaultFilename();
    }

    public void onSelectOutput()
    {
        init();
        setDefaultFilename();
    }

    public void onSelectLanguage()
    {
        init();
        setDefaultFilename();
    }

    private void init()
    {
        pubconflist = (Listbox) getFellow("PubExportPubConfigListbox");
        outconflist = (Listbox) getFellow("PubExportOutConfigListbox");
        languagelist = (Listbox) getFellow("PubExportLangListbox");
        filenamebox = (Textbox) getFellow("PubExportFilenameTextbox");
        okaybutton = (Button) getFellow("PubExportOkayButton");
    }


    private boolean hasInvalidInputs(DocmaSession docmaSess) throws Exception
    {
        String fn = getFilename();
        if (fn.equals("")) {
            Messagebox.show("Please enter a filename.");
            return true;
        }
        if (default_file_extension != null) {
            String fn_ext = getFileExtension(fn);
            if (! default_file_extension.equalsIgnoreCase(fn_ext)) {
                Messagebox.show("Filename must have extension " + default_file_extension);
                return true;
            }
        }
        return false;
    }

    private void updateGUI(DocmaSession docmaSess, DocmaPublication pub)
    {
        // Clear lists
        pubconflist.getItems().clear();
        outconflist.getItems().clear();
        languagelist.getItems().clear();

        // Add list items
        if (mode == MODE_NEW) {
            String[] pub_ids = docmaSess.getPublicationConfigIds();
            for (int i=0; i < pub_ids.length; i++) {
                String pub_id = pub_ids[i];
                pubconflist.appendItem(pub_id, pub_id);
            }
            if (pub_ids.length > 0) pubconflist.setSelectedIndex(0);

            String[] out_ids = docmaSess.getOutputConfigIds();
            for (int i=0; i < out_ids.length; i++) {
                String out_id = out_ids[i];
                outconflist.appendItem(out_id, out_id);
            }
            if (out_ids.length > 0) outconflist.setSelectedIndex(0);

            DocmaLanguage orig = docmaSess.getOriginalLanguage();
            String lang_label = orig.getDescription() + " (" + orig.getCode() + ")";
            languagelist.appendItem(lang_label, orig.getCode());
            int sel_idx = 0;

            String trans_mode = docmaSess.getTranslationMode();
            DocmaLanguage[] trans_langs = docmaSess.getTranslationLanguages();
            for (int i=0; i < trans_langs.length; i++) {
                DocmaLanguage lang = trans_langs[i];
                lang_label = lang.getDescription() + " (" + lang.getCode() + ")";
                languagelist.appendItem(lang_label, lang.getCode());
                if ((trans_mode != null) && trans_mode.equalsIgnoreCase(lang.getCode())) {
                    sel_idx = i + 1;
                }
            }
            languagelist.setSelectedIndex(sel_idx);

            setDefaultFilename();
        } else {  // mode == MODE_EDIT
            String pub_id = pub.getPublicationConfigId();
            String out_id = pub.getOutputConfigId();
            String lang = pub.getLanguage();
            String fn = pub.getFilename();
            pubconflist.appendItem(pub_id, pub_id);
            outconflist.appendItem(out_id, out_id);
            languagelist.appendItem(lang, lang);
            setFilename(fn);
        }
        languagelist.setDisabled(true);

        DocmaI18 i18 = GUIUtil.i18(this);
        if (mode == MODE_NEW) {
            pubconflist.setDisabled(false);
            outconflist.setDisabled(false);
            okaybutton.setLabel(i18.getLabel("label.startexport.btn"));
        } else {  // mode == MODE_EDIT
            pubconflist.setDisabled(true);
            outconflist.setDisabled(true);
            okaybutton.setLabel(i18.getLabel("label.okay.btn"));
        }
    }

    private void setDefaultFilename()
    {
        String sel_pub = getSelectedPubConfigId();
        String sel_out = getSelectedOutConfigId();
        String default_name = "";
        if ((sel_pub != null) && (sel_out != null)) {
            default_name = docmaSess.getDefaultPublicationFilename(sel_pub, sel_out);
        }
        filenamebox.setValue(default_name);

        default_file_extension = getFileExtension(default_name);
    }

    private void setFilename(String fn)
    {
        filenamebox.setValue(fn);
        default_file_extension = getFileExtension(fn);
    }

    private String getFileExtension(String filename)
    {
        int p = filename.lastIndexOf('.');
        if (p < 0) {
            return null;
        } else {
            return filename.substring(p+1);
        }
    }

    private void updateModel(DocmaSession docmaSess, DocmaPublication pub)
    throws DocException
    {
        if (mode == MODE_NEW) {
            start_PublicationExport(docmaSess);
        } else {
            // Edit filename
            pub.setFilename(getFilename());
        }
    }

    private void start_PublicationExport(DocmaSession docmaSess)
    throws DocException
    {
        String pubId = docmaSess.createPublication(getSelectedPubConfigId(),
                                                   getSelectedOutConfigId(),
                                                   getFilename());
        docmaSess.exportPublicationAsync(pubId);

//        DocmaSession tempSess = docmaSess.createNewSession();
//        tempSess.openDocStore(docmaSess.getStoreId(), docmaSess.getVersionId());
//        String transMode = docmaSess.getTranslationMode();
//        if (transMode != null) {
//            tempSess.enterTranslationMode(transMode);
//        }
//        String pubId = tempSess.createPublication(getSelectedPubConfigId(),
//                                                  getSelectedOutConfigId(),
//                                                  getFilename());
//        PublicationExportThread exp_thread = new PublicationExportThread(tempSess, pubId);
//        exp_thread.start();
    }

    private String getSelectedPubConfigId()
    {
        Listitem item = pubconflist.getSelectedItem();
        if (item == null) { 
            return null;
        }
        return (String) item.getValue();
    }

    private String getSelectedOutConfigId()
    {
        Listitem item = outconflist.getSelectedItem();
        if (item == null) { 
            return null;
        }
        return (String) item.getValue();
    }

    private String getSelectedLangCode()
    {
        return (String) languagelist.getSelectedItem().getValue();
    }

    private String getFilename()
    {
        return filenamebox.getValue().trim();
    }
}
