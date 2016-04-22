/*
 * FontWindowComposer.java
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

/**
 *
 * @author manfred
 */
public class FontWindowComposer extends SelectorComposer<Component>
{
    private EventListener editFontOkayListener = null;
    
    @Wire Window        whdFontWin;
    @Wire Combobox      whdEditFontFamilyBox;
    @Wire Doublespinner whdEditFontSizeBox;
    @Wire Listbox       whdEditFontSizeUnit;
    @Wire Textbox       whdEditFontColorTextbox;
    @Wire Colorbox      whdEditFontColorBox;
    @Wire Textbox       whdEditFontBgColorTextbox;
    @Wire Colorbox      whdEditFontBgColorBox;
    @Wire Listbox       whdEditFontWeightBox;
    @Wire Listbox       whdEditFontStyleBox;
    @Wire Combobox      whdEditFontDecorationBox;
    
    //********************************************************************
    //************   "Edit Font"-Dialog Event Handler   ******************
    //********************************************************************

    @Listen("onClick = #whdFontWin #whdEditFontOkayBtn")
    public void onEditFontOkayClick(Event evt) throws Exception
    {
        if (editFontOkayListener != null) {
            editFontOkayListener.onEvent(evt);
            editFontOkayListener = null;
        }
        whdFontWin.setVisible(false);
    }

    @Listen("onClick = #whdFontWin #whdEditFontCancelBtn")
    public void onEditFontCancelClick()
    {
        whdFontWin.setVisible(false);
    }

    @Listen("onChange = #whdFontWin #whdEditFontColorBox")
    public void onEditFontChangeColor()
    {
        whdEditFontColorTextbox.setValue(whdEditFontColorBox.getColor());
    }

    @Listen("onChange = #whdFontWin #whdEditFontColorTextbox")
    public void onEditFontChangeColorText()
    {
        String txt_col = whdEditFontColorTextbox.getValue().trim();
        if (txt_col.equals("")) { 
            txt_col = "#FFFFFF";
        }
        whdEditFontColorBox.setColor(txt_col);
    }

    @Listen("onChanging = #whdFontWin #whdEditFontColorTextbox")
    public void onEditFontChangingColorText(Event evt)
    {
        if (evt instanceof InputEvent) {
            InputEvent ie = (InputEvent) evt;
            String edit_val = ie.getValue();
            edit_val = (edit_val == null) ? "" : edit_val.trim();
            if (edit_val.startsWith("#") && (edit_val.length() == 7)) {
                whdEditFontColorBox.setColor(edit_val);
            }
        }
    }

    @Listen("onChange = #whdFontWin #whdEditFontBgColorBox")
    public void onEditFontChangeBgColor()
    {
        whdEditFontBgColorTextbox.setValue(whdEditFontBgColorBox.getColor());
    }

    @Listen("onChange = #whdFontWin #whdEditFontBgColorTextbox")
    public void onEditFontChangeBgColorText()
    {
        String txt_col = whdEditFontBgColorTextbox.getValue().trim();
        if (txt_col.equals("")) { 
            txt_col = "#FFFFFF";
        }
        whdEditFontBgColorBox.setColor(txt_col);
    }

    @Listen("onChanging = #whdFontWin #whdEditFontBgColorTextbox")
    public void onEditFontChangingBgColorText(Event evt)
    {
        if (evt instanceof InputEvent) {
            InputEvent ie = (InputEvent) evt;
            String edit_val = ie.getValue();
            edit_val = (edit_val == null) ? "" : edit_val.trim();
            if (edit_val.startsWith("#") && (edit_val.length() == 7)) {
                whdEditFontBgColorBox.setColor(edit_val);
            }
        }
    }


    //********************************************************************
    //***********   "Edit Font"-Dialog Public Methods   ******************
    //********************************************************************
    
    public void setOkayListener(EventListener listener) 
    {
        editFontOkayListener = listener;
    }
    
