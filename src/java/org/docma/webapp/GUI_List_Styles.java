/*
 * GUI_List_Styles.java
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
import org.docma.coreapi.*;
import java.util.*;
import org.zkoss.zul.*;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Execution;
import org.zkoss.util.media.Media;

/**
 *
 * @author MP
 */
public class GUI_List_Styles
{
    // Constants used as values for type filter listbox 
    private static final String FILTER_TYPE_INLINE = DocmaStyle.INLINE_STYLE;
    private static final String FILTER_TYPE_BLOCK = DocmaStyle.BLOCK_STYLE;
    
    // Constants used as values for variant filter listbox
    private static final String VARIANTS_ALL = "::all";
    private static final String VARIANTS_NONE = "::none";

    private final MainWindow mainWin;
    private final Listbox typefilter_listbox;
    private final Listbox variantfilter_listbox;
    private final Hbox variantfilter_area;
    private final Listbox styles_listbox;
    private final ListModelList styles_listmodel;
    private String currentTypeFilter = null;     // the selected type filter value
    private String currentVariantFilter = null;  // the selected variant filter value
    private SortedSet<String> currentVariants = new TreeSet<String>();

    // Current style type filter mode
    boolean showInline;
    boolean showBlock;
    
    // Current style variant filter mode
    private boolean all_styles = true; // filter setting to show all styles (base styles and all variants);
                                       // if no variants exist, this is the default
    private boolean no_variants = false; // filter setting to show only base styles
    private boolean single_variant = false;  // filter setting to show only selected variant

    public GUI_List_Styles(MainWindow mainWin)
    {
        this.mainWin = mainWin;
        typefilter_listbox = (Listbox) mainWin.getFellow("StylesTypeFilterList");
        selectItemByValue(typefilter_listbox, FILTER_TYPE_INLINE);  // default 
        variantfilter_area = (Hbox) mainWin.getFellow("StylesVariantFilterArea");
        variantfilter_listbox = (Listbox) mainWin.getFellow("StylesVariantFilterList");
        
        styles_listbox = (Listbox) mainWin.getFellow("StylesListbox");
        styles_listmodel = new ListModelList();
        styles_listmodel.setMultiple(true);
        styles_listbox.setModel(styles_listmodel);
        styles_listbox.setItemRenderer(mainWin.getListitemRenderer());
        
        loadStyles();
    }

    private void loadStyles()
    {
        styles_listmodel.clear();
        
        // Get current filter settings
        currentTypeFilter = typefilter_listbox.getSelectedItem().getValue().toString();
        showInline = currentTypeFilter.equals(FILTER_TYPE_INLINE);
        showBlock = currentTypeFilter.equals(FILTER_TYPE_BLOCK);
        currentVariantFilter = null;
        all_styles = true; // filter setting to show all styles (base styles and all variants);
                                   // if no variants exist, this is the default
        no_variants = false; // filter setting to show only base styles
        single_variant = false;  // filter setting to show selected variant
        if (variantfilter_area.isVisible() && (variantfilter_listbox.getSelectedIndex() >= 0)) {
            currentVariantFilter = variantfilter_listbox.getSelectedItem().getValue().toString();
            all_styles = currentVariantFilter.equals(VARIANTS_ALL);
            no_variants = currentVariantFilter.equals(VARIANTS_NONE);
            single_variant = !(all_styles || no_variants);
        }

        // Add styles to listbox based on current filter settings
        DocmaSession docmaSess = mainWin.getDocmaSession();
        DocmaStyle[] styles = single_variant ? docmaSess.getStyles(currentVariantFilter) : 
                                               docmaSess.getStyles();
        for (DocmaStyle s : styles) {
            if (s.isVariant() && no_variants) {
                continue;
            }
            if (s.isInlineStyle()) {
                if (showInline) styles_listmodel.add(s);
            } else {
                if (showBlock) styles_listmodel.add(s);
            }
        }
        styles_listbox.setRows(styles_listmodel.getSize());
        
        updateVariantFilterListbox();
    }
    
