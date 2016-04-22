/*
 *  BackgroundWindowComposer.java
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

import java.io.File;
import java.io.IOException;

/**
 *
 * @author manfred
 */
public class BackgroundWindowComposer extends SelectorComposer<Component>
{
    private EventListener editBorderOkayListener = null;
    private File initialBgImageFile = null;
    private File tempUploadDir = null;
    private String tempURLPath = "";
    private File editBorderBgImageUploaded = null;
    private String editBorderBgImageURL = null;
    
    @Wire Window whdBorderWin;
    @Wire Checkbox whdEditBorderSameBox;
    @Wire Checkbox whdEditBorderEnableRadiusBox;
    @Wire Div whdEditBorderPreviewDiv;
    @Wire Component whdEditBorderBottomArea;
    @Wire Cell whdEditBorderRightArea;
    @Wire Cell whdEditBorderLeftArea;
    @Wire Cell whdEditBorderTopLeftArea;
    @Wire Cell whdEditBorderTopRightArea;
    @Wire Component whdEditBorderTopLeftRadiusArea;
    @Wire Component whdEditBorderTopRightRadiusArea;
    @Wire Component whdEditBorderBottomLeftRadiusArea;
    @Wire Component whdEditBorderBottomRightRadiusArea;
    @Wire Cell whdEditBorderTopCell;
    @Wire Label whdEditBorderTopLabel;
    
    @Wire  Listbox whdEditBorderTopStyleBox;
    @Wire  Doublespinner whdEditBorderTopWidthBox;
    @Wire Listbox whdEditBorderTopWidthUnit;
    @Wire  Colorbox whdEditBorderTopColorBox;
    @Wire Textbox whdEditBorderTopColorTextbox;

    @Wire  Listbox whdEditBorderRightStyleBox;
    @Wire  Doublespinner whdEditBorderRightWidthBox;
    @Wire Listbox whdEditBorderRightWidthUnit;
    @Wire  Colorbox whdEditBorderRightColorBox;
    @Wire Textbox whdEditBorderRightColorTextbox;

    @Wire  Listbox whdEditBorderBottomStyleBox;
    @Wire  Doublespinner whdEditBorderBottomWidthBox;
    @Wire Listbox whdEditBorderBottomWidthUnit;
    @Wire  Colorbox whdEditBorderBottomColorBox;
    @Wire Textbox whdEditBorderBottomColorTextbox;

    @Wire  Listbox whdEditBorderLeftStyleBox;
    @Wire  Doublespinner whdEditBorderLeftWidthBox;
    @Wire Listbox whdEditBorderLeftWidthUnit;
    @Wire  Colorbox whdEditBorderLeftColorBox;
    @Wire Textbox whdEditBorderLeftColorTextbox;

    @Wire  Doublespinner whdEditBorderTopLeftRad1Box;
    @Wire  Doublespinner whdEditBorderTopLeftRad2Box;
    @Wire  Listbox whdEditBorderTopLeftRadUnit;

    @Wire  Doublespinner whdEditBorderTopRightRad1Box;
    @Wire  Doublespinner whdEditBorderTopRightRad2Box;
    @Wire  Listbox whdEditBorderTopRightRadUnit;

    @Wire  Doublespinner whdEditBorderBottomLeftRad1Box;
    @Wire  Doublespinner whdEditBorderBottomLeftRad2Box;
    @Wire  Listbox whdEditBorderBottomLeftRadUnit;

    @Wire  Doublespinner whdEditBorderBottomRightRad1Box;
    @Wire  Doublespinner whdEditBorderBottomRightRad2Box;
    @Wire  Listbox whdEditBorderBottomRightRadUnit;

    @Wire  Colorbox whdEditBorderBgColorBox;
    @Wire Textbox whdEditBorderBgColorTextbox;
    @Wire Button whdEditBorderBgImageDownloadBtn;
    @Wire Button whdEditBorderBgImageRemoveBtn;
    @Wire Component whdEditBorderBgRepeatArea;
    @Wire Checkbox whdEditBorderBgRepeatXBox;
    @Wire Checkbox whdEditBorderBgRepeatYBox;

    //********************************************************************
    //********   "Edit Border/Background"-Dialog Public Methods   ********
    //********************************************************************
    
    public void setOkayListener(EventListener listener) 
    {
        editBorderOkayListener = listener;
    }
    
    public String getBackgroundImagePreviewURL()
    {
        return editBorderBgImageURL;
    }
    
    
    public boolean hasBackgroundImage()
    {
        return (editBorderBgImageURL != null);
    }

    public File getUploadedBackgroundImageFile()
    {
        return editBorderBgImageUploaded;
    }