    public void initEditFontDialog(String css)
    {
        css = (css == null) ? "" : css.trim();
        
        String family = CssUtils.extractPropertyValue(css, "font-family");
        String size = CssUtils.extractPropertyValue(css, "font-size");
        String color = CssUtils.extractPropertyValue(css, "color");
        String weight = CssUtils.extractPropertyValue(css, "font-weight");
        String style = CssUtils.extractPropertyValue(css, "font-style");
        String deco = CssUtils.extractPropertyValue(css, "text-decoration");
        String bgcolor = CssUtils.extractPropertyValue(css, "background-color");
        
        whdEditFontFamilyBox.setValue((family == null) ? "" : family);
        setFontDialogSize(size);
        setFontDialogColor(color);
        setFontDialogBgColor(bgcolor);
        if ((weight == null) || ! WhdUtils.selectListItem(whdEditFontWeightBox, weight.toLowerCase())) {
            WhdUtils.selectListItem(whdEditFontWeightBox, "");
        }
        if ((style == null) || ! WhdUtils.selectListItem(whdEditFontStyleBox, style.toLowerCase())) {
            WhdUtils.selectListItem(whdEditFontStyleBox, "");
        }
        whdEditFontDecorationBox.setValue((deco == null) ? "" : deco);
    }

    public String getEditFontDialogCSS()
    {
        String family = whdEditFontFamilyBox.getValue().trim();
        String size = whdEditFontSizeBox.getText().trim();
        if (size.length() > 0) {
            Listitem sel = whdEditFontSizeUnit.getSelectedItem();
            size += (sel == null) ? "pt" : sel.getValue();
        }
        String color = whdEditFontColorTextbox.getValue().trim();
        // String color = col.equals("") ? "" : whdEditFontColorBox.getColor();
        String bgcolor = whdEditFontBgColorTextbox.getValue().trim();
        String weight = WhdUtils.getSelectedListValue(whdEditFontWeightBox, "");
        String style = WhdUtils.getSelectedListValue(whdEditFontStyleBox, "");
        String deco = whdEditFontDecorationBox.getValue().trim();
        
        String css = "";
        if (family.length() > 0) {
            css += "font-family:" + family.replace(';', ',') + "; ";
        }
        if (size.length() > 0) {
            css += "font-size:" + size + "; ";
        }
        if (color.length() > 0) {
            css += "color:" + color + "; ";
        }
        if (weight.length() > 0) {
            css += "font-weight:" + weight + "; ";
        }
        if (style.length() > 0) {
            css += "font-style:" + style + "; ";
        }
        if (deco.length() > 0) {
            css += "text-decoration:" + deco.replace(';', ' ').trim() + "; ";
        }
        if (bgcolor.length() > 0) {
            css += "background-color:" + bgcolor + "; ";
        }
        return css;
    }


    //********************************************************************
    //****************   "Edit Font"-Dialog Helper   *********************
    //********************************************************************
    
    private void setFontDialogColor(String color)
    {
        if ((color == null) || color.equals("")) {
            whdEditFontColorTextbox.setValue("");
            whdEditFontColorBox.setColor("#FFFFFF");
        } else {
            whdEditFontColorTextbox.setValue(color);
            whdEditFontColorBox.setColor(color);
        }
    }

    private void setFontDialogBgColor(String color)
    {
        if ((color == null) || color.equals("")) {
            whdEditFontBgColorTextbox.setValue("");
            whdEditFontBgColorBox.setColor("#FFFFFF");
        } else {
            whdEditFontBgColorTextbox.setValue(color);
            whdEditFontBgColorBox.setColor(color);
        }
    }

    private void setFontDialogSize(String size)
    {
        if ((size == null) || size.equals("")) {
            whdEditFontSizeBox.setText("");
            WhdUtils.selectListItem(whdEditFontSizeUnit, "pt");
            return;
        }
        String value;
        String unit;
        if (size.length() >= 2) {
            unit = size.substring(size.length() - 2);
            value = size.substring(0, size.length() - 2);
        } else {
            value = size;
            unit = "pt";
        }
        double doub;
        try {
            doub = Double.parseDouble(value);
        } catch (Exception ex) {
            doub = 1.0;
        }
        whdEditFontSizeBox.setValue(doub);
        if (! WhdUtils.selectListItem(whdEditFontSizeUnit, unit)) {
            WhdUtils.selectListItem(whdEditFontSizeUnit, "pt");  //  unit = "pt";
        }
    }
    

}
