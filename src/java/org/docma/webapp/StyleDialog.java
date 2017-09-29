/*
 * StyleDialog.java
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
import org.docma.app.*;
import org.docma.plugin.AutoFormatCall;
import org.docma.util.CSSParser;
import org.docma.util.Log;

import org.zkoss.zul.*;
// import org.zkoss.zkex.zul.Colorbox;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.Clients;

/**
 *
 * @author MP
 */
public class StyleDialog extends Window 
{
    private static final int MODE_NEW = 0;
    private static final int MODE_EDIT = 1;

    private int modalResult = -1;
    private int mode = -1;
    private String cssText = "";

    private MainWindow mainWin;
    private DocmaSession docmaSess;
    private DocmaStyle docStyle;
    private boolean initialized = false;
    private Textbox id_box;
    // private Label idnote_label;
    private Textbox name_box;
    // private Div css_div;
    private Hbox formal_box;
    private Checkbox formal_checkbox;
    private Textbox labelid_box;
    private Grid cssprops_grid;
    private Label autoformat_label;
    private Hbox autoformat_area;
    private Listbox autoformat_box;
    private ListModelList autoformat_listmodel;
    private boolean autoformat_changed;
    private Button af_add_btn;
    private Button af_edit_btn;
    private Button af_remove_btn;
    private Button af_up_btn;
    private Button af_down_btn;


    /* --------------  Public methods  ---------------------- */

    // public void onEvent(Event evt) throws Exception
    // {
        // Messagebox.show("Event: " + evt.getName() + " Data: " + evt.getData());
        // onCancelClick();
    // }

    public void onOkayClick(Event evt) throws Exception
    {
        // if (evt instanceof ForwardEvent) {
        //     evt = ((ForwardEvent) evt).getOrigin();
        // }
        cssText = getGridCSS();  // getDataString(evt);
        if (! hasInvalidInputs()) {
            modalResult = GUIConstants.MODAL_OKAY;
            setVisible(false);
        }
    }

    public void onCancelClick()
    {
        modalResult = GUIConstants.MODAL_CANCEL;
        setVisible(false);
    }

    public void onCSSUpdatedByEditor(Event evt) throws Exception
    {
        if (evt instanceof ForwardEvent) {
            evt = ((ForwardEvent) evt).getOrigin();
        }
        cssText = getDataString(evt);
        if (DocmaConstants.DEBUG) {
            Log.info("onCSSUpdatedByEditor: " + cssText);
        }
        setCssPropsGridContent(cssText);
    }
    
    public void onCSSNameChange(Event evt)
    {
        if (evt instanceof ForwardEvent) {
            evt = ((ForwardEvent) evt).getOrigin();
        }
        Component target = evt.getTarget();
        if (target instanceof Combobox) {
            Combobox namebox = (Combobox) target;
            String pname = namebox.getValue().trim();
            if (pname.length() > 1) {  // at least 2 characters 
                Row r = (Row) namebox.getParent();
                r.getChildren().clear();
                buildCSSEditRow(r, pname, "");
            }
        }
    }
    
    public void onCSSValueChange()
    {
        cssText = getGridCSS();
        String cssEscaped = cssText.replace("'", "\\'");
        if (DocmaConstants.DEBUG) {
            Log.info("onCSSValueChange: " + cssEscaped);
        }
        Clients.evalJavaScript("updateStylePreview('" + cssEscaped + "');");
        
        checkCssNameSelectRow();
    }

    public void onDeleteCSSProperty(Event evt) throws Exception 
    {
        String propname = getDataString(evt);
        if (propname.equals("") && (evt instanceof ForwardEvent)) {
            evt = ((ForwardEvent) evt).getOrigin();
            propname = getDataString(evt);
        }
        if (DocmaConstants.DEBUG) {
            Log.info("onDeleteCSSProperty: " + propname);
        }
        if (propname.length() > 0) {
            Row row = getCssRow(propname);
            if (row != null) { 
                row.detach();
            }
        }
        onCSSValueChange();  // update cssText field and preview
    }
    