    public void initEditBorderDialog(String css, 
                                     File bgImageFile, 
                                     File tempUploadDir, 
                                     String tempURLPath)
    {
        css = (css == null) ? "" : css.trim();
        
        this.initialBgImageFile = bgImageFile;
        this.tempUploadDir = tempUploadDir;
        this.tempURLPath = (tempURLPath == null) ? "" : tempURLPath;
        if ((this.tempURLPath.length() > 0) && !this.tempURLPath.endsWith("/")) {
            this.tempURLPath += "/";
        }
        
        String bgcolor = CssUtils.extractPropertyValue(css, "background-color");
        if ((bgcolor == null) || bgcolor.equals("") || bgcolor.equals("transparent")) {
            whdEditBorderBgColorTextbox.setValue("");  // empty string means transparent
            whdEditBorderBgColorBox.setColor("#FFFFFF");
        } else {
            setBorderDialogColor(whdEditBorderBgColorBox, whdEditBorderBgColorTextbox, bgcolor);
        }

        editBorderBgImageUploaded = null;
        editBorderBgImageURL = null;
        String bgimage = CssUtils.extractPropertyValue(css, "background-image");
        String bgrep = "";
        if (bgimage != null) {
            // bgimage = bgimage.trim();
            // final String START_URL = "url(";
            // if (bgimage.toLowerCase().startsWith(START_URL)) {
            //     bgimage = bgimage.substring(START_URL.length(), bgimage.length() - 1);
            // }
            editBorderBgImageURL = bgimage;
            bgrep = CssUtils.extractPropertyValue(css, "background-repeat");
        }
        whdEditBorderBgImageDownloadBtn.setVisible(bgimage != null);
        whdEditBorderBgImageRemoveBtn.setVisible(bgimage != null);
        whdEditBorderBgRepeatArea.setVisible(bgimage != null);
        whdEditBorderBgRepeatXBox.setChecked("repeat".equals(bgrep) || "repeat-x".equals(bgrep));
        whdEditBorderBgRepeatYBox.setChecked("repeat".equals(bgrep) || "repeat-y".equals(bgrep));

        String all_style = CssUtils.extractPropertyValue(css, "border-style");
        String all_width = CssUtils.extractPropertyValue(css, "border-width");
        String all_color = CssUtils.extractPropertyValue(css, "border-color");
        
        boolean same_for_all = css.equals("") || (all_style != null);
        boolean has_radius = css.contains("-radius");
        whdEditBorderSameBox.setChecked(same_for_all);
        whdEditBorderEnableRadiusBox.setChecked(has_radius);
        editBorderSameOrRadiusUpdated();
        
        String top_style = same_for_all ? all_style : CssUtils.extractPropertyValue(css, "border-top-style");
        String top_width = same_for_all ? all_width : CssUtils.extractPropertyValue(css, "border-top-width");
        String top_color = same_for_all ? all_color : CssUtils.extractPropertyValue(css, "border-top-color");
        
        String right_style = same_for_all ? all_style : CssUtils.extractPropertyValue(css, "border-right-style");
        String right_width = same_for_all ? all_width : CssUtils.extractPropertyValue(css, "border-right-width");
        String right_color = same_for_all ? all_color : CssUtils.extractPropertyValue(css, "border-right-color");
        
        String bottom_style = same_for_all ? all_style : CssUtils.extractPropertyValue(css, "border-bottom-style");
        String bottom_width = same_for_all ? all_width : CssUtils.extractPropertyValue(css, "border-bottom-width");
        String bottom_color = same_for_all ? all_color : CssUtils.extractPropertyValue(css, "border-bottom-color");
        
        String left_style = same_for_all ? all_style : CssUtils.extractPropertyValue(css, "border-left-style");
        String left_width = same_for_all ? all_width : CssUtils.extractPropertyValue(css, "border-left-width");
        String left_color = same_for_all ? all_color : CssUtils.extractPropertyValue(css, "border-left-color");
        
        setBorderDialogStyle(whdEditBorderTopStyleBox, top_style);
        setBorderDialogWidth(whdEditBorderTopWidthBox, whdEditBorderTopWidthUnit, top_width);
        setBorderDialogColor(whdEditBorderTopColorBox, whdEditBorderTopColorTextbox, top_color);
        
        setBorderDialogStyle(whdEditBorderRightStyleBox, right_style);
        setBorderDialogWidth(whdEditBorderRightWidthBox, whdEditBorderRightWidthUnit, right_width);
        setBorderDialogColor(whdEditBorderRightColorBox, whdEditBorderRightColorTextbox, right_color);
        
        setBorderDialogStyle(whdEditBorderBottomStyleBox, bottom_style);
        setBorderDialogWidth(whdEditBorderBottomWidthBox, whdEditBorderBottomWidthUnit, bottom_width);
        setBorderDialogColor(whdEditBorderBottomColorBox, whdEditBorderBottomColorTextbox, bottom_color);
        
        setBorderDialogStyle(whdEditBorderLeftStyleBox, left_style);
        setBorderDialogWidth(whdEditBorderLeftWidthBox, whdEditBorderLeftWidthUnit, left_width);
        setBorderDialogColor(whdEditBorderLeftColorBox, whdEditBorderLeftColorTextbox, left_color);

        // if (has_radius) {
            String all_radius = CssUtils.extractPropertyValue(css, "border-radius");
            boolean same_radius = (all_radius != null);
            String top_left_rad = same_radius ? all_radius : CssUtils.extractPropertyValue(css, "border-top-left-radius");
            String top_right_rad = same_radius ? all_radius : CssUtils.extractPropertyValue(css, "border-top-right-radius");
            String bot_left_rad = same_radius ? all_radius : CssUtils.extractPropertyValue(css, "border-bottom-left-radius");
            String bot_right_rad = same_radius ? all_radius : CssUtils.extractPropertyValue(css, "border-bottom-right-radius");
            setBorderDialogRadius(whdEditBorderTopLeftRad1Box, whdEditBorderTopLeftRad2Box, whdEditBorderTopLeftRadUnit, top_left_rad);
            setBorderDialogRadius(whdEditBorderTopRightRad1Box, whdEditBorderTopRightRad2Box, whdEditBorderTopRightRadUnit, top_right_rad);
            setBorderDialogRadius(whdEditBorderBottomLeftRad1Box, whdEditBorderBottomLeftRad2Box, whdEditBorderBottomLeftRadUnit, bot_left_rad);
            setBorderDialogRadius(whdEditBorderBottomRightRad1Box, whdEditBorderBottomRightRad2Box, whdEditBorderBottomRightRadUnit, bot_right_rad);
        // }
        whdEditBorderPreviewDiv.setStyle(css);
        // whdEditBorderPreviewDiv.invalidate();
    }
    
