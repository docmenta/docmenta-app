/*
 * MenuUtil.java
 * 
 *  Copyright (C) 2016  Manfred Paula, http://www.docmenta.org
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

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.docma.app.AccessRights;
import org.docma.app.DocmaConstants;
import org.docma.app.DocmaNode;
import org.docma.app.DocmaSession;
import org.docma.coreapi.*;
import org.docma.plugin.implementation.PluginMenuEntry;
import org.docma.plugin.implementation.WebUserSessionImpl;
import org.docma.plugin.implementation.MenuOption;

import org.zkoss.zul.*;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.EventListener;


/**
 *
 * @author MP
 */
public class MenuUtil 
{
    
    public static void createDocTreeContextMenu(Menupopup menu, DocmaWebSession webSess)
    {
        createContentContextMenu(menu, webSess, false);
    }
    
    public static Menupopup createFileViewContextMenu(DocmaWebSession webSess)
    {
        MenupopupFileView menu = new MenupopupFileView(webSess);
        menu.setId("treemenu");  // Same id as the tree menu in main.zul,
                                 // because it represents the same menu and
                                 // plug-ins use this id to insert new items.

        // menu.addEventListener("onOpen", menu);
        // menu.addEventListener("onUpdateContextMenu", menu);
        // menu.setWidgetListener("onUpdateContextMenu", "return true;");
        createContentContextMenu(menu, webSess, true);
        return menu;
    }
    