    public int getModalResult()
    {
        return modalResult;
    }

    public void setMode_NewStyle()
    {
        init();
        mode = MODE_NEW;
        setTitle(GUIUtil.i18(this).getLabel("label.style.dialog.new.title"));
        id_box.setDisabled(false);
    }

    public void setMode_EditStyle()
    {
        init();
        mode = MODE_EDIT;
        setTitle(GUIUtil.i18(this).getLabel("label.style.dialog.edit.title"));
        id_box.setDisabled(true);
    }

    public boolean doEditStyle(DocmaStyle docmaStyle, MainWindow mainWin)
    throws Exception
    {
        this.mainWin = mainWin;
        this.docmaSess = mainWin.getDocmaSession();
        this.docStyle = docmaStyle;

        init();
        // Button btn = (Button) getFellow("StyleDialogOkayBtn");
        // btn.addEventListener("onOkayClick", this);
        if (DocmaConstants.DEBUG) {
            Log.info("Style CSS of " + docmaStyle.getId() + " before edit: " + docmaStyle.getCSS());
        }
        updateGUI(docmaStyle); // init dialog fields
        do {
            modalResult = -1;
            doModal();
            if (getModalResult() != GUIConstants.MODAL_OKAY) {
                return false;
            }
            // if (hasInvalidInputs()) {
            //     continue;
            // }
            updateModel(docmaStyle);
            if (DocmaConstants.DEBUG) {
                Log.info("Style CSS of " + docmaStyle.getId() + " after edit: " + docmaStyle.getCSS());
            }
            return true;
        } while (true);
    }

    public void onChangeFormal() throws Exception
    {
        labelid_box.setDisabled(! formal_checkbox.isChecked());
        autoformat_changed = true;
    }

    public void onChangeLabelID() throws Exception
    {
        autoformat_changed = true;
    }

    public void onAddAutoFormat() throws Exception
    {
        AutoFormatCallDialog dialog = (AutoFormatCallDialog) getPage().getFellow("AutoFormatCallDialog");
        AutoFormatCall afc = dialog.createCall(mainWin);
        if (afc != null) {
            autoformat_listmodel.add(afc);
            autoformat_changed = true;
            updateAutoFormatListHeight();
        }
    }

    public void onEditAutoFormat() throws Exception
    {
        AutoFormatCallDialog dialog = (AutoFormatCallDialog) getPage().getFellow("AutoFormatCallDialog");
        if (autoformat_box.getSelectedCount() != 1) {
            Messagebox.show("Please select an entry from the list!");
            return;
        }
        int sel_idx = autoformat_box.getSelectedIndex();
        AutoFormatCall call = (AutoFormatCall) autoformat_listmodel.get(sel_idx);
        AutoFormatCall edited_call = dialog.editCall(call, mainWin);
        if (edited_call != null) {
            // user has changed values and clicked okay
            autoformat_listmodel.set(sel_idx, edited_call);
            autoformat_changed = true;
        }
    }

    public void onRemoveAutoFormat()
    {
        if (autoformat_box.getSelectedCount() != 1) {
            Messagebox.show("Please select an entry from the list!");
            return;
        }
        int sel_idx = autoformat_box.getSelectedIndex();
        autoformat_listmodel.remove(sel_idx);
        autoformat_changed = true;
        updateAutoFormatListHeight();
    }

    public void onAutoFormatUp()
    {
        if (autoformat_box.getSelectedCount() != 1) {
            Messagebox.show("Please select an entry from the list!");
            return;
        }
        int sel_idx = autoformat_box.getSelectedIndex();
        if (sel_idx > 0) {  // if move up possible
            Object obj = autoformat_listmodel.remove(sel_idx);
            autoformat_listmodel.add(sel_idx - 1, obj);
            autoformat_box.setSelectedIndex(sel_idx - 1);
            autoformat_changed = true;
        }
    }

