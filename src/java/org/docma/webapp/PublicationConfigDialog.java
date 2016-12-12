/*
 * PublicationConfigDialog.java
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
import org.docma.util.XMLUtil;
import org.zkoss.zul.*;
import org.zkoss.zk.ui.event.*;

/**
 *
 * @author MP
 */
public class PublicationConfigDialog extends Window implements org.zkoss.zk.ui.event.EventListener
{
    private static final int MODE_NEW = 0;
    private static final int MODE_EDIT = 1;

    private int modalResult = -1;
    private int mode = -1;

    private DocmaSession docmaSess;
    private List contentAliases = null;
    private List tempList = new ArrayList(500);
    // private boolean is_unreg;

    private Textbox idBox;
    private Combobox contentRootBox;
    private String contentRootId;
    private Combobox prefaceRootBox;
    private String prefaceRootId;
    private Combobox appendixRootBox;
    private String appendixRootId;
    private Textbox coverBox;
    private Textbox filterBox;
    private Textbox referencedPubsBox;
    private Checkbox customTitlePage1Checkbox;
    private Combobox customTitlePage1Box;
    private Checkbox customTitlePage2Checkbox;
    private Combobox customTitlePage2Box;
    private Textbox titleBox;
    private Textbox subtitleBox;
    private Textbox releaseInfoBox;
    private Textbox corporateBox;
    private Textbox authorsBox;
    private Textbox pubDateBox;
    private Textbox copyrightYearBox;
    private Textbox copyrightHolderBox;
    private Combobox abstractBox;
    private Combobox legalNoticeBox;
    private Textbox creditBox;
    private Textbox publisherBox;
    private Textbox biblioIdBox;

    /* -----------  Public methods  ------------------ */

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

    public void setMode_New()
    {
        init();
        mode = MODE_NEW;
        setTitle(GUIUtil.i18(this).getLabel("label.pubconfig.dialog.new.title"));
    }

    public void setMode_Edit()
    {
        init();
        mode = MODE_EDIT;
        setTitle(GUIUtil.i18(this).getLabel("label.pubconfig.dialog.edit.title"));
    }

    public boolean doEdit(DocmaPublicationConfig pubConf, DocmaSession docmaSess)
    throws Exception
    {
        init();
        contentAliases = null;
        updateGUI(pubConf, docmaSess); // init dialog fields
        do {
            modalResult = -1;
            doModal();
            checkIndexTermOnTitlePage();
            if (getModalResult() != GUIConstants.MODAL_OKAY) {
                return false;
            }
            if (hasInvalidInputs()) {
                continue;
            }
            updateModel(pubConf);
            return true;
        } while (true);
    }

    public void onEvent(Event evt) throws Exception
    {
        Object comp = evt.getTarget();
        // if ((comp == creditBox) && (evt instanceof MouseEvent)) {
        //     if (is_unreg) {
        //         Messagebox.show("The credits can only be customized in the registered version of Docmenta!");
        //     }
        //     return;
        // }
        if (! (comp instanceof Combobox)) {
            return;
        }
        Combobox rootBox = (Combobox) comp;
        if (evt instanceof OpenEvent) {
            OpenEvent open_evt = (OpenEvent) evt;
            if (open_evt.isOpen() && (rootBox.getItemCount() == 0)) {
                initRootItems(rootBox);
            }
        } else
        if (evt instanceof SelectEvent) {
            SelectEvent sel_evt = (SelectEvent) evt;
            Iterator it = sel_evt.getSelectedItems().iterator();
            if (it.hasNext()) {
                Comboitem sel_item = (Comboitem) it.next();
                String root_id = (String) sel_item.getValue();
                if (rootBox == contentRootBox) { 
                    contentRootId = root_id;
                } else 
                if (rootBox == prefaceRootBox) { 
                    prefaceRootId = root_id;
                } else 
                if (rootBox == appendixRootBox) { 
                    appendixRootId = root_id;
                } else { 
                    return;  // should not occur
                }
                rootBox.getItems().clear();
            }
        }
    }
    
    public void onEditFilterSetting() throws Exception
    {
        FilterSettingModel fsm = new FilterSettingModel("", filterBox.getValue());
        FilterSettingDialog dialog = (FilterSettingDialog) getPage().getFellow("FilterSettingDialog");
        dialog.setMode_EditFilter();
        if (dialog.doEditFilter(fsm, docmaSess)) {
            filterBox.setValue(fsm.getApplicsAsString());
        }
    }

