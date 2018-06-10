/*
 * MainWindow.java
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
import java.io.*;

import org.docma.coreapi.*;
import org.docma.app.*;
import org.docma.app.ui.*;
import org.docma.util.*;

import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.OpenEvent;
import org.zkoss.zk.ui.event.SizeEvent;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ClientInfoEvent;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Button;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Window;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treecols;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Menupopup;
import org.zkoss.zul.Menuitem;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.LayoutRegion;
import org.zkoss.zul.North;
import org.zkoss.zul.West;
import org.zkoss.zul.Filedownload;

/**
 *
 * @author MP
 */
public class MainWindow extends Window implements EventListener
{
    static final int MIN_LISTBOX_SIZE = 2;

    private boolean adminInitialized = false;
    private boolean contentInitialized = false;
    private boolean stylesInitialized = false;
    private boolean versioningInitialized = false;
    private boolean publishingInitialized = false;

    // Helper objects
    private final DocmaListitemRenderer listitem_renderer = new DocmaListitemRenderer(this);
    private final CutCopyHandler cutCopyHandler = new CutCopyHandler(this);
    private final ImportExportHandler importExportHandler = new ImportExportHandler(this);
    private final PreviewPDFHandler previewPDFHandler = new PreviewPDFHandler(this);
    private final CompareVersionsHandler compareVersionsHandler = new CompareVersionsHandler(this);
    private UploadHandler uploadHandler = null;
    private DownloadHandler downloadHandler = null;
    private GUI_List_UsersAndGroups guilist_usersgroups = null;

    private Listbox products_select_list;
    private Listbox versions_select_list;
    private Listbox language_select_list;
    private Tabbox  mainTabbox;

    // Default preview settings
    private DocmaOutputConfig defaultHTMLConfig = null;    // see getDefaultHTMLConfig()

    // Current preview settings
    private DocmaOutputConfig previewOutputConfig = null;  // see getPreviewOutputConfig()
    private DocmaOutputConfig previewHTMLConfig = null;    // see getPreviewHTMLConfig()
    private DocmaPublicationConfig previewPubConfig = null;  // see getPreviewPubConfig()
    private String previewPubContentRoot = null;  // see getPreviewPubContentRoot()

    // Content Tabpanel elements
    private Tree docTree;
    private DocmaWebTreeModel docTreeModel;

    // Admin Tabpanel elements:
    private UserLoader     user_loader;
    private GUI_List_Products         guilist_products;
    private GUI_List_CharEntities     guilist_char_entities;
    private GUI_List_AutoFormatConfig guilist_autoformatconfig;
    private GUI_List_Rules            guilist_rules;
    private GUI_List_Plugins          guilist_plugins;
    private GUI_List_SystemEditors    guilist_edit_extensions;
    private GUI_List_SystemViewers    guilist_view_extensions;
    private Label          webappdir_label;
    private Label          tempdir_label;
    private Textbox        docstoredir_textbox;
    private Textbox        pubonlinedir_textbox;
    private Radio          pubonline_relpath_radio;
    private Radio          pubonline_abspath_radio;
    private Textbox        pubonlineurl_textbox;
    private Textbox        txt_extensions_textbox;
    private Textbox        fop_config_textbox;
    private Intbox         max_revisions_intbox;
    private Intbox         max_preview_html_intbox;
    private Intbox         max_preview_print_intbox;
    private Listbox        app_themes_listbox;
    private Label          app_edition_label;

    // Styles Tabpanel elements:
    private GUI_List_Styles guilist_styles;

    // Versioning Tabpanel elements:
    private GUI_List_ProductVersions guilist_productversions;

    // Publishing Tabpanel elements:
    private Listbox        declared_applics_listbox;
    private ListModelList  declared_applics_listmodel;
    private Listbox        publicationconfigs_listbox;
    private ListModelList  publicationconfigs_listmodel;
    private Listbox        mediaconfigs_listbox;
    private ListModelList  mediaconfigs_listmodel;
    private Listbox        publicationexports_listbox;
    private ListModelList  publicationexports_listmodel;


    /* --------------  Private methods  --------------- */

    private Tree createDocTree(boolean isHorizontal)
    {
        DocmaSession docmaSess = getDocmaSession();
        DocI18n i18n = docmaSess.getI18n();

        Tree cont_tree = new DocmaWebTree(); // new Tree();
        cont_tree.setId("doctree");
        cont_tree.setVflex(true);
        cont_tree.setMultiple(true); 
        cont_tree.setZclass("z-dottree");  // "z-filetree"
        cont_tree.addForward("onSelect", this, "onDocTreeSelect");
        cont_tree.setStyle("border-width:0px;background-color:transparent;margin:0px;");
        Treecols tcols = new Treecols();
        tcols.setSizable(true);
        tcols.addForward("onColSize", this, "onDocTreeColSize");
        Treecol col1 = new Treecol(i18n.getLabel("label.nodecolumn.title"));
        col1.setId("doctree_col_title");
        col1.setWidth(getDocTreeColWidth(docmaSess, col1.getId(), isHorizontal, "20%"));
        Treecol col2 = new Treecol(i18n.getLabel("label.nodecolumn.lang"));
        col2.setId("doctree_col_lang");
        col2.setWidth(getDocTreeColWidth(docmaSess, col2.getId(), isHorizontal, "40px"));
        col2.setVisible(false);
        Treecol col3 = new Treecol(i18n.getLabel("label.nodecolumn.alias"));
        col3.setId("doctree_col_alias");
        col3.setWidth(getDocTreeColWidth(docmaSess, col3.getId(), isHorizontal, "15%"));
        Treecol col4 = new Treecol(i18n.getLabel("label.nodecolumn.applic"));
        col4.setId("doctree_col_applic");
        col4.setWidth(getDocTreeColWidth(docmaSess, col4.getId(), isHorizontal, "20%"));
        Treecol col5 = new Treecol(i18n.getLabel("label.nodecolumn.status"));
        col5.setId("doctree_col_wfstatus");
        col5.setWidth(getDocTreeColWidth(docmaSess, col5.getId(), isHorizontal, "10%"));
        Treecol col6 = new Treecol(i18n.getLabel("label.nodecolumn.progress"));
        col6.setId("doctree_col_progress");
        col6.setWidth(getDocTreeColWidth(docmaSess, col6.getId(), isHorizontal, "10%"));
        Treecol col7 = new Treecol(i18n.getLabel("label.nodecolumn.lastmod"));
        col7.setId("doctree_col_lastmod");
        col7.setWidth(getDocTreeColWidth(docmaSess, col7.getId(), isHorizontal, "20%"));
        tcols.appendChild(col1);
        tcols.appendChild(col2);
        tcols.appendChild(col3);
        tcols.appendChild(col4);
        tcols.appendChild(col5);
        tcols.appendChild(col6);
        tcols.appendChild(col7);
        cont_tree.appendChild(tcols);
        cont_tree.appendChild(new Treechildren());
        return cont_tree;
    }

    private void setDocTreeColWidths(DocmaSession docmaSess, boolean isHorizontal)
    {
        Treecols tcols = docTree.getTreecols();
        List col_list = tcols.getChildren();
        for (int i=0; i < col_list.size(); i++) {
            Object child = col_list.get(i);
            if (child instanceof Treecol) {
                Treecol tcol = (Treecol) child;
                String cwidth = getDocTreeColWidth(docmaSess, tcol.getId(), isHorizontal, null);
                if (cwidth != null) tcol.setWidth(cwidth);
            }
        }
    }

    private String getDocTreeColWidth(DocmaSession docmaSess,
                                      String col_id,
                                      boolean isHorizontalLayout,
                                      String default_width)
    {
        String prop_name;
        if (isHorizontalLayout) {
            prop_name = GUIConstants.PROP_USER_CONTENT_TREE_COL_WIDTH_HORIZONTAL;
        } else {
            prop_name = GUIConstants.PROP_USER_CONTENT_TREE_COL_WIDTH_VERTICAL;
        }
        prop_name += "." + col_id;
        String val = docmaSess.getUserProperty(prop_name);
        if ((val == null) || val.equals("")) return default_width;
        else return val;
    }

    private void rerenderDocTree()
    {
        List ts_list = new ArrayList();
        GUIUtil.getTreeState(docTree.getTreechildren(), ts_list);
        docTree.setModel(docTree.getModel());
        GUIUtil.applyTreeState(docTree, docTree.getTreechildren(), ts_list);
        docTree.invalidate();
    }

    private void initAdminTab()
    {
        if (adminInitialized) return;

        guilist_products = new GUI_List_Products(this);
        guilist_products.loadProducts();
        guilist_char_entities = new GUI_List_CharEntities(this, (Listbox) getFellow("EntitiesListbox"));
        // guilist_char_entities.loadCharEntities();  // list is loaded in onSelectCharEntitiesTab()
        guilist_rules = new GUI_List_Rules(this);
        guilist_plugins = new GUI_List_Plugins(this);
        guilist_edit_extensions = new GUI_List_SystemEditors(this);
        guilist_view_extensions = new GUI_List_SystemViewers(this);
        
        initAutoFormatConfigList();
        updateAppSettingsPanel();

        adminInitialized = true;
    }

    private void initAutoFormatConfigList()
    {
        if (guilist_autoformatconfig == null) {
            guilist_autoformatconfig = new GUI_List_AutoFormatConfig(this);
        }
    }

    private void updateAppSettingsPanel()
    {
        tempdir_label = (Label) getFellow("SettingsTempDirLabel");
        webappdir_label = (Label) getFellow("SettingsWebAppDirLabel");
        docstoredir_textbox = (Textbox) getFellow("SettingsDocStoreDirTextbox");
        pubonlinedir_textbox = (Textbox) getFellow("SettingsPubOnlineDirTextbox");
        pubonline_relpath_radio = (Radio) getFellow("SettingsPubOnlineRelativePathRadio");
        pubonline_abspath_radio = (Radio) getFellow("SettingsPubOnlineAbsolutePathRadio");
        pubonlineurl_textbox = (Textbox) getFellow("SettingsPubOnlineURLTextbox");
        txt_extensions_textbox = (Textbox) getFellow("SettingsTxtFileExtensionsTextbox");
        fop_config_textbox = (Textbox) getFellow("SettingsFOPConfigPathTextbox");
        max_revisions_intbox = (Intbox) getFellow("SettingsMaxRevisionsIntbox");
        max_preview_html_intbox = (Intbox) getFellow("SettingsPreviewMaxHTMLIntbox");
        max_preview_print_intbox = (Intbox) getFellow("SettingsPreviewMaxPrintIntbox");
        app_themes_listbox = (Listbox) getFellow("SettingsAppThemesListbox");
        app_edition_label = (Label) getFellow("SettingsAppEditionLabel");

        String tmpdir = System.getProperty("java.io.tmpdir");
        tempdir_label.setValue((tmpdir != null) ? tmpdir : "");
        DocmaWebApplication webApp = GUIUtil.getDocmaWebApplication(this);
        DocmaSession docmaSess = getDocmaSession();
        webappdir_label.setValue(webApp.getWebAppDirectory());
        docstoredir_textbox.setValue(docmaSess.getApplicationProperty(DocmaConstants.PROP_BASE_PATH));
        String rel_onlinepath = docmaSess.getApplicationProperty(DocmaConstants.PROP_PUBLICATION_ONLINE_RELATIVE_PATH);
        String onlinepath = docmaSess.getApplicationProperty(DocmaConstants.PROP_PUBLICATION_ONLINE_PATH);
        boolean is_rel = (rel_onlinepath != null) && (rel_onlinepath.trim().length() > 0);
        if (new File(rel_onlinepath).isAbsolute()) {
            is_rel = false;
        }
        pubonline_relpath_radio.setChecked(is_rel);
        pubonline_abspath_radio.setChecked(! is_rel);
        if (is_rel) {
            pubonlinedir_textbox.setValue(rel_onlinepath);
        } else {
            pubonlinedir_textbox.setValue(onlinepath);
        }
        String online_url = docmaSess.getApplicationProperty(DocmaConstants.PROP_PUBLICATION_ONLINE_URL);
        pubonlineurl_textbox.setValue((online_url == null) ? "" : online_url);

        // String txt_ext_str = docmaSess.getApplicationProperty(DocmaConstants.PROP_TEXT_FILE_EXTENSIONS);
        String[] txt_ext = docmaSess.getTextFileExtensions();
        txt_extensions_textbox.setValue(DocmaUtil.concatStrings(Arrays.asList(txt_ext), " ").trim());

        String fop_conf_path = docmaSess.getApplicationProperty(DocmaConstants.PROP_FOP_CUSTOM_CONFIG_FILE);
        fop_config_textbox.setValue((fop_conf_path == null) ? "" : fop_conf_path);
        
        String max_rev_str = docmaSess.getApplicationProperty(DocmaConstants.PROP_MAX_REVISIONS_PER_USER);
        Integer max_rev_int = null;
        try {
            if ((max_rev_str != null) && (max_rev_str.length() > 0)) max_rev_int = new Integer(max_rev_str);
        } catch (Exception ex) {}
        if (max_rev_int == null) max_rev_int = new Integer(DocmaConstants.DEFAULT_MAX_REVISIONS_PER_USER);
        max_revisions_intbox.setValue(max_rev_int);
        
        String max_preview_str = docmaSess.getApplicationProperty(DocmaConstants.PROP_PREVIEW_MAX_NODES);
        int max_preview = 0;
        try {
            if ((max_preview_str != null) && (max_preview_str.length() > 0)) {
                max_preview = Integer.parseInt(max_preview_str);
            }
        } catch (Exception ex) {}
        max_preview_html_intbox.setValue((max_preview < 0) ? 0 : max_preview);
        
        String max_print_str = docmaSess.getApplicationProperty(DocmaConstants.PROP_PREVIEW_MAX_NODES_PRINT);
        int max_print = 0;
        try {
            if ((max_print_str != null) && (max_print_str.length() > 0)) {
                max_print = Integer.parseInt(max_print_str);
            }
        } catch (Exception ex) {}
        max_preview_print_intbox.setValue((max_print < 0) ? 0 : max_print);
        
        String[] themeIds = webApp.getThemeIds();
        String selectedThemeId = webApp.getDefaultTheme();
        app_themes_listbox.getItems().clear();
        for (String theme_id : themeIds) {
            app_themes_listbox.appendItem(theme_id, theme_id);
        }
        GUIUtil.selectListItem(app_themes_listbox, selectedThemeId);

        String edition_name = getAppEditionName(webApp.getApplicationServices());
        app_edition_label.setValue(edition_name);
    }

    private void clearDocTree()
    {
        docTree = (Tree) getFellow("doctree");
        // docTree.setModel(null);
        // docTree.clear();
        Treechildren tchildren = docTree.getTreechildren();
        if (tchildren != null) {
            ListIterator l = tchildren.getChildren().listIterator();
            while (l.hasNext()) {
                l.next();
                l.remove();
            }
        }
    }

    private void initContentTab()
    {
        if (contentInitialized) return;

        DocmaSession docmaSess = getDocmaSession();
        docmaSess.removeDocListeners();
        docmaSess.removeLockListeners();
        // DocmaNode root = docmaSess.getDocumentRoot();
        // Log.info("Root title: " + root.getTitle());

        docTreeModel = null;

        clearDocTree();

        String storeId = docmaSess.getStoreId();
        if (storeId == null) return;  // no store opened

        if (docmaSess.getTranslationMode() != null) {
            String state = docmaSess.getVersionState(storeId, docmaSess.getVersionId());
            if (state.equalsIgnoreCase(DocmaConstants.VERSION_STATE_TRANSLATION_PENDING)) {
                try {
                    Messagebox.show("Translation of this version is pending!");
                } catch (Exception ex) {}
                return;
            }
        }

        // Load preview settings
        loadViewSourceState(docmaSess, storeId);
        String last_format = getLastSelectedFormat(docmaSess, storeId);
        if ((last_format == null) || last_format.equals("")) {
            last_format = "html";  // use HTML preview as default
        }
        setPreviewFormat(last_format);
        loadPreviewPubConfList(docmaSess);
        loadPreviewOutputConfList(docmaSess);

        // Load the document tree
        docTreeModel = new DocmaWebTreeModel(this, docmaSess);
        docmaSess.addDocListener(docTreeModel);
        docmaSess.addLockListener(docTreeModel);
        DocmaTreeitemRenderer tree_renderer = new DocmaTreeitemRenderer(this);
        tree_renderer.applyUserSettings(getUser(docmaSess.getUserId()));
        docTree.setItemRenderer(tree_renderer);
        docTree.setModel(docTreeModel);

        // open the document root folder
        int[] docroot_path = docTreeModel.getPath(docmaSess.getDocumentRoot());
        if (docroot_path != null) {
            // Treeitem ti = docTree.renderItemByPath(docroot_path);  // open path
            // if (ti != null) ti.setOpen(true);
            docTreeModel.addOpenPath(docroot_path);
        }
        docTree.invalidate();

        // cancel any running transactions
        // if (docsess.runningTransaction()) {
        //     try {
        //         docsess.rollbackTransaction();
        //     } catch (Exception ex) {
        //     }
        //     Messagebox.show("Action canceled!");
        // }

        contentInitialized = true;
        Log.info("Content Tab initialized.");
    }

    private void initStylesTab()
    {
        if (stylesInitialized) return;
        
        guilist_styles = new GUI_List_Styles(this);
        stylesInitialized = true;
    }

    private void initVersioningTab()
    {
        if (versioningInitialized) return;

        if (guilist_productversions == null) {
            guilist_productversions =
                new GUI_List_ProductVersions(this, (Listbox) getFellow("VersionsListbox"));
        }
        guilist_productversions.loadProductVersions();
        versioningInitialized = true;
    }

