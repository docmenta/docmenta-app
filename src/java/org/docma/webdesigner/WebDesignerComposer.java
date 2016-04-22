/*
 * WebhelpDesignerComposer.java 
 *
 *  Copyright (C) 2015  Manfred Paula, http://www.docmenta.org
 *   
 *  This file is part of Docmenta. Docmenta is free software: you can 
 *  redistribute it and/or modify it under the terms of the GNU Lesser 
 *  General Public License as published by the Free Software Foundation, 
 *  either version 3 of the License, or (at your option) any later version.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Docmenta.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.docma.webdesigner;

import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.*;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.*;
import org.zkoss.zul.*;
import org.zkoss.zkex.zul.Colorbox;
import org.zkoss.image.AImage;
import org.zkoss.zk.ui.util.Clients;

import java.io.*;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.zip.*;
import javax.servlet.ServletContext;

import org.docma.app.DocmaConstants;
import org.docma.app.AccessRights;
import org.docma.webapp.DocmaWebApplication;
import org.docma.webapp.GUIUtil;
import org.docma.webapp.MainWindow;
import org.docma.userapi.UserManager;
import org.docma.util.DocmaUtil;

/**
 *
 * @author manfred
 */
public class WebDesignerComposer extends SelectorComposer<Component>
{
    private static final String DESIGNER_URL_PATH = "webdesigner";
    private static final String PREVIEW_PATH = "preview";
    private static final String EXAMPLE_PATH_PARTS = "parts";
    private static final String EXAMPLE_PATH_NOPARTS = "noparts";
    private static final String TEMP_UPLOAD_PATH = "temp_upload";
    
    private static final String CONTENT_TO_COMMON_URL_PATH = "common/";
    
    private static final String REGEXP_WHD_ID = "[A-Za-z][0-9A-Za-z_-]+";
    private static final int    MAXLENGTH_WHD_ID = 20;
    private static final Set<String> WHD_MEDIA_EXTENSIONS = new HashSet<String>(
            Arrays.asList(new String[] {"gif", "ico", "jpg", "jpeg", "png"}));
    
    private static final String FILENAME_DESIGNER_PROPS = "webdesigner.properties";

    // File prefixes of all WebHelp-design image files
    private static final String PREFIX_FAVICON                 = "favicon";  // cannot be changed
    private static final String PREFIX_LOGO1                   = "logo1";    // cannot be changed
    private static final String PREFIX_LOGO2                   = "logo2";    // cannot be changed
    private static final String PREFIX_TREEVIEW_CONTENT_ICON   = "file";     // cannot be changed
    private static final String PREFIX_TREEVIEW_ICONS          = "treeview-icons";
    private static final String PREFIX_TREEVIEW_LINE           = "treeview-line";   
    private static final String PREFIX_SHOW_HIDE_TOC_BUTTON    = "showHideToc";
    private static final String PREFIX_HEAD_BG                 = "headbg";
    private static final String PREFIX_NAV_BG                  = "navbg";
    private static final String PREFIX_NAV_ACCORDION_BG        = "navbg-accordion";
    private static final String PREFIX_NAV_INNER_BG            = "navbg-inner";
    private static final String PREFIX_PART_TAB_BG             = "part-tab";
    private static final String PREFIX_PART_TAB_CT_BG          = "part-tab-ct";
    private static final String PREFIX_PART_TAB_HL_BG          = "part-tab-hl";
    private static final String PREFIX_LOCAL_NAV_BG            = "localnav-bg";
    private static final String PREFIX_LOCAL_NAV_PREV          = "prev";
    private static final String PREFIX_LOCAL_NAV_NEXT          = "next";
    private static final String PREFIX_LOCAL_NAV_UP            = "up";
    private static final String PREFIX_LOCAL_NAV_HOME          = "home";
    private static final String PREFIX_LOCAL_NAV_TOC           = "toc";
    private static final String PREFIX_BREAD_BG                = "bread-bg";
    private static final String PREFIX_BREAD_SEP               = "bread-sep";
    private static final String PREFIX_SEARCH_BG               = "searchbg";
    private static final String PREFIX_SEARCH_INNER_BG         = "searchbg-inner";
    private static final String PREFIX_SEARCH_INPUT_BG         = "searchbg-input";
    private static final String PREFIX_SEARCH_BUTTON           = "searchbtn";
    private static final String PREFIX_SEARCH_CLOSE_ICON       = "close-search";
    private static final String PREFIX_SEARCH_HIGHLIGHT_BUTTON = "highlight";
    private static final String PREFIX_SEARCH_TAB_BG           = "search-tab";
    private static final String PREFIX_SEARCH_TAB_CT_BG        = "search-tab-ct";
    private static final String PREFIX_SEARCH_TAB_HL_BG        = "search-tab-hl";
    private static final String PREFIX_SEARCH_PANEL_BG         = "tabpanel-bg";

    // Following filenames are fixed and cannot be changed (i.e. the filenames
    // are hard-coded in the generated web-help HTML code).
    private static final String DEPLOY_FILENAME_FAVICON = PREFIX_FAVICON + ".ico";
    private static final String DEPLOY_FILENAME_LOGO1 = PREFIX_LOGO1 + ".png";
    private static final String DEPLOY_FILENAME_LOGO2 = PREFIX_LOGO2 + ".png";
    private static final String DEPLOY_FILENAME_TREE_CONTENT_ICON = PREFIX_TREEVIEW_CONTENT_ICON + ".gif";
    private static final String DEPLOY_FILENAME_PREV = PREFIX_LOCAL_NAV_PREV + ".png";
    private static final String DEPLOY_FILENAME_NEXT = PREFIX_LOCAL_NAV_NEXT + ".png";
    private static final String DEPLOY_FILENAME_UP = PREFIX_LOCAL_NAV_UP + ".png";
    private static final String DEPLOY_FILENAME_HOME = PREFIX_LOCAL_NAV_HOME + ".png";
    private static final String DEPLOY_FILENAME_TOC = PREFIX_LOCAL_NAV_TOC + ".png";
    // For following images the saved filename differs from the deployed filename.
    // See methods savedName() and deployedName()
    private static final String DEPLOY_FILENAME_TREE_ICONS = "treeview-default.gif";
    private static final String DEPLOY_FILENAME_TREE_LINE = "treeview-default-line.gif";
    private static final String DEPLOY_FILENAME_SEARCH_HIGHLIGHT_BUTTON = "highlight-blue.gif";

    private static final String SAVED_FILENAME_TREE_ICONS = PREFIX_TREEVIEW_ICONS + ".gif";
    private static final String SAVED_FILENAME_TREE_LINE  = PREFIX_TREEVIEW_LINE + ".gif";
    
    @Wire Window       whdMainWin;
    @Wire Borderlayout whdMainLayout;
    @Wire Iframe       whdPreviewFrame;
    @Wire Listbox      whdSelectList;
    @Wire Component    whdPreviewControls;
    @Wire Checkbox     whdPreviewPartsCheckbox;
    @Wire Checkbox     whdPreviewCrumbsCheckbox;
    @Wire Timer        whdPreviewTimer;
    @Wire Component    whdLayoutVersionArea;
    @Wire Button       whdSaveBtn;
    @Wire Label        whdDesignNameLabel;
    @Wire Tabbox       whdControlsTabBox;
    
    // @Wire Groupbox     whdGeneralGroup;
    @Wire Listbox      whdLayoutIdBox;
    @Wire Listbox      whdLayoutVersionBox;
    @Wire Image        whdFaviconImage;
    @Wire Colorbox     whdGeneralBgColorBox;
    @Wire Spinner      whdContentTopBox;
    @Wire Spinner      whdContentPaddingLeftBox;
    @Wire Spinner      whdContentPaddingRightBox;
    @Wire Spinner      whdContentWidthMinBox;
    @Wire Spinner      whdContentWidthMaxBox;
    @Wire Spinner      whdWatermarkXPosBox;
    @Wire Spinner      whdWatermarkYPosBox;
    
    // @Wire Groupbox     whdHeadingGroup;
    @Wire Div          whdHeaderBgPrev;
    @Wire Spinner      whdHeaderHeightBox;
    @Wire Spinner      whdHeaderPaddingTopBox;
    @Wire Spinner      whdHeaderPaddingBottomBox;
    @Wire Spinner      whdHeaderPaddingLeftBox;
    @Wire Spinner      whdHeaderPaddingRightBox;
    @Wire Checkbox     whdHeaderTitle1Checkbox;
    @Wire Checkbox     whdHeaderTitle2Checkbox;
    @Wire Label        whdHeaderTitle1FontPrev;
    @Wire Label        whdHeaderTitle2FontPrev;
    @Wire Listbox      whdHeaderTitleAlignBox;
    @Wire Spinner      whdHeaderTitleMarginTopBox;
    @Wire Spinner      whdHeaderTitleMarginBottomBox;
    @Wire Spinner      whdHeaderTitleMarginLeftBox;
    @Wire Spinner      whdHeaderTitleMarginRightBox;
    
    @Wire Div          whdLogo1Area;
    @Wire Checkbox     whdLogo1Checkbox;
    @Wire Button       whdLogo1DownloadBtn;
    @Wire Button       whdLogo1DeleteBtn;
    @Wire Image        whdLogo1Image;
    @Wire Listbox      whdLogo1PositionBox;
    @Wire Spinner      whdLogo1WidthBox;
    @Wire Spinner      whdLogo1HeightBox;
    @Wire Spinner      whdLogo1XPosBox;
    @Wire Spinner      whdLogo1YPosBox;
    @Wire Div          whdLogo2Area;
    @Wire Checkbox     whdLogo2Checkbox;
    @Wire Button       whdLogo2DownloadBtn;
    @Wire Button       whdLogo2DeleteBtn;
    @Wire Image        whdLogo2Image;
    @Wire Listbox      whdLogo2PositionBox;
    @Wire Spinner      whdLogo2WidthBox;
    @Wire Spinner      whdLogo2HeightBox;
    @Wire Spinner      whdLogo2XPosBox;
    @Wire Spinner      whdLogo2YPosBox;

    // @Wire Groupbox     whdNavigationGroup;
    @Wire Spinner      whdNavigationWidthBox;
    @Wire Spinner      whdNavigationMarginTopBox;
    @Wire Spinner      whdNavigationMarginBottomBox;
    @Wire Spinner      whdNavigationPaddingTopBox;
    @Wire Spinner      whdNavigationPaddingBottomBox;
    @Wire Spinner      whdNavigationPaddingLeftBox;
    @Wire Spinner      whdNavigationPaddingRightBox;
    @Wire Div          whdNavigationBgPrev;
    @Wire Div          whdNavigationTreeBgPrev;
    @Wire Checkbox     whdNavTreeShowTitleBox;
    @Wire Checkbox     whdNavTreeCollapsedBox;
    @Wire Checkbox     whdNavTreeAutoCloseBox;
    @Wire Listbox      whdNavTreeAnimationBox;
    @Wire Label        whdNavigationFontPrev;
    @Wire Label        whdNavigationFontHoverPrev;
    @Wire Label        whdNavigationFontCurrentPrev;
    @Wire Label        whdNavigationFontTitlePrev;
    @Wire Image        whdNavTreeContentImage;
    @Wire Spinner      whdNavTreeContentImgWidthBox;
    @Wire Button       whdNavTreeContentImgDownloadBtn;
    @Wire Button       whdNavTreeContentImgDeleteBtn;
    @Wire Image        whdNavTreeOpenCloseImage;
    @Wire Image        whdNavTreeLineImage;
    @Wire Image        whdNavigationToggleImage;
    @Wire Spinner      whdNavigationToggleWidthBox;
    @Wire Button       whdNavigationToggleDownloadBtn;
    @Wire Button       whdNavigationToggleDeleteBtn;
    @Wire Checkbox     whdNavResizeCheckbox;
    
    @Wire Checkbox     whdNavAccordionCheckbox;
    @Wire Button       whdNavAccordionBgBtn;
    @Wire Button       whdNavAccordionFontBtn;
    @Wire Div          whdNavAccordionBgPrev;
    @Wire Label        whdNavAccordionFontPrev;
    @Wire Spinner      whdNavAccordionPaddingXBox;
    @Wire Spinner      whdNavAccordionHeightBox;
    @Wire Spinner      whdNavAccordionSpacingBox;
    
    @Wire Spinner  whdPartTabsMarginTopBox;
    @Wire Spinner  whdPartTabsMarginLeftBox;
    @Wire Spinner  whdPartTabHeightBox;
    @Wire Spinner  whdPartTabSpacingBox;
    @Wire Checkbox whdPartTabCurrentBox;
    @Wire Checkbox whdPartTabHoverBox;
    @Wire Div      whdPartTabBgPrev;
    @Wire Div      whdPartTabBgCurrentPrev;
    @Wire Div      whdPartTabBgHoverPrev;
    @Wire Label    whdPartTabFontPrev;
    @Wire Label    whdPartTabFontCurrentPrev;
    @Wire Label    whdPartTabFontHoverPrev;

    @Wire Spinner   whdNavLocalHeightBox;
    @Wire Spinner   whdNavLocalMarginTopBox;
    @Wire Spinner   whdNavLocalPaddingTopBox;
    @Wire Spinner   whdNavLocalPaddingBottomBox;
    @Wire Spinner   whdNavLocalPaddingLeftBox;
    @Wire Spinner   whdNavLocalPaddingRightBox;
    @Wire Div       whdNavLocalBgPrev;
    @Wire Label     whdNavLocalFontPrev;
    @Wire Label     whdNavLocalFontHoverPrev;
    @Wire Checkbox  whdNavSeparatorEnabledBox;
    @Wire Label     whdNavSeparatorFontPrev;
    @Wire Checkbox  whdNavLocalShowTextBox;
    @Wire Checkbox  whdNavLocalShowIconsBox;
    @Wire Button    whdNavLocalIconDownloadBtn;
    @Wire Image     whdNavLocalPrevImage;
    @Wire Image     whdNavLocalUpImage;
    @Wire Image     whdNavLocalNextImage;
    @Wire Image     whdNavLocalHomeImage;
    @Wire Image     whdNavLocalTocImage;
    @Wire Listbox   whdNavLocalIconSelectBox;
    @Wire Spinner   whdNavLocalIconsHeightBox;

    @Wire Spinner   whdBreadcrumbsMarginTopBox;
    @Wire Spinner   whdBreadcrumbsMarginBottomBox;
    @Wire Spinner   whdBreadcrumbsMarginLeftBox;
    @Wire Spinner   whdBreadcrumbsMarginRightBox;
    @Wire Spinner   whdBreadcrumbsPaddingTopBox;
    @Wire Spinner   whdBreadcrumbsPaddingBottomBox;
    @Wire Spinner   whdBreadcrumbsPaddingLeftBox;
    @Wire Spinner   whdBreadcrumbsPaddingRightBox;
    @Wire Div       whdBreadcrumbsBgPrev;
    @Wire Label     whdBreadcrumbsFontPrev;
    @Wire Label     whdBreadcrumbsFontLastPrev;
    @Wire Label     whdBreadcrumbsFontHoverPrev;
    @Wire Label     whdBreadcrumbsFontSepPrev;
    @Wire Image     whdBreadcrumbsSeparatorImage;
    @Wire Spinner   whdBreadcrumbsSepImageWidthBox;
    @Wire Button    whdBreadcrumbsSepImageDownloadBtn;
    @Wire Button    whdBreadcrumbsSepImageDeleteBtn;

    @Wire Listbox   whdSearchUIModeBox;
    @Wire Listbox   whdSearchPositionBox;
    @Wire Spinner   whdSearchWidthBox;
    @Wire Spinner   whdSearchHeightBox;
    @Wire Component whdSearchXYPosArea;
    @Wire Spinner   whdSearchXPosBox;
    @Wire Spinner   whdSearchYPosBox;
    @Wire Spinner   whdSearchInnerTopOffsetBox;
    @Wire Spinner   whdSearchInnerLeftOffsetBox;
    @Wire Spinner   whdSearchInnerRightOffsetBox;
    @Wire Div       whdSearchBgPrev;
    @Wire Div       whdSearchInnerBgPrev;
    @Wire Div       whdSearchInputBgPrev;
    @Wire Checkbox  whdSearchBtnShowTextBox;
    @Wire Div       whdSearchBtnBgPrev;
    @Wire Label     whdSearchBtnFontPrev;
    @Wire Spinner   whdSearchBtnWidthBox;
    @Wire Spinner   whdSearchBtnHeightBox;
    @Wire Label     whdSearchLegendFontPrev;
    @Wire Label     whdSearchInputFontPrev;
    @Wire Label     whdSearchTitleFontPrev;
    @Wire Label     whdSearchExpressionFontPrev;
    @Wire Label     whdSearchResultFontPrev;
    @Wire Label     whdSearchHitFontPrev;
    @Wire Label     whdSearchCloseFontPrev;
    @Wire Image     whdSearchToggleImage;
    @Wire Spinner   whdSearchToggleWidthBox;
    @Wire Image     whdSearchCloseImage;
    
    @Wire Tab       whdSearchTabArea;
    @Wire Spinner   whdSearchTabHeightBox;
    @Wire Div       whdSearchTabBgPrev;
    @Wire Div       whdSearchTabBgActivePrev;
    @Wire Checkbox  whdSearchTabHoverBox;
    @Wire Div       whdSearchTabBgHoverPrev;
    @Wire Label     whdSearchTabFontPrev;
    @Wire Label     whdSearchTabFontActivePrev;
    @Wire Label     whdSearchTabFontHoverPrev;
    @Wire Spinner   whdSearchPanelPaddingTopBox;
    @Wire Spinner   whdSearchPanelPaddingBottomBox;
    @Wire Spinner   whdSearchPanelPaddingLeftBox;
    @Wire Spinner   whdSearchPanelPaddingRightBox;
    @Wire Div       whdSearchPanelBgPrev;
    
    @Wire Window whdLoginWin;
    @Wire("#whdLoginWin #username") Textbox usernameBox; 
    @Wire("#whdLoginWin #userpwd") Textbox pwdBox;
    
    @Wire Window whdNewWin;
    @Wire("#whdNewWin #whdNewNameBox") Textbox whdNewNameBox;
    @Wire("#whdNewWin #whdNewLayoutIdBox") Listbox whdNewLayoutIdBox;
    @Wire("#whdNewWin #whdNewLayoutVersionBox") Listbox whdNewLayoutVersionBox;

    @Wire Window whdSaveAsWin;
    @Wire("#whdSaveAsWin #whdSaveAsNameBox") Textbox whdSaveAsNameBox;

    @Wire Window whdBorderWin;
    private BackgroundWindowComposer borderWinComposer = null;

    @Wire Window whdFontWin;
    private FontWindowComposer fontWinComposer = null;

    private File baseDir = null;
    private String openedDesignName = null;
    private WebDesignerProps openedDesignProps = null;
    private File openedPreviewDir = null;
    private String loginUserId = "";
    
    // List of uploaded files that need to be written to the saved design folder 
    // when clicking on the "Save" button. The filenames of these files are
    // the deployed filenames.
    private final List<File> changedFiles = new ArrayList<File>();
    
    // List of saved design files that need to be deleted when clicking on the 
    // "Save" button. 
    private final List<File> removedFiles = new ArrayList<File>();
    
    //********************************************************************
    //********************   GUI initialization   ************************
    //********************************************************************

    public void doAfterCompose(Component comp) throws Exception 
    {
        super.doAfterCompose(comp);
        
        fontWinComposer = (FontWindowComposer) whdFontWin.getAttribute("$composer");
        borderWinComposer = (BackgroundWindowComposer) whdBorderWin.getAttribute("$composer");

        // whdLoginWin.setVisible(false);
        whdFontWin.setVisible(false);
        whdBorderWin.setVisible(false);
        
        showLoginWin();
    }

    private void showLoginWin()
    {
        whdMainLayout.setVisible(false);  // whdSelectList.setVisible(false);
        String uName = GUIUtil.getUserNameFromCookie(whdMainWin);
        whdLoginWin.setVisible(true);
        pwdBox.setValue("");
        if ((uName != null) && (uName.length() > 0)) {
            usernameBox.setValue(uName);
            pwdBox.setFocus(true);
        } else {
            usernameBox.setFocus(true);
        }
    }

    /**
     * Initialization of main window after login.
     */
    private void initMain()
    {
        whdMainLayout.setVisible(true);  // whdSelectList.setVisible(true);
        updateWebhelpSelectionList();
        initLayoutIdListbox(whdLayoutIdBox);
        // WhdUtils.selectListItem(whdSelectList, "");
        setDesignerControlsVisible(false);
    }


    //********************************************************************
    //****************   Login Window Event Handler   ********************
    //********************************************************************

    @Listen("onClick = #whdLoginWin #loginBtn")
    public void onLoginClick() throws Exception
    {
        ServletContext serv_ctx = whdLoginWin.getDesktop().getWebApp().getServletContext();
        DocmaWebApplication webapp = DocmaWebApplication.getInstance(serv_ctx);
        if (webapp == null) {
            Messagebox.show("Internal error: Could not get web application instance.");
            return;
        }
        UserManager um = webapp.getUserManager();
        if (um == null) {
            Messagebox.show("Internal error: Could not get user management instance.");
            return;
        }
        String uname = usernameBox.getValue();
        String pwd = pwdBox.getValue();
        if (! um.verifyUserNamePassword(uname, pwd)) {
            pwdBox.setValue("");
            usernameBox.setFocus(true);
            Messagebox.show("Invalid username or password!");
            return;
        }
        
        // Check if user has at least "admin" or "edit styles" or "manage publication" rights
        boolean access_denied = true;
        String uid = um.getUserIdFromName(uname);
        String[] gids = um.getGroupsOfUser(uid);
        for (int i=0; i < gids.length; i++) {   // check all groups
            String str = um.getGroupProperty(gids[i], DocmaConstants.PROP_USERGROUP_RIGHTS);
            if (str != null) {
                AccessRights[] arr = AccessRights.parseAccessRights(str);
                for (int k=0; k < arr.length; k++) {  // check all store rights
                    if (arr[k].containsRightAnyOf(
                        AccessRights.RIGHT_ADMINISTRATION, 
                        AccessRights.RIGHT_EDIT_STYLES, 
                        AccessRights.RIGHT_MANAGE_PUBLICATIONS)) {
                        access_denied = false;
                        break;
                    }
                }
            }
            if (! access_denied) break;
        }
        
        if (access_denied) {
            Messagebox.show("Access denied (not enough rights)!");
            return;
        }
        loginUserId = uid;
        
        whdLoginWin.setVisible(false);
        baseDir = webapp.getBaseDirectory();
        File saveDir = getWebhelpSavedDir();
        if (! saveDir.exists()) {
            saveDir.mkdirs();
            
            File install_examples_dir = new File(getDesignerWebRoot(), "install_designs");
            DocmaUtil.recursiveFileCopy(install_examples_dir, saveDir, false);
        }
        initMain();
    }

    @Listen("onClick = #whdLoginWin #whdLoginHelpBtn")
    public void onShowLoginHelp() throws Exception
    {
        MainWindow.openHelp("../help/customize_webhelp.html");
    }
    

    //********************************************************************
    //****************   Main Window Event Handler   *********************
    //********************************************************************

    @Listen("onClick = #whdExitBtn")
    public void onExitClick()
    {
        if (isOpenedDesignUnsaved()) {
            confirmDiscard(
                new EventListener() {
                    public void onEvent(Event evt) throws Exception {
                        if ("onYes".equalsIgnoreCase(evt.getName())) {
                            // Clients.evalJavaScript("window.close();");
                            closeDesign();
                            WhdUtils.selectListItem(whdSelectList, "");
                            showLoginWin();
                        }
                    }
                }
            );
        } else {
            // Clients.evalJavaScript("window.close();");
            closeDesign();
            WhdUtils.selectListItem(whdSelectList, "");
            showLoginWin();
        }
    }

    @Listen("onSelect = #whdSelectList")
    public void onDesignSelected()
    {
        Listitem sel_item = whdSelectList.getSelectedItem();
        String sel = (sel_item == null) ? null : sel_item.getValue().toString();
        confirmCloseCurrentDesignOpenOther(sel);
    }
    
    @Listen("onClick = #whdNewBtn")
    public void onNewClick()
    {
        whdNewNameBox.setMaxlength(MAXLENGTH_WHD_ID);
        whdNewNameBox.setValue("");
        if (whdNewLayoutIdBox.getItemCount() == 0) {
            initLayoutIdListbox(whdNewLayoutIdBox);
        }
        whdNewLayoutIdBox.setSelectedIndex(0);
        updateNewWebhelpLayoutVersionList();
        
        whdNewWin.doHighlighted();  // whdNewWin.setVisible(true);
    }