    public static Timer createFileViewContextTimer(Menupopup menu)
    {
        final String contextTimerId = "docma_context_timer";
        Timer contextTimer = new Timer();
        contextTimer.setId(contextTimerId);
        contextTimer.setRepeats(false);
        contextTimer.setDelay(0);
        contextTimer.setRunning(false);
        contextTimer.addForward("onTimer", menu, "onShowContextMenu");
        return contextTimer;
    }

    
    private static void createContentContextMenu(Menupopup menu, DocmaWebSession webSess, boolean clientMode)
    {
        Map<String, Menupopup> menuMap = new HashMap<String, Menupopup>();
        menuMap.put(menu.getId(), menu);
        
        DocI18n i18n = webSess.getMainWindow().getI18n();
        
        addItem(menu, clientMode, "menuitemEditContent", i18n.getLabel("label.menuitem.editcontent"), "img/edit.gif", "onEditContent");
        addItem(menu, clientMode, "menuitemEditNodeProps", i18n.getLabel("label.menuitem.editproperties"), "img/edit_props.gif", "onEditNodeProps");
        addItem(menu, clientMode, "menuitemViewContent", i18n.getLabel("label.menuitem.viewcontent"), "img/view.gif", "onViewContent");
        addSeparator(menu);
        addItem(menu, clientMode, "menuitemAddSubNode", i18n.getLabel("label.menuitem.addsubnode"), "img/add_subnode.gif", "onAddSubNode");
        addItem(menu, clientMode, "menuitemInsertNode", i18n.getLabel("label.menuitem.insertnode"), "img/insert_nodehere.gif", "onInsertNode");
        addSeparator(menu);
        addItem(menu, clientMode, "menuitemPreviewPDF", i18n.getLabel("label.menuitem.previewpdf"), "img/pdf_menuicon.gif", "onPreviewPDF");
        addSeparator(menu);
        addItem(menu, clientMode, "menuitemUploadFile", i18n.getLabel("label.menuitem.uploadfile"), "img/uploadfile.gif", "onUploadFile");
        addItem(menu, clientMode, "menuitemDownloadFile", i18n.getLabel("label.menuitem.downloadfile"), "img/downloadfile.gif", "onDownloadFile");
        addSeparator(menu);
        addItem(menu, clientMode, "menuitemCutNode", i18n.getLabel("label.menuitem.cut"), "img/cut_icon.gif", "onCutNode");
        addItem(menu, clientMode, "menuitemCopyNode", i18n.getLabel("label.menuitem.copy"), "img/copy_icon.gif", "onCopyNode");
        Menupopup submenu1 = addSubMenu(menu, "treemenuPaste", i18n.getLabel("label.submenu.paste"), "img/paste_icon.gif");
        menuMap.put(submenu1.getId(), submenu1);
        addItem(submenu1, clientMode, "menuitemPasteSub", i18n.getLabel("label.menuitem.pastesub"), "img/paste_subnode.gif", clientMode ? "onPasteFilesSub" : "onPasteSub");
        addItem(submenu1, clientMode, "menuitemPasteHere", i18n.getLabel("label.menuitem.pastehere"), "img/paste_nodehere.gif", clientMode ? "onPasteFilesHere": "onPasteHere");
        addItem(menu, clientMode, "menuitemDeleteNode", i18n.getLabel("label.menuitem.deletenode"), "img/delete_20.gif", clientMode ? "onDeleteFileNodes" : "onDeleteNode");
        addSeparator(menu);
        addItem(menu, clientMode, "menuitemCompareVersions", i18n.getLabel("label.menuitem.revhistory"), "img/compare_icon.gif", "onCompareVersions");
        addItem(menu, clientMode, "menuitemSearchReplace", i18n.getLabel("label.menuitem.searchreplace"), "img/search_text.gif", "onSearchAndReplace");
        addItem(menu, clientMode, "menuitemFindReferencingThis", i18n.getLabel("label.menuitem.findrefthis"), null, "onFindReferencingThis");
        // addItem(menu, clientMode, "menuitemFindByAlias", i18n.getLabel("label.menuitem.findbyalias"), null, "onFindByAlias");
        addSeparator(menu);
        Menupopup submenu2 = addSubMenu(menu, "treemenuExtra", i18n.getLabel("label.submenu.extra"), null);
        menuMap.put(submenu2.getId(), submenu2);
        addItem(submenu2, clientMode, "menuitemConsistencyCheck", i18n.getLabel("label.menuitem.consistencycheck"), null, "onConsistencyCheck");
        addItem(submenu2, clientMode, "menuitemSortByFilename", i18n.getLabel("label.menuitem.sortbyfilename"), null, "onSortByFilename");
        addItem(submenu2, clientMode, "menuitemExportNodes", i18n.getLabel("label.menuitem.exportnodes"), "img/export_icon.gif", "onExportNodes");
        addItem(submenu2, clientMode, "menuitemImportSubNodes", i18n.getLabel("label.menuitem.importsubnodes"), "img/import_icon.gif", "onImportSubNodes");
        
        addPluginEntries(menuMap, webSess, clientMode);
    }

/*
  <menuitem id="menuitemEditContent" label="Edit Content" image="img/edit.gif" forward="mainWin.onEditContent"/>
  <menuitem id="menuitemEditNodeProps" label="Edit Properties" image="img/edit_props.gif" forward="mainWin.onEditNodeProps"/>
  <menuseparator/>
  <menuitem id="menuitemAddSubNode" label="Add Sub-Node ..." image="img/add_subnode.gif" forward="mainWin.onAddSubNode" />
  <menuitem id="menuitemInsertNode" label="Insert here ..." image="img/insert_nodehere.gif" forward="mainWin.onInsertNode" />
  <menuseparator/>
  <menuitem id="menuitemPreviewPDF" label="Preview PDF" image="img/pdf_menuicon.gif" forward="mainWin.onPreviewPDF"/>
  <menuseparator/>
  <menuitem id="menuitemUploadFile" label="Upload File" image="img/uploadfile.gif" forward="mainWin.onUploadFile"/>
  <menuitem id="menuitemDownloadFile" label="Download File" image="img/downloadfile.gif" forward="mainWin.onDownloadFile"/>
  <menuseparator/>
  <menuitem id="menuitemCutNode" label="Cut" image="img/cut_icon.gif" forward="mainWin.onCutNode"/>
  <menuitem id="menuitemCopyNode" label="Copy" image="img/copy_icon.gif" forward="mainWin.onCopyNode"/>
  <menu id="menuPaste" label="Paste" image="img/paste_icon.gif">
    <menupopup id="menupopupPaste">
      <menuitem id="menuitemPasteSub" label="As Sub-Nodes" image="img/paste_subnode.gif" forward="mainWin.onPasteSub"/>
      <menuitem id="menuitemPasteHere" label="Insert here" image="img/paste_nodehere.gif" forward="mainWin.onPasteHere"/>
    </menupopup>
  </menu>
  <menuitem id="menuitemDeleteNode" label="Delete" image="img/delete_20.gif" forward="mainWin.onDeleteNode"/>
  <menuseparator/>
  <menuitem id="menuitemCompareVersions" label="Revision History" image="img/compare_icon.gif" forward="mainWin.onCompareVersions"/>
  <menuitem id="menuitemSearchReplace" label="Search and Replace" image="img/search_text.gif" forward="mainWin.onSearchAndReplace"/>
  <menuitem id="menuitemFindReferencingThis" label="Find Referencing This" forward="mainWin.onFindReferencingThis" />
  <!-- <menuitem id="menuitemFindByAlias" label="by Alias" forward="mainWin.onFindByAlias" /> -->
  <menuseparator/>
  <menu id="menuExtra" label="Extra">
    <menupopup id="menupopupExtra">
      <menuitem id="menuitemSortByFilename" label="Sort by Filename" forward="mainWin.onSortByFilename" />
      <menuitem id="menuitemExportNodes" label="Export Nodes" image="img/export_icon.gif" forward="mainWin.onExportNodes" />
      <menuitem id="menuitemImportSubNodes" label="Import Sub-Nodes" image="img/import_icon.gif" forward="mainWin.onImportSubNodes" />
    </menupopup>
  </menu>
 */

