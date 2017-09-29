/*
 * FindNodesComposer.java
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

import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.docma.app.*;
import org.docma.coreapi.DocI18n;
import org.docma.plugin.web.ButtonType;
import org.docma.plugin.web.UIEvent;
import org.docma.plugin.web.UIListener;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.*;
import org.zkoss.zul.*;


/**
 *
 * @author MP
 */
public class FindNodesComposer extends SelectorComposer<Component> implements ListitemRenderer
{
    private static final int MODE_BY_ALIAS = 1;
    private static final int MODE_REFERENCING_ALIAS = 2;
    private static final int MODE_STYLE = 3;
    private static final int MODE_APPLIC = 4;

    private static final int DIALOG_STATE_BEFORE_SEARCH = 1;
    private static final int DIALOG_STATE_EXECUTE_SEARCH = 2;
    
    private static final int BATCH_SIZE = 50;  // max number of nodes to be processed in one call

    private int mode;
    private int dialog_state;

    @Wire("#FindNodesDialog") Window dialog;
    @Wire("#FindNodesSearchTermLabel") Label searchTermLabel;
    @Wire("#FindNodesSearchSummaryLabel") Label searchSummaryLabel;
    @Wire("#FindNodesSearchTermTextbox") Combobox searchTermBox;
    @Wire("#FindNodesSearchResultList") Listbox resultListbox;
    @Wire("#FindNodesStartButton") Button startSearchBtn;
    @Wire("#FindNodesReplaceBar") Component replaceBar;
    @Wire("#FindNodesReplaceSummary") Label replaceLabel;
    @Wire("#FindNodesReplaceTextbox") Combobox replaceTextbox;
    @Wire("#FindNodesReplaceButton") Button replaceBtn;

    // Fields used for user interface
    private ListModelList resultListModel = null;
    private final List<String> tempComboValues = new ArrayList(1000);
    private List<String> nodeAliases = null;
    private List<String> styleIds = null;
    private List<String> applicTerms = null;

    // Fields used during search execution
    private String searchTerm = "";    // The term for which search is executed.
                                       // Depending on the search mode, this can
                                       // be an alias, a style-Id or an 
                                       // applicability-term.
    private DocmaNode searchRoot = null;
    private Deque<DocmaNode> searchStack = null;
    private List<DocmaNode> searchResult = null;
    private int searchProgressCount = 0;   // number of nodes processed
    
    private DocmaSession docmaSess;
    private DocmaWebSession docmaWebSess;


    public void doFindByAlias(DocmaSession docmaSess)
    throws Exception
    {
        doFindByAlias(docmaSess, "");
    }

    public void doFindByAlias(DocmaSession docmaSess, String alias)
    throws Exception
    {
        mode = MODE_BY_ALIAS;
        doFind(docmaSess, alias, null);
    }

    public void doFindReferencingAlias(DocmaSession docmaSess)
    throws Exception
    {
        doFindReferencingAlias(docmaSess, "", null);
    }
    
    public void doFindReferencingAlias(DocmaSession docmaSess, String alias, DocmaNode root)
    throws Exception
    {
        mode = MODE_REFERENCING_ALIAS;
        doFind(docmaSess, alias, root);
    }
    
    public void doFindStyle(DocmaSession docmaSess, DocmaNode root)
    throws Exception
    {
        mode = MODE_STYLE;
        doFind(docmaSess, "", root);
    }
    
    public void doFindApplic(DocmaSession docmaSess, DocmaNode root)
    throws Exception
    {
        mode = MODE_APPLIC;
        doFind(docmaSess, "", root);
    }
    
    public void closeDialog()
    {
        dialog.setVisible(false);
    }
    
    public boolean isDialogOpened()
    {
        return dialog.isVisible();
    }