    @Listen("onClick = #whdExportBtn")
    public void onExportClick() throws Exception
    {
        if (openedDesignName == null) {
            Messagebox.show("No Design selected.");
            return;
        }
        if (! whdSaveBtn.isDisabled()) {
            Messagebox.show("Design modified. Click 'Save' first!");
            return;
        }
        File dir = getWebhelpExportDir();
        if (dir.exists()) {
            try {
                clearExportDirectory(dir);  // delete old export files
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        File tempDir = new File(dir, "exp" + System.currentTimeMillis());
        tempDir.mkdirs();
        
        // Create copy of openedDesignProps
        String src_props = WhdUtils.readFileToString(openedDesignProps.getFile());
        File dest_file = new File(tempDir, FILENAME_DESIGNER_PROPS);
        WhdUtils.writeStringToFile(src_props, dest_file, "UTF-8");
        WebDesignerProps export_props = new WebDesignerProps(dest_file);

        // Write export files to zip
        File exp_file = new File(tempDir, openedDesignName + ".zip");
        FileOutputStream fout = new FileOutputStream(exp_file);;
        try {
            ZipOutputStream zipout = new ZipOutputStream(fout);
            
            // Export images and adapt filenames of exported images
            exportImage(zipout, getSavedFavicon(openedDesignName), PREFIX_FAVICON);
            exportImage(zipout, getSavedLogo1(openedDesignName), PREFIX_LOGO1);
            exportImage(zipout, getSavedLogo2(openedDesignName), PREFIX_LOGO2);
            exportImage(zipout, getSavedTreeviewIcons(openedDesignName), PREFIX_TREEVIEW_ICONS);
            exportImage(zipout, getSavedTreeviewLine(openedDesignName), PREFIX_TREEVIEW_LINE);
            exportImage(zipout, getSavedTreeviewContentIcon(openedDesignName), PREFIX_TREEVIEW_CONTENT_ICON);
            exportImage(zipout, getSavedPrevIcon(openedDesignName), PREFIX_LOCAL_NAV_PREV);
            exportImage(zipout, getSavedNextIcon(openedDesignName), PREFIX_LOCAL_NAV_NEXT);
            exportImage(zipout, getSavedUpIcon(openedDesignName), PREFIX_LOCAL_NAV_UP);
            exportImage(zipout, getSavedHomeIcon(openedDesignName), PREFIX_LOCAL_NAV_HOME);
            exportImage(zipout, getSavedTocIcon(openedDesignName), PREFIX_LOCAL_NAV_TOC);
            export_props.setNavigationToggleButtonImage(
                exportImage(zipout, getSavedDesignFile(openedDesignProps.getNavigationToggleButtonImage()), PREFIX_SHOW_HIDE_TOC_BUTTON)
            );
            export_props.setHeaderBgImage(
                exportImage(zipout, getSavedDesignFile(openedDesignProps.getHeaderBgImage()), PREFIX_HEAD_BG)
            );
            export_props.setNavigationBgImage(
                exportImage(zipout, getSavedDesignFile(openedDesignProps.getNavigationBgImage()), PREFIX_NAV_BG)
            );
            export_props.setAccordionBgImage(
                exportImage(zipout, getSavedDesignFile(openedDesignProps.getAccordionBgImage()), PREFIX_NAV_ACCORDION_BG)
            );
            export_props.setNavigationTreeBgImage(
                exportImage(zipout, getSavedDesignFile(openedDesignProps.getNavigationTreeBgImage()), PREFIX_NAV_INNER_BG)
            );
            export_props.setPartTabBgImage(
                exportImage(zipout, getSavedDesignFile(openedDesignProps.getPartTabBgImage()), PREFIX_PART_TAB_BG)
            );
            export_props.setPartTabBgImageCurrent(
                exportImage(zipout, getSavedDesignFile(openedDesignProps.getPartTabBgImageCurrent()), PREFIX_PART_TAB_CT_BG)
            );
            export_props.setPartTabBgImageHover(
                exportImage(zipout, getSavedDesignFile(openedDesignProps.getPartTabBgImageHover()), PREFIX_PART_TAB_HL_BG)
            );
            export_props.setLocalNavigationBgImage(
                exportImage(zipout, getSavedDesignFile(openedDesignProps.getLocalNavigationBgImage()), PREFIX_LOCAL_NAV_BG)
            );
            export_props.setBreadcrumbsBgImage(
                exportImage(zipout, getSavedDesignFile(openedDesignProps.getBreadcrumbsBgImage()), PREFIX_BREAD_BG)
            );
            export_props.setBreadcrumbsSeparatorImage(
                exportImage(zipout, getSavedDesignFile(openedDesignProps.getBreadcrumbsSeparatorImage()), PREFIX_BREAD_SEP)
            );
            export_props.setSearchBgImage(
                exportImage(zipout, getSavedDesignFile(openedDesignProps.getSearchBgImage()), PREFIX_SEARCH_BG)
            );
            export_props.setSearchInnerBgImage(
                exportImage(zipout, getSavedDesignFile(openedDesignProps.getSearchInnerBgImage()), PREFIX_SEARCH_INNER_BG)
            );
            export_props.setSearchInputBgImage(
                exportImage(zipout, getSavedDesignFile(openedDesignProps.getSearchInputBgImage()), PREFIX_SEARCH_INPUT_BG)
            );
            export_props.setSearchButtonBgImage(
                exportImage(zipout, getSavedDesignFile(openedDesignProps.getSearchButtonBgImage()), PREFIX_SEARCH_BUTTON)
            );
            export_props.setSearchCloseImage(
                exportImage(zipout, getSavedDesignFile(openedDesignProps.getSearchCloseImage()), PREFIX_SEARCH_CLOSE_ICON)
            );
            export_props.setSearchToggleButtonImage(
                exportImage(zipout, getSavedDesignFile(openedDesignProps.getSearchToggleButtonImage()), PREFIX_SEARCH_HIGHLIGHT_BUTTON)
            );
            export_props.setSearchTabBgImage(
                exportImage(zipout, getSavedDesignFile(openedDesignProps.getSearchTabBgImage()), PREFIX_SEARCH_TAB_BG)
            );
            export_props.setSearchTabBgImageActive(
                exportImage(zipout, getSavedDesignFile(openedDesignProps.getSearchTabBgImageActive()), PREFIX_SEARCH_TAB_CT_BG)
            );
            export_props.setSearchTabBgImageHover(
                exportImage(zipout, getSavedDesignFile(openedDesignProps.getSearchTabBgImageHover()), PREFIX_SEARCH_TAB_HL_BG)
            );
            export_props.setSearchPanelBgImage(
                exportImage(zipout, getSavedDesignFile(openedDesignProps.getSearchPanelBgImage()), PREFIX_SEARCH_PANEL_BG)
            );
            export_props.save();

            // Write positioning.css
            String exp_css = getPositioningContent(export_props);
            exportTextFile(zipout, "positioning.css", exp_css);

            // Write webhelp_config.js
            String exp_js = getJsConfigContent(export_props);
            exportTextFile(zipout, "webhelp_config.js", exp_js);
            
            // Write webhelp.properties
            String exp_props_str = WhdUtils.readFileToString(export_props.getFile());
            exportTextFile(zipout, FILENAME_DESIGNER_PROPS, exp_props_str);
            
            
            zipout.close();
        } finally {
            try { 
                fout.close(); 
            } catch (IOException ioe) {}
        }
        
        Filedownload.save(exp_file, null);
    }
    
    private void exportTextFile(ZipOutputStream zipout, String filename, String content) throws IOException
    {
        ZipEntry ze = new ZipEntry(filename);
        zipout.putNextEntry(ze);
        OutputStreamWriter writer = new OutputStreamWriter(zipout, "UTF-8");
        writer.append(content);
        writer.flush();
        zipout.closeEntry();
    }
    
    private String exportImage(ZipOutputStream zipout, File afile, String export_prefix) throws IOException
    {
        if ((afile == null) || ! (afile.exists() && afile.isFile())) {
            return "";
        }
        
        String fn = afile.getName();
        String ext = WhdUtils.getFilenameExtension(fn);
        String exp_fn = export_prefix + "_" + openedDesignName + ((ext == null) ? "" : ("." + ext));
        
        ZipEntry ze = new ZipEntry(exp_fn);
        zipout.putNextEntry(ze);
        // Daten an zipout senden
        InputStream in = new FileInputStream(afile);
        try {
            byte[] buf = new byte[64*1024];
            int cnt;
            while ((cnt = in.read(buf)) >= 0) {
                if (cnt > 0) zipout.write(buf, 0, cnt);
            }
        } finally {
            in.close();
        }
        zipout.closeEntry();
        
        return exp_fn;
    }
    
    private void clearExportDirectory(File dir)
    {
        final String EXP_PREF = "exp";
        String[] subdirs = dir.list();
        if (subdirs == null) {   // dir is no valid directory
            return;
        }
        for (String fn : subdirs) {
            if (fn.toLowerCase().startsWith(EXP_PREF)) {
                try {
                    long stamp = Long.parseLong(fn.substring(EXP_PREF.length()));
                    long now = System.currentTimeMillis();
                    if ((now - stamp) > (1000 * 60 * 60 * 48)) {  // delete files older than 48 hours
                        WhdUtils.recursiveFileDelete(new File(dir, fn));
                    }
                } catch (Exception ex) {}
            }
        }
    }

    @Listen("onUpload = #whdImportBtn")
    public void onImportClick(Event evt) throws Exception
    {
        UploadEvent uevt = (UploadEvent) evt;
        org.zkoss.util.media.Media media = uevt.getMedia();
        String fn = media.getName();
        String ext = (fn == null) ? null : WhdUtils.getFilenameExtension(fn);
        if ((ext == null) || !ext.equalsIgnoreCase("zip")) {
            Messagebox.show("Import package must have file extension .zip!");
            return;
        }
        String design_name = fn.substring(0, fn.lastIndexOf('.'));
        if (design_name.trim().equals("")) {
            Messagebox.show("Invalid filename!");
            return;
        }
        if (! validNewDesignName(design_name)) {
            return;
        }

        File design_dir = getWebhelpSavedDesignDir(design_name);
        InputStream in = media.getStreamData();
        WhdUtils.extractZipStream(in, design_dir);
        File props_file = new File(design_dir, FILENAME_DESIGNER_PROPS);
        if (! props_file.exists()) {
            try {
                WhdUtils.recursiveFileDelete(design_dir);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            Messagebox.show("Invalid WebHelp-Design package. Missing file: " + FILENAME_DESIGNER_PROPS);
            return;
        }
        File css_file = new File(design_dir, "positioning.css");
        File js_file = new File(design_dir, "webhelp_config.js");
        css_file.delete();
        js_file.delete();
        
        updateWebhelpSelectionList();
        
        Messagebox.show("The design '" + design_name + "' has been successfully imported!");
    }

    @Listen("onCheck = #whdPreviewPartsCheckbox")
    public void onPreviewPartsClick()
    {
        updatePreviewUrl();
    }

    @Listen("onCheck = #whdPreviewCrumbsCheckbox")
    public void onPreviewCrumbsClick() throws Exception
    {
        boolean checked = whdPreviewCrumbsCheckbox.isChecked();
        openedDesignProps.setBreadcrumbsDisplay(checked ? "block" : "none");
        
        designChanged();
    }

    @Listen("onClick = #whdPreviewRefreshBtn")
    public void onPreviewRefreshClick() throws Exception
    {
        boolean renamed = renameOpenedPreviewDir();
        writePreviewConfigFiles();
        if (renamed) {
            updatePreviewUrl();
        } else {
            refreshPreview();
        }
    }

    @Listen("onClick = #whdSaveBtn")
    public void onSaveClick() throws Exception
    {
        if (openedDesignProps != null) {
            openedDesignProps.save();
            saveChangedFiles();
            disableSave(true);
        }
    }

    @Listen("onClick = #whdSaveAsBtn")
    public void onSaveAsClick() throws Exception
    {
        if (openedDesignName != null) {
            whdSaveAsNameBox.setMaxlength(Math.max(MAXLENGTH_WHD_ID, openedDesignName.length()));
            whdSaveAsNameBox.setValue(openedDesignName);
            whdSaveAsWin.doHighlighted();
        } else {
            Messagebox.show("No design selected!");
            return;
        }
    }

    @Listen("onClick = #whdDeleteBtn")
    public void onDeleteClick()
    {
        if (openedDesignName != null) {
            final String delDesign = openedDesignName;
            Messagebox.show(
                "Really delete '" + delDesign + "'?", 
                "Delete?", Messagebox.YES | Messagebox.NO, 
                Messagebox.QUESTION, 
                new EventListener() {
                    public void onEvent(Event evt) throws Exception {
                        if ("onYes".equalsIgnoreCase(evt.getName())) {
                            deleteDesign(delDesign);
                        }
                    }
                }
            );
        }
    }

    @Listen("onTimer = #whdPreviewTimer")
    public void onPreviewTimer() throws Exception
    {
        // System.out.println("Header height: " + openedDesignProps.getHeaderHeight());
        writePreviewConfigFiles();
        refreshPreview();
    }

    //********************************************************************
    //**************   General Settings Event Handler   ******************
    //********************************************************************

    @Listen("onSelect = #whdLayoutIdBox")
    public void onLayoutIdSelected()
    {
        Listitem item = whdLayoutIdBox.getSelectedItem();
        String layout_id = (item == null) ? null : item.getValue().toString();
        
        String ver = openedDesignProps.getLayoutVersion();
        initLayoutVersionListbox(layout_id, whdLayoutVersionBox);
        if (! WhdUtils.selectListItem(whdLayoutVersionBox, ver)) {
            Listitem ver_item = whdLayoutVersionBox.getItemAtIndex(whdLayoutVersionBox.getItemCount() - 1);
            whdLayoutVersionBox.setSelectedItem(ver_item);
            ver = ver_item.getValue().toString();
        }

        openedDesignProps.setLayout(layout_id, ver);
        designChanged();
    }

    @Listen("onSelect = #whdLayoutVersionBox")
    public void onLayoutVersionSelected()
    {
        Listitem sel_item = whdLayoutVersionBox.getSelectedItem();
        String sel = (sel_item == null) ? null : sel_item.getValue().toString();
        openedDesignProps.setLayout(openedDesignProps.getLayoutId(), sel);
        designChanged();
    }

    @Listen("onUpload = #whdFaviconReplaceBtn")
    public void onFaviconReplaceClick(Event evt) throws Exception
    {
        UploadEvent uevt = (UploadEvent) evt;
        org.zkoss.util.media.Media media = uevt.getMedia();
        String fn = media.getName();
        if (fn != null) {
            String ext = WhdUtils.getFilenameExtension(fn);
            if (! ext.equalsIgnoreCase("ico")) {
                Messagebox.show("Favorite icon must have file extension .ico!");
                return;
            }
        }
        if (media instanceof org.zkoss.image.Image) {
            File f_uploaded = updatePreviewFile(media.getByteData(), DEPLOY_FILENAME_FAVICON);
            addToChangedFiles(f_uploaded);

            whdFaviconImage.setContent((org.zkoss.image.Image) media);
            designChanged();
        } else {
            Messagebox.show("Unknown image format!");
        }
    }

    @Listen("onClick = #whdFaviconDownloadBtn")
    public void onFaviconDownloadClick() throws Exception
    {
        File down_file = getSavedFavicon(openedDesignName);
        if (down_file != null) {
            if (isFileChanged(down_file.getName())) {
                Messagebox.show("File has been changed. Click save before download.");
                return;
            }
            Filedownload.save(down_file, null);
        }
    }

    @Listen("onChange = #whdGeneralBgColorBox")
    public void onChangeGeneralBgColor()
    {
        openedDesignProps.setBodyBgColor(whdGeneralBgColorBox.getColor());
        designChanged();
    }

    @Listen("onChange = #whdContentTopBox; onChanging = #whdContentTopBox")
    public void onContentTopChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setContentTop(getPixelInput(evt, whdContentTopBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdContentPaddingLeftBox; onChanging = #whdContentPaddingLeftBox")
    public void onContentPaddingLeftChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setContentPaddingLeft(getPixelInput(evt, whdContentPaddingLeftBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdContentPaddingRightBox; onChanging = #whdContentPaddingRightBox")
    public void onContentPaddingRightChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setContentPaddingRight(getPixelInput(evt, whdContentPaddingRightBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdContentWidthMinBox; onChanging = #whdContentWidthMinBox")
    public void onContentWidthMinChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setContentWidthMin(getPixelInput(evt, whdContentWidthMinBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdContentWidthMaxBox; onChanging = #whdContentWidthMaxBox")
    public void onContentWidthMaxChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setContentWidthMax(getPixelInputOrEmpty(evt, whdContentWidthMaxBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdWatermarkXPosBox; onChanging = #whdWatermarkXPosBox")
    public void onWatermarkXPosChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setWatermarkPosX(getPixelInputOrEmpty(evt, whdWatermarkXPosBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdWatermarkYPosBox; onChanging = #whdWatermarkYPosBox")
    public void onWatermarkYPosChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setWatermarkPosY(getPixelInputOrEmpty(evt, whdWatermarkYPosBox));
            designChanged();
        }
    }

    //********************************************************************
    //**************   Heading Settings Event Handler   ******************
    //********************************************************************

    @Listen("onChange = #whdHeaderHeightBox; onChanging = #whdHeaderHeightBox")
    public void onHeaderHeightChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setHeaderHeight(getPixelInput(evt, whdHeaderHeightBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdHeaderPaddingTopBox; onChanging = #whdHeaderPaddingTopBox")
    public void onHeaderPaddingTopChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setHeaderPaddingTop(getPixelInput(evt, whdHeaderPaddingTopBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdHeaderPaddingBottomBox; onChanging = #whdHeaderPaddingBottomBox")
    public void onHeaderPaddingBottomChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setHeaderPaddingBottom(getPixelInput(evt, whdHeaderPaddingBottomBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdHeaderPaddingLeftBox; onChanging = #whdHeaderPaddingLeftBox")
    public void onHeaderPaddingLeftChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setHeaderPaddingLeft(getPixelInput(evt, whdHeaderPaddingLeftBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdHeaderPaddingRightBox; onChanging = #whdHeaderPaddingRightBox")
    public void onHeaderPaddingRightChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setHeaderPaddingRight(getPixelInput(evt, whdHeaderPaddingRightBox));
            designChanged();
        }
    }

    @Listen("onSelect = #whdHeaderTitleAlignBox")
    public void onHeaderTitleAlignSelected()
    {
        Listitem sel_item = whdHeaderTitleAlignBox.getSelectedItem();
        String sel = (sel_item == null) ? "left" : sel_item.getValue().toString();
        openedDesignProps.setHeaderTitleAlignment(sel);
        designChanged();
    }

    @Listen("onChange = #whdHeaderTitleMarginTopBox; onChanging = #whdHeaderTitleMarginTopBox")
    public void onHeaderTitleMarginTopChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setHeaderTitleMarginTop(getPixelInput(evt, whdHeaderTitleMarginTopBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdHeaderTitleMarginBottomBox; onChanging = #whdHeaderTitleMarginBottomBox")
    public void onHeaderTitleMarginBottomChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setHeaderTitleMarginBottom(getPixelInput(evt, whdHeaderTitleMarginBottomBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdHeaderTitleMarginLeftBox; onChanging = #whdHeaderTitleMarginLeftBox")
    public void onHeaderTitleMarginLeftChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setHeaderTitleMarginLeft(getPixelInput(evt, whdHeaderTitleMarginLeftBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdHeaderTitleMarginRightBox; onChanging = #whdHeaderTitleMarginRightBox")
    public void onHeaderTitleMarginRightChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setHeaderTitleMarginRight(getPixelInput(evt, whdHeaderTitleMarginRightBox));
            designChanged();
        }
    }

    @Listen("onCheck = #whdHeaderTitle1Checkbox")
    public void onHeaderTitle1Click()
    {
        boolean is_enabled = whdHeaderTitle1Checkbox.isChecked();
        openedDesignProps.setHeaderTitle1Enabled(is_enabled);
        designChanged();
    }

    @Listen("onCheck = #whdHeaderTitle2Checkbox")
    public void onHeaderTitle2Click()
    {
        boolean is_enabled = whdHeaderTitle2Checkbox.isChecked();
        openedDesignProps.setHeaderTitle2Enabled(is_enabled);
        designChanged();
    }

    //********************************************************************
    //****************   Logo Settings Event Handler   *******************
    //********************************************************************

    @Listen("onCheck = #whdLogo1Checkbox")
    public void onLogo1Click()
    {
        boolean is_enabled = whdLogo1Checkbox.isChecked();
        whdLogo1Area.setVisible(is_enabled);
        openedDesignProps.setLogo1Visible(is_enabled);
        designChanged();
    }

    @Listen("onUpload = #whdLogo1ReplaceBtn")
    public void onLogo1ReplaceClick(Event evt) throws Exception
    {
        UploadEvent uevt = (UploadEvent) evt;
        org.zkoss.util.media.Media media = uevt.getMedia();
        String fn = media.getName();
        if (fn != null) {
            String ext = WhdUtils.getFilenameExtension(fn);
            if (! ext.equalsIgnoreCase("png")) {
                Messagebox.show("Logo images must have file extension .png!");
                return;
            }
        }
        if (media instanceof org.zkoss.image.Image) {
            File f_uploaded = writeImageToPreviewDirs(media, DEPLOY_FILENAME_LOGO1);
            addToChangedFiles(f_uploaded);
            
            whdLogo1Area.setVisible(true);
            whdLogo1Image.setContent((org.zkoss.image.Image) media);
            int img_width = ((org.zkoss.image.Image) media).getWidth();
            int img_height = ((org.zkoss.image.Image) media).getHeight();
            whdLogo1WidthBox.setValue(img_width);
            whdLogo1HeightBox.setValue(img_height);
            whdLogo1Checkbox.setChecked(true);
            whdLogo1DownloadBtn.setDisabled(false);
            whdLogo1DeleteBtn.setDisabled(false);
            openedDesignProps.setLogo1Width(img_width + "px");
            openedDesignProps.setLogo1Height(img_height + "px");
            openedDesignProps.setLogo1Visible(true);
            
            designChanged();
        } else {
            Messagebox.show("Unknown image format!");
            return;
        }
    }

    @Listen("onClick = #whdLogo1DownloadBtn")
    public void onLogo1DownloadClick() throws Exception
    {
        File down_file = getSavedLogo1(openedDesignName);
        if (down_file != null) {
            if (isFileChanged(down_file.getName())) {
                Messagebox.show("File has been changed. Click save before download.");
                return;
            }
            Filedownload.save(down_file, null);
        }
    }

    @Listen("onClick = #whdLogo1DeleteBtn")
    public void onLogo1DeleteClick() throws Exception
    {
        File blank_file = getBlankFile_PNG();
        AImage blank_img = new AImage(blank_file);
        File changed_logo = writeImageToPreviewDirs(blank_img, DEPLOY_FILENAME_LOGO1);
        addToChangedFiles(changed_logo);
        
        whdLogo1Image.setContent(blank_img);
        whdLogo1WidthBox.setValue(0);
        whdLogo1HeightBox.setValue(0);
        whdLogo1Checkbox.setChecked(false);
        whdLogo1Area.setVisible(false);
        openedDesignProps.setLogo1Width("0px");
        openedDesignProps.setLogo1Height("0px");
        openedDesignProps.setLogo1Visible(false);
        designChanged();
        whdLogo1DownloadBtn.setDisabled(true);
        whdLogo1DeleteBtn.setDisabled(true);
    }

    @Listen("onSelect = #whdLogo1PositionBox")
    public void onLogo1PositionSelected()
    {
        Listitem sel_item = whdLogo1PositionBox.getSelectedItem();
        String sel = (sel_item == null) ? "left" : sel_item.getValue().toString();
        openedDesignProps.setLogo1Position(sel);
        designChanged();
    }

    @Listen("onChange = #whdLogo1WidthBox; onChanging = #whdLogo1WidthBox")
    public void onLogo1WidthChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setLogo1Width(getPixelInput(evt, whdLogo1WidthBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdLogo1HeightBox; onChanging = #whdLogo1HeightBox")
    public void onLogo1HeightChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setLogo1Height(getPixelInput(evt, whdLogo1HeightBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdLogo1XPosBox; onChanging = #whdLogo1XPosBox")
    public void onLogo1XPosChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setLogo1PosX(getPixelInput(evt, whdLogo1XPosBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdLogo1YPosBox; onChanging = #whdLogo1YPosBox")
    public void onLogo1YPosChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setLogo1PosY(getPixelInput(evt, whdLogo1YPosBox));
            designChanged();
        }
    }

    @Listen("onCheck = #whdLogo2Checkbox")
    public void onLogo2Click()
    {
        boolean is_enabled = whdLogo2Checkbox.isChecked();
        whdLogo2Area.setVisible(is_enabled);
        openedDesignProps.setLogo2Visible(is_enabled);
        designChanged();
    }

    @Listen("onUpload = #whdLogo2ReplaceBtn")
    public void onLogo2ReplaceClick(Event evt) throws Exception
    {
        UploadEvent uevt = (UploadEvent) evt;
        org.zkoss.util.media.Media media = uevt.getMedia();
        String fn = media.getName();
        if (fn != null) {
            String ext = WhdUtils.getFilenameExtension(fn);
            if (! ext.equalsIgnoreCase("png")) {
                Messagebox.show("Logo images must have file extension .png!");
                return;
            }
        }
        if (media instanceof org.zkoss.image.Image) {
            File f_uploaded = writeImageToPreviewDirs(media, DEPLOY_FILENAME_LOGO2);
            addToChangedFiles(f_uploaded);
            
            whdLogo2Area.setVisible(true);
            whdLogo2Image.setContent((org.zkoss.image.Image) media);
            int img_width = ((org.zkoss.image.Image) media).getWidth();
            int img_height = ((org.zkoss.image.Image) media).getHeight();
            whdLogo2WidthBox.setValue(img_width);
            whdLogo2HeightBox.setValue(img_height);
            whdLogo2Checkbox.setChecked(true);
            whdLogo2DownloadBtn.setDisabled(false);
            whdLogo2DeleteBtn.setDisabled(false);
            openedDesignProps.setLogo2Width(img_width + "px");
            openedDesignProps.setLogo2Height(img_height + "px");
            openedDesignProps.setLogo2Visible(true);
            
            designChanged();
        } else {
            Messagebox.show("Unknown image format!");
            return;
        }
    }

    @Listen("onClick = #whdLogo2DownloadBtn")
    public void onLogo2DownloadClick() throws Exception
    {
        File down_file = getSavedLogo2(openedDesignName);
        if (down_file != null) {
            if (isFileChanged(down_file.getName())) {
                Messagebox.show("File has been changed. Click save before download.");
                return;
            }
            Filedownload.save(down_file, null);
        }
    }

    @Listen("onClick = #whdLogo2DeleteBtn")
    public void onLogo2DeleteClick() throws Exception
    {
        File blank_file = getBlankFile_PNG();
        AImage blank_img = new AImage(blank_file);
        File changed_logo = writeImageToPreviewDirs(blank_img, DEPLOY_FILENAME_LOGO2);
        addToChangedFiles(changed_logo);
        
        whdLogo2Image.setContent(blank_img);
        whdLogo2WidthBox.setValue(0);
        whdLogo2HeightBox.setValue(0);
        whdLogo2Checkbox.setChecked(false);
        whdLogo2Area.setVisible(false);
        openedDesignProps.setLogo2Width("0px");
        openedDesignProps.setLogo2Height("0px");
        openedDesignProps.setLogo2Visible(false);
        designChanged();
        whdLogo2DownloadBtn.setDisabled(true);
        whdLogo2DeleteBtn.setDisabled(true);
    }

    @Listen("onSelect = #whdLogo2PositionBox")
    public void onLogo2PositionSelected()
    {
        Listitem sel_item = whdLogo2PositionBox.getSelectedItem();
        String sel = (sel_item == null) ? "right" : sel_item.getValue().toString();
        openedDesignProps.setLogo2Position(sel);
        designChanged();
    }

    @Listen("onChange = #whdLogo2WidthBox; onChanging = #whdLogo2WidthBox")
    public void onLogo2WidthChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setLogo2Width(getPixelInput(evt, whdLogo2WidthBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdLogo2HeightBox; onChanging = #whdLogo2HeightBox")
    public void onLogo2HeightChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setLogo2Height(getPixelInput(evt, whdLogo2HeightBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdLogo2XPosBox; onChanging = #whdLogo2XPosBox")
    public void onLogo2XPosChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setLogo2PosX(getPixelInput(evt, whdLogo2XPosBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdLogo2YPosBox; onChanging = #whdLogo2YPosBox")
    public void onLogo2YPosChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setLogo2PosY(getPixelInput(evt, whdLogo2YPosBox));
            designChanged();
        }
    }


    //********************************************************************
    //************   Navigation Settings Event Handler   *****************
    //********************************************************************
    
    @Listen("onChange = #whdNavigationWidthBox; onChanging = #whdNavigationWidthBox")
    public void onNavigationWidthChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setNavigationWidth(getPixelInput(evt, whdNavigationWidthBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdNavigationMarginTopBox; onChanging = #whdNavigationMarginTopBox")
    public void onNavigationMarginTopChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setNavigationMarginTop(getPixelInput(evt, whdNavigationMarginTopBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdNavigationMarginBottomBox; onChanging = #whdNavigationMarginBottomBox")
    public void onNavigationMarginBottomChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setNavigationMarginBottom(getPixelInput(evt, whdNavigationMarginBottomBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdNavigationPaddingTopBox; onChanging = #whdNavigationPaddingTopBox")
    public void onNavigationPaddingTopChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setNavigationPaddingTop(getPixelInput(evt, whdNavigationPaddingTopBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdNavigationPaddingBottomBox; onChanging = #whdNavigationPaddingBottomBox")
    public void onNavigationPaddingBottomChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setNavigationPaddingBottom(getPixelInput(evt, whdNavigationPaddingBottomBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdNavigationPaddingLeftBox; onChanging = #whdNavigationPaddingLeftBox")
    public void onNavigationPaddingLeftChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setNavigationPaddingLeft(getPixelInput(evt, whdNavigationPaddingLeftBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdNavigationPaddingRightBox; onChanging = #whdNavigationPaddingRightBox")
    public void onNavigationPaddingRightChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setNavigationPaddingRight(getPixelInput(evt, whdNavigationPaddingRightBox));
            designChanged();
        }
    }

    @Listen("onCheck = #whdNavResizeCheckbox")
    public void onNavigationResizeClick()
    {
        boolean is_enabled = whdNavResizeCheckbox.isChecked();
        openedDesignProps.setNavigationResizeEnabled(is_enabled);
        designChanged();
    }

    @Listen("onCheck = #whdNavAccordionCheckbox")
    public void onAccordionClick()
    {
        boolean is_enabled = whdNavAccordionCheckbox.isChecked();
        openedDesignProps.setAccordionEnabled(is_enabled);
        updateNavigationControls();
        designChanged();
    }

    @Listen("onChange = #whdNavAccordionPaddingXBox; onChanging = #whdNavAccordionPaddingXBox")
    public void onAccordionPaddingXChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setAccordionPaddingX(getPixelInput(evt, whdNavAccordionPaddingXBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdNavAccordionHeightBox; onChanging = #whdNavAccordionHeightBox")
    public void onAccordionHeightChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setAccordionHeight(getPixelInput(evt, whdNavAccordionHeightBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdNavAccordionSpacingBox; onChanging = #whdNavAccordionSpacingBox")
    public void onAccordionSpacingChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setAccordionSpacing(getPixelInput(evt, whdNavAccordionSpacingBox));
            designChanged();
        }
    }

    @Listen("onSelect = #whdNavTreeAnimationBox")
    public void onNavigationTreeAnimationSelected() throws Exception
    {
        Listitem sel_item = whdNavTreeAnimationBox.getSelectedItem();
        String anim = (sel_item == null) ? "off" : sel_item.getValue().toString();
        openedDesignProps.setNavigationTreeAnimation(anim);
        designChanged();
    }

    @Listen("onCheck = #whdNavTreeShowTitleBox")
    public void onNavigationTitleVisibleClick()
    {
        openedDesignProps.setNavigationTreeTitleVisible(whdNavTreeShowTitleBox.isChecked());
        designChanged();
    }

    @Listen("onCheck = #whdNavTreeCollapsedBox")
    public void onNavigationTreeCollapsedClick()
    {
        openedDesignProps.setNavigationTreeCollapsed(whdNavTreeCollapsedBox.isChecked());
        designChanged();
    }

    @Listen("onCheck = #whdNavTreeAutoCloseBox")
    public void onNavigationTreeAutoCloseClick()
    {
        openedDesignProps.setNavigationTreeAutoClose(whdNavTreeAutoCloseBox.isChecked());
        designChanged();
    }

    @Listen("onUpload = #whdNavTreeContentImgReplaceBtn")
    public void onNavigationTreeContentImgReplaceClick(Event evt) throws Exception
    {
        UploadEvent uevt = (UploadEvent) evt;
        org.zkoss.util.media.Media media = uevt.getMedia();
        String fn = media.getName();
        if (fn != null) {
            String ext = WhdUtils.getFilenameExtension(fn);
            if (! ext.equalsIgnoreCase("gif")) {
                Messagebox.show("Content icon must have file extension .gif!");
                return;
            }
        }
        if (media instanceof org.zkoss.image.Image) {
            File f_uploaded = updatePreviewFile(media.getByteData(), webhelpTreeviewImagePath() + File.separator + DEPLOY_FILENAME_TREE_CONTENT_ICON);
            addToChangedFiles(f_uploaded);

            int w = ((org.zkoss.image.Image) media).getWidth();
            whdNavTreeContentImage.setContent((org.zkoss.image.Image) media);
            whdNavTreeContentImgDownloadBtn.setDisabled(false);
            whdNavTreeContentImgDeleteBtn.setDisabled(false);
            whdNavTreeContentImgWidthBox.setValue(w);
            openedDesignProps.setNavigationContentIconWidth(w + "px");
            designChanged();
        } else {
            Messagebox.show("Unknown image format!");
            return;
        }
    }

    @Listen("onClick = #whdNavTreeContentImgDownloadBtn")
    public void onNavigationTreeContentImgDownloadClick() throws Exception
    {
        File down_file = getSavedTreeviewContentIcon(openedDesignName);
        if (down_file != null) {
            if (isFileChanged(down_file.getName())) {
                Messagebox.show("File has been changed. Click save before download.");
                return;
            }
            Filedownload.save(down_file, null);
        }
    }

    @Listen("onClick = #whdNavTreeContentImgDeleteBtn")
    public void onNavigationTreeContentImgDeleteClick() throws Exception
    {
        AImage blank_img = new AImage(getBlankFile_GIF());
        File changed_icon = updatePreviewFile(blank_img.getByteData(), webhelpTreeviewImagePath() + File.separator + DEPLOY_FILENAME_TREE_CONTENT_ICON); 
        addToChangedFiles(changed_icon);
        
        whdNavTreeContentImage.setContent(blank_img);
        whdNavTreeContentImgWidthBox.setValue(0);
        openedDesignProps.setNavigationContentIconWidth("0px");
        designChanged();
        whdNavTreeContentImgDownloadBtn.setDisabled(true);
        whdNavTreeContentImgDeleteBtn.setDisabled(true);
    }

    @Listen("onChange = #whdNavTreeContentImgWidthBox; onChanging = #whdNavTreeContentImgWidthBox")
    public void onNavigationTreeContentImgWidthChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setNavigationContentIconWidth(getPixelInput(evt, whdNavTreeContentImgWidthBox));
            designChanged();
        }
    }

    @Listen("onUpload = #whdNavTreeOpenCloseImgReplaceBtn")
    public void onNavTreeOpenCloseImgReplaceClick(Event evt) throws Exception
    {
        UploadEvent uevt = (UploadEvent) evt;
        org.zkoss.util.media.Media media = uevt.getMedia();
        String fn = media.getName();
        if (fn != null) {
            String ext = WhdUtils.getFilenameExtension(fn);
            if (! ext.equalsIgnoreCase("gif")) {
                Messagebox.show("Image must have file extension .gif!");
                return;
            }
        }
        if (media instanceof org.zkoss.image.Image) {
            File f_uploaded = updatePreviewFile(media.getByteData(), webhelpTreeviewImagePath() + File.separator + DEPLOY_FILENAME_TREE_ICONS);
            addToChangedFiles(f_uploaded);

            whdNavTreeOpenCloseImage.setContent((org.zkoss.image.Image) media);
            designChanged();
        } else {
            Messagebox.show("Unknown image format!");
        }
    }

    @Listen("onClick = #whdNavTreeOpenCloseImgDownloadBtn")
    public void onNavTreeOpenCloseImgDownloadClick() throws Exception
    {
        File down_file = getSavedTreeviewIcons(openedDesignName);
        if (down_file != null) {
            if (isFileChanged(down_file.getName())) {
                Messagebox.show("File has been changed. Click save before download.");
                return;
            }
            Filedownload.save(down_file, null);
        }
    }

    @Listen("onUpload = #whdNavTreeLineImgReplaceBtn")
    public void onNavTreeLineImgReplaceClick(Event evt) throws Exception
    {
        UploadEvent uevt = (UploadEvent) evt;
        org.zkoss.util.media.Media media = uevt.getMedia();
        String fn = media.getName();
        if (fn != null) {
            String ext = WhdUtils.getFilenameExtension(fn);
            if (! ext.equalsIgnoreCase("gif")) {
                Messagebox.show("Image must have file extension .gif!");
                return;
            }
        }
        if (media instanceof org.zkoss.image.Image) {
            File f_uploaded = updatePreviewFile(media.getByteData(), webhelpTreeviewImagePath() + File.separator + DEPLOY_FILENAME_TREE_LINE);
            addToChangedFiles(f_uploaded);

            whdNavTreeLineImage.setContent((org.zkoss.image.Image) media);
            designChanged();
        } else {
            Messagebox.show("Unknown image format!");
        }
    }

    @Listen("onClick = #whdNavTreeLineImgDownloadBtn")
    public void onNavTreeLineImgDownloadClick() throws Exception
    {
        File down_file = getSavedTreeviewLine(openedDesignName);
        if (down_file != null) {
            if (isFileChanged(down_file.getName())) {
                Messagebox.show("File has been changed. Click save before download.");
                return;
            }
            Filedownload.save(down_file, null);
        }
    }

    @Listen("onUpload = #whdNavigationToggleReplaceBtn")
    public void onNavigationToggleReplaceClick(Event evt) throws Exception
    {
        UploadEvent uevt = (UploadEvent) evt;
        org.zkoss.util.media.Media media = uevt.getMedia();
        String fn = media.getName();
        String ext = (fn == null) ? null : WhdUtils.getFilenameExtension(fn);
        if ((ext == null) || ext.equals("")) {
            Messagebox.show("Invalid filename!");
            return;
        }
        if (media instanceof org.zkoss.image.Image) {
            String show_hide_fn = PREFIX_SHOW_HIDE_TOC_BUTTON + System.currentTimeMillis() + "." + ext;
            File f_uploaded = writeImageToPreviewDirs(media, show_hide_fn);
            addToRemovedFiles(openedDesignProps.getNavigationToggleButtonImage());
            addToChangedFiles(f_uploaded);

            int w = ((org.zkoss.image.Image) media).getWidth();
            int btn_w = (w > 1) ? (w / 2) : 0;
            whdNavigationToggleImage.setContent((org.zkoss.image.Image) media);
            whdNavigationToggleDownloadBtn.setDisabled(false);
            whdNavigationToggleDeleteBtn.setDisabled(false);
            whdNavigationToggleWidthBox.setValue(btn_w);
            openedDesignProps.setNavigationToggleButtonImage(show_hide_fn);
            openedDesignProps.setNavigationToggleButtonWidth(btn_w + "px");
            designChanged();
        } else {
            Messagebox.show("Unknown image format!");
            return;
        }
    }

    @Listen("onClick = #whdNavigationToggleDownloadBtn")
    public void onNavigationToggleDownloadClick() throws Exception
    {
        File down_file = getSavedDesignFile(openedDesignName, openedDesignProps.getNavigationToggleButtonImage());
        if (down_file != null) {
            if (isFileChanged(down_file.getName())) {
                Messagebox.show("File has been changed. Click save before download.");
                return;
            }
            Filedownload.save(down_file, null);
        }
    }

    @Listen("onClick = #whdNavigationToggleDeleteBtn")
    public void onNavigationToggleDeleteClick() throws Exception
    {
        String old_img = openedDesignProps.getNavigationToggleButtonImage();
        if (old_img.length() > 0) {
            updatePreviewFile((byte[]) null, webhelpCommonImagePath() + File.separator + old_img); // delete image file from preview folder (optional)
            addToRemovedFiles(old_img);
        }
        AImage blank_img = new AImage(getBlankFile_GIF());
        whdNavigationToggleImage.setContent(blank_img);
        whdNavigationToggleWidthBox.setValue(0);
        
        openedDesignProps.setNavigationToggleButtonImage("");
        openedDesignProps.setNavigationToggleButtonWidth("0px");
        designChanged();
        whdNavigationToggleDownloadBtn.setDisabled(true);
        whdNavigationToggleDeleteBtn.setDisabled(true);
    }

    @Listen("onChange = #whdNavigationToggleWidthBox; onChanging = #whdNavigationToggleWidthBox")
    public void onNavigationToggleWidthChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setNavigationToggleButtonWidth(getPixelInput(evt, whdNavigationToggleWidthBox));
            designChanged();
        }
    }

    //********************************************************************
    //*************   Book Part Settings Event Handler   *****************
    //********************************************************************
    
    @Listen("onChange = #whdPartTabsMarginTopBox; onChanging = #whdPartTabsMarginTopBox")
    public void onPartTabsMarginTopChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setPartTabsMarginTop(getPixelInput(evt, whdPartTabsMarginTopBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdPartTabsMarginLeftBox; onChanging = #whdPartTabsMarginLeftBox")
    public void onPartTabsMarginLeftChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setPartTabsMarginLeft(getPixelInput(evt, whdPartTabsMarginLeftBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdPartTabHeightBox; onChanging = #whdPartTabHeightBox")
    public void onPartTabHeightChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setPartTabHeight(getPixelInput(evt, whdPartTabHeightBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdPartTabSpacingBox; onChanging = #whdPartTabSpacingBox")
    public void onPartTabSpaceChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setPartTabSpacing(getPixelInput(evt, whdPartTabSpacingBox));
            designChanged();
        }
    }

    @Listen("onCheck = #whdPartTabHoverBox")
    public void onPartTabHoverClick()
    {
        openedDesignProps.setPartTabBgHoverEnabled(whdPartTabHoverBox.isChecked());
        designChanged();
    }

    @Listen("onCheck = #whdPartTabCurrentBox")
    public void onPartTabCurrentClick()
    {
        openedDesignProps.setPartTabBgCurrentEnabled(whdPartTabCurrentBox.isChecked());
        designChanged();
    }

    //*************************************************************************
    //*************   Breadcrumbs Settings Event Handler   ********************
    //*************************************************************************

    @Listen("onChange = #whdBreadcrumbsMarginTopBox; onChanging = #whdBreadcrumbsMarginTopBox")
    public void onBreadcrumbsMarginTopChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setBreadcrumbsMarginTop(getPixelInput(evt, whdBreadcrumbsMarginTopBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdBreadcrumbsMarginBottomBox; onChanging = #whdBreadcrumbsMarginBottomBox")
    public void onBreadcrumbsMarginBottomChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setBreadcrumbsMarginBottom(getPixelInput(evt, whdBreadcrumbsMarginBottomBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdBreadcrumbsMarginLeftBox; onChanging = #whdBreadcrumbsMarginLeftBox")
    public void onBreadcrumbsMarginLeftChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setBreadcrumbsMarginLeft(getPixelInput(evt, whdBreadcrumbsMarginLeftBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdBreadcrumbsMarginRightBox; onChanging = #whdBreadcrumbsMarginRightBox")
    public void onBreadcrumbsMarginRightChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setBreadcrumbsMarginRight(getPixelInput(evt, whdBreadcrumbsMarginRightBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdBreadcrumbsPaddingTopBox; onChanging = #whdBreadcrumbsPaddingTopBox")
    public void onBreadcrumbsPaddingTopChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setBreadcrumbsPaddingTop(getPixelInput(evt, whdBreadcrumbsPaddingTopBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdBreadcrumbsPaddingBottomBox; onChanging = #whdBreadcrumbsPaddingBottomBox")
    public void onBreadcrumbsPaddingBottomChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setBreadcrumbsPaddingBottom(getPixelInput(evt, whdBreadcrumbsPaddingBottomBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdBreadcrumbsPaddingLeftBox; onChanging = #whdBreadcrumbsPaddingLeftBox")
    public void onBreadcrumbsPaddingLeftChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setBreadcrumbsPaddingLeft(getPixelInput(evt, whdBreadcrumbsPaddingLeftBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdBreadcrumbsPaddingRightBox; onChanging = #whdBreadcrumbsPaddingRightBox")
    public void onBreadcrumbsPaddingRightChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setBreadcrumbsPaddingRight(getPixelInput(evt, whdBreadcrumbsPaddingRightBox));
            designChanged();
        }
    }

    @Listen("onUpload = #whdBreadcrumbsSepImageReplaceBtn")
    public void onBreadcrumbsSepImageReplaceClick(Event evt) throws Exception
    {
        UploadEvent uevt = (UploadEvent) evt;
        org.zkoss.util.media.Media media = uevt.getMedia();
        String fn = media.getName();
        String ext = (fn == null) ? null : WhdUtils.getFilenameExtension(fn);
        if ((ext == null) || ext.equals("")) {
            Messagebox.show("Invalid filename!");
            return;
        }
        if (media instanceof org.zkoss.image.Image) {
            String sep_fn = PREFIX_BREAD_SEP + System.currentTimeMillis() + "." + ext;
            File f_uploaded = writeImageToPreviewDirs(media, sep_fn);
            addToRemovedFiles(openedDesignProps.getBreadcrumbsSeparatorImage());
            addToChangedFiles(f_uploaded);

            int w = ((org.zkoss.image.Image) media).getWidth();
            whdBreadcrumbsSeparatorImage.setContent((org.zkoss.image.Image) media);
            whdBreadcrumbsSepImageDownloadBtn.setDisabled(false);
            whdBreadcrumbsSepImageDeleteBtn.setDisabled(false);
            whdBreadcrumbsSepImageWidthBox.setValue(w);
            openedDesignProps.setBreadcrumbsSeparatorImage(sep_fn);
            openedDesignProps.setBreadcrumbsSeparatorImageWidth(w + "px");
            designChanged();
        } else {
            Messagebox.show("Unknown image format!");
            return;
        }
    }

    @Listen("onClick = #whdBreadcrumbsSepImageDownloadBtn")
    public void onBreadcrumbsSepImageDownloadClick() throws Exception
    {
        File down_file = getSavedDesignFile(openedDesignName, openedDesignProps.getBreadcrumbsSeparatorImage());
        if (down_file != null) {
            if (isFileChanged(down_file.getName())) {
                Messagebox.show("File has been changed. Click save before download.");
                return;
            }
            Filedownload.save(down_file, null);
        }
    }

    @Listen("onClick = #whdBreadcrumbsSepImageDeleteBtn")
    public void onBreadcrumbsSepImageDeleteClick() throws Exception
    {
        String old_img = openedDesignProps.getBreadcrumbsSeparatorImage();
        if (old_img.length() > 0) {
            updatePreviewFile((byte[]) null, webhelpCommonImagePath() + File.separator + old_img); // delete image file from preview folder (optional)
            addToRemovedFiles(old_img);
        }
        AImage blank_img = new AImage(getBlankFile_GIF());
        whdBreadcrumbsSeparatorImage.setContent(blank_img);
        whdBreadcrumbsSepImageWidthBox.setValue(0);
        
        openedDesignProps.setBreadcrumbsSeparatorImage("");
        openedDesignProps.setBreadcrumbsSeparatorImageWidth("0px");
        designChanged();
        whdBreadcrumbsSepImageDownloadBtn.setDisabled(true);
        whdBreadcrumbsSepImageDeleteBtn.setDisabled(true);
    }

    @Listen("onChange = #whdBreadcrumbsSepImageWidthBox; onChanging = #whdBreadcrumbsSepImageWidthBox")
    public void onBreadcrumbsSepImageWidthChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setBreadcrumbsSeparatorImageWidth(getPixelInput(evt, whdBreadcrumbsSepImageWidthBox));
            designChanged();
        }
    }

    //*************************************************************************
    //****************   Local Navigation Event Handler   *********************
    //*************************************************************************
    
    @Listen("onChange = #whdNavLocalHeightBox; onChanging = #whdNavLocalHeightBox")
    public void onNavLocalHeightChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setLocalNavigationHeight(getPixelInputOrEmpty(evt, whdNavLocalHeightBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdNavLocalMarginTopBox; onChanging = #whdNavLocalMarginTopBox")
    public void onNavLocalMarginTopChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setLocalNavigationMarginTop(getPixelInput(evt, whdNavLocalMarginTopBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdNavLocalPaddingTopBox; onChanging = #whdNavLocalPaddingTopBox")
    public void onNavLocalPaddingTopChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setLocalNavigationPaddingTop(getPixelInput(evt, whdNavLocalPaddingTopBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdNavLocalPaddingBottomBox; onChanging = #whdNavLocalPaddingBottomBox")
    public void onNavLocalPaddingBottomChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setLocalNavigationPaddingBottom(getPixelInput(evt, whdNavLocalPaddingBottomBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdNavLocalPaddingLeftBox; onChanging = #whdNavLocalPaddingLeftBox")
    public void onNavLocalPaddingLeftChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setLocalNavigationPaddingLeft(getPixelInput(evt, whdNavLocalPaddingLeftBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdNavLocalPaddingRightBox; onChanging = #whdNavLocalPaddingRightBox")
    public void onNavLocalPaddingRightChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setLocalNavigationPaddingRight(getPixelInput(evt, whdNavLocalPaddingRightBox));
            designChanged();
        }
    }

    @Listen("onCheck = #whdNavLocalShowTextBox")
    public void onNavLocalShowTextClick()
    {
        boolean is_text_visible = whdNavLocalShowTextBox.isChecked();
        if ((! is_text_visible) && (! whdNavLocalShowIconsBox.isChecked())) {
            whdNavLocalShowIconsBox.setChecked(true);
            openedDesignProps.setLocalNavigationIconsVisible(true);
        }
        openedDesignProps.setLocalNavigationTextVisible(is_text_visible);
        designChanged();
    }

    @Listen("onCheck = #whdNavLocalShowIconsBox")
    public void onNavLocalShowIconsClick()
    {
        boolean is_icons_visible = whdNavLocalShowIconsBox.isChecked();
        if ((! is_icons_visible) && (! whdNavLocalShowTextBox.isChecked())) {
            whdNavLocalShowTextBox.setChecked(true);
            openedDesignProps.setLocalNavigationTextVisible(true);
        }
        openedDesignProps.setLocalNavigationIconsVisible(is_icons_visible);
        designChanged();
    }

    @Listen("onCheck = #whdNavSeparatorEnabledBox")
    public void onNavSeparatorCheckedClick()
    {
        openedDesignProps.setLocalNavigationSeparatorVisible(whdNavSeparatorEnabledBox.isChecked());
        designChanged();
    }

    @Listen("onChange = #whdNavLocalIconsHeightBox; onChanging = #whdNavLocalIconsHeightBox")
    public void onNavLocalIconHeightChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setLocalNavigationIconsHeight(getPixelInput(evt, whdNavLocalIconsHeightBox));
            designChanged();
        }
    }

    @Listen("onSelect = #whdNavLocalIconSelectBox")
    public void onNavLocalIconSelected()
    {
        Listitem sel_item = whdNavLocalIconSelectBox.getSelectedItem();
        String sel = (sel_item == null) ? "" : sel_item.getValue().toString();
        whdNavLocalPrevImage.setSclass(sel.equals("prev") ? "img_selected" : "");
        whdNavLocalNextImage.setSclass(sel.equals("next") ? "img_selected" : "");
        whdNavLocalUpImage.setSclass(sel.equals("up") ? "img_selected" : "");
        whdNavLocalHomeImage.setSclass(sel.equals("home") ? "img_selected" : "");
        whdNavLocalTocImage.setSclass(sel.equals("toc") ? "img_selected" : "");
    }

    @Listen("onClick = #whdNavLocalIconDownloadBtn")
    public void onNavLocalIconDownloadClick() throws Exception
    {
        Listitem sel_item = whdNavLocalIconSelectBox.getSelectedItem();
        String sel = (sel_item == null) ? "" : sel_item.getValue().toString();
        File down_file = null; 
        if (sel.equals("prev")) {
            down_file = getSavedPrevIcon(openedDesignName);
        } else if (sel.equals("next")) {
            down_file = getSavedNextIcon(openedDesignName);
        } else if (sel.equals("up")) {
            down_file = getSavedUpIcon(openedDesignName);
        } else if (sel.equals("home")) {
            down_file = getSavedHomeIcon(openedDesignName);
        } else if (sel.equals("toc")) {
            down_file = getSavedTocIcon(openedDesignName);
        }
        if (down_file != null) {
            if (isFileChanged(down_file.getName())) {
                Messagebox.show("File has been changed. Click save before download.");
                return;
            }
            Filedownload.save(down_file, null);
        } else {
            Messagebox.show("Icon does not exist.");
            return;
        }
    }

    @Listen("onUpload = #whdNavLocalIconReplaceBtn")
    public void onNavLocalIconReplaceClick(Event evt) throws Exception
    {
        UploadEvent uevt = (UploadEvent) evt;
        org.zkoss.util.media.Media[] medias = uevt.getMedias();
        if (medias.length == 0) {
            return;
        }
        Map<String, org.zkoss.image.Image> media_map = new HashMap<String, org.zkoss.image.Image>();
        for (org.zkoss.util.media.Media media : medias) {
            if (! (media instanceof org.zkoss.image.Image)) {
                continue;
            }
            org.zkoss.image.Image img = (org.zkoss.image.Image) media;
            String n = img.getName().toLowerCase();
            String ext = WhdUtils.getFilenameExtension(n);
            if ((ext == null) || ext.equals("")) {
                Messagebox.show("Invalid file extension!");
                return;
            }
            if (! ext.equalsIgnoreCase("png")) {
                Messagebox.show("Icons must have file extension .png!");
                return;
            }
            if (n.startsWith(PREFIX_LOCAL_NAV_PREV)) {
                media_map.put(PREFIX_LOCAL_NAV_PREV, img);
            } else if (n.startsWith(PREFIX_LOCAL_NAV_NEXT)) {
                media_map.put(PREFIX_LOCAL_NAV_NEXT, img);
            } else if (n.startsWith(PREFIX_LOCAL_NAV_UP)) {
                media_map.put(PREFIX_LOCAL_NAV_UP, img);
            } else if (n.startsWith(PREFIX_LOCAL_NAV_HOME)) {
                media_map.put(PREFIX_LOCAL_NAV_HOME, img);
            } else if (n.startsWith(PREFIX_LOCAL_NAV_TOC)) {
                media_map.put(PREFIX_LOCAL_NAV_TOC, img);
            } else {
                Messagebox.show("Invalid filename. Allowed filename prefixes are: prev, next, up, home, toc");
                return;
            }
        }
        Iterator<String> it = media_map.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            String filename = key + ".png";
            org.zkoss.image.Image media = media_map.get(key);
            File f_uploaded = writeImageToPreviewDirs(media, filename);
            addToChangedFiles(f_uploaded);
            
            if (key.equals(PREFIX_LOCAL_NAV_PREV)) {
                whdNavLocalPrevImage.setContent(media);
            } else if (key.equals(PREFIX_LOCAL_NAV_NEXT)) {
                whdNavLocalNextImage.setContent(media);
            } else if (key.equals(PREFIX_LOCAL_NAV_UP)) {
                whdNavLocalUpImage.setContent(media);
            } else if (key.equals(PREFIX_LOCAL_NAV_HOME)) {
                whdNavLocalHomeImage.setContent(media);
            } else if (key.equals(PREFIX_LOCAL_NAV_TOC)) {
                whdNavLocalTocImage.setContent(media);
            }
        }
        designChanged();
    }


    //*************************************************************************
    //****************   Search Settings Event Handler   **********************
    //*************************************************************************

    @Listen("onSelect = #whdSearchUIModeBox")
    public void onSearchUIModeSelected()
    {
        Listitem sel_item = whdSearchUIModeBox.getSelectedItem();
        String mode = (sel_item == null) ? "tab" : sel_item.getValue().toString();
        openedDesignProps.setSearchUIMode(mode);
        boolean is_fixed = mode.equalsIgnoreCase("fixed");
        updateSearchModeVisibility(mode);
        if (is_fixed) {
            if (! WhdUtils.selectListItem(whdSearchPositionBox, openedDesignProps.getSearchPosition())) {
                openedDesignProps.setSearchPosition("fixed-bottom-left");
                WhdUtils.selectListItem(whdSearchPositionBox, openedDesignProps.getSearchPosition());
            }
        }
        designChanged();
    }
    
    @Listen("onSelect = #whdSearchPositionBox")
    public void onSearchPositionSelected()
    {
        Listitem sel_item = whdSearchPositionBox.getSelectedItem();
        String sel = (sel_item == null) ? "fixed-top-right" : sel_item.getValue().toString();
        openedDesignProps.setSearchPosition(sel);
        designChanged();
    }

    @Listen("onChange = #whdSearchWidthBox; onChanging = #whdSearchWidthBox")
    public void onSearchWidthChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setSearchWidth(getPixelInputOrEmpty(evt, whdSearchWidthBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdSearchHeightBox; onChanging = #whdSearchHeightBox")
    public void onSearchHeightChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setSearchHeight(getPixelInput(evt, whdSearchHeightBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdSearchXPosBox; onChanging = #whdSearchXPosBox")
    public void onSearchXPosChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setSearchPosX(getPixelInput(evt, whdSearchXPosBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdSearchYPosBox; onChanging = #whdSearchYPosBox")
    public void onSearchYPosChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setSearchPosY(getPixelInput(evt, whdSearchYPosBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdSearchInnerTopOffsetBox; onChanging = #whdSearchInnerTopOffsetBox")
    public void onSearchInnerTopOffsetChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setSearchInnerTopOffset(getPixelInput(evt, whdSearchInnerTopOffsetBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdSearchInnerLeftOffsetBox; onChanging = #whdSearchInnerLeftOffsetBox")
    public void onSearchInnerLeftOffsetChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setSearchInnerLeftOffset(getPixelInput(evt, whdSearchInnerLeftOffsetBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdSearchInnerRightOffsetBox; onChanging = #whdSearchInnerRightOffsetBox")
    public void onSearchInnerRightOffsetChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setSearchInnerRightOffset(getPixelInput(evt, whdSearchInnerRightOffsetBox));
            designChanged();
        }
    }

    @Listen("onCheck = #whdSearchBtnShowTextBox")
    public void onSearchButtonTextVisibleClick()
    {
        openedDesignProps.setSearchButtonTextVisible(whdSearchBtnShowTextBox.isChecked());
        updateSearchControlPreviews();
        designChanged();
    }

    @Listen("onChange = #whdSearchBtnWidthBox; onChanging = #whdSearchBtnWidthBox")
    public void onSearchButtonWidthChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setSearchButtonWidth(getPixelInputOrEmpty(evt, whdSearchBtnWidthBox));
            updateSearchControlPreviews();
            designChanged();
        }
    }

    @Listen("onChange = #whdSearchBtnHeightBox; onChanging = #whdSearchBtnHeightBox")
    public void onSearchButtonHeightChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setSearchButtonHeight(getPixelInputOrEmpty(evt, whdSearchBtnHeightBox));
            updateSearchControlPreviews();
            designChanged();
        }
    }


    @Listen("onUpload = #whdSearchToggleReplaceBtn")
    public void onSearchToggleReplaceClick(Event evt) throws Exception
    {
        UploadEvent uevt = (UploadEvent) evt;
        org.zkoss.util.media.Media media = uevt.getMedia();
        String fn = media.getName();
        String ext = (fn == null) ? null : WhdUtils.getFilenameExtension(fn);
        if ((ext == null) || ! ext.equalsIgnoreCase("gif")) {
            Messagebox.show("Image must have file extension .gif!");
            return;
        }
        if (media instanceof org.zkoss.image.Image) {
            String highlight_fn = PREFIX_SEARCH_HIGHLIGHT_BUTTON + System.currentTimeMillis() + ".gif";
            File f_uploaded = writeImageToPreviewDirs(media, DEPLOY_FILENAME_SEARCH_HIGHLIGHT_BUTTON);
            addToRemovedFiles(openedDesignProps.getSearchToggleButtonImage());
            addToChangedFiles(f_uploaded);

            int w = ((org.zkoss.image.Image) media).getWidth();
            whdSearchToggleImage.setContent((org.zkoss.image.Image) media);
            whdSearchToggleWidthBox.setValue(w);
            openedDesignProps.setSearchToggleButtonImage(highlight_fn);
            openedDesignProps.setSearchToggleButtonWidth(w + "px");
            designChanged();
        } else {
            Messagebox.show("Unknown image format!");
            return;
        }
    }

    @Listen("onClick = #whdSearchToggleDownloadBtn")
    public void onSearchToggleDownloadClick() throws Exception
    {
        File down_file = getSavedDesignFile(openedDesignName, openedDesignProps.getSearchToggleButtonImage());
        if (down_file != null) {
            if (isFileChanged(down_file.getName())) {
                Messagebox.show("File has been changed. Click save before download.");
                return;
            }
            Filedownload.save(down_file, null);
        }
    }

    @Listen("onChange = #whdSearchToggleWidthBox; onChanging = #whdSearchToggleWidthBox")
    public void onSearchToggleWidthChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setSearchToggleButtonWidth(getPixelInput(evt, whdSearchToggleWidthBox));
            designChanged();
        }
    }

    @Listen("onUpload = #whdSearchCloseImageReplaceBtn")
    public void onSearchCloseImageReplaceClick(Event evt) throws Exception
    {
        UploadEvent uevt = (UploadEvent) evt;
        org.zkoss.util.media.Media media = uevt.getMedia();
        String fn = media.getName();
        String ext = (fn == null) ? null : WhdUtils.getFilenameExtension(fn);
        if ((ext == null) || ext.equals("")) {
            Messagebox.show("Invalid image filename: Missing extension!");
            return;
        }
        if (media instanceof org.zkoss.image.Image) {
            String sclose_fn = PREFIX_SEARCH_CLOSE_ICON + System.currentTimeMillis() + ".gif";
            File f_uploaded = writeImageToPreviewDirs(media, sclose_fn);
            addToRemovedFiles(openedDesignProps.getSearchCloseImage());
            addToChangedFiles(f_uploaded);

            whdSearchCloseImage.setContent((org.zkoss.image.Image) media);
            openedDesignProps.setSearchCloseImage(sclose_fn);
            designChanged();
        } else {
            Messagebox.show("Unknown image format!");
            return;
        }
    }

    @Listen("onClick = #whdSearchCloseImageDownloadBtn")
    public void onSearchCloseImageDownloadClick() throws Exception
    {
        File down_file = getSavedDesignFile(openedDesignName, openedDesignProps.getSearchCloseImage());
        if (down_file != null) {
            if (isFileChanged(down_file.getName())) {
                Messagebox.show("File has been changed. Click save before download.");
                return;
            }
            Filedownload.save(down_file, null);
        }
    }


    //*************************************************************************
    //*****************   Search Tab Event Handler   **************************
    //*************************************************************************

    @Listen("onChange = #whdSearchTabHeightBox; onChanging = #whdSearchTabHeightBox")
    public void onSearchTabHeightChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setSearchTabHeight(getPixelInput(evt, whdSearchTabHeightBox));
            designChanged();
        }
    }

    @Listen("onCheck = #whdSearchTabHoverBox")
    public void onSearchTabHoverClick()
    {
        openedDesignProps.setSearchTabBgHoverEnabled(whdSearchTabHoverBox.isChecked());
        designChanged();
    }

    @Listen("onChange = #whdSearchPanelPaddingTopBox; onChanging = #whdSearchPanelPaddingTopBox")
    public void onSearchPanelPaddingTopChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setSearchPanelPaddingTop(getPixelInput(evt, whdSearchPanelPaddingTopBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdSearchPanelPaddingBottomBox; onChanging = #whdSearchPanelPaddingBottomBox")
    public void onSearchPanelPaddingBottomChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setSearchPanelPaddingBottom(getPixelInput(evt, whdSearchPanelPaddingBottomBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdSearchPanelPaddingLeftBox; onChanging = #whdSearchPanelPaddingLeftBox")
    public void onSearchPanelPaddingLeftChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setSearchPanelPaddingLeft(getPixelInput(evt, whdSearchPanelPaddingLeftBox));
            designChanged();
        }
    }

    @Listen("onChange = #whdSearchPanelPaddingRightBox; onChanging = #whdSearchPanelPaddingRightBox")
    public void onSearchPanelPaddingRightChange(Event evt)
    {
        if (evt instanceof InputEvent) {
            openedDesignProps.setSearchPanelPaddingRight(getPixelInput(evt, whdSearchPanelPaddingRightBox));
            designChanged();
        }
    }

    //*************************************************************************
    //******************   Edit Font Event Handler   **************************
    //*************************************************************************
    
    @Listen("onClick = #whdHeaderTitle1FontBtn; onClick = #whdHeaderTitle1FontPrev; onDoubleClick = #whdHeaderTitle1FontPrev")
    public void onHeaderTitle1FontClick()
    {
        initEditFontDialog(openedDesignProps.getHeaderTitle1FontCSS());
        fontWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setHeaderTitle1FontCSS(getEditFontDialogCSS());
                // Refresh preview
                updateHeaderControlPreviews();
                designChanged();
            }
        });
        whdFontWin.doHighlighted();
    }

    @Listen("onClick = #whdHeaderTitle2FontBtn; onClick = #whdHeaderTitle2FontPrev; onDoubleClick = #whdHeaderTitle2FontPrev")
    public void onHeaderTitle2FontClick()
    {
        initEditFontDialog(openedDesignProps.getHeaderTitle2FontCSS());
        fontWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setHeaderTitle2FontCSS(getEditFontDialogCSS());
                // Refresh preview
                updateHeaderControlPreviews();
                designChanged();
            }
        });
        whdFontWin.doHighlighted();
    }

    @Listen("onClick = #whdNavigationFontBtn; onClick = #whdNavigationFontPrev; onDoubleClick = #whdNavigationFontPrev")
    public void onNavigationFontClick()
    {
        initEditFontDialog(openedDesignProps.getNavigationTreeFontCSS());
        fontWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setNavigationTreeFontCSS(getEditFontDialogCSS());
                // Refresh preview
                updateNavigationControls();
                designChanged();
            }
        });
        whdFontWin.doHighlighted();
    }

    @Listen("onClick = #whdNavigationFontHoverBtn; onClick = #whdNavigationFontHoverPrev; onDoubleClick = #whdNavigationFontHoverPrev")
    public void onNavigationFontHoverClick()
    {
        initEditFontDialog(openedDesignProps.getNavigationTreeFontHoverCSS());
        fontWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setNavigationTreeFontHoverCSS(getEditFontDialogCSS());
                // Refresh preview
                updateNavigationControls();
                designChanged();
            }
        });
        whdFontWin.doHighlighted();
    }

    @Listen("onClick = #whdNavigationFontCurrentBtn; onClick = #whdNavigationFontCurrentPrev; onDoubleClick = #whdNavigationFontCurrentPrev")
    public void onNavigationFontCurrentClick()
    {
        initEditFontDialog(openedDesignProps.getNavigationTreeFontCurrentCSS());
        fontWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setNavigationTreeFontCurrentCSS(getEditFontDialogCSS());
                // Refresh preview
                updateNavigationControls();
                designChanged();
            }
        });
        whdFontWin.doHighlighted();
    }

    @Listen("onClick = #whdNavigationFontTitleBtn; onClick = #whdNavigationFontTitlePrev; onDoubleClick = #whdNavigationFontTitlePrev")
    public void onNavigationFontTitleClick()
    {
        initEditFontDialog(openedDesignProps.getNavigationTreeFontTitleCSS());
        fontWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setNavigationTreeFontTitleCSS(getEditFontDialogCSS());
                // Refresh preview
                updateNavigationControls();
                designChanged();
            }
        });
        whdFontWin.doHighlighted();
    }

    @Listen("onClick = #whdNavAccordionFontBtn; onClick = #whdNavAccordionFontPrev; onDoubleClick = #whdNavAccordionFontPrev")
    public void onAccordionFontClick()
    {
        if (! whdNavAccordionCheckbox.isChecked()) {
            return;
        } 
        initEditFontDialog(openedDesignProps.getAccordionFontCSS());
        fontWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setAccordionFontCSS(getEditFontDialogCSS());
                // Refresh preview
                updateNavigationControls();
                designChanged();
            }
        });
        whdFontWin.doHighlighted();
    }

    @Listen("onClick = #whdPartTabFontBtn; onClick = #whdPartTabFontPrev; onDoubleClick = #whdPartTabFontPrev")
    public void onPartTabFontClick()
    {
        initEditFontDialog(openedDesignProps.getPartTabFontCSS());
        fontWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setPartTabFontCSS(getEditFontDialogCSS());
                // Refresh preview
                updatePartControlPreviews();
                designChanged();
            }
        });
        whdFontWin.doHighlighted();
    }

    @Listen("onClick = #whdPartTabFontHoverBtn; onClick = #whdPartTabFontHoverPrev; onDoubleClick = #whdPartTabFontHoverPrev")
    public void onPartTabFontHoverClick()
    {
        initEditFontDialog(openedDesignProps.getPartTabFontCSSHover());
        fontWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setPartTabFontCSSHover(getEditFontDialogCSS());
                // Refresh preview
                updatePartControlPreviews();
                designChanged();
            }
        });
        whdFontWin.doHighlighted();
    }

    @Listen("onClick = #whdPartTabFontCurrentBtn; onClick = #whdPartTabFontCurrentPrev; onDoubleClick = #whdPartTabFontCurrentPrev")
    public void onPartTabFontCurrentClick()
    {
        initEditFontDialog(openedDesignProps.getPartTabFontCSSCurrent());
        fontWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setPartTabFontCSSCurrent(getEditFontDialogCSS());
                // Refresh preview
                updatePartControlPreviews();
                designChanged();
            }
        });
        whdFontWin.doHighlighted();
    }

    @Listen("onClick = #whdNavLocalFontBtn; onClick = #whdNavLocalFontPrev; onDoubleClick = #whdNavLocalFontPrev")
    public void onLocalNavigationFontClick()
    {
        initEditFontDialog(openedDesignProps.getLocalNavigationLinkCSS());
        fontWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setLocalNavigationLinkCSS(getEditFontDialogCSS());
                // Refresh preview
                updateLocalNavigationControlPreviews();
                designChanged();
            }
        });
        whdFontWin.doHighlighted();
    }

    @Listen("onClick = #whdNavLocalFontHoverBtn; onClick = #whdNavLocalFontHoverPrev; onDoubleClick = #whdNavLocalFontHoverPrev")
    public void onLocalNavigationFontHoverClick()
    {
        initEditFontDialog(openedDesignProps.getLocalNavigationLinkHoverCSS());
        fontWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setLocalNavigationLinkHoverCSS(getEditFontDialogCSS());
                // Refresh preview
                updateLocalNavigationControlPreviews();
                designChanged();
            }
        });
        whdFontWin.doHighlighted();
    }

    @Listen("onClick = #whdNavSeparatorFontBtn; onClick = #whdNavSeparatorFontPrev; onDoubleClick = #whdNavSeparatorFontPrev")
    public void onLocalNavigationSeparatorClick()
    {
        initEditFontDialog(openedDesignProps.getLocalNavigationSeparatorCSS());
        fontWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setLocalNavigationSeparatorCSS(getEditFontDialogCSS());
                // Refresh preview
                updateLocalNavigationControlPreviews();
                designChanged();
            }
        });
        whdFontWin.doHighlighted();
    }

    @Listen("onClick = #whdBreadcrumbsFontBtn; onClick = #whdBreadcrumbsFontPrev; onDoubleClick = #whdBreadcrumbsFontPrev")
    public void onBreadcrumbsFontClick()
    {
        initEditFontDialog(openedDesignProps.getBreadcrumbsLinkCSS());
        fontWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setBreadcrumbsLinkCSS(getEditFontDialogCSS());
                // Refresh preview
                updateBreadcrumbsControlPreviews();
                designChanged();
            }
        });
        whdFontWin.doHighlighted();
    }

    @Listen("onClick = #whdBreadcrumbsFontLastBtn; onClick = #whdBreadcrumbsFontLastPrev; onDoubleClick = #whdBreadcrumbsFontLastPrev")
    public void onBreadcrumbsFontLastClick()
    {
        initEditFontDialog(openedDesignProps.getBreadcrumbsLastCSS());
        fontWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setBreadcrumbsLastCSS(getEditFontDialogCSS());
                // Refresh preview
                updateBreadcrumbsControlPreviews();
                designChanged();
            }
        });
        whdFontWin.doHighlighted();
    }

    @Listen("onClick = #whdBreadcrumbsFontHoverBtn; onClick = #whdBreadcrumbsFontHoverPrev; onDoubleClick = #whdBreadcrumbsFontHoverPrev")
    public void onBreadcrumbsFontHoverClick()
    {
        initEditFontDialog(openedDesignProps.getBreadcrumbsHoverCSS());
        fontWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setBreadcrumbsHoverCSS(getEditFontDialogCSS());
                // Refresh preview
                updateBreadcrumbsControlPreviews();
                designChanged();
            }
        });
        whdFontWin.doHighlighted();
    }

    @Listen("onClick = #whdBreadcrumbsFontSepBtn; onClick = #whdBreadcrumbsFontSepPrev; onDoubleClick = #whdBreadcrumbsFontSepPrev")
    public void onBreadcrumbsFontSepClick()
    {
        initEditFontDialog(openedDesignProps.getBreadcrumbsSeparatorCSS());
        fontWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setBreadcrumbsSeparatorCSS(getEditFontDialogCSS());
                // Refresh preview
                updateBreadcrumbsControlPreviews();
                designChanged();
            }
        });
        whdFontWin.doHighlighted();
    }

    @Listen("onClick = #whdSearchLegendFontBtn; onClick = #whdSearchLegendFontPrev; onDoubleClick = #whdSearchLegendFontPrev")
    public void onSearchLegendFontClick()
    {
        initEditFontDialog(openedDesignProps.getSearchLegendFontCSS());
        fontWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setSearchLegendFontCSS(getEditFontDialogCSS());
                // Refresh preview
                updateSearchControlPreviews();
                designChanged();
            }
        });
        whdFontWin.doHighlighted();
    }

    @Listen("onClick = #whdSearchInputFontBtn; onClick = #whdSearchInputFontPrev; onDoubleClick = #whdSearchInputFontPrev")
    public void onSearchInputFontClick()
    {
        initEditFontDialog(openedDesignProps.getSearchInputFontCSS());
        fontWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setSearchInputFontCSS(getEditFontDialogCSS());
                // Refresh preview
                updateSearchControlPreviews();
                designChanged();
            }
        });
        whdFontWin.doHighlighted();
    }

    @Listen("onClick = #whdSearchBtnFontBtn")
    public void onSearchButtonFontClick()
    {
        initEditFontDialog(openedDesignProps.getSearchButtonFontCSS());
        fontWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setSearchButtonFontCSS(getEditFontDialogCSS());
                // Refresh preview
                updateSearchControlPreviews();
                designChanged();
            }
        });
        whdFontWin.doHighlighted();
    }

    @Listen("onClick = #whdSearchTitleFontBtn; onClick = #whdSearchTitleFontPrev; onDoubleClick = #whdSearchTitleFontPrev")
    public void onSearchTitleFontClick()
    {
        initEditFontDialog(openedDesignProps.getSearchTitleFontCSS());
        fontWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setSearchTitleFontCSS(getEditFontDialogCSS());
                // Refresh preview
                updateSearchControlPreviews();
                designChanged();
            }
        });
        whdFontWin.doHighlighted();
    }

    @Listen("onClick = #whdSearchExpressionFontBtn; onClick = #whdSearchExpressionFontPrev; onDoubleClick = #whdSearchExpressionFontPrev")
    public void onSearchExpressionFontClick()
    {
        initEditFontDialog(openedDesignProps.getSearchExpressionFontCSS());
        fontWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setSearchExpressionFontCSS(getEditFontDialogCSS());
                // Refresh preview
                updateSearchControlPreviews();
                designChanged();
            }
        });
        whdFontWin.doHighlighted();
    }

    @Listen("onClick = #whdSearchResultFontBtn; onClick = #whdSearchResultFontPrev; onDoubleClick = #whdSearchResultFontPrev")
    public void onSearchResultFontClick()
    {
        initEditFontDialog(openedDesignProps.getSearchResultFontCSS());
        fontWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setSearchResultFontCSS(getEditFontDialogCSS());
                // Refresh preview
                updateSearchControlPreviews();
                designChanged();
            }
        });
        whdFontWin.doHighlighted();
    }

    @Listen("onClick = #whdSearchHitFontBtn; onClick = #whdSearchHitFontPrev; onDoubleClick = #whdSearchHitFontPrev")
    public void onSearchHitFontClick()
    {
        initEditFontDialog(openedDesignProps.getSearchHitFontCSS());
        fontWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setSearchHitFontCSS(getEditFontDialogCSS());
                // Refresh preview
                updateSearchControlPreviews();
                designChanged();
            }
        });
        whdFontWin.doHighlighted();
    }

    @Listen("onClick = #whdSearchCloseFontBtn; onClick = #whdSearchCloseFontPrev; onDoubleClick = #whdSearchCloseFontPrev")
    public void onSearchCloseFontClick()
    {
        initEditFontDialog(openedDesignProps.getSearchCloseFontCSS());
        fontWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setSearchCloseFontCSS(getEditFontDialogCSS());
                // Refresh preview
                updateSearchControlPreviews();
                designChanged();
            }
        });
        whdFontWin.doHighlighted();
    }

    @Listen("onClick = #whdSearchTabFontBtn; onClick = #whdSearchTabFontPrev; onDoubleClick = #whdSearchTabFontPrev")
    public void onSearchTabFontClick()
    {
        initEditFontDialog(openedDesignProps.getSearchTabFontCSS());
        fontWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setSearchTabFontCSS(getEditFontDialogCSS());
                // Refresh preview
                updateSearchTabControlPreviews();
                designChanged();
            }
        });
        whdFontWin.doHighlighted();
    }

    @Listen("onClick = #whdSearchTabFontHoverBtn; onClick = #whdSearchTabFontHoverPrev; onDoubleClick = #whdSearchTabFontHoverPrev")
    public void onSearchTabFontHoverClick()
    {
        initEditFontDialog(openedDesignProps.getSearchTabFontCSSHover());
        fontWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setSearchTabFontCSSHover(getEditFontDialogCSS());
                // Refresh preview
                updateSearchTabControlPreviews();
                designChanged();
            }
        });
        whdFontWin.doHighlighted();
    }

    @Listen("onClick = #whdSearchTabFontActiveBtn; onClick = #whdSearchTabFontActivePrev; onDoubleClick = #whdSearchTabFontActivePrev")
    public void onSearchTabFontActiveClick()
    {
        initEditFontDialog(openedDesignProps.getSearchTabFontCSSActive());
        fontWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setSearchTabFontCSSActive(getEditFontDialogCSS());
                // Refresh preview
                updateSearchTabControlPreviews();
                designChanged();
            }
        });
        whdFontWin.doHighlighted();
    }


    //*************************************************************************
    //**************   Edit Border/Background Event Handler   *****************
    //*************************************************************************
    
    @Listen("onClick = #whdHeaderBgBtn; onClick = #whdHeaderBgPrev; onDoubleClick = #whdHeaderBgPrev")
    public void onHeaderBorderBgClick()
    {
        initEditBorderDialog(getHeaderBgPreviewCSS());
        borderWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setHeaderBorderCSS(borderWinComposer.getBorderCSS());
                openedDesignProps.setHeaderBgColor(borderWinComposer.getBgColor());
                
                if (borderWinComposer.hasBackgroundImage()) {
                    File editBorderBgImageUploaded = borderWinComposer.getUploadedBackgroundImageFile();
                    if (editBorderBgImageUploaded != null) {  // background image has been added or changed
                        String temp_fn = editBorderBgImageUploaded.getName();
                        String ext = WhdUtils.getFilenameExtension(temp_fn);
                        String fn = PREFIX_HEAD_BG + System.currentTimeMillis() + "." + ext;
                        File bg_img = updatePreviewFile(editBorderBgImageUploaded, webhelpCommonImagePath() + File.separator + fn);
                        addToRemovedFiles(openedDesignProps.getHeaderBgImage());
                        addToChangedFiles(bg_img);
                        openedDesignProps.setHeaderBgImage(bg_img.getName());
                    }
                    openedDesignProps.setHeaderBgRepeat(borderWinComposer.getBgRepeat());
                } else {
                    addToRemovedFiles(openedDesignProps.getHeaderBgImage());
                    openedDesignProps.setHeaderBgImage(null);
                }

                // Refresh preview
                updateHeaderControlPreviews(); // whdHeaderBgPrev.setStyle(getHeaderBgPreviewCSS());
                designChanged();
            }
        });
        whdBorderWin.doHighlighted();
    }

    @Listen("onClick = #whdNavigationBgBtn; onClick = #whdNavigationBgPrev; onDoubleClick = #whdNavigationBgPrev")
    public void onNavigationBgClick()
    {
        initEditBorderDialog(getNavigationBgPreviewCSS());
        borderWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setNavigationBorderCSS(borderWinComposer.getBorderCSS());
                openedDesignProps.setNavigationBgColor(borderWinComposer.getBgColor());
                
                if (borderWinComposer.hasBackgroundImage()) {
                    File editBorderBgImageUploaded = borderWinComposer.getUploadedBackgroundImageFile();
                    if (editBorderBgImageUploaded != null) {  // background image has been added or changed
                        String temp_fn = editBorderBgImageUploaded.getName();
                        String ext = WhdUtils.getFilenameExtension(temp_fn);
                        String fn = PREFIX_NAV_BG + System.currentTimeMillis() + "." + ext;
                        File bg_img = updatePreviewFile(editBorderBgImageUploaded, webhelpCommonImagePath() + File.separator + fn);
                        addToRemovedFiles(openedDesignProps.getNavigationBgImage());
                        addToChangedFiles(bg_img);
                        openedDesignProps.setNavigationBgImage(bg_img.getName());
                    }
                    openedDesignProps.setNavigationBgRepeat(borderWinComposer.getBgRepeat());
                } else {
                    addToRemovedFiles(openedDesignProps.getNavigationBgImage());
                    openedDesignProps.setNavigationBgImage(null);
                }

                // Refresh preview
                updateNavigationControls(); // whdNavigationBgPrev.setStyle(getNavigationBorderBgCSS());
                designChanged();
            }
        });
        whdBorderWin.doHighlighted();
    }

    @Listen("onClick = #whdNavAccordionBgBtn; onClick = #whdNavAccordionBgPrev; onDoubleClick = #whdNavAccordionBgPrev")
    public void onAccordionBgClick()
    {
        if (! whdNavAccordionCheckbox.isChecked()) {
            return;
        }
        initEditBorderDialog(getAccordionBgPreviewCSS());
        borderWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setAccordionBorderCSS(borderWinComposer.getBorderCSS());
                openedDesignProps.setAccordionBgColor(borderWinComposer.getBgColor());
                
                if (borderWinComposer.hasBackgroundImage()) {
                    File editBorderBgImageUploaded = borderWinComposer.getUploadedBackgroundImageFile();
                    if (editBorderBgImageUploaded != null) {  // background image has been added or changed
                        String temp_fn = editBorderBgImageUploaded.getName();
                        String ext = WhdUtils.getFilenameExtension(temp_fn);
                        String fn = PREFIX_NAV_ACCORDION_BG + System.currentTimeMillis() + "." + ext;
                        File bg_img = updatePreviewFile(editBorderBgImageUploaded, webhelpCommonImagePath() + File.separator + fn);
                        addToRemovedFiles(openedDesignProps.getAccordionBgImage());
                        addToChangedFiles(bg_img);
                        openedDesignProps.setAccordionBgImage(bg_img.getName());
                    }
                    openedDesignProps.setAccordionBgRepeat(borderWinComposer.getBgRepeat());
                } else {
                    addToRemovedFiles(openedDesignProps.getAccordionBgImage());
                    openedDesignProps.setAccordionBgImage(null);
                }

                // Refresh preview
                updateNavigationControls();
                designChanged();
            }
        });
        whdBorderWin.doHighlighted();
    }

    @Listen("onClick = #whdNavigationTreeBgBtn; onClick = #whdNavigationTreeBgPrev; onDoubleClick = #whdNavigationTreeBgPrev")
    public void onNavigationTreeBgClick()
    {
        initEditBorderDialog(getNavigationTreeBgPreviewCSS());
        borderWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setNavigationTreeBorderCSS(borderWinComposer.getBorderCSS());
                openedDesignProps.setNavigationTreeBgColor(borderWinComposer.getBgColor());
                
                if (borderWinComposer.hasBackgroundImage()) {
                    File editBorderBgImageUploaded = borderWinComposer.getUploadedBackgroundImageFile();
                    if (editBorderBgImageUploaded != null) {  // background image has been added or changed
                        String temp_fn = editBorderBgImageUploaded.getName();
                        String ext = WhdUtils.getFilenameExtension(temp_fn);
                        String fn = PREFIX_NAV_INNER_BG + System.currentTimeMillis() + "." + ext;
                        File bg_img = updatePreviewFile(editBorderBgImageUploaded, webhelpCommonImagePath() + File.separator + fn);
                        addToRemovedFiles(openedDesignProps.getNavigationTreeBgImage());
                        addToChangedFiles(bg_img);
                        openedDesignProps.setNavigationTreeBgImage(bg_img.getName());
                    }
                    openedDesignProps.setNavigationTreeBgRepeat(borderWinComposer.getBgRepeat());
                } else {
                    addToRemovedFiles(openedDesignProps.getNavigationTreeBgImage());
                    openedDesignProps.setNavigationTreeBgImage(null);
                }

                // Refresh preview
                updateNavigationControls(); // whdNavigationBgPrev.setStyle(getNavigationBorderBgCSS());
                designChanged();
            }
        });
        whdBorderWin.doHighlighted();
    }

    @Listen("onClick = #whdPartTabBgBtn; onClick = #whdPartTabBgPrev; onDoubleClick = #whdPartTabBgPrev")
    public void onPartTabBgDefaultClick()
    {
        initEditBorderDialog(getPartTabBgPreviewCSSDefault());
        borderWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                String bgcss = "background-color:" + borderWinComposer.getBgColor() + 
                               "; " + borderWinComposer.getBorderCSS();
                openedDesignProps.setPartTabBgCSS(bgcss);
                
                if (borderWinComposer.hasBackgroundImage()) {
                    File editBorderBgImageUploaded = borderWinComposer.getUploadedBackgroundImageFile();
                    if (editBorderBgImageUploaded != null) {  // background image has been added or changed
                        String temp_fn = editBorderBgImageUploaded.getName();
                        String ext = WhdUtils.getFilenameExtension(temp_fn);
                        String fn = PREFIX_PART_TAB_BG + System.currentTimeMillis() + "." + ext;
                        File bg_img = updatePreviewFile(editBorderBgImageUploaded, webhelpCommonImagePath() + File.separator + fn);
                        addToRemovedFiles(openedDesignProps.getPartTabBgImage());
                        addToChangedFiles(bg_img);
                        openedDesignProps.setPartTabBgImage(bg_img.getName());
                    }
                    openedDesignProps.setPartTabBgRepeat(borderWinComposer.getBgRepeat());
                } else {
                    addToRemovedFiles(openedDesignProps.getPartTabBgImage());
                    openedDesignProps.setPartTabBgImage(null);
                }

                // Refresh preview
                updatePartControlPreviews();
                designChanged();
            }
        });
        whdBorderWin.doHighlighted();
    }

    @Listen("onClick = #whdPartTabBgCurrentBtn; onClick = #whdPartTabBgCurrentPrev; onDoubleClick = #whdPartTabBgCurrentPrev")
    public void onPartTabBgCurrentClick()
    {
        initEditBorderDialog(getPartTabBgPreviewCSSCurrent());
        borderWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                String bgcss = "background-color:" + borderWinComposer.getBgColor() + 
                               "; " + borderWinComposer.getBorderCSS();
                openedDesignProps.setPartTabBgCSSCurrent(bgcss);
                
                if (borderWinComposer.hasBackgroundImage()) {
                    File editBorderBgImageUploaded = borderWinComposer.getUploadedBackgroundImageFile();
                    if (editBorderBgImageUploaded != null) {  // background image has been added or changed
                        String temp_fn = editBorderBgImageUploaded.getName();
                        String ext = WhdUtils.getFilenameExtension(temp_fn);
                        String fn = PREFIX_PART_TAB_CT_BG + System.currentTimeMillis() + "." + ext;
                        File bg_img = updatePreviewFile(editBorderBgImageUploaded, webhelpCommonImagePath() + File.separator + fn);
                        addToRemovedFiles(openedDesignProps.getPartTabBgImageCurrent());
                        addToChangedFiles(bg_img);
                        openedDesignProps.setPartTabBgImageCurrent(bg_img.getName());
                    }
                    openedDesignProps.setPartTabBgRepeatCurrent(borderWinComposer.getBgRepeat());
                } else {
                    addToRemovedFiles(openedDesignProps.getPartTabBgImageCurrent());
                    openedDesignProps.setPartTabBgImageCurrent(null);
                }

                // Refresh preview
                updatePartControlPreviews();
                designChanged();
            }
        });
        whdBorderWin.doHighlighted();
    }

    @Listen("onClick = #whdPartTabBgHoverBtn; onClick = #whdPartTabBgHoverPrev; onDoubleClick = #whdPartTabBgHoverPrev")
    public void onPartTabBgHoverClick()
    {
        initEditBorderDialog(getPartTabBgPreviewCSSHover());
        borderWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                String bgcss = "background-color:" + borderWinComposer.getBgColor() + 
                               "; " + borderWinComposer.getBorderCSS();
                openedDesignProps.setPartTabBgCSSHover(bgcss);
                
                if (borderWinComposer.hasBackgroundImage()) {
                    File editBorderBgImageUploaded = borderWinComposer.getUploadedBackgroundImageFile();
                    if (editBorderBgImageUploaded != null) {  // background image has been added or changed
                        String temp_fn = editBorderBgImageUploaded.getName();
                        String ext = WhdUtils.getFilenameExtension(temp_fn);
                        String fn = PREFIX_PART_TAB_HL_BG + System.currentTimeMillis() + "." + ext;
                        File bg_img = updatePreviewFile(editBorderBgImageUploaded, webhelpCommonImagePath() + File.separator + fn);
                        addToRemovedFiles(openedDesignProps.getPartTabBgImageHover());
                        addToChangedFiles(bg_img);
                        openedDesignProps.setPartTabBgImageHover(bg_img.getName());
                    }
                    openedDesignProps.setPartTabBgRepeatHover(borderWinComposer.getBgRepeat());
                } else {
                    addToRemovedFiles(openedDesignProps.getPartTabBgImageHover());
                    openedDesignProps.setPartTabBgImageHover(null);
                }

                // Refresh preview
                updatePartControlPreviews();
                designChanged();
            }
        });
        whdBorderWin.doHighlighted();
    }

    @Listen("onClick = #whdNavLocalBgBtn; onClick = #whdNavLocalBgPrev; onDoubleClick = #whdNavLocalBgPrev")
    public void onNavLocalBgClick()
    {
        initEditBorderDialog(getLocalNavigationBgPreviewCSS());
        borderWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                String bgcss = "background-color:" + borderWinComposer.getBgColor() + 
                               "; " + borderWinComposer.getBorderCSS();
                openedDesignProps.setLocalNavigationBgCSS(bgcss);
                
                if (borderWinComposer.hasBackgroundImage()) {
                    File editBorderBgImageUploaded = borderWinComposer.getUploadedBackgroundImageFile();
                    if (editBorderBgImageUploaded != null) {  // background image has been added or changed
                        String temp_fn = editBorderBgImageUploaded.getName();
                        String ext = WhdUtils.getFilenameExtension(temp_fn);
                        String fn = PREFIX_LOCAL_NAV_BG + System.currentTimeMillis() + "." + ext;
                        File bg_img = updatePreviewFile(editBorderBgImageUploaded, webhelpCommonImagePath() + File.separator + fn);
                        addToRemovedFiles(openedDesignProps.getLocalNavigationBgImage());
                        addToChangedFiles(bg_img);
                        openedDesignProps.setLocalNavigationBgImage(bg_img.getName());
                    }
                    openedDesignProps.setLocalNavigationBgRepeat(borderWinComposer.getBgRepeat());
                } else {
                    addToRemovedFiles(openedDesignProps.getLocalNavigationBgImage());
                    openedDesignProps.setLocalNavigationBgImage(null);
                }

                // Refresh preview
                updateLocalNavigationControlPreviews();
                designChanged();
            }
        });
        whdBorderWin.doHighlighted();
    }

    @Listen("onClick = #whdBreadcrumbsBgBtn; onClick = #whdBreadcrumbsBgPrev; onDoubleClick = #whdBreadcrumbsBgPrev")
    public void onBreadcrumbsBgClick()
    {
        initEditBorderDialog(getBreadcrumbsBgPreviewCSS());
        borderWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                String bgcss = "background-color:" + borderWinComposer.getBgColor() + 
                               "; " + borderWinComposer.getBorderCSS();
                openedDesignProps.setBreadcrumbsBgCSS(bgcss);
                
                if (borderWinComposer.hasBackgroundImage()) {
                    File editBorderBgImageUploaded = borderWinComposer.getUploadedBackgroundImageFile();
                    if (editBorderBgImageUploaded != null) {  // background image has been added or changed
                        String temp_fn = editBorderBgImageUploaded.getName();
                        String ext = WhdUtils.getFilenameExtension(temp_fn);
                        String fn = PREFIX_BREAD_BG + System.currentTimeMillis() + "." + ext;
                        File bg_img = updatePreviewFile(editBorderBgImageUploaded, webhelpCommonImagePath() + File.separator + fn);
                        addToRemovedFiles(openedDesignProps.getBreadcrumbsBgImage());
                        addToChangedFiles(bg_img);
                        openedDesignProps.setBreadcrumbsBgImage(bg_img.getName());
                    }
                    openedDesignProps.setBreadcrumbsBgRepeat(borderWinComposer.getBgRepeat());
                } else {
                    addToRemovedFiles(openedDesignProps.getBreadcrumbsBgImage());
                    openedDesignProps.setBreadcrumbsBgImage(null);
                }

                // Refresh preview
                updateBreadcrumbsControlPreviews();
                designChanged();
            }
        });
        whdBorderWin.doHighlighted();
    }

    @Listen("onClick = #whdSearchBgBtn; onClick = #whdSearchBgPrev; onDoubleClick = #whdSearchBgPrev")
    public void onSearchBgClick()
    {
        initEditBorderDialog(getSearchBgPreviewCSS());
        borderWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setSearchBorderCSS(borderWinComposer.getBorderCSS());
                openedDesignProps.setSearchBgColor(borderWinComposer.getBgColor());
                
                if (borderWinComposer.hasBackgroundImage()) {
                    File editBorderBgImageUploaded = borderWinComposer.getUploadedBackgroundImageFile();
                    if (editBorderBgImageUploaded != null) {  // background image has been added or changed
                        String temp_fn = editBorderBgImageUploaded.getName();
                        String ext = WhdUtils.getFilenameExtension(temp_fn);
                        String fn = PREFIX_SEARCH_BG + System.currentTimeMillis() + "." + ext;
                        File bg_img = updatePreviewFile(editBorderBgImageUploaded, webhelpCommonImagePath() + File.separator + fn);
                        addToRemovedFiles(openedDesignProps.getSearchBgImage());
                        addToChangedFiles(bg_img);
                        openedDesignProps.setSearchBgImage(bg_img.getName());
                    }
                    openedDesignProps.setSearchBgRepeat(borderWinComposer.getBgRepeat());
                } else {
                    addToRemovedFiles(openedDesignProps.getSearchBgImage());
                    openedDesignProps.setSearchBgImage(null);
                }

                // Refresh preview
                updateSearchControlPreviews();
                designChanged();
            }
        });
        whdBorderWin.doHighlighted();
    }

    @Listen("onClick = #whdSearchInnerBgBtn; onClick = #whdSearchInnerBgPrev; onDoubleClick = #whdSearchInnerBgPrev")
    public void onSearchInnerBgClick()
    {
        initEditBorderDialog(getSearchInnerBgPreviewCSS());
        borderWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setSearchInnerBorderCSS(borderWinComposer.getBorderCSS());
                openedDesignProps.setSearchInnerBgColor(borderWinComposer.getBgColor());
                
                if (borderWinComposer.hasBackgroundImage()) {
                    File editBorderBgImageUploaded = borderWinComposer.getUploadedBackgroundImageFile();
                    if (editBorderBgImageUploaded != null) {  // background image has been added or changed
                        String temp_fn = editBorderBgImageUploaded.getName();
                        String ext = WhdUtils.getFilenameExtension(temp_fn);
                        String fn = PREFIX_SEARCH_INNER_BG + System.currentTimeMillis() + "." + ext;
                        File bg_img = updatePreviewFile(editBorderBgImageUploaded, webhelpCommonImagePath() + File.separator + fn);
                        addToRemovedFiles(openedDesignProps.getSearchInnerBgImage());
                        addToChangedFiles(bg_img);
                        openedDesignProps.setSearchInnerBgImage(bg_img.getName());
                    }
                    openedDesignProps.setSearchInnerBgRepeat(borderWinComposer.getBgRepeat());
                } else {
                    addToRemovedFiles(openedDesignProps.getSearchInnerBgImage());
                    openedDesignProps.setSearchInnerBgImage(null);
                }

                // Refresh preview
                updateSearchControlPreviews();
                designChanged();
            }
        });
        whdBorderWin.doHighlighted();
    }

    @Listen("onClick = #whdSearchInputBgBtn; onClick = #whdSearchInputBgPrev; onDoubleClick = #whdSearchInputBgPrev")
    public void onSearchInputBgClick()
    {
        initEditBorderDialog(getSearchInputBgPreviewCSS());
        borderWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setSearchInputBorderCSS(borderWinComposer.getBorderCSS());
                openedDesignProps.setSearchInputBgColor(borderWinComposer.getBgColor());
                
                if (borderWinComposer.hasBackgroundImage()) {
                    File editBorderBgImageUploaded = borderWinComposer.getUploadedBackgroundImageFile();
                    if (editBorderBgImageUploaded != null) {  // background image has been added or changed
                        String temp_fn = editBorderBgImageUploaded.getName();
                        String ext = WhdUtils.getFilenameExtension(temp_fn);
                        String fn = PREFIX_SEARCH_INPUT_BG + System.currentTimeMillis() + "." + ext;
                        File bg_img = updatePreviewFile(editBorderBgImageUploaded, webhelpCommonImagePath() + File.separator + fn);
                        addToRemovedFiles(openedDesignProps.getSearchInputBgImage());
                        addToChangedFiles(bg_img);
                        openedDesignProps.setSearchInputBgImage(bg_img.getName());
                    }
                    openedDesignProps.setSearchInputBgRepeat(borderWinComposer.getBgRepeat());
                } else {
                    addToRemovedFiles(openedDesignProps.getSearchInputBgImage());
                    openedDesignProps.setSearchInputBgImage(null);
                }

                // Refresh preview
                updateSearchControlPreviews();
                designChanged();
            }
        });
        whdBorderWin.doHighlighted();
    }

    @Listen("onClick = #whdSearchBtnBgBtn; onClick = #whdSearchBtnBgPrev; onDoubleClick = #whdSearchBtnBgPrev")
    public void onSearchButtonBgClick()
    {
        initEditBorderDialog(getSearchButtonBgPreviewCSS());
        borderWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setSearchButtonBorderCSS(borderWinComposer.getBorderCSS());
                openedDesignProps.setSearchButtonBgColor(borderWinComposer.getBgColor());
                
                if (borderWinComposer.hasBackgroundImage()) {
                    File editBorderBgImageUploaded = borderWinComposer.getUploadedBackgroundImageFile();
                    if (editBorderBgImageUploaded != null) {  // background image has been added or changed
                        String temp_fn = editBorderBgImageUploaded.getName();
                        String ext = WhdUtils.getFilenameExtension(temp_fn);
                        String fn = PREFIX_SEARCH_BUTTON + System.currentTimeMillis() + "." + ext;
                        File bg_img = updatePreviewFile(editBorderBgImageUploaded, webhelpCommonImagePath() + File.separator + fn);
                        addToRemovedFiles(openedDesignProps.getSearchButtonBgImage());
                        addToChangedFiles(bg_img);
                        openedDesignProps.setSearchButtonBgImage(bg_img.getName());
                    }
                    openedDesignProps.setSearchButtonBgRepeat(borderWinComposer.getBgRepeat());
                } else {
                    addToRemovedFiles(openedDesignProps.getSearchButtonBgImage());
                    openedDesignProps.setSearchButtonBgImage(null);
                }

                // Refresh preview
                updateSearchControlPreviews();
                designChanged();
            }
        });
        whdBorderWin.doHighlighted();
    }

    @Listen("onClick = #whdSearchTabBgBtn; onClick = #whdSearchTabBgPrev; onDoubleClick = #whdSearchTabBgPrev")
    public void onSearchTabBgClick()
    {
        initEditBorderDialog(getSearchTabBgPreviewCSS());
        borderWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                String bgcss = "background-color:" + borderWinComposer.getBgColor() + 
                               "; " + borderWinComposer.getBorderCSS();
                openedDesignProps.setSearchTabBgCSS(bgcss);
                
                if (borderWinComposer.hasBackgroundImage()) {
                    File editBorderBgImageUploaded = borderWinComposer.getUploadedBackgroundImageFile();
                    if (editBorderBgImageUploaded != null) {  // background image has been added or changed
                        String temp_fn = editBorderBgImageUploaded.getName();
                        String ext = WhdUtils.getFilenameExtension(temp_fn);
                        String fn = PREFIX_SEARCH_TAB_BG + System.currentTimeMillis() + "." + ext;
                        File bg_img = updatePreviewFile(editBorderBgImageUploaded, webhelpCommonImagePath() + File.separator + fn);
                        addToRemovedFiles(openedDesignProps.getSearchTabBgImage());
                        addToChangedFiles(bg_img);
                        openedDesignProps.setSearchTabBgImage(bg_img.getName());
                    }
                    openedDesignProps.setSearchTabBgRepeat(borderWinComposer.getBgRepeat());
                } else {
                    addToRemovedFiles(openedDesignProps.getSearchTabBgImage());
                    openedDesignProps.setSearchTabBgImage(null);
                }

                // Refresh preview
                updateSearchTabControlPreviews();
                designChanged();
            }
        });
        whdBorderWin.doHighlighted();
    }

    @Listen("onClick = #whdSearchTabBgActiveBtn; onClick = #whdSearchTabBgActivePrev; onDoubleClick = #whdSearchTabBgActivePrev")
    public void onSearchTabBgActiveClick()
    {
        initEditBorderDialog(getSearchTabBgPreviewCSSActive());
        borderWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                String bgcss = "background-color:" + borderWinComposer.getBgColor() + 
                               "; " + borderWinComposer.getBorderCSS();
                openedDesignProps.setSearchTabBgCSSActive(bgcss);
                
                if (borderWinComposer.hasBackgroundImage()) {
                    File editBorderBgImageUploaded = borderWinComposer.getUploadedBackgroundImageFile();
                    if (editBorderBgImageUploaded != null) {  // background image has been added or changed
                        String temp_fn = editBorderBgImageUploaded.getName();
                        String ext = WhdUtils.getFilenameExtension(temp_fn);
                        String fn = PREFIX_SEARCH_TAB_CT_BG + System.currentTimeMillis() + "." + ext;
                        File bg_img = updatePreviewFile(editBorderBgImageUploaded, webhelpCommonImagePath() + File.separator + fn);
                        addToRemovedFiles(openedDesignProps.getSearchTabBgImageActive());
                        addToChangedFiles(bg_img);
                        openedDesignProps.setSearchTabBgImageActive(bg_img.getName());
                    }
                    openedDesignProps.setSearchTabBgRepeatActive(borderWinComposer.getBgRepeat());
                } else {
                    addToRemovedFiles(openedDesignProps.getSearchTabBgImageActive());
                    openedDesignProps.setSearchTabBgImageActive(null);
                }

                // Refresh preview
                updateSearchTabControlPreviews();
                designChanged();
            }
        });
        whdBorderWin.doHighlighted();
    }

    @Listen("onClick = #whdSearchTabBgHoverBtn; onClick = #whdSearchTabBgHoverPrev; onDoubleClick = #whdSearchTabBgHoverPrev")
    public void onSearchTabBgHoverClick()
    {
        initEditBorderDialog(getSearchTabBgPreviewCSSHover());
        borderWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                String bgcss = "background-color:" + borderWinComposer.getBgColor() + 
                               "; " + borderWinComposer.getBorderCSS();
                openedDesignProps.setSearchTabBgCSSHover(bgcss);
                
                if (borderWinComposer.hasBackgroundImage()) {
                    File editBorderBgImageUploaded = borderWinComposer.getUploadedBackgroundImageFile();
                    if (editBorderBgImageUploaded != null) {  // background image has been added or changed
                        String temp_fn = editBorderBgImageUploaded.getName();
                        String ext = WhdUtils.getFilenameExtension(temp_fn);
                        String fn = PREFIX_SEARCH_TAB_HL_BG + System.currentTimeMillis() + "." + ext;
                        File bg_img = updatePreviewFile(editBorderBgImageUploaded, webhelpCommonImagePath() + File.separator + fn);
                        addToRemovedFiles(openedDesignProps.getSearchTabBgImageHover());
                        addToChangedFiles(bg_img);
                        openedDesignProps.setSearchTabBgImageHover(bg_img.getName());
                    }
                    openedDesignProps.setSearchTabBgRepeatHover(borderWinComposer.getBgRepeat());
                } else {
                    addToRemovedFiles(openedDesignProps.getSearchTabBgImageHover());
                    openedDesignProps.setSearchTabBgImageHover(null);
                }

                // Refresh preview
                updateSearchTabControlPreviews();
                designChanged();
            }
        });
        whdBorderWin.doHighlighted();
    }

    @Listen("onClick = #whdSearchPanelBgBtn; onClick = #whdSearchPanelBgPrev; onDoubleClick = #whdSearchPanelBgPrev")
    public void onSearchPanelBgClick()
    {
        initEditBorderDialog(getSearchPanelBgPreviewCSS());
        borderWinComposer.setOkayListener(new EventListener() {
            public void onEvent(Event evt) throws Exception {
                openedDesignProps.setSearchPanelBorderCSS(borderWinComposer.getBorderCSS());
                openedDesignProps.setSearchPanelBgColor(borderWinComposer.getBgColor());
                
                if (borderWinComposer.hasBackgroundImage()) {
                    File editBorderBgImageUploaded = borderWinComposer.getUploadedBackgroundImageFile();
                    if (editBorderBgImageUploaded != null) {  // background image has been added or changed
                        String temp_fn = editBorderBgImageUploaded.getName();
                        String ext = WhdUtils.getFilenameExtension(temp_fn);
                        String fn = PREFIX_SEARCH_PANEL_BG + System.currentTimeMillis() + "." + ext;
                        File bg_img = updatePreviewFile(editBorderBgImageUploaded, webhelpCommonImagePath() + File.separator + fn);
                        addToRemovedFiles(openedDesignProps.getSearchPanelBgImage());
                        addToChangedFiles(bg_img);
                        openedDesignProps.setSearchPanelBgImage(bg_img.getName());
                    }
                    openedDesignProps.setSearchPanelBgRepeat(borderWinComposer.getBgRepeat());
                } else {
                    addToRemovedFiles(openedDesignProps.getSearchPanelBgImage());
                    openedDesignProps.setSearchPanelBgImage(null);
                }

                // Refresh preview
                updateSearchTabControlPreviews();
                designChanged();
            }
        });
        whdBorderWin.doHighlighted();
    }


    //********************************************************************
    //************   "New Design"-Dialog Event Handler   *****************
    //********************************************************************
    
    @Listen("onSelect = #whdNewWin #whdNewLayoutIdBox")
    public void onNewSelectLayout()
    {
        updateNewWebhelpLayoutVersionList();
    }
    
    @Listen("onClick = #whdNewWin #whdNewOkayBtn")
    public void onNewOkayClick()
    {
        // Validate input
        String new_nm = whdNewNameBox.getValue().trim();
        if (new_nm.equals("")) {
            Messagebox.show("Enter a name!");
            return;
        }
        if (! validNewDesignName(new_nm)) {
            return;
        }
        String layout = whdNewLayoutIdBox.getSelectedItem().getValue().toString();
        String ver = whdNewLayoutVersionBox.getSelectedItem().getValue().toString();
        createWebhelpDesign(new_nm, layout, ver);
        whdNewWin.setVisible(false);
        
        openDesign(new_nm);
        updateWebhelpSelectionList();
    }
    
    @Listen("onClick = #whdNewWin #whdNewCancelBtn")
    public void onNewCancelClick()
    {
        whdNewWin.setVisible(false);
    }

    private void updateNewWebhelpLayoutVersionList()
    {
        Listitem it = whdNewLayoutIdBox.getSelectedItem();
        String nm = it.getValue().toString();
        
        initLayoutVersionListbox(nm, whdNewLayoutVersionBox);
        whdNewLayoutVersionBox.setSelectedIndex(whdNewLayoutVersionBox.getItemCount() - 1);
    }
    

    //********************************************************************
    //**************   "Save As"-Dialog Event Handler   ******************
    //********************************************************************
    
    @Listen("onClick = #whdSaveAsWin #whdSaveAsOkayBtn")
    public void onSaveAsOkayClick()
    {
        // Validate input
        String new_nm = whdSaveAsNameBox.getValue().trim();
        if (new_nm.equals("")) {
            Messagebox.show("Enter a name!");
            return;
        }
        if (! validNewDesignName(new_nm)) {
            return;
        }
        saveOpenedDesignAs(new_nm);
        updateWebhelpSelectionList();
        whdSaveAsWin.setVisible(false);
    }
    
    @Listen("onClick = #whdSaveAsWin #whdSaveAsCancelBtn")
    public void onSaveAsCancelClick()
    {
        whdSaveAsWin.setVisible(false);
    }


    //********************************************************************
    //******************   GUI Helper functions   ************************
    //********************************************************************

    private boolean validNewDesignName(String design_name)
    {
        if (design_name.length() > MAXLENGTH_WHD_ID) {
            Messagebox.show("Name exceeds maximum length of 20 characters!");
            return false;
        }
        if (! design_name.matches(REGEXP_WHD_ID)) {
            Messagebox.show("Invalid name: Use letters, digits, underscore and dash only!");
            return false;
        }
        if (webhelpDesignExists(design_name)) {
            Messagebox.show("Name already exists!");
            return false;
        }
        return true;
    }
    
