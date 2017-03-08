/*
 * SearchReplaceDialog.java
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

import java.util.*;
import java.net.URLEncoder;
import org.docma.app.*;
import org.docma.util.Log;
import org.zkoss.zul.*;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.event.*;


/**
 *
 * @author MP
 */
public class SearchReplaceDialog extends Window
{
    private static final int DIALOG_STATE_BEFORE_SEARCH = 1;
    private static final int DIALOG_STATE_AFTER_SEARCH = 2;

    private Textbox searchTermBox;
    private Textbox replaceTermBox;
    private Checkbox matchCaseBox;
    private Checkbox incStructBox;
    private Checkbox incInlineBox;
    private Button findNextButton;
    private Button findPrevButton;
    private Button replaceButton;
    private Button replaceAllButton;

    private DocmaWebSession docmaWebSess;
    private DocmaSession docmaSess;
    private String searchTerm;
    private String replaceTerm;
    private String jsReplaceTerm;
    private DocmaNode searchNode;
    private String translationMode;
    private boolean allowReplace;

    private int dialog_state;

    private DocmaSearchResult searchResult;
    private ArrayList replaced;
    private int current_match_idx = -1;
    private int wait_cnt = 0;
    private int replace_all_idx = 0;
    private int replace_all_cnt = 0;
    
    private Set<String> revisionCreated = new HashSet<String>(); // set of node-ids for which a
                                                                 // revision was already created

    public void doSearchReplace(DocmaSession docmaSess)
    throws Exception
    {
        this.docmaSess = docmaSess;
        this.docmaWebSess = GUIUtil.getDocmaWebSession(this);

        setTitle(GUIUtil.i18(this).getLabel("label.search.dialog.title"));
        initFields();

        findNextButton.setDisabled(false);
        searchNode = null;
        searchTerm = "";
        prepareNewSearch();

        // MainWindow mainwin = docmaWebSess.getMainWindow();
        setLeft("10px");
        setTop("10px");

        // doModal();
        // doOverlapped();
        setVisible(true);
        setFocus(true);
        searchTermBox.setFocus(true);
    }
    
    public void closeDialog()
    {
        setVisible(false);
    }
    
    public boolean isDialogOpened()
    {
        return isVisible();
    }

    public void onFindNextClick() throws Exception
    {
        if (selectedNodeChanged()) {
            prepareNewSearch();
        }
        if (dialog_state == DIALOG_STATE_BEFORE_SEARCH) {
            // start new search
            String newTerm = searchTermBox.getValue();
            if (newTerm.trim().equals("")) {
                Messagebox.show("Please enter a search term!", "Error",
                                Messagebox.OK, Messagebox.ERROR);
                return;
            }
            searchTerm = newTerm; // DocmaAppUtil.encodeEntitiesNumeric(newTerm);
            boolean ignoreCase = !matchCaseBox.isChecked();
            boolean incStruct = incStructBox.isChecked();
            boolean incInline = incInlineBox.isChecked();

            String deskId = getDesktop().getId();
            MainWindow mainwin = docmaWebSess.getMainWindow();
            searchNode = mainwin.getSelectedDocmaNode();
            translationMode = docmaSess.getTranslationMode();
            if ((searchNode == null) || !(searchNode.isHTMLContent() || searchNode.isSection())) {
                Messagebox.show("No content node selected!");
                return;
            }
            searchResult = null;
            replaced = null;
            docmaWebSess.setSearchResult(null);
            String pubId = mainwin.getPreviewPubConfigId();
            String pubParam = (pubId != null) ? ("&pub=" + pubId) : "";
            String outId = mainwin.getPreviewOutputConfigId();
            String outParam = (outId != null) ? ("&out=" + outId) : "";
            String searchParam = "&search=" + URLEncoder.encode(searchTerm, "UTF-8");
            String optParams = "&ignorecase=" + ignoreCase + 
                               "&incstruct=" + incStruct + 
                               "&incinline=" + incInline;
            String timestamp = "&timestamp=" + System.currentTimeMillis();
            String view_url = getDesktop().getExecution().encodeURL(
                           "viewContent.jsp?nodeid=" + searchNode.getId() + "&desk=" + deskId +
                           pubParam + outParam + searchParam + optParams + timestamp);
            Iframe ifrm = (Iframe) mainwin.getFellow("viewcontentfrm");
            ifrm.setSrc(view_url);

            findNextButton.setLabel(GUIUtil.i18(this).getLabel("label.search.findnext.btn"));
            findPrevButton.setDisabled(true);
            if (allowReplace) {
                replaceButton.setDisabled(false);
                replaceAllButton.setDisabled(false);
            } else {
                replaceButton.setDisabled(true);
                replaceAllButton.setDisabled(true);
            }

            current_match_idx = 0;

            dialog_state = DIALOG_STATE_AFTER_SEARCH;

            // wait until search result page has been loaded
            wait_cnt = 0;
            Clients.showBusy("Execute search...");
            Events.echoEvent("onWaitForResult", this, null);
        } else {
            // highlight next match
            int max_idx = searchResult.size() - 1;
            if (current_match_idx < max_idx) {
                ++current_match_idx;
                if (current_match_idx == max_idx) {
                    findNextButton.setDisabled(true);
                }
                highlightMatch(current_match_idx);
                findPrevButton.setDisabled(false);
            }
            updateTitle();
            if (allowReplace) updateReplaceBtn();
        }
    }