    public String getBgColor()
    {
        String txt_col = whdEditBorderBgColorTextbox.getValue().trim();
        return (txt_col.equals("") || txt_col.equalsIgnoreCase("transparent")) ? "transparent" : whdEditBorderBgColorBox.getColor();
    }
    
    public String getBgRepeat()
    {
        boolean rx = whdEditBorderBgRepeatXBox.isChecked();
        boolean ry = whdEditBorderBgRepeatYBox.isChecked();
        return rx ? (ry ? "repeat" : "repeat-x") : (ry ? "repeat-y" : "no-repeat"); 
    }
    
    public String getBorderCSS()
    {
        String top_style = whdEditBorderTopStyleBox.getSelectedItem().getValue().toString();
        String top_width = whdEditBorderTopWidthBox.getText() + whdEditBorderTopWidthUnit.getSelectedItem().getValue().toString();
        String top_color = whdEditBorderTopColorBox.getColor();
        
        String right_style = whdEditBorderRightStyleBox.getSelectedItem().getValue().toString();
        String right_width = whdEditBorderRightWidthBox.getText() + whdEditBorderRightWidthUnit.getSelectedItem().getValue().toString();
        String right_color = whdEditBorderRightColorBox.getColor();
        
        String bottom_style = whdEditBorderBottomStyleBox.getSelectedItem().getValue().toString();
        String bottom_width = whdEditBorderBottomWidthBox.getText() + whdEditBorderBottomWidthUnit.getSelectedItem().getValue().toString();
        String bottom_color = whdEditBorderBottomColorBox.getColor();
        
        String left_style = whdEditBorderLeftStyleBox.getSelectedItem().getValue().toString();
        String left_width = whdEditBorderLeftWidthBox.getText() + whdEditBorderLeftWidthUnit.getSelectedItem().getValue().toString();
        String left_color = whdEditBorderLeftColorBox.getColor();
        
        boolean same = whdEditBorderSameBox.isChecked();
        String css;
        if (same) {
            css = formatBorderCSS(null, top_style, top_width, top_color);
        } else {
            css = formatBorderCSS("top", top_style, top_width, top_color) +
                  formatBorderCSS("right", right_style, right_width, right_color) +
                  formatBorderCSS("bottom", bottom_style, bottom_width, bottom_color) +
                  formatBorderCSS("left", left_style, left_width, left_color);
        }

        boolean has_radius = whdEditBorderEnableRadiusBox.isChecked();
        if (has_radius) {
            if (same) {
                String r = getBorderRadiusValue(whdEditBorderTopRightRad1Box, whdEditBorderTopRightRad2Box, whdEditBorderTopRightRadUnit, '/', null);
                if (r != null) {
                    css += "border-radius:" + r +  "; ";
                }
            } else {
                css += "border-top-left-radius:" +
                       getBorderRadiusValue(whdEditBorderTopLeftRad1Box, whdEditBorderTopLeftRad2Box, whdEditBorderTopLeftRadUnit, ' ', "0px") + 
                       "; border-top-right-radius:" +
                       getBorderRadiusValue(whdEditBorderTopRightRad1Box, whdEditBorderTopRightRad2Box, whdEditBorderTopRightRadUnit, ' ', "0px") + 
                       "; border-bottom-right-radius:" +
                       getBorderRadiusValue(whdEditBorderBottomRightRad1Box, whdEditBorderBottomRightRad2Box, whdEditBorderBottomRightRadUnit, ' ', "0px") + 
                       "; border-bottom-left-radius:" +
                       getBorderRadiusValue(whdEditBorderBottomLeftRad1Box, whdEditBorderBottomLeftRad2Box, whdEditBorderBottomLeftRadUnit, ' ', "0px") + 
                       "; ";
            }
        }
        
        return css;
    }
    