    private void initPublishingTab()
    {
        if (publishingInitialized) return;

        declared_applics_listbox = (Listbox) getFellow("DeclaredApplicsListbox");
        declared_applics_listmodel = new ListModelList();
        declared_applics_listbox.setModel(declared_applics_listmodel);
        loadDeclaredApplics();

        publicationconfigs_listbox = (Listbox) getFellow("PublicationConfigListbox");
        publicationconfigs_listmodel = new ListModelList();
        publicationconfigs_listbox.setModel(publicationconfigs_listmodel);
        publicationconfigs_listbox.setItemRenderer(listitem_renderer);
        loadPublicationConfigs();

        mediaconfigs_listbox = (Listbox) getFellow("MediaConfigListbox");
        mediaconfigs_listmodel = new ListModelList();
        mediaconfigs_listbox.setModel(mediaconfigs_listmodel);
        mediaconfigs_listbox.setItemRenderer(listitem_renderer);
        loadMediaConfigs();

        publicationexports_listbox = (Listbox) getFellow("PublicationExportListbox");
        publicationexports_listmodel = new ListModelList();
        publicationexports_listmodel.setMultiple(true);
        publicationexports_listbox.setModel(publicationexports_listmodel);
        publicationexports_listbox.setItemRenderer(listitem_renderer);
        loadPublicationExports();

        publishingInitialized = true;
    }

    private void initUserLoader()
    {
        if (user_loader == null) {
            DocmaSession docmaSess = getDocmaSession();
            user_loader = new UserLoader(docmaSess.getUserManager());
        }
    }

    private void loadDeclaredApplics()
    {
        DocmaSession docmaSess = getDocmaSession();
        declared_applics_listmodel.clear();
        String[] app_arr = docmaSess.getDeclaredApplics();
        declared_applics_listmodel.addAll(Arrays.asList(app_arr));
    }

    private void loadPublicationConfigs()
    {
        DocmaSession docmaSess = getDocmaSession();
        String[] pubids = docmaSess.getPublicationConfigIds();
        for (String pubid : pubids) {
            DocmaPublicationConfig pubconf = docmaSess.getPublicationConfig(pubid);
            publicationconfigs_listmodel.add(pubconf);
        }
        publicationconfigs_listbox.setRows(publicationconfigs_listmodel.getSize());
    }

    private void loadMediaConfigs()
    {
        DocmaSession docmaSess = getDocmaSession();
        String[] outids = docmaSess.getOutputConfigIds();
        for (String outid : outids) {
            DocmaOutputConfig outconf = docmaSess.getOutputConfig(outid);
            mediaconfigs_listmodel.add(outconf);
        }
        // mediaconfigs_listbox.setRows(mediaconfigs_listmodel.getSize());
    }

    private void loadPublicationExports()
    {
        DocmaSession docmaSess = getDocmaSession();
        publicationexports_listmodel.clear();

        boolean needs_reload = false;
        DocmaPublication[] pubs = docmaSess.listPublications();
        for (DocmaPublication pub : pubs) {
            publicationexports_listmodel.add(pub);
            if (! pub.isExportFinished()) needs_reload = true;
        }

        enablePublicationExportListRefresh(needs_reload);
    }

    private void enablePublicationExportListRefresh(boolean do_refresh)
    {
        Timer timer = (Timer) getFellow("PublicationExportRefreshTimer");
        timer.setRunning(do_refresh);
    }

    private void loadPreviewPubConfList(DocmaSession docmaSess)
    {
        Listbox listbox = (Listbox) getFellow("previewPubConfList");
        listbox.getItems().clear();
        listbox.appendItem("unfiltered", "");
        String lastconf = getLastSelectedPubConfigId(docmaSess, docmaSess.getStoreId());
        int selidx = 0;
        if (lastconf == null) lastconf = "";
        String[] conf_ids = docmaSess.getPublicationConfigIds();
        for (int i=0; i < conf_ids.length; i++) {
            String cid = conf_ids[i];
            listbox.appendItem(cid, cid);
            if (cid.equals(lastconf)) selidx = i + 1;
        }
        listbox.setSelectedIndex(selidx);
        listbox.invalidate();  // workaround for zk bug
        // previewPubConfig = null;  // clear cached configuration
        // previewPubContentRoot = null;  // clear cached value
        redrawPubContentRoot();
    }

    private void redrawPubContentRoot()
    {
        String old_pub_root = previewPubContentRoot;
        previewPubConfig = null;  // clear previous cached value
        previewPubContentRoot = null;  // clear previous cached value
        String new_pub_root = getPreviewPubContentRoot();
        if (docTreeModel != null) {
            boolean same_root = (old_pub_root != null) && old_pub_root.equals(new_pub_root);
            if ((old_pub_root != null) && !same_root) {
                docTreeModel.redrawDocmaNode(old_pub_root);
            }
            if ((new_pub_root != null) && !same_root) {
                docTreeModel.redrawDocmaNode(new_pub_root);
            }
        }
    }

    private void loadPreviewOutputConfList(DocmaSession docmaSess)
    {
        String storeId = docmaSess.getStoreId();
        String format = getPreviewFormat();
        Listbox listbox = (Listbox) getFellow("previewOutputConfList");
        listbox.getItems().clear();
        listbox.appendItem("default", "");
        String lastconf = getLastSelectedOutputConfigId(docmaSess, storeId, format);
        int selidx = 0;
        if (lastconf == null) lastconf = "";
        String[] conf_ids = docmaSess.getOutputConfigIds();
        int cnt = 0;
        for (String cid : conf_ids) {
            DocmaOutputConfig outconf = docmaSess.getOutputConfig(cid);
            if ((outconf != null) && format.equalsIgnoreCase(outconf.getFormat())) {
                listbox.appendItem(cid, cid);
                cnt++;
                if (cid.equals(lastconf)) selidx = cnt;
            }
        }
        listbox.setSelectedIndex(selidx);
        listbox.invalidate();  // workaround for zk bug
        previewOutputConfig = null;  // clear cached configuration
        previewHTMLConfig = null;  // clear cached configuration
    }

    private String getSelectedMainTabId()
    {
        mainTabbox = (Tabbox) getFellow("mainTabbox");
        Tab sel_tab = mainTabbox.getSelectedTab();
        return (sel_tab != null) ? sel_tab.getId() : null;
    }

    private void reinitTabs() throws Exception
    {
        contentInitialized = false;
        stylesInitialized = false;
        versioningInitialized = false;
        publishingInitialized = false;

        mainTabbox = (Tabbox) getFellow("mainTabbox");
        Tab sel_tab = mainTabbox.getSelectedTab();

        // reload data immediately for selected tab
        if (sel_tab != null) {
            String sel_id = sel_tab.getId();
            if (sel_id.equals("contentTab")) {
                onSelectContentTab();  // initContentTab();
            } else
            if (sel_id.equals("stylesTab")) {
                onSelectStylesTab();  // initStylesTab();
            } else
            if (sel_id.equals("versioningTab")) {
                onSelectVersioningTab();  // initVersioningTab();
            } else
            if (sel_id.equals("publishingTab")) {
                onSelectPublishingTab();  // initPublishingTab();
            } else
            if (sel_id.equals("adminTab")) {
                onSelectAdminTab(); // initAdminTab();
            } else {
                Log.warning("Unknown Tab: " + sel_id);
            }
        }
    }

    private void setTabVisibility(DocmaSession docmaSess)
    {
        Tab contentTab = (Tab) getFellow("contentTab");
        Tab stylesTab = (Tab) getFellow("stylesTab");
        Tab versioningTab = (Tab) getFellow("versioningTab");
        Tab publishingTab = (Tab) getFellow("publishingTab");
        Tab adminTab = (Tab) getFellow("adminTab");

        boolean isOpen = (docmaSess.getStoreId() != null);
        String loginName = getUser(docmaSess.getUserId()).getLoginName();
        boolean adminRight = loginName.equals(DocmaConstants.SYS_ADMIN_LOGIN_NAME) ||
                             docmaSess.hasFullAdminRight() ||
                             (isOpen && docmaSess.hasRight(AccessRights.RIGHT_ADMINISTRATION));

        boolean isContentVisible = true; // isOpen && docmaSess.hasRight(AccessRights.RIGHT_VIEW_CONTENT);
        boolean isStylesVisible = isOpen && docmaSess.hasRight(AccessRights.RIGHT_VIEW_CONTENT);
        boolean isVersioningVisible = isOpen && docmaSess.hasRight(AccessRights.RIGHT_MANAGE_VERSIONS);
        boolean isPublishingVisible = isOpen && docmaSess.hasRight(AccessRights.RIGHT_MANAGE_PUBLICATIONS);

        contentTab.setVisible(isContentVisible);
        stylesTab.setVisible(isStylesVisible);
        versioningTab.setVisible(isVersioningVisible);
        publishingTab.setVisible(isPublishingVisible);
        adminTab.setVisible(adminRight);

        Tab firstVisibleTab = null;
        if (isContentVisible) firstVisibleTab = contentTab;
        else if (isStylesVisible) firstVisibleTab = stylesTab;
        else if (isVersioningVisible) firstVisibleTab = versioningTab;
        else if (isPublishingVisible) firstVisibleTab = publishingTab;
        else if (adminRight) firstVisibleTab = adminTab;

        mainTabbox = (Tabbox) getFellow("mainTabbox");
        Tab sel_tab = mainTabbox.getSelectedTab();

        if ((sel_tab == null) || !sel_tab.isVisible()) {
            if (firstVisibleTab != null) mainTabbox.setSelectedTab(firstVisibleTab);
        }
    }

    private void disableButtons(DocmaSession docmaSess)
    {
        if (! docmaSess.hasRight(AccessRights.RIGHT_EDIT_STYLES)) {
            ((Button) getFellow("NewStyleBtn")).setDisabled(true);
            ((Button) getFellow("EditStyleBtn")).setDisabled(true);
            ((Button) getFellow("DeleteStyleBtn")).setDisabled(true);
            ((Button) getFellow("VariantStyleBtn")).setDisabled(true);
            ((Button) getFellow("ExportStyleBtn")).setDisabled(true);
            ((Button) getFellow("ImportStyleBtn")).setDisabled(true);

            ((Menuitem) getFellow("NewStyleMenuitem")).setDisabled(true);
            ((Menuitem) getFellow("EditStyleMenuitem")).setDisabled(true);
            ((Menuitem) getFellow("DeleteStyleMenuitem")).setDisabled(true);
            ((Menuitem) getFellow("VariantStyleMenuitem")).setDisabled(true);
            ((Menuitem) getFellow("ExportStyleMenuitem")).setDisabled(true);
            ((Menuitem) getFellow("ImportStyleMenuitem")).setDisabled(true);
        }
    }

    private String getVersionSelectionLabel(String storeId, DocVersionId verId,
                                            DocmaSession docmaSess, DocmaI18 docmaI18)
    {
        String ver_state = docmaSess.getVersionState(storeId, verId);
        String state_label;
        if (ver_state.equals(DocmaConstants.VERSION_STATE_DRAFT)) {
            state_label = docmaI18.getLabel("label.versionstatus.draft");
        } else
        if (ver_state.equals(DocmaConstants.VERSION_STATE_RELEASED)) {
            state_label = docmaI18.getLabel("label.versionstatus.released");
        } else
        if (ver_state.equals(DocmaConstants.VERSION_STATE_TRANSLATION_PENDING)) {
            state_label = docmaI18.getLabel("label.versionstatus.translationpending");
        } else {
            state_label = ver_state;
        }
        String vstr = verId.toString();
        if (vstr.equals(DocmaConstants.DEFAULT_LATEST_VERSION_ID)) {
            vstr = docmaI18.getLabel("label.versionid.latest");
        }
        return vstr + " - " + state_label;
    }

    private int getProductSelectIndex(String storeId)
    {
        for (int i=0; i < products_select_list.getItemCount(); i++) {
            if (products_select_list.getItemAtIndex(i).getValue().equals(storeId)) {
                return i;
            }
        }
        return 0;  // index of empty item
    }

    private void loadContentLanguagesList(DocmaSession docmaSess, String sel_lang)
    {
        language_select_list.getItems().clear();

        DocmaLanguage orig_lang = docmaSess.getOriginalLanguage();
        String orig_code = orig_lang.getCode();
        language_select_list.appendItem("Original (" + orig_code.toUpperCase() + ")", null);

        DocmaLanguage[] trans_langs = docmaSess.getTranslationLanguages();
        int sel_idx = 0;
        for (int i=0; i < trans_langs.length; i++) {
            DocmaLanguage lang = trans_langs[i];
            String lang_code = lang.getCode();
            String desc = lang.getDescription() + " (" + lang_code.toUpperCase() + ")";
            language_select_list.appendItem(desc, lang_code);
            if (lang_code.equalsIgnoreCase(sel_lang)) sel_idx = i+1;
        }
        language_select_list.setSelectedIndex(sel_idx);
        language_select_list.invalidate();  // workaround for zk bug
    }

    private void setDocTreeLayout(boolean horizontal, DocmaSession docmaSess)
    {
        Borderlayout border_layout = (Borderlayout) getFellow("contentBorderLayout");
        // Button switch_btn = (Button) getFellow("switchContentLayoutBtn");
        Menuitem m_item = (Menuitem) getFellow("menuitemSwitchLayout");
        String tree_size;
        if (horizontal) {
            tree_size = docmaSess.getUserProperty(GUIConstants.PROP_USER_CONTENT_TREE_SIZE_HORIZONTAL);
        } else {
            tree_size = docmaSess.getUserProperty(GUIConstants.PROP_USER_CONTENT_TREE_SIZE_VERTICAL);
        }
        if ((tree_size == null) || tree_size.trim().equals("")) {
            tree_size = GUIConstants.CONTENT_TREE_DEFAULT_SIZE;
        }
        LayoutRegion old_region = (LayoutRegion) docTree.getParent();
        if (old_region != null) {
            old_region.setParent(null);  // remove old region
        }
        LayoutRegion region;
        if (horizontal) {
            // switch_btn.setImage("img/vmode.gif");
            m_item.setLabel(i18("label.contentpreview.layout.vertical"));
            region = border_layout.getNorth();
            if (region == null) region = new North();
        } else {
            // switch_btn.setImage("img/hmode.gif");
            m_item.setLabel(i18("label.contentpreview.layout.horizontal"));
            region = border_layout.getWest();
            if (region == null) region = new West();
        }
        region.setSize(tree_size);
        region.setSplittable(true);
        region.setCollapsible(true);
        region.setFlex(true);
        region.setMargins("0,0,0,0");
        region.appendChild(docTree);
        if (region.getParent() == null) {
            border_layout.appendChild(region);
        }
        region.addForward("onSize", "mainWin", "onResizeContentTreeRegion");
    }

    private void setDocTreeToolbarEnabled(boolean is_enabled)
    {
        Toolbar tb = (Toolbar) getFellow("mainTabToolbar");
        tb.setVisible(is_enabled);
    }

    private void setStoreSelectionEnabled(boolean is_enabled)
    {
        products_select_list.setDisabled(!is_enabled);
    }

    private void setVersionSelectionEnabled(boolean is_enabled)
    {
        versions_select_list.setDisabled(!is_enabled);
    }

    private void setLanguageSelectionEnabled(boolean is_enabled)
    {
        language_select_list.setDisabled(!is_enabled);
    }

    private boolean isViewSource()
    {
        Toolbarbutton btn = (Toolbarbutton) getFellow("previewSrcBtn");
        return btn.isChecked();
    }
    
    private void loadViewSourceState(DocmaSession docmaSess, String storeId)
    {
        String val = docmaSess.getUserProperty(GUIConstants.PROP_USER_PREVIEW_SOURCE + "." + storeId);
        Toolbarbutton btn = (Toolbarbutton) getFellow("previewSrcBtn");
        btn.setChecked((val != null) && val.equalsIgnoreCase("true"));
    }

    private DocmaOutputConfig getDefaultHTMLConfig()
    {
        if (defaultHTMLConfig == null) {
            defaultHTMLConfig = DocmaAppUtil.createDefaultHTMLConfig();
        }
        return defaultHTMLConfig;
    }
    
    /**
     * Close all non-modal dialogs that are still opened.
     */    
    private void closeOpenedDialogs()
    {
        SearchReplaceDialog search_dialog = getSearchReplaceDialog();
        FindNodesComposer find_comp = getFindNodesComposer();
        ActivityWinComposer act_comp = getActivityWinComposer();
        
        if (search_dialog.isDialogOpened()) {
            search_dialog.closeDialog();
        }
        if (find_comp.isDialogOpened()) {
            find_comp.closeDialog();
        }
        if (act_comp.isWindowOpened()) {
            act_comp.closeWindow();
        }
    }

    /* --------------  Package local methods  --------------- */

    SearchReplaceDialog getSearchReplaceDialog()
    {
        return (SearchReplaceDialog) getPage().getFellow("SearchReplaceDialog");
    }
    
    FindNodesComposer getFindNodesComposer()
    {
        Window dialog = (Window) getPage().getFellow("FindNodesDialog");
        return (FindNodesComposer) dialog.getAttribute("$composer");
    }
    
    ConsistencyCheckComposer getConsistencyCheckComposer() 
    {
        Window dialog = (Window) getPage().getFellow("ConsistencyCheckDialog");
        return (ConsistencyCheckComposer) dialog.getAttribute("$composer");
    }

    ActivityWinComposer getActivityWinComposer() 
    {
        Window actWin = (Window) getPage().getFellow("ActivityWindow");
        return (ActivityWinComposer) actWin.getAttribute("$composer");
    }

    DocmaWebTree getDocTree()
    {
        return (DocmaWebTree) docTree;
    }

    DocmaWebTreeModel getDocTreeModel()
    {
        return docTreeModel;
    }
    
    CutCopyHandler getCutCopyHandler()
    {
        return cutCopyHandler;
    }

    DocmaSession getDocmaSession()
    {
        return GUIUtil.getDocmaWebSession(this).getDocmaSession();
    }
    
    DocmaWebSession getDocmaWebSession()
    {
        return GUIUtil.getDocmaWebSession(this);
    }

    /**
     * Deprecated. Should be replaced by method getI18n().
     * @return An instance of DocmaI18.
     */
    DocmaI18 getDocmaI18()
    {
        return GUIUtil.getDocmaWebApplication(this).i18();
    }

    /**
     * Returns an instance of DocI18n.
     * @return An instance of DocI18n.
     */
    DocI18n getI18n()
    {
        return GUIUtil.getDocmaWebApplication(this).getI18n();
    }

    /**
     * Deprecated. Should be replaced by method i18n().
     * @param key
     * @return The label for the provided key.
     */
    String i18(String key)
    {
        return GUIUtil.getDocmaWebApplication(this).i18().getLabel(key);
    }