    private void updateVariantFilterListbox() 
    {
        DocmaSession docmaSess = mainWin.getDocmaSession();

        // Update variant filter listbox 
        SortedSet<String> variants = new TreeSet<String>(Arrays.asList(docmaSess.getStyleVariantIds()));
        boolean has_variants = (variants.size() > 0);
        variantfilter_area.setVisible(has_variants);
        if (! variants.equals(currentVariants)) {  // variants have changed
            // rebuild variant list
            variantfilter_listbox.getItems().clear();
            if (has_variants) {
                DocI18n i18n = mainWin.getI18n();
                String txt_variants_all = i18n.getLabel("text.styles.show_base_and_variants");
                String txt_variants_none = i18n.getLabel("text.styles.show_only_base");
                variantfilter_listbox.appendItem(txt_variants_all, VARIANTS_ALL);
                variantfilter_listbox.appendItem(txt_variants_none, VARIANTS_NONE);
                for (String variant_id : variants) {
                    variantfilter_listbox.appendItem(variant_id, variant_id);
                }
                if (! selectItemByValue(variantfilter_listbox, currentVariantFilter)) {  // try to reselect previous selection
                    variantfilter_listbox.setSelectedIndex(0);  // show all styles by default
                }
            }
            currentVariants = variants;
        }
    }
    
    public void onChangeStylesFilter()
    {
        loadStyles();
    }

    public void doNewStyle() throws Exception
    {
        newStyle(currentTypeFilter);
    }

    public void doCopyStyle() throws Exception
    {
        DocI18n i18n = mainWin.getI18n();
        int selcnt = styles_listbox.getSelectedCount();
        if (selcnt <= 0) {
            Messagebox.show(i18n.getLabel("text.styles.none_selected"));
            return;
        }
        if (selcnt > 1) {
            Messagebox.show(i18n.getLabel("text.styles.select_single"));
            return;
        }
        DocmaSession docmaSess = mainWin.getDocmaSession();
        int sel_idx = styles_listbox.getSelectedIndex();
        DocmaStyle docstyle = (DocmaStyle) styles_listmodel.getElementAt(sel_idx);
        
        // Create instance that is a copy of the style:
        DocmaStyle style_copy = docmaSess.getStyleVariant(docstyle.getBaseId(), docstyle.getVariantId());
        style_copy.setId("");  // user has to enter a new ID
        
        newStyle(docmaSess, style_copy);
    }

    public void doEditStyle() throws Exception
    {
        DocmaSession docmaSess = mainWin.getDocmaSession();
        if (! docmaSess.hasRight(AccessRights.RIGHT_EDIT_STYLES)) {
            return;  // Editing of styles is not allowed. Disable editing by double click on style.
        }

        DocI18n i18n = mainWin.getI18n();
        if (styles_listbox.getSelectedCount() <= 0) {
            Messagebox.show(i18n.getLabel("text.styles.none_selected"));
            return;
        }
        int sel_idx = styles_listbox.getSelectedIndex();
        DocmaStyle docstyle = (DocmaStyle) styles_listmodel.getElementAt(sel_idx);
        StyleDialog dialog = getStyleDialog();
        dialog.setMode_EditStyle();
        if (dialog.doEditStyle(docstyle, mainWin)) {
            try {
                docmaSess.saveStyle(docstyle);
            } catch (DocException dex) {
                Messagebox.show("Error: " + dex.getMessage());
            }
            styles_listmodel.set(sel_idx, docstyle);
        }
    }