    public static void updateContentMainMenu(Menupopup mainmenu, MainWindow mainWin) 
    {
        updatePluginMenus(mainmenu, mainWin);
    }
    
    public static void updateContentContextMenu(Menupopup treemenu, MainWindow mainWin) 
    {
        Menuitem item_editContent = (Menuitem) treemenu.getFellow("menuitemEditContent");
        Menuitem item_editNodeProps = (Menuitem) treemenu.getFellow("menuitemEditNodeProps");
        Menuitem item_viewContent = (Menuitem) treemenu.getFellow("menuitemViewContent");
        Menuitem item_addSubNode = (Menuitem) treemenu.getFellow("menuitemAddSubNode");
        Menuitem item_insertNode = (Menuitem) treemenu.getFellow("menuitemInsertNode");
        Menuitem item_uploadFile = (Menuitem) treemenu.getFellow("menuitemUploadFile");
        Menuitem item_downloadFile = (Menuitem) treemenu.getFellow("menuitemDownloadFile");
        Menuitem item_previewPDF = (Menuitem) treemenu.getFellow("menuitemPreviewPDF");
        Menuitem item_cutNode = (Menuitem) treemenu.getFellow("menuitemCutNode");
        Menuitem item_copyNode = (Menuitem) treemenu.getFellow("menuitemCopyNode");
        Menuitem item_pasteHere = (Menuitem) treemenu.getFellow("menuitemPasteHere");
        Menuitem item_pasteSub = (Menuitem) treemenu.getFellow("menuitemPasteSub");
        Menuitem item_deleteNode = (Menuitem) treemenu.getFellow("menuitemDeleteNode");
        Menuitem item_searchReplace = (Menuitem) treemenu.getFellow("menuitemSearchReplace");
        Menuitem item_sortFilenames = (Menuitem) treemenu.getFellow("menuitemSortByFilename");
        Menuitem item_exportNodes = (Menuitem) treemenu.getFellow("menuitemExportNodes");
        Menuitem item_importSubNodes = (Menuitem) treemenu.getFellow("menuitemImportSubNodes");

        DocmaSession docmaSess = mainWin.getDocmaSession();
        String storeId = docmaSess.getStoreId();
        DocVersionId verId = docmaSess.getVersionId();
        String ver_state = docmaSess.getVersionState(storeId, verId);

        Tree docTree = mainWin.getDocTree();
        int sel_cnt = docTree.getSelectedCount();
        if (sel_cnt < 1) {
            // if no node is selected, then wait for 0.5 seconds and try again.
            try { Thread.sleep(500); } catch (Exception ex) {}
            sel_cnt = docTree.getSelectedCount();
        }
        Treeitem item = (sel_cnt >= 1) ? docTree.getSelectedItem() : null;
        if (item == null) {  // (sel_cnt < 1)
            Messagebox.show("Please select a node!");
            return;
        }
        // Object sel_obj = null;
        // if (sel_cnt == 1) {
        //     Treeitem item = docTree.getSelectedItem();
        //     sel_obj = item.getValue();
        //     if (! (sel_obj instanceof DocmaNode)) disable_menu = true;
        // }
        // if (disable_menu) {
        //     // nothing selected, therefore disable all menu items
        //     List item_list = treemenu.getChildren();
        //     for (int i=0; i < item_list.size(); i++) {
        //         Object obj = item_list.get(i);
        //         if (obj instanceof Menuitem) {
        //             ((Menuitem) obj).setDisabled(true);
        //         }
        //     }
        //     return;
        // }
        // if (sel_cnt == 1) {
        DocmaNode node = (DocmaNode) item.getValue();
        DocmaNode parent = node.getParent();
        // int pos = parent.getChildPos(node);

        // item_editContent.setAction(client_action);
        // item_editContent.addForward(null, "mainWin", "onEditContent");
        boolean multiple = (sel_cnt > 1);
        boolean isRoot = (parent == docmaSess.getRoot()); // node.isDocumentRoot() || node.isSystemRoot();
        boolean isTransMode = node.isTranslationMode();
        boolean hasContentRight = docmaSess.hasRight(AccessRights.RIGHT_EDIT_CONTENT);
        boolean hasTransRight = docmaSess.hasRight(AccessRights.RIGHT_TRANSLATE_CONTENT);
        boolean noEditRight = ! ((hasContentRight && !isTransMode) || (hasTransRight && isTransMode));
        boolean isReleased = (ver_state != null) && ver_state.equals(DocmaConstants.VERSION_STATE_RELEASED);
        boolean noEdit = noEditRight || isReleased;
        boolean contNode = node.isContent();
        boolean htmlNode = node.isHTMLContent();
        boolean sectNode = node.isSection();
        boolean notContentOrSect = ! (contNode || sectNode || node.isReference());
        boolean sysFolder = node.isSystemFolder();
        boolean folderNode = node.isImageFolder() || sysFolder;
        boolean groupNode = sectNode || folderNode;
        boolean fileNode = contNode && node.isFileContent();
        boolean editorExists = false;
        boolean viewerExists = false;
        if (fileNode) {
            String ext = node.getFileExtension();
            if ((ext != null) && !ext.equals("")) {
                DocmaWebSession webSess = mainWin.getDocmaWebSession();
                editorExists = webSess.getEditorIds(ext).length > 0;
                viewerExists = webSess.getViewerIds(ext).length > 0;
            }
        }
        boolean textFile = fileNode && node.isTextFile();
        boolean editableContent = htmlNode || textFile || editorExists;
        boolean viewableContent = htmlNode || textFile || viewerExists;
        // boolean insertContentAllowed = parent.isInsertContentAllowed(pos);
        // boolean insertSectionAllowed = parent.isInsertSectionAllowed(pos);
        // boolean notSectOrSys = ! (sectNode || sysFolder);

        DocI18n i18n = mainWin.getI18n();
        if (htmlNode) {
            item_viewContent.setLabel(i18n.getLabel("label.menuitem.viewsource"));
        } else {
            item_viewContent.setLabel(i18n.getLabel("label.menuitem.viewcontent"));
        }
        updateContentContextMenuLabels(treemenu, isTransMode, i18n);
        
        CutCopyHandler cutCopyHandler = mainWin.getCutCopyHandler();

        item_editContent.setDisabled(multiple || noEdit || !editableContent);
        item_editNodeProps.setDisabled(noEdit || (notContentOrSect && !folderNode));
        item_viewContent.setDisabled(multiple || !viewableContent);
        item_addSubNode.setDisabled(multiple || noEdit || isTransMode || !groupNode);
        item_insertNode.setDisabled(multiple || noEdit || isTransMode || isRoot);
        item_uploadFile.setDisabled(multiple || noEdit || (! folderNode));
        item_downloadFile.setDisabled(!(folderNode || contNode));
        item_previewPDF.setDisabled(notContentOrSect);
        item_cutNode.setDisabled(noEdit || isTransMode);
        item_copyNode.setDisabled(noEdit || isTransMode);
        item_pasteHere.setDisabled(multiple || noEdit || isRoot || isTransMode || cutCopyHandler.isCutCopyListEmpty());
        item_pasteSub.setDisabled(multiple || noEdit || isTransMode || cutCopyHandler.isCutCopyListEmpty());
        item_deleteNode.setDisabled(noEdit);
        item_searchReplace.setDisabled(multiple || notContentOrSect);
        item_sortFilenames.setDisabled(multiple || noEdit || isTransMode || (! folderNode));
        item_exportNodes.setDisabled(false);
        item_importSubNodes.setDisabled(multiple || noEdit || isTransMode || !groupNode);
        // }
        
        updatePluginMenus(treemenu, mainWin);
    }