    /**
     * Deprecated. Should be replaced by method i18n().
     * @param key
     * @param args
     * @return 
     */
    String i18(String key, Object[] args)
    {
        return GUIUtil.getDocmaWebApplication(this).i18().getLabel(key, args);
    }

    String i18n(String key, Object... args)
    {
        return i18(key, args);
    }

    UserLoader getUserLoader()
    {
        initUserLoader();
        return user_loader;
    }

    UserModel getUser(String user_id)
    {
        return getUserLoader().getUser(user_id);
    }

    String getGUILanguage()
    {
        return GUIUtil.getCurrentUILanguage(this);
    }

    AutoFormatConfigModel[] getAutoFormatConfigs()
    {
        initAutoFormatConfigList();
        return guilist_autoformatconfig.getAutoFormatClasses();
    }

    DocmaListitemRenderer getListitemRenderer()
    {
        return listitem_renderer;
    }

    void echoError(String msg)
    {
        Events.echoEvent("onShowError", this, msg);
        // Messagebox.show("Failed to open document store!");  // can only be called in Event thread
    }

    void openDocStore(DocmaSession docmaSess, String storeId, DocVersionId verId)
    {
        // Remove listeners to stop handling of events which may still be
        // in the queue.
        docmaSess.removeDocListeners();
        docmaSess.removeLockListeners();

        // Close previously opened store
        String old_store = docmaSess.getStoreId();
        if (old_store != null) {
            closeOpenedDialogs();
            try {
                docmaSess.closeDocStore();
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }
        try {
            versions_select_list.getItems().clear();
            previewPubConfig = null;  // clear cached value
            previewPubContentRoot = null;  // clear cached value
            previewOutputConfig = null;  // clear cached value
            previewHTMLConfig = null;  // clear cached value

            DocVersionId[] verIds = docmaSess.listVersions(storeId);
            if (verIds == null) verIds = new DocVersionId[0];
            Arrays.sort(verIds);
            if (verIds.length > 0) {
                // Fill the GUI listbox with the available versions
                DocmaI18 docI18 = getDocmaI18();
                for (DocVersionId vid : verIds) {
                    String lab = getVersionSelectionLabel(storeId, vid, docmaSess, docI18);
                    versions_select_list.appendItem(lab, vid.toString());
                }
                // if no versionId is given, then open the latest version
                List verIdsList = Arrays.asList(verIds);
                if ((verId == null) || !verIdsList.contains(verId)) {
                    verId = verIds[verIds.length - 1];  // latest version
                }
                docmaSess.openDocStore(storeId, verId);
                Log.info("Opened DocStore: " + storeId + " Version: " + verId);

                // Set latest opened store user properties
                String[] names = new String[] { "openedStore", "openedVersion" };
                String[] values = new String[] { storeId, verId.toString() };
                docmaSess.setUserProperties(names, values);

                loadContentLanguagesList(docmaSess, docmaSess.getTranslationMode());

                int p_idx = getProductSelectIndex(storeId);
                if (products_select_list.getSelectedIndex() != p_idx) {
                    products_select_list.setSelectedIndex(p_idx);
                    products_select_list.invalidate();  // workaround for zk bug
                }

                int v_idx = verIdsList.indexOf(verId);
                versions_select_list.setSelectedIndex(v_idx); // getItemAtIndex(v_idx).setSelected(true);
                versions_select_list.invalidate();  // workaround for zk bug
            } else {
                echoError("Could not open store '" + storeId + "': Store contains no version.");
                Log.error("Could not open DocStore. DocStore contains no version.");
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            echoError("Could not open store '" + storeId + "': " + ex.getMessage());
            versions_select_list.getItems().clear();
        }
        try {
            boolean is_opened = (docmaSess.getStoreId() != null);
            if (! is_opened) {  // could not open store
                products_select_list.setSelectedIndex(0);  // select first entry -> no store selected
                products_select_list.invalidate();  // workaround for zk bug
            }
            setTabVisibility(docmaSess);
            reinitTabs();
            disableButtons(docmaSess);
            clearPreview();
            if (is_opened) {
                // Show running / finished user activity
                Activity[] acts = docmaSess.getOpenedStoreUserActivities();
                if ((acts != null) && acts.length > 0) {
                    getActivityWinComposer().openWindow(acts[0]);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void closeDocStore(DocmaSession docmaSess) throws Exception
    {
        closeDocStore(docmaSess, false);
    }

    void closeDocStore(DocmaSession docmaSess, boolean reopen) throws Exception
    {
        // Set reopen to true, if the store shall only be closed temporarily
        // to do some operation and then will immediatelly be re-opened again.
        // In this case the tabs are not closed, to avoid unwanted change
        // of the selected tab. Problem is, that if an exception occurs and
        // the store is not reopened, then the tabs are opened although
        // no store is opened. This can cause errors. Therefore exceptions
        // have to be caught and closeDocStore has to be called again with
        // reopen set to false.
        versions_select_list.getItems().clear();
        language_select_list.getItems().clear();
        // Remove listeners to stop handling of events which may still be
        // in the queue.
        docmaSess.removeDocListeners();
        docmaSess.removeLockListeners();
        if (docmaSess.getStoreId() != null) {
            docmaSess.closeDocStore();
        }
        if (products_select_list.getSelectedIndex() > 0) {
            products_select_list.setSelectedIndex(0);  // no product
            products_select_list.invalidate();  // workaround for zk bug
        }
        if (! reopen) {
            closeOpenedDialogs();
            setTabVisibility(docmaSess);
            reinitTabs();  // onSelectAdminTab();
        }
        clearDocTree();
        clearPreview();
    }

    void updateVersionSelectionList(DocmaSession docmaSess)
    {
        versions_select_list.getItems().clear();
        String opened_sid = docmaSess.getStoreId();
        DocVersionId opened_vid = docmaSess.getVersionId();
        if (opened_sid == null) return;  // no store opened

        DocVersionId[] verIds = docmaSess.listVersions(opened_sid);
        if (verIds == null) verIds = new DocVersionId[0];
        Arrays.sort(verIds);
        List verIdsList = Arrays.asList(verIds);
        if (verIds.length > 0) {
            // Fill the GUI listbox with the available versions
            DocmaI18 docI18 = getDocmaI18();
            for (DocVersionId vid : verIds) {
                String lab = getVersionSelectionLabel(opened_sid, vid, docmaSess, docI18);
                versions_select_list.appendItem(lab, vid.toString());
            }
            int v_idx = verIdsList.indexOf(opened_vid);
            versions_select_list.setSelectedIndex(v_idx);
            versions_select_list.invalidate();  // workaround for zk bug
        }
    }

    void initMainWindow() throws Exception
    {
        this.listitem_renderer.applyUserSettings();
        this.guilist_usersgroups = new GUI_List_UsersAndGroups(this);

        DocmaSession docmaSess = getDocmaSession();

        String contentLayout = docmaSess.getUserProperty(GUIConstants.PROP_USER_CONTENT_WIN_LAYOUT);
        boolean isHorizontal = (contentLayout != null) &&
                               contentLayout.trim().equals(GUIConstants.CONTENT_WIN_LAYOUT_HORIZONTAL);
        // Create doctree
        docTree = createDocTree(isHorizontal);
        setDocTreeLayout(isHorizontal, docmaSess);

        // Fill the GUI listbox with the available DocStores
        products_select_list = (Listbox) getFellow("storesList");
        versions_select_list = (Listbox) getFellow("versionsList");
        language_select_list = (Listbox) getFellow("languageList");
        products_select_list.getItems().clear();
        versions_select_list.getItems().clear();
        language_select_list.getItems().clear();
        String[] storeIds = docmaSess.listDocStores();
        Arrays.sort(storeIds);
        List storeIdsList = new ArrayList(storeIds.length); // Arrays.asList(storeIds);
        products_select_list.appendItem("", "");  // if this item is selected, then no product is opened
        for (String sid : storeIds) {
            try {
                boolean enabled = !docmaSess.isStoreDisabled(sid);
                if (enabled && docmaSess.hasViewRight(sid)) {
                    storeIdsList.add(sid);
                    products_select_list.appendItem(sid, sid);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                Log.error("Could not add store '" + sid + "' to stores list. Exception: " + ex.getMessage());
            }
        }

        String current_storeId = docmaSess.getStoreId();
        DocVersionId current_verId = docmaSess.getVersionId();
        if (current_storeId != null) {
            Log.warning("Initializing MainWindow with an open DocmaSession instance!");
        }

        // if this is a new session, i.e. no DocStore is open, then open the
        // DocStore from the last session or the first one in the DocStore list
        if ((current_storeId == null) && (storeIdsList.size() > 0)) {
            current_storeId = docmaSess.getLastOpenedStore();
            current_verId = docmaSess.getLastOpenedVersion();
            if ((current_storeId == null) || !storeIdsList.contains(current_storeId)) {
                current_storeId = (String) storeIdsList.get(0);
            }
        }

        if (current_storeId != null) {
            // select the opened DocStore in the GUI listbox
            int s_idx = storeIdsList.indexOf(current_storeId);
            products_select_list.setSelectedIndex(s_idx + 1);

            openDocStore(docmaSess, current_storeId, current_verId);

            current_storeId = docmaSess.getStoreId();
            current_verId = docmaSess.getVersionId();

            if ((current_storeId != null) && (current_verId != null)) {
                // if openDocStore was successful
                initContentTab();  // load data in content tab immediately
            }
        } else {
            // no DocStore exists
            setTabVisibility(docmaSess);  // Admin tab will be only active tab
            reinitTabs();  // onSelectAdminTab();  // Init admin tab
        }
    }

    String getAppEditionName(ApplicationServices appservices)
    {
        String edition_name = DocmaConstants.DISPLAY_APP_NAME_BASEEDITION +
                              " V" + DocmaConstants.DISPLAY_APP_VERSION;
        return edition_name;
    }

    void addStoreToSelectList(String storeId) throws Exception
    {
        products_select_list.appendItem(storeId, storeId);
        if (products_select_list.getItemCount() == 2) {  // first product
            products_select_list.setSelectedIndex(1);
            products_select_list.invalidate();  // workaround for zk bug
            onSelectStore();
        }
    }
    
    void removeStoreFromSelectList(String storeId) throws Exception
    {
        int p_idx = getProductSelectIndex(storeId);
        if (p_idx > 0) {
            products_select_list.removeItemAt(p_idx);
        }
    }


    /* --------------  Public methods  --------------- */

    public boolean isShowIndexTermsChecked()
    {
        Menuitem mi = (Menuitem) getFellow("menuitemShowIndexTerms");
        return mi.isChecked();
    }

    public String getPreviewFormat()
    {
        Listbox list = (Listbox) getFellow("previewFormatList");
        Listitem item = list.getSelectedItem();
        if (item == null) item = list.getItemAtIndex(0);
        Object val = item.getValue();
        return (val != null) ? val.toString() : "html";
    }

    public void setPreviewFormat(String format)
    {
        Listbox list = (Listbox) getFellow("previewFormatList");
        for (int i=0; i < list.getItemCount(); i++) {
            Listitem item = list.getItemAtIndex(i);
            if (item.getValue().toString().equals(format)) {
                item.setSelected(true);
                list.invalidate();  // workaround for zk bug
                break;
            }
        }
    }

    public boolean isHTMLPreview()
    {
        return getPreviewFormat().equalsIgnoreCase("html");
    }

    public boolean isPDFPreview()
    {
        return getPreviewFormat().equalsIgnoreCase("pdf");
    }

    public String getPreviewPubConfigId()
    {
        Listbox list = (Listbox) getFellow("previewPubConfList");
        Listitem item = list.getSelectedItem();
        if (item == null) return null;
        Object val = item.getValue();
        if (val == null) return null;
        String str = val.toString();
        if (str.equals("")) return null;
        return str;
    }

    public DocmaPublicationConfig getPreviewPubConfig()
    {
        if (previewPubConfig == null) {
            DocmaSession docmaSess = getDocmaSession();
            String pub_id = getPreviewPubConfigId();
            previewPubConfig = docmaSess.getPublicationConfig(pub_id);
        }
        return previewPubConfig;
    }

    public String getPreviewPubContentRoot()
    {
        if (previewPubContentRoot == null) {
            DocmaPublicationConfig pub_conf = getPreviewPubConfig();
            if (pub_conf != null) {
                previewPubContentRoot = pub_conf.getContentRoot();
            }
        }
        return previewPubContentRoot;
    }

    public String getPreviewOutputConfigId()
    {
        Listbox list = (Listbox) getFellow("previewOutputConfList");
        Listitem item = list.getSelectedItem();
        if (item == null) return null;
        Object val = item.getValue();
        if (val == null) return null;
        String str = val.toString();
        if (str.equals("")) return null;
        return str;
    }

    public DocmaOutputConfig getPreviewOutputConfig()
    {
        String out_id = getPreviewOutputConfigId();
        if (out_id == null) return null;

        if ((previewOutputConfig != null) && !out_id.equals(previewOutputConfig.getId())) {
            previewOutputConfig = null;  // cached config is no longer the selected config
        }
        if (previewOutputConfig == null) {
            DocmaSession docmaSess = getDocmaSession();
            previewOutputConfig = docmaSess.getOutputConfig(out_id);
        }
        return previewOutputConfig;
    }

    public DocmaOutputConfig getPreviewHTMLConfig()
    {
        DocmaSession docmaSess = getDocmaSession();
        String storeId = docmaSess.getStoreId();
        // Get last selected HTML preview configuration
        String outId = getLastSelectedOutputConfigId(docmaSess, storeId, "html");
        if ((outId != null) && (outId.length() > 0)) {
            if ((previewHTMLConfig != null) && !outId.equals(previewHTMLConfig.getId())) {
                previewHTMLConfig = null;
            }
            if (previewHTMLConfig == null) {
                if ((previewOutputConfig != null) && outId.equals(previewOutputConfig.getId())) {
                    previewHTMLConfig = previewOutputConfig;
                } else {
                    previewHTMLConfig = docmaSess.getOutputConfig(outId);
                }
            }
            if (previewHTMLConfig != null) return previewHTMLConfig;
        }
        return getDefaultHTMLConfig();  // fallback to default
    }

    public String getLastSelectedFormat(DocmaSession docmaSess, String storeId)
    {
        return docmaSess.getUserProperty(GUIConstants.PROP_USER_PREVIEW_FORMAT + "." + storeId);
    }

    public void setLastSelectedFormat(DocmaSession docmaSess, String storeId, String format)
    {
        docmaSess.setUserProperty(GUIConstants.PROP_USER_PREVIEW_FORMAT + "." + storeId, format);
    }

    public String getLastSelectedPubConfigId(DocmaSession docmaSess, String storeId)
    {
        return docmaSess.getUserProperty(GUIConstants.PROP_USER_PREVIEW_PUBCONF + "." + storeId);
    }

    public void setLastSelectedPubConfigId(DocmaSession docmaSess, String storeId, String pubConfId)
    {
        docmaSess.setUserProperty(GUIConstants.PROP_USER_PREVIEW_PUBCONF + "." + storeId, pubConfId);
    }

    public String getLastSelectedOutputConfigId(DocmaSession docmaSess, String storeId, String format)
    {
        String outId = null;
        if ("pdf".equalsIgnoreCase(format)) {
            outId = docmaSess.getUserProperty(GUIConstants.PROP_USER_PREVIEW_OUTCONF_PDF + "." + storeId);
        } else { // if ("html".equals(format))
            outId = docmaSess.getUserProperty(GUIConstants.PROP_USER_PREVIEW_OUTCONF_HTML + "." + storeId);
        }
        return outId;
    }

    public void setLastSelectedOutputConfigId(DocmaSession docmaSess, String storeId, String format, String outConfId)
    {
        if ("pdf".equalsIgnoreCase(format)) {
            docmaSess.setUserProperty(GUIConstants.PROP_USER_PREVIEW_OUTCONF_PDF + "." + storeId, outConfId);
        } else {
            docmaSess.setUserProperty(GUIConstants.PROP_USER_PREVIEW_OUTCONF_HTML + "." + storeId, outConfId);
        }
    }

    public int getSelectedNodeCount() 
    {
        return docTree.getSelectedCount();
    }

    public DocmaNode getSelectedDocmaNode()
    {
        return getSelectedDocmaNode(false);
    }
    
    public DocmaNode getSelectedDocmaNode(boolean showError)
    {
        if (docTree.getSelectedCount() == 1) {
            Treeitem item = docTree.getSelectedItem();
            Object selobj = item.getValue();
            if (selobj instanceof DocmaNode) {
                return (DocmaNode) selobj;
            } else {
                if (showError) {
                    MessageUtil.showError(this, "text.no_valid_object_assign_to_tree_item");
                }
                return null;
            }
        } else {
            if (showError) {
                MessageUtil.showError(this, "text.no_tree_item_selected");
            }
            return null;
        }
    }
    
    public List<DocmaNode> getSelectedDocmaNodes(boolean siblingsOnly, boolean showSelectError)
    {
        return GUIUtil.getSelectedDocmaNodes(docTree, siblingsOnly, showSelectError);
    }

    public void refreshPreviewOnClientSide()
    {
        Clients.evalJavaScript("previewRefresh();");
    }

    public void clearPreview()
    {
        Iframe ifrm = (Iframe) getFellow("viewcontentfrm");
        ifrm.setSrc("empty_preview.html");
    }

    /* --------------  Event handlers  --------------- */

    public void onEvent(Event evt) throws Exception
    {
        // Component t = evt.getTarget();
        String name = evt.getName();
        if (DocmaConstants.DEBUG) {
            Log.info("Main window event: " + name);
        }
    }

    public void onClientInfo(Event evt)
    {
        try {
            if (evt instanceof ClientInfoEvent) {
                DocmaWebSession webSess = GUIUtil.getDocmaWebSession(this);
                if (webSess != null) {
                    ClientInfoEvent cinfo = (ClientInfoEvent) evt;
                    webSess.setScreenWidth(cinfo.getScreenWidth());
                    webSess.setScreenHeight(cinfo.getScreenHeight());
                    webSess.setDesktopWidth(cinfo.getDesktopWidth());
                    webSess.setDesktopHeight(cinfo.getDesktopHeight());
                    if (DocmaConstants.DEBUG) {
                        System.out.println("Screen width: " + webSess.getScreenWidth());
                        System.out.println("Screen height: " + webSess.getScreenHeight());
                        System.out.println("Desktop width: " + webSess.getDesktopWidth());
                        System.out.println("Desktop height: " + webSess.getDesktopHeight());
                    }
                } else {
                    Log.warning("DocmaWebSession is not set in MainWindow.onClientInfo!");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.error("Exception in onClientInfo: " + ex.getMessage());
        }
    }

    public void onLogout() throws Exception
    {
        try {
            DocmaWebSession webSess = GUIUtil.removeDocmaWebSession(this);
            webSess.closeSession();
        } finally {
            getDesktop().getExecution().sendRedirect("login.zul");
        }
    }

    public void onShowError(Event evt) throws Exception
    {
        Object data = evt.getData();
        if ((data == null) && (evt instanceof ForwardEvent)) {
            data = ((ForwardEvent) evt).getOrigin().getData();
        }
        String msg = (data == null) ? "Undefined Error!" : data.toString();
        Messagebox.show(msg);
    }

    public void onOpenWebDesigner() throws Exception
    {
        DocmaWebSession webSess = GUIUtil.getDocmaWebSession(this);
        int avail_width = 1200;
        int avail_height = 800;
        if (webSess != null) {
            avail_width = webSess.getDesktopWidth();
            avail_height = webSess.getDesktopHeight();
            if (avail_width <= 0) {
                avail_width = webSess.getScreenWidth();
            }
            if (avail_height <= 0) {
                avail_height = webSess.getScreenHeight();
            }
        }
        int win_width = (avail_width < 600) ? 600 : ((avail_width > 1300) ? 1300 : avail_width);
        int win_height = (avail_height < 600) ? 600 : ((avail_height > 1000) ? 1000 : avail_height);
        String client_action = "window.open('webdesigner/index.zul', 'docma_webdesigner_win'" +
          ",'width=" + win_width + ",height=" + win_height + 
          ",resizable=yes,scrollbars=yes,location=yes,menubar=yes,status=yes');";
        Clients.evalJavaScript(client_action);
    }

    public void onHelp(String path) throws Exception
    {
        openHelp(path);
    }

    public static void openHelp(String path) throws Exception
    {
        if (! path.contains("help/content/")) {
            path = path.replaceFirst("help/", "help/content/");
        }
        String client_action = "window.open('" + path + "', " +
          "'_blank', 'width=850,height=600,resizable=yes,scrollbars=yes,location=yes,menubar=yes,status=yes');";
        Clients.evalJavaScript(client_action);
    }

    public void onProcessDeferredEvents() throws Exception
    {
        try {
            if (DocmaConstants.DEBUG) {
                Log.info("Processing deferred events...");
            }
            if (docTreeModel != null) docTreeModel.processDeferredEvents();
        } catch (Exception ex) {
            ex.printStackTrace();
            // Log.error("Exception in onProcessDeferredEvents: " + ex.getMessage());
        }
    }

    public synchronized void onSetSelectedNodesByIds(Event evt) throws Exception
    {
        Object obj = evt.getData();
        if (obj == null) {
            Messagebox.show("Cannot set selected nodes. Missing parameter!");
            return;
        }
        String sel_ids = obj.toString().trim();
        if (DocmaConstants.DEBUG) {
            Log.info("onSetSelectedNodesByIds: " + sel_ids);
        }
        if (sel_ids.equals("")) {
            docTreeModel.clearSelection();
            return;
        }
        String[] node_ids = sel_ids.split("[, ]");
        DocmaSession docmaSess = getDocmaSession();
        ArrayList<DocmaNode> sel_nodes = new ArrayList<DocmaNode>(200);
        String lastParentId = null;
        for (String node_id : node_ids) {
            DocmaNode selnode = docmaSess.getNodeById(node_id);
            if (selnode != null) {
                sel_nodes.add(selnode);
                // Open parent node to assure child nodes are loaded.
                // Otherwise child will not be added to selection (ZK bug?)
                DocmaNode par = selnode.getParent();
                if (par != null) {
                    String parId = par.getId();
                    if ((parId != null) && !parId.equals(lastParentId)) {
                        if (DocConstants.DEBUG) Log.info("---- Before addOpenObject ----");
                        docTreeModel.addOpenPath(GUIUtil.getDocmaTreePath(par)); // is more efficient than addOpenObject()!
                        if (DocConstants.DEBUG) Log.info("---- After addOpenObject ----");
                        lastParentId = parId;
                    }
                }
            }
        }
        // docTree.clearSelection();  // is required, otherwise old selection is sometimes kept; ZK bug?
        docTreeModel.clearSelection();
        // docTreeModel.setSelection(sel_nodes);   // commented out because too inefficient
        for (DocmaNode sel_nd : sel_nodes) {
            docTreeModel.addSelectionPath(GUIUtil.getDocmaTreePath(sel_nd));
        }
        if (DocmaConstants.DEBUG) {
            Log.info("onSetSelectedNodesByIds model count: " + docTreeModel.getSelectionCount());
            Log.info("onSetSelectedNodesByIds tree count: " + docTree.getSelectedCount());
        }
    }


    public synchronized void onAddSelectedNodesByIds(Event evt) throws Exception
    {
        Object obj = evt.getData();
        if (obj == null) {
            Messagebox.show("Cannot add nodes to selection. Missing parameter!");
            return;
        }
        String sel_ids = obj.toString().trim();
        if (DocmaConstants.DEBUG) {
            Log.info("onAddSelectedNodesByIds: " + sel_ids);
        }
        if (sel_ids.equals("")) return;
        String[] node_ids = sel_ids.split("[, ]");
        DocmaSession docmaSess = getDocmaSession();
        String lastParentId = null;
        for (String node_id : node_ids) {
            DocmaNode selnode = docmaSess.getNodeById(node_id);
            if (selnode != null) {
                // Open parent node to assure child nodes are loaded.
                // Otherwise child will not be added to selection (ZK bug?)
                DocmaNode par = selnode.getParent();
                if (par != null) {
                    String parId = par.getId();
                    if ((parId != null) && !parId.equals(lastParentId)) {
                        docTreeModel.addOpenPath(GUIUtil.getDocmaTreePath(par));
                        lastParentId = parId;
                    }
                }
                // docTreeModel.addToSelection(selnode);
                docTreeModel.addSelectionPath(GUIUtil.getDocmaTreePath(selnode));  // more efficient than addToSelection()
            }
        }
    }

    // public void onSize(org.zkoss.zk.ui.event.SizeEvent se) throws Exception
    // {
    //     Messagebox.show("Event: Size");
    //     if (filtersettings_listbox != null) filtersettings_listbox.setWidth(null);
    // }

    public void onSelectStore() throws Exception
    {
        DocmaSession docmaSess = getDocmaSession();
        Listitem item = products_select_list.getSelectedItem();
        if (item != null) {
            String sId = item.getValue().toString();
            if (sId.equals("")) {
                closeDocStore(docmaSess);
            } else {
                openDocStore(docmaSess, sId, null);
            }
        }
    }

    public void onSelectVersion() throws Exception
    {
        DocmaSession docmaSess = getDocmaSession();
        Listitem s_item = products_select_list.getSelectedItem();
        Listitem v_item = versions_select_list.getSelectedItem();
        if ((s_item != null) && (v_item != null)) {
            String sId = s_item.getValue().toString();
            DocVersionId vId = docmaSess.createVersionId(v_item.getValue().toString());
            openDocStore(docmaSess, sId, vId);
        }
    }

    public void onSelectContentLanguage() throws Exception
    {
        DocmaSession docmaSess = getDocmaSession();
        Listitem lang_item = language_select_list.getSelectedItem();
        Object obj = lang_item.getValue();
        String lang_code = (obj == null) ? null : obj.toString();
        String old_lang = docmaSess.getTranslationMode();
        if ((old_lang == null) && (lang_code == null)) return;
        Treecol lang_col = (Treecol) getFellow("doctree_col_lang");
        if (lang_code == null) {
            docmaSess.leaveTranslationMode();
            lang_col.setVisible(false);
        } else {
            docmaSess.enterTranslationMode(lang_code);
            lang_col.setVisible(true);
            lang_col.setWidth(lang_col.getWidth());  // required due to ZK bug
        }
        // rerenderDocTree();
        // if (versioningInitialized) {
        //     loadProductVersions();
        // }

        updateVersionSelectionList(docmaSess);
        Menupopup treemenu = (Menupopup) getFellow("treemenu");
        MenuUtil.updateContentContextMenuLabels(treemenu, lang_code != null, getI18n());

        String selected_tab = getSelectedMainTabId();
        if ("contentTab".equals(selected_tab)) {
            String sid = docmaSess.getStoreId();
            String state = (sid == null) ? "" : docmaSess.getVersionState(sid, docmaSess.getVersionId());
            if (state.equalsIgnoreCase(DocmaConstants.VERSION_STATE_TRANSLATION_PENDING)) {
                contentInitialized = false;
                initContentTab();  // clear tree and show message that translation is pending
            } else {
                rerenderDocTree();   // rerender document tree immediately
            }
        } else {
            contentInitialized = false;  // reload when content tab is selected
        }

        if ("versioningTab".equals(selected_tab)) {
            guilist_productversions.loadProductVersions();  // reload version models immediately
        } else {
            versioningInitialized = false;  // reload when versioning tab is selected
        }

        if ("publishingTab".equals(selected_tab)) {
            loadPublicationExports();  // reload publication exports list immediately
        } else {
            publishingInitialized = false;  // reload when versioning tab is selected
        }

        clearPreview();
    }

    public void onTogglePreviewIndexTerms() throws Exception
    {
        if (isHTMLPreview()) {
            refreshPreviewOnClientSide();
        }
    }

    public void onToggleViewSource() throws Exception
    {
        // Save state 
        DocmaSession docmaSess = getDocmaSession();
        String storeId = docmaSess.getStoreId();
        boolean switchToSource = isViewSource();
        String val = switchToSource ? "true" : "false";
        docmaSess.setUserProperty(GUIConstants.PROP_USER_PREVIEW_SOURCE + "." + storeId, val);
        
        // Refresh preview frame
        if (switchToSource) {
            // Switch from preview to source.
            onDocTreeSelect();
        } else {
            // Switch from source to preview.
            // Save any unsaved changes on client side and then call onDocTreeSelect:
            Clients.evalJavaScript("saveAndReselectNode();");
        }
    }

    public void onSelectPreviewFormat() throws Exception
    {
        DocmaSession docmaSess = getDocmaSession();
        loadPreviewOutputConfList(docmaSess);   // reload output configurations based on new format
        onDocTreeSelect();
        setLastSelectedFormat(docmaSess, docmaSess.getStoreId(), getPreviewFormat());
    }

    public void onSelectPreviewPub() throws Exception
    {
        redrawPubContentRoot();
        onDocTreeSelect();
        DocmaSession docmaSess = getDocmaSession();
        setLastSelectedPubConfigId(docmaSess, docmaSess.getStoreId(), getPreviewPubConfigId());
    }

    public void onSelectPreviewOutput() throws Exception
    {
        onDocTreeSelect();
        DocmaSession docmaSess = getDocmaSession();
        String storeId = docmaSess.getStoreId();
        String format = getPreviewFormat();
        String outConfId = getPreviewOutputConfigId();
        setLastSelectedOutputConfigId(docmaSess, storeId, format, outConfId);
    }

    public void onTogglePreviewPathBar(Event evt) throws Exception
    {
        DocmaSession docmaSess = getDocmaSession();
        String new_state = null;
        if (evt != null) {
            Object obj = evt.getData();
            new_state = (obj == null) ? null : obj.toString();
        }
        boolean is_opened;
        if ((new_state != null) && (new_state.length() > 0)) {
            is_opened = new_state.equals("true");
        } else {
            String val = docmaSess.getUserProperty(GUIConstants.PROP_USER_PREVIEW_PATH_OPENED);
            boolean current_open = (val == null) || val.equals("") || val.equals("true");
            is_opened = ! current_open;
        }
        docmaSess.setUserProperty(GUIConstants.PROP_USER_PREVIEW_PATH_OPENED, is_opened ? "true" : "false");
    }
    
    public void onSwitchContentLayout() throws Exception
    {
        boolean was_horizontal = isContentLayoutHorizontal();
        boolean is_horizontal = ! was_horizontal;
        DocmaSession docmaSess = getDocmaSession();
        setDocTreeLayout(is_horizontal, docmaSess);
        setDocTreeColWidths(docmaSess, is_horizontal);
        if (is_horizontal) {
            docmaSess.setUserProperty(GUIConstants.PROP_USER_CONTENT_WIN_LAYOUT, GUIConstants.CONTENT_WIN_LAYOUT_HORIZONTAL);
        } else {
            docmaSess.setUserProperty(GUIConstants.PROP_USER_CONTENT_WIN_LAYOUT, GUIConstants.CONTENT_WIN_LAYOUT_VERTICAL);
        }
    }

    public boolean isContentLayoutHorizontal()
    {
        Component comp = docTree.getParent();
        return (comp instanceof North);
    }

    public void onSelectAdminTab() throws Exception
    {
        if (! adminInitialized) {
            Clients.showBusy("Retrieving administration settings...");
            Events.echoEvent("onInitAdminTab", this, null);
        }
        setDocTreeToolbarEnabled(false);
        setStoreSelectionEnabled(false);
        setVersionSelectionEnabled(false);
        setLanguageSelectionEnabled(false);
    }

    public void onInitAdminTab()
    {
        String error = null;
        try {
            initAdminTab();
        } catch (Exception ex) {
            error = "Error: " + ex.getMessage();
        } finally {
            Clients.clearBusy();
        }
        if (error != null) {
            Messagebox.show(error);
        }
    }

    public void onSelectUsersTab()
    {
        Clients.showBusy("Retrieving user list...");
        Events.echoEvent("onLoadUserList", this, null);
    }

    /**
     * This is a follow-up event sent by onSelectUsersTab().
     */
    public void onLoadUserList() 
    {
        String error = null;
        try {
            guilist_usersgroups.loadUsersIfEmpty();
        } catch (Exception ex) {
            error = "Error: " + ex.getMessage();
        } finally {
            Clients.clearBusy();
        }
        if (error != null) {
            Messagebox.show(error);
        }
    }

    public void onSelectUserGroupsTab() throws Exception
    {
        guilist_usersgroups.loadUserGroupsIfEmpty();
    }

    public void onSelectContentTab() throws Exception
    {
        initContentTab();
        setDocTreeToolbarEnabled(true);
        setStoreSelectionEnabled(true);
        setVersionSelectionEnabled(true);
        setLanguageSelectionEnabled(true);
    }

    public void onSelectStylesTab() throws Exception
    {
        initStylesTab();
        setDocTreeToolbarEnabled(false);
        setStoreSelectionEnabled(true);
        setVersionSelectionEnabled(true);
        setLanguageSelectionEnabled(false);
    }

    public void onSelectVersioningTab() throws Exception
    {
        initVersioningTab();
        setDocTreeToolbarEnabled(false);
        setStoreSelectionEnabled(true);
        setVersionSelectionEnabled(false);
        setLanguageSelectionEnabled(true);
    }

    public void onSelectPublishingTab() throws Exception
    {
        initPublishingTab();
        setDocTreeToolbarEnabled(false);
        setStoreSelectionEnabled(true);
        setVersionSelectionEnabled(true);
        setLanguageSelectionEnabled(true);
    }

    public void onSelectCharEntitiesTab() throws Exception
    {
        if (guilist_char_entities.isEmpty()) {
            guilist_char_entities.loadCharEntities();
        } else {
            guilist_char_entities.refreshIfDirty();
        }
    }

    public void onSelectAutoFormatTab() throws Exception
    {
        guilist_autoformatconfig.loadAll();  // load list if not already loaded
    }

    public void onSelectRulesTab() throws Exception
    {
        guilist_rules.onShowList();  // load list if not already loaded
    }
    
    public void onSelectPluginsTab() throws Exception
    {
        guilist_plugins.loadAll();  // load list if not already loaded
    }

    public void onNewRule() 
    {
        guilist_rules.onNewRule();
    }
    
    public void onEditRule() 
    {
        guilist_rules.onEditRule();
    }
    
    public void onDeleteRule() 
    {
        guilist_rules.onDeleteRule();
    }
    
    public void onCopyRule() throws Exception
    {
        guilist_rules.onCopyRule();
    }
    
    public void onEnableRule() 
    {
        guilist_rules.onEnableRule();
    }
    
    public void onDisableRule() 
    {
        guilist_rules.onDisableRule();
    }
    
    public void onInstallPlugin() throws Exception
    {
        guilist_plugins.installPlugin();
    }

    public void onUninstallPlugin() throws Exception
    {
        guilist_plugins.uninstallPlugin();
    }

    public void onEnablePlugin() throws Exception
    {
        guilist_plugins.enablePlugin();
    }

    public void onDisablePlugin() throws Exception
    {
        guilist_plugins.disablePlugin();
    }

    public void onSelectEditAppsTab() throws Exception
    {
        guilist_edit_extensions.loadAll();  // load list if not already loaded
    }
    
    public void onSelectViewAppsTab() throws Exception
    {
        guilist_view_extensions.loadAll();  // load list if not already loaded
    }
    
    public void onDocTreeSelect() throws Exception
    {
        // SelectEvent se = (SelectEvent) fe.getOrigin();
        DocmaNode node = null;
        String anchor = "";
        if (docTree.getSelectedCount() == 1) {
            Treeitem item = docTree.getSelectedItem();
            Object node_obj = item.getValue();
            if (node_obj instanceof DocmaAnchor) {
                anchor =  "#" + ((DocmaAnchor) node_obj).getAlias();
                Treeitem parent_item = item.getParentItem();
                Object parent_node = parent_item.getValue();
                if (parent_node instanceof DocmaNode) {  // should always be the case
                    node = (DocmaNode) parent_node;
                }
            } else
            if (node_obj instanceof DocmaNode) {
                node = (DocmaNode) node_obj;
            }
        }
        doPreviewNode(node, anchor);
    }

    public void onPreviewContentByAlias(Event evt) throws Exception
    {
        DocmaSession docmaSess = getDocmaSession();
        Object obj = evt.getData();
        if (obj == null) {
            Messagebox.show("Cannot preview node. Missing parameter!");
            return;
        }
        String node_alias = obj.toString();
        DocmaNode node = docmaSess.getNodeByAlias(node_alias);
        if (node == null) {
            Messagebox.show("Cannot preview node. Object with alias '" + node_alias + "' not found!");
        } else {
            String anchor = null;
            if (! node_alias.equals(node.getLinkAlias())) {
                anchor = "#" + node_alias;
            }
            doPreviewNode(node, anchor);
        }
    }

    public void onPreviewNodeById(Event evt) throws Exception
    {
        DocmaSession docmaSess = getDocmaSession();
        Object obj = evt.getData();
        if (obj == null) {
            Messagebox.show("Cannot preview node. Missing parameter!");
            return;
        }
        String node_id = obj.toString();
        DocmaNode node = docmaSess.getNodeById(node_id);
        if (node == null) {
            Messagebox.show("Cannot preview node. Object with ID '" + node_id + "' not found!");
        } else {
            doPreviewNode(node, null);
        }
    }

    private void doPreviewNode(DocmaNode node, String anchor) throws Exception
    {
        if (anchor == null) anchor = "";
        String view_url = null;
        if (node != null) {
            String deskId = getDesktop().getId();
            String stamp = "&stamp=" + System.currentTimeMillis();
            if (node.isImageFolder()) {
                view_url = getDesktop().getExecution().encodeURL("viewImageFolder.jsp?nodeid=" +
                           node.getId() + "&desk=" + deskId + stamp);
            } else
            if (node.isSystemFolder()) {
                view_url = getDesktop().getExecution().encodeURL("viewSystemFolder.jsp?nodeid=" +
                           node.getId() + "&desk=" + deskId + stamp);
            } else
            if (node.isImageContent()) {
                view_url = getDesktop().getExecution().encodeURL("viewImage.jsp?nodeid=" +
                           node.getId() + "&desk=" + deskId + stamp);
            } else 
            if (node.isFileContent()) {
                String ext = node.getFileExtension();
                if (VideoUtil.isSupportedVideoExtension(ext)) {
                    view_url = getDesktop().getExecution().encodeURL("viewVideo.jsp?nodeid=" +
                               node.getId() + "&desk=" + deskId + stamp);
                } else {
                    view_url = getPreviewURL(ext, node.getId());
                }
            } else 
            if (isViewSource() && node.isContent()) {
                String ext = node.isHTMLContent() ? "html" : node.getFileExtension();
                view_url = getPreviewURL(node.getFileExtension(), node.getId());
            } else {
                String pubId = getPreviewPubConfigId();
                String pubParam = (pubId != null) ? ("&pub=" + pubId) : "";
                String outId = getPreviewOutputConfigId();
                String outParam = (outId != null) ? ("&out=" + outId) : "";
                String pagename = isPDFPreview() ? "viewPDF.jsp" : "viewContent.jsp";
                view_url = getDesktop().getExecution().encodeURL(pagename +
                           "?nodeid=" + node.getId() + "&desk=" + deskId +
                           pubParam + outParam + stamp + anchor);

                // Do not preview ancestor sections of preview publication root:
                if (node.isSection()) {
                    String pub_root_id = getPreviewPubContentRoot();
                    if (pub_root_id != null) {
                        if (node.isAncestor(pub_root_id)) view_url = null;
                    }
                }
            }
        }
        if (view_url == null) {
            clearPreview();
        } else {
            Iframe ifrm = (Iframe) getFellow("viewcontentfrm");
            ifrm.setSrc(view_url);
        }

        SearchReplaceDialog dialog = (SearchReplaceDialog) getPage().getFellow("SearchReplaceDialog");
        dialog.nodeChanged();
    }
    
    private String getPreviewURL(String ext, String nodeId)
    {
        if ((ext != null) && !ext.equals("")) {
            // Get assigned viewer application for this file extension
            DocmaWebSession webSess = getDocmaWebSession();
            String appid = webSess.getFileViewerId(ext);
            if (appid == null) {
                // If the file has no configured text-file extension, 
                // then the system default text editor shows a hint that
                // file extension needs to be configured.
                appid = webSess.getDocmaWebApplication().getSystemDefaultTextEditor();
            }
            if (appid != null) {
                return webSess.getPreviewURL(appid, nodeId);
            }
        }
        return null;  // invalid extension or missing viewer application
    }

    public void onResizeContentTreeRegion(Event evt)
    {
        if (evt instanceof ForwardEvent) {
            evt = ((ForwardEvent) evt).getOrigin();
        }
        if (evt instanceof SizeEvent) {
            SizeEvent se = (SizeEvent) evt;
            DocmaSession docmaSess = getDocmaSession();
            if (isContentLayoutHorizontal()) {
                String sz = se.getHeight();
                docmaSess.setUserProperty(GUIConstants.PROP_USER_CONTENT_TREE_SIZE_HORIZONTAL, sz);
            } else {
                String sz = se.getWidth();
                docmaSess.setUserProperty(GUIConstants.PROP_USER_CONTENT_TREE_SIZE_VERTICAL, sz);
            }
        } else {
            Log.warning("Unknown event in onResizeContentTreeRegion(): " + evt.getClass().getName());
        }
    }

    public void onDocTreeColSize() throws Exception
    {
        DocmaSession docmaSess = getDocmaSession();
        String prop_prefix;
        if (isContentLayoutHorizontal()) {
            prop_prefix = GUIConstants.PROP_USER_CONTENT_TREE_COL_WIDTH_HORIZONTAL;
        } else {
            prop_prefix = GUIConstants.PROP_USER_CONTENT_TREE_COL_WIDTH_VERTICAL;
        }
        Treecols tcols = docTree.getTreecols();
        List child_list = tcols.getChildren();
        List col_list = new ArrayList(child_list.size());
        for (int i=0; i < child_list.size(); i++) {
            Object child = child_list.get(i);
            if (child instanceof Treecol) {
                col_list.add(child);
            }
        }
        String[] prop_names = new String[col_list.size()];
        String[] prop_values = new String[col_list.size()];
        for (int i=0; i < col_list.size(); i++) {
            Treecol tcol = (Treecol) col_list.get(i);
            prop_names[i] = prop_prefix + "." + tcol.getId();
            prop_values[i] = tcol.getWidth();
        }
        docmaSess.setUserProperties(prop_names, prop_values);
    }

    public void onSelectUserGroup()
    {
        guilist_usersgroups.onSelectUserGroup();
    }

    /**
     * This is a follow-up event created by onSelectUserGroup.
     */
    public void onShowUserGroupData()
    {
        guilist_usersgroups.onShowUserGroupData();
    }

    public void onOpenContentMenu(ForwardEvent fe) throws Exception
    {
        if (((OpenEvent) fe.getOrigin()).isOpen()) {
            Menupopup contmenu = (Menupopup) getFellow("contentmenu");
            DocmaWebSession webSess = GUIUtil.getDocmaWebSession(this);
            webSess.propagateMenuOpenEventToPlugins(contmenu);
            MenuUtil.updateContentMainMenu(contmenu, this);
        }
    }
    
    public void onOpenTreeMenu(ForwardEvent fe) throws Exception
    {
        if (((OpenEvent) fe.getOrigin()).isOpen()) {
            Menupopup treemenu = (Menupopup) getFellow("treemenu");
            DocmaWebSession webSess = GUIUtil.getDocmaWebSession(this);
            webSess.propagateMenuOpenEventToPlugins(treemenu);
            MenuUtil.updateContentContextMenu(treemenu, this);
        }
    }
    
    public void onAddSubNode() throws Exception
    {
        Window dialog = (Window) getPage().getFellow("SelectNodeTypeDialog");
        SelectNodeTypeComposer composer = (SelectNodeTypeComposer) dialog.getAttribute("$composer");
        composer.appendSubNode(this);
    }

    public void onInsertNode() throws Exception
    {
        Window dialog = (Window) getPage().getFellow("SelectNodeTypeDialog");
        SelectNodeTypeComposer composer = (SelectNodeTypeComposer) dialog.getAttribute("$composer");
        composer.insertNodeHere(this);
    }

    public void onViewContent() throws Exception
    {
        Treeitem item = docTree.getSelectedItem();
        if (item == null) {
            Log.warning("Call of onViewContent() without node selection!");
            return;
        }
        Object obj = item.getValue();
        if (obj == null) {
            Messagebox.show("Cannot open viewer. No object assigned to tree item!");
        }
        if (obj instanceof DocmaNode) {
            DocmaNode node = (DocmaNode) obj;
            if (node.isContent()) {
                openFileNodeInWindow(node, false);
            }
        }
    }
    
    public void onEditContent() throws Exception
    {
        Treeitem item = docTree.getSelectedItem();
        if (item == null) {
            Log.warning("Call of onEditContent() without node selection!");
            return;
        }
        Object obj = item.getValue();
        if (obj == null) {
            Messagebox.show("Cannot open editor. No object assigned to tree item!");
        }
        if (obj instanceof DocmaNode) {
            DocmaNode node = (DocmaNode) obj;
            doEditContent(node);
        }
    }

    public void onEditContentClick(Event evt) throws Exception
    {
        DocmaSession docmaSess = getDocmaSession();
        Object obj = evt.getData();
        if (obj == null) {
            Messagebox.show("Cannot open editor. Missing parameter!");
            return;
        }
        String node_id = obj.toString();
        DocmaNode node = docmaSess.getNodeById(node_id);
        if (node == null) {
            Messagebox.show("Cannot open editor. Object with this ID does not exist!");
            return;
        }
        doEditContent(node);
    }

    void doEditContent(DocmaNode node) throws Exception
    {
        if (! GUIUtil.isEditContentAllowed(node, true)) {
            return;
        }
        if (node.isHTMLContent()) {
            getDocmaWebSession().openContentEditor(node.getId());
        } else if (node.isFileContent() || node.isImageContent()) {
            openFileNodeInWindow(node, true);
        } else {
            doEditNodeProps(node, null);
        }
    }

    void openFileNodeInWindow(DocmaNode node, boolean editMode)
    {
        String ext = node.isHTMLContent() ? "html" : node.getFileExtension();
        if ((ext != null) && !ext.equals("")) {
            DocmaWebSession webSess = getDocmaWebSession();
            String appid = editMode ? webSess.getFileEditorId(ext) 
                                    : webSess.getFileViewerId(ext);
            if (appid != null) {
                try {
                    if (editMode) {
                        webSess.openEditor(appid, node.getId());
                    } else {
                        webSess.openViewer(appid, node.getId());
                    }
                } catch (Exception ex) {
                    Messagebox.show(ex.getLocalizedMessage());
                }
            } else {
                if (editMode) {
                    Messagebox.show("No editor assigned to file extension '" + ext + "'.");
                } else {
                    Messagebox.show("No viewer assigned to file extension '" + ext + "'.");
                }
            }
        } else {
            Messagebox.show("Unknown file type.");
        }
    }

    public void onEditNodeProps() throws Exception
    {
        int sel_cnt = docTree.getSelectedCount();
        if (sel_cnt > 1) {
            Messagebox.show("Please select a single node!");
            return;
        }
        Treeitem item = docTree.getSelectedItem();
        if (item == null) {
            Messagebox.show("Cannot edit properties: No node selected!");
            return;
        }
        DocmaNode node = (DocmaNode) item.getValue();
        doEditNodeProps(node, null);
    }

    public void onEditNodePropsById(Event evt) throws Exception
    {
        Object obj = evt.getData();
        if (obj == null) {
            Messagebox.show("Cannot open node properties editor. Missing parameter!");
            return;
        }
        String node_id = obj.toString();
        DocmaSession docmaSess = getDocmaSession();
        DocmaNode node = docmaSess.getNodeById(node_id);
        if (node == null) {
            Messagebox.show("Node with ID " + node_id + " does not exist.");
            return;
        }
        doEditNodeProps(node, new Callback() {
            public void onEvent(String evt) 
            {
                refreshPreviewOnClientSide();
            }
        });
    }

    void doEditNodeProps(final DocmaNode node, final Callback okAction) throws Exception
    {
        try {
            final DocmaSession docmaSess = getDocmaSession();
            if (node.isFolder()) {
                doEditFolder(node, docmaSess, okAction);
                return;
            }
            if (node.isReference()) {
                doEditReference(node, docmaSess, okAction);
                return;
            }
            Window dialog = (Window) getPage().getFellow("NodePropertiesDialog");
            final NodePropertiesComposer composer = (NodePropertiesComposer) dialog.getAttribute("$composer");
            Callback okHandler = new Callback() {
                public void onEvent(String eventName) 
                {
                    try {
                        composer.updateModel(node, docmaSess);
                        if (okAction != null) {
                            okAction.onEvent(eventName);
                        }
                    } catch (Exception ex) {
                        Messagebox.show("Error: " + ex.getMessage());
                    }
                }
            };
            if (node.isContent()) {
                boolean hasApproveRight = docmaSess.hasRight(AccessRights.RIGHT_APPROVE_CONTENT);
                String wfstate = node.getWorkflowStatus();
                if ("approved".equalsIgnoreCase(wfstate) && !hasApproveRight) {
                    Messagebox.show("Content is already approved! You need the 'Approve content' right to change the state.");
                    return;
                }
                composer.doEdit_ContentProps(node, docmaSess, okHandler);
            } else
            if (node.isSection()) {
                composer.doEdit_SectionProps(node, docmaSess, okHandler);
            } else {
                Messagebox.show("Cannot edit properties: Invalid node type!");
            }
        } catch (Exception ex) {
            Messagebox.show("Error: " + ex.getMessage());
        }
    }

    public void onAddContent() throws Exception
    {
        try {
            final DocmaSession docmaSess = getDocmaSession();
            final Treeitem item = docTree.getSelectedItem();
            final DocmaNode node = (DocmaNode) item.getValue();

            if (node == null) {
                Messagebox.show("Internal Error: No object assigned to tree item!");
                return;
            }
            Window dialog = (Window) getPage().getFellow("NodePropertiesDialog");
            final NodePropertiesComposer composer = (NodePropertiesComposer) dialog.getAttribute("$composer");
            
            composer.doEdit_ContentProps(null, docmaSess, new Callback() {  // edit new node
                public void onEvent(String eventName) 
                {
                    if (NodePropertiesComposer.EVENT_OKAY.equals(eventName)) {  // OK button clicked
                        try {
                            DocmaNode new_content = docmaSess.createHTMLContent();
                            composer.updateModel(new_content, docmaSess);
                            int insert_pos = node.getDefaultInsertPos(new_content);
                            node.insertChild(insert_pos, new_content);
                            if (! item.isOpen()) item.setOpen(true);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            Messagebox.show("Error: " + ex.getMessage());
                        }
                    }
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            Messagebox.show("Error: " + ex.getMessage());
        }
    }

    public void onInsertContent() throws Exception
    {
        try {
            final DocmaSession docmaSess = getDocmaSession();
            Treeitem item = docTree.getSelectedItem();
            final DocmaNode node = (DocmaNode) item.getValue();

            if (node == null) {
                Messagebox.show("Internal Error: No object assigned to tree item!");
                return;
            }
            Window dialog = (Window) getPage().getFellow("NodePropertiesDialog");
            final NodePropertiesComposer composer = (NodePropertiesComposer) dialog.getAttribute("$composer");
            
            composer.doEdit_ContentProps(null, docmaSess, new Callback() {  // edit new node
                public void onEvent(String eventName) 
                {
                    if (NodePropertiesComposer.EVENT_OKAY.equals(eventName)) {  // OK button clicked
                        try {
                            DocmaNode parentNode = node.getParent();
                            int insert_pos = parentNode.getChildPos(node);
                            if (! parentNode.isInsertContentAllowed(insert_pos)) {
                                Messagebox.show("Cannot insert content here!");
                                return;
                            }
                            DocmaNode new_content = docmaSess.createHTMLContent();
                            composer.updateModel(new_content, docmaSess);
                            parentNode.insertChild(insert_pos, new_content);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            Messagebox.show("Error: " + ex.getMessage());
                        }
                    }
                }
            });
        } catch (Exception ex) {
            Messagebox.show("Error: " + ex.getMessage());
        }
    }

    public void onAddEmptyTextFile() throws Exception
    {
        try {
            final DocmaSession docmaSess = getDocmaSession();
            final Treeitem item = docTree.getSelectedItem();
            final DocmaNode node = (DocmaNode) item.getValue();

            if (node == null) {
                Messagebox.show("Internal Error: No object assigned to tree item!");
                return;
            }
            Window dialog = (Window) getPage().getFellow("NodePropertiesDialog");
            final NodePropertiesComposer composer = (NodePropertiesComposer) dialog.getAttribute("$composer");
            
            composer.doEdit_NewTextFileProps(docmaSess, new Callback() {  // edit new node
                public void onEvent(String eventName) 
                {
                    if (NodePropertiesComposer.EVENT_OKAY.equals(eventName)) {  // OK button clicked
                        try {
                            DocmaNode new_content = docmaSess.createFileContent();
                            composer.updateModel(new_content, docmaSess);
                            int insert_pos = node.getDefaultInsertPos(new_content);
                            node.insertChild(insert_pos, new_content);
                            new_content.setContentString("");
                            if (! item.isOpen()) item.setOpen(true);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            Messagebox.show("Error: " + ex.getMessage());
                        }
                    }
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            Messagebox.show("Error: " + ex.getMessage());
        }
    }

    public void onInsertEmptyTextFile() throws Exception
    {
        try {
            final DocmaSession docmaSess = getDocmaSession();
            Treeitem item = docTree.getSelectedItem();
            final DocmaNode node = (DocmaNode) item.getValue();

            if (node == null) {
                Messagebox.show("Internal Error: No object assigned to tree item!");
                return;
            }
            Window dialog = (Window) getPage().getFellow("NodePropertiesDialog");
            final NodePropertiesComposer composer = (NodePropertiesComposer) dialog.getAttribute("$composer");
            
            composer.doEdit_NewTextFileProps(docmaSess, new Callback() {  // edit new node
                public void onEvent(String eventName) 
                {
                    if (NodePropertiesComposer.EVENT_OKAY.equals(eventName)) {  // OK button clicked
                        try {
                            DocmaNode parentNode = node.getParent();
                            int insert_pos = parentNode.getChildPos(node);
                            DocmaNode new_content = docmaSess.createFileContent();
                            composer.updateModel(new_content, docmaSess);
                            parentNode.insertChild(insert_pos, new_content);
                            new_content.setContentString("");
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            Messagebox.show("Error: " + ex.getMessage());
                        }
                    }
                }
            });
        } catch (Exception ex) {
            Messagebox.show("Error: " + ex.getMessage());
        }
    }

    public void onAddSubSection() throws Exception
    {
        try {
            final DocmaSession docmaSess = getDocmaSession();
            final Treeitem item = docTree.getSelectedItem();
            final DocmaNode node = (DocmaNode) item.getValue();
            if (node == null) {
                Messagebox.show("Internal Error: No object assigned to tree item!");
                return;
            }
            Window dialog = (Window) getPage().getFellow("NodePropertiesDialog");
            final NodePropertiesComposer composer = (NodePropertiesComposer) dialog.getAttribute("$composer");
            
            composer.doEdit_SectionProps(null, docmaSess, new Callback() {  // edit new node
                public void onEvent(String eventName) 
                {
                    if (NodePropertiesComposer.EVENT_OKAY.equals(eventName)) {  // OK button clicked
                        try {
                            DocmaNode new_section = docmaSess.createSection();
                            // new_section.setTitle("New section");
                            composer.updateModel(new_section, docmaSess);
                            node.addChild(new_section);
                            if (! item.isOpen()) item.setOpen(true);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            Messagebox.show("Error: " + ex.getMessage());
                        }
                    }
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            Messagebox.show("Error: " + ex.getMessage());
        }
    }

    public void onAddSubSectionWithContent() throws Exception
    {
        try {
            final DocmaSession docmaSess = getDocmaSession();
            final Treeitem item = docTree.getSelectedItem();
            final DocmaNode node = (DocmaNode) item.getValue();
            if (node == null) {
                Messagebox.show("Internal Error: No object assigned to tree item!");
                return;
            }
            Window dialog = (Window) getPage().getFellow("NodePropertiesDialog");
            final NodePropertiesComposer composer = (NodePropertiesComposer) dialog.getAttribute("$composer");
            
            composer.doEdit_SectionProps(null, docmaSess, new Callback() {  // edit new node
                public void onEvent(String eventName) 
                {
                    if (NodePropertiesComposer.EVENT_OKAY.equals(eventName)) {  // OK button clicked
                        try {
                            DocmaNode new_section = docmaSess.createSection();
                            // new_section.setTitle("New section");
                            composer.updateModel(new_section, docmaSess);
                            node.addChild(new_section);
                            if (! item.isOpen()) item.setOpen(true);
                            
                            DocmaNode new_content = docmaSess.createHTMLContent();
                            new_content.setTitle(new_section.getTitle());
                            new_section.addChild(new_content);
                            Treeitem sect_item = getDocTree().getTreeitemByDocmaNode(new_section);
                            if (sect_item != null) sect_item.setOpen(true);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            Messagebox.show("Error: " + ex.getMessage());
                        }
                    }
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            Messagebox.show("Error: " + ex.getMessage());
        }
    }

    public void onInsertSection() throws Exception
    {
        try {
            final DocmaSession docmaSess = getDocmaSession();
            final Treeitem item = docTree.getSelectedItem();
            final DocmaNode node = (DocmaNode) item.getValue();
            if (node == null) {
                Messagebox.show("Internal Error: No object assigned to tree item!");
                return;
            }
            if (node.isContent() || node.isContentIncludeReference()) {
                Messagebox.show("Cannot insert section before content node!");
                return;
            }
            Window dialog = (Window) getPage().getFellow("NodePropertiesDialog");
            final NodePropertiesComposer composer = (NodePropertiesComposer) dialog.getAttribute("$composer");
            
            composer.doEdit_SectionProps(null, docmaSess, new Callback() {  // edit new node
                public void onEvent(String eventName) 
                {
                    if (NodePropertiesComposer.EVENT_OKAY.equals(eventName)) {  // OK button clicked
                        try {
                            DocmaNode new_section = docmaSess.createSection();
                            composer.updateModel(new_section, docmaSess);
                            DocmaNode parentNode = node.getParent();
                            int ins_pos = parentNode.getChildPos(node);
                            parentNode.insertChild(ins_pos, new_section);
                            // return new_section;
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            Messagebox.show("Error: " + ex.getMessage());
                        }
                    }
                }
            });
        } catch (Exception ex) {
            Messagebox.show("Error: " + ex.getMessage());
        }
    }

    public void onInsertSectionWithContent() throws Exception
    {
        try {
            final DocmaSession docmaSess = getDocmaSession();
            final Treeitem item = docTree.getSelectedItem();
            final DocmaNode node = (DocmaNode) item.getValue();
            if (node == null) {
                Messagebox.show("Internal Error: No object assigned to tree item!");
                return;
            }
            if (node.isContent() || node.isContentIncludeReference()) {
                Messagebox.show("Cannot insert section before content node!");
                return;
            }
            Window dialog = (Window) getPage().getFellow("NodePropertiesDialog");
            final NodePropertiesComposer composer = (NodePropertiesComposer) dialog.getAttribute("$composer");
            
            composer.doEdit_SectionProps(null, docmaSess, new Callback() {  // edit new node
                public void onEvent(String eventName) 
                {
                    if (NodePropertiesComposer.EVENT_OKAY.equals(eventName)) {  // OK button clicked
                        try {
                            DocmaNode new_section = docmaSess.createSection();
                            composer.updateModel(new_section, docmaSess);
                            DocmaNode parentNode = node.getParent();
                            int ins_pos = parentNode.getChildPos(node);
                            parentNode.insertChild(ins_pos, new_section);
                            
                            DocmaNode new_content = docmaSess.createHTMLContent();
                            new_content.setTitle(new_section.getTitle());
                            new_section.addChild(new_content);
                            Treeitem sect_item = getDocTree().getTreeitemByDocmaNode(new_section);
                            if (sect_item != null) sect_item.setOpen(true);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            Messagebox.show("Error: " + ex.getMessage());
                        }
                    }
                }
            });
        } catch (Exception ex) {
            Messagebox.show("Error: " + ex.getMessage());
        }
    }

    public void onEditReference() throws Exception
    {
        DocmaSession docmaSess = getDocmaSession();
        Treeitem item = docTree.getSelectedItem();
        DocmaNode node = (DocmaNode) item.getValue();
        doEditReference(node, docmaSess, null);
    }

    private void doEditReference(final DocmaNode node, 
                                 final DocmaSession docmaSess, 
                                 final Callback okAction) throws Exception
    {
        if (node == null) {
            Messagebox.show("Internal Error: No object assigned to tree item!");
            return;
        }
        Window dialog = (Window) getPage().getFellow("ReferenceDialog");
        final ReferenceDialogComposer composer = (ReferenceDialogComposer) dialog.getAttribute("$composer");
        if (node.isSectionIncludeReference()) {
            composer.setMode_EditSectionRef();
        } else if (node.isImageIncludeReference()) {
            composer.setMode_EditImageRef();
        } else {
            composer.setMode_EditContentRef();
        }
        composer.doEdit(node, docmaSess, new Callback() {
            public void onEvent(String eventName) 
            {
                if (ReferenceDialogComposer.EVENT_OKAY.equals(eventName)) {  // OK button clicked
                    try {
                        composer.updateModel(node, docmaSess);
                        if (okAction != null) {
                            okAction.onEvent(eventName);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Messagebox.show("Error: " + ex.getMessage());
                    }
                }
            }
        });
    }


    private void onAddInclude(final boolean isSection, 
                              final boolean isContent, 
                              final boolean isImage)
    throws Exception
    {
        final DocmaSession docmaSess = getDocmaSession();
        final Treeitem item = docTree.getSelectedItem();
        final DocmaNode node = (DocmaNode) item.getValue();
        if (node == null) {
            Messagebox.show("Internal Error: No object assigned to tree item!");
            return;
        }
        Window dialog = (Window) getPage().getFellow("ReferenceDialog");
        final ReferenceDialogComposer composer = (ReferenceDialogComposer) dialog.getAttribute("$composer");
        
        if (isSection) composer.setMode_NewSectionRef();
        if (isContent) composer.setMode_NewContentRef();
        if (isImage) composer.setMode_NewImageRef();
        composer.doEdit(null, docmaSess, new Callback() {  // null means dialog fields are cleared
            public void onEvent(String eventName) 
            {
                if (ReferenceDialogComposer.EVENT_OKAY.equals(eventName)) {  // OK button clicked
                    try {
                        DocmaNode new_ref = null;
                        if (isSection) new_ref = docmaSess.createSectionIncludeReference();
                        if (isContent) new_ref = docmaSess.createContentIncludeReference();
                        if (isImage) new_ref = docmaSess.createImageIncludeReference();
                        composer.updateModel(new_ref, docmaSess);
                        int insert_pos = node.getDefaultInsertPos(new_ref);
                        node.insertChild(insert_pos, new_ref);
                        if (! item.isOpen()) item.setOpen(true);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Messagebox.show("Error: " + ex.getMessage());
                    }
                }
            }
        });
    }


    private void onInsertInclude(final boolean isSection, 
                                 final boolean isContent, 
                                 final boolean isImage)
    throws Exception
    {
        final DocmaSession docmaSess = getDocmaSession();
        final Treeitem item = docTree.getSelectedItem();
        final DocmaNode node = (DocmaNode) item.getValue();
        if (node == null) {
            Messagebox.show("Internal Error: No object assigned to tree item!");
            return;
        }
        Window dialog = (Window) getPage().getFellow("ReferenceDialog");
        final ReferenceDialogComposer composer = (ReferenceDialogComposer) dialog.getAttribute("$composer");
        
        if (isSection) composer.setMode_NewSectionRef();
        if (isContent) composer.setMode_NewContentRef();
        if (isImage) composer.setMode_NewImageRef();
        composer.doEdit(null, docmaSess, new Callback() {  // null means dialog fields are cleared
            public void onEvent(String eventName) 
            {
                if (ReferenceDialogComposer.EVENT_OKAY.equals(eventName)) {  // OK button clicked
                    try {
                        DocmaNode parentNode = node.getParent();
                        int insert_pos = parentNode.getChildPos(node);
                        if (isSection && !parentNode.isInsertSectionAllowed(insert_pos)) {
                            Messagebox.show("Cannot insert section here!");
                            return;
                        }
                        if ((isContent || isImage) && !parentNode.isInsertContentAllowed(insert_pos)) {
                            Messagebox.show("Cannot insert content here!");
                            return;
                        }
                        DocmaNode new_ref = null;
                        if (isSection) new_ref = docmaSess.createSectionIncludeReference();
                        if (isContent) new_ref = docmaSess.createContentIncludeReference();
                        if (isImage) new_ref = docmaSess.createImageIncludeReference();
                        composer.updateModel(new_ref, docmaSess);
                        parentNode.insertChild(insert_pos, new_ref);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Messagebox.show("Error: " + ex.getMessage());
                    }
                }
            }
        });
    }


    public void onAddSectionInclude() throws Exception
    {
        onAddInclude(true, false, false);
    }


    public void onInsertSectionInclude() throws Exception
    {
        onInsertInclude(true, false, false);
    }


    public void onAddContentInclude() throws Exception
    {
        onAddInclude(false, true, false);
    }


    public void onInsertContentInclude() throws Exception
    {
        onInsertInclude(false, true, false);
    }


    public void onAddImageInclude() throws Exception
    {
        onAddInclude(false, false, true);
    }


    public void onInsertImageInclude() throws Exception
    {
        onInsertInclude(false, false, true);
    }


    public void onAddImageFolder() throws Exception
    {
        try {
            final DocmaSession docmaSess = getDocmaSession();
            final Treeitem item = docTree.getSelectedItem();
            final DocmaNode node = (DocmaNode) item.getValue();
            if (node == null) {
                Messagebox.show("Internal Error: No object assigned to tree item!");
                return;
            }
            Window dialog = (Window) getPage().getFellow("FolderDialog");
            FolderDialogComposer composer = (FolderDialogComposer) dialog.getAttribute("$composer");
            composer.addImageFolder(node, docmaSess, new Callback() {
                public void onEvent(String eventName) 
                {
                    if (FolderDialogComposer.EVENT_OKAY.equals(eventName)) {  // OK button clicked
                        if (! item.isOpen()) item.setOpen(true);
                    }
                }
            });
        } catch (Exception ex) {
            Messagebox.show("Error: " + ex.getMessage());
        }
    }


    public void onAddSystemFolder() throws Exception
    {
        try {
            final DocmaSession docmaSess = getDocmaSession();
            final Treeitem item = docTree.getSelectedItem();
            DocmaNode node = (DocmaNode) item.getValue();
            if (node == null) {
                Messagebox.show("Internal Error: No object assigned to tree item!");
                return;
            }
            Window dialog = (Window) getPage().getFellow("FolderDialog");
            FolderDialogComposer composer = (FolderDialogComposer) dialog.getAttribute("$composer");
            composer.addSystemFolder(node, docmaSess, new Callback() {
                public void onEvent(String eventName) 
                {
                    if (FolderDialogComposer.EVENT_OKAY.equals(eventName)) {  // OK button clicked
                        if (! item.isOpen()) item.setOpen(true);
                    }
                }
            });
        } catch (Exception ex) {
            Messagebox.show("Error: " + ex.getMessage());
        }
    }


    public void onInsertImageFolder() throws Exception
    {
        try {
            DocmaSession docmaSess = getDocmaSession();
            Treeitem item = docTree.getSelectedItem();
            DocmaNode node = (DocmaNode) item.getValue();
            if (node == null) {
                Messagebox.show("Internal Error: No object assigned to tree item!");
                return;
            }
            Window dialog = (Window) getPage().getFellow("FolderDialog");
            FolderDialogComposer composer = (FolderDialogComposer) dialog.getAttribute("$composer");
            composer.insertImageFolder(node, docmaSess, new Callback() {
                public void onEvent(String eventName) 
                {
                    // if (FolderDialogComposer.EVENT_OKAY.equals(eventName)) {  // OK button clicked
                    //     if (! item.isOpen()) item.setOpen(true);
                    // }
                }
            });
        } catch (Exception ex) {
            Messagebox.show("Error: " + ex.getMessage());
        }
    }


    public void onInsertSystemFolder() throws Exception
    {
        try {
            DocmaSession docmaSess = getDocmaSession();
            Treeitem item = docTree.getSelectedItem();
            DocmaNode node = (DocmaNode) item.getValue();
            if (node == null) {
                Messagebox.show("Internal Error: No object assigned to tree item!");
                return;
            }
            Window dialog = (Window) getPage().getFellow("FolderDialog");
            FolderDialogComposer composer = (FolderDialogComposer) dialog.getAttribute("$composer");
            composer.insertSystemFolder(node, docmaSess, new Callback() {
                public void onEvent(String eventName) 
                {
                    // if (FolderDialogComposer.EVENT_OKAY.equals(eventName)) {  // OK button clicked
                    //     if (! item.isOpen()) item.setOpen(true);
                    // }
                }
            });
        } catch (Exception ex) {
            Messagebox.show("Error: " + ex.getMessage());
        }
    }


    private void doEditFolder(DocmaNode node, DocmaSession docmaSess, final Callback okAction) throws Exception
    {
        Window dialog = (Window) getPage().getFellow("FolderDialog");
        FolderDialogComposer composer = (FolderDialogComposer) dialog.getAttribute("$composer");
        composer.editFolder(node, docmaSess, new Callback() {
            public void onEvent(String eventName) 
            {
                if (FolderDialogComposer.EVENT_OKAY.equals(eventName)) {  // OK button clicked
                    if (okAction != null) {
                        okAction.onEvent(eventName);
                    }
                }
            }
        });
    }

    private synchronized UploadHandler getUploadHandler()
    {
        if (uploadHandler == null) {
            uploadHandler = new UploadHandler(this);
        }
        return uploadHandler;
    }

    public void onUploadFile() throws Exception
    {
        DocmaSession docmaSess = getDocmaSession();
        Treeitem item = docTree.getSelectedItem();
        DocmaNode node = (DocmaNode) item.getValue();
        if (node == null) {
            Messagebox.show("Internal Error: No object assigned to tree item!");
            return;
        }
        if (getUploadHandler().doUpload(node, docmaSess)) {
            if (! item.isOpen()) item.setOpen(true);
        }
    }


    public void onDownloadFile() throws Exception
    {
        if (downloadHandler == null) {
            downloadHandler = new DownloadHandler(this);
        }
        downloadHandler.downloadSelectedTreeNodes(docTree);
    }


    public void onPreviewPDF() throws Exception
    {
        previewPDFHandler.openPreviewWindow();
    }


    public void onCutNode() throws Exception
    {
        cutCopyHandler.cut(docTree);
    }


    public void onCopyNode() throws Exception
    {
        cutCopyHandler.copy(docTree);
    }


    public void onPasteHere() throws Exception
    {
        cutCopyHandler.pasteHere(docTree);
    }


    public void onPasteFilesHere() throws Exception
    {
        cutCopyHandler.pasteHere(docTree);
        refreshPreviewOnClientSide();
    }


    public void onPasteSub() throws Exception
    {
        cutCopyHandler.pasteSub(docTree);
    }


    public void onPasteFilesSub() throws Exception
    {
        cutCopyHandler.pasteSub(docTree);
        refreshPreviewOnClientSide();
    }


    public void onDeleteNode() throws Exception
    {
        cutCopyHandler.delete(docTree);
    }


    public void onDeleteFileNodes() throws Exception
    {
        cutCopyHandler.delete(docTree);
        refreshPreviewOnClientSide();
    }


    public void onClearCutCopyList() throws Exception
    {
        cutCopyHandler.clearCutCopyList();
    }


    public boolean isCutCopyListEmpty()
    {
        return cutCopyHandler.isCutCopyListEmpty();
    }


    public boolean isInCutList(DocmaNode node)
    {
        return cutCopyHandler.isInCutList(node);
    }


    public void onCompareVersions() throws Exception
    {
        compareVersionsHandler.compare();
    }


    public void onSearchAndReplace() throws Exception
    {
        DocmaSession docmaSess = getDocmaSession();
        SearchReplaceDialog dialog = getSearchReplaceDialog();
        dialog.doSearchReplace(docmaSess);
    }


    public void onFindByAliasAll() throws Exception
    {
        DocmaSession docmaSess = getDocmaSession();
        FindNodesComposer composer = getFindNodesComposer();
        composer.doFindByAlias(docmaSess);
    }

    
    public void onFindByAlias() throws Exception
    {
        DocmaNode node = getSelectedDocmaNode(true);
        if (node != null) {
            DocmaSession docmaSess = getDocmaSession();
            FindNodesComposer comp = getFindNodesComposer();
            String alias = node.getAlias();
            if (alias == null) alias = "";
            comp.doFindByAlias(docmaSess, alias);
        }
    }

    public void onFindReferencesAll() throws Exception
    {
        getFindNodesComposer().doFindReferencingAlias(getDocmaSession());
    }

    public void onFindReferences() throws Exception
    {
        DocmaNode node = getSelectedDocmaNode(true);
        if (node != null) {
            getFindNodesComposer().doFindReferencingAlias(getDocmaSession(), "", node);
        }
    }

    public void onFindReferencingThis() throws Exception
    {
        DocmaNode node = getSelectedDocmaNode(true);
        if (node != null) {
            String alias = node.getAlias();
            if ((alias == null) || alias.equals("")) {
                MessageUtil.showInfo(this, "text.node_not_referenced_no_alias");
                return;
            }
            FindNodesComposer comp = getFindNodesComposer();
            comp.doFindReferencingAlias(getDocmaSession(), alias, null);
        }
    }
    
    public void onFindStyleAll() throws Exception
    {
        getFindNodesComposer().doFindStyle(getDocmaSession(), null);
    }
    
    public void onFindStyle() throws Exception
    {
        DocmaNode node = getSelectedDocmaNode(true);
        if (node != null) {
            getFindNodesComposer().doFindStyle(getDocmaSession(), node);
        }
    }

    public void onFindXPathAll() throws Exception
    {
        getFindNodesComposer().doFindXPath(getDocmaSession(), null);
    }
    
    public void onFindXPath() throws Exception
    {
        DocmaNode node = getSelectedDocmaNode(true);
        if (node != null) {
            getFindNodesComposer().doFindXPath(getDocmaSession(), node);
        }
    }

    public void onFindApplicAll() throws Exception
    {
        getFindNodesComposer().doFindApplic(getDocmaSession(), null);
    }

    public void onFindApplic() throws Exception
    {
        DocmaNode node = getSelectedDocmaNode(true);
        if (node != null) {
            getFindNodesComposer().doFindApplic(getDocmaSession(), node);
        }
    }

    public void onChangeStylesFilter() throws Exception
    {
        guilist_styles.onChangeStylesFilter();
    }

    public void onNewStyle() throws Exception
    {
        guilist_styles.doNewStyle();
    }


    public void onEditStyle() throws Exception
    {
        guilist_styles.doEditStyle();
    }


    public void onDeleteStyle() throws Exception
    {
        guilist_styles.doDeleteStyles();
    }


    public void onCopyStyle() throws Exception
    {
        guilist_styles.doCopyStyle();
    }


    public void onVariantStyle() throws Exception
    {
        guilist_styles.doNewVariantStyle();
    }


    public void onImportStyle() throws Exception
    {
        guilist_styles.doImportStyle();
    }


    public void onExportStyle() throws Exception
    {
        guilist_styles.doExportStyle();
    }


    public void onChangeStyleHidden(ForwardEvent fe) throws Exception
    {
        // Event evt = fe.getOrigin();
        Object data = fe.getData();
        if (data == null) {
            Messagebox.show("Error: missing parameter!");
            return;
        }
        String styleId = data.toString();
        guilist_styles.changeStyleHiddenState(styleId);
    }


    public void onNewProduct() throws Exception
    {
        guilist_products.onNewProduct();
    }

    public void onEditProduct() throws Exception
    {
        guilist_products.onEditProduct();
    }

    public void onAddExternalProduct() throws Exception
    {
        guilist_products.onAddExternalProduct();
    }

    public void onChangeProductPath() throws Exception
    {
        guilist_products.onChangeProductPath();
    }

    public void onDeleteProduct() throws Exception
    {
        guilist_products.onDeleteProduct();
    }

    public void onEnableProduct() throws Exception
    {
        guilist_products.onEnableProduct();
    }

    public void onDisableProduct() throws Exception
    {
        guilist_products.onDisableProduct();
    }

    public void onCopyProduct() throws Exception
    {
        guilist_products.onCopyProduct();
    }

    public void onCreateVersion(Event evt) throws Exception
    {
        guilist_productversions.doCreateVersion(evt);
    }

    public void onReleaseVersion() throws Exception
    {
        guilist_productversions.doReleaseVersion();
    }

    public void onUnreleaseVersion() throws Exception
    {
        guilist_productversions.doUnreleaseVersion();
    }

    public void onRenameVersion() throws Exception
    {
        guilist_productversions.doRenameVersion();
    }

    public void onDeleteVersion() throws Exception
    {
        guilist_productversions.doDeleteVersion();
    }

    public void onClearRevisions(Event evt) throws Exception
    {
        guilist_productversions.doClearRevisions(evt);
    }

    public void onEditVersionComment() throws Exception
    {
        guilist_productversions.doEditVersionComment();
    }

    public void onNewPublicationConfig() throws Exception
    {
        PublicationConfigDialog dialog = (PublicationConfigDialog) getPage().getFellow("PublicationConfigDialog");
        DocmaSession docmaSess = getDocmaSession();
        DocmaPublicationConfig pubConf = new DocmaPublicationConfig("");
        dialog.setMode_New();
        if (dialog.doEdit(pubConf, docmaSess)) {
            try {
                // docmaSess.savePublicationConfig(pubConf);
                // int ins_pos = -(Collections.binarySearch(publicationconfigs_listmodel, pubConf)) - 1;
                publicationconfigs_listmodel.add(pubConf);
                publicationconfigs_listbox.setRows(publicationconfigs_listmodel.getSize());
                loadPreviewPubConfList(docmaSess);  // reload preview selection list in content tab
            } catch (Exception ex) {
                Messagebox.show("Error: " + ex.getMessage());
            }
        }
    }

    public void onEditPublicationConfig() throws Exception
    {
        PublicationConfigDialog dialog = (PublicationConfigDialog) getPage().getFellow("PublicationConfigDialog");
        DocmaSession docmaSess = getDocmaSession();
        if (publicationconfigs_listbox.getSelectedCount() <= 0) {
            Messagebox.show("Please select a publication configuration from the list!");
            return;
        }
        int sel_idx = publicationconfigs_listbox.getSelectedIndex();
        DocmaPublicationConfig pubConf = (DocmaPublicationConfig) publicationconfigs_listmodel.getElementAt(sel_idx);
        dialog.setMode_Edit();
        if (dialog.doEdit(pubConf, docmaSess)) {
            try {
                publicationconfigs_listmodel.set(sel_idx, pubConf);
                loadPreviewPubConfList(docmaSess);  // reload preview selection list in content tab
            } catch (Exception ex) {
                Messagebox.show("Error: " + ex.getMessage());
            }
        }
    }

    public void onDeletePublicationConfig() throws Exception
    {
        if (publicationconfigs_listbox.getSelectedCount() <= 0) {
            Messagebox.show("Please select a publication configuration from the list!");
            return;
        }
        int sel_idx = publicationconfigs_listbox.getSelectedIndex();
        DocmaPublicationConfig pubConf = (DocmaPublicationConfig) publicationconfigs_listmodel.getElementAt(sel_idx);
        if (Messagebox.show("Delete publication configuration '" + pubConf.getId() + "'?", "Delete?",
            Messagebox.YES | Messagebox.NO, Messagebox.QUESTION) == Messagebox.YES) {
            DocmaSession docmaSess = getDocmaSession();
            docmaSess.deletePublicationConfig(pubConf.getId());
            publicationconfigs_listmodel.remove(sel_idx);
            // if (publicationconfigs_listmodel.getSize() >= MIN_LISTBOX_SIZE) {
            //     publicationconfigs_listbox.setRows(publicationconfigs_listmodel.getSize());
            // }
            loadPreviewPubConfList(docmaSess);  // reload preview selection list in content tab
        }
    }

    public void onCopyPublicationConfig() throws Exception
    {
        if (publicationconfigs_listbox.getSelectedCount() <= 0) {
            Messagebox.show("Please select a publication configuration from the list!");
            return;
        }
        DocmaSession docmaSess = getDocmaSession();
        int sel_idx = publicationconfigs_listbox.getSelectedIndex();
        DocmaPublicationConfig pubConf = (DocmaPublicationConfig) publicationconfigs_listmodel.getElementAt(sel_idx);
        pubConf = docmaSess.getPublicationConfig(pubConf.getId()); // create copy of object
        pubConf.setId("");  // user has to enter a new ID

        PublicationConfigDialog dialog = (PublicationConfigDialog) getPage().getFellow("PublicationConfigDialog");
        dialog.setMode_New();
        if (dialog.doEdit(pubConf, docmaSess)) {
            try {
                // docmaSess.savePublicationConfig(pubConf);
                // int ins_pos = -(Collections.binarySearch(publicationconfigs_listmodel, pubConf)) - 1;
                publicationconfigs_listmodel.add(pubConf);
                publicationconfigs_listbox.setRows(publicationconfigs_listmodel.getSize());
                loadPreviewPubConfList(docmaSess);  // reload preview selection list in content tab
            } catch (Exception ex) {
                Messagebox.show("Error: " + ex.getMessage());
            }
        }
    }

    public void onNewMediaConfig() throws Exception
    {
        MediaConfigDialog dialog = (MediaConfigDialog) getPage().getFellow("MediaConfigDialog");
        DocmaSession docmaSess = getDocmaSession();
        DocmaOutputConfig outConf = new DocmaOutputConfig("");
        dialog.setMode_New();
        if (dialog.doEdit(outConf, docmaSess)) {
            try {
                // docmaSess.saveOutputConfig(outConf);
                // int ins_pos = -(Collections.binarySearch(publicationconfigs_listmodel, pubConf)) - 1;
                mediaconfigs_listmodel.add(outConf);
                // mediaconfigs_listbox.setRows(mediaconfigs_listmodel.getSize());
                loadPreviewOutputConfList(docmaSess);  // reload preview selection list in content tab
            } catch (Exception ex) {
                Messagebox.show("Error: " + ex.getMessage());
            }
        }
    }

    public void onEditMediaConfig() throws Exception
    {
        MediaConfigDialog dialog = (MediaConfigDialog) getPage().getFellow("MediaConfigDialog");
        DocmaSession docmaSess = getDocmaSession();
        if (mediaconfigs_listbox.getSelectedCount() <= 0) {
            Messagebox.show("Please select an output configuration from the list!");
            return;
        }
        int sel_idx = mediaconfigs_listbox.getSelectedIndex();
        DocmaOutputConfig outConf = (DocmaOutputConfig) mediaconfigs_listmodel.getElementAt(sel_idx);
        dialog.setMode_Edit();
        if (dialog.doEdit(outConf, docmaSess)) {
            try {
                mediaconfigs_listmodel.set(sel_idx, outConf);
                loadPreviewOutputConfList(docmaSess);  // reload preview selection list in content tab
            } catch (Exception ex) {
                Messagebox.show("Error: " + ex.getMessage());
            }
        }
    }

    public void onDeleteMediaConfig() throws Exception
    {
        if (mediaconfigs_listbox.getSelectedCount() <= 0) {
            Messagebox.show("Please select an output configuration from the list!");
            return;
        }
        int sel_idx = mediaconfigs_listbox.getSelectedIndex();
        DocmaOutputConfig outConf = (DocmaOutputConfig) mediaconfigs_listmodel.getElementAt(sel_idx);
        if (Messagebox.show("Delete output configuration '" + outConf.getId() + "'?", "Delete?",
            Messagebox.YES | Messagebox.NO, Messagebox.QUESTION) == Messagebox.YES) {
            DocmaSession docmaSess = getDocmaSession();
            docmaSess.deleteOutputConfig(outConf.getId());
            mediaconfigs_listmodel.remove(sel_idx);
            // if (mediaconfigs_listmodel.getSize() >= MIN_LISTBOX_SIZE) {
            //     mediaconfigs_listbox.setRows(mediaconfigs_listmodel.getSize());
            // }
            loadPreviewOutputConfList(docmaSess);  // reload preview selection list in content tab
        }
    }

    public void onCopyMediaConfig() throws Exception
    {
        if (mediaconfigs_listbox.getSelectedCount() <= 0) {
            Messagebox.show("Please select an output configuration from the list!");
            return;
        }
        DocmaSession docmaSess = getDocmaSession();
        int sel_idx = mediaconfigs_listbox.getSelectedIndex();
        DocmaOutputConfig outConf = (DocmaOutputConfig) mediaconfigs_listmodel.getElementAt(sel_idx);
        outConf = docmaSess.getOutputConfig(outConf.getId());  // create copy of object
        outConf.setId("");  // user has to enter a new ID

        MediaConfigDialog dialog = (MediaConfigDialog) getPage().getFellow("MediaConfigDialog");
        dialog.setMode_New();
        if (dialog.doEdit(outConf, docmaSess)) {
            try {
                // docmaSess.saveOutputConfig(outConf);
                // int ins_pos = -(Collections.binarySearch(mediaconfigs_listmodel, outConf)) - 1;
                mediaconfigs_listmodel.add(outConf);
                mediaconfigs_listbox.setRows(mediaconfigs_listmodel.getSize());
                loadPreviewOutputConfList(docmaSess);  // reload preview selection list in content tab
            } catch (Exception ex) {
                Messagebox.show("Error: " + ex.getMessage());
            }
        }
    }

    public void onAddApplic() throws Exception
    {
        NewApplicDialog dialog = (NewApplicDialog) getPage().getFellow("NewApplicDialog");
        String app_str = "";
        dialog.setApplic(app_str);

        boolean has_error = true;
        do {
            dialog.doModal();
            if (dialog.getModalResult() != GUIConstants.MODAL_OKAY) {
                return;
            }
            if (dialog.hasInvalidInputs()) {
                continue;
            }
            app_str = dialog.getApplic();
            if (declared_applics_listmodel.contains(app_str)) {
                Messagebox.show("Applicability already exists!");
                continue;
            }
            has_error = false;
        } while (has_error);
        int ins_pos = -(Collections.binarySearch(declared_applics_listmodel, app_str)) - 1;
        // if (ins_pos < 0) {
        //     declared_applics_listmodel.add(app_str);
        // } else {
            declared_applics_listmodel.add(ins_pos, app_str);
        // }
        DocmaSession docmaSess = getDocmaSession();
        docmaSess.setDeclaredApplics(declared_applics_listmodel);
    }

    public void onDeleteApplic() throws Exception
    {
        if (declared_applics_listbox.getSelectedCount() > 0) {
            int sel_idx = declared_applics_listbox.getSelectedIndex();
            String val = (String) declared_applics_listmodel.get(sel_idx);
            if (Messagebox.show("Delete applicability '" + val + "'?", "Delete?",
                Messagebox.YES | Messagebox.NO, Messagebox.QUESTION) == Messagebox.YES) {
                declared_applics_listmodel.remove(sel_idx);
                DocmaSession docmaSess = getDocmaSession();
                docmaSess.setDeclaredApplics(declared_applics_listmodel);
            }
        } else {
            Messagebox.show("Please select an applicability from the list!");
        }
    }

    public void onExportPublication() throws Exception
    {
        PublicationExportDialog dialog = (PublicationExportDialog) getPage().getFellow("PublicationExportDialog");
        DocmaSession docmaSess = getDocmaSession();
        try {
            if (dialog.doStartExport(docmaSess)) {
                loadPublicationExports();  // reload
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Messagebox.show("Error: " + ex.getMessage());
        }
    }

    public void onEditPublication() throws Exception
    {
        if (publicationexports_listbox.getSelectedCount() <= 0) {
            Messagebox.show("Please select a publication export from the list!");
            return;
        }
        int sel_idx = publicationexports_listbox.getSelectedIndex();
        DocmaPublication pub = (DocmaPublication) publicationexports_listmodel.getElementAt(sel_idx);

        PublicationExportDialog dialog = (PublicationExportDialog) getPage().getFellow("PublicationExportDialog");
        DocmaSession docmaSess = getDocmaSession();
        try {
            if (dialog.doEditExport(pub, docmaSess)) {
                publicationexports_listmodel.set(sel_idx, pub);  // update list entry
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Messagebox.show("Error: " + ex.getMessage());
        }
    }

    public void onDeletePublication() throws Exception
    {
        DocmaSession docmaSess = getDocmaSession();
        if (publicationexports_listbox.getSelectedCount() <= 0) {
            Messagebox.show("Please select a publication export from the list!");
            return;
        }
        enablePublicationExportListRefresh(false);

        int sel_cnt = publicationexports_listbox.getSelectedCount();
        Iterator it = publicationexports_listbox.getSelectedItems().iterator();
        DocmaPublication[] sel_pubs = new DocmaPublication[sel_cnt];
        for (int i=0; i < sel_cnt; i++) {
            Listitem item = (Listitem) it.next();
            sel_pubs[i] = (DocmaPublication) publicationexports_listmodel.getElementAt(item.getIndex());
        }
        String msg;
        if (sel_cnt == 1) {
            msg = "Delete publication export '" + sel_pubs[0].getFilename() + "'?";
        } else {
            msg = "Delete " + sel_cnt + " publication exports?";
        }
        if (Messagebox.show(msg, "Delete?",
            Messagebox.YES | Messagebox.NO, Messagebox.QUESTION) == Messagebox.YES) {
            for (int i=0; i < sel_cnt; i++) {
                DocmaPublication pub = sel_pubs[i];
                if (! pub.isExportFinished()) {
                    Messagebox.show("Cannot delete running export: " + pub.getFilename());
                    continue;
                }
                if (pub.isOnline()) {
                    Messagebox.show("Cannot delete publication: " + pub.getFilename() +
                                    "\n Remove publication from online directory first.");
                    continue;
                }
                docmaSess.deletePublication(pub.getId());
                // publicationexports_listmodel.remove(sel_idx);
            }
        }
        loadPublicationExports();  // reload
    }

    public void onDownloadPublicationClick(ForwardEvent fe) throws Exception
    {
        // Event evt = fe.getOrigin();
        Object data = fe.getData();
        if (data == null) {
            Messagebox.show("Error: missing parameter!");
            return;
        }
        String publicationId = data.toString();
        DocmaSession docmaSess = getDocmaSession();
        DocmaPublication pub = docmaSess.getPublication(publicationId);
        String filename = pub.getFilename();
        if ((filename == null) || filename.trim().equals("")) {
            filename = pub.getId();
        }
        InputStream pub_in = pub.getContentStream();
        Filedownload.save(pub_in, "application/octet-stream", filename);
    }

    public void onDownloadPublicationLogClick(ForwardEvent fe) throws Exception
    {
        // Event evt = fe.getOrigin();
        Object data = fe.getData();
        if (data == null) {
            Messagebox.show("Error: missing parameter!");
            return;
        }
        String publicationId = data.toString();
        DocmaSession docmaSess = getDocmaSession();
        DocmaPublication pub = docmaSess.getPublication(publicationId);
        String filename = pub.getFilename();
        if ((filename == null) || filename.trim().equals("")) {
            filename = pub.getId();
        }
        int p = filename.lastIndexOf('.');
        if (p >= 0) {
            filename = filename.substring(0, p);  // remove extension
        }
        filename += "_log.html";

        // DocmaExportLog exp_log = pub.getExportLog();
        // String html_log = exp_log.toHTMLString();
        // Filedownload.save(html_log, "text/html", filename);

        Desktop desk = getDesktop();
        String open_url = desk.getExecution().encodeURL("servmedia/" + docmaSess.getSessionId() +
                          "/" + docmaSess.getStoreId() + "/" + docmaSess.getVersionId() +
                          "/" + docmaSess.getLanguageCode() + "/publicationlog/" + publicationId +
                          "/" + filename);
        String client_action = "window.open('" + open_url +
          "', '_blank" +
          "', 'width=620,height=460,left=100,top=100,resizable=yes,location=no,menubar=yes,scrollbars=yes');";
        Clients.evalJavaScript(client_action);
    }

    public void onOpenPublicationClick(ForwardEvent fe) throws Exception
    {
        // Event evt = fe.getOrigin();
        Object data = fe.getData();
        if (data == null) {
            Messagebox.show("Error: missing parameter!");
            return;
        }
        String publicationId = data.toString();
        DocmaSession docmaSess = getDocmaSession();
        DocmaPublication pub = docmaSess.getPublication(publicationId);
        String filename = pub.getFilename();
        int pos = filename.lastIndexOf('.');
        String ext = "";
        if (pos > 0) {
            ext = filename.substring(pos);
        }
        String format = pub.getFormat();
        if (ext.equalsIgnoreCase(".zip")) {
            if (format.equalsIgnoreCase("docbook")) {
                filename = "docbook.xml";
            } else
            if (format.equalsIgnoreCase("html")) {
                filename = "index.html";
            } else 
            if (format.equalsIgnoreCase("pdf")) {
                filename = filename.substring(0, pos) + ".pdf";  // change .zip to .pdf
            } else {
                filename = "";
            }
        }
        String online_url = docmaSess.getApplicationProperty(DocmaConstants.PROP_PUBLICATION_ONLINE_URL);
        if (online_url == null) online_url = "";
        if (online_url.equals("")) {
            String relpath = docmaSess.getApplicationProperty(DocmaConstants.PROP_PUBLICATION_ONLINE_RELATIVE_PATH);
            boolean is_rel = (relpath != null) && (relpath.length() > 0);
            if (! is_rel) {
                Messagebox.show("An online URL has to be configured in the application settings!");
                return;
            }
            online_url = relpath;
        }
        if (! online_url.endsWith("/")) online_url += "/";
        String open_url = online_url + docmaSess.getStoreId() + "/" +
                          docmaSess.getVersionId() + "/" + pub.getId() + "/" + filename;
        DocmaWebSession webSess = GUIUtil.getDocmaWebSession(this);
        int deskWidth = webSess.getDesktopWidth();
        int deskHeight = webSess.getDesktopHeight();
        int winWidth = (deskWidth >= 900) ? 800 : 620;
        int winHeight = (deskHeight >= 700) ? 600 : 460;
        String client_action = "window.open('" + open_url +
          "', '_blank', 'width=" + winWidth + ",height=" + winHeight + 
          ",left=100,top=100,resizable=yes,location=yes,menubar=yes,scrollbars=yes');";
        Clients.evalJavaScript(client_action);
    }

    public void onChangePublicationIsOnline(ForwardEvent fe) throws Exception
    {
        // Event evt = fe.getOrigin();
        Object data = fe.getData();
        if (data == null) {
            Messagebox.show("Error: missing parameter!");
            return;
        }
        String publicationId = data.toString();
        DocmaSession docmaSess = getDocmaSession();
        DocmaPublication pub = docmaSess.getPublication(publicationId);

        boolean is_online = pub.isOnline();
        String fn = pub.getFilename();
        String msg_title;
        String msg_confirm;
        String msg_busy;
        String evt_name;
        if (is_online) {
            msg_title = "Disable online access?";
            msg_confirm = "Remove publication '" + fn + "' from Web-Server directory?";
            msg_busy = "Removing publication from online directory ... please wait!";
            evt_name = "onRemovePublicationOnline";
        } else {
            msg_title = "Enable online access?";
            msg_confirm = "Add publication '" + fn + "' to Web-Server directory?";
            msg_busy = "Adding publication to online directory ... please wait!";
            evt_name = "onSetPublicationOnline";
        }

        if (Messagebox.show(msg_confirm, msg_title,
            Messagebox.YES | Messagebox.NO, Messagebox.QUESTION) == Messagebox.YES) {
            Clients.showBusy(msg_busy);
            try {
                Events.echoEvent(evt_name, this, publicationId);
            } catch (Exception ex) {
                Clients.clearBusy();
            }
        } else {
            loadPublicationExports();  // reload publication list to update checkbox
        }
    }

    public void onSetPublicationOnline(Event evt) throws Exception
    {
        setPublicationOnline(evt, true);
    }

    public void onRemovePublicationOnline(Event evt) throws Exception
    {
        setPublicationOnline(evt, false);
    }

    private void setPublicationOnline(Event evt, boolean set_online) throws Exception
    {
        try {
            String publicationId = evt.getData().toString();
            DocmaSession docmaSess = getDocmaSession();
            DocmaPublication pub = docmaSess.getPublication(publicationId);
            pub.setOnline(set_online);
            Clients.clearBusy();
        } catch (Throwable ex) {
            ex.printStackTrace();
            Clients.clearBusy();
            Messagebox.show("Error: " + ex.getMessage());
        }
        loadPublicationExports();  // reload publication list to update checkbox
                                   // in case an error occured
    }

    public void onRefreshPublicationExportList() throws Exception
    {
        loadPublicationExports();  // reload
    }

    public void onShowPreviewLog() throws Exception
    {
        Desktop desk = getDesktop();
        String url = desk.getExecution().encodeURL("previewLog.jsp?desk=" + desk.getId() +
                          "&stamp=" + System.currentTimeMillis());
        String client_action = "window.open('" + url +
          "', '_blank', 'width=400,height=400,left=50,top=50" +
          ",resizable=yes,location=no,menubar=yes,toolbar=yes,scrollbars=yes');";
        Clients.evalJavaScript(client_action);
    }

    public void onInsertEntity() throws Exception
    {
        guilist_char_entities.doInsertEntity();
    }

    public void onEditEntity() throws Exception
    {
        guilist_char_entities.doEditEntity();
    }

    public void onDeleteEntity() throws Exception
    {
        guilist_char_entities.doDeleteEntity();
    }

    public void onCutEntity() throws Exception
    {
        guilist_char_entities.doCutEntity();
    }

    public void onPasteEntity() throws Exception
    {
        guilist_char_entities.doPasteEntity();
    }

    public void onShowConnectedUsers() throws Exception
    {
        DocmaApplication docmaApp = GUIUtil.getDocmaWebApplication(this);
        DocmaSession docmaSess = getDocmaSession();
        ConnectedUsersDialog dialog = (ConnectedUsersDialog) getPage().getFellow("ConnectedUsersDialog");
        dialog.showConnectedUsers(docmaApp, docmaSess, docmaSess.getStoreId(), null);
    }

    public void onShowExportQueue() throws Exception
    {
        DocmaSession docmaSess = getDocmaSession();
        Window dialog = (Window) getPage().getFellow("exportQueueDialog");
        ExportQueueComposer composer = (ExportQueueComposer) dialog.getAttribute("$composer");
        composer.showExportQueue(docmaSess);
    }

    public void onNewUserGroup() throws Exception
    {
        guilist_usersgroups.onNewUserGroup();
    }

    public void onEditUserGroup() throws Exception
    {
        guilist_usersgroups.onEditUserGroup();
    }

    public void onDeleteUserGroup() throws Exception
    {
        guilist_usersgroups.onDeleteUserGroup();
    }

    public void onCopyUserGroup() throws Exception
    {
        guilist_usersgroups.onCopyUserGroup();
    }

    public void onEditRights() throws Exception
    {
        guilist_usersgroups.onEditRights();
    }

    public void onAddMember() throws Exception
    {
        guilist_usersgroups.onAddMember();
    }

    public void onRemoveMember() throws Exception
    {
        guilist_usersgroups.onRemoveMember();
    }

    public void onEditMember() throws Exception
    {
        guilist_usersgroups.onEditMember();
    }

    public void onNewUser() throws Exception
    {
        guilist_usersgroups.onNewUser();
    }

    public void onEditUser() throws Exception
    {
        guilist_usersgroups.onEditUser();
    }

    public void onEditUserProfile() throws Exception
    {
        guilist_usersgroups.onEditUserProfile();
    }

    public void onDeleteUser() throws Exception
    {
        guilist_usersgroups.onDeleteUser();
    }


    public void onAddAutoFormatClass() throws Exception
    {
        guilist_autoformatconfig.onAddAutoFormatClass();
    }


    public void onRemoveAutoFormatClass() throws Exception
    {
        guilist_autoformatconfig.onRemoveAutoFormatClass();
    }


    public void onConsistencyCheck() throws Exception
    {
        getConsistencyCheckComposer().showDialog();
    }


    public void onSaveApplicationSettings() throws Exception
    {
        DocmaSession docmaSess = getDocmaSession();

        List<String> old_txt_exts = Arrays.asList(docmaSess.getTextFileExtensions());

        boolean is_rel = pubonline_relpath_radio.isSelected();
        // boolean is_abs = ! is_rel;
        String pubonline_path = pubonlinedir_textbox.getValue();
        File pubonline_dir = new File(pubonline_path);
        if (is_rel && pubonline_dir.isAbsolute()) {
            is_rel = false;
            pubonline_abspath_radio.setSelected(true);
        }
        Integer max_revisions = max_revisions_intbox.getValue();
        Integer max_preview = max_preview_html_intbox.getValue();
        Integer max_print = max_preview_print_intbox.getValue();
        Listitem sel_theme = app_themes_listbox.getSelectedItem();
        Object themeId = (sel_theme != null) ? sel_theme.getValue() : null;

        String[] names = new String[9];
        String[] values = new String[9];

        names[0] = is_rel ? DocmaConstants.PROP_PUBLICATION_ONLINE_RELATIVE_PATH
                          : DocmaConstants.PROP_PUBLICATION_ONLINE_PATH;
        values[0] = pubonline_path;

        names[1] = DocmaConstants.PROP_PUBLICATION_ONLINE_URL;
        values[1] = pubonlineurl_textbox.getValue().trim();

        names[2] = DocmaConstants.PROP_MAX_REVISIONS_PER_USER;
        values[2] = (max_revisions == null) ? "" : max_revisions.toString();

        names[3] = DocmaConstants.PROP_TEXT_FILE_EXTENSIONS;
        values[3] = txt_extensions_textbox.getValue().trim();
        
        names[4] = GUIConstants.PROP_APP_THEME;
        values[4] = (themeId != null) ? themeId.toString() : "";

        names[5] = GUIConstants.PROP_APP_THEME_VERSION_RELATION;
        values[5] = DocmaConstants.DISPLAY_APP_VERSION;
        
        names[6] = DocmaConstants.PROP_FOP_CUSTOM_CONFIG_FILE;
        values[6] = fop_config_textbox.getValue().trim();

        names[7] = DocmaConstants.PROP_PREVIEW_MAX_NODES;
        values[7] = (max_preview == null) ? "0" : max_preview.toString();

        names[8] = DocmaConstants.PROP_PREVIEW_MAX_NODES_PRINT;
        values[8] = (max_print == null) ? "0" : max_print.toString();

        docmaSess.setApplicationProperties(names, values);

        // Initialize editor and viewer assignments for new text file extensions.
        boolean ext_added = false;
        DocmaWebApplication webApp = GUIUtil.getDocmaWebApplication(this);
        ExtAssignments editAssigns = webApp.getEditorAssignments();
        ExtAssignments viewAssigns = webApp.getViewerAssignments();
        for (String ext : docmaSess.getTextFileExtensions()) {
            if (! old_txt_exts.contains(ext)) {
                initExtAssignment(editAssigns, ext);
                initExtAssignment(viewAssigns, ext);
                ext_added = true;
            }
        }
        if (ext_added) {
            webApp.saveAssignments();
            guilist_edit_extensions.markRefresh();  // make sure the extension list is refreshed
            guilist_view_extensions.markRefresh();  // make sure the extension list is refreshed
        }
        
        Messagebox.show("Settings saved. \n" + 
                        "Note: For some changes to take effect, \n" +
                        "a restart of the Web-Server is required!");
    }
    
    private void initExtAssignment(ExtAssignments assigns, String ext)
    {
        String appid = assigns.getAssignedApplication(ext);
        if (appid == null) {  // No assignment or default application
            appid = assigns.getApplicationForExtension(ext);
            // Call setAssignment(...) to be sure an assignment entry exists.
            // Otherwise the extension may not be listed by the method 
            // assigns.listAssignments().
            assigns.setAssignment(ext, appid);  // appid may be null; null means default application
        }
    }

    public void onSaveBaseDir() throws Exception
    {
        DocmaWebApplication webApp = GUIUtil.getDocmaWebApplication(this);
        DocmaSession docmaSess = getDocmaSession();

        String old_base = docmaSess.getApplicationProperty(DocmaConstants.PROP_BASE_PATH);
        String new_base = docstoredir_textbox.getValue().trim();
        if (! new_base.equals(old_base)) {
            String webapp_root = webApp.getWebAppDirectory();
            PropertiesLoader propLoader = AppConfigurator.getWebApplicationConfigLoader(webapp_root);
            propLoader.setProp(AppConfigurator.PROP_DOCSTORE_DIR, new_base);
            propLoader.savePropFile("Docmenta configuration properties");
        }
        Messagebox.show("Document-store path has been saved.\n " +
                        "Please move the document-store directory to the " +
                        "new location and restart the Web-Server!");
    }
    
    public void onResetAppSettingsPanel()
    {
        updateAppSettingsPanel();
        ((Button) getFellow("SettingsResetBtn")).setDisabled(true);
        ((Button) getFellow("SettingsResetOnlineDirBtn")).setDisabled(true);
        ((Button) getFellow("SettingsResetDocStoreDirBtn")).setDisabled(true);
    }

    public void onChangePublicationOnlineDir() throws Exception
    {
        DocmaWebApplication webApp = GUIUtil.getDocmaWebApplication(this);

        boolean is_rel = pubonline_relpath_radio.isSelected();
        boolean is_abs = ! is_rel;

        String pubonline_path = pubonlinedir_textbox.getValue();
        File pubonline_file = new File(pubonline_path);
        String webapp_path = webApp.getWebAppDirectory();

        if (is_rel && pubonline_file.isAbsolute() && pubonline_path.startsWith(webapp_path)) {
            String newpath = pubonline_path.substring(webapp_path.length());
            if (newpath.startsWith(File.separator)) newpath = newpath.substring(1);
            pubonlinedir_textbox.setValue(newpath);
        }
        if (is_abs && !pubonline_file.isAbsolute()) {
            String newpath = new File(webapp_path, pubonline_path).getAbsolutePath();
            pubonlinedir_textbox.setValue(newpath);
        }
    }

    public void onSortByFilename() throws Exception
    {
        Treeitem item = docTree.getSelectedItem();
        if (item == null) {
            Messagebox.show("No node selected!");
            return;
        }
        DocmaNode node = (DocmaNode) item.getValue();
        node.sortByFilename();
    }

    public void onExportNodes() throws Exception
    {
        importExportHandler.exportNodes(docTree);
    }

    public void onImportSubNodes() throws Exception
    {
        importExportHandler.importSubNodes(docTree);
    }

//    public void onRegisterApplication() throws Exception
//    {
//        RegistrationHandler regHandler = new RegistrationHandler(this);
//        if (regHandler.showRegisterDialog()) {
//            updateAppSettingsPanel();
//        }
//    }

    public void onAbout() throws Exception
    {
        AboutDialog dialog = (AboutDialog) getPage().getFellow("AboutDialog");
        dialog.showAbout(this);
    }
    
    public void onOpenPluginConfig(Event evt) 
    {
        guilist_plugins.openPluginConfigDialog(evt);
    }
    
    public void onOpenPluginHelp(Event evt) 
    {
        guilist_plugins.openPluginHelp(evt);
    }
    
    public void onOpenPluginLicense(Event evt) 
    {
        guilist_plugins.openPluginLicense(evt);
    }
    
    public void onCheckTempDirAccess()
    {
        String path = System.getProperty("java.io.tmpdir");
        if ((path == null) || (path.length() == 0)) {
            Messagebox.show("Error: Path is empty!", "Error", 0, Messagebox.ERROR);
            return;
        }
        File tmpdir = new File(path);
        if (DocmaUtil.checkDirReadWriteDeleteAccess(tmpdir)) {
            Messagebox.show("Access is allowed!");
        } else {
            Messagebox.show("Access failed! You have to change the access rights, \n" +
                            "to allow the web-server full access to this directory!", 
                            "Warning", 0, Messagebox.EXCLAMATION);
        }
    }

}
