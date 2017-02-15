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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import org.zkoss.zul.Treefooter;
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
    @Wire("#ConsistencyCheckSelectTree") Tree selectTree;
    @Wire("#ConsistencyCheckCountOnLabel") Label countOnLabel;
    @Wire("#ConsistencyCheckCountOffLabel") Label countOffLabel;
    @Wire("#ConsistencyCheckCountCorrectLabel") Label countCorrectLabel;
    @Wire("#ConsistencyCheckCorrectCheckbox") Checkbox correctBox;
    @Wire("#ConsistencyCheckStartBtn") Button startBtn;
    
    private DocmaWebSession webSess;
    private MainWindow mainWin;
    private final List<String> checksOn = new ArrayList<String>();
    private final List<String> checksOff = new ArrayList<String>();
    private int cntAutoCorrect = 0;
    
    public void showDialog()
    {
        webSess = GUIUtil.getDocmaWebSession(checkDialog);
        mainWin = webSess.getMainWindow();
        if (mainWin.getSelectedNodeCount() > 0) {
            updateGUI();
            checkDialog.doHighlighted();
        }
    }
    
    @Listen("onClick = #ConsistencyCheckStartBtn")
    public void onStartClick()
    {
        checkDialog.setVisible(false);   // close dialog
    }
    
    @Listen("onClick = #ConsistencyCheckCancelBtn")
    public void onCancelClick()
    {
        checkDialog.setVisible(false);   // close dialog
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
        updateFooter();
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
    
    private void updateGUI()
    {
        List<DocmaNode> selNodes = mainWin.getSelectedDocmaNodes(true, true);
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
        
        correctBox.setChecked(false);
        correctBox.setDisabled(cntAutoCorrect == 0);
        updateFooter();
        startBtn.setDisabled(checksOn.isEmpty());
    }
    
    private void updateCounters()
    {
        checksOn.clear();
        checksOff.clear();
        cntAutoCorrect = 0;
        
        DefaultTreeModel mod = (DefaultTreeModel) selectTree.getModel();
        DefaultTreeNode root = (DefaultTreeNode) mod.getRoot();
        for (Object obj1 : root.getChildren()) {
            DefaultTreeNode ruleNode = (DefaultTreeNode) obj1;
            for (Object obj2 : ruleNode.getChildren()) {
                DefaultTreeNode checkNode = (DefaultTreeNode) obj2;
                CheckData cd = (CheckData) checkNode.getData();
                if (mod.isSelected(checkNode)) {
                    checksOn.add(cd.getQualifiedId());
                    if (cd.isAutoCorrect()) {
                        cntAutoCorrect++;
                    }
                } else {
                    checksOff.add(cd.getQualifiedId());
                }
            }
        }
    }
    
    private void updateFooter()
    {
        countOnLabel.setValue("" + checksOn.size());
        countOffLabel.setValue("" + checksOff.size());
        countCorrectLabel.setValue("" + cntAutoCorrect);
    }
    
    private DefaultTreeModel createTreeModel(RulesManager rm, String storeId)
    {
        final String LABEL_AUTO_CORRECT = " (" + mainWin.i18n("rule.config.dialog.auto_correct") + ")";
        
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
                    selectedNodes.add(ruleNode);
                }
                level1.add(ruleNode);
            }
        }
        
        DefaultTreeNode root = new DefaultTreeNode("Rules", level1);
        DefaultTreeModel model = new DefaultTreeModel(root);
        model.setMultiple(true);
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