    /**
     * Sets menu item labels depending on whether user session is in translation 
     * mode or not. 
     * This method is also called from external when content language is 
     * switched.
     * 
     * @param treemenu The context menu.
     * @param isTransMode Current translation mode of the user session.
     * @param i18n The internationalization instance to retrieve language dependent labels.
     */
    public static void updateContentContextMenuLabels(Menupopup treemenu, boolean isTransMode, DocI18n i18n)
    {
        Menuitem item_editContent = (Menuitem) treemenu.getFellow("menuitemEditContent");
        Menuitem item_deleteNode = (Menuitem) treemenu.getFellow("menuitemDeleteNode");
        if (isTransMode) {
            item_editContent.setLabel(i18n.getLabel("label.menuitem.translatecontent"));
            item_deleteNode.setLabel(i18n.getLabel("label.menuitem.deletetranslation"));
        } else {
            item_editContent.setLabel(i18n.getLabel("label.menuitem.editcontent"));
            item_deleteNode.setLabel(i18n.getLabel("label.menuitem.deletenode"));
        }
    }

    // ***************** Private methods *********************

    private static void updatePluginMenus(Menupopup rootmenu, MainWindow mainWin)
    {
        // Note: This method updates all menu items that are in the same 
        //       ID space as rootmenu (for all plug-ins). 
        
        DocmaWebSession webSess = mainWin.getDocmaWebSession();
        WebUserSessionImpl userSess = (WebUserSessionImpl) webSess.getPluginInterface();
        List<PluginMenuEntry> entries = userSess.getMenuEntries();
        if (entries == null) {   // no plug-in menu entries exist
            return;
        }
        for (PluginMenuEntry e : entries) {
            String eid = e.getEntryId();
            Component comp = rootmenu.getFellowIfAny(eid);
            // Note that comp could be null in case rootmenu is not in the ID 
            // space of the main window. For example, if rootmenu is the context 
            // menu instantiated in the iframe of the preview area, then menu 
            // items in the content bar will not be updated (because an iframe   
            // has its own ID space).
            if (comp instanceof Menuitem) {
                applyItemOptions((Menuitem) comp, e.getOptions());
            } else if (comp instanceof Menupopup) {
                Component parent = ((Menupopup) comp).getParent();
                if (parent instanceof Menu) {
                    applySubMenuOptions((Menu) parent, e.getOptions());
                }
            } else if (comp instanceof Menuseparator) {
                applySeparatorOptions((Menuseparator) comp, e.getOptions());
            }
        }
    }
    