    //********************************************************************
    //************   "Edit Border"-Dialog Event Handler   ****************
    //********************************************************************

    @Listen("onClick = #whdBorderWin #whdEditBorderOkayBtn")
    public void onEditBorderOkayClick(Event evt) throws Exception
    {
        if (editBorderOkayListener != null) {
            editBorderOkayListener.onEvent(evt);
            editBorderOkayListener = null;
        }
        whdBorderWin.setVisible(false);
    }

    @Listen("onClick = #whdBorderWin #whdEditBorderCancelBtn")
    public void onEditBorderCancelClick()
    {
        whdBorderWin.setVisible(false);
    }

    @Listen("onCheck = #whdBorderWin #whdEditBorderSameBox; onCheck = #whdBorderWin #whdEditBorderEnableRadiusBox;")
    public void onEditBorderSameClick()
    {
        editBorderSameOrRadiusUpdated();
        updateBorderDialogPreview();
    }
    
    @Listen("onSelect = #whdBorderWin #whdEditBorderTopStyleBox")
    public void onEditBorderSelectTopStyle()
    {
        updateBorderDialogPreview();
        borderDialogStyleUpdated(whdEditBorderTopStyleBox);
    }

    @Listen("onSelect = #whdBorderWin #whdEditBorderRightStyleBox")
    public void onEditBorderSelectRightStyle()
    {
        updateBorderDialogPreview();
        borderDialogStyleUpdated(whdEditBorderRightStyleBox);
    }

    @Listen("onSelect = #whdBorderWin #whdEditBorderBottomStyleBox")
    public void onEditBorderSelectBottomStyle()
    {
        updateBorderDialogPreview();
        borderDialogStyleUpdated(whdEditBorderBottomStyleBox);
    }

    @Listen("onSelect = #whdBorderWin #whdEditBorderLeftStyleBox")
    public void onEditBorderSelectLeftStyle()
    {
        updateBorderDialogPreview();
        borderDialogStyleUpdated(whdEditBorderLeftStyleBox);
    }

    @Listen("onChange = #whdBorderWin #whdEditBorderTopWidthBox; onChange = #whdBorderWin #whdEditBorderRightWidthBox; onChange = #whdBorderWin #whdEditBorderBottomWidthBox; onChange = #whdBorderWin #whdEditBorderLeftWidthBox;")
    public void onEditBorderChangeWidth()
    {
        updateBorderDialogPreview();
    }

    @Listen("onChanging = #whdBorderWin #whdEditBorderTopWidthBox; onChanging = #whdBorderWin #whdEditBorderRightWidthBox; onChanging = #whdBorderWin #whdEditBorderBottomWidthBox; onChanging = #whdBorderWin #whdEditBorderLeftWidthBox;")
    public void onEditBorderChangingWidth(Event evt)
    {
        if (evt instanceof InputEvent) {
            InputEvent ie = (InputEvent) evt;
            String edit_val = ie.getValue();
            try {
                Double.parseDouble(edit_val);
                Component comp = ie.getTarget();
                if (comp instanceof Doublespinner) {
                    ((Doublespinner) comp).setText(edit_val);
                }
                updateBorderDialogPreview();
            } catch (Exception ex) {}
        }
    }

    @Listen("onSelect = #whdBorderWin #whdEditBorderTopWidthUnit; onSelect = #whdBorderWin #whdEditBorderRightWidthUnit; onSelect = #whdBorderWin #whdEditBorderBottomWidthUnit; onSelect = #whdBorderWin #whdEditBorderLeftWidthUnit;")
    public void onEditBorderSelectWidthUnit()
    {
        updateBorderDialogPreview();
    }