    public void onAutoFormatDown()
    {
        if (autoformat_box.getSelectedCount() != 1) {
            Messagebox.show("Please select an entry from the list!");
            return;
        }
        int sel_idx = autoformat_box.getSelectedIndex();
        if (sel_idx < (autoformat_listmodel.size() - 1)) {  // if move down possible
            Object obj = autoformat_listmodel.remove(sel_idx);
            autoformat_listmodel.add(sel_idx + 1, obj);
            autoformat_box.setSelectedIndex(sel_idx + 1);
            autoformat_changed = true;
        }
    }
    
    /* --------------  Private methods  ---------------------- */

    private String getDataString(Event evt) throws Exception
    {
        Object data = evt.getData();
        if (data == null) { 
            return "";
        } else { 
            String str = data.toString();
            return (str == null) ? "" : str;
        }
    }

    private void init()
    {
        if (initialized) {
            return;
        }
        id_box = (Textbox) getFellow("StyleIdTextbox");
        formal_box = (Hbox) getFellow("StyleFormalBox");
        formal_checkbox = (Checkbox) getFellow("StyleFormalBlockCheckbox");
        labelid_box = (Textbox) getFellow("StyleFormalLabelTextbox");
        // idnote_label = (Label) getFellow("StyleIdNoteLabel");
        name_box = (Textbox) getFellow("StyleNameTextbox");
        // css_div = (Div) getFellow("StyleCSSDiv");
        cssprops_grid = (Grid) getFellow("StyleCSSPropsGrid");
        autoformat_label = (Label) getFellow("StyleAutoFormatLabel");
        autoformat_area = (Hbox) getFellow("StyleAutoFormatArea");
        autoformat_box = (Listbox) getFellow("StyleAutoFormatListbox");
        autoformat_listmodel = new ListModelList();
        autoformat_listmodel.setMultiple(false);
        autoformat_box.setModel(autoformat_listmodel);
        autoformat_box.setItemRenderer(new AFCRenderer());
        af_add_btn = (Button) getFellow("StyleDialogAddAutoFormatBtn");
        af_edit_btn = (Button) getFellow("StyleDialogEditAutoFormatBtn");
        af_remove_btn = (Button) getFellow("StyleDialogRemoveAutoFormatBtn");
        af_up_btn = (Button) getFellow("StyleDialogAutoFormatUpBtn");
        af_down_btn = (Button) getFellow("StyleDialogAutoFormatDownBtn");
        initialized = true;
    }

    private boolean hasInvalidInputs() throws Exception
    {
        if (mode == MODE_NEW) {
            String sid = id_box.getValue().trim();
            if (sid.equals("")) {
                Messagebox.show("Please enter a style ID!");
                return true;
            }
            if (sid.length() > 30) {
                Messagebox.show("Style ID is too long. Maximum length is 30 characters.");
                return true;
            }
            if (! sid.matches(DocmaConstants.REGEXP_STYLE_BASE_ID)) {
                Messagebox.show("Invalid style ID. Allowed characters are ASCII letters and underscore.");
                return true;
            }
            if (docmaSess.getStyle(sid) != null) {
                Messagebox.show("A style with this ID already exists!");
                return true;
            }
            if (sid.startsWith("_formal_")) {
                Messagebox.show("Invalid ID: The prefix '_formal_' is reserved for internal use!");
                return true;
            }
            if (docStyle.isInlineStyle()) {
                if (sid.startsWith("header")) {
                    Messagebox.show("An ID with prefix 'header' is only allowed for block-styles!");
                    return true;
                }
            }
        }
        String sname = name_box.getValue().trim();
        // if (sname.equals("")) {
        //     Messagebox.show("Please enter a style name!");
        //     return true;
        // }
        if (sname.length() > 40) {
            Messagebox.show("Style name is too long. Maximum length is 40 characters.");
            return true;
        }
        if ((sname.length() > 0) && !sname.matches(DocmaConstants.REGEXP_STYLE_NAME)) {
            Messagebox.show("Invalid style name. Allowed characters are ASCII letters, dash, underscore and space.");
            return true;
            // Important: When CSS is exported to file, then style name and other
            // non-CSS information (like autoformat) is encoded as comment.
            // Therefore style names that contain /* or */ or ':' are not allowed.
            // See DocmaStyle.java for details.
        }
        if (docStyle.isBlockStyle() && formal_checkbox.isChecked()) {
            String label_id = labelid_box.getValue().trim();
            if ((label_id.length() > 0) && !label_id.matches(DocmaConstants.REGEXP_STYLE_BASE_ID)) {
                Messagebox.show("Invalid label ID. Allowed characters are ASCII letters and underscore.");
                return true;
            }
        }
        return false;
    }