    private static void addPluginEntries(Map<String, Menupopup> menuMap, 
                                         DocmaWebSession webSess, 
                                         boolean clientMode)
    {
        WebUserSessionImpl userSess = (WebUserSessionImpl) webSess.getPluginInterface();
        List<PluginMenuEntry> entries = userSess.getMenuEntries();
        if ((entries == null) || entries.isEmpty()) {   // no plug-in menu entries exist
            return;
        }
        EventListener plugListener = new PluginMenuitemListener(webSess);
        for (PluginMenuEntry e : entries) {
            int etype = e.getType();
            String parentId = e.getParentMenuId();
            Menupopup parentMenu = menuMap.get(parentId);
            if (parentMenu == null) {  // the entry is registered for another menu
                continue;  // skip this menu entry
            }
            String neighbourId = e.getNeighbourId();
            boolean insertBefore = e.isInsertBefore();
            Component neighbour;
            if ((neighbourId == null) && insertBefore) {
                // Insert as first element. Therefore the neighbour has to be 
                // set to the first element.
                neighbour = parentMenu.getFirstChild();
            } else {
                // If neighbour is set to null, then the new entry will be added 
                // as last element.
                neighbour = (neighbourId != null) ? getMenuNeighbour(parentMenu, neighbourId) 
                                                  : null;
            }
            // String label = e.getStringOption(PluginMenuEntry.OPTION_LABEL);
            // String iconUrl = e.getStringOption(PluginMenuEntry.OPTION_IMAGE);
            switch (etype) {
                case PluginMenuEntry.ITEM:
                    addItem(parentMenu, clientMode, e.getEntryId(), null, null, e.getOptions(),
                            null, plugListener, neighbour, insertBefore);
                    break;
                case PluginMenuEntry.SEPARATOR:
                    addSeparator(parentMenu, e.getEntryId(), e.getOptions(), 
                                 neighbour, insertBefore);
                    break;
                case PluginMenuEntry.SUB_MENU:
                    Menupopup submenu =
                      addSubMenu(parentMenu, e.getEntryId(), null, null, e.getOptions(), 
                                 neighbour, insertBefore);
                    menuMap.put(submenu.getId(), submenu);
                    break;
            }
        }
    }
    