    @Listen("onChange = #whdBorderWin #whdEditBorderTopColorBox; onChange = #whdBorderWin #whdEditBorderRightColorBox; onChange = #whdBorderWin #whdEditBorderBottomColorBox; onChange = #whdBorderWin #whdEditBorderLeftColorBox;")
    public void onEditBorderChangeColor(Event evt)
    {
        Component comp = evt.getTarget();
        if (comp == whdEditBorderTopColorBox) {
            whdEditBorderTopColorTextbox.setValue(whdEditBorderTopColorBox.getColor());
        } else 
        if (comp == whdEditBorderRightColorBox) {
            whdEditBorderRightColorTextbox.setValue(whdEditBorderRightColorBox.getColor());
        } else 
        if (comp == whdEditBorderBottomColorBox) {
            whdEditBorderBottomColorTextbox.setValue(whdEditBorderBottomColorBox.getColor());
        } else 
        if (comp == whdEditBorderLeftColorBox) {
            whdEditBorderLeftColorTextbox.setValue(whdEditBorderLeftColorBox.getColor());
        }
        updateBorderDialogPreview();
    }

    @Listen("onChange = #whdBorderWin #whdEditBorderTopColorTextbox; onChange = #whdBorderWin #whdEditBorderRightColorTextbox; onChange = #whdBorderWin #whdEditBorderBottomColorTextbox; onChange = #whdBorderWin #whdEditBorderLeftColorTextbox;")
    public void onEditBorderChangeColorText(Event evt)
    {
        Component comp = evt.getTarget();
        if (comp == whdEditBorderTopColorTextbox) {
            whdEditBorderTopColorBox.setColor(whdEditBorderTopColorTextbox.getValue());
        } else 
        if (comp == whdEditBorderRightColorTextbox) {
            whdEditBorderRightColorBox.setColor(whdEditBorderRightColorTextbox.getValue());
        } else 
        if (comp == whdEditBorderBottomColorTextbox) {
            whdEditBorderBottomColorBox.setColor(whdEditBorderBottomColorTextbox.getValue());
        } else 
        if (comp == whdEditBorderLeftColorTextbox) {
            whdEditBorderLeftColorBox.setColor(whdEditBorderLeftColorTextbox.getValue());
        }
        updateBorderDialogPreview();
    }

    @Listen("onChanging = #whdBorderWin #whdEditBorderTopColorTextbox; onChanging = #whdBorderWin #whdEditBorderRightColorTextbox; onChanging = #whdBorderWin #whdEditBorderBottomColorTextbox; onChanging = #whdBorderWin #whdEditBorderLeftColorTextbox;")
    public void onEditBorderChangingColorText(Event evt)
    {
        if (evt instanceof InputEvent) {
            InputEvent ie = (InputEvent) evt;
            String edit_val = ie.getValue();
            edit_val = (edit_val == null) ? "" : edit_val.trim();
            if (edit_val.startsWith("#") && (edit_val.length() == 7)) {
                Component comp = ie.getTarget();
                if (comp == whdEditBorderTopColorTextbox) {
                    whdEditBorderTopColorBox.setColor(edit_val);
                } else 
                if (comp == whdEditBorderRightColorTextbox) {
                    whdEditBorderRightColorBox.setColor(edit_val);
                } else 
                if (comp == whdEditBorderBottomColorTextbox) {
                    whdEditBorderBottomColorBox.setColor(edit_val);
                } else 
                if (comp == whdEditBorderLeftColorTextbox) {
                    whdEditBorderLeftColorBox.setColor(edit_val);
                }
                updateBorderDialogPreview();
            }
        }
    }

    @Listen("onChange = #whdBorderWin #whdEditBorderBgColorBox;")
    public void onEditBorderChangeBgColor()
    {
        whdEditBorderBgColorTextbox.setValue(whdEditBorderBgColorBox.getColor());
        updateBorderDialogPreview();
    }

    @Listen("onChange = #whdBorderWin #whdEditBorderBgColorTextbox;")
    public void onEditBorderChangeBgColorText()
    {
        String txt_col = whdEditBorderBgColorTextbox.getValue().trim();
        if (txt_col.equals("") || txt_col.equals("transparent")) {  // empty string means transparent
            txt_col = "#FFFFFF";
        }
        whdEditBorderBgColorBox.setColor(txt_col);
        updateBorderDialogPreview();
    }

    @Listen("onChanging = #whdBorderWin #whdEditBorderBgColorTextbox;")
    public void onEditBorderChangingBgColorText(Event evt)
    {
        if (evt instanceof InputEvent) {
            InputEvent ie = (InputEvent) evt;
            String edit_val = ie.getValue();
            edit_val = (edit_val == null) ? "" : edit_val.trim();
            if (edit_val.startsWith("#") && (edit_val.length() == 7)) {
                whdEditBorderBgColorBox.setColor(edit_val);
                updateBorderDialogPreview();
            }
        }
    }