    private void setCssPropsGridContent(String cssText)
    {
        Rows gridrows = cssprops_grid.getRows();
        if (gridrows != null) {
            gridrows.getChildren().clear();
        } else {
            gridrows = new Rows();
            cssprops_grid.appendChild(gridrows);
        }
        SortedMap<String, String> props = CSSParser.parseCSSProperties(cssText);
        Iterator<String> it = props.keySet().iterator();
        while (it.hasNext()) {
            String pname = it.next();
            String pval = props.get(pname);
            Row r = new Row();
            buildCSSEditRow(r, pname, pval);
            gridrows.appendChild(r);
        }
        addCssNameSelectRow();
    }
    
    private void buildCSSEditRow(Row r, String pname, String pvalue)
    {
        // CSSPropInfo pi = CSSPropInfo.getInfoByCSSName(pname);
        r.appendChild(new Label(pname));
        Textbox txtbox = new Textbox();
        txtbox.setHflex("1");
        txtbox.setValue(pvalue);
        txtbox.addForward("onChange", this, "onCSSValueChange");
        Toolbarbutton toolbtn = new Toolbarbutton();
        toolbtn.setTooltiptext("Delete");
        toolbtn.setImage("img/del_css_prop.gif");
        toolbtn.setHeight("16px");
        toolbtn.setWidth("18px");
        // toolbtn.setHflex("min");
        toolbtn.addForward("onClick", this, "onDeleteCSSProperty", pname);
        Hbox hb = new Hbox();
        hb.setHflex("1");
        hb.setPack("stretch");
        hb.setAlign("center");
        hb.setSpacing("3px");
        hb.appendChild(txtbox);
        // if (pi.getType() == CSSPropInfo.TYPE_COLOR) {
        //     hb.appendChild(new Colorbox());
        // }
        hb.appendChild(toolbtn);
        r.appendChild(hb);
    }
    
    private void addCssNameSelectRow()
    {
        Rows gridrows = cssprops_grid.getRows();
        if (gridrows == null) {
            gridrows = new Rows();
            cssprops_grid.appendChild(gridrows);
        }
        
        Combobox cbox = new Combobox();
        Iterator<CSSPropInfo> pinfos = CSSPropInfo.allProps();
        while (pinfos.hasNext()) {
            CSSPropInfo pi = pinfos.next();
            cbox.appendItem(pi.getCssName());
        }
        cbox.setValue("");
        cbox.addForward("onChange", this, "onCSSNameChange");
        cbox.setAutocomplete(true);
        
        Cell empty_box = new Cell();   // dummy component (empty cell)
        
        Row r = new Row();
        r.appendChild(cbox);
        r.appendChild(empty_box);
        gridrows.appendChild(r);
    }
    
    private void checkCssNameSelectRow()
    {
        Rows gridrows = cssprops_grid.getRows();
        if (gridrows == null) {
            addCssNameSelectRow();
        } else {
            Component lastcomp = gridrows.getLastChild();
            if (lastcomp instanceof Row) {
                Component name_comp = lastcomp.getFirstChild();
                if (name_comp instanceof Combobox) {  // row with editable name already exists
                    String pname = getCssPropNameFromComponent(name_comp);
                    if (pname.length() > 0) {  // if user has entered property name
                        addCssNameSelectRow();  // add another row with editable name
                    }
                } else {  // add first row with editable name
                    addCssNameSelectRow();
                }
            }
        }
    }
    
