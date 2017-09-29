/*
 * ConsistencyCheckComposer.java
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.docma.app.Activity;
import org.docma.app.ConsistencyCheckRunnable;
import org.docma.app.DocmaNode;
import org.docma.app.DocmaSession;
import org.docma.app.RuleConfig;
import org.docma.app.RulesManager;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.SelectEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.*;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.DefaultTreeModel;
import org.zkoss.zul.DefaultTreeNode;
import org.zkoss.zul.Label;
import org.zkoss.zul.Tree;
import org.zkoss.zul.TreeNode;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Window;

/**
 *
 * @author MP
 */
public class ConsistencyCheckComposer extends SelectorComposer<Component>
{
    @Wire("#ConsistencyCheckDialog") Window checkDialog;
    @Wire("#ConsistencyCheckHeadLabel") Label headLabel;
    @Wire("#ConsistencyCheckLangCodeLabel") Label langCodeLabel;
    @Wire("#ConsistencyCheckSelectTree") Tree selectTree;
    @Wire("#ConsistencyCheckCountOnLabel") Label countOnLabel;
    @Wire("#ConsistencyCheckCountOffLabel") Label countOffLabel;
    @Wire("#ConsistencyCheckCountCorrectLabel") Label countCorrectLabel;
    @Wire("#ConsistencyCheckCorrectArea") Component correctArea;
    @Wire("#ConsistencyCheckCorrectCheckbox") Checkbox correctBox;
    @Wire("#ConsistencyCheckStartBtn") Button startBtn;
    
    private DocmaWebSession webSess;
    private MainWindow mainWin;
    private final List<String> selectedNodeIds = new ArrayList<String>();
    private final List<String> rulesOn = new ArrayList<String>();
    private final List<String> rulesOff = new ArrayList<String>();
    private final List<String> checksOn = new ArrayList<String>();
    private final List<String> checksOff = new ArrayList<String>();
    private int cntAutoCorrect = 0;
    private boolean readOnlyStore;
    
    public void showDialog()
    {
        selectedNodeIds.clear();
        webSess = GUIUtil.getDocmaWebSession(checkDialog);
        mainWin = webSess.getMainWindow();
        if (mainWin.getSelectedNodeCount() <= 0) {
            return;
        }
        
        // Check if a previously started activity is still running. 
        // Only one activity can run at the same time.
        ActivityWinComposer actComp = mainWin.getActivityWinComposer();
        if (actComp.isWindowOpened()) {
            if (actComp.isActivityRunning()) {
                MessageUtil.showError(mainWin, "activity.window.user_activity_exists");
                return;
            } else {
                actComp.closeWindow();
            }
        }
        DocmaSession docmaSess = webSess.getDocmaSession();
        if (! docmaSess.clearFinishedUserActivities()) {  // Clear finished activities.
            // Should never occur because of isActivityRunning() above.
            MessageUtil.showError(mainWin, "activity.window.user_activity_exists");
            return;
        }
        List<DocmaNode> selNodes = mainWin.getSelectedDocmaNodes(true, true);
        if (selNodes != null) {   // if no selection error
            for (DocmaNode nd : selNodes) {
                selectedNodeIds.add(nd.getId());
            }
            updateGUI(selNodes);
            checkDialog.doHighlighted();
        }
    }
    
    @Listen("onClick = #ConsistencyCheckStartBtn")
    public void onStartClick()
    {
        checkDialog.setVisible(false);   // close dialog

        // Clear finished activities (just to be sure).
        DocmaSession docmaSess = webSess.getDocmaSession();
        if (! docmaSess.clearFinishedUserActivities()) {
            // Should never occur because finished activities have been cleared in showDialog().
            MessageUtil.showError(mainWin, "activity.window.user_activity_exists");
            return;
        }
        
        try {
            // Start activity thread
            Activity act = docmaSess.createDocStoreActivity(docmaSess.getStoreId(), docmaSess.getVersionId());
            act.setTitle("consistency.check.activity_title");
            String[] nIds = selectedNodeIds.toArray(new String[selectedNodeIds.size()]);
            Map props = new HashMap();
            for (String rId : rulesOn) {
                props.put(rId, "on");
            }
            for (String rId : rulesOff) {
                props.put(rId, "off");
            }
            for (String chkId : checksOff) {
                props.put(chkId, "off");
            }
            Runnable thread_obj = new ConsistencyCheckRunnable(act, docmaSess, nIds, correctBox.isChecked(), props);
            act.start(thread_obj);

            // Open activity window
            ActivityWinComposer actComp = mainWin.getActivityWinComposer();
            actComp.openWindow(act);
        } catch (Exception ex) {
            MessageUtil.showException(mainWin, ex);
        }
    }
    