    public void onEditReferencedPublications() throws Exception
    {
        ReferencedPubsModel rm = new ReferencedPubsModel(referencedPubsBox.getValue());
        ReferencedPubsDialog dialog = (ReferencedPubsDialog) getPage().getFellow("ReferencedPubsDialog");
        if (dialog.doEdit(rm, idBox.getValue().trim(), docmaSess)) {
            referencedPubsBox.setValue(rm.getIdsAsString());
        }
    }

    public void onChangeTitlePage1(Event evt) throws Exception
    {
        onChangeContentAlias(evt, customTitlePage1Box);
    }

    public void onChangeTitlePage2(Event evt) throws Exception
    {
        onChangeContentAlias(evt, customTitlePage2Box);
    }

    public void onChangeAbstract(Event evt) throws Exception
    {
        onChangeContentAlias(evt, abstractBox);
    }

    public void onChangeLegalNotice(Event evt) throws Exception
    {
        onChangeContentAlias(evt, legalNoticeBox);
    }

    public void onCheckCustTitlePage2(Event evt) throws Exception
    {
        boolean selfchecked = customTitlePage2Checkbox.isChecked();
        // if (is_unreg) {
        //     customTitlePage2Checkbox.setChecked(false);
        //     Messagebox.show("The title page backside can only be customized in the registered version of Docmenta!");
        //     return;
        // } else {
            customTitlePage2Box.setDisabled(! selfchecked);
        // }
    }

    public void onEditTitlePage1Node() throws Exception
    {
        editContentNode(customTitlePage1Box.getValue());
    }

    public void onEditTitlePage2Node() throws Exception
    {
        editContentNode(customTitlePage2Box.getValue());
    }

    public void onEditAbstractNode() throws Exception
    {
        editContentNode(abstractBox.getValue());
    }

    public void onEditLegalNoticeNode() throws Exception
    {
        editContentNode(legalNoticeBox.getValue());
    }

    
    /* -----------  Private methods  ------------------ */

    private MainWindow getMainWindow()
    {
        DocmaWebSession webSess = GUIUtil.getDocmaWebSession(this);
        return webSess.getMainWindow();
    }
    
    private void editContentNode(String alias) throws Exception
    {
        alias = alias.trim();
        if (alias.equals("")) {
            Messagebox.show("Please enter the alias name of a content node!");
            return;
        }
        DocmaNode nd = getValidContentNode(alias);  // shows error and returns null if no valid alias
        if (nd != null) {
            MainWindow mainWin = getMainWindow();
            mainWin.doEditContent(nd);
        }
    }

    private void onChangeContentAlias(Event evt, Combobox cbox) throws Exception
    {
        String val = getInputValue(evt);
        if (val != null) {
            tempList.clear();
            if (val.length() > 0) {
                DocmaAppUtil.listAliasesStartWith(val, getContentAliases(), tempList);
            }
            int item_cnt = cbox.getItemCount();
            if ((item_cnt > 0) && tempList.size() == 1) {  // exact match
                return;  // do nothing; user is scrolling through the list
            }
            cbox.getItems().clear();
            int sz = tempList.size();
            int max = (sz > 20) ? 20 : sz;
            for (int i=0; i < max; i++) {
                cbox.appendItem((String) tempList.get(i));
            }
            if (sz > max) {
                cbox.appendItem("...");
            }
        }
    }

    private String getInputValue(Event evt)
    {
        if (evt instanceof ForwardEvent) {
            evt = ((ForwardEvent) evt).getOrigin();
        }
        if (evt instanceof InputEvent) {
            InputEvent ievt = (InputEvent) evt;
            String val = ievt.getValue(); // customTitlePage1Box.getValue().trim();
            return (val == null) ? "" : val.trim();
        } else {
            return null;
        }
    }