    public void doDeleteStyles() throws Exception
    {
        DocmaSession docmaSess = mainWin.getDocmaSession();
        DocI18n i18n = mainWin.getI18n();
        int sel_cnt = styles_listbox.getSelectedCount();
        if (sel_cnt <= 0) {
            Messagebox.show(i18n.getLabel("text.styles.none_selected"));
            return;
        }
        try {
            Iterator it = styles_listbox.getSelectedItems().iterator();
            DocmaStyle[] sel_styles = new DocmaStyle[sel_cnt];
            for (int i=0; i < sel_cnt; i++) {
                Listitem item = (Listitem) it.next();
                sel_styles[i] = (DocmaStyle) styles_listmodel.getElementAt(item.getIndex());
            }
            String msg;
            if (sel_cnt == 1) {
                msg = i18n.getLabel("text.styles.confirm_delete_style", sel_styles[0].getId());
            } else {
                msg = i18n.getLabel("text.styles.confirm_delete_count", sel_cnt);
            }
            if (Messagebox.show(msg, i18n.getLabel("text.styles.confirm_delete_title"),
                Messagebox.YES | Messagebox.NO, Messagebox.QUESTION) == Messagebox.YES) {

                for (int i=0; i < sel_cnt; i++) {
                    DocmaStyle docstyle = sel_styles[i];
                    docmaSess.deleteStyle(docstyle.getId());
                }
                // styles_listmodel.removeAll(Arrays.asList(sel_styles));
                // if (styles_listmodel.getSize() >= MainWindow.MIN_LISTBOX_SIZE) {
                //     styles_listbox.setRows(styles_listmodel.getSize());
                // }
                loadStyles(); // reload all styles, update variant filter listbox
            }
        } catch (DocException dex) {
            Messagebox.show("Error: " + dex.getMessage());
            loadStyles(); // reload all styles
        }
    }

    public void doImportStyle() throws Exception
    {
        doImportStyle(currentTypeFilter.equals(FILTER_TYPE_INLINE));
    }

    public void doExportStyle() throws Exception
    {
        String filename = 
          currentTypeFilter.equals(FILTER_TYPE_INLINE) ? "inlinestyles.css" : "blockstyles.css";
        doExportStyle(filename);
    }

    public void changeStyleHiddenState(String styleId) throws Exception
    {
        DocmaStyle docstyle = getStyleFromListModelList(styleId);
        if (docstyle == null) {
            Messagebox.show("Error: Style with ID '" + styleId + "' not found in list!");
            return;
        }
        DocmaSession docmaSess = mainWin.getDocmaSession();
        docstyle.setHidden(! docstyle.isHidden());
        try {
            docmaSess.saveStyle(docstyle);
        } catch (DocException dex) {
            Messagebox.show("Error: " + dex.getMessage());
        }
    }

    public void doNewVariantStyle() throws Exception
    {
        DocI18n i18n = mainWin.getI18n();
        if (styles_listbox.getSelectedCount() <= 0) {
            Messagebox.show(i18n.getLabel("text.styles.none_selected"));
            return;
        }
        int sel_idx = styles_listbox.getSelectedIndex();
        DocmaStyle docstyle = (DocmaStyle) styles_listmodel.getElementAt(sel_idx);
        DocmaStyle variantstyle = (DocmaStyle) docstyle.clone();
        if (single_variant) {  // preset the variant field to the selected variant filter 
            variantstyle.setId(docstyle.getBaseId() + DocmaStyle.VARIANT_DELIMITER + currentVariantFilter);
        }
        DocmaSession docmaSess = mainWin.getDocmaSession();
        CreateStyleVariantDialog dialog = (CreateStyleVariantDialog) mainWin.getPage().getFellow("CreateStyleVariantDialog");
        if (dialog.doEdit(variantstyle, docmaSess)) {
            try {
                docmaSess.saveStyle(variantstyle);
                String var_id = variantstyle.getVariantId();
                if (no_variants || (single_variant && !var_id.equals(currentVariantFilter))) {  
                    // if created variant would not be visible with current filter setting,
                    // then change filter setting to show all styles
                    selectItemByValue(variantfilter_listbox, VARIANTS_ALL);
                    loadStyles();
                } else {
                    // if created variant is visible with current filter setting,
                    // then insert new variant at correct list position (list is sorted by id)
                    int ins_pos = -(Collections.binarySearch(styles_listmodel, variantstyle)) - 1;
                    styles_listmodel.add(ins_pos, variantstyle);
                    updateVariantFilterListbox();
                }
                styles_listbox.setRows(styles_listmodel.getSize());
            } catch (DocException dex) {
                Messagebox.show("Error: " + dex.getMessage());
            }
        }
    }

    /* --------------  Private methods  --------------- */

    private DocmaStyle getStyleFromListModelList(String styleId)
    {
        for (int i=0; i < styles_listmodel.size(); i++) {
            DocmaStyle docstyle = (DocmaStyle) styles_listmodel.getElementAt(i);
            if (styleId.equals(docstyle.getId())) return docstyle;
        }
        return null;
    }