    @Listen("onUpload = #whdBorderWin #whdEditBorderBgImageUploadBtn")
    public void onEditBorderBgImageUploadClick(Event evt) throws Exception
    {
        UploadEvent uevt = (UploadEvent) evt;
        org.zkoss.util.media.Media media = uevt.getMedia();
        String fn = media.getName();
        String ext = (fn == null) ? null : WhdUtils.getFilenameExtension(fn);
        if ((ext == null) || (ext.equals(""))) {
            Messagebox.show("Invalid file extension!");
            return;
        }
        if (media instanceof org.zkoss.image.Image) {
            String temp_fname = "bgimg" + System.currentTimeMillis() + "." + ext;
            editBorderBgImageUploaded = writeImageToTempPreviewDir(media, temp_fname);
            editBorderBgImageURL = "url(" + tempURLPath + temp_fname + ")";
            updateBorderDialogPreview();
        } else {
            Messagebox.show("Unknown image format!");
            return;
        }
    }

    @Listen("onClick = #whdBorderWin #whdEditBorderBgImageDownloadBtn")
    public void onEditBorderBgImageDownloadClick() throws Exception
    {
        if (editBorderBgImageUploaded != null) {
            Filedownload.save(editBorderBgImageUploaded, null);
        } else 
        if (editBorderBgImageURL != null) {  // background image exists
            if (initialBgImageFile != null) {
                Filedownload.save(initialBgImageFile, null);
            } else {
                Messagebox.show("File has been changed. Save the design before download.");
                return;
            }
        }
    }

    @Listen("onClick = #whdBorderWin #whdEditBorderBgImageRemoveBtn")
    public void onEditBorderBgImageRemoveClick()
    {
        editBorderBgImageUploaded = null;
        editBorderBgImageURL = null;
        updateBorderDialogPreview();
    }

    @Listen("onCheck = #whdBorderWin #whdEditBorderBgRepeatXBox; onCheck = #whdBorderWin #whdEditBorderBgRepeatYBox")
    public void onEditBorderBgRepeatChanged()
    {
        updateBorderDialogPreview();
    }

    @Listen("onChange = #whdBorderWin #whdEditBorderTopLeftRad1Box; onChange = #whdBorderWin #whdEditBorderTopRightRad1Box; onChange = #whdBorderWin #whdEditBorderBottomLeftRad1Box; onChange = #whdBorderWin #whdEditBorderBottomRightRad1Box;")
    public void onEditBorderChangeRadius1()
    {
        updateBorderDialogPreview();
    }

    @Listen("onChange = #whdBorderWin #whdEditBorderTopLeftRad2Box; onChange = #whdBorderWin #whdEditBorderTopRightRad2Box; onChange = #whdBorderWin #whdEditBorderBottomLeftRad2Box; onChange = #whdBorderWin #whdEditBorderBottomRightRad2Box;")
    public void onEditBorderChangeRadius2(Event evt)
    {
        updateBorderDialogPreview();
    }

    @Listen("onChanging = #whdBorderWin #whdEditBorderTopLeftRad1Box; onChanging = #whdBorderWin #whdEditBorderTopRightRad1Box; onChanging = #whdBorderWin #whdEditBorderBottomLeftRad1Box; onChanging = #whdBorderWin #whdEditBorderBottomRightRad1Box;")
    public void onEditBorderChangingRadius1(Event evt)
    {
        onEditBorderChangingRad(evt);
    }
    
    @Listen("onChanging = #whdBorderWin #whdEditBorderTopLeftRad2Box; onChanging = #whdBorderWin #whdEditBorderTopRightRad2Box; onChanging = #whdBorderWin #whdEditBorderBottomLeftRad2Box; onChanging = #whdBorderWin #whdEditBorderBottomRightRad2Box;")
    public void onEditBorderChangingRadius2(Event evt)
    {
        onEditBorderChangingRad(evt);
    }
    
    private void onEditBorderChangingRad(Event evt)
    {
        if (evt instanceof InputEvent) {
            InputEvent ie = (InputEvent) evt;
            String edit_val = ie.getValue();
            try {
                Double.parseDouble(edit_val);
                Component comp = ie.getTarget();
                if (comp instanceof Doublespinner) {
                    ((Doublespinner) comp).setText(edit_val);
                }
                updateBorderDialogPreview();
            } catch (Exception ex) {}
        }
    }

    @Listen("onSelect = #whdBorderWin #whdEditBorderTopLeftRadUnit; onSelect = #whdBorderWin #whdEditBorderTopRightRadUnit; onSelect = #whdBorderWin #whdEditBorderBottomLeftRadUnit; onSelect = #whdBorderWin #whdEditBorderBottomRightRadUnit;")
    public void onEditBorderSelectRadiusUnit()
    {
        updateBorderDialogPreview();
    }

    //********************************************************************
    //****************   "Edit Border"-Dialog Helper   *******************
    //********************************************************************
    