    public void render(Listitem item, Object model, int index) throws Exception
    {
        if (model instanceof DocmaNode) {
            DocmaNode node = (DocmaNode) model;
            final String nodeId = node.getId();
            // item.setValue(pm.getId());
            
            Listcell c0 = new Listcell(node.getTitle());
            String icon_path = GUIUtil.getNodeIconPath(node);
            if (icon_path != null) {
                c0.setImage(icon_path);
            }
            
            Listcell c1 = new Listcell();
            
            Toolbarbutton previewBtn = new Toolbarbutton();
            previewBtn.setImage("img/preview.png");
            previewBtn.setTooltip("FindNodesPreviewPopup");
            previewBtn.addEventListener("onClick", new EventListener() {
                public void onEvent(Event t) throws Exception {
                    previewNode(nodeId);
                }
            });
            
            Toolbarbutton editBtn = new Toolbarbutton();
            editBtn.setImage("img/edit.gif");
            editBtn.setTooltip("FindNodesEditContentPopup");
            editBtn.addForward("onClick", dialog, "onEditNode");
            editBtn.addEventListener("onClick", new EventListener() {
                public void onEvent(Event t) throws Exception {
                    editNode(nodeId);
                }
            });
            
            Toolbarbutton propsBtn = new Toolbarbutton();
            propsBtn.setImage("img/edit_props.gif");
            propsBtn.setTooltip("FindNodesEditPropsPopup");
            propsBtn.addEventListener("onClick", new EventListener() {
                public void onEvent(Event t) throws Exception {
                    editNodeProps(nodeId);
                }
            });
            
            Hlayout hlay = new Hlayout();
            hlay.setSpacing("0px");
            hlay.appendChild(previewBtn);
            hlay.appendChild(editBtn);
            hlay.appendChild(propsBtn);
            c1.appendChild(hlay);
            
            Listcell c2 = new Listcell(node.getAlias());
            Listcell c3 = new Listcell(node.getApplicability());
            Listcell c4 = new Listcell(getNodePath(node));

            item.appendChild(c0);
            item.appendChild(c1);
            item.appendChild(c2);
            item.appendChild(c3);
            item.appendChild(c4);
            // item.addForward("onDoubleClick", dialog, "onPreviewSelNode");
        }
    }
    
    private String getNodePath(DocmaNode node)
    {
        StringBuilder buf = new StringBuilder();
        DocmaNode parentNode = node.getParent();
        while ((parentNode != null) && !parentNode.isRoot()) {
            if (buf.length() == 0) {
                buf.append(parentNode.getTitle());
            } else {
                buf.insert(0, parentNode.getTitle() + " > ");
            }
            parentNode = parentNode.getParent();
        }
        return buf.toString();
    }

//    @Listen("onPreviewSelNode = #FindNodesDialog")
//    public void onPreviewSelectedNode() throws Exception
//    {
//        int sel_idx = resultListbox.getSelectedIndex();
//        if ((sel_idx < 0) || (sel_idx >= resultListModel.size())) return;
//
//        DocmaNode sel_node = (DocmaNode) resultListModel.get(sel_idx);
//        previewNode(sel_node);
//    }
    
    private void previewNode(String nodeId) throws Exception
    {
        DocmaNode node = docmaSess.getNodeById(nodeId);
        MainWindow mainWin = docmaWebSess.getMainWindow();
        DocmaWebTree webtree = mainWin.getDocTree();
        Treeitem item = webtree.getTreeitemByDocmaNode(node);
        if (item == null) {  // node not loaded yet
            TreeModel tmodel = webtree.getModel();
            int[] path = tmodel.getPath(node);
            item = webtree.renderItemByPath(path);
        }
        if (item != null) {
            webtree.setSelectedItem(item);  // select node
            mainWin.onDocTreeSelect();  // preview selected node
        } else {
            Messagebox.show("Error: Selected node no longer exists!");
        }
    }

    private void editNode(String nodeId) throws Exception
    {
        DocmaNode node = docmaSess.getNodeById(nodeId);
        MainWindow mainWin = docmaWebSess.getMainWindow();
        mainWin.doEditContent(node);
    }
    