    private void init()
    {
        idBox = (Textbox) getFellow("PubConfigIDTextbox");
        contentRootBox = (Combobox) getFellow("PubConfigContentRootCombobox");
        prefaceRootBox = (Combobox) getFellow("PubConfigPrefaceRootCombobox");
        appendixRootBox = (Combobox) getFellow("PubConfigAppendixRootCombobox");
        coverBox = (Textbox) getFellow("PubConfigCoverImageTextbox");
        filterBox = (Textbox) getFellow("PubConfigFilterSettingTextbox");
        referencedPubsBox = (Textbox) getFellow("PubConfigRefPubsTextbox");
        customTitlePage1Checkbox = (Checkbox) getFellow("PubConfigCustTitlePage1Checkbox");
        customTitlePage1Box = (Combobox) getFellow("PubConfigCustTitlePage1Combobox");
        customTitlePage2Checkbox = (Checkbox) getFellow("PubConfigCustTitlePage2Checkbox");
        customTitlePage2Box = (Combobox) getFellow("PubConfigCustTitlePage2Combobox");
        titleBox = (Textbox) getFellow("PubConfigTitleTextbox");
        subtitleBox = (Textbox) getFellow("PubConfigSubTitleTextbox");
        releaseInfoBox = (Textbox) getFellow("PubConfigReleaseInfoTextbox");
        corporateBox = (Textbox) getFellow("PubConfigCorporateTextbox");
        authorsBox = (Textbox) getFellow("PubConfigAuthorsTextbox");
        pubDateBox = (Textbox) getFellow("PubConfigPublicationDateTextbox");
        copyrightYearBox = (Textbox) getFellow("PubConfigCopyrightYearTextbox");
        copyrightHolderBox = (Textbox) getFellow("PubConfigCopyrightHolderTextbox");
        abstractBox = (Combobox) getFellow("PubConfigAbstractCombobox");
        legalNoticeBox = (Combobox) getFellow("PubConfigLegalNoticeCombobox");
        creditBox = (Textbox) getFellow("PubConfigCreditTextbox");
        publisherBox = (Textbox) getFellow("PubConfigPublisherTextbox");
        biblioIdBox = (Textbox) getFellow("PubConfigBiblioIDTextbox");
    }

    private List getContentAliases()
    {
        if (contentAliases == null) {
            contentAliases = docmaSess.listHTMLContentAliases();
        }
        return contentAliases;
    }

    private int getModalResult()
    {
        return modalResult;
    }

    private boolean hasInvalidInputs() throws Exception
    {
        String pubId = idBox.getValue().trim();
        if (pubId.equals("")) {
            Messagebox.show("Please enter an ID value.");
            return true;
        }
        if (pubId.length() > 30) {
            Messagebox.show("ID is too long. Maximum length is 30 characters.");
            return true;
        }
        if (! pubId.matches(DocmaConstants.REGEXP_ID)) {
            Messagebox.show("Invalid ID. Allowed characters are ASCII letters, underscore and dash.");
            return true;
        }
        if (mode == MODE_NEW) {
            String[] ids = docmaSess.getPublicationConfigIds();
            for (int i=0; i < ids.length; i++) {
                if (pubId.equalsIgnoreCase(ids[i])) {
                    Messagebox.show("A publication configuration with this ID already exists.");
                    return true;
                }
            }
        }
        String title = titleBox.getValue().trim();
        // if (title.equals("")) {
        //     Messagebox.show("Please enter a title.");
        //     return true;
        // }
        if (title.length() > 100) {
            Messagebox.show("Title is too long. Maximum length is 100 characters.");
            return true;
        }
        // if (! title.matches(DocmaConstants.REGEXP_NAME)) {
        //     Messagebox.show("Invalid title. Allowed characters are letters, punctuation characters and space.");
        //     return true;
        // }
        String filter = filterBox.getValue().trim();
        if (filter.length() > 0) {
            String[] applics = filter.split("[, ]+");
            List declaredList = Arrays.asList(docmaSess.getDeclaredApplics());
            for (int i=0; i < applics.length; i++) {
                if (! declaredList.contains(applics[i])) {
                    Messagebox.show("Invalid filter setting. Applicability '" + applics[i] + "' is not declared.");
                    return true;
                }
            }
        }
        if ((contentRootId != null) && (contentRootId.length() > 0) && !nodeIsSection(contentRootId)) {
            Messagebox.show("The content root has to be a section node!");
            return true;
        }
        if ((prefaceRootId != null) && (prefaceRootId.length() > 0) && !nodeIsSection(prefaceRootId)) {
            Messagebox.show("The preface root has to be a section node!");
            return true;
        }
        if ((appendixRootId != null) && (appendixRootId.length() > 0) && !nodeIsSection(appendixRootId)) {
            Messagebox.show("The appendix root has to be a section node!");
            return true;
        }
        if (invalidImageAlias(coverBox.getValue().trim())) {
            return true;
        }
        if (customTitlePage1Checkbox.isChecked()) {
            String alias = customTitlePage1Box.getValue().trim();
            if (invalidContentAlias(alias)) return true;
        }
        if (customTitlePage2Checkbox.isChecked()) {
            String alias = customTitlePage2Box.getValue().trim();
            if (invalidContentAlias(alias)) return true;
        }
        String abstract_alias = abstractBox.getValue().trim();
        if (abstract_alias.length() > 0) {
            if (invalidContentAlias(abstract_alias)) return true;
        }
        String legal_alias = legalNoticeBox.getValue().trim();
        if (legal_alias.length() > 0) {
            if (invalidContentAlias(legal_alias)) return true;
        }
        return false;
    }
    