    @Listen("onClick = #ConsistencyCheckCancelBtn")
    public void onCancelClick()
    {
        checkDialog.setVisible(false);   // close dialog
    }

    @Listen("onClick = #ConsistencyCheckHelpBtn")
    public void onHelpClick() throws Exception
    {
        MainWindow.openHelp("help/consistency_check.html");
    }
    
    
    @Listen("onSelect = #ConsistencyCheckSelectTree")
    public void treeSelectionChanged(Event evt)
    {
        if (! (evt instanceof SelectEvent)) {
            return;
        }
        SelectEvent selEvt = (SelectEvent) evt;
        Component reference = selEvt.getReference();
        if (! (reference instanceof Treeitem)) {
            return;
        }
        Treeitem item = (Treeitem) reference;
        DefaultTreeNode node = (DefaultTreeNode) item.getValue();
        Object data = node.getData();
        DefaultTreeModel model = (DefaultTreeModel) selectTree.getModel();
        if (model.isSelected(node)) {
            if (data instanceof RuleData) {
                // Rule has been selected -> select all checks of this rule
                for (Object obj : node.getChildren()) {
                    model.addToSelection(obj);
                }
            } else {
                // Check has been selected -> select rule if not already checked
                updateRuleSelection(model, (DefaultTreeNode) node.getParent());
            }
        } else {
            if (data instanceof RuleData) {
                // Rule has been deselected -> deselect all checks of this rule
                for (Object obj : node.getChildren()) {
                    model.removeFromSelection(obj);
                }
            } else {
                // Check has been deselected -> deselect rule if all checks are deselected
                updateRuleSelection(model, (DefaultTreeNode) node.getParent());
            }
        }
        
        updateCounters();
        updateCountLabels();
        correctBox.setDisabled(cntAutoCorrect == 0);
        startBtn.setDisabled(checksOn.isEmpty());
    }
    
    private void updateRuleSelection(DefaultTreeModel model, DefaultTreeNode ruleNode)
    {
        boolean isCheckSelected = false;
        for (Object checkNode : ruleNode.getChildren()) {
            if (model.isSelected(checkNode)) {
                isCheckSelected = true;
                break;
            }
        }
        // If at least one check is selected, then the rule has to be selected too.
        // If no check is selected then the rule has to be deselected.
        if (isCheckSelected) {
            model.addToSelection(ruleNode);
        } else {
            model.removeFromSelection(ruleNode);
        }
    }
    
    private void updateGUI(List<DocmaNode> selNodes)
    {
        int selCnt = (selNodes != null) ? selNodes.size() : 0;

        String headTxt;
        if (selCnt == 1) { 
            DocmaNode nd = selNodes.get(0);
            String ndTitle = nd.getTitle();
            if (ndTitle.equals("")) {
                ndTitle = nd.getId();
            }
            headTxt = mainWin.i18n("consistency.check.check_single_node", ndTitle);
        } else {
            headTxt = mainWin.i18n("consistency.check.check_multiple_nodes", selCnt);
        }
        headLabel.setValue(headTxt);
        
        DocmaWebApplication webApp = webSess.getDocmaWebApplication();
        DocmaSession docmaSess = webSess.getDocmaSession();
        DefaultTreeModel model = createTreeModel(webApp.getRulesManager(), docmaSess.getStoreId());
        selectTree.setModel(model);

        String langCode = docmaSess.getLanguageCode();
        langCodeLabel.setValue(langCode == null ? "" : langCode.toUpperCase());
        
        readOnlyStore = !GUIUtil.isUpdateVersionAllowed(docmaSess, false);
        
        correctBox.setChecked(false);
        correctBox.setDisabled(readOnlyStore || (cntAutoCorrect == 0));
        correctArea.setVisible(! readOnlyStore);
        
        updateCountLabels();
        startBtn.setDisabled(checksOn.isEmpty());
    }
    