    public void onWaitForResult() throws Exception
    {
            searchResult = docmaWebSess.getSearchResult();
            if ((searchResult != null) && (searchResult.isFinished())) {
                Clients.clearBusy();
                if (searchResult.size() == 0) {
                    Messagebox.show("Nothing found!", "Search finished",
                                    Messagebox.OK, Messagebox.INFORMATION);
                    prepareNewSearch();
                } else {
                    if (searchResult.size() == 1) {
                        findNextButton.setDisabled(true);
                    }
                    // Highlight the first match: This is done in onload script of search result page.
                    // Calling highlightMatch here gives sometimes error that JavaScript
                    // function setMatch() is not defined (if page is not completely loaded yet).
                    // highlightMatch(current_match_idx);  // current_match_idx should be 0
                }
                updateTitle();
            } else {
                Thread.sleep(500);
                int hit_cnt = (searchResult == null) ? 0 : searchResult.size();
                wait_cnt++;
                if ((wait_cnt % 6) == 0) {  // Update displayed hit counter every 3 seconds
                    Clients.showBusy("Execute search...Hits: " + hit_cnt);
                }
                Events.echoEvent("onWaitForResult", this, null);
            }
    }

    public void onFindPreviousClick() throws Exception
    {
        if (selectedNodeChanged()) {
            Messagebox.show("Selected node changed. Please restart search!");
            prepareNewSearch();
            return;
        }
        if (current_match_idx > 0) {
            highlightMatch(--current_match_idx);
            if (current_match_idx == 0) {
                findPrevButton.setDisabled(true);
            }
            findNextButton.setDisabled(false);
        }
        updateTitle();
        if (allowReplace) updateReplaceBtn();
    }

    public void onReplaceClick() throws Exception
    {
        if (selectedNodeChanged()) {
            Messagebox.show("Selected node changed. Please restart search!");
            prepareNewSearch();
            return;
        }
        initReplace();
        SearchMatch sm = searchResult.getSearchMatch(current_match_idx);
        doReplace(sm.getNodeId(), current_match_idx, current_match_idx);
        updateReplaceBtn();
    }

    public void onReplaceAllClick() throws Exception
    {
        if (selectedNodeChanged()) {
            Messagebox.show("Selected node changed. Please restart search!");
            prepareNewSearch();
            return;
        }
        initReplace();
        replace_all_idx = 0;
        replace_all_cnt = 0;
        Clients.showBusy("Replace all...");
        Events.echoEvent("onReplaceAll", this, null);
    }

    public void onReplaceAll() throws Exception
    {
        if (! isDialogOpened()) {
            return;  // Stop replacing the matches if dialog has been closed by user
        }
        if (replace_all_idx < searchResult.size()) {
            String node_id = searchResult.getSearchMatch(replace_all_idx).getNodeId();
            int next_idx = replace_all_idx + 1;
            while ((next_idx < searchResult.size()) &&
                   node_id.equals(searchResult.getSearchMatch(next_idx).getNodeId())) {
                next_idx++;
            }
            replace_all_cnt += doReplace(node_id, replace_all_idx, next_idx - 1);
            replace_all_idx = next_idx;
        }

        if (replace_all_idx < searchResult.size()) {
            int progress = Math.round(replace_all_idx / (float) searchResult.size());
            Clients.showBusy("Replace all..." + progress + "%");
            Events.echoEvent("onReplaceAll", this, null);
        } else {
            Clients.clearBusy();
            updateReplaceBtn();
            Messagebox.show("Replaced " + replace_all_cnt + " occurences of '" + searchTerm + "'",
                            "Replace finished", Messagebox.OK, Messagebox.INFORMATION);
        }
    }