    private void editNodeProps(String nodeId) throws Exception
    {
        DocmaNode node = docmaSess.getNodeById(nodeId);
        MainWindow mainWin = docmaWebSess.getMainWindow();
        mainWin.doEditNodeProps(node, null);
    }
    
    @Listen("onClick = #FindNodesStartButton; onOK = #FindNodesSearchTermTextbox")
    public void onStartSearchClick() throws Exception
    {
        startSearch();
    }
    
    @Listen("onClick = #FindNodesReplaceButton")
    public void onReplaceClick() throws Exception
    {
        final Set selNodes = resultListModel.getSelection();
        if ((selNodes == null) || selNodes.isEmpty()) {
            return;
        }
        final List<String> selIds = new ArrayList<String>();
        for (Object obj : selNodes) {
            if (obj instanceof DocmaNode) {
                selIds.add(((DocmaNode) obj).getId());
            }
        }
        final String searchVal = searchTermBox.getValue().trim();
        final String replaceVal = replaceTextbox.getValue().trim();
        boolean isValid = false;
        String transMsg = null;
        if (mode == MODE_REFERENCING_ALIAS) {
            isValid = checkValidLinkAlias(replaceVal);
            transMsg = "label.findnodes.replace_refs_in_translations";
        } else if (mode == MODE_STYLE) {
            isValid = checkValidStyleId(replaceVal);
            transMsg = "label.findnodes.replace_style_in_translations";
        }
        if (isValid) {
            if (! docmaSess.clearFinishedUserActivities()) {
                MessageUtil.showError(dialog, "activity.window.user_activity_exists");
                return;
            }
            
            String transMode = docmaSess.getTranslationMode();
            DocmaLanguage[] transLangs = docmaSess.getTranslationLanguages();
            if ((transMode == null) && (transLangs != null) && (transLangs.length > 0)) {
                // If session is in original mode and translation languages 
                // exist, then ask user whether translated content shall be
                // processed as well. 
                MessageUtil.showYesNoQuestion(dialog, 
                    "label.findnodes.replace_in_translations.title", transMsg, null, 
                    new UIListener() {
                        public void onEvent(UIEvent evt) {
                            if (evt.isButtonTarget() && evt.isClick()) {
                                boolean trans = ButtonType.YES.equals(evt.getButtonType());
                                startReplace(selIds, searchVal, replaceVal, trans);
                            }
                        }
                    });
            } else {
                // If no translation languages exist, or session is currently
                // in translation mode, then only process content for the
                // current language.
                startReplace(selIds, searchVal, replaceVal, false);
            }
        }
    }

    @Listen("onClick = #FindNodesCloseButton")
    public void onCloseClick() throws Exception
    {
        closeDialog();
    }

    @Listen("onChanging = #FindNodesSearchTermTextbox")
    public void onSearchTermChange(Event evt) throws Exception
    {
        String new_term = getChangingComboValue(searchTermBox, evt).trim();
        // The field searchTerm contains the search-term of the previous 
        // search (or an empty string, if no search has been executed yet). 
        // If a new search-term is entered, then the result of the previous 
        // search is cleared.
        if (! new_term.equals(searchTerm)) {
            dialog_state = DIALOG_STATE_BEFORE_SEARCH;
            if (! resultListModel.isEmpty()) {
                clearResultList();
            }
        }
        updateComboValues(searchTermBox, new_term);
    }

    @Listen("onChanging = #FindNodesReplaceTextbox")
    public void onChangingReplaceText(Event evt) throws Exception
    {
        updateComboValues(replaceTextbox, evt);
    }