    private void updateCounters()
    {
        rulesOn.clear();
        rulesOff.clear();
        checksOn.clear();
        checksOff.clear();
        cntAutoCorrect = 0;
        
        DefaultTreeModel mod = (DefaultTreeModel) selectTree.getModel();
        DefaultTreeNode root = (DefaultTreeNode) mod.getRoot();
        for (Object obj1 : root.getChildren()) {
            DefaultTreeNode ruleNode = (DefaultTreeNode) obj1;
            RuleData rd = (RuleData) ruleNode.getData();
            boolean isRuleOn = false;
            for (Object obj2 : ruleNode.getChildren()) {
                DefaultTreeNode checkNode = (DefaultTreeNode) obj2;
                CheckData cd = (CheckData) checkNode.getData();
                if (mod.isSelected(checkNode)) {
                    isRuleOn = true;
                    checksOn.add(cd.getQualifiedId());
                    if (cd.isAutoCorrect()) {
                        cntAutoCorrect++;
                    }
                } else {
                    checksOff.add(cd.getQualifiedId());
                }
            }
            if (isRuleOn) {
                rulesOn.add(rd.getId());
            } else {
                rulesOff.add(rd.getId());
            }
        }
    }
    
    private void updateCountLabels()
    {
        countOnLabel.setValue("" + checksOn.size());
        countOffLabel.setValue("" + checksOff.size());
        countCorrectLabel.setValue("" + cntAutoCorrect);
    }
    
    private DefaultTreeModel createTreeModel(RulesManager rm, String storeId)
    {
        final String LABEL_AUTO_CORRECT = " (" + mainWin.i18n("rule.config.dialog.auto_correct") + ")";
        
        rulesOn.clear();
        rulesOff.clear();
        checksOn.clear();
        checksOff.clear();
        cntAutoCorrect = 0;
        RuleConfig[] rules = rm.getActiveRules(storeId);
        List<TreeNode> level1 = new ArrayList<TreeNode>();
        Set<TreeNode> selectedNodes = new HashSet<TreeNode>();
        for (RuleConfig rc : rules) {
            boolean select = rc.isDefaultOn();
            List<TreeNode> level2 = new ArrayList<TreeNode>();
            String[] chkIds = rc.getCheckIds();
            for (String chk : chkIds) {
                if (rc.isExecuteOnCheck(chk)) {
                    String chkTitle = chk;
                    boolean isCorrect = rc.supportsAutoCorrection(chk) && rc.isCorrectOnCheck(chk);
                    if (isCorrect) {
                        chkTitle += LABEL_AUTO_CORRECT;
                    }
                    CheckData cd = new CheckData(rc.getQualifiedCheckId(chk), chkTitle, isCorrect);
                    TreeNode checkNode = new DefaultTreeNode(cd);
                    if (select) {
                        selectedNodes.add(checkNode);
                        checksOn.add(cd.getQualifiedId());
                        if (isCorrect) {
                            cntAutoCorrect++;
                        }
                    } else {
                        checksOff.add(cd.getQualifiedId());
                    }
                    level2.add(checkNode);
                }
            }
            if (level2.size() > 0) {
                String title = rc.getTitle().trim();
                if (title.equals("")) {
                    title = rc.getId();
                }
                RuleData rd = new RuleData(rc.getId(), title);
                TreeNode ruleNode = new DefaultTreeNode(rd, level2);
                if (select) {
                    rulesOn.add(rc.getId());
                    selectedNodes.add(ruleNode);
                } else {
                    rulesOff.add(rc.getId());
                }
                level1.add(ruleNode);
            }
        }
        
        DefaultTreeNode root = new DefaultTreeNode("Rules", level1);
        DefaultTreeModel model = new DefaultTreeModel(root);
        model.setMultiple(true);
        model.setOpenObjects(selectedNodes);
        model.setSelection(selectedNodes);
        return model;
    }
    
    private static class RuleData 
    {
        private final String id;
        private final String title;
        
        RuleData(String id, String title) 
        {
            this.id = id;
            this.title = title;
        }
        
        String getId()
        {
            return id;
        }
        
        String getTitle()
        {
            return title;
        }
        
        public String toString()
        {
            return title;
        }
    }

    private static class CheckData
    {
        private final String id;
        private final String title;
        private final boolean isCorrect;
        
        CheckData(String id, String title, boolean isCorrect) 
        {
            this.id = id;
            this.title = title;
            this.isCorrect = isCorrect;
        }

        String getQualifiedId()
        {
            return id;
        }
        
        String getTitle()
        {
            return title;
        }
        
        boolean isAutoCorrect()
        {
            return isCorrect;
        }
        
        public String toString()
        {
            return title;
        }
    }
}