    public void onCloseClick()
    {
        closeDialog();
    }

    public void onSearchChange()
    {
        if (dialog_state == DIALOG_STATE_AFTER_SEARCH) {
            prepareNewSearch();
            searchTermBox.setFocus(true);
        }
    }
    
    public void onOptionChanged()
    {
        prepareNewSearch();
    }

    public void nodeChanged()
    {
        if (isVisible() && (docmaSess != null)) {  // visible and initialized
            initFields();
            prepareNewSearch();
        }
    }

    public void onEncodeSearchTerm()
    {
        String oldterm = searchTermBox.getValue();
        String newterm = GUIUtil.getDocmaWebSession(this).encodeEntitiesNumeric(oldterm);
        if (! oldterm.equals(newterm)) {
            searchTermBox.setValue(newterm);
            prepareNewSearch();
        }
    }

    public void onDecodeSearchTerm()
    {
        String oldterm = searchTermBox.getValue();
        String newterm = GUIUtil.getDocmaWebSession(this).decodeEntities(oldterm);
        if (! oldterm.equals(newterm)) {
            searchTermBox.setValue(newterm);
            prepareNewSearch();
        }
    }

    public void onEncodeReplaceTerm()
    {
        String oldterm = replaceTermBox.getValue();
        String newterm = GUIUtil.getDocmaWebSession(this).encodeEntitiesNumeric(oldterm);
        if (! oldterm.equals(newterm)) {
            replaceTermBox.setValue(newterm);
        }
    }

    public void onDecodeReplaceTerm()
    {
        String oldterm = replaceTermBox.getValue();
        String newterm = decodeEntities(oldterm);
        if (! oldterm.equals(newterm)) {
            replaceTermBox.setValue(newterm);
        }
    }

    /* --------------  Private methods  ---------------------- */

    private String decodeEntities(String str)
    {
        return GUIUtil.getDocmaWebSession(this).decodeEntities(str);
    }

    private void updateTitle()
    {
        if (searchResult != null) {
            setTitle("Match " + (current_match_idx + 1) + " of " + searchResult.size());
        }
    }

    private void updateReplaceBtn()
    {
        initReplace();
        replaceButton.setDisabled(((Boolean) replaced.get(current_match_idx)).booleanValue());
    }

    private void initReplace()
    {
        replaceTerm = replaceTermBox.getValue();
        replaceTerm = replaceTerm.replace("<", "&#60;").replace(">", "&#62;").replace("\"", "&#34;");
        String decoded = decodeEntities(replaceTerm);
        jsReplaceTerm = jsEscape(decoded);  // jsEscape(replaceTerm);
        if (replaced == null) {
            replaced = new ArrayList(searchResult.size());
            int sz = searchResult.size();
            for (int i=0; i < sz; i++) {
                replaced.add(Boolean.FALSE);
            }
        }
    }