    private StyleDialog getStyleDialog() throws Exception
    {
        Page pg = mainWin.getPage();
        if (pg.hasFellow("StyleDialog")) {
            return (StyleDialog) pg.getFellow("StyleDialog");
        } else {
            Execution exec = mainWin.getDesktop().getExecution();
            StyleDialog dialog = (StyleDialog) exec.createComponents("StyleDialog.zul", null)[0];
            dialog.setPage(pg);
            return dialog;
        }
    }

    private void newStyle(String style_type) throws Exception
    {
        DocmaSession docmaSess = mainWin.getDocmaSession();
        DocmaStyle docstyle = new DocmaStyle("", style_type, "", "");
        newStyle(docmaSess, docstyle);
    }

    private void newStyle(DocmaSession docmaSess, DocmaStyle docstyle) throws Exception
    {
        StyleDialog dialog = getStyleDialog();
        dialog.setMode_NewStyle();
        if (dialog.doEditStyle(docstyle, mainWin)) {
            try {
                docmaSess.saveStyle(docstyle);
                int ins_pos = -(Collections.binarySearch(styles_listmodel, docstyle)) - 1;
                styles_listmodel.add(ins_pos, docstyle);
                styles_listbox.setRows(styles_listmodel.getSize());
            } catch (DocException dex) {
                Messagebox.show("Error: " + dex.getMessage());
            }
        }
    }

    private void doImportStyle(boolean is_inline) throws Exception
    {
        DocI18n i18n = mainWin.getI18n();
        String txt_select_file = i18n.getLabel("text.styles.upload_css_select_file");
        String txt_upload_title = i18n.getLabel("text.styles.upload_css_title");
        Media media = Fileupload.get(txt_select_file, txt_upload_title, true);
        if (media == null) return;  // user canceled the upload
        byte[] data = media.getByteData();
        String css = new String(data, "UTF-8");
        DocmaSession docmaSess = mainWin.getDocmaSession();
        DocmaStyle[] styles;
        try {
            styles = DocmaStyle.parseCSS(css, is_inline);
        } catch (Exception ex) {
            Messagebox.show(i18n.getLabel("text.styles.upload_css_invalid", ex.getMessage()));
            return;
        }
        try {
            for (DocmaStyle new_style : styles) {
                DocmaStyle old_style = docmaSess.getStyle(new_style.getId());
                if (old_style != null) {
                    if (is_inline && old_style.isBlockStyle()) {
                        String msg = i18n.getLabel("text.styles.import_failed_blockstyle_exists", old_style.getId());
                        Messagebox.show(msg);
                        continue;
                    }
                    if (!is_inline && old_style.isInlineStyle()) {
                        String msg = i18n.getLabel("text.styles.import_failed_inlinestyle_exists", old_style.getId());
                        Messagebox.show(msg);
                        continue;
                    }
                    String msg = i18n.getLabel("text.styles.import_overwrite_msg", old_style.getId());
                    String title = i18n.getLabel("text.styles.import_overwrite_title");
                    if (Messagebox.show(msg, title,
                            Messagebox.YES | Messagebox.NO, Messagebox.QUESTION) == Messagebox.NO) {
                        continue;
                    }
                }
                docmaSess.saveStyle(new_style);
            }
        } catch (Exception ex) {
            Messagebox.show(i18n.getLabel("text.styles.import_error", ex.getMessage()));
        }
        loadStyles();  // reload styles
    }

    private void doExportStyle(String filename) throws Exception
    {
        DocI18n i18n = mainWin.getI18n();
        if (styles_listbox.getSelectedCount() <= 0) {
            Messagebox.show(i18n.getLabel("text.styles.select_one_or_more"));
            return;
        }
        Set sel_items = styles_listbox.getSelectedItems();
        DocmaStyle[] styles = new DocmaStyle[sel_items.size()];
        Iterator it = sel_items.iterator();
        for (int i=0; i < styles.length; i++) {
            Listitem item = (Listitem) it.next();
            styles[i] = (DocmaStyle) styles_listmodel.getElementAt(item.getIndex());
        }
        String css = DocmaStyle.getCSS(styles, true);
        Filedownload.save(css, "application/octet-stream", filename);
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

}