    private String getGridCSS()
    {
        StringBuilder cssbuf = new StringBuilder();
        Iterator<Component> it = cssprops_grid.getRows().getChildren().iterator();
        while (it.hasNext()) {
            Component comp = it.next();
            if (comp instanceof Row) {
                List<Component> cells = comp.getChildren();
                if (cells.size() < 2) {
                    continue;
                }
                String propname = getCssPropNameFromComponent(cells.get(0));
                if (propname.length() > 0) {
                    Component box = cells.get(1);
                    Component value_comp = box.getFirstChild();
                    if (value_comp instanceof Textbox) {
                        String propval = ((Textbox) value_comp).getValue();
                        propval = (propval == null) ? "" : propval.trim();
                        if (propval.length() > 0) {
                            if (cssbuf.length() > 0) {
                                cssbuf.append(" ");
                            }
                            cssbuf.append(propname).append(":").append(propval).append(";");
                        }
                    }
                }
            }
        }
        return cssbuf.toString();
    }
    
    private String getCssPropNameFromComponent(Component name_comp)
    {
        String propname = "";
        if (name_comp instanceof Label) {
            propname = ((Label) name_comp).getValue().trim();
        } else if (name_comp instanceof Combobox) {
            propname = ((Combobox) name_comp).getValue().trim();
        }
        return propname;
    }
    
    private Row getCssRow(String propname)
    {
        Iterator<Component> it = cssprops_grid.getRows().getChildren().iterator();
        while (it.hasNext()) {
            Component comp = it.next();
            if (comp instanceof Row) {
                String name = getCssPropNameFromComponent(comp.getFirstChild());
                if ((name.length() > 0) && propname.equals(name)) {
                    return (Row) comp;
                }
            }
        }
        return null;
    }
    
    private void updateAutoFormatListHeight()
    {
        int height_increase = (autoformat_listmodel.size() > 0) ? 20 : 0;
        String af_height = String.valueOf(120 + height_increase) + "px";
        autoformat_box.setHeight(af_height);
    }


