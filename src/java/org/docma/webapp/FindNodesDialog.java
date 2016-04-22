/*
 * FindNodesDialog.java
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
import org.zkoss.zk.ui.event.*;

import java.util.*;
import java.text.MessageFormat;

/**
 *
 * @author MP
 */
public class FindNodesDialog extends Window implements ListitemRenderer
{
    private static final int MODE_BY_ALIAS = 1;
    private static final int MODE_REFERENCING_ALIAS = 2;

    private static final int DIALOG_STATE_BEFORE_SEARCH = 1;
    private static final int DIALOG_STATE_START_SEARCH = 2;
    private static final int DIALOG_STATE_EXECUTE_SEARCH = 3;

    private int mode;
    private int dialog_state;

    private Label searchTermLabel;
    private Label searchSummaryLabel;
    private Textbox searchTermBox;
    private Listbox resultListbox;
    private Button startSearchBtn;

    private ListModelList resultListModel = null;
    private String old_search_term = "";

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
        doFind(docmaSess, alias);
    }

    public void doFindReferencingAlias(DocmaSession docmaSess, String alias)
    throws Exception
    {
        mode = MODE_REFERENCING_ALIAS;
        doFind(docmaSess, alias);
    }

    public void render(Listitem item, Object model, int index) throws Exception
    {
        if (model instanceof DocmaNode) {
            DocmaNode node = (DocmaNode) model;
            // item.setValue(pm.getId());
            Listcell c1 = new Listcell(node.getTitle());
            Listcell c2 = new Listcell(node.getAlias());
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
            Listcell c3 = new Listcell(buf.toString());

            String icon_path = GUIUtil.getNodeIconPath(node);
            if (icon_path != null) {
                c1.setImage(icon_path);
            }

            item.appendChild(c1);
            item.appendChild(c2);
            item.appendChild(c3);
            item.addForward("onDoubleClick", "FindNodesDialog", "onPreviewNode");
        }
    }

    public void onPreviewNode() throws Exception
    {
        int sel_idx = resultListbox.getSelectedIndex();
        if ((sel_idx < 0) || (sel_idx >= resultListModel.size())) return;

        DocmaNode sel_node = (DocmaNode) resultListModel.get(sel_idx);
        MainWindow mainWin = docmaWebSess.getMainWindow();
        DocmaWebTree webtree = mainWin.getDocTree();
        Treeitem item = webtree.getTreeitemByDocmaNode(sel_node);
        if (item == null) {  // node not loaded yet
            TreeModel tmodel = webtree.getModel();
            int[] path = tmodel.getPath(sel_node);
            item = webtree.renderItemByPath(path);
        }
        if (item != null) {
            webtree.setSelectedItem(item);  // select node
            mainWin.onDocTreeSelect();  // preview selected node
        } else {
            Messagebox.show("Error: Selected node no longer exists!");
        }
    }

    public void onStartSearchClick() throws Exception
    {
        startSearch();
    }

    public void onCloseClick() throws Exception
    {
        setVisible(false);
    }

    public void onSearchTermChange() throws Exception
    {
        String new_term = searchTermBox.getValue().trim();
        if (! new_term.equals(old_search_term)) {
            dialog_state = DIALOG_STATE_BEFORE_SEARCH;
            clearResultList();
        }
    }

    public void onExecuteSearch() throws Exception
    {
        executeSearch();
    }

    /* --------------  Private methods  ---------------------- */

    private void doFind(DocmaSession docmaSess, String search_term)
    throws Exception
    {
        this.docmaSess = docmaSess;
        this.docmaWebSess = GUIUtil.getDocmaWebSession(this);

        initFields();
        String term_label;
        if (mode == MODE_BY_ALIAS) {
            term_label = GUIUtil.i18(this).getLabel("label.findnodes.searchterm.byalias");
        } else
        if (mode == MODE_REFERENCING_ALIAS) {
            term_label = GUIUtil.i18(this).getLabel("label.findnodes.searchterm.refthis");
        } else {
            term_label = "???";
        }
        if (! term_label.endsWith(":")) term_label = term_label.trim() + ":";
        searchTermLabel.setValue(term_label);

        startSearchBtn.setDisabled(false);
        searchTermBox.setValue(search_term);

        // initialize result list
        resultListbox.setItemRenderer(this);
        if (resultListModel == null) {
            resultListModel = new ListModelList();
        }
        resultListbox.setModel(resultListModel);

        // MainWindow mainwin = docmaWebSess.getMainWindow();
        setLeft("10px");
        setTop("10px");

        // doModal();
        // doOverlapped();
        setVisible(true);
        setFocus(true);
        searchTermBox.setFocus(true);

        dialog_state = DIALOG_STATE_BEFORE_SEARCH;
        clearResultList();
        startSearch();
    }

    private void startSearch() throws Exception
    {
        if (searchTermBox.getValue().trim().equals("")) {
            return;
        }
        dialog_state = DIALOG_STATE_START_SEARCH;
        clearResultList();
        searchTermBox.setDisabled(true);
        startSearchBtn.setDisabled(true);
        String running_label = GUIUtil.i18(this).getLabel("label.findnodes.searchrunning");
        searchSummaryLabel.setValue(running_label);
        Events.echoEvent("onExecuteSearch", this, null);
    }

    private void executeSearch() throws Exception
    {
        dialog_state = DIALOG_STATE_EXECUTE_SEARCH;
        String search_term = searchTermBox.getValue().trim();
        try {
            if (mode == MODE_BY_ALIAS) {
                executeSearchByAlias(search_term);
            } else
            if (mode == MODE_REFERENCING_ALIAS) {
                executeSearchReferencingAlias(search_term);
            } else {
                throw new Exception("Invalid dialog mode: " + mode);
            }
        } finally {
            dialog_state = DIALOG_STATE_BEFORE_SEARCH;
            old_search_term = search_term;
            String sum_label = GUIUtil.i18(this).getLabel("label.findnodes.searchsummary");
            Object[] args = {new Integer(resultListModel.size()), search_term };
            sum_label = MessageFormat.format(sum_label, args);
            searchSummaryLabel.setValue(sum_label);
            searchTermBox.setDisabled(false);
            startSearchBtn.setDisabled(false);
        }
    }

    private void executeSearchByAlias(String search_term)
    {
        if (! search_term.equals("")) {
            boolean exact_match = ! search_term.endsWith("*");
            if (exact_match) {
                DocmaNode[] res = docmaSess.getNodesByLinkAlias(search_term);
                if (res != null) {
                    resultListModel.addAll(Arrays.asList(res));
                }
            } else {
                String search_prefix = search_term.substring(0, search_term.length() - 1);  // strip "*"
                List all = docmaSess.listAliases();
                for (int i=0; i < all.size(); i++) {
                    String alias = (String) all.get(i);
                    if (alias.startsWith(search_prefix)) {
                        DocmaNode nd = docmaSess.getNodeByAlias(alias);
                        if (nd != null) resultListModel.add(nd);
                    }
                }
            }
        }
    }

    private void executeSearchReferencingAlias(String search_term)
    {
        if (! search_term.equals("")) {
            DocmaNode root_nd = docmaSess.getRoot();
            searchReferencingThis_recursiv(root_nd, search_term);
        }
    }

    private void searchReferencingThis_recursiv(DocmaNode node, String thisAlias)
    {
        if (node == null) return;

        if (node.isHTMLContent()) {
            if (ContentUtil.isReferencingThis(node.getContentString(), thisAlias)) {
                resultListModel.add(node);
            }
        } else
        if (node.isReference()) {
            String ref_alias = node.getReferenceTarget();
            if (thisAlias.equals(ref_alias)) resultListModel.add(node);
        } else
        if (node.isChildable()) {
            int cnt = node.getChildCount();
            for (int i=0; i < cnt; i++) {
                searchReferencingThis_recursiv(node.getChild(i), thisAlias);
            }
        }
    }

    private void initFields()
    {
        searchTermBox       = (Textbox) getFellow("FindNodesSearchTermTextbox");
        searchTermLabel     = (Label) getFellow("FindNodesSearchTermLabel");
        searchSummaryLabel  = (Label) getFellow("FindNodesSearchSummaryLabel");
        startSearchBtn      = (Button) getFellow("FindNodesStartButton");
        resultListbox       = (Listbox) getFellow("FindNodesSearchResultList");
    }

    private void clearResultList()
    {
        if (! resultListModel.isEmpty()) resultListModel.clear();
        searchSummaryLabel.setValue("");
        old_search_term = "";
    }

}