    private static Component getMenuNeighbour(Menupopup parentMenu, String neighbourId)
    {
        for (Component child : parentMenu.getChildren()) {
            if (neighbourId.equals(child.getId())) {
                return child;
            } else if (child instanceof Menu) {
                // If the child is a sub-menu, then also compare with the 
                // id of the nested popup-menu.
                Menupopup popup = ((Menu) child).getMenupopup();
                if ((popup != null) && neighbourId.equals(popup.getId())) {
                    return child;
                }
            }
        }
        return null;
    }
    
    private static Menupopup addSubMenu(Menupopup parentMenu, 
                                        String popupId,
                                        String label, 
                                        String imgUrl)
    {
        return addSubMenu(parentMenu, popupId, label, imgUrl, null, null, false);
    }

    private static Menupopup addSubMenu(Menupopup parentMenu, 
                                        String popupId, 
                                        String label, 
                                        String imgUrl, 
                                        Map<MenuOption, Object> options,
                                        Component neighbour, 
                                        boolean insertBefore)
    {
        Menu submenu = new Menu();
        // if (menuId != null) {
        //     submenu.setId(menuId);
        // }
        if (label != null) {
            submenu.setLabel(label);
        }
        if (imgUrl != null) {
            submenu.setImage(imgUrl);
        }
        if (neighbour == null) {
            parentMenu.appendChild(submenu);
        } else {
            parentMenu.insertBefore(submenu, insertBefore ? neighbour : neighbour.getNextSibling());
        }
        Menupopup subpopup = new Menupopup();
        if (popupId != null) {
            subpopup.setId(popupId);
        }
        submenu.appendChild(subpopup);
        applySubMenuOptions(submenu, options);
        return subpopup;
    }
    
    private static Menuitem addItem(Menupopup menu,
                                    boolean clientMode, 
                                    String itemId, 
                                    String label, 
                                    String imgUrl, 
                                    String mainEvent)
    {
        return addItem(menu, clientMode, itemId, label, imgUrl, null, mainEvent, 
                       null, null, false);
    }
    