    @Listen("onExecuteSearch = #FindNodesDialog")
    public void onExecuteSearch() throws Exception
    {
        searchTerm = searchTermBox.getValue().trim();
        if (mode == MODE_BY_ALIAS) {
            try {
                executeSearchByAlias();
            } finally {
                searchFinished();
            }
        } else {
            // For the following search modes, the nodes in the store
            // have to be traversed recursively.
            
            searchStack = new ArrayDeque<DocmaNode>();
            searchResult = new ArrayList<DocmaNode>();
            searchProgressCount = 0;
            try {
                DocmaNode root_nd = (searchRoot != null) ? searchRoot : docmaSess.getRoot();
                searchStack.addLast(root_nd);
            } catch (Throwable ex) {  // some unexpected problem occured
                searchFinished();
                MessageUtil.showError(dialog, ex.getMessage());
                return;
            }
            if (mode == MODE_REFERENCING_ALIAS) {
                onSearchRefAliasRecursive();
            } else if (mode == MODE_STYLE) {
                onSearchStyleRecursive();
            } else if (mode == MODE_APPLIC) {
                onSearchApplicRecursive();
            } else {
                throw new Exception("Invalid dialog mode: " + mode);
            }
        }
    }
    
    @Listen("onSelect = #FindNodesSearchResultList")
    public void onResultListSelection() throws Exception
    {
        updateReplaceBar();
    }

    @Listen("onSearchRefAliasRecursive = #FindNodesDialog")
    public void onSearchRefAliasRecursive()
    {
        try {
            if (! dialog.isVisible()) {  // user has closed the dialog?
                return;  // cancel search
            }
            if (searchStack == null) { // should never occur
                return;
            }
            int cnt = 0;
            DocmaNode node;
            while ((node = searchStack.pollLast()) != null) {
                if (node.isHTMLContent()) {
                    if (ContentUtil.isReferencingAlias(node.getContentString(), searchTerm)) {
                        searchResult.add(node);
                    }
                } else if (node.isReference()) {
                    String ref_alias = node.getReferenceTarget();
                    if (searchTerm.equals(ref_alias)) {
                        searchResult.add(node);
                    }
                } else if (node.isChildable()) {
                    DocmaNode[] childArr = node.getChildren();
                    for (int i = childArr.length - 1; i >= 0; i--) {
                        searchStack.addLast(childArr[i]);
                    }
                }
                searchProgressCount++;
                if (++cnt > BATCH_SIZE) {
                    break;
                }
            }

            if (searchStack.isEmpty()) {
                // Search finished -> update UI
                resultListModel.addAll(searchResult);
                updateReplaceBar();
                searchFinished();
            } else {
                // Process next batch of nodes
                updateSearchProgress();
                Events.echoEvent("onSearchRefAliasRecursive", dialog, null);
            }
        } catch (Throwable ex) {
            searchFinished();
            MessageUtil.showError(dialog, ex.getMessage());
        }
    }

    @Listen("onSearchStyleRecursive = #FindNodesDialog")
    public void onSearchStyleRecursive()
    {
        try {
            if (! dialog.isVisible()) {  // user has closed the dialog?
                return;  // cancel search
            }
            if (searchStack == null) { // should never occur
                return;
            }
            int cnt = 0;
            DocmaNode node;
            while ((node = searchStack.pollLast()) != null) {
                if (node.isHTMLContent()) {
                    if (ContentUtil.isReferencingStyle(node.getContentString(), searchTerm)) {
                        searchResult.add(node);
                    }
                } else if (node.isChildable()) {
                    DocmaNode[] childArr = node.getChildren();
                    for (int i = childArr.length - 1; i >= 0; i--) {
                        searchStack.addLast(childArr[i]);
                    }
                }
                searchProgressCount++;
                if (++cnt > BATCH_SIZE) {
                    break;
                }
            }

            if (searchStack.isEmpty()) {
                // Search finished -> update UI
                resultListModel.addAll(searchResult);
                updateReplaceBar();
                searchFinished();
            } else {
                // Process next batch of nodes
                updateSearchProgress();
                Events.echoEvent("onSearchStyleRecursive", dialog, null);
            }
        } catch (Throwable ex) {
            searchFinished();
            MessageUtil.showError(dialog, ex.getMessage());
        }
    }