    private File writeImageToTempPreviewDir(org.zkoss.util.media.Media media, String filename) throws IOException
    {
        File dir = tempUploadDir;
        if (! dir.exists()) {
            dir.mkdirs();
        }
        File f = new File(dir, filename);
        WhdUtils.writeBytesToFile(media.getByteData(), f);
        return f;
    }

    private void setBorderDialogStyle(Listbox box, String style_val)
    {
        style_val = (style_val == null) ? "none" : style_val.toLowerCase();
        if (! WhdUtils.selectListItem(box, style_val)) {
            WhdUtils.selectListItem(box, "none");
            style_val = "none";
        }
        borderDialogStyleUpdated(box);
    }
    
    private void borderDialogStyleUpdated(Listbox box)
    {
        String style_val = box.getSelectedItem().getValue().toString();
        boolean none = style_val.equals("none");
        if (box == whdEditBorderTopStyleBox) {
            if (none) {
                whdEditBorderTopWidthBox.setText("0");
            }
            whdEditBorderTopWidthBox.setDisabled(none);
            whdEditBorderTopWidthUnit.setDisabled(none);
            whdEditBorderTopColorBox.setDisabled(none);
            whdEditBorderTopColorTextbox.setValue(none ? "" : whdEditBorderTopColorBox.getColor());
            whdEditBorderTopColorTextbox.setDisabled(none);
        } else
        if (box == whdEditBorderRightStyleBox) {
            if (none) {
                whdEditBorderRightWidthBox.setText("0");
            }
            whdEditBorderRightWidthBox.setDisabled(none);
            whdEditBorderRightWidthUnit.setDisabled(none);
            whdEditBorderRightColorBox.setDisabled(none);
            whdEditBorderRightColorTextbox.setValue(none ? "" : whdEditBorderRightColorBox.getColor());
            whdEditBorderRightColorTextbox.setDisabled(none);
        } else
        if (box == whdEditBorderBottomStyleBox) {
            if (none) {
                whdEditBorderBottomWidthBox.setText("0");
            }
            whdEditBorderBottomWidthBox.setDisabled(none);
            whdEditBorderBottomWidthUnit.setDisabled(none);
            whdEditBorderBottomColorBox.setDisabled(none);
            whdEditBorderBottomColorTextbox.setValue(none ? "" : whdEditBorderBottomColorBox.getColor());
            whdEditBorderBottomColorTextbox.setDisabled(none);
        } else
        if (box == whdEditBorderLeftStyleBox) {
            if (none) {
                whdEditBorderLeftWidthBox.setText("0");
            }
            whdEditBorderLeftWidthBox.setDisabled(none);
            whdEditBorderLeftWidthUnit.setDisabled(none);
            whdEditBorderLeftColorBox.setDisabled(none);
            whdEditBorderLeftColorTextbox.setValue(none ? "" : whdEditBorderLeftColorBox.getColor());
            whdEditBorderLeftColorTextbox.setDisabled(none);
        }
    }
    
    private void setBorderDialogWidth(Doublespinner value_box, Listbox unit_box, String width)
    {
        if ((width == null) || width.equals("")) {
            width = "1px";
        }
        String value;
        String unit;
        if (width.length() >= 2) {
            unit = width.substring(width.length() - 2);
            value = width.substring(0, width.length() - 2);
        } else {
            value = width;
            unit = "px";
        }
        double doub;
        try {
            doub = Double.parseDouble(value);
        } catch (Exception ex) {
            doub = 1.0;
        }
        value_box.setValue(doub);
        if (! WhdUtils.selectListItem(unit_box, unit)) {
            WhdUtils.selectListItem(unit_box, "px");  //  unit = "px";
        }
    }
    
    private void setBorderDialogColor(Colorbox col_box, Textbox txt_box, String color)
    {
        if ((color == null) || color.equals("")) {
            color = "#000000";
        } 
        txt_box.setValue(color);
        col_box.setColor(color);
    }
    