    private static Menuitem addItem(Menupopup menu,
                                    boolean clientMode, 
                                    String itemId, 
                                    String label, 
                                    String imgUrl, 
                                    Map<MenuOption, Object> options,
                                    String mainEvent, 
                                    EventListener listener,
                                    Component neighbour, 
                                    boolean insertBefore)
    {
        Menuitem item = new Menuitem();
        item.setId(itemId);
        if (label != null) {
            item.setLabel(label);
        }
        if (imgUrl != null) {
            item.setImage(imgUrl);
        }
        applyItemOptions(item, options);
        // item.setSclass("docmenuitem");
        if (listener != null) {
            item.addEventListener("onClick", listener);
        } else {
            if (clientMode) {
                item.setWidgetListener("onClick", "sendMainEvent('" + mainEvent + "');");
            } else {
                item.addForward("onClick", "mainWin", mainEvent);
            }
        }
        if (neighbour == null) {
            menu.appendChild(item);
        } else {
            menu.insertBefore(item, insertBefore ? neighbour : neighbour.getNextSibling());
        }
        return item;
    }
    
    private static Menuseparator addSeparator(Menupopup menu)
    {
        return addSeparator(menu, null, null, null, false);
    }
    
    private static Menuseparator addSeparator(Menupopup menu, 
                                              String separatorId, 
                                              Map<MenuOption, Object> options,
                                              Component neighbour, 
                                              boolean insertBefore)
    {
        Menuseparator sep = new Menuseparator();
        if (separatorId != null) {
            sep.setId(separatorId);
        }
        applySeparatorOptions(sep, options);
        if (neighbour == null) {
            menu.appendChild(sep);
        } else {
            menu.insertBefore(sep, insertBefore ? neighbour : neighbour.getNextSibling());
        }
        return sep;
    }

    private static void applySubMenuOptions(Menu menu, Map<MenuOption, Object> options)
    {
        if (options == null) {
            return;
        }
        // Menupopup popup = menu.getMenupopup();
        
        for (MenuOption name : options.keySet()) {
            Object val = options.get(name);
            if (val == null) {
                continue;
            }
            if (MenuOption.LABEL.equals(name)) {
                menu.setLabel(val.toString());
            } else if (MenuOption.IMAGE.equals(name)) {
                menu.setImage(val.toString());
            } else if (MenuOption.VISIBLE.equals(name)) {
                menu.setVisible(boolValue(val));
            }
        }
    }
    
    private static void applyItemOptions(Menuitem item, Map<MenuOption, Object> options)
    {
        if (options == null) {
            return;
        }

        Object label = options.get(MenuOption.LABEL);
        Object image = options.get(MenuOption.IMAGE);
        Object disabled = options.get(MenuOption.DISABLED);
        Object visible = options.get(MenuOption.VISIBLE);
        Object checkmark = options.get(MenuOption.CHECKMARK);
        Object checked = options.get(MenuOption.CHECKED);
        
        // Note: 
        // setCheckmark() must be called before setChecked().
        // The autocheck property of ZK menu items is not supported, because
        // in the UI different instances of the same menu item could be created
        // (i.e. the checkbox state cannot be stored by a single item instance).
        // For example, in the preview area, the menu is (re)created every time
        // a folder is previewed.
        
        if (label != null) {
            item.setLabel(label.toString());
        }
        if (image != null) {
            item.setImage(image.toString());
        }
        if (disabled != null) {
            item.setDisabled(boolValue(disabled));
        }
        if (visible != null) {
            item.setVisible(boolValue(visible));
        }
        if (checkmark != null) {
            item.setCheckmark(boolValue(checkmark));
        }
        if (checked != null) {
            item.setChecked(boolValue(checked));
        }
    }
    
    private static void applySeparatorOptions(Menuseparator sep, Map<MenuOption, Object> options)
    {
        if (options == null) {
            return;
        }
        
        for (MenuOption name : options.keySet()) {
            Object val = options.get(name);
            if (val == null) {
                continue;
            }
            if (MenuOption.VISIBLE.equals(name)) {
                sep.setVisible(boolValue(val));
            }
        }
    }
    
    private static boolean boolValue(Object value)
    {
        return (value instanceof Boolean) ? (Boolean) value 
                                          : "true".equalsIgnoreCase(value.toString());
    }
}