    @Listen("onSearchApplicRecursive = #FindNodesDialog")
    public void onSearchApplicRecursive()
    {
        try {
            if (! dialog.isVisible()) {  // user has closed the dialog?
                return;  // cancel search
            }
            if (searchStack == null) { // should never occur
                return;
            }
            int cnt = 0;
            DocmaNode node;
            while ((node = searchStack.pollLast()) != null) {
                String applic = node.getApplicability();
                if ((applic != null) && applic.contains(searchTerm)) {
                    StringTokenizer tokens = new StringTokenizer(applic, " (),|-");
                    while (tokens.hasMoreTokens()) {
                        String tok = tokens.nextToken();
                        if (tok.equals(searchTerm)) {
                            searchResult.add(node);
                            break;
                        }
                    }
                }
                if (node.isChildable()) {
                    DocmaNode[] childArr = node.getChildren();
                    for (int i = childArr.length - 1; i >= 0; i--) {
                        searchStack.addLast(childArr[i]);
                    }
                }
                searchProgressCount++;
                if (++cnt > BATCH_SIZE) {
                    break;
                }
            }

            if (searchStack.isEmpty()) {
                // Search finished -> update UI
                resultListModel.addAll(searchResult);
                searchFinished();
            } else {
                // Process next batch of nodes
                updateSearchProgress();
                Events.echoEvent("onSearchApplicRecursive", dialog, null);
            }
        } catch (Throwable ex) {
            searchFinished();
            MessageUtil.showError(dialog, ex.getMessage());
        }
    }

    /* --------------  Private methods  ---------------------- */

    private void updateComboValues(Combobox box, Event evt)
    {
        String val = getChangingComboValue(box, evt).trim();
        updateComboValues(box, val);
    }
    
    private void updateComboValues(Combobox box, String val)
    {
        final String MORE = "...";
        tempComboValues.clear();
        if (val.equals(MORE)) {
            return;  // do nothing; user has reached the end of the list (more entry)
        }
        if (val.length() > 0) {
            List allValues;
            if ((mode == MODE_BY_ALIAS) || (mode == MODE_REFERENCING_ALIAS)) {
                allValues = getNodeAliases();
            } else if (mode == MODE_STYLE) {
                allValues = getStyleIds();
            } else if (mode == MODE_APPLIC) {
                allValues = getDeclaredApplics();
            } else {  // should never occur
                allValues = new ArrayList();
            }
            // Note: allValues needs to be sorted!
            DocmaAppUtil.listValuesStartWith(val, allValues, tempComboValues);
        }
        int item_cnt = box.getItemCount();
        if ((item_cnt > 0) && tempComboValues.size() == 1) {  // exact match
            return;  // do nothing; user is scrolling through the list
        }
        box.getItems().clear();
        int sz = tempComboValues.size();
        int max = (sz > 20) ? 20 : sz;
        for (int i=0; i < max; i++) {
            box.appendItem((String) tempComboValues.get(i));
        }
        if (sz > max) {
            box.appendItem(MORE);
        }
    }
    
    private List<String> getNodeAliases()
    {
        if (nodeAliases == null) {
            nodeAliases = docmaSess.listNodeAliases();
        }
        return nodeAliases;
    }

    private List<String> getStyleIds()
    {
        if (styleIds == null) {
            String[] arr = docmaSess.getStyleIds();
            Arrays.sort(arr);
            styleIds = Arrays.asList(arr);
        }
        return styleIds;
    }
    
    private List<String> getDeclaredApplics()
    {
        if (applicTerms == null) {
            String[] arr = docmaSess.getDeclaredApplics();
            Arrays.sort(arr);
            applicTerms = Arrays.asList(arr);
        }
        return applicTerms;
    }
    
    private String getChangingComboValue(Combobox box, Event evt)
    {
        if (evt instanceof InputEvent) {
            InputEvent ievt = (InputEvent) evt;
            String val = ievt.getValue(); // customTitlePage1Box.getValue().trim();
            return (val == null) ? "" : val;
        } else {
            return box.getValue();
        }
    }