    private void checkIndexTermOnTitlePage()
    {
        try {
            if (customTitlePage1Checkbox.isChecked()) {
                contentHasIndexTerm(customTitlePage1Box.getValue().trim());
            }
            if (customTitlePage2Checkbox.isChecked()) {
                contentHasIndexTerm(customTitlePage2Box.getValue().trim());
            }
            contentHasIndexTerm(abstractBox.getValue().trim());
            contentHasIndexTerm(legalNoticeBox.getValue().trim());
        } catch (Exception ex) {
            Messagebox.show("Error: " + ex.getMessage());
        }
    }
    
    private boolean contentHasIndexTerm(String alias) throws Exception
    {
        if ((alias == null) || (alias.length() == 0)) {
            return false;
        }
        DocmaNode nd = docmaSess.getNodeByAlias(alias);
        if (nd == null) {
            return false;
        }
        String xml_cont = ContentResolver.getContentRecursive(nd);
        final String INDEX_PATT = "(\\S*\\s+)*indexterm(\\s+\\S*)*";
        boolean res = XMLUtil.attributeValueExists(xml_cont, "span", "class", INDEX_PATT);
        if (res) {
            Messagebox.show("The content node '" + alias + "' contains index terms. \n" + 
                            "Index terms on the title page may cause errors during export.");
        }
        return res;
    }
    
    private boolean invalidContentAlias(String alias) throws Exception
    {
        if (alias.length() > 0) {
            DocmaNode nd = getValidContentNode(alias);  // shows error message if no valid alias
            if (nd == null) {
                return true;
            }
        }
        return false;
    }

    private boolean invalidImageAlias(String alias) throws Exception
    {
        if (alias.length() > 0) {
            DocmaNode nd = docmaSess.getNodeByAlias(alias);
            if (nd == null) {
                Messagebox.show("Image with alias '" + alias + "' not found.");
                return true;
            }
            if (nd.isFileContent()) {
                String fext = nd.getFileExtension();
                if ((fext == null) || !ImageUtil.isSupportedImageExtension(fext)) {
                    Messagebox.show("File with alias '" + alias + "' is no valid image file.");
                    return true;
                }
            } else
            if (! (nd.isImageContent())) {
                Messagebox.show("Node with alias '" + alias + "' is not an image.");
                return true;
            }
        }
        return false;
    }

    private DocmaNode getValidContentNode(String alias) 
    {
        DocmaNode[] nds = docmaSess.getNodesByLinkAlias(alias);
        if ((nds == null) || (nds.length == 0)) {
            Messagebox.show("Node with alias name '" + alias + "' not found.");
            return null;
        }
        DocmaNode nd = nds[0];
        if (! nd.isHTMLContent()) {
            Messagebox.show("Node with alias name '" + alias + "' has wrong node type. Must be a content node.");
            return null;
        }
        return nd;
    }

    private boolean nodeIsSection(String nodeId) 
    {
        DocmaNode node = docmaSess.getNodeById(nodeId);
        return (node != null) && node.isSection();
    }

