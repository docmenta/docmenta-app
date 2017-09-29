/*
 * MediaConfigDialog.java
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
import org.docma.app.ui.FilterSettingModel;
import org.docma.util.Log;

import java.util.*;
import org.zkoss.zul.*;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.OpenEvent;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.util.Clients;

/**
 *
 * @author MP
 */
public class MediaConfigDialog extends Window
{
    private static final int MAX_PDF_HEADER_FOOTER_ROWS = 2;  // max rows of PDF header/footer
    
    private static final int MODE_NEW = 0;
    private static final int MODE_EDIT = 1;

    private int modalResult = -1;
    private int mode = -1;

    private DocmaOutputConfig outConf;
    private DocmaSession docmaSess;

    // private Include formatInclude;
    private Tab generalOptionsTab;
    private Tab htmlOptionsTab;
    private Tab pdfOptionsTab;
    private Tab headerFooterOptionsTab;
    private Tab docbookOptionsTab;

    boolean initialized = false;

    // General output configuration fields
    private Textbox idBox;
    private Textbox filterBox;
    private Checkbox tocBox;
    private Checkbox indexBox;
    private Listbox  maxTocDepthBox;
    private Textbox  tocIndentBox;
    private Checkbox tocNamedPartBox;
    private Checkbox tocNamedChapterBox;
    private Checkbox partTocBox;
    private Checkbox chapterTocBox;
    private Checkbox sectionTocBox;
    private Listbox  sectionTocLevelsBox;
    private Listbox  sectionTocDepthBox;
    private Listbox styleVariantBox;
    private Listbox formatBox;
    private Textbox paraSpaceBox;
    private Listbox paraSpaceUnitBox;
    private Textbox paraIndentBox;
    private Listbox paraIndentUnitBox;
    private Textbox listItemSpaceBox;
    private Listbox listItemSpaceUnitBox;
    private Textbox listIndentBox;
    private Listbox listIndentUnitBox;
    private Textbox listLabelWidthBox;
    private Listbox listLabelWidthUnitBox;
    private Listbox titlePlacementBox;

    // Numbering fields
    private Listbox render1stLevelBox;
    private Checkbox partNumberBox;
    private Listbox partNumberFormatBox;
    private Checkbox chapterNumberBox;
    private Listbox chapterNumberFormatBox;
    private Checkbox sectionNumberBox;
    private Listbox sectionNumberFormatBox;
    private Checkbox appendixNumberBox;
    private Listbox appendixNumberFormatBox;
    private Listbox footnoteNumberFormatBox;
    private Intbox numberingDepthBox;
    private Checkbox omitSingleSectionBox;
    private Checkbox exclude1stLevelNumberBox;
    private Checkbox restartPartBox;

    // HTML specific fields
    private Radiogroup htmlFileTypeRadiogroup;
    private Radio htmlSingleFileRadio;
    private Radio htmlMultipleFilesRadio;
    // private Textbox htmlFilePrefixBox;
    private Checkbox htmlSeparateFileBox;
    private Listbox htmlSeparateFileLevelBox;
    // private Checkbox htmlCreateDirBox;
    // private Listbox htmlDirLevelBox;
    private Checkbox htmlAliasFilenameBox;
    private Checkbox htmlInclude1stSectBox;
    private Checkbox htmlSeparateTOCBox;
    // private Checkbox htmlSeparateEachTableBox;
    private Checkbox htmlNavigationIconsBox;
    private Checkbox htmlNavigationTitlesBox;
    private Checkbox htmlBreadcrumbsBox;
    private Listbox htmlBreadcrumbStartBox;
    private Textbox htmlBreadcrumbSeparatorBox;
    private Textbox htmlRootFolderBox;
    private Textbox htmlURLTargetWindowBox;
    private Textbox htmlCustomHeaderBox;
    private Textbox htmlCustomFooterBox;
    // private Textbox htmlCoverImageBox;
    private Listbox htmlCustomCSSBox;
    private Listbox htmlCustomJSBox;
    private Listbox htmlCustomMetaBox;
    private Textbox htmlCustomFilesBox;
    private Listbox htmlOutputEncodingBox;
    private Component htmlDocTypeArea;
    private Checkbox htmlDocTypeCheckBox;
    private Textbox htmlDocTypeTextBox;
    private Listbox htmlWebhelpConfigBox;
    private Listbox htmlWebhelpHeader1Box;
    private Listbox htmlWebhelpHeader2Box;

    // PDF specific fields
    private Listbox pdfPaperSizeBox;
    private Textbox pdfPageWidthBox;
    private Listbox pdfPageWidthUnitBox;
    private Textbox pdfPageHeightBox;
    private Listbox pdfPageHeightUnitBox;
    private Radiogroup pdfPageOrientRadiogroup;
    private Radio pdfPagePortraitRadio;
    private Radio pdfPageLandscapeRadio;
    private Textbox pdfPageTopBox;
    private Listbox pdfPageTopUnitBox;
    private Textbox pdfPageBottomBox;
    private Listbox pdfPageBottomUnitBox;
    private Textbox pdfPageInnerBox;
    private Listbox pdfPageInnerUnitBox;
    private Textbox pdfPageOuterBox;
    private Listbox pdfPageOuterUnitBox;
    private Textbox pdfBodyTopBox;
    private Listbox pdfBodyTopUnitBox;
    private Textbox pdfBodyBottomBox;
    private Listbox pdfBodyBottomUnitBox;
    private Textbox pdfBodyStartBox;
    private Listbox pdfBodyStartUnitBox;
    private Textbox pdfBodyEndBox;
    private Listbox pdfBodyEndUnitBox;
    private Textbox pdfHeaderHeightBox;
    private Listbox pdfHeaderHeightUnitBox;
    private Textbox pdfFooterHeightBox;
    private Listbox pdfFooterHeightUnitBox;
    private Checkbox pdfDoubleSidedBox;
    private Checkbox pdfBookmarksBox;
    private Checkbox pdfNumbersInRefsBox;
    private Checkbox pdfExportFilesBox;
    private Checkbox pdfFitImagesBox;
    private Checkbox pdfShowExternalHRefBox;
    private Listbox pdfShowExternalHRefTypeBox;
    private Intbox pdfTargetResolutionBox;
    private Intbox pdfSourceResolutionBox;
    private Listbox pdfCoverModeBox;
    private Checkbox pdfCoverBlankPageBox;
    private Textbox pdfCoverXBox;
    private Listbox pdfCoverXUnitBox;
    private Textbox pdfCoverYBox;
    private Listbox pdfCoverYUnitBox;
    private Listbox pdfIndexColumnCountBox;
    private Textbox pdfIndexColumnGapBox;
    private Listbox pdfIndexColumnGapUnitBox;

    // Header/Footer fields
    private Listbox markerSectionLevelBox;
    private Checkbox customHeaderFooterBox;
    private Listbox headerFooterPageClassBox;
    private Checkbox headerFooterSameAsBodyBox;
    private Listbox headerFooterPageSequenceBox;
    private Checkbox headerFooterSameAsOddBox;
    private Listbox headerFooterPublicationPreviewBox;
    
    private Label headerWidth1Label;
    private Label headerWidth3Label;
    private Label footerWidth1Label;
    private Label footerWidth3Label;
    
    private Intbox headerWidth1Box;
    private Intbox headerWidth2Box;
    private Intbox headerWidth3Box;
    private Intbox footerWidth1Box;
    private Intbox footerWidth2Box;
    private Intbox footerWidth3Box;

    private Set headerFooterPageTypes = null;
    private Map headerFooterContentMap = null;