    private void setBorderDialogRadius(Doublespinner box1, Doublespinner box2, Listbox unit_box, String value)
    {
        value = (value == null) ? "" : value.trim();
        // Examples values: "4px", "4px 3px", "4px/3px"
        // If two values are separated by space or slash, then the first value 
        // is the horizontal radius and the second value is the vertical radius.
        String v1 = "";
        String v2 = "";
        if (! value.equals("")) {
            int split_pos = value.indexOf('/');
            if (split_pos < 0) {
                split_pos = value.indexOf(' ');
            }
            v1 = (split_pos < 0) ? value : value.substring(0, split_pos).trim();
            v2 = (split_pos < 0) ? "" : value.substring(split_pos + 1).trim();
        }
        
        // Extract float number and unit:
        String rad1 = (v1.length() >= 2) ? v1.substring(0, v1.length() - 2) : v1;
        String rad2 = (v2.length() >= 2) ? v2.substring(0, v2.length() - 2) : v2;
        String unit = (v1.length() >= 2) ? v1.substring(v1.length() - 2) : "px";
        
        // Assure that only valid numbers are passed:
        try {
            if (rad1.length() > 0) Double.parseDouble(rad1);
            if (rad2.length() > 0) Double.parseDouble(rad2);
        } catch (Exception ex) {
            rad1 = "";
            rad2 = "";
        }
        
        box1.setText(rad1);
        box2.setText(rad2);
        if (! WhdUtils.selectListItem(unit_box, unit)) {
            WhdUtils.selectListItem(unit_box, "px");  //  unit = "px";
        }
    }
    
    private String getEditBorderDialogPreviewCSS()
    {
        String css = getBorderCSS() + "background-color:" + getBgColor() + "; ";
        if (editBorderBgImageURL != null) {
            css += "background-image:" + editBorderBgImageURL + "; background-repeat:" + getBgRepeat() + ";";
        }
        return css;
    }
    
    private String formatBorderCSS(String position, String style, String width, String col)
    {
        String css;
        if (position == null) {
            if (style.equals("none")) {
                css = "border-style:" + style + "; ";
            } else {
                css = "border-style:" + style + 
                    "; border-width:" + width +
                    "; border-color:" + col + "; ";
            }
        } else {
            if (style.equals("none")) {
                css = "border-" + position + "-style:" + style + "; ";
            } else {
                css = "border-" + position + "-style:" + style + 
                    "; border-" + position + "-width:" + width +
                    "; border-" + position + "-color:" + col + "; ";
            }
        }
        return css;
    }
    
    private String getBorderRadiusValue(Doublespinner val1_box, 
                                        Doublespinner val2_box, 
                                        Listbox unit_box, 
                                        char sep, 
                                        String default_value) 
    {
        String v1 = getBorderRadiusValue(val1_box, unit_box);
        String v2 = getBorderRadiusValue(val2_box, unit_box);
        if ((v1 == null) && (v2 == null)) {
            return default_value;
        }
        if (v2 == null) {
            return v1;
        }
        if (v1 == null) {
            Listitem item = unit_box.getSelectedItem();
            v1 = "0" + ((item == null) ? "px" : item.getValue());
        }
        return v1 + sep + v2;
    }

    private String getBorderRadiusValue(Doublespinner val_box, Listbox unit_box) 
    {
        String v = val_box.getText();
        v = (v == null) ? "" : v.trim();
        if (v.replace("0", "").replace(".", "").equals("")) {
            return null;   // zero
        } else {
            Listitem item = unit_box.getSelectedItem();
            return v + ((item == null) ? "px" : item.getValue());
        }
    }

    private void editBorderSameOrRadiusUpdated()
    {
        boolean is_same = whdEditBorderSameBox.isChecked();
        boolean has_radius = whdEditBorderEnableRadiusBox.isChecked();
        
        whdEditBorderTopLeftArea.setVisible(! is_same);
        whdEditBorderTopRightArea.setVisible(has_radius || (! is_same));
        whdEditBorderLeftArea.setVisible(! is_same);
        whdEditBorderRightArea.setVisible(! is_same);
        whdEditBorderBottomArea.setVisible(! is_same);
        
        whdEditBorderTopCell.setAlign((has_radius && is_same) ? "left" : "center");
        whdEditBorderTopLabel.setValue(is_same ? "Border:" : "Border-Top:");
        
        whdEditBorderTopLeftRadiusArea.setVisible(has_radius);
        whdEditBorderTopRightRadiusArea.setVisible(has_radius);
        whdEditBorderBottomLeftRadiusArea.setVisible(has_radius);
        whdEditBorderBottomRightRadiusArea.setVisible(has_radius);
        
        whdEditBorderTopRightArea.setWidth((has_radius && is_same) ? "50%" : "25%");
        
        whdBorderWin.setWidth(is_same ? (has_radius ? "360px" : "300px") : "580px");
        whdBorderWin.invalidate();
    }
    
    private void updateBorderDialogPreview()
    {
        whdEditBorderPreviewDiv.setStyle(getEditBorderDialogPreviewCSS());
        // whdEditBorderPreviewDiv.invalidate();
        updateBgUploadButtons();
        // whdBorderWin.invalidate();
    }

    private void updateBgUploadButtons()
    {
        whdEditBorderBgImageDownloadBtn.setVisible(editBorderBgImageURL != null);
        whdEditBorderBgImageRemoveBtn.setVisible(editBorderBgImageURL != null);
        whdEditBorderBgRepeatArea.setVisible(editBorderBgImageURL != null);
    }
}