    private void initRootItems(Combobox rootBox)
    {
        String rootId = null;
        if (rootBox == contentRootBox) rootId = contentRootId;
        else if (rootBox == prefaceRootBox) rootId = prefaceRootId;
        else if (rootBox == appendixRootBox) rootId = appendixRootId;
        else return;  // should not occur

        if (rootBox.getItemCount() > 0) rootBox.getItems().clear();

        if (rootBox != contentRootBox) {
            // Add empty item, so that user is able to delete preface/appendix
            Comboitem empty_item = new Comboitem("");
            empty_item.setValue("");
            rootBox.appendChild(empty_item);
        }

        DocmaNode node = null;
        if ((rootId == null) || rootId.trim().equals("")) {
            node = docmaSess.getDocumentRoot();
        } else {
            node = docmaSess.getNodeById(rootId);
        }
        if (node == null) return;
        if (! node.isDocumentRoot()) {
            DocmaNode parentNode = node.getParent();
            Comboitem item = new Comboitem(parentNode.getTitle());
            // item.setDescription("Parent folder");
            item.setValue(parentNode.getId());
            item.setImage("img/folder_up_icon.png");
            rootBox.appendChild(item);
        }
        int childcnt = node.getChildCount();
        for (int i=0; i < childcnt; i++) {
            DocmaNode child = node.getChild(i);
            boolean is_sect = child.isSection();
            if (is_sect || child.isFileFolder()) {
                Comboitem item = new Comboitem(child.getTitle());
                item.setValue(child.getId());
                String img_url = is_sect ? "img/section_icon.gif" : "img/file_folder_icon.gif";
                item.setImage(img_url);
                rootBox.appendChild(item);
            }
        }
    }

    private void updateGUI(DocmaPublicationConfig pubConf, DocmaSession docmaSess)
    {
        this.docmaSess = docmaSess;
        // FeatureManager fm = GUIUtil.getDocmaWebApplication(this).getFeatureManager();
        // this.is_unreg = (fm.readKeyString() == null);

        idBox.setValue(pubConf.getId());
        contentRootId = pubConf.getContentRoot();
        prefaceRootId = pubConf.getPrefaceRoot();
        appendixRootId = pubConf.getAppendixRoot();
        DocmaNode rootNode;
        if ((contentRootId == null) || contentRootId.equals("")) {
            rootNode = docmaSess.getDocumentRoot();
            contentRootId = rootNode.getId();
        } else {
            rootNode = docmaSess.getNodeById(contentRootId);
            if (rootNode == null) {
                rootNode = docmaSess.getDocumentRoot();
                contentRootId = rootNode.getId();
            }
        }
        DocmaNode prefaceRootNode = null;
        if ((prefaceRootId != null) && !prefaceRootId.equals("")) {
            prefaceRootNode = docmaSess.getNodeById(prefaceRootId);
        }
        DocmaNode appendixRootNode = null;
        if ((appendixRootId != null) && !appendixRootId.equals("")) {
            appendixRootNode = docmaSess.getNodeById(appendixRootId);
        }

        contentRootBox.removeEventListener("onOpen", this);
        contentRootBox.removeEventListener("onSelect", this);
        contentRootBox.setValue(rootNode.getTitle());
        contentRootBox.addEventListener("onOpen", this);
        contentRootBox.addEventListener("onSelect", this);

        prefaceRootBox.removeEventListener("onOpen", this);
        prefaceRootBox.removeEventListener("onSelect", this);
        String pref_val = (prefaceRootNode == null) ? "" : prefaceRootNode.getTitle();
        prefaceRootBox.setValue(pref_val);
        prefaceRootBox.addEventListener("onOpen", this);
        prefaceRootBox.addEventListener("onSelect", this);

        appendixRootBox.removeEventListener("onOpen", this);
        appendixRootBox.removeEventListener("onSelect", this);
        String append_val = (appendixRootNode == null) ? "" : appendixRootNode.getTitle();
        appendixRootBox.setValue(append_val);
        appendixRootBox.addEventListener("onOpen", this);
        appendixRootBox.addEventListener("onSelect", this);

        coverBox.setValue(pubConf.getCoverImageAlias());
        filterBox.setValue(pubConf.getFilterSetting());
        
        ReferencedPubsModel rm = new ReferencedPubsModel(pubConf.getReferencedPubs());
        rm.retainIds(docmaSess.getPublicationConfigIds());  // remove publication config ids that no longer exist
        referencedPubsBox.setValue(rm.getIdsAsString());

        String custTitleP1 = pubConf.getCustomTitlePage1();
        boolean isCustTitleP1 = (custTitleP1 != null) && !custTitleP1.trim().equals("");
        customTitlePage1Checkbox.setChecked(isCustTitleP1);
        customTitlePage1Box.setValue(isCustTitleP1 ? custTitleP1 : "");
        customTitlePage1Box.setDisabled(!isCustTitleP1);

        String custTitleP2 = pubConf.getCustomTitlePage2();
        boolean isCustTitleP2 = (custTitleP2 != null) && !custTitleP2.trim().equals("");
        // if (is_unreg) isCustTitleP2 = false;
        customTitlePage2Checkbox.setChecked(isCustTitleP2);
        customTitlePage2Box.setValue(isCustTitleP2 ? custTitleP2 : "");
        customTitlePage2Box.setDisabled(!isCustTitleP2);
        titleBox.setValue(pubConf.getTitle());
        subtitleBox.setValue(pubConf.getSubtitle());
        releaseInfoBox.setValue(pubConf.getReleaseInfo());
        corporateBox.setValue(pubConf.getCorporate());
        authorsBox.setValue(pubConf.getAuthors());
        pubDateBox.setValue(pubConf.getPubDate());
        copyrightYearBox.setValue(pubConf.getCopyrightYear());
        copyrightHolderBox.setValue(pubConf.getCopyrightHolder());
        abstractBox.setValue(pubConf.getPubAbstract());
        legalNoticeBox.setValue(pubConf.getLegalNotice());
        String credit_str = pubConf.getCredit();
        // if (is_unreg) {
        //     credit_str = DocmaConstants.DISPLAY_APP_CREDIT;
        //     creditBox.addEventListener("onClick", this);
        // }
        creditBox.setValue(credit_str);
        // creditBox.setReadonly(is_unreg);
        publisherBox.setValue(pubConf.getPublisher());
        biblioIdBox.setValue(pubConf.getBiblioId());
    }