    private void updateGUI(DocmaStyle docmaStyle)
    {
        String sid = docmaStyle.getId();

        // Set visibility, enable/disable elements
        boolean is_block = docmaStyle.isBlockStyle();
        boolean is_internal_style = docmaStyle.isInternalStyle();
        formal_box.setVisible(is_block && !is_internal_style);
        af_add_btn.setDisabled(is_internal_style);
        af_edit_btn.setDisabled(is_internal_style);
        af_remove_btn.setDisabled(is_internal_style);
        af_up_btn.setDisabled(is_internal_style);
        af_down_btn.setDisabled(is_internal_style);
        autoformat_label.setVisible(!is_internal_style);
        autoformat_area.setVisible(!is_internal_style);

        // Set contents
        id_box.setValue(sid);
        boolean is_formal = docmaStyle.isFormal();
        formal_checkbox.setChecked(is_formal);
        String label_id = docmaStyle.getFormalLabelId();
        labelid_box.setValue((label_id == null) ? "" : label_id);
        labelid_box.setDisabled(! is_formal);
        // idnote_label.setVisible((mode == MODE_NEW) && docmaStyle.isBlockStyle());
        name_box.setValue(docmaStyle.getName());
        cssText = docmaStyle.getCSS();
        setCssPropsGridContent(cssText);

        DocmaWebApplication webapp = GUIUtil.getDocmaWebApplication(this);

        String url = "tinymce_editor/css_editor.jsp?desk=" + getDesktop().getId() +
                     "&appid=" + webapp.getSystemDefaultCSSEditor() + 
                     "&styletype=" + docmaStyle.getType() +
                     "&timestamp=" + System.currentTimeMillis();
        if (mode == MODE_EDIT) {
            url += "&styleid=" + docmaStyle.getId();
        }
        url = getDesktop().getExecution().encodeURL(url);

        Groupbox gb = (Groupbox) getFellow("StylePreviewGroupbox");
        Iframe frm = null;  // (Iframe) getFellow("csseditorfrm");
        for (Component comp : gb.getChildren()) {
            String comp_id = comp.getId();
            if ((comp_id != null) && comp_id.startsWith("csseditfrm")) {
                frm = (Iframe) comp;
                break;
            }
        }
        if (frm != null) {
            gb.removeChild(frm);  // frm.setParent(null);
        }
        frm = new Iframe();
        // Due to a problem with Mozilla FireFox Browser, the IFrame name has 
        // to be changed on every opening of the dialog.
        String frm_name = "csseditfrm" + System.currentTimeMillis();
        frm.setId(frm_name);
        frm.setName(frm_name);
        frm.setWidth("310px");
        frm.setHeight("130px");
        gb.appendChild(frm);
        // Iframe frm = (Iframe) getFellow("csseditorfrm");
        frm.setSrc(url);
        Clients.evalJavaScript("setCSSEditFrm('" + frm_name + "');");

//        Component comp;
//        while ((comp = css_div.getLastChild()) != null) {
//            css_div.removeChild(comp);
//        }
//        Span span1 = new Span();
//        span1.setId("docmastyle_preview_node");
//        span1.setStyle(docmaStyle.getCSS());
//        span1.appendChild(new Text("Abc def..."));
//        css_div.appendChild(span1);
//        Span span2 = new Span();
//        span2.setId("docmastyle_nosel_node");
//        css_div.appendChild(span2);

        // Span span1 = (Span) getFellow("seltest");
        // span1.setStyle("background-color:#FF0000;");

        // Initialize autoformat listbox
        autoformat_listmodel.clear();
        autoformat_listmodel.addAll(Arrays.asList(docmaStyle.getAutoFormatCalls(false)));
        autoformat_changed = false;
        updateAutoFormatListHeight();
    }

    private void updateModel(DocmaStyle docmaStyle) throws Exception
    {
        String s_id = id_box.getValue().trim();
        docmaStyle.setId(s_id);
        String s_name = name_box.getValue().trim();
        if (s_name.equals("")) s_name = s_id;  // set id as default name
        docmaStyle.setName(s_name);
        // Span span1 = (Span) getFellow("seltest");
        // String css = span1.getStyle();
        docmaStyle.setCSS(cssText);
        // Messagebox.show(css);
        if (autoformat_changed) {
            int count_calls = autoformat_listmodel.size();
            boolean is_formal = formal_checkbox.isChecked();
            if (is_formal) {
                ++count_calls;
            }
            AutoFormatCall[] afc_arr = new AutoFormatCall[count_calls];
            int offset = 0;
            if (is_formal) {
                String label_id = labelid_box.getValue().trim();
                afc_arr[0] = new AutoFormatCallImpl(DocmaStyle.AUTO_FORMAT_CLASS_FORMAL, label_id);
                ++offset;
            }
            for (int i=0; i < autoformat_listmodel.size(); i++) {
                afc_arr[offset + i] = (AutoFormatCall) autoformat_listmodel.get(i);
            }
            docmaStyle.setAutoFormatCalls(afc_arr);
        }
    }

    
    /* --------------  Helper class  ---------------------- */

    public static class AFCRenderer implements ListitemRenderer
    {
        public void render(Listitem item, Object data, int index) throws Exception
        {
            if (data instanceof AutoFormatCall) {
                AutoFormatCall afc = (AutoFormatCall) data;
                Listcell c1 = new Listcell(afc.getClassName());
                Listcell c2 = new Listcell(afc.getArgumentsLine());
                item.appendChild(c1);
                item.appendChild(c2);
                item.addForward("onDoubleClick", "StyleDialog", "onEditAutoFormat");
            }
        }
    }
}
