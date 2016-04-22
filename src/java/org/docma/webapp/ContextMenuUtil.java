/*
 * ContextMenuUtil.java
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

import org.docma.coreapi.*;
import org.zkoss.zul.*;

/**
 *
 * @author MP
 */
public class ContextMenuUtil 
{
    public static void createDocTreeContextMenu(Menupopup menu, DocmaWebSession webSess)
    {
        createContentContextMenu(menu, webSess, false);
    }
    
    public static Menupopup createFileViewContextMenu(DocmaWebSession webSess)
    {
        MenupopupFileView menu = new MenupopupFileView(webSess);
        menu.setId("docma_fileview_context_menu");
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
        DocmaI18 i18n = webSess.getMainWindow().getDocmaI18();
        addItem(menu, clientMode, "menuitemEditContent", i18n.getLabel("label.menuitem.editcontent"), "img/edit.gif", "onEditContent");
        addItem(menu, clientMode, "menuitemEditNodeProps", i18n.getLabel("label.menuitem.editproperties"), "img/edit_props.gif", "onEditNodeProps");
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
        Menupopup submenu1 = addSubMenu(menu, "menuPaste", i18n.getLabel("label.submenu.paste"), "img/paste_icon.gif", "menupopupPaste");
        addItem(submenu1, clientMode, "menuitemPasteSub", i18n.getLabel("label.menuitem.pastesub"), "img/paste_subnode.gif", clientMode ? "onPasteFilesSub" : "onPasteSub");
        addItem(submenu1, clientMode, "menuitemPasteHere", i18n.getLabel("label.menuitem.pastehere"), "img/paste_nodehere.gif", clientMode ? "onPasteFilesHere": "onPasteHere");
        addItem(menu, clientMode, "menuitemDeleteNode", i18n.getLabel("label.menuitem.deletenode"), "img/delete_20.gif", clientMode ? "onDeleteFileNodes" : "onDeleteNode");
        addSeparator(menu);
        addItem(menu, clientMode, "menuitemCompareVersions", i18n.getLabel("label.menuitem.revhistory"), "img/compare_icon.gif", "onCompareVersions");
        addItem(menu, clientMode, "menuitemSearchReplace", i18n.getLabel("label.menuitem.searchreplace"), "img/search_text.gif", "onSearchAndReplace");
        addItem(menu, clientMode, "menuitemFindReferencingThis", i18n.getLabel("label.menuitem.findrefthis"), null, "onFindReferencingThis");
        // addItem(menu, clientMode, "menuitemFindByAlias", i18n.getLabel("label.menuitem.findbyalias"), null, "onFindByAlias");
        addSeparator(menu);
        Menupopup submenu2 = addSubMenu(menu, "menuExtra", i18n.getLabel("label.submenu.extra"), null, "menupopupExtra");
        addItem(submenu2, clientMode, "menuitemSortByFilename", i18n.getLabel("label.menuitem.sortbyfilename"), null, "onSortByFilename");
        addItem(submenu2, clientMode, "menuitemExportNodes", i18n.getLabel("label.menuitem.exportnodes"), "img/export_icon.gif", "onExportNodes");
        addItem(submenu2, clientMode, "menuitemImportSubNodes", i18n.getLabel("label.menuitem.importsubnodes"), "img/import_icon.gif", "onImportSubNodes");
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
    
    private static Menupopup addSubMenu(Menupopup parentMenu, 
                                        String menuId, 
                                        String label, 
                                        String imgUrl, 
                                        String popupId)
    {
        Menu submenu = new Menu();
        submenu.setId(menuId);
        submenu.setLabel(label);
        if (imgUrl != null) {
            submenu.setImage(imgUrl);
        }
        parentMenu.appendChild(submenu);
        Menupopup subpopup = new Menupopup();
        if (popupId != null) {
            subpopup.setId(popupId);
        }
        submenu.appendChild(subpopup);
        return subpopup;
    }
    
    private static void addItem(Menupopup menu,
                                boolean clientMode, 
                                String itemId, 
                                String label, 
                                String imgUrl, 
                                String mainEvent)
    {
        Menuitem item = new Menuitem();
        item.setId(itemId);
        item.setLabel(label);
        if (imgUrl != null) {
            item.setImage(imgUrl);
        }
        // item.setSclass("docmenuitem");
        if (clientMode) {
            item.setWidgetListener("onClick", "sendMainEvent('" + mainEvent + "');");
        } else {
            item.addForward("onClick", "mainWin", mainEvent);
        }
        menu.appendChild(item);
    }
    
    private static void addSeparator(Menupopup menu)
    {
        menu.appendChild(new Menuseparator());
    }
    
}