    private void updateModel(DocmaPublicationConfig pubConf) throws Exception
    {
        String old_id = pubConf.getId();
        String new_id = idBox.getValue().trim();
        pubConf.setId(new_id);
        pubConf.setContentRoot(contentRootId);
        pubConf.setPrefaceRoot(prefaceRootId);
        pubConf.setAppendixRoot(appendixRootId);
        pubConf.setCoverImageAlias(coverBox.getValue().trim());
        pubConf.setFilterSetting(filterBox.getValue());
        pubConf.setReferencedPubs(referencedPubsBox.getValue());

        String custTitleP1Alias = "";
        if (customTitlePage1Checkbox.isChecked()) {
            custTitleP1Alias = customTitlePage1Box.getValue().trim();
        }
        pubConf.setCustomTitlePage1(custTitleP1Alias);

        String custTitleP2Alias = "";
        if (customTitlePage2Checkbox.isChecked()) {
            custTitleP2Alias = customTitlePage2Box.getValue().trim();
        }
        pubConf.setCustomTitlePage2(custTitleP2Alias);

        pubConf.setTitle(titleBox.getValue().trim());
        pubConf.setSubtitle(subtitleBox.getValue().trim());
        pubConf.setReleaseInfo(releaseInfoBox.getValue());
        pubConf.setCorporate(corporateBox.getValue());
        pubConf.setAuthors(authorsBox.getValue());
        pubConf.setPubDate(pubDateBox.getValue());
        pubConf.setCopyrightYear(copyrightYearBox.getValue());
        pubConf.setCopyrightHolder(copyrightHolderBox.getValue());
        pubConf.setPubAbstract(abstractBox.getValue());
        pubConf.setLegalNotice(legalNoticeBox.getValue());
        pubConf.setCredit(creditBox.getValue());
        pubConf.setPublisher(publisherBox.getValue());
        pubConf.setBiblioId(biblioIdBox.getValue().trim());

        docmaSess.savePublicationConfig(pubConf);
        if (mode == MODE_EDIT) {
            if (! new_id.equals(old_id)) {
                docmaSess.deletePublicationConfig(old_id);
            }
        }
    }

}