    private int doReplace(String node_id, int idx_start, int idx_end) throws Exception
    {
        DocmaNode nd = docmaSess.getNodeById(node_id);
        if (nd == null) {
            Messagebox.show("Unknown content node: " + node_id, "Error",
                            Messagebox.OK, Messagebox.ERROR);
            return 0;
        }
        boolean is_sect = nd.isSection() || nd.isSectionIncludeReference();
        boolean is_cont = nd.isContent();
        if (! (is_sect || is_cont)) {
            Log.warning("Cannot replace search term in node " + node_id +
                        ": Unexpected node type!");
            return 0;
        }

        String txt = (is_cont) ? nd.getContentString() : nd.getTitle();

        StringBuffer idx_buf = new StringBuffer(4 * (idx_end - idx_start));
        int repl_cnt = 0;
        for (int match_idx = idx_start; match_idx <= idx_end; match_idx++) {

            if (((Boolean) replaced.get(match_idx)).booleanValue()) {
                continue;   // this position was already replaced
            }

            SearchMatch sm = searchResult.getSearchMatch(match_idx);
            if (! node_id.equals(sm.getNodeId())) {
                Log.warning("Unexpected node id in SearchReplaceDialog.doReplace(...);");
                continue;
            }

            int offset = sm.getTextPos();
            if (txt.regionMatches(true, offset, searchTerm, 0, searchTerm.length())) {
                txt = txt.substring(0, offset) + replaceTerm +
                      txt.substring(offset + searchTerm.length());
            } else {
                Log.warning("Cannot replace search term in node " + nd.getId() +
                        ": Search term at position " + sm.getTextPos() + " not found!");
                continue;
            }
            adjustOffsets(match_idx, node_id);

            if (idx_buf.length() > 0) idx_buf.append(',');
            idx_buf.append(match_idx);

            replaced.set(match_idx, Boolean.TRUE);
            repl_cnt++;
        }  // for

        if (repl_cnt > 0) {  // if something was replaced
            if (is_cont) {
                if (! revisionCreated.contains(node_id)) {
                    try {
                        nd.makeRevision();
                        revisionCreated.add(node_id);
                    } catch (Exception ex) {
                        Log.warning("Could not create revision in search/replace: " + ex.getMessage());
                    }
                }
                nd.setContentString(txt);
            } else {
                nd.setTitle(txt);
            }

            // String repl_txt = jsEscape(replaceTerm); // URLEncoder.encode(replaceTerm, "UTF-8");
            Clients.evalJavaScript("window.frames['viewcontentfrm'].replaceAll('" +
                    jsReplaceTerm + "', [" + idx_buf + "]);");
        }

        return repl_cnt;
    }

    private void adjustOffsets(int match_idx, String node_id)
    {
        int diff = replaceTerm.length() - searchTerm.length();
        for (int i = match_idx + 1; i < searchResult.size(); i++) {
            SearchMatch sm = searchResult.getSearchMatch(i);
            if (node_id.equals(sm.getNodeId())) {
                sm.setTextPos(sm.getTextPos() + diff);
            } else {
                break;
            }
        }
    }

    private String jsEscape(String txt)
    {
        return txt.replace("\\", "\\\\").replace("'", "\\'");
    }

    private void initFields()
    {
        searchTermBox    = (Textbox) getFellow("SearchTermTextbox");
        replaceTermBox   = (Textbox) getFellow("ReplaceTermTextbox");
        matchCaseBox     = (Checkbox) getFellow("SearchMatchCaseCheckbox");
        incStructBox     = (Checkbox) getFellow("SearchIncludeStructCheckbox");
        incInlineBox     = (Checkbox) getFellow("SearchIncludeInlineCheckbox");
        findNextButton   = (Button) getFellow("SearchFindNextButton");
        findPrevButton   = (Button) getFellow("SearchFindPrevButton");
        replaceButton    = (Button) getFellow("SearchReplaceButton");
        replaceAllButton = (Button) getFellow("SearchReplaceAllButton");
    }

    private void prepareNewSearch()
    {
        // setTitle(GUIUtil.i18(this).getLabel("label.search.dialog.title"));
        findNextButton.setLabel(GUIUtil.i18(this).getLabel("label.search.find.btn"));
        findNextButton.setDisabled(false);
        findPrevButton.setDisabled(true);
        replaceButton.setDisabled(true);
        replaceAllButton.setDisabled(true);

        current_match_idx = -1;

        MainWindow mainwin = docmaWebSess.getMainWindow();
        searchNode = mainwin.getSelectedDocmaNode();
        translationMode = docmaSess.getTranslationMode();
        String edit_right = (translationMode == null) ? AccessRights.RIGHT_EDIT_CONTENT :
                                                        AccessRights.RIGHT_TRANSLATE_CONTENT;
        allowReplace = docmaSess.hasRight(edit_right);
        revisionCreated.clear();

        dialog_state = DIALOG_STATE_BEFORE_SEARCH;
    }

    private boolean selectedNodeChanged()
    {
        MainWindow mainwin = docmaWebSess.getMainWindow();
        DocmaNode selNode = mainwin.getSelectedDocmaNode();
        if (selNode != searchNode) return true;

        // if it's the same node, then check if the translation mode changed
        String trans = docmaSess.getTranslationMode();
        if (trans == null) {
            return (translationMode != null);
        } else {
            return !trans.equals(translationMode);
        }
    }

    private void highlightMatch(int match_idx)
    {
        Clients.evalJavaScript("window.frames['viewcontentfrm'].setMatch(" + match_idx + ");");
    }

}