//    private String previewCSS(String css)
//    {
//        final String URL_PATT = "url(";
//        int url_start = css.toLowerCase().indexOf(URL_PATT);
//        if (url_start < 0) {
//            return css;
//        }
//        url_start += URL_PATT.length();
//        int url_end = css.indexOf(')', url_start);
//        if (url_end < 0) {
//            return css;
//        }
//        String url = css.substring(url_start, url_end);
//        url = getRelativePreviewURL(webhelpCommonCssPath() + "/" + url);
//        return css.substring(0, url_start) + url + css.substring(url_end);
//    }
    
    private String formatBgCSS(String other_css, String bgcolor, String bgimage, String bgrepeat) 
    {
        if ((bgcolor != null) && !bgcolor.equals("")) {
            other_css = "background-color:" + bgcolor + ";" + other_css;
        }
        return formatBgImageCSS(other_css, bgimage, bgrepeat);
    }

    private String formatBgImageCSS(String other_css, String bgimage, String bgrepeat)
    {
        String css = (other_css == null) ? "" : other_css;
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += "; ";
        }
        if ((bgimage != null) && (bgimage.length() > 0)) {
            css += "background-image:url(../images/" + bgimage + 
                   "); background-repeat:" + bgrepeat + ";";
        }
        return css;
    }
    
    private String formatBgPreviewCSS(String bgcolor, String bgimage, String bgrepeat)
    {
        if ((bgcolor == null) || bgcolor.equals("")) {
            bgcolor = "transparent";
        }
        String css = "background-color:" + bgcolor + ";";
        if ((bgimage != null) && (bgimage.length() > 0)) {
            css += formatBgImagePreviewCSS(bgimage, bgrepeat);
        }
        return css;
    }
    
    private String formatBgImagePreviewCSS(String bgimage, String bgrepeat)
    {
        if ((bgimage == null) || bgimage.equals("")) {
            return "";
        }
        return "background-image:url(" + getRelativePreviewURL(webhelpCommonImageURL() + "/" + bgimage) + 
               "); background-repeat:" + bgrepeat + ";";
    }
    
    private String getHeaderBgPreviewCSS()
    {
        String css = openedDesignProps.getHeaderBorderCSS();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        css += formatBgPreviewCSS(openedDesignProps.getHeaderBgColor(), 
                                  openedDesignProps.getHeaderBgImage(), 
                                  openedDesignProps.getHeaderBgRepeat());
        return css;
    }
    
    private String getNavigationBgPreviewCSS()
    {
        String css = openedDesignProps.getNavigationBorderCSS();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        css += formatBgPreviewCSS(openedDesignProps.getNavigationBgColor(), 
                                  openedDesignProps.getNavigationBgImage(), 
                                  openedDesignProps.getNavigationBgRepeat());
        return css;
    }
    
    private String getAccordionBgPreviewCSS()
    {
        String css = openedDesignProps.getAccordionBorderCSS();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        css += formatBgPreviewCSS(openedDesignProps.getAccordionBgColor(), 
                                  openedDesignProps.getAccordionBgImage(), 
                                  openedDesignProps.getAccordionBgRepeat());
        return css;
    }
    
    private String getNavigationTreeBgPreviewCSS()
    {
        String css = openedDesignProps.getNavigationTreeBorderCSS();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        css += formatBgPreviewCSS(openedDesignProps.getNavigationTreeBgColor(), 
                                  openedDesignProps.getNavigationTreeBgImage(), 
                                  openedDesignProps.getNavigationTreeBgRepeat());
        return css;
    }
    
    private String getPartTabBgPreviewCSSDefault()
    {
        String css = openedDesignProps.getPartTabBgCSS();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        css += formatBgImagePreviewCSS(openedDesignProps.getPartTabBgImage(), 
                                       openedDesignProps.getPartTabBgRepeat());
        return css;
    }
    
    private String getPartTabBgPreviewCSSCurrent()
    {
        String css = openedDesignProps.getPartTabBgCSSCurrent();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        css += formatBgImagePreviewCSS(openedDesignProps.getPartTabBgImageCurrent(), 
                                       openedDesignProps.getPartTabBgRepeatCurrent());
        return css;
    }
    
    private String getPartTabBgPreviewCSSHover()
    {
        String css = openedDesignProps.getPartTabBgCSSHover();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        css += formatBgImagePreviewCSS(openedDesignProps.getPartTabBgImageHover(), 
                                       openedDesignProps.getPartTabBgRepeatHover());
        return css;
    }

    private String getLocalNavigationBgPreviewCSS()
    {
        String css = openedDesignProps.getLocalNavigationBgCSS();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        css += formatBgImagePreviewCSS(openedDesignProps.getLocalNavigationBgImage(), 
                                       openedDesignProps.getLocalNavigationBgRepeat());
        return css;
    }

    private String getBreadcrumbsBgPreviewCSS()
    {
        String css = openedDesignProps.getBreadcrumbsBgCSS();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        css += formatBgImagePreviewCSS(openedDesignProps.getBreadcrumbsBgImage(), 
                                       openedDesignProps.getBreadcrumbsBgRepeat());
        return css;
    }

    private String getSearchTabBgPreviewCSS()
    {
        String css = openedDesignProps.getSearchTabBgCSS();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        css += formatBgImagePreviewCSS(openedDesignProps.getSearchTabBgImage(), 
                                       openedDesignProps.getSearchTabBgRepeat());
        return css;
    }
    
    private String getSearchTabBgPreviewCSSActive()
    {
        String css = openedDesignProps.getSearchTabBgCSSActive();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        css += formatBgImagePreviewCSS(openedDesignProps.getSearchTabBgImageActive(), 
                                       openedDesignProps.getSearchTabBgRepeatActive());
        return css;
    }
    
    private String getSearchTabBgPreviewCSSHover()
    {
        String css = openedDesignProps.getSearchTabBgCSSHover();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        css += formatBgImagePreviewCSS(openedDesignProps.getSearchTabBgImageHover(), 
                                       openedDesignProps.getSearchTabBgRepeatHover());
        return css;
    }
    
    private String getSearchBgPreviewCSS()
    {
        String css = openedDesignProps.getSearchBorderCSS();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        css += formatBgPreviewCSS(openedDesignProps.getSearchBgColor(), 
                                  openedDesignProps.getSearchBgImage(), 
                                  openedDesignProps.getSearchBgRepeat());
        return css;
    }
    
    private String getSearchInnerBgPreviewCSS()
    {
        String css = openedDesignProps.getSearchInnerBorderCSS();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        css += formatBgPreviewCSS(openedDesignProps.getSearchInnerBgColor(), 
                                  openedDesignProps.getSearchInnerBgImage(), 
                                  openedDesignProps.getSearchInnerBgRepeat());
        return css;
    }

    private String getSearchInputBgPreviewCSS()
    {
        String css = openedDesignProps.getSearchInputBorderCSS();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        css += formatBgPreviewCSS(openedDesignProps.getSearchInputBgColor(), 
                                  openedDesignProps.getSearchInputBgImage(), 
                                  openedDesignProps.getSearchInputBgRepeat());
        return css;
    }

    private String getSearchButtonBgPreviewCSS()
    {
        String css = openedDesignProps.getSearchButtonBorderCSS();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        css += formatBgPreviewCSS(openedDesignProps.getSearchButtonBgColor(), 
                                  openedDesignProps.getSearchButtonBgImage(), 
                                  openedDesignProps.getSearchButtonBgRepeat());
        return css;
    }

    private String getSearchButtonPreviewCSS()
    {
        return getSearchButtonBgPreviewCSS() + 
               " width:" + cssLenOrAuto(openedDesignProps.getSearchButtonWidth()) +
               "; height:" + cssLenOrAuto(openedDesignProps.getSearchButtonHeight()) +
               "; text-align:center;";  // center button text
    }

    private String getSearchPanelBgPreviewCSS()
    {
        String css = openedDesignProps.getSearchPanelBorderCSS();
        if ((css.length() > 0) && !css.endsWith(";")) {
            css += ";";
        }
        css += formatBgPreviewCSS(openedDesignProps.getSearchPanelBgColor(), 
                                  openedDesignProps.getSearchPanelBgImage(), 
                                  openedDesignProps.getSearchPanelBgRepeat());
        return css;
    }

    private String getPixelInput(Event evt, Spinner box)
    {
        InputEvent ievt = (InputEvent) evt;
        boolean is_changing = ievt.getName().equalsIgnoreCase("onChanging");
        return inputPixelLength(is_changing ? ievt.getValue() : box.getValue(), "0px");
    }
    
    private String getPixelInputOrEmpty(Event evt, Spinner box)
    {
        InputEvent ievt = (InputEvent) evt;
        boolean is_changing = ievt.getName().equalsIgnoreCase("onChanging");
        return inputPixelLength(is_changing ? ievt.getValue() : box.getValue(), "");
    }
    
    /**
     * 
     * @param obj A String or Integer instance. 
     * @return The value with suffix "px".
     */
    private String inputPixelLength(Object obj, String emptyValue)
    {
        String len = (obj != null) ? obj.toString().trim() : "";
        if (len.equals("")) {
            return emptyValue;
        }
        return len + "px";
    }
    
    /**
     * Has to be called if some design properties changed and preview frame 
     * needs to be refreshed. This starts a timer and refresh is done with a
     * delay of 500 msec. This is an optimization: multiple calls to 
     * designChanged within 500 msec lead to only one refresh. 
     */
    private void designChanged()
    {
        disableSave(false);  // enable save button
        
        if (whdPreviewTimer.isRunning()) {
            whdPreviewTimer.stop();
        }
        whdPreviewTimer.start();
    }
    

    private void setDesignerControlsVisible(boolean is_visible)
    {
        whdPreviewControls.setVisible(is_visible);
        // whdNameArea.setVisible(is_visible);
        // whdLayoutVersionArea.setVisible(is_visible);
        // whdButtonsArea.setVisible(is_visible);
        
        whdControlsTabBox.setVisible(is_visible);
        // whdGeneralGroup.setVisible(is_visible);
        // whdHeadingGroup.setVisible(is_visible);
        // whdNavigationGroup.setVisible(is_visible);
    }

    private void openDesign(String design_name)
    {
        setDesignerControlsVisible(true);
        try {
            openedDesignName = design_name;
            openedDesignProps = readDesignProps(design_name);
            
            // Preview controls
            whdPreviewPartsCheckbox.setChecked(true);
            whdPreviewCrumbsCheckbox.setChecked(openedDesignProps.isBreadcrumbsDisplay());
            
            preparePreview();
            
            // Name, Layout and version controls
            whdDesignNameLabel.setValue(design_name);
            String layout_id = openedDesignProps.getLayoutId();
            WhdUtils.selectListItem(whdLayoutIdBox, layout_id);
            initLayoutVersionListbox(layout_id, whdLayoutVersionBox);
            if (! WhdUtils.selectListItem(whdLayoutVersionBox, openedDesignProps.getLayoutVersion())) {
                // Fall back to latest version if version does not exist.
                whdLayoutVersionBox.setSelectedIndex(whdLayoutVersionBox.getItemCount() - 1);
            }

            //
            // General controls
            //
            
            // Body background color
            whdGeneralBgColorBox.setColor(openedDesignProps.getBodyBgColor());
            
            // Favorite icon
            File fav = getSavedFavicon(design_name);
            whdFaviconImage.setContent(fav.exists() ? new AImage(fav) : null);
            whdContentTopBox.setValue(openedDesignProps.getContentTopInteger());
            whdContentPaddingLeftBox.setValue(openedDesignProps.getContentPaddingLeftInteger());
            whdContentPaddingRightBox.setValue(openedDesignProps.getContentPaddingRightInteger());
            whdContentWidthMinBox.setValue(openedDesignProps.getContentWidthMinInteger());
            whdContentWidthMaxBox.setValue(openedDesignProps.getContentWidthMaxInteger());
            whdWatermarkXPosBox.setValue(openedDesignProps.getWatermarkPosXInteger());
            whdWatermarkYPosBox.setValue(openedDesignProps.getWatermarkPosYInteger());

            //
            // Heading controls
            //
            whdHeaderHeightBox.setValue(openedDesignProps.getHeaderHeightInteger());
            whdHeaderPaddingTopBox.setValue(openedDesignProps.getHeaderPaddingTopInteger());
            whdHeaderPaddingBottomBox.setValue(openedDesignProps.getHeaderPaddingBottomInteger());
            whdHeaderPaddingLeftBox.setValue(openedDesignProps.getHeaderPaddingLeftInteger());
            whdHeaderPaddingRightBox.setValue(openedDesignProps.getHeaderPaddingRightInteger());
            whdHeaderTitle1Checkbox.setChecked(openedDesignProps.isHeaderTitle1Enabled());
            whdHeaderTitle2Checkbox.setChecked(openedDesignProps.isHeaderTitle2Enabled());
            WhdUtils.selectListItem(whdHeaderTitleAlignBox, openedDesignProps.getHeaderTitleAlignment());
            whdHeaderTitleMarginTopBox.setValue(openedDesignProps.getHeaderTitleMarginTopInteger());
            whdHeaderTitleMarginBottomBox.setValue(openedDesignProps.getHeaderTitleMarginBottomInteger());
            whdHeaderTitleMarginLeftBox.setValue(openedDesignProps.getHeaderTitleMarginLeftInteger());
            whdHeaderTitleMarginRightBox.setValue(openedDesignProps.getHeaderTitleMarginRightInteger());
            updateHeaderControlPreviews();

            // Logo 1
            File log1 = getSavedLogo1(design_name);
            AImage img1 = (log1.exists()) ? new AImage(log1) : null;
            boolean has_logo1 = openedDesignProps.isLogo1Visible();
            boolean is_blank1 = (img1 == null) || ((img1.getWidth() <= 1) && (img1.getHeight() <= 1));
            whdLogo1Checkbox.setChecked(has_logo1);
            whdLogo1Area.setVisible(has_logo1);
            whdLogo1Image.setContent(img1);
            WhdUtils.selectListItem(whdLogo1PositionBox, openedDesignProps.getLogo1Position());
            whdLogo1WidthBox.setValue(openedDesignProps.getLogo1WidthInteger());
            whdLogo1HeightBox.setValue(openedDesignProps.getLogo1HeightInteger());
            whdLogo1XPosBox.setValue(openedDesignProps.getLogo1PosXInteger());
            whdLogo1YPosBox.setValue(openedDesignProps.getLogo1PosYInteger());
            whdLogo1DownloadBtn.setDisabled(is_blank1);
            whdLogo1DeleteBtn.setDisabled(is_blank1);
       
            // Logo 2
            File log2 = getSavedLogo2(design_name);
            AImage img2 = (log2.exists()) ? new AImage(log2) : null;
            boolean has_logo2 = openedDesignProps.isLogo2Visible();
            boolean is_blank2 = (img2 == null) || ((img2.getWidth() <= 1) && (img2.getHeight() <= 1));
            whdLogo2Checkbox.setChecked(has_logo2);
            whdLogo2Area.setVisible(has_logo2);
            whdLogo2Image.setContent(img2);
            WhdUtils.selectListItem(whdLogo2PositionBox, openedDesignProps.getLogo2Position());
            whdLogo2WidthBox.setValue(openedDesignProps.getLogo2WidthInteger());
            whdLogo2HeightBox.setValue(openedDesignProps.getLogo2HeightInteger());
            whdLogo2XPosBox.setValue(openedDesignProps.getLogo2PosXInteger());
            whdLogo2YPosBox.setValue(openedDesignProps.getLogo2PosYInteger());
            whdLogo2DownloadBtn.setDisabled(is_blank2);
            whdLogo2DeleteBtn.setDisabled(is_blank2);
       
            // Navigation
            whdNavigationWidthBox.setValue(openedDesignProps.getNavigationWidthInteger());
            whdNavigationMarginTopBox.setValue(openedDesignProps.getNavigationMarginTopInteger());
            whdNavigationMarginBottomBox.setValue(openedDesignProps.getNavigationMarginBottomInteger());
            whdNavigationPaddingTopBox.setValue(openedDesignProps.getNavigationPaddingTopInteger());
            whdNavigationPaddingBottomBox.setValue(openedDesignProps.getNavigationPaddingBottomInteger());
            whdNavigationPaddingLeftBox.setValue(openedDesignProps.getNavigationPaddingLeftInteger());
            whdNavigationPaddingRightBox.setValue(openedDesignProps.getNavigationPaddingRightInteger());
            WhdUtils.selectListItem(whdNavTreeAnimationBox, openedDesignProps.getNavigationTreeAnimation());
            whdNavTreeShowTitleBox.setChecked(openedDesignProps.isNavigationTreeTitleVisible());
            whdNavTreeCollapsedBox.setChecked(openedDesignProps.isNavigationTreeCollapsed());
            whdNavTreeAutoCloseBox.setChecked(openedDesignProps.isNavigationTreeAutoClose());
            File cont_icon_file = getSavedTreeviewContentIcon(design_name);
            AImage cont_icon = cont_icon_file.exists() ? new AImage(cont_icon_file) : null;
            whdNavTreeContentImage.setContent(cont_icon);
            whdNavTreeContentImgWidthBox.setValue(openedDesignProps.getNavigationContentIconWidthInteger());
            boolean is_blank_cont_icon = (cont_icon == null) || ((cont_icon.getWidth() <= 1) && (cont_icon.getHeight() <= 1));
            whdNavTreeContentImgDownloadBtn.setDisabled(is_blank_cont_icon);
            whdNavTreeContentImgDeleteBtn.setDisabled(is_blank_cont_icon);
            File tree_icons = getSavedTreeviewIcons(design_name);
            whdNavTreeOpenCloseImage.setContent(tree_icons.exists() ? new AImage(tree_icons) : null);
            whdNavTreeOpenCloseImage.setWidth("100px");
            File tree_line = getSavedTreeviewLine(design_name);
            whdNavTreeLineImage.setContent(tree_line.exists() ? new AImage(tree_line) : null);
            // whdNavTreeLineImage.setWidth("100px");
            File tog_file = getSavedDesignFile(design_name, openedDesignProps.getNavigationToggleButtonImage());
            AImage toggle_img = (tog_file != null) ? new AImage(tog_file) : null;
            whdNavigationToggleImage.setContent(toggle_img);
            whdNavigationToggleWidthBox.setValue(openedDesignProps.getNavigationToggleButtonWidthInteger());
            boolean is_blank_toggle = (toggle_img == null) || ((toggle_img.getWidth() <= 1) && (toggle_img.getHeight() <= 1));
            whdNavigationToggleDownloadBtn.setDisabled(is_blank_toggle);
            whdNavigationToggleDeleteBtn.setDisabled(is_blank_toggle);
            whdNavResizeCheckbox.setChecked(openedDesignProps.isNavigationResizeEnabled());
            whdNavAccordionCheckbox.setChecked(openedDesignProps.isAccordionEnabled());
            whdNavAccordionPaddingXBox.setValue(openedDesignProps.getAccordionPaddingXInteger());
            whdNavAccordionHeightBox.setValue(openedDesignProps.getAccordionHeightInteger());
            whdNavAccordionSpacingBox.setValue(openedDesignProps.getAccordionSpacingInteger());
            updateNavigationControls();

            // Part Tabs
            whdPartTabsMarginTopBox.setValue(openedDesignProps.getPartTabsMarginTopInteger());
            whdPartTabsMarginLeftBox.setValue(openedDesignProps.getPartTabsMarginLeftInteger());
            whdPartTabHeightBox.setValue(openedDesignProps.getPartTabHeightInteger());
            whdPartTabSpacingBox.setValue(openedDesignProps.getPartTabSpacingInteger());
            whdPartTabCurrentBox.setChecked(openedDesignProps.isPartTabBgCurrentEnabled());
            whdPartTabHoverBox.setChecked(openedDesignProps.isPartTabBgHoverEnabled());
            updatePartControlPreviews();
            
            // Breadcrumbs Navigation
            whdBreadcrumbsMarginTopBox.setValue(openedDesignProps.getBreadcrumbsMarginTopInteger());
            whdBreadcrumbsMarginBottomBox.setValue(openedDesignProps.getBreadcrumbsMarginBottomInteger());
            whdBreadcrumbsMarginLeftBox.setValue(openedDesignProps.getBreadcrumbsMarginLeftInteger());
            whdBreadcrumbsMarginRightBox.setValue(openedDesignProps.getBreadcrumbsMarginRightInteger());
            whdBreadcrumbsPaddingTopBox.setValue(openedDesignProps.getBreadcrumbsPaddingTopInteger());
            whdBreadcrumbsPaddingBottomBox.setValue(openedDesignProps.getBreadcrumbsPaddingBottomInteger());
            whdBreadcrumbsPaddingLeftBox.setValue(openedDesignProps.getBreadcrumbsPaddingLeftInteger());
            whdBreadcrumbsPaddingRightBox.setValue(openedDesignProps.getBreadcrumbsPaddingRightInteger());
            File bsep_file = getSavedDesignFile(design_name, openedDesignProps.getBreadcrumbsSeparatorImage());
            AImage bsep_img = (bsep_file != null) ? new AImage(bsep_file) : null;
            whdBreadcrumbsSeparatorImage.setContent(bsep_img);
            whdBreadcrumbsSepImageWidthBox.setValue(openedDesignProps.getBreadcrumbsSeparatorImageWidthInteger());
            boolean is_blank_bsep = (bsep_img == null) || ((bsep_img.getWidth() <= 1) && (bsep_img.getHeight() <= 1));
            whdBreadcrumbsSepImageDownloadBtn.setDisabled(is_blank_bsep);
            whdBreadcrumbsSepImageDeleteBtn.setDisabled(is_blank_bsep);
            updateBreadcrumbsControlPreviews();
            
            // Local Navigation (previous, up, next)
            whdNavLocalHeightBox.setValue(openedDesignProps.getLocalNavigationHeightInteger());
            whdNavLocalMarginTopBox.setValue(openedDesignProps.getLocalNavigationMarginTopInteger());
            whdNavLocalPaddingTopBox.setValue(openedDesignProps.getLocalNavigationPaddingTopInteger());
            whdNavLocalPaddingBottomBox.setValue(openedDesignProps.getLocalNavigationPaddingBottomInteger());
            whdNavLocalPaddingLeftBox.setValue(openedDesignProps.getLocalNavigationPaddingLeftInteger());
            whdNavLocalPaddingRightBox.setValue(openedDesignProps.getLocalNavigationPaddingRightInteger());
            whdNavLocalShowTextBox.setChecked(openedDesignProps.isLocalNavigationTextVisible());
            whdNavLocalShowIconsBox.setChecked(openedDesignProps.isLocalNavigationIconsVisible());
            whdNavLocalIconsHeightBox.setValue(openedDesignProps.getLocalNavigationIconsHeightInteger());
            whdNavLocalIconSelectBox.clearSelection();
            File nav_prev = getSavedPrevIcon(design_name);
            whdNavLocalPrevImage.setContent(nav_prev.exists() ? new AImage(nav_prev) : null);
            whdNavLocalPrevImage.setSclass("");
            File nav_next = getSavedNextIcon(design_name);
            whdNavLocalNextImage.setContent(nav_next.exists() ? new AImage(nav_next) : null);
            whdNavLocalNextImage.setSclass("");
            File nav_up = getSavedUpIcon(design_name);
            whdNavLocalUpImage.setContent(nav_up.exists() ? new AImage(nav_up) : null);
            whdNavLocalUpImage.setSclass("");
            File nav_home = getSavedHomeIcon(design_name);
            whdNavLocalHomeImage.setContent(nav_home.exists() ? new AImage(nav_home) : null);
            whdNavLocalHomeImage.setSclass("");
            File nav_toc = getSavedTocIcon(design_name);
            whdNavLocalTocImage.setContent(nav_toc.exists() ? new AImage(nav_toc) : null);
            whdNavLocalTocImage.setSclass("");
            whdNavSeparatorEnabledBox.setChecked(openedDesignProps.isLocalNavigationSeparatorVisible());
            updateLocalNavigationControlPreviews();
            
            // Search 
            String uimode = openedDesignProps.getSearchUIMode();
            WhdUtils.selectListItem(whdSearchUIModeBox, uimode);
            WhdUtils.selectListItem(whdSearchPositionBox, openedDesignProps.getSearchPosition());
            updateSearchModeVisibility(uimode);
            whdSearchWidthBox.setValue(openedDesignProps.getSearchWidthInteger());
            whdSearchHeightBox.setValue(openedDesignProps.getSearchHeightInteger());
            whdSearchXPosBox.setValue(openedDesignProps.getSearchPosXInteger());
            whdSearchYPosBox.setValue(openedDesignProps.getSearchPosYInteger());
            whdSearchInnerTopOffsetBox.setValue(openedDesignProps.getSearchInnerTopOffsetInteger());
            whdSearchInnerLeftOffsetBox.setValue(openedDesignProps.getSearchInnerLeftOffsetInteger());
            whdSearchInnerRightOffsetBox.setValue(openedDesignProps.getSearchInnerRightOffsetInteger());
            whdSearchBtnShowTextBox.setChecked(openedDesignProps.isSearchButtonTextVisible());
            whdSearchBtnWidthBox.setValue(openedDesignProps.getSearchButtonWidthInteger());
            whdSearchBtnHeightBox.setValue(openedDesignProps.getSearchButtonHeightInteger());
            File search_tog_file = getSavedDesignFile(design_name, openedDesignProps.getSearchToggleButtonImage());
            whdSearchToggleImage.setContent((search_tog_file != null) ? new AImage(search_tog_file) : null);
            whdSearchToggleWidthBox.setValue(openedDesignProps.getSearchToggleButtonWidthInteger());
            File sclose_file = getSavedDesignFile(design_name, openedDesignProps.getSearchCloseImage());
            whdSearchCloseImage.setContent((sclose_file != null) ? new AImage(sclose_file) : null);
            updateSearchControlPreviews();
            
            // Search Tab
            whdSearchTabHeightBox.setValue(openedDesignProps.getSearchTabHeightInteger());
            whdSearchTabHoverBox.setChecked(openedDesignProps.isSearchTabBgHoverEnabled());
            whdSearchPanelPaddingTopBox.setValue(openedDesignProps.getSearchPanelPaddingTopInteger());
            whdSearchPanelPaddingBottomBox.setValue(openedDesignProps.getSearchPanelPaddingBottomInteger());
            whdSearchPanelPaddingLeftBox.setValue(openedDesignProps.getSearchPanelPaddingLeftInteger());
            whdSearchPanelPaddingRightBox.setValue(openedDesignProps.getSearchPanelPaddingRightInteger());
            updateSearchTabControlPreviews();
            
            disableSave(true);  // Save button will be enabled as soon as user changes some settings
            clearChangedAndRemovedFiles();
            
            // Show preview
            // String old_url = whdPreviewFrame.getSrc();
            updatePreviewUrl();
            // if ((old_url != null) && old_url.equals(url)) {
            //     refreshPreview();
            // }
        } catch (Exception ex) {
            ex.printStackTrace();
            Messagebox.show("Error: " + ex.getMessage());
        }
    }
    
    private boolean isOpenedDesignUnsaved()
    {
        return (openedDesignName != null) && (! whdSaveBtn.isDisabled());
    }
    
    private void confirmDiscard(EventListener listener)
    {
        Messagebox.show("Design '" + openedDesignName + "' has been modified. Discard changes?", 
            "Discard changes?", Messagebox.YES | Messagebox.NO, 
            Messagebox.QUESTION, listener
        );
    }

    private void confirmCloseCurrentDesignOpenOther(final String designToOpen)
    {
        if (isOpenedDesignUnsaved()) {
            confirmDiscard(
                new EventListener() {
                    public void onEvent(Event evt) throws Exception {
                        if ("onYes".equalsIgnoreCase(evt.getName())) {
                            closeCurrentDesignOpenOther(designToOpen);
                        } else {
                            // Keep current design open
                            WhdUtils.selectListItem(whdSelectList, openedDesignName);
                        }
                    }
                }
            );
        } else {
            closeCurrentDesignOpenOther(designToOpen);
        }
    }
    
    private void closeCurrentDesignOpenOther(String designToOpen)
    {
        WhdUtils.selectListItem(whdSelectList, (designToOpen == null) ? "" : designToOpen);
        if ((designToOpen != null) && (designToOpen.length() > 0)) {
            openDesign(designToOpen);
        } else {
            closeDesign();
        }
    }
    
    private void deleteDesign(String design_name)
    {
        if ((openedDesignName != null) && openedDesignName.equals(design_name)) {
            closeDesign();
        }
        File delDir = getWebhelpSavedDesignDir(design_name);
        if (delDir.exists()) {
            if (! WhdUtils.recursiveFileDelete(delDir)) {
                Messagebox.show("Error: Some files in '" + delDir.getAbsolutePath() + "' could not be deleted!");
                return;
            }
        } else {
            Messagebox.show("Directory '" + delDir.getAbsolutePath() + "' not found!");
            return;
        }
        WhdUtils.removeListItem(whdSelectList, design_name);
        WhdUtils.selectListItem(whdSelectList, "");
        Messagebox.show("Design '" + design_name + "' has been deleted!");
    }
    
    private void closeDesign()
    {
        openedDesignName = null;
        openedDesignProps = null;
        openedPreviewDir = null;
        setDesignerControlsVisible(false);
        whdPreviewFrame.setSrc("preview_empty.html");
    }
    
    private void saveOpenedDesignAs(String new_name) 
    {
        if ((openedDesignName == null) || openedDesignName.equals("")) {
            return;
        }
        File src_dir = getWebhelpSavedDesignDir(openedDesignName);
        File dest_dir = getWebhelpSavedDesignDir(new_name);
        WhdUtils.recursiveFileCopy(src_dir, dest_dir, true);
        try {
            openedDesignProps.saveAs(new File(dest_dir, FILENAME_DESIGNER_PROPS));
            openDesign(new_name);
        } catch (Exception ex) {
            closeDesign();
            Messagebox.show("Error: " + ex.getLocalizedMessage());
        }
    }
    
    private void preparePreview() throws IOException
    {
        File preview_base_dir = getPreviewBaseDir();
        if (preview_base_dir.exists()) {
            WhdUtils.clearDirectory(preview_base_dir, getRandomPreviewDirPrefix());
        } else {
            preview_base_dir.mkdir();
        }
        openedPreviewDir = new File(preview_base_dir, getRandomPreviewDirName());
        WhdUtils.recursiveFileCopy(getWebhelpExamplesDir(), openedPreviewDir, true);
        
        // Update positioning.css and main.js files
        writePreviewConfigFiles();
        
        writePreviewImageFiles();
    }
    
    private boolean renameOpenedPreviewDir()
    {
        String old_name = openedPreviewDir.getName();
        String new_name = getRandomPreviewDirName();
        File ren_file = new File(getPreviewBaseDir(), new_name);
        if (openedPreviewDir.renameTo(ren_file)) {
            openedPreviewDir = ren_file;
            changeFilePathsInList(changedFiles, old_name, new_name);
            changeFilePathsInList(removedFiles, old_name, new_name);
            return true;
        } else {
            return false;
        }
    }
    
    private void changeFilePathsInList(List<File> flist, String old_name, String new_name)
    {
        for (int i = 0; i < flist.size(); i++) {
            String path = flist.get(i).getPath();
            String matchstr = File.separator + old_name.toLowerCase() + File.separator;
            int p = path.toLowerCase().lastIndexOf(matchstr);
            if (p >= 0) {
                path = path.substring(0, p) + File.separator + new_name + File.separator + path.substring(p + matchstr.length());
                flist.set(i, new File(path));
            }
        }
    }
    
    private String getRandomPreviewDirName()
    {
        String stamp = Long.toString(System.currentTimeMillis());
        if (stamp.length() > 10) {
            stamp = stamp.substring(stamp.length() - 10);
        }
        return getRandomPreviewDirPrefix() + stamp;
    }
    
    private String getRandomPreviewDirPrefix()
    {
        return loginUserId + "_pv";
    }
    
    private void writePreviewImageFiles() throws IOException
    {
        File fav = getSavedFavicon(openedDesignName);
        File logo1 = getSavedLogo1(openedDesignName);
        File logo2 = getSavedLogo2(openedDesignName);
        File treeline = getSavedTreeviewLine(openedDesignName);
        File treeicons = getSavedTreeviewIcons(openedDesignName);
        File tree_cont_icon = getSavedTreeviewContentIcon(openedDesignName);
        File head_bgimage = getSavedDesignFile(openedDesignProps.getHeaderBgImage());
        File nav_bgimage = getSavedDesignFile(openedDesignProps.getNavigationBgImage());
        File tree_bgimage = getSavedDesignFile(openedDesignProps.getNavigationTreeBgImage());
        File toggle_image = getSavedDesignFile(openedDesignProps.getNavigationToggleButtonImage());
        File accord_bgimage = getSavedDesignFile(openedDesignProps.getAccordionBgImage());
        File tab_bgimage = getSavedDesignFile(openedDesignProps.getPartTabBgImage());
        File tab_bgimage_curr = getSavedDesignFile(openedDesignProps.getPartTabBgImageCurrent());
        File tab_bgimage_hover = getSavedDesignFile(openedDesignProps.getPartTabBgImageHover());
        File local_nav_bg = getSavedDesignFile(openedDesignProps.getLocalNavigationBgImage());
        File local_nav_prev = getSavedPrevIcon(openedDesignName);
        File local_nav_next = getSavedNextIcon(openedDesignName);
        File local_nav_up   = getSavedUpIcon(openedDesignName);
        File local_nav_home = getSavedHomeIcon(openedDesignName);
        File local_nav_toc  = getSavedTocIcon(openedDesignName);
        File bread_bg = getSavedDesignFile(openedDesignProps.getBreadcrumbsBgImage());
        File bread_sep = getSavedDesignFile(openedDesignProps.getBreadcrumbsSeparatorImage());
        File search_bg = getSavedDesignFile(openedDesignProps.getSearchBgImage());
        File search_inner_bg = getSavedDesignFile(openedDesignProps.getSearchInnerBgImage());
        File search_input_bg = getSavedDesignFile(openedDesignProps.getSearchInputBgImage());
        File search_btn_bg = getSavedDesignFile(openedDesignProps.getSearchButtonBgImage());
        File search_tog_img = getSavedDesignFile(openedDesignProps.getSearchToggleButtonImage());
        File search_close_img = getSavedDesignFile(openedDesignProps.getSearchCloseImage());
        File search_tab_bg = getSavedDesignFile(openedDesignProps.getSearchTabBgImage());
        File search_tab_active = getSavedDesignFile(openedDesignProps.getSearchTabBgImageActive());
        File search_tab_hover = getSavedDesignFile(openedDesignProps.getSearchTabBgImageHover());
        File searchpanel_bg = getSavedDesignFile(openedDesignProps.getSearchPanelBgImage());
        
        updatePreviewFile(fav, fav.getName());
        updatePreviewFile(logo1, webhelpCommonImagePath() + File.separator + logo1.getName());
        updatePreviewFile(logo2, webhelpCommonImagePath() + File.separator + logo2.getName());
        updatePreviewFile(treeline, webhelpTreeviewImagePath() + File.separator + DEPLOY_FILENAME_TREE_LINE);
        updatePreviewFile(treeicons, webhelpTreeviewImagePath() + File.separator + DEPLOY_FILENAME_TREE_ICONS);
        updatePreviewFile(tree_cont_icon, webhelpTreeviewImagePath() + File.separator + DEPLOY_FILENAME_TREE_CONTENT_ICON);
        if (head_bgimage != null) {
            updatePreviewFile(head_bgimage, webhelpCommonImagePath() + File.separator + head_bgimage.getName());
        }
        if (nav_bgimage != null) {
            updatePreviewFile(nav_bgimage, webhelpCommonImagePath() + File.separator + nav_bgimage.getName());
        }
        if (tree_bgimage != null) {
            updatePreviewFile(tree_bgimage, webhelpCommonImagePath() + File.separator + tree_bgimage.getName());
        }
        if (toggle_image != null) {
            updatePreviewFile(toggle_image, webhelpCommonImagePath() + File.separator + toggle_image.getName());
        }
        if (accord_bgimage != null) {
            updatePreviewFile(accord_bgimage, webhelpCommonImagePath() + File.separator + accord_bgimage.getName());
        }
        if (tab_bgimage != null) {
            updatePreviewFile(tab_bgimage, webhelpCommonImagePath() + File.separator + tab_bgimage.getName());
        }
        if (tab_bgimage_curr != null) {
            updatePreviewFile(tab_bgimage_curr, webhelpCommonImagePath() + File.separator + tab_bgimage_curr.getName());
        }
        if (tab_bgimage_hover != null) {
            updatePreviewFile(tab_bgimage_hover, webhelpCommonImagePath() + File.separator + tab_bgimage_hover.getName());
        }
        if (local_nav_bg != null) {
            updatePreviewFile(local_nav_bg, webhelpCommonImagePath() + File.separator + local_nav_bg.getName());
        }
        if (local_nav_prev != null) {
            updatePreviewFile(local_nav_prev, webhelpCommonImagePath() + File.separator + local_nav_prev.getName());
        }
        if (local_nav_next != null) {
            updatePreviewFile(local_nav_next, webhelpCommonImagePath() + File.separator + local_nav_next.getName());
        }
        if (local_nav_up != null) {
            updatePreviewFile(local_nav_up, webhelpCommonImagePath() + File.separator + local_nav_up.getName());
        }
        if (local_nav_home != null) {
            updatePreviewFile(local_nav_home, webhelpCommonImagePath() + File.separator + local_nav_home.getName());
        }
        if (local_nav_toc != null) {
            updatePreviewFile(local_nav_toc, webhelpCommonImagePath() + File.separator + local_nav_toc.getName());
        }
        if (bread_bg != null) {
            updatePreviewFile(bread_bg, webhelpCommonImagePath() + File.separator + bread_bg.getName());
        }
        if (bread_sep != null) {
            updatePreviewFile(bread_sep, webhelpCommonImagePath() + File.separator + bread_sep.getName());
        }
        if (search_bg != null) {
            updatePreviewFile(search_bg, webhelpCommonImagePath() + File.separator + search_bg.getName());
        }
        if (search_inner_bg != null) {
            updatePreviewFile(search_inner_bg, webhelpCommonImagePath() + File.separator + search_inner_bg.getName());
        }
        if (search_input_bg != null) {
            updatePreviewFile(search_input_bg, webhelpCommonImagePath() + File.separator + search_input_bg.getName());
        }
        if (search_btn_bg != null) {
            updatePreviewFile(search_btn_bg, webhelpCommonImagePath() + File.separator + search_btn_bg.getName());
        }
        if (search_tog_img != null) {
            updatePreviewFile(search_tog_img, webhelpCommonImagePath() + File.separator + DEPLOY_FILENAME_SEARCH_HIGHLIGHT_BUTTON);
        }
        if (search_close_img != null) {
            updatePreviewFile(search_close_img, webhelpCommonImagePath() + File.separator + search_close_img.getName());
        }
        if (search_tab_bg != null) {
            updatePreviewFile(search_tab_bg, webhelpCommonImagePath() + File.separator + search_tab_bg.getName());
        }
        if (search_tab_active != null) {
            updatePreviewFile(search_tab_active, webhelpCommonImagePath() + File.separator + search_tab_active.getName());
        }
        if (search_tab_hover != null) {
            updatePreviewFile(search_tab_hover, webhelpCommonImagePath() + File.separator + search_tab_hover.getName());
        }
        if (searchpanel_bg != null) {
            updatePreviewFile(searchpanel_bg, webhelpCommonImagePath() + File.separator + searchpanel_bg.getName());
        }
    }
    
    private void writePreviewConfigFiles() throws IOException
    {
        String pos_css = getPositioningContent(openedDesignProps);
        String main_js = getJsMainContent(openedDesignProps);
        File[] preview_dirs = new File[] {
            getPartsPreviewDir(), 
            getNoPartsPreviewDir()
        };
        
        // Write positioning.css
        for (File pdir : preview_dirs) {
            File fout = new File(pdir, webhelpCommonCssPath() + File.separator + "positioning.css");
            WhdUtils.writeStringToFile(pos_css, fout, "UTF-8");
        }
        
        // Write main.js
        for (File pdir : preview_dirs) {
            File fout = new File(pdir, "common" + File.separator + "main.js");
            WhdUtils.writeStringToFile(main_js, fout, "UTF-8");
        }
    }

    private void updateHeaderControlPreviews()
    {
        whdHeaderBgPrev.setStyle(getHeaderBgPreviewCSS());
        // whdHeaderBgPrev.invalidate();  // assure refresh even if CSS string did not change (background-image may have changed, though filename is same)
        String bgcol = "background-color:" + openedDesignProps.getHeaderBgColor() + ";";
        whdHeaderTitle1FontPrev.setStyle(bgcol + openedDesignProps.getHeaderTitle1FontCSS());
        whdHeaderTitle2FontPrev.setStyle(bgcol + openedDesignProps.getHeaderTitle2FontCSS());
    }
    
    private void updateNavigationControls()
    {
        whdNavigationBgPrev.setStyle(getNavigationBgPreviewCSS());
        // whdNavigationBgPrev.invalidate(); // assure refresh even if CSS string did not change (background-image may have changed, though filename is same)
        boolean is_accordion = openedDesignProps.isAccordionEnabled();
        whdNavAccordionBgBtn.setDisabled(! is_accordion);
        whdNavAccordionFontBtn.setDisabled(! is_accordion);
        whdNavAccordionPaddingXBox.setDisabled(! is_accordion);
        whdNavAccordionHeightBox.setDisabled(! is_accordion);
        whdNavAccordionSpacingBox.setDisabled(! is_accordion);
        whdNavAccordionBgPrev.setStyle(getAccordionBgPreviewCSS());
        whdNavAccordionFontPrev.setStyle(openedDesignProps.getAccordionFontCSS());
        whdNavigationTreeBgPrev.setStyle(getNavigationTreeBgPreviewCSS());
        // whdNavigationTreeBgPrev.invalidate(); // assure refresh even if CSS string did not change (background-image may have changed, though filename is same)
        String bgcol = "background-color:" + openedDesignProps.getNavigationBgColor() + ";";
        whdNavigationFontPrev.setStyle(bgcol + openedDesignProps.getNavigationTreeFontCSS());
        whdNavigationFontHoverPrev.setStyle(bgcol + openedDesignProps.getNavigationTreeFontHoverCSS());
        whdNavigationFontCurrentPrev.setStyle(bgcol + openedDesignProps.getNavigationTreeFontCurrentCSS());
        whdNavigationFontTitlePrev.setStyle(bgcol + openedDesignProps.getNavigationTreeFontTitleCSS());
    }

    private void updatePartControlPreviews()
    {
        String bg_def = getPartTabBgPreviewCSSDefault() + openedDesignProps.getPartTabFontCSS();
        String bg_curr = getPartTabBgPreviewCSSCurrent() + openedDesignProps.getPartTabFontCSSCurrent();
        String bg_hover = getPartTabBgPreviewCSSHover() + openedDesignProps.getPartTabFontCSSHover();
        
        whdPartTabBgPrev.setStyle(bg_def);
        // whdPartTabBgPrev.invalidate(); // assure refresh even if CSS string did not change (background-image may have changed, though filename is same)
        whdPartTabBgCurrentPrev.setStyle(bg_curr);
        // whdPartTabBgCurrentPrev.invalidate();
        whdPartTabBgHoverPrev.setStyle(bg_hover);
        // whdPartTabBgHoverPrev.invalidate();
        
        whdPartTabFontPrev.setStyle(bg_def);
        whdPartTabFontCurrentPrev.setStyle(bg_curr);
        whdPartTabFontHoverPrev.setStyle(bg_hover);
    }
    
    private void updateLocalNavigationControlPreviews()
    {
        String bg = getLocalNavigationBgPreviewCSS();
        whdNavLocalBgPrev.setStyle(bg);
        whdNavLocalFontPrev.setStyle(bg + openedDesignProps.getLocalNavigationLinkCSS());
        whdNavLocalFontHoverPrev.setStyle(bg + openedDesignProps.getLocalNavigationLinkHoverCSS());
        whdNavSeparatorFontPrev.setStyle(bg + openedDesignProps.getLocalNavigationSeparatorCSS());
    }

    private void updateBreadcrumbsControlPreviews()
    {
        String bg = getBreadcrumbsBgPreviewCSS();
        whdBreadcrumbsBgPrev.setStyle(bg);
        whdBreadcrumbsFontPrev.setStyle(bg + openedDesignProps.getBreadcrumbsLinkCSS());
        whdBreadcrumbsFontLastPrev.setStyle(bg + openedDesignProps.getBreadcrumbsLastCSS());
        whdBreadcrumbsFontHoverPrev.setStyle(bg + openedDesignProps.getBreadcrumbsHoverCSS());
        whdBreadcrumbsFontSepPrev.setStyle(bg + openedDesignProps.getBreadcrumbsSeparatorCSS());
    }

    private void updateSearchControlPreviews()
    {
        whdSearchBgPrev.setStyle(getSearchBgPreviewCSS());
        // whdSearchBgPrev.invalidate(); // assure refresh even if CSS string did not change (background-image may have changed, though filename is same)
        whdSearchInnerBgPrev.setStyle(getSearchInnerBgPreviewCSS());
        whdSearchInputBgPrev.setStyle(getSearchInputBgPreviewCSS());
        whdSearchBtnBgPrev.setStyle(getSearchButtonPreviewCSS());

        String box_bg = "background-color:" + openedDesignProps.getSearchBgColor() + ";";
        // String inp_bg = "background-color:" + openedDesignProps.getSearchInputBgColor() + ";";
        // String res_bg = "background-color:" + openedDesignProps.getNavigationTreeBgColor() + ";";
        whdSearchLegendFontPrev.setStyle(box_bg + openedDesignProps.getSearchLegendFontCSS());
        whdSearchInputFontPrev.setStyle(openedDesignProps.getSearchInputFontCSS());
        whdSearchTitleFontPrev.setStyle(openedDesignProps.getSearchTitleFontCSS());
        whdSearchExpressionFontPrev.setStyle(openedDesignProps.getSearchExpressionFontCSS());
        whdSearchResultFontPrev.setStyle(openedDesignProps.getSearchResultFontCSS());
        whdSearchHitFontPrev.setStyle(openedDesignProps.getSearchHitFontCSS());
        whdSearchCloseFontPrev.setStyle(openedDesignProps.getSearchCloseFontCSS());

        boolean has_txt = openedDesignProps.isSearchButtonTextVisible();
        String btn_css = has_txt ? openedDesignProps.getSearchButtonFontCSS() : "";
        String btn_height = openedDesignProps.getSearchButtonHeight();
        if (has_txt && (btn_height != null) && (btn_height.length() > 0)) {
            btn_css = "line-height:" + btn_height + "; " + btn_css;
        }
        whdSearchBtnFontPrev.setStyle(btn_css);
        whdSearchBtnFontPrev.setVisible(has_txt);
    }
    
    private void updateSearchTabControlPreviews()
    {
        String bg_def = getSearchTabBgPreviewCSS() + openedDesignProps.getSearchTabFontCSS();
        String bg_act = getSearchTabBgPreviewCSSActive() + openedDesignProps.getSearchTabFontCSSActive();
        String bg_hov = getSearchTabBgPreviewCSSHover() + openedDesignProps.getSearchTabFontCSSHover();
        
        whdSearchTabBgPrev.setStyle(bg_def);
        whdSearchTabBgActivePrev.setStyle(bg_act);
        whdSearchTabBgHoverPrev.setStyle(bg_hov);
        
        whdSearchTabFontPrev.setStyle(bg_def);
        whdSearchTabFontActivePrev.setStyle(bg_act);
        whdSearchTabFontHoverPrev.setStyle(bg_hov);
        
        whdSearchPanelBgPrev.setStyle(getSearchPanelBgPreviewCSS());
    }

    private void updateSearchModeVisibility(String mode)
    {
        boolean is_fixed = "fixed".equalsIgnoreCase(mode);
        whdSearchPositionBox.setDisabled(! is_fixed);
        whdSearchPositionBox.setVisible(is_fixed);
        whdSearchXYPosArea.setVisible(is_fixed);
        boolean is_tab = "tab".equalsIgnoreCase(mode);
        // whdSearchTabArea.setDisabled(! is_tab);
        whdSearchTabArea.setVisible(is_tab);
    }

    private void updateWebhelpSelectionList()
    {
        if (whdSelectList.getItemCount() > 0) {
            whdSelectList.getItems().clear();
        }
        
        File f = getWebhelpSavedDir();
        String[] farr = f.list();
        Arrays.sort(farr);
        for (String nm : farr) {
            if (new File(f, nm).isDirectory()) {
                whdSelectList.appendItem(nm, nm);
            }
        }
        whdSelectList.appendItem("", "");

        WhdUtils.selectListItem(whdSelectList, (openedDesignName != null) ? openedDesignName : "");
    }

    private void initLayoutIdListbox(Listbox listbox) 
    {
        if (listbox.getItemCount() > 0) {
            listbox.getItems().clear();
        }
        File f = getWebhelpLayoutsDir();
        String[] farr = f.list();
        Arrays.sort(farr);
        for (String nm : farr) {
            if (new File(f, nm).isDirectory()) {
                listbox.appendItem(nm, nm);
            }
        }
    }
    
    private void initLayoutVersionListbox(String layout_id, Listbox listbox)
    {
        if (listbox.getItemCount() > 0) {
            listbox.getItems().clear();
        }
        File dir = new File(getWebhelpLayoutsDir(), layout_id);
        if (dir.exists() && dir.isDirectory()) {
            String[] subdirs = dir.list();
            Arrays.sort(subdirs);
            for (String vn : subdirs) {
                if (vn.startsWith("v")) {
                    String v = vn.substring(1);
                    listbox.appendItem(v, v);
                }
            }
        }
    }
    
    private void disableSave(boolean disable) 
    {
        whdSaveBtn.setDisabled(disable);   // Save button will be enabled as soon as user changes some settings
        if (disable) {
            whdDesignNameLabel.setValue(openedDesignName);
        } else {
            whdDesignNameLabel.setValue(openedDesignName + "*");
        }
    }
    
    private String savedName(String filename) 
    {
        if (DEPLOY_FILENAME_TREE_ICONS.equalsIgnoreCase(filename)) {
            return SAVED_FILENAME_TREE_ICONS;
        } else
        if (DEPLOY_FILENAME_TREE_LINE.equalsIgnoreCase(filename)) {
            return SAVED_FILENAME_TREE_LINE;
        } else
        if (DEPLOY_FILENAME_SEARCH_HIGHLIGHT_BUTTON.equalsIgnoreCase(filename)) {
            return openedDesignProps.getSearchToggleButtonImage();
        } else {
            return filename;  // Deployed filename is same as saved filename.
        }
    }
    
    private String deployedName(String filename)
    {
        if (SAVED_FILENAME_TREE_ICONS.equalsIgnoreCase(filename)) {
            return DEPLOY_FILENAME_TREE_ICONS;
        } else
        if (SAVED_FILENAME_TREE_LINE.equalsIgnoreCase(filename)) {
            return DEPLOY_FILENAME_TREE_LINE;
        } else
        if (filename.toLowerCase().startsWith(PREFIX_SEARCH_HIGHLIGHT_BUTTON.toLowerCase())) {
            return DEPLOY_FILENAME_SEARCH_HIGHLIGHT_BUTTON;
        } else {
            return filename;  // Deployed filename is same as saved filename.
        }
    }
    
    private void saveChangedFiles()
    {
        for (File remfile : removedFiles) {
            File target = getSavedDesignFile(openedDesignName, savedName(remfile.getName()));
            if ((target != null) && target.exists()) {
                target.delete();
            }
        }
        removedFiles.clear();
        for (File source : changedFiles) {
            File target = getSavedDesignFile(openedDesignName, savedName(source.getName()));
            WhdUtils.fileCopy(source, target, true);
        }
        changedFiles.clear();
    }
    
    private boolean isFileChanged(String saved_filename)
    {
        String dep_name = deployedName(saved_filename);
        boolean names_differ = !dep_name.equals(saved_filename);
        return containsFilename(changedFiles, dep_name) || 
               (names_differ && containsFilename(changedFiles, saved_filename));
    }
    
    private void addToChangedFiles(File afile)
    {
        // Note: the filename of afile needs to be the deployed filename
        if (afile == null) {
            return;
        }
        String fn = afile.getName();
        String saved_fn = savedName(fn);
        removeFromFileList(changedFiles, fn);
        removeFromFileList(removedFiles, fn);
        if (! fn.equals(saved_fn)) {
            removeFromFileList(removedFiles, saved_fn);
        }
        changedFiles.add(afile);
    }
    
    private void addToRemovedFiles(File afile)
    {
        // Note: the filename of afile needs to be the saved file
        if (afile == null) {
            return;
        }
        String fn = afile.getName();
        String deploy_fn = deployedName(fn);
        removeFromFileList(changedFiles, fn);
        if (! fn.equals(deploy_fn)) {
            removeFromFileList(changedFiles, deploy_fn);
        }
        removeFromFileList(removedFiles, fn);
        removedFiles.add(afile);
    }
    
    private void addToRemovedFiles(String filename)
    {
        // Note: the filename needs to be the saved filename
        if (filename != null && (filename.length() > 0)) {
            addToRemovedFiles(getSavedDesignFile(openedDesignName, filename));
        }
    }
    
    private void clearChangedAndRemovedFiles()
    {
        changedFiles.clear();
        removedFiles.clear();
    }
    
    private void removeFromFileList(List<File> alist, String filename)
    {
        Iterator<File> it = alist.iterator();
        while (it.hasNext()) {
            File f = it.next();
            if (filename.equals(f.getName())) it.remove();
        }
    }
    
    private boolean containsFilename(List<File> alist, String filename)
    {
        Iterator<File> it = alist.iterator();
        while (it.hasNext()) {
            File f = it.next();
            if (filename.equals(f.getName())) return true;
        }
        return false;
    }

    //********************************************************************
    //*****************   Preview Helper functions   *********************
    //********************************************************************

    private String webhelpCommonImageURL()
    {
        return "common/images";
    }
    
    private String webhelpCommonImagePath()
    {
        return "common" + File.separator + "images";
    }
    
    private String webhelpTreeviewImagePath()
    {
        return "common" + File.separator + "jquery" + File.separator + "treeview" + File.separator + "images";
    }
    
    private String webhelpCommonCssPath()
    {
        return "common" + File.separator + "css";
    }

    private File updatePreviewFile(File srcFile, String targetPath) throws IOException
    {
        File dest1 = new File(getPartsPreviewDir(), targetPath);
        File dest2 = new File(getNoPartsPreviewDir(), targetPath);
        if ((srcFile != null) && srcFile.exists()) {
            byte[] data = WhdUtils.readFile(srcFile);
            WhdUtils.writeBytesToFile(data, dest1);
            WhdUtils.writeBytesToFile(data, dest2);
            // WhdUtils.fileCopy(f, dest1, true);
            // WhdUtils.fileCopy(f, dest2, true);
        } else {
            if (dest1.exists()) dest1.delete();
            if (dest2.exists()) dest2.delete();
        }
        return dest1;
    }

    private File updatePreviewFile(byte[] data, String targetPath) throws IOException
    {
        File dest1 = new File(getPartsPreviewDir(), targetPath);
        File dest2 = new File(getNoPartsPreviewDir(), targetPath);
        if (data != null) {
            WhdUtils.writeBytesToFile(data, dest1);
            WhdUtils.writeBytesToFile(data, dest2);
            // WhdUtils.fileCopy(f, dest1, true);
            // WhdUtils.fileCopy(f, dest2, true);
        } else {
            if (dest1.exists()) dest1.delete();
            if (dest2.exists()) dest2.delete();
        }
        return dest1;
    }
    
    private File writeImageToPreviewDirs(org.zkoss.util.media.Media media, String filename) throws IOException
    {
        File prev1 = new File(getPartsPreviewDir(), webhelpCommonImagePath() + File.separator + filename);
        File prev2 = new File(getNoPartsPreviewDir(), webhelpCommonImagePath() + File.separator + filename);
        byte[] data = media.getByteData();
        WhdUtils.writeBytesToFile(data, prev1);
        WhdUtils.writeBytesToFile(data, prev2);
        return prev1;
    }
    
    private File writeImageToTempPreviewDir(org.zkoss.util.media.Media media, String filename) throws IOException
    {
        File dir = getTempPreviewDir();
        if (! dir.exists()) {
            dir.mkdirs();
        }
        File f = new File(dir, filename);
        WhdUtils.writeBytesToFile(media.getByteData(), f);
        return f;
    }
    
    private File getTempPreviewDir()
    {
        return new File(getPreviewBaseDir(), TEMP_UPLOAD_PATH);
    }
    
    private File getPartsPreviewDir()
    {
        return new File(openedPreviewDir, EXAMPLE_PATH_PARTS);
    }
    
    private File getNoPartsPreviewDir()
    {
        return new File(openedPreviewDir, EXAMPLE_PATH_NOPARTS);
    }
    
    private String getRelativePreviewURL(String sub_path)
    {
        if (! sub_path.startsWith("/")) {
            sub_path = "/" + sub_path;
        }
        String example_path = whdPreviewPartsCheckbox.isChecked() ? EXAMPLE_PATH_PARTS : EXAMPLE_PATH_NOPARTS ;
        return PREVIEW_PATH + "/" + openedPreviewDir.getName() + "/" + example_path + sub_path;
    }
    
    private String updatePreviewUrl() 
    {
        String start_path = whdPreviewPartsCheckbox.isChecked() ? EXAMPLE_PATH_PARTS + "/pt01.html" : 
                                                                  EXAMPLE_PATH_NOPARTS + "/ch01.html";
        String url = "./" + PREVIEW_PATH + "/" + openedPreviewDir.getName() + "/" + start_path;
        whdPreviewFrame.setSrc(url);
        return url;
    }

    private void refreshPreview()
    {
        Clients.evalJavaScript("window.frames['whdPreviewFrame'].location.reload();");
    }
    

    //********************************************************************
    //***********   Reading/Writing Webhelp-Design files   ***************
    //********************************************************************

    private boolean webhelpDesignExists(String nm)
    {
        return getWebhelpSavedDesignDir(nm).exists();
    }


    private void createWebhelpDesign(String name, String layout, String version)
    {
        if (! version.startsWith("v")) {
            version = "v" + version;
        }
        File layout_dir = getWebhelpLayoutDir(layout, version);
        if (layout_dir == null) {
            Messagebox.show("Unknown layout: " + layout + " " + version);
            return;
        }
        
        File target_dir = getWebhelpSavedDesignDir(name); // new File(getWebhelpSavedDir(), name);
        target_dir.mkdir();
        
        String[] src_files = layout_dir.list();
        for (String fn : src_files) {
            String ext = WhdUtils.getFilenameExtension(fn);
            if (ext != null) {
                String ext_low = ext.toLowerCase();
                if (WHD_MEDIA_EXTENSIONS.contains(ext_low) || ext_low.equals("properties")) {
                    WhdUtils.fileCopy(new File(layout_dir, fn), new File(target_dir, fn), true);
                }
            }
        }
        File fprop = new File(target_dir, FILENAME_DESIGNER_PROPS);
        try {
            WebDesignerProps props = new WebDesignerProps(fprop);
            props.setLayout(layout, version);
            props.save();
        } catch (IOException ex) {
            Messagebox.show("Error accessing file '" + fprop + "': " + ex.getMessage());
        }
    }


    private WebDesignerProps readDesignProps(String design_name) throws IOException
    {
        File dir = getWebhelpSavedDesignDir(design_name);
        File fprop = new File(dir, FILENAME_DESIGNER_PROPS);
        return new WebDesignerProps(fprop);
    }


    private File getSavedFavicon(String design_name) 
    {
        return getSavedDesignFile(design_name, DEPLOY_FILENAME_FAVICON);
    }

    private File getSavedLogo1(String design_name) 
    {
        return getSavedDesignFile(design_name, DEPLOY_FILENAME_LOGO1);
    }


    private File getSavedLogo2(String design_name) 
    {
        return getSavedDesignFile(design_name, DEPLOY_FILENAME_LOGO2);
    }

    private File getSavedTreeviewLine(String design_name) 
    {
        return getSavedDesignFile(design_name, SAVED_FILENAME_TREE_LINE);
    }

    private File getSavedTreeviewIcons(String design_name) 
    {
        return getSavedDesignFile(design_name, SAVED_FILENAME_TREE_ICONS);
    }

    private File getSavedTreeviewContentIcon(String design_name) 
    {
        return getSavedDesignFile(design_name, DEPLOY_FILENAME_TREE_CONTENT_ICON);
    }

    private File getSavedPrevIcon(String design_name) 
    {
        return getSavedDesignFile(design_name, DEPLOY_FILENAME_PREV);
    }

    private File getSavedNextIcon(String design_name) 
    {
        return getSavedDesignFile(design_name, DEPLOY_FILENAME_NEXT);
    }

    private File getSavedUpIcon(String design_name) 
    {
        return getSavedDesignFile(design_name, DEPLOY_FILENAME_UP);
    }

    private File getSavedHomeIcon(String design_name) 
    {
        return getSavedDesignFile(design_name, DEPLOY_FILENAME_HOME);
    }

    private File getSavedTocIcon(String design_name) 
    {
        return getSavedDesignFile(design_name, DEPLOY_FILENAME_TOC);
    }

    private File getSavedDesignFile(String filename) 
    {
        return getSavedDesignFile(openedDesignName, filename);
    }
    
    private File getSavedDesignFile(String design_name, String filename) 
    {
        if ((filename == null) || filename.equals("")) {
            return null;
        }
        return new File(getWebhelpSavedDesignDir(design_name), filename);
    }

    //********************************************************************
    //**********   Reading/Writing Webhelp Template files   **************
    //********************************************************************
    
    private File getPositioningTemplateFile(String layout, String version)
    {
        return new File(getWebhelpLayoutDir(layout, version), "positioning.css");
    }
    
    private File getJsConfigTemplateFile(String layout, String version)
    {
        return new File(getWebhelpLayoutDir(layout, version), "webhelp_config.js");
    }
    
    private File getJsMainTemplateFile(String layout, String version)
    {
        return new File (getDesignerResourcesDir(), "main.js"); 
        // new File(getWebhelpLayoutDir(layout, version), "main.js");
    }
    
    private String formatBgImage2(String othercss, String bgimage, String repeat) 
    {
        if (bgimage.length() > 0) {
            return formatBgImageCSS(othercss, bgimage, repeat);
        } else {
            // required to disable jquery ui default background
            return "background-image:none; " + ((othercss == null) ? "" : othercss);
        }
    }
    
    private String getPositioningContent(WebDesignerProps props) throws IOException
    {
        String layout = props.getLayoutId();
        String version = props.getLayoutVersion();
        
        String l1_pos = props.getLogo1Position();
        boolean is_bottom1 = l1_pos.contains("-bottom");
        boolean is_right1 = l1_pos.contains("right");
        int l1_x = props.getLogo1PosXInteger();
        int l1_y = props.getLogo1PosYInteger();
        String logo1_csspos = "position:" + getCSSPositionValue(l1_pos, l1_x, l1_y) + "; " +
                              (is_bottom1 ? "bottom:" : "top:") + props.getLogo1PosY() + "; " + 
                              (is_right1 ? "right:" : "left:") + props.getLogo1PosX() + "; ";
        
        String l2_pos = props.getLogo2Position();
        boolean is_bottom2 = l2_pos.contains("-bottom");
        boolean is_left2 = l2_pos.contains("left");
        int l2_x = props.getLogo2PosXInteger();
        int l2_y = props.getLogo2PosYInteger();
        String logo2_csspos = "position:" + getCSSPositionValue(l2_pos, l2_x, l2_y) + "; " +
                              (is_bottom2 ? "bottom:" : "top:") + props.getLogo2PosY() + "; " + 
                              (is_left2 ? "left:" : "right:") + props.getLogo2PosX() + "; ";
        
        String head_bgimage = props.getHeaderBgImage();
        if (head_bgimage.length() > 0) {
            head_bgimage = formatBgImageCSS(null, head_bgimage, props.getHeaderBgRepeat());
        }
        
        String nav_bgimage = props.getNavigationBgImage();
        if (nav_bgimage.length() > 0) {
            nav_bgimage = formatBgImageCSS(null, nav_bgimage, props.getNavigationBgRepeat());
        }

        String navtree_bgimage = formatBgImage2(null, 
                                                props.getNavigationTreeBgImage(), 
                                                props.getNavigationTreeBgRepeat());

        String tree_title_css = "display:" + (props.isNavigationTreeTitleVisible() ? "block;" : "none;");
        
        int cont_icon_width = props.getNavigationContentIconWidthInteger();
        int cont_icon_padd = (cont_icon_width == 0) ? 0 : (cont_icon_width + 2);
        String cont_icon_css = "padding-left:" + cont_icon_padd + "px !important; background-size:" + 
                               cont_icon_width + "px !important;";

        String toggle_image = props.getNavigationToggleButtonImage();
        int btn_width = props.getNavigationToggleButtonWidthInteger();
        String toggle_left = "";
        String toggle_right = "";
        if (toggle_image.length() > 0) {
            String s = "background-image:url(../images/" + toggle_image + 
                       "); background-size:" + (2 * btn_width) + 
                       "px; background-repeat: no-repeat; ";
            toggle_left = s + "background-position: 0 0; ";
            toggle_right = s + "background-position: -" + btn_width + "px 0; ";
        }

        boolean is_accordion = props.isAccordionEnabled();
        String accordion_bg = is_accordion ? 
               ("display:block; " +  // required to change display of span element from inline to block
                formatBgCSS(props.getAccordionBorderCSS(),
                            props.getAccordionBgColor(), 
                            props.getAccordionBgImage(), 
                            props.getAccordionBgRepeat())) : "";
        String accordion_padding = is_accordion ?
                ("padding-left:" + props.getAccordionPaddingX() + "; padding-right:0;") : "";
        String accordion_font = is_accordion ? 
                ("line-height:" + props.getAccordionHeight() + "; " + props.getAccordionFontCSS()) : "";
        
        String part_bg = formatBgImageCSS(props.getPartTabBgCSS(), 
                                          props.getPartTabBgImage(), 
                                          props.getPartTabBgRepeat());
        String part_bg_cur = formatBgImageCSS(props.getPartTabBgCSSCurrent(), 
                                              props.getPartTabBgImageCurrent(), 
                                              props.getPartTabBgRepeatCurrent());
        String part_bg_hov = formatBgImageCSS(props.getPartTabBgCSSHover(), 
                                              props.getPartTabBgImageHover(),  
                                              props.getPartTabBgRepeatHover());

        String nav_local_bg = formatBgImageCSS(props.getLocalNavigationBgCSS(), 
                                               props.getLocalNavigationBgImage(), 
                                               props.getLocalNavigationBgRepeat());
        String bread_bg = formatBgImageCSS(props.getBreadcrumbsBgCSS(), 
                                           props.getBreadcrumbsBgImage(), 
                                           props.getBreadcrumbsBgRepeat());
        int bread_height = props.isBreadcrumbsDisplay() ? (8 + // reserve minimum of 8 pixels for the font height
                           props.getBreadcrumbsMarginTopInteger() +
                           props.getBreadcrumbsMarginBottomInteger() +
                           props.getBreadcrumbsPaddingTopInteger() +
                           props.getBreadcrumbsPaddingBottomInteger()) : 0;
        int part_bread_height = props.getPartTabHeightInteger() + bread_height;

        String search_mode = props.getSearchUIMode();
        boolean search_hidden = "hidden".equalsIgnoreCase(search_mode);
        StringBuilder search_pos_css = new StringBuilder();
        String search_tab_bg = "";
        String search_tab_active = "";
        String search_tab_hover = "";
        if ("fixed".equalsIgnoreCase(search_mode) || search_hidden) {
            if (search_hidden) {
                search_pos_css.append("display:none; ");
            }
            String box_pos = props.getSearchPosition();
            boolean right_box = box_pos.contains("-right");
            boolean bottom_box = box_pos.contains("-bottom");
            search_pos_css.append("position: fixed; ");
            search_pos_css.append(right_box ? "right:" : "left:").append(props.getSearchPosX()).append("; ");
            search_pos_css.append(bottom_box ? "bottom:" : "top:").append(props.getSearchPosY()).append("; ");
        } else if ("tab".equalsIgnoreCase(search_mode)) {
            search_tab_bg = formatBgImage2(props.getSearchTabBgCSS(), props.getSearchTabBgImage(), 
                                           props.getSearchTabBgRepeat());
            search_tab_active = formatBgImage2(props.getSearchTabBgCSSActive(), props.getSearchTabBgImageActive(), 
                                               props.getSearchTabBgRepeatActive());
            if (props.isSearchTabBgHoverEnabled()) {
                search_tab_hover = formatBgImage2(props.getSearchTabBgCSSHover(), props.getSearchTabBgImageHover(), 
                                                  props.getSearchTabBgRepeatHover());
            }
        }

        String search_width = props.getSearchWidth();
        if (search_width.equals("")) {
            search_width = props.getNavigationWidth();
        }
        String search_bgimage = props.getSearchBgImage();
        if (search_bgimage.length() > 0) {
            search_bgimage = formatBgImageCSS(null, search_bgimage, props.getSearchBgRepeat());
        }
        
        String search_inner_bg = formatBgCSS(props.getSearchInnerBorderCSS(),
                                             props.getSearchInnerBgColor(), 
                                             props.getSearchInnerBgImage(), 
                                             props.getSearchInnerBgRepeat());
        
        String search_input_bg = formatBgCSS(props.getSearchInputBorderCSS(),
                                             props.getSearchInputBgColor(), 
                                             props.getSearchInputBgImage(), 
                                             props.getSearchInputBgRepeat());

        String search_button_bg = formatBgCSS(props.getSearchButtonBorderCSS(),
                                              props.getSearchButtonBgColor(), 
                                              props.getSearchButtonBgImage(), 
                                              props.getSearchButtonBgRepeat());

        String search_button_font;
        if (props.isSearchButtonTextVisible()) {
            search_button_font = props.getSearchButtonFontCSS();
        } else {
            search_button_font = "color:transparent; font-size:8pt; font-weight:normal;";
        }

        String searchpanel_bgimg = props.getSearchPanelBgImage();
        if (searchpanel_bgimg.length() > 0) {
            searchpanel_bgimg = formatBgImageCSS(null, searchpanel_bgimg, props.getSearchPanelBgRepeat());
        }

        String templ = WhdUtils.readFileToString(getPositioningTemplateFile(layout, version));
        Map<String, String> reps = new HashMap<String, String>();
        reps.put("body_bgcolor", props.getBodyBgColor());
        reps.put("content_top", props.getContentTop());
        reps.put("content_padding_left",    props.getContentPaddingLeft());
        reps.put("content_padding_right",   props.getContentPaddingRight());
        reps.put("content_width_min",       props.getContentWidthMin());
        reps.put("content_width_max",       cssLenOrNone(props.getContentWidthMax()));
        reps.put("watermark_xpos",          cssLenOrCenter(props.getWatermarkPosX()));
        reps.put("watermark_ypos",          cssLenOrCenter(props.getWatermarkPosY()));
        reps.put("header_height",          props.getHeaderHeight());
        reps.put("header_padding_top",     props.getHeaderPaddingTop());
        reps.put("header_padding_bottom",  props.getHeaderPaddingBottom());
        reps.put("header_padding_left",    props.getHeaderPaddingLeft());
        reps.put("header_padding_right",   props.getHeaderPaddingRight());
        reps.put("header_title_display",  (props.isHeaderTitle1Enabled() || props.isHeaderTitle2Enabled()) ? "block" : "none");
        reps.put("header_title1_display",  props.isHeaderTitle1Enabled() ? "block" : "none");
        reps.put("header_title2_display",  props.isHeaderTitle2Enabled() ? "block" : "none");
        reps.put("header_title1_font",     props.getHeaderTitle1FontCSS());
        reps.put("header_title2_font",     props.getHeaderTitle2FontCSS());
        reps.put("header_title_align",     props.getHeaderTitleAlignment());
        reps.put("header_title_margin_top",     props.getHeaderTitleMarginTop());
        reps.put("header_title_margin_bottom",  props.getHeaderTitleMarginBottom());
        reps.put("header_title_margin_left",    props.getHeaderTitleMarginLeft());
        reps.put("header_title_margin_right",   props.getHeaderTitleMarginRight());
        reps.put("header_bgcolor",         props.getHeaderBgColor());
        reps.put("header_border",          props.getHeaderBorderCSS());
        reps.put("header_bgimage",         head_bgimage);
        reps.put("logo1_display",  props.isLogo1Visible() ? "inline" : "none");
        reps.put("logo1_position", logo1_csspos);
        reps.put("logo1_width",    props.getLogo1Width());
        reps.put("logo1_height",   props.getLogo1Height());
        reps.put("logo2_display",  props.isLogo2Visible() ? "inline" : "none");
        reps.put("logo2_position", logo2_csspos);
        reps.put("logo2_width",    props.getLogo2Width());
        reps.put("logo2_height",   props.getLogo2Height());
        reps.put("navigation_width",         props.getNavigationWidth());
        reps.put("navigation_top_offset",    props.getNavigationMarginTop());
        reps.put("navigation_bottom_offset", props.getNavigationMarginBottom());
        reps.put("navigation_padding_top",    props.getNavigationPaddingTop());
        reps.put("navigation_padding_bottom", props.getNavigationPaddingBottom());
        reps.put("navigation_padding_left",    props.getNavigationPaddingLeft());
        reps.put("navigation_padding_right", props.getNavigationPaddingRight());
        reps.put("navigation_bgcolor",       props.getNavigationBgColor());
        reps.put("navigation_border",        props.getNavigationBorderCSS());
        reps.put("navigation_bgimage",       nav_bgimage);
        reps.put("navigation_tree_bgcolor",      props.getNavigationTreeBgColor());
        reps.put("navigation_tree_bgimage",      navtree_bgimage);
        reps.put("navigation_tree_border",       props.getNavigationTreeBorderCSS());
        reps.put("navigation_tree_title",        tree_title_css);
        reps.put("navigation_tree_font_title",   props.getNavigationTreeFontTitleCSS());
        reps.put("navigation_tree_font_default", props.getNavigationTreeFontCSS());
        reps.put("navigation_tree_font_hover",   props.getNavigationTreeFontHoverCSS());
        reps.put("navigation_tree_font_current", props.getNavigationTreeFontCurrentCSS());
        reps.put("navigation_content_icon_space", cont_icon_css);
        reps.put("navigation_toggle_bgimage_left",  toggle_left);
        reps.put("navigation_toggle_bgimage_right", toggle_right);
        reps.put("navigation_toggle_button_width",  btn_width + "px");
        reps.put("accordion_bg",      accordion_bg);
        reps.put("accordion_padding", accordion_padding);
        reps.put("accordion_font",    accordion_font);
        reps.put("accordion_spacing", props.getAccordionSpacing());
        reps.put("part_tabs_margin_top",        props.getPartTabsMarginTop());
        reps.put("part_tabs_margin_left",       props.getPartTabsMarginLeft());
        reps.put("part_tab_height",             props.getPartTabHeight());
        reps.put("part_tab_space",              props.getPartTabSpacing());
        reps.put("part_tab_bg",                 part_bg);
        reps.put("part_tab_bg_current",         props.isPartTabBgCurrentEnabled() ? part_bg_cur : "");
        reps.put("part_tab_bg_hover",           props.isPartTabBgHoverEnabled() ? part_bg_hov : "");
        reps.put("part_tab_font",               props.getPartTabFontCSS());
        reps.put("part_tab_font_current",       props.getPartTabFontCSSCurrent());
        reps.put("part_tab_font_hover",         props.getPartTabFontCSSHover());
        reps.put("part_tab_breadcrumbs_height", part_bread_height + "px");
        reps.put("local_navigation_height",          cssLenOrAuto(props.getLocalNavigationHeight()));
        reps.put("local_navigation_margin_top",      props.getLocalNavigationMarginTop());
        reps.put("local_navigation_padding_top",     props.getLocalNavigationPaddingTop());
        reps.put("local_navigation_padding_bottom",  props.getLocalNavigationPaddingBottom());
        reps.put("local_navigation_padding_left",    props.getLocalNavigationPaddingLeft());
        reps.put("local_navigation_padding_right",   props.getLocalNavigationPaddingRight());
        reps.put("local_navigation_bg",              nav_local_bg);
        reps.put("local_navigation_link_font",       props.getLocalNavigationLinkCSS());
        reps.put("local_navigation_link_hover_font", props.getLocalNavigationLinkHoverCSS());
        reps.put("local_navigation_sep_font",        props.getLocalNavigationSeparatorCSS());
        reps.put("local_navigation_sep_visibility",  props.isLocalNavigationSeparatorVisible() ? "visible" : "hidden");
        reps.put("local_navigation_sep_display",     props.isLocalNavigationSeparatorVisible() ? "inline" : "none");
        reps.put("local_navigation_text_display",    props.isLocalNavigationTextVisible() ? "inline" : "none");
        reps.put("local_navigation_icons_display",   props.isLocalNavigationIconsVisible() ? "inline" : "none");
        reps.put("local_navigation_icons_height",    props.getLocalNavigationIconsHeight());
        reps.put("breadcrumbs_display",         props.getBreadcrumbsDisplay());
        reps.put("breadcrumbs_height",          bread_height + "px");
        reps.put("breadcrumbs_margin_top",      props.getBreadcrumbsMarginTop());
        reps.put("breadcrumbs_margin_bottom",   props.getBreadcrumbsMarginBottom());
        reps.put("breadcrumbs_margin_left",     props.getBreadcrumbsMarginLeft());
        reps.put("breadcrumbs_margin_right",    props.getBreadcrumbsMarginRight());
        reps.put("breadcrumbs_padding_top",     props.getBreadcrumbsPaddingTop());
        reps.put("breadcrumbs_padding_bottom",  props.getBreadcrumbsPaddingBottom());
        reps.put("breadcrumbs_padding_left",    props.getBreadcrumbsPaddingLeft());
        reps.put("breadcrumbs_padding_right",   props.getBreadcrumbsPaddingRight());
        reps.put("breadcrumbs_bg",              bread_bg);
        reps.put("breadcrumbs_link_font",       props.getBreadcrumbsLinkCSS());
        reps.put("breadcrumbs_last_font",       props.getBreadcrumbsLastCSS());
        reps.put("breadcrumbs_hover_font",      props.getBreadcrumbsHoverCSS());
        reps.put("breadcrumbs_sep_font",        props.getBreadcrumbsSeparatorCSS());
        reps.put("breadcrumbs_sep_image_width", props.getBreadcrumbsSeparatorImageWidth());
        reps.put("search_box_position", search_pos_css.toString());
        reps.put("search_box_width", search_width);
        reps.put("search_box_height", props.getSearchHeight());
        reps.put("search_inner_top_offset", props.getSearchInnerTopOffset());
        reps.put("search_inner_left_offset", props.getSearchInnerLeftOffset());
        reps.put("search_inner_right_offset", props.getSearchInnerRightOffset());
        reps.put("search_box_bgcolor", props.getSearchBgColor());
        reps.put("search_box_bgimage", search_bgimage);
        reps.put("search_box_border", props.getSearchBorderCSS());
        reps.put("search_inner_bg", search_inner_bg);
        reps.put("search_input_bg", search_input_bg);
        reps.put("search_button_width", cssLenOrAuto(props.getSearchButtonWidth()));
        reps.put("search_button_height", cssLenOrAuto(props.getSearchButtonHeight()));
        reps.put("search_button_bg", search_button_bg);
        reps.put("search_button_font", search_button_font);
        reps.put("search_legend_font", props.getSearchLegendFontCSS());
        reps.put("search_input_font", props.getSearchInputFontCSS());
        reps.put("search_title_font", props.getSearchTitleFontCSS());
        reps.put("search_expression_font", props.getSearchExpressionFontCSS());
        reps.put("search_result_font", props.getSearchResultFontCSS());
        reps.put("search_highlight_font", props.getSearchHitFontCSS());
        reps.put("search_close_font", props.getSearchCloseFontCSS());
        reps.put("search_close_image_url", "../images/" + props.getSearchCloseImage());
        reps.put("search_toggle_button_width", props.getSearchToggleButtonWidth());
        reps.put("search_tab_height", props.getSearchTabHeight());
        reps.put("search_tab_bg", search_tab_bg);
        reps.put("search_tab_bg_active", search_tab_active);
        reps.put("search_tab_bg_hover", search_tab_hover);
        reps.put("search_tab_font", props.getSearchTabFontCSS());
        reps.put("search_tab_font_active", props.getSearchTabFontCSSActive());
        reps.put("search_tab_font_hover", props.getSearchTabFontCSSHover());
        reps.put("search_panel_padding_top",    props.getSearchPanelPaddingTop());
        reps.put("search_panel_padding_bottom", props.getSearchPanelPaddingBottom());
        reps.put("search_panel_padding_left",   props.getSearchPanelPaddingLeft());
        reps.put("search_panel_padding_right",  props.getSearchPanelPaddingRight());
        reps.put("search_panel_bgcolor",       props.getSearchPanelBgColor());
        reps.put("search_panel_border",        props.getSearchPanelBorderCSS());
        reps.put("search_panel_bgimage",       searchpanel_bgimg);
        return WhdTemplateUtils.replace(templ, reps);
    }
    
    private String cssLenOrAuto(String value)
    {
        return ((value == null) || value.equals("")) ? "auto" : value;
    }
    
    private String cssLenOrNone(String value)
    {
        return ((value == null) || value.equals("")) ? "none" : value;
    }
    
    private String cssLenOrCenter(String value)
    {
        return ((value == null) || value.equals("")) ? "center" : value;
    }
    
    private String getCSSPositionValue(String pos_type, int xpos, int ypos)
    {
        String csspos = pos_type;
        if (pos_type.equals("left") || pos_type.equals("right")) {
            csspos = ((xpos == 0) && (ypos == 0)) ? "static" : "relative";
        } else {
            if (pos_type.startsWith("absolute-")) {
                csspos = "absolute";
            } else
            if (pos_type.startsWith("fixed-")) {
                csspos = "fixed";
            }
        }
        return csspos;
    }

    private String getJsConfigContent(WebDesignerProps props) throws IOException
    {
        String layout = props.getLayoutId();
        String version = props.getLayoutVersion();
        String templ = WhdUtils.readFileToString(getJsConfigTemplateFile(layout, version));
        String anim = props.getNavigationTreeAnimation();
        if (anim.equals("off")) {
            anim = "0";
        } else {
            anim = '"' + anim + '"';
        }
        String bread_sep_url = props.getBreadcrumbsSeparatorImage();
        if (bread_sep_url.length() > 0) {
            bread_sep_url = CONTENT_TO_COMMON_URL_PATH + "images/" + bread_sep_url;
        }
        Integer search_width = props.getSearchWidthInteger();
        templ = templ.replace("###content_top_integer###", "" + props.getContentTopInteger())
                     .replace("###navigation_width_integer###", "" + props.getNavigationWidthInteger())
                     .replace("###search_width_integer###", (search_width == null) ? "0" : search_width.toString())
                     .replace("###search_ui_mode###", '"' + props.getSearchUIMode() + '"')
                     .replace("###tree_title_visible###", props.isNavigationTreeTitleVisible() ? "true" : "false")
                     .replace("###tree_collapsed###", props.isNavigationTreeCollapsed() ? "true" : "false")
                     .replace("###tree_unique###", props.isNavigationTreeAutoClose() ? "true" : "false")
                     .replace("###tree_animated###", anim)
                     .replace("###breadcrumbs_hidden###", props.isBreadcrumbsDisplay() ? "false" : "true")
                     .replace("###breadcrumbs_separator_image_url###", '"' + bread_sep_url + '"')
                     .replace("###search_button_text_hidden###", props.isSearchButtonTextVisible() ? "false" : "true")
                     .replace("###resizetoc_enabled###", props.isNavigationResizeEnabled() ? "true" : "false")
                     .replace("###accordion_enabled###", props.isAccordionEnabled() ? "true" : "false")
                ;
        return templ;
    }

    private String getJsMainContent(WebDesignerProps props) throws IOException
    {
        String layout = props.getLayoutId();
        String version = props.getLayoutVersion();
        String js_conf = getJsConfigContent(props);
        String main = WhdUtils.readFileToString(getJsMainTemplateFile(layout, version));
        main = main.replace("/*__USER_WEBHELP_JS__*/", js_conf);
        return main;
    }

    //********************************************************************
    //*******************     Resource files     *************************
    //********************************************************************
   
    private File getBlankFile_PNG()
    {
        return new File(getDesignerResourcesDir(), "blank.png");
    }

    private File getBlankFile_GIF()
    {
        return new File(getDesignerResourcesDir(), "blank.gif");
    }

    //********************************************************************
    //******************   Directory configuration   *********************
    //********************************************************************

    private File getWebRoot()
    {
        return new File(whdMainWin.getDesktop().getWebApp().getRealPath("/"));
    }
    
    private File getDesignerWebRoot()
    {
        return new File(getWebRoot(), DESIGNER_URL_PATH);
    }
    
    private File getPreviewBaseDir()
    {
        return new File(getDesignerWebRoot(), PREVIEW_PATH);
    }
    
    private File getDesignerBaseDir()
    {
        return new File(baseDir, "webdesigner_base");
    }
    
    private File getWebhelpSavedDir()
    {
        return new File(getDesignerBaseDir(), "saved");
    }

    private File getWebhelpExportDir()
    {
        return new File(getDesignerBaseDir(), "temp");
    }

    private File getWebhelpSavedDesignDir(String design_name)
    {
        return new File(getWebhelpSavedDir(), design_name);
    }
    
    private File getWebhelpLayoutsDir()
    {
        return new File(getDesignerWebRoot(), "web_layouts");
    }
    
    private File getWebhelpLayoutDir(String layout, String version) 
    {
        File layout_dir = new File(getWebhelpLayoutsDir(), layout);
        if (! layout_dir.exists()) {
            return null;
        }
        if (! version.startsWith("v")) {
            version = "v" + version;
        }
        File ver_dir = new File(layout_dir, version);
        if (! ver_dir.exists()) {
            return null;
        }
        return ver_dir;
    }
    
    private File getWebhelpExamplesDir()
    {
        return new File(getDesignerWebRoot(), "web_examples");
    }

    private File getDesignerResourcesDir()
    {
        return new File(getDesignerWebRoot(), "resources");
    }

    //********************************************************************
    //****************   "Edit Font"-Dialog Helper   *********************
    //********************************************************************
    
    private void initEditFontDialog(String css)
    {
        fontWinComposer.initEditFontDialog(css);
    }
    
    private String getEditFontDialogCSS()
    {
        return fontWinComposer.getEditFontDialogCSS();
    }
    
    //********************************************************************
    //****************   "Edit Border"-Dialog Helper   *******************
    //********************************************************************
    
    private void initEditBorderDialog(String css)
    {
        File bgFile = null; 
        String url = CssUtils.extractPropertyValue(css, "background-image");
        if (url != null) {
            final String URL_PATT = "url(";
            if (url.toLowerCase().startsWith(URL_PATT)) {
                int endpos = url.indexOf(')');
                if (endpos > 0) {
                    url = url.substring(URL_PATT.length(), endpos).trim();
                    int p = url.lastIndexOf('/');
                    String fn = (p < 0) ? url : url.substring(p + 1);
                    if (! isFileChanged(fn)) {
                        bgFile = getSavedDesignFile(openedDesignName, fn);
                    }
                }
            }
        }
        String tempURLPath = PREVIEW_PATH + "/" + TEMP_UPLOAD_PATH + "/";
        
        borderWinComposer.initEditBorderDialog(css, bgFile, getTempPreviewDir(), tempURLPath);
    }

}