    private final Map<String, String> oldHeaderFooterContent = new HashMap<String, String>();
    
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
        mode = MODE_NEW;
        setTitle(GUIUtil.i18(this).getLabel("label.mediaconfig.dialog.new.title"));
        idBox = (Textbox) getFellow("MediaConfigIDTextbox");
        // idBox.setDisabled(false);
    }

    public void setMode_Edit()
    {
        mode = MODE_EDIT;
        setTitle(GUIUtil.i18(this).getLabel("label.mediaconfig.dialog.edit.title"));
        idBox = (Textbox) getFellow("MediaConfigIDTextbox");
        // idBox.setDisabled(true);
    }

    public boolean doEdit(DocmaOutputConfig outConf, DocmaSession docmaSess)
    throws Exception
    {
        this.outConf = outConf;
        this.docmaSess = docmaSess;

        initFields();
        updateGUI(); // init dialog fields
        generalOptionsTab.setSelected(true);
        setWidth("680px");   // see width in MediaConfigDialog.zul
        invalidate();
        do {
            modalResult = -1;
            doModal();
            if (getModalResult() != GUIConstants.MODAL_OKAY) {
                return false;
            }
            if (hasInvalidInputs()) {
                continue;
            }
            String old_id = outConf.getId();
            updateModel(outConf);
            String new_id = outConf.getId();
            docmaSess.saveOutputConfig(outConf);
            if (mode == MODE_EDIT) {
                if (! new_id.equals(old_id)) {
                    docmaSess.deleteOutputConfig(old_id);
                }
            }
            return true;
        } while (true);
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

    public void onSelectFormat()
    {
        if (formatBox == null) initFields();
        String format_gui = getCurrentSelectedGUIFormat(); // formatBox.getSelectedItem().getValue().toString();
        String format = getMainFormat(format_gui);
        if (format.equals("html")) {
            activateHTMLTab();
        } else
        if (format.equals("pdf")) {
            activatePDFTab();
        } else
        if (format.equals("docbook")) {
            activateDocBookTab();
        }
    }
    
    public void onCheckComponentTOC()
    {
        boolean part_toc = partTocBox.isChecked();
        boolean chap_toc = chapterTocBox.isChecked();
        boolean sect_toc = sectionTocBox.isChecked();
        sectionTocLevelsBox.setDisabled(! sect_toc);
        sectionTocDepthBox.setDisabled(! (part_toc || chap_toc || sect_toc));
    }
    
    public void onCheckBreadcrumbs()
    {
        boolean no_bread = !htmlBreadcrumbsBox.isChecked();
        String format = getCurrentSelectedGUIFormat();
        boolean web_new = isWebHelp(format) && !isWebHelp1(format);
        htmlBreadcrumbStartBox.setDisabled(no_bread || !web_new);
        htmlBreadcrumbSeparatorBox.setDisabled(no_bread);
    }
    
    public void onEditHTMLCustomHeader() throws Exception
    {
        DocmaNode nd = getContentNodeForEdit(htmlCustomHeaderBox.getValue());
        if (nd != null) {
            MainWindow mainWin = getMainWindow();
            mainWin.doEditContent(nd);
        }
    }

    public void onEditHTMLCustomFooter() throws Exception
    {
        DocmaNode nd = getContentNodeForEdit(htmlCustomFooterBox.getValue());
        if (nd != null) {
            MainWindow mainWin = getMainWindow();
            mainWin.doEditContent(nd);
        }
    }
    
    private DocmaNode getContentNodeForEdit(String alias)
    {
        alias = alias.trim();
        if (alias.equals("")) {
            Messagebox.show("Please enter the alias name of a content node!");
            return null;
        }
        return getValidContentNode(alias);  // shows error and returns null if no valid alias
    }

    public void onEditHTMLCustomCSS() 
    {
        DocmaNode nd = getHTMLConfFileNode(htmlCustomCSSBox);
        if (nd != null) {
            MainWindow mainWin = getMainWindow();
            mainWin.openFileNodeInWindow(nd, true);
        }
    }

    public void onEditHTMLCustomJS() 
    {
        DocmaNode nd = getHTMLConfFileNode(htmlCustomJSBox);
        if (nd != null) {
            MainWindow mainWin = getMainWindow();
            mainWin.openFileNodeInWindow(nd, true);
        }
    }

    public void onEditHTMLCustomMeta() 
    {
        DocmaNode nd = getHTMLConfFileNode(htmlCustomMetaBox);
        if (nd != null) {
            MainWindow mainWin = getMainWindow();
            mainWin.openFileNodeInWindow(nd, true);
        }
    }
    
    private DocmaNode getHTMLConfFileNode(Listbox listbox) 
    {
        Listitem item = listbox.getSelectedItem();
        String fn = (item != null) ? item.getValue().toString() : "";
        if (fn.equals("")) {
            return null;
        }
        DocmaNode conf_folder = docmaSess.getNodeByAlias(DocmaConstants.HTML_CONFIG_FOLDER_ALIAS_NAME);
        if (conf_folder == null) {
            return null;
        }
        DocmaNode nd = conf_folder.getChildByFilename(fn);
        if (nd == null) {
            Messagebox.show("Error: File '" + fn + "' not found in HTML configuration folder!");
            return null;
        }
        if (! nd.isFileContent()) {
            Messagebox.show("Error: '" + fn + "' is not a file node!");
            return null;
        }
        return nd;
    }
    
    public void onCheckCustomDocType()
    {
        String currentFormat = getCurrentSelectedGUIFormat();
        boolean is_webhelp = isWebHelp(currentFormat);
        boolean is_webhelp_1 = isWebHelp1(currentFormat);
        boolean is_webhelp_new = is_webhelp && !is_webhelp_1;
        
        boolean customDocType = htmlDocTypeCheckBox.isChecked();
        // htmlDocTypeTextBox.setDisabled(! customDocType);
        htmlDocTypeTextBox.setReadonly(! customDocType);
        String docType;
        if (customDocType) {
            // Show custom DOCTYPE
            docType = is_webhelp_new ? outConf.getHtmlCustomDocType() : "";
            docType = (docType == null) ? "" : docType.trim();
        } else {
            // Show default DOCTYPE
            docType = is_webhelp_new ? WebFormatter.DOCTYPE_INTRO_DEFAULT : "";
        }
        htmlDocTypeTextBox.setValue(docType);
        if (customDocType) {
            htmlDocTypeTextBox.setFocus(true);
        }
    }

    public void onSelectPaperSize()
    {
        if (pdfPaperSizeBox == null) initFields();
        String psize = pdfPaperSizeBox.getSelectedItem().getValue().toString();
        enableCustomPaperSizeBoxes(psize.equals("custom"));
    }
    
    public void onSelectCoverPageMode()
    {
        boolean is_extra = pdfCoverModeBox.getSelectedItem().getValue().toString().equals("extrapage");
        if (! is_extra) {
            pdfCoverBlankPageBox.setChecked(false);
        }
        pdfCoverBlankPageBox.setDisabled(! is_extra);
    }

    public void onChangeShowExternalHRef()
    {
        pdfShowExternalHRefTypeBox.setDisabled(!pdfShowExternalHRefBox.isChecked());
    }
    
    public void onHeaderFooterTabSelected()
    {
        setWidth("620px"); 
        if (pdfDoubleSidedBox == null) initFields();
        
        if (pdfDoubleSidedBox.isChecked()) {
            headerWidth1Label.setValue("Inside: ");
            headerWidth3Label.setValue("Outside: ");
            footerWidth1Label.setValue("Inside: ");
            footerWidth3Label.setValue("Outside: ");
        } else {
            headerWidth1Label.setValue("Left: ");
            headerWidth3Label.setValue("Right: ");
            footerWidth1Label.setValue("Left: ");
            footerWidth3Label.setValue("Right: ");
        }
        invalidate();
    }
    
    public void onCheckCustomHeaderFooter()
    {
        if (customHeaderFooterBox == null) initFields();
        boolean is_custom = customHeaderFooterBox.isChecked();
        setHeaderFooterFieldsDisabled(! is_custom);
        boolean no_pagetypes = (headerFooterPageTypes == null) || headerFooterPageTypes.isEmpty();
        if (is_custom && no_pagetypes) {
            setDefaultCustomHeaderFooter();  // Set default header/footer configuration
        }
        if (is_custom) {
            resetGUI_HeaderFooterContent();
        }
    }

    public void onSelectHeaderFooterPageClass()
    {
        Listitem item = headerFooterPageClassBox.getSelectedItem();
        if (item == null) return;
        
        String pageClass = item.getValue().toString();
        String pageTypeOdd = pageClass + "_odd";
        boolean is_body = pageClass.equals("body");
        boolean is_same_as_body = (! is_body) && !headerFooterPageTypes.contains(pageTypeOdd);
        headerFooterSameAsBodyBox.setChecked(is_same_as_body);
        headerFooterSameAsBodyBox.setDisabled(is_body);
        headerFooterSameAsBodyBox.setVisible(! is_body);
        
        selectItemByValue(headerFooterPageSequenceBox, "odd");
        headerFooterPageSequenceBox.setDisabled(is_same_as_body);
        headerFooterSameAsOddBox.setChecked(false);
        headerFooterSameAsOddBox.setDisabled(true);
        headerFooterSameAsOddBox.setVisible(false);
        if (is_same_as_body) {
            setHeaderFooterContentFields(null);  // or: "body_odd"
            setHeaderFooterContentFieldsDisabled(true);
        } else {
            setHeaderFooterContentFields(pageTypeOdd);
        }
    }
    
    public void onSelectHeaderFooterPageSequence()
    {
        Listitem item = headerFooterPageClassBox.getSelectedItem();
        if (item == null) return;
        String pageClass = item.getValue().toString();
        item = headerFooterPageSequenceBox.getSelectedItem();
        if (item == null) return;
        String sequence = item.getValue().toString();
        String pageType = pageClass + "_" + sequence;
        boolean is_odd = sequence.equals("odd");

        boolean is_same = (! is_odd) && !headerFooterPageTypes.contains(pageType);
        boolean mirrored = (sequence.equals("even") || sequence.equals("blank")) && pdfDoubleSidedBox.isChecked();
        if (mirrored) {
            headerFooterSameAsOddBox.setLabel("Same as odd pages (mirrored for double-sided output)");
        } else {
            headerFooterSameAsOddBox.setLabel("Same as odd pages");
        }
        headerFooterSameAsOddBox.setChecked(is_same);
        headerFooterSameAsOddBox.setDisabled(is_odd);
        headerFooterSameAsOddBox.setVisible(! is_odd);
        if (is_same) {  // clear and disable header/footer content fields
            setHeaderFooterContentFields(null);  // or: pageClass + "_odd"
            setHeaderFooterContentFieldsDisabled(true);
        } else {
            setHeaderFooterContentFields(pageType);
        }
    }
    
    public void onCheckHeaderFooterSameAsBody()
    {
        Listitem item = headerFooterPageClassBox.getSelectedItem();
        if (item == null) return;
        String pageClass = item.getValue().toString();

        boolean is_same_as_body = headerFooterSameAsBodyBox.isChecked();
        if (is_same_as_body) {
            // Remove all page types for this page class.
            // Note: If no page type exists for a specific page class, then
            //       this has the meaning that this page class uses the same 
            //       header/footer configuration as the page class "body".
            String prefix = pageClass + "_";
            Iterator it = headerFooterPageTypes.iterator();
            while (it.hasNext()) {
                if (((String) it.next()).startsWith(prefix)) it.remove();
            }
        } else {
            headerFooterPageTypes.add(pageClass + "_odd");
        }
        onSelectHeaderFooterPageClass();  // update GUI fields
    }

    public void onCheckHeaderFooterSameAsOdd()
    {
        String pageType = getHeaderFooterPageType();
        boolean is_same = headerFooterSameAsOddBox.isChecked();
        if (is_same) {
            headerFooterPageTypes.remove(pageType);
            // clear and disable header/footer content fields:
            setHeaderFooterContentFields(null);  // or: pageClass + "_odd"
            setHeaderFooterContentFieldsDisabled(true);
        } else {
            headerFooterPageTypes.add(pageType);
            setHeaderFooterContentFields(pageType);
        }
    }
    
    public void onChangeHeaderFooterWidth(ForwardEvent evt) throws Exception
    {
        try {
            for (int i=0; i < 2; i++) {
                for (int col=1; col <= 3; col++) {
                    Intbox b1 = (i == 0) ? headerWidth1Box : footerWidth1Box;
                    Intbox b2 = (i == 0) ? headerWidth2Box : footerWidth2Box;
                    Intbox b3 = (i == 0) ? headerWidth3Box : footerWidth3Box;
                    if (col == 2) {
                        b1 = (i == 0) ? headerWidth2Box : footerWidth2Box;
                        b2 = (i == 0) ? headerWidth1Box : footerWidth1Box;
                    } else
                    if (col == 3) {
                        b1 = (i == 0) ? headerWidth3Box : footerWidth3Box;
                        b3 = (i == 0) ? headerWidth1Box : footerWidth1Box;
                    }
                    if (getHeaderFooterWidth(b1).equals("") && 
                        !getHeaderFooterWidth(b2).equals("") &&
                        !getHeaderFooterWidth(b3).equals("")) {
                        int sum = b2.intValue() + b3.intValue();
                        if (sum <= 100) {
                            b1.setValue(100 - sum);
                        }
                    }
                }
            }
        } catch (Exception ex) {}
        updateHeaderFooterVisibility();
    }

    public void onOpenHeaderFooterContent(ForwardEvent evt) 
    {
        Event orig_evt = evt.getOrigin();
        if (orig_evt instanceof OpenEvent) {
            OpenEvent oevt = (OpenEvent) orig_evt;
            if (oevt.isOpen()) {
                String box_id = oevt.getTarget().getId();
                String region = getRegionFromBoxId(box_id);
                int col = getColumnFromBoxId(box_id);
                int row = getRowFromBoxId(box_id);
                // Combobox combo_box = (Combobox) getFellow(box_id);
                Object value = oevt.getValue(); // combo_box.getValue();
                setOldHeaderFooterContent(region, col, row, (value == null) ? "" : value.toString());
                if (DocmaConstants.DEBUG) {
                    Log.info("Setting old header/footer value for " + box_id + ": " + value);
                }
            }
        }
    }
    
    public void onSelectHeaderFooterContent(ForwardEvent evt) 
    {
        String box_id = evt.getOrigin().getTarget().getId();
        
        Combobox combo_box = (Combobox) getFellow(box_id);
        String region = getRegionFromBoxId(box_id);
        int col = getColumnFromBoxId(box_id);
        int row = getRowFromBoxId(box_id);

        Comboitem item = combo_box.getSelectedItem();
        if (item == null) {
            return;  // no item selected because user has manually edited text field
        }
        String value = item.getLabel();  // the value that user has selected from the drop-down list
        // Get previous value that has been set in onOpenHeaderFooterContent()
        String old_value = getOldHeaderFooterContent(region, col, row);  
        if (DocmaConstants.DEBUG) {
            Log.info("Select combobox event: " + box_id + 
                     "  New value: '" + value + "'  Old value: '" + old_value + "'");
        }
        int sel_start = 0;
        int sel_end = 0;
        if (value.equals("%br")) {
            if (! old_value.trim().equals("")) {  // if cell is not empty, then append line break
                value = old_value.trim() + ' ' + value;
                combo_box.setValue(value);
            }
        } else
        if (value.startsWith("%image{")) {
            value = "%image{alias; height: }";   // add parameter hint
            combo_box.setValue(value);
            sel_start = 7;   // select substring "alias" 
            sel_end = 12;
        } else
        if (value.startsWith("%style{")) {
            value = "%style{style_id}";   // add parameter hint
            if (old_value.contains("%style{")) {
                value = old_value;  // do not add style twice
            } else {
                value = (value + ' ' + old_value.trim()).trim();
                sel_start = 7;   // select substring "style_id" 
                sel_end = 15;
            }
            combo_box.setValue(value);
        } else 
        if (value.startsWith("%cols{")) {
            value = "%cols{2}";   // set default column span (2)
            if (!old_value.trim().equals("")) {
                // append column span but do not add column span twice
                value = old_value.contains("%cols{") ? old_value : value + ' ' + old_value.trim();
            }
            combo_box.setValue(value);
        } else 
        if (value.startsWith("%rows{")) {
            value = "%rows{2}";   // set default row span (2)
            if (!old_value.trim().equals("")) {
                // append row span but do not add row span twice
                value = old_value.contains("%rows{") ? old_value : value + ' ' + old_value.trim();
            }
            combo_box.setValue(value);
        } else {
            if (old_value.contains("%style{") || old_value.contains("%rows{") || 
                old_value.contains("%cols{") || old_value.trim().endsWith("%br")) {
                // append selected value (keep existing %style, %rows, %cols or %br) 
                if (! old_value.contains(value)) {  // do not add twice
                    value = old_value.trim() + ' ' + value;  
                    combo_box.setValue(value);
                }
            }
        }
        if (sel_start >= 0) {
            combo_box.setFocus(true);
            combo_box.setSelectionRange(sel_start, sel_end);
        }
        on_HeaderFooterContentChanged(region, col, row, value);
    }
    
    public void onHeaderFooterContentChanged(ForwardEvent evt)
    {
        String box_id = evt.getOrigin().getTarget().getId();
        
        Combobox combo_box = (Combobox) getFellow(box_id);
        String region = getRegionFromBoxId(box_id);
        int col = getColumnFromBoxId(box_id);
        int row = getRowFromBoxId(box_id);
        String value = combo_box.getValue().trim();
        
        if (DocmaConstants.DEBUG) {
            String old_value = getOldHeaderFooterContent(region, col, row);
            Log.info("Changed combobox event: " + box_id + 
                     "  New value: '" + value + "'  Old value: '" + old_value + "'");
        }
        on_HeaderFooterContentChanged(region, col, row, value);
    }
    
    private void on_HeaderFooterContentChanged(String regionId, int colNum, int rowNum, String value)
    {
        String pageType = getHeaderFooterPageType();
        if (pageType == null) return;

        String key = getHeaderFooterContentKey(pageType, regionId, colNum, rowNum);
        if (DocmaConstants.DEBUG) {
            Log.info("Changed header/footer content: " + key + "  Value: " + value);
        }
        headerFooterContentMap.put(key, value);
    }

    public void onHeaderFooterPreview()
    {
        if (headerFooterPublicationPreviewBox == null) initFields();
        
        Listitem item = headerFooterPublicationPreviewBox.getSelectedItem();
        if (item == null) {
            Messagebox.show("Please select a publication first!");
            return;
        }
        try {
            String pub_id = item.getValue().toString();
            long millis = System.currentTimeMillis();
            String temp_out_id = "__temp_preview_outconf_" + millis;
            DocmaOutputConfig tempOutConf = new DocmaOutputConfig(temp_out_id);
            updateModel(tempOutConf);
            Desktop desk = this.getDesktop();
            DocmaWebSession webSess = GUIUtil.getDocmaWebSession(this);
            webSess.setSessionObject(temp_out_id, tempOutConf);
            
            String url = "viewPDF.jsp?desk=" + desk.getId() +
                         // "&nodeid=" + nodeId +   // no node given -> preview content root of publication
                         "&pub=" + pub_id + 
                         "&tempout=" + temp_out_id +
                         "&draft=true&stamp=" + millis;
            url = desk.getExecution().encodeURL(url);
            String client_action = "window.open('" + url +
                "', '_blank', 'width=" + webSess.getPdfPreviewWidth() + 
                ",height=" + webSess.getPdfPreviewHeight() +
                ",left=" + webSess.getPdfPreviewPositionX() + 
                ",top=" + webSess.getPdfPreviewPositionY() +
                ",resizable=yes,status=yes,location=no,menubar=no');";
            Clients.evalJavaScript(client_action);
        } catch (Exception ex) {
            Messagebox.show("Internal error: " + ex.getMessage());
        }
    }

    /* -----------  Private methods  ------------------ */

    private MainWindow getMainWindow()
    {
        DocmaWebSession webSess = GUIUtil.getDocmaWebSession(this);
        return webSess.getMainWindow();
    }
    
    private void activateHTMLTab()
    {
        updateGUI_HTML();
        // formatInclude.setSrc("config_media_html.zul");
        htmlOptionsTab.setVisible(true);
        // htmlOptionsTab.setSelected(true);
        pdfOptionsTab.setVisible(false);
        headerFooterOptionsTab.setVisible(false);
        docbookOptionsTab.setVisible(false);
    }

    private void activatePDFTab()
    {
        updateGUI_PDF();
        updateGUI_HeaderFooter();
        // formatInclude.setSrc("config_media_pdf.zul");
        pdfOptionsTab.setVisible(true);
        // pdfOptionsTab.setSelected(true);
        headerFooterOptionsTab.setVisible(true);
        htmlOptionsTab.setVisible(false);
        docbookOptionsTab.setVisible(false);
    }

    private void activateDocBookTab()
    {
        updateGUI_DocBook();
        // formatInclude.setSrc("config_media_docbook.zul");
        docbookOptionsTab.setVisible(false); // no DocBook options available yet
        // docbookOptionsTab.setSelected(true);
        htmlOptionsTab.setVisible(false);
        pdfOptionsTab.setVisible(false);
        headerFooterOptionsTab.setVisible(false);
    }

    private void enableCustomPaperSizeBoxes(boolean enable)
    {
        pdfPageWidthBox.setDisabled(! enable);
        pdfPageWidthUnitBox.setDisabled(! enable);
        pdfPageHeightBox.setDisabled(! enable);
        pdfPageHeightUnitBox.setDisabled(! enable);
    }

    private void initFields()
    {
        if (initialized) return;

        // formatInclude = (Include) getFellow("MediaConfigFormat_include");
        generalOptionsTab = (Tab) getFellow("MediaGeneralOptionsTab");
        htmlOptionsTab = (Tab) getFellow("HTMLOptionsTab");
        pdfOptionsTab = (Tab) getFellow("PDFOptionsTab");
        headerFooterOptionsTab = (Tab) getFellow("HeaderFooterOptionsTab");
        docbookOptionsTab = (Tab) getFellow("DocBookOptionsTab");

        // General options
        idBox = (Textbox) getFellow("MediaConfigIDTextbox");
        filterBox = (Textbox) getFellow("MediaConfigFilterSettingTextbox");
        tocBox = (Checkbox) getFellow("MediaConfigTOCCheckbox");
        indexBox = (Checkbox) getFellow("MediaConfigIndexCheckbox");
        maxTocDepthBox = (Listbox) getFellow("MediaConfigMaxTOCDepthListbox");
        tocIndentBox = (Textbox) getFellow("MediaConfigTOCIndentTextbox");
        tocNamedPartBox = (Checkbox) getFellow("MediaConfigTOCNamedPartCheckbox");
        tocNamedChapterBox = (Checkbox) getFellow("MediaConfigTOCNamedChapterCheckbox");
        partTocBox = (Checkbox) getFellow("MediaConfigPartTOCCheckbox");
        chapterTocBox = (Checkbox) getFellow("MediaConfigChapterTOCCheckbox");
        sectionTocBox = (Checkbox) getFellow("MediaConfigSectionTOCCheckbox");
        sectionTocLevelsBox = (Listbox) getFellow("MediaConfigSectionTOCLevelsListbox");
        sectionTocDepthBox = (Listbox) getFellow("MediaConfigSectionTOCDepthListbox");
        styleVariantBox = (Listbox) getFellow("MediaConfigStyleVariantListbox");
        formatBox = (Listbox) getFellow("MediaConfigFormatListbox");
        paraSpaceBox = (Textbox) getFellow("MediaConfigParaSpaceTextbox");
        paraSpaceUnitBox = (Listbox) getFellow("MediaConfigParaSpaceUnitListbox");
        paraIndentBox = (Textbox) getFellow("MediaConfigParaIndentTextbox");
        paraIndentUnitBox = (Listbox) getFellow("MediaConfigParaIndentUnitListbox");
        listItemSpaceBox = (Textbox) getFellow("MediaConfigItemMarginTextbox");
        listItemSpaceUnitBox = (Listbox) getFellow("MediaConfigItemMarginUnitListbox");
        listIndentBox = (Textbox) getFellow("MediaConfigListIndentTextbox");
        listIndentUnitBox = (Listbox) getFellow("MediaConfigListIndentUnitListbox");
        listLabelWidthBox = (Textbox) getFellow("MediaConfigListLabelWidthTextbox");
        listLabelWidthUnitBox = (Listbox) getFellow("MediaConfigListLabelWidthUnitListbox");
        titlePlacementBox = (Listbox) getFellow("MediaConfigTitlePlacementListbox");

        // Numbering options
        render1stLevelBox = (Listbox) getFellow("MediaConfigRender1stLevelListbox");
        partNumberBox = (Checkbox) getFellow("MediaConfigPartNumberCheckbox");
        partNumberFormatBox = (Listbox) getFellow("MediaConfigPartNumberListbox");
        chapterNumberBox = (Checkbox) getFellow("MediaConfigChapterNumberCheckbox");
        chapterNumberFormatBox = (Listbox) getFellow("MediaConfigChapterNumberListbox");
        sectionNumberBox = (Checkbox) getFellow("MediaConfigSectionNumberCheckbox");
        sectionNumberFormatBox = (Listbox) getFellow("MediaConfigSectionNumberListbox");
        appendixNumberBox = (Checkbox) getFellow("MediaConfigAppendixNumberCheckbox");
        appendixNumberFormatBox = (Listbox) getFellow("MediaConfigAppendixNumberListbox");
        footnoteNumberFormatBox = (Listbox) getFellow("MediaConfigFootnoteNumberListbox");
        numberingDepthBox = (Intbox) getFellow("MediaConfigNumberingDepthIntbox");
        omitSingleSectionBox = (Checkbox) getFellow("MediaConfigOmitSingleSectionTitleCheckbox");
        exclude1stLevelNumberBox = (Checkbox) getFellow("MediaConfigExclude1stLevelNumberCheckbox");
        restartPartBox = (Checkbox) getFellow("MediaConfigRestartInPartCheckbox");

        // HTML options
        htmlFileTypeRadiogroup = (Radiogroup) getFellow("MediaHTMLFileTypeRadiogroup");
        htmlSingleFileRadio = (Radio) getFellow("MediaHTMLSingleFileRadio");
        htmlMultipleFilesRadio = (Radio) getFellow("MediaHTMLMultipleFilesRadio");
        // htmlFilePrefixBox = (Textbox) getFellow("MediaHTMLFilePrefixTextbox");
        htmlSeparateFileBox = (Checkbox) getFellow("MediaHTMLCreateSeparateFileCheckbox");
        htmlSeparateFileLevelBox = (Listbox) getFellow("MediaHTMLSeparateFileLevelListbox");
        // htmlCreateDirBox = (Checkbox) getFellow("MediaHTMLCreateDirCheckbox");
        // htmlDirLevelBox = (Listbox) getFellow("MediaHTMLDirLevelListbox");
        htmlAliasFilenameBox = (Checkbox) getFellow("MediaHTMLAliasFilenameCheckbox");
        htmlInclude1stSectBox = (Checkbox) getFellow("MediaHTMLInclude1stSubSectionCheckbox");
        htmlSeparateTOCBox = (Checkbox) getFellow("MediaHTMLSeparateFileTOCCheckbox");
        // htmlSeparateEachTableBox = (Checkbox) getFellow("MediaHTMLSeparateFileEachContentTableCheckbox");
        htmlNavigationIconsBox = (Checkbox) getFellow("MediaHTMLNavigationalIconsCheckbox");
        htmlNavigationTitlesBox = (Checkbox) getFellow("MediaHTMLNavigationalTitlesCheckbox");
        htmlBreadcrumbsBox = (Checkbox) getFellow("MediaHTMLBreadcrumbsCheckbox");
        htmlBreadcrumbStartBox = (Listbox) getFellow("MediaHTMLBreadcrumbsStartListbox");
        htmlBreadcrumbSeparatorBox = (Textbox) getFellow("MediaHTMLBreadcrumbsSepTextbox");
        htmlRootFolderBox = (Textbox) getFellow("MediaHTMLRootFolderTextbox");
        htmlURLTargetWindowBox = (Textbox) getFellow("MediaHTMLURLTargetTextbox");
        htmlCustomHeaderBox = (Textbox) getFellow("MediaHTMLCustomHeaderTextbox");
        htmlCustomFooterBox = (Textbox) getFellow("MediaHTMLCustomFooterTextbox");
        // htmlCoverImageBox = (Textbox) getFellow("MediaHTMLCoverImageTextbox");
        htmlCustomCSSBox = (Listbox) getFellow("MediaHTMLCustomCSSListbox");
        htmlCustomJSBox = (Listbox) getFellow("MediaHTMLCustomJSListbox");
        htmlCustomMetaBox = (Listbox) getFellow("MediaHTMLCustomMetaListbox");
        htmlCustomFilesBox = (Textbox) getFellow("MediaHTMLCustomFilesTextbox");
        htmlOutputEncodingBox = (Listbox) getFellow("MediaHTMLOutputEncodingListbox");
        htmlDocTypeArea = getFellow("MediaHTMLDocTypeArea");
        htmlDocTypeCheckBox = (Checkbox) getFellow("MediaHTMLDocTypeCheckbox");
        htmlDocTypeTextBox = (Textbox) getFellow("MediaHTMLDocTypeTextbox");
        htmlWebhelpConfigBox = (Listbox) getFellow("MediaHTMLWebhelpConfigListbox");
        htmlWebhelpHeader1Box = (Listbox) getFellow("MediaHTMLWebhelpHead1Listbox");
        htmlWebhelpHeader2Box = (Listbox) getFellow("MediaHTMLWebhelpHead2Listbox");

        // PDF options
        pdfPaperSizeBox = (Listbox) getFellow("MediaPDFPaperSizeListbox");
        pdfPageWidthBox = (Textbox) getFellow("MediaPDFPageWidthTextbox");
        pdfPageWidthUnitBox = (Listbox) getFellow("MediaPDFPageWidthUnitListbox");
        pdfPageHeightBox = (Textbox) getFellow("MediaPDFPageHeightTextbox");
        pdfPageHeightUnitBox = (Listbox) getFellow("MediaPDFPageHeightUnitListbox");
        pdfPageOrientRadiogroup = (Radiogroup) getFellow("MediaPDFPageOrientationRadiogroup");
        pdfPagePortraitRadio = (Radio) getFellow("MediaPDFPagePortraitRadio");
        pdfPageLandscapeRadio = (Radio) getFellow("MediaPDFPageLandscapeRadio");
        pdfPageTopBox = (Textbox) getFellow("MediaPDFPageTopTextbox");
        pdfPageTopUnitBox = (Listbox) getFellow("MediaPDFPageTopUnitListbox");
        pdfPageBottomBox = (Textbox) getFellow("MediaPDFPageBottomTextbox");
        pdfPageBottomUnitBox = (Listbox) getFellow("MediaPDFPageBottomUnitListbox");
        pdfPageInnerBox = (Textbox) getFellow("MediaPDFPageInnerTextbox");
        pdfPageInnerUnitBox = (Listbox) getFellow("MediaPDFPageInnerUnitListbox");
        pdfPageOuterBox = (Textbox) getFellow("MediaPDFPageOuterTextbox");
        pdfPageOuterUnitBox = (Listbox) getFellow("MediaPDFPageOuterUnitListbox");
        pdfBodyTopBox = (Textbox) getFellow("MediaPDFBodyTopTextbox");
        pdfBodyTopUnitBox = (Listbox) getFellow("MediaPDFBodyTopUnitListbox");
        pdfBodyBottomBox = (Textbox) getFellow("MediaPDFBodyBottomTextbox");
        pdfBodyBottomUnitBox = (Listbox) getFellow("MediaPDFBodyBottomUnitListbox");
        pdfBodyStartBox = (Textbox) getFellow("MediaPDFBodyStartTextbox");
        pdfBodyStartUnitBox = (Listbox) getFellow("MediaPDFBodyStartUnitListbox");
        pdfBodyEndBox = (Textbox) getFellow("MediaPDFBodyEndTextbox");
        pdfBodyEndUnitBox = (Listbox) getFellow("MediaPDFBodyEndUnitListbox");
        pdfHeaderHeightBox = (Textbox) getFellow("MediaPDFHeaderHeightTextbox");
        pdfHeaderHeightUnitBox = (Listbox) getFellow("MediaPDFHeaderHeightUnitListbox");
        pdfFooterHeightBox = (Textbox) getFellow("MediaPDFFooterHeightTextbox");
        pdfFooterHeightUnitBox = (Listbox) getFellow("MediaPDFFooterHeightUnitListbox");
        pdfDoubleSidedBox = (Checkbox) getFellow("MediaPDFDoubleSidedCheckbox");
        pdfBookmarksBox = (Checkbox) getFellow("MediaPDFBookmarksCheckbox");
        pdfNumbersInRefsBox = (Checkbox) getFellow("MediaPDFPageNumbersInRefsCheckbox");
        pdfExportFilesBox = (Checkbox) getFellow("MediaPDFExportFilesCheckbox");
        pdfFitImagesBox = (Checkbox) getFellow("MediaPDFFitImagesCheckbox");
        pdfShowExternalHRefBox = (Checkbox) getFellow("MediaPDFShowExternalHRefCheckbox");
        pdfShowExternalHRefTypeBox = (Listbox) getFellow("MediaPDFShowExternalHRefTypeListbox");
        pdfSourceResolutionBox = (Intbox) getFellow("MediaPDFSourceResolution");
        pdfTargetResolutionBox = (Intbox) getFellow("MediaPDFTargetResolution");
        pdfCoverModeBox = (Listbox) getFellow("MediaPDFCoverModeListbox");
        pdfCoverBlankPageBox = (Checkbox) getFellow("MediaPDFCoverBlankPageCheckbox");
        pdfCoverXBox = (Textbox) getFellow("MediaPDFCoverPosXBox");
        pdfCoverXUnitBox = (Listbox) getFellow("MediaPDFCoverXUnitListbox");
        pdfCoverYBox = (Textbox) getFellow("MediaPDFCoverPosYBox");
        pdfCoverYUnitBox = (Listbox) getFellow("MediaPDFCoverYUnitListbox");
        pdfIndexColumnCountBox = (Listbox) getFellow("MediaPDFIndexColCountListbox");
        pdfIndexColumnGapBox = (Textbox) getFellow("MediaPDFIndexGapTextbox");
        pdfIndexColumnGapUnitBox = (Listbox) getFellow("MediaPDFIndexGapUnitListbox");

        // Header/Footer options
        markerSectionLevelBox = (Listbox) getFellow("MediaHeaderFooterSectDepthListbox");
        customHeaderFooterBox = (Checkbox) getFellow("MediaHeadFootCustomConfigCheckbox");
        headerFooterPageClassBox = (Listbox) getFellow("MediaHeaderFooterPageClassListbox");
        headerFooterPageSequenceBox = (Listbox) getFellow("MediaHeaderFooterSequenceListbox");
        headerFooterSameAsBodyBox = (Checkbox) getFellow("MediaHeaderFooterSameAsBodyBox");
        headerFooterSameAsOddBox = (Checkbox) getFellow("MediaHeaderFooterSameAsOddBox");
        headerFooterPublicationPreviewBox = (Listbox) getFellow("MediaHeaderFooterPublicationPreviewListbox");
        
        headerWidth1Label = (Label) getFellow("MediaHeaderWidthLeftLabel");
        headerWidth3Label = (Label) getFellow("MediaHeaderWidthRightLabel");
        footerWidth1Label = (Label) getFellow("MediaFooterWidthLeftLabel");
        footerWidth3Label = (Label) getFellow("MediaFooterWidthRightLabel");
    
        headerWidth1Box = (Intbox) getFellow("MediaHeaderWidthLeftBox");
        headerWidth2Box = (Intbox) getFellow("MediaHeaderWidthCenterBox");
        headerWidth3Box = (Intbox) getFellow("MediaHeaderWidthRightBox");
        footerWidth1Box = (Intbox) getFellow("MediaFooterWidthLeftBox");
        footerWidth2Box = (Intbox) getFellow("MediaFooterWidthCenterBox");
        footerWidth3Box = (Intbox) getFellow("MediaFooterWidthRightBox");
        
        initialized = true;
    }

    private int getModalResult()
    {
        return modalResult;
    }

    private boolean hasInvalidInputs() throws Exception
    {
        String id = idBox.getValue().trim();
        if (id.equals("")) {
            Messagebox.show("Please enter an ID value.");
            return true;
        }
        if (id.length() > 30) {
            Messagebox.show("ID is too long. Maximum length is 30 characters.");
            return true;
        }
        if (!id.matches(DocmaConstants.REGEXP_ID)) {
            Messagebox.show("Invalid ID. Allowed characters are ASCII letters, underscore and dash.");
            return true;
        }
        if (mode == MODE_NEW) {
            String[] ids = docmaSess.getOutputConfigIds();
            for (String id_value : ids) {
                if (id.equalsIgnoreCase(id_value)) {
                    Messagebox.show("An output configuration with this ID already exists.");
                    return true;
                }
            }
        }
        String filter = filterBox.getValue().trim();
        if (filter.length() > 0) {
            String[] applics = filter.split("[, ]");
            List declaredList = Arrays.asList(docmaSess.getDeclaredApplics());
            for (String applic : applics) {
                if (!declaredList.contains(applic)) {
                    Messagebox.show("Invalid filter setting. Applicability '" + applic + "' is not declared.");
                    return true;
                }
            }
        }
        if (invalidFloatNumber(tocIndentBox)) return true;
        if (invalidFloatNumber(paraSpaceBox)) return true;
        if (invalidFloatNumber(paraIndentBox)) return true;
        if (invalidFloatNumber(listItemSpaceBox)) return true;
        if (invalidFloatNumber(listIndentBox)) return true;
        if (invalidFloatNumber(listLabelWidthBox)) return true;

        String guiformat = getCurrentSelectedGUIFormat();
        String format = getMainFormat(guiformat);
        if (format.equals("html")) {
            if (! guiformat.equals("webhelp1")) { // custom header is disabled for WebHelp v1
                if (invalidContentAlias(htmlCustomHeaderBox.getValue().trim())) return true;
            }
            if (invalidContentAlias(htmlCustomFooterBox.getValue().trim())) return true;
            // if (invalidImageAlias(htmlCoverImageBox.getValue().trim())) return true;
        }
        if (format.equals("pdf")) {
            if (invalidFloatNumber(pdfPageWidthBox)) return true;
            if (invalidFloatNumber(pdfPageHeightBox)) return true;
            if (invalidFloatNumber(pdfPageTopBox)) return true;
            if (invalidFloatNumber(pdfPageBottomBox)) return true;
            if (invalidFloatNumber(pdfPageInnerBox)) return true;
            if (invalidFloatNumber(pdfPageOuterBox)) return true;
            if (invalidFloatNumber(pdfBodyTopBox)) return true;
            if (invalidFloatNumber(pdfBodyBottomBox)) return true;
            if (invalidFloatNumber(pdfBodyStartBox)) return true;
            if (invalidFloatNumber(pdfBodyEndBox)) return true;
            if (invalidFloatNumber(pdfHeaderHeightBox)) return true;
            if (invalidFloatNumber(pdfFooterHeightBox)) return true;
            if (invalidFloatNumber(pdfCoverXBox)) return true;
            if (invalidFloatNumber(pdfCoverYBox)) return true;
            if (invalidSignedFloatNumber(pdfIndexColumnGapBox)) return true;
            if (customHeaderFooterBox.isChecked()) {
                String h1 = getHeaderFooterWidth(headerWidth1Box);
                String h2  = getHeaderFooterWidth(headerWidth2Box);
                String h3  = getHeaderFooterWidth(headerWidth3Box);
                String f1  = getHeaderFooterWidth(footerWidth1Box);
                String f2  = getHeaderFooterWidth(footerWidth2Box);
                String f3  = getHeaderFooterWidth(footerWidth3Box);
                int hlen = h1.length() + h2.length() + h3.length();
                if ((hlen > 0) && (h1.equals("") || h2.equals("") || h3.equals(""))) {
                    Messagebox.show("Missing header width!");
                    return true;
                }
                int flen = f1.length() + f2.length() + f3.length();
                if ((flen > 0) && (f1.equals("") || f2.equals("") || f3.equals(""))) {
                    Messagebox.show("Missing footer width!");
                    return true;
                }
            }
        }
        return false;
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
    
    private DocmaNode getValidContentNode(String alias) 
    {
        DocmaNode nd = docmaSess.getNodeByAlias(alias);
        if (nd == null) {
            Messagebox.show("Node with alias name '" + alias + "' not found.");
            return null;
        }
        if (! nd.isHTMLContent()) {
            Messagebox.show("Node with alias name '" + alias + "' has wrong node type. Must be a content node.");
            return null;
        }
        return nd;
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

    private boolean invalidFloatNumber(Textbox box) throws Exception
    {
        String val = box.getValue().trim();
        // if (val.equals("")) return false;  // no input -> use default value
        if (isInvalidFloatNumber(val)) {
            box.focus();
            Messagebox.show("Invalid number: " + val);
            return true;
        }
        return false;
    }
    
    private boolean invalidSignedFloatNumber(Textbox box) throws Exception
    {
        String val = box.getValue().trim();
        // if (val.equals("")) return false;  // no input -> use default value
        if (isInvalidSignedFloatNumber(val)) {
            box.focus();
            Messagebox.show("Invalid number: " + val);
            return true;
        }
        return false;
    }
    
    private boolean isInvalidFloatNumber(String val)
    {
        if (val.equals("")) return false;  // no input -> use default value
        return ! val.matches("[0-9]+([.,][0-9]+)?");
    }

    private boolean isInvalidSignedFloatNumber(String val)
    {
        if (val.equals("")) return false;  // no input -> use default value
        return ! val.matches("-?[0-9]+([.,][0-9]+)?");
    }

    private void initStyleVariantItems(String selected)
    {
        styleVariantBox.getItems().clear();
        styleVariantBox.appendItem("", "");
        int sel_idx = 0;
        String[] variants = docmaSess.getStyleVariantIds();
        for (int i=0; i < variants.length; i++) {
            styleVariantBox.appendItem(variants[i], variants[i]);
            if (variants[i].equals(selected)) sel_idx = i+1;
        }
        styleVariantBox.setSelectedIndex(sel_idx);
    }


    private void updateGUI()
    {
        idBox.setValue(outConf.getId());

        String format = outConf.getFormat();
        String subformat = outConf.getSubformat();
        String format_gui = format;
        if (format.equals("html")) {
            if (subformat.startsWith("webhelp") || subformat.startsWith("epub")) {
                format_gui = subformat;
            }
        }
        if (! selectItemByValue(formatBox, format_gui)) {
            formatBox.setSelectedIndex(0);
        }

        filterBox.setValue(outConf.getFilterSetting());
        tocBox.setChecked(outConf.isToc());
        indexBox.setChecked(outConf.isIndex());
        int max_toc_depth = outConf.getMaxTocDepth();
        if (max_toc_depth > 10) {
            max_toc_depth = 10;  // In the GUI listbox the maximum value is 10
        }
        selectItemByValue(maxTocDepthBox, Integer.toString(max_toc_depth), 3);
        int sectLevels = outConf.getSectionTocLevels();
        boolean isPartToc = outConf.isPartToc();
        boolean isChapToc = outConf.isChapterToc();
        boolean isSectToc = outConf.isSectionToc() && (sectLevels > 0);
        partTocBox.setChecked(isPartToc);
        chapterTocBox.setChecked(isChapToc);
        sectionTocBox.setChecked(isSectToc);
        selectItemByValue(sectionTocLevelsBox, Integer.toString(sectLevels), 1);
        sectionTocLevelsBox.setDisabled(! isSectToc);

        int sect_toc_depth = outConf.getSectionTocDepth();
        if (sect_toc_depth > 10) {
            sect_toc_depth = 10;  // In the GUI listbox the maximum value is 10
        }
        boolean isComponentToc = isPartToc || isChapToc || isSectToc;
        selectItemByValue(sectionTocDepthBox, isComponentToc ? Integer.toString(sect_toc_depth) : "0", 0);
        sectionTocDepthBox.setDisabled(! isComponentToc);
        
        String toc_indent = outConf.getTocIndentWidth();
        toc_indent = (toc_indent == null) ? "" : toc_indent.trim().toLowerCase();
        if (toc_indent.endsWith("pt")) {
            toc_indent = toc_indent.substring(0, toc_indent.length() - 2);
        }
        setNonNegativeInt(tocIndentBox, toc_indent, "");
        
        String toc_named = outConf.getTocNamedLabels();
        toc_named = (toc_named == null) ? "" : toc_named.toLowerCase();
        tocNamedPartBox.setChecked(toc_named.contains("part"));
        tocNamedChapterBox.setChecked(toc_named.contains("chapter"));

        initStyleVariantItems(outConf.getStyleVariant());
        setDim(paraSpaceBox, paraSpaceUnitBox, outConf.getParaSpace());
        setDim(paraIndentBox, paraIndentUnitBox, outConf.getParaIndent());
        setDim(listItemSpaceBox, listItemSpaceUnitBox, outConf.getItemSpace());
        setDim(listIndentBox, listIndentUnitBox, outConf.getListIndent());
        setDim(listLabelWidthBox, listLabelWidthUnitBox, outConf.getOrderedListLabelWidth());
        selectItemByValue(titlePlacementBox, outConf.getTitlePlacement());
        selectItemByValue(render1stLevelBox, outConf.getRender1stLevel());
        String pn = outConf.getPartNumbering();
        if (pn.equals("0")) {
            partNumberBox.setChecked(false);
            selectItemByValue(partNumberFormatBox, "I");  // default
        } else {
            partNumberBox.setChecked(true);
            selectItemByValue(partNumberFormatBox, pn);
        }
        String cn = outConf.getChapterNumbering();
        if (cn.equals("0")) {
            chapterNumberBox.setChecked(false);
            selectItemByValue(chapterNumberFormatBox, "1");  // default
        } else {
            chapterNumberBox.setChecked(true);
            selectItemByValue(chapterNumberFormatBox, cn);
        }
        String sn = outConf.getSectionNumbering();
        if (sn.equals("0")) {
            sectionNumberBox.setChecked(false);
            selectItemByValue(sectionNumberFormatBox, "1");  // default
        } else {
            sectionNumberBox.setChecked(true);
            selectItemByValue(sectionNumberFormatBox, sn);
        }
        String an = outConf.getAppendixNumbering();
        if (an.equals("0")) {
            appendixNumberBox.setChecked(false);
            selectItemByValue(appendixNumberFormatBox, "A");  // default
        } else {
            appendixNumberBox.setChecked(true);
            selectItemByValue(appendixNumberFormatBox, an);
        }
        String fn = outConf.getFootnoteNumbering();
        if (fn.equals("")) fn = "1";
        selectItemByValue(footnoteNumberFormatBox, fn);

        // numberingBox.setChecked(outConf.getNumberingDepth() > 0);
        // if (sectionNumberBox.isChecked()) {
        numberingDepthBox.setValue(new Integer(outConf.getNumberingDepth()));
        // } else {
        //     numberingDepthBox.setValue(new Integer(3));  // initialize with default value
        // }
        omitSingleSectionBox.setChecked(outConf.isOmitSingleTitle());
        exclude1stLevelNumberBox.setChecked(outConf.isExclude1stLevelNumber());
        restartPartBox.setChecked(outConf.isRestartPart());
        
        if (format.equals("html")) {
            activateHTMLTab();
        } else
        if (format.equals("pdf")) {
            activatePDFTab();
        } else
        if (format.equals("docbook")) {
            activateDocBookTab();
        }
    }

    private boolean isWebHelp(String format)
    {
        return format.startsWith("webhelp");
    }
    
    private boolean isWebHelp1(String format)
    {
        return format.equals("webhelp1");
    }
    
    private void updateGUI_HTML()
    {
        boolean is_singlefile = outConf.isHtmlSingleFile();
        String currentFormat = getCurrentSelectedGUIFormat();
        boolean is_webhelp = isWebHelp(currentFormat);
        boolean is_webhelp_1 = isWebHelp1(currentFormat);
        boolean is_webhelp_new = is_webhelp && !is_webhelp_1;
        boolean is_epub = currentFormat.equals("epub");
        if (is_webhelp || is_epub) is_singlefile = false;
        htmlSingleFileRadio.setChecked(is_singlefile);
        htmlSingleFileRadio.setDisabled(is_webhelp || is_epub);
        htmlMultipleFilesRadio.setChecked(!is_singlefile);
        // htmlFilePrefixBox.setValue(outConf.getHtmlFilePrefix());
        htmlSeparateFileBox.setChecked(outConf.getHtmlSeparateFileLevel() > 0);
        boolean hit = selectItemByValue(htmlSeparateFileLevelBox, "" + outConf.getHtmlSeparateFileLevel());
        if (! hit) {
            htmlSeparateFileLevelBox.setSelectedIndex(0);
        }
        // htmlCreateDirBox.setChecked(outConf.getHtmlDirLevel() > 0);
        // selectItemByValue(htmlDirLevelBox, "" + outConf.getHtmlDirLevel());
        htmlAliasFilenameBox.setChecked(outConf.isHtmlAliasFilename());
        htmlInclude1stSectBox.setChecked(outConf.isHtmlInclude1stSection());
        htmlSeparateTOCBox.setChecked(outConf.isHtmlSeparateTOC() || is_epub);
        htmlSeparateTOCBox.setDisabled(is_epub);
        // htmlSeparateEachTableBox.setChecked(outConf.isHtmlSeparateEachTable());
        htmlNavigationIconsBox.setChecked(outConf.isHtmlNavigationalIcons());
        htmlNavigationTitlesBox.setChecked(outConf.isHtmlNavigationalTitles());
        
        boolean is_bread = outConf.isHtmlBreadcrumbs();
        String bread_start = outConf.getHtmlBreadcrumbStart();
        String bread_sep = outConf.getHtmlBreadcrumbSeparator();
        htmlBreadcrumbsBox.setChecked(is_bread);
        selectItemByValue(htmlBreadcrumbStartBox, bread_start, 0);
        htmlBreadcrumbSeparatorBox.setValue((bread_sep == null) ? "" : bread_sep);
        htmlBreadcrumbStartBox.setDisabled(! (is_bread && is_webhelp_new));
        htmlBreadcrumbSeparatorBox.setDisabled(! is_bread);
        
        htmlRootFolderBox.setValue(outConf.getHtmlRootFolder());
        htmlRootFolderBox.setDisabled(is_epub);
        
        String targetwin = outConf.getHtmlURLTargetWindow();
        if (targetwin.equals("")) targetwin = DocmaConstants.DEFAULT_URL_TARGET_WINDOW;
        htmlURLTargetWindowBox.setValue(targetwin);
        htmlCustomHeaderBox.setValue(outConf.getHtmlCustomHeaderAlias());
        htmlCustomHeaderBox.setDisabled(is_webhelp_1);
        htmlCustomFooterBox.setValue(outConf.getHtmlCustomFooterAlias());
        // htmlCustomFooterBox.setDisabled(is_webhelp_1);
        // htmlCoverImageBox.setValue(outConf.getHtmlCoverImageAlias());
        // htmlCoverImageBox.setDisabled(is_webhelp);

        htmlCustomCSSBox.getItems().clear();
        htmlCustomJSBox.getItems().clear();
        htmlCustomMetaBox.getItems().clear();
        htmlWebhelpConfigBox.getItems().clear();
        htmlCustomCSSBox.appendItem("", "");
        htmlCustomJSBox.appendItem("", "");
        htmlCustomMetaBox.appendItem("", "");
        htmlWebhelpConfigBox.appendItem("", "");
        DocmaNode conf_folder = docmaSess.getNodeByAlias(DocmaConstants.HTML_CONFIG_FOLDER_ALIAS_NAME);
        if ((conf_folder != null) && conf_folder.isFolder()) {
            for (int i=0; i < conf_folder.getChildCount(); i++) {
                DocmaNode nd = conf_folder.getChild(i);
                if (nd.isFileContent()) {
                    String fn = nd.getDefaultFileName();
                    String fn_low = fn.toLowerCase();
                    if (fn_low.endsWith(".css")) {
                        htmlCustomCSSBox.appendItem(fn, fn);
                    } else
                    if (fn_low.endsWith(".js")) {
                        htmlCustomJSBox.appendItem(fn, fn);
                    } else 
                    if (fn_low.endsWith(".xml") || fn_low.endsWith(".xhtml") || fn_low.endsWith(".xhtm")) {
                        htmlCustomMetaBox.appendItem(fn, fn);
                    } else 
                    if (fn_low.endsWith(".zip")) {
                        htmlWebhelpConfigBox.appendItem(fn, fn);
                    }
                } else
                if (nd.isFolder()) {
                    String foldername = nd.getDefaultFileName();
                    htmlWebhelpConfigBox.appendItem(foldername, foldername);
                }
            }
        }
        selectItemByValue(htmlCustomCSSBox, outConf.getHtmlCustomCSSFilename(), 0);
        selectItemByValue(htmlCustomJSBox, outConf.getHtmlCustomJSFilename(), 0);
        // htmlCustomJSBox.setDisabled(is_webhelp);
        selectItemByValue(htmlCustomMetaBox, outConf.getHtmlCustomMetaFilename(), 0);

        String docType = is_webhelp_new ? outConf.getHtmlCustomDocType() : "";
        docType = (docType == null) ? "" : docType.trim();
        boolean hasCustomDocType = !docType.equals("");
        if (is_webhelp_new && !hasCustomDocType) {
            docType = WebFormatter.DOCTYPE_INTRO_DEFAULT;
        }
        htmlDocTypeCheckBox.setChecked(hasCustomDocType);
        htmlDocTypeCheckBox.setDisabled(! is_webhelp_new);
        htmlDocTypeTextBox.setValue(docType);
        htmlDocTypeTextBox.setDisabled(! is_webhelp_new);
        htmlDocTypeTextBox.setReadonly(! hasCustomDocType);
        htmlDocTypeArea.setVisible(is_webhelp_new);
        
        String custFiles = outConf.getHtmlCustomFiles();
        htmlCustomFilesBox.setValue((custFiles == null) ? "" : custFiles);
        selectItemByValue(htmlOutputEncodingBox, outConf.getHtmlOutputEncoding(), 0);
        String webhelpfolder = outConf.getHtmlWebhelpConfigFolder();
        if ((webhelpfolder == null) || webhelpfolder.equals("")) {
            webhelpfolder = "webhelp";  // default folder
        }
        selectItemByValue(htmlWebhelpConfigBox, webhelpfolder, 0);
        selectItemByValue(htmlWebhelpHeader1Box, outConf.getHtmlWebhelpHeader1(), "upper");
        selectItemByValue(htmlWebhelpHeader2Box, outConf.getHtmlWebhelpHeader2(), "current");
        htmlWebhelpConfigBox.setDisabled(! is_webhelp);
        htmlWebhelpHeader1Box.setDisabled(! is_webhelp_new);
        htmlWebhelpHeader2Box.setDisabled(! is_webhelp_new);
    }

    private void updateGUI_PDF()
    {
        selectItemByValue(pdfPaperSizeBox, outConf.getPdfPaperSize());
        boolean customPaperSize = outConf.getPdfPaperSize().equals("custom");
        if (customPaperSize) {
            setDim(pdfPageWidthBox, pdfPageWidthUnitBox, outConf.getPdfPageWidth());
            setDim(pdfPageHeightBox, pdfPageHeightUnitBox, outConf.getPdfPageHeight());
        } else {
            setDim(pdfPageWidthBox, pdfPageWidthUnitBox, "cm");
            setDim(pdfPageHeightBox, pdfPageHeightUnitBox, "cm");
        }
        enableCustomPaperSizeBoxes(customPaperSize);
        boolean isPortrait = outConf.getPdfPageOrientation().equals("portrait");
        pdfPagePortraitRadio.setChecked(isPortrait);
        pdfPageLandscapeRadio.setChecked(!isPortrait);
        setDim(pdfPageTopBox, pdfPageTopUnitBox, outConf.getPdfPageTop());
        setDim(pdfPageBottomBox, pdfPageBottomUnitBox, outConf.getPdfPageBottom());
        setDim(pdfPageInnerBox, pdfPageInnerUnitBox, outConf.getPdfPageInner());
        setDim(pdfPageOuterBox, pdfPageOuterUnitBox, outConf.getPdfPageOuter());
        setDim(pdfBodyTopBox, pdfBodyTopUnitBox, outConf.getPdfBodyTop());
        setDim(pdfBodyBottomBox, pdfBodyBottomUnitBox, outConf.getPdfBodyBottom());
        setDim(pdfBodyStartBox, pdfBodyStartUnitBox, outConf.getPdfBodyStart());
        setDim(pdfBodyEndBox, pdfBodyEndUnitBox, outConf.getPdfBodyEnd());
        setDim(pdfHeaderHeightBox, pdfHeaderHeightUnitBox, outConf.getPdfHeaderHeight());
        setDim(pdfFooterHeightBox, pdfFooterHeightUnitBox, outConf.getPdfFooterHeight());
        pdfDoubleSidedBox.setChecked(outConf.isPdfDoubleSided());
        pdfBookmarksBox.setChecked(outConf.isPdfBookmarks());
        pdfNumbersInRefsBox.setChecked(outConf.isPdfNumbersInRefs());
        pdfExportFilesBox.setChecked(outConf.isPdfExportReferencedFiles());
        pdfFitImagesBox.setChecked(outConf.isPdfFitImages());
        String showExtHRef = outConf.getPdfShowExternalHRef();
        boolean is_showExtHref = (showExtHRef != null) && (!showExtHRef.trim().equals("")) &&
                                 (!showExtHRef.equalsIgnoreCase("no"));
        pdfShowExternalHRefBox.setChecked(is_showExtHref);
        if (! selectItemByValue(pdfShowExternalHRefTypeBox, showExtHRef)) {
            pdfShowExternalHRefTypeBox.setSelectedIndex(0);
        }
        pdfShowExternalHRefTypeBox.setDisabled(!is_showExtHref);
        pdfSourceResolutionBox.setValue(Integer.valueOf(outConf.getPdfSourceResolution()));
        pdfTargetResolutionBox.setValue(Integer.valueOf(outConf.getPdfTargetResolution()));
        String cov_mode = outConf.getPdfCoverMode();
        if ((cov_mode == null) || cov_mode.trim().equals("")) {
            cov_mode = "extrapage";   // default value
        }
        boolean cov_blank = false;
        if (cov_mode.equals("extrapage_blank")) {
            cov_mode = "extrapage";
            cov_blank = true;
        }
        pdfCoverBlankPageBox.setChecked(cov_blank);
        selectItemByValue(pdfCoverModeBox, cov_mode);
        setDim(pdfCoverXBox, pdfCoverXUnitBox, outConf.getPdfCoverHorizontalPosition());
        setDim(pdfCoverYBox, pdfCoverYUnitBox, outConf.getPdfCoverVerticalPosition());
        if (! selectItemByValue(pdfIndexColumnCountBox, "" + outConf.getPdfIndexColumnCount())) {
            selectItemByValue(pdfIndexColumnCountBox, "2");
        }
        setDim(pdfIndexColumnGapBox, pdfIndexColumnGapUnitBox, outConf.getPdfIndexColumnGap());
    }

    private void updateGUI_HeaderFooter()
    {
        int sectLevel = outConf.getPdfMarkerSectionLevel();
        if (sectLevel < 1) sectLevel = 2;  // default
        selectItemByValue(markerSectionLevelBox, "" + sectLevel);
        
        boolean isCustom = outConf.isPdfCustomHeaderFooter();
        customHeaderFooterBox.setChecked(isCustom);
        setHeaderFooterFieldsDisabled(! isCustom);
        
        // Header widths
        String[] hwidths = parseColumnWidths(outConf.getPdfHeaderWidths());
        headerWidth1Box.setText(hwidths[0]);
        headerWidth2Box.setText(hwidths[1]);
        headerWidth3Box.setText(hwidths[2]);
        // Footer widths
        String[] fwidths = parseColumnWidths(outConf.getPdfFooterWidths());
        footerWidth1Box.setText(fwidths[0]);
        footerWidth2Box.setText(fwidths[1]);
        footerWidth3Box.setText(fwidths[2]);
        
        // Load header/footer content into map:
        headerFooterContentMap = new HashMap();
        headerFooterPageTypes = new TreeSet();
        String[] pageTypes = outConf.getPdfCustomHeaderFooterPageTypes();
        headerFooterPageTypes.addAll(Arrays.asList(pageTypes));
        final String[] regions = {"header", "footer"};
        for (String ptype : pageTypes) {
            for (String region : regions) {
                for (int col = 1; col <= 3; col++) {
                    for (int row = 1; row <= MAX_PDF_HEADER_FOOTER_ROWS; row++) {
                        String cont = outConf.getPdfHeaderFooterContent(ptype, region, col, row);
                        if (cont != null) {
                            String key = getHeaderFooterContentKey(ptype, region, col, row);
                            headerFooterContentMap.put(key, cont);
                        }
                    }
                }
            }
        }
        if (isCustom && headerFooterPageTypes.isEmpty()) {
            setDefaultCustomHeaderFooter();
        }
        
        resetGUI_HeaderFooterContent();
        
        // Hide header/footer element if width is set to 0%:
        updateHeaderFooterVisibility();
        
        // Load publication preview listbox
        loadHeaderFooterPublicationPreviewList();
    }

    private void updateGUI_DocBook()
    {

    }

    private String[] parseColumnWidths(String widths) 
    {
        if ((widths != null) && (widths.length() > 0)) {
            try {
                String[] res = new String[3];
                StringTokenizer st = new StringTokenizer(widths);
                int sum = 0;
                for (int i=0; i < res.length; i++) {
                    String s = st.nextToken();
                    sum += Integer.parseInt(s);   // throw exception if no valid integer number
                    res[i] = s;
                }
                if (sum == 10) {  // transform old values from version before 1.7
                    for (int i=0; i < res.length; i++) {
                        res[i] = Integer.toString(Integer.parseInt(res[i]) * 10);
                    }
                }
                return res;
            } catch (Exception ex) {}
        }
        return new String[] { "", "", "" };
    }

    private void setDefaultCustomHeaderFooter()
    {
        if (headerFooterPageTypes == null) {
            headerFooterPageTypes = new TreeSet();
        } else {
            headerFooterPageTypes.clear();
        }
        if (headerFooterContentMap == null) {
            headerFooterContentMap = new HashMap();
        } else {
            headerFooterContentMap.clear();
        }
        boolean double_sided = pdfDoubleSidedBox.isChecked();
        
        // Set default for odd content pages (if not double-sided this is also used for even pages)
        String pt = "body_odd";
        headerFooterPageTypes.add(pt);
        headerFooterContentMap.put(getHeaderFooterContentKey(pt, "header", 1, 1), "%draft");
        headerFooterContentMap.put(getHeaderFooterContentKey(pt, "header", 2, 1), "%section_title");
        headerFooterContentMap.put(getHeaderFooterContentKey(pt, "header", 3, 1), "%draft");
        if (double_sided) {
            // pagenumber in the outside corner:
            headerFooterContentMap.put(getHeaderFooterContentKey(pt, "footer", 3, 1), "%pagenumber");
            
            // if double-sided, add separate setting for even content pages
            pt = "body_even";
            headerFooterPageTypes.add(pt);
            headerFooterContentMap.put(getHeaderFooterContentKey(pt, "header", 1, 1), "%draft");
            headerFooterContentMap.put(getHeaderFooterContentKey(pt, "header", 2, 1), "%section_title");
            headerFooterContentMap.put(getHeaderFooterContentKey(pt, "header", 3, 1), "%draft");
            // page number in the outside corner:
            headerFooterContentMap.put(getHeaderFooterContentKey(pt, "footer", 1, 1), "%pagenumber");
        } else {
            // pagenumber in the center
            headerFooterContentMap.put(getHeaderFooterContentKey(pt, "footer", 2, 1), "%pagenumber");
        }
        
        // Set default for first content page in sequence
        pt = "body_first";
        headerFooterPageTypes.add(pt);
        headerFooterContentMap.put(getHeaderFooterContentKey(pt, "header", 1, 1), "%draft");
        headerFooterContentMap.put(getHeaderFooterContentKey(pt, "header", 3, 1), "%draft");
        if (double_sided) {  // pagenumber in the outside corner
            headerFooterContentMap.put(getHeaderFooterContentKey(pt, "footer", 3, 1), "%pagenumber");
        } else {  // pagenumber in the center
            headerFooterContentMap.put(getHeaderFooterContentKey(pt, "footer", 2, 1), "%pagenumber");
        }
        
        // Set default for blank content pages
        pt = "body_blank";
        headerFooterPageTypes.add(pt);
        if (double_sided) {  // pagenumber in the outside corner
            headerFooterContentMap.put(getHeaderFooterContentKey(pt, "footer", 1, 1), "%pagenumber");
        } else {  // pagenumber in the center
            headerFooterContentMap.put(getHeaderFooterContentKey(pt, "footer", 2, 1), "%pagenumber");
        }
        
        // Set default for title pages (same for first, odd, even and blank pages)
        pt = "titlepage_odd";
        headerFooterPageTypes.add(pt);
        headerFooterContentMap.put(getHeaderFooterContentKey(pt, "header", 1, 1), "%draft");
        headerFooterContentMap.put(getHeaderFooterContentKey(pt, "header", 3, 1), "%draft");
    }
            
    
    private void loadHeaderFooterPublicationPreviewList()
    {
        headerFooterPublicationPreviewBox.getItems().clear();
        String storeId = docmaSess.getStoreId();
        String lastconf = docmaSess.getUserProperty(GUIConstants.PROP_USER_PREVIEW_PUBCONF + "." + storeId);
        int selidx = 0;
        if (lastconf == null) lastconf = "";
        String[] conf_ids = docmaSess.getPublicationConfigIds();
        for (int i=0; i < conf_ids.length; i++) {
            String cid = conf_ids[i];
            headerFooterPublicationPreviewBox.appendItem(cid, cid);
            if (cid.equals(lastconf)) selidx = i;
        }
        if (conf_ids.length > 0) {
            headerFooterPublicationPreviewBox.setSelectedIndex(selidx);
            // headerFooterPublicationPreviewBox.invalidate();  // workaround for zk bug
        }
    }
    
    private static String getRegionFromBoxId(String box_id)
    {
        return box_id.contains("Header") ? "header" : "footer";
    }
    
    private static int getColumnFromBoxId(String box_id) 
    {
        return box_id.contains("Left") ? 1 : (box_id.contains("Center") ? 2 : 3);
    }
    
    private static int getRowFromBoxId(String box_id) 
    {
        // Allows up to 3 rows
        return box_id.contains("1") ? 1 : (box_id.contains("2") ? 2 : 3);
    }
    
    private String getHeaderFooterContentKey(String pageType, String region, int col, int row)
    {
        return pageType + "_" + region + "_" + col + "_" + row;
    }

    private String getOldHeaderFooterContent(String region, int col, int row)
    {
        String val = oldHeaderFooterContent.get(region + "_" + col + "_" + row);
        return (val == null) ? "" : val;
    }
    
    private void setOldHeaderFooterContent(String region, int col, int row, String content) 
    {
        oldHeaderFooterContent.put(region + "_" + col + "_" + row, (content == null) ? "" : content);
    }

    private String getHeaderFooterContent(String pageType, String region, int col, int row)
    {
        String key = getHeaderFooterContentKey(pageType, region, col, row);
        return (String) headerFooterContentMap.get(key);
    }

    private void resetGUI_HeaderFooterContent()
    {
        selectItemByValue(headerFooterPageClassBox, "body");
        selectItemByValue(headerFooterPageSequenceBox, "odd");
        headerFooterPageSequenceBox.setDisabled(false);
        headerFooterSameAsBodyBox.setChecked(false);
        headerFooterSameAsBodyBox.setDisabled(true);
        headerFooterSameAsBodyBox.setVisible(false);
        headerFooterSameAsOddBox.setChecked(false);
        headerFooterSameAsOddBox.setDisabled(true);
        headerFooterSameAsOddBox.setVisible(false);
        
        setHeaderFooterContentFields("body_odd");
    }

    private void setHeaderFooterContentFields(String pageType)
    {
        oldHeaderFooterContent.clear();
        
        final String[] colNames = { "Left", "Center", "Right"};
        final String[] regionNames = { "Header", "Footer"};
        final String[] regions = { "header", "footer"};
        for (int k = 0; k < regions.length; k++) {
            String reg = regions[k];
            String regName = regionNames[k];
            for (int colNum = 1; colNum <= 3; colNum++) {
                String colName = colNames[colNum-1]; 
                for (int rowNum = 1; rowNum <= MAX_PDF_HEADER_FOOTER_ROWS; rowNum++) {
                    String cont = null;
                    if (pageType != null) {
                        String key = getHeaderFooterContentKey(pageType, reg, colNum, rowNum);
                        cont = (String) headerFooterContentMap.get(key);
                        if (DocmaConstants.DEBUG) {
                            Log.info("Reading header/footer: " + key + "  Value: " + cont);
                        }
                    }
                    setHeaderFooterContent(regName, colName, rowNum, cont);
                }
            }
        }
    }
    
    private void setHeaderFooterContent(String regionName, 
                                        String colName, 
                                        int rowNum, 
                                        String content)
    {
        if (content == null) {
            content = "";
        }
        Combobox combo_box = (Combobox) 
            getFellow("Media" + regionName + "Content" + colName + rowNum + "Combobox");
        combo_box.setDisabled(false);
        combo_box.setValue(content);
    }
    
    private void setHeaderFooterFieldsDisabled(boolean disabled)
    {
        headerWidth1Box.setDisabled(disabled);
        headerWidth2Box.setDisabled(disabled);
        headerWidth3Box.setDisabled(disabled);
        footerWidth1Box.setDisabled(disabled);
        footerWidth2Box.setDisabled(disabled);
        footerWidth3Box.setDisabled(disabled);
        
        headerFooterPageClassBox.setDisabled(disabled);
        headerFooterSameAsBodyBox.setDisabled(disabled);
        headerFooterPageSequenceBox.setDisabled(disabled);
        headerFooterSameAsOddBox.setDisabled(disabled);
        
        setHeaderFooterContentFieldsDisabled(disabled);
    }
    
    private void setHeaderFooterContentFieldsDisabled(boolean disabled)
    {
        // if (disabled) {
        //     setHeaderFooterContentFields(null);  // no content
        // } else {
        //     setHeaderFooterContentFields(getHeaderFooterPageType());
        // }
        final String[] colNames = { "Left", "Center", "Right"};
        final String[] regionNames = { "Header", "Footer"};
        for (String colName : colNames) {
            for (String reg : regionNames) {
                for (int r = 1; r <= MAX_PDF_HEADER_FOOTER_ROWS; r++) {
                    String box_id = "Media" + reg + "Content" + colName + Integer.toString(r) + "Combobox";
                    Combobox combo = (Combobox) getFellow(box_id);
                    combo.setDisabled(disabled);
                }
            }
        }
    }
    
    private String getHeaderFooterPageType()
    {
        Listitem item = headerFooterPageClassBox.getSelectedItem();
        if (item == null) return null;
        String pageClass = item.getValue().toString();
        item = headerFooterPageSequenceBox.getSelectedItem();
        if (item == null) return null;
        String sequence = item.getValue().toString();
        return pageClass + "_" + sequence;
    }
    
    private String getHeaderFooterWidth(Intbox box) 
    {
        String val = null;
        try {
            val = box.getText();
        } catch (Exception ex) {}
        return (val == null) ? "" : val.trim();
    }

    private void updateHeaderFooterVisibility() 
    {
        String[][] val = new String[2][3];
        val[0][0] = getHeaderFooterWidth(headerWidth1Box);
        val[0][1] = getHeaderFooterWidth(headerWidth2Box);
        val[0][2] = getHeaderFooterWidth(headerWidth3Box);
        val[1][0] = getHeaderFooterWidth(footerWidth1Box);
        val[1][1] = getHeaderFooterWidth(footerWidth2Box);
        val[1][2] = getHeaderFooterWidth(footerWidth3Box);

        final String[] regions = {"Header", "Footer"};
        final String[] columns = {"Left", "Center", "Right"};
        for (int reg = 0; reg < regions.length; reg++) {
            String regName = regions[reg];
            String regId = regName.toLowerCase();  // "header"/"footer" instead of "Header"/"Footer"
            for (int col = 0; col < columns.length; col++) {
                String colName = columns[col];
                Component box = (Component) getFellow("Media" + regName + "Content" + colName + "Box");
                boolean hidden = val[reg][col].equals("0");
                box.setVisible(! hidden);
                if (hidden) {
                    for (int r = 1; r <= MAX_PDF_HEADER_FOOTER_ROWS; r++) {
                        String box_id = "Media" + regName + "Content" + colName + Integer.toString(r) + "Combobox";
                        Combobox combo = (Combobox) getFellow(box_id);
                        combo.setValue("");
                        on_HeaderFooterContentChanged(regId, col+1, r, "");
                    }
                }
            }
        }
    }
    

    private void updateModel(DocmaOutputConfig outConf) throws Exception
    {
        String new_id = idBox.getValue().trim();
        outConf.setId(new_id);
        outConf.setFilterSetting(filterBox.getValue());
        outConf.setToc(tocBox.isChecked());
        outConf.setIndex(indexBox.isChecked());
        int maxTocDepth = getListboxInt(maxTocDepthBox, 4);
        if (maxTocDepth < 0) maxTocDepth = 0;
        outConf.setMaxTocDepth(maxTocDepth);
        boolean is_part_toc = partTocBox.isChecked();
        boolean is_chap_toc = chapterTocBox.isChecked();
        boolean is_sect_toc = sectionTocBox.isChecked();
        outConf.setPartToc(is_part_toc);
        outConf.setChapterToc(is_chap_toc);
        outConf.setSectionToc(is_sect_toc);
        int sectTocLevels = getListboxInt(sectionTocLevelsBox, 2);
        if (sectTocLevels < 1) sectTocLevels = 2;  // default is 2 levels
        outConf.setSectionTocLevels(sectTocLevels);
        int sectionTocDepth = getListboxInt(sectionTocDepthBox, 0);
        boolean is_component_toc = is_part_toc || is_chap_toc || is_sect_toc;
        if ((sectionTocDepth < 0) || !is_component_toc) sectionTocDepth = 0;
        outConf.setSectionTocDepth(sectionTocDepth);
        
        String toc_indent = tocIndentBox.getValue().trim();
        if (! toc_indent.equals("")) {
            toc_indent += "pt";
        }
        outConf.setTocIndentWidth(toc_indent);
        
        String toc_named = tocNamedPartBox.isChecked() ? "part" : "";
        if (tocNamedChapterBox.isChecked()) {
            toc_named += (toc_named.equals("") ? "" : ",") + "chapter,appendix"; 
        }
        outConf.setTocNamedLabels(toc_named);
        
        outConf.setStyleVariant(styleVariantBox.getSelectedItem().getValue().toString());
        outConf.setParaSpace(getDim(paraSpaceBox, paraSpaceUnitBox));
        outConf.setParaIndent(getDim(paraIndentBox, paraIndentUnitBox));
        outConf.setItemSpace(getDim(listItemSpaceBox, listItemSpaceUnitBox));
        outConf.setListIndent(getDim(listIndentBox, listIndentUnitBox));
        outConf.setOrderedListLabelWidth(getDim(listLabelWidthBox, listLabelWidthUnitBox));
        outConf.setTitlePlacement(titlePlacementBox.getSelectedItem().getValue().toString());
        outConf.setRender1stLevel(render1stLevelBox.getSelectedItem().getValue().toString());
        if (partNumberBox.isChecked()) {
            outConf.setPartNumbering(partNumberFormatBox.getSelectedItem().getValue().toString());
        } else {
            outConf.setPartNumbering("0");
        }
        if (chapterNumberBox.isChecked()) {
            outConf.setChapterNumbering(chapterNumberFormatBox.getSelectedItem().getValue().toString());
        } else {
            outConf.setChapterNumbering("0");
        }
        if (sectionNumberBox.isChecked()) {
            outConf.setSectionNumbering(sectionNumberFormatBox.getSelectedItem().getValue().toString());
        } else {
            outConf.setSectionNumbering("0");
        }
        if (appendixNumberBox.isChecked()) {
            outConf.setAppendixNumbering(appendixNumberFormatBox.getSelectedItem().getValue().toString());
        } else {
            outConf.setAppendixNumbering("0");
        }
        outConf.setFootnoteNumbering(footnoteNumberFormatBox.getSelectedItem().getValue().toString());
        int numdepth = numberingDepthBox.getValue().intValue();
        if (numdepth < 0) numdepth = 0;
        outConf.setNumberingDepth(numdepth);
        outConf.setOmitSingleTitle(omitSingleSectionBox.isChecked());
        outConf.setExclude1stLevelNumber(exclude1stLevelNumberBox.isChecked());
        outConf.setRestartPart(restartPartBox.isChecked());
        String format_gui = getCurrentSelectedGUIFormat();
        String mainformat = getMainFormat(format_gui);
        String subformat = getSubFormat(format_gui);
        outConf.setFormat(mainformat);
        outConf.setSubformat(subformat);
        if (outConf.getFormat().equals("html")) {
            outConf.setHtmlSingleFile(htmlSingleFileRadio.isSelected());
            // outConf.setHtmlFilePrefix(htmlFilePrefixBox.getValue());
            if (htmlSeparateFileBox.isChecked()) {
                String level = htmlSeparateFileLevelBox.getSelectedItem().getValue().toString();
                outConf.setHtmlSeparateFileLevel(Integer.parseInt(level));
            } else {
                outConf.setHtmlSeparateFileLevel(0);
            }
            // if (htmlCreateDirBox.isChecked()) {
            //     String level = htmlDirLevelBox.getSelectedItem().getValue().toString();
            //     outConf.setHtmlDirLevel(Integer.parseInt(level));
            // } else {
            //     outConf.setHtmlDirLevel(0);
            // }
            outConf.setHtmlAliasFilename(htmlAliasFilenameBox.isChecked());
            outConf.setHtmlInclude1stSection(htmlInclude1stSectBox.isChecked());
            outConf.setHtmlSeparateTOC(htmlSeparateTOCBox.isChecked());
            // outConf.setHtmlSeparateEachTable(htmlSeparateEachTableBox.isChecked());
            outConf.setHtmlNavigationalIcons(htmlNavigationIconsBox.isChecked());
            outConf.setHtmlNavigationalTitles(htmlNavigationTitlesBox.isChecked());
            outConf.setHtmlBreadcrumbs(htmlBreadcrumbsBox.isChecked());
            Listitem bread_item = htmlBreadcrumbStartBox.getSelectedItem();
            outConf.setHtmlBreadcrumbStart((bread_item != null) ? bread_item.getValue().toString() : "");
            outConf.setHtmlBreadcrumbSeparator(htmlBreadcrumbSeparatorBox.getValue());
            outConf.setHtmlRootFolder(htmlRootFolderBox.getValue().trim().replace('\\', '/'));
            outConf.setHtmlURLTargetWindow(htmlURLTargetWindowBox.getValue().trim());
            outConf.setHtmlCustomHeaderAlias(htmlCustomHeaderBox.getValue().trim());
            outConf.setHtmlCustomFooterAlias(htmlCustomFooterBox.getValue().trim());
            // outConf.setHtmlCoverImageAlias(htmlCoverImageBox.getValue().trim());
            Listitem cssitem = htmlCustomCSSBox.getSelectedItem();
            Listitem jsitem = htmlCustomJSBox.getSelectedItem();
            Listitem metaitem = htmlCustomMetaBox.getSelectedItem();
            Listitem encodingitem = htmlOutputEncodingBox.getSelectedItem();
            Listitem webconfigitem = htmlWebhelpConfigBox.getSelectedItem();
            Listitem webhead1item = htmlWebhelpHeader1Box.getSelectedItem();
            Listitem webhead2item = htmlWebhelpHeader2Box.getSelectedItem();
            String cssfn = (cssitem != null) ? cssitem.getValue().toString() : "";
            String jsfn = (jsitem != null) ? jsitem.getValue().toString() : "";
            String metafn = (metaitem != null) ? metaitem.getValue().toString() : "";
            String outenc = (encodingitem != null) ? encodingitem.getValue().toString() : "";
            boolean hasCustomDocType = (! htmlDocTypeCheckBox.isDisabled()) && 
                                       htmlDocTypeCheckBox.isChecked();
            String docType = hasCustomDocType ? htmlDocTypeTextBox.getValue().trim() : "";
            String webconfigfolder = (webconfigitem != null) ? webconfigitem.getValue().toString() : "";
            outConf.setHtmlCustomCSSFilename(cssfn);
            outConf.setHtmlCustomJSFilename(jsfn);
            outConf.setHtmlCustomMetaFilename(metafn);
            outConf.setHtmlCustomFiles(htmlCustomFilesBox.getValue());
            outConf.setHtmlOutputEncoding(outenc);
            outConf.setHtmlCustomDocType(docType);
            outConf.setHtmlWebhelpConfigFolder(webconfigfolder);
            outConf.setHtmlWebhelpHeader1((webhead1item != null) ? webhead1item.getValue().toString() : "");
            outConf.setHtmlWebhelpHeader2((webhead2item != null) ? webhead2item.getValue().toString() : "");
        } else
        if (outConf.getFormat().equals("pdf")) {
            outConf.setPdfPaperSize(pdfPaperSizeBox.getSelectedItem().getValue().toString());
            boolean customPaperSize = outConf.getPdfPaperSize().equals("custom");
            if (customPaperSize) {
                outConf.setPdfPageWidth(getDim(pdfPageWidthBox, pdfPageWidthUnitBox));
                outConf.setPdfPageHeight(getDim(pdfPageHeightBox, pdfPageHeightUnitBox));
            } else {
                outConf.setPdfPageWidth("");
                outConf.setPdfPageHeight("");
            }
            String orient = pdfPagePortraitRadio.isSelected() ? "portrait" : "landscape";
            outConf.setPdfPageOrientation(orient);
            outConf.setPdfPageTop(getDim(pdfPageTopBox, pdfPageTopUnitBox));
            outConf.setPdfPageBottom(getDim(pdfPageBottomBox, pdfPageBottomUnitBox));
            outConf.setPdfPageInner(getDim(pdfPageInnerBox, pdfPageInnerUnitBox));
            outConf.setPdfPageOuter(getDim(pdfPageOuterBox, pdfPageOuterUnitBox));
            outConf.setPdfBodyTop(getDim(pdfBodyTopBox, pdfBodyTopUnitBox));
            outConf.setPdfBodyBottom(getDim(pdfBodyBottomBox, pdfBodyBottomUnitBox));
            outConf.setPdfBodyStart(getDim(pdfBodyStartBox, pdfBodyStartUnitBox));
            outConf.setPdfBodyEnd(getDim(pdfBodyEndBox, pdfBodyEndUnitBox));
            outConf.setPdfHeaderHeight(getDim(pdfHeaderHeightBox, pdfHeaderHeightUnitBox));
            outConf.setPdfFooterHeight(getDim(pdfFooterHeightBox, pdfFooterHeightUnitBox));
            outConf.setPdfDoubleSided(pdfDoubleSidedBox.isChecked());
            outConf.setPdfBookmarks(pdfBookmarksBox.isChecked());
            outConf.setPdfNumbersInRefs(pdfNumbersInRefsBox.isChecked());
            outConf.setPdfExportReferencedFiles(pdfExportFilesBox.isChecked());
            outConf.setPdfFitImages(pdfFitImagesBox.isChecked());
            String showExtHRef = "no";
            if (pdfShowExternalHRefBox.isChecked()) {
                showExtHRef = pdfShowExternalHRefTypeBox.getSelectedItem().getValue().toString();
            }
            outConf.setPdfShowExternalHRef(showExtHRef);
            outConf.setPdfSourceResolution(pdfSourceResolutionBox.getValue().intValue());
            outConf.setPdfTargetResolution(pdfTargetResolutionBox.getValue().intValue());
            String cover_mode = pdfCoverModeBox.getSelectedItem().getValue().toString();
            if (cover_mode.equals("extrapage") && pdfCoverBlankPageBox.isChecked()) {
                cover_mode = "extrapage_blank";
            }
            outConf.setPdfCoverMode(cover_mode);
            outConf.setPdfCoverHorizontalPosition(getDim(pdfCoverXBox, pdfCoverXUnitBox));
            outConf.setPdfCoverVerticalPosition(getDim(pdfCoverYBox, pdfCoverYUnitBox));
            outConf.setPdfIndexColumnCount(getListboxInt(pdfIndexColumnCountBox, 2));
            outConf.setPdfIndexColumnGap(getDim(pdfIndexColumnGapBox, pdfIndexColumnGapUnitBox));
            outConf.setPdfMarkerSectionLevel(getListboxInt(markerSectionLevelBox, 2));
            outConf.setPdfCustomHeaderFooter(customHeaderFooterBox.isChecked());
            String hw1 = getHeaderFooterWidth(headerWidth1Box);
            String hw2 = getHeaderFooterWidth(headerWidth2Box);
            String hw3 = getHeaderFooterWidth(headerWidth3Box);
            if (hw1.equals("") || hw2.equals("") || hw3.equals("")) {
                outConf.setPdfHeaderWidths(null);  // use default widths
            } else {
                outConf.setPdfHeaderWidths(hw1 + " " + hw2 + " " + hw3);
            }
            String fw1 = getHeaderFooterWidth(footerWidth1Box);
            String fw2 = getHeaderFooterWidth(footerWidth2Box);
            String fw3 = getHeaderFooterWidth(footerWidth3Box);
            if (fw1.equals("") || fw2.equals("") || fw3.equals("")) {
                outConf.setPdfFooterWidths(null);  // use default widths
            } else {
                outConf.setPdfFooterWidths(fw1 + " " + fw2 + " " + fw3);
            }
            String[] ptypes =  (headerFooterPageTypes == null) ?  
                new String[0] : (String[]) headerFooterPageTypes.toArray(new String[headerFooterPageTypes.size()]);
            outConf.setPdfCustomHeaderFooterPageTypes(ptypes);
            for (String pageType : ptypes) {
                for (int col = 1; col <= 3; col++) {
                    for (int row = 1; row <= MAX_PDF_HEADER_FOOTER_ROWS; row++) {
                        String head_val = (String)
                                headerFooterContentMap.get(getHeaderFooterContentKey(pageType, "header", col, row));
                        String foot_val = (String)
                                headerFooterContentMap.get(getHeaderFooterContentKey(pageType, "footer", col, row));
                        outConf.setPdfHeaderFooterContent(pageType, "header", col, row, head_val);
                        outConf.setPdfHeaderFooterContent(pageType, "footer", col, row, foot_val);
                    }
                }
            }
        } else
        if (outConf.getFormat().equals("docbook")) {
        }
    }

    private String getCurrentSelectedGUIFormat()
    {
        return formatBox.getSelectedItem().getValue().toString();
    }
    
    private String getMainFormat(String format_gui) 
    {
        boolean is_webhelp = format_gui.startsWith("webhelp");
        boolean is_epub = format_gui.equals("epub");
        return (is_webhelp || is_epub) ? "html" : format_gui;
    }

    private String getSubFormat(String format_gui) 
    {
        boolean is_webhelp = format_gui.startsWith("webhelp");
        boolean is_epub = format_gui.startsWith("epub");
        return (is_webhelp || is_epub) ? format_gui : "";
    }

    private boolean selectItemByValue(Listbox box, String value, int defaultSelIndex)
    {
        boolean selected = selectItemByValue(box, value);
        if (! selected) {
            box.setSelectedIndex(defaultSelIndex);
        }
        return selected;
    }
    
    private boolean selectItemByValue(Listbox box, String value, String defaultValue)
    {
        boolean selected = selectItemByValue(box, value);
        if (! selected) {
            selected = selectItemByValue(box, defaultValue);
        }
        return selected;
    }

    private boolean selectItemByValue(Listbox box, String value)
    {
        for (int i=0; i < box.getItemCount(); i++) {
            Listitem item = box.getItemAtIndex(i);
            if (item.getValue().equals(value)) {
                box.setSelectedIndex(i);
                return true;
            }
        }
        return false;
    }
    
    private int getListboxInt(Listbox box, int default_value)
    {
        Listitem item = box.getSelectedItem();
        if (item == null) return default_value;
        try {
            return Integer.parseInt("" + item.getValue());
        } catch (Exception ex) {
            return default_value;
        }
    }

    private void setNonNegativeInt(Textbox numBox, String value, String default_value)
    {
        try {
            int num = Integer.parseInt(value);
            if (num >= 0) {
                numBox.setValue(value);
                return;
            }
        } catch (Exception ex) {}  // set default
        numBox.setValue(default_value);
    }
    
    private void setDim(Textbox numBox, Listbox unitBox, String value)
    {
        int pos = value.length() - 2;
        if (pos >= 0) {
            selectItemByValue(unitBox, value.substring(pos));
            numBox.setValue(value.substring(0, pos));
        } else {
            unitBox.setSelectedIndex(0);
            numBox.setValue("");
        }
    }

    private String getDim(Textbox numBox, Listbox unitBox)
    {
        String num = numBox.getValue().trim();
        if (num.equals("")) {
            return "";
        } else {
            return num + unitBox.getSelectedItem().getValue();
        }
    }

}