    private void doFind(DocmaSession docmaSess, String search_term, DocmaNode rootNode)
    throws Exception
    {
        this.docmaSess = docmaSess;
        this.docmaWebSess = GUIUtil.getDocmaWebSession(dialog);
        this.searchRoot = rootNode;
        DocI18n i18n = docmaSess.getI18n();
        
        if (rootNode != null) {
            dialog.setTitle(i18n.getLabel("label.findnodes.dialog.title_subtree", rootNode.getTitle()));
        } else {
            dialog.setTitle(i18n.getLabel("label.findnodes.dialog.title"));
        }

        String term_label;
        if (mode == MODE_BY_ALIAS) {
            term_label = i18n.getLabel("label.findnodes.searchterm.byalias");
        } else
        if (mode == MODE_REFERENCING_ALIAS) {
            term_label = i18n.getLabel("label.findnodes.searchterm.refthis");
        } else
        if (mode == MODE_STYLE) {
            term_label = i18n.getLabel("label.findnodes.searchterm.style");
        } else
        if (mode == MODE_APPLIC) {
            term_label = i18n.getLabel("label.findnodes.searchterm.applic");
        } else {
            term_label = "???";
        }
        if (! term_label.endsWith(":")) {
            term_label = term_label.trim() + ":";
        }
        searchTermLabel.setValue(term_label);

        startSearchBtn.setDisabled(false);
        searchTermBox.setDisabled(false);
        if (searchTermBox.getItemCount() > 0) {
            searchTermBox.getItems().clear();
        }
        searchTermBox.setValue(search_term);

        // initialize result list
        boolean allowReplace = (mode == MODE_REFERENCING_ALIAS) || 
                               (mode == MODE_STYLE);
        resultListbox.setCheckmark(allowReplace);
        resultListbox.setItemRenderer(this);
        if (resultListModel == null) {
            resultListModel = new ListModelList();
        }
        resultListbox.setModel(resultListModel);
        resultListModel.setMultiple(allowReplace);

        // MainWindow mainwin = docmaWebSess.getMainWindow();
        dialog.setLeft("10px");
        dialog.setTop("10px");
        
        // Clear cached combobox values
        nodeAliases = null;
        styleIds = null;
        applicTerms = null;

        // doModal();
        // doOverlapped();
        dialog.setVisible(true);
        dialog.setFocus(true);
        searchTermBox.setFocus(true);

        startSearch();
    }

    private void startSearch() throws Exception
    {
        dialog_state = DIALOG_STATE_BEFORE_SEARCH;
        clearResultList();
        hideReplaceBar();

        String sterm = searchTermBox.getValue().trim();
        if (sterm.equals("")) {
            return;
        }
        
        dialog_state = DIALOG_STATE_EXECUTE_SEARCH;
        searchTermBox.setDisabled(true);
        startSearchBtn.setDisabled(true);
        updateSearchProgress();
        
        // Use echo event to update UI before starting search task
        Events.echoEvent("onExecuteSearch", dialog, null);
    }
    
    private void updateSearchProgress()
    {
        Integer found = (searchResult != null) ?  searchResult.size() : 0;
        String running_label = docmaSess.getI18n().getLabel("label.findnodes.searchrunning", searchProgressCount, found);
        searchSummaryLabel.setValue(running_label);
    }

    private void searchFinished()
    {
        dialog_state = DIALOG_STATE_BEFORE_SEARCH;  // user can start next search
        String sum_label = docmaSess.getI18n().getLabel("label.findnodes.searchsummary");
        Object[] args = {new Integer(resultListModel.size()), searchTerm };
        sum_label = MessageFormat.format(sum_label, args);
        searchSummaryLabel.setValue(sum_label);
        searchTermBox.setDisabled(false);
        startSearchBtn.setDisabled(false);
    }

    private void executeSearchByAlias()
    {
        if (! searchTerm.equals("")) {
            boolean exact_match = ! searchTerm.endsWith("*");
            if (exact_match) {
                DocmaNode[] res = docmaSess.getNodesByLinkAlias(searchTerm);
                if (res != null) {
                    resultListModel.addAll(Arrays.asList(res));
                }
            } else {
                String search_prefix = searchTerm.substring(0, searchTerm.length() - 1);  // strip "*"
                for (String alias : docmaSess.getNodeAliases()) {
                    if (alias.startsWith(search_prefix)) {
                        DocmaNode nd = docmaSess.getNodeByAlias(alias);
                        if (nd != null) resultListModel.add(nd);
                    }
                }
            }
        }
    }

    private void startReplace(List<String> nodeIds, 
                              String oldValue, 
                              String newValue, 
                              boolean replaceTranslations)
    {
        if (! docmaSess.clearFinishedUserActivities()) {
            MessageUtil.showError(dialog, "activity.window.user_activity_exists");
            return;
        }

        try {
            // Start activity thread
            Activity act = docmaSess.createDocStoreActivity(docmaSess.getStoreId(), docmaSess.getVersionId());
            String[] nIds = nodeIds.toArray(new String[nodeIds.size()]);
            Runnable thread_obj = null;
            if (mode == MODE_REFERENCING_ALIAS) {
                act.setTitle("replace_references.activity_title");
                thread_obj = 
                    new ReplaceReferencesRunnable(act, docmaSess, nIds, 
                                                  oldValue, newValue, replaceTranslations);
            } else if (mode == MODE_STYLE) {
                act.setTitle("replace_style.activity_title");
                thread_obj = 
                    new ReplaceStyleRunnable(act, docmaSess, nIds, 
                                             oldValue, newValue, replaceTranslations);
            }
            act.start(thread_obj);
            
            // Open activity window
            DocmaWebSession webSess = GUIUtil.getDocmaWebSession(dialog);
            MainWindow mainWin = webSess.getMainWindow();
            ActivityWinComposer actComp = mainWin.getActivityWinComposer();
            actComp.openWindow(act);
        } catch (Exception ex) {
            MessageUtil.showException(dialog, ex);
        }
    }
    
    private boolean checkValidLinkAlias(String alias)
    {
        if (alias.equals("")) {
            MessageUtil.showError(dialog, "label.findnodes.empty_replace_alias");
            return false;
        }
        if (! alias.matches(DocmaConstants.REGEXP_ALIAS)) {
            MessageUtil.showError(dialog, "label.findnodes.invalid_replace_alias");
            return false;
        }
        return true;
    }

    private boolean checkValidStyleId(String styleValue)
    {
        if (styleValue.equals("")) {
            MessageUtil.showError(dialog, "label.findnodes.empty_replace_style");
            return false;
        }
        if (! styleValue.matches(DocmaConstants.REGEXP_STYLE_BASE_ID)) {
            MessageUtil.showError(dialog, "label.findnodes.invalid_replace_style");
            return false;
        }
        return true;
    }
    
    private void clearResultList()
    {
        if (! resultListModel.isEmpty()) resultListModel.clear();
        searchSummaryLabel.setValue("");
        searchTerm = "";
        searchResult = null;
        searchProgressCount = 0;
    }
    
    private void hideReplaceBar()
    {
        replaceBtn.setDisabled(true);
        replaceTextbox.setDisabled(true);
        replaceBar.setVisible(false);
    }
    
    private void updateReplaceBar()
    {
        String sterm = searchTermBox.getValue().trim();
        DocI18n i18n = docmaSess.getI18n();
        String txt;
        if (mode == MODE_REFERENCING_ALIAS) {
            txt = i18n.getLabel("label.findnodes.replace_refs.summary", sterm);
        } else if (mode == MODE_STYLE) {
            txt = i18n.getLabel("label.findnodes.replace_style.summary", sterm);
        } else {
            txt = "";
        }
        Integer cnt = resultListbox.getSelectedCount();
        boolean visible = !sterm.equals("");
        replaceBar.setVisible(visible);
        replaceTextbox.setDisabled(! visible);
        replaceBtn.setDisabled(cnt == 0);
        replaceLabel.setValue(txt);
        replaceBtn.setLabel(i18n.getLabel("label.findnodes.replace.btn", cnt));
    }

}
